# MMMail v2.1.1 UI 设计一致性重构方案

## 1. 现状诊断

本次已启动并检查当前运行环境：

- 后端：`http://127.0.0.1:8080/actuator/health`，状态 `UP`
- 前端：`http://127.0.0.1:5174`，状态 `200 OK`
- 当前实现截图：`.tmp/v21-browser-visual-qa/`
- 设计对照图：`.tmp/v211-ui-comparison/overview-side-by-side.png`

对比结论：v2.1 当前 UI 与 `docs/MMMail/UI` 设计稿不是小范围偏差，而是整体视觉系统不一致。当前 v2.1 更像“通用管理后台骨架”，设计稿是“高密度协作套件工作台”。v2.1.1 应定义为一次 UI 视觉与组件体系重构，不应继续用零散 CSS 修补。

## 2. 设计源

v2.1.1 以 `docs/MMMail/UI` 为最高视觉来源，优先使用每个模块的概览图：

| 模块 | 设计源 |
| --- | --- |
| 首页 | `docs/MMMail/UI/首页/工作台-设计概览.png` |
| 邮件 | `docs/MMMail/UI/邮件/邮件-设计概览.png` |
| 日历 | `docs/MMMail/UI/日历/日历概览.png` |
| 云盘 | `docs/MMMail/UI/云盘/云盘概览.png` |
| 文档 | `docs/MMMail/UI/文档/文档概览.png` |
| Sheets / Labs | `docs/MMMail/UI/Sheets和labs/表格概览.png` |
| Pass | `docs/MMMail/UI/Pass/Pass概览.png` |
| Collaboration | `docs/MMMail/UI/Collaboration/协作概览.png` |
| Command Center | `docs/MMMail/UI/CommandCenter/命令概览.png` |
| Notifications | `docs/MMMail/UI/Notifications/通知概览.png` |
| Admin | `docs/MMMail/UI/Admin/管理后台.png` |
| Settings | `docs/MMMail/UI/Setting/设置概览.png` |

品牌文字继续使用 MMMail，不照搬设计稿中的 Nexa Workspace 等占位品牌。

## 3. 当前主要偏差

| 模块 | 当前偏差 | v2.1.1 目标 |
| --- | --- | --- |
| 全局壳层 | 当前左侧导航宽、右侧上下文面板空白占位明显，顶部栏和设计稿比例不一致 | 使用设计稿的窄侧栏、紧凑顶部栏、右侧真实上下文面板 |
| 首页 | 当前卡片稀疏，缺少 KPI、今日日程、最近文件、Command Center 摘要等复合信息密度 | 还原设计稿工作台：KPI 横排、日程列表、最近文件、快捷入口、右侧动态 |
| 邮件 | 当前三栏结构粗糙，列表和阅读区密度、工具栏、写信抽屉与设计差异大 | 还原文件夹栏、消息列表、阅读区、写信面板、附件条和安全提示 |
| 日历 | 当前网格空白大，月历、资源、事件详情右栏层级不足 | 还原迷你月历、周视图网格、事件色块、资源/冲突/表单右栏 |
| 云盘 | 当前文件区数据稀疏，右侧预览和底部共享/安全信息缺失 | 还原文件列表、预览卡、共享状态、版本和安全摘要 |
| 文档 | 当前主要是列表页，缺少编辑器布局和右侧评论/权限面板的视觉完成度 | 还原文档编辑器、目录、评论、权限、版本信息 |
| Sheets / Labs | 当前列表页替代了真实表格体验 | 还原表格网格、公式栏、AI/Labs 侧栏、图表和批注 |
| Pass | 当前 vault 内容不足，缺少设计稿中的详情密度、风险环、设备/安全建议 | 还原密码列表、详情卡、风险统计、共享与安全操作 |
| Collaboration | 当前只是概览卡片，缺少项目列表、任务流、成员、评论和右侧协同动态 | 还原项目/任务/评论/活动混合工作台 |
| Command Center | 当前执行面板单薄，缺少设计稿中的命令表、风险、日志和批量操作结构 | 还原命令表、运行状态、终端日志、工作流摘要 |
| Notifications | 当前表格样式偏通用，缺少图标、分组、统计和右侧分析 | 还原通知列表、规则、统计环、分组和右侧摘要 |
| Admin | 当前是通用 Overview，和设计稿管理后台的数据密度、图表和多面板差距大 | 还原治理 KPI、趋势图、审计、风险、表单和系统摘要 |
| Settings | 当前表单信息少，设计稿有账号、安全、设备、偏好、存储等更完整结构 | 还原账号安全中心式设置页 |

## 4. Naive UI 使用约束

v2.1.1 的 UI 基线：

- 所有基础交互组件必须来自 Naive UI，不再直接写原生 `button`、`input`、`textarea`、`select`、`table`。
- 自研组件只能作为 Naive UI 组件组合封装，例如 `V211MetricCard` 可以内部使用 `NCard`、`NStatistic`、`NProgress`。
- 原 `surface-card`、`metric-chip`、自定义 `DataTable`、自定义 `Modal`、自定义 `Drawer` 必须迁移为 Naive UI 组合。
- `NConfigProvider` 统一注入 v2.1.1 theme overrides，不再通过大量页面级 CSS 修正视觉。
- 页面 CSS 只负责布局、业务画布和少量 domain-specific grid，不负责重写按钮、输入框、表格、弹窗基础视觉。

当前代码清点结果：

| 指标 | 当前数量 |
| --- | ---: |
| 原生 `button` | 176 |
| 原生 `input` | 22 |
| 原生 `textarea` | 4 |
| 原生 `select` | 4 |
| 原生 `table` | 1 |
| 当前使用到的 Naive 标签 | `n-button`、`n-card`、`n-config-provider`、`n-dialog-provider`、`n-input`、`n-message-provider`、`n-modal`、`n-notification-provider`、`n-step`、`n-steps`、`n-tag` |

这说明当前项目只是局部接入 Naive UI，远没有达到“所有 UI 组件使用 Naive UI”的要求。

## 5. Naive UI 组件映射

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
| 日历基础控件 | `NCalendar`、`NDatePicker`、`NTimePicker` |

## 6. Naive UI 没有的组件清单

以下不是 Naive UI 的现成组件，需要在 v2.1.1 中作为业务组合组件实现，但内部必须使用 Naive UI 基础组件：

| 缺失组件 | 用途 | 实现方式 |
| --- | --- | --- |
| `V211AppShell` | 设计稿级应用框架、顶部栏、左侧导航、右侧上下文栏 | `NLayout` + `NMenu` + `NDropdown` + `NAvatar` |
| `V211RightInsightPanel` | 设计稿右侧通知/助手/详情面板 | `NCard` + `NTabs` + `NList` |
| `V211MetricCard` | 首页和模块 KPI 卡 | `NCard` + `NStatistic` + `NProgress` |
| `V211ModuleToolbar` | 模块标题、搜索、筛选、创建按钮 | `NPageHeader` + `NInput` + `NButton` + `NDropdown` |
| `V211MailWorkbench` | 邮件文件夹、列表、阅读三栏组合 | `NLayout` + `NMenu` + `NList` + `NCard` |
| `V211MailComposer` | 写信侧栏/抽屉、附件、安全提示 | `NDrawer` + `NForm` + `NInput` + `NUpload` |
| `V211CalendarGrid` | 周视图/日视图时间网格、事件色块 | 自定义 CSS grid + `NCard` + `NPopover` + `NDrawer` |
| `V211DriveExplorer` | 文件树、文件表、预览详情、分享面板 | `NTree` + `NDataTable` + `NDrawer` + `NUpload` |
| `V211DocsEditorFrame` | 文档编辑画布、目录、评论、权限侧栏 | 自定义编辑区 + `NTabs` + `NList` + `NDrawer` |
| `V211SheetsGrid` | 类 spreadsheet 网格、公式栏、选区、批注 | 自定义 grid renderer + `NInput` + `NPopover` |
| `V211PassVault` | 密码库列表、详情、安全评分、风险面板 | `NList` + `NCard` + `NProgress` + `NModal` |
| `V211CollaborationBoard` | 项目、任务、评论、成员活动组合 | `NDataTable` + `NList` + `NTimeline` |
| `V211CommandConsole` | 命令执行、工作流、终端日志 | `NDataTable` + `NLog` 或自定义日志容器 |
| `V211NotificationCenter` | 通知列表、规则、渠道、统计 | `NDataTable` + `NList` + `NForm` |
| `V211AdminDashboard` | 管理后台多 KPI、趋势图、审计、策略 | `NGrid` + `NCard` + `V211Chart` |
| `V211MiniChart` | 趋势折线、环形统计、柱状 mini chart | ECharts 按需封装，禁止模块内各自初始化图表 |
| `V211PermissionMatrix` | Docs/Admin 权限矩阵 | `NDataTable` + `NCheckbox` |
| `V211AuditTimeline` | 审计和活动流 | `NTimeline` + `NTag` |

图表库选型确定为 ECharts。v2.1.1 统一通过 `V211Chart` / `V211MiniChart` 封装使用，Admin、首页 KPI、Notifications、Command Center、Pass 风险统计、云盘容量趋势等模块不得直接散落 `echarts.init()`。封装层负责主题 token、按需注册图表类型、空态、加载态、resize 和销毁。只注册 line、bar、pie、gauge、grid、tooltip、legend；新增图表类型必须先更新本方案并复测包体积。静态装饰和极小状态点仍可使用 CSS，但不能替代业务图表。

## 7. v2.1.1 架构方案

### 7.1 第一层：Naive UI 主题和 token

建立 `frontend-v2/src/design-system/v211/`：

- `theme.ts`：Naive UI theme overrides，集中定义主色、圆角、阴影、字号、表格高度。
- `tokens.css`：只保留布局变量和设计稿专用间距，不覆盖 Naive 基础控件。
- `component-map.md` 或测试文件：锁定所有基础控件必须使用 Naive UI。

视觉目标：

- 主色从当前蓝紫倾向改为设计稿中的绿色/青绿色主色。
- 背景使用浅灰工作台底色，卡片白底，边框轻量。
- 默认控件圆角、表格行高、按钮高度全部收敛到设计稿的紧凑企业应用风格。

### 7.2 第二层：全局壳层重构

重构以下文件：

- `frontend-v2/src/layouts/base-layout/BaseLayout.vue`
- `frontend-v2/src/layouts/modules/ShellSideNav.vue`
- `frontend-v2/src/layouts/modules/ShellTopBar.vue`
- `frontend-v2/src/layouts/modules/ContextPanel.vue`
- `frontend-v2/src/layouts/modules/MobileTabBar.vue`

目标：

- 用 `NLayout` / `NMenu` / `NDropdown` / `NAvatar` 替代原生按钮和自定义导航。
- 还原设计稿左侧窄导航、底部容量/状态卡、顶部搜索和右侧用户区。
- 右侧上下文面板必须展示真实内容，不允许大面积空白占位。

右侧上下文面板内容策略：

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

右栏 API 覆盖状态：

| 模块 | 当前可用 API / 状态 | v2.1.1 处理 |
| --- | --- | --- |
| 首页 | `workspace.ts` 已有 summary、activity、tasks | P2 可直接接入，缺少最近文件时复用 Drive list |
| 邮件 | `mail.ts` 已有 folder、detail、trust、rules、bulk action | P2 可直接接入，右栏基于当前选中邮件详情 |
| 日历 | `calendar.ts` 已有 events、agenda、availability、resources、settings | P3 可直接接入，冲突提示优先用 availability |
| 云盘 | `drive.ts` 已有 items、usage、shares、versions | P3 可直接接入，预览内容不足时展示元数据和版本 |
| 文档 | `docs.ts` 已有 notes、detail、comments、versions、share | P3 可直接接入，目录可由前端从正文结构派生 |
| Sheets / Labs | `sheets.ts` 已有 workbook、import、cleaning rules、insights | P3 部分接入；批注、保护区域和图表配置先记为 API 缺口 |
| Pass | `pass.ts` 已有 items、vaults、monitor、secure links、aliases | P4 部分接入；选中密钥详情、轮换建议和成员明细需后端补齐或由列表摘要降级为空态 |
| Collaboration | `collaboration.ts` 已有 projects、tasks、comments、activity | P4 可直接接入 |
| Command Center | `command-center.ts` 已有 commands、runs、workflows、audit | P4 可直接接入 |
| Notifications | `notifications.ts` 已有 list、rules、subscriptions、templates、analytics | P4 可直接接入 |
| Admin | `admin.ts` 已有 summary、users、roles、domains、policies、audit、alerts、system、risk | P5 可直接接入，治理队列不足时用 alerts/audit 组合 |
| Settings | `settings.ts` 已有 profile、security、devices、notifications、integrations、audit | P5 部分接入；存储详情可复用 Drive usage，偏好缺口进入 API gap 清单 |

v2.1.1 不重做后端商业闭环，但允许补充只读聚合接口或字段补齐，前提是基于现有数据表和服务能力，不引入支付、订阅、发票、真实扣款等商业运行时逻辑。P1 结束时必须生成右栏 API gap 清单；缺口未补齐的模块只能展示显式空态或已加载状态派生内容，不能使用伪数据。

### 7.3 第三层：共享业务组合组件

新增或重写：

| 组件 | 服务模块 | P1 验收标准 |
| --- | --- | --- |
| `V211MetricCard` | 首页、Admin、Pass、Notifications、Command Center | 支持 title、value、trend、status、loading、chart slot；至少首页和 Admin 复用 |
| `V211SectionPanel` | 全模块页面区块 | 统一 title、description、actions、loading、empty、error 区域；替代自定义 section card |
| `V211DataToolbar` | 邮件、云盘、Docs、Sheets、Admin、Notifications | 统一搜索、筛选、创建、批量操作入口；内部只用 `NInput`、`NButton`、`NDropdown` |
| `V211EntityList` | 邮件、Pass、Notifications、Collaboration | 支持 avatar、title、meta、tags、selected、actions；替代散落列表样式 |
| `V211RightInsightPanel` | 全模块右栏 | 支持 tabs、loading、empty、error、module context；首页和邮件先接入 |
| `V211ModuleSummary` | 首页、云盘、Admin、Settings | 提供紧凑模块摘要、关键状态和快捷操作，不做营销式 hero |
| `V211ActionBar` | 邮件、云盘、Docs、Pass、Command Center | 支持批量选择、主操作、次操作、危险操作确认 |
| `V211StatusTag` | 全模块状态展示 | 统一 success、warning、error、info、neutral token 和文案映射 |

所有组件内部只能使用 Naive UI 组件和业务布局容器。P1 完成时，每个共享组件必须至少有 1 个真实页面接入，`V211MetricCard`、`V211DataToolbar`、`V211RightInsightPanel` 必须至少有 2 个接入点，否则不能算作 Shell/共享组件迁移完成。

### 7.4 第四层：模块逐屏对齐

实施顺序按设计差距和公共组件复用价值排序：

1. 首页工作台：先打通全局壳层和 KPI/列表/右栏模式。
2. 邮件：三栏密度、写信抽屉、阅读区和附件安全状态。
3. 日历：迷你月历、周视图网格、事件抽屉。
4. 云盘：文件表、预览详情、分享/权限面板。
5. 文档和 Sheets：编辑器框架、右侧协作/AI 面板、表格画布。
6. Pass：vault 列表、详情、风险统计。
7. Collaboration / Command Center / Notifications：运营类密集表格和活动流。
8. Admin / Settings：治理后台和账号安全中心。

### 7.5 迁移策略

v2.1.1 不采用一次性清零 176 个原生按钮的方式，避免大范围重构失控。迁移路径为：

1. 先迁移全局 Shell、主题 token、共享业务组合组件、所有模块顶部工具栏和通用操作按钮，禁止新代码继续增加原生基础控件。
2. 再按首页和邮件验证三栏布局、右栏、KPI、列表密度和 ECharts 封装。
3. 后续模块按复用组件逐屏替换，门禁阈值按阶段收紧。
4. 富文本编辑器、spreadsheet 画布、canvas/grid 交互区可以保留必要原生结构，但必须进入白名单。

当前 `frontend-v2/src` 内 `<button>` 分布验证结果为 `layouts 17`、`design-system 21`、`views 137`、合计 `175`；`frontend-v2` 全量扫描基线仍按 `176` 记录。因此 P1 不能只靠 Shell 迁移完成大幅下降，必须覆盖共享组件和模块顶部工具栏；P1 阈值按 `button <= 120` 设定，P2 后再进入核心页面级收紧。`input`、`textarea`、`select`、`table` 的阶段目标以第 8 节门禁收紧表为准。

白名单由 `frontend-v2/tests/fixtures/v211-native-control-allowlist.json` 维护。每条记录必须包含文件路径、标签类型、保留原因、预计移除阶段和负责人；CI 只跳过白名单命中的元素。新增白名单必须同步更新本方案或测试说明，不能在测试里临时硬编码跳过。

白名单条目格式：

```json
[
  {
    "file": "frontend-v2/src/views/sheets/SheetsGrid.vue",
    "tag": "input",
    "reason": "spreadsheet 单元格编辑器需要原生输入焦点控制",
    "removeBy": "P5",
    "owner": "v2.1.1-ui"
  }
]
```

## 8. 质量门禁

v2.1.1 不再只用 DOM 选择器证明页面存在，需要新增视觉一致性门禁：

1. `v211-naive-ui-contract.test.mjs`
   - 阶段性禁止新增原生 `button`、`input`、`textarea`、`select`、`table`。
   - P1 起对新增原生控件失败；P2 起对超过阶段阈值失败；P5 起非白名单原生基础控件必须清零。
   - 例外只允许富文本、spreadsheet、canvas/grid 交互区等业务画布内部结构，并必须白名单说明。

2. `v211-design-source-contract.test.mjs`
   - 每个模块必须声明对应 `docs/MMMail/UI` 源图。
   - 每个模块必须有当前实现截图和设计源配对。

3. `v211-visual-parity-contract.test.mjs`
   - 锁定左侧导航宽度、顶部栏高度、右侧上下文面板存在性、卡片密度、主色 token。

4. 浏览器截图验证
   - 继续使用 `pnpm --dir frontend-v2 visual:qa`。
   - 输出 `.tmp/v211-ui-comparison/overview-side-by-side.png` 作为人工验收证据。

5. 常规验证
   - `pnpm --dir frontend-v2 test`
   - `pnpm --dir frontend-v2 typecheck`
   - `pnpm --dir frontend-v2 build`

门禁收紧节奏：

| 阶段 | 原生控件门禁 | 目标阈值 |
| --- | --- | --- |
| P0 | 只记录当前基线 | `button 176`、`input 22`、`textarea 4`、`select 4`、`table 1` |
| P1 | 禁止新增，Shell、共享组件和模块顶部工具栏必须迁移 | `button <= 120`、`input <= 16`、`textarea <= 3`、`select <= 3`、`table = 0` |
| P2 | 首页和邮件进入失败门禁 | `button <= 80`、`input <= 10`、`textarea <= 2`、`select <= 2` |
| P3 | 生产力模块进入失败门禁 | `button <= 35`、`input <= 6`、`textarea <= 1`、`select <= 1` |
| P4 | 安全和运营模块进入失败门禁 | `button <= 15`、`input <= 3`、`textarea = 0`、`select <= 1` |
| P5 | 全量模块收口 | 非白名单基础控件为 0 |

设计稿未覆盖交互状态时，默认使用 Naive UI 原生状态和 v2.1.1 theme token。加载态使用 `NSkeleton` / `NSpin`，空态使用 `NEmpty`，错误态使用 `NResult` / `NAlert`，危险操作使用 `NPopconfirm`，复杂提示使用 `NTooltip` / `NPopover`。hover、focus、disabled、selected 状态必须可见，不能用透明或无反馈状态替代。

性能基线：

- P0 记录当前 `pnpm --dir frontend-v2 build` 产物体积作为 baseline。
- P1 完成后，主入口 JS/CSS gzip 体积不得超过 P0 baseline 的 115%。
- ECharts 不进入全局 Shell 首屏同步包，必须通过共享图表组件按需加载或路由级拆分。
- ECharts 独立 chunk gzip 预算为 `<= 80 KB`；若超过预算，必须减少注册图表类型或拆分按路由加载。
- P2 起以首页和邮件页的桌面端 Lighthouse Performance 分数作为 baseline；测量条件统一为 Lighthouse CI desktop preset，视口 `1350x940`，无网络节流，CPU slowdown `4x`，冷启动登录态；后续模块不得低于两者较低值 5 分以上，首页和邮件自身也不得回退超过 5 分。

## 9. 分阶段交付计划

P1-P2 是本轮需要锁定的近期目标周期；P3-P6 先给工作量区间，在 P2 完成后重新评估。

| 阶段 | 目标周期 / 工作量 | 范围 | 完成标准 |
| --- | --- | --- | --- |
| P0 基线冻结 | 0.5-1 个开发日 | 保存当前对照图、建立 v2.1.1 文档和门禁 | 能明确证明当前 v2.1 与设计不一致 |
| P1 Naive UI 基础迁移 | 3-5 个开发日，目标 1 周内 | 主题、Shell、共享组件、模块顶部工具栏、通用操作按钮迁移，确定 ECharts 封装 | 原生基础控件计数下降到阶段阈值内，Shell 接近设计稿，图表入口统一 |
| P2 首页和邮件 | 5-8 个开发日，目标 1-1.5 周 | 首页工作台、邮件三栏、写信抽屉、右栏真实内容 | 两个核心页面与设计稿一眼一致，右栏不再空白 |
| P3 日历、云盘、文档、Sheets | 10-15 个开发日，P2 后复估 | 核心协作生产力模块 | 主要工作区密度和右栏交互一致 |
| P4 Pass、协作、命令、通知 | 8-12 个开发日，P2 后复估 | 安全和运营模块 | 列表、状态、活动流和执行面板一致 |
| P5 Admin、Settings、公共页 | 6-10 个开发日，P2 后复估 | 治理和设置收口 | 管理后台和设置页接近设计稿，非白名单原生控件清零 |
| P6 视觉验收 | 2-4 个开发日 | 全量截图、人工对比、测试门禁 | 设计偏差降为局部文案/数据差异，门禁通过 |

## 10. 非目标

- 不在 v2.1.1 中重做后端商业闭环。
- 不把设计稿中的 Nexa、Acme 等占位品牌硬编码到产品。
- 不引入伪数据来掩盖 API 不足；页面可以展示真实空态，但视觉结构必须完整。
- 不继续维护两套组件体系；v2.1.1 后基础 UI 以 Naive UI 为唯一来源。

## 11. 剩余风险与已缓解项

剩余风险：

- Naive UI 没有 spreadsheet、mail thread、calendar time grid 这类业务级组件，需要明确自研组合组件边界。
- ECharts 已确定为图表库，但按需加载和 tree-shaking 效果要在 P1 build 后用产物体积验证。
- 右栏 API 已有较高覆盖，但 Sheets、Pass、Settings 仍有字段或详情缺口，P1 必须输出 API gap 清单并标注是否纳入 v2.1.1。
- 如果只替换按钮和输入框，不重构 Shell 和模块布局，视觉仍不会接近设计稿。
- 当前视觉一致性风险登记表曾把明显偏差标为 aligned，v2.1.1 必须改为基于并排截图和人工验收。
- P3-P6 工作量估算依赖 P1-P2 的组件复用效果，P2 完成后必须重新评估周期和阈值。
- 右侧上下文面板内容策略已定义，但真实 API 覆盖不足时仍可能出现空态过多，需要在模块实现阶段暴露缺口。

已缓解项：

- 图表库选型已确定为 ECharts，并限定统一封装、注册类型和 chunk 预算。
- 右侧上下文面板已定义按模块的数据来源、加载态、空态和错误态规则。
- P1 原生控件阈值已基于实际代码分布验证，从不现实的 `button <= 80` 调整为 `button <= 120`，覆盖范围扩展到模块顶部工具栏。

## 12. 共享组件接口契约

v2.1.1 共享业务组件不允许"看着像就行"。每个 P1 共享组件必须在落地前固化 props、emits、slots、状态机和 a11y 契约，由 `v211-design-system-components.test.mjs` 锁定签名变更。

### 12.1 `V211MetricCard`

| 维度 | 契约 |
| --- | --- |
| props | `title: TextLike`、`value: number \| string`、`unit?: TextLike`、`delta?: { value: number, direction: 'up' \| 'down' \| 'flat', tone?: 'positive' \| 'negative' \| 'neutral' }`、`trend?: TrendPoint[]`、`status?: 'success' \| 'warning' \| 'danger' \| 'info' \| 'neutral'`、`loading?: boolean`、`empty?: boolean`、`error?: string \| null`、`onClick?: () => void` |
| slots | `default`(主体替换)、`extra`(右上操作)、`footer`(说明/链接) |
| 内部组件 | `NCard` + `NStatistic` + `NNumberAnimation` + `NProgress` + `V211MiniChart`(ECharts 封装) |
| 状态优先级 | `loading` > `error` > `empty` > 数据；不允许同时渲染骨架和数据 |
| a11y | 整卡可点时必须有 `role="button"` + `tabindex="0"` + `aria-label`；趋势/箭头同时输出语义文案而不是只靠颜色区分 |
| 不允许 | 直接接收 ECharts option；直接写原生 `button` 作为操作入口 |

### 12.2 `V211SectionPanel`

| 维度 | 契约 |
| --- | --- |
| props | `title: TextLike`、`description?: TextLike`、`bordered?: boolean`、`padding?: 'none' \| 'compact' \| 'default'`、`loading?: boolean`、`empty?: boolean`、`emptyText?: TextLike`、`error?: string \| null`、`actions?: PanelAction[]` |
| slots | `default`、`actions`(覆盖 props.actions)、`empty`、`error`、`footer` |
| 内部组件 | `NCard` + `NSpin` + `NEmpty` + `NResult` |
| 用途 | 替代分散在各 Vue 文件中的 `surface-card` / `panel-card` / `section-card` 自写样式 |
| 不允许 | 内部使用 `<section>` 自定义阴影/边框；与 `V211MetricCard` 重复职责 |

### 12.3 `V211DataToolbar`

| 维度 | 契约 |
| --- | --- |
| props | `searchPlaceholder?: TextLike`、`searchValue?: string`、`filters?: ToolbarFilter[]`、`primaryAction?: ToolbarAction`、`secondaryActions?: ToolbarAction[]`、`density?: 'compact' \| 'default'`、`disabled?: boolean` |
| emits | `update:searchValue`、`filterChange(filterId, value)`、`primary`、`secondary(actionId)` |
| 内部组件 | `NInput`(search)、`NSelect`/`NDatePicker`(filter)、`NButton`、`NDropdown`、`NSpace` |
| a11y | 搜索框 `aria-label`；筛选器使用 `NSelect` 内置 listbox；批量操作下拉键盘可达 |
| 不允许 | 任何原生 `button` / `input`；直接写 CSS flex 替代 `NSpace` |

### 12.4 `V211EntityList`

| 维度 | 契约 |
| --- | --- |
| props | `items: EntityListItem[]`、`selectedId?: string`、`selectable?: 'single' \| 'multiple' \| false`、`virtualScroll?: boolean`、`itemHeight?: number`、`loading?: boolean`、`empty?: boolean` |
| `EntityListItem` 形状 | `{ id, avatar?, title, meta?, tags?: StatusTag[], badge?, actions?: ItemAction[], disabled?, ariaLabel? }` |
| emits | `update:selectedId`、`select(id)`、`action(id, actionId)` |
| 内部组件 | `NList` + `NThing` + `NAvatar` + `NTag` + `NCheckbox`(批量) + `NVirtualList`(超过 200 行启用) |
| a11y | 列表 `role="listbox"`；多选时 `aria-multiselectable="true"`；每项 `aria-selected`；Tab 进入列表后用方向键移动 |
| 不允许 | 直接写 `<ul><li>` + 自定义 hover/selected 样式 |

### 12.5 `V211RightInsightPanel`

| 维度 | 契约 |
| --- | --- |
| props | `module: ModuleKey`、`tabs: InsightTab[]`、`activeTab?: string`、`loading?: boolean`、`error?: string \| null`、`collapsed?: boolean`、`width?: number` |
| slots | 每个 tab 一个 named slot；`empty`、`error` 兜底 slot |
| 内部组件 | `NCard` + `NTabs` + `NScrollbar` + `NSkeleton` + `NEmpty` + `NResult` |
| 数据契约 | 必须接受外部传入的 `loading`/`error`，不在组件内部发请求；保证父级模块 store 拥有数据所有权 |
| a11y | 折叠按钮 `aria-expanded`、`aria-controls`；切换 tab 后焦点回到 tab panel 起始 |
| 不允许 | 直接调用 service 层 fetch；用 `v-show` 隐藏后保留滚动条事件 |

### 12.6 `V211ModuleToolbar` / `V211ActionBar` / `V211StatusTag` / `V211MiniChart`

| 组件 | 关键契约 |
| --- | --- |
| `V211ModuleToolbar` | props: `title`、`breadcrumbs?`、`tabs?`、`actions?`、`density?`；内部 `NPageHeader` + `NBreadcrumb` + `NTabs` + `NButton`；不接管搜索（搜索归 `V211DataToolbar`） |
| `V211ActionBar` | props: `selectedCount`、`primaryAction`、`secondaryActions?`、`dangerActions?`；危险操作必须包 `NPopconfirm`；显示选择计数文案需走 i18n |
| `V211StatusTag` | props: `tone: 'success' \| 'warning' \| 'danger' \| 'info' \| 'neutral' \| 'brand'`、`label: TextLike`、`icon?`；颜色 token 来自 `tokens.css`，不允许直接写 hex |
| `V211MiniChart` | props: `type: 'line' \| 'bar' \| 'pie' \| 'gauge'`、`data`、`height?`、`loading?`；内部 lazy import ECharts 模块；卸载时调用 `dispose()`；resize 通过 `ResizeObserver` 节流 |

### 12.7 类型与导出约束

- 所有共享组件类型放在 `frontend-v2/src/design-system/v211/types.ts`，模块代码只能从 `@/design-system/v211` barrel 导入。
- `TextLike` 沿用现有 `src/locales/index.ts` 中的 `TextLike` 类型，不允许新建并行类型。
- 任何接收 `value` 字段并展示给用户的 props 必须接受 `TextLike`，不允许 `string` 直写硬编码中文。

## 13. 可访问性、国际化、响应式与主题

### 13.1 可访问性 (a11y)

| 条目 | 要求 |
| --- | --- |
| 键盘可达 | Shell 顶部栏、左侧导航、右侧上下文面板、所有共享组件的主操作必须可通过 Tab/Shift-Tab 进入；模态/抽屉打开时焦点陷阱由 `NModal` / `NDrawer` 内置，不再自写 |
| 焦点可见 | 所有交互元素 `:focus-visible` 必须有 ≥2px outline，颜色取 `tokens.focusRing`；不允许 `outline: none` 没有补偿 |
| ARIA | 列表用 `role="listbox"`；表格沿用 `NDataTable` 默认；自研 grid (Sheets/Calendar) 必须声明 `role="grid"` 并提供 `aria-rowcount` / `aria-colcount` |
| 文案 | 所有 icon-only 按钮必须有 `aria-label` 或 `NTooltip` 包裹；颜色不能作为唯一信息载体（状态点旁需有文字或图标） |
| 对比度 | 主文本/背景 ≥ 4.5:1，次文本 ≥ 3:1，状态色块文本 ≥ 4.5:1；`tokens.css` 中所有色对必须在 P1 提交前用工具核验并记录在 `design-system/v211/contrast.md` |
| 动效 | 尊重 `prefers-reduced-motion`：动画 ≥ 200ms 的过渡必须在 reduced-motion 下降级为瞬时切换 |
| 验收 | `tests/accessibility-shell.test.mjs` 扩展为覆盖 v2.1.1 Shell + 共享组件，包含 Tab 顺序、`aria-*` 必填、icon-only 按钮 label 检查 |

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
| `xs` | < 640 | 移动端壳层：底部 `MobileTabBar`，顶部精简栏，右栏走全屏 `NDrawer` | 模块进入"单栏 + 抽屉"模式，写信、事件、文件详情都用 `NDrawer` 替代右栏 |

- 断点常量集中在 `design-system/v211/breakpoints.ts`，组件通过 `useBreakpoint()` 读取，不允许各页面再写 `window.innerWidth` 判断。
- 右栏在 `md` 及以下默认折叠；展开/折叠状态通过 `useAppStore` 持久化，三端分别存档（不要用同一个 boolean 跨断点共用）。

### 13.4 主题策略

- v2.1.1 默认 light 主题；dark 主题作为 P1+ 后置目标，在 P5 阶段统一通过 `NConfigProvider` 切 `darkTheme` 上线。
- token 双轨：`tokens.css` 提供 CSS 变量给布局/grid/canvas；`naive-theme.ts` 把同一组语义 token 注入 Naive overrides。两者必须共用同一份语义键名（`brandPrimary`、`surface`、`textPrimary` 等），不允许命名漂移。
- 主色按设计稿调整为绿/青绿色系；调整必须在 `tokens.ts` 完成后跑 `v21-design-token-contract.test.mjs`，确保未引入未注册 token。
- 自定义图表的颜色序列写在 `design-system/v211/chart-palette.ts`，ECharts 封装从该文件读取，不在组件内 hardcode。

## 14. 完成定义 (DoD) 与验收清单

每个 PR、每个阶段、每个模块的"完成"必须能用清单检验，避免再次出现"标 aligned 实际未对齐"。

### 14.1 PR 级 DoD

每个 v2.1.1 PR 合入前必须勾选：

- [ ] 涉及的 Vue 文件中，新增基础控件全部使用 Naive UI 或 v211 共享组件，未在白名单外引入 `<button>` / `<input>` / `<textarea>` / `<select>` / `<table>`。
- [ ] 涉及的页面三语文案齐全，无裸中文；`pnpm --dir frontend-v2 test -- locale-contract` 通过。
- [ ] 涉及的页面有对应 `.tmp/v211-ui-comparison/` 截图与设计源并排，附在 PR 描述。
- [ ] `pnpm --dir frontend-v2 typecheck` / `test` / `build` 全绿。
- [ ] 触及右栏的 PR 必须明确空态、加载态、错误态 3 个截图。
- [ ] 触及共享组件 props/emits 的 PR 必须更新 `design-system/v211/types.ts` 与 `v21-design-system-components.test.mjs` 快照。

### 14.2 模块级验收清单

每个模块（首页 / 邮件 / 日历 / 云盘 / 文档 / Sheets / Pass / Collaboration / Command Center / Notifications / Admin / Settings）合入主线前：

- [ ] 顶部工具栏使用 `V211ModuleToolbar` + `V211DataToolbar`，无原生 `button` / `input`。
- [ ] 主内容区至少使用 1 个 `V211SectionPanel` 或 `V211EntityList` 或 `NDataTable`。
- [ ] 右侧上下文面板使用 `V211RightInsightPanel`，且接入了第 7.2 节列出的对应 API；缺口模块明确展示 `NEmpty` 而不是占位假数据。
- [ ] 模块至少在 `xl`、`md`、`xs` 三个断点截图，Shell 不破版。
- [ ] 模块无单独 ECharts 初始化代码，统一通过 `V211Chart` / `V211MiniChart`。
- [ ] 模块的并排对比图入库 `.tmp/v211-ui-comparison/<module>-side-by-side.png`，并由人工签字。

### 14.3 阶段级 Exit Criteria

| 阶段 | Exit Criteria |
| --- | --- |
| P0 | 基线截图、原生控件计数、build 体积、Lighthouse 分数全部记录，`v21-ui-parity-plan-2.1.1.md` 锁定 v1.0 |
| P1 | Shell + 主题 + 共享组件全部交付；`button <= 120`、`input <= 16`、`textarea <= 3`、`select <= 3`、`table = 0`；ECharts 独立 chunk gzip ≤ 80 KB；API gap 清单产出 |
| P2 | 首页 + 邮件全部完成模块级验收；桌面 Lighthouse 分数 ≥ P0 baseline − 5 |
| P3 | 日历 / 云盘 / 文档 / Sheets 完成模块级验收；右栏 API gap 中标记为 P3 的项目全部关闭或显式降级 |
| P4 | Pass / Collaboration / Command Center / Notifications 完成模块级验收 |
| P5 | Admin / Settings 完成；非白名单原生基础控件归零；dark 主题落地（如纳入） |
| P6 | 全量并排截图签字；`v211-visual-parity-contract.test.mjs` 全部通过；性能/产物体积复测达标 |

## 15. API gap 清单（初始登记）

P1 结束前必须完成下表的填空，并以 JSON 形式落到 `frontend-v2/tests/fixtures/v211-api-gap-registry.json`，由专用测试断言"模块右栏所声明字段在已登记接口或 gap 列表中能找到归宿"。

| 模块 | 缺口字段 / 能力 | 归属服务 | 优先级 | 处置方案（v2.1.1 内） |
| --- | --- | --- | --- | --- |
| Sheets / Labs | 单元格批注列表、保护区域定义、图表配置详情 | `sheets.ts` + 后端 `sheets-service` | P3 | 增加只读聚合接口 `GET /sheets/{id}/insights`；批注若未补齐则展示 `NEmpty` |
| Pass | 单条密钥详情中的"轮换建议"、共享成员明细、设备信任度 | `pass.ts` + `pass-service` | P4 | 复用 `monitor` 输出降级展示风险评分；成员明细以 `vault.members` 现有字段为底，缺失字段进 gap |
| Settings | 存储用量明细按模块拆分、设备最近活跃地理信息、第三方集成的连接健康 | `settings.ts` + `drive.ts` + `admin.ts` | P5 | 存储拆分复用 `drive.usage`；地理信息缺失时只展示 IP 与时间；集成健康先静态展示 |
| Notifications | 渠道发送成功率趋势、规则命中率历史 | `notifications.ts` | P4 | 复用 `analytics` 现有日聚合；如无小时粒度先用日维度展示 |
| Admin | 治理队列（合规复核、风险审批）、租户级容量预测 | `admin.ts` | P5 | 治理队列以 `alerts` + `audit` 组合派生；容量预测先展示 30 天用量趋势 |
| 首页 | "最近文件"卡片字段（缩略图、最近编辑者头像） | `workspace.ts` + `drive.ts` | P2 | 暂用 Drive list 派生，缩略图缺失展示文件类型图标 |

清单字段约定：

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

`trackingTicket` 在创建 issue 后回填；CI 校验"声明的右栏字段不在 service 类型 + gap 清单的并集时失败"。

## 16. 迁移与回滚策略

### 16.1 共存策略

- 不创建 `frontend-v3`，v2.1.1 仍在 `frontend-v2/` 继续；通过路由级 feature flag `enableV211Shell` 决定 Shell 走新版还是旧版。
- 共享组件以 `V211*` 前缀命名，与现有 `design-system/components/*` 共存；不做"改名替换"，保留旧组件直到对应模块全部迁移完毕，再做一次清理 PR。
- `tokens.css` 与 `naive-theme.ts` 增加 v211 token 后，旧组件仍能读取到原 token；P1 不删任何旧 token，P5 模块全部迁移后再做 token 清理。

### 16.2 灰度与开关

| 开关 | 作用 | 默认值 | 移除阶段 |
| --- | --- | --- | --- |
| `VITE_V211_SHELL` | 启用新 Shell 与右栏 | `false`（P1）→ `true`（P2 起 dev/staging）→ `true`（P3 起 prod） | P6 删 |
| `VITE_V211_DEBUG_GRID` | 显示设计稿对齐辅助网格（仅本地） | `false` | P6 删 |
| `localStorage: v211.right-panel.collapsed.<breakpoint>` | 持久化右栏折叠状态 | 见 13.3 | 不删 |

开关都集中在 `frontend-v2/src/app/feature-flags.ts`，禁止 `import.meta.env.*` 散落在组件里。

### 16.3 回滚条件

回滚到 v2.1（关闭 `VITE_V211_SHELL`）的触发条件：

- `pnpm test` 在 main 上失败 ≥ 2 次连续提交。
- 桌面 Lighthouse Performance 较 P0 baseline 跌幅超过 10 分。
- 任意阶段验收发现右栏 API 实际返回与 fixtures 不一致并造成线上 5xx。
- 视觉验收发现非白名单模块出现破版（断点错位、Shell 抖动）。

回滚动作：

1. 把 `VITE_V211_SHELL` 改回 `false` 并发版（不需要回滚后端）。
2. 在 `docs/v21-visual-qa-report-roundN.md` 中追加 `Rollback` 段落，记录原因、触发的提交和影响范围；本 plan 文档保持只描述目标状态。
3. 修复后再次开启，必须重新跑 P0 → 当前阶段的所有门禁。

### 16.4 与 v2.1 的兼容性边界

- 路由 path 不变；新版只接管视图层。
- service 层（`frontend-v2/src/service/*.ts`）签名只允许加字段不允许改返回结构，否则旧版页面会在灰度期间崩。
- store 层允许新增 v211 专用 store（`store/modules/v211-*.ts`），但不修改既有 store 的 state 形状。

## 17. 依赖、版本与目录约定

### 17.1 关键依赖版本下限

以 `frontend-v2/package.json` 现状为基线，v2.1.1 锁定如下版本下限（`^` 范围允许小版本升级，禁止跨大版本）：

| 包 | 当前版本 | v2.1.1 下限 | 说明 |
| --- | --- | --- | --- |
| `vue` | `^3.5.13` | `^3.5.0` | 不升 Vue 3.6/4 |
| `naive-ui` | `^2.44.1` | `^2.44.0` | v2.1.1 期间稳定在 2.44.x；如需升 2.45 必须重跑全部视觉门禁 |
| `pinia` | `^2.3.1` | `^2.3.0` | |
| `vue-router` | `^4.5.1` | `^4.5.0` | |
| `@vueuse/core` | `^12.8.2` | `^12.8.0` | 用于断点/事件监听封装 |
| `vite` | `^7.3.2` | `^7.3.0` | |
| `vue-tsc` | `^2.2.0` | `^2.2.0` | |
| `echarts` | 未引入 | `^5.5.0` | P1 引入；按需 `import { use } from 'echarts/core'`，不引入 `echarts` 全包 |

ECharts 必须在 `design-system/v211/chart/` 内集中注册组件（line/bar/pie/gauge/grid/tooltip/legend），其他位置不允许 `import 'echarts'` 或 `echarts.init`。

### 17.2 目录结构

```
frontend-v2/src/design-system/v211/
├── index.ts                 # barrel：仅暴露 V211* 组件与公共类型
├── types.ts                 # 共享 props/emits 类型
├── tokens.css               # CSS 变量（布局/grid 用）
├── theme.ts                 # Naive overrides 工厂
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

约束：

- 模块代码只从 `@/design-system/v211` 引入；禁止深路径 `@/design-system/v211/components/V211MetricCard.vue`。
- 旧 `design-system/components/` 在 v2.1.1 期间冻结：不接受新增组件，只接受 bug 修复；P5 收口阶段做删除 PR。
- 测试 fixtures 集中放 `frontend-v2/tests/fixtures/`：
  - `v211-native-control-allowlist.json`
  - `v211-api-gap-registry.json`
  - `v211-visual-parity-baseline.json`（截图哈希/坐标基线）

### 17.3 命名约定

- 组件：`V211XxxYyy.vue`，PascalCase，禁止缩写（`V211RP.vue` 不允许）。
- token：`brandPrimary`、`surface`、`textPrimary` 等语义键；不允许 `green01`、`blue02` 这类视觉命名。
- store：`useV211ShellStore`、`useV211RightPanelStore`，与既有 store 不混。
- feature flag：`VITE_V211_*` 前缀。
- 截图产物：`.tmp/v211-ui-comparison/<module>-side-by-side.png`、`<module>-current.png`、`<module>-design.png` 三件套。
