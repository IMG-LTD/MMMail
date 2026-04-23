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

const expectedQuickStartTargetPaths = ['/suite', '/inbox', '/calendar', '/drive']

function extractBalancedSource(content, startIndex, openCharacter, closeCharacter) {
  let depth = 0
  let quote = null
  let escaping = false

  for (let index = startIndex; index < content.length; index += 1) {
    const character = content[index]

    if (quote) {
      if (escaping) {
        escaping = false
        continue
      }

      if (character === '\\') {
        escaping = true
        continue
      }

      if (character === quote) {
        quote = null
      }

      continue
    }

    if (character === '\'' || character === '"' || character === '`') {
      quote = character
      continue
    }

    if (character === openCharacter) {
      depth += 1
      continue
    }

    if (character === closeCharacter) {
      depth -= 1

      if (depth === 0) {
        return content.slice(startIndex, index + 1)
      }
    }
  }

  assert.fail(`Expected balanced ${openCharacter}${closeCharacter} source starting at index ${startIndex}`)
}

function extractNamedExportedArray(content, exportName) {
  const declarationPattern = new RegExp(`export\\s+const\\s+${exportName}\\s*(?::[^=]+)?=\\s*\\[`, 'm')
  const declaration = declarationPattern.exec(content)

  assert.ok(declaration, `Expected onboarding content to export ${exportName}`)

  const arrayStart = declaration.index + declaration[0].lastIndexOf('[')
  return extractBalancedSource(content, arrayStart, '[', ']')
}

function countTopLevelObjects(arraySource) {
  let arrayDepth = 0
  let objectDepth = 0
  let quote = null
  let escaping = false
  let objectCount = 0

  for (const character of arraySource) {
    if (quote) {
      if (escaping) {
        escaping = false
        continue
      }

      if (character === '\\') {
        escaping = true
        continue
      }

      if (character === quote) {
        quote = null
      }

      continue
    }

    if (character === '\'' || character === '"' || character === '`') {
      quote = character
      continue
    }

    if (character === '[') {
      arrayDepth += 1
      continue
    }

    if (character === ']') {
      arrayDepth -= 1
      continue
    }

    if (character === '{') {
      if (arrayDepth === 1 && objectDepth === 0) {
        objectCount += 1
      }

      objectDepth += 1
      continue
    }

    if (character === '}') {
      objectDepth -= 1
    }
  }

  return objectCount
}

function extractTargetPathValues(stepSource) {
  return Array.from(stepSource.matchAll(/\btargetPath\s*:\s*(["'`])([^"'`]+)\1/g), (match) => match[2])
}

function findNextNonWhitespaceIndex(content, startIndex) {
  for (let index = startIndex; index < content.length; index += 1) {
    if (!/\s/.test(content[index])) {
      return index
    }
  }

  return -1
}

function extractStatementSource(content, startIndex) {
  let quote = null
  let escaping = false
  let parenDepth = 0
  let arrayDepth = 0
  let objectDepth = 0

  for (let index = startIndex; index < content.length; index += 1) {
    const character = content[index]

    if (quote) {
      if (escaping) {
        escaping = false
        continue
      }

      if (character === '\\') {
        escaping = true
        continue
      }

      if (character === quote) {
        quote = null
      }

      continue
    }

    if (character === '\'' || character === '"' || character === '`') {
      quote = character
      continue
    }

    if (character === '(') parenDepth += 1
    if (character === ')') parenDepth -= 1
    if (character === '[') arrayDepth += 1
    if (character === ']') arrayDepth -= 1
    if (character === '{') objectDepth += 1
    if (character === '}') objectDepth -= 1

    if (parenDepth === 0 && arrayDepth === 0 && objectDepth === 0 && (character === ';' || character === '\n')) {
      return content.slice(startIndex, index + 1)
    }
  }

  return content.slice(startIndex)
}

function extractIfBlocks(content) {
  const blocks = []
  const ifPattern = /\bif\s*\(/g
  let match

  while ((match = ifPattern.exec(content)) !== null) {
    const conditionStart = content.indexOf('(', match.index)
    const conditionSource = extractBalancedSource(content, conditionStart, '(', ')')
    const bodyStart = findNextNonWhitespaceIndex(content, conditionStart + conditionSource.length)

    if (bodyStart === -1) {
      continue
    }

    const bodySource = content[bodyStart] === '{'
      ? extractBalancedSource(content, bodyStart, '{', '}')
      : extractStatementSource(content, bodyStart)

    blocks.push(`if ${conditionSource} ${bodySource}`)
  }

  return blocks
}

function extractReactiveCallBlocks(content) {
  const blocks = []
  const reactiveCallPattern = /\b(?:watchEffect|onMounted|watch)\s*\(/g
  let match

  while ((match = reactiveCallPattern.exec(content)) !== null) {
    const callStart = content.indexOf('(', match.index)
    blocks.push(extractBalancedSource(content, callStart, '(', ')'))
  }

  return blocks
}

function hasAutoOpenGateContract(block) {
  const onboardingStatePattern = /\bshouldAutoOpen\b/
  const authenticatedStatePattern = /\b(?:isAuthenticated|authenticated|accessToken)\b/i
  const baseLayoutPattern = /(?:(?:route\.meta\??\.layout|layoutName|currentLayout)(?:\.value)?\s*(?:={2,3}|!==?)\s*['"`]base['"`]|['"`]base['"`]\s*(?:={2,3}|!==?)\s*(?:route\.meta\??\.layout|layoutName|currentLayout)(?:\.value)?|\bisBaseLayout(?:\.value)?\b)/
  const openGuidePattern = /\bopenGuide\s*\(\s*\)/
  const gateSignalPattern = /\bif\s*\(|&&|\|\|/

  return onboardingStatePattern.test(block) &&
    authenticatedStatePattern.test(block) &&
    baseLayoutPattern.test(block) &&
    openGuidePattern.test(block) &&
    gateSignalPattern.test(block)
}

function hasOnboardingAutoOpenGate(content) {
  return [...extractIfBlocks(content), ...extractReactiveCallBlocks(content)].some(hasAutoOpenGateContract)
}

test('onboarding content exposes exactly four quick-start steps with target paths', async () => {
  const content = await readContractFile(files.onboardingSteps, 'onboarding steps')
  const quickStartStepsSource = extractNamedExportedArray(content, 'onboardingQuickStartSteps')
  const targetPathValues = extractTargetPathValues(quickStartStepsSource)

  assert.match(content, /onboarding/i)
  assert.match(content, /quick start|getting started|快速开始|入门指南|开始使用/i)
  assert.equal(countTopLevelObjects(quickStartStepsSource), expectedQuickStartTargetPaths.length)
  assert.deepEqual(targetPathValues, expectedQuickStartTargetPaths)
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
  assert.ok(
    hasOnboardingAutoOpenGate(content),
    'Expected one conditional/reactive block to gate openGuide() on shouldAutoOpen, authenticated state, and base layout'
  )
})

test('settings workspace exposes getting-started panel and reopens onboarding guide', async () => {
  const content = await readContractFile(files.settings, 'settings workspace')

  assert.match(content, /getting-started|Getting Started|快速开始|入门指南|开始使用/i)
  assert.match(content, /useOnboardingStore|onboardingStore/)
  assert.match(content, /openGuide\(\)/)
})
