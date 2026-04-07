import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import { buildMailAddressBlockedQuery } from '~/utils/org-access-recovery'
import { isProductEnabledForMailAddressMode, resolveProductKeyFromPath } from '~/utils/org-product-access'

const PUBLIC_ROUTES = new Set(['/login', '/register', '/product-access-blocked'])

export default defineNuxtRouteMiddleware(async (to) => {
  if (
    PUBLIC_ROUTES.has(to.path)
    || to.path.startsWith('/public/drive/shares/')
    || to.path.startsWith('/share/mail/')
    || to.path.startsWith('/meet/join/')
  ) {
    return
  }

  const authStore = useAuthStore()
  const orgAccessStore = useOrgAccessStore()

  if (!authStore.isAuthenticated) {
    orgAccessStore.clear()
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
