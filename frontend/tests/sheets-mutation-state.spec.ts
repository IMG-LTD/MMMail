import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { computed, defineComponent, reactive, ref } from 'vue'
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
    listWorkbooks: vi.fn(),
    listShares: vi.fn(),
    listIncomingShares: vi.fn(),
    listVersions: vi.fn(),
    createShare: vi.fn(),
    updateShare: vi.fn(),
    removeShare: vi.fn(),
    restoreVersion: vi.fn(),
    respondIncomingShare: vi.fn(),
  },
}))

const routeState = reactive({
  query: {} as Record<string, unknown>,
})

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
  },
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('~/composables/useSheetsApi', () => ({
  useSheetsApi: () => sheetsApiMock,
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
    currentVersion: 2,
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
      computedGrid: [['A1', '']],
    }],
    grid: [['A1', '']],
    computedGrid: [['A1', '']],
    supportedImportFormats: ['CSV'],
    supportedExportFormats: ['CSV'],
    createdAt: '2026-03-31T09:50:00',
    canManageShares: true,
    canRestoreVersions: true,
  }
}

function buildShare(shareId: string) {
  return {
    shareId,
    collaboratorUserId: `user-${shareId}`,
    collaboratorEmail: `${shareId}@mmmail.local`,
    collaboratorDisplayName: shareId,
    permission: 'EDIT' as const,
    responseStatus: 'NEEDS_ACTION' as const,
    createdAt: '2026-03-31T10:00:00',
    updatedAt: '2026-03-31T10:00:00',
  }
}
function buildIncomingShare(shareId: string) {
  return {
    shareId,
    workbookId: `wb-${shareId}`,
    workbookTitle: `Workbook ${shareId}`,
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    permission: 'VIEW' as const,
    responseStatus: 'NEEDS_ACTION' as const,
    updatedAt: '2026-03-31T10:00:00',
  }
}

function createDeferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((nextResolve) => {
    resolve = nextResolve
  })
  return { promise, resolve }
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
  if (!result) {
    throw new Error('composable did not initialize')
  }
  return {
    result,
    unmount: () => wrapper.unmount(),
  }
}

describe('sheets mutation cleanup state', () => {
  beforeEach(() => {
    routeState.query = {
      workbookId: 'wb-1',
      view: 'WORKBOOKS',
      scope: 'ALL',
    }
    routerReplaceMock.mockReset()
    routerReplaceMock.mockResolvedValue(undefined)
    messageErrorMock.mockReset()
    messageSuccessMock.mockReset()
    Object.values(sheetsApiMock).forEach((mockFn) => mockFn.mockReset())
    sheetsApiMock.listWorkbooks.mockResolvedValue([
      buildWorkbookSummary('wb-1'),
      buildWorkbookSummary('wb-2'),
    ])
    sheetsApiMock.listShares.mockResolvedValue([])
    sheetsApiMock.listIncomingShares.mockResolvedValue([])
    sheetsApiMock.listVersions.mockResolvedValue([])
    vi.stubGlobal('useRoute', () => routeState)
    vi.stubGlobal('useRouter', () => ({ replace: routerReplaceMock }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('keeps current share submit loading when stale submit settles', async () => {
    const firstSubmit = createDeferred<ReturnType<typeof buildShare>>()
    const secondSubmit = createDeferred<ReturnType<typeof buildShare>>()
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    sheetsApiMock.createShare.mockImplementation((workbookId: string) => {
      return workbookId === 'wb-1' ? firstSubmit.promise : secondSubmit.promise
    })
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook,
      activeWorkbookId: computed(() => activeWorkbook.value.id),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true),
    }))
    const sharingVersion = mounted.result

    sharingVersion.inviteEmail.value = 'first@mmmail.local'
    const firstPromise = sharingVersion.submitShare()
    await flushPromises()
    expect(sharingVersion.shareSubmitting.value).toBe(true)

    activeWorkbook.value = buildWorkbookDetail('wb-2')
    await flushPromises()
    sharingVersion.inviteEmail.value = 'second@mmmail.local'
    const secondPromise = sharingVersion.submitShare()
    await flushPromises()

    firstSubmit.resolve(buildShare('share-1'))
    await firstPromise
    await flushPromises()

    expect(sharingVersion.shareSubmitting.value).toBe(true)

    secondSubmit.resolve(buildShare('share-2'))
    await secondPromise
    await flushPromises()

    expect(sharingVersion.shareSubmitting.value).toBe(false)
    mounted.unmount()
  })

  it('keeps current share mutation id when stale share mutation settles', async () => {
    const firstUpdate = createDeferred<void>()
    const secondRemove = createDeferred<void>()
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    sheetsApiMock.updateShare.mockImplementation((workbookId: string) => {
      return workbookId === 'wb-1' ? firstUpdate.promise : Promise.resolve()
    })
    sheetsApiMock.removeShare.mockImplementation((workbookId: string) => {
      return workbookId === 'wb-2' ? secondRemove.promise : Promise.resolve()
    })
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook,
      activeWorkbookId: computed(() => activeWorkbook.value.id),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true),
    }))
    const sharingVersion = mounted.result

    const firstPromise = sharingVersion.updateSharePermission('share-1', 'VIEW')
    await flushPromises()
    expect(sharingVersion.shareMutationId.value).toBe('share-1')

    activeWorkbook.value = buildWorkbookDetail('wb-2')
    await flushPromises()
    const secondPromise = sharingVersion.removeShare('share-2')
    await flushPromises()
    expect(sharingVersion.shareMutationId.value).toBe('share-2')

    firstUpdate.resolve()
    await firstPromise
    await flushPromises()

    expect(sharingVersion.shareMutationId.value).toBe('share-2')

    secondRemove.resolve()
    await secondPromise
    await flushPromises()

    expect(sharingVersion.shareMutationId.value).toBe('')
    mounted.unmount()
  })

  it('keeps current version mutation id when stale restore settles', async () => {
    const firstRestore = createDeferred<SheetsWorkbookDetail>()
    const secondRestore = createDeferred<SheetsWorkbookDetail>()
    const activeWorkbook = ref(buildWorkbookDetail('wb-1'))
    sheetsApiMock.restoreVersion.mockImplementation((workbookId: string) => {
      return workbookId === 'wb-1' ? firstRestore.promise : secondRestore.promise
    })
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook,
      activeWorkbookId: computed(() => activeWorkbook.value.id),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true),
    }))
    const sharingVersion = mounted.result

    const firstPromise = sharingVersion.restoreVersion('version-1')
    await flushPromises()
    expect(sharingVersion.versionMutationId.value).toBe('version-1')

    activeWorkbook.value = buildWorkbookDetail('wb-2')
    await flushPromises()
    const secondPromise = sharingVersion.restoreVersion('version-2')
    await flushPromises()
    expect(sharingVersion.versionMutationId.value).toBe('version-2')

    firstRestore.resolve(buildWorkbookDetail('wb-1'))
    await firstPromise
    await flushPromises()

    expect(sharingVersion.versionMutationId.value).toBe('version-2')

    secondRestore.resolve(buildWorkbookDetail('wb-2'))
    await secondPromise
    await flushPromises()

    expect(sharingVersion.versionMutationId.value).toBe('')
    mounted.unmount()
  })

  it('keeps current incoming mutation id when stale response settles', async () => {
    const firstResponse = createDeferred<{
      shareId: string
      workbookId: string
      workbookTitle: string
      ownerEmail: string
      ownerDisplayName: string
      permission: 'VIEW'
      responseStatus: 'DECLINED'
      updatedAt: string
    }>()
    const secondResponse = createDeferred<{
      shareId: string
      workbookId: string
      workbookTitle: string
      ownerEmail: string
      ownerDisplayName: string
      permission: 'VIEW'
      responseStatus: 'DECLINED'
      updatedAt: string
    }>()
    sheetsApiMock.respondIncomingShare.mockImplementation((shareId: string) => {
      return shareId === 'incoming-1' ? firstResponse.promise : secondResponse.promise
    })
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true),
    }))
    const sharingVersion = mounted.result

    const firstPromise = sharingVersion.respondIncomingShare('incoming-1', 'DECLINE')
    await flushPromises()
    expect(sharingVersion.incomingMutationId.value).toBe('incoming-1')

    const secondPromise = sharingVersion.respondIncomingShare('incoming-2', 'DECLINE')
    await flushPromises()
    expect(sharingVersion.incomingMutationId.value).toBe('incoming-2')

    firstResponse.resolve({
      shareId: 'incoming-1',
      workbookId: 'wb-1',
      workbookTitle: 'Workbook 1',
      ownerEmail: 'owner@mmmail.local',
      ownerDisplayName: 'Owner',
      permission: 'VIEW',
      responseStatus: 'DECLINED',
      updatedAt: '2026-03-31T10:00:00',
    })
    await firstPromise
    await flushPromises()

    expect(sharingVersion.incomingMutationId.value).toBe('incoming-2')

    secondResponse.resolve({
      shareId: 'incoming-2',
      workbookId: 'wb-2',
      workbookTitle: 'Workbook 2',
      ownerEmail: 'owner@mmmail.local',
      ownerDisplayName: 'Owner',
      permission: 'VIEW',
      responseStatus: 'DECLINED',
      updatedAt: '2026-03-31T10:00:00',
    })
    await secondPromise
    await flushPromises()

    expect(sharingVersion.incomingMutationId.value).toBe('')
    mounted.unmount()
  })

  it('ignores stale incoming response follow-through after newer action starts', async () => {
    const firstResponse = createDeferred<{
      shareId: string
      workbookId: string
      workbookTitle: string
      ownerEmail: string
      ownerDisplayName: string
      permission: 'VIEW'
      responseStatus: 'ACCEPTED'
      updatedAt: string
    }>()
    const secondResponse = createDeferred<{
      shareId: string
      workbookId: string
      workbookTitle: string
      ownerEmail: string
      ownerDisplayName: string
      permission: 'VIEW'
      responseStatus: 'DECLINED'
      updatedAt: string
    }>()
    const selectWorkbook = vi.fn(async () => true)
    const refreshCollaboration = vi.fn(async () => undefined)
    sheetsApiMock.respondIncomingShare.mockImplementation((shareId: string) => {
      return shareId === 'incoming-1' ? firstResponse.promise : secondResponse.promise
    })
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1'), buildWorkbookSummary('wb-2')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook,
      refreshCollaboration,
      confirmDiscardChangesIfNeeded: vi.fn(async () => true),
    }))
    const sharingVersion = mounted.result
    sheetsApiMock.listIncomingShares.mockClear()
    sheetsApiMock.listWorkbooks.mockClear()
    messageSuccessMock.mockClear()

    const firstPromise = sharingVersion.respondIncomingShare('incoming-1', 'ACCEPT')
    await flushPromises()
    const secondPromise = sharingVersion.respondIncomingShare('incoming-2', 'DECLINE')
    await flushPromises()

    firstResponse.resolve({
      shareId: 'incoming-1',
      workbookId: 'wb-2',
      workbookTitle: 'Workbook 2',
      ownerEmail: 'owner@mmmail.local',
      ownerDisplayName: 'Owner',
      permission: 'VIEW',
      responseStatus: 'ACCEPTED',
      updatedAt: '2026-03-31T10:00:00',
    })
    await firstPromise
    await flushPromises()

    expect(sheetsApiMock.listIncomingShares).not.toHaveBeenCalled()
    expect(sheetsApiMock.listWorkbooks).not.toHaveBeenCalled()
    expect(refreshCollaboration).not.toHaveBeenCalled()
    expect(selectWorkbook).not.toHaveBeenCalled()
    expect(messageSuccessMock).not.toHaveBeenCalledWith('sheets.messages.shareResponseUpdated')

    secondResponse.resolve({
      shareId: 'incoming-2',
      workbookId: 'wb-1',
      workbookTitle: 'Workbook 1',
      ownerEmail: 'owner@mmmail.local',
      ownerDisplayName: 'Owner',
      permission: 'VIEW',
      responseStatus: 'DECLINED',
      updatedAt: '2026-03-31T10:00:00',
    })
    await secondPromise
    await flushPromises()

    mounted.unmount()
  })

  it('ignores stale incoming refresh results after newer refresh starts', async () => {
    const firstRefresh = createDeferred<ReturnType<typeof buildIncomingShare>[]>()
    const secondRefresh = createDeferred<ReturnType<typeof buildIncomingShare>[]>()
    const mounted = await mountComposable(() => useSheetsSharingVersionWorkbench({
      workbooks: ref([buildWorkbookSummary('wb-1')]),
      activeWorkbook: ref(buildWorkbookDetail('wb-1')),
      activeWorkbookId: computed(() => 'wb-1'),
      selectWorkbook: vi.fn(async () => true),
      refreshCollaboration: vi.fn(async () => undefined),
      confirmDiscardChangesIfNeeded: vi.fn(async () => true),
    }))
    const sharingVersion = mounted.result
    sheetsApiMock.listIncomingShares
      .mockImplementationOnce(() => firstRefresh.promise)
      .mockImplementationOnce(() => secondRefresh.promise)

    const firstPromise = sharingVersion.refreshIncomingShares()
    await flushPromises()
    const secondPromise = sharingVersion.refreshIncomingShares()
    await flushPromises()
    firstRefresh.resolve([buildIncomingShare('stale')])
    await firstPromise
    await flushPromises()

    expect(sharingVersion.incomingShares.value).toEqual([])
    expect(sharingVersion.incomingLoading.value).toBe(true)

    secondRefresh.resolve([buildIncomingShare('current')])
    await secondPromise
    await flushPromises()

    expect(sharingVersion.incomingShares.value).toEqual([buildIncomingShare('current')])
    expect(sharingVersion.incomingLoading.value).toBe(false)
    mounted.unmount()
  })
})
