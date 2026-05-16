<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  NCard,
  NCheckbox,
  NGi,
  NGrid,
  NList,
  NListItem,
  NSpace,
  NStatistic,
  NTag,
  NTimeline,
  NTimelineItem
} from 'naive-ui';
import {
  listWorkspaceActivity,
  listWorkspaceTasks,
  patchWorkspaceTask,
  readWorkspaceSummary
} from '@/service/api/workspace';
import { $t } from '@/locales';

defineOptions({
  name: 'Home'
});

const loading = ref(false);
const summary = ref<Api.Workspace.Summary | null>(null);
const activity = ref<Api.Workspace.ActivityItem[]>([]);
const tasks = ref<Api.Workspace.Task[]>([]);

const completedTasks = computed(() => tasks.value.filter(task => task.completed).length);

async function loadWorkspace() {
  loading.value = true;
  const [summaryResult, activityResult, tasksResult] = await Promise.all([
    readWorkspaceSummary(),
    listWorkspaceActivity(),
    listWorkspaceTasks()
  ]);

  if (!summaryResult.error) {
    summary.value = summaryResult.data;
  }

  if (!activityResult.error) {
    activity.value = activityResult.data;
  }

  if (!tasksResult.error) {
    tasks.value = tasksResult.data;
  }

  loading.value = false;
}

async function toggleTask(task: Api.Workspace.Task, completed: boolean) {
  const { error } = await patchWorkspaceTask(task.id, { completed });

  if (!error) {
    await loadWorkspace();
  }
}

onMounted(loadWorkspace);
</script>

<template>
  <NSpace vertical :size="16">
    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi v-for="card in summary?.productCards || []" :key="card.key" span="24 s:12 m:6">
        <NCard class="card-wrapper" :loading="loading">
          <NStatistic :label="card.label" :value="card.value" />
          <NTag class="mt-12px" size="small">{{ card.state }}</NTag>
        </NCard>
      </NGi>
    </NGrid>

    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 m:8">
        <NCard class="card-wrapper" :title="$t('page.workspace.systemStatus')">
          <NStatistic :label="$t('page.workspace.recommendations')" :value="summary?.recommendationCount || 0" />
          <NTag class="mt-12px">{{ summary?.systemStatus }}</NTag>
        </NCard>
      </NGi>

      <NGi span="24 m:8">
        <NCard class="card-wrapper" :title="$t('page.workspace.tasks')">
          <NStatistic :label="$t('page.workspace.completed')" :value="completedTasks" />
          <NList class="mt-12px">
            <NListItem v-for="task in tasks" :key="task.id">
              <NCheckbox :checked="task.completed" @update:checked="value => toggleTask(task, value)">
                {{ task.title }}
              </NCheckbox>
            </NListItem>
          </NList>
        </NCard>
      </NGi>

      <NGi span="24 m:8">
        <NCard class="card-wrapper" :title="$t('page.workspace.activity')">
          <NTimeline>
            <NTimelineItem
              v-for="item in activity"
              :key="item.id"
              :title="item.title"
              :content="item.actor"
              :time="item.occurredAt"
            />
          </NTimeline>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>
</template>
