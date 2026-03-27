import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useLumoApi } from '~/composables/useLumoApi'
import type {
  LumoConversation,
  LumoMessage,
  LumoProjectKnowledgeParity as LumoProjectKnowledge,
  LumoProjectParity as LumoProject,
  LumoTranslateLocale
} from '~/types/suite-lumo'
import {
  buildLumoWorkspaceMetrics,
  createLumoProjectNameMap,
  filterValidKnowledgeSelection,
  LUMO_MODEL_CODES,
  LUMO_TRANSLATE_LOCALES,
  resolveLumoProjectLabel,
  type LumoModelCode
} from '~/utils/lumo-workspace'

const PROJECT_LIMIT = 100
const CONVERSATION_LIMIT = 100
const MESSAGE_LIMIT = 200
const KNOWLEDGE_LIMIT = 200
const MIN_TITLE_LENGTH = 2

interface CreateProjectPayload {
  name: string
  description: string
}

interface CreateConversationPayload {
  title: string
  modelCode: LumoModelCode
  projectId?: string
}

interface CreateKnowledgePayload {
  title: string
  content: string
}

function messageFromError(error: unknown, fallbackMessage: string): string {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }
  return fallbackMessage
}

export function useLumoWorkspace() {
  const { locale, t } = useI18n()
  const {
    archiveConversation,
    createConversation,
    createProject,
    createProjectKnowledge,
    deleteProjectKnowledge,
    listConversations,
    listMessages,
    listProjectKnowledge,
    listProjects,
    sendMessage,
    updateConversationModel
  } = useLumoApi()

  const loading = ref(false)
  const creatingConversation = ref(false)
  const sending = ref(false)
  const updatingModel = ref(false)
  const archiving = ref(false)
  const creatingProject = ref(false)
  const creatingKnowledge = ref(false)
  const deletingKnowledgeId = ref('')
  const includeArchived = ref(false)
  const selectedProjectId = ref('')
  const activeConversationId = ref('')
  const selectedKnowledgeIds = ref<string[]>([])
  const webSearchEnabled = ref(true)
  const citationsEnabled = ref(true)
  const translateLocale = ref<LumoTranslateLocale>('SYSTEM')
  const projects = ref<LumoProject[]>([])
  const conversations = ref<LumoConversation[]>([])
  const messages = ref<LumoMessage[]>([])
  const projectKnowledge = ref<LumoProjectKnowledge[]>([])

  const projectNameMap = computed(() => createLumoProjectNameMap(projects.value))
  const activeConversation = computed(
    () => conversations.value.find(item => item.conversationId === activeConversationId.value) ?? null
  )
  const activeProjectId = computed(() => activeConversation.value?.projectId ?? '')
  const summary = computed(() => buildLumoWorkspaceMetrics(
    projects.value,
    conversations.value,
    projectKnowledge.value
  ))
  const activeProjectLabel = computed(() => resolveProjectLabel(activeConversation.value?.projectId ?? null))

  function resolveProjectLabel(projectId: string | null): string {
    const fallbackLabel = projectId
      ? t('lumo.sidebar.projectFallback', { id: projectId })
      : t('lumo.sidebar.unassigned')
    return resolveLumoProjectLabel(
      projectId,
      projectNameMap.value,
      t('lumo.sidebar.unassigned'),
      fallbackLabel
    )
  }

  function resolveModelLabel(modelCode: LumoModelCode): string {
    return t(`lumo.model.${modelCode}`)
  }

  function resolveTranslateLocaleLabel(value: LumoTranslateLocale): string {
    return t(`lumo.translate.locale.${value}`)
  }

  async function refreshProjects(): Promise<void> {
    projects.value = await listProjects(PROJECT_LIMIT)
    if (!selectedProjectId.value) {
      return
    }
    const hasSelectedProject = projects.value.some(item => item.projectId === selectedProjectId.value)
    if (!hasSelectedProject) {
      selectedProjectId.value = ''
    }
  }

  async function refreshMessages(): Promise<void> {
    if (!activeConversationId.value) {
      messages.value = []
      return
    }
    messages.value = await listMessages(activeConversationId.value, MESSAGE_LIMIT)
  }

  async function refreshProjectKnowledge(): Promise<void> {
    if (!activeProjectId.value) {
      projectKnowledge.value = []
      selectedKnowledgeIds.value = []
      return
    }
    projectKnowledge.value = await listProjectKnowledge(activeProjectId.value, KNOWLEDGE_LIMIT)
    selectedKnowledgeIds.value = filterValidKnowledgeSelection(
      projectKnowledge.value,
      selectedKnowledgeIds.value
    )
  }

  async function refreshConversations(withDetails = true): Promise<void> {
    conversations.value = await listConversations(
      CONVERSATION_LIMIT,
      includeArchived.value,
      selectedProjectId.value || undefined
    )
    const hasActiveConversation = conversations.value.some(
      item => item.conversationId === activeConversationId.value
    )
    if (!hasActiveConversation) {
      activeConversationId.value = conversations.value[0]?.conversationId ?? ''
    }
    if (!withDetails) {
      return
    }
    await Promise.all([refreshMessages(), refreshProjectKnowledge()])
  }

  async function loadData(): Promise<void> {
    loading.value = true
    try {
      await refreshProjects()
      await refreshConversations(true)
    } catch (error) {
      ElMessage.error(messageFromError(error, t('lumo.messages.loadFailed')))
    } finally {
      loading.value = false
    }
  }

  async function onCreateProject(payload: CreateProjectPayload): Promise<boolean> {
    const name = payload.name.trim()
    if (name.length < MIN_TITLE_LENGTH) {
      ElMessage.warning(t('lumo.messages.projectNameTooShort'))
      return false
    }
    creatingProject.value = true
    try {
      const created = await createProject({
        name,
        description: payload.description.trim() || undefined
      })
      selectedProjectId.value = created.projectId
      await refreshProjects()
      await refreshConversations(true)
      ElMessage.success(t('lumo.messages.projectCreated'))
      return true
    } catch (error) {
      ElMessage.error(messageFromError(error, t('lumo.messages.projectCreateFailed')))
      return false
    } finally {
      creatingProject.value = false
    }
  }

  async function onCreateConversation(payload: CreateConversationPayload): Promise<boolean> {
    const title = payload.title.trim()
    if (title.length < MIN_TITLE_LENGTH) {
      ElMessage.warning(t('lumo.messages.conversationTitleTooShort'))
      return false
    }
    creatingConversation.value = true
    try {
      const created = await createConversation({
        title,
        modelCode: payload.modelCode,
        projectId: payload.projectId || undefined
      })
      if (payload.projectId) {
        selectedProjectId.value = payload.projectId
      }
      activeConversationId.value = created.conversationId
      await refreshProjects()
      await refreshConversations(true)
      ElMessage.success(t('lumo.messages.conversationCreated'))
      return true
    } catch (error) {
      ElMessage.error(messageFromError(error, t('lumo.messages.conversationCreateFailed')))
      return false
    } finally {
      creatingConversation.value = false
    }
  }

  async function onChangeProjectFilter(projectId: string): Promise<void> {
    selectedProjectId.value = projectId
    await refreshConversations(true)
  }

  async function onToggleIncludeArchived(nextValue: boolean): Promise<void> {
    includeArchived.value = nextValue
    await refreshConversations(true)
  }

  async function onSelectConversation(conversationId: string): Promise<void> {
    if (activeConversationId.value === conversationId) {
      return
    }
    activeConversationId.value = conversationId
    await Promise.all([refreshMessages(), refreshProjectKnowledge()])
  }

  async function onUpdateModel(nextModelCode: LumoModelCode): Promise<void> {
    if (!activeConversation.value) {
      ElMessage.warning(t('lumo.messages.selectConversationFirst'))
      return
    }
    updatingModel.value = true
    try {
      await updateConversationModel(activeConversation.value.conversationId, {
        modelCode: nextModelCode
      })
      await Promise.all([refreshConversations(false), refreshProjectKnowledge()])
      ElMessage.success(t('lumo.messages.modelUpdated'))
    } catch (error) {
      ElMessage.error(messageFromError(error, t('lumo.messages.modelUpdateFailed')))
    } finally {
      updatingModel.value = false
    }
  }

  async function onToggleArchive(nextArchived: boolean): Promise<void> {
    if (!activeConversation.value) {
      ElMessage.warning(t('lumo.messages.selectConversationFirst'))
      return
    }
    archiving.value = true
    try {
      await archiveConversation(activeConversation.value.conversationId, { archived: nextArchived })
      await refreshConversations(true)
      ElMessage.success(
        t(nextArchived ? 'lumo.messages.conversationArchived' : 'lumo.messages.conversationRestored')
      )
    } catch (error) {
      ElMessage.error(messageFromError(error, t('lumo.messages.archiveUpdateFailed')))
    } finally {
      archiving.value = false
    }
  }

  async function onSendMessage(content: string): Promise<boolean> {
    if (!activeConversation.value || !activeConversationId.value) {
      ElMessage.warning(t('lumo.messages.selectConversationFirst'))
      return false
    }
    if (activeConversation.value.archived) {
      ElMessage.warning(t('lumo.messages.archivedConversationReadonly'))
      return false
    }
    const messageContent = content.trim()
    if (!messageContent) {
      ElMessage.warning(t('lumo.messages.messageRequired'))
      return false
    }
    sending.value = true
    try {
      await sendMessage(activeConversationId.value, {
        content: messageContent,
        knowledgeIds: selectedKnowledgeIds.value,
        webSearchEnabled: webSearchEnabled.value,
        citationsEnabled: citationsEnabled.value,
        translateToLocale: translateLocale.value === 'SYSTEM' ? locale.value : translateLocale.value
      })
      await Promise.all([refreshConversations(false), refreshMessages(), refreshProjectKnowledge()])
      return true
    } catch (error) {
      ElMessage.error(messageFromError(error, t('lumo.messages.sendFailed')))
      return false
    } finally {
      sending.value = false
    }
  }

  function setSelectedKnowledgeIds(nextSelectedKnowledgeIds: string[]): void {
    selectedKnowledgeIds.value = filterValidKnowledgeSelection(
      projectKnowledge.value,
      nextSelectedKnowledgeIds
    )
  }

  function setWebSearchEnabled(nextValue: boolean): void {
    webSearchEnabled.value = nextValue
  }

  function setCitationsEnabled(nextValue: boolean): void {
    citationsEnabled.value = nextValue
  }

  function setTranslateLocale(nextValue: LumoTranslateLocale): void {
    translateLocale.value = nextValue
  }

  async function onCreateKnowledge(payload: CreateKnowledgePayload): Promise<boolean> {
    if (!activeProjectId.value) {
      ElMessage.warning(t('lumo.messages.knowledgeRequiresProject'))
      return false
    }
    const title = payload.title.trim()
    const content = payload.content.trim()
    if (title.length < MIN_TITLE_LENGTH) {
      ElMessage.warning(t('lumo.messages.knowledgeTitleTooShort'))
      return false
    }
    if (!content) {
      ElMessage.warning(t('lumo.messages.knowledgeContentRequired'))
      return false
    }
    creatingKnowledge.value = true
    try {
      const created = await createProjectKnowledge(activeProjectId.value, { title, content })
      await refreshProjectKnowledge()
      if (!selectedKnowledgeIds.value.includes(created.knowledgeId)) {
        selectedKnowledgeIds.value = [...selectedKnowledgeIds.value, created.knowledgeId]
      }
      ElMessage.success(t('lumo.messages.knowledgeCreated'))
      return true
    } catch (error) {
      ElMessage.error(messageFromError(error, t('lumo.messages.knowledgeCreateFailed')))
      return false
    } finally {
      creatingKnowledge.value = false
    }
  }

  async function onDeleteKnowledge(item: LumoProjectKnowledge): Promise<void> {
    if (!activeProjectId.value) {
      return
    }
    try {
      await ElMessageBox.confirm(
        t('lumo.messages.deleteKnowledgeConfirm', { title: item.title }),
        t('lumo.messages.deleteKnowledgeTitle'),
        {
          type: 'warning',
          confirmButtonText: t('lumo.knowledge.delete'),
          cancelButtonText: t('common.actions.cancel')
        }
      )
    } catch {
      return
    }
    deletingKnowledgeId.value = item.knowledgeId
    try {
      await deleteProjectKnowledge(activeProjectId.value, item.knowledgeId)
      await refreshProjectKnowledge()
      ElMessage.success(t('lumo.messages.knowledgeDeleted'))
    } catch (error) {
      ElMessage.error(messageFromError(error, t('lumo.messages.knowledgeDeleteFailed')))
    } finally {
      deletingKnowledgeId.value = ''
    }
  }

  onMounted(() => {
    void loadData()
  })

  return {
    loading,
    creatingConversation,
    sending,
    updatingModel,
    archiving,
    creatingProject,
    creatingKnowledge,
    deletingKnowledgeId,
    includeArchived,
    selectedProjectId,
    activeConversationId,
    selectedKnowledgeIds,
    webSearchEnabled,
    citationsEnabled,
    translateLocale,
    projects,
    conversations,
    messages,
    projectKnowledge,
    activeConversation,
    activeProjectId,
    activeProjectLabel,
    summary,
    modelOptions: LUMO_MODEL_CODES,
    translateLocaleOptions: LUMO_TRANSLATE_LOCALES,
    resolveProjectLabel,
    resolveModelLabel,
    resolveTranslateLocaleLabel,
    loadData,
    onCreateProject,
    onCreateConversation,
    onChangeProjectFilter,
    onToggleIncludeArchived,
    onSelectConversation,
    onUpdateModel,
    onToggleArchive,
    onSendMessage,
    setSelectedKnowledgeIds,
    setWebSearchEnabled,
    setCitationsEnabled,
    setTranslateLocale,
    onCreateKnowledge,
    onDeleteKnowledge
  }
}
