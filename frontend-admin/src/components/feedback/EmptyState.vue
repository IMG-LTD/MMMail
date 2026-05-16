<script lang="ts" setup>
import { computed } from 'vue';
import { NEmpty } from 'naive-ui';
import { $t } from '@/locales';

defineOptions({ name: 'EmptyState' });

interface Props {
  /** 自定义描述文案；不传则回退 page.state.empty.description */
  description?: string;
  /** 自定义图标（local svg 名）；不传走 NEmpty 默认 */
  icon?: string;
  /** 紧凑布局（小区域内） */
  compact?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  description: '',
  icon: '',
  compact: false
});

const description = computed(() => props.description || $t('page.state.empty.description'));
</script>

<template>
  <div
    class="flex-col-center w-full"
    :class="[compact ? 'py-16px gap-8px' : 'py-48px gap-12px']"
    role="status"
    aria-live="polite"
  >
    <NEmpty :description="description">
      <template v-if="icon" #icon>
        <SvgIcon :local-icon="icon" />
      </template>
      <template v-if="$slots.action" #extra>
        <slot name="action" />
      </template>
    </NEmpty>
  </div>
</template>
