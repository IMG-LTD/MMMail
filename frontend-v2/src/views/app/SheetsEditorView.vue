<script setup lang="ts">
import { NButton } from "naive-ui";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { lt, useLocaleText } from "@/locales";
import {
  readSheetsWorkbook,
  updateSheetsWorkbookCells,
  type SheetsWorkbookDetail,
  type SheetsWorkbookSheet,
} from "@/service/api/sheets";
import { useCopilotPanel } from "@/shared/composables/useCopilotPanel";
import { useAuthStore } from "@/store/modules/auth";
import {
  canSubmitRouteEntitySave,
  createRouteEntityNavigationReset,
  hasRouteEntityChanged,
  isCurrentRouteEntity,
  isRouteEntityEditingLocked,
} from "./route-bound-editor-state";
import {
  buildPendingEdits,
  cloneGrid,
  createSelectedCell,
  isProtectedRangeCell,
  joinText,
  readGridCell,
  resolveRuntimeError,
  resolveSheetGrid,
  toColumnLabel,
  updateGridCell,
} from "./sheets-editor-helpers";
import SheetsProtectedRangeModal from "./sheets/SheetsProtectedRangeModal.vue";

interface SheetsFact {
  label: string;
  value: string;
}

const route = useRoute();
const authStore = useAuthStore();
const { tr } = useLocaleText();
const copilotPanel = useCopilotPanel();
const copilotOpen = copilotPanel.open;

const workbook = ref<SheetsWorkbookDetail | null>(null);
const selectedSheetId = ref("");
const localGrid = ref<string[][]>([]);
const selectedCell = ref(createSelectedCell());
const workbookLoading = ref(false);
const saveLoading = ref(false);
const loadError = ref("");
const saveError = ref("");
const protectedRangeOpen = ref(false);

let latestSheetsWorkbookRequest = 0;
let latestSheetsSaveRequest = 0;

const workbookId = computed(() => String(route.params.id || ""));
const loadedWorkbookMatchesRoute = computed(() =>
  isCurrentRouteEntity(workbookId.value, workbook.value?.id),
);
const editingLocked = computed(() =>
  isRouteEntityEditingLocked(workbookId.value, workbook.value?.id, workbookLoading.value),
);

const title = computed(() => {
  return (
    workbook.value?.title ||
    workbookId.value.replace(/-/g, " ") ||
    tr(lt("未命名工作簿", "未命名活頁簿", "Untitled workbook"))
  );
});

const activeSheet = computed<SheetsWorkbookSheet | null>(() => {
  if (!workbook.value) {
    return null;
  }

  return (
    workbook.value.sheets.find((sheet) => sheet.id === selectedSheetId.value) ||
    workbook.value.sheets[0] ||
    null
  );
});

const rowCount = computed(() => {
  const sheetRowCount = activeSheet.value?.rowCount || 0;
  return Math.max(sheetRowCount, localGrid.value.length);
});

const columnCount = computed(() => {
  const sheetColumnCount = activeSheet.value?.colCount || 0;
  const gridColumnCount = localGrid.value.reduce((max, row) => Math.max(max, row.length), 0);
  return Math.max(sheetColumnCount, gridColumnCount);
});

const rowIndices = computed(() => Array.from({ length: rowCount.value }, (_, index) => index));
const columnIndices = computed(() =>
  Array.from({ length: columnCount.value }, (_, index) => index),
);
const columnLabels = computed(() => columnIndices.value.map((index) => toColumnLabel(index)));
const selectedCellLabel = computed(
  () => `${toColumnLabel(selectedCell.value.colIndex)}${selectedCell.value.rowIndex + 1}`,
);

const selectedCellValue = computed({
  get() {
    return getCellValue(selectedCell.value.rowIndex, selectedCell.value.colIndex);
  },
  set(value: string) {
    updateCellValue(selectedCell.value.rowIndex, selectedCell.value.colIndex, value);
  },
});

const canEdit = computed(
  () => loadedWorkbookMatchesRoute.value && Boolean(workbook.value?.canEdit),
);
const pendingEdits = computed(() =>
  buildPendingEdits(activeSheet.value, workbook.value, localGrid.value),
);

const saveDisabled = computed(() => {
  return (
    !authStore.accessToken ||
    !workbook.value ||
    !activeSheet.value ||
    !canSubmitRouteEntitySave(
      workbookId.value,
      workbook.value.id,
      workbookLoading.value,
      saveLoading.value,
      canEdit.value,
      Boolean(pendingEdits.value.length),
    )
  );
});

const editorStatus = computed(() => {
  if (!authStore.accessToken) {
    return tr(
      lt(
        "登录后即可读取工作簿详情。",
        "登入後即可讀取活頁簿詳情。",
        "Sign in to load workbook detail.",
      ),
    );
  }

  if (loadError.value) {
    return loadError.value;
  }

  if (saveError.value) {
    return saveError.value;
  }

  if (workbookLoading.value) {
    return tr(lt("正在加载工作簿内容。", "正在載入活頁簿內容。", "Loading workbook content."));
  }

  if (!workbook.value) {
    return tr(lt("当前工作簿不可用。", "目前活頁簿無法使用。", "This workbook is unavailable."));
  }

  if (!canEdit.value) {
    return tr(
      lt(
        "该工作簿当前为只读模式。",
        "該活頁簿目前為唯讀模式。",
        "This workbook is currently read only.",
      ),
    );
  }

  if (pendingEdits.value.length) {
    return `${pendingEdits.value.length} ${tr(lt("处单元格待保存", "處儲存格待儲存", "cells pending save"))}`;
  }

  return tr(
    lt(
      "工作簿已从运行时接口载入。",
      "活頁簿已從執行期介面載入。",
      "Workbook loaded from runtime APIs.",
    ),
  );
});

const sideTitle = computed(() => {
  if (!activeSheet.value) {
    return tr(lt("暂无工作表", "暫無工作表", "No sheet loaded"));
  }
  return `${activeSheet.value.name} · ${activeSheet.value.rowCount} × ${activeSheet.value.colCount}`;
});

const sideCopy = computed(() => {
  if (!workbook.value) {
    return tr(
      lt(
        "认证后会在此显示工作簿状态。",
        "驗證後會在此顯示活頁簿狀態。",
        "Workbook state appears here after authentication.",
      ),
    );
  }

  const formatCopy = joinText([
    workbook.value.supportedImportFormats.join(", ") || null,
    workbook.value.supportedExportFormats.join(", ") || null,
  ]);

  return joinText([
    `${workbook.value.ownerDisplayName || workbook.value.ownerEmail}`,
    formatCopy,
    formatDateTime(workbook.value.updatedAt),
  ]);
});

const workbookFacts = computed<SheetsFact[]>(() => {
  if (!workbook.value) {
    return [];
  }

  return [
    {
      label: tr(lt("权限", "權限", "Permission")),
      value: resolvePermission(workbook.value.permission),
    },
    {
      label: tr(lt("范围", "範圍", "Scope")),
      value:
        workbook.value.scope === "SHARED"
          ? tr(lt("共享", "共享", "Shared"))
          : tr(lt("个人", "個人", "Personal")),
    },
    {
      label: tr(lt("协作者", "協作者", "Collaborators")),
      value: `${workbook.value.collaboratorCount}`,
    },
    {
      label: tr(lt("版本", "版本", "Version")),
      value: `v${workbook.value.currentVersion}`,
    },
    {
      label: tr(lt("已填充单元格", "已填入儲存格", "Filled cells")),
      value: `${activeSheet.value?.filledCellCount ?? workbook.value.filledCellCount}`,
    },
    {
      label: tr(lt("公式单元格", "公式儲存格", "Formula cells")),
      value: `${activeSheet.value?.formulaCellCount ?? workbook.value.formulaCellCount}`,
    },
    {
      label: tr(lt("计算错误", "計算錯誤", "Computed errors")),
      value: `${activeSheet.value?.computedErrorCount ?? workbook.value.computedErrorCount}`,
    },
    {
      label: tr(lt("最近打开", "最近開啟", "Last opened")),
      value: formatDateTime(workbook.value.lastOpenedAt),
    },
  ];
});

onMounted(() => {
  void copilotPanel.loadCapabilities().catch(() => {});
});

function toggleCopilotPanel() {
  copilotPanel.toggle();
}

function applyWorkbookDetail(detail: SheetsWorkbookDetail | null) {
  workbook.value = detail;
  selectedSheetId.value = detail?.activeSheetId || detail?.sheets[0]?.id || "";
}

function clearEditorState(
  nextRouteWorkbookId = workbookId.value,
  nextToken = authStore.accessToken,
) {
  applyWorkbookDetail(null);
  localGrid.value = [];
  loadError.value = "";
  saveError.value = "";
  selectedCell.value = createSelectedCell();

  const resetState = createRouteEntityNavigationReset(nextRouteWorkbookId, nextToken);
  workbookLoading.value = resetState.entityLoading;
  saveLoading.value = resetState.saveLoading;
}

async function loadWorkbook() {
  const requestId = ++latestSheetsWorkbookRequest;
  const requestToken = authStore.accessToken;
  const requestPath = route.fullPath;
  const requestWorkbookId = String(route.params.id || "");

  if (!requestToken || !requestWorkbookId) {
    if (
      requestId !== latestSheetsWorkbookRequest ||
      requestToken !== authStore.accessToken ||
      requestPath !== route.fullPath
    ) {
      return;
    }

    clearEditorState(requestWorkbookId, requestToken);
    return;
  }

  workbookLoading.value = true;
  loadError.value = "";
  saveError.value = "";

  try {
    const response = await readSheetsWorkbook(String(route.params.id || ""), requestToken);

    if (
      requestId !== latestSheetsWorkbookRequest ||
      requestToken !== authStore.accessToken ||
      requestPath !== route.fullPath ||
      requestWorkbookId !== String(route.params.id || "")
    ) {
      return;
    }

    applyWorkbookDetail(response.data || null);
  } catch (error) {
    if (
      requestId !== latestSheetsWorkbookRequest ||
      requestToken !== authStore.accessToken ||
      requestPath !== route.fullPath ||
      requestWorkbookId !== String(route.params.id || "")
    ) {
      return;
    }

    applyWorkbookDetail(null);
    localGrid.value = [];
    loadError.value = resolveRuntimeError(
      error,
      tr(
        lt(
          "读取工作簿详情失败，请稍后重试。",
          "讀取活頁簿詳情失敗，請稍後重試。",
          "Failed to load the workbook. Please try again later.",
        ),
      ),
    );
  } finally {
    if (
      requestId === latestSheetsWorkbookRequest &&
      requestToken === authStore.accessToken &&
      requestPath === route.fullPath &&
      requestWorkbookId === String(route.params.id || "")
    ) {
      workbookLoading.value = false;
    }
  }
}

async function saveCells() {
  const requestToken = authStore.accessToken;
  const requestPath = route.fullPath;
  const requestWorkbookId = workbookId.value;
  const currentWorkbook = workbook.value;
  const currentSheet = activeSheet.value;
  const edits = pendingEdits.value;

  if (
    !requestToken ||
    !requestWorkbookId ||
    !currentWorkbook ||
    !currentSheet ||
    !canSubmitRouteEntitySave(
      requestWorkbookId,
      currentWorkbook.id,
      workbookLoading.value,
      saveLoading.value,
      canEdit.value,
      Boolean(edits.length),
    )
  ) {
    return;
  }

  const requestId = ++latestSheetsSaveRequest;

  saveLoading.value = true;
  saveError.value = "";

  try {
    const response = await updateSheetsWorkbookCells(
      requestWorkbookId,
      {
        currentVersion: currentWorkbook.currentVersion,
        sheetId: currentSheet.id,
        edits,
      },
      requestToken,
    );

    if (
      requestId !== latestSheetsSaveRequest ||
      requestToken !== authStore.accessToken ||
      requestPath !== route.fullPath ||
      requestWorkbookId !== String(route.params.id || "")
    ) {
      return;
    }

    applyWorkbookDetail(response.data || null);
  } catch (error) {
    if (
      requestId !== latestSheetsSaveRequest ||
      requestToken !== authStore.accessToken ||
      requestPath !== route.fullPath ||
      requestWorkbookId !== String(route.params.id || "")
    ) {
      return;
    }

    saveError.value = resolveRuntimeError(
      error,
      tr(
        lt(
          "保存工作簿失败，请稍后重试。",
          "儲存活頁簿失敗，請稍後重試。",
          "Failed to save the workbook. Please try again later.",
        ),
      ),
    );
  } finally {
    if (
      requestId === latestSheetsSaveRequest &&
      requestToken === authStore.accessToken &&
      requestPath === route.fullPath &&
      requestWorkbookId === String(route.params.id || "")
    ) {
      saveLoading.value = false;
    }
  }
}

function selectSheet(sheetId: string) {
  if (sheetId === selectedSheetId.value) {
    return;
  }

  selectedSheetId.value = sheetId;
}

function selectCell(rowIndex: number, colIndex: number) {
  selectedCell.value = createSelectedCell(rowIndex, colIndex);
}

function onCellInput(rowIndex: number, colIndex: number, event: Event) {
  const target = event.target;

  if (!(target instanceof HTMLInputElement)) {
    return;
  }

  updateCellValue(rowIndex, colIndex, target.value);
}

function updateCellValue(rowIndex: number, colIndex: number, value: string) {
  if (editingLocked.value || !canEdit.value) {
    return;
  }

  localGrid.value = updateGridCell(localGrid.value, rowIndex, colIndex, value);
}

function getCellValue(rowIndex: number, colIndex: number) {
  return readGridCell(localGrid.value, rowIndex, colIndex);
}

function resolvePermission(permission: SheetsWorkbookDetail["permission"]) {
  if (permission === "OWNER") {
    return tr(lt("所有者", "擁有者", "Owner"));
  }

  if (permission === "EDIT") {
    return tr(lt("可编辑", "可編輯", "Editable"));
  }

  return tr(lt("只读", "唯讀", "Read only"));
}

function formatDateTime(value: string | null) {
  if (!value) {
    return tr(lt("未设置", "未設定", "Not set"));
  }

  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
}

watch(
  () => [workbookId.value, route.fullPath, authStore.accessToken],
  (nextValue, previousValue) => {
    if (!previousValue || hasRouteEntityChanged(previousValue[0], nextValue[0])) {
      clearEditorState(nextValue[0], nextValue[2]);
    }

    void loadWorkbook();
  },
  { immediate: true },
);

watch(
  activeSheet,
  (sheet) => {
    localGrid.value = cloneGrid(resolveSheetGrid(sheet, workbook.value));
    selectedCell.value = createSelectedCell();
  },
  { immediate: true },
);
</script>

<template>
  <section class="page-shell surface-grid sheets-editor">
    <article class="surface-card sheets-editor__top">
      <div>
        <span class="section-label">{{
          tr(lt("表格编辑器", "試算表編輯器", "Sheets editor"))
        }}</span>
        <h1>{{ title }}</h1>
        <p class="page-subtitle">
          {{
            tr(
              lt(
                "公式栏、工作表标签和上下文引用共同提供一个网格优先的 Beta 编辑界面。",
                "公式列、工作表分頁與內容脈絡引用共同提供一個網格優先的 Beta 編輯介面。",
                "Formula bar, sheet tabs, and contextual references deliver a grid-first beta editing surface.",
              ),
            )
          }}
        </p>
        <p class="page-subtitle sheets-editor__status">{{ editorStatus }}</p>
      </div>
      <div class="sheets-editor__actions">
        <NButton native-type="button">{{ tr(lt("格式", "格式", "Format")) }}</NButton>
        <NButton
          class="sheets-protected-range-trigger"
          native-type="button"
          @click="protectedRangeOpen = true"
        >
          {{ tr(lt("保护区域", "保護範圍", "Protect range")) }}
        </NButton>
        <NButton native-type="button">{{
          canEdit ? tr(lt("可编辑", "可編輯", "Editable")) : tr(lt("只读", "唯讀", "Read only"))
        }}</NButton>
        <NButton native-type="button" :disabled="saveDisabled" @click="saveCells()">
          {{ saveLoading ? tr(lt("保存中", "儲存中", "Saving")) : tr(lt("保存", "儲存", "Save")) }}
        </NButton>
        <NButton native-type="button" @click="toggleCopilotPanel()">
          {{
            copilotOpen
              ? tr(lt("Copilot 已打开", "Copilot 已開啟", "Copilot open"))
              : tr(lt("切换 Copilot", "切換 Copilot", "Toggle Copilot"))
          }}
        </NButton>
      </div>
    </article>

    <section class="sheets-editor__layout">
      <article class="surface-card sheets-editor__grid">
        <div class="sheets-editor__sheet-tabs">
          <NButton
            v-for="sheet in workbook?.sheets || []"
            :key="sheet.id"
            native-type="button"
            :class="{ 'sheets-editor__sheet-tab--active': sheet.id === selectedSheetId }"
            @click="selectSheet(sheet.id)"
          >
            {{ sheet.name }}
          </NButton>
        </div>
        <label class="sheets-editor__formula">
          <span>{{ selectedCellLabel }}</span>
          <input
            v-model="selectedCellValue"
            type="text"
            :readonly="editingLocked || !canEdit || !activeSheet"
            :placeholder="
              tr(
                lt(
                  '选择一个单元格以编辑值',
                  '選擇儲存格以編輯內容',
                  'Select a cell to edit its value',
                ),
              )
            "
          />
        </label>
        <div v-if="activeSheet" class="sheets-editor__table-scroll">
          <div
            class="sheets-editor__table"
            :style="{ gridTemplateColumns: `56px repeat(${columnCount}, minmax(96px, 1fr))` }"
          >
            <div class="sheets-editor__corner" />
            <div
              v-for="column in columnLabels"
              :key="column"
              class="sheets-editor__cell sheets-editor__cell--head"
            >
              {{ column }}
            </div>
            <template v-for="rowIndex in rowIndices" :key="rowIndex">
              <div class="sheets-editor__cell sheets-editor__cell--head">{{ rowIndex + 1 }}</div>
              <div
                v-for="columnIndex in columnIndices"
                :key="`${rowIndex}-${columnIndex}`"
                class="sheets-editor__cell"
                :class="{
                  'sheets-editor__cell--protected':
                    protectedRangeOpen && isProtectedRangeCell(rowIndex, columnIndex),
                  'sheets-editor__cell--selected':
                    rowIndex === selectedCell.rowIndex && columnIndex === selectedCell.colIndex,
                }"
              >
                <input
                  :value="getCellValue(rowIndex, columnIndex)"
                  type="text"
                  :readonly="editingLocked || !canEdit"
                  @focus="selectCell(rowIndex, columnIndex)"
                  @input="onCellInput(rowIndex, columnIndex, $event)"
                />
              </div>
            </template>
          </div>
        </div>
        <p v-else class="page-subtitle">
          {{
            tr(
              lt(
                "当前没有可显示的工作表。",
                "目前沒有可顯示的工作表。",
                "There is no sheet to display.",
              ),
            )
          }}
        </p>
      </article>
      <aside class="surface-card sheets-editor__side">
        <span class="section-label">{{
          tr(lt("函数参考", "函數參考", "Function reference"))
        }}</span>
        <strong>{{ sideTitle }}</strong>
        <p class="page-subtitle">{{ sideCopy }}</p>
        <div v-if="workbookFacts.length" class="sheets-editor__facts">
          <div v-for="fact in workbookFacts" :key="fact.label" class="sheets-editor__fact">
            <span class="section-label">{{ fact.label }}</span>
            <strong>{{ fact.value }}</strong>
          </div>
        </div>
      </aside>
    </section>
    <SheetsProtectedRangeModal
      v-model:show="protectedRangeOpen"
      :selected-cell-label="selectedCellLabel"
    />
  </section>
</template>

<style scoped src="./sheets-editor-view.css"></style>
