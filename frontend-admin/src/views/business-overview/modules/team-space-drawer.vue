<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { NButton, NDrawer, NDrawerContent, NList, NListItem, NSpace, NTag, NThing } from 'naive-ui';
import { buildOrgTeamSpaceFileDownloadUrl, listOrgTeamSpaceItems } from '@/service/api/org-business';
import { EmptyState, ErrorState, LoadingState } from '@/components/feedback';
import { $t } from '@/locales';

defineOptions({
  name: 'TeamSpaceDrawer'
});

interface Props {
  orgId: string | number;
  teamSpace: Api.OrgBusiness.TeamSpaceVo | null;
  show: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{ 'update:show': [value: boolean] }>();

const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value)
});

const items = ref<Api.OrgBusiness.TeamSpaceItemVo[]>([]);
const loading = ref(false);
const error = ref<unknown>(null);

async function loadItems() {
  if (!props.teamSpace) return;
  loading.value = true;
  error.value = null;
  const result = await listOrgTeamSpaceItems(props.orgId, props.teamSpace.id, { limit: 200 });
  if (result.error) {
    error.value = result.error;
    items.value = [];
  } else {
    items.value = result.data ?? [];
  }
  loading.value = false;
}

function downloadFile(item: Api.OrgBusiness.TeamSpaceItemVo) {
  if (!props.teamSpace || item.itemType !== 'FILE') return;
  window.open(buildOrgTeamSpaceFileDownloadUrl(props.orgId, props.teamSpace.id, item.id), '_blank');
}

watch(
  () => [props.show, props.teamSpace?.id],
  ([nextShow]) => {
    if (nextShow && props.teamSpace) loadItems();
  }
);
</script>

<template>
  <NDrawer v-model:show="visible" :width="480">
    <NDrawerContent :title="teamSpace?.name || $t('page.businessOverview.teamSpacesTitle')" closable>
      <LoadingState v-if="loading" mode="skeleton" :rows="4" compact />
      <ErrorState v-else-if="error" compact @retry="loadItems" />
      <EmptyState v-else-if="!items.length" compact />
      <NList v-else hoverable clickable>
        <NListItem v-for="item in items" :key="item.id" @click="downloadFile(item)">
          <NThing :title="item.name" :description="item.ownerEmail">
            <template #header-extra>
              <NTag size="small" :type="item.itemType === 'FOLDER' ? 'info' : 'default'">
                {{ item.itemType }}
              </NTag>
            </template>
            <NSpace size="small" :wrap="false" align="center">
              <span class="text-12px op-60">{{ item.updatedAt }}</span>
              <NButton
                v-if="item.itemType === 'FILE'"
                size="tiny"
                type="primary"
                tertiary
                @click.stop="downloadFile(item)"
              >
                {{ $t('page.businessOverview.download') }}
              </NButton>
            </NSpace>
          </NThing>
        </NListItem>
      </NList>
    </NDrawerContent>
  </NDrawer>
</template>
