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
  NStatistic,
  NSwitch
} from 'naive-ui';
import { createSimpleLoginRelayPolicy, listSimpleLoginRelayPolicies, readSimpleLoginOverview } from '@/service/api';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';

defineOptions({ name: 'IntegrationsSimplelogin' });

const authStore = useAuthStore();
const overview = ref<Api.SimpleLogin.Overview | null>(null);
const policies = ref<Api.SimpleLogin.RelayPolicy[]>([]);
const policyModel = reactive({
  catchAllEnabled: true,
  customDomainId: null as number | null,
  defaultMailboxId: null as number | null,
  note: '',
  subdomainMode: 'DISABLED'
});
const subdomainOptions = [
  { label: 'DISABLED', value: 'DISABLED' },
  { label: 'FORWARD', value: 'FORWARD' }
];

const orgId = computed(() => authStore.currentOrgId);
const columns = computed(() => [
  { title: $t('page.simpleLogin.domain'), key: 'domain' },
  { title: $t('page.simpleLogin.defaultMailbox'), key: 'defaultMailboxEmail' },
  { title: $t('page.simpleLogin.subdomainMode'), key: 'subdomainMode' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' }
]);

async function loadSimpleLogin() {
  const overviewResult = await readSimpleLoginOverview(orgId.value ? { orgId: orgId.value } : {});

  if (!overviewResult.error) {
    overview.value = overviewResult.data;
  }

  if (!orgId.value) {
    return;
  }

  const policyResult = await listSimpleLoginRelayPolicies(orgId.value);

  if (!policyResult.error) {
    policies.value = policyResult.data;
  }
}

async function submitPolicy() {
  const { error } = await createSimpleLoginRelayPolicy(orgId.value, { ...policyModel });

  if (!error) {
    policyModel.note = '';
    await loadSimpleLogin();
  }
}

onMounted(loadSimpleLogin);
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 m:16">
      <NCard class="card-wrapper" :title="$t('route.integrations_simplelogin')">
        <NSpace class="mb-12px">
          <NStatistic :label="$t('page.simpleLogin.aliases')" :value="overview?.aliasCount || 0" />
          <NStatistic :label="$t('page.simpleLogin.enabled')" :value="overview?.enabledAliasCount || 0" />
          <NStatistic :label="$t('page.simpleLogin.domains')" :value="overview?.verifiedCustomDomainCount || 0" />
        </NSpace>
        <NDataTable :columns="columns" :data="policies" />
      </NCard>
    </NGi>
    <NGi span="24 m:8">
      <NCard class="card-wrapper" :title="$t('page.simpleLogin.createPolicy')">
        <NForm :model="policyModel" label-placement="top">
          <NFormItem path="customDomainId" :label="$t('page.simpleLogin.customDomainId')">
            <NInputNumber v-model:value="policyModel.customDomainId" class="w-full" />
          </NFormItem>
          <NFormItem path="defaultMailboxId" :label="$t('page.simpleLogin.defaultMailboxId')">
            <NInputNumber v-model:value="policyModel.defaultMailboxId" class="w-full" />
          </NFormItem>
          <NFormItem path="subdomainMode" :label="$t('page.simpleLogin.subdomainMode')">
            <NSelect v-model:value="policyModel.subdomainMode" :options="subdomainOptions" />
          </NFormItem>
          <NFormItem path="catchAllEnabled" :label="$t('page.simpleLogin.catchAll')">
            <NSwitch v-model:value="policyModel.catchAllEnabled" />
          </NFormItem>
          <NFormItem path="note" :label="$t('page.contacts.note')">
            <NInput v-model:value="policyModel.note" type="textarea" :autosize="{ minRows: 3 }" />
          </NFormItem>
          <NButton type="primary" @click="submitPolicy">{{ $t('page.simpleLogin.createPolicy') }}</NButton>
        </NForm>
      </NCard>
    </NGi>
  </NGrid>
</template>
