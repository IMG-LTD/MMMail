<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useAppStore } from '@/store/modules/app';
import { useEcharts } from '@/hooks/common/echarts';
import { listWorkspaceActivity, listWorkspaceTasks } from '@/service/api/workspace';
import { $t } from '@/locales';

defineOptions({
  name: 'LineChart'
});

const MAX_CHART_BUCKETS = 10;
const TIME_FORMATTER = new Intl.DateTimeFormat(undefined, {
  hour: '2-digit',
  minute: '2-digit'
});

interface WorkspaceChartBucket {
  label: string;
  activityCount: number;
  taskCount: number;
}

const appStore = useAppStore();
const buckets = ref<WorkspaceChartBucket[]>([]);

const { domRef, updateOptions } = useEcharts(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
      label: {
        backgroundColor: '#6a7985'
      }
    }
  },
  legend: {
    data: [$t('page.workspace.activity'), $t('page.workspace.tasks')],
    top: '0'
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    top: '15%'
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: [] as string[]
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      color: '#8e9dff',
      name: $t('page.workspace.activity'),
      type: 'line',
      smooth: true,
      stack: 'Total',
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            {
              offset: 0.25,
              color: '#8e9dff'
            },
            {
              offset: 1,
              color: '#fff'
            }
          ]
        }
      },
      emphasis: {
        focus: 'series'
      },
      data: [] as number[]
    },
    {
      color: '#26deca',
      name: $t('page.workspace.tasks'),
      type: 'line',
      smooth: true,
      stack: 'Total',
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            {
              offset: 0.25,
              color: '#26deca'
            },
            {
              offset: 1,
              color: '#fff'
            }
          ]
        }
      },
      emphasis: {
        focus: 'series'
      },
      data: []
    }
  ]
}));

async function loadChartData() {
  const [activityResult, tasksResult] = await Promise.all([listWorkspaceActivity(), listWorkspaceTasks()]);

  if (activityResult.error) {
    throw new Error('Failed to load workspace activity chart data');
  }
  if (tasksResult.error) {
    throw new Error('Failed to load workspace task chart data');
  }

  buckets.value = buildBuckets(activityResult.data, tasksResult.data);
  updateChartData();
}

function buildBuckets(activity: Api.Workspace.ActivityItem[], tasks: Api.Workspace.Task[]) {
  const bucketMap = new Map<string, WorkspaceChartBucket>();
  activity.forEach(item => addBucketCount(bucketMap, item.occurredAt, 'activityCount'));
  tasks.forEach(task => addBucketCount(bucketMap, task.dueAt, 'taskCount'));
  return Array.from(bucketMap.values()).slice(-MAX_CHART_BUCKETS);
}

function addBucketCount(
  bucketMap: Map<string, WorkspaceChartBucket>,
  timestamp: string | null | undefined,
  field: 'activityCount' | 'taskCount'
) {
  if (!timestamp) {
    return;
  }
  const label = toBucketLabel(timestamp);
  const bucket = bucketMap.get(label) ?? { label, activityCount: 0, taskCount: 0 };
  bucket[field] += 1;
  bucketMap.set(label, bucket);
}

function toBucketLabel(timestamp: string) {
  const date = new Date(timestamp);
  if (Number.isNaN(date.getTime())) {
    throw new Error(`Invalid workspace chart timestamp: ${timestamp}`);
  }
  return TIME_FORMATTER.format(date);
}

function updateChartData() {
  updateOptions(opts => {
    opts.xAxis.data = buckets.value.map(bucket => bucket.label);
    opts.series[0].data = buckets.value.map(bucket => bucket.activityCount);
    opts.series[1].data = buckets.value.map(bucket => bucket.taskCount);
    return opts;
  });
}

function updateLocale() {
  updateOptions((opts, factory) => {
    const originOpts = factory();

    opts.legend.data = originOpts.legend.data;
    opts.series[0].name = originOpts.series[0].name;
    opts.series[1].name = originOpts.series[1].name;

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
