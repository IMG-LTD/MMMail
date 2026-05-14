interface CalendarLayoutEvent {
  allDay?: boolean
  endAt: string
  id: string
  shared?: boolean
  startAt: string
  title?: string
}

interface CalendarLayoutDay {
  key: string
}

interface CalendarLayoutOptions {
  days: CalendarLayoutDay[]
  firstSlotHour: number
  selectedEventId: string
}

interface CalendarSegment {
  endAt: Date
  key: string
  startAt: Date
}

interface SegmentBounds {
  day: CalendarLayoutDay
  endAt: Date
  startAt: Date
}

const FIRST_DAY_COLUMN = 2
const FIRST_EVENT_ROW = 2
const HOURS_PER_DAY = 24
const MAX_HOUR = 23
const MIN_HOUR = 0
const NEXT_DAY_OFFSET = 1
const NO_DAY_OFFSET = 0

export function resolveCalendarTimeSlotHours(items: Array<{ endAt: string; startAt: string }>, fallbackHour: number) {
  const hours = items.flatMap(resolveBoundaryHours)
  if (!hours.length) {
    const startHour = Math.max(MIN_HOUR, fallbackHour - 1)
    return createHourRange(startHour, Math.min(MAX_HOUR, startHour + 5))
  }
  return createHourRange(Math.max(MIN_HOUR, Math.min(...hours) - 1), Math.min(MAX_HOUR, Math.max(...hours) + 1))
}

export function resolvePositionedCalendarEvents(
  items: CalendarLayoutEvent[],
  options: CalendarLayoutOptions
) {
  return items.flatMap(item => createVisibleSegments(item, options.days)
    .map(segment => toPositionedEvent(item, segment, options)))
}

function resolveBoundaryHours(item: { endAt: string; startAt: string }) {
  const startAt = parseDate(item.startAt)
  const endAt = parseDate(item.endAt)
  if (!startAt || !endAt) return []
  if (formatDateKey(startAt) !== formatDateKey(endAt)) return [MIN_HOUR, MAX_HOUR]
  return [hourFraction(startAt), Math.ceil(hourFraction(endAt))]
}

function createVisibleSegments(item: CalendarLayoutEvent, days: CalendarLayoutDay[]): CalendarSegment[] {
  const startAt = parseDate(item.startAt)
  const endAt = parseDate(item.endAt)
  if (!startAt || !endAt || endAt <= startAt) return []
  return days.map(day => createSegment(item, { day, endAt, startAt })).filter(isCalendarSegment)
}

function isCalendarSegment(value: CalendarSegment | null): value is CalendarSegment {
  return value !== null
}

function createSegment(item: CalendarLayoutEvent, bounds: SegmentBounds): CalendarSegment | null {
  const { day, endAt, startAt } = bounds
  const dayStart = parseDate(`${day.key}T00:00:00`)
  if (!dayStart) return null
  const dayEnd = addDays(dayStart, NEXT_DAY_OFFSET)
  const segmentStart = new Date(Math.max(startAt.getTime(), dayStart.getTime()))
  const segmentEnd = new Date(Math.min(endAt.getTime(), dayEnd.getTime()))
  if (segmentEnd <= segmentStart) return null
  return { endAt: segmentEnd, key: `${item.id}-${day.key}`, startAt: segmentStart }
}

function toPositionedEvent(item: CalendarLayoutEvent, segment: CalendarSegment, options: CalendarLayoutOptions) {
  const { days, firstSlotHour, selectedEventId } = options
  const dayIndex = days.findIndex(day => day.key === formatDateKey(segment.startAt))
  const rowStart = Math.max(FIRST_EVENT_ROW, Math.floor(hourFraction(segment.startAt) - firstSlotHour) + FIRST_EVENT_ROW)
  const rowSpan = Math.max(1, Math.ceil(resolveSegmentEndHour(segment) - hourFraction(segment.startAt)))
  return {
    id: item.id,
    key: segment.key,
    meta: formatDateWindow(item.startAt, item.endAt, item.allDay),
    selected: item.id === selectedEventId,
    shared: Boolean(item.shared),
    style: { gridColumn: String(dayIndex + FIRST_DAY_COLUMN), gridRow: `${rowStart} / span ${rowSpan}` },
    title: item.title || 'Untitled event'
  }
}

function resolveSegmentEndHour(segment: { endAt: Date; startAt: Date }) {
  return formatDateKey(segment.endAt) === formatDateKey(segment.startAt) ? hourFraction(segment.endAt) : HOURS_PER_DAY
}

function createHourRange(startHour: number, endHour: number) {
  return Array.from({ length: Math.max(1, endHour - startHour + 1) }, (_, index) => startHour + index)
}

function parseDate(value?: string | null) {
  if (!value) return null
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function addDays(value: Date, days: number) {
  return createLocalDate(value, days)
}

function createLocalDate(value: Date, dayOffset = NO_DAY_OFFSET) {
  return new Date(
    value.getFullYear(),
    value.getMonth(),
    value.getDate() + dayOffset,
    value.getHours(),
    value.getMinutes(),
    value.getSeconds(),
    value.getMilliseconds()
  )
}

function formatDateKey(value: Date) {
  const month = `${value.getMonth() + 1}`.padStart(2, '0')
  const day = `${value.getDate()}`.padStart(2, '0')
  return `${value.getFullYear()}-${month}-${day}`
}

function hourFraction(value: Date) {
  return value.getHours() + value.getMinutes() / 60
}

function formatDateWindow(startAt: string, endAt: string, allDay = false) {
  const start = parseDate(startAt)
  const end = parseDate(endAt)
  if (!start || !end) return 'Time unavailable'
  if (allDay) return `${start.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })} · All day`
  return `${start.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })} · ${start.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })} - ${end.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}`
}
