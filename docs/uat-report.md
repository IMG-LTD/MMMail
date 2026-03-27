# UAT 验收报告（v122）

**版本**: `v122.0`  
**日期**: `2026-03-13`  
**作者**: `Codex`  
**主题**: `Suite billing center + payment methods + invoices + subscription lifecycle summary`  
**Jira**: `KAN-287`

## 变更记录
- `2026-03-13`：新增 `v122.0`，记录 `Suite` 账单中心、付款方式、发票记录、订阅生命周期摘要、三语闭环、Playwright 实机验收与 Jira 收口材料。

## 1. 验收范围
- `Suite` 新增 `Billing center` 工作区：
  - `Payment methods`
  - `Invoices`
  - `Subscription lifecycle`
  - `Subscription actions`
- `APPLY_LATEST_DRAFT / CANCEL_AUTO_RENEW / RESUME_AUTO_RENEW` 生命周期动作
- `en / zh-CN / zh-TW` 三语界面与用户操作闭环
- 前后端自动化验证、Playwright 浏览器验收与截图归档

## 2. 验收环境
- 前端：`http://127.0.0.1:3001`
- 后端：`http://127.0.0.1:8080`
- 浏览器执行器：Playwright MCP `browser_*`
- 任务目录：`.codex-tasks/20260313-proton-parity-v122/`
- 官方 billing 基线缓存：
  - `.codex-tasks/20260313-proton-parity-v122/raw/proton.me-payment-options.html`
  - `.codex-tasks/20260313-proton-parity-v122/raw/proton.me-manage-payment-methods.html`
  - `.codex-tasks/20260313-proton-parity-v122/raw/proton.me-invoices.html`
  - `.codex-tasks/20260313-proton-parity-v122/raw/proton.me-upgrade-downgrade.html`
- `2026-03-13` 复核的官方公开页面：
  - `https://proton.me/support/payment-options`
  - `https://proton.me/support/manage-payment-methods`
  - `https://proton.me/support/invoices`
  - `https://proton.me/support/manage-subscription`
  - `https://proton.me/business/plans`
  - `https://proton.me/meet`
  - `https://proton.me/authenticator`
  - `https://proton.me/wallet`
  - `https://proton.me/drive/sheets`
  - `https://proton.me/support/lumo-getting-started`

## 3. 自动化验证

### 3.1 前端
- 定向测试：✅  
  `cd frontend && pnpm exec vitest run tests/i18n.spec.ts tests/suite-plans.spec.ts tests/suite-billing.spec.ts tests/suite-billing-center.spec.ts`
- 类型检查：✅  
  `cd frontend && pnpm exec tsc --noEmit --pretty false`
- 生产构建：✅  
  `cd frontend && pnpm build`
- 结果说明：
  - `suite.vue` 已接入 `SuiteBillingCenterPanel`
  - `suite.billing.center.*` 三语词条已覆盖
  - `build` 完成；仅保留既有 chunk size warning，无新增阻塞错误

### 3.2 后端
- 定向集成测试：✅  
  `cd backend/mmmail-server && timeout 60s mvn -q test -Dtest=SuiteCatalogServiceTest,SuiteBillingParityIntegrationTest,SuiteBillingCenterIntegrationTest`
- 覆盖结果：
  - `GET /api/v1/suite/billing/center`
  - `POST /api/v1/suite/billing/payment-methods`
  - `POST /api/v1/suite/billing/payment-methods/default`
  - `POST /api/v1/suite/billing/subscription-actions`
  - `payment method / pending invoice / subscription state` 三域聚合

## 4. Playwright UAT 用例

### 4.1 UAT-001：注册新账号并进入 Suite
- 步骤：
  1. 打开 `http://127.0.0.1:3001/register`
  2. 注册新账号
  3. 登录后进入 `Suite`
- 结果：
  - 新账号成功创建
  - `Suite` 页面成功打开
  - 账单中心模块成功渲染
- 结论：`通过`

### 4.2 UAT-002：英文态账单中心完整链路
- 步骤：
  1. 在 `suite` 页面保持 `English`
  2. 点击任意 `Use in checkout`
  3. 点击 `Save checkout draft`
  4. 在 `Billing center` 中新增默认付款方式
  5. 点击 `Queue draft`
  6. 复查待处理发票、下载码、默认付款方式标签
- 结果：
  - 新增付款方式成功
  - `Pending invoices: 1`
  - 发票列表出现 1 条 `PENDING` 记录
  - `Download code` 已展示
  - 默认付款方式已标记
  - 截图已落盘：`docs/assets/i18n-v122-suite-billing-center-en.png`
- 结论：`通过`

### 4.3 UAT-003：简体中文界面验收
- 步骤：
  1. 在 `suite` 页面切换为 `简体中文`
  2. 复查 `账单中心 / 付款方式 / 发票记录 / 订阅生命周期`
- 结果：
  - 中文文案正确切换
  - 账单中心已展示已保存的付款方式与待处理发票
  - 截图已落盘：`docs/assets/i18n-v122-suite-billing-center-zh-cn.png`
- 结论：`通过`

### 4.4 UAT-004：繁體中文界面验收
- 步骤：
  1. 在 `suite` 页面切换为 `繁體中文`
  2. 复查 `帳單中心 / 付款方式 / 發票記錄 / 訂閱生命週期`
- 结果：
  - 繁體中文文案正确切换
  - 账单中心状态与英文态一致
  - 截图已落盘：`docs/assets/i18n-v122-suite-billing-center-zh-tw.png`
- 结论：`通过`

### 4.5 UAT-005：控制台与环境复核
- 控制台结果：✅
  - 当前页 `Errors = 0`
  - 当前页 `Warnings = 0`
  - 仅有 1 条 Vue `Suspense` 实验特性 `info`
- 环境问题：
  - 首次进入 `suite` 时，`/api/v1/suite/billing/center` 返回 `500`
  - 排查确认：本机 `8080` 上运行的是旧后端进程，不是当前迭代代码
  - 处理结果：重启 `8080` 为最新后端进程后，Playwright 验收重新通过
- 结论：
  - 本轮 `Suite billing center` 功能无新的阻塞性前端或后端缺陷
  - 验收中暴露的环境问题已定位并修正

## 5. 关键证据
- 前端：
  - `frontend/pages/suite.vue`
  - `frontend/components/suite/SuiteBillingCenterPanel.vue`
  - `frontend/composables/useSuiteBillingCenterApi.ts`
  - `frontend/composables/useSuiteBillingCenterWorkspace.ts`
  - `frontend/locales/suite-billing-center.ts`
  - `frontend/tests/suite-billing-center.spec.ts`
- 后端：
  - `backend/mmmail-server/src/main/java/com/mmmail/server/controller/SuiteBillingController.java`
  - `backend/mmmail-server/src/main/java/com/mmmail/server/service/SuiteBillingCenterService.java`
  - `backend/mmmail-server/src/main/resources/schema.sql`
  - `backend/mmmail-server/src/test/java/com/mmmail/server/SuiteBillingCenterIntegrationTest.java`
- 截图：
  - `docs/assets/i18n-v122-suite-billing-center-en.png`
  - `docs/assets/i18n-v122-suite-billing-center-zh-cn.png`
  - `docs/assets/i18n-v122-suite-billing-center-zh-tw.png`

## 6. 验收结论
- `v122` 已完成以下收口：
  - `Suite` 账单中心 UI 与后端聚合接口
  - 付款方式新增与默认方式管理
  - 发票记录展示与下载码回显
  - 订阅生命周期摘要与动作入口
  - `en / zh-CN / zh-TW` 三语闭环
  - 前端测试、类型检查、构建
  - 后端定向集成测试
  - Playwright 实机验收与证据归档
- 当前结论：`KAN-287` 满足本轮 UAT 通过条件。
