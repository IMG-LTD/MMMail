<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import { resolveHomeRoute } from '~/utils/org-product-access'

const authStore = useAuthStore()
const orgAccessStore = useOrgAccessStore()
const { t } = useI18n()

useHead(() => ({
  title: t('page.suite.title')
}))

if (authStore.isAuthenticated) {
  await orgAccessStore.ensureLoaded()
  await navigateTo(resolveHomeRoute(orgAccessStore.isProductEnabled, authStore.user?.mailAddressMode))
} else {
  await navigateTo('/login')
}
</script>

<template>
  <div />
</template>
