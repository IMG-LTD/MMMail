import { request } from '../request';

type NotePayload = {
  content: string;
  folderId?: string;
  noteType: string;
  pinned?: boolean;
  tags: string[];
  title: string;
};

type UpdateNotePayload = NotePayload & {
  archived?: boolean;
  currentVersion: number;
};

export function readStandardNotesOverview() {
  return request<Api.StandardNotes.Overview>({ url: '/api/v1/standard-notes/overview' });
}

export function listStandardNoteFolders() {
  return request<Api.StandardNotes.Folder[]>({ url: '/api/v1/standard-notes/folders' });
}

export function createStandardNoteFolder(data: { color?: string; description?: string; name: string }) {
  return request<Api.StandardNotes.Folder>({
    url: '/api/v1/standard-notes/folders',
    method: 'post',
    data
  });
}

export function listStandardNotes(params: Record<string, string | number | boolean | undefined> = {}) {
  return request<Api.StandardNotes.Summary[]>({ url: '/api/v1/standard-notes/notes', params });
}

export function createStandardNote(data: NotePayload) {
  return request<Api.StandardNotes.Detail>({
    url: '/api/v1/standard-notes/notes',
    method: 'post',
    data
  });
}

export function readStandardNote(noteId: string) {
  return request<Api.StandardNotes.Detail>({ url: `/api/v1/standard-notes/notes/${noteId}` });
}

export function updateStandardNote(noteId: string, data: UpdateNotePayload) {
  return request<Api.StandardNotes.Detail>({
    url: `/api/v1/standard-notes/notes/${noteId}`,
    method: 'put',
    data
  });
}

export function toggleStandardNoteChecklistItem(
  noteId: string,
  itemIndex: number,
  data: { completed: boolean; currentVersion: number }
) {
  return request<Api.StandardNotes.Detail>({
    url: `/api/v1/standard-notes/notes/${noteId}/checklist-items/${itemIndex}/toggle`,
    method: 'post',
    data
  });
}

export function deleteStandardNote(noteId: string) {
  return request<void>({
    url: `/api/v1/standard-notes/notes/${noteId}`,
    method: 'delete'
  });
}

export function exportStandardNotes() {
  return request<Api.StandardNotes.Export>({ url: '/api/v1/standard-notes/export' });
}
