# Community Edition v1.0 RC Checklist

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前状态
- 当前正式状态：`RC1_READY`
- 状态来源：`docs/release/community-v1-rc-status.md`

## Gate Review
- [x] Gate 0 范围冻结
- [x] Gate 1 冷启动与初始化回执
- [x] Gate 2 核心功能
- [x] Gate 3 容器化迁移 / 恢复 CI 回执
- [x] Gate 4 install / upgrade 完整链路证据
- [x] Gate 5 本地安全基线门禁
- [x] Gate 6 Docker-capable CI 正式回执
- [x] Gate 7 开源治理与文档一致性

## RC 必查项
- [x] README / scope / roadmap / gate / maturity matrix 一致
- [x] README / known issues / support boundaries / rc checklist 一致
- [x] LICENSE / CONTRIBUTING / SECURITY / issue / PR 模板存在
- [x] secrets regression scan 通过
- [x] backend dependency scan 报告已归档
- [x] 安全与权限回归通过
- [x] validate-local / validate-all 通过
- [x] validate-rc1-local 通过
- [x] 本机 RC1 证据已归档：`artifacts/release/rc1-local/community-v1-rc1-local-evidence.md`
- [x] fresh install 证据已归档
- [x] upgrade 证据已归档
- [x] backup / restore 证据已归档
- [x] rollback strategy 证据已归档
- [x] validate-ci workflow 已产出官方回执
- [x] 发布说明已按模板填写

## 官方回执
- GitHub Actions run：`https://github.com/IMG-LTD/MMMail/actions/runs/23661060407`
- 外部回执登记：`docs/release/community-v1-external-receipt-log.md`
- RC1 容器证据：`artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`
