# Community Edition v1.0 Scope

**版本**: `v1.0-draft`  
**日期**: `2026-03-13`  
**作者**: `Codex`

## 版本目标
- 本版目标是交付一个**可自托管、可安装、可运维、可开源协作**的 `Community Edition v1.0`。
- 本版不追求 `proton.me` 全产品完整复刻，也不把深引擎产品作为首发阻塞项。

## GA（正式交付）
- `Auth / Session / MFA`
- `Organization / Tenant / RBAC / Admin`
- `Mail`
- `Calendar`
- `Drive`
- `Workspace Shell / Settings`
- `Suite`（仅作为统一入口与工作区壳层）
- 部署、初始化、升级、回滚、备份恢复
- 开源治理与文档

## Beta（可用但不承诺完整）
- `Docs`
- `Sheets`
- `Billing`（当前承载于 `Suite`，Community 首发不承诺真实支付闭环）

## Preview / Experimental
- `Pass`
- `Authenticator`（独立产品形态）
- `SimpleLogin`
- `Standard Notes`
- `VPN`
- `Meet`
- `Wallet`
- `Lumo`
- `Collaboration`
- `Command Center`
- `Notifications`

## 默认导航策略
- 默认导航只暴露 `GA + Beta`：
  - `Mail`
  - `Calendar`
  - `Drive`
  - `Docs`
  - `Sheets`
  - `Suite`
  - `Business`
  - `Organizations`
  - `Settings`
  - `Security`
  - `Labs`
- 所有 `Preview` 模块移入 `Labs`，不再作为首发默认叙事。

## 明确不包含
- `VPN` 真实 tunnel
- `Meet` 媒体引擎与录制
- `Wallet` 链上签名与广播
- `Pass` 浏览器扩展与 passkeys 生命周期
- `Lumo` 产品化能力
- `Docs / Sheets` 实时协作
- `Billing` 真实支付、税费、发票下载闭环

## 已知限制
- Preview 模块当前仍保留源码与路由，但已从默认导航移除。
- `Authenticator` 仍保留为恢复链路组件，不作为首发独立产品承诺。
- `Suite` 当前仍承载部分治理与账单实验能力，后续批次会继续收口。
