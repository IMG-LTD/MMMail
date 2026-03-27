import type { ApiResponse } from '~/types/api'
import type {
  CreateWalletEmailTransferRequest,
  ImportWalletAccountRequest,
  RotateWalletReceiveAddressRequest,
  UpdateWalletAdvancedSettingsRequest,
  UpdateWalletParityProfileRequest,
  WalletAddressBookItem,
  WalletAdvancedSettingsItem,
  WalletEmailTransferItem,
  WalletParityWorkspaceItem,
  WalletReceiveAddressItem,
  WalletRecoveryPhraseItem,
  WalletParityProfileItem
} from '~/types/wallet-parity'

export function useWalletParityApi() {
  const { $apiClient } = useNuxtApp()

  async function getWorkspace(accountId: string, limit = 10): Promise<WalletParityWorkspaceItem> {
    const response = await $apiClient.get<ApiResponse<WalletParityWorkspaceItem>>(
      `/api/v1/wallet/accounts/${accountId}/parity-workspace`,
      { params: { limit } }
    )
    return response.data.data
  }

  async function getAddressBook(
    accountId: string,
    kind: WalletAddressBookItem['addressKind'],
    query = ''
  ): Promise<WalletAddressBookItem> {
    const response = await $apiClient.get<ApiResponse<WalletAddressBookItem>>(
      `/api/v1/wallet/accounts/${accountId}/address-book`,
      { params: { kind, query: query || undefined } }
    )
    return response.data.data
  }

  async function updateProfile(
    accountId: string,
    payload: UpdateWalletParityProfileRequest
  ): Promise<WalletParityProfileItem> {
    const response = await $apiClient.put<ApiResponse<WalletParityProfileItem>>(
      `/api/v1/wallet/accounts/${accountId}/parity-profile`,
      payload
    )
    return response.data.data
  }

  async function updateAdvancedSettings(
    accountId: string,
    payload: UpdateWalletAdvancedSettingsRequest
  ): Promise<WalletAdvancedSettingsItem> {
    const response = await $apiClient.put<ApiResponse<WalletAdvancedSettingsItem>>(
      `/api/v1/wallet/accounts/${accountId}/advanced-settings`,
      payload
    )
    return response.data.data
  }

  async function importWallet(payload: ImportWalletAccountRequest): Promise<WalletAdvancedSettingsItem> {
    const response = await $apiClient.post<ApiResponse<WalletAdvancedSettingsItem>>(
      '/api/v1/wallet/accounts/import',
      payload
    )
    return response.data.data
  }

  async function rotateReceiveAddress(
    accountId: string,
    payload: RotateWalletReceiveAddressRequest = {}
  ): Promise<WalletReceiveAddressItem> {
    const response = await $apiClient.post<ApiResponse<WalletReceiveAddressItem>>(
      `/api/v1/wallet/accounts/${accountId}/receive-addresses/rotate`,
      payload
    )
    return response.data.data
  }

  async function createEmailTransfer(
    accountId: string,
    payload: CreateWalletEmailTransferRequest
  ): Promise<WalletEmailTransferItem> {
    const response = await $apiClient.post<ApiResponse<WalletEmailTransferItem>>(
      `/api/v1/wallet/accounts/${accountId}/email-transfers`,
      payload
    )
    return response.data.data
  }

  async function claimEmailTransfer(transferId: string): Promise<WalletEmailTransferItem> {
    const response = await $apiClient.post<ApiResponse<WalletEmailTransferItem>>(
      `/api/v1/wallet/email-transfers/${transferId}/claim`
    )
    return response.data.data
  }

  async function revealRecoveryPhrase(accountId: string): Promise<WalletRecoveryPhraseItem> {
    const response = await $apiClient.post<ApiResponse<WalletRecoveryPhraseItem>>(
      `/api/v1/wallet/accounts/${accountId}/recovery/reveal`
    )
    return response.data.data
  }

  return {
    getWorkspace,
    getAddressBook,
    updateProfile,
    updateAdvancedSettings,
    importWallet,
    rotateReceiveAddress,
    createEmailTransfer,
    claimEmailTransfer,
    revealRecoveryPhrase
  }
}
