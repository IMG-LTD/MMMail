# Frontend v2.1 Interaction Closure 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 Drive 分享设置、Docs 分享权限和 Sheets 保护区域从静态视觉 QA 证据升级为真实可点击 overlay 状态。

**架构：** 不继续膨胀已超 500 行的页面文件；为三个 overlay 分别创建局部 Vue 组件，由现有页面只负责打开/关闭与传入上下文。浏览器 QA 场景改为点击真实触发器，并断言 overlay 专属选择器、错误/重试区域和权限控件。

**技术栈：** Vue 3 `<script setup>`、Naive UI 现有 Modal 包装组件、`node:test` 契约测试、Chrome DevTools Protocol 视觉 QA。

---

## 文件结构

- 修改：`frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`：锁定三个 overlay 的 action 和专属 selector。
- 创建：`frontend-v2/src/views/app/drive/DriveSharePanel.vue`：Drive 分享设置 modal，展示成员、公开链接、撤销确认、错误和重试。
- 创建：`frontend-v2/src/views/app/docs/DocsSharePanel.vue`：Docs 分享权限 modal，展示邀请、角色、链接权限、协作者、校验错误和重试。
- 创建：`frontend-v2/src/views/app/sheets/SheetsProtectedRangeModal.vue`：Sheets 保护区域 modal，展示区域输入、编辑者、模式、冲突、保存错误和重试。
- 修改：`frontend-v2/src/views/app/DriveSectionView.vue`：接入 Drive share trigger 和 panel。
- 修改：`frontend-v2/src/views/app/DocsEditorView.vue`：接入 Docs share trigger 和 panel。
- 修改：`frontend-v2/src/views/app/SheetsEditorView.vue`：接入 protected range trigger、modal 和 protected range cell 标记。
- 修改：`frontend-v2/scripts/v21-visual-qa/scenarios.mjs`：overlay 场景改为真实 click action 与专属 selector。
- 修改：`frontend-v2/scripts/v21-visual-qa/browser-harness.mjs`：增加三个 overlay click action。
- 修改：`docs/superpowers/progress/v21-browser-visual-qa-report.md`：由真实 `visual:qa` 重新生成。

## 任务 1：写失败的 overlay 契约测试

**文件：**
- 修改：`frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`

- [ ] **步骤 1：加入专属 selector 和 action 断言**

在现有测试中加入下列常量和循环：

```js
const requiredOverlayContracts = [
  { action: 'clickDriveSharePanel', id: 'drive-share-panel', selector: '.drive-share-panel' },
  { action: 'clickDocsSharePanel', id: 'docs-share-panel', selector: '.docs-share-panel' },
  { action: 'clickSheetsProtectedRange', id: 'sheets-protected-range', selector: '.sheets-protected-range-modal' }
]
```

在测试主体里追加：

```js
for (const contract of requiredOverlayContracts) {
  assert.match(scenarioSource, new RegExp(contract.id))
  assert.match(scenarioSource, new RegExp(contract.action))
  assert.match(scenarioSource, new RegExp(contract.selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
}
assert.doesNotMatch(scenarioSource, /overlay\('drive-share-panel', '云盘', '\/drive', 'none', \['\.drive-surface', '\.drive-surface__table'\]\)/)
assert.doesNotMatch(scenarioSource, /overlay\('docs-share-panel', '文档', '\/docs\/demo-document', 'none', \['\.docs-editor__actions', '\.docs-editor__panel'\]\)/)
assert.doesNotMatch(scenarioSource, /overlay\('sheets-protected-range', 'Sheets和labs', '\/sheets\/demo-sheet', 'none', \['\.sheets-editor__formula', '\.sheets-editor__side'\]\)/)
```

- [ ] **步骤 2：运行测试验证失败**

运行：`timeout 60s pnpm --dir frontend-v2 test -- v21-browser-visual-qa-contract`

预期：FAIL，失败点包含 `clickDriveSharePanel` 或 `.drive-share-panel` 未匹配。

## 任务 2：实现 Drive 分享设置面板

**文件：**
- 创建：`frontend-v2/src/views/app/drive/DriveSharePanel.vue`
- 修改：`frontend-v2/src/views/app/DriveSectionView.vue`

- [ ] **步骤 1：创建 Drive panel 组件**

组件接口：

```ts
defineProps<{
  item: DriveItem | null
  shares: DriveShareLink[]
  show: boolean
}>()
```

必须渲染：

```vue
<Modal
  class="drive-share-panel"
  :show="show"
  size="lg"
  title="Share settings"
  @update:show="emit('update:show', $event)"
>
  <section class="drive-share-panel__summary">
    <h3 class="drive-share-panel__title">{{ item?.name || 'No file selected' }}</h3>
    <StatusBadge class="drive-share-panel__sensitivity" label="Internal" tone="warning" />
  </section>
  <div class="drive-share-panel__members">owner / editor / viewer rows</div>
  <div class="drive-share-panel__public-link">public link controls</div>
  <div class="drive-share-panel__error" role="alert">explicit unavailable/failure state</div>
  <button class="drive-share-panel__retry" type="button">Retry share sync</button>
  <button class="drive-share-panel__revoke" type="button">Revoke public link</button>
  <div v-if="confirmingRevoke" class="drive-share-panel__confirm">confirmation controls</div>
</Modal>
```

行为：

- `Copy link` 只显示本地复制反馈，不声明后端持久化。
- `Revoke public link` 先打开 `.drive-share-panel__confirm`。
- `Retry share sync` 清除错误并显示重试状态文案。

- [ ] **步骤 2：接入 Drive 页面**

在 `DriveSectionView.vue` 中：

```ts
import DriveSharePanel from './drive/DriveSharePanel.vue'

const sharePanelOpen = ref(false)

function openSharePanel() {
  sharePanelOpen.value = true
}
```

在详情或共享卡片内加入：

```vue
<button class="drive-share-trigger" type="button" @click="openSharePanel">
  {{ tr(lt('分享设置', '分享設定', 'Share settings')) }}
</button>
<DriveSharePanel v-model:show="sharePanelOpen" :item="selectedItem" :shares="driveShares" />
```

- [ ] **步骤 3：运行契约测试**

运行：`timeout 60s pnpm --dir frontend-v2 test -- drive-workspace-contract`

预期：PASS，既有 Drive API 边界仍然存在。

## 任务 3：实现 Docs 分享权限面板

**文件：**
- 创建：`frontend-v2/src/views/app/docs/DocsSharePanel.vue`
- 修改：`frontend-v2/src/views/app/DocsEditorView.vue`

- [ ] **步骤 1：创建 Docs panel 组件**

组件接口：

```ts
defineProps<{
  note: DocsNoteDetail | null
  show: boolean
}>()
```

必须渲染：

```vue
<Modal class="docs-share-panel" :show="show" size="md" title="Share permissions">
  <input class="docs-share-panel__invite-input" v-model="inviteEmail">
  <select class="docs-share-panel__role-select" v-model="inviteRole">...</select>
  <select class="docs-share-panel__link-access" v-model="linkAccess">...</select>
  <div class="docs-share-panel__collaborators">owner/editor/viewer rows</div>
  <p v-if="validationError" class="docs-share-panel__error" role="alert">{{ validationError }}</p>
  <button class="docs-share-panel__retry" type="button" @click="retryInvite">Retry invite</button>
</Modal>
```

行为：

- 空邮箱或无 `@` 时，点击 Send invite 显示校验错误。
- `Retry invite` 保留错误可见，并显示 retry 状态，不能伪造发送成功。
- 链接权限切换只显示 pending state，不声明已保存。

- [ ] **步骤 2：接入 Docs 页面**

在 `DocsEditorView.vue` 中：

```ts
import DocsSharePanel from './docs/DocsSharePanel.vue'

const docsSharePanelOpen = ref(false)
```

将现有 share 按钮改为：

```vue
<button class="docs-share-trigger" type="button" @click="docsSharePanelOpen = true">
  {{ shareLabel }}
</button>
<DocsSharePanel v-model:show="docsSharePanelOpen" :note="note" />
```

- [ ] **步骤 3：运行契约测试**

运行：`timeout 60s pnpm --dir frontend-v2 test -- v21-docs-sheets-labs-collaboration-contract`

预期：PASS，Docs API 边界仍然存在。

## 任务 4：实现 Sheets 保护区域 modal

**文件：**
- 创建：`frontend-v2/src/views/app/sheets/SheetsProtectedRangeModal.vue`
- 修改：`frontend-v2/src/views/app/SheetsEditorView.vue`

- [ ] **步骤 1：创建 Sheets modal 组件**

组件接口：

```ts
defineProps<{
  selectedCellLabel: string
  show: boolean
}>()
```

必须渲染：

```vue
<Modal class="sheets-protected-range-modal" :show="show" size="md" title="Protected range">
  <input class="sheets-protected-range-modal__range-input" v-model="rangeValue">
  <div class="sheets-protected-range-modal__editors">editor whitelist</div>
  <div class="sheets-protected-range-modal__mode">warning-only / block-edit controls</div>
  <p class="sheets-protected-range-modal__conflict" role="alert">overlap conflict</p>
  <p v-if="saveError" class="sheets-protected-range-modal__error" role="alert">{{ saveError }}</p>
  <button class="sheets-protected-range-modal__retry" type="button" @click="retrySave">Retry save</button>
</Modal>
```

行为：

- 默认 range 使用 `C2:D8`，同时展示冲突提示。
- Save 显示显式失败，不声明后端成功。
- Retry 保留错误可见并更新 retry 状态。

- [ ] **步骤 2：接入 Sheets 页面和保护区标记**

在 `SheetsEditorView.vue` 中：

```ts
import SheetsProtectedRangeModal from './sheets/SheetsProtectedRangeModal.vue'

const protectedRangeOpen = ref(false)

function isProtectedRangeCell(rowIndex: number, colIndex: number) {
  return rowIndex >= 1 && rowIndex <= 7 && colIndex >= 2 && colIndex <= 3
}
```

在 action 区加入：

```vue
<button class="sheets-protected-range-trigger" type="button" @click="protectedRangeOpen = true">
  {{ tr(lt('保护区域', '保護範圍', 'Protect range')) }}
</button>
<SheetsProtectedRangeModal v-model:show="protectedRangeOpen" :selected-cell-label="selectedCellLabel" />
```

在 cell class 中加入：

```vue
'sheets-editor__cell--protected': protectedRangeOpen && isProtectedRangeCell(rowIndex, columnIndex)
```

- [ ] **步骤 3：运行契约测试**

运行：`timeout 60s pnpm --dir frontend-v2 test -- v21-docs-sheets-labs-collaboration-contract`

预期：PASS，Sheets API 边界仍然存在。

## 任务 5：更新浏览器 QA 场景并生成报告

**文件：**
- 修改：`frontend-v2/scripts/v21-visual-qa/scenarios.mjs`
- 修改：`frontend-v2/scripts/v21-visual-qa/browser-harness.mjs`
- 修改：`docs/superpowers/progress/v21-browser-visual-qa-report.md`

- [ ] **步骤 1：更新 action registry**

在 `ACTION_EXPRESSIONS` 中加入：

```js
clickDriveSharePanel: clickSelectorExpression('.drive-share-trigger'),
clickDocsSharePanel: clickAndSubmitExpression('.docs-share-trigger', '.docs-share-panel__send'),
clickSheetsProtectedRange: clickAndSubmitExpression('.sheets-protected-range-trigger', '.sheets-protected-range-modal__save')
```

同时新增 `clickAndSubmitExpression(triggerSelector, submitSelector)`，它必须先点击 trigger，再点击 submit；缺失任一 selector 必须返回 `false`。

- [ ] **步骤 2：更新 overlay scenarios**

替换三个场景为：

```js
overlay('drive-share-panel', '云盘', '/drive', 'clickDriveSharePanel', ['.drive-share-panel', '.drive-share-panel__title', '.drive-share-panel__members', '.drive-share-panel__public-link', '.drive-share-panel__revoke', '.drive-share-panel__retry'])
overlay('docs-share-panel', '文档', '/docs/demo-document', 'clickDocsSharePanel', ['.docs-share-panel', '.docs-share-panel__invite-input', '.docs-share-panel__role-select', '.docs-share-panel__link-access', '.docs-share-panel__collaborators', '.docs-share-panel__error', '.docs-share-panel__retry'])
overlay('sheets-protected-range', 'Sheets和labs', '/sheets/demo-sheet', 'clickSheetsProtectedRange', ['.sheets-protected-range-modal', '.sheets-protected-range-modal__range-input', '.sheets-protected-range-modal__editors', '.sheets-protected-range-modal__conflict', '.sheets-protected-range-modal__error', '.sheets-protected-range-modal__retry'])
```

- [ ] **步骤 3：运行 overlay 契约测试**

运行：`timeout 60s pnpm --dir frontend-v2 test -- v21-browser-visual-qa-contract`

预期：PASS。

- [ ] **步骤 4：运行真实浏览器 QA**

运行：`pnpm --dir frontend-v2 visual:qa`

预期：PASS，报告中三个 overlay 的 Required visible selectors 不再是 broad page selectors。

## 任务 6：全量验证并提交

**文件：**
- 修改/创建以上所有实现文件。

- [ ] **步骤 1：运行全量验证**

```bash
timeout 60s pnpm --dir frontend-v2 test
pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
pnpm --dir frontend-v2 visual:qa
```

预期：四条命令退出码均为 0。

- [ ] **步骤 2：检查文件行数和工作树**

```bash
wc -l frontend-v2/src/views/app/drive/DriveSharePanel.vue \
  frontend-v2/src/views/app/docs/DocsSharePanel.vue \
  frontend-v2/src/views/app/sheets/SheetsProtectedRangeModal.vue \
  frontend-v2/src/views/app/DriveSectionView.vue \
  frontend-v2/src/views/app/DocsEditorView.vue \
  frontend-v2/src/views/app/SheetsEditorView.vue \
  frontend-v2/scripts/v21-visual-qa/browser-harness.mjs \
  frontend-v2/scripts/v21-visual-qa/scenarios.mjs \
  frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs
git status --short --branch
```

预期：新组件低于 500 行；脚本和测试低于 500 行；既有超大页面不再因本切片新增大块 overlay 逻辑。

- [ ] **步骤 3：只暂存相关文件并提交**

```bash
git add frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs \
  frontend-v2/src/views/app/drive/DriveSharePanel.vue \
  frontend-v2/src/views/app/docs/DocsSharePanel.vue \
  frontend-v2/src/views/app/sheets/SheetsProtectedRangeModal.vue \
  frontend-v2/src/views/app/DriveSectionView.vue \
  frontend-v2/src/views/app/DocsEditorView.vue \
  frontend-v2/src/views/app/SheetsEditorView.vue \
  frontend-v2/scripts/v21-visual-qa/scenarios.mjs \
  frontend-v2/scripts/v21-visual-qa/browser-harness.mjs \
  docs/superpowers/progress/v21-browser-visual-qa-report.md
git diff --cached --check
git commit -m "feat(frontend-v2): close v2.1 share overlay qa gaps"
```
