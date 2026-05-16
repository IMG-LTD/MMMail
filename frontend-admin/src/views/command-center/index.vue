<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui';
import { h, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  NButton,
  NCard,
  NDataTable,
  NDescriptions,
  NDescriptionsItem,
  NGi,
  NGrid,
  NInput,
  NSpace,
  NTabPane,
  NTabs,
  NTag
} from 'naive-ui';
import {
  listCommandCenterCommands,
  listCommandPanelCatalog,
  listCommandPanelRecents,
  pinCommandPanelCommand,
  quickSearchCommandPanel,
  readCommandCenterCommand
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'CommandCenter' });

const DEFAULT_LIMIT = 8;

const router = useRouter();
const loading = ref(false);
const searchLoading = ref(false);
const query = ref('');
const catalog = ref<Api.CommandCenter.CatalogItem[]>([]);
const recents = ref<Api.CommandCenter.Recent[]>([]);
const quickResults = ref<Api.CommandCenter.QuickSearchItem[]>([]);
const commands = ref<Api.CommandCenter.Command[]>([]);
const selectedCommand = ref<Api.CommandCenter.Command | null>(null);

const catalogColumns: DataTableColumns<Api.CommandCenter.CatalogItem> = [
  { title: $t('page.commandCenter.title'), key: 'title', minWidth: 180 },
  { title: $t('page.commandCenter.group'), key: 'group', width: 120 },
  { title: $t('page.commandCenter.shortcut'), key: 'shortcut', width: 96 },
  { title: $t('page.commandCenter.route'), key: 'route', render: row => routePathFromAction(row.action) },
  { title: $t('page.commandCenter.pinned'), key: 'pinned', width: 96, render: row => renderPinned(row.pinned) },
  { title: '', key: 'actions', width: 184, render: row => renderCatalogActions(row) }
];

const recentColumns: DataTableColumns<Api.CommandCenter.Recent> = [
  { title: $t('page.commandCenter.title'), key: 'title', minWidth: 180 },
  { title: $t('page.commandCenter.group'), key: 'group', width: 120 },
  { title: $t('page.commandCenter.usageCount'), key: 'usageCount', width: 112 },
  { title: $t('page.commandCenter.route'), key: 'routePath' },
  { title: '', key: 'actions', width: 96, render: row => renderRouteButton(row.routePath) }
];

const quickSearchColumns: DataTableColumns<Api.CommandCenter.QuickSearchItem> = [
  { title: $t('page.commandCenter.title'), key: 'title', minWidth: 180 },
  { title: $t('page.commandCenter.description'), key: 'summary', minWidth: 160 },
  { title: $t('page.commandCenter.source'), key: 'sourceType', width: 120 },
  { title: $t('page.commandCenter.route'), key: 'routePath' },
  { title: '', key: 'actions', width: 96, render: row => renderRouteButton(row.routePath) }
];

const systemColumns: DataTableColumns<Api.CommandCenter.Command> = [
  { title: $t('page.commandCenter.title'), key: 'name', minWidth: 180 },
  { title: $t('page.commandCenter.product'), key: 'product', width: 120 },
  { title: $t('page.commandCenter.enabled'), key: 'enabled', width: 96, render: row => renderPinned(row.enabled) },
  { title: $t('page.commandCenter.description'), key: 'description', minWidth: 180 }
];

function routePathFromAction(action: Api.CommandCenter.CommandAction) {
  const routePath = action.payload.routePath;
  return typeof routePath === 'string' ? routePath : '';
}

function renderPinned(active: boolean) {
  return h(NTag, { type: active ? 'success' : 'default', bordered: false }, { default: () => String(active) });
}

function renderRouteButton(routePath: string) {
  return h(NButton, { size: 'small', type: 'primary', disabled: !routePath, onClick: () => pushRoute(routePath) }, () =>
    $t('page.commandCenter.execute')
  );
}

function renderCatalogActions(row: Api.CommandCenter.CatalogItem) {
  return h(NSpace, { size: 8 }, () => [
    h(NButton, { size: 'small', tertiary: true, onClick: () => togglePin(row) }, () =>
      row.pinned ? $t('page.commandCenter.unpin') : $t('page.commandCenter.pin')
    ),
    renderRouteButton(routePathFromAction(row.action))
  ]);
}

async function loadPanel() {
  loading.value = true;
  const [catalogResult, recentResult, commandResult] = await Promise.all([
    listCommandPanelCatalog({ context: router.currentRoute.value.path }),
    listCommandPanelRecents({ limit: DEFAULT_LIMIT }),
    listCommandCenterCommands()
  ]);

  if (!catalogResult.error) catalog.value = catalogResult.data;
  if (!recentResult.error) recents.value = recentResult.data;
  if (!commandResult.error) commands.value = commandResult.data;

  loading.value = false;
}

async function refreshPanelLists() {
  const [catalogResult, recentResult] = await Promise.all([
    listCommandPanelCatalog({ context: router.currentRoute.value.path }),
    listCommandPanelRecents({ limit: DEFAULT_LIMIT })
  ]);

  if (!catalogResult.error) catalog.value = catalogResult.data;
  if (!recentResult.error) recents.value = recentResult.data;
}

async function togglePin(row: Api.CommandCenter.CatalogItem) {
  const { error } = await pinCommandPanelCommand({ commandId: row.id, pinned: !row.pinned });
  if (!error) await refreshPanelLists();
}

async function runQuickSearch() {
  const keyword = query.value.trim();
  if (!keyword) {
    quickResults.value = [];
    return;
  }

  searchLoading.value = true;
  const { data, error } = await quickSearchCommandPanel({ q: keyword, limit: DEFAULT_LIMIT });
  if (!error) quickResults.value = data;
  searchLoading.value = false;
}

async function openCommand(row: Api.CommandCenter.Command) {
  const { data, error } = await readCommandCenterCommand(row.id);
  if (!error) selectedCommand.value = data;
}

function systemRowProps(row: Api.CommandCenter.Command) {
  return { onClick: () => openCommand(row) };
}

function pushRoute(routePath: string) {
  if (routePath) router.push(routePath);
}

onMounted(loadPanel);
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 l:16">
      <NCard class="card-wrapper" :title="$t('route.command-center')">
        <NTabs animated type="line">
          <NTabPane name="catalog" :tab="$t('page.commandCenter.catalog')">
            <NDataTable :columns="catalogColumns" :data="catalog" :loading="loading" />
          </NTabPane>
          <NTabPane name="recents" :tab="$t('page.commandCenter.recents')">
            <NDataTable :columns="recentColumns" :data="recents" :loading="loading" />
          </NTabPane>
          <NTabPane name="system" :tab="$t('page.commandCenter.systemCommands')">
            <NDataTable
              :columns="systemColumns"
              :data="commands"
              :loading="loading"
              :row-key="row => row.id"
              :row-props="systemRowProps"
            />
          </NTabPane>
        </NTabs>
      </NCard>
    </NGi>

    <NGi span="24 l:8">
      <NSpace vertical :size="16">
        <NCard class="card-wrapper" :title="$t('page.commandCenter.search')">
          <NSpace vertical :size="12">
            <NInput
              v-model:value="query"
              clearable
              :placeholder="$t('page.commandCenter.searchPlaceholder')"
              @keyup.enter="runQuickSearch"
            />
            <NButton block type="primary" :loading="searchLoading" @click="runQuickSearch">
              {{ $t('page.commandCenter.search') }}
            </NButton>
            <NDataTable :columns="quickSearchColumns" :data="quickResults" :loading="searchLoading" />
          </NSpace>
        </NCard>

        <NCard class="card-wrapper" :title="$t('page.commandCenter.systemCommands')">
          <NDescriptions v-if="selectedCommand" bordered :column="1">
            <NDescriptionsItem :label="$t('page.commandCenter.title')">{{ selectedCommand.name }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.commandCenter.product')">
              {{ selectedCommand.product }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.commandCenter.enabled')">
              <NTag :type="selectedCommand.enabled ? 'success' : 'default'">{{ selectedCommand.enabled }}</NTag>
            </NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.commandCenter.description')">
              {{ selectedCommand.description }}
            </NDescriptionsItem>
          </NDescriptions>
        </NCard>
      </NSpace>
    </NGi>
  </NGrid>
</template>
