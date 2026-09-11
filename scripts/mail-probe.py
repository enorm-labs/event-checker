#!/usr/bin/env python3
"""Prove a role mailbox still receives, and ping healthchecks.io only when it does (#637).

    python3 scripts/mail-probe.py --mailbox hello@event-junkie.de
    python3 scripts/mail-probe.py --mailbox security@event-junkie.de --ping-url https://hc-ping.com/...

**A conditional ping, never a heartbeat** (#271). It sends a message to the mailbox, waits for that
exact message to arrive, deletes it, and only then pings. So one check also catches a dead MX, a
lapsed hosting package, a full mailbox and an apply that removed the records — none of which a
heartbeat, or a bare IMAP login, would notice.

**It sends through Hetzner's SMTP with the mailbox credential, and that is not a convenience.** SPF
authorises the hosting server and nothing else, so a message sent straight from a CI runner claiming
to be `hello@` fails SPF under `p=reject` and never arrives. That failure looks exactly like the
outage this exists to detect.

The password comes from the environment (`MAIL_PROBE_PASSWORD`) rather than an argument, because an
argument is visible in `ps` and in a CI log's command echo.

What it does not prove: that the mailbox's `Kopie an` forward reaches a person. `docs/ops/EMAIL.md`
§7 carries that as a manual step, on purpose — see the issue and the plan for why the alternative
was rejected. That forward also copies every probe to the person before the delete runs, so the
probe lands in their inbox daily; the body says so and names the subject prefix to filter on.
"""

import argparse
import email.utils
import imaplib
import os
import smtplib
import sys
import time
import urllib.error
import urllib.request
import uuid
from email.message import EmailMessage

# Hetzner's generic client hostnames. **Not the MX target**, which is the account's own server: the
# two are different names and swapping them fails as though the password were wrong
# (docs/ops/EMAIL.md §6).
#
# **Never retry a failed login here, and never loop one by hand.** Hetzner blocks the offending
# service for the source address after a failed authentication. One wrong password was enough to
# leave 465 refusing a machine while 25, 587 and 993 still answered from the same address. A probe
# that retried would lock itself out of the port it exists to test, and the refusal reads like an
# outage rather than like a wrong credential.
SMTP_HOST = "mail.your-server.de"
SMTP_PORT = 465
IMAP_HOST = "mail.your-server.de"
IMAP_PORT = 993

# Delivery on the same host is usually under a second. Twelve attempts five seconds apart is a
# minute of patience before calling it broken, which is far past normal and still inside a CI job.
POLL_ATTEMPTS = 12
POLL_SECONDS = 5

# The subject a human will see if a probe ever fails to clean up after itself. It says what it is,
# names the issue, and carries an id no other run can match.
SUBJECT_PREFIX = "[mail-probe]"


class ProbeError(Exception):
    """A named failure. The message is what reaches the alert, so it says which hop broke."""


def send(mailbox: str, password: str, subject: str) -> None:
    message = EmailMessage()
    message["From"] = mailbox
    message["To"] = mailbox
    message["Subject"] = subject
    message["Date"] = email.utils.formatdate(localtime=True)
    message["Message-ID"] = email.utils.make_msgid(domain=mailbox.split("@")[-1])
    message.set_content(
        "Automated delivery probe for the role mailboxes (#637).\n\n"
        "It is sent and deleted by scripts/mail-probe.py, once a day per mailbox.\n\n"
        "Reading it in a forward target is expected: the mailbox's Kopie an forward copies it before\n"
        "the probe deletes the original. Filter on the subject prefix [mail-probe].\n\n"
        "Reading it in the role mailbox itself means the probe could not clean up after itself —\n"
        "see docs/ops/EMAIL.md § Monitoring.\n"
    )

    try:
        with smtplib.SMTP_SSL(SMTP_HOST, SMTP_PORT, timeout=30) as smtp:
            smtp.login(mailbox, password)
            smtp.send_message(message)
    except smtplib.SMTPAuthenticationError as error:
        raise ProbeError(f"SMTP rejected the credential for {mailbox}: {error}") from error
    except (smtplib.SMTPException, OSError) as error:
        raise ProbeError(f"could not submit through {SMTP_HOST}:{SMTP_PORT}: {error}") from error


def open_mailbox(mailbox: str, password: str) -> imaplib.IMAP4_SSL:
    try:
        connection = imaplib.IMAP4_SSL(IMAP_HOST, IMAP_PORT, timeout=30)
    except OSError as error:
        raise ProbeError(f"could not reach {IMAP_HOST}:{IMAP_PORT}: {error}") from error

    try:
        connection.login(mailbox, password)
    except imaplib.IMAP4.error as error:
        raise ProbeError(f"IMAP rejected the credential for {mailbox}: {error}") from error

    connection.select("INBOX")
    return connection


def search(connection: imaplib.IMAP4_SSL, subject: str) -> list[str]:
    """UIDs of the messages with this subject.

    **UID rather than sequence number.** A sequence number is an index into the mailbox as it was
    when the search ran, and real mail arriving between the search and the delete would shift it —
    so the delete would remove somebody's message instead of ours. A UID never moves.
    """
    status, data = connection.uid("SEARCH", None, "SUBJECT", f'"{subject}"')
    if status != "OK" or not data or not data[0]:
        return []
    # imaplib returns bytes, and Python 3.14's command builder rejects them. Decode once, here.
    return data[0].decode().split()


def delete(connection: imaplib.IMAP4_SSL, uids: list[str]) -> None:
    """Remove what this run sent. A probe that fills the mailbox breaks the thing it watches."""
    for uid in uids:
        connection.uid("STORE", uid, "+FLAGS", "(\\Deleted)")
    connection.expunge()


def purge_leftovers(connection: imaplib.IMAP4_SSL) -> int:
    """Delete probe mail from earlier runs, before this run sends its own.

    Anything carrying the prefix at this point is stale by definition: this run's subject does not
    exist yet. A run that fails after sending leaves one message behind, and without this the
    mailbox would fill with exactly the traffic the probe exists to keep it clear of.
    """
    stale = search(connection, SUBJECT_PREFIX)
    if stale:
        delete(connection, stale)
    return len(stale)


def wait_for_delivery(connection: imaplib.IMAP4_SSL, mailbox: str, subject: str) -> None:
    """Poll until the message arrives, then delete it. Raises when it never does."""
    for attempt in range(POLL_ATTEMPTS):
        # A server reports newly arrived mail when a command asks it to, not spontaneously. Without
        # this the loop can search a view of the mailbox frozen at SELECT and time out on a message
        # that is already there.
        try:
            connection.noop()
        except (imaplib.IMAP4.error, OSError) as error:
            raise ProbeError(f"IMAP connection dropped while waiting: {error}") from error

        found = search(connection, subject)
        if found:
            delete(connection, found)
            print(f"delivered after {attempt * POLL_SECONDS}s")
            return
        time.sleep(POLL_SECONDS)

    waited = POLL_ATTEMPTS * POLL_SECONDS
    raise ProbeError(f"accepted by SMTP but not delivered to {mailbox} within {waited}s")


def ping(url: str, suffix: str = "") -> None:
    """Best effort. A failed ping must not turn a healthy mailbox into a red build."""
    try:
        with urllib.request.urlopen(f"{url}{suffix}", timeout=15) as response:
            response.read()
    except (urllib.error.URLError, OSError) as error:
        print(f"warning: could not ping healthchecks.io: {error}", file=sys.stderr)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Send to a role mailbox, wait for it to arrive, and ping healthchecks.io on success.",
        epilog="The password is read from MAIL_PROBE_PASSWORD.",
    )
    parser.add_argument("--mailbox", required=True, help="full address, e.g. hello@event-junkie.de")
    parser.add_argument("--ping-url", default="", help="healthchecks.io ping URL; without it nothing is pinged")
    arguments = parser.parse_args()

    password = os.environ.get("MAIL_PROBE_PASSWORD", "")
    if not password:
        print("MAIL_PROBE_PASSWORD is not set", file=sys.stderr)
        return 2

    subject = f"{SUBJECT_PREFIX} {uuid.uuid4()} {time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())}"

    connection = None
    try:
        connection = open_mailbox(arguments.mailbox, password)
        purged = purge_leftovers(connection)
        if purged:
            print(f"removed {purged} message(s) left by an earlier run")
        send(arguments.mailbox, password, subject)
        wait_for_delivery(connection, arguments.mailbox, subject)
    except ProbeError as error:
        print(f"{arguments.mailbox}: {error}", file=sys.stderr)
        # Fail fast rather than waiting out the grace period. Silence still covers what this cannot
        # report: the runner never starting at all.
        if arguments.ping_url:
            ping(arguments.ping_url, "/fail")
        return 1

    finally:
        if connection is not None:
            try:
                connection.logout()
            except (imaplib.IMAP4.error, OSError):
                # A failed logout says nothing about whether the mailbox receives, which is the only
                # question here. The verdict is already decided above, and raising from a `finally`
                # would replace it with a cleanup error.
                pass

    print(f"{arguments.mailbox}: receives")
    if arguments.ping_url:
        ping(arguments.ping_url)
    return 0


if __name__ == "__main__":
    sys.exit(main())
