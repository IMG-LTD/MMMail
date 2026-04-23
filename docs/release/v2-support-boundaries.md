# MMMail v2 Support Boundaries

**版本**: `v2-mainline`
**日期**: `2026-04-23`

## 支持范围
### GA
- `Auth / Session / MFA`
- `Mail / Calendar / Drive / Suite Shell`
- `Business / Organizations`
- `Settings / Security / System Health`
- 默认自托管部署、升级、备份恢复与数据库迁移链路
- `Mail / Drive / Pass` 已发布的 public-share 访问路径
- 本地与 CI 默认门禁覆盖的后端 capability、租户范围、公开分享与迁移回归

### Beta
- `Pass`
- `Docs`
- `Sheets`
- Beta 模块可见、可用，但不自动成为 release-blocking 的正式承诺面

### Preview
- `Labs`
- `Collaboration`
- `Command Center`
- `Notifications`
- `Authenticator / SimpleLogin / Standard Notes / VPN / Meet / Wallet / Lumo`
- Preview 面只保留方向验证与壳层体验，不承诺稳定性或兼容性

## 当前自托管运行模型
- `frontend-v2 Web + 单个 Spring Boot 后端进程 + MySQL / Redis`
- 标准模式可额外启用 `Nacos`，但这不代表仓库交付了微服务网格
- Compose 运行入口默认使用 `http://127.0.0.1:3001`
- 本地前端开发入口默认使用 `http://127.0.0.1:5174`

## Community 与 Hosted 边界
### Community
- 当前仓库中的自托管运行、升级、备份恢复、验证脚本与文档
- `Mail → Calendar → Drive → Pass` 主线协作链路
- 组织、治理、安全、设置与公开边界页面

### Hosted / Commercial
- 真实支付、税费、发票下载、商业对账与 SLA
- 多租商业结算闭环
- 仓库之外的托管基础设施承诺

## 当前不承诺
- `SMTP inbound / IMAP / Bridge`
- 原生客户端
- 完整零知识邮件或零知识云盘架构
- `Pass` 浏览器扩展、自动填充、真实 passkey ceremony
- Preview 模块的深引擎交付
- 多节点高可用与对象存储编排默认方案

## 自托管责任边界
### 维护者负责
- 当前仓库中的代码、脚本、默认门禁与边界说明
- 示例配置文件中的占位值与安全基线

### 部署者负责
- 真实 secrets 管理、TLS、反向代理与域名
- MySQL / Redis / Nacos 的实际运行环境
- 备份保留策略与恢复演练
- Docker-capable CI runner 与 GitHub secrets

## 参考
- `README.md`
- `docs/open-source/module-maturity-matrix.md`
- `docs/ops/install.md`
- `docs/ops/runbook.md`
- `docs/release/v2.0.3-release-notes.md`
