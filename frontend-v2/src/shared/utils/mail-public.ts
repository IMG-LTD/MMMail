import { decrypt, readMessage } from 'openpgp'

export async function decryptMailPublicBody(ciphertext: string, password: string) {
  const message = await readMessage({ armoredMessage: ciphertext })
  const result = await decrypt({ message, passwords: [password], format: 'utf8' })
  return String(result.data || '')
}

export async function decryptMailPublicAttachmentBlob(blob: Blob, password: string, contentType: string) {
  const message = await readMessage({ binaryMessage: new Uint8Array(await blob.arrayBuffer()) })
  const result = await decrypt({ message, passwords: [password], format: 'binary' })
  const data = new Uint8Array(Array.from(result.data as Uint8Array))
  return new Blob([data], { type: contentType || 'application/octet-stream' })
}

export function triggerMailPublicDownload(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}
