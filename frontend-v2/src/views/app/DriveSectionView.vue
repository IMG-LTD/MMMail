<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { driveSections, findSurface } from '@/shared/content/route-surfaces'

const route = useRoute()
const router = useRouter()
const { tr } = useLocaleText()

const current = computed(() => findSurface(driveSections, String(route.meta.surfaceKey ?? 'drive'), 'drive'))

const files = [
  ['Q3_Strategy_Deck.pdf', '2.4 MB', 'Oct 24'],
  ['handoff-spec.doc', '1.6 MB', 'Oct 20'],
  ['crew-k.xlsx', '450 KB', 'Oct 15']
]

function openSection(key: string) {
  const pathMap: Record<string, string> = {
    drive: '/drive',
    'drive-recent': '/drive/recent',
    'drive-shared': '/drive/shared',
    'drive-starred': '/drive/starred',
    'drive-trash': '/drive/trash'
  }
  router.push(pathMap[key] ?? '/drive')
}
</script>

<template>
  <section class="drive-surface">
    <aside class="drive-surface__nav">
      <button class="drive-surface__primary" type="button">{{ tr(lt('+ 新建', '+ 新增', '+ New')) }}</button>
      <button
        v-for="item in driveSections"
        :key="item.key"
        type="button"
        :class="{ 'drive-surface__nav--active': item.key === current.key }"
        @click="openSection(item.key)"
      >
        {{ tr(item.label) }}
      </button>
      <article class="surface-card drive-surface__storage">
        <strong>11.9 GB / 15 GB</strong>
        <span class="drive-surface__meter"><span /></span>
        <p class="page-subtitle">{{ tr(lt('上传失败前，配额提醒会先在这里出现。', '上傳失敗前，配額提醒會先在這裡出現。', 'Quota notice surfaces here before uploads fail.')) }}</p>
      </article>
    </aside>

    <div class="drive-surface__content">
      <header class="drive-surface__head">
        <div>
          <span class="section-label">{{ tr(lt('云盘', '雲端硬碟', 'Drive')) }}</span>
          <h1>{{ tr(current.label) }}</h1>
          <p class="page-subtitle">{{ tr(current.description) }}</p>
        </div>
        <div class="drive-surface__actions">
          <button type="button">{{ tr(lt('网格', '網格', 'Grid')) }}</button>
          <button type="button">{{ tr(lt('排序', '排序', 'Sort')) }}</button>
          <button type="button">{{ tr(lt('上传', '上傳', 'Upload')) }}</button>
        </div>
      </header>

      <article class="surface-card drive-surface__table">
        <div class="drive-surface__table-head">
          <span>{{ tr(lt('名称', '名稱', 'Name')) }}</span>
          <span>{{ tr(lt('所有者', '擁有者', 'Owner')) }}</span>
          <span>{{ tr(lt('大小', '大小', 'Size')) }}</span>
          <span>{{ tr(lt('修改时间', '修改時間', 'Modified')) }}</span>
        </div>
        <div v-for="file in files" :key="file[0]" class="drive-surface__row">
          <strong>{{ file[0] }}</strong>
          <span>{{ tr(lt('我', '我', 'me')) }}</span>
          <span>{{ file[1] }}</span>
          <span>{{ file[2] }}</span>
        </div>
      </article>

      <div class="drive-surface__cards">
        <article v-for="file in files" :key="`${file[0]}-card`" class="surface-card drive-surface__card">
          <span class="section-label">{{ tr(lt('已加密文件', '已加密檔案', 'Encrypted file')) }}</span>
          <strong>{{ file[0] }}</strong>
          <span>{{ file[1] }}</span>
          <p class="page-subtitle">{{ tr(lt(`最后更新 ${file[2]}。共享与预览仍可从详情抽屉访问。`, `最後更新 ${file[2]}。共享與預覽仍可從詳情抽屜存取。`, `Last updated ${file[2]}. Sharing and preview remain available from the detail drawer.`)) }}</p>
        </article>
      </div>
    </div>

    <aside class="drive-surface__detail">
      <article class="surface-card drive-surface__panel">
        <span class="section-label">{{ tr(lt('预览', '預覽', 'Preview')) }}</span>
        <strong>Project_Proposal.pdf</strong>
        <p class="page-subtitle">{{ tr(lt('本地预览、共享、版本与协作者活动都集中在这个抽屉中。', '本機預覽、共享、版本與協作者活動都集中在這個抽屜中。', 'Local preview, sharing, versions, and collaborator activity stay grouped in this drawer.')) }}</p>
      </article>
      <article class="surface-card drive-surface__panel">
        <span class="section-label">{{ tr(lt('上传队列', '上傳佇列', 'Upload queue')) }}</span>
        <strong>{{ tr(lt('1 个上传已完成', '1 個上傳已完成', '1 upload completed')) }}</strong>
        <p class="page-subtitle">{{ tr(lt('已成功加密并上传。', '已成功加密並上傳。', 'Encrypted and uploaded successfully.')) }}</p>
      </article>
    </aside>
  </section>
</template>

<style scoped>
.drive-surface {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 300px;
  min-height: calc(100vh - 56px);
  background: var(--mm-card);
}

.drive-surface__nav,
.drive-surface__detail {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 16px;
  background: var(--mm-side-surface);
}

.drive-surface__nav {
  border-right: 1px solid var(--mm-border);
}

.drive-surface__detail {
  border-left: 1px solid var(--mm-border);
}

.drive-surface__primary,
.drive-surface__nav button,
.drive-surface__actions button {
  min-height: 36px;
  border-radius: 10px;
}

.drive-surface__primary {
  border: 0;
  background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
  color: #fff;
}

.drive-surface__nav button {
  padding: 0 12px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--mm-text-secondary);
  text-align: left;
}

.drive-surface__nav--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary) !important;
}

.drive-surface__storage,
.drive-surface__panel,
.drive-surface__table,
.drive-surface__card {
  padding: 16px;
}

.drive-surface__meter {
  display: flex;
  height: 6px;
  border-radius: 999px;
  background: var(--mm-card-muted);
  overflow: hidden;
}

.drive-surface__meter span {
  width: 76%;
  background: linear-gradient(90deg, var(--mm-primary) 0%, var(--mm-primary-pressed) 100%);
}

.drive-surface__content {
  display: grid;
  gap: 16px;
  padding: 16px;
}

.drive-surface__head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.drive-surface__head h1 {
  margin: 8px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
}

.drive-surface__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.drive-surface__actions button {
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  background: var(--mm-card);
}

.drive-surface__table-head,
.drive-surface__row {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr 0.7fr 0.7fr;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--mm-border);
}

.drive-surface__table-head {
  color: var(--mm-text-secondary);
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.drive-surface__row span {
  color: var(--mm-text-secondary);
  font-size: 13px;
}

.drive-surface__cards {
  display: none;
  gap: 12px;
}

.drive-surface__card {
  display: grid;
  gap: 8px;
}

@media (max-width: 1180px) {
  .drive-surface {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .drive-surface__detail {
    display: none;
  }
}

@media (max-width: 820px) {
  .drive-surface {
    grid-template-columns: 1fr;
    padding-bottom: 88px;
  }

  .drive-surface__nav {
    display: flex;
    gap: 10px;
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
    white-space: nowrap;
  }

  .drive-surface__nav button,
  .drive-surface__primary,
  .drive-surface__storage {
    flex: 0 0 auto;
  }

  .drive-surface__storage {
    width: 240px;
  }

  .drive-surface__head {
    flex-direction: column;
  }

  .drive-surface__table {
    display: none;
  }

  .drive-surface__cards {
    display: grid;
  }
}
</style>
