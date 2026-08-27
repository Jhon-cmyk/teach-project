<template>
  <div class="community-page">
    <div class="page-container">
      <section class="page-hero">
        <div class="hero-copy">
          <span class="hero-tag">学习交流</span>
          <h1 class="hero-title">社区讨论区</h1>
          <p class="hero-desc">
            和同学一起交流学习问题、查看老师答疑、沉淀高质量讨论内容。
          </p>
        </div>

        <div class="hero-actions">
          <button class="hero-action-btn" @click="router.push('/student/community/mine')">
            <UserOutlined />
            我的动态
            <span v-if="unreadCount > 0" class="notification-dot">
              {{ unreadCount > 99 ? '99+' : unreadCount }}
            </span>
          </button>
          <button class="hero-action-btn" @click="router.push('/student/community/homework-help')">
            <QuestionCircleOutlined />
            作业互助
          </button>
        </div>

      </section>

      <section class="discussion-shell">
        <div class="toolbar-card">
        <div class="toolbar-left">
          <div class="course-filter">
            <button
              v-for="item in COURSE_FILTERS"
              :key="item.id"
              class="filter-chip"
              :class="{ active: courseId === item.id }"
              @click="handleCourseChange(item.id)"
            >
              {{ item.name }}
            </button>
          </div>
        </div>

        <div class="toolbar-right">
          <div class="sort-switch">
            <button
              class="sort-btn"
              :class="{ active: sort === 'latest' }"
              @click="handleSortChange('latest')"
            >
              最新
            </button>
            <button
              class="sort-btn"
              :class="{ active: sort === 'hot' }"
              @click="handleSortChange('hot')"
            >
              热门
            </button>
          </div>

          <div class="search-box">
            <SearchOutlined class="search-icon" />
            <input
              v-model="keyword"
              placeholder="搜索讨论话题..."
              @keyup.enter="handleSearch"
            />
          </div>
        </div>
        </div>

        <div class="list-card">
        <div v-if="loading" class="state-box">
          <a-spin />
          <p class="state-text">正在加载讨论列表...</p>
        </div>

        <div v-else-if="error" class="state-box">
          <div class="state-icon-circle state-error-circle">!</div>
          <p class="state-text">加载失败，请稍后重试</p>
          <button class="btn-retry" @click="loadList">重新加载</button>
        </div>

        <div v-else-if="list.length === 0" class="state-box">
          <div class="state-icon-circle state-empty-circle">
            <MessageOutlined />
          </div>
          <p class="state-text">当前没有符合条件的讨论内容</p>
        </div>

        <div v-else class="discussion-list">
          <div
            v-for="item in list"
            :key="item.id"
            class="discussion-item"
            role="button"
            tabindex="0"
            @click="goDetail(item)"
            @keydown.enter.prevent="goDetail(item)"
            @keydown.space.prevent="goDetail(item)"
          >
            <div class="discussion-main">
              <div class="discussion-head">
                <div class="discussion-title-wrap">
                  <h3 class="discussion-title">
                    {{ item.title }}
                  </h3>
                  <div class="discussion-tags">
                    <span class="course-tag">{{ item.courseName }}</span>
                    <span v-if="item.isHot" class="status-tag hot">热门</span>
                    <span v-if="item.isTeacherAnswered" class="status-tag teacher">老师已答</span>
                  </div>
                </div>
              </div>

              <div class="discussion-meta">
                <span class="meta-item">
                  <MessageOutlined class="meta-icon" />
                  {{ item.replyCount }} 回复
                </span>
                <span class="meta-sep">·</span>
                <span class="meta-item">
                  <EyeOutlined class="meta-icon" />
                  {{ item.viewCount }} 浏览
                </span>
                <span class="meta-sep">·</span>
                <span class="meta-item">
                  <ClockCircleOutlined class="meta-icon" />
                  {{ item.lastActiveTime }}
                </span>
              </div>
            </div>
          </div>
        </div>
        </div>
      </section>

      <div class="pagination-area" :class="{ 'is-placeholder': total <= pageSize }">
        <a-pagination
          v-if="total > pageSize"
          :current="page"
          :total="total"
          :pageSize="pageSize"
          show-less-items
          @change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ClockCircleOutlined,
  EyeOutlined,
  MessageOutlined,
  QuestionCircleOutlined,
  UserOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'

import {
  COURSE_FILTERS,
  getDiscussionList
} from '@/api/community'
import { useCommunityNotificationBadge } from '@/composables/useCommunityNotificationBadge'

import type { DiscussionItem } from '@/types/community'

type CommunityDisplayItem = DiscussionItem & {
  detailId?: number | string
  excerpt?: string
  teacherName?: string
  updatedAt?: string
  isFeatured?: boolean
  isRecommended?: boolean
}

const router = useRouter()
const { unreadCount, refreshUnreadCount } = useCommunityNotificationBadge()

const loading = ref(false)
const error = ref(false)

const list = ref<CommunityDisplayItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 5

const courseId = ref<string | number>('all')
const sort = ref<'latest' | 'hot'>('latest')
const keyword = ref('')

async function loadList() {
  loading.value = true
  error.value = false

  try {
    const res = await getDiscussionList({
      page: page.value,
      pageSize,
      courseId: courseId.value,
      sort: sort.value,
      keyword: keyword.value.trim()
    })

    list.value = (res.records || []).map(item => ({ ...item, detailId: item.id }))
    total.value = res.total || 0
  } catch (e) {
    console.error('[CommunityList] 加载列表失败', e)
    error.value = true
  } finally {
    loading.value = false
  }
}

function handleCourseChange(id: string | number) {
  courseId.value = id
  page.value = 1
  loadList()
}

function handleSortChange(nextSort: 'latest' | 'hot') {
  if (sort.value === nextSort) return
  sort.value = nextSort
  page.value = 1
  loadList()
}

function handleSearch() {
  page.value = 1
  loadList()
}

function handlePageChange(nextPage: number) {
  page.value = nextPage
  loadList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goDetail(item: CommunityDisplayItem) {
  router.push({
    name: 'CommunityDetail',
    params: { id: String(item.detailId || item.id) }
  })
}

onMounted(async () => {
  await Promise.all([
    loadList(),
    refreshUnreadCount()
  ])
})
</script>

<style scoped>
.community-page {
  min-height: calc(100vh - 70px);
  background: linear-gradient(120deg, #ffffff 0%, #f1f5f9 100%);
  color: #0f172a;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.page-container {
  width: 75%;
  min-width: 1200px;
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 16px 20px;
  border-radius: 5px;
  background: #FFFFFF;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

.hero-copy { flex: 1; min-width: 0; max-width: 680px; }
.hero-tag {
  display: inline-flex; align-items: center; height: 26px; padding: 0 10px;
  border-radius: 5px; background: rgba(37, 99, 235, 0.1); color: #2563EB;
  font-size: 12px; font-weight: 800; margin-bottom: 6px;
}
.hero-title { margin: 0; font-size: 22px; font-weight: 800; color: #0F172A; line-height: 1.25; }
.hero-desc { margin: 4px 0 0; font-size: 14px; line-height: 1.5; color: #64748B; }

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.hero-action-btn {
  position: relative;
  height: 34px;
  padding: 0 12px;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  background: #FFFFFF;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.hero-action-btn:hover {
  border-color: #BFDBFE;
  background: #EFF6FF;
  color: #2563EB;
}

.notification-dot {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #EF4444;
  color: #FFFFFF;
  font-size: 11px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.discussion-shell {
  margin-top: 16px;
  border-radius: 5px;
  background: #FFFFFF;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.toolbar-card {
  margin-top: 0; padding: 14px 18px; border-radius: 0;
  background: #FFFFFF; border: none; border-bottom: 1px solid #E7ECF3;
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
}
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 16px; }

.course-filter { display: flex; gap: 8px; flex-wrap: wrap; }
.filter-chip {
  height: 32px; padding: 0 12px; border: 1px solid #E7ECF3; border-radius: 6px;
  background: #FFFFFF; color: #475569; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
}
.filter-chip:hover { border-color: #BFDBFE; color: #2563EB; }
.filter-chip.active { background: #2563EB; border-color: #2563EB; color: #FFFFFF; }

.sort-switch { display: flex; background: #F1F5F9; padding: 4px; border-radius: 6px; }
.sort-btn {
  height: 28px; padding: 0 16px; border: none; background: transparent;
  border-radius: 4px; color: #64748B; font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
}
.sort-btn.active { background: #FFFFFF; color: #0F172A; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }

.search-box {
  display: flex; align-items: center; width: 260px; height: 36px; padding: 0 12px;
  border: 1px solid #E2E8F0; border-radius: 6px; background: #FFFFFF; transition: 0.2s;
}
.search-box:focus-within { border-color: #2563EB; box-shadow: 0 0 0 2px rgba(37,99,235,0.1); }
.search-icon { color: #94A3B8; font-size: 14px; margin-right: 8px; }
.search-box input { flex: 1; border: none; outline: none; font-size: 14px; color: #0F172A; }
.search-box input::placeholder { color: #94A3B8; }

.list-card {
  background: #FFFFFF; border: none; border-radius: 0;
  overflow: hidden; box-shadow: none;
  height: 560px;
  flex: 1 0 auto;
}
.discussion-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}
.discussion-item {
  min-height: 112px;
  padding: 18px 28px;
  border-bottom: 1px solid #EDF1F6;
  cursor: pointer;
  transition: background 0.18s ease;
}
.discussion-item:last-child { border-bottom: none; }
.discussion-item:hover { background: #F8FBFF; }
.discussion-item:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.35);
  outline-offset: -2px;
  background: #F8FBFF;
}

.discussion-main {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
}

.discussion-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 8px; }
.discussion-title { margin: 0; font-size: 16px; line-height: 1.5; font-weight: 700; color: #0F172A; transition: color 0.18s ease; }
.discussion-item:hover .discussion-title { color: #2563EB; }
.discussion-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.course-tag, .status-tag {
  height: 22px; padding: 0 8px; border-radius: 4px; font-size: 12px; font-weight: 500; display: inline-flex; align-items: center;
}
.course-tag { background: #F1F5F9; color: #475569; border: 1px solid #E2E8F0; }
.status-tag.hot { background: #FEF3C7; color: #D97706; }
.status-tag.teacher { background: #ECFCCB; color: #4D7C0F; }

.discussion-meta { display: flex; align-items: center; gap: 12px; color: #64748B; font-size: 13px; margin-top: 10px; }
.meta-item { display: inline-flex; align-items: center; gap: 6px; }
.meta-icon { font-size: 14px; color: #94A3B8; }
.meta-sep { color: #E2E8F0; }

.state-box { height: 100%; min-height: 260px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; }
.state-icon-circle { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
.state-empty-circle { background: #F1F5F9; color: #64748B; }
.state-error-circle { background: #FEF2F2; color: #DC2626; }
.state-text { font-size: 14px; color: #64748B; margin: 0; }
.btn-retry { height: 36px; padding: 0 16px; border: none; border-radius: 6px; background: #0F172A; color: #FFF; font-size: 14px; font-weight: 500; cursor: pointer; }

.pagination-area {
  min-height: 64px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px 0 8px;
}
.pagination-area.is-placeholder {
  visibility: hidden;
}
</style>
