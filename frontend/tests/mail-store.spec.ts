import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useMailStore } from '../stores/mail'

describe('mail store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('sets folder data and unread count', () => {
    const store = useMailStore()
    store.setFolder('INBOX', {
      items: [
        {
          id: '1',
          ownerId: '2',
          senderEmail: null,
          peerEmail: 'admin@mmmail.local',
          folderType: 'INBOX',
          customFolderId: null,
          customFolderName: null,
          subject: 'Test',
          preview: 'Preview',
          isRead: false,
          isStarred: false,
          isDraft: false,
          sentAt: new Date().toISOString(),
          labels: []
        }
      ],
      total: 1,
      page: 1,
      size: 20,
      unread: 3
    })

    expect(store.getFolder('INBOX')).toHaveLength(1)
    expect(store.unreadCount).toBe(3)
  })

  it('updates folder counts and starred number', () => {
    const store = useMailStore()
    store.updateStats({
      folderCounts: {
        INBOX: 8,
        SENT: 5,
        DRAFTS: 2,
        OUTBOX: 4,
        SCHEDULED: 1,
        SNOOZED: 3,
        ARCHIVE: 10,
        SPAM: 1,
        TRASH: 0
      },
      unreadCount: 4,
      starredCount: 6
    })

    expect(store.counts.ARCHIVE).toBe(10)
    expect(store.unreadCount).toBe(4)
    expect(store.starredCount).toBe(6)
  })
})
