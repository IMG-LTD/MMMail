<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { VueDraggable } from 'vue-draggable-plus';
import { NButton, NCard, NDataTable, NForm, NFormItem, NGi, NGrid, NInput, NSelect, NSpace, NTag } from 'naive-ui';
import {
  createCollaborationProject,
  listCollaborationProjects,
  listCollaborationTasks,
  moveCollaborationTask,
  readCollaborationBoard
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'Collaboration' });

interface CollaborationDragEvent {
  newIndex?: number;
  to?: HTMLElement;
}

const projects = ref<Api.Collaboration.Project[]>([]);
const tasks = ref<Api.Collaboration.Task[]>([]);
const boardColumns = ref<Api.Collaboration.BoardColumn[]>([]);
const selectedProjectId = ref<string | null>(null);
const projectModel = reactive({ name: '', product: 'MAIL', status: 'ACTIVE' });

const projectColumns = computed(() => [
  { title: $t('page.collaboration.title'), key: 'name' },
  { title: $t('page.collaboration.product'), key: 'product' },
  { title: $t('page.collaboration.status'), key: 'status' },
  { title: $t('page.collaboration.tasks'), key: 'taskCount' }
]);

const taskColumns = computed(() => [
  { title: $t('page.collaboration.tasks'), key: 'title' },
  { title: $t('page.collaboration.status'), key: 'status' },
  { title: $t('page.mail.to'), key: 'assigneeEmail' }
]);

const persistedProjects = computed(() => projects.value.filter(project => /^\d+$/.test(project.id)));

const projectOptions = computed(() =>
  persistedProjects.value.map(project => ({
    label: `${project.name} / ${project.product}`,
    value: project.id
  }))
);

async function loadCollaboration() {
  const [projectResult, taskResult] = await Promise.all([
    listCollaborationProjects({ limit: 50 }),
    listCollaborationTasks()
  ]);

  if (!projectResult.error) {
    projects.value = projectResult.data;
    selectedProjectId.value = selectedProjectId.value || persistedProjects.value[0]?.id || null;
  }

  if (!taskResult.error) {
    tasks.value = taskResult.data;
  }

  await loadBoard();
}

async function loadBoard() {
  if (!selectedProjectId.value) {
    boardColumns.value = [];
    return;
  }

  const { data, error } = await readCollaborationBoard(selectedProjectId.value);

  if (!error) {
    boardColumns.value = data.columns;
  }
}

async function submitProject() {
  const { data, error } = await createCollaborationProject({ ...projectModel });

  if (!error) {
    selectedProjectId.value = data.id;
    await loadCollaboration();
  }
}

async function selectProject(projectId: string | null) {
  selectedProjectId.value = projectId;
  await loadBoard();
}

async function handleBoardMove(column: Api.Collaboration.BoardColumn, event: CollaborationDragEvent) {
  if (event.newIndex === undefined) return;

  const targetColumnId = event.to?.dataset.columnId || column.columnId;
  const targetColumn = boardColumns.value.find(item => item.columnId === targetColumnId);
  const movedTask = targetColumn?.tasks[event.newIndex];

  if (!targetColumn || !movedTask) return;

  const afterTask = targetColumn.tasks[event.newIndex - 1];
  const beforeTask = targetColumn.tasks[event.newIndex + 1];
  await moveCollaborationTask(movedTask.id, {
    columnId: targetColumn.columnId,
    afterTaskId: afterTask?.id,
    beforeTaskId: beforeTask?.id
  });
  await loadBoard();
}

onMounted(loadCollaboration);
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 l:8">
      <NSpace vertical :size="16">
        <NCard class="card-wrapper" :title="$t('route.collaboration')">
          <NDataTable :columns="projectColumns" :data="projects" />
        </NCard>
        <NCard class="card-wrapper" :title="$t('page.collaboration.create')">
          <NForm :model="projectModel" label-placement="top">
            <NFormItem path="name" :label="$t('page.collaboration.title')">
              <NInput v-model:value="projectModel.name" />
            </NFormItem>
            <NFormItem path="product" :label="$t('page.collaboration.product')">
              <NInput v-model:value="projectModel.product" />
            </NFormItem>
            <NFormItem path="status" :label="$t('page.collaboration.status')">
              <NInput v-model:value="projectModel.status" />
            </NFormItem>
            <NButton type="primary" @click="submitProject">{{ $t('page.collaboration.create') }}</NButton>
          </NForm>
        </NCard>
      </NSpace>
    </NGi>
    <NGi span="24 l:16">
      <NSpace vertical :size="16">
        <div class="rounded-8px border border-[var(--n-border-color)] bg-[var(--n-color)] p-16px">
          <NSpace vertical :size="16">
            <div class="flex flex-wrap items-center justify-between gap-12px">
              <NSelect
                v-model:value="selectedProjectId"
                class="w-280px max-w-full"
                :options="projectOptions"
                @update:value="selectProject"
              />
              <NTag size="small">{{ tasks.length }}</NTag>
            </div>
            <div class="grid grid-cols-1 gap-12px md:grid-cols-2 xl:grid-cols-5">
              <div
                v-for="column in boardColumns"
                :key="column.columnId"
                class="min-h-280px rounded-8px border border-[var(--n-border-color)] bg-[var(--n-card-color)] p-12px"
              >
                <div class="mb-12px flex items-center justify-between gap-8px">
                  <span class="text-14px font-600">{{ column.title }}</span>
                  <NTag size="small">{{ column.tasks.length }}</NTag>
                </div>
                <VueDraggable
                  v-model="column.tasks"
                  group="collaboration-board"
                  item-key="id"
                  :animation="160"
                  :data-column-id="column.columnId"
                  class="min-h-220px space-y-8px"
                  @end="handleBoardMove(column, $event)"
                >
                  <div
                    v-for="task in column.tasks"
                    :key="task.id"
                    class="cursor-move rounded-6px border border-[var(--n-border-color)] bg-[var(--n-color)] p-10px"
                  >
                    <div class="break-words text-13px font-600">{{ task.title }}</div>
                    <div class="mt-8px flex flex-wrap items-center gap-6px">
                      <NTag size="small" :bordered="false">{{ task.status }}</NTag>
                      <NTag v-if="task.assigneeEmail" size="small" :bordered="false">{{ task.assigneeEmail }}</NTag>
                    </div>
                  </div>
                </VueDraggable>
              </div>
            </div>
          </NSpace>
        </div>
        <NCard class="card-wrapper" :title="$t('page.collaboration.tasks')">
          <NDataTable :columns="taskColumns" :data="tasks" />
        </NCard>
      </NSpace>
    </NGi>
  </NGrid>
</template>
