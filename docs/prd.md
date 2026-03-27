# PRD（v97）

**版本**: `v97.0`  
**日期**: `2026-03-10`  
**作者**: `Codex`

## 1. 目标
将 Organizations `product access` 的运行时治理继续扩展到 Proton 风格的 suite 聚合面：
- `suite` 不再展示被禁用产品的 usage / quota / command search 文案
- `command-center` 不再通过 feed 泄漏无产品归属的 aggregate detail
- `collaboration` / `notifications` 按 active org scope 动态净化
- 浏览器主链路在 org scope 与 personal scope 切换时保持一致

## 2. 功能清单
### 2.1 Suite 文案与 quota 净化
- `Usage Overview` 仅显示当前 scope 可见产品对应的 quota 指标
- `Plan Catalog` 仅显示当前 scope 可见产品对应的计划指标
- `Suite Command Search` 说明文案根据当前可见产品动态生成
- `Mail / Docs` 在被禁用时不再以说明文字或 quota 标签形式出现

### 2.2 Command feed 聚合事件裁剪
- `command-center` feed 在 org scope 下移除无产品归属且会泄漏 suite aggregate 状态的事件
- 至少覆盖：
  - `SUITE_PRODUCT_LIST`
  - `SUITE_SUBSCRIPTION_QUERY`
  - `SUITE_PLAN_LIST`
  - `SUITE_PLAN_CHANGE`
  - `SUITE_COMMAND_CENTER_QUERY`
  - `SUITE_COLLABORATION_CENTER_QUERY`
  - `SUITE_READINESS_QUERY`
  - `SUITE_SECURITY_POSTURE_QUERY`
  - `SUITE_GOVERNANCE_*_LIST/OVERVIEW/TEMPLATE`
  - `SUITE_NOTIFICATION_*_QUERY`

### 2.3 Collaboration 隐式入口修复
- filter chips 基于当前 org access 动态渲染
- hero / empty-state / latest-event 文案不再写死 `Docs`
- `Current Filter` 显示 label，而非裸 product code

### 2.4 UAT 与证据
- 组织作用域下：
  - `/inbox` 仍被阻断
  - `/suite` 不再出现 `Mail / Docs` 文本级泄漏
  - `/command-center` feed 不再出现 `count=14` / `plan=FREE` / `SUITE_COLLABORATION_CENTER_QUERY count=...`
  - `/collaboration` 不再出现 `Docs 0`
  - `/notifications` 仅显示当前 scope 可见产品通知
- 切回 personal scope 后：
  - `/inbox` 恢复
  - Mail 相关入口恢复

## 3. 验收标准
1. 前端单测覆盖 command feed aggregate 过滤与 suite scope helper
2. `pnpm typecheck` 与 `pnpm build` 全部通过
3. 后端 `SuiteOrgAccessIntegrationTest` 通过
4. Playwright UAT 证明：
   - org scope 下 `suite` 与 `command-center` 没有隐藏产品泄漏
   - `Switch to personal scope` 仍能恢复 Mail 主路径
5. Jira `KAN-261` 完成评论回写
6. 文档中形成 v97 的差距矩阵，并将国际化缺口显式列出
