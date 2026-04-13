import { spawnSync } from 'node:child_process'
import { chmodSync, existsSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path, { resolve } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'

const SCRIPT_PATH = resolve(process.cwd(), 'scripts/nuxt-command.mjs')
const sandboxes: string[] = []

type Invocation = {
  command: string
  args: string[]
  buildDir: string
  cwd: string
}

function createSandbox(): string {
  const sandbox = mkdtempSync(path.join(tmpdir(), 'nuxt-command-'))
  const binDir = path.join(sandbox, 'bin')
  mkdirSync(binDir)
  const fakeNuxt = path.join(binDir, 'nuxt')
  writeFileSync(
    fakeNuxt,
    '#!/usr/bin/env node\nconst payload = { command: process.argv[2], args: process.argv.slice(3), buildDir: process.env.NUXT_BUILD_DIR, cwd: process.cwd() }\nprocess.stdout.write(JSON.stringify(payload))\n',
    'utf8',
  )
  chmodSync(fakeNuxt, 0o755)
  sandboxes.push(sandbox)
  return sandbox
}

function runNuxtCommand(command: string, args: string[] = []) {
  const sandbox = createSandbox()
  const result = spawnSync(process.execPath, [SCRIPT_PATH, command, ...args], {
    cwd: sandbox,
    encoding: 'utf8',
    env: {
      ...process.env,
      PATH: `${path.join(sandbox, 'bin')}:${process.env.PATH || ''}`,
    },
  })

  return {
    sandbox,
    result,
  }
}

function parseInvocation(stdout: string): Invocation {
  return JSON.parse(stdout.trim()) as Invocation
}

afterEach(() => {
  while (sandboxes.length) {
    rmSync(sandboxes.pop()!, { recursive: true, force: true })
  }
})

describe('nuxt command runner', () => {
  it('uses an isolated build dir for dev so prepare and typecheck do not wipe the live server cache', () => {
    const { result } = runNuxtCommand('dev', ['--port', '3001'])

    expect(result.status).toBe(0)
    expect(parseInvocation(result.stdout).buildDir).toBe('.nuxt-dev')
  })

  it('keeps prepare on the default build dir used by test and typecheck flows', () => {
    const { result } = runNuxtCommand('prepare', ['--logLevel', 'silent'])

    expect(result.status).toBe(0)
    expect(parseInvocation(result.stdout).buildDir).toBe('.nuxt')
  })

  it('keeps typecheck on the default build dir so it matches generated type artifacts', () => {
    const { result } = runNuxtCommand('typecheck')

    expect(result.status).toBe(0)
    expect(parseInvocation(result.stdout).buildDir).toBe('.nuxt')
  })

  it('keeps preview on the shared build dir without wiping existing build artifacts first', () => {
    const sandbox = createSandbox()
    mkdirSync(path.join(sandbox, '.nuxt'), { recursive: true })
    writeFileSync(path.join(sandbox, '.nuxt', 'manifest.json'), 'ready', 'utf8')

    const result = spawnSync(process.execPath, [SCRIPT_PATH, 'preview', '--port', '3001'], {
      cwd: sandbox,
      encoding: 'utf8',
      env: {
        ...process.env,
        PATH: `${path.join(sandbox, 'bin')}:${process.env.PATH || ''}`,
      },
    })

    expect(result.status).toBe(0)
    expect(parseInvocation(result.stdout)).toMatchObject({
      command: 'preview',
      args: ['--port', '3001'],
      buildDir: '.nuxt',
      cwd: sandbox,
    })
    expect(existsSync(path.join(sandbox, '.nuxt', 'manifest.json'))).toBe(true)
  })

  it('cleans only the default build dir for typecheck and leaves the dev cache intact', () => {
    const sandbox = createSandbox()
    mkdirSync(path.join(sandbox, '.nuxt'), { recursive: true })
    mkdirSync(path.join(sandbox, '.nuxt-dev'), { recursive: true })
    writeFileSync(path.join(sandbox, '.nuxt', 'stale.txt'), 'stale', 'utf8')
    writeFileSync(path.join(sandbox, '.nuxt-dev', 'live.txt'), 'live', 'utf8')

    const result = spawnSync(process.execPath, [SCRIPT_PATH, 'typecheck'], {
      cwd: sandbox,
      encoding: 'utf8',
      env: {
        ...process.env,
        PATH: `${path.join(sandbox, 'bin')}:${process.env.PATH || ''}`,
      },
    })

    expect(result.status).toBe(0)
    expect(existsSync(path.join(sandbox, '.nuxt', 'stale.txt'))).toBe(false)
    expect(existsSync(path.join(sandbox, '.nuxt-dev', 'live.txt'))).toBe(true)
  })

  it('cleans the build output before production builds', () => {
    const sandbox = createSandbox()
    mkdirSync(path.join(sandbox, '.output'), { recursive: true })
    writeFileSync(path.join(sandbox, '.output', 'old.txt'), 'old', 'utf8')

    const result = spawnSync(process.execPath, [SCRIPT_PATH, 'build'], {
      cwd: sandbox,
      encoding: 'utf8',
      env: {
        ...process.env,
        PATH: `${path.join(sandbox, 'bin')}:${process.env.PATH || ''}`,
      },
    })

    expect(result.status).toBe(0)
    expect(parseInvocation(result.stdout).buildDir).toBe('.nuxt')
    expect(existsSync(path.join(sandbox, '.output', 'old.txt'))).toBe(false)
  })
})
