# Community Edition v1.0 RC1 Notes

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-14`  
**作者**: `Codex`

## 版本目标
- 输出首个可交付的 `Community Edition v1.0` 候选版。
- 范围冻结在 `Mail / Calendar / Drive / Admin / Workspace Shell / Auth / RBAC / 运维基线`。

## 模块分级
### GA
- `Auth / Session / MFA`
- `Organization / Tenant / RBAC / Admin`
- `Mail`
- `Calendar`
- `Drive`
- `Workspace Shell / Settings`
- 部署、升级、回滚、备份恢复、可观测性

### Beta
- `Docs`
- `Sheets`
- `Billing`（Community 边界内不承诺真实支付闭环）

### Preview
- `Pass`
- `Authenticator`
- `SimpleLogin`
- `Standard Notes`
- `VPN`
- `Meet`
- `Wallet`
- `Lumo`

## Gate 状态
- 当前正式状态：`RC1_READY_PENDING_EXTERNAL`
- 已完成：
  - Gate 0
  - Gate 2
  - Gate 7
- 本机已完成，等待外部回执：
  - Gate 5（`PASS_CANDIDATE`）
- 仍需外部 / 容器环境补证：
  - Gate 1
  - Gate 3
  - Gate 4
  - Gate 6（`BLOCKED_EXTERNAL`）

## 已知限制
- 当前环境无 Git remote 与 `gh` CLI，无法直接产出 GitHub Actions 官方回执。
- Preview 模块仅通过 `Labs` 进入，不纳入首发支持承诺。
- Community 版不承诺 Hosted / Billing 真支付链路。

## 自托管注意事项
- 生产环境必须替换 `.env` 中全部 secret。
- 建议配置 `MMMAIL_NVD_API_KEY`，避免 dependency-check 长时间全量更新。
- 仅向受信网段暴露管理接口与 `actuator` 端点。

## 升级 / 恢复注意事项
- 升级前先执行 `scripts/db-backup.sh`
- 默认回滚策略不是 down migration，而是：
  - 前滚修复，或
  - `scripts/db-rollback.sh` 触发备份恢复

## 证据入口
- 本机 RC1 证据：`scripts/validate-rc1-local.sh`
- 本机 RC1 报告：`artifacts/release/rc1-local/community-v1-rc1-local-evidence.md`
- 外部容器证据：`scripts/validate-rc1-container.sh`
- 外部执行清单：`docs/release/external-execution-checklist.md`
- Gate 状态：`docs/release/community-v1-gate.md`
- RC 清单：`docs/release/community-v1-rc-checklist.md`
- RC 状态：`docs/release/community-v1-rc-status.md`
