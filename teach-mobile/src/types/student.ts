export interface Course {
  id: number
  name: string
  teacherName?: string
  coverImg?: string
  description?: string
  rating?: number
  categoryId?: number
}

export interface CourseChapter {
  id: number
  courseId: number
  title: string
  description?: string
  videoUrl?: string
  sortOrder?: number
  duration?: number
}

export interface StudyHeatmapDay {
  date: string
  hours: number
  minutes: number
  seconds: number
}

export interface Recommendation {
  id: number
  courseId?: number
  courseName?: string
  resourceTitle: string
  resourceType: string
  knowledgeName: string
  recommendationReason: string
  practiceSuggestion?: string
  actionLabel?: string
  actionUrl?: string
  status?: string
}

export interface LearningInsight {
  title: string
  body: string
  riskLevel: string
  riskLabel: string
  overallScore: number
  weakPointCount: number
  wrongQuestionCount: number
  confidence: number
  confidenceLabel: string
  trendLabel: string
  recentActivityCount: number
}

export interface StudentLearningProfile {
  insight?: LearningInsight
  recommendations: Recommendation[]
  advices: Array<{
    title: string
    body: string
    tone: string
  }>
  actionPlans?: Array<{
    title: string
    target: string
    reason: string
    actionText: string
    minutes: number
    priority: number
  }>
}

export interface CommunityPost {
  id: number
  title: string
  content?: string
  authorName?: string
  answerCount?: number
  viewCount?: number
  createdAt?: string
}

export interface HomeworkPending {
  assignmentId: number
  title: string
  teacherNote?: string
  deadline?: string
  questionCount?: number
  attemptCount?: number
  maxAttemptCount?: number
  allowRedo?: boolean
}

export interface HomeworkHistory {
  assignmentId?: number
  submissionId: number
  title: string
  courseName?: string
  submitStatus?: string
  totalScore?: number
  submitTime?: string
}

export interface HomeworkDetail {
  assignmentId: number
  title: string
  teacherNote?: string
  deadline?: string
  contentSnapshot?: string
  answerMode?: 'online' | 'image' | 'mixed' | string
  assignmentType?: string
  questionCount?: number
  totalScore?: number
  allowRedo?: boolean
  maxAttemptCount?: number
  durationMinutes?: number
  attemptCount?: number
  completed?: boolean
  latestSubmissionId?: number
}
