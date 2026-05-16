# AGENTS.md

本文档定义 Codex 在 MMMail 仓库内工作的本地规范。它只记录本仓库真实存在的边界、入口和门禁，不复制个人全局规则或外部 skill 仓库说明。

## 适用范围

- 当前仓库：`MMMail`
- 当前公开协作基线：`v2.1.2-shipping-clean`
- 当前执行规格入口：`docs/v22-open-source-commercial-spec.md`
- 产品前端：`frontend-admin`
- legacy `frontend-v2`：只作为冻结历史参考，不允许新增或修改文件，不承载新产品功能或 CI migration signal
- 后端：`backend/mmmail-server`，Spring Boot + Flyway + Maven
- 运行与发布脚本：`scripts/`
- CI 入口：`.github/workflows/ci.yml`

## 仓库定位

MMMail 是一个开源可自托管的邮件、日历、云盘、密码库与协作套件仓库。当前主线目标是把公开 Free 版本、自托管安装路径、v2.1.2 清理终态和 v2.2 开源商业化边界稳定下来。

权威文档优先级：

1. `README.md`
2. `docs/v22-open-source-commercial-spec.md`
3. `docs/v22-completion-audit.md`
4. `docs/v22-external-evidence-checklist.md`
5. `docs/release/v2-support-boundaries.md`
6. `docs/open-source/module-maturity-matrix.md`
7. `docs/frontend/v22-frontend-topology-audit.md`
8. `docs/open-source/i18n-governance.md`
9. `docs/ops/install.md`、`docs/ops/upgrade.md`、`docs/ops/backup-restore.md`、`docs/ops/runbook.md`
10. `SECURITY.md`、`SUPPORT.md`、`GOVERNANCE.md`、`MAINTAINERS.md`

如果这些文档冲突，优先修正旧文档，而不是在代码里增加兼容绕行。

历史文档边界：

- `docs/superpowers/`、旧 `docs/v21*` / `docs/v212*` spec、旧 `docs/release/v2.0.*` release notes 是历史执行记录或历史发布记录。
- 这些历史文档中出现的 `frontend-v2` 命令、运行时描述或验收口径不代表当前产品入口、当前 CI 门禁或当前发布流程。
- 新实现、新验证和新文档必须以 `frontend-admin`、`docs/frontend/v22-frontend-topology-audit.md`、`docs/frontend/v22-frontend-convergence-decision.md` 和 `scripts/validate-legacy-frontend-v2-freeze.sh` 为准。

## 工作原则

- 先读当前任务相关文件，再改代码或文档。
- 优先复用仓库已有服务、控制器、测试、脚本和文档结构。
- 不为通过测试引入 silent fallback、fake success、mock payment、伪 license 成功路径或吞异常成功。
- 不新增与当前需求无关的抽象、兼容层、临时 cap 或隐藏开关。
- 失败必须显式暴露：错误码、日志、测试失败或文档中的明确限制。
- 变更公开能力、安装方式、安全边界、前端拓扑或商业化口径时，必须同步文档和门禁。
- 当前工作树可能已有用户改动；不要还原未确认属于自己的变更。

## 前端边界

- `frontend-admin` 是唯一产品前端，所有新 UI、路由、i18n、e2e、bundle、style discipline 都应落在这里。
- legacy `frontend-v2` 只保留为冻结历史参考；已迁移的契约由 `frontend-admin/tests` 或根目录 `tests` 承载，只允许删除文件或迁出历史材料。
- 不要把 `frontend-v2` 重新加入 compose、产品安装文档、发布制品、主 release-gate typecheck/lint/format。
- 不要重新引入已退休的 legacy migration signal 或 CI legacy migration job；只保留 freeze gate 阻断旧前端新增和修改。
- `.github/dependabot.yml` 对 `frontend-v2` 只保留依赖告警，不自动开启版本更新 PR，避免绕过 legacy freeze 边界。
- 前端样式必须服从当前 `frontend-admin` 视觉系统，不引入与现有 shell、导航、卡片密度和 i18n 行为冲突的页面。
- `frontend-admin/.env` 与 `frontend-admin/.env.test` 只承载非敏感 Vite 构建默认值，必须跟随源码进入 CI 和 Docker fresh build；真实 secret 仍只能通过环境变量、secret file 或根目录本地 ignored env 注入。
- `frontend-admin` 的 OpenAPI 类型生成必须在同一脚本内完成格式化，避免 CI diff guard 依赖开发者手动运行 formatter。

## 后端边界

- 后端公开 API、权限、租户、商业化门控、迁移脚本必须有自动化回归。
- Flyway 迁移版本必须全局唯一且连续；历史允许缺口只能写入 `scripts/.migration-naming-allowlist`。
- 涉及安全、上传、分享、管理员、license、billing、edition、entitlement 的改动，必须保持失败显式可见。
- Spring 管理组件优先使用单一构造器注入；如果因测试或边界适配保留多个构造器，运行时构造器必须显式 `@Autowired`，避免启动时被 Spring 选错。
- 后端单元或集成测试在本地执行时使用 `timeout 60s` 包裹，避免卡死。
- 数据库、缓存、Nacos、JWT、billing webhook 等 secret 只通过环境变量或 secret file 注入，不能写入源码。

## 状态同步规则

- 任一能力从 missing 变为 partial done / done 时，必须同步 `docs/v22-open-source-commercial-spec.md`、`AGENTS.md`、相关运行文档、`scripts/validate-local.sh`、CI workflow 和 root contract test。
- 新增后端 contract test 时，必须同时确认本地门禁与 CI 至少有一个明确入口；商业化、企业准入、合规、可观测 contract 不允许只存在于测试目录。
- 状态文案只能写已经有代码、文档和门禁证据的部分；剩余项必须落到具体可验收边界，例如 live IdP e2e、真实 tag digest、外部仓库签发，而不是继续使用泛化“未完成”描述。
- 如果规范文档和代码门禁冲突，先修规范漂移和合约测试，再扩大实现范围。
- 版本化 spec、release audit、验收矩阵类文档允许超过常规文件行数建议；不要为满足行数而拆散单一权威规格，但必须保留 frontmatter、变更历史、review pass、工作包、验收门禁和剩余边界。
- 新代码质量上限：函数默认不超过 50 行，常规源码文件默认不超过 500 行；超过时先拆分职责或提取 helper。
- 已存在的 oversized legacy 文件、生成类型、lockfile、历史 spec 和 release evidence 可以作为迁移债务保留，但不得继续扩大已超限文件；触及这些文件时优先只做局部必要变更，并把新增逻辑落到更小的模块。
- 活跃源码文件行数由 root governance contract 使用显式 allowlist 约束；新增超限文件、扩大未列入 allowlist 的超限文件，或把新逻辑塞回历史超限文件，都应先拆分职责。
- 共享 agent 规则只写入本文件；本地 agent / validation 产物（如 `.claude/`、`.codex-tasks/`、`.superpowers/`、`.tmp/`、`.tools/dependency-check-data/`）必须保持 ignored，不作为仓库规范来源。

## 商业化边界

- Free / Pro / Business / Hosted / Enterprise 的承诺以 `docs/v22-open-source-commercial-spec.md` 和 `docs/open-source/module-maturity-matrix.md` 为准。
- 已落地能力可以标为 `Partial Done` 或 `Done`；未接入真实后端、真实支付、真实 license 校验或真实门禁的能力不能宣传为可用。
- `none` billing provider 不得授予 paid success。
- webhook、license、edition、entitlement 失败必须带稳定原因或错误码，不能回退成成功。
- Helm chart / image publishing / SLI/SLO docs / audit export / DSR inventory / OpenTelemetry runtime tracing / OIDC token/session 主仓基线 / OIDC callback 专项 trace 已有代码、文档和门禁；live Keycloak login / callback / session / logout / token refresh e2e、真实支付 provider 和独立商业仓库仍按 spec 拆分推进，不在社区代码里伪造完成状态。

## 本地验证矩阵

按改动范围选择最小但足够的验证命令。声明通过前必须真的运行并读取退出码。

| 改动范围 | 最低验证 |
|---|---|
| 根目录治理、文档、脚本 | `node --test tests/v22-repository-governance-contract.test.mjs tests/v22-repository-governance-validation-contract.test.mjs` |
| 任意仓库治理或合同测试 | `node --test tests/*.test.mjs` |
| secret / env / 安全模板 | `bash scripts/validate-security.sh` 或 `bash scripts/security-secret-scan.sh` |
| Flyway 迁移 | `bash scripts/check-migration-naming.sh` |
| 后端定向改动 | `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=<TestNames> -Dsurefire.failIfNoSpecifiedTests=false test` |
| 后端编译边界 | `mvn -f backend/pom.xml -pl mmmail-server -am -DskipTests compile` |
| 产品前端改动 | `pnpm --dir frontend-admin typecheck`、相关 `test:*` 或 `check:*` |
| i18n 改动 | `pnpm --dir frontend-admin check:i18n` |
| legacy 前端冻结 | `bash scripts/validate-legacy-frontend-v2-freeze.sh` |
| 供应链 / SBOM | `node scripts/generate-sbom-license-report.mjs` |
| Helm chart | `bash scripts/validate-helm-chart.sh` |
| 镜像发布 workflow | `node --test tests/v22-image-publishing-contract.test.mjs` |
| 商业公开边界 | `node --test tests/v22-commercial-boundaries-contract.test.mjs` |
| 商业后端 surface / 权益门控 | `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22CommercialSurfaceCoverageContractTest -Dsurefire.failIfNoSpecifiedTests=false test` |
| 审计 JSONL 导出 | `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22AuditExportContractTest -Dsurefire.failIfNoSpecifiedTests=false test` |
| DSR / data inventory | `node scripts/validate-dsr-inventory.mjs`、`timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22DsrContractTest -Dsurefire.failIfNoSpecifiedTests=false test` |
| SLI/SLO 文档 | `node --test tests/v22-observability-docs-contract.test.mjs` |
| OpenTelemetry runtime tracing | `timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22OpenTelemetryContractTest -Dsurefire.failIfNoSpecifiedTests=false test` |
| v2.2 外部证据验收 | 手动运行 `bash scripts/validate-v22-external-evidence.sh`；外部证据未齐时必须明确失败，不进入默认绿色门禁，也不得通过 root tests 间接执行 |
| 发布候选 | `bash scripts/release-gate.sh`，CI/release candidate 不允许 skip，且必须跑 legacy frontend freeze |
| 全量本地门禁 | `bash scripts/validate-local.sh` |

`scripts/validate-local.sh` 是本地总门禁，CI 中由 `scripts/validate-ci.sh` 接入。不要把只在本机“能跑”的临时条件写进 CI 或 release gate。

## Git 提交规范

### 提交前检查

- 提交前必须运行与本次改动直接相关的验证命令。
- 涉及前端路由、状态、共享组件、样式系统或构建配置时，至少运行对应测试、`typecheck` 和相关 `check:*`。
- 只有在验证命令退出码为 0 后，才允许声明“测试通过”“构建通过”或执行提交。
- 提交前必须执行 `git status --short --branch`，区分本次改动和已有改动。
- 只暂存本次任务相关文件；禁止使用 `git add .`、`git add -A`。
- 当前工作树存在无关未跟踪文件时，忽略它们，除非用户明确要求纳入提交。

### Commit Message

统一使用 Conventional Commits：

```text
<type>(<scope>): <subject>

<body>

<footer>
```

- `type` 使用 `feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`chore`、`ci`、`revert`。
- `scope` 优先用现有技术域，如 `backend`、`frontend-admin`、`commercial`、`docs`、`ci`、`security`。
- `subject` 默认英文简短动宾短语，不加句号。
- 涉及不兼容变更时，在 footer 写 `BREAKING CHANGE:` 并说明迁移方式。
- PR commit 需要 `Signed-off-by: Name <email>`；`DCO.md` 解释签署含义，DCO workflow 会阻断缺失签名的 PR。

### 提交粒度

- 一个提交只表达一个完整语义切片。
- 不把格式化、重构、功能、测试混在一个提交里，除非它们不可拆分。
- 文档、测试、源码可以同提交，但必须服务于同一个目标。
- 不提交构建产物、临时文件、压缩包、截图缓存、`.tmp/`、任务追踪目录或依赖目录。

推荐流程：

```bash
git status --short --branch
<run verification commands>
git add <only-related-files>
git diff --cached --check
git diff --cached --stat
git commit -m "<type>(<scope>): <subject>"
git status --short --branch
```

未经用户明确要求，不要 push、创建远程分支、改写历史、执行 `git reset --hard` 或强制清理。

## 代码和文档要求

- 遵守 SOLID、DRY、分层清晰和 YAGNI。
- 保持函数、文件和测试聚焦；新增复杂逻辑先写定向测试。
- 结构化数据优先用结构化解析，不用脆弱字符串拼接。
- Markdown 变更要同步相关链接、门禁命令和状态表。
- 公开文案不能夸大未交付能力；Preview/Beta/Partial 必须明确写清限制。
- 示例配置只能使用 `replace-with-*` 或明显无效的占位值。
- 文档中的安装命令应指向 `frontend-admin`、`backend`、`docker-compose.yml` 或 `docker-compose.minimal.yml` 的真实路径。
- 新增活跃源码文件默认必须低于 500 行；如果改动触及已 allowlist 的历史超限文件，要把新增职责拆到新模块，并让治理测试继续说明例外原因。

## Skill 使用

如果当前 Codex 环境安装了 task、SDLC、frontend、testing 或 documentation 相关 skill，可以按用户任务选择最小必要 skill。skill 只提供方法论；本文件中的 MMMail 仓库边界、前端拓扑、门禁和 Git 规范优先。

使用原则：

- 一次选择 1 个主 skill，最多补 1-2 个辅助 skill。
- 不为了“显得完整”加载无关 skill。
- 如果 skill 建议与本文件冲突，按本文件执行，并把冲突反馈到最终说明。
- 涉及 UI 实现时，遵守 `frontend-admin` 的当前设计系统和本仓库前端边界。
- 涉及完成声明、提交、发布或修复声明时，必须先运行验证命令。

## 明确禁止

- 新增或修改 `frontend-v2` 文件；该目录只允许删除文件或迁出历史材料。
- 为测试通过新增 mock success、fake billing paid state、伪 license 成功、silent fallback 或吞错成功。
- 在源码、示例、测试夹具中写入真实 secret。
- 让 CI 或 release candidate 使用 skip、only 或本地诊断开关。
- 将商业、Hosted、Enterprise、SLA、支付、license、Helm、镜像发布状态宣传为已完成，除非对应代码、文档和门禁都存在。
- 为无关任务重排大面积格式、改名或目录结构。
