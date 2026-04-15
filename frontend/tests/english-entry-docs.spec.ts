import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function repoPath(path: string): string {
  return resolve(process.cwd(), '..', path)
}

function readRepoFile(path: string): string {
  return readFileSync(repoPath(path), 'utf8')
}

describe('english entry docs', () => {
  it('keeps README as a concise bilingual external entry point', () => {
    const readme = readRepoFile('README.md')

    expect(readme).toContain('## 项目简介 | Overview')
    expect(readme).toContain('## 当前已交付 | What ships today')
    expect(readme).toContain('## 当前不承诺 | What it is not')
    expect(readme).toContain('## 最小自托管启动 | Minimal self-host quick start')
    expect(readme).toContain('## 文档导航 | Documentation')
    expect(readme).toContain('- English overview: `docs/open-source/README.en.md`')
    expect(readme).toContain('- English install quickstart: `docs/ops/install.en.md`')
    expect(readme).toContain('- 支持边界 | Support boundaries: `docs/release/community-v1-support-boundaries.md`')
    expect(readme).toContain('## 验证与贡献 | Validation and contribution')
    expect(readme).not.toContain('## 当前发布状态')
    expect(readme).not.toContain('## 当前版本节奏')
    expect(readme).not.toContain('docs/release/community-v1-gate.md')
    expect(readme).not.toContain('docs/release/external-ci-handoff.md')
  })

  it('ships an English overview entry doc', () => {
    const overviewPath = 'docs/open-source/README.en.md'

    expect(existsSync(repoPath(overviewPath))).toBe(true)
    if (!existsSync(repoPath(overviewPath))) {
      return
    }

    const source = readRepoFile(overviewPath)
    expect(source).toContain('# MMMail Community Edition')
    expect(source).toContain('privacy-first, self-host-friendly collaboration baseline.')
    expect(source).toContain('Install quickstart: `../ops/install.en.md`')
    expect(source).toContain('Support boundaries: `../release/community-v1-support-boundaries.md`')
  })

  it('ships an English install quickstart that starts with minimal mode', () => {
    const installPath = 'docs/ops/install.en.md'

    expect(existsSync(repoPath(installPath))).toBe(true)
    if (!existsSync(repoPath(installPath))) {
      return
    }

    const source = readRepoFile(installPath)
    expect(source).toContain('# MMMail Community Edition Install Quickstart')
    expect(source).toContain('## Recommended first path: minimal self-hosted mode')
    expect(source).toContain('Keep `MMMAIL_NACOS_ENABLED=false`')
    expect(source).toContain('docker compose --env-file .env -f docker-compose.minimal.yml up -d --build')
    expect(source).toContain('http://127.0.0.1:3001/boundary')
  })
})
