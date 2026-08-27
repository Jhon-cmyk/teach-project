<template>
  <div class="teacher-dashboard">
    <section class="workbench-header">
      <div class="header-copy">
        <div class="header-title-row">
          <div class="insight-icon">
            <bulb-outlined />
          </div>
          <div>
            <span class="eyebrow">AI 助教今日洞察</span>
            <h1>{{ teacherName }}老师您好，今日教学概况已汇总</h1>
          </div>
        </div>

        <div class="header-main">
          <p>
            今日平台共活跃 <strong>{{ stats.totalStudents }}</strong> 名学生。数据表明，学生在
            <strong>{{ hottestTimeRangeText }}</strong> 期间学习活跃度最高。课后作业与社区待处理内容建议优先跟进。
          </p>
          <div class="header-actions">
            <button
              class="action-button primary"
              :disabled="reminderSending"
              @click="handleSendReminder"
            >
              <bell-outlined />
              <span>{{ reminderSending ? '发送中...' : '发送作业提醒' }}</span>
            </button>
            <button class="action-button secondary" @click="router.push('/teacher/ai')">
              <experiment-outlined />
              <span>进入备课室</span>
            </button>
          </div>
        </div>

        <div class="header-meta">
          <span>{{ lastUpdatedText }}</span>
          <span>{{ currentDateText }}</span>
        </div>
      </div>
    </section>

    <section class="metric-grid" aria-label="关键指标">
      <article
        v-for="item in metricCards"
        :key="item.key"
        :class="['metric-card', 'surface-card', item.tone]"
      >
        <div class="metric-icon">
          <component :is="item.icon" />
        </div>
        <div class="metric-body">
          <span class="metric-label">{{ item.label }}</span>
          <div class="metric-value-row">
            <strong>{{ item.value }}</strong>
            <span>{{ item.unit }}</span>
          </div>
          <p>{{ item.hint }}</p>
        </div>
      </article>
    </section>

    <section class="dashboard-grid" aria-label="教学数据总览">
      <article class="surface-card chart-panel activity-panel">
        <div class="panel-header compact">
          <div>
            <h2><bar-chart-outlined /> 学生活跃时间</h2>
            <p>{{ activePanelDescription }}</p>
          </div>
          <div class="activity-range-switch" role="group" aria-label="学生活跃时间范围">
            <button
              v-for="item in activeRangeOptions"
              :key="item.value"
              :class="{ active: activeRange === item.value }"
              type="button"
              @click="handleActiveRangeChange(item.value)"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <div class="activity-brief">
          <div>
            <span>高峰时段</span>
            <strong>{{ hottestTimeRangeText }}</strong>
          </div>
          <div>
            <span>参与人次</span>
            <strong>{{ activeTotalCount }}</strong>
          </div>
        </div>

        <div ref="barChart" class="chart-canvas"></div>
      </article>

      <article class="surface-card chart-panel homework-panel">
        <div class="panel-header compact">
          <div>
            <h2><pie-chart-outlined /> 作业完成情况</h2>
            <p>用于判断是否需要提醒</p>
          </div>
        </div>

        <div class="homework-content">
          <div ref="pieChart" class="pie-canvas"></div>
          <div class="homework-summary" aria-label="作业完成率">
            <span>完成率</span>
            <strong>{{ homeworkCompletionRate }}</strong>
            <p>{{ completedHomeworkCount }} / {{ homeworkTotal }} 项</p>
          </div>
        </div>

        <div class="panel-footer">
          <span>可前往学情监控查看作业回收与完成详情</span>
          <button class="link-button strong" @click="goToTaskMonitor">
            去查看
          </button>
        </div>
      </article>
    </section>

    <section class="secondary-grid" aria-label="待处理与动态">
      <article class="surface-card community-panel">
        <div class="panel-header compact">
          <div>
            <h2><message-outlined /> 社区待处理摘要</h2>
            <p>优先处理待解决作业与待精选讨论</p>
          </div>
        </div>

        <div v-if="communityLoading" class="loading-state">
          <a-spin size="small" />
          <span>正在加载社区摘要...</span>
        </div>

        <div v-else class="community-content">
          <div class="community-grid">
            <button class="community-metric urgent" @click="goToCommunityDesk('homework')">
              <span>待解决问题</span>
              <strong>{{ communitySummary.openHomeworkCount }}</strong>
            </button>
            <button class="community-metric focus" @click="goToCommunityDesk('featured')">
              <span>待加入精选</span>
              <strong>{{ communitySummary.pendingFeatureCount }}</strong>
            </button>
            <button class="community-metric calm" @click="goToCommunityDesk()">
              <span>今日新增提问</span>
              <strong>{{ communitySummary.todayQuestionCount }}</strong>
            </button>
            <button class="community-metric growth" @click="goToCommunityDesk()">
              <span>本周新增精选</span>
              <strong>{{ communitySummary.weeklyFeaturedCount }}</strong>
            </button>
          </div>

          <div class="panel-footer">
            <span>优先处理待解决作业与待精选讨论</span>
            <button class="link-button strong" @click="goToCommunityDesk()">
              去处理
            </button>
          </div>
        </div>
      </article>

      <article class="surface-card task-panel">
      <div class="panel-header compact">
        <div>
          <h2><history-outlined /> 系统实时动态</h2>
          <p>根据当前概览生成的关键提醒与建议动作</p>
        </div>
      </div>

      <div class="task-list">
        <article
          v-for="item in actionItems"
          :key="item.key"
          :class="['task-row', item.tone]"
        >
          <div class="task-icon">
            <component :is="item.icon" />
          </div>
          <div class="task-copy">
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </div>
          <button class="task-action" @click="item.action">
            {{ item.actionText }}
          </button>
        </article>
      </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import type { Component } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { getLoginUserRaw } from '@/utils/authStorage'
import { message } from 'ant-design-vue'
import {
  BellOutlined,
  BulbOutlined,
  ExperimentOutlined,
  ReadOutlined,
  TeamOutlined,
  ClockCircleOutlined,
  BarChartOutlined,
  PieChartOutlined,
  HistoryOutlined,
  FileDoneOutlined,
  MessageOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import { getTeacherCommunityPendingSummary } from '@/api/community'

const router = useRouter()

type CommunityFocus = 'homework' | 'featured' | 'overview'
type PieChartItem = { name: string; value: number }
type ActiveTimeItem = { timeRange: string; count: number }
type ActiveRangeKey = 'today' | '7d' | '30d' | 'semester'

interface DashboardStats {
  totalStudents: number
  totalCourses: number
  aiUsage: number
  avgStudyTime: number
  classCount: number
  pieChartData: PieChartItem[]
  activeTimeDistribution: ActiveTimeItem[]
  activeRange?: ActiveRangeKey
  activeRangeLabel?: string
}

interface MetricCard {
  key: string
  label: string
  value: string | number
  unit: string
  hint: string
  icon: Component
  tone: string
}

interface ActionItem {
  key: string
  title: string
  description: string
  actionText: string
  action: () => void
  icon: Component
  tone: string
}

const BAR_TIME_RANGES = ['8-10点', '10-12点', '14-16点', '19-21点', '21-23点']
const activeRangeOptions: Array<{ label: string; value: ActiveRangeKey }> = [
  { label: '今日', value: 'today' },
  { label: '近7天', value: '7d' },
  { label: '近30天', value: '30d' },
  { label: '本学期', value: 'semester' }
]

const activeRange = ref<ActiveRangeKey>('semester')
const activeRangeLabel = computed(() => {
  return activeRangeOptions.find((item) => item.value === activeRange.value)?.label ?? '本学期'
})
const activePanelDescription = computed(() => `${activeRangeLabel.value}关键时段参与趋势`)

const getStoredUser = () => {
  try {
    const raw = getLoginUserRaw()
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

const teacherName = computed(() => {
  const user = getStoredUser()
  return user?.userName || user?.name || user?.userAccount || '老师'
})

const currentDateText = computed(() => {
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  }).format(new Date())
})

const lastUpdatedAt = ref('')
const lastUpdatedText = computed(() => lastUpdatedAt.value ? `${lastUpdatedAt.value} 更新` : '等待数据更新')

const goToCommunityDesk = (focus?: CommunityFocus) => {
  router.push({
    path: '/teacher/community',
    query: focus ? { focus } : undefined
  })
}

const goToTaskMonitor = () => {
  router.push('/teacher/monitor')
}

const normalizeActiveTimeDistribution = (list?: ActiveTimeItem[]) => {
  const source = Array.isArray(list) ? list : []
  return BAR_TIME_RANGES.map((timeRange) => {
    const target = source.find((item) => item.timeRange === timeRange)
    return {
      timeRange,
      count: Number(target?.count ?? 0)
    }
  })
}

const stats = ref<DashboardStats>({
  totalStudents: 0,
  totalCourses: 0,
  aiUsage: 0,
  avgStudyTime: 0,
  classCount: 0,
  pieChartData: [],
  activeTimeDistribution: normalizeActiveTimeDistribution(),
  activeRange: 'semester',
  activeRangeLabel: '本学期'
})

const formatNumber = (value: number) => Number(value || 0).toLocaleString('zh-CN')

const metricCards = computed<MetricCard[]>(() => [
  {
    key: 'courses',
    label: '在教课程',
    value: formatNumber(stats.value.totalCourses),
    unit: '门',
    hint: '当前教师负责课程',
    icon: ReadOutlined,
    tone: 'tone-blue'
  },
  {
    key: 'students',
    label: '班级学生',
    value: formatNumber(stats.value.totalStudents),
    unit: '人',
    hint: '覆盖全部教学班',
    icon: TeamOutlined,
    tone: 'tone-green'
  },
  {
    key: 'study-time',
    label: '生均学习时长',
    value: formatNumber(stats.value.avgStudyTime),
    unit: '分钟',
    hint: '今日平均投入',
    icon: ClockCircleOutlined,
    tone: 'tone-slate'
  }
])

const hottestTimeRangeText = computed(() => {
  const list = normalizeActiveTimeDistribution(stats.value.activeTimeDistribution)
  const top = list.reduce((prev, curr) => (curr.count > prev.count ? curr : prev), list[0])
  return top && top.count > 0 ? top.timeRange : '暂无明显高峰'
})

const activeTotalCount = computed(() => {
  return normalizeActiveTimeDistribution(stats.value.activeTimeDistribution)
    .reduce((sum, item) => sum + Number(item.count || 0), 0)
})

const homeworkTotal = computed(() => {
  return stats.value.pieChartData.reduce((sum, item) => sum + Number(item.value || 0), 0)
})

const completedHomeworkCount = computed(() => {
  const completed = stats.value.pieChartData.find((item) => {
    const name = String(item.name || '')
    return name.includes('已完成') || (name.includes('完成') && !name.includes('未'))
  })

  return Number(completed?.value || 0)
})

const homeworkCompletionRate = computed(() => {
  if (!homeworkTotal.value) return '--'
  return `${Math.round((completedHomeworkCount.value / homeworkTotal.value) * 100)}%`
})

const communityLoading = ref(false)
const communitySummary = ref({
  openHomeworkCount: 0,
  pendingFeatureCount: 0,
  todayQuestionCount: 0,
  weeklyFeaturedCount: 0
})

const actionItems = computed<ActionItem[]>(() => [
  {
    key: 'homework',
    title: communitySummary.value.openHomeworkCount > 0 ? '处理待解决作业问题' : '作业互助暂未堆积',
    description:
      communitySummary.value.openHomeworkCount > 0
        ? `社区中还有 ${communitySummary.value.openHomeworkCount} 个作业问题等待教师处理。`
        : '当前没有待解决作业问题，可继续观察学生讨论情况。',
    actionText: '查看社区',
    action: () => goToCommunityDesk('homework'),
    icon: MessageOutlined,
    tone: communitySummary.value.openHomeworkCount > 0 ? 'warning' : 'calm'
  },
  {
    key: 'monitor',
    title: '复核作业完成与回收',
    description: '进入学情监控查看学生完成率、平均分和未完成名单。',
    actionText: '学情监控',
    action: goToTaskMonitor,
    icon: FileDoneOutlined,
    tone: 'primary'
  },
  {
    key: 'reminder',
    title: '向未完成学生发送提醒',
    description: '当作业完成率偏低时，可直接向学生端推送作业提醒。',
    actionText: reminderSending.value ? '发送中' : '发送提醒',
    action: handleSendReminder,
    icon: WarningOutlined,
    tone: 'danger'
  }
])

const fetchDashboardData = async () => {
  try {
    const data = await request.get<any, any>('/teacher/dashboard/stats', {
      params: { activeRange: activeRange.value },
      skipErrorToast: true
    })

    stats.value = {
      totalStudents: Number(data?.totalStudents ?? 0),
      totalCourses: Number(data?.totalCourses ?? 0),
      aiUsage: Number(data?.aiUsage ?? 0),
      avgStudyTime: Number(data?.avgStudyTime ?? 0),
      classCount: Number(data?.classCount ?? 0),
      pieChartData: Array.isArray(data?.pieChartData) ? data.pieChartData : [],
      activeTimeDistribution: normalizeActiveTimeDistribution(data?.activeTimeDistribution),
      activeRange: data?.activeRange ?? activeRange.value,
      activeRangeLabel: data?.activeRangeLabel ?? activeRangeLabel.value
    }

    lastUpdatedAt.value = new Intl.DateTimeFormat('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date())

    updateBarChart()
    updatePieChart()
  } catch (error) {
    console.error('获取看板数据失败', error)
    updateBarChart()
    updatePieChart()
  }
}

const handleActiveRangeChange = async (range: ActiveRangeKey) => {
  if (activeRange.value === range) return
  activeRange.value = range
  await fetchDashboardData()
}

const fetchCommunitySummary = async () => {
  communityLoading.value = true
  try {
    communitySummary.value = await getTeacherCommunityPendingSummary()
  } catch (error) {
    console.error('获取社区摘要失败', error)
  } finally {
    communityLoading.value = false
  }
}

const getLocalUserId = (): number | null => {
  const user = getStoredUser()
  return user?.id ?? null
}

const reminderSending = ref(false)

async function handleSendReminder() {
  if (reminderSending.value) return

  const teacherId = getLocalUserId()
  if (!teacherId) {
    message.warning('获取用户信息失败，请重新登录后再试')
    return
  }

  reminderSending.value = true
  try {
    await request.post('/notification/send-homework-reminder', {
      message: '老师提醒您：请及时完成作业哦，加油！'
    })
    message.success('提醒已发送，学生端将收到通知。')
  } catch {
    // request 拦截器已弹 message.error，此处静默
  } finally {
    reminderSending.value = false
  }
}

const barChart = ref<HTMLElement | null>(null)
const pieChart = ref<HTMLElement | null>(null)
let myBarChart: echarts.ECharts | null = null
let myPieChart: echarts.ECharts | null = null

const initCharts = () => {
  if (barChart.value) {
    myBarChart = echarts.init(barChart.value)
    updateBarChart()
  }

  if (pieChart.value) {
    myPieChart = echarts.init(pieChart.value)
    updatePieChart()
  }
}

const updateBarChart = () => {
  if (!myBarChart) return

  const chartData = normalizeActiveTimeDistribution(stats.value.activeTimeDistribution)
  const maxActiveCount = Math.max(...chartData.map((item) => item.count), 0)
  const visualMax = Math.max(3, Math.ceil(maxActiveCount * 1.18))

  myBarChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#ffffff',
      borderColor: '#d9e2ec',
      borderWidth: 1,
      borderRadius: 6,
      textStyle: { color: '#1f2937' },
      axisPointer: {
        type: 'shadow',
        shadowStyle: { color: 'rgba(37, 99, 235, 0.06)' }
      }
    },
    grid: { top: 8, bottom: 18, left: 24, right: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: chartData.map((item) => item.timeRange),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#dce6f1' } },
      axisLabel: { color: '#667085', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      max: visualMax,
      minInterval: 1,
      splitNumber: 2,
      splitLine: { lineStyle: { type: 'dashed', color: '#edf3f8' } },
      axisLabel: { color: '#98a2b3', fontSize: 11 }
    },
    series: [
      {
        name: '活跃人数',
        type: 'bar',
        data: chartData.map((item) => item.count),
        itemStyle: {
          color: '#4f7fbf',
          borderRadius: [5, 5, 0, 0]
        },
        emphasis: {
          itemStyle: { color: '#2f68a6' }
        },
        barWidth: 24
      }
    ]
  })
}

const updatePieChart = () => {
  if (!myPieChart) return

  const chartData = Array.isArray(stats.value.pieChartData) ? stats.value.pieChartData : []
  const total = chartData.reduce((sum, item) => sum + Number(item.value || 0), 0)

  if (!chartData.length || total <= 0) {
    myPieChart.setOption({
      title: {
        text: '暂无作业数据',
        subtext: '发布作业后将在此显示完成情况',
        left: 'center',
        top: '34%',
        textStyle: { color: '#5f6f82', fontSize: 14, fontWeight: 700 },
        subtextStyle: { color: '#98a2b3', fontSize: 12 }
      },
      legend: { show: false },
      series: []
    }, true)
    return
  }

  myPieChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#d9e2ec',
      borderWidth: 1,
      borderRadius: 6,
      textStyle: { color: '#1f2937' }
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 14,
      textStyle: { color: '#667085', fontSize: 12 }
    },
    series: [
      {
        name: '作业状态',
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '41%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        color: ['#4f7fbf', '#58a77a', '#d89b3d', '#cf5f5f'],
        data: chartData
      }
    ]
  }, true)
}

const handleResize = () => {
  myBarChart?.resize()
  myPieChart?.resize()
}

onMounted(async () => {
  initCharts()
  await Promise.all([fetchDashboardData(), fetchCommunitySummary()])
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  myBarChart?.dispose()
  myPieChart?.dispose()
})
</script>

<style>
.teacher-dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 4px 2px 0;
  color: #1f2937;
  animation: page-enter 0.28s ease;
  overscroll-behavior: contain;
}

.teacher-dashboard::-webkit-scrollbar {
  width: 8px;
}

.teacher-dashboard::-webkit-scrollbar-thumb {
  background: #c7d5e5;
  border-radius: 999px;
}

.teacher-dashboard::-webkit-scrollbar-track {
  background: transparent;
}

@keyframes page-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.surface-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.workbench-header {
  position: relative;
  overflow: hidden;
  min-height: 184px;
  padding: 28px 36px 30px;
  border: 1px solid rgba(177, 194, 255, 0.5);
  border-radius: 8px;
  background:
    radial-gradient(circle at 92% 22%, rgba(25, 111, 191, 0.34), transparent 30%),
    linear-gradient(116deg, #241b5d 0%, #2a286f 45%, #234779 100%);
  box-shadow: 0 16px 34px rgba(37, 52, 122, 0.16);
}

.workbench-header::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.08), transparent 38%),
    radial-gradient(circle at 8% 100%, rgba(34, 211, 238, 0.16), transparent 28%);
}

.header-copy {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 126px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.insight-icon {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.16);
  color: #f5df72;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  color: #ffffff;
  font-size: 22px;
  line-height: 1.2;
  font-weight: 800;
}

.header-copy h1 {
  margin: 5px 0 0;
  color: rgba(238, 245, 255, 0.72);
  font-size: 13px;
  line-height: 1.4;
  font-weight: 600;
}

.header-main {
  width: 100%;
}

.header-copy p {
  margin: 0;
  max-width: 1040px;
  color: rgba(239, 246, 255, 0.82);
  font-size: 14px;
  line-height: 1.65;
  font-weight: 700;
}

.header-copy p strong {
  color: #65f2ff;
  font-weight: 900;
}

.header-meta {
  position: absolute;
  z-index: 2;
  top: 28px;
  right: 36px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.header-meta span {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 11px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(239, 246, 255, 0.58);
  font-size: 12px;
  font-weight: 700;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-start;
  margin-top: 20px;
}

.action-button,
.link-button,
.task-action {
  border: none;
  outline: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 7px;
  font-weight: 700;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.action-button {
  height: 40px;
  padding: 0 17px;
  font-size: 14px;
  white-space: nowrap;
}

.action-button.primary {
  background: #00d9ea;
  color: #06243c;
  box-shadow: 0 12px 24px rgba(0, 217, 234, 0.26);
}

.action-button.primary:hover:not(:disabled) {
  background: #23e9f5;
  transform: translateY(-1px);
}

.action-button.secondary {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.action-button.secondary:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.34);
  color: #ffffff;
}

.action-button:disabled {
  opacity: 0.62;
  cursor: not-allowed;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  min-height: 118px;
  padding: 22px 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.metric-card:hover {
  transform: translateY(-1px);
  border-color: #cbd9e8;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 25px;
}

.metric-body {
  min-width: 0;
}

.metric-label {
  color: #667085;
  font-size: 14px;
  font-weight: 700;
}

.metric-value-row {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin-top: 7px;
}

.metric-value-row strong {
  color: #172033;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
}

.metric-value-row span {
  color: #7b8796;
  font-size: 15px;
  font-weight: 700;
}

.metric-body p {
  margin: 8px 0 0;
  color: #98a2b3;
  font-size: 12px;
}

.tone-blue .metric-icon {
  background: #edf6ff;
  color: #2f68a6;
}

.tone-green .metric-icon {
  background: #eef8f1;
  color: #3f8d62;
}

.tone-amber .metric-icon {
  background: #fff6e8;
  color: #ad7628;
}

.tone-slate .metric-icon {
  background: #f2f5f8;
  color: #526174;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(380px, 0.98fr);
  gap: 16px;
  align-items: stretch;
}

.secondary-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.48fr) minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}

.chart-panel,
.community-panel,
.task-panel {
  padding: 22px 24px;
}

.activity-panel {
  min-height: 390px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.homework-panel {
  min-height: 390px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.community-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-header.compact {
  margin-bottom: 16px;
}

.panel-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #172033;
  font-size: 18px;
  font-weight: 800;
}

.teacher-dashboard .panel-header h2 svg {
  color: #526174;
}

.panel-header p {
  margin: 7px 0 0;
  color: #7b8796;
  font-size: 13px;
  line-height: 1.5;
}

.panel-tag {
  height: 28px;
  padding: 0 12px;
  border-radius: 7px;
  background: #f3f7fb;
  color: #5f6f82;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
}

.activity-range-switch {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  padding: 4px;
  border-radius: 8px;
  background: #f3f7fb;
  border: 1px solid #e2ebf5;
}

.activity-range-switch button {
  height: 28px;
  min-width: 52px;
  padding: 0 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #5f6f82;
  font-size: 12px;
  font-weight: 700;
  line-height: 28px;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.activity-range-switch button:hover {
  background: #ffffff;
  color: #334155;
}

.activity-range-switch button:focus-visible {
  outline: 2px solid rgba(79, 127, 191, 0.32);
  outline-offset: 2px;
}

.activity-range-switch button.active {
  background: #ffffff;
  color: #2f68a6;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.chart-canvas {
  width: 100%;
  flex: 1 1 230px;
  min-height: 230px;
  margin-top: 10px;
}

.homework-content {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  gap: 12px;
  flex: 1;
}

.pie-canvas {
  width: 100%;
  height: 248px;
}

.homework-summary {
  min-width: 0;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f6f9fd;
  border: 1px solid #e2ebf5;
  display: none;
}

.homework-summary span,
.activity-brief span {
  display: block;
  color: #6b7787;
  font-size: 12px;
  font-weight: 700;
}

.homework-summary strong,
.activity-brief strong {
  display: block;
  margin-top: 6px;
  color: #172033;
  font-size: 22px;
  line-height: 1;
  font-weight: 800;
}

.homework-summary p {
  margin: 8px 0 0;
  color: #98a2b3;
  font-size: 12px;
}

.activity-brief {
  display: none;
}

.activity-brief > div {
  min-width: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f6f9fd;
  border: 1px solid #e2ebf5;
}

.panel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid #edf2f7;
  color: #7b8796;
  font-size: 13px;
}

.link-button {
  height: 34px;
  padding: 0 14px;
  background: #eef5ff;
  color: #235ca8;
  font-size: 14px;
}

.link-button.strong {
  min-width: 76px;
  height: 38px;
  border-radius: 7px;
  background: #2f73f6;
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(47, 115, 246, 0.22);
}

.link-button:hover {
  background: #dfeeff;
}

.link-button.strong:hover {
  background: #1f63e6;
}

.loading-state {
  flex: 1;
  min-height: 138px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #6b7787;
  font-size: 13px;
}

.community-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.community-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.community-metric {
  min-height: 92px;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid transparent;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  cursor: pointer;
  text-align: left;
  transition: transform 0.18s ease, border-color 0.18s ease;
}

.community-metric:hover {
  transform: translateY(-1px);
  border-color: rgba(51, 65, 85, 0.12);
}

.community-metric span {
  color: #5f6f82;
  font-size: 13px;
  font-weight: 700;
}

.community-metric strong {
  color: #172033;
  font-size: 25px;
  line-height: 1;
  font-weight: 800;
}

.community-metric.urgent { background: #fff3f1; }
.community-metric.focus { background: #f3f1ff; }
.community-metric.calm { background: #f1f7ff; }
.community-metric.growth { background: #eef8f1; }

.task-panel {
  padding-bottom: 22px;
}

.task-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-top: 0;
}

.task-row {
  min-height: 78px;
  padding: 16px 18px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: #f7f9fc;
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  grid-template-rows: auto;
  align-items: center;
  gap: 16px;
}

.task-icon {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.task-copy {
  min-width: 0;
}

.task-copy h3 {
  margin: 0;
  color: #172033;
  font-size: 15px;
  font-weight: 800;
}

.task-copy p {
  margin: 6px 0 0;
  color: #6b7787;
  font-size: 12px;
  line-height: 1.6;
}

.task-action {
  grid-column: auto;
  justify-self: end;
  height: 34px;
  padding: 0 13px;
  background: #ffffff;
  border: 1px solid #dce5ef;
  color: #334155;
  font-size: 12px;
}

.task-action:hover {
  background: #f8fbff;
  border-color: #c8d6e6;
}

.task-row.primary .task-icon {
  background: #edf6ff;
  color: #2f68a6;
}

.task-row.warning .task-icon,
.task-row.danger .task-icon {
  background: #fff3f1;
  color: #b44b4b;
}

.task-row.calm .task-icon {
  background: #eef8f1;
  color: #3f8d62;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .secondary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .activity-panel {
    min-height: 360px;
  }
}

@media (max-width: 900px) {
  .workbench-header {
    padding: 18px;
  }

  .header-copy {
    gap: 14px;
  }

  .header-meta {
    position: static;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .metric-grid,
  .secondary-grid {
    grid-template-columns: 1fr;
  }

  .activity-panel {
    min-height: 330px;
  }

  .activity-panel .panel-header {
    flex-direction: column;
    align-items: stretch;
  }

  .activity-range-switch {
    width: 100%;
  }

  .activity-range-switch button {
    flex: 1;
    min-width: 0;
  }

  .chart-canvas {
    min-height: 200px;
  }

  .task-row {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .task-action {
    grid-column: 2;
    justify-self: start;
  }
}

@media (max-width: 640px) {
  .workbench-header,
  .chart-panel,
  .community-panel,
  .task-panel {
    padding: 16px;
  }

  .workbench-header {
    min-height: 190px;
  }

  .header-copy h1 {
    font-size: 13px;
  }

  .eyebrow {
    font-size: 22px;
  }

  .header-title-row {
    align-items: flex-start;
  }

  .insight-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }

  .action-button {
    flex: 1;
    min-width: 160px;
  }

  .community-grid {
    grid-template-columns: 1fr;
  }
}
</style>
