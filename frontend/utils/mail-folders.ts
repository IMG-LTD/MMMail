import type { MailFolderDraft, MailFolderNode, MailFolderOption, MailFolderPayload } from '../types/mail-folders'

const DEFAULT_MAIL_FOLDER_COLOR = '#5B7CFA'
const COLOR_PATTERN = /^#[0-9A-Fa-f]{6}$/

export function createMailFolderDraft(parentId = ''): MailFolderDraft {
  return {
    name: '',
    color: DEFAULT_MAIL_FOLDER_COLOR,
    parentId,
    notificationsEnabled: true
  }
}

export function buildMailFolderPayload(draft: MailFolderDraft): MailFolderPayload {
  const name = draft.name.trim()
  const color = normalizeColor(draft.color)

  if (!name) {
    throw new Error('name')
  }

  return {
    name,
    color,
    parentId: draft.parentId || undefined,
    notificationsEnabled: draft.notificationsEnabled
  }
}

export function flattenMailFolderTree(nodes: MailFolderNode[], depth = 0): MailFolderOption[] {
  const items: MailFolderOption[] = []
  for (const node of nodes) {
    items.push({
      id: node.id,
      name: node.name,
      label: `${'· '.repeat(depth)}${node.name}`,
      color: node.color,
      depth,
      parentId: node.parentId,
      unreadCount: node.unreadCount,
      totalCount: node.totalCount
    })
    items.push(...flattenMailFolderTree(node.children, depth + 1))
  }
  return items
}

export function findMailFolderNode(nodes: MailFolderNode[], folderId: string): MailFolderNode | null {
  for (const node of nodes) {
    if (node.id === folderId) {
      return node
    }
    const child = findMailFolderNode(node.children, folderId)
    if (child) {
      return child
    }
  }
  return null
}

export function findMailFolderPath(nodes: MailFolderNode[], folderId: string): MailFolderNode[] {
  for (const node of nodes) {
    if (node.id === folderId) {
      return [node]
    }
    const childPath = findMailFolderPath(node.children, folderId)
    if (childPath.length) {
      return [node, ...childPath]
    }
  }
  return []
}

function normalizeColor(value: string): string {
  const normalized = value.trim().toUpperCase() || DEFAULT_MAIL_FOLDER_COLOR
  if (!COLOR_PATTERN.test(normalized)) {
    throw new Error('color')
  }
  return normalized
}
