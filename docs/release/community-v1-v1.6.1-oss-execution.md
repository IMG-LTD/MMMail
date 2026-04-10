# Community Edition v1.6.1 OSS Execution Batch

**版本**: `v1.6.1-oss-execution`
**日期**: `2026-04-10`
**作者**: `Codex`

## 背景
- `v1.6.1` 已经把 `Mail → Calendar → Drive → Pass` 主线协作、采用入口和治理口径推进到一个更完整的发布状态。
- 当前仍然存在的开源摩擦，不在“再多交几个模块”，而在“默认入口和部署叙事是否足够诚实、足够可采用”。
- 本批次因此不再横向扩产品面，而是执行一个更适合开源项目的收口包：把默认导航成熟度、开发者站架构说明和运行模型文档口径统一起来。

## 本批目标
### `P0`
- 保持 `Mail → Calendar → Drive → Pass` 主线协作为当前产品优先级，不被新的 `Preview` 扩张打断。
- 默认导航中已经可见的 `Docs / Sheets / Pass` 必须继续明确保持 `Beta` 身份。

### `P1`
- 不额外承诺新的业务深能力，而是把 `Pass` 作为可见 `Beta` 交接面继续说清楚。

### `P2`
- 为自托管部署者和贡献者补一条浏览器内架构说明路径。
- 把 `README`、安装文档、部署拓扑和支持边界统一到当前真实运行模型。

## 本批交付
- 默认导航中的 `Docs / Sheets / Pass` 显式呈现 `Beta` 提示。
- `Developer Station` 新增架构说明入口，形成 `adoption / team / identity / architecture / API` 的浏览器链路。
- 新增浏览器内 `self-hosted architecture` 快速页。
- `README`、`docs/ops/install.md`、`docs/architecture/deployment-topology.md`、`docs/release/community-v1-support-boundaries.md`、`docs/release/community-v1-roadmap.md` 补齐当前运行模型口径。

## 当前运行模型口径
- 当前 Community 自托管基线是：
  - `Nuxt 3` Web 前端
  - 单个 `Spring Boot` 后端进程
  - `MySQL` + `Redis`
- `Nacos` 当前只存在于标准模式里，作为本地注册/配置依赖占位。
- `MMMAIL_NACOS_ENABLED=false` 时，最小模式允许以更轻量的本地依赖启动。
- 当前仓库并未交付真正的 `Spring Cloud` 微服务网格、消息总线编排或服务发现拓扑。

## 非目标
- 不把本批写成“已完成全部 `P0-P3` 路线”。
- 不新增 `SSO / SCIM / LDAP` 自动化。
- 不新增 `SMTP inbound / IMAP / Bridge` 或零知识邮件架构落地。
- 不让 `VPN / Meet / Wallet / Lumo / Notifications / Command Center` 重新进入默认承诺。

## 准出条件
- 默认导航的 `Beta` 入口不再需要用户自行猜测。
- 开发者站可以把部署者直接带到架构说明页，而不是只给 API 链接。
- 顶层文档不再把当前仓库说成已交付的微服务部署模型。
- 定向前端测试与 `typecheck` 通过。

## 版本判断
- 这不是 `v1.6.1` 的新功能扩张批次，而是一个面向开源采用与贡献者认知成本的执行收口批次。
- 成功标准是“真实实现更容易被理解和采用”，而不是“默认菜单里再多几个入口”。
