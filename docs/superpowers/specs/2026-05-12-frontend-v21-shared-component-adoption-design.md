# Frontend v2.1 Shared Component Adoption Design

## 1. Purpose

This specification defines the next v2.1 implementation slice after `frontend-v21-responsive-accessibility-visual-qa`.

The previous slice created the required shared components from the v2.1 master spec section 16.2. This slice adopts those components in real product surfaces so the implementation starts satisfying section 17: shared components must be used for tables, drawers, modals, charts, empty/error/loading states, terminal logs, command palettes, and stateful operational UI.

This document is a design specification only. It does not implement code by itself.

## 2. Current State

Latest local commits:

- `27a25d3e feat(frontend-v2): add v2.1 responsive qa components`
- `2d0f08ff docs(frontend-v2): add v2.1 responsive accessibility qa design`

The shared component layer now exists:

- `DataTable`
- `DataGrid`
- `Drawer`
- `Modal`
- `UploadQueue`
- `CommandPalette`
- `ErrorState`
- `ChartCard`
- `TerminalLog`

Current gap: these components are mostly validated by contract tests but are not yet adopted by real module views. Several module pages still use local handcrafted tables, cards, overlays, and logs.

## 3. Design Source Observations

The referenced UI designs emphasize reusable operational patterns:

- Command Center shows KPI cards, a real-time task table, health panels, quick command cards, recent execution rows, and a dark terminal surface.
- Notifications shows a dense notification table with filters, status dots, category badges, pagination, a right settings panel, analytics chart, and important-notification list.
- Sheets/Labs shows a spreadsheet grid, formula bar, right Labs assistant, chart insight card, protected range popover, and collaboration comments.
- Admin shows shared dashboard cards, service state rows, security alerts, and multiple modal-style forms for user/domain/role/destructive flows.
- Settings and shell surfaces reuse drawer behavior and should not keep separate Naive UI overlay semantics when a shared wrapper exists.

The visual direction remains quiet and work-focused: dense information, thin borders, teal primary actions, semantic badges, compact cards, and stable table/grid dimensions.

## 4. Scope Decision

Three options were considered:

| Option | Summary | Tradeoff |
| --- | --- | --- |
| A. Shell and mid-size module adoption | Adopt shared components in Shell, ThemeDrawer, Command Center, Notifications, and Sheets workspace. | Best ratio of v2.1 acceptance coverage to implementation risk. |
| B. Deep Mail/Drive rewrite | Immediately split large Mail/Drive files and integrate every component there. | Important later, but high risk because those files already exceed 500 lines and need dedicated refactor plans. |
| C. Browser visual QA first | Start with viewport/browser screenshots and visual diff notes. | Useful later, but premature before shared primitives are visible in real pages. |

Approved direction: Option A.

The user has given ongoing authorization to follow the recommended engineering path, so this slice proceeds without additional product questions.

## 5. Target Files and Boundaries

Primary files:

- `frontend-v2/src/layouts/base-layout/BaseLayout.vue`
- `frontend-v2/src/layouts/modules/ThemeDrawer.vue`
- `frontend-v2/src/layouts/modules/ShellCommandPalette.vue`
- `frontend-v2/src/layouts/modules/ShellQuickCreateModal.vue`
- `frontend-v2/src/views/app/CommandCenterView.vue`
- `frontend-v2/src/views/app/NotificationsView.vue`
- `frontend-v2/src/views/app/SheetsWorkspaceView.vue`

Contract tests:

- `frontend-v2/tests/v21-responsive-accessibility-visual-qa-contract.test.mjs`
- `frontend-v2/tests/v21-shared-component-adoption-contract.test.mjs`

Avoid in this slice:

- Do not modify `MailSurfaceView.vue`, `DriveSectionView.vue`, `SheetsEditorView.vue`, or `DocsEditorView.vue`; those files exceed the 500-line baseline or are close enough to require separate split plans.
- Do not add fake success states for uploads, commands, or APIs.
- Do not change route information architecture.
- Do not implement backend endpoints.

## 6. Component Adoption Design

### 6.1 Shell Command Palette

Create `ShellCommandPalette.vue` and mount it in `BaseLayout.vue`.

Behavior:

- Uses `CommandPalette`.
- Reads `shellStore.commandPaletteOpen`.
- Builds command groups from `shellNavGroups`.
- Emits route navigation through `vue-router`.
- Closes through `shellStore.closeCommandPalette()`.
- Shows maturity/restriction badges for preview and beta surfaces.

This makes the existing top-bar command button functional instead of only toggling store state.

### 6.2 Shell Quick Create Modal

Create `ShellQuickCreateModal.vue` and mount it in `BaseLayout.vue`.

Behavior:

- Uses shared `Modal`.
- Reads `shellStore.quickCreateOpen`.
- Offers existing route-backed actions only: compose mail, create calendar event route, Drive route, Docs route, Sheets route, and Command Center route.
- Closes through `shellStore.closeQuickCreate()`.
- Does not claim object creation success; it only navigates to existing creation surfaces or module routes.

This uses the shared modal layer without inventing backend behavior.

### 6.3 Theme Drawer Wrapper

Update `ThemeDrawer.vue` to use shared `Drawer` instead of direct `NDrawer` and `NDrawerContent`.

Behavior:

- Keeps current theme store state.
- Preserves existing controls and preview content.
- Gains shared dialog stack, `aria-modal`, Escape, focus restoration, and consistent drawer chrome.

### 6.4 Command Center Adoption

Update `CommandCenterView.vue`:

- Use `ChartCard` for execution metric cards derived from command, workflow, audit, and active-run data.
- Use `DataTable` for recent audit/runs instead of handcrafted feed rows.
- Use `TerminalLog` for active run log tail and status output.
- Use `ErrorState` for load and run failures.

This directly follows the Command Center design image: metrics, real-time tasks, recent executions, and terminal log.

### 6.5 Notifications Adoption

Update `NotificationsView.vue`:

- Use `DataTable` for the main notification list.
- Use `ChartCard` for analytics statistics and delivery summary.
- Use `ErrorState` for runtime load failure.
- Keep mark-all-read behavior and existing API state.

This follows the notification design image: tabular notification center with analytics on the side.

### 6.6 Sheets Workspace Adoption

Update `SheetsWorkspaceView.vue`:

- Use `DataTable` for workbook rows.
- Add a compact `DataGrid` preview derived from the latest workbook summaries.
- Preserve routing to workbook detail.
- Use `ErrorState` for failed workbook-list loading.

This does not replace the full spreadsheet editor yet; `SheetsEditorView.vue` needs a later file-splitting plan because it already exceeds the file-size baseline.

## 7. Testing Design

Add a new contract test that asserts:

- `BaseLayout.vue` mounts `ShellCommandPalette` and `ShellQuickCreateModal`.
- `ThemeDrawer.vue` imports and uses shared `Drawer`.
- `ShellCommandPalette.vue` imports `CommandPalette`, `shellNavGroups`, `useShellStore`, and `useRouter`.
- `ShellQuickCreateModal.vue` imports `Modal`, `useShellStore`, and `useRouter`.
- `CommandCenterView.vue` imports `ChartCard`, `DataTable`, `TerminalLog`, and `ErrorState`.
- `NotificationsView.vue` imports `ChartCard`, `DataTable`, and `ErrorState`.
- `SheetsWorkspaceView.vue` imports `DataTable`, `DataGrid`, and `ErrorState`.

Verification commands:

- `pnpm --dir frontend-v2 test`
- `pnpm --dir frontend-v2 typecheck`
- `pnpm --dir frontend-v2 build`

## 8. Acceptance Criteria

This slice is complete when:

- Shell command palette is visible from the existing shell store state.
- Shell quick-create modal is visible from the existing shell store state and only navigates to real routes.
- Theme drawer uses the shared drawer wrapper.
- Command Center, Notifications, and Sheets workspace consume the shared component layer.
- No fake API success paths are added.
- No >500-line file is expanded as part of this slice.
- Contract tests, typecheck, and build pass.
