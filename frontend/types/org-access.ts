import type { OrgRole } from '~/types/api'
import type { OrgProductAccessItem, OrgProductKey } from '~/types/organization-admin'

export interface OrgAccessScope {
  orgId: string
  orgName: string
  orgSlug: string
  role: OrgRole
  enabledProductCount: number
  products: OrgProductAccessItem[]
}

export interface OrgScopeOption {
  value: string
  label: string
}

export type ProductEnabledResolver = (productKey: OrgProductKey) => boolean
