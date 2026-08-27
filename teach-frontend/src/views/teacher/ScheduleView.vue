<template>
  <div class="schedule-page">
    <div class="page-header">
      <div class="title-group">
        <h2><calendar-outlined class="title-icon" /> 教务安排</h2>
        <p class="subtitle">按学期管理每周授课安排与课程计划。</p>
      </div>
      <div class="header-actions">
        <a-select v-model:value="selectedSemester" class="semester-select" size="large" @change="() => loadSchedules()">
          <a-select-option v-for="item in semesterOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </a-select-option>
        </a-select>
        <a-button type="primary" size="large" class="primary-btn" @click="openModal('add')">
          <plus-outlined /> 添加课程
        </a-button>
      </div>
    </div>

    <div class="schedule-grid-wrapper glass-panel">
      <div class="table-scroll-area">
        <table class="schedule-table">
          <thead>
          <tr>
            <th class="time-header">节次 / 星期</th>
            <th v-for="day in days" :key="day.value" class="day-header" :class="{ active: day.value === today }">
              <div class="day-name">{{ day.label }}</div>
              <div class="day-date">{{ day.short }}</div>
            </th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="period in periods" :key="period">
            <td class="period-cell">
              <div class="period-num">第{{ period }}节</div>
            </td>
            <template v-for="day in days" :key="day.value">
              <td
                v-if="!isOccupied(day.value, period)"
                class="slot-cell"
                :rowspan="getRowspan(day.value, period)"
                @click="handleCellClick(day.value, period)"
              >
                <template v-for="item in getSchedules(day.value, period)" :key="item.id">
                  <div
                    class="schedule-card"
                    :style="{ backgroundColor: getCardColor(item.id) }"
                    @click.stop="openModal('edit', item)"
                  >
                    <div class="card-course">{{ item.courseName }}</div>
                    <div class="card-class">{{ item.className }}</div>
                    <div class="card-week">第{{ item.weekStart }}-{{ item.weekEnd }}周</div>
                  </div>
                </template>
              </td>
            </template>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 添加/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalType === 'add' ? '添加' : '编辑'"
      :confirmLoading="submitLoading"
      width="520px"
      centered
    >
      <template #footer>
        <a-row>
          <a-col :span="23">
            <div class="modal-footer">
              <div class="footer-left">
                <a-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</a-button>
                <a-button @click="modalVisible = false">取消</a-button>
              </div>
              <div class="footer-right">
                <a-button v-if="modalType === 'edit'" type="primary" danger @click="handleDelete">删除</a-button>
              </div>
            </div>
          </a-col>
        </a-row>
      </template>
      <a-form layout="horizontal" :model="formState" :labelCol="{ span: 5 }" :wrapperCol="{ span: 18 }" class="schedule-form">

        <a-form-item label="课程" required>
          <div class="form-row-flex">
            <a-auto-complete
              v-model:value="formState.courseName"
              :options="curriculumCourseOptions"
              :loading="curriculumLoading"
              placeholder="选择或输入课程名称"
              class="full-width"
            />
            <a-button type="link" @click="courseLinkVisible = true" class="action-link-btn">关联网络课程</a-button>
          </div>
        </a-form-item>

        <a-form-item label="学期" required>
          <a-select v-model:value="formState.semesterLabel" placeholder="请选择学期" class="full-width">
            <a-select-option v-for="item in semesterOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="班级" required>
          <a-select v-model:value="formState.className" placeholder="请选择班级" class="full-width">
            <a-select-option v-for="cls in myClassList" :key="cls.id" :value="cls.name">{{ cls.name }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="上课周次" required>
          <a-select v-model:value="weekRange" placeholder="请选择" class="full-width">
            <a-select-option v-for="w in weekOptions" :key="w" :value="w">{{ w }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="上课时间" required>
          <a-select v-model:value="formState.dayOfWeek" placeholder="请选择" class="full-width">
            <a-select-option v-for="d in days" :key="d.value" :value="d.value">{{ d.label }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="开始节次" required>
          <a-select v-model:value="formState.startPeriod" placeholder="请选择" class="full-width">
            <a-select-option v-for="p in periods" :key="p" :value="p">第{{ p }}节</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="结束节次" required>
          <a-select v-model:value="formState.endPeriod" placeholder="请选择" class="full-width">
            <a-select-option v-for="p in periods" :key="p" :value="p">第{{ p }}节</a-select-option>
          </a-select>
        </a-form-item>

      </a-form>
    </a-modal>

    <!-- 关联网络课程弹窗 -->
    <a-modal v-model:open="courseLinkVisible" title="关联网络课程" width="700px" :footer="null" centered>
      <a-input-search v-model:value="courseSearchKeyword" placeholder="搜索课程名称..." @search="loadCourseList" style="margin-bottom: 16px" />
      <a-table :dataSource="courseList" :columns="courseColumns" rowKey="id" size="small" :loading="courseListLoading">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" @click="linkCourse(record)">选择</a-button>
          </template>
        </template>
      </a-table>
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  CalendarOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import request from '@/utils/request'
import {
  fetchTeacherSchedules,
  addSchedule,
  updateSchedule,
  deleteSchedule,
  fetchCurriculumCourseOptions,
  type TeacherSchedule
} from '@/api/schedule'
import { buildSemesterOptions, getCurrentSemesterValue } from '@/utils/semester'

const days = [
  { label: '周一', value: 1, short: 'Mon' },
  { label: '周二', value: 2, short: 'Tue' },
  { label: '周三', value: 3, short: 'Wed' },
  { label: '周四', value: 4, short: 'Thu' },
  { label: '周五', value: 5, short: 'Fri' },
  { label: '周六', value: 6, short: 'Sat' },
  { label: '周日', value: 7, short: 'Sun' },
]

const periods = [1, 2, 3, 4, 5, 6, 7, 8]
const today = new Date().getDay() || 7

const semesterOptions = buildSemesterOptions()
const selectedSemester = ref(getCurrentSemesterValue())

const scheduleList = ref<TeacherSchedule[]>([])
const loading = ref(false)
const curriculumLoading = ref(false)
const curriculumCourseOptions = ref<Array<{ label: string; value: string }>>([])

const weekOptions = Array.from({ length: 18 }, (_, i) => {
  const s = i + 1
  const e = 18
  return `第${s}-${e}周`
})

const cardColors = ['#e0e7ff', '#dbeafe', '#dcfce7', '#fef3c7', '#fce7f3', '#f3e8ff', '#ccfbf1']
function getCardColor(id: number) {
  return cardColors[id % cardColors.length]
}

function getSchedules(dayOfWeek: number, period: number) {
  return scheduleList.value.filter(s =>
    s.dayOfWeek === dayOfWeek &&
    s.startPeriod === period
  )
}

function isOccupied(dayOfWeek: number, period: number) {
  return scheduleList.value.some(s =>
    s.dayOfWeek === dayOfWeek &&
    s.startPeriod < period &&
    s.endPeriod >= period
  )
}

function getRowspan(dayOfWeek: number, period: number) {
  const items = getSchedules(dayOfWeek, period)
  if (items.length > 0) {
    return items[0].endPeriod - items[0].startPeriod + 1
  }
  return 1
}

async function loadSchedules() {
  loading.value = true
  try {
    scheduleList.value = await fetchTeacherSchedules(selectedSemester.value)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadSchedules)

// 教师带的班级列表
const myClassList = ref<any[]>([])
async function loadMyClasses() {
  try {
    const data: any[] = await request.get('/class/my-classes', { skipErrorToast: true })
    myClassList.value = data || []
  } catch (e) {
    console.error(e)
  }
}
onMounted(loadMyClasses)

// Modal
const modalVisible = ref(false)
const modalType = ref<'add' | 'edit'>('add')
const submitLoading = ref(false)
const weekRange = ref('第1-18周')

const formState = reactive({
  id: undefined as number | undefined,
  courseName: '',
  linkedCourseId: undefined as number | undefined,
  className: '',
  weekStart: 1,
  weekEnd: 18,
  dayOfWeek: 3,
  startPeriod: 1,
  endPeriod: 1,
  semesterLabel: selectedSemester.value,
})

function resolveSemesterNo(className?: string, semesterLabel?: string) {
  const term = Number((semesterLabel || '').match(/-(1|2)$/)?.[1] || 1)
  const gradeText = (className || '').match(/(\d{2})级/)?.[1]
  const startYear = Number((semesterLabel || '').match(/^(\d{4})-/)?.[1])
  if (!gradeText || !startYear) return term
  const entryYear = 2000 + Number(gradeText)
  return Math.min(8, Math.max(1, (startYear - entryYear) * 2 + term))
}

async function loadCurriculumCourseOptions() {
  if (!formState.className) {
    curriculumCourseOptions.value = []
    return
  }
  curriculumLoading.value = true
  try {
    const semesterNo = resolveSemesterNo(formState.className, formState.semesterLabel)
    const list = await fetchCurriculumCourseOptions(formState.className, semesterNo)
    curriculumCourseOptions.value = (list || []).map(item => ({
      label: `第${item.semesterNo}学期 · ${item.courseName}${item.courseType === 'elective' ? '（选修）' : ''}`,
      value: item.courseName
    }))
  } finally {
    curriculumLoading.value = false
  }
}

watch(() => [formState.className, formState.semesterLabel], () => {
  void loadCurriculumCourseOptions()
})

watch(weekRange, (val) => {
  const match = val.match(/第(\d+)-(\d+)周/)
  if (match) {
    formState.weekStart = parseInt(match[1])
    formState.weekEnd = parseInt(match[2])
  }
})

function resetForm() {
  formState.id = undefined
  formState.courseName = ''
  formState.linkedCourseId = undefined
  formState.className = ''
  formState.weekStart = 1
  formState.weekEnd = 18
  formState.dayOfWeek = 3
  formState.startPeriod = 1
  formState.endPeriod = 1
  formState.semesterLabel = selectedSemester.value
  weekRange.value = '第1-18周'
}

function openModal(type: 'add' | 'edit', record?: TeacherSchedule) {
  modalType.value = type
  resetForm()
  if (type === 'edit' && record) {
    formState.id = record.id
    formState.courseName = record.courseName
    formState.linkedCourseId = record.linkedCourseId
    formState.className = record.className || ''
    formState.weekStart = record.weekStart
    formState.weekEnd = record.weekEnd
    formState.dayOfWeek = record.dayOfWeek
    formState.startPeriod = record.startPeriod
    formState.endPeriod = record.endPeriod
    formState.semesterLabel = record.semesterLabel
    weekRange.value = `第${record.weekStart}-${record.weekEnd}周`
  }
  modalVisible.value = true
}

function handleCellClick(dayOfWeek: number, period: number) {
  resetForm()
  formState.dayOfWeek = dayOfWeek
  formState.startPeriod = period
  formState.endPeriod = period
  modalType.value = 'add'
  modalVisible.value = true
}

async function handleSubmit() {
  if (!formState.courseName.trim()) {
    message.warning('请填写课程名称')
    return
  }
  if (!formState.className) {
    message.warning('请选择班级')
    return
  }
  if (!formState.semesterLabel) {
    message.warning('请选择学期')
    return
  }
  if (formState.startPeriod > formState.endPeriod) {
    message.warning('开始节次不能大于结束节次')
    return
  }

  submitLoading.value = true
  try {
    const payload = {
      courseName: formState.courseName,
      linkedCourseId: formState.linkedCourseId,
      className: formState.className,
      weekStart: formState.weekStart,
      weekEnd: formState.weekEnd,
      dayOfWeek: formState.dayOfWeek,
      startPeriod: formState.startPeriod,
      endPeriod: formState.endPeriod,
      semesterLabel: formState.semesterLabel,
    }
    if (modalType.value === 'add') {
      await addSchedule(payload)
      message.success('添加成功')
    } else {
      await updateSchedule({ id: formState.id!, ...payload })
      message.success('修改成功')
    }
    modalVisible.value = false
    selectedSemester.value = formState.semesterLabel
    loadSchedules()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete() {
  if (!formState.id) return
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除课程「${formState.courseName}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteSchedule(formState.id!)
        message.success('删除成功')
        modalVisible.value = false
        loadSchedules()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}

// 关联课程弹窗
const courseLinkVisible = ref(false)
const courseSearchKeyword = ref('')
const courseList = ref<any[]>([])
const courseListLoading = ref(false)
const courseColumns = [
  { title: '课程名称', dataIndex: 'name', key: 'name' },
  { title: '教师', dataIndex: 'teacherName', key: 'teacherName' },
  { title: '操作', key: 'action', width: 80 },
]

async function loadCourseList() {
  courseListLoading.value = true
  try {
    const res: any = await request.get('/course/list/page', {
      params: { current: 1, size: 50, name: courseSearchKeyword.value || undefined },
      skipErrorToast: true,
    })
    courseList.value = res?.records || []
  } catch (e) {
    console.error(e)
  } finally {
    courseListLoading.value = false
  }
}

watch(courseLinkVisible, (v) => { if (v) loadCourseList() })

function linkCourse(record: any) {
  formState.linkedCourseId = record.id
  formState.courseName = record.name
  courseLinkVisible.value = false
}

</script>

<style scoped>
/* ========== 页面容器：固定高度，无滚动 ========== */
.schedule-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ========== 页面头部 ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  margin-bottom: 16px;
}
.title-group h2 {
  margin: 0;
  font-size: 28px;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
}
.title-icon { color: #4f46e5; }
.subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 15px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.semester-select {
  width: 220px;
}
.semester-select :deep(.ant-select-selector) {
  border-radius: 5px !important;
  font-weight: 600;
}
.primary-btn {
  background: linear-gradient(135deg, #4f46e5, #3b82f6);
  border: none; border-radius: 5px; font-weight: 600;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
  height: 40px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.primary-btn:hover {
  background: linear-gradient(135deg, #4338ca, #2563eb);
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.45);
}

/* ========== 课表卡片容器：撑满剩余空间 ========== */
.schedule-grid-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 5px;
  padding: 16px;
  background: #ffffff;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
  border: 1px solid #f1f5f9;
}

/* ========== 表格区域 (已取消滚动，撑满高度) ========== */
.table-scroll-area {
  flex: 1;
  height: 100%;
  overflow: hidden; /* 改为了 hidden，彻底禁止拖动和滚动 */
  display: flex; /* 确保表格在极少情况下也能计算出高度 */
  flex-direction: column;
}

/* ========== 课表表格样式 ========== */
.schedule-table {
  width: 100%;
  height: 100%; /* 新增：让表格的高度被拉伸，填满整个 100% 容器 */
  border-collapse: separate;
  border-spacing: 3px;
  table-layout: fixed;
}
.schedule-table th,
.schedule-table td {
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  text-align: center;
}

/* 节次列表头 */
.time-header {
  width: 80px;
  min-width: 80px;
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
  padding: 10px 4px;
}

/* 星期列表头 */
.day-header {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  padding: 12px 8px;
  transition: all 0.2s;
}
.day-header.active {
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  color: #4f46e5;
}
.day-header.active .day-date {
  color: #818cf8;
}
.day-name { font-size: 14px; }
.day-date { font-size: 11px; color: #94a3b8; margin-top: 1px; }

/* 节次单元格 */
.period-cell {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
  padding: 12px 6px;
}
.period-num { font-size: 13px; }

/* 可点击的格子 */
.slot-cell {
  vertical-align: top;
  padding: 6px;
  cursor: pointer;
  transition: background 0.2s;
  background: #ffffff;
  /* 去除了原先的 height: 90px，让表格自动根据 100% 的总高度去平分每行的高度 */
}
.slot-cell:hover {
  background: #f8fafc;
}

/* 课程卡片 */
.schedule-card {
  border-radius: 5px;
  padding: 10px 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  height: 100%;
  overflow: hidden;
}
.schedule-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}
.card-course {
  font-weight: 700;
  font-size: 13px;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-class {
  font-size: 12px;
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-week {
  font-size: 11px;
  color: #64748b;
}

/* ========== 弹窗表单样式优化 ========== */
.schedule-form {
  margin-top: 20px;
}
.schedule-form .full-width {
  width: 100%; /* 强制所有输入框统一填满外层容器 */
}
.schedule-form .form-row-flex {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}
.schedule-form .form-row-flex .full-width {
  flex: 1; /* 让输入框占据剩余所有空间 */
}
.schedule-form .action-link-btn {
  padding: 0;
  flex-shrink: 0;
}

/* 弹窗底部：精准对齐的左右布局 */
/* 弹窗底部：精准对齐的左右布局 */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.footer-left {
  display: flex;
  align-items: center;
  gap: 12px;
  /* 填补 label 右对齐产生的左侧留白，使按钮边缘精准贴合上方 * 号 */
  margin-left: 22px;
}
.footer-right {
  display: flex;
  align-items: center;
}
</style>
