<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import {
  NAlert,
  NButton,
  NCard,
  NDescriptions,
  NDescriptionsItem,
  NForm,
  NFormItem,
  NInput,
  NSpace,
  NTag
} from 'naive-ui';
import { downloadPublicDriveShareFile, readPublicDriveShareMetadata } from '@/service/api';
import { $t } from '@/locales';

defineOptions({
  name: 'PublicShare'
});

const SHARE_PASSWORD_HEADER = 'X-Drive-Share-Password';

const route = useRoute();
const loading = ref(false);
const password = ref('');
const shareKeyFragment = ref('');
const metadata = ref<Api.Drive.PublicShareMetadata | null>(null);

const token = computed(() => routeParam(route.params.token));

function routeParam(value: string | string[] | undefined) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }

  return value || '';
}

function decodeSecureShareFragment(hash: string) {
  const content = hash.startsWith('#') ? hash.slice(1) : hash;
  const params = new URLSearchParams(content);

  return params.get('k') || '';
}

function downloadName(item: Api.Drive.PublicShareMetadata) {
  if (!item.e2ee?.enabled) {
    return item.itemName;
  }

  return item.itemName.endsWith('.pgp') ? item.itemName : `${item.itemName}.pgp`;
}

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

async function loadShare() {
  if (!token.value) {
    return;
  }

  loading.value = true;
  const { data, error } = await readPublicDriveShareMetadata(token.value);
  loading.value = false;

  if (!error) {
    metadata.value = data;
  }
}

async function downloadShare() {
  if (!metadata.value || !token.value) {
    return;
  }

  const { data, error } = await downloadPublicDriveShareFile(token.value, metadata.value.itemId, password.value);

  if (!error) {
    saveBlob(data, downloadName(metadata.value));
  }
}

onMounted(async () => {
  shareKeyFragment.value = decodeSecureShareFragment(window.location.hash);
  await loadShare();
});
</script>

<template>
  <main class="min-h-screen bg-layout p-16px md:p-32px">
    <NSpace vertical :size="16" class="mx-auto max-w-760px">
      <NCard :title="$t('page.publicShare.title')" :loading="loading">
        <NSpace v-if="metadata" vertical :size="16">
          <NSpace align="center">
            <NTag v-if="metadata.e2ee?.enabled" type="success">{{ $t('page.publicShare.encrypted') }}</NTag>
            <NTag>{{ metadata.permission }}</NTag>
          </NSpace>
          <NDescriptions bordered :column="1">
            <NDescriptionsItem :label="$t('page.drive.fileName')">{{ metadata.itemName }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.drive.sizeBytes')">{{ metadata.sizeBytes }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.calendar.endAt')">{{ metadata.expiresAt || '-' }}</NDescriptionsItem>
          </NDescriptions>
          <!-- info-only, see v213-closure-spec-v1.1 §2.1 -->
          <NAlert type="info" :title="$t('page.publicShare.localKeyNotice')">
            {{ SHARE_PASSWORD_HEADER }} · {{ shareKeyFragment || '-' }}
          </NAlert>
          <NForm label-placement="top">
            <NFormItem v-if="metadata.passwordProtected" :label="$t('page.publicShare.password')">
              <NInput v-model:value="password" type="password" show-password-on="click" />
            </NFormItem>
            <NSpace justify="end">
              <NButton type="primary" @click="downloadShare">{{ $t('page.publicShare.download') }}</NButton>
            </NSpace>
          </NForm>
        </NSpace>
      </NCard>
    </NSpace>
  </main>
</template>
