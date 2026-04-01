import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsCollaborationRail from '../components/sheets/SheetsCollaborationRail.vue'
import SheetsImportExportPanel from '../components/sheets/SheetsImportExportPanel.vue'
import type { SuiteCollaborationEvent } from '../types/api'
import type { SheetsWorkbookDetail, SheetsWorkbookSummary } from '../types/sheets'
import { SHEETS_TEMPLATE_PRESETS } from '../utils/sheets-collaboration'

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
      <option v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</option>
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

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>',
})

function buildWorkbookSummary(overrides: Partial<SheetsWorkbookSummary> = {}): SheetsWorkbookSummary {
  return {
    id: 'wb-1',
    title: 'Roadmap workbook',
    rowCount: 8,
    colCount: 5,
    filledCellCount: 12,
    formulaCellCount: 2,
    computedErrorCount: 1,
    currentVersion: 3,
    sheetCount: 2,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-30T10:00:00',
    lastOpenedAt: '2026-03-30T10:05:00',
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
    sheets: [
      {
        id: 'sheet-1',
        name: 'Summary',
        rowCount: 8,
        colCount: 5,
        frozenRowCount: 1,
        frozenColCount: 1,
        filledCellCount: 12,
        formulaCellCount: 2,
        computedErrorCount: 1,
        grid: [['Roadmap']],
        computedGrid: [['Roadmap']],
      },
    ],
    grid: [['Roadmap']],
    computedGrid: [['Roadmap']],
    supportedImportFormats: ['CSV', 'XLSX'],
    supportedExportFormats: ['CSV', 'JSON'],
    createdAt: '2026-03-30T09:00:00',
    canManageShares: true,
    canRestoreVersions: true,
    ...overrides,
  }
}

function buildEvent(): SuiteCollaborationEvent {
  return {
    eventId: 101,
    productCode: 'SHEETS',
    eventType: 'SHEETS_UPDATED',
    title: 'Workbook updated',
    summary: 'Budget changed',
    routePath: '/sheets?workbookId=wb-1',
    actorEmail: 'owner@mmmail.local',
    sessionId: null,
    createdAt: '2026-03-30T10:10:00',
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
    lastImported: ref({
      title: 'Imported budget',
      rowCount: 8,
      colCount: 5,
      formulaCellCount: 2,
      sourceFormat: 'CSV' as const,
      importedAt: '2026-03-30T10:15:00',
    }),
    lastExport: ref({
      fileName: 'roadmap.json',
      format: 'JSON' as const,
      content: '{}',
      formulaCellCount: 2,
      computedErrorCount: 1,
      exportedAt: '2026-03-30T10:20:00',
    }),
    workbookEvents: ref<SuiteCollaborationEvent[]>([buildEvent()]),
    collaborationLoading: ref(false),
    collaborationError: ref(''),
    creatingTemplateCode: ref<string | null>(null),
    templatePresets: ref(SHEETS_TEMPLATE_PRESETS),
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
      components: {
        SheetsImportExportPanel,
        SheetsCollaborationRail,
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
        ElSelect,
        ElSkeleton,
        ElTag,
        SheetsWorkspaceHero: { template: '<div class="hero-stub" />' },
        SheetsWorkbookSidebar: { template: '<div class="sidebar-stub" />' },
        SheetsWorkbookTabs: { template: '<div class="tabs-stub" />' },
        SheetsDataToolsPanel: { template: '<div class="tools-stub" />' },
        SheetsFormulaPanel: { template: '<div class="formula-stub" />' },
        SheetsGridEditor: { template: '<div class="grid-stub" />' },
        SheetsShareManager: { template: '<div class="share-stub" />' },
        SheetsIncomingSharesPanel: { template: '<div class="incoming-stub" />' },
        SheetsInsightRail: { template: '<div class="insight-stub" />' },
        SheetsVersionHistoryDrawer: { template: '<div class="version-stub" />' },
      },
    },
  })
}

describe('sheets trade boundary smoke', () => {
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

  it('shows import/export summaries from the page runtime', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-import-meta"]').text()).toContain('Imported budget')
    expect(wrapper.get('[data-testid="sheets-export-meta"]').text()).toContain('Roadmap workbook')
    expect(wrapper.get('[data-testid="sheets-last-export-meta"]').text()).toContain('roadmap.json')
  })

  it('disables export controls when no workbook is active', async () => {
    workbenchState.activeWorkbook.value = null
    workbenchState.activeWorkbookForHero.value = null
    workbenchState.activeWorkbookId.value = null

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.find('[data-testid="sheets-export-meta"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="sheets-export-format"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="sheets-export-submit"]').attributes('disabled')).toBeDefined()
  })

  it('shows template busy state and collaboration count', async () => {
    workbenchState.creatingTemplateCode.value = SHEETS_TEMPLATE_PRESETS[0].code

    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-collaboration-event-count"]').text()).toBe('1')
    expect(wrapper.get(`[data-testid="sheets-template-${SHEETS_TEMPLATE_PRESETS[0].code}"]`).attributes('disabled')).toBeDefined()
    expect(wrapper.get(`[data-testid="sheets-template-${SHEETS_TEMPLATE_PRESETS[1].code}"]`).attributes('disabled')).toBeUndefined()
  })
})
