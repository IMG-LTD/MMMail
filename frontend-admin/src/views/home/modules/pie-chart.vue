<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { readWorkspaceSummary } from '@/service/api/workspace';
import { $t } from '@/locales';

defineOptions({
  name: 'PieChart'
});

const CARD_VALUE_NUMBER = /-?\d+(?:\.\d+)?/;

interface ProductSlice {
  name: string;
  value: number;
}

const appStore = useAppStore();
const slices = ref<ProductSlice[]>([]);

const { domRef, updateOptions } = useEcharts(() => ({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    bottom: '1%',
    left: 'center',
    itemStyle: {
      borderWidth: 0
    }
  },
  series: [
    {
      color: ['#5da8ff', '#8e9dff', '#fedc69', '#26deca'],
      name: $t('page.workspace.tasks'),
      type: 'pie',
      radius: ['45%', '75%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 1
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '12'
        }
      },
      labelLine: {
        show: false
      },
      data: [] as { name: string; value: number }[]
    }
  ]
}));

async function loadChartData() {
  const { data, error } = await readWorkspaceSummary();

  if (error) {
    throw new Error('Failed to load workspace summary chart data');
  }

  slices.value = data.productCards.map(toProductSlice).filter(slice => slice.value > 0);
  updateChartData();
}

function toProductSlice(card: Api.Workspace.ProductCard) {
  return {
    name: card.label,
    value: parseCardValue(card.value)
  };
}

function parseCardValue(value: string) {
  const match = value.match(CARD_VALUE_NUMBER);
  if (!match) {
    throw new Error(`Invalid workspace product metric value: ${value}`);
  }
  return Number.parseFloat(match[0]);
}

function updateChartData() {
  updateOptions(opts => {
    opts.series[0].data = slices.value;
    return opts;
  });
}

function updateLocale() {
  updateOptions((opts, factory) => {
    const originOpts = factory();

    opts.series[0].name = originOpts.series[0].name;
    opts.series[0].data = slices.value;

    return opts;
  });
}

watch(
  () => appStore.locale,
  () => {
    updateLocale();
  }
);

onMounted(loadChartData);
</script>

<template>
  <NCard :bordered="false" class="card-wrapper">
    <div ref="domRef" class="h-360px overflow-hidden"></div>
  </NCard>
</template>

<style scoped></style>
