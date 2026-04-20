# MMMail Contracts

This directory is the frozen contract boundary for backend v2 and frontend v2.

## Ownership Labels
- `owner` values in shared contract files use the frozen contract namespace for the owning boundary.
- Labels may name either a frozen domain (for example `identity` or `platform`) or a shared cross-domain contract capability (for example `org-governance` or `public-share`).

## Rules
1. OpenAPI ownership is per domain under `contracts/openapi/`.
2. Event ownership is per domain under `contracts/events/`.
3. Shared error codes live only in `contracts/errors/error-codes.yaml`.
4. Backend implementation, frontend SDK generation, and frontend consumption may only begin after the relevant contract file is merged to `release/2.0.0`.
5. Breaking contract edits require a G-slice escalation.
