# Mail Protocol Stack Discovery

**版本**: `mail-protocol-stack-discovery-v1`
**日期**: `2026-04-02`
**作者**: `Codex`

## 目标
- 为 `SMTP / IMAP / Bridge` 提供真实的阶段拆分、组件边界与前置依赖说明。
- 解释为什么 `v1.2` 不直接宣称“已具备真实邮件协议栈”，同时给出可执行的后续入口。
- 把协议栈引入与 `Mail E2EE / zero-knowledge` 的耦合关系说清楚。

## 当前基线
### 已有能力
- `Mail` 模块具备 Web-first 的 compose、inbox、labels、attachments、alias / relay、recipient readiness 与正文加密主路径。
- 内部投递与工作区模型已经存在真实后端服务、实体、路由与测试。

### 当前未交付能力
- 不存在可验证的外部 `SMTP` 入站 / 出站协议服务。
- 不存在可验证的 `IMAP` 同步或第三方客户端接入层。
- 不存在 `Bridge` 本地代理或桌面客户端加密适配器。

## 非目标
- 本文档不实现任何新的协议服务代码。
- 不把 discovery 文档包装成“即将上线 SMTP / IMAP / Bridge”的承诺。
- 不在 `v1.2` 内处理完整的域名信誉、反垃圾、MTA 运维和客户端兼容矩阵。

## 设计原则
- **先统一域模型，再暴露协议边缘**：协议层必须复用现有 mail domain，而不是绕开现有服务另起一套存储。
- **先出站，后入站，再兼容客户端**：外部互联应按复杂度逐层推进。
- **E2EE 与协议栈分层推进**：协议接入不能回退当前 Mail E2EE 的安全边界。
- **每阶段都要有可验证 DoD**：不接受“先搭骨架，后面再补”的长期悬空状态。

## 分阶段拆分
### Phase 0 - Discovery（`v1.2` 当前交付）
输出：
1. 协议栈组件边界
2. 数据归一化约束
3. 出站 / 入站 / IMAP / Bridge 的依赖顺序
4. 运维与安全风险清单

### Phase 1 - SMTP outbound adapter
**目标**：让系统具备最小真实外发能力。

建议项：
1. 引入独立的 outbound delivery adapter，而不是把 SMTP 细节塞进现有 `MailService`。
2. 建立待投递队列与重试策略。
3. 接入 `SPF / DKIM / DMARC` 配置校验与域名状态检查。
4. 统一发件 envelope 与当前 alias / relay 路由逻辑。

DoD：
- 可验证外发到受控测试域
- 失败重试与 dead-letter 可观测
- 不破坏当前 Web-first 发送路径

### Phase 2 - SMTP inbound ingestion
**目标**：让系统能接收真实外部邮件并归档到当前 mailbox 模型。

建议项：
1. 建立 inbound listener 或受管 MTA 对接边界。
2. 做 MIME 解析、附件落盘、header 归一化与 anti-abuse 检查。
3. 把外部邮件归一化为当前 `MailMessage` / thread / attachment 结构。
4. 为内部用户、公用 alias、relay 规则建立统一投递入口。

DoD：
- 外部入站邮件能进入当前 inbox
- MIME / 附件解析稳定
- 基础 anti-abuse 与审计字段存在

### Phase 3 - IMAP compatibility
**目标**：在域模型稳定后，为第三方客户端提供读取与基础同步能力。

建议项：
1. 先做只读或受限写能力，避免一次性暴露全量状态变更。
2. 明确 `labels / folders / star / unread` 在 IMAP 中的映射。
3. 为大附件、搜索、线程视图定义兼容策略。
4. 只有在 message store 稳定后才进入客户端兼容测试矩阵。

### Phase 4 - Bridge equivalent
**目标**：为桌面客户端提供本地代理与加密适配，而不是直接让服务端承担零知识解密责任。

前置条件：
1. Mail E2EE 的 sender self-copy、附件加密、密钥恢复与设备授权已成熟。
2. 外部 SMTP / IMAP 边缘已经稳定。
3. 有独立的本地凭据存储与 session 管理方案。

Bridge 组件应承担：
- 本地密钥解锁
- 与远端 API / IMAP / SMTP 的安全适配
- 受控凭据缓存
- 客户端兼容层

## 组件边界建议
### Protocol edge
- 负责 SMTP / IMAP 协议细节、连接状态、认证与错误分类。

### Mail normalization pipeline
- 负责把 MIME、attachments、headers 与当前域模型对齐。

### Delivery / ingest queue
- 负责重试、限流、死信与观测。

### Mail domain services
- 继续承载线程、标签、附件、share / relay、recipient readiness 等核心业务。

### Bridge agent
- 只在本地环境运行，不能变成服务端“代用户解密”的后门。

## 关键风险
### 运维风险
- `SMTP / IMAP` 是长期高运维成本能力，涉及证书、端口、信誉、反垃圾与容量规划。
- 若没有独立的观测与重试体系，协议接入会把当前 Web-first 路径一并拖垮。

### 安全风险
- 入站外部邮件会放大恶意附件、钓鱼、header spoofing 与 abuse surface。
- Bridge 若设计成“服务端缓存用户秘密”，会直接破坏后续零知识路线。

### 产品风险
- 第三方客户端兼容一旦承诺，就会引入大量状态一致性要求。
- 若 `labels / threads / aliases / drafts` 的协议映射没定义清楚，体验会非常割裂。

## 与 `Mail E2EE` 的关系
- 在当前阶段，`SMTP / IMAP / Bridge` 不能被当成 `Mail E2EE` 的替代物。
- 在 zero-knowledge 路线未成熟前，Bridge 不应先于密钥恢复、附件加密与 sender self-copy 进入交付承诺。
- 协议栈 discovery 的价值，是避免未来为了“看起来像邮件服务”而牺牲当前安全边界与域模型质量。

## `v1.2` 结论
- `v1.2` 对 `SMTP / IMAP / Bridge` 的交付是 discovery 文档与阶段拆分。
- 当前版本仍是 `Web-first` 协作套件，不应对外表述为“已具备完整外部邮件协议栈”。
- 后续若进入实现，推荐顺序为：`SMTP outbound` → `SMTP inbound` → `IMAP compatibility` → `Bridge equivalent`。
