<template>
  <div class="admin-page">
    <section class="page-header-card">
      <div class="page-copy">
        <h2 class="page-title">AI资源管理</h2>
        <p class="page-desc">
          统一查看教师侧生成的教案、题库与互动课件资源，管理员仅负责查看详情和下架已发布资源。
        </p>
      </div>

      <div class="page-tags">
        <span>总资源 {{ totalResourceCount }}</span>
        <span>已发布 {{ publishedCount }}</span>
        <span>教案 {{ planCount }}</span>
        <span>题库 {{ quizCount }}</span>
        <span>课件 {{ animCount }}</span>
      </div>
    </section>

    <section class="toolbar-card">
      <div class="toolbar-left">
        <a-select
          v-model:value="query.type"
          class="toolbar-select"
          :options="typeOptions"
          @change="handleSearch"
        />
        <a-select
          v-model:value="query.isPublished"
          class="toolbar-select"
          :options="publishOptions"
          @change="handleSearch"
        />
        <a-input
          v-model:value="query.teacherKeyword"
          class="toolbar-select wide-input"
          placeholder="按教师名称或账号筛选"
          allow-clear
          @pressEnter="handleSearch"
        />
      </div>

      <div class="toolbar-right">
        <a-input
          v-model:value="query.title"
          class="search-input"
          placeholder="按资源标题搜索"
          allow-clear
          @pressEnter="handleSearch"
        >
          <template #prefix>
            <SearchOutlined />
          </template>
        </a-input>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">
          <template #icon>
            <ReloadOutlined />
          </template>
          重置
        </a-button>
      </div>
    </section>

    <a-card class="table-card" :bordered="false">
      <div class="table-card-head">
        <strong>资源列表</strong>
        <span>支持预览和下架</span>
      </div>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="resourceList"
        :loading="loading"
        :pagination="pagination"
        :locale="{ emptyText }"
        :scroll="{ x: 960 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeText(record.type) }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'isPublished'">
            <a-tag :color="record.isPublished ? 'green' : 'default'">
              {{ record.isPublished ? '已发布' : '未发布' }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>

          <template v-else-if="column.key === 'action'">
            <div class="action-group">
              <a-button type="link" @click="openPreview(record)">
                查看详情
              </a-button>

              <a-button
                v-if="record.isPublished"
                type="link"
                @click="handleUnpublish(record)"
              >
                下架
              </a-button>

              <a-tag v-else color="default">仅可查看</a-tag>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailOpen"
      width="1100px"
      title="资源详情预览"
      :footer="null"
      centered
      class="admin-ai-preview-modal teacher-wide-modal"
      destroyOnClose
    >
      <div v-if="detailLoading" class="drawer-loading">
        <a-spin />
      </div>

      <a-result
        v-else-if="detailErrorMessage"
        status="error"
        title="资源详情加载失败"
        :sub-title="detailErrorMessage"
      />

      <div v-else-if="detailItem" class="detail-panel">
        <div class="detail-meta-grid">
          <div class="detail-meta-item">
            <span class="meta-label">资源标题</span>
            <strong class="meta-value">{{ detailItem.title }}</strong>
          </div>
          <div class="detail-meta-item">
            <span class="meta-label">资源类型</span>
            <strong class="meta-value">{{ getTypeText(detailItem.type) }}</strong>
          </div>
          <div class="detail-meta-item">
            <span class="meta-label">创建教师</span>
            <strong class="meta-value">{{ detailItem.teacherName || '未命名教师' }}</strong>
          </div>
          <div class="detail-meta-item">
            <span class="meta-label">发布状态</span>
            <strong class="meta-value">{{ detailItem.isPublished ? '已发布' : '未发布' }}</strong>
          </div>
          <div class="detail-meta-item">
            <span class="meta-label">创建时间</span>
            <strong class="meta-value">{{ formatDate(detailItem.createTime) }}</strong>
          </div>
          <div class="detail-meta-item">
            <span class="meta-label">更新时间</span>
            <strong class="meta-value">{{ formatDate(detailItem.updateTime) }}</strong>
          </div>
        </div>

        <div class="detail-block">
          <h4>内容预览</h4>
          <a-empty v-if="!detailItem.content" description="资源内容为空，暂无可展示内容" />
          <div v-else class="detail-content" v-html="previewContentHtml"></div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import {
  getAdminAiResourceDetail,
  getAdminDashboardMetrics,
  getAdminAiResourceList,
  unpublishAdminAiResource
} from '@/api/admin'
import type { AdminAiResourceItem, AdminAiResourceListParams, AdminDashboardMetrics } from '@/types/admin'

const loading = ref(false)
const detailLoading = ref(false)
const detailOpen = ref(false)
const resourceList = ref<AdminAiResourceItem[]>([])
const detailItem = ref<AdminAiResourceItem | null>(null)
const detailErrorMessage = ref('')
const total = ref(0)
const dashboardMetrics = ref<AdminDashboardMetrics | null>(null)
const md = new MarkdownIt({ breaks: true, html: true })

const query = reactive<AdminAiResourceListParams>({
  current: 1,
  size: 10,
  title: '',
  type: '',
  teacherKeyword: '',
  isPublished: ''
})

const columns = [
  {
    title: '资源标题',
    dataIndex: 'title',
    ellipsis: true
  },
  {
    title: '资源类型',
    dataIndex: 'type',
    width: 120
  },
  {
    title: '创建教师',
    dataIndex: 'teacherName',
    width: 140
  },
  {
    title: '发布状态',
    dataIndex: 'isPublished',
    width: 120
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 300
  }
]

const typeOptions = [
  { label: '全部类型', value: '' },
  { label: '教案', value: 'plan' },
  { label: '题库', value: 'quiz' },
  { label: '互动课件', value: 'anim' }
]

const publishOptions = [
  { label: '全部状态', value: '' },
  { label: '已发布', value: 1 },
  { label: '未发布', value: 0 }
]

const pagination = computed(() => ({
  current: query.current,
  pageSize: query.size,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`
}))

const emptyText = computed(() => {
  if (loading.value) return 'AI 资源列表加载中...'
  if (query.title || query.type || query.teacherKeyword || query.isPublished !== '') {
    return '没有找到符合条件的 AI 资源，请调整筛选条件后重试'
  }
  return '当前暂无 AI 资源'
})

const totalResourceCount = computed(() => dashboardMetrics.value?.totalAiResources ?? total.value)
const publishedCount = computed(() => dashboardMetrics.value?.publishedAiResources ?? 0)
const planCount = computed(() => dashboardMetrics.value?.aiPlanResources ?? 0)
const quizCount = computed(() => dashboardMetrics.value?.aiQuizResources ?? 0)
const animCount = computed(() => dashboardMetrics.value?.aiAnimResources ?? 0)

const previewContentHtml = computed(() => renderResourceContent(detailItem.value?.content || ''))

const loadMetrics = async () => {
  dashboardMetrics.value = await getAdminDashboardMetrics()
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAdminAiResourceList(query)
    resourceList.value = res?.records || []
    total.value = res?.total || 0
    await loadMetrics()
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
  query.title = ''
  query.type = ''
  query.teacherKeyword = ''
  query.isPublished = ''
  loadList()
}

const handleTableChange = (pageInfo: any) => {
  query.current = pageInfo.current
  query.size = pageInfo.pageSize
  loadList()
}

const openPreview = async (record: AdminAiResourceItem) => {
  detailOpen.value = true
  detailLoading.value = true
  detailErrorMessage.value = ''
  detailItem.value = null
  try {
    const res = await getAdminAiResourceDetail(record.id)
    detailItem.value = res
  } catch (error: any) {
    detailErrorMessage.value = error?.message || '资源详情加载失败，请稍后重试'
  } finally {
    detailLoading.value = false
  }
}

const handleUnpublish = (record: AdminAiResourceItem) => {
  Modal.confirm({
    title: '下架资源',
    content: `确定下架资源“${record.title}”吗？下架后学生端将不再展示该资源。`,
    okText: '下架',
    okButtonProps: { danger: true },
    onOk: async () => {
      await unpublishAdminAiResource(record.id)
      loadList()
      if (detailItem.value?.id === record.id) {
        detailItem.value.isPublished = 0
      }
    }
  })
}

const getTypeText = (type?: string) => {
  if (type === 'plan') return '教案'
  if (type === 'quiz') return '题库'
  if (type === 'anim') return '互动课件'
  return '未知类型'
}

const getTypeColor = (type?: string) => {
  if (type === 'plan') return 'purple'
  if (type === 'quiz') return 'blue'
  if (type === 'anim') return 'orange'
  return 'default'
}

const formatDate = (value?: string) => {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}

const sanitizeResourceHtml = (value: string) => {
  if (!value) return ''

  const parser = new DOMParser()
  const doc = parser.parseFromString(value, 'text/html')
  const blockedTags = ['script', 'style', 'iframe', 'object', 'embed', 'link', 'meta', 'form']

  blockedTags.forEach((tag) => {
    doc.body.querySelectorAll(tag).forEach((node) => node.remove())
  })

  doc.body.querySelectorAll('*').forEach((node) => {
    Array.from(node.attributes).forEach((attr) => {
      const name = attr.name.toLowerCase()
      const val = attr.value.trim().toLowerCase()
      if (
        name.startsWith('on') ||
        name === 'style' ||
        val.startsWith('javascript:') ||
        val.startsWith('data:text/html')
      ) {
        node.removeAttribute(attr.name)
      }
    })
  })

  return doc.body.innerHTML
}

const renderResourceContent = (value: string) => {
  if (!value) return ''
  const processed = value.replace(
    /【案例参考】/g,
    '<span class="case-badge">📖 案例参考</span>'
  )
  return sanitizeResourceHtml(md.render(processed))
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header-card {
  padding: 26px 28px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #e8eef7;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.04);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: #182230;
}

.page-desc {
  margin: 10px 0 0;
  max-width: 820px;
  font-size: 14px;
  line-height: 1.9;
  color: #667085;
}

.page-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.page-tags span {
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: #eef4ff;
  color: #1e4ed8;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  border: 1px solid #dce8ff;
}

.toolbar-card {
  padding: 18px 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e8eef7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-select {
  width: 150px;
}

.wide-input {
  width: 220px;
}

.search-input {
  width: 260px;
}

.table-card {
  border-radius: 20px;
  border: 1px solid #e8eef7;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.table-card-head {
  margin-bottom: 10px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.table-card-head strong {
  color: #0f172a;
  font-size: 16px;
}

.table-card-head span {
  color: #64748b;
  font-size: 12px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.drawer-loading {
  padding: 32px 0;
  display: flex;
  justify-content: center;
}

.detail-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}

.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.detail-meta-item {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #ebf1fb;
}

.meta-label {
  display: block;
  font-size: 12px;
  color: #7a8699;
}

.meta-value {
  display: block;
  margin-top: 6px;
  color: #182230;
  line-height: 1.45;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.detail-block {
  padding: 14px 16px 16px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e8eef7;
}

.detail-block:last-child {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.detail-block h4 {
  margin: 0 0 10px;
  color: #182230;
  font-size: 15px;
  line-height: 1.4;
}

.detail-content {
  padding: 18px 20px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #edf2f7;
  color: #25324a;
  font: 15px/1.8 "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.detail-content :deep(h1),
.detail-content :deep(h2),
.detail-content :deep(h3),
.detail-content :deep(h4) {
  margin: 18px 0 10px;
  color: #111827;
  font-weight: 700;
  line-height: 1.35;
}

.detail-content :deep(h1) {
  margin-top: 0;
  font-size: 22px;
}

.detail-content :deep(h2) {
  font-size: 18px;
}

.detail-content :deep(h3) {
  font-size: 16px;
}

.detail-content :deep(h4) {
  font-size: 15px;
}

.detail-content :deep(p) {
  margin: 8px 0;
}

.detail-content :deep(ol),
.detail-content :deep(ul) {
  margin: 8px 0 12px;
  padding-left: 22px;
}

.detail-content :deep(li) {
  margin: 5px 0;
  padding-left: 2px;
}

.detail-content :deep(strong) {
  color: #182230;
  font-weight: 700;
}

.detail-content :deep(code) {
  padding: 2px 5px;
  border-radius: 5px;
  background: #eef4ff;
  color: #1d4ed8;
  font-size: 0.92em;
}

.detail-content :deep(.case-badge) {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.detail-content :deep(pre) {
  padding: 12px;
  border-radius: 8px;
  background: #111827;
  color: #f8fafc;
  overflow: auto;
}

.detail-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}

.detail-content :deep(th),
.detail-content :deep(td) {
  padding: 8px 10px;
  border: 1px solid #dbe4f0;
  text-align: left;
  vertical-align: top;
}

.detail-content :deep(th) {
  background: #eef4ff;
  color: #182230;
  font-weight: 700;
}

:deep(.admin-ai-preview-modal) {
  max-width: calc(100vw - 48px);
}

:deep(.admin-ai-preview-modal .ant-modal-content) {
  border-radius: 10px;
}

:deep(.admin-ai-preview-modal .ant-modal-header) {
  padding: 18px 22px 12px;
}

:deep(.admin-ai-preview-modal .ant-modal-body) {
  height: 680px;
  max-height: calc(100vh - 160px);
  padding: 16px 22px 22px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

:deep(.ant-table-wrapper .ant-table-thead > tr > th) {
  background: #f8fbff;
  color: #344054;
  font-weight: 600;
}

:deep(.ant-card-body) {
  padding: 20px;
}

@media (max-width: 1200px) {
  .page-header-card,
  .toolbar-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .page-tags {
    justify-content: flex-start;
  }

  .detail-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .detail-meta-grid {
    grid-template-columns: 1fr;
  }

  :deep(.admin-ai-preview-modal) {
    max-width: calc(100vw - 24px);
  }

  :deep(.admin-ai-preview-modal .ant-modal-body) {
    padding: 10px 14px 16px;
  }
}
</style>
