const CACHE_NAME = 'mmmail-shell-v12'
const APP_SHELL_PATH = '/'
const STATIC_ASSETS = [
  APP_SHELL_PATH,
  '/manifest.webmanifest',
  '/pwa-icon-192.svg',
  '/pwa-icon-512.svg'
]

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(STATIC_ASSETS)).then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys
        .filter((key) => key !== CACHE_NAME)
        .map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', (event) => {
  const { request } = event
  if (request.method !== 'GET') {
    return
  }

  if (request.mode === 'navigate') {
    event.respondWith(handleNavigationRequest(request))
    return
  }

  if (shouldServeFromCache(request)) {
    event.respondWith(handleStaticRequest(request))
  }
})

function handleNavigationRequest(request) {
  return fetch(request).catch(() => caches.match(APP_SHELL_PATH))
}

function handleStaticRequest(request) {
  return caches.match(request).then((cachedResponse) => {
    if (cachedResponse) {
      return cachedResponse
    }

    return fetch(request).then((networkResponse) => {
      const responseClone = networkResponse.clone()
      caches.open(CACHE_NAME).then((cache) => cache.put(request, responseClone))
      return networkResponse
    })
  })
}

function shouldServeFromCache(request) {
  const requestUrl = new URL(request.url)
  if (requestUrl.origin !== self.location.origin) {
    return false
  }

  return requestUrl.pathname.endsWith('.svg')
    || requestUrl.pathname.endsWith('.png')
    || requestUrl.pathname.endsWith('.webmanifest')
}
