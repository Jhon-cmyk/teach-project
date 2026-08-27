import request from '../utils/request'
import type {
  AdminClassItem,
  AdminClassListParams,
  AdminClassSavePayload,
  AdminAiResourceItem,
  AdminAiResourceListParams,
  AdminAuditLogItem,
  AdminAuditLogParams,
  AdminCourseFormData,
  AdminCourseListParams,
  AdminDashboardMetrics,
  AdminHealthOverview,
  AdminBackupFile,
  AdminBackupStatus,
  AdminImportBatchItem,
  AdminImportBatchParams,
  AdminImportResult,
  AdminTeacherAssignedCourse,
  AdminTeacherAssignCoursesPayload,
  AdminTeacherTrackingItem,
  AdminTeacherTrackingParams,
  AdminTutorialDetail,
  AdminTutorialFormData,
  AdminTutorialItem,
  AdminTutorialListParams,
  AdminUserListParams,
  AiModelConfigItem,
  AiModelConfigUpdatePayload,
  CourseCategoryItem,
  CourseCategoryListParams,
  PlatformBannerItem,
  PlatformBannerListParams,
  MajorCurriculumCourseItem,
  TeacherRegistrationCodeCreatePayload,
  TeacherRegistrationCodeItem,
  TeacherRegistrationCodeParams,
  UpdateTeacherTitlePayload,
  UpdateUserClassPayload,
  CourseKnowledgeBinding,
  KnowledgeBaseStatus,
  KnowledgeFileItem,
  KnowledgeRepositoryItem,
  KnowledgeUploadResult
} from '@/types/admin'

type PageResult<T> = {
  records: T[]
  total: number
  current: number
  size: number
}

export const getAdminDashboardMetrics = () => {
  return request.get<AdminDashboardMetrics, AdminDashboardMetrics>('/admin/dashboard/metrics')
}

export const getAdminAuditLogList = (params: AdminAuditLogParams) => {
  return request.get<PageResult<AdminAuditLogItem>, PageResult<AdminAuditLogItem>>(
    '/admin/audit-log/list',
    { params }
  )
}

export const getAdminHealthOverview = () => {
  return request.get<AdminHealthOverview, AdminHealthOverview>('/admin/system-health/overview')
}

export const getAdminClassManageList = (params: AdminClassListParams) => {
  return request.get<PageResult<AdminClassItem>, PageResult<AdminClassItem>>('/admin/class/list', { params })
}

export const getAdminClassMajors = () => {
  return request.get<string[], string[]>('/admin/class/majors')
}

export const saveAdminClass = (data: AdminClassSavePayload) => {
  return request.post<boolean, boolean>('/admin/class/save', data)
}

export const deleteAdminClass = (id: number) => {
  return request.post<boolean, boolean>('/admin/class/delete', { id })
}

export const getMajorCurriculumCourses = (params: { major: string; semesterNo?: number }) => {
  return request.get<MajorCurriculumCourseItem[], MajorCurriculumCourseItem[]>(
    '/admin/major-curriculum/list',
    { params }
  )
}

export const saveMajorCurriculumCourse = (data: MajorCurriculumCourseItem) => {
  return request.post<boolean, boolean>('/admin/major-curriculum/save', data)
}

export const deleteMajorCurriculumCourse = (id: number) => {
  return request.post<boolean, boolean>('/admin/major-curriculum/delete', { id })
}

export const importAdminStudents = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<AdminImportResult, AdminImportResult>('/admin/data-transfer/import/students', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const importAdminClasses = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<AdminImportResult, AdminImportResult>('/admin/data-transfer/import/classes', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getAdminImportBatchList = (params: AdminImportBatchParams) => {
  return request.get<PageResult<AdminImportBatchItem>, PageResult<AdminImportBatchItem>>(
    '/admin/data-transfer/import-batches',
    { params }
  )
}

export const getAdminBackupStatus = () => {
  return request.get<AdminBackupStatus, AdminBackupStatus>('/admin/data-transfer/backup/status')
}

export const getAdminBackupList = () => {
  return request.get<AdminBackupFile[], AdminBackupFile[]>('/admin/data-transfer/backup/list')
}

export const createAdminBackup = () => {
  return request.post<AdminBackupFile, AdminBackupFile>('/admin/data-transfer/backup/create')
}

export const restoreAdminBackup = (filename: string) => {
  return request.post<boolean, boolean>('/admin/data-transfer/backup/restore', { filename })
}

export const getAdminTeacherTrackingList = (params: AdminTeacherTrackingParams) => {
  return request.get<PageResult<AdminTeacherTrackingItem>, PageResult<AdminTeacherTrackingItem>>(
    '/admin/teacher-tracking/list',
    { params }
  )
}

export const getAdminTeacherAssignedCourses = (teacherId: number, semester?: string) => {
  return request.get<AdminTeacherAssignedCourse[], AdminTeacherAssignedCourse[]>(
    '/admin/teacher-tracking/assigned-courses',
    { params: { teacherId, semester } }
  )
}

export const assignAdminTeacherCourses = (data: AdminTeacherAssignCoursesPayload) => {
  return request.post<boolean, boolean>('/admin/teacher-tracking/assign-courses', data)
}

export type AdminCourseChapterItem = {
  id?: number
  courseId?: number
  title: string
  videoUrl: string
  sortOrder: number
  animHtml?: string
  createTime?: string
  updateTime?: string
}

export type ReplaceAdminCourseChaptersPayload = {
  courseId: number
  chapterList: AdminCourseChapterItem[]
}

// ==================== 平台课程管理 ====================

export const getAdminCourseList = (params: AdminCourseListParams) => {
  return request.get<PageResult<any>, PageResult<any>>('/admin/course/list', { params })
}

export const addAdminCourse = (data: AdminCourseFormData) => {
  return request.post<number, number>('/admin/course/add', data)
}

export const updateAdminCourse = (data: AdminCourseFormData) => {
  return request.post<boolean, boolean>('/admin/course/update', data)
}

export const deleteAdminCourse = (id: number) => {
  return request.post<boolean, boolean>('/admin/course/delete', { id })
}

/**
 * 获取平台课程分集列表
 */
export const getAdminCourseChapterList = (courseId: number) => {
  return request.get<AdminCourseChapterItem[], AdminCourseChapterItem[]>(
    '/admin/course/chapter/list',
    {
      params: { courseId }
    }
  )
}

/**
 * 整体替换某门平台课程的分集
 */
export const replaceAdminCourseChapters = (data: ReplaceAdminCourseChaptersPayload) => {
  return request.post<boolean, boolean>('/admin/course/chapter/replace', data)
}

// ==================== 用户管理 ====================

export const getAdminUserList = (params: AdminUserListParams) => {
  return request.get<PageResult<any>, PageResult<any>>('/admin/user/list', { params })
}

export const updateAdminUserClass = (data: UpdateUserClassPayload) => {
  return request.post<boolean, boolean>('/admin/user/update-class', data)
}

export const updateAdminTeacherTitle = (data: UpdateTeacherTitlePayload) => {
  return request.post<boolean, boolean>('/admin/user/update-teacher-title', data)
}

export const getTeacherRegistrationCodeList = (params: TeacherRegistrationCodeParams) => {
  return request.get<PageResult<TeacherRegistrationCodeItem>, PageResult<TeacherRegistrationCodeItem>>(
    '/admin/user/teacher-code/list',
    { params }
  )
}

export const createTeacherRegistrationCode = (data: TeacherRegistrationCodeCreatePayload) => {
  return request.post<number, number>('/admin/user/teacher-code/create', data)
}

export const deleteTeacherRegistrationCode = (id: number) => {
  return request.post<boolean, boolean>('/admin/user/teacher-code/delete', { id })
}

export const deleteAdminUser = (id: number) => {
  return request.post<boolean, boolean>('/admin/user/delete', { id })
}

export const getAdminClassList = () => {
  return request.get<AdminClassItem[], AdminClassItem[]>('/class/list')
}

export const getAiModelConfigList = () => {
  return request.get<AiModelConfigItem[], AiModelConfigItem[]>('/admin/model-config/list')
}

export const updateAiModelConfig = (data: AiModelConfigUpdatePayload) => {
  return request.post<boolean, boolean>('/admin/model-config/update', data)
}

// ==================== AI 资源管理 ====================

export const getAdminAiResourceList = (params: AdminAiResourceListParams) => {
  return request.get<PageResult<AdminAiResourceItem>, PageResult<AdminAiResourceItem>>(
    '/admin/ai-resource/list',
    { params }
  )
}

export const getAdminAiResourceDetail = (id: number) => {
  return request.get<AdminAiResourceItem, AdminAiResourceItem>('/admin/ai-resource/detail', {
    params: { id }
  })
}

export const unpublishAdminAiResource = (id: number) => {
  return request.post<boolean, boolean>('/admin/ai-resource/unpublish', { id })
}

// ==================== Banner 管理 ====================

export const getAdminBannerList = (params: PlatformBannerListParams) => {
  return request.get<PageResult<PlatformBannerItem>, PageResult<PlatformBannerItem>>(
    '/admin/banner/list',
    { params }
  )
}

export const addAdminBanner = (data: PlatformBannerItem) => {
  return request.post<number, number>('/admin/banner/add', data)
}

export const updateAdminBanner = (data: PlatformBannerItem) => {
  return request.post<boolean, boolean>('/admin/banner/update', data)
}

export const deleteAdminBanner = (id: number) => {
  return request.post<boolean, boolean>('/admin/banner/delete', { id })
}

// ==================== 分类管理 ====================

export const getAdminCategoryList = (params: CourseCategoryListParams) => {
  return request.get<PageResult<CourseCategoryItem>, PageResult<CourseCategoryItem>>(
    '/admin/category/list',
    { params }
  )
}

export const addAdminCategory = (data: CourseCategoryItem) => {
  return request.post<number, number>('/admin/category/add', data)
}

export const updateAdminCategory = (data: CourseCategoryItem) => {
  return request.post<boolean, boolean>('/admin/category/update', data)
}

export const deleteAdminCategory = (id: number) => {
  return request.post<boolean, boolean>('/admin/category/delete', { id })
}

// ==================== 文件上传（阿里云 OSS） ====================

export const uploadAdminFile = (file: File, dir = 'common') => {
  const formData = new FormData()
  formData.append('file', file)

  return request.post<string, string>('/file/upload', formData, {
    params: { dir },
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getAdminTutorialList = (params: AdminTutorialListParams) => {
  return request.get<PageResult<AdminTutorialItem>, PageResult<AdminTutorialItem>>(
    '/admin/tutorial/list',
    { params }
  )
}

export const getAdminTutorialDetail = (id: number) => {
  return request.get<AdminTutorialDetail, AdminTutorialDetail>('/admin/tutorial/detail', {
    params: { id }
  })
}

export const saveAdminTutorial = (data: AdminTutorialFormData) => {
  return request.post<number, number>('/admin/tutorial/save', data)
}

export const deleteAdminTutorial = (id: number) => {
  return request.post<boolean, boolean>('/admin/tutorial/delete', { id })
}

// ==================== 星火知识库 ====================

export const getKnowledgeBaseStatus = () => {
  return request.get<KnowledgeBaseStatus, KnowledgeBaseStatus>('/admin/knowledge-base/status')
}

export const getKnowledgeRepositories = () => {
  return request.get<KnowledgeRepositoryItem[], KnowledgeRepositoryItem[]>('/admin/knowledge-base/repos')
}

export const createKnowledgeRepository = (data: {
  name: string
  description?: string
  tags?: string
}) => {
  return request.post<KnowledgeRepositoryItem, KnowledgeRepositoryItem>(
    '/admin/knowledge-base/repo/create',
    data
  )
}

export const getCourseKnowledgeBindings = () => {
  return request.get<CourseKnowledgeBinding[], CourseKnowledgeBinding[]>(
    '/admin/knowledge-base/courses'
  )
}

export const bindCourseKnowledgeRepository = (data: {
  courseId: number
  repoId: string
  repoName: string
  keywords?: string
}) => {
  return request.post<CourseKnowledgeBinding, CourseKnowledgeBinding>(
    '/admin/knowledge-base/course/bind',
    data
  )
}

export const unbindCourseKnowledgeRepository = (courseId: number) => {
  return request.post<CourseKnowledgeBinding, CourseKnowledgeBinding>(
    '/admin/knowledge-base/course/unbind',
    { courseId }
  )
}

export const getKnowledgeRepositoryFiles = (repoId: string) => {
  return request.get<KnowledgeFileItem[], KnowledgeFileItem[]>('/admin/knowledge-base/files', {
    params: { repoId }
  })
}

export const uploadKnowledgeFile = (repoId: string, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<KnowledgeUploadResult, KnowledgeUploadResult>(
    '/admin/knowledge-base/file/upload',
    formData,
    {
      params: { repoId },
      headers: { 'Content-Type': 'multipart/form-data' }
    }
  )
}

export const uploadKnowledgeStarterPack = (repoId: string) => {
  return request.post<KnowledgeUploadResult[], KnowledgeUploadResult[]>(
    '/admin/knowledge-base/starter-pack/upload',
    { repoId }
  )
}

export const refreshKnowledgeFileStatuses = (repoId: string, fileIds: string[]) => {
  return request.post<Record<string, string>, Record<string, string>>(
    '/admin/knowledge-base/file/status',
    { repoId, fileIds }
  )
}

export const deleteKnowledgeFile = (repoId: string, fileId: string, fileName: string) => {
  return request.post<boolean, boolean>('/admin/knowledge-base/file/delete', {
    repoId,
    fileId,
    fileName
  })
}
