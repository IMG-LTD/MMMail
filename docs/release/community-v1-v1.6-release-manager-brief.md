# Community Edition v1.6 Release Manager Brief

**版本**: `v1.6-release-manager-brief`
**日期**: `2026-04-08`
**作者**: `Codex`

## 当前状态
- 当前状态：`RELEASED_TO_MAIN_PENDING_REMOTE_PUSH`
- 开发分支：`dev/v1.6`
- 合并目标：`main`
- 正式 tag：`v1.6.0`
- 当前发布证据：
  - 定向前端验证已通过
  - `v1.6` 计划、路线、release notes、support boundaries、module maturity 已完成
  - `README`、ops docs、self-hosted quick pages 已切到 `v1.6` 口径
  - `validate-local` 已在待发布 commit 上通过
  - `dev/v1.6` 已快进合并到 `main`

## 版本范围
- `Product Focus & IA Refinement`
  - `/suite` 改为 `Overview / Plans / Billing / Operations / Boundary` 分区视图
  - `/labs` 默认 curated catalog 收敛为 `Pass / Authenticator / SimpleLogin / Standard Notes`
  - canonical docs、release boundary map、module maturity 与 locale 统一到 `v1.6`
  - 关键公开面新增 runtime a11y 自动化门禁
- `Carried-forward shipped baseline`
  - `PWA`
  - `Mail E2EE` 当前闭环
  - 外部密码保护安全投递的正文 / 附件 / 草稿恢复 / 公开页本地解密下载
  - `Drive E2EE foundation`
  - `Web Push`
  - `SMTP outbound adapter`
  - `Calendar internal invitation orchestration`
  - `Pass Beta readiness`

## 入口文档
- `v1.6` 规划基线：`docs/release/community-v1-v1.6-plan.md`
- `v1.6` 主线路线：`docs/release/community-v1-v1.6-mainline-roadmap.md`
- `v1.6` release checklist：`docs/release/community-v1-v1.6-release-checklist.md`
- `v1.6` final signoff：`docs/release/community-v1-v1.6-final-signoff.md`
- `v1.6.0` release notes：`docs/release/community-v1-v1.6.0-release-notes.md`
- 支持边界：`docs/release/community-v1-support-boundaries.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`

## 当前允许
- 仅承接 `v1.6.x` 级别的 `release-blocking / security / metadata` 修复
- 继续完成 tag 与远端发布回填

## 当前禁止
- 扩大 `v1.6` 范围
- 修改 `/labs` 底层 raw registry / source-of-truth 以迎合当前默认 catalog
- 把 `v1.6` 包装成零知识邮件、完整协议栈或原生客户端发布
- 在发布前继续深化 `VPN / Meet / Wallet / Lumo`

## 发布判断标准
- [x] `Suite` 分区 IA、`Labs` curated catalog、boundary docs 与 runtime a11y 已落地
- [x] `README / release docs / support boundaries / module maturity / ops docs / self-hosted pages` 已同步
- [x] `bash scripts/validate-local.sh` 已通过
- [x] `community-v1-v1.6-final-signoff.md` 已签收
