<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, watch } from 'vue'
import AuthenticatorPortabilityPanel from '~/components/authenticator/AuthenticatorPortabilityPanel.vue'
import AuthenticatorSecurityPanel from '~/components/authenticator/AuthenticatorSecurityPanel.vue'
import AuthenticatorWorkspacePanel from '~/components/authenticator/AuthenticatorWorkspacePanel.vue'
import { useAuthenticatorRecovery } from '~/composables/useAuthenticatorRecovery'
import { useAuthenticatorSecurity } from '~/composables/useAuthenticatorSecurity'
import { useAuthenticatorWorkspace } from '~/composables/useAuthenticatorWorkspace'
import { useI18n } from '~/composables/useI18n'

const { t } = useI18n()
const {
  context: recoveryContext,
  isRecoveryMode,
  productLabel,
  prepareRecoveryScope,
  restoreAccess
} = useAuthenticatorRecovery()
const {
  loadingPreference,
  savingPreference,
  verifyingPin,
  preference,
  draft,
  unlockPin,
  requiresUnlock,
  initializeSecurity,
  refreshSecurity,
  saveSecurity,
  verifyPinAndUnlock,
  lockNow,
  dispose: disposeSecurity
} = useAuthenticatorSecurity()
const {
  keyword,
  loading,
  creating,
  saving,
  deleting,
  loadingCode,
  activeEntryId,
  entries,
  editor,
  codePanel,
  hasActiveEntry,
  countdownPercent,
  initialize,
  dispose,
  clearWorkspace,
  loadEntries,
  selectEntry,
  createDefaultEntry,
  saveCurrentEntry,
  deleteCurrentEntry,
  refreshCode,
  copyCode,
  formatTime
} = useAuthenticatorWorkspace({
  onEntryCreated: async () => {
    await refreshSecurity()
    if (isRecoveryMode.value) {
      await restoreAccess()
    }
  },
  onEntrySaved: async () => {
    await refreshSecurity()
    if (isRecoveryMode.value) {
      await restoreAccess()
    }
  },
  onEntryDeleted: refreshSecurity
})

const toolbarDisabled = computed(() => loadingPreference.value || savingPreference.value || requiresUnlock.value)

const pageTitle = computed(() => {
  if (!isRecoveryMode.value) {
    return t('authenticator.page.title')
  }
  return t('authenticator.recovery.title', {
    product: productLabel.value || t('authenticator.page.title')
  })
})

const recoveryDescription = computed(() => t('authenticator.recovery.description', {
  organization: recoveryContext.value.orgName || t('orgAccess.scope.personal')
}))

useHead(() => ({
  title: pageTitle.value
}))

onMounted(async () => {
  await prepareRecoveryScope()
  await initializeSecurity()
  if (!requiresUnlock.value) {
    await initialize()
  }
})

onBeforeUnmount(() => {
  dispose()
  disposeSecurity()
})

async function onEntriesImported(preferredEntryId: string): Promise<void> {
  await loadEntries(false)
  if (preferredEntryId) {
    await selectEntry(preferredEntryId)
  }
  await refreshSecurity()
}

async function onPortabilityChanged(): Promise<void> {
  await refreshSecurity()
}

async function onSaveSecurity(): Promise<void> {
  await saveSecurity()
}

async function onUnlockWorkspace(): Promise<void> {
  await verifyPinAndUnlock()
}

function onLockWorkspace(): void {
  lockNow()
  clearWorkspace()
}

function onUnlockPinUpdated(value: string): void {
  unlockPin.value = value
}

watch(requiresUnlock, async (locked, previous) => {
  if (locked) {
    clearWorkspace()
    return
  }
  if (previous) {
    await initialize()
  }
})
</script>

<template>
  <section class="mm-card auth-shell">
    <header v-if="isRecoveryMode" class="recovery-banner">
      <div class="recovery-copy">
        <el-tag type="warning" effect="dark">{{ t('authenticator.recovery.badge') }}</el-tag>
        <div>
          <h1 class="recovery-title">
            {{ t('authenticator.recovery.title', { product: productLabel || t('authenticator.page.title') }) }}
          </h1>
          <p class="recovery-description">{{ recoveryDescription }}</p>
        </div>
      </div>
      <el-button plain :disabled="!hasActiveEntry" @click="restoreAccess">
        {{ t('authenticator.recovery.restore') }}
      </el-button>
    </header>

    <header class="auth-toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('authenticator.toolbar.searchPlaceholder')"
        :disabled="toolbarDisabled"
        @keyup.enter="loadEntries(false)"
      />
      <el-button :loading="loading" :disabled="toolbarDisabled" @click="loadEntries(false)">
        {{ t('authenticator.toolbar.search') }}
      </el-button>
      <el-button type="primary" :loading="creating" :disabled="toolbarDisabled" @click="createDefaultEntry">
        {{ t('authenticator.toolbar.newEntry') }}
      </el-button>
      <el-button :loading="loading" :disabled="toolbarDisabled" @click="loadEntries(true)">
        {{ t('authenticator.toolbar.refresh') }}
      </el-button>
    </header>

    <AuthenticatorSecurityPanel
      :preference="preference"
      :draft="draft"
      :unlock-pin="unlockPin"
      :loading="loadingPreference"
      :saving="savingPreference"
      :verifying="verifyingPin"
      :requires-unlock="requiresUnlock"
      @save="onSaveSecurity"
      @lock="onLockWorkspace"
      @unlock="onUnlockWorkspace"
      @update:unlock-pin="onUnlockPinUpdated"
    />

    <template v-if="!requiresUnlock">
      <AuthenticatorPortabilityPanel
        @changed="onPortabilityChanged"
        @imported="onEntriesImported"
      />

      <AuthenticatorWorkspacePanel
        :entries="entries"
        :active-entry-id="activeEntryId"
        :editor="editor"
        :code-panel="codePanel"
        :has-active-entry="hasActiveEntry"
        :countdown-percent="countdownPercent"
        :loading-code="loadingCode"
        :saving="saving"
        :deleting="deleting"
        :format-time="formatTime"
        @select-entry="selectEntry"
        @refresh-code="refreshCode(true)"
        @copy-code="copyCode"
        @save="saveCurrentEntry"
        @delete="deleteCurrentEntry"
      />
    </template>
  </section>
</template>

<style scoped>
.auth-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.recovery-banner,
.recovery-copy {
  display: flex;
  gap: 12px;
}

.recovery-banner,
.auth-toolbar {
  width: 100%;
}

.recovery-banner {
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(15, 110, 110, 0.92), rgba(12, 90, 90, 0.96)),
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.24), transparent 48%);
  color: #f7fbfd;
}

.recovery-copy {
  align-items: flex-start;
}

.recovery-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.1;
}

.recovery-description {
  margin: 8px 0 0;
  color: rgba(247, 251, 253, 0.82);
  line-height: 1.6;
}

.auth-toolbar {
  display: grid;
  grid-template-columns: 1fr auto auto auto;
  gap: 12px;
}

@media (max-width: 840px) {
  .auth-toolbar {
    grid-template-columns: 1fr 1fr;
  }

  .recovery-banner,
  .recovery-copy {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .auth-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
