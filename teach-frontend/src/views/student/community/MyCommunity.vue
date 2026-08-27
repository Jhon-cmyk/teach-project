<template>
  <div class="personal-community-page">
    <div class="page-container">
      <button class="back-btn" @click="router.push('/student/community')">
        <LeftOutlined />
        返回讨论广场
      </button>

      <section class="page-hero">
        <div>
          <h1 class="page-title">我的动态</h1>
          <p class="page-desc">集中查看我发起的讨论、参与的回复和社区通知。</p>
        </div>

        <button
          v-if="activeTab === 'notifications'"
          class="secondary-btn"
          :disabled="markingAll || unreadCount === 0"
          @click="handleReadAll"
        >
          <CheckOutlined />
          {{ markingAll ? '处理中...' : '全部已读' }}
        </button>
      </section>

      <section class="content-shell">
        <div class="tab-row">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'posts' }"
            @click="switchTab('posts')"
          >
            我的提问
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'replies' }"
            @click="switchTab('replies')"
          >
            我的回复
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'notifications' }"
            @click="switchTab('notifications')"
          >
            动态通知
            <span v-if="unreadCount > 0" class="tab-badge">
              {{ unreadCount > 99 ? '99+' : unreadCount }}
            </span>
          </button>
        </div>

        <div class="list-card">
          <div v-if="loading" class="state-box">
            <a-spin />
            <p class="state-text">正在加载...</p>
          </div>

          <div v-else-if="error" class="state-box">
            <div class="state-icon state-error">!</div>
            <p class="state-text">{{ errorText || '加载失败，请稍后重试' }}</p>
            <button class="retry-btn" @click="loadActiveTab">重新加载</button>
          </div>

          <div v-else-if="authRequired" class="state-box">
            <div class="state-icon state-error">!</div>
            <p class="state-text">请先登录后查看我的动态</p>
            <button class="retry-btn" @click="router.push('/student/community')">返回讨论广场</button>
          </div>

          <div v-else-if="activeTab === 'posts'" class="item-list">
            <div v-if="posts.length === 0" class="state-box">
              <FormOutlined class="empty-icon" />
              <p class="state-text">还没有发起过讨论</p>
            </div>
            <button
              v-for="item in posts"
              :key="item.id"
              class="community-item"
              @click="goToPostDetail(item)"
            >
              <div class="item-main">
                <div class="item-head">
                  <h3 class="item-title">{{ item.title }}</h3>
                  <div class="item-tags">
                    <span class="course-tag">{{ item.courseName }}</span>
                    <span class="tag">{{ item.postType === 'homework' ? '作业互助' : '讨论' }}</span>
                    <span v-if="item.isTeacherAnswered" class="tag tag-success">老师已答</span>
                    <span v-if="isPostFeatured(item)" class="tag tag-featured">已精选</span>
                  </div>
                </div>
                <div class="item-meta">
                  <span><MessageOutlined /> {{ item.replyCount }} 回复</span>
                  <span><EyeOutlined /> {{ item.viewCount }} 浏览</span>
                  <span><ClockCircleOutlined /> {{ item.lastActiveTime }}</span>
                </div>
              </div>
              <ArrowRightOutlined class="item-arrow" />
            </button>
          </div>

          <div v-else-if="activeTab === 'replies'" class="item-list">
            <div v-if="replies.length === 0" class="state-box">
              <MessageOutlined class="empty-icon" />
              <p class="state-text">还没有参与过回复</p>
            </div>
            <button
              v-for="item in replies"
              :key="item.discussionId"
              class="community-item"
              @click="goToReplyDetail(item.discussionId)"
            >
              <div class="item-main">
                <div class="item-head">
                  <h3 class="item-title">{{ item.title }}</h3>
                  <div class="item-tags">
                    <span class="course-tag">{{ item.courseName }}</span>
                    <span class="tag tag-success">已参与回复</span>
                  </div>
                </div>
                <div class="item-meta">
                  <span><CheckOutlined /> 我于 {{ item.myLastReplyTime }} 回复</span>
                  <span>共 {{ item.replyCount }} 条讨论</span>
                  <span><ClockCircleOutlined /> {{ item.lastActiveTime }}</span>
                </div>
              </div>
              <ArrowRightOutlined class="item-arrow" />
            </button>
          </div>

          <div v-else class="notification-panel">
            <div class="notification-toolbar">
              <div class="notification-summary">
                <span class="summary-title">通知</span>
                <span class="summary-meta">{{ notificationSummaryText }}</span>
              </div>

              <div class="notification-filters" aria-label="通知筛选">
                <button
                  v-for="filter in notificationFilters"
                  :key="filter.key"
                  class="filter-btn"
                  :class="{ active: notificationFilter === filter.key }"
                  @click="switchNotificationFilter(filter.key)"
                >
                  {{ filter.label }}
                </button>
              </div>
            </div>

            <div v-if="notifications.length === 0" class="state-box notification-empty">
              <BellOutlined class="empty-icon" />
              <p class="state-text">{{ notificationEmptyText }}</p>
            </div>
            <button
              v-for="item in notifications"
              :key="item.id"
              class="notification-row"
              :class="{ unread: !item.isRead, 'notification-item': true }"
              @click="handleOpenNotification(item)"
            >
              <span class="read-marker" :class="{ visible: !item.isRead }" aria-hidden="true"></span>

              <div class="notification-main">
                <div class="notification-line">
                  <span class="notification-primary">{{ getNotificationPrimaryText(item) }}</span>
                  <span class="tag" :class="getNotificationTagClass(item.type)">
                    {{ getTypeLabel(item.type) }}
                  </span>
                </div>
                <div class="notification-meta">
                  <span v-if="!item.isRead" class="unread-text">未读</span>
                  <span><ClockCircleOutlined /> {{ item.createdAt }}</span>
                </div>
              </div>
              <ArrowRightOutlined class="item-arrow" />
            </button>
          </div>
        </div>
      </section>

      <div class="pagination-area" :class="{ 'is-placeholder': currentTotal <= pageSize }">
        <a-pagination
          v-if="currentTotal > pageSize"
          :current="currentPage"
          :total="currentTotal"
          :pageSize="pageSize"
          show-less-items
          @change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowRightOutlined,
  BellOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  EyeOutlined,
  FormOutlined,
  LeftOutlined,
  MessageOutlined
} from '@ant-design/icons-vue'

import {
  extractCommunityErrorMessage,
  getCommunityNotifications,
  getFeaturedDiscussionMetaMap,
  getMyCommunityPosts,
  getMyCommunityReplies,
  isCommunityAuthError,
  readAllCommunityNotifications,
  readCommunityNotification
} from '@/api/community'
import { useCommunityNotificationBadge } from '@/composables/useCommunityNotificationBadge'

import type {
  CommunityNotificationItem,
  CommunityNotificationType,
  FeaturedAnswerItem,
  MyCommunityPostItem,
  MyCommunityReplyItem
} from '@/types/community'

type PersonalTab = 'posts' | 'replies' | 'notifications'
type NotificationFilter = 'all' | 'unread' | 'replies' | 'resolved' | 'featured'

const router = useRouter()
const route = useRoute()

const activeTab = ref<PersonalTab>('posts')
const loading = ref(false)
const error = ref(false)
const authRequired = ref(false)
const errorText = ref('')
const markingAll = ref(false)

const posts = ref<MyCommunityPostItem[]>([])
const postsTotal = ref(0)
const postsPage = ref(1)
const replies = ref<MyCommunityReplyItem[]>([])
const repliesTotal = ref(0)
const repliesPage = ref(1)
const notifications = ref<CommunityNotificationItem[]>([])
const notificationsTotal = ref(0)
const notificationsPage = ref(1)
const notificationFilter = ref<NotificationFilter>('all')
const featuredMetaMap = ref<Record<string, FeaturedAnswerItem>>({})

const notificationFilters: Array<{ key: NotificationFilter; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'unread', label: '未读' },
  { key: 'replies', label: '回复' },
  { key: 'resolved', label: '已解决' },
  { key: 'featured', label: '精选' }
]

const pageSize = 5
const {
  unreadCount,
  refreshUnreadCount,
  decreaseUnreadCount,
  clearUnreadCount
} = useCommunityNotificationBadge()

const currentPage = computed(() => {
  if (activeTab.value === 'posts') return postsPage.value
  if (activeTab.value === 'replies') return repliesPage.value
  return notificationsPage.value
})

const currentTotal = computed(() => {
  if (activeTab.value === 'posts') return postsTotal.value
  if (activeTab.value === 'replies') return repliesTotal.value
  return notificationsTotal.value
})

const notificationSummaryText = computed(() => {
  const totalText = notificationFilter.value === 'all'
    ? `共 ${notificationsTotal.value} 条`
    : `当前筛选 ${notificationsTotal.value} 条`
  return unreadCount.value > 0
    ? `${totalText}，${unreadCount.value} 条未读`
    : `${totalText}，暂无未读`
})

const notificationEmptyText = computed(() => {
  if (notificationFilter.value === 'unread') return '没有未读通知'
  if (notificationFilter.value === 'replies') return '暂无回复相关通知'
  if (notificationFilter.value === 'resolved') return '暂无已解决状态通知'
  if (notificationFilter.value === 'featured') return '暂无精选相关通知'
  return '当前还没有社区通知'
})

function syncTabFromRoute() {
  if (route.query.tab === 'notifications') {
    activeTab.value = 'notifications'
  } else if (route.query.tab === 'replies') {
    activeTab.value = 'replies'
  } else {
    activeTab.value = 'posts'
  }
}

function resetRequestState() {
  loading.value = true
  error.value = false
  authRequired.value = false
  errorText.value = ''
}

async function fetchPosts() {
  resetRequestState()
  try {
    const res = await getMyCommunityPosts({ page: postsPage.value, pageSize })
    posts.value = res.records || []
    postsTotal.value = res.total || 0
    featuredMetaMap.value = await getFeaturedDiscussionMetaMap({ pageSize: 100 })
  } catch (e) {
    if (isCommunityAuthError(e)) authRequired.value = true
    else {
      error.value = true
      errorText.value = extractCommunityErrorMessage(e, '加载失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

async function fetchReplies() {
  resetRequestState()
  try {
    const res = await getMyCommunityReplies({ page: repliesPage.value, pageSize })
    replies.value = res.records || []
    repliesTotal.value = res.total || 0
  } catch (e) {
    if (isCommunityAuthError(e)) authRequired.value = true
    else {
      error.value = true
      errorText.value = extractCommunityErrorMessage(e, '加载失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

async function fetchNotifications() {
  resetRequestState()
  try {
    const filterParams = getNotificationFilterParams()
    const res = await getCommunityNotifications({
      page: notificationsPage.value,
      pageSize,
      ...filterParams
    })
    notifications.value = res.records || []
    notificationsTotal.value = res.total || 0
    await refreshUnreadCount()
  } catch (e) {
    if (isCommunityAuthError(e)) authRequired.value = true
    else {
      error.value = true
      errorText.value = extractCommunityErrorMessage(e, '加载失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

function loadActiveTab() {
  if (activeTab.value === 'posts') return fetchPosts()
  if (activeTab.value === 'replies') return fetchReplies()
  return fetchNotifications()
}

function switchTab(tab: PersonalTab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  router.replace({
    name: 'MyCommunity',
    query: tab === 'posts' ? {} : { tab }
  })
  loadActiveTab()
}

function switchNotificationFilter(filter: NotificationFilter) {
  if (notificationFilter.value === filter) return
  notificationFilter.value = filter
  notificationsPage.value = 1
  fetchNotifications()
}

function handlePageChange(nextPage: number) {
  if (activeTab.value === 'posts') postsPage.value = nextPage
  else if (activeTab.value === 'replies') repliesPage.value = nextPage
  else notificationsPage.value = nextPage

  loadActiveTab()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function toPlainText(content: string) {
  const raw = String(content || '')
  if (!raw) return ''
  if (typeof window === 'undefined') {
    return raw.replace(/<[^>]+>/g, ' ').replace(/&nbsp;/gi, ' ').replace(/\s+/g, ' ').trim()
  }

  const div = document.createElement('div')
  div.innerHTML = raw
  return (div.textContent || div.innerText || '')
    .replace(/\u00A0/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function getTypeLabel(type: CommunityNotificationType) {
  switch (type) {
    case 'post_replied': return '新回复'
    case 'post_resolved': return '已解决'
    case 'post_featured': return '入选精选'
    case 'followed_discussion_updated': return '讨论更新'
    default: return '社区动态'
  }
}

function getNotificationFilterParams(): {
  isRead?: 0 | 1
  type?: CommunityNotificationType
} {
  switch (notificationFilter.value) {
    case 'unread':
      return { isRead: 0 }
    case 'replies':
      return { type: 'post_replied' }
    case 'resolved':
      return { type: 'post_resolved' }
    case 'featured':
      return { type: 'post_featured' }
    default:
      return {}
  }
}

function getNotificationPrimaryText(item: CommunityNotificationItem) {
  const content = toPlainText(item.content)
  return content || item.title || getTypeLabel(item.type)
}

function getNotificationTagClass(type: CommunityNotificationType) {
  return {
    'tag-info': type === 'post_replied' || type === 'followed_discussion_updated',
    'tag-success': type === 'post_resolved',
    'tag-featured': type === 'post_featured'
  }
}

function buildDetailQuery(item: CommunityNotificationItem) {
  const query: Record<string, string> = { from: 'notifications', notificationType: item.type }
  if (item.replyId) query.replyId = String(item.replyId)
  if (item.type === 'post_replied' || item.type === 'followed_discussion_updated') {
    query.focus = 'replies'
  } else {
    query.focus = 'overview'
    query.highlight = item.type === 'post_featured' ? 'featured' : 'resolved'
  }
  return query
}

async function handleOpenNotification(item: CommunityNotificationItem) {
  try {
    if (!item.isRead) {
      await readCommunityNotification(item.id)
      item.isRead = true
      decreaseUnreadCount()
    }
    router.push({
      name: 'CommunityDetail',
      params: { id: String(item.postId) },
      query: buildDetailQuery(item)
    })
  } catch (e) {
    console.error('[MyCommunity] open notification failed', e)
    message.error('打开动态失败')
  }
}

async function handleReadAll() {
  if (unreadCount.value === 0) return
  markingAll.value = true
  try {
    await readAllCommunityNotifications()
    notifications.value = notifications.value.map(item => ({ ...item, isRead: true }))
    clearUnreadCount()
    message.success('已全部标记为已读')
  } catch (e) {
    console.error('[MyCommunity] read all failed', e)
    message.error('操作失败')
  } finally {
    markingAll.value = false
  }
}

const isPostFeatured = (item: MyCommunityPostItem) => !!featuredMetaMap.value[String(item.id)]
const goToPostDetail = (item: MyCommunityPostItem) => router.push({ name: 'CommunityDetail', params: { id: item.id }, query: { from: 'mine' } })
const goToReplyDetail = (id: number | string) => router.push({ name: 'CommunityDetail', params: { id }, query: { from: 'mine', focus: 'replies' } })

watch(
  () => [route.name, route.query.tab],
  () => {
    const before = activeTab.value
    syncTabFromRoute()
    if (before !== activeTab.value) loadActiveTab()
  }
)

onMounted(async () => {
  syncTabFromRoute()
  await Promise.all([
    loadActiveTab(),
    refreshUnreadCount()
  ])
})
</script>

<style scoped>
.personal-community-page {
  min-height: calc(100vh - 70px);
  background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%);
  color: #0F172A;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  padding-bottom: 40px;
}

.page-container {
  width: 75%;
  min-width: 1200px;
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
}

.back-btn {
  height: 36px;
  padding: 0 14px 0 12px;
  border: 1px solid #DDE7F2;
  border-radius: 6px;
  background: #FFFFFF;
  color: #475569;
  font-size: 14px;
  font-weight: 750;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, color 0.2s;
  box-shadow: none;
}

.back-btn :deep(svg) {
  font-size: 13px;
}

.back-btn:hover {
  color: #2563EB;
  border-color: #BFDBFE;
  background: #EFF6FF;
}

.page-hero {
  margin-top: 12px;
  padding: 16px 20px;
  border-radius: 5px;
  background: #FFFFFF;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-hero > div {
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 22px;
  line-height: 1.25;
  font-weight: 800;
}

.page-desc {
  margin: 5px 0 0;
  color: #64748B;
  font-size: 14px;
  line-height: 1.5;
}

.secondary-btn {
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
}

.secondary-btn:hover:not(:disabled) {
  border-color: #BFDBFE;
  background: #EFF6FF;
  color: #2563EB;
}

.secondary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.content-shell {
  margin-top: 16px;
  border-radius: 5px;
  background: #FFFFFF;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}

.tab-row {
  padding: 14px 18px;
  border-bottom: 1px solid #E7ECF3;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tab-btn {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #E7ECF3;
  border-radius: 6px;
  background: #FFFFFF;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.tab-btn:hover {
  border-color: #BFDBFE;
  color: #2563EB;
}

.tab-btn.active {
  background: #2563EB;
  border-color: #2563EB;
  color: #FFFFFF;
}

.tab-badge {
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

.list-card {
  height: 580px;
  overflow: hidden;
  background: #FFFFFF;
}

.item-list {
  height: 100%;
}

.community-item {
  width: 100%;
  height: 116px;
  padding: 18px 28px;
  box-sizing: border-box;
  border: none;
  border-bottom: 1px solid #EDF1F6;
  background: #FFFFFF;
  color: inherit;
  text-align: left;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  cursor: pointer;
  transition: background 0.18s ease;
}

.community-item:last-child { border-bottom: none; }
.community-item:hover { background: #F8FBFF; }
.community-item.unread { background: #F8FAFC; }
.community-item.unread:hover { background: #EFF6FF; }

.item-main {
  width: 100%;
  max-width: 1180px;
  min-width: 0;
  margin: 0 auto;
}

.item-head {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  margin-bottom: 9px;
}

.item-title {
  margin: 0;
  color: #0F172A;
  font-size: 16px;
  line-height: 1.5;
  font-weight: 800;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.community-item:hover .item-title { color: #2563EB; }

.item-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 8px;
  min-height: 22px;
  overflow: hidden;
}

.course-tag,
.tag,
.unread-dot {
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}

.course-tag {
  background: #F1F5F9;
  color: #475569;
  border: 1px solid #E2E8F0;
}

.tag {
  background: #F3F4F6;
  color: #4B5563;
}

.tag-success {
  background: #ECFCCB;
  color: #4D7C0F;
}

.tag-featured {
  background: #FFF7ED;
  color: #EA580C;
}

.unread-dot {
  background: #EFF6FF;
  color: #2563EB;
}

.notification-text {
  margin: 0 0 8px;
  color: #475569;
  font-size: 14px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-item .item-head {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.notification-item .item-title {
  flex: 1;
  min-width: 0;
}

.notification-item .item-tags {
  flex-wrap: nowrap;
  justify-content: flex-end;
}

.notification-item .notification-text {
  margin-bottom: 6px;
  line-height: 1.4;
}

.notification-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #FFFFFF;
}

.notification-toolbar {
  min-height: 56px;
  padding: 12px 18px;
  border-bottom: 1px solid #EDF1F6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.notification-summary {
  display: inline-flex;
  align-items: baseline;
  gap: 10px;
  min-width: 220px;
}

.summary-title {
  color: #0F172A;
  font-size: 14px;
  font-weight: 800;
}

.summary-meta {
  color: #64748B;
  font-size: 13px;
  white-space: nowrap;
}

.notification-filters {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.filter-btn {
  height: 28px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 5px;
  background: transparent;
  color: #64748B;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.filter-btn:hover {
  border-color: #BFDBFE;
  background: #EFF6FF;
  color: #2563EB;
}

.filter-btn.active {
  border-color: #D7E5FF;
  background: #EFF6FF;
  color: #2563EB;
}

.notification-empty {
  flex: 1;
}

.notification-row {
  width: 100%;
  min-height: 82px;
  padding: 16px 22px 16px 18px;
  box-sizing: border-box;
  border: none;
  border-bottom: 1px solid #EDF1F6;
  background: #FFFFFF;
  color: inherit;
  text-align: left;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: background 0.18s ease;
}

.notification-row:hover,
.notification-row:focus-visible {
  background: #F8FBFF;
}

.notification-row:focus-visible {
  outline: 2px solid rgba(37, 99, 235, 0.35);
  outline-offset: -2px;
}

.notification-row.unread {
  background: #F8FAFC;
}

.notification-row.unread:hover,
.notification-row.unread:focus-visible {
  background: #EFF6FF;
}

.read-marker {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: transparent;
  flex: 0 0 8px;
}

.read-marker.visible {
  background: #2563EB;
}

.notification-main {
  flex: 1;
  min-width: 0;
}

.notification-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notification-primary {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0F172A;
  font-size: 15px;
  line-height: 1.5;
  font-weight: 750;
}

.notification-row:hover .notification-primary {
  color: #2563EB;
}

.notification-meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748B;
  font-size: 13px;
  overflow: hidden;
}

.notification-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.unread-text {
  color: #2563EB;
  font-weight: 700;
}

.tag-info {
  background: #EFF6FF;
  color: #2563EB;
}

.item-meta {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 14px;
  color: #64748B;
  font-size: 13px;
  overflow: hidden;
}

.item-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.item-arrow {
  margin-top: 4px;
  color: #CBD5E1;
  flex-shrink: 0;
  transition: color 0.18s ease, transform 0.18s ease;
}

.community-item:hover .item-arrow {
  color: #2563EB;
  transform: translateX(2px);
}

.state-box {
  height: 100%;
  min-height: 300px;
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.state-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.state-error {
  background: #FEF2F2;
  color: #DC2626;
}

.empty-icon {
  color: #CBD5E1;
  font-size: 40px;
}

.state-text {
  margin: 0;
  color: #64748B;
  font-size: 14px;
}

.retry-btn {
  height: 34px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  background: #0F172A;
  color: #FFFFFF;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.pagination-area {
  min-height: 64px;
  padding: 16px 0 8px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.pagination-area.is-placeholder {
  visibility: hidden;
}
</style>
