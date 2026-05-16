# MMMail v2.1.1 UI 规范（Spec）

本文件是 v2.1.1 的最终规范。所有声明都必须可被代码或测试验证。本文件不描述阶段排期、人力估算、风险登记、回滚原因等过程性内容；过程性内容由 `docs/v21-ui-parity-plan-2.1.1.md` 与各 round 的 `v21-visual-qa-report-roundN.md` 维护。

当本规范与方案文档冲突时，以本文件为准。

## 0. 规范状态

- 版本：v2.1.1（spec v1.0）
- 适用范围：`frontend-v2/`（Vue 3 + Naive UI + Pinia + Vue Router）
- 不在范围：`backend/`、第三方运营页、`docs/MMMail/UI` 设计稿原图本身
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

| 项 | 规范 |
| --- | --- |
| Vue | `^3.5.0`（不跨 3.6/4） |
| Naive UI | `^2.44.0`（v2.1.1 期间锁定 2.44.x） |
| Pinia | `^2.3.0` |
| Vue Router | `^4.5.0` |
| `@vueuse/core` | `^12.8.0` |
| Vite | `^7.3.0` |
| TypeScript | `^5.7.0` |
| `vue-tsc` | `^2.2.0` |
| ECharts | `^5.5.0`（按需 `import { use } from 'echarts/core'`，不引入全包） |

依赖变更必须保持 `frontend-v2/package.json` 与本节同步；新增 UI 库（除 ECharts）必须先修订本规范。

## 3. Naive UI 强制约束

### 3.1 基础控件来源

Vue 模板中的以下原生标签默认禁止：`button`、`input`、`textarea`、`select`、`table`。允许的替代见 §5。

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

### 3.2 自研边界

自研组件只能作为 Naive UI 组件的组合封装。允许保留必要原生结构的业务画布：

- `views/sheets/**`：spreadsheet 网格、公式编辑器、单元格输入。
- `views/calendar/**`：周/日视图时间网格的拖拽轨道。
- `views/docs/**`：文档编辑器内部富文本结构（基于现有富文本方案）。
- `views/drive/**`：上传拖拽区（封装为 `NUploadDragger`，禁止自写 dragenter/dragover）。

业务画布的内部 DOM 不进入 Naive UI 约束扫描范围，但其外层工具栏、属性面板、操作栏仍受约束。

### 3.3 主题注入

- 全局只通过 `NConfigProvider` 注入主题；禁止页面级覆盖 Naive 组件内部样式。
- 主题源是 `src/design-system/v211/theme.ts` 导出的 `buildV211ThemeOverrides(tokens)`。
- 页面 CSS 只允许处理：模块布局 grid、业务画布几何、动画 keyframes；不得重写 `n-button`、`n-input`、`n-data-table`、`n-card` 等内部样式。

## 4. 设计 Token

### 4.1 Token 文件

- `src/design-system/v211/tokens.ts`：TS 导出 `V211Tokens`，是规范的事实之源。
- `src/design-system/v211/tokens.css`：把 `V211Tokens` 投射成 CSS 变量（`--v211-brand-primary` 等），仅供布局/grid/canvas 读。
- `src/design-system/v211/theme.ts`：把同一份 `V211Tokens` 注入 Naive overrides，供 `NConfigProvider` 使用。

三处必须共用同一组语义键名；测试 `v211-design-token-contract.test.mjs` 校验：TS 字段、CSS 变量、Naive overrides 引用键集合完全一致。

### 4.2 Token 表（语义键）

| 类别 | 键 | 用途 |
| --- | --- | --- |
| 表面 | `appBg`、`surface`、`surfaceSoft`、`surfaceMuted`、`overlay` | 页面背景、卡片、抽屉遮罩 |
| 边框 | `border`、`borderStrong`、`focusRing` | 普通边框、强调边框、`:focus-visible` 轮廓 |
| 文本 | `textPrimary`、`textSecondary`、`textMuted`、`textDisabled`、`textInverse` | 文字层级 |
| 品牌 | `brandPrimary`、`brandPrimaryHover`、`brandSoft`、`brandBorder`、`brandContrast` | 主色、悬停、弱化背景、边框、品牌按钮文字 |
| 状态 | `success`、`info`、`warning`、`danger` | 状态色基线 |
| 产品色 | `productMail`、`productCalendar`、`productDrive`、`productDocs`、`productSheets`、`productPass`、`productCollaboration`、`productCommand`、`productNotifications`、`productAdmin`、`productSettings`、`productLabs` | 模块识别色（仅用于侧栏图标、模块徽章、KPI 装饰） |
| 商业 | `premium`、`hosted`、`preview` | Premium / Hosted / 预览态徽章 |
| 圆角 | `radiusXs`、`radiusSm`、`radiusMd`、`radiusLg`、`radiusXl` | 4 → 16，统一圆角 |
| 阴影 | `shadowSm`、`shadowMd`、`shadowLg` | 卡片、悬浮、模态 |
| 动效 | `durationFast`、`durationBase`、`durationSlow`、`easingStandard`、`easingEmphasis` | 过渡时长与曲线 |
| 字号 | `fontSizeXs`/`Sm`/`Base`/`Md`/`Lg`/`Xl`/`Xxl` | 12 / 13 / 14 / 16 / 18 / 20 / 24 |
| 行高 | `lineHeightTight`、`lineHeightBase`、`lineHeightLoose` | 1.2 / 1.5 / 1.7 |
| 间距 | `space1`–`space8` | 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 |
| 密度 | `densityFactor` | `<1` 时主题进入紧凑模式 |

不允许 hex/rgb/`oklch()` 出现在组件 `<style>` 中；颜色一律走 token。新增 token 必须更新 `tokens.ts` + `tokens.css` + 本表，并在同一 PR 中加入对比度记录（§7.1）。

### 4.3 主色

- `brandPrimary` 取设计稿中的青绿色系；HSL 范围 `H 150–170, S 55–75%, L 38–48%`。
- `brandPrimaryHover` 必须比 `brandPrimary` 暗 4–8% L，不得通过透明度叠加生成。
- 产品色仅用于装饰，不作为操作主色；操作主色一律 `brandPrimary`。

### 4.4 命名规则

- 所有 token 使用语义命名，不允许 `green01`、`blue02`、`gray400` 这类视觉命名。
- CSS 变量统一前缀 `--v211-`；忌混用 `--mm-` / `--app-`。
- 测试 `v211-design-token-contract.test.mjs` 校验前缀与命名规则。

## 5. Naive UI 组件映射

| 设计稿元素 | v2.1.1 实现 |
| --- | --- |
| 应用壳层 | `NLayout` / `NLayoutSider` / `NLayoutHeader` / `NLayoutContent` / `NMenu` |
| 顶部搜索区 | `NInput` / `NButton` / `NDropdown` / `NAvatar` / `NBadge` |
| 面包屑 / 标题 | `NBreadcrumb` / `NPageHeader` / `NSpace` / `NFlex` |
| KPI 卡片 | `NCard` / `NStatistic` / `NNumberAnimation` / `NProgress` / `NTag` |
| 表格 | `NDataTable` / `NPagination` / `NCheckbox` / `NDropdown` |
| 普通列表 | `NList` / `NListItem` / `NThing` / `NAvatar` / `NTag` |
| 标签页 | `NTabs` / `NTabPane` |
| 表单 | `NForm` / `NFormItem` / `NInput` / `NSelect` / `NDatePicker` / `NSwitch` / `NCheckbox` / `NRadioGroup` |
| 抽屉 | `NDrawer` / `NDrawerContent` |
| 弹窗 | `NModal` / `NPopconfirm` / `NDialogProvider` |
| 提示 | `NTooltip` / `NPopover` / `NAlert` / `NMessageProvider` / `NNotificationProvider` |
| 上传 | `NUpload` / `NUploadDragger` / `NProgress` |
| 树 | `NTree` / `NTreeSelect` |
| 空 / 加载 / 错误 | `NEmpty` / `NSkeleton` / `NSpin` / `NResult` |
| 时间线 | `NTimeline` / `NTimelineItem` |
| 日历控件 | `NCalendar` / `NDatePicker` / `NTimePicker` |
| 虚拟滚动 | `NVirtualList`（列表 ≥ 200 行启用） |

设计稿中存在但 Naive UI 没有的能力，由第 6 节共享业务组件提供。

## 6. 共享业务组件

所有共享组件位于 `src/design-system/v211/components/`，以 `V211` 前缀命名，barrel `src/design-system/v211/index.ts` 导出。模块代码仅允许从 barrel 导入；禁止深路径导入。

签名变更由 `v21-design-system-components.test.mjs` 锁定，PR 需同步快照。

### 6.1 通用类型

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

### 6.2 V211AppShell

| 维度 | 契约 |
| --- | --- |
| 用途 | 设计稿级应用框架：左侧窄导航 + 顶部栏 + 主内容 + 右侧上下文面板 |
| props | `currentModule: ModuleId`、`navCollapsed?: boolean`、`rightPanelCollapsed?: boolean`、`rightPanelWidth?: number` |
| 内部组件 | `NLayout`、`NLayoutSider`、`NLayoutHeader`、`NLayoutContent`、`NMenu`、`NDropdown`、`NAvatar`、`NBadge` |
| slots | `topBarLeft`、`topBarRight`、`default`（主内容）、`rightPanel` |
| 不允许 | 任何模块自写 `<header>` / `<aside>`；任何模块在 Shell 外发起搜索/通知/账户菜单 |
