import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SKIP_DOCKER_SETUP = '1';
const ROOT_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

export default async function globalSetup() {
  if (process.env.MMMAIL_E2E_SKIP_DOCKER_SETUP === SKIP_DOCKER_SETUP) {
    return;
  }
  runDockerE2eCommand(['e2e', '--setup-only']);
}

function runDockerE2eCommand(args: string[]) {
  const result = spawnSync('bash', ['scripts/run-tests-docker.sh', ...args], {
    cwd: ROOT_DIR,
    env: process.env,
    stdio: 'inherit'
  });

  if (result.status !== 0) {
    throw new Error(`Docker e2e setup failed with status ${result.status}`);
  }
}
