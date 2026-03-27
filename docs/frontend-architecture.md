# 前端架构（v97）

**版本**: `v97.0`  
**日期**: `2026-03-10`  
**作者**: `Codex`

## 1. 本轮前端改动范围
- `frontend/pages/suite.vue`
- `frontend/pages/collaboration.vue`
- `frontend/pages/command-center.vue`
- `frontend/pages/notifications.vue`
- `frontend/utils/org-product-surface-filter.ts`
- `frontend/utils/collaboration.ts`
- `frontend/utils/suite-scope.ts`
- `frontend/composables/useCollaborationSyncStream.ts`
- `frontend/composables/useNotificationSyncStream.ts`

## 2. 设计原则
- org scope 是产品可见性的单一来源
- 聚合页不能绕过 shared shell 的产品过滤规则
- 对无产品归属的 aggregate feed item，前端也保留一层显式裁剪，避免旧数据回流到 UI

## 3. 数据流
### 3.1 可见产品集合
- 来源：`useOrgAccessStore().isProductEnabled`
- 作用范围：
  - shared shell 导航
  - `suite` product matrix / usage / quota / command search
  - `command-center` quick routes / pinned searches / recent keywords / feed
  - `collaboration` product chips 与空状态
  - `notifications` 聚合中心

### 3.2 聚合过滤层
`frontend/utils/org-product-surface-filter.ts` 负责：
- 基于 `productCode` 过滤
- 基于 `routePath` 推导产品过滤
- 在 scope 受限时移除无产品归属的 aggregate command event

### 3.3 Suite scope helper
`frontend/utils/suite-scope.ts` 负责：
- 从 `visibleProducts` 推导 usage rows
- 从 `visibleProducts` 推导 plan quota rows
- 生成 command search summary
- 控制 drive entity 指标是否显示

## 4. 页面级策略
### 4.1 `suite.vue`
- `Usage Overview` 从固定写死行改为 scope-aware rows
- `Plan Catalog` 从固定 Mail/Calendar/Drive 列表改为 scope-aware quotas
- `Suite Command Search` 说明文字改为基于当前可见产品动态拼接

### 4.2 `command-center.vue`
- `commandFeed` 改为先经过 `filterSuiteCommandFeedByAccess`
- 当当前 org scope 隐藏产品时，不再显示无产品归属的 aggregate suite feed

### 4.3 `collaboration.vue`
- 产品 chips 改为根据 org access 动态出现
- 空状态与上下文说明不再硬编码 `Docs`

## 5. 测试策略
- `frontend/tests/org-command-surfaces.spec.ts`
  - 覆盖 route/product filtering
  - 覆盖 aggregate suite feed 裁剪
- `frontend/tests/suite-scope.spec.ts`
  - 覆盖 usage / quota / command search summary
- `frontend/tests/collaboration.spec.ts`
  - 覆盖 collaboration 动态产品集合
- `frontend/tests/org-suite-surfaces.spec.ts`
  - 覆盖 suite / readiness / notification / collaboration 聚合过滤
