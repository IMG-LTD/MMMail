import type { CalendarEvent } from '~/types/api'

export type CalendarRangeView = 'day' | 'week' | 'month'

export interface CalendarRangeWindow {
  from: string
  to: string
  label: string
}

export interface CalendarEventGroup {
  key: string
  label: string
  items: CalendarEvent[]
}

const DAY_MS = 24 * 60 * 60 * 1000
const DEFAULT_TIMEZONE = 'UTC'

export function createCalendarAnchor(date = new Date()): string {
  return toDateKey(date)
}

export function buildCalendarRangeWindow(anchor: string, view: CalendarRangeView): CalendarRangeWindow {
  const base = parseDateKey(anchor)
  if (view === 'day') {
    return buildWindow(base, addDays(base, 1), base)
  }
  if (view === 'week') {
    const start = startOfWeek(base)
    return buildWindow(start, addDays(start, 7), start)
  }
  const start = startOfMonth(base)
  return buildWindow(start, startOfNextMonth(start), start)
}

export function shiftCalendarAnchor(anchor: string, view: CalendarRangeView, step: number): string {
  const base = parseDateKey(anchor)
  if (view === 'day') {
    return toDateKey(addDays(base, step))
  }
  if (view === 'week') {
    return toDateKey(addDays(base, step * 7))
  }
  return toDateKey(addMonths(base, step))
}

export function normalizeAllDayRange(startAt: string, endAt: string): { startAt: string; endAt: string } {
  const start = parseDateTime(startAt)
  const end = parseDateTime(endAt)
  const normalizedStart = new Date(start.getFullYear(), start.getMonth(), start.getDate())
  const normalizedEnd = new Date(end.getFullYear(), end.getMonth(), end.getDate())
  const safeEnd = normalizedEnd.getTime() <= normalizedStart.getTime()
    ? addDays(normalizedStart, 1)
    : normalizedEnd
  return {
    startAt: toDateTimeValue(normalizedStart),
    endAt: toDateTimeValue(safeEnd),
  }
}

export function formatCalendarEventTimeRange(
  event: Pick<CalendarEvent, 'startAt' | 'endAt' | 'allDay' | 'timezone'>,
): string {
  const timezone = event.timezone || DEFAULT_TIMEZONE
  if (event.allDay) {
    return `${event.startAt.slice(0, 10)} · ${timezone} · all day`
  }
  const start = formatStoredDateTime(event.startAt)
  const end = formatStoredDateTime(event.endAt)
  return `${start} → ${end} · ${timezone}`
}

export function formatStoredDateTime(value: string | null): string {
  if (!value) {
    return '—'
  }
  return value.replace('T', ' ').slice(0, 16)
}

export function buildCalendarEventGroups(
  events: CalendarEvent[],
  view: CalendarRangeView,
): CalendarEventGroup[] {
  const map = new Map<string, CalendarEvent[]>()
  for (const event of events) {
    const key = event.startAt.slice(0, 10)
    map.set(key, [...(map.get(key) || []), event])
  }

  return Array.from(map.entries())
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, items]) => ({
      key,
      label: buildGroupLabel(key, view),
      items: items.slice().sort((left, right) => left.startAt.localeCompare(right.startAt)),
    }))
}

function buildGroupLabel(dateKey: string, view: CalendarRangeView): string {
  if (view === 'month') {
    return dateKey
  }
  const date = parseDateKey(dateKey)
  return `${dateKey} · ${['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][date.getDay()]}`
}

function buildWindow(start: Date, end: Date, labelDate: Date): CalendarRangeWindow {
  return {
    from: toDateTimeValue(start),
    to: toDateTimeValue(end),
    label: formatRangeLabel(start, end, labelDate),
  }
}

function formatRangeLabel(start: Date, end: Date, labelDate: Date): string {
  if (start.getTime() + DAY_MS === end.getTime()) {
    return toDateKey(labelDate)
  }
  return `${toDateKey(start)} → ${toDateKey(addDays(end, -1))}`
}

function startOfWeek(date: Date): Date {
  return addDays(new Date(date.getFullYear(), date.getMonth(), date.getDate()), -date.getDay())
}

function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function startOfNextMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth() + 1, 1)
}

function addDays(date: Date, amount: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + amount)
}

function addMonths(date: Date, amount: number): Date {
  return new Date(date.getFullYear(), date.getMonth() + amount, 1)
}

function parseDateKey(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function parseDateTime(value: string): Date {
  const [datePart, timePart] = value.split('T')
  const [year, month, day] = datePart.split('-').map(Number)
  const [hour, minute, second] = timePart.split(':').map(Number)
  return new Date(year, month - 1, day, hour, minute, second || 0)
}

function toDateKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function toDateTimeValue(date: Date): string {
  return `${toDateKey(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function pad(value: number): string {
  return `${value}`.padStart(2, '0')
}
