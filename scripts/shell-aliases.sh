#!/usr/bin/env bash
#
# Shell functions for day-to-day work against the two clusters.
#
#   echo 'source ~/repos/event-junkie/scripts/shell-aliases.sh' >> ~/.zshrc
#
# A file rather than a block pasted into a cheatsheet, because a cheatsheet drifts from reality
# silently and this drifts loudly: it is reviewed in PRs, ShellCheck runs over it in `pre-commit`,
# and a wrong path fails in your terminal instead of reading plausibly on a page.
#
# **Nothing here wraps `tofu`, `helm upgrade`, or anything that writes to production.** Those want
# the friction; see infra/AGENTS.md and deploy/AGENTS.md, both of which open with what must never be
# run on your own initiative.
#
# Functions rather than aliases throughout, so arguments pass through: `ejk get pods -A` works.
#
# The counterpart documentation is docs/ops/DAILY_COMMANDS.md.

# shellcheck shell=bash

EJ_SSH_KEY="${EJ_SSH_KEY:-$HOME/.ssh/id_ed25519_hetzner}"
# Where the checkout is. `${BASH_SOURCE[0]}` is empty when this file is sourced by zsh, which is
# what the install line above does, so the path is a variable with the documented clone as default.
EJ_REPO="${EJ_REPO:-$HOME/repos/event-junkie}"
EJ_STAGING="${EJ_STAGING:-10.10.1.1}"
EJ_PRODUCTION="${EJ_PRODUCTION:-10.10.0.1}"
EJ_PRODUCTION_DB="${EJ_PRODUCTION_DB:-10.0.1.20}"

# --- the session --------------------------------------------------------------------------------
#
# Tunnels, the handshake check and the port-forwards live in scripts/ej.sh; these are its short
# names and nothing more, so there is one copy of the mechanics. `ej-up` is the tunnel *and* the
# three forwards (importer, BFF, OpenObserve); `ej-down` takes both down again.

ej-up() { "$EJ_REPO/scripts/ej.sh" up staging; }
ej-up-prod() { "$EJ_REPO/scripts/ej.sh" up production; }
ej-down() { "$EJ_REPO/scripts/ej.sh" down staging "$@"; }
ej-down-prod() { "$EJ_REPO/scripts/ej.sh" down production "$@"; }
ej-status() { "$EJ_REPO/scripts/ej.sh" status; }
ej-versions() { "$EJ_REPO/scripts/ej.sh" versions; }

# --- cluster ----------------------------------------------------------------------------------
#
# `--context` is pinned rather than relying on the current one. Both clusters live in the same
# kubeconfig, so "which cluster am I on" is otherwise a question you have to remember to ask.

ejk() { kubectl --context event-junkie-staging "$@"; }
ejkp() { kubectl --context event-junkie-production "$@"; }
ejf() { flux --context event-junkie-staging "$@"; }
ejfp() { flux --context event-junkie-production "$@"; }
ej9() { k9s --context event-junkie-staging "$@"; }
ej9p() { k9s --context event-junkie-production "$@"; }

# --- the site ---------------------------------------------------------------------------------
#
# `-k` is correct and must not be "fixed": staging issues from Let's Encrypt's *staging* CA so the
# production rate limit is not burned. --resolve rather than /etc/hosts, so nothing is left behind.

ej-site() {
    curl -sS -k --max-time 20 --resolve "staging.event-junkie.de:443:${EJ_STAGING}" \
        "https://staging.event-junkie.de${1:-/}" -o /dev/null \
        -w 'staging %{http_code} in %{time_total}s\n'
}

ej-api() {
    curl -sS -k --max-time 20 --resolve "staging.event-junkie.de:443:${EJ_STAGING}" \
        "https://staging.event-junkie.de/api/${1:-events?size=1}"
}

# --- one venue, end to end ---------------------------------------------------------------------
#
# "Is this venue importing?" is answered from the APIs rather than from psql on the node: the
# importer's admin API has the source row, and the public API says whether those events reached what
# a visitor sees — the better question, and the one psql cannot answer.
#
# The importer is deliberately unroutable (#416 — no Ingress path names it, and nothing in its
# namespace may reach it), so its half needs a port-forward. Node-originated traffic is not subject
# to NetworkPolicy in k3s, which is why port-forward still works. The site's half goes through the
# ingress on purpose: TLS, routing and middlewares are part of what is being checked.

ej-venue() {
    local slug="${1:?usage: ej-venue <slug>}"
    printf '=== source row (importer admin API) ===\n'
    kubectl --context event-junkie-staging -n event-junkie port-forward svc/event-junkie-importer 8081:8081 >/dev/null 2>&1 &
    local pf=$!
    sleep 3
    # The error body carries a `status` field of its own, so printing it unfiltered renders
    # `"status": 404` where a source row's status belongs — which reads as a very broken venue
    # rather than a typo. Check the code instead.
    local code
    code=$(curl -sS -o /tmp/ej-venue.json -w '%{http_code}' --max-time 20 \
        "http://localhost:8081/api/admin/event-sources/${slug}")
    if [ "$code" = "200" ]; then
        python3 -c 'import json,sys; d=json.load(open("/tmp/ej-venue.json")); print(json.dumps({k: d.get(k) for k in ("slug","status","retryCount","lastImportAt","lastSuccessAt","lastEventCount","lastError")}, indent=2))'
    else
        printf 'no source with slug "%s" (HTTP %s)\n' "$slug" "$code"
    fi
    rm -f /tmp/ej-venue.json
    kill "$pf" 2>/dev/null
    printf '\n=== what the site would serve ===\n'
    ej-api "events?venue=${slug}&size=3" |
        python3 -c 'import json,sys; d=json.load(sys.stdin); print("future events:", d.get("totalElements")); [print(" ", e.get("eventDate"), (e.get("title") or "")[:60]) for e in (d.get("content") or [])]'
}

# --- database ---------------------------------------------------------------------------------
#
# Opens the forward, runs psql, and closes the forward again — an -f -N ssh left running is the
# thing you find three days later wondering what is holding port 15432.

_ej_psql() {
    local ctx="$1" jump="$2" target="$3" port="$4"
    local pw
    pw="$(kubectl --context "$ctx" get secret events-db -n event-junkie -o jsonpath='{.data.password}' | base64 -d)" || return 1
    ssh -f -N -i "$EJ_SSH_KEY" -L "${port}:${target}:5432" "ops@${jump}" || return 1
    PGPASSWORD="$pw" psql -h 127.0.0.1 -p "$port" -U events -d events
    local rc=$?
    pkill -f "ssh -f -N -i ${EJ_SSH_KEY} -L ${port}:${target}:5432" 2>/dev/null
    return $rc
}

ej-db() { _ej_psql event-junkie-staging "$EJ_STAGING" localhost 15432; }
ej-db-prod() { _ej_psql event-junkie-production "$EJ_PRODUCTION" "$EJ_PRODUCTION_DB" 15433; }

# A superuser shell, for anything CREATE ROLE-shaped. The forwards above connect as `events`,
# which cannot do it.
ej-psql-super() { ssh -i "$EJ_SSH_KEY" "ops@${EJ_STAGING}" 'sudo -u postgres psql -d events'; }
ej-psql-super-prod() {
    ssh -i "$EJ_SSH_KEY" -J "ops@${EJ_PRODUCTION}" "ops@${EJ_PRODUCTION_DB}" 'sudo -u postgres psql -d events'
}

# --- backups ------------------------------------------------------------------------------------

# `walg check`, not `systemctl status`: the timers can be green while every archive fails.
ej-backups() { ssh -i "$EJ_SSH_KEY" "ops@${EJ_STAGING}" 'sudo -u postgres walg check'; }
ej-backups-prod() {
    ssh -i "$EJ_SSH_KEY" -J "ops@${EJ_PRODUCTION}" "ops@${EJ_PRODUCTION_DB}" 'sudo -u postgres walg check'
}
