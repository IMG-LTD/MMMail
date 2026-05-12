<script setup lang="ts">
import type { CalendarAvailability, CalendarDayCell } from './calendar-types'
import CalendarConflictPanel from './CalendarConflictPanel.vue'

defineProps<{
  availability: CalendarAvailability | null
  days: CalendarDayCell[]
  loading: boolean
  monthLabel: string
  summaries: Array<{ active: boolean; count: number; key: string; name: string }>
}>()

defineEmits<{
  refresh: []
  selectDay: [date: Date]
  today: []
}>()
</script>

<template>
  <aside class="calendar-filter-sidebar">
    <button class="calendar-filter-sidebar__primary" type="button" @click="$emit('refresh')">
      {{ loading ? 'Refreshing' : 'Refresh calendar' }}
    </button>
    <button type="button" @click="$emit('today')">Today</button>
    <article class="calendar-filter-sidebar__panel">
      <header>
        <strong>{{ monthLabel }}</strong>
        <span>{{ days.length }}</span>
      </header>
      <div class="calendar-filter-sidebar__grid">
        <button
          v-for="day in days"
          :key="day.key"
          :class="{ 'calendar-filter-sidebar__day--active': day.active, 'calendar-filter-sidebar__day--has-event': day.hasEvents }"
          type="button"
          @click="$emit('selectDay', day.date)"
        >
          {{ day.dayNumber }}
        </button>
      </div>
    </article>
    <article class="calendar-filter-sidebar__panel">
      <span class="section-label">Calendars</span>
      <label v-for="calendar in summaries" :key="calendar.key" class="calendar-filter-sidebar__toggle">
        <span :class="{ 'calendar-filter-sidebar__check--active': calendar.active }" />
        <span>{{ calendar.name }}</span>
        <strong>{{ calendar.count }}</strong>
      </label>
    </article>
    <CalendarConflictPanel :availability="availability" :loading="loading" />
  </aside>
</template>
