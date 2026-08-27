import request from '@/utils/request'

export type LearningEventType =
  | 'resource_click'
  | 'video_watch'
  | 'video_pause'
  | 'video_rewatch'
  | 'comment_view'
  | 'comment_post'
  | 'ai_question'
  | 'practice_start'
  | 'practice_submit'
  | 'wrong_question_review'

export type LearningEventPayload = {
  courseId?: number | string | null
  chapterId?: number | string | null
  resourceId?: number | string | null
  resourceType?: string
  knowledgeName?: string
  eventType: LearningEventType
  durationSecond?: number | null
  score?: number | null
  correct?: number | null
  extraJson?: string
}

export function reportLearningEvents(events: LearningEventPayload[]) {
  return request.post<boolean, boolean>('/learning-events/batch', { events }, { skipErrorToast: true })
}

export type StudentLearningProfile = {
  days: number
  preference?: {
    dominantType: string
    summary: string
    videoScore: number
    textScore: number
    practiceScore: number
    discussionScore: number
    aiScore: number
    resourceScore: number
  }
  weakPoints: Array<{
    knowledgeName: string
    courseId?: number
    chapterId?: number
    masteryScore: number
    status: string
    evidenceSummary: string
  }>
  wrongQuestions: Array<{
    submissionId: number
    assignmentId?: number
    courseId?: number
    chapterId?: number
    detailId?: number
    assignmentTitle: string
    questionNo: string
    questionType: string
    stemSnapshot: string
    studentAnswer: string
    aiComment: string
    actionUrl?: string
    actionLabel?: string
  }>
  recommendations: Array<{
    id: number
    courseId?: number
    courseName?: string
    coverImg?: string
    resourceId?: number
    resourceType: string
    resourceTitle: string
    knowledgeName: string
    recommendationReason: string
    practiceSuggestion: string
    recommendationSource?: string
    status: string
    actionType?: string
    actionUrl?: string
    actionLabel?: string
    shortReason?: string
  }>
  advices: Array<{
    title: string
    body: string
    tone: 'primary' | 'success' | 'warning' | 'danger'
  }>
  insight?: {
    title: string
    body: string
    riskLevel: 'low' | 'medium' | 'high' | string
    riskLabel: string
    overallScore: number
    weakPointCount: number
    wrongQuestionCount: number
    confidence: number
    confidenceLabel: string
    trendLabel: string
    recentActivityCount: number
  }
  actionPlans?: Array<{
    title: string
    target: string
    reason: string
    actionType: string
    actionText: string
    actionUrl?: string
    minutes: number
    priority: number
  }>
  evidenceItems?: Array<{
    label: string
    value: string
    detail: string
    tone: 'primary' | 'success' | 'warning' | 'danger' | 'muted' | string
  }>
}

export function fetchStudentLearningProfile(params: {
  classId: number | string
  studentId: number | string
  days?: number
}) {
  return request.get<StudentLearningProfile, StudentLearningProfile>('/teacher/student-learning-profile', {
    params,
    skipErrorToast: true
  })
}

export function fetchMyLearningProfile(params?: { days?: number }) {
  return request.get<StudentLearningProfile, StudentLearningProfile>('/student/learning-profile', {
    params,
    skipErrorToast: true
  })
}

export function fetchMyCourseLearningProfile(params: {
  courseId?: number | string | null
  chapterId?: number | string | null
  days?: number
}) {
  return request.get<StudentLearningProfile, StudentLearningProfile>('/student/course-learning-profile', {
    params,
    skipErrorToast: true
  })
}

export type PersonalPracticeCreateResult = {
  assignmentId: number
  title: string
  sourceType: 'teacher_bank' | 'platform_bank' | 'ai_generated' | string
  sourceLabel: string
  questionCount: number
}

export function createPersonalPractice(payload: {
  courseId?: number | string | null
  chapterId?: number | string | null
  knowledgeName: string
}) {
  return request.post<PersonalPracticeCreateResult, PersonalPracticeCreateResult>(
    '/homework/student/personal-practice/create',
    payload,
    { timeout: 240000 }
  )
}

export function completeRecommendation(id: number | string) {
  return request.post<boolean, boolean>(`/student/recommendations/${id}/complete`, null)
}

export function reopenRecommendation(id: number | string) {
  return request.post<boolean, boolean>(`/student/recommendations/${id}/reopen`, null)
}

export type DailyRecommendationStatus = 'pending' | 'dismissed' | 'completed'

export type DailyRecommendationToday = {
  sessionId?: number
  recommendDate?: string
  status: DailyRecommendationStatus
  shouldPrompt: boolean
  promptType?: 'onboarding_assessment' | 'profile_enrichment' | 'daily_review'
  courseId?: number
  goal?: string
  difficultyText?: string
  availableMinutes?: number
  preferredResourceType?: 'video' | 'text' | 'balanced'
  recommendations: StudentLearningProfile['recommendations']
}

export type DailyRecommendationSubmitPayload = {
  courseId?: number | string | null
  goal?: string
  difficultyText?: string
  learningSituation?: string
  personalityType?: string
  universityName?: string
  developmentGoal?: 'postgraduate' | 'employment' | 'undecided' | ''
  availableMinutes?: number | null
  preferredResourceType?: 'video' | 'text' | 'balanced'
  collectionMode?: 'questionnaire' | 'ai_interview'
  interviewSummary?: string
}

export type DailyRecommendationInterviewMessage = {
  role: 'assistant' | 'user'
  content: string
}

export type StudentLearningContext = {
  universityName: string
  developmentGoal: 'postgraduate' | 'employment' | 'undecided' | ''
  complete: boolean
}

export type DailyRecommendationInterviewResult = {
  reply: string
  ready: boolean
  progress: number
  profile: DailyRecommendationSubmitPayload
  degraded: boolean
}

export function fetchTodayDailyRecommendation() {
  return request.get<DailyRecommendationToday, DailyRecommendationToday>('/student/daily-recommendation/today', {
    skipErrorToast: true
  })
}

export function fetchStudentLearningContext() {
  return request.get<StudentLearningContext, StudentLearningContext>('/student/learning-profile/context', {
    skipErrorToast: true
  })
}

export function updateStudentLearningContext(payload: Pick<StudentLearningContext, 'universityName' | 'developmentGoal'>) {
  return request.post<StudentLearningContext, StudentLearningContext>('/student/learning-profile/context', payload)
}

export function fetchCachedTodayDailyRecommendation() {
  return request.get<DailyRecommendationToday, DailyRecommendationToday>('/student/daily-recommendation/today-cached', {
    skipErrorToast: true
  })
}

export function dismissTodayDailyRecommendation() {
  return request.post<DailyRecommendationToday, DailyRecommendationToday>('/student/daily-recommendation/dismiss', null, {
    skipErrorToast: true
  })
}

export function submitTodayDailyRecommendation(payload: DailyRecommendationSubmitPayload) {
  return request.post<DailyRecommendationToday, DailyRecommendationToday>('/student/daily-recommendation/submit', payload)
}

export function continueDailyRecommendationInterview(payload: {
  messages: DailyRecommendationInterviewMessage[]
  profile: DailyRecommendationSubmitPayload
  courses: Array<{ id: number | string; name: string }>
}) {
  return request.post<DailyRecommendationInterviewResult, DailyRecommendationInterviewResult>(
    '/student/daily-recommendation/interview',
    payload,
    { timeout: 90000, skipErrorToast: true }
  )
}

export function refreshTodayDailyRecommendation() {
  return request.post<DailyRecommendationToday, DailyRecommendationToday>('/student/daily-recommendation/refresh', null, {
    skipErrorToast: true
  })
}
