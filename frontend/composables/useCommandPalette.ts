const COMMAND_PALETTE_STATE_KEY = 'command-palette-open'

export function useCommandPalette() {
  const isOpen = useState<boolean>(COMMAND_PALETTE_STATE_KEY, () => false)

  function openPalette(): void {
    isOpen.value = true
  }

  function closePalette(): void {
    isOpen.value = false
  }

  function togglePalette(): void {
    isOpen.value = !isOpen.value
  }

  return {
    isOpen,
    openPalette,
    closePalette,
    togglePalette
  }
}
