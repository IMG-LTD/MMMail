# 数据库设计（Phase 8）

**版本**: v71.0  
**日期**: 2026-03-07

## 1. 新增表 `pass_alias_mailbox_route`
建议字段：
- `id`
- `alias_id`
- `owner_id`
- `mailbox_id`
- `mailbox_email`
- `created_at`
- `updated_at`
- `deleted`

## 2. 新增列 `mail_message.delivery_targets_json`
用途：
- 存储 sender 视角 mail 的全部投递目标
- 支撑 OUTBOX 延迟发信时恢复多 inbox 投递

建议结构：
```json
[
  {
    "ownerId": "2030...",
    "targetEmail": "team-abc123@passmail.mmmail.local",
    "forwardToEmail": "ops@mmmail.local"
  }
]
```

## 3. 约束
- `alias_id + mailbox_id + deleted` 唯一，防止 route 重复
- `owner_id + mailbox_email` 在 route 表中不需要唯一，但由 alias 维度保证唯一
- route 表中的 mailbox 必须命中 `pass_mailbox(id)` 且状态为 `VERIFIED`

## 4. 既有表关系
- `pass_mail_alias` 1 : N `pass_alias_mailbox_route`
- `pass_mailbox` 1 : N `pass_alias_mailbox_route`
- `mail_message` 继续是 inbox/sent 主表，不新增 route 子表

## 5. 兼容迁移
- 历史 alias 若仅有 `forward_to_email`，运行时回填为单 route
- `forward_to_email` 继续保留，用于兼容老接口和展示主路由
- schema 与 initializer 都要补齐新表、索引、列
