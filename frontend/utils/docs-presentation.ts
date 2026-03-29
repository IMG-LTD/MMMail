import type { DocsPermission, DocsNoteSummary } from '~/types/api'

export function resolveDocsScopeLabelKey(scope: DocsNoteSummary['scope']): string {
  return scope === 'SHARED' ? 'docs.scope.shared' : 'docs.scope.owned'
}

export function resolveDocsScopeTagType(scope: DocsNoteSummary['scope']): 'warning' | 'info' {
  return scope === 'SHARED' ? 'warning' : 'info'
}

export function resolveDocsPermissionLabelKey(permission: DocsPermission): string {
  if (permission === 'OWNER') {
    return 'docs.permission.owner'
  }
  if (permission === 'EDIT') {
    return 'docs.permission.edit'
  }
  return 'docs.permission.view'
}

export function shouldConfirmDocsUnsavedChange(hasUnsavedChanges: boolean, currentNoteId: string, targetNoteId?: string | null): boolean {
  if (!hasUnsavedChanges || !currentNoteId) {
    return false
  }
  if (typeof targetNoteId === 'undefined') {
    return true
  }
  return targetNoteId !== currentNoteId
}
