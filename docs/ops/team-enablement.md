# MMMail Community v1.6.1 Team Enablement

## 定位

本文档描述 Community v1.6.1 在自托管环境下的团队启用路径，目标是把
`Mail → Calendar → Drive → Pass`
串成一条可重复验证的协作闭环。

这不是 Hosted onboarding，也不代表已交付企业身份自动化、原生客户端、Bridge
或零知识全套架构。

## 适用对象

- 平台管理员：负责环境基线、边界说明、试点范围
- 团队负责人：负责 pilot 成员、流程约束、采用验收
- 一线成员：按主线完成真实协作交接

## 启用前提

1. 完成安装与基础运行校验，参考 `docs/ops/install.md`
2. 确认管理员账号已完成 Mail E2EE 基础配置
3. 确认 `Mail`、`Calendar`、`Drive`、`Pass` 在组织内已启用
4. 明确 Community 当前边界，避免把未交付能力写进团队承诺

## 推荐 rollout 顺序

### 1. 管理员基线

- 生成 Mail E2EE key profile 与 recovery package
- 验证 PWA / Web Push 的当前浏览器表现
- 核对边界页，确认 beta/limited/not shipped 能力声明

### 2. 试点团队准备

- 选择 3 到 10 人 pilot 团队
- 约定一条真实协作任务，而不是演示数据
- 为团队准备统一命名、文件夹规则、Pass 项目分类

### 3. 主线协作验证

- `Mail`：发送上下文、收件人、主题
- `Calendar`：建立 review checkpoint 或 handoff meeting
- `Drive`：交付文件、恢复包或共享工件
- `Pass`：交付最终凭证、secure link 或 secret

### 4. 团队验收

- 至少由第二位成员完成消费或确认
- 记录失败点：权限、认知、入口、通知、恢复
- 把失败点回填到 runbook 或培训材料，而不是口头记忆

## 当前优先级

- `P0`：把 `Mail -> Calendar -> Drive -> Pass` 做成一条真实、可复跑、可被第二位成员消费的团队任务流。
- `P1`：继续加深 `Drive E2EE` 交付表达，并保持 `Pass` 作为默认导航中的 `Beta` 主线交接面，而不是长期停留在 `Labs-only` 叙事。
- `P2`：把 `SSO / SCIM / LDAP readiness`、开发者文档站和部署后的团队启用路径讲清楚，但不把它们包装成已交付能力。
- `P3`：非主线 `Preview` 模块继续保持插件化 / 仓库外置化方向，不进入当前团队默认承诺。

## 推荐证据

- 一封主线邮件
- 一个关联日历事件或分享记录
- 一个 Drive 交付动作
- 一个 Pass 安全交接动作
- 一条团队复盘结论

## 当前边界

- Community v1.6.1 没有交付 SSO / SCIM / LDAP 自动化
- 没有交付 SMTP inbound / IMAP / Bridge
- Pass 已进入默认导航，但仍保持 `Beta` 口径
- 非主线 Preview 模块不应进入团队默认承诺

## 浏览器入口

- `frontend/public/self-hosted/adoption.html`：主线定义与采用顺序
- `frontend/public/self-hosted/team.html`：团队 rollout quick page
- `frontend/public/self-hosted/identity.html`：identity readiness 快速说明
- `frontend/public/self-hosted/developer.html`：给实现方的浏览器入口，统一 adoption / team / identity / API 文档站
- `frontend/public/self-hosted/api.html`：后端契约、Swagger UI 与 OpenAPI 入口

## 对外表述建议

建议使用以下口径：

> 我们当前先上线 MMMail 的主线协作闭环：Mail、Calendar、Drive、Pass。
> 企业身份自动化和非主线预览能力仍在 readiness / planning 阶段，不在当前上线承诺内。
