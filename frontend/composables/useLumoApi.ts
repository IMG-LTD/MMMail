import type {
  ApiResponse,
  CreateLumoConversationRequest,
  CreateLumoProjectKnowledgeRequest,
  CreateLumoProjectRequest,
  LumoProject,
  UpdateLumoConversationArchiveRequest,
  UpdateLumoConversationModelRequest
} from '~/types/api'
import type {
  LumoConversation,
  LumoMessage,
  LumoProjectKnowledgeParity as LumoProjectKnowledge,
  SendLumoMessageRequest
} from '~/types/suite-lumo'

export function useLumoApi() {
  const { $apiClient } = useNuxtApp()

  async function listProjects(limit = 50): Promise<LumoProject[]> {
    const response = await $apiClient.get<ApiResponse<LumoProject[]>>('/api/v1/lumo/projects', {
      params: { limit }
    })
    return response.data.data
  }

  async function createProject(payload: CreateLumoProjectRequest): Promise<LumoProject> {
    const response = await $apiClient.post<ApiResponse<LumoProject>>('/api/v1/lumo/projects', payload)
    return response.data.data
  }

  async function listProjectKnowledge(projectId: string, limit = 100): Promise<LumoProjectKnowledge[]> {
    const response = await $apiClient.get<ApiResponse<LumoProjectKnowledge[]>>(`/api/v1/lumo/projects/${projectId}/knowledge`, {
      params: { limit }
    })
    return response.data.data
  }

  async function createProjectKnowledge(projectId: string, payload: CreateLumoProjectKnowledgeRequest): Promise<LumoProjectKnowledge> {
    const response = await $apiClient.post<ApiResponse<LumoProjectKnowledge>>(
      `/api/v1/lumo/projects/${projectId}/knowledge`,
      payload
    )
    return response.data.data
  }

  async function deleteProjectKnowledge(projectId: string, knowledgeId: string): Promise<boolean> {
    const response = await $apiClient.delete<ApiResponse<boolean>>(
      `/api/v1/lumo/projects/${projectId}/knowledge/${knowledgeId}`
    )
    return response.data.data
  }

  async function listConversations(limit = 50, includeArchived = false, projectId?: string): Promise<LumoConversation[]> {
    const response = await $apiClient.get<ApiResponse<LumoConversation[]>>('/api/v1/lumo/conversations', {
      params: {
        limit,
        includeArchived: includeArchived || undefined,
        projectId: projectId || undefined
      }
    })
    return response.data.data
  }

  async function createConversation(payload: CreateLumoConversationRequest): Promise<LumoConversation> {
    const response = await $apiClient.post<ApiResponse<LumoConversation>>('/api/v1/lumo/conversations', payload)
    return response.data.data
  }

  async function updateConversationModel(conversationId: string, payload: UpdateLumoConversationModelRequest): Promise<LumoConversation> {
    const response = await $apiClient.post<ApiResponse<LumoConversation>>(
      `/api/v1/lumo/conversations/${conversationId}/model`,
      payload
    )
    return response.data.data
  }

  async function archiveConversation(conversationId: string, payload: UpdateLumoConversationArchiveRequest): Promise<LumoConversation> {
    const response = await $apiClient.post<ApiResponse<LumoConversation>>(
      `/api/v1/lumo/conversations/${conversationId}/archive`,
      payload
    )
    return response.data.data
  }

  async function listMessages(conversationId: string, limit = 100): Promise<LumoMessage[]> {
    const response = await $apiClient.get<ApiResponse<LumoMessage[]>>(`/api/v1/lumo/conversations/${conversationId}/messages`, {
      params: { limit }
    })
    return response.data.data
  }

  async function sendMessage(conversationId: string, payload: SendLumoMessageRequest): Promise<LumoMessage[]> {
    const response = await $apiClient.post<ApiResponse<LumoMessage[]>>(
      `/api/v1/lumo/conversations/${conversationId}/messages`,
      payload
    )
    return response.data.data
  }

  return {
    listProjects,
    createProject,
    listProjectKnowledge,
    createProjectKnowledge,
    deleteProjectKnowledge,
    listConversations,
    createConversation,
    updateConversationModel,
    archiveConversation,
    listMessages,
    sendMessage
  }
}
