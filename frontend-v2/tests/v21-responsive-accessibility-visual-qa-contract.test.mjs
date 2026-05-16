import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  drawer: new URL("../src/design-system/components/Drawer.vue", import.meta.url),
  modal: new URL("../src/design-system/components/Modal.vue", import.meta.url),
  uploadQueue: new URL("../src/design-system/components/UploadQueue.vue", import.meta.url),
  commandPalette: new URL("../src/design-system/components/CommandPalette.vue", import.meta.url),
  terminalLog: new URL("../src/design-system/components/TerminalLog.vue", import.meta.url),
  dataTable: new URL("../src/design-system/components/DataTable.vue", import.meta.url),
  dataGrid: new URL("../src/design-system/components/DataGrid.vue", import.meta.url),
  chartCard: new URL("../src/design-system/components/ChartCard.vue", import.meta.url),
  errorState: new URL("../src/design-system/components/ErrorState.vue", import.meta.url),
};

test("v2.1 QA components expose accessibility primitives", async () => {
  const [drawer, modal, uploadQueue, commandPalette, terminalLog] = await Promise.all(
    [files.drawer, files.modal, files.uploadQueue, files.commandPalette, files.terminalLog].map(
      (file) => readFile(file, "utf8"),
    ),
  );

  assert.match(drawer, /aria-modal/);
  assert.match(modal, /aria-modal/);
  assert.match(commandPalette, /role="listbox"/);
  assert.match(uploadQueue, /announce/);
  assert.match(terminalLog, /announce/);
});

test("v2.1 QA components expose responsive and failure-state hooks", async () => {
  const [dataTable, dataGrid, chartCard, errorState] = await Promise.all(
    [files.dataTable, files.dataGrid, files.chartCard, files.errorState].map((file) =>
      readFile(file, "utf8"),
    ),
  );

  assert.match(dataTable, /data-table--compact/);
  assert.match(dataTable, /data-table--stacked/);
  assert.match(dataGrid, /data-grid--mobile/);
  assert.match(chartCard, /chart-card--loading/);
  assert.match(errorState, /error-state--inline/);
});
