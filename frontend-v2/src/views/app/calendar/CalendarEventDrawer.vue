<script setup lang="ts">
import type { CalendarAvailability, CalendarSurfaceItem } from './calendar-types'
import CalendarConflictPanel from './CalendarConflictPanel.vue'

defineProps<{
  availability: CalendarAvailability | null
  loading: boolean
  open: boolean
  saveError: string
  selectedItem: CalendarSurfaceItem | null
}>()

defineEmits<{
  close: []
  retry: []
  save: []
}>()
</script>

<template>
  <aside v-if="open" class="calendar-event-drawer" role="dialog" aria-label="Edit calendar event">
    <div class="calendar-event-drawer__head">
      <span class="section-label">Edit event</span>
      <button type="button" @click="$emit('close')">Close</button>
    </div>
    <label>
      <span>Title</span>
      <input aria-label="Event title" :value="selectedItem?.title || 'Privacy and security review'">
    </label>
    <label>
      <span>Date</span>
      <input aria-label="Date" value="2026-05-22">
    </label>
    <label>
      <span>Time</span>
      <input aria-label="Time" :value="selectedItem ? '10:00 - 11:30' : '09:00 - 10:00'">
    </label>
    <label>
      <span>Location</span>
      <input aria-label="Location" :value="selectedItem?.location || 'Nexa Meet / Room A'">
    </label>
    <label>
      <span>Notes</span>
      <textarea aria-label="Notes" value="Review agenda, attendees, room state, and shared visibility before saving." />
    </label>
    <CalendarConflictPanel :availability="availability" :loading="loading" />
    <p v-if="saveError" class="calendar-save-error">{{ saveError }}</p>
    <button class="calendar-event-drawer__save" type="button" @click="$emit('save')">Save</button>
    <button v-if="saveError" class="calendar-save-retry" type="button" @click="$emit('retry')">Retry</button>
  </aside>
</template>
