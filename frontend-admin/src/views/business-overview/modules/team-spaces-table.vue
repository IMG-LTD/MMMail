<script setup lang="ts">
import { computed, h } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import { NButton, NCard, NDataTable, NTag } from 'naive-ui';
import { $t } from '@/locales';

defineOptions({
  name: 'TeamSpacesTable'
});

interface Props {
  rows: Api.OrgBusiness.TeamSpaceVo[];
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
});

const emit = defineEmits<{
  open: [row: Api.OrgBusiness.TeamSpaceVo];
}>();

function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const exponent = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)));
  const value = bytes / 1024 ** exponent;
  return `${value.toFixed(value >= 100 || exponent === 0 ? 0 : 1)} ${units[exponent]}`;
}

const columns = computed<DataTableColumns<Api.OrgBusiness.TeamSpaceVo>>(() => [
  { title: $t('page.businessOverview.column.name'), key: 'name', minWidth: 180 },
  {
    title: $t('page.businessOverview.column.role'),
    key: 'currentAccessRole',
    width: 140,
    render: row => h(NTag, { size: 'small', type: row.canManage ? 'success' : 'default' }, () => row.currentAccessRole)
  },
  { title: $t('page.businessOverview.column.itemCount'), key: 'itemCount', width: 110 },
  {
    title: $t('page.businessOverview.column.storage'),
    key: 'storageBytes',
    width: 200,
    render: row => `${formatBytes(row.storageBytes)} / ${formatBytes(row.storageLimitBytes)}`
  },
  { title: $t('page.businessOverview.column.updatedAt'), key: 'updatedAt', width: 180 },
  {
    title: $t('common.action'),
    key: 'action',
    width: 140,
    render: row =>
      h(NButton, { size: 'small', type: 'primary', tertiary: true, onClick: () => emit('open', row) }, () =>
        $t('page.businessOverview.openSpace')
      )
  }
]);
</script>

<template>
  <NCard class="card-wrapper" :title="$t('page.businessOverview.teamSpacesTitle')">
    <NDataTable
      :columns="columns"
      :data="props.rows"
      :loading="props.loading"
      :row-key="row => row.id"
      :bordered="false"
      :pagination="{ pageSize: 10 }"
    />
  </NCard>
</template>
