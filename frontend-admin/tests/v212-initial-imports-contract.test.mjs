import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const initialShellFiles = [
  'src/store/modules/auth/index.ts',
  'src/store/modules/route/index.ts',
  'src/views/_builtin/login/modules/pwd-login.vue',
  'src/views/_builtin/login/modules/register.vue',
  'src/views/home/index.vue',
  'src/views/home/modules/line-chart.vue',
  'src/views/home/modules/pie-chart.vue',
  'src/layouts/modules/global-search/components/search-modal.vue'
];

function projectFile(path) {
  return new URL(`../${path}`, import.meta.url);
}

test('v2.1.2 initial shell imports API modules directly instead of the all-API barrel', async () => {
  for (const file of initialShellFiles) {
    const source = await readFile(projectFile(file), 'utf8');
    assert.doesNotMatch(source, /from ['"]@\/service\/api['"]/, `${file} must not import the API barrel`);
  }
});

test('v2.1.2 layouts are generated as lazy route imports', async () => {
  const routerPlugin = await readFile(projectFile('build/plugins/router.ts'), 'utf8');
  assert.match(routerPlugin, /layoutLazyImport:\s*\(\)\s*=>\s*true/);

  const routeImports = await readFile(projectFile('src/router/elegant/imports.ts'), 'utf8');
  assert.match(routeImports, /base:\s*\(\)\s*=>\s*import\("@\/layouts\/base-layout\/index\.vue"\)/);
  assert.match(routeImports, /blank:\s*\(\)\s*=>\s*import\("@\/layouts\/blank-layout\/index\.vue"\)/);
});

test('v2.1.2 local SVG sprite is loaded on demand instead of in the app entry', async () => {
  const assetsPlugin = await readFile(projectFile('src/plugins/assets.ts'), 'utf8');
  assert.doesNotMatch(assetsPlugin, /virtual:svg-icons-register/);

  const svgIcon = await readFile(projectFile('src/components/custom/svg-icon.vue'), 'utf8');
  assert.match(svgIcon, /import\('virtual:svg-icons-register'\)/);
  assert.match(svgIcon, /watchEffect/);
});

test('v2.1.2 storage helper keeps crypto and localforage out of the initial route chunk', async () => {
  const storage = await readFile(projectFile('src/utils/storage.ts'), 'utf8');
  assert.doesNotMatch(storage, /@sa\/utils/);
  assert.doesNotMatch(storage, /createLocalforage|localforage|crypto-js/);
});

test('v2.1.2 request client imports only the nanoid utility subpath', async () => {
  const axiosEntry = await readFile(projectFile('packages/axios/src/index.ts'), 'utf8');
  assert.doesNotMatch(axiosEntry, /from ['"]@sa\/utils['"]/);
  assert.match(axiosEntry, /from ['"]@sa\/utils\/nanoid['"]/);

  const utilsPackage = JSON.parse(await readFile(projectFile('packages/utils/package.json'), 'utf8'));
  assert.equal(utilsPackage.exports['./nanoid'], './src/nanoid.ts');
});

test('v2.1.2 initial store plugins import only the clone utility subpath', async () => {
  const storePlugin = await readFile(projectFile('src/store/plugins/index.ts'), 'utf8');
  const tableHook = await readFile(projectFile('src/hooks/common/table.ts'), 'utf8');
  assert.doesNotMatch(storePlugin, /from ['"]@sa\/utils['"]/);
  assert.doesNotMatch(tableHook, /from ['"]@sa\/utils['"]/);
  assert.match(storePlugin, /from ['"]@sa\/utils\/klona['"]/);
  assert.match(tableHook, /from ['"]@sa\/utils\/klona['"]/);

  const utilsPackage = JSON.parse(await readFile(projectFile('packages/utils/package.json'), 'utf8'));
  assert.equal(utilsPackage.exports['./klona'], './src/klona.ts');
});
