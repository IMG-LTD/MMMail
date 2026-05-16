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
import {
  downloadPublicDriveShareFile,
  downloadPublicMailAttachment,
  readPublicDriveShareMetadata,
  readPublicMailShare,
  readPublicPassShare
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({
  name: 'PublicShare'
});

type ShareKind = 'drive' | 'mail' | 'pass';

const SHARE_PASSWORD_HEADER = 'X-Drive-Share-Password';

const route = useRoute();
const loading = ref(false);
const password = ref('');
const shareKeyFragment = ref('');
const driveShare = ref<Api.Drive.PublicShareMetadata | null>(null);
const mailShare = ref<Api.PublicShare.MailShare | null>(null);
const passShare = ref<Api.PublicShare.PassShare | null>(null);

const token = computed(() => routeParam(route.params.token));
const shareKind = computed(resolveShareKind);

function routeParam(value: string | string[] | undefined) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }

  return value || '';
}

function resolveShareKind(): ShareKind {
  const routeName = String(route.name || '');
  if (routeName.includes('mail')) return 'mail';
  if (routeName.includes('pass')) return 'pass';
  return 'drive';
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
  await loadCurrentShare();
  loading.value = false;
}

async function loadCurrentShare() {
  if (shareKind.value === 'mail') {
    const { data, error } = await readPublicMailShare(token.value);
    if (!error) mailShare.value = data;
    return;
  }
  if (shareKind.value === 'pass') {
    const { data, error } = await readPublicPassShare(token.value);
    if (!error) passShare.value = data;
    return;
  }
  const { data, error } = await readPublicDriveShareMetadata(token.value);
  if (!error) driveShare.value = data;
}

async function downloadDriveShare() {
  if (!driveShare.value || !token.value) {
    return;
  }

  const { data, error } = await downloadPublicDriveShareFile(token.value, driveShare.value.itemId, password.value);

  if (!error) {
    saveBlob(data, downloadName(driveShare.value));
  }
}

async function downloadMailAttachment(attachment: Api.PublicShare.MailAttachment) {
  const { data, error } = await downloadPublicMailAttachment(token.value, attachment.id);
  if (!error) {
    saveBlob(data, attachment.fileName);
  }
}

async function copyPassSecret() {
  if (!passShare.value?.secretCiphertext) {
    return;
  }
  await navigator.clipboard.writeText(passShare.value.secretCiphertext);
  window.$message?.success($t('request.copyDetailsSuccess'));
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
        <NSpace v-if="driveShare" vertical :size="16">
          <NSpace align="center">
            <NTag v-if="driveShare.e2ee?.enabled" type="success">{{ $t('page.publicShare.encrypted') }}</NTag>
            <NTag>{{ driveShare.permission }}</NTag>
          </NSpace>
          <NDescriptions bordered :column="1">
            <NDescriptionsItem :label="$t('page.drive.fileName')">{{ driveShare.itemName }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.drive.sizeBytes')">{{ driveShare.sizeBytes }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.calendar.endAt')">{{ driveShare.expiresAt || '-' }}</NDescriptionsItem>
          </NDescriptions>
          <!-- info-only, see v213-closure-spec-v1.1 §2.1 -->
          <NAlert type="info" :title="$t('page.publicShare.localKeyNotice')">
            {{ SHARE_PASSWORD_HEADER }} · {{ shareKeyFragment || '-' }}
          </NAlert>
          <NForm label-placement="top">
            <NFormItem v-if="driveShare.passwordProtected" :label="$t('page.publicShare.password')">
              <NInput v-model:value="password" type="password" show-password-on="click" />
            </NFormItem>
            <NSpace justify="end">
              <NButton type="primary" @click="downloadDriveShare">{{ $t('page.publicShare.download') }}</NButton>
            </NSpace>
          </NForm>
        </NSpace>

        <NSpace v-else-if="mailShare" vertical :size="16">
          <NDescriptions bordered :column="1">
            <NDescriptionsItem :label="$t('page.mail.subject')">{{ mailShare.subject }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.mail.sender')">{{ mailShare.senderEmail }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.mail.to')">{{ mailShare.recipientEmail }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.calendar.endAt')">{{ mailShare.expiresAt || '-' }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.authenticator.algorithm')">{{ mailShare.algorithm }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.publicShare.passwordHint')">
              {{ mailShare.passwordHint || '-' }}
            </NDescriptionsItem>
          </NDescriptions>
          <NInput :value="mailShare.bodyCiphertext" type="textarea" readonly />
          <NSpace v-if="mailShare.attachments?.length" vertical>
            <NButton
              v-for="attachment in mailShare.attachments"
              :key="attachment.id"
              secondary
              @click="downloadMailAttachment(attachment)"
            >
              {{ attachment.fileName }}
            </NButton>
          </NSpace>
        </NSpace>

        <NSpace v-else-if="passShare" vertical :size="16">
          <NDescriptions bordered :column="1">
            <NDescriptionsItem :label="$t('page.pass.title')">{{ passShare.title }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.pass.username')">{{ passShare.username }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.pass.website')">{{ passShare.website || '-' }}</NDescriptionsItem>
            <NDescriptionsItem :label="$t('page.calendar.endAt')">{{ passShare.expiresAt || '-' }}</NDescriptionsItem>
          </NDescriptions>
          <NInput :value="passShare.secretCiphertext" type="textarea" readonly />
          <NInput v-if="passShare.note" :value="passShare.note" type="textarea" readonly />
          <NSpace justify="end">
            <NTag>{{ passShare.currentViews }} / {{ passShare.maxViews }}</NTag>
            <NButton type="primary" @click="copyPassSecret">{{ $t('request.copyDetails') }}</NButton>
          </NSpace>
        </NSpace>
      </NCard>
    </NSpace>
  </main>
</template>
