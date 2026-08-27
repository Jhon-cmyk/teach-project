<template>
  <div class="admin-page">
    <section class="page-header-card">
      <div class="page-copy">
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">
          学生、教师、管理员分开管理。教师注册号和职称只出现在教师视图里，学生视图只保留班级相关信息。
        </p>
      </div>
      <div class="page-tags">
        <span>学生账号</span>
        <span>教师账号</span>
        <span>教师注册号</span>
      </div>
    </section>

    <a-tabs v-model:activeKey="activeTab" class="user-tabs" @change="handleTabChange">
      <a-tab-pane key="students" tab="学生账号">
        <section class="toolbar-card">
          <div class="toolbar-left">
            <a-select
              v-model:value="query.major"
              class="toolbar-major-select"
              :options="majorOptions"
              placeholder="专业"
              allow-clear
              show-search
              option-filter-prop="label"
              @change="handleMajorChange"
            />
            <a-select
              v-model:value="query.classId"
              class="toolbar-class-select"
              :options="classFilterOptions"
              :loading="classLoading"
              placeholder="班级"
              allow-clear
              show-search
              option-filter-prop="label"
              @change="handleSearch"
            />
            <a-button @click="handleReset">
              <template #icon><ReloadOutlined /></template>
              重置筛选
            </a-button>
          </div>
          <div class="toolbar-right">
            <a-input
              v-model:value="query.keyword"
              class="search-input"
              placeholder="搜索学号、姓名"
              allow-clear
              @pressEnter="handleSearch"
            >
              <template #prefix><SearchOutlined /></template>
            </a-input>
            <a-button type="primary" @click="handleSearch">查询</a-button>
          </div>
        </section>

        <a-card class="table-card" :bordered="false">
          <a-table
            row-key="id"
            :columns="studentColumns"
            :data-source="userList"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 760 }"
            :locale="{ emptyText }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'classDisplay'">
                {{ record.classDisplay || '未分配' }}
              </template>
              <template v-else-if="column.dataIndex === 'createTime'">
                {{ formatDate(record.createTime) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <div class="action-group">
                  <a-button type="link" @click="openClassModal(record)">设置班级</a-button>
                  <a-button type="link" danger @click="handleDelete(record)">
                    <template #icon><DeleteOutlined /></template>
                    删除
                  </a-button>
                </div>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="teachers" tab="教师账号">
        <section class="toolbar-card">
          <div class="toolbar-left">
            <a-button @click="handleReset">
              <template #icon><ReloadOutlined /></template>
              重置筛选
            </a-button>
          </div>
          <div class="toolbar-right">
            <a-input
              v-model:value="query.keyword"
              class="search-input"
              placeholder="搜索工号、姓名、职称、注册号"
              allow-clear
              @pressEnter="handleSearch"
            >
              <template #prefix><SearchOutlined /></template>
            </a-input>
            <a-button type="primary" @click="handleSearch">查询</a-button>
          </div>
        </section>

        <a-card class="table-card" :bordered="false">
          <a-table
            row-key="id"
            :columns="teacherColumns"
            :data-source="userList"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 860 }"
            :locale="{ emptyText }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'teacherTitle'">
                {{ record.teacherTitle || '未设置' }}
              </template>
              <template v-else-if="column.dataIndex === 'teacherRegisterCode'">
                <span class="code-text">{{ record.teacherRegisterCode || '-' }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'createTime'">
                {{ formatDate(record.createTime) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <div class="action-group">
                  <a-button type="link" @click="openTeacherTitleModal(record)">设置职称</a-button>
                  <a-button type="link" danger @click="handleDelete(record)">
                    <template #icon><DeleteOutlined /></template>
                    删除
                  </a-button>
                </div>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="admins" tab="管理员账号">
        <section class="toolbar-card">
          <div class="toolbar-left">
            <a-button @click="handleReset">
              <template #icon><ReloadOutlined /></template>
              重置筛选
            </a-button>
          </div>
          <div class="toolbar-right">
            <a-input
              v-model:value="query.keyword"
              class="search-input"
              placeholder="搜索账号、昵称"
              allow-clear
              @pressEnter="handleSearch"
            >
              <template #prefix><SearchOutlined /></template>
            </a-input>
            <a-button type="primary" @click="handleSearch">查询</a-button>
          </div>
        </section>

        <a-card class="table-card" :bordered="false">
          <a-table
            row-key="id"
            :columns="adminColumns"
            :data-source="userList"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 620 }"
            :locale="{ emptyText }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'createTime'">
                {{ formatDate(record.createTime) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" danger @click="handleDelete(record)">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="codes" tab="教师注册号">
        <section class="toolbar-card">
          <div class="toolbar-left">
            <a-select v-model:value="codeQuery.status" class="toolbar-select" :options="codeStatusOptions" @change="handleCodeSearch" />
            <a-input
              v-model:value="codeQuery.keyword"
              class="search-input"
              placeholder="搜索注册号、姓名、职称"
              allow-clear
              @pressEnter="handleCodeSearch"
            />
          </div>
          <div class="toolbar-right">
            <a-button @click="handleCodeReset">重置</a-button>
            <a-button type="primary" @click="openCodeModal">创建注册号</a-button>
          </div>
        </section>

        <a-card class="table-card" :bordered="false">
          <a-table
            row-key="id"
            :columns="codeColumns"
            :data-source="codeList"
            :loading="codeLoading"
            :pagination="codePagination"
            :scroll="{ x: 920 }"
            @change="handleCodeTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'registerCode'">
                <span class="code-text">{{ record.registerCode }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <a-tag :color="record.status === 'used' ? 'green' : 'blue'">
                  {{ record.status === 'used' ? '已使用' : '未使用' }}
                </a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'usedBy'">
                {{ record.usedBy || '-' }}
              </template>
              <template v-else-if="column.dataIndex === 'createTime'">
                {{ formatDate(record.createTime) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button v-if="record.status !== 'used'" type="link" danger @click="handleDeleteCode(record)">
                  删除
                </a-button>
                <span v-else class="muted">已绑定教师</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="classModalOpen"
      title="设置学生班级"
      width="520px"
      centered
      :confirm-loading="submitClassLoading"
      @ok="handleSubmitClass"
      destroyOnClose
    >
      <a-form layout="vertical">
        <a-form-item label="账号"><a-input :value="classForm.userAccount" disabled /></a-form-item>
        <a-form-item label="姓名"><a-input :value="classForm.userName" disabled /></a-form-item>
        <a-form-item label="班级">
          <a-select
            v-model:value="classForm.classId"
            :options="classOptions"
            :loading="classLoading"
            placeholder="请选择班级"
            allow-clear
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <div class="modal-hint">清空后提交，可以解除该学生当前班级绑定。</div>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="teacherTitleModalOpen"
      title="设置教师职称"
      width="480px"
      centered
      :confirm-loading="submitTeacherTitleLoading"
      @ok="handleSubmitTeacherTitle"
      destroyOnClose
    >
      <a-form layout="vertical">
        <a-form-item label="教师账号"><a-input :value="teacherTitleForm.userAccount" disabled /></a-form-item>
        <a-form-item label="教师姓名"><a-input :value="teacherTitleForm.userName" disabled /></a-form-item>
        <a-form-item label="职称" required>
          <a-select v-model:value="teacherTitleForm.teacherTitle" :options="teacherTitleOptions" placeholder="请选择职称" show-search />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="codeModalOpen"
      title="创建教师注册号"
      width="520px"
      centered
      :confirm-loading="submitCodeLoading"
      @ok="handleCreateCode"
      destroyOnClose
    >
      <a-form layout="vertical">
        <a-form-item label="注册号">
          <a-input v-model:value="codeForm.registerCode" placeholder="不填则自动生成" />
        </a-form-item>
        <a-form-item label="教师姓名">
          <a-input v-model:value="codeForm.teacherName" placeholder="可选，用于备注发放对象" />
        </a-form-item>
        <a-form-item label="教师职称" required>
          <a-select v-model:value="codeForm.teacherTitle" :options="teacherTitleOptions" placeholder="请选择职称" show-search />
        </a-form-item>
        <div class="modal-hint">教师注册时必须填写这个注册号。注册成功后，注册号会自动变为已使用。</div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import {
  createTeacherRegistrationCode,
  deleteAdminUser,
  deleteTeacherRegistrationCode,
  getAdminClassList,
  getAdminUserList,
  getTeacherRegistrationCodeList,
  updateAdminTeacherTitle,
  updateAdminUserClass
} from '@/api/admin'
import type {
  AdminClassItem,
  AdminUserItem,
  AdminUserListParams,
  TeacherRegistrationCodeItem,
  TeacherRegistrationCodeParams
} from '@/types/admin'

const activeTab = ref('students')
const loading = ref(false)
const classLoading = ref(false)
const submitClassLoading = ref(false)
const submitTeacherTitleLoading = ref(false)
const submitCodeLoading = ref(false)
const codeLoading = ref(false)
const classModalOpen = ref(false)
const teacherTitleModalOpen = ref(false)
const codeModalOpen = ref(false)

const query = reactive<AdminUserListParams>({
  current: 1,
  size: 10,
  keyword: '',
  role: 'student',
  major: undefined,
  classId: undefined
})

const codeQuery = reactive<TeacherRegistrationCodeParams>({
  current: 1,
  size: 10,
  status: '',
  keyword: ''
})

const userList = ref<AdminUserItem[]>([])
const classList = ref<AdminClassItem[]>([])
const codeList = ref<TeacherRegistrationCodeItem[]>([])
const total = ref(0)
const codeTotal = ref(0)

const classForm = reactive({
  id: undefined as number | undefined,
  userAccount: '',
  userName: '',
  classId: undefined as number | null | undefined
})

const teacherTitleForm = reactive({
  id: undefined as number | undefined,
  userAccount: '',
  userName: '',
  teacherTitle: ''
})

const codeForm = reactive({
  registerCode: '',
  teacherName: '',
  teacherTitle: ''
})

const teacherTitleOptions = [
  { label: '助教', value: '助教' },
  { label: '讲师', value: '讲师' },
  { label: '副教授', value: '副教授' },
  { label: '教授', value: '教授' },
  { label: '实验师', value: '实验师' },
  { label: '高级实验师', value: '高级实验师' }
]

const codeStatusOptions = [
  { label: '全部状态', value: '' },
  { label: '未使用', value: 'unused' },
  { label: '已使用', value: 'used' }
]

const studentColumns = [
  { title: '学号', dataIndex: 'userAccount', width: 150 },
  { title: '姓名', dataIndex: 'userName', width: 140 },
  { title: '班级信息', dataIndex: 'classDisplay', width: 220 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]

const teacherColumns = [
  { title: '工号/账号', dataIndex: 'userAccount', width: 150 },
  { title: '姓名', dataIndex: 'userName', width: 140 },
  { title: '教师职称', dataIndex: 'teacherTitle', width: 140 },
  { title: '教师注册号', dataIndex: 'teacherRegisterCode', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 190, fixed: 'right' }
]

const adminColumns = [
  { title: '账号', dataIndex: 'userAccount', width: 160 },
  { title: '昵称', dataIndex: 'userName', width: 160 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' }
]

const codeColumns = [
  { title: '注册号', dataIndex: 'registerCode', width: 180 },
  { title: '发放对象', dataIndex: 'teacherName', width: 130 },
  { title: '职称', dataIndex: 'teacherTitle', width: 120 },
  { title: '状态', dataIndex: 'status', width: 110 },
  { title: '绑定用户ID', dataIndex: 'usedBy', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' }
]

const classOptions = computed(() => classList.value.map((item) => {
  const meta = [item.college, item.major, typeof item.studentCount === 'number' ? `${item.studentCount} 人` : '']
    .filter(Boolean)
    .join(' / ')
  return { label: meta ? `${item.name}（${meta}）` : item.name, value: item.id }
}))

const majorOptions = computed(() => {
  const majors = Array.from(new Set(
    classList.value.map((item) => item.major).filter((major): major is string => Boolean(major))
  )).sort((first, second) => first.localeCompare(second, 'zh-CN'))

  return [
    { label: '全部专业', value: '' },
    ...majors.map((major) => ({ label: major, value: major }))
  ]
})

const classFilterOptions = computed(() => {
  const classes = query.major ? classList.value.filter((item) => item.major === query.major) : classList.value
  return [
    { label: '全部班级', value: null },
    ...classes.map((item) => {
      const meta = [item.major, typeof item.studentCount === 'number' ? `${item.studentCount} 人` : '']
        .filter(Boolean)
        .join(' / ')
      return { label: meta ? `${item.name}（${meta}）` : item.name, value: item.id }
    })
  ]
})

const pagination = computed(() => ({
  current: query.current,
  pageSize: query.size,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`
}))

const codePagination = computed(() => ({
  current: codeQuery.current,
  pageSize: codeQuery.size,
  total: codeTotal.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`
}))

const emptyText = computed(() => {
  if (loading.value) return '账号列表加载中...'
  if (query.keyword || query.major || query.classId) return '没有找到符合条件的账号'
  return '当前暂无账号数据'
})

const activeRole = computed(() => {
  if (activeTab.value === 'teachers') return 'teacher'
  if (activeTab.value === 'admins') return 'admin'
  return 'student'
})

const loadUserList = async () => {
  loading.value = true
  try {
    query.role = activeRole.value
    const res = await getAdminUserList(query)
    userList.value = res?.records || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const loadClassList = async () => {
  classLoading.value = true
  try {
    classList.value = await getAdminClassList() || []
  } catch (error: any) {
    message.error(error?.message || '班级列表加载失败')
  } finally {
    classLoading.value = false
  }
}

const loadCodeList = async () => {
  codeLoading.value = true
  try {
    const res = await getTeacherRegistrationCodeList(codeQuery)
    codeList.value = res?.records || []
    codeTotal.value = res?.total || 0
  } finally {
    codeLoading.value = false
  }
}

const handleTabChange = (key: string) => {
  if (key === 'codes') {
    loadCodeList()
    return
  }
  query.current = 1
  query.keyword = ''
  query.major = undefined
  query.classId = undefined
  loadUserList()
}

const handleSearch = () => {
  query.current = 1
  loadUserList()
}

const handleMajorChange = () => {
  if (query.major && query.classId) {
    const selectedClass = classList.value.find((item) => item.id === query.classId)
    if (selectedClass && selectedClass.major !== query.major) query.classId = undefined
  }
  handleSearch()
}

const handleReset = () => {
  query.current = 1
  query.size = 10
  query.keyword = ''
  query.major = undefined
  query.classId = undefined
  loadUserList()
}

const handleTableChange = (pageInfo: any) => {
  query.current = pageInfo.current
  query.size = pageInfo.pageSize
  loadUserList()
}

const handleCodeSearch = () => {
  codeQuery.current = 1
  loadCodeList()
}

const handleCodeReset = () => {
  codeQuery.current = 1
  codeQuery.size = 10
  codeQuery.status = ''
  codeQuery.keyword = ''
  loadCodeList()
}

const handleCodeTableChange = (pageInfo: any) => {
  codeQuery.current = pageInfo.current
  codeQuery.size = pageInfo.pageSize
  loadCodeList()
}

const openClassModal = (record: AdminUserItem) => {
  if (!classList.value.length) loadClassList()
  classForm.id = record.id
  classForm.userAccount = record.userAccount
  classForm.userName = record.userName || '未命名学生'
  classForm.classId = record.classId ?? undefined
  classModalOpen.value = true
}

const openTeacherTitleModal = (record: AdminUserItem) => {
  teacherTitleForm.id = record.id
  teacherTitleForm.userAccount = record.userAccount
  teacherTitleForm.userName = record.userName || '未命名教师'
  teacherTitleForm.teacherTitle = record.teacherTitle || ''
  teacherTitleModalOpen.value = true
}

const openCodeModal = () => {
  codeForm.registerCode = ''
  codeForm.teacherName = ''
  codeForm.teacherTitle = ''
  codeModalOpen.value = true
}

const handleSubmitClass = async () => {
  if (!classForm.id) return
  submitClassLoading.value = true
  try {
    await updateAdminUserClass({ id: classForm.id, classId: classForm.classId ?? null })
    classModalOpen.value = false
    loadUserList()
  } finally {
    submitClassLoading.value = false
  }
}

const handleSubmitTeacherTitle = async () => {
  if (!teacherTitleForm.id) return
  if (!teacherTitleForm.teacherTitle) {
    message.warning('请选择教师职称')
    return
  }
  submitTeacherTitleLoading.value = true
  try {
    await updateAdminTeacherTitle({
      id: teacherTitleForm.id,
      teacherTitle: teacherTitleForm.teacherTitle
    })
    teacherTitleModalOpen.value = false
    loadUserList()
  } finally {
    submitTeacherTitleLoading.value = false
  }
}

const handleCreateCode = async () => {
  if (!codeForm.teacherTitle) {
    message.warning('请选择教师职称')
    return
  }
  submitCodeLoading.value = true
  try {
    await createTeacherRegistrationCode({
      registerCode: codeForm.registerCode.trim() || undefined,
      teacherName: codeForm.teacherName.trim(),
      teacherTitle: codeForm.teacherTitle
    })
    message.success('教师注册号已创建')
    codeModalOpen.value = false
    loadCodeList()
  } finally {
    submitCodeLoading.value = false
  }
}

const handleDeleteCode = (record: TeacherRegistrationCodeItem) => {
  Modal.confirm({
    title: '删除教师注册号',
    content: `确定删除注册号“${record.registerCode}”吗？`,
    okText: '确认删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      await deleteTeacherRegistrationCode(record.id)
      loadCodeList()
    }
  })
}

const handleDelete = (record: AdminUserItem) => {
  Modal.confirm({
    title: '删除用户',
    content: `确定删除账号“${record.userAccount}”吗？此操作为逻辑删除。`,
    okText: '确认删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      await deleteAdminUser(record.id)
      if (userList.value.length === 1 && (query.current || 1) > 1) query.current = (query.current || 1) - 1
      loadUserList()
    }
  })
}

const formatDate = (value?: string) => value ? value.replace('T', ' ').slice(0, 19) : '-'

onMounted(() => {
  loadUserList()
  loadClassList()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header-card,
.toolbar-card,
.table-card {
  border: 1px solid #e8eef7;
  background: #ffffff;
}

.page-header-card {
  padding: 20px 22px;
  border-radius: 12px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: #182230;
}

.page-desc {
  margin: 8px 0 0;
  max-width: 820px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.page-tags,
.toolbar-left,
.toolbar-right,
.action-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.page-tags span {
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: #eef4ff;
  color: #1e4ed8;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  border: 1px solid #dce8ff;
}

.user-tabs {
  min-width: 0;
}

.toolbar-card {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-select {
  width: 150px;
}

.toolbar-major-select {
  width: 180px;
}

.toolbar-class-select,
.search-input {
  width: 260px;
}

.table-card {
  border-radius: 12px;
}

.code-text {
  color: #0f172a;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-weight: 700;
}

.muted,
.modal-hint {
  color: #94a3b8;
}

.modal-hint {
  font-size: 13px;
  line-height: 1.7;
}

:deep(.ant-card-body) {
  padding: 16px;
}

:deep(.ant-table-wrapper .ant-table-thead > tr > th) {
  background: #f8fbff;
  color: #344054;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .page-header-card,
  .toolbar-card {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .toolbar-left,
  .toolbar-right,
  .toolbar-select,
  .toolbar-major-select,
  .toolbar-class-select,
  .search-input,
  .toolbar-card .ant-btn {
    width: 100%;
  }
}
</style>
