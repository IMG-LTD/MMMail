<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SuiteBillingCycle, SuiteBillingQuote, SuitePricingOffer } from '~/types/suite-lumo'

interface Props {
  selectedOffer: SuitePricingOffer | null
  selectedBillingCycle: SuiteBillingCycle
  seatCount: number
  organizationName: string
  domainName: string
  quote: SuiteBillingQuote | null
  quoteLoading: boolean
  draftLoading: boolean
  showOrganizationFields: boolean
  refreshQuote: () => Promise<void>
  saveDraft: () => Promise<void>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:selectedBillingCycle': [value: SuiteBillingCycle]
  'update:seatCount': [value: number]
  'update:organizationName': [value: string]
  'update:domainName': [value: string]
}>()
const { locale, t } = useI18n()

const currentLocale = computed(() => locale.value === 'zh-CN'
  ? 'zh-CN'
  : locale.value === 'zh-TW'
    ? 'zh-TW'
    : 'en-US')

function formatMoney(currencyCode: string | null, cents: number): string {
  const currency = currencyCode || 'USD'
  return new Intl.NumberFormat(currentLocale.value, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2
  }).format(cents / 100)
}

function resolveLineTitle(lineCode: string): string {
  const key = `suite.billing.invoiceLine.${lineCode}`
  const translated = t(key)
  return translated === key ? lineCode : translated
}
</script>

<template>
  <section class="mm-card checkout-panel">
    <div class="checkout-grid">
      <article class="checkout-form-card">
        <p class="eyebrow">{{ t('suite.billing.checkout.badge') }}</p>
        <h2 class="mm-section-title">{{ t('suite.billing.checkout.title') }}</h2>
        <p class="mm-muted">{{ t('suite.billing.checkout.subtitle') }}</p>

        <el-empty
          v-if="!props.selectedOffer"
          :description="t('suite.billing.checkout.noOffer')"
          :image-size="72"
        />

        <el-form v-else label-position="top" class="checkout-form">
          <el-form-item :label="t('suite.billing.checkout.fields.offer')">
            <el-input :model-value="`${props.selectedOffer.name} · ${props.selectedOffer.code}`" readonly />
          </el-form-item>

          <el-form-item :label="t('suite.billing.checkout.fields.billingCycle')">
            <el-radio-group
              :model-value="props.selectedBillingCycle"
              @update:model-value="emit('update:selectedBillingCycle', $event as SuiteBillingCycle)"
            >
              <el-radio
                v-for="cycle in props.selectedOffer.billingCycles"
                :key="cycle"
                :value="cycle"
              >
                {{ t(`suite.billing.billingCycle.${cycle}`) }}
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item :label="t('suite.billing.checkout.fields.seatCount')">
            <el-input-number
              :model-value="props.seatCount"
              :min="1"
              :max="500"
              :disabled="!props.selectedOffer.seatEditable"
              @update:model-value="emit('update:seatCount', Number($event || 1))"
            />
          </el-form-item>

          <template v-if="props.showOrganizationFields">
            <el-form-item :label="t('suite.billing.checkout.fields.organizationName')">
              <el-input
                :model-value="props.organizationName"
                :placeholder="t('suite.billing.checkout.placeholders.organizationName')"
                @update:model-value="emit('update:organizationName', String($event || ''))"
              />
            </el-form-item>
            <el-form-item :label="t('suite.billing.checkout.fields.domainName')">
              <el-input
                :model-value="props.domainName"
                :placeholder="t('suite.billing.checkout.placeholders.domainName')"
                @update:model-value="emit('update:domainName', String($event || ''))"
              />
            </el-form-item>
          </template>

          <div class="checkout-actions">
            <el-button :loading="props.quoteLoading" type="primary" @click="void props.refreshQuote()">
              {{ t('suite.billing.checkout.actions.refreshQuote') }}
            </el-button>
            <el-button :loading="props.draftLoading" plain @click="void props.saveDraft()">
              {{ t('suite.billing.checkout.actions.saveDraft') }}
            </el-button>
          </div>
        </el-form>
      </article>

      <article class="quote-card">
        <p class="eyebrow">{{ t('suite.billing.quote.badge') }}</p>
        <h3 class="mm-section-subtitle">{{ t('suite.billing.quote.title') }}</h3>
        <p class="mm-muted">{{ t('suite.billing.quote.subtitle') }}</p>

        <el-empty
          v-if="!props.quote"
          :description="t('suite.billing.quote.empty')"
          :image-size="72"
        />

        <template v-else>
          <div class="quote-top">
            <div>
              <h4 class="quote-name">{{ props.quote.offerName }}</h4>
              <p class="quote-meta">
                {{ t(`suite.billing.quoteStatus.${props.quote.quoteStatus}`) }} ·
                {{ t(`suite.billing.checkoutMode.${props.quote.checkoutMode}`) }}
              </p>
            </div>
            <el-tag v-if="props.quote.marketingBadge" type="warning" effect="plain">
              {{ props.quote.marketingBadge }}
            </el-tag>
          </div>

          <div v-if="props.quote.invoiceSummary" class="invoice-box">
            <div
              v-for="lineItem in props.quote.invoiceSummary.lineItems"
              :key="lineItem.lineCode"
              class="invoice-line"
            >
              <span>{{ resolveLineTitle(lineItem.lineCode) }} × {{ lineItem.quantity }}</span>
              <strong>{{ formatMoney(props.quote.invoiceSummary.currencyCode, lineItem.totalPriceCents) }}</strong>
            </div>
            <div class="invoice-summary">
              <span>{{ t('suite.billing.quote.subtotal') }}</span>
              <strong>{{ formatMoney(props.quote.invoiceSummary.currencyCode, props.quote.invoiceSummary.subtotalCents) }}</strong>
            </div>
            <div class="invoice-summary">
              <span>{{ t('suite.billing.quote.discount') }}</span>
              <strong>{{ formatMoney(props.quote.invoiceSummary.currencyCode, props.quote.invoiceSummary.discountCents) }}</strong>
            </div>
            <div class="invoice-summary invoice-summary--total">
              <span>{{ t('suite.billing.quote.total') }}</span>
              <strong>{{ formatMoney(props.quote.invoiceSummary.currencyCode, props.quote.invoiceSummary.totalCents) }}</strong>
            </div>
          </div>

          <div v-else class="sales-box">
            <el-alert
              :title="t('suite.billing.quote.contactSalesTitle')"
              type="info"
              :closable="false"
              show-icon
            />
          </div>

          <div class="quote-lists">
            <div>
              <h4 class="quote-list-title">{{ t('suite.billing.quote.unlockedProducts') }}</h4>
              <div class="quote-tags">
                <el-tag
                  v-for="productCode in props.quote.entitlementSummary.unlockedProducts"
                  :key="`${props.quote.offerCode}-${productCode}`"
                  effect="plain"
                >
                  {{ productCode }}
                </el-tag>
              </div>
            </div>

            <div>
              <h4 class="quote-list-title">{{ t('suite.billing.quote.nextAction') }}</h4>
              <ul class="quote-checklist">
                <li v-for="checkItem in props.quote.onboardingSummary.checklistCodes" :key="checkItem">
                  {{ checkItem }}
                </li>
              </ul>
            </div>
          </div>
        </template>
      </article>
    </div>
  </section>
</template>

<style scoped>
.checkout-panel {
  --suite-billing-accent: #6d78ff;
  --suite-billing-ink: #132445;
  --suite-billing-muted: #62708d;
  --suite-billing-border: rgba(99, 114, 189, 0.14);
  padding: 24px;
  border: 1px solid var(--suite-billing-border);
  background:
    radial-gradient(circle at bottom left, rgba(109, 120, 255, 0.12), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 247, 255, 0.95));
}

.checkout-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 18px;
}

.checkout-form-card,
.quote-card {
  border-radius: 24px;
  border: 1px solid var(--suite-billing-border);
  background: rgba(255, 255, 255, 0.9);
  padding: 20px;
}

.eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 12px;
  color: var(--suite-billing-accent);
}

.checkout-form {
  margin-top: 16px;
}

.checkout-actions {
  display: flex;
  gap: 12px;
}

.quote-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.quote-name {
  margin: 12px 0 6px;
  color: var(--suite-billing-ink);
}

.quote-meta,
.quote-list-title {
  color: var(--suite-billing-muted);
}

.invoice-box,
.sales-box,
.quote-lists {
  margin-top: 16px;
}

.invoice-box {
  border-radius: 18px;
  border: 1px solid var(--suite-billing-border);
  padding: 14px;
  display: grid;
  gap: 10px;
}

.invoice-line,
.invoice-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--suite-billing-muted);
}

.invoice-summary--total {
  padding-top: 10px;
  border-top: 1px solid var(--suite-billing-border);
  color: var(--suite-billing-ink);
}

.quote-lists {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.quote-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quote-checklist {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 6px;
  color: var(--suite-billing-muted);
}

@media (max-width: 1120px) {
  .checkout-grid,
  .quote-lists {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .checkout-actions,
  .quote-top {
    flex-direction: column;
  }
}
</style>
