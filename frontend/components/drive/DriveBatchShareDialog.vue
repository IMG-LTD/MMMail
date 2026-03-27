<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import DriveErrorBanner from '~/components/drive/DriveErrorBanner.vue'
import { useDriveApi } from '~/composables/useDriveApi'
import { useI18n } from '~/composables/useI18n'
import type { DriveBatchShareResult, DriveItem, DriveSharePermission } from '~/types/api'

const props = defineProps<{
  modelValue: boolean
  items: DriveItem[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: []
}>()

const { t } = useI18n()
const { batchCreateShares } = useDriveApi()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const submitting = ref(false)
const result = ref<DriveBatchShareResult | null>(null)
const errorMessage = ref('')
const form = reactive({
  permission: 'VIEW' as DriveSharePermission,
  expiresAt: '',
  password: '',
})

const itemNameMap = computed(() => Object.fromEntries(props.items.map((item) => [item.id, item.name] as const)))

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      return
    }
    errorMessage.value = ''
    result.value = null
    form.permission = 'VIEW'
    form.expiresAt = ''
    form.password = ''
  },
)

function formatSummaryName(index: number): string {
  return props.items[index]?.name || props.items[index]?.id || '—'
}

function resolveItemName(itemId: string): string {
  return itemNameMap.value[itemId] || itemId
}

function buildShareUrl(token: string): string {
  if (typeof window === 'undefined') {
    return `/public/drive/shares/${token}`
  }
  return `${window.location.origin}/public/drive/shares/${token}`
}

async function copyToClipboard(value: string, successKey: string): Promise<void> {
  if (typeof navigator === 'undefined' || !navigator.clipboard) {
    ElMessage.error(t('drive.messages.clipboardUnavailable'))
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(t(successKey))
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.copyFailed'))
  }
}

async function onSubmit(): Promise<void> {
  if (!props.items.length) {
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    result.value = await batchCreateShares({
      itemIds: props.items.map((item) => item.id),
      permission: form.permission,
      expiresAt: form.expiresAt || undefined,
      password: form.password.trim() || undefined,
    })
    emit('created')
    ElMessage.success(t('drive.messages.batchSharesCreated', {
      success: result.value.successCount,
      requested: result.value.requestedCount,
    }))
  } catch (error) {
    result.value = null
    errorMessage.value = (error as Error).message || t('drive.messages.batchShareFailed')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="dialogVisible" :title="t('drive.batch.shareDialog.title')" width="720px">
    <div class="batch-share-dialog">
      <p class="muted">
        {{ t('drive.batch.shareDialog.description', { count: items.length }) }}
      </p>

      <DriveErrorBanner
        v-if="errorMessage"
        test-id="drive-batch-share-error"
        :title="t('drive.batch.shareDialog.errorTitle')"
        :message="errorMessage"
        @retry="onSubmit"
        @dismiss="errorMessage = ''"
      />

      <section class="batch-share-dialog__section">
        <h3>{{ t('drive.batch.shareDialog.selectedTitle') }}</h3>
        <ul class="batch-share-dialog__selected">
          <li
            v-for="(_, index) in items.slice(0, 5)"
            :key="items[index]?.id || index"
            :data-testid="`drive-batch-share-item-${index}`"
          >
            {{ formatSummaryName(index) }}
          </li>
        </ul>
      </section>

      <section class="batch-share-dialog__form">
        <el-select v-model="form.permission" :placeholder="t('drive.shareDrawer.columns.permission')">
          <el-option :label="t('docs.share.view')" value="VIEW" />
          <el-option :label="t('docs.share.edit')" value="EDIT" />
        </el-select>
        <el-date-picker
          v-model="form.expiresAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          :placeholder="t('drive.shareDrawer.expiresAtPlaceholder')"
        />
        <el-input
          v-model="form.password"
          :placeholder="t('drive.shareDrawer.passwordPlaceholder')"
          show-password
        />
      </section>

      <section v-if="result" class="batch-share-dialog__section" data-testid="drive-batch-share-result">
        <h3>{{ t('drive.batch.shareDialog.resultsTitle') }}</h3>
        <p class="muted">
          {{ t('drive.batch.shareDialog.resultSummary', {
            success: result.successCount,
            failed: result.failedCount,
            requested: result.requestedCount,
          }) }}
        </p>

        <div class="batch-share-dialog__results">
          <article
            v-for="share in result.createdShares"
            :key="share.id"
            class="batch-share-dialog__result-card"
          >
            <div>
              <strong>{{ resolveItemName(share.itemId) }}</strong>
              <p class="muted">{{ buildShareUrl(share.token) }}</p>
            </div>
            <div class="batch-share-dialog__result-actions">
              <el-button
                size="small"
                text
                :data-testid="`drive-batch-share-copy-token-${share.id}`"
                @click="copyToClipboard(share.token, 'drive.messages.shareTokenCopied')"
              >
                {{ t('drive.shareDrawer.actions.copyToken') }}
              </el-button>
              <el-button
                size="small"
                text
                :data-testid="`drive-batch-share-copy-link-${share.id}`"
                @click="copyToClipboard(buildShareUrl(share.token), 'drive.messages.shareLinkCopied')"
              >
                {{ t('drive.shareDrawer.actions.copyLink') }}
              </el-button>
            </div>
          </article>
        </div>

        <ul v-if="result.failedItems.length" class="batch-share-dialog__failures">
          <li v-for="failure in result.failedItems" :key="failure.itemId">
            {{ resolveItemName(failure.itemId) }} — {{ failure.reason }}
          </li>
        </ul>
      </section>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="items.length === 0"
        data-testid="drive-batch-share-submit"
        @click="onSubmit"
      >
        {{ t('drive.batch.shareAction') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.batch-share-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.batch-share-dialog__section h3,
.batch-share-dialog__section p,
.muted {
  margin: 0;
}

.muted {
  color: var(--mm-muted);
}

.batch-share-dialog__selected,
.batch-share-dialog__failures {
  margin: 0;
  padding-left: 18px;
}

.batch-share-dialog__form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.batch-share-dialog__results {
  display: grid;
  gap: 10px;
}

.batch-share-dialog__result-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  background: rgba(247, 250, 255, 0.9);
}

.batch-share-dialog__result-actions {
  display: inline-flex;
  gap: 6px;
  flex-wrap: wrap;
  align-items: flex-start;
}

@media (max-width: 900px) {
  .batch-share-dialog__form {
    grid-template-columns: 1fr;
  }

  .batch-share-dialog__result-card {
    flex-direction: column;
  }
}
</style>
