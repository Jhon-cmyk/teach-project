<template>
  <div class="admin-ops-page">
    <section class="admin-ops-header">
      <div>
        <h2>教师配置跟踪</h2>
        <p>跟踪教师是否已经配置课程、AI资源和教学案例，用于判断新入驻教师是否真正开始使用平台。</p>
      </div>
      <div class="admin-ops-tags">
        <span>课程配置</span>
        <span>AI资源</span>
        <span>案例建设</span>
      </div>
    </section>

    <section class="admin-ops-toolbar">
      <div class="admin-ops-toolbar-left">
        <a-select v-model:value="query.status" class="admin-ops-select" :options="statusOptions" @change="handleSearch" />
        <a-input v-model:value="query.semester" class="admin-ops-input semester-input" placeholder="学期：2025-2026-2" allow-clear @pressEnter="handleSearch" />
        <a-input v-model:value="query.keyword" class="admin-ops-input" placeholder="搜索教师姓名或账号" allow-clear @pressEnter="handleSearch" />
      </div>
      <div class="admin-ops-toolbar-right">
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
      </div>
    </section>

    <section class="admin-ops-card">
      <div class="admin-ops-card-head">
        <div>
          <h3>教师配置跟踪</h3>
          <p>跟踪教师是否已经创建课程、AI资源或教学案例。</p>
        </div>
      </div>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 960 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'name'">
            <strong>{{ record.name }}</strong>
            <div class="muted">{{ record.account }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'configured' ? 'green' : 'default'">
              {{ record.status === 'configured' ? '已配置内容' : '未配置内容' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'content'">
            课程 {{ record.courseCount }} / AI {{ record.aiResourceCount }} / 案例 {{ record.caseCount }}
          </template>
          <template v-else-if="column.dataIndex === 'assignedCourses'">
            <div v-if="record.assignedCourses?.length" class="course-tags">
              <a-tag v-for="course in record.assignedCourses" :key="course.id" color="blue">
                {{ course.name }}
              </a-tag>
            </div>
            <span v-else class="muted">未设置本学期课程</span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>
          <template v-else-if="column.dataIndex === 'lastContentTime'">
            {{ formatDate(record.lastContentTime) }}
          </template>
        </template>
      </a-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getAdminTeacherTrackingList } from '@/api/admin'
import type { AdminTeacherTrackingItem, AdminTeacherTrackingParams } from '@/types/admin'
import './AdminOps.css'

const loading = ref(false)
const list = ref<AdminTeacherTrackingItem[]>([])
const total = ref(0)
const query = reactive<AdminTeacherTrackingParams>({
  current: 1,
  size: 10,
  status: '',
  keyword: '',
  semester: '2025-2026-2'
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已配置内容', value: 'configured' },
  { label: '未配置内容', value: 'not_configured' }
]

const columns = [
  { title: '教师', dataIndex: 'name', width: 190 },
  { title: '配置状态', dataIndex: 'status', width: 130 },
  { title: '本学期授课课程', dataIndex: 'assignedCourses', width: 300 },
  { title: '内容配置', dataIndex: 'content', width: 230 },
  { title: '内容总数', dataIndex: 'totalContentCount', width: 110 },
  { title: '入驻时间', dataIndex: 'createTime', width: 170 },
  { title: '最近内容更新', dataIndex: 'lastContentTime', width: 170 }
]

const pagination = computed(() => ({
  current: query.current,
  pageSize: query.size,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`
}))

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAdminTeacherTrackingList(query)
    list.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.current = 1
  loadList()
}

const handleReset = () => {
  query.current = 1
  query.size = 10
  query.status = ''
  query.keyword = ''
  query.semester = '2025-2026-2'
  loadList()
}

const handleTableChange = (pageInfo: any) => {
  query.current = pageInfo.current
  query.size = pageInfo.pageSize
  loadList()
}

const formatDate = (value?: string) => value ? value.replace('T', ' ').slice(0, 19) : '-'

onMounted(loadList)
</script>

<style scoped>
.muted {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.semester-input {
  width: 180px;
}

.course-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
