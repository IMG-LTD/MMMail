# Community Edition v1.0 Threat Model

**版本**: `v1.0-draft`  
**日期**: `2026-03-13`  
**作者**: `Codex`

## 保护资产
- 用户身份、访问令牌、刷新令牌、会话状态
- 组织与租户边界、RBAC、管理员能力
- Mail / Calendar / Drive 主数据与附件
- 备份数据、迁移脚本、部署配置
- 审计日志、运行时错误事件、系统健康信息

## 攻击者模型
- 匿名互联网访问者：尝试登录爆破、公共分享滥用、公开接口枚举
- 已认证普通成员：尝试跨组织读取、管理员接口越权、下载他人资源
- 恶意共享接收者：尝试绕过分享密码、超额下载、公开链接穷举
- 自托管运维者失误：错误配置 TLS、暴露默认凭据、错误开放 Actuator

## 信任边界
1. 浏览器 / 前端运行时 ↔ 后端 API
2. 公共分享入口 ↔ 受保护 Drive 数据
3. 应用服务 ↔ MySQL / Redis / 对象存储路径
4. 本地与 CI 验证链 ↔ 发布产物与运维脚本

## 已覆盖的首发控制
- `JWT + refresh token` 会话校验与 token rotation
- `CSRF` 校验仅对 refresh cookie 路径开启
- `RBAC + org scope` 保护管理员接口与组织边界
- `MailAttachmentIntegrationTest` / `DriveSecureShareIntegrationTest` / `DrivePublicFolderShareIntegrationTest` 覆盖上传、下载、分享授权
- 登录失败限流与客户端错误上报限流
- `X-Frame-Options`、`X-Content-Type-Options`、`Referrer-Policy`、API 级 `CSP / Permissions-Policy`
- secrets 防回归扫描与后端依赖漏洞扫描门禁
- 审计日志、请求追踪、Prometheus 指标、system-health 可见性

## 首发未覆盖 / 明确不承诺
- 分布式限流一致性：当前为节点内窗口计数，适合 Community Edition 单实例/轻量部署
- 完整 DAST / WAF / Bot 管理能力
- 外部 IdP / SSO / 硬件密钥体系
- Preview 模块的深引擎安全评估（VPN / Meet / Wallet / Lumo）
- 多区域灾备和密钥托管服务

## 主要威胁与缓解
| 威胁 | 影响面 | 当前缓解 |
|---|---|---|
| 登录爆破 | Auth / Session | 登录失败限流、审计记录、统一错误码 |
| 水平越权 / 跨组织访问 | Admin / Mail / Calendar / Drive | 统一 org 校验、RBAC 回归、受保护下载路径测试 |
| 公开分享滥用 | Drive Public Share | 分享密码、访问日志、访问限流、权限回归 |
| 客户端错误上报滥用 | Observability | 鉴权保护、会话级限流、错误缓冲上限 |
| 仓库 secrets 回归 | 配置 / CI | `scripts/security-secret-scan.sh`、模板占位符、SECURITY 要求 |
| 第三方依赖漏洞 | Frontend / Backend | `pnpm audit --prod --audit-level=high`、OWASP Dependency-Check CI 门禁 |

## 自托管默认建议
- 生产环境启用 HTTPS，并设置 `MMMAIL_AUTH_COOKIE_SECURE=true`
- 轮转 `.env` 中所有数据库、Redis、Nacos、JWT secrets
- 仅向受信网段暴露 `actuator` 与管理入口
- 为 MySQL / 备份目录 / Drive 存储路径设置最小权限
- 保留 `artifacts/security/` 与 CI 报告作为发布证据
