# 需求分析（v97）

**版本**: `v97.0`  
**日期**: `2026-03-10`  
**作者**: `Codex`  
**主题**: Proton `org access` 在 `suite / command-center / collaboration / notifications` 聚合面继续收口  
**变更记录**:
- `v97.0`：聚焦 hidden entry、聚合入口、command feed/detail 泄漏与 org scope 主链路补强。

## 1. 迭代主题
- 版本：`v97`
- Jira：`KAN-261`
- 方向：在 `v96` 已完成 org scope、导航过滤、直达阻断、后端 API 拦截基础上，继续对齐 Proton `Access control` 的聚合面行为。

## 2. 背景
`v96` 已经让组织产品访问真正影响：
- shared shell 导航
- 直达路由
- 后端产品 API

但 `suite / command-center / collaboration / notifications` 这些 Proton Business 风格的聚合面仍存在两类问题：
- **文本/入口泄漏**：组织作用域禁用 `Mail / Docs` 后，页面说明、usage、空状态、筛选器仍可能出现被禁用产品。
- **聚合明细泄漏**：`command-center` feed 中仍可能显示 `count=14`、`plan=FREE`、`SUITE_COLLABORATION_CENTER_QUERY count=...` 这类无产品归属的 suite aggregate detail。

这与 Proton 官方帮助文档中“禁用后从 app switcher 消失，并引导用户去已批准服务”的基线不一致。

## 3. 用户故事
- 作为 org member，我希望在组织作用域下看不到被禁用产品的显式入口、隐式入口和聚合统计文案。
- 作为 org admin，我希望 suite 聚合后台不会通过 feed、说明文字、空状态重新泄漏已禁用产品的存在或数量。
- 作为产品团队，我希望前端 UI、前端操作方式、后端聚合接口在 org scope 下保持同一套产品可见性规则。
- 作为后续国际化负责人，我希望明确当前仍未完成 `en / zh-CN / zh-TW` 的页面与能力，以便下轮系统接入。

## 4. 官方基线（2026-03-10）
- Proton 首页：`https://proton.me/`
- Proton for Business：`https://proton.me/business`
- Control product access：`https://proton.me/support/control-product-access-organization`
- Organization monitor：`https://proton.me/support/organization-monitor`
- External accounts / Drive+Docs+Sheets 关系：`https://proton.me/support/external-accounts`
- Sheets：`https://proton.me/drive/sheets`
- SimpleLogin：`https://simplelogin.io/`

## 5. 本轮范围
### In scope
- `suite` 页 usage / quota / command search 文案的 org scope 净化
- `command-center` quick routes / pinned search / feed 的 hidden-entry 治理
- `collaboration` 页 filter chips / hero / empty-state 的动态产品集合
- `notifications` / `command-center` / `suite` 聚合 API 的组织作用域过滤
- Playwright 主链路 UAT
- Jira 闭环
- 当前项目与 Proton 在 UI、用户操作方式、后端能力、国际化四个维度的差距总结

### Out of scope
- 新增 Billing / Seats / Compliance / Procurement 真正业务流
- 为 `VPN / Meet / Wallet / Authenticator / Lumo` 补真实服务内核
- 将全站所有深层页面一次性补齐 `en / zh-CN / zh-TW`
- 重做 `business / security / suite / command-center` 的完整视觉系统

## 6. 非功能性要求
- 不允许 silent fallback、mock success、吞错
- 聚合页面的可见产品集合必须由 active org scope 决定
- 浏览器验证必须使用 Playwright MCP `browser_*`
- 后端定向测试必须使用 60 秒超时
- 差距总结必须区分：
  - 已对齐能力
  - 仍存在的 UI 差距
  - 仍存在的操作流差距
  - 仍存在的后端能力差距
  - 国际化缺口
