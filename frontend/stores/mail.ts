import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MailPage, MailSummary, MailboxStats, SystemMailFolder } from '~/types/api'
import type { MailFolderNode } from '~/types/mail-folders'

const EMPTY_FOLDERS: Record<SystemMailFolder, MailSummary[]> = {
  INBOX: [],
  SENT: [],
  DRAFTS: [],
  OUTBOX: [],
  ARCHIVE: [],
  SPAM: [],
  TRASH: [],
  SCHEDULED: [],
  SNOOZED: []
}

const EMPTY_COUNTS: Record<SystemMailFolder, number> = {
  INBOX: 0,
  SENT: 0,
  DRAFTS: 0,
  OUTBOX: 0,
  ARCHIVE: 0,
  SPAM: 0,
  TRASH: 0,
  SCHEDULED: 0,
  SNOOZED: 0
}

export const useMailStore = defineStore('mail', () => {
  const folders = ref<Record<SystemMailFolder, MailSummary[]>>({ ...EMPTY_FOLDERS })
  const counts = ref<Record<SystemMailFolder, number>>({ ...EMPTY_COUNTS })
  const customFolders = ref<MailFolderNode[]>([])
  const unreadCount = ref<number>(0)
  const starredCount = ref<number>(0)

  function setFolder(folder: SystemMailFolder, page: MailPage): void {
    folders.value[folder] = page.items
    unreadCount.value = page.unread
  }

  function updateStats(stats: MailboxStats): void {
    counts.value = {
      INBOX: stats.folderCounts.INBOX || 0,
      SENT: stats.folderCounts.SENT || 0,
      DRAFTS: stats.folderCounts.DRAFTS || 0,
      OUTBOX: stats.folderCounts.OUTBOX || 0,
      ARCHIVE: stats.folderCounts.ARCHIVE || 0,
      SPAM: stats.folderCounts.SPAM || 0,
      TRASH: stats.folderCounts.TRASH || 0,
      SCHEDULED: stats.folderCounts.SCHEDULED || 0,
      SNOOZED: stats.folderCounts.SNOOZED || 0
    }
    unreadCount.value = stats.unreadCount
    starredCount.value = stats.starredCount
  }

  function setCustomFolders(items: MailFolderNode[]): void {
    customFolders.value = items
  }

  function getFolder(folder: SystemMailFolder): MailSummary[] {
    return folders.value[folder]
  }

  function reset(): void {
    folders.value = { ...EMPTY_FOLDERS }
    counts.value = { ...EMPTY_COUNTS }
    customFolders.value = []
    unreadCount.value = 0
    starredCount.value = 0
  }

  return {
    folders,
    counts,
    customFolders,
    unreadCount,
    starredCount,
    setFolder,
    updateStats,
    setCustomFolders,
    getFolder,
    reset
  }
})
