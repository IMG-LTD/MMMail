# 用 Docker 跑测试

> v2.1.3 起所有集成 / 合约 / E2E 测试**必须**在 Docker 容器内运行。本文是给开发者的 onboarding 步骤与排错手册。spec 出处：`docs/v213-closure-spec.md` §2。

## 1. 一次性准备

### 1.1 把当前用户加入 docker 组

```bash
sudo usermod -aG docker "$USER"
```

加完之后**当前 shell 还感知不到新组**（usermod 改的是 `/etc/group`，但已登录的 shell 进程的 supplementary groups 列表是登录时固化的）。两种刷新办法选一：

```bash
# 方式 A：在当前终端立即生效（仅刷此 shell）
newgrp docker

# 方式 B：彻底刷新（所有以后开的 shell 都自动带 docker 组）
# 注销并重新登录系统
```

> ⚠ **每次新开终端**首次跑测试前，都要确认 `groups` 输出里有 `docker`。如果开发机重启后没了，重跑 `newgrp docker`。

### 1.2 验证

```bash
./scripts/check-docker-group.sh
```

预期输出：

```
[check-docker-group] OK — docker 27.x.x, compose v2.x.x
```

如果报 `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`，去 §3 排错。

## 2. 跑测试

### 2.1 标准入口（推荐）

```bash
# 跑全套（backend 集成 + 前端合约 + 前端 unit/component + e2e）
./scripts/run-tests-docker.sh

# 只跑某一段
./scripts/run-tests-docker.sh backend    # 后端集成
./scripts/run-tests-docker.sh contract   # 前端 v212 合约（39 个）
./scripts/run-tests-docker.sh unit       # 前端 unit + component（vitest）
./scripts/run-tests-docker.sh e2e        # 前端 e2e（playwright）

# 失败后保留容器供排查
./scripts/run-tests-docker.sh backend --keep
```

成功结束会自动 `docker compose down -v` 清测试期数据；带 `--keep` 时保留。

### 2.2 手动模式（调试用）

```bash
# 1. 启基础设施
docker compose -f docker-compose.minimal.yml up -d --wait mysql redis

# 2. 跑后端集成
( cd backend && ./mvnw -pl mmmail-server test -Dtest='*IntegrationTest' )

# 3. 跑前端合约
( cd frontend-admin && pnpm test:v212 )

# 4. 启全栈（e2e 需要）
docker compose -f docker-compose.minimal.yml up -d --wait

# 5. 跑 e2e
( cd frontend-admin && pnpm test:e2e )

# 6. 收尾
docker compose -f docker-compose.minimal.yml down -v
```

### 2.3 各类测试与容器依赖对照

| 测试类型 | 是否需要容器 | 哪些容器 |
|---|---|---|
| 后端 unit（H2） | ❌ | 无 |
| 后端集成 | ✅ | mysql + redis |
| 前端 unit/component（vitest） | ❌ | 无 |
| 前端 v212 合约（node --test） | ✅ | mysql + redis + backend |
| 前端 e2e（playwright） | ✅ | mysql + redis + backend + frontend |

## 3. 排错三连

### 3.1 `permission denied ... docker.sock`

| 现象 | 原因 | 修复 |
|---|---|---|
| `groups` 里没有 `docker` | 没加组 | `sudo usermod -aG docker $USER` 然后 `newgrp docker` |
| `groups` 里有 `docker` 但 daemon 报权限 | 当前 shell 未刷新 | `newgrp docker` 或注销重登 |
| 加完组、重登后仍然报错 | docker daemon 没跑 | `sudo systemctl start docker` |

### 3.2 端口被占用

`docker compose up` 报 `port is already allocated`：

```bash
# 查谁占了端口
sudo ss -ltnp | grep -E '(:3306|:6379|:8080|:3001)'

# 选项 1：停掉宿主上对应服务
sudo systemctl stop mysql        # 占 3306 的
sudo systemctl stop redis        # 占 6379 的

# 选项 2：让测试栈用别的端口（在 .env.test 里改）
# compose 当前绑 127.0.0.1:3306:3306，改第一个 3306 即可
```

### 3.3 测试容器之间留下脏数据

```bash
# 一键清掉所有测试卷（mysql + redis + drive 数据）
docker compose -f docker-compose.minimal.yml down -v

# 更彻底：连同 image 一起清
docker compose -f docker-compose.minimal.yml down -v --rmi local
```

## 4. CI 中的执行方式

GitHub Actions runner 默认带 docker 且自动加组，无需 `newgrp`。CI 配置见 `.github/workflows/ci.yml` 的 `services:` 块。

自托管 runner 需在初始化脚本中执行 `usermod -aG docker $USER` 并重启 runner 进程。

## 5. 常见问题

**Q: 我能不能把测试跑在宿主机的 mysql/redis 上不要 docker？**
A: 本地调试可以，但 spec §2.1 明确要求 release-gate 的所有集成测试在 docker 容器内跑，避免「本机能过 / CI 挂」的环境漂移。本期 PR 评审时会跑 `scripts/run-tests-docker.sh` 验证。

**Q: 拉镜像太慢？**
A: 配 docker registry mirror。Linux: `/etc/docker/daemon.json` 加 `{"registry-mirrors": ["https://your-mirror.example.com"]}` 然后 `sudo systemctl restart docker`。

**Q: M 系列 Mac 跑不起 mysql:8.4？**
A: 在 `.env.test` 里设 `CONTAINER_MYSQL_IMAGE=mysql:8.4-oracle` 或换 `mariadb:11.4`（schema 兼容），compose 会通过 `${CONTAINER_MYSQL_IMAGE}` 读取。

**Q: 我的 PR 没改测试，还要跑 docker 吗？**
A: 是。release-gate 脚本（`scripts/release-gate.sh`，T-7 交付）任意一项失败都阻塞。
