import type {
  CalendarAttendeeInput,
  CalendarAvailability,
  CalendarIncomingShare
} from '~/types/api'

export type CalendarIncomingShareFilter = 'ALL' | 'NEEDS_ACTION' | 'ACCEPTED' | 'DECLINED'

export function parseCalendarAttendeesInput(raw: string): CalendarAttendeeInput[] {
  const chunks = raw
    .split(/[\n,]+/)
    .map(item => item.trim())
    .filter(Boolean)

  const deduplicated = new Map<string, CalendarAttendeeInput>()
  for (const chunk of chunks) {
    const matched = chunk.match(/^(.*)<([^>]+)>$/)
    const displayName = matched?.[1]?.trim() || ''
    const email = (matched?.[2] || chunk).trim().toLowerCase()
    if (!email.includes('@')) {
      continue
    }

    const existing = deduplicated.get(email)
    deduplicated.set(email, {
      email,
      displayName: displayName || existing?.displayName
    })
  }

  return Array.from(deduplicated.values())
}

export function filterCalendarIncomingShares(
  shares: CalendarIncomingShare[],
  filter: CalendarIncomingShareFilter
): CalendarIncomingShare[] {
  if (filter === 'ALL') {
    return shares
  }
  return shares.filter(item => item.responseStatus === filter)
}

export function buildCalendarIncomingShareCounts(shares: CalendarIncomingShare[]): Record<CalendarIncomingShareFilter, number> {
  return {
    ALL: shares.length,
    NEEDS_ACTION: shares.filter(item => item.responseStatus === 'NEEDS_ACTION').length,
    ACCEPTED: shares.filter(item => item.responseStatus === 'ACCEPTED').length,
    DECLINED: shares.filter(item => item.responseStatus === 'DECLINED').length
  }
}

export function formatCalendarDateTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}

export function hasCalendarAvailabilityQuery(startAt: string, endAt: string, attendeeCount: number): boolean {
  return Boolean(startAt && endAt && attendeeCount > 0)
}

export function buildCalendarAvailabilitySummary(availability: CalendarAvailability | null) {
  if (!availability) {
    return {
      attendeeCount: 0,
      busyCount: 0,
      freeCount: 0,
      unknownCount: 0,
      hasConflicts: false
    }
  }
  return availability.summary
}
