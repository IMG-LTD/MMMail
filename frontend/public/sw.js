const CACHE_NAME = 'mmmail-shell-v13'
const APP_SHELL_PATH = '/'
const DEFAULT_NOTIFICATION_TITLE = 'MMMail'
const DEFAULT_NOTIFICATION_BODY = 'You have a new message.'
const DEFAULT_TARGET_PATH = '/inbox'
const ICON_PATH = '/pwa-icon-192.svg'
const STATIC_ASSETS = [
  APP_SHELL_PATH,
  '/manifest.webmanifest',
  ICON_PATH,
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

self.addEventListener('push', (event) => {
  event.waitUntil(handlePushEvent(event))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  event.waitUntil(handleNotificationClick(event))
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

function parsePushPayload(event) {
  if (!event.data) {
    return {}
  }

  try {
    const parsed = event.data.json()
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (error) {
    return {}
  }
}

function buildNotificationTargetPath(payload) {
  if (typeof payload.mailId === 'string' && payload.mailId.trim().length > 0) {
    return `/mail/${payload.mailId.trim()}`
  }

  if (typeof payload.routePath === 'string' && payload.routePath.trim().length > 0) {
    return payload.routePath.trim()
  }

  return DEFAULT_TARGET_PATH
}

async function handlePushEvent(event) {
  const payload = parsePushPayload(event)
  const title = typeof payload.title === 'string' && payload.title.trim()
    ? payload.title.trim()
    : DEFAULT_NOTIFICATION_TITLE
  const body = typeof payload.body === 'string' && payload.body.trim()
    ? payload.body.trim()
    : DEFAULT_NOTIFICATION_BODY
  const targetPath = buildNotificationTargetPath(payload)

  await self.registration.showNotification(title, {
    body,
    icon: ICON_PATH,
    badge: ICON_PATH,
    data: {
      targetPath
    }
  })
}

async function handleNotificationClick(event) {
  const targetPath = event.notification?.data?.targetPath || DEFAULT_TARGET_PATH
  const targetUrl = new URL(targetPath, self.location.origin).href
  const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })

  for (const client of windows) {
    if (!client || client.url.startsWith(self.location.origin) === false) {
      continue
    }

    if ('focus' in client) {
      await client.focus()
    }

    if ('navigate' in client) {
      await client.navigate(targetUrl)
    }
    return
  }

  await self.clients.openWindow(targetUrl)
}
