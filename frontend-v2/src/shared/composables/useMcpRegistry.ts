import { computed, ref } from 'vue'
import { readMcpRegistryCapabilities } from '@/service/api/mcp-registry'
import { useAuthStore } from '@/store/modules/auth'

export function useMcpRegistry() {
  const authStore = useAuthStore()
  const capabilities = ref<string[]>([])

  async function loadCapabilities() {
    const response = await readMcpRegistryCapabilities(authStore.accessToken)
    capabilities.value = Object.entries(response.data).filter(([, value]) => value).map(([key]) => key)
  }

  return {
    capabilities: computed(() => capabilities.value),
    loadCapabilities
  }
}
