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
  it('links README to English overview and install entry points in a standalone section', () => {
    const readme = readRepoFile('README.md')

    expect(readme).toContain('## English entry points')
    expect(readme).toContain('docs/open-source/README.en.md')
    expect(readme).toContain('docs/ops/install.en.md')
    expect(readme).toContain('- 最小自托管 Compose：`docker-compose.minimal.yml`\n- 升级说明：`docs/ops/upgrade.md`')
    expect(readme).toContain('## English entry points\n- Overview: `docs/open-source/README.en.md`\n- Install quickstart: `docs/ops/install.en.md`\n\n## 可观测性入口')
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
    expect(source).toContain('http://127.0.0.1:3001/suite?section=boundary')
  })
})
