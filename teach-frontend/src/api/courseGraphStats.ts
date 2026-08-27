import request from '@/utils/request'

export function fetchOverviewData(classId?: number) {
  return request.get('/teacher/graph-stats/overview', { params: { classId } }) as Promise<{
    studentCount: number
    kpProgress: { name: string; completion: number; mastery: number }[]
    segments: { label: string; count: number; color: string }[]
    students: { name: string; completion: number; mastery: number; rank: number }[]
    hotspots: { name: string; heatScore: number; questionCount: number }[]
  }>
}

export function fetchCompareData(classA: number, classB: number) {
  return request.get('/teacher/graph-stats/compare', { params: { classA, classB } }) as Promise<{
    classes: { name: string; completion: number; mastery: number }[]
    ranges: { range: string; classA: number; classB: number }[]
  }>
}

export function fetchStudentProfile(classId?: number) {
  return request.get('/teacher/graph-stats/student-profile', { params: { classId } }) as Promise<{
    students: {
      id: string
      name: string
      avatar: string
      completionRate: number
      masteryRate: number
      radar: { indicator: string; value: number; max: number }[]
      studyDays: { date: string; minutes: number; completed: number }[]
    }[]
  }>
}

export function fetchBuildStats() {
  return request.get('/teacher/graph-stats/build') as Promise<{
    build: { total: number; linked: number; tagged: number; unlinked: number; crossLinked: number }
    resources: { type: string; count: number; label: string; icon: string }[]
    attributes: { name: string; difficulty: string; importance: string; tags: string[] }[]
    availableTags: string[]
  }>
}

export function fetchActivityCandidates(courseId?: number) {
  return request.get('/teacher/graph-stats/activities/candidates', { params: { courseId } }) as Promise<{
    homework: { id: number; title: string; type: string }[]
    coding: { id: number; title: string; type: string }[]
  }>
}

export function fetchClassList() {
  return request.get('/class/list') as Promise<{ id: number; name: string; studentCount?: number }[]>
}
