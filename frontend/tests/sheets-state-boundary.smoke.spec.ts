import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsDataToolsPanel from '../components/sheets/SheetsDataToolsPanel.vue'
import SheetsFormulaPanel from '../components/sheets/SheetsFormulaPanel.vue'
import SheetsGridEditor from '../components/sheets/SheetsGridEditor.vue'
import type { SheetsWorkbookDetail, SheetsWorkbookSummary } from '../types/sheets'

const { messageErrorMock, messageSuccessMock, navigateToMock } = vi.hoisted(() => ({
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
  template: '<button v-bind="$attrs" type="button" :disabled="disabled || loading" @click="$emit(\'click\', $event)"><slot /></button>',
})

const ElDrawer = defineComponent({
  name: 'ElDrawer',
  props: { modelValue: { type: Boolean, default: false } },
  template: '<div v-if="modelValue" class="el-drawer-stub"><slot /></div>',
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
  },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" :placeholder="placeholder" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)">',
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
      <option v-for="item in options" :key="item.value" :value="item.value">
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
  template: '<div v-bind="$attrs" class="el-skeleton-stub" />',
})

const ElSwitch = defineComponent({
  name: 'ElSwitch',
  props: {
    modelValue: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false },
  },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" type="checkbox" :checked="modelValue" :disabled="disabled" @change="$emit(\'update:modelValue\', $event.target.checked)">',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>',
})

function buildWorkbookSummary(overrides: Partial<SheetsWorkbookSummary> = {}): SheetsWorkbookSummary {
  return {
    id: 'wb-1',
    title: 'Roadmap workbook',
    rowCount: 2,
    colCount: 2,
    filledCellCount: 2,
    formulaCellCount: 1,
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
    ...overrides,
  }
}

function buildWorkbookDetail(overrides: Partial<SheetsWorkbookDetail> = {}): SheetsWorkbookDetail {
  return {
    ...buildWorkbookSummary(overrides),
    sheets: [{
      id: 'sheet-1',
      name: 'Summary',
      rowCount: 2,
      colCount: 2,
      frozenRowCount: 1,
      frozenColCount: 1,
      filledCellCount: 2,
      formulaCellCount: 1,
      computedErrorCount: 0,
      grid: [['Roadmap', '=SUM(A1:A1)'], ['Budget', 'Open']],
      computedGrid: [['Roadmap', '10'], ['Budget', 'Open']],
    }],
    grid: [['Roadmap', '=SUM(A1:A1)'], ['Budget', 'Open']],
    computedGrid: [['Roadmap', '10'], ['Budget', 'Open']],
    supportedImportFormats: ['CSV', 'XLSX'],
    supportedExportFormats: ['CSV', 'JSON'],
    createdAt: '2026-03-29T09:00:00',
    canManageShares: true,
    canRestoreVersions: true,
    ...overrides,
  }
}

function createActionMocks() {
  return {
    selectWorkbook: vi.fn(async () => true),
    createWorkbookFromTemplate: vi.fn(async () => undefined),
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
  }
}

function createWorkbenchState() {
  const workbook = buildWorkbookDetail()
  return {
    activeWorkbook: ref(workbook), activeSheet: ref(workbook.sheets[0]), activeCell: ref<{ rowIndex: number; colIndex: number } | null>({ rowIndex: 0, colIndex: 1 }),
    localGrid: ref([['Roadmap', '=SUM(A1:A1)'], ['Budget', 'Open']]), loadingList: ref(false), loadingDetail: ref(false),
    creating: ref(false), importing: ref(false), exporting: ref(false), refreshing: ref(false), saving: ref(false),
    sheetBusy: ref(false), busyWorkbookId: ref(''), conflictMessage: ref(''), dirtyCount: ref(0), workbookCount: ref(1),
    savedGrid: ref([['Roadmap', '=SUM(A1:A1)'], ['Budget', 'Open']]), computedGrid: ref([['Roadmap', '10'], ['Budget', 'Open']]),
    supportedImportFormats: ref(workbook.supportedImportFormats), supportedExportFormats: ref(workbook.supportedExportFormats),
    activeWorkbookId: ref(workbook.id), activeWorkbookForHero: ref(workbook), activeCellLabel: ref('B1'),
    activeCellPresentation: ref<{
      rawValue: string
      computedValue: string
      displayValue: string
      isFormula: boolean
      isDirty: boolean
      isDirtyFormula: boolean
      hasError: boolean
    } | null>({ rawValue: '=SUM(A1:A1)', computedValue: '10', displayValue: '10', isFormula: true, isDirty: false, isDirtyFormula: false, hasError: false }),
    formulaPreviewHint: ref('sheets.formula.savedHint'), localFormulaCellCount: ref(1), toolsBusy: ref(false), searchQuery: ref('roadmap'),
    searchMatchCount: ref(1), searchMatchKeys: ref(['0:0']), frozenRowCount: ref(1), frozenColCount: ref(1), lastImported: ref(null),
    lastExport: ref(null), workbookEvents: ref([]), collaborationLoading: ref(false), collaborationError: ref(''),
    creatingTemplateCode: ref<string | null>(null), templatePresets: ref([]), workspaceView: ref('WORKBOOKS'), scopeFilter: ref('ALL'),
    filteredWorkbooks: ref([buildWorkbookSummary()]), shares: ref([]), incomingShares: ref([]), versions: ref([]), inviteEmail: ref(''),
    invitePermission: ref<'VIEW' | 'EDIT'>('VIEW'), sharesLoading: ref(false), incomingLoading: ref(false), versionsLoading: ref(false),
    sharesErrorMessage: ref(''), incomingErrorMessage: ref(''), versionsErrorMessage: ref(''), shareSubmitting: ref(false),
    shareMutationId: ref(''), incomingMutationId: ref(''), versionMutationId: ref(''), versionDrawerVisible: ref(false),
    pendingIncomingCount: ref(0), canManageShares: ref(true), canRestoreVersions: ref(true), ...createActionMocks(),
  }
}

async function mountPage() {
  return mount(SheetsPage, {
    global: {
      components: {
        SheetsDataToolsPanel,
        SheetsFormulaPanel,
        SheetsGridEditor,
      },
      directives: { loading: {} },
      stubs: {
        ElAlert, ElBadge, ElButton, ElDrawer, ElInput, ElOption, ElSegmented, ElSelect, ElSkeleton, ElSwitch, ElTag,
        SheetsWorkspaceHero: { template: '<div class="workspace-hero-stub" />' },
        SheetsWorkbookSidebar: { template: '<div class="workbook-sidebar-stub" />' },
        SheetsWorkbookTabs: { template: '<div class="workbook-tabs-stub" />' },
        SheetsShareManager: { template: '<div class="share-manager-stub" />' },
        SheetsIncomingSharesPanel: { template: '<div class="incoming-panel-stub" />' },
        SheetsCollaborationRail: { template: '<div class="collaboration-rail-stub" />' },
        SheetsImportExportPanel: { template: '<div class="import-export-panel-stub" />' },
        SheetsInsightRail: { template: '<div class="insight-rail-stub" />' },
        SheetsVersionHistoryDrawer: { template: '<div class="version-drawer-stub" />' },
      },
    },
  })
}

function applyReadonlyNoSelectionState() {
  const workbook = buildWorkbookDetail({ permission: 'VIEW', canEdit: false })
  workbenchState.activeWorkbook.value = workbook
  workbenchState.activeSheet.value = workbook.sheets[0]
  workbenchState.activeWorkbookForHero.value = workbook
  workbenchState.activeCell.value = null
  workbenchState.activeCellLabel.value = '—'
  workbenchState.activeCellPresentation.value = null
  workbenchState.formulaPreviewHint.value = 'sheets.formula.hints.emptySelection'
  workbenchState.dirtyCount.value = 0
}

describe('sheets state boundary smoke', () => {
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

  it('shows no-selection and owner boundary state across tools and formula', async () => {
    applyReadonlyNoSelectionState()
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-data-tools-selection"]').text()).toBe('sheets.dataTools.selectionLabel')
    expect(wrapper.get('[data-testid="sheets-data-tools-hint"]').text()).toBe('sheets.messages.ownerOnlyStructureAction')
    expect(wrapper.get('[data-testid="sheets-formula-cell-pill"]').text()).toBe('sheets.formula.noCell')
    expect(wrapper.get('[data-testid="sheets-formula-preview-value"]').text()).toBe('—')
    expect(wrapper.get('[data-testid="sheets-formula-input"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-sort-asc"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-sort-desc"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-freeze-rows"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-freeze-cols"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-clear-freeze"]').attributes('disabled')).toBeDefined()
  })

  it('shows loading and empty grid boundary states through the page', async () => {
    workbenchState.loadingDetail.value = true
    let wrapper = await mountPage()
    await flushPromises()
    expect(wrapper.find('[data-testid="sheets-grid-loading"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="sheets-grid-empty"]').exists()).toBe(false)
    wrapper.unmount()

    workbenchState = createWorkbenchState()
    workbenchState.localGrid.value = []
    workbenchState.savedGrid.value = []
    workbenchState.computedGrid.value = []
    workbenchState.activeCell.value = null
    workbenchState.activeCellPresentation.value = null
    wrapper = await mountPage()
    await flushPromises()
    expect(wrapper.find('[data-testid="sheets-grid-loading"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="sheets-grid-empty"]').text()).toContain('sheets.grid.emptyTitle')
  })

  it('keeps inline editing readonly for view-only workbooks', async () => {
    const workbook = buildWorkbookDetail({ permission: 'VIEW', canEdit: false })
    workbenchState.activeWorkbook.value = workbook
    workbenchState.activeSheet.value = workbook.sheets[0]
    workbenchState.activeWorkbookForHero.value = workbook

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-cell-input-0-1"]').attributes('disabled')).toBeDefined()
    await wrapper.get('[data-testid="sheets-cell-button-1-0"]').trigger('click')
    expect(workbenchState.onCellSelect).toHaveBeenCalledWith({ rowIndex: 1, colIndex: 0 })
  })
})
