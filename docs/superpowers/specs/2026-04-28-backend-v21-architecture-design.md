# Backend v2.1 Architecture Design

## 1. Purpose

MMMail v2.1 needs backend architecture that supports the new platform-level frontend capability set without making open-source self-hosting difficult. The target architecture is **Community-first modular monolith, Hosted/Premium service-extraction ready**.

This design is a planning specification only. It does not implement backend code, migrations, service splitting, infrastructure automation, pricing, licensing, or deployment changes.

## 2. Decisions already confirmed

- Community/self-hosted deployments should remain simple and stable.
- Hosted/Premium deployments may split selected workers or services when scale, isolation, compliance, or commercial operations require it.
- Full microservice decomposition is not the default v2.1 target.
- v2.1 backend design must support the frontend v2.1 API namespaces and product modules.
- Current backend uses Java 21, Spring Boot, Maven modules, MySQL, Redis, Flyway, MyBatis-Plus, Actuator, and SpringDoc/OpenAPI.
- Current deployment topology is one frontend, one backend, MySQL, Redis, and optional Nacos placeholder; Kafka is not part of the current public runtime path.

## 3. Current backend baseline

### 3.1 Runtime shape

The current public runtime is a single Spring Boot backend process:

```text
frontend-v2 -> backend -> MySQL
                    -> Redis
```

Standard local topology can also include Nacos as a local registration/configuration placeholder, but the repository does not currently run a real multi-service topology.

### 3.2 Maven module baseline

Current backend Maven modules:

```text
mmmail-common
mmmail-foundation
mmmail-identity
mmmail-org-governance
mmmail-platform
mmmail-mail
mmmail-pass
mmmail-drive
mmmail-workspace
mmmail-billing
mmmail-labs
mmmail-server
```

The current structure is therefore already moving toward a modular backend, but most runtime controllers, services, mappers, entities, configuration, security, and observability code still execute through `mmmail-server`.

### 3.3 Current classification

The current backend is best classified as:

```text
Modular monolith in build structure, single-process monolith at runtime.
```

It is service-extraction-ready only at the planning and boundary level, not as a deployed microservice system.

## 4. Target architecture

### 4.1 Architecture statement

v2.1 should use a **modular monolith runtime** for Community and a **selective service/worker extraction model** for Hosted/Premium.

```text
Community default:
  frontend-v2 -> mmmail-backend -> MySQL
                              -> Redis

Hosted/Premium optional:
  frontend-v2 -> api-gateway / edge
              -> mmmail-api
              -> selected workers/services
              -> shared platform dependencies
```

### 4.2 Why not full microservices by default

Full microservices do not automatically make MMMail more stable. They add distributed-system failure modes:

- network partitions
- partial deploy failures
- cross-service transaction problems
- queue lag
- event ordering problems
- distributed tracing requirements
- schema ownership conflicts
- service-to-service authentication complexity
- local development and self-hosting complexity

Because MMMail is open source with a self-hosted Community edition, v2.1 must not make the baseline deployment harder than necessary.

### 4.3 Target principle

```text
Code boundaries first.
Runtime boundaries later.
Only extract services when operational evidence justifies it.
```

## 5. Edition-specific topology

### 5.1 Community topology

Community should stay deployable as:

```text
frontend
backend
mysql
redis
```

Optional local dependencies such as Nacos may remain disabled by default or documented as non-required.

Community must not require:

- Kafka
- service mesh
- Kubernetes
- distributed tracing stack
- external object storage
- separate worker fleet
- multi-node database topology

Community can support optional advanced local dependencies later, but one-machine self-hosting remains the primary path.

### 5.2 Premium topology

Premium can run the same Community topology with more capabilities enabled by entitlement.

Premium may additionally enable internal workers when an operator chooses to deploy them:

```text
notification-worker
mail-delivery-worker
file-preview-worker
command-runner
audit-export-worker
ai-labs-worker
```

Premium workers must have a safe in-process fallback or disabled state unless the specific feature cannot work without asynchronous processing.

### 5.3 Hosted topology

Hosted may run a larger topology:

```text
edge / ingress
api-gateway
mmmail-api
notification-worker
mail-delivery-worker
file-preview-worker
command-runner
ai-labs-worker
billing-service
audit-export-worker
admin-ops-service
mysql / managed database
redis / managed cache
object storage
queue / event bus
observability stack
```

Hosted-only services provide managed operations, scale, compliance, billing, monitoring, and support. They must not leak as mandatory dependencies into Community installation.

## 6. Domain modules

v2.1 should organize backend capabilities around these domain modules:

| Module | Responsibility | Community runtime | Hosted/Premium extraction candidate |
| --- | --- | --- | --- |
| Foundation | tenant context, common errors, IDs, time, request metadata | in-process | no |
| Identity | auth, sessions, users, devices, account security | in-process | possible only for hosted identity scale |
| Workspace | dashboard, activity, tasks, cross-module summary | in-process | aggregation cache worker optional |
| Mail | folders, messages, threads, contacts, composer, rules | in-process | mail delivery worker |
| Calendar | events, reminders, rooms, seats, resources | in-process | scheduling optimization worker |
| Drive | files, folders, upload metadata, sharing, versions | in-process | file preview/conversion worker |
| Docs | documents, templates, comments, share, versions | in-process | version diff worker optional |
| Sheets | workbooks, imports, cleaning, insights | in-process | import/insight worker |
| Pass | vaults, items, secure links, aliases, monitor | in-process | breach monitor worker optional |
| Collaboration | projects, tasks, comments, knowledge, activity | in-process | automation worker optional |
| Command Center | commands, runs, workflows, logs, audit | in-process controls | command-runner extraction likely |
| Notifications | inbox, preferences, rules, templates, delivery | in-process baseline | notification-worker likely |
| Settings | user profile preferences, security settings, devices, integrations, personal audit view | in-process | no separate service by default |
| Admin/Governance | users, orgs, roles, policies, audit, risk | in-process | audit export and compliance worker |
| Billing | plans, invoices, quotas, entitlement sync | in-process stub/baseline | billing-service likely for Hosted |
| Labs/AI | labs modules, AI features, experiments | in-process metadata | ai-labs-worker likely |

## 7. Module boundary rules

### 7.1 Code ownership

Each module should own:

- application services
- domain services
- DTOs for module API boundaries
- mappers/repositories for owned tables
- event producers for module events
- event consumers for subscribed events
- contract tests for public APIs and events

### 7.2 Forbidden coupling

Modules should not:

- call another module's mapper/repository directly
- mutate another module's owned tables
- depend on another module's internal entity classes
- share transaction boundaries across unrelated domains
- require another module's concrete service implementation where an interface or event would be enough

### 7.3 Allowed coupling

Modules may interact through:

- explicit Java interfaces in stable boundary packages
- REST/internal API contracts
- domain events through an outbox/event bus abstraction
- read models owned by the consuming module
- shared foundation/common libraries

## 8. Data ownership

### 8.1 Ownership principle

Every table must have one owning module. Other modules can read only through:

- owning module service/API
- read model
- event-projected copy
- documented query view

### 8.2 Shared data categories

Shared concepts should be modeled as platform references, not duplicated ownership:

| Shared concept | Owner |
| --- | --- |
| User identity | Identity |
| Tenant context reference | Foundation |
| Organization records | Org Governance |
| Entitlement | Billing |
| Audit event records | Admin/Governance |
| Platform event dispatch state | Platform |
| File binary metadata | Drive |
| Notification delivery state | Notifications |
| Background job state | Platform |

### 8.3 Transaction strategy

Community modular monolith can use local database transactions inside one module boundary.

Cross-module workflows should use:

1. local write in the owning module
2. outbox event record
3. event dispatch
4. consumer-side update or read-model projection
5. retryable failure handling

This prepares Hosted extraction without forcing distributed transactions into Community.

## 9. API contract strategy

### 9.1 Public API namespaces

v2.1 backend should align with the frontend v2.1 route and data needs through these namespaces:

```text
/api/v2/workspace/*
/api/v2/mail/*
/api/v2/calendar/*
/api/v2/drive/*
/api/v2/docs/*
/api/v2/sheets/*
/api/v2/pass/*
/api/v2/collaboration/*
/api/v2/command-center/*
/api/v2/notifications/*
/api/v2/admin/*
/api/v2/settings/*
/api/v2/labs/*
/api/v2/billing/*
/api/v2/entitlements/*
```

### 9.2 Contract rule

Every public v2 API should have:

- OpenAPI documentation
- request and response schema
- error schema
- permission metadata
- entitlement metadata where applicable
- pagination/filter/sort contract where applicable
- contract test coverage

### 9.3 Versioning rule

v2.1 should introduce or stabilize `/api/v2/*` contracts without breaking existing `/api/v1/*` immediately unless the implementation plan explicitly includes migrations and redirects.

## 10. Event and worker strategy

### 10.1 Event abstraction

v2.1 should define an internal event abstraction that can run in two modes:

| Mode | Community | Hosted/Premium |
| --- | --- | --- |
| In-process/outbox polling | default | supported for small deployments |
| External queue/event bus | optional | preferred for extracted workers |

Community must not require Kafka or another external broker to boot.

### 10.2 Event categories

Core event families:

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

### 10.3 Worker extraction candidates

#### notification-worker

Responsibilities:

- notification fan-out
- channel delivery
- template rendering
- retry/backoff
- webhook delivery
- delivery analytics projection

#### mail-delivery-worker

Responsibilities:

- outbound SMTP delivery
- scheduled mail sending
- retry/backoff
- delivery status updates
- bounce processing when supported

#### file-preview-worker

Responsibilities:

- preview generation
- thumbnail generation
- document conversion
- file safety scan integration
- preview cache updates

#### command-runner

Responsibilities:

- command execution
- streaming logs
- cancellation
- retry
- workflow step execution
- run metrics

#### ai-labs-worker

Responsibilities:

- AI insight jobs
- Sheets analysis
- Docs summarization
- Labs experiment execution
- provider isolation

#### billing-service

Responsibilities:

- hosted plan lifecycle
- invoice provider integration
- entitlement synchronization
- quota reconciliation
- commercial audit

#### audit-export-worker

Responsibilities:

- large audit export
- compliance report generation
- long-running governance jobs
- secure export expiration

## 11. Reliability and stability rules

### 11.1 Community stability

Community stability should come from simplicity:

- fewer required moving parts
- local health checks
- clear startup validation
- one backend log stream
- Flyway-controlled schema evolution
- Redis usage is documented and narrowly scoped
- no mandatory distributed transaction coordinator

### 11.2 Hosted stability

Hosted stability should come from operational controls:

- health checks per service
- readiness and liveness probes
- queue lag monitoring
- retry and dead-letter handling
- circuit breakers for external providers
- idempotent workers
- structured logs
- distributed traces
- per-module metrics
- deployment rollback strategy

### 11.3 Failure handling

Every extracted worker must define:

- retry policy
- idempotency key
- dead-letter behavior
- operator-visible failure state
- user-visible failure or pending state
- recovery command or replay path

## 12. Observability

### 12.1 Required metadata

Every request, job, event, and worker run should carry:

```text
requestId
traceId
tenantId
userId when available
module
operation
entitlement context when relevant
```

### 12.2 Metrics

Required metric categories:

- HTTP latency and error rate by module
- database query latency by module
- Redis latency and error rate
- event publish count and failure count
- outbox lag
- worker queue lag
- worker retry count
- worker failure count
- external provider latency and error rate
- entitlement gate denial count
- premium/hosted feature usage count

### 12.3 Logs

Logs should be structured and avoid secrets. Sensitive values such as tokens, passwords, vault data, email content, file content, and secure-link tokens must not be logged.

## 13. Security boundaries

### 13.1 Service-to-service security

Community in-process modules use the existing authenticated request context.

Hosted extracted services need:

- service identity
- signed internal requests or mTLS
- least-privilege database/API access
- tenant context propagation
- audit trail for privileged service actions

### 13.2 Secrets

Secrets must stay in environment or managed secret stores. They must not be stored in repository files, logs, job payloads, or event payloads.

### 13.3 Tenant isolation

Every module and worker must enforce tenant scoping at the boundary. Extracted services cannot trust tenant IDs from unauthenticated callers.

## 14. Deployment model

### 14.1 Community Compose target

Community Compose target remains:

```text
frontend
backend
mysql
redis
```

This target must remain the default install and quick-start path.

### 14.2 Hosted deployment target

Hosted deployment can add:

```text
api-gateway
mmmail-api
notification-worker
mail-delivery-worker
file-preview-worker
command-runner
ai-labs-worker
billing-service
audit-export-worker
object-storage
queue
observability
```

The Hosted deployment target should be documented separately from Community install docs so open-source users are not forced to understand hosted-only topology.

## 15. Migration path

### Phase 1: strengthen modular monolith

- Move domain logic from `mmmail-server` into domain modules.
- Define module-owned table boundaries.
- Add module boundary tests.
- Add API contract tests for `/api/v2/*`.
- Add shared response/error/permission/entitlement models.

### Phase 2: introduce event/outbox foundation

- Define event catalog.
- Add outbox table and publisher abstraction.
- Add in-process event dispatcher for Community.
- Add idempotent consumer interfaces.
- Add outbox lag metrics.

### Phase 3: worker-ready execution

- Convert long-running or retryable operations into background job abstractions.
- Add background job state and progress APIs.
- Add notification, mail delivery, file preview, command run, AI job, and audit export job boundaries.
- Keep in-process Community execution where practical.

### Phase 4: Hosted extraction

- Split selected workers into deployable services for Hosted.
- Add queue/event bus integration.
- Add service-to-service auth.
- Add distributed tracing.
- Add worker deployment and rollback runbooks.

## 16. Testing strategy

### 16.1 Architecture tests

- Module dependency rules prevent direct mapper/entity coupling across module boundaries.
- Controller packages map to the correct domain module.
- Every table has one documented owner.
- Every `/api/v2/*` route has permission and entitlement metadata.
- Hosted-only features are not required for Community startup.

### 16.2 Contract tests

- OpenAPI contracts are generated and validated.
- Error response schema is consistent across modules.
- Pagination/filter/sort behavior is consistent.
- Permission denied and premium locked responses are consistent.
- Event payloads match the event catalog.

### 16.3 Integration tests

- Community backend starts with MySQL and Redis only.
- Community backend starts with hosted workers disabled.
- Outbox in-process dispatch handles publish and retry paths.
- Background jobs expose progress, failure, and retry state.
- Worker extraction mode can consume the same event/job contract.

### 16.4 Operational tests

- Health endpoints identify degraded dependencies.
- Metrics include module labels.
- Logs include request/job/event correlation IDs.
- Worker failures are visible and replayable.
- Queue/event bus outage does not break unrelated Community requests.

## 17. Acceptance criteria

The v2.1 backend architecture is complete when:

- Community can still run as one backend process with MySQL and Redis.
- Hosted/Premium extraction candidates are documented with responsibilities and contracts.
- Every v2.1 product module has a backend owner and API namespace.
- Every shared table and domain table has one owner.
- Cross-module interactions use interfaces, APIs, events, or read models instead of direct repository coupling.
- `/api/v2/*` contracts align with the v2.1 frontend spec.
- Event/outbox strategy works without requiring Kafka for Community.
- Long-running work has job state, progress, retry, and failure semantics.
- Observability carries module, tenant, request, trace, job, and event metadata.
- Security rules cover tenant scoping, secrets, and service-to-service calls.
- Tests verify module boundaries, contracts, startup topology, and worker extraction readiness.
