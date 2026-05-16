# OIDC Live Evidence Template

This template defines the redacted evidence required before BUS-01, OBS-01, and GATE-01 can move from partial done to done. It is not a substitute for a real Keycloak or approved OIDC IdP run.

## Evidence Package

Create one redacted evidence package per backend commit:

- Backend commit SHA:
- Frontend commit SHA:
- MMMail deployment URL:
- Backend deployment mode:
- Keycloak or OIDC provider name:
- Provider version:
- Realm / tenant:
- OIDC issuer:
- Client ID:
- Registered callback URL:
- Test organization ID:
- Test user domain:
- Run started at:
- Run finished at:
- Operator:

Do not include client secrets, tokens, private keys, full cookies, customer data, or live vulnerability details.

## Required Flow

Record evidence for each step:

| Step | Required evidence |
|---|---|
| Configure OIDC | Redacted org config showing issuer, client ID, callback URL, scopes, and allowed post-login redirect |
| Start login | Backend response from `POST /api/v2/auth/oidc/login` with redacted authorization URL and state metadata |
| IdP login | Keycloak/OIDC login event for the test user, with timestamp and client ID |
| Callback success | Backend callback request result showing MMMail session issuance for an existing active local user |
| Session validation | Authenticated MMMail API call after callback, with redacted user identity and organization |
| Token refresh | Successful refresh through the existing `/api/v2/auth` refresh path |
| Logout | Successful logout through the existing `/api/v2/auth` logout path |
| Callback error path | Explicit failure evidence for invalid/replayed state, nonce mismatch, or rejected redirect |

## Trace Evidence

Attach redacted trace or log evidence for:

- `mmmail.oidc.callback` success path.
- `mmmail.oidc.callback` error path.
- Correlated request ID for the login callback.
- HTTP status and explicit failure propagation for the negative path.

Trace evidence must be tied to the same backend commit, provider, run timestamp, and test organization as the Required Flow section.

## Gate Evidence

The final package must include either:

- a CI or release-gate run URL that executed the live Keycloak/OIDC path, or
- a dedicated live OIDC gate run log with command, environment metadata, commit SHA, and exit code.

The gate must cover login, callback, MMMail session issuance, logout, token refresh, and at least one callback failure path.

## Non-Evidence

These do not satisfy the live evidence requirement:

- Local OIDC unit tests.
- Mock IdP responses.
- Browser screenshots without commit, timestamp, provider, and trace correlation metadata.
- `workflow_dispatch` dry runs.
- Logs that omit the callback success and error trace correlation.
- Public issue comments that expose security-sensitive details.
