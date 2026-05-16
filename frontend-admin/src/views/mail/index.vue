<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { VueDraggable } from 'vue-draggable-plus';
import {
  NButton,
  NCard,
  NDataTable,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NResult,
  NSpace,
  NTag
} from 'naive-ui';
import type { DataTableRowKey } from 'naive-ui';
import { LoadingState } from '@/components/feedback';
import {
  bulkActionMailMessages,
  listMailLabels,
  listMailFolders,
  listMailMessages,
  moveMailMessagesToFolder,
  readMailMessage,
  saveMailDraft,
  sendMailMessage,
  updateMailLabels
} from '@/service/api';
import { $t } from '@/locales';
import MailExternalAccountsPanel from './external/MailExternalAccountsPanel.vue';
import MailRulesPanel from './rules/MailRulesPanel.vue';

defineOptions({
  name: 'Mail'
});

interface MailDragEvent {
  item?: HTMLElement;
}

const MAIL_DRAG_FOLDERS = new Set(['archive', 'inbox', 'spam', 'trash']);

const route = useRoute();
const loading = ref(false);
const detailLoading = ref(false);
const composeOpen = ref(false);
const activeFolder = ref('inbox');
const folders = ref<Api.Mail.Folder[]>([]);
const labels = ref<Api.Mail.Label[]>([]);
const messages = ref<Api.Mail.MessageSummary[]>([]);
const selectedMessage = ref<Api.Mail.MessageDetail | null>(null);
const selectedRowKeys = ref<DataTableRowKey[]>([]);
const folderDropBuckets = reactive<Record<string, Api.Mail.MessageSummary[]>>({});
const labelDropBuckets = reactive<Record<string, Api.Mail.MessageSummary[]>>({});

const composeModel = reactive<Api.Mail.SendPayload>({
  toEmail: '',
  subject: '',
  body: ''
});

const folderOptions = computed(() =>
  folders.value.map(folder => ({
    key: folder.key,
    label: `${folder.label}${folder.unreadCount ? ` (${folder.unreadCount})` : ''}`
  }))
);
const draggableFolderOptions = computed(() => folderOptions.value.filter(folder => MAIL_DRAG_FOLDERS.has(folder.key)));
const draggableMessages = computed({
  get: () => messages.value,
  set: value => {
    messages.value = value;
  }
});

const columns = computed(() => [
  { type: 'selection' as const },
  { title: $t('page.mail.sender'), key: 'senderDisplayName' },
  { title: $t('page.mail.subject'), key: 'subject' },
  { title: $t('page.mail.preview'), key: 'preview' },
  { title: $t('page.mail.time'), key: 'sentAt' }
]);

function rowKey(row: Api.Mail.MessageSummary) {
  return row.id;
}

function createRowProps(row: Api.Mail.MessageSummary) {
  return {
    onClick: () => openMessage(row.id)
  };
}

function resolveRouteFolder() {
  const routeFolder = route.params.folder;

  if (Array.isArray(routeFolder)) {
    return routeFolder[0] || '';
  }

  return routeFolder || '';
}

function applyRouteFolder() {
  const routeFolder = resolveRouteFolder();

  if (routeFolder) {
    activeFolder.value = routeFolder;
  }
}

async function loadFolders() {
  const { data, error } = await listMailFolders();

  if (!error) {
    folders.value = data;
    ensureFolderDropBuckets();
  }
}

async function loadLabels() {
  const { data, error } = await listMailLabels();

  if (!error) {
    labels.value = data;
    ensureLabelDropBuckets();
  }
}

async function loadMessages() {
  loading.value = true;
  const { data, error } = await listMailMessages({ folder: activeFolder.value, page: 1, size: 50 });

  if (!error) {
    messages.value = data.items;
  }

  loading.value = false;
}

async function openMessage(messageId: string) {
  detailLoading.value = true;
  const { data, error } = await readMailMessage(messageId);

  if (!error) {
    selectedMessage.value = data;
  }

  detailLoading.value = false;
}

async function submitCompose() {
  const { error } = await sendMailMessage(composeModel);

  if (!error) {
    composeOpen.value = false;
    window.$message?.success($t('common.updateSuccess'));
    await loadMessages();
  }
}

async function submitDraft() {
  const { error } = await saveMailDraft(composeModel);

  if (!error) {
    composeOpen.value = false;
    window.$message?.success($t('common.updateSuccess'));
    await loadMessages();
  }
}

async function runBulkAction(action: string) {
  const messageIds = selectedRowKeys.value.map(String);

  if (!messageIds.length) {
    return;
  }

  const { error } = await bulkActionMailMessages({ action, messageIds });

  if (!error) {
    selectedRowKeys.value = [];
    await loadMessages();
  }
}

async function handleFolderDrop(folderKey: string, event: MailDragEvent) {
  const messageIds = resolveDraggedMessageIds(event);
  folderDropBuckets[folderKey] = [];

  if (!messageIds.length) return;

  const { error } = await moveMailMessagesToFolder(messageIds, folderKey);

  if (!error) {
    selectedRowKeys.value = [];
    window.$message?.success($t('common.updateSuccess'));
    await loadFolders();
    await loadMessages();
  }
}

async function handleLabelDrop(label: Api.Mail.Label, event: MailDragEvent) {
  const messageIds = resolveDraggedMessageIds(event);
  labelDropBuckets[label.name] = [];

  if (!messageIds.length) return;

  const results = await Promise.all(
    messageIds.map(messageId => updateMailLabels(messageId, { labels: nextLabels(messageId, label.name) }))
  );

  if (results.some(result => result.error)) {
    window.$message?.error($t('common.error'));
    await loadMessages();
    return;
  }

  selectedRowKeys.value = [];
  window.$message?.success($t('common.updateSuccess'));
  await loadMessages();
}

async function switchFolder(folderKey: string) {
  activeFolder.value = folderKey;
  selectedMessage.value = null;
  await loadMessages();
}

function ensureFolderDropBuckets() {
  draggableFolderOptions.value.forEach(folder => {
    folderDropBuckets[folder.key] = folderDropBuckets[folder.key] || [];
  });
}

function ensureLabelDropBuckets() {
  labels.value.forEach(label => {
    labelDropBuckets[label.name] = labelDropBuckets[label.name] || [];
  });
}

function resolveDraggedMessageIds(event: MailDragEvent) {
  const fallbackId = event.item?.dataset.messageId;
  const selectedIds = selectedRowKeys.value.map(String);

  if (fallbackId && selectedIds.includes(fallbackId)) {
    return selectedIds;
  }

  return fallbackId ? [fallbackId] : selectedIds;
}

function nextLabels(messageId: string, labelName: string) {
  const message = messages.value.find(item => item.id === messageId);
  return Array.from(new Set([...(message?.labels || []), labelName]));
}

function isSelected(messageId: string) {
  return selectedRowKeys.value.map(String).includes(messageId);
}

onMounted(async () => {
  applyRouteFolder();
  await loadFolders();
  await loadLabels();
  await loadMessages();
});
</script>

<template>
  <div class="mail-page">
    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 m:5">
        <NSpace vertical :size="16">
          <NCard class="card-wrapper" :title="$t('route.mail')">
            <NSpace vertical>
              <NButton type="primary" block @click="composeOpen = true">{{ $t('page.mail.compose') }}</NButton>
              <template v-for="folder in folderOptions" :key="folder.key">
                <VueDraggable
                  v-if="MAIL_DRAG_FOLDERS.has(folder.key)"
                  v-model="folderDropBuckets[folder.key]"
                  group="mail-drag"
                  :sort="false"
                  class="min-h-34px rounded-6px border border-transparent hover:border-primary"
                  @add="handleFolderDrop(folder.key, $event)"
                >
                  <NButton
                    block
                    :type="activeFolder === folder.key ? 'primary' : 'default'"
                    @click="switchFolder(folder.key)"
                  >
                    {{ folder.label }}
                  </NButton>
                </VueDraggable>
                <NButton
                  v-else
                  block
                  :type="activeFolder === folder.key ? 'primary' : 'default'"
                  @click="switchFolder(folder.key)"
                >
                  {{ folder.label }}
                </NButton>
              </template>
            </NSpace>
          </NCard>
          <NCard class="card-wrapper" :title="$t('page.mailFilters.labels')">
            <NSpace vertical>
              <VueDraggable
                v-for="label in labels"
                :key="label.id"
                v-model="labelDropBuckets[label.name]"
                group="mail-drag"
                :sort="false"
                class="min-h-30px rounded-6px border border-transparent hover:border-primary"
                @add="handleLabelDrop(label, $event)"
              >
                <NTag :bordered="false">{{ label.name }}</NTag>
              </VueDraggable>
            </NSpace>
          </NCard>
          <MailExternalAccountsPanel @synced="loadMessages" />
        </NSpace>
      </NGi>

      <NGi span="24 m:11">
        <NCard class="card-wrapper" :title="$t('page.mail.subject')">
          <NSpace class="mb-12px" justify="space-between">
            <NTag>{{ $t('page.mail.selectedCount', { count: selectedRowKeys.length }) }}</NTag>
            <NSpace>
              <NButton @click="runBulkAction('MARK_READ')">{{ $t('page.mail.bulkMarkRead') }}</NButton>
              <NButton type="error" @click="runBulkAction('DELETE')">{{ $t('page.mail.bulkDelete') }}</NButton>
            </NSpace>
          </NSpace>
          <VueDraggable
            v-model="draggableMessages"
            :group="{ name: 'mail-drag', pull: 'clone', put: false }"
            item-key="id"
            :sort="false"
            class="mb-12px grid grid-cols-1 gap-8px lg:grid-cols-2"
          >
            <div
              v-for="message in draggableMessages"
              :key="message.id"
              :data-message-id="message.id"
              class="cursor-move rounded-6px border border-[var(--n-border-color)] bg-[var(--n-color)] p-10px"
            >
              <div class="break-words text-13px font-600">{{ message.subject }}</div>
              <div class="mt-6px flex flex-wrap items-center gap-6px">
                <NTag v-if="isSelected(message.id)" size="small" type="success" :bordered="false">
                  {{ selectedRowKeys.length }}
                </NTag>
                <NTag v-for="label in message.labels" :key="label" size="small" :bordered="false">{{ label }}</NTag>
              </div>
            </div>
          </VueDraggable>
          <NDataTable
            v-model:checked-row-keys="selectedRowKeys"
            :columns="columns"
            :data="messages"
            :loading="loading"
            :row-key="rowKey"
            :row-props="createRowProps"
          />
        </NCard>
      </NGi>

      <NGi span="24 m:8">
        <NSpace vertical :size="16">
          <NCard class="card-wrapper" :title="$t('page.mail.reader')">
            <NSpace v-if="selectedMessage" vertical>
              <h3 class="m-0 text-18px">{{ selectedMessage.subject }}</h3>
              <span>{{ selectedMessage.senderDisplayName || selectedMessage.senderEmail }}</span>
              <NTag v-if="selectedMessage.hasAttachments">{{ selectedMessage.attachments.length }}</NTag>
              <p class="whitespace-pre-wrap">{{ selectedMessage.body }}</p>
            </NSpace>
            <LoadingState v-else-if="detailLoading" compact />
            <NResult v-else status="info" :title="$t('page.mail.reader')" />
          </NCard>
          <MailRulesPanel :labels="labels" />
        </NSpace>
      </NGi>
    </NGrid>

    <NDrawer v-model:show="composeOpen" :width="520">
      <NDrawerContent :title="$t('page.mail.compose')" closable>
        <NForm :model="composeModel" label-placement="top">
          <NFormItem path="toEmail" :label="$t('page.mail.to')">
            <NInput v-model:value="composeModel.toEmail" />
          </NFormItem>
          <NFormItem path="subject" :label="$t('page.mail.subject')">
            <NInput v-model:value="composeModel.subject" />
          </NFormItem>
          <NFormItem path="body" :label="$t('page.mail.body')">
            <NInput v-model:value="composeModel.body" type="textarea" :autosize="{ minRows: 8 }" />
          </NFormItem>
          <NSpace justify="end">
            <NButton @click="submitDraft">{{ $t('page.mail.saveDraft') }}</NButton>
            <NButton type="primary" @click="submitCompose">{{ $t('page.mail.send') }}</NButton>
          </NSpace>
        </NForm>
      </NDrawerContent>
    </NDrawer>
  </div>
</template>
