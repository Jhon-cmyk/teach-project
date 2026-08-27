<template>
  <div class="tutorial-admin-page">
    <section class="hero-panel">
      <div>
        <h2>图文教程管理</h2>
        <p>管理学生端“文字教程”的课程封面、简介和章节正文，保存后直接写入 text_course 与 text_node。</p>
      </div>
      <a-button type="primary" size="large" @click="openCreate">
        <template #icon><PlusOutlined /></template>
        新增图文教程
      </a-button>
    </section>

    <section class="toolbar-panel">
      <a-input
        v-model:value="query.name"
        class="search-input"
        placeholder="按教程名称搜索"
        allow-clear
        @pressEnter="handleSearch"
      >
        <template #prefix><SearchOutlined /></template>
      </a-input>
      <a-button type="primary" @click="handleSearch">查询</a-button>
      <a-button @click="handleReset">
        <template #icon><ReloadOutlined /></template>
        重置
      </a-button>
    </section>

    <a-card class="table-card" :bordered="false">
      <a-table
        class="tutorial-table"
        row-key="id"
        :columns="columns"
        :data-source="tutorialList"
        :loading="loading"
        :pagination="pagination"
        :locale="{ emptyText }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'coverImg'">
            <div class="cover-cell">
              <img v-if="record.coverImg" :src="record.coverImg" :alt="record.name" />
              <span v-else>暂无封面</span>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'name'">
            <div class="course-title">{{ record.name }}</div>
          </template>

          <template v-else-if="column.dataIndex === 'description'">
            <div class="desc-cell">{{ record.description || '暂无简介' }}</div>
          </template>

          <template v-else-if="column.dataIndex === 'nodeCount'">
            <a-tag color="blue">{{ record.nodeCount || 0 }} 章</a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>

          <template v-else-if="column.key === 'action'">
            <div class="action-group">
              <a-button type="link" @click="openEdit(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button type="link" danger @click="handleDelete(record)">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="formState.id ? '编辑图文教程' : '新增图文教程'"
      width="1100px"
      centered
      class="tutorial-edit-modal"
      destroy-on-close
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" layout="vertical" :model="formState" :rules="rules">
        <div class="editor-layout">
          <section class="form-panel">
            <div class="panel-title">
              <strong>基础信息</strong>
              <span>学生端卡片展示的主信息。</span>
            </div>

            <a-form-item label="教程名称" name="name">
              <a-input v-model:value="formState.name" placeholder="例如：Java Web 入门教程" maxlength="80" />
            </a-form-item>

            <a-form-item label="教程简介" name="description">
              <a-textarea
                v-model:value="formState.description"
                placeholder="学生端卡片展示的简介"
                :rows="4"
                maxlength="500"
                show-count
              />
            </a-form-item>

            <a-form-item label="教程封面">
              <div class="cover-upload">
                <div class="cover-preview">
                  <img v-if="formState.coverImg" :src="formState.coverImg" alt="教程封面" />
                  <span v-else>封面预览</span>
                </div>
                <div class="upload-actions">
                  <a-upload :show-upload-list="false" accept="image/*" :custom-request="handleCoverUpload">
                    <a-button :loading="uploading">
                      <template #icon><UploadOutlined /></template>
                      上传封面
                    </a-button>
                  </a-upload>
                  <a-input v-model:value="formState.coverImg" placeholder="也可以粘贴图片 URL" />
                </div>
              </div>
            </a-form-item>
          </section>

          <section class="node-panel">
            <div class="node-head">
              <div>
                <strong>章节内容</strong>
                <span>支持富文本排版，学生端阅读页会按章节渲染。</span>
              </div>
              <a-button type="primary" ghost class="add-node-btn" @click="addNode">
                <template #icon><PlusOutlined /></template>
                添加章节
              </a-button>
            </div>

            <div class="node-list">
              <div v-for="(node, index) in formState.nodes" :key="node.localKey" class="node-card">
                <div class="node-card-head">
                  <span class="node-index">{{ index + 1 }}</span>
                  <a-input v-model:value="node.title" placeholder="章节标题" />
                  <a-button danger type="text" :disabled="formState.nodes.length <= 1" @click="removeNode(index)">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </div>
                <RichTextEditor
                  v-model:modelValue="node.content"
                  class="chapter-rich-editor"
                  placeholder="在这里编辑章节正文，可设置标题、加粗、列表、引用和链接"
                  height="210px"
                />
              </div>
            </div>
          </section>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import {
  deleteAdminTutorial,
  getAdminTutorialDetail,
  getAdminTutorialList,
  saveAdminTutorial,
  uploadAdminFile
} from '@/api/admin'
import RichTextEditor from '@/components/RichTextEditor.vue'
import type {
  AdminTutorialFormData,
  AdminTutorialItem,
  AdminTutorialListParams,
  AdminTutorialNodeItem
} from '@/types/admin'

type NodeFormItem = Omit<AdminTutorialNodeItem, 'content'> & {
  localKey: string
  content: string
}

const loading = ref(false)
const submitLoading = ref(false)
const uploading = ref(false)
const modalOpen = ref(false)
const formRef = ref()
const tutorialList = ref<AdminTutorialItem[]>([])
const total = ref(0)

const query = reactive<AdminTutorialListParams>({
  current: 1,
  size: 10,
  name: ''
})

const formState = reactive<AdminTutorialFormData & { nodes: NodeFormItem[] }>({
  id: undefined,
  name: '',
  description: '',
  coverImg: '',
  nodes: []
})

const columns = [
  { title: '封面', dataIndex: 'coverImg', width: 104 },
  { title: '教程名称', dataIndex: 'name', width: 210 },
  { title: '简介', dataIndex: 'description' },
  { title: '章节数', dataIndex: 'nodeCount', width: 92 },
  { title: '创建时间', dataIndex: 'createTime', width: 156 },
  { title: '操作', key: 'action', width: 128 }
]

const rules: Record<string, Rule[]> = {
  name: [{ required: true, message: '请输入教程名称', trigger: 'blur' }]
}

const pagination = computed(() => ({
  current: query.current,
  pageSize: query.size,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`
}))

const emptyText = computed(() => {
  if (loading.value) return '图文教程加载中...'
  if (query.name) return '没有找到符合条件的图文教程'
  return '暂无图文教程，点击右上角新增'
})

const createNode = (sortOrder = 1, data?: Partial<AdminTutorialNodeItem>): NodeFormItem => ({
  localKey: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
  id: data?.id,
  courseId: data?.courseId,
  title: data?.title || '',
  content: data?.content || '',
  sortOrder
})

const resetForm = () => {
  formState.id = undefined
  formState.name = ''
  formState.description = ''
  formState.coverImg = ''
  formState.nodes = [createNode(1)]
  formRef.value?.clearValidate?.()
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAdminTutorialList(query)
    tutorialList.value = res?.records || []
    total.value = res?.total || 0
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
  query.name = ''
  loadList()
}

const handleTableChange = (pageInfo: any) => {
  query.current = pageInfo.current
  query.size = pageInfo.pageSize
  loadList()
}

const openCreate = () => {
  resetForm()
  modalOpen.value = true
}

const openEdit = async (record: AdminTutorialItem) => {
  resetForm()
  const detail = await getAdminTutorialDetail(record.id)
  formState.id = detail.course.id
  formState.name = detail.course.name || ''
  formState.description = detail.course.description || ''
  formState.coverImg = detail.course.coverImg || ''
  formState.nodes = (detail.nodes || []).length
    ? detail.nodes.map((item, index) => createNode(index + 1, item))
    : [createNode(1)]
  modalOpen.value = true
}

const addNode = () => {
  formState.nodes.push(createNode(formState.nodes.length + 1))
}

const removeNode = (index: number) => {
  if (formState.nodes.length <= 1) return
  formState.nodes.splice(index, 1)
  formState.nodes.forEach((node, idx) => {
    node.sortOrder = idx + 1
  })
}

const normalizeNodes = () => {
  return formState.nodes
    .map((node, index) => ({
      title: (node.title || '').trim(),
      content: node.content || '',
      sortOrder: index + 1
    }))
    .filter(node => node.title || node.content)
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  const nodes = normalizeNodes()
  if (!nodes.length) {
    message.warning('请至少添加一个章节')
    return
  }
  if (nodes.some(node => !node.title)) {
    message.warning('请补全每个章节标题')
    return
  }

  submitLoading.value = true
  try {
    await saveAdminTutorial({
      id: formState.id,
      name: formState.name.trim(),
      description: formState.description,
      coverImg: formState.coverImg,
      nodes
    })
    message.success(formState.id ? '图文教程已更新' : '图文教程已新增')
    modalOpen.value = false
    resetForm()
    loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (record: AdminTutorialItem) => {
  Modal.confirm({
    title: '删除图文教程',
    content: `确定删除“${record.name}”吗？删除后学生端将不再显示该教程，章节也会一起删除。`,
    okText: '确认删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      await deleteAdminTutorial(record.id)
      if (tutorialList.value.length === 1 && (query.current || 1) > 1) {
        query.current = (query.current || 1) - 1
      }
      message.success('删除成功')
      loadList()
    }
  })
}

const handleCoverUpload = async (option: any) => {
  const rawFile = option?.file?.originFileObj || option?.file
  if (!rawFile) return

  uploading.value = true
  try {
    const url = await uploadAdminFile(rawFile as File, 'tutorial/cover')
    formState.coverImg = url
    option?.onSuccess?.(url)
    message.success('封面上传成功')
  } catch (error) {
    option?.onError?.(error)
  } finally {
    uploading.value = false
  }
}

const formatDate = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  resetForm()
  loadList()
})
</script>

<style scoped>
.tutorial-admin-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
  min-width: 0;
  overflow-x: hidden;
}

:global(.admin-content:has(.tutorial-admin-page)) {
  overflow-x: hidden;
  padding-left: 16px;
  padding-right: 16px;
}

.hero-panel,
.toolbar-panel,
.table-card {
  border: 1px solid #e7edf5;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.hero-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 20px;
}

.hero-panel h2 {
  margin: 0;
  color: #182230;
  font-size: 22px;
  font-weight: 800;
}

.hero-panel p {
  margin: 8px 0 0;
  color: #667085;
  font-size: 14px;
}

.toolbar-panel {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
}

.search-input {
  width: 300px;
}

.table-card {
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

:deep(.table-card .ant-card-body) {
  padding: 12px 12px 0;
  overflow-x: hidden;
}

:deep(.tutorial-table),
:deep(.tutorial-table .ant-spin-nested-loading),
:deep(.tutorial-table .ant-spin-container),
:deep(.tutorial-table .ant-table),
:deep(.tutorial-table .ant-table-container),
:deep(.tutorial-table .ant-table-content) {
  width: 100%;
  min-width: 0;
  overflow-x: hidden !important;
}

:deep(.tutorial-table table) {
  width: 100% !important;
  min-width: 0 !important;
  table-layout: fixed;
}

:deep(.tutorial-table .ant-table-cell) {
  padding: 12px 14px;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

:deep(.tutorial-table .ant-table-thead > tr > th) {
  padding-top: 13px;
  padding-bottom: 13px;
}

.cover-cell {
  display: grid;
  place-items: center;
  width: 76px;
  height: 48px;
  overflow: hidden;
  border: 1px solid #e7edf5;
  border-radius: 7px;
  background: #f8fafc;
  color: #98a2b3;
  font-size: 12px;
}

.cover-cell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.course-title {
  color: #182230;
  font-weight: 700;
  line-height: 1.45;
}

.desc-cell {
  max-width: 100%;
  color: #667085;
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

:deep(.action-group .ant-btn) {
  padding-inline: 4px;
}

:deep(.tutorial-edit-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  width: 1100px;
  max-width: calc(100vw - 48px);
  height: min(720px, calc(100vh - 56px));
  max-height: calc(100vh - 56px);
  border-radius: 8px;
  overflow: hidden;
}

:deep(.tutorial-edit-modal .ant-modal-header) {
  padding: 22px 28px 14px;
  margin-bottom: 0;
  border-bottom: 1px solid #eef2f7;
}

:deep(.tutorial-edit-modal .ant-modal-title) {
  color: #182230;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.35;
}

:deep(.tutorial-edit-modal .ant-modal-body) {
  flex: 1;
  height: auto;
  max-height: none;
  min-height: 0;
  overflow: hidden;
  padding: 18px 28px 18px;
}

:deep(.tutorial-edit-modal .ant-modal-footer) {
  flex-shrink: 0;
  margin-top: 0;
  padding: 14px 28px 18px;
  border-top: 1px solid #eef2f7;
}

:deep(.tutorial-edit-modal .ant-form-item) {
  margin-bottom: 20px;
}

:deep(.tutorial-edit-modal .ant-form-item-label > label) {
  height: 24px;
  color: #182230;
  font-size: 14px;
  font-weight: 700;
}

:deep(.tutorial-edit-modal .ant-input),
:deep(.tutorial-edit-modal .ant-input-affix-wrapper),
:deep(.tutorial-edit-modal textarea.ant-input) {
  border-radius: 7px;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.65;
}

:deep(.tutorial-edit-modal .ant-input:not(textarea)) {
  height: 38px;
}

.editor-layout {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  align-items: stretch;
  gap: 24px;
  height: 100%;
  min-height: 0;
}

.form-panel,
.node-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.form-panel {
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;
}

.node-panel {
  overflow: hidden;
}

.panel-title {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  height: 68px;
  margin-bottom: 12px;
}

.panel-title strong {
  color: #182230;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.35;
}

.panel-title span {
  margin-top: 4px;
  color: #7a8699;
  font-size: 13px;
  line-height: 1.45;
}

.cover-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cover-preview {
  display: grid;
  place-items: center;
  width: 100%;
  aspect-ratio: 16 / 9;
  max-height: 210px;
  overflow: hidden;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #f8fafc;
  color: #94a3b8;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f8fafc;
}

.upload-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.node-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex: 0 0 68px;
  height: 68px;
  margin-bottom: 12px;
}

.node-head strong {
  display: block;
  color: #182230;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.35;
}

.node-head span {
  display: block;
  margin-top: 4px;
  color: #7a8699;
  font-size: 13px;
  line-height: 1.45;
}

.add-node-btn {
  min-width: 108px;
  height: 38px;
  margin-top: 1px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  white-space: nowrap;
  padding: 0 14px;
  line-height: 1;
}

:deep(.add-node-btn .anticon) {
  display: inline-flex;
  line-height: 1;
}

.node-list {
  flex: 1;
  max-height: none;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
}

.node-card {
  padding: 14px 16px 16px;
  border: 1px solid #e7edf5;
  border-radius: 8px;
  background: #fbfdff;
}

.node-card + .node-card {
  margin-top: 12px;
}

.node-card-head {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 36px;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.node-index {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: #eef4ff;
  color: #2563eb;
  font-weight: 800;
  line-height: 1;
}

.chapter-rich-editor {
  width: 100%;
}

:deep(.chapter-rich-editor .rich-editor-shell) {
  border-radius: 7px;
  background: #ffffff;
}

:deep(.chapter-rich-editor .w-e-toolbar) {
  flex-wrap: wrap;
  row-gap: 4px;
  padding: 6px 8px;
}

:deep(.chapter-rich-editor .w-e-text-container) {
  min-height: 210px;
  font-size: 14px;
  line-height: 1.75;
}

:deep(.chapter-rich-editor .w-e-text-placeholder) {
  color: #98a2b3;
}

:deep(.node-card-head .ant-input) {
  height: 38px;
  font-size: 14px;
}

:deep(.node-card-head .ant-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
}

@media (max-width: 980px) {
  .hero-panel,
  .toolbar-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .editor-layout {
    grid-template-columns: 1fr;
  }
}
</style>
