import { mkdtemp, rm } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { spawn, spawnSync } from 'node:child_process'
import net from 'node:net'
import process from 'node:process'

const checks = [
  { route: '/login', texts: ['演示模式已开启'] },
  { route: '/home', texts: ['今日待办', '数据清洗练习 02', '缺失值处理专项练习'] },
  { route: '/courses', texts: ['Python 数据分析入门'] },
  { route: '/courses/1?name=Python%20数据分析入门', texts: ['环境准备与数据读取'] },
  { route: '/homework', texts: ['数据清洗练习 02', '提醒我', '截止 07-08 22:00'] },
  { route: '/homework/1', texts: ['读取提供的 score_sample.csv', '拍照上传', '相册选择', '文件选择'] },
  { route: '/learning', texts: ['节奏稳定，练习复盘还可以更及时'] },
  { route: '/community', texts: ['Pandas 分组统计后怎么保留原始列'] },
  { route: '/profile', texts: ['学习提醒', '保存提醒', '移动端开发边界', '退出登录'] }
]

const host = '127.0.0.1'
const previewCommand = process.platform === 'win32' ? 'npx.cmd' : 'npx'

function findBrowser() {
  const candidates =
    process.platform === 'win32'
      ? [
          'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
          'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
          'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
          'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe'
        ]
      : ['google-chrome', 'chromium', 'chromium-browser', 'microsoft-edge']

  for (const candidate of candidates) {
    if (process.platform === 'win32') {
      if (existsSync(candidate)) return candidate
      continue
    }
    const result = spawnSync('which', [candidate], { encoding: 'utf8' })
    if (result.status === 0) return result.stdout.trim()
  }

  return null
}

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
      if (response.ok) return
      lastError = new Error(`HTTP ${response.status}`)
    } catch (error) {
      lastError = error
    }

    await delay(300)
  }

  throw new Error(`Timed out waiting for vite preview: ${lastError?.message ?? 'unknown error'}`)
}

async function stopPreview(child) {
  if (!child.pid || child.exitCode !== null) return

  if (process.platform === 'win32') {
    spawn('taskkill', ['/pid', String(child.pid), '/t', '/f'], { stdio: 'ignore' })
  } else {
    child.kill('SIGTERM')
  }

  await delay(500)
}

function dumpDom(browserPath, userDataDir, url) {
  const result = spawnSync(
    browserPath,
    [
      '--headless=new',
      '--disable-gpu',
      '--disable-extensions',
      '--disable-background-networking',
      '--no-first-run',
      '--no-default-browser-check',
      '--window-size=390,844',
      '--virtual-time-budget=5000',
      `--user-data-dir=${userDataDir}`,
      '--dump-dom',
      url
    ],
    {
      encoding: 'utf8',
      maxBuffer: 20 * 1024 * 1024
    }
  )

  if (result.status !== 0) {
    throw new Error(`Headless browser failed for ${url}\n${result.stderr || result.stdout}`)
  }

  return result.stdout
}

async function main() {
  const browserPath = findBrowser()
  if (!browserPath) {
    throw new Error('Could not find Microsoft Edge or Google Chrome for render smoke testing')
  }

  const port = await getFreePort()
  const baseUrl = `http://${host}:${port}`
  const logBuffer = []
  const userDataRoot = await mkdtemp(join(tmpdir(), 'smartedu-mobile-smoke-'))

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

    for (const check of checks) {
      const dom = dumpDom(browserPath, await mkdtemp(join(userDataRoot, 'profile-')), `${baseUrl}${check.route}`)
      const missing = check.texts.filter((text) => !dom.includes(text))
      if (missing.length) {
        throw new Error(`Rendered route ${check.route} did not include expected text: ${missing.join(', ')}`)
      }
    }

    console.log(`[OK] Mobile render smoke passed on ${baseUrl}`)
    console.log(`[OK] Browser: ${browserPath}`)
    console.log(`[OK] Rendered routes checked: ${checks.map((item) => item.route).join(', ')}`)
  } finally {
    await stopPreview(child)
    await rm(userDataRoot, { recursive: true, force: true })
  }
}

main().catch((error) => {
  console.error('[!!] Mobile render smoke failed')
  console.error(error instanceof Error ? error.message : error)
  process.exit(1)
})
