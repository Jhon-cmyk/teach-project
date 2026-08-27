<template>
  <div class="admin-page platform-case-page">
    <section class="page-header-card">
      <div class="page-copy">
        <h2 class="page-title">平台案例管理</h2>
        <p class="page-desc">
          统一维护平台案例库，支持本地上传、公开案例采集、审核发布与下架管理。
        </p>
      </div>

      <div class="page-tags">
        <span>上传案例</span>
        <span>公开采集</span>
        <span>审核发布</span>
      </div>
    </section>

    <section class="case-workbench">
      <div class="upload-card">
        <div class="workbench-icon upload-icon">
          <UploadOutlined />
        </div>
        <div class="workbench-copy">
          <h3>上传平台案例</h3>
          <p>上传 PDF 或 Word 案例，补充课程、分类、难度和关键词后发布到平台库。</p>
        </div>
        <a-button type="primary" class="upload-inline-btn" @click="openImportModal">
          <template #icon>
            <UploadOutlined />
          </template>
          选择本地文件
        </a-button>
      </div>

      <div class="crawl-card">
        <div class="workbench-icon crawl-icon">
          <CloudDownloadOutlined />
        </div>
        <div class="crawl-form">
          <div class="workbench-copy">
            <h3>采集公开案例</h3>
            <p>按关键词或指定公开 URL 抓取候选案例，采集后进入待审核。</p>
          </div>
          <div class="crawl-controls">
            <a-input
              v-model:value="crawlForm.keyword"
              class="crawl-keyword"
              placeholder="关键词，如 数据结构 / 高校计算机教学案例"
              allow-clear
            />
            <a-input
              v-model:value="crawlForm.sourceUrl"
              class="crawl-url"
              placeholder="指定公开案例 URL，可选"
              allow-clear
            />
            <a-button :loading="crawlLoading" @click="handleCrawl">
              <template #icon>
                <CloudDownloadOutlined />
              </template>
              采集案例
            </a-button>
          </div>
        </div>
      </div>
    </section>

    <section class="status-strip">
      <button
        v-for="item in statusTabs"
        :key="item.value || 'all'"
        :class="['status-tab', { active: normalizedStatus === item.value }]"
        type="button"
        @click="setStatusFilter(item.value)"
      >
        <span>{{ item.label }}</span>
        <strong v-if="item.count !== undefined">{{ item.count }}</strong>
      </button>
    </section>

    <a-card class="table-card case-table-card" :bordered="false">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <a-input
            v-model:value="query.keyword"
            class="search-input"
            placeholder="搜索标题、课程、摘要或关键词"
            allow-clear
            @pressEnter="handleSearch"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input>
          <a-select
            v-model:value="query.status"
            class="status-select"
            allow-clear
            placeholder="全部状态"
            @change="handleSearch"
          >
            <a-select-option value="pending">待审核</a-select-option>
            <a-select-option value="approved">已发布</a-select-option>
            <a-select-option value="rejected">已驳回</a-select-option>
            <a-select-option value="offline">已下架</a-select-option>
          </a-select>
        </div>

        <div class="toolbar-right">
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">
            <template #icon>
              <ReloadOutlined />
            </template>
            重置
          </a-button>
        </div>
      </div>

      <a-table
        row-key="id"
        :loading="loading"
        :columns="columns"
        :data-source="caseList"
        :pagination="pagination"
        :locale="{ emptyText }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div class="case-title-cell">
              <strong>{{ record.title }}</strong>
              <span>{{ record.summary || '暂无摘要' }}</span>
            </div>
          </template>

          <template v-else-if="column.key === 'course'">
            <div class="course-cell">
              <span>{{ record.courseName || '未指定课程' }}</span>
              <small>{{ categoryLabel(record.category) }}</small>
            </div>
          </template>

          <template v-else-if="column.key === 'status'">
            <span :class="['status-pill', `status-${record.status || 'pending'}`]">
              {{ statusLabel(record.status) }}
            </span>
          </template>

          <template v-else-if="column.key === 'source'">
            <a
              class="source-link"
              :href="record.sourceUrl || record.pdfUrl"
              target="_blank"
              rel="noreferrer"
            >
              {{ record.sourceName || '公开来源' }}
            </a>
          </template>

          <template v-else-if="column.key === 'materials'">
            <a-tooltip title="采集时识别到的图片、文档、PPT、视频或课程资源链接数量">
              <span class="material-count">{{ materialCount(record.materialJson) }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.key === 'action'">
            <div class="action-group">
              <a-button size="small" type="link" @click="openPreview(record)">预览</a-button>
              <a-button size="small" type="link" @click="openEdit(record)">编辑</a-button>
              <a-button
                v-if="record.status !== 'approved'"
                size="small"
                type="link"
                @click="handleApprove(record.id)"
              >
                发布
              </a-button>
              <a-button
                v-if="record.status === 'approved'"
                size="small"
                type="link"
                danger
                @click="confirmOffline(record)"
              >
                下架
              </a-button>
              <a-button
                v-if="record.status === 'pending'"
                size="small"
                type="link"
                danger
                @click="confirmReject(record)"
              >
                驳回
              </a-button>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <TeachingCasePreviewModal ref="casePreviewRef" />

    <a-modal
      v-model:open="editOpen"
      title="编辑平台案例"
      width="1100px"
      centered
      class="admin-wide-modal"
      :confirm-loading="editLoading"
      @ok="handleSaveEdit"
    >
      <a-form layout="vertical" :model="editForm">
        <div class="form-grid">
          <a-form-item label="案例标题" required>
            <a-input v-model:value="editForm.title" />
          </a-form-item>
          <a-form-item label="适用课程">
            <a-input v-model:value="editForm.courseName" />
          </a-form-item>
          <a-form-item label="分类">
            <a-select v-model:value="editForm.category">
              <a-select-option value="course_design">课程设计</a-select-option>
              <a-select-option value="enterprise">企业工程</a-select-option>
              <a-select-option value="competition">大赛资源</a-select-option>
              <a-select-option value="small_project">小项目</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="难度">
            <a-select v-model:value="editForm.difficulty">
              <a-select-option value="easy">初级</a-select-option>
              <a-select-option value="medium">中等</a-select-option>
              <a-select-option value="hard">困难</a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <a-form-item label="摘要">
          <a-textarea v-model:value="editForm.summary" :rows="4" />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input v-model:value="editForm.keywords" placeholder="用逗号分隔" />
        </a-form-item>
        <a-form-item label="原始链接">
          <a-input v-model:value="editForm.sourceUrl" />
        </a-form-item>
        <a-form-item label="素材链接 JSON">
          <a-textarea v-model:value="editForm.materialJson" :rows="4" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="importOpen"
      title="上传平台案例"
      width="720px"
      centered
      class="case-import-modal"
      :confirm-loading="importLoading"
      ok-text="上传案例"
      cancel-text="取消"
      @ok="handleImport"
      @cancel="closeImportModal"
    >
      <a-form class="case-import-form" layout="vertical" :model="importForm">
        <div class="form-grid import-form-grid">
          <a-form-item label="案例标题" required>
            <a-input v-model:value="importForm.title" placeholder="请输入案例标题" />
          </a-form-item>
          <a-form-item label="适用课程">
            <a-input v-model:value="importForm.courseName" placeholder="例如：数据结构" />
          </a-form-item>
          <a-form-item label="分类" required>
            <a-select v-model:value="importForm.category" placeholder="请选择分类">
              <a-select-option value="course_design">课程设计</a-select-option>
              <a-select-option value="enterprise">企业工程</a-select-option>
              <a-select-option value="competition">大赛资源</a-select-option>
              <a-select-option value="small_project">小项目</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="难度" required>
            <a-select v-model:value="importForm.difficulty" placeholder="请选择难度">
              <a-select-option value="easy">初级</a-select-option>
              <a-select-option value="medium">中等</a-select-option>
              <a-select-option value="hard">困难</a-select-option>
            </a-select>
          </a-form-item>
        </div>

        <a-form-item class="case-file-field" label="案例文件" required>
          <a-upload
            :file-list="caseFileList"
            :max-count="1"
            accept=".pdf,.doc,.docx"
            :custom-request="handleCaseUpload"
            @remove="handleCaseRemove"
          >
            <a-button :loading="caseUploading">
              <template #icon>
                <UploadOutlined />
              </template>
              {{ caseUploading ? '上传中...' : '选择 PDF / Word 文件' }}
            </a-button>
          </a-upload>
          <div v-if="caseFileName" class="form-hint">已上传：{{ caseFileName }}</div>
        </a-form-item>

        <a-form-item label="摘要">
          <a-textarea
            v-model:value="importForm.summary"
            :rows="3"
            placeholder="可选；为空时系统会使用文档正文预览"
          />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input
            v-model:value="importForm.keywords"
            placeholder="用逗号分隔，例如：队列,循环队列,数据结构"
          />
        </a-form-item>
        <a-form-item label="发布状态">
          <a-select v-model:value="importForm.status">
            <a-select-option value="approved">直接发布</a-select-option>
            <a-select-option value="pending">待审核</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  approveAdminTeachingCase,
  crawlAdminTeachingCases,
  getAdminTeachingCasePage,
  importAdminTeachingCase,
  offlineAdminTeachingCase,
  rejectAdminTeachingCase,
  updateAdminTeachingCase,
  type TeachingCaseItem,
} from '@/api/case'
import {
  CloudDownloadOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { getAdminDashboardMetrics, uploadAdminFile } from '@/api/admin'
import type { AdminDashboardMetrics } from '@/types/admin'
import TeachingCasePreviewModal from '@/components/teacher/TeachingCasePreviewModal.vue'

const loading = ref(false)
const crawlLoading = ref(false)
const editOpen = ref(false)
const editLoading = ref(false)
const importOpen = ref(false)
const importLoading = ref(false)
const caseUploading = ref(false)
const caseFileList = ref<any[]>([])
const caseFileName = ref('')
const caseList = ref<TeachingCaseItem[]>([])
const dashboardMetrics = ref<AdminDashboardMetrics | null>(null)
const casePreviewRef = ref<InstanceType<typeof TeachingCasePreviewModal> | null>(null)

const crawlForm = reactive({
  keyword: '高校计算机 教学案例',
  sourceUrl: '',
})

const query = reactive({
  keyword: '',
  status: 'approved' as string | undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
})

const editForm = reactive<Partial<TeachingCaseItem> & { id?: number }>({})

const importForm = reactive({
  title: '',
  category: 'course_design',
  difficulty: 'medium',
  courseName: '',
  pdfUrl: '',
  summary: '',
  keywords: '',
  status: 'approved',
})

const columns = [
  { title: '案例', key: 'title', width: 420 },
  { title: '课程 / 分类', key: 'course', width: 180 },
  { title: '状态', key: 'status', width: 110 },
  { title: '来源', key: 'source', width: 150 },
  { title: '素材', key: 'materials', width: 90, align: 'center' as const },
  { title: '操作', key: 'action', width: 180, align: 'right' as const },
]

const emptyText = computed(() => {
  if (loading.value) return '正在加载平台案例...'
  return '暂无平台案例'
})

const normalizedStatus = computed(() => query.status || undefined)

const statusTabs = computed(() => [
  { label: '全部', value: undefined, count: dashboardMetrics.value?.totalPlatformCases ?? 0 },
  { label: '待审核', value: 'pending', count: dashboardMetrics.value?.pendingPlatformCases ?? 0 },
  { label: '已发布', value: 'approved', count: dashboardMetrics.value?.approvedPlatformCases ?? 0 },
  { label: '已驳回', value: 'rejected', count: dashboardMetrics.value?.rejectedPlatformCases ?? 0 },
  { label: '已下架', value: 'offline', count: dashboardMetrics.value?.offlinePlatformCases ?? 0 },
])

const statusLabel = (status?: string) => ({
  pending: '待审核',
  approved: '已发布',
  rejected: '已驳回',
  offline: '已下架',
}[status || 'pending'] || status || '待审核')

const categoryLabel = (category?: string) => ({
  course_design: '课程设计',
  enterprise: '企业工程',
  competition: '大赛资源',
  small_project: '小项目',
}[category || ''] || '未分类')

const materialCount = (json?: string) => {
  try {
    const parsed = JSON.parse(json || '[]')
    return Array.isArray(parsed) ? parsed.length : 0
  } catch {
    return 0
  }
}

const loadCases = async () => {
  loading.value = true
  try {
    const page = await getAdminTeachingCasePage({
      current: pagination.current,
      size: pagination.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
    })
    caseList.value = page.records || []
    pagination.total = page.total || 0
    dashboardMetrics.value = await getAdminDashboardMetrics()
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadCases()
}

const handleReset = () => {
  query.keyword = ''
  query.status = 'approved'
  pagination.current = 1
  loadCases()
}

const setStatusFilter = (status?: string) => {
  query.status = status
  handleSearch()
}

const handleTableChange = (pager: any) => {
  pagination.current = pager.current
  pagination.pageSize = pager.pageSize
  loadCases()
}

const handleCrawl = async () => {
  if (!crawlForm.keyword && !crawlForm.sourceUrl) {
    message.warning('请输入关键词或指定公开案例 URL')
    return
  }
  crawlLoading.value = true
  try {
    await crawlAdminTeachingCases({
      keyword: crawlForm.keyword || undefined,
      sourceUrl: crawlForm.sourceUrl || undefined,
    })
    pagination.current = 1
    query.status = 'pending'
    await loadCases()
  } finally {
    crawlLoading.value = false
  }
}

const openImportModal = () => {
  importForm.title = ''
  importForm.category = 'course_design'
  importForm.difficulty = 'medium'
  importForm.courseName = ''
  importForm.pdfUrl = ''
  importForm.summary = ''
  importForm.keywords = ''
  importForm.status = 'approved'
  caseFileList.value = []
  caseFileName.value = ''
  importOpen.value = true
}

const closeImportModal = () => {
  importOpen.value = false
}

const isSupportedCaseFile = (fileName: string) => /\.(pdf|doc|docx)$/i.test(fileName)

const handleCaseUpload = async ({ file, onSuccess, onError }: any) => {
  const fileName = String(file?.name || '')
  if (!isSupportedCaseFile(fileName)) {
    const error = new Error('仅支持 PDF、Word(.doc/.docx) 文件')
    message.warning(error.message)
    onError?.(error)
    return
  }

  caseUploading.value = true
  try {
    const url = await uploadAdminFile(file, 'case')
    importForm.pdfUrl = url
    caseFileName.value = file.name
    caseFileList.value = [{ uid: file.uid, name: file.name, status: 'done', url }]
    onSuccess?.(url)
    message.success('案例文件上传成功')
  } catch (error: any) {
    message.error(error?.message || '案例文件上传失败')
    onError?.(error)
  } finally {
    caseUploading.value = false
  }
}

const handleCaseRemove = () => {
  importForm.pdfUrl = ''
  caseFileName.value = ''
  caseFileList.value = []
}

const handleImport = async () => {
  if (!importForm.title.trim()) {
    return message.warning('案例标题不能为空')
  }
  if (!importForm.category || !importForm.difficulty) {
    return message.warning('请选择分类和难度')
  }
  if (!importForm.pdfUrl) {
    return message.warning('请上传案例文件')
  }

  importLoading.value = true
  try {
    await importAdminTeachingCase({
      title: importForm.title.trim(),
      category: importForm.category,
      difficulty: importForm.difficulty,
      courseName: importForm.courseName.trim(),
      pdfUrl: importForm.pdfUrl,
      summary: importForm.summary.trim(),
      keywords: importForm.keywords.trim(),
      status: importForm.status,
      sourceName: '平台上传',
    })
    importOpen.value = false
    query.status = importForm.status
    pagination.current = 1
    await loadCases()
  } finally {
    importLoading.value = false
  }
}

const openEdit = (record: TeachingCaseItem) => {
  Object.assign(editForm, record)
  editOpen.value = true
}

const openPreview = (record: TeachingCaseItem) => {
  casePreviewRef.value?.open(record.id)
}

const handleSaveEdit = async () => {
  if (!editForm.id || !editForm.title) {
    message.warning('案例标题不能为空')
    return
  }
  editLoading.value = true
  try {
    await updateAdminTeachingCase(editForm as any)
    editOpen.value = false
    await loadCases()
  } finally {
    editLoading.value = false
  }
}

const handleApprove = async (id: number) => {
  await approveAdminTeachingCase(id)
  await loadCases()
}

const confirmReject = (record: TeachingCaseItem) => {
  Modal.confirm({
    title: '驳回该案例？',
    content: `驳回后教师端不会看到“${record.title}”。`,
    okText: '驳回案例',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      await rejectAdminTeachingCase(record.id)
      await loadCases()
    },
  })
}

const confirmOffline = (record: TeachingCaseItem) => {
  Modal.confirm({
    title: '下架该案例？',
    content: `下架后“${record.title}”将不再参与教师端检索和推荐。`,
    okText: '下架案例',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      await offlineAdminTeachingCase(record.id)
      await loadCases()
    },
  })
}

onMounted(loadCases)
</script>

<style scoped>
.platform-case-page {
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

.secondary-action,
.upload-inline-btn {
  height: 38px;
  border-radius: 10px;
  font-weight: 600;
}

.secondary-action {
  border-color: #dfe7f2;
  color: #445268;
}

.case-workbench {
  display: grid;
  grid-template-columns: minmax(300px, 0.88fr) minmax(460px, 1.42fr);
  gap: 18px;
}

.upload-card,
.crawl-card,
.status-strip {
  background: #ffffff;
  border: 1px solid #e8eef7;
  border-radius: 18px;
}

.upload-card,
.crawl-card {
  min-height: 142px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.upload-card {
  border-color: #e8eef7;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
}

.workbench-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.upload-icon {
  color: #1e4ed8;
  background: #eef4ff;
}

.crawl-icon {
  color: #475467;
  background: #f3f6fa;
}

.workbench-copy {
  min-width: 0;
  flex: 1;
}

.workbench-copy h3 {
  margin: 0;
  color: #182230;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
}

.workbench-copy p {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
}

.upload-inline-btn {
  min-width: 132px;
  flex-shrink: 0;
}

.crawl-form {
  min-width: 0;
  flex: 1;
}

.crawl-controls {
  margin-top: 12px;
  display: grid;
  grid-template-columns: minmax(160px, 0.9fr) minmax(220px, 1.15fr) auto;
  gap: 10px;
  align-items: center;
}

.crawl-controls :deep(.ant-input),
.table-toolbar :deep(.ant-input),
.table-toolbar :deep(.ant-select-selector) {
  border-radius: 10px;
}

.status-strip {
  padding: 10px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.status-tab {
  height: 62px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: #667085;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 0 14px;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.status-tab:hover {
  background: #f7faff;
  color: #1e4ed8;
}

.status-tab.active {
  background: #eef4ff;
  border-color: #cfe0ff;
  color: #1e4ed8;
}

.status-tab span {
  font-size: 13px;
  font-weight: 600;
}

.status-tab strong {
  margin-top: 4px;
  color: #182230;
  font-size: 22px;
  line-height: 1;
}

.case-table-card {
  border-radius: 20px;
  border: 1px solid #e8eef7;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.table-toolbar {
  padding: 18px 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e8eef7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-left {
  min-width: 0;
  flex: 1;
}

.search-input {
  width: min(420px, 100%);
}

.status-select {
  width: 150px;
  flex-shrink: 0;
}

.case-title-cell,
.course-cell {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.case-title-cell {
  gap: 5px;
}

.case-title-cell strong {
  color: #182230;
  font-weight: 700;
  line-height: 1.35;
}

.case-title-cell span {
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-cell span {
  color: #182230;
  font-weight: 600;
}

.course-cell small {
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.status-pending {
  color: #9a5b00;
  background: #fff4d6;
}

.status-approved {
  color: #15803d;
  background: #dcfce7;
}

.status-rejected {
  color: #b42318;
  background: #fee4e2;
}

.status-offline {
  color: #475467;
  background: #eef2f6;
}

.source-link {
  color: #1e4ed8;
  font-weight: 600;
}

.material-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 24px;
  border-radius: 999px;
  background: #eef4ff;
  color: #1e4ed8;
  font-weight: 700;
}

.action-group {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  flex-wrap: wrap;
}

.form-hint {
  margin-top: 8px;
  color: #667085;
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.case-import-form {
  margin-top: 2px;
}

.case-import-form :deep(.ant-form-item) {
  margin-bottom: 13px;
}

.case-import-form :deep(.ant-form-item-label) {
  padding-bottom: 5px;
}

.case-import-form :deep(.ant-input),
.case-import-form :deep(.ant-select-selector),
.case-import-form :deep(.ant-btn) {
  border-radius: 8px;
}

.case-import-form :deep(textarea.ant-input) {
  min-height: 76px;
}

.import-form-grid {
  gap: 10px 12px;
}

.case-file-field {
  padding: 12px;
  border: 1px solid #e7edf5;
  border-radius: 8px;
  background: #f8fafc;
}

.case-file-field :deep(.ant-upload-list) {
  margin-top: 8px;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fbff !important;
  color: #344054;
  font-weight: 600;
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fbff !important;
}

@media (max-width: 1180px) {
  .page-header-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .case-workbench {
    grid-template-columns: 1fr;
  }

  .crawl-controls {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .table-toolbar,
  .toolbar-left,
  .toolbar-right {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-right {
    width: 100%;
  }

  .toolbar-right > * {
    flex: 1;
  }

  .status-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .upload-card,
  .crawl-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .upload-inline-btn,
  .search-input,
  .status-select {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .case-import-modal {
    width: calc(100vw - 24px) !important;
  }
}
</style>
