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

  assert.match(api, /\/api\/v2\/mail\/messages/)
  assert.match(api, /\/api\/v2\/mail\/threads\/\$\{threadId\}/)
  assert.match(api, /\/api\/v2\/mail\/contacts/)
  assert.match(api, /\/api\/v2\/mail\/send/)
  assert.doesNotMatch(api, /\/api\/v1\/mails/)

  assert.match(view, /useAuthStore/)
  assert.match(view, /listMailFolder/)
  assert.match(view, /readMailDetail/)
  assert.match(view, /listSenderIdentities/)
  assert.match(view, /readRecipientTrustState/)
  assert.match(view, /Promise\.all\(\s*\[/)
  assert.match(view, /watch\(\s*\(\) => \[route\.fullPath, authStore\.accessToken\]/)
  assert.match(view, /watch\(\s*\(\) => \[composeForm\.value\.toEmail, composeForm\.value\.fromEmail, authStore\.accessToken\]/)
  assert.match(view, /latestWorkspaceRequest/)
  assert.match(view, /latestRecipientTrustRequest/)
  assert.match(view, /isEmailLike/)
  assert.doesNotMatch(view, /const messages = \[/)
})
