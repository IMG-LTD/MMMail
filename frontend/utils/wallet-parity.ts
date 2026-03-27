import type {
  CreateWalletEmailTransferRequest,
  ImportWalletAccountRequest,
  UpdateWalletParityProfileRequest,
  UpdateWalletAdvancedSettingsRequest,
  WalletAddressBookItem,
  WalletAddressKind,
  WalletAddressSource,
  WalletAddressStatus,
  WalletAddressType,
  WalletAdvancedSettingsItem,
  WalletEmailTransferStatus,
  WalletParityProfileItem,
  WalletReceiveAddressItem
} from '~/types/wallet-parity'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const WALLET_ADDRESS_TYPES: WalletAddressType[] = ['NATIVE_SEGWIT', 'NESTED_SEGWIT', 'LEGACY', 'TAPROOT']
const BIP39_LENGTHS = new Set([12, 15, 18, 21, 24])
const SEED_WORD_PATTERN = /^[A-Za-z]+$/

export interface WalletParityProfileDraft {
  bitcoinViaEmailEnabled: boolean
  aliasEmail: string
  balanceMasked: boolean
  addressPrivacyEnabled: boolean
  addressPoolSize: number
  passphraseHint: string
}

export interface WalletEmailTransferDraft {
  amountMinor: number
  recipientEmail: string
  deliveryMessage: string
  memo: string
}

export interface WalletAdvancedSettingsDraft {
  walletName: string
  addressType: WalletAddressType
  accountIndex: number
}

export interface WalletImportDraft {
  walletName: string
  assetSymbol: string
  seedPhrase: string
  passphrase: string
  addressType: WalletAddressType
  accountIndex: number
}

export interface WalletParityActiveAccount {
  accountId: string
  walletName: string
  assetSymbol: string
  address: string
  balanceMinor: number
  createdAt: string
  updatedAt: string
}

export function createWalletParityProfileDraft(
  profile?: Partial<WalletParityProfileItem>
): WalletParityProfileDraft {
  return {
    bitcoinViaEmailEnabled: profile?.bitcoinViaEmailEnabled ?? false,
    aliasEmail: profile?.aliasEmail ?? '',
    balanceMasked: profile?.balanceMasked ?? false,
    addressPrivacyEnabled: profile?.addressPrivacyEnabled ?? true,
    addressPoolSize: profile?.addressPoolSize ?? 3,
    passphraseHint: profile?.passphraseHint ?? ''
  }
}

export function createWalletAdvancedSettingsDraft(
  settings?: Partial<WalletAdvancedSettingsItem>
): WalletAdvancedSettingsDraft {
  return {
    walletName: settings?.walletName ?? '',
    addressType: settings?.addressType ?? 'NATIVE_SEGWIT',
    accountIndex: settings?.accountIndex ?? 0
  }
}

export function createWalletEmailTransferDraft(): WalletEmailTransferDraft {
  return {
    amountMinor: 0,
    recipientEmail: '',
    deliveryMessage: '',
    memo: ''
  }
}

export function createWalletImportDraft(): WalletImportDraft {
  return {
    walletName: '',
    assetSymbol: 'BTC',
    seedPhrase: '',
    passphrase: '',
    addressType: 'NATIVE_SEGWIT',
    accountIndex: 0
  }
}

export function buildWalletParityProfilePayload(
  draft: WalletParityProfileDraft
): UpdateWalletParityProfileRequest {
  const aliasEmail = draft.aliasEmail.trim().toLowerCase()
  const passphraseHint = draft.passphraseHint.trim()

  if (draft.bitcoinViaEmailEnabled && !EMAIL_PATTERN.test(aliasEmail)) {
    throw new Error('wallet.parity.aliasEmailInvalid')
  }
  if (!Number.isInteger(draft.addressPoolSize) || draft.addressPoolSize < 1 || draft.addressPoolSize > 12) {
    throw new Error('wallet.parity.poolSizeInvalid')
  }

  return {
    bitcoinViaEmailEnabled: draft.bitcoinViaEmailEnabled,
    aliasEmail,
    balanceMasked: draft.balanceMasked,
    addressPrivacyEnabled: draft.addressPrivacyEnabled,
    addressPoolSize: draft.addressPoolSize,
    passphraseHint
  }
}

export function buildWalletAdvancedSettingsPayload(
  draft: WalletAdvancedSettingsDraft
): UpdateWalletAdvancedSettingsRequest {
  const walletName = draft.walletName.trim()
  if (walletName.length < 2 || walletName.length > 64) {
    throw new Error('wallet.parity.walletNameInvalid')
  }
  if (!WALLET_ADDRESS_TYPES.includes(draft.addressType)) {
    throw new Error('wallet.parity.addressTypeInvalid')
  }
  if (!Number.isInteger(draft.accountIndex) || draft.accountIndex < 0 || draft.accountIndex > 255) {
    throw new Error('wallet.parity.accountIndexInvalid')
  }
  return {
    walletName,
    addressType: draft.addressType,
    accountIndex: draft.accountIndex
  }
}

export function buildWalletImportPayload(draft: WalletImportDraft): ImportWalletAccountRequest {
  const walletName = draft.walletName.trim()
  const assetSymbol = draft.assetSymbol.trim().toUpperCase()
  const seedPhrase = draft.seedPhrase.trim().replace(/\s+/g, ' ').toLowerCase()
  const passphrase = draft.passphrase.trim()

  if (walletName.length < 2 || walletName.length > 64) {
    throw new Error('wallet.parity.walletNameInvalid')
  }
  if (assetSymbol !== 'BTC') {
    throw new Error('wallet.parity.importAssetInvalid')
  }
  if (!isValidSeedPhrase(seedPhrase)) {
    throw new Error('wallet.parity.seedPhraseInvalid')
  }

  return {
    walletName,
    assetSymbol,
    seedPhrase,
    passphrase,
    addressType: buildWalletAdvancedSettingsPayload({
      walletName,
      addressType: draft.addressType,
      accountIndex: draft.accountIndex
    }).addressType,
    accountIndex: draft.accountIndex
  }
}

export function buildWalletEmailTransferPayload(
  draft: WalletEmailTransferDraft
): CreateWalletEmailTransferRequest {
  const recipientEmail = draft.recipientEmail.trim().toLowerCase()
  const deliveryMessage = draft.deliveryMessage.trim()
  const memo = draft.memo.trim()

  if (!Number.isFinite(draft.amountMinor) || draft.amountMinor <= 0) {
    throw new Error('wallet.parity.transferAmountInvalid')
  }
  if (!EMAIL_PATTERN.test(recipientEmail)) {
    throw new Error('wallet.parity.transferEmailInvalid')
  }

  return {
    amountMinor: Math.trunc(draft.amountMinor),
    recipientEmail,
    deliveryMessage,
    memo
  }
}

export function formatWalletAmountLabel(
  amountMinor: number,
  assetSymbol: string,
  masked: boolean
): string {
  if (masked) {
    return `•••• ${assetSymbol}`
  }
  return `${amountMinor.toLocaleString()} ${assetSymbol}`
}

export function resolveWalletTransferStatusTag(
  status: WalletEmailTransferStatus
): 'success' | 'warning' {
  return status === 'CLAIMED' ? 'success' : 'warning'
}

export function resolveWalletAddressStatusTag(
  status: WalletAddressStatus
): 'success' | 'info' {
  return status === 'USED' ? 'success' : 'info'
}

export function resolveWalletAddressSourceKey(source: WalletAddressSource): string {
  if (source === 'PRIMARY') {
    return 'wallet.parity.addressSourcePrimary'
  }
  if (source === 'ROTATED') {
    return 'wallet.parity.addressSourceRotated'
  }
  return 'wallet.parity.addressSourceInternal'
}

export function resolveWalletAddressTypeKey(type: WalletAddressType): string {
  return `wallet.parity.addressType.${type}`
}

export function resolveWalletAddressKindKey(kind: WalletAddressKind): string {
  return kind === 'CHANGE' ? 'wallet.parity.changeAddresses' : 'wallet.parity.receiveAddresses'
}

export function createWalletAddressBookSnapshot(
  accountId: string,
  addressKind: WalletAddressKind,
  query: string,
  gapLimit: number,
  addresses: WalletReceiveAddressItem[]
): WalletAddressBookItem {
  const normalizedQuery = normalizeWalletAddressQuery(query)
  const filteredAddresses = filterWalletAddressList(addresses, normalizedQuery)
  return {
    accountId,
    addressKind,
    query: normalizedQuery,
    total: filteredAddresses.length,
    gapLimit,
    consecutiveUnusedCount: countTrailingUnusedAddresses(addresses),
    addresses: filteredAddresses
  }
}

export function filterWalletAddressList(
  addresses: WalletReceiveAddressItem[],
  query: string
): WalletReceiveAddressItem[] {
  const normalizedQuery = normalizeWalletAddressQuery(query)
  if (!normalizedQuery) {
    return [...addresses]
  }

  return addresses.filter((item) => {
    return [item.label, item.address, item.reservedFor].some((value) => {
      return value?.toLowerCase().includes(normalizedQuery)
    })
  })
}

export function countTrailingUnusedAddresses(addresses: WalletReceiveAddressItem[]): number {
  let unusedCount = 0
  for (let index = addresses.length - 1; index >= 0; index -= 1) {
    if (addresses[index]?.addressStatus !== 'UNUSED') {
      break
    }
    unusedCount += 1
  }
  return unusedCount
}

export function createParityActiveAccount(
  account: Pick<WalletAdvancedSettingsItem, 'accountId' | 'walletName' | 'assetSymbol' | 'address' | 'updatedAt' | 'importedAt'>,
  balanceMinor = 0
): WalletParityActiveAccount {
  return {
    accountId: account.accountId,
    walletName: account.walletName,
    assetSymbol: account.assetSymbol,
    address: account.address,
    balanceMinor,
    createdAt: account.importedAt || account.updatedAt,
    updatedAt: account.updatedAt
  }
}

function isValidSeedPhrase(seedPhrase: string): boolean {
  if (!seedPhrase) {
    return false
  }
  const words = seedPhrase.split(' ')
  if (!BIP39_LENGTHS.has(words.length)) {
    return false
  }
  return words.every((word) => SEED_WORD_PATTERN.test(word))
}

function normalizeWalletAddressQuery(query: string): string {
  return query.trim().toLowerCase()
}
