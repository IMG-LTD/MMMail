# Backend v2.1 Access Entitlement Gates Design

## Purpose

The next backend v2.1 slice is `backend-v21-access-entitlement-gates`.

The goal is to turn the permission and entitlement metadata already frozen in `V21ApiContractCatalog` into a reusable backend access gate foundation. This closes the Phase 1 architecture gap for shared response/error/permission/entitlement models before implementing more business `/api/v2/*` endpoints.

This slice must not return fake business data, bypass authentication, or hide failed checks. It defines the access decision model and request gate behavior that later v2.1 business APIs can rely on.

## Current Context

Already completed backend v2.1 foundations:

- API contract catalog runtime closure: `BackendV21ApiContractCatalogTest`, `V21ApiContractCatalog`, `/api/v2/platform/contracts`.
- Event outbox foundation: `platform_outbox_event`, `OutboxPublisher`, `InProcessOutboxDispatcher`.
- Background job foundation: `platform_job_run`, `JobRunner`, `InProcessJobRunner`.

Existing related code:

- `V21ApiContractCatalog` contains method, path, owner module, permissions, entitlement, and design source.
- `Result`, `BizException`, and `ErrorCode` define the current response and error envelope.
- `RequestTracingFilter` already captures request ID, org ID, scope ID, MDC, and tenant scope context.
- `OrgProductAccessInterceptor` and product access guard services enforce existing organization product access rules.

The new gate must reuse these pieces rather than introduce a parallel rule source.

## Recommended Approach

Use `V21ApiContractCatalog` as the single source of truth for v2.1 route access metadata.

Create a small platform access model and a server-side contract matcher/gate service. The interceptor enforces only contract-level rules:

- known `/api/v2/*` route
- required authentication or explicit public access
- required permission metadata
- required entitlement tier
- tenant/scope context propagation

Existing organization product access guards continue to run for their current scope. This slice does not replace them.

## Alternatives Considered

### Direct Command Center API First

This would implement `POST /api/v2/command-center/runs` and related endpoints before a shared gate. It was rejected because premium and permission checks would likely become endpoint-local and duplicated.

### Notification Worker Boundary First

This would connect notification jobs to the background job foundation. It was rejected for this step because worker-visible user states still need consistent access, entitlement, and failure semantics.

### Minimal Contract Matcher Only

This would only match method/path to catalog entries without enforcing decisions. It was rejected because it would not close the architecture requirement for permission and entitlement gates.

## Scope

In scope:

- Add immutable access model types in `mmmail-platform`.
- Add deterministic v2.1 method/path contract matching, including `:id`, `:key`, and `:token` path variables.
- Add server-side access gate service for v2.1 requests.
- Add an interceptor for `/api/v2/**`.
- Align public v2 security matchers with the contract catalog public routes.
- Return explicit errors for unknown v2 routes, missing authentication, missing entitlement, and denied permissions.
- Add focused Spring Boot tests.
- Update v2.1 progress documentation.

Out of scope:

- No business controller implementations for Command Center, Notifications, Billing, Admin, or other product APIs.
- No mock or simulated success responses.
- No external authorization server, policy engine, queue, or worker extraction.
- No database migration in this slice.
- No replacement of existing org product access guard behavior.

## Access Model

The platform access model should be small and immutable:

- `AccessEntitlement`: canonical tiers `COMMUNITY`, `PREMIUM`, `HOSTED`, `ENTERPRISE_GOVERNANCE`.
- `AccessPermission`: validated permission value such as `mail:send` or `command:center:run`.
- `AccessRequest`: request method, path, user ID, role, org ID, scope ID, and resolved contract.
- `AccessDecision`: allowed flag, reason code, HTTP status, error code, message, required entitlement, and required permissions.
- `AccessGate`: interface for evaluating an `AccessRequest`.

The model belongs in `mmmail-platform` because it is a stable contract that extracted workers or hosted services may reuse later.

## Contract Matching

The matcher should build from `V21ApiContractCatalog.defaultCatalog()`.

`V21ApiContractCatalog` must also register the existing platform metadata endpoints so the gate does not need a parallel allowlist:

- `GET /api/v2/platform/contracts`
- `GET /api/v2/platform/capabilities`

Rules:

- Match HTTP method exactly.
- Normalize trailing slashes.
- Treat catalog segments beginning with `:` as a single path variable.
- Do not guess unknown `/api/v2/*` routes.
- Preserve the matched contract identity for logs, tests, and future metrics.

Examples:

- `GET /api/v2/docs/123` matches `GET /api/v2/docs/:id`.
- `GET /api/v2/share/pass/abc` matches `GET /api/v2/share/pass/:token`.
- `GET /api/v2/unknown` fails as unknown route.

## Gate Semantics

Public access:

- Contracts with permission `auth:public`, `share:public`, or `system:public` are public.
- Public contracts do not require authentication.
- Public contracts still require a known contract match.

Authenticated access:

- All non-public contracts require an authenticated principal.
- Missing or anonymous authentication returns `UNAUTHORIZED`.

Permission behavior:

- The initial Community server does not yet have a persisted fine-grained permission assignment table.
- Role `ADMIN` satisfies permission checks for Community platform metadata and Community contracts.
- Role `ADMIN` does not bypass entitlement checks.
- A normal authenticated user is allowed for `community` contracts.
- A normal authenticated user is denied for `premium`, `hosted`, and `enterprise-governance` contracts until entitlement support enables them explicitly.
- Denied permission returns `FORBIDDEN` with an explicit message including the required permission.

Entitlement behavior:

- `community` is available in Community runtime.
- `premium`, `hosted`, and `enterprise-governance` are denied by default in Community runtime.
- Denials are explicit and observable; they must not silently fall through to controllers.
- Hosted/Premium enablement is a later slice and must use an explicit provider interface rather than hardcoded controller checks.

Tenant and scope behavior:

- The gate reads org/scope from `TenantScopeContextHolder`, which is populated by `RequestTracingFilter`.
- The gate does not trust tenant IDs from unauthenticated callers for non-public contracts.
- Org product access enforcement remains handled by `OrgProductAccessInterceptor`.

## Server Integration

Add a dedicated `V21ApiAccessGateInterceptor`.

Interceptor order should be deterministic:

1. Spring Security authenticates JWT when required.
2. `V21ApiAccessGateInterceptor` evaluates v2.1 contract-level access.
3. Existing `OrgProductAccessInterceptor` continues to evaluate account/org product rules.

The interceptor should cover `/api/v2/**` and exclude infrastructure endpoints such as Actuator and Swagger by path registration rather than by silent internal fallback.

Security matcher updates:

- Permit `POST /api/v2/auth/login`.
- Permit `POST /api/v2/auth/register`.
- Permit `GET /api/v2/share/mail/**`.
- Permit `GET /api/v2/share/drive/**`.
- Permit `GET /api/v2/share/pass/**`.
- Permit `GET /api/v2/system/status`.

Existing `/api/v2/platform/contracts` and `/api/v2/platform/capabilities` stay authenticated Community platform metadata contracts.

## Error Handling

Use the existing `Result` envelope and `BizException`.

Required explicit errors:

- Unknown v2 contract route: new `V2_API_CONTRACT_NOT_FOUND` error code mapped to `FORBIDDEN`, with message `Unknown v2 API contract`.
- Missing authentication for protected contract: existing `UNAUTHORIZED`.
- Missing entitlement: new `V2_ENTITLEMENT_REQUIRED` error code mapped to `FORBIDDEN`, with required entitlement in the message.
- Missing permission: new `V2_PERMISSION_DENIED` error code mapped to `FORBIDDEN`, with required permission in the message.

If new `ErrorCode` values are added, they must map to HTTP status in `GlobalExceptionHandler.resolveStatus`.

## Observability

The gate should not log secrets, request bodies, path tokens, vault data, or email/file contents.

It should make decisions inspectable through tests and future metrics by preserving:

- method
- normalized route pattern
- owner module
- required entitlement
- required permissions
- decision reason
- request ID through existing MDC

Metric emission is out of scope for this slice. The decision model still preserves the fields needed to add denial counters in a future observability slice.

## Testing Strategy

Add a focused test class named `BackendV21AccessEntitlementGatesTest`.

Coverage:

- Every catalog entry resolves to a non-empty access model.
- Dynamic path segments match catalog patterns.
- Public routes are classified as public and do not require authentication.
- Protected routes require authentication.
- Normal Community user can access a representative community route.
- Normal Community user is denied for representative premium, hosted, and enterprise-governance routes.
- Admin can access authenticated Community platform metadata.
- Admin is still denied for premium, hosted, and enterprise-governance routes when no entitlement provider grants them.
- Unknown `/api/v2/*` route is denied explicitly.
- `/api/v2/platform/contracts` remains authenticated.
- Frontend v2.1 tests and backend compile continue to pass.

The tests should target the gate service and selected MockMvc endpoints where real controllers already exist. They must not add fake business endpoints just to make requests succeed.

## Acceptance Criteria

The slice is complete when:

- `V21ApiContractCatalog` remains the only source of v2.1 route access metadata.
- Existing v2.1 platform metadata endpoints are represented in the catalog and do not need interceptor allowlist exceptions.
- v2.1 contract matching supports dynamic path variables and rejects unknown v2 routes explicitly.
- Community runtime distinguishes public, community, premium, hosted, and enterprise-governance contracts.
- Public contract routes are aligned with Spring Security permit rules.
- Protected v2 routes require authentication before business handling.
- Premium, hosted, and enterprise-governance gates fail visibly in Community runtime.
- Existing org product access enforcement remains in place.
- Targeted backend tests, backend compile, and frontend v2.1 tests pass.

## Non-Goals

- No actual entitlement purchase, billing provider, or plan lifecycle.
- No role/permission administration UI or database.
- No new business API responses.
- No hidden fallback to allow unknown routes.
- No replacement of Spring Security authentication.
