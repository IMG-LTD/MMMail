import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const packageFile = new URL('../package.json', import.meta.url)
const httpFile = new URL('../src/service/request/http.ts', import.meta.url)
const apiFile = new URL('../src/service/api/public-share.ts', import.meta.url)
const utilFile = new URL('../src/shared/utils/mail-public.ts', import.meta.url)

test('public share runtime API and crypto helpers stay wired', async () => {
  const [pkg, http, api, util] = await Promise.all([
    readFile(packageFile, 'utf8'),
    readFile(httpFile, 'utf8'),
    readFile(apiFile, 'utf8'),
    readFile(utilFile, 'utf8')
  ])

  assert.match(pkg, /"openpgp"/)
  assert.match(http, /getBlob<T extends boolean = false>\(/)
  assert.match(api, /export interface PublicMailShare/)
  assert.match(api, /export interface PublicPassShare/)
  assert.match(api, /export interface PublicDriveShareMetadata/)
  assert.match(api, /readPublicMailShare\(token: string\)/)
  assert.match(api, /downloadPublicMailAttachment\(token: string, attachmentId: string\)/)
  assert.match(api, /readPublicDriveShareMetadata\(token: string\)/)
  assert.match(api, /listPublicDriveShareItems\(token: string, password\?: string\)/)
  assert.match(api, /downloadPublicDriveShareItem\(token: string, itemId: string, password\?: string\)/)
  assert.match(api, /readPublicPassShare\(token: string\)/)
  assert.match(util, /decryptMailPublicBody/)
  assert.match(util, /decryptMailPublicAttachmentBlob/)
  assert.match(util, /triggerMailPublicDownload/)
})
