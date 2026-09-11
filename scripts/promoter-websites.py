#!/usr/bin/env python3
"""Write the reviewed promoter websites onto the promoters (#328).

Dry run by default. `--apply` is what writes, because a promoter PUT replaces every field.

    python3 scripts/promoter-websites.py                       # show the plan
    python3 scripts/promoter-websites.py --apply               # write it
    python3 scripts/promoter-websites.py --host http://localhost:18081 --apply
    python3 scripts/promoter-websites.py --promoter loft-concerts

Reads docs/promoters/REVIEWED.tsv, one row per promoter the site holds, with the kind a person
gave it, the site they found, and the spelling that site uses. Only a row with a `website` is
written, and only `website_url` changes: the request carries the promoter's current name and
image fields back unchanged.

A row whose stored name differs from the reviewed one is reported and skipped. The name is not
written here, because a PUT that changes the name changes the slug, and the next import then
creates the old row again unless `PromoterNormalizer` maps the raw credit onto the new spelling.
V023 renames the rows the review found, and the normalizer carries the same pins; after it has
run, the names agree and nothing is skipped.

Standard library only, and no key: the admin API is unauthenticated inside the cluster.
"""

import argparse
import csv
import json
import sys
import urllib.error
import urllib.request

REVIEWED = "docs/promoters/REVIEWED.tsv"
LOCAL_HOST = "http://localhost:18081"
PAGE_SIZE = 100
MAX_PAGES = 100


def get_json(url):
    request = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def fetch_promoters(host):
    """Every promoter the target holds, keyed by slug."""
    promoters = []
    for page in range(MAX_PAGES):
        body = get_json(f"{host}/api/admin/promoters?page={page}&size={PAGE_SIZE}&sort=name,asc")
        promoters.extend(body.get("content", []))
        if page + 1 >= body.get("totalPages", 0):
            break
    return {p["slug"]: p for p in promoters}


def put_promoter(host, promoter_id, body):
    request = urllib.request.Request(
        f"{host}/api/admin/promoters/{promoter_id}",
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json"},
        method="PUT",
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.status


def read_reviewed(only=None):
    with open(REVIEWED, encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    with_site = [r for r in rows if r["website"]]
    if only:
        with_site = [r for r in with_site if r["slug"] == only]
    return rows, with_site


def main():
    parser = argparse.ArgumentParser(description="Write the reviewed promoter websites onto the promoters.")
    parser.add_argument("--host", default=LOCAL_HOST, help=f"importer admin API (default {LOCAL_HOST})")
    parser.add_argument("--apply", action="store_true", help="write; without it nothing is changed")
    parser.add_argument("--promoter", help="only this promoter, by slug")
    parser.add_argument("--force", action="store_true", help="also replace a website a promoter already has")
    args = parser.parse_args()

    rows, with_site = read_reviewed(args.promoter)
    if not with_site:
        print(f"no row with a website in {REVIEWED}" + (f" for {args.promoter!r}" if args.promoter else ""))
        return 1
    print(f"{len(rows)} reviewed, {len(with_site)} with a website\n")

    try:
        promoters = fetch_promoters(args.host)
    except (urllib.error.URLError, OSError) as error:
        print(f"cannot reach {args.host}: {error}")
        return 1

    written = skipped = 0
    for row in with_site:
        promoter = promoters.get(row["slug"])
        if promoter is None:
            print(f"  skip  {row['slug']}: not on the target")
            skipped += 1
            continue
        if promoter["name"] != row["name"]:
            print(f"  skip  {row['slug']}: stored as {promoter['name']!r}, reviewed as {row['name']!r}")
            skipped += 1
            continue
        current = promoter.get("websiteUrl")
        if current == row["website"]:
            continue
        if current and not args.force:
            print(f"  skip  {row['slug']}: already {current} (--force replaces it)")
            skipped += 1
            continue
        body = {
            "name": promoter["name"],
            "websiteUrl": row["website"],
            "imageUrl": promoter.get("imageUrl"),
            "imageAttribution": promoter.get("imageAttribution"),
            "imageLicenceId": promoter.get("imageLicenceId"),
            "imageSourceUrl": promoter.get("imageSourceUrl"),
        }
        verb = "write" if args.apply else "would write"
        print(f"  {verb}  {row['slug']}: {row['website']}")
        if args.apply:
            try:
                put_promoter(args.host, promoter["id"], body)
            except urllib.error.HTTPError as error:
                print(f"  fail  {row['slug']}: {error.code} {error.read().decode(errors='replace')[:200]}")
                skipped += 1
                continue
        written += 1

    print(f"\n{'Wrote' if args.apply else 'Would write'} {written}, skipped {skipped}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
