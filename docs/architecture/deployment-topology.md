# MMMail v2.0.3 Deployment Topology

**版本**: `v2.0.3`
**日期**: `2026-04-23`

## 目标
- 提供两个可复现的单机 `Docker Compose` 拓扑，覆盖当前 `v2.0.3` 公开主路径。
- `docker-compose.yml` 表示标准模式；`docker-compose.minimal.yml` 表示更轻量的最小自托管模式。
- 交付对象是本地试用、自托管验证和后续运维脚本化，不追求生产级高可用。

## 当前运行模型
- 当前自托管运行模型是：
  - 一个 `frontend-v2` Web 前端
  - 一个 `Spring Boot` 后端进程
  - `MySQL` + `Redis`
- 标准模式里的 `Nacos` 只代表本地注册/配置依赖占位，不代表仓库已经拆分出真实服务发现拓扑或多服务编排。
- 当前仓库没有把 `Kafka` 作为当前公开运行主路径。

## 标准模式服务拓扑
| 服务 | Compose 名称 | 端口 | 责任 |
|---|---|---|---|
| Frontend | `frontend` | `3001` | `frontend-v2` Web 工作区入口 |
| Backend | `backend` | `8080` | Spring Boot API、认证、Mail / Calendar / Drive / Admin |
| MySQL | `mysql` | `3306` | 主业务库，启动时由 Flyway 执行版本化迁移 |
| Redis | `redis` | `6379` | 会话、缓存与限流基线 |
| Nacos | `nacos` | `8848` | 标准模式下的本地注册/配置依赖占位 |

## 最小模式服务拓扑
| 服务 | Compose 名称 | 端口 | 责任 |
|---|---|---|---|
| Frontend | `frontend` | `3001` | `frontend-v2` Web 工作区入口 |
| Backend | `backend` | `8080` | Spring Boot API、认证、Mail / Calendar / Drive / Admin |
| MySQL | `mysql` | `3306` | 主业务库，启动时由 Flyway 执行版本化迁移 |
| Redis | `redis` | `6379` | 会话、缓存与限流基线 |

最小模式通过 `MMMAIL_NACOS_ENABLED=false` 显式关闭 Nacos 相关启动校验与 Compose 依赖。

## 网络与启动顺序
- `frontend -> backend`
- 标准模式：`backend -> mysql / redis / nacos`
- 最小模式：`backend -> mysql / redis`
- `mysql`、`redis` 使用 health check 控制依赖顺序。
- 标准模式下 `nacos` 以 `service_started` 作为依赖条件。

## 数据与持久化
- `mmmail-mysql-data`：MySQL 数据目录
- `mmmail-redis-data`：Redis AOF 持久化目录
- 标准模式额外包含 `mmmail-nacos-data`：Nacos 本地数据目录
- `mmmail-drive-data`：Drive 本地文件存储目录

## 运行时环境边界
- 所有真实凭据从仓库外 `.env` 注入。
- `.env.example` 只保留占位值，不可直接用于启动。
- `VITE_API_BASE_URL` 是当前前端 API 目标配置入口。
- 启动前必须执行：
  - `./scripts/validate-runtime-env.sh .env`

## 健康检查
- Frontend：`GET http://127.0.0.1:3001`
- Backend：`GET http://127.0.0.1:8080/actuator/health`
- MySQL：`mysqladmin ping`
- Redis：`redis-cli ping`

## 已知限制
- 当前 Compose 不包含 Kafka；相关配置只应视为保留位。
- 标准模式下 Nacos 以 standalone 方式启动，仅用于本地单机场景。
- 首发阶段不提供多节点、TLS 终止、对象存储、外部反向代理编排。
