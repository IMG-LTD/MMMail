import { ElMessage } from 'element-plus'
import { computed, reactive, ref, watch, type Ref } from 'vue'
import type { WalletAccount } from '~/types/api'
import type {
  WalletAddressBookItem,
  WalletAddressKind,
  WalletEmailTransferItem,
  WalletParityWorkspaceItem,
  WalletRecoveryPhraseItem
} from '~/types/wallet-parity'
import { useWalletParityApi } from '~/composables/useWalletParityApi'
import { useI18n } from '~/composables/useI18n'
import {
  buildWalletAdvancedSettingsPayload,
  buildWalletEmailTransferPayload,
  buildWalletImportPayload,
  buildWalletParityProfilePayload,
  createParityActiveAccount,
  createWalletAddressBookSnapshot,
  createWalletAdvancedSettingsDraft,
  createWalletEmailTransferDraft,
  createWalletImportDraft,
  createWalletParityProfileDraft,
  formatWalletAmountLabel,
  type WalletParityActiveAccount
} from '~/utils/wallet-parity'

type MaskingChangeHandler = (value: boolean) => void
type AccountImportedHandler = (accountId: string) => void

export function useWalletParityPanel(
  account: Ref<WalletAccount | null>,
  emitMaskingChange: MaskingChangeHandler,
  emitAccountImported: AccountImportedHandler
) {
  const { t } = useI18n()
  const {
    getWorkspace,
    getAddressBook,
    updateProfile,
    updateAdvancedSettings,
    importWallet,
    rotateReceiveAddress,
    createEmailTransfer,
    claimEmailTransfer,
    revealRecoveryPhrase
  } = useWalletParityApi()

  const loading = ref(false)
  const addressBookLoading = ref(false)
  const savingProfile = ref(false)
  const savingAdvanced = ref(false)
  const rotating = ref(false)
  const transferring = ref(false)
  const importing = ref(false)
  const revealing = ref(false)
  const claimingTransferId = ref('')
  const workspace = ref<WalletParityWorkspaceItem | null>(null)
  const addressBook = ref<WalletAddressBookItem | null>(null)
  const recoveryDialogVisible = ref(false)
  const recoveryPhrase = ref<WalletRecoveryPhraseItem | null>(null)
  const addressKind = ref<WalletAddressKind>('RECEIVE')
  const addressQuery = ref('')
  const activeAccountOverride = ref<WalletParityActiveAccount | null>(null)

  const profileDraft = reactive(createWalletParityProfileDraft())
  const advancedDraft = reactive(createWalletAdvancedSettingsDraft())
  const transferDraft = reactive(createWalletEmailTransferDraft())
  const importDraft = reactive(createWalletImportDraft())

  let workspaceRequestId = 0
  let addressBookRequestId = 0

  const activeAccount = computed(() => activeAccountOverride.value ?? account.value)
  const activeAccountId = computed(() => activeAccount.value?.accountId ?? '')
  const activeSettings = computed(() => workspace.value?.advancedSettings ?? null)
  const canRevealRecovery = computed(() => !activeSettings.value?.imported)
  const balancePreview = computed(() => {
    if (!activeAccount.value) {
      return ''
    }
    return formatWalletAmountLabel(
      activeAccount.value.balanceMinor,
      activeAccount.value.assetSymbol,
      profileDraft.balanceMasked
    )
  })

  function resetState(clearOverride = false): void {
    workspace.value = null
    addressBook.value = null
    addressBookLoading.value = false
    recoveryPhrase.value = null
    recoveryDialogVisible.value = false
    addressKind.value = 'RECEIVE'
    addressQuery.value = ''
    Object.assign(profileDraft, createWalletParityProfileDraft())
    Object.assign(advancedDraft, createWalletAdvancedSettingsDraft())
    Object.assign(transferDraft, createWalletEmailTransferDraft())
    Object.assign(importDraft, createWalletImportDraft())
    if (clearOverride) {
      activeAccountOverride.value = null
    }
    emitMaskingChange(false)
  }

  function formatError(error: unknown): string {
    const message = (error as Error).message || ''
    return message.startsWith('wallet.parity.') ? t(message) : message || 'Wallet parity action failed'
  }

  function syncWorkspace(nextWorkspace: WalletParityWorkspaceItem): void {
    workspace.value = nextWorkspace
    Object.assign(profileDraft, createWalletParityProfileDraft(nextWorkspace.profile))
    Object.assign(advancedDraft, createWalletAdvancedSettingsDraft(nextWorkspace.advancedSettings))
    addressBook.value = createWalletAddressBookSnapshot(
      nextWorkspace.advancedSettings.accountId,
      addressKind.value,
      addressQuery.value,
      nextWorkspace.profile.addressPoolSize,
      addressKind.value === 'CHANGE' ? nextWorkspace.changeAddresses : nextWorkspace.receiveAddresses
    )
    emitMaskingChange(nextWorkspace.profile.balanceMasked)
  }

  async function loadWorkspaceForAccount(accountId: string): Promise<void> {
    const requestId = workspaceRequestId + 1
    workspaceRequestId = requestId
    loading.value = true
    try {
      const nextWorkspace = await getWorkspace(accountId)
      if (requestId === workspaceRequestId) {
        syncWorkspace(nextWorkspace)
      }
    } catch (error) {
      ElMessage.error(formatError(error))
    } finally {
      if (requestId === workspaceRequestId) {
        loading.value = false
      }
    }
  }

  async function loadAddressBookForActiveAccount(): Promise<void> {
    if (!activeAccountId.value) {
      addressBook.value = null
      return
    }

    const requestId = addressBookRequestId + 1
    addressBookRequestId = requestId
    addressBookLoading.value = true
    try {
      const nextAddressBook = await getAddressBook(
        activeAccountId.value,
        addressKind.value,
        addressQuery.value
      )
      if (requestId === addressBookRequestId) {
        addressBook.value = nextAddressBook
      }
    } catch (error) {
      ElMessage.error(formatError(error))
    } finally {
      if (requestId === addressBookRequestId) {
        addressBookLoading.value = false
      }
    }
  }

  async function refreshWorkspace(): Promise<void> {
    if (!activeAccountId.value) {
      resetState()
      return
    }
    await loadWorkspaceForAccount(activeAccountId.value)
  }

  async function onSaveProfile(): Promise<void> {
    if (!activeAccountId.value) {
      return
    }
    savingProfile.value = true
    try {
      await updateProfile(activeAccountId.value, buildWalletParityProfilePayload(profileDraft))
      await refreshWorkspace()
      ElMessage.success(t('wallet.parity.profileSaved'))
    } catch (error) {
      ElMessage.error(formatError(error))
    } finally {
      savingProfile.value = false
    }
  }

  async function onSaveAdvanced(): Promise<void> {
    if (!activeAccountId.value) {
      return
    }
    savingAdvanced.value = true
    try {
      const settings = await updateAdvancedSettings(
        activeAccountId.value,
        buildWalletAdvancedSettingsPayload(advancedDraft)
      )
      if (activeAccountOverride.value) {
        activeAccountOverride.value = createParityActiveAccount(
          settings,
          activeAccountOverride.value.balanceMinor
        )
      }
      await refreshWorkspace()
      ElMessage.success(t('wallet.parity.advancedSaved'))
    } catch (error) {
      ElMessage.error(formatError(error))
    } finally {
      savingAdvanced.value = false
    }
  }

  async function onImportWallet(): Promise<void> {
    importing.value = true
    try {
      const settings = await importWallet(buildWalletImportPayload(importDraft))
      activeAccountOverride.value = createParityActiveAccount(settings)
      emitAccountImported(settings.accountId)
      Object.assign(importDraft, createWalletImportDraft())
      recoveryDialogVisible.value = false
      recoveryPhrase.value = null
      await refreshWorkspace()
      ElMessage.success(t('wallet.parity.importedLoaded'))
    } catch (error) {
      ElMessage.error(formatError(error))
    } finally {
      importing.value = false
    }
  }

  async function onRotateAddress(): Promise<void> {
    if (!activeAccountId.value) {
      return
    }
    rotating.value = true
    try {
      await rotateReceiveAddress(activeAccountId.value)
      await refreshWorkspace()
      ElMessage.success(t('wallet.parity.addressRotated'))
    } catch (error) {
      ElMessage.error(formatError(error))
    } finally {
      rotating.value = false
    }
  }

  async function onCreateTransfer(): Promise<void> {
    if (!activeAccountId.value) {
      return
    }
    transferring.value = true
    try {
      await createEmailTransfer(activeAccountId.value, buildWalletEmailTransferPayload(transferDraft))
      Object.assign(transferDraft, createWalletEmailTransferDraft())
      await refreshWorkspace()
      ElMessage.success(t('wallet.parity.transferCreated'))
    } catch (error) {
      ElMessage.error(formatError(error))
    } finally {
      transferring.value = false
    }
  }

  async function onClaimTransfer(item: WalletEmailTransferItem): Promise<void> {
    claimingTransferId.value = item.transferId
    try {
      await claimEmailTransfer(item.transferId)
      await refreshWorkspace()
      ElMessage.success(t('wallet.parity.transferClaimed'))
    } catch (error) {
      ElMessage.error(formatError(error))
    } finally {
      claimingTransferId.value = ''
    }
  }

  async function onRevealRecovery(): Promise<void> {
    if (!activeAccountId.value || !canRevealRecovery.value) {
      return
    }
    revealing.value = true
    try {
      recoveryPhrase.value = await revealRecoveryPhrase(activeAccountId.value)
      recoveryDialogVisible.value = true
      await refreshWorkspace()
      ElMessage.success(t('wallet.parity.recoveryLoaded'))
    } catch (error) {
      ElMessage.error(formatError(error))
    } finally {
      revealing.value = false
    }
  }

  function passphraseProtectionLabel(): string {
    const enabled = activeSettings.value?.walletPassphraseProtected ? 'true' : 'false'
    return t(`wallet.parity.passphraseProtected.${enabled}`)
  }

  function transferStatusLabel(status: WalletEmailTransferItem['status']): string {
    return status === 'CLAIMED' ? t('wallet.parity.claimed') : t('wallet.parity.pendingClaim')
  }

  watch(
    () => account.value?.accountId,
    (accountId, previousAccountId) => {
      if (accountId !== previousAccountId) {
        activeAccountOverride.value = null
      }
    }
  )

  watch(
    activeAccountId,
    async (nextAccountId) => {
      if (!nextAccountId) {
        resetState()
        return
      }
      await loadWorkspaceForAccount(nextAccountId)
    },
    { immediate: true }
  )

  watch(
    [activeAccountId, addressKind, addressQuery],
    ([nextAccountId], _previousValues, onCleanup) => {
      if (!nextAccountId) {
        return
      }
      const timerId = setTimeout(() => {
        void loadAddressBookForActiveAccount()
      }, 180)
      onCleanup(() => clearTimeout(timerId))
    },
    { immediate: true }
  )

  return {
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
  }
}
