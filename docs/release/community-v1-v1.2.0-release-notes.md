# Community Edition v1.2.0 Release Notes

**版本**: `v1.2.0-release-notes`
**日期**: `2026-04-02`
**作者**: `Codex`

## 当前状态
- 当前文档为 `v1.2.0` 正式 release notes。
- 发布分支：`release/v1.2`
- 发布前来源分支：`dev/community-v1`
- 正式发布日期：`2026-04-02 14:38 +0800`
- 正式 tag / commit：`v1.2.0` / `38e548a1baa56f70f29841d094fa8f927367b1d9`
- GitHub Release：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.2.0`

## Summary
- `MMMail Community Edition v1.2.0` 在 `v1.1.0` 正式版基础上，补齐了 `PWA baseline`、`Mail E2EE foundation + recipient discovery + message encryption`、`capability honesty`、`architecture discovery` 与 `adoption readiness` 五条主线。
- 本次版本不扩产品广度，不把 `SMTP / IMAP / Bridge / Zero-knowledge / Web Push / Native clients` 写成已上线能力。
- `v1.2.0` 的重点是把“隐私优先”推进到真实可验证的邮件加密主路径，同时保持对未交付范围的明示约束。

## Since `v1.1.0`
- `v1.1.0..v1.2.0` 主要包含以下发布链路提交：
  - `98b0fa8` `ci: run workflow on release branches`
  - `30041b2` `docs(release): finalize v1.1 release records`
  - `ffd42c2` `merge(release): integrate v1.1 into dev/community-v1`
  - `38e548a` `feat: complete v1.2 mainline delivery`

## Included
- `PWA baseline`
  - `manifest.webmanifest`
  - `Service Worker` 注册
  - 安装入口与离线壳层入口
  - 设置页 `PWA readiness`
- `Mail E2EE`
  - key profile foundation
  - recipient readiness
  - `READY` 内部路由正文 OpenPGP 加密发送
  - 邮件详情页本地解密
- `Capability honesty`
  - README、release 文档、前端 `Release boundary map` 与 capability boundary 对齐
- `Architecture discovery`
  - `Zero-knowledge roadmap`
  - `SMTP / IMAP / Bridge discovery`
- `Adoption readiness`
  - adoption panel
  - `Swagger UI` / `OpenAPI JSON`
  - 自托管 install / runbook 快速页
- `Release blockers fixed`
  - Sheets reviewer regression：`DECLINED collaboratorCount` 与 refresh fallback double confirm 已修复
  - 前端高危依赖审计阻塞已解除

## Validation Evidence
- 本地默认门禁：`bash scripts/validate-local.sh`
- release 分支：`https://github.com/IMG-LTD/MMMail/tree/release/v1.2`
- release 分支 CI：`https://github.com/IMG-LTD/MMMail/actions?query=branch%3Arelease%2Fv1.2`
- GitHub Release：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.2.0`

## Support Boundaries
- `GA`
  - `Auth / Session / MFA`
  - `Mail`
  - `Calendar`
  - `Drive`
  - `Suite Shell / Settings / Security / System Health`
  - `Business / Admin`
- `BETA`
  - `Docs`
  - `Sheets`
  - `Billing center`（仅 `Suite` 子入口的 Community 状态展示）
- `PREVIEW`
  - `Pass`
  - `Authenticator`
  - `SimpleLogin`
  - `Standard Notes`
  - `VPN`
  - `Meet`
  - `Wallet`
  - `Lumo`

## Deferred / Out of Scope
- 附件加密、草稿加密、外部收件人完整 E2EE
- `Drive` 客户端加密上传
- 真正零知识邮件架构
- 完整 `SMTP / IMAP / Bridge`
- `Web Push` 下发与原生客户端
- 支付闭环、企业目录、SSO / SCIM / LDAP
