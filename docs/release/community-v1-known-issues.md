# Community Edition v1.0 Known Issues

**版本**: `v1.0-known-issues`
**日期**: `2026-03-31`
**作者**: `Codex`

## 当前状态
- 当前正式状态：`GA_RELEASED`
- `v1.0.0` 已正式发布；以下问题不阻塞当前首发正式版，但需要在 `v1.0.x` / `v1.1` 中持续跟踪

## 发布阻塞外的已知问题
- Flyway 在 MySQL `8.4` 上会输出“高于已验证版本 8.1”的警告：
  - 当前 GA 验证已通过，不构成首发阻塞
  - 后续升级 Flyway 时应复核该告警是否消失
- GitHub Actions 当前仍有 Node.js 20 deprecation warning：
  - 来自 `actions/checkout@v4`、`actions/setup-node@v4`、`actions/setup-java@v4`、`pnpm/action-setup@v4`
  - 当前 workflow 已成功，不构成 `v1.0.0` 阻塞
- `dependency-check` 在无 `MMMAIL_NVD_API_KEY` 时更新较慢：
  - 本机长时间运行可能超时
  - 建议在 CI 中配置 secret 并依赖 cache

## 首发边界内的明确限制
- Community 版不支持真实 Billing 结算、税费、发票闭环。
- Docs / Sheets 不承诺实时协作。
- Preview 模块不保证向后兼容或稳定性。

## 处理方式
- 所有外部回执类问题统一通过：
  - `docs/release/external-ci-handoff.md`
  - `docs/release/external-execution-checklist.md`
- 所有支持边界统一见：
  - `docs/release/community-v1-support-boundaries.md`
