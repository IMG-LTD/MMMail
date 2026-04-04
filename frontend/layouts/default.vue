<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { SystemMailFolder } from '~/types/api'
import { useAuthStore } from '~/stores/auth'
import { useMailStore } from '~/stores/mail'
import { useOrgAccessStore } from '~/stores/org-access'
import { useMailApi } from '~/composables/useMailApi'
import { useMailFolderApi } from '~/composables/useMailFolderApi'
import { useKeyboardShortcuts } from '~/composables/useKeyboardShortcuts'
import { useI18n } from '~/composables/useI18n'
import { DEFAULT_NAV_ITEMS } from '~/utils/default-nav'
import { flattenMailFolderTree } from '~/utils/mail-folders'
import { buildMailAddressBlockedQuery } from '~/utils/org-access-recovery'
import {
  filterNavItemsByAccess,
  isProductAccessible,
  isProductEnabledForMailAddressMode,
  resolveHomeRoute,
  resolveProductKeyFromPath
} from '~/utils/org-product-access'

const authStore = useAuthStore()
const mailStore = useMailStore()
const orgAccessStore = useOrgAccessStore()
const { fetchStats } = useMailApi()
const { listMailFolders } = useMailFolderApi()
const { t } = useI18n()
const quickKeyword = ref('')
const route = useRoute()

useKeyboardShortcuts()

const userLabel = computed(() => authStore.user?.displayName || authStore.user?.email || t('common.guest'))
const securityRailActive = computed(() => route.path.startsWith('/security'))
const mailEnabled = computed(() => isProductAccessible('MAIL', orgAccessStore.isProductEnabled, authStore.user?.mailAddressMode))
const homeLink = computed(() => resolveHomeRoute(orgAccessStore.isProductEnabled, authStore.user?.mailAddressMode))
const localizedAccessibleNavItems = computed(() => filterNavItemsByAccess(
  DEFAULT_NAV_ITEMS,
  orgAccessStore.isProductEnabled,
  authStore.user?.mailAddressMode
).map(item => ({
  ...item,
  label: t(item.labelKey)
})))
const customFolderItems = computed(() => flattenMailFolderTree(mailStore.customFolders))

function folderCount(folder?: SystemMailFolder): number {
  if (!folder) {
    return 0
  }
  return mailStore.counts[folder] || 0
}

function starredCount(): number {
  return mailStore.starredCount || 0
}

function unreadBadgeCount(): number {
  return mailStore.unreadCount || 0
}

async function loadStats(): Promise<void> {
  if (!mailEnabled.value) {
    return
  }
  try {
    const [stats, customFolders] = await Promise.all([fetchStats(), listMailFolders()])
    mailStore.updateStats(stats)
    mailStore.setCustomFolders(customFolders)
  } catch (error) {
    console.error('Failed to refresh mail stats', error)
  }
}

async function goSearch(): Promise<void> {
  if (!mailEnabled.value) {
    return
  }
  await navigateTo({ path: '/search', query: { keyword: quickKeyword.value || undefined } })
}

async function onScopeChange(orgId: string): Promise<void> {
  orgAccessStore.setActiveOrgId(orgId)
  const blockedProduct = resolveProductKeyFromPath(route.path)
  if (blockedProduct && !isProductAccessible(blockedProduct, orgAccessStore.isProductEnabled, authStore.user?.mailAddressMode)) {
    const accountEnabled = isProductEnabledForMailAddressMode(blockedProduct, authStore.user?.mailAddressMode)
    await navigateTo({
      path: '/product-access-blocked',
      query: accountEnabled
        ? {
            productKey: blockedProduct,
            from: route.fullPath,
            orgId: orgAccessStore.activeOrgId || undefined,
            orgName: orgAccessStore.activeScope?.orgName || undefined
          }
        : buildMailAddressBlockedQuery({
            from: route.fullPath,
            productKey: blockedProduct
          })
    })
    return
  }
  if (route.path === '/product-access-blocked') {
    await navigateTo(homeLink.value)
    return
  }
  await loadStats()
}

onMounted(async () => {
  if (authStore.isAuthenticated) {
    await orgAccessStore.ensureLoaded()
  }
  await loadStats()
})
</script>

<template>
  <div>
    <a class="skip-link" href="#shell-main-content">{{ t('shell.a11y.skipToContent') }}</a>
    <header class="topbar">
      <div class="brand-block">
        <NuxtLink class="brand" :to="homeLink">MMMail</NuxtLink>
        <small class="brand-subtitle">{{ t('shell.brand.subtitle') }}</small>
      </div>
      <div class="search-box">
        <el-input
          v-model="quickKeyword"
          :disabled="!mailEnabled"
          :aria-label="t('topbar.searchAriaLabel')"
          :placeholder="mailEnabled ? t('topbar.searchPlaceholder') : t('orgAccess.topbar.searchDisabled')"
          @keyup.enter="goSearch"
        />
      </div>
      <div class="topbar-meta">
        <span class="shortcut-pill">{{ t('topbar.shortcutHint') }} · Ctrl/Cmd + K</span>
        <OrgScopeSwitcher
          v-if="orgAccessStore.hasScopes"
          :model-value="orgAccessStore.activeOrgId"
          :scopes="orgAccessStore.accessScopes"
          @update:model-value="onScopeChange"
        />
        <LocaleSwitcher size="small" />
        <div class="user">{{ userLabel }}</div>
      </div>
    </header>
    <div class="shell">
      <aside class="sidebar mm-card">
        <nav class="sidebar-nav" :aria-label="t('shell.a11y.mainNavigation')">
          <NuxtLink
            v-for="item in localizedAccessibleNavItems"
            :key="item.to"
            :to="item.to"
            class="nav-item"
            active-class="active"
          >
            <span>{{ item.label }}</span>
            <el-badge v-if="item.folder" :value="folderCount(item.folder)" :max="9999" class="badge" />
            <el-badge v-else-if="item.unread" :value="unreadBadgeCount()" :max="9999" class="badge" />
            <el-badge v-else-if="item.starred" :value="starredCount()" :max="9999" class="badge" />
          </NuxtLink>
          <div class="custom-folder-rail" v-if="mailEnabled && customFolderItems.length">
            <div class="custom-folder-head">{{ t('mailFolders.sidebar.title') }}</div>
            <NuxtLink
              v-for="folder in customFolderItems"
              :key="folder.id"
              :to="`/folders/${folder.id}`"
              class="nav-item nav-item--folder"
              active-class="active"
              :style="{ paddingLeft: `${12 + folder.depth * 18}px` }"
            >
              <span class="folder-label">
                <span class="folder-swatch" :style="{ backgroundColor: folder.color }" />
                {{ folder.name }}
              </span>
              <el-badge :value="folder.unreadCount" :hidden="folder.unreadCount === 0" :max="9999" class="badge" />
            </NuxtLink>
          </div>
        </nav>
      </aside>
      <main id="shell-main-content" class="content" tabindex="-1" :aria-label="t('shell.a11y.mainContent')">
        <slot />
      </main>
    </div>
    <NuxtLink
      class="security-rail"
      :class="{ active: securityRailActive }"
      :aria-label="t('topbar.securityAriaLabel')"
      to="/security"
    >
      <span class="security-rail-badge">{{ t('topbar.securityBadge') }}</span>
      <strong>{{ t('topbar.securityTitle') }}</strong>
      <small>{{ t('topbar.securitySubtitle') }}</small>
    </NuxtLink>
    <GlobalCommandPalette />
  </div>
</template>

<style scoped>
.skip-link {
  position: absolute;
  left: 16px;
  top: 12px;
  z-index: 50;
  padding: 8px 12px;
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.16);
  transform: translateY(-160%);
}

.skip-link:focus-visible {
  transform: translateY(0);
}

.topbar {
  height: 64px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  background: linear-gradient(90deg, #0f6e6e 0%, #0c5a5a 100%);
  color: #f7fbfd;
}

.brand {
  font-size: 22px;
  letter-spacing: 1px;
  font-weight: 700;
}

.brand-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-subtitle {
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  opacity: 0.78;
}

.search-box {
  max-width: 520px;
  width: 100%;
}

.user {
  font-size: 14px;
  opacity: 0.92;
}

.topbar-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.shortcut-pill {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.26);
  color: #f8fbfd;
}

.shell {
  display: flex;
  gap: 16px;
  padding: 16px 88px 16px 16px;
}

.sidebar {
  width: 240px;
  padding: 12px;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: fit-content;
}

.nav-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--mm-muted);
  font-weight: 500;
}

.nav-item.active,
.nav-item:hover {
  background: #e8f7f7;
  color: var(--mm-primary-dark);
}

.badge {
  margin-left: 8px;
}

.custom-folder-rail {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(15, 110, 110, 0.12);
}

.custom-folder-head {
  padding: 0 12px 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #6b7f85;
}

.nav-item--folder {
  margin-top: 4px;
}

.folder-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.folder-swatch {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08);
}

.content {
  flex: 1;
  min-width: 0;
}

.security-rail {
  position: fixed;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 64px;
  padding: 14px 10px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(15, 23, 42, 0.12);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.14);
  display: grid;
  gap: 8px;
  text-align: center;
  color: #344054;
  z-index: 30;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.security-rail:hover,
.security-rail.active {
  transform: translateY(-50%) translateX(-2px);
  border-color: rgba(124, 108, 255, 0.28);
  box-shadow: 0 20px 48px rgba(91, 78, 180, 0.18);
}

.security-rail-badge {
  display: inline-flex;
  justify-content: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(124, 108, 255, 0.12);
  color: #5b4eb4;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.security-rail strong {
  font-size: 12px;
  line-height: 1.3;
}

.security-rail small {
  font-size: 10px;
  color: #667085;
  line-height: 1.4;
}

@media (max-width: 1024px) {
  .topbar {
    grid-template-columns: 1fr;
    height: auto;
    padding: 12px 16px;
  }

  .topbar-meta {
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .shell {
    padding-right: 16px;
  }
}

@media (max-width: 768px) {
  .shell {
    flex-direction: column;
    padding: 12px;
  }

  .sidebar {
    width: 100%;
  }

  .security-rail {
    position: static;
    transform: none;
    width: auto;
    margin: 0 12px 12px;
  }

  .security-rail:hover,
  .security-rail.active {
    transform: none;
  }
}
</style>
