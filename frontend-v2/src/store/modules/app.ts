import { ref } from 'vue'
import { useStorage } from '@vueuse/core'
import { defineStore } from 'pinia'
import { detectAppLocale, getNextLocale, type AppLocale } from '@/locales'

export const useAppStore = defineStore('app', () => {
  const collapsed = ref(false)
  const activeScope = ref('Enterprise')
  const locale = useStorage<AppLocale>('mmmail-app-locale', detectAppLocale())

  function toggleCollapsed() {
    collapsed.value = !collapsed.value
  }

  function setScope(scope: string) {
    activeScope.value = scope
  }

  function setLocale(value: AppLocale) {
    locale.value = value
  }

  function cycleLocale() {
    locale.value = getNextLocale(locale.value)
  }

  return {
    activeScope,
    cycleLocale,
    collapsed,
    locale,
    setLocale,
    setScope,
    toggleCollapsed
  }
})
