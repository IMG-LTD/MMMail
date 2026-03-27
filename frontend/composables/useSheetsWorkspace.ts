import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useSheetsApi } from '~/composables/useSheetsApi'
import { useSheetsWorkspaceDialogs } from '~/composables/useSheetsWorkspaceDialogs'
import type { ApiClientError } from '~/utils/request'
import type {
  ActiveSheetsCell,
  SheetCellEditInput,
  SheetsExportFormat,
  SheetsImportSummary,
  SheetsWorkbookDetail,
  SheetsWorkbookExport,
  SheetsWorkbookSheet,
  SheetsWorkbookSummary
} from '~/types/sheets'
import {
  applyCellValue,
  buildCellEditKey,
  buildExportMimeType,
  buildFormulaHint,
  cellLabel,
  collectCellEdits,
  countFilledCells,
  countFormulaCells,
  downloadTextFile,
  normalizeSheetsGrid,
  resolveCellPresentation,
  resolveImportFormat,
  sortSheetsWorkbooks,
  SUPPORTED_SHEETS_EXPORT_FORMATS,
  SUPPORTED_SHEETS_IMPORT_FORMATS
} from '~/utils/sheets'

export function useSheetsWorkspace() {
  const route = useRoute()
  const router = useRouter()
  const api = useSheetsApi()
  const { t } = useI18n()
  const dialogs = useSheetsWorkspaceDialogs()

  const workbooks = ref<SheetsWorkbookSummary[]>([])
  const activeWorkbook = ref<SheetsWorkbookDetail | null>(null)
  const activeCell = ref<ActiveSheetsCell | null>(null)
  const localGrid = ref<string[][]>([])
  const loadingList = ref(false)
  const loadingDetail = ref(false)
  const creating = ref(false)
  const importing = ref(false)
  const exporting = ref(false)
  const refreshing = ref(false)
  const saving = ref(false)
  const sheetBusy = ref(false)
  const busyWorkbookId = ref<string | null>(null)
  const conflictMessage = ref('')
  const dirtyEdits = ref<Map<string, SheetCellEditInput>>(new Map())
  const lastImported = ref<SheetsImportSummary | null>(null)
  const lastExport = ref<SheetsWorkbookExport | null>(null)

  const activeWorkbookId = computed(() => activeWorkbook.value?.id ?? null)
  const activeSheet = computed(() => findSheetById(activeWorkbook.value, activeWorkbook.value?.activeSheetId))
  const activeSheetName = computed(() => activeSheet.value?.name ?? '')
  const dirtyCount = computed(() => dirtyEdits.value.size)
  const filledCellCount = computed(() => countFilledCells(localGrid.value))
  const localFormulaCellCount = computed(() => countFormulaCells(localGrid.value))
  const workbookCount = computed(() => workbooks.value.length)
  const savedGrid = computed(() => buildWorkbookGrid(activeWorkbook.value?.grid))
  const computedGrid = computed(() => buildWorkbookGrid(activeWorkbook.value?.computedGrid))
  const supportedImportFormats = computed(() => activeWorkbook.value?.supportedImportFormats || SUPPORTED_SHEETS_IMPORT_FORMATS)
  const supportedExportFormats = computed(() => activeWorkbook.value?.supportedExportFormats || SUPPORTED_SHEETS_EXPORT_FORMATS)
  const activeCellLabel = computed(() => activeCell.value ? cellLabel(activeCell.value.rowIndex, activeCell.value.colIndex) : '—')
  const activeCellPresentation = computed(() => {
    if (!activeWorkbook.value || !activeCell.value) {
      return null
    }
    return resolveCellPresentation({
      rawGrid: localGrid.value,
      savedGrid: savedGrid.value,
      computedGrid: computedGrid.value,
      rowIndex: activeCell.value.rowIndex,
      colIndex: activeCell.value.colIndex
    })
  })
  const activeWorkbookForHero = computed(() => {
    if (!activeWorkbook.value) {
      return null
    }
    return {
      ...activeWorkbook.value,
      filledCellCount: filledCellCount.value,
      formulaCellCount: localFormulaCellCount.value
    }
  })
  const formulaPreviewHint = computed(() => buildFormulaHint(activeCellPresentation.value, {
    emptySelection: t('sheets.formula.hints.emptySelection'),
    emptyCell: t('sheets.formula.hints.emptyCell'),
    dirtyFormula: t('sheets.formula.hints.dirtyFormula'),
    error: t('sheets.formula.hints.error'),
    formula: t('sheets.formula.hints.formula'),
    literal: t('sheets.formula.hints.literal')
  }))

  onMounted(() => {
    void refreshWorkbookList(resolvePreferredWorkbookId())
  })

  watch(
    () => route.query.workbookId,
    async (value) => {
      const nextId = typeof value === 'string' ? value : null
      if (!nextId || nextId === activeWorkbookId.value) {
        return
      }
      const selected = await selectWorkbook(nextId, false)
      if (!selected) {
        await syncRoute(activeWorkbookId.value)
      }
    }
  )

  async function refreshWorkbookList(preferredWorkbookId: string | null): Promise<void> {
    loadingList.value = true
    try {
      const next = sortSheetsWorkbooks(await api.listWorkbooks(100))
      workbooks.value = next
      if (!next.length) {
        clearSelection()
        await syncRoute(null)
        return
      }
      const targetId = preferredWorkbookId && next.some((item) => item.id === preferredWorkbookId)
        ? preferredWorkbookId
        : next[0].id
      if (!activeWorkbook.value || activeWorkbook.value.id !== targetId) {
        await selectWorkbook(targetId, true)
        return
      }
      mergeWorkbookSummary(toWorkbookSummary(activeWorkbook.value))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.loadWorkbooksFailed'))
    } finally {
      loadingList.value = false
    }
  }

  async function selectWorkbook(workbookId: string, syncRouteAfterLoad: boolean): Promise<boolean> {
    if (activeWorkbookId.value !== workbookId && !await confirmDiscardChangesIfNeeded()) {
      return false
    }
    loadingDetail.value = true
    try {
      const detail = await api.getWorkbook(workbookId)
      applyWorkbookDetail(detail)
      conflictMessage.value = ''
      if (syncRouteAfterLoad) {
        await syncRoute(detail.id)
      }
      return true
    } catch (error) {
      const normalized = error as ApiClientError
      if (normalized.status === 404) {
        ElMessage.warning(t('sheets.messages.workbookMissing'))
        await refreshWorkbookList(null)
        return false
      }
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.openWorkbookFailed'))
      return false
    } finally {
      loadingDetail.value = false
    }
  }

  async function onCreateWorkbook(): Promise<void> {
    const title = await dialogs.requestWorkbookTitle('create', dialogs.buildDefaultWorkbookTitle())
    if (!title || !await confirmDiscardChangesIfNeeded()) {
      return
    }
    creating.value = true
    try {
      const detail = await api.createWorkbook({ title })
      applyWorkbookDetail(detail)
      await syncRoute(detail.id)
      ElMessage.success(t('sheets.messages.workbookCreated'))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.createWorkbookFailed'))
    } finally {
      creating.value = false
    }
  }

  async function onImportWorkbook(payload: { file: File; title: string }): Promise<void> {
    if (!await confirmDiscardChangesIfNeeded()) {
      return
    }
    importing.value = true
    try {
      const detail = await api.importWorkbook(payload)
      lastImported.value = {
        title: detail.title,
        rowCount: detail.rowCount,
        colCount: detail.colCount,
        formulaCellCount: detail.formulaCellCount,
        sourceFormat: resolveImportFormat(payload.file.name),
        importedAt: new Date().toISOString()
      }
      applyWorkbookDetail(detail)
      await syncRoute(detail.id)
      conflictMessage.value = ''
      ElMessage.success(t('sheets.messages.workbookImported', { value: payload.file.name }))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.importWorkbookFailed'))
    } finally {
      importing.value = false
    }
  }

  async function onExportWorkbook(format: SheetsExportFormat): Promise<void> {
    if (!activeWorkbook.value) {
      return
    }
    exporting.value = true
    try {
      const snapshot = await api.exportWorkbook(activeWorkbook.value.id, format)
      lastExport.value = snapshot
      downloadTextFile(snapshot.content, snapshot.fileName, buildExportMimeType(snapshot.format))
      ElMessage.success(t('sheets.messages.workbookExported', { value: snapshot.fileName }))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.exportWorkbookFailed'))
    } finally {
      exporting.value = false
    }
  }

  async function onRenameWorkbook(workbook: SheetsWorkbookSummary): Promise<void> {
    if (workbook.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return
    }
    const title = await dialogs.requestWorkbookTitle('rename', workbook.title)
    if (!title) {
      return
    }
    busyWorkbookId.value = workbook.id
    try {
      const detail = await api.renameWorkbook(workbook.id, { title })
      applyWorkbookDetail(detail)
      ElMessage.success(t('sheets.messages.workbookRenamed'))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.renameWorkbookFailed'))
    } finally {
      busyWorkbookId.value = null
    }
  }

  async function onDeleteWorkbook(workbook: SheetsWorkbookSummary): Promise<void> {
    if (workbook.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return
    }
    if (!await dialogs.confirmDeleteWorkbook(workbook.title)) {
      return
    }
    busyWorkbookId.value = workbook.id
    try {
      await api.deleteWorkbook(workbook.id)
      const remaining = workbooks.value.filter((item) => item.id !== workbook.id)
      workbooks.value = remaining
      await handleWorkbookDeletion(workbook.id, remaining[0]?.id ?? null)
      ElMessage.success(t('sheets.messages.workbookDeleted'))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.deleteWorkbookFailed'))
    } finally {
      busyWorkbookId.value = null
    }
  }

  async function onCreateSheet(): Promise<void> {
    if (!activeWorkbook.value) {
      return
    }
    if (activeWorkbook.value.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return
    }
    if (!await confirmDiscardChangesIfNeeded()) {
      return
    }
    sheetBusy.value = true
    try {
      const detail = await api.createSheet(activeWorkbook.value.id)
      applyWorkbookDetail(detail)
      ElMessage.success(t('sheets.messages.sheetCreated', { value: activeSheetName.value }))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.createSheetFailed'))
    } finally {
      sheetBusy.value = false
    }
  }

  async function onRenameSheet(sheet: SheetsWorkbookSheet): Promise<void> {
    if (!activeWorkbook.value) {
      return
    }
    if (activeWorkbook.value.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return
    }
    const name = await dialogs.requestSheetName(sheet.name)
    if (!name) {
      return
    }
    sheetBusy.value = true
    try {
      const detail = await api.renameSheet(activeWorkbook.value.id, sheet.id, { name })
      applyWorkbookDetail(detail)
      ElMessage.success(t('sheets.messages.sheetRenamed', { value: name }))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.renameSheetFailed'))
    } finally {
      sheetBusy.value = false
    }
  }

  async function onDeleteSheet(sheet: SheetsWorkbookSheet): Promise<void> {
    if (!activeWorkbook.value) {
      return
    }
    if (activeWorkbook.value.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return
    }
    if (!await dialogs.confirmDeleteSheet(sheet.name)) {
      return
    }
    sheetBusy.value = true
    try {
      const detail = await api.deleteSheet(activeWorkbook.value.id, sheet.id)
      applyWorkbookDetail(detail)
      ElMessage.success(t('sheets.messages.sheetDeleted', { value: sheet.name }))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.deleteSheetFailed'))
    } finally {
      sheetBusy.value = false
    }
  }

  async function onSelectSheet(sheetId: string): Promise<boolean> {
    if (!activeWorkbook.value || activeWorkbook.value.activeSheetId === sheetId) {
      return false
    }
    if (!await confirmDiscardChangesIfNeeded()) {
      return false
    }
    sheetBusy.value = true
    try {
      const detail = await api.setActiveSheet(activeWorkbook.value.id, { sheetId })
      applyWorkbookDetail(detail)
      conflictMessage.value = ''
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.switchSheetFailed'))
      return false
    } finally {
      sheetBusy.value = false
    }
  }

  function onCellSelect(payload: ActiveSheetsCell): void {
    activeCell.value = payload
  }

  function onCellChange(payload: { rowIndex: number; colIndex: number; value: string }): void {
    if (!activeWorkbook.value || !activeWorkbook.value.canEdit) {
      return
    }
    localGrid.value = applyCellValue(localGrid.value, payload.rowIndex, payload.colIndex, payload.value)
    dirtyEdits.value = mergeDirtyEdit(payload)
  }

  function onFormulaChange(value: string): void {
    if (!activeCell.value) {
      return
    }
    onCellChange({
      rowIndex: activeCell.value.rowIndex,
      colIndex: activeCell.value.colIndex,
      value
    })
  }

  async function onSaveWorkbook(): Promise<void> {
    if (!activeWorkbook.value || dirtyEdits.value.size === 0) {
      return
    }
    if (!activeWorkbook.value.canEdit) {
      ElMessage.warning(t('sheets.messages.readOnlyWorkbook'))
      return
    }
    saving.value = true
    try {
      const detail = await api.updateWorkbookCells(activeWorkbook.value.id, {
        currentVersion: activeWorkbook.value.currentVersion,
        sheetId: activeWorkbook.value.activeSheetId,
        edits: collectCellEdits(dirtyEdits.value)
      })
      applyWorkbookDetail(detail)
      conflictMessage.value = ''
      ElMessage.success(t('sheets.messages.workbookSaved'))
    } catch (error) {
      if (isConflict(error)) {
        conflictMessage.value = t('sheets.messages.versionConflict')
        ElMessage.error(conflictMessage.value)
      } else {
        ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.saveWorkbookFailed'))
      }
    } finally {
      saving.value = false
    }
  }

  async function onRefreshWorkspace(): Promise<void> {
    refreshing.value = true
    try {
      await refreshWorkbookList(resolvePreferredWorkbookId())
      conflictMessage.value = ''
      ElMessage.success(t('sheets.messages.workspaceRefreshed'))
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.refreshWorkspaceFailed'))
    } finally {
      refreshing.value = false
    }
  }

  function applyWorkbookDetail(detail: SheetsWorkbookDetail): void {
    const previousWorkbookId = activeWorkbook.value?.id ?? null
    const previousSheetId = activeWorkbook.value?.activeSheetId ?? null
    activeWorkbook.value = detail
    localGrid.value = normalizeSheetsGrid(detail.grid, detail.rowCount, detail.colCount)
    dirtyEdits.value = new Map()
    if (previousWorkbookId !== detail.id || previousSheetId !== detail.activeSheetId) {
      lastExport.value = null
    }
    ensureActiveCell(detail.rowCount, detail.colCount)
    mergeWorkbookSummary(toWorkbookSummary(detail))
  }

  function mergeWorkbookSummary(summary: SheetsWorkbookSummary): void {
    const index = workbooks.value.findIndex((item) => item.id === summary.id)
    const next = [...workbooks.value]
    if (index >= 0) {
      next.splice(index, 1, summary)
    } else {
      next.unshift(summary)
    }
    workbooks.value = sortSheetsWorkbooks(next)
  }

  function toWorkbookSummary(detail: SheetsWorkbookDetail): SheetsWorkbookSummary {
    const normalizedGrid = normalizeSheetsGrid(localGrid.value, detail.rowCount, detail.colCount)
    return {
      id: detail.id,
      title: detail.title,
      rowCount: detail.rowCount,
      colCount: detail.colCount,
      filledCellCount: countFilledCells(normalizedGrid),
      formulaCellCount: countFormulaCells(normalizedGrid),
      computedErrorCount: detail.computedErrorCount,
      currentVersion: detail.currentVersion,
      sheetCount: detail.sheetCount,
      activeSheetId: detail.activeSheetId,
      updatedAt: detail.updatedAt,
      lastOpenedAt: detail.lastOpenedAt,
      permission: detail.permission,
      scope: detail.scope,
      ownerEmail: detail.ownerEmail,
      ownerDisplayName: detail.ownerDisplayName,
      collaboratorCount: detail.collaboratorCount,
      canEdit: detail.canEdit
    }
  }

  function ensureActiveCell(rowCount: number, colCount: number): void {
    if (rowCount <= 0 || colCount <= 0) {
      activeCell.value = null
      return
    }
    if (!activeCell.value) {
      activeCell.value = { rowIndex: 0, colIndex: 0 }
      return
    }
    activeCell.value = {
      rowIndex: Math.min(activeCell.value.rowIndex, rowCount - 1),
      colIndex: Math.min(activeCell.value.colIndex, colCount - 1)
    }
  }

  function clearSelection(): void {
    activeWorkbook.value = null
    activeCell.value = null
    localGrid.value = []
    dirtyEdits.value = new Map()
    conflictMessage.value = ''
    lastExport.value = null
  }

  function buildWorkbookGrid(grid: string[][] | undefined): string[][] {
    if (!activeWorkbook.value || !grid) {
      return []
    }
    return normalizeSheetsGrid(grid, activeWorkbook.value.rowCount, activeWorkbook.value.colCount)
  }

  function mergeDirtyEdit(payload: { rowIndex: number; colIndex: number; value: string }): Map<string, SheetCellEditInput> {
    const next = new Map(dirtyEdits.value)
    const key = buildCellEditKey(payload.rowIndex, payload.colIndex)
    const originalValue = activeWorkbook.value?.grid[payload.rowIndex]?.[payload.colIndex] ?? ''
    if (payload.value === originalValue) {
      next.delete(key)
    } else {
      next.set(key, payload)
    }
    return next
  }

  async function handleWorkbookDeletion(currentWorkbookId: string, nextWorkbookId: string | null): Promise<void> {
    if (activeWorkbookId.value !== currentWorkbookId) {
      return
    }
    if (nextWorkbookId) {
      await selectWorkbook(nextWorkbookId, true)
      return
    }
    clearSelection()
    await syncRoute(null)
  }

  async function confirmDiscardChangesIfNeeded(): Promise<boolean> {
    if (dirtyEdits.value.size === 0) {
      return true
    }
    return dialogs.confirmDiscardChanges(activeSheetName.value || t('sheets.tabs.title'))
  }

  function resolvePreferredWorkbookId(): string | null {
    const routeWorkbookId = typeof route.query.workbookId === 'string' ? route.query.workbookId : null
    return routeWorkbookId || activeWorkbookId.value
  }

  async function syncRoute(workbookId: string | null): Promise<void> {
    const currentQueryId = typeof route.query.workbookId === 'string' ? route.query.workbookId : null
    if (currentQueryId === workbookId) {
      return
    }
    await router.replace({
      path: '/sheets',
      query: workbookId ? { workbookId } : {}
    })
  }

  function isConflict(error: unknown): boolean {
    const normalized = error as ApiClientError
    return normalized.code === 30032 || normalized.status === 409
  }

  return {
    workbooks,
    activeWorkbook,
    activeSheet,
    activeCell,
    localGrid,
    loadingList,
    loadingDetail,
    creating,
    importing,
    exporting,
    refreshing,
    saving,
    sheetBusy,
    busyWorkbookId,
    conflictMessage,
    dirtyCount,
    workbookCount,
    savedGrid,
    computedGrid,
    supportedImportFormats,
    supportedExportFormats,
    activeWorkbookId,
    activeWorkbookForHero,
    activeCellLabel,
    activeCellPresentation,
    formulaPreviewHint,
    localFormulaCellCount,
    lastImported,
    lastExport,
    selectWorkbook,
    onCreateWorkbook,
    onImportWorkbook,
    onExportWorkbook,
    onRenameWorkbook,
    onDeleteWorkbook,
    onCreateSheet,
    onRenameSheet,
    onDeleteSheet,
    onSelectSheet,
    onCellSelect,
    onCellChange,
    onFormulaChange,
    onSaveWorkbook,
    onRefreshWorkspace
  }
}

function findSheetById(workbook: SheetsWorkbookDetail | null, sheetId?: string | null): SheetsWorkbookSheet | null {
  if (!workbook || !sheetId) {
    return null
  }
  return workbook.sheets.find((sheet) => sheet.id === sheetId) || null
}
