import request from '@/utils/request'

export type VideoKnowledgeSegment = {
  id?: number
  chapterId?: number
  startSecond: number
  endSecond: number
  knowledgeName: string
  description?: string
  difficulty?: string
  sortOrder?: number
}

export type VideoLearningEventType =
  | 'play'
  | 'pause'
  | 'resume'
  | 'seek_forward'
  | 'seek_backward'
  | 'rate_change'
  | 'heartbeat'
  | 'ended'
  | 'intervention_shown'
  | 'intervention_clicked'

export type VideoLearningEventPayload = {
  eventType: VideoLearningEventType
  segmentId?: number | null
  fromSecond?: number | null
  toSecond?: number | null
  durationSecond?: number | null
  playbackRate?: number | null
  extraJson?: string
}

export type VideoInterventionResult = {
  triggered: boolean
  riskLevel: string
  segmentId?: number
  knowledgeName?: string
  behaviorSummary?: string
  suggestedPrompt?: string
}

export type VideoLearningProfile = {
  days: number
  conclusion: string
  totalEvents: number
  weakPointCount: number
  totalRewatchCount: number
  totalSkipCount: number
  totalPauseSeconds: number
  totalInterventionCount: number
  highSpeedEventCount: number
  latestIntervention?: {
    eventTime: string
    courseName: string
    chapterTitle: string
    knowledgeName: string
  } | null
  weakPoints: Array<{
    segmentId: number
    courseName: string
    chapterTitle: string
    knowledgeName: string
    difficulty: string
    rewatchCount: number
    pauseSeconds: number
    skipCount: number
    interventionCount: number
    conclusion: string
    behaviorDetails?: Array<{
      eventType: string
      label: string
      timeRange: string
      fromSecond?: number
      toSecond?: number
      durationSecond?: number
      eventTime: string
    }>
  }>
}

export type VideoTimelineAnalysisTask = {
  taskId: number
  chapterId: number
  status: 'pending' | 'running' | 'succeeded' | 'failed'
  errorMessage?: string
  createTime?: string
  startedAt?: string
  finishedAt?: string
  segments?: VideoKnowledgeSegment[]
}

export function fetchChapterSegments(chapterId: number | string) {
  return request.get<VideoKnowledgeSegment[], VideoKnowledgeSegment[]>(`/chapter/${chapterId}/segments`)
}

export function saveChapterSegments(chapterId: number | string, segments: VideoKnowledgeSegment[]) {
  return request.post<boolean, boolean>(`/chapter/${chapterId}/segments/save`, { segments })
}

export function startChapterTimelineAnalysis(chapterId: number | string) {
  return request.post<number, number>(`/chapter/${chapterId}/segments/ai-analysis/start`)
}

export function fetchLatestChapterTimelineAnalysisTask(chapterId: number | string) {
  return request.get<VideoTimelineAnalysisTask | null, VideoTimelineAnalysisTask | null>(
    `/chapter/${chapterId}/segments/ai-analysis/latest`
  )
}

export function fetchChapterTimelineAnalysisTask(chapterId: number | string, taskId: number | string) {
  return request.get<VideoTimelineAnalysisTask, VideoTimelineAnalysisTask>(
    `/chapter/${chapterId}/segments/ai-analysis/${taskId}`
  )
}

export function startVideoLearningSession(payload: { courseId: number | string; chapterId: number | string }) {
  return request.post<number, number>('/video-learning/session/start', payload, {
    skipErrorToast: true
  })
}

export function reportVideoLearningEvents(payload: {
  sessionId: number
  events: VideoLearningEventPayload[]
}) {
  return request.post<boolean, boolean>('/video-learning/events/batch', payload, {
    skipErrorToast: true
  })
}

export function checkVideoIntervention(payload: {
  sessionId: number
  segmentId?: number | null
  latestEventType?: VideoLearningEventType
}) {
  return request.post<VideoInterventionResult, VideoInterventionResult>(
    '/video-learning/intervention/check',
    payload,
    {
      skipErrorToast: true
    }
  )
}

export function fetchVideoLearningProfile(params: {
  classId: number | string
  studentId: number | string
  days?: number
}) {
  return request.get<VideoLearningProfile, VideoLearningProfile>('/video-learning/student/profile', {
    params,
    skipErrorToast: true
  })
}
