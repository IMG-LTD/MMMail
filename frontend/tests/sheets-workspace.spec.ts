import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { computed, defineComponent, reactive, ref } from 'vue'
import { useSheetsSharingVersionWorkbench } from '../composables/useSheetsSharingVersionWorkbench'
import { useSheetsWorkbench } from '../composables/useSheetsWorkbench'
import { useSheetsWorkspace } from '../composables/useSheetsWorkspace'
import type { SheetsWorkbookDetail, SheetsWorkbookSummary } from '../types/sheets'
import { SHEETS_TEMPLATE_PRESETS } from '../utils/sheets-collaboration'

const {
  routerReplaceMock,
  messageErrorMock,
  messageSuccessMock,
  confirmDiscardMock,
  requestWorkbookTitleMock,
  requestSheetNameMock,
  confirmDeleteWorkbookMock,
  confirmDeleteSheetMock,
  sheetsApiMock,
  suiteApiMock
} = vi.hoisted(() => ({
  routerReplaceMock: vi.fn(),
  messageErrorMock: vi.fn(),
  messageSuccessMock: vi.fn(),
  confirmDiscardMock: vi.fn(async () => true),
  requestWorkbookTitleMock: vi.fn(),
  requestSheetNameMock: vi.fn(),
  confirmDeleteWorkbookMock: vi.fn(),
  confirmDeleteSheetMock: vi.fn(),
  sheetsApiMock: {
    listWorkbooks: vi.fn(),
    createWorkbook: vi.fn(),
    importWorkbook: vi.fn(),
    getWorkbook: vi.fn(),
    exportWorkbook: vi.fn(),
    renameWorkbook: vi.fn(),
    createSheet: vi.fn(),
    renameSheet: vi.fn(),
    deleteSheet: vi.fn(),
    setActiveSheet: vi.fn(),
    updateWorkbookCells: vi.fn(),
    sortSheet: vi.fn(),
    freezeSheet: vi.fn(),
    deleteWorkbook: vi.fn(),
    listShares: vi.fn(),
    createShare: vi.fn(),
    updateShare: vi.fn(),
    removeShare: vi.fn(),
    listIncomingShares: vi.fn(),
    respondIncomingShare: vi.fn(),
    listVersions: vi.fn(),
    restoreVersion: vi.fn()
  },
  suiteApiMock: {
    getCollaborationCenter: vi.fn(),
  },
}))

const routeState = reactive({
  query: {} as Record<string, unknown>
})
let routeLeaveGuard: ((to?: unknown, from?: unknown) => Promise<boolean> | boolean) | null = null

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock
  }
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) => {
      if (!params) {
        return key
      }
      return Object.entries(params).reduce(
        (result, [paramKey, value]) => result.replace(`{${paramKey}}`, String(value)),
        key
      )
    }
  })
}))

vi.mock('~/composables/useSheetsApi', () => ({
  useSheetsApi: () => sheetsApiMock
}))

vi.mock('~/composables/useSuiteApi', () => ({
  useSuiteApi: () => suiteApiMock
}))

vi.mock('~/composables/useSheetsWorkspaceDialogs', () => ({
  useSheetsWorkspaceDialogs: () => ({
    requestWorkbookTitle: requestWorkbookTitleMock,
    requestSheetName: requestSheetNameMock,
    confirmDeleteWorkbook: confirmDeleteWorkbookMock,
    confirmDeleteSheet: confirmDeleteSheetMock,
    confirmDiscardChanges: confirmDiscardMock,
    buildDefaultWorkbookTitle: () => 'Workbook',
    resolveErrorMessage: (error: unknown, fallbackKey: string) => (error as Error)?.message || fallbackKey
  })
}))

function buildWorkbookSummary(
  workbookId: string,
  scope: 'OWNED' | 'SHARED' = 'OWNED'
): SheetsWorkbookSummary {
  return {
    id: workbookId,
    title: `Workbook ${workbookId}`,
    rowCount: 2,
    colCount: 2,
    filledCellCount: 1,
    formulaCellCount: 0,
    computedErrorCount: 0,
    currentVersion: 1,
    sheetCount: 1,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-29T10:00:00',
    lastOpenedAt: '2026-03-29T10:05:00',
    permission: scope === 'OWNED' ? 'OWNER' : 'EDIT',
    scope,
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 1,
    canEdit: true
  }
}

function buildWorkbookDetail(
  workbookId: string,
  scope: 'OWNED' | 'SHARED' = 'OWNED'
): SheetsWorkbookDetail {
  const summary = buildWorkbookSummary(workbookId, scope)
  return {
    ...summary,
    sheets: [
      {
        id: 'sheet-1',
        name: 'Summary',
        rowCount: 2,
        colCount: 2,
        frozenRowCount: 0,
        frozenColCount: 0,
        filledCellCount: 1,
        formulaCellCount: 0,
        computedErrorCount: 0,
        grid: [['A1', '']],
        computedGrid: [['A1', '']]
      }
    ],
    grid: [['A1', '']],
    computedGrid: [['A1', '']],
    supportedImportFormats: ['CSV'],
    supportedExportFormats: ['CSV'],
    createdAt: '2026-03-29T09:50:00',
    canManageShares: true,
    canRestoreVersions: true
  }
}

function buildVersions() {
  return [{
    versionId: 'version-1',
    versionNo: 1,
    title: 'Snapshot 1',
    rowCount: 2,
    colCount: 2,
    createdByUserId: 'user-1',
    createdByEmail: 'owner@mmmail.local',
    createdByDisplayName: 'Owner',
    sourceEvent: 'SHEETS_WORKBOOK_UPDATE' as const,
    createdAt: '2026-03-29T09:50:00',
  }]
}

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((nextResolve, nextReject) => {
    resolve = nextResolve
    reject = nextReject
  })
  return { promise, resolve, reject }
}

async function mountComposable<T>(factory: () => T): Promise<{ result: T; unmount: () => void }> {
  let result: T | null = null
  const wrapper = mount(defineComponent({
    setup() {
      result = factory()
      return () => null
    }
  }))
  await flushPromises()
  if (!result) {
    throw new Error('composable did not initialize')
  }
  return {
    result,
    unmount: () => wrapper.unmount()
  }
}

describe('sheets workspace runtime', () => {
  beforeEach(() => {
    routeLeaveGuard = null
    routeState.query = {
      workbookId: 'wb-1',
      view: 'INCOMING_SHARES',
      scope: 'SHARED',
      panel: 'meta'
    }
    routerReplaceMock.mockReset()
    routerReplaceMock.mockImplementation(async (location: { query?: Record<string, unknown> }) => {
      routeState.query = { ...(location.query ?? {}) }
    })
    messageErrorMock.mockReset()
    messageSuccessMock.mockReset()
    confirmDiscardMock.mockReset()
    confirmDiscardMock.mockResolvedValue(true)
    requestWorkbookTitleMock.mockReset()
    requestSheetNameMock.mockReset()
    confirmDeleteWorkbookMock.mockReset()
    confirmDeleteSheetMock.mockReset()
    Object.values(sheetsApiMock).forEach((mockFn) => mockFn.mockReset())
    Object.values(suiteApiMock).forEach((mockFn) => mockFn.mockReset())
    sheetsApiMock.listWorkbooks.mockResolvedValue([
      buildWorkbookSummary('wb-1'),
      buildWorkbookSummary('wb-2', 'SHARED')
    ])
    sheetsApiMock.getWorkbook.mockImplementation(async (workbookId: string) => {
      return buildWorkbookDetail(workbookId, workbookId === 'wb-2' ? 'SHARED' : 'OWNED')
    })
    sheetsApiMock.listShares.mockResolvedValue([])
    sheetsApiMock.listIncomingShares.mockResolvedValue([])
    suiteApiMock.getCollaborationCenter.mockResolvedValue({ items: [] })
    vi.stubGlobal('useRoute', () => routeState)
    vi.stubGlobal('useRouter', () => ({ replace: routerReplaceMock }))
    vi.stubGlobal('onBeforeRouteLeave', (guard: typeof routeLeaveGuard) => {
      routeLeaveGuard = guard
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('preserves view and scope when syncing workbook selection', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    routerReplaceMock.mockClear()

    await workspace.selectWorkbook('wb-2', true)

    expect(routerReplaceMock).toHaveBeenCalledWith({
      path: '/sheets',
      query: {
        workbookId: 'wb-2',
        view: 'INCOMING_SHARES',
        scope: 'SHARED',
        panel: 'meta'
      }
    })
    mounted.unmount()
  })

  it('blocks refresh on dirty edits until discard is confirmed, then reloads detail', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    sheetsApiMock.listWorkbooks.mockClear()
    sheetsApiMock.getWorkbook.mockClear()
    messageSuccessMock.mockClear()

    await workspace.selectWorkbook('wb-2', true)
    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })

    confirmDiscardMock.mockResolvedValueOnce(false)
    await workspace.onRefreshWorkspace()

    expect(sheetsApiMock.listWorkbooks).not.toHaveBeenCalled()
    expect(workspace.dirtyCount.value).toBe(1)

    confirmDiscardMock.mockResolvedValueOnce(true)
    await workspace.onRefreshWorkspace()

    expect(sheetsApiMock.listWorkbooks).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.getWorkbook).toHaveBeenLastCalledWith('wb-2')
    expect(sheetsApiMock.getWorkbook.mock.calls.length).toBeGreaterThanOrEqual(2)
    expect(workspace.dirtyCount.value).toBe(0)
    expect(messageSuccessMock).toHaveBeenCalledWith('sheets.messages.workspaceRefreshed')
    expect(confirmDiscardMock).toHaveBeenCalledTimes(2)
    mounted.unmount()
  })

  it('blocks same workbook reload when dirty edits are kept', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    sheetsApiMock.getWorkbook.mockClear()

    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })
    confirmDiscardMock.mockResolvedValueOnce(false)

    const selected = await workspace.selectWorkbook('wb-1', false)

    expect(selected).toBe(false)
    expect(confirmDiscardMock).toHaveBeenCalledWith('Summary')
    expect(sheetsApiMock.getWorkbook).not.toHaveBeenCalled()
    expect(workspace.dirtyCount.value).toBe(1)
    mounted.unmount()
  })

  it('guards browser unload and route leave when workbook has unsaved edits', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result

    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })

    expect(routeLeaveGuard).not.toBeNull()

    const blockedEvent = new Event('beforeunload', { cancelable: true }) as BeforeUnloadEvent & {
      returnValue: string
    }
    Object.defineProperty(blockedEvent, 'returnValue', {
      configurable: true,
      writable: true,
      value: ''
    })

    window.dispatchEvent(blockedEvent)

    expect(blockedEvent.defaultPrevented).toBe(true)
    expect(blockedEvent.returnValue).toBe('')

    confirmDiscardMock.mockResolvedValueOnce(false)
    expect(await routeLeaveGuard?.({ path: '/inbox' }, { path: '/sheets' })).toBe(false)
    expect(confirmDiscardMock).toHaveBeenCalledWith('Summary')

    confirmDiscardMock.mockResolvedValueOnce(true)
    expect(await routeLeaveGuard?.({ path: '/inbox' }, { path: '/sheets' })).toBe(true)

    mounted.unmount()

    const cleanEvent = new Event('beforeunload', { cancelable: true }) as BeforeUnloadEvent & {
      returnValue: string
    }
    Object.defineProperty(cleanEvent, 'returnValue', {
      configurable: true,
      writable: true,
      value: ''
    })

    window.dispatchEvent(cleanEvent)
    expect(cleanEvent.defaultPrevented).toBe(false)
  })

  it('preserves dirty edits when renaming the active workbook', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    requestWorkbookTitleMock.mockResolvedValueOnce('Renamed workbook')
    sheetsApiMock.renameWorkbook.mockResolvedValue({
      ...buildWorkbookDetail('wb-1'),
      title: 'Renamed workbook',
    })

    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })

    await workspace.onRenameWorkbook(buildWorkbookSummary('wb-1'))

    expect(sheetsApiMock.renameWorkbook).toHaveBeenCalledWith('wb-1', { title: 'Renamed workbook' })
    expect(workspace.activeWorkbook.value?.title).toBe('Renamed workbook')
    expect(workspace.localGrid.value[0]?.[0]).toBe('Changed value')
    expect(workspace.dirtyCount.value).toBe(1)
    mounted.unmount()
  })

  it('preserves dirty edits when renaming the active sheet', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    requestSheetNameMock.mockResolvedValueOnce('Overview')
    sheetsApiMock.renameSheet.mockResolvedValue({
      ...buildWorkbookDetail('wb-1'),
      sheets: [{
        ...buildWorkbookDetail('wb-1').sheets[0],
        name: 'Overview',
      }],
    })

    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })

    await workspace.onRenameSheet(buildWorkbookDetail('wb-1').sheets[0])

    expect(sheetsApiMock.renameSheet).toHaveBeenCalledWith('wb-1', 'sheet-1', { name: 'Overview' })
    expect(workspace.activeWorkbook.value?.sheets[0]?.name).toBe('Overview')
    expect(workspace.localGrid.value[0]?.[0]).toBe('Changed value')
    expect(workspace.dirtyCount.value).toBe(1)
    mounted.unmount()
  })

  it('blocks deleting the active workbook when dirty edits are kept', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    confirmDeleteWorkbookMock.mockResolvedValueOnce(true)

    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })
    confirmDiscardMock.mockResolvedValueOnce(false)
    await workspace.onDeleteWorkbook(buildWorkbookSummary('wb-1'))

    expect(confirmDeleteWorkbookMock).toHaveBeenCalledWith('Workbook wb-1')
    expect(confirmDiscardMock).toHaveBeenCalledWith('Summary')
    expect(sheetsApiMock.deleteWorkbook).not.toHaveBeenCalled()
    expect(workspace.dirtyCount.value).toBe(1)
    mounted.unmount()
  })

  it('confirms discard only once when deleting the active workbook and switching to next workbook', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    confirmDeleteWorkbookMock.mockResolvedValueOnce(true)
    confirmDiscardMock.mockResolvedValueOnce(true)
    sheetsApiMock.deleteWorkbook.mockResolvedValue(undefined)
    sheetsApiMock.getWorkbook.mockImplementation(async (workbookId: string) => {
      return buildWorkbookDetail(workbookId, workbookId === 'wb-2' ? 'SHARED' : 'OWNED')
    })

    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })
    await workspace.onDeleteWorkbook(buildWorkbookSummary('wb-1'))

    expect(confirmDiscardMock).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.deleteWorkbook).toHaveBeenCalledWith('wb-1')
    expect(sheetsApiMock.getWorkbook).toHaveBeenLastCalledWith('wb-2')
    expect(workspace.activeWorkbook.value?.id).toBe('wb-2')
    mounted.unmount()
  })

  it('deletes an inactive workbook without forcing discard on current dirty edits', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    confirmDeleteWorkbookMock.mockResolvedValueOnce(true)
    sheetsApiMock.deleteWorkbook.mockResolvedValue(undefined)

    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })
    await workspace.onDeleteWorkbook(buildWorkbookSummary('wb-2'))

    expect(confirmDiscardMock).not.toHaveBeenCalled()
    expect(sheetsApiMock.deleteWorkbook).toHaveBeenCalledWith('wb-2')
    expect(workspace.dirtyCount.value).toBe(1)
    mounted.unmount()
  })

  it('blocks deleting a sheet when dirty edits are kept', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    confirmDeleteSheetMock.mockResolvedValueOnce(true)

    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })
    confirmDiscardMock.mockResolvedValueOnce(false)
    await workspace.onDeleteSheet(buildWorkbookDetail('wb-1').sheets[0])

    expect(confirmDeleteSheetMock).toHaveBeenCalledWith('Summary')
    expect(confirmDiscardMock).toHaveBeenCalledWith('Summary')
    expect(sheetsApiMock.deleteSheet).not.toHaveBeenCalled()
    expect(workspace.dirtyCount.value).toBe(1)
    mounted.unmount()
  })
})

describe('sheets sharing/version route sync', () => {
  beforeEach(() => {
    routeState.query = {
      workbookId: 'wb-2',
      view: 'INCOMING_SHARES',
      scope: 'SHARED',
      panel: 'meta'
    }
    routerReplaceMock.mockReset()
    routerReplaceMock.mockImplementation(async (location: { query?: Record<string, unknown> }) => {
      routeState.query = { ...(location.query ?? {}) }
    })
    Object.values(sheetsApiMock).forEach((mockFn) => mockFn.mockReset())
    sheetsApiMock.listShares.mockResolvedValue([])
    sheetsApiMock.listIncomingShares.mockResolvedValue([])
    vi.stubGlobal('useRoute', () => routeState)
    vi.stubGlobal('useRouter', () => ({ replace: routerReplaceMock }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('restores view and scope from deep links and writes them back to route query', async () => {
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-2', 'SHARED')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-2', 'SHARED')),
      activeWorkbookId: computed(() => 'wb-2'),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result

    expect(sharingVersion.workspaceView.value).toBe('INCOMING_SHARES')
    expect(sharingVersion.scopeFilter.value).toBe('SHARED')

    sharingVersion.workspaceView.value = 'WORKBOOKS'
    sharingVersion.scopeFilter.value = 'ALL'
    await flushPromises()

    expect(routerReplaceMock.mock.calls.some(([location]) => {
      return location.path === '/sheets'
        && location.query?.workbookId === 'wb-2'
        && location.query?.panel === 'meta'
        && !('view' in (location.query ?? {}))
        && !('scope' in (location.query ?? {}))
    })).toBe(true)

    routeState.query = {
      workbookId: 'wb-2',
      view: 'INCOMING_SHARES',
      scope: 'OWNED',
      panel: 'meta'
    }
    await flushPromises()

    expect(sharingVersion.workspaceView.value).toBe('INCOMING_SHARES')
    expect(sharingVersion.scopeFilter.value).toBe('OWNED')
    mounted.unmount()
  })

  it('keeps route state unchanged when incoming workbook open is canceled', async () => {
    routeState.query = {
      workbookId: 'wb-1',
      view: 'INCOMING_SHARES',
      scope: 'OWNED',
      panel: 'meta'
    }
    const selectWorkbook = vi.fn(async () => false)
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2', 'SHARED')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result
    routerReplaceMock.mockClear()

    await sharingVersion.openIncomingWorkbook({
      shareId: 'incoming-open',
      workbookId: 'wb-2',
      workbookTitle: 'Ops board',
      ownerEmail: 'ops@mmmail.local',
      ownerDisplayName: 'Ops',
      permission: 'VIEW',
      responseStatus: 'ACCEPTED',
      updatedAt: '2026-03-30T11:50:00',
    })

    expect(selectWorkbook).toHaveBeenCalledWith('wb-2', false)
    expect(sharingVersion.workspaceView.value).toBe('INCOMING_SHARES')
    expect(sharingVersion.scopeFilter.value).toBe('OWNED')
    expect(routerReplaceMock).not.toHaveBeenCalled()
    mounted.unmount()
  })

  it('blocks version restore before server mutation when dirty edits are kept', async () => {
    const selectWorkbook = vi.fn(async () => true)
    const confirmDiscard = vi.fn(async () => false)
    sheetsApiMock.restoreVersion.mockResolvedValue(buildWorkbookDetail('wb-1'))
    sheetsApiMock.listVersions.mockResolvedValue(buildVersions())
    const refreshCollaboration = vi.fn(async () => undefined)
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: confirmDiscard
    }))
    const sharingVersion = mounted.result

    await sharingVersion.restoreVersion('version-1')

    expect(confirmDiscard).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.restoreVersion).not.toHaveBeenCalled()
    expect(selectWorkbook).not.toHaveBeenCalled()
    expect(refreshCollaboration).not.toHaveBeenCalled()
    mounted.unmount()
  })

  it('skips duplicate discard confirm after version restore is already confirmed', async () => {
    const selectWorkbook = vi.fn(async () => true)
    const confirmDiscard = vi.fn(async () => true)
    const refreshCollaboration = vi.fn(async () => undefined)
    sheetsApiMock.restoreVersion.mockResolvedValue(buildWorkbookDetail('wb-1'))
    sheetsApiMock.listVersions.mockResolvedValue(buildVersions())
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: confirmDiscard
    }))
    const sharingVersion = mounted.result

    await sharingVersion.restoreVersion('version-1')

    expect(confirmDiscard).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.restoreVersion).toHaveBeenCalledWith('wb-1', 'version-1')
    expect(selectWorkbook).toHaveBeenCalledWith('wb-1', false, { skipDiscardConfirm: true })
    expect(refreshCollaboration).toHaveBeenCalledTimes(1)
    mounted.unmount()
  })

  it('does not misreport restore failed when workbook reload after restore is canceled', async () => {
    const selectWorkbook = vi.fn(async () => false)
    const confirmDiscard = vi.fn(async () => true)
    const refreshCollaboration = vi.fn(async () => undefined)
    sheetsApiMock.restoreVersion.mockResolvedValue(buildWorkbookDetail('wb-1'))
    sheetsApiMock.listVersions.mockResolvedValue(buildVersions())
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: confirmDiscard
    }))
    const sharingVersion = mounted.result
    messageErrorMock.mockClear()
    messageSuccessMock.mockClear()

    await sharingVersion.restoreVersion('version-1')

    expect(sheetsApiMock.restoreVersion).toHaveBeenCalledWith('wb-1', 'version-1')
    expect(selectWorkbook).toHaveBeenCalledWith('wb-1', false, { skipDiscardConfirm: true })
    expect(refreshCollaboration).not.toHaveBeenCalled()
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.versionRestored')
    expect(messageErrorMock).not.toHaveBeenCalledWith('sheets.messages.versionRestoreFailed')
    mounted.unmount()
  })

  it('does not misreport restore failed when version list refresh fails after restore', async () => {
    const selectWorkbook = vi.fn(async () => true)
    const confirmDiscard = vi.fn(async () => true)
    const refreshCollaboration = vi.fn(async () => undefined)
    sheetsApiMock.restoreVersion.mockResolvedValue(buildWorkbookDetail('wb-1'))
    sheetsApiMock.listVersions.mockRejectedValueOnce(new Error('version list failed'))
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: confirmDiscard
    }))
    const sharingVersion = mounted.result
    messageErrorMock.mockClear()
    messageSuccessMock.mockClear()

    await sharingVersion.restoreVersion('version-1')

    expect(sheetsApiMock.restoreVersion).toHaveBeenCalledWith('wb-1', 'version-1')
    expect(refreshCollaboration).not.toHaveBeenCalled()
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.versionRestored')
    expect(messageErrorMock).toHaveBeenCalledWith('version list failed')
    expect(messageErrorMock).not.toHaveBeenCalledWith('sheets.messages.versionRestoreFailed')
    mounted.unmount()
  })

  it('resets version drawer context when active workbook changes', async () => {
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    sheetsApiMock.listVersions.mockResolvedValue(buildVersions())
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook,
      activeWorkbookId: computed(() => activeWorkbook.value.id),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result

    await sharingVersion.openVersionHistory()
    expect(sharingVersion.versionDrawerVisible.value).toBe(true)
    expect(sharingVersion.versions.value).toEqual(buildVersions())

    activeWorkbook.value = buildWorkbookDetail('wb-2')
    await flushPromises()

    expect(sharingVersion.versionDrawerVisible.value).toBe(false)
    expect(sharingVersion.versions.value).toEqual([])
    expect(sharingVersion.versionsErrorMessage.value).toBe('')
    mounted.unmount()
  })

  it('ignores stale version refresh results after workbook changes', async () => {
    const staleVersionRequest = createDeferred<ReturnType<typeof buildVersions>>()
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    sheetsApiMock.listVersions.mockImplementation((workbookId: string) => {
      if (workbookId === 'wb-1') {
        return staleVersionRequest.promise
      }
      return Promise.resolve([])
    })
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook,
      activeWorkbookId: computed(() => activeWorkbook.value.id),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result

    const openVersionHistoryPromise = sharingVersion.openVersionHistory()
    await flushPromises()
    activeWorkbook.value = buildWorkbookDetail('wb-2')
    await flushPromises()
    staleVersionRequest.resolve(buildVersions())
    await flushPromises()
    await openVersionHistoryPromise

    expect(sharingVersion.versions.value).toEqual([])
    expect(sharingVersion.versionsLoading.value).toBe(false)
    expect(sharingVersion.versionDrawerVisible.value).toBe(false)
    mounted.unmount()
  })

  it('ignores stale version restore follow-through after workbook changes', async () => {
    const restoreVersionRequest = createDeferred<SheetsWorkbookDetail>()
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    const selectWorkbook = vi.fn(async () => true)
    const refreshCollaboration = vi.fn(async () => undefined)
    sheetsApiMock.restoreVersion.mockImplementation(() => restoreVersionRequest.promise)
    sheetsApiMock.listVersions.mockResolvedValue(buildVersions())
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook,
      activeWorkbookId: computed(() => activeWorkbook.value.id),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result
    messageSuccessMock.mockClear()

    const restorePromise = sharingVersion.restoreVersion('version-1')
    activeWorkbook.value = buildWorkbookDetail('wb-2')
    await flushPromises()
    restoreVersionRequest.resolve(buildWorkbookDetail('wb-1'))
    await restorePromise
    await flushPromises()

    expect(selectWorkbook).not.toHaveBeenCalled()
    expect(refreshCollaboration).not.toHaveBeenCalled()
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.versionRestored')
    mounted.unmount()
  })

  it('syncs collaborator metadata after share creation without reloading workbook', async () => {
    routeState.query = {
      workbookId: 'wb-1',
      view: 'WORKBOOKS',
      scope: 'ALL',
      panel: 'meta'
    }
    const workbooks = ref([buildWorkbookSummary('wb-1')])
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    const selectWorkbook = vi.fn(async () => true)
    const refreshCollaboration = vi.fn(async () => undefined)
    sheetsApiMock.createShare.mockResolvedValue({
      shareId: 'share-2',
      collaboratorUserId: 'user-3',
      collaboratorEmail: 'new@mmmail.local',
      collaboratorDisplayName: 'New user',
      permission: 'EDIT',
      responseStatus: 'NEEDS_ACTION',
      createdAt: '2026-03-30T10:00:00',
      updatedAt: '2026-03-30T10:00:00',
    })
    sheetsApiMock.listShares.mockResolvedValue([
      {
        shareId: 'share-1',
        collaboratorUserId: 'user-2',
        collaboratorEmail: 'teammate@mmmail.local',
        collaboratorDisplayName: 'Teammate',
        permission: 'EDIT',
        responseStatus: 'NEEDS_ACTION',
        createdAt: '2026-03-29T09:20:00',
        updatedAt: '2026-03-29T09:25:00',
      },
      {
        shareId: 'share-2',
        collaboratorUserId: 'user-3',
        collaboratorEmail: 'new@mmmail.local',
        collaboratorDisplayName: 'New user',
        permission: 'EDIT',
        responseStatus: 'NEEDS_ACTION',
        createdAt: '2026-03-30T10:00:00',
        updatedAt: '2026-03-30T10:00:00',
      }
    ])
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks,
      activeWorkbook,
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result

    sharingVersion.inviteEmail.value = 'new@mmmail.local'
    sharingVersion.invitePermission.value = 'EDIT'
    await sharingVersion.submitShare()

    expect(selectWorkbook).not.toHaveBeenCalled()
    expect(sharingVersion.shares.value).toHaveLength(2)
    expect(sharingVersion.inviteEmail.value).toBe('')
    expect(sharingVersion.invitePermission.value).toBe('VIEW')
    expect(activeWorkbook.value.collaboratorCount).toBe(2)
    expect(workbooks.value[0]?.collaboratorCount).toBe(2)
    expect(messageSuccessMock).toHaveBeenCalledWith('sheets.messages.shareCreated')
    mounted.unmount()
  })

  it('ignores stale share create follow-through after workbook changes', async () => {
    const createShareRequest = createDeferred<{
      shareId: string
      collaboratorUserId: string
      collaboratorEmail: string
      collaboratorDisplayName: string
      permission: 'EDIT'
      responseStatus: 'NEEDS_ACTION'
      createdAt: string
      updatedAt: string
    }>()
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    sheetsApiMock.createShare.mockImplementation(() => createShareRequest.promise)
    sheetsApiMock.listShares.mockResolvedValue([])
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook,
      activeWorkbookId: computed(() => activeWorkbook.value.id),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result
    messageSuccessMock.mockClear()

    sharingVersion.inviteEmail.value = 'old@mmmail.local'
    sharingVersion.invitePermission.value = 'EDIT'
    const submitPromise = sharingVersion.submitShare()
    activeWorkbook.value = buildWorkbookDetail('wb-2')
    await flushPromises()
    sheetsApiMock.listShares.mockClear()
    sharingVersion.inviteEmail.value = 'fresh@mmmail.local'
    sharingVersion.invitePermission.value = 'EDIT'

    createShareRequest.resolve({
      shareId: 'share-2',
      collaboratorUserId: 'user-3',
      collaboratorEmail: 'old@mmmail.local',
      collaboratorDisplayName: 'Old',
      permission: 'EDIT',
      responseStatus: 'NEEDS_ACTION',
      createdAt: '2026-03-30T10:00:00',
      updatedAt: '2026-03-30T10:00:00',
    })
    await submitPromise
    await flushPromises()

    expect(sheetsApiMock.listShares).not.toHaveBeenCalled()
    expect(sharingVersion.inviteEmail.value).toBe('fresh@mmmail.local')
    expect(sharingVersion.invitePermission.value).toBe('EDIT')
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.shareCreated')
    mounted.unmount()
  })

  it('does not fake share creation success when share refresh fails', async () => {
    routeState.query = {
      workbookId: 'wb-1',
      view: 'WORKBOOKS',
      scope: 'ALL',
      panel: 'meta'
    }
    const workbooks = ref([{
      ...buildWorkbookSummary('wb-1'),
      collaboratorCount: 1,
    }])
    const activeWorkbook = ref({
      ...buildWorkbookDetail('wb-1'),
      collaboratorCount: 1,
    })
    const refreshCollaboration = vi.fn(async () => undefined)
    sheetsApiMock.createShare.mockResolvedValue({
      shareId: 'share-2',
      collaboratorUserId: 'user-3',
      collaboratorEmail: 'new@mmmail.local',
      collaboratorDisplayName: 'New user',
      permission: 'EDIT',
      responseStatus: 'NEEDS_ACTION',
      createdAt: '2026-03-30T10:00:00',
      updatedAt: '2026-03-30T10:00:00',
    })
    sheetsApiMock.listShares
      .mockResolvedValueOnce([
        {
          shareId: 'share-1',
          collaboratorUserId: 'user-2',
          collaboratorEmail: 'teammate@mmmail.local',
          collaboratorDisplayName: 'Teammate',
          permission: 'EDIT',
          responseStatus: 'NEEDS_ACTION',
          createdAt: '2026-03-29T09:20:00',
          updatedAt: '2026-03-29T09:25:00',
        }
      ])
      .mockRejectedValueOnce(new Error('share refresh failed'))
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks,
      activeWorkbook,
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result
    messageErrorMock.mockClear()
    messageSuccessMock.mockClear()

    sharingVersion.inviteEmail.value = 'new@mmmail.local'
    sharingVersion.invitePermission.value = 'EDIT'
    await sharingVersion.submitShare()

    expect(sharingVersion.inviteEmail.value).toBe('new@mmmail.local')
    expect(sharingVersion.invitePermission.value).toBe('EDIT')
    expect(activeWorkbook.value.collaboratorCount).toBe(1)
    expect(workbooks.value[0]?.collaboratorCount).toBe(1)
    expect(refreshCollaboration).not.toHaveBeenCalled()
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.shareCreated')
    expect(messageErrorMock).toHaveBeenCalledWith('share refresh failed')
    mounted.unmount()
  })

  it('syncs collaborator metadata after share removal without reloading workbook', async () => {
    routeState.query = {
      workbookId: 'wb-1',
      view: 'WORKBOOKS',
      scope: 'ALL',
      panel: 'meta'
    }
    const workbooks = ref([buildWorkbookSummary('wb-1')])
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    activeWorkbook.value = {
      ...activeWorkbook.value,
      collaboratorCount: 2,
    }
    workbooks.value = [{
      ...workbooks.value[0],
      collaboratorCount: 2,
    }]
    const selectWorkbook = vi.fn(async () => true)
    const refreshCollaboration = vi.fn(async () => undefined)
    sheetsApiMock.removeShare.mockResolvedValue(undefined)
    sheetsApiMock.listShares.mockResolvedValue([])
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks,
      activeWorkbook,
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result

    await sharingVersion.removeShare('share-1')

    expect(selectWorkbook).not.toHaveBeenCalled()
    expect(activeWorkbook.value.collaboratorCount).toBe(0)
    expect(workbooks.value[0]?.collaboratorCount).toBe(0)
    expect(messageSuccessMock).toHaveBeenCalledWith('sheets.messages.shareRemoved')
    mounted.unmount()
  })

  it('does not fake share removal success when share refresh fails', async () => {
    routeState.query = {
      workbookId: 'wb-1',
      view: 'WORKBOOKS',
      scope: 'ALL',
      panel: 'meta'
    }
    const workbooks = ref([{
      ...buildWorkbookSummary('wb-1'),
      collaboratorCount: 2,
    }])
    const activeWorkbook = ref({
      ...buildWorkbookDetail('wb-1'),
      collaboratorCount: 2,
    })
    const refreshCollaboration = vi.fn(async () => undefined)
    sheetsApiMock.removeShare.mockResolvedValue(undefined)
    sheetsApiMock.listShares
      .mockResolvedValueOnce([
        {
          shareId: 'share-1',
          collaboratorUserId: 'user-2',
          collaboratorEmail: 'teammate@mmmail.local',
          collaboratorDisplayName: 'Teammate',
          permission: 'EDIT',
          responseStatus: 'NEEDS_ACTION',
          createdAt: '2026-03-29T09:20:00',
          updatedAt: '2026-03-29T09:25:00',
        },
        {
          shareId: 'share-2',
          collaboratorUserId: 'user-3',
          collaboratorEmail: 'new@mmmail.local',
          collaboratorDisplayName: 'New user',
          permission: 'EDIT',
          responseStatus: 'NEEDS_ACTION',
          createdAt: '2026-03-30T10:00:00',
          updatedAt: '2026-03-30T10:00:00',
        }
      ])
      .mockRejectedValueOnce(new Error('share refresh failed'))
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks,
      activeWorkbook,
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result
    messageErrorMock.mockClear()
    messageSuccessMock.mockClear()

    await sharingVersion.removeShare('share-1')

    expect(activeWorkbook.value.collaboratorCount).toBe(2)
    expect(workbooks.value[0]?.collaboratorCount).toBe(2)
    expect(refreshCollaboration).not.toHaveBeenCalled()
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.shareRemoved')
    expect(messageErrorMock).toHaveBeenCalledWith('share refresh failed')
    mounted.unmount()
  })

  it('preserves existing share list when share refresh fails', async () => {
    routeState.query = {
      workbookId: 'wb-1',
      view: 'WORKBOOKS',
      scope: 'ALL',
      panel: 'meta'
    }
    const existingShares = [
      {
        shareId: 'share-1',
        collaboratorUserId: 'user-2',
        collaboratorEmail: 'teammate@mmmail.local',
        collaboratorDisplayName: 'Teammate',
        permission: 'EDIT' as const,
        responseStatus: 'NEEDS_ACTION' as const,
        createdAt: '2026-03-29T09:20:00',
        updatedAt: '2026-03-29T09:25:00',
      }
    ]
    sheetsApiMock.listShares
      .mockResolvedValueOnce(existingShares)
      .mockRejectedValueOnce(new Error('share refresh failed'))
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result
    messageErrorMock.mockClear()

    expect(sharingVersion.shares.value).toEqual(existingShares)

    await sharingVersion.refreshShares()

    expect(sharingVersion.shares.value).toEqual(existingShares)
    expect(messageErrorMock).toHaveBeenCalledWith('share refresh failed')
    mounted.unmount()
  })

  it('resets share context when active workbook changes', async () => {
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    const existingShares = [
      {
        shareId: 'share-1',
        collaboratorUserId: 'user-2',
        collaboratorEmail: 'teammate@mmmail.local',
        collaboratorDisplayName: 'Teammate',
        permission: 'EDIT' as const,
        responseStatus: 'NEEDS_ACTION' as const,
        createdAt: '2026-03-29T09:20:00',
        updatedAt: '2026-03-29T09:25:00',
      }
    ]
    sheetsApiMock.listShares
      .mockResolvedValueOnce(existingShares)
      .mockRejectedValueOnce(new Error('share refresh failed'))
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook,
      activeWorkbookId: computed(() => activeWorkbook.value.id),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result
    messageErrorMock.mockClear()

    sharingVersion.inviteEmail.value = 'draft@mmmail.local'
    sharingVersion.invitePermission.value = 'EDIT'
    expect(sharingVersion.shares.value).toEqual(existingShares)

    activeWorkbook.value = buildWorkbookDetail('wb-2')
    await flushPromises()

    expect(sharingVersion.shares.value).toEqual([])
    expect(sharingVersion.inviteEmail.value).toBe('')
    expect(sharingVersion.invitePermission.value).toBe('VIEW')
    expect(messageErrorMock).toHaveBeenCalledWith('share refresh failed')
    mounted.unmount()
  })

  it('ignores stale share refresh results after workbook changes', async () => {
    const staleShareRequest = createDeferred<Array<{
      shareId: string
      collaboratorUserId: string
      collaboratorEmail: string
      collaboratorDisplayName: string
      permission: 'EDIT'
      responseStatus: 'NEEDS_ACTION'
      createdAt: string
      updatedAt: string
    }>>()
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    sheetsApiMock.listShares.mockImplementation((workbookId: string) => {
      if (workbookId === 'wb-1') {
        return staleShareRequest.promise
      }
      return Promise.resolve([])
    })
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook,
      activeWorkbookId: computed(() => activeWorkbook.value.id),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result

    activeWorkbook.value = buildWorkbookDetail('wb-2')
    await flushPromises()
    staleShareRequest.resolve([
      {
        shareId: 'share-stale',
        collaboratorUserId: 'user-stale',
        collaboratorEmail: 'stale@mmmail.local',
        collaboratorDisplayName: 'Stale',
        permission: 'EDIT',
        responseStatus: 'NEEDS_ACTION',
        createdAt: '2026-03-29T09:20:00',
        updatedAt: '2026-03-29T09:25:00',
      }
    ])
    await flushPromises()

    expect(sharingVersion.shares.value).toEqual([])
    expect(sharingVersion.sharesLoading.value).toBe(false)
    mounted.unmount()
  })

  it('does not fake incoming response success when incoming refresh fails', async () => {
    const refreshCollaboration = vi.fn(async () => undefined)
    const selectWorkbook = vi.fn(async () => true)
    sheetsApiMock.respondIncomingShare.mockResolvedValue({
      shareId: 'incoming-1',
      workbookId: 'wb-2',
      workbookTitle: 'Ops board',
      ownerEmail: 'ops@mmmail.local',
      ownerDisplayName: 'Ops',
      permission: 'VIEW',
      responseStatus: 'ACCEPTED',
      updatedAt: '2026-03-30T11:50:00',
    })
    sheetsApiMock.listIncomingShares
      .mockResolvedValueOnce([
        {
          shareId: 'incoming-1',
          workbookId: 'wb-2',
          workbookTitle: 'Ops board',
          ownerEmail: 'ops@mmmail.local',
          ownerDisplayName: 'Ops',
          permission: 'VIEW',
          responseStatus: 'NEEDS_ACTION',
          updatedAt: '2026-03-30T11:40:00',
        }
      ])
      .mockRejectedValueOnce(new Error('incoming refresh failed'))
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result
    messageErrorMock.mockClear()
    messageSuccessMock.mockClear()
    sheetsApiMock.listWorkbooks.mockClear()

    await sharingVersion.respondIncomingShare('incoming-1', 'ACCEPT')

    expect(refreshCollaboration).not.toHaveBeenCalled()
    expect(selectWorkbook).not.toHaveBeenCalled()
    expect(sheetsApiMock.listWorkbooks).not.toHaveBeenCalled()
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.shareResponseUpdated')
    expect(messageErrorMock).toHaveBeenCalledWith('incoming refresh failed')
    mounted.unmount()
  })

  it('does not fake incoming response success when visible workbook refresh fails', async () => {
    const refreshCollaboration = vi.fn(async () => undefined)
    const selectWorkbook = vi.fn(async () => true)
    sheetsApiMock.respondIncomingShare.mockResolvedValue({
      shareId: 'incoming-1',
      workbookId: 'wb-2',
      workbookTitle: 'Ops board',
      ownerEmail: 'ops@mmmail.local',
      ownerDisplayName: 'Ops',
      permission: 'VIEW',
      responseStatus: 'ACCEPTED',
      updatedAt: '2026-03-30T11:50:00',
    })
    sheetsApiMock.listIncomingShares
      .mockResolvedValueOnce([
        {
          shareId: 'incoming-1',
          workbookId: 'wb-2',
          workbookTitle: 'Ops board',
          ownerEmail: 'ops@mmmail.local',
          ownerDisplayName: 'Ops',
          permission: 'VIEW',
          responseStatus: 'NEEDS_ACTION',
          updatedAt: '2026-03-30T11:40:00',
        }
      ])
      .mockResolvedValueOnce([])
    sheetsApiMock.listWorkbooks.mockRejectedValueOnce(new Error('visible refresh failed'))
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: vi.fn(async () => true)
    }))
    const sharingVersion = mounted.result
    messageErrorMock.mockClear()
    messageSuccessMock.mockClear()

    await sharingVersion.respondIncomingShare('incoming-1', 'ACCEPT')

    expect(refreshCollaboration).not.toHaveBeenCalled()
    expect(selectWorkbook).not.toHaveBeenCalled()
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.shareResponseUpdated')
    expect(messageErrorMock).toHaveBeenCalledWith('visible refresh failed')
    mounted.unmount()
  })
})

describe('sheets workbench template guard', () => {
  beforeEach(() => {
    routeState.query = { workbookId: 'wb-1', panel: 'meta' }
    routerReplaceMock.mockReset()
    routerReplaceMock.mockImplementation(async (location: { query?: Record<string, unknown> }) => {
      routeState.query = { ...(location.query ?? {}) }
    })
    messageErrorMock.mockReset()
    messageSuccessMock.mockReset()
    confirmDiscardMock.mockReset()
    confirmDiscardMock.mockResolvedValue(true)
    Object.values(sheetsApiMock).forEach((mockFn) => mockFn.mockReset())
    Object.values(suiteApiMock).forEach((mockFn) => mockFn.mockReset())
    sheetsApiMock.listWorkbooks.mockResolvedValue([
      buildWorkbookSummary('wb-1'),
      buildWorkbookSummary('wb-2', 'SHARED')
    ])
    sheetsApiMock.getWorkbook.mockImplementation(async (workbookId: string) => {
      return buildWorkbookDetail(workbookId, workbookId === 'wb-2' ? 'SHARED' : 'OWNED')
    })
    sheetsApiMock.listShares.mockResolvedValue([])
    sheetsApiMock.listIncomingShares.mockResolvedValue([])
    sheetsApiMock.createWorkbook.mockResolvedValue(buildWorkbookDetail('wb-template'))
    suiteApiMock.getCollaborationCenter.mockResolvedValue({ items: [] })
    vi.stubGlobal('useRoute', () => routeState)
    vi.stubGlobal('useRouter', () => ({ replace: routerReplaceMock }))
    vi.stubGlobal('onBeforeRouteLeave', () => undefined)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('skips template workbook creation when dirty changes are kept', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result
    workbench.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })
    confirmDiscardMock.mockResolvedValueOnce(false)

    const created = await workbench.createWorkbookFromTemplate(
      SHEETS_TEMPLATE_PRESETS[0],
      SHEETS_TEMPLATE_PRESETS[0].presetTitleKey,
    )

    expect(created).toBe(false)
    expect(sheetsApiMock.createWorkbook).not.toHaveBeenCalled()
    mounted.unmount()
  })

  it('confirms discard only once when creating a template workbook from dirty workspace', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result

    workbench.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })
    confirmDiscardMock.mockResolvedValueOnce(true)

    const created = await workbench.createWorkbookFromTemplate(
      SHEETS_TEMPLATE_PRESETS[0],
      SHEETS_TEMPLATE_PRESETS[0].presetTitleKey,
    )

    expect(created).toBe(true)
    expect(confirmDiscardMock).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.createWorkbook).toHaveBeenCalledWith({
      title: SHEETS_TEMPLATE_PRESETS[0].presetTitleKey,
    })
    expect(sheetsApiMock.getWorkbook).toHaveBeenLastCalledWith('wb-template')
    mounted.unmount()
  })

  it('does not refresh side panels when workspace refresh is canceled by dirty edits', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result

    sheetsApiMock.listWorkbooks.mockClear()
    sheetsApiMock.listShares.mockClear()
    sheetsApiMock.listIncomingShares.mockClear()
    suiteApiMock.getCollaborationCenter.mockClear()

    workbench.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Changed value' })
    confirmDiscardMock.mockResolvedValueOnce(false)

    await workbench.onRefreshWorkspace()

    expect(sheetsApiMock.listWorkbooks).not.toHaveBeenCalled()
    expect(sheetsApiMock.listShares).not.toHaveBeenCalled()
    expect(sheetsApiMock.listIncomingShares).not.toHaveBeenCalled()
    expect(suiteApiMock.getCollaborationCenter).not.toHaveBeenCalled()
    mounted.unmount()
  })

  it('keeps refresh follow-through when workspace refresh succeeds', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result

    sheetsApiMock.listWorkbooks.mockClear()
    sheetsApiMock.getWorkbook.mockClear()
    sheetsApiMock.listShares.mockClear()
    sheetsApiMock.listIncomingShares.mockClear()
    suiteApiMock.getCollaborationCenter.mockClear()

    await workbench.onRefreshWorkspace()

    expect(sheetsApiMock.listWorkbooks).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.getWorkbook).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.listShares).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.listIncomingShares).toHaveBeenCalledTimes(1)
    expect(suiteApiMock.getCollaborationCenter).toHaveBeenCalledTimes(1)
    mounted.unmount()
  })

  it('stops post-refresh chain when incoming refresh fails', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result

    sheetsApiMock.listWorkbooks.mockClear()
    sheetsApiMock.getWorkbook.mockClear()
    sheetsApiMock.listIncomingShares.mockRejectedValueOnce(new Error('incoming refresh failed'))
    sheetsApiMock.listShares.mockClear()
    suiteApiMock.getCollaborationCenter.mockClear()
    messageErrorMock.mockClear()

    await workbench.onRefreshWorkspace()

    expect(sheetsApiMock.listWorkbooks).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.getWorkbook).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.listShares).not.toHaveBeenCalled()
    expect(suiteApiMock.getCollaborationCenter).not.toHaveBeenCalled()
    expect(messageErrorMock).toHaveBeenCalledWith('incoming refresh failed')
    mounted.unmount()
  })

  it('stops post-refresh chain when share refresh fails', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result

    sheetsApiMock.listWorkbooks.mockClear()
    sheetsApiMock.getWorkbook.mockClear()
    sheetsApiMock.listIncomingShares.mockResolvedValueOnce([])
    sheetsApiMock.listShares.mockRejectedValueOnce(new Error('share refresh failed'))
    suiteApiMock.getCollaborationCenter.mockClear()
    messageErrorMock.mockClear()

    await workbench.onRefreshWorkspace()

    expect(sheetsApiMock.listWorkbooks).toHaveBeenCalledTimes(1)
    expect(sheetsApiMock.getWorkbook).toHaveBeenCalledTimes(1)
    expect(suiteApiMock.getCollaborationCenter).not.toHaveBeenCalled()
    expect(messageErrorMock).toHaveBeenCalledWith('share refresh failed')
    mounted.unmount()
  })

  it('refreshes collaboration only when workbook creation succeeds', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result

    requestWorkbookTitleMock.mockResolvedValueOnce('Created workbook')
    sheetsApiMock.createWorkbook.mockResolvedValueOnce(buildWorkbookDetail('wb-created'))
    suiteApiMock.getCollaborationCenter.mockClear()

    await workbench.onCreateWorkbook()

    expect(sheetsApiMock.createWorkbook).toHaveBeenCalledWith({ title: 'Created workbook' })
    expect(suiteApiMock.getCollaborationCenter).toHaveBeenCalledTimes(1)
    mounted.unmount()
  })

  it('does not refresh collaboration when workbook creation is canceled', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result

    requestWorkbookTitleMock.mockResolvedValueOnce(undefined)
    sheetsApiMock.createWorkbook.mockClear()
    suiteApiMock.getCollaborationCenter.mockClear()

    await workbench.onCreateWorkbook()

    expect(sheetsApiMock.createWorkbook).not.toHaveBeenCalled()
    expect(suiteApiMock.getCollaborationCenter).not.toHaveBeenCalled()
    mounted.unmount()
  })

  it('does not refresh collaboration when saving is a no-op', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result

    suiteApiMock.getCollaborationCenter.mockClear()

    await workbench.onSaveWorkbook()

    expect(suiteApiMock.getCollaborationCenter).not.toHaveBeenCalled()
    mounted.unmount()
  })

  it('does not refresh collaboration after export succeeds', async () => {
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result

    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:mock'),
      revokeObjectURL: vi.fn(),
    })
    sheetsApiMock.exportWorkbook.mockResolvedValueOnce({
      fileName: 'roadmap.csv',
      format: 'CSV',
      content: 'a,b',
      exportedAt: '2026-03-30T17:55:00',
    })
    suiteApiMock.getCollaborationCenter.mockClear()

    await workbench.onExportWorkbook('CSV')

    expect(sheetsApiMock.exportWorkbook).toHaveBeenCalledWith('wb-1', 'CSV')
    expect(suiteApiMock.getCollaborationCenter).not.toHaveBeenCalled()
    mounted.unmount()
  })
})
