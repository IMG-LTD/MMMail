# MMMail SLI/SLO Internal Targets

This document defines internal target signals for operating MMMail. It is not a public SLA, not a contractual commitment, and not a hosted-service availability promise.

## Scope

These targets apply to the v2.2 commercial-ready self-hosted baseline:

- backend API runtime
- billing webhook handling
- license verification
- OIDC callback flow after BUS-01 lands
- self-hosted operator diagnostics

They are used to decide where engineering attention is needed. They do not create uptime credits, response guarantees, or managed-service obligations.

## Required SLIs

| SLI | Measurement | Internal target | First response when breached |
|---|---|---:|---|
| API p99 | 99th percentile HTTP request latency for documented API routes | Track trend by release | Inspect route, DB, Redis, and downstream dependency spans |
| 5xx rate | Server error responses divided by total API responses | Track per release and deployment | Review exception logs, recent deploys, and migration status |
| billing webhook success rate | Accepted and processed billing webhook events divided by valid signed webhook attempts | Track per provider and event type | Check signature failures, idempotency conflicts, and provider payload drift |
| license verification failure rate | Failed license verification attempts divided by license upload or sync attempts | Track by failure reason | Check public key config, expiry, org mismatch, and payload tampering |
| OIDC callback failure rate | Failed OIDC callback attempts divided by callback attempts | Track by IdP and error reason | Check state, redirect URI, PKCE, token exchange, and clock skew |

## Evidence Sources

- application logs
- structured exception logs
- Prometheus metrics
- billing webhook audit events
- commercial entitlement denial audit events
- release-gate and CI artifacts
- OpenTelemetry runtime traces from OBS-01; OIDC callback-specific dimensions land with BUS-01

## Alerting Boundary

Alerts should be actionable and tied to a runbook or owner. Do not create alert rules that page a maintainer without enough context to debug the affected path.

Initial alert dimensions:

- route
- organization id when available and safe to log
- feature code
- edition
- provider
- OIDC issuer
- failure reason
- release version

## Public Wording Boundary

Allowed public wording:

- "internal target"
- "quality signal"
- "operator diagnostic"
- "best-effort support target"

Not allowed public wording:

- "availability guarantee"
- "contractual uptime"
- "contractual response guarantee"
- "managed incident response"

## Review Cadence

Review these targets during release preparation when:

- a new commercial feature enters the release gate
- billing, license, OIDC, audit export, or DSR behavior changes
- a new deployment path is added
- an incident or release blocker shows missing telemetry
