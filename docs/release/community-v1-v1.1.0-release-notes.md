# Community Edition v1.1.0 Release Notes Draft

**版本**: `v1.1.0-release-draft`
**日期**: `2026-04-01`
**作者**: `Codex`

## 当前状态
- 当前文档为 `v1.1.0` 正式 release notes 草稿。
- 发布分支：`release/v1.1`
- 候选提交：`a6bdda20cfbf6c2f040a4141c220f5426ae3d7b2`
- 来源 CI：`https://github.com/IMG-LTD/MMMail/actions/runs/23834450221`
- 正式发布日期：`待发布`
- 正式 tag / commit：`待发布`

## Summary
- `MMMail Community Edition v1.1.0` 在 `v1.0.0` 正式版基础上，完成了 `Docs / Sheets / i18n / Community / Hosted` 四条线的第一阶段收口。
- 本次版本不扩产品面，不引入新的 `Preview` 模块，也不调整 `GA / BETA / PREVIEW` 的大范围分级。
- `Docs` 与 `Sheets` 继续保持 `BETA`，但已从“可用”提升到“稳定的单人编辑 / 轻协作”运行态。

## Since `v1.0.0`
- `v1.0.0..a6bdda2` 的提交只包含以下 9 个批次，没有引入额外产品扩面：
  - `6b01ae6` `feat: stabilize docs route selection`
  - `241b656` `docs: split v1.0 release line from v1.1 development`
  - `eb03389` `feat: harden docs beta stability gates`
  - `c3e4f00` `feat: bootstrap i18n governance gates`
  - `d26ee2f` `feat: add i18n page coverage reporting`
  - `a9d8e76` `feat: localize mail compose workflow`
  - `7e12f4a` `feat: localize mail detail and conversation pages`
  - `1596c17` `feat: localize contacts workspace`
  - `8f34286` `feat: add community hosted boundary panel`
  - `a6bdda2` `feat(frontend): complete v1.1 phase-one hardening`

## Included
- `Docs`
  - 深链恢复、未保存保护、导入导出、轻协作/建议/评论页级 smoke、组织边界回归
- `Sheets`
  - 工作区 route/runtime/leave guard、状态恢复、sharing/version/incoming/collaboration follow-through guard
  - reviewer 指出的 `DECLINED collaboratorCount` 与 `refresh fallback double confirm` 回归已修复
- `i18n`
  - 关键页面 `useI18n + useHead` 接线补齐
  - 页面级国际化覆盖率提升到 `100%`
- `Community / Hosted`
  - `Billing center` 作为 `Suite` 内 `BETA` 子入口进入统一边界 registry
  - 支持边界、README、module maturity 与前端边界面板口径一致

## Validation Evidence
- 本地默认门禁：`bash scripts/validate-local.sh`
- 来源分支 CI：`https://github.com/IMG-LTD/MMMail/actions/runs/23834450221`
- `release/v1.1` 分支 CI：待补

## Support Boundaries
- `GA`
  - `Auth / Session / MFA`
  - `Mail`
  - `Calendar`
  - `Drive`
  - `Suite Shell / Settings / Security / System Health`
  - `Business / Admin`
- `BETA`
  - `Docs`
  - `Sheets`
  - `Billing center`（仅 `Suite` 子入口的 Community 状态展示）
- `PREVIEW`
  - `Pass`
  - `Authenticator`
  - `SimpleLogin`
  - `Standard Notes`
  - `VPN`
  - `Meet`
  - `Wallet`
  - `Lumo`

## Deferred / Out of Scope
- 实时多人协作
- 复杂公式引擎
- 真实 Billing 支付 / 税费 / 发票闭环
- `VPN / Meet / Wallet / Lumo` 的成熟度提升

## 发布前待补
- `release/v1.1` 对应 workflow run 链接
- 最终 `v1.1.0` tag / commit
- Release owner / 审核人 / 正式发布日期
