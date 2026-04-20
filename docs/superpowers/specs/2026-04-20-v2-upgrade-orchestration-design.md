# MMMail v2 升级编排设计（2.0.0 Release Orchestration）

- 版本：`v1.0`
- 日期：`2026-04-20`
- 状态：`冻结设计稿`
- 来源方案：
  - `docs/backend-v2-upgrade-solution-plan.md`（v1.8，2026-04-19 冻结）
  - `docs/frontend-v2-replacement-solution-plan.md`（v1.8，2026-04-19 冻结）
  - `docs/fullstack-v2-completeness-review.md`（v1.9，2026-04-19 联审）
- 用户已确认决策：
  - 范围：**A** — 全量落地两份冻结方案
  - 执行模式：**B** — 主 Agent orchestrator + 并行 worktree 子 Agent
  - 质量门禁：**A** — 严格模式，冻结方案门禁一个不减
  - Release 定义：**A** — 机制完备 + tag + GitHub Release，不要求真实生产金丝雀
  - 分支策略：**C** — `release/2.0.0` 集成分支，切片 PR → 集成分支，终局一次性回 `main` + tag

---

## 0. 背景与目标

MMMail 已产出两份 v1.8 冻结稿：backend v2 模块化重组方案、frontend v2 路由与交互替代方案。二者共同定义了 2.0.0 的目标架构、硬门禁与阶段依赖。本设计**不重新决定技术方案**，只解决"如何把两份冻结稿在严格模式下一次性落地并发布 v2.0.0"这一**执行编排**问题。

设计范围：分支拓扑、Milestone 划分、切片规则、review gate、进度持久化、Done 检查清单。
设计非范围：业务实现细节（归 `docs/superpowers/plans/m<N>-*.md`）、契约字段细节（归两份冻结稿及后续详细设计）。

---

## 1. 执行架构

### 1.1 分支拓扑

```
origin/main          ──●─────●─────●──────●─────●──── (仅 hotfix / doc; 冻结期保持 v1 可发)
                           │                           │
origin/release/2.0.0 ──────●──●──●──●──●──●──●──●──●── (v2 集成分支; 每周末从 main 同步)
                              │  │  │  │  │  │  │  │
worktree slices ──────────── PR PR PR PR PR PR PR PR   (每切片独立 worktree + 子 Agent + PR)
```

### 1.2 Worktree 生命周期

- 切片启动：`git worktree add .tmp/worktrees/<slice-id> origin/release/2.0.0 -b slice/<milestone>/<domain>-<topic>`
- 切片结束：PR → `release/2.0.0` → 过 CI + `scripts/validate-local.sh` → 主 Agent 审校 → merge
- worktree 合并或放弃后立即 `git worktree remove --force` 清理

### 1.3 子 Agent 调度

- **主 Agent（编排层）职责**：拆切片、派 sub-agent、审 PR、解冲突、推进 Milestone 门禁、与用户 check-in
- **子 Agent（实现层）职责**：在隔离 worktree 内完成一个切片的实现 + 测试 + 文档 + PR body
- 主 Agent **禁止**直接写业务代码；例外仅限冲突解决、契约同步、跨切片统筹补丁，必须写进 `orchestration-log.md`
- 子 Agent **禁止**触碰其他 worktree 或 `.tmp/worktrees/` 外的文件（除提交 PR 所需 git 操作）

### 1.4 CI 与本地验证

- 切片 PR 必过：`.github/workflows/ci.yml` + `scripts/validate-local.sh`
- Milestone 准出额外跑：契约 diff + 覆盖率报告 + Smoke E2E + migration dry-run
- Release Gate 跑：三语完整校验 + 视觉回归 + AI/MCP end-to-end + public share 安全全回归

### 1.5 代码/文档所有权

- `docs/superpowers/specs/` — brainstorming 产物（本文件 + 后续冻结设计稿）
- `docs/superpowers/plans/` — writing-plans 产物（每 Milestone 一份实施计划）
- `docs/superpowers/progress/` — orchestration 状态（实时快照、切片日志、milestone 报告、升级事项）

---

## 2. Milestone 计划（M0 - M8）

每个 Milestone 有**准入**（必须满足才开始）和**准出**（必须满足才通过）。准出条件直接引用两份冻结方案的对应门禁，不重复论证。

### M0 — 冻结落库（Backend Phase 0）

- **目标**：域边界、表归属矩阵、OpenAPI 清单、事件目录、tenant/scope 模型、gateway rewrite matrix、Labs 清退清单、flag 清单在 `docs/` 正式入档并 commit
- **准入**：本设计稿冻结
- **准出**：backend v2 方案"架构门 + 数据门"通过；用户 G-milestone 签字
- **并行**：无
- **切片估算**：3-4 个 PR

### M1 — Foundation & Identity（Backend Phase 1 + Frontend G1）

- **目标**：backend `foundation / identity / org-governance / gateway baseline` 模块；frontend `G1 Router & Layout`（canonical routes + redirect registry + layout contract）
- **准入**：M0 签字完成
- **准出**：
  - backend：identity SDK 可用、tenant context 传播链路 + 多租户 interceptor + ArchUnit 通过、identity/org 域测试 ≥70% 覆盖
  - frontend：canonical routes 全落地、redirect matrix 与后端 gateway rewrite 一一对应、layout/meta schema 统一
- **并行子流**：backend `identity / foundation / org-governance / gateway` 四个 worktree 同时推进；frontend `G1` 独立推进（不依赖后端契约）
- **切片估算**：8-10 个 PR

### M2 — Platform Kernel & Auth/Scope（Backend Phase 1.5 + Frontend G2 + 部分 G4）

- **目标**：jobs/outbox/relay worker、notification kernel、audit kernel、Redis Streams relay、gateway canary；frontend `useScopeGuard / useSoftAuthLock / useAsyncActionState / useDialogStack`
- **准入**：M1 签字完成
- **准出**：
  - backend：跨域事件可投递、outbox dead-letter 可观测、gateway rewrite 启用并有埋点
  - frontend：`G2 Auth & Scope` 五类场景（未登录 / 无权限 / 脏状态切换 / 401 软锁 / scope switch）通过 Smoke E2E
- **并行子流**：backend platform kernel 四子 Agent + frontend 四 shared contract 子 Agent
- **切片估算**：10-12 个 PR

### M3 — Mail / Pass / Public Share 安全链路（Backend Phase 2 + Frontend G3）

- **目标**：mail runtime 边界重构、pass 拆 vault/secure-link/alias/authenticator、`share/mail|drive|pass` 统一安全模型；frontend `usePublicShareFlow` + 三 public share view 真接线
- **准入**：M2 签字完成
- **准出**：
  - backend：`backendV2PublicShare` flag 可用、三类 share 安全（token 哈希 / 限流 / 锁定 / 吊销 / 审计）回归通过、Identity/Pass/Public Share 覆盖 ≥80% line / 75% branch
  - frontend：`G3 Share & Security Entry` Smoke E2E 绿
- **并行子流**：backend Phase 3（M4 内容）允许此时启动（冻结方案 §9.1 明文允许）
- **切片估算**：10-12 个 PR

### M4 — Drive / Platform / AI / MCP（Backend Phase 3 + Frontend G5 + Frontend Phase 3）

- **目标**：drive storage/share/version/index；search / notification / audit 平台化；**AI Platform**（model routing / context assembly / approval / audit）；**MCP Service**（registry / grant matrix / tool gateway / secret binding）；frontend `useCopilotPanel / useAutomationRunbook / useMcpRegistry` + Mail + Calendar 模块全链路 + Smoke E2E & I18N baseline
- **准入**：M3 签字完成
- **准出**：
  - backend：AI suggestion P95 ≤6s、MCP invocation accepted P95 ≤2s、grant matrix + audit 可按 tenant/module/tool 检索
  - frontend：G5 通过；Mail compose/trust-state/selection/Copilot + Calendar month/week/day/agenda 全接 v2 API；`zh-CN` + `zh-TW` + `en` catalog 到位
- **并行子流**：最高并行期（backend 6+ 子 Agent + frontend 5+ 子 Agent）
- **切片估算**：14-16 个 PR

### M5 — Docs / Sheets / Aggregation / Story（Backend Phase 4 + Frontend Phase 4-5）

- **目标**：calendar / docs / sheets 协作面；frontend Drive + Pass 工作台；Docs/Sheets 编辑器；`/collaboration / /command-center / /notifications`；onboarding / failure-modes 集成
- **准入**：M4 签字完成
- **准出**：docs/sheets contract 稳定 + 长任务模型稳定 + 所有 workspace 模块接入 `useCopilotPanel`
- **并行子流**：backend Phase 4 三域并行 + frontend Phase 4-5 多模块并行
- **切片估算**：12-14 个 PR

### M6 — Billing / Governance & Legacy 退出（Backend Phase 5 + Frontend Phase 6）

- **目标**：billing 子域抽出、team space governance、frontend legacy redirect 全量生效、legacy 路径 `302 → 301` 化
- **准入**：M5 签字完成
- **准出**：legacy `frontend/` 仅保留 redirect stub，业务代码移除；billing 合同稳定
- **并行子流**：backend billing + frontend legacy removal 独立并行
- **切片估算**：5-7 个 PR

### M7 — Labs 决议 + 测试闭环（Backend Phase 6 + Frontend Phase 6 测试）

- **目标**：Labs 模块签字（保留 / 冻结 / 退役）；全量视觉回归、E2E、三语回归、deprecated route exit report
- **准入**：M6 签字完成
- **准出**：本设计 §6 Done 清单除 M8 动作外全部 ✓
- **并行子流**：测试子 Agent 按模块并行
- **切片估算**：6-8 个 PR

### M8 — Release Gate

- **动作**：
  1. `release/2.0.0` → `main` PR（一次性 merge）
  2. `git tag -a v2.0.0 -m "MMMail v2.0.0"` → push
  3. `gh release create v2.0.0` 附 `docs/release/v2.0.0-release-notes.md`
- **准出**：GitHub Release 页 live、`main` CI 绿、tag 推远端；用户 G-release 签字
- **切片估算**：2-3 个 PR（release notes + changelog + final docs 刷新）

**汇总切片估算：68-83 个 PR，跨 8 个 Milestone。**

---

## 3. 切片（Slice）分解规则

### 3.1 切片大小约束

| 维度 | 上限 | 下限 |
|---|---|---|
| 生产代码净增 / 净改 | ≤ 1500 行 | ≥ 50 行 |
| 涉及文件数 | ≤ 30 个 | ≥ 2 个 |
| 跨域跨模块 | ≤ 1 个主域（允许 1 个 adapter / contract 依赖） | — |
| 估计子 Agent 工时 | ≤ 1 个长会话 | — |

超过上限必须再拆；小于下限允许同类合并。

### 3.2 切片自包含交付物

每个切片必须同时交付以下六项，否则不算"完成"：

1. **实现代码**（按目标域的 `api / application / domain / infrastructure / jobs` 分层）
2. **单元 + 集成测试**（敏感域使用 Testcontainers；覆盖率符合该域门禁）
3. **契约片段**（若触及 API）：OpenAPI 片段 + 生成的 TS SDK diff + 错误码字典更新
4. **Migration**（若触及 schema）：expand/backfill/switch/contract 四段中明确的一段，不允许一次性破坏
5. **三语 catalog**（若新增用户可见文案）：`zh-CN` + `zh-TW` + `en` 同时入
6. **切片日志**：`docs/superpowers/progress/slices/<slice-id>.md`，含变更摘要、已过门禁、后续切片 TODO

### 3.3 并行安全 — 文件独占规则

两个并行 worktree 切片**不得同时**修改：

- 同一 `.java` / `.vue` / `.ts` 文件
- 同一张 DB 表的 migration 文件
- 同一 `contracts/openapi/<domain>.yaml` 的同一 path
- `pnpm-lock.yaml` / `pom.xml` 顶层依赖声明（仅允许一个子 Agent 改，其他待合并后 rebase）

主 Agent 调度时预先做"文件冲突矩阵"检查；冲突的切片改串行。

### 3.4 契约先行（Contract Freeze Boundary）

某个域的 API 契约（OpenAPI + 错误码 + 事件 schema）必须由**独立契约切片**先冻结入 `release/2.0.0`，之后方可并行：

- 后端实现切片
- 前端 SDK 生成切片
- 前端消费切片

契约未冻结前禁止三者启动。违反此规则视为严格模式硬性违规。

### 3.5 切片命名与追踪

- 分支：`slice/<milestone>/<domain>-<topic>` 如 `slice/m2/jobs-outbox-kernel`
- PR 标题：`[<milestone>][<domain>] <topic>` 如 `[M2][platform] jobs & outbox kernel`
- PR body 必须声明：所属 Milestone、所属域、依赖前置切片 ID、已过门禁清单、触发的后续切片 ID
- 合并策略：`merge`（遵守 `CLAUDE.md`）

### 3.6 子 Agent 调度规则

- 主 Agent 同时在跑的 worktree 子 Agent **最多 4 个**
- 子 Agent 必须在 worktree 根目录运行，禁止 `cd` 到其他目录
- 子 Agent 返回时必须包含 diff 摘要、门禁通过证据、PR 地址
- 若子 Agent 返回时 worktree 存在未 commit 改动，视为失败，主 Agent 清理 worktree 重派

### 3.7 失败重试

- 切片 PR 第一次 CI 红：子 Agent 在同一 worktree 修复并 push
- 第二次 CI 红：主 Agent 诊断根因，决定修复 / 再拆 / 回滚
- 第三次仍红：视为 Milestone 阻塞，主 Agent 升级给用户

---

## 4. Check-in 与 Review Gate

### 4.1 三级 Review Gate

| 级别 | 触发点 | 主 Agent 默认行为 |
|---|---|---|
| **G-slice** | 每个切片 PR 打开时 | PR 过 CI + `validate-local.sh` 后自动审校并 merge 到 `release/2.0.0`；CI 红或子 Agent 异常时升级 |
| **G-milestone** | Milestone 所有切片合入、准出校验前 | **暂停等用户签字**；推送 Milestone 报告（改动摘要 / 门禁证据 / 下一 Milestone 启动清单 / 已知风险） |
| **G-release** | M8 tag 前 | **暂停等用户签字**；推送最终 release notes、legacy removal 证据、向后兼容回归、2.x 路线 |

理由：68-83 个切片若每个都等签字不现实。严格门禁已由 CI + validate-local + 文件冲突矩阵挡住主要风险；切片级风格与设计契合由主 Agent inline 审校，异常才升级。

### 4.2 强制升级（打断 G-slice 自动流）

即便在 G-slice 自动化流中，以下 **9 类**情形必须暂停并升级到用户：

1. **契约破坏** — 子 Agent 要改已冻结的 OpenAPI path / 事件 schema / 错误码（需变更评审）
2. **Migration destructive** — `DROP TABLE / DROP COLUMN / NON-NULL 回填 / 索引删除`（冻结方案 §7.2 expand-only 外动作）
3. **数据迁移规模超阈** — 单次 migration 影响行数 > 100 万
4. **外部依赖新增** — 新 Maven / npm 依赖（审安全性与许可证）
5. **基础设施引入** — Kafka / 独立 MQ / 独立数据库（backend 方案 §11.1 第 5 项，触发架构级可变项评审）
6. **安全基线变更** — key rotation 机制、session 模型、token 哈希算法改动
7. **Flag 生命周期变动** — 新增 flag、提前删除 flag、owner 变更
8. **Legacy destructive** — 删除旧 `frontend/` 或 `backend/` 代码文件（M6 前禁止；M6 执行时需确认清单）
9. **规划偏离** — 实现偏离冻结方案合同（如未走 `useDialogStack` 私写 Dialog 栈）

**升级消息固定包含**：事项摘要 + 冻结方案引用条款 + 建议 + 风险 + 需用户确认的 A/B 选项。

### 4.3 信息流

- `docs/superpowers/progress/state.md` — 实时快照（见 §5.2）
- 每次 check-in 开始时用户读此文件快速同步
- 主 Agent 每合切片、每派子 Agent 均更新；每 Milestone 结束写一份 `milestone-reports/m<N>-report.md`

### 4.4 进度健康线（超阈主动升级）

| Milestone | 预估切片 | 实际切片超过 | 累计 CI 红超过 | 主 Agent 动作 |
|---|---|---|---|---|
| M0 | 3-4 | 8 | 5 次 | 升级，建议重审 |
| M1 | 8-10 | 18 | 10 次 | 同上 |
| M2 | 10-12 | 22 | 12 次 | 同上 |
| M3 | 10-12 | 22 | 12 次 | 同上 |
| M4 | 14-16 | 28 | 15 次 | 同上 |
| M5 | 12-14 | 24 | 12 次 | 同上 |
| M6 | 5-7 | 14 | 7 次 | 同上 |
| M7 | 6-8 | 16 | 8 次 | 同上 |

### 4.5 用户随时打断

用户任何时刻说"停 / 暂停 / 改方案 / 回滚"，主 Agent 立即停止派新切片，回到当前状态清点；不等下一 check-in 点。

---

## 5. 进度持久化与会话续接

### 5.1 状态目录布局

```
docs/superpowers/
├── specs/
│   └── 2026-04-20-v2-upgrade-orchestration-design.md   ← 本文件
├── plans/
│   ├── m0-phase0-freeze.md
│   ├── m1-foundation-identity.md
│   ├── m2-platform-kernel-auth-scope.md
│   ├── m3-mail-pass-public-share.md
│   ├── m4-drive-ai-mcp-mail-calendar.md
│   ├── m5-docs-sheets-aggregation.md
│   ├── m6-billing-legacy-exit.md
│   ├── m7-labs-test-closure.md
│   └── m8-release-gate.md
├── progress/
│   ├── state.md                          ← 实时快照
│   ├── orchestration-log.md              ← append-only 主 Agent 决策记录
│   ├── slices/<slice-id>.md              ← 一切片一文件
│   ├── milestone-reports/m<N>-report.md  ← G-milestone 签字定稿
│   └── escalations/<date>-<topic>.md     ← 强制升级事项与用户签字
```

### 5.2 `state.md` 固定结构

```md
# v2 升级实时状态

- 当前 Milestone: M<N>
- 当前集成分支: release/2.0.0 @ <commit>
- main @ <commit>
- 上次同步 main → release/2.0.0: <date>

## 在跑切片 (in-flight)
| slice-id | milestone | domain | worktree | sub-agent | status | 阻塞 |

## 已合切片（本 Milestone）
| slice-id | PR# | 合入时间 | CI |

## 阻塞与升级
| 事项 | 升级时间 | 等谁 | 链接 |

## 下一步
- 主 Agent 即将派出的切片
- 等待解除的前置
```

主 Agent 每合切片、每派子 Agent 都更新本文件。

### 5.3 会话续接协议（Resume Protocol）

任何新会话恢复顺序固定：

1. 读 `state.md` — 知"现在在哪"
2. 读当前 Milestone 的 `plans/m<N>-*.md` — 知"要做什么"
3. 读 `orchestration-log.md` 尾部 20 条 — 知"最近决策与为什么"
4. 读所有 `status: in-flight` 切片的 slice log — 知"正在跑的 worktree 状态"
5. 跑 `git worktree list` + `gh pr list -B release/2.0.0` — 对齐实际远端状态
6. 若 5 与 1-4 偏差以 5 为准并纠正状态文件

该 6 步协议在 M0 执行时写进 `CLAUDE.md` 确保未来任何会话遵循。

### 5.4 GitHub 侧镜像

- **不**为每个切片开 Issue（会爆炸）
- **每个 Milestone 开一个 GitHub Issue**，body 同步 `milestone-reports/m<N>-report.md`，关闭即代表 G-milestone 签字
- 切片 PR 在 body 标注 `Milestone: #<issue-id>`
- 2.0.0 Release Issue 作为 M8 签字入口，关闭 = tag 发布

若用户不想开 Issue，全部留本地 docs 亦可；主 Agent 每次签字点主动提醒。

### 5.5 回滚点

- **切片级**：`git revert` 该切片的 merge commit
- **Milestone 级**：每 Milestone 启动时主 Agent 打 `checkpoint/m<N>-start` tag；回滚即 `git reset --hard checkpoint/m<N>-start`（需用户签字）
- **跨 Milestone**：需 check-in 决议，不自行执行
- **Release 后应急**：tag 不删；新发 `v2.0.1` hotfix 或 `v2.0.0-1` revert 组合（由用户决定）

---

## 6. "Done" 定义与 2.0.0 最终检查清单

严格模式下 2.0.0 = 两份冻结方案全部门禁逐条绿灯 + tag + GitHub Release。以下清单可核对。

### 6.1 代码与架构 Done

**Backend**（来源：backend v2 §5、§6、§7）

- [ ] Maven 多模块按 §6.2 分层：foundation / core domain / platform capability / adapter / labs
- [ ] 7 个逻辑运行面各自独立 migration：gateway / identity / mail / drive / pass / workspace / platform
- [ ] 10 个核心域各具 `api / application / domain / infrastructure / jobs / contract / db / test`
- [ ] platform-service 承载：jobs / outbox / relay、search、notification、observability、audit、**AI orchestration / model routing**、**MCP registry / tool gateway**
- [ ] API 版本化：`/api/v1/*` + `/api/v2/*` + `/share/*` 三段共存；gateway rewrite matrix 全量落地
- [ ] Redis Streams relay worker + outbox + dead-letter + retry/backoff 全部就绪
- [ ] row-level tenancy + ArchUnit + Flyway lint + MyBatis tenant interceptor 通过
- [ ] 跨域强制"事件 / 补偿"模式；无 XA；无跨域外键

**Frontend**（来源：frontend v2 §3-§5、§10.3）

- [ ] canonical 路由体系按 §3.2 全表落地（Public / Share / Story / Workspace / Aggregation / Governance / Research）
- [ ] deprecated 路由全部按 §4.1 redirect（`302` → `301`）或 §4.2 retire
- [ ] 10 个 shared contract 全部实装并被业务模块消费
- [ ] shell / scope / drawer / dialog / offline / soft-auth / public-share / async-action 八类交互按 §5 统一合同实现
- [ ] AI Copilot 抽屉 + Command Center automation + Settings Integrations 三 surface 接 backend v2 AI/MCP 平台合同

### 6.2 质量门禁 Done

- [ ] Backend 核心域覆盖 ≥70%；Identity / Pass / Public Share ≥80% line / 75% branch
- [ ] Testcontainers 集成测试覆盖所有核心域
- [ ] 契约测试：OpenAPI snapshot + 生成 TS SDK + 错误码字典
- [ ] Smoke E2E：auth / redirect / public share / route guard 全绿
- [ ] gateway rewrite / compatibility 回归：7 条 redirect 全覆盖
- [ ] 三语（`zh-CN` / `zh-TW` / `en`）catalog 全量；最长文案桌面+mobile 双端回归通过
- [ ] 性能：public LCP ≤2.5s p75、auth shell LCP ≤3.0s p75、INP ≤200ms p75、bundle budget 达标
- [ ] Telemetry：统一 schema、最小事件集、公共字段全落地
- [ ] a11y：键盘 / focus / landmark / 对比度全过
- [ ] 安全：public share token 哈希 / 限流 / 锁定 / 吊销 / 审计全部可验证；AI/MCP audit 可按 tenant/module/tool 检索
- [ ] Labs 清退签字完成（保留 / 冻结 / 退役三类各有 owner 签名）

### 6.3 灰度与回滚 Done（机制完备，不要求真实生产切流）

- [ ] 全量 flag 就位：`backendV2Identity / backendV2MailRuntime / backendV2Drive / backendV2Workspace / backendV2PublicShare / backendV2AiPlatform / backendV2McpPlatform` + `frontendV2.enabled / sharePass / aiCopilot / automation / mcpGovernance / redirects / labsRedirect`
- [ ] flag 有 `owner / created_at / review_at / remove_by` 元数据
- [ ] 灰度切流在 `docker-compose.yml` 内完整演练（按 org / user allowlist / route prefix）
- [ ] 回滚脚本：关闭 flag → 重启 → 验证旧入口恢复；回滚后 `mmmail:v2:*` 本地数据保留 14 天
- [ ] gateway rewrite 灰度和 deprecation header 埋点可验证

### 6.4 文档与发布物 Done

- [ ] `CHANGELOG.md` 新增 2.0.0 段落，覆盖所有 M0-M7 交付
- [ ] `docs/release/v2.0.0-release-notes.md`：已落地清单 + 运维交接手册 + 2.x 路线 + 已知限制
- [ ] `docs/deployment-runbook.md` 更新到 v2 架构
- [ ] `README.md` 更新为 v2 说明 + 升级路径
- [ ] 全部 Milestone reports 签字归档

### 6.5 Tag 与发布 Done

- [ ] `release/2.0.0` 所有 PR 合入、CI 绿、无未解冲突
- [ ] 全量回归脚本最后一次在 `release/2.0.0` 跑通
- [ ] `release/2.0.0 → main` 一次性 merge PR 完成
- [ ] `git tag -a v2.0.0`（带 release notes 摘要） → `git push origin v2.0.0`
- [ ] `gh release create v2.0.0` 附 `docs/release/v2.0.0-release-notes.md`
- [ ] 仓库 `main` 保持 CI 绿

### 6.6 明确**不**在 2.0.0 范围内（记入 2.x Roadmap）

两份方案本身已明确标记为"后续版本"或"不阻塞冻结"的事项，本次**不**塞入 2.0.0：

- Labs 模块真正产品化实现（Meet / VPN / Lumo / Wallet / Standard Notes parity 仅保留 preview 壳层 + route plumbing）
- `database-per-tenant` 升级（backend §11.1 第 2 项）
- Kafka / 独立 MQ 替换 Redis Streams（backend §11.1 第 5 项）
- Billing ledger 精细模型、外部支付编排（backend §11.1 第 3 项）
- Docs/Sheets 实时协作引擎重型升级（backend §11.1 第 4 项）
- 真实生产环境金丝雀切流（用户确认的 Q4-A 决策）

以上在 `docs/release/v2.0.0-release-notes.md` 的 "2.x Roadmap" 段落中明写。

### 6.7 最终签字流程（G-release）

1. 主 Agent 推 M8 准备报告到 `docs/superpowers/progress/milestone-reports/m8-report.md`，附 6.1-6.6 清单全过证据（CI link、报告链接、脚本输出）
2. 用户逐项审校，如全过：主 Agent 执行 6.5 tag + release 动作
3. 如有异议：回退对应 Milestone 返工

---

## 7. 冻结条款

本设计稿一经用户签字冻结，下列事项不得被子 Agent 或后续切片擅自偏离，偏离必须走 §4.2 强制升级：

1. Milestone 数量、顺序、准入准出条件
2. 切片大小约束、契约先行规则、文件独占规则
3. G-slice 自动化、G-milestone / G-release 等签字的分级原则
4. 进度目录布局、`state.md` 固定字段、Resume Protocol 6 步
5. Done 清单构成

细节变更（切片命名微调、报告模板字段扩展）允许主 Agent 在 orchestration-log 记录后自行推进。

---

## 8. 参考

- `docs/backend-v2-upgrade-solution-plan.md`（v1.8）
- `docs/frontend-v2-replacement-solution-plan.md`（v1.8）
- `docs/fullstack-v2-completeness-review.md`（v1.9）
- `docs/backend-v2-eight-expert-review.md`
- `docs/frontend-v2-fourth-audit-review.md`
- `CLAUDE.md`（项目根）
- `scripts/validate-local.sh`
- `.github/workflows/ci.yml`
