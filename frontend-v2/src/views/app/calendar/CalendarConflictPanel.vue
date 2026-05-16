<script setup lang="ts">
import type { CalendarAvailability } from "./calendar-types";

defineProps<{
  availability: CalendarAvailability | null;
  loading: boolean;
}>();
</script>

<template>
  <section class="calendar-conflict-panel">
    <span class="section-label">Conflict detection</span>
    <div
      class="calendar-resource-state"
      :class="{ 'calendar-resource-state--conflict': availability?.summary.hasConflicts }"
    >
      <strong>{{
        availability?.summary.hasConflicts ? "Resource conflict detected" : "No time conflict"
      }}</strong>
      <p v-if="loading">Checking availability.</p>
      <p v-else-if="availability">
        Attendees {{ availability.summary.attendeeCount }} · Busy
        {{ availability.summary.busyCount }} · Free {{ availability.summary.freeCount }}
      </p>
      <p v-else>Current room and attendee state is ready for review.</p>
    </div>
  </section>
</template>
