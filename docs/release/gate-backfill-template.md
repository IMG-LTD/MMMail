# Community Edition v1.0 Gate Backfill Template

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 使用方式
- 外部执行完成后，按下面模板回填 `docs/release/community-v1-gate.md`
- 使用“章节名 + 证据路径”回填，不依赖固定行号

## Gate 4 回填模板
```md
### Gate 4 外部回填
- 执行日期：<YYYY-MM-DD>
- 执行人：<name>
- workflow / job：<workflow-name/job-name 或 manual runner>
- 证据：
  - artifact：`artifacts/release/rc1-container/community-v1-rc1-container-evidence.md`
  - backups：`artifacts/release/rc1-container/backups/`
  - compose log：`artifacts/release/rc1-container/compose.log`
- 结果：
  - fresh install：PASS/FAIL
  - init / seed：PASS/FAIL
  - upgrade：PASS/FAIL
  - backup：PASS/FAIL
  - restore：PASS/FAIL
  - rollback：PASS/FAIL
- Gate 判定：PASS / IN_PROGRESS
```

**PASS 条件**
- `community-v1-rc1-container-evidence.md` 所有项目均为 `PASS`
- Gate 状态总览中的 Gate 4 更新为 `PASS`

## Gate 5 回填模板
```md
### Gate 5 外部回填
- 执行日期：<YYYY-MM-DD>
- 执行人：<name>
- workflow / job：`MMMail CI / validate`
- 证据：
  - report html：`artifacts/security/dependency-check/dependency-check-report.html`
  - report json：`artifacts/security/dependency-check/dependency-check-report.json`
  - workflow run：<url>
- 结果：
  - dependency-check：PASS/FAIL
  - secrets scan：PASS/FAIL
  - security regression：PASS/FAIL
- Gate 判定：PASS / PASS_CANDIDATE
```

**PASS 条件**
- `dependency-check-report.html` 与 `dependency-check-report.json` 都已归档
- workflow run 可追溯
- Gate 状态总览中的 Gate 5 更新为 `PASS`

## Gate 6 回填模板
```md
### Gate 6 外部回填
- 执行日期：<YYYY-MM-DD>
- 执行人：<name>
- workflow / job：`MMMail CI / validate`
- 证据：
  - workflow run：<url>
  - step summary：<url or summary note>
  - artifact：`mmmail-validate-artifacts`
  - logs：`artifacts/ci-logs/`
  - surefire：`backend/mmmail-server/target/surefire-reports/`
- 结果：
  - validate-ci：PASS/FAIL
  - docker-capable runner：YES/NO
  - 回执完整性：PASS/FAIL
- Gate 判定：PASS / BLOCKED_EXTERNAL
```

**PASS 条件**
- `validate` job 绿色
- workflow run、artifact、step summary 可追溯
- Gate 状态总览中的 Gate 6 更新为 `PASS`

## Checklist 回填模板
```md
- [x] backend dependency scan 报告已归档
- [x] fresh install 证据已归档
- [x] upgrade 证据已归档
- [x] backup / restore 证据已归档
- [x] rollback strategy 证据已归档
- [x] validate-ci workflow 已产出官方回执
```
