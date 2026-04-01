import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { useSheetsWorkbench } from '../composables/useSheetsWorkbench'

const suiteApiMock = vi.hoisted(() => ({
  getCollaborationCenter: vi.fn(),
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('~/composables/useSuiteApi', () => ({
  useSuiteApi: () => suiteApiMock,
}))

vi.mock('~/composables/useSheetsApi', () => ({
  useSheetsApi: () => ({
    createWorkbook: vi.fn(),
  }),
}))

vi.mock('~/composables/useSheetsWorkspace', () => ({
  useSheetsWorkspace: () => ({
    workbooks: ref([]),
    activeWorkbook: ref(null),
    activeWorkbookId: computed(() => 'wb-1'),
    activeSheet: computed(() => null),
    activeCell: ref(null),
    localGrid: ref([]),
    computedGrid: computed(() => []),
    conflictMessage: ref(''),
    dirtyCount: computed(() => 0),
    confirmDiscardChangesIfNeeded: vi.fn(async () => true),
    selectWorkbook: vi.fn(async () => true),
    onCreateWorkbook: vi.fn(async () => false),
    onImportWorkbook: vi.fn(async () => false),
    onExportWorkbook: vi.fn(async () => false),
    onRenameWorkbook: vi.fn(async () => false),
    onDeleteWorkbook: vi.fn(async () => false),
    onCreateSheet: vi.fn(async () => false),
    onRenameSheet: vi.fn(async () => false),
    onDeleteSheet: vi.fn(async () => false),
    onSelectSheet: vi.fn(async () => false),
    onSaveWorkbook: vi.fn(async () => false),
    onRefreshWorkspace: vi.fn(async () => false),
  }),
}))

vi.mock('~/composables/useSheetsDataTools', () => ({
  useSheetsDataTools: () => ({
    onSortSheet: vi.fn(async () => false),
    onFreezeRowsToActiveCell: vi.fn(async () => false),
    onFreezeColsToActiveCell: vi.fn(async () => false),
    onClearFreeze: vi.fn(async () => false),
  }),
}))

vi.mock('~/composables/useSheetsSharingVersionWorkbench', () => ({
  useSheetsSharingVersionWorkbench: () => ({
    refreshIncomingShares: vi.fn(async () => true),
    refreshShares: vi.fn(async () => true),
  }),
}))

vi.mock('~/utils/sheets-collaboration', () => ({
  SHEETS_TEMPLATE_PRESETS: [],
  filterSheetsCollaborationEvents: (items: unknown[]) => items,
}))

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
  if (!result) {
    throw new Error('composable did not initialize')
  }
  return {
    result,
    unmount: () => wrapper.unmount(),
  }
}

describe('sheets collaboration refresh state', () => {
  beforeEach(() => {
    suiteApiMock.getCollaborationCenter.mockReset()
    suiteApiMock.getCollaborationCenter.mockResolvedValue({ items: [] })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('ignores stale collaboration refresh results after newer refresh starts', async () => {
    const firstRefresh = createDeferred<{ items: Array<{ id: string }> }>()
    const secondRefresh = createDeferred<{ items: Array<{ id: string }> }>()
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result
    suiteApiMock.getCollaborationCenter
      .mockImplementationOnce(() => firstRefresh.promise)
      .mockImplementationOnce(() => secondRefresh.promise)

    const firstPromise = workbench.refreshCollaboration()
    await flushPromises()
    const secondPromise = workbench.refreshCollaboration()
    await flushPromises()
    firstRefresh.resolve({ items: [{ id: 'stale' }] })
    await firstPromise
    await flushPromises()

    expect(workbench.collaborationEventCount.value).toBe(0)
    expect(workbench.collaborationLoading.value).toBe(true)

    secondRefresh.resolve({ items: [{ id: 'current' }] })
    await secondPromise
    await flushPromises()

    expect(workbench.collaborationEventCount.value).toBe(1)
    expect(workbench.collaborationLoading.value).toBe(false)
    mounted.unmount()
  })

  it('ignores stale collaboration errors after newer refresh starts', async () => {
    const firstRefresh = createDeferred<{ items: Array<{ id: string }> }>()
    const secondRefresh = createDeferred<{ items: Array<{ id: string }> }>()
    const mounted = await mountComposable(() => useSheetsWorkbench())
    const workbench = mounted.result
    suiteApiMock.getCollaborationCenter
      .mockImplementationOnce(() => firstRefresh.promise)
      .mockImplementationOnce(() => secondRefresh.promise)

    const firstPromise = workbench.refreshCollaboration()
    await flushPromises()
    const secondPromise = workbench.refreshCollaboration()
    await flushPromises()
    firstRefresh.reject(new Error('stale failed'))
    await firstPromise
    await flushPromises()

    expect(workbench.collaborationError.value).toBe('')
    expect(workbench.collaborationLoading.value).toBe(true)

    secondRefresh.resolve({ items: [{ id: 'current' }] })
    await secondPromise
    await flushPromises()

    expect(workbench.collaborationError.value).toBe('')
    expect(workbench.collaborationEventCount.value).toBe(1)
    expect(workbench.collaborationLoading.value).toBe(false)
    mounted.unmount()
  })
})
