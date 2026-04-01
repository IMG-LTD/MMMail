import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SheetsPage from '../pages/sheets.vue'
import SheetsIncomingSharesPanel from '../components/sheets/SheetsIncomingSharesPanel.vue'
import SheetsShareManager from '../components/sheets/SheetsShareManager.vue'
import SheetsVersionHistoryDrawer from '../components/sheets/SheetsVersionHistoryDrawer.vue'
import type {
  SheetsIncomingShare,
  SheetsWorkbookDetail,
  SheetsWorkbookShare,
  SheetsWorkbookSummary,
  SheetsWorkbookVersion,
} from '../types/sheets'

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
  template: '<div class="el-alert-stub"><slot />{{ title }}</div>',
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
  template: '<button type="button" :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
})

const ElDrawer = defineComponent({
  name: 'ElDrawer',
  props: { modelValue: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  template: '<div v-if="modelValue" class="el-drawer-stub"><slot /></div>',
})

const ElEmpty = defineComponent({
  name: 'ElEmpty',
  props: { description: { type: String, default: '' } },
  template: '<div class="el-empty-stub">{{ description }}</div>',
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  template: `
    <label class="el-input-stub">
      <slot name="prepend" />
      <input
        :value="modelValue"
        :placeholder="placeholder"
        @input="$emit('update:modelValue', $event.target.value)"
      />
    </label>
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
    <select :value="modelValue" @change="$emit('update:modelValue', $event.target.value)">
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
  template: '<select :value="modelValue" :disabled="disabled" @change="onChange"><slot /></select>',
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
    supportedImportFormats: ['CSV'],
    supportedExportFormats: ['CSV'],
    createdAt: '2026-03-29T09:00:00',
    canManageShares: true,
    canRestoreVersions: true,
  }
}

function buildShare(): SheetsWorkbookShare {
  return {
    shareId: 'share-1',
    collaboratorUserId: 'user-2',
    collaboratorEmail: 'teammate@mmmail.local',
    collaboratorDisplayName: 'Teammate',
    permission: 'EDIT',
    responseStatus: 'NEEDS_ACTION',
    createdAt: '2026-03-29T09:20:00',
    updatedAt: '2026-03-29T09:25:00',
  }
}

function buildIncomingShares(): SheetsIncomingShare[] {
  return [
    {
      shareId: 'incoming-accept',
      workbookId: 'wb-shared-1',
      workbookTitle: 'Shared plan',
      ownerEmail: 'owner@mmmail.local',
      ownerDisplayName: 'Owner',
      permission: 'EDIT',
      responseStatus: 'NEEDS_ACTION',
      updatedAt: '2026-03-29T09:30:00',
    },
    {
      shareId: 'incoming-open',
      workbookId: 'wb-shared-2',
      workbookTitle: 'Accepted workbook',
      ownerEmail: 'owner@mmmail.local',
      ownerDisplayName: 'Owner',
      permission: 'VIEW',
      responseStatus: 'ACCEPTED',
      updatedAt: '2026-03-29T09:40:00',
    },
  ]
}

function buildVersions(): SheetsWorkbookVersion[] {
  return [{
    versionId: 'version-1',
    versionNo: 1,
    title: 'Snapshot 1',
    rowCount: 12,
    colCount: 6,
    createdByUserId: 'user-1',
    createdByEmail: 'owner@mmmail.local',
    createdByDisplayName: 'Owner',
    sourceEvent: 'SHEETS_WORKBOOK_UPDATE',
    createdAt: '2026-03-29T09:50:00',
  }]
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
  const incomingShares = buildIncomingShares()
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
    workbookEvents: ref([]),
    collaborationLoading: ref(false),
    collaborationError: ref(''),
    creatingTemplateCode: ref(''),
    templatePresets: ref([]),
    workspaceView: ref('INCOMING_SHARES'),
    scopeFilter: ref('ALL'),
    filteredWorkbooks: ref([buildWorkbookSummary()]),
    shares: ref([buildShare()]),
    incomingShares: ref(incomingShares),
    versions: ref(buildVersions()),
    inviteEmail: ref(''),
    invitePermission: ref<'VIEW' | 'EDIT'>('VIEW'),
    sharesLoading: ref(false),
    incomingLoading: ref(false),
    versionsLoading: ref(false),
    sharesErrorMessage: ref('Share manager unavailable'),
    incomingErrorMessage: ref('Incoming panel unavailable'),
    versionsErrorMessage: ref('Version history unavailable'),
    shareSubmitting: ref(false),
    shareMutationId: ref(''),
    incomingMutationId: ref(''),
    versionMutationId: ref(''),
    versionDrawerVisible: ref(true),
    pendingIncomingCount: ref(1),
    canManageShares: ref(true),
    canRestoreVersions: ref(true),
    ...createActionMocks(),
  }
}

function findButtonByText(
  wrapper: { findAll: (selector: string) => any[] },
  text: string,
) {
  const match = wrapper.findAll('button').find((button) => button.text().includes(text))
  if (!match) {
    throw new Error(`button not found: ${text}`)
  }
  return match
}

async function mountPage() {
  return mount(SheetsPage, {
    global: {
      components: {
        SheetsIncomingSharesPanel,
        SheetsShareManager,
        SheetsVersionHistoryDrawer,
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
        ElTag,
        SheetsWorkspaceHero: { template: '<div class="workspace-hero-stub" />' },
        SheetsWorkbookSidebar: { template: '<div class="workbook-sidebar-stub" />' },
        SheetsWorkbookTabs: { template: '<div class="workbook-tabs-stub" />' },
        SheetsDataToolsPanel: { template: '<div class="data-tools-stub" />' },
        SheetsFormulaPanel: { template: '<div class="formula-panel-stub" />' },
        SheetsGridEditor: { template: '<div class="grid-editor-stub" />' },
        SheetsCollaborationRail: { template: '<div class="collaboration-rail-stub" />' },
        SheetsImportExportPanel: { template: '<div class="import-export-panel-stub" />' },
        SheetsInsightRail: { template: '<div class="insight-rail-stub" />' },
      },
    },
  })
}

describe('sheets panels smoke', () => {
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

  it('shows visible errors and wires share plus incoming actions on the page', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    const shareManager = wrapper.getComponent(SheetsShareManager)
    const incomingPanel = wrapper.getComponent(SheetsIncomingSharesPanel)

    expect(shareManager.text()).toContain('Share manager unavailable')
    expect(incomingPanel.text()).toContain('Incoming panel unavailable')

    await shareManager.get('input').setValue('new-collaborator@mmmail.local')
    const shareSelects = shareManager.findAll('select')
    await shareSelects[0].setValue('EDIT')
    expect(workbenchState.inviteEmail.value).toBe('new-collaborator@mmmail.local')
    expect(workbenchState.invitePermission.value).toBe('EDIT')

    await findButtonByText(shareManager, 'sheets.share.sendInvite').trigger('click')
    expect(workbenchState.submitShare).toHaveBeenCalledTimes(1)

    await shareSelects[1].setValue('VIEW')
    expect(workbenchState.updateSharePermission).toHaveBeenCalledWith('share-1', 'VIEW')

    await findButtonByText(shareManager, 'sheets.share.revoke').trigger('click')
    expect(workbenchState.removeShare).toHaveBeenCalledWith('share-1')

    await findButtonByText(incomingPanel, 'common.actions.refresh').trigger('click')
    expect(workbenchState.refreshIncomingShares).toHaveBeenCalledTimes(1)

    await findButtonByText(incomingPanel, 'sheets.incoming.accept').trigger('click')
    expect(workbenchState.respondIncomingShare).toHaveBeenCalledWith('incoming-accept', 'ACCEPT')

    await findButtonByText(incomingPanel, 'sheets.incoming.open').trigger('click')
    expect(workbenchState.openIncomingWorkbook).toHaveBeenCalledWith(
      expect.objectContaining({ shareId: 'incoming-open' }),
    )
  })

  it('shows version history errors and wires restore action on the page', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    const versionDrawer = wrapper.getComponent(SheetsVersionHistoryDrawer)

    expect(versionDrawer.text()).toContain('Version history unavailable')
    await findButtonByText(versionDrawer, 'sheets.versions.restore').trigger('click')

    expect(workbenchState.restoreVersion).toHaveBeenCalledWith('version-1')
  })
})
