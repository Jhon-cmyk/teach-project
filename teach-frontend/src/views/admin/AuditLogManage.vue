<template>
  <div class="admin-ops-page">
    <section class="admin-ops-header">
      <div>
        <h2>审计日志</h2>
        <p>记录管理员登录和关键写操作，包含操作模块、对象、IP地址和北京时间，方便追踪谁在什么时候做了什么。</p>
      </div>
      <div class="admin-ops-tags">
        <span>管理员操作</span>
        <span>IP地址</span>
        <span>北京时间</span>
      </div>
    </section>

    <section class="admin-ops-toolbar">
      <div class="admin-ops-toolbar-left">
        <a-select v-model:value="query.module" class="admin-ops-select" :options="moduleOptions" @change="handleSearch" />
        <a-input
          v-model:value="query.keyword"
          class="admin-ops-input"
          placeholder="搜索管理员、动作、对象、摘要"
          allow-clear
          @pressEnter="handleSearch"
        />
      </div>
      <div class="admin-ops-toolbar-right">
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
      </div>
    </section>

    <section class="admin-ops-card">
      <div class="admin-ops-card-head">
        <div>
          <h3>审计日志</h3>
          <p>记录管理员关键操作，方便追踪谁在什么时间修改了什么。</p>
        </div>
      </div>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1120 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'module'">
            <a-tag color="blue">{{ record.module }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'adminName'">
            <strong>{{ record.adminName || record.adminAccount }}</strong>
            <div class="muted">{{ record.adminAccount || '-' }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'targetId'">
            <span>{{ formatTarget(record) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'requestIp'">
            {{ record.requestIp || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>
        </template>
      </a-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getAdminAuditLogList } from '@/api/admin'
import type { AdminAuditLogItem, AdminAuditLogParams } from '@/types/admin'
import './AdminOps.css'

const loading = ref(false)
const list = ref<AdminAuditLogItem[]>([])
const total = ref(0)
const query = reactive<AdminAuditLogParams>({ current: 1, size: 10, module: '', keyword: '' })

const moduleOptions = [
  { label: '全部模块', value: '' },
  { label: '班级专业管理', value: '班级专业管理' },
  { label: '导入导出中心', value: '导入导出中心' },
  { label: '用户管理', value: '用户管理' },
  { label: 'AI资源管理', value: 'AI资源管理' },
  { label: '接口服务配置', value: '接口服务配置' }
]

const targetTypeText: Record<string, string> = {
  user: '用户',
  sys_class: '班级',
  ai_resource: 'AI资源',
  ai_model_config: '模型配置'
}

const columns = [
  { title: '模块', dataIndex: 'module', width: 150 },
  { title: '动作', dataIndex: 'action', width: 150 },
  { title: '管理员', dataIndex: 'adminName', width: 170 },
  { title: '操作对象', dataIndex: 'targetId', width: 170 },
  { title: '摘要', dataIndex: 'summary', ellipsis: true },
  { title: 'IP地址', dataIndex: 'requestIp', width: 150 },
  { title: '北京时间', dataIndex: 'createTime', width: 180 }
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
    const res = await getAdminAuditLogList(query)
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
  query.module = ''
  query.keyword = ''
  loadList()
}

const handleTableChange = (pageInfo: any) => {
  query.current = pageInfo.current
  query.size = pageInfo.pageSize
  loadList()
}

const formatTarget = (record: AdminAuditLogItem) => {
  const type = targetTypeText[record.targetType || ''] || record.targetType || '对象'
  if (!record.targetId) {
    return type
  }
  return `${type} #${record.targetId}`
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
</style>
