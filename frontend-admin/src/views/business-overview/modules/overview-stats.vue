<script setup lang="ts">
import { computed } from 'vue';
import { NCard, NGi, NGrid, NStatistic, NTag } from 'naive-ui';
import { $t } from '@/locales';

defineOptions({
  name: 'OverviewStats'
});

interface Props {
  data: Api.OrgBusiness.OverviewVo | null;
}

const props = defineProps<Props>();

const storageRatio = computed(() => {
  if (!props.data || props.data.storageLimitBytes <= 0) return 0;
  return Math.min(100, Math.round((props.data.storageBytes / props.data.storageLimitBytes) * 100));
});

function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const exponent = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)));
  const value = bytes / 1024 ** exponent;
  return `${value.toFixed(value >= 100 || exponent === 0 ? 0 : 1)} ${units[exponent]}`;
}
</script>

<template>
  <NCard class="card-wrapper" :title="$t('page.businessOverview.statsTitle')">
    <template #header-extra>
      <NTag v-if="data?.currentRole" type="info" size="small">{{ data.currentRole }}</NTag>
    </template>
    <NGrid v-if="data" :cols="24" :x-gap="16" :y-gap="16">
      <NGi span="24 m:6">
        <NStatistic :label="$t('page.businessOverview.memberCount')" :value="data.memberCount" />
      </NGi>
      <NGi span="24 m:6">
        <NStatistic :label="$t('page.businessOverview.adminCount')" :value="data.adminCount" />
      </NGi>
      <NGi span="24 m:6">
        <NStatistic :label="$t('page.businessOverview.pendingInviteCount')" :value="data.pendingInviteCount" />
      </NGi>
      <NGi span="24 m:6">
        <NStatistic :label="$t('page.businessOverview.teamSpaceCount')" :value="data.teamSpaceCount" />
      </NGi>
      <NGi span="24 m:12">
        <NStatistic :label="$t('page.businessOverview.storage')">
          <span>{{ formatBytes(data.storageBytes) }} / {{ formatBytes(data.storageLimitBytes) }}</span>
          <span class="ml-8px text-12px op-60">({{ storageRatio }}%)</span>
        </NStatistic>
      </NGi>
      <NGi span="24 m:12">
        <NStatistic :label="$t('page.businessOverview.governanceSla')" :value="`${data.governanceReviewSlaHours}h`">
          <template #suffix>
            <NTag v-if="data.requireDualReviewGovernance" type="warning" size="small">
              {{ $t('page.businessOverview.dualReview') }}
            </NTag>
          </template>
        </NStatistic>
      </NGi>
    </NGrid>
  </NCard>
</template>
