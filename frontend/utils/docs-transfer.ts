import { downloadTextFile } from './sheets'

export type DocsImportFormat = 'MARKDOWN' | 'TEXT' | 'UNSUPPORTED'
export type DocsExportFormat = 'MARKDOWN' | 'TEXT'

const MARKDOWN_EXTENSIONS = ['MD', 'MARKDOWN']
const TEXT_EXTENSIONS = ['TXT']

export function resolveDocsImportFormat(fileName: string): DocsImportFormat {
  const extension = fileName.split('.').pop()?.trim().toUpperCase() || ''
  if (MARKDOWN_EXTENSIONS.includes(extension)) {
    return 'MARKDOWN'
  }
  if (TEXT_EXTENSIONS.includes(extension)) {
    return 'TEXT'
  }
  return 'UNSUPPORTED'
}

export function buildDocsExportFileName(title: string, format: DocsExportFormat): string {
  const normalizedTitle = title.trim().replace(/[^\w\u4e00-\u9fa5-]+/g, '-').replace(/^-+|-+$/g, '') || 'mmmail-doc'
  return `${normalizedTitle}.${format === 'MARKDOWN' ? 'md' : 'txt'}`
}

export function buildDocsExportContent(title: string, content: string, format: DocsExportFormat): string {
  if (format === 'TEXT') {
    return `${title}\n\n${content}`.trim()
  }
  const normalizedTitle = title.trim() || 'Untitled note'
  const normalizedContent = content.trim()
  return normalizedContent ? `# ${normalizedTitle}\n\n${normalizedContent}` : `# ${normalizedTitle}\n`
}

export function downloadDocsExport(title: string, content: string, format: DocsExportFormat): void {
  const fileName = buildDocsExportFileName(title, format)
  const mimeType = format === 'MARKDOWN' ? 'text/markdown;charset=utf-8' : 'text/plain;charset=utf-8'
  downloadTextFile(buildDocsExportContent(title, content, format), fileName, mimeType)
}

export async function parseDocsImportFile(file: Pick<File, 'name' | 'text'>): Promise<{ title: string; content: string; format: DocsImportFormat }> {
  const format = resolveDocsImportFormat(file.name)
  if (format === 'UNSUPPORTED') {
    throw new Error('UNSUPPORTED_DOCS_IMPORT_FORMAT')
  }
  const raw = await file.text()
  const normalized = raw.replace(/\r\n/g, '\n').trim()
  if (!normalized) {
    throw new Error('EMPTY_DOCS_IMPORT_FILE')
  }
  if (format === 'TEXT') {
    return {
      title: file.name.replace(/\.[^.]+$/, '') || 'Imported note',
      content: normalized,
      format
    }
  }
  const lines = normalized.split('\n')
  const heading = lines[0]?.match(/^#\s+(.+)$/)?.[1]?.trim() || ''
  const content = heading ? lines.slice(1).join('\n').trim() : normalized
  return {
    title: heading || file.name.replace(/\.[^.]+$/, '') || 'Imported note',
    content,
    format
  }
}
