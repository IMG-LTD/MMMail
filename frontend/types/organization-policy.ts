import type { OrgPolicy, UpdateOrgPolicyRequest } from '~/types/api'

export type TwoFactorEnforcementLevel = 'OFF' | 'ADMINS' | 'ALL'

export interface OrganizationPolicy extends OrgPolicy {
  twoFactorEnforcementLevel: TwoFactorEnforcementLevel
  twoFactorGracePeriodDays: number
}

export interface UpdateOrganizationPolicyRequest extends UpdateOrgPolicyRequest {
  twoFactorEnforcementLevel?: TwoFactorEnforcementLevel
  twoFactorGracePeriodDays?: number
}
