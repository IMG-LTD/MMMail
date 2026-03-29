# Community Edition v1.1 Planning Baseline

**版本**: `v1.1-planning-draft`
**日期**: `2026-03-28`
**作者**: `Codex`

## 版本目标
`v1.1` 不扩产品面，集中做四条线：
1. `Docs`
2. `Sheets`
3. 国际化治理
4. Community / Hosted 边界继续收敛

## 工作流入口条件
- `v1.0.0` 已正式发布。
- `dev/community-v1` 最新 head 连续保持绿色。
- `v1.0.0` 的 release-blocking 缺陷已清零。

## 推进顺序
1. `Docs`
2. 国际化治理
3. Community / Hosted 边界
4. `Sheets`

## 分支边界
- `dev/community-v1`：承载 `v1.1`
- `release/v1.0`：承载 `v1.0.0` 与 `v1.0.x`
- 当前 `Docs` 深链与恢复改动（`6b01ae6`）属于 `v1.1`，不回灌 `v1.0.0`

## Stream A - Docs
### 目标
- 将当前 `BETA` 提升到稳定的单人编辑 / 轻协作能力。

### 当前批次
- 已在 `dev/community-v1` 落地首批列表稳定性改进：
  - `keyword` / `scope` 深链恢复
  - 筛选后可见笔记选中恢复
  - 空结果态与筛选态文案收口
  - 前端路由与文案回归测试纳入默认门禁
- 已开始第二批保存稳定性改进：
  - 本地草稿快照与恢复提示
  - 保存失败后的本地恢复路径
  - 协作活动提示文案国际化
- 已落地基础导入导出能力：
  - 本地 `Markdown / TXT` 导入到编辑区
  - 当前文档导出为 `Markdown / TXT`
  - 导入导出工具与文案回归测试纳入默认门禁
- 已补齐未保存保护与展示收口：
  - 切换笔记 / 离开页面前的未保存确认
  - 浏览器刷新/关闭前的 beforeunload 保护
  - 权限、范围、未保存状态与未命名标题去硬编码
- 已纳入 Docs 浏览器等价 smoke：
  - 覆盖草稿恢复、未保存切换确认、搜索/深链恢复、导入导出页面接线
  - `vitest` Docs 回归集 `19/19` 通过，`nuxi typecheck` 通过
  - 运行态回归已进入默认前端门禁
- 已收口活动文档边界：
  - 修复新建文档后 `selectNote()` 被活动文档短路、导致详情/协作数据未加载的问题
  - 导入流程要求存在活动文档且具备编辑权限，不再接受无文档上下文的文件变更事件
  - Docs 回归集提升为 `21/21` 通过，`nuxi typecheck` 通过
- 已补齐失效笔记恢复链：
  - 当 deep link 指向的笔记已删除或撤权、但列表仍残留旧摘要时，前端会清空失效上下文并自动回退到最近可访问笔记
  - 若当前筛选结果里已无可访问笔记，会清理 `noteId` 路由参数并保留删除态，而不是继续卡在失效笔记
  - Docs 回归集提升为 `22/22` 通过，`nuxi typecheck` 通过
- 已补齐 stale note 点击恢复链：
  - 用户点击列表中已经失效的笔记摘要时，前端会提示不可访问并自动回退到最近可访问笔记
  - 点击回退与 deep-link 回退共享同一恢复语义，不再出现“列表点击卡删除态、路由仍指向旧笔记”的分叉行为
  - Docs 回归集提升为 `23/23` 通过，`nuxi typecheck` 通过
- 已接入 Docs 专项门禁：
  - `scripts/validate-local.sh` 现显式执行前端 Docs 回归集与后端 Docs 协作/建议工作流/组织边界回归，不再只依赖全量测试顺带覆盖
  - Docs 后端协作/建议工作流测试已对齐到现有 `test` profile，本机默认门禁不再依赖外部 MySQL / Redis / Nacos 凭据
  - 已在本机执行 `DocsCollaborationIntegrationTest` 与 `DocsSuggestionWorkflowIntegrationTest`，共 `3/3` 通过
  - 已新增 `DocsOrgAccessIntegrationTest`，覆盖 org scope 下 `create / comment / collaboration` 端点在产品禁用与强制 2FA 策略下的 `403 / 30045 / 30046` 回归
  - `DocsOrgAccessIntegrationTest` 已纳入 `BACKEND_DOCS_TESTS`，成为默认本地门禁的一部分
  - 已在本机执行完整 `bash scripts/validate-local.sh`，默认门禁通过且不再受 live-stack 凭据注入影响
  - 已保留 `config/backend.test.env.example` 与 `scripts/validate-backend-test-env.sh` 作为可选 live-stack 配置校验入口
  - `validate` GitHub workflow 可继续直接复用默认测试语义，不需要额外 live-stack 前置条件
  - 这条门禁会随 `validate-all.sh` 与 `validate-ci.sh` 一起执行，成为 `dev/community-v1` 的默认验证链
- 已补齐筛选空结果下的未保存保护：
  - 当用户带着未保存改动应用 `keyword` / `scope` 筛选，且新结果会清空当前文档选择时，前端现在会先弹确认，不再静默重置编辑器
  - 如果用户取消离开，页面会恢复到上一次已应用的筛选与路由状态，避免出现“输入框已改、列表已空、草稿被清掉”的分叉状态
  - Docs 前端专项回归集提升为 `24/24` 通过，`nuxi typecheck` 通过
- 已补齐导入覆盖前的未保存保护：
  - 当当前文档存在未保存本地改动时，导入 `Markdown / TXT` 会先要求确认，不再静默覆盖编辑区内容
  - 用户取消导入时会保留原始编辑内容；确认导入后才会替换编辑区并进入本地待保存状态
- 已补齐手动刷新前的未保存保护：
  - 当当前文档存在未保存本地改动时，手动 `Refresh workspace` 会先要求确认，不再静默用服务端详情覆盖编辑区
  - 用户取消刷新时会保留当前本地编辑内容；确认刷新后才会重新拉取详情、协作和建议数据
- 已补齐协作 / 建议面板的页面级运行态回归：
  - 新增 `frontend/tests/docs-panels.smoke.spec.ts`，直接通过 `DocsPage` 挂载真实 `DocsShareManager` 与 `DocsSuggestionInbox`
  - 覆盖分享创建、权限切换、撤销分享，以及建议创建、接受、拒绝的页面接线与消息反馈
  - 该回归已并入 `FRONTEND_DOCS_TESTS`，作为默认 Docs 前端门禁的一部分
- 已补齐评论面板的页面级运行态回归：
  - 新增 `frontend/tests/docs-comments.smoke.spec.ts`，覆盖引用摘录评论、评论创建与评论解决的页面接线
  - 该回归已并入 `FRONTEND_DOCS_TESTS`，与协作 / 建议面板一起成为默认 Docs 前端门禁的一部分

### 范围
- 文档创建、编辑、保存、列表
- 基础权限与组织边界
- 基础导入导出
- 非实时协作下的稳定性

### 不做
- 实时多人协作
- 评论线程
- 富协同引擎

## Stream B - Sheets
### 目标
- 将当前 `BETA` 提升到稳定的单人编辑 / 轻协作能力。

### 范围
- 表格创建、编辑、保存、分页
- 基础导入导出
- 错误态与恢复
- 组织边界与权限回归

### 不做
- 实时协作
- 复杂公式引擎
- 企业级流程编排

### 进入顺序
- 仅在 `Docs`、国际化治理、Community / Hosted 边界完成第一阶段后再进入

## Stream C - 国际化治理
### 目标
- 建立简体中文 / 繁体中文 / 英语的工程化治理。

### 当前批次
- 已新增 `frontend/utils/i18n-governance.ts`，静态扫描 `frontend/locales/*.ts` 并输出 locale catalog 对齐报告。
- 已新增 `frontend/scripts/i18n-report.mjs`，产出：
  - `artifacts/i18n-consistency-report.json`
  - `artifacts/i18n-consistency-report.md`
- 已新增 `frontend/tests/i18n-governance.spec.ts`，与既有 `frontend/tests/i18n.spec.ts` 一起构成当前 i18n 起步回归。
- 已新增 `frontend/utils/i18n-coverage.ts` 与 `frontend/scripts/i18n-coverage-report.mjs`，输出 `frontend/pages` 的页面级国际化覆盖统计：
  - `artifacts/i18n-page-coverage-report.json`
  - `artifacts/i18n-page-coverage-report.md`
- 已新增 `frontend/tests/i18n-coverage.spec.ts`，保证 `Docs / Sheets / Drive / Calendar / Organizations / Settings` 等关键页面继续保留 `useI18n` 接线与静态 key 覆盖。
- 已补齐 Mail compose 这一条页面级国际化链：
  - `frontend/locales/mail-compose.ts` 收口 compose / attachment 面板的文案、错误态与成功提示
  - `frontend/components/business/MailComposer.vue`、`frontend/components/business/MailAttachmentPanel.vue` 已改为使用 `useI18n`
  - `frontend/pages/compose.vue` 已补页标题与异常路径 fallback 文案
  - `frontend/tests/mail-compose-i18n.spec.ts` 已并入默认 i18n 门禁
- 已补齐 Mail detail / Conversations 这一条页面级国际化链：
  - `frontend/locales/mail-workspace.ts` 收口邮件详情页、会话列表页、会话详情页文案
  - `frontend/pages/mail/[id].vue`、`frontend/pages/conversations/index.vue`、`frontend/pages/conversations/[id].vue` 已改为使用 `useI18n`
  - `frontend/tests/mail-workspace-i18n.spec.ts` 已并入默认 i18n 门禁
  - 页面级国际化覆盖率已提升到 `95.9%`，当前剩余未本地化页面主要为 `contacts.vue`、根路由跳转页与少量纯壳层页面
- 已补齐联系人工作区这一条页面级国际化链：
  - 新增 `frontend/locales/contacts-workspace.ts`，收口联系人、分组、导入导出、重复联系人与确认提示文案
  - `frontend/pages/contacts.vue` 已拆分为薄页入口，真实工作区迁移至 `frontend/components/contacts/ContactsWorkspace.vue`
  - 新增 `frontend/composables/useContactsWorkspace.ts`，承接联系人页逻辑，避免继续在超长页面文件上叠加逻辑
  - `frontend/tests/contacts-i18n.spec.ts` 已并入默认 i18n 门禁，`frontend/tests/i18n-coverage.spec.ts` 已将 `/contacts` 纳入关键页面覆盖断言
- 已将 i18n 专项纳入默认门禁：
  - `pnpm --dir frontend exec vitest run tests/i18n.spec.ts tests/i18n-governance.spec.ts tests/i18n-coverage.spec.ts tests/contacts-i18n.spec.ts tests/mail-compose-i18n.spec.ts tests/mail-workspace-i18n.spec.ts`
  - `node --experimental-strip-types frontend/scripts/i18n-report.mjs`
  - `node --experimental-strip-types frontend/scripts/i18n-coverage-report.mjs`
  - `bash scripts/validate-local.sh`
- 已新增治理文档 `docs/open-source/i18n-governance.md`，明确三语 key 一致性规则、报告产物路径与术语表种子。

### 范围
- 文案抽取
- 缺失检查
- 术语表
- CI 校验
- 页面覆盖率统计

## Stream D - Community / Hosted 边界
### 目标
- 继续把 Community 和 Hosted 的支持边界写死。

### 当前批次
- 已新增 `frontend/utils/community-boundary.ts`，把 Community `GA / Beta / Preview` 模块、Hosted-only 能力与自托管责任整理为前端可消费的数据模型。
- 已新增 `frontend/components/suite/SuiteReleaseBoundaryPanel.vue`，在 `/suite` 中直接展示：
  - Community 默认导航与 Labs 的模块边界
  - Hosted-only 能力清单
  - 自托管责任边界
  - README / Support Boundaries / Roadmap / Module Maturity Matrix 的权威文档入口
- 已新增 `frontend/tests/community-boundary.spec.ts`，并将 `tests/community-navigation.spec.ts + tests/community-boundary.spec.ts` 接入 `scripts/validate-local.sh` 默认门禁。
- 已同步 `README.md` 与 `docs/release/community-v1-support-boundaries.md`，明确：
  - `release/v1.0` 继续承载 `RC1_READY`
  - `dev/community-v1` 已转入 `v1.1`
  - Community 中的 `Billing center` 只承载报价 / 草稿 / 状态展示，不承诺真实支付闭环

### 范围
- `Billing` 边界说明
- Hosted 才承诺的能力清单
- Community 自托管责任边界
- README / Roadmap / Support Boundaries 对齐

## 明确不进入 `v1.1`
- `VPN`
- `Meet`
- `Wallet`
- `Lumo`
- `Pass` 浏览器扩展与 passkeys 完整生命周期
- 真实 Billing 支付链路

## 交付标准
- 每条 stream 都要有独立 release-blocking 集合。
- 继续保持范围收敛，不把 `Preview` 混入 `GA / Beta` 叙事。
- 每条 stream 必须同步文档、测试与门禁，不接受“先做功能后补治理”。

## backlog 入口
- 分诊手册：`docs/release/community-v1-ga-triage-playbook.md`
- backlog seed：`docs/release/community-v1-v1.1-backlog-seed.md`
