import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readPublicPage(path: string): string {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

describe('self-hosted architecture site', () => {
  it('keeps the runtime model explicit for operators and contributors', () => {
    const page = readPublicPage('public/self-hosted/architecture.html')

    expect(page).toContain('Runtime Architecture')
    expect(page).toContain('One <code>Spring Boot</code> backend process')
    expect(page).toContain('MMMAIL_NACOS_ENABLED=false')
    expect(page).toContain('Not proof of shipped service-discovery architecture')
    expect(page).toContain('docker-compose.minimal.yml')
    expect(page).toContain('/self-hosted/developer.html')
  })
})
