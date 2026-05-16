<script lang="ts" setup>
import { NSkeleton, NSpin } from 'naive-ui';
import { $t } from '@/locales';

defineOptions({ name: 'LoadingState' });

interface Props {
  /** 模式：spin（旋转）/ skeleton（骨架屏） */
  mode?: 'spin' | 'skeleton';
  /** skeleton 行数 */
  rows?: number;
  /** 紧凑模式（行内/列表内） */
  compact?: boolean;
  /** spin 文案；为空时走 page.state.loading.description */
  description?: string;
}

withDefaults(defineProps<Props>(), {
  mode: 'spin',
  rows: 3,
  compact: false,
  description: ''
});
</script>

<template>
  <div
    class="w-full"
    :class="[compact ? 'py-12px' : 'py-32px', mode === 'spin' ? 'flex-col-center gap-8px' : '']"
    role="status"
    aria-live="polite"
    :aria-busy="true"
  >
    <template v-if="mode === 'skeleton'">
      <NSkeleton text :repeat="rows" :sharp="false" />
    </template>
    <template v-else>
      <NSpin :size="compact ? 'small' : 'medium'" />
      <div class="text-12px text-#999">
        {{ description || $t('page.state.loading.description') }}
      </div>
    </template>
  </div>
</template>
