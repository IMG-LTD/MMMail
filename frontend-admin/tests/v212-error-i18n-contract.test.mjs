import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

function extractErrorCodes(apiSpec) {
  return [...apiSpec.matchAll(/errors\.(\d+)\.title/g)].map(match => match[1]);
}

function extractExportBlock(source, name) {
  const start = source.indexOf(`export const ${name} = {`);
  const end = source.indexOf('} satisfies ErrorMessageMap;', start);
  assert.notEqual(start, -1, `missing ${name} export`);
  assert.notEqual(end, -1, `missing ${name} satisfies marker`);
  return source.slice(start, end);
}

function assertLocaleHasError(messages, code, markers) {
  const block = new RegExp(`'${code}':\\s*\\{[\\s\\S]*?title:\\s*'[^']+'[\\s\\S]*?message:\\s*'([^']+)'[\\s\\S]*?\\}`);
  const match = messages.match(block);
  assert.ok(match, `missing errors.${code}.title/message`);
  for (const marker of markers) {
    assert.match(match[1], marker);
  }
}

test('v2.1.2 documented error codes have bilingual i18n titles and messages', async () => {
  const [apiSpec, zhCN, enUS, zhTW, schema, errorMessages] = await Promise.all([
    read('../docs/api-spec.md'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/en-us.ts'),
    read('src/locales/langs/zh-tw.ts'),
    read('src/typings/app.d.ts'),
    read('src/locales/langs/v212-error-messages.ts')
  ]);

  const zhCNMessages = extractExportBlock(errorMessages, 'zhCNErrorMessages');
  const enUSMessages = extractExportBlock(errorMessages, 'enUSErrorMessages');
  const codes = [...new Set(extractErrorCodes(apiSpec))];

  assert.ok(codes.length > 0, 'docs/api-spec.md should list v2.1.2 errors.* keys');
  assert.match(schema, /errors:\s*Record<string,\s*\{\s*title: string;\s*message: string;?\s*\}>/);
  assert.match(zhCN, /errors:\s*zhCNErrorMessages/);
  assert.match(enUS, /errors:\s*enUSErrorMessages/);
  assert.match(zhTW, /\.\.\.zhCN/);

  for (const code of codes) {
    assertLocaleHasError(zhCNMessages, code, [/原因：/, /处理：/]);
    assertLocaleHasError(enUSMessages, code, [/Reason:/, /Next step:/]);
  }
});
