<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NLayout,
  NLayoutContent,
  NLayoutSider,
  NRadioButton,
  NRadioGroup,
  NSelect,
  NSpace,
  NStatistic,
  NTag
} from 'naive-ui';
import {
  createStandardNote,
  exportStandardNotes,
  listStandardNoteFolders,
  listStandardNotes,
  readStandardNote,
  readStandardNotesOverview,
  toggleStandardNoteChecklistItem,
  updateStandardNote
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'Notes' });

const folders = ref<Api.StandardNotes.Folder[]>([]);
const notes = ref<Api.StandardNotes.Summary[]>([]);
const overview = ref<Api.StandardNotes.Overview | null>(null);
const selectedNote = ref<Api.StandardNotes.Detail | null>(null);
const exportSnapshot = ref<Api.StandardNotes.Export | null>(null);
const editor = reactive({
  content: '',
  folderId: undefined as string | undefined,
  noteType: 'MARKDOWN',
  tagsText: '',
  title: ''
});

const folderOptions = computed(() => folders.value.map(folder => ({ label: folder.name, value: folder.id })));
const columns = computed(() => [
  { title: $t('page.standardNotes.noteTitle'), key: 'title' },
  { title: $t('page.standardNotes.type'), key: 'noteType' },
  { title: $t('page.standardNotes.folder'), key: 'folderName' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' }
]);

function rowKey(row: Api.StandardNotes.Summary) {
  return row.id;
}

function rowProps(row: Api.StandardNotes.Summary) {
  return { onClick: () => openNote(row.id) };
}

function syncEditor(note: Api.StandardNotes.Detail) {
  selectedNote.value = note;
  editor.content = note.content;
  editor.folderId = note.folderId || undefined;
  editor.noteType = note.noteType;
  editor.tagsText = note.tags.join(',');
  editor.title = note.title;
}

function notePayload() {
  return {
    content: editor.content,
    folderId: editor.folderId,
    noteType: editor.noteType,
    tags: editor.tagsText
      .split(',')
      .map(tag => tag.trim())
      .filter(Boolean),
    title: editor.title
  };
}

async function loadNotes() {
  const [overviewResult, folderResult, noteResult] = await Promise.all([
    readStandardNotesOverview(),
    listStandardNoteFolders(),
    listStandardNotes({ limit: 100 })
  ]);

  if (!overviewResult.error) {
    overview.value = overviewResult.data;
  }

  if (!folderResult.error) {
    folders.value = folderResult.data;
  }

  if (!noteResult.error) {
    notes.value = noteResult.data;
  }
}

async function openNote(noteId: string) {
  const { data, error } = await readStandardNote(noteId);

  if (!error) {
    syncEditor(data);
  }
}

async function createNote() {
  const { data, error } = await createStandardNote(notePayload());

  if (!error) {
    syncEditor(data);
    await loadNotes();
  }
}

async function saveNote() {
  if (!selectedNote.value) {
    await createNote();
    return;
  }

  const { data, error } = await updateStandardNote(selectedNote.value.id, {
    ...notePayload(),
    currentVersion: selectedNote.value.currentVersion
  });

  if (!error) {
    syncEditor(data);
    await loadNotes();
  }
}

async function toggleFirstChecklist() {
  const firstItem = selectedNote.value?.checklistItems[0];

  if (!selectedNote.value || !firstItem) {
    return;
  }

  const { data, error } = await toggleStandardNoteChecklistItem(selectedNote.value.id, firstItem.itemIndex, {
    completed: !firstItem.completed,
    currentVersion: selectedNote.value.currentVersion
  });

  if (!error) {
    syncEditor(data);
    await loadNotes();
  }
}

async function exportNotes() {
  const { data, error } = await exportStandardNotes();

  if (!error) {
    exportSnapshot.value = data;
  }
}

onMounted(loadNotes);
</script>

<template>
  <NLayout has-sider class="min-h-600px">
    <NLayoutSider bordered :width="260">
      <NCard :bordered="false" :title="$t('page.standardNotes.folders')">
        <NSpace vertical>
          <NTag v-for="folder in folders" :key="folder.id">{{ folder.name }}: {{ folder.noteCount }}</NTag>
        </NSpace>
      </NCard>
    </NLayoutSider>
    <NLayoutContent class="pl-16px">
      <NSpace vertical :size="16">
        <NCard class="card-wrapper" :title="$t('route.notes')">
          <NSpace class="mb-12px">
            <NStatistic :label="$t('page.standardNotes.total')" :value="overview?.totalNoteCount || 0" />
            <NStatistic :label="$t('page.standardNotes.checklist')" :value="overview?.checklistTaskCount || 0" />
            <NStatistic :label="$t('page.standardNotes.exported')" :value="exportSnapshot?.notes.length || 0" />
          </NSpace>
          <NDataTable :columns="columns" :data="notes" :row-key="rowKey" :row-props="rowProps" />
        </NCard>
        <NCard class="card-wrapper" :title="$t('page.standardNotes.editor')">
          <NForm :model="editor" label-placement="top">
            <NFormItem path="title" :label="$t('page.standardNotes.noteTitle')">
              <NInput v-model:value="editor.title" />
            </NFormItem>
            <NFormItem path="noteType" :label="$t('page.standardNotes.type')">
              <NRadioGroup v-model:value="editor.noteType">
                <NRadioButton value="PLAIN_TEXT">PLAIN_TEXT</NRadioButton>
                <NRadioButton value="MARKDOWN">MARKDOWN</NRadioButton>
                <NRadioButton value="CHECKLIST">CHECKLIST</NRadioButton>
              </NRadioGroup>
            </NFormItem>
            <NFormItem path="folderId" :label="$t('page.standardNotes.folder')">
              <NSelect v-model:value="editor.folderId" clearable :options="folderOptions" />
            </NFormItem>
            <NFormItem path="tagsText" :label="$t('page.standardNotes.tags')">
              <NInput v-model:value="editor.tagsText" />
            </NFormItem>
            <NFormItem path="content" :label="$t('page.docs.content')">
              <NInput v-model:value="editor.content" type="textarea" :autosize="{ minRows: 10 }" />
            </NFormItem>
            <NSpace>
              <NButton @click="createNote">{{ $t('page.standardNotes.create') }}</NButton>
              <NButton type="primary" @click="saveNote">{{ $t('page.docs.save') }}</NButton>
              <NButton :disabled="!selectedNote?.checklistItems.length" @click="toggleFirstChecklist">
                {{ $t('page.standardNotes.toggleChecklist') }}
              </NButton>
              <NButton @click="exportNotes">{{ $t('page.standardNotes.export') }}</NButton>
            </NSpace>
          </NForm>
        </NCard>
      </NSpace>
    </NLayoutContent>
  </NLayout>
</template>
