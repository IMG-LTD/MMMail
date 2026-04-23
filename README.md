# MMMail Community Edition

> Current shipped release: `v2.0.3` on `main`.

MMMail Community Edition 是一个隐私导向、可自托管、以 Web 为中心的协作套件。
MMMail Community Edition is a privacy-first, self-host-friendly collaboration suite built around a real web runtime.

## 项目简介 | Overview
- 面向团队的自托管协作基线，当前主线覆盖 `Mail / Calendar / Drive / Admin / Workspace Shell / Settings`
- `Pass / Docs / Sheets` 当前作为可见 `Beta` 面保留在主线中
- `Labs` 与 `Collaboration / Command Center / Notifications` 继续作为 `Preview` 面
- 当前仓库主线已切到 `frontend-v2`

## 当前已交付 | What ships today
- `Mail → Calendar → Drive → Pass` 主线协作链路
- `Suite / Business / Organizations / Security / Settings` 治理入口
- `Mail / Drive / Pass` public-share 访问路径
- Flyway 驱动的数据库升级、备份恢复与回滚脚本
- `frontend-v2 Web + 单个 Spring Boot 后端进程 + MySQL / Redis` 的 Compose 自托管路径

## 当前不承诺 | What it is not
- 不承诺完整 `SMTP inbound / IMAP / Bridge`
- Not a shipped microservice mesh, even when standard mode enables `Nacos`
- 不承诺原生客户端、完整商业计费闭环或所有可见模块同等成熟
- 不承诺完整零知识邮件 / 云盘架构

## 最小自托管启动 | Minimal self-host quick start
1. 复制环境模板 | Copy the env template
   - `cp .env.example .env`
2. 至少替换这些占位值 | Replace these placeholders at minimum
   - `MMMAIL_JWT_SECRET`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_REDIS_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
3. 首次采用建议最小模式 | Start with minimal mode first
   - `MMMAIL_NACOS_ENABLED=false`
4. 校验环境 | Validate the environment
   - `./scripts/validate-runtime-env.sh .env`
5. 启动服务 | Start the stack
   - `docker compose --env-file .env -f docker-compose.minimal.yml up -d --build`
6. 验证服务 | Check the stack
   - Frontend: `http://127.0.0.1:3001`
   - Backend health: `http://127.0.0.1:8080/actuator/health`
   - Public boundary page: `http://127.0.0.1:3001/boundary`

## 本地开发用法 | Local development
- Frontend v2 dev server:
  - `pnpm --dir frontend-v2 install`
  - `pnpm --dir frontend-v2 dev`
  - default URL: `http://127.0.0.1:5174`
- Default frontend API target:
  - `VITE_API_BASE_URL=http://localhost:8080`
- Local validation:
  - `bash scripts/validate-local.sh`

## 文档导航 | Documentation
- 安装说明 | Install: `docs/ops/install.md`
- 升级说明 | Upgrade: `docs/ops/upgrade.md`
- 备份恢复 | Backup and restore: `docs/ops/backup-restore.md`
- 运维 Runbook | Operations runbook: `docs/ops/runbook.md`
- 支持边界 | Support boundaries: `docs/release/v2-support-boundaries.md`
- 反馈分流 | Feedback intake: `docs/release/v2-feedback-intake.md`
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
- 当前公开基线 | Current public baseline: `v2.0.3` on `main`
- 当前支持边界 | Current support boundary: `docs/release/v2-support-boundaries.md`
- 当前发布说明 | Current release notes: `docs/release/v2.0.3-release-notes.md`
- 先前 v2 发布说明 | Earlier v2 notes: `docs/release/v2.0.0-release-notes.md`, `docs/release/v2.0.1-release-notes.md`, `docs/release/v2.0.2-release-notes.md`

## 许可证 | License
- `LICENSE`
