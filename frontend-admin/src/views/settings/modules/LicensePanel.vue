<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { NButton, NCard, NDescriptions, NDescriptionsItem, NForm, NFormItem, NInput, NSpace, NTag } from 'naive-ui';
import { readCommercialLicenseStatus, uploadCommercialLicense } from '@/service/api';
import { $t } from '@/locales';

defineOptions({
  name: 'SettingsLicensePanel'
});

type LicenseTagType = 'default' | 'error' | 'info' | 'success' | 'warning';

const LICENSE_INPUT_ROWS = { minRows: 4, maxRows: 8 } as const;

const licenseKey = ref('');
const licenseStatus = ref<Api.Billing.CommercialLicenseStatus | null>(null);
const loading = ref(false);
const uploading = ref(false);

const currentState = computed(() => licenseStatus.value?.state || 'MISSING');
const currentEdition = computed(() => licenseStatus.value?.edition || $t('page.license.noEdition'));
const currentFeatures = computed(() => licenseStatus.value?.features.join(', ') || $t('common.noData'));
const externalBillingStatus = computed(() => licenseStatus.value?.externalBillingStatus || $t('common.noData'));
const expiresAt = computed(() => licenseStatus.value?.expiresAt || $t('common.noData'));
const syncedAt = computed(() => licenseStatus.value?.syncedAt || $t('common.noData'));

const stateTagType = computed<LicenseTagType>(() => {
  if (currentState.value === 'ACTIVE') return 'success';
  if (currentState.value === 'EXPIRED') return 'warning';
  if (currentState.value === 'INVALID') return 'error';
  return 'default';
});

async function loadLicenseStatus() {
  loading.value = true;
  try {
    const { data, error } = await readCommercialLicenseStatus();
    if (!error) {
      licenseStatus.value = data;
    }
  } finally {
    loading.value = false;
  }
}

async function submitLicense() {
  const normalizedLicenseKey = licenseKey.value.trim();

  if (!normalizedLicenseKey) {
    window.$message?.warning($t('page.license.licenseKeyRequired'));
    return;
  }

  uploading.value = true;
  try {
    const { data, error } = await uploadCommercialLicense({ licenseKey: normalizedLicenseKey });
    if (!error) {
      licenseStatus.value = data;
      licenseKey.value = '';
      window.$message?.success($t('page.license.uploadAccepted'));
    }
  } finally {
    uploading.value = false;
  }
}

onMounted(loadLicenseStatus);
</script>

<template>
  <NCard class="card-wrapper license-panel" :title="$t('page.license.title')">
    <NSpace data-testid="license-a11y-scope" vertical :size="16">
      <NDescriptions :column="1" bordered size="small">
        <NDescriptionsItem :label="$t('page.license.state')">
          <NSpace align="center">
            <NTag :type="stateTagType">{{ currentState }}</NTag>
            <span>{{ currentEdition }}</span>
          </NSpace>
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.license.features')">
          {{ currentFeatures }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.license.externalBillingStatus')">
          {{ externalBillingStatus }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.license.expiresAt')">
          {{ expiresAt }}
        </NDescriptionsItem>
        <NDescriptionsItem :label="$t('page.license.syncedAt')">
          {{ syncedAt }}
        </NDescriptionsItem>
      </NDescriptions>

      <NForm label-placement="top">
        <NFormItem path="licenseKey" :label="$t('page.license.licenseKey')">
          <NInput
            v-model:value="licenseKey"
            type="textarea"
            :input-props="{ 'aria-label': $t('page.license.licenseKey') }"
            :autosize="LICENSE_INPUT_ROWS"
            :placeholder="$t('page.license.licenseKeyPlaceholder')"
          />
        </NFormItem>
        <NSpace>
          <NButton type="primary" :loading="uploading" @click="submitLicense">
            {{ $t('page.license.upload') }}
          </NButton>
          <NButton :loading="loading" @click="loadLicenseStatus">
            {{ $t('page.license.refresh') }}
          </NButton>
        </NSpace>
      </NForm>
    </NSpace>
  </NCard>
</template>

<style scoped>
.license-panel :deep(.n-descriptions-table-content) {
  word-break: break-word;
}
</style>
