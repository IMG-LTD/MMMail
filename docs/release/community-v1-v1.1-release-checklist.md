# Community Edition v1.1 Release Checklist

**版本**: `v1.1-release-checklist-draft`
**日期**: `2026-04-01`
**作者**: `Codex`

## 发布基线
- 发布分支：`release/v1.1`
- 候选提交：`a6bdda20cfbf6c2f040a4141c220f5426ae3d7b2`
- 来源分支：`dev/community-v1`
- 来源 CI：`https://github.com/IMG-LTD/MMMail/actions/runs/23834450221`

## 发布前必须满足
- [ ] `release/v1.1` 最新 head 的 `MMMail CI` 为绿色
- [ ] 不存在打开状态的 `v1.1 release-blocking` issue
- [ ] `docs/release/community-v1-v1.1-plan.md` 与实际代码一致
- [ ] `docs/release/community-v1-support-boundaries.md` 已按 `v1.1` 最新状态复核
- [ ] `docs/open-source/module-maturity-matrix.md` 已按 `v1.1` 最新状态复核
- [ ] `docs/release/community-v1-v1.1.0-release-notes.md` 已按待发布 head 复核完成
- [ ] `docs/release/community-v1-v1.1-final-signoff.md` 已完成签收

## 正式发布动作
1. 确认 `release/v1.1` 待发布 head 对应 workflow 为绿色。
2. 复核 `release notes / support boundaries / module maturity / README` 的事实口径。
3. 从 `release/v1.1` 的最新已验证 head 创建 `v1.1.0` tag。
4. 发布 GitHub Release。
5. 将 `dev/community-v1` 继续作为下一轮集成分支，仅承接 `post-v1.1` 或后续已批准范围。

## 仅允许的发布后修复
- `release-blocking` 功能回归
- 安装、升级、备份恢复阻断
- 安全基线回归
- release artifact 元数据错误
