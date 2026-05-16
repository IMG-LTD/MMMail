<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NInput,
  NSpace,
  NTag
} from 'naive-ui';
import type { DataTableRowKey } from 'naive-ui';
import {
  createCalendarEvent,
  createCalendarSubscription,
  deleteCalendarEvent,
  exportCalendarIcs,
  listCalendarEvents,
  listCalendarSubscriptions,
  queryCalendarAvailability,
  readCalendarSettings,
  syncCalendarSubscription
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({
  name: 'Calendar'
});

const ONE_HOUR = 60 * 60 * 1000;
const loading = ref(false);
const drawerOpen = ref(false);
const events = ref<Api.Calendar.Event[]>([]);
const calendarSubscriptions = ref<Api.Calendar.Subscription[]>([]);
const settings = ref<Api.Calendar.Settings | null>(null);
const availability = ref<Api.Calendar.Availability | null>(null);
const selectedRowKeys = ref<DataTableRowKey[]>([]);
const selectedSubscriptionKeys = ref<DataTableRowKey[]>([]);

const eventModel = reactive({
  title: '',
  location: '',
  rrule: '',
  startAt: Date.now(),
  endAt: Date.now() + ONE_HOUR,
  attendees: ''
});

const subscriptionModel = reactive({
  label: '',
  url: '',
  color: '#2f7dd1'
});

const columns = computed(() => [
  { type: 'selection' as const },
  { title: $t('page.calendar.title'), key: 'title' },
  { title: $t('page.calendar.location'), key: 'location' },
  { title: $t('page.calendar.startAt'), key: 'startAt' },
  { title: $t('page.calendar.endAt'), key: 'endAt' }
]);

const subscriptionColumns = computed(() => [
  { type: 'selection' as const },
  { title: $t('page.calendar.subscriptionLabel'), key: 'label' },
  { title: $t('page.calendar.sourceUrl'), key: 'url' },
  { title: $t('page.calendar.status'), key: 'syncStatus' },
  { title: $t('page.calendar.updatedAt'), key: 'updatedAt' }
]);

const availabilityType = computed(() => (availability.value?.summary.hasConflicts ? 'warning' : 'success'));
const availabilityText = computed(() =>
  availability.value?.summary.hasConflicts ? $t('page.calendar.conflict') : $t('page.calendar.noConflict')
);

function rowKey(row: Api.Calendar.Event) {
  return row.id;
}

function subscriptionRowKey(row: Api.Calendar.Subscription) {
  return row.id;
}

function attendeeEmails() {
  return eventModel.attendees
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
}

function toIso(value: number) {
  return new Date(value).toISOString();
}

function createPayload(): Api.Calendar.EventPayload {
  return {
    attendees: attendeeEmails().map(email => ({ email })),
    endAt: toIso(eventModel.endAt),
    location: eventModel.location,
    rrule: eventModel.rrule,
    startAt: toIso(eventModel.startAt),
    timezone: settings.value?.defaultTimezone || Intl.DateTimeFormat().resolvedOptions().timeZone,
    title: eventModel.title
  };
}

async function loadEvents() {
  loading.value = true;
  const { data, error } = await listCalendarEvents();

  if (!error) {
    events.value = data;
  }

  loading.value = false;
}

async function loadSettings() {
  const { data, error } = await readCalendarSettings();

  if (!error) {
    settings.value = data;
  }
}

async function loadCalendarSubscriptions() {
  const { data, error } = await listCalendarSubscriptions();

  if (!error) {
    calendarSubscriptions.value = data;
  }
}

async function checkAvailability() {
  const { data, error } = await queryCalendarAvailability({
    attendeeEmails: attendeeEmails(),
    endAt: toIso(eventModel.endAt),
    startAt: toIso(eventModel.startAt)
  });

  if (!error) {
    availability.value = data;
  }
}

async function submitSubscription() {
  const { error } = await createCalendarSubscription({
    authMode: 'none',
    color: subscriptionModel.color,
    label: subscriptionModel.label,
    url: subscriptionModel.url
  });

  if (!error) {
    subscriptionModel.label = '';
    subscriptionModel.url = '';
    await loadCalendarSubscriptions();
  }
}

async function syncSelectedSubscription() {
  const [subscriptionId] = selectedSubscriptionKeys.value.map(String);

  if (!subscriptionId) {
    return;
  }

  const { error } = await syncCalendarSubscription(subscriptionId);

  if (!error) {
    await Promise.all([loadCalendarSubscriptions(), loadEvents()]);
  }
}

async function downloadDefaultIcs() {
  const { data, error } = await exportCalendarIcs('default');

  if (error) {
    return;
  }

  const url = URL.createObjectURL(data);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = 'calendar.ics';
  anchor.click();
  URL.revokeObjectURL(url);
}

async function submitEvent() {
  const { error } = await createCalendarEvent(createPayload());

  if (!error) {
    drawerOpen.value = false;
    await loadEvents();
  }
}

async function deleteSelected() {
  const [eventId] = selectedRowKeys.value.map(String);

  if (!eventId) {
    return;
  }

  const { error } = await deleteCalendarEvent(eventId);

  if (!error) {
    selectedRowKeys.value = [];
    await loadEvents();
  }
}

onMounted(async () => {
  await loadSettings();
  await Promise.all([loadEvents(), loadCalendarSubscriptions()]);
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard class="card-wrapper" :title="$t('route.calendar')">
      <NSpace justify="space-between">
        <NTag v-if="settings">{{ $t('page.calendar.settings') }}: {{ settings.defaultTimezone }}</NTag>
        <NSpace>
          <NButton @click="downloadDefaultIcs">{{ $t('page.calendar.exportIcs') }}</NButton>
          <NButton @click="deleteSelected">{{ $t('page.calendar.deleteSelected') }}</NButton>
          <NButton type="primary" @click="drawerOpen = true">{{ $t('page.calendar.create') }}</NButton>
        </NSpace>
      </NSpace>
    </NCard>

    <NCard class="card-wrapper">
      <NDataTable
        v-model:checked-row-keys="selectedRowKeys"
        :columns="columns"
        :data="events"
        :loading="loading"
        :row-key="rowKey"
      />
    </NCard>

    <NCard class="card-wrapper" :title="$t('page.calendar.subscriptions')">
      <NSpace vertical :size="12">
        <NSpace>
          <NInput v-model:value="subscriptionModel.label" :placeholder="$t('page.calendar.subscriptionLabel')" />
          <NInput v-model:value="subscriptionModel.url" :placeholder="$t('page.calendar.sourceUrl')" />
          <NInput v-model:value="subscriptionModel.color" :placeholder="$t('page.calendar.color')" />
          <NButton type="primary" @click="submitSubscription">{{ $t('common.confirm') }}</NButton>
          <NButton @click="syncSelectedSubscription">{{ $t('page.calendar.sync') }}</NButton>
        </NSpace>
        <NDataTable
          v-model:checked-row-keys="selectedSubscriptionKeys"
          :columns="subscriptionColumns"
          :data="calendarSubscriptions"
          :row-key="subscriptionRowKey"
        />
      </NSpace>
    </NCard>
  </NSpace>

  <NDrawer v-model:show="drawerOpen" :width="520">
    <NDrawerContent :title="$t('page.calendar.create')" closable>
      <NForm :model="eventModel" label-placement="top">
        <NFormItem path="title" :label="$t('page.calendar.title')">
          <NInput v-model:value="eventModel.title" />
        </NFormItem>
        <NFormItem path="location" :label="$t('page.calendar.location')">
          <NInput v-model:value="eventModel.location" />
        </NFormItem>
        <NFormItem path="startAt" :label="$t('page.calendar.startAt')">
          <NDatePicker v-model:value="eventModel.startAt" type="datetime" class="w-full" />
        </NFormItem>
        <NFormItem path="endAt" :label="$t('page.calendar.endAt')">
          <NDatePicker v-model:value="eventModel.endAt" type="datetime" class="w-full" />
        </NFormItem>
        <NFormItem path="rrule" :label="$t('page.calendar.rrule')">
          <NInput v-model:value="eventModel.rrule" />
        </NFormItem>
        <NFormItem path="attendees" :label="$t('page.calendar.attendees')">
          <NInput v-model:value="eventModel.attendees" />
        </NFormItem>
        <!-- info-only, see v213-closure-spec-v1.1 §2.1 -->
        <NAlert v-if="availability" :type="availabilityType" :title="$t('page.calendar.availability')">
          {{ availabilityText }}
        </NAlert>
        <NSpace class="mt-16px" justify="end">
          <NButton @click="checkAvailability">{{ $t('page.calendar.availability') }}</NButton>
          <NButton type="primary" @click="submitEvent">{{ $t('common.confirm') }}</NButton>
        </NSpace>
      </NForm>
    </NDrawerContent>
  </NDrawer>
</template>
