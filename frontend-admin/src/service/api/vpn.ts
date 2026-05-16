import { request } from '../request';

export function listVpnServers() {
  return request<Api.Vpn.Server[]>({ url: '/api/v1/vpn/servers' });
}

export function listVpnProfiles() {
  return request<Api.Vpn.Profile[]>({ url: '/api/v1/vpn/profiles' });
}

export function createVpnProfile(data: Api.Vpn.ProfilePayload) {
  return request<Api.Vpn.Profile>({
    url: '/api/v1/vpn/profiles',
    method: 'post',
    data
  });
}

export function updateVpnProfile(profileId: string, data: Api.Vpn.ProfilePayload) {
  return request<Api.Vpn.Profile>({
    url: `/api/v1/vpn/profiles/${profileId}`,
    method: 'put',
    data
  });
}

export function deleteVpnProfile(profileId: string) {
  return request<boolean>({
    url: `/api/v1/vpn/profiles/${profileId}`,
    method: 'delete'
  });
}

export function readVpnSettings() {
  return request<Api.Vpn.Settings>({ url: '/api/v1/vpn/settings' });
}

export function updateVpnSettings(data: Api.Vpn.SettingsPayload) {
  return request<Api.Vpn.Settings>({
    url: '/api/v1/vpn/settings',
    method: 'put',
    data
  });
}

export function readCurrentVpnSession() {
  return request<Api.Vpn.Session | null>({ url: '/api/v1/vpn/sessions/current' });
}

export function listVpnSessionHistory(params: Api.Vpn.HistoryParams = {}) {
  return request<Api.Vpn.Session[]>({ url: '/api/v1/vpn/sessions/history', params });
}

export function connectVpnSession(data: Api.Vpn.ConnectPayload) {
  return request<Api.Vpn.Session>({
    url: '/api/v1/vpn/sessions/connect',
    method: 'post',
    data
  });
}

export function quickConnectVpn(data: Api.Vpn.QuickConnectPayload = {}) {
  return request<Api.Vpn.Session>({
    url: '/api/v1/vpn/sessions/quick-connect',
    method: 'post',
    data
  });
}

export function disconnectVpn() {
  return request<Api.Vpn.Session | null>({
    url: '/api/v1/vpn/sessions/disconnect',
    method: 'post'
  });
}
