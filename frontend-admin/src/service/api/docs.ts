import { request } from '../request';

export function listDocsNotes(params: Record<string, string | number | undefined> = {}) {
  return request<Api.Docs.NoteSummary[]>({ url: '/api/v2/docs', params });
}

export function createDocsNote(data: { title: string; content: string }) {
  return request<Api.Docs.NoteDetail>({
    url: '/api/v2/docs',
    method: 'post',
    data
  });
}

export function readDocsNote(noteId: string) {
  return request<Api.Docs.NoteDetail>({ url: `/api/v2/docs/${noteId}` });
}

export function updateDocsNote(noteId: string, data: { title: string; content: string; currentVersion: number }) {
  return request<Api.Docs.NoteDetail>({
    url: `/api/v2/docs/${noteId}`,
    method: 'patch',
    data
  });
}

export function getCollabSnapshot(resourceType: string, resourceId: string) {
  return request<Api.Docs.CollabSnapshot>({ url: `/api/v1/collab/${resourceType}/${resourceId}/snapshot` });
}

export function writeCollabSnapshot(resourceType: string, resourceId: string, data: Api.Docs.CollabSnapshotPayload) {
  return request<Api.Docs.CollabSnapshot>({
    url: `/api/v1/collab/${resourceType}/${resourceId}/snapshot`,
    method: 'post',
    data
  });
}

export function getCollabAwareness(resourceType: string, resourceId: string) {
  return request<Api.Docs.CollabAwareness>({ url: `/api/v1/collab/${resourceType}/${resourceId}/awareness` });
}
