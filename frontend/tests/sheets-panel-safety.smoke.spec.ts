import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsShareManager from '../components/sheets/SheetsShareManager.vue'
import SheetsVersionHistoryDrawer from '../components/sheets/SheetsVersionHistoryDrawer.vue'
import type { SheetsWorkbookDetail, SheetsWorkbookSummary, SheetsWorkbookVersion } from '../types/sheets'

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

const ElAlert = defineComponent({ name: 'ElAlert', props: { title: { type: String, default: '' } }, template: '<div v-bind="$attrs">{{ title }}</div>' })
const ElBadge = defineComponent({ name: 'ElBadge', template: '<div><slot /></div>' })
const ElButton = defineComponent({
  name: 'ElButton',
  props: { disabled: { type: Boolean, default: false }, loading: { type: Boolean, default: false } },
  emits: ['click'],
  template: '<button v-bind="$attrs" type="button" :disabled="disabled || loading" @click="$emit(\'click\', $event)"><slot /></button>',
})
const ElDrawer = defineComponent({ name: 'ElDrawer', props: { modelValue: { type: Boolean, default: false } }, template: '<div v-if="modelValue"><slot /></div>' })
const ElEmpty = defineComponent({ name: 'ElEmpty', props: { description: { type: String, default: '' } }, template: '<div v-bind="$attrs">{{ description }}</div>' })
const ElInput = defineComponent({
  name: 'ElInput',
  props: { modelValue: { type: String, default: '' }, placeholder: { type: String, default: '' }, disabled: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" :placeholder="placeholder" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)">',
})
const ElOption = defineComponent({ name: 'ElOption', props: { label: { type: String, default: '' }, value: { type: String, default: '' } }, template: '<option :value="value">{{ label }}</option>' })
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
const ElSegmented = defineComponent({
  name: 'ElSegmented',
  props: { modelValue: { type: String, default: '' }, options: { type: Array, default: () => [] } },
  emits: ['update:modelValue'],
  template: '<select v-bind="$attrs" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</option></select>',
})
const ElSkeleton = defineComponent({ name: 'ElSkeleton', template: '<div v-bind="$attrs" />' })
const ElTag = defineComponent({ name: 'ElTag', template: '<span><slot /></span>' })

function buildWorkbookSummary(overrides: Partial<SheetsWorkbookSummary> = {}): SheetsWorkbookSummary {
  return {
    id: 'wb-1',
    title: 'Roadmap workbook',
    rowCount: 6,
    colCount: 4,
    filledCellCount: 8,
    formulaCellCount: 1,
    computedErrorCount: 0,
    currentVersion: 2,
    sheetCount: 1,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-30T11:00:00',
    lastOpenedAt: '2026-03-30T11:05:00',
    permission: 'OWNER',
    scope: 'OWNED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 0,
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
      rowCount: 6,
      colCount: 4,
      frozenRowCount: 1,
      frozenColCount: 1,
      filledCellCount: 8,
      formulaCellCount: 1,
      computedErrorCount: 0,
      grid: [['Roadmap']],
      computedGrid: [['Roadmap']],
    }],
    grid: [['Roadmap']],
    computedGrid: [['Roadmap']],
    supportedImportFormats: ['CSV', 'XLSX'],
    supportedExportFormats: ['CSV', 'JSON'],
    createdAt: '2026-03-30T10:30:00',
    canManageShares: true,
    canRestoreVersions: true,
    ...overrides,
  }
}

function buildVersion(versionId: string, versionNo: number): SheetsWorkbookVersion {
  return {
    versionId,
    versionNo,
    title: `Version ${versionNo}`,
    rowCount: 6,
    colCount: 4,
    createdByUserId: 'user-1',
    createdByEmail: 'owner@mmmail.local',
    createdByDisplayName: 'Owner',
    sourceEvent: 'WORKBOOK_SAVE',
    createdAt: '2026-03-30T10:40:00',
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
    activeCellPresentation: ref({ rawValue: 'Roadmap', computedValue: 'Roadmap', displayValue: 'Roadmap', isFormula: false, isDirty: false, isDirtyFormula: false, hasError: false }),
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
    incomingShares: ref([]),
    versions: ref<SheetsWorkbookVersion[]>([buildVersion('version-1', 1)]),
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
    versionDrawerVisible: ref(true),
    pendingIncomingCount: ref(0),
    canManageShares: ref(true),
    canRestoreVersions: ref(true),
    ...createActionMocks(),
  }
}

async function mountPage() {
  return mount(SheetsPage, {
    global: {
      components: { SheetsShareManager, SheetsVersionHistoryDrawer },
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
        SheetsCollaborationRail: { template: '<div />' },
        SheetsImportExportPanel: { template: '<div />' },
        SheetsInsightRail: { template: '<div />' },
      },
    },
  })
}

describe('sheets panel safety smoke', () => {
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

  it('shows owner empty share state with create controls', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.find('[data-testid="sheets-share-create"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="sheets-share-empty"]').text()).toContain('sheets.share.empty')
    expect(wrapper.find('[data-testid="sheets-share-list"]').exists()).toBe(false)
  })

  it('hides share controls without an active workbook', async () => {
    workbenchState.activeWorkbook.value = null
    workbenchState.activeWorkbookForHero.value = null
    workbenchState.activeWorkbookId.value = null
    workbenchState.canManageShares.value = true

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.find('[data-testid="sheets-share-create"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="sheets-share-empty"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="sheets-share-list"]').exists()).toBe(false)
  })

  it('shows version empty and disables restore without an active workbook', async () => {
    workbenchState.versions.value = []
    let wrapper = await mountPage()
    await flushPromises()
    expect(wrapper.get('[data-testid="sheets-version-empty"]').text()).toContain('sheets.versions.empty')
    expect(wrapper.find('[data-testid="sheets-version-list"]').exists()).toBe(false)
    wrapper.unmount()

    workbenchState = createWorkbenchState()
    workbenchState.activeWorkbook.value = null
    workbenchState.activeWorkbookForHero.value = null
    workbenchState.activeWorkbookId.value = null
    wrapper = await mountPage()
    await flushPromises()
    expect(wrapper.get('[data-testid="sheets-version-restore-version-1"]').attributes('disabled')).toBeDefined()
  })
})
