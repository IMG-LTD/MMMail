import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const source = process.env.MMMAIL_OPENAPI_SOURCE ?? '../contracts/openapi/v21-api-catalog.yaml';
const output = 'src/service/api/__generated__/openapi.d.ts';

mkdirSync(dirname(resolve(projectRoot, output)), { recursive: true });

const command = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';
const result = spawnSync(command, ['exec', 'openapi-typescript', source, '-o', output], {
  cwd: projectRoot,
  stdio: 'inherit'
});

if (result.error) {
  throw result.error;
}

process.exit(result.status ?? 1);
