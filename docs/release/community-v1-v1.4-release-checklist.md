# Community Edition v1.4 Release Checklist

**版本**: `v1.4-release-checklist`
**日期**: `2026-04-08`
**作者**: `Codex`

## 发布基线
- 开发分支：`dev/v1.4`
- 合并目标：`main`
- 正式 tag：`v1.4.0`
- 发布策略：以 `main` 的 `v1.4.0` merge commit 作为正式发布切点，`dev/v1.4` 仅作为 release source branch
- GitHub Release：本次未通过自动化创建独立 Release 页面；以 annotated tag `v1.4.0` 作为正式发布引用

## 发布前必须满足
- [x] `docs/release/community-v1-v1.4-plan.md` 与实际代码一致
- [x] `docs/release/community-v1-v1.4-mainline-roadmap.md` 已按当前 tranche 复核
- [x] `docs/release/community-v1-v1.4.0-release-notes.md` 已完成
- [x] `docs/release/community-v1-support-boundaries.md` 已按 `v1.4` 真实边界复核
- [x] `docs/open-source/module-maturity-matrix.md` 已按 `v1.4` 真实边界复核
- [x] `README.md`、`docs/ops/install.md`、`docs/ops/runbook.md`、前端 boundary locale 已与 `v1.4` 对齐
- [x] `bash scripts/validate-local.sh` 在待发布 commit 上通过
- [x] `docs/release/community-v1-v1.4-final-signoff.md` 已签收

## 正式发布动作
1. [x] 在 `dev/v1.4` 上完成 `validate-local` 门禁。
2. [x] 提交 `v1.4` 最终代码与文档。
3. [x] 准备 `v1.4.0` annotated tag。
4. [x] 将 `dev/v1.4` 合并到 `main`。
5. [x] 在 `main` 上复核 `README / release notes / support boundaries / module maturity / install / runbook`。
6. [ ] 推送 `dev/v1.4`、`main` 与 `v1.4.0` 到远端（若远端可写）。
7. [x] 回填本文件中的正式发布策略与 release 引用信息。

## 仅允许的发布后修复
- `release-blocking` 功能回归
- 安装、升级、备份恢复阻断
- 安全基线回归
- release artifact 元数据错误
