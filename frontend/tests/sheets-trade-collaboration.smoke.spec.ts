import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsCollaborationRail from '../components/sheets/SheetsCollaborationRail.vue'
import SheetsImportExportPanel from '../components/sheets/SheetsImportExportPanel.vue'
import type { SuiteCollaborationEvent } from '../types/api'
import type { SheetsWorkbookDetail, SheetsWorkbookSummary } from '../types/sheets'
import { SHEETS_TEMPLATE_PRESETS } from '../utils/sheets-collaboration'

const {
  messageErrorMock,
  messageSuccessMock,
  navigateToMock,
} = vi.hoisted(() => ({
  messageErrorMock: vi.fn(),
  messageSuccessMock: vi.fn(),
  navigateToMock: vi.fn(async () => undefined),
}))

let workbenchState: ReturnType<typeof createWorkbenchState>

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
  },
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) => {
      if (!params) {
        return key
      }
      return Object.entries(params).reduce(
        (result, [paramKey, value]) => result.replace(`{${paramKey}}`, String(value)),
        key,
      )
    },
  }),
}))

vi.mock('~/composables/useSheetsWorkbench', () => ({
  useSheetsWorkbench: () => workbenchState,
}))

const ElAlert = defineComponent({
  name: 'ElAlert',
  props: { title: { type: String, default: '' } },
  template: '<div v-bind="$attrs" class="el-alert-stub">{{ title }}</div>',
})

const ElBadge = defineComponent({
  name: 'ElBadge',
  template: '<div class="el-badge-stub"><slot /></div>',
})

const ElButton = defineComponent({
  name: 'ElButton',
  props: {
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
  },
  emits: ['click'],
  template: '<button v-bind="$attrs" type="button" :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
})

const ElDrawer = defineComponent({
  name: 'ElDrawer',
  props: { modelValue: { type: Boolean, default: false } },
  template: '<div v-if="modelValue" class="el-drawer-stub"><slot /></div>',
})

const ElEmpty = defineComponent({
  name: 'ElEmpty',
  props: { description: { type: String, default: '' } },
  template: '<div v-bind="$attrs" class="el-empty-stub">{{ description }}</div>',
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  template: `
    <input
      v-bind="$attrs"
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `,
})

const ElOption = defineComponent({
  name: 'ElOption',
  props: {
    label: { type: String, default: '' },
    value: { type: String, default: '' },
  },
  template: '<option :value="value">{{ label }}</option>',
})

const ElSegmented = defineComponent({
  name: 'ElSegmented',
  props: {
    modelValue: { type: String, default: '' },
    options: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  template: `
    <select v-bind="$attrs" :value="modelValue" @change="$emit('update:modelValue', $event.target.value)">
      <option
        v-for="item in options"
        :key="item.value"
        :value="item.value"
      >
        {{ item.label }}
      </option>
    </select>
  `,
})

const ElSelect = defineComponent({
  name: 'ElSelect',
  props: {
    modelValue: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
  },
  emits: ['update:modelValue', 'change'],
  methods: {
    onChange(event: Event) {
      const value = (event.target as HTMLSelectElement).value
      this.$emit('update:modelValue', value)
      this.$emit('change', value)
    },
  },
  template: '<select v-bind="$attrs" :value="modelValue" :disabled="disabled" @change="onChange"><slot /></select>',
})

const ElSkeleton = defineComponent({
  name: 'ElSkeleton',
  template: '<div class="el-skeleton-stub" />',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>',
})

function buildWorkbookSummary(): SheetsWorkbookSummary {
  return {
    id: 'wb-1',
    title: 'Roadmap workbook',
    rowCount: 12,
    colCount: 6,
    filledCellCount: 8,
    formulaCellCount: 2,
    computedErrorCount: 0,
    currentVersion: 2,
    sheetCount: 1,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-29T10:00:00',
    lastOpenedAt: '2026-03-29T10:05:00',
    permission: 'OWNER',
    scope: 'OWNED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 2,
    canEdit: true,
  }
}

function buildWorkbookDetail(): SheetsWorkbookDetail {
  return {
    ...buildWorkbookSummary(),
    sheets: [{
      id: 'sheet-1',
      name: 'Summary',
      rowCount: 12,
      colCount: 6,
      frozenRowCount: 0,
      frozenColCount: 0,
      filledCellCount: 8,
      formulaCellCount: 2,
      computedErrorCount: 0,
      grid: [['A1']],
      computedGrid: [['A1']],
    }],
    grid: [['A1']],
    computedGrid: [['A1']],
    supportedImportFormats: ['CSV', 'XLSX'],
    supportedExportFormats: ['CSV', 'JSON'],
    createdAt: '2026-03-29T09:00:00',
    canManageShares: true,
    canRestoreVersions: true,
  }
}

function buildEvent(): SuiteCollaborationEvent {
  return {
    eventId: 101,
    productCode: 'SHEETS',
    eventType: 'SHEETS_WORKBOOK_UPDATE',
    title: 'Workbook updated',
    summary: 'Roadmap workbook changed',
    routePath: '/sheets?workbookId=wb-1',
    actorEmail: 'owner@mmmail.local',
    sessionId: 'session-1',
    createdAt: '2026-03-29T10:30:00',
  }
}

function createActionMocks() {
  return {
    selectWorkbook: vi.fn(async () => true),
    createWorkbookFromTemplate: vi.fn(async () => true),
    onCreateWorkbook: vi.fn(async () => undefined),
    onImportWorkbook: vi.fn(async () => undefined),
    onExportWorkbook: vi.fn(async () => undefined),
    onRenameWorkbook: vi.fn(async () => undefined),
    onDeleteWorkbook: vi.fn(async () => undefined),
    onCreateSheet: vi.fn(async () => undefined),
    onRenameSheet: vi.fn(async () => undefined),
    onDeleteSheet: vi.fn(async () => undefined),
    onSelectSheet: vi.fn(async () => undefined),
    onCellSelect: vi.fn(),
    onCellChange: vi.fn(),
    onFormulaChange: vi.fn(),
    onSaveWorkbook: vi.fn(async () => undefined),
    onSortSheet: vi.fn(async () => undefined),
    onFreezeRowsToActiveCell: vi.fn(async () => undefined),
    onFreezeColsToActiveCell: vi.fn(async () => undefined),
    onClearFreeze: vi.fn(async () => undefined),
    updateSearchQuery: vi.fn(),
    onRefreshWorkspace: vi.fn(async () => undefined),
    refreshIncomingShares: vi.fn(async () => undefined),
    submitShare: vi.fn(async () => undefined),
    updateSharePermission: vi.fn(async () => undefined),
    removeShare: vi.fn(async () => undefined),
    respondIncomingShare: vi.fn(async () => undefined),
    openIncomingWorkbook: vi.fn(async () => undefined),
    openVersionHistory: vi.fn(async () => undefined),
    restoreVersion: vi.fn(async () => undefined),
    refreshCollaboration: vi.fn(async () => undefined),
  }
}

function createWorkbenchState() {
  const workbook = buildWorkbookDetail()
  return {
    activeWorkbook: ref(workbook),
    activeSheet: ref(workbook.sheets[0]),
    activeCell: ref(null),
    localGrid: ref(workbook.grid),
    loadingList: ref(false),
    loadingDetail: ref(false),
    creating: ref(false),
    importing: ref(false),
    exporting: ref(false),
    refreshing: ref(false),
    saving: ref(false),
    sheetBusy: ref(false),
    busyWorkbookId: ref(''),
    conflictMessage: ref(''),
    dirtyCount: ref(0),
    workbookCount: ref(2),
    savedGrid: ref(workbook.grid),
    computedGrid: ref(workbook.computedGrid),
    supportedImportFormats: ref(workbook.supportedImportFormats),
    supportedExportFormats: ref(workbook.supportedExportFormats),
    activeWorkbookId: ref(workbook.id),
    activeWorkbookForHero: ref(workbook),
    activeCellLabel: ref(''),
    activeCellPresentation: ref(null),
    formulaPreviewHint: ref(''),
    localFormulaCellCount: ref(2),
    toolsBusy: ref(false),
    searchQuery: ref(''),
    searchMatchCount: ref(0),
    searchMatchKeys: ref([]),
    frozenRowCount: ref(0),
    frozenColCount: ref(0),
    lastImported: ref(null),
    lastExport: ref(null),
    workbookEvents: ref([buildEvent()]),
    collaborationLoading: ref(false),
    collaborationError: ref(''),
    creatingTemplateCode: ref<string | null>(null),
    templatePresets: ref(SHEETS_TEMPLATE_PRESETS),
    workspaceView: ref('WORKBOOKS'),
    scopeFilter: ref('ALL'),
    filteredWorkbooks: ref([buildWorkbookSummary()]),
    shares: ref([]),
    incomingShares: ref([]),
    versions: ref([]),
    inviteEmail: ref(''),
    invitePermission: ref<'VIEW' | 'EDIT'>('VIEW'),
    sharesLoading: ref(false),
    incomingLoading: ref(false),
    versionsLoading: ref(false),
    sharesErrorMessage: ref(''),
    incomingErrorMessage: ref(''),
    versionsErrorMessage: ref(''),
    shareSubmitting: ref(false),
    shareMutationId: ref(''),
    incomingMutationId: ref(''),
    versionMutationId: ref(''),
    versionDrawerVisible: ref(false),
    pendingIncomingCount: ref(0),
    canManageShares: ref(true),
    canRestoreVersions: ref(true),
    ...createActionMocks(),
  }
}

async function mountPage() {
  return mount(SheetsPage, {
    global: {
      components: {
        SheetsImportExportPanel,
        SheetsCollaborationRail,
      },
      directives: {
        loading: {},
      },
      stubs: {
        ElAlert,
        ElBadge,
        ElButton,
        ElDrawer,
        ElEmpty,
        ElInput,
        ElOption,
        ElSegmented,
        ElSelect,
        ElSkeleton,
        ElTag,
        SheetsWorkspaceHero: { template: '<div class="workspace-hero-stub" />' },
        SheetsWorkbookSidebar: { template: '<div class="workbook-sidebar-stub" />' },
        SheetsWorkbookTabs: { template: '<div class="workbook-tabs-stub" />' },
        SheetsDataToolsPanel: { template: '<div class="data-tools-stub" />' },
        SheetsFormulaPanel: { template: '<div class="formula-panel-stub" />' },
        SheetsGridEditor: { template: '<div class="grid-editor-stub" />' },
        SheetsShareManager: { template: '<div class="share-manager-stub" />' },
        SheetsIncomingSharesPanel: { template: '<div class="incoming-panel-stub" />' },
        SheetsInsightRail: { template: '<div class="insight-rail-stub" />' },
        SheetsVersionHistoryDrawer: { template: '<div class="version-drawer-stub" />' },
      },
    },
  })
}

describe('sheets trade/collaboration smoke', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    workbenchState = createWorkbenchState()
    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('navigateTo', navigateToMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('wires import and export actions through the page', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    await wrapper.get('[data-testid=\"sheets-import-title\"]').setValue('Budget import')
    const fileInput = wrapper.get('[data-testid=\"sheets-import-file-input\"]')
    const importFile = new File(['name,value'], 'budget.csv', { type: 'text/csv' })

    Object.defineProperty(fileInput.element, 'files', {
      configurable: true,
      value: [importFile],
    })

    await fileInput.trigger('change')
    expect(workbenchState.onImportWorkbook).toHaveBeenCalledWith({
      file: importFile,
      title: 'Budget import',
    })

    await wrapper.get('[data-testid=\"sheets-export-format\"]').setValue('JSON')
    await wrapper.get('[data-testid=\"sheets-export-submit\"]').trigger('click')
    expect(workbenchState.onExportWorkbook).toHaveBeenCalledWith('JSON')
  })

  it('wires template creation and collaboration event open through the page', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    await wrapper.get('[data-testid=\"sheets-template-launch-readiness\"]').trigger('click')
    expect(workbenchState.createWorkbookFromTemplate).toHaveBeenCalledWith(
      SHEETS_TEMPLATE_PRESETS[0],
      SHEETS_TEMPLATE_PRESETS[0].presetTitleKey,
    )
    expect(messageSuccessMock).toHaveBeenCalledWith('sheets.messages.templateCreated')

    await wrapper.get('[data-testid=\"sheets-event-101\"]').trigger('click')
    expect(navigateToMock).toHaveBeenCalledWith('/sheets?workbookId=wb-1')
  })

  it('does not show template success when template creation is canceled', async () => {
    workbenchState.createWorkbookFromTemplate.mockResolvedValueOnce(false)
    const wrapper = await mountPage()
    await flushPromises()

    await wrapper.get('[data-testid="sheets-template-launch-readiness"]').trigger('click')

    expect(messageSuccessMock).not.toHaveBeenCalled()
  })
})
