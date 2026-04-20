import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const contentFile = new URL('../src/shared/content/route-surfaces.ts', import.meta.url)

const onboardingKeys = [
  'invitation-landing',
  'create-password',
  'inbox-progress',
  'first-compose-e2ee',
  'recovery-inline-card',
  'recovery-drawer',
  'cross-device-validation',
  'retention-banners',
  'task-drawer'
]

const failureModeKeys = ['f01', 'f03', 'f05', 'f07', 'f09', 'f12', 'f14', 'f16', 'f17', 'f21']
const mobileNavKeys = ['mail', 'calendar', 'drive', 'pass', 'more']

test('onboarding, failure-mode, and mobile-nav contracts remain complete', async () => {
  const content = await readFile(contentFile, 'utf8')

  for (const key of onboardingKeys) {
    assert.match(content, new RegExp(`key:\\s*'${key}'`))
  }

  for (const key of failureModeKeys) {
    assert.match(content, new RegExp(`key:\\s*'${key}'`))
  }

  for (const key of mobileNavKeys) {
    assert.match(content, new RegExp(`key:\\s*'${key}'`))
  }
})
