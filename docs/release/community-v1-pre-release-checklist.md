# Community Edition v1.0 Pre-release Checklist

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 当前状态
- 当前正式状态：`RC1_READY`
- 远端 CI / Docker-capable 回执已完成并回填
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
- [x] `bash scripts/validate-ci.sh`
- [x] `bash scripts/validate-rc1-container.sh`
- [x] `dependency-check-report.html` 已归档
- [x] `dependency-check-report.json` 已归档
- [x] `community-v1-rc1-container-evidence.md` 已归档
- [x] Gate 3 容器化迁移 / 恢复回执已回填
- [x] Gate 6 官方 CI 回执已回填
- [x] Gate 4 install / upgrade / restore / rollback 官方回执已回填

## 三、需要仓库管理员 / 发布经理确认
- [x] 已配置 Git remote
- [x] GitHub Actions runner 具备 Docker 能力
- [x] `MMMAIL_NVD_API_KEY` 已配置（推荐）
- [x] README / scope / roadmap / gate / support boundaries / known issues 口径一致
- [x] `docs/release/community-v1-rc1-notes.md` 已审核

## 四、达到 RC1 Ready 的条件
- [x] 本机检查项全部完成
- [x] 外部 CI 回执项全部完成
- [x] 发布经理确认 RC notes / known issues / support boundaries
- [x] Gate 文档已更新为最新真实状态
- [x] `docs/release/community-v1-final-signoff.md` 已完成
