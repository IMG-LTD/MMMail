import { computed, onMounted, ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SuiteCollaborationEvent } from '~/types/api'
import { useSheetsApi } from '~/composables/useSheetsApi'
import { useSheetsWorkspace } from '~/composables/useSheetsWorkspace'
import { useSheetsDataTools } from '~/composables/useSheetsDataTools'
import { useSheetsSharingVersionWorkbench } from '~/composables/useSheetsSharingVersionWorkbench'
import { useSuiteApi } from '~/composables/useSuiteApi'
import {
  filterSheetsCollaborationEvents,
  SHEETS_TEMPLATE_PRESETS,
  type SheetsTemplatePreset
} from '~/utils/sheets-collaboration'

export function useSheetsWorkbench() {
  const workspace = useSheetsWorkspace()
  const { t } = useI18n()
  const sheetsApi = useSheetsApi()
  const suiteApi = useSuiteApi()

  const collaborationLoading = ref(false)
  const collaborationEvents = ref<SuiteCollaborationEvent[]>([])
  const collaborationError = ref('')
  const creatingTemplateCode = ref<string | null>(null)
  let collaborationRequestId = 0
  const dataTools = useSheetsDataTools({
    workbooks: workspace.workbooks,
    activeWorkbook: workspace.activeWorkbook,
    activeSheet: workspace.activeSheet,
    activeCell: workspace.activeCell,
    localGrid: workspace.localGrid,
    computedGrid: workspace.computedGrid,
    conflictMessage: workspace.conflictMessage,
    dirtyCount: workspace.dirtyCount
  })
  const sharingVersion = useSheetsSharingVersionWorkbench({
    workbooks: workspace.workbooks,
    activeWorkbook: workspace.activeWorkbook,
    activeWorkbookId: workspace.activeWorkbookId,
    selectWorkbook: workspace.selectWorkbook,
    refreshCollaboration,
    confirmDiscardChangesIfNeeded: workspace.confirmDiscardChangesIfNeeded
  })

  const templatePresets = computed<SheetsTemplatePreset[]>(() => SHEETS_TEMPLATE_PRESETS)
  const workbookEvents = computed(() => {
    return filterSheetsCollaborationEvents(collaborationEvents.value, workspace.activeWorkbookId.value)
  })
  const collaborationEventCount = computed(() => workbookEvents.value.length)

  async function refreshCollaboration(): Promise<void> {
    collaborationRequestId += 1
    const requestId = collaborationRequestId
    collaborationLoading.value = true
    collaborationError.value = ''
    try {
      const center = await suiteApi.getCollaborationCenter(48)
      if (requestId !== collaborationRequestId) {
        return
      }
      collaborationEvents.value = center.items
    } catch (error) {
      if (requestId !== collaborationRequestId) {
        return
      }
      collaborationError.value = (error as Error).message || t('sheets.messages.loadCollaborationFailed')
    } finally {
      if (requestId === collaborationRequestId) {
        collaborationLoading.value = false
      }
    }
  }

  async function refreshCollaborationOnSuccess(action: () => Promise<boolean>): Promise<void> {
    if (!await action()) {
      return
    }
    await refreshCollaboration()
  }

  async function createWorkbookFromTemplate(template: SheetsTemplatePreset, title: string): Promise<boolean> {
    if (!await workspace.confirmDiscardChangesIfNeeded()) {
      return false
    }
    creatingTemplateCode.value = template.code
    try {
      const detail = await sheetsApi.createWorkbook({ title })
      const selected = await workspace.selectWorkbook(detail.id, true, { skipDiscardConfirm: true })
      if (!selected) {
        return false
      }
      await refreshCollaboration()
      return true
    } finally {
      creatingTemplateCode.value = null
    }
  }

  async function onCreateWorkbook(): Promise<void> {
    await refreshCollaborationOnSuccess(() => workspace.onCreateWorkbook())
  }

  async function onImportWorkbook(payload: { file: File; title: string }): Promise<void> {
    await refreshCollaborationOnSuccess(() => workspace.onImportWorkbook(payload))
  }

  async function onExportWorkbook(format: Parameters<typeof workspace.onExportWorkbook>[0]): Promise<void> {
    await workspace.onExportWorkbook(format)
  }

  async function onRenameWorkbook(workbook: Parameters<typeof workspace.onRenameWorkbook>[0]): Promise<void> {
    await refreshCollaborationOnSuccess(() => workspace.onRenameWorkbook(workbook))
  }

  async function onDeleteWorkbook(workbook: Parameters<typeof workspace.onDeleteWorkbook>[0]): Promise<void> {
    await refreshCollaborationOnSuccess(() => workspace.onDeleteWorkbook(workbook))
  }

  async function onCreateSheet(): Promise<void> {
    await refreshCollaborationOnSuccess(() => workspace.onCreateSheet())
  }

  async function onRenameSheet(sheet: Parameters<typeof workspace.onRenameSheet>[0]): Promise<void> {
    await refreshCollaborationOnSuccess(() => workspace.onRenameSheet(sheet))
  }

  async function onDeleteSheet(sheet: Parameters<typeof workspace.onDeleteSheet>[0]): Promise<void> {
    await refreshCollaborationOnSuccess(() => workspace.onDeleteSheet(sheet))
  }

  async function onSelectSheet(sheetId: string): Promise<void> {
    const switched = await workspace.onSelectSheet(sheetId)
    if (switched) {
      await refreshCollaboration()
    }
  }

  async function onSaveWorkbook(): Promise<void> {
    await refreshCollaborationOnSuccess(() => workspace.onSaveWorkbook())
  }

  async function onSortSheet(payload: Parameters<typeof dataTools.onSortSheet>[0]): Promise<void> {
    if (await dataTools.onSortSheet(payload)) {
      await refreshCollaboration()
    }
  }

  async function onFreezeRowsToActiveCell(): Promise<void> {
    if (await dataTools.onFreezeRowsToActiveCell()) {
      await refreshCollaboration()
    }
  }

  async function onFreezeColsToActiveCell(): Promise<void> {
    if (await dataTools.onFreezeColsToActiveCell()) {
      await refreshCollaboration()
    }
  }

  async function onClearFreeze(): Promise<void> {
    if (await dataTools.onClearFreeze()) {
      await refreshCollaboration()
    }
  }

  async function onRefreshWorkspace(): Promise<void> {
    const refreshed = await workspace.onRefreshWorkspace()
    if (!refreshed) {
      return
    }
    if (!await sharingVersion.refreshIncomingShares()) {
      return
    }
    if (!await sharingVersion.refreshShares()) {
      return
    }
    await refreshCollaboration()
  }

  onMounted(() => {
    void refreshCollaboration()
  })

  return {
    ...workspace,
    ...dataTools,
    ...sharingVersion,
    collaborationLoading,
    collaborationError,
    workbookEvents,
    collaborationEventCount,
    creatingTemplateCode,
    templatePresets,
    createWorkbookFromTemplate,
    onCreateWorkbook,
    onImportWorkbook,
    onExportWorkbook,
    onRenameWorkbook,
    onDeleteWorkbook,
    onCreateSheet,
    onRenameSheet,
    onDeleteSheet,
    onSelectSheet,
    onSaveWorkbook,
    onSortSheet,
    onFreezeRowsToActiveCell,
    onFreezeColsToActiveCell,
    onClearFreeze,
    onRefreshWorkspace,
    refreshCollaboration
  }
}
