<template>
  <div class="knowledge-page">
    <section class="status-panel" :class="{ 'is-ready': status?.configured }">
      <div class="status-icon">
        <DatabaseOutlined />
      </div>
      <div class="status-copy">
        <div class="eyebrow">PRIVATE KNOWLEDGE · SPARK CHATDOC</div>
        <h2>星火课程知识库</h2>
        <p>
          学生提问先检索课程私有资料，再由现有 AI 模型组织答案。命中内容会以资料编号引用，
          未达到可信阈值时不会冒充课程依据。
        </p>
        <div class="status-meta">
          <a-tag :color="status?.configured ? 'success' : 'warning'">
            {{ status?.configured ? '服务已就绪' : '等待密钥配置' }}
          </a-tag>
          <span>检索 {{ status?.topN ?? 5 }} 条</span>
          <span>可信阈值 {{ status?.minimumScore ?? 45 }}</span>
          <span class="endpoint">{{ status?.baseUrl || 'https://chatdoc.xfyun.cn' }}</span>
        </div>
      </div>
      <div class="status-actions">
        <a-button :loading="pageLoading" @click="refreshAll">
          <template #icon><ReloadOutlined /></template>
          刷新状态
        </a-button>
        <a-button type="primary" :disabled="!status?.configured" @click="createModalOpen = true">
          <template #icon><PlusOutlined /></template>
          新建知识库
        </a-button>
      </div>
    </section>

    <a-alert
      v-if="status && !status.configured"
      class="config-alert"
      type="warning"
      show-icon
      message="后端尚未完成星火知识库配置"
      description="请在后端启动环境中配置 XFYUN_KNOWLEDGE_ENABLED=true、XFYUN_KNOWLEDGE_APP_ID 和 XFYUN_KNOWLEDGE_SECRET，重启后端后再刷新本页。密钥不会传到浏览器。"
    />

    <section class="workspace-card">
      <aside class="repo-rail">
        <div class="section-heading compact">
          <div>
            <span class="section-index">01</span>
            <h3>知识库</h3>
          </div>
          <span>{{ repositories.length }} 个</span>
        </div>

        <a-spin :spinning="repoLoading">
          <div v-if="repositories.length" class="repo-list">
            <button
              v-for="repo in repositories"
              :key="repo.repoId"
              type="button"
              class="repo-item"
              :class="{ active: selectedRepoId === repo.repoId }"
              @click="selectRepository(repo)"
            >
              <span class="repo-mark"><FolderOpenOutlined /></span>
              <span class="repo-content">
                <strong>{{ repo.repoName }}</strong>
                <small>{{ repo.repoDesc || '暂无说明' }}</small>
                <em>{{ repo.repoId }}</em>
              </span>
              <RightOutlined />
            </button>
          </div>
          <a-empty v-else description="还没有可用知识库" :image="simpleImage">
            <a-button type="primary" :disabled="!status?.configured" @click="createModalOpen = true">
              创建第一个知识库
            </a-button>
          </a-empty>
        </a-spin>
      </aside>

      <main class="document-pane">
        <div class="section-heading">
          <div>
            <span class="section-index">02</span>
            <h3>{{ selectedRepo?.repoName || '知识文档' }}</h3>
            <p>支持 PDF、Word、Markdown、TXT、Excel、PPT 等格式，单文件不超过 20MB。</p>
          </div>
          <div class="document-actions">
            <input
              ref="fileInput"
              class="native-file-input"
              type="file"
              accept=".txt,.md,.doc,.docx,.pdf,.xls,.xlsx,.csv,.jpg,.jpeg,.png,.ppt,.pptx"
              @change="handleFileSelected"
            />
            <a-button :disabled="!selectedRepo || !status?.configured" :loading="uploadLoading" @click="openFilePicker">
              <template #icon><UploadOutlined /></template>
              上传资料
            </a-button>
            <a-button
              type="primary"
              ghost
              :disabled="!selectedRepo || !status?.configured"
              :loading="starterLoading"
              @click="handleStarterPack"
            >
              导入数据结构资料包
            </a-button>
          </div>
        </div>

        <div v-if="selectedRepo" class="repo-summary">
          <div>
            <span>可检索资料</span>
            <strong>{{ readyFileCount }}</strong>
          </div>
          <div>
            <span>处理中</span>
            <strong>{{ processingFileCount }}</strong>
          </div>
          <div>
            <span>已绑定课程</span>
            <strong>{{ selectedRepoCourseCount }}</strong>
          </div>
          <a-button type="text" :loading="fileLoading" @click="refreshSelectedFiles">
            <template #icon><SyncOutlined /></template>
            同步状态
          </a-button>
        </div>

        <a-table
          row-key="fileId"
          :columns="fileColumns"
          :data-source="files"
          :loading="fileLoading"
          :pagination="{ pageSize: 7, hideOnSinglePage: true }"
          :scroll="{ x: 760 }"
          :locale="{ emptyText: selectedRepo ? '该知识库还没有资料' : '请先选择左侧知识库' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'fileName'">
              <div class="file-name-cell">
                <span class="file-icon"><FileTextOutlined /></span>
                <div>
                  <strong>{{ record.fileName }}</strong>
                  <small>{{ record.extName || record.fileType || '文档' }} · {{ record.fileId }}</small>
                </div>
              </div>
            </template>
            <template v-else-if="column.dataIndex === 'fileStatus'">
              <a-tag :color="fileStatusInfo(record.fileStatus).color">
                {{ fileStatusInfo(record.fileStatus).text }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'quantity'">
              {{ record.quantity ?? '—' }}
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              {{ formatDate(record.createTime) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger @click="confirmDeleteFile(record)">删除</a-button>
            </template>
          </template>
        </a-table>
      </main>
    </section>

    <section class="binding-card">
      <div class="section-heading">
        <div>
          <span class="section-index">03</span>
          <h3>课程检索路由</h3>
          <p>一个课程绑定一个知识库。关键词用于学生在通用 AI 助手中提问时自动选择对应课程资料。</p>
        </div>
        <a-input-search
          v-model:value="courseKeyword"
          class="course-search"
          allow-clear
          placeholder="搜索课程或教师"
        />
      </div>

      <a-table
        row-key="courseId"
        :columns="courseColumns"
        :data-source="filteredCourses"
        :loading="courseLoading"
        :pagination="{ pageSize: 8, showSizeChanger: false }"
        :scroll="{ x: 960 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'courseName'">
            <div class="course-name-cell">
              <strong>{{ record.courseName }}</strong>
              <small>{{ record.sourceType === 'platform' ? '平台课程' : record.teacherName || '教师课程' }}</small>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'repoName'">
            <span v-if="record.repoId" class="binding-name">
              <LinkOutlined /> {{ record.repoName || record.repoId }}
            </span>
            <span v-else class="muted">尚未绑定</span>
          </template>
          <template v-else-if="column.dataIndex === 'keywords'">
            <span class="keyword-line">{{ record.keywords || '—' }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'syncStatus'">
            <a-tag :color="syncStatusInfo(record.syncStatus).color">
              {{ syncStatusInfo(record.syncStatus).text }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openBindModal(record)">{{ record.repoId ? '调整' : '绑定' }}</a-button>
              <a-button v-if="record.repoId" type="link" danger @click="confirmUnbind(record)">解绑</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="createModalOpen"
      title="创建星火知识库"
      centered
      :confirm-loading="createLoading"
      @ok="handleCreateRepository"
    >
      <a-form layout="vertical">
        <a-form-item label="知识库名称" required>
          <a-input v-model:value="createForm.name" :maxlength="120" placeholder="例如：数据结构课程知识库" />
        </a-form-item>
        <a-form-item label="用途说明">
          <a-textarea v-model:value="createForm.description" :rows="3" :maxlength="500" placeholder="说明覆盖的课程、资料范围和维护责任" />
        </a-form-item>
        <a-form-item label="标签">
          <a-input v-model:value="createForm.tags" :maxlength="200" placeholder="例如：数据结构,考研,就业实战" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="bindModalOpen"
      title="绑定课程知识库"
      centered
      :confirm-loading="bindLoading"
      @ok="handleBindCourse"
    >
      <div class="modal-course-name">{{ bindForm.courseName }}</div>
      <a-form layout="vertical">
        <a-form-item label="星火知识库" required>
          <a-select
            v-model:value="bindForm.repoId"
            show-search
            option-filter-prop="label"
            placeholder="请选择知识库"
            :options="repositoryOptions"
          />
        </a-form-item>
        <a-form-item label="自动路由关键词">
          <a-textarea
            v-model:value="bindForm.keywords"
            :rows="3"
            :maxlength="500"
            placeholder="用逗号分隔，例如：数据结构,链表,二叉树,图,排序,408"
          />
          <div class="form-help">通用 AI 助手会用课程名和这些关键词选择知识库，不会把学校层次作为能力判断。</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Empty, Modal, message } from 'ant-design-vue'
import {
  DatabaseOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  LinkOutlined,
  PlusOutlined,
  ReloadOutlined,
  RightOutlined,
  SyncOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import {
  bindCourseKnowledgeRepository,
  createKnowledgeRepository,
  deleteKnowledgeFile,
  getCourseKnowledgeBindings,
  getKnowledgeBaseStatus,
  getKnowledgeRepositories,
  getKnowledgeRepositoryFiles,
  refreshKnowledgeFileStatuses,
  unbindCourseKnowledgeRepository,
  uploadKnowledgeFile,
  uploadKnowledgeStarterPack
} from '@/api/admin'
import type {
  CourseKnowledgeBinding,
  KnowledgeBaseStatus,
  KnowledgeFileItem,
  KnowledgeRepositoryItem
} from '@/types/admin'

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const status = ref<KnowledgeBaseStatus | null>(null)
const repositories = ref<KnowledgeRepositoryItem[]>([])
const courses = ref<CourseKnowledgeBinding[]>([])
const files = ref<KnowledgeFileItem[]>([])
const selectedRepoId = ref('')
const pageLoading = ref(false)
const repoLoading = ref(false)
const courseLoading = ref(false)
const fileLoading = ref(false)
const uploadLoading = ref(false)
const starterLoading = ref(false)
const createLoading = ref(false)
const bindLoading = ref(false)
const createModalOpen = ref(false)
const bindModalOpen = ref(false)
const courseKeyword = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

const createForm = reactive({ name: '', description: '', tags: '' })
const bindForm = reactive({ courseId: 0, courseName: '', repoId: '', keywords: '' })

const selectedRepo = computed(() => repositories.value.find(item => item.repoId === selectedRepoId.value))
const repositoryOptions = computed(() => repositories.value.map(item => ({ label: item.repoName, value: item.repoId })))
const readyFileCount = computed(() => files.value.filter(item => item.fileStatus === 'vectored').length)
const processingFileCount = computed(() => files.value.filter(item => item.fileStatus !== 'vectored' && item.fileStatus !== 'failed').length)
const selectedRepoCourseCount = computed(() => courses.value.filter(item => item.repoId === selectedRepoId.value).length)
const filteredCourses = computed(() => {
  const keyword = courseKeyword.value.trim().toLowerCase()
  if (!keyword) return courses.value
  return courses.value.filter(item => [item.courseName, item.teacherName, item.repoName, item.keywords]
    .some(value => String(value || '').toLowerCase().includes(keyword)))
})

const fileColumns = [
  { title: '文档', dataIndex: 'fileName', minWidth: 300 },
  { title: '向量状态', dataIndex: 'fileStatus', width: 120 },
  { title: '切片数', dataIndex: 'quantity', width: 90 },
  { title: '上传时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const }
]

const courseColumns = [
  { title: '课程', dataIndex: 'courseName', width: 250 },
  { title: '绑定知识库', dataIndex: 'repoName', width: 220 },
  { title: '路由关键词', dataIndex: 'keywords' },
  { title: '资料状态', dataIndex: 'syncStatus', width: 110 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const }
]

const loadStatus = async () => {
  status.value = await getKnowledgeBaseStatus()
}

const loadRepositories = async () => {
  if (!status.value?.configured) {
    repositories.value = []
    return
  }
  repoLoading.value = true
  try {
    repositories.value = await getKnowledgeRepositories() || []
    if (!repositories.value.some(item => item.repoId === selectedRepoId.value)) {
      selectedRepoId.value = repositories.value[0]?.repoId || ''
    }
  } finally {
    repoLoading.value = false
  }
}

const loadCourses = async () => {
  courseLoading.value = true
  try {
    courses.value = await getCourseKnowledgeBindings() || []
  } finally {
    courseLoading.value = false
  }
}

const loadFiles = async () => {
  if (!selectedRepoId.value || !status.value?.configured) {
    files.value = []
    return
  }
  fileLoading.value = true
  try {
    files.value = await getKnowledgeRepositoryFiles(selectedRepoId.value) || []
  } finally {
    fileLoading.value = false
  }
}

const refreshAll = async () => {
  pageLoading.value = true
  try {
    await loadStatus()
    await Promise.all([loadRepositories(), loadCourses()])
    await loadFiles()
  } catch (error) {
    console.error('加载知识库管理数据失败', error)
  } finally {
    pageLoading.value = false
  }
}

const selectRepository = async (repo: KnowledgeRepositoryItem) => {
  selectedRepoId.value = repo.repoId
  await loadFiles()
}

const refreshSelectedFiles = async () => {
  if (!selectedRepoId.value) return
  fileLoading.value = true
  try {
    const ids = files.value.map(item => item.fileId).filter(Boolean)
    if (ids.length) await refreshKnowledgeFileStatuses(selectedRepoId.value, ids)
    await Promise.all([loadFiles(), loadCourses()])
    message.success('向量状态已同步')
  } finally {
    fileLoading.value = false
  }
}

const handleCreateRepository = async () => {
  if (!createForm.name.trim()) {
    message.warning('请输入知识库名称')
    return
  }
  createLoading.value = true
  try {
    const created = await createKnowledgeRepository({
      name: createForm.name.trim(),
      description: createForm.description.trim(),
      tags: createForm.tags.trim()
    })
    createModalOpen.value = false
    Object.assign(createForm, { name: '', description: '', tags: '' })
    await loadRepositories()
    selectedRepoId.value = created.repoId
    await loadFiles()
    message.success('知识库创建成功')
  } finally {
    createLoading.value = false
  }
}

const openFilePicker = () => fileInput.value?.click()

const handleFileSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !selectedRepoId.value) return
  if (file.size > 20 * 1024 * 1024) {
    message.error('单个文件不能超过 20MB')
    return
  }
  uploadLoading.value = true
  try {
    await uploadKnowledgeFile(selectedRepoId.value, file)
    await Promise.all([loadFiles(), loadCourses()])
    message.success('资料已完成解析并加入知识库')
  } finally {
    uploadLoading.value = false
  }
}

const handleStarterPack = () => {
  if (!selectedRepoId.value) return
  Modal.confirm({
    title: '导入内置课程资料？',
    content: '将数据结构考研理论、就业实战和平台 AI 助手说明合并为 1 份结构化资料上传，以节省文件额度。请勿重复点击导入。',
    okText: '确认导入',
    cancelText: '取消',
    centered: true,
    async onOk() {
      starterLoading.value = true
      try {
        const result = await uploadKnowledgeStarterPack(selectedRepoId.value)
        await Promise.all([loadFiles(), loadCourses()])
        message.success(`已提交 ${result.length} 份资料，等待向量化`)
      } finally {
        starterLoading.value = false
      }
    }
  })
}

const confirmDeleteFile = (record: KnowledgeFileItem) => {
  Modal.confirm({
    title: '删除知识文档？',
    content: `“${record.fileName}”及其向量内容将从星火知识库中删除。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      await deleteKnowledgeFile(selectedRepoId.value, record.fileId, record.fileName)
      await Promise.all([loadFiles(), loadCourses()])
      message.success('资料已删除')
    }
  })
}

const openBindModal = (record: CourseKnowledgeBinding) => {
  bindForm.courseId = record.courseId
  bindForm.courseName = record.courseName
  bindForm.repoId = record.repoId || selectedRepoId.value || ''
  bindForm.keywords = record.keywords || ''
  bindModalOpen.value = true
}

const handleBindCourse = async () => {
  const repo = repositories.value.find(item => item.repoId === bindForm.repoId)
  if (!repo) {
    message.warning('请选择要绑定的知识库')
    return
  }
  bindLoading.value = true
  try {
    await bindCourseKnowledgeRepository({
      courseId: bindForm.courseId,
      repoId: repo.repoId,
      repoName: repo.repoName,
      keywords: bindForm.keywords.trim()
    })
    bindModalOpen.value = false
    await loadCourses()
    message.success('课程知识库绑定成功')
  } finally {
    bindLoading.value = false
  }
}

const confirmUnbind = (record: CourseKnowledgeBinding) => {
  Modal.confirm({
    title: '解除课程知识库绑定？',
    content: `解绑后，“${record.courseName}”的 AI 问答将不再检索该知识库。`,
    okText: '解除绑定',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      await unbindCourseKnowledgeRepository(record.courseId)
      await loadCourses()
      message.success('已解除绑定')
    }
  })
}

const fileStatusInfo = (value?: string) => {
  const map: Record<string, { text: string; color: string }> = {
    vectored: { text: '可检索', color: 'success' },
    failed: { text: '处理失败', color: 'error' },
    texted: { text: '文本化完成', color: 'processing' },
    ocring: { text: 'OCR 识别中', color: 'processing' },
    spliting: { text: '切分中', color: 'processing' },
    splited: { text: '等待向量化', color: 'warning' },
    vectoring: { text: '向量化中', color: 'processing' },
    parsing: { text: '解析中', color: 'processing' },
    uploaded: { text: '等待处理', color: 'warning' }
  }
  return map[String(value || '').toLowerCase()] || { text: value || '处理中', color: 'processing' }
}

const syncStatusInfo = (value?: string) => {
  const map: Record<string, { text: string; color: string }> = {
    ready: { text: '资料就绪', color: 'success' },
    processing: { text: '处理中', color: 'processing' },
    failed: { text: '存在失败', color: 'error' },
    empty: { text: '暂无资料', color: 'default' }
  }
  return map[value || 'empty'] || map.empty
}

const formatDate = (value?: string) => value ? String(value).replace('T', ' ').slice(0, 19) : '—'

onMounted(refreshAll)
</script>

<style scoped>
.knowledge-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: #172033;
}

.status-panel,
.workspace-card,
.binding-card {
  background: #fff;
  border: 1px solid #e5ebf3;
  border-radius: 18px;
  box-shadow: 0 14px 36px rgba(30, 50, 80, 0.06);
}

.status-panel {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  padding: 28px 30px;
  border-left: 4px solid #e2a42d;
}

.status-panel.is-ready { border-left-color: #18a574; }
.status-icon { width: 58px; height: 58px; display: grid; place-items: center; border-radius: 15px; color: #3563e9; background: #edf3ff; font-size: 26px; }
.eyebrow { margin-bottom: 6px; color: #64748b; font-size: 11px; font-weight: 800; letter-spacing: .14em; }
.status-copy h2 { margin: 0; font-size: 25px; line-height: 1.25; }
.status-copy > p { max-width: 820px; margin: 8px 0 12px; color: #64748b; line-height: 1.7; }
.status-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 18px; color: #607087; font-size: 13px; }
.endpoint { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: #8390a4; }
.status-actions { display: flex; gap: 10px; }
.config-alert { border-radius: 12px; }

.workspace-card { display: grid; grid-template-columns: 320px minmax(0, 1fr); min-height: 520px; overflow: hidden; }
.repo-rail { padding: 24px 18px; background: #f8fafc; border-right: 1px solid #e8edf4; }
.document-pane { min-width: 0; padding: 24px 26px; }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 20px; }
.section-heading > div:first-child { min-width: 0; }
.section-heading h3 { display: inline; margin: 0 0 0 10px; font-size: 20px; }
.section-heading p { margin: 7px 0 0 40px; color: #748197; line-height: 1.6; }
.section-heading.compact { align-items: center; padding: 0 8px; }
.section-heading.compact > span { color: #8a97aa; font-size: 13px; }
.section-index { display: inline-grid; place-items: center; width: 30px; height: 24px; color: #3563e9; background: #eaf0ff; border-radius: 7px; font-size: 11px; font-weight: 800; }
.document-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; }
.native-file-input { display: none; }

.repo-list { display: flex; flex-direction: column; gap: 8px; }
.repo-item { width: 100%; display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: 12px; align-items: center; padding: 14px; border: 1px solid transparent; border-radius: 12px; color: inherit; background: transparent; text-align: left; cursor: pointer; transition: .18s ease; }
.repo-item:hover { background: #fff; border-color: #dce5f2; }
.repo-item.active { background: #fff; border-color: #9db8ff; box-shadow: 0 7px 20px rgba(53, 99, 233, .09); }
.repo-mark { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 10px; color: #3563e9; background: #eaf0ff; font-size: 17px; }
.repo-content { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.repo-content strong, .repo-content small, .repo-content em { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.repo-content strong { font-size: 14px; }
.repo-content small { color: #758399; }
.repo-content em { color: #a0aabd; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10px; font-style: normal; }

.repo-summary { display: grid; grid-template-columns: repeat(3, minmax(110px, 160px)) minmax(0, 1fr); gap: 10px; align-items: stretch; margin-bottom: 18px; padding: 14px; border: 1px solid #e8edf4; background: #fafcff; border-radius: 12px; }
.repo-summary > div { display: flex; flex-direction: column; gap: 3px; padding: 0 12px; border-right: 1px solid #e3e9f1; }
.repo-summary span { color: #7b879a; font-size: 12px; }
.repo-summary strong { font-size: 21px; }
.repo-summary > button { justify-self: end; }
.file-name-cell { display: flex; align-items: center; gap: 11px; }
.file-icon { flex: 0 0 auto; width: 34px; height: 38px; display: grid; place-items: center; border-radius: 8px; color: #3563e9; background: #eef3ff; }
.file-name-cell > div, .course-name-cell { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.file-name-cell strong, .file-name-cell small, .course-name-cell strong, .course-name-cell small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-name-cell small, .course-name-cell small { color: #8793a6; font-size: 11px; }

.binding-card { padding: 24px 26px; }
.course-search { width: 250px; }
.binding-name { color: #3159ca; font-weight: 600; }
.keyword-line { display: block; max-width: 420px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #637086; }
.muted { color: #a1aabc; }
.modal-course-name { margin-bottom: 18px; padding: 12px 14px; border-left: 3px solid #3563e9; background: #f3f6fc; border-radius: 4px 10px 10px 4px; font-weight: 700; }
.form-help { margin-top: 7px; color: #8994a7; font-size: 12px; line-height: 1.6; }

@media (max-width: 1180px) {
  .status-panel { grid-template-columns: auto 1fr; }
  .status-actions { grid-column: 1 / -1; justify-content: flex-end; }
  .workspace-card { grid-template-columns: 260px minmax(0, 1fr); }
}

@media (max-width: 880px) {
  .status-panel { grid-template-columns: 1fr; padding: 22px; }
  .status-icon { display: none; }
  .status-actions { justify-content: flex-start; }
  .workspace-card { grid-template-columns: 1fr; }
  .repo-rail { border-right: 0; border-bottom: 1px solid #e8edf4; }
  .repo-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); }
  .section-heading { flex-direction: column; }
  .section-heading p { margin-left: 0; }
  .repo-summary { grid-template-columns: repeat(3, 1fr); }
  .repo-summary > button { grid-column: 1 / -1; justify-self: start; }
  .course-search { width: 100%; }
}
</style>
