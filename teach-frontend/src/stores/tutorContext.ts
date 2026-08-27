import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export type TutorMode = 'explain' | 'hint' | 'check' | 'practice' | 'summary' | 'answer' | 'debug'

export type TutorScene = 'general' | 'course' | 'coding'

interface TutorContext {
  scene: TutorScene
  pageTitle: string
  courseId?: string | number
  courseName?: string
  chapterId?: string | number
  chapterTitle?: string
  problemId?: string | number
  problemTitle?: string
  difficulty?: string
  language?: string
  code?: string
  runResultSummary?: string
}

const limitText = (value: unknown, max = 3000) => {
  const text = String(value ?? '')
  return text.length > max ? `${text.slice(0, max)}\n...（已截断）` : text
}

const summarizeCases = (cases: any[]) => {
  return cases
    .slice(0, 3)
    .map((item, index) => {
      const verdict = item?.passed ? '通过' : '未通过'
      const detail = item?.compileOutput || item?.stderr || item?.statusDescription || item?.actualOutput || ''
      return `样例${index + 1}：${verdict}${detail ? `，${limitText(detail, 500)}` : ''}`
    })
    .join('\n')
}

const summarizeRunResult = (result: any) => {
  if (!result) return ''
  if (Array.isArray(result.testCaseResults) && result.testCaseResults.length > 0) {
    return summarizeCases(result.testCaseResults)
  }
  return limitText(
    result.compileOutput ||
      result.stderr ||
      result.statusDescription ||
      result.stdout ||
      result.status ||
      '',
    1200
  )
}

export const useTutorContextStore = defineStore('tutorContext', () => {
  const context = ref<TutorContext>({
    scene: 'general',
    pageTitle: '学习首页'
  })

  const sceneTitle = computed(() => {
    if (context.value.scene === 'course') {
      return context.value.chapterTitle || context.value.courseName || '课程学习'
    }
    if (context.value.scene === 'coding') {
      return context.value.problemTitle || '编程练习'
    }
    return '通用学习'
  })

  const sceneSubtitle = computed(() => {
    if (context.value.scene === 'course') {
      return [context.value.courseName, context.value.chapterTitle].filter(Boolean).join(' / ') || '正在学习课程内容'
    }
    if (context.value.scene === 'coding') {
      return [context.value.difficulty, context.value.language].filter(Boolean).join(' · ') || '正在完成编程题'
    }
    return '我可以帮你拆解知识点、规划复习或生成练习'
  })

  const requestContext = computed(() => ({ ...context.value }))

  const setGeneralContext = (pageTitle = '学习首页') => {
    context.value = { scene: 'general', pageTitle }
  }

  const setCourseContext = (payload: Partial<TutorContext>) => {
    context.value = {
      ...context.value,
      ...payload,
      scene: 'course',
      pageTitle: payload.pageTitle || '课程学习'
    }
  }

  const setCodingContext = (payload: Partial<TutorContext>) => {
    context.value = {
      ...context.value,
      ...payload,
      scene: 'coding',
      pageTitle: payload.pageTitle || '编程练习',
      code: limitText(payload.code, 3500),
      runResultSummary: limitText(payload.runResultSummary, 1500)
    }
  }

  const updateCodingSnapshot = (payload: {
    language?: string
    code?: string
    runResult?: any
    submitResult?: any
  }) => {
    if (context.value.scene !== 'coding') return
    context.value = {
      ...context.value,
      language: payload.language || context.value.language,
      code: limitText(payload.code ?? context.value.code, 3500),
      runResultSummary: summarizeRunResult(payload.runResult || payload.submitResult)
    }
  }

  return {
    context,
    sceneTitle,
    sceneSubtitle,
    requestContext,
    setGeneralContext,
    setCourseContext,
    setCodingContext,
    updateCodingSnapshot,
    summarizeRunResult
  }
})
