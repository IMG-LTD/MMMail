<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
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
  NModal,
  NProgress,
  NSelect,
  NSpace,
  NTag,
  NTimeline,
  NTimelineItem,
  NTree,
  NUpload,
  NUploadDragger
} from 'naive-ui';
import type { DataTableRowKey, TreeOption, UploadCustomRequestOptions } from 'naive-ui';
import {
  createEncryptedDriveShare,
  createDriveShare,
  createDriveUpload,
  deleteDriveFile,
  listDriveFileVersions,
  listDriveFolders,
  listDriveItems,
  readPublicShareCapabilities,
  readDriveUsage,
  restoreDriveFileVersion
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({
  name: 'Drive'
});

const VERSION_LIMIT = 20;
const MIN_COMPARE_VERSION_COUNT = 2;
const READABLE_SHARE_E2EE_ALGORITHM = 'openpgp-password';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const shareOpen = ref(false);
const secureShareOpen = ref(false);
const activeFolderId = ref<string | null>(null);
const selectedRowKeys = ref<DataTableRowKey[]>([]);
const selectedFolderKeys = ref<DataTableRowKey[]>([]);
const files = ref<Api.Drive.Item[]>([]);
const folders = ref<Api.Drive.Item[]>([]);
const versions = ref<Api.Drive.Version[]>([]);
const secureShareFile = ref<File | null>(null);
const secureShareFileName = ref('');
const secureShareUrl = ref('');
const shareCapabilities = ref<Api.Drive.PublicShareCapabilities | null>(null);
const usage = ref<Api.Drive.Usage | null>(null);

const shareModel = reactive<Api.Drive.SharePayload>({
  permission: 'VIEW',
  expiresAt: null
});

const secureShareModel = reactive({
  permission: 'VIEW' as const,
  expiresAt: null as string | null,
  password: '',
  localKey: '',
  e2eeAlgorithm: READABLE_SHARE_E2EE_ALGORITHM
});

const columns = computed(() => [
  { type: 'selection' as const },
  { title: $t('page.drive.fileName'), key: 'name' },
  { title: $t('page.drive.sizeBytes'), key: 'sizeBytes' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' }
]);

const folderTree = computed<TreeOption[]>(() =>
  folders.value.map(folder => ({
    key: folder.id,
    label: folder.name
  }))
);

const storagePercent = computed(() => {
  if (!usage.value?.storageLimitBytes) {
    return 0;
  }

  return Math.round((usage.value.storageBytes / usage.value.storageLimitBytes) * 100);
});
const routeFileId = computed(() => routeParam(route.params.fileId));
const routeCompareA = computed(() => routeParam(route.params.verA));
const routeCompareB = computed(() => routeParam(route.params.verB));
const activeComparePair = computed(() => {
  if (!routeCompareA.value || !routeCompareB.value) {
    return '';
  }

  return `${routeCompareA.value} / ${routeCompareB.value}`;
});

function routeParam(value: string | string[] | undefined) {
  if (Array.isArray(value)) {
    return value[0] || '';
  }

  return value || '';
}

function rowKey(row: Api.Drive.Item) {
  return row.id;
}

function selectedFileId() {
  return routeFileId.value || String(selectedRowKeys.value[0] || '');
}

async function loadDrive() {
  loading.value = true;
  const [filesResult, foldersResult, usageResult] = await Promise.all([
    listDriveItems({ parentId: activeFolderId.value || undefined }),
    listDriveFolders(),
    readDriveUsage()
  ]);

  if (!filesResult.error) {
    files.value = filesResult.data;
  }

  if (!foldersResult.error) {
    folders.value = foldersResult.data;
  }

  if (!usageResult.error) {
    usage.value = usageResult.data;
  }

  loading.value = false;
}

async function loadShareCapabilities() {
  const { data, error } = await readPublicShareCapabilities();

  if (!error) {
    shareCapabilities.value = data;
  }
}

async function handleUpload(options: UploadCustomRequestOptions) {
  const file = options.file.file;

  if (!file) {
    options.onError();
    return;
  }

  const { error } = await createDriveUpload({
    fileName: file.name,
    parentId: activeFolderId.value,
    sizeBytes: file.size
  });

  if (error) {
    options.onError();
    return;
  }

  options.onFinish();
  await loadDrive();
}

async function deleteSelected() {
  const fileId = selectedFileId();

  if (!fileId) {
    return;
  }

  const { error } = await deleteDriveFile(fileId);

  if (!error) {
    selectedRowKeys.value = [];
    await loadDrive();
  }
}

async function submitShare() {
  const fileId = selectedFileId();

  if (!fileId) {
    return;
  }

  const { error } = await createDriveShare(fileId, shareModel);

  if (!error) {
    shareOpen.value = false;
    window.$message?.success($t('common.updateSuccess'));
  }
}

function selectSecureShareFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const [file] = Array.from(input.files || []);
  secureShareFile.value = file || null;
  secureShareFileName.value = file?.name || '';
}

function buildSecureShareFragment(localKey: string) {
  const normalized = localKey.trim();

  return normalized ? `#k=${encodeURIComponent(normalized)}` : '';
}

function openSecureShareRoute() {
  const fileId = selectedFileId();

  if (!fileId) {
    return;
  }

  secureShareOpen.value = true;
  router.push({ name: 'drive_file_secure_share', params: { fileId } });
}

async function submitEncryptedShare() {
  const fileId = selectedFileId();
  const encryptedFile = secureShareFile.value;

  if (!fileId || !encryptedFile || !secureShareModel.password) {
    window.$message?.error($t('form.required'));
    return;
  }

  const { data, error } = await createEncryptedDriveShare(fileId, {
    permission: secureShareModel.permission,
    expiresAt: secureShareModel.expiresAt,
    password: secureShareModel.password,
    e2eeAlgorithm: secureShareModel.e2eeAlgorithm,
    encryptedFile
  });

  if (!error) {
    secureShareUrl.value = `${window.location.origin}/share/${data.token}${buildSecureShareFragment(secureShareModel.localKey)}`;
    window.$message?.success($t('common.updateSuccess'));
  }
}

async function loadVersions() {
  const fileId = selectedFileId();

  if (!fileId) {
    return;
  }

  const { data, error } = await listDriveFileVersions(fileId, { limit: VERSION_LIMIT });

  if (!error) {
    versions.value = data;
  }
}

async function restoreLatestVersion() {
  const [version] = versions.value;

  if (!version) {
    return;
  }

  restoreVersion(version);
}

async function restoreConfirmedVersion(fileId: string, version: Api.Drive.Version) {
  const { error } = await restoreDriveFileVersion(fileId, version.id);

  if (!error) {
    window.$message?.success($t('common.updateSuccess'));
    await loadDrive();
    await loadVersions();
  }
}

function restoreVersion(version: Api.Drive.Version) {
  const fileId = selectedFileId();

  if (!fileId) {
    return;
  }

  window.$dialog?.warning({
    title: $t('page.driveVersions.restore'),
    content: $t('page.driveVersions.restoreConfirm'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: () => restoreConfirmedVersion(fileId, version)
  });
}

function openVersionRoute() {
  const fileId = selectedFileId();

  if (!fileId) {
    return;
  }

  router.push({ name: 'drive_file_versions', params: { fileId } });
}

function openVersionCompare(version: Api.Drive.Version, index: number) {
  const fileId = selectedFileId();
  const baseline = versions.value[index + 1] || versions.value[index - 1];

  if (!fileId || !baseline) {
    return;
  }

  router.push({ name: 'drive_file_version_compare', params: { fileId, verA: version.id, verB: baseline.id } });
}

async function selectFolder(keys: DataTableRowKey[]) {
  selectedFolderKeys.value = keys;
  activeFolderId.value = keys.length ? String(keys[0]) : null;
  await loadDrive();
}

function syncSecureShareRoute() {
  if (route.name === 'drive_file_secure_share') {
    secureShareOpen.value = true;
  }
}

onMounted(async () => {
  await loadDrive();
  await loadShareCapabilities();

  if (route.params.fileId) {
    await loadVersions();
  }

  syncSecureShareRoute();
});

watch(routeFileId, async fileId => {
  if (!fileId) {
    return;
  }

  selectedRowKeys.value = [fileId];
  await loadVersions();
});

watch(() => route.name, syncSecureShareRoute);
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 m:5">
      <NCard class="card-wrapper" :title="$t('page.drive.folders')">
        <NTree block-line :data="folderTree" :selected-keys="selectedFolderKeys" @update:selected-keys="selectFolder" />
      </NCard>
    </NGi>

    <NGi span="24 m:19">
      <NSpace vertical :size="16">
        <NCard class="card-wrapper" :title="$t('page.drive.storage')">
          <NProgress type="line" :percentage="storagePercent" />
        </NCard>

        <NCard class="card-wrapper" :title="$t('route.drive')">
          <NUpload :custom-request="handleUpload" :show-file-list="false">
            <NUploadDragger>
              <NButton type="primary">{{ $t('page.drive.upload') }}</NButton>
            </NUploadDragger>
          </NUpload>

          <NSpace class="my-12px" justify="end">
            <NButton @click="shareOpen = true">{{ $t('page.drive.share') }}</NButton>
            <NButton @click="openSecureShareRoute">{{ $t('page.driveSecureShare.title') }}</NButton>
            <NButton @click="openVersionRoute">{{ $t('page.driveVersions.title') }}</NButton>
            <NButton type="error" @click="deleteSelected">{{ $t('page.drive.deleteSelected') }}</NButton>
          </NSpace>

          <NDataTable
            v-model:checked-row-keys="selectedRowKeys"
            :columns="columns"
            :data="files"
            :loading="loading"
            :row-key="rowKey"
          />
        </NCard>
        <NCard class="card-wrapper" :title="$t('page.driveVersions.title')">
          <NSpace class="mb-12px" justify="space-between">
            <NSpace>
              <NTag>
                {{
                  shareCapabilities?.supportsPasswordUnlock ? $t('page.driveVersions.e2eeReady') : $t('common.noData')
                }}
              </NTag>
              <NTag v-if="activeComparePair" type="info">
                {{ $t('page.driveVersions.compare') }} {{ activeComparePair }}
              </NTag>
            </NSpace>
            <NButton @click="restoreLatestVersion">{{ $t('page.driveVersions.restore') }}</NButton>
          </NSpace>
          <NTimeline>
            <NTimelineItem v-for="(version, index) in versions" :key="version.id" :time="version.createdAt" type="info">
              <NSpace vertical :size="8">
                <NSpace align="center" justify="space-between">
                  <NSpace align="center">
                    <NTag type="success">{{ $t('page.driveVersions.version') }} {{ version.versionNo }}</NTag>
                    <NTag>{{ $t('page.drive.sizeBytes') }} {{ version.sizeBytes }}</NTag>
                    <NTag>{{ $t('page.driveVersions.author') }} {{ version.authorUserId || version.itemId }}</NTag>
                  </NSpace>
                  <NSpace>
                    <NButton size="small" @click="restoreVersion(version)">
                      {{ $t('page.driveVersions.restore') }}
                    </NButton>
                    <NButton
                      size="small"
                      :disabled="versions.length < MIN_COMPARE_VERSION_COUNT"
                      @click="openVersionCompare(version, index)"
                    >
                      {{ $t('page.driveVersions.compare') }}
                    </NButton>
                  </NSpace>
                </NSpace>
                <NTag :bordered="false">{{ version.changeSummary || version.checksum }}</NTag>
              </NSpace>
            </NTimelineItem>
          </NTimeline>
        </NCard>
      </NSpace>
    </NGi>
  </NGrid>

  <NModal v-model:show="shareOpen" preset="card" class="max-w-480px" :title="$t('page.drive.share')">
    <NForm :model="shareModel" label-placement="top">
      <NFormItem path="permission" :label="$t('page.drive.permission')">
        <NSelect
          v-model:value="shareModel.permission"
          :options="[
            { label: 'VIEW', value: 'VIEW' },
            { label: 'EDIT', value: 'EDIT' }
          ]"
        />
      </NFormItem>
      <NFormItem path="expiresAt" :label="$t('page.calendar.endAt')">
        <NInput v-model:value="shareModel.expiresAt" />
      </NFormItem>
      <NSpace justify="end">
        <NButton type="primary" @click="submitShare">{{ $t('common.confirm') }}</NButton>
      </NSpace>
    </NForm>
  </NModal>

  <NModal v-model:show="secureShareOpen" preset="card" class="max-w-520px" :title="$t('page.driveSecureShare.title')">
    <NForm :model="secureShareModel" label-placement="top">
      <NFormItem :label="$t('page.driveSecureShare.encryptedCopy')">
        <input type="file" @change="selectSecureShareFile" />
        <NTag v-if="secureShareFileName" class="ml-8px">{{ secureShareFileName }}</NTag>
      </NFormItem>
      <NFormItem path="expiresAt" :label="$t('page.calendar.endAt')">
        <NInput v-model:value="secureShareModel.expiresAt" />
      </NFormItem>
      <NFormItem path="password" :label="$t('page.driveSecureShare.sharePassword')">
        <NInput v-model:value="secureShareModel.password" type="password" show-password-on="click" />
      </NFormItem>
      <NFormItem path="localKey" :label="$t('page.driveSecureShare.localKey')">
        <NInput v-model:value="secureShareModel.localKey" type="password" show-password-on="click" />
      </NFormItem>
      <NTag type="warning" :bordered="false">{{ $t('page.driveSecureShare.localKeyWarning') }}</NTag>
      <NFormItem v-if="secureShareUrl" class="mt-12px" :label="$t('page.driveSecureShare.secureLink')">
        <NInput :value="secureShareUrl" type="textarea" readonly />
      </NFormItem>
      <NSpace justify="end">
        <NButton type="primary" @click="submitEncryptedShare">{{ $t('page.driveSecureShare.create') }}</NButton>
      </NSpace>
    </NForm>
  </NModal>
</template>
