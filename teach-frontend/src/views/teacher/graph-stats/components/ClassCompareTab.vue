<template>
  <div class="tab-content">
    <!-- 第一行：横向柱状图 + 折线图 -->
    <div class="row row-2col">
      <div class="panel-card">
        <div class="section-head">
          <h3>班级完成率/掌握率对比</h3>
          <div class="legend-inline">
            <span class="dot" style="background:#3B82F6" /> 完成率
            <span class="dot" style="background:#10B981" /> 掌握率
          </div>
        </div>
        <div ref="barChartRef" class="chart-box" />
      </div>
      <div class="panel-card">
        <div class="section-head">
          <h3>完成率/掌握率区间比例对比</h3>
          <a-select v-model:value="selectedClassBId" size="small" style="width:120px">
            <a-select-option v-for="c in classList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
        </div>
        <div ref="lineChartRef" class="chart-box" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { message } from 'ant-design-vue'
import { fetchCompareData, fetchClassList } from '@/api/courseGraphStats'

const barChartRef = ref<HTMLElement | null>(null)
const lineChartRef = ref<HTMLElement | null>(null)
let barChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

const classData = ref<{ name: string; completion: number; mastery: number }[]>([])
const rangeData = ref<{ range: string; classA: number; classB: number }[]>([])
const classList = ref<{ id: number; name: string }[]>([])
const selectedClassBId = ref<number | undefined>(undefined)

const getClassAId = (): number | undefined => {
  return classList.value.length > 0 ? classList.value[0].id : undefined
}

const getClassBName = (): string => {
  const c = classList.value.find(cls => cls.id === selectedClassBId.value)
  return c ? c.name : '班级B'
}

const initBarChart = () => {
  if (!barChartRef.value) return
  if (barChart) { barChart.dispose(); barChart = null }
  barChart = echarts.init(barChartRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '8%', bottom: '3%', top: '5%', containLabel: true },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: classData.value.map(c => c.name), axisLabel: { fontSize: 13 } },
    series: [
      { name: '完成率', type: 'bar', data: classData.value.map(c => c.completion), itemStyle: { color: '#3B82F6', borderRadius: [0, 4, 4, 0] }, barWidth: 16 },
      { name: '掌握率', type: 'bar', data: classData.value.map(c => c.mastery), itemStyle: { color: '#10B981', borderRadius: [0, 4, 4, 0] }, barWidth: 16 },
    ],
  })
}

const initLineChart = () => {
  if (!lineChartRef.value) return
  lineChart = echarts.init(lineChartRef.value)
  updateLineChart()
}

const updateLineChart = () => {
  if (!lineChart) return
  const classBName = getClassBName()
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: [classData.value[0]?.name || '班级A', classBName], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: rangeData.value.map(r => r.range), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    series: [
      { name: classData.value[0]?.name || '班级A', type: 'line', smooth: true, data: rangeData.value.map(r => r.classA), lineStyle: { color: '#3B82F6', width: 3 }, itemStyle: { color: '#3B82F6' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(59,130,246,0.2)' }, { offset: 1, color: 'rgba(59,130,246,0.02)' }] } } },
      { name: classBName, type: 'line', smooth: true, data: rangeData.value.map(r => r.classB), lineStyle: { color: '#F59E0B', width: 3 }, itemStyle: { color: '#F59E0B' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(245,158,11,0.2)' }, { offset: 1, color: 'rgba(245,158,11,0.02)' }] } } },
    ],
  })
}

watch(selectedClassBId, () => {
  loadData()
})

const loadClassList = async () => {
  try {
    const data = await fetchClassList()
    classList.value = (data || []).map((c: any) => ({ id: c.id, name: c.name }))
    if (classList.value.length >= 2) {
      selectedClassBId.value = classList.value[1].id
    }
  } catch {
    message.error('班级列表加载失败')
  }
}

const loadData = async () => {
  const classAId = getClassAId()
  if (!classAId || !selectedClassBId.value) return
  try {
    const data = await fetchCompareData(classAId, selectedClassBId.value)
    classData.value = data.classes || []
    rangeData.value = data.ranges || []
    nextTick(() => {
      initBarChart()
      if (lineChart) updateLineChart()
      else initLineChart()
    })
  } catch {
    message.error('数据加载失败')
  }
}

onMounted(async () => {
  await loadClassList()
  await loadData()
  window.addEventListener('resize', handleResize)
})

const handleResize = () => {
  barChart?.resize()
  lineChart?.resize()
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  lineChart?.dispose()
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
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-head h3 { margin: 0; font-size: 16px; font-weight: 700; color: #0f172a; }
.legend-inline { display: flex; align-items: center; gap: 12px; font-size: 12px; color: #64748b; }
.legend-inline .dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; }
.chart-box { width: 100%; height: 360px; }
</style>
