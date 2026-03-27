import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AuthenticatorAlgorithm, AuthenticatorEntrySummary } from '~/types/api'
import { useAuthenticatorApi } from '~/composables/useAuthenticatorApi'
import { useI18n } from '~/composables/useI18n'

interface AuthenticatorWorkspaceOptions {
  onEntryCreated?: () => Promise<void>
  onEntrySaved?: () => Promise<void>
  onEntryDeleted?: () => Promise<void>
}

const DEFAULT_ALGORITHM: AuthenticatorAlgorithm = 'SHA1'
const DEFAULT_DIGITS = 6
const DEFAULT_PERIOD_SECONDS = 30
const DEFAULT_SECRET = 'JBSWY3DPEHPK3PXP'

export function useAuthenticatorWorkspace(options: AuthenticatorWorkspaceOptions = {}) {
  const { t } = useI18n()
  const { listEntries, createEntry, getEntry, updateEntry, deleteEntry, generateCode } = useAuthenticatorApi()

  const keyword = ref('')
  const loading = ref(false)
  const creating = ref(false)
  const saving = ref(false)
  const deleting = ref(false)
  const loadingCode = ref(false)
  const activeEntryId = ref('')
  const entries = ref<AuthenticatorEntrySummary[]>([])
  const codeTimer = ref<ReturnType<typeof setInterval> | null>(null)

  const editor = reactive({
    issuer: '',
    accountName: '',
    secretCiphertext: '',
    algorithm: DEFAULT_ALGORITHM,
    digits: DEFAULT_DIGITS,
    periodSeconds: DEFAULT_PERIOD_SECONDS,
    updatedAt: ''
  })

  const codePanel = reactive({
    code: '------',
    expiresInSeconds: 0,
    periodSeconds: DEFAULT_PERIOD_SECONDS,
    digits: DEFAULT_DIGITS,
    lastRefreshedAt: ''
  })

  const hasActiveEntry = computed(() => Boolean(activeEntryId.value))
  const countdownPercent = computed(() => {
    if (!codePanel.periodSeconds || codePanel.expiresInSeconds <= 0) {
      return 0
    }
    return Math.max(0, Math.min(100, Math.round((codePanel.expiresInSeconds / codePanel.periodSeconds) * 100)))
  })

  function dispose(): void {
    clearCodeTicker()
  }

  async function initialize(): Promise<void> {
    await loadEntries(false)
  }

  async function loadEntries(keepSelection = true): Promise<void> {
    loading.value = true
    try {
      const next = await listEntries(keyword.value.trim(), 200)
      entries.value = next
      if (!next.length) {
        clearEditor()
        return
      }
      if (!keepSelection || !activeEntryId.value || !next.some(entry => entry.id === activeEntryId.value)) {
        await selectEntry(next[0].id)
      }
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'authenticator.messages.loadEntriesFailed'))
    } finally {
      loading.value = false
    }
  }

  async function selectEntry(entryId: string): Promise<void> {
    try {
      const detail = await getEntry(entryId)
      activeEntryId.value = detail.id
      editor.issuer = detail.issuer
      editor.accountName = detail.accountName
      editor.secretCiphertext = detail.secretCiphertext
      editor.algorithm = normalizeAlgorithm(detail.algorithm)
      editor.digits = detail.digits
      editor.periodSeconds = detail.periodSeconds
      editor.updatedAt = detail.updatedAt
      await refreshCode(false)
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'authenticator.messages.loadEntryFailed'))
    }
  }

  async function createDefaultEntry(): Promise<void> {
    creating.value = true
    try {
      const suffix = Math.floor(Date.now() / 1000).toString().slice(-6)
      const created = await createEntry({
        issuer: `MMMail ${suffix}`,
        accountName: `user${suffix}@mmmail.local`,
        secretCiphertext: DEFAULT_SECRET,
        algorithm: DEFAULT_ALGORITHM,
        digits: DEFAULT_DIGITS,
        periodSeconds: DEFAULT_PERIOD_SECONDS
      })
      ElMessage.success(t('authenticator.messages.entryCreated'))
      await loadEntries(false)
      await selectEntry(created.id)
      await options.onEntryCreated?.()
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'authenticator.messages.createFailed'))
    } finally {
      creating.value = false
    }
  }

  async function saveCurrentEntry(): Promise<void> {
    if (!activeEntryId.value) {
      ElMessage.warning(t('authenticator.messages.selectEntryRequired'))
      return
    }
    const payload = normalizeEditorPayload()
    if (!payload) {
      ElMessage.warning(t('authenticator.messages.fieldsRequired'))
      return
    }
    saving.value = true
    try {
      const updated = await updateEntry(activeEntryId.value, payload)
      editor.updatedAt = updated.updatedAt
      ElMessage.success(t('authenticator.messages.entrySaved'))
      await loadEntries(true)
      await refreshCode(false)
      await options.onEntrySaved?.()
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'authenticator.messages.saveFailed'))
    } finally {
      saving.value = false
    }
  }

  async function deleteCurrentEntry(): Promise<void> {
    if (!activeEntryId.value) {
      ElMessage.warning(t('authenticator.messages.selectEntryRequired'))
      return
    }
    const confirmed = await confirmDelete()
    if (!confirmed) {
      return
    }
    deleting.value = true
    try {
      await deleteEntry(activeEntryId.value)
      ElMessage.success(t('authenticator.messages.entryDeleted'))
      await loadEntries(false)
      await options.onEntryDeleted?.()
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'authenticator.messages.deleteFailed'))
    } finally {
      deleting.value = false
    }
  }

  async function refreshCode(showSuccess = true): Promise<void> {
    if (!activeEntryId.value) {
      return
    }
    loadingCode.value = true
    try {
      const next = await generateCode(activeEntryId.value)
      codePanel.code = next.code
      codePanel.expiresInSeconds = next.expiresInSeconds
      codePanel.periodSeconds = next.periodSeconds
      codePanel.digits = next.digits
      codePanel.lastRefreshedAt = new Date().toISOString()
      startCodeTicker()
      if (showSuccess) {
        ElMessage.success(t('authenticator.messages.codeRefreshed'))
      }
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'authenticator.messages.refreshCodeFailed'))
    } finally {
      loadingCode.value = false
    }
  }

  async function copyCode(): Promise<void> {
    if (!hasActiveEntry.value || codePanel.code === '------') {
      ElMessage.warning(t('authenticator.messages.noCode'))
      return
    }
    try {
      if (!navigator.clipboard?.writeText) {
        throw new Error('Clipboard API unavailable')
      }
      await navigator.clipboard.writeText(codePanel.code)
      ElMessage.success(t('authenticator.messages.codeCopied'))
    } catch {
      ElMessage.error(t('authenticator.messages.copyCodeFailed'))
    }
  }

  function formatTime(value: string): string {
    return value || '-'
  }

  function clearCodeTicker(): void {
    if (!codeTimer.value) {
      return
    }
    clearInterval(codeTimer.value)
    codeTimer.value = null
  }

  function startCodeTicker(): void {
    clearCodeTicker()
    if (codePanel.expiresInSeconds <= 0) {
      return
    }
    codeTimer.value = setInterval(() => {
      if (codePanel.expiresInSeconds > 1) {
        codePanel.expiresInSeconds -= 1
        return
      }
      codePanel.expiresInSeconds = 0
      clearCodeTicker()
      void refreshCode(false)
    }, 1000)
  }

  function clearEditor(): void {
    activeEntryId.value = ''
    editor.issuer = ''
    editor.accountName = ''
    editor.secretCiphertext = ''
    editor.algorithm = DEFAULT_ALGORITHM
    editor.digits = DEFAULT_DIGITS
    editor.periodSeconds = DEFAULT_PERIOD_SECONDS
    editor.updatedAt = ''
    resetCodePanel()
  }

  function clearWorkspace(): void {
    entries.value = []
    clearEditor()
  }

  function resetCodePanel(): void {
    clearCodeTicker()
    codePanel.code = '------'
    codePanel.expiresInSeconds = 0
    codePanel.periodSeconds = editor.periodSeconds || DEFAULT_PERIOD_SECONDS
    codePanel.digits = editor.digits || DEFAULT_DIGITS
    codePanel.lastRefreshedAt = ''
  }

  function normalizeAlgorithm(algorithm: string): AuthenticatorAlgorithm {
    const value = algorithm.trim().toUpperCase()
    if (value === 'SHA256' || value === 'SHA512') {
      return value
    }
    return DEFAULT_ALGORITHM
  }

  function normalizeEditorPayload() {
    const issuer = editor.issuer.trim()
    const accountName = editor.accountName.trim()
    const secretCiphertext = editor.secretCiphertext.trim()
    if (!issuer || !accountName || !secretCiphertext) {
      return null
    }
    return {
      issuer,
      accountName,
      secretCiphertext,
      algorithm: editor.algorithm,
      digits: editor.digits,
      periodSeconds: editor.periodSeconds
    }
  }

  async function confirmDelete(): Promise<boolean> {
    try {
      await ElMessageBox.confirm(
        t('authenticator.messages.deleteConfirmMessage'),
        t('authenticator.messages.deleteConfirmTitle'),
        {
          type: 'warning',
          confirmButtonText: t('authenticator.messages.deleteConfirmButton'),
          cancelButtonText: t('authenticator.messages.cancelButton')
        }
      )
      return true
    } catch {
      return false
    }
  }

  function resolveErrorMessage(error: unknown, fallbackKey: string): string {
    return error instanceof Error && error.message ? error.message : t(fallbackKey)
  }

  return {
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
  }
}
