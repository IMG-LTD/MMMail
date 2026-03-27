# Community Edition v1.0 RC Checklist

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前状态
- 当前正式状态：`RC1_READY_PENDING_EXTERNAL`
- 状态来源：`docs/release/community-v1-rc-status.md`

## Gate Review
- [x] Gate 0 范围冻结
- [ ] Gate 1 冷启动与初始化回执
- [x] Gate 2 核心功能
- [ ] Gate 3 容器化迁移 / 恢复 CI 回执
- [ ] Gate 4 install / upgrade 完整链路证据
- [x] Gate 5 本地安全基线门禁
- [ ] Gate 6 Docker-capable CI 正式回执
- [x] Gate 7 开源治理与文档一致性

## RC 必查项
- [x] README / scope / roadmap / gate / maturity matrix 一致
- [x] README / known issues / support boundaries / rc checklist 一致
- [x] LICENSE / CONTRIBUTING / SECURITY / issue / PR 模板存在
- [x] secrets regression scan 通过
- [ ] backend dependency scan 报告已归档
- [x] 安全与权限回归通过
- [x] validate-local / validate-all 通过
- [x] validate-rc1-local 通过
- [x] 本机 RC1 证据已归档：`artifacts/release/rc1-local/community-v1-rc1-local-evidence.md`
- [ ] fresh install 证据已归档
- [ ] upgrade 证据已归档
- [ ] backup / restore 证据已归档
- [ ] rollback strategy 证据已归档
- [ ] validate-ci workflow 已产出官方回执
- [ ] 发布说明已按模板填写

## 外部阻塞
- 当前仓库尚未配置 Git remote，且本机无 `gh` CLI；远端 GitHub Actions 的正式回执需在连接远端仓库后生成。
- 外部执行说明：`docs/release/external-ci-handoff.md`
- 外部执行清单：`docs/release/external-execution-checklist.md`
