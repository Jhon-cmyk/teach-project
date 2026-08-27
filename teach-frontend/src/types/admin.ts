export type CourseSourceType = 'platform' | 'teacher'
export type CreatorRole = 'admin' | 'teacher'
export type PublishStatus = 'draft' | 'published' | 'offline'
export type CourseType = 'video' | 'text'
export type UserRole = 'student' | 'teacher' | 'admin'
export type AiResourceType = 'plan' | 'quiz' | 'anim'

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface AdminCourseItem {
  id: number
  name: string
  description?: string
  coverImg?: string
  videoUrl?: string
  type?: string
  teacherId?: number
  teacherName?: string
  sourceType?: string
  creatorId?: number
  creatorRole?: string
  creatorName?: string
  publishStatus?: string
  categoryId?: number
  categoryName?: string
  createTime?: string
  updateTime?: string
}

export interface AdminCourseListParams {
  current?: number
  size?: number
  name?: string
  sourceType?: CourseSourceType | 'all' | ''
  publishStatus?: PublishStatus | ''
}

export interface AdminCourseFormData {
  id?: number
  name: string
  description?: string
  coverImg?: string
  videoUrl?: string
  type: CourseType | string
  publishStatus: PublishStatus | string
  categoryId?: number
}

export interface AdminTutorialItem {
  id: number
  name: string
  description?: string
  coverImg?: string
  createTime?: string
  nodeCount?: number
}

export interface AdminTutorialNodeItem {
  id?: number
  courseId?: number
  title: string
  content?: string
  sortOrder: number
  createTime?: string
}

export interface AdminTutorialListParams {
  current?: number
  size?: number
  name?: string
}

export interface AdminTutorialFormData {
  id?: number
  name: string
  description?: string
  coverImg?: string
  nodes: AdminTutorialNodeItem[]
}

export interface AdminTutorialDetail {
  course: AdminTutorialItem
  nodes: AdminTutorialNodeItem[]
}

export interface AdminUserItem {
  id: number
  userAccount: string
  userName?: string
  userRole: UserRole | string
  teacherTitle?: string
  teacherRegisterCode?: string
  classId?: number | null
  className?: string
  classDisplay?: string
  createTime?: string
}

export interface AdminUserListParams {
  current?: number
  size?: number
  keyword?: string
  role?: UserRole | 'all' | ''
  major?: string
  classId?: number | null
}

export interface UpdateUserClassPayload {
  id: number
  classId?: number | null
}

export interface UpdateTeacherTitlePayload {
  id: number
  teacherTitle: string
}

export interface TeacherRegistrationCodeItem {
  id: number
  registerCode: string
  teacherName?: string
  teacherTitle: string
  status: 'unused' | 'used' | string
  usedBy?: number
  usedTime?: string
  createTime?: string
  updateTime?: string
}

export interface TeacherRegistrationCodeParams {
  current?: number
  size?: number
  status?: string
  keyword?: string
}

export interface TeacherRegistrationCodeCreatePayload {
  registerCode?: string
  teacherName?: string
  teacherTitle: string
}

export interface AdminClassItem {
  id: number
  name: string
  major?: string
  college?: string
  studentCount?: number
}

export interface AiModelConfigItem {
  id: number
  interfaceKey: string
  interfaceName: string
  provider?: string
  endpointUrl: string
  modelName: string
  enabled: number
  remark?: string
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

export interface AiModelConfigUpdatePayload {
  id: number
  interfaceName: string
  provider?: string
  endpointUrl: string
  modelName: string
  enabled: boolean
  remark?: string
}

export interface AdminAiResourceItem {
  id: number
  teacherId: number
  teacherName?: string
  type: AiResourceType | string
  title: string
  content?: string
  paramsJson?: string
  isPublished?: number
  createTime?: string
  updateTime?: string
}

export interface AdminAiResourceListParams {
  current?: number
  size?: number
  title?: string
  type?: AiResourceType | ''
  teacherKeyword?: string
  isPublished?: number | ''
}

export interface PlatformBannerItem {
  id?: number
  title: string
  imageUrl: string
  targetUrl?: string
  sortOrder: number
  isEnabled: number
  createTime?: string
  updateTime?: string
}

export interface PlatformBannerListParams {
  current?: number
  size?: number
  title?: string
}

export interface CourseCategoryItem {
  id?: number
  name: string
  iconUrl: string
  sortOrder: number
  isEnabled: number
  createTime?: string
  updateTime?: string
}

export interface CourseCategoryListParams {
  current?: number
  size?: number
  name?: string
}

export interface AdminDashboardMetrics {
  totalUsers: number
  totalStudents: number
  totalTeachers: number
  totalAdmins: number
  totalCourses: number
  totalPlatformCourses: number
  totalTeacherCourses: number
  publishedPlatformCourses: number
  totalAiResources: number
  publishedAiResources: number
  aiPlanResources: number
  aiQuizResources: number
  aiAnimResources: number
  totalPlatformCases: number
  pendingPlatformCases: number
  approvedPlatformCases: number
  rejectedPlatformCases: number
  offlinePlatformCases: number
  totalBanners: number
  enabledBanners: number
  totalCategories: number
  enabledCategories: number
  totalAssets: number
  recentTeachers?: Array<{
    id: number
    name: string
    account?: string
    userAccount?: string
    avatar?: string
    createTime?: string
    contentCount?: number
  }>
}

export interface AdminAuditLogItem {
  id: number
  adminId: number
  adminAccount: string
  adminName?: string
  module: string
  action: string
  targetType?: string
  targetId?: string
  summary?: string
  requestIp?: string
  createTime?: string
}

export interface AdminAuditLogParams {
  current?: number
  size?: number
  module?: string
  keyword?: string
}

export interface AdminHealthItem {
  key: string
  name: string
  status: 'normal' | 'warning' | 'error' | string
  detail?: string
}

export interface AdminHealthOverview {
  total: number
  abnormalCount: number
  checkedAt: string
  items: AdminHealthItem[]
}

export interface AdminClassListParams {
  current?: number
  size?: number
  keyword?: string
  major?: string
}

export interface AdminClassSavePayload {
  id?: number
  name: string
  major?: string
  college?: string
}

export interface MajorCurriculumCourseItem {
  id?: number
  major: string
  semesterNo: number
  courseName: string
  courseType?: string
  credits?: number
  hours?: number
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

export interface AdminImportResult {
  batchId?: number
  created: number
  skipped: number
  errors: string[]
}

export interface AdminImportBatchItem {
  id: number
  importType: 'students' | 'classes' | string
  fileName: string
  createdCount: number
  skippedCount: number
  errorCount: number
  errorJson?: string
  status: 'success' | 'partial' | 'failed' | string
  adminId?: number
  adminAccount?: string
  adminName?: string
  requestIp?: string
  createTime?: string
}

export interface AdminImportBatchParams {
  current?: number
  size?: number
  importType?: string
}

export interface AdminBackupFile {
  filename: string
  size: number
  sizeText: string
  createTime: string
}

export interface AdminBackupStatus {
  backupDir: string
  backupCount: number
  latestBackup?: AdminBackupFile | null
  restoreEnabled: boolean
}

export interface AdminTeacherTrackingItem {
  id: number
  account: string
  name: string
  createTime?: string
  semester?: string
  courseCount: number
  assignedCourseCount?: number
  aiResourceCount: number
  caseCount: number
  totalContentCount: number
  status: 'configured' | 'not_configured' | string
  lastContentTime?: string
  assignedCourses?: AdminTeacherAssignedCourse[]
}

export interface AdminTeacherTrackingParams {
  current?: number
  size?: number
  keyword?: string
  status?: string
  semester?: string
}

export interface AdminTeacherAssignedCourse {
  id: number
  name: string
  sourceType?: string
  publishStatus?: string
}

export interface AdminTeacherAssignCoursesPayload {
  teacherId: number
  semester?: string
  courseIds: number[]
}

export interface KnowledgeBaseStatus {
  enabled: boolean
  configured: boolean
  baseUrl: string
  appIdConfigured: boolean
  secretConfigured: boolean
  minimumScore: number
  topN: number
}

export interface KnowledgeRepositoryItem {
  repoId: string
  repoName: string
  repoDesc?: string
  repoTags?: string
  createTime?: string
}

export interface KnowledgeFileItem {
  fileId: string
  fileName: string
  fileType?: string
  fileStatus?: string
  extName?: string
  quantity?: number
  expirationStatus?: string
  createTime?: string
  expireTime?: string
}

export interface KnowledgeUploadResult {
  fileId: string
  fileName: string
  parseType?: string
  fileStatus?: string
}

export interface CourseKnowledgeBinding {
  courseId: number
  courseName: string
  sourceType?: string
  teacherName?: string
  publishStatus?: string
  repoId?: string
  repoName?: string
  keywords?: string
  syncStatus: 'empty' | 'processing' | 'ready' | 'failed' | string
  updatedAt?: string
}
