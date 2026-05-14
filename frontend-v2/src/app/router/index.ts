import { createRouter, createWebHistory } from 'vue-router'
import { registerUnauthorizedHandler } from '@/service/request/http'
import { useAuthStore } from '@/store/modules/auth'
import { resolveAuthRedirect } from './auth-guard'
import { routes } from './routes'

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()
  const redirect = resolveAuthRedirect(to, authStore.isAuthenticated)
  if (redirect) {
    next(redirect)
    return
  }
  next()
})

registerUnauthorizedHandler(() => {
  const authStore = useAuthStore()
  authStore.clearSession()
  const currentRoute = router.currentRoute.value
  if (currentRoute.path !== '/login') {
    void router.replace(`/login?redirect=${encodeURIComponent(currentRoute.fullPath)}`)
  }
})
