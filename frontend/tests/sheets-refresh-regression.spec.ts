import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, reactive } from 'vue'
import { useSheetsWorkspace } from '../composables/useSheetsWorkspace'
import type { SheetsWorkbookDetail, SheetsWorkbookSummary } from '../types/sheets'

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
    getCollaborationCenter: vi.fn()
  }
}))

const routeState = reactive({
  query: {} as Record<string, unknown>
})

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

function buildWorkbookSummary(workbookId: string): SheetsWorkbookSummary {
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
    permission: 'OWNER',
    scope: 'OWNED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 1,
    canEdit: true
  }
}

function buildWorkbookDetail(workbookId: string): SheetsWorkbookDetail {
  return {
    ...buildWorkbookSummary(workbookId),
    sheets: [{
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
    }],
    grid: [['A1', '']],
    computedGrid: [['A1', '']],
    supportedImportFormats: ['CSV'],
    supportedExportFormats: ['CSV'],
    createdAt: '2026-03-29T09:50:00',
    canManageShares: true,
    canRestoreVersions: true
  }
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

describe('sheets refresh regressions', () => {
  beforeEach(() => {
    routeState.query = { workbookId: 'wb-1' }
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
    sheetsApiMock.listWorkbooks
      .mockResolvedValueOnce([buildWorkbookSummary('wb-1')])
      .mockResolvedValueOnce([buildWorkbookSummary('wb-2')])
    sheetsApiMock.getWorkbook.mockImplementation(async (workbookId: string) => buildWorkbookDetail(workbookId))
    sheetsApiMock.listShares.mockResolvedValue([])
    sheetsApiMock.listIncomingShares.mockResolvedValue([])
    sheetsApiMock.listVersions.mockResolvedValue([])
    suiteApiMock.getCollaborationCenter.mockResolvedValue({ items: [] })
    vi.stubGlobal('useRoute', () => routeState)
    vi.stubGlobal('useRouter', () => ({ replace: routerReplaceMock }))
    vi.stubGlobal('onBeforeRouteLeave', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('confirms discard only once when refresh falls back to another workbook', async () => {
    const mounted = await mountComposable(() => useSheetsWorkspace())
    const workspace = mounted.result
    messageSuccessMock.mockClear()

    workspace.onCellChange({ rowIndex: 0, colIndex: 0, value: 'Dirty value' })

    const refreshed = await workspace.onRefreshWorkspace()

    expect(refreshed).toBe(true)
    expect(confirmDiscardMock).toHaveBeenCalledTimes(1)
    expect(confirmDiscardMock).toHaveBeenCalledWith('Summary')
    expect(sheetsApiMock.listWorkbooks).toHaveBeenCalledTimes(2)
    expect(sheetsApiMock.getWorkbook).toHaveBeenLastCalledWith('wb-2')
    expect(workspace.activeWorkbook.value?.id).toBe('wb-2')
    expect(routeState.query.workbookId).toBe('wb-2')
    expect(messageSuccessMock).toHaveBeenCalledWith('sheets.messages.workspaceRefreshed')

    mounted.unmount()
  })
})
