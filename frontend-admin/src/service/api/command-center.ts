import { request } from '../request';

export function listCommandCenterCommands() {
  return request<Api.CommandCenter.Command[]>({ url: '/api/v2/command-center/commands' });
}

export function readCommandCenterCommand(commandId: string) {
  return request<Api.CommandCenter.Command>({ url: `/api/v2/command-center/commands/${commandId}` });
}

export function listCommandPanelCatalog(params: Api.CommandCenter.CatalogParams = {}) {
  return request<Api.CommandCenter.CatalogItem[]>({ url: '/api/v2/command-center/catalog', params });
}

export function listCommandPanelRecents(params: Api.CommandCenter.RecentParams = {}) {
  return request<Api.CommandCenter.Recent[]>({ url: '/api/v2/command-center/recents', params });
}

export function pinCommandPanelCommand(data: Api.CommandCenter.PinPayload) {
  return request<Api.CommandCenter.Preference>({
    url: '/api/v2/command-center/pin',
    method: 'post',
    data
  });
}

export function quickSearchCommandPanel(params: Api.CommandCenter.QuickSearchParams) {
  return request<Api.CommandCenter.QuickSearchItem[]>({ url: '/api/v2/command-center/quick-search', params });
}
