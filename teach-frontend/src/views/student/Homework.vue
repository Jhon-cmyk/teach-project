<template>
  <div class="homework-page">
    <header class="page-header">
      <div class="header-container">
        <a-button type="link" @click="router.push(isPersonalPractice ? '/student/diagnosis' : '/student/dashboard')" class="back-btn">
          <arrow-left-outlined /> {{ isPersonalPractice ? '返回学习画像' : '返回主页' }}
        </a-button>

        <div class="title-group">
          <h2>
            <span class="subtitle-text" v-if="assignmentDetail">
           {{ assignmentDetail.title }}
        </span>
          </h2>
        </div>
      </div>
    </header>

    <div v-if="isReportMode" class="practice-report-shell">
      <a-spin :spinning="reportLoading" tip="正在读取练习结果...">
        <HomeworkReportPanel
          :report="reportData"
          role="student"
          :highlight-question-no="route.query.questionNo as string"
        />
      </a-spin>
    </div>

    <div class="workspace" v-else-if="assignmentDetail">
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
        <div v-if="currentStatus === 'pending'" class="status-view">
          <div class="panel-header">
            <div class="tag answer-tag">作答区</div>
            <div class="header-actions">
              <a-button type="primary" class="submit-btn" :loading="isSubmitting" @click="submitHomework">
                {{ isPersonalPractice ? '提交并自动批改' : '提交作业' }}
              </a-button>
            </div>
          </div>

          <div class="smart-answer-sheet" v-if="answerSheet.length > 0">
            <div v-for="q in answerSheet" :key="q.uid" class="sheet-item">
              <div class="q-label">第 {{ q.num }} 题</div>
              <div class="q-stem" v-if="q.stem">{{ q.stem }}</div>

              <div v-if="canUploadImageForQuestion(q)" class="question-image-box">
                <label class="image-upload-btn small">
                  上传本题图片
                  <input type="file" accept="image/jpeg,image/png,image/webp" multiple @change="(event) => handleQuestionImageSelect(q.uid, event)" />
                </label>
                <div v-if="questionImageMap[q.uid]?.length" class="image-chip-list compact">
                  <span v-for="(url, idx) in questionImageMap[q.uid]" :key="url" class="image-chip">
                    <a :href="url" target="_blank">图 {{ idx + 1 }}</a>
                    <button type="button" @click="removeQuestionImage(q.uid, idx)">×</button>
                  </span>
                </div>
              </div>

              <a-radio-group
                v-if="q.type === 'radio'"
                v-model:value="q.val"
                class="custom-options option-list"
              >
                <a-radio
                  v-for="opt in (q.options.length
                  ? q.options
                  : [
                      { label: 'A', text: 'A' },
                      { label: 'B', text: 'B' },
                      { label: 'C', text: 'C' },
                      { label: 'D', text: 'D' },
                    ])"
                  :key="opt.label"
                  :value="opt.label"
                >
                  {{ opt.label }}. {{ opt.text }}
                </a-radio>
              </a-radio-group>

                <div v-else-if="q.type === 'checkbox'" class="custom-checkbox-group">
                  <span class="multi-hint">（可多选）</span>
                  <label
                    v-for="opt in (q.options.length
                    ? q.options
                    : [
                        { label: 'A', text: 'A' },
                        { label: 'B', text: 'B' },
                        { label: 'C', text: 'C' },
                        { label: 'D', text: 'D' },
                      ])"
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

                <a-textarea
                  v-else
                  v-model:value="q.val"
                  :auto-size="{ minRows: 4, maxRows: 12 }"
                  placeholder="请输入解答过程..."
                  class="custom-textarea"
                />
            </div>
          </div>

          <div v-else class="editor-wrapper">
            <textarea
              v-model="fallbackAnswer"
              class="answer-textarea"
              placeholder="请在此处输入解答过程..."
            ></textarea>
          </div>

          <div class="teacher-note" v-if="assignmentDetail.teacherNote">
            <strong>{{ isPersonalPractice ? '练习说明：' : '老师寄语：' }}</strong>{{ assignmentDetail.teacherNote }}
          </div>
        </div>

        <div v-else class="status-view submitted-view">
          <div class="submitted-card">
            <div class="submitted-badge">{{ isPersonalPractice ? '自动批改完成' : '待教师批改' }}</div>
            <h3>{{ isPersonalPractice ? '专项练习已完成' : '作业已提交' }}</h3>
            <p v-if="isPersonalPractice">查看得分和逐题结果，错题会继续用于更新你的学习画像。</p>
            <p v-else>老师完成批改后，你可以在个人主页的作业记录中查看最终分数、逐题详情和评语。</p>
            <div class="submitted-actions">
              <a-button v-if="isPersonalPractice" type="primary" @click="openLatestReport">查看练习结果</a-button>
              <a-button v-else type="primary" @click="router.push('/student/profile?tab=homework')">查看作业记录</a-button>
              <a-button @click="router.push(isPersonalPractice ? '/student/diagnosis' : '/student/dashboard')">
                {{ isPersonalPractice ? '返回学习画像' : '返回主页' }}
              </a-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="pageLoading" class="empty-box solid-panel">
      <a-spin size="large" tip="正在加载作业数据..." />
    </div>

    <div v-else class="empty-box solid-panel">
      <h2>作业数据不存在或已过期</h2>
      <a-button @click="router.push('/student/dashboard')">返回主页</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'
import { normalizePaperLine, splitPaperAndAnswers } from '@/utils/paperParser'
import HomeworkReportPanel from '@/components/homework/HomeworkReportPanel.vue'

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
}

const route = useRoute()
const router = useRouter()
const md = new MarkdownIt({ breaks: true, html: true })
const renderMd = (text: string) => md.render(text || '')

const toggleCheckbox = (q: AnswerQuestion, label: string) => {
  const arr = (q.val as string[]) || []
  if (arr.includes(label)) {
    q.val = arr.filter(v => v !== label)
  } else {
    q.val = [...arr, label]
  }
}

const assignmentId = ref<number>(0)
const assignmentDetail = ref<any>(null)
const pageLoading = ref(true)

const currentStatus = ref<'pending' | 'submitted'>('pending')
const isSubmitting = ref(false)

const paperOnlyText = ref('')
const hiddenAnswers = ref('')
const answerSheet = ref<AnswerQuestion[]>([])
const fallbackAnswer = ref('')
const questionImageMap = ref<Record<string, string[]>>({})
const reportData = ref<any>(null)
const reportLoading = ref(false)
const lastSubmissionId = ref<number | null>(null)

const answerMode = computed(() => assignmentDetail.value?.answerMode || 'online')
const isPersonalPractice = computed(() => assignmentDetail.value?.assignmentType === 'personal_practice' || route.query.personal === '1')
const isReportMode = computed(() => route.query.mode === 'report' && !!route.query.submissionId)
const hasAnyImage = computed(() =>
  Object.values(questionImageMap.value).some(list => Array.isArray(list) && list.length > 0)
)

const canUploadImageForQuestion = (q: AnswerQuestion) => q.type === 'fill' || q.type === 'text'

onMounted(async () => {
  const idParam = route.params.id as string
  assignmentId.value = parseInt(idParam, 10)

  if (isNaN(assignmentId.value)) {
    message.error('作业ID无效')
    pageLoading.value = false
    return
  }

  try {
    const detail = await request.get('/homework/student/detail', {
      params: { assignmentId: assignmentId.value },
    })

    assignmentDetail.value = detail
    lastSubmissionId.value = Number(assignmentDetail.value?.latestSubmissionId) || null

// 关键：不管是否已完成，先把左侧试卷正文解析出来
    parsePaper(assignmentDetail.value?.contentSnapshot || '')

    if (assignmentDetail.value?.completed && !assignmentDetail.value?.allowRedo) {
      currentStatus.value = 'submitted'
    } else {
      currentStatus.value = 'pending'
    }
    if (isReportMode.value) {
      await loadReport(Number(route.query.submissionId))
    }
  } catch (e: any) {
    message.error(e?.message || '网络错误，无法加载作业')
  } finally {
    pageLoading.value = false
  }
})

const normalizeLine = (line: string) => {
  return normalizePaperLine(line)
}

const loadReport = async (submissionId: number) => {
  if (!submissionId) return
  reportLoading.value = true
  try {
    reportData.value = await request.get('/homework/student/report', { params: { submissionId } })
  } catch (error: any) {
    message.error(error?.message || '练习结果加载失败')
  } finally {
    reportLoading.value = false
  }
}

const openLatestReport = async () => {
  if (!lastSubmissionId.value) {
    message.warning('暂未找到本次练习结果')
    return
  }
  await router.replace({
    query: { ...route.query, submissionId: String(lastSubmissionId.value), mode: 'report', personal: '1' }
  })
  await loadReport(lastSubmissionId.value)
}

const isQuestionStart = (line: string) => {
  const text = normalizeLine(line)
  return /^(?:第\s*\d+\s*题|[（(]?\d+[)）]?[\.．、])\s*\S+/.test(text)
}

const parseQuestionStart = (line: string) => {
  const text = normalizeLine(line)
  let match =
    text.match(/^第\s*(\d+)\s*题[：:\s]*(.+)$/) ||
    text.match(/^[（(]?(\d+)[)）]?[\.．、]\s*(.+)$/)

  if (!match) {
    return null
  }

  return {
    num: match[1],
    stem: (match[2] || '').trim(),
  }
}

const isSectionHeading = (line: string) => {
  const text = normalizeLine(line)
  if (!text) return false
  return /^(?:[一二三四五六七八九十]+|\d+)[、.．]?\s*(单选题|单项选择题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|选择题)/.test(
    text
  )
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
  return {
    label: match[1],
    text: match[2].trim(),
  }
}

const finalizeQuestion = (question: AnswerQuestion | null, sectionType: QuestionType): AnswerQuestion | null => {
  if (!question) return null

  const options = question.rawLines
    .map((line) => parseOptionLine(line))
    .filter(Boolean) as QuestionOption[]

  let finalType: QuestionType = sectionType
  const stemHasMultiHint = question.stem && /多[项个]选|多选|不定项选/.test(question.stem)
  const stemHasJudgeHint = question.stem && /（判断题?）|\[判断题?]|（\s*[√×]\s*[\/]\s*[√×]\s*）|（\s*[对错]\s*[\/]\s*[对错]\s*）|[（(]\s*[）)]$/.test(question.stem)
  const rawLinesHaveJudge = question.rawLines.some(line => /^(正确|错误|对|错|√|×)$/.test(line.trim()))

  if (options.length >= 2) {
    const judgeLike =
      options.length === 2 &&
      options.every((item) => /(正确|错误|对|错|√|×)/.test(item.text))

    if (judgeLike) {
      finalType = 'judge'
    } else if (sectionType === 'checkbox' || stemHasMultiHint) {
      finalType = 'checkbox'
    } else {
      finalType = 'radio'
    }
  } else if (rawLinesHaveJudge || stemHasJudgeHint) {
    finalType = 'judge'
  }

  if (finalType === 'judge' && options.length === 0) {
    question.options = [
      { label: 'A', text: '正确' },
      { label: 'B', text: '错误' },
    ]
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
  const blankRegex = /_{3,}|（\s*）|\(\s*\)|【\s*】|\[\s*\]|《\s*》/g
  const segments = stem.split(blankRegex)
  return segments
}

const uploadHomeworkImage = async (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<string, string>('/homework/submission/image/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  } as any)
}

const handleQuestionImageSelect = async (questionNo: string, event: Event) => {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return
  try {
    const urls: string[] = []
    for (const file of files) {
      urls.push(await uploadHomeworkImage(file))
    }
    questionImageMap.value = {
      ...questionImageMap.value,
      [questionNo]: [...(questionImageMap.value[questionNo] || []), ...urls]
    }
    message.success('本题图片上传成功')
  } finally {
    input.value = ''
  }
}

const removeQuestionImage = (questionNo: string, index: number) => {
  questionImageMap.value = {
    ...questionImageMap.value,
    [questionNo]: (questionImageMap.value[questionNo] || []).filter((_, idx) => idx !== index)
  }
}

const parsePaper = (rawMarkdown: string) => {
  const { paper, answers } = splitPaperAndAnswers(rawMarkdown)
  paperOnlyText.value = paper
  hiddenAnswers.value = answers

  const lines = paper.split('\n')
  const sheet: AnswerQuestion[] = []

  let currentSectionType: QuestionType = 'text'
  let currentQuestion: AnswerQuestion | null = null
  let questionUid = 0

  for (const rawLine of lines) {
    const line = normalizeLine(rawLine)
    if (!line) continue

    const sectionType = detectSectionType(line)
    if (sectionType) {
      const finished = finalizeQuestion(currentQuestion, currentSectionType)
      if (finished) {
        sheet.push(finished)
      }
      currentQuestion = null
      currentSectionType = sectionType
      continue
    }

    const questionStart = parseQuestionStart(line)
    if (questionStart) {
      const finished = finalizeQuestion(currentQuestion, currentSectionType)
      if (finished) {
        sheet.push(finished)
      }
      questionUid += 1
      currentQuestion = {
        num: questionStart.num,
        uid: `${currentSectionType}-${questionStart.num}-${questionUid}`,
        type: currentSectionType,
        val: currentSectionType === 'checkbox' ? [] : '',
        stem: questionStart.stem,
        options: [],
        rawLines: [],
      }
      continue
    }

    if (currentQuestion) {
      currentQuestion.rawLines.push(line)
    }
  }

  const lastQuestion = finalizeQuestion(currentQuestion, currentSectionType)
  if (lastQuestion) {
    sheet.push(lastQuestion)
  }

  answerSheet.value = sheet
}

const submitHomework = async () => {
  let studentAnswerData: any[]

  const shouldSubmitText = answerMode.value !== 'image'
  if (answerSheet.value.length > 0) {
    studentAnswerData = answerSheet.value.map((q, index) => ({
      num: String(index + 1),
      originalQuestionNo: q.num,
      imageKey: q.uid,
      type: q.type,
      stem: q.stem,
      fullScore: null,
      answer: Array.isArray(q.val) ? q.val.join(', ') : q.val,
    }))
  } else if (shouldSubmitText) {
    studentAnswerData = [
      {
        num: '1',
        type: 'text',
        stem: '全文作答',
        answer: fallbackAnswer.value,
      },
    ]
  } else {
    studentAnswerData = []
  }

  if ((answerMode.value === 'image' || answerMode.value === 'mixed') && !hasAnyImage.value && answerMode.value === 'image') {
    message.warning('请至少上传一张作答图片')
    return
  }

  const hasEmpty = studentAnswerData.some((q) => !q.answer || String(q.answer).trim() === '')
  if (hasEmpty) {
    message.info('存在未作答题目，提交后将由教师批改')
  }

  isSubmitting.value = true
  const questionImageItems = Object.entries(questionImageMap.value)
    .filter(([, urls]) => Array.isArray(urls) && urls.length > 0)
    .map(([imageKey, imageUrls]) => {
      const q = answerSheet.value.find(item => item.uid === imageKey)
      return q && canUploadImageForQuestion(q) ? { questionNo: q.uid, imageUrls } : null
    })
    .filter(Boolean) as Array<{ questionNo: string; imageUrls: string[] }>
  const hasAllowedImages = questionImageItems.length > 0
  const submissionType = answerMode.value === 'image'
    ? 'image'
    : (hasAllowedImages ? 'mixed' : 'online')

  try {
    const submissionId = await request.post<number, number>(
      '/homework/submission/submit',
      {
        assignmentId: assignmentId.value,
        submissionType,
        studentAnswerJson: JSON.stringify(studentAnswerData),
        wholePaperImageUrls: [],
        questionImageItems,
      },
      { timeout: 120000 }
    )
    lastSubmissionId.value = Number(submissionId) || null
    currentStatus.value = 'submitted'
    message.success(isPersonalPractice.value ? '练习已完成自动批改' : '作业已提交，等待教师批改')
  } catch (error: any) {
    console.error('提交作业失败：', error)
    message.error(error?.message || '提交失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.practice-report-shell {
  width: min(1100px, calc(100vw - 48px));
  min-height: 360px;
  margin: 24px auto 48px;
  padding: 24px;
  border: 1px solid #dce3ea;
  border-radius: 8px;
  background: #ffffff;
  overflow: auto;
}

.homework-page { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif; height: 100vh; background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%); display: flex; flex-direction: column; overflow: hidden; }
/* ================= 头部高级样式重构 ================= */

/* ================= 头部高级样式（居中增强版） ================= */

.page-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  flex-shrink: 0;
  height: 72px;
  display: flex;
  align-items: center;
  box-shadow: 0 4px 24px -6px rgba(15, 23, 42, 0.04);
  z-index: 100;
  position: sticky;
  top: 0;
}

.header-container {
  width: 75%;
  max-width: 1600px;
  min-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  /* 关键：为子元素的绝对定位提供参考系 */
  position: relative;
  box-sizing: border-box;
}

/* 1. 最左侧：返回按钮 */
.back-btn {
  padding: 6px 12px;
  color: #64748b;
  font-weight: 500;
  font-size: 15px; /* 字体略微放大 */
  border-radius: 6px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 4px;
}
.back-btn:hover {
  color: #0f172a;
  background: #f1f5f9;
  transform: translateX(-2px);
}

/* 2. 正中间：标题容器绝对居中 */
.title-group {
  position: absolute;
  left: 50%;
  transform: translateX(-50%); /* 完美居中的核心属性 */
  display: flex;
  align-items: center;
  white-space: nowrap; /* 防止标题过长换行 */
}

/* 3. 主标题：加大字体，保留轻微渐变显高级 */
.title-group h2 {
  margin: 0;
  font-size: 24px; /* 字体大幅加大 */
  font-weight: 700;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 4. 试卷名称：去掉边框，改为优雅的副标题文本 */
.subtitle-text {
  font-size: 20px; /* 试卷名称字体同步加大 */
  font-weight: 500;
  /* 使用竖线分隔时的颜色，或者使用品牌主色调 */
  color: #3b82f6;
  -webkit-text-fill-color: #3b82f6; /* 覆盖前面的文字透明属性 */
  letter-spacing: 0.2px;
}
.workspace { width: 75%; max-width: 1600px; min-width: 1200px; margin: 0 auto; padding: 24px; box-sizing: border-box; display: flex; gap: 24px; flex: 1; min-height: 0; }
.scroll-y { overflow-y: auto; scroll-behavior: smooth; }
.scroll-y::-webkit-scrollbar { width: 6px; }
.scroll-y::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 5px; }
.solid-panel { background: #FFFFFF; border: 1px solid #E7ECF3; box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04); border-radius: 5px; }
.paper-panel { flex: 1.2; padding: 24px 32px; display: flex; flex-direction: column; }
.panel-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #E7ECF3; padding-bottom: 16px; margin-bottom: 16px; }
.header-actions { display: flex; align-items: center; gap: 8px; }
.tag { background: #F3F4F6; color: #4B5563; padding: 4px 10px; border-radius: 5px; font-size: 13px; font-weight: 600; }
:deep(.doc-style) { color: #334155; line-height: 1.8; font-size: 14px;}
:deep(.doc-style h1) { font-size: 18px; text-align: center; border-bottom: 1px solid #E7ECF3; padding-bottom: 12px; color: #1F2937;}
:deep(.doc-style h2) { font-size: 15px; background: #F8FAFD; padding: 8px 12px; border-left: 3px solid #2563EB; margin: 24px 0 12px; color: #1F2937;}
:deep(.doc-style p) { margin-bottom: 12px; }
.answer-panel { flex: 0.8; padding: 24px; display: flex; flex-direction: column; background: #F8FAFD; position: relative;}
.status-view { display: flex; flex-direction: column; height: 100%; }
.answer-tag { background: #EFF6FF; color: #2563EB; }
.submit-btn { background: #2563EB; border: none; font-weight: 500; border-radius: 5px; box-shadow: none; transition: background 0.2s; }
.submit-btn:hover { background: #1D4ED8; }
.smart-answer-sheet { flex: 1; display: flex; flex-direction: column; gap: 16px; padding-bottom: 20px;}
.image-submit-box, .question-image-box { background: #ffffff; border: 1px dashed #bfdbfe; border-radius: 5px; padding: 12px; margin-bottom: 14px; }
.image-submit-head { display: flex; align-items: center; justify-content: space-between; color: #1f2937; font-size: 13px; font-weight: 600; }
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
.q-label { font-weight: 600; color: #1F2937; margin-bottom: 12px; font-size: 14px;}
.q-stem { margin-bottom: 12px; color: #4B5563; font-size: 13px; line-height: 1.6; }
.custom-options { display: flex; gap: 16px; flex-wrap: wrap;}
.option-list { display: flex; flex-direction: column; gap: 10px; }
:deep(.custom-options .ant-radio-wrapper) { margin: 0; padding: 6px 16px; background: #FFFFFF; border: 1px solid #E7ECF3; border-radius: 5px; transition: 0.2s; }
:deep(.custom-options .ant-radio-wrapper-checked) { background: #EFF6FF; border-color: #2563EB; color: #2563EB; font-weight: 500; }
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
.answer-textarea:focus { box-shadow: inset 0 0 0 1px #2563EB; }
/* 批改中：独占面板的流式输出区 */
.grading-hint { font-size: 13px; color: #64748b; }
.streaming-panel { flex: 1; display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.grading-waiting { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; }
.scan-anim { width: 100%; height: 2px; background: #EFF6FF; border-radius: 2px; overflow: hidden; position: relative; }
.scan-bar { position: absolute; top: 0; left: 0; height: 100%; width: 40%; background: #2563EB; border-radius: 2px; animation: scanBar 1.5s ease-in-out infinite; }
@keyframes scanBar { 0% { left: -40%; } 100% { left: 140%; } }
.streaming-content { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.streaming-header { display: flex; align-items: center; gap: 8px; padding: 10px 0; font-size: 13px; font-weight: 600; color: #2563EB; border-bottom: 1px solid #E7ECF3; flex-shrink: 0; }
.streaming-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: #2563EB; animation: pulse 1s ease-in-out infinite; }
@keyframes pulse { 0%,100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(0.8); } }
.streaming-body { flex: 1; overflow-y: auto; padding-top: 12px; font-size: 13px; line-height: 1.7; }
.scan-text { color: #2563EB; font-weight: 600; font-size: 14px; }
.success-tag { background: #ECFDF5; color: #059669; border: 1px solid #A7F3D0; }
.score-badge { color: #2563EB; font-weight: 700; }
.score-num { font-size: 28px; line-height: 1; }
.ai-feedback-box { background: #FFFFFF; border: 1px solid #BFDBFE; border-radius: 5px; padding: 20px; margin-bottom: 20px; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.04);}
.feedback-title { margin: 0 0 16px 0; color: #1E3A8A; font-size: 16px; font-weight: 600; }
:deep(.feedback-content h1) { display: none; }
:deep(.feedback-content h2) { font-size: 14px; color: #2563EB; border-bottom: none; padding-bottom: 0; margin-top: 20px; margin-bottom: 8px;}
:deep(.feedback-content p), :deep(.feedback-content li) { color: #4B5563; font-size: 13px; line-height: 1.6;}
.student-record { background: #F8FAFC; padding: 16px; border-radius: 5px; border: 1px dashed #CBD5E1; }
.student-record h4 { margin: 0 0 8px 0; color: #64748B; font-size: 13px; font-weight: 600;}
.answer-record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.answer-record-item {
  background: #ffffff;
  border: 1px solid #e7ecf3;
  border-radius: 5px;
  padding: 14px 16px;
}

.record-q-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.record-q-type {
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
}

.record-q-stem {
  font-size: 13px;
  color: #475569;
  line-height: 1.7;
  margin-bottom: 8px;
}

.record-q-answer {
  font-size: 13px;
  line-height: 1.6;
}

.record-label {
  color: #64748b;
}

.record-value {
  color: #2563eb;
  font-weight: 500;
  word-break: break-all;
}
.record-text { color: #475569; font-size: 13px; white-space: pre-wrap; font-family: 'SF Mono', Consolas, monospace; background: #FFFFFF; padding: 12px; border-radius: 5px; border: 1px solid #E7ECF3;}
.submitted-view { justify-content: center; }
.submitted-card { background: #fff; border: 1px solid #dbeafe; border-radius: 8px; padding: 28px; color: #1f2937; }
.submitted-badge { display: inline-flex; align-items: center; padding: 4px 10px; border-radius: 5px; background: #eff6ff; color: #2563eb; font-size: 13px; font-weight: 700; margin-bottom: 12px; }
.submitted-card h3 { margin: 0 0 8px; font-size: 20px; color: #111827; }
.submitted-card p { margin: 0; color: #475569; line-height: 1.7; }
.submitted-actions { display: flex; gap: 10px; margin-top: 20px; flex-wrap: wrap; }
.empty-box { text-align: center; padding: 100px; color: #64748B; margin: 24px auto; width: 75%; }
.teacher-note { margin-top: 16px; font-size: 13px; color: #4B5563; background: #F3F4F6; padding: 12px; border-radius: 5px; }
</style>
