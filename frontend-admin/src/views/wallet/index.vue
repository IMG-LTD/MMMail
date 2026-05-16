<script setup lang="ts">
import { computed, h, onMounted, reactive } from 'vue';
import { storeToRefs } from 'pinia';
import { useRoute, useRouter } from 'vue-router';
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NInputNumber,
  NNumberAnimation,
  NSelect,
  NSpace,
  NStep,
  NSteps,
  NStatistic,
  NTag
} from 'naive-ui';
import { useWalletStore } from '@/store/modules/wallet';
import { $t } from '@/locales';

defineOptions({ name: 'Wallet' });

const DEFAULT_AMOUNT_MINOR = 1;
const DEFAULT_CONFIRMATIONS = 1;
const DEFAULT_BATCH_LIMIT = 10;
const STATUS_STEP_INDEX: Record<string, number> = {
  PENDING: 1,
  SIGNED: 2,
  BROADCASTED: 3,
  CONFIRMED: 4,
  FAILED: 4
};

const route = useRoute();
const router = useRouter();
const walletStore = useWalletStore();
const {
  accounts,
  transactions,
  pendingActions,
  overview,
  executionTrace,
  reconciliation,
  selectedAccountId,
  selectedTransactionId
} = storeToRefs(walletStore);

const accountModel = reactive<Api.Wallet.AccountPayload>({
  walletName: '',
  assetSymbol: 'ETH',
  address: ''
});
const sendModel = reactive<Api.Wallet.SendPayload>({
  accountId: '',
  amountMinor: DEFAULT_AMOUNT_MINOR,
  assetSymbol: 'ETH',
  targetAddress: '',
  memo: ''
});
const receiveModel = reactive<Api.Wallet.ReceivePayload>({
  accountId: '',
  amountMinor: DEFAULT_AMOUNT_MINOR,
  assetSymbol: 'ETH',
  sourceAddress: '',
  memo: ''
});
const actionModel = reactive({
  operatorHint: 'wallet-workbench',
  signerHint: 'wallet-workbench',
  networkTxHash: '',
  confirmations: DEFAULT_CONFIRMATIONS,
  strategy: 'RETRY_BROADCAST',
  reason: ''
});
const batchModel = reactive({
  maxItems: DEFAULT_BATCH_LIMIT
});

const accountOptions = computed(() => accounts.value.map(item => ({ label: item.walletName, value: item.accountId })));
const selectedAccount = computed(() => accounts.value.find(item => item.accountId === selectedAccountId.value) || null);
const selectedTransaction = computed(
  () =>
    transactions.value.find(item => item.transactionId === selectedTransactionId.value) || transactions.value[0] || null
);
const transactionStep = computed(() => STATUS_STEP_INDEX[selectedTransaction.value?.status || 'PENDING'] || 1);
const sections = computed(() => [
  { name: 'wallet_accounts' as const, label: $t('page.wallet.accounts') },
  { name: 'wallet_transactions' as const, label: $t('page.wallet.transactions') },
  { name: 'wallet_send' as const, label: $t('page.wallet.send') },
  { name: 'wallet_receive' as const, label: $t('page.wallet.receive') },
  { name: 'wallet_reconciliation' as const, label: $t('page.wallet.reconciliation') }
]);

type WalletSectionName = (typeof sections.value)[number]['name'];

const accountColumns = computed(() => [
  { title: $t('page.wallet.account'), key: 'walletName' },
  { title: $t('page.wallet.asset'), key: 'assetSymbol' },
  { title: $t('page.wallet.balance'), key: 'balanceMinor' },
  { title: $t('page.wallet.address'), key: 'address' },
  {
    title: $t('common.action'),
    key: 'actions',
    render: (row: Api.Wallet.Account) =>
      h(
        NButton,
        {
          size: 'small',
          type: row.accountId === selectedAccountId.value ? 'primary' : 'default',
          onClick: () => selectAccount(row.accountId)
        },
        { default: () => $t('common.select') }
      )
  }
]);
const transactionColumns = computed(() => [
  { title: $t('page.wallet.transaction'), key: 'transactionId' },
  { title: $t('page.wallet.type'), key: 'txType' },
  { title: $t('page.wallet.amount'), key: 'amountMinor' },
  { title: $t('page.wallet.asset'), key: 'assetSymbol' },
  { title: $t('page.notifications.status'), key: 'status' },
  {
    title: $t('common.action'),
    key: 'actions',
    render: (row: Api.Wallet.Transaction) =>
      h(
        NButton,
        { size: 'small', onClick: () => openTransaction(row) },
        { default: () => $t('page.wallet.transactionDetail') }
      )
  }
]);
const pendingColumns = computed(() => [
  { title: $t('page.wallet.transaction'), key: 'transactionId' },
  { title: $t('page.notifications.status'), key: 'status' },
  { title: $t('page.wallet.reason'), key: 'reason' },
  { title: $t('page.wallet.action'), key: 'recommendedOperation' }
]);
const traceColumns = computed(() => [
  { title: $t('page.wallet.action'), key: 'stage' },
  { title: $t('page.calendar.startAt'), key: 'at' },
  { title: $t('page.wallet.reason'), key: 'message' }
]);

function routeIsActive(target: WalletSectionName) {
  const currentRoute = String(route.name || 'wallet_accounts');
  return currentRoute === target || (currentRoute === 'wallet' && target === 'wallet_accounts');
}

function sectionButtonType(target: WalletSectionName) {
  return routeIsActive(target) ? 'primary' : 'default';
}

function openSection(target: WalletSectionName) {
  router.push({ name: target });
}

function routeParam(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] || '' : value || '';
}

function syncAccountModels() {
  const account = selectedAccount.value;

  if (!account) return;
  sendModel.accountId = account.accountId;
  sendModel.assetSymbol = account.assetSymbol;
  receiveModel.accountId = account.accountId;
  receiveModel.assetSymbol = account.assetSymbol;
}

async function selectAccount(accountId: string) {
  walletStore.setSelectedAccountId(accountId);
  await walletStore.loadAccountRuntime(accountId);
  syncAccountModels();
}

async function openTransaction(transaction: Api.Wallet.Transaction) {
  walletStore.setSelectedTransactionId(transaction.transactionId);
  await walletStore.loadTrace(transaction.transactionId);
  router.push({ name: 'wallet_transaction_detail', params: { transactionId: transaction.transactionId } });
}

async function submitAccount() {
  const { error } = await walletStore.createAccount({ ...accountModel });

  if (!error) {
    accountModel.walletName = '';
    accountModel.address = '';
    syncAccountModels();
  }
}

async function submitSend() {
  const { error } = await walletStore.sendTransaction({ ...sendModel });

  if (!error) {
    sendModel.targetAddress = '';
    sendModel.memo = '';
  }
}

async function submitReceive() {
  const { error } = await walletStore.receiveTransaction({ ...receiveModel });

  if (!error) {
    receiveModel.sourceAddress = '';
    receiveModel.memo = '';
  }
}

function activeTransactionId() {
  return selectedTransaction.value?.transactionId || '';
}

async function runAdvance() {
  const transactionId = activeTransactionId();

  if (transactionId) await walletStore.advanceTransaction(transactionId, { operatorHint: actionModel.operatorHint });
}

async function runSign() {
  const transactionId = activeTransactionId();

  if (transactionId) await walletStore.signTransaction(transactionId, { signerHint: actionModel.signerHint });
}

async function runBroadcast() {
  const transactionId = activeTransactionId();

  if (transactionId)
    await walletStore.broadcastTransaction(transactionId, { networkTxHash: actionModel.networkTxHash });
}

async function runConfirm() {
  const transactionId = activeTransactionId();

  if (transactionId) {
    await walletStore.confirmTransaction(transactionId, {
      confirmations: actionModel.confirmations,
      networkTxHash: actionModel.networkTxHash
    });
  }
}

async function runRemediate() {
  const transactionId = activeTransactionId();

  if (transactionId) {
    await walletStore.remediateTransaction(transactionId, {
      strategy: actionModel.strategy,
      reason: actionModel.reason || undefined
    });
  }
}

async function runFail() {
  const transactionId = activeTransactionId();

  if (transactionId) await walletStore.failTransaction(transactionId, { reason: actionModel.reason || undefined });
}

async function runBatchAdvance() {
  if (selectedAccountId.value) {
    await walletStore.batchAdvance({
      accountId: selectedAccountId.value,
      maxItems: batchModel.maxItems,
      operatorHint: actionModel.operatorHint
    });
  }
}

async function runBatchRemediate() {
  if (selectedAccountId.value) {
    await walletStore.batchRemediate({
      accountId: selectedAccountId.value,
      maxItems: batchModel.maxItems,
      strategy: actionModel.strategy,
      reason: actionModel.reason || undefined
    });
  }
}

async function runBatchReconcile() {
  if (selectedAccountId.value) {
    await walletStore.batchReconcile({
      accountId: selectedAccountId.value,
      maxItems: batchModel.maxItems,
      strategy: actionModel.strategy
    });
  }
}

onMounted(async () => {
  await walletStore.loadWallet();
  syncAccountModels();
  const transactionId = routeParam(route.params.transactionId);

  if (transactionId) {
    walletStore.setSelectedTransactionId(transactionId);
    await walletStore.loadTrace(transactionId);
  }
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard class="card-wrapper" :title="$t('route.wallet')">
      <NSpace justify="space-between" align="center">
        <NSpace>
          <NTag type="success">{{ selectedAccount?.walletName || $t('common.noData') }}</NTag>
          <NTag>{{ selectedAccount?.assetSymbol || '-' }}</NTag>
          <NTag>{{ $t('page.wallet.risk') }}: {{ overview?.riskLevel || '-' }}</NTag>
        </NSpace>
        <NSpace>
          <NButton
            v-for="section in sections"
            :key="section.name"
            :type="sectionButtonType(section.name)"
            @click="openSection(section.name)"
          >
            {{ section.label }}
          </NButton>
        </NSpace>
      </NSpace>
    </NCard>

    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:15">
        <NCard class="card-wrapper" :title="$t('page.wallet.accounts')">
          <NDataTable :columns="accountColumns" :data="accounts" />
        </NCard>

        <NCard class="card-wrapper mt-16px" :title="$t('page.wallet.transactions')">
          <NDataTable :columns="transactionColumns" :data="transactions" />
        </NCard>

        <NCard class="card-wrapper mt-16px" :title="$t('page.wallet.pendingActions')">
          <NDataTable :columns="pendingColumns" :data="pendingActions" />
        </NCard>

        <NCard class="card-wrapper mt-16px" :title="$t('page.wallet.trace')">
          <NSpace class="mb-12px">
            <NTag>{{ $t('page.wallet.integrity') }}: {{ executionTrace?.integrityScore || 0 }}</NTag>
            <NTag v-for="warning in executionTrace?.warnings || []" :key="warning" type="warning">{{ warning }}</NTag>
          </NSpace>
          <NDataTable :columns="traceColumns" :data="executionTrace?.stageEvents || []" />
        </NCard>
      </NGi>

      <NGi span="24 l:9">
        <NSpace vertical :size="16">
          <NCard class="card-wrapper" :title="$t('page.wallet.execution')">
            <NStatistic :label="$t('page.wallet.health')">
              <NNumberAnimation :from="0" :to="overview?.executionHealthScore || 0" />
            </NStatistic>
            <NSpace class="mt-12px">
              <NTag>{{ $t('page.wallet.blocked') }}: {{ overview?.blockedCount || 0 }}</NTag>
              <NTag>{{ $t('page.wallet.mismatch') }}: {{ reconciliation?.mismatchCount || 0 }}</NTag>
              <NTag>{{ $t('page.wallet.integrity') }}: {{ reconciliation?.integrityScore || 0 }}</NTag>
            </NSpace>
          </NCard>

          <NCard class="card-wrapper" :title="$t('page.wallet.transactionDetail')">
            <NSteps :current="transactionStep">
              <NStep title="PENDING" />
              <NStep title="SIGNED" />
              <NStep title="BROADCASTED" />
              <NStep title="CONFIRMED" />
            </NSteps>
            <NSpace class="mt-12px">
              <NButton @click="runAdvance">{{ $t('page.wallet.action') }}</NButton>
              <NButton @click="runSign">{{ $t('page.wallet.sign') }}</NButton>
              <NButton @click="runBroadcast">{{ $t('page.wallet.broadcast') }}</NButton>
              <NButton @click="runConfirm">{{ $t('page.wallet.confirm') }}</NButton>
              <NButton type="warning" @click="runRemediate">{{ $t('page.wallet.remediate') }}</NButton>
              <NButton type="error" @click="runFail">{{ $t('page.wallet.fail') }}</NButton>
            </NSpace>
          </NCard>

          <NCard class="card-wrapper" :title="$t('page.wallet.createAccount')">
            <NForm :model="accountModel" label-placement="top">
              <NFormItem path="walletName" :label="$t('page.wallet.account')">
                <NInput v-model:value="accountModel.walletName" />
              </NFormItem>
              <NFormItem path="assetSymbol" :label="$t('page.wallet.asset')">
                <NInput v-model:value="accountModel.assetSymbol" />
              </NFormItem>
              <NFormItem path="address" :label="$t('page.wallet.address')">
                <NInput v-model:value="accountModel.address" />
              </NFormItem>
              <NButton type="primary" @click="submitAccount">{{ $t('page.wallet.createAccount') }}</NButton>
            </NForm>
          </NCard>

          <NCard class="card-wrapper" :title="$t('page.wallet.send')">
            <NForm :model="sendModel" label-placement="top">
              <NFormItem path="accountId" :label="$t('page.wallet.account')">
                <NSelect v-model:value="sendModel.accountId" :options="accountOptions" />
              </NFormItem>
              <NFormItem path="targetAddress" :label="$t('page.wallet.targetAddress')">
                <NInput v-model:value="sendModel.targetAddress" />
              </NFormItem>
              <NFormItem path="amountMinor" :label="$t('page.wallet.amount')">
                <NInputNumber v-model:value="sendModel.amountMinor" class="w-full" />
              </NFormItem>
              <NFormItem path="memo" :label="$t('page.wallet.memo')">
                <NInput v-model:value="sendModel.memo" />
              </NFormItem>
              <NButton type="primary" @click="submitSend">{{ $t('page.wallet.send') }}</NButton>
            </NForm>
          </NCard>

          <NCard class="card-wrapper" :title="$t('page.wallet.receive')">
            <NForm :model="receiveModel" label-placement="top">
              <NFormItem path="accountId" :label="$t('page.wallet.account')">
                <NSelect v-model:value="receiveModel.accountId" :options="accountOptions" />
              </NFormItem>
              <NFormItem path="sourceAddress" :label="$t('page.wallet.sourceAddress')">
                <NInput v-model:value="receiveModel.sourceAddress" />
              </NFormItem>
              <NFormItem path="amountMinor" :label="$t('page.wallet.amount')">
                <NInputNumber v-model:value="receiveModel.amountMinor" class="w-full" />
              </NFormItem>
              <NButton type="primary" @click="submitReceive">{{ $t('page.wallet.receive') }}</NButton>
            </NForm>
          </NCard>

          <NCard class="card-wrapper" :title="$t('page.wallet.reconciliation')">
            <NForm :model="actionModel" label-placement="top">
              <NFormItem path="networkTxHash" :label="$t('page.wallet.networkTxHash')">
                <NInput v-model:value="actionModel.networkTxHash" />
              </NFormItem>
              <NFormItem path="signerHint" :label="$t('page.wallet.signerHint')">
                <NInput v-model:value="actionModel.signerHint" />
              </NFormItem>
              <NFormItem path="strategy" :label="$t('page.wallet.strategy')">
                <NInput v-model:value="actionModel.strategy" />
              </NFormItem>
              <NFormItem path="reason" :label="$t('page.wallet.reason')">
                <NInput v-model:value="actionModel.reason" />
              </NFormItem>
              <NFormItem path="confirmations" :label="$t('page.wallet.confirmations')">
                <NInputNumber v-model:value="actionModel.confirmations" class="w-full" />
              </NFormItem>
              <NFormItem path="maxItems" :label="$t('page.wallet.maxItems')">
                <NInputNumber v-model:value="batchModel.maxItems" class="w-full" />
              </NFormItem>
              <NSpace>
                <NButton @click="runBatchAdvance">{{ $t('page.wallet.batchAdvance') }}</NButton>
                <NButton @click="runBatchRemediate">{{ $t('page.wallet.batchRemediate') }}</NButton>
                <NButton type="primary" @click="runBatchReconcile">{{ $t('page.wallet.batchReconcile') }}</NButton>
              </NSpace>
            </NForm>
          </NCard>
        </NSpace>
      </NGi>
    </NGrid>
  </NSpace>
</template>
