# Frontend v2.1 Core Workbench Closure Design

## 1. Purpose

This specification defines the next frontend v2.1 slice after the Drive, Docs, and Sheets interaction closure work.

The current implementation has broad route coverage and several overlay states, but the three largest remaining product workbenches still combine too much layout, state, styling, and interaction logic in single files:

| Module | Current file | Current size | Risk |
| --- | --- | ---: | --- |
| Mail | `frontend-v2/src/views/app/MailSurfaceView.vue` | 1134 lines | Hard to add compose, trust, attachment, and retry states without expanding a monolith. |
| Calendar | `frontend-v2/src/views/app/CalendarView.vue` | 1085 lines | Event editing, resource conflict, and responsive states are coupled to the board. |
| Pass | `frontend-v2/src/views/app/PassSectionView.vue` | 1014 lines | Vault, item detail, secure links, risk monitor, and high-risk actions are entangled. |

This slice closes those three core workbenches against the v2.1 UI design images, while bringing touched files back under the repository 500-line limit.

## 2. Approved Direction

Approved option: Mail, Calendar, and Pass workbench closure first.

This direction was selected because it addresses the largest remaining v2.1 product surfaces and the most visible engineering debt at the same time:

- Design-image parity for the three main unfinished workbenches.
- Real user-facing interaction states instead of static shells.
- File boundaries that make the next v2.1 slices cheaper and safer.
- Browser QA evidence tied to actual panels, dialogs, and action states.

Non-selected alternatives:

- Horizontal high-risk-action sweep first: useful, but less aligned with the remaining oversized product surfaces.
- Global visual polish first: useful, but likely to create rework before the main workbench boundaries are fixed.

## 3. Design Source Analysis

The authoritative sources remain the v2.1 design images under `docs/MMMail/UI`.

Representative images inspected for this slice:

| Module | Source image | Relevant design signals |
| --- | --- | --- |
| Mail | `docs/MMMail/UI/邮件/邮件-设计概览.png` | Left app nav, KPI strip, folder rail, message list, thread reader, and right compose panel with attachment toolbar. |
| Calendar | `docs/MMMail/UI/日历/日历概览.png` | Range controls, resource filters, week board, right event editor drawer, and conflict status card. |
| Pass | `docs/MMMail/UI/Pass/Pass概览.png` | Vault/category rail, item list, credential detail panel, share settings modal, security health, and recent-risk cards. |

Branding rule:

- The visual structure, density, and interaction hierarchy should follow the images.
- Placeholder names such as `Nexa Workspace` must remain replaced by MMMail product branding and configurable tenant data.

## 4. Current Progress Baseline

Already complete before this slice:

- v2.1 route coverage and browser visual QA baseline.
- Public/auth/share/system route evidence.
- Workspace command palette, quick-create modal, and theme drawer evidence.
- Drive share panel, Docs share panel, and Sheets protected range modal interaction closure.
- Browser QA report with 63 screenshots and overlay-specific selectors for Drive, Docs, and Sheets.

Remaining gap addressed here:

- Mail currently proves inbox and compose shell existence, but not full compose validation, trust warning, attachment state, discard confirmation, or send retry.
- Calendar currently proves board existence, but not event drawer editing, resource conflict, shared meeting status, or save retry.
- Pass currently proves vault, secure links, and monitor routes, but not secret reveal, secure-link settings, rotate/revoke confirmation, risk detail, or retry.

## 5. Scope

In scope:

- Refactor the three oversized workbench files into focused local components and helpers.
- Preserve existing routes and v2.1 visual language while improving design-image alignment.
- Add or strengthen real interaction states for Mail, Calendar, and Pass.
- Add accessible triggers, panels, dialogs, confirmation flows, and explicit error/retry states.
- Update visual QA scenarios, contract tests, and the generated QA report.
- Keep touched Vue files at or below 500 lines.

Out of scope:

- Backend API integration or persistence.
- New entitlement model logic beyond the current v2.1 visual boundary language.
- New product areas outside Mail, Calendar, and Pass.
- Committing generated screenshots.
- Broad visual redesign unrelated to the inspected design-image patterns.

## 6. Shared Workbench Rules

All three modules must follow these rules:

- Use a dense product-workbench layout, not a marketing page.
- Preserve the app shell and top-bar conventions already used by frontend v2.
- Use cards only for repeated items, detail panels, and modal surfaces.
- Keep controls stable in size so hover, validation, and loading states do not shift the layout.
- Use icon buttons where the action is a familiar tool action.
- Use explicit local UI states for demo-only actions; do not pretend backend persistence completed.
- Let failures surface as visible error text and retry controls.
- Require confirmation for high-risk actions before showing the changed state.
- Keep selectors stable and specific for browser QA.
- Extract CSS and pure helpers when a touched file would exceed 500 lines.

## 7. Mail Workbench Design

Primary routes:

- `/mail/inbox`
- `/mail/compose`
- `/mail/conversations/:threadId`

Target layout:

- Desktop: left folder/filter rail, middle message list, right thread reader, with compose as a right-side panel or focused compose route.
- Tablet: folder rail compresses; thread reader remains visible when space allows.
- Mobile: list and detail become a single-column switch; no squeezed three-column layout.

Required component boundaries:

- `MailSurfaceView.vue`: route-level orchestrator only, at or below 500 lines.
- `mail/MailFolderRail.vue`: folders, labels, folder counts, storage/security mini panels.
- `mail/MailMessageList.vue`: search, filters, labels, message rows, selection state.
- `mail/MailThreadReader.vue`: selected conversation, attachments, thread actions.
- `mail/MailComposePanel.vue`: recipients, subject, body, attachment controls, send actions.
- `mail/MailTrustPanel.vue`: low-trust recipient, external-domain, and security prompts.
- Mail helpers and styles should move to local `mail-*` helper or CSS files when needed.

Required interactions:

- Compose draft validation for missing recipients, subject, or body.
- Low-trust or external-recipient warning before sending.
- Attachment strip with at least one visible attachment and remove/download actions.
- Send failure state with retry action.
- Discard-draft confirmation before closing an unsent draft.

Stable QA selectors:

- `.mail-workbench`
- `.mail-folder-rail`
- `.mail-message-list`
- `.mail-thread-reader`
- `.mail-compose-trigger`
- `.mail-compose-panel`
- `.mail-trust-panel`
- `.mail-attachment-strip`
- `.mail-send-error`
- `.mail-send-retry`
- `.mail-discard-confirmation`

## 8. Calendar Workbench Design

Primary routes:

- `/calendar`
- `/calendar/week`
- `/calendar/rooms`
- `/calendar/resources`

Target layout:

- Desktop: left range/resource sidebar, central week board, right event editor drawer.
- Tablet: right drawer overlays the board when editing.
- Mobile: board and event details become separate states.

Required component boundaries:

- `CalendarView.vue`: route-level orchestrator only, at or below 500 lines.
- `calendar/CalendarFilterSidebar.vue`: mini calendar, owned calendars, room/resource filters.
- `calendar/CalendarBoard.vue`: week/day grid, event blocks, current range controls.
- `calendar/CalendarEventDrawer.vue`: create/edit event form.
- `calendar/CalendarConflictPanel.vue`: resource availability and collision state.
- Calendar fixtures, helpers, and styles should move to local files when needed.

Required interactions:

- New or edit event drawer with title, date, time, location, attendees, visibility, meeting mode, reminder, and notes.
- Room or resource conflict warning when a selected slot overlaps existing usage.
- Shared meeting state that shows attendees and shared calendar context.
- Save failure state with retry action.
- Cancel action that preserves visible unsaved-change state until confirmed or dismissed.

Stable QA selectors:

- `.calendar-workbench`
- `.calendar-filter-sidebar`
- `.calendar-board`
- `.calendar-event-trigger`
- `.calendar-event-drawer`
- `.calendar-conflict-panel`
- `.calendar-resource-state`
- `.calendar-save-error`
- `.calendar-save-retry`

## 9. Pass Workbench Design

Primary routes:

- `/pass`
- `/pass/secure-links`
- `/pass/monitor`

Target layout:

- Desktop: vault/category rail, item list, item detail/security actions, and risk monitor cards.
- Tablet: detail panel remains available but can stack under the list.
- Mobile: list, detail, and dialogs become focused single-column states.

Required component boundaries:

- `PassSectionView.vue`: route-level orchestrator only, at or below 500 lines.
- `pass/PassVaultRail.vue`: vaults, categories, item counts, local security status.
- `pass/PassItemList.vue`: search, filters, item rows, risk tags.
- `pass/PassItemDetail.vue`: credential details, copy, edit, reveal, rotate, revoke.
- `pass/PassShareSettingsModal.vue`: secure-link share settings and expiration.
- `pass/PassRiskMonitorPanel.vue`: health chart, recent alerts, risk detail.
- Pass fixtures, helpers, and styles should move to local files when needed.

Required interactions:

- Secure-link share settings with recipients, permission, expiration, and save failure/retry.
- Secret reveal and hide controls with clear visible security semantics.
- Rotate secret confirmation before showing rotated state.
- Revoke sharing confirmation before showing revoked state.
- Risk alert detail panel with remediation action and retry state.
- Delete or destructive actions must require confirmation.

Stable QA selectors:

- `.pass-workbench`
- `.pass-vault-rail`
- `.pass-item-list`
- `.pass-item-detail`
- `.pass-secret-reveal`
- `.pass-secure-link-trigger`
- `.pass-share-settings-modal`
- `.pass-rotate-confirmation`
- `.pass-revoke-confirmation`
- `.pass-risk-monitor-panel`
- `.pass-risk-detail`
- `.pass-action-error`
- `.pass-action-retry`

## 10. Browser QA Design

Update the v2.1 visual QA scenario registry and contract tests so this slice adds interaction evidence, not only route evidence.

Required new or strengthened scenarios:

| Scenario | Route | Required action | Required evidence |
| --- | --- | --- | --- |
| `mail-compose-security` | `/mail/compose` | Trigger validation, trust warning, or retry state through a real button path. | Compose panel, trust panel, attachment strip, send error, retry, discard confirmation. |
| `mail-thread-workbench` | `/mail/inbox` | Select or open a thread if required by the current implementation. | Folder rail, message list, thread reader, attachment area. |
| `calendar-event-drawer` | `/calendar` or `/calendar/week` | Open an event editor from the board or create action. | Event drawer, attendee/resource fields, conflict panel, save error, retry. |
| `pass-secret-actions` | `/pass` | Reveal a secret and trigger rotate or revoke confirmation. | Secret reveal control, item detail, confirmation dialog, action error/retry. |
| `pass-secure-link-settings` | `/pass/secure-links` | Open share settings. | Share settings modal, recipients, expiration, permission, error/retry. |
| `pass-risk-detail` | `/pass/monitor` | Open a risk alert detail. | Risk monitor panel, risk detail, remediation action, retry. |

Screenshots remain written under `.tmp/v21-browser-visual-qa/` and must stay out of git.

## 11. Contract Test Design

Update `frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs` so it enforces:

- The new Mail, Calendar, and Pass scenario ids exist.
- Each interaction scenario has a click action or equivalent user action.
- Required selectors are specific to the target surface and not only broad page shells.
- Existing Drive, Docs, Sheets, Settings, public/share/system evidence remains intact.
- The report path remains `docs/superpowers/progress/v21-browser-visual-qa-report.md`.

## 12. Validation Requirements

Implementation is not complete until these commands pass from the repository root:

```bash
timeout 60s pnpm --dir frontend-v2 test
timeout 60s pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
pnpm --dir frontend-v2 visual:qa
```

The final `visual:qa` run must regenerate the report with the new Mail, Calendar, and Pass interaction evidence.

## 13. Acceptance Criteria

This slice is complete when:

- This design is committed.
- The implementation plan references this design and the v2.1 master spec.
- `MailSurfaceView.vue`, `CalendarView.vue`, and `PassSectionView.vue` are each at or below 500 lines.
- Mail exposes the approved compose, trust, attachment, discard, failure, and retry states.
- Calendar exposes the approved event drawer, resource conflict, shared meeting, failure, and retry states.
- Pass exposes the approved secure-link, secret reveal, rotate/revoke confirmation, risk detail, failure, and retry states.
- Browser QA includes the approved scenario ids and overlay/panel-specific selectors.
- Required tests, typecheck, build, and visual QA pass with fresh output.

## 14. Self-check

- Does this design preserve the v2.1 master acceptance criteria? Yes.
- Does it focus on one implementable slice? Yes: Mail, Calendar, and Pass core workbench closure.
- Does it explicitly reference the inspected UI design groups? Yes.
- Does it avoid backend persistence claims and silent fallback behavior? Yes.
- Does it define file boundaries before adding more behavior? Yes.
- Does it give `writing-plans` enough detail for a step-by-step implementation plan? Yes.
