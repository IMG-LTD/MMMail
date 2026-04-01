import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsInsightRail from '../components/sheets/SheetsInsightRail.vue'
import type { SheetsWorkbookDetail, SheetsWorkbookSummary } from '../types/sheets'

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
      const suffix = Object.entries(params)
        .map(([paramKey, value]) => `${paramKey}=${value}`)
        .join(' ')
      return `${key} ${suffix}`
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

const ElSkeleton = defineComponent({
  name: 'ElSkeleton',
  template: '<div v-bind="$attrs" />',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span><slot /></span>',
})

function buildWorkbookSummary(overrides: Partial<SheetsWorkbookSummary> = {}): SheetsWorkbookSummary {
  return {
    id: 'wb-1',
    title: 'Finance workbook',
    rowCount: 12,
    colCount: 6,
    filledCellCount: 18,
    formulaCellCount: 5,
    computedErrorCount: 2,
    currentVersion: 4,
    sheetCount: 2,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-30T18:00:00',
    lastOpenedAt: '2026-03-30T18:05:00',
    permission: 'OWNER',
    scope: 'OWNED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 1,
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
      rowCount: 12,
      colCount: 6,
      frozenRowCount: 1,
      frozenColCount: 1,
      filledCellCount: 18,
      formulaCellCount: 5,
      computedErrorCount: 2,
      grid: [['Budget']],
      computedGrid: [['Budget']],
    }],
    grid: [['Budget']],
    computedGrid: [['Budget']],
    supportedImportFormats: ['CSV', 'TSV', 'XLSX'],
    supportedExportFormats: ['CSV', 'JSON'],
    createdAt: '2026-03-30T17:30:00',
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
    dirtyCount: ref(4),
    workbookCount: ref(3),
    savedGrid: ref([['Budget']]),
    computedGrid: ref([['Budget']]),
    supportedImportFormats: ref(['CSV', 'TSV', 'XLSX']),
    supportedExportFormats: ref(['CSV', 'JSON']),
    activeWorkbookId: ref<string | null>(workbook.id),
    activeWorkbookForHero: ref<SheetsWorkbookDetail | null>(workbook),
    activeCellLabel: ref('A1'),
    activeCellPresentation: ref({ rawValue: 'Budget', computedValue: 'Budget', displayValue: 'Budget', isFormula: false, isDirty: false, isDirtyFormula: false, hasError: false }),
    formulaPreviewHint: ref('sheets.formula.savedHint'),
    localFormulaCellCount: ref(5),
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
    workspaceView: ref<'WORKBOOKS' | 'INCOMING_SHARES'>('WORKBOOKS'),
    scopeFilter: ref<'ALL' | 'OWNED' | 'SHARED'>('ALL'),
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
      components: { SheetsInsightRail },
      directives: { loading: {} },
      stubs: {
        ElAlert, ElBadge, ElButton, ElDrawer, ElEmpty, ElInput, ElOption, ElSelect, ElSegmented, ElSkeleton, ElTag,
        SheetsWorkspaceHero: { template: '<div />' },
        SheetsWorkbookSidebar: { template: '<div />' },
        SheetsWorkbookTabs: { template: '<div />' },
        SheetsDataToolsPanel: { template: '<div />' },
        SheetsFormulaPanel: { template: '<div />' },
        SheetsGridEditor: { template: '<div />' },
        SheetsIncomingSharesPanel: { template: '<div />' },
        SheetsShareManager: { template: '<div />' },
        SheetsCollaborationRail: { template: '<div />' },
        SheetsImportExportPanel: { template: '<div />' },
        SheetsVersionHistoryDrawer: { template: '<div />' },
      },
    },
  })
}

describe('sheets insight boundary smoke', () => {
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

  it('shows readiness notes when no active workbook is selected', async () => {
    workbenchState.activeWorkbook.value = null
    workbenchState.activeWorkbookForHero.value = null
    workbenchState.activeWorkbookId.value = null

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-insight-rail"]').attributes('data-state')).toBe('empty')
    expect(wrapper.get('[data-testid="sheets-insight-scope-import"]').text()).toContain('value=CSV / TSV / XLSX')
    expect(wrapper.get('[data-testid="sheets-insight-scope-export"]').text()).toContain('value=CSV / JSON')
    expect(wrapper.findAll('[data-testid^="sheets-insight-health-chip-"]')).toHaveLength(4)
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-0"]').text()).toContain('sheets.health.workbooks count=3')
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-1"]').text()).toContain('sheets.health.multiSheetEnabled')
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-2"]').text()).toContain('sheets.health.importReady')
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-3"]').text()).toContain('sheets.health.exportReady')
  })

  it('shows workbook health chips for formulas errors formats and unsaved edits', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-insight-rail"]').attributes('data-state')).toBe('workbook')
    expect(wrapper.findAll('[data-testid^="sheets-insight-health-chip-"]')).toHaveLength(6)
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-0"]').text()).toContain('sheets.health.sheetCount count=2')
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-1"]').text()).toContain('sheets.health.formulas count=5')
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-2"]').text()).toContain('sheets.health.errors count=2')
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-3"]').text()).toContain('sheets.health.importFormats count=3')
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-4"]').text()).toContain('sheets.health.exportFormats count=2')
    expect(wrapper.get('[data-testid="sheets-insight-health-chip-5"]').text()).toContain('sheets.health.dirty count=4')
  })
})
