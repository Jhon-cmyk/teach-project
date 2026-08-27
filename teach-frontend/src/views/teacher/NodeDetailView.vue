<template>
  <div class="node-detail-page">
    <div class="detail-header">
      <a-button type="link" class="back-btn" @click="goBack">
        <left-outlined /> 返回图谱
      </a-button>
      <div class="node-title">
        <div class="icon-wrapper">
          <compass-outlined class="title-icon" />
        </div>
        <h1>{{ nodeDetail.name || '知识点详情' }}</h1>
        <a-tag v-if="nodeDetail.category" color="blue" class="title-tag">{{ nodeDetail.category }}</a-tag>
      </div>
    </div>

    <div class="detail-body">
      <div class="detail-sidebar">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          class="detail-menu"
        >
          <a-menu-item key="content">
            <template #icon><read-outlined /></template>
            学习内容
          </a-menu-item>
          <a-menu-item key="quiz">
            <template #icon><form-outlined /></template>
            题库
          </a-menu-item>
          <a-menu-item key="material">
            <template #icon><folder-outlined /></template>
            资料
          </a-menu-item>
          <a-menu-item key="case">
            <template #icon><snippets-outlined /></template>
            案例
          </a-menu-item>
          <a-menu-item key="stats">
            <template #icon><bar-chart-outlined /></template>
            统计
          </a-menu-item>
        </a-menu>
      </div>

      <div class="detail-content">
        <a-spin :spinning="loading" tip="加载中…">
          <div v-if="selectedKeys[0] === 'content'" class="tab-panel">
            <template v-if="!isEditingContent">
              <div class="panel-header">
                <h3><read-outlined class="header-icon" /> 学习内容</h3>
                <a-button type="primary" shape="round" @click="startEdit" style="border-radius: 5px;">
                  <edit-outlined /> 编辑内容
                </a-button>
              </div>
              <div class="meta-section">
                <div class="info-grid">
                  <div v-if="nodeDetail.description" class="info-card wide">
                    <div class="info-label">描述</div>
                    <div class="info-value">{{ nodeDetail.description }}</div>
                  </div>
                  <div v-if="nodeDetail.learnUrl" class="info-card wide">
                    <div class="info-label">学习链接</div>
                    <a class="link-value" :href="formatUrl(nodeDetail.learnUrl)" target="_blank">{{ nodeDetail.learnUrl }}</a>
                  </div>
                  <div class="info-card">
                    <div class="info-label">难度</div>
                    <div class="info-value">
                      <a-tag :color="difficultyColor(nodeDetail.difficulty)">{{ difficultyText(nodeDetail.difficulty) }}</a-tag>
                    </div>
                  </div>
                  <div class="info-card">
                    <div class="info-label">重要程度</div>
                    <div class="info-value">
                      <a-tag :color="difficultyColor(nodeDetail.importance)">{{ difficultyText(nodeDetail.importance) }}</a-tag>
                    </div>
                  </div>
                  <div class="info-card">
                    <div class="info-label">预计学时</div>
                    <div class="info-value highlight-text">{{ nodeDetail.estimatedHours ? nodeDetail.estimatedHours + ' 小时' : '未设置' }}</div>
                  </div>
                  <div class="info-card">
                    <div class="info-label">核心与重点</div>
                    <div class="info-value tag-group">
                      <a-tag :color="nodeDetail.isCore ? 'red' : 'default'">{{ nodeDetail.isCore ? '核心节点' : '非核心' }}</a-tag>
                      <a-tag :color="nodeDetail.isKeyPoint ? 'orange' : 'default'">{{ nodeDetail.isKeyPoint ? '重点' : '普通' }}</a-tag>
                    </div>
                  </div>
                </div>
              </div>

              <div class="content-section">
                <div class="section-divider">
                  <div class="divider-line"></div>
                  <div class="section-title">
                    <book-outlined class="title-icon" />
                    <span>正文详情</span>
                  </div>
                  <div class="divider-line"></div>
                </div>
                <div v-if="nodeDetail.learningContent" class="rich-content" v-html="nodeDetail.learningContent"></div>
                <div v-else class="empty-content">
                  <a-empty description="暂无学习内容，点击右上角编辑添加" />
                </div>
              </div>
            </template>

            <template v-else>
              <div class="panel-header">
                <h3><edit-outlined class="header-icon" /> 编辑学习内容</h3>
                <div class="action-group">
                  <a-button style="border-radius: 5px;" @click="cancelEdit">取消</a-button>
                  <a-button type="primary" style="border-radius: 5px;" @click="saveEdit">
                    <save-outlined /> 保存更改
                  </a-button>
                </div>
              </div>
              <div class="edit-section">
                <a-form layout="vertical">
                  <div class="info-grid edit-grid">
                    <a-form-item label="描述" class="wide">
                      <a-textarea v-model:value="editForm.description" :rows="2" placeholder="请输入节点描述" />
                    </a-form-item>
                    <a-form-item label="学习链接" class="wide">
                      <a-input v-model:value="editForm.learnUrl" placeholder="请输入学习链接 URL" />
                    </a-form-item>
                    <a-form-item label="难度">
                      <a-select v-model:value="editForm.difficulty" placeholder="请选择">
                        <a-select-option value="high">高</a-select-option>
                        <a-select-option value="medium">中</a-select-option>
                        <a-select-option value="low">低</a-select-option>
                      </a-select>
                    </a-form-item>
                    <a-form-item label="重要程度">
                      <a-select v-model:value="editForm.importance" placeholder="请选择">
                        <a-select-option value="high">高</a-select-option>
                        <a-select-option value="medium">中</a-select-option>
                        <a-select-option value="low">低</a-select-option>
                      </a-select>
                    </a-form-item>
                    <a-form-item label="预计学时">
                      <a-input-number v-model:value="editForm.estimatedHours" :min="1" style="width: 100%" placeholder="请输入预计学时" />
                    </a-form-item>
                    <a-form-item label="教学周">
                      <a-input-number v-model:value="editForm.teachingWeek" :min="1" style="width: 100%" placeholder="请输入教学周次" />
                    </a-form-item>
                    <a-form-item label="是否核心">
                      <a-switch v-model:checked="editForm.isCore" />
                    </a-form-item>
                    <a-form-item label="是否重点">
                      <a-switch v-model:checked="editForm.isKeyPoint" />
                    </a-form-item>
                  </div>
                  <a-form-item label="学习内容正文" style="margin-top: 16px;">
                    <div class="editor-wrapper">
                      <Toolbar :editor="editorRef" :defaultConfig="toolbarConfig" class="editor-toolbar" />
                      <Editor v-model="editForm.learningContent" :defaultConfig="editorConfig" class="editor-main" @onCreated="handleEditorCreated" />
                    </div>
                  </a-form-item>
                </a-form>
              </div>
            </template>
          </div>

          <div v-if="selectedKeys[0] === 'quiz'" class="tab-panel submodule-panel">
            <div class="panel-header">
              <h3><form-outlined class="header-icon" /> 关联题库</h3>
            </div>
            <div v-if="quizzes.length === 0" class="empty-panel">
              <a-empty description="暂无关联习题" />
            </div>
            <div v-else class="list-container">
              <div v-for="quiz in quizzes" :key="quiz.id + '-' + quiz.source" class="list-card">
                <div class="list-card-content">
                  <div class="list-card-header">
                    <span class="list-card-title">{{ quiz.title }}</span>
                    <div class="list-card-tags">
                      <a-tag :color="quiz.source === 'coding' ? 'blue' : 'purple'">{{ quiz.sourceText }}</a-tag>
                      <a-tag v-if="quiz.difficulty" size="small" :color="quizDifficultyColor(quiz.difficulty)">
                        {{ quiz.difficulty }}
                      </a-tag>
                    </div>
                  </div>
                  <div v-if="quiz.content" class="list-card-desc">
                    {{ String(quiz.content).substring(0, 120) }}{{ String(quiz.content).length > 120 ? '...' : '' }}
                  </div>
                </div>
                <div class="list-card-action">
                  <a-button type="primary" ghost style="border-radius: 5px;" size="small" @click="openPreview(quiz)">
                    <eye-outlined /> 预览
                  </a-button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedKeys[0] === 'material'" class="tab-panel submodule-panel">
            <div class="panel-header">
              <h3><folder-outlined class="header-icon" /> 关联资料</h3>
            </div>
            <div v-if="materials.length === 0" class="empty-panel">
              <a-empty description="暂无关联资料" />
            </div>
            <div v-else class="list-container">
              <div v-for="item in materials" :key="item.id + '-' + item.type" class="list-card">
                <div class="list-card-content">
                  <div class="list-card-header">
                    <span class="list-card-title">{{ item.title }}</span>
                    <div class="list-card-tags">
                      <a-tag :color="item.type === 'anim' ? 'cyan' : 'geekblue'">{{ item.typeText }}</a-tag>
                    </div>
                  </div>
                  <div v-if="item.content" class="list-card-desc">
                    {{ item.type === 'anim' ? '交互课件资源' : String(item.content).substring(0, 120) + (String(item.content).length > 120 ? '...' : '') }}
                  </div>
                </div>
                <div class="list-card-action">
                  <a-button type="primary" ghost style="border-radius: 5px;" size="small" @click="openPreview(item)">
                    <eye-outlined /> 预览
                  </a-button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedKeys[0] === 'case'" class="tab-panel submodule-panel">
            <div class="panel-header">
              <h3><snippets-outlined class="header-icon" /> 相关案例</h3>
            </div>
            <div v-if="cases.length === 0" class="empty-panel">
              <a-empty description="暂无相关案例" />
            </div>
            <div v-else class="list-container">
              <div v-for="item in cases" :key="item.id" class="list-card">
                <div class="list-card-content">
                  <div class="list-card-header">
                    <span class="list-card-title">{{ item.title }}</span>
                    <div class="list-card-tags">
                      <a-tag :color="caseCategoryColor(item.category)">{{ caseCategoryLabel(item.category) }}</a-tag>
                      <a-tag :color="caseDifficultyColor(item.difficulty)">{{ caseDifficultyLabel(item.difficulty) }}</a-tag>
                    </div>
                  </div>
                  <div v-if="item.courseName" class="list-card-desc">
                    适用课程：{{ item.courseName }}
                  </div>
                </div>
                <div class="list-card-action">
                  <a-button type="primary" ghost style="border-radius: 5px;" size="small" @click="previewCase(item)">
                    <eye-outlined /> 预览
                  </a-button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedKeys[0] === 'stats'" class="tab-panel submodule-panel">
            <div class="panel-header">
              <h3><bar-chart-outlined class="header-icon" /> 统计与分析</h3>
            </div>
            <div v-if="!analysisFocus" class="empty-panel">
              <a-empty description="暂无统计数据" />
            </div>
            <div v-else class="stats-body">
              <div class="stats-overview">
                <div class="overview-card">
                  <div class="overview-icon coding"><code-outlined /></div>
                  <div class="overview-data">
                    <div class="overview-value">{{ analysisFocus.codingProblemCount ?? 0 }}</div>
                    <div class="overview-label">编程题</div>
                  </div>
                </div>
                <div class="overview-card">
                  <div class="overview-icon quiz"><form-outlined /></div>
                  <div class="overview-data">
                    <div class="overview-value">{{ analysisFocus.quizCount ?? 0 }}</div>
                    <div class="overview-label">随堂测验</div>
                  </div>
                </div>
                <div class="overview-card">
                  <div class="overview-icon plan"><file-text-outlined /></div>
                  <div class="overview-data">
                    <div class="overview-value">{{ analysisFocus.planCount ?? 0 }}</div>
                    <div class="overview-label">教案</div>
                  </div>
                </div>
                <div class="overview-card">
                  <div class="overview-icon anim"><play-circle-outlined /></div>
                  <div class="overview-data">
                    <div class="overview-value">{{ analysisFocus.animCount ?? 0 }}</div>
                    <div class="overview-label">交互课件</div>
                  </div>
                </div>
                <div class="overview-card">
                  <div class="overview-icon complete"><percentage-outlined /></div>
                  <div class="overview-data">
                    <div class="overview-value">{{ analysisFocus.contentCompleteness ?? 0 }}%</div>
                    <div class="overview-label">内容完善度</div>
                  </div>
                </div>
              </div>

              <div class="analysis-summary-box">
                <div class="summary-icon"><bulb-outlined /></div>
                <div class="summary-content">
                  <div class="summary-title">AI 分析总结</div>
                  <div class="summary-text">{{ analysisFocus.summary }}</div>
                </div>
              </div>

              <div class="stats-two-col">
                <div class="stats-col">
                  <h4 class="col-title">详细指标</h4>
                  <div class="metric-list">
                    <div v-for="metric in analysisFocus.metricItems || []" :key="metric.key" class="metric-item">
                      <div class="metric-info">
                        <div class="metric-label">{{ metric.label }}</div>
                        <div class="metric-desc">{{ metric.description }}</div>
                      </div>
                      <div class="metric-value" :class="metric.key">{{ metric.value }}</div>
                    </div>
                  </div>
                </div>

                <div class="stats-col">
                  <h4 class="col-title">系统评级</h4>
                  <div class="level-list">
                    <div class="level-item">
                      <span class="level-label">热度等级</span>
                      <a-tag class="level-tag" :color="levelColor(analysisFocus.heatLevel)">{{ levelText(analysisFocus.heatLevel) }}</a-tag>
                    </div>
                    <div class="level-item">
                      <span class="level-label">薄弱等级</span>
                      <a-tag class="level-tag" :color="levelColor(analysisFocus.weaknessLevel)">{{ levelText(analysisFocus.weaknessLevel) }}</a-tag>
                    </div>
                    <div class="level-item">
                      <span class="level-label">风险等级</span>
                      <a-tag class="level-tag" :color="levelColor(analysisFocus.riskLevel)">{{ levelText(analysisFocus.riskLevel) }}</a-tag>
                    </div>
                  </div>
                </div>
              </div>

              <div class="stats-extra-grid">
                <div class="extra-card">
                  <div class="extra-title"><eye-outlined class="title-ico" /> 推荐视角</div>
                  <ul class="extra-list">
                    <li v-for="(view, idx) in analysisFocus.recommendedViews || []" :key="idx">{{ view }}</li>
                  </ul>
                  <a-empty v-if="!(analysisFocus.recommendedViews?.length)" description="暂无推荐视角" style="margin: 16px 0;" />
                </div>
                <div class="extra-card">
                  <div class="extra-title"><rocket-outlined class="title-ico" /> 建议行动</div>
                  <ul class="extra-list">
                    <li v-for="(action, idx) in analysisFocus.suggestedActions || []" :key="idx">{{ action }}</li>
                  </ul>
                  <a-empty v-if="!(analysisFocus.suggestedActions?.length)" description="暂无建议行动" style="margin: 16px 0;" />
                </div>
              </div>

            </div>
          </div>
        </a-spin>
      </div>
    </div>

    <a-modal
      v-model:open="previewVisible"
      :title="previewData?.title"
      width="900px"
      :footer="null"
      centered
      class="preview-modal"
      @cancel="closePreview"
    >
      <a-spin :spinning="previewLoading" tip="加载中…">
        <div v-if="previewType === 'coding' && previewData" class="preview-body">
          <div class="preview-meta">
            <a-tag v-if="previewData.difficulty" :color="quizDifficultyColor(previewData.difficulty)">
              {{ previewData.difficulty }}
            </a-tag>
            <span v-if="previewData.languages" class="preview-languages">
              支持语言: {{ previewData.languages.join('、') }}
            </span>
          </div>
          <div class="preview-content markdown-render doc-style" v-html="renderMd(previewData.description || '')"></div>
          <div v-if="previewData.sampleTestCases?.length" class="preview-section">
            <div class="preview-section-title">样例用例</div>
            <div v-for="(tc, idx) in previewData.sampleTestCases" :key="idx" class="test-case-card">
              <div class="test-case-header">样例 {{ idx + 1 }}</div>
              <div class="test-case-body">
                <div class="test-case-row">
                  <div class="test-case-label">输入</div>
                  <pre class="test-case-value">{{ tc.input }}</pre>
                </div>
                <div class="test-case-row">
                  <div class="test-case-label">输出</div>
                  <pre class="test-case-value">{{ tc.expectedOutput }}</pre>
                </div>
              </div>
            </div>
          </div>
          <div v-if="previewData.templates?.length" class="preview-section">
            <div class="preview-section-title">代码模板</div>
            <div v-for="tpl in previewData.templates" :key="tpl.language" class="code-template-card">
              <div class="code-template-lang">{{ tpl.language }}</div>
              <pre class="code-template-code"><code>{{ tpl.starterCode }}</code></pre>
            </div>
          </div>
        </div>

        <div v-else-if="previewType === 'quiz' && previewData" class="preview-body">
          <div class="preview-content markdown-render doc-style" v-html="renderMd(previewData.content || '')"></div>
        </div>
        <div v-else-if="previewType === 'plan' && previewData" class="preview-body">
          <div class="preview-content markdown-render doc-style" v-html="renderMd(previewData.content || '')"></div>
        </div>
        <div v-else-if="previewType === 'anim' && previewData" class="preview-body">
          <div v-if="animPreviewPayload" class="anim-preview-box">
            <AnimationWorkbench
              :payload="animPreviewPayload"
              render-status="ready"
              :validation-errors="[]"
              :is-generating="false"
              :is-optimizing="false"
              :autoplay-delay="1800"
              :preview-mode="true"
            />
          </div>
          <div v-else class="anim-parse-error">
            <p>课件数据解析失败，无法预览。</p>
          </div>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import {
  LeftOutlined,
  CompassOutlined,
  ReadOutlined,
  FormOutlined,
  FolderOutlined,
  SnippetsOutlined,
  BarChartOutlined,
  EditOutlined,
  SaveOutlined,
  BookOutlined,
  EyeOutlined,
  BulbOutlined,
  CodeOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  PercentageOutlined,
  RocketOutlined
} from '@ant-design/icons-vue'
import { computed, onMounted, onUnmounted, reactive, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchNodeDetail,
  listNodeActivities,
  fetchNodeAnalysisFocus,
  updateNode,
  fetchNodeQuizzes,
  fetchNodeMaterials,
} from '@/api/courseGraph'
import { getTeachingCaseList } from '@/api/case'
import { getTeacherProblemDetail } from '@/api/coding'
import MarkdownIt from 'markdown-it'
import AnimationWorkbench from '@/components/anim-player/AnimationWorkbench.vue'
import '@wangeditor/editor/dist/css/style.css'
// @ts-ignore
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const route = useRoute()
const router = useRouter()
const nodeId = String(route.params.id)

const loading = ref(false)
const selectedKeys = ref<string[]>(['content'])
const isEditingContent = ref(false)

const nodeDetail = ref<any>({})
const activities = ref<any[]>([])
const cases = ref<any[]>([])
const analysisFocus = ref<any>(null)
const quizzes = ref<any[]>([])
const materials = ref<any[]>([])

const editForm = reactive({
  description: '',
  learnUrl: '',
  difficulty: undefined as string | undefined,
  importance: undefined as string | undefined,
  estimatedHours: undefined as number | undefined,
  teachingWeek: undefined as number | undefined,
  isCore: false,
  isKeyPoint: false,
  learningContent: '',
})

// ====== WangEditor ======
const editorRef = shallowRef()
const toolbarConfig = { excludeKeys: ['fullScreen'] }
const editorConfig = { placeholder: '请输入学习内容正文…' }
const handleEditorCreated = (editor: any) => {
  editorRef.value = editor
}

// ====== Markdown ======
const md = new MarkdownIt({ html: true, breaks: true })
const renderMd = (text: string): string => md.render(text || '')

// ====== 题库预览 ======
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewType = ref<'coding' | 'quiz' | 'plan' | 'anim'>('coding')
const previewData = ref<any>(null)

const animPreviewPayload = computed(() => {
  if (previewType.value !== 'anim' || !previewData.value?.content) return null
  try {
    return JSON.parse(previewData.value.content)
  } catch {
    return null
  }
})

const goBack = () => {
  router.push('/teacher/graph')
}

const formatUrl = (url: string): string => {
  if (!url) return ''
  const u = url.trim()
  if (u.startsWith('http://') || u.startsWith('https://')) return u
  return 'http://' + u
}

const difficultyColor = (d?: string): string => {
  if (!d) return 'default'
  if (d === 'high') return 'red'
  if (d === 'medium') return 'orange'
  return 'green'
}

const difficultyText = (d?: string): string => {
  if (!d) return '未设置'
  if (d === 'high') return '高'
  if (d === 'medium') return '中'
  return '低'
}

const quizDifficultyColor = (d?: string): string => {
  if (!d) return 'default'
  if (d === 'hard') return 'red'
  if (d === 'medium') return 'orange'
  return 'green'
}

const levelColor = (l?: string): string => {
  if (l === 'high') return 'red'
  if (l === 'medium') return 'orange'
  return 'green'
}

const levelText = (l?: string): string => {
  if (l === 'high') return '高'
  if (l === 'medium') return '中'
  return '低'
}

const loadAll = async () => {
  loading.value = true
  try {
    const [detail, acts, analysis] = await Promise.all([
      fetchNodeDetail(nodeId).catch(() => null),
      listNodeActivities(nodeId).catch(() => []),
      fetchNodeAnalysisFocus(nodeId).catch(() => null),
    ])
    nodeDetail.value = detail ?? {}
    activities.value = Array.isArray(acts) ? acts : []
    analysisFocus.value = analysis
  } catch {
    message.error('加载知识点详情失败')
  } finally {
    loading.value = false
  }
}

const loadQuizzes = async () => {
  try {
    const data = await fetchNodeQuizzes(nodeId)
    quizzes.value = Array.isArray(data) ? data : []
  } catch {
    quizzes.value = []
  }
}

const loadMaterials = async () => {
  try {
    const data = await fetchNodeMaterials(nodeId)
    materials.value = Array.isArray(data) ? data : []
  } catch {
    materials.value = []
  }
}

// ====== 案例 ======
const loadCases = async () => {
  try {
    const keyword = nodeDetail.value?.name || ''
    const data = await getTeachingCaseList({ keyword })
    cases.value = Array.isArray(data) ? data : []
  } catch {
    cases.value = []
  }
}

const caseCategoryLabel = (cat: string): string => {
  const map: Record<string, string> = {
    course_design: '课程设计',
    enterprise: '企业实际工程',
    competition: '大赛资源',
    small_project: '小项目',
  }
  return map[cat] || cat
}

const caseCategoryColor = (cat: string): string => {
  const map: Record<string, string> = {
    course_design: 'blue',
    enterprise: 'purple',
    competition: 'orange',
    small_project: 'cyan',
  }
  return map[cat] || 'default'
}

const caseDifficultyLabel = (d: string): string => {
  const map: Record<string, string> = {
    easy: '初级',
    medium: '中等',
    hard: '困难',
  }
  return map[d] || d
}

const caseDifficultyColor = (d: string): string => {
  const map: Record<string, string> = {
    easy: 'green',
    medium: 'orange',
    hard: 'red',
  }
  return map[d] || 'default'
}

const previewCase = (item: any) => {
  if (item.id) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
    window.open(`${baseUrl}/teaching-case/preview/${item.id}`, '_blank')
  }
}

const startEdit = () => {
  editForm.description = nodeDetail.value.description || ''
  editForm.learnUrl = nodeDetail.value.learnUrl || ''
  editForm.difficulty = nodeDetail.value.difficulty
  editForm.importance = nodeDetail.value.importance
  editForm.estimatedHours = nodeDetail.value.estimatedHours
  editForm.teachingWeek = nodeDetail.value.teachingWeek
  editForm.isCore = !!nodeDetail.value.isCore
  editForm.isKeyPoint = !!nodeDetail.value.isKeyPoint
  editForm.learningContent = nodeDetail.value.learningContent || ''
  isEditingContent.value = true
}

const cancelEdit = () => {
  isEditingContent.value = false
  editorRef.value = null
}

const saveEdit = async () => {
  try {
    await updateNode({
      id: nodeId,
      description: editForm.description,
      learnUrl: editForm.learnUrl,
      difficulty: editForm.difficulty,
      importance: editForm.importance,
      estimatedHours: editForm.estimatedHours,
      teachingWeek: editForm.teachingWeek,
      isCore: editForm.isCore,
      isKeyPoint: editForm.isKeyPoint,
      learningContent: editForm.learningContent,
    })
    message.success('保存成功')
    isEditingContent.value = false
    editorRef.value = null
    await loadAll()
  } catch {
    message.error('保存失败')
  }
}

const openPreview = async (item: any) => {
  previewType.value = item.source || item.type
  previewData.value = item
  previewVisible.value = true
  previewLoading.value = true

  if (item.source === 'coding') {
    try {
      const detail = await getTeacherProblemDetail({ problemId: item.id })
      previewData.value = { ...item, ...detail }
    } catch {
      message.error('加载编程题详情失败')
    }
  }

  previewLoading.value = false
}

const closePreview = () => {
  previewVisible.value = false
  previewData.value = null
}

onMounted(async () => {
  if (!nodeId || nodeId === 'undefined') {
    message.error('节点 ID 无效')
    router.push('/teacher/graph')
    return
  }
  await loadAll()
  loadCases()
  loadQuizzes()
  loadMaterials()
})

onUnmounted(() => {
  if (editorRef.value) {
    editorRef.value.destroy()
    editorRef.value = null
  }
})
</script>

<style scoped>
/* 全局变量和基础配置 */
.node-detail-page {
  --primary-color: #1677ff;
  --bg-main: #f4f7fb;
  --bg-card: #ffffff;
  --text-main: #0f172a;
  --text-regular: #334155;
  --text-light: #64748b;
  --border-color: #e2e8f0;
  --border-light: #f1f5f9;
  --radius-all: 5px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.03);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.05);
  --shadow-hover: 0 8px 24px rgba(0, 0, 0, 0.08);

  display: flex;
  flex-direction: column;

  /* 【关键修改 1】：去掉绝对定位，改为占满父容器高度 */
  height: 100%;
  /* 备注：如果 height: 100% 依然无效（取决于你外层 Layout 的写法），
     可以尝试解开下面这行注释，并将 60px 改为你实际顶栏的高度 */
  /* height: calc(100vh - 60px); */

  background: var(--bg-main);
  overflow: hidden; /* 切断该层级的全局滚动 */
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* ============== Header ============== */
.detail-header {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  z-index: 10;
  flex-shrink: 0; /* 保证头部不被挤压 */
}

.back-btn {
  font-weight: 500;
  color: var(--text-regular);
  padding: 0 16px 0 0;
}
.back-btn:hover { color: var(--primary-color); }

.node-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  border-left: 1px solid var(--border-color);
  padding-left: 20px;
}

.icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #e6f4ff;
  border-radius: var(--radius-all);
}

.title-icon {
  color: var(--primary-color);
  font-size: 18px;
}

.node-title h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}
.title-tag { margin-left: 8px; border-radius: var(--radius-all); }


/* ============== Body Layout ============== */
.detail-body {
  display: flex;
  flex: 1;
  overflow: hidden;
  /* 【关键修改 2】：必须保留，打破 flex 子项被超长内容无限撑高的默认规则 */
  min-height: 0;
}

.detail-sidebar {
  width: 220px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  padding: 16px 8px;
  flex-shrink: 0;
  overflow-y: auto; /* 侧边栏内容过多时局部滚动 */
}

.detail-menu {
  border-right: none;
  background: transparent;
}
.detail-menu :deep(.ant-menu-item) {
  border-radius: var(--radius-all);
  margin-bottom: 4px;
  font-weight: 500;
}
.detail-menu :deep(.ant-menu-item-selected) {
  background-color: #e6f4ff;
  color: var(--primary-color);
}

.detail-content {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto; /* 仅在此处允许垂直局部滚动 */
  overflow-x: hidden;
  box-sizing: border-box;
  /* 【关键修改 3】：必须保留，限制内容区的基础尺寸 */
  min-height: 0;
}

.tab-panel {
  animation: fadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  max-width: 1200px;
  margin: 0 auto;
}

.submodule-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-all);
  padding: 28px 32px 32px;
  box-shadow: var(--shadow-sm);
  box-sizing: border-box;
}

.submodule-panel .panel-header {
  padding-bottom: 18px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--border-light);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============== Panel Header ============== */
.panel-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  color: var(--primary-color);
  font-size: 22px;
}

.action-group {
  display: flex;
  gap: 12px;
}

/* ============== Info Grid (学习内容元数据) ============== */
.meta-section {
  background: var(--bg-card);
  border-radius: var(--radius-all);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  margin-bottom: 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.info-card {
  background: var(--bg-main);
  border-radius: var(--radius-all);
  padding: 16px 20px;
  border-left: 3px solid transparent;
  transition: all 0.3s ease;
}

.info-card:hover {
  background: var(--bg-card);
  border-left-color: var(--primary-color);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.info-card.wide {
  grid-column: 1 / -1;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-light);
  margin-bottom: 6px;
}

.info-value {
  font-size: 15px;
  color: var(--text-main);
  font-weight: 500;
  line-height: 1.5;
  word-break: break-word;
}

.highlight-text {
  color: var(--primary-color);
  font-weight: 600;
  font-size: 16px;
}

.tag-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tag-group :deep(.ant-tag) {
  border-radius: var(--radius-all);
}

.link-value {
  color: var(--primary-color);
  font-weight: 500;
  text-decoration: none;
  display: inline-block;
  transition: opacity 0.2s;
}
.link-value:hover { opacity: 0.8; text-decoration: underline; }

/* 编辑表单优化 */
.edit-section {
  background: var(--bg-card);
  border-radius: var(--radius-all);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}
.edit-grid {
  margin-bottom: 16px;
}
.edit-grid :deep(.ant-form-item) {
  margin-bottom: 0;
}
.edit-grid :deep(.ant-input),
.edit-grid :deep(.ant-select-selector),
.edit-grid :deep(.ant-input-number) {
  border-radius: var(--radius-all);
}


/* ============== 富文本区域 ============== */
.content-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-all);
  padding: 28px 36px 36px;
  box-shadow: var(--shadow-sm);
  box-sizing: border-box;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 0 0 24px;
}
.divider-line {
  flex: 1;
  height: 1px;
  background: var(--border-color);
}
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-light);
}

.rich-content {
  background: var(--bg-card);
  border-radius: var(--radius-all);
  padding: 40px 48px;
  min-height: 400px;
  line-height: 1.8;
  color: var(--text-regular);
  font-size: 15px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

/* Markdown 排版美化 */
.rich-content :deep(h2) { font-size: 24px; font-weight: 700; color: var(--text-main); margin: 32px 0 16px; padding-bottom: 8px; border-bottom: 1px solid var(--border-light); }
.rich-content :deep(h3) { font-size: 18px; font-weight: 700; color: var(--text-main); margin: 24px 0 12px; }
.rich-content :deep(p) { margin-bottom: 16px; }
.rich-content :deep(blockquote) { margin: 20px 0; padding: 16px 20px; background: #f0f7ff; border-left: 4px solid var(--primary-color); border-radius: var(--radius-all); color: #1e40af; }
.rich-content :deep(pre) { background: #1e293b; color: #f8fafc; padding: 20px; border-radius: var(--radius-all); overflow-x: auto; font-size: 14px; box-shadow: inset 0 2px 4px rgba(0,0,0,0.1); }
.rich-content :deep(code) { font-family: 'Fira Code', Consolas, monospace; background: var(--bg-main); color: #d946ef; padding: 2px 6px; border-radius: var(--radius-all); font-size: 13px; }
.rich-content :deep(pre code) { background: transparent; color: inherit; padding: 0; }
.rich-content :deep(table) { width: 100%; border-collapse: collapse; margin: 20px 0; border-radius: var(--radius-all); overflow: hidden; border: 1px solid var(--border-color); }
.rich-content :deep(th) { background: var(--bg-main); padding: 12px 16px; text-align: left; font-weight: 600; border-bottom: 1px solid var(--border-color); }
.rich-content :deep(td) { padding: 12px 16px; border-bottom: 1px solid var(--border-light); }


/* ============== 通用列表卡片 (题库, 资料) ============== */
.list-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-all);
  padding: 20px 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.list-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.list-card-content {
  flex: 1;
  padding-right: 24px;
}

.list-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.list-card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

.list-card-tags {
  display: flex;
  gap: 6px;
}
.list-card-tags :deep(.ant-tag) {
  border-radius: var(--radius-all);
}

.list-card-desc {
  font-size: 14px;
  color: var(--text-light);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.list-card-action {
  flex-shrink: 0;
}


/* ============== 统计面板 ============== */
.stats-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 概览数据列 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.overview-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-all);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
}
.overview-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.overview-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-all);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.overview-icon.coding { background: #e0f2fe; color: #0284c7; }
.overview-icon.quiz { background: #fef3c7; color: #d97706; }
.overview-icon.plan { background: #dcfce7; color: #16a34a; }
.overview-icon.anim { background: #f3e8ff; color: #9333ea; }
.overview-icon.complete { background: #ffe4e6; color: #e11d48; }

.overview-data { flex: 1; }
.overview-value { font-size: 24px; font-weight: 700; color: var(--text-main); line-height: 1.2; }
.overview-label { font-size: 13px; color: var(--text-light); margin-top: 4px; }

/* 分析总结块 */
.analysis-summary-box {
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f4ff 100%);
  border: 1px solid #bae0ff;
  border-radius: var(--radius-all);
  padding: 20px 24px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.summary-icon {
  font-size: 24px;
  color: var(--primary-color);
  background: #fff;
  width: 40px; height: 40px;
  border-radius: var(--radius-all); /* 统一要求5px圆角，这里舍弃纯圆 */
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}
.summary-title {
  font-size: 16px; font-weight: 700; color: #003eb3; margin-bottom: 8px;
}
.summary-text {
  font-size: 14px; color: var(--text-regular); line-height: 1.6;
}

/* 双栏布局 (指标 & 评级) */
.stats-two-col {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.stats-col {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-all);
  padding: 24px;
}
.col-title {
  font-size: 16px; font-weight: 600; color: var(--text-main); margin: 0 0 16px 0; padding-bottom: 12px; border-bottom: 1px solid var(--border-light);
}

.metric-list {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px;
}
.metric-item {
  background: var(--bg-main); border-radius: var(--radius-all); padding: 16px; display: flex; justify-content: space-between; align-items: center;
}
.metric-label { font-size: 14px; font-weight: 600; color: var(--text-regular); }
.metric-desc { font-size: 12px; color: var(--text-light); margin-top: 4px; }
.metric-value { font-size: 22px; font-weight: 700; }
.metric-value.heat { color: #ef4444; }
.metric-value.weakness { color: #f59e0b; }
.metric-value.quizCoverage { color: #10b981; }
.metric-value.materialCoverage { color: #0ea5e9; }

.level-list {
  display: flex; flex-direction: column; gap: 16px;
}
.level-item {
  display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: var(--bg-main); border-radius: var(--radius-all);
}
.level-label { font-size: 14px; font-weight: 500; color: var(--text-regular); }
.level-tag { margin: 0; padding: 2px 12px; font-size: 13px; border-radius: var(--radius-all); }

/* 底部附加建议 */
.stats-extra-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 24px;
}
.extra-card {
  background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-all); padding: 24px;
}
.extra-title {
  font-size: 16px; font-weight: 600; color: var(--text-main); margin-bottom: 16px; display: flex; align-items: center; gap: 8px;
}
.title-ico { color: var(--primary-color); }
.extra-list {
  margin: 0; padding-left: 20px; color: var(--text-regular); font-size: 14px; line-height: 1.8;
}
.extra-list li { margin-bottom: 8px; }
.extra-list li::marker { color: var(--primary-color); }


/* ============== WangEditor ============== */
.editor-wrapper {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-all);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.editor-toolbar { border-bottom: 1px solid var(--border-color); }
.editor-main { height: 500px; overflow-y: hidden; }

.empty-panel { padding: 64px 0; }

/* ===== 内容预览 ===== */
.preview-content { max-height: 60vh; overflow-y: auto; padding-right: 16px; }

@media (max-width: 1100px) {
  .detail-content {
    padding: 18px;
  }

  .submodule-panel {
    padding: 22px;
  }

  .content-section {
    padding: 24px;
  }

  .stats-two-col,
  .stats-extra-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .detail-body {
    flex-direction: column;
  }

  .detail-sidebar {
    width: 100%;
    padding: 8px;
    border-right: 0;
    border-bottom: 1px solid var(--border-color);
  }

  .submodule-panel {
    padding: 18px;
  }

  .content-section {
    padding: 18px;
  }

  .list-card,
  .analysis-summary-box,
  .metric-item,
  .level-item {
    align-items: flex-start;
  }

  .list-card {
    flex-direction: column;
    gap: 14px;
  }

  .list-card-content {
    width: 100%;
    padding-right: 0;
  }

  .list-card-action {
    align-self: flex-start;
  }

  .metric-list {
    grid-template-columns: 1fr;
  }
}
</style>
