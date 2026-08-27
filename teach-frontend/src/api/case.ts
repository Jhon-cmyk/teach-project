import request from '../utils/request'

export type TeachingCaseItem = {
  id: number
  teacherId: number
  title: string
  category: string
  difficulty: string
  courseName: string
  pdfUrl: string
  sourceCaseId?: number
  scope?: string
  status?: string
  sourceUrl?: string
  sourceName?: string
  summary?: string
  keywords?: string
  materialJson?: string
  structureJson?: string
  previewText?: string
  previewType?: string
  relevanceScore?: number
  crawlKeyword?: string
  crawlTime?: string
  reviewTime?: string
  reviewerId?: number
  createTime: string
  updateTime: string
}

export type TeachingCasePageParams = {
  keyword?: string
  category?: string
  difficulty?: string
  current?: number
  pageSize?: number
}

export type TeachingCasePage = {
  records: TeachingCaseItem[]
  total: number
  current: number
  size: number
}

export type ImportTeachingCasePayload = {
  title: string
  category: string
  difficulty: string
  courseName: string
  pdfUrl: string
}

export type RecommendTeachingCasePayload = {
  subject?: string
  grade?: string
  topic?: string
  lessonType?: string
  courseName?: string
}

export type RecommendedTeachingCaseItem = Partial<TeachingCaseItem> & {
  id: number
  title: string
  materialCount?: number
  matchScore?: number
  matchReason?: string
  matchLevel?: 'precise' | 'evidence' | 'related' | 'fallback'
  evidenceScore?: number
  topicEvidenceScore?: number
  evidenceSnippet?: string
  evidenceTitle?: string
}

export type AdminTeachingCasePageParams = {
  current?: number
  size?: number
  status?: string
  keyword?: string
}

export type CrawlTeachingCasePayload = {
  keyword?: string
  sourceUrl?: string
}

export type AdminImportTeachingCasePayload = ImportTeachingCasePayload & {
  sourceUrl?: string
  sourceName?: string
  summary?: string
  keywords?: string
  status?: string
}

export type SavePlatformCaseResult = {
  id: number
  alreadySaved: boolean
}

export type TeachingCasePreviewDetail = {
  id: number
  title: string
  category?: string
  difficulty?: string
  courseName?: string
  summary?: string
  previewText?: string
  previewHtml?: string
  previewType?: string
  sourceName?: string
  sourceUrl?: string
  materialJson?: string
  pdfUrl?: string
  canOpenDocument?: boolean
  imageMaterials?: TeachingCaseAssetItem[]
}

export type TeachingCaseAssetItem = {
  id: number
  caseId: number
  type: string
  url: string
  title?: string
  caption?: string
  context?: string
  sortOrder?: number
  width?: number
  height?: number
  source?: string
}

export const getTeachingCaseList = (params?: { keyword?: string; category?: string }) => {
  return request.get<TeachingCaseItem[], TeachingCaseItem[]>('/teaching-case/list', { params })
}

export const getTeachingCasePage = (params: TeachingCasePageParams) => {
  return request.get<TeachingCasePage, TeachingCasePage>('/teaching-case/page', { params })
}

export const getTeachingCasePreviewUrl = (id: number) => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
  return `${baseUrl}/teaching-case/preview/${id}`
}

export const getTeachingCasePreviewDetail = (id: number) => {
  return request.get<TeachingCasePreviewDetail, TeachingCasePreviewDetail>('/teaching-case/preview-detail', {
    params: { id },
  })
}

export const importTeachingCase = (data: ImportTeachingCasePayload) => {
  return request.post<number, number>('/teaching-case/import', data, {
    successMessage: '案例导入成功',
  })
}

export const recommendTeachingCases = (data: RecommendTeachingCasePayload) => {
  return request.post<RecommendedTeachingCaseItem[], RecommendedTeachingCaseItem[]>('/teaching-case/recommend', data, {
    skipSuccessToast: true,
  })
}

export const savePlatformTeachingCase = (id: number) => {
  return request.post<SavePlatformCaseResult, SavePlatformCaseResult>(`/teaching-case/save-platform/${id}`, {}, {
    skipSuccessToast: true,
  })
}

export const getSavedPlatformTeachingCaseIds = () => {
  return request.get<number[], number[]>('/teaching-case/saved-platform-ids', {
    skipErrorToast: true,
  })
}

export const getAdminTeachingCasePage = (params: AdminTeachingCasePageParams) => {
  return request.get<TeachingCasePage, TeachingCasePage>('/admin/teaching-case/page', { params })
}

export const crawlAdminTeachingCases = (data: CrawlTeachingCasePayload) => {
  return request.post<TeachingCaseItem[], TeachingCaseItem[]>('/admin/teaching-case/crawl', data, {
    successMessage: '采集完成，已进入待审核列表',
  })
}

export const importAdminTeachingCase = (data: AdminImportTeachingCasePayload) => {
  return request.post<number, number>('/admin/teaching-case/import', data, {
    successMessage: '平台案例已上传',
  })
}

export const updateAdminTeachingCase = (data: Partial<TeachingCaseItem> & { id: number }) => {
  return request.post<boolean, boolean>('/admin/teaching-case/update', data, {
    successMessage: '案例信息已更新',
  })
}

export const approveAdminTeachingCase = (id: number) => {
  return request.post<boolean, boolean>(`/admin/teaching-case/approve/${id}`, {}, {
    successMessage: '案例已发布到平台库',
  })
}

export const rejectAdminTeachingCase = (id: number) => {
  return request.post<boolean, boolean>(`/admin/teaching-case/reject/${id}`, {}, {
    successMessage: '案例已驳回',
  })
}

export const offlineAdminTeachingCase = (id: number) => {
  return request.post<boolean, boolean>(`/admin/teaching-case/offline/${id}`, {}, {
    successMessage: '案例已下架',
  })
}

export const rebuildAdminTeachingCaseIndex = () => {
  return request.post<boolean, boolean>('/admin/teaching-case/rebuild-index', {}, {
    successMessage: '平台案例索引已重建',
  })
}

export const deleteTeachingCase = (id: number) => {
  return request.post<boolean, boolean>(`/teaching-case/delete/${id}`, {}, {
    successMessage: '删除成功',
  })
}

export const getTeachingCaseDetail = (id: number) => {
  return request.get<TeachingCaseItem, TeachingCaseItem>('/teaching-case/detail', {
    params: { id },
  })
}
