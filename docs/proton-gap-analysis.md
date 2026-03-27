# Proton 全产品差距分析（v122）

**版本**: `v122.0`  
**日期**: `2026-03-13`  
**作者**: `Codex`

## 变更记录
- `2026-03-13`：新增 `v122.0`，基于 `2026-03-13` 官方公开页面与本轮 `Suite billing center` 改造结果，刷新 MMMail 与 Proton 当前差距。

## 1. 官方基线（2026-03-13）

### 1.1 参考来源
- `https://proton.me/support/payment-options`
- `https://proton.me/support/manage-payment-methods`
- `https://proton.me/support/invoices`
- `https://proton.me/support/manage-subscription`
- `https://proton.me/business/plans`
- `https://proton.me/meet`
- `https://proton.me/authenticator`
- `https://proton.me/wallet`
- `https://proton.me/drive/sheets`
- `https://proton.me/support/lumo-getting-started`
- `.codex-tasks/20260313-proton-parity-v122/raw/proton.me-payment-options.html`
- `.codex-tasks/20260313-proton-parity-v122/raw/proton.me-manage-payment-methods.html`
- `.codex-tasks/20260313-proton-parity-v122/raw/proton.me-invoices.html`
- `.codex-tasks/20260313-proton-parity-v122/raw/proton.me-upgrade-downgrade.html`

### 1.2 公开结论摘要
- `support/payment-options`、`manage-payment-methods`、`invoices`、`manage-subscription` 共同定义了 Proton 当前公开的账户账单主路径：
  - 支持的付款方式范围
  - 付款方式管理
  - 发票查看
  - 升级、降级、取消和订阅管理
- `business/plans` 当前公开企业计划包括：
  - `Mail Essentials`
  - `Mail Professional`
  - `Proton Business Suite`
  - `Enterprise`
- `meet` 当前公开为 `closed beta`，并强调端到端加密的视频会议定位。
- `authenticator` 当前公开为跨设备同步/备份的 2FA 产品，强调开源、跨平台，且不要求 Proton 账号才能使用。
- `wallet` 当前公开为自托管 Bitcoin 钱包，并强调：
  - self-custody
  - buy Bitcoin
  - Bitcoin via Email
  - 150+ countries availability
- `drive/sheets` 当前公开强调：
  - formulas
  - charts
  - comments
  - real-time collaboration
- `support/lumo-getting-started` 当前公开显示 Lumo 已覆盖 web 与 mobile 起步路径。
- 从官方支持站点产品导航可见的当前产品面包括：
  - `Mail`
  - `Calendar`
  - `Meet`
  - `VPN`
  - `Pass`
  - `Authenticator`
  - `Drive`
  - `Docs`
  - `Sheets`
  - `Lumo`
  - `SimpleLogin`
  - `Standard Notes`
  - 以及企业与账单相关入口

## 2. 对齐等级说明

| 等级 | 含义 |
|---|---|
| `L0` | 无映射 |
| `L1` | 仅有导航或占位 |
| `L2` | 有工作区骨架，但深能力明显不足 |
| `L3` | 有可用主路径，但与官方仍有明显产品化差距 |
| `L4` | 主路径与深能力大体完整，仅剩工程化缺口 |

## 3. 当前映射矩阵

| 官方产品 / 横切能力 | 当前项目映射 | 对齐等级 | 当前状态 | 主要差距 |
|---|---|---:|---|---|
| Plans / Pricing / Billing | `suite` | `L3` | 已支持公开 offer、`Pass Plus / Drive Plus`、`quote -> draft -> billing center -> pending invoice` | 缺真实支付、invoice download、税费、取消/降级结算、独立 checkout funnel |
| Mail | `inbox / unread / sent / compose / labels / filters / easy-switch` | `L3` | 主工作区较完整 | 缺 Bridge、客户端整合、官方级迁移与安全网关 |
| Calendar | `calendar` | `L2` | 有基础日历与共享骨架 | 缺邀请编排、深协作、订阅与 Meet 调度 |
| Drive | `drive` | `L3` | 文件、分享、公开链接与协作骨架已在 | 缺正式照片备份、多端同步、恢复体验 |
| Docs | `docs` | `L2` | 编辑、建议、分享基础已在 | 缺多人实时协作、presence、评论线程与成熟版本治理 |
| Sheets | `sheets` | `L2` | 工作簿、共享、版本和数据工具骨架已在 | 缺公式引擎、图表、实时协作与官方级数据处理 |
| Pass | `pass / pass-monitor / secure-links` | `L3` | 密码、别名、共享和监控主路径已在 | 缺 passkeys 全生命周期、浏览器扩展自动填充、附件和暗网监控深能力 |
| Authenticator | `authenticator` | `L3` | PIN、导入、导出、恢复与安全面板已在 | 仍强依赖主账号体系；缺“无需账号即可用”的独立产品模式与设备级安全封装 |
| VPN | `vpn` | `L3` | quick connect、策略与状态骨架已在 | 缺真实 tunnel、协议、节点与设备级网络能力 |
| Meet | `meet` | `L3` | access rail、guest join、workspace 主路径已在 | 缺媒体引擎、录制、转录、加密密钥编排与 beta/early-access 运营逻辑 |
| Wallet | `wallet` | `L3` | 账户、执行、对账、地址簿主路径已在 | 缺链上数据面、自托管恢复、买币流程、Bitcoin via Email、真实签名与广播 |
| Lumo | `lumo` | `L3` | 已支持受控资料源扫描、翻译输出、引用持久化与三语 UI | 缺官方级模型编排、web/mobile 一致体验、live search、zero-access/no-logs 级实现 |
| SimpleLogin | `simplelogin` | `L3` | alias / contact / mailbox 主路径已在 | 缺 custom domain、catch-all、reverse alias 与正式账号安全体系 |
| Standard Notes | `standard-notes` | `L3` | list / editor / pulse 主路径已在 | 缺 E2EE 同步、文件域、自动备份与离线一致性 |
| Business / Admin | `business / organizations / security / command-center` | `L3` | 套件入口、组织治理与监控骨架已在 | 缺企业购买、目录治理、合规、安全基线和开通编排 |
| Internationalization | `useI18n + locale catalog + locale switcher` | `L2` | `Suite billing center` 已具备 `en / zh-CN / zh-TW` 主路径 | 缺全站覆盖、格式化规范、术语表、覆盖率统计与 CI 门禁 |

## 4. v122 已缩小的差距

### 4.1 前端 UI
- `Suite` 从 `pricing compare / checkout / billing overview` 进一步升级为带 `billing center` 的账户账单管理层。
- 付款方式、发票记录、订阅生命周期动作已经形成独立区域，不再只停留在购买草稿层。
- 三语状态下的账单中心 UI 已通过浏览器实机验收并落图。

### 4.2 前端用户操作方式
- 当前用户已能在一个页面中完成：
  - 选 offer
  - 保存 checkout draft
  - 添加默认付款方式
  - 生成待处理发票
  - 查看订阅动作状态
- 相比 `v121`，已经从 `quote -> draft` 推进到 `draft -> payment method -> pending invoice -> lifecycle action`。

### 4.3 后端功能
- 现在已有独立的 `payment method / invoice / subscription state` 三域建模。
- `billing center` 聚合接口能返回：
  - 当前订阅摘要
  - 付款方式
  - 发票记录
  - 可执行或锁定的订阅动作
- 当前实现明确避免 silent fallback 与 fake payment success：
  - `APPLY_LATEST_DRAFT` 只进入 `PENDING invoice`
  - 不伪造支付完成与 entitlement 生效

### 4.4 国际化
- `Billing center` 新模块已具备 `en / zh-CN / zh-TW`。
- 本轮已把多语从“套餐目录与购买草稿层”扩展到“账户账单管理层”。

## 5. 当前主要差距

### 5.1 前端 UI 差距
- 当前项目仍以统一控制台为主，缺 `proton.me` 官方站点那种：
  - pricing 页
  - manage payment methods 页
  - invoices 页
  - manage subscription 页
  的分离式信息架构。
- `Suite` 页面同时承载 `plans / billing / readiness / governance / remediation`，外部购买和账户管理体验被内部运营模块稀释。
- 官方页面对 billing 信息的视觉层次更强调：
  - 账单状态
  - 付款方式卡
  - 发票历史
  - 订阅动作
  当前实现仍偏工程控制台。

### 5.2 前端用户操作方式差距
- 当前已做到：
  - `offer -> quote -> draft -> payment method -> pending invoice`
- 仍缺：
  - 真实支付授权与失败重试
  - invoice 下载
  - 税务、地区、币种和优惠码逻辑
  - 取消、降级、退款、恢复订阅后的完整反馈
  - 企业 lead form、contact sales 跟踪与 onboarding
  - 与官方独立产品页一致的分产品购买漏斗

### 5.3 后端功能差距
- 当前账单域仍没有：
  - `order`
  - `payment intent`
  - `payment provider webhook`
  - `invoice file download`
  - `tax calculation`
  - `subscription activation`
  - `renewal / cancellation settlement`
- 企业购买链路仍只是 `CONTACT_SALES` 的简化表达，不包含 CRM、审批或开通流程。
- 除 `Plans / Billing` 外，其余产品域仍普遍缺官方级深能力后端：
  - Mail 迁移与网关
  - Calendar / Drive / Docs / Sheets 实时协作
  - Pass passkeys 与附件
  - VPN 网络层
  - Meet 媒体服务
  - Wallet 链上与签名
  - Lumo 模型与检索编排

### 5.4 国际化差距
- 当前多语仍是页面级补齐，不是工程化体系。
- 主要缺口：
  - 全站字符串抽取完整性
  - `title / meta` 全量多语
  - 日期、货币、数字格式统一
  - 术语表与审校流程
  - 自动化覆盖率统计
  - CI 阻断门禁

## 6. 面向下下一个迭代的输入

### 6.1 Plans / Billing
- 从 `pending invoice` 扩展到真实支付、invoice download、subscription lifecycle 闭环
- 补 `payment method` 的删除/编辑、失败支付、过期卡与切换优先级
- 补 `self-serve checkout` 与 `contact-sales onboarding` 两条完整漏斗

### 6.2 前端体验
- 将 `Suite billing` 从控制台内嵌区升级为更接近官方的独立购买页与账户账单页
- 把公开购买模块与内部治理模块解耦

### 6.3 产品能力
- 继续按 Proton 全产品线补深能力，不只补导航和工作区骨架
- 优先补足与购买、订阅、权限解锁直接相关的后端域模型

### 6.4 国际化
- 把 `en / zh-CN / zh-TW` 从当前模块化覆盖升级为全站工程化治理
