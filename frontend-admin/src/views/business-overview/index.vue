<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { NSpace } from 'naive-ui';
import { listOrgTeamSpaces, readOrgBusinessOverview } from '@/service/api/org-business';
import EntitlementGate from '@/components/access/EntitlementGate.vue';
import { PageStateWrapper } from '@/components/feedback';
import { useAuthStore } from '@/store/modules/auth';
import OverviewStats from './modules/overview-stats.vue';
import TeamSpacesTable from './modules/team-spaces-table.vue';
import TeamSpaceDrawer from './modules/team-space-drawer.vue';

defineOptions({
  name: 'BusinessOverview'
});

const authStore = useAuthStore();
const orgId = computed(() => authStore.currentOrgId);

const overview = ref<Api.OrgBusiness.OverviewVo | null>(null);
const teamSpaces = ref<Api.OrgBusiness.TeamSpaceVo[]>([]);
const loading = ref(false);
const error = ref<unknown>(null);

const drawerOpen = ref(false);
const selectedTeamSpace = ref<Api.OrgBusiness.TeamSpaceVo | null>(null);

const isEmpty = computed(() => !loading.value && !error.value && !overview.value && teamSpaces.value.length === 0);

async function loadAll() {
  if (!orgId.value) return;
  loading.value = true;
  error.value = null;
  const [overviewResult, teamSpaceResult] = await Promise.all([
    readOrgBusinessOverview(orgId.value),
    listOrgTeamSpaces(orgId.value)
  ]);
  if (overviewResult.error || teamSpaceResult.error) {
    error.value = overviewResult.error ?? teamSpaceResult.error;
    overview.value = null;
    teamSpaces.value = [];
  } else {
    overview.value = overviewResult.data ?? null;
    teamSpaces.value = teamSpaceResult.data ?? [];
  }
  loading.value = false;
}

function openTeamSpace(row: Api.OrgBusiness.TeamSpaceVo) {
  selectedTeamSpace.value = row;
  drawerOpen.value = true;
}

onMounted(loadAll);
</script>

<template>
  <EntitlementGate :requires="['BUSINESS']" :org-required="true" fallback="upgrade">
    <PageStateWrapper :loading="loading" :error="error" :empty="isEmpty" loading-mode="skeleton" @retry="loadAll">
      <NSpace vertical :size="16">
        <OverviewStats :data="overview" />
        <TeamSpacesTable :rows="teamSpaces" :loading="loading" @open="openTeamSpace" />
      </NSpace>
    </PageStateWrapper>
    <TeamSpaceDrawer v-model:show="drawerOpen" :org-id="orgId" :team-space="selectedTeamSpace" />
  </EntitlementGate>
</template>
