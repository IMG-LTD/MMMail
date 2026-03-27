import type {
  ApiResponse,
  ContactDuplicateGroup,
  ContactGroup,
  ContactImportResult,
  ContactItem,
  ContactSuggestion,
  CreateContactGroupRequest,
  CreateContactRequest,
  UpdateContactGroupMembersRequest,
  UpdateContactGroupRequest,
  UpdateContactRequest
} from '~/types/api'

export function useContactApi() {
  const { $apiClient } = useNuxtApp()

  async function listContacts(keyword = '', favoriteOnly = false): Promise<ContactItem[]> {
    const response = await $apiClient.get<ApiResponse<ContactItem[]>>('/api/v1/contacts', {
      params: { keyword, favoriteOnly: favoriteOnly || undefined }
    })
    return response.data.data
  }

  async function createContact(payload: CreateContactRequest): Promise<ContactItem> {
    const response = await $apiClient.post<ApiResponse<ContactItem>>('/api/v1/contacts', payload)
    return response.data.data
  }

  async function updateContact(contactId: string, payload: UpdateContactRequest): Promise<ContactItem> {
    const response = await $apiClient.put<ApiResponse<ContactItem>>(`/api/v1/contacts/${contactId}`, payload)
    return response.data.data
  }

  async function deleteContact(contactId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/contacts/${contactId}`)
  }

  async function favoriteContact(contactId: string): Promise<ContactItem> {
    const response = await $apiClient.post<ApiResponse<ContactItem>>(`/api/v1/contacts/${contactId}/favorite`)
    return response.data.data
  }

  async function unfavoriteContact(contactId: string): Promise<ContactItem> {
    const response = await $apiClient.post<ApiResponse<ContactItem>>(`/api/v1/contacts/${contactId}/unfavorite`)
    return response.data.data
  }

  async function quickAddContact(email: string, displayName?: string): Promise<ContactItem> {
    const response = await $apiClient.post<ApiResponse<ContactItem>>('/api/v1/contacts/quick-add', {
      email,
      displayName
    })
    return response.data.data
  }

  async function fetchSuggestions(keyword = '', limit = 8): Promise<ContactSuggestion[]> {
    const response = await $apiClient.get<ApiResponse<ContactSuggestion[]>>('/api/v1/contacts/suggestions', {
      params: { keyword, limit }
    })
    return response.data.data
  }

  async function importContactsCsv(content: string, mergeDuplicates: boolean): Promise<ContactImportResult> {
    const response = await $apiClient.post<ApiResponse<ContactImportResult>>('/api/v1/contacts/import/csv', {
      content,
      mergeDuplicates
    })
    return response.data.data
  }

  async function exportContacts(format: 'csv' | 'vcard'): Promise<string> {
    const response = await $apiClient.get<ApiResponse<string>>('/api/v1/contacts/export', {
      params: { format }
    })
    return response.data.data
  }

  async function listDuplicateContacts(): Promise<ContactDuplicateGroup[]> {
    const response = await $apiClient.get<ApiResponse<ContactDuplicateGroup[]>>('/api/v1/contacts/duplicates')
    return response.data.data
  }

  async function mergeDuplicateContacts(primaryContactId: string, duplicateContactIds: string[]): Promise<ContactItem> {
    const response = await $apiClient.post<ApiResponse<ContactItem>>('/api/v1/contacts/duplicates/merge', {
      primaryContactId,
      duplicateContactIds
    })
    return response.data.data
  }

  async function listGroups(): Promise<ContactGroup[]> {
    const response = await $apiClient.get<ApiResponse<ContactGroup[]>>('/api/v1/contact-groups')
    return response.data.data
  }

  async function createGroup(payload: CreateContactGroupRequest): Promise<ContactGroup> {
    const response = await $apiClient.post<ApiResponse<ContactGroup>>('/api/v1/contact-groups', payload)
    return response.data.data
  }

  async function updateGroup(groupId: string, payload: UpdateContactGroupRequest): Promise<ContactGroup> {
    const response = await $apiClient.put<ApiResponse<ContactGroup>>(`/api/v1/contact-groups/${groupId}`, payload)
    return response.data.data
  }

  async function deleteGroup(groupId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/contact-groups/${groupId}`)
  }

  async function listGroupMembers(groupId: string): Promise<ContactItem[]> {
    const response = await $apiClient.get<ApiResponse<ContactItem[]>>(`/api/v1/contact-groups/${groupId}/members`)
    return response.data.data
  }

  async function addGroupMembers(groupId: string, payload: UpdateContactGroupMembersRequest): Promise<ContactItem[]> {
    const response = await $apiClient.post<ApiResponse<ContactItem[]>>(`/api/v1/contact-groups/${groupId}/members`, payload)
    return response.data.data
  }

  async function removeGroupMember(groupId: string, contactId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/contact-groups/${groupId}/members/${contactId}`)
  }

  return {
    listContacts,
    createContact,
    updateContact,
    deleteContact,
    favoriteContact,
    unfavoriteContact,
    quickAddContact,
    fetchSuggestions,
    importContactsCsv,
    exportContacts,
    listDuplicateContacts,
    mergeDuplicateContacts,
    listGroups,
    createGroup,
    updateGroup,
    deleteGroup,
    listGroupMembers,
    addGroupMembers,
    removeGroupMember
  }
}
