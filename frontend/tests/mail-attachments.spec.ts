import { describe, expect, it } from 'vitest'
import type { MailAttachment } from '../types/api'
import {
  buildMailAttachmentFailureId,
  formatMailAttachmentSize,
  upsertMailAttachment,
  validateMailAttachmentFile
} from '../utils/mail-attachments'

describe('mail attachment utils', () => {
  it('validates size and blocked executable extension', () => {
    expect(() => validateMailAttachmentFile({ name: 'report.pdf', size: 1024 })).not.toThrow()
    expect(() => validateMailAttachmentFile({ name: 'danger.exe', size: 1024 })).toThrowError('Attachment type is not allowed')
    expect(() => validateMailAttachmentFile({ name: 'large.pdf', size: 25 * 1024 * 1024 })).toThrowError('Attachment exceeds 20MB limit')
  })

  it('formats file sizes and failure ids deterministically', () => {
    expect(formatMailAttachmentSize(512)).toBe('512 B')
    expect(formatMailAttachmentSize(2048)).toBe('2 KB')
    expect(formatMailAttachmentSize(3 * 1024 * 1024)).toBe('3.0 MB')
    expect(buildMailAttachmentFailureId({ name: 'report.pdf', size: 2048 })).toBe('report.pdf:2048')
  })

  it('upserts attachment list by id', () => {
    const initial: MailAttachment[] = [{
      id: '1',
      mailId: '9',
      fileName: 'report.pdf',
      contentType: 'application/pdf',
      fileSize: 512
    }]

    expect(upsertMailAttachment(initial, {
      id: '1',
      mailId: '9',
      fileName: 'report-v2.pdf',
      contentType: 'application/pdf',
      fileSize: 768
    })).toEqual([{
      id: '1',
      mailId: '9',
      fileName: 'report-v2.pdf',
      contentType: 'application/pdf',
      fileSize: 768
    }])
  })
})
