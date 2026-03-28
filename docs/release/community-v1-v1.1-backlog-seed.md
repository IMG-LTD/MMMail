# Community Edition v1.1 Backlog Seed

**版本**: `v1.1-backlog-seed`
**日期**: `2026-03-28`
**作者**: `Codex`

## 目标
- 给 `v1.1` 的四条主线提供统一 backlog 落点。
- 避免 `v1.0.0` 稳定窗口内的非阻塞需求回流到当前版本。

## 使用规则
- 仅记录 `v1.1` 已批准范围内的 backlog。
- 每条 backlog 必须绑定一个 stream、labels、验收方向。
- 不在这里记录 `release-blocking` 或 `post-v1.1` 项。

## Stream A - Docs
### 建议 backlog
1. 单文档编辑稳定性与保存恢复
2. 文档列表 / 搜索 / 空态整理
3. 基础导入导出兼容
4. 组织边界与权限回归补强

### 标签
- `v1.1-docs`

## Stream B - Sheets
### 建议 backlog
1. 单表编辑稳定性与保存恢复
2. 分页与基础筛选体验补强
3. 导入导出最小兼容能力
4. 权限与组织边界回归补强

### 标签
- `v1.1-sheets`

## Stream C - i18n
### 建议 backlog
1. 前后端文案抽取与缺失检查
2. 简体中文 / 繁体中文 / 英语术语表
3. CI 文案缺失门禁
4. 页面覆盖率统计

### 标签
- `v1.1-i18n`

## Stream D - Community / Hosted
### 建议 backlog
1. `Billing` 与 Hosted-only 能力边界文档收口
2. README / Support Boundaries / Roadmap 继续对齐
3. 自托管责任边界与 SLA 预期说明
4. Community-Hosted 入口与文档跳转统一

### 标签
- `community-hosted-boundary`

## 不进入 `v1.1`
- `VPN`
- `Meet`
- `Wallet`
- `Lumo`
- `Pass` 扩展能力
- 真实支付闭环
- 新一级产品入口

## issue 落地格式
每条 `v1.1` issue 至少包含：
- 所属 stream
- 用户场景
- 当前缺口
- 最小验收标准
- 不做范围
- 对应 labels
- milestone：`v1.1`
