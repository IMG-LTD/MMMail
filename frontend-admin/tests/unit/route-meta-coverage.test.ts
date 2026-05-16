import { describe, expect, it } from 'vitest';
import { accessRouteMetaPatches } from '@/router/routes/access-meta';

const PREMIUM_KEYS = ['wallet', 'vpn', 'meet'];

describe('access-meta coverage contract', () => {
  it('every patch entry that opts into premiumOnly also declares fallback + requires', () => {
    for (const [key, patch] of Object.entries(accessRouteMetaPatches)) {
      if (!patch?.premiumOnly) continue;
      expect(patch.fallback, `${key} missing fallback`).toBeTruthy();
      expect(patch.requires?.length, `${key} missing requires`).toBeGreaterThan(0);
    }
  });

  it('every paid product surface is declared in the patch table', () => {
    for (const key of PREMIUM_KEYS) {
      expect(
        accessRouteMetaPatches[key as keyof typeof accessRouteMetaPatches],
        `missing patch for ${key}`
      ).toBeTruthy();
    }
  });

  it('integrations namespace links to a stable i18n key and order slot', () => {
    expect(accessRouteMetaPatches.integrations?.i18nKey).toBe('route.integrations');
    expect(typeof accessRouteMetaPatches.integrations?.order).toBe('number');
  });
});
