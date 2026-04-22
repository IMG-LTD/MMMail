import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const mailView = new URL('../src/views/public/PublicMailShareView.vue', import.meta.url)
const driveView = new URL('../src/views/public/PublicDriveShareView.vue', import.meta.url)
const passView = new URL('../src/views/public/PublicPassShareView.vue', import.meta.url)

test('public share views read runtime payloads instead of frozen placeholder copy', async () => {
  const [mail, drive, pass] = await Promise.all([
    readFile(mailView, 'utf8'),
    readFile(driveView, 'utf8'),
    readFile(passView, 'utf8')
  ])

  assert.match(mail, /useRoute/)
  assert.match(mail, /readPublicMailShare/)
  assert.match(mail, /decryptMailPublicBody/)
  assert.match(mail, /route\.params\.token/)
  assert.match(mail, /watch\(token/)
  assert.doesNotMatch(mail, /Project Phoenix: Final Asset Transfer/)

  assert.match(drive, /readPublicDriveShareMetadata/)
  assert.match(drive, /listPublicDriveShareItems/)
  assert.match(drive, /downloadPublicDriveShareItem/)
  assert.match(drive, /shareFlow\.password/)
  assert.match(drive, /watch\(token/)
  assert.match(drive, /downloadError/)

  assert.match(pass, /readPublicPassShare/)
  assert.match(pass, /navigator\.clipboard\.writeText/)
  assert.match(pass, /route\.params\.token/)
  assert.match(pass, /watch\(token/)
})
