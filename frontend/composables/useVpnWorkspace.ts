import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useVpnApi } from '~/composables/useVpnApi'
import { useI18n } from '~/composables/useI18n'
import type {
  VpnProfileDraft,
  VpnProfileItem,
  VpnProtocol,
  VpnServerItem,
  VpnSessionItem,
  VpnSettingsDraft,
  VpnSettingsItem
} from '~/types/vpn'
import {
  buildVpnHeroMetrics,
  buildVpnProfilePayload,
  buildVpnSettingsPayload,
  countSecureCoreProfiles,
  createVpnProfileDraft,
  createVpnSettingsDraft,
  resolveVpnDefaultProfileName
} from '~/utils/vpn'

interface WorkspaceSnapshot {
  servers: VpnServerItem[]
  currentSession: VpnSessionItem | null
  history: VpnSessionItem[]
  settings: VpnSettingsItem
  profiles: VpnProfileItem[]
}

export function useVpnWorkspace() {
  const vpnApi = useVpnApi()
  const { t } = useI18n()

  const servers = ref<VpnServerItem[]>([])
  const currentSession = ref<VpnSessionItem | null>(null)
  const history = ref<VpnSessionItem[]>([])
  const profiles = ref<VpnProfileItem[]>([])
  const settingsDraft = ref<VpnSettingsDraft>(createVpnSettingsDraft())
  const profileDraft = ref<VpnProfileDraft>(createVpnProfileDraft())
  const selectedProtocol = ref<VpnProtocol>('WIREGUARD')
  const profileDialogVisible = ref(false)
  const editingProfileId = ref('')
  const activeTimestamp = ref(Date.now())
  const ticker = ref<ReturnType<typeof setInterval> | null>(null)

  const loading = reactive({
    page: false,
    refresh: false,
    settings: false,
    profile: false,
    quickConnect: false,
    disconnect: false,
    connectServerId: '',
    quickConnectProfileId: '',
    deleteProfileId: ''
  })

  const isConnected = computed(() => currentSession.value?.status === 'CONNECTED')
  const liveDurationSeconds = computed(() => {
    if (!currentSession.value) {
      return 0
    }
    if (currentSession.value.status !== 'CONNECTED') {
      return currentSession.value.durationSeconds
    }
    const connectedAt = new Date(currentSession.value.connectedAt).getTime()
    return Math.max(0, Math.floor((activeTimestamp.value - connectedAt) / 1000))
  })
  const secureCoreProfileCount = computed(() => countSecureCoreProfiles(profiles.value))
  const defaultProfileName = computed(() => resolveVpnDefaultProfileName(settingsDraft.value, profiles.value))
  const heroMetrics = computed(() => buildVpnHeroMetrics({
    servers: servers.value,
    profiles: profiles.value,
    settings: settingsDraft.value,
    currentSession: currentSession.value
  }))

  onMounted(() => {
    void loadWorkspace()
  })

  onBeforeUnmount(() => {
    stopTicker()
  })

  function setSettingsDraft(draft: VpnSettingsDraft): void {
    settingsDraft.value = draft
  }

  function setProfileDraft(draft: VpnProfileDraft): void {
    profileDraft.value = draft
  }

  function resolveMessage(error: unknown, fallbackKey: string): string {
    const message = error instanceof Error ? error.message : ''
    if (message.startsWith('vpn.')) {
      return t(message)
    }
    return message || t(fallbackKey)
  }

  function stopTicker(): void {
    if (!ticker.value) {
      return
    }
    clearInterval(ticker.value)
    ticker.value = null
  }

  function syncTicker(): void {
    stopTicker()
    activeTimestamp.value = Date.now()
    if (!isConnected.value) {
      return
    }
    ticker.value = setInterval(() => {
      activeTimestamp.value = Date.now()
    }, 1000)
  }

  function applySnapshot(snapshot: WorkspaceSnapshot): void {
    servers.value = snapshot.servers
    currentSession.value = snapshot.currentSession
    history.value = snapshot.history
    profiles.value = snapshot.profiles
    settingsDraft.value = createVpnSettingsDraft(snapshot.settings)
    if (profileDialogVisible.value && editingProfileId.value) {
      const currentProfile = snapshot.profiles.find((profile) => profile.profileId === editingProfileId.value)
      if (currentProfile) {
        profileDraft.value = createVpnProfileDraft(currentProfile)
      }
    }
    syncTicker()
  }

  async function fetchSnapshot(): Promise<WorkspaceSnapshot> {
    const [serverList, session, historyList, settings, profileList] = await Promise.all([
      vpnApi.listServers(),
      vpnApi.getCurrentSession(),
      vpnApi.listHistory(20),
      vpnApi.getSettings(),
      vpnApi.listProfiles()
    ])
    return {
      servers: serverList,
      currentSession: session,
      history: historyList,
      settings,
      profiles: profileList
    }
  }

  async function refreshSessionState(): Promise<void> {
    const [session, historyList] = await Promise.all([
      vpnApi.getCurrentSession(),
      vpnApi.listHistory(20)
    ])
    currentSession.value = session
    history.value = historyList
    syncTicker()
  }

  async function refreshPolicies(): Promise<void> {
    const [settings, profileList] = await Promise.all([
      vpnApi.getSettings(),
      vpnApi.listProfiles()
    ])
    settingsDraft.value = createVpnSettingsDraft(settings)
    profiles.value = profileList
  }

  async function loadWorkspace(): Promise<void> {
    loading.page = true
    try {
      applySnapshot(await fetchSnapshot())
    } catch (error) {
      ElMessage.error(resolveMessage(error, 'vpn.messages.workspaceLoadFailed'))
    } finally {
      loading.page = false
    }
  }

  async function refreshWorkspace(): Promise<void> {
    loading.refresh = true
    try {
      applySnapshot(await fetchSnapshot())
      ElMessage.success(t('vpn.messages.workspaceRefreshed'))
    } catch (error) {
      ElMessage.error(resolveMessage(error, 'vpn.messages.workspaceLoadFailed'))
    } finally {
      loading.refresh = false
    }
  }

  async function saveSettings(): Promise<void> {
    loading.settings = true
    try {
      const settings = await vpnApi.updateSettings(buildVpnSettingsPayload(settingsDraft.value))
      settingsDraft.value = createVpnSettingsDraft(settings)
      ElMessage.success(t('vpn.messages.settingsSaved'))
    } catch (error) {
      ElMessage.error(resolveMessage(error, 'vpn.messages.workspaceLoadFailed'))
    } finally {
      loading.settings = false
    }
  }

  function openCreateProfile(): void {
    editingProfileId.value = ''
    profileDraft.value = createVpnProfileDraft()
    profileDialogVisible.value = true
  }

  function openEditProfile(profileId: string): void {
    const profile = profiles.value.find((item) => item.profileId === profileId)
    if (!profile) {
      return
    }
    editingProfileId.value = profileId
    profileDraft.value = createVpnProfileDraft(profile)
    profileDialogVisible.value = true
  }

  function closeProfileDialog(): void {
    profileDialogVisible.value = false
    editingProfileId.value = ''
    profileDraft.value = createVpnProfileDraft()
  }

  async function submitProfile(): Promise<void> {
    loading.profile = true
    try {
      const payload = buildVpnProfilePayload(profileDraft.value)
      if (editingProfileId.value) {
        await vpnApi.updateProfile(editingProfileId.value, payload)
        ElMessage.success(t('vpn.messages.profileUpdated'))
      } else {
        await vpnApi.createProfile(payload)
        ElMessage.success(t('vpn.messages.profileCreated'))
      }
      await refreshPolicies()
      closeProfileDialog()
    } catch (error) {
      ElMessage.error(resolveMessage(error, 'vpn.messages.workspaceLoadFailed'))
    } finally {
      loading.profile = false
    }
  }

  async function deleteProfile(profileId: string): Promise<void> {
    try {
      await ElMessageBox.confirm(t('vpn.messages.deleteProfileConfirm'), t('vpn.panel.profiles'), {
        type: 'warning'
      })
    } catch {
      return
    }
    loading.deleteProfileId = profileId
    try {
      await vpnApi.deleteProfile(profileId)
      await refreshPolicies()
      ElMessage.success(t('vpn.messages.profileDeleted'))
    } catch (error) {
      ElMessage.error(resolveMessage(error, 'vpn.messages.workspaceLoadFailed'))
    } finally {
      loading.deleteProfileId = ''
    }
  }

  async function quickConnect(profileId?: string): Promise<void> {
    loading.quickConnect = !profileId
    loading.quickConnectProfileId = profileId || ''
    try {
      currentSession.value = await vpnApi.quickConnect(profileId)
      await refreshSessionState()
      ElMessage.success(t('vpn.messages.quickConnectSuccess'))
    } catch (error) {
      ElMessage.error(resolveMessage(error, 'vpn.messages.workspaceLoadFailed'))
    } finally {
      loading.quickConnect = false
      loading.quickConnectProfileId = ''
    }
  }

  async function connectServer(serverId: string): Promise<void> {
    loading.connectServerId = serverId
    try {
      currentSession.value = await vpnApi.connect({ serverId, protocol: selectedProtocol.value })
      await refreshSessionState()
      ElMessage.success(t('vpn.messages.connectSuccess'))
    } catch (error) {
      ElMessage.error(resolveMessage(error, 'vpn.messages.workspaceLoadFailed'))
    } finally {
      loading.connectServerId = ''
    }
  }

  async function disconnectSession(): Promise<void> {
    if (!isConnected.value) {
      return
    }
    try {
      await ElMessageBox.confirm(t('vpn.messages.disconnectConfirm'), t('vpn.panel.session'), {
        type: 'warning'
      })
    } catch {
      return
    }
    loading.disconnect = true
    try {
      await vpnApi.disconnect()
      await refreshSessionState()
      ElMessage.success(t('vpn.messages.disconnectSuccess'))
    } catch (error) {
      ElMessage.error(resolveMessage(error, 'vpn.messages.workspaceLoadFailed'))
    } finally {
      loading.disconnect = false
    }
  }

  return {
    currentSession,
    defaultProfileName,
    editingProfileId,
    heroMetrics,
    history,
    isConnected,
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
  }
}
