<script setup lang="ts">
import { Editor, EditorContent } from '@tiptap/vue-3';
import CollaborationExtension from '@tiptap/extension-collaboration';
import StarterKitExtension from '@tiptap/starter-kit';
import * as Y from 'yjs';
import { computed, onBeforeUnmount, onMounted, reactive, shallowRef, ref } from 'vue';
import { NButton, NCard, NDataTable, NForm, NFormItem, NGi, NGrid, NInput, NSpace } from 'naive-ui';
import { createDocsNote, listDocsNotes, readDocsNote, updateDocsNote } from '@/service/api';
import { createDocsCollabSession, type DocsCollabSession } from '@/hooks/business/docs-collab-crdt';
import { $t } from '@/locales';

defineOptions({ name: 'Docs' });

const notes = ref<Api.Docs.NoteSummary[]>([]);
const selectedNote = ref<Api.Docs.NoteDetail | null>(null);
const editorState = reactive({ title: '', content: '', currentVersion: 1 });
const editorInstance = shallowRef<Editor>();
const collabSession = shallowRef<DocsCollabSession | null>(null);

const columns = computed(() => [
  { title: $t('page.docs.title'), key: 'title' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' },
  { title: $t('page.notifications.status'), key: 'permission' }
]);

function rowKey(row: Api.Docs.NoteSummary) {
  return row.id;
}

function rowProps(row: Api.Docs.NoteSummary) {
  return { onClick: () => openNote(row.id) };
}

function syncEditorMetadata(note: Api.Docs.NoteDetail) {
  selectedNote.value = note;
  editorState.title = note.title;
  editorState.content = note.content;
  editorState.currentVersion = note.currentVersion;
}

async function loadNotes() {
  const { data, error } = await listDocsNotes({ limit: 50 });

  if (!error) {
    notes.value = data;
  }
}

async function openNote(noteId: string) {
  const { data, error } = await readDocsNote(noteId);

  if (!error) {
    syncEditorMetadata(data);
    await attachCollabSession(data);
  }
}

async function createNote() {
  editorState.content = readEditorContent();
  const { data, error } = await createDocsNote({ title: editorState.title, content: editorState.content });

  if (!error) {
    syncEditorMetadata(data);
    await attachCollabSession(data);
    await loadNotes();
  }
}

async function saveNote() {
  if (!selectedNote.value) {
    await createNote();
    return;
  }

  editorState.content = readEditorContent();
  await collabSession.value?.flushSnapshot();
  const { data, error } = await updateDocsNote(selectedNote.value.id, { ...editorState });

  if (!error) {
    syncEditorMetadata(data);
    await loadNotes();
  }
}

async function attachCollabSession(note: Api.Docs.NoteDetail) {
  destroyCollabSession();
  const yDoc = new Y.Doc();
  replaceEditor(yDoc);
  const session = createDocsCollabSession({ resourceId: note.id, doc: yDoc, onError: showCollabError });
  collabSession.value = session;
  const snapshot = await session.bootstrap();
  if (!snapshot.snapshotBase64) {
    seedEditorContent(note.content);
    await session.flushSnapshot();
  }
  editorState.content = readEditorContent();
}

function replaceEditor(yDoc: Y.Doc) {
  editorInstance.value?.destroy();
  editorInstance.value = new Editor({
    extensions: [
      StarterKitExtension.configure({ undoRedo: false }),
      CollaborationExtension.configure({ document: yDoc })
    ],
    editorProps: { attributes: { class: 'min-h-340px outline-none' } },
    onUpdate: ({ editor }) => {
      editorState.content = editor.getHTML();
    }
  });
}

function seedEditorContent(content: string) {
  editorInstance.value?.commands.setContent(content || '<p></p>');
}

function readEditorContent() {
  return editorInstance.value?.getHTML() || editorState.content;
}

function destroyCollabSession() {
  collabSession.value?.destroy();
  collabSession.value = null;
}

function showCollabError(error: Error) {
  window.$message?.error(error.message);
}

onMounted(async () => {
  replaceEditor(new Y.Doc());
  await loadNotes();
});

onBeforeUnmount(() => {
  destroyCollabSession();
  editorInstance.value?.destroy();
});
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 m:10">
      <NCard class="card-wrapper" :title="$t('route.docs')">
        <NDataTable :columns="columns" :data="notes" :row-key="rowKey" :row-props="rowProps" />
      </NCard>
    </NGi>
    <NGi span="24 m:14">
      <NCard class="card-wrapper" :title="$t('page.docs.save')">
        <NForm :model="editorState" label-placement="top">
          <NFormItem path="title" :label="$t('page.docs.title')">
            <NInput v-model:value="editorState.title" />
          </NFormItem>
          <NFormItem path="content" :label="$t('page.docs.content')">
            <EditorContent
              :editor="editorInstance"
              class="min-h-360px rounded-8px border border-gray-200 bg-white px-14px py-12px text-14px leading-6 dark:border-gray-700 dark:bg-gray-900"
            />
          </NFormItem>
          <NSpace justify="end">
            <NButton @click="createNote">{{ $t('page.docs.create') }}</NButton>
            <NButton type="primary" @click="saveNote">{{ $t('page.docs.save') }}</NButton>
          </NSpace>
        </NForm>
      </NCard>
    </NGi>
  </NGrid>
</template>
