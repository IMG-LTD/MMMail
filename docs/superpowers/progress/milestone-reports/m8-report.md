# M8 Report

## Purpose
- Record final `G-release` completion evidence for `v2.0.0`.
- Preserve the strong local-complete evidence from this worktree and replace the remaining pending release actions with completed release facts.

## Final released-state evidence

### §6.1 Code and architecture done — released evidence
- `M0` completed the frozen release scaffold and the baseline matrices:
  - `docs/superpowers/progress/domain-boundary-matrix.md`
  - `docs/superpowers/progress/tenant-scope-model.md`
  - `docs/superpowers/progress/gateway-compatibility-matrix.md`
  - `docs/superpowers/progress/feature-flag-matrix.md`
  - `docs/superpowers/progress/labs-disposition-matrix.md`
  - `docs/superpowers/progress/event-catalog.md`
- `M1` evidence is archived in `docs/superpowers/progress/milestone-reports/m1-report.md` and confirms the v2 module reactor, canonical routes, redirect registry, and `X-MMMAIL-SCOPE-ID` contract.
- `M2` evidence is archived in `docs/superpowers/progress/milestone-reports/m2-report.md` and confirms platform capability baseline plus shared `useScopeGuard / useDialogStack / useSoftAuthLock / useAsyncActionState` contracts.
- `M3` evidence is archived in `docs/superpowers/progress/milestone-reports/m3-report.md` and confirms unified public-share capability and shared unlock flow across Mail / Drive / Pass.
- `M4` evidence is archived in `docs/superpowers/progress/milestone-reports/m4-report.md` and confirms `/api/v2/ai-platform/capabilities`, `/api/v2/mcp/registry`, and frontend shared AI / MCP contract consumption.
- `M5` evidence is archived in `docs/superpowers/progress/milestone-reports/m5-report.md` and confirms workspace aggregation plus Docs / Sheets / collaboration surfaces on shared contracts.
- `M6` evidence is archived in `docs/superpowers/progress/milestone-reports/m6-report.md` and `docs/superpowers/progress/legacy-exit-report.md`; billing readiness is present and approved legacy exits are frozen.
- `M7` evidence is archived in `docs/superpowers/progress/milestone-reports/m7-report.md`, `docs/superpowers/progress/labs-decision-log.md`, and `docs/superpowers/progress/release-blocker-checklist.md`; Labs disposition is recorded and the blocker checklist is fully checked.
- PR #5 merged `release/2.0.0` into `main`, making the frozen v2 release branch the shipped mainline result for `v2.0.0`.

### §6.2 Quality gate done — released evidence
- Final release-branch validation passed on `release/2.0.0`: `bash scripts/validate-local.sh` completed with `[validate-local] all checks passed`.
- The local release validation still includes:
  - `pnpm --dir frontend-v2 test`
  - `pnpm --dir frontend-v2 typecheck`
  - `bash scripts/validate-local.sh`
- `scripts/validate-local.sh` includes the `frontend-v2` test and typecheck steps.
- `.github/workflows/ci.yml` includes the `frontend-v2` suite.
- Main CI run `24714868040` (`MMMail CI`) executed on `main` for merge commit `9ef87db869490688684c543cccd6bb1750e62685` and completed with `success`: `https://github.com/IMG-LTD/MMMail/actions/runs/24714868040`.
- The M7 release blocker checklist remains fully checked and records:
  - `frontend-v2` local validation wiring
  - CI validation wiring
  - backend targeted release regressions passing
  - v2 route / share / auth / AI-MCP contract tests passing
  - committed legacy redirect and Labs decision evidence

### §6.3 Rollout and rollback readiness — released evidence
- The rollout/rollback evidence set remains available in:
  - `docs/superpowers/progress/feature-flag-matrix.md`
  - `docs/superpowers/progress/gateway-compatibility-matrix.md`
  - `docs/superpowers/progress/state.md`
- Flag ownership and frontend/backend rollout coupling are documented.
- Gateway compatibility and legacy redirect behavior are documented.
- `v2.0.0` is now merged, tagged, and released, while the report still does not claim a real production canary cutover beyond the documented mechanism/evidence set.

### §6.4 Documentation and release artifacts — released evidence
- `README.md` was modified in the released merge result for `v2.0.0`.
- `CHANGELOG.md` was added in the released merge result for `v2.0.0`.
- `docs/release/v2.0.0-release-notes.md` was added for the release and used to publish the GitHub Release: `https://github.com/IMG-LTD/MMMail/releases/tag/v2.0.0`.
- `docs/ops/runbook.md`, `docs/ops/install.md`, `docs/ops/upgrade.md`, `docs/ops/backup-restore.md`, and `docs/ops/team-enablement.md` remain the referenced operational handoff set for `v2.0.0`.
- `docs/superpowers/progress/milestone-reports/m6-report.md`, `docs/superpowers/progress/milestone-reports/m7-report.md`, and this `m8-report.md` record the milestone completion and release closeout evidence set.

### §6.5 Release execution evidence
- Final release-source validation passed on `release/2.0.0`: `bash scripts/validate-local.sh` completed successfully before release closeout.
- PR #5 `release: merge v2.0.0 into main` merged from `release/2.0.0` to `main` at `2026-04-21T09:29:31Z`: `https://github.com/IMG-LTD/MMMail/pull/5`.
- The merge commit for the release is `9ef87db869490688684c543cccd6bb1750e62685`.
- `origin/main` resolves to `9ef87db869490688684c543cccd6bb1750e62685`, so the merged mainline result matches the release merge commit.
- The annotated tag `v2.0.0` exists locally and remotely, and the peeled tag target is `9ef87db869490688684c543cccd6bb1750e62685`.
- GitHub Release `MMMail v2.0.0` was published at `2026-04-21T09:47:11Z`: `https://github.com/IMG-LTD/MMMail/releases/tag/v2.0.0`.
- Main CI run `24714868040` remained green after the merge/tag/release sequence.

### §6.6 2.x roadmap / out of scope
The exact non-`2.0.0` items from the frozen design remain out of scope for this release:
- Labs 模块真正产品化实现（Meet / VPN / Lumo / Wallet / Standard Notes parity 仅保留 preview 壳层 + route plumbing）
- `database-per-tenant` 升级（backend §11.1 第 2 项）
- Kafka / 独立 MQ 替换 Redis Streams（backend §11.1 第 5 项）
- Billing ledger 精细模型、外部支付编排（backend §11.1 第 3 项）
- Docs/Sheets 实时协作引擎重型升级（backend §11.1 第 4 项）
- 真实生产环境金丝雀切流（用户确认的 Q4-A 决策）

## Release conclusion
- `v2.0.0` is released.
- M8 closeout is complete; any further work moves to normal post-release follow-up from `main`.
