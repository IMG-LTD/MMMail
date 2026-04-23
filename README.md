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

## 自托管 / 快速开始 | Self-host / Quick start
首次部署请先选择安装路径；完整步骤、前置条件与验证方式见 `docs/ops/install.md`。

- 一键安装：Linux / macOS 使用 `scripts/install.sh minimal|standard`，Windows PowerShell 使用 `scripts/install.ps1 minimal|standard`。
- Docker 手动安装：复制 `.env.example` 为 `.env`，替换占位值，按最小模式或标准模式运行 Docker Compose。
- 裸机手动安装：自行准备 MySQL、Redis、Java 后端与前端构建/托管，数据库迁移由 Flyway 管理。
- 本地体验：用于开发或快速试用前端体验，按 `docs/ops/install.md` 的本地命令启动。

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
