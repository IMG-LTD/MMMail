# I18n Governance

**版本**: `v2-mainline`
**日期**: `2026-04-23`

## 当前范围
- 支持语言：`en`、`zh-CN`、`zh-TW`
- 当前治理对象为 `frontend-v2` 的壳层导航、公开页面与工作区路由面
- 当前默认证据来自 `frontend-v2` 的 locale contract 与统一前端测试门禁

## 默认门禁
- 本地：
  - `pnpm --dir frontend-v2 test`
  - `pnpm --dir frontend-v2 exec node --test tests/locale-contract.test.mjs`
- 统一门禁：
  - `bash scripts/validate-local.sh`
- CI：
  - `.github/workflows/ci.yml` 中的 `Frontend-v2 tests` step

## 规则
- `frontend-v2/src/locales/index.ts` 必须维持 `en`、`zh-CN`、`zh-TW` 三套真实映射，且 `zh-TW` 不得回退到 `zh-CN` 文案。
- `frontend-v2/src/store/modules/app.ts` 必须保留 locale 持久化与切换入口。
- 壳层组件必须保持 locale-aware wiring：
  - `frontend-v2/src/layouts/modules/ShellTopBar.vue`
  - `frontend-v2/src/layouts/modules/ShellSideNav.vue`
  - `frontend-v2/src/layouts/modules/MobileTabBar.vue`
  - `frontend-v2/src/layouts/modules/ThemeDrawer.vue`
  - `frontend-v2/src/shared/components/LocaleSwitcher.vue`
- 已纳入主线路由面的页面必须继续使用 locale-aware copy，不允许把英文或中文硬编码回关键页面。
- `frontend-v2/tests/locale-contract.test.mjs` 当前显式覆盖：
  - locale infrastructure 本身
  - 壳层导航的 locale-aware wiring
  - 公开页与工作区路由面的 locale-aware copy
  - 已修复过的回归样式不得重新引入
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
