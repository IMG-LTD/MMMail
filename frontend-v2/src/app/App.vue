<script setup lang="ts">
import { computed, watchEffect } from 'vue'
import { storeToRefs } from 'pinia'
import { NConfigProvider, NDialogProvider, NMessageProvider, NNotificationProvider, darkTheme } from 'naive-ui'
import { RouterView, useRoute } from 'vue-router'
import BlankLayout from '@/layouts/blank-layout/BlankLayout.vue'
import BaseLayout from '@/layouts/base-layout/BaseLayout.vue'
import WelcomeOnboardingModal from '@/shared/components/WelcomeOnboardingModal.vue'
import { getNaiveUiDateLocale, getNaiveUiLocale } from '@/locales'
import { useAppStore } from '@/store/modules/app'
import { useAuthStore } from '@/store/modules/auth'
import { useOnboardingStore } from '@/store/modules/onboarding'
import { useThemeStore } from '@/store/modules/theme'
import { applyThemeVariables } from '@/theme/tokens'

const route = useRoute()
const appStore = useAppStore()
const authStore = useAuthStore()
const onboardingStore = useOnboardingStore()
const themeStore = useThemeStore()
const { locale } = storeToRefs(appStore)

const isBaseLayout = computed(() => route.meta.layout === 'base')

const currentLayout = computed(() => {
  return isBaseLayout.value ? BaseLayout : BlankLayout
})

const currentNaiveTheme = computed(() => {
  return themeStore.isDark ? darkTheme : undefined
})

const currentNaiveLocale = computed(() => getNaiveUiLocale(locale.value))
const currentNaiveDateLocale = computed(() => getNaiveUiDateLocale(locale.value))

watchEffect(() => {
  applyThemeVariables(themeStore.themeModel.cssVars)
  document.documentElement.dataset.themeScheme = themeStore.resolvedScheme
  document.documentElement.lang = locale.value
  document.documentElement.dataset.appLocale = locale.value
  document.body.classList.toggle('density-compact', themeStore.density === 'compact')
  document.body.classList.toggle('density-comfortable', themeStore.density === 'comfortable')
  document.body.classList.toggle('mmmail-dark', themeStore.isDark)
})

watchEffect(() => {
  if (onboardingStore.shouldAutoOpen && authStore.isAuthenticated && isBaseLayout.value) {
    onboardingStore.openGuide()
  }
})
</script>

<template>
  <n-config-provider
    :date-locale="currentNaiveDateLocale"
    :locale="currentNaiveLocale"
    :theme="currentNaiveTheme"
    :theme-overrides="themeStore.naiveThemeOverrides"
  >
    <n-dialog-provider>
      <n-notification-provider>
        <n-message-provider>
          <component :is="currentLayout">
            <router-view />
          </component>
          <welcome-onboarding-modal />
          <div id="sr-live-polite" class="sr-only" role="status" aria-live="polite" aria-atomic="true" />
          <div id="sr-live-assertive" class="sr-only" role="alert" aria-live="assertive" aria-atomic="true" />
        </n-message-provider>
      </n-notification-provider>
    </n-dialog-provider>
  </n-config-provider>
</template>
