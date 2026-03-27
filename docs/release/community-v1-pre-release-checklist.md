# Community Edition v1.0 Pre-release Checklist

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前状态
- 当前正式状态：`RC1_READY_PENDING_EXTERNAL`
- 仅剩远端 CI / Docker-capable 回执与最终签收
- 当前已进入发布冻结；除非 freeze exception 批准，否则不得继续产品或工程实现改动

## 一、本机已完成
- [x] `bash scripts/validate-runtime-env.sh <temp-env>`
- [x] `docker compose --env-file <temp-env> config`
- [x] `bash scripts/validate-local.sh`
- [x] `bash scripts/validate-all.sh`
- [x] `bash scripts/validate-rc1-local.sh`
- [x] 本机证据：`artifacts/release/rc1-local/community-v1-rc1-local-evidence.md`
- [x] Gate 5 本地安全门禁
- [x] Gate 7 开源治理收口

## 二、需要远端 CI 回执
- [ ] `bash scripts/validate-ci.sh`
- [ ] `bash scripts/validate-rc1-container.sh`
- [ ] `dependency-check-report.html` 已归档
- [ ] `dependency-check-report.json` 已归档
- [ ] `community-v1-rc1-container-evidence.md` 已归档
- [ ] Gate 3 容器化迁移 / 恢复回执已回填
- [ ] Gate 6 官方 CI 回执已回填
- [ ] Gate 4 install / upgrade / restore / rollback 官方回执已回填

## 三、需要仓库管理员 / 发布经理确认
- [ ] 已配置 Git remote
- [ ] GitHub Actions runner 具备 Docker 能力
- [ ] `MMMAIL_NVD_API_KEY` 已配置（推荐）
- [ ] README / scope / roadmap / gate / support boundaries / known issues 口径一致
- [ ] `docs/release/community-v1-rc1-notes.md` 已审核

## 四、达到 RC1 Ready 的条件
- [ ] 本机检查项全部完成
- [ ] 外部 CI 回执项全部完成
- [ ] 发布经理确认 RC notes / known issues / support boundaries
- [ ] Gate 文档已更新为最新真实状态
- [ ] `docs/release/community-v1-final-signoff.md` 已完成
