# Community Edition Release Manager Brief

**版本**: `v1.2-released-post-v1.2-active`
**日期**: `2026-04-02`
**作者**: `Codex`

## 当前状态
- 当前正式版本：`v1.2.0`
- 当前发布分支：`release/v1.2`
- 正式 tag / commit：`v1.2.0` / `38e548a1baa56f70f29841d094fa8f927367b1d9`
- GitHub Release：`https://github.com/IMG-LTD/MMMail/releases/tag/v1.2.0`
- `release/v1.0` 继续承接 `v1.0.x`
- `dev/community-v1` 已回到 `post-v1.2` 主线开发职责

## 当前分支职责
- `release/v1.2`
  - 只接收 `v1.2.x` 的 `release-blocking / security / metadata` 修复
- `release/v1.0`
  - 继续维护 `v1.0.x`
- `dev/community-v1`
  - 承接 `post-v1.2` 已批准范围
  - 保持与后续 `main` 的收敛关系

## 入口文档
- `v1.2` 规划基线：`docs/release/community-v1-v1.2-plan.md`
- `v1.2` 主线路线：`docs/release/community-v1-v1.2-mainline-roadmap.md`
- `v1.2` 能力边界：`docs/release/community-v1-v1.2-capability-boundaries.md`
- `v1.2` release checklist：`docs/release/community-v1-v1.2-release-checklist.md`
- `v1.2` final signoff：`docs/release/community-v1-v1.2-final-signoff.md`
- `v1.2.0` release notes：`docs/release/community-v1-v1.2.0-release-notes.md`
- 支持边界：`docs/release/community-v1-support-boundaries.md`
- 模块成熟度：`docs/open-source/module-maturity-matrix.md`

## 当前允许
- `release/v1.2` 上的 `release-blocking / security / metadata` 最小修复
- release notes / checklist / signoff / support boundary 的事实性回填
- `dev/community-v1` 继续承接 `post-v1.2` 范围

## 当前禁止
- 扩大 `v1.2` 范围
- 在 `release/v1.2` 上继续开发下一阶段功能
- 改写 `v1.2.0` tag 指向
- 把 `Preview` 模块提升为正式发布承诺
