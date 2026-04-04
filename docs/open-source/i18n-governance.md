# I18n Governance

## 当前范围
- 支持语言：`en`、`zh-CN`、`zh-TW`
- 当前起步门禁：locale key 一致性 + placeholder 参数一致性检查
- 当前定向 surface 门禁：重点 operator surface 裸字符串扫描
- 当前报告产物：
  - `artifacts/i18n-consistency-report.json`
  - `artifacts/i18n-consistency-report.md`
  - `artifacts/i18n-surface-literal-report.json`
  - `artifacts/i18n-surface-literal-report.md`
  - `artifacts/i18n-page-coverage-report.json`
  - `artifacts/i18n-page-coverage-report.md`

## 默认门禁
- 本地：
  - `pnpm --dir frontend i18n:test`
  - `pnpm --dir frontend i18n:catalog`
  - `pnpm --dir frontend i18n:coverage`
  - `pnpm --dir frontend i18n:check`
- 统一门禁：
  - `bash scripts/validate-local.sh`
  - `.github/workflows/ci.yml` 中的独立 `Frontend i18n gates` step

## 规则
- `en`、`zh-CN`、`zh-TW` 三套 locale catalog 必须保持 key 对齐。
- 新增 locale key 时，必须同步补齐三套翻译。
- 同一 key 在三套语言中的 placeholder 集合也必须保持一致；例如 `{count}` 不能在某个 locale 中漂移为 `{total}`。
- 发现缺失 key 时，默认门禁直接失败，不允许静默 fallback 掩盖问题。
- 发现 placeholder 不一致时，默认门禁同样直接失败，不允许依赖运行时偶然成功。
- 页面覆盖统计会输出 `frontend/pages` 的 `useI18n` 使用率、静态 key 数量与前缀分布，用于收敛后续页面级国际化批次。
- 覆盖统计会把静态 `title-key` 这类翻译绑定也视为已本地化页面，避免把纯路由壳层误判为未接线。
- surface 裸字符串门禁当前覆盖以下高价值入口：
  - `frontend/components/organizations/OrganizationsPolicyPanel.vue`
  - `frontend/components/security/SecurityAliasQuickCreate.vue`
  - `frontend/components/suite/SuiteCommandSearchPanel.vue`
  - `frontend/components/suite/SuiteGovernancePanel.vue`
  - `frontend/components/suite/SuiteReadinessSecurityPanel.vue`
  - `frontend/components/suite/SuiteRemediationPanel.vue`
  - `frontend/composables/useSuiteOperationsWorkspace.ts`
- Batch 11 当前只治理冻结过的 `Suite / Security / Organizations` operator surfaces，不做全仓 AST lint，也不对 Preview/Labs 做全量扫描。
- 当前已收口的页面级批次：
  - `Mail compose`：写信页、附件面板、错误态与成功提示
  - `Mail detail / Conversations`：详情页、会话列表页、会话详情页

## 术语表种子
| English | 简体中文 | 繁體中文 |
| --- | --- | --- |
| Workspace | 工作区 | 工作區 |
| Share | 共享 | 共享 |
| Suggestion | 建议 | 建議 |
| Draft | 草稿 | 草稿 |
| Restore | 恢复 | 恢復 |
| Organization | 组织 | 組織 |
| Read-only | 只读 | 唯讀 |
| Refresh | 刷新 | 重新整理 |
