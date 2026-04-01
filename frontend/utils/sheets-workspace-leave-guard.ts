import type { Ref } from 'vue'
import type { SheetCellEditInput } from '~/types/sheets'

export function hasDirtySheetsWorkspaceEdits(
  dirtyEdits: Map<string, SheetCellEditInput>
): boolean {
  return dirtyEdits.size > 0
}

export function registerSheetsWorkspaceBeforeUnload(options: {
  dirtyEdits: Ref<Map<string, SheetCellEditInput>>
}): () => void {
  if (typeof window === 'undefined') {
    return () => undefined
  }

  const onBeforeUnload = (event: BeforeUnloadEvent): void => {
    if (!hasDirtySheetsWorkspaceEdits(options.dirtyEdits.value)) {
      return
    }
    event.preventDefault()
    event.returnValue = ''
  }

  window.addEventListener('beforeunload', onBeforeUnload)
  return () => window.removeEventListener('beforeunload', onBeforeUnload)
}
