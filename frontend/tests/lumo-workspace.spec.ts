import { describe, expect, it } from 'vitest'
import { messages } from '../locales'
import type {
  LumoConversation,
  LumoProjectKnowledgeParity as LumoProjectKnowledge,
  LumoProjectParity as LumoProject
} from '../types/suite-lumo'
import { translate } from '../utils/i18n'
import {
  buildLumoWorkspaceMetrics,
  createLumoProjectNameMap,
  filterValidKnowledgeSelection,
  LUMO_MODEL_CODES,
  LUMO_TRANSLATE_LOCALES,
  resolveLumoProjectLabel
} from '../utils/lumo-workspace'

const projects: LumoProject[] = [
  {
    projectId: 'proj-1',
    name: 'Launch prep',
    description: 'launch',
    conversationCount: 2,
    createdAt: '2026-03-12T00:00:00',
    updatedAt: '2026-03-12T01:00:00'
  }
]

const conversations: LumoConversation[] = [
  {
    conversationId: 'conv-1',
    projectId: 'proj-1',
    title: 'Risk review',
    pinned: false,
    modelCode: 'LUMO-BASE',
    archived: false,
    createdAt: '2026-03-12T00:00:00',
    updatedAt: '2026-03-12T02:00:00'
  },
  {
    conversationId: 'conv-2',
    projectId: null,
    title: 'Archive trail',
    pinned: false,
    modelCode: 'LUMO-PLUS',
    archived: true,
    createdAt: '2026-03-12T00:00:00',
    updatedAt: '2026-03-12T03:00:00'
  }
]

const knowledgeItems: LumoProjectKnowledge[] = [
  {
    knowledgeId: 'know-1',
    projectId: 'proj-1',
    title: 'Checklist',
    content: 'Scope',
    createdAt: '2026-03-12T00:00:00',
    updatedAt: '2026-03-12T04:00:00'
  },
  {
    knowledgeId: 'know-2',
    projectId: 'proj-1',
    title: 'Constraints',
    content: 'Privacy first',
    createdAt: '2026-03-12T00:00:00',
    updatedAt: '2026-03-12T05:00:00'
  }
]

describe('lumo workspace helpers', () => {
  it('exposes fixed model options and summary metrics', () => {
    expect(LUMO_MODEL_CODES).toEqual(['LUMO-BASE', 'LUMO-PLUS', 'LUMO-BIZ'])
    expect(LUMO_TRANSLATE_LOCALES).toEqual(['SYSTEM', 'en', 'zh-CN', 'zh-TW'])
    expect(buildLumoWorkspaceMetrics(projects, conversations, knowledgeItems)).toEqual({
      projectCount: 1,
      liveConversationCount: 1,
      archivedConversationCount: 1,
      knowledgeCount: 2
    })
  })

  it('resolves project labels and filters invalid knowledge selections', () => {
    const projectNameMap = createLumoProjectNameMap(projects)

    expect(resolveLumoProjectLabel('proj-1', projectNameMap, 'Unassigned', 'Project proj-1')).toBe('Launch prep')
    expect(resolveLumoProjectLabel(null, projectNameMap, 'Unassigned', 'Project unknown')).toBe('Unassigned')
    expect(resolveLumoProjectLabel('proj-9', projectNameMap, 'Unassigned', 'Project proj-9')).toBe('Project proj-9')

    expect(filterValidKnowledgeSelection(knowledgeItems, ['know-1', 'missing', 'know-1', 'know-2'])).toEqual([
      'know-1',
      'know-2'
    ])
  })

  it('registers lumo translations in all supported locales', () => {
    expect(translate(messages, 'en', 'lumo.hero.badge')).toBe('Private AI workspace')
    expect(translate(messages, 'zh-CN', 'lumo.sidebar.createProject')).toBe('创建项目')
    expect(translate(messages, 'zh-TW', 'lumo.model.LUMO-PLUS')).toBe('Lumo 進階')
    expect(translate(messages, 'en', 'lumo.capability.mode.SEARCH_TRANSLATE')).toBe(
      'Curated public-source scan + translated output'
    )
    expect(translate(messages, 'zh-CN', 'lumo.translate.output', { locale: '简体中文' })).toBe('输出：简体中文')
    expect(translate(messages, 'zh-TW', 'lumo.citations.source.PROJECT_KNOWLEDGE')).toBe('專案知識')
    expect(translate(messages, 'zh-CN', 'lumo.messages.deleteKnowledgeConfirm', { title: '发布清单' })).toBe(
      '确认删除知识“发布清单”？'
    )
  })
})
