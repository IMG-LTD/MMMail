import { useAuthStore } from '~/stores/auth'
import { useAuthApi } from '~/composables/useAuthApi'
import { isPublicEntryRoute, isSharedPublicRoute } from '~/utils/org-product-access'

export default defineNuxtRouteMiddleware(async (to) => {
  const authStore = useAuthStore()

  if (isPublicEntryRoute(to.path) || isSharedPublicRoute(to.path)) {
    return
  }

  if (authStore.isAuthenticated && !authStore.needsSessionRefresh) {
    return
  }

  const { refreshSession } = useAuthApi()
  const recovered = await refreshSession()
  if (recovered) {
    return
  }

  return navigateTo('/login')
})
