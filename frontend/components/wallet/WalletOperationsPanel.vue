<script setup lang="ts">
import { reactive } from 'vue'
import { useI18n } from '~/composables/useI18n'

interface CreateAccountPayload {
  walletName: string
  assetSymbol: string
  address: string
}

interface TransactionPayload {
  amountMinor: number
  address: string
  memo: string
}

interface Props {
  creating: boolean
  receiving: boolean
  sending: boolean
}

defineProps<Props>()
const emit = defineEmits<{
  createAccount: [payload: CreateAccountPayload]
  receive: [payload: TransactionPayload]
  send: [payload: TransactionPayload]
}>()

const { t } = useI18n()

const createForm = reactive<CreateAccountPayload>({
  walletName: '',
  assetSymbol: 'BTC',
  address: ''
})

const receiveForm = reactive<TransactionPayload>({
  amountMinor: 0,
  address: '',
  memo: ''
})

const sendForm = reactive<TransactionPayload>({
  amountMinor: 0,
  address: '',
  memo: ''
})
</script>

<template>
  <section class="wallet-operations-grid">
    <article class="mm-card wallet-card">
      <h2 class="mm-section-title">{{ t('wallet.workspace.operations.createTitle') }}</h2>
      <el-form label-position="top">
        <el-form-item :label="t('wallet.workspace.operations.walletName')">
          <el-input v-model="createForm.walletName" :placeholder="t('wallet.workspace.operations.walletNameExample')" />
        </el-form-item>
        <el-form-item :label="t('wallet.workspace.operations.assetSymbol')">
          <el-input v-model="createForm.assetSymbol" placeholder="BTC" maxlength="16" />
        </el-form-item>
        <el-form-item :label="t('wallet.workspace.operations.address')">
          <el-input v-model="createForm.address" placeholder="bc1q..." />
        </el-form-item>
        <el-button type="primary" :loading="creating" @click="emit('createAccount', createForm)">
          {{ t('wallet.workspace.operations.create') }}
        </el-button>
      </el-form>
    </article>

    <article class="mm-card wallet-card">
      <h2 class="mm-section-title">{{ t('wallet.workspace.operations.receiveTitle') }}</h2>
      <el-form label-position="top">
        <el-form-item :label="t('wallet.workspace.operations.amount')">
          <el-input-number v-model="receiveForm.amountMinor" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('wallet.workspace.operations.sourceAddress')">
          <el-input v-model="receiveForm.address" placeholder="bc1qsource..." />
        </el-form-item>
        <el-form-item :label="t('wallet.workspace.operations.memo')">
          <el-input v-model="receiveForm.memo" :placeholder="t('wallet.workspace.operations.optional')" />
        </el-form-item>
        <el-button type="success" :loading="receiving" @click="emit('receive', receiveForm)">
          {{ t('wallet.workspace.operations.recordReceive') }}
        </el-button>
      </el-form>
    </article>

    <article class="mm-card wallet-card">
      <h2 class="mm-section-title">{{ t('wallet.workspace.operations.sendTitle') }}</h2>
      <el-form label-position="top">
        <el-form-item :label="t('wallet.workspace.operations.amount')">
          <el-input-number v-model="sendForm.amountMinor" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('wallet.workspace.operations.targetAddress')">
          <el-input v-model="sendForm.address" placeholder="bc1qtarget..." />
        </el-form-item>
        <el-form-item :label="t('wallet.workspace.operations.memo')">
          <el-input v-model="sendForm.memo" :placeholder="t('wallet.workspace.operations.optional')" />
        </el-form-item>
        <el-button type="warning" :loading="sending" @click="emit('send', sendForm)">
          {{ t('wallet.workspace.operations.recordSend') }}
        </el-button>
      </el-form>
    </article>
  </section>
</template>

<style scoped>
.wallet-operations-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.wallet-card {
  padding: 14px;
}

@media (max-width: 1100px) {
  .wallet-operations-grid {
    grid-template-columns: 1fr;
  }
}
</style>
