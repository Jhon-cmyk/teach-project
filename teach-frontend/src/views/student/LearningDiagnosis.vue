<template>
  <main class="diagnosis-page">
    <a-spin :spinning="loading">
      <div class="diagnosis-shell">
        <header class="page-heading">
          <div>
            <h1>我的学习画像</h1>
            <p>根据你近 14 天的课程、练习和错题记录持续更新</p>
          </div>
          <button
            class="icon-button"
            type="button"
            title="刷新学习画像"
            aria-label="刷新学习画像"
            :disabled="loading"
            @click="loadProfile"
          >
            <ReloadOutlined />
          </button>
        </header>

        <section class="overview-panel" :class="riskClass">
          <div class="overview-copy">
            <div class="status-row">
              <span class="status-chip">{{ insight.riskLabel }}</span>
              <span>{{ insight.trendLabel }}</span>
            </div>
            <h2>{{ insight.title }}</h2>
            <p>{{ insight.body }}</p>
          </div>

          <dl class="metric-list">
            <div class="metric-primary">
              <dt>综合掌握</dt>
              <dd>{{ normalizeScore(insight.overallScore) }}<small>分</small></dd>
            </div>
            <div>
              <dt>薄弱点</dt>
              <dd>{{ insight.weakPointCount || 0 }}</dd>
            </div>
            <div>
              <dt>错题</dt>
              <dd>{{ insight.wrongQuestionCount || 0 }}</dd>
            </div>
          </dl>
        </section>

        <section class="diagnosis-body">
          <div class="main-stack">
            <section class="content-section action-section">
              <div class="section-heading">
                <div>
                  <h2>给你的学习建议</h2>
                  <p>结合你的薄弱点和近期学习表现，按优先级整理。</p>
                </div>
                <span>{{ actionPlans.length }} 项</span>
              </div>

              <div v-if="actionPlans.length" class="action-list">
                <article
                  v-for="plan in actionPlans.slice(0, 3)"
                  :key="plan.priority + '-' + plan.title"
                  class="action-row"
                >
                  <span class="action-index">{{ plan.priority }}</span>
                  <div class="action-copy">
                    <div>
                      <h3>{{ plan.title }}</h3>
                      <span>{{ plan.minutes || 15 }} 分钟</span>
                    </div>
                    <p>{{ plan.reason }}</p>
                  </div>
                  <button
                    class="primary-action"
                    type="button"
                    :disabled="creatingPracticeKey === plan.target"
                    @click="goToAction(plan)"
                  >
                    {{ creatingPracticeKey === plan.target ? '正在准备习题' : (plan.actionText || '开始处理') }}
                    <ArrowRightOutlined />
                  </button>
                </article>
              </div>
              <div v-else class="empty-state">完成一次练习后，这里会生成可执行的学习建议。</div>
            </section>

            <section class="content-section">
              <div class="section-heading">
                <div>
                  <h2>你需要关注的知识点</h2>
                  <p>这些内容掌握得还不够稳定，建议有针对性地巩固。</p>
                </div>
                <span>{{ weakPoints.length }} 个</span>
              </div>

              <div v-if="weakPoints.length" class="weak-list">
                <article
                  v-for="item in weakPoints.slice(0, 5)"
                  :key="item.knowledgeName + '-' + (item.chapterId || '')"
                  class="weak-row"
                >
                  <div class="weak-copy">
                    <div class="weak-title">
                      <h3>{{ item.knowledgeName || '未命名知识点' }}</h3>
                      <span class="weak-status">{{ statusLabel(item.status) }}</span>
                    </div>
                    <p>{{ item.evidenceSummary || '暂无详细学习证据。' }}</p>
                  </div>
                  <div class="mastery">
                    <strong>{{ normalizeScore(item.masteryScore) }}%</strong>
                    <div class="mastery-track">
                      <span :style="{ width: normalizeScore(item.masteryScore) + '%' }"></span>
                    </div>
                  </div>
                  <button
                    class="text-action"
                    type="button"
                    :disabled="creatingPracticeKey === item.knowledgeName"
                    @click="goToWeakPoint(item)"
                  >
                    {{ creatingPracticeKey === item.knowledgeName ? '正在准备' : '专项练习' }}
                    <ArrowRightOutlined />
                  </button>
                </article>
              </div>
              <div v-else class="empty-state">暂未发现明显薄弱点，继续保持当前学习节奏。</div>
            </section>
          </div>

          <aside class="side-stack">
            <section class="side-section">
              <div class="side-heading">
                <h2>你的学习偏好</h2>
                <strong>{{ preferenceLabel }}</strong>
              </div>
              <p class="preference-summary">
                {{ profile?.preference?.summary || '记录不足，暂按混合学习方式安排。' }}
              </p>
              <div class="preference-bars">
                <div v-for="item in preferenceBars" :key="item.label" class="preference-row">
                  <div>
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                  <div class="preference-track">
                    <span :style="{ width: item.percent + '%' }"></span>
                  </div>
                </div>
              </div>
            </section>

            <section class="side-section wrong-section">
              <div class="side-heading">
                <h2>你的近期错题</h2>
                <span>{{ wrongQuestions.length }} 条</span>
              </div>
              <div v-if="wrongQuestions.length" class="wrong-list">
                <article
                  v-for="item in wrongQuestions.slice(0, 3)"
                  :key="item.submissionId + '-' + (item.detailId || '')"
                >
                  <strong>{{ item.assignmentTitle || '练习错题' }}</strong>
                  <p>{{ item.aiComment || item.stemSnapshot || '该错题暂无解析摘要。' }}</p>
                  <button
                    v-if="item.actionUrl"
                    class="text-action"
                    type="button"
                    @click="goToPath(item.actionUrl)"
                  >
                    {{ item.actionLabel || '回看错题' }}
                    <ArrowRightOutlined />
                  </button>
                </article>
              </div>
              <div v-else class="empty-state compact">完成作业或测验后，错题会出现在这里。</div>
            </section>
          </aside>
        </section>
      </div>
    </a-spin>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowRightOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { createPersonalPractice, fetchMyLearningProfile, type StudentLearningProfile } from '@/api/learning'

type ActionPlanItem = NonNullable<StudentLearningProfile['actionPlans']>[number]
type WeakPointItem = StudentLearningProfile['weakPoints'][number]

const router = useRouter()
const loading = ref(false)
const profile = ref<StudentLearningProfile | null>(null)
const creatingPracticeKey = ref('')

const fallbackInsight = {
  title: '画像正在积累真实证据',
  body: '完成学习、作业、测验或练习后，系统会结合真实记录生成诊断结论。',
  riskLevel: 'medium',
  riskLabel: '证据不足',
  overallScore: 72,
  weakPointCount: 0,
  wrongQuestionCount: 0,
  confidence: 30,
  confidenceLabel: '证据偏少',
  trendLabel: '暂无近期趋势',
  recentActivityCount: 0
}

const insight = computed(() => profile.value?.insight || fallbackInsight)
const weakPoints = computed(() => profile.value?.weakPoints || [])
const wrongQuestions = computed(() => profile.value?.wrongQuestions || [])
const actionPlans = computed(() => profile.value?.actionPlans || [])
const riskClass = computed(() => `risk-${insight.value.riskLevel || 'medium'}`)

const preferenceLabel = computed(() => {
  const dominant = profile.value?.preference?.dominantType
  if (dominant === 'video') return '视频讲解更有效'
  if (dominant === 'practice') return '练习转化更明显'
  if (dominant === 'text') return '图文复盘更适合'
  if (dominant === 'discussion') return '问答讨论更活跃'
  if (dominant === 'ai') return 'AI 分步提示使用多'
  return '偏好暂不明显'
})

const preferenceBars = computed(() => {
  const preference = profile.value?.preference
  const rows = [
    { label: '视频', value: Number(preference?.videoScore || 0) },
    { label: '图文', value: Number(preference?.textScore || 0) },
    { label: '练习', value: Number(preference?.practiceScore || 0) },
    { label: '问答', value: Number(preference?.discussionScore || 0) },
    { label: 'AI', value: Number(preference?.aiScore || 0) }
  ]
  const max = Math.max(1, ...rows.map(item => item.value))
  return rows.map(item => ({
    ...item,
    percent: Math.round((item.value / max) * 100)
  }))
})

const normalizeScore = (value?: number | null) => {
  const score = Number(value ?? 0)
  if (Number.isNaN(score)) return 0
  return Math.max(0, Math.min(100, Math.round(score)))
}

const statusLabel = (status?: string) => {
  if (status === 'mastered') return '已掌握'
  if (status === 'partial') return '不稳定'
  if (status === 'not_mastered') return '需补强'
  return '待观察'
}

const goToPath = (url?: string) => {
  if (!url) {
    message.warning('该学习内容暂时没有可用入口')
    return
  }
  const target = router.resolve(url)
  if (!target.matched.length) {
    message.warning('该学习入口暂时不可用')
    return
  }
  router.push(target)
}

const openPersonalPractice = async (item: WeakPointItem) => {
  const key = item.knowledgeName || '薄弱知识点'
  if (creatingPracticeKey.value) return
  creatingPracticeKey.value = key
  try {
    const result = await createPersonalPractice({
      courseId: item.courseId,
      chapterId: item.chapterId,
      knowledgeName: key
    })
    message.success(`已从${result.sourceLabel || '个性化题库'}准备 ${result.questionCount || 0} 道题`)
    await router.push(`/student/homework/${result.assignmentId}?from=diagnosis&personal=1`)
  } catch (error: any) {
    message.error(error?.message || '专项练习准备失败，请稍后重试')
  } finally {
    creatingPracticeKey.value = ''
  }
}

const goToAction = async (plan: ActionPlanItem) => {
  const matchedWeakPoint = weakPoints.value.find(item => item.knowledgeName === plan.target)
  if (plan.actionType === 'student_practice' && matchedWeakPoint) {
    await openPersonalPractice(matchedWeakPoint)
    return
  }
  goToPath(plan.actionUrl)
}

const goToWeakPoint = async (item: WeakPointItem) => {
  await openPersonalPractice(item)
}

const loadProfile = async () => {
  loading.value = true
  try {
    profile.value = await fetchMyLearningProfile({ days: 14 })
  } catch (error: any) {
    message.error(error?.message || '学习画像加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.diagnosis-page {
  min-height: calc(100vh - 70px);
  padding: 24px 0 56px;
  background: #f5f7fa;
  color: #172033;
}

.diagnosis-shell {
  width: min(1120px, calc(100vw - 48px));
  margin: 0 auto;
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
}

.page-heading h1 {
  margin: 0;
  color: #101828;
  font-size: 26px;
  line-height: 1.3;
  font-weight: 800;
}

.page-heading p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 14px;
}

.icon-button {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border: 1px solid #d8e0ea;
  border-radius: 7px;
  background: #ffffff;
  color: #475467;
  cursor: pointer;
  transition: border-color 180ms ease, color 180ms ease, background 180ms ease;
}

.icon-button:hover:not(:disabled) {
  border-color: #2563eb;
  color: #1d4ed8;
  background: #f8fbff;
}

.icon-button:focus-visible,
.primary-action:focus-visible,
.text-action:focus-visible {
  outline: 3px solid rgba(37, 99, 235, 0.22);
  outline-offset: 2px;
}

.icon-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.overview-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 32px;
  align-items: center;
  padding: 24px 26px;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  background: #ffffff;
}

.overview-copy {
  min-width: 0;
}

.status-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  color: #667085;
  font-size: 13px;
  font-weight: 600;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 3px 9px;
  border-radius: 5px;
  background: #fff7ed;
  color: #c2410c;
}

.risk-high .status-chip {
  background: #fff1f2;
  color: #be123c;
}

.risk-low .status-chip {
  background: #ecfdf5;
  color: #047857;
}

.overview-copy h2 {
  margin: 12px 0 7px;
  color: #101828;
  font-size: 22px;
  line-height: 1.35;
  font-weight: 800;
  text-wrap: balance;
}

.overview-copy p {
  max-width: 68ch;
  margin: 0;
  color: #475467;
  font-size: 14px;
  line-height: 1.7;
  text-wrap: pretty;
}

.metric-list {
  display: flex;
  align-items: stretch;
  margin: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: #f8fafc;
}

.metric-list div {
  min-width: 92px;
  padding: 13px 16px;
  border-left: 1px solid #e2e8f0;
}

.metric-list div:first-child {
  border-left: 0;
}

.metric-list dt {
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.metric-list dd {
  margin: 5px 0 0;
  color: #101828;
  font-size: 24px;
  line-height: 1;
  font-weight: 800;
}

.metric-list small {
  margin-left: 2px;
  color: #667085;
  font-size: 12px;
}

.diagnosis-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: 0;
  margin-top: 16px;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  background: #ffffff;
}

.main-stack {
  min-width: 0;
  padding: 0 26px;
}

.content-section {
  padding: 24px 0;
  border-top: 1px solid #e6ebf1;
}

.content-section:first-child {
  border-top: 0;
}

.section-heading,
.side-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.section-heading {
  margin-bottom: 16px;
}

.section-heading h2,
.side-heading h2 {
  margin: 0;
  color: #101828;
  font-size: 18px;
  line-height: 1.35;
  font-weight: 800;
}

.section-heading p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.55;
}

.section-heading > span,
.side-heading > span {
  flex-shrink: 0;
  color: #667085;
  font-size: 13px;
  font-weight: 700;
}

.action-list,
.weak-list,
.wrong-list,
.preference-bars {
  display: grid;
}

.action-row {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 15px 0;
  border-top: 1px solid #edf0f4;
}

.action-row:first-child {
  padding-top: 0;
  border-top: 0;
}

.action-row:last-child {
  padding-bottom: 0;
}

.action-index {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 800;
}

.action-copy {
  min-width: 0;
}

.action-copy > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.action-copy h3,
.weak-title h3 {
  margin: 0;
  color: #1d2939;
  font-size: 15px;
  line-height: 1.45;
  font-weight: 750;
}

.action-copy > div > span {
  flex-shrink: 0;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.action-copy p,
.weak-copy p,
.wrong-list p,
.preference-summary {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
}

.primary-action,
.text-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 0;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.primary-action {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 6px;
  background: #2563eb;
  color: #ffffff;
  font-size: 13px;
  transition: background 180ms ease;
}

.primary-action:hover {
  background: #1d4ed8;
}

.primary-action:disabled,
.text-action:disabled {
  opacity: 0.6;
  cursor: wait;
}

.text-action {
  padding: 4px 0;
  background: transparent;
  color: #1d4ed8;
  font-size: 13px;
}

.text-action:hover {
  color: #1e40af;
}

.weak-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 104px auto;
  gap: 18px;
  align-items: center;
  padding: 15px 0;
  border-top: 1px solid #edf0f4;
}

.weak-row:first-child {
  padding-top: 0;
  border-top: 0;
}

.weak-row:last-child {
  padding-bottom: 0;
}

.weak-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.weak-status {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 2px 7px;
  border-radius: 4px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 11px;
  font-weight: 800;
}

.mastery {
  text-align: right;
}

.mastery strong {
  color: #1d2939;
  font-size: 15px;
}

.mastery-track,
.preference-track {
  height: 6px;
  overflow: hidden;
  margin-top: 7px;
  border-radius: 999px;
  background: #e7ecf2;
}

.mastery-track span,
.preference-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #2563eb;
}

.side-stack {
  min-width: 0;
  border-left: 1px solid #e6ebf1;
}

.side-section {
  padding: 24px 22px;
  border-top: 1px solid #e6ebf1;
}

.side-section:first-child {
  border-top: 0;
}

.side-heading {
  align-items: baseline;
  margin-bottom: 12px;
}

.side-heading strong {
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 800;
  text-align: right;
}

.preference-summary {
  margin-bottom: 16px;
}

.preference-bars {
  gap: 11px;
}

.preference-row > div:first-child {
  display: flex;
  justify-content: space-between;
  color: #475467;
  font-size: 12px;
  font-weight: 700;
}

.preference-track {
  height: 5px;
  margin-top: 5px;
}

.wrong-list article {
  padding: 13px 0;
  border-top: 1px solid #edf0f4;
}

.wrong-list article:first-child {
  padding-top: 0;
  border-top: 0;
}

.wrong-list article:last-child {
  padding-bottom: 0;
}

.wrong-list article > strong {
  display: block;
  color: #1d2939;
  font-size: 14px;
  line-height: 1.45;
}

.wrong-list p {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.wrong-list .text-action {
  margin-top: 7px;
}

.empty-state {
  padding: 24px 16px;
  border-radius: 7px;
  background: #f8fafc;
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}

.empty-state.compact {
  padding: 18px 12px;
  text-align: left;
}

@media (max-width: 900px) {
  .overview-panel {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .metric-list {
    width: fit-content;
  }

  .diagnosis-body {
    grid-template-columns: 1fr;
  }

  .side-stack {
    display: grid;
    grid-template-columns: 1fr 1fr;
    border-top: 1px solid #e6ebf1;
    border-left: 0;
  }

  .side-section {
    border-top: 0;
    border-left: 1px solid #e6ebf1;
  }

  .side-section:first-child {
    border-left: 0;
  }
}

@media (max-width: 640px) {
  .diagnosis-page {
    padding: 18px 0 36px;
  }

  .diagnosis-shell {
    width: min(100%, calc(100vw - 28px));
  }

  .overview-panel,
  .main-stack {
    padding-right: 18px;
    padding-left: 18px;
  }

  .overview-panel {
    padding-top: 20px;
    padding-bottom: 20px;
  }

  .metric-list {
    width: 100%;
  }

  .metric-list div {
    min-width: 0;
    flex: 1;
    padding: 11px 10px;
  }

  .action-row,
  .weak-row {
    grid-template-columns: 28px minmax(0, 1fr);
  }

  .action-row .primary-action,
  .weak-row .text-action {
    grid-column: 2;
    justify-self: start;
  }

  .weak-row {
    grid-template-columns: minmax(0, 1fr) 88px;
  }

  .weak-row .text-action {
    grid-column: 1;
  }

  .side-stack {
    grid-template-columns: 1fr;
  }

  .side-section {
    border-left: 0;
    border-top: 1px solid #e6ebf1;
  }

  .side-section:first-child {
    border-top: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .icon-button,
  .primary-action {
    transition: none;
  }
}
</style>
