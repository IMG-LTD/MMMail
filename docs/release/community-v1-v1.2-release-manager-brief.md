# Community Edition v1.2 Release Manager Brief

**版本**: `v1.2-release-manager-brief-final`
**日期**: `2026-04-02`
**作者**: `Codex`

## 当前状态
- 当前状态：`RELEASED`
- 发布分支：`release/v1.2`
- 正式 tag：`v1.2.0`
- 正式发布 commit：`38e548a1baa56f70f29841d094fa8f927367b1d9`
- GitHub Release：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.2.0`
- 正式发布日期：`2026-04-02 14:38 +0800`
- 发布前来源分支：`dev/community-v1`
- 当前发布证据：
  - 本地默认门禁：`bash scripts/validate-local.sh` 已通过
  - release 分支：`https://github.com/IMG-LTD/MMMail/tree/release/v1.2`
  - release 分支 CI：`https://github.com/IMG-LTD/MMMail/actions?query=branch%3Arelease%2Fv1.2`
  - open `release-blocking` / `v1.2` milestone issue：发布后按 `release/v1.2` 继续分诊

## 版本范围
- `PWA baseline`
  - `manifest`、`Service Worker` 注册、安装入口、离线壳层入口、设置页 PWA readiness
- `Capability honesty / boundary cleanup`
  - README、release docs、front-end boundary map 与 capability boundary 对齐
- `Mail E2EE`
  - key profile foundation
  - recipient readiness
  - `READY` 内部路由正文加密发送
  - 邮件详情页本地解密
- `Architecture discovery`
  - `Zero-knowledge roadmap`
  - `SMTP / IMAP / Bridge discovery`
- `Adoption readiness`
  - adoption panel、OpenAPI / Swagger UI 入口、自托管 install / runbook 快速页

## 入口文档
- `v1.2` 规划基线：`docs/release/community-v1-v1.2-plan.md`
- `v1.2` 主线路线：`docs/release/community-v1-v1.2-mainline-roadmap.md`
- `v1.2` 能力边界：`docs/release/community-v1-v1.2-capability-boundaries.md`
- `v1.2` release checklist：`docs/release/community-v1-v1.2-release-checklist.md`
- `v1.2` final signoff：`docs/release/community-v1-v1.2-final-signoff.md`
- `v1.2.0` release notes：`docs/release/community-v1-v1.2.0-release-notes.md`
- 支持边界：`docs/release/community-v1-support-boundaries.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`

## 当前允许
- 在 `release/v1.2` 上只接收 `v1.2.x` 的 `release-blocking / security / metadata` 修复
- release notes / checklist / signoff / support boundary 的事实性回填
- `dev/community-v1` 继续作为 `post-v1.2` 集成主线

## 当前禁止
- 扩大 `v1.2` 范围
- 在 `release/v1.2` 上继续开发下一阶段功能
- 改写 `v1.2.0` tag 或 GitHub Release 指向
- 把 `Preview` 模块提升为正式发布承诺

## 发布判断标准
- [x] `release/v1.2` 发布切点 `38e548a` 的本地默认门禁为绿色
- [x] `Mail E2EE / PWA / adoption readiness / capability boundaries` 四条线的变更、测试、文档已同步
- [x] `community-v1-v1.2-final-signoff.md` 已签收
