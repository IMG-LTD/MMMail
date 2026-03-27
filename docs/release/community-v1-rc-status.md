# Community Edition v1.0 RC Status

**版本**: `v1.0-rc1-draft`  
**日期**: `2026-03-15`  
**作者**: `Codex`

## 状态枚举
### `NOT_READY`
- 仍存在本机可完成但未完成的实现、文档或门禁缺口

### `READY_FOR_RC1_INTERNAL`
- 本机实现与文档已完整
- 但尚未完成内部门禁或本机证据链

### `RC1_READY_PENDING_EXTERNAL`
- 本机可完成的收口工作全部完成
- 剩余阻塞仅来自外部 Docker-capable 执行与官方 CI 回执
- 可以交给仓库管理员 / 发布经理接手执行

### `RC1_READY`
- Gate 4 / 5 / 6 的外部回执已完成并回填
- RC1 材料、gate、checklist、receipt log、signoff 已同步
- 可以进入正式发布候选确认

### `RELEASE_BLOCKED`
- 即使外部回执完成，仍存在新的 release-blocking 缺陷或证据冲突

## 当前正式判定
- **当前状态：`RC1_READY_PENDING_EXTERNAL`**

## 冻结规则
- 在未收到外部回执前，不得切换为 `RC1_READY`
- 在未获得 freeze exception 批准前，不得继续改动产品或工程实现
- 当前仅允许：
  - 外部执行支持
  - 失败分诊
  - Gate 回填
  - 最终签收待命

## 判定依据
- 已完成：
  - Gate 0 = `PASS`
  - Gate 2 = `PASS`
  - Gate 7 = `PASS`
  - Gate 5 = `PASS_CANDIDATE`
  - 本机 RC1 证据链 = 已归档
- 未完成但均为外部项：
  - Gate 4 需要 Docker-capable install / upgrade / restore / rollback 回执
  - Gate 5 需要 dependency-check 官方归档报告
  - Gate 6 需要 GitHub Actions 官方 workflow run 回执

## 距离 `RC1_READY` 的最小动作
1. 执行 `MMMail CI / validate`
2. 执行 `bash scripts/validate-rc1-container.sh`
3. 回填 `docs/release/community-v1-gate.md`
4. 更新 `docs/release/community-v1-rc-checklist.md`
5. 更新 `docs/release/community-v1-pre-release-checklist.md`
6. 更新 `docs/release/community-v1-external-receipt-log.md`
7. 勾选 `docs/release/community-v1-final-signoff.md`

## 状态迁移条件
### `RC1_READY_PENDING_EXTERNAL` -> `RC1_READY`
- Gate 4 = `PASS`
- Gate 5 = `PASS`
- Gate 6 = `PASS`
- 外部回执登记表已更新
- 最终签收模板已完成

### `RC1_READY` -> 正式发布候选确认
- 发布负责人确认 `community-v1-final-signoff.md`
- `community-v1-rc1-notes.md` 审核通过
- release notes 与 tag/commit 准备完成

### 任意状态 -> `RELEASE_BLOCKED`
- 出现新的 release-blocking regression
- 外部回执与 gate 证据冲突
- 关键 artifact 缺失或无法追溯
- freeze exception 未批准却发生实现改动
