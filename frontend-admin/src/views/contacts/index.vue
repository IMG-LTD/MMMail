<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { NButton, NCard, NDataTable, NForm, NFormItem, NGi, NGrid, NInput, NSpace, NSwitch, NTag } from 'naive-ui';
import {
  createContact,
  exportContacts,
  favoriteContact,
  importContactsCsv,
  listContactDuplicates,
  listContactGroups,
  listContacts,
  mergeDuplicateContacts,
  unfavoriteContact
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'Contacts' });

const contacts = ref<Api.Contacts.Contact[]>([]);
const groups = ref<Api.Contacts.Group[]>([]);
const duplicateGroups = ref<Api.Contacts.DuplicateGroup[]>([]);
const importResult = ref<Api.Contacts.ImportResult | null>(null);
const exportPayload = ref('');
const favoriteOnly = ref(false);
const keyword = ref('');
const contactModel = reactive({ displayName: '', email: '', note: '' });
const csvModel = reactive({ content: '', mergeDuplicates: true });

const columns = computed(() => [
  {
    title: '',
    key: 'favorite',
    width: 92,
    render: (row: Api.Contacts.Contact) =>
      h(
        NButton,
        {
          secondary: true,
          size: 'small',
          type: row.isFavorite ? 'warning' : 'default',
          onClick: () => toggleFavorite(row)
        },
        { default: () => (row.isFavorite ? 'On' : 'Off') }
      )
  },
  { title: $t('page.contacts.name'), key: 'displayName' },
  { title: $t('page.contacts.email'), key: 'email' },
  { title: $t('page.contacts.note'), key: 'note' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' }
]);

const duplicateColumns = computed(() => [
  { title: 'Signature', key: 'signature' },
  { title: 'Count', key: 'count' },
  {
    title: $t('common.action'),
    key: 'actions',
    render: (row: Api.Contacts.DuplicateGroup) =>
      h(
        NButton,
        {
          disabled: row.contacts.length < 2,
          size: 'small',
          type: 'primary',
          onClick: () => mergeDuplicateGroup(row)
        },
        { default: () => 'Merge' }
      )
  }
]);

async function loadContacts() {
  const [contactResult, groupResult, duplicateResult] = await Promise.all([
    listContacts({ favoriteOnly: favoriteOnly.value ? 'true' : undefined, keyword: keyword.value }),
    listContactGroups(),
    listContactDuplicates()
  ]);

  if (!contactResult.error) {
    contacts.value = contactResult.data;
  }

  if (!groupResult.error) {
    groups.value = groupResult.data;
  }

  if (!duplicateResult.error) {
    duplicateGroups.value = duplicateResult.data;
  }
}

async function submitContact() {
  const { error } = await createContact({ ...contactModel });

  if (!error) {
    contactModel.displayName = '';
    contactModel.email = '';
    contactModel.note = '';
    await loadContacts();
  }
}

async function toggleFavorite(contact: Api.Contacts.Contact) {
  const previous = contact.isFavorite;
  contact.isFavorite = !previous;
  const { data, error } = previous ? await unfavoriteContact(contact.id) : await favoriteContact(contact.id);

  if (error) {
    contact.isFavorite = previous;
    window.$message?.error($t('common.error'));
    return;
  }

  replaceContact(data);
}

async function submitImportCsv() {
  const { data, error } = await importContactsCsv({ ...csvModel });

  if (!error) {
    importResult.value = data;
    csvModel.content = '';
    await loadContacts();
  }
}

async function runExportContacts() {
  const { data, error } = await exportContacts({ format: 'csv' });

  if (!error) {
    exportPayload.value = data;
  }
}

async function mergeDuplicateGroup(group: Api.Contacts.DuplicateGroup) {
  if (group.contacts.length < 2) return;

  const [primary, ...duplicates] = group.contacts;
  const { error } = await mergeDuplicateContacts({
    primaryContactId: primary.id,
    duplicateContactIds: duplicates.map(contact => contact.id)
  });

  if (!error) {
    await loadContacts();
  }
}

function replaceContact(contact: Api.Contacts.Contact) {
  contacts.value = contacts.value.map(item => (item.id === contact.id ? contact : item));
}

onMounted(loadContacts);
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 l:16">
      <NCard class="card-wrapper" :title="$t('route.contacts')">
        <NSpace class="mb-12px" vertical>
          <NSpace>
            <NInput v-model:value="keyword" class="w-260px max-w-full" clearable @keyup.enter="loadContacts" />
            <NSwitch v-model:value="favoriteOnly" @update:value="loadContacts" />
            <NButton @click="loadContacts">{{ $t('common.refresh') }}</NButton>
            <NButton @click="runExportContacts">CSV</NButton>
          </NSpace>
          <NSpace>
            <NTag v-for="group in groups" :key="group.id">{{ group.name }}: {{ group.memberCount }}</NTag>
          </NSpace>
        </NSpace>
        <NDataTable :columns="columns" :data="contacts" />
      </NCard>
      <NCard class="card-wrapper mt-16px" title="Duplicates">
        <NDataTable :columns="duplicateColumns" :data="duplicateGroups" />
      </NCard>
    </NGi>
    <NGi span="24 l:8">
      <NSpace vertical :size="16">
        <NCard class="card-wrapper" :title="$t('page.contacts.create')">
          <NForm :model="contactModel" label-placement="top">
            <NFormItem path="displayName" :label="$t('page.contacts.name')">
              <NInput v-model:value="contactModel.displayName" />
            </NFormItem>
            <NFormItem path="email" :label="$t('page.contacts.email')">
              <NInput v-model:value="contactModel.email" />
            </NFormItem>
            <NFormItem path="note" :label="$t('page.contacts.note')">
              <NInput v-model:value="contactModel.note" type="textarea" :autosize="{ minRows: 4 }" />
            </NFormItem>
            <NButton type="primary" @click="submitContact">{{ $t('page.contacts.create') }}</NButton>
          </NForm>
        </NCard>
        <NCard class="card-wrapper" title="CSV">
          <NForm :model="csvModel" label-placement="top">
            <NFormItem path="mergeDuplicates" label="Merge duplicates">
              <NSwitch v-model:value="csvModel.mergeDuplicates" />
            </NFormItem>
            <NFormItem path="content" label="CSV content">
              <NInput v-model:value="csvModel.content" type="textarea" :autosize="{ minRows: 6 }" />
            </NFormItem>
            <NButton type="primary" @click="submitImportCsv">Import</NButton>
          </NForm>
          <NSpace v-if="importResult" class="mt-12px">
            <NTag>{{ importResult.totalRows }}</NTag>
            <NTag type="success">{{ importResult.created }}</NTag>
            <NTag type="warning">{{ importResult.updated }}</NTag>
          </NSpace>
          <NInput
            v-if="exportPayload"
            v-model:value="exportPayload"
            class="mt-12px"
            type="textarea"
            :autosize="{ minRows: 5 }"
          />
        </NCard>
      </NSpace>
    </NGi>
  </NGrid>
</template>
