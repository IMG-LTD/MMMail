# Community Edition v1.5 Release Manager Brief

**版本**: `v1.5-release-manager-brief`
**日期**: `2026-04-08`
**作者**: `Codex`

## 当前状态
- 当前状态：`RELEASED_TO_MAIN`
- 开发分支：`dev/v1.5`
- 合并目标：`main`
- 正式 tag：`v1.5.0`
- 当前发布证据：
  - 定向前端验证已通过
  - 定向后端集成验证已通过
  - `v1.5` 计划、路线、release notes、support boundaries、module maturity 已完成
  - `validate-local` 已在正式发布切点前通过

## 版本范围
- `Mail external secure delivery closure`
  - 发件侧浏览器内加密正文与附件
  - 服务端保存密文正文 / 附件与 secure link metadata
  - external secure draft reopen 保留密码提示与过期时间
  - 公开页面 `/share/mail/[token]` 本地密码解密正文并下载附件
- `Boundary alignment`
  - `README`
  - self-hosted install / API / runbook quick pages
  - `Release boundary map`
  - `support boundaries`
  - `module maturity matrix`

## 入口文档
- `v1.5` 规划基线：`docs/release/community-v1-v1.5-plan.md`
- `v1.5` 主线路线：`docs/release/community-v1-v1.5-mainline-roadmap.md`
- `v1.5` release checklist：`docs/release/community-v1-v1.5-release-checklist.md`
- `v1.5` final signoff：`docs/release/community-v1-v1.5-final-signoff.md`
- `v1.5.0` release notes：`docs/release/community-v1-v1.5.0-release-notes.md`
- 支持边界：`docs/release/community-v1-support-boundaries.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`

## 当前允许
- 在 `dev/v1.5` 上只接收 `release-blocking / security / metadata` 收口
- 发布材料已与正式 tag `v1.5.0` 对齐
- 合并回 `main` 后只接收 `v1.5.x` 级别的发布后修复

## 当前禁止
- 扩大 `v1.5` 范围
- 把 `public secure link` 模式写成完整外部 E2EE 邮件协议
- 在发布前继续扩 `Preview` 模块
- 改写 `v1.4.0` 之前的历史 tag 指向

## 发布判断标准
- [x] `Mail` 外部安全投递的附件闭环、草稿恢复与 trust UX 定向验证已通过
- [x] `README / release docs / support boundaries / module maturity / self-hosted pages` 已同步
- [x] `bash scripts/validate-local.sh` 已通过
- [x] `community-v1-v1.5-final-signoff.md` 已签收
