<script setup lang="ts">
import { computed } from 'vue'
import { lt, type TextLike, useLocaleText } from '@/locales'

type MaturityLevel = 'ga' | 'beta' | 'preview'

const props = withDefaults(
  defineProps<{
    compact?: boolean
    level: MaturityLevel
    text?: TextLike
  }>(),
  {
    compact: false,
    text: undefined
  }
)

const { tr } = useLocaleText()

const defaultLabels: Record<MaturityLevel, TextLike> = {
  ga: lt('正式', '正式', 'GA'),
  beta: lt('Beta', 'Beta', 'Beta'),
  preview: lt('预览', '預覽', 'Preview')
}

const label = computed(() => props.text ?? defaultLabels[props.level])
</script>

<template>
  <span class="maturity-badge" :class="[`maturity-badge--${level}`, { 'maturity-badge--compact': compact }]">
    {{ tr(label) }}
  </span>
</template>

<style scoped>
.maturity-badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 9px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.maturity-badge--compact {
  min-height: 18px;
  padding: 0 7px;
  font-size: 9px;
}

.maturity-badge--ga {
  border-color: color-mix(in srgb, var(--mm-primary) 20%, white);
  background: color-mix(in srgb, var(--mm-primary) 10%, white);
  color: var(--mm-primary);
}

.maturity-badge--beta {
  border-color: color-mix(in srgb, var(--mm-docs) 22%, white);
  background: color-mix(in srgb, var(--mm-docs) 12%, white);
  color: var(--mm-docs);
}

.maturity-badge--preview {
  border-color: color-mix(in srgb, var(--mm-labs) 28%, white);
  background: color-mix(in srgb, var(--mm-labs) 16%, white);
  color: var(--mm-labs);
}
</style>
