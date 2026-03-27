# 后端测试报告（v78）

**版本**: v78.0  
**日期**: 2026-03-08  
**主题**: Sheets Formula + Import/Export Backend

## 1. 验证范围
- `SheetsFormulaService`
- `SheetsImportExportService`
- `SheetsService`
- `SheetsController`
- `SuiteInsightService` 中 `SHEETS` readiness 扩展
- `SheetsWorkbookIntegrationTest`

## 2. 执行结果
### 2.1 编译验证
命令：
- `cd backend && timeout 60s mvn -pl mmmail-server -am -DskipTests compile`

结果：
- **通过**

### 2.2 集成测试
命令：
- `cd backend && timeout 60s mvn -pl mmmail-server -am -Dtest=SheetsWorkbookIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`

结果：
- **通过**
- `2` 个测试用例通过

## 3. 已验证场景
### 3.1 公式链路
- 新建 workbook
- 保存 raw formula
- 获取 `computedGrid`
- 返回 `formulaCellCount`
- 返回 `computedErrorCount`

### 3.2 导入导出链路
- CSV 导入 workbook
- 导入后立即获得 computed result
- CSV 导出 computed values
- JSON 导出 raw grid + computedGrid + metadata

### 3.3 并发保护
- stale `currentVersion` 写入继续返回 `409 / 30032`

## 4. 设计约束验证
- 多 sheet XLSX 不会被静默截断，而是显式报错。
- unsupported format 不会 silent fallback，而是显式错误码返回。
- `SuiteInsightService` 已新增 `sheets_formula_cell_count` 信号，Sheets readiness 不再只看 workbook 数量。

## 5. 结论
- v78 后端基础能力已达到可联调、可回归状态。
- 当前后端最大剩余差距不在本轮 API 本身，而在下一层平台能力：
  - 多 sheet 模型
  - 共享权限
  - 实时协作
  - 历史版本

---

# 后端测试报告（Drive GA）

**版本**: Community Edition v1.0 / Batch 5C  
**日期**: 2026-03-13  
**主题**: Drive GA release-blocking backend

## 1. 验证范围
- `DriveController`
- `DriveService`
- `DriveCollaborationService`
- `DriveReleaseBlockingIntegrationTest`
- `DriveCollaboratorShareIntegrationTest`
- `DriveSharedWithMeIntegrationTest`
- `DriveSecureShareIntegrationTest`
- `DrivePublicFolderShareIntegrationTest`

## 2. 执行结果
### 2.1 集成测试
命令：
- `cd backend && timeout 60s mvn -pl mmmail-server -Dtest=DriveReleaseBlockingIntegrationTest,DriveCollaboratorShareIntegrationTest,DriveSharedWithMeIntegrationTest,DriveSecureShareIntegrationTest,DrivePublicFolderShareIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`

结果：
- **通过**
- `5` 个测试类通过
- `8` 个测试用例通过

## 3. 已验证场景
- 批量公开分享支持部分成功 / 部分失败回执
- 跨组织下载、重命名、删除均被显式拒绝
- 文件大小限制与存储配额返回稳定错误码
- 版本上传、版本恢复、下载与回收站链路稳定
- 协作者共享、公开文件夹共享、Shared with me 入口维持可用

## 4. 结论
- Drive 后端已达到首发级 release-blocking 回归强度，并接入默认本地门禁。
- 当前后端剩余工作已不在 Drive 核心功能闭环，而在后续可观测性、运维与全链路发布门禁。
