import { describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import type { MailSummary } from '../types/api'
import { buildMailListModel } from '../utils/mail-list'

const triageMail: MailSummary = {
  id: 'mail-1',
  ownerId: 'user-1',
  senderEmail: 'alice@example.com',
  peerEmail: 'alice@example.com',
  folderType: 'INBOX',
  customFolderId: null,
  customFolderName: null,
  subject: 'Quarterly update',
  preview: 'Need your decision',
  isRead: false,
  isStarred: true,
  isDraft: false,
  sentAt: '2026-03-13T10:20:30Z',
  labels: ['ops'],
  senderDisplayName: 'Alice',
  senderType: 'EXTERNAL',
  isImportantContact: true,
  hasAttachments: true,
  replyState: 'AWAITING_ME',
  needsReply: true,
  latestActor: 'OTHER',
  conversationMessageCount: 3
}

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    locale: { value: 'en-US' },
    t: (key: string, params?: Record<string, string | number>) => {
      if (key === 'mailList.header.items') {
        return `${params?.count} items`
      }
      if (key === 'mailList.batch.selected') {
        return `${params?.count} selected`
      }
      if (key === 'mailList.signals.messageCount') {
        return `${params?.count} messages`
      }
      if (key === 'mailList.subjectFallback') {
        return '(no subject)'
      }
      const dictionary: Record<string, string> = {
        'mailList.signals.importantContact': 'Important',
        'mailList.signals.senderType.EXTERNAL': 'External',
        'mailList.signals.needsReply': 'Needs reply',
        'mailList.signals.starred': 'Starred',
        'mailList.signals.attachments': 'Attachment',
        'mailList.signals.replyState.AWAITING_ME': 'Awaiting me',
        'mailList.signals.latestActor.OTHER': 'Last active: other side',
        'mailList.status.loading': 'Localized loading',
        'mailList.empty': 'Localized empty',
        'mailList.actions.markRead': 'Mark Read',
        'mailList.actions.markUnread': 'Mark Unread',
        'mailList.actions.read': 'Read',
        'mailList.actions.unread': 'Unread',
        'mailList.actions.star': 'Star',
        'mailList.actions.unstar': 'Unstar',
        'mailList.actions.snooze': 'Snooze',
        'mailList.actions.snooze24h': 'Snooze 24h',
        'mailList.actions.snooze7d': 'Snooze 7d',
        'mailList.actions.customSnooze': 'Snooze…',
        'mailList.actions.more': 'More',
        'mailList.actions.unsnooze': 'Unsnooze',
        'mailList.actions.archive': 'Archive',
        'mailList.actions.spam': 'Spam',
        'mailList.actions.trash': 'Trash',
        'mailList.actions.blockSender': 'Block Sender',
        'mailList.actions.trustSender': 'Trust Sender',
        'mailList.actions.blockDomain': 'Block Domain',
        'mailList.actions.trustDomain': 'Trust Domain',
        'mailList.actions.reportNotPhishing': 'Report Not Phishing',
        'mailList.actions.reportPhishing': 'Report Phishing',
        'mailList.actions.undoSend': 'Undo Send'
      }
      return dictionary[key] || key
    }
  })
}))

const elementStubs = {
  ElButton: defineComponent({
    name: 'ElButton',
    emits: ['click'],
    inheritAttrs: false,
    template: '<button v-bind="$attrs" type="button" @click="$emit(\'click\')"><slot /></button>'
  }),
  ElDropdown: defineComponent({
    name: 'ElDropdown',
    template: '<div class="el-dropdown-stub"><slot /><slot name="dropdown" /></div>'
  }),
  ElDropdownMenu: defineComponent({
    name: 'ElDropdownMenu',
    template: '<div class="el-dropdown-menu-stub"><slot /></div>'
  }),
  ElDropdownItem: defineComponent({
    name: 'ElDropdownItem',
    emits: ['click'],
    template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
  }),
  ElEmpty: defineComponent({
    name: 'ElEmpty',
    props: { description: { type: String, default: '' } },
    template: '<div class="el-empty-stub">{{ description }}</div>'
  })
}

function getDropdownMenuItem(wrapper: ReturnType<typeof mount>, label: string) {
  const item = wrapper.findAll('.el-dropdown-menu-stub button').find((button) => button.text() === label)
  expect(item).toBeTruthy()
  return item!
}

describe('mail list helpers', () => {
  it('derives grouped signals from triage summary fields', () => {
    const model = buildMailListModel(triageMail)

    expect(model.senderName).toBe('Alice')
    expect(model.avatarText).toBe('A')
    expect(model.identitySignals.map((signal) => signal.key)).toEqual([
      'importantContact',
      'senderType:EXTERNAL'
    ])
    expect(model.taskSignals.map((signal) => signal.key)).toEqual(['needsReply', 'starred'])
    expect(model.conversationSignals).toEqual([
      { key: 'attachments' },
      { key: 'replyState:AWAITING_ME' },
      { key: 'latestActor:OTHER' },
      { key: 'messageCount', value: 3 }
    ])
  })

  it('falls back to trimmed sender email and suppresses none or single-message conversation signals', () => {
    const model = buildMailListModel({
      ...triageMail,
      senderDisplayName: '   ',
      senderEmail: '  bob@example.com  ',
      peerEmail: 'peer@example.com',
      isImportantContact: false,
      senderType: null,
      needsReply: false,
      isStarred: false,
      hasAttachments: false,
      replyState: 'NONE',
      latestActor: null,
      conversationMessageCount: 1
    })

    expect(model.senderName).toBe('bob@example.com')
    expect(model.avatarText).toBe('B')
    expect(model.identitySignals).toEqual([])
    expect(model.taskSignals).toEqual([])
    expect(model.conversationSignals).toEqual([])
  })

  it('falls back to trimmed peer email when sender identity is blank', () => {
    const model = buildMailListModel({
      ...triageMail,
      senderDisplayName: '   ',
      senderEmail: '   ',
      peerEmail: '  peer@example.com  '
    })

    expect(model.senderName).toBe('peer@example.com')
    expect(model.avatarText).toBe('P')
  })

  it('prefers the counterparty identity for outgoing folders', () => {
    const model = buildMailListModel({
      ...triageMail,
      folderType: 'OUTBOX',
      senderDisplayName: 'Me Myself',
      senderEmail: 'me@example.com',
      peerEmail: '  recipient@example.com  ',
      isImportantContact: true,
      senderType: 'INTERNAL'
    })

    expect(model.senderName).toBe('recipient@example.com')
    expect(model.avatarText).toBe('R')
    expect(model.identitySignals).toEqual([])
    expect(model.taskSignals.map((signal) => signal.key)).toEqual(['needsReply', 'starred'])
    expect(model.conversationSignals).toEqual([
      { key: 'attachments' },
      { key: 'replyState:AWAITING_ME' },
      { key: 'latestActor:OTHER' },
      { key: 'messageCount', value: 3 }
    ])
  })

  it('supports legacy summaries without triage fields', () => {
    const model = buildMailListModel({
      id: 'mail-legacy',
      ownerId: 'user-1',
      senderEmail: null,
      peerEmail: 'legacy@example.com',
      folderType: 'INBOX',
      customFolderId: null,
      customFolderName: null,
      subject: 'Legacy mail',
      preview: 'Legacy preview',
      isRead: true,
      isStarred: false,
      isDraft: false,
      sentAt: '2026-03-13T10:20:30Z',
      labels: []
    })

    expect(model.senderName).toBe('legacy@example.com')
    expect(model.avatarText).toBe('L')
    expect(model.identitySignals).toEqual([])
    expect(model.taskSignals).toEqual([])
    expect(model.conversationSignals).toEqual([])
  })

  it('falls back to avatar placeholder when every sender value is blank', () => {
    const model = buildMailListModel({
      ...triageMail,
      senderDisplayName: null,
      senderEmail: null,
      peerEmail: '   '
    })

    expect(model.senderName).toBe('')
    expect(model.avatarText).toBe('?')
  })
})

describe('MailListSignals component', () => {
  it('localizes grouped triage signal keys with dotted translation paths', async () => {
    const { default: MailListSignals } = await import('../components/business/mail-list/MailListSignals.vue')
    const wrapper = mount(MailListSignals, {
      props: {
        signals: [
          { key: 'senderType:EXTERNAL' },
          { key: 'replyState:AWAITING_ME' },
          { key: 'latestActor:OTHER' },
          { key: 'messageCount', value: 3 }
        ]
      }
    })

    expect(wrapper.text()).toContain('External')
    expect(wrapper.text()).toContain('Awaiting me')
    expect(wrapper.text()).toContain('Last active: other side')
    expect(wrapper.text()).toContain('3 messages')
    expect(wrapper.text()).not.toContain('mailList.signals.senderType:EXTERNAL')
    expect(wrapper.text()).not.toContain('mailList.signals.replyState:AWAITING_ME')
    expect(wrapper.text()).not.toContain('mailList.signals.latestActor:OTHER')
  })
})

describe('MailListQuickActions component', () => {
  it('routes lower-frequency More menu actions through the quick-actions emits', async () => {
    const { default: MailListQuickActions } = await import('../components/business/mail-list/MailListQuickActions.vue')
    const wrapper = mount(MailListQuickActions, {
      props: {
        mail: triageMail,
        subjectText: triageMail.subject
      },
      global: {
        stubs: elementStubs
      }
    })

    expect(wrapper.get('[data-testid="mail-action-more-mail-1"]').text()).toContain('More')

    await getDropdownMenuItem(wrapper, 'Unstar').trigger('click')
    await getDropdownMenuItem(wrapper, 'Snooze 7d').trigger('click')
    await getDropdownMenuItem(wrapper, 'Snooze…').trigger('click')
    await getDropdownMenuItem(wrapper, 'Spam').trigger('click')
    await getDropdownMenuItem(wrapper, 'Trash').trigger('click')

    expect(wrapper.emitted('action')).toEqual([
      ['UNSTAR'],
      ['SNOOZE_7D'],
      ['MOVE_SPAM'],
      ['MOVE_TRASH']
    ])
    expect(wrapper.emitted('customSnooze')).toEqual([[]])
  })

  it('restores unsnooze as the primary row action for snoozed mail', async () => {
    const { default: MailListQuickActions } = await import('../components/business/mail-list/MailListQuickActions.vue')
    const wrapper = mount(MailListQuickActions, {
      props: {
        mail: { ...triageMail, folderType: 'SNOOZED' },
        subjectText: triageMail.subject
      },
      global: {
        stubs: elementStubs
      }
    })

    expect(wrapper.get('[data-testid="mail-action-snooze-mail-1"]').text()).toBe('Unsnooze')

    await wrapper.get('[data-testid="mail-action-snooze-mail-1"]').trigger('click')

    expect(wrapper.emitted('action')).toEqual([['UNSNOOZE']])
  })
})

describe('MailList component', () => {
  it('renders triage rows and emits the visible primary actions', async () => {
    const { default: MailList } = await import('../components/business/MailList.vue')
    const wrapper = mount(MailList, {
      props: {
        title: 'Inbox',
        mails: [triageMail],
        loading: false
      },
      global: {
        stubs: elementStubs
      }
    })

    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('Quarterly update')
    expect(wrapper.text()).toContain('Need your decision')
    expect(wrapper.text()).toContain('Important')
    expect(wrapper.text()).toContain('Needs reply')
    expect(wrapper.text()).toContain('Attachment')

    await wrapper.get('[data-testid="mail-action-read-mail-1"]').trigger('click')
    await wrapper.get('[data-testid="mail-action-archive-mail-1"]').trigger('click')
    await wrapper.get('[data-testid="mail-action-snooze-mail-1"]').trigger('click')

    expect(wrapper.emitted('action')).toEqual([
      ['mail-1', 'MARK_READ'],
      ['mail-1', 'MOVE_ARCHIVE'],
      ['mail-1', 'SNOOZE_24H']
    ])
  })

  it('keeps undo send visible for outbox rows', async () => {
    const { default: MailList } = await import('../components/business/MailList.vue')
    const wrapper = mount(MailList, {
      props: {
        title: 'Outbox',
        mails: [{ ...triageMail, id: 'mail-2', folderType: 'OUTBOX' }],
        loading: false
      },
      global: {
        stubs: elementStubs
      }
    })

    expect(wrapper.find('[data-testid="mail-action-read-mail-2"]').exists()).toBe(false)
    await wrapper.get('[data-testid="mail-action-undo-mail-2"]').trigger('click')
    expect(wrapper.emitted('undo')).toEqual([['mail-2']])
  })

  it('localizes loading and empty states through mail list translation keys', async () => {
    const { default: MailList } = await import('../components/business/MailList.vue')

    const loadingWrapper = mount(MailList, {
      props: {
        title: 'Inbox',
        mails: [],
        loading: true
      },
      global: {
        stubs: elementStubs
      }
    })

    expect(loadingWrapper.text()).toContain('Localized loading')
    expect(loadingWrapper.text()).not.toContain('Loading…')

    const emptyWrapper = mount(MailList, {
      props: {
        title: 'Inbox',
        mails: [],
        loading: false
      },
      global: {
        stubs: elementStubs
      }
    })

    expect(emptyWrapper.text()).toContain('Localized empty')
    expect(emptyWrapper.text()).not.toContain('No mail')
  })
})
