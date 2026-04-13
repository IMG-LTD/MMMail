import { spawn } from 'node:child_process'
import { rmSync } from 'node:fs'
import path from 'node:path'

const [, , command = 'prepare', ...args] = process.argv
const cwd = process.cwd()
const buildDir = command === 'dev' ? '.nuxt-dev' : '.nuxt'
const shouldCleanBuildDir = new Set(['prepare', 'typecheck', 'build'])

if (shouldCleanBuildDir.has(command)) {
  rmSync(path.join(cwd, buildDir), { recursive: true, force: true })
}
if (command === 'build') {
  rmSync(path.join(cwd, '.output'), { recursive: true, force: true })
}

const child = spawn('nuxt', [command, ...args], {
  cwd,
  env: {
    ...process.env,
    NUXT_BUILD_DIR: buildDir
  },
  shell: true,
  stdio: 'inherit'
})

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal)
    return
  }
  process.exit(code ?? 0)
})
