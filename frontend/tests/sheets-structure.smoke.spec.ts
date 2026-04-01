import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsWorkbookSidebar from '../components/sheets/SheetsWorkbookSidebar.vue'
import SheetsWorkbookTabs from '../components/sheets/SheetsWorkbookTabs.vue'
import SheetsWorkspaceHero from '../components/sheets/SheetsWorkspaceHero.vue'
import type { SheetsWorkbookDetail, SheetsWorkbookSummary } from '../types/sheets'

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
  template: '<button v-bind="$attrs" type="button" :disabled="disabled || loading" @click="$emit(\'click\', $event)"><slot /></button>',
})

const ElDrawer = defineComponent({
  name: 'ElDrawer',
  props: { modelValue: { type: Boolean, default: false } },
  template: '<div v-if="modelValue" class="el-drawer-stub"><slot /></div>',
})

const ElDropdown = defineComponent({
  name: 'ElDropdown',
  template: '<div class="el-dropdown-stub"><slot /><slot name="dropdown" /></div>',
})

const ElDropdownItem = defineComponent({
  name: 'ElDropdownItem',
  emits: ['click'],
  template: '<button type="button" class="el-dropdown-item-stub" @click="$emit(\'click\')"><slot /></button>',
})

const ElDropdownMenu = defineComponent({
  name: 'ElDropdownMenu',
  template: '<div class="el-dropdown-menu-stub"><slot /></div>',
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
  template: '<input v-bind="$attrs" :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)">',
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

function buildWorkbookSummary(
  workbookId: string,
  title: string,
  scope: 'OWNED' | 'SHARED' = 'OWNED',
): SheetsWorkbookSummary {
  return {
    id: workbookId,
    title,
    rowCount: 12,
    colCount: 6,
    filledCellCount: 8,
    formulaCellCount: 2,
    computedErrorCount: 0,
    currentVersion: workbookId === 'wb-1' ? 2 : 1,
    sheetCount: workbookId === 'wb-1' ? 2 : 1,
    activeSheetId: workbookId === 'wb-1' ? 'sheet-1' : 'sheet-3',
    updatedAt: '2026-03-29T10:00:00',
    lastOpenedAt: '2026-03-29T10:05:00',
    permission: scope === 'OWNED' ? 'OWNER' : 'EDIT',
    scope,
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 2,
    canEdit: true,
  }
}

function buildWorkbookDetail(): SheetsWorkbookDetail {
  return {
    ...buildWorkbookSummary('wb-1', 'Roadmap workbook'),
    sheets: [
      {
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
      },
      {
        id: 'sheet-2',
        name: 'Archive',
        rowCount: 12,
        colCount: 6,
        frozenRowCount: 0,
        frozenColCount: 0,
        filledCellCount: 2,
        formulaCellCount: 0,
        computedErrorCount: 0,
        grid: [['B1']],
        computedGrid: [['B1']],
      },
    ],
    grid: [['A1']],
    computedGrid: [['A1']],
    supportedImportFormats: ['CSV', 'XLSX'],
    supportedExportFormats: ['CSV', 'JSON'],
    createdAt: '2026-03-29T09:00:00',
    canManageShares: true,
    canRestoreVersions: true,
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
    conflictMessage: ref('Version conflict surfaced'),
    dirtyCount: ref(2),
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
    workbookEvents: ref([]),
    collaborationLoading: ref(false),
    collaborationError: ref(''),
    creatingTemplateCode: ref<string | null>(null),
    templatePresets: ref([]),
    workspaceView: ref('WORKBOOKS'),
    scopeFilter: ref('ALL'),
    filteredWorkbooks: ref([
      buildWorkbookSummary('wb-1', 'Roadmap workbook'),
      buildWorkbookSummary('wb-2', 'Shared budget', 'SHARED'),
    ]),
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
        SheetsWorkspaceHero,
        SheetsWorkbookSidebar,
        SheetsWorkbookTabs,
      },
      directives: {
        loading: {},
      },
      stubs: {
        ElAlert,
        ElBadge,
        ElButton,
        ElDrawer,
        ElDropdown,
        ElDropdownItem,
        ElDropdownMenu,
        ElEmpty,
        ElInput,
        ElOption,
        ElSegmented,
        ElSelect,
        ElSkeleton,
        ElTag,
        SheetsDataToolsPanel: { template: '<div class="data-tools-stub" />' },
        SheetsFormulaPanel: { template: '<div class="formula-panel-stub" />' },
        SheetsGridEditor: { template: '<div class="grid-editor-stub" />' },
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

describe('sheets structure smoke', () => {
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

  it('shows conflict recovery and wires hero plus sidebar actions', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.get('[data-testid="sheets-conflict-alert"]').text()).toContain('Version conflict surfaced')

    await wrapper.get('[data-testid="sheets-hero-create"]').trigger('click')
    await wrapper.get('[data-testid="sheets-hero-refresh"]').trigger('click')
    await wrapper.get('[data-testid="sheets-hero-save"]').trigger('click')
    expect(workbenchState.onCreateWorkbook).toHaveBeenCalledTimes(1)
    expect(workbenchState.onRefreshWorkspace).toHaveBeenCalledTimes(1)
    expect(workbenchState.onSaveWorkbook).toHaveBeenCalledTimes(1)

    await wrapper.get('[data-testid="sheets-sidebar-create"]').trigger('click')
    expect(workbenchState.onCreateWorkbook).toHaveBeenCalledTimes(2)

    await wrapper.get('[data-testid="sheets-workbook-wb-2"]').trigger('click')
    expect(workbenchState.selectWorkbook).toHaveBeenCalledWith('wb-2', true)

    await wrapper.get('[data-testid="sheets-workbook-rename-wb-1"]').trigger('click')
    expect(workbenchState.onRenameWorkbook).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'wb-1' }),
    )

    await wrapper.get('[data-testid="sheets-workbook-delete-wb-1"]').trigger('click')
    expect(workbenchState.onDeleteWorkbook).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'wb-1' }),
    )
  })

  it('wires sheet select/create and page bindings for rename/delete', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    await wrapper.get('[data-testid="sheets-sheet-sheet-2"]').trigger('click')
    expect(workbenchState.onSelectSheet).toHaveBeenCalledWith('sheet-2')

    await wrapper.get('[data-testid="sheets-sheet-create"]').trigger('click')
    expect(workbenchState.onCreateSheet).toHaveBeenCalledTimes(1)

    const tabs = wrapper.getComponent(SheetsWorkbookTabs)
    tabs.vm.$emit('renameSheet', workbenchState.activeWorkbook.value!.sheets[0])
    tabs.vm.$emit('deleteSheet', workbenchState.activeWorkbook.value!.sheets[1])

    expect(workbenchState.onRenameSheet).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'sheet-1' }),
    )
    expect(workbenchState.onDeleteSheet).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'sheet-2' }),
    )
  })
})
