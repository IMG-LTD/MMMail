import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const dialogFile = new URL('../src/shared/composables/useDialogStack.ts', import.meta.url)

test('dialog stack remains centralized', async () => {
  const content = await readFile(dialogFile, 'utf8')

  assert.match(content, /kind: 'dialog' \| 'drawer' \| 'sheet'/)
  assert.match(content, /stack = ref<DialogStackEntry\[]>\(\[]\)/)
})
