# UI 设计（v96）

**版本**: `v96.0`  
**日期**: `2026-03-10`  
**作者**: `Codex`

## 1. 美学方向
- Archetype：`Swiss / International`
- Differentiator：`scope-aware governance shell`

本轮不追求新页面数量，而是让 Proton 风格的治理能力真正渗透到共享壳层：
- 顶部加入轻量、持续可见的 `Scope` 选择器
- 左侧导航根据当前组织作用域动态收缩
- 被阻断产品使用单页明确说明策略来源，而不是粗暴 404

## 2. 信息架构
- 顶栏：
  - 品牌
  - 全局搜索
  - `Scope` 选择器
  - 语言切换
  - 当前用户
- 左栏：
  - 只展示当前作用域允许的产品和工具入口
- 主区：
  - Organizations 工作台继续承担产品矩阵治理
  - 新增 `product-access-blocked` 作为统一阻断页

## 3. 核心组件
### 3.1 `OrgScopeSwitcher`
- 显示当前作用域名称与角色
- 支持在个人作用域与组织作用域之间切换
- 顶栏与 Organizations 头部共用同一视觉语言

### 3.2 `OrganizationsProductAccessMatrix`
- 保持成员卡片 + 产品开关矩阵
- 增加运行时效果提示：
  - `Disabled products disappear from the org scope sidebar and direct access is blocked.`

### 3.3 `product-access-blocked`
- 结构：
  - 组织策略标签
  - 被禁用产品标题
  - 原因说明
  - 当前 active scope
  - 行动按钮：切回个人作用域 / 打开 Organizations

## 4. 交互规则
- 切换组织作用域后：
  - 顶栏文案立即变化
  - sidebar 入口立即按产品权限过滤
  - API 请求开始自动携带对应 `X-MMMAIL-ORG-ID`
- 访问被禁用产品时：
  - 不闪烁、不先进入内容再报错
  - 直接路由跳转阻断页
- owner 在产品矩阵切换开关后：
  - member 下一次进入该组织作用域时立刻看到入口变化

## 5. 三语规则
- `Scope / Active scope / Switch to personal scope / Open organizations`
- `Docs is disabled in ...`
- 运行时效果提示与组织访问状态文案

以上文案均要求覆盖 `en / zh-CN / zh-TW`，不允许只在英文状态下新增。
