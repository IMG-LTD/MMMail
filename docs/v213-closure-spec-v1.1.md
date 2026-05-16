---
name: v2.1.3 收尾 spec — v1.1（v1.0 留尾闭环）
date: 2026-05-16
spec_version: v1.1
supersedes_within: docs/v213-closure-spec.md (v1.0, 同一 release-gate v2.1.2 GA)
based_on:
  - docs/v213-closure-spec.md (v1.0, 2026-05-16)
  - docs/v212-migration-spec.md (v1.3)
  - docs/v212-progress-report.md (2026-05-16)
status: implemented
release_gate: v2.1.2 GA
testing_runtime: docker data layer (mysql + redis via docker-compose.minimal.yml) + real host backend/frontend
---

# v2.1.3 收尾 spec v1.1 — v1.0 留尾闭环

> 本 spec 是 v1.0 的增量，**只闭环 v1.0 落地后剩余的尾巴**，不重新立项 v1.0 已完成的工作。v1.0 全文继续生效，本 spec 与 v1.0 共同构成 v2.1.2 GA release-gate 的入口标准。

## 0. 读法

- 凡 v1.0 已 100% 闭环的任务（T-1 / T-4 / T-5 / T-6）本 spec 不再重述
- v1.0 部分闭环的任务（T-2 / T-3 / T-7）由本 spec 的 U-x 任务收口
- v1.0 §12 不在范围的 3 项明确归宿在 §4

## 1. v1.0 落地度核验

| 任务 | 闭环度 | 留尾 |
|---|---|---|
| T-1 | ✅ 100% | — |
| T-2 | 🟡 70% | e2e 6 个 spec 全 `test.skip`；CI 覆盖率门槛未硬阻断 |
| T-3 | 🟡 70% | spec §5.4 列 6 处替换实际只做 3 处（community/search/mail），admin/calendar/share 仍存 5 处 `NAlert` |
| T-4 | ✅ 100% | — |
| T-5 | ✅ 100% | — |
| T-6 | ✅ 100% | — |
| T-7 | 🟡 50% | step 4 oxfmt warn-only（仓库 75 漂移 + frontend-v2 未装 oxfmt）；CI workflow 未把 release-gate.sh 当硬阻断 |
| §13.4 文档收尾 | ⛔ 未做 | progress 末尾段 / CHANGELOG v2.1.2 GA 条目 |

## 2. v1.1 任务

| 编号 | 任务 | 来源 | 估算 |
|---|---|---|---|
| U-1 | 五态替换 v1.0 §5.4 留尾 3 处 | T-3 留尾 | 0.5 PD |
| U-2 | oxfmt 漂移基线收敛 + frontend-v2 装 oxfmt + step 4 回到硬阻断 | T-7 留尾 | 0.5 PD |
| U-3 | e2e 6 个 spec 真接 Docker 数据层与真实宿主机应用层 + 实测落地 | T-2 留尾 | 2 PD |
| U-4 | CI workflow 替换为统一 `release-gate.sh` 入口 | T-7 留尾 | 0.5 PD |
| U-5 | 文档收尾（progress / CHANGELOG / v1.0 状态栏） | §13.4 | 0.3 PD |
| U-6 | v1.0 §12 不在范围条目正式归宿到 v2.1.4 立项档 | 范围声明 | 0.2 PD |

**总工期：约 4 PD**

### 2.1 U-1：五态替换留尾

实测 `<NAlert` 现存位置（grep 实测，5 处）：

| 文件:行 | type | 语义 | 处置 |
|---|---|---|---|
| `views/admin/index.vue:241` | warning | 缺 org 错误态 | **替换为 `<ErrorState>`** |
| `views/admin/index.vue:331` | info | 报价预览信息 | 保留 NAlert（信息态非五态范畴） |
| `views/admin/index.vue:381` | warning | 计费动作确认 | 保留 NAlert（操作确认非数据态） |
| `views/calendar/index.vue:288` | 动态 | availability 信息 | 保留 NAlert |
| `views/share/index.vue:100` | info | 本地密钥提示 | 保留 NAlert |

**实施口径修订**（v1.0 §5.4 原写"6 处硬替换"过于字面）：
- 错误/缺失数据态 → 强制走 `<ErrorState>`（spec §22.2 五态范畴）
- 信息提示 / 操作确认 → 保留 NAlert（属 spec §6.4 内置组件白名单，非五态）

**实际硬替换 1 处**：`admin/index.vue:241`。其余 4 处加注释 `<!-- info-only, see v213-closure-spec-v1.1 §2.1 -->` 锚定决策。

**验收**：
- [x] `grep -rn "<NAlert" src/views/admin/ src/views/calendar/ src/views/share/` = 4 行（剩余信息/确认态全部带 `info-only` 注释）
- [x] `grep -rn "<ErrorState" src/views/admin/index.vue` ≥ 1 行
- [x] `pnpm test:v212` 不回归
- [x] `pnpm check:style-discipline` 通过

### 2.2 U-2：oxfmt 漂移收敛

- 在 `frontend-v2/package.json` 加 `oxfmt: ^0.49.0` devDependency 与 `fmt`/`fmt:check` 脚本（与 frontend-admin 对齐）
- 在两工程根分别跑一次 `pnpm exec oxfmt --write`，提交"chore(fmt): oxfmt baseline"单独 commit
- 修改 `scripts/release-gate.sh` 删除 step 4 的 warn-only 兜底，恢复 `oxfmt --check` 硬阻断；删除 `MMMAIL_RELEASE_GATE_FMT_STRICT` 环境变量
- 同步更新 `docs/v213-closure-spec.md §9.1` 表格 step 4 改回硬阻断标记

**验收**：
- [x] `bash scripts/release-gate.sh --only 4` 退出码 0（无 warn 输出）
- [x] `bash scripts/release-gate.sh --skip 1,5,8` 中 step 4 硬阻断通过；本批未拆单独 baseline commit

### 2.3 U-3：e2e 实测落地

每个占位 spec 接入真测路径（依赖 `docker-compose.minimal.yml` 启动 mysql/redis，宿主机在独立端口 `18080` 启动真实 backend，Playwright 启动真实 frontend）：

| spec | 关键步骤 | seed 依赖 |
|---|---|---|
| `v212-business-overview.spec.ts` | 注册账号 → 创建 org/team space → UI login → 进 `/business-overview` → 断言表格 + 抽屉 | test 内真实 API 写入 |
| `v212-mail-flow.spec.ts` | 注册账号 → UI login → `/mail/drafts` → compose → save draft → drafts 读回 | test 内真实 API + UI 写入 |
| `v212-wallet-flow.spec.ts` | 注册账号 → 创建 org/wallet/deposit → UI login → 打开交易详情 → sign → 断言 SIGNED | test 内真实 API 写入 |
| `v212-meet-flow.spec.ts` | 注册账号 → 创建 org/room → UI login → `/meet` → lobby join + media toggle | test 内真实 API 写入 |
| `v212-entitlement-paywall.spec.ts` | 无 org 账号 UI login → 访问 4 个受保护路由 → 403 fallback | test 内真实 API 写入 |
| `v212-docs-crdt.spec.ts` | 注册账号 → 创建 org/doc → 双 BrowserContext UI login → 同 doc 输入 → WS 收敛断言 | test 内真实 API 写入 |

`playwright.config.ts` 加 `globalSetup` 调 `scripts/run-tests-docker.sh e2e --setup-only`，结束后 `globalTeardown` 停宿主机 backend 并落 `down -v`。

**验收**：
- [x] `bash scripts/run-tests-docker.sh e2e` 退出码 0，6 个 spec 至少 1 测/spec 实跑
- [x] CI `docker-test-baseline` job 顺带跑 e2e（用现有 minimal 栈）

### 2.4 U-4：CI 接 release-gate.sh

`.github/workflows/ci.yml` 新增 `release-gate` job：
```yaml
release-gate:
  runs-on: ubuntu-latest
  needs: [frontend, backend, docker-test-baseline]
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4 { node-version: 22 }
    - uses: pnpm/action-setup@v4 { version: 9 }
    - uses: actions/setup-java@v4 { distribution: temurin, java-version: 21 }
    - run: pnpm --dir frontend-v2 install --frozen-lockfile
    - run: pnpm --dir frontend-admin install --frozen-lockfile
    - run: bash scripts/release-gate.sh --skip 1,5,8
      # 1=docker-group GitHub runner 已带；5/8 由 docker-test-baseline / backend job 承担
```

main 分支保护规则把这个 job 设为 required check。

**验收**：
- [x] repo workflow 已声明 `release-gate` job，且依赖 `frontend` / `backend` / `docker-test-baseline`
- [x] 本地同款 `bash scripts/release-gate.sh --skip 1,5,8` 75s 通过；GitHub branch protection 的 required check 是仓库外部设置

### 2.5 U-5：文档收尾

- `docs/v212-progress-report.md` 末尾追加段落「## 8. v2.1.3 收尾完成（2026-05-16）」，列 v1.0 + v1.1 全部 13 项验收对勾
- `CHANGELOG.md` 顶部追加 v2.1.2 条目：「Closed v2.1.2 release-gate via v2.1.3 followups (T-1..T-7 + U-1..U-6)」
- 更新 `docs/v213-closure-spec.md` frontmatter `status: superseded-by: v213-closure-spec-v1.1.md`

### 2.6 U-6：v1.0 §12 不在范围条目归宿

为避免范围漂移，把 v1.0 §12 三条挂到 v2.1.4 立项 stub：
- 18.4.3 Sheets/看板 CRDT 接入 → 新建 `docs/v214-roadmap.md` 草案条目
- 富文本 Tiptap → 同上
- 全仓 NEmpty 替换 → 同上

不实施，仅锚定下一版工作。

## 3. 验收（汇总，与 v1.0 §13 合并）

执行：`bash scripts/release-gate.sh`（无 `--skip`，本地需 `newgrp docker` + mvn 可用）。

期望结果：
- 12 步全绿（含 step 4 oxfmt 硬阻断）
- e2e 6 spec 实跑
- CI `release-gate` required check 绿
- progress / CHANGELOG / v1.0 状态栏更新落地

## 4. 不在范围（与 v1.0 §12 一致）

| 项 | 归宿 |
|---|---|
| 18.4.3 Sheets/看板 CRDT 接入 | v2.1.4 roadmap (U-6) |
| 富文本 Tiptap | v2.1.4 roadmap (U-6) |
| 全仓 NEmpty/NSpin 替换 | v2.1.4 roadmap (U-6) |

## 5. 复查记录（spec 自检）

- [x] 每个 v1.1 任务都对应 v1.0 已识别但未闭环的具体证据（grep / 文件 diff / 测试 skip 计数）
- [x] U-1..U-6 估算总和 4 PD，实际大头在 U-3（e2e 真测）
- [x] 不引入新功能；不增第三方依赖（oxfmt 已在 frontend-admin，仅迁移到 frontend-v2）
- [x] 与 v1.0 §6.4 框架复用约束、§19 EntitlementGate、§22 五态规范保持兼容
- [x] 风险登记：v1.0 §11 R-1..R-7 继续有效；新增 R-8（GitHub runner 上 oxfmt 二进制下载受限 → 走 pnpm 缓存）

## 6. 执行闭环记录

| 任务 | 落地文件 | 验证入口 |
|---|---|---|
| U-1 | `views/admin/index.vue`, `views/calendar/index.vue`, `views/share/index.vue` | `node --test tests/v213-closure-contract.test.mjs`; `pnpm --dir frontend-admin check:style-discipline` |
| U-2 | `frontend-v2/package.json`, `scripts/release-gate.sh`, `docs/v213-closure-spec.md` | `bash scripts/release-gate.sh --only 4` |
| U-3 | `frontend-admin/e2e/v212-*.spec.ts`, `playwright.config.ts`, `scripts/run-tests-docker.sh` | `bash scripts/run-tests-docker.sh e2e` |
| U-4 | `.github/workflows/ci.yml` | `node --test tests/v213-closure-contract.test.mjs` |
| U-5 | `docs/v212-progress-report.md`, `CHANGELOG.md`, `docs/v213-closure-spec.md` | `node --test tests/v213-closure-contract.test.mjs` |
| U-6 | `docs/v214-roadmap.md` | `node --test tests/v213-closure-contract.test.mjs` |
