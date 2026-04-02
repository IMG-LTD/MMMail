# Community Edition Support Boundaries

**版本**: `v1.2-release`
**日期**: `2026-04-02`
**作者**: `Codex`

## 当前分支状态
- `release/v1.2`：`v1.2.0 RELEASED`
- `release/v1.0`：`v1.0.0 RELEASED`，继续承接 `v1.0.x`
- `dev/community-v1`：`post-v1.2` 主线开发分支
- 含义：`release/v1.2` 承接 `v1.2.x` 的 release-blocking / security / metadata 修复；`dev/community-v1` 不再作为 `v1.2.0` 的发布基线。

## 支持范围
### GA
- 缺陷修复优先级最高
- 纳入 release-blocking 门禁
- 纳入默认文档与自托管支持说明

### Beta
- 可用，但不作为正式发布阻塞
- 允许功能不完整
- 仅提供基础文档，不承诺完整深能力

### Preview
- 默认通过 `Labs` 隔离
- 不提供稳定性承诺
- 不保证兼容性
- 不应阻塞正式发布

## Community 与 Hosted 边界
### Community
- 自托管部署、升级、备份恢复
- `Mail / Calendar / Drive / Admin / Workspace Shell / Settings`
- `Mail E2EE` 当前为 `GA` 邮件主路径上的受限增强：只覆盖 key profile、recipient readiness、`READY` 内部路由正文加密与详情本地解密
- `Docs / Sheets / Billing center` 维持 `Beta`
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
  - 仅暴露 `Preview` 模块，不承载 Community 支持承诺

## 自托管责任边界
### 维护者负责
- 代码、文档、脚本、默认门禁
- `v1.2` 已实现能力的真实边界说明

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
- `Mail E2EE` 附件、草稿、外部收件人、Drive 零知识模型
- `Web Push` 下发、原生客户端、完整 `SMTP / IMAP / Bridge`
