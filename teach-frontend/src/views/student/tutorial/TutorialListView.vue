<template>
  <div class="edu-app">
    <main class="edu-main">
      <div class="page-shell">
        <section class="page-hero">
          <div class="hero-copy">
            <span class="hero-tag"><read-outlined /> 资料库</span>
            <h1 class="hero-title">查找课程、教程与知识点</h1>
            <p class="hero-desc">通过搜索和分类筛选快速进入内容阅读页。</p>
          </div>

          <div class="search-container">
            <div class="search-box">
              <search-outlined class="search-icon" />
              <input
                v-model="searchText"
                type="text"
                placeholder="搜索教程、文档或知识点"
                @keyup.enter="onSearch"
              />
              <button class="search-btn" @click="onSearch">站内搜索</button>
            </div>

            <div class="external-search">
              <span class="label">站外搜索：</span>
              <button class="engine-btn bilibili" @click="searchExternal('bilibili')">Bilibili</button>
              <button class="engine-btn csdn" @click="searchExternal('csdn')">CSDN</button>
              <button class="engine-btn baidu" @click="searchExternal('baidu')">百度</button>
            </div>
          </div>
        </section>

        <section class="toolbar-card">
          <div class="course-filter">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              class="filter-chip"
              :class="{ active: currentTab === tab.key }"
              @click="handleTabChange(tab.key)"
            >
              {{ tab.name }}
            </button>
          </div>

          <div class="sort-switch">
            <button class="sort-btn" :class="{ active: sortType === 'latest' }" @click="handleSortChange('latest')">
              最新
            </button>
            <button class="sort-btn" :class="{ active: sortType === 'hot' }" @click="handleSortChange('hot')">
              热门
            </button>
          </div>
        </section>

        <section class="content-section">
          <div v-if="loading" class="state-box card-box">
            <a-spin size="large" />
            <p class="state-text">正在加载资料列表...</p>
          </div>

          <div v-else-if="list.length === 0" class="state-box card-box">
            <div class="state-icon-circle state-empty-circle">📚</div>
            <p class="state-text">没有找到符合条件的搜索结果</p>
            <button class="btn-retry" @click="resetSearch">重置搜索</button>
          </div>

          <template v-else>
            <div class="tutorial-grid">
              <article
                v-for="item in list"
                :key="item.id"
                class="tutorial-card"
                @click="goToRead(item.id)"
              >
                <div class="card-cover">
                  <img
                    v-if="getTutorialCover(item)"
                    :src="getTutorialCover(item)"
                    :alt="item.name"
                    loading="lazy"
                    @error="markTutorialCoverFailed(item.id)"
                  />
                  <div v-else class="cover-placeholder">
                    <span>图文教程</span>
                    <strong>{{ getCoverPlaceholderTitle(item) }}</strong>
                  </div>
                </div>
                <div class="card-body">
                  <div class="card-topline">
                    <span class="resource-type-pill">图文教程</span>
                    <span class="source-pill">图文课程</span>
                  </div>

                  <div class="card-main">
                    <h3 class="card-title">{{ item.name }}</h3>
                    <p class="card-desc">{{ item.description || '暂无简介，进入阅读页查看完整内容。' }}</p>
                  </div>

                  <div class="card-footer">
                    <span class="knowledge-chip">{{ item.name }}</span>
                    <span class="card-action">阅读教程</span>
                  </div>
                </div>
              </article>
            </div>

            <div class="pagination-bar">
              <a-pagination
                :current="pageCurrent"
                :page-size="pageSize"
                :total="total"
                :show-size-changer="false"
                show-less-items
                @change="handlePageChange"
              />
            </div>
          </template>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReadOutlined, FileTextOutlined } from '@ant-design/icons-vue'

type TutorialItem = {
  id: number
  name: string
  coverImg?: string
  description?: string
  createTime?: string
}

type PageResult<T> = {
  records: T[]
  total: number
  current: number
  size: number
}

type SortType = 'latest' | 'hot'

const router = useRouter()

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const SERVER_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const list = ref<TutorialItem[]>([])
const loading = ref(true)
const searchText = ref('')
const currentTab = ref('all')
const sortType = ref<SortType>('latest')
const pageCurrent = ref(1)
const pageSize = ref(8)
const total = ref(0)
const failedTutorialCoverIds = ref<Set<number>>(new Set())

const tabs = [
  { name: '全部', key: 'all' },
  { name: '后端开发', key: 'backend' },
  { name: '前端技术', key: 'frontend' },
  { name: '人工智能', key: 'ai' },
  { name: '计算机基础', key: 'cs' },
]

const fetchList = async () => {
  loading.value = true
  try {
    const keyword = searchText.value.trim()
    const data = await request.get<PageResult<TutorialItem>, PageResult<TutorialItem>>('/tutorial/page', {
      params: {
        current: pageCurrent.value,
        size: pageSize.value,
        name: keyword || undefined,
        category: currentTab.value === 'all' ? undefined : currentTab.value,
        sort: sortType.value,
      },
    })
    list.value = data?.records || []
    total.value = data?.total || 0
    pageCurrent.value = Number(data?.current || pageCurrent.value)
  } catch (e) {
    message.error('加载教程列表失败')
  } finally {
    loading.value = false
  }
}

const getTutorialCover = (item: TutorialItem) => {
  if (failedTutorialCoverIds.value.has(item.id)) return ''
  const rawCover = item?.coverImg
  if (!rawCover) return ''
  if (rawCover.startsWith('http') || rawCover.startsWith('data:image')) return rawCover
  if (rawCover.startsWith('/course-covers/') || rawCover.startsWith('/icons/')) return rawCover
  return `${SERVER_BASE_URL}${rawCover.startsWith('/') ? rawCover : `/${rawCover}`}`
}

const markTutorialCoverFailed = (id: number) => {
  const failedIds = new Set(failedTutorialCoverIds.value)
  failedIds.add(id)
  failedTutorialCoverIds.value = failedIds
}

const getCoverPlaceholderTitle = (item: TutorialItem) => {
  const name = String(item.name || '').trim()
  if (name.includes('数据结构')) return '数据结构专题'
  return name || '课程封面待补充'
}

const goToRead = (courseId: number) => {
  router.push(`/student/tutorial/${courseId}/read`)
}

const onSearch = () => {
  pageCurrent.value = 1
  fetchList()
}

const resetSearch = () => {
  searchText.value = ''
  currentTab.value = 'all'
  sortType.value = 'latest'
  pageCurrent.value = 1
  fetchList()
}

const handleTabChange = (key: string) => {
  currentTab.value = key
  pageCurrent.value = 1
  fetchList()
}

const handleSortChange = (type: SortType) => {
  sortType.value = type
  pageCurrent.value = 1
  fetchList()
}

const handlePageChange = (page: number) => {
  pageCurrent.value = page
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const searchExternal = (engine: string) => {
  const query = searchText.value.trim()

  if (!query) {
    message.warning('请输入需要搜索的关键词')
    return
  }

  const encodedQuery = encodeURIComponent(query)
  let targetUrl = ''

  switch (engine) {
    case 'bilibili':
      targetUrl = `https://search.bilibili.com/all?keyword=${encodedQuery}`
      break
    case 'csdn':
      targetUrl = `https://so.csdn.net/so/search?q=${encodedQuery}`
      break
    case 'baidu':
      targetUrl = `https://www.baidu.com/s?wd=${encodedQuery}`
      break
    default:
      return
  }

  window.open(targetUrl, '_blank', 'noopener,noreferrer')
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.edu-app {
  min-height: 100vh;
  background: #f6f8fc;
  color: #344054;
}

.edu-main {
  width: min(75%, 1380px);
  margin: 0 auto;
  padding: 20px 24px 80px;
}

.page-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 30px;
  border: 1px solid #eaf0f7;
  border-radius: 5px;
  background: linear-gradient(135deg, #ffffff 0%, #f7faff 100%);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  margin-bottom: 12px;
  padding: 0 12px;
  border-radius: 5px;
  background: #eef4ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.hero-title {
  margin: 0 0 10px;
  color: #1f2937;
  font-size: 30px;
  font-weight: 800;
  line-height: 1.2;
}

.hero-desc {
  margin: 0;
  color: #667085;
  font-size: 14px;
  line-height: 1.9;
}

.search-container {
  width: 400px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-box {
  display: flex;
  align-items: center;
  height: 42px;
  padding: 0 4px 0 14px;
  border: 1px solid #e7ecf3;
  border-radius: 5px;
  background: #ffffff;
}

.search-icon {
  color: #98a2b3;
  font-size: 14px;
}

.search-box input {
  flex: 1;
  min-width: 0;
  padding: 0 10px;
  border: 0;
  outline: none;
  color: #1f2937;
  font-size: 13px;
}

.search-box input::placeholder {
  color: #667085;
}

.search-btn {
  height: 34px;
  padding: 0 16px;
  border: 0;
  border-radius: 5px;
  background: #2563eb;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.external-search {
  display: flex;
  align-items: center;
  gap: 8px;
}

.external-search .label {
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.engine-btn {
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e7ecf3;
  border-radius: 5px;
  background: #ffffff;
  color: #344054;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.engine-btn:hover {
  border-color: #bfdbfe;
  color: #2563eb;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
}

.toolbar-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid #e7ecf3;
  border-radius: 5px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.course-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip {
  height: 34px;
  padding: 0 12px;
  border: 1px solid #e7ecf3;
  border-radius: 5px;
  background: #ffffff;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.filter-chip.active {
  border-color: #2563eb;
  background: #2563eb;
  color: #ffffff;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.14);
}

.sort-switch {
  display: inline-flex;
  flex-shrink: 0;
  gap: 4px;
  padding: 4px;
  border: 1px solid #e7ecf3;
  border-radius: 5px;
  background: #f8fafc;
}

.sort-btn {
  min-width: 58px;
  height: 30px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.sort-btn.active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.06);
}

.tutorial-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  align-items: stretch;
}

.tutorial-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.03);
  border-radius: 5px;
  background: #ffffff;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.tutorial-card:hover {
  transform: translateY(-2px);
  border-color: #93c5fd;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
}

.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #f3f4f6;
}

.card-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.tutorial-card:hover .card-cover img {
  transform: scale(1.04);
}

.cover-placeholder {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  background: linear-gradient(135deg, #f8fafc 0%, #eef4ff 100%);
  color: #64748b;
}

.cover-placeholder span {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: 6px;
  background: #ffffff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 800;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.cover-placeholder strong {
  color: #334155;
  font-size: 16px;
  font-weight: 800;
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.card-topline,
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 18px;
}

.card-topline {
  padding-top: 14px;
}

.card-main {
  flex: 1;
  min-height: 0;
  padding: 12px 18px 14px;
}

.card-title {
  margin: 0 0 10px;
  color: #111827;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-desc {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  margin-top: auto;
  padding-bottom: 16px;
}

.resource-type-pill,
.source-pill,
.knowledge-chip {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  height: 26px;
  padding: 0 10px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-type-pill {
  border-color: #dbeafe;
  background: #eff6ff;
  color: #1d4ed8;
}

.source-pill {
  border-color: #d1fae5;
  background: #ecfdf5;
  color: #047857;
}

.knowledge-chip {
  max-width: calc(100% - 86px);
  color: #475569;
  background: #f8fafc;
}

.card-action {
  flex-shrink: 0;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  padding: 24px 0 4px;
}

.card-box {
  border: 1px solid #e7ecf3;
  border-radius: 5px;
  background: #ffffff;
}

.state-box {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.state-icon-circle {
  width: 52px;
  height: 52px;
  border-radius: 5px;
  background: #eef4ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.state-text {
  color: #667085;
  font-size: 14px;
  font-weight: 500;
}

.btn-retry {
  height: 38px;
  padding: 0 20px;
  border: 0;
  border-radius: 5px;
  background: #2563eb;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 1180px) {
  .edu-main {
    width: 92%;
  }

  .tutorial-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .page-hero,
  .toolbar-card {
    align-items: stretch;
    flex-direction: column;
  }

  .search-container {
    width: 100%;
  }

  .tutorial-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .edu-main {
    width: 100%;
    padding: 14px 12px 64px;
  }

  .page-hero {
    padding: 22px 18px;
  }

  .hero-title {
    font-size: 24px;
  }

  .tutorial-grid {
    grid-template-columns: 1fr;
  }

  .card-topline,
  .card-footer {
    padding-left: 14px;
    padding-right: 14px;
  }

  .card-main {
    padding: 12px 14px 14px;
  }
}
</style>
