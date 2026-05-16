<script setup lang="ts">
import { computed } from "vue";
import { NCard, NNumberAnimation, NProgress, NStatistic, NTag } from "naive-ui";
import { useLocaleText } from "@/locales";
import type { TextLike, Tone } from "../types";

const props = withDefaults(
  defineProps<{
    title: TextLike;
    value: number | string;
    trend?: { direction: "up" | "down" | "flat"; percent?: number };
    status?: Tone;
    loading?: boolean;
  }>(),
  {
    loading: false,
    status: "neutral",
    trend: undefined,
  },
);

const { tr } = useLocaleText();
const numericValue = computed(() => (typeof props.value === "number" ? props.value : null));
const trendType = computed(() => {
  if (props.trend?.direction === "down") {
    return "warning";
  }

  if (props.trend?.direction === "up") {
    return "success";
  }

  return "default";
});
</script>

<template>
  <NCard class="v211-metric-card" :aria-busy="loading">
    <NStatistic :label="tr(title)">
      <template #default>
        <NNumberAnimation v-if="numericValue !== null" :from="0" :to="numericValue" />
        <span v-else>{{ value }}</span>
      </template>
    </NStatistic>
    <NTag v-if="trend" :type="trendType" size="small">
      {{ trend.direction }}{{ trend.percent !== undefined ? ` ${trend.percent}%` : "" }}
    </NTag>
    <NProgress
      v-if="status !== 'neutral'"
      :percentage="numericValue !== null ? Math.min(numericValue, 100) : 0"
      :show-indicator="false"
      :type="'line'"
    />
    <slot name="chart" />
    <slot name="footer" />
  </NCard>
</template>
