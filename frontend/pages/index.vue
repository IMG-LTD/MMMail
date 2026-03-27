<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import { resolveHomeRoute } from '~/utils/org-product-access'

const authStore = useAuthStore()
const orgAccessStore = useOrgAccessStore()

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
