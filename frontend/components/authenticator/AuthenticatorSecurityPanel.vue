<script setup lang="ts">
import { computed } from 'vue'
import type {
  AuthenticatorSecurityDraft,
  AuthenticatorSecurityPreference
} from '~/types/authenticator-security'
import { AUTHENTICATOR_LOCK_TIMEOUT_OPTIONS } from '~/utils/authenticator-security'
import { useI18n } from '~/composables/useI18n'

interface Props {
  preference: AuthenticatorSecurityPreference | null
  draft: AuthenticatorSecurityDraft
  unlockPin: string
  loading: boolean
  saving: boolean
  verifying: boolean
  requiresUnlock: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  save: []
  lock: []
  unlock: []
  'update:unlockPin': [value: string]
}>()

const { t } = useI18n()

const fieldsDisabled = computed(() => props.loading || props.requiresUnlock)
const lockTimeoutOptions = computed(() => AUTHENTICATOR_LOCK_TIMEOUT_OPTIONS.map((seconds) => ({
  value: seconds,
  label: t(`authenticator.security.timeout.${seconds}`)
})))
const pinHintKey = computed(() => {
  if (props.preference?.pinConfigured) {
    return 'authenticator.security.fields.pinHintExisting'
  }
  return 'authenticator.security.fields.pinHintNew'
})

const statusTags = computed(() => [
  props.draft.syncEnabled
    ? t('authenticator.security.status.syncOn')
    : t('authenticator.security.status.syncOff'),
  props.draft.encryptedBackupEnabled
    ? t('authenticator.security.status.backupOn')
    : t('authenticator.security.status.backupOff'),
  props.requiresUnlock
    ? t('authenticator.security.status.locked')
    : t('authenticator.security.status.unlocked')
])

function formatDateTime(value: string | null | undefined): string {
  return value || '-'
}

function onUnlockPinChange(value: string | number): void {
  emit('update:unlockPin', String(value || ''))
}
</script>

<template>
  <article class="mm-card security-shell">
    <div class="security-head">
      <div>
        <h2 class="mm-section-title">{{ t('authenticator.security.title') }}</h2>
        <p class="mm-muted">{{ t('authenticator.security.description') }}</p>
      </div>
      <div class="security-badges">
        <el-tag
          v-for="tag in statusTags"
          :key="tag"
          effect="plain"
          round
          type="info"
        >
          {{ tag }}
        </el-tag>
      </div>
    </div>

    <div class="security-metrics">
      <article class="metric-card">
        <span class="metric-label">{{ t('authenticator.security.metrics.lastSyncedAt') }}</span>
        <strong class="metric-value">{{ formatDateTime(preference?.lastSyncedAt) }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">{{ t('authenticator.security.metrics.lastBackupAt') }}</span>
        <strong class="metric-value">{{ formatDateTime(preference?.lastBackupAt) }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">{{ t('authenticator.security.metrics.lockTimeout') }}</span>
        <strong class="metric-value">{{ t(`authenticator.security.timeout.${draft.lockTimeoutSeconds}`) }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">{{ t('authenticator.security.metrics.pinStatus') }}</span>
        <strong class="metric-value">
          {{ preference?.pinConfigured ? t('authenticator.security.metrics.pinConfigured') : t('authenticator.security.metrics.pinMissing') }}
        </strong>
      </article>
    </div>

    <el-alert
      v-if="requiresUnlock"
      :closable="false"
      type="warning"
      show-icon
      :title="t('authenticator.security.locked.title')"
      :description="t('authenticator.security.locked.description')"
    />
    <el-alert
      v-else-if="preference?.pinProtectionEnabled"
      :closable="false"
      type="success"
      show-icon
      :title="t('authenticator.security.unlocked.title')"
      :description="t('authenticator.security.unlocked.description')"
    />

    <div class="security-grid" :class="{ 'security-grid--single': !draft.pinProtectionEnabled }">
      <article class="security-form-card">
        <div class="security-form-title">{{ t('authenticator.security.formTitle') }}</div>
        <el-form label-position="top" class="security-form">
          <div class="security-switches">
            <el-form-item :label="t('authenticator.security.fields.syncEnabled')">
              <el-switch v-model="draft.syncEnabled" :disabled="fieldsDisabled" />
            </el-form-item>
            <el-form-item :label="t('authenticator.security.fields.encryptedBackupEnabled')">
              <el-switch v-model="draft.encryptedBackupEnabled" :disabled="fieldsDisabled" />
            </el-form-item>
            <el-form-item :label="t('authenticator.security.fields.pinProtectionEnabled')">
              <el-switch v-model="draft.pinProtectionEnabled" :disabled="fieldsDisabled" />
            </el-form-item>
          </div>

          <el-form-item :label="t('authenticator.security.fields.lockTimeoutSeconds')">
            <el-select v-model="draft.lockTimeoutSeconds" :disabled="fieldsDisabled">
              <el-option
                v-for="item in lockTimeoutOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>

          <template v-if="draft.pinProtectionEnabled">
            <div class="pin-grid">
              <el-form-item :label="t('authenticator.security.fields.pin')">
                <el-input
                  v-model="draft.pin"
                  maxlength="12"
                  show-password
                  :disabled="fieldsDisabled"
                  :placeholder="t('authenticator.security.fields.pinPlaceholder')"
                />
              </el-form-item>
              <el-form-item :label="t('authenticator.security.fields.pinConfirm')">
                <el-input
                  v-model="draft.pinConfirm"
                  maxlength="12"
                  show-password
                  :disabled="fieldsDisabled"
                  :placeholder="t('authenticator.security.fields.pinConfirmPlaceholder')"
                />
              </el-form-item>
            </div>
            <p class="mm-muted">{{ t(pinHintKey) }}</p>
          </template>
        </el-form>
      </article>

      <article v-if="draft.pinProtectionEnabled" class="security-unlock-card">
        <div class="security-form-title">{{ t('authenticator.security.unlockTitle') }}</div>
        <p class="mm-muted">{{ t('authenticator.security.unlockDescription') }}</p>
        <el-input
          v-if="requiresUnlock"
          :model-value="unlockPin"
          maxlength="12"
          show-password
          :placeholder="t('authenticator.security.fields.unlockPin')"
          @update:model-value="onUnlockPinChange"
          @keyup.enter="emit('unlock')"
        />
        <div v-else class="unlock-state">{{ t('authenticator.security.unlocked.description') }}</div>
        <div class="security-actions">
          <el-button
            v-if="requiresUnlock"
            type="primary"
            :loading="verifying"
            @click="emit('unlock')"
          >
            {{ t('authenticator.security.actions.unlock') }}
          </el-button>
          <el-button plain :disabled="requiresUnlock" @click="emit('lock')">
            {{ t('authenticator.security.actions.lockNow') }}
          </el-button>
        </div>
      </article>
    </div>

    <div class="security-footer">
      <el-button
        type="primary"
        :loading="saving"
        :disabled="fieldsDisabled"
        @click="emit('save')"
      >
        {{ t('authenticator.security.actions.save') }}
      </el-button>
    </div>
  </article>
</template>

<style scoped>
.security-shell,
.security-form,
.security-form-card,
.security-unlock-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-shell {
  padding: 20px;
}

.security-head,
.security-badges,
.security-actions {
  display: flex;
  gap: 12px;
}

.security-head {
  justify-content: space-between;
}

.security-badges {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.security-grid,
.security-metrics,
.security-switches,
.pin-grid {
  display: grid;
  gap: 16px;
}

.security-grid {
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
}

.security-grid--single {
  grid-template-columns: 1fr;
}

.security-metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.security-switches,
.pin-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.pin-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-card,
.security-form-card,
.security-unlock-card {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(15, 110, 110, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(242, 247, 248, 0.92));
}

.metric-label,
.security-form-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.metric-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.metric-value {
  color: #13222a;
  font-size: 18px;
  line-height: 1.3;
}

.unlock-state {
  color: #4f6470;
  line-height: 1.6;
}

.security-footer {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1120px) {
  .security-grid,
  .security-metrics,
  .security-switches {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .security-head,
  .security-badges,
  .security-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .security-grid,
  .security-metrics,
  .security-switches,
  .pin-grid {
    grid-template-columns: 1fr;
  }
}
</style>
