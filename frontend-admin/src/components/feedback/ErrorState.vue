<script lang="ts" setup>
import { computed } from 'vue';
import { NButton, NEmpty } from 'naive-ui';
import { $t } from '@/locales';

defineOptions({ name: 'ErrorState' });

interface Props {
  /** 错误码（如 ErrorCode.MAIL_NOT_FOUND.code = 30001），用于 i18n errors.{code}.* */
  code?: number | string;
  /** 自定义错误标题；不传则按 code 走 errors.{code}.title，再退回 page.state.error.title */
  title?: string;
  /** 自定义错误描述 */
  description?: string;
  /** 是否显示重试按钮 */
  retryable?: boolean;
  /** 紧凑布局 */
  compact?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  code: '',
  title: '',
  description: '',
  retryable: true,
  compact: false
});

defineEmits<{
  retry: [];
}>();

const title = computed(() => {
  if (props.title) return props.title;
  if (props.code) {
    const key = `errors.${String(props.code)}.title`;
    const translated = $t(key as App.I18n.I18nKey);
    if (translated && translated !== key) return translated;
  }
  return $t('page.state.error.title');
});

const description = computed(() => {
  if (props.description) return props.description;
  if (props.code) {
    const key = `errors.${String(props.code)}.message`;
    const translated = $t(key as App.I18n.I18nKey);
    if (translated && translated !== key) return translated;
  }
  return $t('page.state.error.description');
});
</script>

<template>
  <div
    class="flex-col-center w-full text-center"
    :class="[compact ? 'py-16px gap-8px' : 'py-48px gap-12px']"
    role="alert"
    aria-live="assertive"
  >
    <NEmpty :description="title">
      <template #icon>
        <SvgIcon local-icon="service-error" />
      </template>
      <template #extra>
        <div class="text-12px text-#999">{{ description }}</div>
        <NButton v-if="retryable" class="mt-12px" type="primary" ghost size="small" @click="$emit('retry')">
          {{ $t('common.refresh') }}
        </NButton>
        <slot name="action" />
      </template>
    </NEmpty>
  </div>
</template>
