<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { NEmpty, NSkeleton } from "naive-ui";
import { useLocaleText } from "@/locales";
import { v211ChartPalette } from "../chart-palette";
import type { ChartKind, V211ChartOption } from "../types";
import { createV211Chart, type ECharts, type EChartsCoreOption } from "./index";

const RESIZE_DELAY_MS = 200;

const props = withDefaults(
  defineProps<{
    option: V211ChartOption;
    height?: number;
    loading?: boolean;
  }>(),
  {
    height: 260,
    loading: false,
  },
);

const { tr } = useLocaleText();
const container = ref<HTMLElement | null>(null);
let chart: ECharts | null = null;
let observer: ResizeObserver | null = null;
let renderVersion = 0;
let resizeTimer = 0;

const chartOption = computed<EChartsCoreOption>(() => {
  return {
    color: [...v211ChartPalette],
    grid:
      props.option.type === "pie" || props.option.type === "gauge"
        ? undefined
        : { containLabel: true },
    legend: { bottom: 0 },
    series: [buildSeries(props.option.type)],
    tooltip: { trigger: props.option.type === "pie" ? "item" : "axis" },
    xAxis:
      props.option.type === "line" || props.option.type === "bar"
        ? { type: "category", data: props.option.data.map((item) => item.name) }
        : undefined,
    yAxis:
      props.option.type === "line" || props.option.type === "bar" ? { type: "value" } : undefined,
  };
});

function buildSeries(type: ChartKind) {
  if (type === "gauge") {
    return { type, data: props.option.data.slice(0, 1) };
  }

  if (type === "line" || type === "bar") {
    return { type, data: props.option.data.map((item) => item.value), smooth: type === "line" };
  }

  return { type, data: props.option.data };
}

async function renderChart() {
  const version = renderVersion + 1;
  renderVersion = version;

  if (!container.value || props.loading || !props.option.data.length) {
    return;
  }

  if (!chart) {
    const nextChart = await createV211Chart(container.value);

    if (version !== renderVersion) {
      nextChart.dispose();
      return;
    }

    chart = nextChart;
  }

  chart.setOption(chartOption.value, true);
}

function scheduleResize() {
  window.clearTimeout(resizeTimer);
  resizeTimer = window.setTimeout(() => chart?.resize(), RESIZE_DELAY_MS);
}

onMounted(() => {
  renderChart();
  observer = new ResizeObserver(scheduleResize);

  if (container.value) {
    observer.observe(container.value);
  }
});

watch(chartOption, renderChart, { deep: true });
watch(() => props.loading, renderChart);

onBeforeUnmount(() => {
  observer?.disconnect();
  window.clearTimeout(resizeTimer);
  chart?.dispose();
  chart = null;
});
</script>

<template>
  <NSkeleton v-if="loading" :style="{ height: `${height}px` }" />
  <NEmpty
    v-else-if="!option.data.length"
    :description="option.title ? tr(option.title) : undefined"
  />
  <div v-else ref="container" class="v211-chart" :style="{ height: `${height}px` }" />
</template>
