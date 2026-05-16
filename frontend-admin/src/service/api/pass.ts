import { request } from '../request';

export function listPassVaults() {
  return request<Api.Pass.Vault[]>({ url: '/api/v2/pass/vaults' });
}

export function listPassItems(params: Record<string, string | number | boolean | undefined> = {}) {
  return request<Api.Pass.Item[]>({ url: '/api/v2/pass/items', params });
}

export function createPassItem(data: Record<string, string>) {
  return request<Api.Pass.Item>({
    url: '/api/v2/pass/items',
    method: 'post',
    data
  });
}

export function readPassMonitor() {
  return request<Api.Pass.Monitor>({ url: '/api/v2/pass/monitor' });
}
