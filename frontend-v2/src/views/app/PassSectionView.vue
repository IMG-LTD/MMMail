<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { findSurface, passSections } from '@/shared/content/route-surfaces'

const route = useRoute()
const router = useRouter()
const { tr } = useLocaleText()

const current = computed(() => findSurface(passSections, String(route.meta.surfaceKey ?? 'pass'), 'pass'))

const entries = [
  ['Google', 'user@example.com'],
  ['GitHub', 'dev_ninja'],
  ['Chase Bank', 'Personal Checking']
]

function openSection(key: string) {
  const pathMap: Record<string, string> = {
    pass: '/pass',
    'pass-shared-library': '/pass/shared-library',
    'pass-secure-links': '/pass/secure-links',
    'pass-alias-center': '/pass/alias-center',
    'pass-mailbox': '/pass/mailbox',
    'pass-business-policy': '/pass/business-policy'
  }
  router.push(pathMap[key] ?? '/pass')
}
</script>

<template>
  <section class="pass-surface">
    <aside class="pass-surface__nav">
      <span class="section-label">{{ tr(lt('密码库', '密碼庫', 'Pass')) }}</span>
      <strong>{{ tr(current.label) }}</strong>
      <button
        v-for="item in passSections"
        :key="item.key"
        type="button"
        :class="{ 'pass-surface__nav--active': item.key === current.key }"
        @click="openSection(item.key)"
      >
        {{ tr(item.label) }}
      </button>
      <button class="pass-surface__primary" type="button">{{ tr(lt('+ 新建项目', '+ 新增項目', '+ New Item')) }}</button>
    </aside>

    <section class="pass-surface__list">
      <header class="pass-surface__head">
        <div>
          <span class="section-label">{{ tr(lt('分区', '分區', 'Section')) }}</span>
          <h1>{{ tr(current.label) }}</h1>
          <p class="page-subtitle">{{ tr(current.description) }}</p>
        </div>
      </header>
      <article v-for="entry in entries" :key="entry[0]" class="surface-card pass-surface__item">
        <span class="pass-surface__avatar">{{ entry[0][0] }}</span>
        <div>
          <strong>{{ entry[0] }}</strong>
          <span>{{ entry[1] }}</span>
        </div>
      </article>
    </section>

    <section class="pass-surface__detail">
      <article class="surface-card pass-surface__card">
        <span class="section-label">{{ tr(lt('已选项目', '已選項目', 'Selected item')) }}</span>
        <strong>GitHub</strong>
        <p class="page-subtitle">{{ tr(lt('用户名、密码、备注和一次性验证码会统一保留在同一界面中。', '使用者名稱、密碼、備註與一次性驗證碼會統一保留在同一介面中。', 'Usernames, passwords, notes, and one-time codes stay grouped in the same surface.')) }}</p>
      </article>
      <article class="surface-card pass-surface__card">
        <span class="section-label">{{ tr(lt('策略提示', '政策提示', 'Policy note')) }}</span>
        <strong>{{ tr(lt('14 天后需要轮换', '14 天後需要輪換', 'Rotation required in 14 days')) }}</strong>
        <p class="page-subtitle">{{ tr(lt('共享资料库与策略分区可以在组织范围内提高保险库限制。', '共享資料庫與政策分區可以在組織範圍內提高保險庫限制。', 'Shared library and policy sections can elevate vault restrictions inside organization scope.')) }}</p>
      </article>
    </section>
  </section>
</template>

<style scoped>
.pass-surface {
  display: grid;
  grid-template-columns: 220px 360px minmax(0, 1fr);
  min-height: calc(100vh - 56px);
  background: var(--mm-card);
}

.pass-surface__nav,
.pass-surface__list,
.pass-surface__detail {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 16px;
}

.pass-surface__nav,
.pass-surface__list {
  border-right: 1px solid var(--mm-border);
}

.pass-surface__nav {
  background: var(--mm-side-surface);
}

.pass-surface__nav button,
.pass-surface__primary {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 10px;
}

.pass-surface__nav button {
  border: 1px solid transparent;
  background: transparent;
  color: var(--mm-text-secondary);
  text-align: left;
}

.pass-surface__nav--active {
  border-color: rgba(228, 123, 57, 0.24) !important;
  background: rgba(228, 123, 57, 0.1) !important;
  color: var(--mm-pass) !important;
}

.pass-surface__primary {
  margin-top: auto;
  border: 0;
  background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
  color: #fff;
}

.pass-surface__head h1 {
  margin: 8px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.pass-surface__item,
.pass-surface__card {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.pass-surface__item {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
}

.pass-surface__avatar {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: rgba(228, 123, 57, 0.12);
  color: var(--mm-pass);
  font-weight: 700;
}

.pass-surface__item span:last-child {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

@media (max-width: 980px) {
  .pass-surface {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .pass-surface__detail {
    display: none;
  }
}

@media (max-width: 820px) {
  .pass-surface {
    grid-template-columns: 1fr;
    padding-bottom: 88px;
  }

  .pass-surface__nav {
    align-items: center;
    display: flex;
    gap: 10px;
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
    white-space: nowrap;
  }

  .pass-surface__nav > * {
    flex: 0 0 auto;
  }

  .pass-surface__nav button {
    white-space: nowrap;
  }

  .pass-surface__list {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }
}
</style>
