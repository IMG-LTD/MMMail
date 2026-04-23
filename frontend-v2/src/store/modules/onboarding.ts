import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_KEY = 'mmmail.onboarding.v1'

interface PersistedOnboardingState {
  hasSeenGuide: boolean
  completedAt: string | null
  skippedAt: string | null
}

const DEFAULT_STATE: PersistedOnboardingState = {
  hasSeenGuide: false,
  completedAt: null,
  skippedAt: null
}

function canUseLocalStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function normalizeDate(value: unknown) {
  return typeof value === 'string' && value.length > 0 ? value : null
}

function readPersistedState(): PersistedOnboardingState {
  if (!canUseLocalStorage()) {
    return DEFAULT_STATE
  }

  const rawState = window.localStorage.getItem(STORAGE_KEY)
  if (!rawState) {
    return DEFAULT_STATE
  }

  try {
    const parsedState = JSON.parse(rawState) as Partial<PersistedOnboardingState>

    return {
      hasSeenGuide: Boolean(parsedState.hasSeenGuide),
      completedAt: normalizeDate(parsedState.completedAt),
      skippedAt: normalizeDate(parsedState.skippedAt)
    }
  } catch {
    return DEFAULT_STATE
  }
}

function persistState(state: PersistedOnboardingState) {
  if (!canUseLocalStorage()) {
    return
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

export const useOnboardingStore = defineStore('onboarding', () => {
  const restoredState = readPersistedState()
  const hasSeenGuide = ref(restoredState.hasSeenGuide)
  const completedAt = ref<string | null>(restoredState.completedAt)
  const skippedAt = ref<string | null>(restoredState.skippedAt)
  const isGuideOpen = ref(false)

  const shouldAutoOpen = computed(() => !hasSeenGuide.value && !isGuideOpen.value)

  function persistCurrentState() {
    persistState({
      hasSeenGuide: hasSeenGuide.value,
      completedAt: completedAt.value,
      skippedAt: skippedAt.value
    })
  }

  function openGuide() {
    isGuideOpen.value = true
  }

  function closeGuide() {
    isGuideOpen.value = false
  }

  function skipGuide() {
    hasSeenGuide.value = true
    skippedAt.value = new Date().toISOString()
    isGuideOpen.value = false
    persistCurrentState()
  }

  function completeGuide() {
    hasSeenGuide.value = true
    completedAt.value = new Date().toISOString()
    skippedAt.value = null
    isGuideOpen.value = false
    persistCurrentState()
  }

  return {
    closeGuide,
    completedAt,
    completeGuide,
    hasSeenGuide,
    isGuideOpen,
    openGuide,
    shouldAutoOpen,
    skippedAt,
    skipGuide
  }
})
