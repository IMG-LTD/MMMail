import { describe, expect, it } from 'vitest';
import { accessRouteMetaPatches } from '@/router/routes/access-meta';

describe('access route meta patches', () => {
  it('marks paid product routes with entitlements and fallbacks', () => {
    expect(accessRouteMetaPatches.wallet).toMatchObject({
      featureFlag: 'feat.wallet.enabled',
      fallback: 'upgrade',
      orgRequired: true,
      premiumOnly: true,
      requires: ['WALLET']
    });
    expect(accessRouteMetaPatches.vpn).toMatchObject({
      featureFlag: 'feat.vpn.enabled',
      fallback: 'upgrade',
      premiumOnly: true,
      requires: ['VPN']
    });
    expect(accessRouteMetaPatches.meet).toMatchObject({
      fallback: 'contact-sales',
      premiumOnly: true,
      requires: ['MEET']
    });
  });

  it('keeps public and integration routes explicitly described', () => {
    expect(accessRouteMetaPatches.contacts).toMatchObject({
      orgRequired: false,
      requires: []
    });
    expect(accessRouteMetaPatches.integrations).toMatchObject({
      icon: 'mdi:puzzle-outline',
      i18nKey: 'route.integrations',
      order: 18
    });
    expect(accessRouteMetaPatches.integrations_simplelogin).toMatchObject({
      featureFlag: 'feat.simplelogin.enabled',
      orgRequired: true,
      requires: ['SIMPLE_LOGIN']
    });
  });
});
