import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import { buildMailAddressBlockedQuery } from '~/utils/org-access-recovery'
import {
  isProductEnabledForMailAddressMode,
  isPublicEntryRoute,
  isSharedPublicRoute,
  resolveProductKeyFromPath
} from '~/utils/org-product-access'

export default defineNuxtRouteMiddleware(async (to) => {
  const authStore = useAuthStore()
  const orgAccessStore = useOrgAccessStore()

  if (!authStore.isAuthenticated) {
    orgAccessStore.clear()
    return
  }

  if (to.path === '/product-access-blocked' || isPublicEntryRoute(to.path) || isSharedPublicRoute(to.path)) {
    return
  }

  await orgAccessStore.ensureLoaded()

  const productKey = resolveProductKeyFromPath(to.path)
  if (!productKey) {
    return
  }

  const orgEnabled = orgAccessStore.isProductEnabled(productKey)
  const accountEnabled = isProductEnabledForMailAddressMode(productKey, authStore.user?.mailAddressMode)
  if (orgEnabled && accountEnabled) {
    return
  }

  return navigateTo({
    path: '/product-access-blocked',
    query: accountEnabled
      ? {
          productKey,
          from: to.fullPath,
          orgId: orgAccessStore.activeOrgId || undefined,
          orgName: orgAccessStore.activeScope?.orgName || undefined
        }
      : buildMailAddressBlockedQuery({
          from: to.fullPath,
          productKey
        })
  })
})
