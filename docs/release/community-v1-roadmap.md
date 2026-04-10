# Community Edition v1.6.1 Roadmap

**版本**: `v1.6.1-roadmap`
**日期**: `2026-04-09`
**作者**: `Codex`

## 当前阶段
- 当前公开基线：`main` → `v1.6.1`
- 当前主线定位：`mainline collaboration depth + adoption guidance + governance alignment`
- `dev/v1.6` 保留为 `v1.6.0` 历史 release source branch；`dev/v1.6.1-mainline-depth` 为本批收口分支历史上下文

## 已在 `v1.6.1` 收口的事项
- `Mail → Calendar → Drive → Pass` 主线协作链路已从单纯文案推进到可见 handoff / evidence / next-step 的产品面。
- `Pass` 已保持默认导航可见 `Beta` 入口，不再作为长期 `Labs-only` 叙事。
- 浏览器内 `Developer station / Team enablement / Adoption guide / API docs` 已形成 post-deploy enablement 入口。
- `README`、support boundaries、feedback intake、issue templates、CI 触发器与 threat model 已切到 `v1.6.1` 口径。
- `v1.6.x` 当前 adoption friction 收口已补一条显式 `minimal self-host` 路径：`MMMAIL_NACOS_ENABLED=false` 时可使用 `docker-compose.minimal.yml` 与运行时脚本完成更轻量的本地部署。
- `v1.6.1` OSS 执行批次继续把默认导航 `Beta` 可见性、开发者站架构说明和运行模型文档对齐到当前真实实现，不再暗示已交付微服务部署面。

## 当前优先级
### `P0`
- 把 `Mail → Calendar → Drive → Pass` 做成一条真实、可复跑、可被第二位成员消费的团队任务流。
- 优先修复主线中的权限、认知、入口、通知、恢复断点，而不是继续扩大产品面。

### `P1`
- 继续加深 `Drive E2EE` 交付表达，降低 secure delivery / readable-share 的理解和恢复成本。
- 把 `Pass` 从“可见 Beta”继续推进到“可信交接面”，但不夸大为已交付扩展、自动填充或 passkey ceremony。

### `P2`
- 把 `SSO / SCIM / LDAP readiness`、开发者文档站、部署后团队启用路径继续明确化和任务化。
- 保持这些能力作为 readiness / guidance，而不是包装成已交付企业自动化。

### `P3`
- 非主线 `Preview` 模块继续走 pluginization / externalization 路线。
- 不让 `VPN / Meet / Wallet / Lumo / Command Center / Notifications` 重新进入默认 rollout 承诺。

## 当前不推进的方向
- 不在 `v1.6.x` 内交付真实 `SSO / SCIM / LDAP` 自动化。
- 不在 `v1.6.x` 内交付 `SMTP inbound / IMAP / Bridge`、零知识全栈邮件、原生客户端。
- 不把真实支付、税费、发票下载和订阅结算混入 Community 承诺。
- 不为了对齐更宽的产品矩阵而牺牲主线深度和边界诚实度。
