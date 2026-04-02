# Community Edition v1.2 Planning Baseline

**版本**: `v1.2-planning-draft`
**日期**: `2026-04-01`
**作者**: `Codex`

## 背景
- `v1.1.0` 已正式发布，`dev/community-v1` 转入 `v1.2` 集成开发。
- 用户提供了对标 `Proton.me` 的差距分析；该分析对产品方向有价值，但不能直接等同于仓库真实状态。
- `v1.2` 必须先做范围收敛，再进入实现，避免把 `E2EE / SMTP / 原生客户端 / 支付闭环` 一次性塞进同一版本导致目标失真。

## 基于仓库的真实判断
### 已确认的真实缺口
1. 前端当前未发现 `manifest.webmanifest`、`Service Worker`、PWA 安装链路。
2. 当前代码库未发现 `OpenPGP / E2EE / 零知识密钥管理` 的落地实现。
3. 当前代码库未发现可验证的 `SMTP / IMAP / Bridge` 协议栈实现。
4. 当前 Community 账单能力仍停留在 `quote / draft / invoice state`，未进入真实支付闭环。

### 需要纠偏的外部结论
1. `Mail / Calendar / Drive` 不是“纯 UI mock”：仓库存在对应 controller、service、mapper 与集成测试。
2. `Docs / Sheets` 也不只是静态页面：仓库存在协作、建议、版本、share/incoming 等工作流代码与测试。
3. `i18n` 并非“没有门禁”：`v1.1` 已引入页面覆盖率与文案回归链，但当前语言覆盖仍然较窄。

## Assumptions
- `v1.2` 仍以 `Community Edition`、`Web-first`、`self-hosted collaboration suite` 为基线，不承诺在单一版本内追平 Proton 全产品栈。
- `E2EE / 零知识架构 / SMTP/IMAP/Bridge` 若进入产品承诺，必须先经过架构评审、威胁模型更新与数据迁移设计；因此在 `v1.2` 只进入 discovery / ADR，不作为本次直接 release 承诺。
- `移动端策略` 是 `v1.2` 最适合落地的首批改进，因为它与现有 Web 代码库兼容，且可为后续通知与原生客户端打基础。

## v1.2 版本目标
`v1.2` 不追求产品广度扩张，集中做四条线：
1. `Mobile / PWA baseline`
2. `Capability honesty & boundary cleanup`
3. `Privacy / transport architecture discovery`
4. `Adoption readiness`（开放文档 / a11y / 自托管接入体验）

## 推进顺序
1. `PWA / 移动端基线`
2. `能力边界与对外口径收口`
3. `隐私与协议栈架构预研`
4. `开放接入与可访问性基线`

## Stream A - Mobile / PWA Baseline
### 目标
- 让 Community Web 端具备基础安装能力、离线壳层与浏览器端移动使用入口。

### 本批范围
- `manifest.webmanifest`
- `Service Worker` 注册与离线壳层缓存
- 设置页 `PWA readiness` / 安装入口 / 通知权限入口
- 相关前端回归测试与 typecheck

### DoD
- 浏览器可识别为 installable web app
- 前端在支持环境下注册 `Service Worker`
- 设置页明确展示安装状态、连接状态、通知权限状态
- 没有把尚未实现的 Web Push / 原生推送伪装成已完成能力

### 不做
- 真实 Web Push 下发
- iOS / Android 原生 App
- 离线写入同步

## Stream B - Capability Honesty & Boundary Cleanup
### 目标
- 收敛 README / release / support boundary 中可能被误解为“已经具备 E2EE / 真实协议栈”的描述。

### 范围
- 统一 `privacy-first` 的实现边界说明
- 明确 Community 与 Hosted / future commercial capability 的口径
- 为 `Mail / Drive` 的未来加密路线预留术语与边界文档

### 不做
- 不为了“显得更强”而写超出代码现状的承诺

## Stream C - Privacy / Transport Architecture Discovery
### 目标
- 给 `E2EE / 零知识 / SMTP/IMAP/Bridge` 做真实可执行的前置设计，而不是在产品版本里直接硬上半成品。

### 交付物
- 架构决策文档（ADR）
- 密钥管理与数据迁移风险清单
- 邮件协议栈接入策略与阶段拆分

### 不做
- 不在 `v1.2` 直接承诺“全量 E2EE 已上线”
- 不引入半实现的加密文案误导用户

## Stream D - Adoption Readiness
### 目标
- 补足 Community Edition 被真实采用前最短板的基础能力。

### 候选范围
- a11y 基线检查与高频页面修复
- OpenAPI / API docs 暴露方式梳理
- 自托管接入与运维文档的移动端 / PWA 补充

### 不做
- 不把 `支付闭环 / SSO / SCIM / LDAP` 塞入 `v1.2` 首批

## 当前首批实施项
本轮实现只落 `Stream A - Mobile / PWA Baseline`：
1. PWA manifest
2. Service Worker 注册
3. 设置页 PWA 能力面板
4. 对应测试与验证

## 明确不进入 v1.2 首批代码开发
- `VPN / Meet / Wallet / Lumo` 真能力补强
- `Pass` 浏览器扩展
- 真实支付闭环
- `E2EE / 零知识 / SMTP / IMAP / Bridge` 的半成品实现
