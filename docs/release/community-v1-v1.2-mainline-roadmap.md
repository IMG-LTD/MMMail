# Community Edition v1.2 Mainline Roadmap

**版本**: `v1.2-mainline-roadmap-final`
**日期**: `2026-04-02`
**作者**: `Codex`

## 发布结论
- 发布分支：`release/v1.2`
- 正式 tag / commit：`v1.2.0` / `38e548a1baa56f70f29841d094fa8f927367b1d9`
- `dev/community-v1` 在 `v1.2.0` 发布后回到 `post-v1.2` 主线开发职责。

## 主线原则
- 只承诺当前仓库可真实落地并可验证的能力。
- 优先补齐根基性差距的基础设施，不做半成品营销叙事。
- 将“已交付能力”和“后续 discovery / roadmap”明确拆开。

## `v1.2` 主线流
| Stream | 主题 | 状态 | 说明 |
|---|---|---|---|
| A | `Mobile / PWA baseline` | `Done` | 已落地 PWA 壳层、安装入口、测试与本地门禁。 |
| B | `Capability honesty & boundary cleanup` | `Done` | 已收口 Community 对外口径，避免把未实现能力写成已上线。 |
| C | `Mail E2EE foundation + recipient discovery + message encryption` | `Done` | foundation、recipient discovery、正文加密发送与详情本地解密已落地并完成验证。 |
| D | `Zero-knowledge / protocol discovery` | `Done` | 已补零知识路线与协议栈 discovery 文档，明确阶段拆分与非目标。 |
| E | `Adoption readiness` | `Done` | 已交付 adoption panel、OpenAPI / Swagger UI、自托管 install / runbook 快速页与文档增强。 |

## 当前批次冻结范围
### 已完成
1. `Mail E2EE` key profile foundation 文档、后端 API 与前端设置页
2. `READY` 内部路由正文加密发送与详情页本地解密
3. `Zero-knowledge roadmap` 与 `SMTP / IMAP / Bridge discovery`
4. `PWA` 壳层、安装入口与 readiness 面板
5. Adoption readiness 入口与自托管文档刷新
6. Sheets 发布前 reviewer regressions 修复

### 明确未做
1. 附件加密发送与外部收件人完整 E2EE
2. 真正零知识架构
3. `SMTP / IMAP / Bridge` 实现代码
4. `Drive` 客户端加密上传
5. `Web Push` 下发、原生客户端、Pass 扩展
6. 支付闭环、企业目录、SSO / SCIM / LDAP

## 版本门禁结果
- 前端 `typecheck + vitest`：通过
- 后端目标测试（`timeout 60s`）：通过
- 本地正式门禁 `bash scripts/validate-local.sh`：通过
- 远端 release 分支：`origin/release/v1.2` 已创建

## 后续入口
- `v1.2.x`：只接受 `release-blocking / security / metadata` 修复
- `post-v1.2`：`dev/community-v1` 承接下一轮已批准范围
