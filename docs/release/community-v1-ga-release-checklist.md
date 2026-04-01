# Community Edition v1.0 GA Release Checklist

**版本**: `v1.0-ga-checklist`
**日期**: `2026-03-28`
**作者**: `Codex`

## 适用范围
- 仅用于 `v1.0.0` 正式发布前的最终确认。
- 不用于 `v1.1` 范围规划。

## 发布基线
- 候选 tag：`v1.0.0-rc1`
- 候选提交：`6cd3bbc`
- 正式发布分支：`release/v1.0`
- `v1.1` 集成分支：`dev/community-v1`
- 当前策略：`docs/release/community-v1-ga-stabilization.md`

## 发布前必须满足
- [ ] `release/v1.0` 最新待发布 head 的 `MMMail CI` 为绿色
- [ ] 不存在打开状态的 `release-blocking` issue
- [ ] `community-v1-final-signoff.md` 仍有效
- [ ] `community-v1-known-issues.md` 已按最新状态复核
- [ ] `community-v1-support-boundaries.md` 已按最新状态复核
- [ ] `docs/release/community-v1-v1.0.0-release-notes.md` 已按 `release/v1.0` 待发布 head 复核完成
- [ ] 自托管反馈已完成分流：
  - [ ] `release-blocking`
  - [ ] `v1.0.0` 文档勘误
  - [ ] `v1.1`
  - [ ] `post-v1.1`

## 正式发布动作
1. 确认 `release/v1.0` 待发布 head 对应 workflow 为绿色。
2. 若 `release/v1.0` 待发布提交已偏离 `v1.0.0-rc1`，先更新正式 release notes、known issues 与 signoff 引用。
3. 从 `release/v1.0` 的最新已验证 head 创建 `v1.0.0` tag。
4. 发布 GitHub Release。
5. 在 `README` 与发布文档中将当前阶段切换到 `v1.0.0`。

## 发布后动作
- 将新增反馈继续按 `community-v1-feedback-intake.md` 分流。
- 非 release-blocking 项不回流到 `v1.0.0`。
- `v1.1` 仅按 `community-v1-v1.1-plan.md` 推进。
