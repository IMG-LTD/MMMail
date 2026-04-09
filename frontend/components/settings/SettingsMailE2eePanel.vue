<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAuthStore } from '~/stores/auth'
import { useI18n } from '~/composables/useI18n'
import { useMailE2ee } from '~/composables/useMailE2ee'
import { useMailE2eeRecovery } from '~/composables/useMailE2eeRecovery'

const authStore = useAuthStore()
const { t } = useI18n()

const {
  loadingProfile,
  savingProfile,
  errorMessage,
  profile,
  draft,
  hasProfile,
  statusLabel,
  privateKeyLabel,
  createdAtLabel,
  initializeMailE2ee,
  generateAndSaveProfile,
  disableProfile
} = useMailE2ee({
  defaultIdentityName: computed(() => authStore.user?.displayName || ''),
  defaultIdentityEmail: computed(() => authStore.user?.email || '')
})

const {
  loadingRecovery,
  savingRecovery,
  recoveryErrorMessage,
  recoveryDraft,
  restoreDraft,
  hasRecovery,
  recoveryStatusLabel,
  recoveryUpdatedAtLabel,
  initializeRecovery,
  saveRecoveryPackage,
  disableRecoveryPackage,
  restoreFromRecoveryPackage
} = useMailE2eeRecovery({
  profile
})

async function handleGenerate(): Promise<void> {
  const saved = await generateAndSaveProfile()
  if (saved) {
    await initializeRecovery()
  }
}

async function handleDisable(): Promise<void> {
  const disabled = await disableProfile()
  if (disabled) {
    await initializeRecovery()
  }
}

async function handleSaveRecovery(): Promise<void> {
  await saveRecoveryPackage()
}

async function handleDisableRecovery(): Promise<void> {
  await disableRecoveryPackage()
}

async function handleRestoreRecovery(): Promise<void> {
  const restored = await restoreFromRecoveryPackage()
  if (restored) {
    await initializeRecovery()
  }
}

onMounted(() => {
  void initializeMailE2ee().then(() => initializeRecovery())
})
</script>

<template>
  <section id="settings-mail-e2ee-panel" class="mm-card mail-e2ee-panel" data-testid="settings-mail-e2ee-panel">
    <div class="mail-e2ee-panel__copy">
      <span class="mail-e2ee-panel__eyebrow">{{ t('settings.mailE2ee.eyebrow') }}</span>
      <h2 class="mm-section-title">{{ t('settings.mailE2ee.title') }}</h2>
      <p>{{ t('settings.mailE2ee.description') }}</p>
    </div>

    <div class="mail-e2ee-panel__grid">
      <div class="mail-e2ee-panel__metric" data-testid="settings-mail-e2ee-status">
        <span>{{ t('settings.mailE2ee.labels.status') }}</span>
        <strong>{{ statusLabel }}</strong>
      </div>
      <div class="mail-e2ee-panel__metric" data-testid="settings-mail-e2ee-fingerprint">
        <span>{{ t('settings.mailE2ee.labels.fingerprint') }}</span>
        <strong>{{ profile.fingerprint || t('settings.mailE2ee.values.notConfigured') }}</strong>
      </div>
      <div class="mail-e2ee-panel__metric" data-testid="settings-mail-e2ee-algorithm">
        <span>{{ t('settings.mailE2ee.labels.algorithm') }}</span>
        <strong>{{ profile.algorithm || t('settings.mailE2ee.values.notConfigured') }}</strong>
      </div>
      <div class="mail-e2ee-panel__metric" data-testid="settings-mail-e2ee-created-at">
        <span>{{ t('settings.mailE2ee.labels.createdAt') }}</span>
        <strong>{{ createdAtLabel }}</strong>
      </div>
      <div class="mail-e2ee-panel__metric" data-testid="settings-mail-e2ee-private-key">
        <span>{{ t('settings.mailE2ee.labels.privateKey') }}</span>
        <strong>{{ privateKeyLabel }}</strong>
      </div>
      <div class="mail-e2ee-panel__metric" data-testid="settings-mail-e2ee-recovery-status">
        <span>{{ t('settings.mailE2ee.recovery.labels.status') }}</span>
        <strong>{{ recoveryStatusLabel }}</strong>
      </div>
      <div class="mail-e2ee-panel__metric" data-testid="settings-mail-e2ee-recovery-updated-at">
        <span>{{ t('settings.mailE2ee.recovery.labels.updatedAt') }}</span>
        <strong>{{ recoveryUpdatedAtLabel }}</strong>
      </div>
    </div>

    <el-form
      label-position="top"
      class="mail-e2ee-panel__form"
      v-loading="loadingProfile || savingProfile"
    >
      <el-form-item :label="t('settings.mailE2ee.fields.identityName')">
        <el-input v-model="draft.identityName" data-testid="settings-mail-e2ee-identity-name" />
      </el-form-item>
      <el-form-item :label="t('settings.mailE2ee.fields.identityEmail')">
        <el-input v-model="draft.identityEmail" data-testid="settings-mail-e2ee-identity-email" />
      </el-form-item>
      <el-form-item :label="t('settings.mailE2ee.fields.passphrase')">
        <el-input
          v-model="draft.passphrase"
          show-password
          type="password"
          data-testid="settings-mail-e2ee-passphrase"
        />
      </el-form-item>
      <el-form-item :label="t('settings.mailE2ee.fields.confirmPassphrase')">
        <el-input
          v-model="draft.confirmPassphrase"
          show-password
          type="password"
          data-testid="settings-mail-e2ee-confirm-passphrase"
        />
      </el-form-item>
      <p class="mail-e2ee-panel__hint">{{ t('settings.mailE2ee.hints.passphrase') }}</p>
      <div class="mail-e2ee-panel__actions">
        <el-button
          type="primary"
          :loading="savingProfile"
          data-testid="settings-mail-e2ee-generate"
          @click="handleGenerate"
        >
          {{ t('settings.mailE2ee.actions.generate') }}
        </el-button>
        <el-button
          :disabled="!hasProfile || savingProfile"
          data-testid="settings-mail-e2ee-disable"
          @click="handleDisable"
        >
          {{ t('settings.mailE2ee.actions.disable') }}
        </el-button>
      </div>
    </el-form>

    <el-form
      label-position="top"
      class="mail-e2ee-panel__form"
      v-loading="loadingRecovery || savingRecovery"
    >
      <h3 class="mail-e2ee-panel__subsection">{{ t('settings.mailE2ee.recovery.title') }}</h3>
      <p class="mail-e2ee-panel__hint">{{ t('settings.mailE2ee.recovery.description') }}</p>
      <el-form-item :label="t('settings.mailE2ee.recovery.fields.currentPassphrase')">
        <el-input
          v-model="recoveryDraft.currentPassphrase"
          type="password"
          show-password
          data-testid="settings-mail-e2ee-recovery-current-passphrase"
        />
      </el-form-item>
      <el-form-item :label="t('settings.mailE2ee.recovery.fields.recoveryPassphrase')">
        <el-input
          v-model="recoveryDraft.recoveryPassphrase"
          type="password"
          show-password
          data-testid="settings-mail-e2ee-recovery-passphrase"
        />
      </el-form-item>
      <el-form-item :label="t('settings.mailE2ee.recovery.fields.confirmRecoveryPassphrase')">
        <el-input
          v-model="recoveryDraft.confirmRecoveryPassphrase"
          type="password"
          show-password
          data-testid="settings-mail-e2ee-recovery-confirm-passphrase"
        />
      </el-form-item>
      <div class="mail-e2ee-panel__actions">
        <el-button
          type="primary"
          :disabled="!hasProfile"
          :loading="savingRecovery"
          data-testid="settings-mail-e2ee-recovery-save"
          @click="handleSaveRecovery"
        >
          {{ t('settings.mailE2ee.recovery.actions.save') }}
        </el-button>
        <el-button
          :disabled="!hasRecovery || savingRecovery"
          data-testid="settings-mail-e2ee-recovery-disable"
          @click="handleDisableRecovery"
        >
          {{ t('settings.mailE2ee.recovery.actions.disable') }}
        </el-button>
      </div>
      <p class="mail-e2ee-panel__hint">{{ t('settings.mailE2ee.recovery.hint') }}</p>
    </el-form>

    <el-form
      label-position="top"
      class="mail-e2ee-panel__form"
      v-loading="savingRecovery"
    >
      <h3 class="mail-e2ee-panel__subsection">{{ t('settings.mailE2ee.recovery.restoreTitle') }}</h3>
      <p class="mail-e2ee-panel__hint">{{ t('settings.mailE2ee.recovery.restoreDescription') }}</p>
      <el-form-item :label="t('settings.mailE2ee.recovery.fields.restorePassphrase')">
        <el-input
          v-model="restoreDraft.recoveryPassphrase"
          type="password"
          show-password
          data-testid="settings-mail-e2ee-restore-passphrase"
        />
      </el-form-item>
      <el-form-item :label="t('settings.mailE2ee.recovery.fields.nextPassphrase')">
        <el-input
          v-model="restoreDraft.nextPassphrase"
          type="password"
          show-password
          data-testid="settings-mail-e2ee-restore-next-passphrase"
        />
      </el-form-item>
      <el-form-item :label="t('settings.mailE2ee.recovery.fields.confirmNextPassphrase')">
        <el-input
          v-model="restoreDraft.confirmNextPassphrase"
          type="password"
          show-password
          data-testid="settings-mail-e2ee-restore-confirm-next-passphrase"
        />
      </el-form-item>
      <div class="mail-e2ee-panel__actions">
        <el-button
          type="primary"
          :disabled="!hasRecovery"
          :loading="savingRecovery"
          data-testid="settings-mail-e2ee-recovery-restore"
          @click="handleRestoreRecovery"
        >
          {{ t('settings.mailE2ee.recovery.actions.restore') }}
        </el-button>
      </div>
    </el-form>

    <el-alert
      :closable="false"
      type="info"
      data-testid="settings-mail-e2ee-boundary"
      :title="t('settings.mailE2ee.boundaryTitle')"
      :description="t('settings.mailE2ee.boundaryDescription')"
    />

    <p
      v-if="errorMessage || recoveryErrorMessage"
      class="mail-e2ee-panel__error"
      data-testid="settings-mail-e2ee-error"
    >
      {{ errorMessage || recoveryErrorMessage }}
    </p>
  </section>
</template>

<style scoped>
.mail-e2ee-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.mail-e2ee-panel__copy {
  display: grid;
  gap: 8px;
}

.mail-e2ee-panel__copy p {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.mail-e2ee-panel__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mm-accent, #0c5a5a);
}

.mail-e2ee-panel__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.mail-e2ee-panel__metric {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(12, 90, 90, 0.05);
  border: 1px solid rgba(12, 90, 90, 0.12);
}

.mail-e2ee-panel__metric span {
  font-size: 12px;
  color: var(--mm-muted);
}

.mail-e2ee-panel__metric strong {
  font-size: 14px;
  overflow-wrap: anywhere;
}

.mail-e2ee-panel__form {
  display: grid;
}

.mail-e2ee-panel__hint {
  margin: 0 0 12px;
  color: var(--mm-muted);
  font-size: 13px;
  line-height: 1.6;
}

.mail-e2ee-panel__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.mail-e2ee-panel__subsection {
  margin: 0 0 4px;
  font-size: 16px;
}

.mail-e2ee-panel__error {
  margin: 0;
  color: #b42318;
  font-size: 13px;
}
</style>
