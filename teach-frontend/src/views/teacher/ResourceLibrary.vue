<template>
  <div class="resource-library modern-page">
    <div class="page-header">
      <div class="title-group">
        <h2><folder-open-outlined class="title-icon" /> 我的教学资源库</h2>
        <p class="subtitle">管理您的教学资源，并一键将其分发给学生。</p>
      </div>

      <div class="header-actions">
        <a-input-search
          v-model:value="searchKeyword"
          allowClear
          size="large"
          class="resource-search"
          placeholder="搜索资源标题、内容、类型"
        />
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab" class="custom-tabs" size="large">
      <a-tab-pane key="all">
        <template #tab><span><appstore-outlined /> 全部资源</span></template>
      </a-tab-pane>
      <a-tab-pane key="plan">
        <template #tab><span><file-text-outlined /> 我的教案</span></template>
      </a-tab-pane>
      <a-tab-pane key="quiz">
        <template #tab><span><form-outlined /> 试题库</span></template>
      </a-tab-pane>
      <a-tab-pane key="anim">
        <template #tab><span><desktop-outlined /> 交互课件库</span></template>
      </a-tab-pane>
      <a-tab-pane key="micro_video">
        <template #tab><span><video-camera-outlined /> 微课视频</span></template>
      </a-tab-pane>
      <a-tab-pane key="analysis">
        <template #tab><span><radar-chart-outlined /> 评课分析</span></template>
      </a-tab-pane>
      <a-tab-pane key="coding">
        <template #tab><span><code-outlined /> 编程题库</span></template>
      </a-tab-pane>
    </a-tabs>

    <div class="resource-scroll-card">
      <div class="resource-grid" v-if="filteredResources.length > 0">
      <div v-for="item in filteredResources" :key="item.id" class="resource-card">
        <div class="card-header">
          <div class="type-icon" :class="item.type">
            <file-text-outlined v-if="item.type === 'plan'" />
            <form-outlined v-else-if="item.type === 'quiz'" />
            <radar-chart-outlined v-else-if="item.type === 'analysis'" />
            <code-outlined v-else-if="item.type === 'coding'" />
            <video-camera-outlined v-else-if="item.type === 'micro_video'" />
            <desktop-outlined v-else />
          </div>
          <span class="date">{{ item.createTime }}</span>
        </div>

        <div class="card-body">
          <h3 class="title">{{ item.title }}</h3>
          <p class="snippet">{{ getSnippet(item.content) }}</p>
        </div>

        <div class="card-footer">
          <div class="tags">
            <span v-if="item.isPublished" class="tag success-tag">
              <check-circle-outlined /> 已发布
            </span>
            <span v-else class="tag">
              {{ item.type === 'plan' ? '智能教案' : item.type === 'quiz' ? '测试题库' : item.type === 'analysis' ? '评课报告' : item.type === 'coding' ? '编程题库' : '交互课件' }}
            </span>
            <span v-if="item.graphMapped" class="tag graph-tag">图谱映射</span>
          </div>
          <div class="actions">
            <a-button type="text" size="small" @click="previewDoc(item)" class="action-text-btn">
              <eye-outlined /> 预览
            </a-button>

            <a-dropdown placement="bottomRight">
              <a-button type="text" size="small" class="action-text-btn">操作 <down-outlined /></a-button>
              <template #overlay>
                <a-menu class="custom-dropdown-menu">
                  <a-menu-item
                    v-if="item.type === 'quiz'"
                    :disabled="isQuizBankImported(item) || quizBankImportLoadingId === item.id"
                    @click="importQuizToBank(item)"
                  >
                    <import-outlined />
                    {{ quizBankImportLoadingId === item.id ? '导入中...' : isQuizBankImported(item) ? '已导入题库' : '导入题库' }}
                  </a-menu-item>
                  <a-menu-item v-if="item.type === 'coding'" @click="importCodingToBank(item)">
                    <import-outlined /> 导入题库
                  </a-menu-item>
                  <a-menu-item v-if="item.type === 'anim'" @click="openPublishModal(item)">
                    <rocket-outlined /> {{ item.isPublished ? '再次发布课件' : '发布课件' }}
                  </a-menu-item>
                  <a-menu-item v-if="item.type === 'micro_video' && !item.isPublished" @click="openPublishModal(item)">
                    <rocket-outlined /> 发布资源
                  </a-menu-item>
                  <a-menu-divider v-if="item.type === 'quiz' || item.type === 'coding' || item.type === 'anim' || (item.type === 'micro_video' && !item.isPublished)" />
                  <a-menu-item v-if="item.type === 'plan' || item.type === 'quiz'" @click="openEditModal(item)">
                    <edit-outlined /> 编辑内容
                  </a-menu-item>
                  <a-menu-divider v-if="item.type === 'plan' || item.type === 'quiz'" />
                  <a-menu-item @click="downloadDoc(item)">
                    <download-outlined /> 导出文件
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item @click="deleteDoc(item.id)" class="text-danger">
                    <delete-outlined /> 永久删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>
      </div>
    </div>

    <a-empty
      v-else-if="!listLoading"
      description="资源库空空如也，快去 AI 备课室生成一些精美的教案和试卷吧！"
    >
      <a-button type="primary" @click="$router.push('/teacher/ai')">
        前往 AI 备课室
      </a-button>
    </a-empty>

    </div>

    <a-modal v-model:open="previewVisible" :title="currentPreview?.title" width="1100px" :footer="null" centered class="preview-modal teacher-wide-modal">
      <div v-if="currentPreview?.type === 'anim'" class="anim-preview-box">
        <AnimationWorkbench
          v-if="animPreviewPayload"
          :payload="animPreviewPayload"
          render-status="ready"
          :validation-errors="[]"
          :is-generating="false"
          :is-optimizing="false"
          :autoplay-delay="1800"
          :preview-mode="true"
        />
        <div v-else class="anim-parse-error">
          <p>课件数据解析失败，无法预览。</p>
        </div>
      </div>
      <div v-else-if="currentPreview?.type === 'micro_video'" class="micro-video-preview">
        <video v-if="microPreviewVideoUrl" :src="microPreviewVideoUrl" controls playsinline />
        <div v-else class="micro-video-empty">该微课缺少视频地址，暂时无法预览。</div>

        <div class="micro-preview-meta">
          <div class="micro-meta-item">
            <span>实际时长</span>
            <strong>{{ microPreviewDuration }}</strong>
          </div>
          <div class="micro-meta-item">
            <span>TTS 声音</span>
            <strong>{{ microPreviewVoice }}</strong>
          </div>
          <div class="micro-meta-item">
            <span>字幕</span>
            <strong>{{ readResourceParam(currentPreview, 'subtitleUrl') ? '已生成' : '未生成' }}</strong>
          </div>
        </div>

        <div class="micro-preview-summary">
          <h4>微课摘要</h4>
          <p>{{ microPreviewSummary }}</p>
        </div>

        <a-collapse ghost class="micro-advanced">
          <a-collapse-panel key="script" header="高级查看：脚本 JSON">
            <pre>{{ formattedMicroScript }}</pre>
          </a-collapse-panel>
        </a-collapse>
      </div>
      <div v-else class="preview-content markdown-render doc-style" v-html="renderMd(currentPreview?.content || '')"></div>
    </a-modal>

    <a-modal
      v-model:open="publishVisible"
      @ok="handlePublish"
      :confirmLoading="publishLoading"
      okText="确认发布"
      cancelText="取消"
      :width="currentPublishItem?.type === 'micro_video' ? '640px' : '500px'"
      centered
    >
      <template #title>
        <div class="modal-custom-title">
          <desktop-outlined v-if="currentPublishItem?.type === 'anim'" class="m-icon anim-icon" />
          <rocket-outlined v-else class="m-icon quiz-icon" />
          <span>{{ currentPublishItem?.type === 'anim' ? '发布交互课件' : '分发作业至学生端' }}</span>
        </div>
      </template>

      <div class="publish-info-banner" :class="currentPublishItem?.type">
        <span class="banner-icon">
          <desktop-outlined v-if="currentPublishItem?.type === 'anim'" />
          <form-outlined v-else />
        </span>
        <div class="info-text">
          <span class="label">即将发布{{ currentPublishItem?.type === 'anim' ? '课件' : '试卷' }}：</span>
          <strong>{{ currentPublishItem?.title }}</strong>
        </div>
      </div>

      <a-form layout="vertical" :model="publishForm" class="publish-form">
        <template v-if="currentPublishItem?.type === 'micro_video'">
          <div class="micro-publish-preview">
            <video v-if="readResourceParam(currentPublishItem, 'videoUrl')" :src="readResourceParam(currentPublishItem, 'videoUrl')" controls playsinline />
            <div class="micro-publish-copy">
              <strong>{{ currentPublishItem?.title }}</strong>
              <span>{{ microPublishDuration }}</span>
            </div>
          </div>

          <a-form-item label="发布方式">
            <a-radio-group v-model:value="publishForm.microCourseMode" button-style="solid">
              <a-radio-button value="existing">追加到已有课程</a-radio-button>
              <a-radio-button value="new">新建课程并发布</a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item v-if="publishForm.microCourseMode === 'existing'" label="目标课程" required>
            <a-select v-model:value="publishForm.courseId" @change="handleCourseChange" placeholder="请选择要追加微课的课程" size="large" allowClear>
              <a-select-option v-for="course in courseList" :key="course.id" :value="course.id">
                {{ course.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item v-else label="新课程名称" required>
            <a-input v-model:value="publishForm.courseName" size="large" placeholder="请输入课程名称" />
          </a-form-item>

          <a-row :gutter="12">
            <a-col :span="16">
              <a-form-item label="分集标题" required>
                <a-input v-model:value="publishForm.chapterTitle" size="large" placeholder="请输入分集标题" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="排序">
                <a-input-number v-model:value="publishForm.sortOrder" :min="1" :precision="0" size="large" style="width: 100%" placeholder="自动追加" />
              </a-form-item>
            </a-col>
          </a-row>
        </template>

        <template v-else-if="currentPublishItem?.type === 'anim'">
          <a-form-item label="目标课程" required>
            <a-select v-model:value="publishForm.courseId" @change="handleCourseChange" placeholder="请选择要把课件发到哪门课" size="large">
              <a-select-option v-for="course in courseList" :key="course.id" :value="course.id">
                {{ course.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="目标选集 (挂载点)" required extra="课件将精准展示在该集视频下方的「交互课件」Tab中">
            <a-select v-model:value="publishForm.chapterId" placeholder="请先选择课程，再选择对应章节" size="large" :disabled="chapterList.length === 0">
              <a-select-option v-for="chapter in chapterList" :key="chapter.id" :value="chapter.id">
                P{{ chapter.sortOrder }} - {{ chapter.title }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </template>

        <template v-else>
          <a-form-item label="目标班级" required>
            <a-select v-model:value="publishForm.classId" size="large" placeholder="请选择班级">
              <a-select-option v-for="cls in classList" :key="cls.id" :value="cls.id">
                {{ cls.className }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="关联课程 (选填)">
            <a-select v-model:value="publishForm.courseId" size="large" placeholder="可选择关联课程" allowClear>
              <a-select-option v-for="course in courseList" :key="course.id" :value="course.id">
                {{ course.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="作业截止时间">
            <a-date-picker
              v-model:value="publishForm.deadline"
              show-time
              format="YYYY-MM-DD HH:mm"
              placeholder="选择截止时间"
              size="large"
              style="width: 100%"
              :disabled-date="disabledDeadlineDate"
              :disabled-time="disabledDeadlineTime"
            />
          </a-form-item>

          <a-form-item label="允许重做">
            <a-switch v-model:checked="publishForm.allowRedo" />
          </a-form-item>

          <a-form-item label="给学生的寄语/要求 (选填)">
            <a-textarea v-model:value="publishForm.note" placeholder="例如：请同学们独立完成，主观题部分AI助教会进行严格查重..." :rows="3" />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="editVisible"
      :confirmLoading="editLoading"
      okText="保存修改"
      cancelText="取消"
      width="860px"
      centered
      @ok="handleEditSave"
    >
      <template #title>
        <div class="modal-custom-title">
          <edit-outlined class="m-icon" style="color: #3b82f6;" />
          <span>编辑资源内容</span>
        </div>
      </template>

      <a-form layout="vertical" class="edit-form">
        <a-form-item label="资源标题">
          <a-input v-model:value="editForm.title" size="large" placeholder="请输入标题" />
        </a-form-item>
        <a-form-item label="正文内容">
          <!-- 富文本编辑器：与学生端「随堂笔记」保持一致，支持加粗/列表/字号/颜色/插入图片等富文本样式 -->
          <div v-if="editVisible" class="edit-wysiwyg-box">
            <Toolbar
              class="edit-toolbar"
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              mode="default"
            />
            <Editor
              class="edit-editor"
              style="height: 460px; overflow-y: hidden;"
              v-model="editForm.content"
              :defaultConfig="editorConfig"
              mode="default"
              @onCreated="handleEditorCreated"
            />
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, shallowRef, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import dayjs, { type Dayjs } from 'dayjs'
import AnimationWorkbench from '@/components/anim-player/AnimationWorkbench.vue'
// 引入富文本编辑器（与学生端「随堂笔记」使用同一套组件）
import '@wangeditor/editor/dist/css/style.css'
// @ts-ignore  <-- 忽略该行类型检查，wangeditor-for-vue 没有完善的 TS 类型
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import {
  FolderOpenOutlined,
  AppstoreOutlined,
  FileTextOutlined,
  FormOutlined,
  DesktopOutlined,
  CheckCircleOutlined,
  RocketOutlined,
  EyeOutlined,
  DownOutlined,
  DownloadOutlined,
  DeleteOutlined,
  RadarChartOutlined,
  EditOutlined,
  CodeOutlined,
  VideoCameraOutlined,
  ImportOutlined,
} from '@ant-design/icons-vue'

const activeTab = ref('all')
const resources = ref<any[]>([])
const previewVisible = ref(false)
const currentPreview = ref<any>(null)
const listLoading = ref(false)
const microPublishLoadingId = ref<number | null>(null)
const quizBankImportLoadingId = ref<number | null>(null)

const searchKeyword = ref('')


const animPreviewPayload = computed(() => {
  if (currentPreview.value?.type !== 'anim' || !currentPreview.value?.content) return null
  try {
    return JSON.parse(currentPreview.value.content)
  } catch {
    return null
  }
})

const formatTime = (timeStr: string) => {
  if (!timeStr) return '刚刚';

  try {
    // 尝试将字符串解析为 Date 对象（这会自动处理 +00:00 并转换为本地时区时间）
    const date = new Date(timeStr);

    // 如果解析失败（非法日期），则降级为直接截断字符串
    if (isNaN(date.getTime())) {
      return String(timeStr).replace('T', ' ').substring(0, 16);
    }

    // 提取年月日时分，并补齐前导 0
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const h = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');

    return `${y}-${m}-${d} ${h}:${min}`;
  } catch (error) {
    // 发生异常时，安全地返回截取后的原字符串
    return String(timeStr).replace('T', ' ').substring(0, 16);
  }
};

const md = new MarkdownIt({ breaks: true, html: true })
const renderMd = (text: string) => md.render(text || '')


onMounted(() => {
  void loadResources()
  void fetchCourseList()
  void fetchClassList()
})

const loadResources = async () => {
  listLoading.value = true
  try {
    const data = (await request.get('/ai/resource/list')) as any[]
    resources.value = (data || []).map((item: any) => ({
      ...item,
      graphMapped: false,
      // 使用新的时间格式化函数
      createTime: formatTime(item.createTime),
    }))
  } catch (error) {
    console.error('获取云端资源失败:', error)
  } finally {
    listLoading.value = false
  }
}

const getTypeLabel = (type: string) => {
  if (type === 'plan') return '教案'
  if (type === 'quiz') return '测验 题库 作业'
  if (type === 'anim') return '交互课件'
  if (type === 'analysis') return '评课分析'
  if (type === 'coding') return '编程题库'
  return '资源'
}

const readResourceParam = (item: any, field: string) => {
  if (!item?.paramsJson) return ''
  try {
    return JSON.parse(item.paramsJson)?.[field] || ''
  } catch {
    return ''
  }
}

const readResourceParams = (item: any) => {
  if (!item?.paramsJson) return {}
  try {
    return JSON.parse(item.paramsJson) || {}
  } catch {
    return {}
  }
}

const normalizeQuizBankScenario = (value: any) => {
  const text = String(value || '').trim()
  if (!text) return ''
  if (/(考试|试卷|测验卷|测试卷)/.test(text)) return '考试试卷'
  if (/(课后|作业|练习|习题)/.test(text)) return '课后作业'
  return ''
}

const inferQuizBankScenario = (item: any) => {
  const params: any = readResourceParams(item)
  const explicitScenario = normalizeQuizBankScenario(params.scenario)
  if (explicitScenario) return explicitScenario

  const titleText = `${item?.title || ''} ${params.title || ''} ${params.useCase || ''} ${params.scene || ''}`
  const titleScenario = normalizeQuizBankScenario(titleText)
  if (titleScenario) return titleScenario

  const contentScenario = normalizeQuizBankScenario(String(item?.content || '').slice(0, 300))
  return contentScenario || '课后作业'
}

const isQuizBankImported = (item: any) => {
  if (item?.type !== 'quiz') return false
  const params: any = readResourceParams(item)
  return params.importedToQuizBank === true
}

const parseMicroScript = (item: any) => {
  if (!item?.content) return {}
  try {
    return JSON.parse(item.content)
  } catch {
    return {}
  }
}

const formatSeconds = (value: any) => {
  const seconds = Number(value || 0)
  if (!Number.isFinite(seconds) || seconds <= 0) return '待检测'
  const minutes = Math.floor(seconds / 60)
  const remain = Math.round(seconds % 60)
  return minutes > 0 ? `${minutes}分${String(remain).padStart(2, '0')}秒` : `${remain}秒`
}

const microPreviewVideoUrl = computed(() => readResourceParam(currentPreview.value, 'videoUrl'))
const microPreviewDuration = computed(() => formatSeconds(readResourceParam(currentPreview.value, 'durationSeconds')))
const microPreviewVoice = computed(() => {
  const params: any = readResourceParams(currentPreview.value)
  const stats = safeJson(params.renderStatsJson)
  return stats?.audio?.resolvedVoiceName || stats?.audio?.voiceId || '已生成'
})
const microPreviewSummary = computed(() => {
  const script: any = parseMicroScript(currentPreview.value)
  return script.summary || getSnippet(currentPreview.value?.content || '') || '暂无摘要'
})
const formattedMicroScript = computed(() => {
  const script = parseMicroScript(currentPreview.value)
  return JSON.stringify(script && Object.keys(script).length ? script : currentPreview.value?.content || {}, null, 2)
})
const microPublishDuration = computed(() => formatSeconds(readResourceParam(currentPublishItem.value, 'durationSeconds')))
const publishModalTitle = computed(() => {
  if (currentPublishItem.value?.type === 'micro_video') return '发布微课到课程'
  if (currentPublishItem.value?.type === 'anim') return '发布互动课件'
  return '分发作业至学生端'
})
const publishBannerLabel = computed(() => {
  if (currentPublishItem.value?.type === 'micro_video') return '即将发布微课：'
  if (currentPublishItem.value?.type === 'anim') return '即将发布课件：'
  return '即将发布试卷：'
})

const safeJson = (value: any) => {
  if (!value) return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

const normalizeSearchText = (text: any) =>
  String(text || '')
    .toLowerCase()
    .replace(/\s+/g, '')
    .trim()

const filteredResources = computed(() => {
  const tabList =
    activeTab.value === 'all'
      ? resources.value
      : resources.value.filter((item: any) => item.type === activeTab.value)

  const keyword = normalizeSearchText(searchKeyword.value)

  const searchedList = !keyword
    ? tabList
    : tabList.filter((item: any) => {
      const titleText = normalizeSearchText(item.title)
      const snippetText = normalizeSearchText(getSnippet(item.content))
      const typeText = normalizeSearchText(getTypeLabel(item.type))
      const courseText = normalizeSearchText(item.courseName || item.subject || '')

      return (
        titleText.includes(keyword) ||
        snippetText.includes(keyword) ||
        typeText.includes(keyword) ||
        courseText.includes(keyword)
      )
    })

  return [...searchedList].sort((a: any, b: any) =>
    String(b.createTime || '').localeCompare(String(a.createTime || ''))
  )
})


const getSnippet = (content: string) => {
  if (!content) return ''
  if (content.includes('<!DOCTYPE html>')) {
    return '这是一个交互式 Web 推演组件，包含动画与控制逻辑。'
  }
  if (content.trimStart().startsWith('{') || content.trimStart().startsWith('[')) {
    try {
      const parsed = JSON.parse(content)
      const title = parsed.title || parsed.name || ''
      const desc = parsed.subtitle || parsed.description || ''
      if (title || desc) return `${title}${desc ? ' — ' + desc : ''}`
      return '这是一个交互式 Web 推演组件，包含动画与控制逻辑。'
    } catch {
      return '这是一个交互式 Web 推演组件，包含动画与控制逻辑。'
    }
  }
  const plainText = content.replace(/[#*`>-]/g, '').trim()
  return plainText.length > 50 ? `${plainText.substring(0, 50)}...` : plainText
}

const previewDoc = (item: any) => {
  currentPreview.value = item
  previewVisible.value = true
}

const downloadDoc = (item: any) => {
  let extension: string
  let mimeType: string
  if (item.type === 'anim') {
    const isJson = item.content?.trimStart().startsWith('{') || item.content?.trimStart().startsWith('[')
    extension = isJson ? 'json' : 'html'
    mimeType = isJson ? 'application/json' : 'text/html'
  } else {
    extension = 'md'
    mimeType = 'text/markdown'
  }
  const blob = new Blob([item.content], { type: `${mimeType};charset=utf-8` })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${item.title}_${item.id}.${extension}`
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
  message.success(`已导出为 ${extension.toUpperCase()} 文件`)
}

const deleteDoc = async (id: number) => {
  try {
    await request.post(`/ai/resource/delete/${id}`)
    message.success('已移至回收站')
    void loadResources()
  } catch (error: any) {
    console.error('删除失败:', error)
    message.error(error?.message || '删除失败，请稍后重试')
  }
}

const publishMicroResource = async (item: any) => {
  if (!item?.id) return
  microPublishLoadingId.value = item.id
  try {
    await request.post(`/ai/resource/publish/${item.id}`)
    item.isPublished = 1
    const target = resources.value.find((r) => r.id === item.id)
    if (target) {
      target.isPublished = 1
    }
    message.success('微课视频已发布')
  } catch (error: any) {
    message.error(error?.message || '发布失败，请稍后重试')
  } finally {
    microPublishLoadingId.value = null
  }
}

const importCodingToBank = async (item: any) => {
  if (!item.paramsJson) {
    message.warning('该资源缺少编程题数据')
    return
  }
  try {
    const params = JSON.parse(item.paramsJson)
    const payload = {
      title: params.title || item.title,
      description: params.description,
      difficulty: params.difficulty,
      languages: params.languages,
      timeLimitMs: params.timeLimitMs ?? 5000,
      memoryLimitKb: params.memoryLimitKb ?? 262144,
      templates: params.templates || [],
      testCases: params.testCases || []
    }
    if (!payload.title) {
      message.warning('题目标题不能为空')
      return
    }
    const hiddenCases = payload.testCases.filter((tc: any) => tc.isSample === 0 || tc.isSample === false)
    if (hiddenCases.length === 0) {
      message.warning('请至少添加一个隐藏测试用例（非样例）')
      return
    }
    await request.post('/coding/problem/add', payload)
    message.success('已导入到编程题库')
  } catch (error: any) {
    console.error('导入编程题失败:', error)
    message.error(error?.message || '导入失败')
  }
}

const importQuizToBank = async (item: any) => {
  if (!item?.id) {
    message.warning('资源数据异常，无法导入题库')
    return
  }
  if (!String(item.content || '').trim()) {
    message.warning('该资源缺少习题内容，无法导入题库')
    return
  }

  const targetScenario = inferQuizBankScenario(item)
  const params: any = readResourceParams(item)
  const nextParams = {
    ...params,
    scenario: targetScenario,
    importedToQuizBank: true,
    importedAt: new Date().toISOString(),
    originalScenario: params.originalScenario || params.scenario || undefined,
  }

  quizBankImportLoadingId.value = item.id
  try {
    await request.post('/ai/resource/update', {
      id: item.id,
      type: 'quiz',
      title: item.title,
      content: item.content,
      paramsJson: JSON.stringify(nextParams),
      isPublished: item.isPublished ?? 0,
    })

    item.type = 'quiz'
    item.paramsJson = JSON.stringify(nextParams)
    const target = resources.value.find((resource) => resource.id === item.id)
    if (target) {
      target.type = 'quiz'
      target.paramsJson = item.paramsJson
    }

    message.success(`已导入题库管理，可在「${targetScenario}」中查看`)
  } catch (error: any) {
    console.error('导入试卷题库失败:', error)
    message.error(error?.message || '导入失败，请稍后重试')
  } finally {
    quizBankImportLoadingId.value = null
  }
}

// ==================== 编辑功能 ====================
const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({ id: 0, title: '', content: '' })

// --- 富文本编辑器配置（与学生端随堂笔记保持一致）---
const editorRef = shallowRef()   // 必须用 shallowRef 保存编辑器实例，避免响应式代理冲突
const toolbarConfig = {}
const editorConfig = { placeholder: '请输入正文内容，支持富文本格式...' }

const handleEditorCreated = (editor: any) => {
  editorRef.value = editor
}

// 关闭弹窗时销毁编辑器实例，避免下次打开时残留上次内容或出现重复实例
watch(editVisible, (val) => {
  if (!val) {
    const editor = editorRef.value
    if (editor) {
      editor.destroy()
      editorRef.value = null
    }
  }
})

// 组件卸载时也兜底销毁一次
onUnmounted(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
    editorRef.value = null
  }
})

const openEditModal = (item: any) => {
  editForm.id = item.id
  editForm.title = item.title
  // 历史资源内容多以 Markdown 形式存储，这里先渲染成 HTML 再交给富文本编辑器
  // 若已经是 HTML（例如通过新编辑器保存过一次），renderMd 也会原样保留（MarkdownIt 开启了 html: true）
  editForm.content = renderMd(item.content || '')
  editVisible.value = true
}

const handleEditSave = async () => {
  if (!editForm.title.trim()) {
    return message.warning('标题不能为空')
  }
  // 富文本空内容判定：wangEditor 未初始化或内容为空都视为未填写
  const plain = (editForm.content || '').replace(/<[^>]+>/g, '').replace(/&nbsp;/g, '').trim()
  if (!plain) {
    return message.warning('正文内容不能为空')
  }
  editLoading.value = true
  try {
    await request.post('/ai/resource/update', {
      id: editForm.id,
      title: editForm.title,
      content: editForm.content,
    })
    // 同步本地数据，无需重新请求
    const target = resources.value.find((r) => r.id === editForm.id)
    if (target) {
      target.title = editForm.title
      target.content = editForm.content
    }
    message.success('保存成功')
    editVisible.value = false
  } catch (err: any) {
    message.error(err?.message || err?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    editLoading.value = false
  }
}
// ==================== 编辑功能 end ====================

const publishVisible = ref(false)
const publishLoading = ref(false)
const currentPublishItem = ref<any>(null)
const courseList = ref<any[]>([])
const classList = ref<any[]>([])
const chapterList = ref<any[]>([])

const publishForm = reactive({
  courseId: undefined as number | undefined,
  chapterId: undefined as number | undefined,
  classId: undefined as number | undefined,
  microCourseMode: 'existing',
  courseName: '',
  chapterTitle: '',
  sortOrder: undefined as number | undefined,
  deadline: null as any,
  note: '',
  allowRedo: false,
})

const disabledDeadlineDate = (current: Dayjs) => {
  if (!current) return false
  // 禁止选择今天之前的日期
  return current.endOf('day').valueOf() < Date.now()
}

const disabledDeadlineTime = (current: Dayjs | null) => {
  if (!current) return {}

  const now = dayjs()

  // 只有当选择“今天”时，才限制小时和分钟
  if (!current.isSame(now, 'day')) {
    return {}
  }

  return {
    disabledHours: () => {
      return Array.from({ length: now.hour() }, (_, i) => i)
    },
    disabledMinutes: (selectedHour: number) => {
      if (selectedHour < now.hour()) {
        return Array.from({ length: 60 }, (_, i) => i)
      }
      if (selectedHour === now.hour()) {
        // 当前小时内，当前分钟及之前都不可选
        return Array.from({ length: now.minute() + 1 }, (_, i) => i)
      }
      return []
    },
    disabledSeconds: () => []
  }
}

const extractListFromResponse = (response: any) => {
  const payload = response?.data ?? response

  if (Array.isArray(payload)) {
    return payload
  }

  if (Array.isArray(payload?.records)) {
    return payload.records
  }

  if (Array.isArray(payload?.data)) {
    return payload.data
  }

  return []
}

const fetchCourseList = async () => {
  try {
    const response = await request.get('/course/list/page', {
      params: { current: 1, size: 100 },
      skipErrorToast: true,
    })

    const rawList = extractListFromResponse(response)

    courseList.value = rawList.map((item: any) => ({
      id: Number(item.id),
      name: item.name || item.courseName || item.title || `课程${item.id}`,
    }))
  } catch (error) {
    console.error('获取课程列表失败：', error)
    courseList.value = []
  }
}

const fetchClassList = async () => {
  classList.value = []

  try {
    const response = await request.get('/class/list', {
      skipErrorToast: true,
    })

    const rawList = extractListFromResponse(response)

    classList.value = rawList.map((item: any) => ({
      id: Number(item.id),
      className: item.className || item.class_name || item.name || `班级${item.id}`,
    }))

    if (!classList.value.length) {
      message.warning('未获取到真实班级列表，请检查 /class/list 接口返回')
    }
  } catch (error) {
    console.error('获取班级列表失败：', error)
    classList.value = []
    message.error('获取班级列表失败，暂时不能发布作业')
  }
}

const handleCourseChange = async (courseId: number) => {
  publishForm.chapterId = undefined
  chapterList.value = []

  try {
    const response = await request.get('/chapter/list', {
      params: { courseId },
      skipErrorToast: true,
    })

    const rawList = extractListFromResponse(response)

    chapterList.value = rawList.map((item: any, index: number) => ({
      id: Number(item.id),
      title: item.title || item.chapterTitle || item.name || `章节${item.id}`,
      sortOrder: item.sortOrder ?? item.sort ?? index + 1,
    }))
  } catch (error) {
    console.error('获取章节失败：', error)
    chapterList.value = []
    message.error('获取章节失败')
  }
}



const openPublishModal = (item: any) => {
  currentPublishItem.value = item
  publishForm.classId = undefined
  publishForm.courseId = undefined
  publishForm.chapterId = undefined
  publishForm.microCourseMode = courseList.value.length ? 'existing' : 'new'
  publishForm.courseName = item?.title || ''
  publishForm.chapterTitle = item?.title || ''
  publishForm.sortOrder = undefined
  publishForm.deadline = null
  publishForm.note = ''
  publishForm.allowRedo = false
  chapterList.value = []
  publishVisible.value = true
}

const handlePublish = async () => {
  if (!currentPublishItem.value) {
    return
  }

  publishLoading.value = true

  try {
    if (currentPublishItem.value.type === 'micro_video') {
      if (!readResourceParam(currentPublishItem.value, 'videoUrl')) {
        publishLoading.value = false
        return message.warning('该微课缺少视频地址，无法发布')
      }
      if (publishForm.microCourseMode === 'existing' && !publishForm.courseId) {
        publishLoading.value = false
        return message.warning('请选择目标课程')
      }
      if (publishForm.microCourseMode === 'new' && !publishForm.courseName.trim()) {
        publishLoading.value = false
        return message.warning('请输入新课程名称')
      }
      if (!publishForm.chapterTitle.trim()) {
        publishLoading.value = false
        return message.warning('请输入分集标题')
      }

      const result = await request.post<any, any>('/ai/resource/micro-video/publish-to-course', {
        resourceId: currentPublishItem.value.id,
        courseId: publishForm.microCourseMode === 'existing' ? publishForm.courseId : null,
        courseName: publishForm.microCourseMode === 'new' ? publishForm.courseName.trim() : null,
        chapterTitle: publishForm.chapterTitle.trim(),
        sortOrder: publishForm.sortOrder || null,
      })
      const target = resources.value.find((r) => r.id === currentPublishItem.value.id)
      if (target) {
        target.isPublished = 1
        const params = readResourceParams(target) as any
        target.paramsJson = JSON.stringify({
          ...params,
          publishedCourseId: result?.courseId,
          publishedChapterId: result?.chapterId,
          publishMode: 'course_chapter',
        })
      }
      currentPublishItem.value.isPublished = 1
      message.success('微课已追加到课程分集')
      publishVisible.value = false
      void fetchCourseList()
      return
    } else if (currentPublishItem.value.type === 'anim') {
      if (!publishForm.chapterId) {
        publishLoading.value = false
        return message.warning('请选择要挂载的章节！')
      }

      await request.post('/chapter/update', {
        id: publishForm.chapterId,
        animHtml: currentPublishItem.value.content,
      })
      message.success('交互课件已成功挂载至目标章节！')
    } else {
      if (!publishForm.classId) {
        publishLoading.value = false
        return message.warning('请选择目标班级！')
      }

      const targetClass = classList.value.find(
        (item) => Number(item.id) === Number(publishForm.classId)
      )

      if (!targetClass) {
        publishLoading.value = false
        return message.error('当前选择的班级无效，请重新选择真实班级')
      }

      const deadlineValue = publishForm.deadline
        ? (typeof publishForm.deadline === 'object' && publishForm.deadline.toDate
          ? publishForm.deadline.toDate()
          : new Date(publishForm.deadline))
        : null

      if (deadlineValue && deadlineValue.getTime() <= Date.now()) {
        publishLoading.value = false
        return message.warning('截止时间必须晚于当前时间')
      }

      const assignmentId = await request.post(
        '/homework/assignment/publish',
        {
          quizResourceId: currentPublishItem.value.id,
          classId: Number(publishForm.classId),
          courseId: publishForm.courseId ? Number(publishForm.courseId) : null,
          title: currentPublishItem.value.title,
          teacherNote: publishForm.note || null,
          deadline: deadlineValue,
          allowRedo: publishForm.allowRedo ? 1 : 0,
          maxAttemptCount: publishForm.allowRedo ? 3 : 1,
        },
        {
          successMessage: '作业已成功下发！',
        }
      )

      console.log('发布成功 assignmentId =', assignmentId)
    }

    await request.post(`/ai/resource/publish/${currentPublishItem.value.id}`)
    const target = resources.value.find((r) => r.id === currentPublishItem.value.id)
    if (target) {
      target.isPublished = 1
    }
    publishVisible.value = false
  } catch (err: any) {
    console.error('发布过程异常:', err)
    message.error(err?.message || err?.response?.data?.message || '发布失败，请稍后重试')
  } finally {
    publishLoading.value = false
  }
}
</script>


<style scoped>
.resource-library.modern-page {
  font-family: 'Plus Jakarta Sans', sans-serif;
  animation: fadeIn 0.4s ease;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: transparent;
  padding: 0 !important;
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

.type-icon.analysis { background: #f0fdf4; color: #10b981; }

.page-header {
  margin-bottom: 12px !important; /* 进一步收缩标题组与下方 Tab 的距离 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.title-group h2 { margin: 0; font-size: 28px; font-weight: 800; color: #0f172a; display: flex; align-items: center;}
.title-icon { color: #f59e0b; margin-right: 10px; font-size: 30px; }
.title-group .subtitle { margin: 6px 0 0; color: #64748b; font-size: 15px; }

:deep(.custom-tabs .ant-tabs-nav::before) { border-bottom-color: #e2e8f0; }
:deep(.custom-tabs .ant-tabs-tab) { font-weight: 500; color: #64748b; }
:deep(.custom-tabs .ant-tabs-tab-active) { font-weight: 700; color: #3b82f6; }

.resource-scroll-card {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 24px;
  background: #ffffff;
  border: 1px solid #e8eef6;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(30, 123, 196, 0.05);
}

.resource-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 24px; margin-top: 0; }
.resource-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 5px; padding: 20px; transition: 0.3s; display: flex; flex-direction: column; box-shadow: 0 4px 12px rgba(0,0,0,0.02);}
.resource-card:hover { border-color: #cbd5e1; transform: translateY(-4px); box-shadow: 0 12px 24px -5px rgba(0,0,0,0.08); }

.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.type-icon { width: 40px; height: 40px; border-radius: 5px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
.type-icon.plan { background: #eff6ff; color: #3b82f6; }
.type-icon.quiz { background: #fff7ed; color: #f59e0b; }
.type-icon.anim { background: #fdf2f8; color: #db2777; }
.type-icon.coding { background: #e0e7ff; color: #4f46e5; }
.date { font-size: 13px; color: #94a3b8; font-weight: 500; }

:deep(.custom-tabs) {
  margin-top: -4px;
  flex-shrink: 0;
}

.card-body { flex: 1; margin-bottom: 20px; }
.card-body .title { margin: 0 0 8px 0; font-size: 16px; font-weight: 800; color: #1e293b; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;}
.card-body .snippet { font-size: 13px; color: #64748b; line-height: 1.6; margin: 0; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }

.card-footer { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; padding-top: 16px; border-top: 1px dashed #e2e8f0; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; min-width: 0; }
.tag { font-size: 12px; background: #f1f5f9; color: #475569; padding: 4px 10px; border-radius: 5px; font-weight: 600; display: inline-flex; align-items: center; gap: 4px;}
.success-tag { background: #dcfce7; color: #166534; border: 1px solid #bbf7d0;}
.actions { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; justify-content: flex-end; }

.publish-btn { background: linear-gradient(135deg, #f59e0b, #ea580c); border: none; font-weight: 700; margin-right: 8px; border-radius: 5px; display: flex; align-items: center; gap: 4px;}
.publish-btn:hover { box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3); transform: translateY(-1px); }
.anim-pub-btn { background: linear-gradient(135deg, #ec4899, #be185d); }
.anim-pub-btn:hover { box-shadow: 0 4px 12px rgba(236, 72, 153, 0.3); }
.micro-video-pub-btn { background: linear-gradient(135deg, #0f766e, #14b8a6); }
.micro-video-pub-btn:hover { box-shadow: 0 4px 12px rgba(20, 184, 166, 0.28); }

.action-text-btn { color: #64748b; font-weight: 500; display: inline-flex; align-items: center; gap: 4px;}
.action-text-btn:hover { color: #3b82f6; background: #f8fafc; }

.custom-dropdown-menu { border-radius: 5px; padding: 4px; box-shadow: 0 10px 25px rgba(0,0,0,0.08);}
.text-danger { color: #ef4444 !important; }
.text-danger:hover { background: #fef2f2 !important; color: #dc2626 !important; }

.micro-video-preview video {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #0f172a;
  border-radius: 6px;
  margin-bottom: 16px;
}

.micro-video-preview {
  height: 100%;
  max-height: none;
  overflow-y: auto;
  padding-right: 8px;
}

.micro-video-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  color: #64748b;
  background: #f8fafc;
  margin-bottom: 16px;
}

.micro-preview-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.micro-meta-item {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px 14px;
  background: #fff;
}

.micro-meta-item span {
  display: block;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 4px;
}

.micro-meta-item strong {
  color: #0f172a;
  font-size: 15px;
}

.micro-preview-summary {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  padding: 14px 16px;
  margin-bottom: 10px;
}

.micro-preview-summary h4 {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 14px;
}

.micro-preview-summary p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.micro-advanced pre {
  max-height: 260px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  padding: 12px;
  border-radius: 6px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.6;
}

.micro-publish-preview {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 14px;
  align-items: center;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  margin-bottom: 18px;
}

.micro-publish-preview video {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 5px;
  background: #0f172a;
}

.micro-publish-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.micro-publish-copy strong {
  color: #0f172a;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.micro-publish-copy span {
  color: #64748b;
  font-size: 13px;
}

.preview-content {
  flex: 1;
  min-height: 0;
  height: 100%;
  max-height: none;
  overflow-y: auto;
  padding-right: 16px;
}
.anim-preview-box {
  flex: 1;
  min-height: 0;
  height: 100%;
  border-radius: 5px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  max-height: none;
}
.anim-parse-error { padding: 60px 20px; text-align: center; color: #94a3b8; font-size: 15px; background: #f8fafc; }
:deep(.doc-style) { color: #334155; font-family: 'SimSun', 'Microsoft YaHei', sans-serif; line-height: 1.8; }
:deep(.doc-style h1) { font-size: 22px; text-align: center; color: #0f172a; margin-bottom: 20px; border-bottom: 2px solid #e2e8f0; padding-bottom: 12px;}
:deep(.doc-style h2) { font-size: 16px; color: #1e293b; background: #f8fafc; padding: 8px 12px; border-left: 4px solid #3b82f6; margin: 20px 0 12px; }

.modal-custom-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 700; color: #1e293b; }
.m-icon { font-size: 18px; }
.m-icon.anim-icon { color: #db2777; }
.m-icon.quiz-icon { color: #f59e0b; }

.publish-info-banner { border-radius: 5px; padding: 16px; display: flex; gap: 16px; align-items: center; margin-bottom: 24px; }
.publish-info-banner.quiz { background: #fff7ed; border: 1px solid #ffedd5; }
.publish-info-banner.quiz .label { color: #d97706; }
.publish-info-banner.quiz strong { color: #9a3412; }

.publish-info-banner.anim { background: #fdf2f8; border: 1px solid #fce7f3; }
.publish-info-banner.anim .label { color: #db2777; }
.publish-info-banner.anim strong { color: #9d174d; }

.banner-icon { font-size: 28px; color: inherit; opacity: 0.8;}
.info-text { display: flex; flex-direction: column; }
.info-text .label { font-size: 13px; font-weight: 600;}
.info-text strong { font-size: 16px; margin-top: 4px; color: #1e293b;}
.publish-form { margin-top: 10px; }

.graph-tag { background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; }

/* ==================== 编辑弹窗样式 ==================== */
.edit-form { margin-top: 4px; }

/* 富文本编辑器外层容器：与学生端随堂笔记保持视觉风格一致 */
.edit-wysiwyg-box {
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  overflow: hidden;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.edit-wysiwyg-box:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}
.edit-toolbar {
  border-bottom: 1px solid #e2e8f0;
}

/* 让编辑区内文字阅读感与预览保持接近 */
:deep(.edit-editor .w-e-text-container) {
  background: #fff;
}
:deep(.edit-editor .w-e-text-container [data-slate-editor]) {
  font-size: 14px;
  line-height: 1.8;
  color: #334155;
  padding: 16px 24px;
}
/* ===================================================== */
.header-actions {
  margin-left: auto;
  width: 360px;
  max-width: 100%;
  flex-shrink: 0;
  margin-top: 0;
}

/* 整体容器增加微小阴影提升质感 */
.resource-search {
  width: 100%;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.02));
  transition: all 0.3s ease;
}

.resource-search:hover {
  filter: drop-shadow(0 4px 12px rgba(59, 130, 246, 0.12));
}

:deep(.resource-search .ant-input-group-addon .ant-btn) {
  height: 42px !important;
  border-radius: 0 5px 5px 0 !important;
  border: 1px solid #e2e8f0;
  background-color: #fff;
  color: #64748b;
  padding: 0 20px;
  box-shadow: none;
  transition: all 0.3s ease;
  /* === 新增以下代码，强制图标绝对居中 === */
  display: flex !important;
  align-items: center;
  justify-content: center;
}

:deep(.resource-search .ant-input-group-wrapper) {
  width: 100%;
}

/* 1. 关键修复：针对带有 allowClear 的外层 wrapper 设置边框和圆角 */
:deep(.resource-search .ant-input-affix-wrapper) {
  height: 42px;
  border-radius: 5px 0 0 5px !important;
  border: 1px solid #e2e8f0;
  border-right: none; /* 移除右边框避免与按钮重叠变粗 */
  padding-left: 16px;
  box-shadow: none !important;
}

/* 2. wrapper 的聚焦状态 */
:deep(.resource-search .ant-input-affix-wrapper-focused),
:deep(.resource-search .ant-input-affix-wrapper:focus-within) {
  border-color: #3b82f6 !important;
  border-right: 1px solid #3b82f6 !important;
  box-shadow: none !important;
  z-index: 2; /* 确保蓝色边框盖住按钮的默认边框 */
}

/* 3. 去除内部真实 input 的自带边框，防止出现“框中框” */
:deep(.resource-search .ant-input) {
  height: 100% !important;
  border: none !important;
  padding-left: 0 !important; /* padding 已经交给了外层 wrapper */
  box-shadow: none !important;
  background: transparent !important;
}

/* 确保内部 input 聚焦时不会出现原生的黑框 */
:deep(.resource-search .ant-input:focus) {
  outline: none !important;
}

/* 搜索按钮：左侧直角对接输入框，右侧 5px 圆角 */
:deep(.resource-search .ant-input-group-addon .ant-btn) {
  height: 42px !important;
  border-radius: 0 5px 5px 0 !important;
  border: 1px solid #e2e8f0;
  background-color: #fff;
  color: #64748b;
  padding: 0 20px;
  box-shadow: none;
  transition: all 0.3s ease;
}

/* 搜索按钮悬浮/点击状态 */
:deep(.resource-search .ant-input-group-addon .ant-btn:hover),
:deep(.resource-search .ant-input-group-addon .ant-btn:focus) {
  background-color: #f8fafc;
  color: #3b82f6;
  border-color: #3b82f6;
  z-index: 2;
}
/* =============================================================== */
:global(.teacher-wide-modal) {
  max-width: calc(100vw - 48px);
}
</style>
