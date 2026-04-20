<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import AppLogo from '@/shared/components/AppLogo.vue'
import { lt, useLocaleText } from '@/locales'
import MaturityBadge from '@/shared/components/MaturityBadge.vue'
import LocaleSwitcher from '@/shared/components/LocaleSwitcher.vue'
import { useAppStore } from '@/store/modules/app'
import { useThemeStore } from '@/store/modules/theme'
import { findShellSurface, getToneColorVar } from './shell-nav'

interface UtilityAction {
  badge?: string
  icon: string
  key: 'command' | 'notifications' | 'theme'
  label: ReturnType<typeof lt>
  path?: string
}

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const themeStore = useThemeStore()
const { activeScope } = storeToRefs(appStore)
const { drawerOpen } = storeToRefs(themeStore)
const { tr } = useLocaleText()

const currentSurface = computed(() => findShellSurface(route.path))

const activeScopeLabel = computed(() => {
  if (activeScope.value === 'Enterprise') {
    return tr(lt('企业范围', '企業範圍', 'Enterprise scope'))
  }

  return activeScope.value
})

const utilityActions: UtilityAction[] = [
  { key: 'command', icon: '⌘', label: lt('指挥中心', '指揮中心', 'Command Center'), path: '/command-center' },
  { key: 'notifications', icon: '◉', label: lt('通知', '通知', 'Notifications'), path: '/notifications', badge: '2' },
  { key: 'theme', icon: '⚙', label: lt('主题设置', '主題設定', 'Theme settings') }
]

function handleUtilityAction(key: string) {
  if (key === 'theme') {
    themeStore.openDrawer()
    return
  }

  const action = utilityActions.find(item => item.key === key)

  if (action?.path && route.path !== action.path) {
    router.push(action.path)
  }
}

function isUtilityActionActive(key: string) {
  if (key === 'theme') {
    return drawerOpen.value
  }

  const action = utilityActions.find(item => item.key === key)

  return !!action?.path && route.path.startsWith(action.path)
}
</script>

<template>
  <header class="top-bar">
    <div class="top-bar__left">
      <app-logo />
      <div v-if="currentSurface" class="top-bar__surface">
        <span class="top-bar__surface-dot" :style="{ background: getToneColorVar(currentSurface.tone) }" />
        <span class="top-bar__surface-copy">{{ tr(currentSurface.label) }}</span>
        <maturity-badge v-if="currentSurface.maturity" compact :level="currentSurface.maturity" />
      </div>
    </div>
    <div class="top-bar__center">
      <label class="top-bar__search">
        <span>⌕</span>
        <input type="text" :placeholder="tr(lt('搜索邮件、联系人、文件…', '搜尋郵件、聯絡人、檔案…', 'Search mail, people, and files…'))" />
        <kbd>⌘K</kbd>
      </label>
    </div>
    <div class="top-bar__right">
      <button class="scope-pill" type="button">{{ activeScopeLabel }} ▾</button>
      <locale-switcher class="top-bar__locale" compact />
      <button
        v-for="action in utilityActions"
        :key="action.key"
        class="icon-button"
        :class="{ 'icon-button--active': isUtilityActionActive(action.key) }"
        type="button"
        :aria-label="tr(action.label)"
        @click="handleUtilityAction(action.key)"
      >
        {{ action.icon }}
        <span v-if="action.badge" class="icon-button__count">{{ action.badge }}</span>
      </button>
      <button class="avatar-chip" type="button" :aria-label="tr(lt('账户', '帳戶', 'Account'))">NE</button>
    </div>
  </header>
</template>

<style scoped>
.top-bar {
  display: grid;
  grid-template-columns: auto minmax(320px, 520px) auto;
  align-items: center;
  gap: 16px;
  position: sticky;
  top: 0;
  z-index: 20;
  height: 56px;
  padding: 0 12px 0 10px;
  background: var(--mm-topbar);
  border-bottom: 1px solid var(--mm-border);
  backdrop-filter: blur(18px);
}

.top-bar__left,
.top-bar__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-bar__right {
  justify-content: flex-end;
}

.top-bar__center {
  width: 100%;
}

.top-bar__surface {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--mm-border);
  border-radius: 999px;
  background: var(--mm-card-muted);
}

.top-bar__surface-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.top-bar__surface-copy {
  font-size: 12px;
  font-weight: 600;
  color: var(--mm-ink);
}

.scope-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 999px;
  color: var(--mm-text-secondary);
  background: var(--mm-card-muted);
  font-size: 12px;
}

.top-bar__search {
  display: grid;
  grid-template-columns: 16px 1fr auto;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 999px;
  background: var(--mm-card-muted);
}

.top-bar__search:focus-within {
  border-color: var(--mm-accent-border);
  box-shadow: 0 0 0 1px var(--mm-accent-border);
}

.top-bar__search input {
  width: 100%;
  border: 0;
  background: transparent;
  color: var(--mm-text);
  font-size: 12px;
  outline: none;
}

.top-bar__search span,
.top-bar__search kbd {
  color: var(--mm-text-secondary);
  font-size: 11px;
}

.top-bar__search kbd {
  font-family: inherit;
}

.top-bar__locale {
  min-height: 28px;
  padding: 0 8px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: var(--mm-card);
  font-size: 11px;
  color: var(--mm-text-secondary);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.icon-button {
  position: relative;
  width: 28px;
  height: 28px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: var(--mm-card);
  color: var(--mm-text-secondary);
  font-size: 11px;
}

.icon-button__count {
  position: absolute;
  top: -4px;
  right: -4px;
  display: inline-grid;
  place-items: center;
  min-width: 14px;
  height: 14px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--mm-danger);
  color: #fff;
  font-size: 8px;
  font-weight: 700;
}

.icon-button--active {
  border-color: var(--mm-accent-border);
  background: var(--mm-accent-soft);
  color: var(--mm-primary);
}

.avatar-chip {
  display: inline-grid;
  place-items: center;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--mm-primary) 0%, var(--mm-primary-pressed) 100%);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
}

@media (max-width: 1024px) {
  .top-bar {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .top-bar__right {
    gap: 8px;
  }
}

@media (max-width: 820px) {
  .top-bar {
    gap: 10px;
    padding-inline: 10px;
  }

  .top-bar__center {
    min-width: 0;
  }

  .top-bar__surface {
    display: none;
  }

  .top-bar__search {
    grid-template-columns: 16px 1fr;
  }

  .top-bar__search input {
    font-size: 11px;
  }

  .top-bar__search kbd,
  .top-bar__locale,
  .icon-button {
    display: none;
  }

  .scope-pill {
    display: none;
  }
}
</style>
