# 安全审计（v78）

**版本**: v78.0  
**日期**: 2026-03-08  
**主题**: Sheets Formula + Import/Export Foundation

## 1. 本轮安全关注点
- workbook import 是否具备显式格式边界与失败暴露
- workbook export 是否严格限制在当前用户 own workbook
- formula evaluation 是否会吞错或产生隐式降级
- stale version 写入保护是否在新链路中继续有效

## 2. 已确认的安全性与约束
### 2.1 访问边界
- `Sheets` 接口继续受登录态保护。
- workbook 获取、更新、导出、删除均通过 owner 归属校验。
- 导出接口不会跨用户读取 workbook。

### 2.2 导入边界
- 本轮仅允许 `CSV / TSV / XLSX`。
- unsupported format 返回显式错误码：`SHEETS_WORKBOOK_IMPORT_INVALID`。
- 多 sheet XLSX 不做 silent fallback，而是显式报错。

### 2.3 公式错误暴露
- raw formula 继续保存为用户输入值。
- 公式解析失败或 evaluator 失败时，前后端显示显式错误状态，不吞错。
- 没有引入 mock success 或隐藏性兜底路径。

### 2.4 并发完整性
- `currentVersion` 继续保护 workbook 更新。
- stale version 请求仍然返回冲突，不会静默覆盖更高版本数据。

## 3. 仍然存在的系统级差距
- 尚无真实 E2EE / zero-access 文档与表格密钥体系。
- 尚无多人协作权限模型、presence、审计级 revision history。
- 尚无上传内容安全扫描、恶意宏/复杂文件威胁治理体系。
- 尚无 Drive/Docs/Sheets 的共享链接、组织级访问控制与水印策略。

## 4. 审计结论
- v78 新增能力没有引入新的 silent fallback 或明显越权路径。
- 当前最大安全差距仍然是平台层：
  - 密钥模型
  - 共享权限
  - 协作审计
  - 多端同步
而不是本轮新增 API 的显式实现本身。
