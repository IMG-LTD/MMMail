# MMMail v2.1.1 UI 集成测试报告（Round 7）

**测试日期**: 2026-05-15
**测试环境**: Frontend Vite dev server (port 5174) + Backend Spring Boot (port 8080, Flyway disabled)
**浏览器**: Chrome headless via Playwright-core 1.52.0
**主视口**: 1350×940
**响应式视口**: 375 / 768 / 1024 / 1440
**设计规范**: `docs/v21-ui-spec-2.1.1.md` v1.0
**设计源**: `docs/MMMail/UI/` 各模块概览图

---

## 测试覆盖

| 维度 | 测试方法 | 覆盖范围 |
|------|----------|----------|
| 视觉与布局 | 截图 vs 设计稿逐像素对比 | 首页、邮件、设置、日历、云盘、登录 |
| 交互与动效 | Playwright 模拟用户操作 | 登录表单、路由跳转、导航菜单、快捷键、前进后退 |
| 多端与兼容性 | 4 断点 × 7 页面 = 28 组截图 | xs/md/lg/xl 全覆盖 |
| 前端基础与可访问性 | DOM 结构检查 + 控制台监控 + WCAG 基础项 | 表单标签、HTTP 状态、主题注入、路由守卫 |

---

## 一、根因分析

### Vite 缓存导致路由失效（影响多个缺陷）

**现象**: 多个路由不可达、Auth guard 完全失效。

**根因**: Vite dev server 提供的 `src/app/router/index.ts` 是旧版缓存，与磁盘文件不一致。磁盘文件包含完整的 `useAuthStore` + `resolveAuthRedirect` auth guard，但 served JavaScript 中 `beforeEach` 仅检查 `/share/` 前缀后无条件 `next()`。Sourcemap 确认 served source 与磁盘 source 不同。

**影响范围**: NAV-009, NAV-001, NAV-010, DEF-03, DEF-07, NAV-012

**修复建议**:
```bash
cd frontend-v2
rm -rf node_modules/.vite
pnpm dev
```
若问题持续，检查是否有 Vite 插件在 transform 阶段修改 router 文件。

---

## 二、缺陷清单

### P0 — 阻断级

| ID | 维度 | 页面 | 描述 | 根因 |
|----|------|------|------|------|
| NAV-009 | 交互 | 全局 | Auth guard 完全失效：7 个受保护路由（/mail/inbox, /inbox, /drive, /pass, /calendar, /suite, /settings）无需登录即可访问 | Vite 缓存 |

### P1 — 严重

| ID | 维度 | 页面 | 描述 | 根因 |
|----|------|------|------|------|
| NAV-001 | 交互 | `/` | 根路由重定向到 `/suite` 而非 `/workspace`（磁盘定义为 `/workspace`） | Vite 缓存 |
| NAV-010 | 交互 | `/workspace` | `/workspace` 解析为 404，相关子路由 /today /activity /tasks 均不可达 | Vite 缓存 |
| DEF-03 | 交互 | `/community` | 社区模块路由不可达，重定向到 /404 | Vite 缓存 |
| NAV-012 | 交互 | `/admin` | 管理后台路由不可达，解析为 404 | Vite 缓存 |

### P2 — 需修复

| ID | 维度 | 页面 | 描述 | 修复建议 |
|----|------|------|------|----------|
| DEF-07 | 交互 | 全局 | 未登录访问受保护路由显示 /404 而非重定向 /login | 清除缓存后验证；若仍存在则修复 auth guard fallback |
| DEF-08 | 交互 | 全局 | Ctrl+K 快捷键未触发 Command Palette | 检查键盘事件绑定 |
| NAV-007 | 交互 | `/register` | 注册页无标准提交按钮（仅有"返回"和"继续进入恢复包"） | 添加"注册"/"Create Account" 按钮 |
| DEF-05 | 可访问性 | `/login` `/register` | 6 个 input 缺少关联 label 或 aria-label（违反 WCAG 1.3.1 / 4.1.2） | NFormItem 添加 label prop 或 input 添加 aria-label |
| DEF-01 | 视觉 | 首页 | 工作台 KPI 卡片 glass-morphism 透明度过低（~0.85 vs 设计稿 ~0.6-0.7），blur 不足（~4px vs 设计稿 ~12-16px） | `backdrop-filter: blur(12px)` + 降低 opacity |
| V-01 | 视觉 | 首页 | 左侧导航栏宽度偏宽，图标与边缘间距大于设计稿（设计稿约 64px 纯图标模式） | 调整 NLayoutSider width 和 padding |
| V-02 | 视觉 | 首页 | 搜索框圆角/边框/placeholder 颜色与设计稿有差异 | 调整 NInput 主题覆盖 |
| V-06 | 视觉 | 邮件 | 邮件列表行高与间距不一致（设计稿 64-72px/行 + 1px 分隔线） | 调整列表 item padding |
| V-07 | 视觉 | 邮件 | 文件夹侧栏宽度偏差（设计稿 200-220px），影响三栏比例 | 调整侧栏固定宽度 |
| V-10 | 视觉 | 设置 | 选中态缺少左侧蓝色竖条指示器 + 浅蓝背景 | 添加 NMenu active 样式覆盖 |
| V-13 | 视觉 | 全局 | 导航图标尺寸不统一（设计稿统一 20-22px） | 统一 icon size token |
| NAV-011 | 交互 | `/mail/inbox` | 重定向到 `/conversations/inbox`（catch-all 匹配），非预期行为 | 添加显式 `/mail/inbox` 路由 |

### P3 — 低优先级

| ID | 维度 | 页面 | 描述 |
|----|------|------|------|
| DEF-02 | 视觉 | 首页 | Context Panel 内部间距比设计稿大 4-8px |
| DEF-06 | 基础 | `/login` | 控制台 404 资源请求错误（不影响功能） |
| DEF-09 | 交互 | `/mail/*` | `/mail/nonexistent` 被 catch-all 重写到 conversations 视图而非 404 |
| V-03 | 视觉 | 首页 | 卡片阴影偏重，圆角与设计稿 12px 不一致 |
| V-04 | 视觉 | 首页 | 主内容区背景色偏白（设计稿 #F5F6FA 浅灰蓝） |
| V-05 | 视觉 | 首页 | 顶部栏通知图标与头像间距偏差 |
| V-08 | 视觉 | 邮件 | 未读标记圆点大小/饱和度与设计稿有差异 |
| V-09 | 视觉 | 邮件 | 顶部栏按钮间距、图标尺寸有微调空间 |
| V-11 | 视觉 | 设置 | 表单元素垂直间距偏差（设计稿 24-32px） |
| V-12 | 视觉 | 设置 | 内容区卡片容器圆角/阴影/内边距微调 |
| V-14 | 视觉 | 全局 | 品牌色在不同组件上饱和度/色相轻微偏差 |
| V-15 | 视觉 | 全局 | 个别位置字体大小与设计稿有 1-2px 偏差 |

---

## 三、通过项

### 视觉与布局
- AppShell 左侧窄导航 + 顶部栏 + 主内容结构与设计稿一致
- NDataTable 表格布局正确
- NCard 卡片结构层级正确
- 面包屑 / NPageHeader 布局正确

### 交互与动效
- 登录表单填写 + 错误密码提交：正确显示错误反馈
- 登录表单空提交：显示验证反馈（第三 agent 确认）
- 导航菜单项点击：路由正确跳转
- 浏览器前进/后退导航：正常工作
- CSS transitions 存在（51 个元素）
- 邮件 compose 按钮功能正常，cursor 样式正确
- 搜索框可聚焦并接受输入
- 未知路由正确重定向到 `/404`

### 多端与兼容性
- xs (375px)：侧栏自动折叠，内容区自适应 ✓
- md (768px)：侧栏折叠，表格横向滚动 ✓
- lg (1024px)：标准三栏布局 ✓
- xl (1440px)：宽屏内容区正确拉伸 ✓
- 表单在移动端单列排布 ✓
- 7 个页面 × 4 断点 = 28 组全部通过

### 前端基础与可访问性
- 所有页面 HTTP 状态码 200
- NConfigProvider 正确注入主题 token
- 登录页基本布局合理（居中表单、品牌 logo、输入框、按钮）

---

## 四、统计汇总

| 严重度 | 数量 | 状态 |
|--------|------|------|
| P0 | 1 | 清除 Vite 缓存后预计自动修复 |
| P1 | 4 | 清除 Vite 缓存后预计自动修复 |
| P2 | 12 | 需代码修复 |
| P3 | 12 | 可排入下一迭代 |
| **总计** | **29** | — |

---

## 五、修复建议与优先级

### 第一步：清除 Vite 缓存（预计修复 P0 + 4 个 P1）

```bash
cd frontend-v2
rm -rf node_modules/.vite
pnpm dev
```

清除后重新验证：
- Auth guard 是否生效
- `/workspace` 是否可达
- `/community` 和 `/admin` 是否可达
- 根路由是否重定向到 `/workspace`

### 第二步：代码修复（P2 交互 + 可访问性）

1. 注册页添加标准提交按钮
2. 登录/注册 input 添加 label 或 aria-label
3. Ctrl+K Command Palette 键盘绑定
4. `/mail/inbox` 添加显式路由（避免 catch-all 匹配）

### 第三步：视觉还原（P2 视觉）

1. 导航栏宽度调整为 64px
2. 搜索框主题覆盖（圆角、边框、placeholder 色）
3. 邮件列表行高 + 侧栏宽度
4. 设置页选中态竖条指示器
5. 导航图标尺寸统一
6. Glass-morphism 效果调整

### 第四步：微调（P3，可排入后续迭代）

间距、阴影、圆角、背景色、字体等像素级微调。

---

## 六、整体评估

框架结构和布局与设计稿基本吻合，核心交互路径（登录 → 邮件 → 设置）在功能层面可用。**最关键的问题是 Vite 缓存导致路由/Auth guard 失效**，这是一个环境问题而非代码缺陷，清除缓存后 P0 和 P1 预计全部消失。

剩余的 P2 缺陷分为两类：交互功能缺失（注册按钮、快捷键、a11y 标签）和视觉还原精度（间距、宽度、样式细节）。建议在 v2.1.1 正式发布前完成 P2 修复，P3 可视团队节奏安排。

响应式兼容性表现良好，4 个断点 28 组测试全部通过，无需额外处理。
