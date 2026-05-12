# Frontend v2.1 UI Coverage Visual QA 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 扩展 v2.1 浏览器视觉 QA，使其覆盖 `docs/MMMail/UI` 的全部设计图组、主要产品路由、公共边界页和关键弹层/面板证据。

**架构：** 保留 `pnpm --dir frontend-v2 visual:qa` 作为唯一入口，将当前 490 行入口拆成入口编排、CDP 客户端、浏览器 harness、场景注册表和报告生成器。场景注册表显式声明 UI 图组、路由、视口、选择器和证据类型，报告按 UI 图组输出覆盖矩阵。

**技术栈：** Node.js ESM、Chrome DevTools Protocol、Vite dev server、`node:test` 契约测试、Vue 现有 CSS 类名。

---

## 文件结构

- 修改：`frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`：锁定全部 UI 图组、路由、overlay/panel evidence、报告字段。
- 创建：`frontend-v2/scripts/v21-visual-qa/scenarios.mjs`：声明 viewports、route scenarios、public scenarios、overlay scenarios。
- 创建：`frontend-v2/scripts/v21-visual-qa/cdp-client.mjs`：封装 CDP WebSocket 请求、响应和事件等待。
- 创建：`frontend-v2/scripts/v21-visual-qa/browser-harness.mjs`：启动 Vite/Chrome、导航、截图、DOM 断言和动作表达式。
- 创建：`frontend-v2/scripts/v21-visual-qa/report.mjs`：生成按 UI 图组聚合的 Markdown 报告。
- 修改：`frontend-v2/scripts/v21-browser-visual-qa.mjs`：入口编排，保持小于 500 行。
- 修改：`docs/superpowers/progress/v21-browser-visual-qa-report.md`：真实运行生成的最新报告。

## 任务 1：写失败的覆盖契约测试

**文件：**
- 修改：`frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`

- [ ] **步骤 1：替换契约测试**

使用下面的测试结构替换现有文件，关键是读取即将创建的 `scenarios.mjs` 和 `report.mjs`，先让测试失败。

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const packageJsonUrl = new URL('../package.json', import.meta.url)
const qaScriptUrl = new URL('../scripts/v21-browser-visual-qa.mjs', import.meta.url)
const scenariosUrl = new URL('../scripts/v21-visual-qa/scenarios.mjs', import.meta.url)
const reportUrl = new URL('../scripts/v21-visual-qa/report.mjs', import.meta.url)

const requiredUiGroups = ['首页', '邮件', '日历', '云盘', '文档', 'Sheets和labs', 'Pass', 'Collaboration', 'CommandCenter', 'Notifications', 'Admin', 'Setting', 'PublicAuthShareSystem']
const requiredRoutes = ['/workspace', '/mail/inbox', '/mail/compose', '/calendar', '/drive', '/docs', '/docs/demo-document', '/sheets', '/sheets/demo-sheet', '/labs', '/pass', '/pass/secure-links', '/pass/monitor', '/collaboration', '/command-center', '/notifications', '/admin', '/admin/users', '/admin/system', '/admin/risk', '/settings', '/login', '/register', '/boundary', '/product-access-blocked', '/share/mail/demo-token', '/share/drive/demo-token', '/share/pass/demo-token', '/offline', '/maintenance', '/404', '/500']
const requiredEvidenceIds = ['command-palette', 'quick-create', 'theme-drawer', 'mail-compose', 'drive-share-panel', 'docs-share-panel', 'sheets-protected-range', 'settings-delete-confirmation']

test('v2.1 browser visual QA runner exposes expanded coverage registry', async () => {
  const [packageJsonRaw, qaScript, scenarioSource, reportSource] = await Promise.all([
    readFile(packageJsonUrl, 'utf8'),
    readFile(qaScriptUrl, 'utf8'),
    readFile(scenariosUrl, 'utf8'),
    readFile(reportUrl, 'utf8')
  ])
  const packageJson = JSON.parse(packageJsonRaw)

  assert.equal(packageJson.scripts['visual:qa'], 'node scripts/v21-browser-visual-qa.mjs')
  assert.match(qaScript, /runVisualQa/)
  assert.match(qaScript, /Chrome DevTools Protocol/)
  assert.match(scenarioSource, /1440/)
  assert.match(scenarioSource, /1024/)
  assert.match(scenarioSource, /390/)
  for (const group of requiredUiGroups) assert.match(scenarioSource, new RegExp(group))
  for (const route of requiredRoutes) assert.match(scenarioSource, new RegExp(route.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  for (const evidenceId of requiredEvidenceIds) assert.match(scenarioSource, new RegExp(evidenceId))
  assert.match(reportSource, /UI group/)
  assert.match(reportSource, /Screenshot evidence/)
  assert.match(reportSource, /Covered overlay and panel evidence/)
  assert.match(reportSource, /v21-browser-visual-qa-report\.md/)
})
```

- [ ] **步骤 2：运行测试验证失败**

运行：`timeout 60s pnpm --dir frontend-v2 test -- v21-browser-visual-qa-contract`

预期：FAIL，报错包含 `ENOENT`，因为 `frontend-v2/scripts/v21-visual-qa/scenarios.mjs` 还不存在。

- [ ] **步骤 3：提交失败测试**

```bash
git add frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs
git diff --cached --check
git commit -m "test(frontend-v2): require expanded v2.1 visual qa coverage"
```

## 任务 2：创建场景注册表和报告模块

**文件：**
- 创建：`frontend-v2/scripts/v21-visual-qa/scenarios.mjs`
- 创建：`frontend-v2/scripts/v21-visual-qa/report.mjs`

- [ ] **步骤 1：创建 `scenarios.mjs`**

实现字段约定：`id`、`uiGroup`、`path`、`checks`、`viewports`、`viewportChecks`、`action`。必须包含：

```js
export const VIEWPORTS = [
  { height: 900, mobile: false, name: 'desktop', width: 1440 },
  { height: 768, mobile: false, name: 'tablet', width: 1024 },
  { height: 844, mobile: false, name: 'mobile', width: 390 }
]

export const DESKTOP_VIEWPORT = VIEWPORTS[0]
export const ALL_VIEWPORTS = ['desktop', 'tablet', 'mobile']
const APP_SHELL_CHECKS = ['.base-layout', '.top-bar', '.base-layout__content']
```

`ROUTE_SCENARIOS` 至少包含这些真实选择器：

```js
{ id: 'workspace-shell', uiGroup: '首页', path: '/workspace', checks: APP_SHELL_CHECKS, viewports: ALL_VIEWPORTS }
{ id: 'mail-inbox', uiGroup: '邮件', path: '/mail/inbox', checks: [...APP_SHELL_CHECKS, '.mail-surface', '.mail-workspace'], viewports: ALL_VIEWPORTS }
{ id: 'calendar-board', uiGroup: '日历', path: '/calendar', checks: [...APP_SHELL_CHECKS, '.calendar-page', '.calendar-board', '.calendar-drawer'], viewports: ALL_VIEWPORTS }
{ id: 'drive-files', uiGroup: '云盘', path: '/drive', checks: [...APP_SHELL_CHECKS, '.drive-surface', '.drive-surface__table', '.drive-surface__detail'], viewports: ALL_VIEWPORTS }
{ id: 'docs-workspace', uiGroup: '文档', path: '/docs', checks: [...APP_SHELL_CHECKS, '.docs-shell', '.docs-shell__list'], viewports: ALL_VIEWPORTS }
{ id: 'docs-editor', uiGroup: '文档', path: '/docs/demo-document', checks: [...APP_SHELL_CHECKS, '.docs-editor', '.docs-editor__canvas'], viewports: ['desktop'] }
{ id: 'sheets-workspace', uiGroup: 'Sheets和labs', path: '/sheets', checks: [...APP_SHELL_CHECKS, '.sheets-workspace', '.data-table', '.sheets-preview'], viewports: ALL_VIEWPORTS }
{ id: 'sheets-editor', uiGroup: 'Sheets和labs', path: '/sheets/demo-sheet', checks: [...APP_SHELL_CHECKS, '.sheets-editor', '.sheets-editor__grid'], viewports: ['desktop'] }
{ id: 'labs-overview', uiGroup: 'Sheets和labs', path: '/labs', checks: [...APP_SHELL_CHECKS, '.labs-grid'], viewports: ['desktop'] }
{ id: 'pass-vault', uiGroup: 'Pass', path: '/pass', checks: [...APP_SHELL_CHECKS, '.pass-surface', '.pass-surface__list'], viewports: ALL_VIEWPORTS }
{ id: 'pass-secure-links', uiGroup: 'Pass', path: '/pass/secure-links', checks: [...APP_SHELL_CHECKS, '.pass-surface__detail'], viewports: ['desktop'] }
{ id: 'pass-monitor', uiGroup: 'Pass', path: '/pass/monitor', checks: [...APP_SHELL_CHECKS, '.security-page'], viewports: ['desktop'] }
{ id: 'collaboration-overview', uiGroup: 'Collaboration', path: '/collaboration', checks: [...APP_SHELL_CHECKS, '.collaboration-grid'], viewports: ALL_VIEWPORTS }
{ id: 'command-center', uiGroup: 'CommandCenter', path: '/command-center', checks: [...APP_SHELL_CHECKS, '.command-grid', '.terminal-log'], viewports: ALL_VIEWPORTS }
{ id: 'notifications', uiGroup: 'Notifications', path: '/notifications', checks: [...APP_SHELL_CHECKS, '.notifications-layout', '.data-table'], viewports: ALL_VIEWPORTS }
{ id: 'admin-overview', uiGroup: 'Admin', path: '/admin', checks: [...APP_SHELL_CHECKS, '.admin-page', '.admin-grid'], viewports: ALL_VIEWPORTS }
{ id: 'settings-overview', uiGroup: 'Setting', path: '/settings', checks: [...APP_SHELL_CHECKS, '.settings-shell', '.settings-panel'], viewports: ALL_VIEWPORTS }
```

再补齐 `/admin/users`、`/admin/system`、`/admin/risk`，以及 public/auth/share/system 路由。`OVERLAY_SCENARIOS` 必须包含 8 个 evidence id：`command-palette`、`quick-create`、`theme-drawer`、`mail-compose`、`drive-share-panel`、`docs-share-panel`、`sheets-protected-range`、`settings-delete-confirmation`。

- [ ] **步骤 2：创建 `report.mjs`**

导出 `REPORT_PATH_SUFFIX` 和 `writeVisualQaReport()`。报告必须包含这些标题：

```markdown
# v2.1 Browser Visual QA Report
## UI group coverage
## Scenario evidence
## Covered overlay and panel evidence
```

每行 evidence 输出 `UI group`、`Scenario`、`Viewport`、`Route`、`Required visible selectors`、`Screenshot evidence`。

- [ ] **步骤 3：运行契约测试**

运行：`timeout 60s pnpm --dir frontend-v2 test -- v21-browser-visual-qa-contract`

预期：仍 FAIL；此时文件存在，失败点应转为入口脚本尚未导出或使用 `runVisualQa`。

## 任务 3：拆分 CDP/harness 并重写入口

**文件：**
- 创建：`frontend-v2/scripts/v21-visual-qa/cdp-client.mjs`
- 创建：`frontend-v2/scripts/v21-visual-qa/browser-harness.mjs`
- 修改：`frontend-v2/scripts/v21-browser-visual-qa.mjs`

- [ ] **步骤 1：创建 `cdp-client.mjs`**

从现有入口脚本搬移 `CdpClient` 类并导出。保留 `send()`、`waitForEvent()`、`resolvePending()`、`resolveWaiters()` 的现有错误暴露行为；不要吞掉 CDP 错误。

- [ ] **步骤 2：创建 `browser-harness.mjs`**

从现有入口脚本搬移这些真实逻辑：`prepareOutput`、`resolveChromePath`、`startViteServer`、`startChrome`、`connectCdp`、`stopProcess`、`findFreePort`、`buildDomCheckExpression`、`clickByLabelExpression`、`clickSelectorExpression`、`captureRouteScenario`、`captureOverlayScenario`。动作注册表必须是：

```js
const ACTION_EXPRESSIONS = {
  clickCommandPalette: clickByLabelExpression('Command palette|命令面板'),
  clickDeleteAccount: clickByLabelExpression('Delete account|删除账户|刪除帳戶'),
  clickQuickCreate: clickSelectorExpression('.quick-create-button'),
  clickThemeDrawer: clickByLabelExpression('Theme settings|主题设置|主題設定'),
  none: '(() => true)()'
}
```

- [ ] **步骤 3：重写入口脚本**

入口脚本只保留配置、`runVisualQa()`、`runScenarios()` 和错误出口。必须导入：

```js
import { DESKTOP_VIEWPORT, OVERLAY_SCENARIOS, allRouteScenarios, resolveScenarioViewports } from './v21-visual-qa/scenarios.mjs'
import { REPORT_PATH_SUFFIX, writeVisualQaReport } from './v21-visual-qa/report.mjs'
```

`runScenarios()` 对所有 route scenarios 按声明 viewports 截图，对 overlay scenarios 使用 desktop 截图。

- [ ] **步骤 4：运行契约测试验证通过**

运行：`timeout 60s pnpm --dir frontend-v2 test -- v21-browser-visual-qa-contract`

预期：PASS。

## 任务 4：运行真实浏览器 QA 并修正选择器

**文件：**
- 修改：`frontend-v2/scripts/v21-visual-qa/scenarios.mjs`
- 修改：`docs/superpowers/progress/v21-browser-visual-qa-report.md`

- [ ] **步骤 1：运行真实浏览器 QA**

运行：`pnpm --dir frontend-v2 visual:qa`

预期：如果 FAIL，错误会明确指出缺失选择器、路由或 Vite 渲染错误。

- [ ] **步骤 2：只修正真实缺失选择器**

规则：

```text
页面已有稳定类名时，更新 scenarios.mjs 的 selector。
页面不存在该证据时，不伪造 selector，不吞掉失败。
设计证据落成路由态或侧栏面板时，保留 evidence id，并用真实类名断言。
```

- [ ] **步骤 3：重新运行真实浏览器 QA**

运行：`pnpm --dir frontend-v2 visual:qa`

预期：PASS，输出包含 `v2.1 browser visual QA passed`，报告写入 `docs/superpowers/progress/v21-browser-visual-qa-report.md`。

## 任务 5：全量验证并提交实现

**文件：**
- 修改：`frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`
- 修改：`frontend-v2/scripts/v21-browser-visual-qa.mjs`
- 创建：`frontend-v2/scripts/v21-visual-qa/scenarios.mjs`
- 创建：`frontend-v2/scripts/v21-visual-qa/cdp-client.mjs`
- 创建：`frontend-v2/scripts/v21-visual-qa/browser-harness.mjs`
- 创建：`frontend-v2/scripts/v21-visual-qa/report.mjs`
- 修改：`docs/superpowers/progress/v21-browser-visual-qa-report.md`

- [ ] **步骤 1：运行全量验证**

```bash
pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
```

预期：三条命令退出码均为 0。

- [ ] **步骤 2：检查文件行数**

运行：

```bash
wc -l frontend-v2/scripts/v21-browser-visual-qa.mjs frontend-v2/scripts/v21-visual-qa/*.mjs frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs
```

预期：单个源文件均小于 500 行。

- [ ] **步骤 3：检查并提交**

```bash
git status --short --branch
git add frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs \
  frontend-v2/scripts/v21-browser-visual-qa.mjs \
  frontend-v2/scripts/v21-visual-qa/scenarios.mjs \
  frontend-v2/scripts/v21-visual-qa/cdp-client.mjs \
  frontend-v2/scripts/v21-visual-qa/browser-harness.mjs \
  frontend-v2/scripts/v21-visual-qa/report.mjs \
  docs/superpowers/progress/v21-browser-visual-qa-report.md
git diff --cached --check
git diff --cached --stat
git commit -m "test(frontend-v2): expand v2.1 visual qa coverage"
git status --short --branch
```

预期：提交成功；无关未跟踪文件仍未被暂存。

## 自检

- 规格覆盖度：覆盖已批准设计规格中的覆盖矩阵、全 UI 图组、公共边界、overlay/panel evidence、报告字段和契约测试。
- 占位符扫描：计划不包含空函数实现；缺失 selector 的处理规则是暴露失败并修正真实选择器。
- 类型一致性：场景对象字段统一为 `id`、`uiGroup`、`path`、`checks`、`viewports`、`viewportChecks`、`action`。
- 工程约束：入口脚本拆分，避免超过 500 行；不加入 Playwright/Puppeteer；不提交截图；不使用 `git add .`。
