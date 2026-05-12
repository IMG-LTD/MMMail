# Frontend v2.1 UI Coverage and Visual QA Design

## 1. Purpose

This specification defines the next v2.1 slice after the initial browser visual QA runner.

The v2.1 master specification requires every design image group under `docs/MMMail/UI` to be mapped to routes, states, panels, dialogs, or module capabilities. The current browser QA runner proves that Chrome screenshots can be captured, but it only covers the shell, Command Center, Notifications, Sheets, and three global overlays.

This slice turns the design images into an explicit coverage matrix and expands browser validation across the full v2.1 product surface.

## 2. Current State

Completed foundations:

- Route metadata exists in `frontend-v2/src/app/router/v21-route-meta.ts`.
- Shared components exist for tables, grids, drawers, modals, gates, charts, terminal logs, empty states, and error states.
- `frontend-v2/scripts/v21-browser-visual-qa.mjs` can launch local Chrome through CDP, capture screenshots, and write a Markdown report.
- `pnpm --dir frontend-v2 visual:qa` currently validates a small scenario set.

Current gaps:

- The QA runner does not cover all design image groups.
- The report does not prove route coverage for mail, calendar, drive, docs, pass, collaboration, admin, settings, public/auth/share, and system pages.
- There is no committed design-image coverage matrix.
- Existing screenshots do not cover major editors, module dashboards, or high-risk modal/drawer states.

## 3. Design Source Summary

Design source directory: `docs/MMMail/UI`.

The inspected overview images share these interaction patterns:

- Left product navigation with active module state.
- Top bar with global search, tenant selector, quick create, notifications, theme/help/profile controls.
- Main dashboard cards followed by dense work surfaces.
- Right context panels for details, comments, settings, health, or recommendations.
- Module-specific drawers or modals for compose, edit, share, invite, delete, policy, and protected-range operations.
- Visible status labels that should not rely on color alone.

The design images use historical placeholder branding such as Nexa and Acme. The implementation must validate MMMail product branding or clearly configurable tenant data instead of hardcoded design placeholders.

## 4. Coverage Matrix

| UI group | Primary route scenarios | Key states and surfaces | Overlay or panel evidence |
| --- | --- | --- | --- |
| `首页` | `/workspace`, `/workspace/today` | summary cards, agenda, activity, system status, quick actions | notification drawer, quick-create modal, command palette |
| `邮件` | `/mail`, `/mail/inbox`, `/mail/compose` | folder nav, message list, thread reader, composer | compose drawer, attachment area, security panel |
| `日历` | `/calendar`, `/calendar/week`, `/calendar/rooms` | calendar grid, agenda, room/resource state | event editor drawer, availability/status panel |
| `云盘` | `/drive`, `/drive/files/:fileId`, `/drive/uploads` | folder tree, file table, preview panel, upload queue | share settings modal, preview drawer |
| `文档` | `/docs`, `/docs/:documentId` | document list, editor, outline, comments, version summary | share permissions panel, comments panel |
| `Sheets和labs` | `/sheets`, `/sheets/:sheetId`, `/labs` | workbook list, spreadsheet grid, formula bar, Labs assistant | protected range modal, AI insight panel |
| `Pass` | `/pass`, `/pass/secure-links`, `/pass/monitor` | vault list, item detail, risk monitor, secure links | share settings modal, secret reveal/rotate controls |
| `Collaboration` | `/collaboration` | spaces list, project activity, tasks, comments, members | invite member panel |
| `CommandCenter` | `/command-center` | run summary, live tasks, health checks, terminal log | command palette, run cancel/retry controls |
| `Notifications` | `/notifications` | notification inbox, filters, statistics, settings | notification drawer, rule/settings panel |
| `Admin` | `/admin`, `/admin/users`, `/admin/system`, `/admin/risk` | governance summary, users, roles, health, alerts | create user, role permission, delete confirmation modal |
| `Setting` | `/settings` | profile, security, devices, sessions, storage, audit | delete account confirmation, security action panel |
| Public/auth/share/system | `/`, `/login`, `/register`, `/share/mail/:token`, `/share/drive/:token`, `/share/pass/:token`, `/offline`, `/maintenance`, `/404`, `/500` | blank-layout boundaries, public share content, error/offline states | product access gate and premium boundary state |

## 5. QA Runner Design

The existing CDP runner remains the execution mechanism.

The runner should evolve from hardcoded route checks into named scenario groups:

- `shellScenarios`: app shell, desktop side nav, tablet context panel, mobile tab bar.
- `moduleScenarios`: one dashboard or editor scenario per UI image group.
- `overlayScenarios`: command palette, quick create, theme drawer, compose drawer, share modal, delete confirmation, protected range modal.
- `publicBoundaryScenarios`: auth, share, error, offline, maintenance, and product access boundaries.

Each route scenario must define:

- `id`
- `uiGroup`
- `path`
- `viewports`
- `checks`
- optional `viewportChecks`
- optional `notes`

Each overlay scenario must define:

- `id`
- `uiGroup`
- `path`
- `action`
- `checks`
- `viewport`

## 6. Browser Assertions

Assertions must stay tied to real rendered UI.

Required assertion categories:

- Page is not blank and has no Vite error overlay.
- Required selectors are visible.
- App shell routes expose top bar and correct responsive navigation.
- Public blank-layout routes do not require the authenticated shell.
- Screenshots are non-empty and written under `.tmp/v21-browser-visual-qa/`.
- Report records route, viewport, UI group, selectors, and screenshot evidence.
- Placeholder product branding such as `Nexa Workspace` must not appear in app-rendered screenshots unless the route explicitly represents imported design reference content.

No fake backend success, mock screenshot, or swallowed error path is allowed.

## 7. Report Design

The Markdown report at `docs/superpowers/progress/v21-browser-visual-qa-report.md` should include:

- Generated timestamp.
- Total scenario count and screenshot count.
- Viewport table.
- Coverage table grouped by `uiGroup`.
- Per-scenario route, viewport, selector checks, and screenshot path.
- Explicit list of covered overlay states.
- Explicit list of public/auth/share/system boundary routes.

The report is a generated progress artifact, but the script and contract test are the source of truth for reproducible validation.

## 8. Contract Test Design

Update `frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs` so it verifies:

- `visual:qa` still points to `node scripts/v21-browser-visual-qa.mjs`.
- The script keeps Chrome DevTools Protocol screenshot capture.
- Required viewports exist: `1440x900`, `1024x768`, `390x844`.
- Every UI group from the coverage matrix appears in the scenario registry.
- Every primary route category appears in the script.
- Overlay scenarios include command palette, quick create, theme drawer, compose, share, protected range, and delete confirmation.
- The report path remains `docs/superpowers/progress/v21-browser-visual-qa-report.md`.

## 9. Non-goals

- Do not commit generated screenshots.
- Do not introduce Playwright, Puppeteer, or new browser dependencies in this slice.
- Do not rewrite oversized module files unless a selector or overlay hook is required for validation.
- Do not hide failing pages behind skip lists or soft warnings.
- Do not claim final visual parity for screenshots that are only captured, not reviewed.

## 10. Acceptance Criteria

This slice is complete when:

- This coverage design is committed.
- The implementation plan references this design and the v2.1 master spec.
- `frontend-v2/scripts/v21-browser-visual-qa.mjs` covers all UI groups in the matrix.
- `frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs` enforces the expanded coverage.
- `pnpm --dir frontend-v2 visual:qa` passes and produces an updated report.
- `pnpm --dir frontend-v2 test`, `pnpm --dir frontend-v2 typecheck`, and `pnpm --dir frontend-v2 build` pass after implementation.
- The final report identifies any remaining visual parity risks instead of silently treating captured screenshots as design approval.

## 11. Self-check

- Does this design preserve the v2.1 master acceptance criteria? Yes.
- Does it map every current UI image group? Yes.
- Does it avoid mock success paths and silent fallbacks? Yes.
- Does it keep generated screenshots out of git? Yes.
- Does it provide a clear handoff to `writing-plans`? Yes.
