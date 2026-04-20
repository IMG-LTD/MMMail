import { computed, ref } from 'vue'
import { readPublicShareCapabilities } from '@/service/api/public-share'

export type PublicShareState = 'token-valid' | 'password-required' | 'unlocked' | 'expired' | 'revoked' | 'locked' | 'download-blocked'

export function usePublicShareFlow() {
  const state = ref<PublicShareState>('password-required')
  const password = ref('')
  const loading = ref(false)
  const auditedActions = ref<string[]>([])

  async function loadCapabilities() {
    loading.value = true
    try {
      const response = await readPublicShareCapabilities()
      auditedActions.value = response.data.auditedActions
    } catch {
    } finally {
      loading.value = false
    }
  }

  function unlock() {
    state.value = 'unlocked'
  }

  return {
    auditedActions: computed(() => auditedActions.value),
    loadCapabilities,
    loading: computed(() => loading.value),
    password,
    state,
    unlock
  }
}
