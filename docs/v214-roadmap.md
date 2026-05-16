---
name: v2.1.4 roadmap
date: 2026-05-16
status: draft
source:
  - docs/v213-closure-spec.md §12
  - docs/v213-closure-spec-v1.1.md §2.6
---

# v2.1.4 Roadmap

本文件只承接 v2.1.3 收尾 spec 明确排除的范围，不作为 v2.1.3 release-gate 的实现项。

## 候选条目

| 编号 | 条目 | 当前状态 | 下一步定义 |
|---|---|---|---|
| R-1 | 18.4.3 Sheets/看板 CRDT 接入 | 未实施 | 定义 Sheets/看板协同数据模型、WebSocket 通道与冲突收敛验收 |
| R-2 | 富文本 Tiptap | 未实施 | 明确编辑器范围、文档 schema、迁移策略与安全过滤边界 |
| R-3 | 全仓 NEmpty/NSpin 替换 | 未实施 | 盘点剩余数据态组件，按页面域拆分替换批次 |

## 边界

- 不回补到 v2.1.3。
- 不改变 v2.1.2 GA release-gate。
- 进入 v2.1.4 前需要单独 spec、测试矩阵和验收命令。
