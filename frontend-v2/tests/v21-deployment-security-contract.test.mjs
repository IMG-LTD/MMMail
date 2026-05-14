import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const root = new URL('../../', import.meta.url)
const composeFile = new URL('docker-compose.yml', root)
const dockerfile = new URL('backend/Dockerfile', root)
const ciFile = new URL('.github/workflows/ci.yml', root)

test('deployment assets keep v2.1 security and build hygiene gates explicit', async () => {
  const [compose, backendDockerfile, ci] = await Promise.all([
    readFile(composeFile, 'utf8'),
    readFile(dockerfile, 'utf8'),
    readFile(ciFile, 'utf8')
  ])

  assert.match(compose, /NACOS_AUTH_ENABLE:\s*"true"/)
  assert.match(compose, /NACOS_AUTH_TOKEN:\s*\$\{NACOS_AUTH_TOKEN\}/)
  assert.doesNotMatch(compose, /SPRING_KAFKA_BOOTSTRAP_SERVERS:\s*127\.0\.0\.1:9092/)

  assert.match(backendDockerfile, /COPY backend\/mmmail-billing\/pom\.xml backend\/mmmail-billing\/pom\.xml/)
  assert.match(backendDockerfile, /ENV JAVA_TOOL_OPTIONS=/)
  assert.match(backendDockerfile, /USER mmmail/)

  assert.match(ci, /cache:\s*['"]?pnpm['"]?/)
  assert.match(ci, /cache:\s*['"]?maven['"]?/)
})
