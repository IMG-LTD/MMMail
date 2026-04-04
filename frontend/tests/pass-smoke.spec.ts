import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import {
  addItemShareMock,
  clipboardWriteTextMock,
  findButton,
  listOrganizationsMock,
  loadIncomingShareDetailMock,
  loadIncomingSharesMock,
  mountPassMonitorPage,
  mountPassPage,
  mountPublicSharePage,
  navigateToMock,
  passApiMock,
  resetPassApiMocks,
  resetSharingState,
  setupPassPageGlobals,
  windowOpenMock,
} from './support/pass-page-fixtures'

beforeAll(() => {
  setupPassPageGlobals()
})

beforeEach(() => {
  vi.clearAllMocks()
  resetSharingState()
  resetPassApiMocks()
})

describe('pass smoke', () => {
  it('loads personal workspace and wires mailbox and alias actions', async () => {
    const wrapper = await mountPassPage()
    await flushPromises()

    expect(listOrganizationsMock).toHaveBeenCalledTimes(1)
    expect(passApiMock.listItems).toHaveBeenCalledTimes(1)
    expect(passApiMock.listAliases).toHaveBeenCalledTimes(1)
    expect(passApiMock.listMailboxes).toHaveBeenCalledTimes(1)
    expect(passApiMock.getItem).toHaveBeenCalledWith('item-1')

    await findButton(wrapper, 'New Item').trigger('click')
    await flushPromises()
    expect(passApiMock.createItem).toHaveBeenCalledTimes(1)

    wrapper.findComponent({ name: 'PassMailboxPanel' }).vm.$emit('create', { mailboxEmail: 'relay@mmmail.local' })
    wrapper.findComponent({ name: 'PassMailboxPanel' }).vm.$emit('verify', 'mailbox-2', { verificationCode: '123456' })
    wrapper.findComponent({ name: 'PassMailboxPanel' }).vm.$emit('set-default', 'mailbox-2')
    wrapper.findComponent({ name: 'PassAliasCenter' }).vm.$emit('create', { title: 'Relay', note: '', forwardToEmails: ['relay@mmmail.local'], prefix: 'relay' })
    wrapper.findComponent({ name: 'PassAliasCenter' }).vm.$emit('disable', 'alias-1')
    wrapper.findComponent({ name: 'PassAliasCenter' }).vm.$emit('enable', 'alias-1')
    wrapper.findComponent({ name: 'PassAliasContactsPanel' }).vm.$emit('create', { targetEmail: 'new@example.com', displayName: 'New', note: '' })
    wrapper.findComponent({ name: 'PassAliasContactsPanel' }).vm.$emit('compose', 'reply@reply.passmail.mmmail.local')
    wrapper.findComponent({ name: 'PassAliasContactsPanel' }).vm.$emit('copy', 'reply@reply.passmail.mmmail.local')
    await flushPromises()

    expect(passApiMock.createMailbox).toHaveBeenCalledWith({ mailboxEmail: 'relay@mmmail.local' })
    expect(passApiMock.verifyMailbox).toHaveBeenCalledWith('mailbox-2', { verificationCode: '123456' })
    expect(passApiMock.setDefaultMailbox).toHaveBeenCalledWith('mailbox-2')
    expect(passApiMock.createAlias).toHaveBeenCalledWith({ title: 'Relay', note: '', forwardToEmails: ['relay@mmmail.local'], prefix: 'relay' })
    expect(passApiMock.disableAlias).toHaveBeenCalledWith('alias-1')
    expect(passApiMock.enableAlias).toHaveBeenCalledWith('alias-1')
    expect(passApiMock.createAliasContact).toHaveBeenCalledWith('alias-1', { targetEmail: 'new@example.com', displayName: 'New', note: '' })
    expect(navigateToMock).toHaveBeenCalledTimes(1)
    expect(clipboardWriteTextMock).toHaveBeenCalledWith('reply@reply.passmail.mmmail.local')
  })

  it('switches into shared workspace and wires secure link and sharing actions', async () => {
    const wrapper = await mountPassPage()
    await flushPromises()

    wrapper.findComponent({ name: 'PassWorkspaceHero' }).vm.$emit('update:workspace-mode', 'SHARED')
    await flushPromises()

    expect(passApiMock.getBusinessOverview).toHaveBeenCalledWith('org-1')
    expect(passApiMock.getBusinessPolicy).toHaveBeenCalledWith('org-1')
    expect(passApiMock.listSharedVaults).toHaveBeenCalledWith('org-1', '')
    expect(passApiMock.listSharedVaultMembers).toHaveBeenCalledWith('org-1', 'vault-1')
    expect(passApiMock.listSharedItems).toHaveBeenCalledWith('org-1', 'vault-1', '', false, 200, undefined)
    expect(passApiMock.getSharedItem).toHaveBeenCalledWith('org-1', 'vault-1', 'shared-item-1')

    await findButton(wrapper, 'New Item').trigger('click')
    await flushPromises()
    expect(passApiMock.createSharedItem).toHaveBeenCalledTimes(1)

    wrapper.findComponent({ name: 'PassItemSharePanel' }).vm.$emit('update:draft-email', 'auditor@mmmail.local')
    wrapper.findComponent({ name: 'PassItemSharePanel' }).vm.$emit('create')
    wrapper.findComponent({ name: 'PassIncomingItemSharesPanel' }).vm.$emit('refresh')
    wrapper.findComponent({ name: 'PassIncomingItemSharesPanel' }).vm.$emit('select', 'shared-item-1')
    wrapper.findComponent({ name: 'PassSecureLinksDashboard' }).vm.$emit('copy', 'https://mmmail.local/share/pass/public-token')
    wrapper.findComponent({ name: 'PassSecureLinksDashboard' }).vm.$emit('open', 'https://mmmail.local/share/pass/public-token')
    wrapper.findComponent({ name: 'PassSecureLinksDashboard' }).vm.$emit('focus-item', { itemId: 'shared-item-1', sharedVaultId: 'vault-1' })
    wrapper.findComponent({ name: 'PassSecureLinksDashboard' }).vm.$emit('revoke', 'link-1')
    await flushPromises()

    expect(addItemShareMock).toHaveBeenCalledWith('org-1', 'shared-item-1', 'auditor@mmmail.local')
    expect(loadIncomingSharesMock).toHaveBeenCalled()
    expect(loadIncomingShareDetailMock).toHaveBeenCalledWith('org-1', 'shared-item-1')
    expect(clipboardWriteTextMock).toHaveBeenCalledWith('https://mmmail.local/share/pass/public-token')
    expect(windowOpenMock).toHaveBeenCalledWith('https://mmmail.local/share/pass/public-token', '_blank', 'noopener,noreferrer')
    expect(passApiMock.revokeSecureLink).toHaveBeenCalledWith('org-1', 'link-1')
  })

  it('loads public secure links and supports copy and open actions', async () => {
    const wrapper = await mountPublicSharePage()
    await flushPromises()

    expect(passApiMock.getPublicSecureLink).toHaveBeenCalledWith('public-token')

    await findButton(wrapper, 'pass.publicShare.actions.copyUsername').trigger('click')
    await findButton(wrapper, 'pass.publicShare.actions.openWebsite').trigger('click')
    await flushPromises()

    expect(clipboardWriteTextMock).toHaveBeenCalledWith('shared@example.com')
    expect(windowOpenMock).toHaveBeenCalledWith('https://shared.example.com', '_blank', 'noopener,noreferrer')
  })

  it('loads pass monitor and wires shared-scope exclusion and two-factor actions', async () => {
    const wrapper = await mountPassMonitorPage()
    await flushPromises()

    expect(passApiMock.getPersonalMonitor).toHaveBeenCalledTimes(1)

    wrapper.findComponent({ name: 'PassMonitorHero' }).vm.$emit('update:workspace-mode', 'SHARED')
    await flushPromises()
    expect(passApiMock.getSharedMonitor).toHaveBeenCalledWith('org-1')

    const section = wrapper.findComponent({ name: 'PassMonitorSectionPanel' })
    section.vm.$emit('toggle', {
      id: 'shared-item-1',
      orgId: 'org-1',
      scopeType: 'SHARED',
      excluded: false,
      canToggleExclusion: true,
      canManageTwoFactor: true,
      inactiveTwoFactor: true,
      twoFactor: { enabled: false }
    })
    section.vm.$emit('manage-two-factor', {
      id: 'shared-item-1',
      orgId: 'org-1',
      scopeType: 'SHARED',
      title: 'Shared Login',
      website: 'https://shared.example.com',
      username: 'shared@example.com',
      itemType: 'LOGIN',
      updatedAt: '2026-04-02T10:00:00',
      excluded: false,
      canToggleExclusion: true,
      canManageTwoFactor: true,
      inactiveTwoFactor: true,
      twoFactor: { enabled: false }
    })
    await flushPromises()

    const dialog = wrapper.findComponent({ name: 'PassMonitorTwoFactorDialog' })
    dialog.vm.$emit('save', {
      issuer: 'MMMail',
      accountName: 'shared@example.com',
      secretCiphertext: 'ciphertext',
      algorithm: 'SHA1',
      digits: 6,
      periodSeconds: 30
    })
    dialog.vm.$emit('generate')
    await flushPromises()

    expect(passApiMock.excludeSharedMonitorItem).toHaveBeenCalledWith('org-1', 'shared-item-1')
    expect(passApiMock.upsertSharedItemTwoFactor).toHaveBeenCalledWith('org-1', 'shared-item-1', {
      issuer: 'MMMail',
      accountName: 'shared@example.com',
      secretCiphertext: 'ciphertext',
      algorithm: 'SHA1',
      digits: 6,
      periodSeconds: 30
    })
    expect(passApiMock.generateSharedItemTwoFactorCode).toHaveBeenCalledWith('org-1', 'shared-item-1')
  })
})
