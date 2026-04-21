import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  app: new URL('../src/app/App.vue', import.meta.url),
  globalCss: new URL('../src/styles/global.css', import.meta.url),
  topBar: new URL('../src/layouts/modules/ShellTopBar.vue', import.meta.url),
  compactHeader: new URL('../src/shared/components/CompactPageHeader.vue', import.meta.url),
  srLive: new URL('../src/shared/composables/useSrLive.ts', import.meta.url),
  docs: new URL('../src/views/app/DocsWorkspaceView.vue', import.meta.url),
  labs: new URL('../src/views/app/LabsOverviewView.vue', import.meta.url),
  notifications: new URL('../src/views/app/NotificationsView.vue', import.meta.url)
}

test('shell accessibility and maturity infrastructure remain wired', async () => {
  const [app, globalCss, topBar, compactHeader, srLive, docs, labs, notifications] = await Promise.all(
    Object.values(files).map(file => readFile(file, 'utf8'))
  )

  assert.match(app, /NNotificationProvider/)
  assert.match(app, /id="sr-live-polite"/)
  assert.match(app, /id="sr-live-assertive"/)

  assert.match(globalCss, /prefers-reduced-motion: reduce/)
  assert.match(globalCss, /prefers-contrast: more/)
  assert.match(globalCss, /:focus-visible/)
  assert.match(globalCss, /\.sr-only/)
  assert.match(srLive, /sr-live-polite/)
  assert.match(srLive, /sr-live-assertive/)

  assert.match(topBar, /Notifications/)
  assert.match(topBar, /currentSurface/)
  assert.match(compactHeader, /MaturityBadge/)

  assert.match(docs, /badge-tone="beta"/)
  assert.match(labs, /badge-tone="preview"/)
  assert.match(notifications, /badge-tone="preview"/)
})
