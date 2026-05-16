import type { ElegantConstRoute } from '@elegant-router/types';

type RouteMetaPatch = Partial<NonNullable<ElegantConstRoute['meta']>>;

export const accessRouteMetaPatches: Partial<Record<string, RouteMetaPatch>> = {
  wallet: {
    orgRequired: true,
    premiumOnly: true,
    featureFlag: 'feat.wallet.enabled',
    requires: ['WALLET'],
    fallback: 'upgrade'
  },
  vpn: {
    orgRequired: false,
    premiumOnly: true,
    featureFlag: 'feat.vpn.enabled',
    requires: ['VPN'],
    fallback: 'upgrade'
  },
  meet: {
    orgRequired: false,
    premiumOnly: true,
    featureFlag: 'feat.meet.enabled',
    requires: ['MEET'],
    fallback: 'contact-sales'
  },
  contacts: {
    orgRequired: false,
    requires: []
  },
  community: {
    orgRequired: false,
    requires: [],
    featureFlag: 'feat.community.enabled',
    fallback: 'trial'
  },
  integrations: {
    title: 'integrations',
    i18nKey: 'route.integrations',
    icon: 'mdi:puzzle-outline',
    order: 18
  },
  integrations_simplelogin: {
    title: 'integrations_simplelogin',
    i18nKey: 'route.integrations_simplelogin',
    orgRequired: true,
    premiumOnly: true,
    featureFlag: 'feat.simplelogin.enabled',
    requires: ['SIMPLE_LOGIN'],
    fallback: 'upgrade'
  }
};
