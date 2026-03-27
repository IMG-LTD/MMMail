import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useSuiteApi } from '~/composables/useSuiteApi'
import { useOrgAccessStore } from '~/stores/org-access'
import type { SuiteProductItem } from '~/types/api'
import type { SuitePlan, SuitePlanCode, SuiteSubscription } from '~/types/suite-lumo'
import { filterSuiteProductsByAccess } from '~/utils/org-product-surface-filter'
import { buildSuiteCommandSearchSummary } from '~/utils/suite-scope'
import {
  buildSuitePlanCatalogSections,
  buildSuiteJourneyQuotaRows,
  buildSuitePlanUsageRows,
  buildSuiteUpgradeSummary
} from '~/utils/suite-plans'

function messageFromError(error: unknown, fallbackMessage: string): string {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }
  return fallbackMessage
}

export function useSuitePlansWorkspace() {
  const { t } = useI18n()
  const orgAccessStore = useOrgAccessStore()
  const { changePlan, getSubscription, listPlans, listProducts } = useSuiteApi()

  const loading = ref(false)
  const changingPlan = ref(false)
  const plans = ref<SuitePlan[]>([])
  const subscription = ref<SuiteSubscription | null>(null)
  const products = ref<SuiteProductItem[]>([])

  const visibleProducts = computed(() => filterSuiteProductsByAccess(
    products.value,
    orgAccessStore.isProductEnabled
  ))
  const visibleProductDescriptors = computed(() => {
    return visibleProducts.value.map(item => ({
      code: item.code,
      name: item.name,
      enabledByPlan: item.enabledByPlan
    }))
  })
  const productColumns = computed(() => {
    return visibleProducts.value.map(item => ({
      code: item.code,
      name: item.name
    }))
  })
  const usageRows = computed(() => {
    return buildSuitePlanUsageRows(subscription.value, visibleProductDescriptors.value)
  })
  const commandSearchSummary = computed(() => {
    return buildSuiteCommandSearchSummary(visibleProductDescriptors.value)
  })
  const showDriveEntityUsage = computed(() => {
    return visibleProducts.value.some(item => item.code === 'DRIVE')
  })
  const upgradeSummary = computed(() => {
    return buildSuiteUpgradeSummary(subscription.value, plans.value, visibleProductDescriptors.value)
  })
  const planSections = computed(() => buildSuitePlanCatalogSections(plans.value))

  async function loadPlanJourneyData(): Promise<void> {
    loading.value = true
    try {
      const [planList, nextSubscription, productList] = await Promise.all([
        listPlans(),
        getSubscription(),
        listProducts()
      ])
      plans.value = planList
      subscription.value = nextSubscription
      products.value = productList
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.plans.loadFailed')))
    } finally {
      loading.value = false
    }
  }

  function planQuotaRows(plan: SuitePlan) {
    return buildSuiteJourneyQuotaRows(plan, visibleProductDescriptors.value)
  }

  function isCurrentPlan(planCode: SuitePlanCode): boolean {
    return subscription.value?.planCode === planCode
  }

  function resolveSubscriptionStatusLabel(status: string): string {
    const translationKey = `suite.subscriptionStatus.${status}`
    const translated = t(translationKey)
    return translated === translationKey ? status : translated
  }

  async function onChangePlan(planCode: SuitePlanCode): Promise<boolean> {
    if (isCurrentPlan(planCode)) {
      return false
    }
    try {
      await ElMessageBox.confirm(
        t('suite.plans.changeConfirmMessage', { plan: planCode }),
        t('suite.plans.changeConfirmTitle'),
        {
          type: 'warning',
          confirmButtonText: t('common.actions.confirm'),
          cancelButtonText: t('common.actions.cancel')
        }
      )
    } catch {
      return false
    }

    changingPlan.value = true
    try {
      subscription.value = await changePlan({ planCode })
      const [refreshedPlans, refreshedProducts] = await Promise.all([
        listPlans(),
        listProducts()
      ])
      plans.value = refreshedPlans
      products.value = refreshedProducts
      ElMessage.success(t('suite.plans.changeSuccess'))
      return true
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.plans.changeFailed')))
      return false
    } finally {
      changingPlan.value = false
    }
  }

  onMounted(() => {
    void loadPlanJourneyData()
  })

  watch(
    () => orgAccessStore.activeOrgId,
    (nextOrgId, previousOrgId) => {
      if (nextOrgId === previousOrgId) {
        return
      }
      void loadPlanJourneyData()
    }
  )

  return {
    loading,
    changingPlan,
    plans,
    subscription,
    products,
    visibleProducts,
    productColumns,
    usageRows,
    commandSearchSummary,
    showDriveEntityUsage,
    upgradeSummary,
    planSections,
    loadPlanJourneyData,
    planQuotaRows,
    isCurrentPlan,
    resolveSubscriptionStatusLabel,
    onChangePlan
  }
}
