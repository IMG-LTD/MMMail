export type MailEasySwitchProvider = 'GOOGLE' | 'OUTLOOK' | 'YAHOO' | 'OTHER'
export type ImportedMailFolder = 'INBOX' | 'ARCHIVE'
export type MailEasySwitchStatus = 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface MailEasySwitchSession {
  id: string
  provider: MailEasySwitchProvider
  sourceEmail: string
  importContacts: boolean
  mergeContactDuplicates: boolean
  importCalendar: boolean
  importMail: boolean
  importedMailFolder: ImportedMailFolder
  status: MailEasySwitchStatus
  contactsCreated: number
  contactsUpdated: number
  contactsSkipped: number
  contactsInvalid: number
  calendarImported: number
  calendarInvalid: number
  mailImported: number
  mailSkipped: number
  mailInvalid: number
  errorMessage: string | null
  createdAt: string
  completedAt: string | null
}

export interface CreateMailEasySwitchSessionRequest {
  provider: MailEasySwitchProvider
  sourceEmail: string
  importContacts?: boolean
  mergeContactDuplicates?: boolean
  contactsCsv?: string
  importCalendar?: boolean
  calendarIcs?: string
  importMail?: boolean
  mailMessages?: string[]
  importedMailFolder?: ImportedMailFolder
}

export interface MailEasySwitchDraft {
  provider: MailEasySwitchProvider
  sourceEmail: string
  importContacts: boolean
  mergeContactDuplicates: boolean
  contactsCsv: string
  importCalendar: boolean
  calendarIcs: string
  importMail: boolean
  mailMessages: string[]
  importedMailFolder: ImportedMailFolder
}
