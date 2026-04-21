# M8 Report

## Purpose
- Support `G-release` signoff for the local-preparable part of `v2.0.0`.
- Record what is already complete in this worktree and what remains under remote release execution.
- This report does not claim that any remote merge, tag push, or GitHub Release action has already happened.

## Local-complete evidence already achieved in this worktree

### §6.1 Code and architecture done — local evidence
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

### §6.2 Quality gate done — local evidence
- Local validations pass in this worktree:
  - `pnpm --dir frontend-v2 test`
  - `pnpm --dir frontend-v2 typecheck`
  - `bash scripts/validate-local.sh`
- `scripts/validate-local.sh` includes the `frontend-v2` test and typecheck steps.
- `.github/workflows/ci.yml` includes the `frontend-v2` suite.
- The M7 release blocker checklist is fully checked and records:
  - `frontend-v2` local validation wiring
  - CI validation wiring
  - backend targeted release regressions passing
  - v2 route / share / auth / AI-MCP contract tests passing
  - committed legacy redirect and Labs decision evidence

### §6.3 Rollout and rollback readiness — local evidence
- The local rollout/rollback evidence set exists for signoff review:
  - `docs/superpowers/progress/feature-flag-matrix.md`
  - `docs/superpowers/progress/gateway-compatibility-matrix.md`
  - `docs/superpowers/progress/state.md`
- Flag ownership and frontend/backend rollout coupling are documented.
- Gateway compatibility and legacy redirect behavior are documented.
- This session does not claim a real production canary cutover; only the documented local mechanism/evidence set is prepared.

### §6.4 Documentation and release artifacts — local evidence
- `docs/release/v2.0.0-release-notes.md` is prepared for `v2.0.0` and includes landed scope, ops handoff pointers, `2.x Roadmap`, and `Known Limitations`.
- `docs/superpowers/progress/milestone-reports/m6-report.md` exists and records M6 completion evidence.
- `docs/superpowers/progress/milestone-reports/m7-report.md` exists and records M7 completion evidence.
- `docs/superpowers/progress/milestone-reports/m8-report.md` now records the local release-evidence handoff for G-release.
- Per this task's instruction, `README`, `CHANGELOG`, and deployment runbook files were not modified in this session and are therefore not claimed as newly updated by this report.

### §6.6 2.x roadmap / out of scope
The exact non-`2.0.0` items from the frozen design remain out of scope for this release:
- Labs 模块真正产品化实现（Meet / VPN / Lumo / Wallet / Standard Notes parity 仅保留 preview 壳层 + route plumbing）
- `database-per-tenant` 升级（backend §11.1 第 2 项）
- Kafka / 独立 MQ 替换 Redis Streams（backend §11.1 第 5 项）
- Billing ledger 精细模型、外部支付编排（backend §11.1 第 3 项）
- Docs/Sheets 实时协作引擎重型升级（backend §11.1 第 4 项）
- 真实生产环境金丝雀切流（用户确认的 Q4-A 决策）

## pending remote release actions
The following §6.5 items are required for final release completion but were not executed in this session:
- Confirm `release/2.0.0` contains all intended PRs, is green in remote CI, and has no unresolved conflicts.
- Run the final release-branch regression pass on `release/2.0.0` as the release source of truth.
- Create and complete the one-shot `release/2.0.0 → main` merge PR.
- Create and push the annotated tag: `git tag -a v2.0.0 -m "MMMail v2.0.0"` and `git push origin v2.0.0`.
- Publish the GitHub release with the prepared notes: `gh release create v2.0.0 --notes-file docs/release/v2.0.0-release-notes.md`.
- Verify `main` remains green after the remote merge/tag/release sequence.

## Release handoff note
- Local release documentation/evidence is prepared for `v2.0.0`.
- Final G-release completion still depends on explicit user signoff plus the pending remote release actions above.
