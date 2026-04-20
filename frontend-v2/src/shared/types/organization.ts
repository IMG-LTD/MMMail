import type { OrgRole } from './api'

export type OrgProductKey =
  | 'MAIL'
  | 'CALENDAR'
  | 'DRIVE'
  | 'DOCS'
  | 'SHEETS'
  | 'PASS'
  | 'SIMPLELOGIN'
  | 'STANDARD_NOTES'
  | 'VPN'
  | 'WALLET'
  | 'AUTHENTICATOR'
  | 'MEET'
  | 'LUMO'

export type OrgProductAccessState = 'ENABLED' | 'DISABLED'

export interface OrgProductAccessItem {
  productKey: OrgProductKey
  accessState: OrgProductAccessState
}

export interface OrgAccessScope {
  orgId: string
  orgName: string
  orgSlug: string
  role: OrgRole
  enabledProductCount: number
  products: OrgProductAccessItem[]
}
