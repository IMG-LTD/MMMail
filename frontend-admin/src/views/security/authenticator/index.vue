<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
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
  NProgress,
  NSpace,
  NSwitch,
  NTag
} from 'naive-ui';
import {
  createAuthenticatorEntry,
  exportAuthenticatorBackup,
  exportAuthenticatorEntries,
  generateAuthenticatorCode,
  importAuthenticatorBackup,
  importAuthenticatorEntries,
  importAuthenticatorQrImage,
  listAuthenticatorEntries,
  readAuthenticatorSecurity,
  updateAuthenticatorSecurity,
  verifyAuthenticatorPin
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'SecurityAuthenticator' });

const CODE_REFRESH_MS = 1000;
const FULL_PROGRESS = 100;
const DEFAULT_LOCK_TIMEOUT_SECONDS = 300;

const entries = ref<Api.Authenticator.Entry[]>([]);
const security = ref<Api.Authenticator.Security | null>(null);
const generatedCodes = ref<Record<string, Api.Authenticator.Code>>({});
const exportedContent = ref('');
const refreshTimer = ref<number | null>(null);
const entryModel = reactive<Api.Authenticator.EntryPayload>({ accountName: '', issuer: '', secretCiphertext: '' });
const importModel = reactive<Api.Authenticator.ImportPayload>({ content: '', format: 'OTPAUTH_URI' });
const qrImportModel = reactive<Api.Authenticator.QrImagePayload>({ dataUrl: '' });
const backupModel = reactive<Api.Authenticator.BackupImportPayload>({ content: '', passphrase: '' });
const pinModel = reactive<Api.Authenticator.PinPayload>({ pin: '' });
const securityModel = reactive<Api.Authenticator.SecurityPayload>({
  syncEnabled: false,
  encryptedBackupEnabled: false,
  pinProtectionEnabled: false,
  lockTimeoutSeconds: DEFAULT_LOCK_TIMEOUT_SECONDS,
  pin: ''
});

const columns = computed(() => [
  { title: $t('page.authenticator.issuer'), key: 'issuer' },
  { title: $t('page.authenticator.account'), key: 'accountName' },
  { title: $t('page.authenticator.algorithm'), key: 'algorithm' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' }
]);

function syncSecurityModel(nextSecurity: Api.Authenticator.Security) {
  securityModel.syncEnabled = nextSecurity.syncEnabled;
  securityModel.encryptedBackupEnabled = nextSecurity.encryptedBackupEnabled;
  securityModel.pinProtectionEnabled = nextSecurity.pinProtectionEnabled;
  securityModel.lockTimeoutSeconds = nextSecurity.lockTimeoutSeconds;
}

function codeFor(entryId: string) {
  return generatedCodes.value[entryId];
}

function codeProgress(entry: Api.Authenticator.Entry) {
  const code = codeFor(entry.id);
  const remaining = code?.expiresInSeconds ?? entry.periodSeconds;
  const period = code?.periodSeconds || entry.periodSeconds;

  return Math.max(0, Math.round((remaining / period) * FULL_PROGRESS));
}

function tickGeneratedCodes() {
  generatedCodes.value = Object.fromEntries(
    Object.entries(generatedCodes.value).map(([entryId, code]) => [
      entryId,
      { ...code, expiresInSeconds: Math.max(0, code.expiresInSeconds - 1) }
    ])
  );
}

async function loadAuthenticator() {
  const [entryResult, securityResult] = await Promise.all([listAuthenticatorEntries(), readAuthenticatorSecurity()]);

  if (!entryResult.error) {
    entries.value = entryResult.data;
  }

  if (!securityResult.error) {
    security.value = securityResult.data;
    syncSecurityModel(securityResult.data);
  }
}

async function verifyPinIfNeeded() {
  if (!security.value?.pinProtectionEnabled || !security.value.pinConfigured) {
    return true;
  }

  const { data, error } = await verifyAuthenticatorPin({ ...pinModel });
  return !error && data.verified;
}

async function submitEntry() {
  if (!(await verifyPinIfNeeded())) {
    return;
  }

  const { error } = await createAuthenticatorEntry({ ...entryModel });

  if (!error) {
    entryModel.accountName = '';
    entryModel.issuer = '';
    entryModel.secretCiphertext = '';
    await loadAuthenticator();
  }
}

async function generateCodeForEntry(entry: Api.Authenticator.Entry) {
  const { data, error } = await generateAuthenticatorCode(entry.id);

  if (!error) {
    generatedCodes.value = { ...generatedCodes.value, [entry.id]: data };
  }
}

async function generateAllCodes() {
  await Promise.all(entries.value.map(generateCodeForEntry));
}

async function submitQrImport() {
  if (!(await verifyPinIfNeeded())) {
    return;
  }

  const { error } = await importAuthenticatorQrImage({ ...qrImportModel });

  if (!error) {
    qrImportModel.dataUrl = '';
    await loadAuthenticator();
  }
}

async function submitTextImport() {
  if (!(await verifyPinIfNeeded())) {
    return;
  }

  const { error } = await importAuthenticatorEntries({ ...importModel });

  if (!error) {
    importModel.content = '';
    await loadAuthenticator();
  }
}

async function submitSecurity() {
  const { data, error } = await updateAuthenticatorSecurity({ ...securityModel });

  if (!error) {
    security.value = data;
    syncSecurityModel(data);
  }
}

async function exportBackup() {
  const { data, error } = await exportAuthenticatorBackup({ passphrase: backupModel.passphrase });

  if (!error) {
    exportedContent.value = data.content;
  }
}

async function importBackup() {
  if (!(await verifyPinIfNeeded())) {
    return;
  }

  const { error } = await importAuthenticatorBackup({ ...backupModel });

  if (!error) {
    backupModel.content = '';
    await loadAuthenticator();
  }
}

async function exportPlainEntries() {
  const { data, error } = await exportAuthenticatorEntries();

  if (!error) {
    exportedContent.value = data.content;
  }
}

onMounted(async () => {
  await loadAuthenticator();
  await generateAllCodes();
  refreshTimer.value = window.setInterval(tickGeneratedCodes, CODE_REFRESH_MS);
});

onBeforeUnmount(() => {
  if (refreshTimer.value) {
    window.clearInterval(refreshTimer.value);
  }
});
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24">
      <NCard class="card-wrapper" :title="$t('route.security_authenticator')">
        <NSpace class="mb-12px" justify="space-between">
          <NSpace>
            <NTag>
              {{ $t('page.authenticator.sync') }}:
              {{ security?.syncEnabled ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no') }}
            </NTag>
            <NTag>
              {{ $t('page.authenticator.pin') }}:
              {{ security?.pinConfigured ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no') }}
            </NTag>
          </NSpace>
          <NButton @click="generateAllCodes">{{ $t('page.authenticator.generate') }}</NButton>
        </NSpace>
        <div class="grid grid-cols-1 gap-16px sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <NCard v-for="entry in entries" :key="entry.id" size="small">
            <NSpace vertical>
              <NTag type="success">{{ entry.issuer }}</NTag>
              <div class="break-words text-13px">{{ entry.accountName }}</div>
              <div class="font-mono text-28px font-700">{{ codeFor(entry.id)?.code || '------' }}</div>
              <NProgress type="circle" :percentage="codeProgress(entry)" :height="72" />
              <NButton block @click="generateCodeForEntry(entry)">{{ $t('page.authenticator.generate') }}</NButton>
            </NSpace>
          </NCard>
        </div>
        <NDataTable class="mt-16px" :columns="columns" :data="entries" />
      </NCard>
    </NGi>

    <NGi span="24 l:8">
      <NCard class="card-wrapper" :title="$t('page.authenticator.create')">
        <NForm :model="entryModel" label-placement="top">
          <NFormItem path="issuer" :label="$t('page.authenticator.issuer')">
            <NInput v-model:value="entryModel.issuer" />
          </NFormItem>
          <NFormItem path="accountName" :label="$t('page.authenticator.account')">
            <NInput v-model:value="entryModel.accountName" />
          </NFormItem>
          <NFormItem path="secretCiphertext" :label="$t('page.authenticator.secret')">
            <NInput v-model:value="entryModel.secretCiphertext" type="password" />
          </NFormItem>
          <NFormItem path="pin" :label="$t('page.authenticator.pin')">
            <NInput v-model:value="pinModel.pin" type="password" />
          </NFormItem>
          <NButton type="primary" block @click="submitEntry">{{ $t('page.authenticator.create') }}</NButton>
        </NForm>
      </NCard>
    </NGi>

    <NGi span="24 l:8">
      <NCard class="card-wrapper" :title="$t('page.authenticator.import')">
        <NForm :model="importModel" label-placement="top">
          <NFormItem path="dataUrl" :label="$t('page.authenticator.qrImage')">
            <NInput v-model:value="qrImportModel.dataUrl" type="textarea" :autosize="{ minRows: 3 }" />
          </NFormItem>
          <NButton class="mb-12px" block @click="submitQrImport">{{ $t('page.authenticator.import') }}</NButton>
          <NFormItem path="content" :label="$t('page.authenticator.secret')">
            <NInput v-model:value="importModel.content" type="textarea" :autosize="{ minRows: 4 }" />
          </NFormItem>
          <NButton type="primary" block @click="submitTextImport">{{ $t('page.authenticator.import') }}</NButton>
        </NForm>
      </NCard>
    </NGi>

    <NGi span="24 l:8">
      <NCard class="card-wrapper" :title="$t('page.authenticator.settings')">
        <NForm :model="securityModel" label-placement="top">
          <NFormItem path="syncEnabled" :label="$t('page.authenticator.sync')">
            <NSwitch v-model:value="securityModel.syncEnabled" />
          </NFormItem>
          <NFormItem path="pinProtectionEnabled" :label="$t('page.authenticator.pin')">
            <NSwitch v-model:value="securityModel.pinProtectionEnabled" />
          </NFormItem>
          <NFormItem path="lockTimeoutSeconds" :label="$t('page.authenticator.lockTimeout')">
            <NInputNumber v-model:value="securityModel.lockTimeoutSeconds" class="w-full" :min="60" />
          </NFormItem>
          <NFormItem path="pin" :label="$t('page.authenticator.pin')">
            <NInput v-model:value="securityModel.pin" type="password" />
          </NFormItem>
          <NButton type="primary" block @click="submitSecurity">{{ $t('common.confirm') }}</NButton>
        </NForm>
      </NCard>
    </NGi>

    <NGi span="24">
      <NCard class="card-wrapper" :title="$t('page.authenticator.backup')">
        <NForm :model="backupModel" label-placement="top">
          <NFormItem path="passphrase" :label="$t('page.login.common.passwordPlaceholder')">
            <NInput v-model:value="backupModel.passphrase" type="password" />
          </NFormItem>
          <NFormItem path="content" :label="$t('page.authenticator.backup')">
            <NInput v-model:value="backupModel.content" type="textarea" :autosize="{ minRows: 3 }" />
          </NFormItem>
          <NSpace>
            <NButton @click="exportPlainEntries">{{ $t('page.authenticator.exported') }}</NButton>
            <NButton @click="exportBackup">{{ $t('page.authenticator.backup') }}</NButton>
            <NButton type="primary" @click="importBackup">{{ $t('page.authenticator.import') }}</NButton>
          </NSpace>
          <NInput class="mt-12px" :value="exportedContent" type="textarea" readonly :autosize="{ minRows: 3 }" />
        </NForm>
      </NCard>
    </NGi>
  </NGrid>
</template>
