import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readPublicPage(path: string): string {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

describe('self-hosted developer station', () => {
  it('ships the unified browser handoff for docs and backend contract entry points', () => {
    const page = readPublicPage('public/self-hosted/developer.html')

    expect(page).toContain('Developer Station')
    expect(page).toContain('id="api-base-input"')
    expect(page).toContain('No backend API base provided yet.')
    expect(page).toContain('/self-hosted/adoption.html')
    expect(page).toContain('/self-hosted/team.html')
    expect(page).toContain('/self-hosted/identity.html')
    expect(page).toContain('/self-hosted/architecture.html')
    expect(page).toContain('/self-hosted/api.html')
    expect(page).toContain('/swagger-ui.html')
    expect(page).toContain('/v3/api-docs')
  })
})
