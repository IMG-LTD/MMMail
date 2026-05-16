<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NInputNumber,
  NSelect,
  NSpace,
  NSwitch,
  NTag
} from 'naive-ui';
import type { DataTableRowKey } from 'naive-ui';
import {
  createAdminDomain,
  deleteDeviceSession,
  deleteWebPushSubscription,
  listAdminDomainDnsRecords,
  listAdminDomains,
  listDeviceSessions,
  listWebPushSubscriptions,
  readAdminDomainDiagnostics,
  readNotificationSettings,
  readSecuritySettings,
  readUserProfile,
  readWebPushPublicKey,
  registerWebPushSubscription,
  testWebPushSubscription,
  updateNotificationSettings,
  updateSecuritySettings,
  updateUserProfile,
  verifyAdminDomain
} from '@/service/api';
import { useOrgStore } from '@/store/modules/org';
import { $t } from '@/locales';

defineOptions({
  name: 'Settings'
});

const orgStore = useOrgStore();
const devices = ref<Api.Settings.DeviceSession[]>([]);
const domains = ref<Api.Admin.Domain[]>([]);
const domainDnsRecords = ref<Api.Admin.DomainDnsRecord[]>([]);
const domainDiagnostics = ref<Api.Admin.DomainDnsDiagnostics | null>(null);
const webPushPublicKey = ref('');
const webPushSubscriptions = ref<Api.Settings.WebPushSubscription[]>([]);
const selectedDeviceKeys = ref<DataTableRowKey[]>([]);
const domainModel = reactive({ domain: '' });
const webPushModel = reactive({ auth: '', endpoint: '', label: '', p256dh: '', userAgent: '' });

const profile = reactive<Api.Settings.UserProfile>({
  autoSaveSeconds: 30,
  displayName: '',
  driveVersionRetentionCount: 20,
  driveVersionRetentionDays: 180,
  mailAddressMode: 'PROTON_ADDRESS',
  preferredLocale: 'zh-CN',
  signature: '',
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
  undoSendSeconds: 10
});

const security = reactive<Api.Settings.SecuritySettings>({
  mfaEnabled: false,
  recoveryEmail: ''
});

const notificationSettings = reactive<Api.Settings.NotificationSettings>({
  emailDigest: false,
  productUpdates: false
});

const deviceColumns = computed(() => [
  { type: 'selection' as const },
  { title: $t('page.settings.devices'), key: 'deviceName' },
  { title: $t('page.drive.updatedAt'), key: 'lastActiveAt' }
]);
const domainColumns = computed(() => [
  { title: $t('page.domains.domain'), key: 'domain' },
  { title: $t('page.notifications.status'), key: 'status' },
  { title: $t('page.domains.token'), key: 'verificationToken' }
]);
const dnsRecordColumns = computed(() => [
  { title: $t('page.domains.type'), key: 'type' },
  { title: $t('page.domains.host'), key: 'host' },
  { title: $t('page.domains.expected'), key: 'expected' }
]);
const domainDiagnosticColumns = computed(() => [
  { title: $t('page.domains.type'), key: 'type' },
  { title: $t('page.domains.host'), key: 'host' },
  { title: $t('page.domains.expected'), key: 'expected' },
  { title: $t('page.domains.actual'), key: 'actualText' },
  { title: $t('page.domains.matched'), key: 'matchedText' }
]);
const webPushSubscriptionColumns = computed(() => [
  { title: $t('page.webPush.label'), key: 'label' },
  { title: $t('page.webPush.endpoint'), key: 'endpointHash' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' }
]);
const domainDiagnosticRows = computed(() =>
  (domainDiagnostics.value?.records ?? []).map(record => ({
    type: record.type,
    host: record.host,
    expected: record.expected,
    actualText: record.actual.join(', ') || '-',
    matchedText: record.matched ? 'OK' : 'ACTION_REQUIRED'
  }))
);

const localeOptions = [
  { label: '简体中文', value: 'zh-CN' },
  { label: '繁體中文', value: 'zh-TW' },
  { label: 'English', value: 'en' }
];

const mailAddressModeOptions = [
  { label: 'Proton address', value: 'PROTON_ADDRESS' },
  { label: 'External account', value: 'EXTERNAL_ACCOUNT' }
];

function rowKey(row: Api.Settings.DeviceSession) {
  return row.id;
}

function domainRowKey(row: Api.Admin.Domain) {
  return row.id;
}

function dnsRecordRowKey(row: { host: string; type: string }) {
  return `${row.type}:${row.host}`;
}

function webPushRowKey(row: Api.Settings.WebPushSubscription) {
  return row.subscriptionId;
}

function firstDomainId() {
  return domains.value[0]?.id || '';
}

function firstWebPushSubscriptionId() {
  return webPushSubscriptions.value[0]?.subscriptionId;
}

async function loadSettings() {
  const [profileResult, securityResult, devicesResult, notificationsResult] = await Promise.all([
    readUserProfile(),
    readSecuritySettings(),
    listDeviceSessions(),
    readNotificationSettings()
  ]);

  if (!profileResult.error) {
    Object.assign(profile, profileResult.data);
  }

  if (!securityResult.error) {
    Object.assign(security, securityResult.data);
  }

  if (!devicesResult.error) {
    devices.value = devicesResult.data;
  }

  if (!notificationsResult.error) {
    Object.assign(notificationSettings, notificationsResult.data);
  }

  await loadDomainAndWebPush();
}

async function loadDomainAndWebPush() {
  const [publicKeyResult, subscriptionsResult] = await Promise.all([
    readWebPushPublicKey(),
    listWebPushSubscriptions()
  ]);

  if (!publicKeyResult.error) {
    webPushPublicKey.value = publicKeyResult.data.publicKey;
  }

  if (!subscriptionsResult.error) {
    webPushSubscriptions.value = subscriptionsResult.data;
  }

  if (!orgStore.currentOrgId) {
    return;
  }

  const { data, error } = await listAdminDomains(orgStore.currentOrgId);

  if (!error) {
    domains.value = data;
    await loadDomainDnsRecords();
  }
}

async function submitProfile() {
  const { error } = await updateUserProfile({ ...profile });

  if (!error) {
    window.$message?.success($t('common.updateSuccess'));
  }
}

async function submitSecurity() {
  const { error } = await updateSecuritySettings({ ...security });

  if (!error) {
    window.$message?.success($t('common.updateSuccess'));
  }
}

async function submitNotificationSettings() {
  const { error } = await updateNotificationSettings({ ...notificationSettings });

  if (!error) {
    window.$message?.success($t('common.updateSuccess'));
  }
}

async function deleteSelectedDevice() {
  const [deviceId] = selectedDeviceKeys.value.map(String);

  if (!deviceId) {
    return;
  }

  const { error } = await deleteDeviceSession(deviceId);

  if (!error) {
    selectedDeviceKeys.value = [];
    await loadSettings();
  }
}

async function submitDomain() {
  const { error } = await createAdminDomain(orgStore.currentOrgId, { ...domainModel });

  if (!error) {
    domainModel.domain = '';
    await loadDomainAndWebPush();
  }
}

async function loadDomainDnsRecords() {
  const domainId = firstDomainId();

  if (!domainId) {
    domainDnsRecords.value = [];
    domainDiagnostics.value = null;
    return;
  }

  const { data, error } = await listAdminDomainDnsRecords(domainId);

  if (!error) {
    domainDnsRecords.value = data.records;
    domainDiagnostics.value = null;
  }
}

async function inspectDomainDns() {
  const domainId = firstDomainId();

  if (!domainId) {
    return;
  }

  const { data, error } = await readAdminDomainDiagnostics(domainId);

  if (!error) {
    domainDiagnostics.value = data;
  }
}

async function verifyDomain() {
  const domainId = firstDomainId();

  if (!domainId) {
    return;
  }

  const { error } = await verifyAdminDomain(orgStore.currentOrgId, domainId);

  if (!error) {
    await loadDomainAndWebPush();
  }
}

async function submitWebPush() {
  const { error } = await registerWebPushSubscription({ ...webPushModel });

  if (!error) {
    await loadDomainAndWebPush();
  }
}

async function removeWebPush() {
  const subscriptionId = firstWebPushSubscriptionId();

  if (!subscriptionId) {
    return;
  }

  const { error } = await deleteWebPushSubscription(subscriptionId);

  if (!error) {
    await loadDomainAndWebPush();
  }
}

async function testWebPush() {
  const { error } = await testWebPushSubscription(firstWebPushSubscriptionId());

  if (!error) {
    window.$message?.success($t('common.updateSuccess'));
  }
}

onMounted(loadSettings);
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 m:12">
      <NCard class="card-wrapper" :title="$t('page.settings.profile')">
        <NForm :model="profile" label-placement="top">
          <NFormItem path="displayName" :label="$t('page.settings.displayName')">
            <NInput v-model:value="profile.displayName" />
          </NFormItem>
          <NFormItem path="signature" :label="$t('page.settings.signature')">
            <NInput v-model:value="profile.signature" type="textarea" />
          </NFormItem>
          <NFormItem path="timezone" :label="$t('page.settings.timezone')">
            <NInput v-model:value="profile.timezone" />
          </NFormItem>
          <NFormItem path="preferredLocale" :label="$t('page.settings.preferredLocale')">
            <NSelect v-model:value="profile.preferredLocale" :options="localeOptions" />
          </NFormItem>
          <NFormItem path="mailAddressMode" :label="$t('page.settings.mailAddressMode')">
            <NSelect v-model:value="profile.mailAddressMode" :options="mailAddressModeOptions" />
          </NFormItem>
          <NSpace>
            <NInputNumber v-model:value="profile.autoSaveSeconds" :min="5" :max="300" />
            <NInputNumber v-model:value="profile.undoSendSeconds" :min="0" :max="60" />
          </NSpace>
          <NButton class="mt-16px" type="primary" @click="submitProfile">{{ $t('common.update') }}</NButton>
        </NForm>
      </NCard>
    </NGi>

    <NGi span="24 m:12">
      <NSpace vertical :size="16">
        <NCard class="card-wrapper" :title="$t('page.settings.security')">
          <NForm :model="security" label-placement="left">
            <NFormItem path="mfaEnabled" :label="$t('page.settings.mfaEnabled')">
              <NSwitch v-model:value="security.mfaEnabled" />
            </NFormItem>
            <NFormItem path="recoveryEmail" :label="$t('page.settings.recoveryEmail')">
              <NInput v-model:value="security.recoveryEmail" />
            </NFormItem>
            <NButton type="primary" @click="submitSecurity">{{ $t('common.update') }}</NButton>
          </NForm>
        </NCard>

        <NCard class="card-wrapper" :title="$t('page.settings.notifications')">
          <NSpace vertical>
            <NSwitch v-model:value="notificationSettings.emailDigest">
              {{ $t('page.settings.emailDigest') }}
            </NSwitch>
            <NSwitch v-model:value="notificationSettings.productUpdates">
              {{ $t('page.settings.productUpdates') }}
            </NSwitch>
            <NButton type="primary" @click="submitNotificationSettings">{{ $t('common.update') }}</NButton>
          </NSpace>
        </NCard>
      </NSpace>
    </NGi>

    <NGi span="24">
      <NCard class="card-wrapper" :title="$t('page.settings.devices')">
        <NSpace class="mb-12px" justify="end">
          <NButton type="error" @click="deleteSelectedDevice">{{ $t('page.settings.revokeDevice') }}</NButton>
        </NSpace>
        <NDataTable
          v-model:checked-row-keys="selectedDeviceKeys"
          :columns="deviceColumns"
          :data="devices"
          :row-key="rowKey"
        />
      </NCard>
    </NGi>

    <NGi span="24 m:12">
      <NCard class="card-wrapper" :title="$t('page.domains.title')">
        <NForm :model="domainModel" label-placement="top">
          <NFormItem path="domain" :label="$t('page.domains.domain')">
            <NInput v-model:value="domainModel.domain" />
          </NFormItem>
          <NSpace class="mb-12px">
            <NButton type="primary" @click="submitDomain">{{ $t('common.add') }}</NButton>
            <NButton @click="inspectDomainDns">{{ $t('page.domains.diagnostics') }}</NButton>
            <NButton @click="verifyDomain">{{ $t('page.domains.verify') }}</NButton>
          </NSpace>
        </NForm>
        <NDataTable :columns="domainColumns" :data="domains" :row-key="domainRowKey" />
        <NDataTable class="mt-12px" :columns="dnsRecordColumns" :data="domainDnsRecords" :row-key="dnsRecordRowKey" />
        <NSpace v-if="domainDiagnostics" class="mt-12px" vertical>
          <NTag>{{ domainDiagnostics.status }}</NTag>
          <NDataTable :columns="domainDiagnosticColumns" :data="domainDiagnosticRows" :row-key="dnsRecordRowKey" />
        </NSpace>
      </NCard>
    </NGi>

    <NGi span="24 m:12">
      <NCard class="card-wrapper" :title="$t('page.webPush.title')">
        <NSpace class="mb-12px">
          <NTag>{{ webPushPublicKey || $t('common.noData') }}</NTag>
          <NTag>{{ webPushSubscriptions.length }}</NTag>
        </NSpace>
        <NForm :model="webPushModel" label-placement="top">
          <NFormItem path="endpoint" :label="$t('page.webPush.endpoint')">
            <NInput v-model:value="webPushModel.endpoint" />
          </NFormItem>
          <NFormItem path="label" :label="$t('page.webPush.label')">
            <NInput v-model:value="webPushModel.label" />
          </NFormItem>
          <NFormItem path="p256dh" :label="$t('page.webPush.p256dh')">
            <NInput v-model:value="webPushModel.p256dh" />
          </NFormItem>
          <NFormItem path="auth" :label="$t('page.webPush.auth')">
            <NInput v-model:value="webPushModel.auth" />
          </NFormItem>
          <NFormItem path="userAgent" :label="$t('page.webPush.userAgent')">
            <NInput v-model:value="webPushModel.userAgent" />
          </NFormItem>
          <NSpace>
            <NButton type="primary" @click="submitWebPush">{{ $t('page.webPush.register') }}</NButton>
            <NButton @click="removeWebPush">{{ $t('common.delete') }}</NButton>
            <NButton @click="testWebPush">{{ $t('page.webPush.test') }}</NButton>
          </NSpace>
        </NForm>
        <NDataTable
          class="mt-12px"
          :columns="webPushSubscriptionColumns"
          :data="webPushSubscriptions"
          :row-key="webPushRowKey"
        />
      </NCard>
    </NGi>
  </NGrid>
</template>
