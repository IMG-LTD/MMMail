# Contributing to MMMail

## Scope
- `Community Edition v1.0` 的首发范围以 `docs/release/community-v1-scope.md` 为准。
- `GA` 模块的回归、文档、运维能力属于首发阻塞；`Beta / Preview` 改动不得破坏 `GA` 门禁。

## Before You Open a PR
1. 阅读 `README.md`、`SECURITY.md`、`docs/release/community-v1-gate.md`。
2. 使用 `.env.example` 或 `config/backend.env.example` 创建本地环境，并执行 `./scripts/validate-runtime-env.sh .env`。
3. 运行默认门禁：
   - `bash scripts/validate-local.sh`
4. 如修改数据库、备份或迁移逻辑，补充：
   - `bash scripts/validate-batch3.sh`
5. 如修改安全相关能力，确认：
   - `bash scripts/validate-security.sh`

## Branch / Commit Rules
- 使用语义化提交前缀：`feat:` `fix:` `docs:` `test:` `refactor:` `chore:`。
- 一个 PR 只解决一个清晰问题，避免把新功能和收口修复混在一起。
- 涉及 release gate 的改动必须同步更新对应文档。

## Code Expectations
- 遵守仓库 `AGENTS.md` 中的工程约束：函数长度、文件长度、无静默 fallback、显式失败。
- 不要提交真实 secrets、密钥、生产凭据或私有基础设施地址。
- 安全或权限逻辑改动必须附带自动化回归。
- 前端改动需要类型检查；后端改动需要定向 Maven 测试。

## PR Checklist
- [ ] 改动范围仍在 Community Edition v1.0 首发范围内
- [ ] 对应测试已更新并通过
- [ ] 文档已同步更新
- [ ] 无真实 secrets / token / password 写入仓库
- [ ] 若影响运维流程，已更新 `docs/ops/*` 或 `docs/release/*`

## Security Reports
- 不要通过公开 issue 披露活跃安全漏洞。
- 按 `SECURITY.md` 中的流程私下报告。
