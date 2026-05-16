<script lang="ts" setup>
/**
 * 五态包装器：根据状态自动渲染 loading / error / empty / success 之一。
 *
 * 使用示例：
 *   <PageStateWrapper :loading="loading" :error="error" :empty="!list.length" @retry="reload">
 *     <ul>...</ul>
 *   </PageStateWrapper>
 *
 * spec docs/v212-migration-spec.md §22.2 五态规范统一入口。
 */
import EmptyState from './EmptyState.vue';
import ErrorState from './ErrorState.vue';
import LoadingState from './LoadingState.vue';

defineOptions({ name: 'PageStateWrapper' });

interface Props {
  loading?: boolean;
  /** 任意 truthy 值表示有错（Error 实例 / 错误码 / 字符串） */
  error?: unknown;
  /** 错误码（透传 ErrorState） */
  errorCode?: number | string;
  empty?: boolean;
  /** 紧凑模式（透传所有子组件） */
  compact?: boolean;
  /** loading 模式 */
  loadingMode?: 'spin' | 'skeleton';
  /** 空态描述 */
  emptyDescription?: string;
  /** 空态图标 */
  emptyIcon?: string;
  /** 错误是否可重试 */
  retryable?: boolean;
}

withDefaults(defineProps<Props>(), {
  loading: false,
  error: undefined,
  errorCode: '',
  empty: false,
  compact: false,
  loadingMode: 'spin',
  emptyDescription: '',
  emptyIcon: '',
  retryable: true
});

defineEmits<{
  retry: [];
}>();
</script>

<template>
  <LoadingState v-if="loading" :mode="loadingMode" :compact="compact" />
  <ErrorState v-else-if="error" :code="errorCode" :retryable="retryable" :compact="compact" @retry="$emit('retry')">
    <template v-if="$slots.errorAction" #action>
      <slot name="errorAction" />
    </template>
  </ErrorState>
  <EmptyState v-else-if="empty" :description="emptyDescription" :icon="emptyIcon" :compact="compact">
    <template v-if="$slots.emptyAction" #action>
      <slot name="emptyAction" />
    </template>
  </EmptyState>
  <slot v-else />
</template>
