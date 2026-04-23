<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { lt, useLocaleText } from '@/locales'
import {
  listCalendarAgenda,
  listCalendarEvents,
  queryCalendarAvailability,
  type CalendarAgendaItem,
  type CalendarAvailability,
  type CalendarEvent
} from '@/service/api/calendar'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'
import { useAuthStore } from '@/store/modules/auth'

type CalendarViewMode = 'month' | 'week' | 'day' | 'agenda'

interface CalendarSurfaceItem {
  id: string
  title: string
  location: string | null
  startAt: string
  endAt: string
  attendeeCount: number
  shared: boolean
  ownerEmail: string | null
  allDay: boolean
}

interface CalendarDayCell {
  key: string
  date: Date
  dayNumber: string
  label: string
  active: boolean
  hasEvents: boolean
}

interface CalendarTimeSlot {
  key: string
  label: string
  hour: number
}

interface PositionedCalendarEvent {
  id: string
  title: string
  meta: string
  shared: boolean
  selected: boolean
  style: {
    gridColumn: string
    gridRow: string
  }
}

const { tr } = useLocaleText()
const authStore = useAuthStore()
const copilotPanel = useCopilotPanel()
const copilotOpen = copilotPanel.open

const calendarLoading = ref(false)
const loadError = ref('')
const activeViewMode = ref<CalendarViewMode>('week')
const focusedDay = ref(startOfDay(new Date()))
const calendarEvents = ref<CalendarEvent[]>([])
const agendaItems = ref<CalendarAgendaItem[]>([])
const availability = ref<CalendarAvailability | null>(null)
const selectedEventId = ref('')
const hasLoadedCalendar = ref(false)
let latestCalendarRequest = 0

const viewModes = [
  { key: 'month', label: lt('月', '月', 'Month') },
  { key: 'week', label: lt('周', '週', 'Week') },
  { key: 'day', label: lt('日', '日', 'Day') },
  { key: 'agenda', label: lt('议程', '議程', 'Agenda') }
] satisfies Array<{ key: CalendarViewMode; label: ReturnType<typeof lt> }>

const surfaceItems = computed<CalendarSurfaceItem[]>(() => {
  return mergeCalendarSurfaceItems(calendarEvents.value, agendaItems.value)
})

const scheduleDays = computed<CalendarDayCell[]>(() => {
  const baseDate = activeViewMode.value === 'day' ? startOfDay(focusedDay.value) : startOfWeek(focusedDay.value)
  const total = activeViewMode.value === 'day' ? 1 : 5

  return Array.from({ length: total }, (_, index) => {
    const date = addDays(baseDate, index)
    const key = formatDateKey(date)
    const hasEvents = surfaceItems.value.some((item) => formatDateKey(parseDate(item.startAt) || date) === key)

    return {
      active: isSameDay(date, focusedDay.value),
      date,
      dayNumber: date.toLocaleDateString(undefined, { day: 'numeric' }),
      hasEvents,
      key,
      label: date.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' })
    }
  })
})

const scheduleDayKeySet = computed(() => {
  return new Set(scheduleDays.value.map(item => item.key))
})

const miniCalendarDays = computed<CalendarDayCell[]>(() => {
  const startDate = addDays(startOfWeek(focusedDay.value), -5)

  return Array.from({ length: 20 }, (_, index) => {
    const date = addDays(startDate, index)
    const key = formatDateKey(date)
    const hasEvents = surfaceItems.value.some((item) => formatDateKey(parseDate(item.startAt) || date) === key)

    return {
      active: scheduleDayKeySet.value.has(key),
      date,
      dayNumber: date.toLocaleDateString(undefined, { day: 'numeric' }),
      hasEvents,
      key,
      label: date.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric' })
    }
  })
})

const toolbarMonthLabel = computed(() => {
  const referenceDate = scheduleDays.value[0]?.date || focusedDay.value
  return referenceDate.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })
})

const toolbarRangeLabel = computed(() => {
  const firstDay = scheduleDays.value[0]?.date || focusedDay.value
  const lastDay = scheduleDays.value[scheduleDays.value.length - 1]?.date || focusedDay.value

  if (activeViewMode.value === 'day') {
    return firstDay.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })
  }

  return `${firstDay.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} – ${lastDay.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}`
})

const calendarSummaries = computed(() => {
  const currentUserEmail = authStore.user?.email || ''
  const ownedCount = surfaceItems.value.filter((item) => !item.ownerEmail || item.ownerEmail === currentUserEmail).length
  const sharedCount = surfaceItems.value.filter(item => item.shared).length

  return [
    {
      active: ownedCount > 0,
      count: ownedCount,
      key: 'owned',
      name: tr(lt('我的事件', '我的事件', 'My events'))
    },
    {
      active: sharedCount > 0,
      count: sharedCount,
      key: 'shared',
      name: tr(lt('共享事件', '共享事件', 'Shared events'))
    },
    {
      active: agendaItems.value.length > 0,
      count: agendaItems.value.length,
      key: 'agenda',
      name: tr(lt('近期议程', '近期議程', 'Upcoming agenda'))
    }
  ]
})

const timeSlots = computed<CalendarTimeSlot[]>(() => {
  const boundaries = surfaceItems.value
    .flatMap((item) => [parseDate(item.startAt), parseDate(item.endAt)])
    .filter((value): value is Date => Boolean(value))

  if (!boundaries.length) {
    const initialHour = Math.max(0, new Date().getHours() - 1)
    const lastHour = Math.min(23, initialHour + 5)

    return Array.from({ length: lastHour - initialHour + 1 }, (_, index) => {
      const date = startOfDay(focusedDay.value)
      date.setHours(initialHour + index, 0, 0, 0)
      return {
        hour: date.getHours(),
        key: `slot-${date.getHours()}`,
        label: date.toLocaleTimeString(undefined, { hour: 'numeric' })
      }
    })
  }

  const startHour = Math.max(0, Math.min(...boundaries.map(date => date.getHours())) - 1)
  const endHour = Math.min(23, Math.max(...boundaries.map(date => date.getHours() + (date.getMinutes() > 0 ? 1 : 0))) + 1)

  return Array.from({ length: Math.max(1, endHour - startHour + 1) }, (_, index) => {
    const date = startOfDay(focusedDay.value)
    date.setHours(startHour + index, 0, 0, 0)
    return {
      hour: date.getHours(),
      key: `slot-${date.getHours()}`,
      label: date.toLocaleTimeString(undefined, { hour: 'numeric' })
    }
  })
})

const firstSlotHour = computed(() => {
  return timeSlots.value[0]?.hour ?? 0
})

const positionedEvents = computed<PositionedCalendarEvent[]>(() => {
  return calendarEvents.value
    .map((item) => {
      const startAt = parseDate(item.startAt)
      const endAt = parseDate(item.endAt)

      if (!startAt || !endAt) {
        return null
      }

      const dayIndex = scheduleDays.value.findIndex(day => day.key === formatDateKey(startAt))
      if (dayIndex === -1) {
        return null
      }

      const startHour = startAt.getHours() + startAt.getMinutes() / 60
      const endHour = endAt.getHours() + endAt.getMinutes() / 60
      const rowStart = Math.max(2, Math.floor(startHour - firstSlotHour.value) + 2)
      const rowSpan = Math.max(1, Math.ceil(Math.max(0.5, endHour - startHour)))

      return {
        id: item.id,
        meta: formatDateWindow(item.startAt, item.endAt, item.allDay),
        selected: item.id === selectedEventId.value,
        shared: item.shared,
        style: {
          gridColumn: String(dayIndex + 2),
          gridRow: `${rowStart} / span ${rowSpan}`
        },
        title: item.title || tr(lt('未命名事件', '未命名事件', 'Untitled event'))
      }
    })
    .filter((item): item is PositionedCalendarEvent => Boolean(item))
})

const agendaPreviewItems = computed(() => {
  return agendaItems.value
    .slice()
    .sort((left, right) => String(left.startAt).localeCompare(String(right.startAt)))
    .slice(0, 6)
    .map((item) => ({
      id: item.id,
      meta: formatDateWindow(item.startAt, item.endAt, false),
      title: item.title || tr(lt('未命名事件', '未命名事件', 'Untitled event'))
    }))
})

const selectedItem = computed<CalendarSurfaceItem | null>(() => {
  return surfaceItems.value.find(item => item.id === selectedEventId.value) || surfaceItems.value[0] || null
})

const selectedItemTitle = computed(() => {
  return selectedItem.value?.title || tr(lt('暂无日程', '暫無行程', 'No calendar event selected'))
})

const selectedItemMeta = computed(() => {
  if (!selectedItem.value) {
    return tr(lt('登录后即可读取已认证日历数据。', '登入後即可讀取已驗證日曆資料。', 'Sign in to load authenticated calendar data.'))
  }

  return formatDateWindow(selectedItem.value.startAt, selectedItem.value.endAt, selectedItem.value.allDay)
})

const selectedItemGuests = computed(() => {
  if (!selectedItem.value) {
    return tr(lt('暂无参与人数据', '暫無參與者資料', 'No guest data yet'))
  }

  return `${selectedItem.value.attendeeCount} ${tr(lt('位参与人', '位參與者', 'guests'))}`
})

const selectedItemLocation = computed(() => {
  return selectedItem.value?.location || tr(lt('未设置地点', '未設定地點', 'No location set'))
})

const availabilitySummary = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可查询空闲状态。', '登入後即可查詢空閒狀態。', 'Sign in to query availability.'))
  }

  if (calendarLoading.value) {
    return tr(lt('正在查询忙闲数据。', '正在查詢忙閒資料。', 'Querying availability data.'))
  }

  if (!availability.value) {
    return tr(lt('暂无可用性结果。', '暫無可用性結果。', 'No availability result yet.'))
  }

  const summary = availability.value.summary
  return `${tr(lt('参与人', '參與者', 'Attendees'))} ${summary.attendeeCount} · ${tr(lt('冲突', '衝突', 'Busy'))} ${summary.busyCount} · ${tr(lt('空闲', '空閒', 'Free'))} ${summary.freeCount}`
})

const boardSubtitle = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可将主日历切换到实时数据。', '登入後即可將主日曆切換到即時資料。', 'Sign in to switch this calendar surface to runtime data.'))
  }

  return `${tr(lt('事件', '事件', 'Events'))} ${calendarEvents.value.length} · ${tr(lt('议程', '議程', 'Agenda'))} ${agendaItems.value.length}`
})

const emptyState = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可加载你的日历工作区。', '登入後即可載入你的日曆工作區。', 'Sign in to load your calendar workspace.'))
  }

  return tr(lt('当前时间窗口暂无事件。', '目前時間視窗暫無事件。', 'No events in the current time window.'))
})

const visibleRange = computed(() => {
  const rangeStart = activeViewMode.value === 'day' ? startOfDay(focusedDay.value) : startOfWeek(focusedDay.value)
  const rangeEnd = addDays(rangeStart, activeViewMode.value === 'day' ? 1 : 5)

  return {
    from: formatLocalDateTime(rangeStart),
    to: formatLocalDateTime(rangeEnd)
  }
})

onMounted(() => {
  void copilotPanel.loadCapabilities().catch(() => {})
  void loadCalendar()
})

watch(() => authStore.accessToken, (token, previousToken) => {
  if (token === previousToken) {
    return
  }

  void loadCalendar()
})

async function loadCalendar() {
  const requestId = ++latestCalendarRequest
  const requestToken = authStore.accessToken
  const requestRange = visibleRange.value
  const requestRangeKey = `${requestRange.from}|${requestRange.to}`

  if (!requestToken) {
    if (requestId !== latestCalendarRequest || requestToken !== authStore.accessToken || requestRangeKey !== `${visibleRange.value.from}|${visibleRange.value.to}`) {
      return
    }

    calendarEvents.value = []
    agendaItems.value = []
    availability.value = null
    selectedEventId.value = ''
    loadError.value = ''
    hasLoadedCalendar.value = false
    calendarLoading.value = false
    return
  }

  calendarLoading.value = true
  loadError.value = ''

  try {
    const [eventsResponse, agendaResponse] = await Promise.all([
      listCalendarEvents(requestToken, requestRange.from, requestRange.to),
      listCalendarAgenda(requestToken)
    ])

    if (requestId !== latestCalendarRequest || requestToken !== authStore.accessToken || requestRangeKey !== `${visibleRange.value.from}|${visibleRange.value.to}`) {
      return
    }

    const nextCalendarEvents = Array.isArray(eventsResponse.data) ? eventsResponse.data : []
    const nextAgendaItems = Array.isArray(agendaResponse.data) ? agendaResponse.data : []

    const attendeeEmails = Array.from(new Set([
      authStore.user?.email || '',
      ...nextCalendarEvents.map(item => item.ownerEmail || ''),
      ...nextAgendaItems.map(item => item.ownerEmail || '')
    ].filter(isEmailLike)))

    let nextAvailability: CalendarAvailability | null = null
    let nextLoadError = ''

    try {
      nextAvailability = (await queryCalendarAvailability(requestToken, {
        attendeeEmails,
        endAt: requestRange.to,
        startAt: requestRange.from
      })).data
    } catch (error) {
      nextAvailability = null
      nextLoadError = resolveErrorMessage(error)
    }

    if (requestId !== latestCalendarRequest || requestToken !== authStore.accessToken || requestRangeKey !== `${visibleRange.value.from}|${visibleRange.value.to}`) {
      return
    }

    calendarEvents.value = nextCalendarEvents
    agendaItems.value = nextAgendaItems
    availability.value = nextAvailability
    loadError.value = nextLoadError

    const nextSurfaceItems = mergeCalendarSurfaceItems(nextCalendarEvents, nextAgendaItems)
    if (!nextSurfaceItems.some(item => item.id === selectedEventId.value)) {
      selectedEventId.value = nextSurfaceItems[0]?.id || ''
    }

    hasLoadedCalendar.value = true
  } catch (error) {
    if (requestId !== latestCalendarRequest || requestToken !== authStore.accessToken || requestRangeKey !== `${visibleRange.value.from}|${visibleRange.value.to}`) {
      return
    }

    calendarEvents.value = []
    agendaItems.value = []
    availability.value = null
    selectedEventId.value = ''
    loadError.value = resolveErrorMessage(error)
  } finally {
    if (requestId === latestCalendarRequest) {
      calendarLoading.value = false
    }
  }
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

function selectItem(itemId: string) {
  selectedEventId.value = itemId
}

function mergeCalendarSurfaceItems(events: CalendarEvent[], agenda: CalendarAgendaItem[]) {
  const items: CalendarSurfaceItem[] = []
  const seen = new Set<string>()

  events.forEach((item) => {
    if (seen.has(item.id)) {
      return
    }

    seen.add(item.id)
    items.push({
      allDay: item.allDay,
      attendeeCount: item.attendeeCount,
      endAt: item.endAt,
      id: item.id,
      location: item.location,
      ownerEmail: item.ownerEmail,
      shared: item.shared,
      startAt: item.startAt,
      title: item.title
    })
  })

  agenda.forEach((item) => {
    if (seen.has(item.id)) {
      return
    }

    seen.add(item.id)
    items.push({
      allDay: false,
      attendeeCount: item.attendeeCount,
      endAt: item.endAt,
      id: item.id,
      location: item.location,
      ownerEmail: item.ownerEmail,
      shared: item.shared,
      startAt: item.startAt,
      title: item.title
    })
  })

  return items.sort((left, right) => {
    return String(left.startAt).localeCompare(String(right.startAt))
  })
}

function parseDate(value?: string | null) {
  if (!value) {
    return null
  }

  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function startOfDay(value: Date) {
  const next = new Date(value)
  next.setHours(0, 0, 0, 0)
  return next
}

function startOfWeek(value: Date) {
  const next = startOfDay(value)
  const weekday = next.getDay()
  const offset = weekday === 0 ? -6 : 1 - weekday
  next.setDate(next.getDate() + offset)
  return next
}

function addDays(value: Date, days: number) {
  const next = new Date(value)
  next.setDate(next.getDate() + days)
  return next
}

function isSameDay(left: Date, right: Date) {
  return formatDateKey(left) === formatDateKey(right)
}

function formatDateKey(value: Date) {
  return `${value.getFullYear()}-${`${value.getMonth() + 1}`.padStart(2, '0')}-${`${value.getDate()}`.padStart(2, '0')}`
}

function formatDateWindow(startAt: string, endAt: string, allDay = false) {
  const start = parseDate(startAt)
  const end = parseDate(endAt)

  if (!start || !end) {
    return tr(lt('时间未知', '時間未知', 'Time unavailable'))
  }

  if (allDay) {
    return `${start.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })} · ${tr(lt('全天', '全天', 'All day'))}`
  }

  return `${start.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })} · ${start.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })} – ${end.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}`
}

function formatLocalDateTime(value: Date) {
  return `${value.getFullYear()}-${`${value.getMonth() + 1}`.padStart(2, '0')}-${`${value.getDate()}`.padStart(2, '0')}T${`${value.getHours()}`.padStart(2, '0')}:${`${value.getMinutes()}`.padStart(2, '0')}:${`${value.getSeconds()}`.padStart(2, '0')}`
}

function isEmailLike(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

function resolveErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return tr(lt('读取日历数据失败，请稍后重试。', '讀取日曆資料失敗，請稍後重試。', 'Failed to load calendar data. Please try again later.'))
}
</script>

<template>
  <section class="calendar-page">
    <div class="calendar-page__layout">
      <aside class="calendar-sidebar">
        <button class="calendar-sidebar__primary" type="button" @click="loadCalendar">
          {{ calendarLoading ? tr(lt('刷新中…', '重新整理中…', 'Refreshing…')) : tr(lt('刷新日历', '重新整理日曆', 'Refresh calendar')) }}
        </button>
        <button class="calendar-sidebar__secondary" type="button" @click="focusTodayAndReload">
          {{ tr(lt('今天', '今天', 'Today')) }}
        </button>
        <button class="calendar-sidebar__secondary" type="button" @click="copilotPanel.toggle()">
          {{ copilotOpen ? tr(lt('Copilot 已打开', 'Copilot 已開啟', 'Copilot open')) : tr(lt('切换 Copilot', '切換 Copilot', 'Toggle Copilot')) }}
        </button>

        <article class="calendar-sidebar__panel">
          <header>
            <strong>{{ toolbarMonthLabel }}</strong>
            <span>{{ surfaceItems.length }}</span>
          </header>
          <div class="calendar-sidebar__grid">
            <button
              v-for="day in miniCalendarDays"
              :key="day.key"
              class="calendar-sidebar__day"
              :class="{
                'calendar-sidebar__day--active': day.active,
                'calendar-sidebar__day--has-event': day.hasEvents
              }"
              type="button"
              @click="selectDay(day.date)"
            >
              {{ day.dayNumber }}
            </button>
          </div>
        </article>

        <article class="calendar-sidebar__panel">
          <span class="section-label">{{ tr(lt('我的日历', '我的日曆', 'My Calendars')) }}</span>
          <label v-for="calendar in calendarSummaries" :key="calendar.key" class="calendar-sidebar__toggle">
            <span class="calendar-sidebar__check" :class="{ 'calendar-sidebar__check--active': calendar.active }" />
            <span>{{ calendar.name }}</span>
            <strong class="calendar-sidebar__count">{{ calendar.count }}</strong>
          </label>
        </article>

        <article class="calendar-sidebar__panel">
          <span class="section-label">{{ tr(lt('空闲状态', '空閒狀態', 'Availability')) }}</span>
          <p class="calendar-sidebar__availability">{{ availabilitySummary }}</p>
        </article>
      </aside>

      <div class="calendar-board">
        <header class="calendar-board__toolbar">
          <div>
            <span class="section-label">{{ toolbarMonthLabel }}</span>
            <h1>{{ toolbarRangeLabel }}</h1>
            <p class="page-subtitle calendar-page__status">{{ loadError || boardSubtitle }}</p>
          </div>
          <div class="calendar-board__actions">
            <button type="button" @click="focusTodayAndReload">{{ tr(lt('今天', '今天', 'Today')) }}</button>
            <div class="calendar-board__switcher">
              <button
                v-for="mode in viewModes"
                :key="mode.key"
                type="button"
                :class="{ 'calendar-board__switcher--active': mode.key === activeViewMode }"
                @click="setViewMode(mode.key)"
              >
                {{ tr(mode.label) }}
              </button>
            </div>
          </div>
        </header>

        <div v-if="activeViewMode === 'agenda'" class="calendar-board__agenda">
          <button
            v-for="item in agendaPreviewItems"
            :key="item.id"
            class="calendar-board__agenda-item"
            type="button"
            @click="selectItem(item.id)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.meta }}</span>
          </button>
          <p v-if="!agendaPreviewItems.length" class="calendar-page__empty">{{ emptyState }}</p>
        </div>

        <div v-else class="calendar-board__schedule">
          <div class="calendar-board__head">
            <span />
            <strong v-for="day in scheduleDays" :key="day.key">{{ day.label }}</strong>
          </div>

          <div v-for="slot in timeSlots" :key="slot.key" class="calendar-board__row">
            <span class="calendar-board__time">{{ slot.label }}</span>
            <span v-for="day in scheduleDays" :key="`${day.key}-${slot.key}`" class="calendar-board__cell" />
          </div>

          <button
            v-for="event in positionedEvents"
            :key="event.id"
            class="calendar-board__event"
            :class="{
              'calendar-board__event--selected': event.selected,
              'calendar-board__event--shared': event.shared
            }"
            :style="event.style"
            type="button"
            @click="selectItem(event.id)"
          >
            <strong>{{ event.title }}</strong>
            <span>{{ event.meta }}</span>
          </button>

          <p v-if="!positionedEvents.length && !calendarLoading" class="calendar-page__empty calendar-page__empty--board">
            {{ emptyState }}
          </p>
        </div>
      </div>

      <aside class="calendar-drawer">
        <div class="calendar-drawer__head">
          <span class="section-label">{{ tr(lt('议程详情', '議程詳情', 'Agenda details')) }}</span>
          <span>{{ agendaPreviewItems.length }}</span>
        </div>
        <strong>{{ selectedItemTitle }}</strong>
        <p class="page-subtitle">{{ selectedItemMeta }}</p>

        <div class="calendar-drawer__block">
          <span class="section-label">{{ tr(lt('参与人', '參與者', 'Guests')) }}</span>
          <p>{{ selectedItemGuests }}</p>
        </div>
        <div class="calendar-drawer__block">
          <span class="section-label">{{ tr(lt('地点', '地點', 'Location')) }}</span>
          <p>{{ selectedItemLocation }}</p>
        </div>
        <div class="calendar-drawer__block">
          <span class="section-label">{{ tr(lt('忙闲摘要', '忙閒摘要', 'Availability summary')) }}</span>
          <p>{{ availabilitySummary }}</p>
        </div>
        <div class="calendar-drawer__block">
          <span class="section-label">{{ tr(lt('近期议程', '近期議程', 'Upcoming agenda')) }}</span>
          <div class="calendar-drawer__list">
            <button
              v-for="item in agendaPreviewItems"
              :key="item.id"
              class="calendar-drawer__list-item"
              type="button"
              @click="selectItem(item.id)"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.meta }}</span>
            </button>
            <p v-if="!agendaPreviewItems.length" class="calendar-page__empty">{{ emptyState }}</p>
          </div>
        </div>

        <div class="calendar-drawer__footer">
          <button type="button" @click="focusTodayAndReload">{{ tr(lt('今天', '今天', 'Today')) }}</button>
          <button class="calendar-drawer__save" type="button" @click="loadCalendar">
            {{ tr(lt('刷新', '重新整理', 'Refresh')) }}
          </button>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.calendar-page {
  min-height: calc(100vh - 56px);
  background: var(--mm-card);
}

.calendar-page__layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 260px;
  min-height: calc(100vh - 56px);
}

.calendar-sidebar,
.calendar-drawer {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 18px;
  background: var(--mm-side-surface);
}

.calendar-sidebar {
  border-right: 1px solid var(--mm-border);
}

.calendar-drawer {
  border-left: 1px solid var(--mm-border);
  background: var(--mm-card);
}

.calendar-sidebar__primary,
.calendar-sidebar__secondary,
.calendar-board__toolbar button,
.calendar-drawer__footer button {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 10px;
}

.calendar-sidebar__primary,
.calendar-drawer__save {
  border: 0;
  background: linear-gradient(180deg, var(--mm-primary) 0%, var(--mm-primary-pressed) 100%);
  color: #fff;
  font-weight: 600;
}

.calendar-sidebar__secondary,
.calendar-board__toolbar button,
.calendar-drawer__footer button:not(.calendar-drawer__save) {
  border: 1px solid var(--mm-border);
  background: var(--mm-card);
}

.calendar-sidebar__panel {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius);
  background: var(--mm-card);
}

.calendar-sidebar__panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.calendar-sidebar__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.calendar-sidebar__day,
.calendar-sidebar__check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.calendar-sidebar__day {
  width: 100%;
  min-height: 28px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.calendar-sidebar__day--active {
  background: var(--mm-accent-soft);
  color: var(--mm-primary) !important;
  font-weight: 600;
}

.calendar-sidebar__day--has-event {
  box-shadow: inset 0 0 0 1px var(--mm-accent-border);
}

.calendar-sidebar__toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--mm-text);
}

.calendar-sidebar__check {
  width: 14px;
  height: 14px;
  border: 1px solid var(--mm-border-strong);
  border-radius: 4px;
  background: var(--mm-card);
}

.calendar-sidebar__check--active {
  border-color: var(--mm-primary);
  background: var(--mm-primary);
  box-shadow: inset 0 0 0 2px var(--mm-card);
}

.calendar-sidebar__count {
  margin-left: auto;
  font-size: 12px;
  color: var(--mm-text-secondary);
}

.calendar-sidebar__availability,
.calendar-page__status {
  margin: 0;
  color: var(--mm-text-secondary);
  line-height: 1.5;
}

.calendar-board {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
}

.calendar-board__toolbar {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 18px;
  padding: 20px 24px 14px;
  border-bottom: 1px solid var(--mm-border);
}

.calendar-board__toolbar h1 {
  margin: 6px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.calendar-board__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.calendar-board__switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.calendar-board__switcher button {
  min-width: 64px;
}

.calendar-board__switcher--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

.calendar-board__schedule {
  position: relative;
  display: grid;
  grid-template-columns: 72px repeat(5, minmax(0, 1fr));
  grid-auto-rows: minmax(72px, auto);
  overflow: hidden;
}

.calendar-board__agenda {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 24px;
}

.calendar-board__agenda-item,
.calendar-drawer__list-item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card);
  text-align: left;
}

.calendar-board__agenda-item strong,
.calendar-drawer__list-item strong {
  color: var(--mm-text);
}

.calendar-board__agenda-item span,
.calendar-drawer__list-item span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.calendar-board__head,
.calendar-board__row {
  display: contents;
}

.calendar-board__head strong,
.calendar-board__time,
.calendar-board__cell {
  border-right: 1px solid var(--mm-border);
  border-bottom: 1px solid var(--mm-border);
}

.calendar-board__head strong {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 52px;
  font-size: 12px;
}

.calendar-board__time {
  display: flex;
  align-items: start;
  justify-content: center;
  padding-top: 12px;
  color: var(--mm-text-secondary);
  font-size: 11px;
}

.calendar-board__cell {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.2) 0%, transparent 100%);
}

.calendar-board__event {
  z-index: 1;
  margin: 12px 10px;
  padding: 12px;
  border-radius: 14px;
  background: var(--mm-accent-soft);
  border: 1px solid var(--mm-accent-border);
  align-self: stretch;
  text-align: left;
}

.calendar-board__event--shared {
  background: rgba(114, 99, 255, 0.12);
}

.calendar-board__event--selected {
  box-shadow: 0 0 0 2px rgba(114, 99, 255, 0.2);
}

.calendar-board__event strong {
  display: block;
  color: var(--mm-primary);
}

.calendar-board__event span {
  display: block;
  margin-top: 6px;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.calendar-page__empty {
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.calendar-page__empty--board {
  grid-column: 2 / -1;
  grid-row: 3 / span 1;
  padding: 18px;
}

.calendar-drawer__head,
.calendar-drawer__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.calendar-drawer strong {
  font-size: 22px;
  line-height: 1.15;
}

.calendar-drawer__block {
  display: grid;
  gap: 6px;
  padding: 14px 0;
  border-top: 1px solid var(--mm-border);
}

.calendar-drawer__block p {
  margin: 0;
  color: var(--mm-text);
  font-size: 13px;
  line-height: 1.6;
}

.calendar-drawer__list {
  display: grid;
  gap: 10px;
}

@media (max-width: 1120px) {
  .calendar-page__layout {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .calendar-drawer {
    display: none;
  }
}

@media (max-width: 820px) {
  .calendar-page__layout {
    grid-template-columns: 1fr;
  }

  .calendar-sidebar {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }

  .calendar-board__toolbar {
    flex-direction: column;
    align-items: start;
  }

  .calendar-board__schedule {
    grid-template-columns: 56px repeat(5, minmax(0, 1fr));
  }
}
</style>
