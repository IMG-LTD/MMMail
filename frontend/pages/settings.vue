<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { LOCALE_OPTIONS } from '~/constants/i18n'
import type { MailAddressMode, PreferredLocale } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useSettingsApi } from '~/composables/useSettingsApi'
import { useAuthStore } from '~/stores/auth'
import { useSettingsStore } from '~/stores/settings'

const loading = ref(false)
const authStore = useAuthStore()
const settingsStore = useSettingsStore()
const { fetchProfile, updateProfile } = useSettingsApi()
const { applyPreferredLocale, applyProfileLocale, rememberLocaleSelection, t } = useI18n()
const mailAddressModeOptions = computed(() => ([
  { label: t('externalAccount.settings.mode.protonAddress'), value: 'PROTON_ADDRESS' },
  { label: t('externalAccount.settings.mode.externalAccount'), value: 'EXTERNAL_ACCOUNT' }
]))
const isExternalAccountMode = computed(() => form.mailAddressMode === 'EXTERNAL_ACCOUNT')
const lockedProducts = computed(() => [
  t('organizations.products.MAIL'),
  t('organizations.products.CALENDAR')
])
const availableProducts = computed(() => [
  t('organizations.products.DRIVE'),
  t('organizations.products.DOCS'),
  t('organizations.products.SHEETS'),
  t('organizations.products.PASS'),
  t('organizations.products.SIMPLELOGIN'),
  t('organizations.products.VPN'),
  t('organizations.products.AUTHENTICATOR'),
  t('organizations.products.WALLET'),
  t('organizations.products.LUMO')
])

const form = reactive<{
  displayName: string
  signature: string
  timezone: string
  preferredLocale: PreferredLocale
  mailAddressMode: MailAddressMode
  autoSaveSeconds: number
  undoSendSeconds: number
  driveVersionRetentionCount: number
  driveVersionRetentionDays: number
}>({
  displayName: '',
  signature: '',
  timezone: 'UTC',
  preferredLocale: 'en',
  mailAddressMode: 'PROTON_ADDRESS',
  autoSaveSeconds: 15,
  undoSendSeconds: 10,
  driveVersionRetentionCount: 50,
  driveVersionRetentionDays: 365
})

async function loadSettings(): Promise<void> {
  loading.value = true
  try {
    const profile = await fetchProfile()
    settingsStore.setProfile(profile)
    const effectiveLocale = applyProfileLocale(profile.preferredLocale)
    form.displayName = profile.displayName
    form.signature = profile.signature
    form.timezone = profile.timezone
    form.preferredLocale = effectiveLocale
    form.mailAddressMode = profile.mailAddressMode
    form.autoSaveSeconds = profile.autoSaveSeconds
    form.undoSendSeconds = profile.undoSendSeconds
    form.driveVersionRetentionCount = profile.driveVersionRetentionCount
    form.driveVersionRetentionDays = profile.driveVersionRetentionDays
    authStore.updateUserProfile({
      displayName: profile.displayName,
      mailAddressMode: profile.mailAddressMode
    })
  } finally {
    loading.value = false
  }
}

async function saveSettings(): Promise<void> {
  loading.value = true
  try {
    const profile = await updateProfile({
      displayName: form.displayName,
      signature: form.signature,
      timezone: form.timezone,
      preferredLocale: form.preferredLocale,
      mailAddressMode: form.mailAddressMode,
      autoSaveSeconds: form.autoSaveSeconds,
      undoSendSeconds: form.undoSendSeconds,
      driveVersionRetentionCount: form.driveVersionRetentionCount,
      driveVersionRetentionDays: form.driveVersionRetentionDays
    })
    settingsStore.setProfile(profile)
    rememberLocaleSelection(profile.preferredLocale)
    applyPreferredLocale(profile.preferredLocale)
    authStore.updateUserProfile({
      displayName: profile.displayName,
      mailAddressMode: profile.mailAddressMode
    })
    ElMessage.success(t('settings.messages.saved'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<template>
  <div class="mm-page">
    <section class="mm-card panel">
      <h1 class="mm-section-title">{{ t('settings.title') }}</h1>
      <el-form label-position="top" v-loading="loading">
        <el-form-item :label="t('settings.fields.displayName')">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item :label="t('settings.fields.signature')">
          <el-input v-model="form.signature" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('settings.fields.timezone')">
          <el-select v-model="form.timezone" style="width: 100%">
            <el-option label="UTC" value="UTC" />
            <el-option label="Asia/Shanghai" value="Asia/Shanghai" />
            <el-option label="America/New_York" value="America/New_York" />
            <el-option label="Europe/Berlin" value="Europe/Berlin" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.fields.language')">
          <el-select v-model="form.preferredLocale" style="width: 100%" :aria-label="t('topbar.localeAriaLabel')">
            <el-option
              v-for="option in LOCALE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">{{ t('externalAccount.settings.section') }}</el-divider>
        <el-form-item :label="t('externalAccount.settings.field')">
          <div class="mail-address-mode">
            <div class="mail-address-mode__copy">
              <h2>{{ t('externalAccount.settings.title') }}</h2>
              <p>{{ t('externalAccount.settings.description') }}</p>
            </div>
            <el-segmented v-model="form.mailAddressMode" :options="mailAddressModeOptions" />
            <div class="mail-address-mode__panel" :class="{ 'mail-address-mode__panel--warning': isExternalAccountMode }">
              <strong>{{ isExternalAccountMode ? t('externalAccount.settings.externalTitle') : t('externalAccount.settings.protonTitle') }}</strong>
              <p>{{ isExternalAccountMode ? t('externalAccount.settings.externalDescription') : t('externalAccount.settings.protonDescription') }}</p>
              <div class="mail-address-mode__chips">
                <span class="mail-address-mode__label">{{ t('externalAccount.settings.lockedProducts') }}</span>
                <el-tag v-for="product in lockedProducts" :key="product" effect="plain" type="danger">{{ product }}</el-tag>
              </div>
              <div class="mail-address-mode__chips" v-if="isExternalAccountMode">
                <span class="mail-address-mode__label">{{ t('externalAccount.settings.availableProducts') }}</span>
                <el-tag v-for="product in availableProducts" :key="product" effect="plain" type="success">{{ product }}</el-tag>
              </div>
              <el-alert
                v-if="isExternalAccountMode"
                :closable="false"
                type="warning"
                :title="t('externalAccount.settings.warningTitle')"
                :description="t('externalAccount.settings.warningDescription')"
              />
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="t('settings.fields.autoSaveSeconds')">
          <el-input-number v-model="form.autoSaveSeconds" :min="5" :max="300" />
        </el-form-item>
        <el-form-item :label="t('settings.fields.undoSendSeconds')">
          <el-input-number v-model="form.undoSendSeconds" :min="0" :max="60" />
        </el-form-item>
        <el-divider content-position="left">{{ t('settings.sections.driveVersionGovernance') }}</el-divider>
        <el-form-item :label="t('settings.fields.driveRetentionCount')">
          <el-input-number v-model="form.driveVersionRetentionCount" :min="1" :max="200" />
        </el-form-item>
        <el-form-item :label="t('settings.fields.driveRetentionDays')">
          <el-input-number v-model="form.driveVersionRetentionDays" :min="1" :max="3650" />
        </el-form-item>
        <template v-if="authStore.user?.role === 'ADMIN'">
          <el-divider content-position="left">{{ t('settings.sections.systemHealth') }}</el-divider>
          <div class="operations-link">
            <div>
              <strong>{{ t('settings.sections.systemHealth') }}</strong>
              <p>{{ t('settings.systemHealth.description') }}</p>
            </div>
            <NuxtLink
              class="operations-link__action"
              data-testid="settings-system-health-link"
              to="/settings/system-health"
            >
              {{ t('settings.actions.openSystemHealth') }}
            </NuxtLink>
          </div>
        </template>
        <el-button type="primary" :loading="loading" @click="saveSettings">{{ t('settings.actions.save') }}</el-button>
      </el-form>
    </section>
    <SettingsMailE2eePanel />
    <SettingsAdoptionReadinessPanel />
    <SettingsPwaPanel />
    <SettingsMailFiltersPanel />
    <SettingsMailEasySwitchPanel />
  </div>
</template>

<style scoped>
.panel {
  padding: 20px;
  max-width: 760px;
}

.mail-address-mode {
  display: grid;
  gap: 14px;
  width: 100%;
}

.mail-address-mode__copy h2 {
  margin: 0;
  font-size: 18px;
}

.mail-address-mode__copy p {
  margin: 8px 0 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.mail-address-mode__panel {
  display: grid;
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(12, 90, 90, 0.05);
  border: 1px solid rgba(12, 90, 90, 0.12);
}

.mail-address-mode__panel--warning {
  background: rgba(245, 158, 11, 0.08);
  border-color: rgba(245, 158, 11, 0.28);
}

.mail-address-mode__panel p {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.mail-address-mode__chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.mail-address-mode__label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--mm-primary-dark);
}

.operations-link {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border-radius: 18px;
  background: rgba(12, 90, 90, 0.05);
  border: 1px solid rgba(12, 90, 90, 0.12);
  margin-bottom: 16px;
}

.operations-link p {
  margin: 8px 0 0;
  color: var(--mm-muted);
}

.operations-link__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 168px;
  padding: 10px 16px;
  border-radius: 999px;
  background: #0f6e6e;
  color: #fff;
  font-weight: 600;
}
</style>
