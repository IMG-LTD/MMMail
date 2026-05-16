import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const source = process.env.MMMAIL_OPENAPI_SOURCE ?? '../contracts/openapi/v21-api-catalog.yaml';
const output = 'src/service/api/__generated__/openapi.d.ts';
const command = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';

function runCommand(args) {
  const result = spawnSync(command, args, {
    cwd: projectRoot,
    stdio: 'inherit'
  });

  if (result.error) {
    throw result.error;
  }

  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

mkdirSync(dirname(resolve(projectRoot, output)), { recursive: true });

runCommand(['exec', 'openapi-typescript', source, '-o', output]);
runCommand(['exec', 'oxfmt', output]);
