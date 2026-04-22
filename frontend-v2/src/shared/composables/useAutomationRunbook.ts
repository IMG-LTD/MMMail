import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type AutomationRunbookView = 'overview' | 'automation' | 'runs'

const automationRunbookViews: AutomationRunbookView[] = ['overview', 'automation', 'runs']

function isAutomationRunbookView(value: string): value is AutomationRunbookView {
  return automationRunbookViews.includes(value as AutomationRunbookView)
}

export function useAutomationRunbook() {
  const route = useRoute()
  const router = useRouter()

  const currentView = computed<AutomationRunbookView>(() => {
    const viewQuery = Array.isArray(route.query.view) ? route.query.view[0] : route.query.view
    const requestedView = typeof viewQuery === 'string' ? viewQuery : ''
    return isAutomationRunbookView(requestedView) ? requestedView : 'overview'
  })

  function setView(view: AutomationRunbookView) {
    void router.replace({
      path: route.path,
      query: {
        ...route.query,
        view
      }
    })
  }

  return {
    currentView,
    setView
  }
}
