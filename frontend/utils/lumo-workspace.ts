import type {
  LumoConversation,
  LumoProjectKnowledgeParity as LumoProjectKnowledge,
  LumoProjectParity as LumoProject,
  LumoTranslateLocale
} from '../types/suite-lumo'

export type LumoModelCode = LumoConversation['modelCode']

export interface LumoWorkspaceMetrics {
  projectCount: number
  liveConversationCount: number
  archivedConversationCount: number
  knowledgeCount: number
}

export const LUMO_MODEL_CODES: readonly LumoModelCode[] = ['LUMO-BASE', 'LUMO-PLUS', 'LUMO-BIZ'] as const
export const LUMO_TRANSLATE_LOCALES: readonly LumoTranslateLocale[] = ['SYSTEM', 'en', 'zh-CN', 'zh-TW'] as const

export function createLumoProjectNameMap(projects: readonly LumoProject[]): Map<string, string> {
  const mapping = new Map<string, string>()
  for (const project of projects) {
    mapping.set(project.projectId, project.name)
  }
  return mapping
}

export function resolveLumoProjectLabel(
  projectId: string | null,
  projectNameMap: ReadonlyMap<string, string>,
  unassignedLabel: string,
  fallbackLabel: string
): string {
  if (!projectId) {
    return unassignedLabel
  }
  return projectNameMap.get(projectId) || fallbackLabel
}

export function filterValidKnowledgeSelection(
  knowledgeItems: readonly LumoProjectKnowledge[],
  selectedKnowledgeIds: readonly string[]
): string[] {
  const validKnowledgeIdSet = new Set(knowledgeItems.map(item => item.knowledgeId))
  const nextSelection = new Set<string>()
  for (const knowledgeId of selectedKnowledgeIds) {
    if (validKnowledgeIdSet.has(knowledgeId)) {
      nextSelection.add(knowledgeId)
    }
  }
  return Array.from(nextSelection)
}

export function buildLumoWorkspaceMetrics(
  projects: readonly LumoProject[],
  conversations: readonly LumoConversation[],
  knowledgeItems: readonly LumoProjectKnowledge[]
): LumoWorkspaceMetrics {
  const archivedConversationCount = conversations.filter(item => item.archived).length
  return {
    projectCount: projects.length,
    liveConversationCount: conversations.length - archivedConversationCount,
    archivedConversationCount,
    knowledgeCount: knowledgeItems.length
  }
}
