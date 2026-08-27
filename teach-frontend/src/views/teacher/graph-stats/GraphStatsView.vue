<template>
  <div class="stats-page">
    <!-- 顶部栏 -->
    <div class="page-header">
      <div class="header-left">
        <a-button type="link" class="back-btn" @click="goBack">
          <arrow-left-outlined /> 返回图谱
        </a-button>
        <h2><bar-chart-outlined class="title-icon" /> 知识图谱统计</h2>
      </div>
      <div class="header-filters">
        <a-select v-model:value="selectedClassId" size="middle" style="width: 160px" placeholder="选择班级">
          <a-select-option v-for="c in classList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
        </a-select>
      </div>
    </div>

    <!-- Tab 切换 -->
    <a-tabs v-model:activeKey="activeTab" class="stats-tabs" type="card">
      <a-tab-pane key="overview" tab="班级学情">
        <ClassOverviewTab :classId="selectedClassId" />
      </a-tab-pane>
      <a-tab-pane key="compare" tab="班级对比">
        <ClassCompareTab />
      </a-tab-pane>
      <a-tab-pane key="student" tab="学生画像">
        <StudentProfileTab :classId="selectedClassId" />
      </a-tab-pane>
      <a-tab-pane key="build" tab="知识点建设">
        <KnowledgeBuildTab :classId="selectedClassId" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BarChartOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue'
import ClassOverviewTab from './components/ClassOverviewTab.vue'
import ClassCompareTab from './components/ClassCompareTab.vue'
import StudentProfileTab from './components/StudentProfileTab.vue'
import KnowledgeBuildTab from './components/KnowledgeBuildTab.vue'
import { fetchClassList } from '@/api/courseGraphStats'

const router = useRouter()
const activeTab = ref('overview')
const selectedClassId = ref<number | undefined>(undefined)
const classList = ref<{ id: number; name: string }[]>([])

const loadClassList = async () => {
  try {
    const data = await fetchClassList()
    classList.value = (data || []).map((c: any) => ({ id: c.id, name: c.name }))
    if (classList.value.length > 0) {
      selectedClassId.value = classList.value[0].id
    }
  } catch {
    // 静默处理
  }
}

onMounted(() => {
  loadClassList()
})

const goBack = () => {
  router.push('/teacher/graph')
}
</script>

<style scoped>
.stats-page {
  font-family: 'Plus Jakarta Sans', sans-serif;
  background: #f8fafc;
  min-height: calc(100vh - 64px);
  padding: 24px 32px;
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  padding: 0;
  color: #64748b;
  font-size: 14px;
}
.back-btn:hover {
  color: #3b82f6;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  display: flex;
  align-items: center;
}

.title-icon {
  margin-right: 10px;
  font-size: 24px;
  color: #3b82f6;
}

.header-filters {
  display: flex;
  gap: 12px;
}

.stats-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}

.stats-tabs :deep(.ant-tabs-tab) {
  border-radius: 5px 5px 0 0;
  font-weight: 500;
  color: #64748b;
}

.stats-tabs :deep(.ant-tabs-tab-active) {
  color: #3b82f6;
  font-weight: 600;
}

@media (max-width: 768px) {
  .stats-page {
    padding: 16px;
  }
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
