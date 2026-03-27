<script setup lang="ts">
import { computed } from 'vue'
import type { WalletAddressBookItem, WalletAddressKind, WalletAddressStatus } from '~/types/wallet-parity'
import { useI18n } from '~/composables/useI18n'
import {
  formatWalletAmountLabel,
  resolveWalletAddressKindKey,
  resolveWalletAddressSourceKey,
  resolveWalletAddressStatusTag
} from '~/utils/wallet-parity'

const props = defineProps<{
  addressBook: WalletAddressBookItem | null
  addressKind: WalletAddressKind
  loading: boolean
  query: string
}>()

const emit = defineEmits<{
  rotate: []
  'update:addressKind': [value: WalletAddressKind]
  'update:query': [value: string]
}>()

const { t } = useI18n()

const currentAddressKind = computed({
  get: () => props.addressKind,
  set: (value: string | number) => emit('update:addressKind', value as WalletAddressKind)
})

const currentQuery = computed({
  get: () => props.query,
  set: (value: string) => emit('update:query', value)
})

const gapHint = computed(() => {
  if (!props.addressBook) {
    return ''
  }
  return t('wallet.parity.addressGapHint', {
    count: props.addressBook.consecutiveUnusedCount,
    limit: props.addressBook.gapLimit
  })
})

function resolveStatusLabel(status: WalletAddressStatus): string {
  return t(`wallet.parity.addressStatus.${status}`)
}
</script>

<template>
  <article class="wallet-parity-card wallet-address-book-card" v-loading="loading">
    <div class="wallet-parity-card-header">
      <div>
        <h3 class="mm-section-subtitle">{{ t('wallet.parity.addresses') }}</h3>
        <p class="mm-muted">{{ gapHint || t(resolveWalletAddressKindKey(addressKind)) }}</p>
      </div>
      <el-button plain :disabled="addressKind !== 'RECEIVE'" @click="emit('rotate')">
        {{ t('wallet.parity.rotateAddress') }}
      </el-button>
    </div>

    <div class="wallet-address-book-toolbar">
      <el-tabs v-model="currentAddressKind" class="wallet-address-book-tabs">
        <el-tab-pane :label="t('wallet.parity.receiveAddresses')" name="RECEIVE" />
        <el-tab-pane :label="t('wallet.parity.changeAddresses')" name="CHANGE" />
      </el-tabs>
      <el-input
        v-model="currentQuery"
        clearable
        :placeholder="t('wallet.parity.addressSearchPlaceholder')"
      >
        <template #prefix>
          <span class="wallet-address-book-search-label">{{ t('wallet.parity.addressSearch') }}</span>
        </template>
      </el-input>
    </div>

    <el-table
      v-if="addressBook?.addresses.length"
      class="wallet-address-book-table"
      :data="addressBook.addresses"
      style="width: 100%"
    >
      <el-table-column prop="addressIndex" :label="t('wallet.parity.addressIndex')" width="90" />
      <el-table-column :label="t('wallet.parity.addresses')" min-width="320">
        <template #default="scope">
          <div class="wallet-address-book-address">
            <strong>{{ scope.row.label }}</strong>
            <p class="mm-muted">{{ scope.row.address }}</p>
            <div class="wallet-address-book-meta">
              <el-tag size="small" type="info">
                {{ t(resolveWalletAddressSourceKey(scope.row.sourceType)) }}
              </el-tag>
              <el-tag v-if="scope.row.reservedFor" size="small" type="warning">
                {{ scope.row.reservedFor }}
              </el-tag>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('wallet.parity.addressValue')" width="150">
        <template #default="scope">
          {{ formatWalletAmountLabel(scope.row.valueMinor, 'BTC', false) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('wallet.parity.addressStatus')" width="120">
        <template #default="scope">
          <el-tag size="small" :type="resolveWalletAddressStatusTag(scope.row.addressStatus)">
            {{ resolveStatusLabel(scope.row.addressStatus) }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else :description="t('wallet.parity.emptyAddressBook')" :image-size="70" />
  </article>
</template>

<style scoped>
.wallet-address-book-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba(10, 16, 34, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
  min-height: 100%;
}

.wallet-parity-card-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.wallet-address-book-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 340px);
  gap: 16px;
  align-items: start;
  margin-bottom: 16px;
}

.wallet-address-book-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.wallet-address-book-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.wallet-address-book-search-label {
  font-size: 12px;
  color: rgba(216, 224, 255, 0.72);
}

.wallet-address-book-address {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.wallet-address-book-address strong {
  color: #f8faff;
}

.wallet-address-book-address p {
  margin: 0;
  word-break: break-all;
}

.wallet-address-book-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.wallet-address-book-table :deep(.el-table) {
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(16, 24, 51, 0.92);
  --el-table-row-hover-bg-color: rgba(99, 122, 255, 0.08);
  --el-table-border-color: rgba(150, 171, 255, 0.16);
  --el-table-text-color: #edf2ff;
  --el-table-header-text-color: rgba(216, 224, 255, 0.76);
  border-radius: 16px;
  overflow: hidden;
}

@media (max-width: 1024px) {
  .wallet-address-book-toolbar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .wallet-parity-card-header {
    flex-direction: column;
  }
}
</style>
