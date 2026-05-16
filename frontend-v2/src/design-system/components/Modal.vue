<script setup lang="ts">
import { NButton } from "naive-ui";
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";
import { NModal } from "naive-ui";
import { useDialogStack } from "@/shared/composables/useDialogStack";

let modalIdSeed = 0;

const props = withDefaults(
  defineProps<{
    closeLabel?: string;
    description?: string;
    show: boolean;
    size?: "sm" | "md" | "lg";
    title: string;
    tone?: "neutral" | "danger";
  }>(),
  {
    closeLabel: "Close modal",
    description: undefined,
    size: "md",
    tone: "neutral",
  },
);

const emit = defineEmits<{
  afterLeave: [];
  close: [];
  escape: [];
  "update:show": [value: boolean];
}>();

const dialogStack = useDialogStack();
const modalId = `mm-modal-${++modalIdSeed}`;
const modalRef = ref<HTMLElement | null>(null);
const registered = ref(false);
const restoreTarget = ref<HTMLElement | null>(null);
const titleId = computed(() => `${modalId}-title`);
const descriptionId = computed(() => `${modalId}-description`);

const modalWidth = computed(() => {
  if (props.size === "sm") {
    return "420px";
  }
  if (props.size === "lg") {
    return "760px";
  }
  return "560px";
});

function registerStack() {
  if (!registered.value) {
    dialogStack.push({ id: modalId, kind: "dialog" });
    registered.value = true;
  }
}

function releaseStack() {
  if (registered.value && dialogStack.top.value?.id === modalId) {
    dialogStack.pop();
  }
  registered.value = false;
}

function focusModal() {
  if (typeof document !== "undefined") {
    restoreTarget.value =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
  }
  nextTick(() => modalRef.value?.focus());
}

function handleClose() {
  emit("update:show", false);
  emit("close");
}

function handleAfterLeave() {
  releaseStack();
  restoreTarget.value?.focus();
  restoreTarget.value = null;
  emit("afterLeave");
}

function handleEscape() {
  emit("escape");
  handleClose();
}

watch(
  () => props.show,
  (value) => {
    if (value) {
      registerStack();
      focusModal();
      return;
    }
    releaseStack();
  },
  { immediate: true },
);

onBeforeUnmount(releaseStack);
</script>

<template>
  <NModal :show="show" @after-leave="handleAfterLeave" @update:show="emit('update:show', $event)">
    <section
      ref="modalRef"
      class="mm-modal"
      :class="`mm-modal--${tone}`"
      :style="{ width: modalWidth }"
      role="dialog"
      aria-modal="true"
      :aria-describedby="description ? descriptionId : undefined"
      :aria-labelledby="titleId"
      tabindex="-1"
      @keydown.esc="handleEscape"
    >
      <header class="mm-modal__header">
        <div>
          <h2 :id="titleId">{{ title }}</h2>
          <p v-if="description" :id="descriptionId">{{ description }}</p>
        </div>
        <NButton native-type="button" :aria-label="closeLabel" @click="handleClose">×</NButton>
      </header>
      <div class="mm-modal__body">
        <slot />
      </div>
      <footer v-if="$slots.footer || $slots.actions" class="mm-modal__footer">
        <slot name="footer" />
        <slot name="actions" />
      </footer>
    </section>
  </NModal>
</template>

<style scoped>
.mm-modal {
  max-width: calc(100vw - 32px);
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-lg);
  background: var(--mm-surface);
  box-shadow: var(--mm-shadow-lg);
  color: var(--mm-text-primary);
}

.mm-modal__header,
.mm-modal__footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid var(--mm-border);
}

.mm-modal__footer {
  align-items: center;
  border-top: 1px solid var(--mm-border);
  border-bottom: 0;
}

.mm-modal__header h2 {
  margin: 0;
  font-size: 18px;
  letter-spacing: 0;
}

.mm-modal__header p {
  margin: 6px 0 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.mm-modal__header button {
  width: 34px;
  height: 34px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface);
}

.mm-modal__body {
  max-height: min(68vh, 620px);
  overflow: auto;
  padding: 18px;
}

.mm-modal--danger {
  border-color: color-mix(in srgb, var(--mm-danger) 24%, var(--mm-border));
}
</style>
