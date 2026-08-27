<template>
  <a-modal
    :open="open"
    title="选择参考案例"
    width="1080px"
    centered
    class="teaching-case-picker-modal"
    :footer="null"
    @cancel="handleCancel"
  >
    <div class="case-picker-shell">
      <section class="case-picker-list">
        <div class="case-filter-bar">
          <a-input-search
            v-model:value="filter.keyword"
            placeholder="搜索案例标题或适用课程"
            allow-clear
            size="large"
            @search="handleSearch"
            @change="handleKeywordChange"
          >
            <template #prefix>
              <search-outlined />
            </template>
          </a-input-search>
          <div class="filter-row">
            <a-select
              v-model:value="filter.category"
              placeholder="全部分类"
              allow-clear
              @change="handleFilterChange"
            >
              <a-select-option value="course_design">课程设计</a-select-option>
              <a-select-option value="enterprise">企业实际工程</a-select-option>
              <a-select-option value="competition">大赛资源</a-select-option>
              <a-select-option value="small_project">小项目</a-select-option>
            </a-select>
            <a-select
              v-model:value="filter.difficulty"
              placeholder="全部难度"
              allow-clear
              @change="handleFilterChange"
            >
              <a-select-option value="easy">初级</a-select-option>
              <a-select-option value="medium">中等</a-select-option>
              <a-select-option value="hard">困难</a-select-option>
            </a-select>
          </div>
        </div>

        <a-spin :spinning="loading">
          <div v-if="caseList.length === 0" class="case-empty">
            <file-search-outlined />
            <span>没有找到匹配的案例</span>
          </div>
          <div v-else class="case-result-list">
            <button
              v-for="item in caseList"
              :key="item.id"
              type="button"
              class="case-result-card"
              :class="{ active: currentCase?.id === item.id }"
              @click="previewCase(item)"
              @dblclick="confirmCase(item)"
            >
              <div class="case-card-main">
                <div class="case-title-line">
                  <file-pdf-outlined />
                  <span>{{ item.title }}</span>
                </div>
                <div class="case-card-meta">
                  <span>{{ item.courseName || '未指定适用课程' }}</span>
                  <span>{{ formatTime(item.createTime) }}</span>
                </div>
              </div>
              <div class="case-tags">
                <a-tag v-if="recommendMatchLabel(item)" :color="recommendMatchColor(item)">
                  {{ recommendMatchLabel(item) }}
                </a-tag>
                <a-tag :color="categoryColor(item.category)">{{ categoryLabel(item.category) }}</a-tag>
                <a-tag :color="difficultyColor(item.difficulty)">{{ difficultyLabel(item.difficulty) }}</a-tag>
              </div>
            </button>
          </div>
        </a-spin>

        <a-pagination
          v-if="pagination.total > pagination.pageSize"
          size="small"
          :current="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          :show-size-changer="false"
          @change="handlePageChange"
        />
      </section>

      <section class="case-preview-panel">
        <template v-if="currentCase">
          <div class="preview-header">
            <div>
              <h3>{{ currentCase.title }}</h3>
              <p>{{ currentCase.courseName || '未指定适用课程' }}</p>
            </div>
            <div class="preview-tags">
              <a-tag :color="categoryColor(currentCase.category)">{{ categoryLabel(currentCase.category) }}</a-tag>
              <a-tag :color="difficultyColor(currentCase.difficulty)">{{ difficultyLabel(currentCase.difficulty) }}</a-tag>
            </div>
          </div>

          <a-spin :spinning="previewLoading">
          <div v-if="isPdfPreview" class="pdf-frame-wrap">
            <iframe :key="currentCase.id" :src="previewUrl" title="案例文件预览" />
          </div>

          <div v-else class="word-preview-wrap">
            <div class="word-preview-head">
              <strong>文档正文预览</strong>
              <a-tag color="geekblue">{{ fileTypeLabel }}</a-tag>
            </div>
            <div v-if="previewHtml" class="word-preview-html" v-html="previewHtml"></div>
            <pre v-else-if="normalizedPreviewText" class="word-preview-text">{{ normalizedPreviewText }}</pre>
            <a-empty v-else description="暂未提取到可预览正文" />
          </div>

          </a-spin>

        </template>
        <div v-else class="preview-empty">
          <file-pdf-outlined />
          <span>选择左侧案例后在这里预览案例文件</span>
        </div>
      </section>
    </div>

    <div class="case-picker-footer">
      <span class="selected-copy">
        {{ currentCase ? `将选择：${currentCase.title}` : '请选择一个案例作为教案参考' }}
      </span>
      <div class="footer-actions">
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" :disabled="!currentCase" @click="confirmCurrentCase">
          <check-outlined />
          确认选择
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckOutlined,
  FilePdfOutlined,
  FileSearchOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import {
  getTeachingCaseDetail,
  getTeachingCasePage,
  getTeachingCasePreviewDetail,
  getTeachingCasePreviewUrl,
  recommendTeachingCases,
  type RecommendTeachingCasePayload,
  type RecommendedTeachingCaseItem,
  type TeachingCasePreviewDetail,
  type TeachingCaseItem,
} from '@/api/case'

const props = defineProps<{
  open: boolean
  selectedId?: number
  recommendContext?: RecommendTeachingCasePayload
}>()

const emit = defineEmits<{
  (event: 'update:open', value: boolean): void
  (event: 'select', item: TeachingCaseItem): void
}>()

const loading = ref(false)
const previewLoading = ref(false)
const caseList = ref<TeachingCaseItem[]>([])
const currentCase = ref<TeachingCaseItem | null>(null)
const previewDetail = ref<TeachingCasePreviewDetail | null>(null)

const filter = reactive({
  keyword: '',
  category: undefined as string | undefined,
  difficulty: undefined as string | undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 8,
  total: 0,
})

const hasRecommendContext = computed(() => {
  return Boolean(props.recommendContext?.subject?.trim() && props.recommendContext?.topic?.trim())
})

const hasActiveFilter = computed(() => {
  return Boolean(filter.keyword.trim() || filter.category || filter.difficulty)
})

const shouldUseRecommendCases = computed(() => {
  return hasRecommendContext.value && !hasActiveFilter.value && pagination.current === 1
})

const previewUrl = computed(() => {
  return currentCase.value ? getTeachingCasePreviewUrl(currentCase.value.id) : ''
})

const previewFileUrl = computed(() => previewDetail.value?.pdfUrl || currentCase.value?.pdfUrl || '')
const lowerPreviewFileUrl = computed(() => previewFileUrl.value.toLowerCase())
const isPdfPreview = computed(() => lowerPreviewFileUrl.value.includes('.pdf'))
const fileTypeLabel = computed(() => {
  if (lowerPreviewFileUrl.value.includes('.docx')) return 'Word DOCX'
  if (lowerPreviewFileUrl.value.includes('.doc')) return 'Word DOC'
  if (isPdfPreview.value) return 'PDF'
  return '文档'
})
const normalizedPreviewText = computed(() => {
  const text = previewDetail.value?.previewText || previewDetail.value?.summary || currentCase.value?.summary || ''
  return text.replace(/\r\n/g, '\n').replace(/\n{3,}/g, '\n\n').trim()
})
const previewHtml = computed(() => previewDetail.value?.previewHtml || '')

const categoryLabel = (category: string) => {
  const map: Record<string, string> = {
    course_design: '课程设计',
    enterprise: '企业实际工程',
    competition: '大赛资源',
    small_project: '小项目',
  }
  return map[category] || category || '未分类'
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
  return map[difficulty] || difficulty || '未设置'
}

const difficultyColor = (difficulty: string) => {
  const map: Record<string, string> = {
    easy: 'green',
    medium: 'orange',
    hard: 'red',
  }
  return map[difficulty] || 'default'
}

const formatTime = (timeStr?: string) => {
  if (!timeStr) return '未知时间'
  const date = new Date(timeStr)
  if (Number.isNaN(date.getTime())) {
    return String(timeStr).replace('T', ' ').substring(0, 16)
  }
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const recommendMatchLabel = (item: TeachingCaseItem) => {
  const level = (item as TeachingCaseItem & { matchLevel?: RecommendedTeachingCaseItem['matchLevel'] }).matchLevel
  const map: Record<string, string> = {
    precise: '精准匹配',
    evidence: '强关联',
  }
  return level ? map[level] || '' : ''
}

const recommendMatchColor = (item: TeachingCaseItem) => {
  const level = (item as TeachingCaseItem & { matchLevel?: RecommendedTeachingCaseItem['matchLevel'] }).matchLevel
  const map: Record<string, string> = {
    precise: 'green',
    evidence: 'blue',
  }
  return level ? map[level] || 'default' : 'default'
}

const sortRecommendedCases = (items: RecommendedTeachingCaseItem[]) => {
  const rank: Record<string, number> = {
    precise: 0,
    evidence: 1,
    related: 2,
    fallback: 3,
  }
  return [...items].sort((a, b) => {
    const rankA = rank[a.matchLevel || ''] ?? 4
    const rankB = rank[b.matchLevel || ''] ?? 4
    if (rankA !== rankB) return rankA - rankB
    return Number(b.matchScore || 0) - Number(a.matchScore || 0)
  })
}

const syncCurrentCaseFromList = () => {
  const selectedInPage = caseList.value.find((item) => item.id === props.selectedId)
  const currentInPage = caseList.value.find((item) => item.id === currentCase.value?.id)
  if (selectedInPage) {
    currentCase.value = selectedInPage
  } else if (currentInPage) {
    currentCase.value = currentInPage
  } else if (!currentCase.value && caseList.value.length > 0) {
    currentCase.value = caseList.value[0]
  } else if (caseList.value.length === 0 && !props.selectedId) {
    currentCase.value = null
  }
}

const loadCases = async () => {
  loading.value = true
  try {
    if (shouldUseRecommendCases.value && props.recommendContext) {
      const recommended = await recommendTeachingCases(props.recommendContext)
      const sorted = sortRecommendedCases(recommended || [])
      if (sorted.length > 0) {
        caseList.value = sorted as unknown as TeachingCaseItem[]
        pagination.total = Math.min(sorted.length, pagination.pageSize)
        syncCurrentCaseFromList()
        return
      }
    }

    const data = await getTeachingCasePage({
      keyword: filter.keyword.trim() || undefined,
      category: filter.category,
      difficulty: filter.difficulty,
      current: pagination.current,
      pageSize: pagination.pageSize,
    })
    caseList.value = data?.records || []
    pagination.total = Number(data?.total || 0)
    syncCurrentCaseFromList()
  } catch (error: any) {
    console.error('加载案例分页失败:', error)
    message.error(error?.message || '加载案例失败')
  } finally {
    loading.value = false
  }
}

const hydrateSelectedCase = async () => {
  if (!props.selectedId) return
  const itemInPage = caseList.value.find((item) => item.id === props.selectedId)
  if (itemInPage) {
    currentCase.value = itemInPage
    return
  }
  try {
    currentCase.value = await getTeachingCaseDetail(props.selectedId)
  } catch (error) {
    console.error('加载已选案例详情失败:', error)
  }
}

const resetFilters = () => {
  filter.keyword = ''
  filter.category = undefined
  filter.difficulty = undefined
}

const openPicker = async () => {
  pagination.current = 1
  resetFilters()
  currentCase.value = null
  await loadCases()
  await hydrateSelectedCase()
}

const handleSearch = () => {
  pagination.current = 1
  currentCase.value = null
  loadCases()
}

const handleKeywordChange = () => {
  if (!filter.keyword) {
    handleSearch()
  }
}

const handleFilterChange = () => {
  handleSearch()
}

const handlePageChange = (page: number) => {
  pagination.current = page
  currentCase.value = null
  loadCases()
}

const previewCase = (item: TeachingCaseItem) => {
  currentCase.value = item
}

const loadPreviewDetail = async (id?: number) => {
  previewDetail.value = null
  if (!id) {
    previewLoading.value = false
    return
  }
  previewLoading.value = true
  try {
    const data = await getTeachingCasePreviewDetail(id)
    if (currentCase.value?.id === id) {
      previewDetail.value = data
    }
  } catch (error: any) {
    console.error('加载案例预览失败:', error)
    message.error(error?.message || '加载案例预览失败')
  } finally {
    if (currentCase.value?.id === id) {
      previewLoading.value = false
    }
  }
}

const confirmCase = (item: TeachingCaseItem) => {
  currentCase.value = item
  confirmCurrentCase()
}

const confirmCurrentCase = () => {
  if (!currentCase.value) {
    message.warning('请先选择一个案例')
    return
  }
  emit('select', currentCase.value)
  emit('update:open', false)
}

const handleCancel = () => {
  emit('update:open', false)
}

watch(
  () => props.open,
  (visible) => {
    if (visible) {
      openPicker()
    }
  },
)

watch(
  () => props.selectedId,
  () => {
    if (props.open) {
      hydrateSelectedCase()
    }
  },
)

watch(
  () => currentCase.value?.id,
  (id) => {
    loadPreviewDetail(id)
  },
)
</script>

<style scoped>
.case-picker-shell {
  height: clamp(420px, calc(100vh - 220px), 560px);
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(340px, 390px) minmax(0, 1fr);
  gap: 18px;
  overflow: hidden;
}

.case-picker-list,
.case-preview-panel {
  min-height: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.case-picker-list {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.case-filter-bar {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.filter-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.case-result-list {
  flex: 1;
  min-height: 0;
  max-height: none;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 2px;
}

.case-result-card {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.case-picker-list :deep(.ant-spin-nested-loading) {
  flex: 1;
  min-height: 0;
}

.case-picker-list :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.case-result-card:hover,
.case-result-card.active {
  border-color: #3b82f6;
  background: #f8fbff;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.1);
}

.case-card-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.case-title-line {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
}

.case-title-line span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.case-card-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: #64748b;
  font-size: 12px;
}

.case-card-meta span:first-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.case-tags {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.case-empty,
.preview-empty {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #94a3b8;
}

.case-empty :deep(.anticon),
.preview-empty :deep(.anticon) {
  font-size: 34px;
}

.case-preview-panel {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.preview-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.preview-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
}

.preview-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.preview-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.case-preview-panel :deep(.ant-spin-nested-loading) {
  flex: 1;
  min-height: 0;
}

.case-preview-panel :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.pdf-frame-wrap {
  flex: 1;
  min-height: 0;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  overflow: hidden;
  background: #f8fafc;
}

.pdf-frame-wrap iframe {
  width: 100%;
  height: 100%;
  min-height: 0;
  border: 0;
  display: block;
}

.word-preview-wrap {
  flex: 1;
  min-height: 0;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

.word-preview-head {
  min-height: 46px;
  padding: 10px 14px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #0f172a;
}

.word-preview-text {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 16px 18px 22px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font: 14px/1.8 "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  color: #1e293b;
}

.word-preview-html {
  flex: 1;
  min-height: 0;
  padding: 16px 18px 22px;
  overflow: auto;
  font: 14px/1.8 "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  color: #1e293b;
}

.word-preview-html :deep(p) {
  margin: 0 0 12px;
}

.word-preview-html :deep(h3) {
  margin: 16px 0 10px;
  color: #0f172a;
  font-size: 17px;
}

.word-preview-html :deep(.case-preview-table) {
  width: 100%;
  margin: 12px 0;
  border-collapse: collapse;
  table-layout: fixed;
}

.word-preview-html :deep(.case-preview-table td) {
  padding: 8px 10px;
  border: 1px solid #cbd5e1;
  vertical-align: top;
  word-break: break-word;
}

.word-preview-html :deep(.case-preview-figure) {
  margin: 14px 0;
  text-align: center;
}

.word-preview-html :deep(.case-preview-figure img) {
  max-width: 100%;
  max-height: 360px;
  border-radius: 8px;
  object-fit: contain;
}

.word-preview-html :deep(.case-preview-figure figcaption) {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.preview-fallback {
  min-height: 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.case-picker-footer {
  flex-shrink: 0;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}

.selected-copy {
  min-width: 0;
  color: #475569;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer-actions {
  flex-shrink: 0;
  display: flex;
  gap: 10px;
}

:global(.teaching-case-picker-modal) {
  max-width: calc(100vw - 48px);
}

:global(.teaching-case-picker-modal .ant-modal-content) {
  max-height: calc(100vh - 64px);
  overflow: hidden;
}

:global(.teaching-case-picker-modal .ant-modal-body) {
  display: flex;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

@media (max-width: 900px) {
  .case-picker-shell {
    grid-template-columns: 1fr;
    height: min(640px, calc(100vh - 220px));
    min-height: 0;
  }

  .case-result-list {
    max-height: 300px;
  }

  .pdf-frame-wrap,
  .pdf-frame-wrap iframe {
    min-height: 360px;
  }

  .case-picker-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .footer-actions {
    justify-content: flex-end;
  }
}
</style>
