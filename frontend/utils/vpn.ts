import type {
  CreateVpnProfileRequest,
  UpdateVpnSettingsRequest,
  VpnHeroMetric,
  VpnDefaultConnectionMode,
  VpnNetShieldMode,
  VpnProfileDraft,
  VpnProfileItem,
  VpnSettingsDraft,
  VpnWorkspaceSnapshot
} from '~/types/vpn'

const LOAD_WARNING_THRESHOLD = 55
const LOAD_DANGER_THRESHOLD = 80

interface VpnSettingsDraftSource {
  netshieldMode?: VpnNetShieldMode | null
  killSwitchEnabled?: boolean | null
  defaultConnectionMode?: VpnDefaultConnectionMode | null
  defaultProfileId?: string | null
}

export function createVpnSettingsDraft(settings?: VpnSettingsDraftSource | null): VpnSettingsDraft {
  return {
    netshieldMode: settings?.netshieldMode || 'OFF',
    killSwitchEnabled: settings?.killSwitchEnabled ?? false,
    defaultConnectionMode: settings?.defaultConnectionMode || 'FASTEST',
    defaultProfileId: settings?.defaultProfileId || ''
  }
}

export function createVpnProfileDraft(profile?: Partial<VpnProfileItem> | null): VpnProfileDraft {
  return {
    name: profile?.name || '',
    protocol: profile?.protocol || 'WIREGUARD',
    routingMode: profile?.routingMode || 'FASTEST',
    targetServerId: profile?.targetServerId || '',
    targetCountry: profile?.targetCountry || '',
    secureCoreEnabled: profile?.secureCoreEnabled ?? false,
    netshieldMode: profile?.netshieldMode || 'OFF',
    killSwitchEnabled: profile?.killSwitchEnabled ?? false
  }
}

export function buildVpnSettingsPayload(draft: VpnSettingsDraft): UpdateVpnSettingsRequest {
  const defaultProfileId = draft.defaultProfileId.trim()
  if (draft.defaultConnectionMode === 'PROFILE' && !defaultProfileId) {
    throw new Error('vpn.settings.profileRequired')
  }
  return {
    netshieldMode: draft.netshieldMode,
    killSwitchEnabled: draft.killSwitchEnabled,
    defaultConnectionMode: draft.defaultConnectionMode,
    defaultProfileId: draft.defaultConnectionMode === 'PROFILE' ? defaultProfileId : null
  }
}

export function buildVpnProfilePayload(draft: VpnProfileDraft): CreateVpnProfileRequest {
  const name = draft.name.trim()
  if (!name) {
    throw new Error('vpn.profile.nameRequired')
  }
  const targetServerId = draft.targetServerId.trim().toUpperCase()
  const targetCountry = draft.targetCountry.trim()
  if (draft.routingMode === 'SERVER' && !targetServerId) {
    throw new Error('vpn.profile.serverRequired')
  }
  if (draft.routingMode === 'COUNTRY' && !targetCountry) {
    throw new Error('vpn.profile.countryRequired')
  }
  return {
    name,
    protocol: draft.protocol,
    routingMode: draft.routingMode,
    targetServerId: draft.routingMode === 'SERVER' ? targetServerId : null,
    targetCountry: draft.routingMode === 'COUNTRY' ? targetCountry : null,
    secureCoreEnabled: draft.secureCoreEnabled,
    netshieldMode: draft.netshieldMode,
    killSwitchEnabled: draft.killSwitchEnabled
  }
}

export function formatVpnDuration(seconds: number): string {
  const safeSeconds = Math.max(0, Math.floor(seconds))
  const hours = String(Math.floor(safeSeconds / 3600)).padStart(2, '0')
  const minutes = String(Math.floor((safeSeconds % 3600) / 60)).padStart(2, '0')
  const remainingSeconds = String(safeSeconds % 60).padStart(2, '0')
  return `${hours}:${minutes}:${remainingSeconds}`
}

export function formatVpnTimestamp(value: string | null): string {
  if (!value) {
    return '—'
  }
  return value.replace('T', ' ')
}

export function resolveVpnLoadTag(loadPercent: number): 'success' | 'warning' | 'danger' {
  if (loadPercent >= LOAD_DANGER_THRESHOLD) {
    return 'danger'
  }
  if (loadPercent >= LOAD_WARNING_THRESHOLD) {
    return 'warning'
  }
  return 'success'
}

export function resolveVpnTargetSummary(profile: Pick<VpnProfileItem, 'routingMode' | 'targetServerId' | 'targetCountry' | 'secureCoreEnabled'>): string {
  if (profile.routingMode === 'SERVER') {
    return `SERVER · ${profile.targetServerId || '—'}`
  }
  if (profile.routingMode === 'COUNTRY') {
    return `COUNTRY · ${profile.targetCountry || '—'}`
  }
  return profile.secureCoreEnabled ? 'FASTEST · SECURE_CORE' : 'FASTEST · STANDARD'
}

export function resolveVpnDefaultProfileName(settings: VpnSettingsDraft, profiles: VpnProfileItem[]): string | null {
  if (settings.defaultConnectionMode !== 'PROFILE' || !settings.defaultProfileId) {
    return null
  }
  return profiles.find((profile) => profile.profileId === settings.defaultProfileId)?.name || null
}

export function countSecureCoreProfiles(profiles: VpnProfileItem[]): number {
  return profiles.filter((profile) => profile.secureCoreEnabled).length
}

export function buildVpnHeroMetrics(snapshot: VpnWorkspaceSnapshot): VpnHeroMetric[] {
  const onlineServers = snapshot.servers.filter((server) => server.status === 'ONLINE').length
  const secureCoreServers = snapshot.servers.filter((server) => server.tier === 'SECURE_CORE' && server.status === 'ONLINE').length
  const defaultProfileName = resolveVpnDefaultProfileName(snapshot.settings, snapshot.profiles)
  return [
    {
      key: 'servers',
      value: String(onlineServers),
      hint: `${secureCoreServers} secure core`
    },
    {
      key: 'profiles',
      value: String(snapshot.profiles.length),
      hint: `${countSecureCoreProfiles(snapshot.profiles)} secure core`
    },
    {
      key: 'route',
      value: snapshot.currentSession?.serverId || defaultProfileName || snapshot.settings.defaultConnectionMode,
      hint: snapshot.currentSession?.connectionSource || snapshot.settings.defaultConnectionMode
    },
    {
      key: 'hardening',
      value: snapshot.currentSession?.netshieldMode || snapshot.settings.netshieldMode,
      hint: snapshot.currentSession?.killSwitchEnabled || snapshot.settings.killSwitchEnabled ? 'KILL_SWITCH_ON' : 'KILL_SWITCH_OFF'
    }
  ]
}
