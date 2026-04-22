import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const apiFile = new URL('../src/service/api/mail.ts', import.meta.url)
const viewFile = new URL('../src/views/app/MailSurfaceView.vue', import.meta.url)

test('mail workspace reads folder, detail, sender, and trust state from APIs', async () => {
  const [api, view] = await Promise.all([
    readFile(apiFile, 'utf8'),
    readFile(viewFile, 'utf8')
  ])

  assert.match(api, /\/api\/v1\/mails\/inbox/)
  assert.match(api, /\/api\/v1\/mails\/search/)
  assert.match(api, /\/api\/v1\/mails\/identities/)
  assert.match(api, /\/api\/v1\/mails\/e2ee-recipient-status/)
  assert.match(api, /\/api\/v1\/mails\/send/)

  assert.match(view, /useAuthStore/)
  assert.match(view, /listMailFolder/)
  assert.match(view, /readMailDetail/)
  assert.match(view, /listSenderIdentities/)
  assert.match(view, /readRecipientTrustState/)
  assert.match(view, /watch\(\s*\(\) => \[route\.fullPath, authStore\.accessToken\]/)
  assert.doesNotMatch(view, /const messages = \[/)
})
