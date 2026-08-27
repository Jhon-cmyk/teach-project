import { spawn } from 'node:child_process'
import net from 'node:net'
import process from 'node:process'

const routes = [
  '/login',
  '/home',
  '/courses',
  '/courses/1',
  '/homework',
  '/homework/1',
  '/learning',
  '/community',
  '/profile'
]

const host = '127.0.0.1'
const previewCommand = process.platform === 'win32' ? 'npx.cmd' : 'npx'

function getFreePort() {
  return new Promise((resolve, reject) => {
    const server = net.createServer()
    server.once('error', reject)
    server.listen(0, host, () => {
      const address = server.address()
      const port = typeof address === 'object' && address ? address.port : null
      server.close(() => {
        if (port) {
          resolve(port)
        } else {
          reject(new Error('Could not allocate a preview port'))
        }
      })
    })
  })
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitForPreview(baseUrl, child, logBuffer) {
  const deadline = Date.now() + 30000
  let lastError = null

  while (Date.now() < deadline) {
    if (child.exitCode !== null) {
      throw new Error(`vite preview exited early with code ${child.exitCode}\n${logBuffer.join('')}`)
    }

    try {
      const response = await fetch(`${baseUrl}/login`)
      if (response.ok) {
        return
      }
      lastError = new Error(`HTTP ${response.status}`)
    } catch (error) {
      lastError = error
    }

    await delay(300)
  }

  throw new Error(`Timed out waiting for vite preview: ${lastError?.message ?? 'unknown error'}`)
}

async function stopPreview(child) {
  if (!child.pid || child.exitCode !== null) {
    return
  }

  if (process.platform === 'win32') {
    spawn('taskkill', ['/pid', String(child.pid), '/t', '/f'], { stdio: 'ignore' })
  } else {
    child.kill('SIGTERM')
  }

  await delay(500)
}

function extractAssetUrls(html) {
  const urls = new Set()
  const assetPattern = /(?:src|href)="([^"]*\/assets\/[^"]+)"/g
  let match = assetPattern.exec(html)

  while (match) {
    urls.add(match[1])
    match = assetPattern.exec(html)
  }

  return [...urls]
}

async function assertOk(url, label) {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`${label} returned HTTP ${response.status}: ${url}`)
  }
  return response
}

async function main() {
  const port = await getFreePort()
  const baseUrl = `http://${host}:${port}`
  const logBuffer = []

  const child = spawn(
    previewCommand,
    ['vite', 'preview', '--host', host, '--port', String(port), '--strictPort'],
    {
      cwd: process.cwd(),
      shell: process.platform === 'win32',
      stdio: ['ignore', 'pipe', 'pipe']
    }
  )

  child.stdout.on('data', (chunk) => logBuffer.push(chunk.toString()))
  child.stderr.on('data', (chunk) => logBuffer.push(chunk.toString()))

  try {
    await waitForPreview(baseUrl, child, logBuffer)

    const routeResults = []
    let indexHtml = ''

    for (const route of routes) {
      const response = await assertOk(`${baseUrl}${route}`, `Route ${route}`)
      const body = await response.text()

      if (!body.includes('<div id="app">')) {
        throw new Error(`Route ${route} did not return the app shell`)
      }

      if (!indexHtml) {
        indexHtml = body
      }

      routeResults.push(route)
    }

    const assetUrls = extractAssetUrls(indexHtml)
    for (const assetUrl of assetUrls) {
      await assertOk(new URL(assetUrl, baseUrl).toString(), `Asset ${assetUrl}`)
    }

    console.log(`[OK] Mobile route smoke passed on ${baseUrl}`)
    console.log(`[OK] Routes checked: ${routeResults.join(', ')}`)
    console.log(`[OK] Assets checked: ${assetUrls.length}`)
  } finally {
    await stopPreview(child)
  }
}

main().catch((error) => {
  console.error('[!!] Mobile route smoke failed')
  console.error(error instanceof Error ? error.message : error)
  process.exit(1)
})
