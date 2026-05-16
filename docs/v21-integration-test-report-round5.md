# MMMail v2.1 集成测试报告（第五轮）

**测试日期**: 2026-05-15
**测试分支**: main
**最新提交**: e22f1ec4 (fix(backend): add suite preview runtime migrations)
**测试执行时间**: 09:15 ~ 09:20
**测试类型**: 前端合约 + 后端 API 运行时 + 浏览器视觉 QA + 页面截图人工审查

---

## 1. 测试概览

| 维度 | 第四轮 (05-15 早) | 第五轮 (05-15) | 状态 |
|------|-------------------|----------------|------|
| 前端 TypeScript 编译 | ✅ 通过 | ✅ 通过 | 稳定 |
| 前端单元测试 | ✅ 94/94 | ✅ 94/94 | 稳定 |
| 前端合约测试 | ✅ 94/94 | ✅ 25/25 (独立运行) | 稳定 |
| 后端 API 运行时端点验证 | ✅ 50/50 | ✅ 17/20 (3 问题) | 见缺陷 |
| 浏览器视觉 QA (DOM + 截图) | — | ✅ **69/69 场景通过** | 新增 |
| 页面视觉审查 (33 张截图) | — | ✅ 30 PASS / 3 WARN | 新增 |

---

## 2. 前端测试详情

### 2.1 单元测试（94/94 通过）

- 执行命令: `pnpm test`
- 耗时: 601ms
- 结果: **全部通过，零失败**

### 2.2 合约测试（25/25 通过）

- 执行命令: `pnpm exec node --test tests/*.contract.test.mjs`（15 个合约文件）
- 耗时: 428ms
- 结果: **全部通过，零失败**

### 2.3 TypeScript 类型检查

- 执行命令: `vue-tsc --noEmit`
- 结果: **通过，零类型错误**

---

## 3. 后端 API 运行时验证

### 3.1 端点测试结果（17/20 通过）

| # | 端点 | 预期 | 实际 | 结果 |
|---|------|------|------|------|
| 1 | GET /actuator/health | 200 | 200 | ✅ |
| 2 | GET /api/v2/platform/capabilities | 200 | 401 | ❌ |
| 3 | POST /api/v2/auth/login (空 body) | 400/401 | 400 | ✅ |
| 4 | GET /api/v2/mail/messages | 401 | 401 | ✅ |
| 5 | GET /api/v2/calendar/events | 401 | 401 | ✅ |
| 6 | GET /api/v2/drive/files | 401 | 401 | ✅ |
| 7 | GET /api/v2/pass/items | 401 | 401 | ✅ |
| 8 | GET /api/v2/docs/documents | 401 | 401 | ✅ |
| 9 | GET /api/v2/sheets/workbooks | 401 | 401 | ✅ |
| 10 | GET /api/v2/collaboration/projects | 401 | 401 | ✅ |
| 11 | GET /api/v2/command-center/commands | 401 | 401 | ✅ |
| 12 | GET /api/v2/settings/profile | 401 | 401 | ✅ |
| 13 | GET /api/v2/workspace/summary | 401 | 401 | ✅ |
| 14 | GET /api/v2/system/health | 200 | 401 | ❌ |
| 15 | GET /api/v2/ai/mcp/registry | 401 | 401 | ✅ |
| 16 | GET /api/v2/notifications | 401 | 401 | ✅ |
| 17 | GET /api/v2/admin/overview | 401/403 | 401 | ✅ |
| 18 | GET /api/v2/share/mail/demo-token | 200/404 | 500 | ❌ |
| 19 | GET /api/v2/billing/status | 401 | 401 | ✅ |
| 20 | GET /api/v2/community/feed | 401 | 401 | ✅ |

---

## 4. 浏览器视觉 QA

### 4.1 执行概况

- 工具: Chrome DevTools Protocol (headless)
- 视口: Desktop 1440x900 / Tablet 1024x768 / Mobile 390x844
- 场景总数: 69（路由场景 + 公共页面 + 覆盖层/弹窗）
- 结果: **69/69 全部通过**（DOM 元素检查 + 截图捕获）

### 4.2 UI 分组覆盖

| UI 分组 | 场景数 | 截图数 |
|---------|--------|--------|
| 首页 (Workspace) | 4 | 6 |
| 邮件 (Mail) | 4 | 6 |
| 日历 (Calendar) | 2 | 4 |
| 云盘 (Drive) | 2 | 4 |
| 文档 (Docs) | 3 | 5 |
| Sheets 和 Labs | 4 | 6 |
| Pass | 6 | 8 |
| Collaboration | 1 | 3 |
| Command Center | 1 | 3 |
| Notifications | 1 | 3 |
| Admin | 4 | 6 |
| Settings | 2 | 4 |
| Public/Auth/Share/System | 11 | 11 |

### 4.3 覆盖层/弹窗场景

全部通过 DOM 检查：
- Command Palette（命令面板）
- Quick Create（快速创建弹窗）
- Theme Drawer（主题抽屉）
- Mail Compose + Security 状态
- Mail Thread Workbench
- Calendar Event Drawer（含冲突面板、资源状态、保存错误/重试）
- Drive Share Panel（含成员、公开链接、撤销、重试）
- Docs Share Panel（含邀请输入、角色选择、链接访问、协作者、错误/重试）
- Sheets Protected Range（含范围输入、编辑者、冲突、错误/重试）
- Pass Secret Actions（含密码揭示、轮换确认、错误/重试）
- Pass Secure Link Settings
- Pass Risk Detail
- Settings Delete Confirmation

---

## 5. 视觉审查结果（33 张截图人工检查）

### 5.1 审查总结

| 分组 | 检查数 | PASS | WARN | FAIL |
|------|--------|------|------|------|
| 核心应用 (Workspace/Mail/Calendar) | 8 | 7 | 1 | 0 |
| 生产力 (Drive/Docs/Sheets) | 9 | 8 | 1 | 0 |
| 安全/管理 (Pass/Admin/Settings) | 8 | 7 | 1 | 0 |
| 公共/覆盖层 (Login/Share/Overlays) | 8 | 8 | 0 | 0 |
| **合计** | **33** | **30** | **3** | **0** |

### 5.2 视觉审查通过项

- 所有页面布局正确，无元素重叠或溢出
- 响应式适配正常（Desktop/Tablet/Mobile 三端）
- 设计一致性良好（配色、字体、间距统一）
- 导航栏（侧边栏 + 顶栏）结构完整
- 所有弹窗/抽屉正确居中/定位，有遮罩层
- 安全 UI 元素（锁图标、密码遮罩、揭示按钮）正确渲染
- 公共页面品牌标识一致

---

## 6. 缺陷清单

### 6.1 后端缺陷

| ID | 严重程度 | 模块 | 描述 | 根因分析 |
|----|----------|------|------|----------|
| B-001 | Major | Auth/Security | `/api/v2/platform/capabilities` 返回 401，应为公开端点 | 安全过滤器 permit list 缺少该路径 |
| B-002 | Major | System | `/api/v2/system/health` 返回 401，应为公开端点 | 同上，监控/负载均衡探针无法访问 |
| B-003 | Critical | Share | `/api/v2/share/mail/demo-token` 返回 500 Internal Server Error | 公共分享端点抛出未处理异常（可能是空指针或缺少服务依赖） |

### 6.2 前端视觉缺陷

| ID | 严重程度 | 模块 | 描述 | 截图 |
|----|----------|------|------|------|
| F-001 | Minor | Calendar | 移动端日历头部导航箭头与日期文字间距过小，可能存在触摸目标重叠 | calendar-board-mobile.png |
| F-002 | Cosmetic | Sheets | Protected Range 警告横幅文字对比度略低，建议检查 WCAG AA 标准 (4.5:1) | sheets-protected-range-desktop.png |
| F-003 | Minor | Admin | 移动端管理面板用户表格水平滚动提示不明显，用户可能无法发现截断列 | admin-overview-mobile.png |

---

## 7. 测试环境

| 项目 | 值 |
|------|-----|
| 操作系统 | Linux 6.17.0-14-generic |
| 浏览器 | Google Chrome (headless) |
| Node.js | 当前系统版本 |
| 后端端口 | 127.0.0.1:8080 |
| 前端端口 | 127.0.0.1:5174 |
| 截图目录 | .tmp/v21-browser-visual-qa/ |
| 视觉 QA 报告 | docs/superpowers/progress/v21-browser-visual-qa-report.md |

---

## 8. 结论与建议

### 整体评估

v2.1 前端 UI 质量良好，69 个浏览器场景全部通过 DOM 检查和截图捕获，33 张截图人工审查仅发现 3 个轻微视觉问题。前端合约测试和类型检查全部通过。

后端存在 3 个需要修复的问题，其中公共分享端点 500 错误为最高优先级。

### 修复优先级

1. **P0 (Critical)**: 修复 `/api/v2/share/mail/demo-token` 500 错误 — 影响公共分享功能
2. **P1 (Major)**: 将 `/api/v2/platform/capabilities` 和 `/api/v2/system/health` 加入安全过滤器白名单
3. **P2 (Minor)**: 移动端日历头部间距优化、Admin 表格滚动提示
4. **P3 (Cosmetic)**: Sheets Protected Range 对比度调整

### 与第四轮对比

- 前端测试保持稳定（94/94 + 25/25 合约）
- 新增浏览器视觉 QA 全覆盖（69 场景）
- 后端 API 运行时发现 3 个新问题（第四轮未覆盖这些公共端点）
- 无回归问题
