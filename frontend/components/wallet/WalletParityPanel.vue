<script setup lang="ts">
import { toRef } from 'vue'
import WalletParityAddressBookCard from './WalletParityAddressBookCard.vue'
import type { WalletAccount } from '~/types/api'
import { useWalletParityPanel } from '~/composables/useWalletParityPanel'
import { formatWalletAmountLabel, resolveWalletAddressTypeKey, resolveWalletTransferStatusTag } from '~/utils/wallet-parity'

const props = defineProps<{
  account: WalletAccount | null
}>()

const emit = defineEmits<{
  accountImported: [accountId: string]
  maskingChange: [value: boolean]
}>()

const {
  t,
  loading,
  addressBookLoading,
  savingProfile,
  savingAdvanced,
  rotating,
  transferring,
  importing,
  revealing,
  claimingTransferId,
  workspace,
  addressBook,
  recoveryDialogVisible,
  recoveryPhrase,
  addressKind,
  addressQuery,
  profileDraft,
  advancedDraft,
  transferDraft,
  importDraft,
  activeAccount,
  activeSettings,
  canRevealRecovery,
  balancePreview,
  passphraseProtectionLabel,
  transferStatusLabel,
  onSaveProfile,
  onSaveAdvanced,
  onImportWallet,
  onRotateAddress,
  onCreateTransfer,
  onClaimTransfer,
  onRevealRecovery
} = useWalletParityPanel(
  toRef(props, 'account'),
  (value) => emit('maskingChange', value),
  (accountId) => emit('accountImported', accountId)
)
</script>

<template>
  <section v-if="activeAccount" class="wallet-parity-shell mm-card" v-loading="loading">
    <div class="wallet-parity-hero">
      <div>
        <p class="wallet-parity-kicker">Wallet parity v112</p>
        <h2 class="mm-section-title">{{ t('wallet.parity.title') }}</h2>
        <p class="mm-muted wallet-parity-subtitle">{{ t('wallet.parity.subtitle') }}</p>
      </div>
      <div class="wallet-parity-balance">
        <span class="wallet-parity-balance-label">{{ t('wallet.parity.balancePreview') }}</span>
        <strong>{{ balancePreview }}</strong>
      </div>
    </div>

    <section class="wallet-parity-grid">
      <article class="wallet-parity-card">
        <div class="wallet-parity-card-header">
          <div>
            <h3 class="mm-section-subtitle">{{ t('wallet.parity.title') }}</h3>
            <p class="mm-muted">
              {{ workspace?.profile.recoveryPhrasePreview }} · {{ workspace?.profile.recoveryFingerprint }}
            </p>
          </div>
          <el-button
            v-if="canRevealRecovery"
            type="primary"
            plain
            :loading="revealing"
            @click="onRevealRecovery"
          >
            {{ t('wallet.parity.revealRecovery') }}
          </el-button>
          <el-tag v-else type="warning">{{ t('wallet.parity.imported') }}</el-tag>
        </div>

        <p v-if="!canRevealRecovery" class="mm-muted wallet-parity-inline-note">
          {{ t('wallet.parity.recoveryUnavailable') }}
        </p>

        <el-form label-position="top">
          <el-form-item :label="t('wallet.parity.aliasEmail')">
            <el-input v-model="profileDraft.aliasEmail" />
          </el-form-item>
          <div class="wallet-parity-switches">
            <el-switch v-model="profileDraft.bitcoinViaEmailEnabled" />
            <span>{{ t('wallet.parity.viaEmail') }}</span>
          </div>
          <div class="wallet-parity-switches">
            <el-switch v-model="profileDraft.balanceMasked" />
            <span>{{ t('wallet.parity.balanceMasked') }}</span>
          </div>
          <div class="wallet-parity-switches">
            <el-switch v-model="profileDraft.addressPrivacyEnabled" />
            <span>{{ t('wallet.parity.addressPrivacy') }}</span>
          </div>
          <el-form-item :label="t('wallet.parity.addressPoolSize')">
            <el-input-number v-model="profileDraft.addressPoolSize" :min="1" :max="12" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('wallet.parity.passphraseHint')">
            <el-input v-model="profileDraft.passphraseHint" />
          </el-form-item>
          <el-button type="primary" :loading="savingProfile" @click="onSaveProfile">
            {{ t('wallet.parity.save') }}
          </el-button>
        </el-form>
      </article>

      <article class="wallet-parity-card">
        <div class="wallet-parity-card-header">
          <div>
            <h3 class="mm-section-subtitle">{{ t('wallet.parity.advancedTitle') }}</h3>
            <p class="mm-muted">{{ t('wallet.parity.advancedSubtitle') }}</p>
          </div>
          <div class="wallet-parity-badges">
            <el-tag v-if="activeSettings?.imported" type="warning">{{ t('wallet.parity.imported') }}</el-tag>
            <el-tag type="info">{{ passphraseProtectionLabel() }}</el-tag>
          </div>
        </div>

        <div class="wallet-parity-meta">
          <div>
            <span class="wallet-parity-meta-label">{{ t('wallet.parity.recoveryFingerprint') }}</span>
            <strong>{{ activeSettings?.walletSourceFingerprint || workspace?.profile.recoveryFingerprint }}</strong>
          </div>
          <div>
            <span class="wallet-parity-meta-label">{{ t('wallet.parity.addressType') }}</span>
            <strong>{{ activeSettings ? t(resolveWalletAddressTypeKey(activeSettings.addressType)) : '—' }}</strong>
          </div>
        </div>

        <el-form label-position="top">
          <el-form-item :label="t('wallet.parity.walletName')">
            <el-input v-model="advancedDraft.walletName" />
          </el-form-item>
          <el-form-item :label="t('wallet.parity.addressType')">
            <el-select v-model="advancedDraft.addressType" style="width: 100%">
              <el-option :label="t('wallet.parity.addressType.NATIVE_SEGWIT')" value="NATIVE_SEGWIT" />
              <el-option :label="t('wallet.parity.addressType.NESTED_SEGWIT')" value="NESTED_SEGWIT" />
              <el-option :label="t('wallet.parity.addressType.LEGACY')" value="LEGACY" />
              <el-option :label="t('wallet.parity.addressType.TAPROOT')" value="TAPROOT" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('wallet.parity.accountIndex')">
            <el-input-number v-model="advancedDraft.accountIndex" :min="0" :max="255" :step="1" style="width: 100%" />
          </el-form-item>
          <el-button type="primary" plain :loading="savingAdvanced" @click="onSaveAdvanced">
            {{ t('wallet.parity.saveAdvanced') }}
          </el-button>
        </el-form>
      </article>
    </section>

    <section class="wallet-parity-grid wallet-parity-grid--feature">
      <WalletParityAddressBookCard
        :address-book="addressBook"
        :address-kind="addressKind"
        :loading="addressBookLoading"
        :query="addressQuery"
        @rotate="onRotateAddress"
        @update:address-kind="addressKind = $event"
        @update:query="addressQuery = $event"
      />

      <article class="wallet-parity-card">
        <div class="wallet-parity-card-header">
          <div>
            <h3 class="mm-section-subtitle">{{ t('wallet.parity.importTitle') }}</h3>
            <p class="mm-muted">{{ t('wallet.parity.importSubtitle') }}</p>
          </div>
        </div>

        <el-form label-position="top">
          <el-form-item :label="t('wallet.parity.walletName')">
            <el-input v-model="importDraft.walletName" />
          </el-form-item>
          <el-form-item :label="t('wallet.parity.addressType')">
            <el-select v-model="importDraft.addressType" style="width: 100%">
              <el-option :label="t('wallet.parity.addressType.NATIVE_SEGWIT')" value="NATIVE_SEGWIT" />
              <el-option :label="t('wallet.parity.addressType.NESTED_SEGWIT')" value="NESTED_SEGWIT" />
              <el-option :label="t('wallet.parity.addressType.LEGACY')" value="LEGACY" />
              <el-option :label="t('wallet.parity.addressType.TAPROOT')" value="TAPROOT" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('wallet.parity.accountIndex')">
            <el-input-number v-model="importDraft.accountIndex" :min="0" :max="255" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="BTC">
            <el-input v-model="importDraft.assetSymbol" disabled />
          </el-form-item>
          <el-form-item :label="t('wallet.parity.seedPhrase')">
            <el-input
              v-model="importDraft.seedPhrase"
              :autosize="{ minRows: 4, maxRows: 6 }"
              type="textarea"
            />
          </el-form-item>
          <el-form-item :label="t('wallet.parity.passphrase')">
            <el-input v-model="importDraft.passphrase" show-password />
          </el-form-item>
          <el-button type="warning" :loading="importing" @click="onImportWallet">
            {{ t('wallet.parity.importWallet') }}
          </el-button>
        </el-form>
      </article>
    </section>

    <section class="wallet-parity-grid">
      <article class="wallet-parity-card">
        <div class="wallet-parity-card-header">
          <div>
            <h3 class="mm-section-subtitle">{{ t('wallet.parity.emailTransfers') }}</h3>
            <p class="mm-muted">{{ t('wallet.parity.viaEmail') }}</p>
          </div>
        </div>

        <el-form label-position="top">
          <el-form-item :label="t('wallet.parity.transferRecipient')">
            <el-input v-model="transferDraft.recipientEmail" />
          </el-form-item>
          <el-form-item :label="t('wallet.parity.transferAmount')">
            <el-input-number v-model="transferDraft.amountMinor" :min="0" :step="1" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('wallet.parity.transferMessage')">
            <el-input v-model="transferDraft.deliveryMessage" />
          </el-form-item>
          <el-form-item :label="t('wallet.parity.transferMemo')">
            <el-input v-model="transferDraft.memo" />
          </el-form-item>
          <el-button type="warning" :loading="transferring" @click="onCreateTransfer">
            {{ t('wallet.parity.createTransfer') }}
          </el-button>
        </el-form>
      </article>

      <article class="wallet-parity-card">
        <div class="wallet-parity-card-header">
          <div>
            <h3 class="mm-section-subtitle">{{ t('wallet.parity.emailTransfers') }}</h3>
            <p class="mm-muted">{{ t('wallet.parity.recoveryPreview') }}</p>
          </div>
        </div>

        <div v-if="workspace?.emailTransfers.length" class="wallet-parity-list">
          <article
            v-for="item in workspace.emailTransfers"
            :key="item.transferId"
            class="wallet-parity-list-item wallet-parity-transfer"
          >
            <div>
              <strong>{{ item.recipientEmail }}</strong>
              <p class="mm-muted">{{ item.deliveryMessage || item.claimCode }}</p>
              <p class="mm-muted">{{ formatWalletAmountLabel(item.amountMinor, item.assetSymbol, false) }}</p>
            </div>
            <div class="wallet-parity-transfer-actions">
              <el-tag size="small" :type="resolveWalletTransferStatusTag(item.status)">
                {{ transferStatusLabel(item.status) }}
              </el-tag>
              <el-tag v-if="item.inviteRequired" size="small" type="warning">
                {{ t('wallet.parity.inviteRequired') }}
              </el-tag>
              <el-button
                v-if="item.status === 'PENDING_CLAIM'"
                size="small"
                type="success"
                :loading="claimingTransferId === item.transferId"
                @click="onClaimTransfer(item)"
              >
                {{ t('wallet.parity.claimTransfer') }}
              </el-button>
            </div>
          </article>
        </div>
        <el-empty v-else :description="t('wallet.parity.emptyTransfers')" :image-size="70" />
      </article>
    </section>

    <el-dialog
      v-model="recoveryDialogVisible"
      :title="t('wallet.parity.recoveryDialogTitle')"
      width="620px"
    >
      <template v-if="recoveryPhrase">
        <div class="wallet-recovery-dialog">
          <el-tag type="info">
            {{ t('wallet.parity.recoveryFingerprint') }} · {{ recoveryPhrase.recoveryFingerprint }}
          </el-tag>
          <p class="wallet-recovery-phrase">{{ recoveryPhrase.recoveryPhrase }}</p>
          <p class="mm-muted">{{ t('wallet.parity.revealedAt') }} · {{ recoveryPhrase.revealedAt }}</p>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped src="../../assets/styles/wallet-parity-panel.css"></style>
