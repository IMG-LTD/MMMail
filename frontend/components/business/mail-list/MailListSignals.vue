<script setup lang="ts">
import type { MailListSignal } from '~/utils/mail-list'
import { useI18n } from '~/composables/useI18n'

const props = withDefaults(defineProps<{
  signals: MailListSignal[]
  compact?: boolean
}>(), {
  compact: false
})

const { t } = useI18n()

function formatSignalLabel(signal: MailListSignal): string {
  if (signal.key === 'messageCount') {
    return t('mailList.signals.messageCount', { count: Number(signal.value ?? 0) })
  }

  const translationKey = signal.key.replace(/:/g, '.')
  return t(`mailList.signals.${translationKey}`)
}
</script>

<template>
  <span v-if="props.signals.length" :class="['signal-group', { compact: props.compact }]">
    <span
      v-for="signal in props.signals"
      :key="`${signal.key}-${signal.value ?? ''}`"
      class="signal-chip"
    >
      {{ formatSignalLabel(signal) }}
    </span>
  </span>
</template>

<style scoped>
.signal-group {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
}

.signal-group.compact {
  gap: 4px;
}

.signal-chip {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-color-primary, #409eff) 10%, white);
  color: var(--el-text-color-regular, #606266);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

.signal-group.compact .signal-chip {
  min-height: 20px;
  padding: 0 7px;
}
</style>
