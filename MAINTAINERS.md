# MMMail Maintainers

## Maintainer Roles

| Area | Owner | Scope |
|---|---|---|
| BDFL | Xiang | Final roadmap, architecture, licensing, release scope |
| Release | Xiang | Release gate, tags, release notes, blocker decisions |
| Backend | Xiang | Spring Boot server, migrations, security, API contracts |
| Frontend product | Xiang | `frontend-admin`, routes, UX, i18n, e2e, bundle gates |
| Legacy frontend | Xiang | `frontend-v2` contract migration and archival decisions |
| Docs / OSS | Xiang | README, governance, support boundaries, roadmap |
| Security | Xiang | Private disclosure, threat model, secret scanning |

## Response Targets

These are best-effort targets, not SLA commitments.

| Item | Target |
|---|---|
| Release-blocking regression triage | 3 business days |
| Security contact request acknowledgement | 3 business days |
| Regular bug/feature triage | 7 business days |
| External contribution review | 10 business days |

## Ownership Rules

- Changes touching an owned area need owner review before merge.
- Changes that affect release status, support boundaries, security posture, deployment, or commercial promises require BDFL approval.
- `frontend-v2` is legacy reference scope. New product features must target `frontend-admin`.
