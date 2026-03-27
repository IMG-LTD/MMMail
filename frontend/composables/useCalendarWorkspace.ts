import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  CalendarAgendaItem,
  CalendarAvailability,
  CalendarEvent,
  CalendarEventDetail,
  CalendarEventShare,
  CalendarImportResult,
  CalendarIncomingShare,
  CreateCalendarEventRequest,
  UpdateCalendarEventRequest,
} from '~/types/api'
import { useCalendarApi } from '~/composables/useCalendarApi'
import { useContactApi } from '~/composables/useContactApi'
import { useI18n } from '~/composables/useI18n'
import {
  type CalendarIncomingShareFilter,
  hasCalendarAvailabilityQuery,
  parseCalendarAttendeesInput,
} from '~/utils/calendar-availability'
import {
  buildCalendarRangeWindow,
  createCalendarAnchor,
  normalizeAllDayRange,
  shiftCalendarAnchor,
  type CalendarRangeView,
} from '~/utils/calendar-workspace'

type CalendarView = 'events' | 'incoming-shares'

interface CalendarEditorForm {
  title: string
  description: string
  location: string
  startAt: string
  endAt: string
  allDay: boolean
  timezone: string
  reminderMinutes: number
  attendeesText: string
}

interface CalendarShareForm {
  targetEmail: string
  permission: 'VIEW' | 'EDIT'
}

const DEFAULT_TIMEZONE = 'UTC'
const DEFAULT_REMINDER_MINUTES = 15
const AVAILABILITY_DELAY_MS = 240

export function useCalendarWorkspace() {
  const { t } = useI18n()
  const route = useRoute()
  const currentView = ref<CalendarView>('events')
  const rangeView = ref<CalendarRangeView>('week')
  const rangeAnchor = ref(createCalendarAnchor())
  const selectedEventId = ref('')
  const selectedEventDetail = ref<CalendarEventDetail | null>(null)
  const events = ref<CalendarEvent[]>([])
  const agendaItems = ref<CalendarAgendaItem[]>([])
  const availability = ref<CalendarAvailability | null>(null)
  const eventShares = ref<CalendarEventShare[]>([])
  const incomingShares = ref<CalendarIncomingShare[]>([])
  const contactOptions = ref<Array<{ email: string; label: string }>>([])
  const selectedContactEmails = ref<string[]>([])
  const mutationShareId = ref('')
  const respondingShareId = ref('')
  const loading = ref(false)
  const detailLoading = ref(false)
  const agendaLoading = ref(false)
  const availabilityLoading = ref(false)
  const saving = ref(false)
  const importing = ref(false)
  const eventsErrorMessage = ref('')
  const detailErrorMessage = ref('')
  const agendaErrorMessage = ref('')
  const incomingErrorMessage = ref('')
  const availabilityErrorMessage = ref('')
  const saveErrorMessage = ref('')
  const importErrorMessage = ref('')
  const importResult = ref<CalendarImportResult | null>(null)
  const incomingFilter = ref<CalendarIncomingShareFilter>('ALL')
  const availabilityTimer = ref<ReturnType<typeof setTimeout> | null>(null)
  const icsContent = ref('')
  const editingEventId = ref('')
  const editingCanEdit = ref(true)
  const editingCanDelete = ref(false)

  const form = reactive<CalendarEditorForm>({
    title: '',
    description: '',
    location: '',
    startAt: '',
    endAt: '',
    allDay: false,
    timezone: DEFAULT_TIMEZONE,
    reminderMinutes: DEFAULT_REMINDER_MINUTES,
    attendeesText: '',
  })
  const shareForm = reactive<CalendarShareForm>({
    targetEmail: '',
    permission: 'VIEW',
  })

  const {
    listEvents,
    getEvent,
    createEvent,
    updateEvent,
    deleteEvent,
    listAgenda,
    exportCalendar,
    importCalendarIcs,
    shareEvent,
    listShares,
    updateSharePermission,
    removeShare,
    listIncomingShares,
    respondShare,
    queryAvailability,
  } = useCalendarApi()
  const { listContacts } = useContactApi()

  const currentRange = computed(() => buildCalendarRangeWindow(rangeAnchor.value, rangeView.value))
  const attendeesInput = computed(() => parseCalendarAttendeesInput(form.attendeesText))
  const availabilityQueryReady = computed(() => hasCalendarAvailabilityQuery(
    form.startAt,
    form.endAt,
    attendeesInput.value.length,
  ))
  const viewOptions = computed<Array<{ value: CalendarView; label: string }>>(() => [
    { value: 'events', label: t('calendar.views.events') },
    { value: 'incoming-shares', label: t('calendar.views.incomingShares') },
  ])
  const rangeOptions = computed<Array<{ value: CalendarRangeView; label: string }>>(() => [
    { value: 'day', label: t('calendar.range.day') },
    { value: 'week', label: t('calendar.range.week') },
    { value: 'month', label: t('calendar.range.month') },
  ])
  const importResultSummary = computed(() => {
    if (!importResult.value) {
      return ''
    }
    return t('calendar.messages.importedSummary', {
      total: importResult.value.totalCount,
      imported: importResult.value.importedCount,
    })
  })

  function resetForm() {
    editingEventId.value = ''
    editingCanEdit.value = true
    editingCanDelete.value = false
    eventShares.value = []
    availability.value = null
    availabilityErrorMessage.value = ''
    saveErrorMessage.value = ''
    selectedContactEmails.value = []
    form.title = ''
    form.description = ''
    form.location = ''
    form.startAt = nowPlusHours(1)
    form.endAt = nowPlusHours(2)
    form.allDay = false
    form.timezone = DEFAULT_TIMEZONE
    form.reminderMinutes = DEFAULT_REMINDER_MINUTES
    form.attendeesText = ''
    shareForm.targetEmail = ''
    shareForm.permission = 'VIEW'
  }

  function nowPlusHours(hours: number): string {
    const base = new Date()
    base.setMinutes(0, 0, 0)
    base.setHours(base.getHours() + hours)
    return `${base.getFullYear()}-${pad(base.getMonth() + 1)}-${pad(base.getDate())}T${pad(base.getHours())}:${pad(base.getMinutes())}:${pad(base.getSeconds())}`
  }

  function pad(value: number): string {
    return `${value}`.padStart(2, '0')
  }

  function applySelectedContacts(emails: string[]) {
    if (!emails.length) {
      return
    }
    const byEmail = new Map(attendeesInput.value.map((item) => [item.email, item]))
    for (const email of emails) {
      const option = contactOptions.value.find((item) => item.email === email)
      if (!byEmail.has(email)) {
        byEmail.set(email, {
          email,
          displayName: option?.label.split(' <')[0] || undefined,
        })
      }
    }
    form.attendeesText = Array.from(byEmail.values())
      .map((item) => item.displayName ? `${item.displayName} <${item.email}>` : item.email)
      .join('\n')
    selectedContactEmails.value = []
  }

  async function updateView(view: CalendarView) {
    currentView.value = view
    const query = { ...route.query }
    if (view === 'incoming-shares') {
      query.view = view
    } else {
      delete query.view
    }
    await navigateTo({ path: '/calendar', query }, { replace: true })
  }

  function selectRangeView(view: CalendarRangeView) {
    rangeView.value = view
  }

  function moveRange(step: number) {
    rangeAnchor.value = shiftCalendarAnchor(rangeAnchor.value, rangeView.value, step)
  }

  function resetRangeToToday() {
    rangeAnchor.value = createCalendarAnchor()
  }

  async function refreshWorkspace() {
    await Promise.all([loadEvents(), loadAgenda(), loadIncoming()])
    if (selectedEventId.value) {
      await loadEventDetail(selectedEventId.value)
    }
    await loadSharesForEditingEvent()
  }

  async function loadEvents() {
    loading.value = true
    eventsErrorMessage.value = ''
    try {
      events.value = await listEvents(currentRange.value.from, currentRange.value.to)
    } catch (error) {
      events.value = []
      eventsErrorMessage.value = resolveErrorMessage(error, 'calendar.messages.loadEventsFailed')
      ElMessage.error(eventsErrorMessage.value)
    } finally {
      loading.value = false
    }
  }

  async function loadAgenda() {
    agendaLoading.value = true
    agendaErrorMessage.value = ''
    try {
      agendaItems.value = await listAgenda(7)
    } catch (error) {
      agendaItems.value = []
      agendaErrorMessage.value = resolveErrorMessage(error, 'calendar.messages.loadAgendaFailed')
    } finally {
      agendaLoading.value = false
    }
  }

  async function loadContacts() {
    const contacts = await listContacts('', false)
    contactOptions.value = contacts.map((contact) => ({
      email: contact.email,
      label: `${contact.displayName} <${contact.email}>`,
    }))
  }

  async function loadIncoming() {
    incomingErrorMessage.value = ''
    try {
      incomingShares.value = await listIncomingShares()
    } catch (error) {
      incomingShares.value = []
      incomingErrorMessage.value = resolveErrorMessage(error, 'calendar.messages.loadIncomingFailed')
    }
  }

  async function loadEventDetail(eventId: string) {
    selectedEventId.value = eventId
    detailLoading.value = true
    detailErrorMessage.value = ''
    try {
      selectedEventDetail.value = await getEvent(eventId)
    } catch (error) {
      selectedEventDetail.value = null
      detailErrorMessage.value = resolveErrorMessage(error, 'calendar.messages.loadEventDetailFailed')
      ElMessage.error(detailErrorMessage.value)
    } finally {
      detailLoading.value = false
    }
  }

  async function loadSharesForEditingEvent() {
    if (!editingEventId.value || !editingCanDelete.value) {
      eventShares.value = []
      return
    }
    eventShares.value = await listShares(editingEventId.value)
  }

  async function submitEvent() {
    const validationError = validateEditorForm()
    if (validationError) {
      saveErrorMessage.value = validationError
      ElMessage.warning(validationError)
      return
    }
    const payload = buildEventPayload()
    saving.value = true
    saveErrorMessage.value = ''
    try {
      const detail = editingEventId.value
        ? await updateEvent(editingEventId.value, payload)
        : await createEvent(payload)
      selectedEventDetail.value = detail
      selectedEventId.value = detail.id
      ElMessage.success(t(editingEventId.value ? 'calendar.messages.eventUpdated' : 'calendar.messages.eventCreated'))
      resetForm()
      await refreshWorkspace()
      await loadEventDetail(detail.id)
    } catch (error) {
      saveErrorMessage.value = resolveErrorMessage(error, 'calendar.messages.saveFailed')
      ElMessage.error(saveErrorMessage.value)
    } finally {
      saving.value = false
    }
  }

  async function startEditEvent(eventId: string) {
    try {
      const detail = await getEvent(eventId)
      selectedEventId.value = eventId
      selectedEventDetail.value = detail
      editingEventId.value = eventId
      editingCanEdit.value = detail.canEdit
      editingCanDelete.value = detail.canDelete
      form.title = detail.title
      form.description = detail.description || ''
      form.location = detail.location || ''
      form.startAt = detail.startAt
      form.endAt = detail.endAt
      form.allDay = detail.allDay
      form.timezone = detail.timezone || DEFAULT_TIMEZONE
      form.reminderMinutes = detail.reminderMinutes ?? DEFAULT_REMINDER_MINUTES
      form.attendeesText = detail.attendees
        .map((attendee) => attendee.displayName ? `${attendee.displayName} <${attendee.email}>` : attendee.email)
        .join('\n')
      await loadSharesForEditingEvent()
      scheduleAvailabilityRefresh()
    } catch (error) {
      detailErrorMessage.value = resolveErrorMessage(error, 'calendar.messages.loadEventDetailFailed')
      ElMessage.error(detailErrorMessage.value)
    }
  }

  async function removeEventById(eventId: string, canDelete: boolean) {
    if (!canDelete) {
      ElMessage.warning(t('calendar.messages.ownerDeleteOnly'))
      return
    }
    try {
      await ElMessageBox.confirm(t('calendar.prompts.deleteEventMessage'), t('calendar.prompts.deleteEventTitle'), {
        confirmButtonText: t('calendar.actions.delete'),
        cancelButtonText: t('common.actions.cancel'),
        type: 'warning',
      })
    } catch {
      return
    }
    try {
      await deleteEvent(eventId)
      ElMessage.success(t('calendar.messages.eventDeleted'))
      if (editingEventId.value === eventId) {
        resetForm()
      }
      if (selectedEventId.value === eventId) {
        selectedEventId.value = ''
        selectedEventDetail.value = null
      }
      await refreshWorkspace()
    } catch (error) {
      detailErrorMessage.value = resolveErrorMessage(error, 'calendar.messages.deleteFailed')
      ElMessage.error(detailErrorMessage.value)
    }
  }

  async function onShareEvent() {
    if (!editingEventId.value || !editingCanDelete.value) {
      ElMessage.warning(t('calendar.messages.selectOwnedEvent'))
      return
    }
    const targetEmail = shareForm.targetEmail.trim().toLowerCase()
    if (!targetEmail) {
      ElMessage.warning(t('calendar.messages.targetEmailRequired'))
      return
    }
    await shareEvent(editingEventId.value, {
      targetEmail,
      permission: shareForm.permission,
    })
    shareForm.targetEmail = ''
    ElMessage.success(t('calendar.messages.shareCreated'))
    await Promise.all([loadSharesForEditingEvent(), loadIncoming()])
  }

  async function onUpdateSharePermission(payload: { shareId: string; permission: 'VIEW' | 'EDIT' }) {
    if (!editingEventId.value || !editingCanDelete.value) {
      return
    }
    mutationShareId.value = payload.shareId
    try {
      await updateSharePermission(editingEventId.value, payload.shareId, { permission: payload.permission })
      ElMessage.success(t('calendar.messages.sharePermissionUpdated'))
      await Promise.all([loadSharesForEditingEvent(), loadIncoming()])
    } finally {
      mutationShareId.value = ''
    }
  }

  async function onRemoveShare(shareId: string) {
    if (!editingEventId.value || !editingCanDelete.value) {
      return
    }
    mutationShareId.value = shareId
    try {
      await removeShare(editingEventId.value, shareId)
      ElMessage.success(t('calendar.messages.shareRemoved'))
      await Promise.all([loadSharesForEditingEvent(), loadIncoming()])
    } finally {
      mutationShareId.value = ''
    }
  }

  async function onRespondShare(payload: { shareId: string; response: 'ACCEPT' | 'DECLINE' }) {
    respondingShareId.value = payload.shareId
    try {
      await respondShare(payload.shareId, { response: payload.response })
      ElMessage.success(t(payload.response === 'ACCEPT'
        ? 'calendar.messages.inviteAccepted'
        : 'calendar.messages.inviteDeclined'))
      await refreshWorkspace()
    } finally {
      respondingShareId.value = ''
    }
  }

  async function onExportIcs() {
    try {
      const text = await exportCalendar('ics')
      downloadText('mmmail-calendar.ics', text, 'text/calendar;charset=utf-8')
      ElMessage.success(t('calendar.messages.exported'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'calendar.messages.exportFailed'))
    }
  }

  async function onImportIcs() {
    if (!icsContent.value.trim()) {
      importErrorMessage.value = t('calendar.messages.importContentRequired')
      ElMessage.warning(importErrorMessage.value)
      return
    }
    importing.value = true
    importErrorMessage.value = ''
    try {
      importResult.value = await importCalendarIcs({
        content: icsContent.value.trim(),
        timezone: form.timezone.trim() || DEFAULT_TIMEZONE,
        reminderMinutes: form.reminderMinutes,
      })
      icsContent.value = ''
      ElMessage.success(t('calendar.messages.importSucceeded'))
      await refreshWorkspace()
      const firstEventId = importResult.value.eventIds[0]
      if (firstEventId) {
        await loadEventDetail(firstEventId)
      }
    } catch (error) {
      importResult.value = null
      importErrorMessage.value = resolveErrorMessage(error, 'calendar.messages.importFailed')
      ElMessage.error(importErrorMessage.value)
    } finally {
      importing.value = false
    }
  }

  function updateImportContent(value: string) {
    icsContent.value = value
  }

  function retryEventsLoad() {
    void loadEvents()
  }

  function retryDetailLoad() {
    if (selectedEventId.value) {
      void loadEventDetail(selectedEventId.value)
    }
  }

  function retryImport() {
    void onImportIcs()
  }

  function scheduleAvailabilityRefresh() {
    if (availabilityTimer.value) {
      clearTimeout(availabilityTimer.value)
    }
    availabilityTimer.value = setTimeout(() => {
      void loadAvailabilityData()
    }, AVAILABILITY_DELAY_MS)
  }

  async function loadAvailabilityData() {
    availabilityErrorMessage.value = ''
    if (!availabilityQueryReady.value) {
      availability.value = null
      return
    }
    availabilityLoading.value = true
    try {
      availability.value = await queryAvailability({
        startAt: form.startAt,
        endAt: form.endAt,
        attendeeEmails: attendeesInput.value.map((item) => item.email),
        excludeEventId: editingEventId.value || undefined,
      })
    } catch (error) {
      availability.value = null
      availabilityErrorMessage.value = resolveErrorMessage(error, 'calendar.messages.loadAvailabilityFailed')
    } finally {
      availabilityLoading.value = false
    }
  }

  function buildEventPayload(): CreateCalendarEventRequest | UpdateCalendarEventRequest {
    return {
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      location: form.location.trim() || undefined,
      startAt: form.startAt,
      endAt: form.endAt,
      allDay: form.allDay,
      timezone: form.timezone.trim() || DEFAULT_TIMEZONE,
      reminderMinutes: form.reminderMinutes,
      attendees: attendeesInput.value,
    }
  }

  function validateEditorForm(): string {
    if (!form.title.trim()) {
      return t('calendar.messages.titleRequired')
    }
    if (!form.startAt || !form.endAt) {
      return t('calendar.messages.timeRequired')
    }
    if (new Date(form.startAt).getTime() >= new Date(form.endAt).getTime()) {
      return t('calendar.messages.invalidTimeRange')
    }
    if (editingEventId.value && !editingCanEdit.value) {
      return t('calendar.messages.readOnlyEvent')
    }
    return ''
  }

  function resolveErrorMessage(error: unknown, fallbackKey: string): string {
    if (error instanceof Error && error.message) {
      return error.message
    }
    return t(fallbackKey)
  }

  function downloadText(filename: string, content: string, type: string) {
    const blob = new Blob([content], { type })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = filename
    anchor.click()
    URL.revokeObjectURL(url)
  }

  watch(() => route.query.view, (nextView) => {
    currentView.value = nextView === 'incoming-shares' ? 'incoming-shares' : 'events'
  }, { immediate: true })

  watch([rangeView, rangeAnchor], () => {
    void loadEvents()
  }, { immediate: true })

  watch(() => form.allDay, (enabled) => {
    if (!enabled || !form.startAt || !form.endAt) {
      return
    }
    const normalized = normalizeAllDayRange(form.startAt, form.endAt)
    form.startAt = normalized.startAt
    form.endAt = normalized.endAt
  })

  watch(
    [() => form.startAt, () => form.endAt, () => attendeesInput.value.map((item) => item.email).join(','), editingEventId],
    () => scheduleAvailabilityRefresh(),
  )

  onMounted(async () => {
    resetForm()
    await Promise.all([loadAgenda(), loadIncoming(), loadContacts()])
  })

  onBeforeUnmount(() => {
    if (availabilityTimer.value) {
      clearTimeout(availabilityTimer.value)
    }
  })

  return {
    agendaErrorMessage,
    agendaItems,
    agendaLoading,
    applySelectedContacts,
    availability,
    availabilityErrorMessage,
    availabilityLoading,
    availabilityQueryReady,
    contactOptions,
    currentRange,
    currentView,
    detailErrorMessage,
    detailLoading,
    editingCanDelete,
    editingCanEdit,
    editingEventId,
    eventShares,
    events,
    eventsErrorMessage,
    form,
    importErrorMessage,
    importing,
    importResultSummary,
    incomingErrorMessage,
    incomingFilter,
    incomingShares,
    loading,
    moveRange,
    mutationShareId,
    onExportIcs,
    onImportIcs,
    onRemoveShare,
    onRespondShare,
    onShareEvent,
    onUpdateSharePermission,
    rangeOptions,
    rangeView,
    resetForm,
    resetRangeToToday,
    respondingShareId,
    retryDetailLoad,
    retryEventsLoad,
    retryImport,
    saveErrorMessage,
    saving,
    selectedContactEmails,
    selectedEventDetail,
    selectedEventId,
    selectRangeView,
    shareForm,
    startEditEvent,
    submitEvent,
    updateImportContent,
    updateView,
    viewOptions,
    loadEventDetail,
    refreshWorkspace,
    removeEventById,
    icsContent,
  }
}
