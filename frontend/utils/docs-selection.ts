export function extractSelectedExcerpt(content: string, selectionStart: number, selectionEnd: number): string {
  if (selectionStart < 0 || selectionEnd <= selectionStart) {
    return ''
  }
  const excerpt = content.slice(selectionStart, selectionEnd).trim()
  if (!excerpt) {
    return ''
  }
  return excerpt.length > 200 ? `${excerpt.slice(0, 197)}...` : excerpt
}
