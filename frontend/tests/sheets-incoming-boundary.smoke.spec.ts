import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsIncomingSharesPanel from '../components/sheets/SheetsIncomingSharesPanel.vue'
import type { SheetsIncomingShare, SheetsWorkbookDetail, SheetsWorkbookSummary } from '../types/sheets'

const { messageErrorMock, messageSuccessMock, navigateToMock } = vi.hoisted(() => ({
  messageErrorMock: vi.fn(),
  messageSuccessMock: vi.fn(),
  navigateToMock: vi.fn(async () => undefined),
}))

let workbenchState: ReturnType<typeof createWorkbenchState>

vi.mock('element-plus', () => ({
  ElMessage: { error: messageErrorMock, success: messageSuccessMock },
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
  template: '<div v-bind="$attrs">{{ title }}</div>',
})

const ElBadge = defineComponent({
  name: 'ElBadge',
  template: '<div><slot /></div>',
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
  template: '<div v-if="modelValue"><slot /></div>',
})

const ElEmpty = defineComponent({
  name: 'ElEmpty',
  props: { description: { type: String, default: '' } },
  template: '<div v-bind="$attrs">{{ description }}</div>',
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
  props: { label: { type: String, default: '' }, value: { type: String, default: '' } },
  template: '<option :value="value">{{ label }}</option>',
})

const ElSegmented = defineComponent({
  name: 'ElSegmented',
  props: { modelValue: { type: String, default: '' }, options: { type: Array, default: () => [] } },
  emits: ['update:modelValue'],
  template: '<select v-bind="$attrs" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</option></select>',
})

const ElSelect = defineComponent({
  name: 'ElSelect',
  props: { modelValue: { type: String, default: '' }, disabled: { type: Boolean, default: false } },
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

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span><slot /></span>',
})

function buildWorkbookSummary(overrides: Partial<SheetsWorkbookSummary> = {}): SheetsWorkbookSummary {
  return {
    id: 'wb-1',
    title: 'Shared budget',
    rowCount: 6,
    colCount: 4,
    filledCellCount: 8,
    formulaCellCount: 1,
    computedErrorCount: 0,
    currentVersion: 2,
    sheetCount: 1,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-30T12:00:00',
    lastOpenedAt: '2026-03-30T12:05:00',
    permission: 'VIEW',
    scope: 'SHARED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 1,
    canEdit: false,
    ...overrides,
  }
}

function buildWorkbookDetail(overrides: Partial<SheetsWorkbookDetail> = {}): SheetsWorkbookDetail {
  return {
    ...buildWorkbookSummary(overrides),
    sheets: [{
      id: 'sheet-1',
      name: 'Summary',
      rowCount: 6,
      colCount: 4,
      frozenRowCount: 1,
      frozenColCount: 1,
      filledCellCount: 8,
      formulaCellCount: 1,
      computedErrorCount: 0,
      grid: [['Budget']],
      computedGrid: [['Budget']],
    }],
    grid: [['Budget']],
    computedGrid: [['Budget']],
    supportedImportFormats: ['CSV'],
    supportedExportFormats: ['CSV'],
    createdAt: '2026-03-30T11:30:00',
    canManageShares: false,
    canRestoreVersions: false,
    ...overrides,
  }
}

function buildIncomingShares(): SheetsIncomingShare[] {
  return [
    {
      shareId: 'incoming-accept',
      workbookId: 'wb-shared-1',
      workbookTitle: 'Finance plan',
      ownerEmail: 'finance@mmmail.local',
      ownerDisplayName: 'Finance',
      permission: 'EDIT',
      responseStatus: 'NEEDS_ACTION',
      updatedAt: '2026-03-30T11:40:00',
    },
    {
      shareId: 'incoming-open',
      workbookId: 'wb-shared-2',
      workbookTitle: 'Ops board',
      ownerEmail: 'ops@mmmail.local',
      ownerDisplayName: 'Ops',
      permission: 'VIEW',
      responseStatus: 'ACCEPTED',
      updatedAt: '2026-03-30T11:50:00',
    },
  ]
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
    activeWorkbook: ref<SheetsWorkbookDetail | null>(workbook),
    activeSheet: ref(workbook.sheets[0]),
    activeCell: ref({ rowIndex: 0, colIndex: 0 }),
    localGrid: ref([['Budget']]),
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
    workbookCount: ref(1),
    savedGrid: ref([['Budget']]),
    computedGrid: ref([['Budget']]),
    supportedImportFormats: ref(workbook.supportedImportFormats),
    supportedExportFormats: ref(workbook.supportedExportFormats),
    activeWorkbookId: ref<string | null>(workbook.id),
    activeWorkbookForHero: ref<SheetsWorkbookDetail | null>(workbook),
    activeCellLabel: ref('A1'),
    activeCellPresentation: ref({ rawValue: 'Budget', computedValue: 'Budget', displayValue: 'Budget', isFormula: false, isDirty: false, isDirtyFormula: false, hasError: false }),
    formulaPreviewHint: ref('sheets.formula.savedHint'),
    localFormulaCellCount: ref(0),
    toolsBusy: ref(false),
    searchQuery: ref(''),
    searchMatchCount: ref(0),
    searchMatchKeys: ref<string[]>([]),
    frozenRowCount: ref(1),
    frozenColCount: ref(1),
    lastImported: ref(null),
    lastExport: ref(null),
    workbookEvents: ref([]),
    collaborationLoading: ref(false),
    collaborationError: ref(''),
    creatingTemplateCode: ref<string | null>(null),
    templatePresets: ref([]),
    workspaceView: ref<'WORKBOOKS' | 'INCOMING_SHARES'>('INCOMING_SHARES'),
    scopeFilter: ref<'ALL' | 'OWNED' | 'SHARED'>('ALL'),
    filteredWorkbooks: ref([buildWorkbookSummary()]),
    shares: ref([]),
    incomingShares: ref(buildIncomingShares()),
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
    pendingIncomingCount: ref(1),
    canManageShares: ref(false),
    canRestoreVersions: ref(false),
    ...createActionMocks(),
  }
}

async function mountPage() {
  return mount(SheetsPage, {
    global: {
      components: { SheetsIncomingSharesPanel },
      directives: { loading: {} },
      stubs: {
        ElAlert, ElBadge, ElButton, ElDrawer, ElEmpty, ElInput, ElOption, ElSegmented, ElSelect, ElTag,
        SheetsWorkspaceHero: { template: '<div />' },
        SheetsWorkbookSidebar: { template: '<div />' },
        SheetsWorkbookTabs: { template: '<div />' },
        SheetsDataToolsPanel: { template: '<div />' },
        SheetsFormulaPanel: { template: '<div />' },
        SheetsGridEditor: { template: '<div />' },
        SheetsShareManager: { template: '<div />' },
        SheetsCollaborationRail: { template: '<div />' },
        SheetsImportExportPanel: { template: '<div />' },
        SheetsInsightRail: { template: '<div />' },
        SheetsVersionHistoryDrawer: { template: '<div />' },
      },
    },
  })
}

describe('sheets incoming boundary smoke', () => {
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

  it('shows incoming counts and action visibility from page runtime', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-incoming-total"]').text()).toBe('2')
    expect(wrapper.get('[data-testid="sheets-incoming-pending"]').text()).toBe('1')
    expect(wrapper.get('[data-testid="sheets-incoming-accepted"]').text()).toBe('1')
    expect(wrapper.find('[data-testid="sheets-incoming-accept-incoming-accept"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="sheets-incoming-open-incoming-open"]').exists()).toBe(true)
  })

  it('shows empty state when incoming list is empty', async () => {
    workbenchState.incomingShares.value = []
    workbenchState.pendingIncomingCount.value = 0

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-incoming-total"]').text()).toBe('0')
    expect(wrapper.get('[data-testid="sheets-incoming-empty"]').text()).toContain('sheets.incoming.empty')
    expect(wrapper.find('[data-testid="sheets-incoming-body"]').exists()).toBe(false)
  })

  it('disables mutation buttons and wires refresh/open actions', async () => {
    workbenchState.incomingMutationId.value = 'incoming-accept'

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-incoming-accept-incoming-accept"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-incoming-decline-incoming-accept"]').attributes('disabled')).toBeDefined()

    await wrapper.get('[data-testid="sheets-incoming-refresh"]').trigger('click')
    expect(workbenchState.refreshIncomingShares).toHaveBeenCalledTimes(1)

    await wrapper.get('[data-testid="sheets-incoming-open-incoming-open"]').trigger('click')
    expect(workbenchState.openIncomingWorkbook).toHaveBeenCalledWith(
      expect.objectContaining({ shareId: 'incoming-open' }),
    )
  })
})
