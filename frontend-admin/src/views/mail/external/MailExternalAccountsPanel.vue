<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import {
  NButton,
  NCard,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NInputNumber,
  NPopconfirm,
  NSelect,
  NSpace,
  NTag
} from 'naive-ui';
import {
  createMailExternalAccount,
  deleteMailExternalAccount,
  listMailExternalAccounts,
  syncMailExternalAccount,
  testMailExternalAccount,
  updateMailExternalAccount
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({
  name: 'MailExternalAccountsPanel'
});

const emit = defineEmits<{
  (event: 'synced'): void;
}>();

interface ExternalAccountFormModel extends Api.Mail.ExternalAccountPayload {
  authMode: string;
  email: string;
  imap: Api.Mail.ExternalServer;
  password: string;
  provider: string;
  smtp: Api.Mail.ExternalServer;
  username: string;
}

const providerOptions = [
  { label: 'Gmail', value: 'GMAIL' },
  { label: 'Outlook', value: 'OUTLOOK' },
  { label: 'IMAP', value: 'IMAP' }
];
const authModeOptions = [
  { label: 'Password', value: 'PASSWORD' },
  { label: 'OAuth2', value: 'OAUTH2' }
];

const externalAccounts = ref<Api.Mail.ExternalAccount[]>([]);
const externalAccountBusy = ref(false);
const externalAccountEditingId = ref('');
const externalAccountModel = reactive<ExternalAccountFormModel>(defaultExternalAccountModel());

onMounted(loadExternalAccounts);

async function loadExternalAccounts() {
  const { data, error } = await listMailExternalAccounts();

  if (!error) {
    externalAccounts.value = data;
  }
}

async function submitExternalAccount() {
  externalAccountBusy.value = true;
  try {
    const request = externalAccountEditingId.value
      ? updateMailExternalAccount(externalAccountEditingId.value, externalAccountModel)
      : createMailExternalAccount(externalAccountModel);
    const { error } = await request;

    if (!error) {
      resetExternalAccountModel();
      window.$message?.success($t('common.updateSuccess'));
      await loadExternalAccounts();
    }
  } finally {
    externalAccountBusy.value = false;
  }
}

async function runExternalAccountTest(accountId: string) {
  externalAccountBusy.value = true;
  try {
    const { data, error } = await testMailExternalAccount(accountId);

    if (!error) {
      window.$message?.success(data.message || $t('common.updateSuccess'));
    }
  } finally {
    externalAccountBusy.value = false;
  }
}

async function runExternalAccountSync(accountId: string) {
  externalAccountBusy.value = true;
  try {
    const { error } = await syncMailExternalAccount(accountId);

    if (!error) {
      window.$message?.success($t('common.updateSuccess'));
      await loadExternalAccounts();
      emit('synced');
    }
  } finally {
    externalAccountBusy.value = false;
  }
}

async function removeExternalAccount(accountId: string) {
  const { error } = await deleteMailExternalAccount(accountId);

  if (!error) {
    await loadExternalAccounts();
  }
}

function editExternalAccount(account: Api.Mail.ExternalAccount) {
  externalAccountEditingId.value = account.accountId;
  Object.assign(externalAccountModel, {
    provider: account.provider,
    authMode: account.authMode,
    email: account.email,
    username: account.username,
    password: '',
    oauthRefreshToken: '',
    imap: { ...account.imap },
    smtp: { ...account.smtp }
  });
}

function resetExternalAccountModel() {
  externalAccountEditingId.value = '';
  Object.assign(externalAccountModel, defaultExternalAccountModel());
}

function defaultExternalAccountModel(): ExternalAccountFormModel {
  return {
    provider: 'GMAIL',
    authMode: 'PASSWORD',
    email: '',
    username: '',
    password: '',
    oauthRefreshToken: '',
    imap: { host: 'imap.gmail.com', port: 993, ssl: true },
    smtp: { host: 'smtp.gmail.com', port: 587, starttls: true, ssl: false }
  };
}
</script>

<template>
  <NCard class="card-wrapper" :title="$t('page.mail.externalAccounts')">
    <NForm :model="externalAccountModel" label-placement="top">
      <NGrid :x-gap="8" :y-gap="4" responsive="screen" item-responsive>
        <NGi span="12">
          <NFormItem path="provider" :label="$t('page.mail.provider')">
            <NSelect v-model:value="externalAccountModel.provider" :options="providerOptions" />
          </NFormItem>
        </NGi>
        <NGi span="12">
          <NFormItem path="authMode" :label="$t('page.mail.authMode')">
            <NSelect v-model:value="externalAccountModel.authMode" :options="authModeOptions" />
          </NFormItem>
        </NGi>
        <NGi span="24">
          <NFormItem path="email" :label="$t('page.mail.accountEmail')">
            <NInput v-model:value="externalAccountModel.email" />
          </NFormItem>
        </NGi>
        <NGi span="24">
          <NFormItem path="username" :label="$t('page.mail.username')">
            <NInput v-model:value="externalAccountModel.username" />
          </NFormItem>
        </NGi>
        <NGi span="24">
          <NFormItem path="password" :label="$t('page.mail.password')">
            <NInput v-model:value="externalAccountModel.password" type="password" show-password-on="click" />
          </NFormItem>
        </NGi>
        <NGi v-if="externalAccountModel.authMode === 'OAUTH2'" span="24">
          <NFormItem path="oauthRefreshToken" :label="$t('page.mail.oauthToken')">
            <NInput v-model:value="externalAccountModel.oauthRefreshToken" type="password" show-password-on="click" />
          </NFormItem>
        </NGi>
        <NGi span="16">
          <NFormItem path="imap.host" :label="$t('page.mail.imapHost')">
            <NInput v-model:value="externalAccountModel.imap.host" />
          </NFormItem>
        </NGi>
        <NGi span="8">
          <NFormItem path="imap.port" :label="$t('page.mail.imapPort')">
            <NInputNumber v-model:value="externalAccountModel.imap.port" class="w-full" :min="1" :max="65535" />
          </NFormItem>
        </NGi>
        <NGi span="16">
          <NFormItem path="smtp.host" :label="$t('page.mail.smtpHost')">
            <NInput v-model:value="externalAccountModel.smtp.host" />
          </NFormItem>
        </NGi>
        <NGi span="8">
          <NFormItem path="smtp.port" :label="$t('page.mail.smtpPort')">
            <NInputNumber v-model:value="externalAccountModel.smtp.port" class="w-full" :min="1" :max="65535" />
          </NFormItem>
        </NGi>
      </NGrid>
      <NSpace justify="end">
        <NButton quaternary @click="resetExternalAccountModel">{{ $t('common.reset') }}</NButton>
        <NButton type="primary" :loading="externalAccountBusy" @click="submitExternalAccount">
          {{ $t('page.mail.addAccount') }}
        </NButton>
      </NSpace>
    </NForm>
    <NSpace class="mt-14px" vertical>
      <div
        v-for="account in externalAccounts"
        :key="account.accountId"
        class="rounded-6px border border-[var(--n-border-color)] p-10px"
      >
        <div class="flex flex-wrap items-center justify-between gap-8px">
          <span class="break-all text-13px font-600">{{ account.email }}</span>
          <NTag size="small" :bordered="false">{{ account.syncStatus }}</NTag>
        </div>
        <div class="mt-6px break-all text-12px text-[var(--n-text-color-3)]">
          {{ account.provider }} - {{ account.imap.host }}:{{ account.imap.port }}
        </div>
        <NSpace class="mt-10px" size="small" wrap>
          <NButton size="small" @click="editExternalAccount(account)">{{ $t('common.edit') }}</NButton>
          <NButton size="small" @click="runExternalAccountTest(account.accountId)">
            {{ $t('page.mail.testConnection') }}
          </NButton>
          <NButton size="small" type="primary" @click="runExternalAccountSync(account.accountId)">
            {{ $t('page.mail.sync') }}
          </NButton>
          <NPopconfirm @positive-click="removeExternalAccount(account.accountId)">
            <template #trigger>
              <NButton size="small" type="error">{{ $t('page.mail.deleteAccount') }}</NButton>
            </template>
            {{ $t('common.confirmDelete') }}
          </NPopconfirm>
        </NSpace>
      </div>
    </NSpace>
  </NCard>
</template>
