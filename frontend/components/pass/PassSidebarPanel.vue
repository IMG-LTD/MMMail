<script setup lang="ts">
import type { PassSharedVault, PassWorkspaceItemSummary, PassWorkspaceMode } from '~/types/pass-business'
import { formatPassItemType, formatPassTime } from '~/utils/pass'

const props = defineProps<{
  mode: PassWorkspaceMode
  items: PassWorkspaceItemSummary[]
  sharedVaults: PassSharedVault[]
  activeItemId: string
  activeVaultId: string
}>()

const emit = defineEmits<{
  'select-item': [itemId: string]
  'select-vault': [vaultId: string]
}>()
</script>

<template>
  <aside class="mm-card pass-sidebar">
    <div class="sidebar-head">
      <strong>{{ mode === 'PERSONAL' ? 'Credentials' : 'Vaults' }}</strong>
      <span>{{ mode === 'PERSONAL' ? items.length : sharedVaults.length }}</span>
    </div>

    <div v-if="mode === 'PERSONAL'" class="sidebar-list">
      <button
        v-for="item in items"
        :key="item.id"
        type="button"
        class="sidebar-card"
        :class="{ active: item.id === activeItemId }"
        @click="emit('select-item', item.id)"
      >
        <div class="sidebar-line">
          <span class="card-title">{{ item.title }}</span>
          <span class="card-badge">{{ formatPassItemType(item.itemType) }}</span>
        </div>
        <span class="card-meta">{{ item.username || item.website || 'No username or site' }}</span>
        <div class="sidebar-line card-foot">
          <span>{{ formatPassTime(item.updatedAt) }}</span>
          <span>{{ item.favorite ? '★ Favorite' : `Links ${item.secureLinkCount}` }}</span>
        </div>
      </button>
      <el-empty v-if="items.length === 0" description="No personal credentials yet" />
    </div>

    <div v-else class="sidebar-list">
      <button
        v-for="vault in sharedVaults"
        :key="vault.id"
        type="button"
        class="sidebar-card vault-card"
        :class="{ active: vault.id === activeVaultId }"
        @click="emit('select-vault', vault.id)"
      >
        <div class="sidebar-line">
          <span class="card-title">{{ vault.name }}</span>
          <span class="card-badge">{{ vault.accessRole }}</span>
        </div>
        <span class="card-meta">{{ vault.description || 'No vault description yet' }}</span>
        <div class="sidebar-line card-foot">
          <span>{{ vault.memberCount }} members</span>
          <span>{{ vault.itemCount }} items</span>
        </div>
      </button>
      <el-empty v-if="sharedVaults.length === 0" description="No shared vaults yet" />
    </div>
  </aside>
</template>

<style scoped>
.pass-sidebar {
  min-height: 640px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: linear-gradient(180deg, rgba(250, 252, 255, 0.9), rgba(239, 247, 250, 0.86));
}

.sidebar-head,
.sidebar-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.sidebar-head {
  font-size: 14px;
  color: #3a4a60;
}

.sidebar-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-card {
  border: 1px solid rgba(16, 24, 40, 0.08);
  background: rgba(255, 255, 255, 0.88);
  border-radius: 18px;
  padding: 14px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.sidebar-card:hover,
.sidebar-card.active {
  transform: translateY(-1px);
  border-color: rgba(71, 120, 255, 0.32);
  box-shadow: 0 20px 40px rgba(77, 102, 164, 0.14);
}

.vault-card.active {
  border-color: rgba(103, 76, 255, 0.34);
  box-shadow: 0 20px 46px rgba(103, 76, 255, 0.12);
}

.card-title {
  font-weight: 700;
  color: #101828;
}

.card-badge {
  font-size: 11px;
  border-radius: 999px;
  padding: 4px 8px;
  background: rgba(17, 24, 39, 0.06);
  color: #4b5563;
}

.card-meta,
.card-foot {
  font-size: 12px;
  color: #667085;
}
</style>
