# Community Edition v1.2 Planning Baseline

**版本**: `v1.2-planning-final`
**日期**: `2026-04-02`
**作者**: `Codex`

## 背景
- `v1.1.0` 已正式发布后，`dev/community-v1` 转入 `v1.2` 集成开发。
- `v1.2` 的目标不是追平 Proton 全产品矩阵，而是把“隐私优先”的真实基础能力、平台口径和采用门槛补齐到可验证状态。
- 当前版本仍以 `Community Edition`、`Web-first`、`self-hosted collaboration suite` 为基线。

## 版本目标
`v1.2` 聚焦五条主线：
1. `PWA baseline`
2. `Capability honesty & boundary cleanup`
3. `Mail E2EE foundation`
4. `Zero-knowledge / protocol discovery`
5. `Adoption readiness`

## 已交付范围
### Stream A - PWA baseline
- `manifest.webmanifest`
- `Service Worker` 注册与离线壳层入口
- 设置页 `PWA readiness` 面板与安装状态展示

### Stream B - Capability honesty & boundary cleanup
- 收敛 README、release、front-end boundary map 与文档口径
- 明确 `Community / Hosted / Discovery / Limited` 的真实能力边界

### Stream C - Mail E2EE foundation
- 设置页生成 / 更新 `Mail E2EE key profile`
- 服务端保存公钥、加密私钥包、指纹和算法元数据
- 写信页收件人 readiness 查询
- `READY` 内部路由正文浏览器端 OpenPGP 加密发送
- 邮件详情页本地解密正文

### Stream D - Zero-knowledge / protocol discovery
- `Zero-knowledge roadmap` 文档
- `SMTP / IMAP / Bridge` discovery 文档
- 明确后续阶段拆分与非目标

### Stream E - Adoption readiness
- 设置页 `Adoption readiness` 面板
- `Swagger UI` 与 `OpenAPI JSON` 入口
- 自托管 install / runbook 快速页
- 自托管文档刷新到 `v1.2`

## 明确不进入 `v1.2`
- 附件加密、草稿加密、外部收件人公钥发现闭环
- `Drive` 客户端加密上传 / 零知识文件模型
- 真实 `Web Push` 下发与离线写入同步
- 原生 `iOS / Android / Desktop` 客户端
- 完整 `SMTP / IMAP / Bridge` 实现代码
- 支付闭环、SSO、SCIM、LDAP、商业 SLA
- `VPN / Meet / Wallet / Lumo / Pass` 的成熟度提升

## 验证基线
- 本地正式门禁：`bash scripts/validate-local.sh`
- 前端目标验证：`typecheck + vitest`
- 后端目标验证：`MailE2eeFoundationIntegrationTest`、`MailE2eeRecipientDiscoveryIntegrationTest`、`MailE2eeMessageEncryptionIntegrationTest`
- 发布切点：`v1.2.0` / `38e548a1baa56f70f29841d094fa8f927367b1d9`

## 结论
- `v1.2` 已把“隐私优先”从纯文案推进到真实的 `Mail E2EE foundation + message encryption` 主路径。
- `v1.2` 明确区分了 `Implemented / Limited / Discovery / Hosted-only`，避免把未交付能力写成已上线承诺。
- `v1.2` 不等于完整零知识邮件服务，也不等于完整外部协议栈交付。
