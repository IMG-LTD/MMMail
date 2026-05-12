# Frontend v2.1 Responsive Accessibility Visual QA 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 补齐 v2.1 section 16.2 要求的共享组件基础，为后续响应式、无障碍、视觉 QA 提供统一组件层。

**架构：** 在 `frontend-v2/src/design-system/components/` 下新增缺失横向组件，保持现有 Vue 3 `<script setup>`、CSS token 和 Naive UI 包装模式。先用 Node contract tests 固定组件 API、状态、无障碍语义和响应式 hook，再实现最小可复用组件。

**技术栈：** Vue 3, TypeScript, Vite, Pinia shell state, Naive UI wrapper pattern, Node test runner, MMMail design tokens.

---

## Source specification

Approved design: `docs/superpowers/specs/2026-05-12-frontend-v21-responsive-accessibility-visual-qa-design.md`.

Master source: `docs/superpowers/specs/2026-04-28-frontend-v21-ui-upgrade-design.md` sections 16.2, 16.3, 16.4, and 17.

## File structure

- Modify: `frontend-v2/tests/v21-design-system-components.test.mjs`
  - Adds contract coverage for all required section 16.2 components.
- Create: `frontend-v2/tests/v21-responsive-accessibility-visual-qa-contract.test.mjs`
  - Covers overlay semantics, live-region integration, responsive classes, and no-silent-success expectations.
- Create: `frontend-v2/src/design-system/components/DataTable.vue`
  - Record-table primitive for Mail, Drive, Notifications, Admin, Pass, Collaboration, and Command Center lists.
- Create: `frontend-v2/src/design-system/components/DataGrid.vue`
  - Spreadsheet-style primitive for Sheets, protected ranges, and permission matrices.
- Create: `frontend-v2/src/design-system/components/Drawer.vue`
  - Naive UI drawer wrapper with stack, Escape, focus, and labelled-region semantics.
- Create: `frontend-v2/src/design-system/components/Modal.vue`
  - Naive UI modal wrapper with consistent title/body/footer/action slots.
- Create: `frontend-v2/src/design-system/components/UploadQueue.vue`
  - Async upload queue with visible progress, failure, retry, cancel, and screen-reader announcements.
- Create: `frontend-v2/src/design-system/components/CommandPalette.vue`
  - Shell command/search palette with grouped commands and entitlement/permission badges.
- Create: `frontend-v2/src/design-system/components/ErrorState.vue`
  - Explicit failure surface for API, permission, upload, command, and module-load failures.
- Create: `frontend-v2/src/design-system/components/ChartCard.vue`
  - Stable chart card wrapper with screen-reader text summary and non-color-only status cues.
- Create: `frontend-v2/src/design-system/components/TerminalLog.vue`
  - Command Center log viewer with running/failure/completion announcements and copy/filter actions.

Do not modify app routes or backend files in this slice.

---

### Task 1: Add failing component and QA contracts

**Files:**
- Modify: `frontend-v2/tests/v21-design-system-components.test.mjs`
- Create: `frontend-v2/tests/v21-responsive-accessibility-visual-qa-contract.test.mjs`

- [ ] **Step 1: Extend design-system component file registry**

Add these entries to the existing `files` object in `frontend-v2/tests/v21-design-system-components.test.mjs`:

```js
  chartCard: new URL('../src/design-system/components/ChartCard.vue', import.meta.url),
  commandPalette: new URL('../src/design-system/components/CommandPalette.vue', import.meta.url),
  dataGrid: new URL('../src/design-system/components/DataGrid.vue', import.meta.url),
  dataTable: new URL('../src/design-system/components/DataTable.vue', import.meta.url),
  drawer: new URL('../src/design-system/components/Drawer.vue', import.meta.url),
  errorState: new URL('../src/design-system/components/ErrorState.vue', import.meta.url),
  modal: new URL('../src/design-system/components/Modal.vue', import.meta.url),
  terminalLog: new URL('../src/design-system/components/TerminalLog.vue', import.meta.url),
  uploadQueue: new URL('../src/design-system/components/UploadQueue.vue', import.meta.url),
```

- [ ] **Step 2: Add required component existence/API assertions**

Append a test that reads the nine new files and asserts these exact names exist in source:

```js
const requiredComponentFiles = [
  ['dataTable', /interface DataTableColumn/, /aria-sort/, /permissionDenied/, /premiumLocked/],
  ['dataGrid', /interface DataGridColumn/, /activeCell/, /selectedRange/, /stickyFirstColumn/],
  ['drawer', /useDialogStack/, /aria-modal/, /closeLabel/, /@keydown\\.esc/],
  ['modal', /useDialogStack/, /aria-modal/, /after-leave/, /footer/],
  ['uploadQueue', /useSrLive/, /retry/, /cancel/, /progress/],
  ['commandPalette', /useShellStore/, /role="combobox"/, /PremiumBadge/, /HostedBadge/],
  ['errorState', /errorCode/, /retryLabel/, /emit\\('retry'\\)/, /error-state--overlay/],
  ['chartCard', /vue-data-ui/, /aria-label/, /summary/, /chart-card__slot/],
  ['terminalLog', /useSrLive/, /autoFollow/, /copy/, /terminal-log__line/]
]
```

- [ ] **Step 3: Create responsive/accessibility QA contract test**

Create `frontend-v2/tests/v21-responsive-accessibility-visual-qa-contract.test.mjs` with one test for overlay/live-region contracts and one test for responsive hooks:

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  drawer: new URL('../src/design-system/components/Drawer.vue', import.meta.url),
  modal: new URL('../src/design-system/components/Modal.vue', import.meta.url),
  uploadQueue: new URL('../src/design-system/components/UploadQueue.vue', import.meta.url),
  commandPalette: new URL('../src/design-system/components/CommandPalette.vue', import.meta.url),
  terminalLog: new URL('../src/design-system/components/TerminalLog.vue', import.meta.url),
  dataTable: new URL('../src/design-system/components/DataTable.vue', import.meta.url),
  dataGrid: new URL('../src/design-system/components/DataGrid.vue', import.meta.url),
  chartCard: new URL('../src/design-system/components/ChartCard.vue', import.meta.url),
  errorState: new URL('../src/design-system/components/ErrorState.vue', import.meta.url)
}

test('v2.1 QA components expose accessibility primitives', async () => {
  const [drawer, modal, uploadQueue, commandPalette, terminalLog] = await Promise.all(
    [files.drawer, files.modal, files.uploadQueue, files.commandPalette, files.terminalLog].map(file => readFile(file, 'utf8'))
  )

  assert.match(drawer, /aria-modal/)
  assert.match(modal, /aria-modal/)
  assert.match(commandPalette, /role="listbox"/)
  assert.match(uploadQueue, /announce/)
  assert.match(terminalLog, /announce/)
})

test('v2.1 QA components expose responsive and failure-state hooks', async () => {
  const [dataTable, dataGrid, chartCard, errorState] = await Promise.all(
    [files.dataTable, files.dataGrid, files.chartCard, files.errorState].map(file => readFile(file, 'utf8'))
  )

  assert.match(dataTable, /data-table--compact/)
  assert.match(dataTable, /data-table--stacked/)
  assert.match(dataGrid, /data-grid--mobile/)
  assert.match(chartCard, /chart-card--loading/)
  assert.match(errorState, /error-state--inline/)
})
```

- [ ] **Step 4: Run tests and verify failure**

Run:

```bash
pnpm --dir frontend-v2 test
```

Expected: FAIL because the nine component files do not yet exist.

---

### Task 2: Implement table, grid, chart, and error surfaces

**Files:**
- Create: `frontend-v2/src/design-system/components/DataTable.vue`
- Create: `frontend-v2/src/design-system/components/DataGrid.vue`
- Create: `frontend-v2/src/design-system/components/ChartCard.vue`
- Create: `frontend-v2/src/design-system/components/ErrorState.vue`
- Tests: `frontend-v2/tests/v21-design-system-components.test.mjs`
- Tests: `frontend-v2/tests/v21-responsive-accessibility-visual-qa-contract.test.mjs`

- [ ] **Step 1: Implement `ErrorState.vue`**

Use a typed `variant` union of `inline | card | full | overlay`, expose `errorCode`, `retryLabel`, and emit `retry`, `support`, and `secondary`. Include classes `error-state--inline`, `error-state--card`, `error-state--full`, and `error-state--overlay`.

- [ ] **Step 2: Implement `ChartCard.vue`**

Expose `title`, `description`, `value`, `trend`, `status`, `loading`, `error`, and `summary`. Include the literal `vue-data-ui` in a code comment to preserve the preferred charting source, a `.chart-card__slot` chart area, `.chart-card--loading`, and a screen-reader summary.

- [ ] **Step 3: Implement `DataTable.vue`**

Define `interface DataTableColumn` with `key`, `label`, `width`, `align`, `sortable`, and `cellSlot`. Support `loading`, `empty`, `error`, `permissionDenied`, `premiumLocked`, `density`, `selectedKeys`, and `stacked`. Include `aria-sort`, `data-table--compact`, and `data-table--stacked`.

- [ ] **Step 4: Implement `DataGrid.vue`**

Define `interface DataGridColumn`, `interface DataGridCell`, `activeCell`, `selectedRange`, `protectedRanges`, `stickyHeader`, and `stickyFirstColumn`. Include keyboard handling for arrow navigation, `data-grid--mobile`, and non-color-only locked-cell text.

- [ ] **Step 5: Run component contracts**

Run:

```bash
pnpm --dir frontend-v2 test
```

Expected: tests still FAIL because overlay, upload, command, and terminal components are not implemented yet.

---

### Task 3: Implement overlay, upload, command, and terminal components

**Files:**
- Create: `frontend-v2/src/design-system/components/Drawer.vue`
- Create: `frontend-v2/src/design-system/components/Modal.vue`
- Create: `frontend-v2/src/design-system/components/UploadQueue.vue`
- Create: `frontend-v2/src/design-system/components/CommandPalette.vue`
- Create: `frontend-v2/src/design-system/components/TerminalLog.vue`
- Tests: `frontend-v2/tests/v21-design-system-components.test.mjs`
- Tests: `frontend-v2/tests/v21-responsive-accessibility-visual-qa-contract.test.mjs`

- [ ] **Step 1: Implement `Drawer.vue`**

Wrap `NDrawer` and `NDrawerContent`, accept `show`, `title`, `description`, `size`, `tone`, and `closeLabel`, emit `update:show`, `close`, `escape`, and `after-leave`, call `useDialogStack()`, and include `aria-modal` plus `@keydown.esc`.

- [ ] **Step 2: Implement `Modal.vue`**

Wrap `NModal`, expose the same labelled overlay contract, push/pop `useDialogStack()`, include `aria-modal`, header/body/footer slots, and emit `after-leave`.

- [ ] **Step 3: Implement `UploadQueue.vue`**

Define upload item status union `queued | uploading | paused | completed | failed | canceled`. Use `useSrLive()` and `announce()` on state changes. Emit `retry`, `cancel`, `pause`, `resume`, `remove`, and `openDestination`.

- [ ] **Step 4: Implement `CommandPalette.vue`**

Use `useShellStore()`, `PremiumBadge`, and `HostedBadge`. Render combobox/listbox roles, command groups, restricted badges, and emit `update:show`, `update:query`, `close`, `select`, `search`, and `execute`.

- [ ] **Step 5: Implement `TerminalLog.vue`**

Define log levels and streams, render `.terminal-log__line`, support `autoFollow`, `copyable`, filter text, copy/clear/pause/resume emits, and call `announce()` for running, failed, retry, and completed log lines.

- [ ] **Step 6: Run component contracts**

Run:

```bash
pnpm --dir frontend-v2 test
```

Expected: PASS.

---

### Task 4: Typecheck, build, and commit implementation

**Files:**
- All files from Tasks 1-3.

- [ ] **Step 1: Run typecheck**

Run:

```bash
pnpm --dir frontend-v2 typecheck
```

Expected: PASS.

- [ ] **Step 2: Run production build**

Run:

```bash
pnpm --dir frontend-v2 build
```

Expected: PASS.

- [ ] **Step 3: Inspect git state**

Run:

```bash
git status --short --branch
```

Expected: only this plan, the new tests, and the new shared component files are staged or unstaged for this task. Existing unrelated untracked paths remain ignored for the commit.

- [ ] **Step 4: Commit**

Stage only related files:

```bash
git add -f docs/superpowers/plans/2026-05-12-frontend-v21-responsive-accessibility-visual-qa.md
git add frontend-v2/tests/v21-design-system-components.test.mjs
git add frontend-v2/tests/v21-responsive-accessibility-visual-qa-contract.test.mjs
git add frontend-v2/src/design-system/components/DataTable.vue
git add frontend-v2/src/design-system/components/DataGrid.vue
git add frontend-v2/src/design-system/components/Drawer.vue
git add frontend-v2/src/design-system/components/Modal.vue
git add frontend-v2/src/design-system/components/UploadQueue.vue
git add frontend-v2/src/design-system/components/CommandPalette.vue
git add frontend-v2/src/design-system/components/ErrorState.vue
git add frontend-v2/src/design-system/components/ChartCard.vue
git add frontend-v2/src/design-system/components/TerminalLog.vue
git diff --cached --check
git commit -m "feat(frontend-v2): add v2.1 responsive qa components"
```
