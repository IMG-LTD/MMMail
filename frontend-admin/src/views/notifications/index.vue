<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { NBadge, NButton, NCard, NDataTable, NGi, NGrid, NSpace, NTag } from 'naive-ui';
import type { DataTableRowKey } from 'naive-ui';
import { listNotificationSubscriptions, listNotifications, patchNotification } from '@/service/api';
import { connectNotificationRealtime } from '@/hooks/business/notification-realtime';
import { $t } from '@/locales';

defineOptions({
  name: 'Notifications'
});

const loading = ref(false);
const notifications = ref<Api.Notifications.Notification[]>([]);
const subscriptions = ref<Api.Notifications.Subscription[]>([]);
const selectedRowKeys = ref<DataTableRowKey[]>([]);
let stopNotificationRealtime: (() => void) | null = null;

const unreadCount = computed(() => notifications.value.filter(item => !item.readAt && item.status !== 'READ').length);

const columns = computed(() => [
  { type: 'selection' as const },
  { title: $t('page.notifications.title'), key: 'title' },
  { title: $t('page.notifications.product'), key: 'product' },
  { title: $t('page.notifications.severity'), key: 'severity' },
  { title: $t('page.notifications.status'), key: 'status' },
  { title: $t('page.notifications.createdAt'), key: 'createdAt' }
]);

function rowKey(row: Api.Notifications.Notification) {
  return row.id;
}

function selectedNotificationIds() {
  return selectedRowKeys.value.map(String);
}

async function loadNotifications() {
  loading.value = true;
  const [notificationResult, subscriptionResult] = await Promise.all([
    listNotifications({ includeSnoozed: true, limit: 50 }),
    listNotificationSubscriptions()
  ]);

  if (!notificationResult.error) {
    notifications.value = notificationResult.data;
  }

  if (!subscriptionResult.error) {
    subscriptions.value = subscriptionResult.data;
  }

  loading.value = false;
}

async function patchSelected(status: string) {
  const ids = selectedNotificationIds();

  if (!ids.length) {
    return;
  }

  await Promise.all(ids.map(id => patchNotification(id, { status })));
  selectedRowKeys.value = [];
  await loadNotifications();
}

function applyRealtimeEvent(event: Api.Notifications.RealtimeEvent) {
  if (event.type === 'notification') {
    window.$notification?.info({
      title: event.payload.eventType,
      content: event.payload.operation,
      duration: 3500
    });
  }
  void loadNotifications();
}

onMounted(() => {
  void loadNotifications();
  stopNotificationRealtime = connectNotificationRealtime({
    onEvent: applyRealtimeEvent,
    onError: error => window.$message?.error(error.message)
  });
});

onBeforeUnmount(() => {
  stopNotificationRealtime?.();
  stopNotificationRealtime = null;
});
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 m:16">
      <NCard class="card-wrapper" :title="$t('route.notifications')">
        <NSpace class="mb-12px" justify="space-between">
          <NBadge :value="unreadCount">
            <NTag>{{ $t('page.notifications.unread') }}</NTag>
          </NBadge>
          <NSpace>
            <NButton @click="patchSelected('READ')">{{ $t('page.notifications.markRead') }}</NButton>
            <NButton @click="patchSelected('ARCHIVED')">{{ $t('page.notifications.archive') }}</NButton>
          </NSpace>
        </NSpace>
        <NDataTable
          v-model:checked-row-keys="selectedRowKeys"
          :columns="columns"
          :data="notifications"
          :loading="loading"
          :row-key="rowKey"
        />
      </NCard>
    </NGi>

    <NGi span="24 m:8">
      <NCard class="card-wrapper" :title="$t('page.notifications.subscriptions')">
        <NSpace vertical>
          <NTag
            v-for="subscription in subscriptions"
            :key="subscription.id"
            :type="subscription.enabled ? 'success' : 'default'"
          >
            {{ subscription.product }} / {{ subscription.channel }}
          </NTag>
        </NSpace>
      </NCard>
    </NGi>
  </NGrid>
</template>
