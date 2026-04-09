# Community Edition v1.6.1 Deployment Topology

**版本**: `v1.6.1-topology`  
**日期**: `2026-04-09`  
**作者**: `Codex`

## 目标
- 提供两个可复现的单机 `Docker Compose` 拓扑，覆盖 `v1.6.1` 当前公开主路径。
- `docker-compose.yml` 表示标准模式；`docker-compose.minimal.yml` 表示更轻量的最小自托管模式。
- 交付对象是本地试用、自托管验证和后续运维脚本化，不追求生产级高可用。

## 标准模式服务拓扑
| 服务 | Compose 名称 | 端口 | 责任 |
|---|---|---|---|
| Frontend | `frontend` | `3001` | Nuxt 3 Web 工作区入口 |
| Backend | `backend` | `8080` | Spring Boot API、认证、Mail / Calendar / Drive / Admin |
| MySQL | `mysql` | `3306` | 主业务库，启动时由 Flyway 执行版本化迁移 |
| Redis | `redis` | `6379` | 会话、缓存与限流基线 |
| Nacos | `nacos` | `8848` | 标准模式下的本地注册/配置依赖占位，当前以 standalone 模式提供 |

## 最小模式服务拓扑
| 服务 | Compose 名称 | 端口 | 责任 |
|---|---|---|---|
| Frontend | `frontend` | `3001` | Nuxt 3 Web 工作区入口 |
| Backend | `backend` | `8080` | Spring Boot API、认证、Mail / Calendar / Drive / Admin |
| MySQL | `mysql` | `3306` | 主业务库，启动时由 Flyway 执行版本化迁移 |
| Redis | `redis` | `6379` | 会话、缓存与限流基线 |

最小模式通过 `MMMAIL_NACOS_ENABLED=false` 显式关闭 Nacos 相关启动校验与 Compose 依赖，不把 Nacos 拉入默认本地采用路径。

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
- `MMMAIL_NACOS_ENABLED` 默认为 `true`；设置为 `false` 时进入最小模式。
- `SPRING_SQL_INIT_MODE` 必须保持 `never`；`schema.sql` / `data.sql` 仅作为 Flyway 基线迁移输入，不再由 Spring SQL init 直接执行。
- 启动前必须执行：
  - `./scripts/validate-runtime-env.sh .env`

## 健康检查
- Frontend：`GET http://127.0.0.1:3001`
- Backend：`GET http://127.0.0.1:8080/actuator/health`
- MySQL：`mysqladmin ping`
- Redis：`redis-cli ping`

## 已知限制
- 当前 Compose 不包含 Kafka；后端使用本地默认地址 `127.0.0.1:9092`，若相关能力启用需在宿主侧额外提供。
- 标准模式下 Nacos 以 standalone 且关闭 auth 的方式启动，仅用于本地单机场景。
- 最小模式只降低本地采用依赖，不等于交付真正的服务注册/配置中心替代方案。
- 首发阶段不提供多节点、TLS 终止、对象存储、外部反向代理编排。
