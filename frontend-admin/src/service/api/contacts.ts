import { request } from '../request';

type ContactPayload = {
  displayName: string;
  email: string;
  note?: string;
};

export function listContacts(params: Record<string, string | number | undefined> = {}) {
  return request<Api.Contacts.Contact[]>({ url: '/api/v1/contacts', params });
}

export function createContact(data: ContactPayload) {
  return request<Api.Contacts.Contact>({
    url: '/api/v1/contacts',
    method: 'post',
    data
  });
}

export function updateContact(contactId: string, data: ContactPayload) {
  return request<Api.Contacts.Contact>({
    url: `/api/v1/contacts/${contactId}`,
    method: 'put',
    data
  });
}

export function deleteContact(contactId: string) {
  return request<void>({
    url: `/api/v1/contacts/${contactId}`,
    method: 'delete'
  });
}

export function favoriteContact(contactId: string) {
  return request<Api.Contacts.Contact>({
    url: `/api/v1/contacts/${contactId}/favorite`,
    method: 'post'
  });
}

export function unfavoriteContact(contactId: string) {
  return request<Api.Contacts.Contact>({
    url: `/api/v1/contacts/${contactId}/unfavorite`,
    method: 'post'
  });
}

export function listContactSuggestions(params: Api.Contacts.SuggestionParams = {}) {
  return request<Api.Contacts.Suggestion[]>({ url: '/api/v1/contacts/suggestions', params });
}

export function importContactsCsv(data: Api.Contacts.ImportCsvPayload) {
  return request<Api.Contacts.ImportResult>({
    url: '/api/v1/contacts/import/csv',
    method: 'post',
    data
  });
}

export function exportContacts(params: Api.Contacts.ExportParams = {}) {
  return request<string>({ url: '/api/v1/contacts/export', params });
}

export function listContactDuplicates() {
  return request<Api.Contacts.DuplicateGroup[]>({ url: '/api/v1/contacts/duplicates' });
}

export function mergeDuplicateContacts(data: Api.Contacts.MergeDuplicatesPayload) {
  return request<Api.Contacts.Contact>({
    url: '/api/v1/contacts/duplicates/merge',
    method: 'post',
    data
  });
}

export function listContactGroups() {
  return request<Api.Contacts.Group[]>({ url: '/api/v1/contact-groups' });
}
