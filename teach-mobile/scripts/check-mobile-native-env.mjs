import { readFile } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import { join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import process from 'node:process'

const mobileRoot = resolve(fileURLToPath(new URL('..', import.meta.url)))
const envFiles = ['.env.mobile', '.env.mobile.local']
const placeholderHosts = new Set(['your-domain.example', 'example.com'])
const localHosts = new Set(['localhost', '127.0.0.1', '0.0.0.0'])
const values = new Map()

function parseEnv(content) {
  const parsed = new Map()
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue

    const separator = line.indexOf('=')
    if (separator === -1) continue

    const key = line.slice(0, separator).trim()
    let value = line.slice(separator + 1).trim()
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    parsed.set(key, value)
  }
  return parsed
}

async function loadMobileEnv() {
  const loadedFiles = []

  for (const fileName of envFiles) {
    try {
      const filePath = join(mobileRoot, fileName)
      const parsed = parseEnv(await readFile(filePath, 'utf8'))
      for (const [key, value] of parsed) {
        values.set(key, value)
      }
      loadedFiles.push(fileName)
    } catch (error) {
      if (error?.code !== 'ENOENT') throw error
    }
  }

  return loadedFiles
}

function fail(message) {
  console.error('[!!] Mobile native environment check failed')
  console.error(message)
  console.error('')
  console.error('Create teach-mobile/.env.mobile from .env.mobile.example and set VITE_API_BASE_URL to a phone/emulator reachable API URL.')
  console.error('Android emulator example: VITE_API_BASE_URL=http://10.0.2.2:8820/api')
  console.error('Physical device example: VITE_API_BASE_URL=http://<your-lan-ip>:8820/api')
  process.exit(1)
}

function assertLoadedEnvFilesAreIgnored(loadedFiles) {
  const gitProbe = spawnSync('git', ['rev-parse', '--is-inside-work-tree'], {
    cwd: mobileRoot,
    encoding: 'utf8'
  })
  if (gitProbe.status !== 0) return

  for (const fileName of loadedFiles) {
    const result = spawnSync('git', ['check-ignore', '--quiet', fileName], {
      cwd: mobileRoot,
      encoding: 'utf8'
    })
    if (result.status !== 0) {
      fail(`${fileName} contains native API configuration and must be ignored by Git.`)
    }
  }
}

const loadedFiles = await loadMobileEnv()
if (!loadedFiles.length) {
  fail('Missing .env.mobile or .env.mobile.local for a native app build.')
}
assertLoadedEnvFilesAreIgnored(loadedFiles)

const apiBaseUrl = values.get('VITE_API_BASE_URL') || ''
if (!apiBaseUrl) {
  fail('VITE_API_BASE_URL is not set in the mobile native environment.')
}

let parsedUrl
try {
  parsedUrl = new URL(apiBaseUrl)
} catch {
  fail(`VITE_API_BASE_URL must be an absolute http(s) URL, got "${apiBaseUrl}".`)
}

if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
  fail(`VITE_API_BASE_URL must use http or https, got "${parsedUrl.protocol}".`)
}

if (localHosts.has(parsedUrl.hostname)) {
  fail(`VITE_API_BASE_URL uses "${parsedUrl.hostname}", which points at the phone/emulator itself in a native app.`)
}

if (placeholderHosts.has(parsedUrl.hostname)) {
  fail('VITE_API_BASE_URL still uses the placeholder domain from .env.mobile.example.')
}

if (values.get('VITE_MOBILE_DEMO') === 'true' && process.env.ALLOW_MOBILE_DEMO_BUILD !== 'true') {
  fail('VITE_MOBILE_DEMO=true is enabled. Set ALLOW_MOBILE_DEMO_BUILD=true only for an intentional demo APK.')
}

console.log(`[OK] Mobile native API URL is configured from ${loadedFiles.join(', ')}: ${apiBaseUrl}`)
