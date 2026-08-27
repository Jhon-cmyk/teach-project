/**
 * 学习交流模块 - API 层（V1 收尾稳定版）
 */

import type {
  DiscussionItem,
  DiscussionDetail,
  HomeworkQuestionItem,
  FeaturedAnswerItem,
  HomeworkHelpSummary,
  FeaturedAnswersSummary,
  CommunityOverview,
  MyCommunityPostItem,
  MyCommunityReplyItem,
  CommunityNotificationItem,
  CommunityNotificationType
} from '@/types/community'
import { getAuthToken } from '@/utils/authStorage'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'

export class CommunityApiError extends Error {
  code?: number
  status?: number
  rawMessage?: string

  constructor(
    message: string,
    extra: {
      code?: number
      status?: number
      rawMessage?: string
    } = {}
  ) {
    super(message)
    this.name = 'CommunityApiError'
    this.code = extra.code
    this.status = extra.status
    this.rawMessage = extra.rawMessage
  }
}

function buildApiError(
  message: string,
  extra: {
    code?: number
    status?: number
    rawMessage?: string
  } = {}
) {
  return new CommunityApiError(message, extra)
}

export function extractCommunityErrorMessage(error: unknown, fallback = '请求失败') {
  if (!error) return fallback
  if (error instanceof CommunityApiError) return error.message || fallback
  if (error instanceof Error) return error.message || fallback
  return fallback
}

export function isCommunityAuthError(error: unknown) {
  const message = extractCommunityErrorMessage(error, '')
  const status = error instanceof CommunityApiError ? error.status : undefined
  return status === 401
    || /请先登录|未登录|登录失效|无权限|权限不足|仅教师/.test(message)
}

export function isCommunityNotFoundError(error: unknown) {
  const message = extractCommunityErrorMessage(error, '')
  const status = error instanceof CommunityApiError ? error.status : undefined
  return status === 404 || /不存在|未找到/.test(message)
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
  const headers: HeadersInit = {
    ...(options.body && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
    ...(options.headers || {})
  }

  let res: Response
  try {
    res = await fetch(url, {
      credentials: 'include',
      ...options,
      headers
    })
  } catch (error: any) {
    throw buildApiError('网络异常，请检查后端服务是否已启动', {
      status: 0,
      rawMessage: error?.message
    })
  }

  const text = await res.text()
  let json: any = null

  if (text) {
    try {
      json = JSON.parse(text)
    } catch {
      json = null
    }
  }

  const responseMessage =
    json?.message
    || json?.msg
    || (!res.ok ? `请求失败（${res.status}）` : '')

  if (!res.ok) {
    throw buildApiError(responseMessage || '请求失败', {
      status: res.status,
      code: json?.code,
      rawMessage: responseMessage
    })
  }

  if (!json || typeof json !== 'object') {
    throw buildApiError('服务返回格式异常', {
      status: res.status
    })
  }

  if (json.code !== 0) {
    throw buildApiError(json.message || '请求失败', {
      status: res.status,
      code: json.code,
      rawMessage: json.message
    })
  }

  return json.data as T
}

function qs(params: Record<string, any>): string {
  const parts: string[] = []

  for (const [key, val] of Object.entries(params)) {
    if (val !== undefined && val !== null && val !== '') {
      parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(val)}`)
    }
  }

  return parts.length ? `?${parts.join('&')}` : ''
}

export const COURSE_FILTERS = [
  { id: 'all', name: '全部课程' },
  { id: '101', name: '数据结构' },
  { id: '102', name: '计算机网络' },
  { id: '103', name: 'Python 基础' },
  { id: '104', name: '高等数学' },
  { id: '105', name: '数据库原理' }
]

export const HOMEWORK_STATUS_FILTERS = [
  { key: 'all' as const, label: '全部' },
  { key: 'open' as const, label: '待解决' },
  { key: 'resolved' as const, label: '已解决' },
  { key: 'teacher' as const, label: '老师已答' }
]

export async function getCommunityOverview(): Promise<CommunityOverview> {
  return request<CommunityOverview>(`${BASE_URL}/community/overview`)
}

export async function getDiscussionList(params: {
  page?: number
  pageSize?: number
  courseId?: number | string
  sort?: 'latest' | 'hot'
  keyword?: string
}): Promise<{ records: DiscussionItem[]; total: number }> {
  const query = qs({
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10,
    courseId: params.courseId && params.courseId !== 'all' ? params.courseId : undefined,
    sort: params.sort,
    keyword: params.keyword
  })
  const data = await request<any>(`${BASE_URL}/community/discussions${query}`)
  return { records: data.records || [], total: data.total || 0 }
}

export async function getDiscussionDetail(id: number | string): Promise<DiscussionDetail | null> {
  try {
    return await request<DiscussionDetail>(`${BASE_URL}/community/discussions/${id}`)
  } catch (error) {
    if (isCommunityNotFoundError(error)) {
      return null
    }
    throw error
  }
}

export async function getRelatedDiscussions(
  currentId: number | string,
  courseId?: number | string
): Promise<DiscussionItem[]> {
  const query = qs({ courseId, limit: 4 })
  return request<DiscussionItem[]>(`${BASE_URL}/community/discussions/${currentId}/related${query}`)
}

export async function getHomeworkHelpList(params: {
  page?: number
  pageSize?: number
  courseId?: string
  status?: 'all' | 'open' | 'resolved' | 'teacher'
  keyword?: string
}): Promise<{ records: HomeworkQuestionItem[]; total: number }> {
  const query = qs({
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10,
    courseId: params.courseId && params.courseId !== 'all' ? params.courseId : undefined,
    status: params.status,
    keyword: params.keyword
  })
  const data = await request<any>(`${BASE_URL}/community/homework-help${query}`)
  return { records: data.records || [], total: data.total || 0 }
}

export async function getTeacherHomeworkHelpList(params: {
  page?: number
  pageSize?: number
  courseId?: number | string
  status?: 'all' | 'open' | 'resolved' | 'teacher'
  keyword?: string
}): Promise<{ records: HomeworkQuestionItem[]; total: number }> {
  const query = qs({
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10,
    courseId: params.courseId === 'all' ? undefined : params.courseId,
    status: params.status === 'all' ? undefined : params.status,
    keyword: params.keyword
  })

  const data = await request<any>(`${BASE_URL}/community/teacher/homework-help${query}`)
  return { records: data.records || [], total: data.total || 0 }
}

export async function getTeacherDiscussionList(params: {
  page?: number
  pageSize?: number
  courseId?: number | string
  sort?: 'latest' | 'hot'
  keyword?: string
}): Promise<{ records: DiscussionItem[]; total: number }> {
  const query = qs({
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10,
    courseId: params.courseId === 'all' ? undefined : params.courseId,
    sort: params.sort,
    keyword: params.keyword
  })

  const data = await request<any>(`${BASE_URL}/community/teacher/discussions${query}`)
  return { records: data.records || [], total: data.total || 0 }
}

export async function getFeaturedAnswersList(params: {
  page?: number
  pageSize?: number
  courseId?: string
  sort?: 'latest' | 'recommended'
  keyword?: string
}): Promise<{ records: FeaturedAnswerItem[]; total: number }> {
  const query = qs({
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10,
    courseId: params.courseId && params.courseId !== 'all' ? params.courseId : undefined,
    sort: params.sort,
    keyword: params.keyword
  })
  const data = await request<any>(`${BASE_URL}/community/featured-answers${query}`)
  return { records: data.records || [], total: data.total || 0 }
}

export async function getFeaturedAnswersPreview(
  limit = 3,
  params: {
    courseId?: string | number
    sort?: 'latest' | 'recommended'
  } = {}
): Promise<FeaturedAnswerItem[]> {
  const res = await getFeaturedAnswersList({
    page: 1,
    pageSize: Math.max(limit, 3),
    courseId: params.courseId ? String(params.courseId) : undefined,
    sort: params.sort || 'recommended'
  })

  return (res.records || []).slice(0, limit)
}

export async function getFeaturedDiscussionMetaMap(params: {
  pageSize?: number
  courseId?: string | number
  sort?: 'latest' | 'recommended'
} = {}): Promise<Record<string, FeaturedAnswerItem>> {
  const res = await getFeaturedAnswersList({
    page: 1,
    pageSize: params.pageSize || 100,
    courseId: params.courseId ? String(params.courseId) : undefined,
    sort: params.sort || 'recommended'
  })

  const map: Record<string, FeaturedAnswerItem> = {}
  ;(res.records || []).forEach(item => {
    map[String(item.discussionId)] = item
  })

  return map
}

export async function getFeaturedDiscussionMeta(
  discussionId: number | string,
  params: {
    pageSize?: number
    courseId?: string | number
    sort?: 'latest' | 'recommended'
  } = {}
): Promise<FeaturedAnswerItem | null> {
  const map = await getFeaturedDiscussionMetaMap(params)
  return map[String(discussionId)] || null
}

export async function addHomeworkHelp(data: {
  title: string
  content: string
  courseId: number
  courseName: string
}): Promise<number> {
  return request<number>(`${BASE_URL}/community/homework-help/add`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export async function addCommunityReply(data: {
  postId: number | string
  content: string
}): Promise<number> {
  return request<number>(`${BASE_URL}/community/reply/add`, {
    method: 'POST',
    body: JSON.stringify({
      postId: Number(data.postId),
      content: data.content
    })
  })
}

export async function uploadCommunityReplyImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)

  return request<string>(`${BASE_URL}/community/teacher/reply/image`, {
    method: 'POST',
    body: formData
  })
}

export async function deleteCommunityReply(id: number | string): Promise<boolean> {
  return request<boolean>(`${BASE_URL}/community/teacher/reply/delete/${id}`, {
    method: 'POST'
  })
}

export async function resolveCommunityPost(id: number | string): Promise<boolean> {
  return request<boolean>(`${BASE_URL}/community/post/resolve/${id}`, {
    method: 'POST'
  })
}

export async function addFeaturedAnswer(data: {
  postId: number | string
  replyId?: number | string
  teacherId: number | string
  teacherName: string
  excerpt: string
  isRecommended?: boolean
  sortOrder?: number
}): Promise<number> {
  return request<number>(`${BASE_URL}/community/featured/add`, {
    method: 'POST',
    body: JSON.stringify({
      postId: Number(data.postId),
      replyId: data.replyId ? Number(data.replyId) : undefined,
      teacherId: Number(data.teacherId),
      teacherName: data.teacherName,
      excerpt: data.excerpt,
      isRecommended: data.isRecommended ? 1 : 0,
      sortOrder: data.sortOrder ?? 0
    })
  })
}

export type TeacherCommunityPendingSummary = {
  openHomeworkCount: number
  pendingFeatureCount: number
  todayQuestionCount: number
  weeklyFeaturedCount: number
}

export async function getTeacherCommunityPendingSummary(): Promise<TeacherCommunityPendingSummary> {
  const [overviewData, homeworkData, discussionData, featuredData] = await Promise.all([
    getCommunityOverview(),
    getHomeworkHelpList({
      page: 1,
      pageSize: 1,
      status: 'open'
    }),
    getDiscussionList({
      page: 1,
      pageSize: 100,
      sort: 'latest'
    }),
    getFeaturedAnswersList({
      page: 1,
      pageSize: 100,
      sort: 'latest'
    })
  ])

  const homeworkRecords = homeworkData.records || []
  const discussionRecords = discussionData.records || []
  const featuredRecords = featuredData.records || []

  const featuredIdSet = new Set(
    featuredRecords.map(item => String(item.discussionId))
  )

  const pendingFeatureCount = discussionRecords.filter(item => {
    return !!item.isTeacherAnswered && !featuredIdSet.has(String(item.id))
  }).length

  return {
    openHomeworkCount: homeworkData.total || homeworkRecords.length,
    pendingFeatureCount,
    todayQuestionCount: overviewData?.homeworkHelp?.todayQuestionCount || 0,
    weeklyFeaturedCount: overviewData?.featuredAnswers?.weeklySelectedCount || 0
  }
}

export async function getMyCommunityPosts(params: {
  page?: number
  pageSize?: number
  postType?: 'all' | 'discussion' | 'homework'
}): Promise<{ records: MyCommunityPostItem[]; total: number }> {
  const query = qs({
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10,
    postType: params.postType && params.postType !== 'all' ? params.postType : undefined
  })
  const data = await request<any>(`${BASE_URL}/community/my/posts${query}`)
  return { records: data.records || [], total: data.total || 0 }
}

export async function getMyCommunityReplies(params: {
  page?: number
  pageSize?: number
}): Promise<{ records: MyCommunityReplyItem[]; total: number }> {
  const query = qs({
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10
  })
  const data = await request<any>(`${BASE_URL}/community/my/replies${query}`)
  return { records: data.records || [], total: data.total || 0 }
}

export async function getCommunityNotifications(params: {
  page?: number
  pageSize?: number
  isRead?: 0 | 1
  type?: CommunityNotificationType
}): Promise<{ records: CommunityNotificationItem[]; total: number }> {
  const query = qs({
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10,
    isRead: params.isRead,
    type: params.type
  })
  const data = await request<any>(`${BASE_URL}/community/notifications${query}`)
  return { records: data.records || [], total: data.total || 0 }
}

export async function readCommunityNotification(id: number | string): Promise<boolean> {
  return request<boolean>(`${BASE_URL}/community/notifications/read/${id}`, {
    method: 'POST'
  })
}

export async function readAllCommunityNotifications(): Promise<number> {
  return request<number>(`${BASE_URL}/community/notifications/read-all`, {
    method: 'POST'
  })
}

export async function getCommunityNotificationUnreadCount(): Promise<number> {
  return request<number>(`${BASE_URL}/community/notifications/unread-count`)
}
