# Community Edition v1.1 Release Checklist

**版本**: `v1.1-release-checklist-final`
**日期**: `2026-04-01`
**作者**: `Codex`

## 发布基线
- 发布分支：`release/v1.1`
- 正式 tag：`v1.1.0`
- 正式发布 commit：`400dc764ca2d9cd57e179a0a4e0fe13bfcb120cb`
- 来源分支：`dev/community-v1`
- 来源分支 CI：`https://github.com/IMG-LTD/MMMail/actions/runs/23834759633`
- 发布分支 CI：`https://github.com/IMG-LTD/MMMail/actions/runs/23834771915`
- GitHub Release：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.1.0`

## 发布前必须满足
- [x] `release/v1.1` 发布 commit 的 `MMMail CI` 为绿色
- [x] 不存在打开状态的 `v1.1 release-blocking` issue
- [x] `docs/release/community-v1-v1.1-plan.md` 与实际代码一致
- [x] `docs/release/community-v1-support-boundaries.md` 已按 `v1.1` 最新状态复核
- [x] `docs/open-source/module-maturity-matrix.md` 已按 `v1.1` 最新状态复核
- [x] `docs/release/community-v1-v1.1.0-release-notes.md` 已按正式发布 commit 复核完成
- [x] `docs/release/community-v1-v1.1-final-signoff.md` 已完成签收

## 正式发布动作
1. [x] 确认 `release/v1.1` 发布 commit 对应 workflow 为绿色。
2. [x] 复核 `release notes / support boundaries / module maturity / README` 的事实口径。
3. [x] 从 `release/v1.1` 的已验证 head 创建 `v1.1.0` tag。
4. [x] 发布 GitHub Release。
5. [x] 将 `dev/community-v1` 继续作为下一轮集成分支，仅承接 `post-v1.1` 或后续已批准范围。

## 仅允许的发布后修复
- `release-blocking` 功能回归
- 安装、升级、备份恢复阻断
- 安全基线回归
- release artifact 元数据错误
