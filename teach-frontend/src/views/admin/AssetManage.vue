<template>
  <div class="admin-page">
    <section class="page-header-card">
      <div class="page-copy">
        <h2 class="page-title">运营素材管理</h2>
        <p class="page-desc">
          统一管理学生端首页广告图与课程分类图标。当前页面已接通列表、新增、编辑、删除、启停与排序值配置。
        </p>
      </div>

      <div class="page-tags">
        <span>广告图</span>
        <span>分类图标</span>
        <span>后台配置</span>
      </div>
    </section>

    <section class="content-grid">
      <a-card class="info-card" :bordered="false">
        <template #title>素材概览</template>
        <div class="metric-grid">
          <div class="metric-box">
            <span class="metric-label">广告图数量</span>
            <strong class="metric-value">{{ bannerOverviewTotal }}</strong>
          </div>
          <div class="metric-box">
            <span class="metric-label">分类数量</span>
            <strong class="metric-value">{{ categoryOverviewTotal }}</strong>
          </div>
          <div class="metric-box">
            <span class="metric-label">启用中素材</span>
            <strong class="metric-value">{{ enabledBannerCount + enabledCategoryCount }}</strong>
          </div>
        </div>
      </a-card>

      <a-card class="info-card" :bordered="false">
        <template #title>管理说明</template>
        <ul class="info-list">
          <li>广告图与分类图标都走统一上传接口</li>
          <li>启停采用开关控制，不做拖拽排序</li>
          <li>学生端公开接口只返回启用状态数据</li>
          <li>排序值越小越靠前</li>
        </ul>
      </a-card>
    </section>

    <a-card class="tab-card" :bordered="false">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="banner" tab="广告图管理">
          <div class="toolbar-card inner-toolbar">
            <div class="toolbar-left">
              <a-button type="primary" @click="openBannerModal()">
                新增广告图
              </a-button>
            </div>

            <div class="toolbar-right">
              <a-input
                v-model:value="bannerQuery.title"
                class="search-input"
                placeholder="按广告图标题搜索"
                allow-clear
                @pressEnter="loadBannerList"
              >
                <template #prefix>
                  <SearchOutlined />
                </template>
              </a-input>
              <a-button type="primary" @click="handleBannerSearch">查询</a-button>
              <a-button @click="resetBannerSearch">
                <template #icon>
                  <ReloadOutlined />
                </template>
                重置
              </a-button>
            </div>
          </div>

          <a-table
            row-key="id"
            :columns="bannerColumns"
            :data-source="bannerList"
            :loading="bannerLoading"
            :pagination="bannerPagination"
            :locale="{ emptyText: bannerEmptyText }"
            @change="handleBannerTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'imageUrl'">
                <div class="image-cell">
                  <img :src="record.imageUrl" :alt="record.title" class="image-preview" />
                </div>
              </template>

              <template v-else-if="column.dataIndex === 'isEnabled'">
                <a-switch
                  :checked="record.isEnabled === 1"
                  :loading="bannerToggleId === record.id"
                  @change="(checked: boolean) => toggleBannerStatus(record, checked)"
                />
              </template>

              <template v-else-if="column.dataIndex === 'createTime'">
                {{ formatDate(record.createTime) }}
              </template>

              <template v-else-if="column.key === 'action'">
                <div class="action-group">
                  <a-button type="link" @click="openBannerModal(record)">编辑</a-button>
                  <a-button type="link" danger @click="handleDeleteBanner(record)">删除</a-button>
                </div>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="category" tab="分类图标管理">
          <div class="toolbar-card inner-toolbar">
            <div class="toolbar-left">
              <a-button type="primary" @click="openCategoryModal()">
                新增分类图标
              </a-button>
            </div>

            <div class="toolbar-right">
              <a-input
                v-model:value="categoryQuery.name"
                class="search-input"
                placeholder="按分类名称搜索"
                allow-clear
                @pressEnter="loadCategoryList"
              >
                <template #prefix>
                  <SearchOutlined />
                </template>
              </a-input>
              <a-button type="primary" @click="handleCategorySearch">查询</a-button>
              <a-button @click="resetCategorySearch">
                <template #icon>
                  <ReloadOutlined />
                </template>
                重置
              </a-button>
            </div>
          </div>

          <a-table
            row-key="id"
            :columns="categoryColumns"
            :data-source="categoryList"
            :loading="categoryLoading"
            :pagination="categoryPagination"
            :locale="{ emptyText: categoryEmptyText }"
            @change="handleCategoryTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'iconUrl'">
                <div class="icon-cell">
                  <img :src="record.iconUrl" :alt="record.name" class="icon-preview" />
                </div>
              </template>

              <template v-else-if="column.dataIndex === 'isEnabled'">
                <a-switch
                  :checked="record.isEnabled === 1"
                  :loading="categoryToggleId === record.id"
                  @change="(checked: boolean) => toggleCategoryStatus(record, checked)"
                />
              </template>

              <template v-else-if="column.dataIndex === 'createTime'">
                {{ formatDate(record.createTime) }}
              </template>

              <template v-else-if="column.key === 'action'">
                <div class="action-group">
                  <a-button type="link" @click="openCategoryModal(record)">编辑</a-button>
                  <a-button type="link" danger @click="handleDeleteCategory(record)">删除</a-button>
                </div>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="bannerModalOpen"
      :title="bannerForm.id ? '编辑广告图' : '新增广告图'"
      :confirm-loading="bannerSubmitLoading"
      width="720px"
      centered
      class="asset-form-modal"
      @ok="submitBanner"
      destroyOnClose
    >
      <a-form ref="bannerFormRef" :model="bannerForm" :rules="bannerRules" layout="vertical">
        <a-form-item label="广告图标题" name="title">
          <a-input v-model:value="bannerForm.title" placeholder="请输入广告图标题" />
        </a-form-item>

        <a-form-item label="广告图图片" name="imageUrl">
          <div class="upload-panel">
            <div class="upload-preview">
              <img v-if="bannerForm.imageUrl" :src="bannerForm.imageUrl" class="upload-preview-image" />
              <div v-else class="upload-placeholder">图片预览</div>
            </div>

            <div class="upload-actions">
              <a-upload :show-upload-list="false" accept="image/*" :custom-request="handleBannerUpload">
                <a-button :loading="bannerUploading">上传图片</a-button>
              </a-upload>
              <a-input v-model:value="bannerForm.imageUrl" placeholder="也可以直接填写图片链接" />
            </div>
          </div>
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="跳转地址">
              <a-input v-model:value="bannerForm.targetUrl" placeholder="请输入跳转地址，可为空" />
            </a-form-item>
          </a-col>

          <a-col :span="6">
            <a-form-item label="排序值" name="sortOrder">
              <a-input-number v-model:value="bannerForm.sortOrder" class="full-width" :min="0" />
            </a-form-item>
          </a-col>

          <a-col :span="6">
            <a-form-item label="启用状态" name="isEnabled">
              <a-select v-model:value="bannerForm.isEnabled" :options="enableOptions" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="categoryModalOpen"
      :title="categoryForm.id ? '编辑分类图标' : '新增分类图标'"
      :confirm-loading="categorySubmitLoading"
      width="640px"
      centered
      class="asset-form-modal asset-icon-modal"
      @ok="submitCategory"
      destroyOnClose
    >
      <a-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" layout="vertical">
        <a-form-item label="分类名称" name="name">
          <a-input v-model:value="categoryForm.name" placeholder="请输入分类名称" />
        </a-form-item>

        <a-form-item label="分类图标" name="iconUrl">
          <div class="upload-panel">
            <div class="upload-preview icon-preview-box">
              <img v-if="categoryForm.iconUrl" :src="categoryForm.iconUrl" class="upload-preview-image contain" />
              <div v-else class="upload-placeholder">图标预览</div>
            </div>

            <div class="upload-actions">
              <a-upload :show-upload-list="false" accept="image/*" :custom-request="handleCategoryUpload">
                <a-button :loading="categoryUploading">上传图标</a-button>
              </a-upload>
              <a-input v-model:value="categoryForm.iconUrl" placeholder="也可以直接填写图标链接" />
            </div>
          </div>
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="排序值" name="sortOrder">
              <a-input-number v-model:value="categoryForm.sortOrder" class="full-width" :min="0" />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="启用状态" name="isEnabled">
              <a-select v-model:value="categoryForm.isEnabled" :options="enableOptions" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import {
  addAdminBanner,
  addAdminCategory,
  deleteAdminBanner,
  deleteAdminCategory,
  getAdminBannerList,
  getAdminCategoryList,
  getAdminDashboardMetrics,
  updateAdminBanner,
  updateAdminCategory,
  uploadAdminFile
} from '@/api/admin'
import type {
  AdminDashboardMetrics,
  CourseCategoryItem,
  CourseCategoryListParams,
  PlatformBannerItem,
  PlatformBannerListParams
} from '@/types/admin'

const activeTab = ref('category')

const bannerUploading = ref(false)
const categoryUploading = ref(false)

const bannerLoading = ref(false)
const categoryLoading = ref(false)
const bannerSubmitLoading = ref(false)
const categorySubmitLoading = ref(false)
const bannerToggleId = ref<number | null>(null)
const categoryToggleId = ref<number | null>(null)

const bannerModalOpen = ref(false)
const categoryModalOpen = ref(false)

const bannerFormRef = ref()
const categoryFormRef = ref()

const bannerList = ref<PlatformBannerItem[]>([])
const categoryList = ref<CourseCategoryItem[]>([])
const bannerTotal = ref(0)
const categoryTotal = ref(0)
const dashboardMetrics = ref<AdminDashboardMetrics | null>(null)

const bannerQuery = reactive<PlatformBannerListParams>({
  current: 1,
  size: 10,
  title: ''
})

const categoryQuery = reactive<CourseCategoryListParams>({
  current: 1,
  size: 18,
  name: ''
})

const bannerForm = reactive<PlatformBannerItem>({
  title: '',
  imageUrl: '',
  targetUrl: '',
  sortOrder: 0,
  isEnabled: 1
})

const categoryForm = reactive<CourseCategoryItem>({
  name: '',
  iconUrl: '',
  sortOrder: 0,
  isEnabled: 1
})

const enableOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const bannerRules: Record<string, Rule[]> = {
  title: [{ required: true, message: '请输入广告图标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请上传广告图图片', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }]
}

const categoryRules: Record<string, Rule[]> = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  iconUrl: [{ required: true, message: '请上传分类图标', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }]
}

const bannerColumns = [
  {
    title: '广告图',
    dataIndex: 'imageUrl',
    width: 120
  },
  {
    title: '标题',
    dataIndex: 'title'
  },
  {
    title: '跳转地址',
    dataIndex: 'targetUrl',
    ellipsis: true
  },
  {
    title: '排序值',
    dataIndex: 'sortOrder',
    width: 100
  },
  {
    title: '启用',
    dataIndex: 'isEnabled',
    width: 100
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 140
  }
]

const categoryColumns = [
  {
    title: '图标',
    dataIndex: 'iconUrl',
    width: 100
  },
  {
    title: '分类名称',
    dataIndex: 'name'
  },
  {
    title: '排序值',
    dataIndex: 'sortOrder',
    width: 100
  },
  {
    title: '启用',
    dataIndex: 'isEnabled',
    width: 100
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 140
  }
]

const bannerPagination = computed(() => ({
  current: bannerQuery.current,
  pageSize: bannerQuery.size,
  total: bannerTotal.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`
}))

const categoryPagination = computed(() => ({
  current: categoryQuery.current,
  pageSize: categoryQuery.size,
  total: categoryTotal.value,
  showSizeChanger: true,
  pageSizeOptions: ['18', '36', '72'],
  showTotal: (value: number) => `共 ${value} 条`
}))

const bannerEmptyText = computed(() => {
  if (bannerLoading.value) return '广告图列表加载中...'
  if (bannerQuery.title) return '没有找到符合条件的广告图'
  return '当前暂无广告图，请先新增一张广告图'
})

const categoryEmptyText = computed(() => {
  if (categoryLoading.value) return '分类图标列表加载中...'
  if (categoryQuery.name) return '没有找到符合条件的分类图标'
  return '当前暂无分类图标，请先新增一个分类图标'
})

const bannerOverviewTotal = computed(() => dashboardMetrics.value?.totalBanners ?? bannerTotal.value)
const categoryOverviewTotal = computed(() => dashboardMetrics.value?.totalCategories ?? categoryTotal.value)
const enabledBannerCount = computed(() => dashboardMetrics.value?.enabledBanners ?? bannerList.value.filter(item => item.isEnabled === 1).length)
const enabledCategoryCount = computed(() => dashboardMetrics.value?.enabledCategories ?? categoryList.value.filter(item => item.isEnabled === 1).length)

const loadMetrics = async () => {
  dashboardMetrics.value = await getAdminDashboardMetrics()
}

const loadBannerList = async () => {
  bannerLoading.value = true
  try {
    const res = await getAdminBannerList(bannerQuery)
    bannerList.value = res?.records || []
    bannerTotal.value = res?.total || 0
    await loadMetrics()
  } finally {
    bannerLoading.value = false
  }
}

const loadCategoryList = async () => {
  categoryLoading.value = true
  try {
    const res = await getAdminCategoryList(categoryQuery)
    categoryList.value = res?.records || []
    categoryTotal.value = res?.total || 0
    await loadMetrics()
  } finally {
    categoryLoading.value = false
  }
}

const handleBannerSearch = () => {
  bannerQuery.current = 1
  loadBannerList()
}

const resetBannerSearch = () => {
  bannerQuery.current = 1
  bannerQuery.size = 10
  bannerQuery.title = ''
  loadBannerList()
}

const handleCategorySearch = () => {
  categoryQuery.current = 1
  loadCategoryList()
}

const resetCategorySearch = () => {
  categoryQuery.current = 1
  categoryQuery.size = 18
  categoryQuery.name = ''
  loadCategoryList()
}

const handleBannerTableChange = (pageInfo: any) => {
  bannerQuery.current = pageInfo.current
  bannerQuery.size = pageInfo.pageSize
  loadBannerList()
}

const handleCategoryTableChange = (pageInfo: any) => {
  categoryQuery.current = pageInfo.current
  categoryQuery.size = pageInfo.pageSize
  loadCategoryList()
}

const resetBannerForm = () => {
  bannerForm.id = undefined
  bannerForm.title = ''
  bannerForm.imageUrl = ''
  bannerForm.targetUrl = ''
  bannerForm.sortOrder = 0
  bannerForm.isEnabled = 1
}

const resetCategoryForm = () => {
  categoryForm.id = undefined
  categoryForm.name = ''
  categoryForm.iconUrl = ''
  categoryForm.sortOrder = 0
  categoryForm.isEnabled = 1
}

const openBannerModal = (record?: PlatformBannerItem) => {
  if (record) {
    bannerForm.id = record.id
    bannerForm.title = record.title
    bannerForm.imageUrl = record.imageUrl
    bannerForm.targetUrl = record.targetUrl || ''
    bannerForm.sortOrder = record.sortOrder
    bannerForm.isEnabled = record.isEnabled
  } else {
    resetBannerForm()
  }
  bannerModalOpen.value = true
}

const openCategoryModal = (record?: CourseCategoryItem) => {
  if (record) {
    categoryForm.id = record.id
    categoryForm.name = record.name
    categoryForm.iconUrl = record.iconUrl
    categoryForm.sortOrder = record.sortOrder
    categoryForm.isEnabled = record.isEnabled
  } else {
    resetCategoryForm()
  }
  categoryModalOpen.value = true
}

const submitBanner = async () => {
  await bannerFormRef.value?.validate()
  bannerSubmitLoading.value = true
  try {
    if (bannerForm.id) {
      await updateAdminBanner({ ...bannerForm })
    } else {
      await addAdminBanner({ ...bannerForm })
    }
    bannerModalOpen.value = false
    loadBannerList()
  } finally {
    bannerSubmitLoading.value = false
  }
}

const submitCategory = async () => {
  await categoryFormRef.value?.validate()
  categorySubmitLoading.value = true
  try {
    if (categoryForm.id) {
      await updateAdminCategory({ ...categoryForm })
    } else {
      await addAdminCategory({ ...categoryForm })
    }
    categoryModalOpen.value = false
    loadCategoryList()
  } finally {
    categorySubmitLoading.value = false
  }
}

const toggleBannerStatus = async (record: PlatformBannerItem, checked: boolean) => {
  bannerToggleId.value = record.id ?? null
  try {
    await updateAdminBanner({
      ...record,
      isEnabled: checked ? 1 : 0
    })
    message.success(`广告图已${checked ? '启用' : '停用'}`)
    loadBannerList()
  } catch (error) {
    message.error(`广告图${checked ? '启用' : '停用'}失败，请稍后重试`)
  } finally {
    bannerToggleId.value = null
  }
}

const toggleCategoryStatus = async (record: CourseCategoryItem, checked: boolean) => {
  categoryToggleId.value = record.id ?? null
  try {
    await updateAdminCategory({
      ...record,
      isEnabled: checked ? 1 : 0
    })
    message.success(`分类图标已${checked ? '启用' : '停用'}`)
    loadCategoryList()
  } catch (error) {
    message.error(`分类图标${checked ? '启用' : '停用'}失败，请稍后重试`)
  } finally {
    categoryToggleId.value = null
  }
}

const handleDeleteBanner = (record: PlatformBannerItem) => {
  Modal.confirm({
    title: '删除广告图',
    content: `确定删除广告图“${record.title}”吗？`,
    okButtonProps: { danger: true },
    onOk: async () => {
      await deleteAdminBanner(record.id as number)
      if (bannerList.value.length === 1 && (bannerQuery.current || 1) > 1) {
        bannerQuery.current = (bannerQuery.current || 1) - 1
      }
      loadBannerList()
    }
  })
}

const handleDeleteCategory = (record: CourseCategoryItem) => {
  Modal.confirm({
    title: '删除分类图标',
    content: `确定删除分类“${record.name}”吗？`,
    okButtonProps: { danger: true },
    onOk: async () => {
      await deleteAdminCategory(record.id as number)
      if (categoryList.value.length === 1 && (categoryQuery.current || 1) > 1) {
        categoryQuery.current = (categoryQuery.current || 1) - 1
      }
      loadCategoryList()
    }
  })
}

const handleBannerUpload = async (option: any) => {
  const rawFile = option?.file?.originFileObj || option?.file
  if (!rawFile) return

  bannerUploading.value = true
  try {
    const fileUrl = await uploadAdminFile(rawFile as File)
    bannerForm.imageUrl = fileUrl
    message.success('广告图上传成功')
    option?.onSuccess?.(fileUrl)
  } catch (error) {
    message.error('广告图上传失败，请检查图片格式或 OSS 配置后重试')
    option?.onError?.(error)
  } finally {
    bannerUploading.value = false
  }
}

const handleCategoryUpload = async (option: any) => {
  const rawFile = option?.file?.originFileObj || option?.file
  if (!rawFile) return

  categoryUploading.value = true
  try {
    const fileUrl = await uploadAdminFile(rawFile as File)
    categoryForm.iconUrl = fileUrl
    message.success('分类图标上传成功')
    option?.onSuccess?.(fileUrl)
  } catch (error) {
    message.error('分类图标上传失败，请检查图片格式或 OSS 配置后重试')
    option?.onError?.(error)
  } finally {
    categoryUploading.value = false
  }
}

const formatDate = (value?: string) => {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadBannerList()
  loadCategoryList()
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
  max-width: 760px;
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

.content-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
}

.info-card,
.tab-card {
  border-radius: 20px;
  border: 1px solid #e8eef7;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-box {
  padding: 18px 16px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #e8eef7;
}

.metric-label {
  display: block;
  font-size: 13px;
  color: #7a8699;
}

.metric-value {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  color: #182230;
  line-height: 1;
}

.info-list {
  margin: 0;
  padding-left: 18px;
  color: #445268;
  line-height: 2;
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

.inner-toolbar {
  margin-bottom: 18px;
  box-shadow: none;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.image-cell {
  width: 72px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f7fb;
  border: 1px solid #edf1f7;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.icon-cell {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #edf1f7;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-preview {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.upload-panel {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.upload-preview {
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

.icon-preview-box {
  width: 112px;
  height: 112px;
}

.upload-preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-preview-image.contain {
  object-fit: contain;
}

.upload-placeholder {
  color: #98a2b3;
  font-size: 14px;
}

.upload-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
}

.full-width {
  width: 100%;
}

:global(.asset-form-modal) {
  max-width: calc(100vw - 32px);
}

:global(.asset-form-modal .ant-modal-content) {
  border-radius: 8px;
  overflow: hidden;
}

:global(.asset-form-modal .ant-modal-body) {
  max-height: calc(100vh - 170px);
  overflow-y: auto;
}

:global(.asset-icon-modal .ant-modal-body) {
  padding-bottom: 18px;
}

:deep(.ant-tabs-nav) {
  margin-bottom: 18px;
}

:deep(.ant-table-wrapper .ant-table-thead > tr > th) {
  background: #f8fbff;
  color: #344054;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .page-header-card,
  .content-grid,
  .toolbar-card {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .search-input {
    width: 220px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .upload-panel {
    flex-direction: column;
  }

  .upload-preview {
    width: 100%;
  }
}
</style>
