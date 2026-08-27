const parseJsonMaybe = (value: unknown): unknown => {
  if (typeof value !== 'string') return value
  const trimmed = value.trim()
  if (!trimmed) return []
  try {
    return JSON.parse(trimmed)
  } catch {
    return trimmed
  }
}

const pushUrl = (result: string[], value: unknown) => {
  if (typeof value !== 'string') return
  const trimmed = value.trim()
  if (trimmed) result.push(trimmed)
}

const collectUrls = (result: string[], value: unknown) => {
  const parsed = parseJsonMaybe(value)
  if (Array.isArray(parsed)) {
    parsed.forEach(item => collectUrls(result, item))
    return
  }
  if (parsed && typeof parsed === 'object') {
    Object.values(parsed as Record<string, unknown>).forEach(item => collectUrls(result, item))
    return
  }
  if (typeof parsed === 'string' && parsed.includes(',')) {
    parsed.split(',').forEach(item => pushUrl(result, item))
    return
  }
  pushUrl(result, parsed)
}

export const parseSubmissionImageUrls = (detail: Record<string, unknown> | null | undefined): string[] => {
  const urls: string[] = []
  collectUrls(urls, detail?.imageUrls)
  collectUrls(urls, detail?.imageUrlsJson)
  return Array.from(new Set(urls))
}
