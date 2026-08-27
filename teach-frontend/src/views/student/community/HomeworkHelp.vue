<template>
  <div class="hw-page">
    <div class="page-container">
      <div class="page-top">
        <button class="btn-back-link" @click="router.push({ name: 'CommunityList' })">
          <LeftOutlined />
          <span>返回学习交流</span>
        </button>
      </div>

      <div class="page-header">
        <div class="header-text">
          <h1 class="page-title">作业互助</h1>
          <p class="page-desc">围绕作业、实验和项目的常见问题交流</p>
        </div>

        <div class="header-actions">
          <div class="search-box">
            <SearchOutlined class="search-icon" />
            <input
              v-model="keyword"
              placeholder="搜索作业问题..."
              @keyup.enter="handleSearch"
            />
          </div>

          <button class="btn-ask" @click="handleAsk">
            <EditOutlined />
            <span>我要提问</span>
          </button>
        </div>
      </div>

      <div class="filter-bar">
        <div class="status-tabs">
          <button
            v-for="tab in statusFilters"
            :key="tab.key"
            class="status-tab"
            :class="{ active: currentStatus === tab.key }"
            @click="handleStatusChange(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="course-filters">
          <button
            v-for="c in courseFilters"
            :key="c.id"
            class="course-pill"
            :class="{ active: currentCourseId === c.id }"
            @click="handleCourseFilter(c.id)"
          >
            {{ c.name }}
          </button>
        </div>
      </div>

      <div class="list-card">
        <div v-if="loading" class="state-box">
          <a-spin />
          <span class="state-text">正在加载问题列表...</span>
        </div>

        <div v-else-if="error" class="state-box">
          <div class="state-icon-circle state-error-circle">!</div>
          <p class="state-text">加载失败，请稍后再试</p>
          <button class="btn-retry" @click="fetchList">重新加载</button>
        </div>

        <div v-else-if="questions.length === 0" class="state-box">
          <div class="state-icon-circle state-empty-circle">
            <MessageOutlined />
          </div>
          <p class="state-text">{{ keyword ? '没有找到相关问题' : '暂无作业提问' }}</p>
          <button v-if="keyword" class="btn-retry" @click="handleClearSearch">清除搜索</button>
        </div>

        <div v-else class="question-list">
          <div
            v-for="item in questions"
            :key="item.id"
            class="question-item"
            @click="goToDetail(item)"
          >
            <div class="q-status-bar" :class="item.status === 'resolved' ? 'resolved' : 'open'"></div>

            <div class="q-body">
              <div class="q-top">
                <h3 class="q-title">{{ item.title }}</h3>

                <div class="q-badges">
                  <span v-if="item.status === 'resolved'" class="badge badge-resolved">已解决</span>
                  <span v-else class="badge badge-open">待解决</span>
                  <span v-if="item.isTeacherAnswered" class="badge badge-teacher">老师已答</span>

                  <button
                    v-if="isTeacher && item.status === 'open'"
                    class="mini-action-btn"
                    :disabled="resolvingId === item.id"
                    @click.stop="handleResolve(item)"
                  >
                    {{ resolvingId === item.id ? '处理中...' : '标记已解决' }}
                  </button>
                </div>
              </div>

              <p v-if="item.excerpt" class="q-excerpt">{{ toPlainText(item.excerpt) }}</p>

              <div class="q-meta">
                <span class="course-tag">{{ item.courseName }}</span>
                <span class="meta-sep">·</span>

                <span class="meta-item" v-if="item.authorName">{{ item.authorName }}</span>
                <span class="meta-sep" v-if="item.authorName">·</span>

                <span class="meta-item">
                  <MessageOutlined class="meta-icon" />
                  {{ item.replyCount }} 回复
                </span>

                <span class="meta-sep">·</span>

                <span class="meta-item">
                  <EyeOutlined class="meta-icon" />
                  {{ item.viewCount }}
                </span>

                <span class="meta-time">{{ item.lastActiveTime }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-area" :class="{ 'is-placeholder': total <= pageSize }">
        <a-pagination
          v-if="total > pageSize"
          v-model:current="currentPage"
          :total="total"
          :pageSize="pageSize"
          @change="handlePageChange"
          show-less-items
        />
      </div>
    </div>

    <a-modal
      v-model:open="askVisible"
      title="我要提问"
      :width="960"
      :footer="null"
      :maskClosable="!askSubmitting"
      destroyOnClose
    >
      <div class="ask-form">
        <div class="form-item">
          <label class="form-label">标题</label>
          <input
            v-model="askForm.title"
            class="form-input"
            maxlength="100"
            placeholder="请输入问题标题"
          />
        </div>

        <div class="form-item">
          <label class="form-label">所属课程</label>
          <select v-model="askForm.courseId" class="form-select">
            <option value="">请选择课程</option>
            <option
              v-for="course in selectableCourses"
              :key="course.id"
              :value="course.id"
            >
              {{ course.name }}
            </option>
          </select>
        </div>

        <div class="form-item">
          <label class="form-label">内容</label>
          <div class="rich-editor-shell ask-rich-editor">
            <div class="rich-editor-surface">
              <Toolbar
                style="border-bottom: 1px solid #E2E8F0"
                :editor="askEditorRef"
                :defaultConfig="askToolbarConfig"
                mode="default"
              />
              <Editor
                style="height: 220px; overflow-y: hidden;"
                v-model="askForm.content"
                :defaultConfig="askEditorConfig"
                mode="default"
                @onCreated="handleAskEditorCreated"
              />
            </div>
          </div>
        </div>

        <div class="form-actions">
          <button
            class="btn-cancel"
            :disabled="askSubmitting"
            @click="askVisible = false"
          >
            取消
          </button>
          <button
            class="btn-submit"
            :disabled="askSubmitting"
            @click="submitAsk"
          >
            {{ askSubmitting ? '提交中...' : '提交问题' }}
          </button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
// @ts-ignore
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import { getLoginUserRaw } from '@/utils/authStorage'
import {
  EditOutlined,
  EyeOutlined,
  LeftOutlined,
  MessageOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'

import {
  HOMEWORK_STATUS_FILTERS,
  addHomeworkHelp,
  getHomeworkHelpList,
  resolveCommunityPost
} from '@/api/community'
import type { HomeworkQuestionItem } from '@/types/community'

type StatusFilter = 'all' | 'open' | 'resolved' | 'teacher'

const router = useRouter()

const statusFilters = HOMEWORK_STATUS_FILTERS
const courseFilters = ref<Array<{ id: string; name: string }>>([
  { id: 'all', name: '全部课程' }
])
const selectableCourses = computed(() => courseFilters.value.filter(item => item.id !== 'all'))

const questions = ref<HomeworkQuestionItem[]>([])
const loading = ref(false)
const error = ref(false)

const keyword = ref('')
const currentStatus = ref<StatusFilter>('all')
const currentCourseId = ref<string>('all')

const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)

const askVisible = ref(false)
const askSubmitting = ref(false)
const resolvingId = ref<number | string | null>(null)

const askForm = ref({
  title: '',
  content: '',
  courseId: ''
})

const askEditorRef = shallowRef()
const askToolbarConfig = {}
const askEditorConfig = { placeholder: '请描述你在作业、实验或项目中遇到的问题' }

const handleAskEditorCreated = (editor: any) => {
  askEditorRef.value = editor
}

const loginUser = computed(() => {
  try {
    const raw = getLoginUserRaw()
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
})

const isTeacher = computed(() => loginUser.value?.userRole === 'teacher')

async function fetchList() {
  loading.value = true
  error.value = false

  try {
    const data = await getHomeworkHelpList({
      page: currentPage.value,
      pageSize: pageSize.value,
      courseId: currentCourseId.value,
      status: currentStatus.value,
      keyword: keyword.value.trim()
    })

    questions.value = data.records || []
    total.value = data.total || 0
  } catch (err) {
    console.error('[HomeworkHelp] 获取列表失败', err)
    error.value = true
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchList()
}

function handleClearSearch() {
  keyword.value = ''
  currentPage.value = 1
  fetchList()
}

function handleStatusChange(status: StatusFilter) {
  currentStatus.value = status
  currentPage.value = 1
  fetchList()
}

function handleCourseFilter(courseId: string) {
  currentCourseId.value = courseId
  currentPage.value = 1
  fetchList()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchList()
}

function goToDetail(item: HomeworkQuestionItem) {
  router.push({
    name: 'CommunityDetail',
    params: { id: String(item.id) },
    query: { from: 'homework' }
  })
}

function toPlainText(content: string) {
  const raw = String(content || '')
  if (!raw) return ''

  if (typeof window === 'undefined') {
    return raw
      .replace(/<[^>]+>/g, ' ')
      .replace(/&nbsp;/gi, ' ')
      .replace(/\s+/g, ' ')
      .trim()
  }

  const div = document.createElement('div')
  div.innerHTML = raw
  return (div.textContent || div.innerText || '')
    .replace(/\u00A0/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function hasRichTextContent(content: string) {
  return !!toPlainText(content).trim()
}

function handleAsk() {
  askVisible.value = true
}

function resetAskForm() {
  askForm.value = {
    title: '',
    content: '',
    courseId: ''
  }
}

async function submitAsk() {
  const title = askForm.value.title.trim()
  const content = askForm.value.content
  const courseId = askForm.value.courseId

  if (!title) {
    message.warning('请输入标题')
    return
  }

  if (!courseId) {
    message.warning('请选择所属课程')
    return
  }

  if (!hasRichTextContent(content)) {
    message.warning('请输入问题内容')
    return
  }

  const selectedCourse = courseFilters.value.find(item => item.id === courseId)

  askSubmitting.value = true
  try {
    await addHomeworkHelp({
      title,
      content,
      courseId: Number(courseId),
      courseName: selectedCourse?.name || ''
    })

    message.success('提问发布成功')
    askVisible.value = false
    resetAskForm()
    currentPage.value = 1
    await fetchList()
  } catch (err: any) {
    console.error('[HomeworkHelp] 发布提问失败', err)
    message.error(err?.message || '提问失败，请稍后重试')
  } finally {
    askSubmitting.value = false
  }
}

async function handleResolve(item: HomeworkQuestionItem) {
  resolvingId.value = item.id
  try {
    await resolveCommunityPost(item.id)
    message.success('已标记为已解决')
    await fetchList()
  } catch (err: any) {
    console.error('[HomeworkHelp] 标记已解决失败', err)
    message.error(err?.message || '操作失败')
  } finally {
    resolvingId.value = null
  }
}

watch(askVisible, (visible) => {
  if (!visible) {
    resetAskForm()
  }
})

async function loadMyClassCourses() {
  try {
    const courses = await request.get<any[], any[]>('/course/list/my-class', {
      skipErrorToast: true
    })
    courseFilters.value = [
      { id: 'all', name: '全部课程' },
      ...(courses || []).map(course => ({
        id: String(course.id),
        name: String(course.name || '未命名课程')
      }))
    ]
  } catch (error) {
    console.error('[HomeworkHelp] 获取本班课程失败', error)
    message.warning('本班课程加载失败，暂时无法发布提问')
  }
}

onMounted(async () => {
  await Promise.all([loadMyClassCourses(), fetchList()])
})

onBeforeUnmount(() => {
  askEditorRef.value?.destroy?.()
})
</script>
<style scoped>


/* ================= 状态展示 (加载/空/错误) ================= */
.state-box {
  flex: 1; /* 填满父容器 list-card 的剩余空间 */
  width: 100%;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #FFFFFF;
}

.state-icon-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #F1F5F9;
  color: #94A3B8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-bottom: 4px;
}

/* 针对空状态给予主题色弱暗示 */
.state-empty-circle {
  background: #EFF6FF;
  color: #3B82F6;
}

.state-error-circle {
  background: #FEF2F2;
  color: #EF4444;
}

.state-text {
  font-size: 15px;
  color: #64748B;
  margin: 0;
  font-weight: 500;
}

.btn-retry {
  margin-top: 8px;
  height: 36px;
  padding: 0 20px;
  border: none;
  border-radius: 6px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  color: #0F172A;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
}

.btn-retry:hover {
  background: #F8FAFC;
  border-color: #CBD5E1;
}

/* ================= 提问弹窗 (Modal) ================= */
.ask-form { display: flex; flex-direction: column; gap: 16px; }
.form-label { font-size: 14px; font-weight: 500; color: #334155; margin-bottom: 6px; display: block;}
.form-input, .form-select { width: 100%; padding: 10px 12px; border: 1px solid #E2E8F0; border-radius: 6px; outline: none; font-size: 14px; }
.form-input:focus { border-color: #2563EB; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
.rich-editor-shell { border: 1px solid #E2E8F0; border-radius: 8px; overflow: hidden; background: #FFFFFF; }
.rich-editor-surface { background: #FFFFFF; }
.ask-rich-editor :deep(.w-e-bar) { background: #F8FAFC; }
.ask-rich-editor :deep(.w-e-text-container) { background: #FFFFFF; }
.ask-rich-editor :deep(.w-e-text-placeholder) { top: 14px; }
.ask-rich-editor :deep(.w-e-text-container [data-slate-editor]) { padding: 0 14px; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; }
.btn-submit { background: #0F172A; color: #FFF; border: none; padding: 8px 16px; border-radius: 6px; font-weight: 500; cursor: pointer; }
.btn-cancel { background: #FFFFFF; color: #475569; border: 1px solid #E2E8F0; padding: 8px 16px; border-radius: 6px; cursor: pointer; }

/* ================= 分页器 ================= */
.pagination-area { display: flex; justify-content: center; padding-top: 32px; }

.hw-page {
  min-height: 100vh;
  padding-bottom: 80px;
  background: #F8FAFC;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
}

.page-container {
  width: 75%;
  min-width: 1000px;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-top { padding: 24px 0 16px; }
.btn-back-link {
  display: inline-flex; align-items: center; gap: 8px;
  background: transparent; border: none; padding: 6px 12px; margin-left: -12px;
  border-radius: 6px; font-size: 15px; font-weight: 500; color: #64748B;
  cursor: pointer; transition: all 0.2s;
}
.btn-back-link:hover { color: #0F172A; background: #F1F5F9; }

.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24px 32px; margin-bottom: 24px; border-radius: 8px;
  background: #FFFFFF; border: 1px solid #E2E8F0; box-shadow: 0 1px 2px rgba(0,0,0,0.02);
}
.page-title { font-size: 24px; font-weight: 700; color: #0F172A; margin: 0 0 4px; }
.page-desc { font-size: 14px; color: #64748B; margin: 0; }

.header-actions { display: flex; align-items: center; gap: 16px; }
.search-box {
  display: flex; align-items: center; gap: 8px; width: 280px; height: 40px;
  padding: 0 12px; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 6px; transition: 0.2s;
}
.search-box:focus-within { border-color: #2563EB; box-shadow: 0 0 0 2px rgba(37,99,235,0.1); }
.search-icon { color: #94A3B8; font-size: 14px; }
.search-box input { flex: 1; border: none; outline: none; font-size: 14px; color: #0F172A; }

.btn-ask {
  height: 40px; padding: 0 16px; background: #0F172A; color: #FFFFFF; border: none;
  border-radius: 6px; font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 8px; cursor: pointer;
}
.btn-ask:hover { background: #334155; }

.filter-bar {
  display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px;
  padding: 20px; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 8px;
}
.status-tabs, .course-filters { display: flex; flex-wrap: wrap; gap: 12px; }
.status-tab, .course-pill {
  height: 30px; padding: 0 14px; border: 1px solid transparent; background: #F1F5F9;
  color: #475569; font-size: 13px; font-weight: 500; border-radius: 15px; cursor: pointer; transition: 0.2s;
}
.status-tab:hover, .course-pill:hover { background: #E2E8F0; }
.status-tab.active, .course-pill.active { background: #0F172A; color: #FFFFFF; }

.list-card {
  background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05); height: 600px; display: flex; flex-direction: column; overflow: hidden;
}
.question-list { flex: 1; overflow-y: auto; }
.question-list::-webkit-scrollbar { width: 4px; }
.question-list::-webkit-scrollbar-track { background: transparent; }
.question-list::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 4px; }

.question-item { display: flex; padding: 24px; cursor: pointer; border-bottom: 1px solid #F1F5F9; position: relative; transition: 0.2s; }
.question-item:last-child { border-bottom: none; }
.question-item:hover { background: #F8FAFC; }

.q-status-bar { position: absolute; left: 0; top: 24px; bottom: 24px; width: 3px; border-radius: 0 2px 2px 0; }
.q-status-bar.open { background: #3B82F6; }
.q-status-bar.resolved { background: #10B981; }

.q-body { flex: 1; min-width: 0; padding-left: 8px; }
.q-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 8px; }
.q-title { font-size: 16px; font-weight: 600; color: #0F172A; margin: 0; }

.q-excerpt { font-size: 14px; color: #64748B; line-height: 1.5; margin-bottom: 12px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.q-meta { display: flex; align-items: center; gap: 12px; font-size: 13px; color: #94A3B8; }
.course-tag { height: 22px; padding: 0 8px; background: #F1F5F9; color: #475569; border-radius: 4px; display: flex; align-items: center; font-weight: 500; }
.meta-item { display: flex; align-items: center; gap: 4px; }
.meta-time { margin-left: auto; }
.meta-sep { color: #E2E8F0; }

.q-badges { display: flex; gap: 8px; }
.badge { height: 22px; padding: 0 8px; font-size: 12px; font-weight: 500; border-radius: 4px; display: inline-flex; align-items: center; }
.badge-open { background: #EFF6FF; color: #2563EB; }
.badge-resolved { background: #DCFCE7; color: #15803D; }
.badge-teacher { background: #ECFCCB; color: #4D7C0F; }

.mini-action-btn { background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 4px; padding: 0 8px; font-size: 12px; color: #475569; cursor: pointer; }
.mini-action-btn:hover { background: #F1F5F9; }

/* Modal 表单去 AI 味 */
.ask-form { display: flex; flex-direction: column; gap: 16px; }
.form-label { font-size: 14px; font-weight: 500; color: #334155; margin-bottom: 6px; display: block;}
.form-input, .form-select { width: 100%; padding: 10px 12px; border: 1px solid #E2E8F0; border-radius: 6px; outline: none; font-size: 14px; }
.form-input:focus { border-color: #2563EB; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); }
.rich-editor-shell { border: 1px solid #E2E8F0; border-radius: 8px; overflow: hidden; background: #FFFFFF; }
.rich-editor-surface { background: #FFFFFF; }
.ask-rich-editor :deep(.w-e-bar) { background: #F8FAFC; }
.ask-rich-editor :deep(.w-e-text-container) { background: #FFFFFF; }
.ask-rich-editor :deep(.w-e-text-placeholder) { top: 14px; }
.ask-rich-editor :deep(.w-e-text-container [data-slate-editor]) { padding: 0 14px; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; }
.btn-submit { background: #0F172A; color: #FFF; border: none; padding: 8px 16px; border-radius: 6px; font-weight: 500; cursor: pointer; }
.btn-cancel { background: #FFFFFF; color: #475569; border: 1px solid #E2E8F0; padding: 8px 16px; border-radius: 6px; cursor: pointer; }

.pagination-area { display: flex; justify-content: center; padding-top: 32px; }

/* Unified student community layout */
.hw-page {
  min-height: calc(100vh - 70px);
  padding-bottom: 40px;
  background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%);
  color: #0F172A;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.page-container {
  width: 75%;
  min-width: 1200px;
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

.page-top { padding: 0 0 12px; }

.btn-back-link {
  height: 36px;
  padding: 0 14px 0 12px;
  margin-left: 0;
  border: 1px solid #DDE7F2;
  border-radius: 6px;
  background: #FFFFFF;
  color: #475569;
  font-size: 14px;
  font-weight: 750;
  transition: border-color 0.2s, background 0.2s, color 0.2s;
  box-shadow: none;
}

.btn-back-link :deep(svg) {
  font-size: 13px;
}

.btn-back-link:hover {
  color: #2563EB;
  border-color: #BFDBFE;
  background: #EFF6FF;
}

.page-header {
  padding: 16px 20px;
  margin-bottom: 16px;
  border-radius: 5px;
  background: #FFFFFF;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.page-title {
  font-size: 22px;
  line-height: 1.25;
  font-weight: 800;
}

.page-desc {
  margin-top: 5px;
  font-size: 14px;
  line-height: 1.5;
  color: #64748B;
}

.header-actions { gap: 12px; }

.search-box {
  width: 260px;
  height: 36px;
}

.btn-ask {
  height: 36px;
  border-radius: 6px;
  background: #2563EB;
  font-size: 13px;
  font-weight: 700;
}

.btn-ask:hover { background: #1D4ED8; }

.filter-bar {
  margin-bottom: 0;
  padding: 14px 18px;
  border-radius: 5px 5px 0 0;
  background: #FFFFFF;
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-bottom: 1px solid #E7ECF3;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  gap: 10px;
}

.status-tabs,
.course-filters {
  gap: 8px;
}

.status-tab,
.course-pill {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #E7ECF3;
  border-radius: 6px;
  background: #FFFFFF;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.status-tab:hover,
.course-pill:hover {
  border-color: #BFDBFE;
  background: #EFF6FF;
  color: #2563EB;
}

.status-tab.active,
.course-pill.active {
  background: #2563EB;
  border-color: #2563EB;
  color: #FFFFFF;
}

.list-card {
  height: 560px;
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-top: none;
  border-radius: 0 0 5px 5px;
  background: #FFFFFF;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.question-list {
  overflow: hidden;
}

.question-item {
  min-height: 112px;
  padding: 18px 28px;
  border-bottom: 1px solid #EDF1F6;
  transition: background 0.18s ease;
}

.question-item:hover { background: #F8FBFF; }

.q-status-bar { display: none; }

.q-body {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding-left: 0;
}

.q-title {
  font-size: 16px;
  line-height: 1.5;
  font-weight: 800;
}

.q-excerpt {
  margin: 6px 0 10px;
  color: #475569;
}

.q-meta {
  color: #64748B;
}

.pagination-area {
  min-height: 64px;
  padding: 16px 0 8px;
  align-items: center;
}

.pagination-area.is-placeholder {
  visibility: hidden;
}

/* Ask modal final layout override */
.ask-form {
  display: grid !important;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 16px 18px;
}

.ask-form .form-item:nth-child(3),
.ask-form .form-actions {
  grid-column: 1 / -1;
}

.ask-form .form-label {
  margin-bottom: 8px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.ask-form .form-input,
.ask-form .form-select {
  height: 40px;
  box-sizing: border-box;
}

.ask-rich-editor {
  border-radius: 6px;
}

.ask-rich-editor :deep(.w-e-bar) {
  min-height: 44px;
}

.ask-rich-editor :deep(.w-e-text-container) {
  height: 220px !important;
}

.form-actions {
  margin-top: 0 !important;
  padding-top: 2px;
}

.btn-cancel,
.btn-submit {
  height: 36px;
  min-width: 92px;
  padding: 0 16px;
  font-weight: 700;
}
</style>
