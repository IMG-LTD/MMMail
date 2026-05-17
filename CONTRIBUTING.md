# Contributing to MMMail

## Scope
- 当前公开协作基线以 `v2.1.2-shipping-clean` 为准，权威范围见 `README.md`、`docs/release/v2-support-boundaries.md` 与 `docs/open-source/module-maturity-matrix.md`。
- `GA` 模块的回归、文档与运维能力属于 release-blocking 范围；`Beta / Preview` 改动不得破坏 `GA` 门禁，也不得夸大未交付能力。
- 当前主线优先级仍是 `Mail → Calendar → Drive → Pass` 协作闭环与自托管可采用性。
- `frontend-admin` 是唯一产品前端；`frontend-v2` 是冻结 legacy reference，只允许删除文件或迁出历史材料，不允许新增或修改文件。

## Before You Open a PR
1. 阅读 `README.md`、`SUPPORT.md`、`SECURITY.md`、`docs/release/v2-support-boundaries.md`。
2. 阅读 `AGENTS.md`、`CODE_OF_CONDUCT.md`、`GOVERNANCE.md`、`MAINTAINERS.md`。
3. 使用 `.env.example`（最小模式）或 `config/backend.env.example`（标准模式）创建本地 `.env`，并执行 `./scripts/validate-runtime-env.sh .env`。
4. 运行默认门禁：
   - `bash scripts/validate-local.sh`
5. 如修改数据库、备份或迁移逻辑，补充：
   - `bash scripts/validate-batch3.sh`
6. 如修改安全、权限、上传、分享或管理员能力，补充：
   - `bash scripts/validate-security.sh`
7. 如改动依赖、lockfile、POM、CI 运行时或安全扫描脚本，先复查 open Dependabot 告警：
   - `gh api repos/IMG-LTD/MMMail/dependabot/alerts?state=open&per_page=100`
   - GHCR / GitHub Package 证据需要当前 `gh` token 具备 `read:packages` scope；权限不足时记录权限缺口，不得把 package 不可见当作不存在。
8. 如修改商业化、支持承诺、可观测目标或企业准入文案，先对齐 `docs/commercial/pricing-boundaries.md`、`docs/commercial/support-policy.md` 与 `docs/observability/sli-slo.md`。
9. 如把 v2.2 外部证据项从 partial / pending 改为 done，必须先提供 `docs/v22-external-evidence-checklist.md` 要求的真实证据，并记录 `bash scripts/validate-v22-external-evidence.sh` 的结果。

## Branch / Commit Rules
- 使用 Conventional Commits 前缀：`feat:` `fix:` `docs:` `test:` `refactor:` `chore:`。
- 所有 PR commit 必须包含 `Signed-off-by: Name <email>` trailer；细则见 `DCO.md`，DCO workflow 会硬阻断缺失签名的 PR。
- 一个 PR 只解决一个清晰问题，避免把新功能和收口修复混在一起。
- 若改动影响公开边界、安装方式或自托管运维路径，必须同步更新 `README.md`、`docs/ops/*`、`docs/release/v2-support-boundaries.md` 或 `docs/open-source/module-maturity-matrix.md`。
- 若改动影响 Business / Hosted / payment / SLA / license 口径，必须同步商业边界文档；real payment processing is not live，license signing private keys stay outside the public repository。
- 若改动影响 Helm / image / env / deployment 口径，必须同步 `docs/ops/*`、Helm chart、image workflow 或 release notes 模板。
- 若改动响应 Dependabot 告警，必须说明告警包名、修复版本和验证命令；供应链安全修复不能只关闭扫描或降低 CVSS 门槛。
- 不允许只凭本地测试、dry run、mock billing 或截图把 live Keycloak、OIDC trace、image digest 或独立计费仓事项标为 done。
- 若调整 GitHub private vulnerability reporting 状态，必须用 GitHub API 结果同步 `SECURITY.md`、`SUPPORT.md`、完成审计和外部证据 verifier。

## Code Expectations
- 遵守仓库工程约束：保持实现聚焦，不为假设需求增加抽象或兼容层。
- 不要提交真实 secrets、密钥、生产凭据或私有基础设施地址。
- 安全或权限逻辑改动必须附带自动化回归。
- 依赖安全修复必须把 fixed version 固定到 `frontend-admin/pnpm-lock.yaml`、Maven POM、`DependencyVersionGuardTest` 或根目录供应链 contract，不能只依赖一次性本地扫描结果。
- 前端改动需要类型检查；后端改动需要定向 Maven 测试。
- Spring 管理组件如保留多个构造器，必须显式标注运行时注入构造器，避免本地 contract 通过但 e2e 启动失败。
- 新增活跃源码文件默认不超过 500 行；触及历史超限文件时，把新增职责拆到小模块，不扩大未列入治理 allowlist 的超限文件。
- 公开商业文案不得承诺未落地价格、真实扣款、合同化服务等级、托管可用性或 license 签发能力。

## PR Checklist
- [ ] 改动范围仍在当前 `v2` 支持边界内
- [ ] 每个 commit 都包含 `Signed-off-by`，并符合 `DCO.md`
- [ ] 对应测试已更新并通过
- [ ] 文档已同步更新，或确认无需更新
- [ ] 无真实 secrets / token / password 写入仓库
- [ ] 若影响采用或运维流程，已更新 `README.md`、`docs/ops/*` 或边界文档
- [ ] 若影响商业、可观测或支持承诺，已更新 `docs/commercial/*` 或 `docs/observability/sli-slo.md`
- [ ] 若影响 Helm / image / env / deployment，已同步 chart、workflow、release notes 或 install docs
- [ ] 若响应 Dependabot 告警，已记录告警包名、fixed version，并运行供应链安全相关 contract / typecheck / backend guard
- [ ] 若把 v2.2 外部证据项标为 done，已附真实外部证据并运行 `bash scripts/validate-v22-external-evidence.sh`
- [ ] 未新增或修改 `frontend-v2` 文件；如触及该目录，只包含删除或迁出历史材料
- [ ] 未新增超过 500 行的活跃源码文件；若触及历史超限文件，新增职责已拆到小模块

## Support Routing
- 普通缺陷、release blocker、自托管反馈和功能建议先按 `SUPPORT.md` 选择 issue 模板。
- live security vulnerability 不进入公开 issue；按 `SECURITY.md` 请求私密交接。

## Security Reports
- 不要通过公开 issue 披露活跃安全漏洞。
- 按 `SECURITY.md` 中的流程私下报告。
