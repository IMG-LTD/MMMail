# Community Edition v1.2 Mainline Roadmap

**版本**: `v1.2-mainline-roadmap-r2`
**日期**: `2026-04-02`
**作者**: `Codex`

## 背景
- 当前主线以 `dev/community-v1` 为日常集成分支，并与本地/远端 `main` 收敛。
- 用户提供的 `MMMail vs Proton.me` 差距分析成立于“产品竞争力”视角，但不能直接转译成“单版本全部完成”。
- `v1.2` 的目标不是追平 Proton 全产品矩阵，而是把最关键、最真实、最能验证的缺口拆成可执行主线。

## 主线原则
- 只承诺当前仓库可真实落地并可验证的能力。
- 优先补齐 `P0` 根基性差距的基础设施，不做半成品营销叙事。
- `v1.2` 采用“先 foundation，再扩主路径，再补 architecture discovery”的推进方式。

## 对产品报告的执行拆分
### P0 - 根基性差距
1. `Mail E2EE foundation`
   - 客户端生成密钥对
   - 服务端只保存公钥与加密私钥包
   - 设置页可查看 / 更新 key profile
2. `Mail E2EE recipient discovery`
   - 写信页查询收件人投递路由的公钥可用性
   - 基于真实路由返回 `READY / NOT_READY / UNDELIVERABLE`
   - READY 场景下再进入正文自动加密发送
3. `Mail E2EE message encryption`
   - 写信页在浏览器内对正文执行 OpenPGP 加密
   - 邮件详情页本地解密正文
   - 列表预览不再直接暴露 armored 密文
4. `Zero-knowledge roadmap`
   - 形成零知识演进路线，不在本批伪装成已交付
5. `SMTP / IMAP / Bridge discovery`
   - 形成协议栈接入策略、阶段拆分与依赖评估

### P1 - 平台完整性
1. `PWA baseline`
   - 已落地 manifest、Service Worker、安装入口与设置面板
2. `Web Push readiness`
   - 仅保留 readiness 入口，不承诺真实推送下发
3. `Pass extension / native clients`
   - 进入后续版本，不纳入当前批次

### P2 - 产品深度
1. `Mail`
   - 当前主线已推进到正文加密发送 + 详情本地解密
   - 后续才进入外部收件人加密发送、追踪器屏蔽、自定义域名
2. `Drive`
   - 后续进入客户端加密上传、版本历史深化、多端同步
3. `Docs / Sheets`
   - 继续维持现有 `Beta` 主路径，不在本批承诺实时协作引擎

### P3 - 商业化与企业能力
- 支付闭环、SSO、SCIM、LDAP、商业 SLA 不纳入 `v1.2` 当前批次。
- 只保留 roadmap 与 capability boundary 的真实口径，不制造“即将上线”的错觉。

### P4 - 工程化与生态
- `i18n` 工程化、a11y、公开 API 文档站、第三方安全审计属于后续主线。
- 当前只在必要范围内补齐与 `Mail E2EE` 和 capability honesty 直接相关的文案、测试和边界说明。

## `v1.2` 主线流
| Stream | 主题 | 状态 | 本轮说明 |
|---|---|---|---|
| A | `Mobile / PWA baseline` | `Done` | 已落地 PWA 壳层、安装入口、测试与本地门禁。 |
| B | `Capability honesty & boundary cleanup` | `Done` | 已收口 Community 对外口径，避免把未实现能力写成已上线。 |
| C | `Mail E2EE foundation + recipient discovery + message encryption` | `Done` | foundation、recipient discovery、正文加密发送与详情本地解密已落地并完成验证。 |
| D | `Zero-knowledge / protocol discovery` | `Done` | 已补零知识路线与协议栈 discovery 文档，明确阶段拆分与非目标。 |
| E | `Adoption readiness` | `Done` | 已交付 settings adoption panel、OpenAPI / Swagger UI 入口、自托管 install / runbook 快速页与文档增强；更广的 a11y 扩面仍留待后续。 |

## 当前批次范围冻结
### 必须完成
1. `Mail E2EE key management foundation` 设计文档
2. 后端 `mail key profile` 存储与读取 API
3. 前端设置页 `Mail E2EE foundation` 面板
4. 客户端真实生成密钥对并保存 profile
5. READY 路由下的正文加密发送与详情本地解密
6. `Zero-knowledge roadmap` 文档
7. `SMTP / IMAP / Bridge discovery` 文档
8. 前后端最小测试与验证链

### 明确不做
1. 附件加密发送
2. 跨外部用户公钥发现与消息解密
3. 真正零知识架构
4. `SMTP / IMAP / Bridge` 实现代码
5. `Drive` 客户端加密上传
6. `Web Push` 下发、原生客户端、Pass 浏览器扩展
7. 支付闭环、企业目录、SSO / SCIM / LDAP

## 批次目标定义
### 目标成品
- 用户可以在设置页生成自己的 `Mail E2EE` key profile。
- 系统可以保存并再次读取：
  - 公钥
  - 加密私钥包
  - 指纹
  - 算法
  - 创建时间
- 页面明确标注这是 `foundation`，不是完整邮件端到端加密。
- READY 的内部路由发送会在浏览器内自动加密正文。
- 邮件详情页支持本地解密加密正文。
- 零知识路线与协议栈 discovery 已有明确文档，不再停留在 README 口头描述。

### 非目标说明
- 当前批的价值在于把“隐私优先”从纯文案推进到真实密钥管理 + 正文加密闭环，并把后续零知识 / 协议栈工作拆成可执行路线。
- 当前批仍不等于“MMMail 已具备 Proton 级完整 Mail E2EE”或完整外部协议栈。

## 版本门禁
### 进入后端开发前
- `mail-e2ee foundation` 文档已冻结
- 数据存储方案与 API 边界已明确

### 进入前端开发前
- 后端字段、DTO、接口完成
- 设置页入口和 i18n key 已定义

### 批次完成条件
- 文档、后端、前端、测试四类交付物全部存在
- 前端 `typecheck + vitest` 通过
- 后端目标测试在 `timeout 60s` 约束下通过

## 后续版本入口
- `v1.2.next`: 附件加密、草稿加密、Drive client-side encryption 设计、Web Push discovery
- `v1.3+`: 协议栈实现、Bridge、支付闭环、企业能力按独立设计评审进入
