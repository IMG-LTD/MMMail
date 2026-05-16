import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 drive e2ee share services use backend readable-share contract', async () => {
  const service = await read('src/service/api/drive.ts');
  const typings = await read('src/typings/api/drive.d.ts');

  assert.match(service, /createEncryptedDriveShare/);
  assert.match(service, /FormData/);
  assert.match(service, /\/api\/v1\/drive\/items\/\$\{fileId\}\/shares\/e2ee/);
  assert.match(service, /readPublicDriveShareMetadata/);
  assert.match(service, /\/api\/v2\/share\/drive\/\$\{token\}/);
  assert.match(service, /downloadPublicDriveShareFile/);
  assert.match(typings, /interface EncryptedSharePayload/);
  assert.match(typings, /encryptedFile: File/);
  assert.match(typings, /interface PublicShareMetadata/);
  assert.match(typings, /e2ee: ShareReadableE2ee \| null/);
});

test('v2.1.2 drive e2ee share routes expose private and public entry points', async () => {
  const routes = await read('src/router/routes/index.ts');
  const imports = await read('src/router/elegant/imports.ts');

  assert.match(routes, /name: 'drive_file_secure_share'/);
  assert.match(routes, /path: '\/drive\/files\/:fileId\/share\/secure'/);
  assert.match(routes, /name: 'public_share'/);
  assert.match(routes, /path: '\/share\/:token'/);
  assert.match(routes, /component: 'layout\.blank\$view\.share'/);
  assert.match(routes, /constant: true/);
  assert.match(imports, /share: \(\) => import\("@\/views\/share\/index\.vue"\)/);
});

test('v2.1.2 drive page creates fragment-only e2ee links without uploading key material', async () => {
  const page = await read('src/views/drive/index.vue');

  assert.match(page, /secureShareOpen/);
  assert.match(page, /secureShareModel/);
  assert.match(page, /createEncryptedDriveShare/);
  assert.match(page, /buildSecureShareFragment/);
  assert.match(page, /#k=\$\{encodeURIComponent/);
  assert.match(page, /encryptedFile/);
  assert.match(page, /page\.driveSecureShare\.localKeyWarning/);
  assert.doesNotMatch(page, /encryptedKeyMaterialBase64/);
  assert.doesNotMatch(page, /passwordHashHex/);
});

test('v2.1.2 public share page reads token metadata and keeps fragment local', async () => {
  const page = await read('src/views/share/index.vue');

  assert.match(page, /readPublicDriveShareMetadata/);
  assert.match(page, /downloadPublicDriveShareFile/);
  assert.match(page, /window\.location\.hash/);
  assert.match(page, /decodeSecureShareFragment/);
  assert.match(page, /shareKeyFragment/);
  assert.match(page, /X-Drive-Share-Password/);
  assert.match(page, /page\.publicShare\.localKeyNotice/);
});
