---
name: Release-blocking regression
about: 报告阻塞 Community Edition 当前公开基线的回归问题
title: "[Release Blocker] "
labels: bug,release-blocking,ga-stabilization
assignees: ""
---

## Blocker summary

> 若涉及 live security vulnerability 或未公开披露的越权问题，请不要公开提交 issue，改按 `SECURITY.md` 处理。只有已公开披露或已修复后的安全 / 越权回归才使用本模板。

## Affected GA module
- [ ] `Auth / Session / MFA`
- [ ] `Organization / RBAC / Admin`
- [ ] `Mail`
- [ ] `Calendar`
- [ ] `Drive`
- [ ] `Workspace Shell / Settings`
- [ ] `Install / Upgrade / Backup / Restore`
- [ ] `Security / Observability / CI`

## Why this blocks the current public baseline
- [ ] `GA` 主路径不可用
- [ ] 数据错误或数据丢失风险
- [ ] 安装 / 升级 / 恢复阻断
- [ ] 已公开披露或已修复后的安全 / 越权回归
- [ ] 最新 `main`、活跃 `dev/v*` 或 `release/v*` workflow 失绿

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
- GitHub Actions / artifact / workflow link:

## Environment
- Deployment mode:
- Browser / OS:
- Database / Redis / Nacos:
- Commit / branch:

## Minimal fix boundary
- [ ] 仅修复 release-blocking 本身
- [ ] 不扩产品范围
- [ ] 不引入大范围重构
