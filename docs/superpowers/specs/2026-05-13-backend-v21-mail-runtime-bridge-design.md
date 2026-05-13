# Backend v2.1 Mail Runtime Bridge Design

Date: 2026-05-13

## Context

The previous backend v2.1 runtime slices completed API contract publication, access entitlement gates, Calendar runtime bridge, Drive runtime bridge, and Docs/Sheets runtime bridge. The largest remaining core-workspace runtime gap is Mail.

Frontend v2.1 already calls `/api/v2/mail/*` from `frontend-v2/src/service/api/mail.ts` and the Mail workbench. The backend already has mature real Mail behavior under `/api/v1/mails/*` through `MailController`, `MailService`, sender identity, recipient trust, drafts, folder listing, conversation detail, send, and batch actions.

## Decision

Implement one focused slice: `backend-v21-mail-runtime-bridge`.

This slice adds a thin v2 Mail controller that maps the v2.1 Mail contract to existing real Mail runtime services. It must not introduce mock data, fake success responses, silent fallback to v1 URLs, or broad Mail feature redesign.

## Scope

In scope:

- Add `V21MailController` under `/api/v2/mail`.
- Reuse existing Mail runtime services and DTOs where the v2 route shape matches.
- Add small v2 request records only when path-level v2 semantics cannot be represented by an existing DTO directly.
- Add focused backend integration coverage for v2 Mail create draft, update draft, send, folder reads, thread read, contacts, recipient trust, batch action, and Premium rule gate.
- Update v2.1 progress after implementation.

Out of scope:

- v2 attachment upload or download endpoints.
- New mail rule creation or editing.
- Advanced search DSL beyond existing folder and keyword filters.
- SMTP gateway redesign.
- Public mail share runtime changes.
- Any Pass, Collaboration, Notifications, Command Center, Admin, Billing, Settings, or Auth runtime bridge.

## Architecture

The v2 Mail controller is an API compatibility adapter:

- It owns the `/api/v2/mail/*` route surface.
- It resolves the authenticated user through the existing security utilities.
- It maps v2 query and path parameters to existing Mail service calls.
- It returns existing Mail VO models through `Result.success`.
- It relies on the existing v2 access gate for authentication, permissions, entitlement checks, and unknown route handling.

Business behavior remains in `MailService` and its collaborators. The bridge must not duplicate send rules, draft persistence, folder ownership checks, conversation access checks, outbound delivery behavior, audit behavior, sender identity logic, recipient trust logic, or batch-action rules.

## Route Mapping

| v2 route | Runtime mapping | Entitlement |
| --- | --- | --- |
| `GET /api/v2/mail/folders` | Build folder summary from existing Mail folder/stat behavior | Community |
| `GET /api/v2/mail/messages` | Dispatch by `folder` query to existing folder list methods | Community |
| `GET /api/v2/mail/threads/{id}` | Read existing conversation/detail state for the requested id | Community |
| `POST /api/v2/mail/drafts` | `MailService.saveDraft(userId, SaveDraftRequest, ip)` | Community |
| `PATCH /api/v2/mail/drafts/{id}` | `MailService.saveDraft` with the path id as `draftId` | Community |
| `POST /api/v2/mail/send` | `MailService.send(userId, SendMailRequest, ip, publicBaseUrl)` | Community |
| `POST /api/v2/mail/messages/bulk-action` | `MailService.applyBatchAction(userId, BatchMailActionRequest, ip)` | Community |
| `GET /api/v2/mail/contacts` | Sender identities or recipient-trust preview by `capability` query | Community |
| `GET /api/v2/mail/rules` | Blocked before controller by Premium gate | Premium |

## Folder Semantics

`GET /api/v2/mail/messages` accepts `folder`, `page`, `size`, `keyword`, and supported triage filters. Folder values are explicit and case-insensitive:

- `inbox` maps to `MailService.listInbox`.
- `unread` maps to `MailService.listUnread`.
- `sent`, `drafts`, `archive`, `spam`, `trash`, `outbox`, `scheduled`, and `snoozed` map to `MailService.listFolder`.
- `starred` maps to `MailService.listStarred`.

Unknown folder values return an explicit invalid-argument error. They must not silently fall back to inbox.

## Contacts Semantics

`GET /api/v2/mail/contacts` has two explicit modes:

- Without `capability=recipient-trust`, it returns existing sender identities from `MailService.listSenderIdentities`.
- With `capability=recipient-trust`, it requires `toEmail`, accepts optional `fromEmail`, and returns existing `MailService.previewRecipientE2eeStatus` output.

The response shape can remain close to existing VO output because the frontend v2 client already normalizes payloads defensively. The backend still must return real service output, not hard-coded readiness text.

## Draft and Send Semantics

`POST /api/v2/mail/drafts` uses the existing `SaveDraftRequest` body. `PATCH /api/v2/mail/drafts/{id}` uses the same body but treats the path id as authoritative. If the body also contains a different `draftId`, the controller returns an explicit invalid-argument error.

`POST /api/v2/mail/send` uses the existing `SendMailRequest`. The existing Mail service remains responsible for:

- idempotency,
- internal delivery,
- outbound SMTP delivery,
- scheduled send,
- labels,
- encrypted body payloads,
- audit and sender IP.

## Error Handling

No silent fallback is introduced.

- Missing or invalid authentication is handled by existing security infrastructure.
- Unknown v2 Mail routes are handled by `V21ApiAccessGateInterceptor`.
- Unknown folders return `INVALID_ARGUMENT`.
- Malformed numeric ids return explicit request errors.
- `GET /api/v2/mail/rules` remains Premium-gated in Community with `V2_ENTITLEMENT_REQUIRED`.
- Existing service-level not-found, ownership, validation, E2EE, outbound delivery, and state errors remain the source of truth.

## Tests

Add `BackendV21MailRuntimeBridgeTest` before implementation as a red test. It should cover:

- Register two real users.
- Create a draft through `POST /api/v2/mail/drafts`.
- Update that draft through `PATCH /api/v2/mail/drafts/{id}`.
- Send mail through `POST /api/v2/mail/send`.
- Read sender state through `GET /api/v2/mail/messages?folder=sent`.
- Read receiver state through `GET /api/v2/mail/messages?folder=inbox`.
- Read thread/detail state through `GET /api/v2/mail/threads/{id}`.
- Read sender identities through `GET /api/v2/mail/contacts`.
- Read recipient trust through `GET /api/v2/mail/contacts?capability=recipient-trust`.
- Apply a real batch action through `POST /api/v2/mail/messages/bulk-action`.
- Verify an unknown folder returns `INVALID_ARGUMENT`.
- Verify `GET /api/v2/mail/rules` returns `V2_ENTITLEMENT_REQUIRED` for Community runtime.

Regression verification must include:

- `BackendV21MailRuntimeBridgeTest`
- `MailAttachmentIntegrationTest`
- `SmtpOutboundDeliveryIntegrationTest`
- `BackendV21AccessEntitlementGatesTest`
- `BackendV21ApiContractCatalogTest`
- `pnpm --dir frontend-v2 test`
- `pnpm --dir frontend-v2 typecheck`
- `pnpm --dir frontend-v2 build`

Backend test commands must use the repository-required `timeout 60s` wrapper.

## Acceptance Criteria

- Community v2 Mail routes use real persisted Mail runtime data.
- The Mail workbench can use `/api/v2/mail/*` without relying on v1 URLs or local fake state.
- Draft save, draft update, send, folder reads, contact reads, recipient trust, and batch actions are covered by backend integration tests.
- Premium Mail rules remain an explicit locked boundary in Community.
- Existing v1 Mail attachment and SMTP delivery regressions continue to pass.
- `docs/superpowers/progress/v21-implementation-progress.md` records the completed slice and verification evidence after implementation.

## Spec Self-Check

- Marker scan: no unresolved marker or incomplete section remains.
- Consistency check: route mappings match the current v2.1 contract catalog and existing Mail service capabilities.
- Scope check: this is one implementable runtime bridge slice; attachments, rules mutation, public share, and non-Mail modules are explicitly excluded.
- Ambiguity check: `PATCH /api/v2/mail/drafts/{id}` path id is authoritative and body/path id conflicts are explicit invalid-argument errors.
