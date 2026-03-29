# I18n Governance

## 当前范围
- 支持语言：`en`、`zh-CN`、`zh-TW`
- 当前起步门禁：locale key 一致性检查
- 当前报告产物：
  - `artifacts/i18n-consistency-report.json`
  - `artifacts/i18n-consistency-report.md`

## 默认门禁
- 本地：
  - `pnpm --dir frontend exec vitest run tests/i18n.spec.ts tests/i18n-governance.spec.ts`
  - `node --experimental-strip-types frontend/scripts/i18n-report.mjs`
- 统一门禁：
  - `bash scripts/validate-local.sh`

## 规则
- `en`、`zh-CN`、`zh-TW` 三套 locale catalog 必须保持 key 对齐。
- 新增 locale key 时，必须同步补齐三套翻译。
- 发现缺失 key 时，默认门禁直接失败，不允许静默 fallback 掩盖问题。

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
