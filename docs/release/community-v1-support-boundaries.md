# Community Edition Support Boundaries

**版本**: `v1.3-mainline`
**日期**: `2026-04-02`
**作者**: `Codex`

## 当前分支状态
- `release/v1.2`：`v1.2.0 RELEASED`
- `release/v1.0`：`v1.0.0 RELEASED`，继续承接 `v1.0.x`
- `dev/community-v1`：`v1.3-mainline` 当前主线开发分支
- 含义：`release/v1.2` 承接 `v1.2.x` 的 release-blocking / security / metadata 修复；`dev/community-v1` 继续承接 `v1.3` 主线能力闭环。

## 支持范围
### GA
- 缺陷修复优先级最高
- 纳入 release-blocking 门禁
- 纳入默认文档与自托管支持说明

### Beta
- 可用，但不作为正式发布阻塞
- 允许功能不完整
- 仅提供基础文档，不承诺完整深能力
- 若模块仍位于 `Labs` 面，则不进入默认导航与首页候选

### Preview
- 默认通过 `Labs` 隔离
- 不提供稳定性承诺
- 不保证兼容性
- 不应阻塞正式发布

## Community 与 Hosted 边界
### Community
- 自托管部署、升级、备份恢复
- `Mail / Calendar / Drive / Admin / Workspace Shell / Settings`
- `Mail E2EE` 当前为 `GA` 邮件主路径上的受限增强：覆盖 key profile、recipient readiness、`READY` 内部路由正文加密、草稿加密、附件加密与详情本地解密
- `Drive E2EE foundation` 当前只覆盖 owner 文件上传 / 版本上传 / 本地预览 / 本地下载解密
- `Web Push` 当前只覆盖 Mail inbox 新邮件的真实浏览器订阅与下发
- `SMTP outbound adapter` 当前只覆盖最小 external outbound，不承诺 inbound / IMAP / Bridge
- `Docs / Sheets / Billing center / Pass` 维持 `Beta`；其中 `Pass` 虽升为 `Beta`，但仍只通过 `Labs` 暴露
- 设置页暴露 `Swagger UI`、`OpenAPI JSON`、install / runbook 快速页，便于自托管采用

### Hosted / Commercial
- 真实支付 / 扣款
- 商业订阅生命周期
- 税费 / 发票下载 / 财务对账
- 商业 SLA

## 前端入口
- `/suite`
  - `Billing center`：可见 Community 范围内的报价、付款方式占位与账单状态
  - `Release boundary map`：统一展示 `GA / Beta / Preview` 模块、Hosted-only 承诺与自托管责任
- `/settings`
  - `PWA readiness`
  - `Mail E2EE foundation`
  - `Adoption readiness`
- `/labs`
  - 暴露 `Labs-only` 模块，不等同于默认支持承诺
  - 当前包含 `Pass (Beta, Labs only)` 与其余 `Preview` 模块

## Pass 当前边界
- 已验证范围：个人 item 主链、mailbox、alias、shared vault、incoming share、secure link public read、monitor 主路径
- 暂不承诺：浏览器扩展、自动填充、真实 `WebAuthn / passkey ceremony`、暗网真实数据源
- 成熟度口径：`BETA`
- 暴露口径：`Labs only`

## 自托管责任边界
### 维护者负责
- 代码、文档、脚本、默认门禁
- `v1.3-mainline` 已实现能力的真实边界说明

### 部署者负责
- 真实 secrets 管理
- TLS / 反向代理
- 数据库 / Redis / Nacos / Docker 运行环境
- 备份保留策略
- 远端 CI secrets 与 Docker-capable runner 能力

## 不支持项
- `Preview` 模块生产承诺
- 多节点高可用拓扑保证
- Hosted 专属商业能力
- `Mail E2EE` 外部加密投递与零知识模型
- `Drive` public share / collaborator decrypt
- 原生客户端、完整 `SMTP / IMAP / Bridge`
- `Pass` 浏览器扩展 / 自动填充 / 真实 passkey ceremony / 暗网真实数据源
