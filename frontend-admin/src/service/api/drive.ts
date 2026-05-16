import { request } from '../request';

export function listDriveItems(params: Record<string, string | number | undefined> = {}) {
  return request<Api.Drive.Item[]>({ url: '/api/v2/drive/files', params });
}

export function listDriveFolders(params: Record<string, string | number | undefined> = {}) {
  return request<Api.Drive.Item[]>({ url: '/api/v2/drive/folders', params });
}

export function createDriveUpload(data: Api.Drive.UploadPayload) {
  return request<Api.Drive.Item>({
    url: '/api/v2/drive/uploads',
    method: 'post',
    data
  });
}

export function readDriveUsage() {
  return request<Api.Drive.Usage>({ url: '/api/v2/drive/storage/summary' });
}

export function createDriveShare(fileId: string, data: Api.Drive.SharePayload) {
  return request<Api.Drive.ShareLink>({
    url: `/api/v2/drive/files/${fileId}/share`,
    method: 'post',
    data
  });
}

export function createEncryptedDriveShare(fileId: string, data: Api.Drive.EncryptedSharePayload) {
  const formData = new FormData();
  formData.append('file', data.encryptedFile);
  formData.append('permission', data.permission);
  formData.append('password', data.password);
  formData.append('e2eeAlgorithm', data.e2eeAlgorithm);

  if (data.expiresAt) {
    formData.append('expiresAt', data.expiresAt);
  }

  return request<Api.Drive.ShareLink>({
    url: `/api/v1/drive/items/${fileId}/shares/e2ee`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

export function listDriveFileVersions(fileId: string, params: Record<string, number | undefined> = {}) {
  return request<Api.Drive.Version[]>({ url: `/api/v1/drive/files/${fileId}/versions`, params });
}

export function restoreDriveFileVersion(fileId: string, versionId: string) {
  return request<Api.Drive.Item>({
    url: `/api/v1/drive/files/${fileId}/versions/${versionId}/restore`,
    method: 'post'
  });
}

export function cleanupDriveFileVersions(fileId: string) {
  return request<Api.Drive.VersionCleanup>({
    url: `/api/v1/drive/files/${fileId}/versions/cleanup`,
    method: 'post'
  });
}

export function readPublicShareCapabilities() {
  return request<Api.Drive.PublicShareCapabilities>({ url: '/api/v2/public-share/capabilities' });
}

export function readPublicDriveShareMetadata(token: string) {
  return request<Api.Drive.PublicShareMetadata>({ url: `/api/v2/share/drive/${token}` });
}

export function downloadPublicDriveShareFile(token: string, itemId: string, password?: string) {
  return request<Blob, 'blob'>({
    url: `/api/v2/share/drive/${token}/items/${itemId}/download`,
    responseType: 'blob',
    headers: {
      'X-Drive-Share-Password': password || undefined
    }
  });
}

export function deleteDriveFile(fileId: string) {
  return request<void>({
    url: `/api/v2/drive/files/${fileId}`,
    method: 'delete'
  });
}
