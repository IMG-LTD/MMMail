<script setup lang="ts">
import type { WalletAccount } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { formatWalletAmountLabel } from '~/utils/wallet-parity'

interface Props {
  accounts: WalletAccount[]
  selectedAccountId: string
  selectedAccount: WalletAccount | null
  walletBalanceMasked: boolean
}

defineProps<Props>()
const emit = defineEmits<{
  refresh: []
  changeAccount: [accountId: string]
}>()

const { t } = useI18n()

function onChange(value: string | number | boolean): void {
  emit('changeAccount', String(value))
}
</script>

<template>
  <section class="mm-card wallet-header-card">
    <div class="wallet-header">
      <div>
        <h1 class="mm-section-title">{{ t('page.wallet.title') }}</h1>
        <p class="mm-muted">{{ t('wallet.workspace.subtitle') }}</p>
      </div>
      <el-button @click="emit('refresh')">{{ t('common.actions.refresh') }}</el-button>
    </div>

    <div class="wallet-account-row">
      <span class="wallet-label">{{ t('wallet.workspace.currentAccount') }}</span>
      <el-select :model-value="selectedAccountId" :placeholder="t('wallet.workspace.selectAccount')" @change="onChange">
        <el-option
          v-for="item in accounts"
          :key="item.accountId"
          :label="`${item.walletName} (${item.assetSymbol})`"
          :value="item.accountId"
        />
      </el-select>
    </div>

    <div v-if="selectedAccount" class="wallet-balance">
      <el-tag type="success">
        {{ t('wallet.workspace.balance') }}:
        {{ formatWalletAmountLabel(selectedAccount.balanceMinor, selectedAccount.assetSymbol, walletBalanceMasked) }}
      </el-tag>
      <span class="mm-muted">{{ t('wallet.workspace.address') }}: {{ selectedAccount.address }}</span>
    </div>
  </section>
</template>

<style scoped>
.wallet-header-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.wallet-header,
.wallet-account-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.wallet-label {
  font-size: 13px;
  color: var(--mm-muted);
}

.wallet-balance {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

@media (max-width: 760px) {
  .wallet-account-row {
    align-items: flex-start;
  }
}
</style>
