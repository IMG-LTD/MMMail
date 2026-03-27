<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { AuthenticatorAlgorithm, AuthenticatorCodePayload } from '~/types/api'
import type { PassMonitorItem, UpsertPassItemTwoFactorRequest } from '~/types/pass-business'
import { useI18n } from '~/composables/useI18n'

const DEFAULT_ALGORITHM: AuthenticatorAlgorithm = 'SHA1'
const DEFAULT_DIGITS = 6
const DEFAULT_PERIOD_SECONDS = 30
const MIN_DIGITS = 6
const MAX_DIGITS = 8
const MIN_PERIOD_SECONDS = 15
const MAX_PERIOD_SECONDS = 120
const TIMER_INTERVAL_MS = 1000

const props = withDefaults(defineProps<{
  modelValue: boolean
  item: PassMonitorItem | null
  saving?: boolean
  removing?: boolean
  codeLoading?: boolean
  code: AuthenticatorCodePayload | null
}>(), {
  saving: false,
  removing: false,
  codeLoading: false,
  code: null
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [payload: UpsertPassItemTwoFactorRequest]
  remove: []
  generate: []
}>()

const { t } = useI18n()
const remainingSeconds = ref(0)
const timerId = ref<ReturnType<typeof setInterval> | null>(null)
const algorithmOptions: AuthenticatorAlgorithm[] = ['SHA1', 'SHA256', 'SHA512']
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const form = reactive<UpsertPassItemTwoFactorRequest>({
  issuer: '',
  accountName: '',
  secretCiphertext: '',
  algorithm: DEFAULT_ALGORITHM,
  digits: DEFAULT_DIGITS,
  periodSeconds: DEFAULT_PERIOD_SECONDS
})

const statusLabel = computed(() => {
  return props.item?.twoFactor.enabled
    ? t('pass.monitor.dialog.enabled')
    : t('pass.monitor.dialog.missing')
})

watch(
  () => [props.modelValue, props.item?.id, props.item?.twoFactor.updatedAt] as const,
  ([visible]) => {
    if (!visible) {
      stopTimer()
      remainingSeconds.value = 0
      return
    }
    syncForm(props.item)
  },
  { immediate: true }
)

watch(
  () => props.code,
  (code) => {
    startTimer(code?.expiresInSeconds || 0)
  },
  { immediate: true }
)

onBeforeUnmount(stopTimer)

function syncForm(item: PassMonitorItem | null): void {
  form.issuer = item?.twoFactor.issuer || item?.title || ''
  form.accountName = item?.twoFactor.accountName || item?.username || ''
  form.secretCiphertext = ''
  form.algorithm = item?.twoFactor.algorithm || DEFAULT_ALGORITHM
  form.digits = item?.twoFactor.digits || DEFAULT_DIGITS
  form.periodSeconds = item?.twoFactor.periodSeconds || DEFAULT_PERIOD_SECONDS
}

function startTimer(seconds: number): void {
  stopTimer()
  remainingSeconds.value = seconds
  if (seconds <= 0) {
    return
  }
  timerId.value = setInterval(() => {
    if (remainingSeconds.value <= 1) {
      stopTimer()
      remainingSeconds.value = 0
      return
    }
    remainingSeconds.value -= 1
  }, TIMER_INTERVAL_MS)
}

function stopTimer(): void {
  if (!timerId.value) {
    return
  }
  clearInterval(timerId.value)
  timerId.value = null
}

function emitSave(): void {
  const issuer = form.issuer.trim()
  const accountName = form.accountName.trim()
  const secretCiphertext = form.secretCiphertext.trim()
  if (!issuer) {
    ElMessage.warning(t('pass.monitor.messages.issuerRequired'))
    return
  }
  if (!accountName) {
    ElMessage.warning(t('pass.monitor.messages.accountNameRequired'))
    return
  }
  if (!secretCiphertext) {
    ElMessage.warning(t('pass.monitor.messages.secretRequired'))
    return
  }
  emit('save', {
    issuer,
    accountName,
    secretCiphertext,
    algorithm: form.algorithm || DEFAULT_ALGORITHM,
    digits: form.digits || DEFAULT_DIGITS,
    periodSeconds: form.periodSeconds || DEFAULT_PERIOD_SECONDS
  })
}

async function copyCode(): Promise<void> {
  if (!props.code?.code || typeof navigator === 'undefined' || !navigator.clipboard) {
    return
  }
  await navigator.clipboard.writeText(props.code.code)
  ElMessage.success(t('pass.monitor.messages.codeCopied'))
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="t('pass.monitor.dialog.title')"
    width="720px"
    destroy-on-close
  >
    <div v-if="item" class="dialog-shell">
      <section class="summary-card">
        <div>
          <div class="summary-title-row">
            <h2>{{ item.title }}</h2>
            <el-tag :type="item.twoFactor.enabled ? 'success' : 'warning'" round>
              {{ item.twoFactor.enabled ? t('pass.monitor.labels.twoFactorReady') : t('pass.monitor.labels.inactiveTwoFactor') }}
            </el-tag>
          </div>
          <p class="summary-text">{{ t('pass.monitor.dialog.description') }}</p>
          <p class="summary-text">{{ statusLabel }}</p>
        </div>
        <div class="summary-meta">
          <span>{{ item.website || t('pass.monitor.labels.noWebsite') }}</span>
          <span>{{ item.username || t('pass.monitor.labels.noUsername') }}</span>
        </div>
      </section>

      <el-alert :title="t('pass.monitor.dialog.secretHint')" type="info" :closable="false" />

      <el-form label-position="top" class="dialog-form">
        <div class="form-grid">
          <el-form-item :label="t('pass.monitor.dialog.issuer')">
            <el-input v-model="form.issuer" maxlength="128" />
          </el-form-item>

          <el-form-item :label="t('pass.monitor.dialog.accountName')">
            <el-input v-model="form.accountName" maxlength="254" />
          </el-form-item>

          <el-form-item class="secret-field" :label="t('pass.monitor.dialog.secret')">
            <el-input
              v-model="form.secretCiphertext"
              type="password"
              show-password
              maxlength="512"
              autocomplete="off"
            />
          </el-form-item>

          <el-form-item :label="t('pass.monitor.dialog.algorithm')">
            <el-select v-model="form.algorithm">
              <el-option
                v-for="option in algorithmOptions"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('pass.monitor.dialog.digits')">
            <el-input-number v-model="form.digits" :min="MIN_DIGITS" :max="MAX_DIGITS" />
          </el-form-item>

          <el-form-item :label="t('pass.monitor.dialog.periodSeconds')">
            <el-input-number
              v-model="form.periodSeconds"
              :min="MIN_PERIOD_SECONDS"
              :max="MAX_PERIOD_SECONDS"
            />
          </el-form-item>
        </div>
      </el-form>

      <section class="code-card">
        <div class="code-head">
          <div>
            <h3>{{ t('pass.monitor.dialog.code') }}</h3>
            <p>{{ t('pass.monitor.dialog.codeHint') }}</p>
          </div>
          <div class="code-actions">
            <el-button
              type="primary"
              plain
              :disabled="!item.twoFactor.enabled"
              :loading="codeLoading"
              @click="emit('generate')"
            >
              {{ t('pass.monitor.actions.generateCode') }}
            </el-button>
            <el-button
              :disabled="!code?.code"
              @click="copyCode"
            >
              {{ t('pass.monitor.actions.copyCode') }}
            </el-button>
          </div>
        </div>

        <div v-if="code" class="code-body">
          <div class="code-value">{{ code.code }}</div>
          <div class="code-meta">
            <span>{{ t('pass.monitor.dialog.expiresIn', { value: remainingSeconds }) }}</span>
            <span>{{ code.periodSeconds }}s · {{ code.digits }}d</span>
          </div>
        </div>

        <el-empty v-else :description="t('pass.monitor.dialog.codeHint')" />
      </section>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">
          {{ t('common.actions.cancel') }}
        </el-button>
        <el-button
          v-if="item?.twoFactor.enabled"
          type="danger"
          plain
          :loading="removing"
          @click="emit('remove')"
        >
          {{ t('common.actions.delete') }}
        </el-button>
        <el-button type="primary" :loading="saving" @click="emitSave">
          {{ t('common.actions.save') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-shell,
.dialog-form,
.summary-card,
.code-card {
  display: flex;
  flex-direction: column;
}

.dialog-shell {
  gap: 16px;
}

.summary-card,
.code-card {
  gap: 12px;
  padding: 18px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(246, 248, 255, 0.96), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(128, 146, 204, 0.16);
}

.summary-title-row,
.code-head,
.code-actions,
.dialog-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.summary-title-row h2,
.code-head h3 {
  margin: 0;
}

.summary-text,
.code-head p {
  margin: 0;
  color: #5a6881;
}

.summary-meta,
.code-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #71809d;
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.secret-field {
  grid-column: 1 / -1;
}

.code-body {
  display: grid;
  gap: 10px;
}

.code-value {
  font-size: clamp(28px, 6vw, 44px);
  font-weight: 700;
  letter-spacing: 0.16em;
  color: #182445;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .summary-title-row,
  .code-head,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
