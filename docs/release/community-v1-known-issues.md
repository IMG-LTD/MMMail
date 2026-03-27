# Community Edition v1.0 Known Issues

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前状态
- 当前正式状态：`RC1_READY_PENDING_EXTERNAL`
- 已无本机实现阻塞；剩余问题全部属于外部执行与回填

## 发布阻塞外的已知问题
- 远端 GitHub Actions 官方回执尚未生成：
  - 当前环境无 Git remote
  - 当前环境无 `gh` CLI
- 本机无法执行 Docker-backed cold start / restore 验证：
  - `scripts/validate-rc1-container.sh` 需在 Docker-capable runner 执行
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
