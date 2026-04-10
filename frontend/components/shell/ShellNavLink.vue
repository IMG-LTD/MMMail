<script setup lang="ts">
import { computed } from 'vue'
import type { DefaultNavMaturityTone } from '~/utils/default-nav-maturity'

interface Props {
  to: string
  label: string
  badgeValue?: number | string
  badgeMax?: number
  maturityLabel?: string | null
  maturityTone?: DefaultNavMaturityTone | null
  dataTestid?: string
}

const props = withDefaults(defineProps<Props>(), {
  badgeValue: undefined,
  badgeMax: 9999,
  maturityLabel: null,
  maturityTone: null,
  dataTestid: undefined
})

const showBadge = computed(() => props.badgeValue !== undefined && props.badgeValue !== null)
const showMaturity = computed(() => Boolean(props.maturityLabel))
</script>

<template>
  <NuxtLink
    :to="to"
    class="nav-item"
    active-class="active"
    :data-testid="dataTestid"
  >
    <span class="nav-item__label">
      <span>{{ label }}</span>
      <span
        v-if="showMaturity"
        class="nav-item__maturity"
        :data-tone="maturityTone || undefined"
        :aria-label="maturityLabel || undefined"
      >
        {{ maturityLabel }}
      </span>
    </span>
    <el-badge v-if="showBadge" :value="badgeValue" :max="badgeMax" class="badge" />
  </NuxtLink>
</template>

<style scoped>
.nav-item__label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.nav-item__maturity {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  border: 1px solid rgba(180, 83, 9, 0.22);
  background: rgba(245, 158, 11, 0.12);
  color: #9a3412;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
}

.nav-item__maturity[data-tone='preview'] {
  border-color: rgba(8, 145, 178, 0.2);
  background: rgba(6, 182, 212, 0.1);
  color: #155e75;
}
</style>
