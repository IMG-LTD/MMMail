import { computed, ref } from 'vue'
import { readPublicShareCapabilities, type PublicShareCapabilityState } from '@/service/api/public-share'

export type PublicShareState = PublicShareCapabilityState

export function usePublicShareFlow() {
  const state = ref<PublicShareState>('password-required')
  const password = ref('')
  const loading = ref(false)
  const auditedActions = ref<string[]>([])
  const capabilityStates = ref<PublicShareCapabilityState[]>([])
  const passwordHeader = ref('')

  async function loadCapabilities() {
    loading.value = true
    try {
      const response = await readPublicShareCapabilities()
      auditedActions.value = response.data.auditedActions
      capabilityStates.value = response.data.states
      passwordHeader.value = response.data.passwordHeader
    } catch {
      auditedActions.value = []
      capabilityStates.value = []
      passwordHeader.value = ''
    } finally {
      loading.value = false
    }
  }

  function unlock() {
    state.value = 'unlocked'
  }

  return {
    auditedActions: computed(() => auditedActions.value),
    capabilityStates: computed(() => capabilityStates.value),
    loadCapabilities,
    loading: computed(() => loading.value),
    password,
    passwordHeader: computed(() => passwordHeader.value),
    state,
    unlock
  }
}
