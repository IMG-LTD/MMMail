declare namespace Api {
  namespace Vpn {
    type Protocol = 'WIREGUARD' | 'OPENVPN_UDP' | 'OPENVPN_TCP';
    type RoutingMode = 'FASTEST' | 'COUNTRY' | 'SERVER';
    type NetShieldMode = 'OFF' | 'BLOCK_MALWARE' | 'BLOCK_MALWARE_ADS_TRACKERS';
    type DefaultConnectionMode = 'FASTEST' | 'RANDOM' | 'LAST_CONNECTION' | 'PROFILE';

    interface Settings {
      netshieldMode: NetShieldMode;
      killSwitchEnabled: boolean;
      defaultConnectionMode: DefaultConnectionMode;
      defaultProfileId: string | null;
    }

    interface ProfilePayload {
      name: string;
      protocol: Protocol;
      routingMode: RoutingMode;
      targetServerId?: string;
      targetCountry?: string;
      secureCoreEnabled: boolean;
      netshieldMode: NetShieldMode;
      killSwitchEnabled: boolean;
    }

    interface SettingsPayload {
      netshieldMode: NetShieldMode;
      killSwitchEnabled: boolean;
      defaultConnectionMode: DefaultConnectionMode;
      defaultProfileId?: string;
    }

    interface ConnectPayload {
      serverId: string;
      protocol: Protocol;
    }

    interface QuickConnectPayload {
      profileId?: string;
    }

    interface HistoryParams {
      limit?: number;
    }
  }
}
