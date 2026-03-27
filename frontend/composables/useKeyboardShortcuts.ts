import { onBeforeUnmount, onMounted } from 'vue'
import { useCommandPalette } from '~/composables/useCommandPalette'

function shouldIgnoreShortcut(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false
  }
  const tag = target.tagName.toLowerCase()
  return tag === 'input' || tag === 'textarea' || target.isContentEditable
}

export function useKeyboardShortcuts() {
  const { isOpen, togglePalette } = useCommandPalette()

  const handler = (event: KeyboardEvent) => {
    if (shouldIgnoreShortcut(event.target)) {
      return
    }

    const lowerKey = event.key.toLowerCase()
    const isCommandCenterShortcut = (event.ctrlKey || event.metaKey) && lowerKey === 'k'
    if (isCommandCenterShortcut) {
      event.preventDefault()
      togglePalette()
      return
    }

    if (isOpen.value) {
      return
    }

    if (event.key === 'c' || event.key === 'C') {
      event.preventDefault()
      void navigateTo('/compose')
      return
    }

    if (event.key === '/') {
      event.preventDefault()
      void navigateTo('/search?focus=1')
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', handler)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handler)
  })
}
