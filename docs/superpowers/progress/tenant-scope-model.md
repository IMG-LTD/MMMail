# Tenant Scope Model

## Request Headers
- `X-MMMAIL-ORG-ID` — active organization id when the actor is inside an organization scope.
- `X-MMMAIL-SCOPE-ID` — active scope id when the actor is in a narrower workspace scope.

## Required Propagation
1. HTTP request headers -> request context holder.
2. Request context holder -> MDC and trace tags.
3. Request context holder -> audit events, jobs, notifications, and public-share access records.

## Hard Rules
- Product access checks never guess tenant identity.
- Background jobs must persist org and scope context explicitly.
- Frontend scope switching must update both headers together.
