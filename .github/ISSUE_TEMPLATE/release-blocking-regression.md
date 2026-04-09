---
name: Release-blocking regression
about: 报告阻塞 Community Edition v1.6.1 当前公开基线的回归问题
title: "[Release Blocker] "
labels: bug,release-blocking,ga-stabilization
assignees: ""
---

## Blocker summary

## Affected GA module
- [ ] `Auth / Session / MFA`
- [ ] `Organization / RBAC / Admin`
- [ ] `Mail`
- [ ] `Calendar`
- [ ] `Drive`
- [ ] `Workspace Shell / Settings`
- [ ] `Install / Upgrade / Backup / Restore`
- [ ] `Security / Observability / CI`

## Why this blocks `v1.6.1`
- [ ] `GA` 主路径不可用
- [ ] 数据错误或数据丢失风险
- [ ] 安装 / 升级 / 恢复阻断
- [ ] 安全 / 越权回归
- [ ] 最新 `main` 或活跃 `dev/v*` CI 失绿

## Last known good baseline
- Tag / commit:
- Workflow run:

## Reproduction
1.
2.
3.

## Evidence
- Logs:
- Screenshots:
- `bash scripts/validate-local.sh`:
- `bash scripts/validate-all.sh`:
- GitHub Actions / artifact:

## Environment
- Deployment mode:
- Browser / OS:
- Database / Redis / Nacos:
- Commit / branch:

## Minimal fix boundary
- [ ] 仅修复 release-blocking 本身
- [ ] 不扩产品范围
- [ ] 不引入大范围重构
