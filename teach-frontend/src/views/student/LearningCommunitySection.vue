<template>
  <section class="layer-community">
    <div class="container-1200">
      <div class="community-header">
        <div class="header-left">
          <h3 class="section-title">学习交流</h3>
          <span class="header-dot">·</span>
          <span class="section-subtitle">看看大家最近在讨论什么，也别错过高质量答疑</span>
        </div>
        <button class="btn-view-more" @click="goToCommunityList">
          查看更多
          <RightOutlined class="more-icon" />
        </button>
      </div>

      <div class="community-body">
        <div class="community-main">
          <div class="discussion-card">
            <div v-if="loading" class="state-box">
              <a-spin />
              <span class="state-text">正在加载讨论...</span>
            </div>

            <div v-else-if="error" class="state-box">
              <div class="state-icon-circle state-error-circle">!</div>
              <p class="state-text">加载失败，请稍后再试</p>
              <button class="btn-retry" @click="fetchOverview">重新加载</button>
            </div>

            <div v-else-if="discussions.length === 0" class="state-box">
              <div class="state-icon-circle state-empty-circle">
                <MessageOutlined />
              </div>
              <p class="state-text">暂无讨论内容</p>
            </div>

            <div v-else class="discussion-list">
              <div
                v-for="(item, idx) in visibleDiscussions"
                :key="item.id"
                class="discussion-item"
                @click="goToDiscussionDetail(item)"
              >
                <span class="discussion-index">{{ idx + 1 }}</span>
                <div class="discussion-content">
                  <div class="discussion-title">{{ item.title }}</div>
                  <div class="discussion-meta">
                    <span class="course-tag">{{ item.courseName }}</span>
                    <span class="meta-sep">·</span>
                    <span class="meta-item">
                      <MessageOutlined class="meta-icon" />
                      {{ item.replyCount }}
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
        </div>

        <div class="community-aside">
          <div class="aside-card" @click="goToHomeworkHelp">
            <div class="aside-card-top">
              <div class="aside-stat">
                <span class="aside-stat-num">{{ homeworkHelp.todayQuestionCount }}</span>
                <span class="aside-stat-unit">条新提问</span>
              </div>
              <span class="aside-badge homework-badge">今日</span>
            </div>
            <h4 class="aside-card-title">作业互助</h4>
            <p class="aside-card-desc">交流解题思路、错因分析和经验分享</p>
            <div class="aside-card-foot">
              <span class="btn-aside">
                进入作业区
                <RightOutlined class="aside-arrow" />
              </span>
            </div>
          </div>

          <div class="aside-card aside-card-featured" @click="goToFeaturedAnswers">
            <div class="aside-card-top">
              <div class="aside-stat">
                <span class="aside-stat-num">{{ featuredAnswers.weeklySelectedCount }}</span>
                <span class="aside-stat-unit">条精选回答</span>
              </div>
              <span class="aside-badge answer-badge">本周</span>
            </div>

            <div class="featured-card-head">
              <h4 class="aside-card-title">答疑精选</h4>
              <span class="featured-card-note">本周值得看</span>
            </div>

            <p class="aside-card-desc">老师整理的重点问题与高质量答疑，适合快速补齐易错点。</p>

            <div v-if="featuredPreviewList.length > 0" class="featured-preview-list">
              <button
                v-for="item in featuredPreviewList"
                :key="item.id"
                class="featured-preview-item"
                @click.stop="goToFeaturedDetail(item)"
              >
                <span class="featured-preview-title">{{ item.title }}</span>
                <span class="featured-preview-meta">
                  {{ item.courseName }}
                  <span v-if="item.isRecommended" class="featured-mini-tag">推荐</span>
                </span>
              </button>
            </div>

            <div class="aside-card-foot">
              <span class="btn-aside">
                查看全部答疑
                <RightOutlined class="aside-arrow" />
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { RightOutlined, MessageOutlined, EyeOutlined } from '@ant-design/icons-vue'
import {
  getCommunityOverview,
  getFeaturedAnswersPreview
} from '@/api/community'
import type {
  DiscussionItem,
  HomeworkHelpSummary,
  FeaturedAnswersSummary,
  FeaturedAnswerItem
} from '@/types/community'

const COMMUNITY_OVERVIEW_CACHE_TTL = 2 * 60 * 1000
const DASHBOARD_DISCUSSION_LIMIT = 6

type CommunityOverviewCache = {
  discussions: DiscussionItem[]
  homeworkHelp: HomeworkHelpSummary
  featuredAnswers: FeaturedAnswersSummary
  featuredPreviewList: FeaturedAnswerItem[]
  cachedAt: number
}

type CommunityOverviewCacheHost = Window & {
  __studentDashboardCommunityCache?: CommunityOverviewCache | null
  __studentDashboardCommunityRequest?: Promise<void> | null
}

const getCommunityOverviewCacheHost = () =>
  window as unknown as CommunityOverviewCacheHost

const router = useRouter()
const loading = ref(false)
const error = ref(false)

const discussions = ref<DiscussionItem[]>([])
const homeworkHelp = ref<HomeworkHelpSummary>({ todayQuestionCount: 0 })
const featuredAnswers = ref<FeaturedAnswersSummary>({ weeklySelectedCount: 0 })
const featuredPreviewList = ref<FeaturedAnswerItem[]>([])
const visibleDiscussions = computed(() => discussions.value.slice(0, DASHBOARD_DISCUSSION_LIMIT))

function applyOverviewData(cache: Omit<CommunityOverviewCache, 'cachedAt'>) {
  discussions.value = cache.discussions || []
  homeworkHelp.value = cache.homeworkHelp || { todayQuestionCount: 0 }
  featuredAnswers.value = cache.featuredAnswers || { weeklySelectedCount: 0 }
  featuredPreviewList.value = cache.featuredPreviewList || []
}

async function fetchOverview() {
  const cacheHost = getCommunityOverviewCacheHost()
  const cached = cacheHost.__studentDashboardCommunityCache
  if (cached && Date.now() - cached.cachedAt < COMMUNITY_OVERVIEW_CACHE_TTL) {
    applyOverviewData(cached)
    return
  }

  const pendingRequest = cacheHost.__studentDashboardCommunityRequest
  if (pendingRequest) {
    loading.value = true
    error.value = false
    try {
      await pendingRequest
      const latestCache = cacheHost.__studentDashboardCommunityCache
      if (latestCache) {
        applyOverviewData(latestCache)
      }
    } catch (e) {
      error.value = true
    } finally {
      loading.value = false
    }
    return
  }

  loading.value = true
  error.value = false

  try {
    const requestPromise = Promise.all([
      getCommunityOverview(),
      getFeaturedAnswersPreview(2)
    ])
    cacheHost.__studentDashboardCommunityRequest = requestPromise.then(() => undefined)
    const [overviewData, previewData] = await requestPromise

    discussions.value = overviewData.discussions || []
    homeworkHelp.value = overviewData.homeworkHelp || { todayQuestionCount: 0 }
    featuredAnswers.value = overviewData.featuredAnswers || { weeklySelectedCount: 0 }
    // 强制将精选回答数量写死为 3
    featuredAnswers.value.weeklySelectedCount = 3
    featuredPreviewList.value = previewData || []
    cacheHost.__studentDashboardCommunityCache = {
      discussions: discussions.value,
      homeworkHelp: homeworkHelp.value,
      featuredAnswers: featuredAnswers.value,
      featuredPreviewList: featuredPreviewList.value,
      cachedAt: Date.now()
    }
  } catch (e) {
    error.value = true
    console.error('[LearningCommunity] 加载首页学习交流数据失败', e)
  } finally {
    cacheHost.__studentDashboardCommunityRequest = null
    loading.value = false
  }
}

function goToCommunityList() {
  router.push({ name: 'CommunityList' })
}

function goToDiscussionDetail(item: DiscussionItem) {
  router.push({
    name: 'CommunityDetail',
    params: { id: String(item.id) },
    query: { from: 'dashboard' }
  })
}

function goToHomeworkHelp() {
  router.push({ name: 'HomeworkHelp' })
}

function goToFeaturedAnswers() {
  router.push({ name: 'FeaturedAnswers' })
}

function goToFeaturedDetail(item: FeaturedAnswerItem) {
  router.push({
    name: 'CommunityDetail',
    params: { id: String(item.discussionId) },
    query: {
      from: 'dashboard',
      highlight: 'featured'
    }
  })
}

onMounted(() => {
  fetchOverview()
})
</script>

<style scoped>
/* ================= 基础布局 ================= */
.layer-community {
  background: transparent;
  padding-top: 10px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.container-1200 {
  width: 75%;
  min-width: 1000px;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ================= 头部区域：恢复蓝条规范 ================= */
.community-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #0F172A;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 统一的左侧蓝色指示条 */
.section-title::before {
  content: '';
  display: block;
  width: 4px;
  height: 18px;
  background: #2563EB;
  border-radius: 2px;
}

.header-dot {
  color: #CBD5E1;
  font-weight: bold;
}

.section-subtitle {
  font-size: 14px;
  color: #64748B;
  font-weight: 400;
}

.btn-view-more {
  background: transparent;
  color: #475569;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-view-more:hover {
  background: #F1F5F9;
  color: #0F172A;
}

/* ================= 主体结构 ================= */
.community-body {
  display: flex;
  gap: 20px;
  align-items: stretch;
}

.community-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.community-aside {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex-shrink: 0;
}

/* ================= 左侧讨论列表卡片 ================= */
.discussion-card {
  flex: 1;
  background: #FFFFFF;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.discussion-list {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.discussion-item {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 16px 22px;
  cursor: pointer;
  background: #FFFFFF;
  border-bottom: 1px solid #F1F5F9;
  transition: background 0.2s ease;
}

.discussion-item:last-child {
  border-bottom: none;
}

.discussion-item:hover {
  background: #F8FAFC;
}

.discussion-index {
  display: none;
}

.discussion-content {
  flex: 1;
  min-width: 0;
}

.discussion-title {
  font-size: 15px;
  line-height: 1.5;
  color: #0F172A;
  font-weight: 600;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.discussion-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #64748B;
  font-size: 13px;
}

.course-tag {
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  background: #F1F5F9;
  color: #475569;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.meta-icon {
  font-size: 14px;
  color: #94A3B8;
}

.meta-time {
  margin-left: auto;
  font-size: 12px;
  color: #94A3B8;
}

/* ================= 右侧边栏卡片 ================= */
.aside-card {
  padding: 16px 18px;
  background: #FFFFFF;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.aside-card:hover {
  border-color: #CBD5E1;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.aside-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.aside-stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #0F172A;
  letter-spacing: -0.5px;
  line-height: 1;
}

.aside-stat-unit {
  font-size: 13px;
  color: #64748B;
  margin-left: 6px;
  font-weight: 500;
}

.aside-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.homework-badge {
  background: #EFF6FF;
  color: #2563EB;
}

.answer-badge {
  background: #FFF7ED;
  color: #EA580C;
}

.aside-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #0F172A;
  margin: 12px 0 5px;
}

.aside-card-desc {
  font-size: 13px;
  line-height: 1.5;
  color: #64748B;
  margin-bottom: 14px;
}

/* 精选卡片特定布局 */
.aside-card-featured {
  flex: 1;
}

.featured-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.featured-card-note {
  font-size: 12px;
  color: #64748B;
  background: #F1F5F9;
  padding: 2px 8px;
  border-radius: 4px;
  margin-top: 10px;
  font-weight: 500;
}

.aside-card-featured .aside-card-foot {
  margin-top: auto;
}

/* 侧边精选列表内嵌 */
.featured-preview-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin-bottom: 14px;
}

.featured-preview-item {
  width: 100%;
  border: 1px solid transparent;
  background: #F8FAFC;
  border-radius: 6px;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.featured-preview-item:hover {
  background: #FFFFFF;
  border-color: #CBD5E1;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.featured-preview-title {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  font-weight: 500;
  color: #0F172A;
  margin-bottom: 6px;
  line-height: 1.4;
}

.featured-preview-meta {
  font-size: 12px;
  color: #94A3B8;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.featured-mini-tag {
  background: #FEE2E2;
  color: #DC2626;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
  font-size: 11px;
}

/* 底部按钮线 */
.aside-card-foot {
  border-top: 1px solid #F1F5F9;
  padding-top: 12px;
}

.btn-aside {
  font-size: 13px;
  font-weight: 500;
  color: #0F172A;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}

.aside-card:hover .btn-aside {
  color: #2563EB;
}

.btn-aside .aside-arrow {
  color: #94A3B8;
  transition: transform 0.2s, color 0.2s;
}

.aside-card:hover .aside-arrow {
  color: #2563EB;
  transform: translateX(2px);
}

/* ================= 状态加载框 ================= */
.state-box {
  padding: 60px 0;
  color: #64748B;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.state-icon-circle {
  font-size: 28px;
  margin-bottom: 12px;
  color: #CBD5E1;
}

.btn-retry {
  margin-top: 16px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  padding: 6px 16px;
  border-radius: 6px;
  color: #0F172A;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
}

.btn-retry:hover {
  background: #F8FAFC;
  border-color: #CBD5E1;
}

/* ================= 响应式调整 ================= */
@media (max-width: 1200px) {
  .community-body {
    flex-direction: column;
  }

  .community-aside {
    width: 100%;
    flex-direction: row;
  }

  .aside-card {
    flex: 1;
  }
}
</style>
