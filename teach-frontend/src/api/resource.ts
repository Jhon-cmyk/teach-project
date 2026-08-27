import request from '@/utils/request'

export type ResourceType = 'video' | 'plan' | 'quiz' | 'anim' | 'micro_video'

export interface ResourceSearchItem {
  id: number
  type: ResourceType
  title: string
  desc: string
  cover?: string
  author: string
  views: number
  date: string
  course: string
  duration?: string
  tags: string[]
  previewText: string
  link?: string
  sourceType?: string
}

export interface ResourceSearchPageData {
  records: ResourceSearchItem[]
  total: number
  current: number
  pageSize: number
  videoCount: number
  planCount: number
  quizCount: number
  animCount: number
  microVideoCount?: number
  supportNotice?: string
}

export interface ResourcePreviewData {
  id: number
  type: ResourceType
  title: string
  author: string
  cover?: string
  videoUrl?: string
  content?: string
  summary?: string
  createTime?: string
}


export interface SearchInternalResourcesParams {
  current?: number
  pageSize?: number
  keyword?: string
  type?: 'all' | 'video' | 'plan' | 'quiz' | 'anim' | 'micro_video'
  sortMode?: 'relevance' | 'newest' | 'popular'
}

export function searchInternalResources(params: SearchInternalResourcesParams) {
  return request.get('/course/search/resources', {
    params,
  }) as Promise<ResourceSearchPageData>
}

export function getResourcePreview(params: { id: number; type: ResourceType }) {
  return request.get('/course/search/resource-preview', {
    params,
  }) as Promise<ResourcePreviewData>
}
