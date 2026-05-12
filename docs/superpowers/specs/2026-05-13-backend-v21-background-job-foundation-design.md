# Backend v2.1 Background Job Foundation Design

## 1. Purpose

This slice implements the next backend v2.1 Phase 3 foundation from `docs/superpowers/specs/2026-04-28-backend-v21-architecture-design.md`.

The goal is to add a Community-safe background job contract and in-process execution path that can later be extracted into Hosted/Premium workers without changing the public job model.

This design does not implement business-specific jobs. It freezes the generic job lifecycle, persistence, retry semantics, runtime visibility, and tests that future notification, mail delivery, file preview, command runner, AI, billing, and audit export work can reuse.

## 2. Current State

The backend already has early job and observability markers:

- `backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunState.java`
- `backend/mmmail-server/src/main/java/com/mmmail/server/observability/JobRunMonitorService.java`
- `backend/mmmail-server/src/main/java/com/mmmail/server/model/vo/SystemHealthOverviewVo.java`
- `backend/mmmail-server/src/test/java/com/mmmail/server/JobRunMonitorServiceTest.java`

The current gap is that job runs are in-memory observability entries only. They are not persisted, cannot expose durable progress/failure/retry state, and are not yet a stable contract for Hosted/Premium worker extraction.

The previous v2.1 backend slice added database-backed outbox support. This slice should build on that direction while keeping Community runtime as one Spring Boot backend process.

## 3. Scope

In scope:

- Define immutable platform job contracts in `mmmail-platform`.
- Add `platform_job_run` persistence in `mmmail-server`.
- Add a database-backed job repository.
- Add an in-process job runner for Community.
- Add explicit job status transitions, progress updates, failure details, retry state, and metrics.
- Add focused tests for contract validation, persistence, execution, retry, and Community startup boundaries.
- Update v2.1 progress documentation after implementation.

Out of scope:

- No Kafka, RabbitMQ, cloud queue, or external worker dependency.
- No extracted worker service.
- No fake business job success path.
- No public mock API payloads.
- No command-center business endpoint implementation.
- No broad refactor of existing suite, command-center, notification, or mail services.

## 4. Job Contract

Add or expand the platform job package:

```text
backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/
```

Expected contract units:

- `JobRunType`
- `JobRunMetadata`
- `JobRunRecord`
- `JobRunRequest`
- `JobRunResult`
- `JobRunHandler`
- `JobRunner`

`JobRunType` freezes worker extraction candidates from the backend architecture design:

```text
notification.delivery
mail.delivery
file.preview
command.run
ai.labs
billing.entitlement_sync
audit.export
```

Each type exposes:

- stable job name
- owner module
- whether tenant ID is required
- whether user ID is optional or required
- whether retry is supported
- whether Hosted/Premium extraction is expected later

`JobRunMetadata` carries:

- `tenantId`
- `userId`
- `requestId`
- `traceId`
- `module`
- `operation`
- `requestedAt`

`JobRunRequest` carries:

- `type`
- `metadata`
- `aggregateType`
- `aggregateId`
- `payloadJson`
- `idempotencyKey`

Validation is explicit. Missing required metadata, blank payload, blank aggregate identity, invalid module ownership, or blank idempotency key raises an exception.

## 5. Job Lifecycle

`JobRunState` should remain a stable platform enum, but the lifecycle must become explicit and testable.

Allowed state flow:

```text
QUEUED -> RUNNING
RUNNING -> WAITING_APPROVAL
RUNNING -> SUCCEEDED
RUNNING -> FAILED
RUNNING -> RETRYABLE
WAITING_APPROVAL -> RUNNING
WAITING_APPROVAL -> FAILED
RETRYABLE -> QUEUED
RETRYABLE -> FAILED
```

Invalid transitions throw an explicit exception.

`JobRunRecord` stores:

- run ID
- job type
- owner module
- tenant ID
- user ID
- request ID
- trace ID
- aggregate type
- aggregate ID
- payload JSON
- idempotency key
- status
- progress percentage
- attempts
- max attempts
- next attempt time
- last error code
- last error message
- result JSON
- created time
- updated time
- started time
- completed time

Progress is bounded to `0..100`. Failures are visible through `lastErrorCode` and `lastErrorMessage`; errors are not swallowed or converted into success.

## 6. Server Persistence

Add Flyway migration:

```text
backend/mmmail-server/src/main/resources/db/migration/V12__platform_job_run.sql
```

Create table:

```text
platform_job_run
```

Required columns:

- `id`
- `job_type`
- `owner_module`
- `tenant_id`
- `user_id`
- `request_id`
- `trace_id`
- `aggregate_type`
- `aggregate_id`
- `payload_json`
- `idempotency_key`
- `status`
- `progress_percent`
- `attempts`
- `max_attempts`
- `next_attempt_at`
- `last_error_code`
- `last_error_message`
- `result_json`
- `created_at`
- `updated_at`
- `started_at`
- `completed_at`

Required indexes:

- unique index on `idempotency_key`
- index on `status, next_attempt_at`
- index on `owner_module, created_at`
- index on `tenant_id, created_at`
- index on `job_type, created_at`

Update `schema.sql` and `db/baseline/community-v1-schema.sql` with the same table and indexes.

## 7. Server Runtime Implementation

Add focused server package:

```text
backend/mmmail-server/src/main/java/com/mmmail/server/jobs/
```

Expected implementation units:

- `PlatformJobRun`
- `PlatformJobRunMapper`
- `DatabaseJobRunRepository`
- `InProcessJobRunner`
- `ExplicitJobRunHandlerRegistry`

`DatabaseJobRunRepository` persists and updates job runs. Duplicate `idempotencyKey` returns the existing matching run. A duplicate key with mismatched job identity or payload raises an explicit exception.

`InProcessJobRunner` starts due queued jobs in the current Spring Boot process. It delegates to registered handlers, updates progress and status, records failure state, and marks jobs retryable only when the job type supports retry and attempts remain.

If no handler exists for a job type, the runner must fail the run explicitly with a missing-handler error. It must not pretend the job succeeded.

## 8. Community and Hosted Boundaries

Community default:

```text
frontend-v2 -> mmmail-server -> MySQL
                              -> Redis
```

Community must not require:

- external queue
- separate worker process
- service mesh
- distributed tracing stack
- cloud object storage

Hosted/Premium can later reuse the same persisted contract from extracted workers. The extraction boundary is the job contract and database state, not a new mock API layer.

## 9. Observability

Metrics should include:

- `mmmail.jobs.runs.total`
- `mmmail.jobs.runs.failed.total`
- `mmmail.jobs.runs.retryable.total`
- `mmmail.jobs.run.duration`
- `mmmail.jobs.active.runs`

Metric tags:

- `job`
- `module`
- `status`

Logs and metrics must not include payload JSON, secrets, vault values, email body content, secure-link tokens, or file contents.

## 10. Test Design

Add focused backend test coverage, preferably:

```text
backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21BackgroundJobFoundationTest.java
```

Required behavior:

- Job catalog covers all v2.1 worker extraction candidates.
- Metadata validation rejects missing required tenant/user/module data.
- Job record enforces allowed state transitions.
- Repository persists queued jobs and rejects idempotency mismatches.
- In-process runner marks successful jobs as `SUCCEEDED`.
- In-process runner marks failed retryable jobs as `RETRYABLE`.
- In-process runner marks non-retryable or exhausted jobs as `FAILED`.
- Missing handler produces an explicit failed state.
- Migration, schema, and baseline contain `platform_job_run` and required indexes.
- Community test profile exposes in-process job execution without external worker or broker beans.

Required verification commands:

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21BackgroundJobFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile
timeout 60s pnpm --dir frontend-v2 test
```

## 11. Progress Tracking

Update `docs/superpowers/progress/v21-implementation-progress.md` after implementation with:

- Slice: `backend-v21-background-job-foundation`
- Implementation commit hash.
- Files changed.
- Targeted backend test outcome.
- Maven compile outcome.
- Frontend v2.1 test outcome.
- Any remaining backend v2.1 worker-ready risks.

Do not add `.tmp/`, generated target artifacts, screenshots, archives, or unrelated untracked paths to commits.

## 12. Acceptance Criteria

This slice is complete when:

- Platform job contracts exist and validate metadata explicitly.
- `platform_job_run` persistence exists in migration, schema, and baseline.
- Community in-process runner can execute, fail, and retry jobs without external infrastructure.
- Job failures remain visible and do not become silent fallback success.
- Metrics expose job status, owner module, and failure counters without sensitive payloads.
- Tests prove state transitions, persistence, retry, missing-handler failure, migration coverage, and Community startup boundaries.
- Backend target test, backend compile, and frontend v2.1 tests pass.

## 13. Self-Check

- This design is one implementation slice.
- It follows v2.1 Phase 3 after event/outbox foundation.
- It does not implement business-specific worker features.
- It does not introduce a broker, external worker, fake success path, or silent fallback.
- It keeps Community runtime simple while preserving Hosted/Premium extraction readiness.
