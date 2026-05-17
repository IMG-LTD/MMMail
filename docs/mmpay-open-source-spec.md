---
name: MMPay open-source payment gateway spec
date: 2026-05-17
spec_version: mmpay-v0.1
based_on:
  - docs/v22-open-source-commercial-spec.md (oss-comm-v1.97)
  - docs/v22-ga-closure-spec.md (ga-v3.0)
  - docs/v22-external-evidence-checklist.md (2026-05-17)
  - docs/billing/private-billing-evidence-template.md
  - docs/billing/webhook-signature.md
  - docs/commercial/pricing-boundaries.md
target_repository: git@github.com:IMG-LTD/MMPay.git
status: draft / not-implemented
release_gate: 与 MMMail v2.2 full GA 强耦合，按本 spec 推进；未实现前 MMMail 私有计费证据保持 external/deferred
target_audience: maintainer / MMPay 贡献者 / MMMail 集成方 / 外部审阅人
---

# MMPay Open-Source Payment Gateway Spec

本 spec 描述把 MMMail 计费侧从"私有 billing gateway"调整为"独立开源支付网关 MMPay + 商户自托管密钥"的整体方案。它必须与 `docs/v22-ga-closure-spec.md`、`docs/v22-external-evidence-checklist.md`、`docs/billing/private-billing-evidence-template.md`、`docs/commercial/pricing-boundaries.md` 保持自洽。

## 0. 决策摘要

- MMPay 是 `IMG-LTD/MMPay` 下的独立开源项目，定位为「多家三方支付统一网关」，不绑定 MMMail。
- MMPay 是一个完全独立的仓库，不能与 MMMail 源码混在一个工作目录；本地克隆路径默认为 `/home/xiang/桌面/project/MMMail-test/MMPay`，与现有 `/home/xiang/桌面/project/MMMail-test/MMMail` 平级并列，具体约束见 §3.0。
- MMPay 不在源码中携带任何商户私钥、支付私钥、license 签发私钥；这些只能由自托管运营方通过环境变量、secret file 或外部 KMS 注入。
- MMPay 首批接入「汇付（Huifu）」一家，借助 `huifurepo/dg-payment-skills` 的 AI skill 包加速。其余 provider 通过 adapter 接口扩展。
- MMPay 最终对接到 MMMail：MMMail 仍只消费 webhook 状态，不在主仓内实现支付，`docs/billing/webhook-signature.md` 保持权威。
- **MMPay 不承担 MMMail license 签发**。MMPay 只产出「某笔订单已付款 / 已退款」事实事件；MMMail Pro / Business license 的签发仅由 IMG-LTD 厂商私有运维的 license issuer 服务完成，该服务不在 MMPay 开源仓里、不以任何形式随 MMPay 镜像发布。详见 §11。
- 因为 MMPay 是开源仓库，原来"私有 billing 仓库 = 私有"的隐含前提需要在 MMMail 侧调整：v2.2 完成度评估里"private billing repository accessible"的检查目标，从「`IMG-LTD/mmmail-billing-gateway` 私有可访问」改成「`IMG-LTD/MMPay` 公开可见 + 商户私钥/Provider 凭证/license 签发私钥由厂商独立持有」。具体调整方式见 §7。

## 1. 范围与不做的事

### 1.1 在范围内

- 独立仓库 `IMG-LTD/MMPay` 的目录结构、技术选型、licensing、CI、release 策略。
- 网关核心域：商户（merchant）、渠道（channel/provider）、订单（order/payment-intent）、支付结果（transaction）、退款（refund）、回调签名、幂等、对账。
- 第一家 provider：汇付（Huifu）。
- Admin UI：基于 soybean-admin 的运营管理后台。
- 与 MMMail 的契约：webhook 协议、license 交付通道（只转发，不签发，详见 §11）、订阅 / 计费状态同步。

### 1.2 明确不做

- 不在 MMPay 源码里写真实商户私钥、支付私钥、license 签发私钥、客户个人数据。
- **不在 MMPay 中实现 MMMail license 签发**。不提供 license 签发的开源参考实现、不提供 license 签发用的私钥生成资源、不提供「运营方在本地部署后即可签发」的任何路径。license issuer 是 IMG-LTD 厂商私有服务，不随 MMPay 仓、不随 MMPay 镜像、不随 MMPay Helm chart 发布。
- 不在 MMPay 内做 MMMail 的业务特性；MMPay 是通用网关，MMMail 只是其中一个上游消费者。
- 不在 MMPay 内绕过 `docs/billing/webhook-signature.md` 定义的 webhook 协议。
- 不重复实现 `pig` 已经覆盖的鉴权、网关、配置中心、用户体系；优先复用，必要时只做最小裁剪。
- 第一阶段不接入除汇付外的其他 provider；接口预留，但落地按需排期。

## 2. 技术选型与许可证审查

### 2.1 后端：pig

- 来源：`https://gitee.com/log4j/pig`
- 形态：Spring Cloud Alibaba + Spring Security OAuth2 微服务脚手架。
- 优势：自带 RBAC、网关、配置中心、代码生成；与 MMMail 主仓 Spring Boot 技术栈兼容。
- 落地约束：MMPay 是单一应用域，不需要把 pig 全部微服务拆开；必须给出最小可运行子集（auth + admin + gateway-core），并在 README 中明确禁用的模块。
- 许可证：必须在 P-0 期间核验 pig 的 LICENSE；若与开源商业化兼容，则在 MMPay 顶层 LICENSE 同步说明。若发现 pig 主体或其子模块为 GPL/LGPL/AGPL，必须更换或剥离相关模块，避免污染 MMPay 顶层许可证。

### 2.2 前端：soybean-admin

- 来源：`https://github.com/soybeanjs/soybean-admin`
- 形态：Vue 3 + TypeScript + Vite + Pinia + Naive UI / Element Plus 的中后台模板。
- 落地约束：MMPay 不是邮件类产品，UI 风格、布局、品牌色必须与 MMMail `frontend-admin` 解耦；不复用 MMMail 的视觉系统。
- 许可证：必须在 P-0 期间核验 soybean-admin 的 LICENSE（社区版通常 MIT），并在 MMPay 前端 `package.json` 中保留 attribution。

### 2.3 第一家 Provider：汇付 + dg-payment-skills

- 来源：`https://github.com/huifurepo/dg-payment-skills`
- 用法：作为 AI 接入加速包，不直接编译进 MMPay；MMPay 内的汇付 adapter 通过官方 SDK 或 HTTP 协议接入，skill 包用于辅助代码生成、协议解释、回调字段对齐。
- 许可证 / 商标：必须明确 dg-payment-skills 与汇付官方 SDK 的许可证、商标使用边界；汇付商户编号、密钥、证书一律不入仓。
- 隔离要求：`mmpay-adapter-huifu` 模块必须可独立打包、可独立失败；任何 provider 接入失败时网关返回显式错误，不得回落为 mock 成功。

### 2.4 不做的技术选择

- 不引入 Node 后端、Go 后端、Rust 后端来执行支付主流程；后端统一在 pig（Java/Spring）。
- 不引入第二套前端框架；UI 统一在 soybean-admin。
- 不引入第二套数据库；默认 MySQL/PostgreSQL，在 README 中标记官方支持矩阵。

## 3. 仓库与目录结构

### 3.0 本地工作目录隔离

MMPay 是独立仓库，不能作为子目录、submodule 或 monorepo 包含在 MMMail 工作区内。

- **项目根路径**：两个仓库在本地平级并列，位于同一个背景目录 `/home/xiang/桌面/project/MMMail-test/` 下：
  - MMMail：`/home/xiang/桌面/project/MMMail-test/MMMail`（已存在）
  - MMPay：`/home/xiang/桌面/project/MMMail-test/MMPay`（新建）
- **克隆命令**：
  ```bash
  cd /home/xiang/桌面/project/MMMail-test/
  git clone git@github.com:IMG-LTD/MMPay.git
  ```
- **禁止的布局**：
  - 不允许 `MMMail/MMPay/`、`MMMail/external/MMPay/` 等嵌套路径。
  - 不允许在 MMMail 仓库中添加 `MMPay` 作为 `git submodule` 或 `git subtree`。
  - 不允许用软连接、硬连接把 MMPay 源码映射进 MMMail 工作区。
  - 不允许在 MMMail 的 `pom.xml`、`pnpm-workspace.yaml`、`docker-compose.yml` 或 `Helm chart` 中直接引用 MMPay 的本地路径。
- **原因**：
  - MMMail 和 MMPay 有各自独立的版本号、release 节奏、CI 门禁、许可证 NOTICE 和供应链告警。混在一个工作区会让 `frontend-admin`、Flyway 迁移命名、供应链 lockfile、legacy frontend freeze 两边互相污染。
  - MMMail 仓库的 governance contract、root tests、`scripts/validate-local.sh` 只扫 MMMail 路径下的文件；如果误把 MMPay 嵌进来，会被错误列入治理范围或被错误忽略。
  - 两个仓库的 AGENTS.md 边界不同：MMMail 的 AGENTS.md 仅适用 MMMail；MMPay 需要在自己的仓库根下维护 AGENTS.md。
- **跨仓协作**：只能通过以下方式：
  - HTTP API + webhook（运行时）
  - 发布后的镜像 / Maven artifact / npm package（依赖时）
  - 跨仓 PR（文档同步时，例如 MP-9 阶段同时提 MMMail 主仓 PR 与 MMPay 仓 PR，各自走自己的 review）

### 3.1 MMPay 仓库内部布局

`IMG-LTD/MMPay` 默认布局（首期）：

```
MMPay/
  README.md                          # 项目定位、安装、配置、provider 矩阵
  LICENSE                            # MMPay 顶层许可证（待 P-0 决定，建议 Apache-2.0）
  NOTICE                             # 第三方依赖 attribution
  SECURITY.md                        # 安全报告通道
  SUPPORT.md
  GOVERNANCE.md
  AGENTS.md                          # MMPay 自己的 AI/Agent 协作规则
  docs/
    architecture/
      overview.md                    # 网关分层架构
      adapter-contract.md            # provider adapter SPI 契约
      webhook-protocol.md            # MMPay→上游 webhook 协议（与 MMMail webhook-signature.md 对齐）
      idempotency.md
      data-model.md
    providers/
      huifu.md                       # 汇付接入细节、字段映射、沙箱配置
    integrations/
      mmmail.md                      # 与 MMMail 的对接契约（订阅、状态、license）
    ops/
      install.md
      upgrade.md
      backup-restore.md
      runbook.md
    security/
      threat-model.md
      key-management.md
    release/
      release-process.md
      image-publishing.md
  backend/
    pom.xml
    mmpay-bom/                       # 版本与依赖治理
    mmpay-common/                    # 通用 DTO / 工具
    mmpay-gateway-core/              # 订单、支付意图、状态机、幂等、对账
    mmpay-adapter-spi/               # provider adapter 接口
    mmpay-adapter-huifu/             # 汇付 adapter（第一家）
    mmpay-webhook-out/               # 向上游（如 MMMail）签发「付款事实」webhook（不含 license 签名逻辑）
    mmpay-admin-api/                 # admin 后端 API（被前端调用）
    mmpay-app/                       # 启动入口、配置加载、健康检查
    # 注意：本仓不会出现 mmpay-license-signer 模块。license 签发不是开源 MMPay 的职责，不能以任何形式出现在本仓。
  frontend-admin/                    # soybean-admin 定制
    package.json
    pnpm-lock.yaml
    src/
    ...
  deploy/
    docker-compose.yml               # 自托管最小可运行栈
    docker-compose.minimal.yml
    helm/                            # 可选 Helm chart
  scripts/
    validate-local.sh                # MMPay 本地总门禁
    validate-ci.sh
    release-gate.sh
    check-migration-naming.sh
    security-secret-scan.sh
  .github/
    workflows/
      ci.yml
      images.yml
      release.yml
      dependabot-mirror.yml
```

要求：

- 单一职责分模块；adapter 模块不得直接依赖 gateway-core 的实现细节，只能依赖 adapter-spi。
- 函数默认 ≤ 50 行，源码文件默认 ≤ 500 行，超限需要拆模块或加 governance allowlist。
- 任何包含真实凭证的目录都必须默认 ignored；模板里只能放 `replace-with-*` 占位符。

## 4. 架构与核心域

### 4.1 分层

```text
+--------------------------------------------------------------+
|  上游消费者 (MMMail / 其他产品)                                |
+--------------------------------------------------------------+
            ▲                                ▲
            |  Webhook (signed)              |  REST API
            |                                |
+--------------------------------------------------------------+
|  mmpay-webhook-out         mmpay-admin-api / public-api      |
+--------------------------------------------------------------+
|  mmpay-gateway-core: order / payment-intent / refund          |
|                     state machine / idempotency / ledger      |
+--------------------------------------------------------------+
|  mmpay-adapter-spi (interface)                                |
|     ├─ mmpay-adapter-huifu                                    |
|     └─ (future) mmpay-adapter-stripe / wechatpay / alipay     |
+--------------------------------------------------------------+
|  Provider HTTP API / SDK                                      |
+--------------------------------------------------------------+
```

### 4.2 核心域对象

- `Merchant`：自托管运营方的商户身份，持有 provider 凭证引用（不存明文）。
- `Channel`：商户在某个 provider 下的具体渠道配置（如汇付的某产品号、终端号）。
- `PaymentIntent`：上游创建的支付意图，含 amount、currency、orderRef、idempotencyKey。
- `Transaction`：provider 实际产生的交易记录，幂等绑定到 `PaymentIntent`。
- `Refund`：基于 Transaction 的退款，强幂等。
- `WebhookEvent`：provider→MMPay、MMPay→上游两类事件，全部带签名、版本、时间窗。`payload` 只携带「订单状态 + 付款金额 + 商户账单 ID」这类“付款事实”，**不携带 edition / seats / features 等 license claims 字段**，以避免被误用作授权凭证。
- （本仓不出现）`LicenseClaim`：MMMail license claims 数据结构由 MMMail 仓 `contracts/license/license-claims.schema.json` 维护，MMPay 不复制、不生成、不签发该类型实例。

### 4.3 Adapter SPI

`mmpay-adapter-spi` 定义最小接口：

- `createPayment(PaymentIntent) → ProviderRedirectOrChallenge`
- `queryPayment(providerOrderId) → ProviderPaymentStatus`
- `refund(providerOrderId, RefundRequest) → ProviderRefundResult`
- `verifyInboundWebhook(headers, rawBody) → ProviderEvent`（解析 + 校验签名）
- `capabilities() → Set<Capability>`（说明该 provider 支持哪些能力）

约束：

- 所有方法对失败必须显式抛出/返回明确错误码；禁止 silent fallback。
- adapter 不持有数据库，状态写回 gateway-core。
- adapter 不直接调用上游 webhook，只通过 webhook-out 模块发出。

## 5. 与 MMMail 的对接契约

### 5.1 不变的部分

- `docs/billing/webhook-signature.md` 仍是 MMMail 接收 webhook 的权威协议。MMPay 的 `mmpay-webhook-out` 必须能产出符合该协议的 `X-MMMail-Billing-Signature: v1=<hex-hmac-sha256>` 签名事件。
- 共享密钥 `MMMAIL_BILLING_WEBHOOK_SECRET` 由 MMMail 自托管运营方与 MMPay 自托管运营方协商（多数情况下是同一个运营方）。
- 幂等键沿用 `eventId`。
- 5 分钟时间窗约束保持不变。

### 5.2 新增的部分

- MMPay 暴露面向 MMMail 的 REST API（创建支付意图、查询状态、发起退款），路径前缀 `/api/v1/`。
- MMMail 调用 MMPay 时通过 mTLS 或 bearer token，token 不入仓。
- **License 签发不由 MMPay 完成**。完整链路拆为两段（详见 §11）：
  1. MMPay 在付款成功后只向 IMG-LTD 厂商私有运营的 **license issuer 服务**（不在 MMPay 仓、不在 MMMail 仓）推送「付款事实」webhook。
  2. license issuer 服务用厂商私钥签发 license，直接以厂商身份向客户部署的 MMMail 推送 license（或签发后由客户手动上传到其 MMMail）。
  - 运行中 **MMPay 从不接触 license 私钥，不持有 license 签名能力**。
  - 客户部署的 MMPay 实例可能与厂商 license issuer 根本不联网运行（例如严格内网部署），那种场景下客户获取 license 的唯一路径是、也仅能是厂商离线签发。

### 5.3 文档同步要求

MMPay 落地后，MMMail 主仓需要同步更新以下文件，且这些更新必须保持当前 v2.2 GA 的「失败显式可见」原则：

- `docs/v22-open-source-commercial-spec.md`：把"私有 billing 仓库"段落改为"独立开源 MMPay + 商户私钥外置"叙述。
- `docs/v22-ga-closure-spec.md` §0.3、§F-3：把"Private billing evidence `Public MMMail repository commit SHA`"的同源仓库从 `IMG-LTD/mmmail-billing-gateway` 改为 `IMG-LTD/MMPay`，并增加"商户凭证、provider 私钥、license 签发私钥仍在公开仓库之外"这条强约束。
- `docs/billing/private-billing-evidence-template.md`：把"Billing repository URL"语义从「私有仓库 URL」改为「MMPay 仓库 URL + 商户密钥外置证据」，并把"License signing key location"明确为 **IMG-LTD 厂商 KMS**的句柄（不是运营方本地环境里的 KMS / secret file）。
- `scripts/validate-v22-external-evidence.sh`：`gh repo view IMG-LTD/mmmail-billing-gateway` 改为 `gh repo view IMG-LTD/MMPay`；同时新增对「商户密钥外置」的证据校验入口（例如要求证据文件包含 `Merchant credentials externally managed: <KMS reference>` 等非空字段）。修改 verifier 时不得弱化「private billing remains external 直到证据齐」的整体语义。
- `docs/commercial/pricing-boundaries.md`：保留"merchant credentials / payment provider private keys / license signing private keys 不入主仓"红线；新增"也不入 MMPay 仓"对称约束。

### 5.4 不允许的简化路径

- 不允许把 MMPay 当作 mock provider，在 MMMail 内伪造支付成功。
- 不允许在 MMPay 仓里建一个"demo merchant"，自动放行 paid 状态。
- 不允许在 MMMail 仓里直接 import MMPay 模块；必须通过 HTTP API + webhook 边界通讯。
- **不允许让 MMPay 产出「签名后的 license claims」**：MMPay 的出站 webhook 只可以携带「订单付款状态」，不得携带 `edition / seats / features / issuedAt / expiresAt` 等任何 license claims 字段。如果未来需要 license，应由 §11 定义的厂商 license issuer 独立签发。
- **不允许 MMMail `MMMAIL_LICENSE_PUBLIC_KEY` 的轮换或信任路径被 MMPay 接管**：厂商公钥的发布、轮换、吊销只能随 MMMail release artifact 走，不能从 MMPay 动态注入。

## 6. 推进阶段（Phases）

阶段编号与 MMMail v22 spec 不冲突；MMPay 自己另起 P 系列。

### MP-0：仓库与许可证奠基

- 在本地 `/home/xiang/桌面/project/MMMail-test/` 下 `git clone git@github.com:IMG-LTD/MMPay.git`，确保与 MMMail 平级并列，不进入 MMMail 工作区（参见 §3.0）。
- 在 `IMG-LTD/MMPay` 推首个 commit：LICENSE、NOTICE、README、SECURITY.md、SUPPORT.md、GOVERNANCE.md、AGENTS.md、CODE_OF_CONDUCT.md、CONTRIBUTING.md、.gitignore。
- 完成 pig、soybean-admin、dg-payment-skills 三家许可证审查；不兼容则替换或剥离。
- 建立空 backend / frontend-admin 骨架（可启动，空业务）。
- 在 MMPay 的 AGENTS.md 中纳入「不可以任何形式被嵌套进 MMMail 工作区」这条边界约束。

### MP-1：pig 子集裁剪

- 引入 pig 必要模块（auth、admin、gateway-core 雏形），禁用不必要的微服务。
- 给出最小 docker-compose：MySQL/PostgreSQL + Redis + Nacos（如保留）+ mmpay-app。
- 启动健康检查通过；提供 `scripts/validate-local.sh` 雏形。

### MP-2：核心域与状态机

- 实现 PaymentIntent / Transaction / Refund 状态机、幂等键、对账表结构。
- Flyway 迁移脚本接入，版本号全局唯一连续。
- 单元测试覆盖核心域；CI 跑通。

### MP-3：Huifu adapter MVP

- 借助 `dg-payment-skills` 完成汇付 adapter：createPayment / queryPayment / refund / verifyInboundWebhook。
- 沙箱环境跑通一笔完整支付（下单 → 支付 → 回调 → 状态写回）。
- adapter 在凭证缺失、签名错误、时间窗过期、provider 异常时全部显式失败，不回落 success。

### MP-4：Webhook-out 与上游契约

- 实现 mmpay-webhook-out：按 `docs/billing/webhook-signature.md` 协议向上游推送签名事件。
- 在 MMPay 内部完成回放测试：同一 eventId 再次到达必须返回 duplicate，不重复推送。

### MP-5：Admin UI

- soybean-admin 集成：商户管理、渠道配置、订单查询、退款发起、回调日志、对账下载。
- UI 不展示任何明文凭证，凭证只能引用 secret 句柄。
- i18n 至少覆盖 zh-CN / en-US。

### MP-6：退款 / 发票 / 对账生命周期

- 退款全链路；发票字段（如 provider 支持）；按日 / 按月对账。
- 不出做不到的承诺：未对接 provider 发票能力时，明确写「unsupported by current provider」。

### MP-7：License 交付通道（不签发，只投递）

MMPay 在本阶段只实现「license 交付通道」，不实现任何 license 签名逻辑。

- 实现 `mmpay-license-relay`（可选模块）：接收厂商 license issuer 产出的**已签名**license，转发给客户部署的 MMMail。转发过程不重签、不拆包、不加字段。
- **不实现** `mmpay-license-signer`（spec 中也不允许后期添加）。任何能让运营方“拿私钥签 license”的代码都不能出现在 MMPay 仓。
- 转发时 MMPay 只能看到 license 的 opaque bytes，不反序列化 claims、不读 edition / seats / features 等字段。
- 转发通道本身的可用性不是 license 本身的可用性。客户可以完全跳过 MMPay relay，直接从厂商 license issuer 获取 license 并手动上传到 MMMail。

### MP-8：MMMail 集成 e2e

- 在受控自托管环境内：部署一套 MMMail（任意 v2.2 候选 commit）+ 一套 MMPay + 一个 Huifu 沙箱商户。
- 跑通 MMMail Pro / Business 订阅的完整 happy path 与至少一条失败路径（签名错、过期、provider 失败、双花重放）。
- 产出 redacted 证据包：commit SHA、provider event ID、webhook event ID、时间戳、license claim ID（如有）。

### MP-9：MMMail v2.2 GA closure 对接

- 用 MP-8 的证据包填写 MMMail 主仓 `docs/billing/private-billing-evidence.md`，所有字段非空、commit 同源、provider 非 `none`。
- license signing key location 必须记录为「IMG-LTD vendor KMS」类型的句柄，明确不是 MMPay 运营方环境里的 key store；verifier 需拒收「self-operator KMS」形状的 license signing key 证据（详见 §7.2）。
- 同步更新 §5.3 列出的 MMMail 主仓文档与 verifier。
- 再跑 `bash scripts/validate-v22-external-evidence.sh`，达成 exit code 0。

## 7. 对 MMMail v2.2 GA closure 的影响评估

### 7.1 兼容性结论

- 把 billing 仓从私有改成开源 MMPay，**不会**削弱 v2.2 当前的失败显式可见原则，前提是 §5.3 列出的文档与脚本同步更新，且商户凭证 / provider 私钥 / license 私钥继续外置。
- 在 MP-9 完成前，MMMail 主仓的 `audit_status` 必须保持 `not-complete-external-evidence-required`；`scripts/validate-v22-external-evidence.sh` 必须保持期望失败。
- `docs/v22-ga-closure-spec.md` §0.1 的全部非协商规则继续生效；不允许通过引入 MMPay 来绕过 private billing evidence 检查。

### 7.2 需要在 MMMail 主仓改动的文件清单

- `docs/v22-open-source-commercial-spec.md`
- `docs/v22-ga-closure-spec.md`
- `docs/v22-external-evidence-checklist.md`
- `docs/billing/private-billing-evidence-template.md`
- `docs/commercial/pricing-boundaries.md`
- `scripts/validate-v22-external-evidence.sh`
- 对应的根 governance / contract 测试

这些改动应在 MP-9 阶段一次性提交，避免文档与脚本中途半改造成 verifier 误判。

### 7.3 在 MMPay 完成前可以先做的事

- 在 MMMail 主仓追加一条 spec 引用：`docs/v22-open-source-commercial-spec.md` 加 1 段说明"billing gateway 将由独立开源项目 MMPay 提供，未交付前 private billing evidence 保持 external"。
- 不允许提前在 MMMail 主仓宣布 MMPay 已可用、已完成或已对接。

## 8. 风险与开放问题

### 8.1 已知风险

- **仓库越界**：如果开发者为了「方便」把 MMPay clone 到 MMMail 子目录，会同时污染 MMMail 的 `git status`、`scripts/validate-local.sh`、legacy frontend freeze、供应链扫描与版本发布节奏。预防措施：MMMail `.gitignore` 黑名单 `^MMPay/`，同时在根 governance contract 中增加「不准出现名为 `MMPay` 的子目录」断言。
- **pig 体量**：pig 是微服务脚手架，对单一支付网关可能过重。需要在 MP-1 给出明确「禁用模块清单」与启动时间、内存基线；若发现裁剪后仍超出自托管常识范围，考虑 fork 出一个 mmpay-only 精简分支或换用 Spring Boot 原生。
- **dg-payment-skills 授权**：必须确认 skill 包及其引用的汇付 SDK / 证书工具链对开源商业化兼容；若不兼容，仅允许在开发者本地使用，不入 MMPay 仓。
- **汇付资质**：汇付商户进件、产品号、终端号属于商户运营资质，MMPay 仓不能预置任何具体值。文档与 demo 只能使用 `replace-with-*` 占位。
- **跨仓证据协调**：MMMail v2.2 GA 评估需要同时引用 MMPay 仓的 commit、Huifu 沙箱事件 ID、MMMail 仓的接收 webhook event ID，三方时间戳、commit、event ID 必须一致；MP-8 必须给出 redacted evidence 包模板。
- **品牌 / 商标**：MMPay、MMMail、Huifu 三方商标使用边界须在 MP-0 写入 `docs/commercial/trademark-policy.md`（MMPay 自己版本）。

### 8.2 待决问题

- license 签发是否由 MMPay 兼任？**已决 — 不兼任**，见 §11。待决问题仅下移为「license issuer 服务的部署形态（SaaS / self-hosted-by-vendor / 离线 CLI）以及与 MMPay 事件流的接入契约」，这部分以后另起独立 spec 记载。
- MMPay 是否提供 Hosted（云）部署形态？默认仅自托管，Hosted 形态留待未来 spec。
- 数据库官方支持矩阵（MySQL 8 / PostgreSQL 14+）需要在 MP-1 确认。
- 多租户隔离策略：单实例多商户共用 schema 还是按租户分 schema？默认共用 schema + tenant_id 列，待 MP-2 评估。

## 9. 验收门禁（MMPay 自己的最低门禁）

| 阶段 | 最低门禁 |
|---|---|
| MP-0 | LICENSE / NOTICE 通过审查；secret-scan 通过；CI 骨架绿 |
| MP-1 | 最小栈可启动；健康检查通过；`scripts/validate-local.sh` 雏形跑通 |
| MP-2 | 核心域单元/集成测试通过；Flyway 迁移连续；命名 lint 通过 |
| MP-3 | Huifu 沙箱 happy path + 至少 3 条失败路径自动化回归通过 |
| MP-4 | 向 MMMail 协议出站 webhook 的契约测试通过；重放幂等用例通过 |
| MP-5 | Admin UI typecheck / lint / e2e 关键路径通过；不出现明文凭证 |
| MP-6 | 退款 / 对账契约测试通过；不支持的能力必须显式声明 |
| MP-7 | License relay 转发合同测试通过；MMPay 仓中不存在任何 license 签名逻辑；governance 脚本静态断言 webhook payload 不含 `edition`/`seats`/`features`/`issuedAt`/`expiresAt` 五个字段 |
| MP-8 | MMMail × MMPay × Huifu 沙箱 e2e 通过；redacted evidence 包齐 |
| MP-9 | MMMail 主仓 `scripts/validate-v22-external-evidence.sh` exit code 0 |

## 10. 不允许的捷径

- 不允许把 MMPay 源码放进 MMMail 仓库、放进 MMMail 子目录、作为 submodule / subtree 嵌套。两仓必须在本地平级并列，位于 `/home/xiang/桌面/project/MMMail-test/` 下的两个同级目录。
- 不允许把 MMPay 包装成"只对 MMMail 可用"以规避通用支付网关的接口约束。
- 不允许把 dg-payment-skills 的 AI 输出直接 paste 入仓而不做审查、不做安全/许可证审计。
- 不允许在 MMPay 仓内放真实商户号、真实密钥、真实证书。
- 不允许在 MMMail 主仓的 v2.2 verifier 中临时跳过 private billing 检查"等 MMPay 完了再加回来"——按 §7.1 与 §7.2 一次性切换。
- 不允许把"汇付支付能跑通"宣传为「MMPay v1 GA」；MMPay 自己的 GA 标准必须涵盖至少一家 provider 完整通过 §9 的 MP-3 → MP-6 门禁，并补齐 ops、security、release 文档。

## 11. License 防破解设计（限制 MMPay 开源后 Pro / Business 被绕过授权的可能）

这里集中回答一个问题：开源 MMPay 之后，是否会让 MMMail Pro / Business 能力在自托管场景下被“低成本破解授权”。

### 11.1 职责拆分

两件事情必须被明确拆开：

- **付款（Payment）**：创建订单、调用 provider、接收回调、推送「某订单 paid」事实。这件事情开源安全，由 MMPay 承担。
- **授权（License signing）**：将付款事实转化为“某客户获得 PRO / BUSINESS 能力”的可验签名凭证。这件事情必须由 IMG-LTD 厂商私有服务进行，**不在任何开源仓中提供参考实现或可运行代码**。

### 11.2 为什么必须这么拆

如果 MMPay 开源仓同时提供可运行的 license signer（哪怕要求“私钥不入仓”），这里会出现一条零门槛攻击路径：

1. 任一运营方本地 `openssl genpkey` 生成一对密钥。
2. 将私钥投到本地 MMPay license signer。
3. 将公钥写到自己 fork 的 MMMail 部署的 `MMMAIL_LICENSE_PUBLIC_KEY`。
4. 本地签任意 `edition: BUSINESS` license，上传、验证通过。

这条路径与有没有 MMPay 无关——但 spec 一旦提供“MMPay license signer”，就等于把这一条路径“化零为零”。反之，只要 license signer 不随 MMPay 发布，需要“拿到 MMMail 尊重的私钥”的零件代码在公开仓中都不存在，运营方要伪造 license 就需要自己重新实现（与任何开源自托管产品本来面临的越狱问题同级）。

### 11.3 另外三道防线（全部保留不动）

MMMail 侧现有设计已有三道防线，本 spec 不动主仓代码但明确依赖于它们：

1. **服务端强制**：`CommercialAuthorizationGate` / `FeatureGate` 是唯一安全边界，前端 `EntitlementGate` 只是 UX hint（`docs/commercial/edition-entitlement-surface.md`）。
2. **subscription state 优先**：运行时 edition 解析顺序是 subscription state → active license → fallback；非付费 subscription 不会被 license 静默覆盖。伪 license 在没有真实 webhook 推送的 paid subscription 时仍解析为 Free。
3. **Webhook 边界**：`POST /api/v2/billing/webhook` HMAC + time window + idempotency + `none` provider 拒绝 paid。这三项与 MMPay 开源与否无关。

### 11.4 MMPay 必须遵守的边界（含在 governance 里）

- 仓中不出现对 `MMMAIL_LICENSE_PUBLIC_KEY` 的生成、轮换、信任注入代码。
- 仓中不出现「generate license signing key」「how to self-sign license」「internal license issuer reference impl」之类文档、脚本、提示词。
- `mmpay-webhook-out` 负载只能是「订单状态 + 金额 + 商户账单 ID + 提供方事件 ID」，不允许包含 `edition` / `seats` / `features` / `issuedAt` / `expiresAt` 字段。`mmpay-webhook-out` 的合同测试必须动态拒绝这五个字段。
- `mmpay-license-relay`（如果实现）只能以 opaque 字节流转发厂商产出的 license，不反序列化内部字段。
- MMPay admin UI 不提供「手动签发 license」「重新签发」「升级 edition」等可视化操作。

### 11.5 MMMail 侧可选加固（不属于本表本次提交范围）

以下措施是可选增强项，是否上身由 MMMail 侧后续权衡，不在本 spec 代付：

- 在 MMMail release artifact 中内置一个 `MMMAIL_VENDOR_TRUSTED_PUBLIC_KEY` 作为额外可验证的“IMG-LTD root”，限定 license issuer 的身份。运营方可以转换公钥为自己的，但这样做仅能驱动本地部署，不得在任何商业界面上被认定为合法授权。
- 可选「激活心跳」：Pro / Business license 启用后，周期性与厂商 license issuer 通信以检查吊销状态。进入该能力以前仅依靠 §11.1–11.4 即足够。

## 12. 下一步

1. 在 `IMG-LTD/MMPay` 仓发起 MP-0 初始化 PR：LICENSE 选型、骨架文件、AGENTS.md、CI 骨架；AGENTS.md 里明记 §11.4 的 governance 边界。
2. 在 MMMail 主仓追加一段引用本 spec 的说明（不更改 verifier），表明 billing gateway 路线调整为开源 MMPay，同时明确 license signer 仍为厂商私有。
3. 按 §7.2 在 MP-9 阶段一次性提交 MMMail 侧的文档与 verifier 切换 PR，避免半切换状态。
