<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NButton, NCard, NGi, NGrid, NInput, NList, NListItem, NSelect, NSpace, NTag, NThing } from 'naive-ui';
import { EmptyState } from '@/components/feedback';
import { readSearchFacets, readSearchResults } from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'Search' });

const PAGE_SIZE = 20;
const MODULE_OPTIONS = [
  { label: 'Mail', value: 'mail' },
  { label: 'Docs', value: 'doc' },
  { label: 'Sheets', value: 'sheet' },
  { label: 'Drive', value: 'drive' },
  { label: 'Contacts', value: 'contact' },
  { label: 'Notes', value: 'note' },
  { label: 'Community', value: 'community' }
];

const route = useRoute();
const router = useRouter();

const keyword = ref('');
const selectedTypes = ref<string[]>([]);
const loading = ref(false);
const results = ref<Api.Search.Item[]>([]);
const facets = ref<Api.Search.Facets>({ byType: {} });
const total = ref(0);

const typeParam = computed(() => (selectedTypes.value.length ? selectedTypes.value.join(',') : undefined));
const canSearch = computed(() => keyword.value.trim().length >= 2);

watch(
  () => route.query.q,
  () => syncRouteQuery(),
  { immediate: true }
);

onMounted(loadSearch);

async function loadSearch() {
  if (!canSearch.value) {
    resetResults();
    return;
  }

  loading.value = true;
  const params = queryParams();
  const [resultResponse, facetResponse] = await Promise.all([readSearchResults(params), readSearchFacets(params)]);
  loading.value = false;

  if (!resultResponse.error) {
    results.value = resultResponse.data.items;
    total.value = resultResponse.data.total;
  }

  if (!facetResponse.error) {
    facets.value = facetResponse.data;
  }
}

function queryParams(): Api.Search.QueryParams {
  return {
    q: keyword.value.trim(),
    types: typeParam.value,
    page: 1,
    size: PAGE_SIZE
  };
}

function runSearch() {
  router.replace({ path: '/search', query: { q: keyword.value.trim() } });
  loadSearch();
}

function syncRouteQuery() {
  keyword.value = typeof route.query.q === 'string' ? route.query.q : '';
}

function resetResults() {
  results.value = [];
  facets.value = { byType: {} };
  total.value = 0;
}

function openItem(item: Api.Search.Item) {
  router.push(item.navigation.path);
}

function moduleCount(type: string) {
  return facets.value.byType[type] || 0;
}

function moduleLabel(type: string) {
  return MODULE_OPTIONS.find(item => item.value === type)?.label || type;
}
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 l:7">
      <NCard class="card-wrapper" :title="$t('route.search')">
        <NSpace vertical :size="14">
          <NInput
            v-model:value="keyword"
            clearable
            :placeholder="$t('common.keywordSearch')"
            @keyup.enter="runSearch"
          />
          <NSelect
            v-model:value="selectedTypes"
            multiple
            clearable
            :options="MODULE_OPTIONS"
            @update:value="loadSearch"
          />
          <NButton block type="primary" :loading="loading" @click="runSearch">{{ $t('common.search') }}</NButton>
        </NSpace>
      </NCard>

      <NCard class="card-wrapper mt-16px" title="Facets">
        <NSpace>
          <NTag v-for="item in MODULE_OPTIONS" :key="item.value" :bordered="false">
            {{ item.label }} {{ moduleCount(item.value) }}
          </NTag>
        </NSpace>
      </NCard>
    </NGi>

    <NGi span="24 l:17">
      <NCard class="card-wrapper" :title="`${$t('route.search')} · ${total}`">
        <NList v-if="results.length" clickable hoverable>
          <NListItem v-for="item in results" :key="`${item.moduleType}-${item.resourceId}`" @click="openItem(item)">
            <NThing :title="item.title" :description="item.updatedAt">
              <template #header-extra>
                <NTag size="small">{{ moduleLabel(item.moduleType) }}</NTag>
              </template>
              <p class="m-0 text-13px leading-6 opacity-80">{{ item.snippet }}</p>
            </NThing>
          </NListItem>
        </NList>
        <EmptyState v-else :description="canSearch ? $t('common.noData') : $t('common.keywordSearch')" />
      </NCard>
    </NGi>
  </NGrid>
</template>
