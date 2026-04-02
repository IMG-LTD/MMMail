import { onBeforeUnmount, ref } from 'vue'
import type { MailE2eeRecipientStatus } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMailApi } from '~/composables/useMailApi'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const LOOKUP_DELAY_MS = 250

export function useMailComposeE2ee() {
  const { fetchRecipientE2eeStatus } = useMailApi()
  const { t } = useI18n()

  const recipientE2eeLoading = ref(false)
  const recipientE2eeStatus = ref<MailE2eeRecipientStatus | null>(null)
  const recipientE2eeError = ref('')

  let refreshTimer: ReturnType<typeof setTimeout> | null = null
  let latestRequestId = 0

  function scheduleRecipientE2eeRefresh(toEmail: string, fromEmail: string): void {
    clearRefreshTimer()
    if (!EMAIL_PATTERN.test(toEmail.trim())) {
      resetRecipientE2eeState()
      return
    }
    refreshTimer = setTimeout(() => {
      void refreshRecipientE2ee(toEmail, fromEmail)
    }, LOOKUP_DELAY_MS)
  }

  async function refreshRecipientE2ee(toEmail: string, fromEmail: string): Promise<MailE2eeRecipientStatus | null> {
    const requestId = latestRequestId + 1
    latestRequestId = requestId
    recipientE2eeLoading.value = true
    try {
      const status = await fetchRecipientE2eeStatus(toEmail.trim(), fromEmail.trim() || undefined)
      if (requestId !== latestRequestId) {
        return null
      }
      recipientE2eeStatus.value = status
      recipientE2eeError.value = ''
      return status
    } catch (error) {
      if (requestId !== latestRequestId) {
        return null
      }
      recipientE2eeStatus.value = null
      recipientE2eeError.value = error instanceof Error ? error.message : t('mailCompose.e2ee.loadFailed')
      return null
    } finally {
      if (requestId === latestRequestId) {
        recipientE2eeLoading.value = false
      }
    }
  }

  async function ensureRecipientE2eeStatus(toEmail: string, fromEmail: string): Promise<MailE2eeRecipientStatus | null> {
    clearRefreshTimer()
    if (!EMAIL_PATTERN.test(toEmail.trim())) {
      resetRecipientE2eeState()
      return null
    }
    const status = await refreshRecipientE2ee(toEmail, fromEmail)
    if (!status && recipientE2eeError.value) {
      throw new Error(recipientE2eeError.value)
    }
    return status
  }

  function resetRecipientE2eeState(): void {
    recipientE2eeLoading.value = false
    recipientE2eeStatus.value = null
    recipientE2eeError.value = ''
  }

  function clearRefreshTimer(): void {
    if (refreshTimer) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }
  }

  onBeforeUnmount(() => {
    clearRefreshTimer()
  })

  return {
    recipientE2eeLoading,
    recipientE2eeStatus,
    recipientE2eeError,
    scheduleRecipientE2eeRefresh,
    ensureRecipientE2eeStatus,
    resetRecipientE2eeState
  }
}
