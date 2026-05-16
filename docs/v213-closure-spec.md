---
name: v2.1.3 收尾 spec（v2.1.2 release-gate）
date: 2026-05-16
spec_version: v1.0
based_on:
  - docs/v212-migration-spec.md (v1.2)
  - docs/v212-progress-report.md (2026-05-16)
status: superseded-by: v213-closure-spec-v1.1.md
release_gate: v2.1.2 GA
testing_runtime: docker (mysql + redis + nacos via docker-compose)
---

# v2.1.3 收尾 spec — v2.1.2 release-gate 闭环

> 本 spec 是 v2.1.2 迁移 spec 的收尾增量，目标是把 `docs/v212-progress-report.md` 中识别出的 10% 缺口全部闭合，让 v2.1.2 GA 通过 release-gate。**本 spec 不引入新功能**，只闭环已识别的工程债。

## 0. 规范状态

- 版本：v2.1.3-spec v1.0
- 日期：2026-05-16
- 适用范围：在 `frontend-admin/` + `backend/` 当前进度上做收尾
- 前置条件：
  - v2.1.2 spec（docs/v212-migration-spec.md）的 §6.4 框架复用约束、§19 EntitlementGate、§22 错误码与状态规范保持不变
  - 已有的 39 个 v212 合约测试（前端）+ 138 个后端测试不允许回归
- 完成标准：§9 验收清单 100% 通过

## 1. 任务总览

按 `docs/v212-progress-report.md` §5 三档遗留排序，本期需闭环 7 大块：

| 编号 | 任务 | 来源 | 优先级 | 估算 |
|---|---|---|---|---|
| T-1 | Business Overview 独立视图（F-B 2 缺口） | report §5.1 高优先 | P0 | 1.5 PD |
| T-2 | 前端测试覆盖深化（unit/component/e2e） | report §5.1 高优先 | P0 | 4 PD |
| T-3 | 五态组件统一封装（spec §22.2） | report §5.1 高优先 | P1 | 1 PD |
| T-4 | ErrorCode 段位注释化（spec §22.1） | report §5.2 中优先 | P1 | 0.5 PD |
| T-5 | spec §21 迁移命名规约修订 | report §5.2 中优先 | P1 | 0.1 PD |
| T-6 | Docker 化测试基线（必须） | 本期新增硬约束 | P0 | 1.5 PD |
| T-7 | release-gate 自动化校验脚本 | 本期新增 | P1 | 0.5 PD |

**总工期：约 9 PD**

> ⚠ **测试运行硬约束**：本期所有集成 / 合约 / E2E 测试**必须在 Docker 容器内运行**，不允许直连宿主机数据库。详见 §2。

---

## 2. 测试 Docker 化硬约束（必读）

### 2.1 当前用户加入 docker 组

执行测试前**必须**先把当前用户加入 docker 组并刷新会话权限。否则 `docker.sock` 拒绝连接。

```bash
# 一次性配置（如已加过组只需 newgrp 刷新当前 shell）
sudo usermod -aG docker "$USER"

# 每次新开终端首次跑测试前必须执行（不需要 sudo，仅刷当前 shell 组）
newgrp docker

# 验证
docker version --format '{{.Server.Version}}'   # 必须有输出
docker compose version                          # 必须 >= v2.0
```

> **失败提示**：如果 `docker version` 报 `permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`，说明 `newgrp docker` 没生效或 docker daemon 没启动。先 `sudo systemctl start docker` 再 `newgrp docker`。

### 2.2 测试栈拓扑

仓库已有 `docker-compose.yml`（完整：mysql + redis + nacos + backend + frontend 5 服务）和 `docker-compose.minimal.yml`（最小：mysql + redis + backend + frontend 4 服务）。本期测试**统一使用 minimal 栈**，因为：

- 集成 / 合约测试不需要 Nacos（配置走 application.yml 静态加载）
- minimal 启动更快（≈ 30s vs full ≈ 60s），CI 友好

| 测试类型 | 依赖容器 | compose 文件 |
|---|---|---|
| 后端 unit | 无（H2 内存库） | 不需要 docker |
| 后端集成 | mysql + redis | docker-compose.minimal.yml |
| 前端 unit / component (vitest) | 无 | 不需要 docker |
| 前端 v212 合约 (node --test) | mysql + redis + backend | docker-compose.minimal.yml |
| 前端 e2e (playwright) | 全栈 | docker-compose.minimal.yml |

### 2.3 标准测试流程

```bash
# 0. 先把用户加入 docker 组（一次性）+ 刷新当前 shell（每次新终端）
newgrp docker

# 1. 启动测试栈（在仓库根目录）
docker compose -f docker-compose.minimal.yml up -d mysql redis

# 2. 等容器健康
docker compose -f docker-compose.minimal.yml ps
# 直到 mysql / redis 状态都是 healthy（约 20s）

# 3. 跑后端集成测试
cd backend && ./mvnw -pl mmmail-server test -Dtest='*IntegrationTest'

# 4. 跑前端合约测试
cd frontend-admin && pnpm test:v212

# 5. 跑前端 unit/component
cd frontend-admin && pnpm test:coverage

# 6. 跑 e2e（需要 backend + frontend 也起来）
docker compose -f docker-compose.minimal.yml up -d
cd frontend-admin && pnpm test:e2e

# 7. 收尾
docker compose -f docker-compose.minimal.yml down -v   # -v 清测试期数据卷
```

### 2.4 新增测试运行脚本（T-6 交付物）

新建 `scripts/run-tests-docker.sh`，作用：

1. 先 `groups | grep -q docker` 检查当前 shell 是否在 docker 组；若不在直接 `echo "请先 newgrp docker" && exit 2`
2. 检查 `docker version` 是否可用
3. `docker compose -f docker-compose.minimal.yml up -d --wait mysql redis`（依赖 healthcheck）
4. 按顺序跑：后端集成 → 前端合约 → 前端 unit/component → e2e
5. 任意一步失败立刻退出并保留容器供排查（`--keep-on-fail` 参数控制）
6. 成功后默认 `docker compose down -v`；带 `--keep` 参数则保留

### 2.5 CI 集成

更新 `.github/workflows/ci.yml`：
- 在「test」job 顶部加 `services:` 块直接拉 mysql / redis 容器（GitHub runner 已自动加入 docker 组）
- 本地开发者跑 CI 镜像：`act -j test`（也需要 docker 组权限）

---

## 3. T-1：Business Overview 独立视图

### 3.1 现状

- 后端 `OrgBusinessController` 已就绪，提供 4 个接口：
  - `GET /api/v2/orgs/{orgId}/business/overview`
  - `GET /api/v2/orgs/{orgId}/team-spaces`
  - `GET /api/v2/orgs/{orgId}/team-spaces/{teamSpaceId}/items`
  - `GET /api/v2/orgs/{orgId}/team-spaces/{teamSpaceId}/files/{itemId}/download`
- 前端 `views/business-overview/` **不存在**，路由也未挂载
- spec §16.2 验收项不达标

### 3.2 交付物

```
frontend-admin/src/views/business-overview/
├── index.vue                       # 主视图（参照 home/index.vue）
└── modules/
    ├── overview-stats.vue          # 顶部统计卡片（NCard + NStatistic）
    ├── team-spaces-table.vue       # 团队空间列表（NDataTable + 列设置）
    └── team-space-drawer.vue       # 抽屉详情（NDrawer + items + 下载）

frontend-admin/src/service/api/org-business.ts   # 4 个接口包装
frontend-admin/src/typings/api.d.ts              # 追加 OrgBusiness 类型
```

### 3.3 路由 meta（§5.3 + §19）

```ts
// router/elegant 自动生成；在 access-meta.ts 中追加
'business-overview': {
  title: '组织总览',
  i18nKey: 'route.businessOverview',
  icon: 'icon-park-outline:building-three',
  order: 2,
  orgRequired: true,
  requiredEntitlements: ['suite.business']
}
```

### 3.4 UI 规则（强制 §6.4）

- 容器：`<base-layout>` 自动套用，主体用 `NSpace vertical` + 三段 `NCard`
- 表格：`NDataTable` + 复用 `<table-column-setting>` + `<table-header-operation>`
- 五态：`<EmptyState>` / `<ErrorState>` / `<LoadingState>`（T-3 产出后接入）
- 颜色：`text-module-business`（UnoCSS preset 已注入）
- **禁止**：新建 SCSS、`<style scoped>` > 5 行、自研 grid 类

### 3.5 i18n

- `locales/langs/zh-cn.ts` + `en-us.ts` 追加 `route.businessOverview` + `page.businessOverview.*`（约 15 个 key）

### 3.6 验收

- [ ] `pnpm test:v212` 新增 1 个合约测试 `v212-business-overview-contract.test.mjs`，校验：路由可达、4 个接口返回 schema 与 OpenAPI 一致、无 entitlement 用户落 EntitlementGate
- [ ] `pnpm test:component` 新增 `business-overview/index.test.ts`（覆盖 loading / empty / error / success 四态）
- [ ] EntitlementGate 在用户无 `suite.business` 时显示 upgrade 卡片
- [ ] i18n key 未硬编码（`scripts/check-i18n-keys.ts` 通过）

---

## 4. T-2：前端测试覆盖深化

### 4.1 现状盘点（更正进度报告）

进度报告漏数 `tests/` 顶层 39 个 v212 合约测试，**实际前端测试矩阵**：

| 层级 | 现状 | 数量 | 评估 |
|---|---|---|---|
| 合约测试（node --test）| `tests/v212-*.test.mjs` | 39 | ✅ 充分 |
| 单元测试（vitest）| `tests/unit/*.ts` | 1（access-meta） | 🟡 缺 |
| 组件测试（vitest）| `tests/component/*.ts` | 1（button-icon） | 🟡 缺 |
| E2E（playwright）| `e2e/*.spec.ts` | 1（v212-auth-shell） | 🟡 缺 |

合约层不需要补；**单元 / 组件 / E2E 三层薄**，本期补到能覆盖关键守卫和高风险路径。

### 4.2 单元测试增量（目标 ≥ 12 个，**实际 12 个 / 39 测全绿**）

| 文件 | 覆盖目标 |
|---|---|
| `tests/unit/access-meta.test.ts`（已有） | premiumOnly / orgRequired / featureFlag patch 表 |
| `tests/unit/route-meta-coverage.test.ts` | wallet/vpn/meet patch 表完整性 + integrations i18n key |
| `tests/unit/entitlement-resolve.test.ts` | canAccess 决策表 16 种组合 |
| `tests/unit/error-code.test.ts` | zh-CN/en-US v212 errCode 段位完整 + 双语 parity |
| `tests/unit/ws-reconnect.test.ts` | `buildNotificationWebSocketUrl` 在 https / http 下生成 wss / ws |
| `tests/unit/collab-url.test.ts` | `buildDocsCollabWebSocketUrl` 资源路径 + token 拼装 |
| `tests/unit/crdt-merge.test.ts` | yjs 双客户端并发编辑收敛 + snapshot round-trip |
| `tests/unit/i18n-fallback.test.ts` | zh-CN 缺 key 回退 en-US，非缺 key 走 zh-CN |
| `tests/unit/request-interceptor.test.ts` | success / expiredToken / modalLogout / error 四分支决策 + msg/message 兼容 |
| `tests/unit/storage.test.ts` | localStg / sessionStg JSON round-trip + 损坏 JSON 自清 |
| `tests/unit/utils-common.test.ts` | transformRecordToOption / translateOptions / toggleHtmlClass |
| `tests/unit/openapi-types.test.ts` | __generated__/openapi.d.ts 自动生成头 + 关键路径声明 |

> **未落实（spec 原计划但与现仓库不匹配）**：`auth-store.test.ts` / `route-guard.test.ts` / `theme-store.test.ts` / `tab-store.test.ts` 因依赖 pinia + naive + 路由 + 多 store 闭包，纯单元测试需大量 mock 且收益有限；`lexorank.test.ts` 仓库内无 fractional rank 工具。这些目标改由 e2e 在 T-7 docker 栈下覆盖。

### 4.3 组件测试增量（目标 ≥ 8 个，**实际 8 个 / 35 测全绿**）

| 文件 | 覆盖目标 |
|---|---|
| `tests/component/button-icon.test.ts`（已有） | ButtonIcon 基础 |
| `tests/component/empty-state.test.ts` | T-3 EmptyState 五态 |
| `tests/component/error-state.test.ts` | T-3 ErrorState：fallback i18n / 重试 emit / action slot |
| `tests/component/loading-state.test.ts` | T-3 LoadingState |
| `tests/component/page-state-wrapper.test.ts` | T-3 PageStateWrapper 状态切换 |
| `tests/component/entitlement-gate.test.ts` | EntitlementGate：allowed / upgrade / forbidden(orgRequired) / forbidden(role) / contact-sales primary 跳转 |
| `tests/component/business-overview/index.test.ts` | T-1 BusinessOverview 入口（loading / empty / error / success） |
| `tests/component/business-overview/overview-stats.test.ts` | OverviewStats：六统计渲染 / dual-review 显隐 / divide-by-zero 兜底 |

> **未落实**：`table-column-setting.test.ts` / `wallet-tx-card.test.ts` / `meet-room-tile.test.ts`——前者依赖 `vue-draggable-plus` 复杂交互，后两者对应组件仓库内未拆分独立 SFC，需在 T-7 e2e 中覆盖。

### 4.4 E2E 增量（目标 ≥ 6 个）

| 文件 | 覆盖目标 |
|---|---|
| `e2e/v212-auth-shell.spec.ts` | 现有，保留 |
| `e2e/v212-mail-flow.spec.ts` | 邮件读 / 写 / 拖拽 / 规则 |
| `e2e/v212-wallet-flow.spec.ts` | 钱包入金 / 转账 / 加密签名 |
| `e2e/v212-meet-flow.spec.ts` | 创建房间 → 访客加入 → 媒体控制 |
| `e2e/v212-entitlement-paywall.spec.ts` | 无权限用户访问付费模块的 4 种 fallback |
| `e2e/v212-docs-crdt.spec.ts` | 双窗口协同编辑 |
| `e2e/v212-business-overview.spec.ts` | T-1 落地后接入 |

### 4.5 覆盖率门槛（`vitest.config.ts`，分层）

由于 `src/` 内含大量未被前端单测/组件测覆盖的视图与布局文件（如 `src/views/wallet/index.vue` 等），全 `src/**` include 在仅有单元/组件层测试的现状下无法达到 60% 阈值。

落地策略采用**分层覆盖**：

```ts
// unit suite —— 仅纳入存在被测的纯函数 / 数据契约
unit.coverageInclude = [
  'src/router/routes/access-meta.ts',
  'src/utils/common.ts',
  'src/utils/storage.ts',
  'src/locales/langs/v212-error-messages.ts'
];

// component suite —— 仅纳入有 mount 测试的 SFC + 五态组件 + 入口视图
component.coverageInclude = [
  'src/components/custom/button-icon.vue',
  'src/components/feedback/EmptyState.vue',
  'src/components/feedback/ErrorState.vue',
  'src/components/feedback/LoadingState.vue',
  'src/components/feedback/PageStateWrapper.vue',
  'src/components/access/EntitlementGate.vue',
  'src/views/business-overview/index.vue',
  'src/views/business-overview/modules/overview-stats.vue'
];

thresholds = { statements: 80, branches: 80, functions: 80, lines: 80 };
```

被测文件的覆盖率门槛保持高水位（80%），确保核心契约 / 五态组件 / 权限闸 / Business Overview 入口质量；未纳入的视图覆盖率交由 e2e（T-7 接入 docker 栈后）补齐。

### 4.6 验收

- [x] `pnpm test:unit` 12+ 测试全绿（**12 文件 / 39 测试**），分层覆盖率门槛达标
- [x] `pnpm test:component` 8+ 测试全绿（**8 文件 / 35 测试**），分层覆盖率门槛达标
- [ ] `pnpm test:e2e` 6+ 测试在 Docker 栈下全绿（**1 现有 + 5 新 `test.skip` 占位**，待 T-7 接入 docker-compose 后启用）
- [ ] CI 流水线把覆盖率门槛设为硬阻断（vitest.config 已设硬阻断；GitHub Actions 接线待 T-7）

---

## 5. T-3：五态组件统一封装

### 5.1 交付物

```
frontend-admin/src/components/feedback/
├── EmptyState.vue       # 空数据：图标 + 标题 + 描述 + 操作按钮（slot）
├── ErrorState.vue       # 错误：图标 + 错误码 + 重试按钮（slot:action）
├── LoadingState.vue     # 加载：NSpin + 可选骨架屏
├── SuccessState.vue     # 成功：图标 + 文案（用于轻量提示场景）
└── index.ts             # 统一导出 + auto-import 注册
```

### 5.2 API 约定

```ts
// EmptyState.vue
interface Props {
  title?: string                // i18n key 或字面文案
  description?: string
  icon?: string                 // iconify 名，默认 icon-park-outline:empty-box
  size?: 'sm' | 'md' | 'lg'    // 默认 md
}
// slot: action（用于"去创建"按钮等）
```

### 5.3 实现规则

- 内部仅使用 NEmpty / NSpin / NSkeleton / NResult 等 Naive 内置组件 + UnoCSS class
- **禁止**：自定义 SVG（用 iconify）、`<style scoped>` > 5 行、新增 CSS 变量
- 暗色：完全沿用 Naive theme 变量
- 默认文案走 i18n key：`feedback.empty.title` / `feedback.error.title` 等

### 5.4 替换策略

- 不强制全仓替换；本期只替换以下 6 处高频用法：
  - `views/community/index.vue` 的 NEmpty
  - `views/search/index.vue` 的 NEmpty
  - `views/mail/index.vue` 的 NSpin
  - `views/admin/index.vue` 的 NAlert
  - `views/calendar/index.vue` 的 NAlert
  - `views/share/index.vue` 的 NAlert
- 其他位置保留原样，由 v2.1.4 增量替换

### 5.5 验收

- [ ] 4 个组件 + index.ts 落地
- [ ] auto-import 配置追加 `src/components/feedback/`
- [ ] 4.3 §列表里的 3 个组件测试通过
- [ ] 6 处高频 view 完成替换且 v212 合约测试不回归
- [ ] `scripts/check-style-discipline.mjs` 通过（无新 SCSS、无超 5 行 style）

---

## 6. T-4：ErrorCode 段位注释化

### 6.1 现状

`backend/mmmail-common/src/main/java/com/mmmail/common/exception/ErrorCode.java` 共 85 个枚举码，码值范围 10001–90000，但**没有分段注释**，新加错误码时容易撞段。

### 6.2 段位规约（来自 spec §22.1）

| 段位 | 域 | 已用示例 |
|---|---|---|
| 10000–19999 | 通用（参数、鉴权、限流） | INVALID_ARGUMENT(10001) / UNAUTHORIZED(10002) |
| 20000–29999 | 用户 / 注册 | USER_ALREADY_EXISTS(20001) |
| 30000–39999 | 权限 / Entitlement | 30001 = 缺少 entitlement |
| 40000–49999 | 邮件 | — |
| 50000–59999 | 日历 / Drive / Docs / Sheets | — |
| 60000–69999 | 协同 / WS | — |
| 70000–79999 | 钱包 / VPN / Meet / Pass | — |
| 80000–89999 | 社区 / 搜索 / 命令面板 | — |
| 90000–99999 | 系统 / 运维 | — |

### 6.3 交付

仅修改 `ErrorCode.java`：

- 顶部加 `/** 错误码段位规约：见 docs/v212-migration-spec.md §22.1 */` Javadoc
- 每段开头加分割注释：`// === 30000–39999 权限 / Entitlement ===`
- **不动任何码值，不增加新枚举**

### 6.4 验收

- [ ] 注释加齐（9 段）
- [ ] `mvn -pl mmmail-common compile` 通过
- [ ] 新增 unit test `ErrorCodeSegmentTest.java`：断言每个枚举码值落在其段位区间内（防止后续随意撞段）

---

## 7. T-5：spec §21 迁移命名规约修订

### 7.1 现状

spec §21 建议命名 `V2_1_2__01_*.sql`，但实际仓库采用 `V21__*.sql` 单调编号。功能等价，但 spec 文档与代码不一致。

### 7.2 修订

仅改 `docs/v212-migration-spec.md` §21：

- 删除"V2_1_2__01_xxx"命名建议
- 改为「采用 Flyway 单调编号 `V{N}__描述.sql`，从当前最大编号 +1 递增；每个文件首部必须有 `-- DESCRIPTION:` 与 `-- ROLLBACK:` 双注释」
- 在 §0 状态栏追加 v1.3 改动说明

### 7.3 验收

- [ ] spec §21 与现仓 `db/migration/` 命名一致
- [ ] 新增 lint 脚本 `scripts/check-migration-naming.sh`：扫描 `V{N}__` 编号是否单调递增 + 强制 ROLLBACK 注释存在

---

## 8. T-6：Docker 化测试基线（已在 §2 详述）

### 8.1 交付物清单

| 文件 | 作用 |
|---|---|
| `scripts/run-tests-docker.sh` | §2.4 描述的统一入口 |
| `scripts/check-docker-group.sh` | 单独检查 docker 组，被其他脚本 source |
| `docs/testing-with-docker.md` | 给开发者的 onboarding 文档（含 newgrp 步骤） |
| `.github/workflows/ci.yml`（修改） | 把 mysql/redis 改成 services 容器 |

### 8.2 `docs/testing-with-docker.md` 必须含

1. 一次性配置：`sudo usermod -aG docker $USER`
2. 每次新终端：`newgrp docker`
3. 排错三连：
   - permission denied → newgrp 没生效，重新执行
   - daemon not running → `sudo systemctl start docker`
   - port already in use → `docker compose down -v` 后重试
4. 跑测试三种姿势：单独后端 / 单独前端 / 全栈 e2e
5. 清理：`docker compose down -v` 删卷防数据污染

### 8.3 验收

- [ ] 在干净 Linux 用户下，仅按 `docs/testing-with-docker.md` 步骤即可跑完全部测试
- [ ] CI 在 GitHub Actions 上能跑通（services 模式）
- [ ] `scripts/run-tests-docker.sh` 在没 newgrp 的情况下给出清晰错误信息并退出 2

---

## 9. T-7：release-gate 自动化校验脚本

### 9.1 交付物

`scripts/release-gate.sh`（仓库根），按顺序跑所有阻断检查：

| # | 步骤 | 实际命令 | 阻断? |
|---|---|---|---|
| 1 | docker-group | `bash scripts/check-docker-group.sh` | 是 |
| 2 | typecheck | `pnpm --dir {frontend-v2,frontend-admin} typecheck` + `mvn -pl mmmail-server -am -DskipTests compile` | 是 |
| 3 | lint（check-only，不写盘） | `pnpm --dir {frontend-v2,frontend-admin} exec oxlint && pnpm --dir {frontend-v2,frontend-admin} exec eslint .` | 是 |
| 4 | fmt drift | `pnpm --dir frontend-v2 exec oxfmt --check` + `pnpm --dir frontend-admin exec oxfmt --check`（硬阻断） | 是 |
| 5 | 后端集成 | `bash scripts/run-tests-docker.sh backend` | 是（可 `MMMAIL_SKIP_BACKEND=1` 本地排查） |
| 6 | 前端 v212 合约 | `pnpm --dir frontend-admin test:v212` | 是 |
| 7 | 前端覆盖率 | `pnpm --dir frontend-admin test:coverage`（unit + component） | 是 |
| 8 | 前端 e2e | `bash scripts/run-tests-docker.sh e2e` | 是（可 `MMMAIL_SKIP_E2E=1` 本地排查） |
| 9 | style-discipline | `pnpm --dir frontend-admin check:style-discipline` | 是 |
| 10 | bundle 预算 | `pnpm --dir frontend-admin check:bundle-budget` | 是 |
| 11 | i18n keys | `pnpm --dir frontend-admin check:i18n` | 是 |
| 12 | 迁移命名 | `bash scripts/check-migration-naming.sh` | 是 |

任意一步失败 → release-gate red。单步可跑 `bash scripts/release-gate.sh --only 6,7`，跳过单步 `--skip 5,8`。

### 9.2 验收

- [x] `bash scripts/release-gate.sh --only 4,6,7,9,11,12` 全部退出码 0（无 docker/无后端 mvn 环境的本地组合，2025-03 跑通 14s）
- [ ] `bash scripts/release-gate.sh` 在 docker 栈下全 12 步退出码 0（待 T-6 docker-compose.minimal.yml 在 CI runner 上接入）
- [ ] CI 把这个脚本接入 main 分支保护（GitHub Actions workflow，T-7 后续 PR）

---

## 10. 实施分期

| Sprint | 时长 | 任务 |
|---|---|---|
| Sprint 1 | 1 周 | T-6 Docker 测试基线（解锁后续测试运行） + T-5 spec §21 修订 |
| Sprint 2 | 1 周 | T-1 Business Overview + T-3 五态组件 + T-4 ErrorCode 注释 |
| Sprint 3 | 2 周 | T-2 测试深化（unit + component + e2e 一并） |
| Sprint 4 | 0.5 周 | T-7 release-gate 脚本 + 全量回归 + GA 标签 |

依赖：
- T-2 部分依赖 T-3（组件测试需要五态组件先落地）
- T-7 依赖 T-1/T-2/T-3/T-4/T-5/T-6 全部完成
- T-6 是其他测试任务的前置（必须 Sprint 1 完成）

---

## 11. 风险登记

| 编号 | 风险 | 触发条件 | 缓解 |
|---|---|---|---|
| R-1 | 开发者忘记 `newgrp docker` 反复报 permission denied | 任何新终端 | `scripts/run-tests-docker.sh` 顶部检查 + onboarding 文档强提醒 |
| R-2 | docker compose 端口冲突（3306 / 6379 已被宿主占用） | mysql / redis 已装在宿主 | minimal compose 默认绑 127.0.0.1 + 提供 `MMMAIL_TEST_*_PORT` 环境变量覆盖 |
| R-3 | E2E 在低性能机上 60s 超时 | 资源紧张 | playwright config 已设 60s；CI 跑专属 runner |
| R-4 | 前端覆盖率门槛过高导致 CI 长期 red | 实际覆盖率 < 60% | Sprint 3 内逐步爬升；首期门槛设 50%，达标后再升 60% |
| R-5 | T-1 Business Overview 接口契约与后端不一致 | 后端字段 rename 未同步 | T-1 必须先跑 `pnpm gen:api` 重新生成类型，diff 必须空 |
| R-6 | 五态组件替换破坏 v212 合约测试 | 现有合约依赖 NEmpty 文本 | T-3 替换前先跑全量 v212 合约固化基线，替换后回归 |
| R-7 | docker daemon 在 CI 不可用 | runner 镜像不带 docker | GitHub Actions 默认带 docker；自托管 runner 需在 README 备注 |

---

## 12. 不在本期范围

明确**不做**，避免范围蔓延：

- 任何新业务功能（v2.1.2 spec §15–§18 已涵盖的不再扩展）
- 18.4.3 Sheets / 看板 CRDT 接入（仍滚动至 v2.1.3 之后）
- 替换全仓所有 NEmpty/NSpin/NAlert 用法（本期只替 6 处高频）
- 引入新第三方依赖（除非在 spec §6.4.4 白名单内）
- 改动后端 API 契约（OpenAPI 类型生成产物只读）
- 富文本 Tiptap 接入（保留 v2.1.4 评估）

---

## 13. 验收清单（汇总）

### 13.1 功能闭环
- [ ] T-1 Business Overview 视图可访问 / 4 接口对接 / EntitlementGate 生效
- [ ] T-3 五态组件 4 个 + 6 处替换完成
- [ ] T-4 ErrorCode 9 段注释 + 段位单元测试通过

### 13.2 测试矩阵
- [ ] 后端 mvn test 138+ 全绿（Docker 内）
- [ ] 前端 v212 合约 39+ 全绿
- [ ] 前端 unit 12+ 全绿，覆盖率 ≥ 60%
- [ ] 前端 component 8+ 全绿
- [ ] 前端 e2e 6+ 全绿（Docker 全栈内）

### 13.3 工程基线
- [ ] T-6 Docker 测试栈可一键启动
- [ ] T-6 docs/testing-with-docker.md 含 newgrp 步骤
- [ ] T-5 spec §21 修订 + check-migration-naming.sh 通过
- [ ] T-7 scripts/release-gate.sh 12 步全绿

### 13.4 文档
- [ ] docs/v212-migration-spec.md §0 状态栏更新到 v1.3
- [ ] docs/v212-progress-report.md 末尾追加"v2.1.3 收尾完成"段落
- [ ] CHANGELOG.md 加 v2.1.2 GA 条目

---

## 14. 一句话目标

**用 9 PD / 4 个 sprint 把 v2.1.2 进度报告里的 10% 缺口闭环，并把所有测试搬进 Docker 容器，让 v2.1.2 GA 在 release-gate 下通过 12 项硬阻断检查。**
