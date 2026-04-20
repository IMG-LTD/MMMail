<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import MaturityBadge from '@/shared/components/MaturityBadge.vue'
import { getToneColorVar, isMailRoute, isRouteMatch, mailNavSections, shellNavGroups, type NavItem } from './shell-nav'

const route = useRoute()
const router = useRouter()
const { tr } = useLocaleText()

const activePath = computed(() => route.path)
const isInboxNav = computed(() => isMailRoute(route.path))

function isMailItemActive(path: string) {
  return route.path.startsWith(path)
}

function isShellItemActive(item: NavItem) {
  return isRouteMatch(route.path, item)
}

function openPath(path: string) {
  if (activePath.value !== path) {
    router.push(path)
  }
}
</script>

<template>
  <aside class="side-nav" :class="{ 'side-nav--mail': isInboxNav }">
    <template v-if="isInboxNav">
      <button class="compose-button" type="button" @click="openPath('/compose')">
        {{ tr(lt('写邮件', '寫郵件', 'Compose')) }}
      </button>

      <div
        v-for="group in mailNavSections"
        :key="group.items.map(item => item.key).join('-')"
        class="side-nav__group"
      >
        <p class="side-nav__title">{{ tr(group.title) }}</p>
        <button
          v-for="item in group.items"
          :key="item.key"
          type="button"
          class="side-nav__item"
          :class="{ 'side-nav__item--active': isMailItemActive(item.path) }"
          @click="openPath(item.path)"
        >
          <span class="side-nav__glyph">{{ item.icon }}</span>
          <span>{{ tr(item.label) }}</span>
          <span v-if="item.badge" class="side-nav__badge">{{ tr(item.badge) }}</span>
        </button>
      </div>

      <div class="side-nav__footer side-nav__footer--stacked">
        <div class="side-nav__meter-line">
          <span class="side-nav__meter" />
          <span>4.2 GB / 16 GB</span>
        </div>
        <button type="button" @click="openPath('/security')">{{ tr(lt('安全', '安全', 'Security')) }}</button>
        <button type="button" @click="openPath('/notifications')">{{ tr(lt('活动', '活動', 'Activity')) }}</button>
      </div>
    </template>

    <template v-else>
      <div class="side-nav__identity">
        <span class="side-nav__title">{{ tr(lt('工作区', '工作區', 'Workspace')) }}</span>
        <strong>{{ tr(lt('已认证', '已驗證', 'Authenticated')) }}</strong>
      </div>

      <div
        v-for="group in shellNavGroups"
        :key="group.items.map(item => item.key).join('-')"
        class="side-nav__group"
      >
        <p class="side-nav__title">{{ tr(group.title) }}</p>
        <p v-if="group.caption" class="side-nav__caption">{{ tr(group.caption) }}</p>
        <button
          v-for="item in group.items"
          :key="item.key"
          type="button"
          class="side-nav__item"
          :class="{ 'side-nav__item--active': isShellItemActive(item) }"
          @click="openPath(item.path)"
        >
          <span class="side-nav__dot" :style="{ background: getToneColorVar(item.tone) }" />
          <span class="side-nav__item-copy">
            <span>{{ tr(item.label) }}</span>
            <maturity-badge v-if="item.maturity" compact :level="item.maturity" />
          </span>
          <span v-if="item.badge" class="side-nav__badge">{{ tr(item.badge) }}</span>
        </button>
      </div>
      <div class="side-nav__footer">
        <span class="side-nav__meter" />
        <span>{{ tr(lt('存储', '儲存空間', 'Storage')) }}</span>
      </div>
    </template>
  </aside>
</template>

<style scoped>
.side-nav {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 240px;
  padding: 14px 12px 12px;
  border-right: 1px solid var(--mm-border);
  background: linear-gradient(180deg, var(--mm-card) 0%, var(--mm-side-surface) 100%);
}

.side-nav--mail {
  gap: 14px;
  padding-top: 14px;
}

.side-nav__group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.side-nav__title {
  margin: 0 6px 2px;
  color: color-mix(in srgb, var(--mm-text-secondary) 74%, white);
  font-size: 10px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.side-nav__caption {
  margin: -2px 6px 2px;
  color: color-mix(in srgb, var(--mm-text-secondary) 56%, white);
  font-size: 10px;
}

.side-nav__identity strong {
  display: block;
  margin: 2px 6px 0;
  font-size: 13px;
  font-weight: 600;
}

.side-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  color: var(--mm-text-secondary);
  background: transparent;
  text-align: left;
  font-size: 13px;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease;
}

.side-nav__item:hover {
  background: color-mix(in srgb, var(--mm-card-muted) 76%, white);
}

.side-nav__item-copy {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.side-nav__item--active {
  background: var(--mm-accent-soft);
  border-color: var(--mm-accent-border);
  color: var(--mm-primary);
  font-weight: 600;
}

.side-nav__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.side-nav__glyph {
  width: 14px;
  color: color-mix(in srgb, var(--mm-text-secondary) 82%, white);
  font-size: 11px;
  text-align: center;
}

.side-nav__badge {
  margin-left: auto;
  color: color-mix(in srgb, var(--mm-text-secondary) 82%, white);
  font-size: 11px;
}

.compose-button {
  min-height: 36px;
  padding: 0 12px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(180deg, var(--mm-primary) 0%, var(--mm-primary-pressed) 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 10px 22px color-mix(in srgb, var(--mm-primary) 22%, transparent);
}

.side-nav__footer {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  font-size: 12px;
  color: var(--mm-text-secondary);
  background: var(--mm-card-muted);
}

.side-nav__footer--stacked {
  align-items: stretch;
  flex-direction: column;
}

.side-nav__footer--stacked button {
  min-height: 28px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--mm-text-secondary);
  text-align: left;
}

.side-nav__meter-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.side-nav__meter {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--mm-governance) 0%, var(--mm-governance) 24%, color-mix(in srgb, var(--mm-border) 92%, white) 24%, color-mix(in srgb, var(--mm-border) 92%, white) 100%);
}

@media (max-width: 820px) {
  .side-nav {
    display: none;
  }
}
</style>
