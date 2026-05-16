<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NProgress,
  NSelect,
  NSpace,
  NSwitch,
  NTag
} from 'naive-ui';
import {
  connectVpnSession,
  createVpnProfile,
  deleteVpnProfile,
  disconnectVpn,
  listVpnProfiles,
  listVpnServers,
  listVpnSessionHistory,
  quickConnectVpn,
  readCurrentVpnSession,
  readVpnSettings,
  updateVpnProfile,
  updateVpnSettings
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'Vpn' });

const DEFAULT_HISTORY_LIMIT = 20;
const DEFAULT_PROTOCOL: Api.Vpn.Protocol = 'WIREGUARD';
const ONLINE_STATUS = 'ONLINE';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const servers = ref<Api.Vpn.Server[]>([]);
const profiles = ref<Api.Vpn.Profile[]>([]);
const sessionHistory = ref<Api.Vpn.Session[]>([]);
const currentSession = ref<Api.Vpn.Session | null>(null);
const settings = ref<Api.Vpn.Settings | null>(null);
const editingProfileId = ref('');

const profileModel = reactive<Api.Vpn.ProfilePayload>({
  name: '',
  protocol: DEFAULT_PROTOCOL,
  routingMode: 'FASTEST',
  targetServerId: '',
  targetCountry: '',
  secureCoreEnabled: false,
  netshieldMode: 'OFF',
  killSwitchEnabled: false
});
const settingsModel = reactive<Api.Vpn.SettingsPayload>({
  netshieldMode: 'OFF',
  killSwitchEnabled: false,
  defaultConnectionMode: 'FASTEST',
  defaultProfileId: ''
});

const protocolOptions = [
  { label: 'WireGuard', value: 'WIREGUARD' },
  { label: 'OpenVPN UDP', value: 'OPENVPN_UDP' },
  { label: 'OpenVPN TCP', value: 'OPENVPN_TCP' }
];
const routingModeOptions = [
  { label: 'FASTEST', value: 'FASTEST' },
  { label: 'COUNTRY', value: 'COUNTRY' },
  { label: 'SERVER', value: 'SERVER' }
];
const netshieldOptions = [
  { label: 'OFF', value: 'OFF' },
  { label: 'BLOCK_MALWARE', value: 'BLOCK_MALWARE' },
  { label: 'BLOCK_MALWARE_ADS_TRACKERS', value: 'BLOCK_MALWARE_ADS_TRACKERS' }
];
const defaultModeOptions = [
  { label: 'FASTEST', value: 'FASTEST' },
  { label: 'RANDOM', value: 'RANDOM' },
  { label: 'LAST_CONNECTION', value: 'LAST_CONNECTION' },
  { label: 'PROFILE', value: 'PROFILE' }
];

const profileOptions = computed(() => profiles.value.map(item => ({ label: item.name, value: item.profileId })));
const serverOptions = computed(() =>
  servers.value.map(item => ({ label: `${item.serverId} - ${item.country}/${item.city}`, value: item.serverId }))
);
const countryOptions = computed(() =>
  Array.from(new Set(servers.value.map(item => item.country))).map(country => ({ label: country, value: country }))
);
const currentLoadPercent = computed(
  () => servers.value.find(item => item.serverId === currentSession.value?.serverId)?.loadPercent || 0
);
const profileFormTitle = computed(() =>
  editingProfileId.value ? $t('page.vpn.updateProfile') : $t('page.vpn.createProfile')
);
const sections = computed(() => [
  { name: 'vpn_servers' as const, label: $t('page.vpn.servers') },
  { name: 'vpn_profiles' as const, label: $t('page.vpn.profiles') },
  { name: 'vpn_sessions' as const, label: $t('page.vpn.sessions') },
  { name: 'vpn_settings' as const, label: $t('page.vpn.settings') }
]);

type VpnSectionName = (typeof sections.value)[number]['name'];

const serverColumns = computed(() => [
  { title: 'ID', key: 'serverId' },
  { title: $t('page.vpn.country'), key: 'country' },
  { title: $t('page.vpn.city'), key: 'city' },
  { title: $t('page.vpn.tier'), key: 'tier' },
  {
    title: $t('page.vpn.load'),
    key: 'loadPercent',
    render: (row: Api.Vpn.Server) => h(NProgress, { type: 'line', percentage: row.loadPercent, height: 16 })
  },
  {
    title: $t('common.action'),
    key: 'actions',
    render: (row: Api.Vpn.Server) =>
      h(
        NButton,
        { disabled: row.status !== ONLINE_STATUS, size: 'small', type: 'primary', onClick: () => connectServer(row) },
        { default: () => $t('page.vpn.connect') }
      )
  }
]);

const profileColumns = computed(() => [
  { title: $t('page.vpn.name'), key: 'name' },
  { title: $t('page.vpn.protocol'), key: 'protocol' },
  { title: $t('page.vpn.routingMode'), key: 'routingMode' },
  { title: $t('page.vpn.netshield'), key: 'netshieldMode' },
  {
    title: $t('common.action'),
    key: 'actions',
    render: (row: Api.Vpn.Profile) =>
      h(
        NSpace,
        { size: 8 },
        {
          default: () => [
            h(
              NButton,
              { size: 'small', type: 'primary', onClick: () => connectProfile(row) },
              { default: () => $t('page.vpn.connect') }
            ),
            h(NButton, { size: 'small', onClick: () => editProfile(row) }, { default: () => $t('common.edit') }),
            h(
              NButton,
              { size: 'small', type: 'error', onClick: () => removeProfile(row) },
              { default: () => $t('common.delete') }
            )
          ]
        }
      )
  }
]);

const historyColumns = computed(() => [
  { title: $t('page.vpn.country'), key: 'serverCountry' },
  { title: $t('page.vpn.city'), key: 'serverCity' },
  { title: $t('page.vpn.protocol'), key: 'protocol' },
  { title: $t('page.notifications.status'), key: 'status' },
  { title: $t('page.vpn.defaultMode'), key: 'connectionSource' },
  { title: $t('page.calendar.startAt'), key: 'connectedAt' }
]);

function routeIsActive(target: VpnSectionName) {
  const currentRoute = String(route.name || 'vpn_servers');
  return currentRoute === target || (currentRoute === 'vpn' && target === 'vpn_servers');
}

function sectionButtonType(target: VpnSectionName) {
  return routeIsActive(target) ? 'primary' : 'default';
}

function openSection(target: VpnSectionName) {
  router.push({ name: target });
}

function syncSettingsModel(nextSettings: Api.Vpn.Settings) {
  settingsModel.netshieldMode = nextSettings.netshieldMode;
  settingsModel.killSwitchEnabled = nextSettings.killSwitchEnabled;
  settingsModel.defaultConnectionMode = nextSettings.defaultConnectionMode;
  settingsModel.defaultProfileId = nextSettings.defaultProfileId || '';
}

function resetProfileModel() {
  editingProfileId.value = '';
  profileModel.name = '';
  profileModel.protocol = DEFAULT_PROTOCOL;
  profileModel.routingMode = 'FASTEST';
  profileModel.targetServerId = '';
  profileModel.targetCountry = '';
  profileModel.secureCoreEnabled = false;
  profileModel.netshieldMode = 'OFF';
  profileModel.killSwitchEnabled = false;
}

function profilePayload(): Api.Vpn.ProfilePayload {
  return {
    ...profileModel,
    targetServerId: profileModel.routingMode === 'SERVER' ? profileModel.targetServerId || undefined : undefined,
    targetCountry: profileModel.routingMode === 'COUNTRY' ? profileModel.targetCountry || undefined : undefined
  };
}

function settingsPayload(): Api.Vpn.SettingsPayload {
  return {
    ...settingsModel,
    defaultProfileId:
      settingsModel.defaultConnectionMode === 'PROFILE' ? settingsModel.defaultProfileId || undefined : undefined
  };
}

function editProfile(profile: Api.Vpn.Profile) {
  editingProfileId.value = profile.profileId;
  profileModel.name = profile.name;
  profileModel.protocol = profile.protocol as Api.Vpn.Protocol;
  profileModel.routingMode = profile.routingMode as Api.Vpn.RoutingMode;
  profileModel.targetServerId = profile.targetServerId || '';
  profileModel.targetCountry = profile.targetCountry || '';
  profileModel.secureCoreEnabled = profile.secureCoreEnabled;
  profileModel.netshieldMode = profile.netshieldMode as Api.Vpn.NetShieldMode;
  profileModel.killSwitchEnabled = profile.killSwitchEnabled;
}

async function loadVpn() {
  loading.value = true;
  const [serverResult, profileResult, sessionResult, settingsResult, historyResult] = await Promise.all([
    listVpnServers(),
    listVpnProfiles(),
    readCurrentVpnSession(),
    readVpnSettings(),
    listVpnSessionHistory({ limit: DEFAULT_HISTORY_LIMIT })
  ]);

  if (!serverResult.error) servers.value = serverResult.data;
  if (!profileResult.error) profiles.value = profileResult.data;
  if (!sessionResult.error) currentSession.value = sessionResult.data;
  if (!historyResult.error) sessionHistory.value = historyResult.data;
  if (!settingsResult.error) {
    settings.value = settingsResult.data;
    syncSettingsModel(settingsResult.data);
  }
  loading.value = false;
}

async function submitProfile() {
  const payload = profilePayload();
  const request = editingProfileId.value
    ? updateVpnProfile(editingProfileId.value, payload)
    : createVpnProfile(payload);
  const { error } = await request;

  if (!error) {
    resetProfileModel();
    await loadVpn();
  }
}

async function removeProfile(profile: Api.Vpn.Profile) {
  const { error } = await deleteVpnProfile(profile.profileId);

  if (!error) {
    if (editingProfileId.value === profile.profileId) resetProfileModel();
    await loadVpn();
  }
}

async function submitSettings() {
  const { data, error } = await updateVpnSettings(settingsPayload());

  if (!error) {
    settings.value = data;
    syncSettingsModel(data);
  }
}

async function connectServer(server: Api.Vpn.Server) {
  const { error } = await connectVpnSession({ serverId: server.serverId, protocol: DEFAULT_PROTOCOL });

  if (!error) {
    await loadVpn();
  }
}

async function connectProfile(profile: Api.Vpn.Profile) {
  const { error } = await quickConnectVpn({ profileId: profile.profileId });

  if (!error) {
    await loadVpn();
  }
}

async function connectVpn() {
  const profileId =
    settingsModel.defaultConnectionMode === 'PROFILE' ? settingsModel.defaultProfileId || undefined : undefined;
  const { error } = await quickConnectVpn({ profileId });

  if (!error) {
    await loadVpn();
  }
}

async function stopVpn() {
  const { error } = await disconnectVpn();

  if (!error) {
    await loadVpn();
  }
}

onMounted(loadVpn);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard class="card-wrapper" :title="$t('route.vpn')">
      <NSpace vertical :size="12">
        <NSpace justify="space-between" align="center">
          <NSpace>
            <NTag :type="currentSession?.status === 'CONNECTED' ? 'success' : 'default'">
              {{ currentSession?.status || $t('common.noData') }}
            </NTag>
            <NTag v-if="currentSession">{{ currentSession.serverCountry }} / {{ currentSession.serverCity }}</NTag>
            <NTag>{{ settings?.netshieldMode || settingsModel.netshieldMode }}</NTag>
            <NTag>
              {{ $t('page.vpn.killSwitch') }}:
              {{ settingsModel.killSwitchEnabled ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no') }}
            </NTag>
          </NSpace>
          <NSpace>
            <NButton
              v-for="section in sections"
              :key="section.name"
              :type="sectionButtonType(section.name)"
              @click="openSection(section.name)"
            >
              {{ section.label }}
            </NButton>
          </NSpace>
        </NSpace>
        <NProgress type="line" :percentage="currentLoadPercent" />
        <NSpace>
          <NButton type="primary" @click="connectVpn">{{ $t('page.vpn.quickConnect') }}</NButton>
          <NButton @click="stopVpn">{{ $t('page.vpn.disconnect') }}</NButton>
          <NButton @click="loadVpn">{{ $t('common.refresh') }}</NButton>
        </NSpace>
      </NSpace>
    </NCard>

    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 l:15">
        <NCard class="card-wrapper" :title="$t('page.vpn.servers')">
          <NDataTable :columns="serverColumns" :data="servers" :loading="loading" />
        </NCard>

        <NCard class="card-wrapper mt-16px" :title="$t('page.vpn.profiles')">
          <NDataTable :columns="profileColumns" :data="profiles" :loading="loading" />
        </NCard>

        <NCard class="card-wrapper mt-16px" :title="$t('page.vpn.history')">
          <NDataTable :columns="historyColumns" :data="sessionHistory" :loading="loading" />
        </NCard>
      </NGi>

      <NGi span="24 l:9">
        <NSpace vertical :size="16">
          <NCard class="card-wrapper" :title="profileFormTitle">
            <NForm :model="profileModel" label-placement="top">
              <NFormItem path="name" :label="$t('page.vpn.name')">
                <NInput v-model:value="profileModel.name" />
              </NFormItem>
              <NFormItem path="protocol" :label="$t('page.vpn.protocol')">
                <NSelect v-model:value="profileModel.protocol" :options="protocolOptions" />
              </NFormItem>
              <NFormItem path="routingMode" :label="$t('page.vpn.routingMode')">
                <NSelect v-model:value="profileModel.routingMode" :options="routingModeOptions" />
              </NFormItem>
              <NFormItem path="targetServerId" :label="$t('page.vpn.targetServer')">
                <NSelect v-model:value="profileModel.targetServerId" clearable :options="serverOptions" />
              </NFormItem>
              <NFormItem path="targetCountry" :label="$t('page.vpn.targetCountry')">
                <NSelect v-model:value="profileModel.targetCountry" clearable :options="countryOptions" />
              </NFormItem>
              <NFormItem path="netshieldMode" :label="$t('page.vpn.netshield')">
                <NSelect v-model:value="profileModel.netshieldMode" :options="netshieldOptions" />
              </NFormItem>
              <NFormItem path="secureCoreEnabled" :label="$t('page.vpn.secureCore')">
                <NSwitch v-model:value="profileModel.secureCoreEnabled" />
              </NFormItem>
              <NFormItem path="killSwitchEnabled" :label="$t('page.vpn.killSwitch')">
                <NSwitch v-model:value="profileModel.killSwitchEnabled" />
              </NFormItem>
              <NSpace>
                <NButton type="primary" @click="submitProfile">{{ profileFormTitle }}</NButton>
                <NButton @click="resetProfileModel">{{ $t('common.reset') }}</NButton>
              </NSpace>
            </NForm>
          </NCard>

          <NCard class="card-wrapper" :title="$t('page.vpn.settings')">
            <NForm :model="settingsModel" label-placement="top">
              <NFormItem path="defaultConnectionMode" :label="$t('page.vpn.defaultMode')">
                <NSelect v-model:value="settingsModel.defaultConnectionMode" :options="defaultModeOptions" />
              </NFormItem>
              <NFormItem path="defaultProfileId" :label="$t('page.vpn.profiles')">
                <NSelect v-model:value="settingsModel.defaultProfileId" clearable :options="profileOptions" />
              </NFormItem>
              <NFormItem path="netshieldMode" :label="$t('page.vpn.netshield')">
                <NSelect v-model:value="settingsModel.netshieldMode" :options="netshieldOptions" />
              </NFormItem>
              <NFormItem path="killSwitchEnabled" :label="$t('page.vpn.killSwitch')">
                <NSwitch v-model:value="settingsModel.killSwitchEnabled" />
              </NFormItem>
              <NButton type="primary" @click="submitSettings">{{ $t('page.vpn.settings') }}</NButton>
            </NForm>
          </NCard>
        </NSpace>
      </NGi>
    </NGrid>
  </NSpace>
</template>
