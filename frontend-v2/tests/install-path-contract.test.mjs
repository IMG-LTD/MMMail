import test from 'node:test'
import assert from 'node:assert/strict'
import { access, readFile } from 'node:fs/promises'

const repoRoot = new URL('../../', import.meta.url)

const files = {
  readme: new URL('README.md', repoRoot),
  installZh: new URL('docs/ops/install.md', repoRoot),
  installEn: new URL('docs/ops/install.en.md', repoRoot),
  runbook: new URL('docs/deployment-runbook.md', repoRoot),
  installSh: new URL('scripts/install.sh', repoRoot),
  installPs1: new URL('scripts/install.ps1', repoRoot)
}

async function fileExists(file) {
  await access(file)
  return true
}

test('install docs expose one-click, manual, local, and production paths', async () => {
  const [readme, installZh, installEn, runbook] = await Promise.all([
    readFile(files.readme, 'utf8'),
    readFile(files.installZh, 'utf8'),
    readFile(files.installEn, 'utf8'),
    readFile(files.runbook, 'utf8')
  ])

  assert.match(readme, /一键安装/)
  assert.match(readme, /Docker 手动安装/)
  assert.match(readme, /裸机手动安装/)
  assert.match(readme, /本地体验/)
  assert.match(readme, /scripts\/install\.sh/)
  assert.match(readme, /scripts\/install\.ps1/)

  assert.match(installZh, /## 1\. 路径选择/)
  assert.match(installZh, /## 2\. 一键安装/)
  assert.match(installZh, /## 3\. Docker 手动安装/)
  assert.match(installZh, /## 4\. 裸机手动安装/)
  assert.match(installZh, /## 5\. 本地体验 \/ 开发/)
  assert.match(installZh, /## 6\. 验证方式/)
  assert.match(installZh, /## 7\. 常见问题/)

  assert.match(installEn, /## 1\. Choose an install path/)
  assert.match(installEn, /## 2\. One-click install/)
  assert.match(installEn, /## 3\. Docker manual install/)
  assert.match(installEn, /## 4\. Bare-metal manual install/)
  assert.match(installEn, /## 5\. Local experience \/ development/)
  assert.match(installEn, /## 6\. Verification/)
  assert.match(installEn, /## 7\. FAQ/)

  assert.match(runbook, /First-time install source of truth/)
  assert.match(runbook, /docs\/ops\/install\.md/)
})

test('install scripts expose the same minimal and standard compose modes', async () => {
  assert.equal(await fileExists(files.installSh), true)
  assert.equal(await fileExists(files.installPs1), true)

  const [installSh, installPs1] = await Promise.all([
    readFile(files.installSh, 'utf8'),
    readFile(files.installPs1, 'utf8')
  ])

  assert.match(installSh, /docker compose --env-file "\$ENV_FILE" -f docker-compose\.minimal\.yml up -d --build/)
  assert.match(installSh, /docker compose --env-file "\$ENV_FILE" up -d --build/)
  assert.match(installSh, /validate-runtime-env\.sh/)
  assert.match(installSh, /MMMAIL_NACOS_ENABLED=false/)
  assert.match(installSh, /MMMAIL_NACOS_ENABLED=true/)

  assert.match(installPs1, /docker compose --env-file \$EnvFile -f docker-compose\.minimal\.yml up -d --build/)
  assert.match(installPs1, /docker compose --env-file \$EnvFile up -d --build/)
  assert.match(installPs1, /Check-EnvForMode/)
  assert.match(installPs1, /MMMAIL_NACOS_ENABLED=false/)
  assert.match(installPs1, /MMMAIL_NACOS_ENABLED=true/)
})
