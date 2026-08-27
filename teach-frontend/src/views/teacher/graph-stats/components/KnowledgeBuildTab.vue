<template>
  <div class="tab-content">
    <!-- 第一行：建设统计圆环 + 资源统计卡片 -->
    <div class="row row-2col">
      <div class="panel-card">
        <div class="section-head"><h3>知识点建设情况</h3></div>
        <div class="build-stats">
          <div ref="ringChartRef" class="ring-chart" />
          <div class="build-info">
            <div class="build-item">
              <div class="num">{{ build.total }}</div>
              <div class="label">图谱知识点总数</div>
            </div>
            <div class="build-item">
              <div class="num" style="color:#3B82F6">{{ build.linked }}</div>
              <div class="label">已关联资源的知识点</div>
            </div>
            <div class="build-item">
              <div class="num" style="color:#10B981">{{ build.tagged }}</div>
              <div class="label">已设置标签的知识点</div>
            </div>
            <div class="build-item">
              <div class="num" style="color:#F59E0B">{{ build.unlinked }}</div>
              <div class="label">未关联资源的知识点</div>
            </div>
            <div class="build-item">
              <div class="num" style="color:#8B5CF6">{{ build.crossLinked }}</div>
              <div class="label">跨域关联的知识点</div>
            </div>
          </div>
        </div>
      </div>
      <div class="panel-card">
        <div class="section-head"><h3>关联资源统计</h3></div>
        <div class="resource-grid">
          <div v-for="r in resourceStats" :key="r.type" class="resource-card">
            <div class="resource-icon" :style="{ background: resourceBg(r.type) }">
              <form-outlined v-if="r.type==='homework' || r.type==='practice' || r.type==='quiz'" />
              <code-outlined v-else-if="r.type==='coding'" />
              <video-camera-outlined v-else-if="r.type==='video'" />
              <sound-outlined v-else-if="r.type==='audio'" />
              <file-text-outlined v-else-if="r.type==='doc'" />
              <folder-outlined v-else />
            </div>
            <div class="resource-info">
              <div class="count">{{ r.count }}<span>个</span></div>
              <div class="label">{{ r.label }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二行：知识点属性概况 -->
    <div class="panel-card">
      <div class="section-head">
        <h3>知识点属性概况</h3>
        <a-select v-model:value="selectedTag" size="small" style="width:140px" placeholder="按分类筛选">
          <a-select-option value="all">全部分类</a-select-option>
          <a-select-option v-for="tag in availableTags" :key="tag" :value="tag">{{ tag }}</a-select-option>
        </a-select>
      </div>
      <a-table :dataSource="filteredAttributes" :columns="columns" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'difficulty'">
            <a-tag :color="tagColor(record.difficulty)">{{ record.difficulty }}</a-tag>
          </template>
          <template v-if="column.key === 'importance'">
            <a-tag :color="record.importance === '核心' ? 'blue' : 'green'">{{ record.importance }}</a-tag>
          </template>
          <template v-if="column.key === 'tags'">
            <a-tag v-for="tag in record.tags" :key="tag" size="small">{{ tag }}</a-tag>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { message } from 'ant-design-vue'
import { fetchBuildStats } from '@/api/courseGraphStats'
import {
  VideoCameraOutlined, SoundOutlined, FileTextOutlined,
  FormOutlined, FolderOutlined, CodeOutlined,
} from '@ant-design/icons-vue'

const props = defineProps<{ classId?: number }>()

const ringChartRef = ref<HTMLElement | null>(null)
let ringChart: echarts.ECharts | null = null

const build = ref({ total: 0, linked: 0, tagged: 0, unlinked: 0, crossLinked: 0 })
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const resourceStats = ref<any[]>([])
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const kpAttributes = ref<any[]>([])
const availableTags = ref<string[]>([])
const selectedTag = ref('all')

const filteredAttributes = computed(() => {
  if (selectedTag.value === 'all') return kpAttributes.value
  return kpAttributes.value.filter((a: any) =>
    a.tags && a.tags.some((t: string) => t.includes(selectedTag.value))
  )
})

const columns = [
  { title: '知识点名称', dataIndex: 'name', key: 'name' },
  { title: '难度', dataIndex: 'difficulty', key: 'difficulty', width: 80 },
  { title: '重要程度', dataIndex: 'importance', key: 'importance', width: 100 },
  { title: '标签', dataIndex: 'tags', key: 'tags' },
]

const tagColor = (d: string) => {
  if (d === '高') return 'red'
  if (d === '中') return 'orange'
  return 'green'
}

const resourceBg = (type: string) => {
  const map: Record<string, string> = {
    homework: '#DBEAFE', practice: '#DBEAFE', coding: '#F3E8FF',
    video: '#DBEAFE', audio: '#F3E8FF', doc: '#D1FAE5',
    quiz: '#FEF3C7', other: '#F1F5F9',
  }
  return map[type] || '#F1F5F9'
}

const initRingChart = () => {
  if (!ringChartRef.value) return
  ringChart = echarts.init(ringChartRef.value)
  updateRingChart()
}

const updateRingChart = () => {
  if (!ringChart) return
  ringChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['55%', '75%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: { scaleSize: 5 },
      data: [
        { value: build.value.linked, name: '已关联', itemStyle: { color: '#3B82F6' } },
        { value: build.value.tagged, name: '已标签', itemStyle: { color: '#10B981' } },
        { value: build.value.unlinked, name: '未关联', itemStyle: { color: '#F59E0B' } },
        { value: build.value.crossLinked, name: '跨域', itemStyle: { color: '#8B5CF6' } },
      ],
    }],
  })
}

const loadData = async () => {
  try {
    const data = await fetchBuildStats()
    build.value = data.build || { total: 0, linked: 0, tagged: 0, unlinked: 0, crossLinked: 0 }
    resourceStats.value = data.resources || []
    kpAttributes.value = data.attributes || []
    availableTags.value = data.availableTags || []
    nextTick(() => {
      if (ringChart) updateRingChart()
      else initRingChart()
    })
  } catch {
    message.error('知识点建设数据加载失败')
  }
}

const handleResize = () => {
  ringChart?.resize()
}

onMounted(async () => {
  await loadData()
  window.addEventListener('resize', handleResize)
})

watch(() => props.classId, () => {
  loadData()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  ringChart?.dispose()
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

.build-stats { display: flex; gap: 20px; align-items: center; }
.ring-chart { width: 200px; height: 200px; flex-shrink: 0; }
.build-info { flex: 1; display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.build-item .num { font-size: 22px; font-weight: 800; color: #0f172a; }
.build-item .label { font-size: 12px; color: #94a3b8; margin-top: 2px; }

.resource-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.resource-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 5px;
  background: #f8fafc;
}
.resource-icon {
  width: 40px; height: 40px;
  border-radius: 5px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #475569;
}
.resource-info .count { font-size: 20px; font-weight: 800; color: #0f172a; }
.resource-info .count span { font-size: 12px; font-weight: 400; color: #94a3b8; margin-left: 2px; }
.resource-info .label { font-size: 12px; color: #64748b; margin-top: 2px; }

:deep(.ant-table) { font-size: 13px; }
:deep(.ant-tag) { font-size: 12px; }
</style>
