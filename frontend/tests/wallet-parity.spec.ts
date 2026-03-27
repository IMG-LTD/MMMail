import { describe, expect, it } from 'vitest'
import {
  buildWalletEmailTransferPayload,
  buildWalletParityProfilePayload,
  createWalletParityProfileDraft,
  formatWalletAmountLabel,
  resolveWalletTransferStatusTag
} from '../utils/wallet-parity'

describe('wallet parity utils', () => {
  it('builds wallet profile payloads with explicit validation', () => {
    const draft = createWalletParityProfileDraft({
      bitcoinViaEmailEnabled: true,
      aliasEmail: ' Wallet+Ops@example.com ',
      balanceMasked: true,
      addressPrivacyEnabled: true,
      addressPoolSize: 4,
      passphraseHint: ' steel vault '
    })

    expect(buildWalletParityProfilePayload(draft)).toEqual({
      bitcoinViaEmailEnabled: true,
      aliasEmail: 'wallet+ops@example.com',
      balanceMasked: true,
      addressPrivacyEnabled: true,
      addressPoolSize: 4,
      passphraseHint: 'steel vault'
    })

    expect(() => buildWalletParityProfilePayload({
      ...draft,
      aliasEmail: 'bad-email'
    })).toThrowError('wallet.parity.aliasEmailInvalid')

    expect(() => buildWalletParityProfilePayload({
      ...draft,
      addressPoolSize: 0
    })).toThrowError('wallet.parity.poolSizeInvalid')
  })

  it('builds email transfer payloads and display helpers', () => {
    expect(buildWalletEmailTransferPayload({
      amountMinor: 120000,
      recipientEmail: ' treasury@example.com ',
      deliveryMessage: ' Launch budget ',
      memo: ' Private route '
    })).toEqual({
      amountMinor: 120000,
      recipientEmail: 'treasury@example.com',
      deliveryMessage: 'Launch budget',
      memo: 'Private route'
    })

    expect(() => buildWalletEmailTransferPayload({
      amountMinor: 0,
      recipientEmail: 'treasury@example.com',
      deliveryMessage: '',
      memo: ''
    })).toThrowError('wallet.parity.transferAmountInvalid')

    expect(() => buildWalletEmailTransferPayload({
      amountMinor: 5,
      recipientEmail: 'broken',
      deliveryMessage: '',
      memo: ''
    })).toThrowError('wallet.parity.transferEmailInvalid')

    expect(formatWalletAmountLabel(420000, 'BTC', false)).toBe('420,000 BTC')
    expect(formatWalletAmountLabel(420000, 'BTC', true)).toBe('•••• BTC')
    expect(resolveWalletTransferStatusTag('CLAIMED')).toBe('success')
    expect(resolveWalletTransferStatusTag('PENDING_CLAIM')).toBe('warning')
  })
})
