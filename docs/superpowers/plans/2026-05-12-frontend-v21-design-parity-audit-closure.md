# Frontend v2.1 Design Parity Audit Closure 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 v2.1 前端视觉一致性风险登记表扩展到所有 visual QA UI 组，并用测试、报告和进度记录形成可提交闭环。

**架构：** 保留现有 Vue 视图、visual QA runner 和 Markdown progress artifacts。新增一个合同测试锁定全 UI 组风险登记覆盖；扩展 `v21-visual-parity-risk-register.md`，重新生成浏览器 visual QA 报告，并在实现提交后更新总进度文件。只有风险登记表明确标为 `must-fix` 的具体问题才允许进入 Vue/CSS 小修。

**技术栈：** Vue 3、Vite、Node test runner、Markdown progress artifacts、Chrome DevTools Protocol visual QA runner。

---

## 文件结构

创建：

- `frontend-v2/tests/v21-design-parity-audit-closure-contract.test.mjs`：合同测试，解析 visual QA 报告与风险登记表，锁定 UI 组覆盖、列结构、状态枚举和进度记录。

修改：

- `docs/superpowers/progress/v21-visual-parity-risk-register.md`：扩展到 visual QA 报告中的全部 UI 组。
- `docs/superpowers/progress/v21-browser-visual-qa-report.md`：由 `pnpm --dir frontend-v2 visual:qa` 刷新。
- `docs/superpowers/progress/v21-implementation-progress.md`：记录 active slice、实现提交、验证结果、截图数和剩余风险。

可能修改：

- 仅当风险登记表出现 `must-fix` 行时，修改对应 Vue/CSS 文件；否则不做 UI 源码改动。

---

### 任务 1：写入红灯合同测试

**文件：**

- 创建：`frontend-v2/tests/v21-design-parity-audit-closure-contract.test.mjs`
- 测试：`frontend-v2/tests/v21-design-parity-audit-closure-contract.test.mjs`

- [ ] **步骤 1：创建合同测试文件**

写入以下测试代码：

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  report: new URL('../../docs/superpowers/progress/v21-browser-visual-qa-report.md', import.meta.url),
  riskRegister: new URL('../../docs/superpowers/progress/v21-visual-parity-risk-register.md', import.meta.url),
  progress: new URL('../../docs/superpowers/progress/v21-implementation-progress.md', import.meta.url)
}

const REQUIRED_COLUMNS = ['UI group', 'Source design', 'QA evidence', 'Status', 'Notes', 'Owner slice']
const ALLOWED_STATUSES = new Set(['aligned', 'acceptable-delta', 'must-fix'])
const OWNER_SLICE = 'frontend-v21-design-parity-audit-closure'

function parseMarkdownRows(source) {
  return source
    .split('\n')
    .filter(line => line.startsWith('|') && !line.includes('---'))
    .map(line => line.split('|').slice(1, -1).map(cell => cell.trim()))
}

function parseRiskRows(source) {
  const rows = parseMarkdownRows(source)
  const header = rows.at(0) ?? []
  assert.deepEqual(header, REQUIRED_COLUMNS)
  return rows.slice(1).map(row => Object.fromEntries(header.map((column, index) => [column, row[index] ?? ''])))
}

function extractReportGroups(report) {
  const rows = parseMarkdownRows(report)
  const coverageStart = rows.findIndex(row => row[0] === 'UI group' && row[1] === 'Scenario count')
  assert.notEqual(coverageStart, -1, 'visual QA report must include UI group coverage')
  const groups = []

  for (const row of rows.slice(coverageStart + 1)) {
    if (row[0] === 'UI group' && row[1] === 'Scenario') break
    if (row.length >= 3 && row[0]) groups.push(row[0])
  }

  return [...new Set(groups)]
}

test('visual parity register covers every browser visual QA UI group', async () => {
  const [report, riskRegister] = await Promise.all([
    readFile(files.report, 'utf8'),
    readFile(files.riskRegister, 'utf8')
  ])

  const reportGroups = extractReportGroups(report)
  const riskRows = parseRiskRows(riskRegister)
  const riskGroups = new Set(riskRows.map(row => row['UI group']))
  const missingGroups = reportGroups.filter(group => !riskGroups.has(group))

  assert.deepEqual(missingGroups, [])
  assert.ok(reportGroups.length >= 13, 'visual QA report should keep broad v2.1 UI coverage')
})

test('visual parity register rows are concrete and status values are bounded', async () => {
  const riskRegister = await readFile(files.riskRegister, 'utf8')
  const riskRows = parseRiskRows(riskRegister)

  for (const row of riskRows) {
    for (const column of REQUIRED_COLUMNS) {
      assert.ok(row[column], `${row['UI group']} must include ${column}`)
    }
    assert.ok(ALLOWED_STATUSES.has(row.Status), `${row['UI group']} has invalid status ${row.Status}`)
  }

  assert.ok(riskRows.some(row => row['Owner slice'] === OWNER_SLICE))
})

test('visual QA report and progress documentation reference the audit closure', async () => {
  const [report, progress] = await Promise.all([
    readFile(files.report, 'utf8'),
    readFile(files.progress, 'utf8')
  ])

  assert.match(report, /v21-visual-parity-risk-register\.md/)
  assert.match(progress, new RegExp(OWNER_SLICE))
})
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test -- tests/v21-design-parity-audit-closure-contract.test.mjs
```

预期：FAIL。当前风险登记表只覆盖 Public/Auth/Share/System，缺少 `首页`、`邮件`、`日历`、`云盘`、`文档`、`Sheets和labs`、`Pass`、`Collaboration`、`CommandCenter`、`Notifications`、`Admin`、`Setting` 等 UI 组；进度文件也尚未引用 `frontend-v21-design-parity-audit-closure`。

---

### 任务 2：扩展视觉一致性风险登记表

**文件：**

- 修改：`docs/superpowers/progress/v21-visual-parity-risk-register.md`
- 测试：`frontend-v2/tests/v21-design-parity-audit-closure-contract.test.mjs`

- [ ] **步骤 1：替换风险登记表内容**

将 `docs/superpowers/progress/v21-visual-parity-risk-register.md` 替换为：

```markdown
# v2.1 Visual Parity Risk Register

Last updated: 2026-05-12

| UI group | Source design | QA evidence | Status | Notes | Owner slice |
| --- | --- | --- | --- | --- | --- |
| 首页 | `docs/MMMail/UI/首页/工作台-设计概览.png` | `workspace-shell`, `command-palette`, `quick-create`, `theme-drawer` | aligned | Workspace shell keeps the product-first app frame, command surface, quick-create modal, theme drawer, and responsive navigation evidence. | frontend-v21-design-parity-audit-closure |
| 邮件 | `docs/MMMail/UI/邮件/邮件-设计概览.png` | `mail-inbox`, `mail-compose`, `mail-compose-security`, `mail-thread-workbench` | aligned | Mail keeps triage density, folder/list/thread separation, compose security states, attachment strip, and trust panel evidence. | frontend-v21-design-parity-audit-closure |
| 日历 | `docs/MMMail/UI/日历/日历概览.png` | `calendar-board`, `calendar-event-drawer` | aligned | Calendar preserves board, filter sidebar, event drawer, conflict panel, resource state, and retry evidence. | frontend-v21-design-parity-audit-closure |
| 云盘 | `docs/MMMail/UI/云盘/云盘概览.png` | `drive-files`, `drive-share-panel` | aligned | Drive keeps table/card responsive modes and share panel member, public-link, revoke, and retry evidence. | frontend-v21-design-parity-audit-closure |
| 文档 | `docs/MMMail/UI/文档/文档概览.png` | `docs-workspace`, `docs-editor`, `docs-share-panel` | aligned | Docs keeps workspace list, editor canvas, share panel roles, collaborators, link access, error, and retry evidence. | frontend-v21-design-parity-audit-closure |
| Sheets和labs | `docs/MMMail/UI/Sheets和labs/表格概览.png` | `sheets-workspace`, `sheets-editor`, `labs-overview`, `sheets-protected-range` | acceptable-delta | Sheets and Labs share a grouped QA family; current implementation keeps spreadsheet density, preview cards, protected range states, and Labs grid while using MMMail product language instead of sample design brands. | frontend-v21-design-parity-audit-closure |
| Pass | `docs/MMMail/UI/Pass/Pass概览.png` | `pass-vault`, `pass-secure-links`, `pass-monitor`, `pass-secret-actions`, `pass-secure-link-settings`, `pass-risk-detail` | aligned | Pass keeps vault rail, item detail, risk monitor, secret reveal, rotation confirmation, secure-link settings, and retry evidence. | frontend-v21-design-parity-audit-closure |
| Collaboration | `docs/MMMail/UI/Collaboration/协作概览.png` | `collaboration-overview` | acceptable-delta | Collaboration keeps the overview grid and responsive coverage; deeper collaboration interactions remain represented by route-level evidence instead of new overlay states. | frontend-v21-design-parity-audit-closure |
| CommandCenter | `docs/MMMail/UI/CommandCenter/命令概览.png` | `command-center` | aligned | Command Center keeps command grid and terminal log evidence for operational density. | frontend-v21-design-parity-audit-closure |
| Notifications | `docs/MMMail/UI/Notifications/通知概览.png` | `notifications` | aligned | Notifications keeps data-table density and responsive layout evidence. | frontend-v21-design-parity-audit-closure |
| Admin | `docs/MMMail/UI/Admin/管理后台.png` | `admin-overview`, `admin-users`, `admin-system`, `admin-risk` | aligned | Admin keeps governance grid, KPI, system, risk, hosted, and entitlement surfaces aligned with management-console direction. | frontend-v21-design-parity-audit-closure |
| Setting | `docs/MMMail/UI/Setting/设置概览.png` | `settings-overview`, `settings-delete-confirmation` | aligned | Settings keeps panel navigation, overview density, onboarding access, and high-risk delete confirmation evidence. | frontend-v21-design-parity-audit-closure |
| PublicAuthShareSystem | `docs/MMMail/UI/首页/工作台-设计概览.png` and public boundary routes | `login`, `register`, `boundary`, `product-access-blocked`, `offline`, `maintenance`, `not-found`, `server-error` | acceptable-delta | Public pages intentionally use MMMail blank-layout branding rather than the authenticated product shell while preserving first-impression and failure-state clarity. | frontend-v21-design-parity-audit-closure |
| Login | Public auth route and MMMail branding rules | `.tmp/v21-browser-visual-qa/login-desktop.png` | aligned | Login keeps brand story, form, SSO, MFA, and support links visible without claiming auth success. | frontend-v21-design-parity-audit-closure |
| Register | Public auth route and MMMail branding rules | `.tmp/v21-browser-visual-qa/register-desktop.png` | aligned | Register remains a public-shell card with explicit account creation boundary. | frontend-v21-design-parity-audit-closure |
| Boundary | `docs/MMMail/UI/Admin/管理后台.png` and boundary matrix rules | `.tmp/v21-browser-visual-qa/boundary-desktop.png` | aligned | Boundary page exposes Premium, Hosted, maturity, and permission language. | frontend-v21-design-parity-audit-closure |
| System | System state routes | `offline`, `maintenance`, `not-found`, `server-error` | aligned | System pages preserve clear failure and offline states under blank layout. | frontend-v21-design-parity-audit-closure |
| Public shares | Public share routes | `share-mail`, `share-drive`, `share-pass` | acceptable-delta | Public share pages are route-specific rather than one generic share design, but all expose concrete shared content states. | frontend-v21-design-parity-audit-closure |
```

- [ ] **步骤 2：添加 active slice 进度记录**

在 `docs/superpowers/progress/v21-implementation-progress.md` 的 `## Remaining v2.1 Risks` 前插入：

```markdown
## Active Slice

- Slice: `frontend-v21-design-parity-audit-closure`
- Status: `in progress`
- Goal: expand the visual parity risk register to every browser visual QA UI group, refresh screenshot evidence, and update this progress file after implementation is committed.
```

- [ ] **步骤 3：运行合同测试验证通过**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test -- tests/v21-design-parity-audit-closure-contract.test.mjs
```

预期：PASS，输出包含 `# fail 0`。

---

### 任务 3：刷新浏览器 visual QA 报告

**文件：**

- 修改：`docs/superpowers/progress/v21-browser-visual-qa-report.md`
- 读取：`.tmp/v21-browser-visual-qa/*`

- [ ] **步骤 1：运行 visual QA**

运行：

```bash
pnpm --dir frontend-v2 visual:qa
```

预期：PASS，输出包含：

```text
v2.1 browser visual QA passed: 69 screenshots
Report: docs/superpowers/progress/v21-browser-visual-qa-report.md
```

- [ ] **步骤 2：核对报告仍链接风险登记表**

运行：

```bash
rg -n "Total screenshots|Visual parity risk register|v21-visual-parity-risk-register" docs/superpowers/progress/v21-browser-visual-qa-report.md
```

预期：输出包含 `Total screenshots: 69`、`Visual parity risk register` 和 `v21-visual-parity-risk-register.md`。

- [ ] **步骤 3：确认没有提交截图**

运行：

```bash
git status --short --ignored .tmp
```

预期：`.tmp/` 仍是未跟踪或忽略状态，不进入任何 `git add` 命令。

---

### 任务 4：完整验证并提交实现切片

**文件：**

- 修改：`docs/superpowers/progress/v21-visual-parity-risk-register.md`
- 修改：`docs/superpowers/progress/v21-browser-visual-qa-report.md`
- 修改：`docs/superpowers/progress/v21-implementation-progress.md`
- 创建：`frontend-v2/tests/v21-design-parity-audit-closure-contract.test.mjs`

- [ ] **步骤 1：运行完整验证**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test
timeout 60s pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
pnpm --dir frontend-v2 visual:qa
```

预期：

- test：`# fail 0`
- typecheck：退出码 `0`
- build：输出 `built`
- visual QA：`v2.1 browser visual QA passed: 69 screenshots`

- [ ] **步骤 2：检查改动范围和空白错误**

运行：

```bash
git status --short --branch
git diff --check
git diff --stat
```

预期：

- 只看到本任务相关文件和既有未跟踪辅助目录。
- `git diff --check` 无输出。
- `.superpowers/`、`.tmp/`、`docs/MMMail.zip`、`docs/MMMail/`、`frontend/` 不进入暂存区。

- [ ] **步骤 3：暂存实现切片**

运行：

```bash
git add frontend-v2/tests/v21-design-parity-audit-closure-contract.test.mjs
git add -f docs/superpowers/progress/v21-visual-parity-risk-register.md docs/superpowers/progress/v21-browser-visual-qa-report.md docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
```

预期：缓存区只包含合同测试、风险登记表、visual QA 报告和 active progress 记录；`git diff --cached --check` 无输出。

- [ ] **步骤 4：提交实现切片**

运行：

```bash
git commit -m "test(frontend-v2): close v2.1 design parity audit"
git log --oneline -1
```

预期：生成本地 `main` 提交，最新提交信息为 `test(frontend-v2): close v2.1 design parity audit`。

---

### 任务 5：更新最终进度记录并提交

**文件：**

- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：读取实现提交和最新截图数**

运行：

```bash
git log --oneline -1
rg -n "Total screenshots" docs/superpowers/progress/v21-browser-visual-qa-report.md
```

预期：第一条输出是任务 4 的实现提交；第二条输出是 `Total screenshots: 69`。

- [ ] **步骤 2：更新进度文件**

将 `docs/superpowers/progress/v21-implementation-progress.md` 中的 `## Active Slice` 替换为：

```markdown
## Latest Completed Slice

- Slice: `frontend-v21-design-parity-audit-closure`
- Commit: the exact single-line output produced by `git log --oneline -1` after task 4.
- Files changed: added the full UI-group design parity audit contract, expanded `v21-visual-parity-risk-register.md`, refreshed browser visual QA evidence, and recorded all visual QA UI groups against source design images.
- Verification:
  - `timeout 60s pnpm --dir frontend-v2 test`: PASS
  - `timeout 60s pnpm --dir frontend-v2 typecheck`: PASS
  - `pnpm --dir frontend-v2 build`: PASS
  - `pnpm --dir frontend-v2 visual:qa`: PASS (`69 screenshots`)
- Remaining parity risks: no `must-fix` row remains in the current register; `acceptable-delta` rows document intentional MMMail productization differences.
```

然后把 `Current Repository State` 中的 latest frontend implementation commit 更新为任务 4 的实现提交，并把 branch ahead 数更新为当前 `git status --short --branch` 输出。

- [ ] **步骤 3：提交进度记录**

运行：

```bash
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(frontend-v2): update v2.1 design parity progress"
```

预期：提交只包含 `docs/superpowers/progress/v21-implementation-progress.md`。

- [ ] **步骤 4：最终核对**

运行：

```bash
git status --short --branch
git log --oneline -5
```

预期：

- `main` 本地 ahead 数增加。
- 工作树只剩既有未跟踪辅助目录：`.superpowers/`、`.tmp/`、`docs/MMMail.zip`、`docs/MMMail/`、`frontend/`。
- 最近提交包含实现切片提交和进度记录提交。
