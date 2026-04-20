import { computed, ref } from 'vue'

export function useAutomationRunbook() {
  const currentView = ref<'overview' | 'automation' | 'runs'>('overview')

  function setView(view: 'overview' | 'automation' | 'runs') {
    currentView.value = view
  }

  return {
    currentView: computed(() => currentView.value),
    setView
  }
}
