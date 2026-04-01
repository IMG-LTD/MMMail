import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsCollaborationRail from '../components/sheets/SheetsCollaborationRail.vue'
import SheetsShareManager from '../components/sheets/SheetsShareManager.vue'
import SheetsVersionHistoryDrawer from '../components/sheets/SheetsVersionHistoryDrawer.vue'
import type { SuiteCollaborationEvent } from '../types/api'
import type {
  SheetsWorkbookDetail,
  SheetsWorkbookShare,
  SheetsWorkbookSummary,
  SheetsWorkbookVersion,
} from '../types/sheets'

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
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' },
  },
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

const ElSegmented = defineComponent({
  name: 'ElSegmented',
  props: {
    modelValue: { type: String, default: '' },
    options: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  template: `
    <select v-bind="$attrs" :value="modelValue" @change="$emit('update:modelValue', $event.target.value)">
      <option v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</option>
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
    title: 'Shared budget',
    rowCount: 6,
    colCount: 4,
    filledCellCount: 10,
    formulaCellCount: 2,
    computedErrorCount: 0,
    currentVersion: 3,
    sheetCount: 1,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-30T09:00:00',
    lastOpenedAt: '2026-03-30T09:05:00',
    permission: 'VIEW',
    scope: 'SHARED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 2,
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
      filledCellCount: 10,
      formulaCellCount: 2,
      computedErrorCount: 0,
      grid: [['Budget']],
      computedGrid: [['Budget']],
    }],
    grid: [['Budget']],
    computedGrid: [['Budget']],
    supportedImportFormats: ['CSV', 'XLSX'],
    supportedExportFormats: ['CSV', 'JSON'],
    createdAt: '2026-03-30T08:00:00',
    canManageShares: false,
    canRestoreVersions: false,
    ...overrides,
  }
}

function buildShare(): SheetsWorkbookShare {
  return {
    shareId: 'share-1',
    collaboratorUserId: 'user-2',
    collaboratorEmail: 'viewer@mmmail.local',
    collaboratorDisplayName: 'Viewer',
    permission: 'VIEW',
    responseStatus: 'ACCEPTED',
    createdAt: '2026-03-30T08:10:00',
    updatedAt: '2026-03-30T08:20:00',
  }
}

function buildVersions(): SheetsWorkbookVersion[] {
  return [
    {
      versionId: 'version-3',
      versionNo: 3,
      title: 'Current version',
      rowCount: 6,
      colCount: 4,
      createdByUserId: 'user-1',
      createdByEmail: 'owner@mmmail.local',
      createdByDisplayName: 'Owner',
      sourceEvent: 'WORKBOOK_SAVE',
      createdAt: '2026-03-30T08:40:00',
    },
    {
      versionId: 'version-2',
      versionNo: 2,
      title: 'Previous version',
      rowCount: 6,
      colCount: 4,
      createdByUserId: 'user-1',
      createdByEmail: 'owner@mmmail.local',
      createdByDisplayName: 'Owner',
      sourceEvent: 'WORKBOOK_SAVE',
      createdAt: '2026-03-30T08:20:00',
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
    activeCellPresentation: ref({
      rawValue: 'Budget',
      computedValue: 'Budget',
      displayValue: 'Budget',
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
    workbookEvents: ref<SuiteCollaborationEvent[]>([]),
    collaborationLoading: ref(false),
    collaborationError: ref(''),
    creatingTemplateCode: ref<string | null>(null),
    templatePresets: ref([]),
    workspaceView: ref<'WORKBOOKS' | 'INCOMING_SHARES'>('WORKBOOKS'),
    scopeFilter: ref<'ALL' | 'OWNED' | 'SHARED'>('ALL'),
    filteredWorkbooks: ref([buildWorkbookSummary()]),
    shares: ref([buildShare()]),
    incomingShares: ref([]),
    versions: ref(buildVersions()),
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
    canManageShares: ref(false),
    canRestoreVersions: ref(false),
    ...createActionMocks(),
  }
}

async function mountPage() {
  return mount(SheetsPage, {
    global: {
      components: {
        SheetsShareManager,
        SheetsCollaborationRail,
        SheetsVersionHistoryDrawer,
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
        ElSelect,
        ElSegmented,
        ElSkeleton,
        ElTag,
        SheetsWorkspaceHero: { template: '<div class="hero-stub" />' },
        SheetsWorkbookSidebar: { template: '<div class="sidebar-stub" />' },
        SheetsWorkbookTabs: { template: '<div class="tabs-stub" />' },
        SheetsDataToolsPanel: { template: '<div class="tools-stub" />' },
        SheetsFormulaPanel: { template: '<div class="formula-stub" />' },
        SheetsGridEditor: { template: '<div class="grid-stub" />' },
        SheetsIncomingSharesPanel: { template: '<div class="incoming-stub" />' },
        SheetsImportExportPanel: { template: '<div class="trade-stub" />' },
        SheetsInsightRail: { template: '<div class="insight-stub" />' },
      },
    },
  })
}

describe('sheets sharing boundary smoke', () => {
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

  it('shows readonly share state without management controls', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-share-readonly"]').text()).toContain('sheets.share.readonlyOwner')
    expect(wrapper.find('[data-testid="sheets-share-permission-share-1"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="sheets-share-revoke-share-1"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="sheets-share-list"]').exists()).toBe(true)
  })

  it('shows collaboration loading and empty states', async () => {
    workbenchState.collaborationLoading.value = true
    let wrapper = await mountPage()
    await flushPromises()
    expect(wrapper.find('[data-testid="sheets-collaboration-loading"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="sheets-collaboration-empty"]').exists()).toBe(false)
    wrapper.unmount()

    workbenchState = createWorkbenchState()
    workbenchState.workbookEvents.value = []
    wrapper = await mountPage()
    await flushPromises()
    expect(wrapper.find('[data-testid="sheets-collaboration-loading"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="sheets-collaboration-empty"]').text()).toContain('sheets.collaboration.empty')
  })

  it('disables version restore for readonly workbooks and current version', async () => {
    let wrapper = await mountPage()
    await flushPromises()
    expect(wrapper.get('[data-testid="sheets-version-restore-version-3"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-version-restore-version-2"]').attributes('disabled')).toBeDefined()
    wrapper.unmount()

    workbenchState = createWorkbenchState()
    workbenchState.canRestoreVersions.value = true
    wrapper = await mountPage()
    await flushPromises()
    expect(wrapper.get('[data-testid="sheets-version-restore-version-3"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-version-restore-version-2"]').attributes('disabled')).toBeUndefined()
  })
})
