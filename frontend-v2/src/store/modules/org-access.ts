import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { OrgAccessScope, OrgProductKey } from '@/shared/types/organization'

const STORAGE_KEY = 'mmmail.active-org-scope.v1'

interface EnsureLoadedOptions {
  force?: boolean
  isAuthenticated: boolean
  loadAccessContext: () => Promise<OrgAccessScope[]>
}

function canUseLocalStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function readPersistedActiveOrgId() {
  if (!canUseLocalStorage()) {
    return ''
  }

  return window.localStorage.getItem(STORAGE_KEY) || ''
}

function persistActiveOrgId(orgId: string) {
  if (!canUseLocalStorage()) {
    return
  }

  if (!orgId) {
    window.localStorage.removeItem(STORAGE_KEY)
    return
  }

  window.localStorage.setItem(STORAGE_KEY, orgId)
}

function resolveActiveOrgId(scopes: OrgAccessScope[], candidateOrgId: string) {
  if (candidateOrgId && scopes.some(scope => scope.orgId === candidateOrgId)) {
    return candidateOrgId
  }

  return scopes.length === 1 ? scopes[0].orgId : ''
}

export const useOrgAccessStore = defineStore('org-access', () => {
  const accessScopes = ref<OrgAccessScope[]>([])
  const activeOrgId = ref(readPersistedActiveOrgId())
  const initialized = ref(false)
  const loading = ref(false)

  const activeScope = computed(() => accessScopes.value.find(scope => scope.orgId === activeOrgId.value) || null)
  const hasScopes = computed(() => accessScopes.value.length > 0)
  const enabledProductSet = computed(() => {
    const items = activeScope.value?.products || []
    return new Set(items.filter(item => item.accessState === 'ENABLED').map(item => item.productKey))
  })

  function applyScopes(scopes: OrgAccessScope[]) {
    accessScopes.value = scopes
    activeOrgId.value = resolveActiveOrgId(scopes, activeOrgId.value)
    initialized.value = true
    persistActiveOrgId(activeOrgId.value)
  }

  async function ensureLoaded(options: EnsureLoadedOptions) {
    if (!options.isAuthenticated) {
      clear()
      return
    }

    if (initialized.value && !options.force) {
      return
    }

    loading.value = true
    try {
      applyScopes(await options.loadAccessContext())
    } finally {
      loading.value = false
    }
  }

  function setActiveOrgId(orgId: string) {
    activeOrgId.value = resolveActiveOrgId(accessScopes.value, orgId)
    persistActiveOrgId(activeOrgId.value)
  }

  function setPersonalScope() {
    activeOrgId.value = ''
    persistActiveOrgId('')
  }

  function isProductEnabled(productKey: OrgProductKey) {
    if (!activeScope.value) {
      return true
    }

    return enabledProductSet.value.has(productKey)
  }

  function clear() {
    accessScopes.value = []
    activeOrgId.value = ''
    initialized.value = false
    persistActiveOrgId('')
  }

  return {
    accessScopes,
    activeOrgId,
    activeScope,
    applyScopes,
    clear,
    ensureLoaded,
    hasScopes,
    initialized,
    isProductEnabled,
    loading,
    setActiveOrgId,
    setPersonalScope
  }
})
