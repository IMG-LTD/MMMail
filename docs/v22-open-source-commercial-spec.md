---
name: v2.2 开源 + 商业化筹备 spec
date: 2026-05-16
spec_version: oss-comm-v1.79
based_on:
  - docs/v213-closure-spec-v1.1.md (implemented)
  - docs/v212-shipping-cleanup-spec.md (implemented)
  - docs/v212-migration-spec.md (implemented baseline / still relevant for frontend topology)
  - docs/open-source/module-maturity-matrix.md
  - docs/security/threat-model.md
  - docs/v214-roadmap.md (draft)
status: main-repo-implemented-external-evidence-required
release_gate: v2.2 OSS-launch + commercial-MVP + frontend-convergence
risk_level: HIGH (新增商业化、法务、合规、支付边界、前端拓扑收敛，非纯工程)
target_audience: 个人维护者（BDFL）/ 早期合作伙伴 / 首批自托管商业用户 / 外部贡献者
scope:
  - MMMail 主仓公开可交付部分
  - 独立计费仓库的接口边界与验收契约
  - v2.2 前端拓扑收敛和唯一发布入口判定
  - v2.2 release gate、CI、文档与治理补强
non_goals:
  - 不在主仓实现 Adapay SDK、商户进件、真实扣款、退款、发票、私钥签发
  - 不承诺 24/7 SLA、SOC2/ISO 认证、多区域数据驻留
  - 不把 v2.1.2 已 GA 能力从 Free 挪到收费档
  - 不在 v2.2 之后继续把 `frontend-admin` 与 `frontend-v2` 都当成同等产品入口维护
  - 不为通过测试而引入静默 fallback、mock 支付成功、伪 license 成功路径
iteration_history:
  - v0.draft 一锅烩列 80 条待办（弃，没分层、没优先级、没估算）
  - v0.5 按 OSS / 商业化二分（弃，遗漏合规、生态、运营）
  - v1.0 假设两人团队 + Stripe 国际首发 + 公司化治理（弃）
  - v1.1 改为个人开发者主体 / Free-Pro-Business / 中国大陆首发 / 独立计费仓库 / BDFL 治理
  - v1.2 清除旧版 Community/Enterprise/Stripe/SaaS-only 残留，补齐验收门禁、风险和子 spec 拆分
  - v1.3 补充双前端拓扑审计，新增 frontend convergence P0
  - v1.4 不限制 500 行，按工作包把每个 P0 拆到输入、输出、步骤、验收、门禁
  - v1.5 修正未来日期、Flyway 编号冲突、FE-02 品牌验收范围和 Sprint 1 checkpoint 口径
  - v1.6 当前版：同步仓库规范落地状态，补充治理文件、唯一产品前端、legacy 前端迁移信号和 release-gate 收敛结果
  - v1.7 补充根级仓库规范：SUPPORT.md、.editorconfig、.gitattributes、i18n 治理前端归属和治理 contract gate
  - v1.8 修正项目级 AGENTS.md：从外部 skill 仓库口径改为 MMMail 真实仓库边界、唯一产品前端、commercial gate 和验证矩阵
  - v1.9 同步 COMM-04 backend entitlement enforcement 落地状态：edition context resolver、annotation interceptor、失败审计和 commercial backend contract gate
  - v1.10 补充 COMM-04 复查后的实现细节：商业化授权 gate 从通用拦截器抽离，Spring 构造器注入入口显式化，license 篡改负例改为确定性字节篡改
  - v1.11 同步 COMM-05 首批落地：license status/upload API、Settings License 面板、Admin Billing 外部计费状态和 EntitlementGate 后端授权详情展示
  - v1.12 同步 COMM-05 OIDC setup entry：Admin 入口通过 EntitlementGate 按 `oidc.sso` Business 权益拦截，不提供 mock 配置成功路径
  - v1.13 同步 COMM-05 Playwright axe gate：登录、注册、license、billing、OIDC blocked state 已有浏览器级严重 a11y 回归覆盖，并修复测试暴露的登录页和 Admin 可访问性问题
  - v1.14 同步 GATE-01 supply-chain gate：新增 CycloneDX SBOM、dependency license report、SPDX 摘要生成脚本，并接入 validate-local、release-gate 和 CI artifact
  - v1.15 同步仓库规范复查：修正 release-gate supply-chain 步骤口径、SBOM 状态、PR 模板 required file 保护和 legacy frontend-v2 治理注释
  - v1.16 同步 DEP-01 Helm chart：新增 `helm/mmmail` chart、Helm 安装文档、`validate-helm-chart.sh`、CI Helm setup 和 release-gate `helm-lint`
  - v1.17 同步 DEP-02 image publishing：新增 GHCR buildx multi-arch workflow、image digest release-note 段和 release-gate `image-workflow-contract`
  - v1.18 同步 GTM-01 commercial boundaries：新增 pricing/support/trademark 三份公开边界文档，明确无虚假 SLA、无 live payment 声称、Free 自托管不削弱
  - v1.19 同步 OBS-02 SLI/SLO docs：新增 `docs/observability/sli-slo.md`，定义 API p99、5xx、billing webhook、license、OIDC 内部质量目标且明确不是对外 SLA
  - v1.20 同步仓库规范复查：PR/CONTRIBUTING 增加商业、部署、可观测边界检查，并修正已落地 Helm / image / SLI 状态漂移
  - v1.21 同步 BUS-02 audit JSONL export：新增 v2 Business JSONL/SIEM 导出接口、cursor/eventTypes 查询、文档和 backend contract gate
  - v1.22 同步 BUS-03 DSR + data inventory：新增 Business DSR export/erasure job、data inventory 文档、migration inventory gate 和 release-gate step 16
  - v1.23 同步仓库规范复查：部署 runbook、部署拓扑和威胁模型的运行时前端口径统一到 `frontend-admin`，并新增 root governance contract 防回归
  - v1.24 同步 OBS-01 runtime tracing：新增 OpenTelemetry 依赖、运行时 span wrapper、HTTP/DB/Redis/webhook/license span 接入、`docs/observability/opentelemetry.md` 和 `BackendV22OpenTelemetryContractTest`
  - v1.25 同步 BUS-01 OIDC SSO 基线：新增 org OIDC config、single-use auth state、PKCE S256、callback trace、Keycloak/国内 IdP 文档和 `BackendV22OidcSsoContractTest`
  - v1.26 同步仓库规范复查：AGENTS / root governance contract 已把 OIDC SSO 基线和 OIDC callback trace 从未落地项移到有代码、文档和门禁证据的 partial done，剩余边界收窄为 live Keycloak token/session/logout/refresh e2e
  - v1.27 同步 FE-05 legacy frontend freeze：新增 `docs/frontend/v22-frontend-convergence-decision.md` 和 `scripts/validate-legacy-frontend-v2-freeze.sh`，阻断 `frontend-v2` 新增或修改产品文件
  - v1.28 同步 BUS-01 OIDC token/session 主仓基线：callback 已接真实 token exchange client、ID token signature / issuer / audience / nonce 校验、现有用户 session issuance 和 `/api/v2/auth` refresh/logout 会话路径；剩余为 live Keycloak e2e 证据
  - v1.29 同步 FE-03 legacy frontend contract migration：selected legacy contracts 已迁入 `tests/v22-legacy-frontend-contract-migration.test.mjs` 和 `frontend-admin` public share surface，旧迁移信号脚本与 CI legacy migration job 已退休
  - v1.30 同步 COMM-04 commercial API surface coverage：新增 `docs/commercial/edition-entitlement-surface.md` 和 `BackendV22CommercialSurfaceCoverageContractTest`，固定当前 v2.2 Pro/Business API、upgrade path 和外部 webhook 边界
  - v1.31 同步仓库规范复查：AGENTS 明确版本化 spec / release audit / 验收矩阵文档可超过常规行数建议，root governance contract 固定 commercial surface coverage gate
  - v1.32 同步完成审计：新增 `docs/v22-completion-audit.md`，把用户目标、spec gate、仓库证据和外部剩余项映射为可复查清单，并纳入 validate-local required files 与 root governance contract
  - v1.33 同步外部证据清单：新增 `docs/v22-external-evidence-checklist.md`，明确 GitHub private vulnerability reporting、live Keycloak、OIDC trace、image digest 和独立计费仓的真实验收证据
  - v1.34 同步外部证据 verifier：新增 `scripts/validate-v22-external-evidence.sh`，在外部证据未齐时明确失败并列出缺口，不进入默认通过路径
  - v1.35 同步外部 verifier 门禁边界：root governance contract 固定该 verifier 当前应失败且不得被默认 validate-local、CI 或 release-gate 当成绿色门禁执行
  - v1.36 同步贡献入口外部证据规则：CONTRIBUTING 和 PR template 要求把外部项标为 done 前必须附真实证据并记录 verifier 结果
  - v1.37 同步完成状态：frontmatter 从 `implementation-started` 收敛为 `main-repo-implemented-external-evidence-required`，避免把主仓已落地但外部证据未齐的状态写成仍在启动
  - v1.38 同步外部 verifier 覆盖范围：`scripts/validate-v22-external-evidence.sh` 现在也显式报告独立计费仓 / 真实支付 / license signing 证据仍在主仓外，避免商业闭环缺口被 GitHub、Keycloak 和镜像 digest 缺口掩盖
  - v1.39 同步剩余决策状态：原 Sprint 0 决策点已改为当前决策复查表，区分已落地决策、外部证据项和后续 v2.3 / 私有仓边界
  - v1.40 同步外部证据 checklist：`docs/v22-external-evidence-checklist.md` 现在列出当前 verifier 必须报告的 8 个 incomplete markers，避免脚本文档和实际输出漂移
  - v1.41 同步前端拓扑现状叙述：§0.3 不再说 README / CI / release-gate 现在仍把两个前端当产品入口，而是记录历史风险与当前 frontend-admin-only 落地状态
  - v1.42 同步 release-gate skip 防线：root governance contract 现在动态验证 `CI=true` 时 `MMMAIL_SKIP_BACKEND` / `MMMAIL_SKIP_E2E` 会被 `scripts/release-gate.sh` 早期阻断
  - v1.43 同步 edition source precedence：文档和 `BackendV22EditionCoreContractTest` 固定运行时 edition 按 subscription state、active license、workspace fallback 解析，非付费 subscription 不会被 active license 静默覆盖为 paid
  - v1.44 同步完成审计命令表：`docs/v22-completion-audit.md` 现在记录 `BackendV22EditionCoreContractTest` 的 6-test build success 证据，避免 edition source precedence 只存在于实现和局部说明
  - v1.45 同步仓库规范复查：新增根级 `DCO.md`，把 DCO 签署含义、修复命令、README / CONTRIBUTING 导航和 root governance contract 固定下来
  - v1.46 同步仓库规范复查：明确 `docs/superpowers/`、旧 v2.0/v2.1 spec 和历史 release notes 中的 `frontend-v2` 命令只属于归档证据，不代表当前产品入口或发布门禁
  - v1.47 同步外部证据进展：GitHub private vulnerability reporting 已通过 GitHub API 启用并验证，GOV-02 从 partial done 收敛为 done；剩余外部证据缩小到 live Keycloak、真实 image digest 和独立计费仓
  - v1.48 同步仓库规范复查：DCO workflow 改为枚举 PR commits，避免检查 GitHub 生成的 merge commit；legacy `frontend-v2` Dependabot 版本更新 PR 关闭，只保留安全告警
  - v1.49 同步新鲜外部状态核查：本机无可验证 Keycloak/OIDC 环境，远端无 `MMMail Images` workflow 和 GHCR package digest，独立计费仓在当前账号可见范围内仍不可解析
  - v1.50 同步 OTel 文档漂移修复：`docs/observability/opentelemetry.md` 不再把 OIDC callback span 写成 future/pending，改为主仓已接入 `mmmail.oidc.callback`、live trace evidence 仍需外部 Keycloak 验收
  - v1.51 同步 live OIDC 证据模板：新增 `docs/commercial/oidc-live-evidence-template.md`，固定 BUS-01 / OBS-01 / GATE-01 的真实 Keycloak/OIDC 证据包字段和 non-evidence 边界
  - v1.52 同步 DEP-02 / billing 外部证据模板：新增 `docs/release/image-digest-evidence-template.md` 和 `docs/billing/private-billing-evidence-template.md`，固定 image digest、真实支付、客户门户、发票/退款和 license signing 外部证据口径
  - v1.53 同步远端 CI 状态核查：最新可见远端 `main` run 失败发生在旧 `e8903bf6` workflow，仍含 Soybean / `frontend-v2` 旧 job；当前本地 workflow 收敛由 root governance contract 固定，远端失败不能作为当前本地完成证明
  - v1.54 同步后端 v2.2 contract 新鲜验证：10 个 BackendV22 contract class 共 40 个测试在 60 秒 timeout 内通过，作为主仓商业/OIDC/OTel/DSR/audit 证据；外部 live evidence 仍未完成
  - v1.55 同步前端 commercial surface 新鲜验证：`pnpm --dir frontend-admin test:v212` 通过 124 tests，包含 v2.2 commercial license、billing、OIDC entry、entitlement localization 和 commercial a11y gate contract
  - v1.56 同步仓库规范复查：README、安装文档、拓扑文档、CONTRIBUTING、PR template 和 CODEOWNERS 已把 legacy `frontend-v2` 从“只禁止新产品功能”收紧为冻结目录，只允许删除或迁出历史材料
  - v1.57 同步仓库规范复查：补充 Spring 管理组件多构造器必须显式运行时注入规则，记录 OIDC StateService 构造器回归和登录页初始动画 Lighthouse 修复；后端 v2.2 contract 新鲜验证现为 41 tests
  - v1.58 同步仓库规范复查：修复本地安全报告默认写入仓库根 `artifacts/` 的漂移；安全扫描默认输出到系统临时目录，CI 通过显式 `MMMAIL_SECURITY_REPORT_DIR` 收集 artifact
  - v1.59 同步 auth shell 性能复查：登录页首屏只同步加载密码登录模块，非首屏 auth 模块懒加载；品牌 logo、feature chip、语言和主题控件改为轻量原生实现，Lighthouse fresh build 提升到 performance=84
  - v1.60 同步完整本地门禁复查：V39 OIDC migration 更新 `system_release_metadata.schema_version` 到 39，Batch 3 migration gates 重新通过；`sg docker` + 临时 Helm PATH 下 `scripts/validate-local.sh` 输出 `all checks passed`
  - v1.61 同步生成文件 hygiene 复查：`frontend-admin` 的 `gen-route` 和 `scripts/validate-local.sh` 会在路由类型生成后运行 `normalize-generated-types.mjs`，本地总门禁末尾新增 `git diff --check`，避免生成器把尾随空格带入工作树
  - v1.62 同步外部状态复查：远端仍只有 `MMMail CI` workflow、无 `MMMail Images` workflow，GHCR backend / frontend-admin packages 仍为 404，独立 billing 仓不可解析；本机 `sg docker` 只看到 Nacos/Kafka，无 Keycloak/OIDC 容器或 OIDC env
  - v1.63 同步外部 verifier 加固：`scripts/validate-v22-external-evidence.sh` 不再只依赖文档 markers；当 markers 被移除后必须校验 live OIDC、image digest、private billing evidence 文件和 GH workflow / GHCR / billing repo 可达性，防止只改文档伪造完成态
  - v1.64 同步 completed external evidence 复验：GitHub private vulnerability reporting 已标记 done，外部 verifier 现在会直接调用 GitHub API 确认 `{"enabled":true}`，不再只信 checklist 文本
  - v1.65 同步外部 evidence 文件防模板误判：完成态 evidence 文件必须包含 `Evidence status: completed-external-evidence`，且 verifier 会拒绝模板正文、未填写模板和 `sha256:*` wildcard digest
  - v1.66 同步 image digest 完成态门禁收紧：DEP-02 evidence 必须证明 `MMMail Images` workflow 是 `push` event 且 conclusion 为 `success`，verifier 对 GH run list 也使用 `--event push --status success`
  - v1.67 同步外部 evidence 字段级校验：verifier 要求 OIDC、image digest 和 private billing evidence 的 commit、provider、run、workflow、digest 与 billing repo 核心字段均为非空，避免空模板加完成标记通过
  - v1.68 同步外部 verifier 当前态实证输出：在 incomplete markers 之外，verifier 现在也报告 evidence 文件缺失、成功 tag-push Images workflow 不可见、GHCR package 不可见和 private billing repo 不可访问
  - v1.69 同步外部 verifier 失败口径：旧“7 个缺口”口径收敛为 7 个 status markers 加 7 个 read-only evidence gaps，避免误读当前失败输出
  - v1.70 同步仓库规范 release-gate 复查：release-gate 增加 legacy frontend freeze 第 17 步，CI release-gate job 使用完整历史，根 contract gate 文案从 v2.1.2 收敛为 repository contract gates，AGENTS 补入新代码质量上限
  - v1.71 同步外部 verifier 发布前置条件：verifier 当前态输出新增本地 v2.2 实现未发布到远端 commit/tag 的 read-only gap，当前失败口径变为 7 个 status markers 加 8 个 read-only evidence gaps
  - v1.72 同步默认门禁外部 verifier 隔离：root governance tests 不再直接执行 `scripts/validate-v22-external-evidence.sh`；默认 validate-local / CI / release-gate 只静态保护 verifier 合同、manual-only wiring 和 failure markers，真实执行仍限手动外部证据验收
  - v1.73 同步完成态发布前置条件加固：外部 verifier 完成态会比较 OIDC backend/frontend commit、image commit、private billing 记录的 Public MMMail commit、origin release tag commit 和 origin branch containment，防止 evidence package 指向不同代码版本
  - v1.74 同步仓库规范超大文件复查：`frontend-admin/src/router/routes/index.ts` 拆出 `custom-routes.ts` / `public-share-routes.ts`，locale 字典拆出 `v22-commercial/*`，`DriveService` 拆出 `DrivePublicShareRateLimiter`，避免本轮新增路由、commercial i18n 和 Redis tracing 继续扩大历史 oversized 文件
  - v1.75 同步仓库规范复查：新增活跃源码 500 行 allowlist contract，CONTRIBUTING / PR template 纳入源码行数检查，`.gitignore` 明确忽略本地 agent / validation 产物
  - v1.76 同步远端 CI 规范复查：修复 root contract 依赖未跟踪 `docs/superpowers/specs/*` 文档的问题，并把依赖这些文档的 fixtures 纳入 Git 跟踪
  - v1.77 同步 CI 工具链复查：CI pnpm 版本对齐 `frontend-admin` 的 `engines.pnpm >=10.5.0`，并把 GitHub Actions / Docker Actions 升到当前 Node 24 兼容主版本
  - v1.78 同步镜像构建复查：`frontend-admin` 显式声明 Vite config 直接依赖的 `@iconify/utils`，避免 Docker fresh install 依赖 pnpm transitive dependency 泄漏
  - v1.79 同步 Docker context 规范复查：`.dockerignore` 排除 `frontend-admin` 生成产物、本地 agent 目录和 validation cache，避免镜像构建上传 GB 级无关上下文
review_passes:
  - pass-1 现状对账：用 grep / ls / package.json / CI / release-gate 核对已存在与缺失项
  - pass-2 一致性复查：统一 Free-Pro-Business、Adapay 独立仓、个人开发者容量
  - pass-3 必要性筛除：v2.2 只保留能解锁 OSS 发布、Commercial MVP 或前端收敛的 P0
  - pass-4 可计划性复查：每个 P0 均能拆成单独 spec，单项目标可验证
  - pass-5 前端拓扑复查：确认 `frontend-admin` 是目标产品前端，`frontend-v2` 只能短期承载历史契约，必须在 v2.2 收敛
  - pass-6 个人开发者遍历复查：15 处修正（5 必修改 + 6 建议修 + 4 残余数字/日期/步骤同步）；全部闭合，spec 达到子 spec 发放标准
  - pass-7 可执行性复查：用当前仓库 Flyway Java/SQL migration、frontend-admin README/packages 和 release-gate 脚本校正 spec
  - pass-8 仓库规范复查：治理文件、DCO、CODEOWNERS、Dependabot、NOTICE、前端拓扑审计和产品 release-gate 收敛已进入主仓变更
  - pass-9 仓库规范复查：补齐 SUPPORT.md、根级 EditorConfig/Git attributes，修正 i18n governance 的产品前端归属，并把这些入口加入 validate-local 和 root contract
  - pass-10 仓库规范复查：AGENTS.md 不再引用外部 full-stack skill 仓库；已加入 validate-local required files 和 root governance contract
  - pass-11 COMM-04 复查：后端拦截器读取 org + edition context，Free 调 Business feature 显式 403，并记录失败审计
  - pass-12 COMM-04 回归复查：修复商业化组件多构造器 Spring 注入歧义、license 签名失败分类和不稳定篡改测试数据
  - pass-13 COMM-05 首批落地复查：license 管理 API 和唯一产品前端展示已串通，contract gate 覆盖后端 API、前端 API client、Settings License、Admin Billing 和 blocked state
  - pass-14 COMM-05 OIDC entry 复查：Admin OIDC 入口已接 EntitlementGate 和三语文案，只暴露权益边界，不伪造配置成功
  - pass-15 COMM-05 a11y 复查：`frontend-admin/e2e/v22-commercial-a11y.spec.ts` 用 axe-core 覆盖登录、注册、license、billing status 和 OIDC blocked state；Vue DevTools 在 test mode 显式关闭，Admin 多根模板错误已修复
  - pass-16 GATE-01 supply-chain 复查：`scripts/generate-sbom-license-report.mjs` 输出 `mmmail-sbom.cdx.json`、`dependency-license-report.json` 和 `dependency-license-report.spdx.json`，CI 从 runner temp 上传 supply-chain artifact
  - pass-17 仓库规范复查：`scripts/release-gate.sh` 文档口径改为全 15 步，`AGENTS.md` 移除 SBOM 未完成误导并补供应链验证矩阵，`validate-local` 直接保护 PR 模板
  - pass-18 DEP-01 复查：Helm chart 只渲染 backend / frontend-admin，values 覆盖 license、billing、OIDC、audit export、OTEL，敏感值通过 Kubernetes Secret 注入
  - pass-19 DEP-02 复查：`.github/workflows/images.yml` 以 tag 触发 GHCR 多架构 backend / frontend-admin 镜像构建，workflow_dispatch 作为 dry run，不发布 `frontend-v2`
  - pass-20 GTM-01 复查：`docs/commercial/pricing-boundaries.md`、`support-policy.md`、`trademark-policy.md` 已写清商业口径边界，并进入 `tests/v22-commercial-boundaries-contract.test.mjs`
  - pass-21 OBS-02 复查：`docs/observability/sli-slo.md` 已写清 required SLIs、evidence sources、alerting boundary 和 public wording boundary
  - pass-22 仓库规范复查：商业/部署/可观测边界进入 PR 与贡献规范，v22 spec 不再把 Helm、image workflow 和 SLI/SLO docs 误列为未落地
  - pass-23 BUS-02 复查：`/api/v2/orgs/{orgId}/audit/events/export` 输出 JSONL，`audit.export` Business gate、cursor 和 eventTypes 由 `BackendV22AuditExportContractTest` 固定
  - pass-24 BUS-03 复查：`/api/v2/orgs/{orgId}/dsr/*` 提供 DSR export/erasure job，`dsr.requests` Business gate、public job status 和 data inventory gate 由 `BackendV22DsrContractTest` / `validate-dsr-inventory.mjs` 固定
  - pass-25 仓库规范复查：`docs/deployment-runbook.md`、`docs/architecture/deployment-topology.md` 和 `docs/security/threat-model.md` 不再把 `frontend-v2` 写成运行时前端，`tests/v22-repository-governance-contract.test.mjs` 固定该约束
  - pass-26 OBS-01 复查：OpenTelemetry 默认关闭，启用后暴露 OTLP endpoint、HTTP/DB/Redis/billing webhook/license span；OIDC callback 专项 span 随 BUS-01 OIDC 后端落地
  - pass-27 BUS-01 复查：OIDC org-level config、state/nonce/PKCE、callback allowlist、`mmmail.oidc.callback` trace、threat model 和 `docs/commercial/oidc-sso.md` 已进入主仓；live Keycloak token exchange/e2e 仍保留为 done 前硬验收
  - pass-28 仓库规范复查：修正 AGENTS、P0 缺口表、COMM-05 / OBS-01 状态和治理 contract，避免把已落地 OIDC 基线继续写成缺失项
  - pass-29 FE-05 复查：前端收敛决策文档选择 v2.2 保留 `frontend-v2` legacy reference，新增 freeze gate 并进入 `validate-local` 与 root governance contract
  - pass-30 BUS-01 复查：OIDC callback 不再停留在 state 校验，已通过 `OidcTokenExchangeClient`、`OidcIdTokenValidator` 和 `OidcSessionIssuer` 串到真实 MMMail session 发行路径；live Keycloak e2e 仍是 done 前外部证据
  - pass-31 FE-03 复查：legacy auth/workspace/public-share/settings/command contracts 已迁到 root contract 和 `frontend-admin`，旧 migration signal 脚本与 CI job 已删除，剩余 frontend-v2 约束只保留 freeze gate
  - pass-32 COMM-04 复查：当前 v2.2 commercial endpoint surface 已分类，OIDC/audit/DSR Business API 有服务端 feature gate，license status/upload 是升级入口，billing webhook 走 HMAC/provider state，不靠 silent paid fallback
  - pass-33 仓库规范复查：确认 `docs/v22-open-source-commercial-spec.md` 作为单一权威规格可超过 500 行但必须保留 frontmatter、变更历史、review pass、工作包、验收门禁和剩余边界；root governance contract 已强制 `BackendV22CommercialSurfaceCoverageContractTest` 出现在本地与 CI commercial gate
  - pass-34 完成审计复查：`docs/v22-completion-audit.md` 明确当前主仓已落地证据、验证命令和仍需外部完成的 live Keycloak e2e、真实 image digest、独立计费仓事项
  - pass-35 外部证据复查：`docs/v22-external-evidence-checklist.md` 把可接受证据和 non-evidence 固定下来，避免把 dry run、mock billing、local OIDC unit test 或无元数据截图误判为完成
  - pass-36 外部证据 verifier 复查：`scripts/validate-v22-external-evidence.sh` 当前应失败，失败输出必须包含 completion audit、external checklist、BUS-01、DEP-02、OBS-01、GATE-01 和 private billing repository 缺口；GitHub private vulnerability reporting 已从当前缺口移到 completed external evidence
  - pass-37 外部 verifier 门禁边界复查：`scripts/validate-local.sh` 只把 verifier 作为 required file，不执行；`scripts/release-gate.sh` 不执行该 verifier，避免外部未齐时默认绿色门禁永久失败
  - pass-38 贡献入口复查：`CONTRIBUTING.md` 和 `.github/pull_request_template.md` 已要求外部证据项转 done 前提供真实证据，不接受 dry run、mock billing、本地 OIDC unit test 或无元数据截图
  - pass-39 完成状态复查：主仓代码、文档和本地门禁已收敛，spec 状态必须表达“主仓已实现但外部证据仍未齐”，不能继续停留在 implementation-started
  - pass-40 外部证据脚本覆盖复查：独立计费仓、真实支付、客户门户、发票/退款和 license signing 是外部商业闭环硬证据，verifier 必须像 Keycloak 和 image digest 一样显式报告
  - pass-41 剩余决策复查：第 10 节不再按 Sprint 0 口径要求重新拍板已落地事项，改为记录当前状态、v2.2 处理和仍需外部证据的边界
  - pass-42 外部证据 checklist 复查：checklist 固定当前 verifier 应报的 completion audit、checklist、BUS-01、DEP-02、OBS-01、GATE-01 和 private billing incomplete markers；GitHub private vulnerability reporting 后续只作为 completed evidence 校验
  - pass-43 前端拓扑叙述复查：README、CI 和 release-gate 已经收敛到 `frontend-admin`，spec 只保留双前端历史风险，不再把已修复问题写成当前缺陷
  - pass-44 release-gate skip 防线复查：`scripts/release-gate.sh` 不只在文档上禁止 CI skip，还在 `CI=true` 且设置 `MMMAIL_SKIP_BACKEND` / `MMMAIL_SKIP_E2E` 时直接失败
  - pass-45 edition source precedence 复查：`docs/commercial/edition-entitlement-surface.md` 和后端 contract 明确 subscription state 优先于 active license，避免 paid / canceled 状态被静默误判
  - pass-46 完成审计命令表复查：completion audit 的 verified commands 已补入 edition core 定向 Maven 测试，和商业 surface 测试共同覆盖商业 edition / entitlement 边界
  - pass-47 仓库规范复查：DCO 不再只停留在 workflow 和 PR checklist，根级 `DCO.md` 明确签署承诺、常用 Git 修复命令和无 CLA 路线，且治理测试拆分后每个测试文件低于 500 行
  - pass-48 仓库规范复查：AGENTS、前端拓扑审计和收敛决策已把历史 `frontend-v2` 文档归类为 archival evidence，避免旧计划和旧 release notes 被误用为当前门禁
  - pass-49 外部证据复查：`gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` 返回 `{"enabled":true}`；SECURITY / SUPPORT / completion audit / verifier 和 root governance contract 已移除 GitHub 未启用缺口
  - pass-50 仓库规范复查：DCO workflow 改为通过 GitHub API 检查 PR commit messages，避免 pull_request merge commit 误报；`.github/dependabot.yml` 对 legacy `frontend-v2` 设置 `open-pull-requests-limit: 0`，避免自动 PR 修改冻结前端
  - pass-51 外部状态核查：`git ls-remote --tags origin 'refs/tags/v2.1.2*'` 确认 shipping-clean tag 存在；`gh workflow list` 仍只有 `MMMail CI`，`MMMail Images` workflow 不在远端默认分支；GHCR `mmmail-backend` / `mmmail-frontend-admin` packages 均 404；普通 shell Docker 权限不足，后续 `sg docker` 复查只看到 Nacos/Kafka，无 OIDC/Keycloak env 或容器，不能取得 live Keycloak e2e 证据
  - pass-52 OTel 文档复查：`docs/observability/opentelemetry.md` 已列出 `mmmail.oidc.callback` 主仓 span surface，并明确 BUS-01 / OBS-01 仍需 live Keycloak/OIDC trace evidence；`tests/v22-observability-docs-contract.test.mjs` 阻断 future/pending 旧口径回归
  - pass-53 live OIDC 证据模板复查：`docs/commercial/oidc-live-evidence-template.md` 已定义 backend/frontend commit、IdP version、callback URL、login/callback/session/refresh/logout、success/error trace 和 gate run evidence；`validate-local` required files 与 root governance contract 固定该模板存在
  - pass-54 DEP-02 / billing 外部证据模板复查：`docs/release/image-digest-evidence-template.md` 定义 tag-triggered workflow run、GHCR backend/frontend digest 和 release notes 证据；`docs/billing/private-billing-evidence-template.md` 定义真实 provider adapter、webhook、customer portal、invoice/refund、license signing 和 idempotency 证据；external verifier 固定模板存在但仍按真实外部缺口失败
  - pass-55 远端 CI 状态复查：`gh run view 25959159527 --repo IMG-LTD/MMMail --json jobs` 显示旧 `e8903bf6` run 失败；`git show e8903bf6:.github/workflows/ci.yml` 证明失败 workflow 仍含 Soybean / `frontend-v2` 旧 job，当前本地 CI 收敛状态仍以工作树和 root governance tests 为准
  - pass-56 后端 v2.2 contract 新鲜验证：`timeout 60s mvn -f backend/pom.xml -pl mmmail-server -am -Dtest=BackendV22EditionCoreContractTest,BackendV22LicenseVerifierContractTest,BackendV22BillingWebhookContractTest,BackendV22EntitlementEnforcementContractTest,BackendV22CommercialSurfaceCoverageContractTest,BackendV22LicenseManagementApiContractTest,BackendV22AuditExportContractTest,BackendV22DsrContractTest,BackendV22OpenTelemetryContractTest,BackendV22OidcSsoContractTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过，41 tests / build success
  - pass-57 前端 commercial surface 新鲜验证：`pnpm --dir frontend-admin test:v212` 通过，124 tests 覆盖 v2.2 license API client、Settings License、Admin Billing 外部状态、OIDC entitlement entry、blocked state i18n 和 commercial browser a11y gate contract
  - pass-58 仓库规范冻结口径复查：README、安装文档、拓扑文档、CONTRIBUTING、PR template 和 CODEOWNERS 已明确 legacy `frontend-v2` 不允许新增或修改文件，只允许删除或迁出历史材料；root governance validation contract 固定该口径
  - pass-59 仓库规范 Spring 注入与 auth shell 性能复查：`BackendV22OidcSsoContractTest` 固定 `OidcStateService` 运行时构造器必须显式 `@Autowired`，`frontend-admin` 登录/注册初始模块不再使用 appear transition
  - pass-60 仓库规范安全产物复查：`validate-security.sh`、`security-secret-scan.sh` 和 `security-backend-dependency-scan.sh` 默认报告目录改为 `${TMPDIR:-/tmp}/mmmail-security`，CI 通过 `MMMAIL_SECURITY_REPORT_DIR=artifacts/security` 显式收集报告，避免本地 `validate-local` 生成仓库根 `artifacts/`
  - pass-61 auth shell 首屏性能复查：非首屏 auth 模块通过 `defineAsyncComponent` 懒加载；登录页移除 `SystemLogo`、`NTag`、`ThemeSchemaSwitch` 和 `LangSwitch` 首屏依赖，保留原生主题切换和语言选择交互；`pnpm --dir frontend-admin test:lighthouse` 重新构建后登录页 performance=84
  - pass-62 完整本地门禁复查：`V39__oidc_sso_init.sql` 已把 `system_release_metadata.schema_version` 更新到 39；`sg docker -c "cd /home/xiang/桌面/project/MMMail-test/MMMail && PATH=/tmp/mmmail-helm-bin:$PATH env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy bash scripts/validate-local.sh"` 输出 `all checks passed`
  - pass-63 生成文件 hygiene 复查：完整门禁后发现 `elegant-router.d.ts` 被路由类型生成器重新写入尾随空格；`gen-route`、`validate-local` 和治理 contract 已固定生成后清理与 `git diff --check`，防止门禁绿色但工作树格式失败
  - pass-64 外部状态再核查：`gh workflow list`、`gh run list --workflow "MMMail Images"`、GHCR package API、`gh repo view IMG-LTD/mmmail-billing-gateway`、OIDC env grep、`sg docker` 和 `ss` 均未提供 live Keycloak、image digest 或私有 billing 仓证据；外部 verifier 当前按 7 个 status markers 加 7 个 read-only evidence gaps 失败
  - pass-65 外部 verifier 完成态加固：当前缺口存在时 verifier 仍按 7 个 markers 失败；一旦文档 markers 被移除，verifier 会要求 `MMMAIL_OIDC_LIVE_EVIDENCE_FILE`、`MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE`、`MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE` 和真实 GH / GHCR / billing repo 查询通过
  - pass-66 completed external evidence 复验：`scripts/validate-v22-external-evidence.sh` 会用 `gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` 重新确认 GitHub private vulnerability reporting 为 enabled，避免已完成外部项退化成纯文档声明
  - pass-67 外部 evidence 文件防模板误判：`scripts/validate-v22-external-evidence.sh` 要求三个完成态 evidence 文件都有 `Evidence status: completed-external-evidence`，并拒绝模板正文、未填写模板与 `sha256:*` wildcard digest
  - pass-68 image digest 完成态门禁收紧：`scripts/validate-v22-external-evidence.sh` 要求 image evidence 写明 `Workflow event: push` 和 `Workflow conclusion: success`，并用 `gh run list --event push --status success` 查询真实 `MMMail Images` run
  - pass-69 外部 evidence 字段级校验：`scripts/validate-v22-external-evidence.sh` 对 OIDC、image digest 和 private billing evidence 的关键字段执行非空校验，包括 commit SHA、provider metadata、workflow URL、immutable digest、billing repo URL 和 signing key location
  - pass-70 外部 verifier 当前态实证输出：`scripts/validate-v22-external-evidence.sh` 在当前 incomplete 状态下也运行只读 GH / evidence file 检查，输出 evidence file、Images workflow、GHCR packages 和 private billing repo 的实际缺口
  - pass-71 外部 verifier 失败口径复查：spec 不再把当前失败笼统写成“7 个缺口”，而是区分 7 个 status markers 和 7 个 read-only evidence gaps
  - pass-72 仓库规范 release-gate 复查：`scripts/release-gate.sh` 现在第 17 步执行 `scripts/validate-legacy-frontend-v2-freeze.sh`，CI release-gate checkout 使用 `fetch-depth: 0`，root gate 名称改为 repository contract gates，AGENTS 固定新代码 50/500 质量上限和 oversized legacy 例外
  - pass-73 外部 verifier 发布前置条件复查：`scripts/validate-v22-external-evidence.sh` 当前态会检查本地 tracked / untracked / ahead 状态，并在 v2.2 实现尚未发布到远端 commit/tag 时输出 read-only gap；当前失败口径为 7 个 status markers 加 8 个 read-only evidence gaps
  - pass-74 默认门禁外部 verifier 隔离复查：root governance tests 不再 spawn 外部证据 verifier，避免 `node --test tests/*.test.mjs` / `scripts/validate-local.sh` 间接依赖 GitHub CLI、GHCR、远端 workflow 或私有 billing repo；默认门禁只静态断言手动 verifier 的合同和 expected markers
  - pass-75 完成态发布前置条件加固复查：`scripts/validate-v22-external-evidence.sh` 完成态会要求 OIDC backend/frontend commit、image evidence commit、private billing evidence 的 Public MMMail commit 互相一致，origin release tag 指向同一 commit，并且该 commit 出现在 `origin/main` 或 `origin/release/*`
  - pass-76 仓库规范超大文件复查：把新增公共分享路由从 router index 拆到自定义路由模块，把 commercial i18n 文案拆到 `v22-commercial/*`，把 public share Redis limiter 从 `DriveService` 拆到 `DrivePublicShareRateLimiter`，并用治理 contract 固定本轮新增逻辑不得继续扩大已超限文件
  - pass-77 仓库规范源码行数与本地产物复查：root governance validation contract 扫描活跃源码文件，未列入历史 allowlist 或生成类型例外的文件不得超过 500 行；CONTRIBUTING / PR template 要求新 PR 检查该边界；`.gitignore` 忽略 `.claude/` 等本地 agent 产物
  - pass-78 远端 CI 根因复查：`v2.2.0-rc.1` 的 root contract 失败来自三份被本地 `.git/info/exclude` 忽略但未跟踪的 spec fixture 文档，以及测试直接依赖 runner 未声明的 `rg`；修复为跟踪文档并用 Node 文件扫描替代 `rg`
  - pass-79 CI 工具链复查：`v2.2.0-rc.1` 的 docker baseline 失败来自 workflow 使用 pnpm 9，而 `frontend-admin/package.json` 要求 pnpm `>=10.5.0`；修复为统一 `MMMAIL_PNPM_VERSION=10.5.0` 并升级 actions major，避免 Node 20 actions deprecation 进入 2026-06 强制切换窗口
  - pass-80 image publishing 复查：frontend-admin 镜像 fresh install 失败来自 `build/plugins/unocss.ts` 直接 import `@iconify/utils` 但 package 未声明直接依赖；修复为显式 devDependency，并用 CI toolchain contract 固定
  - pass-81 Docker context 复查：本地 frontend-admin 镜像 clean build 证明缺失依赖修复有效，同时暴露 `.dockerignore` 未排除 `frontend-admin/node_modules`、`.claude`、`.tools` 等目录导致 context 约 1GB；修复后由 CI toolchain contract 固定
---

# v2.2 开源 + 商业化筹备 spec

本 spec 站在 v2.1.2 shipping clean 之后，回答三个问题：

1. MMMail 要成为真正可被外部采用和贡献的开源项目，还缺什么信任层？
2. MMMail 要启动个人开发者可承受的商业化 MVP，还缺什么最小闭环？
3. 当前仓库存在两个前端项目，这是否合理，v2.2 应该如何收敛？

本 spec 不写代码，不替代法务、财税或支付机构意见。它定义主仓可公开交付的边界、差距、工作包、验收口径、release gate 和后续子 spec 拆分。

---

## 0. TL;DR

### 0.1 已锁定的 v2.2 方向

| 决策 | v2.2 取值 | 说明 |
|---|---|---|
| 治理模型 | BDFL | 单维护者决策，公开升级路径，不伪装成委员会 |
| Edition 命名 | Free / Pro / Business | 替代旧版 Community / Enterprise 二分 |
| 首发市场 | 中国大陆优先 | 支付、文案、合规和支持口径先按大陆商业用户收敛 |
| 支付实现 | 独立私有仓库 `mmmail-billing-gateway` | 主仓只定义 provider 接口、webhook、license 校验和订阅状态 |
| 支付通道 | Adapay（汇付天下）沙箱/生产 | 独立仓库承载；主仓零接触商户证书/私钥/进件 |
| 主仓许可 | Apache 2.0 主线保持 | 支付私钥、商户证书、签发私钥不进入主仓和公开镜像 |
| OSS 信任线 | v2.1.2 GA 能力永远保留在 Free | Pro/Business 只承载 v2.2 后新增的商业和企业准入能力 |
| 前端拓扑 | `frontend-admin` 成为唯一产品前端 | `frontend-v2` 只作为短期迁移参考和历史契约来源，v2.2 内退出发布入口 |

### 0.2 P0 投入概览

| 交付线 | P0 范围 | 主仓估算 | 解锁能力 |
|---|---|---:|---|
| 开源治理 | CODE_OF_CONDUCT / GOVERNANCE / ROADMAP / MAINTAINERS / DCO / NOTICE / 私有漏洞报告口径 | 2.4 PD | 外部贡献者可进入、可判断、可追责 |
| 前端收敛 | 双前端审计、唯一产品入口、契约迁移、CI/release-gate 收敛、文档修正 | 8.5 PD | 避免开源发布时暴露两个互相竞争的前端 |
| 商业化主仓核心 | EditionContext / license 验签 / BillingProvider / webhook / billing contract / 后端 entitlement | 9.5 PD | 主仓能可靠识别付费状态和功能边界 |
| 独立计费仓库 | Adapay 接入、订阅、客户门户、license 签发 | 12 PD（另排） | 真正收钱和签发 license，不污染主仓 |
| 企业准入 | OIDC SSO / 审计 JSONL 导出 / DSR / data inventory | 7.5 PD | Business 档可回答早期 B2B 售前问题 |
| 部署生态 | Helm chart / 多架构镜像发布 / 自托管商业配置文档 | 4.5 PD | 自托管 Pro/Business 可落地 |
| 可观测与门禁 | OpenTelemetry / SLI 文档 / release gate 新增 license + helm + secret/SBOM gate | 4 PD | 商业化前可定位问题并防止供应链回退 |
| GTM 信任材料 | 定价边界页 / 支持政策 / 商标与品牌最小政策 | 2 PD | 用户知道买什么、不买什么、能获得什么支持 |

**主仓 P0 合计约 38.4 PD。个人维护者按 60% 有效开发日估算，约 11-13 周日历周期。独立计费仓库 12 PD 单独排期，可并行但不阻塞主仓 OSS launch。**

### 0.3 关键判断：两个前端不应长期并存

当前仓库仍存在两个前端目录，但产品入口已经收敛：

- `frontend-admin`：v2.1.2 目标前端，基于 Soybean Admin / Naive UI / Elegant Router，承载 v212 合约、覆盖率、e2e、bundle、i18n、style discipline、lighthouse。
- `frontend-v2`：v2.0 自研前端，v2.2 中只作为冻结 legacy reference；selected auth / workspace / public share / settings / command contracts 已迁入 root tests 或 `frontend-admin`。

双前端长期并存对 v2.2 开源 + 商业化发布不正确。原始风险包括：

1. README 若把两者都写成“当前仓库前端”，外部使用者会误判需要部署两个 Web 应用。
2. CI 和 release-gate 若同时把两套前端作为产品 gate，维护成本、依赖扫描和漏洞面会翻倍。
3. v2.1.2 迁移 spec 的目标是把业务迁到 `frontend-admin`，继续保留 `frontend-v2` 作为产品前端会制造方向冲突。
4. 商业化页面、license、billing、OIDC 等新入口若同时做两遍，会直接拖垮个人维护者容量。

v2.2 的落地结论是：**只保留一个产品前端：`frontend-admin`。`frontend-v2` 在 v2.2 只能作为 legacy reference；所有仍有价值的契约、公共页面、视觉基线和设计系统约束已经迁入 `frontend-admin` 或提升为主仓级 contract；README、安装文档、release gate、CI 产品路径不再把 `frontend-v2` 当成产品入口。**

---

## 1. 范围与硬边界

### 1.1 主仓可以做

- 定义 Free / Pro / Business edition 模型。
- 服务端强制 entitlement，前端 EntitlementGate 只做体验层提示。
- 通过 Ed25519 公钥验证 license key。
- 接收来自独立计费服务的 webhook，并更新本地订阅状态。
- 提供 `NoopBillingProvider`，让 Free 自托管永远可运行。
- 生成 SBOM、依赖 license 报告、公开治理文档和发布门禁。
- 收敛前端拓扑，让开源用户只面对一个可部署 Web 前端。

### 1.2 主仓明确不做

- 不集成 Adapay SDK、商户号、证书、私钥、进件流程、真实退款或发票。
- 不把 license 私钥写入源码、CI secret 输出、Docker image 或测试 fixture。
- 不在 v2.2 承诺 SCIM、SAML、BYOK、多区域、SOC2、24/7 SLA。
- 不为通过测试而引入静默 fallback、mock 支付成功、伪 license 成功路径。
- 不把商业化页面同时实现到 `frontend-admin` 和 `frontend-v2`。

### 1.3 独立计费仓库边界

`mmmail-billing-gateway` 是独立私有仓库。主仓只约定接口，不复制实现：

- 支付通道：Adapay 沙箱 / 生产订单、退款、对账。
- 商业数据：套餐、订阅、账单、客户门户。
- license 签发：Ed25519 私钥、签发审计、邮件或控制台下发。
- webhook 推送：签名、重试、幂等、死信队列。

### 1.4 前端边界

| 项 | v2.2 决策 |
|---|---|
| 产品前端 | `frontend-admin` |
| 历史参考 | `frontend-v2` |
| 新功能实现位置 | 仅 `frontend-admin` |
| 新 e2e / coverage / bundle gate | 仅 `frontend-admin` |
| 旧契约迁移方式 | 移入 `frontend-admin/tests` 或主仓 `tests` |
| 旧源码处理 | 迁移完成后从产品文档和 release gate 移除；是否删除目录由单独 frontend convergence spec 决定 |

---

## 2. 现状对账

### 2.1 已具备

| 能力 | 证据 |
|---|---|
| Apache 2.0 LICENSE | `LICENSE` |
| README 双语入口 | `README.md` |
| SECURITY.md | 已说明 GitHub private vulnerability reporting 已启用，并保留 minimal public security-contact fallback |
| SUPPORT.md | 已新增公开支持入口，指向支持边界、反馈分流、issue 模板和 SECURITY.md |
| ISSUE 模板 | `.github/ISSUE_TEMPLATE/*.md` |
| PR 模板 | `.github/pull_request_template.md` |
| DCO 说明 | `DCO.md` 已说明签署含义、常用 Git 命令、无 CLA 路线，并由 `.github/workflows/dco.yml` 执行 |
| Agent 工作规范 | `AGENTS.md` 已改为 MMMail 仓库边界、前端拓扑、验证矩阵和禁止事项 |
| 根级格式规范 | `.editorconfig`、`.gitattributes` 已补齐 |
| 威胁模型 | `docs/security/threat-model.md` |
| 模块成熟度矩阵 | `docs/open-source/module-maturity-matrix.md` |
| Prometheus / Grafana 基础 | `application.yml`、`ops/grafana/` |
| OpenAPI 契约 | `contracts/openapi/v21-api-catalog.yaml` |
| Billing 骨架 | `mmmail-billing/`、`SuiteBillingController`、subscription / invoice / payment_method mapper |
| 前端体验层 entitlement | `frontend-admin/src/components/access/EntitlementGate.vue` |
| 审计事件基础 | `AuditEventRegistry` |
| 多语言基础 | `zh-cn`、`en-us`、`zh-tw`、`v212-error-messages` |

### 2.2 双前端实测

| 维度 | `frontend-admin`（现状） | v2.2 目标 | `frontend-v2`（现状） | 结论 |
|---|---|---|---|---|
| package name | `mmmail-frontend-admin` | `mmmail-frontend-admin` | `mmmail-frontend-v2` | 已重命名；上游 attribution 写入 NOTICE |
| 角色 | v2.1.2 目标 admin/workspace 前端 | **唯一产品前端** | v2.0 自研前端 / 旧契约来源 | 不能继续并列产品化 |
| scripts | build/test/e2e/coverage/lighthouse/bundle/i18n/style | 不变，全部保留 | dev/build/typecheck/unit/component/visual QA | v2.2 新 gate 只对 `frontend-admin` |
| views 文件数 | 约 40 | 新增 ~10 个商业化页面 | 约 71 | 旧契约迁移，不新增 |
| tests/e2e 文件数 | 约 69 | 新增 ~15 个 gate contract | 约 56 | 旧契约必须迁移 |
| release-gate | step 2/3/4/6-11 已只检查 `frontend-admin` | step 6-11 只依赖 `frontend-admin` | 不再作为 release-gate 必需前端 | 产品 gate 已单向收敛 |
| README | 声明 `frontend-admin` 为产品前端 | **只提 `frontend-admin`** | 标记 legacy reference | 对外不误导 |
| CI | `frontend` job 检查 `frontend-admin`，legacy migration job 已移除 | 产品 CI 不把 `frontend-v2` 当发布入口 | 只保留 freeze gate 防止新改 legacy 文件 | 维护边界已拆开 |

### 2.3 P0 缺口

| 类别 | 缺口 | 当前状态 | v2.2 处理 |
|---|---|---|---|
| 治理 | `CODE_OF_CONDUCT.md` | 已新增初版 | P0 done，后续随治理流程迭代 |
| 治理 | `GOVERNANCE.md` | 已新增 BDFL / release owner / module owner 口径 | P0 done |
| 治理 | 公开 `ROADMAP.md` | 已新增公开 roadmap | P0 done，避免承诺未排期能力 |
| 治理 | `MAINTAINERS.md` | 已新增 maintainer / module owner | P0 done |
| 治理 | `SUPPORT.md` | 已新增支持入口和安全分流口径 | P0 done |
| 治理 | `AGENTS.md` | 已从外部 skill 仓库口径改为 MMMail 本仓规范，明确 `frontend-admin` / legacy `frontend-v2` / validate-local / commercial spec | P0 done |
| 治理 | DCO 校验 | 已新增 `.github/workflows/dco.yml` | P0 done，避免 CLA 法务成本 |
| 治理 | 根级编辑器 / Git 属性规范 | 已新增 `.editorconfig` / `.gitattributes` | P0 done，统一 LF、缩进和二进制 diff 规则 |
| 法务 | `NOTICE` | 已新增 Apache 2.0 + Soybean attribution | P0 done |
| 安全 | GitHub private vulnerability reporting | GitHub API 已确认启用，`SECURITY.md` / `SUPPORT.md` 已同步 | P0 done |
| 供应链 | SBOM / dependency license report | 已新增 CycloneDX / dependency license / SPDX 生成脚本，并进入 validate-local、release-gate step 13 和 CI artifact | P0 done；真实 tag digest 和 live Keycloak e2e evidence 分别归 DEP-02 / BUS-01 |
| 前端 | 唯一产品前端判定 | 已写入 README / install docs / release docs / deployment runbook / topology / threat model / topology audit；旧 contract migration signal 已退休 | P0 done，后续只执行 v2.3 删除或归档决策 |
| 前端 | `frontend-admin` metadata | 已改为 MMMail metadata，保留上游 NOTICE | P0 done |
| 前端 | `frontend-v2` 契约迁移 | selected auth/workspace/public-share/settings/command contracts 已迁入 `tests/v22-legacy-frontend-contract-migration.test.mjs` 和 `frontend-admin` public share surface；旧迁移信号脚本与 CI legacy migration job 已删除；freeze gate 阻断新增或修改 legacy 文件 | P0 done，v2.3 只剩删除或归档 legacy 目录 |
| 商业 | License key 系统 | 已落地 Ed25519 verifier、claims schema、license_state repository/service、`V37__license_init.sql`、license status/upload API、`frontend-admin` license UI 和 surface 文档；签发私钥不进主仓 | P0 done，真实签发留在独立计费仓 |
| 商业 | Edition 服务端强制 | 已落地 Edition / FeatureCode / FeatureGate / 注解骨架、`EditionContextResolver`、拦截器强制、失败审计、`docs/commercial/edition-entitlement-surface.md` 和 `BackendV22CommercialSurfaceCoverageContractTest` | P0 done，新增 Pro/Business API 必须同步 surface contract |
| 商业 | 支付 provider 抽象和 webhook | 已落地 `none` / `webhook` provider registry、HMAC webhook 验签、幂等事件表、订阅状态机、`V38__billing_webhook_init.sql`、schema、签名文档和 webhook surface 分类；未实现真实支付网关 | P0 done，真实收款和签发仍在独立计费仓 |
| 企业 | OIDC SSO | 已新增 org-level config、single-use state、nonce、PKCE `S256`、callback allowlist、真实 token exchange client、ID token signature / issuer / audience / nonce validation、现有用户 session issuance、`mmmail.oidc.callback` trace、Keycloak/国内 IdP callback 文档和 `BackendV22OidcSsoContractTest` | P0 partial done；live Keycloak login / callback / session / logout / token refresh e2e 仍需真实 IdP 验收 |
| 企业 | 审计日志导出 | 已新增 v2 JSONL / SIEM export、Business `audit.export` gate 和 `docs/compliance/audit-export.md` | P0 done |
| 企业 | DSR 导出 / 删除 / 匿名化 | 已新增 Business-gated DSR export/erasure job、public status、`docs/compliance/data-inventory.yaml` 和 inventory gate | P0 done |
| 部署 | Helm chart | 已新增 `helm/mmmail` chart、`docs/ops/helm.md` 和 `scripts/validate-helm-chart.sh`，CI / validate-local / release-gate 已接入 Helm 校验 | P0 done；真实镜像 tag / digest 归 DEP-02 |
| 部署 | 镜像发布流水 | 已新增 GHCR buildx multi-arch workflow，matrix 只包含 backend 与 frontend-admin，release notes 模板要求记录 digest | P0 partial done，真实 tag push 后由 workflow 产出 digest |
| 可观测 | OpenTelemetry tracing | 已新增 runtime tracing 依赖、配置、`RuntimeTraceService`、HTTP/DB/Redis/billing webhook/license/OIDC callback span、`docs/observability/opentelemetry.md`、`BackendV22OpenTelemetryContractTest` 和 `BackendV22OidcSsoContractTest` | P0 partial done；live Keycloak e2e 的真实 route/error trace evidence 待补 |
| 可观测 | SLI/SLO 文档 | 已新增 `docs/observability/sli-slo.md`，覆盖 API p99、5xx rate、billing webhook success rate、license verification failure rate、OIDC callback failure rate，且明确不是对外 SLA | P0 done |

### 2.4 本轮仓库规范落地状态

已落地：

- 开源治理入口：`CODE_OF_CONDUCT.md`、`GOVERNANCE.md`、`ROADMAP.md`、`MAINTAINERS.md`。
- 支持入口：`SUPPORT.md` 连接支持边界、反馈分流、release blocker、自托管反馈和安全私密交接。
- Agent 工作规范：`AGENTS.md` 已重写为 MMMail 本仓边界，锁定 `frontend-admin` 是唯一产品前端、legacy `frontend-v2` 只做冻结历史参考、`scripts/validate-local.sh` 是本地总门禁、`docs/v22-open-source-commercial-spec.md` 是当前执行规格入口。
- 规格文件治理：版本化 spec、release audit 和验收矩阵类文档允许超过常规文件行数建议；`docs/v22-open-source-commercial-spec.md` 作为本轮单一权威规格不为行数拆散，但必须持续保留 frontmatter、变更历史、review pass、工作包、验收门禁和剩余边界。
- 活跃源码行数治理：root governance validation contract 已扫描 `backend/mmmail-server`、`frontend-admin`、`tests`、`scripts` 等活跃源码入口；未列入历史 allowlist 或生成类型例外的文件不得超过 500 行，PR template 和 CONTRIBUTING 要求新增代码先拆分职责。
- 本地产物治理：`.gitignore` 明确忽略 `.claude/`、`.codex-tasks/`、`.superpowers/`、`.tmp/` 和 `.tools/dependency-check-data/` 等本地 agent / validation 产物；共享 agent 规则只以 `AGENTS.md` 为准。
- 完成审计入口：`docs/v22-completion-audit.md` 把当前用户目标、spec P0、release gate、仓库规范复查、验证命令和外部剩余项映射到具体证据；该文件已进入 `scripts/validate-local.sh` required files 和 root governance contract。
- 外部证据清单：`docs/v22-external-evidence-checklist.md` 明确 GitHub private vulnerability reporting 已完成，并继续定义 live Keycloak SSO、OIDC trace、真实 image digest 和独立计费仓证据的验收口径；`docs/commercial/oidc-live-evidence-template.md` 固定 BUS-01 / OBS-01 / GATE-01 的 redacted evidence package；`docs/release/image-digest-evidence-template.md` 固定 DEP-02 tag workflow digest evidence；`docs/billing/private-billing-evidence-template.md` 固定独立计费仓 / 真实支付 / license signing evidence；`scripts/validate-v22-external-evidence.sh` 当前必须明确失败并列出剩余外部缺口，且在外部证据未齐前不得进入默认 validate-local、CI 或 release-gate 绿色路径；root governance tests 只静态保护 verifier 合同、manual-only wiring 和 expected markers，不直接执行该外部 verifier；完成态 verifier 必须校验 OIDC / image / billing evidence 指向同一 Public MMMail commit，且 origin release tag 和远端分支可见性与该 commit 一致。
- 贡献入口防误判：`CONTRIBUTING.md` 和 `.github/pull_request_template.md` 已要求外部证据项从 partial / pending 标为 done 前必须附真实证据并记录 `scripts/validate-v22-external-evidence.sh` 结果。
- 法务与贡献入口：`NOTICE`、`DCO.md`、DCO workflow、CODEOWNERS、Dependabot、PR template。
- 仓库格式入口：`.editorconfig` 和 `.gitattributes` 明确 LF、缩进和二进制文件规则。
- 公开治理文档收敛：`docs/open-source/i18n-governance.md` 已改为 `frontend-admin` 当前治理对象，`frontend-v2` 只保留 legacy reference / freeze 口径。
- 运行时规范收敛：`docs/deployment-runbook.md`、`docs/architecture/deployment-topology.md` 和 `docs/security/threat-model.md` 已统一为 `frontend-admin` 运行时前端，legacy `frontend-v2` 只保留冻结历史参考。
- 前端拓扑审计：`docs/frontend/v22-frontend-topology-audit.md`。
- 产品前端收敛：README、安装文档、Compose、Dockerfile、release notes 均指向 `frontend-admin`。
- 历史文档边界：`docs/superpowers/`、旧 `docs/v21*` / `docs/v212*` spec 和旧 `docs/release/v2.0.*` release notes 中的 `frontend-v2` 命令只记录当时状态，不是当前产品入口、CI 门禁或发布流程。
- 发布门禁收敛：`scripts/release-gate.sh` 不再无条件运行 `frontend-v2`，CI 禁止 release-gate skip，且 17 步口径包含 `sbom-license`、`helm-lint`、`image-workflow-contract`、`dsr-inventory` 和 `legacy-frontend-freeze`。
- legacy 契约迁移：selected legacy auth/workspace/public-share/settings/command contracts 已迁入 `tests/v22-legacy-frontend-contract-migration.test.mjs`；mail/pass/drive public share surface 已迁入 `frontend-admin`；旧迁移信号脚本与 CI legacy migration job 已退休。
- legacy 前端冻结：`docs/frontend/v22-frontend-convergence-decision.md` 明确 v2.2 保留 `frontend-v2` 仅作 legacy reference，`scripts/validate-legacy-frontend-v2-freeze.sh` 进入 `validate-local` 并阻断新增或修改 `frontend-v2` 文件。
- 迁移编号门禁：`scripts/check-migration-naming.sh` 已同时扫描 SQL / Java Flyway migration，允许历史 `V14` 缺口但阻断 v2.2 新增版本重复或不连续。
- 商业化骨架：COMM-01 / COMM-02 / COMM-03 已落地 `backend/mmmail-server/src/main/java/com/mmmail/server/commercial/`、`V36__edition_init.sql`、`V37__license_init.sql`、`V38__billing_webhook_init.sql`、license/billing contracts 和 webhook 签名文档。
- commercial / OIDC contract gate：`BackendV22EditionCoreContractTest` / `BackendV22LicenseVerifierContractTest` / `BackendV22BillingWebhookContractTest` / `BackendV22EntitlementEnforcementContractTest` / `BackendV22CommercialSurfaceCoverageContractTest` / `BackendV22LicenseManagementApiContractTest` / `BackendV22AuditExportContractTest` / `BackendV22DsrContractTest` / `BackendV22OidcSsoContractTest` 已进入 `scripts/validate-local.sh` 和 CI backend job；root governance contract 已要求 surface coverage gate 同时出现在本地与 CI。
- secret scan 收敛：`scripts/security-secret-scan.sh` 已改为扫描 Git 管理范围内文件（tracked + untracked non-ignored），不再误扫 `node_modules`、临时 worktree、构建产物等 `.gitignore` 生成目录。
- supply-chain gate：`scripts/generate-sbom-license-report.mjs` 已输出 CycloneDX、dependency license report 和 SPDX 摘要，并进入 `scripts/validate-local.sh`、`scripts/release-gate.sh` 与 CI artifact。
- Helm 部署入口：`helm/mmmail` chart 只发布 `backend` 和 `frontend-admin`，通过外部 MySQL / Redis 与 Kubernetes Secret 注入运行时配置，并进入 `scripts/validate-helm-chart.sh`。
- 镜像发布入口：`.github/workflows/images.yml` 在 tag 上构建 GHCR 多架构 backend / frontend-admin 镜像，workflow_dispatch 仅用于 dry run，release notes 模板要求记录 digest。
- OpenTelemetry runtime tracing：`RuntimeTraceService`、`RequestTracingFilter`、commercial repository/webhook/license service 和 Drive Redis limiter 已接入 `mmmail.http.request` / `mmmail.db.operation` / `mmmail.redis.operation` / `mmmail.billing.webhook` / `mmmail.license.verify` span；`docs/observability/opentelemetry.md`、`BackendV22OpenTelemetryContractTest`、validate-local 和 CI 固定默认关闭、OTLP 配置和失败显式传播。
- OIDC SSO 基线：`OidcSsoController`、`OidcStateService`、`OidcTokenExchangeClient`、`OidcIdTokenValidator`、`OidcSessionIssuer`、`V39__oidc_sso_init.sql`、`docs/commercial/oidc-sso.md` 和 `BackendV22OidcSsoContractTest` 已固定 org config、single-use state、nonce、PKCE S256、callback allowlist、真实 token exchange client、ID token signature / issuer / audience / nonce validation、现有用户 session issuance、`mmmail.oidc.callback` trace 和 Keycloak hard acceptance 文档。
- 商业公开边界：`docs/commercial/pricing-boundaries.md`、`docs/commercial/support-policy.md`、`docs/commercial/trademark-policy.md` 已明确无公开定价承诺、无 live payment、无虚假 SLA、Free 自托管不削弱。
- DSR / data inventory：`DsrRequestController`、`DsrRequestService`、`DsrExecutionService` 已提供 Business-gated export / erasure job，`docs/compliance/dsr.md` 和 `docs/compliance/data-inventory.yaml` 已声明表级 owner / retention / export / delete 策略，`scripts/validate-dsr-inventory.mjs` 已阻断新表缺 inventory。
- SLI/SLO 内部目标：`docs/observability/sli-slo.md` 已明确 API p99、5xx rate、billing webhook success rate、license verification failure rate、OIDC callback failure rate 是 internal target，不是 public SLA 或 contractual commitment。

仍未落地：

- 独立计费仓真实支付与签发仍是后续 P0；OIDC 已有 token/session 主仓基线，live Keycloak login / callback / session / logout / token refresh e2e 证据仍未完成。
- Helm / image gates 已完成，后续只补真实 tag digest 和 tag push 产生的发布证据；SLI/SLO docs 与 OBS-01 runtime trace evidence 已接入主仓门禁。

---

## 3. 目标架构

### 3.1 Repository Topology

```text
MMMail/
  backend/                         # Spring Boot + modular Maven
  frontend-admin/                  # v2.2 唯一产品前端
  frontend-v2/                     # v2.2 迁移期 legacy reference；发布路径移除
  contracts/
    openapi/
    billing/
    license/
  docs/
    open-source/
    commercial/                    # GTM-01 产出：定价边界、支持政策、商标政策
    billing/
    compliance/
    frontend/
  helm/mmmail/                     # DEP-01 产出
  scripts/
  tests/
```

### 3.2 v2.2 运行时拓扑

```text
Browser
  -> frontend-admin static assets
  -> backend API
      -> Edition / FeatureGate / License verifier
      -> BillingProvider webhook state
      -> Audit / DSR / OIDC
      -> MySQL / Redis

mmmail-billing-gateway (private)
  -> Adapay
  -> License signing key
  -> POST backend /api/v2/billing/webhook/{provider}
```

`frontend-v2` 不在运行时拓扑里。

### 3.3 Edition / Billing / License 数据流

1. Free 自托管启动：`MMMAIL_BILLING_PROVIDER=none`，系统加载 Free entitlement。
2. 自托管 Pro/Business：管理员提供 license key，后端用内置公钥验签。
3. 独立计费服务收到 Adapay 订阅事件，签名后推送主仓 webhook。
4. 主仓 webhook 校验签名、幂等、状态流转，并写订阅状态。
5. 请求进入后端时，`@RequiresEdition` / `FeatureGate` 决定是否允许。
6. 前端 `EntitlementGate` 只展示升级提示，不作为安全边界。

---

## 4. P0 工作包总表

| ID | 工作包 | Owner | 估算 | 前置 | 输出 |
|---|---|---|---:|---|---|
| GOV-01 | 开源治理文件 | 主仓 | 1.5 PD | 无 | CODE_OF_CONDUCT / GOVERNANCE / ROADMAP / MAINTAINERS |
| GOV-02 | DCO + NOTICE + 私有漏洞报告口径 | 主仓 | 0.9 PD | GOV-01 | DCO.md / DCO workflow / NOTICE / SECURITY 更新 |
| FE-01 | 双前端基线审计 | 主仓 | 0.8 PD | 无 | frontend topology report |
| FE-02 | `frontend-admin` 品牌与 metadata 收敛 | 主仓 | 0.7 PD | FE-01 | package metadata / NOTICE attribution |
| FE-03 | `frontend-v2` 契约迁移 | 主仓 | 4.0 PD | FE-01 | moved contracts under frontend-admin/tests or root tests |
| FE-04 | 前端发布入口收敛 | 主仓 | 2.0 PD | FE-03 | README / CI / release-gate / install docs 单前端 |
| FE-05 | 前端收敛验收与归档决策 | 主仓 | 1.0 PD | FE-04 | remove/demote frontend-v2 decision record |
| COMM-01 | Edition core | 主仓 | 1.5 PD | FE-04（软依赖：enum 定义需先于导航入口收敛） | EditionContext / FeatureCode / RequiresEdition / V36__edition_init.sql |
| COMM-02 | License verifier | 主仓 | 2.0 PD | COMM-01 | Ed25519 verify / claims / expiry / V37__license_init.sql |
| COMM-03 | BillingProvider + webhook | 主仓 | 2.0 PD | COMM-01 | provider registry / webhook controller / state machine / V38__billing_webhook_init.sql |
| COMM-04 | Backend entitlement enforcement | 主仓 | 1.5 PD | COMM-01 | interceptors / annotations / audit |
| COMM-05 | Frontend entitlement surfaces | 主仓 | 1.5 PD | FE-04, COMM-01（FE-04 提供唯一前端入口；COMM-01 提供 enum） | license/admin/billing UI in frontend-admin + ~30 i18n keys（zh-cn/en-us/zh-tw） |
| BILL-01 | Billing contract schema | 主仓 + 独立仓 | 1.0 PD | COMM-03 | webhook schema / signature spec / license claims schema |
| BUS-01 | OIDC SSO | 主仓 | 3.0 PD | COMM-04 | OIDC config / login flow / tests |
| BUS-02 | Audit JSONL export | 主仓 | 2.0 PD | COMM-04 | export API / retention / tests |
| BUS-03 | DSR + data inventory | 主仓 | 2.5 PD | BUS-02 | export/delete/anonymize jobs / inventory gate |
| DEP-01 | Helm chart | 主仓 | 3.0 PD | FE-04 | helm/mmmail chart |
| DEP-02 | Image publishing | 主仓 | 1.0 PD | DEP-01 | GHCR multi-arch workflow |
| DEP-03 | Commercial self-host docs | 主仓 | 0.5 PD | COMM/BUS/DEP | install/config docs |
| OBS-01 | OpenTelemetry | 主仓 | 1.5 PD | COMM-03 | traces for API/DB/Redis/webhook/license |
| OBS-02 | SLI/SLO docs | 主仓 | 1.0 PD | OBS-01 | docs/observability/sli-slo.md |
| GATE-01 | v2.2 release gate expansion | 主仓 | 1.5 PD | all P0 | governance/license/billing/helm/sbom/a11y gates |
| GTM-01 | Pricing/support/trademark boundaries | 主仓 | 2.0 PD | GOV-01, FE-04 | public docs, no false SLA |

### 4.1 当前执行状态（2026-05-17）

| ID | 状态 | 证据 | 剩余边界 |
|---|---|---|---|
| GOV-01 | done | `CODE_OF_CONDUCT.md`、`GOVERNANCE.md`、`ROADMAP.md`、`MAINTAINERS.md`、`SUPPORT.md`、`AGENTS.md`、`.github/pull_request_template.md`、`.editorconfig`、`.gitattributes` 和 README 导航已进入主仓；root governance contract 固定这些文件存在、互相可发现且 AGENTS 不再引用外部 skill 仓库口径 | 后续公开边界变化必须同步这些治理入口，不允许只改实现不改支持 / 治理文档 |
| GOV-02 | done | `DCO.md`、`.github/workflows/dco.yml`、`NOTICE`、`SECURITY.md`、`SUPPORT.md` 和 `.github/ISSUE_TEMPLATE/security-contact-request.md` 已提供 DCO、署名、Apache 2.0 attribution、GitHub private vulnerability reporting 和 fallback 私密安全联络路径；`gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` 已返回 `{"enabled":true}`；root governance contract 固定无 CLA 路线、DCO sign-off 命令和公开 issue 不披露 live 漏洞 | 后续安全披露口径变化必须同步 SECURITY / SUPPORT / governance contract |
| FE-01 | done | `docs/frontend/v22-frontend-topology-audit.md` 明确 `frontend-admin` 是唯一产品前端，`frontend-v2` 只作为 legacy reference；README、安装文档、部署拓扑和 threat model 均使用单前端运行时口径 | v2.3 继续执行删除或归档 legacy 目录，不影响 v2.2 发布入口 |
| FE-02 | done | `frontend-admin/package.json`、README、构建脚本和 NOTICE 已收敛 MMMail metadata 与 Soybean attribution；root governance contract 固定前端品牌边界 | 保留上游 attribution，不把 Soybean metadata 暴露为 MMMail 产品名 |
| FE-03 | done | `tests/v22-legacy-frontend-contract-migration.test.mjs` 固定 auth/workspace/public-share/settings/command legacy contract 等价覆盖；`frontend-admin/src/service/api/public-share.ts`、`frontend-admin/src/typings/api/public-share.d.ts`、`frontend-admin/src/views/share/index.vue` 承接 mail/pass/drive public share surface；旧迁移信号脚本和 CI legacy migration job 已删除 | 未迁移的 stale legacy tests 不再作为 v2.2 产品 gate；v2.3 删除或归档前不得为通过检查静默删除仍有价值的 contract |
| FE-04 | done | README、docs/ops、release docs、Compose、Helm、Docker image workflow、release-gate 和 CI 均只发布 / 验证 `frontend-admin` 产品前端；`frontend-v2` 不进入运行时拓扑 | `frontend-v2` 目录仍在仓库内作为历史参考，靠 freeze gate 防止重新产品化 |
| FE-05 | done | `docs/frontend/v22-frontend-convergence-decision.md` 选择 v2.2 保留 `frontend-v2` legacy reference，`scripts/validate-legacy-frontend-v2-freeze.sh` 已进入 `validate-local`，root governance contract 用临时 git repo 验证新增 legacy 文件会失败 | v2.3 仍需实际删除或外部归档 `frontend-v2` |
| COMM-01 | done | `Edition` / `EditionContext` / `FeatureCode` / `FeatureGate` / `RequiresEdition` / `RequiresFeature`、`V36__edition_init.sql`、`BackendV22EditionCoreContractTest` | 新增 Pro/Business endpoint 必须复用 registry，不允许散落字符串 |
| COMM-02 | done | `LicenseKeyVerifier` / `LicenseClaims` / `LicenseStateRepository` / `LicenseSyncService`、`contracts/license/license-claims.schema.json`、`V37__license_init.sql`、`BackendV22LicenseVerifierContractTest`；签名篡改负例使用解码后字节翻转，避免 Base64URL 尾字符 padding 位导致误判 | 主仓仍不做 license 私钥签发；独立计费仓负责签发和支付 provider |
| COMM-03 | done | `BillingProvider` / `NoopBillingProvider` / `BillingProviderRegistry` / `BillingWebhookController` / `SubscriptionStateMachine`、`contracts/billing/webhook-event.schema.json`、`docs/billing/webhook-signature.md`、`V38__billing_webhook_init.sql`、`BackendV22BillingWebhookContractTest` | 独立计费仓 provider adapter、真实支付、客户门户和 license 签发仍在主仓外 |
| COMM-04 | done | `EditionContextResolver` / `JdbcEditionContextResolver`、`CommercialAuthorizationGate`、`AuthorizationAnnotationInterceptor` 已执行 `@RequiresEdition` / `@RequiresFeature`，失败写 `COMMERCIAL_ENTITLEMENT_DENIED`；`docs/commercial/edition-entitlement-surface.md` 和 `BackendV22CommercialSurfaceCoverageContractTest` 固定当前 v2.2 OIDC / audit / DSR Business API gate、license upgrade path 和 billing webhook HMAC boundary | 现有 v2.1.2 GA API 不因商业化 retroactively 收费；新增 Pro/Business API 必须同步 surface doc 和 contract |
| COMM-05 | done | `CommercialLicenseController` / `CommercialLicenseStatusReader` / `CommercialLicenseUploadService` / `LicensePublicKeyProvider` 已提供 `/api/v2/billing/license/status` 和 `/api/v2/billing/license`；`frontend-admin` 已新增 license API client、Settings License 面板、Admin Billing 外部计费状态、Admin OIDC setup entry、EntitlementGate 后端授权详情和三语 key；`BackendV22LicenseManagementApiContractTest`、`BackendV22CommercialSurfaceCoverageContractTest`、`frontend-admin/tests/v22-commercial-entitlement-surfaces-contract.test.mjs` 与 `frontend-admin/e2e/v22-commercial-a11y.spec.ts` 已覆盖 | 真实付款仍不在主仓；OIDC token/session 主仓基线已落地，live Keycloak e2e 证据仍跟随 BUS-01 验收 |
| BILL-01 | partial done | `contracts/billing/webhook-event.schema.json`、`contracts/license/license-claims.schema.json`、`docs/billing/webhook-signature.md`、`docs/billing/private-billing-evidence-template.md`、`docs/commercial/edition-entitlement-surface.md` 和 `BackendV22BillingWebhookContractTest` 已固定主仓 webhook schema、HMAC 签名、时间窗、幂等、订阅状态、license claims 边界和独立计费仓外部证据口径 | 独立 `mmmail-billing-gateway` 仍需实现 Adapay adapter、客户门户、真实订单 / 退款 / 发票和 license signing 私钥签发，主仓不能代替真实计费仓验收 |
| BUS-01 | partial done | `OidcSsoController` 暴露 `/api/v2/orgs/{orgId}/oidc/config`、`/api/v2/auth/oidc/login`、`/api/v2/auth/oidc/callback`；`OidcStateService` 生成 single-use `state`、`nonce` 和 PKCE `S256` challenge；`OidcTokenExchangeClient` 通过真实 token endpoint 交换 code；`OidcIdTokenValidator` 校验 signature / issuer / audience / nonce / subject / email；`OidcSessionIssuer` 接入现有 `AuthService` JWT 与 refresh session 发行路径；`V39__oidc_sso_init.sql` 增加 `org_oidc_config` / `oidc_auth_state`；`docs/commercial/oidc-sso.md` 记录 Keycloak hard acceptance 和飞书/钉钉/企业微信 callback URL 口径；`docs/commercial/oidc-live-evidence-template.md` 固定 live evidence package；`docs/security/threat-model.md` 补 OIDC attack surface；`BackendV22OidcSsoContractTest` 进入 validate-local 与 CI | live Keycloak login / callback / session / logout / token refresh e2e 仍需接真实 IdP 后补齐；本仓不做自动用户 provision |
| BUS-02 | done | `OrgAuditExportController` 提供 `/api/v2/orgs/{orgId}/audit/events/export`，`OrgAuditQueryService` 输出 `application/x-ndjson` JSONL，`CommercialAuthorizationGate.enforceFeature(..., AUDIT_EXPORT)` 强制 Business 权益，`BackendV22AuditExportContractTest` 覆盖 JSONL 字段、cursor 和 gate，`docs/compliance/audit-export.md` 记录 SIEM 映射 | 对象存储归档和大文件异步任务仍是未来扩展，不在 v2.2 已交付范围内宣传 |
| BUS-03 | done | `DsrRequestController` 提供 `/api/v2/orgs/{orgId}/dsr/export`、`/dsr/erasure`、`/dsr/jobs/{jobId}`；`DsrRequestService` 通过 `platform_job_run` 排队 `dsr.export` / `dsr.erasure`；`DsrExecutionService` 按 inventory 执行导出、软删、删除和匿名化；`docs/compliance/data-inventory.yaml` 覆盖 113 个 schema/Flyway 表；`scripts/validate-dsr-inventory.mjs`、`BackendV22DsrContractTest` 和 `tests/v22-dsr-inventory-contract.test.mjs` 固定 gate | 法定保留、财务、安全、治理记录按 inventory retain/anonymize，不宣传为物理全删除 |
| DEP-01 | done | `helm/mmmail/Chart.yaml`、`helm/mmmail/values.yaml`、backend/frontend-admin/config/secret/ingress templates、`docs/ops/helm.md`、`scripts/validate-helm-chart.sh`、`tests/v22-deployment-helm-contract.test.mjs` | 真实镜像 tag / digest 由 DEP-02 image publishing 补齐 |
| DEP-02 | partial done | `.github/workflows/images.yml`、`docs/release/release-notes-template.md` Image Digests 段、`docs/release/image-digest-evidence-template.md`、`tests/v22-image-publishing-contract.test.mjs`；workflow 使用 GHCR、QEMU、Buildx、metadata-action、build-push-action、linux/amd64+linux/arm64 | 真实 digest 只有 tag push 后生成；Docker Hub 未作为 v2.2 首发目标 |
| DEP-03 | done | `.env.example`、`config/backend.env.example`、`docs/ops/install.md`、`docs/ops/install.en.md`、`docs/ops/helm.md` 和 `docs/commercial/pricing-boundaries.md` 已记录 license、billing webhook、OIDC、OTel、Helm Secret 和商业自托管边界 | 文档只覆盖主仓可配置入口；商户证书、付款、客户门户和 license signing 私钥仍在独立计费仓 |
| OBS-01 | partial done | `RuntimeTraceService` 统一包装 Micrometer Observation；`RequestTracingFilter` 输出 `mmmail.http.request`；commercial JDBC repository 输出 `mmmail.db.operation`；Drive Redis limiter 输出 `mmmail.redis.operation`；billing webhook 输出 `mmmail.billing.webhook`；license sync 输出 `mmmail.license.verify`；OIDC callback 输出 `mmmail.oidc.callback`；`.env.example`、`config/backend.env.example`、Helm config 和 `application.yml` 均默认关闭并暴露 OTLP 配置；`docs/observability/opentelemetry.md`、`BackendV22OpenTelemetryContractTest`、validate-local 和 CI 固定门禁 | live Keycloak e2e 中的真实 route/error trace evidence 仍需随 BUS-01 done 前补齐 |
| OBS-02 | done | `docs/observability/sli-slo.md`、`tests/v22-observability-docs-contract.test.mjs`；文档声明 internal target、not a public SLA、not a contractual commitment | OIDC callback failure rate 已有主仓 route/error 基线；live Keycloak e2e 证据随 BUS-01 done 前补齐 |
| GTM-01 | done | `docs/commercial/pricing-boundaries.md`、`docs/commercial/support-policy.md`、`docs/commercial/trademark-policy.md`、`tests/v22-commercial-boundaries-contract.test.mjs`；README / SUPPORT / validate-local 已暴露入口 | 不包含真实价格、SLA、托管服务合同或商标注册结论 |
| GATE-01 | partial done | `scripts/validate-local.sh` 和 CI 已加入 v2.2 commercial backend contracts；`BackendV22CommercialSurfaceCoverageContractTest` 已作为 commercial surface coverage gate 固定当前 v2.2 paid API / upgrade path / webhook boundary；`BackendV22AuditExportContractTest` 已作为 audit-export-smoke 进入本地和 CI commercial regression；`BackendV22DsrContractTest`、`tests/v22-dsr-inventory-contract.test.mjs` 和 `scripts/validate-dsr-inventory.mjs` 已作为 DSR gate 进入本地、CI 和 release-gate step 16；`scripts/validate-legacy-frontend-v2-freeze.sh` 已作为 release-gate step 17，CI release-gate job 使用完整历史以验证 `origin/main` 对比；`BackendV22OpenTelemetryContractTest` 已作为 OpenTelemetry runtime trace gate 进入 validate-local 和 CI；`BackendV22OidcSsoContractTest` 已作为 OIDC config/state/PKCE/callback trace/token exchange/session issuance gate 进入 validate-local 和 CI；`security-secret-scan.sh` 已收敛扫描范围；commercial a11y 已随 `frontend-admin` Playwright e2e 进入 `test:e2e`；`scripts/generate-sbom-license-report.mjs` 已生成 CycloneDX / license / SPDX 报告并进入 release-gate + CI artifact；`helm-lint` 已通过 `scripts/validate-helm-chart.sh` 进入 validate-local / release-gate / CI；`image-workflow-contract` 已进入 validate-local / release-gate | live Keycloak e2e gate 未完成 |

---

## 5. 工作包详细拆分

### 5.1 GOV-01 开源治理文件

**目标**：外部贡献者不需要私聊维护者，也能判断如何参与、谁决策、怎么升级争议。

**输入**：
- `CONTRIBUTING.md`
- `SECURITY.md`
- `AGENTS.md`
- `.github/ISSUE_TEMPLATE/*`
- `docs/open-source/module-maturity-matrix.md`

**输出**：
- `CODE_OF_CONDUCT.md`
- `GOVERNANCE.md`
- `ROADMAP.md`
- `MAINTAINERS.md`
- `SUPPORT.md`
- `AGENTS.md`
- `.github/pull_request_template.md`
- `.editorconfig`
- `.gitattributes`

**步骤**：
1. 写 `CODE_OF_CONDUCT.md`，采用 Contributor Covenant 2.1，联系路径指向 SECURITY 的私有披露流程。
2. 写 `GOVERNANCE.md`，明确 BDFL、release owner、模块 owner、决策记录方式、争议升级路径。
3. 写 `ROADMAP.md`，只公开 v2.2/v2.3 方向、Free/Pro/Business 边界和不承诺项。
4. 写 `MAINTAINERS.md`，列出当前 maintainer、模块 owner、响应目标、交接规则。
5. 写 `AGENTS.md`，只描述 MMMail 本仓真实边界、前端拓扑、commercial spec、验证矩阵和禁止事项，不引用外部 skill 仓库结构。
6. 写 PR template，要求标注范围、验证、文档同步、DCO、legacy frontend-v2 边界和 security checklist。
7. 写根级 `.editorconfig` / `.gitattributes`，固定 LF、缩进和二进制文件处理。
8. 在 README 文档导航中加入这些文件。

**验收**：
- `rg -n "BDFL|release owner|module owner" GOVERNANCE.md MAINTAINERS.md` 有命中。
- `AGENTS.md` 包含 MMMail 仓库声明、`frontend-admin`、legacy `frontend-v2`、`scripts/validate-local.sh` 和 `docs/v22-open-source-commercial-spec.md`，且不再命中外部 skill 仓库映射入口。
- `ROADMAP.md` 不泄露独立计费仓库私有实现细节。
- README 链接可点击且文件存在。

### 5.2 GOV-02 DCO + NOTICE + 私有漏洞报告口径

**目标**：避免 CLA 法务成本，同时补齐 Apache 2.0 和安全披露基本信任层。

**输出**：
- `DCO.md`
- `.github/workflows/dco.yml`
- `NOTICE`
- `SECURITY.md` 更新

**步骤**：
1. 添加 `DCO.md`，说明 DCO 取代 CLA、`Signed-off-by` 的含义、常用 Git 修复命令和公开记录边界。
2. 添加 DCO workflow，PR commit 缺 `Signed-off-by` 时失败。
3. 添加 `NOTICE`，写 MMMail copyright、Apache 2.0、上游 Soybean/Admin attribution。
4. 启用 GitHub private vulnerability reporting 后，更新 SECURITY.md 删除“未启用”口径。
5. 增加治理 contract test，确保文件存在且互相链接。

**验收**：
- 根级 contract 检查 `DCO.md`、DCO workflow、NOTICE、SECURITY 文案。
- 不引入 CLA。

**当前落地状态（2026-05-17 / pass-49）**：

- GitHub private vulnerability reporting 已通过 `gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` 验证为 `{"enabled":true}`。
- `SECURITY.md` 和 `SUPPORT.md` 已把首选路径改为 GitHub private vulnerability reporting，保留 public security-contact issue 仅作为无法访问私密报告入口时的无细节 fallback。
- `docs/v22-external-evidence-checklist.md` 和 `scripts/validate-v22-external-evidence.sh` 不再把 GitHub private vulnerability reporting 列为当前 incomplete marker。

### 5.3 FE-01 双前端基线审计

**目标**：用事实确定两个前端的职责，避免基于感觉删除或保留。

**输入**：
- `frontend-admin/package.json`
- `frontend-v2/package.json`
- `.github/workflows/ci.yml`
- `scripts/release-gate.sh`
- `scripts/validate-local.sh`
- `docs/v212-migration-spec.md`
- `README.md`

**步骤**：
1. 统计两个前端的 routes、views、API client、stores、tests、e2e、public routes。
2. 列出 `frontend-v2` 中仍未在 `frontend-admin` 对等覆盖的契约。
3. 列出 CI/release-gate/validate-local 中仍引用 `frontend-v2` 的步骤。
4. 输出 `docs/frontend/v22-frontend-topology-audit.md`。

**验收**：
- 审计文档必须包含“唯一产品前端建议”。
- 审计文档必须列出迁移清单，而不是直接删除 `frontend-v2`。

### 5.4 FE-02 `frontend-admin` 品牌与 metadata 收敛

**目标**：让目标产品前端不再以 Soybean 模板身份对外发布，但保留上游 attribution。

**输入**：
- `frontend-admin/package.json`
- `frontend-admin/README*.md`
- `frontend-admin/packages/**`
- `frontend-admin/scripts/**`
- `frontend-admin/pnpm-lock.yaml`
- `.github/workflows/ci.yml`
- `scripts/validate-local.sh`
- `NOTICE`

**步骤**：
1. 将 package name 从 `soybean-admin` 改为 MMMail 项目名，例如 `mmmail-frontend-admin`。
2. 替换 homepage、bugs、repository、author 等对外 metadata。
3. 替换前端 README 标题、徽章、clone 地址、issue 地址和合作推广文案，避免产品入口继续指向 Soybean 上游。
4. 替换开发者可见的 Soybean CLI / preset / script log 命名，例如 `cac("soybean-admin")`、`preset-soybean-admin`、CI step name、validate-local log 文件名。
5. 保留 MIT license 和 Soybean 上游 attribution 到 NOTICE，不删除上游历史贡献。
6. 若 package name 写入 lockfile，运行 `pnpm --dir frontend-admin install --lockfile-only` 并人工确认 lockfile 只出现预期 metadata 变更。

**验收**：
- `rg -n "soybean-admin|SoybeanAdmin|Soybean v2\\.1\\.2|mmmail-soybean|preset-soybean-admin" frontend-admin/package.json frontend-admin/README*.md frontend-admin/packages frontend-admin/scripts .github scripts README.md` 的每个命中都必须被分类为上游 attribution；产品标题、package metadata、CLI 名称、CI step/log 名不得命中。
- `pnpm --dir frontend-admin typecheck` 通过。

### 5.5 FE-03 `frontend-v2` 契约迁移

**目标**：保留有价值的历史契约，移除双前端运行时依赖。

**迁移对象**：
- public auth/share/system boundary 契约。
- route/access/redirect 契约。
- mail/calendar/drive/pass/docs/sheets 工作流契约。
- visual QA 中仍对 v2.2 有价值的设计 token / a11y / responsive 断言。

**步骤**：
1. 按测试类别把 `frontend-v2/tests/*.mjs` 分为：必须迁移、可归档、可删除。
2. 必须迁移的 contract 改写为读取 `frontend-admin` 文件或主仓文档。
3. 视觉 QA 如仍需要，改为针对 `frontend-admin` 路由运行。
4. 每迁移一类，先让新测试红，再补 `frontend-admin` 对应能力或调整断言，再绿。
5. 全部迁移完成后，跑一次 `bash scripts/release-gate.sh --only 6,7,9,11,12` 确认 gate 不依赖 `frontend-v2`，且无新增 regression。

**验收**：
- `frontend-v2` 不再是 release-gate 通过所必需的产品前端。
- 所有迁移后的 contract 在 `node --test tests/*.test.mjs` 或 `pnpm --dir frontend-admin test:v212` 中运行。
- 不允许通过删除断言来制造通过。

**当前落地状态（2026-05-17 / pass-31）**：

- Selected legacy contracts have moved into `tests/v22-legacy-frontend-contract-migration.test.mjs` and `frontend-admin`.
- Root contract 固定 auth scope、workspace API/header、mail/drive/pass workspace、public share route/API/type/view、settings panel、command-center query 的等价覆盖。
- `frontend-admin` 已新增 mail/pass/drive public share routes、API client、typing 和统一 share view，不再依赖 `frontend-v2` 承载公共分享能力。
- 旧迁移信号脚本已删除，CI legacy migration job 已移除；后续只保留 `scripts/validate-legacy-frontend-v2-freeze.sh` 防止 legacy 目录新增或修改。

### 5.6 FE-04 前端发布入口收敛

**目标**：开源用户和商业用户只看到一个 Web 前端部署路径。

**步骤**：
1. README 改为“产品前端：frontend-admin；legacy reference：frontend-v2”。
2. `docs/ops/install*.md`、deployment runbook 和 release docs 只部署 `frontend-admin`。
3. `scripts/release-gate.sh` 的产品前端步骤只跑 `frontend-admin`。
4. CI 中 `frontend-v2` legacy migration job 在契约迁移完成后移除；`frontend-v2` 不再有产品 CI job。
5. Compose / Helm / Docker image 不发布 `frontend-v2`。

**验收**：
- `rg -n "frontend-v2" README.md docs/ops docs/release scripts .github` 的每个命中都必须标明 legacy/reference，不能再称为当前产品前端。
- release gate 不再在 typecheck/lint/fmt 中无条件要求 `frontend-v2`。
- CI 不再包含 legacy migration job 或旧迁移信号脚本；`frontend-v2` 只由 freeze gate 约束新增 / 修改。
- `docs/superpowers/`、旧 v2.0/v2.1 spec 和旧 release notes 中的 `frontend-v2` 命令必须被视为历史记录，不能作为当前验证命令引用到 README、安装文档、CI 或 release gate。

### 5.7 FE-05 前端收敛验收与归档决策

**目标**：明确 `frontend-v2` 的终态。

**可选终态**：
1. 删除目录：所有契约迁移完成后，从主仓删除 `frontend-v2`。
2. 移到 `archive/frontend-v2`：保留历史参考，但完全退出 CI/release-gate。
3. 保留目录但标记 legacy：只允许 docs 引用，不允许新功能进入。

**推荐**：v2.2 选择 2 或 3，v2.3 删除。原因是当前还有 60 个左右历史测试和 71 个左右 view 文件，直接删除风险高。

**验收**：
- `docs/frontend/v22-frontend-convergence-decision.md` 写明选择。
- 新功能 PR 模板增加“是否误改 legacy frontend-v2”检查项。

**当前落地状态（2026-05-17 / pass-31）**：

- `docs/frontend/v22-frontend-convergence-decision.md` 已选择 v2.2 保留 `frontend-v2` 为 legacy reference，v2.3 删除或归档。
- Selected legacy contracts have moved to root tests and `frontend-admin`; 旧迁移信号脚本与 CI legacy migration job 已退休。
- 历史计划、progress log、旧 v2.0/v2.1 spec 和旧 release notes 可继续保留 `frontend-v2` 记录，但仅作为 archival evidence；新文档和新门禁不能把这些旧命令重新引用为当前 product gate。
- `scripts/validate-legacy-frontend-v2-freeze.sh` 以 `MMMAIL_LEGACY_FRONTEND_BASE_REF` 为基准检查 `frontend-v2` diff，仅允许删除或 rename out，不允许新增或修改文件。
- `scripts/validate-local.sh` 已接入 legacy freeze gate，CI validate job checkout 使用完整历史以保证基准引用可验证。
- `tests/v22-repository-governance-validation-contract.test.mjs` 已用临时 git repo 验证新增 `frontend-v2` 文件会失败。

### 5.8 COMM-01 Edition core

**目标**：建立服务端唯一 edition 判定模型。

**migration 编号约束**：
- 新增 Flyway migration 前，必须同时扫描 `backend/mmmail-server/src/main/resources/db/migration` 和 `backend/mmmail-server/src/main/java/db/migration`。
- 当前仓库 `V33`、`V34`、`V35` 已被 E2EE Java migration 占用；v2.2 commercial migration 从 `V36` 开始。
- v2.2 release gate 需要补一个跨 SQL / Java migration 的版本唯一性检查，不能只检查 `src/main/resources/db/migration/*.sql`。

**输出**：
- `Edition`
- `EditionContext`
- `FeatureCode`
- `FeatureGate`
- `RequiresEdition`
- `RequiresFeature`
- `V36__edition_init.sql`（org 表新增 `edition` 列 + `feature_flag` 表结构）

**当前落地状态（2026-05-17 / pass-32）**：
- 已新增 `com.mmmail.server.commercial` edition / feature 基础类型、注解和 gate。
- 已新增 `V36__edition_init.sql`，并通过 SQL / Java migration 全局连续性检查。
- 已新增 `BackendV22EditionCoreContractTest`，并接入 `scripts/validate-local.sh` 与 CI `Backend v2.2 commercial regression`。
- 当前 v2.2 feature registry 的真实接口覆盖由 `docs/commercial/edition-entitlement-surface.md` 和 `BackendV22CommercialSurfaceCoverageContractTest` 固定，新增 Pro/Business endpoint 必须同步扩展。

**步骤**：
1. 先确认 Flyway SQL / Java migration 最高编号和重复编号，禁止占用已存在的 `V33`、`V34`、`V35`。
2. 定义 `FREE / PRO / BUSINESS` enum。
3. 定义 feature code registry，禁止散落字符串。
4. 定义 org-level edition source：Free default、license state、billing subscription。
5. 建立 controller/service 层注解和拦截器。

**验收**：
- SQL / Java migration 版本号全局唯一，`V36__edition_init.sql` 不与现有 Java migration 冲突。
- Free 核心能力无 license 仍可用。
- Pro/Business endpoint 对 Free 返回明确 403 和错误码。
- 错误码写入 API spec 和 i18n。

### 5.9 COMM-02 License verifier

**目标**：主仓能验证 license，但永远不能签发真实 license。

**输出**：
- `LicenseClaims`
- `LicenseKeyVerifier`
- `LicenseStateRepository`
- `LicenseSyncService`
- `contracts/license/license-claims.schema.json`
- `V37__license_init.sql`（`license_state` 表：orgId / claims / status / syncedAt）

**当前落地状态（2026-05-17 / pass-32）**：
- 已实现 `header.payload.signature` 格式、Ed25519 公钥验签、claims 校验、过期 / 篡改 / org mismatch / 错误公钥 / 格式错误的显式失败原因。
- 已新增 `LicenseStateRepository` 接口、JDBC repository 和 `LicenseSyncService`，只保存验签通过的 ACTIVE license state。
- 已新增 `contracts/license/license-claims.schema.json` 和 `V37__license_init.sql`。
- 已新增 `BackendV22LicenseVerifierContractTest`，并接入 `scripts/validate-local.sh` 与 CI `Backend v2.2 commercial regression`。
- `docs/commercial/edition-entitlement-surface.md` 明确 license status/upload 是升级入口，不要求已付费 entitlement，避免 Free 自托管组织无法上传 license。
- 主仓仍不包含 license 私钥或真实签发逻辑；签发留给独立私有计费仓。

**步骤**：
1. 定义 `header.payload.signature` 格式。
2. 使用 Ed25519 公钥验签。
3. 校验 `orgId`、`edition`、`seats`、`features`、`issuedAt`、`expiresAt`。
4. 实现篡改、过期、错误 org、错误公钥、格式错误的明确失败。
5. 到期后只关闭付费能力，不影响 Free。

**验收**：
- SQL / Java migration 版本号全局唯一，`V37__license_init.sql` 连续跟在 `V36` 后。
- 单测覆盖成功、篡改、过期、格式错误、org mismatch。
- secret scan 确认没有私钥。

### 5.10 COMM-03 BillingProvider + webhook

**目标**：主仓能接收外部计费状态，但不实现支付网关。

**输出**：
- `BillingProvider`
- `NoopBillingProvider`
- `BillingProviderRegistry`
- `BillingWebhookController`
- `SubscriptionStateMachine`
- `contracts/billing/webhook-event.schema.json`
- `docs/billing/webhook-signature.md`
- `V38__billing_webhook_init.sql`（`billing_webhook_event` 幂等表 + `org_subscription_state` 表）

**当前落地状态（2026-05-17 / pass-32）**：
- 已定义 `BillingProviderType.NONE` / `WEBHOOK`，`NoopBillingProvider` 明确 `supportsPaidState=false`，避免 `none` 被误认为付费成功。
- 已实现 `BillingWebhookVerifier`：`X-MMMail-Billing-Signature: v1=<hex-hmac-sha256>`、5 分钟时间窗、密钥缺失 / 签名错误 / 版本不支持 / 时间窗失败均显式失败。
- 已实现 `BillingWebhookEventRepository` / `SubscriptionStateRepository` / `SubscriptionStateMachine`，`eventId` 幂等，重复 event 不重复写订阅状态。
- 已新增 `BillingWebhookController`、`contracts/billing/webhook-event.schema.json`、`docs/billing/webhook-signature.md` 和 `V38__billing_webhook_init.sql`。
- 已新增 `MMMAIL_BILLING_WEBHOOK_SECRET` 到 `application.yml`、`.env.example`、`config/backend.env.example`、Compose 和 `validate-local` required env gate。
- 已新增 `BackendV22BillingWebhookContractTest`，并接入 `scripts/validate-local.sh` 与 CI `Backend v2.2 commercial regression`。
- `docs/commercial/edition-entitlement-surface.md` 明确 billing webhook 通过 HMAC / provider state 认证，不走用户 edition，且 `none` provider 不能 apply paid state。
- 主仓仍不做真实收款、退款、客户门户或 license 签发。

**步骤**：
1. 定义 provider enum：`none`、`webhook`。
2. 定义 webhook payload：eventId、orgId、plan、status、occurredAt、signatureVersion。
3. 实现签名校验、时间窗、幂等表。
4. 实现状态机：trial、active、past_due、canceled、expired。
5. 失败事件写安全审计。

**验收**：
- SQL / Java migration 版本号全局唯一，`V38__billing_webhook_init.sql` 连续跟在 `V37` 后。
- 重放 event 不重复变更。
- 签名失败不吞错、不 fallback。
- `none` 不代表付费成功。

### 5.11 COMM-04 Backend entitlement enforcement

**目标**：安全边界在后端，而不是前端按钮隐藏。

**当前落地状态（2026-05-17 / pass-32）**：
- 已新增 `EditionContextResolver` / `JdbcEditionContextResolver`，edition 来源按订阅状态、active license 和 `org_workspace.edition` fallback 解析；订阅状态优先，非付费状态解析为 Free，不会被 active license 静默覆盖为 paid。
- 已把 `@RequiresEdition` / `@RequiresFeature` 接入 `AuthorizationAnnotationInterceptor`；拦截器只解析注解和 active org，实际商业化授权、失败错误和审计由 `CommercialAuthorizationGate` 执行。
- 已显式标注商业化组件生产构造器注入入口，避免 Spring 在测试构造器和生产构造器之间出现歧义。
- Free 调 Business feature 时返回 `V2_ENTITLEMENT_REQUIRED`，message 包含 `requiredEdition`、`currentEdition`、`upgradeAction`。
- 缺少 org context 时显式失败为 `upgradeAction=select-org`，不默认放行。
- 失败访问写入 `COMMERCIAL_ENTITLEMENT_DENIED` audit event，包含 path、requiredEdition、currentEdition、upgradeAction 和 feature code。
- 已新增 `BackendV22EntitlementEnforcementContractTest` 和 `BackendV22CommercialSurfaceCoverageContractTest`，并接入 `scripts/validate-local.sh` 与 CI `Backend v2.2 commercial regression`。
- `docs/commercial/edition-entitlement-surface.md` 已列出当前 v2.2 commercial endpoints：OIDC config / login / callback、audit JSONL export、DSR export / erasure / job status、license upgrade path、billing webhook 和 billing readiness。
- 当前 v2.2 Business API 均有服务端 gate 或明确 public callback / upgrade / webhook 例外；不把 v2.1.2 GA API retroactively 改成付费。

**步骤**：
1. 给 Pro/Business API 添加 `@RequiresEdition` 或 `@RequiresFeature`。
2. 拦截器读取 org context + edition context。
3. 403 响应包含错误码、requiredEdition、currentEdition、upgradeAction。
4. 审计失败访问。

**验收**：
- contract test 覆盖 Free 调 Business API。
- 前端隐藏按钮不作为验收依据。

### 5.12 COMM-05 Frontend entitlement surfaces

**目标**：在唯一前端 `frontend-admin` 中提供 license / billing / upgrade 体验。

**页面**：
- Settings > License。
- Admin > Billing。
- Business feature blocked state。
- OIDC setup entry。

**步骤**：
1. 新增 license status API client。
2. 新增 license upload / paste UI。
3. 新增 billing status 展示，只展示外部计费状态，不做真实付款。
4. EntitlementGate 接后端 requiredEdition 响应。
5. 新增约 30 个 i18n key（覆盖 license 页 / billing 状态 / blocked state / OIDC 入口文案），zh-cn/en-us/zh-tw 三语同步，进入 `check:i18n` gate。

**验收**：
- a11y axe 覆盖 license/billing/blocked state。
- 无 mock 支付成功路径。
- 三语 key 无硬编码文案。

**当前落地状态（2026-05-17 / pass-32）**：
- 已新增 `/api/v2/billing/license/status` 和 `/api/v2/billing/license`，上传路径必须使用 `MMMAIL_LICENSE_PUBLIC_KEY` 配置的 Ed25519 X.509 公钥验签；缺 org 或缺公钥均显式失败，不返回伪成功。
- 已新增 `frontend-admin` license API client、Settings License 面板、Admin Billing 外部计费状态展示和 EntitlementGate 后端授权详情展示。
- 已新增 zh-cn / en-us / zh-tw 的 license、billing external status、blocked state 文案 key，并由 `frontend-admin/tests/v22-commercial-entitlement-surfaces-contract.test.mjs` 固定无 mock 支付成功文案。
- 已新增 Admin OIDC setup entry，通过 `EntitlementGate` 按 `oidc.sso` Business 权益拦截；后端 BUS-01 主仓 token/session baseline 已落地，live Keycloak e2e 前仍不宣称 done。
- 已新增 `frontend-admin/e2e/v22-commercial-a11y.spec.ts`，用 `axe-core` 覆盖登录、注册、Settings License、Admin Billing status 和 OIDC blocked state 的 serious/critical 违规；该 spec 通过 `test:e2e` 进入现有 CI / validate-local 浏览器门禁。
- a11y gate 同步修复：test mode 显式关闭 Vue DevTools、Admin 页面改为单根模板、登录页图标按钮补 accessible name、登录页说明文字和 billing statistic label 对比度提升。
- `BackendV22CommercialSurfaceCoverageContractTest` 固定前后端 surface 边界：license upload 是升级入口，Admin Billing 只展示外部状态 / draft，不提供 live payment 或 paid-success mock。

### 5.13 BUS-01 OIDC SSO

**目标**：Business 用户能接入主流 IdP。

**步骤**：
1. 定义 org-level OIDC config。
2. 使用 Spring Security OAuth2 Client。
3. 实现 state、nonce、PKCE、redirect URI 白名单。
4. 以 Keycloak 作为**硬验收 IdP**：OIDC e2e 必须跑通（包括 login / callback / session / logout / token refresh）。
5. 国内 IdP（飞书 OIDC / 钉钉 OIDC / 企业微信 OIDC）至少验证 callback URL 可达并写入兼容性文档；不承诺深度适配（如组织架构同步、SCIM 联动）。

**验收**：
- Keycloak e2e 全链路绿。
- 错误 redirect/state/nonce 被正确拒绝。
- threat model 增补 OIDC attack surface 段位。
- 飞书 OIDC callback URL 至少在文档中有可达性验证记录。

**当前落地状态（2026-05-17 / pass-30）**：

- `V39__oidc_sso_init.sql` 新增 `org_oidc_config` 和 `oidc_auth_state`，并进入 `docs/compliance/data-inventory.yaml`。
- `OidcSsoController` 暴露 org config、login start 和 callback endpoint；config endpoint 通过 `CommercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.OIDC_SSO)` 强制 Business 权益。
- `OidcStateService` 生成 single-use `state`、`nonce` 和 PKCE `S256` `codeVerifier`，并拒绝未列入 allowlist 的 post-login redirect。
- `OidcSsoService` 在 callback 中通过 `mmmail.oidc.callback` span 暴露 OIDC callback 观测点，并在 state 缺失、过期、重复消费、redirect mismatch、code 缺失、token response 缺失、ID token issuer / audience / nonce mismatch 或本地用户不存在时显式失败。
- `RestClientOidcTokenExchangeClient` 使用真实 IdP token endpoint 交换 authorization code，提交 `grant_type=authorization_code`、`code`、`redirect_uri`、`client_id`、`client_secret` 和 `code_verifier`；`client_secret_ref` 只解析环境变量，不把 secret 写入数据库。
- `DefaultOidcIdTokenValidator` 使用 Spring Security JWT decoder 校验 ID token signature，并显式校验 issuer、audience、nonce、subject 和 email。
- `AuthServiceOidcSessionIssuer` 将 verified email 映射到现有 active `user_account`，复用 `AuthService` 的 JWT、refresh session、refresh rotation 和 logout 路径；不在主仓静默自动创建用户。
- `docs/security/threat-model.md` 已补 OIDC attack surface，`docs/commercial/oidc-sso.md` 已记录 Keycloak hard acceptance、现有用户映射边界与飞书 / 钉钉 / 企业微信 callback URL 可达性口径。
- `BackendV22OidcSsoContractTest` 已进入 `scripts/validate-local.sh` 和 CI。
- 剩余硬验收：live Keycloak login / callback / session / logout / token refresh e2e，以及对应真实 route/error trace evidence。

### 5.14 BUS-02 Audit JSONL export

**目标**：Business 用户能导出审计证据。

**步骤**：
1. 定义 audit export API：orgId、time range、event types、cursor。
2. 输出 JSONL。
3. 支持对象存储归档配置。
4. 大文件异步任务化。

**验收**：
- 13 类事件都有导出样例。
- 无权限用户不能跨 org 导出。

**当前落地状态（2026-05-17 / pass-23）**：
- 已新增 `OrgAuditExportController`：`GET /api/v2/orgs/{orgId}/audit/events/export`，返回 `application/x-ndjson` 和 `.jsonl` 附件文件名。
- 已新增 `OrgAuditQueryService.exportJsonlEvents`：支持 `limit`、`eventTypes`、`cursor`、`fromDate`、`toDate`、`sortDirection`，每行输出 `schemaVersion`、`source`、`id`、`cursor`、`orgId`、`actorId`、`actorEmail`、`eventType`、`targetType`、`targetId`、`severity`、`ipAddress`、`detail`、`createdAt`。
- 已新增 `AuditService.listByOrgForExport`：支持多 event types、cursor 后续页、日期范围和稳定排序。
- 已新增 `CommercialAuthorizationGate.enforceFeature`，v2 audit export 在返回内容前强制 `FeatureCode.AUDIT_EXPORT`，Free / Pro 不会绕过 Business gate。
- 已新增 `docs/compliance/audit-export.md`，记录 API、JSONL schema、SIEM mapping、retention、安全边界和 validation gate。
- 已新增 `BackendV22AuditExportContractTest`，并接入 `scripts/validate-local.sh` 与 CI `Backend v2.2 commercial regression`。
- 对象存储归档和大文件异步任务仍是后续扩展，不在当前 v2.2 主仓已交付能力中宣传。

### 5.15 BUS-03 DSR + data inventory

**目标**：能处理用户数据导出、删除、匿名化。

**输出**：
- `docs/compliance/data-inventory.yaml`
- DSR export job。
- DSR delete/anonymize job。
- migration inventory gate。

**步骤**：
1. 为核心表声明 owner、retention、export strategy、delete strategy。
2. 实现 DSR job 状态：queued、running、completed、failed。
3. 删除失败必须返回具体表和原因。
4. 新 Flyway migration 未更新 inventory 时 gate 失败。

**验收**：
- 至少覆盖 users、orgs、mail metadata、drive metadata、audit event。
- 不删除法定保留所需审计时，必须匿名化并写明原因。

**当前落地状态（2026-05-17 / pass-24）**：
- 已新增 `DsrRequestController`：`POST /api/v2/orgs/{orgId}/dsr/export`、`POST /api/v2/orgs/{orgId}/dsr/erasure` 和 `GET /api/v2/orgs/{orgId}/dsr/jobs/{jobId}`。
- DSR API 在排队前强制 `CommercialAuthorizationGate.enforceFeature(..., FeatureCode.DSR_REQUESTS)`，Free / Pro 不能绕过 Business gate。
- 已新增 `DsrRequestService`：将 DSR export / erasure 转成 `JobRunType.DSR_EXPORT` / `JobRunType.DSR_ERASURE`，写入 `platform_job_run`，公开状态映射为 `queued`、`running`、`completed`、`failed`。
- 已新增 `DsrExecutionService` 和 `DsrJobHandlerConfig`：生产 runner 能注册 typed handlers；export job 按 inventory 查询 subject rows；erasure job 按 inventory 执行 `SOFT_DELETE`、`DELETE_ROWS`、`ANONYMIZE` 或显式 `RETAIN`。
- 已新增 `docs/compliance/dsr.md` 和 `docs/compliance/data-inventory.yaml`；当前 inventory 覆盖 schema / Flyway 声明的 113 个表，声明 owner、retention、subjectRef、export 和 delete 策略。
- 已新增 `scripts/validate-dsr-inventory.mjs`，新 Flyway/schema 表没有 inventory 条目或缺字段时失败；该 gate 已进入 `scripts/validate-local.sh`、`scripts/release-gate.sh` step 16、CI root tests 和 `tests/v22-dsr-inventory-contract.test.mjs`。
- `scripts/release-gate.sh` step 17 执行 `scripts/validate-legacy-frontend-v2-freeze.sh`，避免只跑 release-gate 时绕过 legacy frontend freeze。
- 保留型数据（security / audit / financial / governance）不会伪装成已物理删除；inventory 必须写明 retain 原因，并对可识别字段使用 anonymize 策略。

### 5.16 DEP-01 Helm chart

**目标**：自托管商业部署不只依赖 docker-compose。

**输出**：
- `helm/mmmail/Chart.yaml`
- `helm/mmmail/values.yaml`
- templates for backend、frontend-admin、mysql/redis external config、ingress、secret。

**步骤**：
1. chart 只发布 `frontend-admin`。
2. values 覆盖 license、billing provider、OIDC、audit export、OTEL。
3. secret 使用 Kubernetes secret，不把敏感值写 values 默认值。
4. 文档给出最小部署和外部 MySQL/Redis 部署。

**验收**：
- `helm lint helm/mmmail` 通过。
- `helm template` 输出包含 backend 和 frontend-admin，不包含 frontend-v2。

**当前落地状态（2026-05-16 / pass-18）**：
- 已新增 `helm/mmmail` chart，默认只渲染 backend / frontend-admin，不包含 MySQL / Redis workload，也不包含 `frontend-v2`。
- `values.yaml` 已覆盖 license、billing provider、OIDC、audit export、OTEL、外部 MySQL、外部 Redis、Secret key 映射。
- `docs/ops/helm.md` 已给出最小 values、Secret 创建、lint/template/install 命令。
- `scripts/validate-helm-chart.sh` 已执行 `helm lint`、`helm template`，并检查渲染结果包含 backend / frontend-admin 且不含 `frontend-v2`。

### 5.17 DEP-02 Image publishing

**目标**：tag 可生成可部署镜像。

**步骤**：
1. 配置 GHCR multi-arch buildx。
2. 镜像包括 backend 和 frontend-admin。
3. release notes 写入 image digest。
4. 不发布 frontend-v2 镜像。

**验收**：
- CI dry run 或 tag workflow contract 证明 image matrix。

**当前落地状态（2026-05-16 / pass-19）**：
- 已新增 `.github/workflows/images.yml`，tag `v*` 触发 GHCR push，`workflow_dispatch` 触发 dry run。
- workflow matrix 仅包含 `backend/Dockerfile` 与 `frontend-admin/Dockerfile`，平台为 `linux/amd64,linux/arm64`。
- workflow 不包含 `frontend-v2` 或 `mmmail-frontend-v2`。
- `docs/release/release-notes-template.md` 已新增 Image Digests 段，要求记录 `mmmail-backend` 和 `mmmail-frontend-admin` digest。
- `tests/v22-image-publishing-contract.test.mjs` 已固定 workflow、release notes 和 gate 接入。

### 5.18 OBS-01 OpenTelemetry

**目标**：商业化后能定位慢请求、webhook、license sync 问题。

**span 范围**：
- HTTP request。
- DB query 或 repository operation。
- Redis operation。
- billing webhook。
- license verification。
- OIDC callback。

**验收**：
- OTEL disabled 时无额外失败。
- OTEL enabled 时 trace id 进入日志。

**当前落地状态（2026-05-17 / pass-26）**：

- `backend/mmmail-server/pom.xml` 已引入 `micrometer-tracing-bridge-otel` 和 `opentelemetry-exporter-otlp`。
- `backend/mmmail-server/src/main/resources/application.yml` 已通过 `MMMAIL_OTEL_ENABLED=false`、`MMMAIL_OTEL_SAMPLING_PROBABILITY=1.0`、`OTEL_SERVICE_NAME=mmmail-server` 和 `OTEL_EXPORTER_OTLP_ENDPOINT=` 暴露 runtime 配置；`.env.example`、`config/backend.env.example`、Helm configmap 与 values 同步。
- `RuntimeTraceService` 用 Micrometer Observation 统一创建 span，并在失败时记录 error 后原样抛出，不引入 fallback success。
- `RequestTracingFilter` 输出 `mmmail.http.request`，保持 existing `traceId` / `requestId` MDC 供日志关联。
- `JdbcBillingWebhookEventRepository`、`JdbcLicenseStateRepository` 输出 `mmmail.db.operation`。
- `DrivePublicShareRateLimiter` 的 public share Redis limiter 输出 `mmmail.redis.operation`。
- `BillingWebhookService` 输出 `mmmail.billing.webhook`。
- `LicenseSyncService` 输出 `mmmail.license.verify`。
- `OidcSsoService` callback flow 输出 `mmmail.oidc.callback`，并在 state 缺失、过期、重复消费、redirect mismatch 或 code 缺失时显式失败。
- `docs/observability/opentelemetry.md` 说明默认关闭、OTLP 启用方式、Helm values、span surface、log correlation 和 OIDC 边界。
- `BackendV22OpenTelemetryContractTest` 已进入 `scripts/validate-local.sh` 和 CI backend job。
- `BackendV22OidcSsoContractTest` 已固定 OIDC callback 专项 span 入口；live Keycloak e2e 中的真实 route/error trace evidence 仍随 BUS-01 done 前补齐。

### 5.19 OBS-02 SLI/SLO docs

**目标**：不承诺 24/7 SLA，但内部有质量目标。

**SLI**：
- API p99。
- 5xx rate。
- billing webhook success rate。
- license verification failure rate。
- OIDC callback failure rate。

**验收**：
- 文档明确“内部目标，不是对外 SLA”。

**当前落地状态（2026-05-16 / pass-21）**：
- 已新增 `docs/observability/sli-slo.md`。
- 文档覆盖 API p99、5xx rate、billing webhook success rate、license verification failure rate、OIDC callback failure rate。
- 文档明确这些指标是 internal target、operator diagnostic，不是 public SLA 或 contractual commitment。
- `tests/v22-observability-docs-contract.test.mjs` 已固定 required SLI 和 no false SLA 口径。

### 5.20 GATE-01 v2.2 release gate expansion

**目标**：所有 P0 不是靠人工记忆验收。

**新增 gate**：
- governance-files。
- dco。
- frontend-convergence。
- frontend-admin-branding。
- legacy-contract-migration。
- legacy-frontend-freeze。
- license-smoke。
- billing-webhook-smoke。
- oidc-smoke。
- audit-export-smoke。
- dsr-inventory。
- helm-lint。
- image-workflow-contract。
- sbom-license。
- secret-scan。
- a11y-axe。

**验收**：
- `bash scripts/release-gate.sh` 输出新增 step。
- 最终 `git diff --exit-code` 仍保留。

**当前落地状态（2026-05-17 / pass-81）**：
- `tests/v22-legacy-frontend-contract-migration.test.mjs` 已作为 `legacy-contract-migration` gate，证明 selected legacy contracts 已迁入 root tests / `frontend-admin`，并证明旧迁移信号脚本和 CI job 不会被重新引入。
- `BackendV22CommercialSurfaceCoverageContractTest` 已作为 commercial surface coverage gate，证明当前 v2.2 OIDC / audit / DSR Business API 有服务端 feature gate，并证明 license upgrade path 与 billing webhook 是显式例外而非 silent paid fallback。
- 已新增 `scripts/generate-sbom-license-report.mjs`，默认输出到系统临时目录 `mmmail-supply-chain`；可用 `MMMAIL_SUPPLY_CHAIN_REPORT_DIR` 显式指定目录，避免在仓库内留下 `artifacts/`。
- `scripts/validate-local.sh` 已新增 supply-chain 报告生成检查，并把脚本列入 required files。
- `scripts/release-gate.sh` 已新增 step 13 `sbom-license`。
- `scripts/validate-helm-chart.sh` 已新增 `helm lint` / `helm template` 检查，并作为 step 14 `helm-lint` 接入 release-gate。
- `tests/v22-image-publishing-contract.test.mjs` 已作为 step 15 `image-workflow-contract` 接入 release-gate。
- `scripts/validate-dsr-inventory.mjs` 已作为 step 16 `dsr-inventory` 接入 release-gate，并通过 `tests/v22-dsr-inventory-contract.test.mjs` 固定本地 / CI / spec wiring。
- `scripts/validate-legacy-frontend-v2-freeze.sh` 已作为 step 17 `legacy-frontend-freeze` 接入 release-gate；CI release-gate checkout 使用 `fetch-depth: 0`，确保 `origin/main` 基准可验证。
- `scripts/validate-v22-external-evidence.sh` 仍是手动外部证据 verifier，不进入默认 `validate-local`、CI 或 release-gate 绿色路径，也不通过 root governance tests 间接执行。
- 外部 verifier 完成态会校验 OIDC backend/frontend commit、image evidence commit、private billing evidence 的 Public MMMail commit、origin release tag commit 和 `origin/main` / `origin/release/*` branch containment，防止不同代码版本的 evidence package 混用。
- `tests/v22-repository-governance-validation-contract.test.mjs` 已新增活跃源码 500 行 allowlist 扫描；生成类型和历史超限债务显式例外，新超限活跃源码会失败；`.gitignore` 同步忽略本地 agent / validation 产物。
- `BackendV22DsrContractTest` 已进入 `scripts/validate-local.sh` 的 v2.2 commercial backend regression 和 CI backend job。
- `.github/workflows/ci.yml` 已新增 “Generate SBOM and dependency license report” 与 `actions/upload-artifact@v7`，从 `${{ runner.temp }}/supply-chain/` 上传报告。
- `.github/workflows/ci.yml` 使用 `MMMAIL_PNPM_VERSION=10.5.0`，和 `frontend-admin/package.json` 的 pnpm engine 下限一致；`.github/workflows/ci.yml`、`.github/workflows/images.yml`、`.github/workflows/dco.yml` 已升级到当前 Node 24 兼容 action major。
- `docs/superpowers/specs/2026-05-15-v212-decision-log.md`、`2026-05-15-v212-module-design-coverage.md`、`2026-05-15-collab-sheets-board-design.md` 是 root contract 读取的 fixture 文档，必须跟随测试一起进入 Git 跟踪。
- `.dockerignore` 明确排除 `frontend-admin/node_modules`、`frontend-admin/dist`、`.claude`、`.tools`、`artifacts` 等本地产物和缓存目录，防止 image workflow 上传无关 GB 级 context。
- `.github/workflows/ci.yml` 已在 validate / release-gate job 安装 Helm。
- `tests/v22-repository-governance-contract.test.mjs`、`tests/v22-repository-governance-validation-contract.test.mjs`、`tests/v22-deployment-helm-contract.test.mjs` 与 `tests/v22-image-publishing-contract.test.mjs` 已覆盖脚本存在、门禁接入和产物可解析。

### 5.21 GTM-01 Pricing / support / trademark boundaries

**目标**：商业化叙述不夸张、不违法、不误导开源用户。

**输出**：
- `docs/commercial/pricing-boundaries.md`
- `docs/commercial/support-policy.md`
- `docs/commercial/trademark-policy.md`

**验收**：
- 不写虚假 SLA。
- 不声称真实支付已上线，除非独立计费仓库完成。
- 明确 Free 自托管不被削弱。

**当前落地状态（2026-05-16 / pass-20）**：
- 已新增 `docs/commercial/pricing-boundaries.md`，明确 no public price is committed、real payment processing is not live、license signing private keys stay outside this public repository、v2.1.2 GA capabilities remain Free。
- 已新增 `docs/commercial/support-policy.md`，公开响应目标为 best effort 且 not SLA commitments，安全问题仍走 `SECURITY.md`。
- 已新增 `docs/commercial/trademark-policy.md`，允许事实性引用和 fork attribution，但不得制造 official MMMail 或 no endorsement 混淆。
- `README.md`、`SUPPORT.md` 与 `scripts/validate-local.sh` 已加入 discoverability / required-file 入口。
- `tests/v22-commercial-boundaries-contract.test.mjs` 已固定 no false SLA、no live payment、Free not weakened、no endorsement。

---

## 6. Free / Pro / Business 拆分矩阵

| 模块 / 能力 | Free | Pro | Business | Hosted 运维服务 |
|---|:-:|:-:|:-:|:-:|
| Mail / Calendar / Drive / Suite Shell / Settings / Auth | ✅ | ✅ | ✅ | ✅ |
| v2.1.2 已 GA 能力 | ✅ | ✅ | ✅ | ✅ |
| Docs / Sheets / Pass 当前 Beta 能力 | ✅ | ✅ | ✅ | ✅ |
| Prometheus / Grafana 基础指标 | ✅ | ✅ | ✅ | ✅ |
| 本地审计事件写入 | ✅ | ✅ | ✅ | ✅ |
| 基础组织 / 成员管理 | ✅ | ✅ | ✅ | ✅ |
| 团队治理增强（v2.2 新增） | ⛔ | ✅ | ✅ | ✅ |
| License key 离线解锁 | n/a | ✅ | ✅ | n/a |
| 自定义 RBAC（v2.3） | ⛔ | 🔄 v2.3 | 🔄 v2.3 | 🔄 v2.3 |
| OIDC SSO | ⛔ | ⛔ | ✅ | ✅ |
| SCIM 2.0（v2.3） | ⛔ | 🔄 v2.3 | 🔄 v2.3 | 🔄 v2.3 |
| SAML 2.0（v2.3） | ⛔ | ⛔ | 🔄 v2.3 | 🔄 v2.3 |
| 审计 JSONL / SIEM 导出 | ⛔ | ⛔ | ✅ | ✅ |
| DSR 导出 / 删除 / 匿名化 | ⛔ | ⛔ | ✅ | ✅ |
| 状态页（v2.3） | ⛔ | 🔄 v2.3 | 🔄 v2.3 | 🔄 v2.3 |
| 多区域 / 数据驻留 / BYOK | ⛔ | ⛔ | v2.4+ | v2.4+ |
| 商业计费、发票、退款、客户门户 | ⛔ | 独立计费仓库 | 独立计费仓库 | 独立计费仓库 |
| 托管运维、备份、升级、监控代管 | ⛔ | ⛔ | 可选 | ✅ |

原则：

- Free 是完整可用的自托管协作套件，不做“演示版”。
- Pro/Business 只解锁 v2.2 后新增能力，重点是团队治理、企业准入和商业支持。
- Hosted 解决“我不想运维”，不是用来削弱自托管。
- 所有新前端入口只进入 `frontend-admin`。

---

## 7. 路线图

### 7.1 v2.2 主仓 GA 计划

| Sprint | 周期 | 范围 | 主要验收 |
|---|---:|---|---|
| Sprint 0 | 1 周 | GOV-01/02 + FE-01 | 治理路径闭环；双前端审计完成 |
| Sprint 1 | 2 周 | FE-02/03/04/05 | `frontend-admin` 成为唯一产品前端，`frontend-v2` 退出发布入口 |
| **🔴 Checkpoint** | — | scoped local gate（`--skip 1,5,8`，仅本地排查）| `frontend-v2` 退出产品 gate 的局部验收通过；无 skip 的完整 release-gate 通过后才放行 Sprint 2 |
| Sprint 2 | 2 周 | COMM-01/02/03/04 + BILL-01 | Edition/license/webhook 服务端闭环 |
| Sprint 3 | 2 周 | COMM-05 + BUS-01/02/03 | 唯一前端展示 license/billing/Business；企业准入 P0 闭环 |
| Sprint 4 | 1.5 周 | DEP-01/02/03 + OBS-01/02 | Helm、image、OTEL、SLI 可验收 |
| Sprint 5 | 0.5 周 | GATE-01 + GTM-01 + release docs | gate 失败即阻断 release；文档不误导 |

主仓 P0 约 38.4 PD。Sprint 1 放 2 周（FE-03 契约迁移 4 PD 为最长单任务）。独立计费仓库 12 PD 可与 Sprint 2-4 并行；若独立仓库未完成，v2.2 主仓仍可发布 OSS launch + commercial-ready adapter，但不能宣称真实支付闭环已上线。

### 7.2 v2.3

- 删除或外部归档 `frontend-v2`，如果 v2.2 选择保留 legacy 目录。
- SCIM 2.0。
- SAML 2.0。
- 自定义 RBAC。
- 状态页。
- k8s 裸 manifest / Terraform。
- SDK 生成和客户成功 runbook。

### 7.3 v2.4+

- BYOK。
- 多区域 / 数据驻留。
- SOC2 / ISO 27001 文档框架。
- Marketplace / CLI / 移动端壳。

---

## 8. Release Gate 升级要求

v2.2 不允许只靠“文档存在”验收。release gate 至少新增：

| Gate | 命令方向 | 必须证明 |
|---|---|---|
| governance-files | Node/bash contract | AGENTS / CODE_OF_CONDUCT / GOVERNANCE / ROADMAP / MAINTAINERS / SUPPORT / DCO.md / NOTICE / EditorConfig / Git attributes 存在且链接互通 |
| active-source-size-guardrail | `node --test tests/v22-repository-governance-validation-contract.test.mjs` | 活跃源码文件默认不超过 500 行；生成类型和历史超限债务必须显式列入 allowlist；CONTRIBUTING / PR template 提醒新 PR 不得扩大超限文件 |
| dco | GitHub Actions lint | DCO.md 和 DCO workflow 存在，PR 贡献路径要求 `Signed-off-by` |
| frontend-convergence | Node contract | README/docs/scripts/CI 不再把两个前端写成同等产品入口 |
| frontend-admin-branding | Node contract | `frontend-admin/package.json` 不再以 Soybean 上游项目 metadata 对外发布 |
| legacy-contract-migration | `node --test tests/v22-legacy-frontend-contract-migration.test.mjs` | `frontend-v2` selected legacy contracts 已进入 `frontend-admin` 或 root tests，旧迁移信号脚本和 CI job 不得重新出现 |
| legacy-frontend-freeze | `bash scripts/validate-legacy-frontend-v2-freeze.sh` | `git diff $MMMAIL_LEGACY_FRONTEND_BASE_REF...HEAD -- frontend-v2` 只允许删除或迁移，不允许新增或修改文件 |
| commercial-backend-contracts | `BackendV22EditionCoreContractTest,BackendV22LicenseVerifierContractTest,BackendV22EntitlementEnforcementContractTest,BackendV22CommercialSurfaceCoverageContractTest,BackendV22LicenseManagementApiContractTest,BackendV22AuditExportContractTest,BackendV22DsrContractTest` | Edition / feature 基础类型、license 验签成功 / 失败 / 过期 / 篡改 / org mismatch、license status/upload API、commercial endpoint surface coverage、audit JSONL export、DSR export/erasure job、Free 访问 Business feature 的 403 和失败审计均有明确行为，并进入 `validate-local` 和 CI |
| billing-webhook-smoke | `BackendV22BillingWebhookContractTest` | provider registry、HMAC 签名、时间窗、幂等、状态流转和 `none` 不代表付费成功 |
| otel-runtime-trace | `BackendV22OpenTelemetryContractTest` | OTel 默认关闭、OTLP 配置、HTTP/DB/Redis/webhook/license span wiring、错误显式传播、README/spec/CI/validate-local discovery |
| oidc-smoke | `BackendV22OidcSsoContractTest` + backend/e2e | OIDC config、state、nonce、redirect allowlist、PKCE、token exchange wiring、ID token validation wiring、session issuance wiring 通过；live Keycloak login/callback/session/logout/token refresh e2e 才能把 BUS-01 标为 done |
| audit-export-smoke | `BackendV22AuditExportContractTest` | Business `audit.export` gate 生效，JSONL 字段、eventTypes、cursor 和 SIEM 文档可验证 |
| dsr-inventory | `node scripts/validate-dsr-inventory.mjs` + `tests/v22-dsr-inventory-contract.test.mjs` | 新 migration 必须声明 data inventory，且每个表必须有 owner / retention / subjectRef / export / delete |
| migration-version-unique | bash/Node contract | SQL 和 Java Flyway migration 版本号全局唯一，v2.2 新增 migration 从 `V36` 连续推进 |
| helm-lint | `bash scripts/validate-helm-chart.sh` | chart 可 lint/template，只发布 backend 和 frontend-admin，不包含 frontend-v2 |
| image-workflow-contract | `node --test tests/v22-image-publishing-contract.test.mjs` | tag workflow 使用 GHCR buildx multi-arch，只发布 backend 和 frontend-admin，并要求 release notes 记录 digest |
| sbom-license | CycloneDX / SPDX | 产物生成且进入 release artifact |
| secret-scan | `bash scripts/security-secret-scan.sh` | 主仓无支付私钥、商户证书、license 私钥；扫描范围遵循 Git tracked + untracked non-ignored，避免依赖目录和临时 worktree 假阳性 |
| a11y-axe | Playwright | 登录、注册、license、billing、OIDC 关键页无严重 a11y 问题 |

---

## 9. 风险登记

| 编号 | 风险 | 触发条件 | 缓解 |
|---|---|---|---|
| O-1 | 单维护者风险 | BDFL 无法及时响应安全或商业问题 | GOVERNANCE 写明代理维护路径；商业 SLA 不夸口 |
| O-2 | OSS 信任受损 | 把已 GA 能力移到 Pro/Business | “v2.1.2 GA 永远 Free”写入 GOVERNANCE 和定价页 |
| O-3 | 支付合规泄漏到主仓 | Adapay 证书、私钥、商户资料被提交 | 独立仓库 + secret scan + 主仓 contract fixture 不含真实密钥 |
| O-4 | license 体验伤害自托管 | 到期后影响 Free 核心能力 | 只关闭付费能力；Free 路径永远可运行 |
| O-5 | webhook 伪造或重放 | 签名错误、eventId 重放 | HMAC/EdDSA 签名、时间窗、幂等表、安全审计 |
| O-6 | OIDC 安全错误 | redirect_uri/state/PKCE 漏洞 | 使用 Spring Security OAuth2 Client；补 threat model |
| O-7 | DSR 漏删漏导 | 新表未进 data inventory | CI 校验新 migration 必须声明 retention / export / delete |
| O-8 | Helm 与 Compose 配置漂移 | release 时漏更新 chart | release gate 渲染 chart 并比对关键 env |
| O-9 | 商业化拖慢 OSS bug 修复 | 付费需求挤占 Free 稳定性 | roadmap 分轨；Free regression 仍是 release blocker |
| O-10 | 法务/财税判断滞后 | 定价、发票、退款上线前才确认 | 独立计费仓库 Sprint 0 先做合规清单，不在主仓硬编码政策 |
| O-11 | 双前端继续漂移 | 新功能同时进两个前端或只进旧前端 | v2.2 gate 阻断 `frontend-v2` 新功能；唯一产品前端写入 README/GOVERNANCE |
| O-12 | 删除 `frontend-v2` 造成契约丢失 | 旧公共分享/路由/视觉测试未迁移 | FE-03 先迁移 contract，FE-05 再决定归档或删除 |
| O-13 | 上游模板 attribution 丢失 | 重命名 `frontend-admin` 后删掉 Soybean attribution | NOTICE 保留上游说明，metadata 改品牌但不抹除来源 |

---

## 10. 决策复查与剩余边界

原 Sprint 0 决策点已经随主仓实现和仓库规范复查收敛为下表。这里不再要求重新拍板已落地事项，只记录当前状态、v2.2 处理和仍需外部证据的边界。

| 决策项 | 当前状态 | v2.2 处理 | 剩余边界 |
|---|---|---|---|
| Pro 的具体付费能力 | 已按 Free / Pro / Business edition model 和 `docs/commercial/edition-entitlement-surface.md` 收敛 | 主仓只固定 edition / entitlement / license / webhook / upgrade path，不把 v2.1.2 GA 能力 retroactively 收费 | 新增 Pro 能力必须先更新 surface doc、后端 gate 和前端 EntitlementGate |
| Business 首发企业能力 | 已选择 OIDC SSO + audit JSONL export + DSR / data inventory | OIDC、audit、DSR 均已有主仓代码、文档和 contract gate；自定义 RBAC 不进入 v2.2 P0 | live Keycloak e2e 与 OIDC trace 仍需外部证据 |
| 定价页是否写具体价格 | 已选择不承诺具体价格 | `docs/commercial/pricing-boundaries.md` 只保留 pilot / contact 边界，不写公开价格、不承诺 SLA | 财税、退款、发票和真实支付闭环留给独立计费仓 |
| GitHub private vulnerability reporting 启用时间 | 已完成 | `gh api --method GET repos/IMG-LTD/MMMail/private-vulnerability-reporting` 返回 `{"enabled":true}`；`SECURITY.md` 和 support routing 已把私密报告设为首选路径，公共 security-contact issue 只做无细节 fallback | 后续如关闭该设置，必须同步 SECURITY、完成审计和外部证据 verifier |
| 独立计费仓库是否立即立项 | 仍在主仓外 | 主仓可以发布 OSS launch + commercial-ready adapter，不声称 payment-ready | `mmmail-billing-gateway`、Adapay、客户门户、发票/退款和 license signing 需要私有仓真实证据 |
| FE-05 推荐方案 | 已确认 v2.2 保留 `frontend-v2` legacy reference，退出产品 gate | `frontend-admin` 是唯一产品前端；legacy freeze gate 阻断 `frontend-v2` 新增或修改文件，只允许删除或迁出历史材料 | v2.3 再执行实际删除或外部归档决策 |

---

## 11. 后续子 spec 拆分

| 子 spec | 文件建议 | 触发条件 |
|---|---|---|
| 开源治理 | `docs/v22-oss-governance-spec.md` | 本 spec 批准后先做 |
| 前端收敛 | `docs/v22-frontend-convergence-spec.md` | 与治理 spec 并行先做 |
| Edition + license | `docs/v22-edition-license-spec.md` | 前端收敛进入实现后 |
| BillingProvider contract | `docs/v22-billing-provider-contract-spec.md` | 独立计费仓库启动前 |
| 企业准入 | `docs/v22-business-access-spec.md` | Edition / license 骨架确定后 |
| Helm + image publish | `docs/v22-deployment-ecosystem-spec.md` | release pipeline 开始前 |
| OTEL + SLI + gate | `docs/v22-observability-release-gate-spec.md` | v2.2 release candidate 前 |
| GTM 边界 | `docs/v22-commercial-boundaries-spec.md` | pricing/support/trademark 开始前 |

---

## 12. 一句话目标

用约 11-13 周把 v2.1.2 shipping clean 升级为 v2.2：主仓达到可信开源发布和商业化适配就绪，先把双前端收敛为 `frontend-admin` 唯一产品入口，再交付 Free/Pro/Business、license、BillingProvider、Business 准入、Helm、OTEL 和 release gate；真实 Adapay 收款和 license 签发留在独立私有计费仓库。
