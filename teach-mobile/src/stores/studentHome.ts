import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  fetchPendingHomework,
  fetchCommunityPosts,
  fetchMyLearningProfile,
  getMyClassCourses,
  getRecommendedCourses,
  getStudyHeatmap,
  userCheckIn
} from '@/api/student'
import type { CommunityPost, Course, HomeworkPending, StudentLearningProfile, StudyHeatmapDay } from '@/types/student'

function asRecords<T>(payload: { records?: T[] } | T[] | undefined): T[] {
  if (Array.isArray(payload)) return payload
  return payload?.records ?? []
}

export const useStudentHomeStore = defineStore('studentHome', () => {
  const courses = ref<Course[]>([])
  const heatmap = ref<StudyHeatmapDay[]>([])
  const profile = ref<StudentLearningProfile | null>(null)
  const posts = ref<CommunityPost[]>([])
  const pendingHomework = ref<HomeworkPending[]>([])
  const loading = ref(false)
  const checkInLoading = ref(false)
  const lastError = ref('')

  const todayMinutes = computed(() => {
    const today = heatmap.value[heatmap.value.length - 1]
    if (!today) return 0
    return Math.round((today.hours * 3600 + today.minutes * 60 + today.seconds) / 60)
  })

  const activeDays = computed(() => heatmap.value.filter((item) => item.hours || item.minutes || item.seconds).length)
  const overallScore = computed(() => profile.value?.insight?.overallScore ?? 72)
  const focusTasks = computed(() => {
    const homeworkTasks = pendingHomework.value.slice(0, 2).map((item) => ({
      id: `homework-${item.assignmentId}`,
      title: item.title,
      meta: item.deadline ? `截止 ${item.deadline.slice(5, 16)}` : '待完成作业',
      tag: '作业',
      to: { name: 'HomeworkDetail', params: { id: item.assignmentId } }
    }))

    const planTasks =
      profile.value?.actionPlans?.slice(0, 2).map((item, index) => ({
        id: `plan-${index}-${item.title}`,
        title: item.title,
        meta: `${item.minutes || 15} 分钟 · ${item.target || item.reason}`,
        tag: '计划',
        to: null
      })) ?? []

    return [...homeworkTasks, ...planTasks].slice(0, 4)
  })

  async function load() {
    loading.value = true
    lastError.value = ''
    const results = await Promise.allSettled([
      getMyClassCourses().catch(() => getRecommendedCourses({ current: 1, size: 6 })),
      getStudyHeatmap(14),
      fetchMyLearningProfile(14),
      fetchCommunityPosts({ current: 1, size: 5 }),
      fetchPendingHomework()
    ])

    if (results[0].status === 'fulfilled') courses.value = asRecords<Course>(results[0].value)
    if (results[1].status === 'fulfilled') heatmap.value = results[1].value
    if (results[2].status === 'fulfilled') profile.value = results[2].value
    if (results[3].status === 'fulfilled') posts.value = asRecords<CommunityPost>(results[3].value)
    if (results[4].status === 'fulfilled') pendingHomework.value = results[4].value

    const failed = results.find((item) => item.status === 'rejected') as PromiseRejectedResult | undefined
    if (failed) {
      lastError.value = failed.reason?.message || '部分数据暂时不可用'
    }
    loading.value = false
  }

  async function checkIn() {
    checkInLoading.value = true
    try {
      const points = await userCheckIn()
      localStorage.setItem('smartedu_mobile_checkin_date', new Date().toISOString().slice(0, 10))
      return points
    } finally {
      checkInLoading.value = false
    }
  }

  return {
    courses,
    heatmap,
    profile,
    posts,
    pendingHomework,
    loading,
    checkInLoading,
    lastError,
    todayMinutes,
    activeDays,
    overallScore,
    focusTasks,
    load,
    checkIn
  }
})
