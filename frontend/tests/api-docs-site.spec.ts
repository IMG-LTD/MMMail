import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readPublicPage(path: string): string {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

describe('self-hosted api docs site', () => {
  it('ships explicit api base controls and canonical entry points', () => {
    const page = readPublicPage('public/self-hosted/api.html')

    expect(page).toContain('Developer API Docs')
    expect(page).toContain('id="api-base-input"')
    expect(page).toContain('No backend API base provided')
    expect(page).toContain('/self-hosted/developer.html')
    expect(page).toContain('/swagger-ui.html')
    expect(page).toContain('/v3/api-docs')
    expect(page).toContain('/self-hosted/install.html')
    expect(page).toContain('/self-hosted/runbook.html')
  })
})
