# Mail E2EE Message Encryption

**版本**: `v1.2`
**日期**: `2026-04-01`
**作者**: `Codex`

## 目标
- 在已完成的 `Mail E2EE foundation` 与 `recipient discovery` 之上，补齐“正文加密发送 + 详情本地解密”的最小真实闭环。
- 保证密文只在前端浏览器内生成，服务端只接收密文与显式加密元数据。
- 让收件箱、发件箱都能通过同一份用户私钥本地解密。

## 当前边界
- 只处理 **邮件正文**，不处理附件。
- 只对 `recipient discovery = READY` 的 MMMail 内部路由启用自动加密发送。
- 草稿仍按现状保存，不在本批改造成加密草稿。
- 不实现零知识、服务端解密、SMTP / IMAP / Bridge。

## 发送链路
1. `/compose` 输入收件人后，前端通过 `GET /api/v1/mails/e2ee-recipient-status` 查询路由就绪状态。
2. 若 `encryptionReady = true`：
   - 前端再读取当前账号的 `Mail E2EE key profile`
   - 使用收件路由公钥 + 当前账号公钥，在浏览器内对正文执行 OpenPGP 加密
   - 发送请求只提交：
     - `e2ee.encryptedBody`
     - `e2ee.algorithm`
     - `e2ee.recipientFingerprints`
   - 不再把明文 `body` 一起上传
3. 后端再次基于真实投递路由校验：
   - 收件路由仍然 `READY`
   - 当前账号也有 key profile
   - `recipientFingerprints` 与当前路由 + 发送者指纹集合一致
4. 校验通过后，服务端持久化：
   - `body_ciphertext`
   - `body_e2ee_enabled`
   - `body_e2ee_algorithm`
   - `body_e2ee_fingerprints_json`

## 读取链路
1. 邮件详情接口返回：
   - `body`（若已加密，则为 armored ciphertext）
   - `e2ee` 元数据
2. 详情页检测 `mail.e2ee?.enabled = true` 时，不直接展示密文。
3. 用户输入私钥口令后，前端从设置接口读取已加密私钥包，在本地：
   - 解锁私钥
   - 解密 `body`
   - 展示明文结果
4. 口令只保留在当前页面内存，解密成功后立即清空。

## 预览策略
- 邮件列表与会话摘要不再直接显示 armored 密文。
- 加密邮件统一返回占位预览：`Mail E2EE encrypted body`。
- 这样可以避免列表、搜索结果、会话流中出现大段密文噪音。

## 风险与取舍
- **草稿仍为明文**：这是当前批次有意保留的边界，后续若推进零知识草稿需独立设计。
- **正文搜索能力下降**：密文消息无法继续按正文内容搜索，这是引入客户端加密后的正常取舍。
- **发送失败显式暴露**：收件路由查询失败、缺少发送者 key profile、指纹不匹配时，直接中断发送，不做静默降级。
