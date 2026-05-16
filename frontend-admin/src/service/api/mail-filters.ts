import { request } from '../request';

type MailFilterPayload = {
  enabled?: boolean;
  keywordContains?: string;
  labels?: string[];
  markRead?: boolean;
  name: string;
  senderContains?: string;
  subjectContains?: string;
  targetCustomFolderId?: number | null;
  targetFolder?: string;
};

type PreviewPayload = {
  body?: string;
  senderEmail: string;
  subject?: string;
};

export function listMailFilters() {
  return request<Api.MailFilters.Filter[]>({ url: '/api/v1/mail-filters' });
}

export function createMailFilter(data: MailFilterPayload) {
  return request<Api.MailFilters.Filter>({
    url: '/api/v1/mail-filters',
    method: 'post',
    data
  });
}

export function updateMailFilter(filterId: string, data: MailFilterPayload) {
  return request<Api.MailFilters.Filter>({
    url: `/api/v1/mail-filters/${filterId}`,
    method: 'put',
    data
  });
}

export function deleteMailFilter(filterId: string) {
  return request<void>({
    url: `/api/v1/mail-filters/${filterId}`,
    method: 'delete'
  });
}

export function previewMailFilter(data: PreviewPayload) {
  return request<Api.MailFilters.Preview>({
    url: '/api/v1/mail-filters/preview',
    method: 'post',
    data
  });
}
