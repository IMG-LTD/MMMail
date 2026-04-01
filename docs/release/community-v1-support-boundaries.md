# Community Edition v1.0 Support Boundaries

**版本**: `v1.0-support-boundaries`
**日期**: `2026-03-31`
**作者**: `Codex`

## 当前发布状态
- 当前正式状态：`GA_RELEASED`
- 含义：Community 首发正式版 `v1.0.0` 已发布；`release/v1.0` 当前作为 `v1.0.x` 维护线

## 支持范围
### GA
- 缺陷修复优先级最高
- 纳入 release-blocking 门禁
- 纳入首发文档与自托管支持说明

### Beta
- 可用，但不作为首发阻塞
- 允许功能不完整
- 仅提供基础文档，不承诺完整深能力

### Preview
- 默认通过 `Labs` 隔离
- 不提供稳定性承诺
- 不保证兼容性
- 不应阻塞首发发布

## Community 与 Hosted 边界
- Community：
  - 自托管
  - 单仓库部署、升级、备份恢复
  - 不承诺真实 Billing 闭环
- Hosted / Commercial（当前不在首发范围）：
  - 真实支付
  - 订阅生命周期
  - 税费 / 发票 / 对账
  - 商业 SLA

## 自托管责任边界
- 维护者负责：
  - 代码、文档、脚本、默认门禁
- 部署者负责：
  - 真实 secrets 管理
  - TLS / 反向代理
  - 数据库 / Redis / Nacos / Docker 运行环境
  - 备份保留策略
  - 远端 CI secrets 与 runner 能力

## 不支持项
- Preview 模块生产承诺
- 多节点高可用拓扑保证
- Hosted 专属商业能力
- 超出 `Mail / Calendar / Drive / Admin / Workspace Shell` 的首发功能承诺
