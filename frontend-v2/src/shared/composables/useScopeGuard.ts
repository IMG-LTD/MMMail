import { computed } from 'vue'
import { useAuthStore } from '@/store/modules/auth'
import { useOrgAccessStore } from '@/store/modules/org-access'

export function useScopeGuard() {
  const authStore = useAuthStore()
  const orgAccessStore = useOrgAccessStore()

  const requestHeaders = computed(() => {
    const headers: Record<string, string> = {}

    if (orgAccessStore.activeOrgId) {
      headers['X-MMMAIL-ORG-ID'] = orgAccessStore.activeOrgId
    }

    if (orgAccessStore.activeScopeId) {
      headers['X-MMMAIL-SCOPE-ID'] = orgAccessStore.activeScopeId
    }

    return headers
  })

  function canAccessProduct(productKey: Parameters<typeof orgAccessStore.isProductEnabled>[0]) {
    if (!authStore.isAuthenticated) {
      return false
    }

    return orgAccessStore.isProductEnabled(productKey)
  }

  return {
    canAccessProduct,
    requestHeaders
  }
}
