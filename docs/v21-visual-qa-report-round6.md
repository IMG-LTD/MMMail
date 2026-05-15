# MMMail v2.1 视觉QA测试报告 (Round 6)

**测试日期**: 2026-05-15
**测试方式**: 真实 Chrome 浏览器自动化截图 + AI 视觉识别审查
**测试覆盖**: 69 个场景，涵盖 13 个 UI 模块，3 种视口（desktop 1440x900 / tablet 1024x768 / mobile 390x844）
**前端版本**: frontend-v2 (Vite dev server :5174)
**后端版本**: Java backend (:8080)
**TypeScript 类型检查**: 通过（0 错误）

---

## 测试总结

| 指标 | 结果 |
|------|------|
| 总截图数 | 69 |
| DOM 选择器验证 | 全部通过 |
| 严重缺陷 (P0/P1) | 0 |
| 中等缺陷 (P2) | 1 |
| 低优先级缺陷 (P3) | 12 |
| 信息性问题 | 3 |

**结论**: v2.1 整体视觉质量良好，无阻断性缺陷。所有页面在三种断点下布局响应正确，设计一致性高。

---

## 模块覆盖情况

| UI 模块 | 场景数 | 截图数 | 状态 |
|---------|--------|--------|------|
| 首页 (Workspace) | 4 | 6 | PASS |
| 邮件 (Mail) | 4 | 6 | PASS |
| 日历 (Calendar) | 2 | 4 | PASS |
| 云盘 (Drive) | 2 | 4 | PASS |
| 文档 (Docs) | 3 | 5 | PASS |
| Sheets & Labs | 4 | 6 | PASS |
| Pass 密码管理 | 6 | 8 | PASS |
| 协作 (Collaboration) | 1 | 3 | PASS |
| 指挥中心 (CommandCenter) | 1 | 3 | PASS |
| 通知 (Notifications) | 1 | 3 | PASS |
| 管理后台 (Admin) | 4 | 6 | PASS |
| 设置 (Settings) | 2 | 4 | PASS |
| 公共页面 (Public/Auth/Share/System) | 11 | 11 | PASS |

---

## 缺陷清单

### P2 中等缺陷

| # | 模块 | 截图 | 描述 |
|---|------|------|------|
| 1 | 邮件 | mail-inbox-mobile.png | 长发件人名称未做截断处理，挤压右侧时间戳显示空间，影响信息可读性 |

### P3 低优先级缺陷

| # | 模块 | 截图 | 描述 |
|---|------|------|------|
| 2 | 首页 | workspace-shell-mobile.png | 顶栏右侧用户头像与边缘间距偏小，接近触控热区重叠 |
| 3 | 邮件 | mail-inbox-tablet.png | 邮件列表与导航栏分隔线在深色模式下对比度略弱 |
| 4 | 日历 | calendar-board-mobile.png | 移动端事件文字行间距偏紧，多事件日期下文字可读性略降 |
| 5 | 云盘 | drive-files-mobile.png | 文件操作菜单(三点图标)触控热区偏小，移动端易误触 |
| 6 | 文档 | docs-workspace-mobile.png | 底部导航栏与最后一张文档卡片间距略紧，滚动到底时内容贴边 |
| 7 | Sheets | sheets-editor-desktop.png | 工具栏按钮在窄窗口下未显示溢出菜单提示 |
| 8 | Labs | labs-overview-desktop.png | Premium 徽章与卡片标题间距不一致，第二行卡片徽章偏右约2px |
| 9 | Pass | pass-vault-mobile.png | 密码强度指示条在小屏下与文字间距过窄，视觉略拥挤 |
| 10 | 协作 | collaboration-overview-mobile.png | 底部操作按钮与卡片内容间距过紧，可能误触 |
| 11 | 指挥中心 | command-center-mobile.png | 搜索栏与页面标题间距不足，视觉层次不够分明 |
| 12 | 通知 | notifications-tablet.png | 通知项右侧操作图标在窄屏下略拥挤，触控目标偏小 |
| 13 | 管理 | admin-users-desktop.png | 表格最后一列操作按钮在窄屏桌面下可能被截断 |

### 信息性问题

| # | 模块 | 描述 |
|---|------|------|
| 14 | Pass | pass-item-detail-desktop.png 文件名与实际截图名(pass-secret-actions-desktop.png)不一致 |
| 15 | Pass | pass-share-settings-desktop.png 文件名与实际(pass-secure-link-settings-desktop.png)不一致 |
| 16 | 邮件 | mail-thread-reader-desktop.png 文件名与实际(mail-thread-workbench-desktop.png)不一致 |

---

## 各模块视觉评估摘要

### 首页 (Workspace Shell)
- Desktop: 左侧导航栏、顶部工具栏、主内容区和右侧上下文面板完整渲染
- Tablet: 导航栏正确收缩为图标模式，内容区自适应填充
- Mobile: 导航栏隐藏为汉堡菜单，底部 Tab Bar 可见

### 邮件 (Mail)
- Desktop: 三栏布局（导航/列表/预览）正常，未读标记和操作按钮可见
- Tablet: 预览面板隐藏，双栏展示
- Mobile: 单栏列表视图，邮件摘要截断合理
- 写信面板: 工具栏图标清晰，收件人/主题字段对齐良好
- 线程阅读: 消息气泡层次分明，回复区域可见

### 日历 (Calendar)
- Desktop: 网格对齐正确，事件色块可区分
- Tablet: 网格自适应缩放，事件标签未被截断
- Mobile: 切换为紧凑视图，日期导航可操作
- 事件抽屉: 表单字段完整，关闭按钮可见

### 云盘 (Drive)
- Desktop: 文件列表对齐良好，侧边栏与主内容区比例合理
- Tablet: 侧边栏折叠为汉堡菜单，文件网格自适应
- Mobile: 单列卡片堆叠合理
- 分享面板: 权限选择器和输入框清晰可辨

### 文档 (Docs)
- Desktop: 文档列表整齐，编辑器工具栏完整
- Tablet/Mobile: 卡片宽度自适应，无溢出
- 分享面板: 与 Drive 分享面板风格一致

### Sheets & Labs
- Desktop: 表格列头对齐，排序图标可见，编辑器网格正常
- Tablet/Mobile: 卡片自适应，无水平滚动溢出
- Labs: 实验功能卡片布局整齐，Premium 标记醒目

### Pass 密码管理
- Desktop: 左侧导航+右侧密码列表结构清晰
- Tablet: 侧边栏收缩为图标模式
- Mobile: 全宽列表布局，底部导航栏可见
- 安全链接/监控/风险详情: 布局合理，颜色编码清晰

### 协作 (Collaboration)
- Desktop: 卡片网格对齐良好，协作成员头像和状态指示器可见
- Tablet: 卡片从三列变为两列
- Mobile: 单列布局，导航折叠正常

### 指挥中心 (Command Center)
- Desktop: 仪表盘面板排列整齐，图表清晰可读
- Tablet: 面板重排为两列
- Mobile: 单列堆叠，统计卡片完整
- 命令面板: 模态居中，搜索输入框聚焦状态明显

### 通知 (Notifications)
- Desktop: 通知列表分组清晰，未读标记醒目
- Tablet: 列表自适应良好
- Mobile: 单列展示正常

### 管理后台 (Admin)
- Desktop: 统计卡片、图表、用户表格均完整呈现
- Tablet: 卡片自动换行为两列
- Mobile: 侧边栏正确折叠
- 子页面: 用户管理/风险/系统状态均正常

### 设置 (Settings)
- Desktop: 分区清晰，表单控件对齐良好
- Mobile: 表单堆叠正常，无水平溢出
- 删除确认弹窗: 居中显示，警告文案醒目

### 公共页面
- 登录/注册: 品牌Logo和配色正确，表单字段完整
- 公开分享页: 邮件/云盘/密码分享预览正常
- 系统状态页(404/500/维护/离线): 风格统一，操作按钮清晰
- 产品访问受限: 升级提示和CTA按钮醒目

---

## 建议修复优先级

1. **P2 (建议本迭代修复)**: 邮件移动端长发件人名称截断
2. **P3 (下一迭代)**: 移动端触控热区和间距微调（共12项）
3. **信息性**: 测试脚本文件名与实际截图名同步（3项）

---

## 修复闭环状态

**修复提交**: `d94dc0fd fix(frontend-v2): close round6 visual qa gaps`

| 原问题范围 | 当前状态 | 修复说明 |
|------------|----------|----------|
| P2 邮件移动端长发件人挤压时间戳 | 已修复 | 邮件列表发件人增加单行省略、最小宽度约束，时间戳固定为不压缩区域 |
| P3 顶栏、日历、云盘、文档、Sheets、Labs、Pass、协作、指挥中心、通知、管理后台视觉问题 | 已修复 | 已补齐移动端间距、触控热区、表格操作按钮尺寸、底部导航留白、工具栏溢出提示和窄屏换行策略 |
| 信息性截图命名不一致 | 已修复 | 视觉 QA 场景与报告统一使用 `pass-secret-actions`、`pass-secure-link-settings`、`mail-thread-workbench` |
| 视觉 QA 运行时数据稳定性 | 已修复 | 脚本注入真实 API 地址、认证态和引导态，并为 Docs/Sheets 创建运行时测试数据 |

**回归保障**:

- 新增 `frontend-v2/tests/v21-round6-visual-regression-contract.test.mjs`，锁定 Round6 视觉修复点。
- 更新 `frontend-v2/tests/v21-browser-visual-qa-contract.test.mjs`，锁定视觉 QA 真实运行时和截图命名契约。
- 最新自动化视觉 QA 已通过，生成 69 张截图，报告见 `docs/superpowers/progress/v21-browser-visual-qa-report.md`。

---

## 测试环境

- OS: Linux 6.17.0-14-generic
- Chrome: headless (CDP 协议)
- Node.js: 运行 visual QA 脚本
- 认证: 自动注册测试用户获取 accessToken
- 截图存储: `.tmp/v21-browser-visual-qa/` (69 files)
