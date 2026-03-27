<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type {
  CreateSuiteBillingPaymentMethodRequest,
  SuiteBillingActionCode,
  SuiteBillingCenter,
  SuiteBillingPaymentMethodType,
  SuiteBillingSubscriptionAction
} from '~/types/suite-lumo'
import {
  countPendingSuiteInvoices,
  isSuiteBillingActionRunnable,
  pickDefaultSuitePaymentMethod
} from '~/utils/suite-billing-center'

interface Props {
  center: SuiteBillingCenter | null
  paymentMethodSaving: boolean
  actionLoadingCode: SuiteBillingActionCode | null
  addPaymentMethod: (payload: CreateSuiteBillingPaymentMethodRequest) => Promise<boolean>
  setDefaultPaymentMethod: (paymentMethodId: number) => Promise<boolean>
  executeSubscriptionAction: (actionCode: SuiteBillingActionCode) => Promise<boolean>
}

const props = defineProps<Props>()
const { locale, t } = useI18n()

const paymentMethodType = ref<SuiteBillingPaymentMethodType>('CARD')
const displayLabel = ref('')
const brand = ref('VISA')
const lastFour = ref('')
const expiresAt = ref('')
const makeDefault = ref(true)

const defaultPaymentMethod = computed(() => {
  return pickDefaultSuitePaymentMethod(props.center?.paymentMethods ?? [])
})

const pendingInvoiceCount = computed(() => {
  return countPendingSuiteInvoices(props.center?.invoices ?? [])
})

const isCardMethod = computed(() => paymentMethodType.value === 'CARD')

const paymentMethodTypeOptions: ReadonlyArray<SuiteBillingPaymentMethodType> = ['CARD', 'PAYPAL', 'BITCOIN', 'CASH']

function formatMoney(currencyCode: string, amountCents: number): string {
  return new Intl.NumberFormat(locale.value, {
    style: 'currency',
    currency: currencyCode,
    minimumFractionDigits: 2
  }).format(amountCents / 100)
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return '—'
  }
  return new Intl.DateTimeFormat(locale.value, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value))
}

function actionDescription(action: SuiteBillingSubscriptionAction): string {
  if (action.reasonCode) {
    return t(`suite.billing.center.actions.reason.${action.reasonCode}`)
  }
  return t(`suite.billing.center.actions.${action.actionCode}.description`, {
    offerName: action.targetOfferName || '—',
    effectiveAt: formatDateTime(action.effectiveAt)
  })
}

async function onAddPaymentMethod(): Promise<void> {
  const saved = await props.addPaymentMethod({
    methodType: paymentMethodType.value,
    displayLabel: displayLabel.value,
    brand: isCardMethod.value ? brand.value : undefined,
    lastFour: isCardMethod.value ? lastFour.value : undefined,
    expiresAt: isCardMethod.value ? expiresAt.value : undefined,
    makeDefault: makeDefault.value
  })
  if (!saved) {
    return
  }
  displayLabel.value = ''
  lastFour.value = ''
  expiresAt.value = ''
  makeDefault.value = true
}
</script>

<template>
  <section class="mm-card billing-center-panel">
    <div class="center-head">
      <div>
        <p class="eyebrow">{{ t('suite.billing.center.badge') }}</p>
        <h2 class="mm-section-title">{{ t('suite.billing.center.title') }}</h2>
        <p class="mm-muted">{{ t('suite.billing.center.subtitle') }}</p>
      </div>
      <div class="head-metrics">
        <el-tag effect="plain" type="primary">
          {{ t('suite.billing.center.pendingInvoices', { count: pendingInvoiceCount }) }}
        </el-tag>
        <el-tag v-if="defaultPaymentMethod" effect="plain">
          {{ defaultPaymentMethod.displayLabel }}
        </el-tag>
      </div>
    </div>

    <div class="center-grid">
      <article class="center-card">
        <div class="card-head">
          <div>
            <h3 class="card-title">{{ t('suite.billing.center.paymentMethods.title') }}</h3>
            <p class="mm-muted">{{ t('suite.billing.center.paymentMethods.subtitle') }}</p>
          </div>
        </div>

        <div v-if="props.center?.paymentMethods.length" class="payment-method-list">
          <div
            v-for="paymentMethod in props.center.paymentMethods"
            :key="paymentMethod.id"
            class="payment-method-row"
          >
            <div>
              <div class="payment-method-label">
                <strong>{{ paymentMethod.displayLabel }}</strong>
                <el-tag
                  v-if="paymentMethod.defaultMethod"
                  size="small"
                  effect="dark"
                  type="success"
                >
                  {{ t('suite.billing.center.paymentMethods.defaultBadge') }}
                </el-tag>
              </div>
              <p class="mm-muted payment-method-meta">
                {{ t(`suite.billing.center.paymentMethods.methodType.${paymentMethod.methodType}`) }}
                <template v-if="paymentMethod.lastFour">
                  · {{ t('suite.billing.center.paymentMethods.lastFour', { value: paymentMethod.lastFour }) }}
                </template>
                <template v-if="paymentMethod.expiresAt">
                  · {{ t('suite.billing.center.paymentMethods.expiresAt', { value: paymentMethod.expiresAt }) }}
                </template>
              </p>
            </div>
            <el-button
              v-if="!paymentMethod.defaultMethod"
              plain
              size="small"
              @click="void props.setDefaultPaymentMethod(paymentMethod.id)"
            >
              {{ t('suite.billing.center.paymentMethods.actions.setDefault') }}
            </el-button>
          </div>
        </div>
        <el-empty
          v-else
          :description="t('suite.billing.center.paymentMethods.empty')"
          :image-size="64"
        />

        <el-divider />

        <el-form label-position="top" class="payment-form">
          <el-form-item :label="t('suite.billing.center.paymentMethods.form.methodType')">
            <el-select v-model="paymentMethodType">
              <el-option
                v-for="methodType in paymentMethodTypeOptions"
                :key="methodType"
                :label="t(`suite.billing.center.paymentMethods.methodType.${methodType}`)"
                :value="methodType"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('suite.billing.center.paymentMethods.form.displayLabel')">
            <el-input
              v-model="displayLabel"
              :placeholder="t('suite.billing.center.paymentMethods.form.placeholders.displayLabel')"
            />
          </el-form-item>
          <template v-if="isCardMethod">
            <el-form-item :label="t('suite.billing.center.paymentMethods.form.brand')">
              <el-input v-model="brand" :placeholder="t('suite.billing.center.paymentMethods.form.placeholders.brand')" />
            </el-form-item>
            <div class="payment-form-inline">
              <el-form-item :label="t('suite.billing.center.paymentMethods.form.lastFour')">
                <el-input v-model="lastFour" maxlength="4" :placeholder="t('suite.billing.center.paymentMethods.form.placeholders.lastFour')" />
              </el-form-item>
              <el-form-item :label="t('suite.billing.center.paymentMethods.form.expiresAt')">
                <el-input v-model="expiresAt" maxlength="7" :placeholder="t('suite.billing.center.paymentMethods.form.placeholders.expiresAt')" />
              </el-form-item>
            </div>
          </template>
          <el-checkbox v-model="makeDefault">
            {{ t('suite.billing.center.paymentMethods.form.makeDefault') }}
          </el-checkbox>
          <el-button
            type="primary"
            class="payment-form-button"
            :loading="props.paymentMethodSaving"
            @click="void onAddPaymentMethod()"
          >
            {{ t('suite.billing.center.paymentMethods.actions.add') }}
          </el-button>
        </el-form>
      </article>

      <article class="center-card">
        <div class="card-head">
          <div>
            <h3 class="card-title">{{ t('suite.billing.center.subscription.title') }}</h3>
            <p class="mm-muted">{{ t('suite.billing.center.subscription.subtitle') }}</p>
          </div>
        </div>

        <dl v-if="props.center" class="subscription-grid">
          <div>
            <dt>{{ t('suite.billing.center.subscription.activePlan') }}</dt>
            <dd>{{ props.center.subscriptionSummary.activePlanName }}</dd>
          </div>
          <div>
            <dt>{{ t('suite.billing.center.subscription.billingCycle') }}</dt>
            <dd>
              {{ props.center.subscriptionSummary.billingCycle
                ? t(`suite.billing.billingCycle.${props.center.subscriptionSummary.billingCycle}`)
                : t('suite.billing.center.subscription.noBillingCycle') }}
            </dd>
          </div>
          <div>
            <dt>{{ t('suite.billing.center.subscription.seatCount') }}</dt>
            <dd>{{ props.center.subscriptionSummary.seatCount }}</dd>
          </div>
          <div>
            <dt>{{ t('suite.billing.center.subscription.autoRenew') }}</dt>
            <dd>{{ t(`suite.billing.center.subscription.autoRenewValue.${props.center.subscriptionSummary.autoRenew ? 'ON' : 'OFF'}`) }}</dd>
          </div>
          <div>
            <dt>{{ t('suite.billing.center.subscription.currentPeriodEndsAt') }}</dt>
            <dd>{{ formatDateTime(props.center.subscriptionSummary.currentPeriodEndsAt) }}</dd>
          </div>
          <div>
            <dt>{{ t('suite.billing.center.subscription.defaultPaymentMethod') }}</dt>
            <dd>{{ props.center.subscriptionSummary.defaultPaymentMethodLabel || '—' }}</dd>
          </div>
        </dl>

        <div
          v-if="props.center?.subscriptionSummary.pendingActionCode"
          class="pending-action-box"
        >
          <p class="pending-action-label">{{ t('suite.billing.center.subscription.pendingAction') }}</p>
          <strong>
            {{ t(`suite.billing.center.actions.${props.center.subscriptionSummary.pendingActionCode}.title`) }}
          </strong>
          <p class="mm-muted">
            {{ props.center.subscriptionSummary.pendingOfferName || '—' }} ·
            {{ formatDateTime(props.center.subscriptionSummary.pendingEffectiveAt) }}
          </p>
        </div>

        <div class="action-list">
          <div
            v-for="action in props.center?.availableActions || []"
            :key="action.actionCode"
            class="action-row"
          >
            <div>
              <div class="action-title-row">
                <strong>{{ t(`suite.billing.center.actions.${action.actionCode}.title`) }}</strong>
                <el-tag size="small" effect="plain">
                  {{ t(`suite.billing.center.actionStatus.${action.actionStatus}`) }}
                </el-tag>
              </div>
              <p class="mm-muted">{{ actionDescription(action) }}</p>
            </div>
            <el-button
              size="small"
              :disabled="!isSuiteBillingActionRunnable(action)"
              :loading="props.actionLoadingCode === action.actionCode"
              @click="void props.executeSubscriptionAction(action.actionCode)"
            >
              {{ t(`suite.billing.center.actions.${action.actionCode}.cta`) }}
            </el-button>
          </div>
        </div>
      </article>

      <article class="center-card">
        <div class="card-head">
          <div>
            <h3 class="card-title">{{ t('suite.billing.center.invoices.title') }}</h3>
            <p class="mm-muted">{{ t('suite.billing.center.invoices.subtitle') }}</p>
          </div>
        </div>

        <div v-if="props.center?.invoices.length" class="invoice-list">
          <div
            v-for="invoice in props.center.invoices"
            :key="invoice.invoiceNumber"
            class="invoice-row"
          >
            <div>
              <div class="invoice-title-row">
                <strong>{{ invoice.offerName }}</strong>
                <el-tag size="small" effect="plain" type="warning">
                  {{ t(`suite.billing.center.invoices.status.${invoice.invoiceStatus}`) }}
                </el-tag>
              </div>
              <p class="mm-muted">
                {{ invoice.invoiceNumber }} ·
                {{ invoice.billingCycle ? t(`suite.billing.billingCycle.${invoice.billingCycle}`) : '—' }} ·
                {{ t('suite.billing.center.invoices.seatCount', { count: invoice.seatCount }) }}
              </p>
            </div>
            <div class="invoice-meta">
              <strong>{{ formatMoney(invoice.currencyCode, invoice.totalCents) }}</strong>
              <span class="mm-muted">{{ formatDateTime(invoice.issuedAt) }}</span>
              <span class="download-code">{{ t('suite.billing.center.invoices.downloadCode', { code: invoice.downloadCode }) }}</span>
            </div>
          </div>
        </div>
        <el-empty
          v-else
          :description="t('suite.billing.center.invoices.empty')"
          :image-size="64"
        />
      </article>
    </div>
  </section>
</template>

<style scoped>
.billing-center-panel {
  --suite-billing-accent: #6d78ff;
  --suite-billing-ink: #122240;
  --suite-billing-muted: #64728c;
  --suite-billing-border: rgba(99, 113, 188, 0.14);
  padding: 24px;
  border: 1px solid var(--suite-billing-border);
  background:
    radial-gradient(circle at top right, rgba(109, 120, 255, 0.16), transparent 36%),
    linear-gradient(180deg, rgba(246, 248, 255, 0.98), rgba(255, 255, 255, 0.98));
}

.center-head,
.card-head,
.payment-method-row,
.action-row,
.invoice-row,
.invoice-meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.center-head,
.card-head {
  align-items: flex-start;
}

.head-metrics,
.payment-method-label,
.action-title-row,
.invoice-title-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 12px;
  color: var(--suite-billing-accent);
}

.center-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.center-card {
  border-radius: 22px;
  border: 1px solid var(--suite-billing-border);
  background: rgba(255, 255, 255, 0.88);
  padding: 18px;
}

.card-title,
.pending-action-box strong {
  margin: 0;
  color: var(--suite-billing-ink);
}

.payment-method-list,
.action-list,
.invoice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.payment-method-row,
.action-row,
.invoice-row {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--suite-billing-border);
  background: rgba(244, 247, 255, 0.78);
  align-items: center;
}

.payment-method-meta,
.invoice-meta {
  font-size: 13px;
}

.payment-form {
  margin-top: 12px;
}

.payment-form-inline {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.payment-form-button {
  width: 100%;
  margin-top: 12px;
}

.subscription-grid {
  margin: 14px 0 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.subscription-grid dt,
.pending-action-label {
  color: var(--suite-billing-muted);
  font-size: 13px;
}

.subscription-grid dd {
  margin: 4px 0 0;
  color: var(--suite-billing-ink);
  font-weight: 600;
}

.pending-action-box {
  margin-top: 16px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(109, 120, 255, 0.2);
  background: rgba(109, 120, 255, 0.08);
}

.invoice-meta {
  flex-direction: column;
  align-items: flex-end;
}

.download-code {
  color: var(--suite-billing-accent);
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 1180px) {
  .center-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .payment-form-inline,
  .subscription-grid {
    grid-template-columns: 1fr;
  }

  .payment-method-row,
  .action-row,
  .invoice-row,
  .invoice-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
