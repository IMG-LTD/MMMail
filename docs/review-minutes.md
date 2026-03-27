# 评审纪要（v78）

**版本**: v78.0  
**日期**: 2026-03-08  
**主题**: Sheets Formula + Import/Export Foundation

## 1. 本轮评审目标
- 把 `Sheets` 从纯 grid CRUD 提升到更接近 Proton Sheets 的生产力工作台
- 在不引入 silent fallback 的前提下，补齐公式、导入、导出主链路
- 保持代码结构可持续，避免 `pages/sheets.vue` 继续膨胀失控

## 2. 关键结论
### 2.1 后端实现决策
- **公式计算采用 Apache POI**，不自写 parser。
- workbook 继续复用 `sheets_workbook.grid_json` 存 raw grid，不新增表结构。
- detail 接口返回：
  - `grid`
  - `computedGrid`
  - `formulaCellCount`
  - `computedErrorCount`
  - `supportedImportFormats`
  - `supportedExportFormats`
- 新增接口：
  - `POST /api/v1/sheets/workbooks/import`
  - `GET /api/v1/sheets/workbooks/{workbookId}/export`

### 2.2 范围边界决策
- 本轮支持：`CSV / TSV / XLSX` 导入，`CSV / TSV / JSON` 导出。
- 本轮**显式不支持**：
  - 多 sheet tabs
  - 实时协作 / CRDT
  - chart renderer
  - history / conditional formatting / validation
- 对于多 sheet XLSX，后端显式报错，不做“只导第一张”的 silent fallback。

### 2.3 前端架构决策
- `pages/sheets.vue` 已拆成轻页面壳 + `useSheetsWorkspace.ts` orchestrator。
- 新增组件：
  - `SheetsFormulaPanel.vue`
  - `SheetsImportExportPanel.vue`
  - `SheetsInsightRail.vue`
- `SheetsGridEditor.vue` 改为“computed display + active cell raw editing”的双视角。

## 3. 评审中确认的质量约束
- 错误必须显式暴露；公式错误统一显示为显式 error state。
- 导入失败必须明确提示，不使用 mock success。
- Workbook 并发保存继续依赖 `currentVersion`。
- 页面层与 API 层解耦，组件不直接请求后端。

## 4. 需要保留给下一轮的事实
- 当前 `Sheets` 已补齐 foundation，但与官方仍有明显差距：
  - 多 tab
  - 共享与权限
  - 实时协作
  - chart / filter / sort / history
- `frontend build` 已通过，但存在一个 >500 kB 的大 chunk 警告，说明后续需要做更细的代码分片。

## 5. 评审结论
- v78 范围合理、实现路径清晰、验证证据充足。
- 本轮可以视为 `Sheets workspace foundation` 已完成。
- 下一轮若继续推进 `Sheets`，应优先围绕“多 sheet + share/collaboration + Drive 内联工作流”展开，而不是回头重做本轮基础层。
