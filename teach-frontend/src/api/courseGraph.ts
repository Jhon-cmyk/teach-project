import request from '@/utils/request'

// ========================
//  数据类型定义
// ========================
export interface GraphNodeDTO {
  id: string
  parentId: string | null
  name: string
  category: string
  symbolSize: number
  description?: string
  learnUrl?: string
  learningContent?: string
  difficulty?: string
  importance?: string
  estimatedHours?: number
  teachingWeek?: number
  commonMistakes?: string[]
  teachingTips?: string[]
  isCore?: boolean
  isKeyPoint?: boolean
  resourceCount?: number
  exerciseCount?: number
  analysisHeatLevel?: string
  weaknessLevel?: string
  recommendedForVisual?: boolean
  recommendedForAnalysis?: boolean
  recommendedForCommunityDesk?: boolean
  analysisSummary?: string
  communityHotLevel?: string
  pendingCommunityCount?: number
  featuredCommunityCount?: number
}

export interface GraphLinkDTO {
  id: number
  source: string
  target: string
  relationType?: string
  description?: string
}

export interface GraphCategoryDTO {
  id: string
  name: string
}

export interface GraphDataDTO {
  nodes: GraphNodeDTO[]
  links: GraphLinkDTO[]
  categories: GraphCategoryDTO[]
}

// ========================
//  API 函数
// ========================

/**
 * 获取课程图谱主数据（节点、连线、分类）
 */
export function fetchGraphData(): Promise<GraphDataDTO> {
  return request.get('/teacher/course-graph/data') as Promise<GraphDataDTO>
}

/**
 * 创建新节点
 */
export function createNode(payload: Partial<GraphNodeDTO>): Promise<GraphNodeDTO> {
  return request.post('/teacher/course-graph/node/create', payload) as Promise<GraphNodeDTO>
}

/**
 * 更新节点
 */
export function updateNode(payload: Partial<GraphNodeDTO> & { id: string }): Promise<GraphNodeDTO> {
  return request.post('/teacher/course-graph/node/update', payload) as Promise<GraphNodeDTO>
}

/**
 * 删除节点（级联删除子孙和连线）
 */
export function deleteNode(id: string): Promise<boolean> {
  return request.post('/teacher/course-graph/node/delete', { id }) as Promise<boolean>
}

/**
 * 创建连线
 */
export function createLink(payload: {
  source: string
  target: string
  description?: string
  relationType?: string
}): Promise<GraphLinkDTO> {
  return request.post('/teacher/course-graph/link/create', payload) as Promise<GraphLinkDTO>
}

/**
 * 删除连线
 */
export function deleteLink(id: number): Promise<boolean> {
  return request.post('/teacher/course-graph/link/delete', { id }) as Promise<boolean>
}

/**
 * 导入默认图谱数据
 */
export function seedDefaultGraph(): Promise<GraphDataDTO> {
  return request.post('/teacher/course-graph/seed-default') as Promise<GraphDataDTO>
}

/**
 * 绑定学习活动到知识点
 */
export function bindNodeActivity(nodeId: string, activityType: string, activityId: number): Promise<void> {
  return request.post('/teacher/course-graph/node/bind-activity', null, {
    params: { nodeId, activityType, activityId },
  }) as Promise<void>
}

/**
 * 解绑学习活动
 */
export function unbindNodeActivity(activityId: number): Promise<boolean> {
  return request.post('/teacher/course-graph/node/unbind-activity', null, {
    params: { activityId },
  }) as Promise<boolean>
}

/**
 * 查询知识点绑定的学习活动
 */
export function listNodeActivities(nodeId: string): Promise<any[]> {
  return request.get('/teacher/course-graph/node/activities', {
    params: { nodeId },
  }) as Promise<any[]>
}

/**
 * 获取单个图谱节点详情
 */
export function fetchNodeDetail(nodeId: string): Promise<GraphNodeDTO> {
  return request.get(`/teacher/course-graph/node/${nodeId}`) as Promise<GraphNodeDTO>
}

/**
 * 获取当前节点资源聚焦
 */
export function fetchNodeResourceFocus(nodeId: string): Promise<any> {
  return request.get('/teacher/course-graph/resource-focus', { params: { nodeId } })
}

/**
 * 获取当前节点分析聚焦（统计）
 */
export function fetchNodeAnalysisFocus(nodeId: string): Promise<any> {
  return request.get('/teacher/course-graph/analysis-focus', { params: { nodeId } })
}

/**
 * 查询知识点关联的题库（编程题+随堂测验）
 */
export function fetchNodeQuizzes(nodeId: string): Promise<any[]> {
  return request.get('/teacher/course-graph/node/quizzes', { params: { nodeId } })
}

/**
 * 查询知识点关联的资料（教案+交互课件）
 */
export function fetchNodeMaterials(nodeId: string): Promise<any[]> {
  return request.get('/teacher/course-graph/node/materials', { params: { nodeId } })
}
