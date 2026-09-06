#!/usr/bin/env python3
"""Render docs/LINKS.md and docs/ops/DAILY_COMMANDS.md into the files the operations page reads.

Run it through `scripts/dashboard-parity.sh`, which owns the check mode and the CSS half. This half
only reads Markdown and writes two files, and needs nothing outside the standard library:

    python3 scripts/links_export.py --js docs/ops/dashboard/links.js --bookmarks docs/event-junkie-bookmarks.html

LINKS.md is the source of every link. A section is a `## N. Title` heading; its links are the first
table under it whose first header is `Link`, `Address` or `Document`. Any other table (the buckets
under §2) and all prose are skipped by that rule. A link cell is one of `<url>`, `<url> _(added)_`,
`` `text` `` (no URL — kept as text) or ``[`label`](relative)``, which becomes a GitHub blob URL.

DAILY_COMMANDS.md is the source of the cheatsheet: every fenced `sh` block, with the `##` heading
above it. Nothing here is typed twice; a command changed in the runbook changes on the page.

The bookmarks file is the same set in the Netscape format every browser imports. It carries a
constant ADD_DATE so that unchanged inputs produce a byte-identical file, which is what lets the
parity check be a plain diff.
"""

import argparse
import html
import json
import os
import posixpath
import re

REPO_BLOB = "https://github.com/enorm-labs/event-junkie/blob/main/"
BOOKMARK_DATE = "1787140800"
LINK_HEADERS = {"Link", "Address", "Document"}

HEADING = re.compile(r"^## (\d+)\. (.+?)\s*$")
FENCE = re.compile(r"^```sh\s*$")
TABLE_ROW = re.compile(r"^\|(.*)\|\s*$")
AUTOLINK = re.compile(r"<(https?://[^>]+)>")
MD_LINK = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")
BOLD = re.compile(r"\*\*(.+?)\*\*")
EM = re.compile(r"(?<![\w`])_(.+?)_(?![\w`])")
CODE_SPLIT = re.compile(r"(`[^`]*`)")


def inline_html(text):
    """Inline Markdown to HTML: code, links, bold, emphasis. Everything else is escaped."""
    out = []
    for part in CODE_SPLIT.split(text):
        if part.startswith("`") and part.endswith("`") and len(part) >= 2:
            out.append("<code>%s</code>" % html.escape(part[1:-1]))
            continue
        piece = html.escape(part, quote=False)
        piece = AUTOLINK.sub(r'<a href="\1">\1</a>', piece)
        piece = MD_LINK.sub(r'<a href="\2">\1</a>', piece)
        piece = BOLD.sub(r"<strong>\1</strong>", piece)
        piece = EM.sub(r"<em>\1</em>", piece)
        out.append(piece)
    return "".join(out)


def inline_text(text):
    """The same Markdown with every marker removed, for the bookmark description."""
    text = CODE_SPLIT.sub(lambda m: m.group(1)[1:-1], text)
    text = AUTOLINK.sub(r"\1", text)
    text = MD_LINK.sub(r"\1", text)
    text = BOLD.sub(r"\1", text)
    text = EM.sub(r"\1", text)
    return text.strip()


def parse_link_cell(cell, docs_dir):
    """Return (url, label) — url is None for a cell that names something with no address."""
    cell = cell.replace("_(added)_", "").strip()
    m = AUTOLINK.fullmatch(cell)
    if m:
        url = m.group(1)
        return url, re.sub(r"^https?://", "", url)
    m = MD_LINK.fullmatch(cell)
    if m:
        label = inline_text(m.group(1))
        target = m.group(2)
        if target.startswith(("http://", "https://")):
            return target, label
        path = posixpath.normpath(posixpath.join(docs_dir, target))
        return REPO_BLOB + path, label
    return None, inline_text(cell)


def read_lines(path):
    with open(path, encoding="utf-8") as f:
        return f.read().splitlines()


def split_row(line):
    m = TABLE_ROW.match(line)
    if not m:
        return None
    return [c.strip() for c in m.group(1).split("|")]


def parse_links(path):
    docs_dir = posixpath.dirname(path)
    lines = read_lines(path)
    sections = []
    current = None
    i = 0
    while i < len(lines):
        line = lines[i]
        m = HEADING.match(line)
        if m:
            current = {"number": int(m.group(1)), "title": m.group(2), "columns": [], "rows": []}
            sections.append(current)
            i += 1
            continue
        cells = split_row(line)
        if cells and current is not None and not current["rows"] and cells[0] in LINK_HEADERS:
            current["columns"] = cells
            i += 2  # the header and its `| --- |` line
            while i < len(lines):
                row = split_row(lines[i])
                if row is None:
                    break
                if len(row) != len(cells):
                    raise SystemExit("%s:%d: %d cells under a %d-column header" % (path, i + 1, len(row), len(cells)))
                url, label = parse_link_cell(row[0], docs_dir)
                entry = {
                    "url": url,
                    "label": label,
                    "what": inline_html(row[1]),
                    "whatText": inline_text(row[1]),
                    "status": inline_html(row[2]) if len(row) > 2 else "",
                    "statusText": inline_text(row[2]) if len(row) > 2 else "",
                }
                current["rows"].append(entry)
                i += 1
            continue
        i += 1
    if not sections:
        raise SystemExit("%s: no `## N. Title` section found" % path)
    return sections


def parse_cheatsheet(path):
    lines = read_lines(path)
    groups = []
    heading = None
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("## "):
            heading = line[3:].strip()
        elif FENCE.match(line):
            block = []
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                block.append(lines[i])
                i += 1
            commands = []
            pending = ""
            for raw in block:
                if not raw.strip():
                    continue
                pending = (pending + "\n" + raw) if pending else raw
                if raw.rstrip().endswith("\\"):
                    continue
                m = re.match(r"^(.*?\S)\s{2,}#\s*(.*)$", pending, re.S)
                if m:
                    commands.append({"command": m.group(1), "note": m.group(2)})
                else:
                    commands.append({"command": pending, "note": ""})
                pending = ""
            if commands:
                if not groups or groups[-1]["heading"] != heading:
                    groups.append({"heading": heading, "commands": []})
                groups[-1]["commands"].extend(commands)
        i += 1
    if not groups:
        raise SystemExit("%s: no fenced sh block found" % path)
    return groups


def bookmark_title(row):
    text = row["whatText"]
    for sep in (" — ", ". ", ", "):
        if sep in text:
            text = text.split(sep, 1)[0]
    text = text.strip().rstrip(".")
    return text if text else row["label"]


def render_bookmarks(sections, source):
    out = [
        "<!DOCTYPE NETSCAPE-Bookmark-file-1>",
        "<!-- Event Junkie — every external service, console and reference the project depends on.",
        "     Generated from %s by scripts/links_export.py; edit that file, then run scripts/dashboard-parity.sh." % source,
        "     Import via your browser's bookmark manager:",
        "       Firefox  Bookmarks > Manage Bookmarks > Import and Backup > Import Bookmarks from HTML",
        "       Chrome   Bookmarks > Bookmark Manager > (kebab menu) > Import Bookmarks",
        "       Safari   File > Import From > Bookmarks HTML File",
        "     Importing ADDS a folder; it does not replace what you already have. -->",
        '<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">',
        "<TITLE>Bookmarks</TITLE>",
        "<H1>Bookmarks</H1>",
        "",
        "<DL><p>",
        '    <DT><H3 ADD_DATE="%s" LAST_MODIFIED="%s">Event Junkie</H3>' % (BOOKMARK_DATE, BOOKMARK_DATE),
        "    <DL><p>",
    ]
    for section in sections:
        rows = [r for r in section["rows"] if r["url"]]
        if not rows:
            continue
        out.append("")
        out.append(
            '        <DT><H3 ADD_DATE="%s" LAST_MODIFIED="%s">%d · %s</H3>'
            % (BOOKMARK_DATE, BOOKMARK_DATE, section["number"], html.escape(section["title"], quote=False))
        )
        out.append("        <DL><p>")
        for row in rows:
            out.append(
                '            <DT><A HREF="%s" ADD_DATE="%s">%s</A>'
                % (html.escape(row["url"], quote=True), BOOKMARK_DATE, html.escape(bookmark_title(row), quote=False))
            )
            description = row["whatText"]
            if row["statusText"]:
                description = "%s Status: %s." % (description.rstrip(".") + ".", row["statusText"].rstrip("."))
            if description.rstrip(".") != bookmark_title(row):
                out.append("            <DD>%s" % html.escape(description, quote=False))
        out.append("        </DL><p>")
    out.extend(["", "    </DL><p>", "</DL><p>", ""])
    return "\n".join(out)


def render_js(sections, cheatsheet, links_source, commands_source):
    doc = {
        "generatedFrom": {"links": links_source, "cheatsheet": commands_source},
        "sections": sections,
        "cheatsheet": cheatsheet,
    }
    return "window.EJ_LINKS = " + json.dumps(doc, indent=2, ensure_ascii=False) + ";\n"


def main():
    parser = argparse.ArgumentParser(description="Render LINKS.md and DAILY_COMMANDS.md for the operations page.")
    parser.add_argument("--links", default="docs/LINKS.md", help="the links document (default: docs/LINKS.md)")
    parser.add_argument("--commands", default="docs/ops/DAILY_COMMANDS.md", help="the cheatsheet source")
    parser.add_argument("--js", required=True, help="where to write links.js")
    parser.add_argument("--bookmarks", required=True, help="where to write the Netscape bookmarks file")
    args = parser.parse_args()

    sections = parse_links(args.links)
    cheatsheet = parse_cheatsheet(args.commands)
    with open(args.js, "w", encoding="utf-8") as f:
        f.write(render_js(sections, cheatsheet, args.links, args.commands))
    with open(args.bookmarks, "w", encoding="utf-8") as f:
        f.write(render_bookmarks(sections, os.path.relpath(args.links, os.path.dirname(args.bookmarks) or ".")))
    print(
        "%s: %d sections, %d links; %s: %d groups"
        % (args.js, len(sections), sum(len(s["rows"]) for s in sections), args.commands, len(cheatsheet))
    )


if __name__ == "__main__":
    main()
