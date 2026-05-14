<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { lt, useLocaleText } from '@/locales'
import {
  createCalendarEvent,
  listCalendarAgenda,
  listCalendarEvents,
  queryCalendarAvailability,
  updateCalendarEvent,
  type CalendarAgendaItem,
  type CalendarAvailability,
  type CalendarEvent,
  type CalendarEventMutationPayload
} from '@/service/api/calendar'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'
import { useAuthStore } from '@/store/modules/auth'
import CalendarBoard from './calendar/CalendarBoard.vue'
import CalendarEventDrawer from './calendar/CalendarEventDrawer.vue'
import CalendarFilterSidebar from './calendar/CalendarFilterSidebar.vue'
import { resolveCalendarTimeSlotHours, resolvePositionedCalendarEvents } from './calendar/calendar-layout'
import {
  addDays,
  formatDateKey,
  formatDateWindow,
  formatLocalDateTime,
  mergeCalendarSurfaceItems,
  parseDate,
  startOfDay,
  startOfWeek,
  workWeekLength
} from './calendar/calendar-view-helpers'
import type { CalendarDayCell, CalendarEventDraft, CalendarSurfaceItem, CalendarTimeSlot, CalendarViewMode, PositionedCalendarEvent } from './calendar/calendar-types'
import './calendar-view.css'

const { tr } = useLocaleText()
const authStore = useAuthStore()
const copilotPanel = useCopilotPanel()
const calendarLoading = ref(false)
const loadError = ref('')
const activeViewMode = ref<CalendarViewMode>('week')
const focusedDay = ref(startOfDay(new Date()))
const calendarEvents = ref<CalendarEvent[]>([])
const agendaItems = ref<CalendarAgendaItem[]>([])
const availability = ref<CalendarAvailability | null>(null)
const selectedEventId = ref('')
const eventDrawerOpen = ref(false)
const calendarSaveError = ref('')
let latestCalendarRequest = 0

const viewModes = [
  { key: 'month', label: 'Month' },
  { key: 'week', label: 'Week' },
  { key: 'day', label: 'Day' },
  { key: 'agenda', label: 'Agenda' }
] satisfies Array<{ key: CalendarViewMode; label: string }>
const surfaceItems = computed(() => mergeCalendarSurfaceItems(calendarEvents.value, agendaItems.value))
const scheduleDays = computed<CalendarDayCell[]>(() => createScheduleDays())
const miniCalendarDays = computed<CalendarDayCell[]>(() => createMiniCalendarDays())
const toolbarMonthLabel = computed(() => (scheduleDays.value[0]?.date || focusedDay.value).toLocaleDateString(undefined, { month: 'long', year: 'numeric' }))
const toolbarRangeLabel = computed(resolveToolbarRangeLabel)
const calendarSummaries = computed(resolveCalendarSummaries)
const timeSlots = computed<CalendarTimeSlot[]>(resolveTimeSlots)
const firstSlotHour = computed(() => timeSlots.value[0]?.hour ?? 0)
const positionedEvents = computed<PositionedCalendarEvent[]>(resolvePositionedEvents)
const agendaPreviewItems = computed(resolveAgendaPreviewItems)
const selectedItem = computed<CalendarSurfaceItem | null>(() => surfaceItems.value.find(item => item.id === selectedEventId.value) || surfaceItems.value[0] || null)
const boardSubtitle = computed(() => authStore.accessToken ? `Events ${calendarEvents.value.length} · Agenda ${agendaItems.value.length}` : 'Sign in to switch this calendar surface to runtime data.')
const emptyState = computed(() => authStore.accessToken ? 'No events in the current time window.' : 'Sign in to load your calendar workspace.')
const visibleRange = computed(() => {
  const rangeStart = activeViewMode.value === 'day' ? startOfDay(focusedDay.value) : startOfWeek(focusedDay.value)
  return { from: formatLocalDateTime(rangeStart), to: formatLocalDateTime(addDays(rangeStart, activeViewMode.value === 'day' ? 1 : 5)) }
})

onMounted(() => {
  void copilotPanel.loadCapabilities().catch(() => {})
  void loadCalendar()
})

watch(() => authStore.accessToken, (token, previousToken) => {
  if (token !== previousToken) void loadCalendar()
})

async function loadCalendar() {
  const request = createCalendarRequest()
  if (!request.token) return resetSignedOutCalendar(request)
  calendarLoading.value = true
  loadError.value = ''
  const requestToken = request.token
  const requestRange = request.range
  try {
    const [eventsResponse, agendaResponse] = await Promise.all([
      listCalendarEvents(requestToken, requestRange.from, requestRange.to),
      listCalendarAgenda(requestToken)
    ])
    await applyCalendarResponses(request, eventsResponse.data || [], agendaResponse.data || [])
  } catch (error) {
    if (isCurrentCalendarRequest(request)) resetCalendarAfterError(resolveErrorMessage(error))
  } finally {
    if (isCurrentCalendarRequest(request)) calendarLoading.value = false
  }
}

function createCalendarRequest() {
  latestCalendarRequest += 1
  return { id: latestCalendarRequest, range: visibleRange.value, token: authStore.accessToken }
}

function isCurrentCalendarRequest(request: { id: number; range: { from: string; to: string }; token: string }) {
  const rangeKey = `${request.range.from}|${request.range.to}`
  return request.id === latestCalendarRequest && request.token === authStore.accessToken && rangeKey === `${visibleRange.value.from}|${visibleRange.value.to}`
}

function resetSignedOutCalendar(request: { id: number; range: { from: string; to: string }; token: string }) {
  if (!isCurrentCalendarRequest(request)) return
  calendarEvents.value = []
  agendaItems.value = []
  availability.value = null
  selectedEventId.value = ''
  loadError.value = ''
  calendarLoading.value = false
}

async function applyCalendarResponses(request: { id: number; range: { from: string; to: string }; token: string }, events: CalendarEvent[], agenda: CalendarAgendaItem[]) {
  if (!isCurrentCalendarRequest(request)) return
  const attendeeEmails = resolveAttendeeEmails(events, agenda)
  calendarEvents.value = events
  agendaItems.value = agenda
  availability.value = await readAvailability(request, attendeeEmails)
  loadError.value = ''
  selectedEventId.value = surfaceItems.value.find(item => item.id === selectedEventId.value)?.id || surfaceItems.value[0]?.id || ''
}

async function readAvailability(request: { id: number; range: { from: string; to: string }; token: string }, attendeeEmails: string[]) {
  try {
    const response = await queryCalendarAvailability(request.token, { attendeeEmails, endAt: request.range.to, startAt: request.range.from })
    return isCurrentCalendarRequest(request) ? response.data : null
  } catch (error) {
    if (isCurrentCalendarRequest(request)) loadError.value = resolveErrorMessage(error)
    return null
  }
}

function resetCalendarAfterError(message: string) {
  calendarEvents.value = []
  agendaItems.value = []
  availability.value = null
  selectedEventId.value = ''
  loadError.value = message
}

function setViewMode(mode: CalendarViewMode) {
  activeViewMode.value = mode
  void loadCalendar()
}

function selectDay(date: Date) {
  focusedDay.value = startOfDay(date)
  void loadCalendar()
}

function focusTodayAndReload() {
  focusedDay.value = startOfDay(new Date())
  void loadCalendar()
}

function openEventDrawer(itemId = '') {
  selectedEventId.value = itemId || selectedItem.value?.id || surfaceItems.value[0]?.id || ''
  calendarSaveError.value = ''
  eventDrawerOpen.value = true
}

async function saveEventDraft(draft: CalendarEventDraft) {
  const token = authStore.accessToken
  if (!token) {
    calendarSaveError.value = 'Sign in to save calendar events.'
    return
  }
  try {
    calendarSaveError.value = ''
    const payload = buildCalendarEventPayload(draft)
    const response = selectedItem.value?.id
      ? await updateCalendarEvent(token, selectedItem.value.id, payload)
      : await createCalendarEvent(token, payload)
    selectedEventId.value = response.data.id
    eventDrawerOpen.value = false
    await loadCalendar()
  } catch (error) {
    calendarSaveError.value = resolveErrorMessage(error)
  }
}

function buildCalendarEventPayload(draft: CalendarEventDraft): CalendarEventMutationPayload {
  return {
    allDay: draft.allDay,
    attendees: [],
    description: draft.description,
    endAt: normalizeDraftDateTime(draft.endAt),
    location: draft.location,
    reminderMinutes: draft.reminderMinutes,
    startAt: normalizeDraftDateTime(draft.startAt),
    timezone: draft.timezone,
    title: draft.title
  }
}

function normalizeDraftDateTime(value: string) {
  return value.length === 16 ? `${value}:00` : value
}

function createScheduleDays() {
  const baseDate = activeViewMode.value === 'day' ? startOfDay(focusedDay.value) : startOfWeek(focusedDay.value)
  return Array.from({ length: workWeekLength(activeViewMode.value) }, (_, index) => createDayCell(addDays(baseDate, index)))
}

function createMiniCalendarDays() {
  const startDate = addDays(startOfWeek(focusedDay.value), -5)
  return Array.from({ length: 20 }, (_, index) => createDayCell(addDays(startDate, index)))
}

function createDayCell(date: Date): CalendarDayCell {
  const key = formatDateKey(date)
  return {
    active: key === formatDateKey(focusedDay.value),
    date,
    dayNumber: date.toLocaleDateString(undefined, { day: 'numeric' }),
    hasEvents: surfaceItems.value.some(item => formatDateKey(parseDate(item.startAt) || date) === key),
    key,
    label: date.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' })
  }
}

function resolveToolbarRangeLabel() {
  const first = scheduleDays.value[0]?.date || focusedDay.value
  const last = scheduleDays.value[scheduleDays.value.length - 1]?.date || focusedDay.value
  if (activeViewMode.value === 'day') return first.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })
  return `${first.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} - ${last.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}`
}

function resolveCalendarSummaries() {
  const currentUserEmail = authStore.user?.email || ''
  const ownedCount = surfaceItems.value.filter(item => !item.ownerEmail || item.ownerEmail === currentUserEmail).length
  const sharedCount = surfaceItems.value.filter(item => item.shared).length
  return [
    { active: ownedCount > 0, count: ownedCount, key: 'owned', name: tr(lt('我的事件', '我的事件', 'My events')) },
    { active: sharedCount > 0, count: sharedCount, key: 'shared', name: tr(lt('共享事件', '共享事件', 'Shared events')) },
    { active: agendaItems.value.length > 0, count: agendaItems.value.length, key: 'agenda', name: tr(lt('近期议程', '近期議程', 'Upcoming agenda')) }
  ]
}

function resolveTimeSlots() {
  const hours = resolveCalendarTimeSlotHours(surfaceItems.value, new Date().getHours())
  return hours.map(createTimeSlot)
}

function createTimeSlot(hour: number) {
  const date = startOfDay(focusedDay.value)
  date.setHours(hour, 0, 0, 0)
  return { hour, key: `slot-${hour}`, label: date.toLocaleTimeString(undefined, { hour: 'numeric' }) }
}

function resolvePositionedEvents() {
  return resolvePositionedCalendarEvents(calendarEvents.value, {
    days: scheduleDays.value,
    firstSlotHour: firstSlotHour.value,
    selectedEventId: selectedEventId.value
  })
}

function resolveAgendaPreviewItems() {
  return agendaItems.value.slice().sort((left, right) => String(left.startAt).localeCompare(String(right.startAt))).slice(0, 6).map(item => ({
    id: item.id,
    meta: formatDateWindow(item.startAt, item.endAt, false),
    title: item.title || 'Untitled event'
  }))
}

function resolveAttendeeEmails(events: CalendarEvent[], agenda: CalendarAgendaItem[]) {
  return Array.from(new Set([authStore.user?.email || '', ...events.map(item => item.ownerEmail || ''), ...agenda.map(item => item.ownerEmail || '')].filter(isEmailLike)))
}

function isEmailLike(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error && error.message ? error.message : 'Failed to load calendar data. Please try again later.'
}
</script>

<template>
  <section class="calendar-page calendar-workbench">
    <div class="calendar-page__layout">
      <CalendarFilterSidebar
        :availability="availability"
        :days="miniCalendarDays"
        :loading="calendarLoading"
        :month-label="toolbarMonthLabel"
        :summaries="calendarSummaries"
        @refresh="loadCalendar"
        @select-day="selectDay"
        @today="focusTodayAndReload"
      />
      <div class="calendar-page__main">
        <p class="calendar-page__status">{{ loadError || boardSubtitle }}</p>
        <CalendarBoard
          :active-view-mode="activeViewMode"
          :agenda-items="agendaPreviewItems"
          :empty-copy="emptyState"
          :loading="calendarLoading"
          :month-label="toolbarMonthLabel"
          :positioned-events="positionedEvents"
          :range-label="toolbarRangeLabel"
          :schedule-days="scheduleDays"
          :time-slots="timeSlots"
          :view-modes="viewModes"
          @open-event="openEventDrawer"
          @set-mode="setViewMode"
          @today="focusTodayAndReload"
        />
      </div>
      <CalendarEventDrawer
        :availability="availability"
        :loading="calendarLoading"
        :open="eventDrawerOpen"
        :save-error="calendarSaveError"
        :selected-item="selectedItem"
        @close="eventDrawerOpen = false"
        @retry="saveEventDraft"
        @save="saveEventDraft"
      />
    </div>
  </section>
</template>
