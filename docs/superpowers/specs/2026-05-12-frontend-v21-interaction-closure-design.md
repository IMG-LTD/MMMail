# Frontend v2.1 Interaction Closure Design

## 1. Purpose

This specification defines the next frontend v2.1 slice after expanded browser visual QA coverage.

The current QA report proves that Drive, Docs, and Sheets routes render and can be captured, but three overlay scenarios still use broad page selectors rather than proving the specific design-image interactions:

- Drive share settings on `/drive`
- Docs share permissions on `/docs/demo-document`
- Sheets protected range on `/sheets/demo-sheet`

This slice upgrades those scenarios from static route evidence into real interaction closure: user-visible triggers, accessible overlay surfaces, deterministic state transitions, explicit error exposure, retry affordances, and browser QA assertions tied to the actual overlay elements.

## 2. Approved Direction

Approved option: design-image interaction closure first.

The slice focuses on three high-value UI states because they directly map to the v2.1 master requirement that design image groups under `docs/MMMail/UI` be represented by routes, states, panels, dialogs, and capability boundaries.

Non-selected alternatives:

- Large-file decomposition first: valuable, but less directly tied to visual evidence gaps.
- High-risk confirmation sweep first: valuable, but less focused than these three concrete design-image states.

## 3. Current Evidence Gap

The current report contains these weak overlay checks:

| Scenario | Current route | Current selectors | Gap |
| --- | --- | --- | --- |
| `drive-share-panel` | `/drive` | `.drive-surface`, `.drive-surface__table` | Proves the file table exists, not the share settings panel. |
| `docs-share-panel` | `/docs/demo-document` | `.docs-editor__actions`, `.docs-editor__panel` | Proves the editor shell exists, not the share permissions surface. |
| `sheets-protected-range` | `/sheets/demo-sheet` | `.sheets-editor__formula`, `.sheets-editor__side` | Proves the editor side area exists, not a protected range modal. |

The new implementation must make those scenario names true by asserting specific overlay selectors and visible state content.

## 4. Scope

In scope:

- Add or strengthen the Drive share settings surface.
- Add or strengthen the Docs share permissions surface.
- Add or strengthen the Sheets protected range surface.
- Add accessible triggers that browser QA can activate.
- Add loading, explicit failure, retry, and disabled states where an action implies asynchronous work.
- Update the visual QA scenario registry and contract test so these overlays are required.
- Update the generated visual QA report through the existing `visual:qa` command.

Out of scope:

- Backend API integration.
- Persisting real share or protection changes.
- Full visual parity review for every design image group.
- Adding Playwright, Puppeteer, or browser automation dependencies.
- Committing generated screenshots.
- Broad refactors unrelated to these three UI states.

## 5. Drive Share Settings Design

Route: `/drive`

Primary trigger:

- A visible share action on the selected or primary file row/card.
- QA selector: `.drive-share-trigger`

Overlay surface:

- Use the existing shared modal or drawer pattern already present in `frontend-v2`.
- QA selector: `.drive-share-panel`
- Required accessibility: `role="dialog"` and `aria-modal="true"` when rendered as a modal/drawer.

Content requirements:

- Selected file name and sensitivity/status badge.
- Member list with at least owner, editor, and viewer roles.
- Public link section with enabled/disabled state, expiration value, and copy link affordance.
- Permission controls for viewer/editor role changes.
- Revoke public link action presented as a high-risk operation.
- Activity or audit summary showing the last share-related event.

State requirements:

- Default state shows current share policy.
- Copy link action exposes copied feedback without pretending to persist remote data.
- Revoke action must require explicit confirmation before the revoked state is shown.
- Failure state must be visible with a retry action when QA activates the failure path.

## 6. Docs Share Permissions Design

Route: `/docs/demo-document`

Primary trigger:

- Share action in the editor action area.
- QA selector: `.docs-share-trigger`

Overlay surface:

- Use a compact permissions popover or drawer matching the document editor layout.
- QA selector: `.docs-share-panel`
- Required accessibility: dialog semantics if the surface traps focus or behaves modally.

Content requirements:

- Document title and current access summary.
- Invite input with visible validation for an invalid email-like value.
- Role selector for invited users.
- Link access selector with restricted, workspace, and public-read states.
- Existing collaborators list with role badges.
- Send invite action, retry action, and explicit error message area.

State requirements:

- Empty invite input must not silently send.
- Invalid invite input must surface validation text.
- Send failure must remain visible until dismissed or retried.
- Link permission changes must show pending or changed state explicitly, not a fake persisted success.

## 7. Sheets Protected Range Design

Route: `/sheets/demo-sheet`

Primary trigger:

- Protected range action near the formula bar or sheet tools.
- QA selector: `.sheets-protected-range-trigger`

Overlay surface:

- Use a modal-style protected range editor so the selected cell range remains visible behind it.
- QA selector: `.sheets-protected-range-modal`
- Required accessibility: `.mm-modal[role="dialog"][aria-modal="true"]` or the project equivalent.

Content requirements:

- Range input prefilled with a visible range such as `C2:D8`.
- Editor whitelist with named collaborators.
- Warning-only versus block-edit mode.
- Conflict notice when the range overlaps an existing protected range.
- Save action, retry action, and explicit error message area.

State requirements:

- Invalid range values must show validation before any save action.
- Overlap/conflict state must be visible in QA.
- Save failure must expose an error and retry control.
- The spreadsheet grid should visually mark the protected range while the modal is open.

## 8. Shared Interaction Rules

All three surfaces must follow the same interaction contract:

- Triggers are visible, keyboard reachable buttons.
- Overlays have stable selectors, accessible labels, and close actions.
- State text must fit within compact panels at desktop and mobile widths.
- Error states are explicit and testable; no swallowed errors, soft skip paths, or mock success strings.
- Actions that are only local UI demonstrations must be labeled by state, not represented as completed backend persistence.
- Existing shared components should be reused for modal/drawer, badges, tables, empty/error states, and progress indicators where available.
- If a touched view file is already oversized, extract only the local focused surface needed for this slice rather than expanding the monolith.

## 9. Browser QA Design

Update `frontend-v2/scripts/v21-browser-visual-qa.mjs`:

- `drive-share-panel` must click `.drive-share-trigger`.
- It must assert `.drive-share-panel`, the selected file title, member roles, public link controls, and revoke confirmation affordance.
- `docs-share-panel` must click `.docs-share-trigger`.
- It must assert `.docs-share-panel`, invite input, role selector, link permission controls, collaborator list, and error/retry area.
- `sheets-protected-range` must click `.sheets-protected-range-trigger`.
- It must assert `.sheets-protected-range-modal`, range input, editor whitelist, conflict notice, and retry area.

Screenshots:

- Keep writing screenshots under `.tmp/v21-browser-visual-qa/`.
- Keep screenshots out of git.
- Report entries must list the overlay-specific selectors, not broad page selectors.

## 10. Contract Test Design

Update `frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs` so it enforces:

- The three overlay scenarios still exist.
- Each scenario includes a click action or equivalent user interaction.
- Required selectors include `.drive-share-panel`, `.docs-share-panel`, and `.sheets-protected-range-modal`.
- Broad page-only selectors are not the only proof for these scenario ids.
- The existing report path remains `docs/superpowers/progress/v21-browser-visual-qa-report.md`.

## 11. Acceptance Criteria

This slice is complete when:

- This design is committed.
- The implementation plan references this design and the v2.1 master spec.
- Drive, Docs, and Sheets expose the approved overlay triggers and surfaces.
- Overlay states cover default, validation or warning, explicit failure, and retry where applicable.
- `pnpm --dir frontend-v2 visual:qa` passes and updates the report with overlay-specific evidence.
- `pnpm --dir frontend-v2 test` passes.
- `pnpm --dir frontend-v2 typecheck` passes.
- `pnpm --dir frontend-v2 build` passes.

## 12. Self-check

- Does this design preserve the v2.1 master acceptance criteria? Yes.
- Does it focus on a slice that one implementation plan can cover? Yes.
- Does it avoid fake backend success paths and silent fallbacks? Yes.
- Does it identify stable selectors for browser QA? Yes.
- Does it avoid unrelated broad refactors? Yes.
- Does it provide a clear handoff to `writing-plans`? Yes.
