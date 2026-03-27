<script setup lang="ts">
import VpnHistoryPanel from '~/components/vpn/VpnHistoryPanel.vue'
import VpnPrivacyPanel from '~/components/vpn/VpnPrivacyPanel.vue'
import VpnProfilesPanel from '~/components/vpn/VpnProfilesPanel.vue'
import VpnServerDirectoryPanel from '~/components/vpn/VpnServerDirectoryPanel.vue'
import VpnSessionPanel from '~/components/vpn/VpnSessionPanel.vue'
import VpnWorkspaceHero from '~/components/vpn/VpnWorkspaceHero.vue'
import { useI18n } from '~/composables/useI18n'
import { useVpnWorkspace } from '~/composables/useVpnWorkspace'
import type { VpnProfileDraft, VpnProtocol, VpnSettingsDraft } from '~/types/vpn'

definePageMeta({
  layout: 'default'
})

const { t } = useI18n()

useHead(() => ({
  title: t('vpn.page.title')
}))

const {
  currentSession,
  defaultProfileName,
  editingProfileId,
  heroMetrics,
  history,
  liveDurationSeconds,
  loading,
  profileDialogVisible,
  profileDraft,
  profiles,
  secureCoreProfileCount,
  selectedProtocol,
  servers,
  settingsDraft,
  closeProfileDialog,
  connectServer,
  deleteProfile,
  disconnectSession,
  openCreateProfile,
  openEditProfile,
  quickConnect,
  refreshWorkspace,
  saveSettings,
  setProfileDraft,
  setSettingsDraft,
  submitProfile
} = useVpnWorkspace()

function updateSettingsDraft(draft: VpnSettingsDraft): void {
  setSettingsDraft(draft)
}

function updateProfileDraft(draft: VpnProfileDraft): void {
  setProfileDraft(draft)
}

function updateSelectedProtocol(value: VpnProtocol): void {
  selectedProtocol.value = value
}
</script>

<template>
  <div class="vpn-page" v-loading="loading.page">
    <VpnWorkspaceHero
      :metrics="heroMetrics"
      :settings="settingsDraft"
      :default-profile-name="defaultProfileName"
      :secure-core-profile-count="secureCoreProfileCount"
      :current-session="currentSession"
      :refreshing="loading.refresh"
      :quick-connecting="loading.quickConnect"
      @refresh="refreshWorkspace"
      @quick-connect="quickConnect()"
    />

    <section class="vpn-grid vpn-grid--top">
      <VpnPrivacyPanel
        :draft="settingsDraft"
        :profiles="profiles"
        :saving="loading.settings"
        @update:draft="updateSettingsDraft"
        @save="saveSettings"
      />
      <VpnSessionPanel
        :current-session="currentSession"
        :live-duration-seconds="liveDurationSeconds"
        :disconnecting="loading.disconnect"
        @disconnect="disconnectSession"
      />
    </section>

    <section class="vpn-grid vpn-grid--middle">
      <VpnProfilesPanel
        :profiles="profiles"
        :draft="profileDraft"
        :dialog-visible="profileDialogVisible"
        :editing-profile-id="editingProfileId"
        :saving="loading.profile"
        :quick-connect-profile-id="loading.quickConnectProfileId"
        :delete-profile-id="loading.deleteProfileId"
        @update:draft="updateProfileDraft"
        @create="openCreateProfile"
        @edit="openEditProfile"
        @delete="deleteProfile"
        @quick-connect="quickConnect"
        @close="closeProfileDialog"
        @submit="submitProfile"
      />
      <VpnServerDirectoryPanel
        :servers="servers"
        :selected-protocol="selectedProtocol"
        :connecting-server-id="loading.connectServerId"
        @update:selected-protocol="updateSelectedProtocol"
        @connect="connectServer"
      />
    </section>

    <VpnHistoryPanel :history="history" />
  </div>
</template>

<style scoped>
.vpn-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.vpn-grid {
  display: grid;
  gap: 18px;
}

.vpn-grid--top {
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
}

.vpn-grid--middle {
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
}

@media (max-width: 1100px) {
  .vpn-grid--top,
  .vpn-grid--middle {
    grid-template-columns: 1fr;
  }
}
</style>
