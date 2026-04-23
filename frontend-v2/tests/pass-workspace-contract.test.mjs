import test from 'node:test'
import assert from 'node:assert/strict'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const apiFile = new URL('../src/service/api/pass.ts', import.meta.url)
const viewFile = new URL('../src/views/app/PassSectionView.vue', import.meta.url)
const stateFile = new URL('../src/views/app/pass-section-state.ts', import.meta.url)

async function loadTsModule(fileUrl) {
  const source = await readFile(fileUrl, 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    }
  })

  const module = { exports: {} }
  const sandbox = {
    exports: module.exports,
    module,
    require() {
      throw new Error('Unexpected import')
    }
  }

  vm.runInNewContext(outputText, sandbox, { filename: fileUrl.pathname })
  return module.exports
}

test('pass workspace reads items, mailboxes, and secure links from APIs', async () => {
  const [api, view, state] = await Promise.all([
    readFile(apiFile, 'utf8'),
    readFile(viewFile, 'utf8'),
    readFile(stateFile, 'utf8')
  ])

  assert.match(api, /\/api\/v1\/pass\/items/)
  assert.match(api, /\/api\/v1\/pass\/mailboxes/)
  assert.match(api, /\/api\/v1\/pass\/monitor/)
  assert.match(view, /useAuthStore/)
  assert.match(view, /listPassItems/)
  assert.match(view, /listPassMailboxes/)
  assert.match(view, /readPassMonitor/)
  assert.match(view, /PASS_ITEMS_FETCH_LIMIT/)
  assert.match(view, /cappedStateNotice/)
  assert.match(view, /listPassItems\(requestToken, \{ limit: String\(PASS_ITEMS_FETCH_LIMIT\) \}\)/)
  assert.match(view, /watch\(\s*\(\) => \[route\.fullPath, authStore\.accessToken\]/)
  assert.match(view, /latestPassRequest/)
  assert.match(state, /derivePassSectionState/)
  assert.match(state, /itemCoverageCapped/)
  assert.doesNotMatch(view, /const entries = \[/)
})

test('pass runtime derivation merges monitor issues and exposes capped item coverage', async () => {
  const { PASS_ITEMS_FETCH_LIMIT, createPassMonitorIssueMap, derivePassSectionState } = await loadTsModule(stateFile)

  const items = Array.from({ length: PASS_ITEMS_FETCH_LIMIT }, (_, index) => ({
    id: `item-${index + 1}`,
    title: index === 0 ? 'Shared GitHub' : index === 1 ? 'Alias Login' : `Vault Item ${index + 1}`,
    website: index === 0 ? 'github.com' : null,
    username: index === 0 ? 'octo' : index === 1 ? 'alias@relay.test' : `user-${index + 1}`,
    favorite: index === 0,
    updatedAt: `2026-04-${String((index % 28) + 1).padStart(2, '0')}T08:00:00.000Z`,
    scopeType: index === 0 ? 'SHARED' : 'PERSONAL',
    itemType: index === 1 ? 'ALIAS' : 'LOGIN',
    sharedVaultId: index === 0 ? 'vault-shared' : null,
    secureLinkCount: index < 2 ? index + 1 : 0
  }))

  const mailboxes = [
    {
      id: 'mailbox-default',
      mailboxEmail: 'default@relay.test',
      status: 'VERIFIED',
      defaultMailbox: true,
      primaryMailbox: false,
      createdAt: '2026-04-01T00:00:00.000Z',
      updatedAt: '2026-04-10T00:00:00.000Z',
      verifiedAt: '2026-04-02T00:00:00.000Z'
    },
    {
      id: 'mailbox-primary',
      mailboxEmail: 'primary@relay.test',
      status: 'PENDING',
      defaultMailbox: false,
      primaryMailbox: true,
      createdAt: '2026-04-03T00:00:00.000Z',
      updatedAt: '2026-04-11T00:00:00.000Z',
      verifiedAt: null
    }
  ]

  const monitor = {
    totalItemCount: 480,
    trackedItemCount: 320,
    weakPasswordCount: 5,
    reusedPasswordCount: 2,
    inactiveTwoFactorCount: 3,
    excludedItemCount: 1,
    weakPasswords: [
      {
        id: 'item-1',
        title: 'Shared GitHub',
        website: 'github.com',
        username: 'octo',
        itemType: 'LOGIN',
        scopeType: 'SHARED',
        orgId: null,
        sharedVaultId: 'vault-shared',
        sharedVaultName: 'Shared',
        excluded: false,
        weakPassword: true,
        reusedPassword: false,
        inactiveTwoFactor: false,
        reusedGroupSize: 0,
        updatedAt: '2026-04-20T00:00:00.000Z'
      }
    ],
    reusedPasswords: [
      {
        id: 'item-1',
        title: 'Shared GitHub',
        website: 'github.com',
        username: 'octo',
        itemType: 'LOGIN',
        scopeType: 'SHARED',
        orgId: null,
        sharedVaultId: 'vault-shared',
        sharedVaultName: 'Shared',
        excluded: false,
        weakPassword: false,
        reusedPassword: true,
        inactiveTwoFactor: false,
        reusedGroupSize: 4,
        updatedAt: '2026-04-21T00:00:00.000Z'
      }
    ],
    inactiveTwoFactorItems: [
      {
        id: 'item-2',
        title: 'Alias Login',
        website: null,
        username: 'alias@relay.test',
        itemType: 'ALIAS',
        scopeType: 'PERSONAL',
        orgId: null,
        sharedVaultId: null,
        sharedVaultName: null,
        excluded: false,
        weakPassword: false,
        reusedPassword: false,
        inactiveTwoFactor: true,
        reusedGroupSize: 0,
        updatedAt: '2026-04-22T00:00:00.000Z'
      }
    ],
    excludedItems: []
  }

  const issueMap = createPassMonitorIssueMap(monitor)
  const derived = derivePassSectionState(items, mailboxes, monitor, PASS_ITEMS_FETCH_LIMIT)

  assert.equal(issueMap.size, 2)
  assert.equal(issueMap.get('item-1')?.weakPassword, true)
  assert.equal(issueMap.get('item-1')?.reusedPassword, true)
  assert.equal(issueMap.get('item-1')?.reusedGroupSize, 4)
  assert.equal(derived.itemCoverageCapped, true)
  assert.equal(derived.loadedItemCount, PASS_ITEMS_FETCH_LIMIT)
  assert.equal(derived.sharedItemCount, 1)
  assert.equal(derived.aliasItemCount, 1)
  assert.equal(derived.secureLinkItemCount, 2)
  assert.equal(derived.totalSecureLinkCount, 3)
  assert.equal(derived.sharedSecureLinkCount, 1)
  assert.equal(derived.sharedVaultCount, 1)
  assert.equal(derived.policyItemCount, 2)
  assert.equal(derived.mailboxCount, 2)
  assert.equal(derived.verifiedMailboxCount, 1)
  assert.equal(derived.defaultMailboxEmail, 'default@relay.test')
  assert.equal(derived.primaryMailboxEmail, 'primary@relay.test')
  assert.equal(derived.trackedItemCount, 320)
})
