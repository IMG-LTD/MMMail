# Mail E2EE Foundation Architecture

**版本**: `mail-e2ee-foundation-v1`
**日期**: `2026-04-01`
**作者**: `Codex`

## 目标
- 为 `Mail` 建立第一条真实的端到端加密基础设施：`key management foundation`。
- 让客户端可以生成 OpenPGP 密钥对，并把公钥与加密私钥包保存到服务端。
- 明确服务端保存的是密钥资料，不是解密能力；当前阶段不提供完整邮件加密发送。

## 非目标
- 不加密邮件正文、附件或草稿内容。
- 不做跨用户公钥发现、密钥信任链或消息解密流程。
- 不宣称当前实现已经达到零知识架构。
- 不接入 `SMTP / IMAP / Bridge`。

## 设计原则
- 密钥对必须在客户端生成，服务端不生成也不持有明文私钥。
- 私钥必须先在客户端用用户输入的 passphrase 加密，再上传服务端。
- 服务端只保存密钥资料与元数据，不能依赖这些数据直接解密用户邮件。
- 所有 UI 必须显式写明 `foundation` 边界，避免误导为“完整 E2EE 已上线”。

## 当前阶段架构
### 客户端职责
1. 用户在设置页输入展示名称、邮箱和保护私钥的 passphrase。
2. 前端使用 `OpenPGP.js` 在浏览器生成密钥对。
3. 前端提取以下数据：
   - `publicKeyArmored`
   - `encryptedPrivateKeyArmored`
   - `fingerprint`
   - `algorithm`
   - `createdAt`
4. 前端通过设置 API 保存 key profile。

### 服务端职责
1. 接收并校验 key profile 负载。
2. 将 key profile 关联到当前登录用户。
3. 提供读取接口给设置页显示当前状态。
4. 不接触用户 passphrase，不尝试解密私钥包。

## 数据模型
### 建议字段
- `mail_e2ee_enabled`
- `mail_e2ee_key_fingerprint`
- `mail_e2ee_public_key_armored`
- `mail_e2ee_private_key_encrypted`
- `mail_e2ee_key_algorithm`
- `mail_e2ee_key_created_at`

### 存储策略
- 当前阶段复用 `user_preference`，避免为 foundation 过早引入独立聚合表与复杂迁移链。
- 字段允许为空；只有用户显式生成并保存后才进入启用态。
- 如果用户重新生成密钥，直接覆盖当前 profile；不在本批做历史版本管理。

## API 边界
### `GET /api/v1/settings/mail-e2ee`
- 返回当前用户的 key profile 摘要。
- 若未初始化，返回 `enabled = false` 与空 profile。

### `PUT /api/v1/settings/mail-e2ee`
- 输入：
  - `enabled`
  - `publicKeyArmored`
  - `encryptedPrivateKeyArmored`
  - `fingerprint`
  - `algorithm`
  - `createdAt`
- 行为：
  - 校验必填字段
  - 保存 profile
  - 返回最新状态

## 安全边界
### 本批已经做到
- 私钥在浏览器端加密后再上传。
- 服务端只保存加密私钥包。
- 前端必须显式要求用户提供保护 passphrase。

### 本批还没做到
- 服务端看不到邮件明文这一层并未建立。
- 用户密码派生密钥、设备间同步、恢复机制尚未建立。
- 密钥吊销、轮换、信任、外部联系人密钥发现尚未建立。

## 风险与限制
### 用户体验风险
- 用户丢失 passphrase 后，当前批次无法恢复原私钥，只能重新生成新密钥。
- 当前阶段没有邮件加密发送能力，用户可能误以为“生成密钥 = 邮件已加密”。

### 工程风险
- `user_preference` 承载大文本字段后，需要注意列类型和迁移兼容性。
- OpenPGP key generation 发生在浏览器端，需关注性能与错误显式暴露。

### 安全风险
- 前端若把 passphrase 写入日志、错误消息或本地存储，会破坏边界；必须禁止。
- 后端若扩展接口时读取或缓存明文 passphrase，会破坏当前 foundation 约束。

## 实施阶段
### Phase 1 - Foundation
- 文档冻结
- 后端 key profile API
- 前端 key generation + settings panel
- 基础测试

### Phase 2 - Message encryption path
- 发件时使用收件人公钥加密邮件内容
- 建立发件失败与缺失公钥提示
- 加入外部联系人加密发送策略

### Phase 3 - Zero-knowledge evolution
- 用户密码派生密钥
- 私钥设备同步与恢复
- 密钥轮换、吊销、信任与审计

## 批次完成条件
- 设置页能真实生成并保存 key profile。
- 服务端接口能读回已保存的 profile。
- 文档与 UI 都明确声明当前能力是 `Mail E2EE foundation`。
- 对应测试可稳定验证主路径。
