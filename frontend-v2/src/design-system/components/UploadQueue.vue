<script setup lang="ts">
import { NButton } from "naive-ui";
import { watch } from "vue";
import { useSrLive } from "@/shared/composables/useSrLive";

type UploadStatus = "queued" | "uploading" | "paused" | "completed" | "failed" | "canceled";

export interface UploadQueueItem {
  destination?: string;
  error?: string;
  id: string;
  name: string;
  progress: number;
  size: string;
  status: UploadStatus;
}

const props = withDefaults(
  defineProps<{
    ariaLabel?: string;
    compact?: boolean;
    items: readonly UploadQueueItem[];
    position?: "inline" | "floating";
  }>(),
  {
    ariaLabel: "Upload queue",
    compact: false,
    position: "inline",
  },
);

const emit = defineEmits<{
  cancel: [item: UploadQueueItem];
  openDestination: [item: UploadQueueItem];
  pause: [item: UploadQueueItem];
  remove: [item: UploadQueueItem];
  resume: [item: UploadQueueItem];
  retry: [item: UploadQueueItem];
}>();

const srLive = useSrLive();

function announce(message: string, urgent = false) {
  if (urgent) {
    srLive.assertive(message);
    return;
  }
  srLive.polite(message);
}

function itemStatusText(item: UploadQueueItem) {
  if (item.status === "failed") {
    return `Failed: ${item.error || "Upload error"}`;
  }
  return `${item.status} ${Math.round(item.progress)}%`;
}

watch(
  () => props.items.map((item) => `${item.id}:${item.status}:${item.progress}`).join("|"),
  () => {
    const activeItem = props.items.find(
      (item) =>
        item.status === "failed" || item.status === "completed" || item.status === "uploading",
    );
    if (activeItem) {
      announce(`${activeItem.name} ${itemStatusText(activeItem)}`, activeItem.status === "failed");
    }
  },
);
</script>

<template>
  <section
    class="upload-queue"
    :class="[`upload-queue--${position}`, { 'upload-queue--compact': compact }]"
    :aria-label="ariaLabel"
  >
    <header class="upload-queue__header">
      <h3>Uploads</h3>
      <span>{{ items.length }}</span>
    </header>
    <ol class="upload-queue__items">
      <li
        v-for="item in items"
        :key="item.id"
        class="upload-queue__item"
        :class="`upload-queue__item--${item.status}`"
      >
        <div class="upload-queue__copy">
          <strong>{{ item.name }}</strong>
          <span>{{ item.size }} · {{ item.destination || "Current workspace" }}</span>
          <em>{{ itemStatusText(item) }}</em>
        </div>
        <progress :value="item.progress" max="100">{{ item.progress }}%</progress>
        <div class="upload-queue__actions">
          <NButton v-if="item.status === 'failed'" native-type="button" @click="emit('retry', item)"
            >Retry</NButton
          >
          <NButton
            v-if="item.status === 'uploading'"
            native-type="button"
            @click="emit('pause', item)"
            >Pause</NButton
          >
          <NButton
            v-if="item.status === 'paused'"
            native-type="button"
            @click="emit('resume', item)"
            >Resume</NButton
          >
          <NButton
            v-if="item.status === 'completed'"
            native-type="button"
            @click="emit('openDestination', item)"
            >Open</NButton
          >
          <NButton
            v-if="item.status !== 'completed'"
            native-type="button"
            @click="emit('cancel', item)"
            >Cancel</NButton
          >
          <NButton native-type="button" @click="emit('remove', item)">Remove</NButton>
        </div>
      </li>
    </ol>
  </section>
</template>

<style scoped>
.upload-queue {
  display: grid;
  gap: 12px;
  width: min(420px, 100%);
  padding: 14px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-md);
  background: var(--mm-surface);
  box-shadow: var(--mm-shadow-sm);
}

.upload-queue--floating {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 40;
}

.upload-queue__header,
.upload-queue__item,
.upload-queue__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload-queue__header {
  justify-content: space-between;
}

.upload-queue__header h3 {
  margin: 0;
  font-size: 15px;
}

.upload-queue__items {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.upload-queue__item {
  align-items: stretch;
  flex-direction: column;
  padding: 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface-soft);
}

.upload-queue__copy {
  display: grid;
  gap: 3px;
}

.upload-queue__copy span,
.upload-queue__copy em {
  color: var(--mm-text-secondary);
  font-size: 12px;
  font-style: normal;
}

.upload-queue progress {
  width: 100%;
  accent-color: var(--mm-brand-primary);
}

.upload-queue__actions {
  flex-wrap: wrap;
}

.upload-queue__actions button {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface);
  color: var(--mm-brand-primary);
  font-size: 12px;
  font-weight: 800;
}

.upload-queue__item--failed {
  border-color: color-mix(in srgb, var(--mm-danger) 28%, var(--mm-border));
}

.upload-queue--compact {
  width: 320px;
}
</style>
