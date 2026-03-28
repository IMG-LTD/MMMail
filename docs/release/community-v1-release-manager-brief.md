# Community Edition v1.0 Release Manager Brief

**版本**: `v1.0-ga-window`
**日期**: `2026-03-28`
**作者**: `Codex`

## 当前状态
- 正式状态：`RC1_READY`
- 含义：Gate 0 ~ Gate 7 已全部通过，`v1.0.0-rc1` prerelease 已发布；`release/v1.0` 已从 `6f86864` 切出，承接 `v1.0.0` 与后续 `v1.0.x`
- `v1.0` 最新 green head：以 GitHub Actions 上 `release/v1.0` 的最新成功 run 为准
- `v1.1` 集成分支：`dev/community-v1`

## Gate 概览
- 已完成：Gate 0、Gate 1、Gate 2、Gate 3、Gate 4、Gate 5、Gate 6、Gate 7
- 待外部执行：无

## GitHub milestones
- `v1.0.0`
- `v1.1`
- `post-v1.1`

## 分支职责
- `release/v1.0`：`v1.0.0` GA 候选、正式发布与 `v1.0.x` 维护线
- `dev/community-v1`：`v1.1` 集成分支
- `6b01ae6 feat: stabilize docs route selection` 保留在 `dev/community-v1`，不进入 `v1.0.0` GA 基线

## 入口文档
- GA 稳定窗口：`docs/release/community-v1-ga-stabilization.md`
- GA 正式发布清单：`docs/release/community-v1-ga-release-checklist.md`
- 反馈收集：`docs/release/community-v1-feedback-intake.md`
- GA 分诊手册：`docs/release/community-v1-ga-triage-playbook.md`
- `v1.1` 规划：`docs/release/community-v1-v1.1-plan.md`
- `v1.1` backlog seed：`docs/release/community-v1-v1.1-backlog-seed.md`
- 自托管反馈模板：`.github/ISSUE_TEMPLATE/self-hosting-feedback.md`
- release-blocking 模板：`.github/ISSUE_TEMPLATE/release-blocking-regression.md`
- 外部执行：`docs/release/external-execution-checklist.md`
- 失败分诊：`docs/release/external-failure-triage.md`
- Gate 回填：`docs/release/gate-backfill-template.md`
- 回执登记：`docs/release/community-v1-external-receipt-log.md`
- 状态判定：`docs/release/community-v1-rc-status.md`
- 最终签收：`docs/release/community-v1-final-signoff.md`

## 当前允许
- `release/v1.0` 上的 release-blocking regression 分诊与最小修复
- 自托管反馈分流
- `v1.1` backlog 整理与已批准范围开发
- 已批准的 freeze exception

## 当前禁止
- 新增任何产品功能
- 新增非阻塞工程能力
- 向 `release/v1.0` 回灌 `Docs / Sheets` Beta 功能改动
- 调整首发范围、模块分级、GA / Beta / Preview
- 大范围重构
