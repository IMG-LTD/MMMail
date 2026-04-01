import type { Ref } from 'vue'

export function shouldBlockDocsBeforeUnload(hasUnsavedChanges: boolean): boolean {
  return hasUnsavedChanges
}

export function applyDocsBeforeUnloadGuard(
  event: BeforeUnloadEvent,
  hasUnsavedChanges: boolean
): void {
  if (!shouldBlockDocsBeforeUnload(hasUnsavedChanges)) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

export function registerDocsBeforeUnloadGuard(options: {
  hasUnsavedChanges: Ref<boolean>
}): () => void {
  if (typeof window === 'undefined') {
    return () => undefined
  }

  const onBeforeUnload = (event: BeforeUnloadEvent): void => {
    applyDocsBeforeUnloadGuard(event, options.hasUnsavedChanges.value)
  }

  window.addEventListener('beforeunload', onBeforeUnload)
  return () => window.removeEventListener('beforeunload', onBeforeUnload)
}

export function createDocsRouteLeaveGuard(
  confirmDiscardUnsavedChanges: () => Promise<boolean>
): () => Promise<boolean> {
  return () => confirmDiscardUnsavedChanges()
}
