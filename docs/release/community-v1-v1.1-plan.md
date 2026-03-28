# Community Edition v1.1 Planning Baseline

**版本**: `v1.1-planning-draft`
**日期**: `2026-03-28`
**作者**: `Codex`

## 版本目标
`v1.1` 不扩产品面，集中做四条线：
1. `Docs`
2. `Sheets`
3. 国际化治理
4. Community / Hosted 边界继续收敛

## 工作流入口条件
- `v1.0.0` 已正式发布。
- `dev/community-v1` 最新 head 连续保持绿色。
- `v1.0.0` 的 release-blocking 缺陷已清零。

## Stream A - Docs
### 目标
- 将当前 `BETA` 提升到稳定的单人编辑 / 轻协作能力。

### 范围
- 文档创建、编辑、保存、列表
- 基础权限与组织边界
- 基础导入导出
- 非实时协作下的稳定性

### 不做
- 实时多人协作
- 评论线程
- 富协同引擎

## Stream B - Sheets
### 目标
- 将当前 `BETA` 提升到稳定的单人编辑 / 轻协作能力。

### 范围
- 表格创建、编辑、保存、分页
- 基础导入导出
- 错误态与恢复
- 组织边界与权限回归

### 不做
- 实时协作
- 复杂公式引擎
- 企业级流程编排

## Stream C - 国际化治理
### 目标
- 建立简体中文 / 繁体中文 / 英语的工程化治理。

### 范围
- 文案抽取
- 缺失检查
- 术语表
- CI 校验
- 页面覆盖率统计

## Stream D - Community / Hosted 边界
### 目标
- 继续把 Community 和 Hosted 的支持边界写死。

### 范围
- `Billing` 边界说明
- Hosted 才承诺的能力清单
- Community 自托管责任边界
- README / Roadmap / Support Boundaries 对齐

## 明确不进入 `v1.1`
- `VPN`
- `Meet`
- `Wallet`
- `Lumo`
- `Pass` 浏览器扩展与 passkeys 完整生命周期
- 真实 Billing 支付链路

## 交付标准
- 每条 stream 都要有独立 release-blocking 集合。
- 继续保持范围收敛，不把 `Preview` 混入 `GA / Beta` 叙事。
- 每条 stream 必须同步文档、测试与门禁，不接受“先做功能后补治理”。
