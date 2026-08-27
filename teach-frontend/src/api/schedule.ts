import request from '@/utils/request'

export interface TeacherSchedule {
  id: number
  teacherId: number
  courseName: string
  linkedCourseId?: number
  className?: string
  teachingPlanId?: number
  weekStart: number
  weekEnd: number
  dayOfWeek: number
  startPeriod: number
  endPeriod: number
  semesterLabel: string
  createTime?: string
  updateTime?: string
}

export interface CurriculumCourseOption {
  id: number
  major: string
  semesterNo: number
  courseName: string
  courseType?: string
  credits?: number
  hours?: number
}

export function fetchTeacherSchedules(semesterLabel?: string): Promise<TeacherSchedule[]> {
  return request.get('/teacher/schedule/list', {
    params: semesterLabel ? { semesterLabel } : {},
    skipErrorToast: true,
  }) as Promise<TeacherSchedule[]>
}

export function addSchedule(data: Omit<TeacherSchedule, 'id' | 'teacherId' | 'createTime' | 'updateTime'>): Promise<number> {
  return request.post('/teacher/schedule/add', data, { skipErrorToast: true }) as Promise<number>
}

export function updateSchedule(data: Omit<TeacherSchedule, 'teacherId' | 'createTime' | 'updateTime'>): Promise<boolean> {
  return request.post('/teacher/schedule/update', data, { skipErrorToast: true }) as Promise<boolean>
}

export function deleteSchedule(id: number): Promise<boolean> {
  return request.post(`/teacher/schedule/delete/${id}`, null, { skipErrorToast: true }) as Promise<boolean>
}

export function fetchCurriculumCourseOptions(className: string, semesterNo?: number): Promise<CurriculumCourseOption[]> {
  return request.get('/teacher/schedule/curriculum-options', {
    params: { className, semesterNo },
    skipErrorToast: true,
  }) as Promise<CurriculumCourseOption[]>
}
