import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { OrgAccessScope, OrgProductKey } from '@/shared/types/organization'

const STORAGE_KEY = 'mmmail.active-org-scope.v1'
const SCOPE_STORAGE_KEY = 'mmmail.active-scope.v2'

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
  const activeScopeId = ref(canUseLocalStorage() ? window.localStorage.getItem(SCOPE_STORAGE_KEY) || '' : '')
  const initialized = ref(false)
  const loading = ref(false)

  const activeScope = computed(() => accessScopes.value.find(scope => scope.orgId === activeOrgId.value) || null)
  const hasScopes = computed(() => accessScopes.value.length > 0)
  const enabledProductSet = computed(() => {
    const items = activeScope.value?.products || []
    return new Set(items.filter(item => item.accessState === 'ENABLED').map(item => item.productKey))
  })

  function applyScopes(scopes: OrgAccessScope[]) {
    const nextActiveOrgId = resolveActiveOrgId(scopes, activeOrgId.value)
    accessScopes.value = scopes
    if (nextActiveOrgId !== activeOrgId.value) {
      setActiveScopeId('')
    }
    activeOrgId.value = nextActiveOrgId
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
    setActiveScopeId('')
    persistActiveOrgId(activeOrgId.value)
  }

  function setActiveScopeId(scopeId: string) {
    activeScopeId.value = scopeId
    if (canUseLocalStorage()) {
      if (scopeId) {
        window.localStorage.setItem(SCOPE_STORAGE_KEY, scopeId)
      } else {
        window.localStorage.removeItem(SCOPE_STORAGE_KEY)
      }
    }
  }

  function setPersonalScope() {
    activeOrgId.value = ''
    setActiveScopeId('')
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
    setActiveScopeId('')
    initialized.value = false
    persistActiveOrgId('')
  }

  return {
    accessScopes,
    activeOrgId,
    activeScope,
    activeScopeId,
    applyScopes,
    clear,
    ensureLoaded,
    hasScopes,
    initialized,
    isProductEnabled,
    loading,
    setActiveOrgId,
    setActiveScopeId,
    setPersonalScope
  }
})
