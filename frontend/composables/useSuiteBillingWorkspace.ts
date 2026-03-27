import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useSuiteBillingApi } from '~/composables/useSuiteBillingApi'
import { useOrgAccessStore } from '~/stores/org-access'
import type {
  CreateSuiteBillingQuoteRequest,
  CreateSuiteCheckoutDraftRequest,
  SuiteBillingCycle,
  SuiteBillingOverview,
  SuiteBillingQuote,
  SuiteCheckoutDraft,
  SuiteOfferCode,
  SuitePricingOffer
} from '~/types/suite-lumo'
import {
  buildSuitePricingSections,
  buildSuiteQuoteFromDraft,
  pickDefaultSuiteOfferCode
} from '~/utils/suite-billing'

function messageFromError(error: unknown, fallbackMessage: string): string {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }
  return fallbackMessage
}

function buildQuotePayload(
  offerCode: SuiteOfferCode,
  billingCycle: SuiteBillingCycle,
  seatCount: number
): CreateSuiteBillingQuoteRequest {
  return { offerCode, billingCycle, seatCount }
}

function buildDraftPayload(
  offerCode: SuiteOfferCode,
  billingCycle: SuiteBillingCycle,
  seatCount: number,
  organizationName: string,
  domainName: string
): CreateSuiteCheckoutDraftRequest {
  return {
    offerCode,
    billingCycle,
    seatCount,
    organizationName: organizationName.trim() || undefined,
    domainName: domainName.trim() || undefined
  }
}

export function useSuiteBillingWorkspace() {
  const { t } = useI18n()
  const orgAccessStore = useOrgAccessStore()
  const { createBillingQuote, getBillingOverview, listPricingOffers, saveCheckoutDraft } = useSuiteBillingApi()

  const loading = ref(false)
  const quoteLoading = ref(false)
  const draftLoading = ref(false)
  const offers = ref<SuitePricingOffer[]>([])
  const overview = ref<SuiteBillingOverview | null>(null)
  const quote = ref<SuiteBillingQuote | null>(null)
  const selectedOfferCode = ref<SuiteOfferCode | null>(null)
  const selectedBillingCycle = ref<SuiteBillingCycle>('ANNUAL')
  const seatCount = ref(1)
  const organizationName = ref('')
  const domainName = ref('')

  const selectedOffer = computed(() => {
    return offers.value.find(offer => offer.code === selectedOfferCode.value) ?? null
  })
  const pricingSections = computed(() => buildSuitePricingSections(offers.value))
  const showOrganizationFields = computed(() => {
    return selectedOffer.value?.organizationRequired ?? false
  })

  function clearQuote(): void {
    quote.value = null
  }

  function applySelection(offer: SuitePricingOffer, draft?: SuiteCheckoutDraft | null): void {
    selectedOfferCode.value = offer.code
    selectedBillingCycle.value = draft?.billingCycle ?? offer.defaultBillingCycle
    seatCount.value = draft?.seatCount ?? offer.defaultSeatCount
    organizationName.value = draft?.organizationName ?? ''
    domainName.value = draft?.domainName ?? ''
    quote.value = buildSuiteQuoteFromDraft(draft ?? null)
  }

  async function loadBillingData(): Promise<void> {
    loading.value = true
    try {
      const [nextOffers, nextOverview] = await Promise.all([listPricingOffers(), getBillingOverview()])
      offers.value = nextOffers
      overview.value = nextOverview
      const defaultOfferCode = pickDefaultSuiteOfferCode(nextOffers, nextOverview)
      if (!defaultOfferCode) {
        clearQuote()
        return
      }
      const nextOffer = nextOffers.find(offer => offer.code === defaultOfferCode)
      if (!nextOffer) {
        clearQuote()
        return
      }
      const draft = nextOverview.latestDraft?.offerCode === nextOffer.code ? nextOverview.latestDraft : null
      applySelection(nextOffer, draft)
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.billing.messages.loadFailed')))
    } finally {
      loading.value = false
    }
  }

  async function refreshQuote(): Promise<void> {
    if (!selectedOffer.value) {
      return
    }
    quoteLoading.value = true
    try {
      quote.value = await createBillingQuote(buildQuotePayload(
        selectedOffer.value.code,
        selectedBillingCycle.value,
        seatCount.value
      ))
      ElMessage.success(t('suite.billing.messages.quoteReady'))
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.billing.messages.quoteFailed')))
    } finally {
      quoteLoading.value = false
    }
  }

  async function saveDraft(): Promise<void> {
    if (!selectedOffer.value) {
      return
    }
    draftLoading.value = true
    try {
      const nextDraft = await saveCheckoutDraft(buildDraftPayload(
        selectedOffer.value.code,
        selectedBillingCycle.value,
        seatCount.value,
        organizationName.value,
        domainName.value
      ))
      quote.value = buildSuiteQuoteFromDraft(nextDraft)
      overview.value = await getBillingOverview()
      ElMessage.success(t('suite.billing.messages.draftSaved'))
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.billing.messages.draftFailed')))
    } finally {
      draftLoading.value = false
    }
  }

  async function onSelectOffer(offerCode: SuiteOfferCode): Promise<void> {
    const nextOffer = offers.value.find(offer => offer.code === offerCode)
    if (!nextOffer) {
      return
    }
    const draft = overview.value?.latestDraft?.offerCode === offerCode ? overview.value.latestDraft : null
    applySelection(nextOffer, draft)
    await refreshQuote()
  }

  async function restoreLatestDraft(): Promise<void> {
    const latestDraft = overview.value?.latestDraft
    if (!latestDraft) {
      return
    }
    const draftOffer = offers.value.find(offer => offer.code === latestDraft.offerCode)
    if (!draftOffer) {
      return
    }
    applySelection(draftOffer, latestDraft)
  }

  onMounted(() => {
    void loadBillingData()
  })

  watch([selectedBillingCycle, seatCount], () => {
    clearQuote()
  })

  watch(
    () => orgAccessStore.activeOrgId,
    (nextOrgId, previousOrgId) => {
      if (nextOrgId === previousOrgId) {
        return
      }
      void loadBillingData()
    }
  )

  return {
    loading,
    quoteLoading,
    draftLoading,
    offers,
    overview,
    quote,
    selectedOfferCode,
    selectedBillingCycle,
    seatCount,
    organizationName,
    domainName,
    selectedOffer,
    pricingSections,
    showOrganizationFields,
    loadBillingData,
    refreshQuote,
    saveDraft,
    onSelectOffer,
    restoreLatestDraft
  }
}
