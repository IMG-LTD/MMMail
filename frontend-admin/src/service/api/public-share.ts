import { request } from '../request';

export function readPublicMailShare(token: string) {
  return request<Api.PublicShare.MailShare>({ url: `/api/v2/share/mail/${token}` });
}

export function downloadPublicMailAttachment(token: string, attachmentId: string) {
  return request<Blob, 'blob'>({
    url: `/api/v2/share/mail/${token}/attachments/${attachmentId}/download`,
    responseType: 'blob'
  });
}

export function readPublicPassShare(token: string) {
  return request<Api.PublicShare.PassShare>({ url: `/api/v2/share/pass/${token}` });
}
