import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 EntitlementGate mirrors route meta into access props', async () => {
  const [gate, authTypes, routerTypes] = await Promise.all([
    read('src/components/access/EntitlementGate.vue'),
    read('src/typings/api/auth.d.ts'),
    read('src/typings/router.d.ts')
  ]);

  assert.match(gate, /useRoute/);
  assert.match(gate, /route\.meta/);
  assert.match(gate, /mergedAccessMeta/);
  assert.match(gate, /role/);
  assert.match(authTypes, /role\?: string/);
  assert.match(authTypes, /fallback\?: AccessFallback/);
  assert.match(routerTypes, /fallback\?: Api\.Auth\.AccessFallback/);
});

test('v2.1.2 EntitlementGate provides localized fallback actions', async () => {
  const [gate, appTypes, zhCN, enUS] = await Promise.all([
    read('src/components/access/EntitlementGate.vue'),
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  for (const fallback of ['upgrade', 'contact-sales', 'trial', 'forbidden']) {
    assert.match(gate, new RegExp(`['"]?${fallback}['"]?\\s*:`));
  }
  assert.match(gate, /page\.accessGate\.\$\{fallbackType(?:\.value)?\}\.title/);
  assert.match(gate, /handlePrimaryAction/);
  assert.match(gate, /handleSecondaryAction/);
  assert.doesNotMatch(gate, />upgrade</);
  assert.match(appTypes, /accessGate: \{[\s\S]*upgrade:/);
  assert.match(zhCN, /accessGate: \{[\s\S]*立即升级/);
  assert.match(enUS, /accessGate: \{[\s\S]*Upgrade now/);
});
