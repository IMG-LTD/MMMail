<script setup lang="ts">
import { NButton } from "naive-ui";
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";
import { NDrawer, NDrawerContent } from "naive-ui";
import { useDialogStack } from "@/shared/composables/useDialogStack";

let drawerIdSeed = 0;

const props = withDefaults(
  defineProps<{
    closeLabel?: string;
    description?: string;
    show: boolean;
    size?: number | string;
    title: string;
    tone?: "neutral" | "danger";
  }>(),
  {
    closeLabel: "Close drawer",
    description: undefined,
    size: 420,
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
const drawerId = `mm-drawer-${++drawerIdSeed}`;
const panelRef = ref<HTMLElement | null>(null);
const restoreTarget = ref<HTMLElement | null>(null);
const registered = ref(false);
const titleId = computed(() => `${drawerId}-title`);
const descriptionId = computed(() => `${drawerId}-description`);

function registerStack() {
  if (registered.value) {
    return;
  }
  dialogStack.push({ id: drawerId, kind: "drawer" });
  registered.value = true;
}

function releaseStack() {
  if (!registered.value) {
    return;
  }
  if (dialogStack.top.value?.id === drawerId) {
    dialogStack.pop();
  }
  registered.value = false;
}

function focusPanel() {
  if (typeof document !== "undefined") {
    restoreTarget.value =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
  }
  nextTick(() => panelRef.value?.focus());
}

function restoreFocus() {
  restoreTarget.value?.focus();
  restoreTarget.value = null;
}

function handleAfterLeave() {
  releaseStack();
  restoreFocus();
  emit("afterLeave");
}

function handleClose() {
  emit("update:show", false);
  emit("close");
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
      focusPanel();
      return;
    }
    releaseStack();
  },
  { immediate: true },
);

onBeforeUnmount(releaseStack);
</script>

<template>
  <NDrawer
    :show="show"
    :width="size"
    placement="right"
    @after-leave="handleAfterLeave"
    @update:show="emit('update:show', $event)"
  >
    <NDrawerContent>
      <section
        ref="panelRef"
        class="mm-drawer"
        :class="`mm-drawer--${tone}`"
        role="dialog"
        aria-modal="true"
        :aria-describedby="description ? descriptionId : undefined"
        :aria-labelledby="titleId"
        tabindex="-1"
        @keydown.esc="handleEscape"
      >
        <header class="mm-drawer__header">
          <div>
            <h2 :id="titleId">{{ title }}</h2>
            <p v-if="description" :id="descriptionId">{{ description }}</p>
          </div>
          <NButton
            class="mm-drawer__close"
            native-type="button"
            :aria-label="closeLabel"
            @click="handleClose"
            >×</NButton
          >
        </header>
        <div class="mm-drawer__body">
          <slot />
        </div>
        <footer v-if="$slots.footer || $slots.actions" class="mm-drawer__footer">
          <slot name="footer" />
          <slot name="actions" />
        </footer>
      </section>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped>
.mm-drawer {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 100%;
  color: var(--mm-text-primary);
}

.mm-drawer__header,
.mm-drawer__footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid var(--mm-border);
}

.mm-drawer__footer {
  align-items: center;
  border-top: 1px solid var(--mm-border);
  border-bottom: 0;
}

.mm-drawer__header h2 {
  margin: 0;
  font-size: 18px;
  letter-spacing: 0;
}

.mm-drawer__header p {
  margin: 6px 0 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.mm-drawer__close {
  width: 34px;
  height: 34px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface);
}

.mm-drawer__body {
  min-height: 0;
  overflow: auto;
  padding: 18px;
}

.mm-drawer--danger .mm-drawer__header {
  border-color: color-mix(in srgb, var(--mm-danger) 24%, var(--mm-border));
}
</style>
