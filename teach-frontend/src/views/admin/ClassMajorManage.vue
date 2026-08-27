<template>
  <div class="admin-ops-page">
    <section class="admin-ops-header">
      <div>
        <h2>班级专业管理</h2>
        <p>维护班级、专业、学院信息，并查看当前绑定学生数量。这里的数据会被学生账号、导入中心和统计分析共同使用。</p>
      </div>
      <div class="admin-ops-tags">
        <span>真实数据库</span>
        <span>专业筛选</span>
        <span>学生绑定</span>
      </div>
    </section>

    <section class="admin-ops-card curriculum-card">
      <div class="admin-ops-card-head">
        <div>
          <h3>专业四年课程安排</h3>
          <p>按专业维护 1-8 学期课程，教师课表会从这里选择课程名称，和视频课程无关。</p>
        </div>
        <a-button type="primary" :disabled="!curriculumQuery.major" @click="openCurriculumEditor()">新增课程</a-button>
      </div>
      <div class="curriculum-toolbar">
        <a-select v-model:value="curriculumQuery.major" class="admin-ops-select" :options="majorOptions" @change="loadCurriculum" />
        <a-select v-model:value="curriculumQuery.semesterNo" class="admin-ops-select" :options="semesterOptions" @change="loadCurriculum" />
      </div>
      <a-table
        row-key="id"
        size="middle"
        :columns="curriculumColumns"
        :data-source="curriculumList"
        :loading="curriculumLoading"
        :pagination="false"
        :scroll="{ x: 820 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'semesterNo'">
            第 {{ record.semesterNo }} 学期
          </template>
          <template v-else-if="column.dataIndex === 'courseType'">
            <a-tag :color="record.courseType === 'elective' ? 'orange' : 'blue'">
              {{ record.courseType === 'elective' ? '选修' : '必修' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="openCurriculumEditor(record)">编辑</a-button>
            <a-button type="link" danger @click="handleDeleteCurriculum(record)">删除</a-button>
          </template>
        </template>
      </a-table>
    </section>

    <section class="admin-ops-toolbar">
      <div class="admin-ops-toolbar-left">
        <a-select v-model:value="query.major" class="admin-ops-select" :options="majorOptions" @change="handleSearch" />
        <a-input v-model:value="query.keyword" class="admin-ops-input" placeholder="搜索班级、专业、学院" allow-clear @pressEnter="handleSearch" />
      </div>
      <div class="admin-ops-toolbar-right">
        <a-button type="primary" @click="openEditor()">新增班级</a-button>
        <a-button @click="handleReset">重置</a-button>
      </div>
    </section>

    <section class="admin-ops-card">
      <div class="admin-ops-card-head">
        <div>
          <h3>班级专业管理</h3>
          <p>维护班级、专业、学院信息，并显示当前绑定学生数。</p>
        </div>
      </div>
      <a-table
        row-key="id"
        size="middle"
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 860 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" @click="openEditor(record)">编辑</a-button>
            <a-button type="link" danger :disabled="record.studentCount > 0" @click="handleDelete(record)">删除</a-button>
          </template>
          <template v-else-if="column.dataIndex === 'studentCount'">
            <a-tag color="blue">{{ record.studentCount || 0 }} 人</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>
        </template>
      </a-table>
    </section>

    <a-modal v-model:open="editorOpen" :title="form.id ? '编辑班级' : '新增班级'" width="520px" @ok="handleSave">
      <a-form layout="vertical">
        <a-form-item label="班级名称" required>
          <a-input v-model:value="form.name" placeholder="例如：25级软件工程1班" />
        </a-form-item>
        <a-form-item label="专业">
          <a-input v-model:value="form.major" placeholder="例如：软件工程" />
        </a-form-item>
        <a-form-item label="学院">
          <a-input v-model:value="form.college" placeholder="例如：计算机学院" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="curriculumEditorOpen" :title="curriculumForm.id ? '编辑专业课程' : '新增专业课程'" width="560px" @ok="handleSaveCurriculum">
      <a-form layout="vertical">
        <a-form-item label="专业" required>
          <a-input v-model:value="curriculumForm.major" placeholder="例如：软件工程" />
        </a-form-item>
        <a-form-item label="学期" required>
          <a-select v-model:value="curriculumForm.semesterNo" :options="semesterEditOptions" />
        </a-form-item>
        <a-form-item label="课程名称" required>
          <a-input v-model:value="curriculumForm.courseName" placeholder="例如：数据结构" />
        </a-form-item>
        <div class="form-grid">
          <a-form-item label="课程性质">
            <a-select v-model:value="curriculumForm.courseType" :options="courseTypeOptions" />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number v-model:value="curriculumForm.sortOrder" :min="0" class="full-input" />
          </a-form-item>
        </div>
        <div class="form-grid">
          <a-form-item label="学分">
            <a-input-number v-model:value="curriculumForm.credits" :min="0" :step="0.5" class="full-input" />
          </a-form-item>
          <a-form-item label="学时">
            <a-input-number v-model:value="curriculumForm.hours" :min="0" class="full-input" />
          </a-form-item>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  deleteAdminClass,
  deleteMajorCurriculumCourse,
  getAdminClassMajors,
  getAdminClassManageList,
  getMajorCurriculumCourses,
  saveAdminClass,
  saveMajorCurriculumCourse
} from '@/api/admin'
import type { AdminClassItem, AdminClassListParams, AdminClassSavePayload, MajorCurriculumCourseItem } from '@/types/admin'
import './AdminOps.css'

const loading = ref(false)
const list = ref<AdminClassItem[]>([])
const total = ref(0)
const majors = ref<string[]>([])
const editorOpen = ref(false)
const curriculumLoading = ref(false)
const curriculumEditorOpen = ref(false)
const curriculumList = ref<MajorCurriculumCourseItem[]>([])
const query = reactive<AdminClassListParams>({ current: 1, size: 10, keyword: '', major: '' })
const form = reactive<AdminClassSavePayload>({ id: undefined, name: '', major: '', college: '' })
const curriculumQuery = reactive<{ major: string; semesterNo?: number }>({ major: '', semesterNo: undefined })
const curriculumForm = reactive<MajorCurriculumCourseItem>({
  id: undefined,
  major: '',
  semesterNo: 1,
  courseName: '',
  courseType: 'required',
  credits: undefined,
  hours: undefined,
  sortOrder: 0
})

const majorOptions = computed(() => [
  { label: '全部专业', value: '' },
  ...majors.value.map((item) => ({ label: item, value: item }))
])

const columns = [
  { title: '班级名称', dataIndex: 'name', width: 220 },
  { title: '专业', dataIndex: 'major', width: 180 },
  { title: '学院', dataIndex: 'college', width: 180 },
  { title: '学生数', dataIndex: 'studentCount', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 150 }
]

const semesterOptions = [
  { label: '全部学期', value: undefined },
  ...Array.from({ length: 8 }, (_, index) => ({ label: `第 ${index + 1} 学期`, value: index + 1 }))
]

const semesterEditOptions = Array.from({ length: 8 }, (_, index) => ({ label: `第 ${index + 1} 学期`, value: index + 1 }))

const courseTypeOptions = [
  { label: '必修', value: 'required' },
  { label: '选修', value: 'elective' }
]

const curriculumColumns = [
  { title: '学期', dataIndex: 'semesterNo', width: 120 },
  { title: '课程名称', dataIndex: 'courseName' },
  { title: '性质', dataIndex: 'courseType', width: 100 },
  { title: '学分', dataIndex: 'credits', width: 90 },
  { title: '学时', dataIndex: 'hours', width: 90 },
  { title: '排序', dataIndex: 'sortOrder', width: 90 },
  { title: '操作', key: 'action', width: 140 }
]

const pagination = computed(() => ({
  current: query.current,
  pageSize: query.size,
  total: total.value,
  showSizeChanger: true,
  showTotal: (value: number) => `共 ${value} 条`
}))

const loadMajors = async () => {
  majors.value = await getAdminClassMajors()
  if (!curriculumQuery.major && majors.value.length) {
    curriculumQuery.major = majors.value[0]
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getAdminClassManageList(query)
    list.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const openEditor = (record?: AdminClassItem) => {
  form.id = record?.id
  form.name = record?.name || ''
  form.major = record?.major || ''
  form.college = record?.college || ''
  editorOpen.value = true
}

const handleSave = async () => {
  if (!form.name?.trim()) {
    message.warning('请填写班级名称')
    return
  }
  await saveAdminClass({ ...form })
  editorOpen.value = false
  await loadMajors()
  await loadList()
}

const loadCurriculum = async () => {
  if (!curriculumQuery.major) {
    curriculumList.value = []
    return
  }
  curriculumLoading.value = true
  try {
    curriculumList.value = await getMajorCurriculumCourses(curriculumQuery)
  } finally {
    curriculumLoading.value = false
  }
}

const openCurriculumEditor = (record?: MajorCurriculumCourseItem) => {
  curriculumForm.id = record?.id
  curriculumForm.major = record?.major || curriculumQuery.major || ''
  curriculumForm.semesterNo = record?.semesterNo || curriculumQuery.semesterNo || 1
  curriculumForm.courseName = record?.courseName || ''
  curriculumForm.courseType = record?.courseType || 'required'
  curriculumForm.credits = record?.credits
  curriculumForm.hours = record?.hours
  curriculumForm.sortOrder = record?.sortOrder || 0
  curriculumEditorOpen.value = true
}

const handleSaveCurriculum = async () => {
  if (!curriculumForm.major?.trim() || !curriculumForm.courseName?.trim()) {
    message.warning('请填写专业和课程名称')
    return
  }
  await saveMajorCurriculumCourse({ ...curriculumForm })
  curriculumEditorOpen.value = false
  curriculumQuery.major = curriculumForm.major
  await loadMajors()
  await loadCurriculum()
}

const handleDeleteCurriculum = (record: MajorCurriculumCourseItem) => {
  if (!record.id) return
  Modal.confirm({
    title: '删除专业课程',
    content: `确定删除“${record.courseName}”吗？`,
    okButtonProps: { danger: true },
    onOk: async () => {
      await deleteMajorCurriculumCourse(record.id!)
      await loadCurriculum()
    }
  })
}

const handleDelete = (record: AdminClassItem) => {
  Modal.confirm({
    title: '删除班级',
    content: `确定删除“${record.name}”吗？`,
    okButtonProps: { danger: true },
    onOk: async () => {
      await deleteAdminClass(record.id)
      await loadMajors()
      await loadList()
    }
  })
}

const handleSearch = () => {
  query.current = 1
  loadList()
}

const handleReset = () => {
  query.current = 1
  query.size = 10
  query.keyword = ''
  query.major = ''
  loadList()
}

const handleTableChange = (pageInfo: any) => {
  query.current = pageInfo.current
  query.size = pageInfo.pageSize
  loadList()
}

const formatDate = (value?: string) => value ? value.replace('T', ' ').slice(0, 19) : '-'

onMounted(async () => {
  await loadMajors()
  await loadList()
  await loadCurriculum()
})
</script>

<style scoped>
.curriculum-card {
  margin-top: 18px;
}

.curriculum-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.full-input {
  width: 100%;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
