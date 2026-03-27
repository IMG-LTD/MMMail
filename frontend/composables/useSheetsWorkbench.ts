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
    refreshCollaboration
  })

  const templatePresets = computed<SheetsTemplatePreset[]>(() => SHEETS_TEMPLATE_PRESETS)
  const workbookEvents = computed(() => {
    return filterSheetsCollaborationEvents(collaborationEvents.value, workspace.activeWorkbookId.value)
  })
  const collaborationEventCount = computed(() => workbookEvents.value.length)

  async function refreshCollaboration(): Promise<void> {
    collaborationLoading.value = true
    collaborationError.value = ''
    try {
      const center = await suiteApi.getCollaborationCenter(48)
      collaborationEvents.value = center.items
    } catch (error) {
      collaborationError.value = (error as Error).message || t('sheets.messages.loadCollaborationFailed')
    } finally {
      collaborationLoading.value = false
    }
  }

  async function createWorkbookFromTemplate(template: SheetsTemplatePreset, title: string): Promise<void> {
    creatingTemplateCode.value = template.code
    try {
      const detail = await sheetsApi.createWorkbook({ title })
      await workspace.selectWorkbook(detail.id, true)
      await refreshCollaboration()
    } finally {
      creatingTemplateCode.value = null
    }
  }

  async function onCreateWorkbook(): Promise<void> {
    await workspace.onCreateWorkbook()
    await refreshCollaboration()
  }

  async function onImportWorkbook(payload: { file: File; title: string }): Promise<void> {
    await workspace.onImportWorkbook(payload)
    await refreshCollaboration()
  }

  async function onExportWorkbook(format: Parameters<typeof workspace.onExportWorkbook>[0]): Promise<void> {
    await workspace.onExportWorkbook(format)
    await refreshCollaboration()
  }

  async function onRenameWorkbook(workbook: Parameters<typeof workspace.onRenameWorkbook>[0]): Promise<void> {
    await workspace.onRenameWorkbook(workbook)
    await refreshCollaboration()
  }

  async function onDeleteWorkbook(workbook: Parameters<typeof workspace.onDeleteWorkbook>[0]): Promise<void> {
    await workspace.onDeleteWorkbook(workbook)
    await refreshCollaboration()
  }

  async function onCreateSheet(): Promise<void> {
    await workspace.onCreateSheet()
    await refreshCollaboration()
  }

  async function onRenameSheet(sheet: Parameters<typeof workspace.onRenameSheet>[0]): Promise<void> {
    await workspace.onRenameSheet(sheet)
    await refreshCollaboration()
  }

  async function onDeleteSheet(sheet: Parameters<typeof workspace.onDeleteSheet>[0]): Promise<void> {
    await workspace.onDeleteSheet(sheet)
    await refreshCollaboration()
  }

  async function onSelectSheet(sheetId: string): Promise<void> {
    const switched = await workspace.onSelectSheet(sheetId)
    if (switched) {
      await refreshCollaboration()
    }
  }

  async function onSaveWorkbook(): Promise<void> {
    await workspace.onSaveWorkbook()
    await refreshCollaboration()
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
    await workspace.onRefreshWorkspace()
    await sharingVersion.refreshIncomingShares()
    await sharingVersion.refreshShares()
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
