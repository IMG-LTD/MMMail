import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsWorkspaceHero from '../components/sheets/SheetsWorkspaceHero.vue'
import SheetsWorkbookSidebar from '../components/sheets/SheetsWorkbookSidebar.vue'
import SheetsIncomingSharesPanel from '../components/sheets/SheetsIncomingSharesPanel.vue'
import type { SheetsIncomingShare, SheetsWorkbookDetail, SheetsWorkbookSheet, SheetsWorkbookSummary } from '../types/sheets'

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

const ElSkeleton = defineComponent({
  name: 'ElSkeleton',
  template: '<div v-bind="$attrs" class="el-skeleton-stub" />',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>',
})

function buildWorkbookSummary(overrides: Partial<SheetsWorkbookSummary> = {}): SheetsWorkbookSummary {
  return {
    id: 'wb-1',
    title: 'Roadmap workbook',
    rowCount: 8,
    colCount: 4,
    filledCellCount: 12,
    formulaCellCount: 2,
    computedErrorCount: 0,
    currentVersion: 3,
    sheetCount: 1,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-30T08:00:00',
    lastOpenedAt: '2026-03-30T08:05:00',
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
      rowCount: 8,
      colCount: 4,
      frozenRowCount: 1,
      frozenColCount: 1,
      filledCellCount: 12,
      formulaCellCount: 2,
      computedErrorCount: 0,
      grid: [['Roadmap']],
      computedGrid: [['Roadmap']],
    }],
    grid: [['Roadmap']],
    computedGrid: [['Roadmap']],
    supportedImportFormats: ['CSV', 'XLSX'],
    supportedExportFormats: ['CSV', 'JSON'],
    createdAt: '2026-03-29T09:00:00',
    canManageShares: true,
    canRestoreVersions: true,
    ...overrides,
  }
}

function buildIncomingShare(): SheetsIncomingShare {
  return {
    shareId: 'share-incoming-1',
    workbookId: 'wb-shared-1',
    workbookTitle: 'Shared budget',
    ownerEmail: 'finance@mmmail.local',
    ownerDisplayName: 'Finance',
    permission: 'EDIT',
    responseStatus: 'NEEDS_ACTION',
    updatedAt: '2026-03-30T07:00:00',
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
    activeSheet: ref<SheetsWorkbookSheet | null>(workbook.sheets[0]),
    activeCell: ref({ rowIndex: 0, colIndex: 0 }),
    localGrid: ref([['Roadmap']]),
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
    savedGrid: ref([['Roadmap']]),
    computedGrid: ref([['Roadmap']]),
    supportedImportFormats: ref(workbook.supportedImportFormats),
    supportedExportFormats: ref(workbook.supportedExportFormats),
    activeWorkbookId: ref<string | null>(workbook.id),
    activeWorkbookForHero: ref<SheetsWorkbookDetail | null>(workbook),
    activeCellLabel: ref('A1'),
    activeCellPresentation: ref({
      rawValue: 'Roadmap',
      computedValue: 'Roadmap',
      displayValue: 'Roadmap',
      isFormula: false,
      isDirty: false,
      isDirtyFormula: false,
      hasError: false,
    }),
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
    workspaceView: ref<'WORKBOOKS' | 'INCOMING_SHARES'>('WORKBOOKS'),
    scopeFilter: ref<'ALL' | 'OWNED' | 'SHARED'>('ALL'),
    filteredWorkbooks: ref([buildWorkbookSummary()]),
    shares: ref([]),
    incomingShares: ref([buildIncomingShare()]),
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
    canManageShares: ref(true),
    canRestoreVersions: ref(true),
    ...createActionMocks(),
  }
}

async function mountPage() {
  return mount(SheetsPage, {
    global: {
      components: {
        SheetsWorkspaceHero,
        SheetsWorkbookSidebar,
        SheetsIncomingSharesPanel,
      },
      directives: { loading: {} },
      stubs: {
        ElAlert,
        ElBadge,
        ElButton,
        ElDrawer,
        ElEmpty,
        ElInput,
        ElOption,
        ElSegmented,
        ElSkeleton,
        ElTag,
        SheetsWorkbookTabs: { template: '<div class="tabs-stub" />' },
        SheetsDataToolsPanel: { template: '<div class="data-tools-stub" />' },
        SheetsFormulaPanel: { template: '<div class="formula-stub" />' },
        SheetsGridEditor: { template: '<div class="grid-stub" />' },
        SheetsShareManager: { template: '<div class="share-stub" />' },
        SheetsCollaborationRail: { template: '<div class="collaboration-stub" />' },
        SheetsImportExportPanel: { template: '<div class="trade-stub" />' },
        SheetsInsightRail: { template: '<div class="insight-stub" />' },
        SheetsVersionHistoryDrawer: { template: '<div class="version-stub" />' },
      },
    },
  })
}

describe('sheets toolbar/empty smoke', () => {
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

  it('switches between workbook and incoming views from the toolbar', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.find('[data-testid="sheets-scope-filter"]').exists()).toBe(true)
    expect(wrapper.findComponent(SheetsWorkbookSidebar).exists()).toBe(true)

    await wrapper.get('[data-testid="sheets-workspace-view"]').setValue('INCOMING_SHARES')
    expect(workbenchState.workspaceView.value).toBe('INCOMING_SHARES')
    expect(wrapper.find('[data-testid="sheets-scope-filter"]').exists()).toBe(false)
    expect(wrapper.findComponent(SheetsIncomingSharesPanel).exists()).toBe(true)

    await wrapper.get('[data-testid="sheets-refresh-incoming"]').trigger('click')
    expect(workbenchState.refreshIncomingShares).toHaveBeenCalledTimes(1)

    await wrapper.get('[data-testid="sheets-workspace-view"]').setValue('WORKBOOKS')
    await wrapper.get('[data-testid="sheets-scope-filter"]').setValue('SHARED')
    expect(workbenchState.workspaceView.value).toBe('WORKBOOKS')
    expect(workbenchState.scopeFilter.value).toBe('SHARED')
  })

  it('shows empty workspace hero and disabled save when no workbook is active', async () => {
    workbenchState.activeWorkbook.value = null
    workbenchState.activeWorkbookForHero.value = null
    workbenchState.activeWorkbookId.value = null
    workbenchState.activeSheet.value = null
    workbenchState.filteredWorkbooks.value = []
    workbenchState.workbookCount.value = 0
    workbenchState.localGrid.value = []
    workbenchState.savedGrid.value = []
    workbenchState.computedGrid.value = []

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-hero-title"]').text()).toBe('sheets.hero.emptyTitle')
    expect(wrapper.get('[data-testid="sheets-hero-description"]').text()).toBe('sheets.hero.emptyDescription')
    expect(wrapper.get('[data-testid="sheets-hero-save"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-sidebar-empty"]').text()).toContain('sheets.sidebar.emptyTitle')
  })

  it('shows sidebar loading state while workbook library is loading', async () => {
    workbenchState.loadingList.value = true

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.find('[data-testid="sheets-sidebar-loading"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="sheets-sidebar-empty"]').exists()).toBe(false)
  })
})
