<script setup lang="ts">
import { NButton, NInput } from "naive-ui";
import { reactive, watch } from "vue";
import type {
  CalendarAvailability,
  CalendarEventDraft,
  CalendarSurfaceItem,
} from "./calendar-types";
import CalendarConflictPanel from "./CalendarConflictPanel.vue";

const props = defineProps<{
  availability: CalendarAvailability | null;
  loading: boolean;
  open: boolean;
  saveError: string;
  selectedItem: CalendarSurfaceItem | null;
}>();

const emit = defineEmits<{
  close: [];
  retry: [draft: CalendarEventDraft];
  save: [draft: CalendarEventDraft];
}>();

const draft = reactive<CalendarEventDraft>(createDraft(props.selectedItem));

watch(
  () => [props.open, props.selectedItem?.id],
  () => {
    Object.assign(draft, createDraft(props.selectedItem));
  },
);

function createDraft(item: CalendarSurfaceItem | null): CalendarEventDraft {
  return {
    allDay: item?.allDay || false,
    description: "",
    endAt: toDateTimeInputValue(item?.endAt || "2026-05-22T10:00:00"),
    location: item?.location || "Nexa Meet / Room A",
    reminderMinutes: item?.reminderMinutes ?? 15,
    startAt: toDateTimeInputValue(item?.startAt || "2026-05-22T09:00:00"),
    timezone: item?.timezone || "UTC",
    title: item?.title || "Privacy and security review",
  };
}

function emitSave() {
  emit("save", { ...draft });
}

function toDateTimeInputValue(value: string) {
  return value.slice(0, 16);
}
</script>

<template>
  <aside v-if="open" class="calendar-event-drawer" role="dialog" aria-label="Edit calendar event">
    <div class="calendar-event-drawer__head">
      <span class="section-label">Edit event</span>
      <NButton native-type="button" @click="$emit('close')">Close</NButton>
    </div>
    <label>
      <span>Title</span>
      <NInput v-model:value="draft.title" aria-label="Event title" />
    </label>
    <label>
      <span>Start</span>
      <NInput v-model:value="draft.startAt" aria-label="Start time" />
    </label>
    <label>
      <span>End</span>
      <NInput v-model:value="draft.endAt" aria-label="End time" />
    </label>
    <label>
      <span>Location</span>
      <NInput v-model:value="draft.location" aria-label="Location" />
    </label>
    <label>
      <span>Notes</span>
      <NInput v-model:value="draft.description" aria-label="Notes" type="textarea" />
    </label>
    <CalendarConflictPanel :availability="availability" :loading="loading" />
    <p v-if="saveError" class="calendar-save-error">{{ saveError }}</p>
    <NButton class="calendar-event-drawer__save" native-type="button" @click="emitSave"
      >Save</NButton
    >
    <NButton
      v-if="saveError"
      class="calendar-save-retry"
      native-type="button"
      @click="$emit('retry', { ...draft })"
      >Retry</NButton
    >
  </aside>
</template>
