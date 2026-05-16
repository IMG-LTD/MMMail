<script setup lang="ts">
import V211Chart from "./V211Chart.vue";
import type { ChartKind, ChartDatum } from "../types";

withDefaults(
  defineProps<{
    type: ChartKind;
    data: readonly ChartDatum[];
    height?: number;
    loading?: boolean;
  }>(),
  {
    height: 96,
    loading: false,
  },
);
</script>

<template>
  <V211Chart :height="height" :loading="loading" :option="{ type, data }" />
</template>
