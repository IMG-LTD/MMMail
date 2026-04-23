# MMMail Community Edition

> Current shipped release: `v2.0.0` on `main` (tag `v2.0.0`).

MMMail Community Edition 是一个隐私导向、可自托管、以 Web 为中心的协作套件。
MMMail Community Edition is a privacy-first, self-host-friendly collaboration suite built around a real web runtime.

## 项目简介 | Overview
- 面向团队的自托管协作基线，当前主线覆盖 `Mail / Calendar / Drive / Admin / Workspace Shell / Settings`
- A real self-hosted baseline across `Mail / Calendar / Drive / Admin / Workspace Shell / Settings`
- `Pass` 当前作为默认导航可见的 `Beta` 入口，不等同于 `GA`
- `Labs` 继续承载 `Preview` 模块，不把未交付能力包装成正式承诺

## 当前已交付 | What ships today
- `Mail → Calendar → Drive → Pass` 主线协作链路
- `Suite` 分区化工作台与边界说明
- `PWA`、`Web Push`、`SMTP outbound adapter`
- `Mail E2EE` 当前主路径闭环
- `Drive E2EE foundation`
- 最小自托管运行模型：`Nuxt Web + 单个 Spring Boot 后端进程 + MySQL / Redis`

## 当前不承诺 | What it is not
- 不承诺完整 `SMTP inbound / IMAP / Bridge`
- Not a shipped microservice mesh, even when standard mode enables `Nacos`
- 不承诺完整 MIME 级外部 E2EE 互通或零知识邮件架构
- 不承诺原生客户端、完整商业计费闭环、所有可见模块同等成熟

## 最小自托管启动 | Minimal self-host quick start
1. 复制环境模板 | Copy the env template
   - `cp .env.example .env`
2. 首次采用建议最小模式 | Start with minimal mode first
   - `MMMAIL_NACOS_ENABLED=false`
3. 至少替换这些占位值 | Replace these placeholders at minimum
   - `MMMAIL_JWT_SECRET`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_REDIS_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
4. 校验环境 | Validate the environment
   - `./scripts/validate-runtime-env.sh .env`
5. 启动服务 | Start the stack
   - `docker compose --env-file .env -f docker-compose.minimal.yml up -d --build`
6. 验证服务 | Check the stack
   - Frontend: `http://127.0.0.1:3001`
   - Backend health: `http://127.0.0.1:8080/actuator/health`
   - Public boundary page: `http://127.0.0.1:3001/boundary`

## 文档导航 | Documentation
- 安装说明 | Install: `docs/ops/install.md`
- 升级说明 | Upgrade: `docs/ops/upgrade.md`
- 备份恢复 | Backup and restore: `docs/ops/backup-restore.md`
- 运维 Runbook | Operations runbook: `docs/ops/runbook.md`
- 支持边界 | Support boundaries: `docs/release/community-v1-support-boundaries.md`
- 模块成熟度 | Module maturity: `docs/open-source/module-maturity-matrix.md`
- English overview: `docs/open-source/README.en.md`
- English install quickstart: `docs/ops/install.en.md`

## 验证与贡献 | Validation and contribution
- 默认校验 | Default validation: `bash scripts/validate-local.sh`
- 安全校验 | Security validation: `bash scripts/validate-security.sh`
- 贡献指南 | Contributing: `CONTRIBUTING.md`
- 安全策略 | Security policy: `SECURITY.md`
- Issue / PR templates: `.github/ISSUE_TEMPLATE/`、`.github/pull_request_template.md`

## 当前版本说明 | Release reference
- 当前公开基线 | Current public baseline: `v2.0.0` on `main` (tag `v2.0.0`)
- 当前硬化轨道 | Current hardening track: `docs/release/v2.0.1-release-notes.md`, `docs/release/v2.0.2-release-notes.md`
- 当前已发布说明 | Current shipped release notes: `docs/release/v2.0.0-release-notes.md`
- 正式支持边界以 `docs/release/community-v1-support-boundaries.md` 与 `docs/open-source/module-maturity-matrix.md` 为准

## 许可证 | License
- `LICENSE`
