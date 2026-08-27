import { get, post } from '@/utils/request'
import {
  demoChapters,
  demoCommunityPosts,
  demoCourses,
  demoHeatmap,
  demoHomeworkDetail,
  demoHomeworkHistory,
  demoPendingHomework,
  demoProfile,
  demoSubmitHomework,
  isMobileDemo
} from '@/api/mock'
import type {
  CommunityPost,
  Course,
  CourseChapter,
  HomeworkDetail,
  HomeworkHistory,
  HomeworkPending,
  StudentLearningProfile,
  StudyHeatmapDay
} from '@/types/student'

export function getRecommendedCourses(params?: {
  current?: number
  size?: number
  name?: string
  categoryId?: number
}) {
  if (isMobileDemo) {
    const keyword = params?.name?.trim().toLowerCase()
    const records = keyword
      ? demoCourses.filter((course) => `${course.name}${course.teacherName}${course.description}`.toLowerCase().includes(keyword))
      : demoCourses
    return Promise.resolve({ records: records.slice(0, params?.size ?? 8), total: records.length })
  }
  return get<{ records?: Course[]; total?: number } | Course[]>('/course/list/all', {
    params: {
      current: params?.current ?? 1,
      size: params?.size ?? 8,
      ...(params?.name ? { name: params.name } : {}),
      ...(params?.categoryId ? { categoryId: params.categoryId } : {})
    }
  })
}

export function getMyClassCourses() {
  if (isMobileDemo) return Promise.resolve(demoCourses.slice(0, 3))
  return get<Course[]>('/course/list/my-class')
}

export function getCourseChapters(courseId: number | string) {
  if (isMobileDemo) {
    const normalizedCourseId = Number(courseId) || 1
    return Promise.resolve(demoChapters.map((chapter) => ({ ...chapter, courseId: normalizedCourseId })))
  }
  return get<CourseChapter[]>('/chapter/list', { params: { courseId } })
}

export function getStudyHeatmap(days = 30) {
  if (isMobileDemo) return Promise.resolve(demoHeatmap.slice(-days))
  return get<StudyHeatmapDay[]>('/learning/heatmap', { params: { days } })
}

export function userCheckIn() {
  if (isMobileDemo) return Promise.resolve(8)
  return post<number>('/plan/checkin')
}

export function fetchMyLearningProfile(days = 14) {
  if (isMobileDemo) return Promise.resolve(demoProfile)
  return get<StudentLearningProfile>('/student/learning-profile', {
    params: { days }
  })
}

export function fetchMyCourseLearningProfile(params: {
  courseId?: number | string | null
  chapterId?: number | string | null
  days?: number
}) {
  if (isMobileDemo) return Promise.resolve(demoProfile)
  return get<StudentLearningProfile>('/student/course-learning-profile', {
    params: {
      ...(params.courseId ? { courseId: params.courseId } : {}),
      ...(params.chapterId ? { chapterId: params.chapterId } : {}),
      days: params.days ?? 14
    }
  })
}

export function fetchCommunityPosts(params?: { current?: number; size?: number }) {
  if (isMobileDemo) {
    return Promise.resolve({
      records: demoCommunityPosts.slice(0, params?.size ?? 10),
      total: demoCommunityPosts.length
    })
  }
  return get<{ records?: CommunityPost[]; total?: number } | CommunityPost[]>('/community/discussions', {
    params: {
      current: params?.current ?? 1,
      size: params?.size ?? 10
    }
  })
}

export function fetchPendingHomework() {
  if (isMobileDemo) return Promise.resolve(demoPendingHomework)
  return get<HomeworkPending[]>('/homework/student/pending')
}

export function fetchHomeworkHistory() {
  if (isMobileDemo) return Promise.resolve(demoHomeworkHistory)
  return get<HomeworkHistory[]>('/homework/student/history')
}

export function fetchHomeworkDetail(assignmentId: number | string) {
  if (isMobileDemo) {
    const matched = demoPendingHomework.find((item) => String(item.assignmentId) === String(assignmentId))
    return Promise.resolve({
      ...demoHomeworkDetail,
      assignmentId: Number(assignmentId) || demoHomeworkDetail.assignmentId,
      title: matched?.title || demoHomeworkDetail.title,
      teacherNote: matched?.teacherNote || demoHomeworkDetail.teacherNote
    })
  }
  return get<HomeworkDetail>('/homework/student/detail', { params: { assignmentId } })
}

export function submitHomework(payload: {
  assignmentId: number | string
  submissionType: 'online' | 'image' | 'mixed'
  studentAnswerJson: string
  wholePaperImageUrls?: string[]
  questionImageItems?: Array<{ questionNo: string; imageUrls: string[] }>
}) {
  if (isMobileDemo) return demoSubmitHomework()
  return post<number>('/homework/submission/submit', payload, { timeout: 120000 })
}

export function uploadHomeworkImage(file: File) {
  if (isMobileDemo) return Promise.resolve(URL.createObjectURL(file))
  const form = new FormData()
  form.append('file', file)
  return post<string>('/homework/submission/image/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
