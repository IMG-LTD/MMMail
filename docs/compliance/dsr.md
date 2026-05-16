# DSR and Data Inventory

MMMail v2.2 exposes Business-gated data subject request (DSR) jobs for export and erasure. The API does not return synthetic completion: requests enqueue platform jobs, and the job runner executes table-level work from the inventory.

## API

All endpoints require an authenticated user and `FeatureCode.DSR_REQUESTS`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v2/orgs/{orgId}/dsr/export` | Queue subject data export |
| `POST` | `/api/v2/orgs/{orgId}/dsr/erasure` | Queue subject anonymize/delete work |
| `GET` | `/api/v2/orgs/{orgId}/dsr/jobs/{jobId}` | Read public job status |

Request body:

```json
{
  "subjectUserId": 77,
  "subjectEmail": "subject@example.com",
  "mode": "ANONYMIZE",
  "reason": "customer request"
}
```

Public status values are `queued`, `running`, `completed`, and `failed`. Internally they map to `platform_job_run` states so retries and failures remain visible in operational logs.

## Execution

- Export jobs use `JobRunType.DSR_EXPORT` and write a JSON result with `schemaVersion=mmmail.dsr.v1`.
- Erasure jobs use `JobRunType.DSR_ERASURE`.
- `docs/compliance/data-inventory.yaml` is the compliance source for table ownership, retention, export behavior, and erasure behavior.
- The runtime catalog handles high-value subject-owned product tables; the repository gate requires every schema/Flyway table to be declared before release.
- Audit, security, financial, and governance records are retained only where the inventory states a retention reason; direct PII columns are anonymized when listed.

## Gate

Run:

```bash
node scripts/validate-dsr-inventory.mjs
```

The gate fails when a new table is added without `owner`, `retention`, `subjectRef`, `export`, and `delete` metadata. This keeps migration review tied to DSR handling instead of relying on manual checklist memory.
