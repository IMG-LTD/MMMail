import type { MeetAccessOverview, MeetAccessState } from '~/types/api'

type AccessTagType = 'success' | 'warning' | 'info'

export function meetWorkspaceUnlocked(overview: MeetAccessOverview | null): boolean {
  return overview?.accessGranted ?? false
}

export function meetAccessStateTagType(state?: MeetAccessState | null): AccessTagType {
  if (state === 'GRANTED') {
    return 'success'
  }
  if (state === 'WAITLISTED') {
    return 'warning'
  }
  return 'info'
}

export function meetShouldShowWaitlistAction(overview: MeetAccessOverview | null): boolean {
  if (!overview) {
    return false
  }
  return !overview.accessGranted && !overview.eligibleForInstantAccess && !overview.waitlistRequested
}

export function meetShouldShowActivateAction(overview: MeetAccessOverview | null): boolean {
  if (!overview) {
    return false
  }
  return !overview.accessGranted && overview.eligibleForInstantAccess
}
