import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import { useOrgAccessStore } from '~/stores/org-access'
import { resolveProductLabelKey } from '~/utils/org-product-access'
import { resolveAuthenticatorRecoveryContext } from '~/utils/org-access-recovery'

export function useAuthenticatorRecovery() {
  const route = useRoute()
  const { t } = useI18n()
  const orgAccessStore = useOrgAccessStore()

  const context = computed(() => resolveAuthenticatorRecoveryContext(route.query))
  const isRecoveryMode = computed(() => context.value.enabled)
  const productLabel = computed(() => {
    if (!context.value.productKey) {
      return ''
    }
    return t(resolveProductLabelKey(context.value.productKey))
  })

  async function prepareRecoveryScope(): Promise<void> {
    if (!isRecoveryMode.value) {
      return
    }
    orgAccessStore.setPersonalScope()
  }

  async function restoreAccess(): Promise<void> {
    const target = context.value.returnTo || '/inbox'
    if (context.value.restoreOrgId) {
      await orgAccessStore.ensureLoaded(true)
      orgAccessStore.setActiveOrgId(context.value.restoreOrgId)
    }
    await navigateTo(target)
  }

  return {
    context,
    isRecoveryMode,
    productLabel,
    prepareRecoveryScope,
    restoreAccess
  }
}
