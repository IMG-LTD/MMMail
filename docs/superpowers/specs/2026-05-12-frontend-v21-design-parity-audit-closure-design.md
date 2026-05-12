# Frontend v2.1 Design Parity Audit Closure Design

## 1. Purpose

This specification defines the next v2.1 frontend slice after the final public/auth visual parity closure.

The current frontend has route metadata, design tokens, shared components, browser visual QA, core workbench interactions, and a public/auth/share/system parity register. The remaining closure gap is broader design-image parity discipline: every major UI group needs an explicit source design, screenshot evidence, status, and owner note so visual approval does not depend on informal memory.

This slice turns `docs/MMMail/UI/*` design references and `.tmp/v21-browser-visual-qa/*` browser screenshots into a full UI-group risk register, then fixes only small, concrete `must-fix` visual or selector issues discovered by that audit.

## 2. Current Progress Baseline

Latest progress is recorded in:

- `docs/superpowers/progress/v21-implementation-progress.md`
- `docs/superpowers/progress/v21-visual-parity-risk-register.md`
- `docs/superpowers/progress/v21-browser-visual-qa-report.md`

Current baseline:

- Latest frontend implementation commit: `0ba10014 feat(frontend-v2): close v2.1 public auth visual parity`
- Latest progress commit: `26dbaccf docs(frontend-v2): update v2.1 implementation progress`
- Browser visual QA screenshot count: `69`
- Current parity register coverage: Public/Auth/Share/System only.
- No known Public/Auth file-size violation remains.

## 3. Design Sources

Primary design inputs:

- `docs/MMMail/UI/首页/工作台-设计概览.png`
- `docs/MMMail/UI/邮件/邮件-设计概览.png`
- `docs/MMMail/UI/日历/日历概览.png`
- `docs/MMMail/UI/云盘/云盘概览.png`
- `docs/MMMail/UI/文档/文档概览.png`
- `docs/MMMail/UI/Sheets和labs/表格概览.png`
- `docs/MMMail/UI/Pass/Pass概览.png`
- `docs/MMMail/UI/Collaboration/协作概览.png`
- `docs/MMMail/UI/CommandCenter/命令概览.png`
- `docs/MMMail/UI/Notifications/通知概览.png`
- `docs/MMMail/UI/Admin/管理后台.png`
- `docs/MMMail/UI/Setting/设置概览.png`
- Existing browser QA evidence under `.tmp/v21-browser-visual-qa/`

The design images are reference direction, not literal brand-copy locks. Historical sample brands such as Nexa or Nova must not replace MMMail product language.

## 4. Scope

In scope:

- Expand `docs/superpowers/progress/v21-visual-parity-risk-register.md` from Public/Auth/Share/System to all UI groups covered by visual QA.
- Add an auditable source design and QA screenshot reference for each UI group.
- Classify each group as `aligned`, `acceptable-delta`, or `must-fix`.
- Implement small, concrete fixes only for discovered `must-fix` items.
- Refresh browser visual QA and progress artifacts after implementation.
- Keep every touched implementation file at or below 500 lines.

Out of scope:

- Rewriting the full visual system.
- Replacing the browser visual QA runner.
- Backend API, auth, billing, or persistence changes.
- Committing `.tmp` screenshots.
- Adding fake DOM, mock success states, silent fallbacks, or scenario skips just to pass QA.
- Large editor refactors not required by a concrete `must-fix` parity item.

## 5. Audit Model

The parity audit is a four-step loop:

1. Map a UI group to its primary `docs/MMMail/UI` design image.
2. Map the same UI group to one or more current browser QA screenshot paths.
3. Record a status and concrete notes in the risk register.
4. Convert only `must-fix` findings into scoped Vue/CSS or selector work.

The register remains the source of truth for parity status. The generated browser QA report remains evidence that routes render and selectors are visible. Screenshots prove rendered state; they do not by themselves prove visual parity approval.

## 6. Status Definitions

`aligned` means the implementation matches the design direction for layout hierarchy, density, information grouping, and responsive intent.

`acceptable-delta` means a difference exists but is intentional for MMMail v2.1 productization, tenant branding, real route structure, or current data model boundaries.

`must-fix` means the current UI has a concrete visual or structural problem that should be fixed in this slice. Examples include broken responsive layout, missing stable selector evidence, text overlap, weak first-viewport product signal, incoherent hierarchy, or a route that clearly diverges from the approved UI direction without an explicit product reason.

## 7. Implementation Touchpoints

Expected files:

- `docs/superpowers/progress/v21-visual-parity-risk-register.md`
- `docs/superpowers/progress/v21-browser-visual-qa-report.md`
- `docs/superpowers/progress/v21-implementation-progress.md`
- A new focused contract test under `frontend-v2/tests/`
- Any small Vue/CSS files required by concrete `must-fix` findings.

The implementation plan must inspect the current visual QA matrix before editing UI. It should not preselect UI files for changes until the register identifies real `must-fix` rows.

## 8. Testing Design

Add a focused contract test that verifies:

- The parity risk register contains all visual QA UI groups.
- The register preserves the required columns: UI group, source design, QA evidence, status, notes, and owner slice.
- Status values are limited to `aligned`, `acceptable-delta`, and `must-fix`.
- The browser visual QA report still links to the parity register.
- Progress documentation references the completed audit slice after implementation.

Required verification commands for the implementation slice:

```bash
timeout 60s pnpm --dir frontend-v2 test
timeout 60s pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
pnpm --dir frontend-v2 visual:qa
```

## 9. Acceptance Criteria

This slice is complete when:

- Every UI group in the visual QA report has a row in `v21-visual-parity-risk-register.md`.
- Every row has a design source, QA evidence, status, concrete notes, and owner slice.
- Any `must-fix` row either has an implemented fix in the same slice or remains explicitly documented with a reason and owner.
- Browser visual QA still passes with fresh screenshot evidence.
- `docs/superpowers/progress/v21-implementation-progress.md` records the implementation commit, verification commands, screenshot count, and remaining parity risks.
- No touched implementation file violates the 500-line limit.

## 10. Self-Check

- No incomplete sections remain.
- The scope is a single implementation slice, not a full UI rewrite.
- The status vocabulary is fixed and testable.
- The design distinguishes screenshot existence from design parity approval.
- The plan avoids fake success paths, silent fallbacks, and untracked screenshot commits.
