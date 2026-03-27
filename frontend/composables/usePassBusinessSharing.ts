import { ref } from 'vue'
import { usePassApi } from '~/composables/usePassApi'
import type {
  PassIncomingSharedItemDetail,
  PassIncomingSharedItemSummary,
  PassItemShare
} from '~/types/pass-business'

interface LoadIncomingSharesOptions {
  orgId?: string
  keyword?: string
  favoriteOnly?: boolean
  limit?: number
  itemType?: string
  keepSelection?: boolean
}

export function usePassBusinessSharing() {
  const {
    listItemShares,
    createItemShare,
    removeItemShare,
    listIncomingItemShares,
    getIncomingItemShare
  } = usePassApi()

  const itemShares = ref<PassItemShare[]>([])
  const incomingSharedItems = ref<PassIncomingSharedItemSummary[]>([])
  const selectedIncomingItemId = ref('')
  const incomingSharedItemDetail = ref<PassIncomingSharedItemDetail | null>(null)
  const loadingItemShares = ref(false)
  const loadingIncomingSharedItems = ref(false)
  const loadingIncomingSharedItemDetail = ref(false)

  function clearItemShares(): void {
    itemShares.value = []
  }

  function clearIncomingShares(): void {
    incomingSharedItems.value = []
    selectedIncomingItemId.value = ''
    incomingSharedItemDetail.value = null
  }

  async function loadItemShares(orgId?: string, itemId?: string): Promise<void> {
    if (!orgId || !itemId) {
      clearItemShares()
      return
    }
    loadingItemShares.value = true
    try {
      itemShares.value = await listItemShares(orgId, itemId)
    } finally {
      loadingItemShares.value = false
    }
  }

  async function loadIncomingShareDetail(orgId?: string, itemId?: string): Promise<void> {
    if (!orgId || !itemId) {
      selectedIncomingItemId.value = ''
      incomingSharedItemDetail.value = null
      return
    }
    selectedIncomingItemId.value = itemId
    loadingIncomingSharedItemDetail.value = true
    try {
      incomingSharedItemDetail.value = await getIncomingItemShare(orgId, itemId)
    } finally {
      loadingIncomingSharedItemDetail.value = false
    }
  }

  async function loadIncomingShares(options: LoadIncomingSharesOptions): Promise<void> {
    if (!options.orgId) {
      clearIncomingShares()
      return
    }
    loadingIncomingSharedItems.value = true
    try {
      const nextItems = await listIncomingItemShares(
        options.orgId,
        options.keyword || '',
        options.favoriteOnly || false,
        options.limit || 100,
        options.itemType
      )
      incomingSharedItems.value = nextItems
      if (!nextItems.length) {
        selectedIncomingItemId.value = ''
        incomingSharedItemDetail.value = null
        return
      }
      const targetItemId = options.keepSelection && selectedIncomingItemId.value
        && nextItems.some((item) => item.itemId === selectedIncomingItemId.value)
        ? selectedIncomingItemId.value
        : nextItems[0].itemId
      await loadIncomingShareDetail(options.orgId, targetItemId)
    } finally {
      loadingIncomingSharedItems.value = false
    }
  }

  async function addItemShare(orgId: string, itemId: string, email: string): Promise<void> {
    await createItemShare(orgId, itemId, { email })
    await loadItemShares(orgId, itemId)
  }

  async function deleteItemShare(orgId: string, itemId: string, shareId: string): Promise<void> {
    await removeItemShare(orgId, itemId, shareId)
    await loadItemShares(orgId, itemId)
  }

  return {
    itemShares,
    incomingSharedItems,
    selectedIncomingItemId,
    incomingSharedItemDetail,
    loadingItemShares,
    loadingIncomingSharedItems,
    loadingIncomingSharedItemDetail,
    clearItemShares,
    clearIncomingShares,
    loadItemShares,
    loadIncomingShares,
    loadIncomingShareDetail,
    addItemShare,
    deleteItemShare
  }
}
