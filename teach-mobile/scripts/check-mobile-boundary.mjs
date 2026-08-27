import { readdir, readFile } from 'node:fs/promises'
import { dirname, extname, join, relative, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
import process from 'node:process'

const mobileRoot = resolve(fileURLToPath(new URL('..', import.meta.url)))
const srcRoot = join(mobileRoot, 'src')
const generatedRoots = [join(mobileRoot, 'dist'), join(mobileRoot, 'android', 'app', 'src', 'main', 'assets', 'public')]
const webNamePattern = /teach-frontend/i
const sourceExtensions = new Set(['.js', '.mjs', '.ts', '.vue'])
const generatedExtensions = new Set(['.html', '.js', '.mjs', '.css', '.json', '.map'])
const forbiddenWebStorageKeys = ['smartedu_token', 'loginUser', 'user_login']
const importPattern =
  /\b(?:import|export)\s+(?:type\s+)?(?:[\s\S]*?\s+from\s+)?['"]([^'"]+)['"]|import\s*\(\s*['"]([^'"]+)['"]\s*\)|require\s*\(\s*['"]([^'"]+)['"]\s*\)/g

const mode = process.argv.includes('--generated') ? 'generated' : 'source'
const issues = []

function toDisplayPath(file) {
  return relative(mobileRoot, file).split(sep).join('/')
}

function addIssue(file, message) {
  issues.push(`${toDisplayPath(file)} - ${message}`)
}

async function listFiles(dir, extensions) {
  const entries = await readdir(dir, { withFileTypes: true })
  const files = []

  for (const entry of entries) {
    const fullPath = join(dir, entry.name)
    if (entry.isDirectory()) {
      files.push(...(await listFiles(fullPath, extensions)))
    } else if (extensions.has(extname(entry.name))) {
      files.push(fullPath)
    }
  }

  return files
}

async function listExistingFiles(roots, extensions) {
  const files = []

  for (const root of roots) {
    try {
      files.push(...(await listFiles(root, extensions)))
    } catch (error) {
      if (error?.code === 'ENOENT') {
        addIssue(root, 'generated output is missing; run the mobile build and Android sync first')
        continue
      }
      throw error
    }
  }

  return files
}

function assertMobileLocalImport(file, specifier) {
  if (webNamePattern.test(specifier)) {
    addIssue(file, `imports Web frontend path "${specifier}"`)
    return
  }

  if (!specifier.startsWith('.')) return

  const resolvedImport = resolve(dirname(file), specifier)
  const srcRelativePath = relative(srcRoot, resolvedImport)
  if (srcRelativePath.startsWith('..') || resolve(srcRoot, srcRelativePath) !== resolvedImport) {
    addIssue(file, `relative import leaves mobile src: "${specifier}"`)
  }
}

async function checkSourceImports() {
  const files = await listFiles(srcRoot, sourceExtensions)

  for (const file of files) {
    const content = await readFile(file, 'utf8')
    importPattern.lastIndex = 0

    for (const storageKey of forbiddenWebStorageKeys) {
      if (content.includes(storageKey)) {
        addIssue(file, `uses Web frontend storage key "${storageKey}"`)
      }
    }

    let match
    while ((match = importPattern.exec(content))) {
      const specifier = match[1] || match[2] || match[3]
      if (specifier) assertMobileLocalImport(file, specifier)
    }
  }
}

async function checkGeneratedOutputs() {
  const files = await listExistingFiles(generatedRoots, generatedExtensions)

  for (const file of files) {
    const content = await readFile(file, 'utf8')

    if (webNamePattern.test(content)) {
      addIssue(file, 'generated output references teach-frontend')
    }

    for (const storageKey of forbiddenWebStorageKeys) {
      if (content.includes(storageKey)) {
        addIssue(file, `generated output contains Web frontend storage key "${storageKey}"`)
      }
    }
  }
}

async function checkPackageDependencies() {
  const packageFile = join(mobileRoot, 'package.json')
  const content = await readFile(packageFile, 'utf8')
  const pkg = JSON.parse(content)
  const dependencyGroups = ['dependencies', 'devDependencies', 'peerDependencies', 'optionalDependencies']

  for (const group of dependencyGroups) {
    for (const [name, version] of Object.entries(pkg[group] ?? {})) {
      const declaration = `${name}@${version}`
      if (webNamePattern.test(declaration) || String(version).startsWith('file:..')) {
        addIssue(packageFile, `dependency crosses the mobile boundary: ${declaration}`)
      }
    }
  }
}

async function checkTypeScriptPaths() {
  const tsconfigFile = join(mobileRoot, 'tsconfig.app.json')
  const content = await readFile(tsconfigFile, 'utf8')
  const tsconfig = JSON.parse(content)
  const paths = tsconfig.compilerOptions?.paths ?? {}

  for (const [alias, targets] of Object.entries(paths)) {
    for (const target of targets) {
      if (webNamePattern.test(target) || !target.startsWith('./src')) {
        addIssue(tsconfigFile, `path alias "${alias}" points outside mobile src: ${target}`)
      }
    }
  }
}

async function checkViteAlias() {
  const viteConfigFile = join(mobileRoot, 'vite.config.ts')
  const content = await readFile(viteConfigFile, 'utf8')

  if (webNamePattern.test(content)) {
    addIssue(viteConfigFile, 'Vite config references teach-frontend')
  }
  if (!content.includes("new URL('./src', import.meta.url)")) {
    addIssue(viteConfigFile, "Vite '@' alias is not pinned to mobile ./src")
  }
}

if (mode === 'generated') {
  await checkGeneratedOutputs()
} else {
  await checkSourceImports()
  await checkPackageDependencies()
  await checkTypeScriptPaths()
  await checkViteAlias()
}

if (issues.length) {
  console.error(`[!!] Mobile ${mode} boundary check failed`)
  for (const issue of issues) {
    console.error(`- ${issue}`)
  }
  process.exit(1)
}

console.log(`[OK] Mobile ${mode} boundary check passed.`)
