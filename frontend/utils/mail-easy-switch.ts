import type {
  CreateMailEasySwitchSessionRequest,
  ImportedMailFolder,
  MailEasySwitchDraft,
  MailEasySwitchSession,
  MailEasySwitchStatus
} from '~/types/mail-easy-switch'

const DEFAULT_PROVIDER = 'GOOGLE'
const DEFAULT_IMPORTED_MAIL_FOLDER: ImportedMailFolder = 'ARCHIVE'

export function createMailEasySwitchDraft(): MailEasySwitchDraft {
  return {
    provider: DEFAULT_PROVIDER,
    sourceEmail: '',
    importContacts: true,
    mergeContactDuplicates: true,
    contactsCsv: '',
    importCalendar: false,
    calendarIcs: '',
    importMail: false,
    mailMessages: [],
    importedMailFolder: DEFAULT_IMPORTED_MAIL_FOLDER
  }
}

export function appendMailEasySwitchMessage(existing: string[], content: string): string[] {
  const normalized = content.trim()
  if (!normalized) {
    return existing
  }
  const next = new Set(existing)
  next.add(normalized)
  return [...next]
}

export function buildMailEasySwitchPayload(draft: MailEasySwitchDraft): CreateMailEasySwitchSessionRequest {
  const sourceEmail = draft.sourceEmail.trim()
  const contactsCsv = draft.contactsCsv.trim()
  const calendarIcs = draft.calendarIcs.trim()
  const mailMessages = draft.mailMessages.map((item) => item.trim()).filter(Boolean)

  if (!sourceEmail) {
    throw new Error('sourceEmail')
  }
  if (!draft.importContacts && !draft.importCalendar && !draft.importMail) {
    throw new Error('importType')
  }
  if (draft.importContacts && !contactsCsv) {
    throw new Error('contactsCsv')
  }
  if (draft.importCalendar && !calendarIcs) {
    throw new Error('calendarIcs')
  }
  if (draft.importMail && mailMessages.length === 0) {
    throw new Error('mailMessages')
  }

  return {
    provider: draft.provider,
    sourceEmail,
    importContacts: draft.importContacts || undefined,
    mergeContactDuplicates: draft.importContacts ? draft.mergeContactDuplicates : undefined,
    contactsCsv: draft.importContacts ? contactsCsv : undefined,
    importCalendar: draft.importCalendar || undefined,
    calendarIcs: draft.importCalendar ? calendarIcs : undefined,
    importMail: draft.importMail || undefined,
    mailMessages: draft.importMail ? mailMessages : undefined,
    importedMailFolder: draft.importMail ? draft.importedMailFolder : undefined
  }
}

export function resolveMailEasySwitchStatusType(status: MailEasySwitchStatus): 'info' | 'success' | 'danger' {
  if (status === 'COMPLETED') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

export function resolveMailEasySwitchMetricTotal(session: MailEasySwitchSession): number {
  return session.contactsCreated
    + session.contactsUpdated
    + session.calendarImported
    + session.mailImported
}
