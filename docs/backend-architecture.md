# 后端架构（v97）

**版本**: `v97.0`  
**日期**: `2026-03-10`  
**作者**: `Codex`

## 1. 本轮后端目标
让 org scope 不仅控制单产品 API，还能控制 suite 聚合接口返回内容与审计聚合面暴露内容。

## 2. 关键服务
### 2.1 `SuiteOrgScopeService`
- 负责解析当前请求的 active org scope
- 产出 `visibleProductCodes`
- 为 `suite / command-center / collaboration / notifications` 提供统一产品可见性上下文

### 2.2 `SuiteCommandCenterService`
- `getCommandCenter(...)`
  - quick routes / pinned searches / recent keywords / recommended actions 按 `visibleProductCodes` 过滤
- `getCommandFeed(...)`
  - 过滤被禁用产品事件
  - 过滤 org scope 下无产品归属的 aggregate suite query/list/change 事件
- 新增一层 `SCOPE_RESTRICTED_AGGREGATE_COMMAND_EVENT_TYPES`
  - 避免 `count=14`、`plan=FREE`、`SUITE_COLLABORATION_CENTER_QUERY count=...` 一类 detail 泄漏

### 2.3 `SuiteCollaborationService`
- collaboration center 与 sync payload 按 `visibleProductCodes` 过滤
- `productCounts` 基于过滤后的 items 重算

### 2.4 `SuiteInsightService`
- readiness / posture / unified search 在 org scope 下按可见产品重算
- remediation action 执行在 org scope 下继续受产品访问保护

## 3. 审计与 feed 策略
### 3.1 保留的事件
- 有明确 `productCode`
- 可由 `routePath` 稳定映射到具体产品
- 对当前 scope 有直接操作意义的 remediation / governance / workflow 事件

### 3.2 裁剪的事件
- 无产品归属、仅表达 suite aggregate 状态的 query/list/change 事件
- 这些事件在 personal scope 下可见，但在受限 org scope 下会泄漏隐藏产品数量、计划或聚合状态

## 4. 测试策略
- `backend/mmmail-server/src/test/java/com/mmmail/server/SuiteOrgAccessIntegrationTest.java`
  - 校验 `suite / command-center / collaboration / notifications / unified-search` 全链路 org scope 生效
  - 新增断言：
    - `commandFeed` 不含 `SUITE_PRODUCT_LIST`
    - `commandFeed` 不含 `SUITE_SUBSCRIPTION_QUERY`
    - `commandFeed` 不含 `SUITE_COMMAND_CENTER_QUERY`
    - `commandFeed` 不含 `SUITE_COLLABORATION_CENTER_QUERY`
    - `detail` 不再以 `count=` 或 `plan=` 这类 aggregate 泄漏开头
