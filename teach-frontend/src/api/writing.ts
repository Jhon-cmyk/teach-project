import request from '../utils/request'
import { getAuthToken } from '../utils/authStorage'

const getBaseURL = () =>
  (request as any).defaults?.baseURL?.replace(/\/$/, '') ?? 'http://localhost:8820/api'

export type WritingSavePayload = {
  id?: number
  title: string
  content: string
  type?: 'writing'
}

export type WritingDocument = WritingSavePayload & {
  id: number
  teacherId?: number
  createTime?: string
  updateTime?: string
}

export type WritingStreamOptions = {
  signal?: AbortSignal
}

export type WritingAiOperation = 'polish' | 'expand' | 'rewrite' | 'continue' | 'summarize'

// 文档 CRUD（复用 ai_resource 接口）
export const getWritingList = () =>
  request.get<WritingDocument[], WritingDocument[]>('/ai/resource/list', {
    params: { type: 'writing' },
  })

export const saveWriting = (data: WritingSavePayload) =>
  request.post('/ai/resource/save', { ...data, type: 'writing' }) as any

export const deleteWriting = (id: number) =>
  request.post(`/ai/resource/delete/${id}`) as any

export const updateWriting = (data: WritingSavePayload) =>
  request.post('/ai/resource/update', data) as any

// AI 文章生成 — SSE 流式
export const streamGenerateArticle = (prompt: string, options: WritingStreamOptions = {}) => {
  const token = getAuthToken()
  const baseURL = getBaseURL()
  return fetch(`${baseURL}/ai/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    credentials: 'include',
    signal: options.signal,
    body: JSON.stringify({ question: prompt, type: 'article' }),
  })
}

// AI 内联操作 — SSE 流式
export const streamWritingAi = (text: string, operation: WritingAiOperation | string) => {
  const token = getAuthToken()
  const baseURL = getBaseURL()
  return fetch(`${baseURL}/ai/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    credentials: 'include',
    body: JSON.stringify({ question: text, type: 'writing', operation }),
  })
}
