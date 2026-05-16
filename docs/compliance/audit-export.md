# Audit JSONL Export

This document defines the v2.2 Business audit export boundary for SIEM ingestion.

## Scope

- Endpoint: `GET /api/v2/orgs/{orgId}/audit/events/export`
- Required edition: Business
- Required feature: `audit.export`
- Output format: JSON Lines, one audit event per line
- Media type: `application/x-ndjson`
- Filename pattern: `organization-audit-{orgId}-{yyyyMMdd-HHmmss}.jsonl`

The v1 CSV endpoint remains available for the existing organization admin workflow. The v2 endpoint is the Business JSONL/SIEM export surface.

## Query Parameters

| Parameter | Required | Meaning |
|---|---:|---|
| `orgId` | yes | Organization path id. Users must be organization managers. |
| `limit` | no | Export page size. Defaults to 100 and is capped at 10000. |
| `eventTypes` | no | Comma-separated event types, for example `ORG_DOMAIN_ADD,ORG_MEMBER_INVITE`. |
| `cursor` | no | Last exported audit event id. The next export returns events with a greater id. |
| `fromDate` | no | Inclusive start date in `YYYY-MM-DD`. |
| `toDate` | no | Inclusive end date in `YYYY-MM-DD`. |
| `sortDirection` | no | `ASC` for cursor-friendly forward export; defaults to `DESC`. |

Invalid date ranges and non-numeric cursors fail explicitly with `INVALID_ARGUMENT`.

## JSONL Schema

Each line is a standalone JSON object:

```json
{
  "schemaVersion": "mmmail.audit.v1",
  "source": "mmmail",
  "id": "13",
  "cursor": "13",
  "orgId": "99",
  "actorId": "42",
  "actorEmail": "owner@mmmail.local",
  "eventType": "ORG_DOMAIN_ADD",
  "targetType": "organization",
  "targetId": "99",
  "severity": "medium",
  "ipAddress": "203.0.113.10",
  "detail": "domain=audit.example.com",
  "createdAt": "2026-05-16T08:30"
}
```

Consumers should store the largest `cursor` value they have accepted and pass it to the next request.

## Security Boundary

- Free and Pro organizations are denied by the `audit.export` feature gate.
- Failed commercial access records `COMMERCIAL_ENTITLEMENT_DENIED`.
- Successful export records `ORG_AUDIT_JSONL_EXPORT`.
- The endpoint never bypasses organization manager checks.
- The export contains audit metadata only; it does not include message bodies, drive file content, secrets, tokens, license private keys, or payment credentials.

## SIEM Mapping

| JSONL field | SIEM usage |
|---|---|
| `schemaVersion` | Parser version guard |
| `source` | Source product |
| `id` / `cursor` | Deduplication and incremental checkpoint |
| `orgId` | Tenant dimension |
| `actorId` / `actorEmail` | User dimension |
| `eventType` | Event category |
| `targetType` / `targetId` | Affected entity |
| `severity` | Alert priority seed |
| `ipAddress` | Network source |
| `detail` | Structured detail string |
| `createdAt` | Event timestamp |

## Retention

Audit events remain immutable under the current organization monitor policy. The v2.2 endpoint provides paged export through `cursor`; object storage archival and large asynchronous export jobs remain future extensions and must not be represented as already available.

## Validation

- Backend contract: `BackendV22AuditExportContractTest`
- Local gate: `BACKEND_V22_COMMERCIAL_TESTS` in `scripts/validate-local.sh`
- CI gate: `Backend v2.2 commercial regression` in `.github/workflows/ci.yml`
