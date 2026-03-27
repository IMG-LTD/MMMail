<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import OrganizationsDomainsPanel from '~/components/organizations/OrganizationsDomainsPanel.vue'
import PassAliasCenter from '~/components/pass/PassAliasCenter.vue'
import PassAliasContactsPanel from '~/components/pass/PassAliasContactsPanel.vue'
import PassMailboxPanel from '~/components/pass/PassMailboxPanel.vue'
import SimpleLoginKnowledgeRail from '~/components/simplelogin/SimpleLoginKnowledgeRail.vue'
import SimpleLoginRelayControlsPanel from '~/components/simplelogin/SimpleLoginRelayControlsPanel.vue'
import SimpleLoginWorkspaceHero from '~/components/simplelogin/SimpleLoginWorkspaceHero.vue'
import { useI18n } from '~/composables/useI18n'
import { useOrganizationApi } from '~/composables/useOrganizationApi'
import { usePassApi } from '~/composables/usePassApi'
import { useSimpleLoginApi } from '~/composables/useSimpleLoginApi'
import type { OrgWorkspace } from '~/types/api'
import type { OrgCustomDomain } from '~/types/organization-admin'
import type {
  CreatePassAliasContactRequest,
  CreatePassMailboxRequest,
  CreatePassMailAliasRequest,
  PassAliasContact,
  PassMailbox,
  PassMailAlias,
  UpdatePassAliasContactRequest,
  UpdatePassMailAliasRequest,
  VerifyPassMailboxRequest
} from '~/types/pass-business'
import type { SimpleLoginOverview } from '~/types/simplelogin'
import { isOrganizationManager } from '~/utils/organization-admin'
import {
  resolvePreferredSimpleLoginAliasId,
  resolvePreferredSimpleLoginOrgId
} from '~/utils/simplelogin'

definePageMeta({
  layout: 'default'
})

const route = useRoute()
const router = useRouter()

const organizationApi = useOrganizationApi()
const passApi = usePassApi()
const simpleLoginApi = useSimpleLoginApi()
const { t } = useI18n()

const organizations = ref<OrgWorkspace[]>([])
const selectedOrgId = ref('')
const overview = ref<SimpleLoginOverview | null>(null)
const aliases = ref<PassMailAlias[]>([])
const selectedAliasId = ref('')
const aliasContacts = ref<PassAliasContact[]>([])
const mailboxes = ref<PassMailbox[]>([])
const domains = ref<OrgCustomDomain[]>([])

const loading = reactive({
  page: false,
  aliases: false,
  aliasContacts: false,
  mailboxes: false,
  domains: false,
  refresh: false,
  aliasMutationId: '',
  aliasContactMutationId: '',
  mailboxMutationId: '',
  domainMutationId: ''
})

const createDomainForm = reactive({
  domain: ''
})

const selectedOrg = computed(() => organizations.value.find((item) => item.id === selectedOrgId.value) || null)
const canManageDomains = computed(() => isOrganizationManager(selectedOrg.value?.role))
const selectedAlias = computed(() => aliases.value.find((item) => item.id === selectedAliasId.value) || null)
const forwardTargetOptions = computed(() => mailboxes.value
  .filter((mailbox) => mailbox.status === 'VERIFIED')
  .map((mailbox) => ({
    label: [
      mailbox.mailboxEmail,
      mailbox.defaultMailbox ? t('simplelogin.relay.tag.default') : t('simplelogin.relay.tag.verified'),
      mailbox.primaryMailbox ? t('simplelogin.relay.tag.primary') : ''
    ].filter(Boolean).join(' · '),
    value: mailbox.mailboxEmail
  })))

const routeOrgId = computed(() => typeof route.query.orgId === 'string' ? route.query.orgId : null)
const routeAliasId = computed(() => typeof route.query.aliasId === 'string' ? route.query.aliasId : null)

useHead(() => ({
  title: t('page.simplelogin.title')
}))

onMounted(() => {
  void bootstrapPage()
})

watch(routeOrgId, (nextOrgId) => {
  if ((nextOrgId || '') === selectedOrgId.value) {
    return
  }
  selectedOrgId.value = resolvePreferredSimpleLoginOrgId(organizations.value, nextOrgId)
  void refreshOverviewAndDomains()
})

watch(routeAliasId, (nextAliasId) => {
  if ((nextAliasId || '') === selectedAliasId.value) {
    return
  }
  selectedAliasId.value = resolvePreferredSimpleLoginAliasId(aliases.value, nextAliasId)
  void loadAliasContacts(selectedAliasId.value)
})

async function bootstrapPage(): Promise<void> {
  loading.page = true
  try {
    await loadOrganizations()
    selectedOrgId.value = resolvePreferredSimpleLoginOrgId(organizations.value, routeOrgId.value)
    await Promise.all([
      loadOverview(),
      loadMailboxes(),
      loadAliases(routeAliasId.value),
      loadDomains()
    ])
    await syncRoute()
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.loadWorkspaceFailed')))
  } finally {
    loading.page = false
  }
}

async function loadOrganizations(): Promise<void> {
  organizations.value = await organizationApi.listOrganizations()
}

async function loadOverview(): Promise<void> {
  overview.value = await simpleLoginApi.getOverview(selectedOrgId.value || undefined)
}

async function loadMailboxes(): Promise<void> {
  loading.mailboxes = true
  try {
    mailboxes.value = await passApi.listMailboxes()
  } finally {
    loading.mailboxes = false
  }
}

async function loadAliases(preferredAliasId: string | null = null): Promise<void> {
  loading.aliases = true
  try {
    aliases.value = await passApi.listAliases()
    selectedAliasId.value = resolvePreferredSimpleLoginAliasId(aliases.value, preferredAliasId)
    await loadAliasContacts(selectedAliasId.value)
  } finally {
    loading.aliases = false
  }
}

async function loadAliasContacts(aliasId: string): Promise<void> {
  if (!aliasId) {
    aliasContacts.value = []
    return
  }
  loading.aliasContacts = true
  try {
    aliasContacts.value = await passApi.listAliasContacts(aliasId)
  } finally {
    loading.aliasContacts = false
  }
}

async function loadDomains(): Promise<void> {
  if (!selectedOrgId.value || !canManageDomains.value) {
    domains.value = []
    return
  }
  loading.domains = true
  try {
    domains.value = await organizationApi.listOrgCustomDomains(selectedOrgId.value)
  } finally {
    loading.domains = false
  }
}

async function refreshOverviewAndDomains(): Promise<void> {
  loading.refresh = true
  try {
    await Promise.all([loadOverview(), loadDomains()])
    await syncRoute()
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.refreshScopeFailed')))
  } finally {
    loading.refresh = false
  }
}

async function refreshWorkspace(): Promise<void> {
  loading.refresh = true
  try {
    await Promise.all([
      loadOverview(),
      loadMailboxes(),
      loadAliases(selectedAliasId.value || routeAliasId.value),
      loadDomains()
    ])
    await syncRoute()
    ElMessage.success(t('simplelogin.messages.workspaceRefreshed'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.refreshWorkspaceFailed')))
  } finally {
    loading.refresh = false
  }
}

async function syncRoute(): Promise<void> {
  const nextQuery = {
    ...route.query,
    orgId: selectedOrgId.value || undefined,
    aliasId: selectedAliasId.value || undefined
  }
  if (String(route.query.orgId || '') === String(nextQuery.orgId || '')
    && String(route.query.aliasId || '') === String(nextQuery.aliasId || '')) {
    return
  }
  await router.replace({ query: nextQuery })
}

async function onOrgChange(orgId: string): Promise<void> {
  selectedOrgId.value = orgId || ''
  await refreshOverviewAndDomains()
}

async function onSelectAlias(aliasId: string): Promise<void> {
  selectedAliasId.value = aliasId
  await loadAliasContacts(aliasId)
  await syncRoute()
}

async function onCreateAlias(payload: CreatePassMailAliasRequest): Promise<void> {
  loading.aliasMutationId = 'create'
  try {
    const created = await passApi.createAlias(payload)
    await Promise.all([loadOverview(), loadAliases(created.id)])
    ElMessage.success(t('simplelogin.messages.aliasCreated'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.aliasCreateFailed')))
  } finally {
    loading.aliasMutationId = ''
  }
}

async function onUpdateAlias(aliasId: string, payload: UpdatePassMailAliasRequest): Promise<void> {
  loading.aliasMutationId = `save:${aliasId}`
  try {
    await passApi.updateAlias(aliasId, payload)
    await Promise.all([loadOverview(), loadAliases(aliasId)])
    ElMessage.success(t('simplelogin.messages.aliasUpdated'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.aliasUpdateFailed')))
  } finally {
    loading.aliasMutationId = ''
  }
}

async function onEnableAlias(aliasId: string): Promise<void> {
  loading.aliasMutationId = `enable:${aliasId}`
  try {
    await passApi.enableAlias(aliasId)
    await Promise.all([loadOverview(), loadAliases(aliasId)])
    ElMessage.success(t('simplelogin.messages.aliasEnabled'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.aliasEnableFailed')))
  } finally {
    loading.aliasMutationId = ''
  }
}

async function onDisableAlias(aliasId: string): Promise<void> {
  loading.aliasMutationId = `disable:${aliasId}`
  try {
    await passApi.disableAlias(aliasId)
    await Promise.all([loadOverview(), loadAliases(aliasId)])
    ElMessage.success(t('simplelogin.messages.aliasDisabled'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.aliasDisableFailed')))
  } finally {
    loading.aliasMutationId = ''
  }
}

async function onRemoveAlias(aliasId: string): Promise<void> {
  try {
    await ElMessageBox.confirm(t('simplelogin.confirm.deleteAliasMessage'), t('simplelogin.confirm.deleteAliasTitle'), {
      type: 'warning'
    })
  } catch {
    return
  }
  loading.aliasMutationId = `delete:${aliasId}`
  try {
    await passApi.deleteAlias(aliasId)
    await Promise.all([loadOverview(), loadAliases(routeAliasId.value)])
    ElMessage.success(t('simplelogin.messages.aliasDeleted'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.aliasDeleteFailed')))
  } finally {
    loading.aliasMutationId = ''
  }
}

async function onCreateMailbox(payload: CreatePassMailboxRequest): Promise<void> {
  loading.mailboxMutationId = 'create'
  try {
    await passApi.createMailbox(payload)
    await Promise.all([loadOverview(), loadMailboxes()])
    ElMessage.success(t('simplelogin.messages.mailboxAdded'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.mailboxCreateFailed')))
  } finally {
    loading.mailboxMutationId = ''
  }
}

async function onVerifyMailbox(mailboxId: string, payload: VerifyPassMailboxRequest): Promise<void> {
  loading.mailboxMutationId = `verify:${mailboxId}`
  try {
    await passApi.verifyMailbox(mailboxId, payload)
    await Promise.all([loadOverview(), loadMailboxes(), loadAliases(selectedAliasId.value)])
    ElMessage.success(t('simplelogin.messages.mailboxVerified'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.mailboxVerifyFailed')))
  } finally {
    loading.mailboxMutationId = ''
  }
}

async function onSetDefaultMailbox(mailboxId: string): Promise<void> {
  loading.mailboxMutationId = `default:${mailboxId}`
  try {
    await passApi.setDefaultMailbox(mailboxId)
    await Promise.all([loadOverview(), loadMailboxes()])
    ElMessage.success(t('simplelogin.messages.defaultMailboxUpdated'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.defaultMailboxUpdateFailed')))
  } finally {
    loading.mailboxMutationId = ''
  }
}

async function onRemoveMailbox(mailboxId: string): Promise<void> {
  try {
    await ElMessageBox.confirm(t('simplelogin.confirm.deleteMailboxMessage'), t('simplelogin.confirm.deleteMailboxTitle'), { type: 'warning' })
  } catch {
    return
  }
  loading.mailboxMutationId = `delete:${mailboxId}`
  try {
    await passApi.deleteMailbox(mailboxId)
    await Promise.all([loadOverview(), loadMailboxes(), loadAliases(selectedAliasId.value)])
    ElMessage.success(t('simplelogin.messages.mailboxDeleted'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.mailboxDeleteFailed')))
  } finally {
    loading.mailboxMutationId = ''
  }
}

async function onCreateAliasContact(payload: CreatePassAliasContactRequest): Promise<void> {
  if (!selectedAliasId.value) {
    ElMessage.warning(t('simplelogin.messages.selectAliasFirst'))
    return
  }
  loading.aliasContactMutationId = 'create'
  try {
    await passApi.createAliasContact(selectedAliasId.value, payload)
    await Promise.all([loadOverview(), loadAliasContacts(selectedAliasId.value)])
    ElMessage.success(t('simplelogin.messages.reverseAliasContactCreated'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.reverseAliasContactCreateFailed')))
  } finally {
    loading.aliasContactMutationId = ''
  }
}

async function onUpdateAliasContact(contactId: string, payload: UpdatePassAliasContactRequest): Promise<void> {
  if (!selectedAliasId.value) {
    return
  }
  loading.aliasContactMutationId = `save:${contactId}`
  try {
    await passApi.updateAliasContact(selectedAliasId.value, contactId, payload)
    await Promise.all([loadOverview(), loadAliasContacts(selectedAliasId.value)])
    ElMessage.success(t('simplelogin.messages.reverseAliasContactUpdated'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.reverseAliasContactUpdateFailed')))
  } finally {
    loading.aliasContactMutationId = ''
  }
}

async function onRemoveAliasContact(contactId: string): Promise<void> {
  if (!selectedAliasId.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      t('simplelogin.confirm.deleteReverseAliasContactMessage'),
      t('simplelogin.confirm.deleteReverseAliasContactTitle'),
      { type: 'warning' }
    )
  } catch {
    return
  }
  loading.aliasContactMutationId = `delete:${contactId}`
  try {
    await passApi.deleteAliasContact(selectedAliasId.value, contactId)
    await Promise.all([loadOverview(), loadAliasContacts(selectedAliasId.value)])
    ElMessage.success(t('simplelogin.messages.reverseAliasContactDeleted'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.reverseAliasContactDeleteFailed')))
  } finally {
    loading.aliasContactMutationId = ''
  }
}

async function onCreateDomain(): Promise<void> {
  if (!selectedOrgId.value || !canManageDomains.value) {
    ElMessage.warning(t('simplelogin.messages.selectManageableOrg'))
    return
  }
  const domain = createDomainForm.domain.trim()
  if (!domain) {
    ElMessage.warning(t('simplelogin.messages.domainRequired'))
    return
  }
  loading.domainMutationId = 'create'
  try {
    await organizationApi.createOrgCustomDomain(selectedOrgId.value, { domain })
    createDomainForm.domain = ''
    await Promise.all([loadOverview(), loadDomains()])
    ElMessage.success(t('simplelogin.messages.domainCreated'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.domainCreateFailed')))
  } finally {
    loading.domainMutationId = ''
  }
}

async function onVerifyDomain(domainId: string): Promise<void> {
  if (!selectedOrgId.value) {
    return
  }
  loading.domainMutationId = domainId
  try {
    await organizationApi.verifyOrgCustomDomain(selectedOrgId.value, domainId)
    await Promise.all([loadOverview(), loadDomains()])
    ElMessage.success(t('simplelogin.messages.domainVerified'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.domainVerifyFailed')))
  } finally {
    loading.domainMutationId = ''
  }
}

async function onSetDefaultDomain(domainId: string): Promise<void> {
  if (!selectedOrgId.value) {
    return
  }
  loading.domainMutationId = domainId
  try {
    await organizationApi.setDefaultOrgCustomDomain(selectedOrgId.value, domainId)
    await Promise.all([loadOverview(), loadDomains()])
    ElMessage.success(t('simplelogin.messages.defaultDomainUpdated'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.defaultDomainUpdateFailed')))
  } finally {
    loading.domainMutationId = ''
  }
}

async function onRemoveDomain(domainId: string): Promise<void> {
  if (!selectedOrgId.value) {
    return
  }
  try {
    await ElMessageBox.confirm(t('simplelogin.confirm.deleteDomainMessage'), t('simplelogin.confirm.deleteDomainTitle'), { type: 'warning' })
  } catch {
    return
  }
  loading.domainMutationId = domainId
  try {
    await organizationApi.removeOrgCustomDomain(selectedOrgId.value, domainId)
    await Promise.all([loadOverview(), loadDomains()])
    ElMessage.success(t('simplelogin.messages.domainDeleted'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('simplelogin.messages.domainDeleteFailed')))
  } finally {
    loading.domainMutationId = ''
  }
}

async function onCompose(reverseAliasEmail: string): Promise<void> {
  await navigateTo({ path: '/compose', query: { senderEmail: reverseAliasEmail } })
}

async function onCopy(value: string): Promise<void> {
  await navigator.clipboard.writeText(value)
  ElMessage.success(t('simplelogin.messages.clipboardCopied'))
}

function resolveMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}
</script>

<template>
  <div class="simplelogin-page">
    <SimpleLoginWorkspaceHero
      :organizations="organizations"
      :selected-org-id="selectedOrgId"
      :overview="overview"
      :loading="loading.refresh || loading.page"
      @update:selected-org-id="onOrgChange"
      @refresh="refreshWorkspace"
    />

    <section class="workspace-grid">
      <div class="workspace-stack workspace-stack--main">
        <PassAliasCenter
          :aliases="aliases"
          :forward-target-options="forwardTargetOptions"
          :loading="loading.aliases"
          :mutation-id="loading.aliasMutationId"
          :selected-alias-id="selectedAliasId"
          @create="onCreateAlias"
          @update="onUpdateAlias"
          @enable="onEnableAlias"
          @disable="onDisableAlias"
          @remove="onRemoveAlias"
          @select="onSelectAlias"
        />

        <PassAliasContactsPanel
          :alias="selectedAlias"
          :contacts="aliasContacts"
          :loading="loading.aliasContacts"
          :mutation-id="loading.aliasContactMutationId"
          @create="onCreateAliasContact"
          @update="onUpdateAliasContact"
          @remove="onRemoveAliasContact"
          @compose="onCompose"
          @copy="onCopy"
        />
      </div>

      <div class="workspace-stack workspace-stack--side">
        <PassMailboxPanel
          :mailboxes="mailboxes"
          :loading="loading.mailboxes"
          :mutation-id="loading.mailboxMutationId"
          @create="onCreateMailbox"
          @verify="onVerifyMailbox"
          @set-default="onSetDefaultMailbox"
          @remove="onRemoveMailbox"
        />

        <OrganizationsDomainsPanel
          :domains="domains"
          :loading="loading.domains"
          :can-manage="canManageDomains"
          :domain-input="createDomainForm.domain"
          :mutation-id="loading.domainMutationId"
          @update:domain-input="createDomainForm.domain = $event"
          @create="onCreateDomain"
          @verify="onVerifyDomain"
          @set-default="onSetDefaultDomain"
          @remove="onRemoveDomain"
        />

        <SimpleLoginRelayControlsPanel
          :selected-org-id="selectedOrgId"
          :domains="domains"
          :mailboxes="mailboxes"
          :can-manage="canManageDomains"
          @changed="loadOverview"
        />

        <SimpleLoginKnowledgeRail
          :overview="overview"
          :has-organizations="organizations.length > 0"
          :can-manage-domains="canManageDomains"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.simplelogin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.95fr);
  gap: 18px;
  align-items: start;
}

.workspace-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

@media (max-width: 1280px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
