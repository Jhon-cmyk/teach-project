<template>
  <div class="tab-content">
    <!-- 第一行：柱状图 + 热度排名 -->
    <div class="row row-2col">
      <div class="panel-card">
        <div class="section-head">
          <h3>知识点完成与掌握情况</h3>
          <div class="legend-inline">
            <span class="dot" style="background:#3B82F6" /> 完成率
            <span class="dot" style="background:#10B981" /> 掌握率
          </div>
        </div>
        <div ref="barChartRef" class="chart-box" />
      </div>
      <div class="panel-card">
        <div class="section-head"><h3>知识点热度</h3></div>
        <div v-if="hotspots.length > 0" class="hotspot-list">
          <div v-for="(item, idx) in hotspots" :key="item.name" class="hotspot-item">
            <span class="rank">{{ idx + 1 }}</span>
            <span class="name">{{ item.name }}</span>
            <div class="bar-wrap">
              <div class="bar" :style="{ width: item.heatScore + '%', background: heatColor(idx) }" />
            </div>
            <span class="count">{{ item.questionCount }} 问</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二行：圆环图 + 学生列表 -->
    <div class="row row-2col">
      <div class="panel-card">
        <div class="section-head"><h3>学情分段人数</h3></div>
        <div ref="pieChartRef" class="chart-box" />
      </div>
      <div class="panel-card">
        <div class="section-head"><h3>学生完成与掌握情况</h3></div>
        <div class="student-list">
          <div v-for="item in students" :key="item.name" class="student-row">
            <div class="info">
              <span class="rank" :class="'rank-' + item.rank">{{ item.rank }}</span>
              <span class="name">{{ item.name }}</span>
            </div>
            <div class="bars">
              <div class="bar-line">
                <span class="label">完成</span>
                <a-progress :percent="item.completion" size="small" :stroke-color="'#3B82F6'" :show-info="false" />
                <span class="pct">{{ item.completion }}%</span>
              </div>
              <div class="bar-line">
                <span class="label">掌握</span>
                <a-progress :percent="item.mastery" size="small" :stroke-color="'#10B981'" :show-info="false" />
                <span class="pct">{{ item.mastery }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { message } from 'ant-design-vue'
import { fetchOverviewData } from '@/api/courseGraphStats'

const props = defineProps<{ classId?: number }>()

const barChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const loading = ref(false)
const kpData = ref<{ name: string; completion: number; mastery: number }[]>([])
const hotspots = ref<{ name: string; heatScore: number; questionCount: number }[]>([])
const segments = ref<{ label: string; count: number; color: string }[]>([])
const students = ref<{ name: string; completion: number; mastery: number; rank: number }[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const data = await fetchOverviewData(props.classId)
    kpData.value = data.kpProgress || []
    hotspots.value = data.hotspots || []
    segments.value = data.segments || []
    students.value = data.students || []
    nextTick(() => {
      initBarChart()
      initPieChart()
    })
  } catch {
    message.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

const heatColor = (idx: number) => {
  const colors = ['#EF4444', '#F97316', '#F59E0B', '#10B981', '#3B82F6', '#8B5CF6', '#64748B', '#94A3B8']
  return colors[idx] || '#94A3B8'
}

const initBarChart = () => {
  if (!barChartRef.value) return
  if (barChart) { barChart.dispose(); barChart = null }
  barChart = echarts.init(barChartRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: kpData.value.map(k => k.name), axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      { name: '完成率', type: 'bar', data: kpData.value.map(k => k.completion), itemStyle: { color: '#3B82F6', borderRadius: [4, 4, 0, 0] } },
      { name: '掌握率', type: 'bar', data: kpData.value.map(k => k.mastery), itemStyle: { color: '#10B981', borderRadius: [4, 4, 0, 0] } },
    ],
  })
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  if (pieChart) { pieChart.dispose(); pieChart = null }
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    legend: { bottom: 0, left: 'center', itemWidth: 10, itemHeight: 10 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: segments.value.map(s => ({ value: s.count, name: s.label, itemStyle: { color: s.color } })),
    }],
  })
}

const handleResize = () => {
  barChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

watch(() => props.classId, () => {
  loadData()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.tab-content { display: flex; flex-direction: column; gap: 16px; }
.row { display: grid; gap: 16px; }
.row-2col { grid-template-columns: 1.2fr 1fr; }
@media (max-width: 1024px) { .row-2col { grid-template-columns: 1fr; } }

.panel-card {
  background: #fff;
  border-radius: 5px;
  border: 1px solid #e8ecf1;
  box-shadow: 0 4px 20px rgba(15,23,42,0.04);
  padding: 20px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-head h3 { margin: 0; font-size: 16px; font-weight: 700; color: #0f172a; }
.legend-inline { display: flex; align-items: center; gap: 12px; font-size: 12px; color: #64748b; }
.legend-inline .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; }
.chart-box { width: 100%; height: 320px; }

.hotspot-list { display: flex; flex-direction: column; gap: 10px; }
.hotspot-item { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.hotspot-item .rank { width: 22px; height: 22px; border-radius: 50%; background: #f1f5f9; color: #64748b; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.hotspot-item .name { width: 100px; color: #1e293b; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hotspot-item .bar-wrap { flex: 1; height: 8px; background: #f1f5f9; border-radius: 4px; overflow: hidden; }
.hotspot-item .bar { height: 100%; border-radius: 4px; transition: width 0.4s ease; }
.hotspot-item .count { width: 50px; text-align: right; color: #94a3b8; font-size: 12px; flex-shrink: 0; }

.student-list { display: flex; flex-direction: column; gap: 12px; }
.student-row { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f1f5f9; }
.student-row:last-child { border-bottom: none; }
.student-row .info { display: flex; align-items: center; gap: 8px; width: 90px; flex-shrink: 0; }
.student-row .rank { width: 20px; height: 20px; border-radius: 50%; background: #f1f5f9; color: #64748b; font-size: 11px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.student-row .rank-1 { background: #FEF3C7; color: #D97706; }
.student-row .rank-2 { background: #E0F2FE; color: #0284C7; }
.student-row .rank-3 { background: #F3E8FF; color: #9333EA; }
.student-row .name { font-size: 13px; color: #1e293b; font-weight: 500; }
.student-row .bars { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.bar-line { display: flex; align-items: center; gap: 6px; }
.bar-line .label { width: 32px; font-size: 11px; color: #94a3b8; flex-shrink: 0; }
.bar-line :deep(.ant-progress) { flex: 1; margin: 0; }
.bar-line .pct { width: 36px; font-size: 11px; color: #64748b; text-align: right; flex-shrink: 0; }
</style>
