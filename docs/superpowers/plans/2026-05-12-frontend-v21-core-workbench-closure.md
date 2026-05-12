# Frontend v2.1 Core Workbench Closure 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 `superpowers-zh:executing-plans` 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 Mail、Calendar、Pass 三个 v2.1 核心工作台按设计图收口，补齐真实交互状态，并把三个超大视图文件拆回 500 行以内。

**架构：** 保留现有路由、API 客户端、Pinia 鉴权和请求竞争保护；把模板、样式、纯函数和本地交互状态拆到模块目录。浏览器 QA 通过真实按钮路径打开面板、弹窗和失败/重试状态，报告继续写入现有 v2.1 QA 报告。

**技术栈：** Vue 3 `<script setup>`、TypeScript、Vue Router、Pinia、Node test、Vite、vue-tsc、Chrome DevTools Protocol visual QA runner。

---

## 规格来源

- `docs/superpowers/specs/2026-04-28-frontend-v21-ui-upgrade-design.md`
- `docs/superpowers/specs/2026-05-12-frontend-v21-core-workbench-closure-design.md`
- `docs/MMMail/UI/邮件/邮件-设计概览.png`
- `docs/MMMail/UI/日历/日历概览.png`
- `docs/MMMail/UI/Pass/Pass概览.png`

## 文件结构

创建：

- `frontend-v2/tests/v21-core-workbench-closure-contract.test.mjs`：源码级合同测试，锁定组件边界、稳定选择器和 500 行限制。
- `frontend-v2/src/views/app/mail/mail-view-helpers.ts`、`mail-types.ts`、`MailFolderRail.vue`、`MailMessageList.vue`、`MailThreadReader.vue`、`MailTrustPanel.vue`、`MailComposePanel.vue`。
- `frontend-v2/src/views/app/calendar/calendar-view-helpers.ts`、`calendar-types.ts`、`CalendarFilterSidebar.vue`、`CalendarBoard.vue`、`CalendarConflictPanel.vue`、`CalendarEventDrawer.vue`。
- `frontend-v2/src/views/app/pass/pass-types.ts`、`pass-view-helpers.ts`、`PassVaultRail.vue`、`PassItemList.vue`、`PassItemDetail.vue`、`PassShareSettingsModal.vue`、`PassRiskMonitorPanel.vue`、`PassConfirmDialog.vue`。
- `frontend-v2/src/views/app/mail-surface-view.css`、`calendar-view.css`、`pass-section-view.css`。

修改：

- `frontend-v2/src/views/app/MailSurfaceView.vue`
- `frontend-v2/src/views/app/CalendarView.vue`
- `frontend-v2/src/views/app/PassSectionView.vue`
- `frontend-v2/scripts/v21-visual-qa/scenarios.mjs`
- `frontend-v2/scripts/v21-visual-qa/browser-harness.mjs`
- `frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`
- `docs/superpowers/progress/v21-browser-visual-qa-report.md`

---

### 任务 1：增加核心工作台源码合同测试

**文件：**

- 创建：`frontend-v2/tests/v21-core-workbench-closure-contract.test.mjs`

- [ ] **步骤 1：编写失败的合同测试**

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const viewFiles = ['MailSurfaceView.vue', 'CalendarView.vue', 'PassSectionView.vue']
const componentFiles = [
  'mail/MailFolderRail.vue', 'mail/MailMessageList.vue', 'mail/MailThreadReader.vue', 'mail/MailTrustPanel.vue', 'mail/MailComposePanel.vue',
  'calendar/CalendarFilterSidebar.vue', 'calendar/CalendarBoard.vue', 'calendar/CalendarEventDrawer.vue', 'calendar/CalendarConflictPanel.vue',
  'pass/PassVaultRail.vue', 'pass/PassItemList.vue', 'pass/PassItemDetail.vue', 'pass/PassShareSettingsModal.vue', 'pass/PassRiskMonitorPanel.vue', 'pass/PassConfirmDialog.vue'
]
const selectors = [
  'mail-folder-rail', 'mail-message-list', 'mail-thread-reader', 'mail-compose-trigger', 'mail-compose-panel', 'mail-trust-panel', 'mail-attachment-strip', 'mail-send-error', 'mail-send-retry', 'mail-discard-confirmation',
  'calendar-filter-sidebar', 'calendar-board', 'calendar-event-trigger', 'calendar-event-drawer', 'calendar-conflict-panel', 'calendar-resource-state', 'calendar-save-error', 'calendar-save-retry',
  'pass-vault-rail', 'pass-item-list', 'pass-item-detail', 'pass-secret-reveal', 'pass-secure-link-trigger', 'pass-share-settings-modal', 'pass-rotate-confirmation', 'pass-revoke-confirmation', 'pass-risk-monitor-panel', 'pass-risk-detail', 'pass-action-error', 'pass-action-retry'
]

test('core workbench closure splits large views and exposes stable selectors', async () => {
  const views = await Promise.all(viewFiles.map(file => readFile(new URL(`../src/views/app/${file}`, import.meta.url), 'utf8')))
  const components = await Promise.all(componentFiles.map(file => readFile(new URL(`../src/views/app/${file}`, import.meta.url), 'utf8')))
  for (const [index, source] of views.entries()) assert.ok(source.split('\n').length <= 500, `${viewFiles[index]} must stay at or below 500 lines`)
  assert.match(views[0], /MailComposePanel/)
  assert.match(views[0], /mail-surface-view\.css/)
  assert.match(views[1], /CalendarEventDrawer/)
  assert.match(views[1], /calendar-view\.css/)
  assert.match(views[2], /PassShareSettingsModal/)
  assert.match(views[2], /pass-section-view\.css/)
  const source = components.join('\n')
  for (const selector of selectors) assert.match(source, new RegExp(selector))
})
```

- [ ] **步骤 2：运行测试验证失败**

运行：`timeout 60s pnpm --dir frontend-v2 test`

预期：FAIL，报错包含 `ENOENT` 或缺少 `MailComposePanel`、`CalendarEventDrawer`、`PassShareSettingsModal`。

- [ ] **步骤 3：Commit 失败测试**

```bash
git add frontend-v2/tests/v21-core-workbench-closure-contract.test.mjs
git commit -m "test(frontend-v2): cover v2.1 core workbench closure"
```

---

### 任务 2：拆分 Mail 工作台并补齐写信交互

**文件：**

- 创建：`frontend-v2/src/views/app/mail/mail-view-helpers.ts`
- 创建：`frontend-v2/src/views/app/mail/mail-types.ts`
- 创建：`frontend-v2/src/views/app/mail/MailFolderRail.vue`
- 创建：`frontend-v2/src/views/app/mail/MailMessageList.vue`
- 创建：`frontend-v2/src/views/app/mail/MailThreadReader.vue`
- 创建：`frontend-v2/src/views/app/mail/MailTrustPanel.vue`
- 创建：`frontend-v2/src/views/app/mail/MailComposePanel.vue`
- 创建：`frontend-v2/src/views/app/mail-surface-view.css`
- 修改：`frontend-v2/src/views/app/MailSurfaceView.vue`

- [ ] **步骤 1：移动 Mail 纯函数和类型**

```ts
export function isEmailLike(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

export function validateMailDraft(draft: { body: string; subject: string; toEmail: string }) {
  if (!isEmailLike(draft.toEmail.trim())) return 'Enter a valid recipient before sending.'
  if (!draft.subject.trim()) return 'Add a subject before sending.'
  if (!draft.body.trim()) return 'Write a message before sending.'
  return ''
}
```

- [ ] **步骤 2：创建展示组件**

`MailComposePanel.vue` 接收 `draft`、`senderOptions`、`trustState`、`trustCopy`、`sending`、`sendError`，emit `update:draft`、`submit`、`retry`、`discard`，并包含：

```vue
<section class="mail-compose-panel">
  <button class="mail-compose-trigger mail-send-trigger" type="button" @click="$emit('submit')">Send</button>
  <div class="mail-trust-panel">...</div>
  <div class="mail-attachment-strip">...</div>
  <p v-if="sendError" class="mail-send-error">{{ sendError }}</p>
  <button v-if="sendError" class="mail-send-retry" type="button" @click="$emit('retry')">Retry</button>
  <div v-if="discardConfirmationOpen" class="mail-discard-confirmation" role="dialog" aria-modal="true">...</div>
</section>
```

其余组件必须提供 `.mail-folder-rail`、`.mail-message-list`、`.mail-thread-reader`、`.mail-trust-panel`。

- [ ] **步骤 3：改造 `MailSurfaceView.vue`**

保留 `listMailFolder()`、`listSenderIdentities()`、`readMailDetail()`、`readRecipientTrustState()`、`sendMail()`、`latestWorkspaceRequest`、`latestRecipientTrustRequest`。新增：

```ts
const composeValidationError = ref('')
const composeSendError = ref('')
const discardConfirmationOpen = ref(false)
```

`submitCompose()` 先调用 `validateMailDraft()`；校验失败和 API 发送失败都写入 `.mail-send-error`，retry 调回同一个 `submitCompose()`。

- [ ] **步骤 4：抽出样式并验证**

将原 `<style scoped>` 移入 `mail-surface-view.css`，在视图顶部导入 `import './mail-surface-view.css'`。

运行：`timeout 60s pnpm --dir frontend-v2 test -- tests/mail-workspace-contract.test.mjs tests/v21-core-workbench-closure-contract.test.mjs`

预期：PASS。

- [ ] **步骤 5：Commit Mail 切片**

```bash
git add frontend-v2/src/views/app/MailSurfaceView.vue frontend-v2/src/views/app/mail frontend-v2/src/views/app/mail-surface-view.css
git commit -m "feat(frontend-v2): close v2.1 mail workbench interactions"
```

---

### 任务 3：拆分 Calendar 工作台并补齐事件抽屉

**文件：**

- 创建：`frontend-v2/src/views/app/calendar/calendar-view-helpers.ts`
- 创建：`frontend-v2/src/views/app/calendar/calendar-types.ts`
- 创建：`frontend-v2/src/views/app/calendar/CalendarFilterSidebar.vue`
- 创建：`frontend-v2/src/views/app/calendar/CalendarBoard.vue`
- 创建：`frontend-v2/src/views/app/calendar/CalendarConflictPanel.vue`
- 创建：`frontend-v2/src/views/app/calendar/CalendarEventDrawer.vue`
- 创建：`frontend-v2/src/views/app/calendar-view.css`
- 修改：`frontend-v2/src/views/app/CalendarView.vue`

- [ ] **步骤 1：移动 Calendar 纯函数和类型**

`calendar-view-helpers.ts` 导出 `parseDate()`、`startOfDay()`、`startOfWeek()`、`addDays()`、`formatDateKey()`、`formatDateWindow()`、`mergeCalendarSurfaceItems()`。

`calendar-types.ts` 导出 `CalendarViewMode`、`CalendarSurfaceItem`、`CalendarDayCell`、`CalendarTimeSlot`、`PositionedCalendarEvent`。

- [ ] **步骤 2：创建侧栏、主板、冲突和抽屉组件**

`CalendarFilterSidebar.vue` 包含 `.calendar-filter-sidebar` 和 `.calendar-resource-state`。`CalendarBoard.vue` 包含 `.calendar-board`，事件按钮包含 `.calendar-event-trigger`。

```vue
<aside class="calendar-event-drawer" role="dialog" aria-label="Edit calendar event">
  <input aria-label="Event title">
  <input aria-label="Date">
  <input aria-label="Time">
  <input aria-label="Location">
  <textarea aria-label="Notes" />
  <section class="calendar-conflict-panel"><div class="calendar-resource-state">...</div></section>
  <p v-if="saveError" class="calendar-save-error">{{ saveError }}</p>
  <button v-if="saveError" class="calendar-save-retry calendar-event-drawer__save" type="button" @click="$emit('retry')">Retry</button>
</aside>
```

- [ ] **步骤 3：改造 `CalendarView.vue`**

保留 `listCalendarEvents()`、`listCalendarAgenda()`、`queryCalendarAvailability()`、`latestCalendarRequest`。新增：

```ts
const eventDrawerOpen = ref(false)
const calendarSaveError = ref('')
const unsavedEventChanges = ref(false)
```

`saveEventDraft()` 显示本地失败文本 `Calendar save requires an API endpoint in the next backend slice.`，并显示 `.calendar-save-retry`。

- [ ] **步骤 4：抽出样式并验证**

将原 `<style scoped>` 移入 `calendar-view.css`，在视图顶部导入 `import './calendar-view.css'`。

运行：`timeout 60s pnpm --dir frontend-v2 test -- tests/calendar-workspace-contract.test.mjs tests/v21-core-workbench-closure-contract.test.mjs`

预期：PASS。

- [ ] **步骤 5：Commit Calendar 切片**

```bash
git add frontend-v2/src/views/app/CalendarView.vue frontend-v2/src/views/app/calendar frontend-v2/src/views/app/calendar-view.css
git commit -m "feat(frontend-v2): close v2.1 calendar workbench interactions"
```

---

### 任务 4：拆分 Pass 工作台并补齐安全动作

**文件：**

- 创建：`frontend-v2/src/views/app/pass/pass-types.ts`
- 创建：`frontend-v2/src/views/app/pass/pass-view-helpers.ts`
- 创建：`frontend-v2/src/views/app/pass/PassVaultRail.vue`
- 创建：`frontend-v2/src/views/app/pass/PassItemList.vue`
- 创建：`frontend-v2/src/views/app/pass/PassItemDetail.vue`
- 创建：`frontend-v2/src/views/app/pass/PassShareSettingsModal.vue`
- 创建：`frontend-v2/src/views/app/pass/PassRiskMonitorPanel.vue`
- 创建：`frontend-v2/src/views/app/pass/PassConfirmDialog.vue`
- 创建：`frontend-v2/src/views/app/pass-section-view.css`
- 修改：`frontend-v2/src/views/app/PassSectionView.vue`

- [ ] **步骤 1：移动 Pass 纯函数和类型**

`pass-view-helpers.ts` 导出排序、格式化、头像、风险计数和文本拼接函数。`pass-types.ts` 导出 `PassSurfaceEntry`、`PassCardFact`、`PassDetailCard`、`PassConfirmAction`。

- [ ] **步骤 2：创建展示和动作组件**

`PassVaultRail.vue` 提供 `.pass-vault-rail`，`PassItemList.vue` 提供 `.pass-item-list`，`PassItemDetail.vue` 提供 `.pass-item-detail`、`.pass-secret-reveal`、`.pass-secure-link-trigger`。

```vue
<section class="pass-share-settings-modal" role="dialog" aria-modal="true">
  <input aria-label="Share recipients">
  <select aria-label="Share permission">...</select>
  <input aria-label="Expiration date">
  <p v-if="actionError" class="pass-action-error">{{ actionError }}</p>
  <button v-if="actionError" class="pass-action-retry pass-share-settings-modal__save" type="button" @click="$emit('retry')">Retry</button>
</section>
```

`PassRiskMonitorPanel.vue` 包含 `.pass-risk-monitor-panel`、`.pass-risk-trigger`、`.pass-risk-detail`。`PassConfirmDialog.vue` 根据 action 输出 `.pass-rotate-confirmation` 或 `.pass-revoke-confirmation`。

- [ ] **步骤 3：改造 `PassSectionView.vue`**

保留 `listPassItems()`、`listPassMailboxes()`、`readPassMonitor()`、`derivePassSectionState()`、`latestPassRequest`。新增：

```ts
const secretVisible = ref(false)
const shareSettingsOpen = ref(false)
const passActionError = ref('')
const confirmAction = ref<PassConfirmAction | null>(null)
const selectedRiskId = ref('')
```

高风险动作只在确认后更新本地 UI 状态；失败文本使用 `.pass-action-error` 暴露，并通过 `.pass-action-retry` 保持可重试。

- [ ] **步骤 4：抽出样式并验证**

将原 `<style scoped>` 移入 `pass-section-view.css`，在视图顶部导入 `import './pass-section-view.css'`。

运行：`timeout 60s pnpm --dir frontend-v2 test -- tests/pass-workspace-contract.test.mjs tests/v21-core-workbench-closure-contract.test.mjs`

预期：PASS。

- [ ] **步骤 5：Commit Pass 切片**

```bash
git add frontend-v2/src/views/app/PassSectionView.vue frontend-v2/src/views/app/pass frontend-v2/src/views/app/pass-section-view.css
git commit -m "feat(frontend-v2): close v2.1 pass workbench interactions"
```

---

### 任务 5：扩展浏览器 QA 场景和动作

**文件：**

- 修改：`frontend-v2/scripts/v21-visual-qa/scenarios.mjs`
- 修改：`frontend-v2/scripts/v21-visual-qa/browser-harness.mjs`
- 修改：`frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`

- [ ] **步骤 1：先更新 QA 合同测试并确认失败**

在 `requiredEvidenceIds` 加入 `mail-compose-security`、`mail-thread-workbench`、`calendar-event-drawer`、`pass-secret-actions`、`pass-secure-link-settings`、`pass-risk-detail`。

在 `requiredOverlayContracts` 加入：

```js
{ action: 'activateMailComposeSecurity', id: 'mail-compose-security', selector: '.mail-trust-panel' },
{ action: 'openCalendarEventDrawer', id: 'calendar-event-drawer', selector: '.calendar-event-drawer' },
{ action: 'activatePassSecretActions', id: 'pass-secret-actions', selector: '.pass-rotate-confirmation' },
{ action: 'openPassShareSettings', id: 'pass-secure-link-settings', selector: '.pass-share-settings-modal' },
{ action: 'openPassRiskDetail', id: 'pass-risk-detail', selector: '.pass-risk-detail' }
```

运行：`timeout 60s pnpm --dir frontend-v2 test -- tests/v21-browser-visual-qa-contract.test.mjs`

预期：FAIL，缺少新增 scenario id 或 action。

- [ ] **步骤 2：新增 QA scenario**

```js
overlay('mail-compose-security', '邮件', '/mail/compose', 'activateMailComposeSecurity', ['.mail-compose-panel', '.mail-trust-panel', '.mail-attachment-strip', '.mail-send-error', '.mail-send-retry', '.mail-discard-confirmation']),
overlay('mail-thread-workbench', '邮件', '/mail/inbox', 'none', ['.mail-folder-rail', '.mail-message-list', '.mail-thread-reader', '.mail-attachment-strip']),
overlay('calendar-event-drawer', '日历', '/calendar', 'openCalendarEventDrawer', ['.calendar-event-drawer', '.calendar-conflict-panel', '.calendar-resource-state', '.calendar-save-error', '.calendar-save-retry']),
overlay('pass-secret-actions', 'Pass', '/pass', 'activatePassSecretActions', ['.pass-item-detail', '.pass-secret-reveal', '.pass-rotate-confirmation', '.pass-action-error', '.pass-action-retry']),
overlay('pass-secure-link-settings', 'Pass', '/pass/secure-links', 'openPassShareSettings', ['.pass-share-settings-modal', '.pass-action-error', '.pass-action-retry']),
overlay('pass-risk-detail', 'Pass', '/pass/monitor', 'openPassRiskDetail', ['.pass-risk-monitor-panel', '.pass-risk-detail', '.pass-action-retry'])
```

- [ ] **步骤 3：新增浏览器动作**

```js
activateMailComposeSecurity: clickSequenceExpression(['.mail-send-trigger', '.mail-discard-trigger']),
openCalendarEventDrawer: clickAndSubmitExpression('.calendar-event-trigger', '.calendar-event-drawer__save'),
activatePassSecretActions: clickSequenceExpression(['.pass-secret-reveal', '.pass-rotate-trigger']),
openPassShareSettings: clickAndSubmitExpression('.pass-secure-link-trigger', '.pass-share-settings-modal__save'),
openPassRiskDetail: clickSelectorExpression('.pass-risk-trigger')
```

实现 `clickSequenceExpression(selectors)`：逐个查询 selector、点击、用 `SELECTOR_RETRY_DELAY_MS` 等待下一个 selector，不存在时返回 `false`。

- [ ] **步骤 4：验证并提交 QA 切片**

运行：`timeout 60s pnpm --dir frontend-v2 test -- tests/v21-browser-visual-qa-contract.test.mjs tests/v21-core-workbench-closure-contract.test.mjs`

预期：PASS。

```bash
git add frontend-v2/scripts/v21-visual-qa/scenarios.mjs frontend-v2/scripts/v21-visual-qa/browser-harness.mjs frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs
git commit -m "test(frontend-v2): expand v2.1 core workbench visual qa"
```

---

### 任务 6：完整验证、刷新报告和交付

**文件：**

- 修改：`docs/superpowers/progress/v21-browser-visual-qa-report.md`

- [ ] **步骤 1：运行完整验证**

```bash
timeout 60s pnpm --dir frontend-v2 test
timeout 60s pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
pnpm --dir frontend-v2 visual:qa
```

预期：四条命令退出码均为 0；报告包含 `mail-compose-security`、`calendar-event-drawer`、`pass-secret-actions`、`pass-secure-link-settings`、`pass-risk-detail`。

- [ ] **步骤 2：检查行数和工作树**

```bash
wc -l frontend-v2/src/views/app/MailSurfaceView.vue frontend-v2/src/views/app/CalendarView.vue frontend-v2/src/views/app/PassSectionView.vue
git status --short --branch
```

预期：三个视图文件每个不超过 500 行；工作树只包含本计划相关改动和既有无关未跟踪项。

- [ ] **步骤 3：提交 QA 报告**

```bash
git add -f docs/superpowers/progress/v21-browser-visual-qa-report.md
git diff --cached --check
git commit -m "docs(frontend-v2): refresh v2.1 core workbench visual qa report"
```

- [ ] **步骤 4：最终审查**

运行：

```bash
git log --oneline -8
git status --short --branch
```

最终回复必须列出新增/修改的核心文件、三个主视图最终行数、`test`、`typecheck`、`build`、`visual:qa` 的最新运行结果、本轮提交哈希。
