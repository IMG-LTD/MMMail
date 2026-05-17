## Summary

## Scope
- [ ] `GA`
- [ ] `Beta`
- [ ] `Preview`

## Validation
- [ ] `bash scripts/validate-local.sh`
- [ ] `bash scripts/validate-security.sh`
- [ ] `node --test tests/v22-supply-chain-security-contract.test.mjs`（依赖、lockfile、POM、Dependabot 告警或供应链安全改动）
- [ ] `bash scripts/release-gate.sh`（release / gate / CI 改动必须无 skip 通过）
- [ ] 相关定向测试

## Documentation
- [ ] README / support boundaries / module maturity / ops docs 已同步
- [ ] Business / Hosted / payment / SLA / license 口径已对齐商业边界文档
- [ ] Helm / image / env / deployment 变更已同步 install docs、chart、workflow 或 release notes
- [ ] 若把 v2.2 外部证据项标为 done，已按 `docs/v22-external-evidence-checklist.md` 附真实证据，并记录 `bash scripts/validate-v22-external-evidence.sh` 结果
- [ ] 无需文档更新（说明原因）

## Governance
- [ ] Commit 已包含 `Signed-off-by`
- [ ] 未新增或修改 legacy `frontend-v2` 文件；如触及该目录，只包含删除或迁出历史材料
- [ ] 如修改 owner / support / roadmap，已同步 `GOVERNANCE.md`、`MAINTAINERS.md` 或 `ROADMAP.md`
- [ ] 商业文案仍保持 `real payment processing is not live`；`license signing private keys stay outside the public repository`
- [ ] 如调整 GitHub private vulnerability reporting 状态，已用 GitHub API 结果同步 `SECURITY.md` / `SUPPORT.md` / completion audit / external evidence verifier
- [ ] 如响应 Dependabot 告警，已记录 `gh api repos/IMG-LTD/MMMail/dependabot/alerts?state=open&per_page=100` 结果、fixed version 和验证命令；GHCR 证据需要 `read:packages`
- [ ] 未新增超过 500 行的活跃源码文件；若触及历史超限文件，新增职责已拆到小模块并保持治理 allowlist 可解释

## Security
- [ ] 未引入真实 secrets
- [ ] 涉及权限/上传/分享/管理员接口时已补自动化回归
- [ ] 供应链安全修复没有关闭扫描、降低 CVSS 门槛或移除真实依赖路径
