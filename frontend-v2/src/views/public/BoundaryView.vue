<script setup lang="ts">
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()

const modules = [
  { key: 'mail', name: lt('邮件', '郵件', 'Mail'), status: lt('GA', 'GA', 'GA'), copy: lt('私密邮件、信任状态与共享投递。', '私密郵件、信任狀態與共享投遞。', 'Private mail, trust states, share delivery.') },
  { key: 'calendar', name: lt('日历', '日曆', 'Calendar'), status: lt('GA', 'GA', 'GA'), copy: lt('可用性、ICS 流程与共享排期。', '可用性、ICS 流程與共享排程。', 'Availability, ICS flow, shared scheduling.') },
  { key: 'drive', name: lt('云盘', '雲端硬碟', 'Drive'), status: lt('GA', 'GA', 'GA'), copy: lt('资源管理、共享中心与公开投递。', '資源管理、共享中心與公開投遞。', 'Explorer, sharing center, public delivery.') },
  { key: 'pass', name: lt('密码库', '密碼庫', 'Pass'), status: lt('Beta', 'Beta', 'Beta'), copy: lt('保险库、监控与安全链接。', '保險庫、監控與安全連結。', 'Vault, monitor, secure links.') },
  { key: 'docs', name: lt('文档', '文件', 'Docs'), status: lt('Beta', 'Beta', 'Beta'), copy: lt('轻量文档编辑界面。', '輕量文件編輯介面。', 'Lightweight editor surface.') },
  { key: 'sheets', name: lt('表格', '試算表', 'Sheets'), status: lt('Beta', 'Beta', 'Beta'), copy: lt('结构化网格编辑界面。', '結構化網格編輯介面。', 'Structured grid editor surface.') },
  { key: 'labs', name: lt('Labs', 'Labs', 'Labs'), status: lt('Preview', 'Preview', 'Preview'), copy: lt('统一占位预览外壳。', '統一佔位預覽外殼。', 'Unified placeholder shell.') }
]
</script>

<template>
  <section class="public-shell page-shell boundary-page">
    <div class="surface-grid boundary-page__grid">
      <article class="surface-card boundary-hero">
        <span class="section-label">{{ tr(lt('公开边界', '公開邊界', 'Public boundary')) }}</span>
        <h1 class="page-title">{{ tr(lt('MMMail 产品边界与成熟度', 'MMMail 產品邊界與成熟度', 'MMMail product scope and maturity')) }}</h1>
        <p class="page-subtitle">{{ tr(lt('用平静的方式说明哪些界面已 GA，哪些仍处于 Beta，以及哪些保持克制的轻量预览。', '用平靜的方式說明哪些介面已 GA、哪些仍處於 Beta，以及哪些保持克制的輕量預覽。', 'A calm overview of which surfaces are general availability, which remain beta, and which stay intentionally lightweight.')) }}</p>
      </article>

      <article class="surface-card boundary-card">
        <span class="section-label">{{ tr(lt('指导原则', '指導原則', 'Guiding principles')) }}</span>
        <ul>
          <li>{{ tr(lt('以隐私为先，但不把密码学细节塞进主阅读路径。', '以隱私為先，但不把密碼學細節塞進主閱讀路徑。', 'Privacy-first without pushing cryptographic details into the main reading path.')) }}</li>
          <li>{{ tr(lt('所有登录后产品界面共享同一套认证壳层。', '所有登入後產品介面共享同一套驗證殼層。', 'One authenticated shell for all logged-in product surfaces.')) }}</li>
          <li>{{ tr(lt('Beta 与 Preview 在视觉上必须低于核心工作区。', 'Beta 與 Preview 在視覺上必須低於核心工作區。', 'Beta and Preview remain visually subordinate to core workspaces.')) }}</li>
        </ul>
      </article>
    </div>

    <article class="surface-card boundary-matrix">
      <div class="boundary-matrix__header">
        <div>
          <span class="section-label">{{ tr(lt('成熟度矩阵', '成熟度矩陣', 'Maturity matrix')) }}</span>
          <h2>{{ tr(lt('当前交付边界', '目前交付邊界', 'Current delivery envelope')) }}</h2>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>{{ tr(lt('界面', '介面', 'Surface')) }}</th>
            <th>{{ tr(lt('状态', '狀態', 'Status')) }}</th>
            <th>{{ tr(lt('承诺', '承諾', 'Promise')) }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="module in modules" :key="module.key">
            <td>{{ tr(module.name) }}</td>
            <td>{{ tr(module.status) }}</td>
            <td>{{ tr(module.copy) }}</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<style scoped>
.public-shell {
  padding: 56px 0;
}

.boundary-page {
  display: grid;
  gap: 20px;
}

.boundary-page__grid {
  grid-template-columns: 1.4fr 0.6fr;
}

.boundary-hero,
.boundary-card,
.boundary-matrix {
  padding: 28px;
}

.boundary-card ul {
  margin: 18px 0 0;
  padding-left: 18px;
  color: var(--mm-text-secondary);
  line-height: 1.7;
}

.boundary-matrix h2 {
  margin: 8px 0 0;
  font-size: 30px;
  letter-spacing: -0.04em;
}

table {
  width: 100%;
  margin-top: 20px;
  border-collapse: collapse;
}

th,
td {
  padding: 14px 0;
  text-align: left;
  border-bottom: 1px solid var(--mm-border);
}

th {
  color: #7b8284;
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

td:nth-child(2) {
  font-weight: 600;
}

@media (max-width: 900px) {
  .boundary-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
