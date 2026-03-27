export type VpnServerTier = 'STANDARD' | 'SECURE_CORE'
export type VpnServerStatus = 'ONLINE' | 'MAINTENANCE'
export type VpnProtocol = 'WIREGUARD' | 'OPENVPN_UDP' | 'OPENVPN_TCP'
export type VpnSessionStatus = 'CONNECTED' | 'DISCONNECTED'
export type VpnConnectionSource = 'MANUAL' | 'QUICK_CONNECT' | 'PROFILE'
export type VpnDefaultConnectionMode = 'FASTEST' | 'RANDOM' | 'LAST_CONNECTION' | 'PROFILE'
export type VpnNetShieldMode = 'OFF' | 'BLOCK_MALWARE' | 'BLOCK_MALWARE_ADS_TRACKERS'
export type VpnProfileRoutingMode = 'FASTEST' | 'COUNTRY' | 'SERVER'

export interface VpnServerItem {
  serverId: string
  country: string
  city: string
  tier: VpnServerTier
  status: VpnServerStatus
  loadPercent: number
}

export interface VpnSessionItem {
  sessionId: string
  serverId: string
  serverCountry: string
  serverCity: string
  serverTier: VpnServerTier
  protocol: VpnProtocol
  status: VpnSessionStatus
  profileId: string | null
  profileName: string | null
  netshieldMode: VpnNetShieldMode
  killSwitchEnabled: boolean
  connectionSource: VpnConnectionSource
  connectedAt: string
  disconnectedAt: string | null
  durationSeconds: number
}

export interface VpnSettingsItem {
  netshieldMode: VpnNetShieldMode
  killSwitchEnabled: boolean
  defaultConnectionMode: VpnDefaultConnectionMode
  defaultProfileId: string | null
}

export interface VpnProfileItem {
  profileId: string
  name: string
  protocol: VpnProtocol
  routingMode: VpnProfileRoutingMode
  targetServerId: string | null
  targetCountry: string | null
  secureCoreEnabled: boolean
  netshieldMode: VpnNetShieldMode
  killSwitchEnabled: boolean
  createdAt: string
  updatedAt: string
}

export interface ConnectVpnSessionRequest {
  serverId: string
  protocol: VpnProtocol
}

export interface QuickConnectVpnSessionRequest {
  profileId?: string
}

export interface UpdateVpnSettingsRequest {
  netshieldMode: VpnNetShieldMode
  killSwitchEnabled: boolean
  defaultConnectionMode: VpnDefaultConnectionMode
  defaultProfileId: string | null
}

export interface CreateVpnProfileRequest {
  name: string
  protocol: VpnProtocol
  routingMode: VpnProfileRoutingMode
  targetServerId: string | null
  targetCountry: string | null
  secureCoreEnabled: boolean
  netshieldMode: VpnNetShieldMode
  killSwitchEnabled: boolean
}

export type UpdateVpnProfileRequest = CreateVpnProfileRequest

export interface VpnSettingsDraft {
  netshieldMode: VpnNetShieldMode
  killSwitchEnabled: boolean
  defaultConnectionMode: VpnDefaultConnectionMode
  defaultProfileId: string
}

export interface VpnProfileDraft {
  name: string
  protocol: VpnProtocol
  routingMode: VpnProfileRoutingMode
  targetServerId: string
  targetCountry: string
  secureCoreEnabled: boolean
  netshieldMode: VpnNetShieldMode
  killSwitchEnabled: boolean
}

export interface VpnHeroMetric {
  key: string
  value: string
  hint: string
}

export interface VpnWorkspaceSnapshot {
  servers: VpnServerItem[]
  profiles: VpnProfileItem[]
  settings: VpnSettingsDraft
  currentSession: VpnSessionItem | null
}
