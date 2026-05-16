# I18n Governance

**版本**: `v2-mainline`
**日期**: `2026-05-16`

## 当前范围
- 支持语言：`en-US`、`zh-CN`、`zh-TW`
- 当前治理对象为 `frontend-admin` 的壳层导航、公开页面、工作区路由面和 v2.1.2 迁移后的产品页面
- 当前默认证据来自 `frontend-admin` 的 i18n key parity contract、v2.1.2 contract tests 与统一前端测试门禁
- legacy `frontend-v2` 只作为冻结历史参考，不再是当前产品 i18n 治理对象

## 默认门禁
- 本地：
  - `pnpm --dir frontend-admin check:i18n`
  - `pnpm --dir frontend-admin test:v212`
- 统一门禁：
  - `bash scripts/validate-local.sh`
- CI：
  - `.github/workflows/ci.yml` 中的 MMMail admin i18n、contract 和 coverage steps

## 规则
- `frontend-admin/src/locales/locale.ts` 必须维持 `en-US`、`zh-CN`、`zh-TW` 三套真实映射，且 `zh-TW` 不得回退到 `zh-CN` 文案。
- `frontend-admin/src/locales/langs/*.ts` 的 key 集合必须保持一致；新增 key 必须同步三语。
- `frontend-admin/src/store/modules/app/index.ts` 相关 locale 状态必须保留持久化与切换入口。
- 壳层组件必须保持 locale-aware wiring：
  - `frontend-admin/src/layouts/modules/global-header/index.vue`
  - `frontend-admin/src/layouts/modules/global-menu/index.vue`
  - `frontend-admin/src/layouts/modules/global-search/index.vue`
  - `frontend-admin/src/layouts/modules/theme-drawer/index.vue`
- 已纳入主线路由面的页面必须继续使用 locale-aware copy，不允许把英文或中文硬编码回关键页面。
- `frontend-admin/scripts/check-i18n-keys.ts` 当前显式覆盖：
  - 三语 key parity
  - 新增 namespace 不漏翻译
  - 多余 key 不悄悄漂移
- `frontend-admin/tests/v212-*-contract.test.mjs` 显式覆盖 v2.1.2 产品页面、公开页与工作区路由面的 locale-aware copy。
- `main` 已不再承载 legacy `frontend/`，因此不再维护基于 `frontend/pages` 的旧门禁与报告口径。

## 术语表种子
| English | 简体中文 | 繁體中文 |
| --- | --- | --- |
| Workspace | 工作区 | 工作區 |
| Share | 共享 | 共享 |
| Draft | 草稿 | 草稿 |
| Restore | 恢复 | 恢復 |
| Organization | 组织 | 組織 |
| Read-only | 只读 | 唯讀 |
| Refresh | 刷新 | 重新整理 |
