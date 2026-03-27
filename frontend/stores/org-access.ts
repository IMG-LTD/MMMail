import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { OrgProductKey } from '~/types/organization-admin'
import type { OrgAccessScope } from '~/types/org-access'
import { useOrgAccessApi } from '../composables/useOrgAccessApi'
import { useAuthStore } from './auth'

const STORAGE_KEY = 'mmmail.active-org-scope.v1'

function canUseLocalStorage(): boolean {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function readPersistedActiveOrgId(): string {
  if (!canUseLocalStorage()) {
    return ''
  }
  return window.localStorage.getItem(STORAGE_KEY) || ''
}

function persistActiveOrgId(orgId: string): void {
  if (!canUseLocalStorage()) {
    return
  }
  if (!orgId) {
    window.localStorage.removeItem(STORAGE_KEY)
    return
  }
  window.localStorage.setItem(STORAGE_KEY, orgId)
}

function resolveActiveOrgId(scopes: OrgAccessScope[], candidateOrgId: string): string {
  if (candidateOrgId && scopes.some(scope => scope.orgId === candidateOrgId)) {
    return candidateOrgId
  }
  if (scopes.length === 1) {
    return scopes[0].orgId
  }
  return ''
}

export const useOrgAccessStore = defineStore('org-access', () => {
  const authStore = useAuthStore()
  const accessScopes = ref<OrgAccessScope[]>([])
  const activeOrgId = ref(readPersistedActiveOrgId())
  const initialized = ref(false)
  const loading = ref(false)

  const activeScope = computed(() => accessScopes.value.find(scope => scope.orgId === activeOrgId.value) || null)
  const hasScopes = computed(() => accessScopes.value.length > 0)
  const enabledProductSet = computed(() => new Set(
    activeScope.value?.products
      .filter(item => item.accessState === 'ENABLED')
      .map(item => item.productKey) || []
  ))

  function applyScopes(scopes: OrgAccessScope[]): void {
    accessScopes.value = scopes
    activeOrgId.value = resolveActiveOrgId(scopes, activeOrgId.value)
    persistActiveOrgId(activeOrgId.value)
    initialized.value = true
  }

  async function ensureLoaded(force = false): Promise<void> {
    if (!authStore.isAuthenticated) {
      clear()
      return
    }
    if (initialized.value && !force) {
      return
    }
    loading.value = true
    try {
      const { listAccessContext } = useOrgAccessApi()
      applyScopes(await listAccessContext())
    } finally {
      loading.value = false
    }
  }

  function setActiveOrgId(orgId: string): void {
    activeOrgId.value = resolveActiveOrgId(accessScopes.value, orgId)
    persistActiveOrgId(activeOrgId.value)
  }

  function setPersonalScope(): void {
    activeOrgId.value = ''
    persistActiveOrgId('')
  }

  function isProductEnabled(productKey: OrgProductKey): boolean {
    if (!activeScope.value) {
      return true
    }
    return enabledProductSet.value.has(productKey)
  }

  function clear(): void {
    accessScopes.value = []
    activeOrgId.value = ''
    initialized.value = false
    persistActiveOrgId('')
  }

  return {
    accessScopes,
    activeOrgId,
    activeScope,
    hasScopes,
    initialized,
    loading,
    applyScopes,
    ensureLoaded,
    setActiveOrgId,
    setPersonalScope,
    isProductEnabled,
    clear
  }
})
