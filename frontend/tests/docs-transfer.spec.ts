import { describe, expect, it, vi } from 'vitest'
import {
  buildDocsExportContent,
  buildDocsExportFileName,
  parseDocsImportFile,
  resolveDocsImportFormat
} from '../utils/docs-transfer'

describe('docs transfer utilities', () => {
  it('resolves supported import formats', () => {
    expect(resolveDocsImportFormat('plan.md')).toBe('MARKDOWN')
    expect(resolveDocsImportFormat('plan.markdown')).toBe('MARKDOWN')
    expect(resolveDocsImportFormat('plan.txt')).toBe('TEXT')
    expect(resolveDocsImportFormat('plan.docx')).toBe('UNSUPPORTED')
  })

  it('builds export filenames and markdown content', () => {
    expect(buildDocsExportFileName('Q2 Plan', 'MARKDOWN')).toBe('Q2-Plan.md')
    expect(buildDocsExportFileName('  ', 'TEXT')).toBe('mmmail-doc.txt')
    expect(buildDocsExportContent('Q2 Plan', 'Line 1', 'MARKDOWN')).toBe('# Q2 Plan\n\nLine 1')
    expect(buildDocsExportContent('Q2 Plan', 'Line 1', 'TEXT')).toBe('Q2 Plan\n\nLine 1')
  })

  it('parses markdown headings and plain text imports', async () => {
    const markdownFile = {
      name: 'handoff.md',
      text: vi.fn().mockResolvedValue('# Handoff\n\nFirst line\nSecond line')
    }
    const textFile = {
      name: 'notes.txt',
      text: vi.fn().mockResolvedValue('First line\nSecond line')
    }

    await expect(parseDocsImportFile(markdownFile)).resolves.toEqual({
      title: 'Handoff',
      content: 'First line\nSecond line',
      format: 'MARKDOWN'
    })
    await expect(parseDocsImportFile(textFile)).resolves.toEqual({
      title: 'notes',
      content: 'First line\nSecond line',
      format: 'TEXT'
    })
  })

  it('rejects unsupported or empty import files', async () => {
    await expect(parseDocsImportFile({
      name: 'handoff.docx',
      text: vi.fn().mockResolvedValue('content')
    })).rejects.toThrow('UNSUPPORTED_DOCS_IMPORT_FORMAT')

    await expect(parseDocsImportFile({
      name: 'handoff.md',
      text: vi.fn().mockResolvedValue('   ')
    })).rejects.toThrow('EMPTY_DOCS_IMPORT_FILE')
  })
})
