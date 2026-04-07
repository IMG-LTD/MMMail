# Community Edition v1.4 Release Manager Brief

**版本**: `v1.4-release-manager-brief-draft`
**日期**: `2026-04-08`
**作者**: `Codex`

## 当前状态
- 当前状态：`READY_FOR_RELEASE`
- 开发分支：`dev/v1.4`
- 合并目标：`main`
- 计划正式 tag：`v1.4.0`
- 当前发布证据：
  - 定向前端验证已通过
  - 定向后端集成验证已通过
  - `v1.4` 计划、路线、release notes、support boundaries、module maturity 已完成
  - `validate-local` 待在最终发布切点执行

## 版本范围
- `Mail external password-protected encrypted delivery`
  - 浏览器内加密正文
  - 服务端保存密文正文与 secure link metadata
  - `SMTP outbound` 外发 secure link 通知
  - 公开页面 `/share/mail/[token]` 本地密码解密
- `Boundary alignment`
  - `README`
  - `ops install / runbook`
  - `Release boundary map`
  - `support boundaries`
  - `module maturity matrix`

## 入口文档
- `v1.4` 规划基线：`docs/release/community-v1-v1.4-plan.md`
- `v1.4` 主线路线：`docs/release/community-v1-v1.4-mainline-roadmap.md`
- `v1.4` release checklist：`docs/release/community-v1-v1.4-release-checklist.md`
- `v1.4` final signoff：`docs/release/community-v1-v1.4-final-signoff.md`
- `v1.4.0` release notes：`docs/release/community-v1-v1.4.0-release-notes.md`
- 支持边界：`docs/release/community-v1-support-boundaries.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`

## 当前允许
- 在 `dev/v1.4` 上只接收 `release-blocking / security / metadata` 收口
- 在正式 tag 前回填 release checklist / signoff / manager brief
- 合并回 `main` 后只接收 `v1.4.x` 级别的发布后修复

## 当前禁止
- 扩大 `v1.4` 范围
- 把 `body-only secure delivery` 写成完整外部 E2EE 邮件协议
- 在发布前继续扩 `Preview` 模块
- 改写 `v1.3.1` 之前的历史 tag 指向

## 发布判断标准
- [x] `Mail` 外部密码保护加密投递的定向前后端验证已通过
- [x] `README / release docs / support boundaries / module maturity / self-hosted pages` 已同步
- [ ] `bash scripts/validate-local.sh` 已通过
- [ ] `community-v1-v1.4-final-signoff.md` 已签收
