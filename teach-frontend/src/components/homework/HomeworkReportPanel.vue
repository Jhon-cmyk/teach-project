<template>
  <div class="homework-report-panel" v-if="report">

    <div class="report-header">
      <div class="header-main">
        <div class="header-kicker">
          <span class="status-tag" :class="statusClass">{{ statusText }}</span>
          <span v-if="showStudentName && studentName" class="meta-item student-name">{{ studentName }}</span>
        </div>
        <h2 class="report-title">{{ report.assignmentTitle || '作答报告' }}</h2>
      </div>

      <div class="score-block" :class="scoreStateClass">
        <div class="score-number">{{ displayScore }}</div>
        <div class="score-label">{{ scoreLabel }}</div>
        <div class="score-detail">
          <span class="correct">正确 {{ displayCorrect }} 题</span>
          <span class="wrong">错误 {{ displayWrong }} 题</span>
        </div>
      </div>
    </div>

    <!-- 教师评语 -->
    <div v-if="report.submission?.teacherRemark" class="teacher-remark">
      <div class="body-label">教师评语</div>
      <div class="remark-content">{{ report.submission.teacherRemark }}</div>
    </div>

    <!-- 逐题详情（考试模式） -->
    <div v-if="detailItems.length > 0" class="question-details">
      <div class="body-label">逐题详情</div>
      <div class="detail-table">
        <div
          v-for="(item, idx) in detailItems"
          :key="item.id ?? idx"
          class="detail-row"
          :class="[detailStateClass(item), { 'is-highlighted': isHighlighted(item) }]"
          :data-question-no="item.questionNo"
        >
          <div class="detail-header">
            <div class="detail-titleline">
              <span class="detail-no">第{{ item.questionNo ?? (idx + 1) }}题</span>
              <span class="detail-type">{{ questionTypeText(item.questionType) }}</span>
              <span v-if="parseDetailImageUrls(item.imageUrlsJson).length" class="detail-type image-type">图片作答</span>
            </div>
            <span class="detail-score">
              {{ detailScoreText(item) }}
            </span>
          </div>
          <div v-if="item.stemSnapshot" class="detail-stem">{{ item.stemSnapshot }}</div>
          <div v-if="parseDetailOptions(item.optionsJson).length" class="detail-options">
            <div
              v-for="option in parseDetailOptions(item.optionsJson)"
              :key="option.label"
              class="detail-option"
              :class="optionStateClass(item, option.label)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-text">{{ option.text }}</span>
            </div>
          </div>
          <div class="detail-answer-row">
            <div class="detail-col">
              <span class="col-label">学生答案</span>
              <span class="col-value" :class="answerStateClass(item)">{{ studentAnswerText(item) }}</span>
            </div>
            <div class="detail-col">
              <span class="col-label">参考答案</span>
              <span class="col-value ref">{{ item.standardAnswer || '未提供' }}</span>
            </div>
          </div>
          <div v-if="parseDetailImageUrls(item.imageUrlsJson).length" class="detail-images">
            <div
              v-for="(url, imageIdx) in parseDetailImageUrls(item.imageUrlsJson)"
              :key="url"
              class="detail-image-card"
            >
              <a-image
                :src="url"
                :width="132"
                :height="96"
                class="detail-preview-image"
                :preview="{ mask: '预览大图' }"
              />
              <span>第 {{ item.questionNo || idx + 1 }} 题 · 图片 {{ imageIdx + 1 }}</span>
            </div>
          </div>
          <div v-if="displayAiComment(item)" class="detail-comment">{{ displayAiComment(item) }}</div>
        </div>
      </div>
    </div>

    <div v-if="isReviewPending && role === 'student'" class="review-pending-note">
      作业已提交，正在等待教师批改。最终分数会在批改完成后显示。
    </div>

    <div v-if="normalizedImageItems.length" class="submission-images">
      <div class="body-label">图片作答</div>
      <div class="image-grid">
        <div
          v-for="(image, idx) in normalizedImageItems"
          :key="image.id || idx"
          class="image-chip"
        >
          <a-image
            :src="image.imageUrl"
            :width="156"
            :height="112"
            class="submission-preview-image"
            :preview="{ mask: '预览大图' }"
          />
          <span>{{ image.questionNo ? `第 ${image.questionNo} 题` : '整份作答' }} · 图片 {{ idx + 1 }}</span>
        </div>
      </div>
    </div>

    <div v-if="isExamMode && reportMarkdown" class="report-body">
      <div class="body-label">{{ isExamMode ? 'AI 批改报告' : '批改报告' }}</div>
      <div class="markdown-render doc-style" v-html="renderMd(reportMarkdown)"></div>
    </div>

    <!-- 考试待批阅提示 -->
    <div v-if="isExamMode && detailItems.length === 0 && !reportMarkdown" class="report-body">
      <div class="body-label">作答内容</div>
      <a-empty description="该学生已提交考试，等待教师批阅" />
    </div>

  </div>

  <a-empty v-else description="暂无作答报告" />
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, watch } from 'vue'
import MarkdownIt from 'markdown-it'

type RawDate = string | number | Date | null | undefined

interface SubmissionEntity {
  id?: number
  submitStatus?: string
  totalScore?: number | null
  correctCount?: number | null
  wrongCount?: number | null
  submitTime?: RawDate
  aiReportMarkdown?: string | null
  teacherRemark?: string | null
  aiSuggestedTotalScore?: number | null
}

interface DetailItem {
  id?: number
  questionNo?: number | string
  questionType?: string
  stemSnapshot?: string
  standardAnswer?: string
  studentAnswer?: string
  imageUrlsJson?: string
  optionsJson?: string
  recognizedText?: string
  visionConfidence?: number | null
  fullScore?: number | null
  aiSuggestedScore?: number | null
  score?: number | null
  isCorrect?: number | null
  aiComment?: string
}

interface ImageItem {
  id?: number
  questionNo?: string | null
  imageUrl?: string
}

interface HomeworkReportData {
  submission?: SubmissionEntity
  assignmentTitle?: string
  contentSnapshot?: string
  details?: DetailItem[]
  images?: ImageItem[]
  examMode?: boolean
}

const props = withDefaults(defineProps<{
  report: HomeworkReportData | null
  studentName?: string
  showStudentName?: boolean
  showSubmissionId?: boolean
  role?: 'student' | 'teacher'
  streamingContent?: string
  highlightQuestionNo?: string | number
}>(), {
  studentName: '',
  showStudentName: false,
  showSubmissionId: true,
  role: 'student',
  streamingContent: '',
  highlightQuestionNo: '',
})

const md = new MarkdownIt({ breaks: true, html: true })
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const SERVER_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const normalizeImageUrl = (url?: string) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:image')) return url
  return `${SERVER_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`
}

const renderMd = (text: string) => {
  const normalized = props.role === 'student'
    ? text.replace(/辅导建议/g, '学习建议')
    : text
  return md.render(normalized || '')
}

const isExamMode = computed(() => props.report?.examMode === true)
const role = computed(() => props.role)
const isReviewPending = computed(() => props.report?.submission?.submitStatus === 'review_pending')
const displayScore = computed(() => {
  if (isReviewPending.value && props.role === 'student') return '--'
  return props.report?.submission?.totalScore ?? '--'
})
const scoreLabel = computed(() => isReviewPending.value && props.role === 'student' ? '待批改' : '分')
const displayCorrect = computed(() => isReviewPending.value && props.role === 'student' ? '--' : (props.report?.submission?.correctCount ?? '--'))
const displayWrong = computed(() => isReviewPending.value && props.role === 'student' ? '--' : (props.report?.submission?.wrongCount ?? '--'))
const detailItems = computed(() => props.report?.details || [])
const totalPossibleScore = computed(() => {
  const total = detailItems.value.reduce((sum, item) => sum + (Number(item.fullScore) || 0), 0)
  return total > 0 ? total : null
})
const scoreStateClass = computed(() => {
  if (isReviewPending.value) return 'is-pending'
  const score = Number(props.report?.submission?.totalScore ?? NaN)
  if (Number.isNaN(score)) return 'is-pending'
  const ratio = totalPossibleScore.value ? score / totalPossibleScore.value : score / 100
  if (ratio >= 0.8) return 'is-strong'
  if (ratio >= 0.6) return 'is-pass'
  return 'is-risk'
})

const imageItems = computed(() => props.report?.images || [])
const normalizedImageItems = computed(() =>
  imageItems.value
    .map(image => ({ ...image, imageUrl: normalizeImageUrl(image.imageUrl) }))
    .filter(image => image.imageUrl)
)

const parseDetailImageUrls = (raw: string | undefined): string[] => {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.map(normalizeImageUrl).filter(Boolean) : []
  } catch {
    return []
  }
}

interface DetailOption {
  label: string
  text: string
}

const parseDetailOptions = (raw: string | undefined): DetailOption[] => {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .map((item: any) => ({
        label: String(item?.label ?? item?.key ?? '').trim(),
        text: String(item?.text ?? item?.content ?? item?.value ?? '').trim()
      }))
      .filter((item: DetailOption) => item.label && item.text)
  } catch {
    return []
  }
}

const isPendingImageDetail = (item: DetailItem) =>
  isReviewPending.value && item.score == null && parseDetailImageUrls(item.imageUrlsJson).length > 0

const detailScoreText = (item: DetailItem) => {
  if (isPendingImageDetail(item)) return '待教师批改'
  const score = item.score
  if (score == null && item.fullScore == null) return '待确认'
  if (item.fullScore == null) return `${score ?? '--'} 分`
  return `${score ?? '--'} / ${item.fullScore} 分`
}

const detailStateClass = (item: DetailItem) => {
  if (isPendingImageDetail(item)) return 'is-pending'
  if (item.isCorrect === 1 || (item.score != null && item.fullScore != null && item.score >= item.fullScore)) return 'is-correct'
  if (item.isCorrect === 0 || (item.score != null && item.score <= 0)) return 'is-wrong'
  return 'is-neutral'
}

const normalizeAnswerToken = (value: unknown) =>
  String(value ?? '')
    .trim()
    .toUpperCase()
    .replace(/^选项\s*/, '')
    .replace(/[,，、\s]+/g, '')

const optionStateClass = (item: DetailItem, label: string) => {
  const option = normalizeAnswerToken(label)
  const student = normalizeAnswerToken(item.studentAnswer)
  const standard = normalizeAnswerToken(item.standardAnswer)
  return {
    'is-selected': !!option && student.includes(option),
    'is-reference': !!option && standard.includes(option)
  }
}

const answerStateClass = (item: DetailItem) => ({
  'is-correct-answer': detailStateClass(item) === 'is-correct',
  'is-wrong-answer': detailStateClass(item) === 'is-wrong',
  'is-pending-answer': detailStateClass(item) === 'is-pending'
})

const questionTypeText = (type?: string) => {
  const map: Record<string, string> = {
    radio: '单选题',
    checkbox: '多选题',
    judge: '判断题',
    fill: '填空题',
    text: '简答题',
    image: '图片题'
  }
  return map[String(type || '')] || '未知题型'
}

const studentAnswerText = (item: DetailItem) => {
  const answer = String(item.studentAnswer || '').trim()
  if (answer) return answer
  if (parseDetailImageUrls(item.imageUrlsJson).length) return '见图片作答'
  return '（未作答）'
}

const displayAiComment = (item: DetailItem) => {
  if (props.role === 'student') return ''
  const comment = String(item.aiComment || '')
    .replace(/<!--STATS:\{.*?\}-->/g, '')
    .replace(/\*\*/g, '')
    .trim()
  if (!comment) return ''
  if (studentAnswerText(item) !== '（未作答）' && comment.includes('未作答')) return ''
  return comment
}

const normalizedHighlightNo = computed(() => normalizeQuestionNo(props.highlightQuestionNo))

const normalizeQuestionNo = (value: string | number | null | undefined) =>
  String(value ?? '').trim().replace(/^第/, '').replace(/题$/, '')

const isHighlighted = (item: DetailItem) => {
  const target = normalizedHighlightNo.value
  return !!target && normalizeQuestionNo(item.questionNo) === target
}

const scrollToHighlighted = async () => {
  if (!normalizedHighlightNo.value) return
  await nextTick()
  document
    .querySelector('.homework-report-panel .detail-row.is-highlighted')
    ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

watch(
  () => [props.highlightQuestionNo, detailItems.value.length],
  () => {
    scrollToHighlighted()
  }
)

onMounted(scrollToHighlighted)

const reportMarkdown = computed(() =>
  props.streamingContent?.trim()
    ? props.streamingContent
    : (props.report?.submission?.aiReportMarkdown || '')
)

const statusText = computed(() => {
  const s = props.report?.submission?.submitStatus
  if (s === 'review_pending') return '待教师批改'
  if (s === 'completed') return '批改完成'
  if (s === 'judging') return '待教师批改'
  if (s === 'submitted') return isExamMode.value ? '待批阅' : '已提交'
  if (s === 'failed') return '批改失败'
  return '未完成'
})

const statusClass = computed(() => {
  const s = props.report?.submission?.submitStatus
  if (s === 'review_pending') return 'pending'
  if (s === 'completed') return 'completed'
  if (s === 'failed') return 'failed'
  if (s === 'judging' || s === 'submitted') return 'pending'
  return 'plain'
})

function parseDate(value: RawDate): Date | null {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function formatDateTime(value: RawDate): string {
  const date = parseDate(value)
  if (!date) return '--'
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}
</script>

<style scoped>
.homework-report-panel {
  --text-strong: #111827;
  --text-main: #334155;
  --text-muted: #64748b;
  --line: #dce3ea;
  --line-soft: #eef2f6;
  --panel: #ffffff;
  --paper: #f7f9fc;
  --blue: #2563eb;
  --green: #16a34a;
  --red: #dc2626;
  --amber: #d97706;
  display: flex;
  flex-direction: column;
  gap: 22px;
  color: var(--text-main);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.report-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  align-items: stretch;
  gap: 22px;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--paper);
}

.header-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.header-kicker,
.header-meta,
.score-detail,
.detail-titleline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid transparent;
}

.status-tag.completed { background: #ecfdf3; color: #128047; border-color: #b8e7c9; }
.status-tag.pending { background: #fff7ed; color: #b45309; border-color: #fed7aa; }
.status-tag.failed { background: #fff1f2; color: #be123c; border-color: #fecdd3; }
.status-tag.plain { background: #f1f5f9; color: #475569; border-color: #dbe4ee; }

.report-title {
  margin: 12px 0 8px;
  color: var(--text-strong);
  font-size: 24px;
  font-weight: 800;
  line-height: 1.3;
  letter-spacing: 0;
}

.meta-item {
  color: var(--text-muted);
  font-size: 14px;
}

.meta-item.student-name {
  color: #0f766e;
  font-weight: 700;
}

.score-block {
  width: 180px;
  min-width: 0;
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  display: grid;
  grid-template-columns: auto auto;
  grid-template-areas:
    "number label"
    "detail detail";
  align-content: center;
  justify-content: end;
  column-gap: 6px;
  row-gap: 8px;
}

.score-number {
  grid-area: number;
  color: var(--text-strong);
  font-size: 52px;
  font-weight: 900;
  line-height: .9;
  letter-spacing: 0;
}

.score-label {
  grid-area: label;
  align-self: end;
  padding-bottom: 3px;
  color: var(--text-muted);
  font-size: 16px;
  font-weight: 700;
}

.score-detail {
  grid-area: detail;
  justify-content: flex-end;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.score-block.is-strong .score-number,
.score-detail .correct {
  color: var(--green);
}

.score-block.is-risk .score-number,
.score-detail .wrong {
  color: var(--red);
}

.score-block.is-pass .score-number {
  color: #0f766e;
}

.score-block.is-pending .score-number {
  color: var(--amber);
}

.teacher-remark,
.question-details,
.submission-images,
.report-body {
  padding-top: 2px;
}

.body-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #475569;
  font-size: 14px;
  font-weight: 800;
}

.body-label::before {
  content: "";
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: #0f766e;
}

.remark-content {
  position: relative;
  padding: 16px 18px 16px 20px;
  border: 1px solid #f1d89a;
  border-left: 5px solid #eab308;
  border-radius: 8px;
  background: #fffdf2;
  color: #253244;
  font-size: 15px;
  line-height: 1.9;
}

.detail-table {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-row {
  position: relative;
  padding: 16px 18px 18px;
  border: 1px solid var(--line);
  border-left-width: 5px;
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.detail-row.is-correct { border-left-color: var(--green); }
.detail-row.is-wrong { border-left-color: var(--red); }
.detail-row.is-pending { border-left-color: var(--amber); }
.detail-row.is-neutral { border-left-color: #94a3b8; }

.detail-row.is-highlighted {
  border-color: #93c5fd;
  border-left-color: var(--blue);
  background: #f8fbff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.detail-no {
  color: var(--text-strong);
  font-size: 17px;
  font-weight: 900;
}

.detail-type {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.detail-type.image-type {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.detail-score {
  flex: 0 0 auto;
  color: #5b21b6;
  font-size: 16px;
  font-weight: 900;
}

.detail-row.is-correct .detail-score { color: var(--green); }
.detail-row.is-wrong .detail-score { color: var(--red); }
.detail-row.is-pending .detail-score { color: var(--amber); }

.detail-stem {
  margin-bottom: 12px;
  color: #334155;
  font-size: 15px;
  line-height: 1.75;
}

.detail-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 14px;
}

.detail-option {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 8px;
  align-items: flex-start;
  min-height: 42px;
  padding: 9px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fbfdff;
  color: #334155;
  font-size: 14px;
  line-height: 1.45;
}

.detail-option.is-selected {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.detail-option.is-reference {
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.detail-option.is-selected.is-reference {
  box-shadow: inset 0 0 0 1px rgba(22, 163, 74, 0.25);
}

.option-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #e0e7ff;
  color: #1d4ed8;
  font-weight: 900;
}

.detail-option.is-reference .option-label {
  background: #bbf7d0;
  color: #15803d;
}

.detail-option.is-selected:not(.is-reference) .option-label {
  background: #dbeafe;
  color: #1d4ed8;
}

.option-text {
  min-width: 0;
  word-break: break-word;
}

.detail-answer-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 4px;
}

.detail-col {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.col-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.col-value {
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid #dbe4ee;
  border-radius: 6px;
  background: #f8fafc;
  color: var(--text-strong);
  font-size: 15px;
  line-height: 1.45;
  word-break: break-word;
}

.col-value.ref,
.col-value.is-correct-answer {
  border-color: #a7f3d0;
  background: #ecfdf5;
  color: #15803d;
  font-weight: 800;
}

.col-value.is-wrong-answer {
  border-color: #fecaca;
  background: #fff1f2;
  color: #be123c;
  font-weight: 800;
}

.col-value.is-pending-answer {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #b45309;
  font-weight: 800;
}

.detail-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.detail-image-card {
  display: grid;
  gap: 8px;
  width: 146px;
  padding: 7px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.detail-preview-image,
.submission-preview-image {
  overflow: hidden;
  border-radius: 6px;
  background: #fff;
}

.detail-preview-image :deep(.ant-image-img),
.submission-preview-image :deep(.ant-image-img) {
  object-fit: cover;
  display: block;
}

.detail-comment {
  margin-top: 12px;
  padding: 9px 11px;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  line-height: 1.65;
}

.review-pending-note {
  padding: 12px 14px;
  border: 1px solid #bfdbfe;
  border-left: 5px solid var(--blue);
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 700;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-chip {
  display: grid;
  gap: 8px;
  width: 172px;
  padding: 8px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.doc-style {
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  color: #334155;
  font-size: 14px;
  line-height: 1.85;
}

.doc-style :deep(h1) {
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--line-soft);
  color: var(--text-strong);
  font-size: 18px;
  font-weight: 900;
}

.doc-style :deep(h2) {
  margin: 18px 0 8px;
  color: #1f2937;
  font-size: 16px;
  font-weight: 800;
}

.doc-style :deep(h3) {
  margin: 14px 0 6px;
  color: #334155;
  font-size: 14px;
  font-weight: 800;
}

.doc-style :deep(p) {
  margin: 0 0 10px;
}

.doc-style :deep(ul),
.doc-style :deep(ol) {
  margin: 6px 0 10px;
  padding-left: 20px;
}

.doc-style :deep(li) {
  margin-bottom: 4px;
}

.doc-style :deep(strong) {
  color: #1f2937;
  font-weight: 800;
}

@media (max-width: 720px) {
  .report-header {
    grid-template-columns: 1fr;
  }

  .score-block {
    width: 100%;
    justify-content: start;
  }

  .detail-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-score {
    margin-left: 0;
  }

  .detail-options,
  .detail-answer-row {
    grid-template-columns: 1fr;
  }
}
</style>
