# MMMail v2 Mainline 首次安装指南

**版本**: `v2.0.4`
**日期**: `2026-04-24`

## 当前边界与前置条件
- 当前公开基线为 `main` / `v2.0.4`，仓库主线只保留 v2 代码与运行路径。
- 当前默认自托管运行模型为 `frontend-v2 Web + 单个 Spring Boot 后端进程 + MySQL / Redis`。
- 标准模式可额外启用 `Nacos`，但这不代表仓库已经交付真实微服务网格。
- Docker 安装需要 `Docker` 与 `Docker Compose v2`；建议至少 `4 CPU / 8 GB RAM`。
- 最小模式端口：`3001`、`8080`、`3306`、`6379`；标准模式额外使用 `8848`。
- Docker 路径会在宿主机发布 MySQL `3306` 与 Redis `6379`；上线前请用防火墙或安全组限制访问，并在面向互联网暴露前为 Web/API 配置 TLS 与反向代理。
- 权威范围见 `docs/release/v2-support-boundaries.md` 与 `docs/open-source/module-maturity-matrix.md`。

## 1. 路径选择
首次部署前先选择一种路径：

| 路径 | 适用场景 | 运行形态 |
| --- | --- | --- |
| 一键安装 | 希望脚本完成校验并启动 Compose | 可选最小模式或标准模式 |
| Docker 手动安装 | 希望自己执行 Compose 命令、查看状态和日志 | 可选最小模式或标准模式 |
| 裸机手动安装 | 不使用 Docker，需要接入现有主机、数据库或前端托管 | MySQL 8.4 + Redis 7.4 + Java 后端 + 前端构建产物 |
| 本地体验 / 开发 | 贡献代码、快速体验前端或调试本地 API | Vite dev server + 本地后端/依赖 |

模式边界：
- 最小模式：`MySQL + Redis + backend + frontend`，不启动 Nacos；`.env` 中必须保持 `MMMAIL_NACOS_ENABLED=false`，使用 `docker-compose.minimal.yml`。
- 标准模式：在最小模式基础上包含 `Nacos`；`.env` 中必须设置 `MMMAIL_NACOS_ENABLED=true`，使用默认 `docker-compose.yml`。

所有自托管路径都应从环境模板开始：
1. 复制模板：`cp .env.example .env`
2. 编辑 `.env` 并至少替换这些占位值：
   - `MMMAIL_JWT_SECRET`
   - `SPRING_DATASOURCE_PASSWORD`
   - `SPRING_REDIS_PASSWORD`
   - `MYSQL_ROOT_PASSWORD`
3. 标准模式还需要替换：
   - `NACOS_USERNAME`
   - `NACOS_PASSWORD`
4. 保持 `SPRING_SQL_INIT_MODE=never`，数据库结构迁移由 Flyway 管理。

## 2. 一键安装
一键安装脚本会检查环境文件、验证必填变量，并调用对应的 Docker Compose 文件。

Linux / macOS：
- 最小模式：`bash scripts/install.sh minimal`
- 标准模式：`bash scripts/install.sh standard`

Windows PowerShell：
- 最小模式：`./scripts/install.ps1 minimal`
- 标准模式：`./scripts/install.ps1 standard`

说明：
- 如果未传模式，Bash 与 PowerShell 脚本都会优先尝试交互选择；在非交互环境中会回退到最小模式。自动化部署建议仍显式传入 `minimal` 或 `standard`。
- 默认读取仓库根目录 `.env`；如需自定义路径，可设置 `MMMAIL_ENV_FILE=/path/to/.env`，PowerShell 也可使用 `-EnvFile path`。
- 脚本不会替你生成生产级密钥；如果 `.env` 不存在，脚本会从 `.env.example` 创建后退出，请先替换占位值再重跑。
- 最小模式要求 `MMMAIL_NACOS_ENABLED=false`；标准模式要求 `MMMAIL_NACOS_ENABLED=true`。

## 3. Docker 手动安装
### 最小模式
最小模式适合首次自托管验证：它只启动 MySQL、Redis、后端与前端，不启动 Nacos。

1. 确认 `.env`：
   - `MMMAIL_NACOS_ENABLED=false`
   - `VITE_API_BASE_URL=http://localhost:8080`
   - 必填密钥与密码已替换占位值
2. 启动：
   - `docker compose --env-file .env -f docker-compose.minimal.yml up -d --build`
3. 查看状态：
   - `docker compose -f docker-compose.minimal.yml ps`
4. 查看日志：
   - `docker compose -f docker-compose.minimal.yml logs -f backend`
   - `docker compose -f docker-compose.minimal.yml logs -f frontend`
5. 停止：
   - `docker compose -f docker-compose.minimal.yml down`

### 标准模式
标准模式在 Compose 中额外包含 Nacos，适合需要验证 Nacos 集成边界的环境。

1. 确认 `.env`：
   - `MMMAIL_NACOS_ENABLED=true`
   - `NACOS_USERNAME` 与 `NACOS_PASSWORD` 已替换占位值
   - 其他必填密钥与密码已替换占位值
2. 启动：
   - `docker compose --env-file .env up -d --build`
3. 查看状态：
   - `docker compose ps`
4. 查看日志：
   - `docker compose logs -f backend`
   - `docker compose logs -f frontend`
5. 停止：
   - `docker compose down`

启动前可手动执行运行时校验：
- `./scripts/validate-runtime-env.sh .env`

如需连同卷一起清理，请先确认数据可丢弃：
- 最小模式：`docker compose -f docker-compose.minimal.yml down -v`
- 标准模式：`docker compose down -v`

## 4. 裸机手动安装
裸机路径用于不依赖 Docker 的主机安装。以下为高层步骤，具体安全加固、进程守护、反向代理、TLS 与备份策略需要由运维环境补齐。

1. 准备基础依赖：
   - MySQL `8.4`，创建 `mmmail` 数据库与应用用户。
   - Redis `7.4`，设置访问密码并持久化数据。
   - Java `21` 与 Maven。
   - Node.js / pnpm，用于构建 `frontend-v2`。
2. 准备环境变量：
   - `cp .env.example .env`
   - 替换所有 `replace-with-*` 占位值。
   - 设置 `SPRING_DATASOURCE_URL` 指向裸机 MySQL。
   - 设置 `SPRING_REDIS_HOST`、`SPRING_REDIS_PORT`、`SPRING_REDIS_PASSWORD` 指向裸机 Redis。
   - 不使用 Nacos 时设置 `MMMAIL_NACOS_ENABLED=false`；需要 Nacos 时准备 Nacos 并设置 `MMMAIL_NACOS_ENABLED=true`、`NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`。
   - 保持 `SPRING_SQL_INIT_MODE=never`，Flyway 负责 schema migration。
3. 启动后端：
   - `bash scripts/start-backend-local.sh` 与 Maven 命令不会自动读取仓库根目录 `.env`；运行前先在当前 shell 导出或加载变量，例如：`set -a; source .env; set +a`。
   - 可用本地辅助脚本：`bash scripts/start-backend-local.sh`
   - 或在 `backend` 目录内运行 Maven Spring Boot：`mvn -pl mmmail-server -am -DskipTests spring-boot:run`
   - 生产守护方式可用 systemd、supervisor 或等价平台能力管理 Java 进程；systemd 服务请配置 `EnvironmentFile=/path/to/.env` 或等价环境注入。
4. 构建并托管前端：
   - `pnpm --dir frontend-v2 install`
   - `VITE_API_BASE_URL=https://api.example.com pnpm --dir frontend-v2 build`
   - 将 `frontend-v2/dist` 交给静态文件服务器或反向代理托管。
   - 前端命令不会自动读取仓库根目录 `.env`；请通过 shell 环境变量，或 `frontend-v2/.env.production` 设置构建时 `VITE_API_BASE_URL`。
5. 数据迁移：
   - 查看迁移状态：`./scripts/db-upgrade.sh .env info`
   - 需要升级时执行：`./scripts/db-upgrade.sh .env upgrade`

## 5. 本地体验 / 开发
本地体验适合开发、调试或快速查看前端交互。默认前端开发端口是 `5174`，Compose 托管前端端口是 `3001`。

前端开发服务器：
- `pnpm --dir frontend-v2 install`
- 通过 shell 环境变量或 `frontend-v2/.env.local` 设置 `VITE_API_BASE_URL=http://localhost:8080`。
- `pnpm --dir frontend-v2 dev`
- 默认地址：`http://127.0.0.1:5174`

后端本地启动：
- 先准备 MySQL、Redis 与 `.env`。
- `bash scripts/start-backend-local.sh` 不会自动读取仓库根目录 `.env`；启动前先导出或加载变量，例如：`set -a; source .env; set +a`。
- 启动后端：`bash scripts/start-backend-local.sh`
- 默认后端健康检查：`http://127.0.0.1:8080/actuator/health`

本地校验：
- `bash scripts/validate-local.sh`
- 前端测试：`pnpm --dir frontend-v2 test`

## 6. 验证方式
安装或启动后至少验证：
- Frontend：`http://127.0.0.1:3001`
- Backend health：`http://127.0.0.1:8080/actuator/health`
- Boundary page：`http://127.0.0.1:3001/boundary`
- Migration info：`./scripts/db-upgrade.sh .env info`

可选验证：
- Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI：`http://127.0.0.1:8080/v3/api-docs`
- System health：`http://127.0.0.1:3001/settings/system-health`

## 7. 常见问题
- `validate-runtime-env.sh` 失败：
  - 检查 `.env` 是否仍保留 `replace-with-*` 占位值，或是否缺少必填变量。
- 最小模式仍要求 Nacos 凭据：
  - 确认 `.env` 中 `MMMAIL_NACOS_ENABLED=false`，并使用 `docker-compose.minimal.yml` 或 `scripts/install.sh minimal` / `scripts/install.ps1 minimal`。
- 标准模式启动后后端无法连接 Nacos：
  - 确认使用默认 `docker-compose.yml`，并设置 `MMMAIL_NACOS_ENABLED=true`、`NACOS_USERNAME`、`NACOS_PASSWORD`。
- Frontend 页面可打开但 API 指向错误：
  - 检查 `VITE_API_BASE_URL`，重新构建前端镜像或重新启动 Vite dev server。
- Backend 无法连接 MySQL：
  - 检查 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` 与 MySQL 监听地址。
- 数据库表结构异常或缺表：
  - 不要启用 Spring SQL 初始化；保持 `SPRING_SQL_INIT_MODE=never`，使用 `./scripts/db-upgrade.sh .env info` 查看 Flyway 状态。
