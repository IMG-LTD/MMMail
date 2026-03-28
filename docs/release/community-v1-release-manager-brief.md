# Community Edition v1.0 Release Manager Brief

**版本**: `v1.0-ga-window`
**日期**: `2026-03-28`
**作者**: `Codex`

## 当前状态
- 正式状态：`RC1_READY`
- 含义：Gate 0 ~ Gate 7 已全部通过，`v1.0.0-rc1` prerelease 已发布；当前进入 `v1.0.0` GA 稳定窗口
- 最新 green head：以 GitHub Actions 上 `dev/community-v1` 的最新成功 run 为准

## Gate 概览
- 已完成：Gate 0、Gate 1、Gate 2、Gate 3、Gate 4、Gate 5、Gate 6、Gate 7
- 待外部执行：无

## 入口文档
- GA 稳定窗口：`docs/release/community-v1-ga-stabilization.md`
- 反馈收集：`docs/release/community-v1-feedback-intake.md`
- `v1.1` 规划：`docs/release/community-v1-v1.1-plan.md`
- 外部执行：`docs/release/external-execution-checklist.md`
- 失败分诊：`docs/release/external-failure-triage.md`
- Gate 回填：`docs/release/gate-backfill-template.md`
- 回执登记：`docs/release/community-v1-external-receipt-log.md`
- 状态判定：`docs/release/community-v1-rc-status.md`
- 最终签收：`docs/release/community-v1-final-signoff.md`

## 当前允许
- release-blocking regression 分诊与最小修复
- 自托管反馈分流
- `v1.1` backlog 整理
- 已批准的 freeze exception

## 当前禁止
- 新增任何产品功能
- 新增非阻塞工程能力
- 扩展 Mail / Calendar / Drive / Admin / Billing / Docs / Sheets
- 调整首发范围、模块分级、GA / Beta / Preview
- 大范围重构
