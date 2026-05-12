# Frontend v2.1 Shared Component Adoption 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 v2.1 共享组件接入 Shell、Theme Drawer、Command Center、Notifications 和 Sheets workspace，让组件从契约存在进入真实页面使用。

**架构：** 保持 `frontend-v2/src/design-system/components/` 作为共享组件层。Shell 新增小型模块组件承载 `CommandPalette` 与 `Modal`，中型模块视图直接采用 `DataTable`、`DataGrid`、`ChartCard`、`ErrorState` 和 `TerminalLog`。避免触碰已超 500 行的大型 Mail/Drive/Sheets editor 文件。

**技术栈：** Vue 3, TypeScript, Pinia, Vue Router, Naive UI wrappers, Node test runner, Vite.

---

## Source specification

Approved design: `docs/superpowers/specs/2026-05-12-frontend-v21-shared-component-adoption-design.md`.

## File structure

- Create: `frontend-v2/tests/v21-shared-component-adoption-contract.test.mjs`
  - Locks the expected shared-component adoption points.
- Create: `frontend-v2/src/layouts/modules/ShellCommandPalette.vue`
  - Converts `shellNavGroups` into command palette groups and navigates selected commands.
- Create: `frontend-v2/src/layouts/modules/ShellQuickCreateModal.vue`
  - Uses shared `Modal` for route-backed quick create actions.
- Modify: `frontend-v2/src/layouts/base-layout/BaseLayout.vue`
  - Mounts shell command palette and quick-create modal.
- Modify: `frontend-v2/src/layouts/modules/ThemeDrawer.vue`
  - Replaces direct Naive UI drawer usage with shared `Drawer`.
- Modify: `frontend-v2/src/views/app/CommandCenterView.vue`
  - Uses `ChartCard`, `DataTable`, `TerminalLog`, and `ErrorState`.
- Modify: `frontend-v2/src/views/app/NotificationsView.vue`
  - Uses `ChartCard`, `DataTable`, and `ErrorState`.
- Modify: `frontend-v2/src/views/app/SheetsWorkspaceView.vue`
  - Uses `DataTable`, `DataGrid`, and `ErrorState`.

Do not modify `MailSurfaceView.vue`, `DriveSectionView.vue`, `SheetsEditorView.vue`, or `DocsEditorView.vue` in this plan.

---

### Task 1: Add failing adoption contract

**Files:**
- Create: `frontend-v2/tests/v21-shared-component-adoption-contract.test.mjs`

- [ ] **Step 1: Create the contract test**

Create this file:

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  baseLayout: new URL('../src/layouts/base-layout/BaseLayout.vue', import.meta.url),
  themeDrawer: new URL('../src/layouts/modules/ThemeDrawer.vue', import.meta.url),
  shellCommandPalette: new URL('../src/layouts/modules/ShellCommandPalette.vue', import.meta.url),
  shellQuickCreateModal: new URL('../src/layouts/modules/ShellQuickCreateModal.vue', import.meta.url),
  commandCenter: new URL('../src/views/app/CommandCenterView.vue', import.meta.url),
  notifications: new URL('../src/views/app/NotificationsView.vue', import.meta.url),
  sheetsWorkspace: new URL('../src/views/app/SheetsWorkspaceView.vue', import.meta.url)
}

test('v2.1 shell mounts shared command palette and quick-create modal', async () => {
  const [baseLayout, commandPalette, quickCreate, themeDrawer] = await Promise.all([
    readFile(files.baseLayout, 'utf8'),
    readFile(files.shellCommandPalette, 'utf8'),
    readFile(files.shellQuickCreateModal, 'utf8'),
    readFile(files.themeDrawer, 'utf8')
  ])

  assert.match(baseLayout, /ShellCommandPalette/)
  assert.match(baseLayout, /ShellQuickCreateModal/)
  assert.match(commandPalette, /CommandPalette/)
  assert.match(commandPalette, /shellNavGroups/)
  assert.match(commandPalette, /useShellStore/)
  assert.match(commandPalette, /useRouter/)
  assert.match(quickCreate, /Modal/)
  assert.match(quickCreate, /quickCreateOpen/)
  assert.match(quickCreate, /closeQuickCreate/)
  assert.match(themeDrawer, /Drawer/)
  assert.doesNotMatch(themeDrawer, /NDrawerContent/)
})

test('v2.1 operational modules consume shared QA components', async () => {
  const [commandCenter, notifications, sheetsWorkspace] = await Promise.all([
    readFile(files.commandCenter, 'utf8'),
    readFile(files.notifications, 'utf8'),
    readFile(files.sheetsWorkspace, 'utf8')
  ])

  assert.match(commandCenter, /ChartCard/)
  assert.match(commandCenter, /DataTable/)
  assert.match(commandCenter, /TerminalLog/)
  assert.match(commandCenter, /ErrorState/)
  assert.match(notifications, /ChartCard/)
  assert.match(notifications, /DataTable/)
  assert.match(notifications, /ErrorState/)
  assert.match(sheetsWorkspace, /DataTable/)
  assert.match(sheetsWorkspace, /DataGrid/)
  assert.match(sheetsWorkspace, /ErrorState/)
})
```

- [ ] **Step 2: Run the failing test**

Run:

```bash
pnpm --dir frontend-v2 test
```

Expected: FAIL because `ShellCommandPalette.vue` and `ShellQuickCreateModal.vue` do not yet exist, and views do not yet import the shared components.

---

### Task 2: Implement Shell shared overlays

**Files:**
- Create: `frontend-v2/src/layouts/modules/ShellCommandPalette.vue`
- Create: `frontend-v2/src/layouts/modules/ShellQuickCreateModal.vue`
- Modify: `frontend-v2/src/layouts/base-layout/BaseLayout.vue`
- Modify: `frontend-v2/src/layouts/modules/ThemeDrawer.vue`

- [ ] **Step 1: Create `ShellCommandPalette.vue`**

Implement a small component that imports `CommandPalette`, `shellNavGroups`, `useShellStore`, `useRouter`, and locale helpers. It should keep a local `query`, build groups from nav entries, navigate with `router.push(item.path || '/')`, then call `shellStore.closeCommandPalette()`.

- [ ] **Step 2: Create `ShellQuickCreateModal.vue`**

Implement a small component that imports `Modal`, `useShellStore`, `useRouter`, and locale helpers. It should render route-backed buttons for `/mail/compose`, `/calendar`, `/drive`, `/docs`, `/sheets`, and `/command-center`, then close the modal after navigation.

- [ ] **Step 3: Mount both shell modules**

In `BaseLayout.vue`, import `ShellCommandPalette` and `ShellQuickCreateModal`, then render them next to the existing `ThemeDrawer`.

- [ ] **Step 4: Replace ThemeDrawer overlay wrapper**

In `ThemeDrawer.vue`, remove direct `NDrawer` / `NDrawerContent` usage and wrap existing body content with shared `Drawer`. Preserve `handleDrawerVisibility(value)`.

- [ ] **Step 5: Run the contract**

Run:

```bash
pnpm --dir frontend-v2 test
```

Expected: Still FAIL because module views are not yet adopted.

---

### Task 3: Adopt shared components in Command Center

**Files:**
- Modify: `frontend-v2/src/views/app/CommandCenterView.vue`

- [ ] **Step 1: Import shared components**

Add imports for:

```ts
import ChartCard from '@/design-system/components/ChartCard.vue'
import DataTable, { type DataTableColumn } from '@/design-system/components/DataTable.vue'
import ErrorState from '@/design-system/components/ErrorState.vue'
import TerminalLog, { type TerminalLogLine } from '@/design-system/components/TerminalLog.vue'
```

- [ ] **Step 2: Add table and terminal computed models**

Add `auditColumns`, `auditRows`, and `terminalLogLines` computed values using current API data. Use `activeRun.startedAt || item.createdAt || '-'` for timestamps and never invent a successful run.

- [ ] **Step 3: Replace local feed/log rendering**

Use `ErrorState` for `loadError` and `runError`, `DataTable` for audit rows, `TerminalLog` for log output, and `ChartCard` for the three existing command summary cards.

- [ ] **Step 4: Run tests**

Run:

```bash
pnpm --dir frontend-v2 test
```

Expected: Still FAIL until Notifications and Sheets are adopted.

---

### Task 4: Adopt shared components in Notifications and Sheets

**Files:**
- Modify: `frontend-v2/src/views/app/NotificationsView.vue`
- Modify: `frontend-v2/src/views/app/SheetsWorkspaceView.vue`

- [ ] **Step 1: Update Notifications imports and computed table rows**

Import `ChartCard`, `DataTable`, `DataTableColumn`, and `ErrorState`. Add `notificationColumns` and `notificationRows` from existing notification data.

- [ ] **Step 2: Replace Notifications list and analytics cards**

Use `DataTable` for notifications, `ErrorState` for `loadError`, and `ChartCard` for analytics/rules/template panels. Preserve `markAllRead()`.

- [ ] **Step 3: Update Sheets workspace imports and computed table/grid data**

Import `DataTable`, `DataTableColumn`, `DataGrid`, `DataGridCell`, and `ErrorState`. Add workbook table columns and a compact grid preview from the first six visible workbook summaries.

- [ ] **Step 4: Replace Sheets local table**

Use `DataTable` for workbook summaries and `DataGrid` for the preview. Preserve `openWorkbook(sheet.id)`.

- [ ] **Step 5: Run tests**

Run:

```bash
pnpm --dir frontend-v2 test
```

Expected: PASS.

---

### Task 5: Typecheck, build, line-count check, and commit

**Files:**
- All files from Tasks 1-4.

- [ ] **Step 1: Run typecheck**

Run:

```bash
pnpm --dir frontend-v2 typecheck
```

Expected: PASS.

- [ ] **Step 2: Run build**

Run:

```bash
pnpm --dir frontend-v2 build
```

Expected: PASS.

- [ ] **Step 3: Check edited file sizes**

Run:

```bash
wc -l frontend-v2/src/layouts/base-layout/BaseLayout.vue frontend-v2/src/layouts/modules/ThemeDrawer.vue frontend-v2/src/layouts/modules/ShellCommandPalette.vue frontend-v2/src/layouts/modules/ShellQuickCreateModal.vue frontend-v2/src/views/app/CommandCenterView.vue frontend-v2/src/views/app/NotificationsView.vue frontend-v2/src/views/app/SheetsWorkspaceView.vue
```

Expected: every edited/created file remains under 500 lines.

- [ ] **Step 4: Commit related files only**

Run:

```bash
git add -f docs/superpowers/plans/2026-05-12-frontend-v21-shared-component-adoption.md
git add frontend-v2/tests/v21-shared-component-adoption-contract.test.mjs
git add frontend-v2/src/layouts/base-layout/BaseLayout.vue
git add frontend-v2/src/layouts/modules/ThemeDrawer.vue
git add frontend-v2/src/layouts/modules/ShellCommandPalette.vue
git add frontend-v2/src/layouts/modules/ShellQuickCreateModal.vue
git add frontend-v2/src/views/app/CommandCenterView.vue
git add frontend-v2/src/views/app/NotificationsView.vue
git add frontend-v2/src/views/app/SheetsWorkspaceView.vue
git diff --cached --check
git commit -m "feat(frontend-v2): adopt v2.1 shared components"
```
