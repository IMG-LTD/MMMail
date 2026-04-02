# Community Edition v1.2 Release Checklist

**版本**: `v1.2-release-checklist-final`
**日期**: `2026-04-02`
**作者**: `Codex`

## 发布基线
- 发布分支：`release/v1.2`
- 正式 tag：`v1.2.0`
- 正式发布 commit：`38e548a1baa56f70f29841d094fa8f927367b1d9`
- 来源分支：`dev/community-v1`
- release 分支：`https://github.com/IMG-LTD/MMMail/tree/release/v1.2`
- release 分支 CI：`https://github.com/IMG-LTD/MMMail/actions?query=branch%3Arelease%2Fv1.2`
- GitHub Release：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.2.0`

## 发布前必须满足
- [x] `bash scripts/validate-local.sh` 在发布切点 `38e548a` 上通过
- [x] `docs/release/community-v1-v1.2-plan.md` 与实际代码一致
- [x] `docs/release/community-v1-support-boundaries.md` 已按 `v1.2` 最新状态复核
- [x] `docs/open-source/module-maturity-matrix.md` 已按 `v1.2` 最新状态复核
- [x] `docs/release/community-v1-v1.2.0-release-notes.md` 已按正式发布 commit 复核完成
- [x] `docs/release/community-v1-v1.2-final-signoff.md` 已完成签收

## 正式发布动作
1. [x] 从发布切点 `38e548a` 创建 `release/v1.2` 分支。
2. [x] 从同一发布切点创建 `v1.2.0` tag。
3. [x] 推送 `release/v1.2` 到远端。
4. [x] 推送 `v1.2.0` tag 到远端。
5. [x] 复核 `release notes / support boundaries / module maturity / README` 的事实口径。
6. [x] 将 `dev/community-v1` 继续作为下一轮 `post-v1.2` 集成分支。

## 仅允许的发布后修复
- `release-blocking` 功能回归
- 安装、升级、备份恢复阻断
- 安全基线回归
- release artifact 元数据错误
