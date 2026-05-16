<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NInputNumber,
  NSelect,
  NSpace,
  NStatistic
} from 'naive-ui';
import {
  addSuiteBillingPaymentMethod,
  createSuiteBillingQuote,
  createSuiteCheckoutDraft,
  executeSuiteBillingSubscriptionAction,
  listAdminDomains,
  listAdminMemberSessions,
  listAdminProductAccess,
  listSuitePricingOffers,
  readAdminSummary,
  readSuiteBillingCenter,
  readSuiteBillingOverview,
  setSuiteBillingDefaultPaymentMethod
} from '@/service/api';
import { ErrorState } from '@/components/feedback';
import { useOrgStore } from '@/store/modules/org';
import { $t } from '@/locales';

defineOptions({ name: 'Admin' });

const DEFAULT_BILLING_SEAT_COUNT = 1;
const CURRENCY_MINOR_UNIT = 100;

const orgStore = useOrgStore();
const summary = ref<Api.Admin.Summary | null>(null);
const domains = ref<Api.Admin.Domain[]>([]);
const productAccess = ref<Api.Admin.ProductAccess[]>([]);
const sessions = ref<Api.Admin.MemberSession[]>([]);
const pricingOffers = ref<Api.Billing.PricingOffer[]>([]);
const billingOverview = ref<Api.Billing.Overview | null>(null);
const billingCenter = ref<Api.Billing.Center | null>(null);
const quotePreview = ref<Api.Billing.Quote | null>(null);
const checkoutDraft = ref<Api.Billing.CheckoutDraft | null>(null);
const selectedBillingAction = ref<Api.Billing.SubscriptionAction | null>(null);
const subscriptionActionDrawerOpen = ref(false);

const quoteModel = reactive<Api.Billing.QuotePayload>({
  offerCode: '',
  billingCycle: 'MONTHLY',
  seatCount: DEFAULT_BILLING_SEAT_COUNT
});
const checkoutModel = reactive({ organizationName: '', domainName: '' });
const paymentMethodModel = reactive<Api.Billing.PaymentMethodPayload>({
  methodType: 'CARD',
  displayLabel: '',
  brand: '',
  lastFour: '',
  expiresAt: '',
  makeDefault: true
});

const billingCycleOptions = [
  { label: 'MONTHLY', value: 'MONTHLY' },
  { label: 'ANNUAL', value: 'ANNUAL' }
];
const paymentMethodTypeOptions = [
  { label: 'CARD', value: 'CARD' },
  { label: 'PAYPAL', value: 'PAYPAL' },
  { label: 'BITCOIN', value: 'BITCOIN' },
  { label: 'CASH', value: 'CASH' }
];

const domainColumns = computed(() => [
  { title: $t('page.admin.domains'), key: 'domain' },
  { title: $t('page.notifications.status'), key: 'status' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' }
]);
const accessColumns = computed(() => [
  { title: $t('page.admin.members'), key: 'userEmail' },
  { title: $t('page.settings.mailAddressMode'), key: 'role' },
  { title: $t('page.admin.productAccess'), key: 'enabledProductCount' }
]);
const sessionColumns = computed(() => [
  { title: $t('page.admin.sessions'), key: 'memberEmail' },
  { title: $t('page.settings.mailAddressMode'), key: 'role' },
  { title: $t('page.calendar.endAt'), key: 'expiresAt' }
]);
const invoiceColumns = computed(() => [
  { title: $t('page.billing.invoice'), key: 'invoiceNumber' },
  { title: $t('page.billing.offer'), key: 'offerName' },
  { title: $t('page.billing.total'), key: 'totalCents' }
]);
const offerColumns = computed(() => [
  { title: $t('page.billing.offer'), key: 'name' },
  { title: $t('page.billing.billingCycle'), key: 'defaultBillingCycle' },
  { title: $t('page.billing.total'), key: 'priceValue' }
]);
const paymentMethodColumns = computed(() => [
  { title: $t('page.billing.paymentMethods'), key: 'displayLabel' },
  { title: $t('page.settings.mailAddressMode'), key: 'methodType' },
  { title: $t('page.billing.defaultPayment'), key: 'defaultMethod' }
]);
const offerOptions = computed(() => pricingOffers.value.map(item => ({ label: item.name, value: item.code })));

async function loadOrgAdmin() {
  const orgId = orgStore.currentOrgId;

  if (!orgId) {
    return;
  }

  const [summaryResult, domainsResult, accessResult, sessionsResult] = await Promise.all([
    readAdminSummary(orgId),
    listAdminDomains(orgId),
    listAdminProductAccess(orgId),
    listAdminMemberSessions(orgId)
  ]);

  if (!summaryResult.error) summary.value = summaryResult.data;
  if (!domainsResult.error) domains.value = domainsResult.data;
  if (!accessResult.error) productAccess.value = accessResult.data;
  if (!sessionsResult.error) sessions.value = sessionsResult.data;
}

async function loadBilling() {
  const [offersResult, billingOverviewResult, billingCenterResult] = await Promise.all([
    listSuitePricingOffers(),
    readSuiteBillingOverview(),
    readSuiteBillingCenter()
  ]);

  if (!offersResult.error) pricingOffers.value = offersResult.data;
  if (!billingOverviewResult.error) billingOverview.value = billingOverviewResult.data;
  if (!billingCenterResult.error) billingCenter.value = billingCenterResult.data;
  applyDefaultOffer();
}

async function loadAdmin() {
  await Promise.all([loadOrgAdmin(), loadBilling()]);
}

function applyDefaultOffer() {
  const [offer] = pricingOffers.value;

  if (quoteModel.offerCode || !offer) {
    return;
  }

  quoteModel.offerCode = offer.code;
  quoteModel.billingCycle = offer.defaultBillingCycle as Api.Billing.BillingCycle;
  quoteModel.seatCount = offer.defaultSeatCount || DEFAULT_BILLING_SEAT_COUNT;
}

function billingQuotePayload(): Api.Billing.QuotePayload {
  return { ...quoteModel };
}

function formatCents(value?: number) {
  return ((value || 0) / CURRENCY_MINOR_UNIT).toFixed(2);
}

async function submitQuote() {
  const { data, error } = await createSuiteBillingQuote(billingQuotePayload());

  if (!error) {
    quotePreview.value = data;
  }
}

async function submitCheckoutDraft() {
  const { data, error } = await createSuiteCheckoutDraft({
    ...billingQuotePayload(),
    organizationName: checkoutModel.organizationName || undefined,
    domainName: checkoutModel.domainName || undefined
  });

  if (!error) {
    checkoutDraft.value = data;
  }
}

async function submitPaymentMethod() {
  if (!paymentMethodModel.displayLabel) {
    return;
  }

  const { data, error } = await addSuiteBillingPaymentMethod(paymentMethodModel);

  if (!error) {
    billingCenter.value = data;
  }
}

async function setDefaultPaymentMethod() {
  const method = billingCenter.value?.paymentMethods.find(item => !item.defaultMethod);

  if (!method) {
    return;
  }

  const { data, error } = await setSuiteBillingDefaultPaymentMethod({ paymentMethodId: method.id });

  if (!error) {
    billingCenter.value = data;
  }
}

function openSubscriptionActionDrawer() {
  selectedBillingAction.value = billingCenter.value?.availableActions.find(item => item.enabled) || null;
  subscriptionActionDrawerOpen.value = Boolean(selectedBillingAction.value);
}

async function confirmSubscriptionAction() {
  if (!selectedBillingAction.value) {
    return;
  }

  const { data, error } = await executeSuiteBillingSubscriptionAction({
    actionCode: selectedBillingAction.value.actionCode
  });

  if (!error) {
    billingCenter.value = data;
    subscriptionActionDrawerOpen.value = false;
  }
}

onMounted(loadAdmin);
</script>

<template>
  <NSpace vertical :size="16">
    <ErrorState
      v-if="!orgStore.currentOrgId"
      compact
      :retryable="false"
      :title="$t('page.admin.title')"
      :description="$t('page.accessGate.forbidden.description')"
    />
    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 s:12 m:6">
        <NCard class="card-wrapper">
          <NStatistic :label="$t('page.admin.members')" :value="summary?.memberCount || 0" />
        </NCard>
      </NGi>
      <NGi span="24 s:12 m:6">
        <NCard class="card-wrapper">
          <NStatistic :label="$t('page.admin.domains')" :value="summary?.domainCount || 0" />
        </NCard>
      </NGi>
      <NGi span="24 s:12 m:6">
        <NCard class="card-wrapper">
          <NStatistic :label="$t('page.admin.productAccess')" :value="summary?.enabledProductCount || 0" />
        </NCard>
      </NGi>
      <NGi span="24 s:12 m:6">
        <NCard class="card-wrapper">
          <NStatistic :label="$t('page.settings.security')" :value="summary?.adminCount || 0" />
        </NCard>
      </NGi>
    </NGrid>

    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 m:8">
        <NCard class="card-wrapper" :title="$t('page.admin.domains')">
          <NDataTable :columns="domainColumns" :data="domains" />
        </NCard>
      </NGi>
      <NGi span="24 m:8">
        <NCard class="card-wrapper" :title="$t('page.admin.productAccess')">
          <NDataTable :columns="accessColumns" :data="productAccess" />
        </NCard>
      </NGi>
      <NGi span="24 m:8">
        <NCard class="card-wrapper" :title="$t('page.admin.sessions')">
          <NDataTable :columns="sessionColumns" :data="sessions" />
        </NCard>
      </NGi>
      <NGi span="24">
        <NCard class="card-wrapper" :title="$t('page.billing.title')">
          <NSpace vertical :size="16">
            <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <NGi span="24 s:8">
                <NStatistic
                  :label="$t('page.billing.plan')"
                  :value="billingOverview?.activePlanName || $t('common.noData')"
                />
              </NGi>
              <NGi span="24 s:8">
                <NStatistic
                  :label="$t('page.billing.seatCount')"
                  :value="billingCenter?.subscriptionSummary.seatCount || 0"
                />
              </NGi>
              <NGi span="24 s:8">
                <NStatistic
                  :label="$t('page.billing.checkoutDraft')"
                  :value="checkoutDraft?.updatedAt || billingOverview?.latestDraft?.updatedAt || $t('common.noData')"
                />
              </NGi>
            </NGrid>

            <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <NGi span="24 l:12">
                <NDataTable :columns="offerColumns" :data="pricingOffers" />
              </NGi>
              <NGi span="24 l:12">
                <NForm :model="quoteModel" label-placement="top">
                  <NFormItem path="offerCode" :label="$t('page.billing.offer')">
                    <NSelect v-model:value="quoteModel.offerCode" :options="offerOptions" />
                  </NFormItem>
                  <NFormItem path="billingCycle" :label="$t('page.billing.billingCycle')">
                    <NSelect v-model:value="quoteModel.billingCycle" :options="billingCycleOptions" />
                  </NFormItem>
                  <NFormItem path="seatCount" :label="$t('page.billing.seatCount')">
                    <NInputNumber v-model:value="quoteModel.seatCount" class="w-full" :min="1" />
                  </NFormItem>
                  <NFormItem path="organizationName" :label="$t('page.admin.title')">
                    <NInput v-model:value="checkoutModel.organizationName" />
                  </NFormItem>
                  <NFormItem path="domainName" :label="$t('page.admin.domains')">
                    <NInput v-model:value="checkoutModel.domainName" />
                  </NFormItem>
                  <NSpace justify="end">
                    <NButton @click="submitQuote">{{ $t('page.billing.createQuote') }}</NButton>
                    <NButton type="primary" @click="submitCheckoutDraft">{{ $t('page.billing.createDraft') }}</NButton>
                  </NSpace>
                </NForm>
              </NGi>
            </NGrid>

            <!-- info-only, see v213-closure-spec-v1.1 §2.1 -->
            <NAlert v-if="quotePreview" type="info" :title="$t('page.billing.quote')">
              {{ quotePreview.offerName }} · {{ quotePreview.currencyCode }}
              {{ formatCents(quotePreview.invoiceSummary.totalCents) }}
            </NAlert>

            <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <NGi span="24 l:12">
                <NCard :bordered="false" :title="$t('page.billing.invoices')">
                  <NDataTable :columns="invoiceColumns" :data="billingCenter?.invoices || []" />
                </NCard>
              </NGi>
              <NGi span="24 l:12">
                <NCard :bordered="false" :title="$t('page.billing.paymentMethods')">
                  <NSpace vertical>
                    <NDataTable :columns="paymentMethodColumns" :data="billingCenter?.paymentMethods || []" />
                    <NForm :model="paymentMethodModel" label-placement="top">
                      <NFormItem path="methodType" :label="$t('page.settings.mailAddressMode')">
                        <NSelect v-model:value="paymentMethodModel.methodType" :options="paymentMethodTypeOptions" />
                      </NFormItem>
                      <NFormItem path="displayLabel" :label="$t('page.billing.paymentMethods')">
                        <NInput v-model:value="paymentMethodModel.displayLabel" />
                      </NFormItem>
                      <NFormItem path="lastFour" label="****">
                        <NInput v-model:value="paymentMethodModel.lastFour" />
                      </NFormItem>
                      <NSpace justify="end">
                        <NButton @click="setDefaultPaymentMethod">{{ $t('page.billing.defaultPayment') }}</NButton>
                        <NButton type="primary" @click="submitPaymentMethod">
                          {{ $t('page.billing.addPaymentMethod') }}
                        </NButton>
                      </NSpace>
                    </NForm>
                  </NSpace>
                </NCard>
              </NGi>
            </NGrid>

            <NSpace justify="end">
              <NButton type="warning" @click="openSubscriptionActionDrawer">
                {{ $t('page.billing.runAction') }}
              </NButton>
            </NSpace>
          </NSpace>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>

  <NDrawer v-model:show="subscriptionActionDrawerOpen" :width="420">
    <NDrawerContent :title="$t('page.billing.subscriptions')">
      <NSpace vertical>
        <!-- info-only, see v213-closure-spec-v1.1 §2.1 -->
        <NAlert type="warning" :title="selectedBillingAction?.actionCode || $t('common.noData')" />
        <NButton type="primary" block @click="confirmSubscriptionAction">
          {{ $t('page.billing.runAction') }}
        </NButton>
      </NSpace>
    </NDrawerContent>
  </NDrawer>
</template>
