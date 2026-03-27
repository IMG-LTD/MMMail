# 后端 ADR（v64）

**版本**: v64.0  
**日期**: 2026-03-06

## ADR-064-01 复用 `pass_vault_item` 承载 personal/shared item
- **决策**: 在 `pass_vault_item` 上新增 `org_id / shared_vault_id / scope_type / item_type`。
- **原因**: 共享 item 与个人 item 的字段骨架一致；复用单表可减少重复 CRUD 与 mapper 复杂度。
- **代价**: 个人域与共享域共享底层表，领域边界仍需通过 service 严格隔离。

## ADR-064-02 `shared vault` 独立成表
- **决策**: 新增 `pass_shared_vault` 与 `pass_shared_vault_member`。
- **原因**: vault 是协作容器，需要承载成员、item 计数、角色、审计。
- **代价**: 新增 vault access 校验链路。

## ADR-064-03 Pass team policies 复用 `org_policy`
- **决策**: 不新增 Pass policy 专表，而是新增 Pass 相关 policy keys。
- **原因**: 组织策略已有快照/更新基础设施，复用能减少冗余模型。
- **代价**: policy key 命名需要持续治理，避免跨产品污染。

## ADR-064-04 公共 secure link 审计允许匿名 actor
- **决策**: `audit_event.actor_id` 允许为空，匿名 secure link view 显式记录为无 actor 事件。
- **原因**: 外部 secure link 的访问不应伪装成某个内部用户。
- **代价**: 审计消费方必须接受 `actorId = null` 的事件。

## ADR-064-05 启动期 schema initializer 负责存量库升级
- **决策**: 新增 `PassBusinessSchemaInitializer` 在启动时显式补列、建表、建索引。
- **原因**: 旧库已存在时，单纯依赖 `schema.sql` 不能可靠完成演进。
- **代价**: 启动期多一次元数据探测，但换来可见、可验证的迁移行为。
