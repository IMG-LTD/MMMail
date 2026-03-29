import { describe, expect, it } from 'vitest'
import {
  resolveDocsPermissionLabelKey,
  resolveDocsScopeLabelKey,
  resolveDocsScopeTagType,
  shouldConfirmDocsUnsavedChange
} from '../utils/docs-presentation'

describe('docs presentation utils', () => {
  it('maps scope and permission labels to stable translation keys', () => {
    expect(resolveDocsScopeLabelKey('OWNED')).toBe('docs.scope.owned')
    expect(resolveDocsScopeLabelKey('SHARED')).toBe('docs.scope.shared')
    expect(resolveDocsScopeTagType('OWNED')).toBe('info')
    expect(resolveDocsScopeTagType('SHARED')).toBe('warning')
    expect(resolveDocsPermissionLabelKey('OWNER')).toBe('docs.permission.owner')
    expect(resolveDocsPermissionLabelKey('EDIT')).toBe('docs.permission.edit')
    expect(resolveDocsPermissionLabelKey('VIEW')).toBe('docs.permission.view')
  })

  it('only prompts for unsaved confirmation when the current note would be abandoned', () => {
    expect(shouldConfirmDocsUnsavedChange(false, 'note-1', 'note-2')).toBe(false)
    expect(shouldConfirmDocsUnsavedChange(true, '', 'note-2')).toBe(false)
    expect(shouldConfirmDocsUnsavedChange(true, 'note-1', 'note-1')).toBe(false)
    expect(shouldConfirmDocsUnsavedChange(true, 'note-1', 'note-2')).toBe(true)
    expect(shouldConfirmDocsUnsavedChange(true, 'note-1')).toBe(true)
  })
})
