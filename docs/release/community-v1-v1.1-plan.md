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

## 推进顺序
1. `Docs`
2. 国际化治理
3. Community / Hosted 边界
4. `Sheets`

## 分支边界
- `dev/community-v1`：承载 `v1.1`
- `release/v1.0`：承载 `v1.0.0` 与 `v1.0.x`
- 当前 `Docs` 深链与恢复改动（`6b01ae6`）属于 `v1.1`，不回灌 `v1.0.0`

## Stream A - Docs
### 目标
- 将当前 `BETA` 提升到稳定的单人编辑 / 轻协作能力。

### 当前批次
- 已在 `dev/community-v1` 落地首批列表稳定性改进：
  - `keyword` / `scope` 深链恢复
  - 筛选后可见笔记选中恢复
  - 空结果态与筛选态文案收口
  - 前端路由与文案回归测试纳入默认门禁
- 已开始第二批保存稳定性改进：
  - 本地草稿快照与恢复提示
  - 保存失败后的本地恢复路径
  - 协作活动提示文案国际化
- 已落地基础导入导出能力：
  - 本地 `Markdown / TXT` 导入到编辑区
  - 当前文档导出为 `Markdown / TXT`
  - 导入导出工具与文案回归测试纳入默认门禁
- 已补齐未保存保护与展示收口：
  - 切换笔记 / 离开页面前的未保存确认
  - 浏览器刷新/关闭前的 beforeunload 保护
  - 权限、范围、未保存状态与未命名标题去硬编码
- 已纳入 Docs 浏览器等价 smoke：
  - 覆盖草稿恢复、未保存切换确认、搜索/深链恢复、导入导出页面接线
  - `vitest` Docs 回归集 `19/19` 通过，`nuxi typecheck` 通过
  - 运行态回归已进入默认前端门禁
- 已收口活动文档边界：
  - 修复新建文档后 `selectNote()` 被活动文档短路、导致详情/协作数据未加载的问题
  - 导入流程要求存在活动文档且具备编辑权限，不再接受无文档上下文的文件变更事件
  - Docs 回归集提升为 `21/21` 通过，`nuxi typecheck` 通过
- 已补齐失效笔记恢复链：
  - 当 deep link 指向的笔记已删除或撤权、但列表仍残留旧摘要时，前端会清空失效上下文并自动回退到最近可访问笔记
  - 若当前筛选结果里已无可访问笔记，会清理 `noteId` 路由参数并保留删除态，而不是继续卡在失效笔记
  - Docs 回归集提升为 `22/22` 通过，`nuxi typecheck` 通过
- 已补齐 stale note 点击恢复链：
  - 用户点击列表中已经失效的笔记摘要时，前端会提示不可访问并自动回退到最近可访问笔记
  - 点击回退与 deep-link 回退共享同一恢复语义，不再出现“列表点击卡删除态、路由仍指向旧笔记”的分叉行为
  - Docs 回归集提升为 `23/23` 通过，`nuxi typecheck` 通过
- 已接入 Docs 专项门禁：
  - `scripts/validate-local.sh` 现显式执行前端 Docs 回归集与后端 Docs 协作/建议工作流/组织边界回归，不再只依赖全量测试顺带覆盖
  - Docs 后端协作/建议工作流测试已对齐到现有 `test` profile，本机默认门禁不再依赖外部 MySQL / Redis / Nacos 凭据
  - 已在本机执行 `DocsCollaborationIntegrationTest` 与 `DocsSuggestionWorkflowIntegrationTest`，共 `3/3` 通过
  - 已新增 `DocsOrgAccessIntegrationTest`，覆盖 org scope 下 `create / comment / collaboration` 端点在产品禁用与强制 2FA 策略下的 `403 / 30045 / 30046` 回归
  - `DocsOrgAccessIntegrationTest` 已纳入 `BACKEND_DOCS_TESTS`，成为默认本地门禁的一部分
  - 已在本机执行完整 `bash scripts/validate-local.sh`，默认门禁通过且不再受 live-stack 凭据注入影响
  - 已保留 `config/backend.test.env.example` 与 `scripts/validate-backend-test-env.sh` 作为可选 live-stack 配置校验入口
  - `validate` GitHub workflow 可继续直接复用默认测试语义，不需要额外 live-stack 前置条件
  - 这条门禁会随 `validate-all.sh` 与 `validate-ci.sh` 一起执行，成为 `dev/community-v1` 的默认验证链
- 已补齐筛选空结果下的未保存保护：
  - 当用户带着未保存改动应用 `keyword` / `scope` 筛选，且新结果会清空当前文档选择时，前端现在会先弹确认，不再静默重置编辑器
  - 如果用户取消离开，页面会恢复到上一次已应用的筛选与路由状态，避免出现“输入框已改、列表已空、草稿被清掉”的分叉状态
  - Docs 前端专项回归集提升为 `24/24` 通过，`nuxi typecheck` 通过
- 已补齐导入覆盖前的未保存保护：
  - 当当前文档存在未保存本地改动时，导入 `Markdown / TXT` 会先要求确认，不再静默覆盖编辑区内容
  - 用户取消导入时会保留原始编辑内容；确认导入后才会替换编辑区并进入本地待保存状态
- 已补齐手动刷新前的未保存保护：
  - 当当前文档存在未保存本地改动时，手动 `Refresh workspace` 会先要求确认，不再静默用服务端详情覆盖编辑区
  - 用户取消刷新时会保留当前本地编辑内容；确认刷新后才会重新拉取详情、协作和建议数据
- 已补齐协作 / 建议面板的页面级运行态回归：
  - 新增 `frontend/tests/docs-panels.smoke.spec.ts`，直接通过 `DocsPage` 挂载真实 `DocsShareManager` 与 `DocsSuggestionInbox`
  - 覆盖分享创建、权限切换、撤销分享，以及建议创建、接受、拒绝的页面接线与消息反馈
  - 该回归已并入 `FRONTEND_DOCS_TESTS`，作为默认 Docs 前端门禁的一部分
- 已补齐评论面板的页面级运行态回归：
  - 新增 `frontend/tests/docs-comments.smoke.spec.ts`，覆盖引用摘录评论、评论创建与评论解决的页面接线
  - 该回归已并入 `FRONTEND_DOCS_TESTS`，与协作 / 建议面板一起成为默认 Docs 前端门禁的一部分
- 已补齐 Docs 离页保护回归：
  - 新增 `frontend/utils/docs-leave-guard.ts`，将浏览器 `beforeunload` 与 route leave guard 抽离为独立 util，避免继续堆叠 `frontend/pages/docs.vue`
  - 新增 `frontend/tests/docs-leave-guard.spec.ts`，覆盖 dirty editor 下的 `beforeunload` 阻断、listener cleanup 与 route leave confirm 语义
  - `scripts/validate-local.sh` 已将 `docs-leave-guard.spec.ts` 纳入默认 `FRONTEND_DOCS_TESTS`，确保 Docs 未保存离页保护持续回归

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

### 当前批次
- 已开始第一批 `Sheets` 状态恢复与门禁硬化：
  - `workbookId / view / scope` 深链状态抽离到统一 route helper，刷新与切换工作簿不再丢失工作区视图和范围筛选
  - 手动 `Refresh workspace` 先做未保存确认，确认后会真正重拉当前工作簿详情，不再只刷新列表后仍保留脏编辑
  - 已新增 `frontend/tests/sheets-workspace-route.spec.ts` 与 `frontend/tests/sheets-workspace.spec.ts`，覆盖 deep-link 恢复、query 回写、刷新前 discard confirm 与详情重载
  - `scripts/validate-local.sh` 已显式接入前端 `Sheets` 专项回归与后端 `SheetsWorkbook* / SheetsSharingVersion*` 集成回归，`validate-all / validate-ci` 将随默认门禁一并执行
- 已补页面级协作 / 版本运行态第一批：
  - `SheetsShareManager / SheetsIncomingSharesPanel / SheetsVersionHistoryDrawer` 已接入页面可见错误态，不再只依赖 `ElMessage` 瞬时提示
  - 新增 `frontend/tests/sheets-panels.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖分享邀请、权限切换、撤销分享、incoming refresh / accept / open、版本恢复
  - `scripts/validate-local.sh` 已将 `sheets-panels.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保页面接线和错误可见性持续回归
- 已补导入导出 / 协作侧栏运行态第二批：
  - `SheetsImportExportPanel` 与 `SheetsCollaborationRail` 已补稳定 `data-testid` 选择器，避免页面级 smoke 继续依赖脆弱文本匹配
  - 新增 `frontend/tests/sheets-trade-collaboration.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖导入、导出、模板建表、协作事件打开
  - `scripts/validate-local.sh` 已将 `sheets-trade-collaboration.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保 trade / collaboration 主路径持续回归
- 已补结构操作 / 保存恢复运行态第三批：
  - `SheetsWorkspaceHero`、`SheetsWorkbookSidebar`、`SheetsWorkbookTabs` 与页面冲突提示已补稳定 `data-testid`，覆盖 create / refresh / save、工作簿切换、工作簿 rename/delete、sheet select/create
  - 新增 `frontend/tests/sheets-structure.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖结构操作页面接线，并显式断言冲突提示可见
  - `scripts/validate-local.sh` 已将 `sheets-structure.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保 structure / save 主路径持续回归
- 已补数据工具 / 公式运行态第四批：
  - `SheetsDataToolsPanel` 与 `SheetsFormulaPanel` 已补稳定 `data-testid`，覆盖 search、sort、freeze、formula input 这条核心编辑链
  - 新增 `frontend/tests/sheets-tools-formula.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖搜索更新、排序、冻结、公式编辑页面接线
  - `scripts/validate-local.sh` 已将 `sheets-tools-formula.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保编辑工具主路径持续回归
- 已补 Grid 主编辑运行态第五批：
  - `SheetsGridEditor` 已补稳定 `data-testid`，覆盖单元格选择与激活单元格编辑，不再依赖 DOM 结构猜测
  - 新增 `frontend/tests/sheets-grid.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖 cell select 与 active cell input edit 页面接线
  - `scripts/validate-local.sh` 已将 `sheets-grid.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保 Grid 主编辑链持续回归
- 已补状态边界运行态第六批：
  - `SheetsDataToolsPanel`、`SheetsFormulaPanel`、`SheetsGridEditor` 已补稳定 `data-testid`，显式暴露 `readonly / no-selection / loading / empty` 关键状态
  - 非 owner 用户的 `clear freeze` 入口已按真实权限边界禁用，不再绕过排序 / 冻结的 owner-only 约束
  - 新增 `frontend/tests/sheets-state-boundary.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖工具栏空选区、view-only 公式栏、Grid loading / empty / readonly 三类边界
  - `scripts/validate-local.sh` 已将 `sheets-state-boundary.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保页级状态边界持续回归
- 已补工具栏 / 空工作区运行态第七批：
  - `frontend/pages/sheets.vue`、`SheetsWorkspaceHero`、`SheetsWorkbookSidebar` 已补稳定 `data-testid`，显式暴露 workspace view / scope filter / incoming refresh、hero 空态、sidebar loading / empty 状态
  - 新增 `frontend/tests/sheets-toolbar-empty.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖 library / incoming 切换、scope filter 更新、toolbar refresh incoming、empty hero 与 sidebar loading / empty
  - `scripts/validate-local.sh` 已将 `sheets-toolbar-empty.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保工具栏与空工作区运行态持续回归
- 已补分享 / 协作 / 版本边界第八批：
  - `SheetsShareManager` 已收口 view-only 共享工作簿的权限边界，不再向只读共享成员暴露权限下拉与 revoke 操作
  - `SheetsCollaborationRail` 与 `SheetsVersionHistoryDrawer` 已补稳定 `data-testid`，显式暴露 loading / empty / restore disable 状态
  - 新增 `frontend/tests/sheets-sharing-boundary.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖 readonly share、collaboration loading / empty、version restore current / no-permission 两类禁用边界
  - `scripts/validate-local.sh` 已将 `sheets-sharing-boundary.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保共享与版本边界持续回归
- 已补 trade / template 边界第九批：
  - `SheetsImportExportPanel` 与 `SheetsCollaborationRail` 已补稳定 `data-testid`，显式暴露 last import / export metadata、export disabled、template busy 与 collaboration count
  - 新增 `frontend/tests/sheets-trade-boundary.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖无工作簿下 export disable、导入导出摘要展示、template busy 禁用与协作计数展示
  - `scripts/validate-local.sh` 已将 `sheets-trade-boundary.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保 trade / template 边界持续回归
- 已补 no-workbook safety 第十批：
  - `SheetsShareManager` 已收口无活动工作簿时的 share create / empty 空白态，不再在无上下文时暴露邀请控件或渲染空白 share list
  - `SheetsVersionHistoryDrawer` 已补 `!workbook` 的 restore disable 边界，并显式暴露 version empty 状态
  - 新增 `frontend/tests/sheets-panel-safety.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖 owner share empty、no-workbook share hidden、version empty 与 no-workbook restore disabled
  - `scripts/validate-local.sh` 已将 `sheets-panel-safety.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保无工作簿安全边界持续回归
- 已补 incoming 边界第十一批：
  - `SheetsIncomingSharesPanel` 已补稳定 `data-testid`，显式暴露 incoming total / pending / accepted 计数、empty 状态、refresh 按钮与 accept / decline / open 动作入口
  - 新增 `frontend/tests/sheets-incoming-boundary.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖 incoming counts、empty state、mutation disabled 与 refresh / open 接线
  - `scripts/validate-local.sh` 已将 `sheets-incoming-boundary.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保 incoming 运行态持续回归
- 已补 InsightRail 运行态第十二批：
  - `SheetsInsightRail` 已补稳定 `data-testid` 与 `data-state`，显式暴露 scope / health / limits 三块区域与 readiness chips
  - 新增 `frontend/tests/sheets-insight-boundary.smoke.spec.ts`，通过 `frontend/pages/sheets.vue` 直接覆盖 no-workbook readiness、import / export 摘要、dirty / formula / error / format chips 页面接线
  - `scripts/validate-local.sh` 已将 `sheets-insight-boundary.smoke.spec.ts` 纳入默认 `Sheets` 前端门禁，确保 InsightRail 运行态持续回归
- 已补离页保护第十三批：
  - `useSheetsWorkspace` 已补齐未保存改动的离页保护，浏览器刷新 / 关闭走标准 `beforeunload`，从 `/sheets` 跳转到其他路由时复用既有 discard confirm
  - 新增 `frontend/utils/sheets-workspace-leave-guard.ts`，将浏览器离开保护抽到独立 helper，避免继续拉长 `useSheetsWorkspace`
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 dirty workbook 下的 `beforeunload` 阻断、route leave confirm 与 unmount cleanup
  - 该回归继续复用既有 `tests/sheets-workspace.spec.ts` 默认门禁入口，不额外分叉新的校验脚本
- 已补模板建表 / incoming open 的未保存保护第十四批：
  - `useSheetsWorkbench` 现会在模板建表前先执行 discard confirm，取消时不再先创建后遗留新工作簿
  - `useSheetsSharingVersionWorkbench` 现会在 incoming workbook 打开成功后才回写 `view / scope / workbookId`，取消切换时不再污染当前路由状态
  - `frontend/tests/sheets-workspace.spec.ts` 已补 composable 回归，覆盖“取消模板建表不触发 createWorkbook”与“取消 incoming open 不改 route state”
  - `frontend/tests/sheets-trade-collaboration.smoke.spec.ts` 已补页面回归，确认模板建表取消时不会误报成功提示
- 已补版本恢复 / 同工作簿 reload 未保存保护第十五批：
  - `useSheetsWorkspace.selectWorkbook()` 新增显式 `skipDiscardConfirm` 选项；默认只要存在本地脏编辑，即使是 reload 当前 workbook 也会先要求确认，不再静默用服务端详情覆盖本地改动
  - `useSheetsSharingVersionWorkbench.restoreVersion()` 改为在服务端恢复前先执行 discard confirm；确认后才真正 restore，随后 reload 当前 workbook 时跳过重复确认
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖“取消同 workbook reload 不触发 getWorkbook”“版本恢复在确认前不触发服务端 mutation”“恢复后不重复二次确认”三类场景
- 已补 rename 元数据保脏编辑第十六批：
  - `applySheetsWorkbookDetailState()` 新增 `preserveLocalState` 语义；当 workbook / sheet 结构未变时，可只更新服务端 detail 元数据而不清空 `localGrid / dirtyEdits`
  - `useSheetsWorkspace.onRenameWorkbook()` 与 `onRenameSheet()` 现走 metadata-only apply，不再因为 rename 成功后的 detail 回写而静默丢失未保存编辑
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 workbook rename 与 sheet rename 后 dirty edits 仍保留
- 已补 share metadata-only 刷新第十七批：
  - `useSheetsSharingVersionWorkbench.submitShare()` 与 `removeShare()` 不再为刷新 `collaboratorCount` 去 reload 当前 workbook
  - 分享创建 / 撤销后改为直接同步 `activeWorkbook` 与 summary list 的 `collaboratorCount`，避免在本地脏编辑存在时触发无意义的 discard confirm
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 share create / remove 后 metadata 更新且不触发 `selectWorkbook()`
- 已补 destructive action delete guard 第十八批：
  - `useSheetsWorkspace.onDeleteWorkbook()` 在删除当前 active workbook 且存在未保存改动时，会先执行 discard confirm；删除非当前 workbook 不额外打断当前编辑
  - `useSheetsWorkspace.onDeleteSheet()` 在删除当前 workbook 内任意 sheet 前，会先确认未保存改动，避免因 detail 重载而静默丢失本地编辑
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 active workbook delete cancel、inactive workbook delete 不强制 discard、sheet delete cancel 三类场景
- 已补 delete follow-through double confirm 第十九批：
  - `handleDeletedSheetsWorkbook()` 新增 `skipDiscardConfirm` 语义；当前 workbook 删除后若自动切到 next workbook，不再重复触发第二次 discard confirm
  - `useSheetsWorkspace.onDeleteWorkbook()` 在用户已确认 discard 的前提下，会把 follow-through workbook switch 作为同一条 destructive action 链路继续执行
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖“删除当前 workbook 并切到下一个 workbook 只确认一次”场景
- 已补 template follow-through double confirm 第二十批：
  - `useSheetsWorkbench.createWorkbookFromTemplate()` 在用户已确认 discard 的前提下，后续 `selectWorkbook()` 会显式跳过重复确认，不再二次打断模板建表
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖“dirty workspace 下模板建表只确认一次”场景
- 已补 refresh follow-through guard 第二十一批：
  - `useSheetsWorkspace.onRefreshWorkspace()` 现返回显式成功态；只有 workspace refresh 真正完成时，`useSheetsWorkbench` 才继续刷新 incoming / shares / collaboration
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖“取消 refresh 不触发 side-effect refresh”“refresh 成功仍保留 follow-through”两类场景
- 已补 collaboration refresh guard 第二十二批：
  - `useSheetsWorkspace` 的 `create / import / rename / delete / save / createSheet / renameSheet / deleteSheet` 现统一返回显式成功态，避免 `useSheetsWorkbench` 再无条件把取消或失败操作当成成功动作继续刷新协作面板
  - `useSheetsWorkbench` 新增统一 success-only refresh helper；只有真实成功的 workbook mutation 才刷新 `collaboration`，`export` 这类非变更动作不再触发无意义的协作刷新
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 create success / create cancel / save no-op / export success 四类协作刷新守卫场景
- 已补 share refresh integrity 第二十三批：
  - `refreshShares()` 与 `refreshIncomingShares()` 现返回显式成功态；share create / update / remove 与 incoming respond 的 follow-through 只有在列表 refresh 成功后才继续执行
  - `submitShare()` / `removeShare()` 在 refresh 失败时不再错误同步 `collaboratorCount`，也不再假报成功；incoming respond 在 refresh 失败时同样停止 success follow-through
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 share create/remove 与 incoming respond 在 refresh 失败时不报成功、不误刷 metadata 的场景
- 已补 incoming visible refresh integrity 第二十四批：
  - `refreshVisibleWorkbooks()` 现补齐显式成功态与本地错误暴露；incoming respond 只有在 incoming 列表与 visible workbook 列表都刷新成功后，才继续协作刷新与 success 叙事
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 incoming refresh 成功但 `listWorkbooks` 失败时不报成功、不继续 follow-through 的场景
- 已补 version restore integrity 第二十五批：
  - `restoreVersion()` 已将 restore mutation 与后续 workbook reload / versions refresh 的失败语义拆开；当 restore 已成功但 follow-through 失败时，不再误报 `versionRestoreFailed`
  - `openVersionHistory()` 与 restore follow-through 现复用统一 `refreshVersions()` helper，版本列表刷新失败会暴露真实错误来源
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖“reload 失败不误报 restore failed”“listVersions 失败不误报 restore failed”两类场景
- 已补 post-refresh side-effect guard 第二十六批：
  - `useSheetsWorkbench.onRefreshWorkspace()` 现按 `workspace -> incoming -> shares -> collaboration` 的成功态逐层推进；`incoming` 或 `shares` 任一失败时，不再继续刷新后续面板
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 workspace refresh 成功但 incoming / shares 刷新失败时的链路短路场景
- 已补 share list preserve 第二十七批：
  - `refreshShares()` 不再在请求开始前清空现有 share list；只有刷新成功后才替换列表，刷新失败时保留旧数据并暴露错误
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 share refresh 失败时旧 list 仍保留的场景
- 已补 share draft preserve 第二十八批：
  - `submitShare()` 现仅在 `refreshShares()` 成功后才清空 `inviteEmail / invitePermission`，避免 share create 成功但 refresh 失败时丢失当前 invite draft
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 share create 成功后清空 draft，以及 share refresh 失败时保留 draft 的场景
- 已补 version context reset 第二十九批：
  - `useSheetsSharingVersionWorkbench` 现会在 active workbook 或 restore capability 变化时重置 `versions / versionsError / versionDrawerVisible`，避免版本抽屉继续显示旧 workbook 的历史记录
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 workbook 切换时 version drawer 自动关闭并清空旧 versions 的场景
- 已补 share context reset 第三十批：
  - `useSheetsSharingVersionWorkbench` 现会在 active workbook 或 share management capability 变化时重置 `shares / invite draft / share mutation context`，避免 share panel 延续旧 workbook 的共享列表和邀请输入
  - `frontend/tests/sheets-workspace.spec.ts` 已补回归，覆盖 workbook 切换时 share list 与 invite draft 自动清空的场景
- 已补 stale request guard 第三十一批：
  - `useSheetsSharingVersionWorkbench` 现为 `refreshShares()` 与 `refreshVersions()` 引入 request token 守卫；workbook 切换后，旧 `listShares / listVersions` 返回结果不会再覆盖当前面板状态
  - `frontend/tests/sheets-workspace.spec.ts` 已补 race 回归，覆盖 workbook 切换后旧 share/version 请求被忽略的场景
- 已补 stale mutation follow-through 第三十二批：
  - `useSheetsSharingVersionWorkbench` 现为 `submitShare()`、`updateSharePermission()`、`removeShare()` 与 `restoreVersion()` 增加 workbook context 守卫；workbook 切换后，旧 mutation 完成后不再继续刷新当前 share/version 面板或触发成功提示
  - `restoreVersion()` 现于 discard confirm 之前锁定目标 `workbookId`，避免用户在确认期间切换 workbook 后，把 restore 请求误绑定到新上下文
  - `frontend/tests/sheets-workspace.spec.ts` 已补 race 回归，覆盖 workbook 切换后旧 share create / version restore follow-through 被忽略的场景
- 已补 stale mutation cleanup 第三十三批：
  - `useSheetsSharingVersionWorkbench` 现将 `submitShare()` 的 loading 收尾绑定到原 workbook context；workbook 切换后，旧 share create 完成不再反向清空当前 workbook 的提交状态
  - `useSheetsSharingVersionWorkbench` 现将 `shareMutationId / versionMutationId` 的 finally 清理绑定到当前 mutation id；旧 share/version mutation 完成后不再覆盖新上下文正在进行中的行级 loading
  - 已新增 `frontend/tests/sheets-mutation-state.spec.ts`，覆盖 stale submit / share mutation / version restore cleanup 三类场景，并已纳入默认 `Sheets` 门禁
- 已补 incoming mutation cleanup 第三十四批：
  - `useSheetsSharingVersionWorkbench.respondIncomingShare()` 现将 `incomingMutationId` 的 finally 清理绑定到当前 `shareId`；两个 incoming action 并发时，旧请求完成不再把后一个请求的行级 loading 提前清空
  - `frontend/tests/sheets-mutation-state.spec.ts` 已补回归，覆盖 stale incoming response settle 后仍保留当前 action loading 的场景
- 已补 incoming follow-through guard 第三十五批：
  - `useSheetsSharingVersionWorkbench.respondIncomingShare()` 现为每次 incoming response 分配 mutation token；若期间已有更新 action 启动，旧请求完成后不再继续刷新 incoming/workbooks、打开共享工作簿或触发 success toast
  - `frontend/tests/sheets-mutation-state.spec.ts` 已补 race 回归，覆盖并发 incoming response 下旧 action follow-through 被忽略的场景
- 已补 incoming request guard 第三十六批：
  - `useSheetsSharingVersionWorkbench.refreshIncomingShares()` 现引入 `incomingRequestId` 守卫；多个 incoming refresh 并发时，旧请求返回结果不再覆盖当前 incoming 列表、错误态或 loading 状态
  - `frontend/tests/sheets-mutation-state.spec.ts` 已补 race 回归，覆盖 stale incoming refresh result 被忽略的场景
- 已补 collaboration request guard 第三十七批：
  - `useSheetsWorkbench.refreshCollaboration()` 现引入 `collaborationRequestId` 守卫；多个 collaboration refresh 并发时，旧 success/error 不再覆盖当前事件列表、错误态或 loading 状态
  - 已新增 `frontend/tests/sheets-collaboration-state.spec.ts`，覆盖 stale collaboration refresh success / error 被忽略的场景，并已纳入默认 `Sheets` 门禁
- 已补 visible workbooks guard 第三十八批：
  - `useSheetsSharingVersionWorkbench.refreshVisibleWorkbooks()` 现接受 `canApply` 守卫；并发 incoming action 下，旧 visible workbook refresh 返回后不再写回 stale workbook list 或错误态
  - 已新增 `frontend/tests/sheets-visible-workbooks-state.spec.ts`，覆盖 stale visible workbooks success / error 被忽略的场景，并已纳入默认 `Sheets` 门禁
- 已补 review regression fix 第三十九批：
  - `useSheetsSharingVersionWorkbench.submitShare()` 与 `removeShare()` 现改用统一协作者计数 helper，`DECLINED` share 不再被误计入 `collaboratorCount`
  - `useSheetsWorkspace.onRefreshWorkspace()` 透传 `skipDiscardConfirm` 到 refresh fallback selection；当前 workbook 在 refresh 后切到替代 workbook 时，不再触发第二次 discard confirm
  - 已新增 `frontend/tests/sheets-refresh-regression.spec.ts`，并扩充 `frontend/tests/sheets-sharing-version.spec.ts`，覆盖上述两条 reviewer 回归；`scripts/validate-local.sh` 已将新回归纳入默认 `Sheets` 门禁
- 已补 workbook sidebar filter 第四十批：
  - `SheetsWorkbookSidebar` 已新增本地关键字筛选，按工作簿标题、owner display name、owner email 匹配，帮助工作簿增多后的快速定位
  - 侧栏空态已区分“无工作簿”与“筛选无结果”，避免把真实空库和筛选命中为空混成同一状态
  - 已新增 `frontend/tests/sheets-sidebar.spec.ts`，覆盖筛选、清空与 action 接线；`scripts/validate-local.sh` 已将该回归纳入默认 `Sheets` 门禁

### 范围
- 表格创建、编辑、保存、分页
- 基础导入导出
- 错误态与恢复
- 组织边界与权限回归

### 不做
- 实时协作
- 复杂公式引擎
- 企业级流程编排

### 进入顺序
- 仅在 `Docs`、国际化治理、Community / Hosted 边界完成第一阶段后再进入

## Stream C - 国际化治理
### 目标
- 建立简体中文 / 繁体中文 / 英语的工程化治理。

### 当前批次
- 已新增 `frontend/utils/i18n-governance.ts`，静态扫描 `frontend/locales/*.ts` 并输出 locale catalog 对齐报告。
- 已新增 `frontend/scripts/i18n-report.mjs`，产出：
  - `artifacts/i18n-consistency-report.json`
  - `artifacts/i18n-consistency-report.md`
- 已新增 `frontend/tests/i18n-governance.spec.ts`，与既有 `frontend/tests/i18n.spec.ts` 一起构成当前 i18n 起步回归。
- 已新增 `frontend/utils/i18n-coverage.ts` 与 `frontend/scripts/i18n-coverage-report.mjs`，输出 `frontend/pages` 的页面级国际化覆盖统计：
  - `artifacts/i18n-page-coverage-report.json`
  - `artifacts/i18n-page-coverage-report.md`
- 已新增 `frontend/tests/i18n-coverage.spec.ts`，保证 `Docs / Sheets / Drive / Calendar / Organizations / Settings` 等关键页面继续保留 `useI18n` 接线与静态 key 覆盖。
- 已补齐 Mail compose 这一条页面级国际化链：
  - `frontend/locales/mail-compose.ts` 收口 compose / attachment 面板的文案、错误态与成功提示
  - `frontend/components/business/MailComposer.vue`、`frontend/components/business/MailAttachmentPanel.vue` 已改为使用 `useI18n`
  - `frontend/pages/compose.vue` 已补页标题与异常路径 fallback 文案
  - `frontend/tests/mail-compose-i18n.spec.ts` 已并入默认 i18n 门禁
- 已补齐 Mail detail / Conversations 这一条页面级国际化链：
  - `frontend/locales/mail-workspace.ts` 收口邮件详情页、会话列表页、会话详情页文案
  - `frontend/pages/mail/[id].vue`、`frontend/pages/conversations/index.vue`、`frontend/pages/conversations/[id].vue` 已改为使用 `useI18n`
  - `frontend/tests/mail-workspace-i18n.spec.ts` 已并入默认 i18n 门禁
  - 页面级国际化覆盖率已提升到 `95.9%`，当前剩余未本地化页面主要为 `contacts.vue`、根路由跳转页与少量纯壳层页面
- 已补齐联系人工作区这一条页面级国际化链：
  - 新增 `frontend/locales/contacts-workspace.ts`，收口联系人、分组、导入导出、重复联系人与确认提示文案
  - `frontend/pages/contacts.vue` 已拆分为薄页入口，真实工作区迁移至 `frontend/components/contacts/ContactsWorkspace.vue`
  - 新增 `frontend/composables/useContactsWorkspace.ts`，承接联系人页逻辑，避免继续在超长页面文件上叠加逻辑
  - `frontend/tests/contacts-i18n.spec.ts` 已并入默认 i18n 门禁，`frontend/tests/i18n-coverage.spec.ts` 已将 `/contacts` 纳入关键页面覆盖断言
- 已收口 mailbox 壳层页与根路由跳转页的页面级 i18n 遗留：
  - `frontend/pages/archive.vue`、`drafts.vue`、`inbox.vue`、`outbox.vue`、`scheduled.vue`、`sent.vue`、`snoozed.vue`、`spam.vue`、`trash.vue` 与 `index.vue` 已显式接入 `useI18n + useHead`
  - `frontend/tests/i18n-coverage.spec.ts` 已升级为全页覆盖断言，要求 `frontend/pages` 下全部页面都具备 `useI18n` 与静态 key
  - `artifacts/i18n-page-coverage-report.md` 当前已提升到 `49/49` 页面使用 i18n、覆盖率 `100%`
- 已将 i18n 专项纳入默认门禁：
  - `pnpm --dir frontend exec vitest run tests/i18n.spec.ts tests/i18n-governance.spec.ts tests/i18n-coverage.spec.ts tests/contacts-i18n.spec.ts tests/mail-compose-i18n.spec.ts tests/mail-workspace-i18n.spec.ts`
  - `node --experimental-strip-types frontend/scripts/i18n-report.mjs`
  - `node --experimental-strip-types frontend/scripts/i18n-coverage-report.mjs`
  - `bash scripts/validate-local.sh`
- 已新增治理文档 `docs/open-source/i18n-governance.md`，明确三语 key 一致性规则、报告产物路径与术语表种子。

### 范围
- 文案抽取
- 缺失检查
- 术语表
- CI 校验
- 页面覆盖率统计

## Stream D - Community / Hosted 边界
### 目标
- 继续把 Community 和 Hosted 的支持边界写死。

### 当前批次
- 已新增 `frontend/utils/community-boundary.ts`，把 Community `GA / Beta / Preview` 模块、Hosted-only 能力与自托管责任整理为前端可消费的数据模型。
- 已新增 `frontend/components/suite/SuiteReleaseBoundaryPanel.vue`，在 `/suite` 中直接展示：
  - Community 默认导航与 Labs 的模块边界
  - Hosted-only 能力清单
  - 自托管责任边界
  - README / Support Boundaries / Roadmap / Module Maturity Matrix 的权威文档入口
- 已新增 `frontend/tests/community-boundary.spec.ts`，并将 `tests/community-navigation.spec.ts + tests/community-boundary.spec.ts` 接入 `scripts/validate-local.sh` 默认门禁。
- 已同步 `README.md` 与 `docs/release/community-v1-support-boundaries.md`，明确：
  - `release/v1.0` 继续承载 `v1.0.0 / v1.0.x`
  - `dev/community-v1` 已转入 `v1.1`
  - Community 中的 `Billing center` 只承载报价 / 草稿 / 状态展示，不承诺真实支付闭环
- 已补齐 `Billing center` 的边界 registry 遗漏：
  - `frontend/constants/module-maturity.ts` 已将 `Billing center` 显式登记为 `BETA + SUITE` 子入口，不再在边界面板中被漏掉
  - `SuiteReleaseBoundaryPanel` 与 `community-boundary.spec.ts` 已同步校验 `Suite` 子入口计数与 `Billing center` 可见性
  - `docs/open-source/module-maturity-matrix.md` 与 `docs/release/community-v1-support-boundaries.md` 已回填 `Billing center` 为 `Suite` 内 `BETA` 子入口的真实口径

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

## backlog 入口
- 分诊手册：`docs/release/community-v1-ga-triage-playbook.md`
- backlog seed：`docs/release/community-v1-v1.1-backlog-seed.md`
