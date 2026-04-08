# Community Edition v1.6 Planning Baseline

**版本**: `v1.6-planning-batch-1`
**日期**: `2026-04-08`
**作者**: `Codex`

## 基线
- 当前公开基线：`main` → `v1.5.0`
- 当前开发分支：`dev/v1.6`
- `v1.6` 不继续扩产品宽度，转而收窄到“产品聚焦 + 信息架构重构 + 边界可信度提升”。

## 核心目标
1. 把 `/suite` 从单页混排改为明确 section 化视图。
2. 把 `/labs` 默认目录收敛到与主战略相邻的模块。
3. 修复 `community-boundary`、canonical docs 与版本元数据的漂移。
4. 把无障碍从静态基线提升到运行时自动化校验。
5. 完成 `v1.6.0` 的定向验证、发布文档和 release 收口。

## 明确不进入 v1.6
- `SMTP inbound / IMAP / Bridge`
- 完整零知识邮件架构
- 多设备密钥同步 / 真正密钥轮换
- `VPN / Meet / Wallet / Lumo` 深化

## 交付物
- `/suite` section navigation + overview + focused section containers
- `/labs` curated catalog
- `community-v1-support-boundaries.md`、module maturity、boundary locale 对齐 `v1.6`
- 运行时 a11y 自动化测试
- `v1.6.0` release docs 与验证记录
