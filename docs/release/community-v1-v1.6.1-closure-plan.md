# Community Edition v1.6.1 Closure Plan

**版本**: `v1.6.1-closure-plan`  
**日期**: `2026-04-09`  
**作者**: `Codex`

## 背景
- `v1.6.0` 已完成 `Suite sectioned IA`、`curated Labs catalog`、边界同步与运行时 a11y 门禁。
- `v1.6.1` 不再继续横向扩产品面，而是把已经公开的主线做深、把采用入口做成闭环、把开源治理口径收拢到当前真实版本。
- 当前 Community 的核心定位仍然是：
  - `Web-first`
  - `self-hosted`
  - `privacy-oriented`
  - 围绕 `Mail → Calendar → Drive → Pass` 的团队任务流

## 问题判断
- 当前最大的风险不是“功能太少”，而是“用户能看到很多能力，但很难判断哪些真能长期使用、哪些只是方向验证”。
- 开源项目最大的产品摩擦不在单一页面，而在：
  - 主线任务流深度不足
  - 自托管采用路径仍偏重
  - 版本、CI、Issue 模板、治理文档口径不一致
  - 贡献者面对大页 / 大服务时进入成本偏高

## 目标
### `P0`
- 把 `Mail → Calendar → Drive → Pass` 做成一条真实、可复跑、可被第二位成员消费的团队任务流。
- 在 `Suite / Collaboration / Drive / Pass / Settings` 之间形成一条可观察、有证据、有下一步动作的连续 handoff。

### `P1`
- 加深 `Drive E2EE` 交付表达与恢复语义。
- 保持 `Pass` 作为默认导航中的可见 `Beta` 交接面，而不是重新滑回 `Labs-only` 叙事。

### `P2`
- 把 `SSO / SCIM / LDAP readiness`、开发者文档站和部署后团队启用路径讲清楚。
- 这些能力只作为 readiness / guidance 输出，不包装成已交付企业自动化。

### `P3`
- 非主线 `Preview` 模块继续保持 pluginization / externalization 路线。
- 不让 `VPN / Meet / Wallet / Lumo / Command Center / Notifications` 进入当前默认 rollout 承诺。

## 本批交付
- `Suite` 总览展示主线旅程、handoff run、evidence 与 next-step。
- `Collaboration` 与 `Drive` 复用同一条主线 handoff 视图，显式暴露当前交接状态。
- `Drive` 启动面展示 owner-side E2EE readiness、share readiness 和 `Pass` handoff CTA。
- `Developer station / Team enablement / Adoption guide / API docs` 形成 post-deploy enablement 闭环。
- 自托管入口补齐显式 `minimal self-host` 模式：当 `MMMAIL_NACOS_ENABLED=false` 时，模板、校验脚本和 `docker-compose.minimal.yml` 允许团队先以更轻量的依赖面启动 Community。
- `README`、support boundaries、feedback intake、roadmap、known issues、CI、Issue 模板与 threat model 统一到 `v1.6.1`。

## 非目标
- 不实现真实 `SSO / SCIM / LDAP` 自动化。
- 不实现 `SMTP inbound / IMAP / Bridge`、零知识全栈邮件、原生客户端。
- 不把 `Pass` 伪装成已交付扩展、自动填充或真实 passkey ceremony。
- 不把真实支付、税费、发票闭环混入 Community。
- 不为了增加模块宽度而重新放大 `Preview` 默认承诺。

## 准出条件
- `Suite / Collaboration / Drive` 可以清楚回答三件事：
  - 当前主线做到哪一步
  - 证据是什么
  - 下一步应该去哪
- 团队管理员可以从浏览器内完成：
  - adoption
  - team enablement
  - identity readiness
  - developer/API handoff
- 运维入口可以明确区分：
  - 标准模式（含 Nacos）
  - 最小模式（关闭 Nacos）
- 顶层文档和治理入口不再停留在 `v1.0` 或 `dev/community-v1` 口径。
- 定向前端验证与 `typecheck` 通过。

## 版本判断
- `v1.6.1` 的成功标准不是“再多几个模块入口”，而是“把已经公开的主线能力做得更可信、更连续、更容易被团队采用”。
