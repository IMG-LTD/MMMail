# Community Edition v1.1 Release Manager Brief

**版本**: `v1.1-release-branch-draft`
**日期**: `2026-04-01`
**作者**: `Codex`

## 当前状态
- 当前状态：`RELEASE_BRANCH_PREPARED`
- 发布分支：`release/v1.1`
- 候选提交：`a6bdda20cfbf6c2f040a4141c220f5426ae3d7b2`
- 来源分支：`dev/community-v1`
- 当前准入条件：
  - 本地默认门禁：`bash scripts/validate-local.sh` 已通过
  - `dev/community-v1` 远端 CI：run `23834450221`
  - 仅当上述 run 结论为 `success`，才允许继续推送 `release/v1.1`

## 版本范围
- `Docs`
  - 深链恢复、未保存保护、导入导出、轻协作/建议/评论运行态 smoke、组织边界回归
- `Sheets`
  - route/runtime/leave guard/state guard、share/version/incoming/collaboration 运行态、页级 smoke 与 reviewer regression fix
- `i18n`
  - 页面级 `useI18n + useHead` 补齐
  - 页面覆盖率报告提升到 `100%`
- `Community / Hosted`
  - `Billing center` 显式登记为 `Suite` 内 `BETA` 子入口
  - `Release boundary map` 与文档口径对齐

## 入口文档
- `v1.1` 规划基线：`docs/release/community-v1-v1.1-plan.md`
- `v1.1` release checklist：`docs/release/community-v1-v1.1-release-checklist.md`
- `v1.1` final signoff：`docs/release/community-v1-v1.1-final-signoff.md`
- `v1.1.0` release notes：`docs/release/community-v1-v1.1.0-release-notes.md`
- 支持边界：`docs/release/community-v1-support-boundaries.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`

## 当前允许
- 在 `release/v1.1` 上只接收 `release-blocking` 修复
- release notes / checklist / signoff / README 的事实性对齐
- tag / GitHub Release 元数据收口

## 当前禁止
- 扩大 `v1.1` 范围
- 新增 `Preview` 模块或提升其成熟度
- 回灌 `v1.1` 改动到 `release/v1.0`
- 在 `release/v1.1` 上继续开发第二阶段功能

## 发布判断标准
- `release/v1.1` 最新 head 的 `MMMail CI` 为绿色
- 不存在已知的 `v1.1 release-blocking` 缺陷
- `Docs / Sheets / i18n / Community-Hosted` 四条线的变更、测试、文档已同步
- `community-v1-v1.1-final-signoff.md` 可签收
