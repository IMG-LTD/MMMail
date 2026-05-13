<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { CalendarAvailability, CalendarEventDraft, CalendarSurfaceItem } from './calendar-types'
import CalendarConflictPanel from './CalendarConflictPanel.vue'

const props = defineProps<{
  availability: CalendarAvailability | null
  loading: boolean
  open: boolean
  saveError: string
  selectedItem: CalendarSurfaceItem | null
}>()

const emit = defineEmits<{
  close: []
  retry: [draft: CalendarEventDraft]
  save: [draft: CalendarEventDraft]
}>()

const draft = reactive<CalendarEventDraft>(createDraft(props.selectedItem))

watch(() => [props.open, props.selectedItem?.id], () => {
  Object.assign(draft, createDraft(props.selectedItem))
})

function createDraft(item: CalendarSurfaceItem | null): CalendarEventDraft {
  return {
    allDay: item?.allDay || false,
    description: '',
    endAt: toDateTimeInputValue(item?.endAt || '2026-05-22T10:00:00'),
    location: item?.location || 'Nexa Meet / Room A',
    reminderMinutes: item?.reminderMinutes ?? 15,
    startAt: toDateTimeInputValue(item?.startAt || '2026-05-22T09:00:00'),
    timezone: item?.timezone || 'UTC',
    title: item?.title || 'Privacy and security review'
  }
}

function emitSave() {
  emit('save', { ...draft })
}

function toDateTimeInputValue(value: string) {
  return value.slice(0, 16)
}
</script>

<template>
  <aside v-if="open" class="calendar-event-drawer" role="dialog" aria-label="Edit calendar event">
    <div class="calendar-event-drawer__head">
      <span class="section-label">Edit event</span>
      <button type="button" @click="$emit('close')">Close</button>
    </div>
    <label>
      <span>Title</span>
      <input aria-label="Event title" v-model="draft.title">
    </label>
    <label>
      <span>Start</span>
      <input aria-label="Start time" v-model="draft.startAt" type="datetime-local">
    </label>
    <label>
      <span>End</span>
      <input aria-label="End time" v-model="draft.endAt" type="datetime-local">
    </label>
    <label>
      <span>Location</span>
      <input aria-label="Location" v-model="draft.location">
    </label>
    <label>
      <span>Notes</span>
      <textarea aria-label="Notes" v-model="draft.description" />
    </label>
    <CalendarConflictPanel :availability="availability" :loading="loading" />
    <p v-if="saveError" class="calendar-save-error">{{ saveError }}</p>
    <button class="calendar-event-drawer__save" type="button" @click="emitSave">Save</button>
    <button v-if="saveError" class="calendar-save-retry" type="button" @click="$emit('retry', { ...draft })">Retry</button>
  </aside>
</template>
