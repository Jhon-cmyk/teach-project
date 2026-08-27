<template>
  <a-modal
    :open="open"
    width="1080px"
    :footer="null"
    centered
    destroyOnClose
    class="student-portrait-modal"
    @update:open="emit('update:open', $event)"
  >
    <template #title>
      <div class="portrait-modal-title">
        <idcard-outlined />
        <span>学生画像</span>
      </div>
    </template>

    <a-spin :spinning="loading">
      <div v-if="student" class="portrait-shell">
        <section class="portrait-hero">
          <div class="student-identity">
            <a-avatar :size="76" :src="avatarUrl" class="student-avatar">
              <user-outlined />
            </a-avatar>
            <div class="identity-text">
              <div class="name-row">
                <h2>{{ displayName }}</h2>
                <span class="status-badge" :class="portraitStatus.tone">{{ portraitStatus.label }}</span>
              </div>
              <div class="meta-row">
                <span>{{ classInfo?.name || trajectory?.className || '未选择班级' }}</span>
                <span>学号 {{ trajectory?.studentNo || student.studentNo || '--' }}</span>
                <span>最近记录 {{ trajectory?.fatigue?.latestRecordDate || '暂无' }}</span>
              </div>
            </div>
          </div>

          <div class="portrait-score">
            <div class="score-value">{{ portraitScore }}</div>
            <div class="score-label">学习指数</div>
          </div>
        </section>

        <section class="metric-grid">
          <div v-for="metric in metrics" :key="metric.label" class="metric-card" :class="metric.tone">
            <component :is="metric.icon" class="metric-icon" />
            <div>
              <div class="metric-label">{{ metric.label }}</div>
              <div class="metric-value">{{ metric.value }}</div>
              <div class="metric-note">{{ metric.note }}</div>
            </div>
          </div>
        </section>

        <section class="portrait-main">
          <div class="left-column">
            <div class="panel-card homework-panel priority-panel">
              <div class="panel-head compact">
                <div>
                  <h3>未完成作业</h3>
                  <p>优先处理会影响学习节奏的任务</p>
                </div>
                <span class="data-source-chip">作业发布/提交记录</span>
              </div>

              <div v-if="unfinishedHomework.length" class="homework-list">
                <div v-for="item in unfinishedHomework" :key="item.assignmentId" class="homework-item">
                  <div class="homework-title">{{ item.title || '未命名作业' }}</div>
                  <div class="homework-meta">
                    <span>{{ item.questionCount ?? '--' }} 题</span>
                    <span>{{ item.totalScore ?? '--' }} 分</span>
                    <span>{{ formatDateTimeText(item.deadline) }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="empty-block small">当前没有未完成作业</div>
            </div>

            <div class="panel-card ability-panel">
              <div class="panel-head">
                <div>
                  <h3>核心能力画像</h3>
                  <p>基于知识图谱进度、作业和编程提交综合推断</p>
                </div>
              </div>
              <div v-if="hasRadarData" ref="radarChartRef" class="radar-chart"></div>
              <div v-else class="empty-block">暂无能力画像数据</div>
            </div>

            <div class="panel-card timeline-panel">
              <div class="panel-head">
                <div>
                  <h3>学习轨迹解读</h3>
                  <p>把原始记录转换成教师可读的状态线索</p>
                </div>
              </div>

              <div class="timeline-list">
                <div v-for="item in timelineItems" :key="item.title" class="timeline-item" :class="item.tone">
                  <div class="timeline-dot"></div>
                  <div class="timeline-content">
                    <div class="timeline-title">{{ item.title }}</div>
                    <div class="timeline-desc">{{ item.desc }}</div>
                  </div>
                </div>
              </div>
            </div>

            <div class="panel-card video-behavior-panel">
              <div class="panel-head">
                <div>
                  <h3>视频学习行为</h3>
                  <p>最近 {{ videoProfile?.days || 7 }} 天基于暂停、回看、跳过与辅导触发生成</p>
                </div>
                <span class="video-conclusion" :class="videoConclusionTone">{{ videoProfile?.conclusion || '学习稳定' }}</span>
              </div>

              <div v-if="videoProfile" class="video-stats">
                <div class="video-stat">
                  <span>回看</span>
                  <strong>{{ videoProfile.totalRewatchCount || 0 }}</strong>
                </div>
                <div class="video-stat">
                  <span>暂停</span>
                  <strong>{{ videoProfile.totalPauseSeconds || 0 }}s</strong>
                </div>
                <div class="video-stat">
                  <span>跳过</span>
                  <strong>{{ videoProfile.totalSkipCount || 0 }}</strong>
                </div>
                <div class="video-stat">
                  <span>辅导</span>
                  <strong>{{ videoProfile.totalInterventionCount || 0 }}</strong>
                </div>
              </div>

              <div v-if="videoWeakPoints.length" class="video-weak-list">
                <div v-for="item in videoWeakPoints" :key="item.segmentId" class="video-weak-item">
                  <div class="weak-main">
                    <div class="weak-title">{{ item.knowledgeName || '未命名知识点' }}</div>
                    <div class="weak-meta">
                      <span>{{ item.courseName || '未知课程' }}</span>
                      <span>{{ item.chapterTitle || '未知章节' }}</span>
                      <span>难度 {{ item.difficulty || '中' }}</span>
                    </div>
                  </div>
                  <div class="weak-numbers">
                    <span>回看 {{ item.rewatchCount || 0 }}</span>
                    <span>暂停 {{ item.pauseSeconds || 0 }}s</span>
                    <span>辅导 {{ item.interventionCount || 0 }}</span>
                  </div>
                  <span class="weak-tag">{{ item.conclusion || '需要关注' }}</span>
                  <div v-if="item.behaviorDetails?.length" class="behavior-detail-strip">
                    <span
                      v-for="detail in item.behaviorDetails"
                      :key="`${detail.eventType}-${detail.eventTime}-${detail.timeRange}`"
                      class="behavior-chip"
                      :class="detail.eventType"
                    >
                      <b>{{ detail.label }}</b>
                      <span>{{ detail.timeRange }}</span>
                      <em>{{ detail.eventTime }}</em>
                    </span>
                  </div>
                </div>
              </div>
              <div v-else class="empty-block small">暂无明显视频学习行为风险</div>

              <div v-if="videoProfile?.latestIntervention" class="latest-intervention">
                最近辅导：{{ videoProfile.latestIntervention.eventTime }} ·
                {{ videoProfile.latestIntervention.knowledgeName }}
              </div>
            </div>
          </div>

          <aside class="right-column">
            <div class="panel-card precision-panel">
              <div class="panel-head compact">
                <div>
                  <h3>一人一案</h3>
                  <p>近 {{ learningProfile?.days || 7 }} 天综合学习画像</p>
                </div>
              </div>

              <div v-if="learningProfile" class="precision-stack">
                <div class="preference-box">
                  <div class="preference-main">
                    <span>学习偏好</span>
                    <strong>{{ preference?.dominantType || 'balanced' }}</strong>
                  </div>
                  <p>{{ preference?.summary || '暂无明显单一偏好，建议混合推送视频、文字和练习。' }}</p>
                  <div class="preference-bars">
                    <span>视频 {{ preference?.videoScore || 0 }}</span>
                    <span>文字 {{ preference?.textScore || 0 }}</span>
                    <span>练习 {{ preference?.practiceScore || 0 }}</span>
                    <span>讨论 {{ preference?.discussionScore || 0 }}</span>
                    <span>AI {{ preference?.aiScore || 0 }}</span>
                  </div>
                </div>

                <div class="mini-section">
                  <div class="mini-title">薄弱知识点</div>
                  <div v-if="learningWeakPoints.length" class="mastery-list">
                    <div v-for="item in learningWeakPoints.slice(0, 4)" :key="`${item.knowledgeName}-${item.chapterId}`" class="mastery-item">
                      <div>
                        <strong>{{ item.knowledgeName }}</strong>
                        <p>{{ item.evidenceSummary }}</p>
                      </div>
                      <span :class="['mastery-score', item.masteryScore < 60 ? 'danger' : 'warning']">
                        {{ item.masteryScore ?? '--' }}%
                      </span>
                    </div>
                  </div>
                  <div v-else class="empty-block small">暂无明显薄弱知识点</div>
                </div>

                <div class="mini-section">
                  <div class="mini-title">推荐资源</div>
                  <div v-if="learningRecommendations.length" class="recommendation-list">
                    <div v-for="item in learningRecommendations.slice(0, 3)" :key="item.id" class="recommendation-item">
                      <div class="recommendation-title">{{ item.resourceTitle || '未命名资源' }}</div>
                      <div class="recommendation-meta">{{ item.knowledgeName }} · {{ item.resourceType || 'resource' }}</div>
                      <p>{{ item.practiceSuggestion }}</p>
                    </div>
                  </div>
                  <div v-else class="empty-block small">暂无匹配资源，请补充资源标签</div>
                </div>

                <div class="mini-section">
                  <div class="mini-title">错题回练</div>
                  <div v-if="wrongQuestions.length" class="wrong-list">
                    <div v-for="item in wrongQuestions.slice(0, 2)" :key="`${item.submissionId}-${item.questionNo}`" class="wrong-item">
                      <strong>{{ item.assignmentTitle }} · 第 {{ item.questionNo || '--' }} 题</strong>
                      <p>{{ item.aiComment || item.stemSnapshot || '暂无错因说明' }}</p>
                    </div>
                  </div>
                  <div v-else class="empty-block small">暂无近期错题记录</div>
                </div>
              </div>

              <div v-else class="empty-block small">暂无综合画像数据</div>
            </div>

            <div class="panel-card suggestion-panel">
              <div class="panel-head compact">
                <div>
                  <h3>教学建议</h3>
                  <p>下一步可直接执行的跟进动作</p>
                </div>
              </div>

              <div class="suggestion-list">
                <div v-for="item in suggestions" :key="item.title" class="suggestion-item" :class="item.tone">
                  <div class="suggestion-kicker">{{ item.kicker }}</div>
                  <div class="suggestion-title">{{ item.title }}</div>
                  <div class="suggestion-body">{{ item.body }}</div>
                </div>
              </div>
            </div>

          </aside>
        </section>
      </div>

      <div v-else class="empty-block modal-empty">请选择学生后查看画像</div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { message } from 'ant-design-vue'
import {
  AlertOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  FireOutlined,
  IdcardOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import request from '@/utils/request'
import { fetchStudentProfile } from '@/api/courseGraphStats'
import { fetchVideoLearningProfile, type VideoLearningProfile } from '@/api/videoLearning'
import { fetchStudentLearningProfile, type StudentLearningProfile } from '@/api/learning'

type StudentTrajectoryData = {
  classId: number
  className: string
  studentId: number
  studentNo: string
  studentName: string
  summary?: {
    totalStudySeconds: number
    totalStudyDurationText: string
    totalHomeworkCount: number
    completedHomeworkCount: number
    unfinishedHomeworkCount: number
    completionRate: number
    hasUnfinishedHomework: boolean
  }
  fatigue?: {
    latestRecordDate: string
    fatigueCount: number
    yawnCount: number
    noFaceCount: number
    normalCount: number
    totalDetections: number
    monitorSeconds: number
    lastStatus: string
    lastStatusText: string
    fatigueLevelText: string
  }
  unfinishedHomeworkList?: Array<{
    assignmentId: number
    title: string
    deadline: string
    questionCount: number
    totalScore: number
    assignmentType: string
  }>
}

type StudentProfileCard = {
  id: string
  name: string
  avatar: string
  completionRate: number
  masteryRate: number
  radar: { indicator: string; value: number; max: number }[]
  studyDays: { date: string; minutes: number; completed: number }[]
}

type PortraitSuggestion = {
  kicker: string
  title: string
  body: string
  tone: 'primary' | 'success' | 'warning' | 'danger'
}

const props = defineProps<{
  open: boolean
  classInfo: any | null
  student: any | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

const loading = ref(false)
const trajectory = ref<StudentTrajectoryData | null>(null)
const profile = ref<StudentProfileCard | null>(null)
const videoProfile = ref<VideoLearningProfile | null>(null)
const learningProfile = ref<StudentLearningProfile | null>(null)
const radarChartRef = ref<HTMLElement | null>(null)
let radarChart: echarts.ECharts | null = null

const displayName = computed(() => {
  return trajectory.value?.studentName || props.student?.name || profile.value?.name || '学生'
})

const avatarUrl = computed(() => {
  return profile.value?.avatar || props.student?.userAvatar || props.student?.avatar || ''
})

const summary = computed(() => trajectory.value?.summary)
const fatigue = computed(() => trajectory.value?.fatigue)
const unfinishedHomework = computed(() => trajectory.value?.unfinishedHomeworkList || [])
const hasRadarData = computed(() => Boolean(profile.value?.radar?.length))
const videoWeakPoints = computed(() => videoProfile.value?.weakPoints || [])
const preference = computed(() => learningProfile.value?.preference)
const learningWeakPoints = computed(() => learningProfile.value?.weakPoints || [])
const learningRecommendations = computed(() => learningProfile.value?.recommendations || [])
const learningAdvices = computed(() => learningProfile.value?.advices || [])
const wrongQuestions = computed(() => learningProfile.value?.wrongQuestions || [])
const videoConclusionTone = computed(() => {
  const conclusion = videoProfile.value?.conclusion || '学习稳定'
  if (conclusion.includes('疑似')) return 'danger'
  if (conclusion.includes('关注')) return 'warning'
  return 'success'
})

const portraitScore = computed(() => {
  const mastery = profile.value?.masteryRate ?? 0
  const completion = profile.value?.completionRate ?? summary.value?.completionRate ?? 0
  const unfinishedPenalty = Math.min((summary.value?.unfinishedHomeworkCount || 0) * 4, 16)
  const fatiguePenalty = isFatigueRisk.value ? 8 : 0
  return Math.max(0, Math.min(100, Math.round(mastery * 0.5 + completion * 0.5 - unfinishedPenalty - fatiguePenalty)))
})

const isFatigueRisk = computed(() => {
  const text = fatigue.value?.fatigueLevelText || fatigue.value?.lastStatusText || ''
  return text.includes('疲劳') || text.includes('离屏') || text.includes('yawn') || text.includes('no_face')
})

const portraitStatus = computed(() => {
  const score = portraitScore.value
  const unfinished = summary.value?.unfinishedHomeworkCount || 0
  if (score >= 88 && unfinished === 0 && !isFatigueRisk.value) return { label: '优秀', tone: 'success' }
  if (score >= 72 && unfinished <= 1) return { label: '稳定', tone: 'primary' }
  if (score >= 58) return { label: '需关注', tone: 'warning' }
  return { label: '风险', tone: 'danger' }
})

const metrics = computed(() => [
  {
    label: '今日学习',
    value: summary.value?.totalStudyDurationText || '0分钟',
    note: `累计 ${summary.value?.totalStudySeconds || 0} 秒`,
    tone: 'primary',
    icon: ClockCircleOutlined,
  },
  {
    label: '作业完成率',
    value: `${summary.value?.completionRate || profile.value?.completionRate || 0}%`,
    note: `已完成 ${summary.value?.completedHomeworkCount || 0}/${summary.value?.totalHomeworkCount || 0} 项`,
    tone: 'success',
    icon: CheckCircleOutlined,
  },
  {
    label: '待处理任务',
    value: `${summary.value?.unfinishedHomeworkCount || 0} 项`,
    note: summary.value?.hasUnfinishedHomework ? '建议优先跟进' : '当前节奏良好',
    tone: (summary.value?.unfinishedHomeworkCount || 0) > 0 ? 'warning' : 'success',
    icon: AlertOutlined,
  },
  {
    label: '疲劳状态',
    value: fatigue.value?.fatigueLevelText || '暂无记录',
    note: `今日状态：${fatigue.value?.lastStatusText || '暂无记录'}`,
    tone: isFatigueRisk.value ? 'danger' : 'primary',
    icon: FireOutlined,
  },
])

const timelineItems = computed(() => {
  const items = [
    {
      title: '学习活跃',
      desc: `今日学习时长 ${summary.value?.totalStudyDurationText || '0分钟'}，最近记录 ${fatigue.value?.latestRecordDate || '暂无'}。`,
      tone: (summary.value?.totalStudySeconds || 0) > 0 ? 'primary' : 'muted',
    },
    {
      title: '作业节奏',
      desc: `完成率 ${summary.value?.completionRate || 0}%，未完成 ${summary.value?.unfinishedHomeworkCount || 0} 项。`,
      tone: (summary.value?.unfinishedHomeworkCount || 0) > 0 ? 'warning' : 'success',
    },
    {
      title: '掌握表现',
      desc: `知识掌握率 ${profile.value?.masteryRate ?? 0}%，课程完成率 ${profile.value?.completionRate ?? 0}%。`,
      tone: (profile.value?.masteryRate || 0) >= 70 ? 'success' : 'warning',
    },
    {
      title: '状态观察',
      desc: `疲劳结论：${fatigue.value?.fatigueLevelText || '暂无记录'}。`,
      tone: isFatigueRisk.value ? 'danger' : 'primary',
    },
  ]
  return items
})

const suggestions = computed<PortraitSuggestion[]>(() => {
  const result: PortraitSuggestion[] = []
  const completion = summary.value?.completionRate ?? profile.value?.completionRate ?? 0
  const mastery = profile.value?.masteryRate ?? 0
  const unfinished = summary.value?.unfinishedHomeworkCount || 0

  if (unfinished > 0) {
    result.push({
      kicker: '作业跟进',
      title: '先清理未完成任务',
      body: `当前还有 ${unfinished} 项未完成作业，建议课后单独确认原因，并给出明确补交时间。`,
      tone: 'warning',
    })
  }

  if (mastery < 60) {
    result.push({
      kicker: '知识补强',
      title: '安排低门槛专项练习',
      body: '掌握率偏低，建议先用基础题定位断点，再逐步增加综合题，避免直接进入高难任务。',
      tone: 'danger',
    })
  } else if (mastery < 75) {
    result.push({
      kicker: '巩固提升',
      title: '加强错因复盘',
      body: '掌握水平处在可提升区间，建议围绕薄弱知识点安排 5-8 题短练并及时讲评。',
      tone: 'primary',
    })
  }

  if (completion >= 85 && mastery >= 80 && unfinished === 0) {
    result.push({
      kicker: '拔高挑战',
      title: '提供进阶任务',
      body: '学习节奏和掌握情况较好，可以布置开放题或编程拓展题，保持挑战感。',
      tone: 'success',
    })
  }

  if (isFatigueRisk.value) {
    result.push({
      kicker: '状态干预',
      title: '关注课堂专注度',
      body: '最近存在疲劳或离屏风险，建议降低连续任务时长，并在课堂中增加短反馈检查。',
      tone: 'danger',
    })
  }

  learningAdvices.value.forEach((item) => {
    result.push({
      kicker: '精准干预',
      title: item.title,
      body: item.body,
      tone: item.tone,
    })
  })

  if (!result.length) {
    result.push({
      kicker: '持续观察',
      title: '保持当前学习节奏',
      body: '当前画像没有明显风险，建议维持常规练习，并在下一次测验后复查掌握变化。',
      tone: 'success',
    })
  }

  return result.slice(0, 3)
})

const renderRadarChart = async () => {
  await nextTick()
  if (!radarChartRef.value || !profile.value?.radar?.length || !props.open) return
  if (!radarChart) radarChart = echarts.init(radarChartRef.value)

  const radar = profile.value.radar
  radarChart.setOption({
    color: ['#2563eb'],
    tooltip: { trigger: 'item' },
    radar: {
      indicator: radar.map((item) => ({ name: item.indicator, max: item.max || 100 })),
      radius: '62%',
      center: ['50%', '52%'],
      axisName: { color: '#475569', fontSize: 12 },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      axisLine: { lineStyle: { color: '#dbe3ef' } },
      splitArea: { areaStyle: { color: ['#ffffff', '#f8fafc'] } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: radar.map((item) => item.value),
            name: displayName.value,
            areaStyle: { color: 'rgba(37, 99, 235, 0.16)' },
            lineStyle: { width: 2 },
            symbolSize: 5,
          },
        ],
      },
    ],
  })
  radarChart.resize()
}

const loadData = async () => {
  if (!props.open || !props.student?.id || !props.classInfo?.id) return

  loading.value = true
  trajectory.value = null
  profile.value = null
  videoProfile.value = null
  learningProfile.value = null

  try {
    const [trajectoryData, profileData, videoData, learningData] = await Promise.all([
      request.get<StudentTrajectoryData, StudentTrajectoryData>('/class/student/trajectory', {
        params: {
          classId: props.classInfo.id,
          studentId: props.student.id,
        },
        skipErrorToast: true,
      }),
      fetchStudentProfile(props.classInfo.id),
      fetchVideoLearningProfile({
        classId: props.classInfo.id,
        studentId: props.student.id,
        days: 7,
      }).catch(() => null),
      fetchStudentLearningProfile({
        classId: props.classInfo.id,
        studentId: props.student.id,
        days: 7,
      }).catch(() => null),
    ])

    trajectory.value = trajectoryData || null
    profile.value =
      (profileData?.students || []).find((item) => String(item.id) === String(props.student.id)) || null
    videoProfile.value = videoData || null
    learningProfile.value = learningData || null
    await renderRadarChart()
  } catch (error: any) {
    message.error(error?.message || '学生画像数据加载失败')
  } finally {
    loading.value = false
  }
}

const formatDateTimeText = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`
}

const handleResize = () => {
  radarChart?.resize()
}

watch(
  () => [props.open, props.classInfo?.id, props.student?.id],
  () => {
    if (props.open) {
      loadData()
    } else {
      radarChart?.dispose()
      radarChart = null
    }
  },
  { immediate: true },
)

watch(
  () => profile.value?.radar,
  () => renderRadarChart(),
)

watch(
  () => props.open,
  (visible) => {
    if (visible) window.addEventListener('resize', handleResize)
    else window.removeEventListener('resize', handleResize)
  },
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  radarChart?.dispose()
})
</script>

<style scoped>
.portrait-modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1e293b;
  font-weight: 700;
}

.student-portrait-modal :deep(.ant-modal-content) {
  border-radius: 5px;
  overflow: hidden;
}

.portrait-shell {
  max-height: 76vh;
  overflow-y: auto;
  padding: 4px 2px 10px;
  color: #0f172a;
}

.portrait-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #f8fafc;
}

.student-identity {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.student-avatar {
  flex-shrink: 0;
  border: 3px solid #fff;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.12);
  background: #e2e8f0;
}

.identity-text {
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.name-row h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.status-badge {
  padding: 4px 9px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid transparent;
}

.status-badge.success { background: #ecfdf5; color: #047857; border-color: #bbf7d0; }
.status-badge.primary { background: #eff6ff; color: #1d4ed8; border-color: #bfdbfe; }
.status-badge.warning { background: #fffbeb; color: #b45309; border-color: #fde68a; }
.status-badge.danger { background: #fef2f2; color: #dc2626; border-color: #fecaca; }

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.meta-row span {
  padding: 4px 8px;
  border-radius: 5px;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.portrait-score {
  width: 116px;
  height: 86px;
  border-radius: 5px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.score-value {
  font-size: 34px;
  line-height: 1;
  font-weight: 800;
  color: #2563eb;
}

.score-label {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.metric-card {
  display: flex;
  gap: 12px;
  min-width: 0;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #fff;
}

.metric-icon {
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 20px;
}

.metric-card.primary .metric-icon { color: #2563eb; }
.metric-card.success .metric-icon { color: #059669; }
.metric-card.warning .metric-icon { color: #d97706; }
.metric-card.danger .metric-icon { color: #dc2626; }

.metric-label,
.metric-note {
  color: #64748b;
  font-size: 12px;
}

.metric-value {
  margin: 4px 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
  word-break: break-word;
}

.portrait-main {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr);
  gap: 14px;
  margin-top: 14px;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.panel-card {
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #fff;
  padding: 18px;
}

.priority-panel {
  border-color: #fed7aa;
  background: #fffaf5;
}

.priority-panel .panel-head h3 {
  font-size: 18px;
}

.data-source-chip {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 5px;
  border: 1px solid #fed7aa;
  background: #fff7ed;
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-head.compact {
  margin-bottom: 14px;
}

.panel-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

.panel-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.radar-chart {
  width: 100%;
  height: 330px;
}

.timeline-list,
.suggestion-list,
.homework-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-item {
  position: relative;
  display: grid;
  grid-template-columns: 18px 1fr;
  gap: 10px;
}

.timeline-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 18px;
  bottom: -12px;
  width: 1px;
  background: #e2e8f0;
}

.timeline-dot {
  width: 15px;
  height: 15px;
  margin-top: 3px;
  border-radius: 50%;
  border: 3px solid #fff;
  background: #94a3b8;
  box-shadow: 0 0 0 1px #cbd5e1;
}

.timeline-item.success .timeline-dot { background: #10b981; }
.timeline-item.primary .timeline-dot { background: #2563eb; }
.timeline-item.warning .timeline-dot { background: #f59e0b; }
.timeline-item.danger .timeline-dot { background: #ef4444; }

.timeline-content {
  padding: 12px 14px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.timeline-title {
  color: #1e293b;
  font-size: 14px;
  font-weight: 700;
}

.timeline-desc {
  margin-top: 4px;
  color: #64748b;
  line-height: 1.6;
  font-size: 13px;
}

.suggestion-item {
  padding: 14px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.suggestion-item.primary { border-left: 3px solid #2563eb; }
.suggestion-item.success { border-left: 3px solid #10b981; }
.suggestion-item.warning { border-left: 3px solid #f59e0b; }
.suggestion-item.danger { border-left: 3px solid #ef4444; }

.suggestion-kicker {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.suggestion-title {
  margin-top: 4px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
}

.suggestion-body {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.65;
}

.homework-item {
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #fff;
}

.priority-panel .homework-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-color: #fed7aa;
}

.homework-title {
  color: #1e293b;
  font-size: 14px;
  font-weight: 700;
  min-width: 0;
}

.homework-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.priority-panel .homework-meta {
  flex-shrink: 0;
  justify-content: flex-end;
}

.precision-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.preference-box {
  padding: 14px;
  border-radius: 5px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
}

.preference-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preference-main span {
  color: #475569;
  font-size: 12px;
}

.preference-main strong {
  color: #1d4ed8;
  font-size: 16px;
}

.preference-box p,
.recommendation-item p,
.wrong-item p,
.mastery-item p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
}

.preference-bars {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.preference-bars span {
  padding: 4px 7px;
  border-radius: 5px;
  border: 1px solid #bfdbfe;
  background: #fff;
  color: #2563eb;
  font-size: 12px;
}

.mini-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mini-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.mastery-list,
.recommendation-list,
.wrong-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mastery-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #fff;
}

.mastery-item strong,
.wrong-item strong {
  color: #1e293b;
  font-size: 13px;
}

.mastery-score {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  height: 28px;
  border-radius: 5px;
  font-weight: 800;
  font-size: 12px;
  background: #fffbeb;
  color: #b45309;
}

.mastery-score.danger {
  background: #fef2f2;
  color: #dc2626;
}

.recommendation-item,
.wrong-item {
  padding: 10px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #fff;
}

.recommendation-title {
  color: #1e293b;
  font-size: 13px;
  font-weight: 800;
}

.recommendation-meta {
  margin-top: 4px;
  color: #2563eb;
  font-size: 12px;
}

.video-behavior-panel {
  gap: 16px;
}

.video-conclusion {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid #dbeafe;
  color: #2563eb;
  background: #eff6ff;
}

.video-conclusion.success {
  color: #15803d;
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.video-conclusion.warning {
  color: #b45309;
  border-color: #fde68a;
  background: #fffbeb;
}

.video-conclusion.danger {
  color: #dc2626;
  border-color: #fecaca;
  background: #fef2f2;
}

.video-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.video-stat {
  padding: 12px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #fff;
}

.video-stat span {
  display: block;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 4px;
}

.video-stat strong {
  color: #1e293b;
  font-size: 18px;
}

.video-weak-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.video-weak-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #fff;
}

.behavior-detail-strip {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px dashed #e2e8f0;
}

.behavior-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: 6px 8px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  line-height: 1;
}

.behavior-chip b {
  color: #1e293b;
  font-weight: 700;
}

.behavior-chip em {
  color: #94a3b8;
  font-style: normal;
}

.behavior-chip.seek_backward {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.behavior-chip.seek_forward {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #475569;
}

.behavior-chip.pause {
  background: #fffbeb;
  border-color: #fde68a;
  color: #92400e;
}

.behavior-chip.intervention_shown {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #15803d;
}

.weak-title {
  font-weight: 700;
  color: #1e293b;
  font-size: 14px;
}

.weak-meta,
.weak-numbers {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #64748b;
  font-size: 12px;
  margin-top: 5px;
}

.weak-numbers {
  justify-content: flex-end;
  margin-top: 0;
}

.weak-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  height: 26px;
  padding: 0 8px;
  border-radius: 5px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 12px;
  font-weight: 700;
}

.latest-intervention {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 5px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  border: 1px solid #e2e8f0;
}

.empty-block {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  border-radius: 5px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 14px;
}

.empty-block.small {
  min-height: 96px;
}

.modal-empty {
  min-height: 260px;
}

@media (max-width: 980px) {
  .portrait-hero,
  .student-identity {
    align-items: flex-start;
  }

  .portrait-hero {
    flex-direction: column;
  }

  .portrait-score {
    width: 100%;
    height: 72px;
  }

  .metric-grid,
  .portrait-main {
    grid-template-columns: 1fr;
  }

  .video-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .video-weak-item {
    grid-template-columns: 1fr;
  }

  .weak-numbers {
    justify-content: flex-start;
  }

  .radar-chart {
    height: 300px;
  }
}

@media (max-width: 640px) {
  .student-identity {
    flex-direction: column;
  }

  .metric-card {
    padding: 14px;
  }
}
</style>
