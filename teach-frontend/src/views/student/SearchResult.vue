<template>
  <div class="search-page">
    <div class="container-1200">

      <section class="result-toolbar">
        <div class="result-info">
          <span class="result-count">共找到 {{ total }} 门相关课程</span>
          <span class="result-tip" v-if="isCategoryMode">
            当前分类：{{ typeName }}
          </span>
          <span class="result-tip" v-else-if="keyword">
            当前关键词：{{ keyword }}
          </span>
        </div>

      </section>

      <a-spin :spinning="loading">
        <div v-if="pagedCourseList.length > 0" class="course-grid">
          <div
            v-for="course in pagedCourseList"
            :key="course.id"
            class="new-course-card"
            @click="goToLearn(course.id)"
          >
            <div class="ncc-cover">
              <img v-if="getCover(course)" :src="getCover(course)" :alt="course.name" loading="lazy" />
              <div v-else class="cover-placeholder">
                <span>{{ isTextCourse(course) ? '图文教程' : '课程资源' }}</span>
                <strong>待上传封面</strong>
              </div>
              <div class="ncc-overlay">{{ isTextCourse(course) ? '阅读教程' : '开始学习' }}</div>
            </div>
            <div class="ncc-bottom">
              <h3 class="ncc-title" :title="course.name">{{ course.name }}</h3>
              <div class="ncc-desc">
                {{ course.description || '探索核心知识与实践技巧，系统提升专业能力。' }}
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="!loading" class="empty-state card">
          <div class="empty-icon">🔎</div>
          <div class="empty-title">暂无相关课程</div>
          <div class="empty-desc">
            换个关键词试试，比如“前端”、“算法”、“人工智能”
          </div>
        </div>

        <div class="pagination-area" v-if="total > 0">
          <a-pagination
            v-model:current="currentPage"
            :total="total"
            :pageSize="pageSize"
            show-less-items
            @change="handlePageChange"
          />
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { getRecommendedCourses } from '@/api/dashboard'
import type { Course } from '@/api/dashboard'

const route = useRoute()
const router = useRouter()

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const SERVER_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const keyword = ref('')
const typeName = ref('')
const categoryId = ref<number | undefined>()
const searchText = ref('')
const loading = ref(false)
const courseList = ref<Course[]>([])
const currentPage = ref(1)
const pageSize = ref(8)

const isAdminCourse = (course: any) => {
  return String(course?.creatorRole || '').toLowerCase() === 'admin'
}

const isTextCourse = (course: Course) => {
  const rawType = String((course as Course & { type?: string; resourceType?: string }).type || (course as Course & { type?: string; resourceType?: string }).resourceType || '').toLowerCase()
  const title = String(course.name || '').toLowerCase()
  return rawType.includes('text') ||
    rawType.includes('tutorial') ||
    title.includes('图文教程') ||
    title.includes('图文课程')
}

const total = computed(() => courseList.value.length)
const pagedCourseList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return courseList.value.slice(start, end)
})

const isCategoryMode = computed(() => !!typeName.value)

const effectiveKeyword = computed(() => {
  return (keyword.value || typeName.value || '').trim()
})

const getCover = (course: any) => {
  if (course && course.coverImg) {
    if (course.coverImg.startsWith('http') || course.coverImg.startsWith('data:image')) {
      return course.coverImg
    }
    return `${SERVER_BASE_URL}${course.coverImg.startsWith('/') ? course.coverImg : `/${course.coverImg}`}`
  }
  return ''
}

const syncRouteState = () => {
  keyword.value = String(route.query.keyword || '').trim()
  typeName.value = String(route.query.typeName || '').trim()
  const rawCategoryId = Number(route.query.categoryId)
  categoryId.value = Number.isFinite(rawCategoryId) && rawCategoryId > 0 ? rawCategoryId : undefined
  searchText.value = effectiveKeyword.value
}

const normalizeCourseRecords = (res: any) => {
  let pageData = res
  if (res && res.code === 0 && res.data) {
    pageData = res.data
  }

  const records = pageData.records
    ? pageData.records
    : Array.isArray(pageData)
      ? pageData
      : []

  return records.filter(isAdminCourse)
}

const fetchCourses = async () => {
  loading.value = true
  try {
    const res: any = await getRecommendedCourses(
      1,
      500,
      categoryId.value ? keyword.value : effectiveKeyword.value,
      categoryId.value
    )

    let records = normalizeCourseRecords(res)

    if (categoryId.value && typeName.value) {
      const fallbackRes: any = await getRecommendedCourses(1, 500, typeName.value)
      const fallbackRecords = normalizeCourseRecords(fallbackRes)
      const mergedMap = new Map<number | string, Course>()
      ;[...records, ...fallbackRecords].forEach((course: Course) => {
        mergedMap.set(course.id, course)
      })
      records = Array.from(mergedMap.values())
    }

    courseList.value = records
  } catch (error) {
    console.error('加载课程失败', error)
    message.error('加载课程失败，请稍后重试')
    courseList.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  const value = searchText.value.trim()

  if (!value) {
    message.warning('请输入要搜索的课程关键词')
    return
  }

  currentPage.value = 1
  router.push({
    path: '/student/search',
    query: { keyword: value }
  })
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}

const goToLearn = (id: number | string) => {
  router.push(`/learn/${id}`)
}

watch(
  () => route.query,
  () => {
    syncRouteState()
    currentPage.value = 1
    fetchCourses()
  },
  { immediate: true, deep: true }
)
</script>

<style scoped>
/* 整体下移，并设置背景 */
.search-page {
  height: 100vh; /* 👈 核心修改 1：把 min-height 改成绝对的 height，锁死一屏高度 */
  overflow: hidden; /* 👈 核心修改 2：隐藏溢出部分，彻底消灭右侧滚动条 */
  box-sizing: border-box; /* 👈 核心修改 3：让上下 100px 的 padding 包含在这 100vh 内部，不往外撑 */
  padding: 60px 0 40px;
  background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
}

.container-1200 {
  width: 75%;
  max-width: 1600px;
  min-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  box-sizing: border-box;
}

.card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
}

/* 居中搜索区域 */
.centered-search-section {
  display: flex;
  justify-content: center;
  padding: 30px 0 40px;
}

.hero-search-box {
  width: 580px; /* 稍微加宽，单独存在时更大气 */
  max-width: 100%;
  height: 56px;
  padding: 0 10px 0 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
  transition: all 0.3s ease;
}

.hero-search-box:focus-within {
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
  transform: translateY(-2px);
}

.search-icon {
  color: #94a3b8;
  font-size: 18px;
}

.hero-search-box input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: #111827;
  font-size: 15px;
}

.btn-search {
  height: 42px;
  padding: 0 24px;
  border: none;
  outline: none;
  cursor: pointer;
  border-radius: 999px;
  background: linear-gradient(135deg, #4f46e5 0%, #2563eb 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.btn-search:hover {
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.result-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.result-info {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.result-count {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.result-tip {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: #eef4ff;
  color: #315efb;
  font-size: 13px;
  font-weight: 500;
}

.btn-back {
  height: 38px;
  padding: 0 16px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

/* ================= 课程卡片 (Dashboard 同款) ================= */
.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: 280px; /* 👈 核心：强制规定每一行的高度为 280px */
  gap: 24px;
  min-height: 584px; /* 👈 核心：280px * 2行 + 24px间距 = 584px。死死锁住两行的高度 */
  align-content: start; /* 👈 数据不够 8 个时，依然从最上面开始排列 */
}

.new-course-card {
  background: #FFFFFF;
  border-radius: 8px; /* 稍微加大一点圆角 */
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  height: 100%; /* 让卡片自动填满 280px 的行高 */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  transform: translateZ(0);
}

.ncc-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  position: relative;
  overflow: hidden;
  background: #F3F4F6;
}

.ncc-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.5s ease;
}

.cover-placeholder {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, #F8FAFC 0%, #EEF4FF 100%);
  color: #64748B;
}

.cover-placeholder span {
  display: inline-flex;
  height: 26px;
  align-items: center;
  padding: 0 10px;
  border-radius: 6px;
  background: #FFFFFF;
  color: #2563EB;
  font-size: 12px;
  font-weight: 800;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.cover-placeholder strong {
  color: #334155;
  font-size: 16px;
  font-weight: 800;
}

.new-course-card:hover .ncc-cover img {
  transform: scale(1.04);
}

.ncc-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #FFF;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 1px;
  opacity: 0;
  transition: opacity 0.3s;
  backdrop-filter: blur(2px);
}

.new-course-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
}

.new-course-card:hover .ncc-overlay { opacity: 1; }

.ncc-bottom {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  flex: 1;
  background: #FFFFFF;
}

.ncc-title {
  font-size: 16px;
  font-weight: 700;
  color: #1F2937;
  margin: 0 0 8px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ncc-desc {
  font-size: 13px;
  color: #6B7280;
  margin-bottom: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
}

/* ================= 缺省态与其他 ================= */
.empty-state {
  min-height: 584px; /* 👈 核心：让空状态的高度和 8 个课程的网格高度完全一致 */
  display: flex;
  flex-direction: column;
  justify-content: center; /* 内容垂直居中 */
  align-items: center;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.empty-desc {
  margin-top: 8px;
  font-size: 14px;
  color: #64748b;
}

.pagination-area {
  display: flex;
  justify-content: center;
  margin-top: 36px;
  /* 因为上面的网格/空状态都被锁死了 584px 的高度，这个分页器现在永远都会固定在屏幕的同一个位置！ */
}
/* 响应式适配 */
@media (max-width: 1100px) {
  .course-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .course-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .result-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 560px) {
  .container-1200 {
    width: min(1200px, calc(100% - 20px));
  }
  .course-grid {
    grid-template-columns: 1fr;
  }
  .centered-search-section {
    padding: 20px 0;
  }
}
</style>
