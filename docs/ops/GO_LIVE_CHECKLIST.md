# The go-live checklist

Read this once, in order, on launch day. It assumes you are tired.

Every line names the issue or the evidence that satisfies it. A line is done when it carries a
**date**, not when it looks true. The date is the point.

## The short version

```sh
# 1. DNS first. This publishes the apex and removes prod-check in one apply.
#    Edit publish_dns to true in infra/environments/production/variables.tf, then:
cd infra/environments/production && tofu plan -out=golive.tfplan     # READ IT

# 2. Immediately after, the chart must serve the apex instead of the rehearsal name.
#    Merge the deploy change, then stop waiting for the ten-minute poll:
flux --context event-junkie-production reconcile source oci event-junkie -n flux-system
```

**Both changes, or neither.** Section 0 explains the gap between them.

## 0 · The launch itself

Going live is **four changes in four places**. The first two serve the site. The last two stop the monitoring from
alarming about it.

| #   | Change                                                                      | Where                                          |
| --- | --------------------------------------------------------------------------- | ---------------------------------------------- |
| 1   | `publish_dns` from `false` to `true`, then apply                            | `infra/environments/production/variables.tf`   |
| 2   | `ingress.host` back to the apex, `redirectHosts` restored, `noindex: false` | `deploy/clusters/production/helm-release.yaml` |
| 3   | **Delete the `SITE_URL` repository variable**                               | GitHub → Settings → Variables                  |
| 4   | **Point the Better Stack monitor at the apex**                              | the Better Stack console, monitor `4876693`    |

**Changes 3 and 4 are not tidying, and forgetting them alarms you on launch day.** `prod-check.event-junkie.de` stops
resolving the moment change 1 applies. The daily probe then fails against a name that is gone and pings
healthchecks.io `/fail`. The Better Stack monitor reports the site down while it is up. Two false alarms, in the hour
you least want them.

Change 3 is a **deletion**, not an edit. `site-probe.yml` falls back to the apex when the variable is absent, and that
is the value that should survive somebody forgetting this page exists.

`scripts/cluster-assertions.sh` fails the build if you do half of change 2. It ties `noindex` to the
hostname. The apex with `noindex` on is an invisible launch. The rehearsal host without it is an
unfinished site in Google.

### The gap you cannot close

`publish_dns` **swaps** rather than adds. `prod-check` disappears in the same apply that publishes the
apex. So no instant exists where both names resolve.

| Order                   | What breaks, and for how long                                                                                                          |
| ----------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| **DNS first** (do this) | The apex resolves. The Ingress still names `prod-check`. Traefik answers 404 until Flux reconciles. Minutes, and you control it.       |
| Deploy first            | The Ingress names the apex, which does not resolve yet. No certificate can issue. DNS propagation controls the length, and you do not. |

Do not publish both names for one apply to avoid this. `variables.tf` rejects that. Two lists that
could both be published is how a temporary record becomes permanent.

### Read the plan before you apply

Any edit under `infra/modules/environment/cloud-init/` since the last apply replaces **both nodes**.
`user_data` is a force-new attribute. A comment change is enough.

Expect the apply to touch `hcloud_zone_rrset` only. Stop if a server appears.

## 1 · What must be true first

### Platform

| Done       | Item                                    | Evidence                                             |
| ---------- | --------------------------------------- | ---------------------------------------------------- |
| 2026-08-21 | Production applied                      | #560                                                 |
| 2026-08-30 | Flux reconciles production              | `flux get all -A`                                    |
| 2026-08-30 | A real certificate has issued           | Let's Encrypt `CN=YR1`, not `(STAGING) Pretend Pear` |
| 2026-08-30 | The site answers over TLS from outside  | `curl https://prod-check.event-junkie.de/`           |
| 2026-08-30 | #813 patched on the database node       | `ss -lntp` names the private address                 |
|            | `tofu plan` shows no server replacement | Section 0                                            |

### Backups and recovery

| Done       | Item                                   | Evidence                        |
| ---------- | -------------------------------------- | ------------------------------- |
| 2026-08-30 | `walg check` passes on production      | `ok: newest …, disk 1%`         |
| 2026-08-30 | Base backups run nightly               | `walg-basebackup.timer`         |
| 2026-08-21 | The dead-man's switch reaches a human  | `HEALTHCHECKS.md` drill log     |
|            | **A restore drill against production** | #862, `RESTORE_RUNBOOK.md` §4–5 |

The restore drill is the line most easily nodded through. `infra/AGENTS.md` calls it not optional
before go-live. The drill covers staging only, so far.

### Monitoring and alerting

| Done       | Item                                                         | Evidence                                        |
| ---------- | ------------------------------------------------------------ | ----------------------------------------------- |
| 2026-08-21 | `walg-production` fires                                      | drill log                                       |
| 2026-08-30 | Production records its deploys                               | #872                                            |
| 2026-08-31 | A decision on how the site is watched from outside           | ADR-021                                         |
| 2026-08-31 | **The Better Stack monitor exists and polls production**     | ADR-021, HEALTHCHECKS.md                        |
| 2026-08-31 | That monitor proven by inducing a failure                    | HEALTHCHECKS.md drill log                       |
|            | The monitor and `SITE_URL` both name the apex                | Section 0, changes 3 and 4                      |
|            | **Decide whether to publish an uptime badge** in `README.md` | HEALTHCHECKS.md § It is measured, not published |
| 2026-09-06 | Decide how visitors and traffic are counted                  | #1126 — page loads, nothing new                 |
| 2026-08-31 | **Production has any in-cluster monitoring**                 | #880, and the dashboard push below              |
|            | Alerts reach a person                                        | #877                                            |
| 2026-08-31 | An alert proven by breaking something on prod                | #285                                            |

**Production has its own observability now** (#880, closed). It runs OpenObserve, the collector agent and gateway,
the OTel operator and `postgres-exporter`. It carries all twelve alert rules. So the external layer below is no
longer the only thing watching production. That layer still gives one thing an in-cluster stack cannot: a view from
**outside** the cluster.

**Two layers are pushed by hand, and one of them was silently missing for eleven days.** OpenObserve dashboards and
alert rules are API objects rather than Kubernetes ones, so Flux cannot reconcile them. `OPENOBSERVE.md` calls this
the seam where GitOps stops. Production had the alert rules and **not** the dashboard, from #880 until 2026-08-31.
Nothing reported it. A missing dashboard is not an error, it is an empty list. Someone opened the console and asked
why it was empty.

**So after any production rebuild, and after any change to either file, run both.** `EJ_NODE` selects the cluster
and defaults to **staging**, so omitting it succeeds against the wrong one and says nothing:

```sh
cd deploy/dashboards && EJ_NODE=ops@10.10.0.1 ./apply.sh --diff   # is production running this file at all?
cd deploy/alerts     && EJ_NODE=ops@10.10.0.1 ./apply.sh --diff   # the same question, for the rules
```

`--diff` answers "is it there", `--check` answers "do its queries return data", and neither substitutes for the
other. Drop the flag to push. Both are idempotent — the dashboard import matches on title and replaces.

**The external layer works, and a drill proved it on 2026-08-31.** `site-production` went live on 2026-08-30 and
alarmed within hours with the site healthy. GitHub delivers about 8% of a 15-minute cron, at a median interval of 129
minutes. #889 replaced the shape rather than the numbers. A Better Stack monitor polls every three minutes and alerts
in about six. The probe stays as a daily dead-man's switch at `24h`/`24h`, and it asserts the monitor's own settings
against the repository once a day. ADR-021 has the reasoning.

**One hop was genuinely in doubt and is now measured.** The free plan carries no on-call schedule, so nothing
established that an alert routed to a human. The drill changed the monitor's keyword to a string the site does not
serve, and the e-mail arrived. **The row _Alerts reach a person_ above still refers to the in-cluster path** (#877,
OpenObserve to Signal). That is a different chain, and it stays unbuilt.

### Content and data

| Done       | Item                                                                                                      | Evidence |
| ---------- | --------------------------------------------------------------------------------------------------------- | -------- |
| 2026-08-30 | Event sources registered **and enabled**, so the site has content                                         | #876     |
| 2026-08-30 | Venue addresses, districts and coordinates audited                                                        | #329     |
| 2026-09-07 | Venue descriptions read against the venue they describe                                                   | #1124    |
|            | Venue descriptions proof-read once more **by the maintainer**, after #1124 and before the flip            | #1124    |
|            | Every page read in both languages **by the maintainer**, as a reader — About and the legal texts included | #280     |
| 2026-08-31 | Images served from our own cache, not hotlinked                                                           | #843     |

**Production serves the full catalogue.** All 86 sources are registered, enabled and carry their licence
verdicts, and the importer runs on schedule (#876, #285). Two venues forbid their descriptions and images, and
that was recorded before the sources were enabled. Images serve from the cache (#843), not from venue sites.

**#329 audited district, address and coordinates on 2026-08-30**, and corrected 30 venue records (#986). A wrong
coordinate puts a pin in the wrong place, and it drops the venue out of a radius search without saying so. The
second failure is the quiet one, and it is the one the audit looked for.

**The descriptions are hand-written prose. Issue #1124 read each one against the venue itself on
2026-09-07.** Issue #986 read them against the address only. It found two that were wrong. Both
repeated something the row's address said. A corrected address does not correct the sentence that
quotes it. Issue #1124 read all 86 against the venue's own site, Resident Advisor, Google's category and
Wikipedia. 65 stood. 21 were corrected or rewritten, in the seed and in `V016`. One more sentence
was correct in the seed and stale on both clusters, and `V016` carries it over. The address was
rarely the fault. The faults were a building the venue never occupied, or a station that is not the
nearest. Or a genre the house does not play, or a cinema that screens films again.
The pass was made from sources, not from knowing the venues. The unsettled facts are listed on pull
request #1196. The maintainer reads all 86 once more before the flip, as a reader who knows the city,
and that read is the row above.

**The prose is two independent documents in two languages.** The key-parity test proves every German
key exists. It cannot tell you a translation is good, or that a claim is still true.

### Legal

| Done       | Item                                 | Evidence |
| ---------- | ------------------------------------ | -------- |
| 2026-09-02 | The privacy notice matches what runs | #278     |
|            | Legal review of the German notice    | #279     |
| 2026-08-30 | Copyright status per source          | #283     |

### SEO

| Done | Item                          | Evidence  |
| ---- | ----------------------------- | --------- |
|      | `noindex` off, apex served    | Section 0 |
|      | Search Console set up         | #288      |
|      | Sitemap and hreflang accepted | #289      |
|      | Rich results tested           | #290      |
|      | Link previews checked         | #291      |
|      | Indexing watched afterwards   | #293      |

### Security

| Done       | Item                                   | Evidence             |
| ---------- | -------------------------------------- | -------------------- |
| 2026-09-07 | CSP enforced, not report-only          | #854, and #843 first |
| 2026-09-03 | Rate limiting on the public API        | #268                 |
|            | The Security tab is at zero or triaged | `/security-triage`   |

### Product

| Done | Item                                                                                                                                 | Evidence    |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------ | ----------- |
|      | The beta badge decision                                                                                                              | #295        |
|      | Maintenance mode, if wanted first                                                                                                    | #296        |
|      | The README says the site is live                                                                                                     | `README.md` |
|      | The repo health files proof-read **by the maintainer** — README, CONTRIBUTING, SUPPORT, SECURITY, the Code of Conduct, the templates | #281        |

**The README claims a status in two places, and both are wrong the moment the apex serves.** The badge near the top
reads `Status-In Development`, and § Status opens with "In development — deployed, but not public yet." Change the
badge to `Status-Live-brightgreen`, and rewrite the section around what production serves. One without the other
leaves the page contradicting itself.

## 2 · What is deliberately not here

Items in `v1.0 — Go-live` with the `needs-deployment` label wait on a live origin. They are not
blocked on effort. Listing them makes a checklist look permanently unfinished.

**Four of them can run early**, because production serves a real hostname over a real certificate:
#290, #291, #292 and #298. Point them at `prod-check`.

Two cannot. #288 and #293 need the real domain.

## 3 · Going dark again

Revert both changes from Section 0. The apex stops resolving within one TTL, which is 300 seconds.

Two things to know before you need them:

- **HSTS `preload` is off.** It is the one setting here that is hard to undo. Removal takes months to
  reach browsers. Leave it off until the domain is settled.
- **`max-age` is one year, with `includeSubDomains`.** A browser that loaded the site once refuses
  plain HTTP for a year. Losing TLS is therefore an outage, not a degradation. That is the argument
  for the certificate being automatic.

## Related

- [CLUSTER_BOOTSTRAP.md](CLUSTER_BOOTSTRAP.md) — standing an environment up, and §12 on running dark
- [CLUSTER_ACCESS.md](CLUSTER_ACCESS.md) — reaching production day to day
- [RELEASING.md](RELEASING.md) — how a commit becomes a running deployment
- [HEALTHCHECKS.md](HEALTHCHECKS.md) — the switches that watch from outside
