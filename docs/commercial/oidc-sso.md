# MMMail Business OIDC SSO

This document defines the public repository boundary for Business OIDC SSO. It covers configuration, callback URLs, token exchange, ID token validation, MMMail session issuance, and operator validation. It does not promise SCIM, organization sync, hosted identity operations, or automatic user provisioning.

## Runtime Boundary

OIDC is disabled by default:

```env
MMMAIL_OIDC_ENABLED=false
MMMAIL_OIDC_CALLBACK_PATH=/api/v2/auth/oidc/callback
MMMAIL_OIDC_CLIENT_SECRET=
```

Organization-level runtime configuration is stored in `org_oidc_config`. The table stores `client_secret_ref`, not the secret value; the runtime resolves that reference from the environment, for example `MMMAIL_OIDC_CLIENT_SECRET`.

Short-lived login state is stored in `oidc_auth_state` with a 10 minute TTL. The state record carries `state`, `nonce`, `codeVerifier`, callback URI, and post-login redirect URI so callback validation can reject replay, expiry, redirect mismatch, and missing PKCE material.

The callback flow exchanges the authorization code at the IdP token endpoint, validates the returned `id_token` signature, issuer, audience, and nonce, then maps the verified email to an existing active MMMail user. If no matching active local user exists, login fails explicitly; the public repository does not silently auto-provision accounts.

## Endpoints

| Endpoint | Auth | Boundary |
|---|---|---|
| `GET /api/v2/orgs/{orgId}/oidc/config` | JWT + Business `oidc.sso` | Read sanitized org OIDC config |
| `PUT /api/v2/orgs/{orgId}/oidc/config` | JWT + Business `oidc.sso` | Save issuer, client id, secret reference, callback URI, scopes, and allowed post-login redirects |
| `POST /api/v2/auth/oidc/login` | public | Create state / nonce / PKCE challenge and return IdP authorization URL |
| `GET /api/v2/auth/oidc/callback` | public | Consume state once, exchange code, validate ID token, issue MMMail session |

## Keycloak Hard Acceptance

Use Keycloak as the hard acceptance IdP for BUS-01:

```text
Issuer: https://idp.example.com/realms/mmmail
Client ID: mmmail-business
Callback URL: https://app.example.com/api/v2/auth/oidc/callback
Scopes: openid email profile
Post-login redirect: https://app.example.com/admin/oidc/complete
```

The required flow is login -> callback -> session -> logout -> token refresh. The current public repository gate validates configuration, state, nonce, PKCE, redirect allowlist, callback state consumption, token exchange wiring, ID token validation wiring, MMMail session issuance wiring, and trace wiring. A live Keycloak e2e must still be executed with a real IdP before BUS-01 can move from partial done to done.

Use `docs/commercial/oidc-live-evidence-template.md` for the redacted evidence package. The template covers BUS-01 live SSO flow evidence, OBS-01 `mmmail.oidc.callback` trace evidence, and GATE-01 live gate evidence. Completing the template requires a real Keycloak or approved OIDC IdP run; it must not be filled from local unit tests, mock IdP responses, or screenshots without trace correlation metadata.

## Domestic IdP Callback Notes

The v2.2 compatibility target is callback URL reachability, not deep organization sync:

| Provider | Callback URL check | Notes |
|---|---|---|
| 飞书 OIDC | `https://app.example.com/api/v2/auth/oidc/callback` | Verify the URL can be registered and reached by the IdP tenant |
| 钉钉 OIDC | `https://app.example.com/api/v2/auth/oidc/callback` | Verify redirect URI exact match behavior |
| 企业微信 OIDC | `https://app.example.com/api/v2/auth/oidc/callback` | Verify issuer and callback host policy before production use |

## Security Requirements

- `state` is single-use and expires quickly.
- `nonce` is stored with the state and must be matched during token validation.
- PKCE uses `S256`; `codeVerifier` is never sent to the browser after state creation.
- post-login redirect URI must match the configured allowlist exactly.
- `id_token` must be validated for signature, issuer, audience, nonce, subject, and email before session issuance.
- OIDC login only issues a MMMail session for an existing active local user matched by verified email.
- refresh and logout use the same `/api/v2/auth` session endpoints and refresh-token rotation path as password login.
- OIDC callback traces use `mmmail.oidc.callback`.
- OIDC failures must surface as explicit errors; do not add mock success or fallback login.
