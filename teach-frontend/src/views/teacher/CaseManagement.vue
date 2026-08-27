<template>
  <div class="case-page modern-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="title-group">
        <h2><snippets-outlined class="title-icon" /> 案例管理</h2>
        <p class="subtitle">管理我的教学案例；本地上传的案例仅供本人备课和教案生成使用。</p>
      </div>
      <div class="header-actions">
        <a-button type="primary" class="add-btn" @click="openImportModal">
          <upload-outlined /> 本地上传
        </a-button>
      </div>
    </div>

    <!-- 筛选 + 视图切换 -->
    <div class="filter-dashboard glass-panel">
      <div class="filter-group">
        <div class="filter-item">
          <a-input
            v-model:value="filter.keyword"
            class="case-search-input"
            placeholder="搜索案例标题或适用课程"
            style="width: 260px"
            allow-clear
            @pressEnter="loadCaseList"
          >
            <template #suffix>
              <search-outlined class="case-search-icon" @click="loadCaseList" />
            </template>
          </a-input>
        </div>
        <div class="filter-item">
          <span class="label">分类</span>
          <a-select v-model:value="filter.category" style="width: 150px" placeholder="全部" allow-clear @change="loadCaseList">
            <a-select-option value="course_design">课程设计</a-select-option>
            <a-select-option value="enterprise">企业实际工程</a-select-option>
            <a-select-option value="competition">大赛资源</a-select-option>
            <a-select-option value="small_project">小项目</a-select-option>
          </a-select>
        </div>
      </div>
      <div class="filter-group">
        <div class="view-toggle">
          <button :class="['toggle-btn', { active: viewMode === 'table' }]" @click="viewMode = 'table'">
            <unordered-list-outlined />
          </button>
          <button :class="['toggle-btn', { active: viewMode === 'grid' }]" @click="viewMode = 'grid'">
            <appstore-outlined />
          </button>
        </div>
        <a-button @click="resetFilter">重置</a-button>
        <a-button type="link" @click="loadCaseList"><reload-outlined /> 刷新</a-button>
      </div>
    </div>

    <!-- 表格视图 -->
    <div v-if="viewMode === 'table'" class="table-panel glass-panel">
      <a-table
        :dataSource="caseList"
        :columns="columns"
        :loading="loading"
        rowKey="id"
        :pagination="pagination"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">
            <span class="index-badge">{{ (pagination.current - 1) * pagination.pageSize + index + 1 }}</span>
          </template>
          <template v-if="column.key === 'title'">
            <div class="case-title-table">
              <span>{{ record.title }}</span>
              <a-tag v-if="isPlatformCase(record)" color="geekblue">平台共享</a-tag>
            </div>
          </template>
          <template v-if="column.key === 'category'">
            <a-tag :color="categoryColor(record.category)">{{ categoryLabel(record.category) }}</a-tag>
          </template>
          <template v-if="column.key === 'difficulty'">
            <a-tag :color="difficultyColor(record.difficulty)">{{ difficultyLabel(record.difficulty) }}</a-tag>
          </template>
          <template v-if="column.key === 'courseName'">
            <span v-if="record.courseName" class="course-tag">{{ record.courseName }}</span>
            <span v-else class="text-muted">-</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" type="link" @click="previewCase(record)">
                <eye-outlined /> 预览
              </a-button>
              <a-button v-if="!isPlatformCase(record)" size="small" type="link" danger @click="deleteCase(record.id)">
                <delete-outlined /> 删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 卡片网格视图 -->
    <div v-else class="card-grid">
      <div v-for="item in caseList" :key="item.id" class="case-card glass-panel">
        <div class="card-top">
          <a-tag :color="categoryColor(item.category)" class="card-cat-tag">{{ categoryLabel(item.category) }}</a-tag>
          <a-tag :color="difficultyColor(item.difficulty)" class="card-diff-tag">{{ difficultyLabel(item.difficulty) }}</a-tag>
        </div>
        <h3 class="card-title">{{ item.title }}</h3>
        <a-tag v-if="isPlatformCase(item)" color="geekblue" class="scope-tag">平台共享</a-tag>
        <div class="card-meta">
          <span v-if="item.courseName" class="card-course"><book-outlined /> {{ item.courseName }}</span>
          <span v-else class="card-course text-muted"><book-outlined /> 未指定课程</span>
        </div>
        <div class="card-time">{{ item.createTime }}</div>
        <div class="card-actions">
          <a-button size="small" type="link" @click="previewCase(item)">
            <eye-outlined /> 预览
          </a-button>
          <a-button v-if="!isPlatformCase(item)" size="small" type="link" danger @click="deleteCase(item.id)">
            <delete-outlined /> 删除
          </a-button>
        </div>
      </div>
      <div v-if="caseList.length === 0 && !loading" class="empty-grid">
        <a-empty description="暂无案例数据" />
      </div>
    </div>

    <!-- 导入弹窗 (保持原有逻辑) -->
    <a-modal
      v-model:open="importVisible"
      title="导入案例"
      width="560px"
      centered
      :confirmLoading="importLoading"
      @ok="handleImport"
      @cancel="closeImportModal"
      okText="确认导入"
      cancelText="取消"
    >
      <a-form layout="vertical" :model="importForm" class="import-form">
        <a-form-item label="案例标题" required>
          <a-input v-model:value="importForm.title" placeholder="请输入案例标题" />
        </a-form-item>

        <a-form-item label="案例文件" required>
          <a-upload
            :file-list="pdfFileList"
            :max-count="1"
            accept=".pdf,.doc,.docx"
            :custom-request="handlePdfUpload"
            @remove="handlePdfRemove"
          >
            <a-button :loading="pdfUploading">
              <upload-outlined />
              {{ pdfUploading ? '上传中...' : '选择 PDF / Word 文件' }}
            </a-button>
          </a-upload>
          <div class="upload-hint">支持 .pdf、.doc、.docx，Word 文件将以正文方式预览。</div>
          <div v-if="importForm.pdfUrl" class="upload-hint">已上传：{{ pdfFileName }}</div>
        </a-form-item>

        <a-form-item label="案例分类" required>
          <a-select v-model:value="importForm.category" placeholder="请选择案例分类">
            <a-select-option value="course_design">课程设计</a-select-option>
            <a-select-option value="enterprise">企业实际工程</a-select-option>
            <a-select-option value="competition">大赛资源</a-select-option>
            <a-select-option value="small_project">小项目</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="难度等级" required>
          <a-select v-model:value="importForm.difficulty" placeholder="请选择难度等级">
            <a-select-option value="easy">初级</a-select-option>
            <a-select-option value="medium">中等</a-select-option>
            <a-select-option value="hard">困难</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="适用课程">
          <a-input v-model:value="importForm.courseName" placeholder="请输入适用课程名称" />
        </a-form-item>
      </a-form>
    </a-modal>

    <TeachingCasePreviewModal ref="casePreviewModal" />

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import TeachingCasePreviewModal from '@/components/teacher/TeachingCasePreviewModal.vue'
import {
  UploadOutlined,
  ReloadOutlined,
  SnippetsOutlined,
  EyeOutlined,
  DeleteOutlined,
  SearchOutlined,
  UnorderedListOutlined,
  AppstoreOutlined,
  BookOutlined,
} from '@ant-design/icons-vue'
import {
  getTeachingCaseList,
  importTeachingCase,
  deleteTeachingCase,
  type TeachingCaseItem,
} from '@/api/case'
import { uploadAdminFile } from '@/api/admin'

const loading = ref(false)
const caseList = ref<TeachingCaseItem[]>([])
const viewMode = ref<'table' | 'grid'>('table')
const casePreviewModal = ref<InstanceType<typeof TeachingCasePreviewModal> | null>(null)

const isPlatformCase = (item: TeachingCaseItem) => item.scope === 'platform'

const filter = reactive({
  keyword: '',
  category: undefined as string | undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 8,
  showSizeChanger: true,
  pageSizeOptions: ['8', '16', '24', '40'],
  onChange: (page: number, pageSize: number) => {
    pagination.current = page
    pagination.pageSize = pageSize
  },
})

const columns = [
  { title: '序号', key: 'index', width: 70, align: 'center' as const },
  { title: '案例标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '案例分类', key: 'category', width: 140, align: 'center' as const },
  { title: '适用课程', dataIndex: 'courseName', key: 'courseName', width: 180, align: 'center' as const },
  { title: '难度等级', key: 'difficulty', width: 120, align: 'center' as const },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180, align: 'center' as const },
  { title: '操作', key: 'action', width: 180, align: 'center' as const },
]

const categoryLabel = (category: string) => {
  const map: Record<string, string> = {
    course_design: '课程设计',
    enterprise: '企业实际工程',
    competition: '大赛资源',
    small_project: '小项目',
  }
  return map[category] || category
}

const categoryColor = (category: string) => {
  const map: Record<string, string> = {
    course_design: 'blue',
    enterprise: 'purple',
    competition: 'orange',
    small_project: 'cyan',
  }
  return map[category] || 'default'
}

const difficultyLabel = (difficulty: string) => {
  const map: Record<string, string> = {
    easy: '初级',
    medium: '中等',
    hard: '困难',
  }
  return map[difficulty] || difficulty
}

const difficultyColor = (difficulty: string) => {
  const map: Record<string, string> = {
    easy: 'green',
    medium: 'orange',
    hard: 'red',
  }
  return map[difficulty] || 'default'
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return '-'
  try {
    const date = new Date(timeStr)
    if (isNaN(date.getTime())) {
      return String(timeStr).replace('T', ' ').substring(0, 16)
    }
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    const h = String(date.getHours()).padStart(2, '0')
    const min = String(date.getMinutes()).padStart(2, '0')
    return `${y}-${m}-${d} ${h}:${min}`
  } catch {
    return String(timeStr).replace('T', ' ').substring(0, 16)
  }
}

const loadCaseList = async () => {
  loading.value = true
  try {
    const data = await getTeachingCaseList({
      keyword: filter.keyword || undefined,
      category: filter.category || undefined,
    })
    caseList.value = (data || []).map((item: any) => ({
      ...item,
      createTime: formatTime(item.createTime),
    }))
  } catch (error: any) {
    console.error('获取案例列表失败:', error)
    message.error(error?.message || '获取案例列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filter.keyword = ''
  filter.category = undefined
  loadCaseList()
}

// ==================== 导入功能 ====================
const importVisible = ref(false)
const importLoading = ref(false)
const pdfUploading = ref(false)
const pdfFileList = ref<any[]>([])
const pdfFileName = ref('')

const importForm = reactive({
  title: '',
  category: undefined as string | undefined,
  difficulty: undefined as string | undefined,
  courseName: '',
  pdfUrl: '',
})

const openImportModal = () => {
  importForm.title = ''
  importForm.category = undefined
  importForm.difficulty = undefined
  importForm.courseName = ''
  importForm.pdfUrl = ''
  pdfFileList.value = []
  pdfFileName.value = ''
  importVisible.value = true
}

const closeImportModal = () => {
  importVisible.value = false
}

const handlePdfUpload = async ({ file, onSuccess, onError }: any) => {
  const fileName = String(file?.name || '')
  if (!isSupportedCaseFile(fileName)) {
    const error = new Error('仅支持 PDF、Word(.doc/.docx) 文件')
    message.warning(error.message)
    onError?.(error)
    return
  }

  pdfUploading.value = true
  try {
    const url = await uploadAdminFile(file, 'case')
    importForm.pdfUrl = url
    pdfFileName.value = file.name
    pdfFileList.value = [{ uid: file.uid, name: file.name, status: 'done', url }]
    onSuccess?.(url)
    message.success('案例文件上传成功')
  } catch (error: any) {
    console.error('案例文件上传失败:', error)
    message.error(error?.message || '案例文件上传失败')
    onError?.(error)
  } finally {
    pdfUploading.value = false
  }
}

const handlePdfRemove = () => {
  importForm.pdfUrl = ''
  pdfFileName.value = ''
  pdfFileList.value = []
}

const handleImport = async () => {
  if (!importForm.title.trim()) {
    return message.warning('请输入案例标题')
  }
  if (!importForm.pdfUrl) {
    return message.warning('请上传 PDF 或 Word 文件')
  }
  if (!importForm.category) {
    return message.warning('请选择案例分类')
  }
  if (!importForm.difficulty) {
    return message.warning('请选择难度等级')
  }

  importLoading.value = true
  try {
    await importTeachingCase({
      title: importForm.title.trim(),
      category: importForm.category,
      difficulty: importForm.difficulty,
      courseName: importForm.courseName.trim(),
      pdfUrl: importForm.pdfUrl,
    })
    importVisible.value = false
    loadCaseList()
  } catch (error: any) {
    console.error('导入案例失败:', error)
    message.error(error?.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

// ==================== 预览功能 ====================
const previewCase = (record: TeachingCaseItem) => {
  if (!record.id) {
    return message.warning('该案例没有文件')
  }
  casePreviewModal.value?.open(record.id)
}

const isSupportedCaseFile = (fileName: string) => {
  return /\.(pdf|doc|docx)$/i.test(fileName)
}

// ==================== 删除功能 ====================
const deleteCase = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复，是否确认删除该案例？',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteTeachingCase(id)
        loadCaseList()
      } catch (error: any) {
        console.error('删除案例失败:', error)
        message.error(error?.message || '删除失败')
      }
    },
  })
}

onMounted(() => {
  loadCaseList()
})
</script>

<style scoped>
/* ===== 基础容器 ===== */
.modern-page {
  font-family: 'Plus Jakarta Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  padding: 20px 28px 24px;
  height: 100%;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  gap: 16px;
}

/* ===== 页面标题 ===== */
.page-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.title-group h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  color: #0ea5e9;
  font-size: 30px;
}

.title-group .subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 15px;
}

.add-btn {
  background: #0ea5e9 !important;
  border-color: #0ea5e9 !important;
  font-weight: 600;
  border-radius: 8px !important;
  height: 40px;
  padding: 0 18px;
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.25);
}
.add-btn:hover {
  background: #0284c7 !important;
  border-color: #0284c7 !important;
}

/* ===== 筛选 + 视图切换 ===== */
.filter-dashboard {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 10px;
}

.filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-item .label {
  font-size: 15px;
  font-weight: 700;
  color: #334155;
}

.filter-dashboard :deep(.ant-input),
.filter-dashboard :deep(.ant-select-selection-item),
.filter-dashboard :deep(.ant-select-selection-placeholder),
.filter-dashboard :deep(.ant-btn) {
  font-size: 15px;
}

.filter-dashboard :deep(.ant-input),
.filter-dashboard :deep(.ant-select-selection-item) {
  color: #1e293b;
  font-weight: 500;
}

.filter-dashboard :deep(.ant-input::placeholder),
.filter-dashboard :deep(.ant-select-selection-placeholder) {
  color: #64748b !important;
  font-weight: 500;
}

.filter-dashboard :deep(.ant-select-selector),
.filter-dashboard :deep(.ant-btn) {
  min-height: 40px;
}

.filter-dashboard :deep(.ant-select-selector) {
  height: 40px !important;
  align-items: center;
}

.filter-dashboard :deep(.ant-select-selection-search-input) {
  height: 38px !important;
}

.filter-dashboard :deep(.ant-select-selection-item),
.filter-dashboard :deep(.ant-select-selection-placeholder) {
  line-height: 38px !important;
}

.filter-dashboard :deep(.case-search-input.ant-input-affix-wrapper) {
  height: 40px;
  min-height: 40px;
  padding: 0 12px 0 16px;
  align-items: center;
  border-radius: 8px !important;
}

.filter-dashboard :deep(.case-search-input .ant-input) {
  height: 38px;
  line-height: 38px;
  padding: 0;
}

.filter-dashboard :deep(.case-search-input .ant-input-clear-icon) {
  display: inline-flex;
  align-items: center;
}

.case-search-icon {
  color: #64748b;
  cursor: pointer;
  font-size: 18px;
  transition: color 0.15s;
}

.case-search-icon:hover {
  color: #147ed9;
}

.filter-dashboard :deep(.ant-btn) {
  color: #334155;
  font-weight: 600;
}

.filter-dashboard :deep(.ant-btn-link) {
  color: #147ed9;
}

/* 视图切换 */
.view-toggle {
  display: flex;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 3px;
}

.toggle-btn {
  width: 34px;
  height: 30px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  font-size: 16px;
  transition: all 0.2s;
}
.toggle-btn.active {
  background: #fff;
  color: #0ea5e9;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.toggle-btn:hover:not(.active) {
  color: #64748b;
}

/* ===== 表格面板 ===== */
.table-panel {
  flex: 1;
  min-height: 0;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
}

.index-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.course-tag {
  display: inline-block;
  padding: 4px 11px;
  background: #f0f9ff;
  color: #0369a1;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
}

.case-title-table {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.case-title-table span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
  line-height: 1.55;
}

.scope-tag {
  align-self: flex-start;
  width: fit-content;
}

.text-muted { color: #cbd5e1; }

/* ===== 卡片网格视图 ===== */
.card-grid {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
  align-content: start;
  padding-right: 4px;
}

.case-card {
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 10px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: box-shadow 0.2s, transform 0.2s;
}
.case-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
  transform: translateY(-2px);
}

.card-top {
  display: flex;
  gap: 8px;
}

.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-course {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-time {
  font-size: 12px;
  color: #94a3b8;
}

.card-actions {
  display: flex;
  gap: 4px;
  padding-top: 6px;
  border-top: 1px solid #f1f5f9;
}

.empty-grid {
  grid-column: 1 / -1;
  padding: 60px 0;
}

/* ===== 表格深度美化 ===== */
:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-btn) {
  border-radius: 8px !important;
}

:deep(.ant-table),
:deep(.ant-table-container) {
  background: transparent !important;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fafc !important;
  color: #475569;
  font-weight: 700;
  font-size: 14px;
  line-height: 1.45;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid #e8eef6 !important;
  padding: 15px 16px !important;
}
:deep(.ant-table-thead > tr > th::before) {
  display: none !important;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 16px !important;
  border-bottom: 1px solid #f1f5f9 !important;
  color: #334155;
  font-size: 15px;
  line-height: 1.55;
  vertical-align: middle !important;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: #fafbfc !important;
}

:deep(.ant-tag) {
  border-radius: 6px !important;
  border: none !important;
  padding: 4px 11px;
  font-weight: 600;
  font-size: 13px;
  line-height: 1.45;
}

:deep(.ant-table-tbody .ant-btn-link) {
  padding: 5px 9px;
  height: auto;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
  line-height: 1.45;
  transition: all 0.15s;
}
:deep(.ant-table-tbody .ant-btn-link:hover) {
  background-color: #eff6ff;
}
:deep(.ant-table-tbody .ant-btn-link.ant-btn-dangerous) {
  color: #ef4444;
}
:deep(.ant-table-tbody .ant-btn-link.ant-btn-dangerous:hover) {
  background-color: #fef2f2;
}

/* 表格容器 flex 布局 */
:deep(.ant-table-wrapper),
:deep(.ant-spin-nested-loading),
:deep(.ant-spin-container) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
:deep(.ant-table) {
  flex: 1;
}
:deep(.ant-table-pagination) {
  margin-top: auto !important;
  padding-top: 14px;
}

/* ===== 导入弹窗 ===== */
.import-form {
  margin-top: 4px;
}
.upload-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

/* ===== 滚动条 ===== */
.card-grid::-webkit-scrollbar {
  width: 5px;
}
.card-grid::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .modern-page { padding: 14px 16px; }
  .page-header { align-items: flex-start; gap: 12px; flex-direction: column; }
  .header-actions { width: 100%; }
  .header-actions :deep(.ant-btn) { flex: 1; }
  .card-grid { grid-template-columns: 1fr; }
  .filter-dashboard { flex-direction: column; gap: 12px; align-items: stretch; }
}
</style>
