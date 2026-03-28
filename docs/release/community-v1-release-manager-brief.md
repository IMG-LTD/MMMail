# Community Edition v1.0 Release Manager Brief

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前状态
- 正式状态：`RC1_READY`
- 含义：Gate 0 ~ Gate 7 已全部通过，官方 CI 与容器化 RC1 证据链已完成回填；当前可进入发布候选确认

## Gate 概览
- 已完成：Gate 0、Gate 1、Gate 2、Gate 3、Gate 4、Gate 5、Gate 6、Gate 7
- 待外部执行：无

## 入口文档
- 外部执行：`docs/release/external-execution-checklist.md`
- 失败分诊：`docs/release/external-failure-triage.md`
- Gate 回填：`docs/release/gate-backfill-template.md`
- 回执登记：`docs/release/community-v1-external-receipt-log.md`
- 状态判定：`docs/release/community-v1-rc-status.md`
- 最终签收：`docs/release/community-v1-final-signoff.md`

## 当前允许
- RC1 最终签收
- 发布候选确认
- release-blocking regression 分诊
- 已批准的 freeze exception

## 当前禁止
- 新增任何产品功能
- 新增非阻塞工程能力
- 扩展 Mail / Calendar / Drive / Admin / Billing / Docs / Sheets
- 调整首发范围、模块分级、GA / Beta / Preview
- 大范围重构
