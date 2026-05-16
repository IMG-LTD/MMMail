# v2.1.2 Module Design Coverage

This document closes the documentation requirement from `docs/v212-migration-spec.md §26.6`: every v2.1.2 new module must have an explicit design record under `docs/superpowers/specs/`.

The design rule is strict:

- No mock success paths.
- No silent fallback.
- Existing backend services, controllers, mappers, and entitlement gates remain the source of truth.
- P2 items may be route or sub-spec placeholders only when the migration spec explicitly allows them.

## Runtime contracts

Each module section records the runtime contract that the frontend or backend implementation must call. Runtime contracts must be real endpoints or explicit unavailable states; they must not return fake success payloads.

## State and error model

Each module must expose loading, empty, error, action feedback, and confirmation states where the workflow mutates data. Backend failures should surface the documented error code and trace details instead of being swallowed.

## Access and audit

Route meta, `EntitlementGate`, backend entitlement or role annotations, and audit events must agree. Sensitive flows must not log private key material, TOTP seeds, wallet secrets, encrypted share keys, or mail credentials.

## Verification

The minimum verification set for every module is a contract test for route/service wiring, a UI binding test for the main workflow, and backend integration coverage for new server endpoints. P2 placeholders must have a spec or route placeholder test that prevents silent removal.

## Module coverage

### Wallet

Scope: P1 encrypted wallet accounts, transactions, mail transfer, and reconciliation workflows.

- Access: `entitlement: WALLET`, `featureFlag: feat.wallet.enabled`, organization required, premium gated.
- Runtime: `/api/v1/wallet` account, transfer, execution, and reconciliation endpoints.
- State: transfer forms use explicit confirmation, execution status steps, insufficient-balance errors, and reconciliation empty states.
- Security: wallet private key or recovery material must never be logged or sent in plaintext.
- Verification: wallet service contract, route meta gate, write-after-refetch store behavior, and transfer UI binding.

### VPN

Scope: P1 VPN server directory, quick connect, profile CRUD, session history, and settings.

- Access: `entitlement: VPN`, `featureFlag: feat.vpn.enabled`, premium gated.
- Runtime: `/api/v1/vpn` servers, settings, profiles, current session, history, quick-connect, connect, and disconnect.
- State: connection state is explicit: disconnected, connecting, connected, degraded, and failed.
- Verification: profile CRUD, quick connect, server connect, disconnect, and settings persistence contract.

### Meet

Scope: P1 meeting room lifecycle, media controls, guest approval, public entry, and quality monitoring.

- Access: `entitlement: MEET`, premium gated for private rooms.
- Runtime: `/api/v1/meet` rooms, participants, guest requests, signals, quality; public entry uses `/api/v1/public/meet`.
- State: join code rotation invalidates old codes, media controls show immediate local state, guest decisions clear local session data.
- Verification: host plus guest room flow, signal APIs, quality reporting, and public join rejection path.

### Contacts

Scope: P0 standalone contacts module with grouping, favorites, duplicate merge, import, and export.

- Runtime: `/api/v1/contacts` and `/api/v1/contact-groups`.
- State: CSV import shows parsing, validation, duplicate, and committed counts; empty groups use empty state.
- Workflow: favorite toggle and duplicate merge must refetch affected rows.
- Verification: contacts service API, route entry, CSV import, export, favorite, and merge UI binding.

### SimpleLogin

Scope: P2 relay overview and organization relay policy management retained as a placeholder-capable module.

- Access: `featureFlag: feat.simplelogin.enabled`, `entitlement: SIMPLE_LOGIN`, organization required.
- Runtime: `/api/v1/simplelogin` overview and relay policy endpoints.
- State: disabled feature flag shows an entitlement fallback, not a blank page.
- Verification: service and route contracts are retained even if full workflow rolls to v2.1.3.

### Standard Notes

Scope: P2 notes overview, folders, notes, export, and checklist item toggle.

- Access: `featureFlag: feat.notes.enabled`.
- Runtime: `/api/v1/standard-notes` overview, folders, notes, export, and checklist endpoints.
- State: checklist rows update optimistically only after backend success; export failures surface trace details.
- Verification: route placeholder and service contract keep the module visible for v2.1.2.

### Authenticator

Scope: P1 TOTP / Authenticator standalone page with entry CRUD, PIN gate, QR import, and backup.

- Runtime: `/api/v1/authenticator` entries, code, import, export, backup, security, PIN, and QR endpoints.
- Security: TOTP seed and backup payloads must not appear in logs or frontend persistent storage.
- State: PIN challenge blocks code reveal; QR import reports invalid image and duplicate-account errors.
- Verification: TOTP grid, PIN verification, QR import, backup export/import, and route contracts.

### Mail Rules

Scope: P1 mail rules editor, condition/action builder, preview, and enable toggle.

- Runtime: `/api/v1/mail/rules` list, detail, create, update, delete, preview, and enable toggle endpoints.
- State: preview output is read-only until saved; conflicting rules use explicit error copy.
- Verification: rule CRUD route, preview binding, and enable toggle persistence.

### Mail Drag

Scope: P0 mail message multi-select drag to folder and label targets.

- Runtime: existing mail move and label action endpoints.
- State: drag hover target must remain stable; folder and label operations show partial-failure feedback.
- Verification: drag gesture, selected message IDs, folder destination, label destination, and refresh behavior.

### Drive Versions

Scope: P1 Drive version history, restore, and text compare workflow.

- Runtime: Drive file version list, restore, and compare endpoints.
- State: version history is chronological; restore requires confirmation; compare is lazy-loaded for text assets.
- Verification: version history route, restore confirmation, and compare navigation contract.

### Drive E2EE Share

Scope: P1 encrypted Drive share creation and public readable-share access.

- Runtime: Drive readable-share metadata and token endpoints.
- Security: share links keep key material fragment-only; the server never receives fragment key material.
- State: public share page reads token metadata and keeps unavailable or expired token states explicit.
- Verification: encrypted share creation, public access, no server upload of fragment key material.

### Domain

Scope: P1 custom domain management for settings.

- Runtime: `/api/v1/domains` create, list, verify, dns-records, and diagnostics endpoints.
- State: DNS rows show pending, verified, failed, and stale diagnostic states.
- Verification: dns-records rendering, diagnostics path, verify action, and backend contract coverage.

### Web Push

Scope: P1 Web Push subscriptions and test delivery.

- Runtime: `/api/v1/web-push` vapid-public-key, subscriptions, register, delete, and test endpoints.
- State: permission denied, unsupported browser, unsubscribed, subscribed, and test-delivery failure are distinct states.
- Verification: vapid-public-key retrieval, subscriptions CRUD, test delivery, and rate limit coverage.

### Admin Billing

Scope: P1 admin billing offer, quote, checkout draft, payment method, and subscription management.

- Runtime: `/api/v1/billing` offer, quote, checkout, payment, and subscription endpoints.
- Access: billing routes require billing admin role and premium gate alignment.
- State: quote preview is non-mutating; subscription changes require confirmation.
- Verification: route role meta, quote preview, draft checkout, and payment method binding.

### Community

Scope: P2 community posts, comments, topics, reactions, bookmarks, reports, and moderation.

- Access: `featureFlag: feat.community.enabled`.
- Runtime: community posts, comments, topics, reactions, bookmarks, views, reports, and moderation endpoints.
- State: reports enter moderation queue; deleted parent comments keep a visible placeholder.
- Verification: posts CRUD, reports workflow, engagement counters, and placeholder route retention.

### Search

Scope: P1 global search index, read path, reindex jobs, and command palette suggestions.

- Runtime: `/api/v1/search` query, suggestions, facets, navigation, and reindex endpoints.
- Access: every result passes permission filter before response serialization.
- State: reindex jobs expose queued, running, succeeded, failed, and cancelled states.
- Verification: six content types, permission filter, reindex progress, and suggestion latency contract.

### Command Panel

Scope: P1 command catalog, pins, recents, quick search, and command run entry points.

- Runtime: `/api/v2/command-center` catalog, pins, recents, quick search, runs, cancel, and retry endpoints.
- State: command run output streams are explicit; cancel and retry require status validation.
- Verification: catalog, recents, pin toggle, quick search, and rate-limited command run contract.

### Calendar Subscriptions

Scope: P2 CalDAV and ICS calendar subscriptions retained for route and API readiness.

- Runtime: calendar subscriptions import, sync, and event mapping endpoints.
- State: subscription sync exposes imported, skipped, failed, and stale states.
- Verification: CalDAV and ICS route/API placeholders remain wired while full delivery can roll forward.

### Calendar RRULE

Scope: P1 recurring calendar events and scoped update behavior.

- Runtime: calendar event create/update accepts RRULE and exception data.
- State: recurrence preview must make thisAndFollowing scope clear before mutation.
- Verification: recurrence expansion, exception handling, thisAndFollowing update, and ICS export behavior.

### Mail External Accounts

Scope: P2 IMAP and SMTP external accounts with server testing and sync state.

- Runtime: external accounts create, update, delete, test, sync, and server metadata endpoints.
- Security: external accounts credentials are encrypted at rest and never logged.
- Verification: IMAP test, SMTP test, sync trigger, credential redaction, and route retention.

### Collab CRDT

Scope: P1 CRDT infrastructure and Docs editor realtime collaboration.

- Runtime: collab room, update, snapshot, awareness, and WebSocket endpoints.
- State: reconnect replays missing updates from snapshot and update log; awareness expires after heartbeat timeout.
- Verification: two-tab edit, snapshot recovery, awareness broadcast, and offline reconnect behavior.

### Collab Sheets Board Placeholder

Scope: P2 Sheets and board collaboration integration is explicitly deferred.

- Placeholder: `2026-05-15-collab-sheets-board-design.md` must exist before v2.1.3 implementation starts.
- Release rule: v2.1.2 may ship only a placeholder sub-spec and routes where required by the migration spec.
- State: unavailable state says the feature is planned, not silently disabled.
- Verification: placeholder contract prevents accidental deletion.

### Sheets Formula

Scope: P2 server-side spreadsheet formula evaluation and dependency analysis.

- Runtime: formula evaluate, dependency graph, and recalculate endpoints.
- State: formula errors distinguish parse errors, circular references, missing sheets, and permission failures.
- Verification: formula evaluation, dependency graph, recalculate, and route placeholder coverage.

### Collaboration Board

Scope: P1 board card drag persistence.

- Runtime: collaboration board read and task move endpoints.
- State: task move uses lexorank ordering and returns the committed board position.
- Verification: task move, board refresh, concurrent position stability, and no negative ordering gaps.

### Notification Realtime

Scope: P0 WebSocket notification fanout and disconnected replay.

- Runtime: notification realtime WebSocket, heartbeat, fanout, and replay API.
- State: clients track since cursor for replay and expose disconnected, reconnecting, and caught-up states.
- Verification: WebSocket fanout, heartbeat timeout, since cursor replay, and page cleanup on unmount.

### Login Security

Scope: P1 login anomaly detection, lock countdown, user acknowledgement, and admin action workflow.

- Runtime: auth login risk response plus security events user/admin endpoints.
- State: login anomaly risk separates low, medium, and high decisions; lock countdown is driven by backend time.
- Verification: login anomaly, risk challenge, security events acknowledgement, admin action, and audit coverage.
