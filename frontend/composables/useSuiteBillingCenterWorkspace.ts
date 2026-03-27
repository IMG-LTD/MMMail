import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useSuiteBillingCenterApi } from '~/composables/useSuiteBillingCenterApi'
import type {
  CreateSuiteBillingPaymentMethodRequest,
  SuiteBillingActionCode,
  SuiteBillingCenter
} from '~/types/suite-lumo'

function messageFromError(error: unknown, fallbackMessage: string): string {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }
  return fallbackMessage
}

export function useSuiteBillingCenterWorkspace() {
  const { t } = useI18n()
  const {
    getBillingCenter,
    addPaymentMethod: addPaymentMethodRequest,
    setDefaultPaymentMethod: setDefaultPaymentMethodRequest,
    executeSubscriptionAction: executeSubscriptionActionRequest
  } = useSuiteBillingCenterApi()

  const loading = ref(false)
  const paymentMethodSaving = ref(false)
  const actionLoadingCode = ref<SuiteBillingActionCode | null>(null)
  const center = ref<SuiteBillingCenter | null>(null)

  async function loadBillingCenter(): Promise<void> {
    loading.value = true
    try {
      center.value = await getBillingCenter()
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.billing.center.messages.loadFailed')))
    } finally {
      loading.value = false
    }
  }

  async function addPaymentMethod(payload: CreateSuiteBillingPaymentMethodRequest): Promise<boolean> {
    paymentMethodSaving.value = true
    try {
      center.value = await addPaymentMethodRequest(payload)
      ElMessage.success(t('suite.billing.center.messages.paymentMethodSaved'))
      return true
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.billing.center.messages.paymentMethodFailed')))
      return false
    } finally {
      paymentMethodSaving.value = false
    }
  }

  async function setDefaultPaymentMethod(paymentMethodId: number): Promise<boolean> {
    actionLoadingCode.value = null
    try {
      center.value = await setDefaultPaymentMethodRequest({ paymentMethodId })
      ElMessage.success(t('suite.billing.center.messages.defaultUpdated'))
      return true
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.billing.center.messages.defaultUpdateFailed')))
      return false
    }
  }

  async function executeSubscriptionAction(actionCode: SuiteBillingActionCode): Promise<boolean> {
    actionLoadingCode.value = actionCode
    try {
      center.value = await executeSubscriptionActionRequest({ actionCode })
      ElMessage.success(t(`suite.billing.center.messages.actionSuccess.${actionCode}`))
      return true
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.billing.center.messages.actionFailed')))
      return false
    } finally {
      actionLoadingCode.value = null
    }
  }

  onMounted(() => {
    void loadBillingCenter()
  })

  return {
    loading,
    paymentMethodSaving,
    actionLoadingCode,
    center,
    loadBillingCenter,
    addPaymentMethod,
    setDefaultPaymentMethod,
    executeSubscriptionAction
  }
}
