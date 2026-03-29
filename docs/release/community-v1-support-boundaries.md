# Community Edition v1.0 Support Boundaries

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前分支状态
- `release/v1.0`：`RC1_READY`
- `dev/community-v1`：`v1.1` 集成中
- 含义：`v1.0` 发布线继续承接正式发版与 backport；当前开发主线已转入 `v1.1`

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
  - `Billing center` 仅承载报价、草稿、账单状态展示，不承诺真实 Billing / 支付闭环
- Hosted / Commercial（当前不在首发范围）：
  - 真实支付 / 扣款
  - 商业订阅生命周期
  - 税费 / 发票下载 / 财务对账
  - 商业 SLA

## 前端入口
- `/suite`
  - `Billing center`：可见 Community 范围内的报价、付款方式占位与账单状态
  - `Release boundary map`：统一展示 `GA / Beta / Preview` 模块、Hosted-only 承诺与自托管责任
- `/labs`
  - 仅暴露 `Preview` 模块，不承载 `Community` 支持承诺

## 自托管责任边界
- 维护者负责：
  - 代码、文档、脚本、默认门禁
- 部署者负责：
  - 真实 secrets 管理
  - TLS / 反向代理
  - 数据库 / Redis / Nacos / Docker 运行环境
  - 备份保留策略
  - 远端 CI secrets 与 Docker-capable runner 能力

## 不支持项
- Preview 模块生产承诺
- 多节点高可用拓扑保证
- Hosted 专属商业能力
- 超出 `Mail / Calendar / Drive / Admin / Workspace Shell` 的首发功能承诺
