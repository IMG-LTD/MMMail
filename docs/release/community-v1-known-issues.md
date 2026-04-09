# Community Edition v1.6.1 Known Issues

**版本**: `v1.6.1-known-issues`
**日期**: `2026-04-09`
**作者**: `Codex`

## 当前状态
- 当前公开基线：`main` → `v1.6.1`
- 当前无已知 release-blocking 阻塞；以下问题保留为收口外的已知限制或下一批 backlog 输入

## 非阻塞已知问题
- Flyway 在 MySQL `8.4` 上会输出“高于已验证版本 8.1”的警告：
  - 当前验证已通过，不构成 `v1.6.1` 阻塞
  - 后续升级 Flyway 时应复核该告警是否消失
- GitHub Actions 当前仍有 Node.js 20 deprecation warning：
  - 来自 `actions/checkout@v4`、`actions/setup-node@v4`、`actions/setup-java@v4`、`pnpm/action-setup@v4`
  - 当前 workflow 已成功，不构成 `v1.6.1` 阻塞
- `dependency-check` 在无 `MMMAIL_NVD_API_KEY` 时更新较慢：
  - 本机长时间运行可能超时
  - 建议在 CI 中配置 secret 并依赖 cache
- 当前 self-hosted 默认安装仍偏重：
  - `docker-compose.yml` 仍要求 `MySQL / Redis / Nacos`
  - `application-local.yml` 仍保留 `Kafka` bootstrap 配置
  - 这不阻塞已支持部署，但会提高 Community 首次试用门槛
- 前端大页与后端大服务仍偏重：
  - `drive.vue`、`pass.vue`、`docs.vue` 与多个核心 service 文件体量较大
  - 不影响当前主线使用，但会提高开源贡献者进入成本

## 当前边界内的明确限制
- Community 版不支持真实 Billing 结算、税费、发票闭环。
- Docs / Sheets 不承诺实时协作。
- `Pass` 仍为默认导航可见的 `Beta`，不承诺浏览器扩展、自动填充与真实 passkey ceremony。
- 企业身份自动化（`SSO / SCIM / LDAP`）仍处于 readiness / planning 范围。
- Preview 模块不保证向后兼容或稳定性。

## 处理方式
- 所有外部回执类问题统一通过：
  - `docs/release/community-v1-feedback-intake.md`
  - `docs/release/community-v1-v1.6.1-closure-plan.md`
- 所有支持边界统一见：
  - `docs/release/community-v1-support-boundaries.md`
