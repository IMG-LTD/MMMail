---
name: v2.1.2 上线清理 spec — 仓库瘦身 / 重命名 / 首次入仓 / 残留治理
date: 2026-05-16
spec_version: shipping-v1.1
based_on:
  - docs/v213-closure-spec.md (v1.0, superseded)
  - docs/v213-closure-spec-v1.1.md (v1.1, implemented)
  - docs/v212-migration-spec.md (v1.3)
status: implemented
implemented_commit: pending
release_gate: v2.1.2 GA shipping
risk_level: HIGH (大规模目录重命名 + untracked 首次入仓 + 不可逆删除)
iteration_history:
  - v0.draft 一次性脚本（弃，未考虑 worktree / 引用图 / 可逆性）
  - v0.5 分阶段（弃，遗漏 frontend/ 重叠、.env.test 含 secrets、worktree 占用 .worktrees）
  - v1.0 当前版（含三轮内审 + 引用图实测 + 每步可逆 + release-gate 兼容矩阵）
  - v1.1 执行优化版（移除 git add . / stash -u / dev-only 大文件入 Git，改为外部本地归档 + 显式暂存 + repo-local worktree 清理）
review_passes:
  - pass-1 数字对账：所有计数 / 路径与 git/grep 输出比对
  - pass-2 可逆性：每个阶段给出 reset 命令与回退验证
  - pass-3 release-gate 兼容：每步对 12 个 release-gate step 的影响表
---

# v2.1.2 上线清理 spec — 仓库瘦身 / 重命名 / 首次入仓 / 残留治理

> 本 spec 在 v2.1.3 收尾全部完成（v1.0 + v1.1）的基础上，处理**进入上线分支前的仓库整理**：清理无关文件、重命名前端目录、把开发期产物迁到仓库外本地归档。**本 spec 不改动业务代码逻辑**，只做仓库形态调整。任何阶段失败都必须能基于 baseline tag 与本地归档回退。

---

## 0. 背景调查（实测）

### 0.1 顶层目录跟踪状况

| 目录 | tracked 文件数 | 物理大小 | 性质 |
|---|---:|---:|---|
| `backend/` | 1081 | 170 MB | ✅ 已入仓，核心 Java |
| `frontend-v2/` | 234 | 370 MB | ✅ 已入仓，v2 主线（含 366 MB node_modules） |
| `soybean-admin-main/` | **0** | 935 MB | ⛔ **从未入仓**，含 927 MB node_modules + v2.1.x 全部前端代码 |
| `frontend/` | **0** | 407 MB | ⛔ 空骨架 + 400 MB node_modules，CHANGELOG v2.0.3 已宣布废弃 |
| `tests/` | **0** | 36 KB | ⛔ 顶层 v213-closure-contract 测试，未入仓 |
| `ops/` | **0** | 20 KB | ⛔ Grafana dashboard / 压测脚本，未入仓 |
| `docs/` | 194 | 347 MB | 🟡 含 274 MB 历史快照（MMMail.zip + MMMail/） |
| `scripts/` | 18 | 128 KB | ✅ 已入仓 |
| `contracts/` | 17 | 156 KB | ✅ 已入仓 |
| `config/` | 3 | 16 KB | ✅ 已入仓 |
| `artifacts/` | 0 | 1.6 MB | ⛔ CI 产物（已 .gitignore） |

**核心震撼**：v2.1.3 收尾期所有 T-/U- 落地的前端代码（views/business-overview、tests/unit/component、e2e/real-backend.ts、release-gate 测试…）**全部躺在 untracked**。release-gate 在我本地能跑绿，是因为文件物理存在，但 push 到 origin 会丢失。这是**必须修复的最高优先项**。

### 0.2 顶层"残留"目录

```
.tmp/                 — diagnostic-chrome 浏览器 profile 缓存（占用大）
.superpowers/         — brainstorm html / state / events
.codex-tasks/         — 214 个 task 目录
.gstack/              — gstack 工具状态
.worktrees/           — 6 个活跃 git worktree（不能删，需 worktree remove）
docs/MMMail.zip       — 127 MB 历史包
docs/MMMail/          — 147 MB 解压副本
docs/assets/          — 72 MB 设计图，部分被 v21-ui-spec 引用
artifacts/            — CI 产物，已 ignore
.tools/               — dependency-check 数据缓存，已 ignore
```

### 0.3 frontend-v2 vs soybean-admin-main

`frontend-v2/` 是 v2.0 mmmail 自研前端，仍是合约 / 工作流测试承载方（`docs/superpowers/progress/v21-implementation-progress.md` 明确）。`soybean-admin-main/` 是 v2.1.x 引入的 admin/工作台前端，由本期 T-1..T-7 / U-1..U-6 全面落地。**两者并存**：frontend-v2 跑契约 + 工作流测试；soybean-admin-main 跑 admin / e2e / coverage。重命名只动 soybean-admin-main，不动 frontend-v2。

### 0.4 引用图（grep "soybean-admin-main" 实测）

| 区域 | 命中数 | 处理 |
|---|---:|---|
| `.github/workflows/ci.yml` | 16 | sed 替换 |
| `scripts/*.sh` + `scripts/release-gate.sh` | ~25 | sed 替换 |
| `docs/v213-closure-spec*.md` + `docs/v212-progress-report.md` | ~40 | sed 替换 |
| `docs/superpowers/**/*.md` | ~30 | sed 替换（仅 path 引用，不动事实记录） |
| `tests/v213-closure-contract.test.mjs` | ≥3 | sed 替换 |
| `docker-compose.yml` / `docker-compose.minimal.yml` | 0 | 无引用 ✅ |
| `backend/**` | 0 | 无引用 ✅ |

精确数以 §3.2 落地阶段 grep 输出为准，本表为 spec 起草时估算。

---

## 1. 目标 / 非目标

### 1.1 目标

1. **零代码丢失**：v2.1.3 收尾期所有未入仓代码（soybean-admin-main / tests / ops / 修改的 frontend-v2 / 修改的 backend）全部进入 main 历史
2. **重命名**：`soybean-admin-main/` → 中性名（默认 `frontend-admin/`，§2.3 备选）
3. **清理**：删除真"残留"（`.tmp` / `.superpowers` / `.codex-tasks` / `.gstack` / `docs/MMMail*` / 顶层空 `frontend/`）
4. **保留路径**：开发期产物（brainstorm / codex tasks / worktree 产物）迁到仓库外本地目录 `../MMMail-dev-archive-$(date +%F-%H%M%S)`，**不入 Git / 不 push**
5. **release-gate 不回归**：执行后 `bash scripts/release-gate.sh --skip 1,5,8` 仍 75s 内绿
6. **完全可逆**：每阶段前打 tag，任意阶段失败 `git reset --hard tag` 回到上一稳态

### 1.2 非目标

- 不改业务代码逻辑（前端 .vue / 后端 .java 一律不动）
- 不重构 docs/ 内容（仅删 MMMail.zip / MMMail/ 两个历史快照）
- 不动 frontend-v2/ 的目录名
- 不动后端 Maven 模块名

---

## 2. 命名决策

### 2.1 候选

| 候选 | 优点 | 缺点 |
|---|---|---|
| `frontend-admin/` | 与 frontend-v2/ 平级，语义清楚 | 与 frontend-v2 容易混淆"哪个是哪个" |
| `mmmail-admin/` | 项目品牌前缀 | 已有 `backend/mmmail-server/`，重复 mmmail 前缀 |
| `admin-web/` | 短 | 不带"前端"语义 |
| `frontend-suite/` | 暗示 suite 工作台 | 抽象 |

### 2.2 决策

**`frontend-admin/`**（默认）。理由：
- 与 `frontend-v2/` 平级，IDE/grep/path 都直观
- "admin" 体现"工作台 + 管理后台"语义（对应 soybean-admin 的产品定位）
- 不带开源项目原名，符合用户要求

如用户偏好其他候选，§3.4 sed 替换脚本只需改一处变量。

### 2.3 frontend-v2 / frontend-admin 角色边界（写入 README）

| 子项目 | 职责 | 测试入口 |
|---|---|---|
| `frontend-admin/` | v2.1.x admin / 工作台 / e2e | `pnpm --dir frontend-admin {test:v212,test:unit,test:component,test:e2e}` |
| `frontend-v2/` | v2 公共合约 / 路由 / 工作流契约 | `pnpm --dir frontend-v2 test` |

---

## 3. 实施阶段（每阶段独立可回退）

### 3.0 前置：baseline tag

```bash
DATE="$(date +%F-%H%M%S)"
BASELINE_TAG="baseline/pre-shipping-cleanup-$DATE"
git tag "$BASELINE_TAG"                         # 当前 main HEAD
git rev-parse HEAD > /tmp/shipping-cleanup-baseline-head.txt
```

说明：
- 不使用 `git stash -u`：当前未跟踪目录含数 GB node_modules / browser profile / task cache，stash 会把依赖缓存写入 Git 对象库。
- 回退点：任何阶段失败需要人工回滚时，使用 `git reset --hard "$BASELINE_TAG"` 前必须先确认不会覆盖用户新改动。

### 3.1 阶段 A — 仓库外本地归档（保开发期产物）

**目的**：把要删的"开发期残留"先移出仓库，保留在同级本地目录；不压缩、不入 Git、不污染 origin。

```bash
DATE="${DATE:-$(date +%F-%H%M%S)}"
ARCHIVE_ROOT="../MMMail-dev-archive-$DATE"
mkdir -p "$ARCHIVE_ROOT"

printf '%s\n' \
  "source=$(pwd)" \
  "created_at=$(date -Is)" \
  "baseline=$(git rev-parse HEAD)" \
  > "$ARCHIVE_ROOT/MANIFEST.txt"

for path in .tmp .superpowers .codex-tasks .gstack artifacts frontend docs/MMMail docs/MMMail.zip; do
  if [[ -e "$path" ]]; then
    mkdir -p "$ARCHIVE_ROOT/$(dirname "$path")"
    mv "$path" "$ARCHIVE_ROOT/$path"
  fi
done
```

**验收**：
- `test -f "$ARCHIVE_ROOT/MANIFEST.txt"`
- `du -sh "$ARCHIVE_ROOT"` 显示归档实际占用
- 仓库根不再存在 `.tmp/ .superpowers/ .codex-tasks/ .gstack/ artifacts/ frontend/ docs/MMMail docs/MMMail.zip`

**回退**：把 `"$ARCHIVE_ROOT"` 内对应路径 `mv` 回仓库根。

### 3.2 阶段 B — 引用图实测

```bash
# B.1 输出精确引用清单
rg -n "soybean-admin-main" . \
  -g '*.{md,sh,mjs,ts,yml,yaml,json,xml,java}' \
  -g '!node_modules/**' \
  -g '!target/**' \
  -g '!dist/**' \
  -g '!.git/**' \
  > /tmp/cleanup-refs-soybean.txt

# B.2 输出物理大小排序
du -sh .tmp .superpowers .codex-tasks .gstack docs/MMMail.zip docs/MMMail frontend/ artifacts/ 2>/dev/null > /tmp/cleanup-sizes.txt || true

# B.3 worktree 清单
git worktree list > /tmp/cleanup-worktrees.txt
```

**验收**：三个清单非空、无 syntax 错误。

**回退**：N/A（只读步骤）

### 3.3 阶段 C — .gitignore 扩张

```gitignore
# 追加到根 .gitignore（按段加，保留现有）

# Dev-only artifacts (kept outside the repository in MMMail-dev-archive-*)
.tmp/
.superpowers/
.codex-tasks/
.gstack/
.worktrees/
.claude/worktrees/

# Frontend
frontend-admin/node_modules/
frontend-admin/dist/
frontend-admin/coverage/
frontend-admin/test-results/
frontend-admin/playwright-report/
soybean-admin-main/node_modules/
soybean-admin-main/dist/
soybean-admin-main/coverage/

# Legacy (frontend/ 已废弃)
frontend/

# Historical snapshots
docs/MMMail.zip
docs/MMMail/
```

**验收**：`git check-ignore -v .tmp/ .superpowers/ frontend/` 全部命中

**回退**：`git checkout main -- .gitignore`

### 3.4 阶段 D — 重命名（含引用替换）

**关键约束**：soybean-admin-main 是 untracked 目录，**git mv 不可用**。流程：

```bash
# D.1 物理重命名（在 untracked 状态下做）
mv soybean-admin-main frontend-admin

# D.2 引用替换（基于 §3.2 清单；先 dry-run 后落盘）
RENAME_FROM="soybean-admin-main"
RENAME_TO="frontend-admin"

# D.2.a Dry-run
rg -l "$RENAME_FROM" . \
  -g '*.{md,sh,mjs,ts,yml,yaml,json,xml,java}' \
  -g '!node_modules/**' \
  -g '!target/**' \
  -g '!.git/**' \
  -g '!.claude/**' \
  -g '!.worktrees/**' \
  -g '!.codex-tasks/**' \
  -g '!.tmp/**' \
  -g '!.superpowers/**'

# D.2.b 实际替换（仅在确认 dry-run 列表合理后执行）
rg -l "$RENAME_FROM" . \
  -g '*.{md,sh,mjs,ts,yml,yaml,json,xml,java}' \
  -g '!node_modules/**' \
  -g '!target/**' \
  -g '!.git/**' \
  -g '!.claude/**' \
  -g '!.worktrees/**' \
  -g '!.codex-tasks/**' \
  -g '!.tmp/**' \
  -g '!.superpowers/**' \
  | xargs -r perl -0pi -e "s|$RENAME_FROM|$RENAME_TO|g"

# D.3 路径敏感：检查 docker-compose / pnpm-workspace 没有依赖路径
grep -rn "$RENAME_TO\|$RENAME_FROM" docker-compose*.yml \
  frontend-admin/pnpm-workspace.yaml 2>/dev/null
```

**关键人审点**：
- `.codex-tasks/` 内文件**不替换**（archive 分支已封存原始路径，避免与历史不一致）—— `--exclude-dir=.codex-tasks` 已被根 .gitignore 排除
- `docs/superpowers/` 是事实记录，**只替换路径引用**，不重写历史描述

**验收**：
- `rg -n "soybean-admin-main" docs scripts tests .github frontend-v2 README.md CONTRIBUTING.md -g '!docs/v212-shipping-cleanup-spec.md' -g '!node_modules/**' -g '!.git/**' -g '!.claude/**' -g '!.worktrees/**'` 返回 0 行
- `frontend-admin/` 目录存在
- `bash scripts/release-gate.sh --only 6,7,9,11,12` 退出 0（不依赖 docker / mvn）

**回退**：
- 物理回退：`mv frontend-admin soybean-admin-main`
- 文本回退：`git checkout "$BASELINE_TAG" -- .` 后重做
- 完全回退：确认不会覆盖用户新改动后，`git reset --hard "$BASELINE_TAG"`

### 3.5 阶段 E — 物理清理

```bash
# E.1 worktree 决策（只处理仓库内 .worktrees/，不动仓库外 release worktree 和 .claude/worktrees）
git worktree list
# 对每个 .worktrees/<name>：
git -C .worktrees/<name> status --short
git worktree remove .worktrees/<name>     # 仅限干净 worktree
# 有未提交的 repo-local worktree：先移入 "$ARCHIVE_ROOT/.worktrees/"，再 git worktree prune

# E.2 物理删除：阶段 A 已 mv 到外部归档；这里仅确认残留不存在
test ! -e .tmp
test ! -e .superpowers
test ! -e .codex-tasks
test ! -e .gstack
test ! -e docs/MMMail.zip
test ! -e docs/MMMail
test ! -e frontend
```

**保护项**（绝不删）：
- `.git/` `.github/` `.gitignore` `.dockerignore`
- `frontend-admin/` `frontend-v2/` `backend/`
- `docs/`（除 §3.5 列举两项）
- `scripts/` `contracts/` `config/` `ops/` `tests/`
- `docker-compose*.yml` `.env.example`

**验收**：
- `du -sh .` 总大小较 baseline 降幅 ≥ 274 MB（MMMail* 274 MB + 其他）
- 仓库根 `ls -la` 不含 §0.2 残留目录
- `git worktree list` 不再包含仓库根 `.worktrees/` 路径；仓库外 release worktree / `.claude/worktrees` 不属于本 spec 删除目标

**回退**：确认不会覆盖用户新改动后，`git reset --hard "$BASELINE_TAG"`；开发期产物从 `"$ARCHIVE_ROOT"` 移回对应路径。

### 3.6 阶段 F — 首次纳入前端代码

```bash
# F.1 确认 .gitignore 配置正确（避免 add 进 node_modules）
git check-ignore -v frontend-admin/node_modules/some-file
git check-ignore -v frontend-admin/dist/anything

# F.2 add tracked 目标
git add frontend-admin/
git add tests/
git add ops/
git add .gitignore
git add .github/ scripts/ docs/ CHANGELOG.md README.md CONTRIBUTING.md
git add -u         # 只拾取 tracked 文件的修改/删除，不纳入未知残留

# F.3 检视 staged 大小
git diff --cached --stat | tail -5

# F.4 sanity check：确认未夹带 secrets
git diff --cached -- '*.env*' '*secret*' '*key*'   # 只允许示例文件/文档占位，不允许真实 secret

# F.5 commit
git commit -m "chore(repo): rename admin frontend and prune dev artifacts"
```

**验收**：
- `git ls-files frontend-admin/ | wc -l` 数量与原 soybean-admin-main 物理文件数（除 node_modules / dist / coverage）一致
- `git ls-files tests/ | wc -l` ≥ 1（至少含 v213-closure-contract）
- `git ls-files ops/ | wc -l` ≥ 1
- `git status --short` 不含待提交的源码/测试/文档；允许外部归档目录不在仓库内

**回退**：`git reset --soft HEAD~1`（保 working tree） 或 `git reset --hard HEAD~1`（彻底回退）

### 3.7 阶段 G — release-gate 全量回归

```bash
sg docker -c 'bash scripts/release-gate.sh'
```

**期望**：12 步全绿。

如失败：
- step 4 oxfmt 失败 → 重命名后 oxfmt 命中新路径，需 `pnpm --dir frontend-admin exec oxfmt --write` 单独提交
- step 6/7 失败 → 检查 `pnpm-workspace.yaml` / `vitest.config.ts` 内的 alias 是否漏改
- step 12 失败 → 重命名 sed 误伤了迁移文件路径

**回退**：每 step 独立日志在 `/tmp/release-gate-*.log`，按 spec v1.1 §2.2 排查

### 3.8 阶段 H — 文档收尾

更新引用：
- `docs/v212-progress-report.md` 末尾追加 §9：「v2.1.2 上线清理完成（重命名 + 残留治理 + 首次入仓）」
- `CHANGELOG.md` 追加：「Renamed soybean-admin-main → frontend-admin; moved dev-only artifacts to a local filesystem archive; ingested v2.1.3 closure code into main history.」
- `README.md` / `CONTRIBUTING.md` 出现 soybean-admin-main 处全部更新
- 本 spec frontmatter `status: implemented` + commit SHA 备注

---

## 4. 验收（汇总）

- [ ] §3.0 baseline tag 存在（`git tag | grep baseline/pre-shipping-cleanup`）
- [ ] §3.1 仓库外本地归档存在且含 `MANIFEST.txt`
- [ ] §3.4 grep "soybean-admin-main" 在活动工程文件（除本 spec 历史说明 / archive / node_modules）= 0
- [ ] §3.5 残留目录全清，`du -sh .` 降幅 ≥ 274 MB
- [ ] §3.6 `git ls-files frontend-admin/` ≥ 200 个文件（实际原 soybean-admin-main 工程文件量）
- [ ] §3.7 release-gate 全 12 步绿（带 docker + mvn 环境）
- [ ] §3.8 文档全部更新

---

## 5. 风险登记

| 编号 | 风险 | 触发 | 缓解 |
|---|---|---|---|
| C-1 | sed 替换误伤 | 路径含 `soybean-admin-main` 的字面常量被改 | dry-run 强制人审；`--exclude-dir=.codex-tasks/.tmp/.superpowers` |
| C-2 | 本地归档误入仓 | 显式 add archive 路径 | 归档放仓库外 `../MMMail-dev-archive-*`，不在 `git status` 范围内 |
| C-3 | node_modules 误入仓 | .gitignore 漏配 | §3.6 F.3 `git diff --cached --stat` 阈值告警（> 50 MB 即停） |
| C-4 | worktree 强删丢未提交工作 | 6 个 worktree 状态未知 | §3.5 E.1 单独 review；archive 已保所有 untracked |
| C-5 | release-gate 路径硬编码 | step 6/7/9 内 pnpm --dir 旧名 | §3.4 D.2 sed 覆盖 ci.yml + scripts/release-gate.sh + 所有 .sh |
| C-6 | docs/superpowers 历史描述被 sed 误改 | 事实记录里出现"soybean-admin-main"是历史事实 | docs/superpowers 内只替换 path-like 引用（带 `/` 前后缀）；纯文字事实保留 |
| C-7 | .env.test 含 secrets 被提交 | 显式 `git add` 漏审 | `git diff --cached -- '*.env*' '*secret*' '*key*'` 必查；真实 secret 不允许进入提交 |
| C-8 | docs/MMMail/ 被某文档引用 | 引用图 §3.2 漏检 | §3.5 E.2 删前 `grep -r "MMMail.zip\|docs/MMMail/" docs/ --exclude-dir=MMMail` 必须 0 |

---

## 6. 三轮内审（spec 起草过程，留存）

### 6.1 第一轮（v0.draft → v0.5）

发现：
- v0 用单个 shell 脚本一次性执行，缺断点；改为 8 阶段，每阶段独立可回退
- v0 未识别 soybean-admin-main 是 untracked，假设可 git mv；改为 §3.4 物理 mv
- v0 未识别 `.worktrees/` 占用，rm -rf 会破坏 git；增 §3.5 E.1 worktree remove

### 6.2 第二轮（v0.5 → v0.9）

发现：
- v0.5 未列引用图实测命令，sed 范围不清；增 §3.2 阶段 B
- v0.5 dev-archive 未防误推 origin；v1.1 改为仓库外本地归档并更新 §5 C-2
- v0.5 未识别 frontend/ 也是空骨架（400 MB node_modules）；§0.1 表明确，§3.5 E.2 显式 rm -rf

### 6.3 第三轮（v0.9 → v1.0）

发现：
- v0.9 未做"frontend-v2 vs frontend-admin 角色边界"说明，新人会困惑；增 §0.3 + §2.3
- v0.9 §3.4 sed 没排除 .codex-tasks（214 个目录会被误改）；加 `--exclude-dir`
- v0.9 §5 风险表只有 C-1..C-5；增 C-6（docs/superpowers 事实保护）/ C-7（.env.test secrets）/ C-8（MMMail 引用反查）
- v0.9 §3.6 F.4 缺 secret sanity check；补上

---

## 7. 三轮复查（spec 完成后，凭实测）

### 7.1 复查 1 — 数字对账

实测命令对照（执行时间 spec 起草日）：

| spec 断言 | 命令 | 期望 | 实测 |
|---|---|---:|---:|
| `soybean-admin-main` tracked = 0 | `git ls-files soybean-admin-main/ \| wc -l` | 0 | 0 ✅ |
| `frontend/` tracked = 0 | `git ls-files frontend/ \| wc -l` | 0 | 0 ✅ |
| `frontend-v2` tracked = 234 | `git ls-files frontend-v2/ \| wc -l` | 234 | 234 ✅ |
| modified 数 | `git status --short \| wc -l` | ~560 | 560 ✅ |
| docs/MMMail* 大小 | `du -sh docs/MMMail*` | 274 MB | 127M+147M ✅ |
| worktree 数 | `git worktree list \| wc -l` | 6+1 | （需运行时校验） |
| 引用 soybean-admin-main 命中 | `grep -rln "soybean-admin-main" --include="*.md" docs/` | ~40 | （阶段 B 输出） |

**结论**：起草期所有数字与 git/du 输出一致；运行时数字（worktree / grep 命中）由阶段 B 实测落档。

### 7.2 复查 2 — 可逆性矩阵

| 阶段 | 可逆动作 | 回退命令 | 回退后状态等价于 |
|---|---|---|---|
| 3.0 baseline tag | 打 tag | `git tag -d baseline/pre-shipping-cleanup-$DATE` | tag 不存在 |
| 3.1 local archive | 仓库外 mv | `mv ../MMMail-dev-archive-$DATE/<path> <path>` | 路径恢复 |
| 3.2 引用图 | 只读 | N/A | — |
| 3.3 .gitignore | 改 1 文件 | `git checkout main -- .gitignore` | 原 .gitignore |
| 3.4 重命名 | mv + sed | `mv frontend-admin soybean-admin-main && git checkout -- .` | 原状（前提：sed 改的全是 tracked 文件） |
| 3.5 物理清理 | 外部归档 + repo-local worktree remove | 从 `../MMMail-dev-archive-$DATE` 移回对应路径 | 数据不丢 |
| 3.6 首次入仓 | git add + commit | `git reset --soft HEAD~1` 或 `--hard` | 入仓前 |
| 3.7 release-gate | 只读 | N/A | — |
| 3.8 文档 | 改文本 | `git checkout HEAD~1 -- docs/ CHANGELOG.md README.md` | 改前 |

**关键**：3.4 是不可逆里风险最高的一步（mv + sed 同时做）。缓解：
- mv 前打 tag `baseline/pre-rename`
- sed 仅作用于 tracked 文件（`grep --exclude-dir=...`）
- 失败可 `git checkout baseline/pre-rename -- .` 恢复 tracked 部分，物理 mv 回退

### 7.3 复查 3 — release-gate 兼容矩阵

每个阶段对 release-gate 12 step 的影响：

| step | 命令 | 阶段 A | 阶段 D | 阶段 E | 阶段 F |
|---|---|:-:|:-:|:-:|:-:|
| 1 docker-group | `check-docker-group.sh` | ✅ | ✅ | ✅ | ✅ |
| 2 typecheck | `pnpm --dir frontend-admin typecheck` | ⚠ 旧名 | ✅ 改名后 | ✅ | ✅ |
| 3 lint | `pnpm --dir frontend-admin lint` | ⚠ 旧名 | ✅ | ✅ | ✅ |
| 4 fmt | `oxfmt --check` | ✅ | ✅ | ✅ | ✅ |
| 5 backend tests | `run-tests-docker.sh backend` | ✅ | ✅ | ✅ | ✅ |
| 6 v212 contract | `pnpm --dir frontend-admin test:v212` | ⚠ 旧名 | ✅ | ✅ | ✅ |
| 7 coverage | `pnpm --dir frontend-admin test:coverage` | ⚠ | ✅ | ✅ | ✅ |
| 8 e2e | `run-tests-docker.sh e2e` | ⚠ 内部引用 | ✅ | ✅ | ✅ |
| 9 style-discipline | `pnpm --dir frontend-admin check:style-discipline` | ⚠ | ✅ | ✅ | ✅ |
| 10 bundle-budget | `pnpm --dir frontend-admin check:bundle-budget` | ⚠ | ✅ | ✅ | ✅ |
| 11 i18n | `pnpm --dir frontend-admin check:i18n` | ⚠ | ✅ | ✅ | ✅ |
| 12 migration | `check-migration-naming.sh` | ✅ | ✅ | ✅ | ✅ |

阶段 A（建 archive）期间脚本仍指向旧名 → A 完成后立即进入 D 修复。窗口约 5 min。CI 期间不重叠（main 上单线性 commit）。

---

## 8. 一句话目标

**用 1 个原子 commit 把 v2.1.3 全部代码入仓 + soybean-admin-main 改名为 frontend-admin + 删除 274 MB+ 残留 + 把开发产物移到仓库外本地归档，三轮内审三轮复查保证全程可回退、release-gate 全 12 步不退化。**
