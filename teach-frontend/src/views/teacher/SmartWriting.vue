<template>
  <div class="smart-writing-page">
    <header class="page-header">
      <div>
        <h2><EditOutlined /> 智能编写</h2>
        <p>面向教案、反思、课程思政和实验材料的教研写作助手。</p>
      </div>
      <div class="header-actions">
        <a-button @click="openWritingLibrary">
          <HistoryOutlined /> 我的文档
        </a-button>
        <a-tag color="blue">{{ currentMaterial.name }}</a-tag>
        <a-button v-if="generating" danger @click="stopGenerate">停止生成</a-button>
      </div>
    </header>

    <div class="writing-workspace">
      <aside class="config-panel panel">
        <section class="material-switcher">
          <div class="material-switcher-head">
            <span>材料类型</span>
            <small>{{ currentMaterial.focus }}</small>
          </div>
          <div class="material-strip">
            <button
              v-for="item in materialTypes"
              :key="item.key"
              class="material-chip"
              :class="{ active: selectedType === item.key }"
              @click="selectMaterial(item.key)"
            >
              <component :is="item.icon" />
              <span>{{ item.name }}</span>
            </button>
          </div>
        </section>

        <a-tabs v-model:activeKey="activeConfigTab" class="config-tabs" size="small">
          <a-tab-pane key="basic" tab="基础信息">
            <section class="panel-section compact-section">
              <div class="section-heading">
                <span>基础信息</span>
                <small>决定生成内容的边界</small>
              </div>
              <a-form layout="vertical" class="compact-form">
                <a-form-item label="材料主题" required>
                  <a-input
                    v-model:value="form.title"
                    :placeholder="currentMaterial.exampleTitle"
                    size="large"
                  />
                </a-form-item>
                <div class="form-row">
                  <a-form-item :label="currentFieldConfig.contextLabel">
                    <a-input
                      v-model:value="form.contextInfo"
                      :placeholder="currentFieldConfig.contextPlaceholder"
                      size="large"
                    />
                  </a-form-item>
                  <a-form-item :label="currentFieldConfig.rangeLabel">
                    <a-input
                      v-model:value="form.rangeInfo"
                      :placeholder="currentFieldConfig.rangePlaceholder"
                      size="large"
                    />
                  </a-form-item>
                </div>
                <a-form-item :label="currentFieldConfig.primaryLabel">
                  <a-textarea
                    v-model:value="form.primaryInfo"
                    :rows="3"
                    :placeholder="currentFieldConfig.primaryPlaceholder"
                  />
                </a-form-item>
                <a-form-item :label="currentFieldConfig.secondaryLabel">
                  <a-textarea
                    v-model:value="form.secondaryInfo"
                    :rows="3"
                    :placeholder="currentFieldConfig.secondaryPlaceholder"
                  />
                </a-form-item>
              </a-form>
            </section>
          </a-tab-pane>

          <a-tab-pane key="outline" tab="目录结构">
            <section class="panel-section compact-section outline-section">
              <div class="section-heading outline-heading">
                <div>
                  <span>目录结构</span>
                  <small>点击章节即可启用或取消</small>
                </div>
                <em>{{ selectedOutlines.length }}/{{ allOutlineTags.length }}</em>
              </div>
              <div class="outline-list">
                <button
                  v-for="(tag, index) in allOutlineTags"
                  :key="tag"
                  class="outline-item"
                  :class="{ active: selectedOutlines.includes(tag) }"
                  @click="toggleOutline(tag)"
                >
                  <span class="outline-index">{{ String(index + 1).padStart(2, '0') }}</span>
                  <span class="outline-name">{{ tag }}</span>
                </button>
                <input
                  v-show="addingOutline"
                  v-model="newOutline"
                  class="outline-input outline-add-input"
                  placeholder="回车确认"
                  @keyup.enter="finishAddOutline"
                  @blur="finishAddOutline"
                />
                <button v-show="!addingOutline" class="outline-add-row" @click="startAddOutline">
                  <PlusOutlined />
                  <span>添加目录项</span>
                </button>
              </div>
            </section>
          </a-tab-pane>

          <a-tab-pane key="focus" tab="侧重点">
            <section class="panel-section compact-section">
              <div class="section-heading">
                <span>生成侧重点</span>
                <small>AI 会按这些要求收束</small>
              </div>
              <ul class="focus-list">
                <li v-for="focus in currentMaterial.requirements" :key="focus">{{ focus }}</li>
              </ul>
            </section>
          </a-tab-pane>
        </a-tabs>
      </aside>

      <main class="editor-panel panel">
        <div class="editor-topbar">
          <div>
            <h3><FileTextOutlined /> 正文编辑区</h3>
            <p>{{ editorStatusText }}</p>
          </div>
          <div class="editor-actions">
            <a-button type="primary" :loading="generating" @click="handleGenerate">
              <ThunderboltOutlined />
              {{ hasContent ? '重新生成' : '开始生成' }}
            </a-button>
            <a-button :disabled="!hasContent || generating" @click="showAddSectionModal = true">
              <PlusOutlined /> 追加章节
            </a-button>
            <a-dropdown placement="bottomRight" :disabled="!hasContent">
              <a-button :disabled="!hasContent">
                <ExportOutlined /> 导出
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="exportWord">
                    <FileWordOutlined /> 导出 Word
                  </a-menu-item>
                  <a-menu-item @click="exportPdf">
                    <FilePdfOutlined /> 导出 PDF
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button :loading="saving" :disabled="!hasContent" @click="handleSave">
              <SaveOutlined /> 保存
            </a-button>
          </div>
        </div>

        <div class="editor-body">
          <div v-if="!hasContent && !generating" class="empty-state">
            <FileTextOutlined />
            <strong>填写主题即可生成一份可编辑材料</strong>
            <span>其他基础信息用于提高内容贴合度，生成后可继续局部润色和扩写。</span>
          </div>

          <div v-else-if="generating && !streamingText" class="skeleton-state">
            <div class="skeleton-line title"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-gap"></div>
            <div class="skeleton-line title"></div>
            <div class="skeleton-line"></div>
          </div>

          <div v-else class="editor-wrapper" @click="hideAiToolbar">
            <div
              v-show="showAiToolbar"
              class="ai-toolbar"
              :style="{ left: aiToolbarPos.x + 'px', top: aiToolbarPos.y + 'px' }"
              @mousedown.prevent
            >
              <button class="ai-tool-btn" @click="inlineAi('polish')">
                <HighlightOutlined />
                <span>润色</span>
              </button>
              <button class="ai-tool-btn" @click="inlineAi('expand')">
                <ColumnWidthOutlined />
                <span>扩写</span>
              </button>
              <button class="ai-tool-btn" @click="inlineAi('rewrite')">
                <SyncOutlined />
                <span>改写</span>
              </button>
              <button class="ai-tool-btn" @click="inlineAi('continue')">
                <ThunderboltOutlined />
                <span>续写</span>
              </button>
              <button class="ai-tool-btn" @click="inlineAi('summarize')">
                <CompressOutlined />
                <span>总结</span>
              </button>
            </div>

            <div class="rich-editor-frame">
              <Toolbar
                class="rich-editor-toolbar"
                :editor="richEditorRef"
                :defaultConfig="toolbarConfig"
                mode="default"
              />
              <div
                ref="contentEditableRef"
                class="rich-editor-content"
                @mouseup="onSelectionChange"
                @keyup="onSelectionChange"
              >
                <Editor
                  v-model="editorContent"
                  class="content-editor"
                  :defaultConfig="editorConfig"
                  mode="default"
                  @onCreated="handleRichEditorCreated"
                  @onChange="onEditorInput"
                  @onDestroyed="handleRichEditorDestroyed"
                />
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <a-modal
      v-model:open="showAddSectionModal"
      title="追加正文章节"
      width="420px"
      wrap-class-name="add-section-modal-wrap"
      class="add-section-modal"
      ok-text="追加"
      cancel-text="取消"
      @ok="confirmAddSection"
      @cancel="cancelAddSection"
    >
      <div class="add-section-form">
        <p>会根据当前正文内容，在文章末尾补写一个新章节。</p>
        <a-input
          v-model:value="newSectionTitle"
          placeholder="请输入章节标题，如：教学效果分析"
          size="large"
        />
      </div>
    </a-modal>

    <a-modal
      v-model:open="showWritingLibrary"
      width="min(1080px, calc(100vw - 64px))"
      wrap-class-name="writing-library-modal-wrap"
      class="writing-library-modal"
      :footer="null"
      :destroy-on-close="true"
    >
      <template #title>
        <div class="writing-library-modal-title">
          <span class="writing-library-modal-icon"><HistoryOutlined /></span>
          <div>
            <strong>我的文档</strong>
            <span>查找、预览并继续编辑已保存的内容</span>
          </div>
        </div>
      </template>
      <div class="writing-library-layout">
        <aside class="writing-library-sidebar">
          <div class="writing-library-sidebar-head">
            <div class="writing-library-sidebar-label">
              <strong>文档列表</strong>
              <span>{{ filteredWritingDocuments.length }} 份</span>
            </div>
            <a-input v-model:value="writingSearch" allow-clear placeholder="搜索文档标题">
              <template #prefix><SearchOutlined /></template>
            </a-input>
          </div>
          <a-spin :spinning="writingListLoading" class="writing-library-list-spin">
            <div v-if="filteredWritingDocuments.length" class="writing-document-list">
              <button
                v-for="item in filteredWritingDocuments"
                :key="item.id"
                class="writing-document-item"
                :class="{ active: librarySelectedDocId === item.id }"
                @click="selectWritingDocument(item)"
              >
                <span class="writing-document-item-title">
                  <FileTextOutlined />
                  <strong>{{ item.title || '未命名文档' }}</strong>
                </span>
                <time>{{ formatWritingTime(item.updateTime || item.createTime) }}</time>
              </button>
            </div>
            <a-empty v-else-if="!writingListLoading" :description="writingSearch ? '没有匹配的文档' : '还没有保存的文档'" />
          </a-spin>
        </aside>

        <section v-if="librarySelectedDocument" class="writing-library-editor">
          <div class="writing-library-editor-head">
            <div class="writing-library-title-field">
              <div class="writing-library-title-meta">
                <label for="writing-library-title">文档标题</label>
                <span>最近修改 {{ formatWritingTime(librarySelectedDocument.updateTime || librarySelectedDocument.createTime) }}</span>
              </div>
              <a-input id="writing-library-title" v-model:value="libraryEditorTitle" maxlength="100" />
            </div>
            <div class="writing-library-head-actions">
              <a-popconfirm
                title="确定删除这份文档吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="removeWritingDocument(librarySelectedDocument)"
              >
                <a-button danger type="text"><DeleteOutlined /> 删除文档</a-button>
              </a-popconfirm>
              <a-button type="primary" :loading="librarySaving" @click="saveLibraryDocument">
                <SaveOutlined /> 保存修改
              </a-button>
            </div>
          </div>
          <div class="writing-library-rich-editor">
            <Toolbar
              class="writing-library-toolbar"
              :editor="libraryRichEditorRef"
              :defaultConfig="toolbarConfig"
              mode="default"
            />
            <Editor
              v-model="libraryEditorContent"
              class="writing-library-content-editor"
              :defaultConfig="libraryEditorConfig"
              mode="default"
              @onCreated="handleLibraryEditorCreated"
              @onDestroyed="handleLibraryEditorDestroyed"
            />
          </div>
          <footer class="writing-library-footer">
            <span><EditOutlined /> 需要使用 AI 续写时，可将当前内容载入主编辑区。</span>
            <a-button @click="loadLibraryDocumentToMain"><ExportOutlined /> 载入主编辑区</a-button>
          </footer>
        </section>
        <section v-else class="writing-library-empty-preview">
          <FileTextOutlined />
          <strong>选择一份文档开始预览和编辑</strong>
          <span>保存过的智能编写内容会显示在左侧。</span>
        </section>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
// @ts-ignore
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { computed, markRaw, nextTick, onUnmounted, reactive, ref, shallowRef, watch } from 'vue'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import {
  BulbOutlined,
  ColumnWidthOutlined,
  CompressOutlined,
  DeleteOutlined,
  EditOutlined,
  ExperimentOutlined,
  ExportOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  FileWordOutlined,
  FlagOutlined,
  HighlightOutlined,
  HistoryOutlined,
  PlusOutlined,
  ProjectOutlined,
  ReadOutlined,
  SaveOutlined,
  SearchOutlined,
  SyncOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'

import {
  deleteWriting,
  getWritingList,
  saveWriting,
  streamGenerateArticle,
  streamWritingAi,
  type WritingDocument,
  updateWriting,
} from '@/api/writing'

type MaterialTypeKey =
  | 'teaching_speech'
  | 'reflection'
  | 'ideological'
  | 'experiment_design'
  | 'experiment_report'
  | 'work_summary'

type MaterialType = {
  key: MaterialTypeKey
  name: string
  desc: string
  focus: string
  icon: any
  exampleTitle: string
  outlines: string[]
  requirements: string[]
}

type FieldConfig = {
  contextLabel: string
  contextPlaceholder: string
  rangeLabel: string
  rangePlaceholder: string
  primaryLabel: string
  primaryPlaceholder: string
  secondaryLabel: string
  secondaryPlaceholder: string
}

const md = new MarkdownIt({ breaks: true, linkify: true, html: false })
let html2pdf: any = null
import('html2pdf.js').then((m: any) => {
  html2pdf = m.default || m
})

const materialTypes: MaterialType[] = [
  {
    key: 'teaching_speech',
    name: '讲课稿',
    desc: '用于讲课、评审和教学展示',
    focus: '说清教材、学情、目标和教学流程',
    icon: markRaw(ReadOutlined),
    exampleTitle: '栈与队列课程讲课稿',
    outlines: ['说教材', '说学情', '说教学目标', '说教学重难点', '说教学方法', '说教学过程', '说教学反思'],
    requirements: ['语言适合教师现场讲课表达', '说清为什么这样设计，而不是只罗列教学环节', '突出教学重难点突破和学生学习活动'],
  },
  {
    key: 'reflection',
    name: '教学反思',
    desc: '总结课堂效果与改进方向',
    focus: '问题识别、证据描述和改进措施',
    icon: markRaw(BulbOutlined),
    exampleTitle: '数据结构课程教学反思',
    outlines: ['教学背景', '目标达成情况', '课堂亮点', '存在问题', '原因分析', '改进措施'],
    requirements: ['避免空泛表态，要写出具体课堂现象', '改进措施要能在下一次课执行', '体现教师专业成长'],
  },
  {
    key: 'ideological',
    name: '课程思政',
    desc: '提炼课程内容中的育人元素',
    focus: '知识点、价值引导和教学活动融合',
    icon: markRaw(FlagOutlined),
    exampleTitle: '数据结构课程中的工匠精神培养',
    outlines: ['案例背景', '知识点切入', '思政元素', '融入路径', '课堂活动', '评价方式'],
    requirements: ['思政元素要自然嵌入知识学习', '至少设计一个讨论或实践活动', '避免生硬口号化表达'],
  },
  {
    key: 'experiment_design',
    name: '实验方案',
    desc: '规划实验目标、步骤和评价',
    focus: '实验目的、过程、数据和评价标准',
    icon: markRaw(ExperimentOutlined),
    exampleTitle: '链表基本操作实验设计方案',
    outlines: ['实验目的', '实验环境', '实验原理', '实验任务', '实验步骤', '提交要求', '评价标准'],
    requirements: ['实验任务要明确输入、过程和输出', '步骤适合学生独立操作', '评价标准要可量化'],
  },
  {
    key: 'experiment_report',
    name: '实验报告',
    desc: '生成标准实验报告模板',
    focus: '过程记录、结果分析和问题总结',
    icon: markRaw(FileTextOutlined),
    exampleTitle: 'TCP 协议分析实验报告',
    outlines: ['实验目的', '实验原理', '实验过程', '实验数据', '结果分析', '问题与解决', '实验结论'],
    requirements: ['结果分析要围绕数据或现象展开', '问题与解决要具体可复盘', '结论不要只重复实验目的'],
  },
  {
    key: 'work_summary',
    name: '工作总结',
    desc: '梳理教学工作成果与规划',
    focus: '成果、问题、改进和后续计划',
    icon: markRaw(ProjectOutlined),
    exampleTitle: '2026 年度教学工作总结',
    outlines: ['工作概况', '主要成果', '教学改革与实践', '问题不足', '改进措施', '下一步计划'],
    requirements: ['成果要有条理和证据感', '问题不足要客观具体', '计划要体现可执行性'],
  },
]

const selectedType = ref<MaterialTypeKey>('teaching_speech')
const activeConfigTab = ref('basic')
const currentMaterial = computed(
  () => materialTypes.find((item) => item.key === selectedType.value) || materialTypes[0],
)

const fieldConfigMap: Record<MaterialTypeKey, FieldConfig> = {
  teaching_speech: {
    contextLabel: '授课对象',
    contextPlaceholder: '例如：软件工程专业大二学生',
    rangeLabel: '讲课场景',
    rangePlaceholder: '例如：课程评审 / 教学比赛 / 集体备课',
    primaryLabel: '设计亮点',
    primaryPlaceholder: '例如：用可视化动画突破抽象概念，结合真实网络案例导入',
    secondaryLabel: '重难点突破',
    secondaryPlaceholder: '例如：通过任务驱动和分组讨论理解算法过程',
  },
  reflection: {
    contextLabel: '授课对象',
    contextPlaceholder: '例如：计算机网络本科二年级',
    rangeLabel: '课次/章节',
    rangePlaceholder: '例如：第4周 TCP协议分析',
    primaryLabel: '反思重点',
    primaryPlaceholder: '例如：课堂互动不足、协议状态变化理解不充分',
    secondaryLabel: '改进方向',
    secondaryPlaceholder: '例如：增加抓包演示和分组讨论，补充可视化流程图',
  },
  ideological: {
    contextLabel: '适用课程',
    contextPlaceholder: '例如：数据结构 / 计算机网络',
    rangeLabel: '融入环节',
    rangePlaceholder: '例如：课堂导入、案例分析、实验任务',
    primaryLabel: '育人目标',
    primaryPlaceholder: '例如：培养工程责任意识、规范意识和协作精神',
    secondaryLabel: '思政元素',
    secondaryPlaceholder: '例如：工匠精神、网络安全意识、科技报国',
  },
  experiment_design: {
    contextLabel: '实验对象',
    contextPlaceholder: '例如：软件工程专业大二学生',
    rangeLabel: '实验时长',
    rangePlaceholder: '例如：2课时 / 1次实验课',
    primaryLabel: '实验目标',
    primaryPlaceholder: '例如：掌握链表插入、删除与遍历的实现方法',
    secondaryLabel: '实验重难点',
    secondaryPlaceholder: '例如：指针边界处理、异常输入处理、结果验证',
  },
  experiment_report: {
    contextLabel: '实验环境',
    contextPlaceholder: '例如：Windows 11、Wireshark、Packet Tracer',
    rangeLabel: '实验时间/批次',
    rangePlaceholder: '例如：第6周实验 / 第2组',
    primaryLabel: '实验目的',
    primaryPlaceholder: '例如：分析 TCP 三次握手过程和报文关键字段',
    secondaryLabel: '关键数据/现象',
    secondaryPlaceholder: '例如：SYN、ACK序号变化，丢包后的重传现象',
  },
  work_summary: {
    contextLabel: '总结周期',
    contextPlaceholder: '例如：2026年度 / 本学期 / 近三个月',
    rangeLabel: '工作范围',
    rangePlaceholder: '例如：课程建设、课堂教学、竞赛指导',
    primaryLabel: '主要成果',
    primaryPlaceholder: '例如：完成课程资源建设，指导学生项目获奖，优化实验教学',
    secondaryLabel: '问题与计划',
    secondaryPlaceholder: '例如：课堂数据沉淀不足，下一步完善过程性评价',
  },
}

const currentFieldConfig = computed(() => fieldConfigMap[selectedType.value])

const form = reactive({
  title: '',
  contextInfo: '',
  rangeInfo: '',
  primaryInfo: '',
  secondaryInfo: '',
})

const selectedOutlines = ref<string[]>([])
const addingOutline = ref(false)
const newOutline = ref('')

const generating = ref(false)
const streamingText = ref('')
const markdownText = ref('')
const editorContent = ref('')
const docTitle = ref('')
const saving = ref(false)
const currentDocId = ref<number | null>(null)
const showWritingLibrary = ref(false)
const writingListLoading = ref(false)
const writingDocuments = ref<WritingDocument[]>([])
const writingSearch = ref('')
const librarySelectedDocId = ref<number | null>(null)
const libraryEditorTitle = ref('')
const libraryEditorContent = ref('')
const librarySaving = ref(false)
const libraryRichEditorRef = shallowRef<any>()
const showAddSectionModal = ref(false)
const newSectionTitle = ref('')
const contentEditableRef = ref<HTMLDivElement>()
const richEditorRef = shallowRef<any>()
const isUpdatingFromExternal = ref(false)

const showAiToolbar = ref(false)
const aiToolbarPos = ref({ x: 0, y: 0 })
const selectedText = ref('')
let savedRange: Range | null = null
let abortController: AbortController | null = null

const toolbarConfig = {
  toolbarKeys: [
    'headerSelect',
    'bold',
    'italic',
    'underline',
    'through',
    'color',
    'bgColor',
    'fontSize',
    'divider',
    'justifyLeft',
    'justifyCenter',
    'justifyRight',
    'divider',
    'numberedList',
    'bulletedList',
    'insertLink',
    'divider',
    'undo',
    'redo',
  ],
}

const editorConfig = {
  placeholder: 'AI 生成后可在这里继续编辑正文',
  scroll: true,
}

const libraryEditorConfig = {
  placeholder: '在这里继续编辑文档内容',
  scroll: true,
}

const filteredWritingDocuments = computed(() => {
  const keyword = writingSearch.value.trim().toLowerCase()
  if (!keyword) return writingDocuments.value
  return writingDocuments.value.filter((item) => (item.title || '').toLowerCase().includes(keyword))
})

const librarySelectedDocument = computed(
  () => writingDocuments.value.find((item) => item.id === librarySelectedDocId.value) || null,
)

const defaultOutlines = computed(() => currentMaterial.value.outlines)
const allOutlineTags = computed(() => {
  const custom = selectedOutlines.value.filter((tag) => !defaultOutlines.value.includes(tag))
  return [...defaultOutlines.value, ...custom]
})
const hasContent = computed(() => plainText.value.trim().length > 0 || streamingText.value.trim().length > 0)
const editorStatusText = computed(() => {
  if (generating.value) return 'AI 正在写作，可停止后继续编辑'
  if (hasContent.value) return '选中文本可调用局部 AI 编辑'
  return '生成后可直接二次编辑、保存或导出'
})

watch(
  currentMaterial,
  (material) => {
    selectedOutlines.value = [...material.outlines]
  },
  { immediate: true },
)

watch(
  () => editorContent.value,
  () => {
    if (isUpdatingFromExternal.value) isUpdatingFromExternal.value = false
  },
)

const plainText = computed(() => {
  const html = editorContent.value || md.render(markdownText.value || streamingText.value)
  if (!html) return ''
  const div = document.createElement('div')
  div.innerHTML = html
  return div.textContent || ''
})

onUnmounted(() => {
  stopGenerate()
  richEditorRef.value?.destroy?.()
  libraryRichEditorRef.value?.destroy?.()
})

function selectMaterial(key: MaterialTypeKey) {
  selectedType.value = key
}

function toggleOutline(tag: string) {
  if (!defaultOutlines.value.includes(tag)) {
    selectedOutlines.value = selectedOutlines.value.filter((item) => item !== tag)
    return
  }
  if (selectedOutlines.value.includes(tag)) {
    selectedOutlines.value = selectedOutlines.value.filter((item) => item !== tag)
  } else {
    selectedOutlines.value.push(tag)
  }
}

function startAddOutline() {
  addingOutline.value = true
  newOutline.value = ''
  nextTick(() => {
    document.querySelector<HTMLInputElement>('.outline-input')?.focus()
  })
}

function finishAddOutline() {
  const val = newOutline.value.trim()
  if (val && !selectedOutlines.value.includes(val)) {
    selectedOutlines.value.push(val)
  }
  addingOutline.value = false
  newOutline.value = ''
}

function buildPrompt(extraTask?: string) {
  const material = currentMaterial.value
  const fields = currentFieldConfig.value
  const outline = selectedOutlines.value.join('、')
  const requirements = material.requirements.map((item, index) => `${index + 1}. ${item}`).join('\n')

  return `【材料类型】${material.name}
【材料主题】${form.title.trim()}
${form.contextInfo ? `【${fields.contextLabel}】${form.contextInfo.trim()}\n` : ''}${form.rangeInfo ? `【${fields.rangeLabel}】${form.rangeInfo.trim()}\n` : ''}${form.primaryInfo ? `【${fields.primaryLabel}】${form.primaryInfo.trim()}\n` : ''}${form.secondaryInfo ? `【${fields.secondaryLabel}】${form.secondaryInfo.trim()}\n` : ''}${outline ? `【目录结构】请严格按照以下章节组织：${outline}\n` : ''}${extraTask ? `【追加任务】${extraTask}\n` : ''}
【写作要求】
${requirements}
4. 使用 Markdown 格式输出，结构清晰，内容专业、具体、可直接编辑。
5. 严禁输出开场白、客套话、解释性文字，直接从标题或第一个正式章节开始。`
}

function buildInlinePrompt(text: string, operation: string) {
  const actionMap: Record<string, string> = {
    polish: '润色这段文字，使表达更专业、流畅、适合教师材料',
    expand: '扩写这段文字，补充教学细节、课堂场景或论证依据',
    rewrite: '改写这段文字，保持原意但让结构和表达更清晰',
    continue: '顺着这段文字自然续写，保持当前材料语境',
    summarize: '总结这段文字，提炼为简洁清晰的教师材料表述',
  }
  return `【当前材料类型】${currentMaterial.value.name}
【材料主题】${form.title || docTitle.value || '未命名'}
【任务】${actionMap[operation] || actionMap.polish}
【待处理文本】
${text}

要求：只输出处理后的正文，不要解释，不要开场白。`
}

async function handleGenerate() {
  if (!form.title.trim()) {
    message.warning('请输入材料主题')
    return
  }

  generating.value = true
  streamingText.value = ''
  markdownText.value = ''
  editorContent.value = ''
  abortController = new AbortController()

  try {
    const response = await streamGenerateArticle(buildPrompt(), { signal: abortController.signal })
    if (!response.ok) {
      const errText = await response.text()
      throw new Error(errText || '请求失败')
    }
    if (!response.body) throw new Error('无响应体')

    await readStream(response, (chunk) => {
      streamingText.value += chunk
      renderMarkdown(streamingText.value)
    })

    if (streamingText.value) {
      docTitle.value = form.title.trim()
      markdownText.value = streamingText.value
      const html = md.render(streamingText.value)
      isUpdatingFromExternal.value = true
      editorContent.value = html
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') {
      message.error(e?.message || 'AI 生成失败，请稍后重试')
    }
  } finally {
    await nextTick()
    streamingText.value = ''
    generating.value = false
    abortController = null
  }
}

async function confirmAddSection() {
  const title = newSectionTitle.value.trim()
  if (!title) {
    message.warning('请输入章节标题')
    return
  }
  if (!markdownText.value && !editorContent.value) {
    message.warning('请先生成文章主体内容')
    return
  }

  showAddSectionModal.value = false
  generating.value = true
  streamingText.value = ''
  abortController = new AbortController()

  try {
    const currentText = markdownText.value || plainText.value
    const prompt = buildPrompt(`请在现有材料末尾新增章节「${title}」。只输出新增章节内容，不要重复前文。现有材料末尾上下文：${currentText.slice(-1000)}`)
    const response = await streamGenerateArticle(prompt, { signal: abortController.signal })
    if (!response.ok) throw new Error('请求失败')
    if (!response.body) throw new Error('无响应体')

    let appended = ''
    await readStream(response, (chunk) => {
      appended += chunk
      streamingText.value += chunk
      renderMarkdown(`${currentText}\n\n${streamingText.value}`)
    })

    if (appended) {
      markdownText.value = `${currentText}\n\n${appended}`
      const html = md.render(markdownText.value)
      isUpdatingFromExternal.value = true
      editorContent.value = html
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') message.error(e?.message || '章节生成失败')
  } finally {
    await nextTick()
    streamingText.value = ''
    generating.value = false
    abortController = null
    newSectionTitle.value = ''
  }
}

function cancelAddSection() {
  showAddSectionModal.value = false
  newSectionTitle.value = ''
}

function stopGenerate() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
}

async function readStream(response: Response, onChunk: (chunk: string) => void) {
  const reader = response.body!.getReader()
  const decoder = new TextDecoder('utf-8')
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    onChunk(decoder.decode(value, { stream: true }))
  }
}

function renderMarkdown(text: string) {
  isUpdatingFromExternal.value = true
  editorContent.value = md.render(text)
}

function onEditorInput(editor?: any) {
  if (editor?.getHtml) {
    editorContent.value = editor.getHtml()
  }
  markdownText.value = plainText.value
}

function handleRichEditorCreated(editor: any) {
  richEditorRef.value = editor
}

function handleRichEditorDestroyed() {
  richEditorRef.value = null
}

function onSelectionChange() {
  const selection = window.getSelection()
  if (!selection || selection.isCollapsed) {
    showAiToolbar.value = false
    savedRange = null
    return
  }

  const text = selection.toString().trim()
  if (text.length > 0 && text.length < 2000) {
    selectedText.value = text
    savedRange = selection.getRangeAt(0).cloneRange()
    const rect = selection.getRangeAt(0).getBoundingClientRect()
    aiToolbarPos.value = {
      x: rect.left + rect.width / 2,
      y: Math.max(72, rect.top - 48),
    }
    showAiToolbar.value = true
  } else {
    showAiToolbar.value = false
    savedRange = null
  }
}

function hideAiToolbar(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.ai-toolbar')) {
    showAiToolbar.value = false
    savedRange = null
  }
}

function replaceSavedRange(html: string) {
  if (!savedRange || !contentEditableRef.value) return

  const editor = richEditorRef.value
  if (editor?.restoreSelection && editor?.dangerouslyInsertHtml) {
    editor.restoreSelection()
    editor.deleteFragment?.()
    editor.dangerouslyInsertHtml(html)
    editorContent.value = editor.getHtml?.() || editorContent.value
    markdownText.value = plainText.value
    savedRange = null
    showAiToolbar.value = false
    return
  }

  const selection = window.getSelection()
  selection?.removeAllRanges()
  selection?.addRange(savedRange)
  savedRange.deleteContents()

  const div = document.createElement('div')
  div.innerHTML = html
  const fragment = document.createDocumentFragment()
  let lastNode: ChildNode | null = null
  while (div.firstChild) {
    lastNode = div.firstChild
    fragment.appendChild(div.firstChild)
  }
  savedRange.insertNode(fragment)

  if (lastNode) {
    const newRange = document.createRange()
    newRange.setStartAfter(lastNode)
    newRange.collapse(true)
    selection?.removeAllRanges()
    selection?.addRange(newRange)
  }

  editorContent.value = richEditorRef.value?.getHtml?.() || editorContent.value
  markdownText.value = plainText.value
  savedRange = null
  showAiToolbar.value = false
}

async function inlineAi(operation: string) {
  if (!selectedText.value || !savedRange) return
  showAiToolbar.value = false
  message.loading({ content: 'AI 处理中...', duration: 0, key: 'inline-ai' })

  try {
    const response = await streamWritingAi(buildInlinePrompt(selectedText.value, operation), operation)
    if (!response.ok) throw new Error('请求失败')
    if (!response.body) throw new Error('无响应体')

    let result = ''
    await readStream(response, (chunk) => {
      result += chunk
    })

    if (result) {
      replaceSavedRange(md.render(result))
      message.success({ content: 'AI 处理完成', key: 'inline-ai' })
    }
  } catch {
    message.error({ content: 'AI 操作失败', key: 'inline-ai' })
  }
}

async function loadWritingDocuments() {
  writingListLoading.value = true
  try {
    writingDocuments.value = (await getWritingList()) || []
  } catch (e: any) {
    message.error(e?.message || '加载文档列表失败')
  } finally {
    writingListLoading.value = false
  }
}

async function openWritingLibrary() {
  showWritingLibrary.value = true
  await loadWritingDocuments()
  const preferredDocument =
    writingDocuments.value.find((item) => item.id === currentDocId.value) || writingDocuments.value[0]
  if (preferredDocument) selectWritingDocument(preferredDocument)
}

function selectWritingDocument(item: WritingDocument) {
  librarySelectedDocId.value = item.id
  libraryEditorTitle.value = item.title || ''
  libraryEditorContent.value = item.content || ''
}

function loadLibraryDocumentToMain() {
  const item = librarySelectedDocument.value
  if (!item) return
  currentDocId.value = item.id
  docTitle.value = libraryEditorTitle.value.trim() || item.title || ''
  form.title = docTitle.value
  streamingText.value = ''
  markdownText.value = ''
  isUpdatingFromExternal.value = true
  editorContent.value = libraryEditorContent.value
  showWritingLibrary.value = false
  message.success('文档已打开，可继续编辑')
}

function handleLibraryEditorCreated(editor: any) {
  libraryRichEditorRef.value = editor
}

function handleLibraryEditorDestroyed() {
  libraryRichEditorRef.value = null
}

async function saveLibraryDocument() {
  const item = librarySelectedDocument.value
  if (!item) return
  const title = libraryEditorTitle.value.trim() || '未命名文档'
  librarySaving.value = true
  try {
    await updateWriting({
      id: item.id,
      title,
      content: libraryEditorContent.value,
      type: 'writing',
    })
    item.title = title
    item.content = libraryEditorContent.value
    item.updateTime = new Date().toISOString()
    if (currentDocId.value === item.id) {
      docTitle.value = title
      form.title = title
      editorContent.value = libraryEditorContent.value
    }
    message.success('修改已保存')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    librarySaving.value = false
  }
}

async function removeWritingDocument(item: WritingDocument) {
  try {
    await deleteWriting(item.id)
    writingDocuments.value = writingDocuments.value.filter((doc) => doc.id !== item.id)
    if (currentDocId.value === item.id) currentDocId.value = null
    if (librarySelectedDocId.value === item.id) {
      const nextDocument = filteredWritingDocuments.value[0] || writingDocuments.value[0]
      if (nextDocument) selectWritingDocument(nextDocument)
      else {
        librarySelectedDocId.value = null
        libraryEditorTitle.value = ''
        libraryEditorContent.value = ''
      }
    }
    message.success('文档已删除')
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

function formatWritingTime(value?: string) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

async function handleSave() {
  const title = docTitle.value.trim() || form.title.trim() || '未命名文档'
  const content = editorContent.value
  saving.value = true
  try {
    if (currentDocId.value) {
      await updateWriting({ id: currentDocId.value, title, content, type: 'writing' })
    } else {
      const data = await saveWriting({ title, content })
      if (data?.id) currentDocId.value = data.id
    }
    await loadWritingDocuments()
    message.success('保存成功')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function exportWord() {
  const title = docTitle.value.trim() || form.title.trim() || '未命名文档'
  const content = editorContent.value
  const html = `<!DOCTYPE html>
<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
<head><meta charset='utf-8'><title>${title}</title>
<style>
body{font-family:'Microsoft YaHei',sans-serif;max-width:800px;margin:40px auto;padding:0 20px;line-height:1.8;color:#333;}
h1{font-size:22px;font-weight:800;border-bottom:2px solid #2563eb;padding-bottom:8px;}
h2{font-size:18px;font-weight:700;color:#1e293b;margin-top:24px;padding-left:12px;border-left:4px solid #2563eb;}
p{margin:0 0 12px;} ul,ol{margin:0 0 12px;padding-left:24px;} li{margin-bottom:6px;}
table{width:100%;border-collapse:collapse;margin:12px 0;font-size:14px;} th,td{border:1px solid #d1d5db;padding:8px 12px;text-align:left;} th{background:#f3f4f6;}
</style></head><body>${content}</body></html>`

  const blob = new Blob(['\ufeff', html], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${title}.doc`
  a.click()
  URL.revokeObjectURL(url)
}

async function exportPdf() {
  if (!html2pdf) {
    message.loading('正在加载导出组件...', 1)
    const m: any = await import('html2pdf.js')
    html2pdf = m.default || m
  }

  const title = docTitle.value.trim() || form.title.trim() || '未命名文档'
  const element = contentEditableRef.value
  if (!element) {
    message.error('未找到导出内容')
    return
  }

  html2pdf()
    .set({
      margin: [10, 10, 10, 10],
      filename: `${title}.pdf`,
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: { scale: 2, useCORS: true, logging: false },
      jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
    })
    .from(element)
    .save()
}
</script>

<style scoped>
.smart-writing-page {
  height: 100%;
  padding: 28px;
  overflow: hidden;
  background: #f7f8fa;
  color: #0f172a;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-shrink: 0;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 15px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions :deep(.ant-btn) {
  font-size: 14px;
  font-weight: 600;
}

:global(.writing-library-modal-wrap .ant-modal) {
  top: 60px;
  width: min(1080px, calc(100vw - 64px)) !important;
  padding-bottom: 0;
}

:global(.writing-library-modal .ant-modal-content) {
  height: calc(100vh - 120px);
  max-height: 740px;
  padding: 0;
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.18);
}

:global(.writing-library-modal .ant-modal-header) {
  height: 70px;
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0 24px;
  border-bottom: 1px solid #e2e8f0;
}

:global(.writing-library-modal .ant-modal-title) {
  color: #172033;
}

:global(.writing-library-modal .ant-modal-close) {
  top: 15px;
  right: 16px;
  width: 40px;
  height: 40px;
  color: #64748b;
}

:global(.writing-library-modal .ant-modal-body) {
  height: calc(100% - 70px);
}

.writing-library-modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.writing-library-modal-icon {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 10px;
  background: #eaf3ff;
  color: #1769aa;
  font-size: 19px;
}

.writing-library-modal-title > div {
  display: grid;
  gap: 2px;
}

.writing-library-modal-title strong {
  color: #172033;
  font-size: 18px;
  line-height: 1.3;
  font-weight: 800;
}

.writing-library-modal-title > div > span {
  color: #64748b;
  font-size: 12.5px;
  line-height: 1.4;
  font-weight: 500;
}

.writing-library-layout {
  height: 100%;
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr);
  background: #fff;
}

.writing-library-sidebar {
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #dde5ee;
  background: #f5f7fa;
}

.writing-library-sidebar-head {
  display: grid;
  gap: 12px;
  padding: 18px 16px 16px;
  border-bottom: 1px solid #dde5ee;
}

.writing-library-sidebar-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.writing-library-sidebar-label strong {
  color: #263548;
  font-size: 14px;
  font-weight: 750;
}

.writing-library-sidebar-label span {
  min-width: 38px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  border-radius: 999px;
  background: #e7ecf2;
  color: #516174;
  font-size: 12px;
  font-weight: 700;
}

.writing-library-sidebar-head :deep(.ant-input-affix-wrapper) {
  height: 40px;
  border-color: #d7e0ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: none;
  font-size: 14px;
}

.writing-library-sidebar-head :deep(.ant-input-prefix) {
  margin-inline-end: 8px;
  color: #78889a;
}

.writing-library-sidebar-head :deep(.ant-input-affix-wrapper:hover),
.writing-library-sidebar-head :deep(.ant-input-affix-wrapper-focused) {
  border-color: #69a7dd;
}

.writing-library-list-spin {
  flex: 1;
  min-height: 0;
}

.writing-library-list-spin :deep(.ant-spin-container) {
  height: 100%;
}

.writing-document-list {
  height: 100%;
  padding: 10px;
  overflow-y: auto;
}

.writing-document-item {
  width: 100%;
  display: grid;
  gap: 7px;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.18s cubic-bezier(0.25, 1, 0.5, 1),
    border-color 0.18s cubic-bezier(0.25, 1, 0.5, 1);
}

.writing-document-item:hover {
  background: #ebeff4;
}

.writing-document-item.active {
  background: #fff;
  border-color: #8dbde7;
}

.writing-document-item-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.writing-document-item-title > .anticon {
  flex-shrink: 0;
  color: #7a8a9d;
  font-size: 15px;
}

.writing-document-item.active .writing-document-item-title > .anticon {
  color: #1769aa;
}

.writing-document-item-title strong {
  min-width: 0;
  overflow: hidden;
  color: #1e293b;
  font-size: 14.5px;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.writing-document-item time {
  padding-left: 23px;
  color: #697a8f;
  font-size: 12.5px;
}

.writing-library-editor {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) 58px;
  background: #eef2f6;
}

.writing-library-editor-head {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 14px 20px;
  border-bottom: 1px solid #dde5ee;
  background: #fff;
}

.writing-library-title-field {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 7px;
}

.writing-library-title-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.writing-library-title-meta label {
  color: #42536a;
  font-size: 12.5px;
  font-weight: 750;
}

.writing-library-title-meta span {
  overflow: hidden;
  color: #75859a;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.writing-library-title-field :deep(.ant-input) {
  height: 40px;
  border-color: #d7e0ea;
  border-radius: 8px;
  color: #172033;
  font-size: 15px;
  font-weight: 750;
  box-shadow: none;
}

.writing-library-title-field :deep(.ant-input:hover),
.writing-library-title-field :deep(.ant-input:focus) {
  border-color: #69a7dd;
}

.writing-library-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.writing-library-head-actions :deep(.ant-btn) {
  height: 40px;
  border-radius: 8px;
  font-weight: 600;
}

.writing-library-rich-editor {
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #eef2f6;
}

.writing-library-toolbar {
  flex-shrink: 0;
  z-index: 1;
  border-bottom: 1px solid #dde5ee;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.writing-library-content-editor {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #eef2f6;
}

.writing-library-content-editor :deep(.w-e-text-container) {
  height: 100% !important;
  min-height: 100%;
  background: transparent;
}

.writing-library-content-editor :deep(.w-e-scroll) {
  box-sizing: border-box;
  padding: 24px 28px;
  background: #eef2f6;
}

.writing-library-content-editor :deep([data-slate-editor]) {
  box-sizing: border-box;
  max-width: 820px;
  min-height: 100%;
  margin: 0 auto;
  padding: 46px 64px 88px;
  background: #fff;
  color: #263548;
  font-size: 15.5px;
  line-height: 1.9;
  box-shadow: 0 2px 7px rgba(15, 23, 42, 0.1);
}

.writing-library-content-editor :deep([data-slate-editor] h1) {
  margin: 0 0 28px;
  color: #172033;
  font-size: 30px;
  line-height: 1.35;
  letter-spacing: -0.02em;
  font-weight: 800;
  text-wrap: balance;
}

.writing-library-content-editor :deep([data-slate-editor] h2) {
  margin: 30px 0 12px;
  color: #1f2f43;
  font-size: 21px;
  line-height: 1.45;
  font-weight: 750;
  text-wrap: balance;
}

.writing-library-content-editor :deep([data-slate-editor] h3) {
  margin: 24px 0 10px;
  color: #2c3d52;
  font-size: 17px;
  line-height: 1.5;
}

.writing-library-content-editor :deep([data-slate-editor] p) {
  margin: 0 0 12px;
}

.writing-library-content-editor :deep([data-slate-editor] ul),
.writing-library-content-editor :deep([data-slate-editor] ol) {
  margin: 0 0 18px;
  padding-left: 24px;
}

.writing-library-content-editor :deep([data-slate-editor] li) {
  margin-bottom: 6px;
}

.writing-library-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 20px;
  border-top: 1px solid #dde5ee;
  background: #fff;
}

.writing-library-footer span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  overflow: hidden;
  color: #64748b;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.writing-library-footer span > .anticon {
  flex-shrink: 0;
  color: #1769aa;
}

.writing-library-footer :deep(.ant-btn) {
  height: 38px;
  border-color: #cdd8e4;
  border-radius: 8px;
  color: #294660;
  font-weight: 600;
}

.writing-library-empty-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  background: #f4f7fa;
  color: #738499;
}

.writing-library-empty-preview > .anticon {
  font-size: 42px;
  color: #c5d2e0;
}

.writing-library-empty-preview strong {
  color: #42526a;
  font-size: 16px;
}

.writing-library-empty-preview span {
  font-size: 13.5px;
}

.writing-workspace {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 18px;
  overflow: hidden;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.04);
  min-height: 0;
  overflow: hidden;
}

.config-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-section {
  padding: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-section:last-child {
  border-bottom: none;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-heading span {
  font-size: 15px;
  font-weight: 800;
  color: #1e293b;
}

.section-heading small {
  color: #7c8da3;
  font-size: 12.5px;
  text-align: right;
}

.material-switcher {
  padding: 14px 14px 12px;
  border-bottom: 1px solid #edf2f7;
  flex-shrink: 0;
}

.material-switcher-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.material-switcher-head span {
  font-size: 15px;
  font-weight: 800;
  color: #1e293b;
}

.material-switcher-head small {
  max-width: 190px;
  color: #7c8da3;
  font-size: 12.5px;
  line-height: 1.45;
  text-align: right;
}

.material-strip {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 7px;
}

.material-chip {
  height: 44px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 6px;
  padding: 0 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 14.5px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.material-chip:hover,
.material-chip.active {
  color: #0f5f9e;
  border-color: #9fc9ed;
  background: #edf6ff;
}

.material-chip .anticon {
  color: #147ed9;
  font-size: 18px;
  flex-shrink: 0;
}

.material-chip span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.config-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.config-tabs :deep(.ant-tabs-nav) {
  margin: 0;
  padding: 0 14px;
  flex-shrink: 0;
}

.config-tabs :deep(.ant-tabs-tab) {
  font-size: 15px;
  font-weight: 700;
}

.config-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.config-tabs :deep(.ant-tabs-content),
.config-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  min-height: 0;
}

.compact-section {
  height: 100%;
  overflow-y: auto;
  border-bottom: none;
  padding: 14px;
}

.compact-section::-webkit-scrollbar {
  width: 0;
}

.compact-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.compact-form :deep(.ant-form-item-label > label) {
  color: #1e293b;
  font-size: 14.5px;
  font-weight: 700;
}

.compact-form :deep(.ant-input),
.compact-form :deep(.ant-input-affix-wrapper),
.compact-form :deep(.ant-input::placeholder),
.compact-form :deep(textarea.ant-input::placeholder) {
  font-size: 14.5px;
}

.compact-form :deep(.ant-input),
.compact-form :deep(textarea.ant-input) {
  color: #1e293b;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.outline-section {
  padding-top: 16px;
}

.outline-heading {
  align-items: flex-start;
  margin-bottom: 14px;
}

.outline-heading > div {
  display: grid;
  gap: 3px;
}

.outline-heading small {
  text-align: left;
}

.outline-heading em {
  min-width: 46px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #eef6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-style: normal;
  font-weight: 800;
}

.outline-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.outline-item {
  position: relative;
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
  background: #fff;
  color: #475569;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.outline-item:hover {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.outline-item.active {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1e3a8a;
  box-shadow: inset 3px 0 0 #2563eb;
}

.outline-item.active::after {
  content: '';
  position: absolute;
  right: 12px;
  width: 7px;
  height: 12px;
  border: solid #2563eb;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.outline-index {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 800;
}

.outline-name {
  min-width: 0;
  padding-right: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.outline-input {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #bfdbfe;
  border-radius: 7px;
  color: #1e293b;
  font-size: 14px;
  outline: none;
}

.outline-add-input {
  grid-column: 1 / -1;
}

.outline-add-row {
  min-height: 42px;
  grid-column: 1 / -1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed #cbd5e1;
  border-radius: 7px;
  background: #fbfdff;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
}

.outline-add-row:hover {
  border-color: #93c5fd;
  color: #1d4ed8;
  background: #f8fbff;
}

.focus-list {
  padding-left: 20px;
  margin: 0;
  color: #334155;
  font-size: 14.5px;
  line-height: 1.75;
  font-weight: 500;
}

.focus-list li {
  margin-bottom: 8px;
  padding-left: 2px;
}

.editor-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.editor-topbar {
  min-height: 72px;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.editor-topbar h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 8px;
}

.editor-topbar p {
  margin: 4px 0 0;
  color: #7c8da3;
  font-size: 13.5px;
}

.editor-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.editor-actions :deep(.ant-btn) {
  font-size: 14.5px;
  font-weight: 600;
}

:global(.add-section-modal-wrap .ant-modal) {
  top: 180px;
}

:global(.add-section-modal .ant-modal-content) {
  border-radius: 10px;
}

:global(.add-section-modal .ant-modal-header) {
  padding: 18px 20px 10px;
  border-bottom: 0;
}

:global(.add-section-modal .ant-modal-title) {
  color: #1e293b;
  font-size: 17px;
  font-weight: 800;
}

:global(.add-section-modal .ant-modal-body) {
  padding: 8px 20px 18px;
}

:global(.add-section-modal .ant-modal-footer) {
  padding: 0 20px 18px;
  border-top: 0;
}

.add-section-form {
  display: grid;
  gap: 12px;
}

.add-section-form p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.add-section-form :deep(.ant-input) {
  height: 42px;
  color: #1e293b;
  font-size: 14.5px;
}

.editor-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  gap: 10px;
  color: #94a3b8;
  padding: 24px;
}

.empty-state .anticon {
  font-size: 42px;
  color: #cbd5e1;
}

.empty-state strong {
  color: #334155;
  font-size: 18px;
}

.empty-state span {
  font-size: 14.5px;
  line-height: 1.6;
}

.skeleton-state {
  padding: 42px 56px;
}

.skeleton-line {
  height: 16px;
  border-radius: 5px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.skeleton-line.title {
  height: 24px;
  width: 48%;
}

.skeleton-line.short {
  width: 40%;
}

.skeleton-gap {
  height: 28px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.editor-wrapper {
  height: 100%;
  position: relative;
  overflow: hidden;
}

.rich-editor-frame {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.rich-editor-toolbar {
  flex-shrink: 0;
  border-bottom: 1px solid #e2e8f0;
}

.rich-editor-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.content-editor {
  height: 100%;
  outline: none;
  overflow: hidden;
  font-size: 15px;
  line-height: 1.9;
  color: #334155;
}

.content-editor :deep(.w-e-text-container) {
  height: 100% !important;
  background: #fff;
}

.content-editor :deep(.w-e-scroll) {
  padding: 34px 56px;
}

.content-editor :deep([data-slate-editor]) {
  min-height: 100%;
}

.content-editor :deep(.w-e-text-placeholder) {
  top: 34px;
  left: 56px;
  color: #94a3b8;
  font-size: 15px;
}

.content-editor :deep(h1) {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  margin: 20px 0 14px;
  padding-bottom: 8px;
  border-bottom: 2px solid #2563eb;
}

.content-editor :deep(h2) {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  margin: 22px 0 10px;
  padding-left: 12px;
  border-left: 4px solid #2563eb;
}

.content-editor :deep(h3) {
  font-size: 16px;
  font-weight: 700;
  margin: 16px 0 8px;
}

.content-editor :deep(p) {
  margin: 0 0 10px;
}

.content-editor :deep(ul),
.content-editor :deep(ol) {
  margin: 0 0 12px;
  padding-left: 24px;
}

.content-editor :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 14px;
  background: #eff6ff;
  border-left: 4px solid #2563eb;
  color: #1e3a8a;
}

.content-editor :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 14px;
}

.content-editor :deep(th),
.content-editor :deep(td) {
  border: 1px solid #e2e8f0;
  padding: 8px 12px;
}

.content-editor :deep(th) {
  background: #f8fafc;
}

.ai-toolbar {
  position: fixed;
  z-index: 1000;
  display: flex;
  gap: 4px;
  padding: 4px;
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.16);
  transform: translateX(-50%);
}

.ai-tool-btn {
  border: none;
  background: transparent;
  border-radius: 4px;
  padding: 5px 9px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #475569;
  cursor: pointer;
}

.ai-tool-btn:hover {
  background: #eff6ff;
  color: #1d4ed8;
}

.scroll-y::-webkit-scrollbar,
.config-panel::-webkit-scrollbar,
.content-editor::-webkit-scrollbar {
  width: 6px;
}

.scroll-y::-webkit-scrollbar-thumb,
.config-panel::-webkit-scrollbar-thumb,
.content-editor::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 6px;
}

@media (max-width: 1280px) {
  .writing-workspace {
    grid-template-columns: 300px minmax(0, 1fr);
  }
}

@media (max-width: 960px) {
  :global(.writing-library-modal-wrap .ant-modal) {
    top: 12px;
    width: calc(100vw - 24px) !important;
  }

  :global(.writing-library-modal .ant-modal-content) {
    height: calc(100vh - 24px);
  }

  .writing-library-layout {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .writing-library-content-editor :deep([data-slate-editor]) {
    padding: 38px 38px 72px;
  }

  .writing-library-footer span {
    display: none;
  }

  .writing-library-footer {
    justify-content: flex-end;
  }

  .smart-writing-page {
    height: auto;
    min-height: 100%;
    padding: 18px;
    overflow: auto;
  }

  .page-header,
  .editor-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions,
  .editor-actions {
    justify-content: flex-start;
  }

  .writing-workspace {
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .config-panel,
  .editor-panel {
    overflow: visible;
  }

  .editor-panel {
    min-height: 620px;
  }

  .content-editor {
    padding: 24px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .writing-library-modal-title > div > span {
    display: none;
  }

  .writing-library-layout {
    grid-template-columns: 1fr;
    grid-template-rows: 190px minmax(0, 1fr);
  }

  .writing-library-sidebar {
    border-right: 0;
    border-bottom: 1px solid #e6edf5;
  }

  .writing-library-sidebar-head {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    padding: 10px 12px;
  }

  .writing-library-editor-head {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .writing-library-head-actions {
    justify-content: flex-end;
  }

  .writing-library-content-editor :deep(.w-e-scroll) {
    padding: 12px;
  }

  .writing-library-content-editor :deep([data-slate-editor]) {
    padding: 28px 22px 56px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .writing-document-item {
    transition: none;
  }
}
</style>
