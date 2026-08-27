<template>
  <div class="admin-page">
    <section class="page-header-card">
      <div class="page-copy">
        <h2 class="page-title">平台课程管理</h2>
        <p class="page-desc">
          面向整个平台统一维护课程主信息，和教师面向班级投放的课程区分管理。
        </p>
      </div>

      <div class="page-tags">
        <span>平台课程</span>
        <span>真实管理</span>
        <span>统一发布</span>
      </div>
    </section>

    <section class="toolbar-card">
      <div class="toolbar-left">
        <a-button type="primary" @click="openAddModal">
          <template #icon>
            <PlusOutlined />
          </template>
          新增平台课程
        </a-button>

        <a-select
          v-model:value="query.sourceType"
          class="toolbar-select"
          :options="sourceTypeOptions"
          @change="handleSearch"
        />

        <a-select
          v-model:value="query.publishStatus"
          class="toolbar-select"
          :options="publishStatusOptions"
          @change="handleSearch"
        />
      </div>

      <div class="toolbar-right">
        <a-input
          v-model:value="query.name"
          class="search-input"
          placeholder="按课程名称搜索"
          allow-clear
          @pressEnter="handleSearch"
        >
          <template #prefix>
            <SearchOutlined />
          </template>
        </a-input>

        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">
          <template #icon>
            <ReloadOutlined />
          </template>
          重置
        </a-button>
      </div>
    </section>

    <a-card class="table-card" :bordered="false">
      <a-table
        row-key="id"
        :columns="columns"
        :data-source="courseList"
        :loading="loading"
        :pagination="pagination"
        :locale="{ emptyText }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'coverImg'">
            <div class="cover-cell">
              <img
                v-if="record.coverImg"
                :src="record.coverImg"
                :alt="record.name"
                class="cover-image"
              />
              <div v-else class="cover-placeholder">暂无封面</div>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'type'">
            <a-tag :color="record.type === 'text' ? 'purple' : 'blue'">
              {{ record.type === 'text' ? '图文课程' : '视频课程' }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'categoryName'">
            <a-tag v-if="record.categoryName" color="cyan">{{ record.categoryName }}</a-tag>
            <span v-else class="readonly-tip">未分类</span>
          </template>

          <template v-else-if="column.dataIndex === 'sourceType'">
            <a-tag :color="record.sourceType === 'platform' ? 'processing' : 'default'">
              {{ record.sourceType === 'platform' ? '平台课程' : '教师课程' }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'publishStatus'">
            <a-tag :color="getPublishStatusColor(record.publishStatus)">
              {{ getPublishStatusText(record.publishStatus) }}
            </a-tag>
          </template>

          <template v-else-if="column.dataIndex === 'description'">
            <div class="desc-cell">{{ record.description || '—' }}</div>
          </template>

          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>

          <template v-else-if="column.key === 'action'">
            <div class="action-group">
              <template v-if="record.sourceType === 'platform'">
                <a-button type="link" @click="openEditModal(record)">
                  <template #icon>
                    <EditOutlined />
                  </template>
                  编辑
                </a-button>

                <a-button type="link" danger @click="handleDelete(record)">
                  <template #icon>
                    <DeleteOutlined />
                  </template>
                  删除
                </a-button>
              </template>

              <span v-else class="readonly-tip">教师侧课程</span>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="isEditMode ? '编辑平台课程' : '新增平台课程'"
      width="1100px"
      centered
      class="admin-wide-modal"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      destroyOnClose
    >
      <a-form
        ref="courseFormRef"
        :model="formState"
        :rules="rules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="课程名称" name="name">
              <a-input
                v-model:value="formState.name"
                placeholder="请输入课程名称"
                maxlength="80"
              />
            </a-form-item>
          </a-col>

          <a-col :span="8">
            <a-form-item label="课程类型" name="type">
              <a-select
                v-model:value="formState.type"
                :options="courseTypeOptions"
                placeholder="请选择课程类型"
              />
            </a-form-item>
          </a-col>

          <a-col :span="8">
            <a-form-item label="课程分类" name="categoryId">
              <a-select
                v-model:value="formState.categoryId"
                :options="categoryOptions"
                placeholder="请选择学生端分类"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="课程简介" name="description">
          <a-textarea
            v-model:value="formState.description"
            :rows="4"
            placeholder="请输入课程简介"
            maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item label="课程封面" name="coverImg">
          <div class="cover-upload-panel">
            <div class="cover-preview-box">
              <img
                v-if="formState.coverImg"
                :src="formState.coverImg"
                alt="课程封面"
                class="cover-preview-image"
              />
              <div v-else class="cover-preview-placeholder">封面预览</div>
            </div>

            <div class="cover-upload-actions">
              <a-upload
                :show-upload-list="false"
                accept="image/*"
                :custom-request="handleCoverUpload"
              >
                <a-button :loading="uploading">
                  <template #icon>
                    <UploadOutlined />
                  </template>
                  上传封面
                </a-button>
              </a-upload>

              <a-input
                v-model:value="formState.coverImg"
                placeholder="也可以直接填写封面链接"
              />
            </div>
          </div>
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="发布状态" name="publishStatus">
              <a-select
                v-model:value="formState.publishStatus"
                :options="coursePublishOptions"
                placeholder="请选择发布状态"
              />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="主视频链接">
              <a-input
                v-model:value="formState.videoUrl"
                placeholder="系统会自动同步为第一集视频地址，这里一般不用手填"
                disabled
              />
            </a-form-item>
          </a-col>
        </a-row>

        <template v-if="formState.type === 'video'">
          <div class="chapter-section">
            <div class="chapter-section-header">
              <div>
                <div class="chapter-title">课程分集</div>
                <div class="chapter-desc">平台课程按选集管理，第一集视频会自动作为课程主视频回填</div>
              </div>

              <a-button type="dashed" @click="addChapterRow">
                <template #icon>
                  <PlusOutlined />
                </template>
                新增分集
              </a-button>
            </div>

            <div
              v-for="(chapter, index) in chapterList"
              :key="chapter.localKey"
              class="chapter-row"
            >
              <div class="chapter-row-head">
                <span class="chapter-index">P{{ index + 1 }}</span>

                <a-button
                  v-if="chapterList.length > 1"
                  type="link"
                  danger
                  @click="removeChapterRow(index)"
                >
                  删除分集
                </a-button>
              </div>

              <a-row :gutter="12">
                <a-col :span="8">
                  <a-input
                    v-model:value="chapter.title"
                    :placeholder="`请输入第 ${index + 1} 集标题`"
                    maxlength="100"
                  />
                </a-col>

                <a-col :span="5">
                  <a-input-number
                    v-model:value="chapter.sortOrder"
                    :min="1"
                    :precision="0"
                    style="width: 100%"
                    placeholder="排序"
                  />
                </a-col>

                <a-col :span="11">
                  <a-upload
                    :show-upload-list="false"
                    accept="video/*"
                    :custom-request="handleChapterVideoUpload.bind(null, index)"
                  >
                    <a-button :loading="chapter.uploading">
                      <template #icon>
                        <UploadOutlined />
                      </template>
                      上传本集视频
                    </a-button>
                  </a-upload>
                </a-col>
              </a-row>

              <div class="chapter-link-row">
                <a-input
                  v-model:value="chapter.videoUrl"
                  placeholder="也可以直接填写本集视频链接"
                />
              </div>

              <iframe
                v-if="getBilibiliEmbedUrl(chapter.videoUrl)"
                :src="getBilibiliEmbedUrl(chapter.videoUrl)"
                class="chapter-video-preview"
                allow="autoplay; fullscreen; picture-in-picture"
                sandbox="allow-scripts allow-same-origin allow-presentation"
                allowfullscreen
                frameborder="0"
              ></iframe>
              <video
                v-else-if="chapter.videoUrl"
                :src="chapter.videoUrl"
                class="chapter-video-preview"
                controls
                preload="metadata"
              />
            </div>
          </div>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
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
  addAdminCourse,
  deleteAdminCourse,
  getAdminCourseChapterList,
  getAdminCourseList,
  replaceAdminCourseChapters,
  updateAdminCourse,
  uploadAdminFile
} from '@/api/admin'
import { getCategoryList, type PlatformCategoryItem } from '@/api/platform'
import type {
  AdminCourseFormData,
  AdminCourseItem,
  AdminCourseListParams
} from '@/types/admin'

type ChapterFormItem = {
  id?: number
  localKey: string
  title: string
  videoUrl: string
  sortOrder: number
  uploading?: boolean
}

const loading = ref(false)
const submitLoading = ref(false)
const uploading = ref(false)
const modalOpen = ref(false)
const courseFormRef = ref()

const query = reactive<AdminCourseListParams>({
  current: 1,
  size: 10,
  name: '',
  sourceType: 'platform',
  publishStatus: ''
})

const courseList = ref<AdminCourseItem[]>([])
const total = ref(0)
const categoryOptions = ref<Array<{ label: string; value: number }>>([])

const formState = reactive<AdminCourseFormData>({
  id: undefined,
  name: '',
  description: '',
  coverImg: '',
  videoUrl: '',
  type: 'video',
  publishStatus: 'published',
  categoryId: undefined
})

const chapterList = ref<ChapterFormItem[]>([])

const createChapterRow = (sortOrder = 1): ChapterFormItem => ({
  localKey: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
  title: '',
  videoUrl: '',
  sortOrder,
  uploading: false
})

const getBilibiliEmbedUrl = (raw?: string) => {
  if (!raw) return ''
  const bvidMatch = raw.match(/(?:bilibili\.com\/video\/|\/video\/)(BV[a-zA-Z0-9]+)/)
  if (!bvidMatch) return ''

  let page = '1'
  try {
    const parsed = new URL(raw)
    page = parsed.searchParams.get('p') || '1'
  } catch (error) {
    const pageMatch = raw.match(/[?&]p=(\d+)/)
    page = pageMatch?.[1] || '1'
  }

  return `https://player.bilibili.com/player.html?bvid=${bvidMatch[1]}&page=${page}&autoplay=0`
}

const columns = [
  {
    title: '课程封面',
    dataIndex: 'coverImg',
    width: 130
  },
  {
    title: '课程名称',
    dataIndex: 'name',
    ellipsis: true,
    minWidth: 280
  },
  {
    title: '课程简介',
    dataIndex: 'description',
    ellipsis: true,
    minWidth: 240
  },
  {
    title: '课程类型',
    dataIndex: 'type',
    width: 110
  },
  {
    title: '课程分类',
    dataIndex: 'categoryName',
    width: 90
  },
  {
    title: '来源',
    dataIndex: 'sourceType',
    width: 110
  },
  {
    title: '发布状态',
    dataIndex: 'publishStatus',
    width: 100
  },
  {
    title: '创建人',
    dataIndex: 'creatorName',
    width: 150
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 170
  },
  {
    title: '操作',
    key: 'action',
    width: 160,
    fixed: 'right'
  }
]

const rules: Record<string, Rule[]> = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择课程类型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择课程分类', trigger: 'change' }],
  publishStatus: [{ required: true, message: '请选择发布状态', trigger: 'change' }]
}

const sourceTypeOptions = [
  { label: '平台课程', value: 'platform' },
  { label: '教师课程', value: 'teacher' },
  { label: '全部来源', value: 'all' }
]

const publishStatusOptions = [
  { label: '全部状态', value: '' },
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
  { label: '已下线', value: 'offline' }
]

const coursePublishOptions = [
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
  { label: '已下线', value: 'offline' }
]

const courseTypeOptions = [
  { label: '视频课程', value: 'video' },
  { label: '图文课程', value: 'text' }
]

const isEditMode = computed(() => Boolean(formState.id))

const pagination = computed(() => ({
  current: query.current,
  pageSize: query.size,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`
}))

const emptyText = computed(() => {
  if (loading.value) return '课程列表加载中...'
  if (query.name || query.publishStatus || (query.sourceType && query.sourceType !== 'platform')) {
    return '没有找到符合条件的课程，请调整筛选条件后重试'
  }
  return '当前暂无平台课程，可先新增一门平台课程'
})

const resetFormState = () => {
  formState.id = undefined
  formState.name = ''
  formState.description = ''
  formState.coverImg = ''
  formState.videoUrl = ''
  formState.type = 'video'
  formState.publishStatus = 'published'
  formState.categoryId = undefined
  chapterList.value = [createChapterRow(1)]
}

const loadCategoryOptions = async () => {
  const res = await getCategoryList()
  categoryOptions.value = (Array.isArray(res) ? res : [])
    .filter((item: PlatformCategoryItem) => item.id && item.name)
    .slice(0, 18)
    .map((item: PlatformCategoryItem) => ({
      label: item.name,
      value: item.id as number
    }))
}

const loadCourseList = async () => {
  loading.value = true
  try {
    const res = await getAdminCourseList(query)
    courseList.value = res?.records || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const loadChapterList = async (courseId: number) => {
  const res = await getAdminCourseChapterList(courseId)
  const rows = (res || []).map((item: any, index: number) => ({
    id: item.id,
    localKey: `${item.id || 'chapter'}_${index}`,
    title: item.title || '',
    videoUrl: item.videoUrl || '',
    sortOrder: item.sortOrder || index + 1,
    uploading: false
  }))

  chapterList.value = rows.length ? rows : [createChapterRow(1)]
}

const handleSearch = () => {
  query.current = 1
  loadCourseList()
}

const handleReset = () => {
  query.current = 1
  query.size = 10
  query.name = ''
  query.sourceType = 'platform'
  query.publishStatus = ''
  loadCourseList()
}

const handleTableChange = (pageInfo: any) => {
  query.current = pageInfo.current
  query.size = pageInfo.pageSize
  loadCourseList()
}

const openAddModal = () => {
  resetFormState()
  modalOpen.value = true
}

const openEditModal = async (record: AdminCourseItem) => {
  formState.id = record.id
  formState.name = record.name || ''
  formState.description = record.description || ''
  formState.coverImg = record.coverImg || ''
  formState.videoUrl = record.videoUrl || ''
  formState.type = record.type || 'video'
  formState.publishStatus = record.publishStatus || 'published'
  formState.categoryId = record.categoryId

  if (formState.type === 'video' && record.id) {
    await loadChapterList(record.id)
  } else {
    chapterList.value = []
  }

  modalOpen.value = true
}

const normalizeChapterList = () => {
  return chapterList.value
    .map((item, index) => ({
      ...item,
      title: (item.title || '').trim(),
      videoUrl: (item.videoUrl || '').trim(),
      sortOrder: item.sortOrder || index + 1
    }))
    .filter(item => item.title || item.videoUrl)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
}

const handleSubmit = async () => {
  await courseFormRef.value?.validate()

  const normalizedChapters = formState.type === 'video' ? normalizeChapterList() : []

  if (formState.type === 'video') {
    if (!normalizedChapters.length) {
      message.warning('请至少添加一个分集')
      return
    }

    const invalidItem = normalizedChapters.find(item => !item.title || !item.videoUrl)
    if (invalidItem) {
      message.warning('请补全每个分集的标题和视频链接')
      return
    }

    formState.videoUrl = normalizedChapters[0]?.videoUrl || ''
  } else {
    formState.videoUrl = ''
  }

  submitLoading.value = true

  try {
    let courseId = formState.id as number | undefined

    if (isEditMode.value) {
      await updateAdminCourse({ ...formState })
    } else {
      courseId = await addAdminCourse({ ...formState })
    }

    if (courseId) {
      await replaceAdminCourseChapters({
        courseId,
        chapterList: normalizedChapters.map(item => ({
          id: item.id,
          title: item.title,
          videoUrl: item.videoUrl,
          sortOrder: item.sortOrder
        }))
      })
    }

    message.success(isEditMode.value ? '平台课程更新成功' : '平台课程新增成功')
    modalOpen.value = false
    resetFormState()
    loadCourseList()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (record: AdminCourseItem) => {
  Modal.confirm({
    title: '删除平台课程',
    content: `确定删除课程“${record.name}”吗？删除后课程及其分集都会移除。`,
    okText: '确认删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      await deleteAdminCourse(record.id)
      if (courseList.value.length === 1 && (query.current || 1) > 1) {
        query.current = (query.current || 1) - 1
      }
      message.success('删除成功')
      loadCourseList()
    }
  })
}

const handleCoverUpload = async (option: any) => {
  const rawFile = option?.file?.originFileObj || option?.file
  if (!rawFile) return

  uploading.value = true
  try {
    const fileUrl = await uploadAdminFile(rawFile as File, 'course/platform/cover')
    formState.coverImg = fileUrl
    message.success('课程封面上传成功')
    option?.onSuccess?.(fileUrl)
  } catch (error) {
    message.error('封面上传失败，请检查图片格式或 OSS 配置后重试')
    option?.onError?.(error)
  } finally {
    uploading.value = false
  }
}

const handleChapterVideoUpload = async (index: number, option: any) => {
  const rawFile = option?.file?.originFileObj || option?.file
  if (!rawFile) return

  const current = chapterList.value[index]
  if (!current) return

  current.uploading = true
  try {
    const fileUrl = await uploadAdminFile(rawFile as File, 'course/platform/video')
    current.videoUrl = fileUrl

    if (index === 0) {
      formState.videoUrl = fileUrl
    }

    message.success(`第 ${index + 1} 集视频上传成功`)
    option?.onSuccess?.(fileUrl)
  } catch (error) {
    message.error('视频上传失败，请检查视频格式、大小或 OSS 配置后重试')
    option?.onError?.(error)
  } finally {
    current.uploading = false
  }
}

const addChapterRow = () => {
  chapterList.value.push(createChapterRow(chapterList.value.length + 1))
}

const removeChapterRow = (index: number) => {
  if (chapterList.value.length <= 1) return
  chapterList.value.splice(index, 1)
  chapterList.value = chapterList.value.map((item, idx) => ({
    ...item,
    sortOrder: idx + 1
  }))
}

const formatDate = (value?: string) => {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}

const getPublishStatusText = (status?: string) => {
  if (status === 'draft') return '草稿'
  if (status === 'offline') return '已下线'
  return '已发布'
}

const getPublishStatusColor = (status?: string): string => {
  if (status === 'draft') return 'default'
  if (status === 'offline') return 'orange'
  return 'green'
}

onMounted(() => {
  resetFormState()
  loadCategoryOptions()
  loadCourseList()
})
</script>

<style scoped>
.admin-page {
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

.toolbar-card {
  padding: 18px 20px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e8eef7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-select {
  width: 150px;
}

.search-input {
  width: 260px;
}

.table-card {
  border-radius: 20px;
  border: 1px solid #e8eef7;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.cover-cell {
  width: 72px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f7fb;
  border: 1px solid #edf1f7;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  font-size: 12px;
  color: #98a2b3;
}

.desc-cell {
  max-width: 280px;
  color: #667085;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.readonly-tip {
  color: #98a2b3;
  font-size: 13px;
}

.cover-upload-panel {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.cover-preview-box {
  width: 180px;
  height: 112px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px dashed #d0d9e5;
  background: #f8fafc;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-preview-placeholder {
  color: #98a2b3;
  font-size: 14px;
}

.cover-upload-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
}

.chapter-section {
  margin-top: 8px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid #e8eef7;
  background: #f8fbff;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chapter-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chapter-title {
  font-size: 16px;
  font-weight: 600;
  color: #182230;
}

.chapter-desc {
  margin-top: 4px;
  font-size: 13px;
  color: #667085;
}

.chapter-row {
  padding: 16px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e8eef7;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chapter-row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chapter-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 46px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eef4ff;
  color: #1e4ed8;
  font-size: 13px;
  font-weight: 600;
}

.chapter-link-row {
  display: flex;
}

.chapter-video-preview {
  width: 100%;
  max-height: 220px;
  border-radius: 12px;
  background: #000;
}

:deep(.ant-table-wrapper .ant-table) {
  border-radius: 16px;
}

:deep(.ant-table-wrapper .ant-table-thead > tr > th) {
  background: #f8fbff;
  color: #344054;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .page-header-card,
  .toolbar-card,
  .chapter-section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .search-input {
    width: 220px;
  }
}

@media (max-width: 768px) {
  .cover-upload-panel {
    flex-direction: column;
  }

  .cover-preview-box {
    width: 100%;
  }
}
</style>
