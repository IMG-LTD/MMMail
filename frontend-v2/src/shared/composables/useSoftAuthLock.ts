import { computed } from 'vue'
import { useAuthStore } from '@/store/modules/auth'

export function useSoftAuthLock() {
  const authStore = useAuthStore()

  return {
    locked: computed(() => authStore.softAuthLocked),
    lock: () => authStore.setSoftAuthLocked(true),
    unlock: () => authStore.setSoftAuthLocked(false)
  }
}
