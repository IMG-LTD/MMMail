# Community Edition Support Boundaries

**版本**: `v1.6.1`
**日期**: `2026-04-15`
**作者**: `Codex`

## 支持范围
### GA
- 缺陷修复优先级最高
- 纳入 release-blocking 门禁
- 纳入默认文档与自托管支持说明

### Beta
- 可用，但不作为正式发布阻塞
- 允许功能不完整
- 仅提供基础文档，不承诺完整深能力
- 若模块仍位于 `Labs` 面，则不进入默认导航与首页候选；若进入默认导航，也不自动提升为 `GA`

### Preview
- 默认通过 `Labs` 隔离
- 不提供稳定性承诺
- 不保证兼容性
- 不应阻塞正式发布

## Community 与 Hosted 边界
### Community
- 自托管部署、升级、备份恢复
- 当前自托管运行模型是 `Nuxt Web + 单个 Spring Boot 后端进程 + MySQL / Redis`；标准模式可带 `Nacos`，但不等于已交付微服务部署形态
- `Mail / Calendar / Drive / Admin / Workspace Shell / Settings`
- `Mail E2EE` 当前为 `GA` 邮件主路径上的受限增强：覆盖 key profile、recipient readiness、`READY` 内部路由正文加密、草稿加密、附件加密、恢复包、详情本地解密，以及外部密码保护安全投递的草稿恢复与附件闭环
- 外部密码保护加密投递当前语义：
  - 浏览器内加密正文与附件
  - 服务端保存密文正文、密文附件与 secure link metadata
  - `saveDraft / reopen draft` 保留 external secure delivery intent、密码提示与过期时间
  - `SMTP outbound` 只外发通知邮件与 secure link，不外发明文正文或明文附件
  - 公开页面 `/share/mail/[token]` 通过密码在浏览器内本地解密正文，并解密下载附件
- `Drive E2EE foundation` 当前覆盖 owner 文件上传 / 版本上传 / 本地预览 / 本地下载解密，以及单文件 `readable-share` E2EE foundation
- `Web Push` 当前只覆盖 Mail inbox 新邮件的真实浏览器订阅与下发
- `SMTP outbound adapter` 当前只覆盖最小 external outbound，不承诺 inbound / IMAP / Bridge
- `Docs / Sheets / Billing center / Pass` 维持 `Beta`；其中 `Pass` 已进入默认导航，作为主线可见 Beta 入口，但仍不进入 home fallback，也不提升为 GA
- `Suite` 按 `Overview / Plans / Billing / Operations / Boundary` 分区收口，避免将计划、账单、运营和边界信息混排在同一长页中
- `Suite Overview` 当前强调 `Mail → Calendar → Drive → Pass` 的主线协作链路，作为团队采用的连续任务流
- `Labs` 默认只展示与主战略相邻的 `Authenticator / SimpleLogin / Standard Notes`；其余 `Preview` 模块保留在底层 registry 中，但不再进入默认 curated catalog
- 设置页暴露 `Swagger UI`、`OpenAPI JSON`、install / runbook / adoption guide 快速页，便于自托管采用

### Hosted / Commercial
- 真实支付 / 扣款
- 商业订阅生命周期
- 税费 / 发票下载 / 财务对账
- 商业 SLA

## 前端入口
- `/suite`
  - `Overview`：展示当前可见产品面和各分区入口
  - `Plans`：集中展示计划目录、配额与能力矩阵
  - `Billing`：可见 Community 范围内的报价、付款方式占位与账单状态
  - `Operations`：集中展示命令搜索、就绪度、安全、治理与整改动作
  - `Boundary`：统一展示 `GA / Beta / Preview` 模块、Hosted-only 承诺与自托管责任
- `/settings`
  - `PWA readiness`
  - `Mail E2EE foundation`
  - `Adoption readiness`
  - `Launch tracks` 与浏览器内 `adoption guide`
- `/labs`
  - 暴露 `Labs-only` 模块，不等同于默认支持承诺
  - 默认 curated catalog 只包含 `Authenticator`、`SimpleLogin` 与 `Standard Notes`
  - 其余 `Preview` 模块保留在底层 registry，但默认不展示在 curated catalog

## Pass 当前边界
- 已验证范围：个人 item 主链、mailbox、alias、shared vault、incoming share、secure link public read、monitor 主路径
- 暂不承诺：浏览器扩展、自动填充、真实 `WebAuthn / passkey ceremony`、暗网真实数据源
- 成熟度口径：`BETA`
- 暴露口径：默认导航可见，但仍是 `Beta`

## 自托管责任边界
### 维护者负责
- 代码、文档、脚本、默认门禁
- 当前公开基线已实现能力的真实边界说明

### 部署者负责
- 真实 secrets 管理
- TLS / 反向代理
- 数据库 / Redis / Docker 运行环境，以及标准模式下可选的 `Nacos`
- 备份保留策略
- 远端 CI secrets 与 Docker-capable runner 能力

## 不支持项
- `Preview` 模块生产承诺
- 多节点高可用拓扑保证
- Hosted 专属商业能力
- `Mail E2EE` 完整 MIME 外部 E2EE、外部联系人公钥发现、零知识模型
- `Drive` collaborator decrypt、folder descendants readable-share decrypt、零知识元数据
- 原生客户端、完整 `SMTP / IMAP / Bridge`
- `Pass` 浏览器扩展 / 自动填充 / 真实 passkey ceremony / 暗网真实数据源
