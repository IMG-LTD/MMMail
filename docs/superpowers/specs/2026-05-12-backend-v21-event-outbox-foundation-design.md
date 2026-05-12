# Backend v2.1 Event Outbox Foundation Design

## 1. Purpose

This slice implements the backend v2.1 Phase 2 event/outbox foundation described in `docs/superpowers/specs/2026-04-28-backend-v21-architecture-design.md`.

The goal is to freeze the internal event contract and database-backed outbox path that lets Community run as one backend process while keeping Hosted/Premium worker extraction possible later.

## 2. Current State

The backend already has early platform markers:

- `backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/OutboxEventStatus.java`
- `backend/mmmail-platform/src/main/java/com/mmmail/platform/jobs/JobRunState.java`
- `docs/superpowers/progress/event-catalog.md`

The current gap is that those markers do not yet define a usable event model, outbox record, publisher contract, dispatcher contract, migration, or backend tests. The repository also must not require Kafka or another broker for Community startup.

## 3. Scope

In scope:

- Define immutable event and outbox contracts in `mmmail-platform`.
- Add a database-backed outbox table for `mmmail-server`.
- Add server-side persistence and in-process dispatch implementations.
- Add tests that prove event metadata, status transitions, retry/dead-letter behavior, and migration coverage.
- Update v2.1 progress documentation after implementation.

Out of scope:

- No Kafka, RabbitMQ, NATS, cloud queue, or external broker dependency.
- No extracted worker service.
- No real business-service event emission wiring.
- No fake event success path.
- No public business API response mocking.
- No broad refactor of existing service/controller packages.

## 4. Event Contract

Add a platform event package:

```text
backend/mmmail-platform/src/main/java/com/mmmail/platform/event/
```

Core records and enums:

- `PlatformEventType`
- `PlatformEventMetadata`
- `PlatformEvent`

`PlatformEventType` freezes the v2.1 event catalog names from the backend architecture design:

```text
identity.user.created
identity.session.revoked
workspace.activity.recorded
mail.message.created
mail.message.sent
mail.rule.matched
calendar.event.created
calendar.booking.created
drive.file.uploaded
drive.file.shared
docs.document.updated
docs.version.created
sheets.workbook.imported
pass.item.updated
pass.secure_link.created
collaboration.task.updated
command.run.requested
command.run.completed
notification.delivery.requested
admin.audit.recorded
billing.entitlement.changed
labs.ai_job.requested
```

Each type exposes:

- stable event name
- owner module
- whether tenant ID is required
- whether user ID is optional or required
- whether the event can be replayed by outbox dispatcher

`PlatformEventMetadata` carries:

- `tenantId`
- `userId`
- `requestId`
- `traceId`
- `module`
- `operation`
- `occurredAt`

`PlatformEvent` carries:

- `type`
- `aggregateType`
- `aggregateId`
- `metadata`
- `payloadJson`
- `idempotencyKey`

Validation is explicit. Missing required metadata, blank payload, unknown event type, or blank idempotency key raises an exception.

## 5. Outbox Contract

Add or expand the platform outbox package:

```text
backend/mmmail-platform/src/main/java/com/mmmail/platform/outbox/
```

Core records and interfaces:

- `OutboxEventRecord`
- `OutboxPublishRequest`
- `OutboxPublishResult`
- `OutboxDispatchResult`
- `OutboxPublisher`
- `OutboxDispatcher`

`OutboxEventRecord` is immutable and represents the persisted event. It includes:

- event ID
- event type
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
- attempts
- next attempt time
- last error
- created time
- updated time
- published time

Allowed status flow:

```text
PENDING -> PUBLISHED
PENDING -> FAILED
FAILED -> PENDING
FAILED -> DEAD_LETTER
```

Invalid transitions throw an explicit exception.

## 6. Server Persistence

Add Flyway migration:

```text
backend/mmmail-server/src/main/resources/db/migration/V11__platform_outbox_event.sql
```

Create table:

```text
platform_outbox_event
```

Required columns:

- `id`
- `event_type`
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
- `attempts`
- `next_attempt_at`
- `last_error`
- `created_at`
- `updated_at`
- `published_at`

Required indexes:

- unique index on `idempotency_key`
- index on `status, next_attempt_at`
- index on `owner_module, created_at`
- index on `tenant_id, created_at`

Update `schema.sql` and `db/baseline/community-v1-schema.sql` to keep local schema initialization and baseline schema aligned with Flyway.

## 7. Server Runtime Implementation

Add server-side implementation under focused packages:

```text
backend/mmmail-server/src/main/java/com/mmmail/server/outbox/
```

Expected implementation units:

- `PlatformOutboxEvent`
- `PlatformOutboxEventMapper`
- `DatabaseOutboxPublisher`
- `InProcessOutboxDispatcher`

`DatabaseOutboxPublisher` writes one row per event and returns the persisted ID and status. Duplicate `idempotencyKey` is treated as an explicit conflict by reading and returning the existing record if it matches the same event identity. A mismatch raises an exception because it indicates idempotency-key misuse.

`InProcessOutboxDispatcher` reads due `PENDING` events and delegates to a handler registry. This slice defines the dispatcher contract and test handler path only; it does not wire business handlers into existing services.

## 8. Failure Handling

Dispatch success marks the event `PUBLISHED` and sets `published_at`.

Dispatch failure:

- increments `attempts`
- stores the failure message in `last_error`
- sets `status` to `FAILED`
- schedules `next_attempt_at`

Retry moves `FAILED` back to `PENDING` only when the retry time is due.

When attempts exceed the configured maximum for the dispatcher, status becomes `DEAD_LETTER`. This rule is explicit in dispatcher configuration and unit tests, not a hidden fallback.

## 9. Observability

The publisher and dispatcher emit Micrometer metrics:

- `mmmail.outbox.events.published.total`
- `mmmail.outbox.dispatch.total`
- `mmmail.outbox.dispatch.failed.total`
- `mmmail.outbox.dead_letter.total`

Metric tags:

- `event`
- `module`
- `status`

Logs must include event ID, event type, module, tenant ID, request ID, and trace ID when available. Logs must not include payload JSON.

## 10. Tests

Add targeted backend tests:

```text
backend/mmmail-server/src/test/java/com/mmmail/server/BackendV21EventOutboxFoundationTest.java
```

The test covers:

- event catalog contains every v2.1 core event family from the architecture design
- event metadata validation rejects blank required fields
- outbox record accepts valid status transitions and rejects invalid transitions
- publisher persists `PENDING` event records with full metadata
- publisher rejects idempotency-key mismatch
- dispatcher marks success as `PUBLISHED`
- dispatcher marks repeated failure as `DEAD_LETTER`
- migration SQL contains `platform_outbox_event` and required indexes
- no external broker property is required for Community test profile

Existing validation that must continue passing:

```bash
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml test -Dtest=BackendV21EventOutboxFoundationTest -Dsurefire.failIfNoSpecifiedTests=false
timeout 60s mvn -pl mmmail-server -am -f backend/pom.xml compile
timeout 60s pnpm --dir frontend-v2 test
```

## 11. Acceptance Criteria

- `mmmail-platform` exposes immutable event and outbox contracts.
- `mmmail-server` has a Flyway-backed `platform_outbox_event` table.
- Community backend tests run without Kafka or another external broker.
- Status transitions and dead-letter behavior are explicit and tested.
- No business service is changed to emit real events in this slice.
- No mock business API success path is added.
- Progress documentation records the slice before and after implementation.
