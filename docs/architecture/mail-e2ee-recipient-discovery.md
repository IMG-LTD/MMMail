# Mail E2EE Recipient Discovery

**版本**: `mail-e2ee-recipient-discovery-v1`
**日期**: `2026-04-01`
**作者**: `Codex`

## 目标
- 在现有 `Mail E2EE key profile foundation` 之上，补齐“收件人是否具备公钥”的真实查询链。
- 让 `/compose` 能基于真实投递路由显示 `encryption readiness`，为后续消息加密发送做前置接线。

## 非目标
- 不实现正文或附件的自动 OpenPGP 加密发送。
- 不实现写信页私钥解密、签名或 passphrase 会话缓存。
- 不把 recipient discovery 对外表述为“完整 E2EE 已上线”。

## 设计原则
- 收件人 readiness 必须复用真实投递路由，不能另写一套“邮箱存在性猜测”逻辑。
- 公钥属于可分发资料；服务端可以返回公开的 armored public key，但不能返回私钥或 passphrase。
- UI 必须明确区分：
  - `route deliverable`
  - `public key ready`
  - `message encryption not shipped yet`

## 路由解析模型
### 输入
- `toEmail`
- `fromEmail`（可选；若提供则必须通过当前发送身份授权校验）

### 解析步骤
1. 规范化收件地址。
2. 若当前 sender 是受控 alias，则按现有 reverse-alias 规则解析。
3. 复用 `MailDeliveryRouteService` 预览最终投递目标：
   - 直投内部用户
   - 经过 `Pass alias` relay 的一个或多个 mailbox
4. 对每个投递目标查询其 `Mail E2EE key profile`。

### 输出
- 当前地址是否可投递。
- 当前投递路由是否全部具备公钥。
- 每个投递目标的：
  - `targetEmail`
  - `forwardToEmail`
  - `keyAvailable`
  - `fingerprint`
  - `algorithm`
  - `publicKeyArmored`

## API 边界
### `GET /api/v1/mails/e2ee-recipient-status`
- 返回单一收件地址的 E2EE readiness。
- 响应分三类：
  1. `READY`：可投递且所有投递目标都具备公钥。
  2. `NOT_READY`：可投递，但至少一个投递目标缺少公钥。
  3. `UNDELIVERABLE`：当前地址无法通过 MMMail 投递。

## 前端行为
### `/compose`
- 监听 `toEmail / fromEmail` 变化，延迟查询 recipient readiness。
- 若地址为空或格式非法，不发起查询。
- 根据 readiness 展示：
  - 成功提示：路由已具备公钥
  - 警告提示：路由可投递但未全部具备公钥
  - 错误提示：当前地址不可投递
- 所有提示都必须附带边界文案：`message encryption is not automatically applied yet`

## 安全边界
- recipient discovery 只返回公钥资料，不返回私钥资料。
- preview 查询不应复用真实发送审计事件，避免把输入过程污染为“发信事件”。
- 不在前端缓存 passphrase，不在服务端保存或推断 passphrase。

## 后续演进
### Phase 2
- 用 recipient discovery 返回的 public keys 做真正的 OpenPGP message encryption。
- 允许单收件人与 alias 多路由的多公钥加密。

### Phase 3
- 建立已发送邮件的可读副本策略：
  - 自加密给 sender 自己
  - 或建立 sender decrypt session

## 本批完成条件
- 写信页能实时展示 recipient E2EE readiness。
- readiness 查询复用真实投递路由。
- 前后端测试能覆盖 `READY / NOT_READY / UNDELIVERABLE` 主路径。
