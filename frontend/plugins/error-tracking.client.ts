import { useAuthStore } from '~/stores/auth'
import { useSystemApi } from '~/composables/useSystemApi'
import { setupRuntimeErrorTracking } from '~/utils/error-tracking'

export default defineNuxtPlugin(() => {
  if (typeof window === 'undefined') {
    return
  }

  const authStore = useAuthStore()
  const router = useRouter()
  const { reportClientError } = useSystemApi()

  setupRuntimeErrorTracking({
    target: window,
    enabled: () => Boolean(authStore.accessToken),
    resolvePath: () => router.currentRoute.value.fullPath || window.location.pathname,
    report: reportClientError,
  })
})
