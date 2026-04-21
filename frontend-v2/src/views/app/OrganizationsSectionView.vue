<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lt, type TextLike, useLocaleText } from '@/locales'
import { findSurface, organizationSections } from '@/shared/content/route-surfaces'

const route = useRoute()
const router = useRouter()
const { tr } = useLocaleText()

const current = computed(() => findSurface(organizationSections, String(route.meta.surfaceKey ?? 'summary'), 'summary'))

interface OrganizationRow {
  id: string
  name: TextLike
  role: TextLike
  status: TextLike
  activity: TextLike
}

const rows: OrganizationRow[] = [
  {
    id: 'executive-board',
    name: lt('执行委员会', '執行委員會', 'Executive board'),
    role: lt('所有者', '擁有者', 'Owner'),
    status: lt('已启用', '已啟用', 'Enabled'),
    activity: lt('2 分钟前', '2 分鐘前', '2 mins ago')
  },
  {
    id: 'core-management',
    name: lt('核心管理组', '核心管理組', 'Core management'),
    role: lt('管理员', '管理員', 'Admin'),
    status: lt('已启用', '已啟用', 'Enabled'),
    activity: lt('12 分钟前', '12 分鐘前', '12 mins ago')
  },
  {
    id: 'external-contractors',
    name: lt('外部承包方', '外部承包方', 'External contractors'),
    role: lt('成员', '成員', 'Member'),
    status: lt('受限', '受限', 'Limited'),
    activity: lt('昨天', '昨天', 'Yesterday')
  }
]

function openSection(key: string) {
  const pathMap: Record<string, string> = {
    summary: '/organizations',
    members: '/organizations/members',
    'product-access': '/organizations/product-access',
    domains: '/organizations/domains',
    'mail-identities': '/organizations/mail-identities',
    policy: '/organizations/policy',
    monitor: '/organizations/monitor',
    'session-monitor': '/organizations/session-monitor',
    audit: '/organizations/audit'
  }
  router.push(pathMap[key] ?? '/organizations')
}
</script>

<template>
  <section class="page-shell surface-grid org-section">
    <header class="surface-card org-section__hero">
      <div>
        <span class="section-label">{{ tr(lt('组织', '組織', 'Organizations')) }}</span>
        <h1>{{ tr(current.label) }}</h1>
        <p class="page-subtitle">{{ tr(current.description) }}</p>
      </div>
      <button type="button">{{ tr(lt('编辑组织', '編輯組織', 'Edit organization')) }}</button>
    </header>

    <nav class="org-section__nav">
      <button
        v-for="item in organizationSections"
        :key="item.key"
        type="button"
        :class="{ 'org-section__nav--active': item.key === current.key }"
        @click="openSection(item.key)"
      >
        {{ tr(item.label) }}
      </button>
    </nav>

    <article class="surface-card org-section__table">
      <div class="org-section__table-head">
        <span>{{ tr(lt('单元 / 团队', '單元 / 團隊', 'Unit / Team')) }}</span>
        <span>{{ tr(lt('角色', '角色', 'Role')) }}</span>
        <span>{{ tr(lt('状态', '狀態', 'Status')) }}</span>
        <span>{{ tr(lt('最近活跃', '最近活躍', 'Last active')) }}</span>
      </div>
      <div v-for="row in rows" :key="row.id" class="org-section__row">
        <strong>{{ tr(row.name) }}</strong>
        <span>{{ tr(row.role) }}</span>
        <span>{{ tr(row.status) }}</span>
        <span>{{ tr(row.activity) }}</span>
      </div>
    </article>

    <div class="org-section__cards">
      <article v-for="row in rows" :key="`${row.id}-card`" class="surface-card org-section__card">
        <span class="section-label">{{ tr(row.role) }}</span>
        <strong>{{ tr(row.name) }}</strong>
        <span>{{ tr(row.status) }}</span>
        <p class="page-subtitle">
          {{
            tr(
              lt(
                `最近活跃 ${tr(row.activity)}。移动卡片视图会优先保留最重要属性。`,
                `最近活躍 ${tr(row.activity)}。行動卡片檢視會優先保留最重要屬性。`,
                `Last active ${tr(row.activity)}. Mobile card view preserves the most important attributes first.`
              )
            )
          }}
        </p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.org-section__hero,
.org-section__nav,
.org-section__table,
.org-section__cards {
  display: grid;
  gap: 16px;
}

.org-section__hero {
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 18px;
}

.org-section__hero h1 {
  margin: 8px 0 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.org-section__hero button,
.org-section__nav button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card);
}

.org-section__nav {
  display: flex;
  flex-wrap: wrap;
}

.org-section__nav--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

.org-section__table {
  padding: 18px;
}

.org-section__table-head,
.org-section__row {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr 0.8fr 0.8fr;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--mm-border);
}

.org-section__table-head {
  color: var(--mm-text-secondary);
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.org-section__row span {
  color: var(--mm-text-secondary);
  font-size: 13px;
}

.org-section__cards {
  display: none;
}

.org-section__card {
  display: grid;
  gap: 8px;
  padding: 18px;
}

@media (max-width: 980px) {
  .org-section__hero {
    grid-template-columns: 1fr;
  }

  .org-section__table {
    display: none;
  }

  .org-section__cards {
    display: grid;
  }
}

@media (max-width: 820px) {
  .org-section__nav {
    overflow-x: auto;
    flex-wrap: nowrap;
    white-space: nowrap;
  }

  .org-section__nav button {
    flex: 0 0 auto;
  }
}
</style>
