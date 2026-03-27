import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import type {
  CalendarAgendaItem,
  CalendarAvailability,
  CalendarEvent,
  CalendarEventDetail,
  CalendarImportResult,
  CalendarIncomingShare,
} from '~/types/api'

const navigateToMock = vi.fn(async () => undefined)
const messageErrorMock = vi.fn()
const messageSuccessMock = vi.fn()
const messageWarningMock = vi.fn()
const confirmMock = vi.fn(async () => undefined)

const routeState = {
  query: {} as Record<string, unknown>,
}

const baseEvents: CalendarEvent[] = [
  {
    id: 'evt-1',
    title: 'Team sync',
    location: 'Room A',
    startAt: '2026-03-13T09:00:00',
    endAt: '2026-03-13T10:00:00',
    allDay: false,
    timezone: 'UTC',
    reminderMinutes: 15,
    attendeeCount: 2,
    updatedAt: '2026-03-13T08:00:00',
    shared: false,
    ownerEmail: 'owner@mmmail.local',
    sharePermission: 'OWNER',
    canEdit: true,
    canDelete: true,
  },
]

const eventDetail: CalendarEventDetail = {
  id: 'evt-1',
  title: 'Team sync',
  description: 'Weekly sync',
  location: 'Room A',
  startAt: '2026-03-13T09:00:00',
  endAt: '2026-03-13T10:00:00',
  allDay: false,
  timezone: 'UTC',
  reminderMinutes: 15,
  attendees: [
    {
      id: 'att-1',
      email: 'alice@example.com',
      displayName: 'Alice',
      responseStatus: 'NEEDS_ACTION',
    },
  ],
  createdAt: '2026-03-13T08:00:00',
  updatedAt: '2026-03-13T08:30:00',
  shared: false,
  ownerEmail: 'owner@mmmail.local',
  sharePermission: 'OWNER',
  canEdit: true,
  canDelete: true,
}

const agendaItems: CalendarAgendaItem[] = [{
  id: 'agenda-1',
  title: 'Team sync',
  location: 'Room A',
  startAt: '2026-03-13T09:00:00',
  endAt: '2026-03-13T10:00:00',
  attendeeCount: 2,
  shared: false,
  ownerEmail: 'owner@mmmail.local',
  sharePermission: 'OWNER',
}]

const incomingShares: CalendarIncomingShare[] = [{
  shareId: 'share-1',
  eventId: 'evt-2',
  eventTitle: 'Shared planning',
  ownerEmail: 'owner@mmmail.local',
  permission: 'EDIT',
  responseStatus: 'NEEDS_ACTION',
  updatedAt: '2026-03-13T08:00:00',
}]

const availability: CalendarAvailability = {
  startAt: '2026-03-13T09:00:00',
  endAt: '2026-03-13T10:00:00',
  summary: {
    attendeeCount: 1,
    busyCount: 0,
    freeCount: 1,
    unknownCount: 0,
    hasConflicts: false,
  },
  attendees: [{
    email: 'alice@example.com',
    availability: 'FREE',
    overlapCount: 0,
    busySlots: [],
  }],
}

const importResult: CalendarImportResult = {
  totalCount: 1,
  importedCount: 1,
  eventIds: ['evt-3'],
}

const calendarApiMock = {
  listEvents: vi.fn(),
  getEvent: vi.fn(),
  createEvent: vi.fn(),
  updateEvent: vi.fn(),
  deleteEvent: vi.fn(),
  listAgenda: vi.fn(),
  exportCalendar: vi.fn(),
  importCalendarIcs: vi.fn(),
  shareEvent: vi.fn(),
  listShares: vi.fn(),
  updateSharePermission: vi.fn(),
  removeShare: vi.fn(),
  listIncomingShares: vi.fn(),
  respondShare: vi.fn(),
  queryAvailability: vi.fn(),
}

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
    warning: messageWarningMock,
  },
  ElMessageBox: {
    confirm: confirmMock,
  },
}))

vi.mock('~/composables/useCalendarApi', () => ({
  useCalendarApi: () => calendarApiMock,
}))

vi.mock('~/composables/useContactApi', () => ({
  useContactApi: () => ({
    listContacts: vi.fn(async () => [{
      email: 'alice@example.com',
      displayName: 'Alice',
    }]),
  }),
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) => {
      if (!params) {
        return key
      }
      return Object.entries(params).reduce(
        (result, [paramKey, value]) => result.replace(`{${paramKey}}`, String(value)),
        key,
      )
    },
  }),
}))

const ElAlert = defineComponent({
  name: 'ElAlert',
  props: { title: { type: String, default: '' } },
  template: '<div v-bind="$attrs" class="el-alert-stub"><slot />{{ title }}</div>',
})

const ElButton = defineComponent({
  name: 'ElButton',
  emits: ['click'],
  template: '<button v-bind="$attrs" type="button" @click="$emit(\'click\')"><slot /></button>',
})

const ElEmpty = defineComponent({
  name: 'ElEmpty',
  props: { description: { type: String, default: '' } },
  template: '<div v-bind="$attrs" class="el-empty-stub">{{ description }}</div>',
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: [String, Number], default: '' },
    type: { type: String, default: 'text' },
  },
  emits: ['update:modelValue'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      v-bind="$attrs"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <input
      v-else
      v-bind="$attrs"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `,
})

const ElInputNumber = defineComponent({
  name: 'ElInputNumber',
  props: { modelValue: { type: Number, default: 0 } },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))">',
})

const ElOption = defineComponent({
  name: 'ElOption',
  props: { label: { type: String, default: '' }, value: { type: [String, Number], default: '' } },
  template: '<option :value="value">{{ label || value }}</option>',
})

const ElRadioGroup = defineComponent({
  name: 'ElRadioGroup',
  template: '<div><slot /></div>',
})

const ElRadioButton = defineComponent({
  name: 'ElRadioButton',
  props: { label: { type: String, default: '' } },
  template: '<button type="button"><slot /></button>',
})

const ElSelect = defineComponent({
  name: 'ElSelect',
  props: {
    modelValue: { type: [String, Array], default: '' },
    multiple: { type: Boolean, default: false },
  },
  emits: ['update:modelValue'],
  methods: {
    onChange(event: Event) {
      const target = event.target as HTMLSelectElement
      if (this.multiple) {
        const values = Array.from(target.selectedOptions).map((option) => option.value)
        this.$emit('update:modelValue', values)
        return
      }
      this.$emit('update:modelValue', target.value)
    },
  },
  template: `
    <select v-bind="$attrs" :multiple="multiple" @change="onChange">
      <slot />
    </select>
  `,
})

const ElSkeleton = defineComponent({
  name: 'ElSkeleton',
  template: '<div class="el-skeleton-stub"></div>',
})

const ElSwitch = defineComponent({
  name: 'ElSwitch',
  props: { modelValue: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)">',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>',
})

const CalendarShareTableStub = defineComponent({
  name: 'CalendarShareTable',
  props: { shares: { type: Array, default: () => [] } },
  template: '<div data-testid="calendar-share-table">{{ shares.length }}</div>',
})

const CalendarAvailabilityPanelStub = defineComponent({
  name: 'CalendarAvailabilityPanel',
  props: {
    errorMessage: { type: String, default: '' },
  },
  template: '<div data-testid="calendar-availability-panel">{{ errorMessage }}</div>',
})

function resetApiMocks() {
  calendarApiMock.listEvents.mockResolvedValue(baseEvents)
  calendarApiMock.getEvent.mockResolvedValue(eventDetail)
  calendarApiMock.createEvent.mockResolvedValue({
    ...eventDetail,
    id: 'evt-2',
    title: 'Created event',
  } satisfies CalendarEventDetail)
  calendarApiMock.updateEvent.mockResolvedValue({
    ...eventDetail,
    title: 'Edited sync',
  } satisfies CalendarEventDetail)
  calendarApiMock.deleteEvent.mockResolvedValue(undefined)
  calendarApiMock.listAgenda.mockResolvedValue(agendaItems)
  calendarApiMock.exportCalendar.mockResolvedValue('BEGIN:VCALENDAR')
  calendarApiMock.importCalendarIcs.mockResolvedValue(importResult)
  calendarApiMock.shareEvent.mockResolvedValue(undefined)
  calendarApiMock.listShares.mockResolvedValue([])
  calendarApiMock.updateSharePermission.mockResolvedValue(undefined)
  calendarApiMock.removeShare.mockResolvedValue(undefined)
  calendarApiMock.listIncomingShares.mockResolvedValue(incomingShares)
  calendarApiMock.respondShare.mockResolvedValue({
    ...incomingShares[0],
    responseStatus: 'ACCEPTED',
  })
  calendarApiMock.queryAvailability.mockResolvedValue(availability)
}

async function importCalendarPage() {
  return await import('../pages/calendar.vue')
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.setSystemTime(new Date(2026, 2, 13, 9, 0, 0))
  vi.clearAllMocks()
  resetApiMocks()
  routeState.query = {}
  ;(globalThis as Record<string, unknown>).useRoute = () => routeState
  ;(globalThis as Record<string, unknown>).navigateTo = navigateToMock
  ;(globalThis as Record<string, unknown>).definePageMeta = () => undefined
  const urlApi = globalThis.URL as typeof URL & {
    createObjectURL?: (object: Blob | MediaSource) => string
    revokeObjectURL?: (url: string) => void
  }
  urlApi.createObjectURL = vi.fn(() => 'blob:calendar')
  urlApi.revokeObjectURL = vi.fn()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('calendar smoke', () => {
  it('covers view switch, detail, edit, create, delete and ICS import/export', async () => {
    const { default: CalendarPage } = await importCalendarPage()
    const wrapper = mount(CalendarPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElInputNumber,
          ElOption,
          ElRadioButton,
          ElRadioGroup,
          ElSelect,
          ElSkeleton,
          ElSwitch,
          ElTag,
          CalendarShareTable: CalendarShareTableStub,
          CalendarAvailabilityPanel: CalendarAvailabilityPanelStub,
        },
      },
    })

    await flushPromises()
    await vi.runAllTimersAsync()
    await flushPromises()
    expect(calendarApiMock.listEvents).toHaveBeenCalledWith('2026-03-08T00:00:00', '2026-03-15T00:00:00')

    await wrapper.get('[data-testid="calendar-range-month"]').trigger('click')
    await flushPromises()
    expect(calendarApiMock.listEvents).toHaveBeenLastCalledWith('2026-03-01T00:00:00', '2026-04-01T00:00:00')

    await wrapper.get('[data-testid="calendar-view-details"]').trigger('click')
    await flushPromises()
    expect(calendarApiMock.getEvent).toHaveBeenCalledWith('evt-1')
    expect(wrapper.get('[data-testid="calendar-detail-title"]').text()).toContain('Team sync')

    await wrapper.get('[data-testid="calendar-edit-event"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="calendar-form-title"]').setValue('Edited sync')
    await wrapper.get('[data-testid="calendar-submit"]').trigger('click')
    await flushPromises()
    expect(calendarApiMock.updateEvent).toHaveBeenCalledWith('evt-1', expect.objectContaining({ title: 'Edited sync' }))

    await wrapper.get('[data-testid="calendar-detail-delete"]').trigger('click')
    await flushPromises()
    expect(calendarApiMock.deleteEvent).toHaveBeenCalledWith('evt-1')

    await wrapper.get('[data-testid="calendar-form-title"]').setValue('Created event')
    await wrapper.get('[data-testid="calendar-form-start"]').setValue('2026-03-13T13:00:00')
    await wrapper.get('[data-testid="calendar-form-end"]').setValue('2026-03-13T14:00:00')
    await wrapper.get('[data-testid="calendar-submit"]').trigger('click')
    await flushPromises()
    expect(calendarApiMock.createEvent).toHaveBeenCalledWith(expect.objectContaining({ title: 'Created event' }))

    await wrapper.get('[data-testid="calendar-ics-textarea"]').setValue('BEGIN:VCALENDAR')
    await wrapper.get('[data-testid="calendar-ics-import"]').trigger('click')
    await flushPromises()
    expect(calendarApiMock.importCalendarIcs).toHaveBeenCalledWith({
      content: 'BEGIN:VCALENDAR',
      timezone: 'UTC',
      reminderMinutes: 15,
    })
    expect(wrapper.get('[data-testid="calendar-ics-result"]').text()).toContain('calendar.messages.importedSummary')

    await wrapper.get('[data-testid="calendar-ics-export"]').trigger('click')
    await flushPromises()
    expect(calendarApiMock.exportCalendar).toHaveBeenCalledWith('ics')
  })

  it('shows visible recovery for load, save, import and incoming-share failures', async () => {
    calendarApiMock.listEvents
      .mockRejectedValueOnce(new Error('Session expired'))
      .mockResolvedValue(baseEvents)
    calendarApiMock.createEvent.mockRejectedValueOnce(new Error('Save failed'))
    calendarApiMock.importCalendarIcs
      .mockRejectedValueOnce(new Error('ICS import failed'))
      .mockResolvedValue(importResult)

    const { default: CalendarPage } = await importCalendarPage()
    const wrapper = mount(CalendarPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElInputNumber,
          ElOption,
          ElRadioButton,
          ElRadioGroup,
          ElSelect,
          ElSkeleton,
          ElSwitch,
          ElTag,
          CalendarShareTable: CalendarShareTableStub,
          CalendarAvailabilityPanel: CalendarAvailabilityPanelStub,
        },
      },
    })

    await flushPromises()
    expect(wrapper.get('[data-testid="calendar-events-error"]').text()).toContain('Session expired')

    await wrapper.get('[data-testid="calendar-events-retry"]').trigger('click')
    await flushPromises()
    expect(calendarApiMock.listEvents).toHaveBeenCalledTimes(2)

    await wrapper.get('[data-testid="calendar-form-title"]').setValue('Broken create')
    await wrapper.get('[data-testid="calendar-form-start"]').setValue('2026-03-13T15:00:00')
    await wrapper.get('[data-testid="calendar-form-end"]').setValue('2026-03-13T16:00:00')
    await wrapper.get('[data-testid="calendar-submit"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="calendar-save-error"]').text()).toContain('Save failed')

    await wrapper.get('[data-testid="calendar-ics-textarea"]').setValue('BROKEN')
    await wrapper.get('[data-testid="calendar-ics-import"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="calendar-ics-error"]').text()).toContain('ICS import failed')

    await wrapper.get('[data-testid="calendar-ics-retry"]').trigger('click')
    await flushPromises()
    expect(calendarApiMock.importCalendarIcs).toHaveBeenCalledTimes(2)
  })

  it('switches to incoming shares and accepts a shared event', async () => {
    const { default: CalendarPage } = await importCalendarPage()
    const wrapper = mount(CalendarPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElInputNumber,
          ElOption,
          ElRadioButton,
          ElRadioGroup,
          ElSelect,
          ElSkeleton,
          ElSwitch,
          ElTag,
          CalendarShareTable: CalendarShareTableStub,
          CalendarAvailabilityPanel: CalendarAvailabilityPanelStub,
        },
      },
    })

    await flushPromises()
    await wrapper.get('[data-testid="calendar-surface-incoming-shares"]').trigger('click')
    await flushPromises()
    expect(navigateToMock).toHaveBeenCalled()

    const acceptButton = wrapper.findAll('button').find((node) => node.text().includes('calendar.actions.accept'))
    expect(acceptButton).toBeTruthy()
    await acceptButton!.trigger('click')
    await flushPromises()
    expect(calendarApiMock.respondShare).toHaveBeenCalledWith('share-1', { response: 'ACCEPT' })
  })
})
