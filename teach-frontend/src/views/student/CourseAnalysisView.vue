<template>
  <div class="edu-app">
    <main class="edu-main">
      <div class="page-shell">
        <section class="analysis-layout">
          <!-- 左栏：上传 -->
          <div class="upload-panel glass-panel">
            <div class="section-head">
              <div>
                <div class="section-kicker">课程洞察</div>
                <h2>本学期课表识别</h2>
                <p>上传本学期课表 PDF，自动匹配四年课程体系并高亮标识。</p>
              </div>
            </div>

            <div v-if="hasSavedData && !analysisReady" class="saved-hint">
              <div class="saved-hint-text">
                <check-circle-outlined class="saved-icon" />
                <span>检测到上次分析结果</span>
              </div>
              <div class="saved-file-info">
                <file-text-outlined class="saved-file-icon" />
                <div class="saved-file-main">
                  <div class="saved-file-name">{{ latestRecordFileName }}</div>
                  <div class="saved-file-meta">
                    {{ latestRecordTimeText }} · {{ latestRecord?.matchedCourses.length || 0 }} 门课程
                  </div>
                </div>
              </div>
              <div class="saved-actions">
                <button class="btn-load" @click="loadSavedData">查看课表分析</button>
                <button class="btn-reupload" @click="clearSavedState">重新上传</button>
              </div>
            </div>


            <a-upload-dragger
              v-if="!hasSavedData || analysisReady"
              name="file"
              :multiple="false"
              :show-upload-list="false"
              :before-upload="handleUpload"
              :class="['custom-dragger', { 'is-compact': analysisReady }]"
            >
              <p class="ant-upload-drag-icon">
                <inbox-outlined v-if="!analyzing" />
                <loading-outlined v-else spin />
              </p>
              <p class="ant-upload-text">
                {{ analyzing ? progressText : '点击或拖拽本学期课表 PDF' }}
              </p>
              <p class="ant-upload-hint">AI 识别课程名 → 匹配四年课程库 → 生成图谱</p>
            </a-upload-dragger>

            <div v-if="analysisReady" class="stat-cards">
              <div class="stat-card">
                <span class="stat-label">课程总数</span>
                <span class="stat-value">{{ allCourses.length }}</span>
              </div>
              <div class="stat-card">
                <span class="stat-label">本学期</span>
                <span class="stat-value amber">{{ currentNames.length }}</span>
              </div>
              <div class="stat-card">
                <span class="stat-label">总学分</span>
                <span class="stat-value">
                  {{ allCourses.reduce((s, c) => s + c.credits, 0) }}
                </span>
              </div>
              <div class="stat-card">
                <span class="stat-label">学期学分</span>
                <span class="stat-value amber">{{ currentSemesterCredits }}</span>
              </div>
            </div>

            <div v-if="analysisReady" class="side-card summary-card">
              <div class="side-card-head">
                <span>本次分析摘要</span>
                <span class="side-badge light">实时</span>
              </div>

              <div class="summary-grid">
                <div class="summary-item">
                  <span>已识别课程</span>
                  <strong>{{ currentNames.length }} 门</strong>
                </div>
                <div class="summary-item">
                  <span>本学期学分</span>
                  <strong>{{ currentSemesterCredits }}</strong>
                </div>
                <div class="summary-item">
                  <span>覆盖类别</span>
                  <strong>{{ currentCategoryCount }} 类</strong>
                </div>
                <div class="summary-item">
                  <span>最多类别</span>
                  <strong>{{ currentTopCategoryText }}</strong>
                </div>
              </div>
            </div>

            <div v-if="analysisReady && currentNames.length > 0" class="current-list">
              <div class="current-title">本学期课程（已高亮）</div>
              <div v-for="name in currentNames" :key="name" class="current-tag">
                {{ name }}
              </div>
            </div>

            <div v-if="historyRecords.length > 0" class="side-card history-card">
              <div class="side-card-head">
                <span>最近分析记录</span>
                <button class="mini-text-btn" @click="clearHistoryRecords">清空</button>
              </div>

              <div
                v-for="item in historyRecords"
                :key="item.id"
                class="history-item"
              >
                <div class="history-main">
                  <div class="history-file">{{ item.sourceFileName }}</div>
                  <div class="history-meta">
                    {{ formatTimeText(item.savedAt) }} · {{ item.matchedCourses.length }} 门课程
                  </div>
                </div>
                <button class="history-view-btn" @click="restoreHistoryRecord(item)">
                  查看
                </button>
              </div>
            </div>

            <div v-else class="side-card tips-card">
              <div class="side-card-head">
                <span>使用提示</span>
              </div>
              <div class="tip-line">· 支持上传 PDF 格式课表</div>
              <div class="tip-line">· 最近分析记录默认保留 5 条</div>
              <div class="tip-line">· 重新上传不会自动清空历史</div>
            </div>
          </div>

          <!-- 右栏：分析仪表盘 -->
          <div class="visual-panel glass-panel">
            <div v-if="!analysisReady && !analyzing" class="empty-state">
              <node-index-outlined class="empty-icon" />
              <h3>等待课表上传</h3>
              <p>上传本学期课表后，将在四年完整课程体系中高亮你的当前课程。</p>
            </div>

            <div v-else-if="analyzing" class="empty-state">
              <loading-outlined class="empty-icon spin-icon" />
              <h3>正在识别中...</h3>
              <p>{{ progressText }}</p>
            </div>

            <div v-else class="dashboard-content">
              <div class="dash-tabs">
                <div
                  class="dash-tab"
                  :class="{ active: activeTab === 'graph' }"
                  @click="switchTab('graph')"
                >
                  <share-alt-outlined />
                  关系图谱
                </div>
                <div
                  class="dash-tab"
                  :class="{ active: activeTab === 'tree' }"
                  @click="switchTab('tree')"
                >
                  <apartment-outlined />
                  知识结构
                </div>
                <div
                  class="dash-tab"
                  :class="{ active: activeTab === 'insights' }"
                  @click="switchTab('insights')"
                >
                  <bulb-outlined />
                  AI 建议
                </div>
              </div>

              <!-- 关系图谱 -->
              <div v-show="activeTab === 'graph'" class="tab-body">
                <div class="graph-toolbar">
                  <div class="graph-hint">
                    <info-circle-outlined />
                    黄色节点为本学期课程，点击节点高亮关联链路
                  </div>
                  <div class="graph-controls">
                    <a-select
                      v-model:value="graphFilter"
                      placeholder="筛选类别"
                      allowClear
                      style="width: 160px"
                      size="small"
                      @change="renderGraphChart"
                    >
                      <a-select-option
                        v-for="cat in graphCategories"
                        :key="cat"
                        :value="cat"
                      >
                        {{ cat }}
                      </a-select-option>
                    </a-select>
                    <button class="reset-view-btn" @click="resetGraphView">
                      <redo-outlined />
                      重置
                    </button>
                  </div>
                </div>
                <div ref="graphChartDom" class="chart-area full-chart"></div>
              </div>

              <!-- 知识结构 -->
              <div v-show="activeTab === 'tree'" class="tab-body">
                <div ref="treeChartDom" class="chart-area full-chart"></div>
              </div>

              <!-- AI 建议 -->
              <div v-show="activeTab === 'insights'" class="tab-body insights-body">
                <template v-if="hasInsightsContent">
                  <div class="insight-section" v-if="savedInsights?.recommendations?.length">
                    <h4>
                      <rocket-outlined />
                      学业规划建议
                    </h4>
                    <div
                      v-for="(r, i) in savedInsights.recommendations"
                      :key="i"
                      class="insight-item rec-item"
                    >
                      <span class="insight-num">{{ i + 1 }}</span>
                      <span>{{ r }}</span>
                    </div>
                  </div>

                  <div class="insight-section" v-if="savedInsights?.riskWarnings?.length">
                    <h4>
                      <warning-outlined />
                      风险预警
                    </h4>
                    <div
                      v-for="(w, i) in savedInsights.riskWarnings"
                      :key="i"
                      class="insight-item warn-item"
                    >
                      <warning-outlined class="warn-icon" />
                      <span>{{ w }}</span>
                    </div>
                  </div>

                  <div class="workload-verdict" v-if="savedInsights?.workloadVerdict">
                    <bulb-outlined class="verdict-icon" />
                    <span>{{ savedInsights.workloadVerdict }}</span>
                  </div>
                </template>

                <div v-else class="empty-insights">
                  <p>当前暂无 AI 建议内容，可能是后端未返回建议字段。</p>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { getAuthToken, getLoginUserRaw } from '@/utils/authStorage'
import { ref, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  InboxOutlined,
  LoadingOutlined,
  ShareAltOutlined,
  NodeIndexOutlined,
  ApartmentOutlined,
  InfoCircleOutlined,
  RedoOutlined,
  CheckCircleOutlined,
  BulbOutlined,
  RocketOutlined,
  FileTextOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'
import { ALL_COURSES } from './curriculum'
import type { CurriculumCourse } from './curriculum'

type AnalysisInsights = {
  recommendations: string[]
  riskWarnings: string[]
  workloadVerdict: string
}

type ServerAnalysisRecord = {
  id?: number
  userId?: number
  semesterLabel?: string
  sourceFileName?: string
  extractedCourses?: string[]
  matchedCourses: string[]
  insights?: any
  status?: string
  updateTime?: string
}

type LocalHistoryRecord = {
  id: string
  sourceFileName: string
  matchedCourses: string[]
  insights: AnalysisInsights | null
  savedAt: number
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const STORAGE_KEY_PREFIX = 'course_analysis_result'
const STORAGE_HISTORY_KEY_PREFIX = 'course_analysis_history'

const analyzing = ref(false)
const progressText = ref('')
const activeTab = ref<'graph' | 'tree' | 'insights'>('graph')
const analysisReady = ref(false)
const hasSavedData = ref(false)
const historyRecords = ref<LocalHistoryRecord[]>([])

const allCourses = ref<CurriculumCourse[]>(ALL_COURSES)
const currentNames = ref<string[]>([])
const savedInsights = ref<AnalysisInsights | null>(null)
const serverSavedData = ref<ServerAnalysisRecord | null>(null)

const graphChartDom = ref<HTMLDivElement | null>(null)
const treeChartDom = ref<HTMLDivElement | null>(null)

const graphFilter = ref<string | undefined>(undefined)
const graphCategories = ref<string[]>([])

let graphChart: echarts.ECharts | null = null
let treeChart: echarts.ECharts | null = null

const readStoredLoginUser = () => {
  const candidates = [getLoginUserRaw()].filter(Boolean) as string[]

  for (const raw of candidates) {
    try {
      const user = JSON.parse(raw)
      if (user && typeof user === 'object') {
        return user
      }
    } catch {
      // ignore invalid cached user payload
    }
  }

  return null
}

const getCurrentStudentStorageScope = () => {
  const user = readStoredLoginUser()
  const id = user?.id
  const account = user?.userAccount || user?.account

  if (id !== undefined && id !== null && `${id}`.trim()) {
    return `${id}`.trim()
  }

  if (account !== undefined && account !== null && `${account}`.trim()) {
    return `${account}`.trim()
  }

  return 'anonymous'
}

const getStorageKey = () => `${STORAGE_KEY_PREFIX}:${getCurrentStudentStorageScope()}`

const getHistoryStorageKey = () => `${STORAGE_HISTORY_KEY_PREFIX}:${getCurrentStudentStorageScope()}`

const normalizeInsights = (raw: any): AnalysisInsights | null => {
  if (!raw || typeof raw !== 'object') return null

  const recommendations = Array.isArray(raw.recommendations)
    ? raw.recommendations
    : Array.isArray(raw.suggestions)
      ? raw.suggestions
      : Array.isArray(raw.advice)
        ? raw.advice
        : []

  const riskWarnings = Array.isArray(raw.riskWarnings)
    ? raw.riskWarnings
    : Array.isArray(raw.warnings)
      ? raw.warnings
      : Array.isArray(raw.risks)
        ? raw.risks
        : []

  const workloadVerdict =
    typeof raw.workloadVerdict === 'string'
      ? raw.workloadVerdict
      : typeof raw.verdict === 'string'
        ? raw.verdict
        : typeof raw.summary === 'string'
          ? raw.summary
          : ''

  if (!recommendations.length && !riskWarnings.length && !workloadVerdict) {
    return null
  }

  return {
    recommendations,
    riskWarnings,
    workloadVerdict,
  }
}

const hasInsightsContent = computed(() => {
  return !!(
    savedInsights.value &&
    (
      savedInsights.value.recommendations.length > 0 ||
      savedInsights.value.riskWarnings.length > 0 ||
      savedInsights.value.workloadVerdict
    )
  )
})

const currentSemesterCourses = computed(() => {
  return allCourses.value.filter((course) => currentNames.value.includes(course.name))
})

const currentSemesterCredits = computed(() => {
  return currentSemesterCourses.value.reduce((sum, course) => sum + course.credits, 0)
})

const currentCategoryCount = computed(() => {
  return new Set(currentSemesterCourses.value.map((course) => course.category)).size
})

const currentTopCategoryText = computed(() => {
  const categoryMap: Record<string, number> = {}

  currentSemesterCourses.value.forEach((course) => {
    categoryMap[course.category] = (categoryMap[course.category] || 0) + 1
  })

  const top = Object.entries(categoryMap).sort((a, b) => b[1] - a[1])[0]
  return top ? `${top[0]}（${top[1]}门）` : '--'
})

const latestRecord = computed(() => {
  return historyRecords.value.length > 0 ? historyRecords.value[0] : null
})

const latestRecordTimeText = computed(() => {
  return latestRecord.value ? formatTimeText(latestRecord.value.savedAt) : '--'
})

const latestRecordFileName = computed(() => {
  return latestRecord.value?.sourceFileName || '--'
})

const getCurrentSemesterLabel = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1

  if (month >= 2 && month <= 7) {
    return `${year - 1}-${year}-2`
  }
  if (month >= 8) {
    return `${year}-${year + 1}-1`
  }
  return `${year - 1}-${year}-1`
}

const saveToLocal = (names: string[], insights?: AnalysisInsights | null) => {
  localStorage.setItem(
    getStorageKey(),
    JSON.stringify({
      userScope: getCurrentStudentStorageScope(),
      currentNames: names,
      insights: insights || null,
      savedAt: Date.now(),
    }),
  )
}

const loadHistoryRecords = () => {
  try {
    const raw = localStorage.getItem(getHistoryStorageKey())
    if (!raw) {
      historyRecords.value = []
      return
    }

    const parsed = JSON.parse(raw)
    historyRecords.value = Array.isArray(parsed) ? parsed : []
  } catch (e) {
    console.warn('读取历史记录失败', e)
    historyRecords.value = []
  }
}

const saveHistoryRecords = () => {
  localStorage.setItem(getHistoryStorageKey(), JSON.stringify(historyRecords.value))
}

const pushHistoryRecord = (
  sourceFileName: string,
  matchedCourses: string[],
  insights: AnalysisInsights | null,
  savedAt = Date.now(),
) => {
  const newRecord: LocalHistoryRecord = {
    id: `${savedAt}_${sourceFileName}`,
    sourceFileName: sourceFileName || '未命名课表',
    matchedCourses,
    insights,
    savedAt,
  }

  const newSign = `${newRecord.sourceFileName}__${newRecord.matchedCourses.join('|')}`

  historyRecords.value = [
    newRecord,
    ...historyRecords.value.filter((item) => {
      const sign = `${item.sourceFileName}__${item.matchedCourses.join('|')}`
      return sign !== newSign
    }),
  ].slice(0, 5)

  saveHistoryRecords()
}

const restoreHistoryRecord = (record: LocalHistoryRecord) => {
  currentNames.value = Array.isArray(record.matchedCourses) ? record.matchedCourses : []
  savedInsights.value = record.insights || null
  analysisReady.value = true
  hasSavedData.value = true
  activeTab.value = 'graph'

  saveToLocal(currentNames.value, savedInsights.value)

  nextTick(() => {
    renderGraphChart()
  })
}

const clearHistoryRecords = () => {
  historyRecords.value = []
  localStorage.removeItem(getHistoryStorageKey())
  message.success('历史记录已清空')
}

const formatTimeText = (time?: number | string) => {
  if (!time) return '--'

  const date = typeof time === 'number' ? new Date(time) : new Date(time)
  if (Number.isNaN(date.getTime())) return '--'

  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')

  return `${y}-${m}-${d} ${hh}:${mm}`
}

const syncServerRecordToHistory = (record: ServerAnalysisRecord) => {
  if (!record || !Array.isArray(record.matchedCourses) || record.matchedCourses.length === 0) return

  pushHistoryRecord(
    record.sourceFileName || '服务器分析记录',
    record.matchedCourses,
    normalizeInsights(record.insights),
    record.updateTime ? new Date(record.updateTime).getTime() : Date.now(),
  )
}

const applyRecordToView = (record: ServerAnalysisRecord) => {
  currentNames.value = Array.isArray(record.matchedCourses) ? record.matchedCourses : []
  savedInsights.value = normalizeInsights(record.insights)
  analysisReady.value = true
  hasSavedData.value = true
  activeTab.value = 'graph'

  saveToLocal(currentNames.value, savedInsights.value)

  nextTick(() => {
    renderGraphChart()
  })
}

const clearSavedState = () => {
  localStorage.removeItem(getStorageKey())
  hasSavedData.value = false
  analysisReady.value = false
  currentNames.value = []
  savedInsights.value = null
  activeTab.value = 'graph'
}

const fetchLatestFromServer = async () => {
  const semesterLabel = encodeURIComponent(getCurrentSemesterLabel())
  const response = await fetch(
    `${API_BASE_URL}/course-analysis/latest?semesterLabel=${semesterLabel}`,
    {
      method: 'GET',
      credentials: 'include',
      headers: getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {},
    },
  )

  if (!response.ok) {
    throw new Error(`读取课表分析失败：${response.status}`)
  }

  const result = await response.json()

  if (result?.code !== 0) {
    throw new Error(result?.message || '读取课表分析失败')
  }

  const data = result?.data

  if (data && Array.isArray(data.matchedCourses) && data.matchedCourses.length > 0) {
    serverSavedData.value = data
    hasSavedData.value = true
    return data as ServerAnalysisRecord
  }

  return null
}

const saveAnalysisToServer = async (
  fileName: string,
  extractedNames: string[],
  matched: string[],
  insights: AnalysisInsights | null,
) => {
  const response = await fetch(`${API_BASE_URL}/course-analysis/save`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
    },
    credentials: 'include',
    body: JSON.stringify({
      semesterLabel: getCurrentSemesterLabel(),
      sourceFileName: fileName,
      extractedCourses: extractedNames,
      matchedCourses: matched,
      insights: insights || null,
    }),
  })

  if (!response.ok) {
    throw new Error(`保存课表分析失败：${response.status}`)
  }

  const result = await response.json()

  if (result?.code !== 0) {
    throw new Error(result?.message || '保存课表分析失败')
  }

  const data = result?.data
  if (data) {
    serverSavedData.value = data
  }

  return data as ServerAnalysisRecord | null
}

const loadSavedData = () => {
  if (
    serverSavedData.value &&
    Array.isArray(serverSavedData.value.matchedCourses) &&
    serverSavedData.value.matchedCourses.length > 0
  ) {
    applyRecordToView(serverSavedData.value)
    return
  }

  try {
    const raw = localStorage.getItem(getStorageKey())
    if (!raw) return

    const saved = JSON.parse(raw)
    currentNames.value = Array.isArray(saved.currentNames) ? saved.currentNames : []
    savedInsights.value = normalizeInsights(saved.insights)
    analysisReady.value = true
    hasSavedData.value = true
    activeTab.value = 'graph'

    nextTick(() => {
      renderGraphChart()
    })
  } catch (e) {
    console.warn('加载缓存失败', e)
  }
}

const checkSavedData = async () => {
  try {
    const serverData = await fetchLatestFromServer()
    if (serverData) {
      syncServerRecordToHistory(serverData)
      return
    }
  } catch (e) {
    console.warn('读取服务器课表分析失败，回退到本地缓存', e)
  }

  try {
    const raw = localStorage.getItem(getStorageKey())
    if (!raw) return

    const saved = JSON.parse(raw)
    const names = Array.isArray(saved.currentNames) ? saved.currentNames : []
    const notExpired =
      typeof saved.savedAt === 'number' &&
      Date.now() - saved.savedAt < 30 * 24 * 3600 * 1000

    if (names.length > 0 && notExpired) {
      hasSavedData.value = true
    }
  } catch {
    // ignore
  }
}

const matchCourses = (extracted: string[]): string[] => {
  const matched: string[] = []
  const allNames = allCourses.value.map((c) => c.name)

  extracted.forEach((name) => {
    if (!name) return

    const normalizedName = name.replace(/[()（）\s]/g, '')

    if (allNames.includes(name)) {
      if (!matched.includes(name)) matched.push(name)
      return
    }

    const found = allNames.find((n) => {
      const normalizedCourseName = n.replace(/[()（）\s]/g, '')
      return (
        n.includes(name) ||
        name.includes(n) ||
        normalizedCourseName.includes(normalizedName) ||
        normalizedName.includes(normalizedCourseName)
      )
    })

    if (found && !matched.includes(found)) {
      matched.push(found)
    }
  })

  return matched
}

const handleUpload = async (file: File) => {
  const isPdf =
    file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')

  if (!isPdf) {
    message.error('请上传 PDF 文件！')
    return false
  }

  analyzing.value = true
  analysisReady.value = false
  progressText.value = '正在提取文档内容...'

  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await fetch(`${API_BASE_URL}/ai/analyze/file`, {
      method: 'POST',
      body: formData,
      credentials: 'include',
      headers: getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {},
    })

    if (!response.ok) {
      throw new Error(`请求失败：${response.status}`)
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    if (!reader) {
      throw new Error('连接失败')
    }

    progressText.value = '正在识别课程名称...'

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
    }

    const clean = buffer.replace(/```json/g, '').replace(/```/g, '').trim()

    let extractedNames: string[] = []
    let aiInsightsRaw: any = null

    try {
      const parsed = JSON.parse(clean)
      console.log('AI分析返回结果：', parsed)
      console.log('AI建议原始字段：', parsed?.insights)

      if (Array.isArray(parsed)) {
        extractedNames = parsed.filter(Boolean)
      } else if (Array.isArray(parsed?.currentCourses)) {
        extractedNames = parsed.currentCourses.filter(Boolean)
        aiInsightsRaw = parsed.insights ?? null
      } else if (Array.isArray(parsed?.courses)) {
        extractedNames = parsed.courses
          .map((c: any) => (typeof c === 'string' ? c : c?.name))
          .filter(Boolean)
        aiInsightsRaw = parsed.insights ?? null
      } else if (Array.isArray(parsed?.courseNames)) {
        extractedNames = parsed.courseNames.filter(Boolean)
        aiInsightsRaw = parsed.insights ?? null
      } else {
        aiInsightsRaw = parsed?.insights ?? null
      }
    } catch {
      extractedNames = clean
        .split(/[,\n]/)
        .map((s) => s.replace(/["\[\]{}]/g, '').trim())
        .filter(Boolean)
    }

    progressText.value = `识别到 ${extractedNames.length} 门课程，正在匹配课程库...`

    const matched = matchCourses(extractedNames)
    const normalizedInsights = normalizeInsights(aiInsightsRaw)

    currentNames.value = matched
    savedInsights.value = normalizedInsights

    saveToLocal(matched, normalizedInsights)
    pushHistoryRecord(file.name, matched, normalizedInsights)

    try {
      await saveAnalysisToServer(file.name, extractedNames, matched, normalizedInsights)
    } catch (e) {
      console.warn('服务器保存失败，仅保留本地缓存', e)
      message.warning('分析已完成，但服务器保存失败，当前仅保留本地缓存')
    }

    analysisReady.value = true
    hasSavedData.value = true
    activeTab.value = 'graph'

    message.success(`匹配成功！${matched.length} 门本学期课程已高亮`)

    await nextTick()
    setTimeout(() => {
      renderGraphChart()
    }, 50)
  } catch (error) {
    console.error(error)
    message.error('识别失败，请重试')
  } finally {
    analyzing.value = false
  }

  return false
}

const switchTab = (tab: 'graph' | 'tree' | 'insights') => {
  activeTab.value = tab

  nextTick(() => {
    if (tab === 'graph') {
      renderGraphChart()
    } else if (tab === 'tree') {
      renderTreeChart()
    }
  })
}

const renderGraphChart = () => {
  if (!graphChartDom.value) return

  if (graphChart) {
    graphChart.dispose()
  }

  graphChart = echarts.init(graphChartDom.value)

  const courses = allCourses.value
  const curSet = new Set(currentNames.value)

  const categorySet = new Set<string>()
  courses.forEach((c) => categorySet.add(c.category))
  const categories = Array.from(categorySet)
  graphCategories.value = categories

  const catColors: Record<string, string> = {
    基础课: '#6366f1',
    专业核心: '#06b6d4',
    专业选修: '#8b5cf6',
    通识课: '#f59e0b',
    实践环节: '#10b981',
  }

  const CURRENT_COLOR = '#f59e0b'

  let nodes = courses.map((c) => {
    const isCurrent = curSet.has(c.name)

    return {
      name: c.name,
      category: categories.indexOf(c.category),
      value: c.credits,
      symbolSize: isCurrent
        ? Math.max(35, c.credits * 12)
        : Math.max(18, c.credits * 7),
      itemStyle: {
        color: isCurrent ? CURRENT_COLOR : (catColors[c.category] || '#94a3b8'),
        borderColor: isCurrent ? '#d97706' : '#fff',
        borderWidth: isCurrent ? 3 : 1.5,
        shadowBlur: isCurrent ? 12 : 0,
        shadowColor: isCurrent ? 'rgba(245,158,11,0.4)' : 'transparent',
      },
      label: {
        show: true,
        fontSize: isCurrent ? 13 : 11,
        fontWeight: isCurrent ? 700 : 400,
        color: isCurrent ? '#92400e' : '#555',
      },
      draggable: true,
      _isCurrent: isCurrent,
      _course: c,
    }
  })

  const nodeNames = new Set(courses.map((c) => c.name))
  const links: any[] = []

  courses.forEach((c) => {
    c.prerequisites.forEach((pre) => {
      if (nodeNames.has(pre)) {
        links.push({
          source: pre,
          target: c.name,
        })
      }
    })
  })

  if (graphFilter.value) {
    const keepCat = graphFilter.value
    const keepNames = new Set(
      courses.filter((c) => c.category === keepCat).map((c) => c.name),
    )

    links.forEach((l) => {
      if (keepNames.has(l.source)) keepNames.add(l.target)
      if (keepNames.has(l.target)) keepNames.add(l.source)
    })

    nodes = nodes.filter((n) => keepNames.has(n.name))
  }

  graphChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: '#e5e7eb',
      padding: [14, 18],
      textStyle: { color: '#333', fontSize: 13 },
      formatter: (p: any) => {
        if (p.dataType === 'node') {
          const c = p.data._course as CurriculumCourse
          const tag = p.data._isCurrent
            ? '<span style="background:#fef3c7;color:#92400e;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:700;margin-left:8px">本学期</span>'
            : ''

          let h = `<div style="font-size:16px;font-weight:800;margin-bottom:8px">${c.name}${tag}</div>`
          h += `<div style="color:#666;font-size:12px;margin-bottom:6px">${c.category} · ${c.credits} 学分 · 第${c.semester}学期 · ${c.type}</div>`
          h += `<div style="color:#444;font-size:13px;line-height:1.7;max-width:320px;white-space:normal;word-break:break-word;">${c.description}</div>`
          if (c.prerequisites.length > 0) {
            h += `<div style="color:#888;font-size:12px;margin-top:8px;border-top:1px solid #f0f0f0;padding-top:8px">前置课程：${c.prerequisites.join('、')}</div>`
          }

          return h
        }

        if (p.dataType === 'edge') {
          return `<span style="color:#888">${p.data.source} → ${p.data.target}</span>`
        }

        return ''
      },
    },
    legend: {
      data: categories,
      bottom: 10,
      type: 'scroll',
      icon: 'circle',
      textStyle: { fontSize: 11, color: '#666' },
      itemWidth: 12,
      itemHeight: 12,
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: nodes,
        links,
        categories: categories.map((name) => ({
          name,
          itemStyle: { color: catColors[name] || '#999' },
        })),
        roam: true,
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: [0, 7],
        lineStyle: {
          color: 'source',
          curveness: 0.25,
          width: 1.2,
          opacity: 0.5,
        },
        force: {
          repulsion: 350,
          edgeLength: [50, 200],
          gravity: 0.08,
          friction: 0.6,
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 3, opacity: 1 },
          itemStyle: { borderWidth: 4 },
          label: { fontSize: 15, fontWeight: 800 },
        },
        blur: {
          itemStyle: { opacity: 0.12 },
          lineStyle: { opacity: 0.04 },
        },
        scaleLimit: { min: 0.3, max: 3 },
      },
    ],
  })
}



const resetGraphView = () => {
  graphFilter.value = undefined
  renderGraphChart()
}

const renderTreeChart = () => {
  if (!treeChartDom.value) return

  if (treeChart) {
    treeChart.dispose()
  }

  treeChart = echarts.init(treeChartDom.value)

  const curSet = new Set(currentNames.value)
  const catMap: Record<string, any[]> = {}

  allCourses.value
    .filter((c) => curSet.has(c.name))
    .forEach((c) => {
      const cat = c.category
      if (!catMap[cat]) catMap[cat] = []

      catMap[cat].push({
        name: c.name,
        value: c.credits,
        _desc: c.description,
        _isCurrent: true,
        itemStyle: { color: '#f59e0b', borderColor: '#d97706' },
        label: { color: '#92400e', fontWeight: 700 },
      })
    })

  const treeData =
    currentNames.value.length > 0
      ? {
        name: `本学期课程 (${currentNames.value.length}门)`,
        children: Object.entries(catMap).map(([name, children]) => ({
          name: `${name} (${children.length}门)`,
          children,
        })),
      }
      : {
        name: '暂无本学期课程',
        children: [],
      }

  treeChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: '#e5e7eb',
      padding: [12, 16],
      textStyle: { color: '#333', fontSize: 13 },
      formatter: (p: any) => {
        const d = p.data

        if (d._isCurrent && d._desc) {
          return (
            `<div style="font-size:15px;font-weight:700;margin-bottom:6px">${d.name} <span style="background:#fef3c7;color:#92400e;padding:1px 6px;border-radius:3px;font-size:10px;font-weight:700">本学期</span></div>` +
            `<div style="color:#888;font-size:12px;margin-bottom:6px">${d.value} 学分</div>` +
            `<div style="color:#444;font-size:13px;line-height:1.7;max-width:300px;white-space:normal;word-break:break-word;">${d._desc}</div>`
          )
        }

        if (d.value) return `<b>${d.name}</b><br/>${d.value} 学分`
        if (d.children?.length) return `<b>${d.name}</b><br/>包含 ${d.children.length} 门课程`
        return d.name
      },
    },
    series: [
      {
        type: 'tree',
        data: [treeData],
        top: '4%',
        left: '10%',
        bottom: '4%',
        right: '22%',
        symbolSize: 9,
        symbol: 'circle',
        itemStyle: {
          color: '#6366f1',
          borderColor: '#6366f1',
          borderWidth: 1.5,
        },
        lineStyle: {
          color: '#ddd',
          width: 1.2,
          curveness: 0.4,
        },
        label: {
          position: 'left',
          verticalAlign: 'middle',
          align: 'right',
          fontSize: 12,
          color: '#555',
        },
        leaves: {
          label: {
            position: 'right',
            verticalAlign: 'middle',
            align: 'left',
          },
        },
        expandAndCollapse: true,
        initialTreeDepth: 2,
        emphasis: {
          focus: 'descendant',
          itemStyle: { borderWidth: 3 },
        },
      },
    ],
  })
}

const handleResize = () => {
  graphChart?.resize()
  treeChart?.resize()
}

onMounted(() => {
  loadHistoryRecords()
  void checkSavedData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  graphChart?.dispose()
  treeChart?.dispose()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

.edu-app {
  height: calc(100vh - 82px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: linear-gradient(120deg, #ffffff 0%, #f1f5f9 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
  color: #344054;
}

.glass-panel {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}

.analysis-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 22px;
  align-items: stretch;
  height: 100%;
}

.edu-main {
  flex: 1;
  min-height: 0;
  width: 75%;
  max-width: 1600px;
  min-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.page-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.upload-panel {
  border-radius: 5px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
}

.visual-panel {
  border-radius: 5px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.section-head h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
}

.section-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.section-kicker {
  font-size: 11px;
  font-weight: 800;
  color: #5b6cff;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 6px;
}

.saved-hint {
  padding: 18px;
  border-radius: 5px;
  background: #fefce8;
  border: 1px solid #fde68a;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.saved-hint-text {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #92400e;
}

.saved-icon {
  color: #f59e0b;
  font-size: 18px;
}

.saved-actions {
  display: flex;
  gap: 8px;
}

.saved-file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 5px;
  border: 1px solid #fde68a;
}

.saved-file-icon {
  color: #92400e;
  font-size: 16px;
  flex-shrink: 0;
}

.saved-file-main {
  min-width: 0;
  flex: 1;
}

.saved-file-name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.saved-file-meta {
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
}

.btn-load {
  flex: 1;
  height: 38px;
  border: none;
  border-radius: 5px;
  background: #111;
  color: #fff;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}

.btn-reupload {
  flex: 1;
  height: 38px;
  border: 1px solid #ddd;
  border-radius: 5px;
  background: #fff;
  color: #666;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
}

.custom-dragger {
  border-radius: 5px;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

:deep(.custom-dragger.ant-upload-wrapper) {
  height: 100%;
}

:deep(.custom-dragger.ant-upload-wrapper .ant-upload-drag) {
  border-radius: 5px;
  border: 1.5px dashed #cbd5e1;
  background: linear-gradient(180deg, #fbfdff 0%, #f7faff 100%);
  padding: 32px 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

:deep(.custom-dragger.ant-upload-wrapper .ant-upload-drag:hover) {
  border-color: #5b6cff;
  background: #f0f4ff;
}

:deep(.custom-dragger .ant-upload-drag-icon) {
  color: #5b6cff;
  font-size: 40px;
  margin-bottom: 10px;
}

:deep(.custom-dragger .ant-upload-text) {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

:deep(.custom-dragger .ant-upload-hint) {
  color: #64748b;
  font-size: 12px;
}

:deep(.custom-dragger.is-compact) {
  flex: 0 0 auto;
  height: auto;
}

:deep(.custom-dragger.is-compact .ant-upload-drag) {
  height: auto;
  padding: 14px 20px;
}

:deep(.custom-dragger.is-compact .ant-upload-drag-icon) {
  font-size: 22px;
  margin-bottom: 4px;
}

:deep(.custom-dragger.is-compact .ant-upload-text) {
  font-size: 13px;
}

:deep(.custom-dragger.is-compact .ant-upload-hint) {
  font-size: 11px;
  margin-top: 2px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.stat-card {
  padding: 12px 10px;
  border-radius: 5px;
  background: #fff;
  border: 1px solid #edf2f7;
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.stat-value.amber {
  color: #d97706;
}

.current-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.current-title {
  width: 100%;
  font-size: 12px;
  font-weight: 700;
  color: #92400e;
  margin-bottom: 2px;
}

.current-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 8px;
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

.side-card {
  border: 1px solid #edf2f7;
  border-radius: 5px;
  background: #ffffff;
  padding: 14px 14px 12px;
}

.side-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 800;
  color: #0f172a;
}

.side-badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: #111827;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.side-badge.light {
  background: #eef2ff;
  color: #4f46e5;
}

.status-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
  padding: 6px 0;
  border-top: 1px dashed #edf2f7;
}

.status-line:first-of-type {
  border-top: none;
  padding-top: 0;
}

.status-line strong {
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
}

.status-file {
  max-width: 170px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.summary-item {
  border-radius: 5px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
  padding: 10px;
}

.summary-item span {
  display: block;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.summary-item strong {
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.history-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px dashed #edf2f7;
}

.history-item:first-of-type {
  border-top: none;
  padding-top: 0;
}

.history-main {
  min-width: 0;
  flex: 1;
}

.history-file {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #94a3b8;
}

.history-view-btn {
  flex-shrink: 0;
  height: 30px;
  padding: 0 12px;
  border-radius: 5px;
  border: 1px solid #dbe4f0;
  background: #fff;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.history-view-btn:hover {
  border-color: #5b6cff;
  color: #5b6cff;
}

.mini-text-btn {
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
}

.mini-text-btn:hover {
  color: #5b6cff;
}

.tips-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tip-line {
  font-size: 12px;
  color: #64748b;
  line-height: 1.7;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.85), rgba(255, 255, 255, 0.72));
  border: 1px dashed #dbe4f0;
  padding: 40px;
}

.empty-icon {
  font-size: 52px;
  color: #b8c3d5;
  margin-bottom: 14px;
}

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-state h3 {
  margin: 0 0 8px;
  font-size: 22px;
}

.empty-state p {
  color: #64748b;
}

.dashboard-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.dash-tabs {
  display: flex;
  gap: 8px;
}

.dash-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 5px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s;
}

.dash-tab:hover {
  color: #475569;
  border-color: #93c5fd;
}

.dash-tab.active {
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 10px 20px rgba(99, 102, 241, 0.2);
}

.tab-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.chart-area {
  flex: 1;
  min-height: 280px;
}

.full-chart {
  flex: 1;
  min-height: 0;
  border-radius: 5px;
  background: #fafcff;
  border: 1px solid #edf2f7;
}

.graph-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.graph-hint {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 6px;
}

.graph-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.reset-view-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 14px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.reset-view-btn:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.insights-body {
  overflow-y: auto;
  padding: 4px;
}

.insight-section {
  margin-bottom: 20px;
}

.insight-section h4 {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #111;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 8px;
}

.insight-num {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: #f0f0f0;
  color: #333;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rec-item {
  color: #333;
  padding: 10px 14px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #eee;
}

.warn-item {
  color: #b91c1c;
  padding: 10px 14px;
  background: #fef2f2;
  border-radius: 8px;
  border: 1px solid #fecaca;
}

.warn-icon {
  font-size: 14px;
  flex-shrink: 0;
  margin-top: 2px;
}

.workload-verdict {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-radius: 10px;
  background: #f5f5f5;
  font-size: 13px;
  color: #333;
  line-height: 1.7;
}

.verdict-icon {
  font-size: 18px;
  color: #666;
  flex-shrink: 0;
}

.empty-insights {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

@media (max-width: 1100px) {
  .analysis-layout {
    grid-template-columns: 1fr;
  }

  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
