<template>
  <div class="exam-page">
    <header class="page-header">
      <div class="header-container">
        <a-button type="link" @click="router.push('/student/dashboard')" class="back-btn">
          <arrow-left-outlined /> 返回主页
        </a-button>

        <div class="title-group">
          <h2>
            <span class="subtitle-text" v-if="assignmentDetail">
              {{ assignmentDetail.title }}
            </span>
          </h2>
        </div>

        <div class="timer-group" v-if="currentStatus === 'pending' && !isSubmitted">
          <clock-circle-outlined />
          <span class="timer-text" :class="{ 'timer-warning': remainingMinutes < 5 }">
            {{ formatRemaining }}
          </span>
        </div>
      </div>
    </header>

    <div class="workspace" v-if="assignmentDetail && currentStatus === 'pending' && !isSubmitted">
      <div class="paper-panel solid-panel scroll-y">
        <div class="panel-header">
          <div class="tag">试卷正文</div>
        </div>
        <div
          class="paper-content markdown-render doc-style"
          v-html="renderMd(paperOnlyText)"
        ></div>
      </div>

      <div class="answer-panel solid-panel scroll-y">
        <div class="panel-header">
          <div class="tag answer-tag">答题卡</div>
          <a-button type="primary" class="submit-btn" @click="submitExam" :loading="submitting">
            交卷
          </a-button>
        </div>

        <div class="smart-answer-sheet" v-if="answerSheet.length > 0">
          <template v-for="(q, idx) in answerSheet" :key="q.uid">
            <div v-if="q.sectionLabel && (idx === 0 || answerSheet[idx - 1].sectionLabel !== q.sectionLabel)" class="sheet-section-header">{{ q.sectionLabel }}</div>
            <div class="sheet-item" :class="{ 'has-error': !hasQuestionImage(q) && (!q.val || (Array.isArray(q.val) && q.val.length === 0)) }">
            <div class="q-label">第 {{ q.num }} 题</div>
            <div class="q-stem" v-if="q.stem">{{ q.stem }}</div>

            <div v-if="canUploadImageForQuestion(q)" class="question-image-box">
              <label class="image-upload-btn small">
                上传本题图片
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  multiple
                  @change="(event) => handleQuestionImageSelect(q.uid, event)"
                />
              </label>
              <div v-if="questionImageMap[q.uid]?.length" class="image-chip-list compact">
                <span v-for="(url, imageIdx) in questionImageMap[q.uid]" :key="url" class="image-chip">
                  <a :href="url" target="_blank" rel="noreferrer">原图 {{ imageIdx + 1 }}</a>
                  <button type="button" @click="removeQuestionImage(q.uid, imageIdx)">×</button>
                </span>
              </div>
            </div>

            <a-radio-group
              v-if="q.type === 'radio'"
              v-model:value="q.val"
              class="custom-options option-list"
            >
              <a-radio
                v-for="opt in (q.options.length ? q.options : [{ label: 'A', text: 'A' }, { label: 'B', text: 'B' }, { label: 'C', text: 'C' }, { label: 'D', text: 'D' }])"
                :key="opt.label"
                :value="opt.label"
              >
                {{ opt.label }}. {{ opt.text }}
              </a-radio>
            </a-radio-group>

            <div v-else-if="q.type === 'checkbox'" class="custom-checkbox-group">
              <span class="multi-hint">（可多选）</span>
              <label
                v-for="opt in (q.options.length ? q.options : [{ label: 'A', text: 'A' }, { label: 'B', text: 'B' }, { label: 'C', text: 'C' }, { label: 'D', text: 'D' }])"
                :key="opt.label"
                class="custom-checkbox-item"
                :class="{ active: Array.isArray(q.val) && q.val.includes(opt.label) }"
              >
                <input
                  type="checkbox"
                  :value="opt.label"
                  :checked="Array.isArray(q.val) && q.val.includes(opt.label)"
                  @change="toggleCheckbox(q, opt.label)"
                  class="native-checkbox"
                />
                <span class="checkbox-indicator"></span>
                <span class="checkbox-text">{{ opt.label }}. {{ opt.text }}</span>
              </label>
            </div>

            <a-radio-group
              v-else-if="q.type === 'judge'"
              v-model:value="q.val"
              class="custom-options"
            >
              <a-radio value="正确">正确</a-radio>
              <a-radio value="错误">错误</a-radio>
            </a-radio-group>

            <div v-else-if="q.type === 'fill'" class="fill-blanks-wrap">
              <template v-if="q.blankSegments && q.blankSegments.length > 1">
                <template v-for="(seg, idx) in q.blankSegments" :key="idx">
                  <span class="fill-text-seg">{{ seg }}</span>
                  <a-input
                    v-if="idx < q.blankSegments.length - 1"
                    v-model:value="(q.val as any[])[idx]"
                    class="inline-fill-input"
                    placeholder="请填写"
                    size="small"
                  />
                </template>
              </template>
              <a-input
                v-else
                v-model:value="q.val"
                placeholder="请输入填空答案"
                class="custom-input"
              />
            </div>

            <RichTextEditor
              v-else
              v-model:modelValue="(q.val as string)"
              placeholder="请输入解答过程..."
            />
          </div>
          </template>
        </div>

        <div v-else class="editor-wrapper">
          <RichTextEditor v-model:modelValue="fallbackAnswer" placeholder="请在此处输入解答过程..." />
        </div>
      </div>
    </div>

    <!-- 已提交待批阅 -->
    <div v-else-if="isSubmitted && !reportVO" class="empty-box solid-panel">
      <div class="submitted-icon">
        <check-circle-filled style="font-size: 64px; color: #2563EB;" />
      </div>
      <h2>试卷已提交</h2>
      <p>等待教师批阅，请耐心等待</p>
      <a-button @click="router.push('/student/dashboard')">返回主页</a-button>
    </div>

    <!-- 批阅完成显示报告 -->
    <div v-else-if="reportVO" class="workspace">
      <div class="paper-panel solid-panel scroll-y" style="flex: 1;">
        <div class="panel-header">
          <div class="tag">试卷正文</div>
        </div>
        <div
          class="paper-content markdown-render doc-style"
          v-html="renderMd(paperOnlyText)"
        ></div>
      </div>

      <div class="answer-panel solid-panel scroll-y" style="flex: 0.8;">
        <div class="panel-header">
          <div class="tag answer-tag">批阅结果</div>
        </div>
        <div class="report-section">
          <div class="score-summary" v-if="reportVO?.submission">
            <div class="score-big">
              <span class="score-label">总分</span>
              <span class="score-value">{{ reportVO.submission.totalScore ?? '--' }}</span>
            </div>
            <div class="teacher-remark" v-if="reportVO.submission.teacherRemark">
              <strong>教师评语：</strong>
              <p>{{ reportVO.submission.teacherRemark }}</p>
            </div>
          </div>

            <div class="score-summary-row" v-if="reportVO?.submission">
            <div class="summary-item">
              <span class="summary-label">得分</span>
              <span class="summary-value">{{ reportVO.submission.totalScore ?? '--' }}</span>
              <span class="summary-unit">分</span>
            </div>
            <div class="summary-item" v-if="reportVO.submission.correctCount != null">
              <span class="summary-label">正确</span>
              <span class="summary-value correct">{{ reportVO.submission.correctCount }}</span>
              <span class="summary-unit">题</span>
            </div>
            <div class="summary-item" v-if="reportVO.submission.wrongCount != null">
              <span class="summary-label">错误</span>
              <span class="summary-value wrong">{{ reportVO.submission.wrongCount }}</span>
              <span class="summary-unit">题</span>
            </div>
          </div>

          <div class="answer-records">
            <div v-for="d in reportVO?.details || []" :key="d.id" class="record-item">
              <div class="record-header">
                <span class="record-q-title">第 {{ d.questionNo }} 题</span>
                <span class="record-score" :class="(d.score ?? 0) > 0 ? 'score-pass' : 'score-fail'">
                  {{ d.score ?? '--' }} / {{ d.fullScore ?? '--' }} 分
                </span>
              </div>
              <div class="record-stem" v-if="d.stemSnapshot">{{ d.stemSnapshot }}</div>
              <div class="record-answer">
                <span class="record-label">你的答案：</span>
                <span class="record-value" v-html="d.studentAnswer || '未作答'"></span>
              </div>
              <div v-if="getRecordImageUrls(d).length" class="record-answer-images">
                <a-image
                  v-for="(url, imageIndex) in getRecordImageUrls(d)"
                  :key="`${d.id || d.questionNo}-image-${imageIndex}`"
                  :src="url"
                  :width="120"
                  :height="88"
                  class="record-answer-image"
                  :preview="{ mask: '预览' }"
                />
              </div>
              <div class="record-standard-answer" v-if="d.standardAnswer">
                <span class="record-label">参考答案：</span>
                <span class="record-value">{{ d.standardAnswer }}</span>
              </div>
              <div class="record-ai-comment" v-if="d.aiComment">
                <span class="record-label">批阅：</span>
                <span class="record-value">{{ d.aiComment }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="pageLoading" class="empty-box solid-panel">
      <a-spin size="large" tip="正在加载考试数据..." />
    </div>

    <div v-else class="empty-box solid-panel">
      <h2>考试数据不存在或已过期</h2>
      <a-button @click="router.push('/student/dashboard')">返回主页</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { message, Modal } from 'ant-design-vue'
import { ArrowLeftOutlined, ClockCircleOutlined, CheckCircleFilled } from '@ant-design/icons-vue'
import request from '@/utils/request'
import RichTextEditor from '@/components/RichTextEditor.vue'
import { normalizePaperLine, splitPaperAndAnswers } from '@/utils/paperParser'
import { parseSubmissionImageUrls } from '@/utils/submissionImage'

type QuestionType = 'radio' | 'checkbox' | 'judge' | 'fill' | 'text'

interface QuestionOption {
  label: string
  text: string
}

interface AnswerQuestion {
  num: string
  uid: string
  type: QuestionType
  val: string | string[]
  stem: string
  options: QuestionOption[]
  rawLines: string[]
  blankSegments?: string[]
  sectionLabel?: string  // 题型标题，如"一、单选题"
}

const route = useRoute()
const router = useRouter()
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const SERVER_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')
const md = new MarkdownIt({ breaks: true, html: true })
const renderMd = (text: string) => md.render(text || '')

const normalizeServerAssetUrl = (url?: string) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:image')) return url
  return `${SERVER_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`
}

const getRecordImageUrls = (detail: Record<string, unknown>) =>
  parseSubmissionImageUrls(detail).map(normalizeServerAssetUrl).filter(Boolean)


const assignmentId = ref<number>(0)
const assignmentDetail = ref<any>(null)
const pageLoading = ref(true)

const currentStatus = ref<'pending' | 'completed'>('pending')
const isSubmitted = ref(false)
const submitting = ref(false)
const reportVO = ref<any>(null)

const paperOnlyText = ref('')
const answerSheet = ref<AnswerQuestion[]>([])
const fallbackAnswer = ref('')
const questionImageMap = ref<Record<string, string[]>>({})

// 倒计时
const remainingSeconds = ref(0)
const durationMinutes = ref(0)
let timerInterval: ReturnType<typeof setInterval> | null = null

const remainingMinutes = computed(() => Math.floor(remainingSeconds.value / 60))

const formatRemaining = computed(() => {
  const m = Math.floor(remainingSeconds.value / 60)
  const s = remainingSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const startTimer = (minutes: number) => {
  durationMinutes.value = minutes
  remainingSeconds.value = minutes * 60
  timerInterval = setInterval(() => {
    remainingSeconds.value--
    if (remainingSeconds.value <= 0) {
      clearInterval(timerInterval!)
      timerInterval = null
      autoSubmit()
    }
  }, 1000)
}

const autoSubmit = () => {
  message.warning('考试时间已到，自动交卷')
  submitExam()
}

onMounted(async () => {
  const idParam = route.params.id as string
  assignmentId.value = parseInt(idParam, 10)

  if (isNaN(assignmentId.value)) {
    message.error('考试ID无效')
    pageLoading.value = false
    return
  }

  try {
    const detail = await request.get('/homework/student/detail', {
      params: { assignmentId: assignmentId.value },
    })

    assignmentDetail.value = detail
    parsePaper(assignmentDetail.value?.contentSnapshot || '')

    // 检查是否已有提交
    const history: any[] = await request.get('/exam/student/history', {
      skipErrorToast: true,
    }) || []

    const matched = (Array.isArray(history) ? history : []).find(
      (r: any) => Number(r.assignmentId) === assignmentId.value
    )

    if (matched) {
      isSubmitted.value = true
      if (matched.submitStatus === 'completed') {
        await loadReport(matched.submissionId)
      }
    } else {
      currentStatus.value = 'pending'
      // 启动倒计时
      if ((detail as any).durationMinutes) {
        startTimer((detail as any).durationMinutes)
      }
    }
  } catch (e: any) {
    message.error(e?.message || '网络错误，无法加载考试')
  } finally {
    pageLoading.value = false
  }
})

const loadReport = async (submissionId: number) => {
  try {
    const data = await request.get('/exam/student/report', {
      params: { submissionId },
    })
    reportVO.value = data
    if (!paperOnlyText.value?.trim()) {
      parsePaper((data as any)?.contentSnapshot || assignmentDetail.value?.contentSnapshot || '')
    }
  } catch (e: any) {
    message.error(e?.message || '加载报告失败')
  }
}

const toggleCheckbox = (q: AnswerQuestion, label: string) => {
  const arr = (Array.isArray(q.val) ? q.val : []) as string[]
  if (arr.includes(label)) {
    q.val = arr.filter(v => v !== label)
  } else {
    q.val = [...arr, label]
  }
}

const canUploadImageForQuestion = (q: AnswerQuestion) => q.type === 'fill' || q.type === 'text'

const hasQuestionImage = (q: AnswerQuestion) =>
  canUploadImageForQuestion(q) && (questionImageMap.value[q.uid]?.length || 0) > 0

const uploadExamImage = async (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<string, string>('/homework/submission/image/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  } as any)
}

const handleQuestionImageSelect = async (questionUid: string, event: Event) => {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return
  try {
    const urls: string[] = []
    for (const file of files) {
      urls.push(await uploadExamImage(file))
    }
    questionImageMap.value = {
      ...questionImageMap.value,
      [questionUid]: [...(questionImageMap.value[questionUid] || []), ...urls],
    }
    message.success('本题图片上传成功')
  } finally {
    input.value = ''
  }
}

const removeQuestionImage = (questionUid: string, index: number) => {
  questionImageMap.value = {
    ...questionImageMap.value,
    [questionUid]: (questionImageMap.value[questionUid] || []).filter((_, idx) => idx !== index),
  }
}

const submitExam = async () => {
  let studentAnswerData: any[]

  if (answerSheet.value.length > 0) {
    studentAnswerData = answerSheet.value.map((q, index) => {
      const imageUrls = canUploadImageForQuestion(q) ? (questionImageMap.value[q.uid] || []) : []
      const textAnswer = Array.isArray(q.val) ? q.val.join(', ') : q.val
      const answer = String(textAnswer || '').trim() || (imageUrls.length ? '见图片作答' : '')
      return {
        num: String(index + 1),
        originalQuestionNo: q.num,
        imageKey: q.uid,
        type: q.type,
        stem: q.stem,
        answer,
        ...(imageUrls.length ? { imageUrls } : {}),
      }
    })
  } else {
    studentAnswerData = [
      { num: '1', type: 'text', stem: '全文作答', answer: fallbackAnswer.value },
    ]
  }

  // 检查填空题的各空是否已填写
  const hasEmptyFill = answerSheet.value.some(
    (q) => q.type === 'fill'
      && !hasQuestionImage(q)
      && Array.isArray(q.val)
      && (q.val.length === 0 || q.val.every(v => String(v || '').trim() === ''))
  )
  if (hasEmptyFill) {
    message.warning('请答完卷后再交卷')
    return
  }

  // 检查是否有未作答的题
  const hasEmpty = studentAnswerData.some(
    (q) => (!q.answer || String(q.answer).trim() === '')
      && (!Array.isArray(q.imageUrls) || q.imageUrls.length === 0)
  )
  if (hasEmpty) {
    message.warning('请答完卷后再交卷')
    return
  }

  submitting.value = true
  const questionImageItems = Object.entries(questionImageMap.value)
    .filter(([, urls]) => Array.isArray(urls) && urls.length > 0)
    .map(([imageKey, imageUrls]) => {
      const q = answerSheet.value.find(item => item.uid === imageKey)
      return q && canUploadImageForQuestion(q) ? { questionNo: q.uid, imageUrls } : null
    })
    .filter(Boolean) as Array<{ questionNo: string; imageUrls: string[] }>
  try {
    await request.post('/exam/submission/submit', {
      assignmentId: assignmentId.value,
      submissionType: questionImageItems.length ? 'mixed' : 'online',
      studentAnswerJson: JSON.stringify(studentAnswerData),
      wholePaperImageUrls: [],
      questionImageItems,
    })

    if (timerInterval) {
      clearInterval(timerInterval)
      timerInterval = null
    }

    isSubmitted.value = true
    message.success('交卷成功')
  } catch (e: any) {
    message.error(e?.message || '交卷失败')
  } finally {
    submitting.value = false
  }
}

onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval)
  }
})

// ====== 试卷解析（复用 Homework.vue 逻辑） ======
const normalizeLine = (line: string) => {
  return normalizePaperLine(line)
}

const isQuestionStart = (line: string) => {
  const text = normalizeLine(line)
  return /^(?:第\s*\d+\s*题|[（(]?\d+[)）]?[\.．、])\s*\S+/.test(text)
}

const parseQuestionStart = (line: string) => {
  const text = normalizeLine(line)
  const match =
    text.match(/^第\s*(\d+)\s*题[：:\s]*(.+)$/) ||
    text.match(/^[（(]?(\d+)[)）]?[\.．、]\s*(.+)$/)

  if (!match) return null
  return { num: match[1], stem: (match[2] || '').trim() }
}

const isSectionHeading = (line: string) => {
  const text = normalizeLine(line)
  if (!text) return false
  // 支持中文数字（"三、"）和阿拉伯数字（"3."）前缀
  return /^(?:[一二三四五六七八九十]+|\d+)[、.．]?\s*(单选题|单项选择题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|选择题)/.test(text)
}

const detectSectionType = (line: string): QuestionType | null => {
  const text = normalizeLine(line)
  if (!isSectionHeading(text)) return null
  if (/多(?:项选择|选)|不定项/.test(text)) return 'checkbox'
  if (/(单选|单项选择|选择题)/.test(text)) return 'radio'
  if (/判断/.test(text)) return 'judge'
  if (/填空/.test(text)) return 'fill'
  if (/(简答|问答|论述|计算|代码|编程|综合)/.test(text)) return 'text'
  return null
}

const parseOptionLine = (line: string): QuestionOption | null => {
  const text = normalizeLine(line)
  const match = text.match(/^([A-H])[\.．、:：\)）]\s*(.+)$/)
  if (!match) return null
  return { label: match[1], text: match[2].trim() }
}

const finalizeQuestion = (question: AnswerQuestion | null, sectionType: QuestionType): AnswerQuestion | null => {
  if (!question) return null
  const options = question.rawLines.map((line) => parseOptionLine(line)).filter(Boolean) as QuestionOption[]
  let finalType: QuestionType = sectionType
  const stemHasMultiHint = question.stem && /多[项个]选|多选|不定项选/.test(question.stem)
  const stemHasJudgeHint = question.stem && /（判断题?）|\[判断题?]|（\s*[√×]\s*[\/]\s*[√×]\s*）|（\s*[对错]\s*[\/]\s*[对错]\s*）/.test(question.stem)
  const rawLinesHaveJudge = question.rawLines.some(line => /^(正确|错误|对|错|√|×)$/.test(line.trim()))

  if (options.length >= 2) {
    const judgeLike = options.length === 2 && options.every((item) => /(正确|错误|对|错|√|×)/.test(item.text))
    if (judgeLike) finalType = 'judge'
    else if (sectionType === 'checkbox' || stemHasMultiHint) finalType = 'checkbox'
    else finalType = 'radio'
  } else if (rawLinesHaveJudge || stemHasJudgeHint) {
    finalType = 'judge'
  }

  if (finalType === 'judge' && options.length < 2) {
    question.options = [{ label: 'A', text: '正确' }, { label: 'B', text: '错误' }]
  } else {
    question.options = options
  }

  question.type = finalType
  question.val = finalType === 'checkbox' ? [] : ''

  // 填空题：解析题干中的空白占位符，拆分为多空
  if (finalType === 'fill' && question.stem) {
    const segments = parseFillBlanks(question.stem)
    if (segments.length > 1) {
      question.blankSegments = segments
      question.val = new Array(segments.length - 1).fill('')
    }
  }

  return question
}

const parseFillBlanks = (stem: string): string[] => {
  // 匹配各种空白占位符：连续下划线、中文括号、英文括号、方括号、尖括号
  const blankRegex = /_{3,}|（\s*）|\(\s*\)|【\s*】|\[\s*\]|《\s*》/g
  const segments = stem.split(blankRegex)
  return segments
}

const parsePaper = (rawMarkdown: string) => {
  const { paper } = splitPaperAndAnswers(rawMarkdown)
  paperOnlyText.value = paper

  const lines = paper.split('\n')
  const sheet: AnswerQuestion[] = []
  let currentSectionType: QuestionType = 'text'
  let currentSectionLabel = ''
  let currentQuestion: AnswerQuestion | null = null
  let questionUid = 0

  for (const rawLine of lines) {
    const line = normalizeLine(rawLine)
    if (!line) continue

    // 先检测题型标题（如"一、单选题""3.判断题"），优先于题目解析
    const sectionType = detectSectionType(line)
    if (sectionType) {
      // 完成上一题
      const finished = finalizeQuestion(currentQuestion, currentSectionType)
      if (finished) sheet.push(finished)
      currentQuestion = null
      // 更新当前题型
      currentSectionType = sectionType
      currentSectionLabel = line
      continue
    }

    const questionStart = parseQuestionStart(line)
    if (questionStart) {
      const finished = finalizeQuestion(currentQuestion, currentSectionType)
      if (finished) sheet.push(finished)
      questionUid += 1
      currentQuestion = {
        num: questionStart.num,
        uid: `${currentSectionType}-${questionStart.num}-${questionUid}`,
        type: currentSectionType,
        val: currentSectionType === 'checkbox' ? [] : '',
        stem: questionStart.stem,
        options: [],
        rawLines: [],
        sectionLabel: currentSectionLabel,
      }
      continue
    }

    if (currentQuestion) currentQuestion.rawLines.push(line)
  }

  const lastQuestion = finalizeQuestion(currentQuestion, currentSectionType)
  if (lastQuestion) sheet.push(lastQuestion)
  answerSheet.value = sheet
}
</script>

<style scoped>
.exam-page { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif; height: 100vh; background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%); display: flex; flex-direction: column; overflow: hidden; }
.page-header { background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); border-bottom: 1px solid rgba(226, 232, 240, 0.8); flex-shrink: 0; height: 72px; display: flex; align-items: center; box-shadow: 0 4px 24px -6px rgba(15, 23, 42, 0.04); z-index: 100; position: sticky; top: 0; }
.header-container { width: 75%; max-width: 1600px; min-width: 1200px; margin: 0 auto; padding: 0 24px; display: flex; align-items: center; position: relative; box-sizing: border-box; }
.back-btn { padding: 6px 12px; color: #64748b; font-weight: 500; font-size: 15px; border-radius: 6px; transition: all 0.3s; display: flex; align-items: center; gap: 4px; }
.back-btn:hover { color: #0f172a; background: #f1f5f9; transform: translateX(-2px); }
.title-group { position: absolute; left: 50%; transform: translateX(-50%); display: flex; align-items: center; white-space: nowrap; }
.title-group h2 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; background: linear-gradient(135deg, #0f172a 0%, #334155 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; display: flex; align-items: center; gap: 12px; }
.subtitle-text { font-size: 20px; font-weight: 500; color: #3b82f6; -webkit-text-fill-color: #3b82f6; letter-spacing: 0.2px; }
.timer-group { position: absolute; right: 24px; display: flex; align-items: center; gap: 8px; color: #2563EB; font-weight: 600; font-size: 18px; }
.timer-text { font-variant-numeric: tabular-nums; }
.timer-warning { color: #EF4444; animation: pulse 1s ease-in-out infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.5; } }
.workspace { width: 75%; max-width: 1600px; min-width: 1200px; margin: 0 auto; padding: 24px; box-sizing: border-box; display: flex; gap: 24px; flex: 1; min-height: 0; }
.scroll-y { overflow-y: auto; scroll-behavior: smooth; }
.scroll-y::-webkit-scrollbar { width: 6px; }
.scroll-y::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 5px; }
.solid-panel { background: #FFFFFF; border: 1px solid #E7ECF3; box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04); border-radius: 5px; }
.paper-panel { flex: 1.2; padding: 24px 32px; display: flex; flex-direction: column; }
.panel-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #E7ECF3; padding-bottom: 16px; margin-bottom: 16px; }
.tag { background: #F3F4F6; color: #4B5563; padding: 4px 10px; border-radius: 5px; font-size: 13px; font-weight: 600; }
:deep(.doc-style) { color: #334155; line-height: 1.8; font-size: 14px;}
:deep(.doc-style h1) { font-size: 18px; text-align: center; border-bottom: 1px solid #E7ECF3; padding-bottom: 12px; color: #1F2937;}
:deep(.doc-style h2) { font-size: 15px; background: #F8FAFD; padding: 8px 12px; border-left: 3px solid #2563EB; margin: 24px 0 12px; color: #1F2937;}
:deep(.doc-style p) { margin-bottom: 12px; }
.answer-panel { flex: 0.8; padding: 24px; display: flex; flex-direction: column; background: #F8FAFD; position: relative;}
.answer-tag { background: #EFF6FF; color: #2563EB; }
.submit-btn { background: #2563EB; border: none; font-weight: 500; border-radius: 5px; }
.submit-btn:hover { background: #1D4ED8; }
.smart-answer-sheet { flex: 1; display: flex; flex-direction: column; gap: 16px; padding-bottom: 20px;}
.sheet-section-header { font-weight: 700; color: #1F2937; font-size: 15px; padding: 12px 0 4px; border-bottom: 1px solid #E7ECF3; margin-bottom: 8px; }
.question-image-box { background: #ffffff; border: 1px dashed #bfdbfe; border-radius: 5px; padding: 12px; margin-bottom: 14px; }
.image-upload-btn { position: relative; overflow: hidden; display: inline-flex; align-items: center; justify-content: center; padding: 6px 12px; border-radius: 5px; background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; cursor: pointer; font-size: 13px; font-weight: 600; }
.image-upload-btn.small { padding: 4px 10px; font-size: 12px; }
.image-upload-btn input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
.image-chip-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.image-chip-list.compact { margin-top: 8px; }
.image-chip { display: inline-flex; align-items: center; gap: 6px; padding: 4px 8px; border-radius: 5px; background: #f8fafc; border: 1px solid #dbeafe; font-size: 12px; }
.image-chip a { color: #2563eb; }
.image-chip button { border: none; background: transparent; color: #64748b; cursor: pointer; padding: 0; }
.sheet-item { background: #FFFFFF; border: 1px solid #E7ECF3; padding: 16px; border-radius: 5px; transition: 0.2s; }
.sheet-item:hover { border-color: #BFDBFE; box-shadow: 0 2px 8px rgba(37, 99, 235, 0.05); }
.sheet-item.has-error { border-color: #FCA5A5; background: #FFF5F5; }
.q-label { font-weight: 600; color: #1F2937; margin-bottom: 12px; font-size: 14px;}
.q-stem { margin-bottom: 12px; color: #4B5563; font-size: 13px; line-height: 1.6; }
.custom-options { display: flex; gap: 16px; flex-wrap: wrap;}
.option-list { display: flex; flex-direction: column; gap: 10px; }
:deep(.custom-options .ant-radio-wrapper) { margin: 0; padding: 6px 16px; background: #FFFFFF; border: 1px solid #E7ECF3; border-radius: 5px; transition: 0.2s; }
:deep(.custom-options .ant-radio-wrapper-checked) { background: #EFF6FF; border-color: #2563EB; color: #2563EB; font-weight: 500; }
:deep(.custom-options .ant-checkbox-wrapper) {
  margin: 0;
  padding: 6px 16px;
  background: #FFFFFF;
  border: 1px solid #E7ECF3;
  border-radius: 5px;
  transition: 0.2s;
}
:deep(.custom-options .ant-checkbox-wrapper-checked) {
  background: #EFF6FF;
  border-color: #2563EB;
  color: #2563EB;
  font-weight: 500;
}
.custom-checkbox-group { display: flex; flex-direction: column; gap: 8px; }
.custom-checkbox-item { display: inline-flex; align-items: center; gap: 8px; padding: 6px 16px; background: #FFFFFF; border: 1px solid #E7ECF3; border-radius: 5px; cursor: pointer; transition: all 0.2s; user-select: none; }
.custom-checkbox-item:hover { border-color: #BFDBFE; }
.custom-checkbox-item.active { background: #EFF6FF; border-color: #2563EB; }
.native-checkbox { display: none; }
.checkbox-indicator { width: 16px; height: 16px; border: 2px solid #CBD5E1; border-radius: 3px; display: inline-flex; align-items: center; justify-content: center; transition: all 0.2s; flex-shrink: 0; }
.custom-checkbox-item.active .checkbox-indicator { background: #2563EB; border-color: #2563EB; position: relative; }
.custom-checkbox-item.active .checkbox-indicator::after { content: ''; display: block; width: 4px; height: 8px; border: solid #FFFFFF; border-width: 0 2px 2px 0; transform: rotate(45deg); margin-top: -2px; }
.checkbox-text { color: #334155; font-size: 14px; }
.custom-checkbox-item.active .checkbox-text { color: #2563EB; font-weight: 500; }
.custom-input { height: 38px; border-radius: 5px; border-color: #E7ECF3; background: #FFFFFF; }
.custom-input:focus, .custom-input:hover { border-color: #2563EB; box-shadow: none; }
.fill-blanks-wrap { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; line-height: 2; }
.fill-text-seg { color: #334155; font-size: 14px; white-space: pre-wrap; }
.inline-fill-input { width: 100px; border-radius: 4px; border-color: #93C5FD; background: #F0F7FF; text-align: center; }
.inline-fill-input:hover { border-color: #2563EB; }
.inline-fill-input:focus { border-color: #2563EB; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
.multi-hint { font-size: 12px; color: #6B7280; display: block; margin-bottom: 4px; }
.custom-textarea { border-radius: 5px; border-color: #E7ECF3; background: #FFFFFF; font-size: 14px; }
.custom-textarea:focus { border-color: #2563EB; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
.editor-wrapper { flex: 1; border-radius: 5px; overflow: hidden; border: 1px solid #E7ECF3; background: #FFFFFF; }
.answer-textarea { width: 100%; height: 100%; padding: 16px; border: none; resize: none; outline: none; font-size: 14px; color: #334155; font-family: inherit; }
.empty-box { text-align: center; padding: 100px; color: #64748B; margin: 24px auto; width: 75%; }
.submitted-icon { margin-bottom: 24px; }
.report-section { flex: 1; overflow-y: auto; }
.score-summary { text-align: center; padding: 24px; background: #FFFFFF; border: 1px solid #E7ECF3; border-radius: 5px; margin-bottom: 20px; }
.score-big { margin-bottom: 16px; }
.score-label { display: block; font-size: 14px; color: #64748B; margin-bottom: 4px; }
.score-value { font-size: 48px; font-weight: 800; color: #2563EB; }
.score-summary-row { display: flex; gap: 16px; padding: 16px 20px; background: #EFF6FF; border: 1px solid #BFDBFE; border-radius: 5px; margin-bottom: 20px; }
.summary-item { display: flex; align-items: baseline; gap: 4px; }
.summary-label { font-size: 13px; color: #64748B; }
.summary-value { font-size: 24px; font-weight: 800; color: #2563EB; }
.summary-value.correct { color: #16A34A; }
.summary-value.wrong { color: #DC2626; }
.summary-unit { font-size: 13px; color: #64748B; }
.teacher-remark { text-align: left; background: #FFFBEB; border: 1px solid #FDE68A; border-radius: 5px; padding: 12px 16px; }
.teacher-remark strong { color: #92400E; }
.teacher-remark p { margin: 4px 0 0; color: #78350F; font-size: 14px; }
.answer-records { display: flex; flex-direction: column; gap: 12px; }
.record-item { background: #FFFFFF; border: 1px solid #E7ECF3; border-radius: 5px; padding: 14px 16px; }
.record-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.record-q-title { font-weight: 600; color: #1F2937; font-size: 14px; }
.record-score { font-weight: 700; font-size: 16px; }
.score-pass { color: #16A34A; }
.score-fail { color: #DC2626; }
.record-stem { font-size: 13px; color: #475569; line-height: 1.7; margin-bottom: 8px; }
.record-answer { font-size: 13px; }
.record-label { color: #64748B; }
.record-value { color: #2563EB; font-weight: 500; word-break: break-all; }
.record-answer-images { display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0 10px; padding-left: 72px; }
.record-answer-image { border: 1px solid #dbeafe; border-radius: 5px; overflow: hidden; background: #f8fafc; }
.record-answer-image :deep(.ant-image-img) { object-fit: cover; display: block; }
.record-standard-answer { font-size: 13px; margin-top: 4px; }
.record-standard-answer .record-value { color: #059669; }
.record-ai-comment { font-size: 13px; margin-top: 4px; padding: 6px 10px; background: #F9FAFB; border-radius: 4px; }
.record-ai-comment .record-label { font-weight: 600; }
.record-ai-comment .record-value { color: #6B7280; font-weight: 400; }
</style>
