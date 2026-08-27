import request from '@/utils/request'
import type {
  CourseMindmapData,
  CourseMindmapNode,
  CourseMindmapStatus
} from '@/types/courseMindmap'

function normalizeNode(raw: unknown): CourseMindmapNode | null {
  if (!raw || typeof raw !== 'object') return null

  const node = raw as Record<string, unknown>
  const name = String(node.name || '').trim()
  if (!name) return null

  const children = Array.isArray(node.children)
    ? node.children
      .map((item: unknown) => normalizeNode(item))
      .filter((item): item is CourseMindmapNode => item !== null)
    : []

  return {
    name,
    children
  }
}

function normalizeStatus(raw: unknown): CourseMindmapStatus {
  return raw === 'fallback' ? 'fallback' : 'ready'
}

function normalizeUpdatedAt(raw: unknown): string {
  if (!raw) return ''

  const value = String(raw).trim()
  if (!value) return ''

  if (value.includes('T')) {
    return value.replace('T', ' ').slice(0, 16)
  }

  return value.slice(0, 16)
}

function normalizeMindmap(raw: unknown): CourseMindmapData | null {
  if (!raw) return null

  let source: any = raw

  if (typeof source === 'string') {
    try {
      source = JSON.parse(source)
    } catch {
      return null
    }
  }

  if (source?.mindmapJson && typeof source.mindmapJson === 'string') {
    try {
      const parsedMindmap = JSON.parse(source.mindmapJson)
      source = {
        ...parsedMindmap,
        status: source.status,
        updatedAt: source.updatedAt,
        updated_at: source.updated_at,
        sourceHash: source.sourceHash,
        source_hash: source.source_hash
      }
    } catch {
      return null
    }
  }

  if (source?.mindmap_json && typeof source.mindmap_json === 'string') {
    try {
      const parsedMindmap = JSON.parse(source.mindmap_json)
      source = {
        ...parsedMindmap,
        status: source.status,
        updatedAt: source.updatedAt,
        updated_at: source.updated_at,
        sourceHash: source.sourceHash,
        source_hash: source.source_hash
      }
    } catch {
      return null
    }
  }

  const root = normalizeNode(source?.root)
  if (!root) return null

  return {
    title: String(source?.title || `${root.name}课程思维导图`).trim(),
    root,
    status: normalizeStatus(source?.status),
    updatedAt: normalizeUpdatedAt(source?.updatedAt || source?.updated_at),
    sourceHash: String(source?.sourceHash || source?.source_hash || '').trim()
  }
}

function ensureMindmapData(raw: unknown): CourseMindmapData {
  const parsed = normalizeMindmap(raw)
  if (!parsed) {
    throw new Error('思维导图数据格式异常')
  }
  return parsed
}

export async function getCourseMindmap(courseId: number | string): Promise<CourseMindmapData> {
  const data = await request.get<any, any>('/course/mindmap', {
    params: { courseId }
  })
  return ensureMindmapData(data)
}

export async function regenerateCourseMindmap(courseId: number | string): Promise<CourseMindmapData> {
  const data = await request.post<any, any>('/course/mindmap/regenerate', {
    courseId
  })
  return ensureMindmapData(data)
}
