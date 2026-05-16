import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 mail rules routes expose list, detail, and create entry points', async () => {
  const routes = await read('src/router/routes/index.ts');

  assert.match(routes, /name: 'mail_rules'/);
  assert.match(routes, /path: '\/mail\/rules'/);
  assert.match(routes, /name: 'mail_rule_detail'/);
  assert.match(routes, /path: '\/mail\/rules\/:ruleId'/);
  assert.match(routes, /name: 'mail_rule_new'/);
  assert.match(routes, /path: '\/mail\/rules\/new'/);
  assert.match(routes, /component: 'layout\.base\$view\.mail'/);
});

test('v2.1.2 mail rules page binds CRUD, enable toggle, and preview workflows', async () => {
  const [page, rulesPanel] = await Promise.all([
    read('src/views/mail/index.vue'),
    read('src/views/mail/rules/MailRulesPanel.vue')
  ]);
  const source = `${page}\n${rulesPanel}`;

  assert.match(page, /MailRulesPanel/);
  assert.match(source, /updateMailFilter/);
  assert.match(source, /deleteMailFilter/);
  assert.match(source, /filterEditorOpen/);
  assert.match(source, /editFilter/);
  assert.match(source, /toggleFilter/);
  assert.match(source, /removeFilter/);
  assert.match(source, /previewMailFilter/);
  assert.match(source, /subjectContains/);
  assert.match(source, /keywordContains/);
  assert.match(source, /labels/);
  assert.match(source, /NSwitch/);
});
