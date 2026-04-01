# Community Edition v1.1 Release Manager Brief

**版本**: `v1.1-release-manager-brief-final`
**日期**: `2026-04-01`
**作者**: `Codex`

## 当前状态
- 当前状态：`RELEASED`
- 发布分支：`release/v1.1`
- 正式 tag：`v1.1.0`
- 正式发布 commit：`400dc764ca2d9cd57e179a0a4e0fe13bfcb120cb`
- GitHub Release：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.1.0`
- 正式发布日期：`2026-04-01 14:29 +0800`
- 发布前来源分支：`dev/community-v1`
- 发布前验证提交：`98b0fa844ef8109bd2a25a8841e8d8cb6efe3fae`
- 当前发布证据：
  - 本地默认门禁：`bash scripts/validate-local.sh` 已通过
  - `dev/community-v1` 远端 CI：run `23834759633` = `success`
  - `release/v1.1` 远端 CI：run `23834771915` = `success`
  - open `release-blocking` / `v1.1` milestone issue：`0`

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
- 在 `release/v1.1` 上只接收 `v1.1.x` 的 `release-blocking` / 安全 / 元数据修正
- release notes / checklist / signoff / support boundary 的事实性回填
- `dev/community-v1` 继续承接后续集成开发

## 当前禁止
- 扩大 `v1.1` 范围
- 新增 `Preview` 模块或提升其成熟度
- 回灌 `v1.1` 改动到 `release/v1.0`
- 改写 `v1.1.0` tag 或 GitHub Release 指向
- 在 `release/v1.1` 上继续开发第二阶段功能

## 发布判断标准
- [x] `release/v1.1` 发布 commit 的 `MMMail CI` 为绿色
- [x] 不存在已知的 `v1.1 release-blocking` 缺陷
- [x] `Docs / Sheets / i18n / Community-Hosted` 四条线的变更、测试、文档已同步
- [x] `community-v1-v1.1-final-signoff.md` 已签收
