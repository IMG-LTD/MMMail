# Community Edition v1.0.0 Release Notes Draft

**版本**: `v1.0.0-release-draft`  
**日期**: `2026-03-31`  
**作者**: `Codex`

## 当前状态
- 当前文档为正式 `v1.0.0` release notes 草稿。
- 仅在 `release/v1.0` 最新待发布 head 完成最终签收后，才可作为正式发布说明使用。
- 正式发布日期：`待发布`
- 正式 tag / commit：`待发布（以 release/v1.0 最新已验证 head 为准）`
- Release owner：`待填写`

## Summary
- `MMMail Community Edition v1.0.0` 是首个公开的 Community 正式版。
- 首发交付聚焦 `Mail / Calendar / Drive / Admin / Workspace Shell / Auth / RBAC / 运维基线`。
- `Docs / Sheets / Billing` 继续保留为 `Beta`，`Preview` 模块继续通过 `Labs` 隔离。

## Gate Status
- Gate 0：`PASS`
- Gate 1：`PASS`
- Gate 2：`PASS`
- Gate 3：`PASS`
- Gate 4：`PASS`
- Gate 5：`PASS`
- Gate 6：`PASS`
- Gate 7：`PASS`
- 证据：
  - GitHub Actions run `23661060407`
  - `artifacts/security/dependency-check/dependency-check-report.{html,json}`
  - `artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`

## Included
- 核心功能：
  - `Auth / Session / MFA`
  - `Organization / Tenant / RBAC / Admin`
  - `Mail`
  - `Calendar`
  - `Drive`
  - `Workspace Shell / Settings`
- 工程化能力：
  - 安装、升级、回滚、备份恢复
  - 默认门禁：`validate-local / validate-all / validate-ci`
  - 安全扫描、依赖审计、观测与健康页
- 文档 / 治理：
  - 发布门禁、签收、反馈分流、支持边界、开源协作入口

## Security
- 安全修复：当前草稿未单列新增安全修复；以正式发布时 `release/v1.0` 待发布 head 的变更为准
- 扫描结果：
  - `dependency-check` 报告已归档到 `artifacts/security/dependency-check/`
  - 当前已知安全基线门禁已通过
- 已知限制：
  - 生产环境必须替换 `.env` 中全部 secrets
  - 建议配置 `MMMAIL_NVD_API_KEY`，减少 `dependency-check` 更新耗时
  - 仅向受信网段暴露管理接口与 `actuator` 端点

## Upgrade Notes
- 升级前准备：
  - 升级前先执行 `scripts/db-backup.sh`
  - 复核 `.env` 中全部 secrets 与运行时配置
- 迁移步骤：
  - 以 `docs/ops/upgrade.md` 为准执行升级
  - 以 `release/v1.0` 的待发布镜像 / 源码基线为准验证迁移
- 回滚策略：
  - 默认不提供 down migration
  - 使用前滚修复，或通过 `scripts/db-rollback.sh` 触发备份恢复

## Validation Evidence
- 本机默认门禁：`bash scripts/validate-local.sh`
- 本机全量门禁：`bash scripts/validate-all.sh`
- CI 门禁：`MMMAIL_VALIDATE_CONTAINER_TESTS=true bash scripts/validate-ci.sh`
- RC1 本机证据：`artifacts/release/rc1-local/community-v1-rc1-local-evidence.md`
- RC1 容器证据：`artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`

## Known Limitations
- Flyway 在 MySQL `8.4` 上会输出高于已验证版本的警告；当前不构成首发阻塞
- GitHub Actions 仍存在 Node.js 20 deprecation warning；当前 workflow 已成功，不构成首发阻塞
- `dependency-check` 在无 `MMMAIL_NVD_API_KEY` 时更新较慢，本机长时间运行可能超时
- `Docs / Sheets` 不承诺实时协作
- Community 版不支持真实 Billing 结算、税费、发票闭环
- `Preview` 模块不保证稳定性或向后兼容

## Support Boundaries
- GA：
  - `Auth / Session / MFA`
  - `Organization / RBAC / Admin`
  - `Mail`
  - `Calendar`
  - `Drive`
  - `Workspace Shell / Settings`
  - 部署、升级、备份恢复、可观测性
- Beta：
  - `Docs`
  - `Sheets`
  - `Billing`（仅 Community 边界内的状态展示，不承诺真实支付闭环）
- Preview：
  - `Pass`
  - `Authenticator`
  - `SimpleLogin`
  - `Standard Notes`
  - `VPN`
  - `Meet`
  - `Wallet`
  - `Lumo`

## Deferred / Out of Scope
- `Docs / Sheets` 深能力扩展
- 真实 Billing / 商业订阅闭环
- `Preview` 模块成熟度提升
- `VPN / Meet / Wallet / Lumo` 进入正式版范围

## 发布前待补最后信息
- `release/v1.0` 最新待发布 head 对应的 workflow run 链接
- 最终 `v1.0.0` tag / commit
- Release owner / 审核人 / 正式发布日期
- 若 `release/v1.0` 待发布提交已偏离 `v1.0.0-rc1`，需同步补最终差异说明
