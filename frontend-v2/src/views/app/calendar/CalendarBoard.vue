<script setup lang="ts">
import { NButton } from "naive-ui";
import type {
  CalendarDayCell,
  CalendarTimeSlot,
  CalendarViewMode,
  PositionedCalendarEvent,
} from "./calendar-types";

defineProps<{
  activeViewMode: CalendarViewMode;
  agendaItems: Array<{ id: string; meta: string; title: string }>;
  emptyCopy: string;
  loading: boolean;
  monthLabel: string;
  positionedEvents: PositionedCalendarEvent[];
  rangeLabel: string;
  scheduleDays: CalendarDayCell[];
  timeSlots: CalendarTimeSlot[];
  viewModes: Array<{ key: CalendarViewMode; label: string }>;
}>();

defineEmits<{
  openEvent: [eventId: string];
  selectEvent: [eventId: string];
  setMode: [mode: CalendarViewMode];
  today: [];
}>();
</script>

<template>
  <section class="calendar-board">
    <header class="calendar-board__toolbar">
      <div>
        <span class="section-label">{{ monthLabel }}</span>
        <h1>{{ rangeLabel }}</h1>
      </div>
      <div class="calendar-board__actions">
        <NButton native-type="button" @click="$emit('today')">Today</NButton>
        <NButton
          class="calendar-event-trigger"
          native-type="button"
          @click="$emit('openEvent', positionedEvents[0]?.id || '')"
          >New event</NButton
        >
        <div class="calendar-board__switcher">
          <NButton
            v-for="mode in viewModes"
            :key="mode.key"
            :class="{ 'calendar-board__switcher--active': mode.key === activeViewMode }"
            native-type="button"
            @click="$emit('setMode', mode.key)"
          >
            {{ mode.label }}
          </NButton>
        </div>
      </div>
    </header>
    <div v-if="activeViewMode === 'agenda'" class="calendar-board__agenda">
      <NButton
        v-for="item in agendaItems"
        :key="item.id"
        class="calendar-board__agenda-item"
        native-type="button"
        @click="$emit('openEvent', item.id)"
      >
        <strong>{{ item.title }}</strong>
        <span>{{ item.meta }}</span>
      </NButton>
      <p v-if="!agendaItems.length">{{ emptyCopy }}</p>
    </div>
    <div v-else class="calendar-board__schedule">
      <div class="calendar-board__head">
        <span />
        <strong v-for="day in scheduleDays" :key="day.key">{{ day.label }}</strong>
      </div>
      <div v-for="slot in timeSlots" :key="slot.key" class="calendar-board__row">
        <span class="calendar-board__time">{{ slot.label }}</span>
        <span
          v-for="day in scheduleDays"
          :key="`${day.key}-${slot.key}`"
          class="calendar-board__cell"
        />
      </div>
      <NButton
        v-for="event in positionedEvents"
        :key="event.key"
        class="calendar-board__event calendar-event-trigger"
        :class="{
          'calendar-board__event--selected': event.selected,
          'calendar-board__event--shared': event.shared,
        }"
        :style="event.style"
        native-type="button"
        @click="$emit('openEvent', event.id)"
      >
        <strong>{{ event.title }}</strong>
        <span>{{ event.meta }}</span>
      </NButton>
      <p v-if="!positionedEvents.length && !loading" class="calendar-board__empty">
        {{ emptyCopy }}
      </p>
    </div>
  </section>
</template>
