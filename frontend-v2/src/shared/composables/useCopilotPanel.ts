import { computed, ref } from 'vue'
import { readAiPlatformCapabilities } from '@/service/api/ai-platform'
import { useAuthStore } from '@/store/modules/auth'

export function useCopilotPanel() {
  const authStore = useAuthStore()
  const open = ref(false)
  const runStates = ref<string[]>([])

  async function loadCapabilities() {
    const response = await readAiPlatformCapabilities(authStore.accessToken)
    runStates.value = response.data.runStates
  }

  return {
    loadCapabilities,
    open,
    runStates: computed(() => runStates.value),
    toggle: () => {
      open.value = !open.value
    }
  }
}
