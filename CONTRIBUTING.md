# Contributing to MMMail

## Scope
- 当前公开协作基线以 `Community Edition v1.6.1` 为准，权威范围见 `README.md`、`docs/release/community-v1-support-boundaries.md` 与 `docs/open-source/module-maturity-matrix.md`。
- `GA` 模块的回归、文档、运维能力属于当前 release-blocking 范围；`Beta / Preview` 改动不得破坏 `GA` 门禁，也不得夸大未交付能力。
- 当前主线优先级仍是 `Mail → Calendar → Drive → Pass` 协作闭环与自托管可采用性，不继续横向扩张 `Preview` 宽度。

## Before You Open a PR
1. 阅读 `README.md`、`SECURITY.md`、`docs/release/community-v1-support-boundaries.md`。
2. 使用 `.env.example`（最小模式）或 `config/backend.env.example`（标准模式）创建本地 `.env`，并执行 `./scripts/validate-runtime-env.sh .env`。
3. 运行默认门禁：
   - `bash scripts/validate-local.sh`
4. 如修改数据库、备份或迁移逻辑，补充：
   - `bash scripts/validate-batch3.sh`
5. 如修改安全、权限、上传、分享或管理员能力，补充：
   - `bash scripts/validate-security.sh`

## Branch / Commit Rules
- 使用语义化提交前缀：`feat:` `fix:` `docs:` `test:` `refactor:` `chore:`。
- 一个 PR 只解决一个清晰问题，避免把新功能和收口修复混在一起。
- 若改动影响公开边界、安装方式或自托管运维路径，必须同步更新 `README.md`、`docs/ops/*`、`docs/release/community-v1-support-boundaries.md` 或 `docs/open-source/module-maturity-matrix.md`。

## Code Expectations
- 遵守仓库 `AGENTS.md` 中的工程约束：函数长度、文件长度、无静默 fallback、显式失败。
- 不要提交真实 secrets、密钥、生产凭据或私有基础设施地址。
- 安全或权限逻辑改动必须附带自动化回归。
- 前端改动需要类型检查；后端改动需要定向 Maven 测试。

## PR Checklist
- [ ] 改动范围仍在 Community Edition `v1.6.1` 当前公开支持边界内
- [ ] 对应测试已更新并通过
- [ ] 文档已同步更新，或确认无需更新
- [ ] 无真实 secrets / token / password 写入仓库
- [ ] 若影响采用或运维流程，已更新 `README.md`、`docs/ops/*` 或边界文档

## Security Reports
- 不要通过公开 issue 披露活跃安全漏洞。
- 按 `SECURITY.md` 中的流程私下报告。
