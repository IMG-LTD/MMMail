<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { NButton, NCard, NDataTable, NForm, NFormItem, NGi, NGrid, NInput, NInputNumber, NSpace, NTag } from 'naive-ui';
import {
  createSheetsWorkbook,
  evaluateSheetsCells,
  listSheetsWorkbooks,
  readSheetsDependencyGraph,
  readSheetsWorkbook,
  recalculateSheetsWorkbook,
  updateSheetsWorkbookCells
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'Sheets' });

const workbooks = ref<Api.Sheets.WorkbookSummary[]>([]);
const workbook = ref<Api.Sheets.WorkbookDetail | null>(null);
const dependencyGraph = ref<Api.Sheets.DependencyGraph | null>(null);
const formulaEvaluation = ref<Api.Sheets.FormulaEvaluation | null>(null);
const createModel = reactive({ title: '', rowCount: 20, colCount: 8 });
const cellModel = reactive({ rowIndex: 0, colIndex: 0, value: '' });
const formulaModel = reactive({ ref: 'B1', formula: '=SUM(A1:A2)' });

const columns = computed(() => [
  { title: $t('page.sheets.title'), key: 'title' },
  { title: $t('page.sheets.rows'), key: 'rowCount' },
  { title: $t('page.sheets.columns'), key: 'colCount' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' }
]);

const gridPreview = computed(() => workbook.value?.computedGrid?.slice(0, 8) || []);
const formulaResults = computed(() =>
  (formulaEvaluation.value?.results || []).map(item => ({
    ...item,
    dependsOn: item.dependsOn.join(', '),
    value: String(item.value ?? '')
  }))
);
const graphRows = computed(() =>
  (dependencyGraph.value?.nodes || []).map(item => ({
    ...item,
    dependsOn: item.dependsOn.join(', '),
    dependents: item.dependents.join(', ')
  }))
);
const graphOrder = computed(() => dependencyGraph.value?.topologicalOrder.slice(0, 16) || []);
const resultColumns = computed(() => [
  { title: $t('page.sheets.formula'), key: 'ref' },
  { title: $t('page.sheets.value'), key: 'value' },
  { title: $t('page.sheets.dependencies'), key: 'dependsOn' }
]);
const graphColumns = computed(() => [
  { title: $t('page.sheets.formula'), key: 'ref' },
  { title: $t('page.sheets.dependencies'), key: 'dependsOn' },
  { title: $t('page.sheets.dependents'), key: 'dependents' }
]);

function rowKey(row: Api.Sheets.WorkbookSummary) {
  return row.id;
}

function rowProps(row: Api.Sheets.WorkbookSummary) {
  return { onClick: () => openWorkbook(row.id) };
}

async function loadWorkbooks() {
  const { data, error } = await listSheetsWorkbooks({ limit: 50 });

  if (!error) {
    workbooks.value = data;
  }
}

async function openWorkbook(workbookId: string) {
  const { data, error } = await readSheetsWorkbook(workbookId);

  if (!error) {
    workbook.value = data;
    formulaEvaluation.value = null;
    await loadDependencyGraph();
  }
}

async function createWorkbook() {
  const { data, error } = await createSheetsWorkbook({ ...createModel });

  if (!error) {
    workbook.value = data;
    formulaEvaluation.value = null;
    dependencyGraph.value = null;
    await loadWorkbooks();
  }
}

async function saveCell() {
  if (!workbook.value) {
    return;
  }

  const payload = {
    currentVersion: workbook.value.currentVersion,
    edits: [{ ...cellModel }],
    sheetId: workbook.value.activeSheetId
  };
  const { data, error } = await updateSheetsWorkbookCells(workbook.value.id, payload);

  if (!error) {
    workbook.value = data;
    await loadDependencyGraph();
  }
}

async function evaluateFormula() {
  if (!workbook.value) {
    return;
  }

  const payload = { cells: [{ ...formulaModel }] };
  const { data, error } = await evaluateSheetsCells(workbook.value.id, payload);

  if (!error) {
    formulaEvaluation.value = data;
  }
}

async function loadDependencyGraph() {
  if (!workbook.value) {
    return;
  }

  const { data, error } = await readSheetsDependencyGraph(workbook.value.id);

  if (!error) {
    dependencyGraph.value = data;
  }
}

async function recalculateWorkbook() {
  if (!workbook.value) {
    return;
  }

  const { data, error } = await recalculateSheetsWorkbook(workbook.value.id);

  if (!error) {
    workbook.value = data;
    await loadDependencyGraph();
  }
}

onMounted(loadWorkbooks);
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 m:10">
      <NCard class="card-wrapper" :title="$t('route.sheets')">
        <NDataTable :columns="columns" :data="workbooks" :row-key="rowKey" :row-props="rowProps" />
      </NCard>
    </NGi>
    <NGi span="24 m:14">
      <NSpace vertical :size="16">
        <NCard class="card-wrapper" :title="$t('page.sheets.create')">
          <NForm :model="createModel" label-placement="top">
            <NFormItem path="title" :label="$t('page.sheets.title')">
              <NInput v-model:value="createModel.title" />
            </NFormItem>
            <NSpace>
              <NInputNumber v-model:value="createModel.rowCount" :min="1" :max="200" />
              <NInputNumber v-model:value="createModel.colCount" :min="1" :max="52" />
              <NButton type="primary" @click="createWorkbook">{{ $t('page.sheets.create') }}</NButton>
            </NSpace>
          </NForm>
        </NCard>
        <NCard class="card-wrapper" :title="workbook?.title || $t('route.sheets')">
          <NDataTable
            :columns="gridPreview[0]?.map((_, index) => ({ title: String(index + 1), key: String(index) })) || []"
            :data="gridPreview.map(row => Object.fromEntries(row.map((value, index) => [String(index), value])))"
          />
          <NSpace class="mt-12px">
            <NInputNumber v-model:value="cellModel.rowIndex" :min="0" />
            <NInputNumber v-model:value="cellModel.colIndex" :min="0" />
            <NInput v-model:value="cellModel.value" />
            <NButton type="primary" @click="saveCell">{{ $t('page.sheets.saveCell') }}</NButton>
            <NButton @click="recalculateWorkbook">{{ $t('page.sheets.recalculate') }}</NButton>
          </NSpace>
          <NForm class="mt-16px" :model="formulaModel" label-placement="top">
            <NGrid :x-gap="12" :y-gap="12" responsive="screen" item-responsive>
              <NGi span="24 s:6">
                <NFormItem path="ref" :label="$t('page.sheets.formula')">
                  <NInput v-model:value="formulaModel.ref" />
                </NFormItem>
              </NGi>
              <NGi span="24 s:12">
                <NFormItem path="formula" :label="$t('page.sheets.formula')">
                  <NInput v-model:value="formulaModel.formula" />
                </NFormItem>
              </NGi>
              <NGi span="24 s:6">
                <NSpace class="h-full items-end">
                  <NButton type="primary" @click="evaluateFormula">{{ $t('page.sheets.evaluate') }}</NButton>
                  <NButton @click="loadDependencyGraph">{{ $t('page.sheets.dependencyGraph') }}</NButton>
                </NSpace>
              </NGi>
            </NGrid>
          </NForm>
          <NDataTable
            v-if="formulaResults.length"
            class="mt-12px"
            size="small"
            :columns="resultColumns"
            :data="formulaResults"
          />
          <NSpace v-if="graphOrder.length" class="mt-12px">
            <NTag v-for="cellRef in graphOrder" :key="cellRef" size="small" :bordered="false">{{ cellRef }}</NTag>
          </NSpace>
          <NDataTable v-if="graphRows.length" class="mt-12px" size="small" :columns="graphColumns" :data="graphRows" />
        </NCard>
      </NSpace>
    </NGi>
  </NGrid>
</template>
