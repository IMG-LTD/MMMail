<script setup lang="ts">
import { computed } from 'vue';
import type { PopoverPlacement } from 'naive-ui';
import { twMerge } from 'tailwind-merge';

defineOptions({
  name: 'ButtonIcon',
  inheritAttrs: false
});

interface Props {
  /** Button class */
  class?: string;
  /** Iconify icon name */
  icon?: string;
  /** Tooltip content */
  tooltipContent?: string;
  /** Accessible label for icon-only buttons */
  label?: string;
  /** Tooltip placement */
  tooltipPlacement?: PopoverPlacement;
  zIndex?: number;
}

const props = withDefaults(defineProps<Props>(), {
  class: '',
  icon: '',
  label: '',
  tooltipContent: '',
  tooltipPlacement: 'bottom',
  zIndex: 98
});

const DEFAULT_CLASS = 'h-[36px] text-icon';
const accessibleLabel = computed(() => props.label || props.tooltipContent || undefined);
</script>

<template>
  <NTooltip :placement="tooltipPlacement" :z-index="zIndex" :disabled="!tooltipContent">
    <template #trigger>
      <NButton quaternary :class="twMerge(DEFAULT_CLASS, props.class)" :aria-label="accessibleLabel" v-bind="$attrs">
        <div class="flex-center gap-8px">
          <slot>
            <SvgIcon :icon="icon" />
          </slot>
        </div>
      </NButton>
    </template>
    {{ tooltipContent }}
  </NTooltip>
</template>

<style scoped></style>
