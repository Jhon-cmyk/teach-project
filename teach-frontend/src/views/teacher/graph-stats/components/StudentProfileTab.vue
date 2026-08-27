<template>
  <div class="tab-content">
    <!-- 学生选择器 -->
    <div class="student-selector">
      <div
        v-for="s in students" :key="s.id"
        class="student-chip"
        :class="{ active: selectedId === s.id }"
        @click="selectedId = s.id"
      >
        <img :src="s.avatar || '/default-avatar.png'" class="avatar" />
        <div class="info">
          <div class="name">{{ s.name }}</div>
          <div class="pct">完成 {{ s.completionRate }}%</div>
        </div>
      </div>
      <a-empty v-if="!students.length && !loading" description="暂无学生数据" />
    </div>

    <template v-if="currentStudent">
    <!-- 第一行：基本信息 + 雷达图 -->
    <div class="row row-2col">
      <div class="panel-card info-card">
        <div class="section-head"><h3>基本信息</h3></div>
        <div class="info-grid">
          <div class="info-item">
            <div class="label">姓名</div>
            <div class="value">{{ currentStudent.name }}</div>
          </div>
          <div class="info-item">
            <div class="label">完成率</div>
            <div class="value">{{ currentStudent.completionRate }}%</div>
          </div>
          <div class="info-item">
            <div class="label">掌握率</div>
            <div class="value">{{ currentStudent.masteryRate }}%</div>
          </div>
          <div class="info-item">
            <div class="label">持续度</div>
            <div class="value">{{ currentStudent.radar.find(r => r.indicator === '持续度')?.value ?? 0 }}%</div>
          </div>
        </div>
        <div class="progress-section">
          <div class="progress-row">
            <span>完成率</span>
            <a-progress :percent="currentStudent.completionRate" :stroke-color="{ '0%': '#3B82F6', '100%': '#60A5FA' }" />
          </div>
          <div class="progress-row">
            <span>掌握率</span>
            <a-progress :percent="currentStudent.masteryRate" :stroke-color="{ '0%': '#10B981', '100%': '#34D399' }" />
          </div>
        </div>
      </div>
      <div class="panel-card">
        <div class="section-head"><h3>学情雷达</h3></div>
        <div ref="radarChartRef" class="chart-box" />
      </div>
    </div>

    <!-- 第二行：日历热力图 -->
    <div class="panel-card">
      <div class="section-head"><h3>学习记录与趋势</h3></div>
      <div ref="calendarChartRef" class="chart-box calendar-chart" />
    </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { message } from 'ant-design-vue'
import { fetchStudentProfile } from '@/api/courseGraphStats'

const props = defineProps<{ classId?: number }>()

const radarChartRef = ref<HTMLElement | null>(null)
const calendarChartRef = ref<HTMLElement | null>(null)
let radarChart: echarts.ECharts | null = null
let calendarChart: echarts.ECharts | null = null

interface StudentCard {
  id: string
  name: string
  avatar: string
  completionRate: number
  masteryRate: number
  radar: { indicator: string; value: number; max: number }[]
  studyDays: { date: string; minutes: number; completed: number }[]
}

const students = ref<StudentCard[]>([])
const selectedId = ref('')
const currentStudent = ref<StudentCard | null>(null)
const loading = ref(false)

watch(selectedId, (id) => {
  const s = students.value.find(x => x.id === id)
  if (s) currentStudent.value = s
})

const initRadarChart = () => {
  if (!radarChartRef.value) return
  radarChart = echarts.init(radarChartRef.value)
  updateRadarChart()
}

const updateRadarChart = () => {
  if (!radarChart || !currentStudent.value) return
  const r = currentStudent.value.radar
  radarChart.setOption({
    tooltip: {},
    radar: {
      indicator: r.map(i => ({ name: i.indicator, max: i.max })),
      radius: '65%',
      axisName: { color: '#64748b', fontSize: 12 },
      splitArea: { areaStyle: { color: ['#f8fafc', '#fff'] } },
    },
    series: [{
      type: 'radar',
      data: [{
        value: r.map(i => i.value),
        name: currentStudent.value.name,
        areaStyle: { color: 'rgba(59,130,246,0.2)' },
        lineStyle: { color: '#3B82F6', width: 2 },
        itemStyle: { color: '#3B82F6' },
      }],
    }],
  })
}

const initCalendarChart = () => {
  if (!calendarChartRef.value) return
  calendarChart = echarts.init(calendarChartRef.value)
  updateCalendarChart()
}

const updateCalendarChart = () => {
  if (!calendarChart || !currentStudent.value) return
  const days = currentStudent.value.studyDays
  const data = days.map(d => [d.date, d.minutes])
  const year = new Date().getFullYear()
  const yearStr = String(year)

  calendarChart.setOption({
    tooltip: {
      formatter: (p: any) => {
        const v = p.data[1]
        return `${p.data[0]}<br/>学习 ${v} 分钟`
      },
    },
    visualMap: {
      min: 0, max: 180,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: { color: ['#f1f5f9', '#bfdbfe', '#60a5fa', '#2563eb'] },
      show: false,
    },
    calendar: {
      top: 30, left: 30, right: 30, bottom: 30,
      range: yearStr,
      cellSize: ['auto', 18],
      yearLabel: { show: false },
      dayLabel: { firstDay: 1, nameMap: 'cn', fontSize: 10, color: '#94a3b8' },
      monthLabel: { fontSize: 11, color: '#64748b' },
      itemStyle: { borderWidth: 2, borderColor: '#fff' },
      splitLine: { show: false },
    },
    series: [{
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data,
    }],
  })
}

watch(selectedId, () => {
  updateRadarChart()
  updateCalendarChart()
})

const handleResize = () => {
  radarChart?.resize()
  calendarChart?.resize()
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await fetchStudentProfile(props.classId)
    students.value = data.students || []
    if (students.value.length > 0) {
      selectedId.value = students.value[0].id
      currentStudent.value = students.value[0]
    }
  } catch {
    message.error('学生画像数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadData()
  initRadarChart()
  initCalendarChart()
  window.addEventListener('resize', handleResize)
})

watch(() => props.classId, () => {
  loadData()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  radarChart?.dispose()
  calendarChart?.dispose()
})
</script>

<style scoped>
.tab-content { display: flex; flex-direction: column; gap: 16px; }
.row { display: grid; gap: 16px; }
.row-2col { grid-template-columns: 1fr 1fr; }
@media (max-width: 1024px) { .row-2col { grid-template-columns: 1fr; } }

.panel-card {
  background: #fff;
  border-radius: 5px;
  border: 1px solid #e8ecf1;
  box-shadow: 0 4px 20px rgba(15,23,42,0.04);
  padding: 20px;
}
.section-head { margin-bottom: 16px; }
.section-head h3 { margin: 0; font-size: 16px; font-weight: 700; color: #0f172a; }
.chart-box { width: 100%; height: 320px; }
.calendar-chart { height: 220px; }

.student-selector {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.student-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 5px;
  background: #fff;
  border: 1px solid #e8ecf1;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}
.student-chip:hover { border-color: #3b82f6; }
.student-chip.active { border-color: #3b82f6; background: #eff6ff; }
.student-chip .avatar { width: 32px; height: 32px; border-radius: 50%; }
.student-chip .info { display: flex; flex-direction: column; }
.student-chip .name { font-size: 13px; font-weight: 600; color: #1e293b; }
.student-chip .pct { font-size: 11px; color: #64748b; }

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}
.info-item .label { font-size: 12px; color: #94a3b8; margin-bottom: 4px; }
.info-item .value { font-size: 18px; font-weight: 700; color: #0f172a; }

.progress-section { display: flex; flex-direction: column; gap: 12px; }
.progress-row { display: flex; align-items: center; gap: 12px; }
.progress-row span { width: 48px; font-size: 13px; color: #64748b; flex-shrink: 0; }
.progress-row :deep(.ant-progress) { flex: 1; }
</style>
