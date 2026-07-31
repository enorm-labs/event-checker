#!/usr/bin/env bash
#
# dev-env.sh — local importer environment control for importer smoke tests.
#
# Deterministic mechanics behind the /importer-smoke and /next-importer skills:
# start/stop the dev stack, seed sources, trigger imports, and query the resulting
# data. Everything here is scripted on purpose so the agent does not re-derive
# docker/psql/curl incantations on every loop iteration.
#
# Usage: scripts/dev-env.sh <command> [args]
#
#   db-reset                     Drop the Postgres volume and start a fresh database
#   up [--scheduling]            Start the importer in the background, wait for health
#   down [--db]                  Stop the importer (and with --db the database too)
#   status                       Show database + importer state
#   seed-all                     Run http/importer/dev-seed.http via ijhttp (all sources)
#   seed-one <venue.json> <source.json>
#                                POST one venue + one event source, print the source slug
#   import <slug> [timeout]      Trigger an import for one source, poll until it settles
#   snapshot [file]              Write per-source event counts to a TSV file
#   diff-snapshot <before> <after>
#                                Show per-source count deltas between two snapshots
#   check <slug>                 Data-quality report for a single source
#   psql <sql>                   Run raw SQL against the events schema
#
# Environment overrides:
#   POSTGRES_HOST_PORT (56298) · IMPORTER_HOST (http://localhost:8081)
#   PGUSER_LOCAL (admin) · PGPASSWORD_LOCAL (admin) · PGDATABASE_LOCAL (event_checker)

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$REPO_ROOT/build/dev-env"
LOG_FILE="$RUN_DIR/importer.log"
PID_FILE="$RUN_DIR/importer.pid"

DB_PORT="${POSTGRES_HOST_PORT:-56298}"
DB_USER="${PGUSER_LOCAL:-admin}"
DB_PASS="${PGPASSWORD_LOCAL:-admin}"
DB_NAME="${PGDATABASE_LOCAL:-event_checker}"
HOST="${IMPORTER_HOST:-http://localhost:8081}"
IMPORTER_PORT="${HOST##*:}"

log() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m!!\033[0m %s\n' "$*" >&2; }
die() {
    printf '\033[1;31mxx\033[0m %s\n' "$*" >&2
    exit 1
}

need() { command -v "$1" >/dev/null 2>&1 || die "'$1' is required but not installed"; }

run_psql() {
    need psql
    PGPASSWORD="$DB_PASS" psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        -v ON_ERROR_STOP=1 "$@"
}

query() { run_psql -qAt -c "SET search_path TO events; $1"; }

importer_healthy() {
    curl -sf --max-time 3 "$HOST/actuator/health" 2>/dev/null | grep -q '"status":"UP"'
}

db_ready() {
    docker compose -f "$REPO_ROOT/compose.yaml" exec -T postgres \
        pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1
}

# ---------------------------------------------------------------- commands

cmd_db_reset() {
    need docker
    log "Tearing down Postgres including volumes"
    (cd "$REPO_ROOT" && docker compose down --volumes --remove-orphans)
    log "Starting a fresh Postgres"
    (cd "$REPO_ROOT" && docker compose up -d postgres)
    for _ in $(seq 1 60); do
        if db_ready; then
            log "Database ready on port $DB_PORT (empty — Flyway runs on importer start)"
            return 0
        fi
        sleep 1
    done
    die "Database did not become ready within 60s"
}

cmd_up() {
    local scheduling=false
    [[ "${1:-}" == "--scheduling" ]] && scheduling=true

    if importer_healthy; then
        log "Importer already healthy at $HOST"
        return 0
    fi

    mkdir -p "$RUN_DIR"
    # bootRun's working directory is the module, so Spring's Docker Compose support has to be
    # pointed at the root compose.yaml explicitly; `start_only` leaves Postgres running when the
    # importer stops, so restarts between smoke-test rounds keep the data.
    local props=(
        "--spring.docker.compose.file=$REPO_ROOT/compose.yaml"
        "--spring.docker.compose.lifecycle-management=start_only"
    )
    # Scheduled imports are off by default: a smoke test should scrape only the source
    # under test, not every source whose 24h interval happens to be due (ADR-007 ethics).
    $scheduling || props+=("--app.scheduling.enabled=false")

    log "Starting importer (scheduling=$scheduling) → $LOG_FILE"
    (cd "$REPO_ROOT" && nohup ./gradlew :events-importer:bootRun "--args=${props[*]}" \
        >"$LOG_FILE" 2>&1 &
    echo $! >"$PID_FILE")

    for _ in $(seq 1 180); do
        if importer_healthy; then
            log "Importer healthy at $HOST"
            return 0
        fi
        if grep -qE 'BUILD FAILED|APPLICATION FAILED TO START' "$LOG_FILE" 2>/dev/null; then
            tail -40 "$LOG_FILE" >&2
            die "Importer failed to start — see $LOG_FILE"
        fi
        sleep 1
    done
    tail -40 "$LOG_FILE" >&2
    die "Importer did not become healthy within 180s — see $LOG_FILE"
}

cmd_down() {
    if [[ -f "$PID_FILE" ]]; then
        kill "$(cat "$PID_FILE")" 2>/dev/null || true
        rm -f "$PID_FILE"
    fi
    # bootRun forks the app JVM, so the Gradle client PID alone is not enough.
    local pids
    pids="$(lsof -ti "tcp:${IMPORTER_PORT}" 2>/dev/null || true)"
    [[ -n "$pids" ]] && kill $pids 2>/dev/null || true
    log "Importer stopped"

    if [[ "${1:-}" == "--db" ]]; then
        (cd "$REPO_ROOT" && docker compose down)
        log "Database stopped"
    fi
}

cmd_status() {
    if db_ready; then
        printf 'database : up (port %s, %s events, %s sources)\n' \
            "$DB_PORT" "$(query 'SELECT count(*) FROM event;' 2>/dev/null || echo '?')" \
            "$(query 'SELECT count(*) FROM event_source;' 2>/dev/null || echo '?')"
    else
        printf 'database : down\n'
    fi
    if importer_healthy; then
        printf 'importer : up (%s)\n' "$HOST"
    else
        printf 'importer : down\n'
    fi
}

cmd_seed_all() {
    need ijhttp
    importer_healthy || die "Importer is not running — run 'dev-env.sh up' first"
    log "Seeding all sources from http/importer/dev-seed.http (this scrapes every venue)"
    (cd "$REPO_ROOT/http" && ijhttp --env-file http-client.env.json --env local -L VERBOSE \
        importer/dev-seed.http)
}

cmd_seed_one() {
    need jq
    local venue_json="${1:?usage: seed-one <venue.json> <source.json>}"
    local source_json="${2:?usage: seed-one <venue.json> <source.json>}"
    importer_healthy || die "Importer is not running — run 'dev-env.sh up' first"

    local venue_response venue_id
    venue_response="$(curl -sS -X POST "$HOST/api/admin/venues" \
        -H 'Content-Type: application/json' --data-binary "@$venue_json")"
    venue_id="$(jq -r '.id // empty' <<<"$venue_response")"

    if [[ -z "$venue_id" ]]; then
        # 409 on re-runs is expected — fall back to the existing venue by name.
        local name
        name="$(jq -r '.name' "$venue_json")"
        venue_id="$(query "SELECT id FROM venue WHERE name = $(printf "'%s'" "${name//\'/\'\'}") LIMIT 1;")"
        [[ -n "$venue_id" ]] || die "Could not create or find venue: $venue_response"
        warn "Venue already existed — reusing id $venue_id"
    fi
    log "Venue id $venue_id"

    local source_response slug
    source_response="$(jq --argjson vid "$venue_id" '.venueId = $vid' "$source_json" |
        curl -sS -X POST "$HOST/api/admin/event-sources" \
            -H 'Content-Type: application/json' --data-binary @-)"
    slug="$(jq -r '.slug // empty' <<<"$source_response")"

    if [[ -z "$slug" ]]; then
        local source_type
        source_type="$(jq -r '.sourceType' "$source_json")"
        slug="$(query "SELECT slug FROM event_source WHERE source_type = $(printf "'%s'" "$source_type") LIMIT 1;")"
        [[ -n "$slug" ]] || die "Could not create or find event source: $source_response"
        warn "Event source already existed — reusing slug $slug"
    fi
    log "Event source slug: $slug"
    printf '%s\n' "$slug"
}

cmd_import() {
    local slug="${1:?usage: import <slug> [timeout-seconds]}"
    local timeout="${2:-180}"
    importer_healthy || die "Importer is not running — run 'dev-env.sh up' first"

    local code
    code="$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$HOST/api/admin/event-sources/$slug/import")"
    [[ "$code" == "202" ]] || die "Import trigger returned HTTP $code for '$slug'"
    log "Import triggered for '$slug' (202) — polling…"

    local body status
    for _ in $(seq 1 "$timeout"); do
        body="$(curl -sS "$HOST/api/admin/event-sources/$slug")"
        status="$(jq -r '.status' <<<"$body")"
        if [[ "$status" != "RUNNING" && "$status" != "PENDING" ]]; then
            jq '{slug, status, lastEventCount, lastImportAt, lastError}' <<<"$body"
            [[ "$status" == "SUCCESS" ]] || die "Import finished with status $status"
            return 0
        fi
        sleep 1
    done
    die "Import for '$slug' still RUNNING after ${timeout}s"
}

cmd_snapshot() {
    local out="${1:-$RUN_DIR/snapshot.tsv}"
    mkdir -p "$(dirname "$out")"
    run_psql -qAtF $'\t' -c "SET search_path TO events;
        SELECT es.slug, count(e.id)
        FROM event_source es LEFT JOIN event e ON e.event_source_id = es.id
        GROUP BY es.slug ORDER BY es.slug;" >"$out"
    log "Snapshot written to $out ($(wc -l <"$out" | tr -d ' ') sources)"
}

cmd_diff_snapshot() {
    local before="${1:?usage: diff-snapshot <before.tsv> <after.tsv>}"
    local after="${2:?usage: diff-snapshot <before.tsv> <after.tsv>}"
    mkdir -p "$RUN_DIR"
    awk -F'\t' '
        NR == FNR { was[$1] = $2; next }
        { now[$1] = $2 }
        END {
            for (s in was) {
                if (s in now) {
                    delta = now[s] - was[s]
                    marker = delta < 0 ? "REGRESSION" : (delta > 0 ? "grew" : "same")
                    printf "%-28s %6d -> %-6d %+6d  %s\n", s, was[s], now[s], delta, marker
                } else {
                    printf "%-28s %6d -> %-6s %+6d  GONE\n", s, was[s], "-", -was[s]
                }
            }
            for (s in now) if (!(s in was)) printf "%-28s %6s -> %-6d %+6d  new\n", s, "-", now[s], now[s]
        }
    ' "$before" "$after" | sort >"$RUN_DIR/diff.txt"
    cat "$RUN_DIR/diff.txt"
    printf '\n%s source(s) lost events\n' "$(grep -c 'REGRESSION\|GONE' "$RUN_DIR/diff.txt" || true)"
}

cmd_check() {
    local slug="${1:?usage: check <slug>}"

    run_psql -v "slug=$slug" -f - <<'SQL'
SET search_path TO events;
\echo '--- summary ---'
SELECT es.slug, es.source_type, es.status, es.last_event_count,
       count(e.id) AS events_in_db,
       min(e.event_date) AS first_date, max(e.event_date) AS last_date,
       count(*) FILTER (WHERE e.event_date < current_date) AS past_events
FROM event_source es LEFT JOIN event e ON e.event_source_id = es.id
WHERE es.slug = :'slug'
GROUP BY es.slug, es.source_type, es.status, es.last_event_count;

\echo '--- field coverage (missing / total) ---'
SELECT count(*) AS total,
       count(*) FILTER (WHERE e.start_time IS NULL) AS no_start_time,
       count(*) FILTER (WHERE e.doors_time IS NULL) AS no_doors_time,
       count(*) FILTER (WHERE e.image_url IS NULL) AS no_image,
       count(*) FILTER (WHERE e.ticket_url IS NULL) AS no_ticket_url,
       count(*) FILTER (WHERE e.source_url IS NULL) AS no_source_url,
       count(*) FILTER (WHERE e.description IS NULL OR btrim(e.description) = '') AS no_description,
       count(*) FILTER (WHERE e.genre IS NULL) AS no_genre,
       count(*) FILTER (WHERE e.price_presale IS NULL AND e.price_box_office IS NULL
                          AND e.price_note IS NULL AND NOT e.free) AS no_price_info,
       count(*) FILTER (WHERE e.event_type = 'OTHER') AS type_other,
       count(*) FILTER (WHERE NOT EXISTS (SELECT 1 FROM event_artist ea WHERE ea.event_id = e.id)) AS no_artists
FROM event e JOIN event_source es ON es.id = e.event_source_id
WHERE es.slug = :'slug';

\echo '--- types & statuses ---'
SELECT e.event_type, e.status, count(*)
FROM event e JOIN event_source es ON es.id = e.event_source_id
WHERE es.slug = :'slug'
GROUP BY e.event_type, e.status ORDER BY count(*) DESC;

\echo '--- suspicious rows (placeholder titles, duplicate title+date) ---'
SELECT e.id, e.event_date, e.title
FROM event e JOIN event_source es ON es.id = e.event_source_id
WHERE es.slug = :'slug'
  AND (btrim(e.title) = '' OR upper(e.title) IN ('TBA', 'N.N.', 'N/A', 'NULL', '-')
       OR EXISTS (SELECT 1 FROM event d WHERE d.event_source_id = e.event_source_id
                    AND d.event_date = e.event_date AND lower(d.title) = lower(e.title) AND d.id <> e.id))
ORDER BY e.event_date LIMIT 20;

\echo '--- sample (5 earliest upcoming) ---'
SELECT e.event_date, e.doors_time, e.start_time, e.event_type, e.status, e.title,
       e.price_presale, e.price_box_office, e.free, e.sold_out, e.genre,
       (SELECT string_agg(a.name, ', ' ORDER BY ea.billing_order)
        FROM event_artist ea JOIN artist a ON a.id = ea.artist_id
        WHERE ea.event_id = e.id) AS artists,
       e.source_id
FROM event e JOIN event_source es ON es.id = e.event_source_id
WHERE es.slug = :'slug' AND e.event_date >= current_date
ORDER BY e.event_date LIMIT 5;
SQL
}

cmd_psql() { run_psql -c "SET search_path TO events; ${1:?usage: psql <sql>}"; }

# ---------------------------------------------------------------- dispatch

case "${1:-}" in
    db-reset) shift && cmd_db_reset "$@" ;;
    up) shift && cmd_up "$@" ;;
    down) shift && cmd_down "$@" ;;
    status) shift && cmd_status "$@" ;;
    seed-all) shift && cmd_seed_all "$@" ;;
    seed-one) shift && cmd_seed_one "$@" ;;
    import) shift && cmd_import "$@" ;;
    snapshot) shift && cmd_snapshot "$@" ;;
    diff-snapshot) shift && cmd_diff_snapshot "$@" ;;
    check) shift && cmd_check "$@" ;;
    psql) shift && cmd_psql "$@" ;;
    *)
        sed -n '3,28p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
        exit 1
        ;;
esac
