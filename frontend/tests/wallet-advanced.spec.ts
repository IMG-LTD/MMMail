import { describe, expect, it } from 'vitest'
import {
  buildWalletAdvancedSettingsPayload,
  buildWalletImportPayload,
  countTrailingUnusedAddresses,
  createParityActiveAccount,
  createWalletAddressBookSnapshot,
  filterWalletAddressList
} from '../utils/wallet-parity'
import type { WalletReceiveAddressItem } from '../types/wallet-parity'

function createAddressFixture(
  overrides: Partial<WalletReceiveAddressItem> = {}
): WalletReceiveAddressItem {
  return {
    addressId: overrides.addressId || 'addr-1',
    address: overrides.address || 'bc1qfixture0001',
    label: overrides.label || 'Primary lane',
    sourceType: overrides.sourceType || 'PRIMARY',
    addressKind: overrides.addressKind || 'RECEIVE',
    addressIndex: overrides.addressIndex ?? 0,
    addressStatus: overrides.addressStatus || 'UNUSED',
    valueMinor: overrides.valueMinor ?? 0,
    reservedFor: overrides.reservedFor ?? null,
    createdAt: overrides.createdAt || '2026-03-12T00:00:00',
    updatedAt: overrides.updatedAt || '2026-03-12T00:00:00'
  }
}

describe('wallet advanced utils', () => {
  it('builds advanced settings payloads with normalized values', () => {
    expect(buildWalletAdvancedSettingsPayload({
      walletName: '  Treasury lane  ',
      addressType: 'TAPROOT',
      accountIndex: 7
    })).toEqual({
      walletName: 'Treasury lane',
      addressType: 'TAPROOT',
      accountIndex: 7
    })

    expect(() => buildWalletAdvancedSettingsPayload({
      walletName: 'A',
      addressType: 'NATIVE_SEGWIT',
      accountIndex: 0
    })).toThrowError('wallet.parity.walletNameInvalid')

    expect(() => buildWalletAdvancedSettingsPayload({
      walletName: 'Treasury lane',
      addressType: 'NATIVE_SEGWIT',
      accountIndex: 256
    })).toThrowError('wallet.parity.accountIndexInvalid')
  })

  it('builds import payloads with BIP39 seed normalization', () => {
    expect(buildWalletImportPayload({
      walletName: '  Imported vault  ',
      assetSymbol: 'btc',
      seedPhrase: '  legal winner thank year wave sausage worth useful legal winner thank yellow  ',
      passphrase: '  hidden words  ',
      addressType: 'NATIVE_SEGWIT',
      accountIndex: 0
    })).toEqual({
      walletName: 'Imported vault',
      assetSymbol: 'BTC',
      seedPhrase: 'legal winner thank year wave sausage worth useful legal winner thank yellow',
      passphrase: 'hidden words',
      addressType: 'NATIVE_SEGWIT',
      accountIndex: 0
    })

    expect(() => buildWalletImportPayload({
      walletName: 'Imported vault',
      assetSymbol: 'ETH',
      seedPhrase: 'legal winner thank year wave sausage worth useful legal winner thank yellow',
      passphrase: '',
      addressType: 'NATIVE_SEGWIT',
      accountIndex: 0
    })).toThrowError('wallet.parity.importAssetInvalid')

    expect(() => buildWalletImportPayload({
      walletName: 'Imported vault',
      assetSymbol: 'BTC',
      seedPhrase: 'not enough words',
      passphrase: '',
      addressType: 'NATIVE_SEGWIT',
      accountIndex: 0
    })).toThrowError('wallet.parity.seedPhraseInvalid')
  })

  it('creates address book snapshots with filtering and gap semantics', () => {
    const addresses = [
      createAddressFixture({ addressId: 'addr-1', label: 'Primary lane', addressStatus: 'USED', valueMinor: 4000 }),
      createAddressFixture({ addressId: 'addr-2', label: 'Stealth lane', addressIndex: 1, reservedFor: 'Invoice #77' }),
      createAddressFixture({ addressId: 'addr-3', label: 'Change lane', addressKind: 'CHANGE', addressIndex: 2 })
    ]

    expect(filterWalletAddressList(addresses, 'invoice')).toHaveLength(1)
    expect(countTrailingUnusedAddresses(addresses)).toBe(2)

    expect(createWalletAddressBookSnapshot('9', 'RECEIVE', 'lane', 5, addresses)).toEqual({
      accountId: '9',
      addressKind: 'RECEIVE',
      query: 'lane',
      total: 3,
      gapLimit: 5,
      consecutiveUnusedCount: 2,
      addresses
    })
  })

  it('creates local active account snapshots for imported accounts', () => {
    expect(createParityActiveAccount({
      accountId: '12',
      walletName: 'Imported vault',
      assetSymbol: 'BTC',
      address: 'bc1qimported0001',
      updatedAt: '2026-03-12T10:00:00',
      importedAt: '2026-03-12T09:00:00'
    }, 5200)).toEqual({
      accountId: '12',
      walletName: 'Imported vault',
      assetSymbol: 'BTC',
      address: 'bc1qimported0001',
      balanceMinor: 5200,
      createdAt: '2026-03-12T09:00:00',
      updatedAt: '2026-03-12T10:00:00'
    })
  })
})
