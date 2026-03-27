export interface MailFolderNode {
  id: string
  parentId: string | null
  name: string
  color: string
  notificationsEnabled: boolean
  unreadCount: number
  totalCount: number
  updatedAt: string
  children: MailFolderNode[]
}

export interface MailFolderDraft {
  name: string
  color: string
  parentId: string
  notificationsEnabled: boolean
}

export interface MailFolderPayload {
  name: string
  color?: string
  parentId?: string
  notificationsEnabled: boolean
}

export interface MailFolderOption {
  id: string
  name: string
  label: string
  color: string
  depth: number
  parentId: string | null
  unreadCount: number
  totalCount: number
}
