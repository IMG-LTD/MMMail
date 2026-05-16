import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 sheets services expose formula evaluation contracts', async () => {
  const [service, types] = await Promise.all([
    read('src/service/api/sheets.ts'),
    read('src/typings/api/expanded.d.ts')
  ]);

  for (const marker of ['evaluateSheetsCells', 'readSheetsDependencyGraph', 'recalculateSheetsWorkbook']) {
    assert.match(service, new RegExp(marker));
  }

  for (const endpoint of [
    '/api/v1/sheets/${workbookId}/cells/evaluate',
    '/api/v1/sheets/${workbookId}/dependency-graph',
    '/api/v1/sheets/${workbookId}/recalculate'
  ]) {
    assert.match(service, new RegExp(endpoint.replaceAll('/', '\\/').replaceAll('$', '\\$')));
  }

  for (const typeName of [
    'FormulaEvaluationPayload',
    'FormulaEvaluation',
    'FormulaCellResult',
    'DependencyGraph',
    'FormulaGraphNode'
  ]) {
    assert.match(types, new RegExp(`interface ${typeName}`));
  }
});

test('v2.1.2 sheets page binds evaluate, graph, and recalculate flows', async () => {
  const page = await read('src/views/sheets/index.vue');

  for (const marker of [
    'evaluateSheetsCells',
    'readSheetsDependencyGraph',
    'recalculateSheetsWorkbook',
    'formulaEvaluation',
    'dependencyGraph'
  ]) {
    assert.match(page, new RegExp(marker));
  }
});

test('v2.1.2 sheets formula labels are translated', async () => {
  const [appTypes, zhCN, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  for (const key of ['formula', 'evaluate', 'recalculate', 'dependencies', 'dependencyGraph']) {
    assert.match(appTypes, new RegExp(`${key}: string`));
    assert.match(zhCN, new RegExp(`${key}: '`));
    assert.match(enUS, new RegExp(`${key}: '`));
  }
});
