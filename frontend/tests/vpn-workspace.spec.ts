import { describe, expect, it } from 'vitest'
import type { VpnProfileItem } from '../types/vpn'
import {
  buildVpnHeroMetrics,
  buildVpnProfilePayload,
  buildVpnSettingsPayload,
  countSecureCoreProfiles,
  createVpnProfileDraft,
  createVpnSettingsDraft,
  formatVpnDuration,
  resolveVpnDefaultProfileName,
  resolveVpnLoadTag,
  resolveVpnTargetSummary
} from '../utils/vpn'

describe('vpn workspace utils', () => {
  it('builds settings and profile payloads with explicit validation', () => {
    const settingsDraft = createVpnSettingsDraft({
      netshieldMode: 'BLOCK_MALWARE',
      killSwitchEnabled: true,
      defaultConnectionMode: 'FASTEST',
      defaultProfileId: '2031884504265195521'
    })
    expect(buildVpnSettingsPayload(settingsDraft)).toEqual({
      netshieldMode: 'BLOCK_MALWARE',
      killSwitchEnabled: true,
      defaultConnectionMode: 'FASTEST',
      defaultProfileId: null
    })

    expect(buildVpnSettingsPayload({
      ...settingsDraft,
      defaultConnectionMode: 'PROFILE'
    })).toEqual({
      netshieldMode: 'BLOCK_MALWARE',
      killSwitchEnabled: true,
      defaultConnectionMode: 'PROFILE',
      defaultProfileId: '2031884504265195521'
    })

    expect(() => buildVpnSettingsPayload({
      ...settingsDraft,
      defaultConnectionMode: 'PROFILE',
      defaultProfileId: ''
    })).toThrowError('vpn.settings.profileRequired')

    const profileDraft = createVpnProfileDraft({
      name: ' Secure route ',
      protocol: 'WIREGUARD',
      routingMode: 'SERVER',
      targetServerId: 'ch-gva-sc1',
      targetCountry: 'Switzerland',
      secureCoreEnabled: true,
      netshieldMode: 'BLOCK_MALWARE_ADS_TRACKERS',
      killSwitchEnabled: true
    })
    expect(buildVpnProfilePayload(profileDraft)).toEqual({
      name: 'Secure route',
      protocol: 'WIREGUARD',
      routingMode: 'SERVER',
      targetServerId: 'CH-GVA-SC1',
      targetCountry: null,
      secureCoreEnabled: true,
      netshieldMode: 'BLOCK_MALWARE_ADS_TRACKERS',
      killSwitchEnabled: true
    })
  })

  it('derives metrics, labels, and formatting helpers', () => {
    const profiles: VpnProfileItem[] = [
      {
        profileId: 'p-1',
        name: 'Swiss secure core',
        protocol: 'WIREGUARD',
        routingMode: 'SERVER',
        targetServerId: 'CH-GVA-SC1',
        targetCountry: null,
        secureCoreEnabled: true,
        netshieldMode: 'BLOCK_MALWARE',
        killSwitchEnabled: true,
        createdAt: '2026-03-12T07:00:00',
        updatedAt: '2026-03-12T07:00:00'
      },
      {
        profileId: 'p-2',
        name: 'Nordics',
        protocol: 'OPENVPN_TCP',
        routingMode: 'COUNTRY',
        targetServerId: null,
        targetCountry: 'Sweden',
        secureCoreEnabled: false,
        netshieldMode: 'OFF',
        killSwitchEnabled: false,
        createdAt: '2026-03-12T07:01:00',
        updatedAt: '2026-03-12T07:01:00'
      }
    ]

    expect(resolveVpnDefaultProfileName({
      netshieldMode: 'OFF',
      killSwitchEnabled: false,
      defaultConnectionMode: 'PROFILE',
      defaultProfileId: 'p-1'
    }, profiles)).toBe('Swiss secure core')
    expect(resolveVpnTargetSummary(profiles[0])).toBe('SERVER · CH-GVA-SC1')
    expect(resolveVpnTargetSummary(profiles[1])).toBe('COUNTRY · Sweden')
    expect(countSecureCoreProfiles(profiles)).toBe(1)
    expect(formatVpnDuration(3661)).toBe('01:01:01')
    expect(resolveVpnLoadTag(29)).toBe('success')
    expect(resolveVpnLoadTag(62)).toBe('warning')
    expect(resolveVpnLoadTag(92)).toBe('danger')

    const metrics = buildVpnHeroMetrics({
      servers: [
        { serverId: 'SE-STO-04', country: 'Sweden', city: 'Stockholm', tier: 'STANDARD', status: 'ONLINE', loadPercent: 29 },
        { serverId: 'CH-GVA-SC1', country: 'Switzerland', city: 'Geneva', tier: 'SECURE_CORE', status: 'ONLINE', loadPercent: 61 },
        { serverId: 'JP-TYO-03', country: 'Japan', city: 'Tokyo', tier: 'STANDARD', status: 'MAINTENANCE', loadPercent: 0 }
      ],
      profiles,
      settings: {
        netshieldMode: 'BLOCK_MALWARE_ADS_TRACKERS',
        killSwitchEnabled: true,
        defaultConnectionMode: 'PROFILE',
        defaultProfileId: 'p-1'
      },
      currentSession: null
    })

    expect(metrics).toEqual([
      { key: 'servers', value: '2', hint: '1 secure core' },
      { key: 'profiles', value: '2', hint: '1 secure core' },
      { key: 'route', value: 'Swiss secure core', hint: 'PROFILE' },
      { key: 'hardening', value: 'BLOCK_MALWARE_ADS_TRACKERS', hint: 'KILL_SWITCH_ON' }
    ])
  })
})
