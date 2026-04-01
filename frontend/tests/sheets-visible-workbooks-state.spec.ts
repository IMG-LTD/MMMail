import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, defineComponent, reactive, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { useSheetsSharingVersionWorkbench } from '../composables/useSheetsSharingVersionWorkbench'
import type { SheetsWorkbookDetail, SheetsWorkbookSummary } from '../types/sheets'

const {
  routerReplaceMock,
  messageErrorMock,
  messageSuccessMock,
  sheetsApiMock,
} = vi.hoisted(() => ({
  routerReplaceMock: vi.fn(),
  messageErrorMock: vi.fn(),
  messageSuccessMock: vi.fn(),
  sheetsApiMock: {
    listShares: vi.fn(),
    listIncomingShares: vi.fn(),
    listVersions: vi.fn(),
    respondIncomingShare: vi.fn(),
    listWorkbooks: vi.fn(),
  },
}))

const routeState = reactive({ query: {} as Record<string, unknown> })

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
  },
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

vi.mock('~/composables/useSheetsApi', () => ({
  useSheetsApi: () => sheetsApiMock,
}))

function buildWorkbookSummary(id: string): SheetsWorkbookSummary {
  return {
    id,
    title: `Workbook ${id}`,
    rowCount: 2,
    colCount: 2,
    filledCellCount: 1,
    formulaCellCount: 0,
    computedErrorCount: 0,
    currentVersion: 1,
    sheetCount: 1,
    activeSheetId: 'sheet-1',
    updatedAt: '2026-03-31T10:00:00',
    lastOpenedAt: '2026-03-31T10:05:00',
    permission: 'OWNER',
    scope: 'OWNED',
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 1,
    canEdit: true,
  }
}

function buildWorkbookDetail(id: string): SheetsWorkbookDetail {
  return {
    ...buildWorkbookSummary(id),
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
      grid: [['A1']],
      computedGrid: [['A1']],
    }],
    grid: [['A1']],
    computedGrid: [['A1']],
    supportedImportFormats: ['CSV'],
    supportedExportFormats: ['CSV'],
    createdAt: '2026-03-31T09:50:00',
    canManageShares: true,
    canRestoreVersions: true,
  }
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
    },
  }))
  await flushPromises()
  if (!result) throw new Error('composable did not initialize')
  return { result, unmount: () => wrapper.unmount() }
}

describe('sheets visible workbooks state', () => {
  beforeEach(() => {
    routeState.query = { workbookId: 'wb-1', view: 'WORKBOOKS', scope: 'ALL' }
    routerReplaceMock.mockReset()
    routerReplaceMock.mockResolvedValue(undefined)
    messageErrorMock.mockReset()
    messageSuccessMock.mockReset()
    Object.values(sheetsApiMock).forEach((mockFn) => mockFn.mockReset())
    sheetsApiMock.listIncomingShares.mockResolvedValue([])
    sheetsApiMock.listShares.mockResolvedValue([])
    sheetsApiMock.listVersions.mockResolvedValue([])
    vi.stubGlobal('useRoute', () => routeState)
    vi.stubGlobal('useRouter', () => ({ replace: routerReplaceMock }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('ignores stale visible workbook results after newer incoming action starts', async () => {
    const firstResponse = createDeferred<{ workbookId: string }>()
    const secondResponse = createDeferred<{ workbookId: string }>()
    const firstList = createDeferred<SheetsWorkbookSummary[]>()
    const secondList = createDeferred<SheetsWorkbookSummary[]>()
    const workbooks = ref([buildWorkbookSummary('wb-1')])
    sheetsApiMock.respondIncomingShare.mockImplementation((shareId: string) => (
      shareId === 'incoming-1' ? firstResponse.promise : secondResponse.promise
    ))
    sheetsApiMock.listWorkbooks
      .mockImplementationOnce(() => firstList.promise)
      .mockImplementationOnce(() => secondList.promise)
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks,
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true),
    }))
    const sharingVersion = mounted.result

    const firstPromise = sharingVersion.respondIncomingShare('incoming-1', 'DECLINE')
    await flushPromises()
    firstResponse.resolve({ workbookId: 'wb-stale' })
    await flushPromises()

    const secondPromise = sharingVersion.respondIncomingShare('incoming-2', 'DECLINE')
    await flushPromises()
    secondResponse.resolve({ workbookId: 'wb-current' })
    await flushPromises()

    firstList.resolve([buildWorkbookSummary('wb-stale')])
    await firstPromise
    await flushPromises()

    expect(workbooks.value).toEqual([buildWorkbookSummary('wb-1')])
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.shareResponseUpdated')

    secondList.resolve([buildWorkbookSummary('wb-current')])
    await secondPromise
    await flushPromises()

    expect(workbooks.value).toEqual([buildWorkbookSummary('wb-current')])
    mounted.unmount()
  })

  it('ignores stale visible workbook errors after newer incoming action starts', async () => {
    const firstResponse = createDeferred<{ workbookId: string }>()
    const secondResponse = createDeferred<{ workbookId: string }>()
    const firstList = createDeferred<SheetsWorkbookSummary[]>()
    const secondList = createDeferred<SheetsWorkbookSummary[]>()
    const workbooks = ref([buildWorkbookSummary('wb-1')])
    sheetsApiMock.respondIncomingShare.mockImplementation((shareId: string) => (
      shareId === 'incoming-1' ? firstResponse.promise : secondResponse.promise
    ))
    sheetsApiMock.listWorkbooks
      .mockImplementationOnce(() => firstList.promise)
      .mockImplementationOnce(() => secondList.promise)
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks,
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true),
    }))
    const sharingVersion = mounted.result

    const firstPromise = sharingVersion.respondIncomingShare('incoming-1', 'DECLINE')
    await flushPromises()
    firstResponse.resolve({ workbookId: 'wb-stale' })
    await flushPromises()

    const secondPromise = sharingVersion.respondIncomingShare('incoming-2', 'DECLINE')
    await flushPromises()
    secondResponse.resolve({ workbookId: 'wb-current' })
    await flushPromises()

    firstList.reject(new Error('stale visible failed'))
    await firstPromise
    await flushPromises()

    expect(sharingVersion.incomingErrorMessage.value).toBe('')
    expect(messageErrorMock).not.toHaveBeenCalledWith('stale visible failed')

    secondList.resolve([buildWorkbookSummary('wb-current')])
    await secondPromise
    await flushPromises()

    expect(workbooks.value).toEqual([buildWorkbookSummary('wb-current')])
    mounted.unmount()
  })
})
