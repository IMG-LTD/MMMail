import { computed, ref } from 'vue'
import { usePreferredDark, useStorage } from '@vueuse/core'
import { defineStore } from 'pinia'
import {
  defaultThemeSettings,
  type ResolvedThemeScheme,
  type ThemeDensity,
  type ThemePresetId,
  type ThemeScheme,
  themeStorageKeys
} from '@/theme/settings'
import { buildMmMailTheme } from '@/theme/tokens'

export const useThemeStore = defineStore('theme', () => {
  const prefersDark = usePreferredDark()
  const drawerOpen = ref(false)
  const density = useStorage<ThemeDensity>(themeStorageKeys.density, defaultThemeSettings.density)
  const radius = useStorage<number>(themeStorageKeys.radius, defaultThemeSettings.radius)
  const themePreset = useStorage<ThemePresetId>(themeStorageKeys.themePreset, defaultThemeSettings.themePreset)
  const themeScheme = useStorage<ThemeScheme>(themeStorageKeys.themeScheme, defaultThemeSettings.themeScheme)

  const resolvedScheme = computed<ResolvedThemeScheme>(() => {
    if (themeScheme.value === 'auto') {
      return prefersDark.value ? 'dark' : 'light'
    }

    return themeScheme.value
  })

  const isDark = computed(() => resolvedScheme.value === 'dark')
  const themeModel = computed(() => {
    return buildMmMailTheme({
      density: density.value,
      preset: themePreset.value,
      radius: radius.value,
      scheme: resolvedScheme.value
    })
  })

  const naiveThemeOverrides = computed(() => themeModel.value.naiveThemeOverrides)

  function setDrawerOpen(value: boolean) {
    drawerOpen.value = value
  }

  function openDrawer() {
    setDrawerOpen(true)
  }

  function closeDrawer() {
    setDrawerOpen(false)
  }

  function setThemeScheme(value: ThemeScheme) {
    themeScheme.value = value
  }

  function setThemePreset(value: ThemePresetId) {
    themePreset.value = value
  }

  function setDensity(value: ThemeDensity) {
    density.value = value
  }

  function setRadius(value: number) {
    radius.value = value
  }

  return {
    closeDrawer,
    density,
    drawerOpen,
    isDark,
    naiveThemeOverrides,
    openDrawer,
    radius,
    resolvedScheme,
    setDensity,
    setDrawerOpen,
    setRadius,
    setThemePreset,
    setThemeScheme,
    themeModel,
    themePreset,
    themeScheme
  }
})
