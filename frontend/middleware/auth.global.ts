import { useAuthStore } from '~/stores/auth'
import { useAuthApi } from '~/composables/useAuthApi'

const publicRoutes = new Set<string>(['/login', '/register'])

export default defineNuxtRouteMiddleware(async (to) => {
  const authStore = useAuthStore()

  if (
    publicRoutes.has(to.path)
    || to.path.startsWith('/public/drive/shares/')
    || to.path.startsWith('/share/mail/')
    || to.path.startsWith('/meet/join/')
  ) {
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
