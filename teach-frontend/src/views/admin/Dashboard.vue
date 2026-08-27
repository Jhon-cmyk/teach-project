<template>
  <div class="admin-dashboard">
    <a-spin :spinning="loading">
      <section class="dashboard-hero">
        <div>
          <span class="hero-kicker">管理工作台</span>
          <h2>平台运营总览</h2>
          <p>首页优先呈现待办、发布健康度、资源供给和关键配置入口。</p>
        </div>
        <a-button type="primary" :loading="loading" @click="loadDashboardOverview">
          刷新数据
        </a-button>
      </section>

      <section class="metric-strip">
        <article
          v-for="item in summaryCards"
          :key="item.key"
          class="metric-card"
          :style="{ '--accent': item.color }"
        >
          <span class="metric-icon">
            <component :is="item.icon" />
          </span>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
            <em>{{ item.hint }}</em>
          </div>
        </article>
      </section>

      <section class="dashboard-main">
        <article class="panel-card todo-panel">
          <div class="panel-head">
            <div>
              <h3>今日优先处理</h3>
              <p>这些会直接影响教师端可用资源和前台展示。</p>
            </div>
            <strong class="danger-number">{{ pendingWorkCount }}</strong>
          </div>

          <div class="todo-list">
            <button
              v-for="item in workItems"
              :key="item.key"
              class="todo-row"
              type="button"
              @click="go(item.path)"
            >
              <span class="todo-dot" :style="{ background: item.color }"></span>
              <span class="todo-copy">
                <strong>{{ item.label }}</strong>
                <small>{{ item.hint }}</small>
              </span>
              <b>{{ item.value }}</b>
            </button>
          </div>
        </article>

        <article class="panel-card health-panel">
          <div class="panel-head">
            <div>
              <h3>发布健康度</h3>
              <p>判断课程、AI 资源、案例和素材是否已对外可用。</p>
            </div>
            <strong class="health-score">{{ overallHealthPercent }}%</strong>
          </div>

          <div class="health-list">
            <div v-for="item in healthItems" :key="item.key" class="health-row">
              <div class="health-copy">
                <strong>{{ item.label }}</strong>
                <span>{{ item.done }} / {{ item.total }}</span>
              </div>
              <div class="health-track">
                <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
              </div>
            </div>
          </div>
        </article>

        <article class="panel-card chart-panel">
          <div class="panel-head">
            <div>
              <h3>内容资产结构</h3>
              <p>总量和可用量对比。</p>
            </div>
          </div>
          <div ref="contentChartRef" class="chart-box"></div>
        </article>

        <article class="panel-card chart-panel">
          <div class="panel-head">
            <div>
              <h3>案例状态分布</h3>
              <p>案例库审核、发布和下架状态。</p>
            </div>
          </div>
          <div ref="caseChartRef" class="chart-box"></div>
        </article>

        <article class="panel-card quick-panel">
          <div class="panel-head">
            <div>
              <h3>管理入口</h3>
              <p>按平台维护场景快速进入。</p>
            </div>
          </div>

          <div class="quick-grid">
            <button v-for="item in quickLinks" :key="item.path" type="button" @click="go(item.path)">
              <component :is="item.icon" />
              <span>{{ item.label }}</span>
              <small>{{ item.desc }}</small>
            </button>
          </div>
        </article>

        <article class="panel-card teacher-panel">
          <div class="panel-head">
            <div>
              <h3>最近教师账号</h3>
              <p>新入驻教师是否已经开始配置内容。</p>
            </div>
          </div>

          <div v-if="recentTeachers.length" class="teacher-list">
            <div v-for="teacher in recentTeachers" :key="teacher.id" class="teacher-row">
              <a-avatar :size="34" :src="teacher.avatar">{{ teacher.name?.slice(0, 1) }}</a-avatar>
              <div class="teacher-info">
                <div class="teacher-title-line">
                  <strong>{{ teacher.name || '未命名教师' }}</strong>
                  <a-tag :color="teacherContentStatus(teacher).color">
                    {{ teacherContentStatus(teacher).text }}
                  </a-tag>
                </div>
                <div class="teacher-sub-line">
                  <span>{{ teacherAccount(teacher) }}</span>
                  <time>{{ formatTeacherCreateTime(teacher.createTime) }}</time>
                </div>
              </div>
            </div>
          </div>
          <a-empty v-else description="暂无教师账号记录" />
        </article>
      </section>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  ApiOutlined,
  AppstoreOutlined,
  BookOutlined,
  FileTextOutlined,
  PictureOutlined,
  RobotOutlined,
  TeamOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'
import { getAdminDashboardMetrics } from '@/api/admin'
import type { AdminDashboardMetrics } from '@/types/admin'
import './Dashboard.css'

const router = useRouter()
const loading = ref(false)
const metrics = ref<AdminDashboardMetrics | null>(null)

const contentChartRef = ref<HTMLElement | null>(null)
const caseChartRef = ref<HTMLElement | null>(null)
let contentChart: echarts.ECharts | null = null
let caseChart: echarts.ECharts | null = null

const numberValue = (value?: number) => value ?? 0
const diffValue = (total?: number, done?: number) => Math.max(numberValue(total) - numberValue(done), 0)
const percentValue = (done?: number, total?: number) => {
  const safeTotal = numberValue(total)
  if (!safeTotal) return 0
  return Math.round((numberValue(done) / safeTotal) * 100)
}

const enabledAssetCount = computed(
  () => numberValue(metrics.value?.enabledBanners) + numberValue(metrics.value?.enabledCategories)
)

const disabledAssetCount = computed(() => diffValue(metrics.value?.totalAssets, enabledAssetCount.value))

const contentTotal = computed(
  () =>
    numberValue(metrics.value?.totalPlatformCourses) +
    numberValue(metrics.value?.totalAiResources) +
    numberValue(metrics.value?.totalPlatformCases) +
    numberValue(metrics.value?.totalAssets)
)

const availableContentTotal = computed(
  () =>
    numberValue(metrics.value?.publishedPlatformCourses) +
    numberValue(metrics.value?.publishedAiResources) +
    numberValue(metrics.value?.approvedPlatformCases) +
    enabledAssetCount.value
)

const pendingWorkCount = computed(
  () =>
    numberValue(metrics.value?.pendingPlatformCases) +
    diffValue(metrics.value?.totalPlatformCourses, metrics.value?.publishedPlatformCourses) +
    diffValue(metrics.value?.totalAiResources, metrics.value?.publishedAiResources) +
    disabledAssetCount.value
)

const overallHealthPercent = computed(() => percentValue(availableContentTotal.value, contentTotal.value))
const recentTeachers = computed(() => metrics.value?.recentTeachers || [])

type RecentTeacherItem = NonNullable<AdminDashboardMetrics['recentTeachers']>[number]

const teacherAccount = (teacher: RecentTeacherItem) => teacher.account || teacher.userAccount || `ID ${teacher.id}`

const teacherContentStatus = (teacher: RecentTeacherItem) => {
  if (typeof teacher.contentCount !== 'number') {
    return { text: '状态待同步', color: 'default' }
  }
  return teacher.contentCount > 0
    ? { text: '已配置内容', color: 'green' }
    : { text: '未配置内容', color: 'default' }
}

const formatTeacherCreateTime = (value?: string) => {
  if (!value) return '刚刚'
  if (/^\d{4}-\d{2}-\d{2}/.test(value)) return value.slice(0, 16)

  const match = value.match(/^[A-Za-z]{3}\s+([A-Za-z]{3})\s+(\d{1,2})\s+(\d{2}:\d{2}):\d{2}\s+\S+\s+(\d{4})$/)
  if (!match) return value

  const monthMap: Record<string, string> = {
    Jan: '01',
    Feb: '02',
    Mar: '03',
    Apr: '04',
    May: '05',
    Jun: '06',
    Jul: '07',
    Aug: '08',
    Sep: '09',
    Oct: '10',
    Nov: '11',
    Dec: '12'
  }
  const [, month, day, time, year] = match
  return `${year}-${monthMap[month] || month}-${day.padStart(2, '0')} ${time}`
}

const summaryCards = computed(() => [
  {
    key: 'users',
    label: '账号规模',
    value: numberValue(metrics.value?.totalUsers),
    unit: '人',
    hint: `学生 ${numberValue(metrics.value?.totalStudents)} / 教师 ${numberValue(metrics.value?.totalTeachers)}`,
    icon: TeamOutlined,
    color: '#2563eb',
  },
  {
    key: 'content',
    label: '内容资产',
    value: contentTotal.value,
    unit: '项',
    hint: `当前可用 ${availableContentTotal.value} 项`,
    icon: AppstoreOutlined,
    color: '#0f766e',
  },
  {
    key: 'ai',
    label: 'AI 资源',
    value: numberValue(metrics.value?.totalAiResources),
    unit: '项',
    hint: `已发布 ${numberValue(metrics.value?.publishedAiResources)} 项`,
    icon: RobotOutlined,
    color: '#7c3aed',
  },
  {
    key: 'pending',
    label: '待处理',
    value: pendingWorkCount.value,
    unit: '项',
    hint: '审核、发布、启用',
    icon: WarningOutlined,
    color: '#dc2626',
  },
])

const workItems = computed(() => [
  {
    key: 'case-review',
    label: '待审核案例',
    value: numberValue(metrics.value?.pendingPlatformCases),
    hint: '进入平台案例处理审核',
    color: '#f59e0b',
    path: '/admin/cases',
  },
  {
    key: 'course-publish',
    label: '未发布课程',
    value: diffValue(metrics.value?.totalPlatformCourses, metrics.value?.publishedPlatformCourses),
    hint: '检查平台课程发布状态',
    color: '#0f766e',
    path: '/admin/courses',
  },
  {
    key: 'ai-publish',
    label: '未发布 AI 资源',
    value: diffValue(metrics.value?.totalAiResources, metrics.value?.publishedAiResources),
    hint: '教案、题库、互动课件发布',
    color: '#7c3aed',
    path: '/admin/ai-resources',
  },
  {
    key: 'asset-enable',
    label: '未启用运营素材',
    value: disabledAssetCount.value,
    hint: '广告图和分类图标',
    color: '#0891b2',
    path: '/admin/assets',
  },
])

const healthItems = computed(() => [
  {
    key: 'courses',
    label: '课程发布率',
    done: numberValue(metrics.value?.publishedPlatformCourses),
    total: numberValue(metrics.value?.totalPlatformCourses),
    percent: percentValue(metrics.value?.publishedPlatformCourses, metrics.value?.totalPlatformCourses),
    color: '#0f766e',
  },
  {
    key: 'ai',
    label: 'AI 资源发布率',
    done: numberValue(metrics.value?.publishedAiResources),
    total: numberValue(metrics.value?.totalAiResources),
    percent: percentValue(metrics.value?.publishedAiResources, metrics.value?.totalAiResources),
    color: '#7c3aed',
  },
  {
    key: 'cases',
    label: '案例发布率',
    done: numberValue(metrics.value?.approvedPlatformCases),
    total: numberValue(metrics.value?.totalPlatformCases),
    percent: percentValue(metrics.value?.approvedPlatformCases, metrics.value?.totalPlatformCases),
    color: '#ea580c',
  },
  {
    key: 'assets',
    label: '素材启用率',
    done: enabledAssetCount.value,
    total: numberValue(metrics.value?.totalAssets),
    percent: percentValue(enabledAssetCount.value, metrics.value?.totalAssets),
    color: '#0891b2',
  },
])

const contentChartItems = computed(() => [
  { name: '课程', total: numberValue(metrics.value?.totalPlatformCourses), available: numberValue(metrics.value?.publishedPlatformCourses) },
  { name: 'AI资源', total: numberValue(metrics.value?.totalAiResources), available: numberValue(metrics.value?.publishedAiResources) },
  { name: '案例', total: numberValue(metrics.value?.totalPlatformCases), available: numberValue(metrics.value?.approvedPlatformCases) },
  { name: '素材', total: numberValue(metrics.value?.totalAssets), available: enabledAssetCount.value },
])

const quickLinks = [
  { label: '接口服务配置', desc: '模型、OSS、ASR 参数', path: '/admin/model-configs', icon: ApiOutlined },
  { label: '平台课程', desc: '课程内容和分集', path: '/admin/courses', icon: BookOutlined },
  { label: '平台案例', desc: '审核、预览、发布', path: '/admin/cases', icon: FileTextOutlined },
  { label: '运营素材', desc: '广告图和分类图标', path: '/admin/assets', icon: PictureOutlined },
]

const axisText = { color: '#64748b', fontSize: 12 }

const renderContentChart = () => {
  if (!contentChartRef.value) return
  if (!contentChart) contentChart = echarts.init(contentChartRef.value)
  const items = contentChartItems.value
  contentChart.setOption({
    color: ['#2563eb', '#0f766e'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, right: 8, itemWidth: 10, itemHeight: 10, textStyle: axisText },
    grid: { left: 8, right: 16, top: 40, bottom: 6, containLabel: true },
    xAxis: {
      type: 'category',
      data: items.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: axisText,
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#eef2f7' } },
      axisLabel: axisText,
    },
    series: [
      { name: '总量', type: 'bar', barWidth: 18, data: items.map((item) => item.total), itemStyle: { borderRadius: [5, 5, 0, 0] } },
      { name: '可用', type: 'bar', barWidth: 18, data: items.map((item) => item.available), itemStyle: { borderRadius: [5, 5, 0, 0] } },
    ],
  })
}

const renderCaseChart = () => {
  if (!caseChartRef.value) return
  if (!caseChart) caseChart = echarts.init(caseChartRef.value)
  caseChart.setOption({
    color: ['#f59e0b', '#16a34a', '#ef4444', '#64748b'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, left: 'center', itemWidth: 10, itemHeight: 10, textStyle: axisText },
    series: [
      {
        name: '案例状态',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '42%'],
        label: { formatter: '{b}\n{c}', color: '#1e293b', fontSize: 12, fontWeight: 700 },
        data: [
          { name: '待审核', value: numberValue(metrics.value?.pendingPlatformCases) },
          { name: '已发布', value: numberValue(metrics.value?.approvedPlatformCases) },
          { name: '已驳回', value: numberValue(metrics.value?.rejectedPlatformCases) },
          { name: '已下架', value: numberValue(metrics.value?.offlinePlatformCases) },
        ],
      },
    ],
  })
}

const renderCharts = async () => {
  await nextTick()
  renderContentChart()
  renderCaseChart()
}

const resizeCharts = () => {
  contentChart?.resize()
  caseChart?.resize()
}

const loadDashboardOverview = async () => {
  loading.value = true
  try {
    metrics.value = await getAdminDashboardMetrics()
    await renderCharts()
  } finally {
    loading.value = false
  }
}

const go = (path: string) => {
  router.push(path)
}

watch(metrics, () => renderCharts())

onMounted(() => {
  loadDashboardOverview()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  contentChart?.dispose()
  caseChart?.dispose()
})
</script>
