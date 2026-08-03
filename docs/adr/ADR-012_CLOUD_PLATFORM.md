# ADR-012: Cloud Platform & Hosting

## Status

Proposed (2026-08-03)

> Resolves the first item of the **🔴 Now (path to go-live)** backlog in [TODO.md](../../TODO.md) — *"Choose a cloud platform / runtime environment"*. This ADR
> picks
> the **platform**; the Terraform/OpenTofu layout, the Helm chart, and the CI/CD workflows are follow-up items that depend on it. All prices in this document
> were checked on **2026-08-03** and must be re-verified before the money is actually committed — cloud list prices moved twice in 2026 already (see
> [Hetzner's 15 June 2026 adjustment](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/)).

## Context

Event Junkie is a Berlin music-events guide, **not yet deployed anywhere** (see [README §Project Status](../../README.md)). The decision is being made before
the first deploy, which means we are choosing the platform we will write Terraform and a Helm chart *against* — switching later costs real work, so it is worth
recording why.

### What actually has to run

Four deployables, derived from the modules in this repo:

| Component         | Shape                                                             | Runtime demand                                                     |
|-------------------|-------------------------------------------------------------------|--------------------------------------------------------------------|
| `events-bff`      | Spring Boot 4 / WebFlux / R2DBC, read-only public API (port 8080) | Stateless, horizontally scalable, JVM ≈ 512 MB–1 GB heap           |
| `events-importer` | Spring Boot 4 / WebFlux / R2DBC + admin API (port 8081)           | **Always-on, effectively single-instance** — see below             |
| `events-frontend` | Vue 3 + Vite **SPA** — `npm run build` emits a static `dist/`     | No server runtime of its own; static files + a router fallback     |
| PostgreSQL 18     | The only stateful component; owns all event/venue/artist data     | ~2 vCPU / 4 GB, tens of GB, needs backups + point-in-time recovery |

Three properties of `events-importer` constrain the platform choice more than anything else:

1. **It is a scheduler, not a request handler.** [ADR-008](ADR-008_IMPORT_JOB_SCHEDULING.md) runs a `@Scheduled(fixedDelay = 60s)` tick that queries
   `event_source` for due venues. A wall-clock tick every minute is incompatible with request-driven scale-to-zero (Cloud Run request-billing, Lambda, Container
   Apps scale-to-zero) — the tick simply does not fire when there are no requests.
2. **It must be exactly one instance.** ADR-008 is explicit that the `status = 'RUNNING'` exclusion "is not a true lock" and that multi-instance operation needs
   `SELECT … FOR UPDATE SKIP LOCKED` first. Any platform whose default deploy strategy runs old and new replicas concurrently needs `maxSurge: 0` / `Recreate`
   semantics, or we accept the idempotency argument in ADR-008.
3. **Its jobs are long.** A heavy importer (Badehaus) fetches ~90 throttled detail pages and runs **over a minute**; the staleness guard is 30 minutes. Request
   timeouts on serverless container platforms (Cloud Run caps at 60 min, App Runner at 120 s per request) are a real constraint — mitigated here because ADR-008
   already made manual triggers fire-and-forget, but it rules out anything with a short hard ceiling.

Also relevant: [ADR-005](ADR-005_MIGRATIONS_OWNED_BY_IMPORTER.md) puts Flyway in the importer, so the importer must reach the database before the BFF serves
traffic, and the importer's admin API **must not be publicly reachable**.

### Non-functional context

- **Scale is small and will stay small for a while.** Eight venues live today, ~40 planned; the public API is read-mostly and cacheable. Realistic launch
  traffic is well under 100 GB egress/month.
- **One developer.** Ops time is the scarcest resource, scarcer than euros — but euros are not free either, and a €200/month bill for a pre-revenue side project
  is its own kind of pressure.
- **A separate staging stage is required** (TODO 🔴 Now). On every platform with per-hour floors this roughly doubles the bill, so "cost of the second
  environment" is a first-class criterion, not an afterthought.
- **EU / Germany hosting is a hard requirement.** The product is German (`event-junkie.de`), the users are in Berlin, and the data includes third-party website
  content plus — once accounts land (TODO 🟠 Next) — personal data under GDPR.
- **Existing skills**: Docker, Kubernetes, Helm, Terraform. TODO already plans a Helm chart and to exercise it locally on k3d/kind, so a Kubernetes-shaped
  target reuses work that is going to happen anyway.

### Criteria

| #  | Criterion                           | Weight | Why it matters here                                                                                         |
|----|-------------------------------------|--------|-------------------------------------------------------------------------------------------------------------|
| 1  | Fit for always-on JVM + scheduler   | High   | ADR-008's tick rules out scale-to-zero for the importer; JVM cold starts (2–5 s) hurt request-billed models |
| 2  | Managed PostgreSQL (backup/PITR)    | High   | The only stateful thing we own; losing it loses everything. Self-managing it is the main hidden cost        |
| 3  | Total cost at *this* scale          | High   | Pre-revenue. Per-hour floors (LB, NAT, DB) dominate — not per-request pricing                               |
| 4  | Cost of a second (staging) stage    | High   | Explicitly required; on hyperscalers it is nearly a second full bill                                        |
| 5  | EU / German data residency          | High   | GDPR, German users, German domain; DPA/AVV and jurisdiction matter                                          |
| 6  | Ops burden                          | High   | Who patches the OS, the DB, the K8s control plane — paid in evenings, not invoices                          |
| 7  | Reuse of Docker/K8s/Helm/Terraform  | Med    | Skills already held; a Helm chart is already on the backlog                                                 |
| 8  | Terraform/OpenTofu provider quality | Med    | IaC is the next backlog item after this one                                                                 |
| 9  | CI/CD integration (GitHub Actions)  | Med    | Build workflows already exist; want OIDC over long-lived keys                                               |
| 10 | Egress predictability               | Med    | Metered egress is the classic source of bill shock                                                          |
| 11 | Observability included              | Med    | TODO wants monitoring/alerting/dashboards; "included" beats "assemble"                                      |
| 12 | Lock-in / exit cost                 | Med    | A managed-container + managed-Postgres app is portable; proprietary glue is not                             |
| 13 | Scaling headroom                    | Low    | Nothing here needs to scale for a long time, but the door shouldn't be nailed shut                          |
| 14 | Career / CV signal                  | Low    | Real, but it is a tie-breaker, not a driver                                                                 |

### Which platforms are actually popular?

Worth stating plainly, because "popular" and "right for this" are different questions. Per Synergy Research for 2026, the global cloud-infrastructure market is
roughly **AWS ~28–31 %, Microsoft Azure ~21–25 %, Google Cloud ~11–14 %** — together about two-thirds of the market. Everything else (Alibaba, Oracle, IBM,
DigitalOcean, Hetzner, OVH, Scaleway, and the PaaS layer of Fly/Render/Railway/Vercel) shares the remaining third.

So yes — **AWS is the market leader and the enterprise default.** That is a fact about procurement, hiring, and breadth of catalogue. It is not, by itself, an
argument that AWS is the right home for two containers and a small Postgres, and this ADR distinguishes the two.

---

## Candidate options

### Option A — Hetzner Cloud (Nuremberg / Falkenstein) + k3s

German company (Gunzenhausen), German data centres, ISO 27001, standard AVV. Compute is plain VMs; we run **k3s** on them with the official
`hcloud-cloud-controller-manager` and CSI driver, deploy with the Helm chart TODO already plans, provision with the `hetznercloud/hcloud` Terraform provider.
PostgreSQL runs on its **own VM** (not inside the cluster) with `wal-g`/`pgBackRest` streaming WAL to Hetzner Object Storage or a Storage Box.

- **Pros**: By far the cheapest — a factor of 5–6 versus AWS for this workload. 20 TB egress **included** per server, so no bandwidth bill shock. Cleanest GDPR
  story of any candidate (German entity, German jurisdiction, no CLOUD Act exposure). Every existing skill applies directly. A second (staging) environment
  costs
  ~€7/month, so the required staging stage is genuinely affordable. Always-on is the default, so ADR-008's scheduler needs no workarounds.
- **Cons**: **No managed PostgreSQL** — backups, PITR, restore drills, minor-version upgrades and disk-full are ours. We also own the k3s control plane, OS
  patching, and node upgrades. No managed observability (Prometheus/Grafana or an external SaaS is on us). Single-region; DDoS protection is basic (Hetzner has
  volumetric protection, but no WAF/rate-limiting layer). Support is email-only.
- **Hetzner raised cloud prices on 15 June 2026** (CX23 €3.99 → €5.49; the dedicated CCX/CPX lines up to 2–3×). Still the cheapest by a wide margin, but the
  "Hetzner never raises prices" assumption is now dead and should not be planned around.

### Option B — AWS (ECS Fargate + RDS + ALB + CloudFront/S3), `eu-central-1`

The market-leading, maximal-optionality choice.

- **Pros**: Largest service catalogue by far; if we later want managed Elasticsearch/OpenSearch (README lists it as "maybe later"), SQS, EventBridge, or Cognito
  for the planned auth, it is all one `terraform apply` away. Best-in-class Terraform provider and by far the most documentation, examples, and hiring signal.
  RDS is a genuinely excellent managed Postgres (automated backups, PITR, one-click restore, Multi-AZ when we want it). GitHub Actions OIDC is first-class.
  Frankfurt (`eu-central-1`) satisfies EU residency, and since **15 January 2026** the **AWS European Sovereign Cloud** is generally available with its first
  region in Brandenburg, Germany — separate German legal entity, EU-resident staff, independent IAM/billing/DNS — which is the strongest answer available to the
  CLOUD Act concern, at a price premium and with a smaller service set.
- **Cons**: **The fixed floor is the problem, not the variable cost.** An ALB bills ~$24/month whether or not anyone visits; a NAT Gateway adds ~$38/month
  before a byte moves; RDS bills per hour; Fargate has no scale-to-zero. None of that is elastic at our size — we would pay for capacity nobody uses. ECS +
  Fargate is also markedly more assembly-required than Cloud Run or Container Apps (task definitions, target groups, listener rules, execution roles, log
  groups), so it is the most Terraform to write and own. Egress is metered at ~$0.09/GB, plus a further ~$0.052/GB if it traverses NAT. Staging nearly doubles
  the bill.

### Option C — Google Cloud (Cloud Run + Cloud SQL), `europe-west3` (Frankfurt)

The best hyperscaler *ergonomics* for exactly this workload shape.

- **Pros**: Cloud Run is the nicest managed-container experience of the three — you push an image, you get an HTTPS endpoint with a managed certificate, and
  **there is no load-balancer line item**, which removes ~$24/month of AWS's floor outright. Scale-to-zero makes the **staging** environment nearly free, which
  directly serves criterion #4. Cloud SQL is a solid managed Postgres. Cloud Build/Artifact Registry and GitHub OIDC integrate cleanly.
- **Cons**: Cloud Run's request-based billing is the wrong model for `events-importer` — the scheduler needs `min-instances: 1` with instance-based billing,
  which is just "an always-on container with extra steps" and prices out close to Fargate. Cloud SQL has awkward small-instance economics (a public IPv4 alone
  is
  ~$9.57/month idle; private IP needs Direct VPC egress or a connector). Google's product-deprecation reputation is a real, if often overstated, planning risk.
  Same CLOUD Act posture as AWS, and no German-jurisdiction sovereign offering equivalent to AWS's ESC.

### Option D — Azure (Container Apps + PostgreSQL Flexible Server), Germany West Central

- **Pros**: Container Apps has Cloud Run-like ergonomics with free built-in ingress, and it is KEDA-based so cron-style scaling is native. Azure Database for
  PostgreSQL **Flexible Server** has the friendliest small-instance pricing of the three hyperscalers (B-series burstable, ~$13–15/month for B1ms). Germany West
  Central is a full region.
- **Cons**: The smallest third-party ecosystem of the three for this stack; Terraform's `azurerm` provider is good but the resource model is chattier. No
  compelling advantage over Option C for us, and the same jurisdictional posture. Included mainly for completeness.

### Option E — PaaS: Fly.io / Render / Railway

- **Pros**: Lowest ops burden of anything here. Push a Dockerfile, get a URL, TLS, and a managed Postgres. Render has a Frankfurt region, Fly has `fra`, Railway
  has EU West (Amsterdam). All support always-on containers, so ADR-008's scheduler is unproblematic.
- **Cons**: All three are **US companies** — acceptable under SCCs, but weaker than a German provider against the stated Germany preference. Prices land between
  Hetzner and the hyperscalers with far less headroom than either. Their managed Postgres offerings are the least battle-tested part of each product, and the DB
  is the thing we least want to be adventurous about. Meaningful lock-in in the deploy model, and **none of the Docker/K8s/Helm/Terraform skills apply** — the
  whole point of a PaaS is that it hides that layer.

### Option F — Managed Kubernetes at a hyperscaler (EKS / GKE / AKS)

Evaluated and **rejected on price alone**: EKS and GKE both charge roughly **$0.10 per cluster-hour ≈ $73/month for the control plane before a single pod runs**
(GKE waives one zonal cluster; AKS's free tier has no uptime SLA). Add nodes, a load balancer, NAT, and a managed database and the floor is $150–250/month per
environment. That is the correct answer for a team running many services; it is indefensible for two containers. Note that this rejection is about *managed K8s
at hyperscaler prices*, not about Kubernetes — Option A uses Kubernetes, just with a control plane we run ourselves for ~€0.

### Option G — Hybrid: cheap EU compute + specialist managed Postgres

Hetzner (or a PaaS) for compute, paired with **Neon** (~$0.106/CU-hour on Launch, EU regions available), **Aiven** (Finnish/EU company, ~$60–80/month for a
production-grade small plan), or **Supabase Pro** (~$25/month) for the database.

- **Pros**: Removes the single biggest drawback of Option A — we stop owning Postgres backups and PITR — while keeping cheap compute. Neon's branching is a
  genuinely nice fit for a staging stage.
- **Cons**: Two vendors, two DPAs, two bills, and cross-provider network latency on every query, which matters for a WebFlux/R2DBC app doing chatty per-event
  upserts. Aiven is EU-owned; Neon and Supabase are US companies with EU regions.

---

## Comparison

Scores are for **this** workload at **this** scale, not in general.

| Criterion (weight)                     | A: Hetzner + k3s          | B: AWS Fargate           | C: GCP Cloud Run          | E: PaaS (Fly/Render/Railway) | F: Managed K8s          |
|----------------------------------------|---------------------------|--------------------------|---------------------------|------------------------------|-------------------------|
| Always-on JVM + scheduler fit (High)   | ✅ Native                 | ✅ Native                | 🟡 Needs `min-instances`  | ✅ Native                    | ✅ Native               |
| Managed PostgreSQL (High)              | ❌ Self-managed           | ✅ RDS, excellent        | ✅ Cloud SQL              | 🟡 Adequate, less proven     | ✅ (same as B/C)        |
| Cost at this scale (High)              | ✅ ~€25/mo                | ❌ ~$150/mo              | 🟡 ~$110/mo               | 🟡 ~$50–70/mo                | ❌ $150–250/mo          |
| Cost of staging stage (High)           | ✅ ~€7/mo                 | ❌ ~+$70/mo              | ✅ Scale-to-zero, ~$30    | 🟡 ~+$35/mo                  | ❌ Second control plane |
| EU / German residency (High)           | ✅ German entity + DCs    | 🟡 Frankfurt; ✅ via ESC | 🟡 Frankfurt, US entity   | 🟡 EU region, US entity      | 🟡 As B/C               |
| Ops burden (High)                      | ❌ OS + k3s + DB are ours | ✅ Low                   | ✅ Lowest of hyperscalers | ✅ Lowest overall            | 🟡 Nodes still ours     |
| Reuses Docker/K8s/Helm/Terraform (Med) | ✅ All of it              | 🟡 Docker + TF only      | 🟡 Docker + TF only       | ❌ Bypasses the whole layer  | ✅ All of it            |
| Terraform provider quality (Med)       | ✅ `hcloud`, solid        | ✅ Best in class         | ✅ Very good              | 🟡 Thin/uneven               | ✅ Very good            |
| GitHub Actions CI/CD (Med)             | 🟡 SSH/kubeconfig secret  | ✅ OIDC, first-class     | ✅ OIDC, first-class      | ✅ Native integrations       | ✅ OIDC                 |
| Egress predictability (Med)            | ✅ 20 TB included         | ❌ $0.09/GB + NAT        | 🟡 Metered                | 🟡 Metered                   | ❌ Metered              |
| Observability included (Med)           | ❌ Bring your own         | ✅ CloudWatch            | ✅ Cloud Ops              | 🟡 Basic dashboards          | ✅ Included             |
| Lock-in / exit cost (Med)              | ✅ Plain VMs + K8s        | 🟡 Moderate glue         | 🟡 Moderate glue          | ❌ Highest                   | ✅ Portable manifests   |
| Scaling headroom (Low)                 | 🟡 Manual, single region  | ✅ Effectively unlimited | ✅ Effectively unlimited  | 🟡 Bounded                   | ✅ High                 |
| Career / CV signal (Low)               | 🟡 Niche but respected    | ✅ Strongest             | ✅ Strong                 | ❌ Weak                      | ✅ Strong               |

---

## Pricing comparison

### Sizing assumption

One production stage: `events-bff` 0.5 vCPU / 1 GB · `events-importer` 0.5 vCPU / 1 GB · PostgreSQL 2 vCPU / 4 GB with 40 GB storage · static SPA · < 100 GB
egress/month. Plus one staging stage at roughly half that. All figures **as of 2026-08-03**, EU regions, list price, excl. VAT/credits.

### Option A — Hetzner Cloud (recommended)

| Item                                                 | Plan                         | € / month  |
|------------------------------------------------------|------------------------------|------------|
| k3s node — bff + importer + frontend + ingress       | CX33 (4 vCPU / 8 GB / 80 GB) | 8.49       |
| PostgreSQL VM (private network only, no public IPv4) | CX23 (2 vCPU / 4 GB / 40 GB) | 5.49       |
| Public IPv4 (k3s node only)                          | 1 ×                          | ~1.70      |
| Automated snapshots (20 % of server price)           | both servers                 | ~2.80      |
| Backup target for WAL + base backups                 | Storage Box BX11 (1 TB)      | ~3.81      |
| **Production subtotal**                              |                              | **~22.30** |
| Staging — everything on one node                     | CX23 + IPv4                  | ~7.20      |
| **Total (prod + staging)**                           |                              | **~29.50** |

Add a Hetzner Load Balancer (LB11, ~€7.49/month) only when a second k3s node arrives; until then the ingress binds the node IP directly.

### Option B — AWS `eu-central-1`

| Item                                                              | $ / month     |
|-------------------------------------------------------------------|---------------|
| Fargate — 2 tasks × (0.5 vCPU + 1 GB), x86 (ARM/Graviton ≈ −20 %) | ~40 (~32 ARM) |
| Application Load Balancer (hourly + minimum LCUs)                 | ~24           |
| RDS `db.t4g.small` Single-AZ + 40 GB gp3 + backups                | ~32           |
| NAT Gateway ($0.052/h in Frankfurt + $0.052/GB processed)         | ~40           |
| S3 + CloudFront for the SPA                                       | ~2            |
| Route 53, Secrets Manager, ECR, CloudWatch                        | ~12           |
| **Production subtotal**                                           | **~150**      |
| Without NAT (tasks in public subnets — a security trade-off)      | ~110          |
| Staging (~60 % of prod)                                           | ~70           |
| **Total (prod + staging)**                                        | **~180–220**  |

Note how the shape differs from Hetzner: **~$64 of that is load balancer + NAT Gateway**, two line items that do no application work and cannot be scaled down.

### Option C — Google Cloud `europe-west3`

| Item                                                                   | $ / month |
|------------------------------------------------------------------------|-----------|
| Cloud Run — importer, always on (`min-instances: 1`, instance billing) | ~29       |
| Cloud Run — BFF, `min-instances: 1` (JVM cold starts otherwise)        | ~29       |
| Cloud SQL — smallest shared-core + 40 GB SSD + public IPv4             | ~45       |
| Firebase Hosting / Cloud Storage + CDN for the SPA                     | ~1        |
| Artifact Registry, Secret Manager, Cloud Logging                       | ~5        |
| **Production subtotal**                                                | **~110**  |
| Staging (BFF scales to zero; smallest Cloud SQL)                       | ~30       |
| **Total (prod + staging)**                                             | **~140**  |

### Option E — PaaS

| Platform | Configuration                                                             | $ / month (prod) |
|----------|---------------------------------------------------------------------------|------------------|
| Fly.io   | 2 × shared-cpu-1x/1 GB (`fra`) + Managed Postgres + volumes               | ~50              |
| Railway  | 2 svc × (0.5 vCPU + 1 GB) @ $20/vCPU + $10/GB + Postgres + $20 Pro seat   | ~65              |
| Render   | 2 × Standard (1 CPU / 2 GB) @ $25 + Postgres Basic 1 GB; static site free | ~70              |

### Summary — what this application costs per month

| Platform                     | Production | Prod + staging | Multiple of cheapest |
|------------------------------|------------|----------------|----------------------|
| **Hetzner Cloud + k3s**      | **~€22**   | **~€30**       | 1×                   |
| Fly.io                       | ~$50       | ~$75           | ~2×                  |
| Railway                      | ~$65       | ~$95           | ~2.7×                |
| Render                       | ~$70       | ~$105          | ~3×                  |
| Google Cloud (Cloud Run)     | ~$110      | ~$140          | ~4×                  |
| AWS (Fargate)                | ~$150      | ~$200          | ~6×                  |
| EKS / GKE managed Kubernetes | ~$200      | ~$350          | ~10×                 |

Two honest caveats on this table. First, hyperscaler **free credits** distort year one — AWS and GCP both hand new accounts a few hundred dollars, which can
make the first 6–12 months look free and the thirteenth month look alarming; the table is steady-state. Second, the Hetzner number **excludes the labour** of
running PostgreSQL and k3s ourselves. If that is valued at even two hours a month, the gap to a PaaS narrows considerably — which is precisely the trade-off the
decision below turns on.

---

## Decision

**Proposed: Option A — Hetzner Cloud (Nuremberg/Falkenstein), k3s + Helm, provisioned with OpenTofu, PostgreSQL on a dedicated VM with `wal-g` PITR backups.**

Rationale, against the weighted criteria:

- **The Germany requirement is decisive and Hetzner satisfies it best.** It is a German company, in German data centres, under German jurisdiction, with a
  standard AVV — no SCCs, no CLOUD Act analysis, no sovereign-cloud premium. AWS's European Sovereign Cloud (GA since January 2026, Brandenburg) is the only
  candidate that matches this posture, and it costs more than regular AWS while offering fewer services.
- **Cost is not a rounding error at this scale — it is ~6× between the top and bottom of the table.** For a pre-revenue project, €30/month versus $200/month is
  the difference between "run staging and production properly" and "cut corners to keep the bill down". The saving directly funds the required staging stage,
  the domain, and an external uptime/monitoring service.
- **The workload wants exactly what a VM is.** `events-importer` is an always-on, single-instance, long-job scheduler (ADR-008). Every serverless pricing model
  in this comparison is optimised for the opposite shape, so we would pay a premium for elasticity the application cannot use.
- **The existing skills apply in full, and the Helm chart is on the backlog anyway.** Options B, C, and E all discard the Kubernetes/Helm layer; Option A is the
  only one where the planned "exercise the Helm chart on k3d/kind" work (TODO → Operations & Hardening) becomes the actual production deployment path.
- **Egress is included (20 TB/server).** For a public, image-heavy events site, metered egress is the most likely source of a surprise bill on any other option.

**The cost of this decision is that we own PostgreSQL and the k3s control plane.** That is the real trade, and it should not be glossed over: RDS and Cloud SQL
give automated backups, tested restores, PITR, and minor-version upgrades for free, and here we buy all of that with our own time. The mitigations below are
therefore **not optional** — they are the price of the decision.

### Deployment shape

```
                     Cloudflare (DNS, TLS, CDN, WAF/rate limiting — free plan)
                                        │
                            ┌───────────┴────────────┐
                            │   Hetzner CX33 (k3s)   │   Falkenstein / Nuremberg
                            │  ┌──────────────────┐  │
                            │  │ Traefik ingress  │  │
                            │  │   /      → web   │  │   nginx serving Vite dist/
                            │  │   /api   → bff   │  │   events-bff       (N replicas)
                            │  │  (admin: private)│  │   events-importer  (exactly 1)
                            │  └──────────────────┘  │
                            └───────────┬────────────┘
                                        │ private network (no public IP)
                            ┌───────────┴────────────┐
                            │   Hetzner CX23         │   PostgreSQL 18
                            │   + wal-g → Storage Box│   base backups + WAL, PITR
                            └────────────────────────┘
```

### Frontend hosting — containerise it, same origin as the API

`events-frontend` is a plain Vite SPA: `npm run build` produces a static `dist/`, and there is no SSR, no Nuxt, no server runtime. That means the *industry
default* would be a static host + CDN (Cloudflare Pages, Netlify, Vercel, S3+CloudFront) — for a generic SPA that is the right answer, and it is usually free.

**For this project we should still ship it as a Docker image** (multi-stage: `node` builds, `nginx`/`Caddy` serves `dist/`), deployed by the same Helm chart
behind the same ingress. The reasons are specific rather than dogmatic:

1. **Same origin removes CORS entirely.** Routing `/` → frontend and `/api` → BFF through one ingress means no preflight requests, no `Access-Control-*`
   configuration to keep in sync across three environments, and — importantly for the planned authentication work (TODO 🟠 Next) — session cookies are
   first-party, so `SameSite` and third-party-cookie restrictions stop being a problem. This alone justifies the choice.
2. **It keeps the Germany requirement intact.** Cloudflare Pages, Netlify, and Vercel are US companies serving from a global edge; every one of them adds a
   processor and a jurisdiction question that a container in Falkenstein does not.
3. **One pipeline, one rollback.** Same registry, same `helm upgrade`, same `helm rollback`, same environment promotion. A second deploy mechanism for one
   static bundle is not worth the split.
4. **The cost is negligible.** nginx serving a few MB of assets runs comfortably in 16–32 MB of RAM.

Three things to get right, because a containerised SPA is easy to ship subtly broken:

- **History-mode fallback.** vue-router uses HTML5 history mode, so nginx needs `try_files $uri $uri/ /index.html;` — without it, deep links and page refreshes
  return 404.
- **Cache headers.** Vite content-hashes everything under `/assets/`, so serve those with `Cache-Control: public, max-age=31536000, immutable` and serve
  `index.html` with `no-cache`. Get this backwards and users either never see deploys or re-download the bundle constantly.
- **Build-time config.** Vite inlines `import.meta.env.*` at build time, so a per-environment API URL would mean one image per environment. Avoid this by having
  the SPA call a **relative** `/api` path — the image then becomes environment-agnostic and the identical artifact promotes from staging to production, which is
  what we want anyway.

Put **Cloudflare in front** (free plan, proxied DNS) for TLS, edge caching of the static assets, and rate limiting / DDoS protection — which also makes progress
on the "Protect the public BFF API (rate limiting, DDoS)" backlog item. Note the residency nuance: Cloudflare terminates TLS at its edge, so if strictly
German-only processing is ever required, either drop Cloudflare's proxy mode or buy its EU data-localisation add-on.

### When to revisit

This decision should be **reopened**, not defended, if any of these become true:

- The project takes on **a team, paying customers, or a compliance obligation** (SOC 2, an enterprise customer's security review) — managed services and audit
  trails start earning their price.
- **Database operations become a recurring source of pain or fear** — the first restore that does not work is the signal. Cheapest fix is Option G: keep Hetzner
  compute, move Postgres to a managed EU provider (Aiven is EU-owned; Neon has EU regions).
- **Uptime requirements harden** past what a single-region, single-node k3s cluster can honestly promise.
- The roadmap pulls in **managed building blocks** — README lists Elasticsearch as "maybe later", and TODO lists Keycloak/auth. If we end up wanting managed
  search, managed identity, and managed queues, the hyperscaler discount on *integration effort* starts to outweigh the compute premium.

**If the recommendation is rejected**, the ranked fallbacks are: **Fly.io or Render** (if the real goal is to spend zero evenings on ops — roughly 2–3× the
cost, managed Postgres included, but it discards the Kubernetes/Helm work); then **GCP Cloud Run + Cloud SQL** (the best hyperscaler fit for this shape — no
load-balancer line item and free scale-to-zero staging); and **AWS** last for *this* stage of the project, but first the moment breadth of managed services or
enterprise credibility becomes the binding constraint.

### On "AWS is the most flexible and the standard, isn't it?"

Both halves are true, and neither is decisive here.

**"The standard"** — yes, by market share (~28–31 %) and by enterprise default. That is a strong reason to *know* AWS and a weak reason to *host on it*. Nothing
in this application needs a service that only AWS has.

**"The most flexible"** — yes, in breadth of catalogue. But flexibility at AWS is sold as *provisioned capacity with an hourly floor*. An ALB, a NAT Gateway,
and an RDS instance bill ~$96/month combined before the application does anything; that is the price of options we would not exercise. Flexibility we pay for
monthly and never use is not flexibility, it is overhead.

There is also a quieter point: for a two-service application, ECS+Fargate is the *least* ergonomic of the managed-container platforms compared here. Cloud Run
and Container Apps give an HTTPS endpoint from an image; ECS wants task definitions, target groups, listener rules, execution roles, and log groups — all of
which we would author and maintain in Terraform. AWS's flexibility is real, but at this size it is charged in both euros and YAML.

The honest summary: **AWS is the right answer to a different question** — one with a team, a compliance requirement, or a service catalogue to draw on. Choosing
Hetzner now does not close that door; the application is containers and Postgres, and the Helm chart is the portable artifact.

---

## Consequences

- **Positive**: Lowest total cost by a wide margin (~€30/month for production *and* staging), so the required staging stage is affordable. Strongest GDPR/data
  residency position of any candidate. Docker, Kubernetes, Helm, and Terraform skills apply directly, and the planned Helm chart becomes the production
  deployment path. Egress is included, removing the most common bill-shock vector. Always-on containers suit ADR-008's scheduler without workarounds. Low
  lock-in — the workload is containers, Kubernetes manifests, and Postgres.
- **Negative**: **We own PostgreSQL.** Backups, PITR, restore verification, and minor-version upgrades are ours. We also own the k3s control plane, OS patching,
  and node upgrades. No managed observability. Single region and, initially, a single node — a node failure is an outage. Email-only support.
- **Backups are the load-bearing mitigation** (`wal-g` or `pgBackRest` streaming to Hetzner Storage Box, plus Hetzner server snapshots). A **restore drill must
  be part of the go-live checklist and repeated on a schedule** — an untested backup is not a backup. This is the single highest-risk item created by this ADR.
- **Single-instance importer**: the Helm chart must set `replicas: 1` with `strategy: Recreate` for `events-importer` so a rolling deploy never runs two
  schedulers. Multi-replica operation stays blocked on the `SELECT … FOR UPDATE SKIP LOCKED` work noted in ADR-008.
- **Admin API exposure**: `events-importer`'s admin endpoints must not be routed publicly by the ingress — cluster-internal service only, reachable via
  `kubectl port-forward` or, later, behind the planned authentication.
- **IaC**: use the `hetznercloud/hcloud` OpenTofu/Terraform provider for servers, networks, firewalls, and volumes; keep state in Hetzner Object Storage (S3
  API) or Terraform Cloud. This unblocks the "Infrastructure as code" backlog item.
- **CI/CD**: GitHub Actions cannot use OIDC against Hetzner, so deploys authenticate with a scoped kubeconfig or deploy key held as a repository secret, rotated
  deliberately. This is a genuine step down from AWS/GCP OIDC and should be treated as such.
- **Observability is now our problem**: budget for either a self-hosted `kube-prometheus-stack` + Grafana (fits the "Dashboard for analysing the data" backlog
  item) or an external SaaS free tier. Alerting must exist before launch, not after the first outage.
- **Frontend**: adds a `Dockerfile` and an nginx config to `events-frontend/`, and the SPA must call the API via a relative `/api` path so one image serves
  every environment.
- **Cost re-check**: Hetzner raised prices in 2026 and may again. Re-verify the numbers in this ADR at go-live and revisit annually.
- **Follow-ups unblocked**: register `event-junkie.de`; write the OpenTofu configuration; write the Helm chart; add release/deploy workflows; build the go-live
  checklist (legal, security, SEO, monitoring, alerting, dashboards, backups, recovery).

## References

- [Hetzner Cloud price adjustment, 15 June 2026](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/) — current CX/CPX/CAX/CCX
  pricing
- [Hetzner Cloud](https://www.hetzner.com/cloud/) · [
  `hetznercloud/hcloud` Terraform provider](https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs)
- [AWS Fargate pricing](https://aws.amazon.com/fargate/pricing/) · [Amazon VPC pricing (NAT Gateway, IPv4)](https://aws.amazon.com/vpc/pricing/)
- [AWS launches the European Sovereign Cloud, 15 January 2026](https://press.aboutamazon.com/aws/2026/1/aws-launches-aws-european-sovereign-cloud-and-announces-expansion-across-europe)
- [Google Cloud Run pricing](https://cloud.google.com/run/pricing) · [Cloud SQL pricing](https://cloud.google.com/sql/pricing)
- [Fly.io pricing](https://fly.io/docs/about/pricing/) · [Railway pricing](https://docs.railway.com/reference/pricing/plans) · [Render pricing](https://render.com/pricing)
- [DigitalOcean managed databases pricing](https://www.digitalocean.com/pricing/managed-databases) · [Neon pricing](https://neon.com/pricing)
- [Cloud market share 2026 (Synergy Research, via Statista)](https://www.statista.com/chart/18819/worldwide-market-share-of-leading-cloud-infrastructure-service-providers/)
- [ADR-005 — Migrations owned by the importer](ADR-005_MIGRATIONS_OWNED_BY_IMPORTER.md)
- [ADR-008 — Import job scheduling](ADR-008_IMPORT_JOB_SCHEDULING.md) — the single-instance / always-on constraint
- [TODO.md](../../TODO.md) — 🔴 Now: path to go-live
