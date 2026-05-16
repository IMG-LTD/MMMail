<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NSpace,
  NSwitch,
  NTag
} from 'naive-ui';
import {
  createMailFilter,
  deleteMailFilter,
  listMailFilters,
  previewMailFilter,
  updateMailFilter
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'MailRulesPanel' });

const props = defineProps<{
  labels: Api.Mail.Label[];
}>();

const filterEditorOpen = ref(false);
const filters = ref<Api.MailFilters.Filter[]>([]);
const filterPreview = ref<Api.MailFilters.Preview | null>(null);
const editingFilterId = ref<string | null>(null);
const filterModel = reactive({
  enabled: true,
  keywordContains: '',
  labels: [] as string[],
  markRead: false,
  name: '',
  senderContains: '',
  subjectContains: '',
  targetFolder: 'inbox'
});
const previewModel = reactive({ body: '', senderEmail: '', subject: '' });

const targetFolderOptions = [
  { label: 'inbox', value: 'inbox' },
  { label: 'archive', value: 'archive' },
  { label: 'spam', value: 'spam' },
  { label: 'trash', value: 'trash' }
];
const labelOptions = computed(() => props.labels.map(label => ({ label: label.name, value: label.name })));
const filterColumns = computed(() => [
  { title: $t('page.mailFilters.name'), key: 'name' },
  { title: $t('page.mailFilters.targetFolder'), key: 'targetFolder' },
  { title: $t('page.mailFilters.labels'), key: 'labels' },
  {
    title: $t('page.notifications.status'),
    key: 'enabled',
    render: (row: Api.MailFilters.Filter) =>
      h(NSwitch, { value: row.enabled, 'onUpdate:value': value => toggleFilter(row, value) })
  },
  {
    title: $t('common.action'),
    key: 'actions',
    render: (row: Api.MailFilters.Filter) =>
      h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', onClick: () => editFilter(row) }, { default: () => $t('common.edit') }),
          h(
            NButton,
            { size: 'small', type: 'error', onClick: () => removeFilter(row) },
            { default: () => $t('common.delete') }
          )
        ]
      })
  }
]);

function resetFilterModel() {
  editingFilterId.value = null;
  filterModel.enabled = true;
  filterModel.keywordContains = '';
  filterModel.labels = [];
  filterModel.markRead = false;
  filterModel.name = '';
  filterModel.senderContains = '';
  filterModel.subjectContains = '';
  filterModel.targetFolder = 'inbox';
}

function filterPayload() {
  return {
    enabled: filterModel.enabled,
    keywordContains: filterModel.keywordContains,
    labels: filterModel.labels,
    markRead: filterModel.markRead,
    name: filterModel.name,
    senderContains: filterModel.senderContains,
    subjectContains: filterModel.subjectContains,
    targetFolder: filterModel.targetFolder
  };
}

function filterRowPayload(row: Api.MailFilters.Filter, enabled: boolean) {
  return {
    enabled,
    keywordContains: row.keywordContains,
    labels: row.labels,
    markRead: row.markRead,
    name: row.name,
    senderContains: row.senderContains,
    subjectContains: row.subjectContains,
    targetFolder: row.targetFolder
  };
}

async function loadFilters() {
  const { data, error } = await listMailFilters();

  if (!error) {
    filters.value = data;
  }
}

function openNewFilter() {
  resetFilterModel();
  filterEditorOpen.value = true;
}

function editFilter(filter: Api.MailFilters.Filter) {
  editingFilterId.value = filter.id;
  filterModel.enabled = filter.enabled;
  filterModel.keywordContains = filter.keywordContains;
  filterModel.labels = [...filter.labels];
  filterModel.markRead = filter.markRead;
  filterModel.name = filter.name;
  filterModel.senderContains = filter.senderContains;
  filterModel.subjectContains = filter.subjectContains;
  filterModel.targetFolder = filter.targetFolder || 'inbox';
  filterEditorOpen.value = true;
}

async function submitFilter() {
  const request = editingFilterId.value
    ? updateMailFilter(editingFilterId.value, filterPayload())
    : createMailFilter(filterPayload());
  const { error } = await request;

  if (!error) {
    filterEditorOpen.value = false;
    resetFilterModel();
    await loadFilters();
  }
}

async function toggleFilter(filter: Api.MailFilters.Filter, enabled: boolean) {
  const { error } = await updateMailFilter(filter.id, filterRowPayload(filter, enabled));

  if (!error) {
    await loadFilters();
  }
}

function removeFilter(filter: Api.MailFilters.Filter) {
  window.$dialog?.warning({
    title: $t('common.confirmDelete'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: () => deleteSelectedFilter(filter.id)
  });
}

async function deleteSelectedFilter(filterId: string) {
  const { error } = await deleteMailFilter(filterId);

  if (!error) {
    await loadFilters();
  }
}

async function runFilterPreview() {
  const { data, error } = await previewMailFilter({ ...previewModel });

  if (!error) {
    filterPreview.value = data;
  }
}

onMounted(loadFilters);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard class="card-wrapper" :title="$t('page.mailFilters.title')">
      <NSpace vertical>
        <NSpace justify="space-between">
          <NButton type="primary" @click="openNewFilter">{{ $t('page.mailFilters.create') }}</NButton>
          <NButton @click="runFilterPreview">{{ $t('page.mailFilters.preview') }}</NButton>
        </NSpace>
        <NDataTable :columns="filterColumns" :data="filters" />
      </NSpace>
    </NCard>

    <NCard class="card-wrapper" :title="$t('page.mailFilters.preview')">
      <NForm :model="previewModel" label-placement="top">
        <NFormItem path="senderEmail" :label="$t('page.mail.sender')">
          <NInput v-model:value="previewModel.senderEmail" />
        </NFormItem>
        <NFormItem path="subject" :label="$t('page.mail.subject')">
          <NInput v-model:value="previewModel.subject" />
        </NFormItem>
        <NFormItem path="body" :label="$t('page.mail.body')">
          <NInput v-model:value="previewModel.body" type="textarea" :autosize="{ minRows: 3 }" />
        </NFormItem>
        <NSpace align="center">
          <NButton @click="runFilterPreview">{{ $t('page.mailFilters.preview') }}</NButton>
          <NTag v-if="filterPreview">{{ filterPreview.effectiveFolder }}</NTag>
        </NSpace>
      </NForm>
    </NCard>
  </NSpace>

  <NDrawer v-model:show="filterEditorOpen" :width="460">
    <NDrawerContent :title="$t('page.mailFilters.title')" closable>
      <NForm :model="filterModel" label-placement="top">
        <NFormItem path="name" :label="$t('page.mailFilters.name')">
          <NInput v-model:value="filterModel.name" />
        </NFormItem>
        <NFormItem path="senderContains" :label="$t('page.mail.sender')">
          <NInput v-model:value="filterModel.senderContains" />
        </NFormItem>
        <NFormItem path="subjectContains" :label="$t('page.mail.subject')">
          <NInput v-model:value="filterModel.subjectContains" />
        </NFormItem>
        <NFormItem path="keywordContains" :label="$t('page.mail.preview')">
          <NInput v-model:value="filterModel.keywordContains" />
        </NFormItem>
        <NFormItem path="targetFolder" :label="$t('page.mailFilters.targetFolder')">
          <NSelect v-model:value="filterModel.targetFolder" :options="targetFolderOptions" />
        </NFormItem>
        <NFormItem path="labels" :label="$t('page.mailFilters.labels')">
          <NSelect v-model:value="filterModel.labels" multiple :options="labelOptions" />
        </NFormItem>
        <NFormItem path="markRead" :label="$t('page.mailFilters.markRead')">
          <NSwitch v-model:value="filterModel.markRead" />
        </NFormItem>
        <NFormItem path="enabled" :label="$t('page.notifications.status')">
          <NSwitch v-model:value="filterModel.enabled" />
        </NFormItem>
        <NSpace justify="end">
          <NButton type="primary" @click="submitFilter">{{ $t('common.confirm') }}</NButton>
        </NSpace>
      </NForm>
    </NDrawerContent>
  </NDrawer>
</template>
