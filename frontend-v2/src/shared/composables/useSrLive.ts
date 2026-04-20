function writeToRegion(regionId: string, message: string) {
  const node = document.getElementById(regionId)

  if (!node) {
    return
  }

  node.textContent = ''
  window.requestAnimationFrame(() => {
    node.textContent = message
  })
}

export function useSrLive() {
  return {
    assertive(message: string) {
      writeToRegion('sr-live-assertive', message)
    },
    polite(message: string) {
      writeToRegion('sr-live-polite', message)
    }
  }
}
