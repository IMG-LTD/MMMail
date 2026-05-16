# v2.2 Edition Entitlement Surface

Date: 2026-05-17

This document classifies the v2.2 commercial API surface. It is the source of truth for deciding whether an endpoint is a paid feature gate, an upgrade path, a public callback, or an external webhook.

## Rules

- Pro and Business runtime features must be enforced on the server side with `CommercialAuthorizationGate` or `FeatureGate`.
- Frontend `EntitlementGate` is only a UX hint and is never a security boundary.
- Upgrade paths must not require an already-paid entitlement, otherwise a Free self-hosted organization could not upload or inspect a license.
- External webhooks authenticate by signature and provider state, not by a user edition.
- No endpoint in the public repository may grant paid success through `none` billing provider behavior.
- Runtime edition resolution is deterministic: subscription state first, active license second, and `org_workspace.edition` fallback last. Non-paid subscription states resolve to Free and must not be silently overwritten by an active license.

## Current Surface

| Endpoint | Boundary | Required server evidence |
|---|---|---|
| `GET /api/v2/billing/readiness` | Public read-only readiness | Returns capability flags only; does not grant paid state |
| `GET /api/v2/billing/license/status` | Authenticated upgrade path | Requires active organization context; no paid entitlement required |
| `POST /api/v2/billing/license` | Authenticated upgrade path | Requires active organization context and verifies license with `MMMAIL_LICENSE_PUBLIC_KEY`; signing private key is not in this repository |
| `POST /api/v2/billing/webhook` | External billing gateway callback | Requires HMAC signature, timestamp window and `webhook` provider; `none` provider cannot apply paid state |
| `GET /api/v2/orgs/{orgId}/oidc/config` | Business `oidc.sso` | Calls `CommercialAuthorizationGate.enforceFeature(..., FeatureCode.OIDC_SSO)` |
| `PUT /api/v2/orgs/{orgId}/oidc/config` | Business `oidc.sso` | Calls `CommercialAuthorizationGate.enforceFeature(..., FeatureCode.OIDC_SSO)` |
| `POST /api/v2/auth/oidc/login` | Public login start | Loads enabled org config and `OidcSsoService` enforces `FeatureCode.OIDC_SSO` before returning an authorization URL |
| `GET /api/v2/auth/oidc/callback` | Public IdP callback | Consumes single-use state, validates token and `OidcSsoService` enforces `FeatureCode.OIDC_SSO` through enabled config lookup |
| `GET /api/v2/orgs/{orgId}/audit/events/export` | Business `audit.export` | Calls `CommercialAuthorizationGate.enforceFeature(..., FeatureCode.AUDIT_EXPORT)` |
| `POST /api/v2/orgs/{orgId}/dsr/export` | Business `dsr.requests` | Calls `CommercialAuthorizationGate.enforceFeature(..., FeatureCode.DSR_REQUESTS)` |
| `POST /api/v2/orgs/{orgId}/dsr/erasure` | Business `dsr.requests` | Calls `CommercialAuthorizationGate.enforceFeature(..., FeatureCode.DSR_REQUESTS)` |
| `GET /api/v2/orgs/{orgId}/dsr/jobs/{jobId}` | Business `dsr.requests` | Calls `CommercialAuthorizationGate.enforceFeature(..., FeatureCode.DSR_REQUESTS)` |

## Non-Commercial v2.1 Surfaces

The existing `/api/v1/suite/billing/*` endpoints remain v2.1 admin workflow surfaces. They may show external billing status and local quote or draft records, but they are not a live payment provider and must not be documented as paid entitlement enforcement.

## Change Rule

When a new Pro or Business backend endpoint is added, update this document and `BackendV22CommercialSurfaceCoverageContractTest` in the same change.
