export interface WalletParityProfileItem {
  accountId: string
  bitcoinViaEmailEnabled: boolean
  aliasEmail: string
  balanceMasked: boolean
  addressPrivacyEnabled: boolean
  addressPoolSize: number
  recoveryFingerprint: string
  recoveryPhrasePreview: string
  passphraseHint: string
  lastRecoveryViewedAt: string | null
  updatedAt: string
}

export type WalletAddressType = 'NATIVE_SEGWIT' | 'NESTED_SEGWIT' | 'LEGACY' | 'TAPROOT'
export type WalletAddressKind = 'RECEIVE' | 'CHANGE'
export type WalletAddressStatus = 'UNUSED' | 'USED'
export type WalletAddressSource = 'PRIMARY' | 'ROTATED' | 'INTERNAL'

export interface WalletAdvancedSettingsItem {
  accountId: string
  walletName: string
  assetSymbol: string
  address: string
  addressType: WalletAddressType
  accountIndex: number
  imported: boolean
  walletSourceFingerprint: string
  walletPassphraseProtected: boolean
  importedAt: string | null
  updatedAt: string
}

export interface WalletReceiveAddressItem {
  addressId: string
  address: string
  label: string
  sourceType: WalletAddressSource
  addressKind: WalletAddressKind
  addressIndex: number
  addressStatus: WalletAddressStatus
  valueMinor: number
  reservedFor: string | null
  createdAt: string
  updatedAt: string
}

export interface WalletAddressBookItem {
  accountId: string
  addressKind: WalletAddressKind
  query: string
  total: number
  gapLimit: number
  consecutiveUnusedCount: number
  addresses: WalletReceiveAddressItem[]
}

export type WalletEmailTransferStatus = 'PENDING_CLAIM' | 'CLAIMED'

export interface WalletEmailTransferItem {
  transferId: string
  transactionId: string
  recipientEmail: string
  deliveryMessage: string
  claimCode: string
  status: WalletEmailTransferStatus
  inviteRequired: boolean
  amountMinor: number
  assetSymbol: string
  claimedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface WalletRecoveryPhraseItem {
  accountId: string
  recoveryPhrase: string
  recoveryFingerprint: string
  wordCount: number
  revealedAt: string
}

export interface WalletParityWorkspaceItem {
  advancedSettings: WalletAdvancedSettingsItem
  profile: WalletParityProfileItem
  receiveAddresses: WalletReceiveAddressItem[]
  changeAddresses: WalletReceiveAddressItem[]
  emailTransfers: WalletEmailTransferItem[]
}

export interface UpdateWalletParityProfileRequest {
  bitcoinViaEmailEnabled: boolean
  aliasEmail: string
  balanceMasked: boolean
  addressPrivacyEnabled: boolean
  addressPoolSize: number
  passphraseHint: string
}

export interface RotateWalletReceiveAddressRequest {
  label?: string
}

export interface UpdateWalletAdvancedSettingsRequest {
  walletName: string
  addressType: WalletAddressType
  accountIndex: number
}

export interface ImportWalletAccountRequest {
  walletName: string
  assetSymbol: string
  seedPhrase: string
  passphrase: string
  addressType: WalletAddressType
  accountIndex: number
}

export interface CreateWalletEmailTransferRequest {
  amountMinor: number
  recipientEmail: string
  deliveryMessage: string
  memo: string
}
