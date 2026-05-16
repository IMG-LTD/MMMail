# MMMail v2.1.2 前端迁移规范（Spec）

基于 Soybean Admin 框架重构 MMMail 前端，将现有 `frontend-v2/` 全部业务功能迁移至 Soybean Admin 架构体系。

## 0. 规范状态

- 版本：v2.1.2（spec v1.3）
- 日期：2026-05-16（v1.0 初稿 / v1.1 增补差距章节 §14–§28 / v1.2 增补 §6.4 框架复用强制约束 / v1.3 §21 迁移命名规约修订）
- 适用范围：新前端项目（基于 `frontend-admin/`）
- 源项目：`frontend-v2/`（Vue 3 + Naive UI + Pinia + Vue Router）
- 目标框架：Soybean Admin（Vue 3.5 + Naive UI 2.44 + Pinia 3 + UnoCSS + Elegant Router）
- 后端：不变，仍为 `backend/` Spring Boot 服务
- v1.3 变更摘要：
  - §21.1 迁移命名规约从 `V2_1_2__NN` 改为 `V{N}` 单调递增整数（避免 Flyway 版本比较的"out of order"歧义）
  - §21.2 本期迁移列表更新为实际落库的 12 个文件（V21–V32），与代码现状对齐
  - 新增 `scripts/check-migration-naming.sh` CI 校验入口
  - 配套：本期收尾按 `docs/v213-closure-spec.md` 执行，闭合 §27 验收清单剩余 10% 缺口
- v1.2 变更摘要：
  - 强化 §1.1 核心原则，新增"禁止重新自研基础设施"条款
  - 新增 §6.4 Soybean Admin 框架强制复用约束（必读）：
    - §6.4.1 必须复用清单（布局 / 通用组件 / store / hooks / UI 库与样式系统）
    - §6.4.2 严格禁止做法（自研 SCSS、自研 CSS 变量、引入第二个 UI 库等）
    - §6.4.3 新页面标准做法（路由 / 表格 / 表单 / 五态 / 图标 / 主题色）
    - §6.4.4 第三方依赖白名单流程（默认拒绝 + 已批准列表 + 默认拒绝列表）
    - §6.4.5 PR 检查项（CI 自动化）含 `scripts/check-style-discipline.mjs`
  - 强化 §13 不迁移项：列出"新代码不允许重新引入"清单
  - 改写 §15.2 / §15.6 / §15.7 / §16.5 中暧昧的样式表述（明确改用 ECharts geo / NLayout / UnoCSS grid 等现有能力）
  - §27.13 新增框架复用验收组
- v1.1 变更摘要：
  - 新增 §14 差距总览 + §15–§18 模块需求（27 项功能差距）
  - 新增 §19 EntitlementGate / §20 WS 网关 / §21 数据迁移 / §22 错误码与状态 / §23 i18n+审计+可观测性 / §24 测试数据 seed
  - §15.3 会议拆为 4 子项；§17.1 社区拆为 6 子项；§17.2 搜索拆为 4 子项；§18.4 文档协同拆为 3 层
  - 全部新接口补 API 契约样例 + 错误码段位
  - 新增 §28 风险与未决（7 个未决项 + 10 个风险登记）

## 1. 迁移目标

### 1.1 核心原则

- **功能完整迁移**：`frontend-v2/` 中所有业务模块必须 100% 迁移，不允许功能丢失
- **UI 以 Soybean Admin 为主**：布局、主题、导航、交互模式全部采用 Soybean Admin 体系
- **禁止重新自研基础设施**：现有 `frontend-admin/` 已经满足全部 UI、布局、主题、路由、权限、国际化、标签页、可视化基础设施。本期**不允许**新增手写 SCSS 文件、自研 CSS 变量、自研布局组件、自研主题系统、自研路由生成器、自研 i18n 抽象。所有功能优先用现有 Soybean / Naive UI / UnoCSS / `@sa/*` 包内能力实现，详见 §6
- **保留业务逻辑**：API 调用、数据模型、加密逻辑、权限模型原样迁移
- **渐进式迁移**：按模块分阶段迁移，每阶段可独立验证

### 1.2 技术收益

| 维度 | frontend-v2 现状 | v2.1.2 目标 |
|------|-----------------|-------------|
| 路由管理 | 手动定义 routes.ts（200+ 路由） | Elegant Router 自动生成 |
| CSS 方案 | 手写 SCSS + CSS 变量 | UnoCSS 原子化 + 主题预设 |
| 布局系统 | 自研 AppShell | Soybean AdminLayout（6 种布局模式） |
| 标签页 | 无 | 内置多标签页（chrome/button/slider） |
| 主题 | 自研 token 系统 | Soybean 主题预设 + Naive UI overrides |
| 国际化 | 自研 TextLike 类型 | Vue I18n 标准方案 |
| 权限 | 自研 auth-guard | Soybean 路由权限 + 静态/动态模式 |
| 代码规范 | ESLint | ESLint + Oxlint + Oxfmt |

## 2. 技术基线

| 项 | 版本 |
|---|---|
| Vue | ^3.5.0 |
| Naive UI | ^2.44.0 |
| Pinia | ^3.0.0 |
| Vue Router | ^5.0.0 |
| Vite | ^8.0.0 |
| TypeScript | ^6.0.0 |
| UnoCSS | ^66.0.0 |
| ECharts | ^6.0.0 |
| @vueuse/core | ^14.0.0 |
| @iconify/vue | ^5.0.0 |
| Vue I18n | ^11.0.0 |
| OpenPGP | ^6.1.0 |
| Axios | ^1.9.0 |

## 3. 架构映射

### 3.1 目录结构映射

```
frontend-admin/src/          →  新项目 src/
├── router/                      →  路由（Elegant Router 自动生成）
│   ├── elegant/                 →  自动生成的路由声明
│   ├── guard/                   →  路由守卫（auth + progress + title）
│   └── routes/                  →  路由配置
├── store/modules/               →  状态管理
│   ├── auth/                    →  认证状态
│   ├── theme/                   →  主题状态
│   ├── app/                     →  全局应用状态
│   ├── route/                   →  路由状态
│   └── tab/                     →  标签页状态
├── layouts/                     →  布局组件
│   ├── base-layout/             →  主布局（AdminLayout）
│   ├── blank-layout/            →  空白布局（登录/错误页）
│   └── modules/                 →  布局子组件
├── views/                       →  页面组件
│   ├── _builtin/                →  内置页面（login/403/404/500）
│   ├── home/                    →  工作台/仪表盘
│   ├── mail/                    →  邮件模块
│   ├── calendar/                →  日历模块
│   ├── drive/                   →  云盘模块
│   ├── docs/                    →  文档模块
│   ├── sheets/                  →  表格模块
│   ├── pass/                    →  密码管理模块
│   ├── collaboration/           →  协作模块
│   ├── command-center/          →  命令中心
│   ├── notifications/           →  通知模块
│   ├── admin/                   →  管理后台
│   ├── settings/                →  设置模块
│   └── community/               →  社区模块
├── service/                     →  API 服务层
│   ├── api/                     →  各模块 API
│   └── request/                 →  HTTP 请求封装
├── components/                  →  公共组件
├── hooks/                       →  组合式函数
├── locales/                     →  国际化
├── theme/                       →  主题配置
├── plugins/                     →  插件
├── utils/                       →  工具函数
└── styles/                      →  全局样式
```

### 3.2 布局系统映射

| frontend-v2 | v2.1.2 (Soybean Admin) |
|---|---|
| `V211AppShell`（自研） | `AdminLayout`（base-layout） |
| 左侧窄导航 `NLayoutSider` | Soybean `vertical-mix` 布局模式 |
| 顶部搜索栏 | `layouts/modules/global-header/` |
| 右侧上下文面板 | 自定义 slot 或抽屉 |
| 空白布局（登录页） | `blank-layout/` |

**推荐布局模式**：`vertical-mix`（左侧一级菜单图标 + 二级菜单展开），最接近 MMMail 设计稿的窄导航 + 内容区布局。

### 3.3 路由系统映射

frontend-v2 手动定义的路由将通过 Elegant Router 文件约定自动生成：

```
src/views/
├── mail/
│   ├── index.vue              → /mail
│   ├── inbox/index.vue        → /mail/inbox
│   ├── starred/index.vue      → /mail/starred
│   ├── drafts/index.vue       → /mail/drafts
│   ├── sent/index.vue         → /mail/sent
│   └── [folder]/index.vue     → /mail/:folder
├── calendar/index.vue         → /calendar
├── drive/
│   ├── index.vue              → /drive
│   ├── shared/index.vue       → /drive/shared
│   └── trash/index.vue        → /drive/trash
├── docs/index.vue             → /docs
├── sheets/index.vue           → /sheets
├── pass/index.vue             → /pass
├── collaboration/index.vue    → /collaboration
├── command-center/index.vue   → /command-center
├── notifications/index.vue    → /notifications
├── community/index.vue        → /community
├── admin/
│   ├── index.vue              → /admin
│   ├── users/index.vue        → /admin/users
│   ├── orgs/index.vue         → /admin/orgs
│   ├── security/index.vue     → /admin/security
│   └── audit/index.vue        → /admin/audit
└── settings/
    ├── index.vue              → /settings
    ├── profile/index.vue      → /settings/profile
    ├── security/index.vue     → /settings/security
    └── preferences/index.vue  → /settings/preferences
```

### 3.4 状态管理映射

| frontend-v2 Store | v2.1.2 Store | 说明 |
|---|---|---|
| `useAuthStore` | `store/modules/auth/` | 扩展 Soybean auth store，保留 MMMail 的 session/org/entitlement 逻辑 |
| `useMailStore` | `store/modules/mail/` | 新增，邮件状态 |
| `useCalendarStore` | `store/modules/calendar/` | 新增，日历状态 |
| `useDriveStore` | `store/modules/drive/` | 新增，云盘状态 |
| `useOrgStore` | `store/modules/org/` | 新增，组织状态 |
| `useNotificationStore` | `store/modules/notification/` | 新增，通知状态 |
| `useSettingsStore` | `store/modules/settings/` | 新增，用户设置 |
| — | `store/modules/theme/` | 沿用 Soybean 主题 store |
| — | `store/modules/route/` | 沿用 Soybean 路由 store |
| — | `store/modules/tab/` | 沿用 Soybean 标签页 store |
| — | `store/modules/app/` | 沿用 Soybean 应用 store |

## 4. 业务模块迁移清单

### 4.1 邮件模块（Mail）

**优先级**：P0（核心模块）

**功能清单**：

| 功能 | 源文件 | 迁移要求 |
|------|--------|----------|
| 邮件文件夹导航 | `views/app/mail/MailFolderRail.vue` | 适配 Soybean 侧栏菜单或自定义二级导航 |
| 邮件列表 | `views/app/mail/MailMessageList.vue` | 使用 NDataTable + 虚拟滚动 |
| 邮件阅读 | `views/app/mail/MailThreadReader.vue` | 保留线程视图，适配主内容区 |
| 邮件撰写 | `views/app/mail/MailComposePanel.vue` | 抽屉或模态方式 |
| 附件管理 | `views/app/mail/MailAttachments.vue` | NUpload 组件 |
| 邮件搜索 | `views/app/mail/MailSearchBar.vue` | 集成到 Soybean 全局搜索 |
| 邮件规则 | `views/app/mail/MailRulesPanel.vue` | 表单 + 表格 |
| 发件人身份 | `views/app/mail/MailIdentityManager.vue` | 设置子页面 |
| E2E 加密 | `views/app/mail/MailTrustPanel.vue` | 保留 OpenPGP 逻辑 |
| 联系人 | `views/app/mail/MailContacts.vue` | 独立视图 |

**API 接口**（`service/api/mail.ts`）：
- `GET /api/v1/mail/folders` — 获取文件夹列表
- `GET /api/v1/mail/messages` — 获取邮件列表（分页）
- `GET /api/v1/mail/messages/:id` — 获取邮件详情
- `POST /api/v1/mail/messages` — 发送邮件
- `PUT /api/v1/mail/messages/:id` — 更新邮件状态
- `DELETE /api/v1/mail/messages/:id` — 删除邮件
- `POST /api/v1/mail/drafts` — 保存草稿
- `GET /api/v1/mail/rules` — 获取邮件规则
- `POST /api/v1/mail/rules` — 创建规则
- `GET /api/v1/mail/identities` — 获取发件人身份
- `POST /api/v1/mail/trust/verify` — 验证收件人加密信任

### 4.2 日历模块（Calendar）

**优先级**：P0（核心模块）

**功能清单**：

| 功能 | 源文件 | 迁移要求 |
|------|--------|----------|
| 日历视图（周/月/日） | `views/app/calendar/CalendarBoard.vue` | 自定义日历网格组件 |
| 事件创建/编辑 | `views/app/calendar/CalendarEventDrawer.vue` | NDrawer + NForm |
| 参与者管理 | `views/app/calendar/CalendarAttendees.vue` | NSelect + 头像列表 |
| 可用性检查 | `views/app/calendar/CalendarAvailability.vue` | 时间轴可视化 |
| 冲突检测 | `views/app/calendar/CalendarConflictPanel.vue` | NAlert 提示 |
| 日历筛选 | `views/app/calendar/CalendarFilterSidebar.vue` | 侧栏筛选器 |
| 时区支持 | 业务逻辑层 | 保留 dayjs timezone 处理 |
| 提醒 | 业务逻辑层 | 通知集成 |

**API 接口**（`service/api/calendar.ts`）：
- `GET /api/v1/calendar/events` — 获取事件列表
- `POST /api/v1/calendar/events` — 创建事件
- `PUT /api/v1/calendar/events/:id` — 更新事件
- `DELETE /api/v1/calendar/events/:id` — 删除事件
- `GET /api/v1/calendar/availability` — 查询可用时间
- `POST /api/v1/calendar/events/:id/attendees` — 管理参与者

### 4.3 云盘模块（Drive）

**优先级**：P0（核心模块）

**功能清单**：

| 功能 | 源文件 | 迁移要求 |
|------|--------|----------|
| 文件列表 | `views/app/drive/DriveFileList.vue` | NDataTable + 图标视图切换 |
| 文件上传 | `views/app/drive/DriveUpload.vue` | NUploadDragger |
| 文件夹树 | `views/app/drive/DriveFolderTree.vue` | NTree |
| 文件预览 | `views/app/drive/DrivePreview.vue` | 模态预览器 |
| 文件分享 | `views/app/drive/DriveShareDialog.vue` | NModal + 权限设置 |
| 版本历史 | `views/app/drive/DriveVersionHistory.vue` | NTimeline |
| 存储用量 | `views/app/drive/DriveStorageUsage.vue` | NProgress |
| 回收站 | `views/app/drive/DriveTrash.vue` | 独立视图 |

**API 接口**（`service/api/drive.ts`）：
- `GET /api/v1/drive/files` — 获取文件列表
- `POST /api/v1/drive/files/upload` — 上传文件
- `POST /api/v1/drive/folders` — 创建文件夹
- `DELETE /api/v1/drive/files/:id` — 删除文件
- `GET /api/v1/drive/files/:id/versions` — 获取版本历史
- `POST /api/v1/drive/files/:id/share` — 分享文件
- `GET /api/v1/drive/storage` — 获取存储用量

### 4.4 文档模块（Docs）

**优先级**：P1

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| 文档列表 | NDataTable |
| 富文本编辑器 | 保留现有编辑器方案 |
| 协同编辑 | WebSocket 实时同步 |
| 文档分享 | 权限管理 |
| 模板管理 | 模板选择器 |
| 导出（PDF/Word） | 后端 API 调用 |

### 4.5 表格模块（Sheets）

**优先级**：P1

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| 电子表格网格 | 自定义 canvas/grid 组件（白名单豁免） |
| 公式编辑器 | 保留现有实现 |
| 单元格格式化 | 工具栏 + 属性面板 |
| 数据导入/导出 | NUpload + 后端处理 |
| 图表生成 | ECharts 集成 |

### 4.6 密码管理模块（Pass）

**优先级**：P1

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| 密码库列表 | NDataTable + 搜索 |
| 密码条目 CRUD | NForm + NDrawer |
| 密码生成器 | 自定义组件 |
| 密码强度检测 | NProgress + 评分 |
| 分类/标签 | NTag + NTree |
| 自动填充集成 | 浏览器扩展 API |
| 安全审计 | 弱密码/重复密码检测 |

### 4.7 协作模块（Collaboration）

**优先级**：P1

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| 项目看板 | 拖拽卡片（vue-draggable-plus） |
| 任务管理 | NDataTable + 状态流转 |
| 团队成员 | NAvatar 组 + 权限 |
| 评论/讨论 | 消息列表组件 |
| 文件关联 | Drive 模块集成 |

### 4.8 命令中心（Command Center）

**优先级**：P2

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| 工作流列表 | NDataTable |
| 工作流编辑器 | 可视化节点编辑器 |
| 运行历史 | NTimeline + 日志 |
| 触发器管理 | NForm |
| Runbook | Markdown 渲染 + 步骤执行 |

### 4.9 通知模块（Notifications）

**优先级**：P1

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| 通知列表 | NList + 分类标签页 |
| 通知偏好设置 | NForm + NSwitch |
| 实时推送 | WebSocket / SSE |
| 通知徽章 | 集成到 Soybean Header NBadge |
| 批量操作 | 全部已读/清除 |

### 4.10 管理后台（Admin）

**优先级**：P1

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| 用户管理 | NDataTable + CRUD |
| 组织管理 | NTree + NForm |
| 角色权限 | NCheckbox 矩阵 |
| 审计日志 | NDataTable + 筛选 |
| 安全监控 | ECharts 仪表盘 |
| 系统设置 | NForm |
| 计费/订阅 | 套餐卡片 + NStatistic |

### 4.11 设置模块（Settings）

**优先级**：P0

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| 个人资料 | NForm + NAvatar 上传 |
| 安全设置 | 密码修改 + 2FA |
| 偏好设置 | 主题/语言/通知偏好 |
| 会话管理 | 设备列表 + 踢出 |
| 数据导出 | 下载个人数据 |

### 4.12 社区模块（Community）

**优先级**：P2

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| 帖子列表 | NList + 分页 |
| 发帖/回复 | 富文本编辑器 |
| 分类/标签 | NTag + 筛选 |
| 用户主页 | 个人资料卡片 |

### 4.13 工作台/首页（Home/Workspace）

**优先级**：P0

**功能清单**：

| 功能 | 迁移要求 |
|------|----------|
| KPI 卡片 | NCard + NStatistic + NNumberAnimation |
| 活动时间线 | NTimeline |
| 快捷操作 | NButton 网格 |
| 待办事项 | NList + NCheckbox |
| 最近文件 | NDataTable |
| 日历预览 | 迷你日历组件 |
| 邮件预览 | 最近邮件列表 |

## 5. 认证与权限适配

### 5.1 认证流程

Soybean Admin 内置了完整的认证流程，需要适配 MMMail 后端：

```
┌─────────────────────────────────────────────────────────┐
│ Soybean Auth Store 扩展                                  │
├─────────────────────────────────────────────────────────┤
│ 保留 Soybean:                                           │
│   - token 存储/刷新机制                                  │
│   - 路由权限守卫                                         │
│   - 登录/登出流程框架                                    │
│                                                         │
│ 扩展 MMMail:                                            │
│   - 多组织上下文 (orgId scope header)                    │
│   - 产品权限 (entitlements)                              │
│   - 会话安全 (soft auth lock)                            │
│   - 2FA 验证流程                                         │
│   - OpenPGP 密钥管理                                     │
└─────────────────────────────────────────────────────────┘
```

### 5.2 API 请求适配

基于 Soybean 的 `@sa/axios` 包扩展：

| 功能 | 实现方式 |
|------|----------|
| Token 刷新 | 沿用 Soybean 的 `refreshToken` 拦截器 |
| 组织上下文 | 请求拦截器注入 `X-Org-Id` header |
| 错误码映射 | 适配 MMMail 后端错误码（code: 20001-90000） |
| 文件上传 | multipart/form-data 处理 |
| WebSocket | 独立连接管理（通知/协同编辑） |

### 5.3 权限模型

```typescript
// 路由 meta 扩展
interface RouteMeta {
  // Soybean 原有
  title: string
  i18nKey?: string
  icon?: string
  order?: number
  roles?: string[]

  // MMMail 扩展
  requiredEntitlements?: string[]  // 产品权限要求
  orgRequired?: boolean           // 是否需要组织上下文
  premiumOnly?: boolean           // 是否仅限付费用户
  adminOnly?: boolean             // 是否仅限管理员
}
```

## 6. 主题与品牌适配

### 6.1 主题策略

采用 Soybean Admin 主题系统，通过预设覆盖实现 MMMail 品牌：

```typescript
// src/theme/preset/mmmail.ts
export const mmmailThemePreset = {
  themeColor: '#2D9D8F',           // MMMail 品牌青绿色
  otherColor: {
    info: '#2080F0',
    success: '#18A058',
    warning: '#F0A020',
    error: '#D03050'
  },
  isCustomizeInfoColor: false,
  themeScheme: 'light',
  layout: {
    mode: 'vertical-mix',          // 左侧图标导航 + 二级菜单
    scrollMode: 'content'
  },
  sider: {
    inverted: true,                // 深色侧栏
    width: 220,
    collapsedWidth: 64,
    mixWidth: 90,
    mixCollapsedWidth: 64,
    mixChildMenuWidth: 200
  },
  header: {
    height: 56,
    breadcrumb: { visible: true }
  },
  tab: {
    visible: true,
    mode: 'chrome',
    cache: true
  },
  footer: {
    visible: false
  }
}
```

### 6.2 品牌资源

| 资源 | 位置 | 说明 |
|------|------|------|
| Logo | `src/assets/images/logo.svg` | MMMail 品牌 logo |
| Favicon | `public/favicon.svg` | 浏览器标签图标 |
| 登录页背景 | `src/views/_builtin/login/` | 自定义登录页设计 |
| 产品名 | `src/constants/app.ts` | `MMMail` 统一声明 |

### 6.3 产品模块色

沿用 v2.1.1 的模块识别色，注入 UnoCSS 自定义颜色：

```typescript
// uno.config.ts 扩展
theme: {
  colors: {
    'module-mail': '#4C6EF5',
    'module-calendar': '#F59F00',
    'module-drive': '#12B886',
    'module-docs': '#7950F2',
    'module-sheets': '#20C997',
    'module-pass': '#E64980',
    'module-collaboration': '#FD7E14',
    'module-command': '#845EF7',
    'module-notifications': '#FA5252',
    'module-admin': '#495057',
    'module-settings': '#868E96'
  }
}
```

### 6.4 Soybean Admin 框架强制复用约束（必读）

> 现有 `frontend-admin/` 已经满足本次迁移所有 UI / 布局 / 路由 / 权限 / 主题 / i18n / 可视化 / 网络层基础设施。**所有新代码必须先尝试用现有能力实现**；要新增第三方依赖、写 SCSS 或自研组件，必须走 §6.4.4 白名单流程。

#### 6.4.1 必须复用的现有能力（不允许重写）

**布局 / 容器**

| 类别 | 必须使用 | 位置 |
|---|---|---|
| 主布局 | `BaseLayout` | `src/layouts/base-layout/index.vue` |
| 空白布局 | `BlankLayout` | `src/layouts/blank-layout/` |
| Header | `GlobalHeader` | `src/layouts/modules/global-header/` |
| 侧栏 | `GlobalSider` + `GlobalMenu` | `src/layouts/modules/global-sider/`、`global-menu/` |
| 标签页 | `GlobalTab` | `src/layouts/modules/global-tab/` |
| 面包屑 | `GlobalBreadcrumb` | `src/layouts/modules/global-breadcrumb/` |
| 主题抽屉 | `ThemeDrawer` | `src/layouts/modules/theme-drawer/` |
| 全局搜索 | `GlobalSearch` | `src/layouts/modules/global-search/`（§17.2 命令面板必须挂在此处，禁止另起一套） |
| 内容容器 | `GlobalContent` | `src/layouts/modules/global-content/` |
| Logo | `GlobalLogo` | `src/layouts/modules/global-logo/` |
| AdminLayout 包 | `@sa/materials` 的 `AdminLayout`、`PageTab`、`SimpleScrollbar` | `packages/materials/src/libs/` |

**通用组件**

| 类别 | 必须使用 | 位置 |
|---|---|---|
| 用户头像 | `SoybeanAvatar` | `src/components/custom/soybean-avatar.vue` |
| 系统 Logo | `system-logo.vue` | `src/components/common/` |
| 全屏切换 | `full-screen.vue` | `src/components/common/` |
| 主题切换 | `theme-schema-switch.vue` | `src/components/common/` |
| 语言切换 | `lang-switch.vue` | `src/components/common/` |
| 菜单折叠 | `menu-toggler.vue` | `src/components/common/` |
| Pin 切换 | `pin-toggler.vue` | `src/components/common/` |
| 重载按钮 | `reload-button.vue` | `src/components/common/` |
| Icon 提示 | `icon-tooltip.vue` | `src/components/common/` |
| 空状态 | `look-forward.vue` + Naive `NEmpty` | `src/components/custom/` |
| 异常页 | `exception-base.vue` | `src/components/common/` |
| 暗色容器 | `dark-mode-container.vue` | `src/components/common/` |
| 数字滚动 | `count-to.vue` | `src/components/custom/` |
| SVG 图标 | `svg-icon.vue` | `src/components/custom/` |
| Better Scroll | `better-scroll.vue` | `src/components/custom/` |
| 表格列设置 | `table-column-setting.vue` | `src/components/advanced/` |
| 表头操作栏 | `table-header-operation.vue` | `src/components/advanced/` |

> 上面列出的组件**已覆盖** §15–§16 中需要的"图标/头像/空状态/全屏/暗色/列设置/表头操作/滚动"等所有诉求。新模块只需引用，不允许复制重写。

**业务能力（store / hooks）**

| 必须使用 | 位置 | 用途 |
|---|---|---|
| `useAuthStore` | `src/store/modules/auth/` | §5 鉴权扩展点 |
| `useThemeStore` | `src/store/modules/theme/` | §6 主题完全沿用 |
| `useRouteStore` | `src/store/modules/route/` | 路由权限 |
| `useTabStore` | `src/store/modules/tab/` | 标签页 |
| `useAppStore` | `src/store/modules/app/` | 全局应用 |
| `src/hooks/business/` | — | 业务通用 hooks（loading / 表单 / 表格） |
| `src/hooks/common/` | — | 通用 hooks（element-size / icon / context） |
| `@sa/hooks` | `packages/hooks/` | 跨包共享 hooks |

**UI 库与样式系统（唯一允许）**

| 类别 | 唯一允许 | 备注 |
|---|---|---|
| 组件库 | **Naive UI 2.44**（`naive-ui`） | 严禁同时引入 Element Plus / Ant Design Vue / Vant |
| 样式 | **UnoCSS 原子化**（`uno.config.ts` 已配置） | 优先 utility class；主题色用 `mmmailThemePreset`（§6.1） |
| 图标 | **`@iconify/vue` + `unplugin-icons`** | 通过 `<icon-park-outline:xxx />` JSX 标签或 `<SvgIcon />` 引入；禁止自定义 SVG 文件除非确无可用图标 |
| 图表 | **ECharts 6** | 严禁引入 Chart.js / D3 直接绘图 |
| 地图 | Naive UI 不内置；优先用 ECharts geo 系列 | 禁止默认引入 MapBox/Google Maps；如确需走 §6.4.4 白名单 |
| 拖拽 | **`vue-draggable-plus` 0.6.1**（已在 `package.json`） | 禁止 Sortable.js / vue-draggable（旧） |
| 富文本 | `@sa/materials` 未内置；走 §6.4.4 流程统一选 1 个（建议 Tiptap） | §17.1 / §15.6 共用 |
| 滚动 | `@better-scroll/core` + `simple-scrollbar` | 禁止 perfect-scrollbar |
| 国际化 | **Vue I18n 11**（`src/locales/`） | 禁止自研 TextLike |
| 网络 | **`@sa/axios`**（已封装拦截器/刷新/错误统一） | 禁止裸 axios / fetch / ofetch |
| 日期 | **dayjs 1.11** | 禁止 moment / date-fns |

**响应式断点 / 设计 token**

- 沿用 UnoCSS 默认断点 + Soybean 主题 store 暴露的 `themeColors`，禁止自定义 CSS 变量另起体系
- §6.3 中的"模块色"通过 UnoCSS `theme.colors` 注入，使用 utility class（`text-module-mail`、`bg-module-drive`），禁止 inline style 或 `<style scoped>` 内手写颜色

#### 6.4.2 严格禁止的做法

- ❌ 新建 `*.scss` 文件（除 `src/styles/scss/` 已存在的全局文件维护外）
- ❌ 新建自研 CSS 变量体系（如 `--mm-*`、`--v211-*`）
- ❌ 自研 layout 组件（任何 `*Shell`、`*Frame`、`*Container`，必须用 `BaseLayout`/`AdminLayout`）
- ❌ 自研主题系统 / 主题 token JSON（沿用 §6.1 `mmmailThemePreset`）
- ❌ 重写已有的 `useThemeStore` / `useTabStore` / `useRouteStore` / `useAppStore`
- ❌ 重写 `service/request`，必须用 `@sa/axios`
- ❌ 重写错误页 / 登录页 / 403 / 404 / 500（直接复用 `src/views/_builtin/`，仅替换 logo 与文案）
- ❌ 引入第二个组件库（Element Plus、Ant Design Vue 等）
- ❌ 引入第二个样式方案（Tailwind、原生 SCSS module 等）
- ❌ 自定义 CSS Grid / Flex 布局类（直接用 UnoCSS：`grid grid-cols-3 gap-4`、`flex items-center`）
- ❌ 在 `<style scoped>` 内写超过 5 行的样式（超过即视为应抽到 UnoCSS preset / shortcut）

#### 6.4.3 需要新增页面 / 组件时的标准做法

1. 路由：在 `src/views/{module}/` 下新建 `index.vue`，由 Elegant Router 自动生成路由声明，**不要手写 `routes.ts`**
2. 页面骨架：`<template>` 顶层用 Naive UI 容器（`NCard` / `NSpace` / `NGrid` / `NLayout`），不要写自定义 div + class
3. 表格：`NDataTable` + `table-column-setting.vue` + `table-header-operation.vue`（已封装常用操作）
4. 表单：`NForm` + `NFormItem`，校验用 `useNaiveForm`（项目内已有的 hook）
5. 空 / 错 / 加载：`NEmpty` + `<look-forward />` / `<exception-base />` / `NSpin` + `NSkeleton`，绑定 §22.2 五态规范
6. 弹层：`NModal` / `NDrawer` / `NPopover` / `useDialog` / `useMessage`
7. 图标：`<svg-icon icon="icon-park-outline:mail" />` 或 unplugin-icons 自动 import
8. 主题色：`bg-primary` / `text-primary`（UnoCSS 通过 Soybean preset 注入），需要模块色用 `text-module-{mail|calendar|drive|...}`
9. 暗色：所有色值通过 `bg-layout` / `text-base` 等 Naive theme 变量，禁止 hard-code `#fff`/`#000`

#### 6.4.4 新增第三方依赖白名单流程

- **默认拒绝**新增依赖。如确实需要，必须满足：
  1. Soybean / Naive UI / `@sa/*` / 现有 dependencies 都无法实现
  2. 包大小 < 50KB（gzip）或在 §23.5 Bundle 预算内
  3. MIT / Apache-2.0 等宽松许可
  4. 最近一年有活跃维护
- 流程：在 `docs/superpowers/specs/2026-05-15-v212-decision-log.md`（§28.4 提到的占位文件）登记 `Q-{n}` 决议项，给出"为何 Soybean 现状不能满足"的论证，由 Tech Lead 批准
- v2.1.2 已默认通过的新依赖：`vue-draggable-plus`（已在 lock）、`monaco-editor`（仅 Drive 文本对比，懒加载）、Tiptap（CRDT Docs 编辑器，懒加载）
- v2.1.2 默认拒绝：MapBox（用 ECharts geo 替代）、Element Plus、Ant Design、Tailwind、moment、Sortable.js、perfect-scrollbar、Chart.js、D3

#### 6.4.5 PR 检查项（CI 自动化）

下列检查在 PR 合并前必须通过：

- `pnpm typecheck` 通过（vue-tsc）
- `pnpm lint`（oxlint + eslint）通过
- `pnpm fmt`（oxfmt）通过；diff 必须为空
- 自定义脚本 `scripts/check-style-discipline.mjs`（本期新增）：
  - 扫描新增 `.scss` 文件 → 失败
  - 扫描 `<style>` 块超过 5 行 → 警告（可豁免）
  - 扫描裸 `import axios` / `import 'sortablejs'` 等禁用库 → 失败
  - 扫描 `--mm-*` / `--v211-*` 等自定义 CSS 变量 → 失败
- `pnpm gen:api` 后 `git diff --exit-code`（§23.6）

## 7. 国际化适配

### 7.1 语言支持

| 语言 | 代码 | 优先级 |
|------|------|--------|
| 简体中文 | zh-CN | P0 |
| 繁体中文 | zh-TW | P1 |
| English | en-US | P1 |

### 7.2 翻译文件结构

```
src/locales/
├── langs/
│   ├── zh-CN/
│   │   ├── common.ts        ← Soybean 通用翻译
│   │   ├── route.ts         ← 路由标题
│   │   ├── mail.ts          ← 邮件模块
│   │   ├── calendar.ts      ← 日历模块
│   │   ├── drive.ts         ← 云盘模块
│   │   ├── docs.ts          ← 文档模块
│   │   ├── sheets.ts        ← 表格模块
│   │   ├── pass.ts          ← 密码管理
│   │   ├── collaboration.ts ← 协作模块
│   │   ├── admin.ts         ← 管理后台
│   │   └── settings.ts      ← 设置模块
│   ├── zh-TW/               ← 同结构
│   └── en-US/               ← 同结构
└── index.ts
```

### 7.3 迁移规则

- frontend-v2 中的 `TextLike` 类型废弃，统一使用 Vue I18n 的 `$t()` 函数
- 所有用户可见文案必须走 i18n，禁止硬编码
- 产品名 `MMMail` 通过 i18n 变量注入，不硬编码到模板

## 8. API 服务层迁移

### 8.1 服务模块清单

从 `frontend-v2/src/service/api/` 迁移以下模块：

| 模块 | 源文件 | 接口数量（估） |
|------|--------|---------------|
| auth | `auth.ts` | 15+ |
| mail | `mail.ts` | 20+ |
| calendar | `calendar.ts` | 12+ |
| drive | `drive.ts` | 15+ |
| docs | `docs.ts` | 10+ |
| sheets | `sheets.ts` | 8+ |
| pass | `pass.ts` | 12+ |
| collaboration | `collaboration.ts` | 10+ |
| command-center | `command-center.ts` | 8+ |
| notifications | `notifications.ts` | 6+ |
| admin | `admin.ts` | 20+ |
| settings | `settings.ts` | 10+ |
| community | `community.ts` | 8+ |
| org | `org.ts` | 10+ |
| user | `user.ts` | 8+ |
| search | `search.ts` | 4+ |
| share | `share.ts` | 6+ |
| labs | `labs.ts` | 4+ |
| suite | `suite.ts` | 6+ |
| security | `security.ts` | 8+ |
| workspace | `workspace.ts` | 6+ |

### 8.2 请求封装适配

```typescript
// src/service/request/index.ts
// 基于 Soybean 的 @sa/axios 扩展

import { createRequest } from '@sa/axios'

export const request = createRequest({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  // MMMail 扩展：组织上下文注入
  onRequest(config) {
    const orgStore = useOrgStore()
    if (orgStore.currentOrgId) {
      config.headers['X-Org-Id'] = orgStore.currentOrgId
    }
    return config
  },
  // MMMail 扩展：错误码映射
  onResponseError(error) {
    const code = error.response?.data?.code
    // MMMail 错误码: 20001-认证失败, 30001-权限不足, 90000-内部错误
    handleMmmailError(code, error.response?.data?.message)
  }
})
```

## 9. 公共组件迁移

### 9.1 保留 Soybean 内置组件

| 组件 | 用途 |
|------|------|
| `SoybeanAvatar` | 用户头像 |
| `TableColumnSetting` | 表格列配置 |
| `TableHeaderOperation` | 表格头部操作栏 |
| `LookForward` | 空状态 |
| `MenuToggler` | 菜单折叠按钮 |
| `FullScreen` | 全屏切换 |
| `ThemeSchemeSwitch` | 主题切换 |
| `LangSwitch` | 语言切换 |

### 9.2 新增 MMMail 业务组件

| 组件 | 用途 | 位置 |
|------|------|------|
| `MmMailComposer` | 邮件编辑器 | `src/components/business/mail/` |
| `MmCalendarGrid` | 日历网格 | `src/components/business/calendar/` |
| `MmFilePreview` | 文件预览器 | `src/components/business/drive/` |
| `MmRichEditor` | 富文本编辑器 | `src/components/business/editor/` |
| `MmEncryptionBadge` | 加密状态标识 | `src/components/business/security/` |
| `MmStorageBar` | 存储用量条 | `src/components/business/drive/` |
| `MmUserPicker` | 用户选择器 | `src/components/business/common/` |
| `MmOrgSwitcher` | 组织切换器 | `src/components/business/common/` |
| `MmCommandPalette` | 命令面板（Ctrl+K） | `src/components/business/common/` |

## 10. 迁移阶段规划

### Phase 1：基础框架（1 周）

- [ ] 基于 frontend-admin 初始化项目
- [ ] 配置 MMMail 主题预设（品牌色、布局模式）
- [ ] 配置 API 请求层（baseURL、拦截器、错误处理）
- [ ] 扩展 Auth Store（适配 MMMail 登录/注册/session）
- [ ] 配置国际化（zh-CN 基础翻译）
- [ ] 登录/注册页面实现
- [ ] 路由权限守卫适配

**验收标准**：能登录、看到空白工作台、侧栏导航可点击

### Phase 2：核心模块（2 周）

- [ ] 工作台/首页（KPI 卡片、活动时间线、快捷操作）
- [ ] 邮件模块（完整 CRUD + 线程 + 搜索）
- [ ] 日历模块（事件 CRUD + 视图切换）
- [ ] 云盘模块（文件 CRUD + 上传 + 分享）
- [ ] 设置模块（个人资料 + 安全 + 偏好）
- [ ] 通知模块（列表 + 实时推送）

**验收标准**：核心业务流程可走通（收发邮件、创建事件、上传文件）

### Phase 3：扩展模块（2 周）

- [ ] 文档模块（列表 + 编辑器 + 协同）
- [ ] 表格模块（网格 + 公式 + 图表）
- [ ] 密码管理模块（CRUD + 生成器 + 审计）
- [ ] 协作模块（看板 + 任务 + 讨论）
- [ ] 管理后台（用户/组织/角色/审计）
- [ ] 社区模块（帖子 + 回复）

**验收标准**：所有模块页面可访问，核心功能可用

### Phase 4：完善与优化（1 周）

- [ ] 命令中心（工作流编辑器）
- [ ] 全局搜索（Ctrl+K 命令面板）
- [ ] E2E 加密完整流程
- [ ] 多语言完善（zh-TW + en-US）
- [ ] 响应式适配（移动端）
- [ ] 性能优化（懒加载、虚拟滚动）
- [ ] 无障碍合规（WCAG 2.1 AA）

**验收标准**：功能与 frontend-v2 完全对等，UI 质量超越

## 11. 兼容性要求

### 11.1 浏览器支持

| 浏览器 | 最低版本 |
|--------|----------|
| Chrome | 90+ |
| Firefox | 90+ |
| Safari | 15+ |
| Edge | 90+ |

### 11.2 响应式断点

沿用 Soybean Admin + UnoCSS 默认断点：

| 断点 | 宽度 | 布局行为 |
|------|------|----------|
| sm | 640px | 侧栏隐藏，汉堡菜单 |
| md | 768px | 侧栏折叠为图标 |
| lg | 1024px | 侧栏展开 |
| xl | 1280px | 完整布局 |
| 2xl | 1536px | 宽屏优化 |

## 12. 质量要求

### 12.1 代码规范

- ESLint + Oxlint 零警告
- TypeScript strict 模式，无 `any` 逃逸
- 组件 props 必须有 TypeScript 类型定义
- 所有 API 响应必须有类型定义

### 12.2 测试要求

| 类型 | 覆盖率要求 | 工具 |
|------|-----------|------|
| 单元测试 | 核心业务逻辑 80%+ | Vitest |
| 组件测试 | 公共组件 100% | @vue/test-utils |
| E2E 测试 | 核心流程覆盖 | Playwright |

### 12.3 性能指标

| 指标 | 目标 |
|------|------|
| FCP | < 1.5s |
| LCP | < 2.5s |
| TTI | < 3.5s |
| Bundle Size (gzip) | < 500KB（首屏） |
| 路由懒加载 | 所有非首屏模块 |

## 13. 不迁移项

以下 frontend-v2 中的内容不迁移到 v2.1.2，且**新代码不允许重新引入**（违者 PR 拒收）：

| 项 | 原因 | 替代 |
|---|---|---|
| `src/design-system/v211/` | 被 Soybean 主题系统替代 | `src/theme/preset/mmmail.ts`（§6.1） |
| `V211AppShell` 组件 | 被 Soybean AdminLayout 替代 | `BaseLayout` + `@sa/materials` AdminLayout（§6.4.1） |
| 自研 CSS 变量系统 (`--v211-*`、`--mm-*`) | 被 UnoCSS + Naive overrides 替代 | UnoCSS utility class + Naive theme overrides |
| 手动路由定义 (`routes.ts`) | 被 Elegant Router 替代 | `src/views/{module}/index.vue` 自动生成 |
| 自研 `TextLike` 国际化类型 | 被 Vue I18n 替代 | `vue-i18n` 11 + `src/locales/`（§7） |
| `v211-*.test.mjs` 合规测试 | 重写为新架构的测试 | Vitest + Playwright（§26.3） |
| 自研 axios 封装 | `@sa/axios` 已涵盖拦截器/刷新/错误统一 | `@sa/axios` + `src/service/request/` |
| 自研全屏 / 主题切换 / 语言切换 | Soybean 内置组件已覆盖 | `src/components/common/` 直接引用 |
| 任何 SCSS module / 手写样式文件 | UnoCSS 已覆盖 99% 场景 | UnoCSS utility / shortcut / preset |
| 第二个组件库（Element Plus 等） | 一个项目一套 UI 库 | Naive UI |

## 14. 功能差距与新增需求（v2.1.2 必须闭环）

> 本章基于 2026-05-15 完成的前后端差距分析，把当前 `frontend-v2/` + `backend/` 中**完全缺失**与**部分缺失**的功能拆分成迁移期必须闭环的需求项。所有需求统一按以下结构描述：现状、目标、依赖接口、UI/UX 要求、数据模型、验收标准、优先级、估算。
>
> **统一约定**：
> - 优先级：P0=阻塞发布；P1=与发布同期交付；P2=可滚动到 v2.1.3 但需在 spec 中保留占位
> - 估算单位：人日（PD），按 1 名熟悉新栈的全栈工程师计算
> - 路由全部基于 Elegant Router 文件约定生成（见 §3.3）；后端接口前缀统一为 `/api/v1` 或 `/api/v2`，沿用现有后端命名
> - 凡涉及付费门控的模块，前端必须由 §19.1 `EntitlementGate` 组件渲染降级 UI（驱动数据：`auth-store.entitlements` + 路由 meta，见 §5.3）；后端必须在 Controller 上加 `@RequireEntitlement(...)` / `@RequireRole(...)` 注解（沿用 v2.1 已上线的权限注解切面），未授权时返回 `403 + 错误码 30001`

### 14.1 差距总览

| 类别 | 数量 | 摘要 |
|---|---|---|
| F-A 前端完全缺失 | 7 个模块 | 钱包、VPN、会议、联系人独立、SimpleLogin、Standard Notes、TOTP 管理 |
| F-B 前端部分缺失 | 9 项功能 | Suite/Business 真数据、邮件规则 UI、邮件拖拽、Drive 版本/E2EE 分享、设置域名/WebPush、Admin 计费 |
| B-A 后端完全缺失 | 5 个模块 | 社区、全局搜索、命令面板契约、自定义域名（解锁 F-B 7）、WebPush 订阅 API（解锁 F-B 8） |
| B-B 后端部分缺失 | 8 项功能 | CalDAV、RRULE、IMAP/SMTP、协同编辑、表格公式、看板排序、WS 推送、登录异常检测 |

### 14.2 模块编号与依赖

```
F-A 1  钱包 Wallet              ← 后端 WalletController（已就绪）
F-A 2  VPN                      ← 后端 VpnController（已就绪）
F-A 3  会议 Meet                ← 后端 MeetController + MeetGuest + MeetPublic（已就绪）
F-A 4  联系人 Contacts          ← 后端 ContactController + ContactGroupController（已就绪）
F-A 5  SimpleLogin              ← 后端 SimpleLoginController（已就绪）
F-A 6  Standard Notes           ← 后端 StandardNotesController（已就绪）
F-A 7  TOTP / Authenticator     ← 后端 AuthenticatorController（已就绪）

F-B 1  Suite 工作台真数据       ← 复用 WorkspaceAggregationController
F-B 2  Business Overview 真数据 ← 复用 OrgBusinessController
F-B 3  邮件规则编辑 UI          ← 后端 MailFilterController（已就绪）
F-B 4  邮件标签/文件夹拖拽       ← 后端 LabelController + MailFolderController（已就绪）
F-B 5  Drive 版本历史 UI        ← 后端 DriveController（versions 接口已就绪）
F-B 6  Drive E2EE 分享         ← 后端 PublicShareCapabilityController（已就绪）
F-B 7  设置-自定义域名         ← 依赖 B-A：本期需补 DomainController
F-B 8  设置-Web Push           ← 依赖 B-A：本期需补 WebPush 订阅 API
F-B 9  Admin 计费/订阅         ← 后端 SuiteBillingController（已就绪）

B-A 1  社区 Community           ← 全新模块
B-A 2  全局搜索 Search          ← 跨模块全文检索
B-A 3  命令面板契约 Cmd-K       ← 扩展 V21OpsController.command-center
B-A 4  自定义域名 Domain         ← 全新 DomainController（解锁 F-B 7）
B-A 5  Web Push 订阅 API         ← 全新 WebPushController（解锁 F-B 8）

B-B 1  日历 CalDAV              ← 扩展 V21CalendarController
B-B 2  日历 RRULE               ← 扩展 V21CalendarController
B-B 3  邮件 IMAP/SMTP 外部账号  ← 扩展 MailController
B-B 4  文档实时协同 (CRDT)      ← 新增 WebSocket 服务
B-B 5  表格公式引擎             ← 扩展 SheetsController
B-B 6  看板拖拽排序             ← 扩展 V21OpsController.collaboration
B-B 7  通知 WebSocket 推送      ← 扩展 V21OpsController.notifications
B-B 8  登录异常检测             ← 扩展 AuthController + 审计

```

---

## 15. F-A：前端完全缺失模块需求

### 15.1 钱包 / 加密货币（Wallet）

**优先级**：P1 / **估算**：8 PD / **门控**：`entitlement: WALLET`

**现状**：后端 `WalletController` 已实现账户、交易、邮件转账、批量推进/对账、签名/广播、助记词恢复等接口；前端无任何对应路由与页面。

**目标**：在 `views/wallet/` 下交付完整钱包工作台，使付费用户可通过浏览器完成创建账户、查看交易、发起邮件转账、签名/广播、对账、批处理推进的端到端闭环。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/wallet` | `views/wallet/index.vue` | 钱包总览：资产卡片 + 最近交易 + 待处理任务 |
| `/wallet/accounts` | `views/wallet/accounts/index.vue` | 账户列表（CRUD + 助记词恢复） |
| `/wallet/transactions` | `views/wallet/transactions/index.vue` | 交易列表（含状态、链、Tx Hash、过滤） |
| `/wallet/transactions/[txId]` | `views/wallet/transactions/[txId]/index.vue` | 交易详情：执行计划、轨迹、签名、广播、失败补救 |
| `/wallet/send` | `views/wallet/send/index.vue` | 发起转账（支持邮件转账） |
| `/wallet/receive` | `views/wallet/receive/index.vue` | 收款（含二维码、地址簿） |
| `/wallet/reconciliation` | `views/wallet/reconciliation/index.vue` | 对账总览（批量推进 / 批量补救 / 批量对账） |

**通用 meta**：`{ requires: ['WALLET'], orgRequired: true, premiumOnly: true, featureFlag: 'feat.wallet.enabled' }`

**后端依赖（已就绪，无需变更）**

```
GET    /api/v1/wallet/accounts
POST   /api/v1/wallet/accounts
GET    /api/v1/wallet/transactions
GET    /api/v1/wallet/execution-overview
GET    /api/v1/wallet/execution-plan
GET    /api/v1/wallet/transactions/{txId}/execution-trace
GET    /api/v1/wallet/reconciliation-overview
POST   /api/v1/wallet/transactions/receive
POST   /api/v1/wallet/transactions/send
POST   /api/v1/wallet/transactions/{txId}/{advance|remediate|confirm|sign|broadcast|fail}
POST   /api/v1/wallet/transactions/batch-{advance|remediate|reconcile}
```

**API 契约样例**

```jsonc
// POST /api/v1/wallet/transactions/send 请求
{ "fromAccountId":"wa_01H...","chain":"ETH","recipientEmail":"alice@x.com",
  "amountWei":"1000000000000000000","memo":"Q2 Bonus","gasOption":"FAST" }
// 202 { "txId":"tx_01H...","state":"INITIATED","estimatedConfirmAt":"..." }
// POST /api/v1/wallet/transactions/{txId}/sign 请求
{ "signedRawTxHex":"0xf86c...","signerKeyId":"k_..." }
// 200 { "state":"SIGNED" }
// 错误：40021 INSUFFICIENT_BALANCE, 42221 RECIPIENT_INVALID, 42321 GAS_TOO_LOW
```

**UI/UX 要求**

- 总览页：4 张资产卡（NCard + NStatistic + NNumberAnimation），右侧"最近交易"NTimeline，下方"待处理任务" NDataTable
- 转账表单：链 / 资产 / 收件人邮箱（触发邮件转账分支）/ 金额 / Gas 选项（NFormItem + NSelect + NInputNumber）
- 交易状态机用 NSteps 可视化（发起 → 签名 → 广播 → 确认 / 失败 → 补救），每步显示链上回执
- 助记词恢复采用 12/24 词分块输入（NInput + 自定义校验），完成前禁止网络上传
- 所有金额数字均使用等宽字体并对齐右边距

**数据模型（前端）**

```typescript
interface WalletAccount {
  id: string
  label: string
  chain: 'ETH' | 'BTC' | 'POLYGON' | 'BASE'
  address: string
  balanceWei: string         // 后端均为字符串，前端不做精度转换
  balanceFiat: string
  isDefault: boolean
  recoveryRequired: boolean
}
interface WalletTransaction {
  id: string
  accountId: string
  direction: 'IN' | 'OUT'
  state: 'INITIATED'|'SIGNED'|'BROADCAST'|'CONFIRMED'|'FAILED'|'REMEDIATING'
  amountWei: string
  recipient: { address: string; emailHandle?: string }
  txHash?: string
  createdAt: string
  trace: WalletTraceEntry[]
}
```

**Pinia store**：`store/modules/wallet/`，至少导出 `useWalletStore` 含 `accounts`、`transactions`、`pendingActions` 三个响应式集合，所有写操作在动作完成后**强制 refetch**对应详情，避免乐观更新与链上状态错位。

**验收标准**

- 7 条路由均可访问；未持有 `WALLET` entitlement 的用户访问时落到 §5.3 定义的"未授权付费"占位页
- 创建账户 → 收款 → 邮件发起转账 → 签名 → 广播 → 确认 6 步全流程能在 UI 内完成且最终页面显示链上 Tx Hash
- 失败交易在 `/wallet/transactions/[txId]` 可点击"补救"调用 `POST /transactions/{txId}/remediate`
- 助记词恢复表单在网络层断网状态下仍能完成本地校验
- E2E：Playwright 用例覆盖发起转账与补救路径

### 15.2 VPN

**优先级**：P1 / **估算**：5 PD / **门控**：`entitlement: VPN`

**现状**：后端 `VpnController` 提供服务器目录、连接配置、会话管理；前端无路由。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/vpn` | `views/vpn/index.vue` | 总览：当前会话、快速连接、地图 |
| `/vpn/servers` | `views/vpn/servers/index.vue` | 服务器目录（按地区/负载/特性筛选） |
| `/vpn/profiles` | `views/vpn/profiles/index.vue` | 连接配置（CRUD：协议、DNS、Killswitch） |
| `/vpn/sessions` | `views/vpn/sessions/index.vue` | 会话历史（可强制下线） |
| `/vpn/settings` | `views/vpn/settings/index.vue` | 全局偏好（自动重连、协议、DNS over HTTPS） |

**通用 meta**：`{ requires: ['VPN'], orgRequired: false, premiumOnly: true, featureFlag: 'feat.vpn.enabled' }`

**API 契约样例**

```jsonc
// POST /api/v1/vpn/sessions/quick-connect 请求
{ "preferredRegion": "AUTO", "protocol": "WIREGUARD" }
// 200 响应
{ "sessionId": "vs_01H...", "serverId": "sv_jp_tokyo_3",
  "exitIp": "203.0.113.10", "egressCity": "Tokyo",
  "connectedAt": "2026-05-15T10:00:00Z" }
// 错误：40921 ALREADY_CONNECTED, 50321 NO_SERVER_AVAILABLE
```

**后端依赖**（`/api/v1/vpn/*`，已就绪）：`servers`、`settings`、`profiles` CRUD、`sessions/current|history`、`sessions/{connect|quick-connect|disconnect}`。

**UI/UX 要求**

- 总览左半屏世界地图（**ECharts 6 geo 系列**，使用 `world.json` 资源；禁止引入 MapBox），右半屏"当前会话"卡片：状态 / 入口节点 / 出口 IP / 加密协议 / 已连接时长（NTime）
- 快速连接按钮触发 `POST /sessions/quick-connect`，Loading 期间禁用，并显示"正在握手"
- 服务器目录表格列：国家、城市、负载条 (NProgress)、Ping、特性 Tag（P2P / Streaming / Tor）、连接按钮
- 配置表单使用 NForm + NRadioGroup（协议：WireGuard/OpenVPN）+ NSwitch（Killswitch、Split Tunnel、LAN 允许）

**验收标准**

- 快速连接 → 切换服务器 → 断开三条主路径在网络受限环境下均显示明确状态
- 离线时显示降级提示，不阻塞页面渲染
- 未持有 entitlement 时显示购买引导而非空白
- E2E：连接/断开 + 配置 CRUD

### 15.3 会议（Meet）—— 总览

**优先级**：P1 / **总估算**：14 PD（拆分为 15.3.1–15.3.4） / **门控**：`entitlement: MEET`

**现状**：后端 `MeetController` + `MeetGuestController` + `MeetPublicController` 已就绪；前端无任何会议入口。

**路由与页面**

| 路由 | 文件 | 鉴权 | 说明 |
|---|---|---|---|
| `/meet` | `views/meet/index.vue` | 已登录 | 大堂：当前会议、历史、加入码输入 |
| `/meet/access` | `views/meet/access/index.vue` | 已登录 | 接入开通、Waitlist、销售联系、激活 |
| `/meet/rooms` | `views/meet/rooms/index.vue` | 已登录 | 我的会议室列表 + 创建按钮 |
| `/meet/rooms/[roomId]` | `views/meet/rooms/[roomId]/index.vue` | 已登录或访客 | **会议室主舞台**（视频墙、轨道控制、聊天、参与者面板、主持人控制） |
| `/meet/rooms/[roomId]/lobby` | `views/meet/rooms/[roomId]/lobby/index.vue` | 已登录或访客 | 入会前预览（设备测试、虚拟背景、姓名） |
| `/meet/join/[code]` | `views/meet/join/[code]/index.vue` | 公开 | 加入码登陆点（公开） |
| `/meet/host/[roomId]` | `views/meet/host/[roomId]/index.vue` | 主持人 | 访客请求审批、参与者管理、主持人转移 |

**通用 meta**：`{ requires: ['MEET'], orgRequired: false, premiumOnly: true }`（公开路由除外）

#### 15.3.1 房间生命周期

**估算**：3 PD

**目标**：开通/Waitlist/接入激活、房间 CRUD、加入码轮换、会议结束归档。

**后端依赖**：`/access/{overview|waitlist|contact-sales|activate}`、`/rooms`、`/rooms/current`、`/rooms/history`、`POST /rooms/{roomId}/join-code/rotate`、`POST /rooms/{roomId}/end`。

**API 契约样例**

```jsonc
// POST /api/v1/meet/rooms  请求
{ "title": "Sprint Review", "scheduledStartAt": "2026-05-20T09:00:00Z", "guestApproval": "required", "lobbyEnabled": true }
// 200 响应
{ "roomId": "rm_01H...", "joinCode": "K7Q4-ZJ", "hostUserId": "u_123",
  "joinCodeExpiresAt": "2026-05-20T10:00:00Z", "status": "scheduled" }
// POST /rooms/{roomId}/join-code/rotate  204 No Content
// 错误：40031 ROOM_LOCKED, 40331 NOT_HOST, 40431 ROOM_NOT_FOUND
```

**验收**：创建/查询/结束三条主路径；加入码轮换后旧码立即返回 40131 INVALID_JOIN_CODE。

#### 15.3.2 媒体控制与本地引擎

**估算**：4 PD

**目标**：在 `service/meet/rtc-engine.ts` 封装 `RTCPeerConnection`、设备选择、轨道开关、屏幕共享、虚拟背景。

**实现要点**

- 媒体状态以服务端回执为准：每次 mute/camera/share 调用 `POST /participants/{id}/media`，乐观 UI + 失败回滚
- 屏幕共享单独占独立 transceiver，避免与摄像头互相覆盖
- 虚拟背景使用 `MediaStreamTrackProcessor` + 离屏 Canvas，性能不达标时降级为静态背景

**API 契约样例**

```jsonc
// POST /api/v1/meet/rooms/{roomId}/participants/{participantId}/media
{ "audioEnabled": false, "videoEnabled": true, "screenSharing": false, "handRaised": false }
// 200 { "appliedAt": "2026-05-20T09:01:23Z" }
```

**验收**：mute/unmute/共享 三动作在 200ms 内可见；切换摄像头不打断音频。

#### 15.3.3 访客审批与公共入口

**估算**：3 PD

**目标**：未登录用户通过加入码进入候审 → 主持人审批 → 获得 `guestSessionToken` → 公开接口入会。

**后端依赖**：`/api/v1/public/meet/join/{joinCode}`、`POST /api/v1/public/meet/join/{joinCode}/requests`、`/api/v1/meet/rooms/{roomId}/guest-requests`、`approve|reject`、`/api/v1/public/meet/sessions/{guestSessionToken}/{heartbeat|media|leave}`。

**API 契约样例**

```jsonc
// POST /api/v1/public/meet/join/{joinCode}/requests
{ "displayName": "Alice", "deviceFingerprint": "fp_..." }
// 202 { "requestToken": "rq_...", "status": "pending" }
// 主持人 POST /api/v1/meet/rooms/{rid}/guest-requests/{rqId}/approve
// 200 { "guestSessionToken": "gs_...", "expiresAt": "..." }
// 拒绝 → 40331 GUEST_REJECTED, 客户端清理 requestToken
```

**前端**：访客侧轮询 `GET /api/v1/public/meet/requests/{requestToken}` 1 秒间隔；超过 5 分钟无响应自动放弃。

**验收**：访客 → 主持人审批通过 → 进入舞台；拒绝路径文案明确并清理本地 token。

#### 15.3.4 信令链路与质量监控

**估算**：4 PD

**目标**：搭建 SSE 下行 + REST 上行的信令通路，完成质量上报与主持人监控面板。

**后端依赖**：`POST /signals/{offer|answer|ice}`、`GET /signals` 与 `GET /signals/stream`（SSE）、`POST /participants/{id}/quality`、`GET /rooms/{roomId}/quality`。

**实现要点**

- SSE 连接断线 5 秒内不重连即视为掉线；重连时携带 `Last-Event-Id`
- 质量上报每 30 秒一次：`{rttMs, jitterMs, packetLossPct, codec, simulcastLayer}`
- 主持人 `quality` 面板使用 ECharts 6 实时折线（最多保留最近 5 分钟）

**API 契约样例**

```jsonc
// POST /api/v1/meet/rooms/{rid}/participants/{pid}/quality
{ "rttMs": 80, "jitterMs": 12, "packetLossPct": 0.4, "codec": "VP8", "simulcastLayer": "M" }
// SSE 事件：
// event: signal
// data: {"type":"offer","fromParticipantId":"p_123","sdp":"...","seq":42}
```

**验收**：弱网 200ms+ 时仍能维持音视频；主持人面板能在 1 秒内看到掉线参与者高亮。

**E2E（覆盖 15.3.1–15.3.4）**：主持人 + 1 访客 + 1 已登录用户的三方会议（创建 → 访客审批 → mute → 共享 → 主持人转移 → 结束）。

### 15.4 联系人独立模块（Contacts）

**优先级**：P0 / **估算**：5 PD

**现状**：联系人当前只在邮件撰写抽屉中作为收件人选择内嵌；`ContactController` + `ContactGroupController` 已实现完整 CRUD、收藏、快速添加、建议、CSV 导入/导出、重复检测与合并、分组成员管理，但前端无独立模块。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/contacts` | `views/contacts/index.vue` | 联系人主表（搜索、筛选、批量操作、收藏） |
| `/contacts/[id]` | `views/contacts/[id]/index.vue` | 联系人详情（基本信息 + 邮件历史 + 共享文档 + 通话） |
| `/contacts/groups` | `views/contacts/groups/index.vue` | 分组管理（树状） |
| `/contacts/groups/[groupId]` | `views/contacts/groups/[groupId]/index.vue` | 分组成员视图 |
| `/contacts/duplicates` | `views/contacts/duplicates/index.vue` | 重复检测与合并向导 |
| `/contacts/import-export` | `views/contacts/import-export/index.vue` | CSV 导入向导 + 导出 |

**通用 meta**：`{ requires: [], orgRequired: false, premiumOnly: false }`

**API 契约样例**

```jsonc
// POST /api/v1/contacts/import/csv multipart 请求
// form: file=@contacts.csv, mapping={"Name":"displayName","Email":"primaryEmail"}
// 202 { "jobId": "imp_01H...", "estimated": 1024 }
// GET /api/v1/contacts/duplicates → 200 { "groups": [{"key":"alice@x.com","contactIds":[...]}] }
// POST /api/v1/contacts/duplicates/merge { "primaryId":"c_1","mergeIds":["c_2","c_3"], "fieldChoices":{...} }
```

**后端依赖**（已就绪）：`/api/v1/contacts` 全部端点 + `/api/v1/contact-groups` 全部端点。

**UI/UX 要求**

- 主表：左侧分组树 + 右侧 NDataGrid（虚拟滚动，>1000 联系人）
- 收藏星标即时调用 `POST /{id}/favorite|unfavorite`，乐观更新失败回滚
- 重复检测向导分三步：扫描 → 比对 → 合并；合并使用并排表对比并允许字段级选择
- 导入向导：上传 CSV → 字段映射（拖拽匹配）→ 预览 → 提交，进度显示
- 详情页"邮件历史"通过 `/api/v1/mail?contactId=xxx` 查询，复用邮件列表组件

**验收标准**

- 联系人独立路由可访问且不依赖邮件模块
- 邮件撰写中的收件人选择改用统一 `useContactsStore`（移除内嵌副本）
- 导入 1k 行 CSV → 合并重复 → 导出能在 UI 内完整执行
- E2E：CRUD + 分组 + 合并 + 导入

### 15.5 SimpleLogin 集成

**优先级**：P2 / **估算**：3 PD / **门控**：`entitlement: SIMPLE_LOGIN`（组织级）

**现状**：后端 `SimpleLoginController` 提供概览、组织级中继策略 CRUD；前端无入口。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/integrations/simplelogin` | `views/integrations/simplelogin/index.vue` | 概览 + 别名总数 + 中继状态 |
| `/integrations/simplelogin/orgs/[orgId]` | `views/integrations/simplelogin/orgs/[orgId]/index.vue` | 组织级中继策略管理 |

**通用 meta**：`{ requires: ['SIMPLE_LOGIN'], orgRequired: true, premiumOnly: true, featureFlag: 'feat.simplelogin.enabled' }`

**后端依赖**：`GET /api/v1/simplelogin/overview`、`/orgs/{orgId}/relay-policies`（GET/POST/PUT/DELETE）。

**API 契约样例**

```jsonc
// POST /api/v1/simplelogin/orgs/{orgId}/relay-policies 请求
{ "matchDomain":"@brand.example.com","forwardTo":"alias-pool@simplelogin.io",
  "enabled":true,"note":"market" }
// 201 { "policyId":"slp_01H...","status":"ACTIVE","createdAt":"..." }
```

**UI/UX 要求**

- 概览展示：别名总数、活跃别名、本月接收数、中继命中率（NStatistic）
- 策略表单：发件域名 → 转发目标 → 启用状态 → 备注（NForm + NSwitch）
- 策略列表支持启停切换、按状态过滤；删除二次确认

**验收标准**

- 不持有 entitlement 时显示购买引导
- 策略 CRUD + 启停 4 条主路径可演示
- E2E：策略 CRUD

### 15.6 Standard Notes

**优先级**：P2 / **估算**：4 PD

**现状**：后端 `StandardNotesController` 提供笔记 + 文件夹 + 清单 + 导出接口；前端无入口。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/notes` | `views/notes/index.vue` | 笔记主表（左：文件夹树，中：笔记列表，右：编辑器） |
| `/notes/[noteId]` | `views/notes/[noteId]/index.vue` | 笔记详情/编辑（与主表共布局） |
| `/notes/folders` | `views/notes/folders/index.vue` | 文件夹管理（树形） |
| `/notes/export` | `views/notes/export/index.vue` | 导出向导（按文件夹、按时间） |

**通用 meta**：`{ requires: [], orgRequired: false, premiumOnly: false, featureFlag: 'feat.notes.enabled' }`

**后端依赖**：`/api/v1/standard-notes/{overview|folders|notes|export}`，含清单项 toggle：`POST /notes/{noteId}/checklist-items/{itemIndex}/toggle`。

**API 契约样例**

```jsonc
// POST /api/v1/standard-notes/notes 请求
{ "folderId":"fd_01H...","title":"待读清单","kind":"checklist",
  "items":[{"text":"Read Domain-Driven Design","done":false}] }
// 201 { "noteId":"nt_01H...","kind":"checklist","items":[...],"updatedAt":"..." }
// POST /notes/{noteId}/checklist-items/0/toggle → 200 { "done": true }
```

**UI/UX 要求**

- 三栏布局基于 `NLayout` + `NLayoutSider` + `NLayoutContent`（与邮件相同栅格，UnoCSS `grid grid-cols-[260px_1fr_2fr]`），编辑器复用 `MmRichEditor`（§9.2），纯文本 / Markdown 切换通过 `NRadioGroup`
- 清单项支持勾选 → 调用 toggle 接口；勾选状态以服务端为准
- 文件夹树支持拖动笔记跨文件夹（移动）
- 导出生成 zip 并触发浏览器下载

**验收标准**

- CRUD 笔记/文件夹 + 清单勾选 + 导出 4 条主路径
- 笔记移动文件夹后刷新页面状态保持一致
- E2E：创建笔记 → 添加清单 → 勾选 → 导出

### 15.7 TOTP / Authenticator 独立页

**优先级**：P1 / **估算**：4 PD

**现状**：后端 `AuthenticatorController` 已提供条目 CRUD、单条目验证码生成、QR 导入、CSV 导入/导出、加密备份、PIN 安全；前端只在登录 2FA 时使用，无独立管理页。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/security/authenticator` | `views/security/authenticator/index.vue` | 主页：条目网格 + 当前 6 位码 + 倒计时环 |
| `/security/authenticator/[entryId]` | `views/security/authenticator/[entryId]/index.vue` | 条目详情/编辑 |
| `/security/authenticator/import` | `views/security/authenticator/import/index.vue` | 导入向导（QR 图片 / 文本 / CSV） |
| `/security/authenticator/backup` | `views/security/authenticator/backup/index.vue` | 加密备份导入/导出 |
| `/security/authenticator/settings` | `views/security/authenticator/settings/index.vue` | PIN 与生物识别设置 |

**通用 meta**：`{ requires: [], orgRequired: false, premiumOnly: false }`

**API 契约样例**

```jsonc
// POST /api/v1/authenticator/entries/{id}/code 200
{ "code": "847291", "remainingSeconds": 23, "step": 30, "digits": 6 }
// PUT /api/v1/authenticator/security 请求 { "pinEnabled": true, "pin": "1234", "biometricsEnabled": true }
// 错误：40121 PIN_REQUIRED, 40321 PIN_INVALID, 40921 PIN_LOCKED_RETRY_AFTER
```

**后端依赖**：`/api/v1/authenticator/{entries|entries/{id}|entries/{id}/code|import|export|backup/export|backup/import|security|security/verify-pin|import/qr-image}`。

**UI/UX 要求**

- 主页采用 UnoCSS 响应式卡片网格（`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4`），每张卡片用 `NCard` + `NStatistic`：服务图标（`<svg-icon>`）、账户、6 位码（`<count-to>` 或等宽 `font-mono`）、30 秒倒计时圆环（`NProgress` circle）
- 倒计时归零后自动重新调用 `POST /entries/{id}/code` 刷新；不依赖前端 TOTP 计算（保持 token 在服务端）
- QR 导入：摄像头扫码（getUserMedia）或上传图片（`POST /import/qr-image`）
- PIN 设置后所有写操作前端拦截要求 PIN，调用 `POST /security/verify-pin`

**验收标准**

- 主网格刷新与倒计时完全同步，无 5 秒以上漂移
- 设置 PIN 后未输入 PIN 不能访问 `/security/authenticator/[entryId]`
- 导入向导支持 QR 图片 + Otpauth URI 文本 + CSV 三种来源
- E2E：CRUD + QR 导入 + 备份导出/导入

---

## 16. F-B：前端部分缺失功能需求

### 16.1 Suite 工作台真数据

**优先级**：P0 / **估算**：3 PD

**现状**：`frontend-v2/src/views/app/SuiteOverviewView.vue` 整页 178 行均为 mock 常量数组，无任何 API 调用。

**目标**：迁移到 `views/home/index.vue` 时直接绑定真实聚合接口；保留视觉骨架，仅替换数据源。

**后端依赖**

| 接口 | 用途 |
|---|---|
| `GET /api/v1/workspace/aggregation/overview`（`WorkspaceAggregationController`） | KPI 卡（未读邮件、今日事件、待办、最近文件） |
| `GET /api/v1/mail?folder=inbox&limit=5` | 最近邮件预览 |
| `GET /api/v1/calendar/events?range=today` | 今日日程 |
| `GET /api/v1/drive/files?recent=true&limit=10` | 最近文件 |
| `GET /api/v2/notifications?unread=true&limit=5` | 通知预览 |

**UI/UX 要求**

- 所有卡片支持骨架屏（NSkeleton），首屏 LCP < 1.5s
- 数据 5 分钟内复用 Pinia 缓存，下拉触发 force refresh
- 接口失败时单卡片降级显示错误态而非整页报错

**验收标准**

- 关闭 mock 后 KPI 数字与各模块详情页一致
- 网络错误时单卡片可独立重试

### 16.2 Business Overview 真数据

**优先级**：P1 / **估算**：3 PD

**现状**：`BusinessOverviewView.vue` 410 行，全部硬编码。

**后端依赖**：`OrgBusinessController` 现有 GET 端点（业务指标、KPI、组织树），具体路径以 backend 实现为准；如缺字段在本期补齐。

**目标**：迁移到 `views/home/business/index.vue`，把图表与表格替换为真实数据。

**UI/UX 要求**

- 图表使用 ECharts 6（与 §2 技术基线一致）
- 时间范围切换（7 / 30 / 90 天）由后端参数支持
- 部门切换时仅刷新对应卡片

**验收标准**

- 切换组织 / 时间范围 → 数据正确刷新
- 当前组织无 BUSINESS_INSIGHTS entitlement 时显示购买引导

### 16.3 邮件规则编辑 UI

**优先级**：P1 / **估算**：4 PD

**现状**：后端 `MailFilterController` 提供 `GET/POST/PUT/DELETE /api/v1/mail-filters` 与 `POST /preview`；前端无规则编辑界面。

**目标**：在 `views/mail/rules/` 下交付规则管理。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/mail/rules` | `views/mail/rules/index.vue` | 规则列表（启停切换、拖动排序） |
| `/mail/rules/[ruleId]` | `views/mail/rules/[ruleId]/index.vue` | 规则编辑器（条件 + 动作 + 实时预览） |
| `/mail/rules/new` | `views/mail/rules/new/index.vue` | 新建规则向导 |

**通用 meta**：`{ requires: [], orgRequired: false, premiumOnly: false }`

**API 契约样例**

```jsonc
// POST /api/v1/mail-filters 请求
{ "name": "归档促销", "enabled": true, "matchAll": false, "position": 1,
  "conditions": [
    { "field": "subject", "op": "contains", "value": "促销" },
    { "field": "from",    "op": "regex",    "value": ".*@newsletter\\.com$" }
  ],
  "actions": [
    { "type": "addLabel", "labelId": "lb_promo" },
    { "type": "moveToFolder", "folderId": "fd_archive" },
    { "type": "markRead" }
  ] }
// 201 { "filterId": "fl_01H...", ... }
// POST /api/v1/mail-filters/preview body: { "filterId": "fl_01H...", "limit": 50 }
// 200 { "matched": [{"messageId":"ml_..","subject":"...","from":"..."}, ...], "totalScanned": 1024 }
```

**UI/UX 要求**

- 条件构建器：字段（发件人 / 主题 / 收件人 / 大小 / 附件 / 标签）+ 操作符（contains / equals / regex / >）+ 值；支持多条件 AND/OR
- 动作列表：移动到文件夹、加标签、标记已读、转发、归档、删除、永久删除
- 预览面板：实时调用 `POST /preview`，显示命中前 50 封邮件
- 规则顺序通过拖拽并保存到后端 `position` 字段（B-B6 中也会用到 `position`）

**验收标准**

- 创建规则 → 预览命中 → 启用 → 收新邮件命中规则 → 邮件被自动处理
- 规则启停切换在 NDataTable 内即时生效
- E2E：CRUD + 预览 + 启停

### 16.4 邮件标签 / 文件夹拖拽

**优先级**：P0 / **估算**：3 PD

**现状**：标签与文件夹接口齐全（`/api/v1/labels`、`/api/v1/mail-folders`），但前端列表中无法拖拽邮件到侧边树。

**目标**：在邮件列表 + 侧边树之间打通 `vue-draggable-plus` 拖拽，实现：
- 邮件 → 文件夹：移动（调用 `PUT /api/v1/mail/{id}` 更新 folderId）
- 邮件 → 标签：添加标签（调用 `POST /api/v1/mail/{id}/labels`）
- 邮件多选 + 拖拽：批量操作

**API 契约样例**

```jsonc
// PUT /api/v1/mail/{messageId} 请求 { "folderId": "fd_archive" } → 200 { "messageId":"...","folderId":"fd_archive" }
// POST /api/v1/mail/{messageId}/labels 请求 { "labelIds": ["lb_promo","lb_q2"] } → 200 { "labelIds": [...] }
// 批量：POST /api/v1/mail/bulk { "ids":["ml_1","ml_2"], "op":"moveToFolder", "folderId":"fd_archive" }
```

**UI/UX 要求**

- 拖拽过程中显示半透明拖影 + 邮件数量徽章
- 接受目标在 hover 时高亮且显示提示文字
- 失败时整体回滚（乐观更新失败）并显示 Toast

**验收标准**

- 单封 + 多选拖拽到文件夹/标签均成功
- 跨域拖拽（邮件 → 收藏夹外的非法目标）显示禁止图标
- 移动设备上长按拖拽手势可用

### 16.5 Drive 文件版本历史 UI

**优先级**：P1 / **估算**：3 PD

**现状**：后端 `DriveController` 已提供 versions 接口；前端 `DriveExplorerView` 无版本入口。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/drive/files/[fileId]/versions` | `views/drive/files/[fileId]/versions/index.vue` | 版本时间线 + 元数据 |
| `/drive/files/[fileId]/versions/[verA]/compare/[verB]` | `views/drive/files/[fileId]/versions/[verA]/compare/[verB]/index.vue` | 对比视图（文本/图片/PDF） |

**通用 meta**：`{ requires: [], orgRequired: false, premiumOnly: false }`

**API 契约样例**

```jsonc
// GET /api/v1/drive/files/{fileId}/versions → 200
{ "versions": [
  { "versionId":"vr_01H...","number":7,"authorUserId":"u_123",
    "createdAt":"...","sizeBytes":102400,"sha256":"...",
    "changeSummary":"+12 -4 (paragraph 3)" }
] }
// POST /api/v1/drive/files/{fileId}/versions/{versionId}/restore → 202
{ "newVersionId":"vr_02H...","restoredFromVersionId":"vr_01H..." }
```

**UI/UX 要求**

- 版本时间线：NTimeline，每条目显示作者头像、时间、变更摘要、文件大小差
- 操作按钮：预览、下载、还原（确认）、对比
- 对比视图：文本走 **monaco-editor 双栏 diff**（已在 §6.4.4 白名单，懒加载）；图片左右滑动对比用 `NImage` + UnoCSS `flex` 布局；PDF 缩略图用 `NImage` + 后端缩略图接口并排
- 还原后调用 `POST /drive/files/{id}/versions/{versionId}/restore`，并跳回详情

**验收标准**

- 时间线展示完整版本历史并显示作者
- 文本版本对比显示行级 diff 高亮
- 还原后文件预览展示对应版本内容

### 16.6 Drive E2EE 加密分享

**优先级**：P1 / **估算**：5 PD

**现状**：后端 `PublicShareCapabilityController` 已暴露 E2EE share 接口（创建、续期、销毁、密钥派生）；前端未集成。

**目标**：在 Drive 文件详情/上下文菜单加入"加密分享"，本地浏览器侧使用现有 OpenPGP/WebCrypto 抽象（沿用 §10 加密策略）派生分享密钥，密钥不落服务端，仅链接含 fragment（`#k=base64`）。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/drive/files/[fileId]/share/secure` | `views/drive/files/[fileId]/share/secure/index.vue` | 创建/管理加密分享 |
| `/share/[token]` | `views/_public/share/[token]/index.vue`（blank-layout） | 公开访问入口（解密 fragment） |

**通用 meta**（私有）：`{ requires: [], orgRequired: false, premiumOnly: false }`；公开页 blank-layout，跳过 auth-guard。

**API 契约样例**

```jsonc
// POST /api/v1/drive/files/{fileId}/share/secure 请求
{ "expiresAt":"2026-06-01T00:00:00Z","maxDownloads":10,
  "passwordHashHex":"sha256:...","encryptedKeyMaterialBase64":"AQH...",
  "allowPreview": true }
// 201 { "shareToken":"sh_01H...","url":"https://app/share/sh_01H..." }
// GET /api/v1/public/share/{token} → 200 { "ciphertextBase64":"...","metadata":{...} }
// 错误：41021 SHARE_EXPIRED, 41031 DOWNLOAD_LIMIT_REACHED, 40121 PASSWORD_REQUIRED
```

**UI/UX 要求**

- 创建分享对话框：到期时间、最大下载次数、密码保护开关、是否允许预览
- 创建成功后展示完整链接（含 `#k=`），提供"复制链接"按钮，警示"链接片段不要发送给后端"
- 公开访问页：从 URL fragment 提取密钥 → 调用 `GET /api/v1/public/share/{token}` 拉取密文 → 浏览器侧解密 → 渲染
- 错误情形：链接过期、超过下载次数、密钥不匹配、密码错误，分别显示明确文案

**验收标准**

- 创建链接 → 在隐身模式打开 → 输入密码 → 成功下载/预览
- 服务器日志中不出现密钥明文
- 链接过期后访问展示明确过期文案

### 16.7 设置 - 自定义域名管理

**优先级**：P1 / **估算**：4 PD（后端 +5 PD，见 B-A4）

**现状**：分析中称"后端有 DomainController"，实际仓库中**当前不存在该控制器**；本期需先补齐后端（编号 B-A4，见 §17.4），前端在其上构建 UI。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/settings/domains` | `views/settings/domains/index.vue` | 域名列表（含验证状态） |
| `/settings/domains/[domainId]` | `views/settings/domains/[domainId]/index.vue` | 域名详情：DNS 检测、DKIM、SPF、DMARC、MX |
| `/settings/domains/new` | `views/settings/domains/new/index.vue` | 新增域名向导（4 步：输入 → DNS 配置 → 验证 → 完成） |

**通用 meta**：`{ requires: [], orgRequired: true, premiumOnly: false, adminOnly: true }`

**UI/UX 要求**

- DNS 检测面板：每条记录显示当前值、期望值、状态（绿/黄/红）、复制按钮、再次检测
- 新增向导每步引导清晰，提供示例配置（按 Cloudflare/Aliyun/Route53 模板）

**验收标准**

- 添加域名 → 配置 DNS → 验证通过 → 域名可用于发件别名（依赖 SimpleLogin 与 SMTP）
- 验证失败时给出明确文案和修复建议

### 16.8 设置 - Web Push 订阅管理

**优先级**：P1 / **估算**：3 PD（后端 +4 PD，见 B-A5）

**现状**：分析中称"后端有 WebPushController"，实际**未发现独立控制器**，仅有 `VapidWebPushDeliveryGateway` 服务与订阅 Mapper；本期需补齐后端订阅 CRUD（编号 B-A5，见 §17.5）。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/settings/notifications/web-push` | `views/settings/notifications/web-push/index.vue` | 订阅 + 设备列表 |

**通用 meta**：`{ requires: [], orgRequired: false, premiumOnly: false }`

**UI/UX 要求**

- 启用 Web Push 按钮触发浏览器 Notification 权限申请 → 注册 Service Worker → 调用 `POST /api/v1/web-push/subscriptions` 上报
- 设备列表显示 UA 摘要、注册时间、最后推送、删除按钮
- 关闭时本地取消订阅 + 调用后端删除

**验收标准**

- 启用后实际推送一条测试通知能在浏览器系统托盘出现
- 卸载/清理浏览器存储后能正确显示"已解除订阅"

### 16.9 Admin 计费 / 订阅管理

**优先级**：P1 / **估算**：5 PD

**现状**：后端 `SuiteBillingController`（pricing offers / billing overview / billing center / quote / checkout-draft / payment-methods / subscription-actions）已就绪；Admin 后台无对应 tab。

**路由与页面**

| 路由 | 文件 | 说明 |
|---|---|---|
| `/admin/billing` | `views/admin/billing/index.vue` | 总览：MRR / ARR / 订阅数 / 续费率 |
| `/admin/billing/subscriptions` | `views/admin/billing/subscriptions/index.vue` | 组织订阅列表（升降级、退款、暂停） |
| `/admin/billing/invoices` | `views/admin/billing/invoices/index.vue` | 发票列表（导出 PDF、补开） |
| `/admin/billing/payment-methods` | `views/admin/billing/payment-methods/index.vue` | 支付方式审计与默认设置 |
| `/admin/billing/offers` | `views/admin/billing/offers/index.vue` | 套餐与定价（offers） |

**通用 meta**：`{ requires: [], orgRequired: false, premiumOnly: false, adminOnly: true, role: 'BILLING_ADMIN' }`

**API 契约样例**

```jsonc
// GET /api/v1/suite/billing/center?orgId=org_1 → 200
{ "currentPlan":"BUSINESS_YEARLY","seats":50,"usedSeats":42,
  "renewAt":"2027-01-01T00:00:00Z","mrrCents":120000,"arrCents":1440000,
  "renewalRate30d":0.94 }
// POST /api/v1/suite/billing/subscription-actions 请求
{ "orgId":"org_1","action":"PAUSE","effectiveAt":"2026-06-01T00:00:00Z","reason":"customer-request" }
// 200 { "actionId":"act_...","status":"queued" }
```

**UI/UX 要求**

- 总览图表：MRR 30 天趋势（ECharts line）+ 续费率（gauge）
- 订阅操作（升降级、暂停、恢复）使用 NDrawer 二次确认；调用 `POST /billing/subscription-actions`
- 发票补开：选择周期 → 调用 `POST /billing/quote` → 预览金额 → 创建草稿

**验收标准**

- 切换组织订阅 + 查看发票 + 补开草稿 3 条主路径可演示
- 普通管理员无 BILLING_ADMIN 角色访问 `/admin/billing` 落到 403

---

## 17. B-A：后端完全缺失模块需求

### 17.1 社区 Community 模块 —— 总览

**优先级**：P2 / **总估算**：14 PD（后端 10 + 前端 4） / **门控**：`featureFlag: feat.community.enabled`

**现状**：前端 `routes.ts` 已有社区路由占位；后端无 `CommunityController`，亦无相关表结构。

**子项拆分**

| 编号 | 子项 | 估算 | 优先级 |
|---|---|---|---|
| 17.1.1 | 帖子 CRUD + 列表 | 3 PD | P2 |
| 17.1.2 | 评论树 + 1 级回复 | 2 PD | P2 |
| 17.1.3 | 话题 / 标签管理 | 2 PD | P2 |
| 17.1.4 | 点赞 / 收藏 / 浏览计数 | 2 PD | P2 |
| 17.1.5 | 举报 / 审核 / 后台管理 | 3 PD | P2 |
| 17.1.6 | 前端微调（路由占位 → 真实页面） | 2 PD | P2 |

**通用 meta**：`{ requires: [], orgRequired: false, premiumOnly: false, featureFlag: 'feat.community.enabled' }`

#### 17.1.1 帖子 CRUD + 列表

**后端契约**

```
GET    /api/v1/community/posts?topicId=&q=&page=&size=&sort=
POST   /api/v1/community/posts
GET    /api/v1/community/posts/{postId}
PATCH  /api/v1/community/posts/{postId}
DELETE /api/v1/community/posts/{postId}      软删 status=deleted，30 天后清理
POST   /api/v1/community/posts/{postId}/pin  置顶（管理员）
POST   /api/v1/community/posts/{postId}/lock 锁帖（管理员）
```

**API 契约样例**

```jsonc
// POST /api/v1/community/posts 请求
{ "topicId": "tp_general", "title": "欢迎使用 MMMail v2.1.2",
  "bodyMd": "# 介绍\n本帖讨论...", "tags": ["release","feedback"] }
// 201 响应
{ "postId": "ps_01H...", "authorUserId": "u_123", "status": "published",
  "bodyHtml": "<h1>介绍</h1>...", "likeCount": 0, "commentCount": 0,
  "viewCount": 0, "pinned": false, "locked": false,
  "createdAt": "2026-05-15T10:00:00Z" }
// 错误：40021 TITLE_REQUIRED, 40321 NOT_AUTHOR, 40921 POST_LOCKED
```

**数据表**

```
community_post(id, author_user_id, org_id, topic_id, title, body_md, body_html,
               like_count, comment_count, view_count, pinned, locked,
               status enum[draft|published|hidden|deleted], created_at, updated_at,
               deleted_at)
community_post_tag(post_id, tag, PRIMARY KEY(post_id, tag))
```

**验收**：作者可创建/编辑/软删自己帖子；列表分页 + 排序（hot / latest）；超管视角跨组织。

#### 17.1.2 评论树 + 1 级回复

**后端契约**

```
GET    /api/v1/community/posts/{postId}/comments?cursor=&size=
POST   /api/v1/community/posts/{postId}/comments  body: {bodyMd, parentCommentId?}
DELETE /api/v1/community/comments/{commentId}     级联软删子评论
```

**API 契约样例**

```jsonc
// POST 请求 { "bodyMd": "同意 +1", "parentCommentId": "cm_01H..." }
// 201 { "commentId": "cm_02...", "parentCommentId": "cm_01H...",
//       "authorUserId": "u_456", "bodyHtml": "<p>同意 +1</p>",
//       "createdAt": "...", "status": "published" }
```

**数据表**

```
community_comment(id, post_id, parent_comment_id, author_user_id, body_md,
                  body_html, status, created_at, deleted_at)
```

**实现要点**

- 限制嵌套 1 级（parent 必须是 post 直属评论）
- 删除评论级联 `UPDATE ... SET status='deleted'` 子评论
- 列表使用 cursor 分页（`createdAt` + `id`）

**验收**：1 级回复正确显示；删除父评论后子评论标记为"已删除（原作者）"占位。

#### 17.1.3 话题 / 标签

**后端契约**

```
GET    /api/v1/community/topics                 公开
POST   /api/v1/community/topics                 管理员
PATCH  /api/v1/community/topics/{topicId}       管理员
DELETE /api/v1/community/topics/{topicId}       管理员（必须先迁移帖子）
GET    /api/v1/community/tags?limit=20          热门标签
```

**数据表**

```
community_topic(id, slug UNIQUE, title, description, sort, created_at)
```

**验收**：话题/标签筛选生效；删除话题时校验"无活跃帖子"。

#### 17.1.4 点赞 / 收藏 / 浏览计数

**后端契约**

```
POST /api/v1/community/posts/{postId}/like      toggle，返回新计数
POST /api/v1/community/posts/{postId}/bookmark  toggle
POST /api/v1/community/posts/{postId}/view      埋点（限频 1 次/用户/小时）
GET  /api/v1/community/me/bookmarks?page=&size=
```

**API 契约样例**

```jsonc
// POST /like 200
{ "liked": true, "likeCount": 12 }
```

**数据表**

```
community_post_like(post_id, user_id, created_at, PRIMARY KEY(post_id, user_id))
community_post_bookmark(post_id, user_id, created_at, PRIMARY KEY(post_id, user_id))
```

**实现要点**

- 计数采用增量字段 + 异步对账（每 5 分钟从明细表回填，避免热点 row）
- 浏览埋点写 Redis HLL 去重，每小时 flush 到 `view_count`

**验收**：高并发场景计数无负数；用户取消点赞后 like_count 减 1。

#### 17.1.5 举报 / 审核 / 后台

**后端契约**

```
POST   /api/v1/community/reports                       body: {targetType:'post'|'comment', targetId, reason, detail}
GET    /api/v1/community/admin/reports?status=&page=   管理员
PATCH  /api/v1/community/admin/reports/{reportId}      管理员，body: {action: 'dismiss'|'hide'|'delete'|'ban_user'}
```

**API 契约样例**

```jsonc
// POST /reports 请求
{ "targetType": "post", "targetId": "ps_01H...", "reason": "spam", "detail": "广告" }
// 202 { "reportId": "rp_...", "status": "pending" }
// 管理员处理 PATCH /admin/reports/rp_... { "action": "hide" } → 200 { "status": "actioned" }
```

**数据表**

```
community_report(id, target_type, target_id, reporter_user_id, reason, detail,
                 status enum[pending|actioned|dismissed], assignee_user_id,
                 action enum[dismiss|hide|delete|ban_user], action_note,
                 created_at, actioned_at)
```

**审计**：所有 `action` 写入 `audit_log`，类型 `community.report.actioned`。

**验收**：举报后 30 秒内出现在管理员队列；处理后举报人收到通知（§18.7 通道）。

#### 17.1.6 前端微调

**估算**：2 PD

**目标**：将路由占位 `views/community/index.vue` 升级为真实列表页 + 详情页 + 创建/编辑富文本（沿用 §9.2 的 `MmRichEditor` 业务组件）。

**安全**

- 富文本服务端 sanitize（jsoup safelist：标题、段落、列表、链接、图片、代码块）
- 客户端 DOMPurify 二次保护
- 链接 rel="noopener noreferrer" 强制

**验收**：CRUD + 评论 + 点赞 + 举报全流程在浏览器内闭环。

### 17.2 全局搜索 Search —— 总览

**优先级**：P1 / **总估算**：11 PD（后端 8 + 前端 3） / **门控**：无

**现状**：仅有 `SearchHistoryController` + `SearchPresetController`，无跨模块全文检索。

**子项拆分**

| 编号 | 子项 | 估算 | 优先级 |
|---|---|---|---|
| 17.2.1 | 索引视图 + 写路径（事件 outbox） | 4 PD | P1 |
| 17.2.2 | 读路径 + API + 权限二次过滤 | 3 PD | P1 |
| 17.2.3 | 索引重建（异步任务 + 进度） | 1 PD | P1 |
| 17.2.4 | 前端联动（CommandPalette + ShellTopBar） | 3 PD | P1 |

#### 17.2.1 索引视图 + 写路径

**索引引擎**：PostgreSQL `tsvector` + `pg_trgm`（沿用现 schema），不引入 ES。

**索引表设计**

```
search_index(
  id BIGSERIAL PK,
  module_type text CHECK (module_type IN ('mail','doc','sheet','drive','contact','note','community')),
  resource_id text,
  org_id text,
  owner_user_id text,
  acl_user_ids text[],          -- 显式可见用户集合（≤200）
  title text,
  body text,
  body_tsv tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('simple', coalesce(title,'')), 'A') ||
    setweight(to_tsvector('simple', coalesce(body,'')),  'B')
  ) STORED,
  updated_at timestamptz,
  UNIQUE(module_type, resource_id)
);
CREATE INDEX search_index_tsv_gin ON search_index USING GIN(body_tsv);
CREATE INDEX search_index_trgm ON search_index USING GIN(title gin_trgm_ops);
```

**写路径**

- 各模块服务层在事务提交后写一条 `domain event`（沿用 v2.1 已上线的事件 outbox）
- `SearchIndexConsumer` 异步消费 → `INSERT ... ON CONFLICT (module_type, resource_id) DO UPDATE`
- 删除事件触发 `DELETE`
- 失败重试 3 次，进死信队列后告警

**验收**：邮件 / 文档 / 表格 / 文件 / 联系人 / 笔记 / 帖子 创建后 5 秒内可被搜索到。

#### 17.2.2 读路径 + API + 权限过滤

**API 契约**

```
GET  /api/v1/search?q=&types=&page=&size=&from=&to=&orgId=
GET  /api/v1/search/suggestions?q=
GET  /api/v1/search/facets?q=
```

**API 契约样例**

```jsonc
// GET /api/v1/search?q=合同&types=mail,doc&size=10  → 200
{
  "total": 42,
  "items": [
    { "moduleType": "mail", "resourceId": "ml_01H...", "title": "Re: 合同审阅",
      "snippet": "...这份<em>合同</em>需要在...", "score": 12.4,
      "updatedAt": "2026-05-10T08:00:00Z",
      "navigation": { "kind": "navigate", "path": "/mail/inbox/ml_01H..." } }
  ],
  "facets": { "byType": { "mail": 30, "doc": 12 } }
}
// 错误：40021 QUERY_TOO_SHORT (<2 字符), 42921 RATE_LIMITED
```

**权限二次过滤**

- 读路径在 SQL 之外**再用业务可见性规则过滤**（`acl_user_ids` 仅是预选过滤器）
- 联合查询限制：单查询最多扫描 10000 行，超出需附加 type 限定

**评分**

- `ts_rank_cd(body_tsv, query) * 5`（标题）+ `ts_rank_cd * 1`（正文）+ `0.1 / max(days_old, 1)`（最近性加权）
- `pinned/featured` 内容 +2 加分

**验收**：6 类内容均能命中且不返回越权结果；响应 P50 < 150ms（10k 文档规模）。

#### 17.2.3 索引重建

**API 契约**

```
POST /api/v1/search/reindex/{moduleType}    超管，body: {orgId?, since?}
GET  /api/v1/search/reindex/{jobId}         查询进度
```

**实现要点**

- 异步任务（沿用现有后台作业框架，参考 `backend-v21-background-job-foundation-design`）
- 进度写 Redis：`{processed, total, eta, errors}`
- 重建过程不阻塞读：写入 `search_index_staging` 表，完成后 `BEGIN; TRUNCATE; INSERT FROM staging; COMMIT;`

**验收**：1 万条记录的重建在 5 分钟内完成；进度查询每秒可调用。

#### 17.2.4 前端联动

**改动范围**

- `frontend-v2/src/design-system/components/CommandPalette.vue` 替换内置假数据为 `/search/suggestions`
- `frontend-v2/src/layouts/modules/ShellTopBar.vue` 搜索框加入 debounced（200ms）调用
- 新建 `views/search/index.vue`（路由 `/search?q=`），完整结果页 + 分面侧栏

**UI/UX 要求**

- 类型分组：邮件 / 文档 / 表格 / 文件 / 联系人 / 笔记 / 社区，每组最多显示 5 条
- 高亮关键词使用后端返回的 `<em>` 片段（已 sanitize），客户端再过 DOMPurify
- 键盘导航：↑↓ 选择，Enter 跳转，Esc 关闭

**验收**：从 ShellTopBar 输入 → 200ms 内显示建议；点击结果跳转到对应模块。

### 17.3 命令面板（Cmd-K）后端契约

**优先级**：P1 / **估算**：4 PD（后端扩展） + 2 PD（前端）

**现状**：`V21OpsController` 已提供 `/api/v2/command-center/{commands|runs|workflows|audit}`，但缺少前端命令面板需要的"动作目录注册 / 上下文敏感动作过滤 / 跨模块跳转 payload"。

**目标**：在现有 `command-center` 基础上扩展契约，使前端能在 Cmd-K 中根据当前路由显示场景化命令。

**后端契约扩展**

```
GET  /api/v2/command-center/catalog?context=routePath  返回：
     [{ id, title, i18nKey, group, icon, shortcut?, action: {kind:'navigate'|'invoke'|'wizard', payload}, requires?: entitlement[] }]
POST /api/v2/command-center/runs                      （已有，扩展 payload schema）
GET  /api/v2/command-center/recents?limit=20          用户近用命令
POST /api/v2/command-center/pin                       置顶 / 取消
GET  /api/v2/command-center/quick-search?q=           跨模块速查（邮件 / 联系人 / 文件 / 命令）
```

**实现要点**

- 命令注册放在 `command_catalog` 表 + 代码内 `CommandRegistry` 双源；启动时 reconcile
- 上下文匹配规则按路由前缀 + entitlements + role 三层过滤
- `quick-search` 复用 §17.2 全局搜索，限定 limit=5 + 高优先模块

**验收标准**

- 在 `/mail/inbox` 触发 Cmd-K 显示"撰写新邮件 / 切换文件夹 / 打开规则"等上下文命令
- 近用命令在多设备一致（需 SSO 同步至同一 user）
- 没有 entitlement 的命令在面板中**不出现**而非置灰

### 17.4 自定义域名 Domain（前端依赖项）

**优先级**：P1 / **估算**：5 PD

**现状**：仓库无 `DomainController`，但邮件别名与中继依赖域名验证。

**后端契约**（新增 `DomainController`，前缀 `/api/v1/domains`）

```
GET    /                              当前组织域名列表
POST   /                              新增域名（生成 DNS 记录与验证 token）
GET    /{domainId}
DELETE /{domainId}
POST   /{domainId}/verify             触发 DNS 解析检测
GET    /{domainId}/dns-records        期望 DNS 记录（DKIM、SPF、DMARC、MX、CNAME）
GET    /{domainId}/diagnostics        当前实际 DNS + 比对结果
POST   /{domainId}/aliases            把域名挂到别名/收件入口
```

**实现要点**

- DNS 解析使用 dnsjava；缓存 5 分钟
- DKIM 选择 2048 位，密钥落安全 KMS
- 域名归属于 org，需要 ORG_ADMIN 角色

**API 契约样例**

```jsonc
// POST /api/v1/domains 请求 { "domain": "mail.example.com" }
// 201 { "domainId":"dm_01H...","status":"PENDING_VERIFICATION",
//      "verificationToken":"mmmail-verify=ab12cd","expectedAt":"..." }
// GET  /api/v1/domains/{id}/dns-records → 200
// { "records": [
//   { "type":"TXT","host":"_mmmail-verify","expected":"mmmail-verify=ab12cd" },
//   { "type":"TXT","host":"@","expected":"v=spf1 include:_spf.mmmail.com ~all" },
//   { "type":"TXT","host":"_dmarc","expected":"v=DMARC1; p=quarantine; rua=mailto:dmarc@..." },
//   { "type":"CNAME","host":"mm._domainkey","expected":"mm.dkim.mmmail.com" },
//   { "type":"MX","host":"@","expected":"10 inbound.mmmail.com" }
// ] }
// POST /api/v1/domains/{id}/verify → 200 { "status":"ACTIVE" } 或 422 { "code":42221, "missing":["DKIM"] }
```

**验收标准**

- 添加域名 → 拉取期望 DNS → 用户配置后 verify 通过 → 状态变为 `ACTIVE`
- 删除域名前要求所有挂载的别名先迁移

### 17.5 Web Push 订阅 API（前端依赖项）

**优先级**：P1 / **估算**：4 PD

**现状**：后端有 `VapidWebPushDeliveryGateway` + 订阅 Mapper，但无独立 REST 控制器；现有订阅写入散落在通知发送链路中。

**后端契约**（新增 `WebPushController`，前缀 `/api/v1/web-push`）

```
GET    /vapid-public-key             返回 VAPID 公钥（base64url）
GET    /subscriptions                当前用户订阅列表
POST   /subscriptions                注册订阅 (endpoint, keys.p256dh, keys.auth, ua, label)
DELETE /subscriptions/{subscriptionId}
POST   /test                         发送测试推送到指定订阅或全部
```

**实现要点**

- 沿用现有 `VapidWebPushDeliveryGateway` 完成下行
- 订阅记录字段：endpoint、p256dh、auth、ua、label、orgId、userId、lastSuccessAt、lastFailureAt、failureCount
- 失败 5 次自动失活；登录时清理已失活订阅

**API 契约样例**

```jsonc
// GET /api/v1/web-push/vapid-public-key → 200 { "publicKey":"BNJxw..."}
// POST /api/v1/web-push/subscriptions 请求
{ "endpoint":"https://fcm.googleapis.com/fcm/send/eN...",
  "keys":{ "p256dh":"BNc...","auth":"a1b2c3..." },
  "ua":"Mozilla/5.0 ...","label":"工作笔记本" }
// 201 { "subscriptionId":"ws_01H...","registeredAt":"..." }
// POST /api/v1/web-push/test → 202 { "deliveryId":"wd_..." }
```

**验收标准**

- 浏览器注册 → 收到测试推送 → 删除订阅 → 不再收到推送
- 失败计数与失活策略生效

---

## 18. B-B：后端部分缺失功能需求

### 18.1 日历订阅（CalDAV）

**优先级**：P2 / **估算**：8 PD

**目标**：支持外部 ICS/CalDAV 订阅与导出。

**后端契约扩展**（在 `V21CalendarController` 上新增）

```
POST /api/v2/calendar/subscriptions               { url, authMode, label, color }
GET  /api/v2/calendar/subscriptions
DELETE /api/v2/calendar/subscriptions/{id}
POST /api/v2/calendar/subscriptions/{id}/sync     强制拉取
GET  /api/v2/calendar/{calendarId}/ics            导出 ICS（含 ETag）
```

**实现要点**

- ICS 解析使用 ical4j；保留原始 UID 以支持增量更新
- 同步任务通过现有 background-job foundation；默认 15 分钟拉取
- CalDAV：按需在 v2.1.3 启用 PROPFIND 等 WebDAV 谓词；本期至少支持订阅 URL（HTTP ICS）

**API 契约样例**

```jsonc
// POST /api/v2/calendar/subscriptions 请求
{ "url":"https://calendar.google.com/calendar/ical/.../basic.ics",
  "authMode":"NONE","label":"Google 工作","color":"#1a73e8" }
// 201 { "subscriptionId":"cs_01H...","syncStatus":"PENDING","nextSyncAt":"..." }
// POST /subscriptions/{id}/sync → 202 { "jobId":"sj_..." }
```

**验收标准**

- 添加 Google/Outlook 公开 ICS URL → 事件出现在日历视图 → 一致性 < 15 分钟

### 18.2 重复事件（RRULE）

**优先级**：P1 / **估算**：6 PD

**目标**：在事件创建/编辑时支持 iCalendar RRULE。

**后端契约扩展**

```
POST /api/v2/calendar/events                     扩展 body：rrule, rdate, exdate
PATCH /api/v2/calendar/events/{id}               扩展支持："this | thisAndFollowing | all" 的 scope
GET  /api/v2/calendar/events?from=&to=           按时间窗展开重复实例（服务端展开，最多 366 天）
```

**实现要点**

- 使用 ical4j RRULE 引擎；每次查询展开生成虚拟实例（不写库）
- 例外处理：编辑单实例时生成 EXDATE + 单独事件
- DST / 时区：以事件原时区存储，渲染时按用户时区转换

**API 契约样例**

```jsonc
// POST /api/v2/calendar/events 请求
{ "title":"Sprint Standup","startAt":"2026-05-18T09:00:00+08:00",
  "endAt":"2026-05-18T09:30:00+08:00","timezone":"Asia/Shanghai",
  "rrule":"FREQ=WEEKLY;BYDAY=MO;UNTIL=20261231T000000Z" }
// 201 { "eventId":"ev_01H...","seriesId":"se_01H..." }
// PATCH /api/v2/calendar/events/{eventId}?scope=thisAndFollowing
// { "endAt":"2026-05-18T10:00:00+08:00" }
// 200 { "newSeriesId":"se_02H...","truncatedSeriesId":"se_01H...","truncatedAt":"..." }
```

**验收标准**

- 创建每周一 9:00 直到年底的会议 → 列表正确展开 52 个实例
- 修改"本次及之后"截断旧规则并创建新规则
- 删除单实例只新增 EXDATE

### 18.3 邮件 IMAP/SMTP 外部账号

**优先级**：P2 / **估算**：12 PD

**目标**：用户可接入 Gmail / Outlook / 自建 IMAP 邮箱，统一在 MMMail 邮件视图中查看与回复。

**后端契约**（新增 `MailExternalAccountController`，前缀 `/api/v1/mail/external-accounts`）

```
GET    /                                  外部账号列表
POST   /                                  添加（imap/smtp 主机、端口、用户名、密码或 OAuth token）
GET    /{accountId}
PATCH  /{accountId}
DELETE /{accountId}
POST   /{accountId}/test                  测试连通性
POST   /{accountId}/sync                  立即同步
```

**实现要点**

- 使用 jakarta.mail；密码加密存储（KMS 信封加密）
- 同步 worker 通过 background-job 调度，按账号增量拉取（UID 高水位）
- OAuth：Gmail 使用 XOAUTH2；后期支持 Microsoft

**安全**

- 密码字段不允许明文返回；前端只能写
- 拒绝同步明显异常的服务器（rate-limited 时退避）

**API 契约样例**

```jsonc
// POST /api/v1/mail/external-accounts 请求
{ "provider":"GMAIL","authMode":"OAUTH2",
  "email":"alice@gmail.com","oauthRefreshToken":"...",
  "imap":{"host":"imap.gmail.com","port":993,"ssl":true},
  "smtp":{"host":"smtp.gmail.com","port":587,"starttls":true} }
// 201 { "accountId":"ext_01H...","syncStatus":"INITIAL_SYNC" }
// POST /{accountId}/test → 200 { "imapOk": true, "smtpOk": true, "latencyMs": 120 }
// 错误：40121 OAUTH_INVALID, 50421 IMAP_TIMEOUT, 50521 RATE_LIMITED
```

**验收标准**

- 添加 Gmail（应用专用密码）→ 同步收件箱前 100 封 → 在 MMMail 中读取并回复 → 对方收件箱看到正确发件人

### 18.4 文档实时协同（CRDT/WS）—— 总览

**优先级**：P2（v2.1.2 内仅交付 18.4.1 的基础设施 + 18.4.2 的 Docs 接入；18.4.3 滚动至 v2.1.3）

**总估算**：22 PD（拆分为 18.4.1–18.4.3）

**现状**：当前文档保存为单写入；`SheetsController` 同；`V21OpsController` 看板不支持并发写。

**子项拆分**

| 编号 | 子项 | 估算 | 优先级 |
|---|---|---|---|
| 18.4.1 | CRDT 基础设施 + WS 网关接入 + 持久化 snapshot | 10 PD | P1 |
| 18.4.2 | Docs（Tiptap + y-prosemirror）接入 | 6 PD | P1 |
| 18.4.3 | Sheets / 看板接入（独立 sub-spec） | 6 PD | P2 |

> 注：18.4.3 的工作量与风险较高，建议在本期仅保留**占位 sub-spec**：`docs/superpowers/specs/2026-05-15-collab-sheets-board-design.md`，落地排到 v2.1.3。

#### 18.4.1 CRDT 基础设施

**选型**

- CRDT 引擎：**Yjs**（浏览器侧）+ y-protocols（v1）
- 服务端：使用 [yrs](https://github.com/y-crdt/y-crdt) Java JNI 绑定，作为 `mmmail-collab` 子模块加载到 Spring Boot 应用进程；不引入独立 Node 微服务以减少运维面
- 备选：若 yrs 性能不达标，降级方案是后端仅作"消息中转"（不解析 update），快照由前端定期回写

**WS 网关接入**

- 共用 §20 的 WebSocket 网关，channel 命名 `collab/docs/{docId}`、`collab/sheets/{sheetId}`、`collab/board/{boardId}`
- 鉴权：连接时通过 `token` 查 ACL；不通过则关闭码 4401

**持久化**

- 每 30 秒或每 200 次 update 落库一次 snapshot
- `collab_snapshot(resource_type, resource_id, version, snapshot bytea, created_at)`
- update 流落 `collab_update(resource_type, resource_id, seq, update bytea, created_at)`，每天清理 7 天前的 update（snapshot 之后）
- 崩溃恢复：从最近 snapshot + WAL 重放

**API 契约**

```
WS   /ws/collab/{resourceType}/{resourceId}?token=...      y-protocols v1
GET  /api/v1/collab/{resourceType}/{resourceId}/snapshot   最新快照（base64）
POST /api/v1/collab/{resourceType}/{resourceId}/snapshot   客户端定期回写
GET  /api/v1/collab/{resourceType}/{resourceId}/awareness  活跃用户（光标 / 选择）
```

**API 契约样例**

```jsonc
// GET /api/v1/collab/docs/dc_01H.../snapshot → 200
{ "version": 142, "snapshotBase64": "AQH...==", "updatedAt": "..." }
// WS 上行 binary（y-protocols sync step 2 / awareness）
```

**验收**：基础设施联调（2 个 tab 编辑同一资源，1 秒内互见 update）；崩溃后从 snapshot 恢复成功率 100%。

#### 18.4.2 Docs 接入

**前端**

- 编辑器：Tiptap + y-prosemirror，连接 `WS /ws/collab/docs/{docId}`
- 离线策略：网络断开时进入"本地草稿"模式，使用 IndexedDB 暂存 update；重连后通过 y-protocols 自动合并

**冲突示例**

- 同段同位置插入：CRDT 自然合并，结果按客户端 clientId 排序
- 一方删一方改：CRDT 保留改动文字（行业惯例），不强制覆盖

**性能要求**

- 1k 字文档协同延迟 P50 < 200ms
- 10 人同时编辑场景下，单连接 update 频率不超过 30 op/s（前端节流）

**验收**：2 个 tab 同时编辑同一文档，1 秒内互见光标与字符；强制断网 30 秒后重连可无冲突合并。

#### 18.4.3 Sheets / 看板接入（占位）

**估算**：6 PD（v2.1.3）

**约束**

- Sheets 公式（§18.5）的服务端执行需要在 CRDT 写入后触发，避免循环
- 看板拖拽（§18.6）的 position 字段需要在 CRDT 与传统持久化之间**单源**：建议 position 不进 CRDT，仍走 §18.6 REST

**待解决问题**：放入 §28 风险与未决。

### 18.5 表格服务端公式

**优先级**：P2 / **估算**：10 PD

**目标**：在 `SheetsController` 增加服务端公式求值，支持基础 SUM/AVG/IF/VLOOKUP 等 60 个函数。

**后端契约扩展**

```
POST /api/v1/sheets/{sheetId}/cells/evaluate     入参：单元格 + 公式 + 依赖图
GET  /api/v1/sheets/{sheetId}/dependency-graph    返回拓扑
POST /api/v1/sheets/{sheetId}/recalculate         强制重算
```

**实现要点**

- 使用 Apache POI 或自研 ANTLR 公式解析器；MVP 用 [EvalEx](https://github.com/ezylang/EvalEx) 起步
- 依赖图增量更新：每个单元格写入时维护正向 & 反向依赖
- 防爆：单次计算最大 100k 单元格 + 5 秒超时

**API 契约样例**

```jsonc
// POST /api/v1/sheets/{sheetId}/cells/evaluate 请求
{ "cells": [{ "ref":"B1", "formula":"=SUM(A1:A100)" }] }
// 200 { "results": [{ "ref":"B1", "value":5050, "type":"NUMBER", "dependsOn":["A1:A100"] }] }
// 错误：42221 CIRCULAR_REF { "cycle":["B1","B2","B1"] }
```

**验收标准**

- 输入 `=SUM(A1:A100)` → 服务端返回正确值
- 修改 A1 时 B1 公式自动重算并广播（结合 §18.4 协同通道）
- 循环引用返回明确错误而非阻塞

### 18.6 看板拖拽排序持久化

**优先级**：P1 / **估算**：3 PD

**现状**：`V21OpsController` 协作任务/项目接口缺 `position`/`order` 字段。

**后端契约扩展**

```
PATCH /api/v2/collaboration/tasks/{id}/move
      { columnId, position }            原子更新所属列与位置
GET   /api/v2/collaboration/projects/{id}/board
      返回包含每张卡 position 的看板
```

**实现要点**

- 使用 fractional indexing（如 `lexorank`）避免大量重排
- 并发：同列同位置由后端去重；返回最新 position 给所有客户端

**API 契约样例**

```jsonc
// PATCH /api/v2/collaboration/tasks/{id}/move 请求
{ "columnId":"col_doing", "afterTaskId":"tsk_03","beforeTaskId":"tsk_07" }
// 服务端解析为 fractional position（lexorank "0|hzzzzz:")
// 200 { "taskId":"tsk_05","columnId":"col_doing","position":"0|hzzzzz:" }
```

**验收标准**

- 拖拽卡片到新列新位置后刷新页面位置不变
- 两个客户端同时拖到同一位置时按服务器最终顺序一致

### 18.7 通知 WebSocket 实时推送

**优先级**：P0 / **估算**：5 PD

**现状**：前端通过轮询拉 `/api/v2/notifications`；无长连接。

**后端契约**

```
WS  /ws/notifications?token=...
    下行帧 type: 'notification' | 'badge-update' | 'subscription-changed'
GET /api/v2/notifications/since?cursor=...     断线重连补发
```

**实现要点**

- 复用与文档协同相同的 WS 网关（同 host 不同 path）
- 服务器侧通过事件总线（已有 outbox foundation）扇出至 hub
- 心跳：客户端 30 秒 ping，服务端 60 秒未收到关闭

**API 契约样例**

```jsonc
// WS /ws/notifications?token=... 下行 JSON
{ "type":"notification", "channel":"user/u_123", "seq":4521,
  "payload":{ "id":"nt_01H...","title":"新邮件","body":"来自 Alice 的邮件",
              "category":"mail.received","actionUrl":"/mail/inbox/ml_..." } }
// GET /api/v2/notifications/since?cursor=4500 → 200 { "events":[...], "nextCursor":4521 }
```

**验收标准**

- 后端发出通知 → 前端不刷新页面 1 秒内出现
- 断线重连后历史通知不丢失（通过 cursor 补发）

### 18.8 登录异常检测

**优先级**：P1 / **估算**：6 PD

**目标**：检测异地登录、暴力破解、可疑会话并触发告警。

**后端契约**（在 `AuthController` + 审计模块新增）

```
POST /api/v1/auth/login                 现有；扩展返回 risk={low|medium|high}
GET  /api/v1/security/events?type=&page= 用户安全事件列表
POST /api/v1/security/events/{id}/ack    用户确认"是我"
GET  /api/v1/admin/security/anomalies    管理员视图（聚合）
POST /api/v1/admin/security/anomalies/{id}/action   block | force-logout | mark-safe
```

**实现要点**

- 规则引擎（轻量）：
  - 异地：当前 IP 城市与最近 30 天主要城市不同 → medium
  - 时差：连续两次登录地理跨度 > 1000 km / 时间差 < 1 小时 → high
  - 暴力：单 IP 单账号 5 次失败 / 10 分钟 → 触发账号锁 15 分钟
- IP 库：使用 MaxMind GeoLite2（需许可声明）
- 用户级通知：触发 high 时通过 §18.7 WS + Email 双通道告警

**API 契约样例**

```jsonc
// POST /api/v1/auth/login → 200
{ "token":"...","refreshToken":"...","userId":"u_123",
  "risk":"medium","riskReasons":["geo_change"],
  "secondFactorRequired":true,"securityEventId":"se_01H..." }
// GET /api/v1/security/events?page=1 → 200
// { "items":[{"id":"se_01H...","type":"LOGIN_GEO_CHANGE","severity":"medium",
//             "ip":"1.2.3.4","city":"Tokyo","userAgent":"...","createdAt":"...","ackedAt":null}] }
// POST /api/v1/admin/security/anomalies/{id}/action 请求 { "action":"force-logout","note":"VPN 切换" }
```

**验收标准**

- VPN 切换地区触发 medium 提示并要求二次验证
- 暴力破解触发账号短锁，UI 显示倒计时
- 管理员后台可看到聚合异常并强制踢人

---

## 19. 共享组件与门控

### 19.1 EntitlementGate 共享组件

**位置**：`src/components/access/EntitlementGate.vue`

**目的**：所有付费/角色/feature flag 受限页面统一通过此组件渲染未授权降级 UI，避免每个模块自行实现"未授权"页面。

**Props**

```typescript
interface EntitlementGateProps {
  requires?: string[]                              // 必须持有的所有 entitlements（AND）
  anyOf?: string[]                                 // 至少持有其中之一（OR）
  role?: 'ORG_ADMIN' | 'BILLING_ADMIN' | string    // 角色门控
  featureFlag?: string                             // 例：'feat.wallet.enabled'
  orgRequired?: boolean                            // 必须在组织上下文
  fallback?: 'upgrade' | 'contact-sales' | 'trial' | 'forbidden'  // 默认 upgrade
}
```

**行为**

1. 校验顺序：`featureFlag` → `orgRequired` → `role` → `requires/anyOf`
2. 失败时按 `fallback` 渲染对应 slot（默认提供购买引导卡片）
3. 通过时渲染默认 slot
4. 同时把当前页路由 meta 镜像到组件 props，避免重复声明

**降级 UI 模板（4 种）**

| fallback | 标题 | 主操作 | 次要操作 |
|---|---|---|---|
| `upgrade` | 升级 {planName} 解锁 {feature} | 立即升级 → /admin/billing | 了解更多 → /pricing |
| `contact-sales` | {feature} 需联系销售 | 联系销售 → 表单 | 加入 Waitlist |
| `trial` | 免费试用 14 天 | 启动试用 | 查看条款 |
| `forbidden` | 无权限 | 返回首页 | 联系管理员 |

**与路由 meta 联动**

- 路由 guard 调用 `auth-store.canAccess(meta)`：返回 `true` 直接进入；返回 `false` 路由仍然命中目标组件，由 `EntitlementGate` 决定渲染哪种降级
- 这样保证 SEO/书签/直链行为一致（不会被静默重定向）

**验收**

- 用户切换组织后立即重新校验（不需手刷）
- 未持有 entitlement 时不发起业务 API 请求，避免后端 403 噪音

### 19.2 路由 meta 默认值汇总

| 模块 | requires | role | orgRequired | premiumOnly | featureFlag |
|---|---|---|---|---|---|
| 钱包 | WALLET | — | true | true | feat.wallet.enabled |
| VPN | VPN | — | false | true | feat.vpn.enabled |
| 会议 | MEET | — | false | true | feat.meet.enabled |
| 联系人 | — | — | false | false | — |
| SimpleLogin | SIMPLE_LOGIN | — | true | true | feat.simplelogin.enabled |
| Standard Notes | — | — | false | false | feat.notes.enabled |
| TOTP | — | — | false | false | — |
| Drive 域名 | — | ORG_ADMIN | true | false | — |
| Admin 计费 | — | BILLING_ADMIN | false | false | — |
| 社区 | — | — | false | false | feat.community.enabled |

---

## 20. WebSocket 网关规范

> 适用于 §15.3.4 会议信令、§18.4 文档协同、§18.7 通知推送。

### 20.1 路径与命名

```
/ws/{module}                                同一域名同一端口
  /ws/notifications
  /ws/collab/{resourceType}/{resourceId}    （docs|sheets|board）
  /ws/meet/{roomId}                         （仅当后续从 SSE 改为 WS）
```

### 20.2 鉴权

- 连接 URL 携带 `?token=...`（短期 access token，5 分钟过期）；不接受 cookie 鉴权（避免跨域问题）
- 服务端 upgrade 前校验 token + 关联 `userId/orgId/scope`
- 失败关闭码：`4401` 鉴权失败、`4403` 权限不足、`4429` 限流

### 20.3 消息封装

```jsonc
// 上行 / 下行（除 CRDT binary 帧）统一 JSON
{ "type": "subscribe|publish|ack|ping",
  "channel": "notifications|collab/docs/dc_...|meet/rm_...",
  "seq": 4521,
  "payload": { ... } }
```

- `seq` 单 channel 单调递增；客户端按 `seq` 去重 / 补发
- 服务端推送 `pong` 响应客户端 30 秒 ping；客户端 60 秒未收到 pong 主动重连
- 重连：`?since={lastSeq}` 触发服务端补发缺失消息（最近 5 分钟、最多 1000 条）

### 20.4 限制与背压

| 项 | 限制 |
|---|---|
| 单连接订阅上限 | 32 channels |
| 单连接消息速率 | 100 msg/s |
| 单连接最大空闲 | 5 分钟 |
| 全局并发连接 | 单实例 10000；横向扩容由 sticky session（cookie `WS_AFFINITY`） |

超过限制：服务端发 `{type:'throttle', retryAfterMs}` 后关闭。

### 20.5 可观测性指标

```
ws_active_connections{module="notifications|collab|meet"}
ws_message_total{module,direction="up|down",type}
ws_message_latency_ms_bucket{module}
ws_disconnect_total{module,reason="client|server|throttle|auth"}
```

### 20.6 验收

- 连接 → 订阅 → 收消息 → 心跳 → 主动断开 → 5 分钟内重连补发 全链路通过
- 模拟 1000 连接稳定 30 分钟，CPU < 30%、内存 < 1 GB

---

## 21. 数据迁移与回滚

### 21.1 迁移工具

- 工具：**Flyway**（已在项目中使用）
- 目录：`backend/mmmail-server/src/main/resources/db/migration/`
- 命名：`V{N}__{module}_{purpose}.sql`，其中 `{N}` 是仓库内**单调递增整数**，与历史编号连续不允许跳号或并行分支号。例：`V27__community_init.sql`、`V28__search_index_init.sql`。
  > 历史背景：spec v1.2 之前建议过 `V2_1_2__NN`（语义版本前缀）的命名，但与 Flyway 的版本比较算法配合时容易触发"out of order"歧义；v1.3（2026-05-16 修订）起统一改为 V{N} 单调编号。本期实际落库的所有 v2.1.2 迁移已按此规约提交（V21–V32）。
- 强制：`-- DESCRIPTION:` 注释 + `-- ROLLBACK:` 注释（即使 Flyway Community 不直接执行也作为审查依据）
- 约束脚本：`scripts/check-migration-naming.sh`（CI 内强制执行），校验：
  1. 所有 `V*.sql` 必须匹配 `^V[0-9]+__[a-z0-9_]+\.sql$`
  2. 编号连续无跳号（允许从 `V1__` 起，禁止 `V100__` 突然出现）
  3. 每个文件首行必须以 `-- DESCRIPTION:` 开头，前 20 行内必须出现 `-- ROLLBACK:`

### 21.2 本期所需迁移（v2.1.2 落库现状）

| 编号 | 实际文件名 | 说明 | spec 引用 |
|---|---|---|---|
| V21 | command_panel_preferences.sql | §17.3 命令面板偏好 | §17.3 |
| V22 | v21_collaboration_board_positions.sql | §18.6 看板 lexorank position | §18.6 |
| V23 | web_push_subscription_label.sql | §17.5 WebPush 订阅 label | §17.5 |
| V24 | calendar_rrule_columns.sql | §18.2 RRULE 列扩展 | §18.2 |
| V25 | login_security_events.sql | §18.8 登录异常检测事件表 | §18.8 |
| V26 | collab_crdt_snapshot_update.sql | §18.4 CRDT snapshot/update | §18.4 |
| V27 | community_init.sql | §17.1 社区 6 张表 | §17.1 |
| V28 | search_index_init.sql | §17.2 索引视图 + 索引 | §17.2 |
| V29 | calendar_subscriptions.sql | §18.1 CalDAV/ICS 订阅 | §18.1 |
| V30 | mail_external_accounts.sql | §18.3 邮件 IMAP/SMTP 外账户 | §18.3 |
| V31 | v212_audit_event_metadata.sql | §23 审计 metadata 扩展 | §23 |
| V32 | feature_flags.sql | §19 / §27 feature flag 表 | §19 |

> 共 12 个 v2.1.2 期迁移；命名形式为 V{N} 单调编号，文件主题贴近模块名。回填脚本 / 回滚策略见 §21.3 / §21.4。

### 21.3 回填脚本

- WebPush：将现有零散订阅（散落在 mailbox 设置中的 endpoint 字段）回填到 `web_push_subscription`，保留原 `lastSuccessAt`
- 看板：现有 task `created_at` 排序作为初始 `position`（lexorank 间隔 `0|i00000:` 起步）
- 写入逻辑：先发布读端开关 → 双写一周 → 切读 → 删除旧字段（分两个 release）

### 21.4 回滚预案

| 变更 | 回滚策略 | 副作用处理 |
|---|---|---|
| 钱包发起转账 | 不可回滚（链上副作用） | 失败时调用 `POST /transactions/{id}/fail` 标记并人工介入 |
| 域名验证 | DNS 由用户掌控；删除域名时强制 30 天 grace | 已发出邮件不撤回 |
| WebPush | 关闭 feature flag → 推送停止 | 现有订阅保留，可重新启用 |
| 社区 | 软删 30 天保留 | 误删可手工恢复 |
| 索引重建 | 双表切换；失败保留 staging | 无 |
| CRDT 协同 | feature flag + 回滚到单写入模式 | snapshot 仍可读，不丢数据 |

### 21.5 删除/保留策略统一表

| 模块 | 软删 | 硬删延迟 | 还原窗口 |
|---|---|---|---|
| 邮件 | 是 | 30 天（垃圾箱） | 30 天 |
| 邮件规则 | 是 | 30 天 | 30 天 |
| 邮件外部账户 | 是 | 30 天；授权密钥删除时立即吊销 | 30 天（不恢复密钥） |
| Drive 文件 | 是 | 30 天（回收站） | 30 天 |
| Drive 版本 | 随文件 | 跟随 Drive 文件硬删窗口 | 文件还原窗口内可用 |
| Drive E2EE 分享链接 | 是（撤销即禁用） | 7 天 | 仅重新生成链接 |
| 联系人 | 是 | 30 天 | 30 天 |
| SimpleLogin Alias | 是 | 30 天 | 30 天 |
| Standard Notes 笔记 | 是 | 30 天 | 30 天 |
| TOTP 密钥 | 否（密钥立即销毁） | 立即 | — |
| 社区帖子 | 是 | 30 天 | 仅作者/管理员 |
| 全局搜索索引 | 否（可重建） | staging 7 天；主索引随源数据 | 重新触发重建 |
| 命令面板偏好 | 是 | 30 天 | 30 天 |
| 钱包账户 | 否（链上不可逆） | — | — |
| VPN 配置 | 是 | 30 天 | 30 天 |
| 会议房间 | 否（结束后归档） | 90 天 | — |
| 自定义域名 | 是 | 7 天 | 仅 ORG_ADMIN |
| Web Push 订阅 | 是（禁用即停推） | 30 天 | 30 天 |
| 日历订阅 | 是 | 30 天 | 30 天 |
| CRDT 协同快照 | 否（随文档保留） | update 日志 30 天；snapshot 跟随文档 | snapshot 可读 |
| 表格公式 | 随工作簿 | 跟随表格工作簿 | 工作簿还原窗口内可用 |
| 看板任务 | 是 | 30 天 | 30 天 |
| 通知实时事件 | 否（仅 replay） | 5 分钟 replay 窗口 | — |
| 登录安全事件 | 否 | 审计保留 365 天 | — |
| Admin 计费记录 | 否（财务留存） | 发票/订阅审计保留 7 年 | — |

---

## 22. 错误码与统一状态规范

### 22.1 错误码段位

后端复用现有 20001–90000 段；本期新增段位登记：

| 段位 | 模块 |
|---|---|
| 40021–40031 | F-A 1 钱包 |
| 40121–40131 | F-A 2 VPN |
| 40221–40231 | F-A 3 会议 |
| 40321–40331 | F-A 4 联系人 |
| 40921 | F-A 7 TOTP（PIN 锁） |
| 41021–41031 | F-B 6 Drive E2EE |
| 42221 | 通用 公式循环引用 |
| 42321 | F-A 1 Gas 太低 |
| 42921 | 通用 RATE_LIMITED |

> 完整错误码表与 i18n 键同步至 `docs/api-spec.md` 的 `errors.{code}.title|message` 命名空间。

### 22.2 统一交互状态

所有模块必须使用以下五态组件之一：

| 状态 | 组件 | 行为 |
|---|---|---|
| Loading | `<NSpin>` 或 `<NSkeleton>` | 首屏 ≤ 200ms 不显示，>200ms 显示骨架屏 |
| Empty | `<EmptyState>`（已存在） | 主图 + 文案 + 主操作 |
| Error | `<ErrorState>`（已存在） | 错误码 + 文案 + 重试 |
| Toast | `useMessage()` | 写操作成功/失败均必须有反馈 |
| 二次确认 | `useDialog().warning()` | 删除/不可逆操作必须二次确认 |

### 22.3 文案规范

- 不使用感叹号
- 错误文案"是什么 → 为什么 → 怎么办"三段式
- 包含 `traceId` 的失败提示提供"复制详情"按钮

---

## 23. 国际化与审计与可观测性

### 23.1 i18n 键约定

- 命名空间最多 3 层：`{module}.{section}.{key}`，例：`wallet.transaction.send`
- 公共键放 `common.*`：`common.action.save`、`common.state.empty`
- 复数：使用 ICU 格式 `{count, plural, one {# 项} other {# 项}}`
- 键文件：`src/locales/{lang}/{module}.ts`，模块自治
- 后端错误码 i18n 键：`errors.{code}` 双语必填

### 23.2 审计事件登记表

凡涉及"权限变更 / 财务 / 安全 / 可外发"的动作必须写审计：

| event_type | actor | target | severity | 详情字段 |
|---|---|---|---|---|
| wallet.tx.send | userId | txId | high | chain, amountWei, recipient |
| wallet.tx.sign | userId | txId | critical | signerKeyId |
| wallet.account.recover | userId | accountId | critical | method=mnemonic |
| meet.host.transfer | userId | roomId | medium | fromUserId, toUserId |
| domain.add | userId | domainId | high | domain |
| domain.delete | userId | domainId | high | domain |
| totp.entry.add | userId | entryId | medium | service |
| totp.security.update | userId | userId | high | pinEnabled, biometrics |
| community.post.delete | userId | postId | medium | reason |
| community.report.actioned | adminUserId | reportId | medium | action |
| auth.login.high_risk | userId | sessionId | high | ip, geo, riskReasons |
| billing.subscription.action | adminUserId | orgId | high | action, effectiveAt |
| webpush.subscription.delete | userId | subscriptionId | low | endpointHash |

所有审计写 `audit_log` 表 + Prometheus counter `audit_event_total{type,severity}`。

### 23.3 可观测性指标命名

```
{module}_request_total{endpoint,method,status}                Counter
{module}_request_duration_ms_bucket{endpoint,method}          Histogram
{module}_business_event_total{event}                          Counter
{module}_active_resources{kind}                               Gauge
ws_*                                                          见 §20.5
audit_event_total{type,severity}                              Counter
```

业务事件（必登记）：

| 模块 | 事件 |
|---|---|
| wallet | wallet.tx.broadcast / wallet.tx.confirm / wallet.tx.fail |
| meet | meet.room.created / meet.guest.approved / meet.host.transfer |
| search | search.query / search.zero_result / search.reindex.start |
| community | community.post.published / community.report.opened |
| collab | collab.session.start / collab.update.applied |
| security | security.event.high / security.account.locked |

### 23.4 Feature flag 命名

`feat.{module}.{capability}`：`feat.wallet.send_via_email`、`feat.collab.docs`、`feat.community.enabled`

flag 来源：`SettingsController` 现有 feature_flag 表 + 启动时载入 + Watch 实时刷新。

### 23.5 Bundle 预算

| 路由组 | 预算（gzip） |
|---|---|
| 工作台首屏 | 500 KB |
| `/wallet/*` | 200 KB |
| `/vpn/*` | 150 KB |
| `/meet/*` | 350 KB（含 WebRTC） |
| `/docs/*`（含 CRDT） | 350 KB |
| `/sheets/*` | 250 KB |
| 其他单模块 | 200 KB |

构建期由 `vite-plugin-bundle-analyzer` + `size-limit` 检查，超出阈值 PR 失败。

### 23.6 类型同步链路

- 后端通过 `springdoc-openapi` 暴露 `/v3/api-docs`
- 前端 `pnpm gen:api` 调用 `openapi-typescript` 生成 `src/service/api/__generated__/*.ts`
- CI 步骤：先跑 `pnpm gen:api && git diff --exit-code` 阻止"接口改了类型未刷新"
- 各模块 `service/api/{module}.ts` 仅对生成类型做语义封装，禁止手抄

---

## 24. 测试数据 seed

### 24.1 目录

```
backend/mmmail-server/src/main/resources/data-seed/
  ├── wallet.sql           （3 个示例账户 + 5 条交易）
  ├── meet.sql             （1 个测试会议室 + 1 个加入码）
  ├── community.sql        （5 篇示例帖子 + 评论）
  ├── search-index.sql     （触发一次重建）
  ├── domain.sql           （1 个 PENDING 域名 + 期望 DNS）
  ├── webpush.sql          （注释掉，需要真实 endpoint）
  └── ...
```

### 24.2 启用方式

- 仅 `SPRING_PROFILES_ACTIVE=dev` 生效
- `application-dev.yml`：`mmmail.dev.seed.enabled: true`
- 各模块独立开关 `mmmail.dev.seed.{module}: true`

### 24.3 验收

- 全新数据库 + dev profile 启动后 5 分钟内可看到 demo 内容
- seed 失败不影响应用启动（仅 warn 日志）

---



## 25. 实施分期与依赖关系

```
Sprint 1（P0 必交付，闭环工作台与基础协同）
  - F-B 1 Suite 真数据
  - F-A 4 联系人模块
  - F-B 4 邮件标签/文件夹拖拽
  - B-B 7 通知 WS 推送
  - §19 EntitlementGate 共享组件
  - §20 WS 网关骨架（鉴权 + 心跳 + 补发）

Sprint 2（P1 业务模块）
  - F-A 1 钱包
  - F-A 2 VPN
  - F-A 7 TOTP
  - F-B 3 邮件规则 UI
  - F-B 5 Drive 版本历史
  - F-B 9 Admin 计费
  - §21 数据迁移基础（Flyway 脚本审查机制）

Sprint 3（P1 平台能力）
  - F-A 3 会议（拆为 15.3.1 → 15.3.4，按 sprint 子周期）
  - F-B 2 Business Overview 真数据
  - F-B 6 Drive E2EE 分享
  - B-A 2 全局搜索（17.2.1 写路径 + 17.2.2 读路径 + 17.2.4 前端联动）
  - B-A 3 命令面板契约
  - B-B 2 RRULE
  - B-B 6 看板排序
  - B-B 8 登录异常检测
  - §22 错误码段位登记 + 统一状态组件采纳
  - §23 i18n / 审计 / 指标命名落地

Sprint 4（P1 涉及 B-A 新建）
  - B-A 4 域名（DomainController）→ 解锁 F-B 7
  - B-A 5 Web Push（WebPushController）→ 解锁 F-B 8
  - 17.2.3 索引重建（异步任务 + staging 切换）

Sprint 5（P2 长尾，部分滚动到 v2.1.3）
  - F-A 5 SimpleLogin
  - F-A 6 Standard Notes
  - B-A 1 社区（17.1.1 → 17.1.6 子项）
  - B-B 1 CalDAV
  - B-B 3 IMAP/SMTP
  - B-B 4 文档协同（仅交付 18.4.1 + 18.4.2；18.4.3 滚动 v2.1.3）
  - B-B 5 表格公式（v2.1.3）
```

**关键依赖**

- F-B 7 ⟂ B-A 4（域名后端必须先于前端落地）
- F-B 8 ⟂ B-A 5
- B-B 4 ⟂ B-B 7（共用 §20 WS 网关，必须同期实现网关骨架）
- B-A 2 ⟂ B-A 3（命令面板的 quick-search 复用全局搜索）
- §19 EntitlementGate ⟂ 所有付费模块（必须 Sprint 1 内交付，否则后续模块无法做降级 UI）
- 17.2.1 写路径 ⟂ v2.1 事件 outbox foundation（已就绪，不在 v2.1.2 范围内）
- 18.4.1 CRDT 基础设施 ⟂ §20 WS 网关（同期）

---

## 26. 跨切面要求（保留原文，与 §19–§24 配合）

### 26.1 国际化

所有新模块必须在 `locales/` 同时提供 `zh-CN.ts` 与 `en-US.ts`，命名空间按模块（`wallet.*`、`vpn.*`、`meet.*` 等）。后端错误码必须有对应国际化键。

### 26.2 主题与无障碍

- 所有新页面通过现有视觉 QA 检查（参考 `docs/v21-visual-qa-report-round6.md` 的检查表）
- 键盘可操作：Tab / Shift+Tab 顺序合理，所有交互控件带 `aria-label`
- 颜色对比度 ≥ 4.5:1（正文）/ 3:1（大字号）

### 26.3 测试矩阵

| 模块层级 | 单元 | 组件 | 集成 | E2E |
|---|---|---|---|---|
| F-A 1~7 完全缺失 | ≥80% | 公共组件 100% | 关键 store 100% | 主路径 |
| F-B 1~9 部分缺失 | ≥70% | 涉及组件 100% | 视图层关键路径 | 主路径 |
| B-A 1~5 后端新增 | ≥85% | — | controller + service 100% | API 契约 |
| B-B 1~8 后端扩展 | ≥80% | — | 涉及方法 100% | API 契约 |

E2E 主路径以 Playwright 编写，纳入 CI（`bash scripts/validate-local.sh` 链路）。

### 26.4 运行时与可观测性

- 所有新接口在 `prometheus` 指标中暴露 RED 三件（rate / errors / duration）
- WS 连接数 / 协同房间数 / 通知扇出延迟 必须有 Grafana 面板
- 日志结构化字段：`module`, `userId`, `orgId`, `traceId`

### 26.5 安全清单

- 新接口默认加 `@RequireEntitlement` / `@RequireRole` 后端注解；前端路由 meta 同步声明（§5.3 + §19）
- 所有富文本写入由后端 sanitize（jsoup safelist）
- 加密分享 / 钱包私钥 / TOTP 种子 任何路径都不允许明文落服务器日志
- 速率限制：登录 / 邮件外发 / 推送测试 / 命令运行 必须接入现有限流器

### 26.6 文档与运维

- 每个新模块在 `docs/superpowers/specs/` 下补 design 文档
- 后端新增控制器同步更新 `docs/api-spec.md`
- 部署变更同步更新 `docs/deployment-runbook.md`

---

## 27. 验收清单

v2.1.2 发布前必须全部通过：

### 27.1 原始迁移项（保留）

- [ ] 所有 13 个业务模块页面可访问
- [ ] 登录/注册/登出流程正常
- [ ] 邮件收发完整流程
- [ ] 日历事件 CRUD
- [ ] 文件上传/下载/分享
- [ ] 管理后台用户/组织管理
- [ ] 多语言切换（zh-CN / en-US）
- [ ] 主题切换（亮色/暗色）
- [ ] 响应式布局（4 断点）
- [ ] 标签页功能正常
- [ ] 全局搜索/命令面板
- [ ] 通知实时推送
- [ ] E2E 加密流程
- [ ] Lighthouse 性能评分 > 80
- [ ] 无 TypeScript 编译错误
- [ ] 无 ESLint/Oxlint 错误
- [ ] E2E 测试全部通过

### 27.2 F-A 完全缺失模块（必须 P0/P1 全部交付）

- [ ] 钱包：账户/交易/邮件转账/补救端到端可用
- [ ] VPN：快速连接/服务器目录/配置 CRUD
- [ ] 会议：主持人 + 1 访客 + 1 用户三方会议跑通
- [ ] 联系人：独立路由 + CSV 导入 + 重复合并
- [ ] TOTP：主网格 + PIN + QR 导入

### 27.3 F-B 部分缺失（P0/P1 必须）

- [ ] Suite/Business 工作台无 mock 数据
- [ ] 邮件规则编辑 + 预览
- [ ] 邮件拖拽到文件夹/标签
- [ ] Drive 版本历史 + 还原
- [ ] Drive E2EE 分享创建与公开访问
- [ ] 设置 - 域名管理（依赖 B-A 4）
- [ ] 设置 - Web Push（依赖 B-A 5）
- [ ] Admin 计费总览 + 订阅管理

### 27.4 B-A 后端新增

- [ ] 全局搜索（17.2.1 索引视图 + 17.2.2 读路径 + 17.2.3 重建 + 17.2.4 前端）6 类内容命中且无越权
- [ ] 命令面板 catalog/recents/quick-search 上线
- [ ] DomainController 完整契约（含 dns-records / verify / diagnostics）
- [ ] WebPushController 完整契约（含 vapid-public-key / subscriptions / test）

### 27.5 B-B 后端扩展（P0/P1）

- [ ] RRULE 重复事件展开（含 thisAndFollowing scope）
- [ ] 看板拖拽 position 持久化（lexorank）
- [ ] 通知 WS 推送 + 断线补发（含 since cursor）
- [ ] 登录异常检测 medium/high 分级 + 二次验证
- [ ] CRDT 基础设施（18.4.1）+ Docs 协同接入（18.4.2）

### 27.6 共享组件与门控（§19）

- [ ] EntitlementGate 4 种 fallback 全部上线
- [ ] 路由 meta 默认值表（§19.2）逐项核对
- [ ] 用户切组织自动重新校验

### 27.7 WebSocket 网关（§20）

- [ ] 鉴权 / 心跳 / 补发 / 限流 全链路可用
- [ ] 1000 连接稳定 30 分钟测试通过
- [ ] WS 指标 Grafana 面板上线

### 27.8 数据迁移（§21）

- [ ] 10 条 Flyway 脚本全部 review 并附 ROLLBACK 注释
- [ ] WebPush / 看板 position 数据回填脚本验证
- [ ] 删除/保留策略表覆盖所有新模块

### 27.9 错误码与状态（§22）

- [ ] 新错误码段位逐项登记并写入 `docs/api-spec.md`
- [ ] 每个新页面五态（loading/empty/error/toast/二次确认）齐备
- [ ] 文案"是什么 → 为什么 → 怎么办"三段式

### 27.10 i18n / 审计 / 可观测性（§23）

- [ ] zh-CN + en-US 全量翻译
- [ ] 13 个审计事件 actor/target/severity 字段齐全
- [ ] 模块业务事件 Counter 指标可见
- [ ] Bundle 预算每路由组 <= 表中阈值
- [ ] `pnpm gen:api` CI 步骤生效

### 27.11 测试数据 seed（§24）

- [ ] dev profile 启动后 5 分钟内 demo 内容可见

### 27.12 跨切面（§26 原文）

- [ ] 所有新接口 Prometheus 指标可见
- [ ] WS 网关 Grafana 面板上线
- [ ] 单元/组件/集成/E2E 覆盖率达标（§26.3）
- [ ] 国际化 zh-CN + en-US 全量
- [ ] 安全清单（§26.5）逐项审过

### 27.13 Soybean 框架强制复用（§6.4）

- [ ] 全仓搜索 `\.scss$` 文件无新增（仅保留 `src/styles/scss/` 原文件）
- [ ] 全仓搜索 `--mm-` / `--v211-` CSS 变量为零
- [ ] 全仓搜索 `import axios` 裸引（必须 `@sa/axios`）为零
- [ ] 没有自研的 `*Shell` / `*AppLayout` / `*Layout` 自定义布局组件
- [ ] 所有新页面用 `BaseLayout` + `src/layouts/modules/*` 现有组件
- [ ] 所有图标走 `@iconify/vue` / `unplugin-icons` / `<svg-icon />`，无自定义 SVG 散落
- [ ] `package.json` 没有 Element Plus / Ant Design / Tailwind / moment / Sortable.js / Chart.js / D3 / MapBox 等被禁库
- [ ] `scripts/check-style-discipline.mjs`（§6.4.5）CI 通过
- [ ] 任何新依赖（除 §6.4.4 默认通过列表）都有决议记录

> P2 项（社区、SimpleLogin、Standard Notes、CalDAV、IMAP/SMTP、文档协同 18.4.3、表格公式）允许滚动至 v2.1.3，但 spec 与占位路由必须在 v2.1.2 中保留。

---

## 28. 风险与未决（Open Questions）

> 本章列出本期 spec 中**未做最终决定**或**已知风险**的事项。每项含触发条件、当前倾向、决定截止日期与责任人占位。

### 28.1 技术选型未决

| 编号 | 问题 | 当前倾向 | 备选 | 决定截止 |
|---|---|---|---|---|
| Q-1 | CRDT 服务端实现 | yrs JNI 嵌入 Spring | 独立 Node 微服务 / 后端仅中转（前端打 snapshot） | Sprint 5 启动前 |
| Q-2 | 全局搜索引擎 | PG `tsvector` + `pg_trgm` | OpenSearch（v2.2 起迁移） | 本期固定 PG，v2.2 重评 |
| Q-3 | 钱包链支持范围 | ETH + Polygon + Base + BTC（mainnet） | 加 Solana / 测试网 | Sprint 2 启动前 |
| Q-4 | 公式引擎 | EvalEx MVP | Apache POI / 自研 ANTLR | v2.1.3 评估 |
| Q-5 | 社区是否联邦化（ActivityPub） | 否 | 是 | 暂不考虑，纳入 v3 路线图 |
| Q-6 | IMAP 是否仅企业版 | 是（`requires: IMAP_BRIDGE`） | 全量开放 | 法务/反滥用评估后定 |
| Q-7 | 命令面板"近用命令"跨设备同步 | 同步至 user，组织无关 | 按 org 分桶 | Sprint 3 启动前 |

### 28.2 潜在风险

| 编号 | 风险 | 影响 | 缓解 |
|---|---|---|---|
| R-1 | yrs JNI 与 JVM GC 冲突 | 协同延迟尖峰 | Sprint 5 内 spike：1 周原型；不达标走"后端中转 + 前端 snapshot" |
| R-2 | WebRTC 在企业网络穿透失败 | 会议无法连通 | 默认 STUN，企业版 TURN（自建 coturn）；运维需要确认资源 |
| R-3 | DKIM 私钥落 KMS 性能 | 大量发件签名延迟 | 进程内缓存 5 分钟 + 后台轮换 |
| R-4 | MaxMind GeoLite2 许可 | 法务合规 | 法务确认许可声明放置位置 |
| R-5 | E2EE 分享密钥放 URL fragment | 浏览器历史/书签泄露 | UI 警示 + 默认开启密码保护 + 默认 7 天过期 |
| R-6 | CSV 联系人导入 1k 行性能 | 浏览器卡顿 | 切片提交（每片 100 行） + Web Worker 解析 |
| R-7 | 看板 lexorank 极端长 | 多次重排导致 key 膨胀 | 每 10000 次写入触发一次 rebalance（后台任务） |
| R-8 | 全局搜索权限二次过滤 N+1 | 慢查询 | 走批量 ACL 校验接口 + Redis 缓存 |
| R-9 | feature flag 错误关闭引发功能消失 | 用户误以为故障 | 关闭时显示"该功能已停用"占位页而非空白 |
| R-10 | i18n 漏键 | 显示英文 fallback | CI 步骤校验 zh-CN/en-US 键集合一致 |

### 28.3 显式不做（v2.1.2 范围外）

- 移动端原生 App：仅保证 Web 响应式
- 视频会议录制 / 字幕 / 翻译：v2.2
- 钱包多签 / 硬件钱包：v2.2
- 社区联邦（ActivityPub）：v3
- 表格透视表 / 图表向导：v2.1.3
- IMAP CalDAV 双向写：v2.1.3 起

### 28.4 责任与决定流程

- 每个 Q-* / R-* 在 v2.1.2 启动会上指定 Owner + Reviewer
- 决定记录在 `docs/superpowers/specs/2026-05-15-v212-decision-log.md`（占位文件，启动会后填充）
- Owner 在截止日 T-3 给出书面决议，否则升级至 CTO 决策

---
