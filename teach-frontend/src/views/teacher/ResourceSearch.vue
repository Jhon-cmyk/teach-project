<template>
  <div class="resource-search-page">
    <div class="page-top">
      <div class="title-group">
        <h2 class="page-title">
          <GlobalOutlined class="title-icon" />
          资源检索中心
        </h2>
        <p class="page-subtitle">
          统一检索平台内课程、教师发布教案、练习题与互动课件；若内部资源不足，可一键跳转外部平台继续搜索。
        </p>
      </div>

      <div class="toolbar-card">
        <div class="toolbar-main">
          <a-select
            v-model:value="sourceMode"
            class="source-select"
            :options="sourceOptions"
          />

          <div class="search-input-wrap">
            <SearchOutlined class="search-icon" />
            <input
              v-model="searchText"
              type="text"
              class="search-input"
              placeholder="输入关键词检索资源，如：TCP 三次握手 / Java 基础 / 冒泡排序"
              @keydown.enter="handleSearch"
            />
          </div>

          <a-button type="primary" class="action-btn search-btn" @click="handleSearch">
            搜索
          </a-button>

          <a-button class="action-btn" @click="handleReset">
            重置
          </a-button>
        </div>

      </div>
    </div>

    <div class="workbench">
      <aside class="left-panel panel-card">
        <div class="panel-title">资源分类</div>

        <div class="type-list">
          <div
            v-for="item in typeStats"
            :key="item.value"
            class="type-item"
            :class="{ active: activeType === item.value }"
            @click="handleTypeChange(item.value)"
          >
            <div class="type-item-left">
              <component :is="item.icon" class="type-item-icon" />
              <span>{{ item.label }}</span>
            </div>
            <span class="type-item-count">{{ item.count }}</span>
          </div>
        </div>

        <div class="side-divider"></div>

        <div class="panel-title">检索说明</div>
        <div class="tips-box">
          <div class="tip-row">
            <CheckCircleOutlined class="tip-icon success" />
            <span>左侧筛选资源类型</span>
          </div>
          <div class="tip-row">
            <CheckCircleOutlined class="tip-icon success" />
            <span>中间点击结果切换摘要</span>
          </div>
          <div class="tip-row">
            <CheckCircleOutlined class="tip-icon success" />
            <span>右侧查看摘要与操作入口</span>
          </div>
          <div class="tip-row">
            <RocketOutlined class="tip-icon primary" />
            <span>切换可直接跳转外部平台</span>
          </div>
        </div>
      </aside>

      <section class="center-panel panel-card">
        <div class="result-header">
          <div class="result-title-wrap">
            <h3 class="result-title">检索结果</h3>
            <p class="result-subtitle">
              <template v-if="sourceMode === 'internal'">
                共找到 <strong>{{ total }}</strong> 条内部资源
              </template>
              <template v-else>
                当前来源为 <strong>{{ sourceLabelMap[sourceMode] }}</strong>，点击搜索将直接跳转
              </template>
            </p>
          </div>

          <div class="result-badges">
            <span class="badge-chip">
              {{ activeTypeLabel }}
            </span>
            <span class="badge-chip light">
              {{ sourceLabelMap[sourceMode] }}
            </span>
          </div>
        </div>



        <div v-if="sourceMode !== 'internal'" class="external-mode-box">
          <div class="external-mode-icon-wrap">
            <RocketOutlined class="external-mode-icon" />
          </div>
          <div class="external-mode-content">
            <h4>当前为外部平台模式</h4>
            <p>
              你已选择 <strong>{{ sourceLabelMap[sourceMode] }}</strong>。
              输入关键词后点击上方“搜索”，将直接打开对应平台的搜索结果页。
            </p>
            <div class="external-actions">
              <a-button type="primary" @click="handleSearch">立即前往搜索</a-button>
              <a-button @click="switchToInternal">返回内部资源</a-button>
            </div>
          </div>
        </div>

        <template v-else>
          <a-spin :spinning="loading">
            <div v-if="internalResources.length > 0" class="result-list">
              <div
                v-for="item in internalResources"
                :key="`${item.type}-${item.id}`"
                class="result-item"
                :class="{ active: selectedResourceKey === `${item.type}-${item.id}` }"
                @click="selectResource(item)"
              >
                <div class="result-item-cover">
                  <img :src="item.cover" :alt="item.title" />
                </div>

                <div class="result-item-main">
                  <div class="result-item-top">
                    <h4 class="item-title" v-html="highlightText(item.title, searchText)"></h4>
                    <span class="item-date">{{ item.date }}</span>
                  </div>

                  <p class="item-desc">
                    {{ item.desc }}
                  </p>

                  <div class="item-tags">
                    <span
                      v-for="tag in item.tags"
                      :key="tag"
                      class="item-tag"
                    >
                      {{ tag }}
                    </span>
                  </div>

                  <div class="item-meta">
                    <span class="meta-item">
                      <UserOutlined />
                      {{ item.author }}
                    </span>
                    <span class="meta-item" v-if="item.course">
                      <BookOutlined />
                      {{ item.course }}
                    </span>
                    <span class="meta-item">
                      <EyeOutlined />
                      {{ formatViews(item.views) }}
                    </span>
                    <span class="meta-item" v-if="item.duration">
                      <PlayCircleOutlined />
                      {{ item.duration }}
                    </span>
                  </div>
                </div>

                <div class="result-item-arrow">
                  <RightOutlined />
                </div>
              </div>
            </div>

            <div v-else class="empty-wrap">
              <FolderOpenOutlined class="empty-icon" />
              <h3 class="empty-title">没有找到相关内部资源</h3>
              <p class="empty-desc">
                你可以更换关键词、切换资源分类，或者直接使用外部平台继续搜索。
              </p>
              <div class="empty-actions">
                <a-button class="quick-btn bili" @click="searchExternal('bilibili')">
                  <PlaySquareOutlined />
                  去 B站 搜索
                </a-button>
                <a-button class="quick-btn csdn" @click="searchExternal('csdn')">
                  <CodeOutlined />
                  去 CSDN 搜索
                </a-button>
                <a-button class="quick-btn gitee" @click="searchExternal('gitee')">
                  <CodeOutlined />
                  去 Gitee 搜索
                </a-button>
              </div>
            </div>
          </a-spin>

          <div
            v-if="total > 0"
            class="pagination-wrap"
          >
            <a-pagination
              v-model:current="page"
              :page-size="pageSize"
              :total="total"
              :show-size-changer="false"
              :show-less-items="true"
              @change="handlePageChange"
            />
          </div>
        </template>
      </section>

      <aside class="right-panel panel-card">
        <div class="preview-header">
          <div>
            <div class="panel-title">资源预览</div>
            <p class="preview-subtitle">
              选中左侧资源后，在这里查看详细信息与操作入口
            </p>
          </div>
        </div>

        <template v-if="selectedResource && sourceMode === 'internal'">
          <div class="preview-cover">
            <img :src="selectedResource.cover" :alt="selectedResource.title" />
            <div class="preview-cover-mask"></div>
            <div class="preview-type-chip" :class="selectedResource.type">
              {{ typeLabelMap[selectedResource.type] }}
            </div>
          </div>

          <div class="preview-content">
            <div class="preview-title-row">
              <h3 class="preview-title">
                {{ selectedResource.title }}
              </h3>
            </div>

            <div class="preview-meta-grid">
              <div class="meta-grid-item">
                <span class="meta-grid-label">资源类型</span>
                <span class="meta-grid-value">{{ typeLabelMap[selectedResource.type] }}</span>
              </div>
              <div class="meta-grid-item">
                <span class="meta-grid-label">上传作者</span>
                <span class="meta-grid-value">{{ selectedResource.author }}</span>
              </div>
              <div class="meta-grid-item">
                <span class="meta-grid-label">所属栏目</span>
                <span class="meta-grid-value">{{ selectedResource.course || '未归类' }}</span>
              </div>
              <div class="meta-grid-item">
                <span class="meta-grid-label">发布时间</span>
                <span class="meta-grid-value">{{ selectedResource.date }}</span>
              </div>
            </div>

            <div class="preview-section">
              <div class="preview-section-title">关键词</div>
              <div class="preview-tags">
                <span
                  v-for="tag in selectedResource.tags"
                  :key="tag"
                  class="preview-tag"
                >
                  {{ tag }}
                </span>
              </div>
            </div>

            <div class="preview-actions" :class="{ 'single-action': !canShowSaveButton }">
              <a-button type="primary" class="preview-btn primary" @click="handleViewDetail">
                <FileSearchOutlined />
                查看详情
              </a-button>

              <a-button
                v-if="canShowSaveButton"
                class="preview-btn save-btn"
                :class="{ saved: hasSavedSelectedResource }"
                :loading="saveLoading"
                :disabled="hasSavedSelectedResource"
                @click="handleSaveResource"
              >
                <template v-if="!saveLoading">
                  <SaveOutlined />
                  {{ saveButtonText }}
                </template>
              </a-button>
            </div>
          </div>
        </template>

        <div v-else class="preview-empty">
          <FileSearchOutlined class="preview-empty-icon" />
          <h3>暂无预览内容</h3>
          <p>
            {{ sourceMode === 'internal'
            ? '请从左侧结果列表中选择一项资源'
            : '当前为外部搜索模式，点击顶部搜索后将直接跳转平台页面' }}
          </p>
        </div>
      </aside>
    </div>

    <a-modal
      v-model:open="detailVisible"
      width="1100px"
      title="资源详情"
      destroy-on-close
      centered
      :footer="null"
      class="resource-detail-modal teacher-wide-modal"
      @cancel="handleCloseDetail"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="detailData">
          <div class="detail-header">
            <div class="detail-title-wrap">
              <h2 class="detail-title">{{ detailData.title }}</h2>
              <div class="detail-meta">
                <span>{{ detailData.author || '未命名作者' }}</span>
                <span>{{ detailData.createTime || '--' }}</span>
                <span>{{ typeLabelMap[detailData.type] }}</span>
              </div>
            </div>
          </div>

          <div class="detail-body">
            <template v-if="detailData.type === 'video' || detailData.type === 'micro_video'">
              <template v-if="detailChapters.length > 0">
                <div class="video-player-wrap">
                  <div class="video-main">
                    <video
                      v-if="activeChapterVideo"
                      class="detail-video"
                      :src="activeChapterVideo"
                      :key="activeChapterVideo"
                      controls
                      autoplay
                      preload="metadata"
                    />
                    <div v-else class="detail-empty">当前选集暂无可播放地址</div>
                  </div>
                  <div class="chapter-list">
                    <div class="chapter-list-title">选集列表</div>
                    <div
                      v-for="(ch, idx) in detailChapters"
                      :key="ch.id"
                      class="chapter-item"
                      :class="{ active: activeChapterIndex === idx }"
                      @click="activeChapterIndex = idx"
                    >
                      <span class="chapter-index">P{{ ch.sortOrder ?? idx + 1 }}</span>
                      <span class="chapter-title">{{ ch.title }}</span>
                      <PlayCircleOutlined v-if="activeChapterIndex === idx" class="chapter-playing-icon" />
                    </div>
                  </div>
                </div>
              </template>
              <template v-else>
                <video
                  v-if="detailData.videoUrl"
                  class="detail-video"
                  :src="detailData.videoUrl"
                  controls
                  preload="metadata"
                />
                <div v-else class="detail-empty">当前视频暂无可播放地址</div>
              </template>
              <!-- 视频课程简介 -->
              <div v-if="detailData.summary || detailData.content" class="video-desc-box">
                <div class="video-desc-label"><ReadOutlined /> 课程简介</div>
                <p class="video-desc-text">{{ detailData.summary || detailData.content }}</p>
              </div>
            </template>

            <template v-else-if="detailData.type === 'anim' || selectedResource?.type === 'anim'">
              <AnimationWorkbench
                v-if="detailAnimPayload"
                :payload="detailAnimPayload"
                render-status="ready"
                :validation-errors="[]"
                :is-generating="false"
                :is-optimizing="false"
                :autoplay-delay="1800"
                :preview-mode="true"
              />
              <div v-else class="detail-empty">当前互动课件暂无内容</div>
            </template>

            <template v-else>
              <div
                v-if="detailData.content"
                class="detail-doc-content markdown-render doc-style"
                v-html="renderContent(detailData.content)"
              ></div>
              <div v-else class="detail-empty">当前资源暂无正文内容</div>
            </template>
          </div>
        </template>

        <div v-else class="detail-empty">暂无详情内容</div>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import request from '@/utils/request'
import AnimationWorkbench from '@/components/anim-player/AnimationWorkbench.vue'
import {
  BookOutlined,
  CheckCircleOutlined,
  CodeOutlined,
  SaveOutlined,
  EyeOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  FundProjectionScreenOutlined,
  GlobalOutlined,
  PlayCircleOutlined,
  PlaySquareOutlined,
  ReadOutlined,
  RightOutlined,
  RocketOutlined,
  SearchOutlined,
  UserOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons-vue'

type ResourceType = 'video' | 'plan' | 'quiz' | 'anim' | 'micro_video' | 'case'
type ResourceTypeWithAll = 'all' | ResourceType
type SourceMode = 'internal' | 'bilibili' | 'csdn' | 'gitee'
type SortMode = 'relevance' | 'newest' | 'popular'

interface ResourceSearchItem {
  id: number
  type: ResourceType
  title: string
  desc: string
  cover?: string
  author: string
  views: number
  date: string
  course: string
  duration?: string
  tags: string[]
  previewText: string
  link?: string
  sourceType?: string
}

interface ResourceSearchPageData {
  records: ResourceSearchItem[]
  total: number
  current: number
  pageSize: number
  videoCount: number
  planCount: number
  quizCount: number
  animCount: number
  microVideoCount?: number
  caseCount?: number
  supportNotice?: string
}

interface ResourcePreviewData {
  id: number
  type: ResourceType
  title: string
  author: string
  cover?: string
  videoUrl?: string
  content?: string
  summary?: string
  createTime?: string
}

interface SaveResourceResult {
  id: number
  alreadySaved: boolean
}

const searchText = ref('')
const activeType = ref<ResourceTypeWithAll>('all')
const sourceMode = ref<SourceMode>('internal')
const sortMode = ref<SortMode>('relevance')
const page = ref(1)
const pageSize = 6
const loading = ref(false)
const backendNotice = ref('')
const total = ref(0)

const internalResources = ref<ResourceSearchItem[]>([])
const selectedResource = ref<ResourceSearchItem | null>(null)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<ResourcePreviewData | null>(null)

// 教学视频专用：章节列表 & 当前选中章节
interface ChapterItem {
  id: number
  title: string
  videoUrl?: string
  sortOrder?: number
}
const detailChapters = ref<ChapterItem[]>([])
const activeChapterIndex = ref(0)
const activeChapterVideo = computed(() =>
  detailChapters.value[activeChapterIndex.value]?.videoUrl || ''
)

// 已保存资源的 key 集合，格式同 selectedResourceKey：`${type}-${id}`


const savedResourceKeys = ref<Set<string>>(new Set())
const saveLoading = ref(false)

const detailAnimPayload = computed(() => {
  const content = detailData.value?.content
  if (!content) return null
  try {
    const parsed = JSON.parse(content)
    if (parsed && typeof parsed === 'object') return parsed
    return null
  } catch {
    return null
  }
})

const typeCounts = ref({
  video: 0,
  plan: 0,
  quiz: 0,
  anim: 0,
  micro_video: 0,
  case: 0,
})

const sourceOptions = [
  { label: '内部资源', value: 'internal' },
  { label: 'Bilibili', value: 'bilibili' },
  { label: 'CSDN', value: 'csdn' },
  { label: 'Gitee', value: 'gitee' },
]

const sourceLabelMap: Record<SourceMode, string> = {
  internal: '内部资源',
  bilibili: 'Bilibili',
  csdn: 'CSDN',
  gitee: 'Gitee',
}

const typeLabelMap: Record<string, string> = {
  video: '教学视频',
  plan: '教案文档',
  quiz: '练习题',
  anim: '互动课件',
}

typeLabelMap.micro_video = '微课视频'
typeLabelMap.case = '教学案例'

const selectedResourceKey = computed(() => {
  if (!selectedResource.value) return ''
  return `${selectedResource.value.type}-${selectedResource.value.id}`
})

const canShowSaveButton = computed(() => {
  return !!selectedResource.value && selectedResource.value.type !== 'video'
})

const hasSavedSelectedResource = computed(() => {
  return !!selectedResourceKey.value && savedResourceKeys.value.has(selectedResourceKey.value)
})

const saveButtonText = computed(() => {
  if (selectedResource.value?.type === 'case') {
    return hasSavedSelectedResource.value ? '已在案例管理' : '保存到案例管理'
  }
  return hasSavedSelectedResource.value ? '已有该资源' : '保存到我的资源'
})

const typeStats = computed(() => {
  const totalCount =
    typeCounts.value.video +
    typeCounts.value.plan +
    typeCounts.value.quiz +
    typeCounts.value.anim +
    typeCounts.value.micro_video +
    typeCounts.value.case

  return [
    {
      value: 'all' as ResourceTypeWithAll,
      label: '综合全部',
      count: totalCount,
      icon: GlobalOutlined,
    },
    {
      value: 'video' as ResourceTypeWithAll,
      label: '教学视频',
      count: typeCounts.value.video,
      icon: VideoCameraOutlined,
    },
    {
      value: 'plan' as ResourceTypeWithAll,
      label: '教案文档',
      count: typeCounts.value.plan,
      icon: FileTextOutlined,
    },
    {
      value: 'quiz' as ResourceTypeWithAll,
      label: '练习题',
      count: typeCounts.value.quiz,
      icon: ReadOutlined,
    },
    {
      value: 'anim' as ResourceTypeWithAll,
      label: '互动课件',
      count: typeCounts.value.anim,
      icon: FundProjectionScreenOutlined,
    },
    {
      value: 'micro_video' as ResourceTypeWithAll,
      label: '微课视频',
      count: typeCounts.value.micro_video,
      icon: VideoCameraOutlined,
    },
    {
      value: 'case' as ResourceTypeWithAll,
      label: '教学案例',
      count: typeCounts.value.case,
      icon: FileTextOutlined,
    },
  ]
})

const activeTypeLabel = computed(() => {
  return typeStats.value.find(item => item.value === activeType.value)?.label || '综合全部'
})

function createCover(title: string, left = '#4f46e5', right = '#3b82f6') {
  const shortTitle = title.length > 20 ? `${title.slice(0, 20)}...` : title
  const svg = `
  <svg xmlns="http://www.w3.org/2000/svg" width="800" height="450">
    <defs>
      <linearGradient id="g" x1="0" x2="1" y1="0" y2="1">
        <stop offset="0%" stop-color="${left}" />
        <stop offset="100%" stop-color="${right}" />
      </linearGradient>
    </defs>
    <rect width="800" height="450" fill="url(#g)" rx="5" />
    <circle cx="680" cy="70" r="120" fill="rgba(255,255,255,0.08)" />
    <circle cx="740" cy="380" r="140" fill="rgba(255,255,255,0.08)" />
    <text x="56" y="160" fill="white" font-size="26" font-weight="700" font-family="Arial, sans-serif">智慧教育平台</text>
    <text x="56" y="220" fill="white" font-size="34" font-weight="800" font-family="Arial, sans-serif">${shortTitle}</text>
    <text x="56" y="290" fill="rgba(255,255,255,0.92)" font-size="22" font-family="Arial, sans-serif">教学资源检索预览封面</text>
  </svg>
  `
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

function getDefaultDurationByType(type: ResourceType) {
  if (type === 'micro_video') return '微课视频'
  if (type === 'case') return '教学案例'
  switch (type) {
    case 'video':
      return '视频课程'
    case 'plan':
      return '教案资源'
    case 'quiz':
      return '练习题资源'
    case 'anim':
      return '互动课件'
  }
}

function getDefaultCoverColor(type: ResourceType) {
  if (type === 'micro_video') return ['#0f766e', '#14b8a6']
  if (type === 'case') return ['#1d4ed8', '#64748b']
  switch (type) {
    case 'video':
      return ['#2563eb', '#0ea5e9']
    case 'plan':
      return ['#059669', '#10b981']
    case 'quiz':
      return ['#ea580c', '#f97316']
    case 'anim':
      return ['#7c3aed', '#a855f7']
  }
}

function normalizeResourceItem(item: ResourceSearchItem): ResourceSearchItem {
  const [left, right] = getDefaultCoverColor(item.type)

  return {
    ...item,
    cover: item.cover || createCover(item.title, left, right),
    desc: item.desc || '暂无资源简介',
    previewText: item.previewText || item.desc || '暂无摘要预览',
    tags: Array.isArray(item.tags) ? item.tags : [],
    views: Number(item.views || 0),
    date: item.date || '--',
    author: item.author || '金牌讲师',
    course: item.course || item.title,
    duration: item.duration || getDefaultDurationByType(item.type),
  }
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function highlightText(text: string, keyword: string) {
  const value = keyword.trim()
  if (!value) return text
  const reg = new RegExp(`(${escapeRegExp(value)})`, 'gi')
  return text.replace(reg, '<span class="keyword-highlight">$1</span>')
}

function formatViews(value: number) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function textToHtml(text?: string) {
  if (!text) return ''
  return escapeHtml(text).replace(/\n/g, '<br/>')
}

const _md = new MarkdownIt({ breaks: true, html: true })
// 兼容两种存储格式：HTML（contenteditable 编辑后保存）和 Markdown
function renderContent(content?: string): string {
  if (!content) return ''
  const trimmed = content.trimStart()
  // 已经是 HTML（以标签开头），直接渲染
  if (trimmed.startsWith('<')) return content
  // 否则按 Markdown 渲染
  return _md.render(content)
}

async function searchInternalResources(params: {
  current?: number
  pageSize?: number
  keyword?: string
  type?: ResourceTypeWithAll
  sortMode?: SortMode
}) {
  return request.get('/course/search/resources', {
    params,
  }) as Promise<ResourceSearchPageData>
}

async function getResourcePreview(params: { id: number; type: ResourceType }) {
  return request.get('/course/search/resource-preview', {
    params,
  }) as Promise<ResourcePreviewData>
}

async function fetchSavedResourceKeys() {
  try {
    const [resourceKeys, savedCaseIds] = await Promise.all([
      request.get<string[], string[]>('/ai/resource/saved-keys', {
        skipErrorToast: true,
      }),
      request.get<number[], number[]>('/teaching-case/saved-platform-ids', {
        skipErrorToast: true,
      }),
    ])
    savedResourceKeys.value = new Set([
      ...(resourceKeys || []),
      ...(savedCaseIds || []).map(id => `case-${id}`),
    ])
  } catch {
    savedResourceKeys.value = new Set()
  }
}

async function fetchResources(showSuccess = false) {
  if (sourceMode.value !== 'internal') return

  loading.value = true
  try {
    const data = await searchInternalResources({
      current: page.value,
      pageSize,
      keyword: searchText.value.trim() || undefined,
      type: activeType.value,
      sortMode: sortMode.value,
    })

    internalResources.value = (data.records || []).map(normalizeResourceItem)
    total.value = data.total || 0
    typeCounts.value = {
      video: data.videoCount || 0,
      plan: data.planCount || 0,
      quiz: data.quizCount || 0,
      anim: data.animCount || 0,
      micro_video: data.microVideoCount || 0,
      case: data.caseCount || 0,
    }
    backendNotice.value = data.supportNotice || ''

    if (showSuccess) {
      if (searchText.value.trim()) {
        message.success(`已完成内部检索：${searchText.value.trim()}`)
      } else {
        message.success('已加载内部资源')
      }
    }
  } catch (error) {
    internalResources.value = []
    total.value = 0
    typeCounts.value = {
      video: 0,
      plan: 0,
      quiz: 0,
      anim: 0,
      micro_video: 0,
      case: 0,
    }
    backendNotice.value = ''
  } finally {
    loading.value = false
  }
}

watch(internalResources, (list) => {
  if (!list.length) {
    selectedResource.value = null
    return
  }

  const currentKey = selectedResourceKey.value
  const matched = list.find(item => `${item.type}-${item.id}` === currentKey)
  selectedResource.value = matched || list[0]
})

watch(sourceMode, async (value) => {
  if (value === 'internal') {
    page.value = 1
    await fetchResources()
  } else {
    selectedResource.value = null
  }
})

onMounted(async () => {
  await Promise.all([
    fetchResources(),
    fetchSavedResourceKeys(),
  ])
})

async function handleSearch() {
  const keyword = searchText.value.trim()

  if (sourceMode.value === 'internal') {
    page.value = 1
    await fetchResources(true)
    return
  }

  if (!keyword) {
    message.warning('请输入要搜索的关键词')
    return
  }

  searchExternal(sourceMode.value)
}

async function handleReset() {
  searchText.value = ''
  activeType.value = 'all'
  sourceMode.value = 'internal'
  sortMode.value = 'relevance'
  page.value = 1
  detailVisible.value = false
  detailData.value = null
  await fetchResources()
  message.success('已重置检索条件')
}

async function handleTypeChange(type: ResourceTypeWithAll) {
  if (activeType.value === type) return
  activeType.value = type
  page.value = 1

  if (sourceMode.value === 'internal') {
    await fetchResources()
  }
}

async function handlePageChange(current: number) {
  page.value = current
  await fetchResources()
}

function selectResource(item: ResourceSearchItem) {
  selectedResource.value = item
}

function switchToInternal() {
  sourceMode.value = 'internal'
}

async function handleSaveResource() {
  if (!selectedResource.value) {
    message.warning('请先选择一项资源')
    return
  }

  const key = selectedResourceKey.value
  if (savedResourceKeys.value.has(key)) {
    message.info(selectedResource.value.type === 'case' ? '该案例已保存到案例管理' : '该资源已保存到我的资源')
    return
  }

  saveLoading.value = true
  try {
    if (selectedResource.value.type === 'case') {
      const res = await request.post<SaveResourceResult, SaveResourceResult>(
        `/teaching-case/save-platform/${selectedResource.value.id}`,
        {},
        { skipSuccessToast: true }
      )
      savedResourceKeys.value = new Set([...savedResourceKeys.value, key])
      if (res?.alreadySaved) {
        message.success(`「${selectedResource.value.title}」已在案例管理中，无需重复保存`)
      } else {
        message.success(`「${selectedResource.value.title}」已保存到案例管理`)
      }
      return
    }

    let preview = detailData.value
    if (!preview || `${preview.type}-${preview.id}` !== key) {
      preview = await getResourcePreview({
        id: selectedResource.value.id,
        type: selectedResource.value.type,
      })
    }

    if (!preview?.content) {
      message.warning('该资源暂无可保存的内容')
      return
    }

    const res = await request.post<SaveResourceResult, SaveResourceResult>('/ai/resource/save', {
      sourceId: selectedResource.value.id,
      sourceType: selectedResource.value.type,
      type: preview.type,
      title: preview.title,
      content: preview.content,
    })

    savedResourceKeys.value = new Set([...savedResourceKeys.value, key])

    if (res?.alreadySaved) {
      message.success(`「${selectedResource.value.title}」已在我的资源中，无需重复保存`)
    } else {
      message.success(`「${selectedResource.value.title}」已保存到我的资源库`)
    }
  } catch (error: any) {
    message.error(error?.message || '保存失败，请稍后重试')
  } finally {
    saveLoading.value = false
  }
}

async function handleViewDetail() {
  if (!selectedResource.value) {
    message.warning('请先选择一项资源')
    return
  }

  detailLoading.value = true
  detailVisible.value = true
  detailChapters.value = []
  activeChapterIndex.value = 0

  try {
    const data = await getResourcePreview({
      id: selectedResource.value.id,
      type: selectedResource.value.type,
    })
    detailData.value = data

    // 视频类型：额外拉章节列表，从 course_chapter.video_url 获取真实播放地址
    if (data.type === 'video') {
      try {
        const chapters = await request.get('/chapter/list', {
          params: { courseId: selectedResource.value.id },
        }) as any[]
        detailChapters.value = (chapters || []).map((ch: any) => ({
          id: ch.id,
          title: ch.title || ch.chapterTitle || `第${ch.sortOrder}集`,
          videoUrl: ch.videoUrl || ch.video_url || '',
          sortOrder: ch.sortOrder ?? ch.sort_order,
        }))
        // 默认定位到第一个有视频的选集
        const firstValid = detailChapters.value.findIndex(ch => ch.videoUrl)
        if (firstValid >= 0) activeChapterIndex.value = firstValid
      } catch {
        // 章节接口失败不影响主流程，静默降级到课程级 videoUrl
        detailChapters.value = []
      }
    }
  } catch (error) {
    detailData.value = null
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function handleCloseDetail() {
  detailVisible.value = false
  detailChapters.value = []
  activeChapterIndex.value = 0
}

function searchExternal(platform: Exclude<SourceMode, 'internal'>) {
  const keyword = searchText.value.trim()
  if (!keyword) {
    message.warning('请先输入搜索关键词')
    return
  }

  const encoded = encodeURIComponent(keyword)
  let url = ''

  switch (platform) {
    case 'bilibili':
      url = `https://search.bilibili.com/all?keyword=${encoded}`
      break
    case 'csdn':
      url = `https://so.csdn.net/so/search/s.do?q=${encoded}`
      break
    case 'gitee':
      url = `https://search.gitee.com/?q=${encoded}`
      break
  }

  if (url) {
    window.open(url, '_blank')
  }
}
</script>

<style scoped>
.resource-search-page {
  /* 统一字体与入场动画 */
  font-family: 'Plus Jakarta Sans', sans-serif;
  animation: fadeIn 0.4s ease;

  /* 统一背景、内边距和圆角 */
  background: #f8fafc;
  border-radius: 5px;
  padding: 32px;
  box-sizing: border-box;

  /* 保留 Flex 布局与高度限制，防止撑破屏幕 */
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-top {
  display: grid;
  grid-template-columns: minmax(300px, 0.9fr) minmax(680px, 1.35fr);
  gap: 24px;
  align-items: start;
  margin-bottom: 16px;
  flex-shrink: 0; /* 关键修改：防止顶部区域被挤压 */
}

.title-group {
  min-width: 0;
  margin-bottom: 0;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
}

.title-icon {
  font-size: 30px;
  color: #4f46e5;
}

.page-subtitle {
  margin: 8px 0 0;
  font-size: 15px;
  line-height: 1.7;
  color: #64748b;
}

.toolbar-card,
.panel-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.92);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.toolbar-card {
  justify-self: end;
  width: 100%;
  max-width: 820px;
  padding: 0;
  background: transparent !important;
  border: 0 !important;
  box-shadow: none !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
}

.toolbar-main {
  display: grid;
  grid-template-columns: 132px minmax(300px, 520px) 72px 68px;
  gap: 8px;
  align-items: center;
  justify-content: end;
}

.source-select {
  width: 100%;
}

.source-select :deep(.ant-select-selector) {
  height: 40px !important;
  border-color: #dbe3ee !important;
  background: #ffffff !important;
}

.source-select :deep(.ant-select-selection-item) {
  line-height: 38px !important;
  font-weight: 600;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 12px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  background: #ffffff;
  border: 1px solid #dbe3ee;
  transition: 0.2s ease;
}

.search-input-wrap:focus-within {
  background: #ffffff;
  border-color: #dbe3ee !important;
  outline: none !important;
  box-shadow: none !important;
}

.search-input-wrap:focus,
.search-input-wrap:focus-visible,
.search-input-wrap:active {
  outline: none !important;
  box-shadow: none !important;
}

.search-icon {
  font-size: 16px;
  color: #94a3b8;
  margin-right: 10px;
}

.search-input {
  flex: 1;
  height: 100%;
  border: none;
  outline: none !important;
  outline-offset: 0;
  box-shadow: none !important;
  -webkit-appearance: none;
  appearance: none;
  background: transparent;
  color: #0f172a;
  font-size: 14px;
}

.search-input:focus,
.search-input:focus-visible,
.search-input:active {
  border: none !important;
  outline: none !important;
  outline-offset: 0 !important;
  box-shadow: none !important;
}

.search-input::placeholder {
  color: #94a3b8;
}

.action-btn {
  height: 40px;
  padding: 0 14px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  font-weight: 600;
}

.search-btn {
  background: #2563eb;
  border: none;
  box-shadow: none;
}

.toolbar-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}

.keyword-tip {
  font-size: 13px;
  color: #64748b;
}

.keyword-tip span {
  color: #4f46e5;
  font-weight: 700;
}

.quick-links,
.external-actions,
.empty-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  border: none;
  color: #fff;
  font-weight: 600;
}

.quick-btn.bili {
  background: #fb7299;
}

.quick-btn.csdn {
  background: #fc5531;
}

.quick-btn.gitee {
  background: #c71d23;
}

.workbench {
  display: grid;
  grid-template-columns: 220px minmax(520px, 1fr) 420px;
  gap: 16px;
  /* 关键修改：撑满剩余高度，并使用 min-height: 0 防止子元素撑破父级 */
  flex: 1;
  min-height: 0;
}

.left-panel,
.right-panel {
  padding: 16px;
  /* 关键修改：高度100%，允许内部Y轴滚动，取消固定最小高度 */
  height: 100%;
  overflow-y: auto;
}

.center-panel {
  padding: 16px;
  height: 100%;
  overflow-y: auto;
  /* 新增：改为 Flex 纵向布局，为了把分页器挤到底部 */
  display: flex;
  flex-direction: column;
}

/* 添加：美化卡片内部的滚动条 */
.panel-card::-webkit-scrollbar {
  width: 6px;
}
.panel-card::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 5px;
}

.panel-title {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.preview-subtitle,
.result-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
}

.type-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  background: #f8fafc;
  border: 1px solid transparent;
  cursor: pointer;
  transition: 0.22s ease;
}

.type-item:hover {
  transform: translateY(-1px);
  border-color: #dbeafe;
  background: #f8fbff;
}

.type-item.active {
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.1), rgba(37, 99, 235, 0.08));
  border-color: rgba(79, 70, 229, 0.18);
  box-shadow: 0 10px 20px rgba(79, 70, 229, 0.08);
}

.type-item-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.type-item-left span {
  font-size: 14px;
  color: #0f172a;
  font-weight: 600;
}

.type-item-icon {
  color: #4f46e5;
  font-size: 16px;
}

.type-item-count {
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  background: #ffffff;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.side-divider {
  height: 1px;
  margin: 18px 0;
  background: linear-gradient(90deg, transparent, #e2e8f0, transparent);
}

.tips-box {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.tip-icon {
  font-size: 15px;
}

.tip-icon.success {
  color: #10b981;
}

.tip-icon.primary {
  color: #4f46e5;
}

.stats-card {
  margin-top: 18px;
  padding: 16px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  background: linear-gradient(135deg, #eef2ff, #eff6ff);
  border: 1px solid rgba(99, 102, 241, 0.12);
}

.stats-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.stats-value {
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
  color: #1e3a8a;
  margin-bottom: 8px;
}

.stats-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}

.result-header,
.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.result-title {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
  font-weight: 800;
}

.result-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badge-chip {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  font-size: 12px;
  font-weight: 700;
  color: #4338ca;
  background: #eef2ff;
}

.badge-chip.light {
  color: #0369a1;
  background: #ecfeff;
}

.support-notice {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  background: linear-gradient(135deg, #fff7ed, #fffbeb);
  border: 1px solid #fed7aa;
  color: #9a3412;
  font-size: 13px;
  line-height: 1.7;
}

.external-mode-box {
  display: flex;
  gap: 16px;
  align-items: center;
  min-height: 240px;
  padding: 24px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  background: linear-gradient(135deg, #f8fafc, #eef2ff);
  border: 1px dashed #c7d2fe;
}

.external-mode-icon-wrap {
  width: 72px;
  height: 72px;
  flex: 0 0 72px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.1);
}

.external-mode-icon {
  font-size: 28px;
  color: #4f46e5;
}

.external-mode-content h4 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #0f172a;
  font-weight: 800;
}

.external-mode-content p {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.8;
  color: #64748b;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  /* 关键修改：移除固定高度，由外部控制 */
}

.result-item {
  display: grid;
  grid-template-columns: 168px minmax(0, 1fr) 22px;
  gap: 14px;
  align-items: center;
  padding: 12px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  border: 1px solid #edf2f7;
  background: #ffffff;
  cursor: pointer;
  transition: 0.22s ease;
}

.result-item:hover {
  transform: translateY(-2px);
  border-color: #c7d2fe;
  box-shadow: 0 14px 26px rgba(37, 99, 235, 0.08);
}

.result-item.active {
  border-color: rgba(79, 70, 229, 0.28);
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.72), rgba(248, 250, 252, 0.98));
  box-shadow: 0 16px 30px rgba(79, 70, 229, 0.12);
}

.result-item-cover {
  position: relative;
  width: 100%;
  height: 102px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  overflow: hidden;
  background: #f1f5f9;
}

.result-item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-badge,
.preview-type-chip {
  position: absolute;
  top: 10px;
  left: 10px;
  height: 28px;
  padding: 0 10px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  color: #fff;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.cover-badge.video,
.preview-type-chip.video {
  background: rgba(37, 99, 235, 0.9);
}

.cover-badge.plan,
.preview-type-chip.plan {
  background: rgba(16, 185, 129, 0.92);
}

.cover-badge.quiz,
.preview-type-chip.quiz {
  background: rgba(245, 158, 11, 0.95);
}

.cover-badge.anim,
.preview-type-chip.anim {
  background: rgba(124, 58, 237, 0.95);
}

.result-item-main {
  min-width: 0;
}

.result-item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.item-title {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
  font-weight: 800;
  line-height: 1.5;
  /* 关键修改：取消多行省略，改为严格的单行省略 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-date {
  flex: 0 0 auto;
  font-size: 12px;
  color: #94a3b8;
}

.item-desc {
  margin: 8px 0 10px;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
  /* 关键修改：取消多行省略，改为严格的单行省略 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-tags,
.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.item-tag,
.preview-tag {
  height: 28px;
  padding: 0 10px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  border: 1px solid #e2e8f0;
}

.item-meta {
  display: flex;
  /* 关键修改：将原来的 flex-wrap: wrap 改为 nowrap，强制在一行内显示 */
  flex-wrap: nowrap;
  gap: 12px;
  margin-top: 10px;
  /* 隐藏整体超出的部分 */
  overflow: hidden;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #64748b;
  /* 关键修改：当单个标签（如作者名过长）空间不足时，也显示省略号 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-item-arrow {
  color: #94a3b8;
  font-size: 14px;
}

.empty-wrap,
.preview-empty {
  min-height: 420px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  border: 1px dashed #dbe4ef;
  background: linear-gradient(180deg, #fcfdff, #f8fafc);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: #64748b;
  text-align: center;
  padding: 24px;
}

.empty-icon,
.preview-empty-icon {
  font-size: 56px;
  margin-bottom: 14px;
  color: #cbd5e1;
}

.empty-title,
.preview-empty h3 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
}

.empty-desc,
.preview-empty p {
  margin: 0 0 18px;
  max-width: 380px;
  line-height: 1.8;
  font-size: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding-top: 16px;
  /* Flex 魔法：推到容器最底部 */
  margin-top: auto;
  /* Sticky 魔法：滚动时吸附在卡片底部，防止被数据遮挡 */
  position: sticky;
  bottom: -16px; /* 抵消 center-panel 的 padding */
  padding-bottom: 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  z-index: 10;
  border-top: 1px dashed #e2e8f0;
}

.preview-cover {
  position: relative;
  height: 220px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  overflow: hidden;
  background: #f1f5f9;
  margin-bottom: 16px;
}

.preview-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-cover-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(15, 23, 42, 0.3), rgba(15, 23, 42, 0.05));
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.preview-title {
  margin: 0;
  font-size: 22px;
  line-height: 1.45;
  font-weight: 800;
  color: #0f172a;
}

.preview-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.meta-grid-item {
  padding: 8px 10px;
  border-radius: 5px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
}

.meta-grid-label {
  display: block;
  margin-bottom: 4px;
  font-size: 11px;
  color: #94a3b8;
}

.meta-grid-value {
  display: block;
  font-size: 12px;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-section-title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.preview-text-box {
  padding: 14px 16px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  background: linear-gradient(180deg, #f8fafc, #ffffff);
  border: 1px solid #e2e8f0;
  font-size: 13px;
  line-height: 1.85;
  color: #475569;
}

.preview-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 4px;
}

.preview-btn {
  height: 42px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  font-weight: 700;
}

.preview-btn.primary {
  background: linear-gradient(135deg, #4f46e5, #2563eb);
  border: none;
  box-shadow: 0 10px 18px rgba(79, 70, 229, 0.2);
}

.preview-btn.save-btn {
  border-color: #10b981;
  color: #10b981;
}

.preview-btn.save-btn:hover:not(:disabled) {
  background: #f0fdf4;
  border-color: #059669;
  color: #059669;
}

.preview-btn.save-btn.saved {
  background: #f0fdf4;
  border-color: #a7f3d0;
  color: #059669;
  cursor: default;
}

.detail-header {
  margin-bottom: 16px;
}

.detail-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.45;
  font-weight: 800;
  color: #0f172a;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.detail-summary {
  margin-bottom: 18px;
  padding: 14px 16px;
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
}

.detail-body {
  border-radius: 5px; /* 修改：统一 5px 圆角 */
  border: 1px solid #e2e8f0;
  background: #fff;
  flex: 1;
  min-height: 0;
  max-height: none;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.resource-detail-modal .ant-spin-container) {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

:global(.resource-detail-modal .detail-header) {
  flex-shrink: 0;
}

.detail-video {
  display: block;
  width: 100%;
  max-height: 560px;
  background: #000;
}

/* 选集播放器布局 */
.video-player-wrap {
  display: flex;
  height: 480px;
}

.video-main {
  flex: 1;
  min-width: 0;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-main .detail-video {
  max-height: 480px;
}

.chapter-list {
  width: 220px;
  flex-shrink: 0;
  border-left: 1px solid #e2e8f0;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chapter-list-title {
  padding: 12px 14px;
  font-size: 13px;
  font-weight: 800;
  color: #0f172a;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}

.chapter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.15s;
  font-size: 13px;
  color: #475569;
  overflow: hidden;
}

.chapter-item:hover {
  background: #f1f5f9;
}

.chapter-item.active {
  background: #ede9fe;
  color: #4f46e5;
}

.chapter-index {
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  flex-shrink: 0;
  min-width: 24px;
}

.chapter-item.active .chapter-index {
  color: #7c3aed;
}

.chapter-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.chapter-playing-icon {
  font-size: 14px;
  color: #4f46e5;
  flex-shrink: 0;
}

/* 章节列表滚动区 */
.chapter-list {
  overflow-y: auto;
}

.detail-doc-content {
  max-height: 640px;
  overflow: auto;
  padding: 28px 32px;
  font-size: 14px;
  line-height: 1.9;
  color: #334155;
  word-break: break-word;
}

/* Markdown 渲染样式 */
.detail-doc-content :deep(h1) {
  font-size: 22px; font-weight: 800; color: #0f172a;
  text-align: center; margin-bottom: 20px;
  padding-bottom: 12px; border-bottom: 2px solid #e2e8f0;
}
.detail-doc-content :deep(h2) {
  font-size: 16px; font-weight: 700; color: #1e293b;
  background: #f8fafc; padding: 8px 12px;
  border-left: 4px solid #4f46e5; margin: 24px 0 12px;
  border-radius: 0 5px 5px 0;
}
.detail-doc-content :deep(h3) {
  font-size: 15px; font-weight: 700; color: #1e293b;
  margin: 16px 0 8px; padding-left: 10px;
  border-left: 3px solid #a5b4fc;
}
.detail-doc-content :deep(p) { margin: 0 0 12px; }
.detail-doc-content :deep(ul), .detail-doc-content :deep(ol) { padding-left: 20px; margin: 0 0 12px; }
.detail-doc-content :deep(li) { margin-bottom: 6px; }
.detail-doc-content :deep(strong) { color: #1e293b; font-weight: 700; }
.detail-doc-content :deep(code) {
  background: #f1f5f9; padding: 2px 6px;
  border-radius: 4px; font-size: 13px; color: #4f46e5;
}
.detail-doc-content :deep(blockquote) {
  margin: 12px 0; padding: 10px 14px;
  background: #fffbeb; border-left: 4px solid #f59e0b;
  border-radius: 0 5px 5px 0; color: #92400e; font-size: 13px;
}
.detail-doc-content :deep(hr) {
  border: none; border-top: 1px dashed #e2e8f0; margin: 20px 0;
}

/* 视频课程简介 */
.video-desc-box {
  padding: 18px 20px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}
.video-desc-label {
  font-size: 13px; font-weight: 700; color: #475569;
  display: flex; align-items: center; gap: 6px; margin-bottom: 8px;
}
.video-desc-text {
  font-size: 14px; line-height: 1.8; color: #334155; margin: 0;
}

.detail-empty {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 14px;
  background: #f8fafc;
}

:deep(.ant-select-selector) {
  height: 44px !important;
  border-radius: 5px !important; /* 修改：统一 5px 圆角 */
  display: flex !important;
  align-items: center !important;
  border-color: #e2e8f0 !important;
  box-shadow: none !important;
}

:deep(.ant-select-selection-item) {
  line-height: 42px !important;
  font-weight: 600;
  color: #0f172a;
}

:deep(.ant-pagination-item),
:deep(.ant-pagination-prev),
:deep(.ant-pagination-next) {
  border-radius: 5px !important; /* 修改：统一 5px 圆角 */
}

:deep(.ant-pagination-item-active) {
  border-color: #4f46e5 !important;
}

:deep(.ant-pagination-item-active a) {
  color: #4f46e5 !important;
}

:deep(.keyword-highlight) {
  color: #2563eb;
  font-weight: 800;
  background: linear-gradient(transparent 55%, rgba(37, 99, 235, 0.12) 55%);
}

@media (max-width: 1440px) {
  .workbench {
    grid-template-columns: 210px minmax(420px, 1fr) 360px;
  }

  .page-top {
    grid-template-columns: minmax(260px, 0.8fr) minmax(600px, 1.2fr);
  }

  .toolbar-main {
    grid-template-columns: 124px minmax(260px, 480px) 70px 66px;
  }
}

@media (max-width: 1200px) {
  .resource-search-page {
    padding: 16px; /* 原为 16px，由于前面改成了 32px，这里在小屏下恢复成 16px */
    height: auto;
  }

  .workbench {
    grid-template-columns: 1fr;
  }

  .page-top {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .toolbar-card {
    justify-self: stretch;
    max-width: none;
  }

  .toolbar-main {
    grid-template-columns: 1fr 1fr;
  }

  .search-input-wrap {
    grid-column: span 2;
  }

  .preview-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.preview-actions.single-action {
  display: flex;
}

.preview-actions.single-action .preview-btn.primary {
  width: 100%;
}

.preview-btn.save-btn.saved,
.preview-btn.save-btn[disabled] {
  background: #f8fafc !important;
  color: #94a3b8 !important;
  border-color: #e2e8f0 !important;
  box-shadow: none !important;
  cursor: not-allowed !important;
}

:global(.teacher-wide-modal) {
  max-width: calc(100vw - 48px);
}
</style>
