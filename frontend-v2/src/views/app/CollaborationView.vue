<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { useScopeGuard } from '@/shared/composables/useScopeGuard'
import {
  listCollaborationActivity,
  listCollaborationProjects,
  listCollaborationTasks,
  type CollaborationActivity,
  type CollaborationProject,
  type CollaborationTask
} from '@/service/api/collaboration'
import { useAuthStore } from '@/store/modules/auth'

const { tr } = useLocaleText()
const authStore = useAuthStore()
const { requestHeaders } = useScopeGuard()
const projects = ref<CollaborationProject[]>([])
const tasks = ref<CollaborationTask[]>([])
const activity = ref<CollaborationActivity[]>([])
const collaborationLoading = ref(false)
const loadError = ref('')
let latestCollaborationRequest = 0

const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取协作项目。', '登入後即可讀取協作專案。', 'Sign in to load collaboration projects.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  return collaborationLoading.value
    ? tr(lt('正在读取协作运行时。', '正在讀取協作執行期。', 'Loading collaboration runtime.'))
    : `${projects.value.length} ${tr(lt('个项目', '個專案', 'projects'))} · ${tasks.value.length} ${tr(lt('个任务', '個任務', 'tasks'))}`
})

const summaryCards = computed(() => [
  [lt('项目', '專案', 'Projects'), `${projects.value.length}`, statusCopy.value],
  [lt('任务', '任務', 'Tasks'), `${tasks.value.length}`, tr(lt('来自当前组织范围。', '來自目前組織範圍。', 'Loaded from the current organization scope.'))],
  [lt('活动', '活動', 'Activity'), `${activity.value.length}`, tr(lt('最近协作事件。', '最近協作事件。', 'Recent collaboration events.'))]
])

function clearCollaborationState() {
  projects.value = []
  tasks.value = []
  activity.value = []
  loadError.value = ''
  collaborationLoading.value = false
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(lt('读取协作数据失败。', '讀取協作資料失敗。', 'Failed to load collaboration data.'))
}

async function loadCollaboration() {
  const requestId = ++latestCollaborationRequest
  const requestToken = authStore.accessToken
  const scopeHeaders = requestHeaders.value
  if (!requestToken) {
    clearCollaborationState()
    return
  }

  collaborationLoading.value = true
  loadError.value = ''

  try {
    const options = { scopeHeaders, token: requestToken }
    const [nextProjects, nextTasks, nextActivity] = await Promise.all([
      listCollaborationProjects(options),
      listCollaborationTasks(options),
      listCollaborationActivity(options)
    ])
    if (requestId !== latestCollaborationRequest || requestToken !== authStore.accessToken) {
      return
    }
    projects.value = Array.isArray(nextProjects) ? nextProjects : []
    tasks.value = Array.isArray(nextTasks) ? nextTasks : []
    activity.value = Array.isArray(nextActivity) ? nextActivity : []
  } catch (error) {
    if (requestId !== latestCollaborationRequest || requestToken !== authStore.accessToken) {
      return
    }
    clearCollaborationState()
    loadError.value = resolveErrorMessage(error)
  } finally {
    if (requestId === latestCollaborationRequest && requestToken === authStore.accessToken) {
      collaborationLoading.value = false
    }
  }
}

watch(
  () => [authStore.accessToken, JSON.stringify(requestHeaders.value)],
  () => {
    void loadCollaboration()
  },
  { immediate: true }
)
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('聚合', '聚合', 'Aggregation')"
      :title="lt('协作焦点', '協作焦點', 'Collaboration focus')"
      :description="lt('跨模块协作信号会按当前范围内可见产品进行过滤。', '跨模組協作訊號會依目前範圍內可見產品進行過濾。', 'Cross-module collaboration signals filtered to the products visible in the current scope.')"
      :badge="lt('预览', '預覽', 'Preview')"
      badge-tone="preview"
    />

    <div class="collaboration-filters">
      <span class="metric-chip">{{ tr(lt('当前筛选：共享工作', '目前篩選：共享工作', 'Current filter: Shared work')) }}</span>
      <span class="metric-chip">{{ tr(lt('感知组织访问范围', '感知組織存取範圍', 'Org access aware')) }}</span>
      <span class="metric-chip">{{ tr(lt('不含邮件的摘要', '不含郵件的摘要', 'Mail-free summaries')) }}</span>
      <span v-for="project in projects" :key="project.id" class="metric-chip">{{ project.name }}</span>
    </div>

    <div class="collaboration-grid">
      <article v-for="([title, value, copy], index) in summaryCards" :key="index" class="surface-card collaboration-card">
        <span class="section-label">{{ tr(title) }}</span>
        <strong>{{ value }}</strong>
        <p class="page-subtitle">{{ copy }}</p>
      </article>
    </div>

    <article class="surface-card collaboration-activity">
      <span class="section-label">{{ tr(lt('最近活动', '最近活動', 'Recent activity')) }}</span>
      <p v-if="!activity.length" class="page-subtitle">{{ statusCopy }}</p>
      <ul v-else>
        <li v-for="item in activity" :key="item.id">
          <strong>{{ item.title }}</strong>
          <span>{{ item.product }} · {{ item.occurredAt }}</span>
        </li>
      </ul>
    </article>
  </section>
</template>

<style scoped>
.collaboration-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.collaboration-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.collaboration-card {
  padding: 20px;
}

.collaboration-activity {
  display: grid;
  gap: 12px;
  padding: 20px;
}

.collaboration-activity ul {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.collaboration-activity li {
  display: grid;
  gap: 4px;
}

.collaboration-card strong {
  display: block;
  margin: 10px 0 8px;
  font-size: 40px;
  line-height: 1;
}

@media (max-width: 920px) {
  .collaboration-grid {
    grid-template-columns: 1fr;
  }
}
</style>
