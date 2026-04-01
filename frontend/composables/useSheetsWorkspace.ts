import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
  buildExportMimeType,
  buildFormulaHint,
  cellLabel,
  collectCellEdits,
  countFilledCells,
  countFormulaCells,
  downloadTextFile,
  resolveCellPresentation,
  resolveImportFormat,
  SUPPORTED_SHEETS_EXPORT_FORMATS,
  SUPPORTED_SHEETS_IMPORT_FORMATS
} from '~/utils/sheets'
import { extractSheetsWorkbookIdFromRouteQuery, resolvePreferredSheetsWorkbookId } from '~/utils/sheets-workspace-route'
import {
  buildSheetsWorkbookGrid,
  findSheetsWorkbookSheetById,
  mergeSheetsDirtyEdit,
} from '~/utils/sheets-workspace-state'
import {
  applySheetsWorkbookDetailState,
  clearSheetsWorkspaceSelection,
  confirmSheetsWorkspaceDiscard,
  handleDeletedSheetsWorkbook,
  isSheetsConflictError,
  refreshSheetsWorkbookList,
  syncSheetsWorkspaceRoute,
  type SheetsWorkbookSelectionOptions
} from '~/utils/sheets-workspace-runtime'
import { registerSheetsWorkspaceBeforeUnload } from '~/utils/sheets-workspace-leave-guard'
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
  const activeSheet = computed(() => findSheetsWorkbookSheetById(activeWorkbook.value, activeWorkbook.value?.activeSheetId))
  const activeSheetName = computed(() => activeSheet.value?.name ?? '')
  const dirtyCount = computed(() => dirtyEdits.value.size)
  const filledCellCount = computed(() => countFilledCells(localGrid.value))
  const localFormulaCellCount = computed(() => countFormulaCells(localGrid.value))
  const workbookCount = computed(() => workbooks.value.length)
  const savedGrid = computed(() => buildSheetsWorkbookGrid(activeWorkbook.value, activeWorkbook.value?.grid))
  const computedGrid = computed(() => buildSheetsWorkbookGrid(activeWorkbook.value, activeWorkbook.value?.computedGrid))
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
  const removeBeforeUnloadGuard = registerSheetsWorkspaceBeforeUnload({ dirtyEdits })
  onMounted(() => {
    void refreshWorkbookList(resolvePreferredWorkbookId())
  })
  onBeforeRouteLeave(() => confirmDiscardChangesIfNeeded())
  onBeforeUnmount(() => {
    removeBeforeUnloadGuard()
  })
  watch(
    () => route.query.workbookId,
    async (value) => {
      const nextId = extractSheetsWorkbookIdFromRouteQuery(value)
      if (!nextId || nextId === activeWorkbookId.value) {
        return
      }
      const selected = await selectWorkbook(nextId, false)
      if (!selected) {
        await syncRoute(activeWorkbookId.value)
      }
    }
  )
  async function refreshWorkbookList(
    preferredWorkbookId: string | null,
    selectionOptions?: SheetsWorkbookSelectionOptions
  ): Promise<boolean> {
    return refreshSheetsWorkbookList({
      preferredWorkbookId,
      listWorkbooks: api.listWorkbooks,
      workbooks,
      activeWorkbook,
      localGrid,
      loadingList,
      clearSelection,
      syncRoute,
      selectWorkbook,
      selectionOptions,
      onLoadError: (error) => ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.loadWorkbooksFailed'))
    })
  }
  async function selectWorkbook(
    workbookId: string,
    syncRouteAfterLoad: boolean,
    options: SheetsWorkbookSelectionOptions = {}
  ): Promise<boolean> {
    if (!options.skipDiscardConfirm && dirtyEdits.value.size > 0 && !await confirmDiscardChangesIfNeeded()) {
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
  async function onCreateWorkbook(): Promise<boolean> {
    const title = await dialogs.requestWorkbookTitle('create', dialogs.buildDefaultWorkbookTitle())
    if (!title || !await confirmDiscardChangesIfNeeded()) {
      return false
    }
    creating.value = true
    try {
      const detail = await api.createWorkbook({ title })
      applyWorkbookDetail(detail)
      await syncRoute(detail.id)
      ElMessage.success(t('sheets.messages.workbookCreated'))
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.createWorkbookFailed'))
      return false
    } finally {
      creating.value = false
    }
  }
  async function onImportWorkbook(payload: { file: File; title: string }): Promise<boolean> {
    if (!await confirmDiscardChangesIfNeeded()) {
      return false
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
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.importWorkbookFailed'))
      return false
    } finally {
      importing.value = false
    }
  }
  async function onExportWorkbook(format: SheetsExportFormat): Promise<boolean> {
    if (!activeWorkbook.value) {
      return false
    }
    exporting.value = true
    try {
      const snapshot = await api.exportWorkbook(activeWorkbook.value.id, format)
      lastExport.value = snapshot
      downloadTextFile(snapshot.content, snapshot.fileName, buildExportMimeType(snapshot.format))
      ElMessage.success(t('sheets.messages.workbookExported', { value: snapshot.fileName }))
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.exportWorkbookFailed'))
      return false
    } finally {
      exporting.value = false
    }
  }
  async function onRenameWorkbook(workbook: SheetsWorkbookSummary): Promise<boolean> {
    if (workbook.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return false
    }
    const title = await dialogs.requestWorkbookTitle('rename', workbook.title)
    if (!title) {
      return false
    }
    busyWorkbookId.value = workbook.id
    try {
      const detail = await api.renameWorkbook(workbook.id, { title })
      applyWorkbookDetail(detail, true)
      ElMessage.success(t('sheets.messages.workbookRenamed'))
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.renameWorkbookFailed'))
      return false
    } finally {
      busyWorkbookId.value = null
    }
  }
  async function onDeleteWorkbook(workbook: SheetsWorkbookSummary): Promise<boolean> {
    if (workbook.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return false
    }
    if (!await dialogs.confirmDeleteWorkbook(workbook.title)) {
      return false
    }
    if (workbook.id === activeWorkbookId.value && !await confirmDiscardChangesIfNeeded()) {
      return false
    }
    busyWorkbookId.value = workbook.id
    try {
      await api.deleteWorkbook(workbook.id)
      const remaining = workbooks.value.filter((item) => item.id !== workbook.id)
      workbooks.value = remaining
      await handleWorkbookDeletion(workbook.id, remaining[0]?.id ?? null, true)
      ElMessage.success(t('sheets.messages.workbookDeleted'))
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.deleteWorkbookFailed'))
      return false
    } finally {
      busyWorkbookId.value = null
    }
  }
  async function onCreateSheet(): Promise<boolean> {
    if (!activeWorkbook.value) {
      return false
    }
    if (activeWorkbook.value.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return false
    }
    if (!await confirmDiscardChangesIfNeeded()) {
      return false
    }
    sheetBusy.value = true
    try {
      const detail = await api.createSheet(activeWorkbook.value.id)
      applyWorkbookDetail(detail)
      ElMessage.success(t('sheets.messages.sheetCreated', { value: activeSheetName.value }))
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.createSheetFailed'))
      return false
    } finally {
      sheetBusy.value = false
    }
  }
  async function onRenameSheet(sheet: SheetsWorkbookSheet): Promise<boolean> {
    if (!activeWorkbook.value) {
      return false
    }
    if (activeWorkbook.value.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return false
    }
    const name = await dialogs.requestSheetName(sheet.name)
    if (!name) {
      return false
    }
    sheetBusy.value = true
    try {
      const detail = await api.renameSheet(activeWorkbook.value.id, sheet.id, { name })
      applyWorkbookDetail(detail, true)
      ElMessage.success(t('sheets.messages.sheetRenamed', { value: name }))
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.renameSheetFailed'))
      return false
    } finally {
      sheetBusy.value = false
    }
  }
  async function onDeleteSheet(sheet: SheetsWorkbookSheet): Promise<boolean> {
    if (!activeWorkbook.value) {
      return false
    }
    if (activeWorkbook.value.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return false
    }
    if (!await dialogs.confirmDeleteSheet(sheet.name)) {
      return false
    }
    if (!await confirmDiscardChangesIfNeeded()) {
      return false
    }
    sheetBusy.value = true
    try {
      const detail = await api.deleteSheet(activeWorkbook.value.id, sheet.id)
      applyWorkbookDetail(detail)
      ElMessage.success(t('sheets.messages.sheetDeleted', { value: sheet.name }))
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.deleteSheetFailed'))
      return false
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
    dirtyEdits.value = mergeSheetsDirtyEdit(dirtyEdits.value, activeWorkbook.value, payload)
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
  async function onSaveWorkbook(): Promise<boolean> {
    if (!activeWorkbook.value || dirtyEdits.value.size === 0) {
      return false
    }
    if (!activeWorkbook.value.canEdit) {
      ElMessage.warning(t('sheets.messages.readOnlyWorkbook'))
      return false
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
      return true
    } catch (error) {
      if (isSheetsConflictError(error)) {
        conflictMessage.value = t('sheets.messages.versionConflict')
        ElMessage.error(conflictMessage.value)
      } else {
        ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.saveWorkbookFailed'))
      }
      return false
    } finally {
      saving.value = false
    }
  }
  async function onRefreshWorkspace(): Promise<boolean> {
    if (!await confirmDiscardChangesIfNeeded()) {
      return false
    }
    refreshing.value = true
    const previousActiveId = activeWorkbookId.value
    try {
      const refreshed = await refreshWorkbookList(resolvePreferredWorkbookId(), { skipDiscardConfirm: true })
      if (!refreshed) {
        return false
      }
      if (previousActiveId && activeWorkbookId.value === previousActiveId) {
        const reloaded = await selectWorkbook(previousActiveId, false, { skipDiscardConfirm: true })
        if (!reloaded) {
          return false
        }
      }
      conflictMessage.value = ''
      ElMessage.success(t('sheets.messages.workspaceRefreshed'))
      return true
    } catch (error) {
      ElMessage.error(dialogs.resolveErrorMessage(error, 'sheets.messages.refreshWorkspaceFailed'))
      return false
    } finally {
      refreshing.value = false
    }
  }
  function applyWorkbookDetail(detail: SheetsWorkbookDetail, preserveLocalState = false): void {
    applySheetsWorkbookDetailState({
      activeWorkbook,
      activeCell,
      localGrid,
      dirtyEdits,
      lastExport,
      workbooks
    }, detail, preserveLocalState)
  }
  function clearSelection(): void {
    clearSheetsWorkspaceSelection({
      activeWorkbook,
      activeCell,
      localGrid,
      dirtyEdits,
      conflictMessage,
      lastExport
    })
  }
  async function handleWorkbookDeletion(
    currentWorkbookId: string,
    nextWorkbookId: string | null,
    skipDiscardConfirm = false
  ): Promise<void> {
    await handleDeletedSheetsWorkbook({
      activeWorkbookId: activeWorkbookId.value,
      currentWorkbookId,
      nextWorkbookId,
      selectWorkbook,
      clearSelection,
      syncRoute,
      skipDiscardConfirm
    })
  }
  async function confirmDiscardChangesIfNeeded(): Promise<boolean> {
    return confirmSheetsWorkspaceDiscard({
      dirtyEdits,
      activeSheetName,
      fallbackTitle: t('sheets.tabs.title'),
      confirmDiscardChanges: dialogs.confirmDiscardChanges
    })
  }
  function resolvePreferredWorkbookId(): string | null {
    return resolvePreferredSheetsWorkbookId(route.query, activeWorkbookId.value)
  }
  async function syncRoute(workbookId: string | null): Promise<void> {
    await syncSheetsWorkspaceRoute({
      query: route.query,
      workbookId,
      replace: (location) => router.replace(location)
    })
  }
  return {
    workbooks, activeWorkbook, activeSheet, activeCell, localGrid,
    loadingList, loadingDetail, creating, importing, exporting,
    refreshing, saving, sheetBusy, busyWorkbookId, conflictMessage,
    dirtyCount, workbookCount, savedGrid, computedGrid,
    supportedImportFormats, supportedExportFormats, activeWorkbookId,
    activeWorkbookForHero, activeCellLabel, activeCellPresentation,
    formulaPreviewHint, localFormulaCellCount, lastImported, lastExport,
    selectWorkbook, onCreateWorkbook, onImportWorkbook, onExportWorkbook,
    onRenameWorkbook, onDeleteWorkbook, onCreateSheet, onRenameSheet,
    onDeleteSheet, onSelectSheet, onCellSelect, onCellChange,
    onFormulaChange, onSaveWorkbook, onRefreshWorkspace,
    confirmDiscardChangesIfNeeded
  }
}
