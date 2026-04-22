import type { PassMailbox, PassMonitorItem, PassMonitorOverview, PassWorkspaceItemSummary } from '@/service/api/pass'

export const PASS_ITEMS_FETCH_LIMIT = 200

export interface PassSectionDerivedState {
  loadedItemCount: number
  personalItemCount: number
  sharedItemCount: number
  secureLinkItemCount: number
  aliasItemCount: number
  mailboxCount: number
  verifiedMailboxCount: number
  sharedVaultCount: number
  totalSecureLinkCount: number
  sharedSecureLinkCount: number
  policyItemCount: number
  trackedItemCount: number
  weakPasswordCount: number
  reusedPasswordCount: number
  inactiveTwoFactorCount: number
  excludedItemCount: number
  defaultMailboxEmail: string | null
  primaryMailboxEmail: string | null
  itemCoverageCapped: boolean
}

export function createPassMonitorIssueMap(monitor: PassMonitorOverview | null) {
  const map = new Map<string, PassMonitorItem>()
  const sources = [
    ...(monitor?.weakPasswords || []),
    ...(monitor?.reusedPasswords || []),
    ...(monitor?.inactiveTwoFactorItems || []),
    ...(monitor?.excludedItems || [])
  ]

  for (const item of sources) {
    const existing = map.get(item.id)

    if (!existing) {
      map.set(item.id, { ...item })
      continue
    }

    map.set(item.id, {
      ...existing,
      excluded: existing.excluded || item.excluded,
      inactiveTwoFactor: existing.inactiveTwoFactor || item.inactiveTwoFactor,
      reusedGroupSize: Math.max(existing.reusedGroupSize || 0, item.reusedGroupSize || 0),
      reusedPassword: existing.reusedPassword || item.reusedPassword,
      updatedAt: existing.updatedAt || item.updatedAt,
      weakPassword: existing.weakPassword || item.weakPassword
    })
  }

  return map
}

export function derivePassSectionState(
  items: PassWorkspaceItemSummary[],
  mailboxes: PassMailbox[],
  monitor: PassMonitorOverview | null,
  itemFetchLimit = PASS_ITEMS_FETCH_LIMIT
): PassSectionDerivedState {
  const sharedItems = items.filter(item => isPassItemShared(item))
  const monitorIssueMap = createPassMonitorIssueMap(monitor)
  const defaultMailbox = mailboxes.find(mailbox => mailbox.defaultMailbox) || mailboxes[0] || null
  const primaryMailbox = mailboxes.find(mailbox => mailbox.primaryMailbox) || mailboxes[0] || null

  return {
    aliasItemCount: items.filter(item => item.itemType === 'ALIAS').length,
    defaultMailboxEmail: defaultMailbox?.mailboxEmail || null,
    excludedItemCount: monitor?.excludedItemCount || 0,
    inactiveTwoFactorCount: monitor?.inactiveTwoFactorCount || 0,
    itemCoverageCapped: itemFetchLimit > 0 && items.length >= itemFetchLimit,
    loadedItemCount: items.length,
    mailboxCount: mailboxes.length,
    personalItemCount: items.filter(item => !isPassItemShared(item)).length,
    policyItemCount: monitorIssueMap.size,
    primaryMailboxEmail: primaryMailbox?.mailboxEmail || null,
    reusedPasswordCount: monitor?.reusedPasswordCount || 0,
    secureLinkItemCount: items.filter(item => item.secureLinkCount > 0).length,
    sharedItemCount: sharedItems.length,
    sharedSecureLinkCount: sumSecureLinks(sharedItems),
    sharedVaultCount: new Set(sharedItems.map(item => item.sharedVaultId).filter((value): value is string => Boolean(value))).size,
    totalSecureLinkCount: sumSecureLinks(items),
    trackedItemCount: monitor?.trackedItemCount || 0,
    verifiedMailboxCount: mailboxes.filter(mailbox => isPassMailboxVerified(mailbox)).length,
    weakPasswordCount: monitor?.weakPasswordCount || 0
  }
}

export function isPassItemShared(item: PassWorkspaceItemSummary) {
  return item.scopeType === 'SHARED' || Boolean(item.sharedVaultId)
}

export function isPassMailboxVerified(mailbox: PassMailbox) {
  return mailbox.status === 'VERIFIED' || Boolean(mailbox.verifiedAt)
}

function sumSecureLinks(items: PassWorkspaceItemSummary[]) {
  return items.reduce((total, item) => total + Math.max(0, item.secureLinkCount || 0), 0)
}
