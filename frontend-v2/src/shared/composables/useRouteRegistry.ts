import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { redirectRegistry } from '@/app/router/redirect-registry'

export function useRouteRegistry() {
  const route = useRoute()

  const redirectMap = computed(() => new Map(redirectRegistry.map(item => [item.from, item.to])))

  function resolveRedirect(path: string) {
    return redirectMap.value.get(path) || null
  }

  return {
    currentPath: computed(() => route.path),
    redirectRegistry,
    resolveRedirect
  }
}
