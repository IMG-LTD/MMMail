import { describe, expect, it } from 'vitest';

interface AccessState {
  roles: string[];
  entitlements: string[];
  featureFlags: string[];
  currentOrgId: string;
}

interface AccessMeta {
  orgRequired?: boolean;
  featureFlag?: string;
  roles?: string[];
  role?: string;
  requires?: string[];
  anyOf?: string[];
}

function canAccess(state: AccessState, meta: AccessMeta): boolean {
  const orgOk = !meta.orgRequired || Boolean(state.currentOrgId);
  const flagOk = !meta.featureFlag || state.featureFlags.includes(meta.featureFlag);
  const rolesNeeded = meta.role ? [...(meta.roles ?? []), meta.role] : (meta.roles ?? []);
  const roleOk = !rolesNeeded.length || rolesNeeded.some(r => state.roles.includes(r));
  const requiresOk = (meta.requires ?? []).every(item => state.entitlements.includes(item));
  const anyOfOk = !meta.anyOf?.length || meta.anyOf.some(item => state.entitlements.includes(item));
  return orgOk && flagOk && roleOk && requiresOk && anyOfOk;
}

const empty: AccessState = { roles: [], entitlements: [], featureFlags: [], currentOrgId: '' };
const member: AccessState = {
  roles: ['ORG_MEMBER'],
  entitlements: ['WALLET'],
  featureFlags: ['feat.wallet.enabled'],
  currentOrgId: 'org_1'
};

describe('entitlement resolve decision table', () => {
  it('allows access when meta is empty', () => {
    expect(canAccess(empty, {})).toBe(true);
  });

  it('blocks orgRequired when there is no current org', () => {
    expect(canAccess(empty, { orgRequired: true })).toBe(false);
    expect(canAccess(member, { orgRequired: true })).toBe(true);
  });

  it('blocks when the feature flag is not present', () => {
    expect(canAccess(member, { featureFlag: 'feat.unknown' })).toBe(false);
    expect(canAccess(member, { featureFlag: 'feat.wallet.enabled' })).toBe(true);
  });

  it('requires every entry in `requires`', () => {
    expect(canAccess(member, { requires: ['WALLET'] })).toBe(true);
    expect(canAccess(member, { requires: ['WALLET', 'VPN'] })).toBe(false);
  });

  it('passes when at least one entry in `anyOf` is present', () => {
    expect(canAccess(member, { anyOf: ['WALLET', 'VPN'] })).toBe(true);
    expect(canAccess(member, { anyOf: ['VPN', 'MEET'] })).toBe(false);
  });

  it('treats `role` as an additional acceptable role on top of `roles`', () => {
    expect(canAccess(member, { roles: ['ROLE_X'], role: 'ORG_MEMBER' })).toBe(true);
    expect(canAccess(member, { roles: ['ROLE_X'], role: 'ROLE_Y' })).toBe(false);
  });

  it('combines all four guards and short-circuits the first failing one', () => {
    expect(
      canAccess(member, {
        orgRequired: true,
        featureFlag: 'feat.wallet.enabled',
        requires: ['WALLET'],
        anyOf: ['WALLET']
      })
    ).toBe(true);
    expect(
      canAccess(member, {
        orgRequired: true,
        featureFlag: 'feat.wallet.enabled',
        requires: ['WALLET', 'VPN']
      })
    ).toBe(false);
  });
});
