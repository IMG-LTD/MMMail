# Frontend v2.1 Final Visual Parity and Public/Auth Closure Design

## 1. Purpose

This specification defines the next v2.1 frontend slice after the core workbench closure.

The current v2.1 frontend has route metadata, design-system primitives, shared-component adoption, browser visual QA, share overlays, and Mail/Calendar/Pass core workbench interactions. The remaining frontend closure risk is no longer broad route coverage. It is final visual parity discipline and the public/auth/system first-impression surfaces.

This slice turns screenshot evidence into a design-image parity record, fixes the current `LoginView.vue` file-size violation, and strengthens public/auth/share/system browser evidence without adding fake backend success paths.

## 2. Current Progress Baseline

Latest implementation progress is recorded in:

- `docs/superpowers/progress/v21-implementation-progress.md`

Current baseline:

- Latest local progress commit: `a480199b docs(frontend-v2): record v2.1 implementation progress`
- Latest frontend implementation commit: `25cdda5a feat(frontend-v2): close v2.1 core workbench interactions`
- Browser visual QA report: `docs/superpowers/progress/v21-browser-visual-qa-report.md`
- Current visual QA screenshot count: `69`
- Current known file-size violation: `frontend-v2/src/views/public/LoginView.vue` at `532` lines.

Existing completed slices include v2.1 route metadata, gates, design tokens, shared components, shared component adoption, browser visual QA, public/auth/share/system contracts, Admin/runtime contracts, Docs/Sheets/Labs/Collaboration contracts, Pass/Notifications/Command Center contracts, share overlay interactions, and core Mail/Calendar/Pass workbench closure.

## 3. Design Sources

Primary sources:

- `docs/superpowers/specs/2026-04-28-frontend-v21-ui-upgrade-design.md`
- `docs/superpowers/progress/v21-browser-visual-qa-report.md`
- `docs/MMMail/UI/首页/工作台-设计概览.png`
- `docs/MMMail/UI/Setting/设置概览.png`
- `docs/MMMail/UI/Admin/管理后台.png`
- Public/auth/share/system routes already captured by the visual QA report.

The design image set is still the visual source of truth. The implementation must keep MMMail branding and configurable tenant data, not historical sample brands such as Nexa or Nova.

## 4. Scope

In scope:

- Split `LoginView.vue` into focused local public/auth components and CSS so every touched file remains below 500 lines.
- Preserve the current login route behavior and public blank-layout boundary.
- Improve Login/Register/Boundary/Product Access/Public Share/System selectors where needed for stable visual QA evidence.
- Add a design parity risk register that maps key UI groups and public/auth/system routes to screenshot evidence and design-source observations.
- Extend the visual QA Markdown report generation so it can include a structured parity-risk section.
- Refresh visual QA and progress records after implementation.

Out of scope:

- Backend auth implementation or persistence changes.
- Real SSO/MFA execution.
- New pricing, licensing, or legal copy.
- Rewriting large product editors such as `SheetsEditorView.vue` or `DocsEditorView.vue`.
- Committing screenshots under `.tmp/`.
- Hiding visual QA failures behind skips, soft warnings, or fake selectors.

## 5. Public/Auth Component Design

`LoginView.vue` should become a route-level composition file under the 500-line limit.

Recommended local component boundary:

- `public/auth/LoginBrandPanel.vue`: MMMail story, value points, and visual illustration surface.
- `public/auth/LoginFormPanel.vue`: login form, SSO entry, MFA prompt, recovery/help links.
- `public/auth/LoginLegalBar.vue`: privacy, boundary, status, locale support links.
- `public/auth/login-view.css`: extracted login-specific styling.
- `public/auth/login-view-helpers.ts`: small immutable arrays or text models when useful.

The split must preserve:

- `.login-screen`
- `.signin-block`
- existing localized text behavior through `lt()` and `useLocaleText()`
- public links to `/boundary` and `/register`
- no real-submit success claim.

The visual language should remain dense, calm, and public-product focused. It should avoid decorative gradients or stock-like hero patterns that do not communicate the product. Existing code-native illustration can stay if it remains compact and does not push the route file over the limit.

## 6. Public/Auth/System QA Design

The browser visual QA runner already captures these public boundary routes:

- `/login`
- `/register`
- `/boundary`
- `/product-access-blocked`
- `/share/mail/demo-token`
- `/share/drive/demo-token`
- `/share/pass/demo-token`
- `/offline`
- `/maintenance`
- `/404`
- `/500`

This slice should strengthen the evidence around public/auth/system first-impression surfaces:

- Login: verify split components still expose `.login-screen` and `.signin-block`.
- Register: keep `.public-shell` and `.register-card`.
- Boundary: keep `.public-surface-frame`, `.boundary-page`, and `.boundary-matrix`.
- Product access blocked: keep `.blocked-page` and `.premium-gate`.
- Public shares: keep route-specific share selectors.
- System states: keep `.system-state`.

If a selector is missing, the implementation must fix the rendered UI or update the scenario to a real stable selector. It must not introduce fake DOM purely to make QA pass.

## 7. Visual Parity Risk Register

Add a structured progress artifact or report section that distinguishes screenshot existence from design approval.

Recommended path:

- `docs/superpowers/progress/v21-visual-parity-risk-register.md`

Minimum fields:

| Field | Meaning |
| --- | --- |
| UI group | Design image group or route family. |
| Source design | Relevant `docs/MMMail/UI` image or design group. |
| QA evidence | Screenshot path or scenario id from the visual QA report. |
| Status | `aligned`, `acceptable-delta`, or `must-fix`. |
| Notes | Concrete visual or interaction delta. |
| Owner slice | Current slice or future slice. |

The risk register should initially focus on public/auth/share/system and any cross-shell deltas discovered while running visual QA. It can reference existing screenshot paths under `.tmp/`, but screenshots remain uncommitted.

## 8. Report Generation Design

Update `frontend-v2/scripts/v21-visual-qa/report.mjs` to include a stable section for parity risks or a link to the risk register.

Required report behavior:

- Keep existing route, viewport, selector, screenshot evidence rows.
- Preserve `docs/superpowers/progress/v21-browser-visual-qa-report.md` as the generated report path.
- Add a clear statement that screenshot capture is evidence, not final manual design approval.
- Include or link the public/auth parity risk register.

The report generator must not hide failed scenarios. It should only report results after the runner has successfully validated the declared selectors.

## 9. Testing Design

Add a focused contract test for this slice.

Recommended file:

- `frontend-v2/tests/v21-final-visual-parity-public-auth-closure-contract.test.mjs`

The test should assert:

- `LoginView.vue` imports extracted auth components and CSS.
- `LoginView.vue` remains at or below 500 lines.
- New auth component files expose `.login-screen`, `.signin-block`, public auth links, SSO/MFA copy, and locale helpers.
- `report.mjs` includes the visual parity risk section or link.
- `docs/superpowers/progress/v21-visual-parity-risk-register.md` contains the required columns and at least public/auth/share/system rows.
- `docs/superpowers/progress/v21-implementation-progress.md` is updated after the implementation slice.

Verification commands for the implementation plan:

```bash
timeout 60s pnpm --dir frontend-v2 test
timeout 60s pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
pnpm --dir frontend-v2 visual:qa
```

## 10. Acceptance Criteria

This slice is complete when:

- `LoginView.vue` and all new public/auth files are below 500 lines.
- Public/auth/system routes continue passing browser visual QA.
- The visual QA report includes route evidence and an explicit parity-risk section or link.
- A committed visual parity risk register exists for public/auth/share/system closure.
- `docs/superpowers/progress/v21-implementation-progress.md` is updated with the new commit hash, verification outcomes, screenshot count, and remaining risks.
- `test`, `typecheck`, `build`, and `visual:qa` pass with fresh outputs.

## 11. Self-Check

- No unfinished markers remain in this design.
- The scope is narrow enough for one implementation plan.
- The design follows current v2.1 route, QA, and shared-component patterns.
- The design does not add silent fallbacks, fake successes, or mock persistence.
- The design leaves backend persistence and large editor rewrites for separate future slices.
