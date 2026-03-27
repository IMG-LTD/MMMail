# 前后端联调报告（v78）

**版本**: v78.0  
**日期**: 2026-03-08  
**主题**: Sheets Formula + Import/Export Foundation

## 1. 联调环境
- 前端：`http://127.0.0.1:3001`
- 后端：`http://127.0.0.1:8080`
- 健康检查：`curl -sf http://127.0.0.1:8080/actuator/health`
- 浏览器：Playwright MCP

## 2. 联调主链路
### 2.1 创建 workbook
- 浏览器进入 `/sheets`
- 点击 `New workbook`
- 创建 `V78 UAT Workbook`
- 页面成功进入 workbook detail 状态

### 2.2 公式编辑与保存
- 通过 formula bar 分别写入：
  - `A1 = 10`
  - `B1 = 20`
  - `C1 = =SUM(A1:B1)`
- 点击 `Save changes`
- 页面成功显示：
  - `Formula cells = 1`
  - `Computed preview = 30`

### 2.3 导入验证
- 通过导入面板上传 CSV：`5,7,=SUM(A1:B1)`
- 自动创建 `Imported UAT Sheet`
- 页面成功显示：
  - `1 rows × 3 columns`
  - `1 formulas`
  - `Computed preview = 12`

### 2.4 导出验证
- 在 `Imported UAT Sheet` 点击 `Export`
- 页面显示最近导出摘要：
  - `imported-uat-sheet-2026-03-08.csv`
  - `CSV`
  - 导出时间戳
- 浏览器下载行为成功触发

## 3. 浏览器质量信号
- 浏览器控制台 `error`：`0`
- UAT 截图：`docs/assets/v78-sheets-uat.png`

## 4. 联调期间的真实问题
### 4.1 前端测试类型导致 build 失败
- 现象：`pnpm build` 初次执行时，`tests/sheets-business.spec.ts` 中 `supportedImportFormats` / `supportedExportFormats` 的类型被推断为 `string[]`，与 `SheetsWorkbookDetail` 不兼容。
- 处理：为测试样例显式标注 `SheetsWorkbookDetail`。
- 结果：build 恢复通过。

### 4.2 旧运行态风险
- 现象：本仓库此前已存在旧后端/前端会话，若直接验收可能会打到旧版本。
- 处理：显式释放 `8080` 和 `3001` 端口，并重新启动当前代码。
- 结果：本次 UAT 明确基于 v78 代码完成。

## 5. 联调结论
- v78 前后端联调通过。
- `Sheets` 已具备完整闭环：
  - blank workbook create
  - formula authoring
  - save + computed preview
  - CSV import
  - CSV export
  - readiness / insight surface
