# MMMail v2.1.1 UI 最终规范

本文件是 v2.1.1 的唯一权威规范。所有声明必须可被代码或测试验证。本文件不描述阶段排期、人力估算、风险登记、回滚原因等过程性内容。

当本规范与其他文档冲突时，以本文件为准。

## 0. 规范状态

- 版本：v2.1.1（final spec）
- 适用范围：`frontend-v2/`（Vue 3 + Naive UI + Pinia + Vue Router）
- 不在范围：`backend/` 商业运行时/业务闭环改造（支付、订阅、发票、真实扣款）、第三方运营页、`docs/MMMail/UI` 设计稿原图本身。允许 v2.1.1 所需的只读聚合接口和字段补齐（基于现有数据表和服务能力）。
- 生效条件：第 14 节合规验收清单全部通过

## 1. 设计源与品牌

### 1.1 设计源

v2.1.1 视觉来源是 `docs/MMMail/UI` 中各模块的概览图。每个模块必须声明其设计源；声明落在 `frontend-v2/tests/fixtures/v211-design-source.json`，由 `v211-design-source-contract.test.mjs` 校验"声明的源图存在且模块路由能映射到声明"。

| 模块 ID | 路由根 | 设计源 |
| --- | --- | --- |
| `home` | `/` | `docs/MMMail/UI/首页/工作台-设计概览.png` |
| `mail` | `/mail` | `docs/MMMail/UI/邮件/邮件-设计概览.png` |
| `calendar` | `/calendar` | `docs/MMMail/UI/日历/日历概览.png` |
| `drive` | `/drive` | `docs/MMMail/UI/云盘/云盘概览.png` |
| `docs` | `/docs` | `docs/MMMail/UI/文档/文档概览.png` |
| `sheets` | `/sheets` | `docs/MMMail/UI/Sheets和labs/表格概览.png` |
| `pass` | `/pass` | `docs/MMMail/UI/Pass/Pass概览.png` |
| `collaboration` | `/collaboration` | `docs/MMMail/UI/Collaboration/协作概览.png` |
| `command-center` | `/command-center` | `docs/MMMail/UI/CommandCenter/命令概览.png` |
| `notifications` | `/notifications` | `docs/MMMail/UI/Notifications/通知概览.png` |
| `admin` | `/admin` | `docs/MMMail/UI/Admin/管理后台.png` |
| `settings` | `/settings` | `docs/MMMail/UI/Setting/设置概览.png` |

### 1.2 品牌

- 产品名：`MMMail`，统一在 `src/locales/index.ts` 中声明，禁止 Vue 模板硬编码。
- 设计稿中的 `Nexa Workspace` / `Acme` / `Workspace` 等占位品牌不得进入任何用户可见文案、`<title>`、邮件模板、API meta。
- favicon、登录页 logo、Shell 顶部品牌区都从 `src/design-system/v211/branding.ts` 读取，禁止散落 PNG/SVG 直接 import 到组件。

## 2. 技术基线

### 2.1 依赖版本

| 包 | v2.1.1 版本范围 | 约束 |
| --- | --- | --- |
| `vue` | `^3.5.0` | 不跨 3.6/4 |
| `naive-ui` | `^2.44.0` | v2.1.1 期间锁定 2.44.x；升 2.45 必须重跑全部视觉门禁 |
| `pinia` | `^2.3.0` | |
| `vue-router` | `^4.5.0` | |
| `@vueuse/core` | `^12.8.0` | 用于断点/事件监听封装 |
| `vite` | `^7.3.0` | |
| `typescript` | `^5.7.0` | |
| `vue-tsc` | `^2.2.0` | |
| `echarts` | `^5.5.0` | 按需 `import { use } from 'echarts/core'`，不引入全包 |

依赖变更必须保持 `frontend-v2/package.json` 与本节同步；新增 UI 库（除 ECharts）必须先修订本规范。

### 2.2 构建产物约束

| 指标 | 阈值 |
| --- | --- |
| ECharts 独立 chunk (gzip) | ≤ 80 KB |
| 主入口 JS/CSS (gzip) | 不超过 P0 基线 + 15% |
| Lighthouse Performance (桌面) | ≥ P0 基线 − 5 分 |

ECharts 不进入全局 Shell 首屏同步包，必须通过共享图表组件按需加载或路由级拆分。

### 2.3 性能测量条件

- P0 记录当前 `pnpm --dir frontend-v2 build` 产物体积作为 baseline。
- P2 起以首页和邮件页的桌面端 Lighthouse Performance 分数作为 baseline。
- 测量条件统一为：Lighthouse CI desktop preset，视口 `1350x940`，无网络节流，CPU slowdown `4x`，冷启动登录态。
- 后续模块不得低于首页/邮件较低值 5 分以上；首页和邮件自身也不得回退超过 5 分。

## 3. Naive UI 强制约束

### 3.1 基础控件来源

Vue 模板中的以下原生标签默认禁止：`button`、`input`、`textarea`、`select`、`table`。

例外只允许进入白名单 `frontend-v2/tests/fixtures/v211-native-control-allowlist.json`，每条记录格式：

```json
{
  "file": "frontend-v2/src/views/sheets/SheetsGrid.vue",
  "tag": "input",
  "reason": "spreadsheet 单元格编辑器需要原生输入焦点控制",
  "removeBy": "P5",
  "owner": "v2.1.1-ui"
}
```

CI 由 `v211-naive-ui-contract.test.mjs` 扫描 `src/**/*.vue`，在白名单外发现禁用标签即失败。Markdown 渲染产物、第三方 iframe 内的标签不受此约束。

### 3.2 门禁阈值

| 标签 | P0 基线 | P1 上限 | P2 上限 | P3 上限 | P4 上限 | P5 目标 |
| --- | --- | --- | --- | --- | --- | --- |
| `button` | 176 | ≤ 120 | ≤ 80 | ≤ 35 | ≤ 15 | 0（非白名单） |
| `input` | 22 | ≤ 16 | ≤ 10 | ≤ 6 | ≤ 3 | 0（非白名单） |
| `textarea` | 4 | ≤ 3 | ≤ 2 | ≤ 1 | 0 | 0（非白名单） |
| `select` | 4 | ≤ 3 | ≤ 2 | ≤ 1 | ≤ 1 | 0（非白名单） |
| `table` | 1 | 0 | 0 | 0 | 0 | 0 |

### 3.3 自研边界

自研组件只能作为 Naive UI 组件的组合封装。允许保留必要原生结构的业务画布：

- Spreadsheet 单元格编辑器（`SheetsGrid.vue`）
- 日历时间网格（`V211CalendarGrid.vue`）
- 富文本编辑器画布
- Canvas/SVG 图表渲染区

以上必须进入白名单并声明 `removeBy` 阶段。

### 3.4 NConfigProvider 约束

- 全局唯一 `NConfigProvider` 在 `App.vue` 注入 v2.1.1 theme overrides。
- 页面级 CSS 只负责布局、业务画布和 domain-specific grid，不负责重写按钮、输入框、表格、弹窗基础视觉。
- 禁止在组件内嵌套 `NConfigProvider` 覆盖主题（除非有明确的 dark/light 区域切换需求并进入白名单）。

## 4. Naive UI 组件映射

设计稿中的每类 UI 元素必须映射到以下 Naive UI 组件，模块代码不得绕过此映射自行实现。

| 设计稿元素 | v2.1.1 使用组件 |
| --- | --- |
| 应用壳层 | `NLayout`、`NLayoutSider`、`NLayoutHeader`、`NLayoutContent`、`NMenu` |
| 顶部搜索 | `NInput`、`NButton`、`NDropdown`、`NAvatar`、`NBadge` |
| 面包屑/标题 | `NBreadcrumb`、`NPageHeader`、`NSpace`、`NFlex` |
| KPI 卡片 | `NCard`、`NStatistic`、`NNumberAnimation`、`NProgress`、`NTag` |
| 表格 | `NDataTable`、`NPagination`、`NCheckbox`、`NDropdown` |
| 普通列表 | `NList`、`NListItem`、`NThing`、`NAvatar`、`NTag` |
| 标签页 | `NTabs`、`NTabPane` |
| 表单 | `NForm`、`NFormItem`、`NInput`、`NSelect`、`NDatePicker`、`NSwitch`、`NCheckbox`、`NRadioGroup` |
| 抽屉 | `NDrawer`、`NDrawerContent` |
| 弹窗 | `NModal`、`NPopconfirm`、`NDialogProvider` |
| 提示 | `NTooltip`、`NPopover`、`NAlert`、`NMessageProvider`、`NNotificationProvider` |
| 上传 | `NUpload`、`NUploadDragger`、`NProgress` |
| 树和目录 | `NTree`、`NTreeSelect` |
| 空态/加载态 | `NEmpty`、`NResult`、`NSkeleton`、`NSpin` |
| 时间线/活动流 | `NTimeline`、`NTimelineItem` |
| 日历控件 | `NCalendar`、`NDatePicker`、`NTimePicker` |
| 虚拟滚动 | `NVirtualList`（列表 ≥ 200 行启用） |

设计稿中存在但 Naive UI 没有的能力，由第 5 节共享业务组件提供。

## 5. 共享业务组件

所有共享组件位于 `src/design-system/v211/components/`，以 `V211` 前缀命名，barrel `src/design-system/v211/index.ts` 导出。模块代码仅允许从 barrel 导入；禁止深路径导入。

签名变更由 `v21-design-system-components.test.mjs` 锁定，PR 需同步快照。

### 5.1 通用类型

```ts
type TextLike = string | { 'zh-CN': string; 'zh-TW': string; en: string }
type Tone = 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'brand'
type ModuleId =
  | 'home' | 'mail' | 'calendar' | 'drive' | 'docs' | 'sheets' | 'pass'
  | 'collaboration' | 'command-center' | 'notifications' | 'admin' | 'settings'

interface AsyncState {
  loading?: boolean
  error?: string | null
  empty?: boolean
}
```

`TextLike` 必须复用 `src/locales/index.ts` 中的现有定义，禁止新建并行类型。

### 5.2 V211AppShell

| 维度 | 契约 |
| --- | --- |
| 用途 | 设计稿级应用框架：左侧窄导航 + 顶部栏 + 主内容 + 右侧上下文面板 |
| props | `currentModule: ModuleId`、`navCollapsed?: boolean`、`rightPanelCollapsed?: boolean`、`rightPanelWidth?: number` |
| 内部组件 | `NLayout`、`NLayoutSider`、`NLayoutHeader`、`NLayoutContent`、`NMenu`、`NDropdown`、`NAvatar`、`NBadge` |
| slots | `topBarLeft`、`topBarRight`、`default`（主内容）、`rightPanel` |
| 不允许 | 任何模块自写 `<header>` / `<aside>`；任何模块在 Shell 外发起搜索/通知/账户菜单 |

### 5.3 V211MetricCard

| 维度 | 契约 |
| --- | --- |
| props | `title: TextLike`、`value: number \| string`、`trend?: { direction: 'up' \| 'down' \| 'flat'; percent?: number }`、`status?: Tone`、`loading?: boolean` |
| slots | `chart`（底部 mini chart 区域）、`footer` |
| 内部组件 | `NCard`、`NStatistic`、`NNumberAnimation`、`NProgress`、`NTag` |
| 复用要求 | 至少首页和 Admin 两个模块接入 |
| 不允许 | 直接写 `<div class="metric-card">` + 自定义数字动画 |

### 5.4 V211SectionPanel

| 维度 | 契约 |
| --- | --- |
| props | `title: TextLike`、`description?: TextLike`、`actions?: SectionAction[]`、`loading?: boolean`、`empty?: boolean`、`error?: string \| null`、`collapsible?: boolean`、`defaultCollapsed?: boolean` |
| slots | `default`、`empty`、`error`、`actions` |
| 内部组件 | `NCard`、`NButton`、`NSkeleton`、`NEmpty`、`NResult`、`NCollapse` |
| 不允许 | 自定义 section card 样式；模块内重复实现 loading/empty/error 三态 |

### 5.5 V211DataToolbar

| 维度 | 契约 |
| --- | --- |
| props | `searchPlaceholder?: TextLike`、`filters?: FilterConfig[]`、`createLabel?: TextLike`、`createAction?: () => void`、`bulkActions?: BulkAction[]`、`selectedCount?: number` |
| emits | `search(keyword: string)`、`filter(filters: Record<string, unknown>)`、`create()`、`bulkAction(actionId: string)` |
| 内部组件 | `NInput`、`NButton`、`NDropdown`、`NSpace`、`NBadge` |
| 复用要求 | 至少邮件和云盘两个模块接入 |
| 不允许 | 模块内各自实现搜索栏 + 筛选 + 创建按钮组合 |

### 5.6 V211EntityList

| 维度 | 契约 |
| --- | --- |
| props | `items: EntityListItem[]`、`selectedId?: string`、`selectable?: 'single' \| 'multiple' \| false`、`virtualScroll?: boolean`、`itemHeight?: number`、`loading?: boolean`、`empty?: boolean` |
| `EntityListItem` 形状 | `{ id, avatar?, title, meta?, tags?: StatusTag[], badge?, actions?: ItemAction[], disabled?, ariaLabel? }` |
| emits | `update:selectedId`、`select(id)`、`action(id, actionId)` |
| 内部组件 | `NList` + `NThing` + `NAvatar` + `NTag` + `NCheckbox`(批量) + `NVirtualList`(超过 200 行启用) |
| a11y | 列表 `role="listbox"`；多选时 `aria-multiselectable="true"`；每项 `aria-selected`；Tab 进入列表后用方向键移动 |
| 不允许 | 直接写 `<ul><li>` + 自定义 hover/selected 样式 |

### 5.7 V211RightInsightPanel

| 维度 | 契约 |
| --- | --- |
| props | `module: ModuleId`、`tabs: InsightTab[]`、`activeTab?: string`、`loading?: boolean`、`error?: string \| null`、`collapsed?: boolean`、`width?: number` |
| slots | 每个 tab 一个 named slot；`empty`、`error` 兜底 slot |
| 内部组件 | `NCard` + `NTabs` + `NScrollbar` + `NSkeleton` + `NEmpty` + `NResult` |
| 数据契约 | 必须接受外部传入的 `loading`/`error`，不在组件内部发请求；保证父级模块 store 拥有数据所有权 |
| a11y | 折叠按钮 `aria-expanded`、`aria-controls`；切换 tab 后焦点回到 tab panel 起始 |
| 不允许 | 直接调用 service 层 fetch；用 `v-show` 隐藏后保留滚动条事件 |

### 5.8 V211ModuleToolbar / V211ActionBar / V211StatusTag / V211MiniChart

| 组件 | 关键契约 |
| --- | --- |
| `V211ModuleToolbar` | props: `title`、`breadcrumbs?`、`tabs?`、`actions?`、`density?`；内部 `NPageHeader` + `NBreadcrumb` + `NTabs` + `NButton`；不接管搜索（搜索归 `V211DataToolbar`） |
| `V211ActionBar` | props: `selectedCount`、`primaryAction`、`secondaryActions?`、`dangerActions?`；危险操作必须包 `NPopconfirm`；显示选择计数文案需走 i18n |
| `V211StatusTag` | props: `tone: Tone`、`label: TextLike`、`icon?`；颜色 token 来自 `tokens.css`，不允许直接写 hex |
| `V211MiniChart` | props: `type: 'line' \| 'bar' \| 'pie' \| 'gauge'`、`data`、`height?`、`loading?`；内部 lazy import ECharts 模块；卸载时调用 `dispose()`；resize 通过 `ResizeObserver` 节流 |

### 5.9 类型与导出约束

- 所有共享组件类型放在 `frontend-v2/src/design-system/v211/types.ts`，模块代码只能从 `@/design-system/v211` barrel 导入。
- `TextLike` 沿用现有 `src/locales/index.ts` 中的 `TextLike` 类型，不允许新建并行类型。
- 任何接收 `value` 字段并展示给用户的 props 必须接受 `TextLike`，不允许 `string` 直写硬编码中文。

## 6. 架构分层

### 6.1 第一层：主题与 Token

目录：`frontend-v2/src/design-system/v211/`

| 文件 | 职责 |
| --- | --- |
| `theme.ts` | Naive UI theme overrides 工厂，集中定义主色、圆角、阴影、字号、表格行高 |
| `tokens.css` | CSS 变量，只保留布局变量和设计稿专用间距，不覆盖 Naive 基础控件 |
| `branding.ts` | 品牌资源（logo、favicon、产品名）统一导出 |
| `breakpoints.ts` | 响应式断点常量与 `useBreakpoint()` composable |
| `format.ts` | 数字/日期/容量格式化（千分位、小数位、时区） |
| `chart-palette.ts` | ECharts 色板，图表封装从此读取 |

视觉目标：
- 主色从当前蓝紫倾向改为设计稿中的绿色/青绿色主色
- 背景使用浅灰工作台底色，卡片白底，边框轻量
- 默认控件圆角、表格行高、按钮高度收敛到设计稿的紧凑企业应用风格

Token 双轨约束：
- `tokens.css` 提供 CSS 变量给布局/grid/canvas
- `theme.ts` 把同一组语义 token 注入 Naive overrides
- 两者必须共用同一份语义键名（`brandPrimary`、`surface`、`textPrimary` 等），不允许命名漂移
- 自定义图表颜色序列写在 `chart-palette.ts`，ECharts 封装从该文件读取，不在组件内 hardcode

### 6.2 第二层：全局壳层

重构文件清单：

| 文件 | 职责 |
| --- | --- |
| `src/layouts/base-layout/BaseLayout.vue` | 顶层 Shell 容器 |
| `src/layouts/modules/ShellSideNav.vue` | 左侧窄导航 |
| `src/layouts/modules/ShellTopBar.vue` | 顶部搜索/用户区 |
| `src/layouts/modules/ContextPanel.vue` | 右侧上下文面板 |
| `src/layouts/modules/MobileTabBar.vue` | 移动端底部导航 |

目标：
- 用 `NLayout` / `NMenu` / `NDropdown` / `NAvatar` 替代原生按钮和自定义导航
- 还原设计稿左侧窄导航、底部容量/状态卡、顶部搜索和右侧用户区
- 右侧上下文面板必须展示真实内容，不允许大面积空白占位

### 6.3 第三层：共享业务组合组件

见第 5 节。每个共享组件必须至少有 1 个真实页面接入；`V211MetricCard`、`V211DataToolbar`、`V211RightInsightPanel` 必须至少有 2 个接入点。

### 6.4 第四层：模块逐屏对齐

实施顺序按设计差距和公共组件复用价值排序：

1. 首页工作台：打通全局壳层和 KPI/列表/右栏模式
2. 邮件：三栏密度、写信抽屉、阅读区和附件安全状态
3. 日历：迷你月历、周视图网格、事件抽屉
4. 云盘：文件表、预览详情、分享/权限面板
5. 文档和 Sheets：编辑器框架、右侧协作/AI 面板、表格画布
6. Pass：vault 列表、详情、风险统计
7. Collaboration / Command Center / Notifications：运营类密集表格和活动流
8. Admin / Settings：治理后台和账号安全中心

## 7. 模块视觉目标（概览）

每个模块的 v2.1.1 目标状态概览。详细规范见第 8 节（全局壳层）和第 9 节（各模块）。模块验收时必须与设计源并排对比。

### 7.1 全局壳层

| 区域 | 目标 |
| --- | --- |
| 左侧导航 | 窄侧栏（56-64px），图标 + tooltip，底部容量/状态卡 |
| 顶部栏 | 紧凑高度（48-56px），左侧品牌 + 搜索，右侧通知/用户 |
| 右侧面板 | 真实上下文内容，按模块切换，支持折叠 |
| 移动端 | 底部 TabBar，顶部精简栏，右栏走全屏 Drawer |

### 7.2 首页

还原设计稿工作台：KPI 横排（4 列）、今日日程列表、最近文件卡片、快捷入口、右侧动态流。

### 7.3 邮件

还原文件夹栏 + 消息列表 + 阅读区三栏布局、写信面板（Drawer）、附件条和安全提示。

### 7.4 日历

还原迷你月历、周视图时间网格、事件色块、资源/冲突/表单右栏。

### 7.5 云盘

还原文件列表（表格 + 网格切换）、预览卡、共享状态、版本和安全摘要。

### 7.6 文档

还原文档编辑器画布、目录、评论、权限、版本信息右栏。

### 7.7 Sheets / Labs

还原表格网格、公式栏、AI/Labs 侧栏、图表和批注。

### 7.8 Pass

还原密码库列表、详情卡、风险统计环、共享与安全操作。

### 7.9 Collaboration

还原项目/任务/评论/活动混合工作台。

### 7.10 Command Center

还原命令表、运行状态、终端日志、工作流摘要。

### 7.11 Notifications

还原通知列表、规则、统计环、分组和右侧摘要。

### 7.12 Admin

还原治理 KPI、趋势图、审计、风险、表单和系统摘要。

### 7.13 Settings

还原账号安全中心式设置页：账号、安全、设备、偏好、存储。

## 8. 全局壳层规范

### 8.1 Shell 结构

重构以下文件实现设计稿级壳层：

- `frontend-v2/src/layouts/base-layout/BaseLayout.vue`
- `frontend-v2/src/layouts/modules/ShellSideNav.vue`
- `frontend-v2/src/layouts/modules/ShellTopBar.vue`
- `frontend-v2/src/layouts/modules/ContextPanel.vue`
- `frontend-v2/src/layouts/modules/MobileTabBar.vue`

目标：

- 用 `NLayout` / `NMenu` / `NDropdown` / `NAvatar` 替代原生按钮和自定义导航。
- 还原设计稿左侧窄导航（图标 + tooltip）、底部容量/状态卡、顶部搜索和右侧用户区。
- 右侧上下文面板必须展示真实内容，不允许大面积空白占位。

### 8.2 右侧上下文面板内容策略

| 模块 | 右栏内容来源 |
| --- | --- |
| 首页 | 通知流、今日日程、最近协作动态、AI 助手入口，优先来自 workspace summary API |
| 邮件 | 当前邮件联系人、附件列表、安全/可信状态、相关会话，来自当前选中邮件详情 |
| 日历 | 当前事件详情、参与人、资源、冲突提示、快速编辑表单，来自日历事件状态 |
| 云盘 | 当前文件预览、元数据、分享权限、版本和活动记录，来自文件详情 API |
| 文档 | 评论、目录、权限、版本信息，来自文档详情和协作状态 |
| Sheets / Labs | AI 洞察、批注、保护区域、图表配置，来自表格当前工作簿状态 |
| Pass | 当前密钥详情、安全评分、共享成员、轮换建议，来自 vault item 详情 |
| Collaboration | 成员、任务活动、评论、关联文件，来自项目/任务选中状态 |
| Command Center | 当前命令运行、日志、风险、审批和审计摘要，来自执行流状态 |
| Notifications | 当前通知详情、规则、渠道、统计，来自通知中心列表选中项 |
| Admin | 租户风险、审计动作、系统健康、治理队列，来自 admin summary API |
| Settings | 账号安全、设备、存储、偏好和帮助入口，来自用户 profile/settings API |

内容优先级为 API 数据，其次是当前模块已加载状态。接口为空时展示显式 `NEmpty`；请求中使用 `NSkeleton` 或 `NSpin`；错误使用 `NResult` 或 `NAlert`。不允许用伪数据填满右栏来制造完成感。

### 8.3 右栏 API 覆盖状态

| 模块 | 当前可用 API / 状态 | 处理方式 |
| --- | --- | --- |
| 首页 | `workspace.ts` 已有 summary、activity、tasks | 可直接接入，缺少最近文件时复用 Drive list |
| 邮件 | `mail.ts` 已有 folder、detail、trust、rules、bulk action | 可直接接入，右栏基于当前选中邮件详情 |
| 日历 | `calendar.ts` 已有 events、agenda、availability、resources、settings | 可直接接入，冲突提示优先用 availability |
| 云盘 | `drive.ts` 已有 items、usage、shares、versions | 可直接接入，预览内容不足时展示元数据和版本 |
| 文档 | `docs.ts` 已有 notes、detail、comments、versions、share | 可直接接入，目录可由前端从正文结构派生 |
| Sheets / Labs | `sheets.ts` 已有 workbook、import、cleaning rules、insights | 部分接入；批注、保护区域和图表配置先记为 API 缺口 |
| Pass | `pass.ts` 已有 items、vaults、monitor、secure links、aliases | 部分接入；选中密钥详情、轮换建议和成员明细需后端补齐或降级为空态 |
| Collaboration | `collaboration.ts` 已有 projects、tasks、comments、activity | 可直接接入 |
| Command Center | `command-center.ts` 已有 commands、runs、workflows、audit | 可直接接入 |
| Notifications | `notifications.ts` 已有 list、rules、subscriptions、templates、analytics | 可直接接入 |
| Admin | `admin.ts` 已有 summary、users、roles、domains、policies、audit、alerts、system、risk | 可直接接入，治理队列不足时用 alerts/audit 组合 |
| Settings | `settings.ts` 已有 profile、security、devices、notifications、integrations、audit | 部分接入；存储详情可复用 Drive usage，偏好缺口进入 API gap 清单 |

v2.1.1 不重做后端商业闭环，但允许补充只读聚合接口或字段补齐，前提是基于现有数据表和服务能力，不引入支付、订阅、发票、真实扣款等商业运行时逻辑。

## 9. 模块视觉对齐目标

每个模块必须达到以下视觉目标，验收通过并排截图对比。

### 9.1 首页（Home）

| 维度 | 规范 |
| --- | --- |
| 布局 | KPI 横排（4 列 xl / 3 列 lg / 2 列 md / 1 列 sm）+ 今日日程列表 + 最近文件 + 快捷入口 + 右侧动态 |
| 必用组件 | `V211MetricCard`、`V211SectionPanel`、`V211EntityList`、`V211RightInsightPanel`、`V211MiniChart` |
| 数据源 | `workspace.ts` summary + activity + tasks；最近文件复用 `drive.ts` list |
| 不允许 | 营销式 hero banner；大面积空白卡片；硬编码中文 |

### 9.2 邮件（Mail）

| 维度 | 规范 |
| --- | --- |
| 布局 | 三栏：文件夹栏（`NMenu`）+ 消息列表（`V211EntityList`）+ 阅读区（`NCard`）|
| 写信 | `NDrawer` 侧栏，内含 `NForm` + `NInput` + `NUpload`；附件条和安全提示 |
| 工具栏 | `V211DataToolbar` 搜索 + 筛选 + 批量操作 |
| 右栏 | 联系人、附件、安全状态、相关会话 |
| 不允许 | 自定义三栏 CSS grid 不使用 `NLayout`；原生 `<button>` 工具栏 |

### 9.3 日历（Calendar）

| 维度 | 规范 |
| --- | --- |
| 布局 | 迷你月历（`NCalendar`）+ 周视图时间网格（`V211CalendarGrid`）+ 事件色块 + 右栏事件详情 |
| 事件操作 | 点击事件 → 右栏详情 + 快速编辑表单（`NDrawer`）|
| 资源/冲突 | 右栏展示参与人、资源占用、冲突提示 |
| 不允许 | 空白大面积网格无事件数据；月历和周视图不联动 |

### 9.4 云盘（Drive）

| 维度 | 规范 |
| --- | --- |
| 布局 | 文件树（`NTree`）+ 文件表（`NDataTable`）+ 右栏预览详情 |
| 操作 | 上传（`NUpload`）、分享（`NModal`）、版本（`NDrawer`）|
| 右栏 | 文件预览、元数据、分享权限、版本历史、活动记录 |
| 不允许 | 文件区数据稀疏无分页；右栏空白占位 |

### 9.5 文档（Docs）

| 维度 | 规范 |
| --- | --- |
| 布局 | 文档列表页 + 编辑器布局（编辑画布 + 目录 + 右侧评论/权限面板）|
| 编辑器 | 自定义编辑区（白名单）+ `NTabs` 切换评论/版本/权限 |
| 右栏 | 评论列表、目录导航、权限矩阵、版本信息 |
| 不允许 | 只有列表页无编辑器框架；评论面板用原生 HTML |

### 9.6 Sheets / Labs

| 维度 | 规范 |
| --- | --- |
| 布局 | 表格网格（`SheetsGrid`，白名单）+ 公式栏 + AI/Labs 侧栏 + 图表和批注 |
| 图表 | 统一通过 `V211Chart` 渲染，不散落 `echarts.init()` |
| 右栏 | AI 洞察、批注、保护区域、图表配置 |
| API 缺口 | 单元格批注列表、保护区域定义、图表配置详情 → 缺失展示 `NEmpty` |

### 9.7 Pass

| 维度 | 规范 |
| --- | --- |
| 布局 | 密码库列表（`V211EntityList`）+ 详情卡（`NCard`）+ 风险统计（`V211MiniChart` gauge）|
| 操作 | 创建/编辑密钥（`NModal`）、安全共享（`NDrawer`）|
| 右栏 | 密钥详情、安全评分、共享成员、轮换建议 |
| API 缺口 | 轮换建议、共享成员明细、设备信任度 → 降级展示风险评分或空态 |

### 9.8 Collaboration

| 维度 | 规范 |
| --- | --- |
| 布局 | 项目列表（`NDataTable`）+ 任务流（`NList` + `NTimeline`）+ 成员 + 评论 + 右侧协同动态 |
| 右栏 | 成员、任务活动、评论、关联文件 |
| 不允许 | 只有概览卡片无项目/任务详情 |

### 9.9 Command Center

| 维度 | 规范 |
| --- | --- |
| 布局 | 命令表（`NDataTable`）+ 运行状态 + 终端日志（`NLog` 或自定义日志容器）+ 工作流摘要 |
| 右栏 | 当前命令运行、日志、风险、审批和审计摘要 |
| 不允许 | 执行面板单薄无日志；命令表无分页和筛选 |

### 9.10 Notifications

| 维度 | 规范 |
| --- | --- |
| 布局 | 通知列表（`V211EntityList`）+ 规则管理（`NDataTable`）+ 统计环（`V211MiniChart` pie）+ 分组 + 右侧摘要 |
| 右栏 | 通知详情、规则、渠道、统计 |
| API 缺口 | 渠道发送成功率趋势、规则命中率历史 → 复用 analytics 日聚合 |

### 9.11 Admin

| 维度 | 规范 |
| --- | --- |
| 布局 | 治理 KPI（`V211MetricCard` 横排）+ 趋势图（`V211Chart`）+ 审计（`NDataTable`）+ 风险 + 表单 + 系统摘要 |
| 右栏 | 租户风险、审计动作、系统健康、治理队列 |
| API 缺口 | 治理队列、租户级容量预测 → 以 alerts + audit 组合派生 |

### 9.12 Settings

| 维度 | 规范 |
| --- | --- |
| 布局 | 账号安全中心式设置页：账号、安全、设备、偏好、存储等 tab 结构（`NTabs`）+ 各 tab 内 `NForm` |
| 右栏 | 账号安全、设备、存储、偏好和帮助入口 |
| API 缺口 | 存储用量明细按模块拆分、设备最近活跃地理信息、第三方集成连接健康 → 降级处理 |

## 10. ECharts 封装规范

### 10.1 统一入口

图表统一通过 `design-system/v211/chart/` 封装使用：

- `V211Chart.vue`：标准尺寸图表（趋势、柱状、饼图等）
- `V211MiniChart.vue`：紧凑 mini chart（KPI 卡片内嵌、列表行内趋势）

### 10.2 注册约束

只注册以下 ECharts 模块：`line`、`bar`、`pie`、`gauge`、`grid`、`tooltip`、`legend`。新增图表类型必须先更新本规范并复测包体积。

集中注册位于 `design-system/v211/chart/index.ts`：

```ts
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart, GaugeChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, BarChart, PieChart, GaugeChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])
```

### 10.3 封装职责

- 主题 token 从 `chart-palette.ts` 读取
- 空态展示 `NEmpty`
- 加载态展示 `NSkeleton`
- resize 通过 `ResizeObserver` 节流（200ms）
- 组件卸载时调用 `dispose()`
- 静态装饰和极小状态点仍可使用 CSS，但不能替代业务图表

### 10.4 禁止行为

- 模块内直接 `import 'echarts'` 或 `echarts.init()`
- 在组件内 hardcode 颜色序列
- 引入未注册的图表类型

## 11. API Gap 清单规范

### 11.1 登记格式

落到 `frontend-v2/tests/fixtures/v211-api-gap-registry.json`，由专用测试断言"模块右栏所声明字段在已登记接口或 gap 列表中能找到归宿"。

每条记录格式：

```json
{
  "module": "pass",
  "field": "rotationSuggestion",
  "ownerService": "pass-service",
  "priority": "P4",
  "fallback": "show 'No suggestion' empty state",
  "trackingTicket": null
}
```

### 11.2 已知缺口

| 模块 | 缺口字段 / 能力 | 归属服务 | 优先级 | 处置方案 |
| --- | --- | --- | --- | --- |
| Sheets / Labs | 单元格批注列表、保护区域定义、图表配置详情 | `sheets-service` | P3 | 增加只读聚合接口；未补齐展示 `NEmpty` |
| Pass | 轮换建议、共享成员明细、设备信任度 | `pass-service` | P4 | 复用 monitor 输出降级展示风险评分；成员明细以 vault.members 现有字段为底 |
| Settings | 存储用量明细按模块拆分、设备最近活跃地理信息、第三方集成连接健康 | `settings` + `drive` + `admin` | P5 | 存储拆分复用 drive.usage；地理信息缺失只展示 IP 与时间；集成健康先静态展示 |
| Notifications | 渠道发送成功率趋势、规则命中率历史 | `notifications` | P4 | 复用 analytics 现有日聚合 |
| Admin | 治理队列、租户级容量预测 | `admin` | P5 | 治理队列以 alerts + audit 组合派生；容量预测先展示 30 天用量趋势 |
| 首页 | 最近文件缩略图、最近编辑者头像 | `workspace` + `drive` | P2 | 暂用 Drive list 派生，缩略图缺失展示文件类型图标 |

### 11.3 处置原则

- 缺口未补齐的模块只能展示显式空态或已加载状态派生内容，不能使用伪数据
- `trackingTicket` 在创建 issue 后回填
- CI 校验：声明的右栏字段不在 service 类型 + gap 清单的并集时失败

## 12. 迁移策略

### 12.1 迁移路径

1. 先迁移全局 Shell、主题 token、共享业务组合组件、所有模块顶部工具栏和通用操作按钮，禁止新代码继续增加原生基础控件。
2. 再按首页和邮件验证三栏布局、右栏、KPI、列表密度和 ECharts 封装。
3. 后续模块按复用组件逐屏替换，门禁阈值按阶段收紧。
4. 富文本编辑器、spreadsheet 画布、canvas/grid 交互区可以保留必要原生结构，但必须进入白名单。

### 12.2 共存策略

- 不创建 `frontend-v3`，v2.1.1 仍在 `frontend-v2/` 继续。
- 通过路由级 feature flag `VITE_V211_SHELL` 决定 Shell 走新版还是旧版。
- 共享组件以 `V211*` 前缀命名，与现有 `design-system/components/*` 共存。
- `tokens.css` 与 `theme.ts` 增加 v211 token 后，旧组件仍能读取到原 token。
- 旧 `design-system/components/` 在 v2.1.1 期间冻结：不接受新增组件，只接受 bug 修复；P5 收口阶段做删除 PR。

### 12.3 Feature Flags

| 开关 | 作用 | 默认值 | 移除阶段 |
| --- | --- | --- | --- |
| `VITE_V211_SHELL` | 启用新 Shell 与右栏 | `false`（P1）→ `true`（P2 起 dev/staging）→ `true`（P3 起 prod） | P6 删 |
| `VITE_V211_DEBUG_GRID` | 显示设计稿对齐辅助网格（仅本地） | `false` | P6 删 |
| `localStorage: v211.right-panel.collapsed.<breakpoint>` | 持久化右栏折叠状态 | 见 §13.3 | 不删 |

开关都集中在 `frontend-v2/src/app/feature-flags.ts`，禁止 `import.meta.env.*` 散落在组件里。

### 12.4 Service 层兼容性

- 路由 path 不变；新版只接管视图层。
- service 层签名只允许加字段不允许改返回结构，否则旧版页面会在灰度期间崩。
- store 层允许新增 v211 专用 store（`store/modules/v211-*.ts`），但不修改既有 store 的 state 形状。

## 13. 可访问性、国际化、响应式与主题

### 13.1 可访问性 (a11y)

| 条目 | 要求 |
| --- | --- |
| 键盘可达 | Shell 顶部栏、左侧导航、右侧上下文面板、所有共享组件的主操作必须可通过 Tab/Shift-Tab 进入；模态/抽屉打开时焦点陷阱由 `NModal` / `NDrawer` 内置，不再自写 |
| 焦点可见 | 所有交互元素 `:focus-visible` 必须有 ≥2px outline，颜色取 `tokens.focusRing`；不允许 `outline: none` 没有补偿 |
| ARIA | 列表用 `role="listbox"`；表格沿用 `NDataTable` 默认；自研 grid (Sheets/Calendar) 必须声明 `role="grid"` 并提供 `aria-rowcount` / `aria-colcount` |
| 文案 | 所有 icon-only 按钮必须有 `aria-label` 或 `NTooltip` 包裹；颜色不能作为唯一信息载体（状态点旁需有文字或图标） |
| 对比度 | 主文本/背景 ≥ 4.5:1，次文本 ≥ 3:1，状态色块文本 ≥ 4.5:1；`tokens.css` 中所有色对必须用工具核验并记录在 `design-system/v211/contrast.md` |
| 动效 | 尊重 `prefers-reduced-motion`：动画 ≥ 200ms 的过渡必须在 reduced-motion 下降级为瞬时切换 |
| 验收 | `tests/accessibility-shell.test.mjs` 覆盖 v2.1.1 Shell + 共享组件，包含 Tab 顺序、`aria-*` 必填、icon-only 按钮 label 检查 |

### 13.2 国际化 (i18n)

- 三语基线沿用 `src/locales/index.ts`：`zh-CN`、`zh-TW`、`en`，不新增语言。
- v2.1.1 所有展示文案必须使用 `LocalizedText` 或 `TextLike`，禁止 Vue `<template>` 中出现裸中文。
- 新增的右栏内容、空态、错误态、骨架后回填提示都必须三语齐全；缺失语种必须 fallback 到 `zh-CN`，并在 `pnpm test` 中由 `locale-contract.test.mjs` 校验。
- 数字/日期使用 `Intl.NumberFormat` / `Intl.DateTimeFormat`，时区跟随用户偏好（`settings.profile.timezone`），不允许 `toLocaleString()` 不传 locale。
- 货币、容量、百分比展示的"千分位"和"小数位"约定写在 `design-system/v211/format.ts`，组件不得本地实现。

### 13.3 响应式与断点

| 断点 | 宽度 | Shell 行为 | 模块行为 |
| --- | --- | --- | --- |
| `xl` | ≥ 1440 | 三栏：侧栏展开 + 主内容 + 右侧上下文 | 默认密度，KPI 满 4 列 |
| `lg` | 1200–1439 | 三栏，右栏宽度收紧到 320px | KPI 3–4 列；表格列优先级降级 |
| `md` | 960–1199 | 右栏可折叠为抽屉触发器 | KPI 2 列；筛选区收起为下拉 |
| `sm` | 640–959 | 侧栏收为图标栏，右栏默认隐藏，需手动唤起 | KPI 1–2 列；表格转为卡片列表 |
| `xs` | < 640 | 移动端壳层：底部 `MobileTabBar`，顶部精简栏，右栏走全屏 `NDrawer` | 模块进入"单栏 + 抽屉"模式 |

- 断点常量集中在 `design-system/v211/breakpoints.ts`，组件通过 `useBreakpoint()` 读取，不允许各页面再写 `window.innerWidth` 判断。
- 右栏在 `md` 及以下默认折叠；展开/折叠状态通过 `useAppStore` 持久化，三端分别存档。

### 13.4 主题策略

- v2.1.1 默认 light 主题；dark 主题作为后置目标，在 P5 阶段统一通过 `NConfigProvider` 切 `darkTheme` 上线。
- token 双轨：`tokens.css` 提供 CSS 变量给布局/grid/canvas；`theme.ts` 把同一组语义 token 注入 Naive overrides。两者必须共用同一份语义键名（`brandPrimary`、`surface`、`textPrimary` 等），不允许命名漂移。
- 主色按设计稿调整为绿/青绿色系；调整必须在 `tokens.css` 和 `theme.ts` 完成后跑 `v21-design-token-contract.test.mjs`，确保未引入未注册 token。
- 自定义图表的颜色序列写在 `design-system/v211/chart-palette.ts`，ECharts 封装从该文件读取，不在组件内 hardcode。

## 14. 完成定义 (DoD) 与验收清单

### 14.1 PR 级 DoD

每个 v2.1.1 PR 合入前必须勾选：

- [ ] 涉及的 Vue 文件中，新增基础控件全部使用 Naive UI 或 v211 共享组件，未在白名单外引入 `<button>` / `<input>` / `<textarea>` / `<select>` / `<table>`。
- [ ] 涉及的页面三语文案齐全，无裸中文；`pnpm --dir frontend-v2 test -- locale-contract` 通过。
- [ ] 涉及的页面有对应 `.tmp/v211-ui-comparison/` 截图与设计源并排，附在 PR 描述。截图通过 `pnpm --dir frontend-v2 visual:qa` 生成，输出 `.tmp/v211-ui-comparison/overview-side-by-side.png` 作为人工验收证据。
- [ ] `pnpm --dir frontend-v2 typecheck` / `test` / `build` 全绿。
- [ ] 触及右栏的 PR 必须明确空态、加载态、错误态 3 个截图。
- [ ] 触及共享组件 props/emits 的 PR 必须更新 `design-system/v211/types.ts` 与 `v21-design-system-components.test.mjs` 快照。

### 14.2 模块级验收清单

每个模块合入主线前：

- [ ] 顶部工具栏使用 `V211ModuleToolbar` + `V211DataToolbar`，无原生 `button` / `input`。
- [ ] 主内容区至少使用 1 个 `V211SectionPanel` 或 `V211EntityList` 或 `NDataTable`。
- [ ] 右侧上下文面板使用 `V211RightInsightPanel`，且接入了第 8.2 节列出的对应 API；缺口模块明确展示 `NEmpty` 而不是占位假数据。
- [ ] 模块至少在 `xl`、`md`、`xs` 三个断点截图，Shell 不破版。
- [ ] 模块无单独 ECharts 初始化代码，统一通过 `V211Chart` / `V211MiniChart`。
- [ ] 模块的并排对比图通过 `pnpm --dir frontend-v2 visual:qa` 生成，入库 `.tmp/v211-ui-comparison/<module>-side-by-side.png`，并由人工签字。

### 14.3 阶段级 Exit Criteria

| 阶段 | Exit Criteria |
| --- | --- |
| P0 | 基线截图、原生控件计数、build 体积、Lighthouse 分数全部记录 |
| P1 | Shell + 主题 + 共享组件全部交付；门禁阈值达标；ECharts chunk ≤ 80 KB；API gap 清单产出 |
| P2 | 首页 + 邮件全部完成模块级验收；Lighthouse ≥ P0 baseline − 5 |
| P3 | 日历 / 云盘 / 文档 / Sheets 完成模块级验收；门禁达标（button ≤ 35、input ≤ 6、textarea ≤ 1、select ≤ 1）；P3 API gap 全部关闭或显式降级 |
| P4 | Pass / Collaboration / Command Center / Notifications 完成模块级验收；门禁达标（button ≤ 15、input ≤ 3、textarea = 0、select ≤ 1） |
| P5 | Admin / Settings 完成；非白名单原生基础控件归零；dark 主题落地（如纳入） |
| P6 | 全量并排截图签字；`v211-visual-parity-contract.test.mjs` 全部通过；性能/产物体积复测达标 |

## 15. 目录结构与命名约定

### 15.1 目录结构

```
frontend-v2/src/design-system/v211/
├── index.ts                 # barrel：仅暴露 V211* 组件与公共类型
├── types.ts                 # 共享 props/emits 类型
├── tokens.css               # CSS 变量（布局/grid 用）
├── theme.ts                 # Naive overrides 工厂
├── branding.ts              # 品牌资源集中管理
├── breakpoints.ts           # 响应式断点常量与 useBreakpoint
├── format.ts                # 数字/日期/容量格式化
├── chart-palette.ts         # ECharts 色板
├── chart/
│   ├── index.ts             # echarts.use(...) 集中注册
│   ├── V211Chart.vue
│   └── V211MiniChart.vue
└── components/
    ├── V211AppShell.vue
    ├── V211ModuleToolbar.vue
    ├── V211DataToolbar.vue
    ├── V211SectionPanel.vue
    ├── V211MetricCard.vue
    ├── V211EntityList.vue
    ├── V211RightInsightPanel.vue
    ├── V211ActionBar.vue
    └── V211StatusTag.vue
```

### 15.2 命名约定

- 组件：`V211XxxYyy.vue`，PascalCase，禁止缩写（`V211RP.vue` 不允许）。
- token：`brandPrimary`、`surface`、`textPrimary` 等语义键；不允许 `green01`、`blue02` 这类视觉命名。
- store：`useV211ShellStore`、`useV211RightPanelStore`，与既有 store 不混。
- feature flag：`VITE_V211_*` 前缀。
- 截图产物：`.tmp/v211-ui-comparison/<module>-side-by-side.png`、`<module>-current.png`、`<module>-design.png` 三件套。

### 15.3 测试 Fixtures

集中放 `frontend-v2/tests/fixtures/`：

- `v211-design-source.json` — 模块与设计源映射
- `v211-native-control-allowlist.json` — 原生控件白名单
- `v211-api-gap-registry.json` — API 缺口注册
- `v211-visual-parity-baseline.json` — 截图哈希/坐标基线

### 15.4 导入约束

- 模块代码只从 `@/design-system/v211` 引入；禁止深路径 `@/design-system/v211/components/V211MetricCard.vue`。
- 旧 `design-system/components/` 在 v2.1.1 期间冻结：不接受新增组件，只接受 bug 修复。
- ECharts 只能从 `@/design-system/v211` 的 chart barrel 导入，其他位置不允许 `import 'echarts'` 或 `echarts.init`。
