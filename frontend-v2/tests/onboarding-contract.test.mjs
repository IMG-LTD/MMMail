import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = {
  onboardingSteps: new URL('../src/shared/content/onboarding-steps.ts', import.meta.url),
  onboardingStore: new URL('../src/store/modules/onboarding.ts', import.meta.url),
  welcomeModal: new URL('../src/shared/components/WelcomeOnboardingModal.vue', import.meta.url),
  app: new URL('../src/app/App.vue', import.meta.url),
  settings: new URL('../src/views/app/SettingsWorkspaceView.vue', import.meta.url)
}

async function readContractFile(file, label) {
  try {
    return await readFile(file, 'utf8')
  } catch (error) {
    assert.fail(`Expected ${label} contract file to exist at ${file.pathname}: ${error.message}`)
  }
}

test('onboarding content exposes four quick-start steps with target paths', async () => {
  const content = await readContractFile(files.onboardingSteps, 'onboarding steps')

  assert.match(content, /onboarding/i)
  assert.match(content, /quick start|getting started|快速开始|入门指南|开始使用/i)
  assert.match(content, /\/suite/)
  assert.match(content, /\/inbox/)
  assert.match(content, /\/calendar/)
  assert.match(content, /\/drive/)
  assert.match(content, /\[[\s\S]*(\/suite)[\s\S]*(\/inbox)[\s\S]*(\/calendar)[\s\S]*(\/drive)[\s\S]*\]/)
})

test('onboarding store persists local-only state and exposes guide actions', async () => {
  const content = await readContractFile(files.onboardingStore, 'onboarding store')

  assert.match(content, /mmmail\.onboarding\.v1/)
  assert.match(content, /shouldAutoOpen/)
  assert.match(content, /openGuide/)
  assert.match(content, /skipGuide/)
  assert.match(content, /completeGuide/)
  assert.match(content, /localStorage|useStorage|useLocalStorage/)
  assert.doesNotMatch(content, /fetch\(|axios|useApi|apiClient|trpc|graphql/i)
})

test('welcome onboarding modal uses Naive UI steps and supports skip, complete, and navigation', async () => {
  const content = await readContractFile(files.welcomeModal, 'welcome onboarding modal')

  assert.match(content, /NModal|<n-modal/i)
  assert.match(content, /NCard|<n-card/i)
  assert.match(content, /NSteps|<n-steps/i)
  assert.match(content, /NButton|<n-button/i)
  assert.match(content, /skipGuide/)
  assert.match(content, /completeGuide/)
  assert.match(content, /useRouter\(|router\.push/)
  assert.match(content, /router\.push\([^)]*(step\.targetPath|targetPath|path|route)/s)
})

test('app shell auto-opens onboarding for authenticated first-login base-layout sessions', async () => {
  const content = await readContractFile(files.app, 'app shell')

  assert.match(content, /WelcomeOnboardingModal|welcome-onboarding-modal/)
  assert.match(content, /useAuthStore|authStore/)
  assert.match(content, /useOnboardingStore|onboardingStore/)
  assert.match(content, /watch\(/)
  assert.match(content, /shouldAutoOpen/)
  assert.match(content, /openGuide/)
  assert.match(content, /isAuthenticated|authenticated|accessToken/)
})

test('settings workspace exposes getting-started panel and reopens onboarding guide', async () => {
  const content = await readContractFile(files.settings, 'settings workspace')

  assert.match(content, /getting-started|Getting Started|快速开始|入门指南|开始使用/i)
  assert.match(content, /useOnboardingStore|onboardingStore/)
  assert.match(content, /openGuide\(\)/)
})
