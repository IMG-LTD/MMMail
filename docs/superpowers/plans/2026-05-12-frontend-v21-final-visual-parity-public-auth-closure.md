# Frontend v2.1 Final Visual Parity and Public/Auth Closure 实现计划
> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 收口 v2.1 前端最终视觉一致性与 Public/Auth/System 第一印象页面，拆分 `LoginView.vue` 并建立设计图差异风险记录。

**架构：** 保留现有 public blank layout、路由、API 和 visual QA runner；把登录页拆为 public/auth 局部组件与 CSS，让 route view 只做编排。visual QA 报告继续由 `report.mjs` 生成，并新增设计图差异风险登记入口，避免把截图存在误当成设计通过。

**技术栈：** Vue 3 `<script setup>`、TypeScript、Vite、Vue Router、Node test runner、Chrome DevTools Protocol visual QA runner、Markdown progress artifacts。

---

## 文件结构

创建：

- `frontend-v2/tests/v21-final-visual-parity-public-auth-closure-contract.test.mjs`：源码合同测试，锁定登录页拆分、文件行数、风险登记和报告链接。
- `frontend-v2/src/views/public/auth/LoginBrandPanel.vue`：登录页左侧 MMMail 品牌、价值点和插画。
- `frontend-v2/src/views/public/auth/LoginFormPanel.vue`：登录表单、SSO、MFA、安全/帮助链接。
- `frontend-v2/src/views/public/auth/LoginLegalBar.vue`：隐私、边界、状态和语言支持链接。
- `frontend-v2/src/views/public/auth/login-view-helpers.ts`：登录页不可变文本/价值点模型。
- `frontend-v2/src/views/public/auth/login-view.css`：登录页样式。
- `docs/superpowers/progress/v21-visual-parity-risk-register.md`：设计图差异风险登记表。

修改：

- `frontend-v2/src/views/public/LoginView.vue`：降为 route-level 编排文件。
- `frontend-v2/scripts/v21-visual-qa/report.mjs`：报告追加视觉一致性风险登记入口。
- `docs/superpowers/progress/v21-implementation-progress.md`：实现完成后更新提交、验证、截图数和剩余风险。
- `docs/superpowers/progress/v21-browser-visual-qa-report.md`：由 `pnpm --dir frontend-v2 visual:qa` 刷新。

---

### 任务 1：增加最终收口合同测试

**文件：**

- 创建：`frontend-v2/tests/v21-final-visual-parity-public-auth-closure-contract.test.mjs`

- [ ] **步骤 1：编写失败的合同测试**

创建测试文件，锁定登录页拆分、选择器、风险登记和报告链接：

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  loginView: new URL('../src/views/public/LoginView.vue', import.meta.url),
  brandPanel: new URL('../src/views/public/auth/LoginBrandPanel.vue', import.meta.url),
  formPanel: new URL('../src/views/public/auth/LoginFormPanel.vue', import.meta.url),
  legalBar: new URL('../src/views/public/auth/LoginLegalBar.vue', import.meta.url),
  helpers: new URL('../src/views/public/auth/login-view-helpers.ts', import.meta.url),
  css: new URL('../src/views/public/auth/login-view.css', import.meta.url),
  report: new URL('../scripts/v21-visual-qa/report.mjs', import.meta.url),
  riskRegister: new URL('../../docs/superpowers/progress/v21-visual-parity-risk-register.md', import.meta.url),
  progress: new URL('../../docs/superpowers/progress/v21-implementation-progress.md', import.meta.url)
}

test('public auth login view is split into focused files under the size limit', async () => {
  const [view, brandPanel, formPanel, legalBar, helpers, css] = await Promise.all([
    readFile(files.loginView, 'utf8'),
    readFile(files.brandPanel, 'utf8'),
    readFile(files.formPanel, 'utf8'),
    readFile(files.legalBar, 'utf8'),
    readFile(files.helpers, 'utf8'),
    readFile(files.css, 'utf8')
  ])

  for (const [name, source] of Object.entries({ LoginView: view, brandPanel, formPanel, legalBar, helpers, css })) {
    assert.ok(source.split('\n').length <= 500, `${name} must stay at or below 500 lines`)
  }

  assert.match(view, /LoginBrandPanel/)
  assert.match(view, /LoginFormPanel/)
  assert.match(view, /LoginLegalBar/)
  assert.match(view, /login-view\.css/)
  assert.match(brandPanel, /login-screen__panel--story/)
  assert.match(brandPanel, /MMMail/)
  assert.match(formPanel, /signin-block/)
  assert.match(formPanel, /Enterprise SSO|企业单点登录|企業單一登入/)
  assert.match(formPanel, /Two-factor authentication|双重验证|雙重驗證/)
  assert.match(legalBar, /\/boundary/)
  assert.match(helpers, /loginValuePoints/)
  assert.match(css, /\.login-screen/)
})

test('visual qa report links to a committed design parity risk register', async () => {
  const [report, riskRegister, progress] = await Promise.all([
    readFile(files.report, 'utf8'),
    readFile(files.riskRegister, 'utf8'),
    readFile(files.progress, 'utf8')
  ])

  assert.match(report, /Visual parity risk register/)
  assert.match(report, /v21-visual-parity-risk-register\.md/)
  assert.match(riskRegister, /\| UI group \| Source design \| QA evidence \| Status \| Notes \| Owner slice \|/)
  for (const group of ['PublicAuthShareSystem', 'Login', 'Register', 'Boundary', 'System']) {
    assert.match(riskRegister, new RegExp(group))
  }
  assert.match(progress, /frontend-v21-final-visual-parity-public-auth-closure/)
})
```

- [ ] **步骤 2：运行测试验证失败**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test -- tests/v21-final-visual-parity-public-auth-closure-contract.test.mjs
```

预期：FAIL，失败原因是 `frontend-v2/src/views/public/auth/LoginBrandPanel.vue` 或 `docs/superpowers/progress/v21-visual-parity-risk-register.md` 不存在。

- [ ] **步骤 3：保留红灯测试，暂不提交**

按照本仓库提交规范，失败测试不单独提交。等任务 2-4 最小实现完成并验证退出码为 0 后统一提交。

---

### 任务 2：拆分 LoginView 为 public/auth 组件

**文件：**

- 修改：`frontend-v2/src/views/public/LoginView.vue`
- 创建：`frontend-v2/src/views/public/auth/LoginBrandPanel.vue`
- 创建：`frontend-v2/src/views/public/auth/LoginFormPanel.vue`
- 创建：`frontend-v2/src/views/public/auth/LoginLegalBar.vue`
- 创建：`frontend-v2/src/views/public/auth/login-view-helpers.ts`
- 创建：`frontend-v2/src/views/public/auth/login-view.css`
- 测试：`frontend-v2/tests/v21-final-visual-parity-public-auth-closure-contract.test.mjs`

- [ ] **步骤 1：创建登录页文本模型**

在 `frontend-v2/src/views/public/auth/login-view-helpers.ts` 中创建不可变价值点模型：

```ts
import { lt } from '@/locales'

export const loginValuePoints = [
  ['⌂', lt('默认私密', '預設私密', 'Private by default')],
  ['⟡', lt('支持自托管', '支援自託管', 'Self-host ready')],
  ['⇄', lt('为团队流转而生', '為團隊流轉而生', 'Built for team flow')]
] as const
```

- [ ] **步骤 2：创建 `LoginBrandPanel.vue`**

移动原登录页 story panel 模板，保留 `.login-screen__panel--story` 和插画 class：

```vue
<script setup lang="ts">
import { lt, useLocaleText } from '@/locales'
import { loginValuePoints } from './login-view-helpers'

const { tr } = useLocaleText()
</script>

<template>
  <div class="login-screen__panel login-screen__panel--story">
    <span class="section-label">MMMail</span>
    <h1>{{ tr(lt('面向专注型企业的安全沟通。', '面向專注型企業的安全溝通。', 'Secure communication for focused enterprise.')) }}</h1>
    <p>{{ tr(lt('一个以瑞士工程方法打造的生产力环境，服务于重视隐私、深度工作与完整数据主权的组织。', '一個以瑞士工程方法打造的生產力環境，服務於重視隱私、深度工作與完整資料主權的組織。', 'A Swiss-engineered productivity environment designed for privacy-first organizations who value calm, deep work and total data sovereignty.')) }}</p>
    <div class="value-grid">
      <article v-for="([icon, point], index) in loginValuePoints" :key="index" class="value-tile">
        <span class="value-tile__icon">{{ icon }}</span>
        <span>{{ tr(point) }}</span>
      </article>
    </div>
    <div class="hero-surface" aria-hidden="true">
      <div class="scene-lamp">
        <span class="scene-lamp__base" />
        <span class="scene-lamp__arm scene-lamp__arm--primary" />
        <span class="scene-lamp__arm scene-lamp__arm--secondary" />
        <span class="scene-lamp__head" />
      </div>
      <div class="scene-desk">
        <span class="scene-desk__top" />
        <span class="scene-desk__leg scene-desk__leg--left" />
        <span class="scene-desk__leg scene-desk__leg--right" />
      </div>
      <div class="scene-monitor">
        <span class="scene-monitor__screen" />
        <span class="scene-monitor__stand" />
      </div>
      <div class="scene-chair">
        <span class="scene-chair__back" />
        <span class="scene-chair__seat" />
        <span class="scene-chair__leg" />
      </div>
      <span class="scene-paper scene-paper--a" />
      <span class="scene-paper scene-paper--b" />
    </div>
  </div>
</template>
```

- [ ] **步骤 3：创建 `LoginFormPanel.vue`**

移动登录 form panel，保留 `.signin-block`、SSO 和 MFA 文案，不添加提交成功路径：

```vue
<script setup lang="ts">
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()
</script>

<template>
  <div class="login-screen__panel login-screen__panel--form">
    <div class="login-actions">
      <a href="/boundary">{{ tr(lt('支持', '支援', 'Support')) }}</a>
      <a class="outline-link" href="/register">{{ tr(lt('创建账户', '建立帳戶', 'Create Account')) }}</a>
    </div>
    <div class="signin-block">
      <span class="section-label">{{ tr(lt('登录入口', '登入入口', 'Login portal')) }}</span>
      <h2>{{ tr(lt('登录 MMMail', '登入 MMMail', 'Sign in to MMMail')) }}</h2>
      <label>{{ tr(lt('工作邮箱', '工作郵箱', 'Work email')) }}</label>
      <input class="field" type="email" placeholder="alex.stein@enterprise.ch" />
      <div class="signin-block__row">
        <label>{{ tr(lt('密码', '密碼', 'Password')) }}</label>
        <a href="/boundary">{{ tr(lt('忘记密码？', '忘記密碼？', 'Forgot password?')) }}</a>
      </div>
      <input class="field" type="password" placeholder="••••••••" />
      <button class="primary-action" type="button">{{ tr(lt('继续', '繼續', 'Continue')) }}</button>
      <div class="section-divider">{{ tr(lt('企业单点登录', '企業單一登入', 'Enterprise SSO')) }}</div>
      <button class="ghost-action" type="button">{{ tr(lt('单点登录（OIDC）', '單一登入（OIDC）', 'Single Sign-On (OIDC)')) }}</button>
      <div class="mfa-strip">
        <div>
          <span class="section-label">{{ tr(lt('双重验证', '雙重驗證', 'Two-factor authentication')) }}</span>
          <p>{{ tr(lt('输入你可信设备上的一次性验证码。', '輸入你可信裝置上的一次性驗證碼。', 'Enter one code from your trusted device.')) }}</p>
        </div>
        <strong>{{ tr(lt('步骤 2 / 2', '步驟 2 / 2', 'Step 2 / 2')) }}</strong>
      </div>
      <div class="otp-grid">
        <span v-for="cell in 6" :key="cell">•</span>
      </div>
      <div class="signin-block__row signin-block__row--muted">
        <a href="/boundary">{{ tr(lt('使用其他设备', '使用其他裝置', 'Use another device')) }}</a>
        <a href="/boundary">{{ tr(lt('需要帮助？', '需要協助？', 'Need help?')) }}</a>
      </div>
    </div>
  </div>
</template>
```

- [ ] **步骤 4：创建 `LoginLegalBar.vue`**

移动 legal footer，保留 boundary 链接：

```vue
<script setup lang="ts">
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()
</script>

<template>
  <footer class="login-screen__legal">
    <span>{{ tr(lt('© 2026 MMMAIL AG. 瑞士隐私保障。', '© 2026 MMMAIL AG. 瑞士隱私保障。', '© 2026 MMMAIL AG. Swiss privacy guaranteed.')) }}</span>
    <div>
      <a href="/boundary">{{ tr(lt('隐私', '隱私', 'Privacy')) }}</a>
      <a href="/boundary">{{ tr(lt('边界', '邊界', 'Boundary')) }}</a>
      <a href="/boundary">{{ tr(lt('状态', '狀態', 'Status')) }}</a>
      <span>{{ tr(lt('支持 EN / 简 / 繁', '支援 EN / 簡 / 繁', 'Supports EN / 简 / 繁')) }}</span>
    </div>
  </footer>
</template>
```

- [ ] **步骤 5：抽出 CSS 并改造 route view**

把原 `<style scoped>` 内容移动到 `frontend-v2/src/views/public/auth/login-view.css`。将 `LoginView.vue` 改为：

```vue
<script setup lang="ts">
import LoginBrandPanel from './auth/LoginBrandPanel.vue'
import LoginFormPanel from './auth/LoginFormPanel.vue'
import LoginLegalBar from './auth/LoginLegalBar.vue'
import './auth/login-view.css'
</script>

<template>
  <section class="login-screen">
    <div class="login-screen__body">
      <LoginBrandPanel />
      <LoginFormPanel />
    </div>
    <LoginLegalBar />
  </section>
</template>
```

- [ ] **步骤 6：运行合同测试确认登录拆分通过或进入下一个失败点**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test -- tests/v21-final-visual-parity-public-auth-closure-contract.test.mjs
```

预期：登录拆分相关断言通过；如果仍 FAIL，失败点应指向风险登记或报告链接尚未实现。

---

### 任务 3：增加视觉一致性风险登记和报告入口

**文件：**

- 创建：`docs/superpowers/progress/v21-visual-parity-risk-register.md`
- 修改：`frontend-v2/scripts/v21-visual-qa/report.mjs`
- 测试：`frontend-v2/tests/v21-final-visual-parity-public-auth-closure-contract.test.mjs`

- [ ] **步骤 1：创建风险登记表**

创建 `docs/superpowers/progress/v21-visual-parity-risk-register.md`：

```markdown
# v2.1 Visual Parity Risk Register

Last updated: 2026-05-12

| UI group | Source design | QA evidence | Status | Notes | Owner slice |
| --- | --- | --- | --- | --- | --- |
| PublicAuthShareSystem | `docs/MMMail/UI/首页/工作台-设计概览.png` and public boundary routes | `login`, `register`, `boundary`, `product-access-blocked` | acceptable-delta | Public pages use MMMail blank-layout branding instead of historical design sample shell. | frontend-v21-final-visual-parity-public-auth-closure |
| Login | Public auth route and MMMail branding rules | `.tmp/v21-browser-visual-qa/login-desktop.png` | aligned | Login keeps brand story, form, SSO, MFA, and support links visible without claiming auth success. | frontend-v21-final-visual-parity-public-auth-closure |
| Register | Public auth route and MMMail branding rules | `.tmp/v21-browser-visual-qa/register-desktop.png` | aligned | Register remains a public-shell card with explicit account creation boundary. | frontend-v21-final-visual-parity-public-auth-closure |
| Boundary | `docs/MMMail/UI/Admin/管理后台.png` and boundary matrix rules | `.tmp/v21-browser-visual-qa/boundary-desktop.png` | aligned | Boundary page exposes Premium, Hosted, maturity, and permission language. | frontend-v21-final-visual-parity-public-auth-closure |
| System | System state routes | `offline`, `maintenance`, `not-found`, `server-error` | aligned | System pages preserve clear failure/offline states under blank layout. | frontend-v21-final-visual-parity-public-auth-closure |
| Public shares | Public share routes | `share-mail`, `share-drive`, `share-pass` | acceptable-delta | Public share pages are route-specific rather than a single generic share design, but all expose concrete shared content states. | frontend-v21-final-visual-parity-public-auth-closure |
```

- [ ] **步骤 2：更新 visual QA 报告生成器**

在 `frontend-v2/scripts/v21-visual-qa/report.mjs` 中添加常量和报告区块：

```js
const PARITY_REGISTER_PATH = 'docs/superpowers/progress/v21-visual-parity-risk-register.md'
```

在 report 数组末尾、截图说明之前加入：

```js
'## Visual parity risk register',
'',
`Design-image parity findings are tracked in \`${PARITY_REGISTER_PATH}\`. Screenshot capture proves rendered evidence, not final manual design approval.`,
'',
```

- [ ] **步骤 3：运行合同测试确认通过**

运行：

```bash
timeout 60s pnpm --dir frontend-v2 test -- tests/v21-final-visual-parity-public-auth-closure-contract.test.mjs
```

预期：PASS。

---

### 任务 4：完整验证并刷新报告

**文件：**

- 修改：`docs/superpowers/progress/v21-browser-visual-qa-report.md`

- [ ] **步骤 1：运行完整验证**

按顺序运行：

```bash
timeout 60s pnpm --dir frontend-v2 test
timeout 60s pnpm --dir frontend-v2 typecheck
pnpm --dir frontend-v2 build
pnpm --dir frontend-v2 visual:qa
```

预期：四条命令退出码均为 0。`visual:qa` 输出包含 `v2.1 browser visual QA passed`，报告仍写入 `docs/superpowers/progress/v21-browser-visual-qa-report.md`。

- [ ] **步骤 2：检查行数**

运行：

```bash
wc -l frontend-v2/src/views/public/LoginView.vue frontend-v2/src/views/public/auth/*.vue frontend-v2/src/views/public/auth/*.ts frontend-v2/src/views/public/auth/*.css frontend-v2/scripts/v21-visual-qa/report.mjs
```

预期：每个文件不超过 500 行。

- [ ] **步骤 3：检查工作树**

运行：

```bash
git status --short --branch
git diff --check
git diff --stat
```

预期：只包含本计划相关源码、测试、报告和风险登记文档；`.superpowers/`、`.tmp/`、`docs/MMMail.zip`、`docs/MMMail/`、`frontend/` 仍为无关未跟踪项，不纳入提交。

---

### 任务 5：提交实现

**文件：**

- 所有任务 1-4 相关文件。

- [ ] **步骤 1：暂存相关文件**

运行：

```bash
git add frontend-v2/tests/v21-final-visual-parity-public-auth-closure-contract.test.mjs frontend-v2/src/views/public/LoginView.vue frontend-v2/src/views/public/auth frontend-v2/scripts/v21-visual-qa/report.mjs
git add -f docs/superpowers/progress/v21-visual-parity-risk-register.md docs/superpowers/progress/v21-browser-visual-qa-report.md
```

- [ ] **步骤 2：运行暂存检查**

运行：

```bash
git diff --cached --check
git diff --cached --stat
```

预期：无 whitespace/error 输出，stat 只包含本切片相关文件。

- [ ] **步骤 3：提交实现**

运行：

```bash
git commit -m "feat(frontend-v2): close v2.1 public auth visual parity"
```

- [ ] **步骤 4：记录实现提交哈希**

运行：

```bash
git log --oneline -1
```

预期：输出最新实现提交，例如 `abcdef12 feat(frontend-v2): close v2.1 public auth visual parity`。

---

### 任务 6：更新进度并最终审查

**文件：**

- 修改：`docs/superpowers/progress/v21-implementation-progress.md`

- [ ] **步骤 1：更新进度文档**

在 `docs/superpowers/progress/v21-implementation-progress.md` 中追加或更新以下内容，使用任务 5 的真实提交哈希和任务 4 的真实验证结果：

```markdown
## Latest Completed Slice

- Slice: `frontend-v21-final-visual-parity-public-auth-closure`
- Commit: `<actual-implementation-commit-hash>`
- Verification:
  - `timeout 60s pnpm --dir frontend-v2 test`: PASS
  - `timeout 60s pnpm --dir frontend-v2 typecheck`: PASS
  - `pnpm --dir frontend-v2 build`: PASS
  - `pnpm --dir frontend-v2 visual:qa`: PASS
- Visual QA screenshots: `<count from report>`
- Remaining frontend v2.1 closure risks: `<known risk summary>`
```

- [ ] **步骤 2：提交进度文档**

运行：

```bash
git add -f docs/superpowers/progress/v21-implementation-progress.md
git diff --cached --check
git diff --cached --stat
git commit -m "docs(frontend-v2): update v2.1 implementation progress"
```

- [ ] **步骤 3：最终审查**

运行：

```bash
git status --short --branch
git log --oneline -5
```

最终回复必须包含：

- 新增/修改的核心文件。
- `LoginView.vue` 和新 public/auth 文件行数。
- `test`、`typecheck`、`build`、`visual:qa` 的最新运行结果。
- 实现提交哈希和进度提交哈希。
