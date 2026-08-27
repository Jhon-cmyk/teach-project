<template>
  <div class="page">
    <main class="main-content-wrapper">
      <section class="layer-welcome">
        <div class="container-1200">
          <div class="welcome-hero">
            <div class="hero-left">
              <h1 class="greeting-title">{{ greetingInfo.title }}，{{ displayUserName }}</h1>
              <p class="greeting-sub">{{ greetingInfo.sub }}</p>
            </div>
            <div class="hero-search-box">
              <search-outlined class="search-icon" />
              <input v-model="searchText" @keyup.enter="handleSearch" placeholder="搜索课程、知识点或资料..." />
              <button class="btn-hero-search" @click="handleSearch">搜索</button>
            </div>
          </div>
        </div>
      </section>

      <section class="layer-dashboard">
        <div class="container-1200">
          <div class="top-section">
            <div class="left-block card">
              <div class="left-block-title">课程分类</div>
              <div class="left-top-content">
                <a-row :gutter="10">
                  <a-col
                    :span="8"
                    v-for="item in baseTypeData"
                    :key="'cat-' + item.name"
                    class="category-item"
                    @click="goToTypeFilter(item)"
                  >
                    <img :src="item.img" alt="" class="cat-icon-img" />
                    <div class="cat-name">{{ item.name }}</div>
                  </a-col>
                </a-row>
              </div>

              <div class="left-block-title" style="margin-top: 15px;">兴趣拔高课</div>
              <div class="left-middle-content excellent-section">
                <a-row :gutter="10">
                  <a-col
                    :span="8"
                    v-for="item in excellentTypeData"
                    :key="'int-' + item.name"
                    class="category-item"
                    @click="goToTypeFilter(item)"
                  >
                    <img :src="item.img" alt="" class="cat-icon-img" />
                    <div class="cat-name">{{ item.name }}</div>
                  </a-col>
                </a-row>
              </div>
            </div>

            <div class="middle-top-block">
              <div class="banner-wrapper">
                <a-carousel autoplay effect="fade" :dots="true" arrows>
                  <template #prevArrow><div class="custom-arrow left-arrow"><left-outlined /></div></template>
                  <template #nextArrow><div class="custom-arrow right-arrow"><right-outlined /></div></template>

                  <div
                    v-for="(item, index) in bannerList"
                    :key="item.id || index"
                    class="carousel-slide"
                    :class="{ clickable: !!item.targetUrl }"
                    @click="handleBannerClick(item)"
                  >
                    <img :src="item.imageUrl" alt="课程推荐" class="banner-image" />
                    <div class="banner-overlay">
                      <span class="banner-tag">本周推荐</span>
                      <h2 class="banner-title">{{ item.title || '开启你的系统化学习之旅' }}</h2>
                    </div>
                  </div>
                </a-carousel>
              </div>

              <div class="card heatmap-card calendar-heatmap">
                <div class="card-header" style="border-bottom: none; margin-bottom: 0;">
                  <h3 style="font-size: 16px; font-weight: bold; margin: 5px 0;">学习打卡热力图</h3>
                </div>
                <div ref="heatmapChartRef" class="heatmap-chart-box"></div>
                <div class="heatmap-footer">颜色越深表示学习时间越长</div>
              </div>
            </div>

            <div class="right-top-block card">
              <div style="font-size: 16px; font-weight: bold; text-align: center; margin-bottom: 0px; padding-top: 5px;">
                近 7 天学习专注
              </div>
              <div ref="chartRef" class="echarts-box" style="height: 190px; width: 100%;"></div>
              <div style="margin-top: 5px; font-size: 12px; color: #98A2B3; text-align: center; margin-bottom: 12px;">
                本周学习时长趋势（单位：小时）
              </div>

              <div class="todo-section">
                <div class="todo-header">
                  <span>今日计划</span>
                  <span class="plan-progress-text">{{ completedPlanCount || 0 }}/{{ dailyPlans?.length || 0 }}</span>
                </div>

                <div class="todo-list" v-if="dailyPlans && dailyPlans.length > 0">
                  <div v-for="plan in dailyPlans" :key="plan.id" class="todo-item" :class="{ done: plan.completed }">
                    <label class="custom-checkbox">
                      <input type="checkbox" :checked="plan.completed" @change="userStore.togglePlanStatus(plan.id)">
                      <span class="checkmark"></span>
                    </label>
                    <span class="todo-text" @click="userStore.togglePlanStatus(plan.id)">{{ plan.content }}</span>
                    <button class="btn-delete-todo" @click="userStore.deletePlan(plan.id)"><close-outlined /></button>
                  </div>
                </div>
                <div v-else class="plan-empty">
                  <schedule-outlined style="font-size: 36px; color: #D1D5DB; margin-bottom: 8px;" />
                  <span>暂无计划，开启新课程吧</span>
                </div>

                <div class="todo-add-area">
                  <input v-model="newPlanContent" @keyup.enter="handleAddPlan" placeholder="添加学习任务..." />
                  <button class="btn-todo-add" @click="handleAddPlan" :disabled="!newPlanContent.trim()">添加</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section ref="recommendationSectionRef" class="layer-bottom">
        <div class="container-1200">
          <div class="enhanced-header">
            <div class="header-left">
              <h3 class="section-title">个性化资源推荐</h3>
              <p class="section-subtitle">根据今日推荐、学习画像和薄弱点整理，优先展示适合继续学习的视频课程资源。</p>
            </div>
            <div class="recommendation-tools">
              <button class="profile-link-btn" @click="goToLearningProfile">学习画像</button>
              <div class="tab-row">
                <button
                  v-for="tab in tabs"
                  :key="tab.key"
                  class="tab-btn"
                  :class="{ active: activeTab === tab.key }"
                  @click="activeTab = tab.key"
                >
                  {{ tab.label }}
                </button>
              </div>
            </div>
          </div>

          <a-spin :spinning="recommendationSpinVisible">
            <div v-if="displayedRecommendationList.length > 0" class="recommendation-grid">
              <div
                v-for="(item, index) in displayedRecommendationList"
                :key="item.id"
                v-show="index < RECOMMENDATIONS_PER_PAGE"
                class="recommendation-card"
                @click="goToRecommendation(item)"
              >
                <div class="recommendation-cover">
                  <img
                    v-if="getRecommendationCover(item)"
                    :src="getRecommendationCover(item)"
                    :alt="displayRecommendationTitle(item)"
                    loading="lazy"
                    @error="markRecommendationCoverFailed(item)"
                  />
                  <div
                    v-else
                    class="recommendation-smart-cover"
                    :class="getRecommendationCoverTheme(item).className"
                  >
                    <div class="smart-cover-grid"></div>
                    <div class="smart-cover-top">
                      <span>{{ resourceTypeLabel(item.resourceType) }}</span>
                      <span>{{ getRecommendationCoverTheme(item).badge }}</span>
                    </div>
                    <div class="smart-cover-center">
                      <span class="smart-cover-symbol">{{ getRecommendationCoverTheme(item).symbol }}</span>
                      <span class="smart-cover-title">{{ getRecommendationCoverTheme(item).title }}</span>
                    </div>
                    <div class="smart-cover-bottom">
                      <span v-for="mark in getRecommendationCoverMarks(item)" :key="mark">{{ mark }}</span>
                    </div>
                  </div>
                  <div class="recommendation-cover-overlay">{{ item.actionLabel || '打开资源' }}</div>
                </div>
                <div class="recommendation-topline">
                  <span class="resource-type-pill">{{ resourceTypeLabel(item.resourceType) }}</span>
                  <span v-if="recommendationSourceLabel(item.recommendationSource)" class="source-pill">
                    {{ recommendationSourceLabel(item.recommendationSource) }}
                  </span>
                </div>
                <div class="recommendation-main">
                  <h3 class="recommendation-title" :title="displayRecommendationTitle(item)">
                    {{ displayRecommendationTitle(item) }}
                  </h3>
                  <p class="recommendation-reason">
                    {{ item.shortReason || item.recommendationReason || '系统根据近期学习记录为你整理了这条资源。' }}
                  </p>
                </div>
                <div class="recommendation-footer">
                  <span class="knowledge-chip">{{ item.knowledgeName || '综合补强' }}</span>
                  <span class="recommendation-action">{{ item.actionLabel || '打开资源' }}</span>
                </div>
              </div>
            </div>

            <div class="pagination-area" v-if="filteredTotal > RECOMMENDATIONS_PER_PAGE">
              <a-pagination
                v-model:current="currentPage"
                :total="filteredTotal"
                :page-size="RECOMMENDATIONS_PER_PAGE"
                :show-size-changer="false"
                :hide-on-single-page="true"
                @change="handlePageChange"
                show-less-items
              />
            </div>
            <div v-if="!loadingRecommendations && displayedRecommendationList.length > 0" class="recommendation-regenerate-area">
              <span class="regenerate-text">想换一组更适合今天的资源？</span>
              <button class="empty-action regenerate-action" @click="openSurvey">{{ emptyRecommendationActionText }}</button>
            </div>
            <div v-if="!loadingRecommendations && displayedRecommendationList.length === 0" class="empty-course-state">
              {{ emptyRecommendationMessage }}
              <button class="empty-action" @click="openSurvey">{{ emptyRecommendationActionText }}</button>
            </div>
          </a-spin>
        </div>
      </section>

      <section class="layer-community">
        <LearningCommunitySection />
      </section>
    </main>


    <footer class="site-footer">
      <div class="container-1200">
        <div class="footer-main">
          <div class="footer-links">
            <dl>
              <dt>学习指南</dt>
              <dd><a href="#">新手入门</a></dd>
              <dd><a href="#">选课说明</a></dd>
              <dd><a href="#">学分与证书</a></dd>
            </dl>
            <dl>
              <dt>平台支持</dt>
              <dd><a href="#">设备环境要求</a></dd>
              <dd><a href="#">播放问题排查</a></dd>
              <dd><a href="#">学习资料下载</a></dd>
            </dl>
            <dl>
              <dt>互动交流</dt>
              <dd><a href="#">问答社区规范</a></dd>
              <dd><a href="#">学习小组申请</a></dd>
              <dd><a href="#">导师答疑预约</a></dd>
            </dl>
            <dl>
              <dt>关于平台</dt>
              <dd><a href="#">平台介绍</a></dd>
              <dd><a href="#">联系我们</a></dd>
              <dd><a href="#">意见反馈</a></dd>
            </dl>
            <dl>
              <dt>支持热线</dt>
              <dd><a href="#"><span class="iconfont icon-customer-service"></span> 在线学习辅导</a></dd>
              <dd><a href="#">客服电话 400-8888-000</a></dd>
              <dd><a href="#">工作时间 8:00-22:00</a></dd>
            </dl>
          </div>

          <div class="footer-qrcode">
            <div class="qr-item">
              <img :src="qrCodeImg" alt="微信公众号" />
              <p>关注官方公众号</p>
            </div>
          </div>
        </div>

        <div class="footer-copyright">
          <p class="bottom-links">
            <a href="#">关于平台</a> <span class="divider">|</span>
            <a href="#">学习支持</a> <span class="divider">|</span>
            <a href="#">隐私政策</a> <span class="divider">|</span>
            <a href="#">服务协议</a> <span class="divider">|</span>
            <a href="#">加入我们</a> <span class="divider">|</span>
            <a href="#">平台合作</a> <span class="divider">|</span>
            <a href="#">学习资源链接</a>
          </p>
          <p>Bright Scholars © 多模态智慧教育平台 保留所有权利</p>
        </div>
      </div>
    </footer>

  </div>
</template>

<script setup lang="ts">
import banner1 from '@/assets/images/banner1.jpg'
import banner2 from '@/assets/images/banner2.jpg'
import banner3 from '@/assets/images/banner3.jpg'
import qrCodeImg from '@/assets/images/qrcode.png'
import { ref, onMounted, onUnmounted, computed, shallowRef, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { message, Modal } from 'ant-design-vue'
import * as echarts from 'echarts'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

import {
  SearchOutlined,
  BellOutlined,
  CloseOutlined,
  RobotOutlined,
  SendOutlined,
  CalendarOutlined,
  ArrowUpOutlined,
  LeftOutlined,
  RightOutlined,
  ClockCircleOutlined,
  ScheduleOutlined // <-- 加上这个图标
} from '@ant-design/icons-vue'

import {
  getBannerList,
  getCategoryList,
  type PlatformBannerItem,
  type PlatformCategoryItem
} from '@/api/platform'
import { getStudyHeatmap } from '@/api/dashboard'
import request from '@/utils/request'
import { getAuthToken, getLoginUser, getLoginUserRaw } from '@/utils/authStorage'
import {
  searchInternalResources,
  type ResourceSearchItem,
  type ResourceType
} from '@/api/resource'
import {
  createPersonalPractice,
  fetchCachedTodayDailyRecommendation,
  reportLearningEvents,
  type DailyRecommendationToday,
  type StudentLearningProfile
} from '@/api/learning'
import { useUserStore } from '@/stores/user'
import LearningCommunitySection from './LearningCommunitySection.vue'
import GlobalFloatTools from '@/components/GlobalFloatTools.vue'

interface CategoryDisplayItem {
  id?: number
  name: string
  img: string
}

type RecommendationItem = StudentLearningProfile['recommendations'][number]
type RecommendationCategory = 'video' | 'text' | 'practice'

type TutorialCourseItem = {
  id: number
  name: string
  description?: string
  coverImg?: string
}

const MIN_RECOMMENDATIONS_PER_CATEGORY = 8
const DASHBOARD_RECOMMENDATION_CACHE_TTL = 2 * 60 * 1000
const DASHBOARD_RECOMMENDATION_CACHE_VERSION = 5

type DashboardRecommendationCache = {
  version?: number
  recommendationList: RecommendationItem[]
  todayRecommendationSubmitted: boolean
  cachedAt: number
}

let dashboardRecommendationCache: DashboardRecommendationCache | null = null
let dashboardRecommendationRequest: Promise<void> | null = null
let dashboardRecommendationRequestVersion = 0

type DashboardRecommendationCacheHost = Window & {
  __studentDashboardRecommendationCache?: DashboardRecommendationCache | null
  __studentDashboardRecommendationRequest?: Promise<void> | null
  __studentDashboardRecommendationRequestVersion?: number
}

const getDashboardRecommendationCacheHost = () =>
  window as unknown as DashboardRecommendationCacheHost

const RESOURCE_TYPE_BY_CATEGORY: Record<RecommendationCategory, ResourceType> = {
  video: 'video',
  text: 'plan',
  practice: 'quiz'
}

const CATEGORY_FALLBACK_TOPICS: Record<RecommendationCategory, Array<Pick<RecommendationItem, 'resourceTitle' | 'knowledgeName' | 'recommendationReason' | 'actionUrl'>>> = {
  video: [
    { resourceTitle: 'Python 基础视频课', knowledgeName: 'Python', recommendationReason: '适合补齐编程基础，先看核心语法与案例。', actionUrl: '/student/search?keyword=Python' },
    { resourceTitle: 'Java 入门视频课', knowledgeName: 'Java', recommendationReason: '从变量、流程控制到面向对象逐步建立知识框架。', actionUrl: '/student/search?keyword=Java' },
    { resourceTitle: '数据结构视频精讲', knowledgeName: '数据结构', recommendationReason: '用视频梳理数组、链表、栈、队列和树的关键概念。', actionUrl: '/student/search?keyword=数据结构' },
    { resourceTitle: '算法思维训练课', knowledgeName: '算法', recommendationReason: '通过典型题型理解递归、排序和搜索策略。', actionUrl: '/student/search?keyword=算法' },
    { resourceTitle: '前端开发视频课', knowledgeName: '前端', recommendationReason: '覆盖页面结构、交互和组件化开发的常用能力。', actionUrl: '/student/search?keyword=前端' },
    { resourceTitle: '数据库基础视频课', knowledgeName: '数据库', recommendationReason: '理解表、查询、索引和事务，为项目实践打底。', actionUrl: '/student/search?keyword=数据库' },
    { resourceTitle: '人工智能导论视频', knowledgeName: '人工智能', recommendationReason: '快速了解 AI 基础概念和常见应用场景。', actionUrl: '/student/search?keyword=人工智能' },
    { resourceTitle: '项目实战视频课', knowledgeName: '项目实战', recommendationReason: '用完整项目串联知识点，提升综合应用能力。', actionUrl: '/student/search?keyword=项目实战' }
  ],
  text: [
    { resourceTitle: 'Python 语法速查', knowledgeName: 'Python', recommendationReason: '用图文方式复盘常用语法，适合课后查漏补缺。', actionUrl: '/student/tutorial' },
    { resourceTitle: 'Java 面向对象笔记', knowledgeName: 'Java', recommendationReason: '梳理类、对象、继承和接口的关键区别。', actionUrl: '/student/tutorial' },
    { resourceTitle: '数据结构知识卡片', knowledgeName: '数据结构', recommendationReason: '用结构化笔记快速回顾核心概念。', actionUrl: '/student/tutorial' },
    { resourceTitle: '算法复杂度图文教程', knowledgeName: '算法复杂度', recommendationReason: '帮助判断代码效率，理解常见复杂度。', actionUrl: '/student/tutorial' },
    { resourceTitle: '前端基础阅读材料', knowledgeName: '前端', recommendationReason: '适合在练习前快速建立 HTML、CSS、JS 的知识框架。', actionUrl: '/student/tutorial' },
    { resourceTitle: '数据库 SQL 笔记', knowledgeName: 'SQL', recommendationReason: '集中复习查询、连接和聚合等高频知识点。', actionUrl: '/student/tutorial' },
    { resourceTitle: 'AI 基础概念手册', knowledgeName: 'AI', recommendationReason: '用短文档理解模型、数据和训练的基本关系。', actionUrl: '/student/tutorial' },
    { resourceTitle: '学习方法整理', knowledgeName: '学习规划', recommendationReason: '帮助安排复习节奏，减少低效重复学习。', actionUrl: '/student/tutorial' }
  ],
  practice: [
    { resourceTitle: 'Python 基础练习', knowledgeName: 'Python', recommendationReason: '通过小题巩固变量、判断和循环。', actionUrl: '/student/coding' },
    { resourceTitle: 'Java 基础练习', knowledgeName: 'Java', recommendationReason: '适合检验语法、类和方法的掌握情况。', actionUrl: '/student/coding' },
    { resourceTitle: '数组与字符串练习', knowledgeName: '数组与字符串', recommendationReason: '训练最常见的数据处理题型。', actionUrl: '/student/coding' },
    { resourceTitle: '排序与查找练习', knowledgeName: '排序与查找', recommendationReason: '练习二分、排序和边界处理能力。', actionUrl: '/student/coding' },
    { resourceTitle: 'SQL 查询练习', knowledgeName: 'SQL', recommendationReason: '巩固筛选、分组、排序和多表查询。', actionUrl: '/student/coding' },
    { resourceTitle: '前端交互练习', knowledgeName: '前端交互', recommendationReason: '通过小任务巩固 DOM、事件和状态变化。', actionUrl: '/student/coding' },
    { resourceTitle: '错题回顾练习', knowledgeName: '错题回顾', recommendationReason: '优先处理薄弱点，减少同类错误反复出现。', actionUrl: '/student/diagnosis' },
    { resourceTitle: '综合能力练习', knowledgeName: '综合练习', recommendationReason: '适合阶段复盘，检查多个知识点的综合应用。', actionUrl: '/student/coding' }
  ]
}

const router = useRouter()
const userStore = useUserStore()
const { userInfo, isCheckedIn, checkInLoading, dailyPlans, completedPlanCount } = storeToRefs(userStore)
const mergedUserInfo = computed(() => {
  const localUser = getLoginUser<any>() || {}
  return {
    ...localUser,
    ...(userInfo.value || {})
  }
})

const displayUserName = computed(() => {
  return mergedUserInfo.value?.userName || mergedUserInfo.value?.name || '同学'
})

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const SERVER_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const normalizeAssetUrl = (url?: string) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:image')) return url

  // 本地 public 静态资源保留原写法
  if (url.startsWith('/icons/') || url.startsWith('/course-covers/')) return url

  return `${SERVER_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`
}

const fallbackBannerList: PlatformBannerItem[] = [
  { title: '开启你的系统化学习之旅', imageUrl: banner1 },
  { title: '发现更适合你的知识路径', imageUrl: banner2 },
  { title: '让每一次学习都有方向', imageUrl: banner3 }
]

const fallbackCategoryList: PlatformCategoryItem[] = [
  { name: '编程', iconUrl: '/icons/types/编程.png' },
  { name: '算法', iconUrl: '/icons/types/算法.png' },
  { name: '前端', iconUrl: '/icons/types/前端.png' },
  { name: '后端', iconUrl: '/icons/types/后端.png' },
  { name: '数据', iconUrl: '/icons/types/数据.png' },
  { name: '运维', iconUrl: '/icons/types/运维.png' },
  { name: 'python', iconUrl: '/icons/types/python.png' },
  { name: 'java', iconUrl: '/icons/types/java.png' },
  { name: 'C', iconUrl: '/icons/types/C.png' },
  { name: 'AI', iconUrl: '/icons/types/AI.png' },
  { name: '设计', iconUrl: '/icons/types/设计.png' },
  { name: '职场', iconUrl: '/icons/types/职场.png' },
  { name: '心理', iconUrl: '/icons/types/心理.png' },
  { name: '多模态', iconUrl: '/icons/types/多模态.png' },
  { name: '阅读', iconUrl: '/icons/types/阅读.png' },
  { name: '人工智能', iconUrl: '/icons/types/人工智能.png' },
  { name: '深度学习', iconUrl: '/icons/types/深度学习.png' },
  { name: '机器学习', iconUrl: '/icons/types/机器学习.png' }
]

const bannerList = ref<PlatformBannerItem[]>([...fallbackBannerList])
const platformCategoryList = ref<PlatformCategoryItem[]>([])
const searchText = ref('')
const activeTab = ref('today')
const tabs = [
  { key: 'today', label: '今日推荐' },
  { key: 'all', label: '全部' },
  { key: 'video', label: '视频课程' },
  { key: 'text', label: '图文教程' },
  { key: 'practice', label: '练习任务' }
]
const TODAY_RECOMMENDATION_SOURCES = new Set([
  'daily_survey',
  'learning_history',
  'video_behavior',
  'exam_behavior',
  'homework_behavior'
])
const newPlanContent = ref('')

const mergedCategoryList = computed<CategoryDisplayItem[]>(() => {
  const source = platformCategoryList.value.length ? platformCategoryList.value : fallbackCategoryList
  return source.slice(0, 18).map((item) => ({
    id: item.id,
    name: item.name,
    img: normalizeAssetUrl(item.iconUrl)
  }))
})

const baseTypeData = computed(() => mergedCategoryList.value.slice(0, 9))
const excellentTypeData = computed(() => mergedCategoryList.value.slice(9, 18))

const loadPlatformAssets = async () => {
  try {
    const [bannerRes, categoryRes] = await Promise.all([
      getBannerList(),
      getCategoryList()
    ])

    if (Array.isArray(bannerRes) && bannerRes.length > 0) {
      bannerList.value = bannerRes.map((item) => ({
        id: item.id,
        title: item.title,
        imageUrl: normalizeAssetUrl(item.imageUrl),
        targetUrl: item.targetUrl
      }))
    } else {
      bannerList.value = [...fallbackBannerList]
    }

    if (Array.isArray(categoryRes) && categoryRes.length > 0) {
      platformCategoryList.value = categoryRes
        .filter((item) => item.name && item.iconUrl)
        .map((item) => ({
          id: item.id,
          name: item.name,
          iconUrl: normalizeAssetUrl(item.iconUrl),
          sortOrder: item.sortOrder,
          isEnabled: item.isEnabled
        }))
    } else {
      platformCategoryList.value = []
    }
  } catch (error) {
    console.warn('平台素材加载失败，已回退到本地默认素材', error)
    bannerList.value = [...fallbackBannerList]
    platformCategoryList.value = []
  }
}

const handleBannerClick = (item: PlatformBannerItem) => {
  if (!item?.targetUrl) return

  if (item.targetUrl.startsWith('http')) {
    window.open(item.targetUrl, '_blank')
    return
  }

  router.push(item.targetUrl)
}

const goToTypeFilter = (category: CategoryDisplayItem) => {
  router.push({
    path: '/student/search',
    query: {
      typeName: category.name,
      ...(category.id ? { categoryId: category.id } : {})
    }
  })
}

const greetingInfo = computed(() => {
  const hour = new Date().getHours()
  if (hour >= 5 && hour < 12) return { title: '早上好', sub: '一日之计在于晨，开启今天的新知旅程吧。' }
  if (hour >= 12 && hour < 14) return { title: '中午好', sub: '午休时间，记得放松一下眼睛。' }
  if (hour >= 14 && hour < 18) return { title: '下午好', sub: '保持专注，继续探索未知的领域。' }
  if (hour >= 18 && hour < 22) return { title: '晚上好', sub: '夜色渐深，总结一下今天的收获吧。' }
  return { title: '夜深了', sub: '还在努力学习吗？请注意休息，保护身体。' }
})

const displayAvatarUrl = computed(() => {
  const rawAvatar = userInfo.value?.avatar
  if (!rawAvatar) return 'https://api.dicebear.com/7.x/notionists/svg?seed=smart-edu'
  if (rawAvatar.startsWith('http') || rawAvatar.startsWith('data:image')) return rawAvatar
  return `${SERVER_BASE_URL}${rawAvatar.startsWith('/') ? rawAvatar : `/${rawAvatar}`}`
})

const getCover = (course: any) => {
  if (course && course.coverImg) {
    if (course.coverImg.startsWith('http') || course.coverImg.startsWith('data:image')) return course.coverImg
    return `${SERVER_BASE_URL}${course.coverImg.startsWith('/') ? course.coverImg : `/${course.coverImg}`}`
  }
  return `https://api.dicebear.com/7.x/shapes/svg?seed=${course?.id || Math.random()}&backgroundColor=F6F8FC`
}

const today = new Date()
const currentDay = today.getDate()

const heatmapChartRef = ref<HTMLElement | null>(null)
const heatmapChartInstance = shallowRef<echarts.ECharts | null>(null)
const calendarData = ref<any[]>([])

const formatDate = (date: Date | string | undefined, format = 'yyyy-MM-dd') => {
  if (!date) return ''
  const d = typeof date === 'string' ? new Date(date) : date
  const year = d.getFullYear()
  const month = d.getMonth() + 1
  const day = d.getDate()
  if (format === 'yyyy-MM-dd') {
    return `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`
  }
  return `${month}月${day}日`
}

/**
 * 1. 生成固定数据的函数
 * Legacy calendar generator kept inert; the dashboard uses backend heatmap data.
 */
const unusedLegacyCalendarData = () => {
  // 🚩 1. 你的原始“写死”数据
  const emptyLegacyData: Record<string, number> = {
    // --- 2025年 遗留记录 ---

    // --- 2026年 1-2月：蓄力期 (1-5h 随机，较多间隔) ---
    

    // --- 2026年 3-4月：冲刺期 (5-11h 随机，高密度) ---
  }

  const data: [string, number][] = []

  // 🚩 2. 计算起始和结束日期
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 6)

  // 🚩 3. 核心逻辑：遍历这 6 个月里的每一天
  const currentCursor = new Date(start)
  while (currentCursor <= end) {
    const dateStr = formatDate(currentCursor, 'yyyy-MM-dd')
    // No static study values are kept here.
    const value = emptyLegacyData[dateStr] !== undefined ? emptyLegacyData[dateStr] : 0

    data.push([dateStr, value])

    // 指针向后移动一天
    currentCursor.setDate(currentCursor.getDate() + 1)
  }

  calendarData.value = data
}

/**
 * 2. 渲染热力图的完整函数
 */
const getCurrentUserId = () => {
  try {
    const uStr = getLoginUserRaw()
    return uStr ? JSON.parse(uStr).id || null : null
  } catch (e) {
    return null
  }
}

const loadCalendarData = async () => {
  try {
    const days = await getStudyHeatmap(undefined, 180)
    calendarData.value = Array.isArray(days)
      ? days.map(day => [day.date, Number(day.minutes || 0)])
      : []
  } catch (error) {
    console.warn('加载学习热力图失败', error)
    calendarData.value = []
  }
}

const formatStudyMinutes = (minutes: number) => {
  const safeMinutes = Math.max(0, Math.round(Number(minutes) || 0))
  if (safeMinutes < 60) return `${safeMinutes} 分钟`
  const hours = Math.floor(safeMinutes / 60)
  const restMinutes = safeMinutes % 60
  return restMinutes > 0 ? `${hours} 小时 ${restMinutes} 分钟` : `${hours} 小时`
}

const initHeatmapChart = () => {
  if (!heatmapChartRef.value) return

  // 初始化实例
  heatmapChartInstance.value = echarts.init(heatmapChartRef.value)

  // 计算显示范围：当前日期往前推 6 个月
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 6)
  const range = [formatDate(start, 'yyyy-MM-dd'), formatDate(end, 'yyyy-MM-dd')]

  const option = {
    // 鼠标悬停时的提示框
    tooltip: {
      formatter: function (params: any) {
        return `<span style="color:#667085;font-size:12px">${params.value[0]}</span><br/>学习时长：<strong style="font-size:14px;color:#1F2937">${formatStudyMinutes(params.value[1])}</strong>`
      },
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: '#E7ECF3',
      borderRadius: 8,
      padding: [8, 12],
      shadowBlur: 10,
      shadowColor: 'rgba(0,0,0,0.05)'
    },

    visualMap: {
      min: 0,
      max: 240,
      type: 'piecewise',
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      itemWidth: 12,
      itemHeight: 12,
      textGap: 8,
      textStyle: { color: '#0b0a0a', fontSize: 12 },
      pieces: [
        // 🚩 无记录：使用标准的 GitHub 占位灰，确保框线可见
        { value: 0, label: '无记录', color: '#f6f2f2' },

        // 按分钟分段，避免 0.x 小时落不到颜色区间
        { min: 1, max: 15, label: '1-15min', color: '#DCFCE7' },
        { min: 16, max: 59, label: '15min-1h', color: '#BBF7D0' },
        { min: 60, max: 179, label: '1-3h', color: '#86EFAC' },
        { min: 180, max: 299, label: '3-5h', color: '#22C55E' },
        { min: 300, label: '5h+', color: '#15803D' }
      ]
    },

    // 日历坐标系配置
    calendar: {
      top: 30,
      left: 50,
      right: 20,
      range: range,
      cellSize: ['auto', 13],
      yearLabel: { show: false },

      // 1. 单元格样式：保持每个“小格子”之间的细微缝隙
      itemStyle: {
        borderWidth: 2,
        borderColor: '#ffffff' // 格子间的白线，模拟 GitHub 效果
      },

      // 2. 🌟 核心修改：月份分割线（让月份之间的框感瞬间出来）
      splitLine: {
        show: true,
        lineStyle: {
          color: '#D1D5DB',    // 分割线颜色：建议用中等灰色，比格子底色深
          width: 3,            // 🚩 核心：加粗线宽，让间隙变得极明显
          type: 'solid'
        }
      },

      dayLabel: {
        nameMap: ['日', '一', '二', '三', '四', '五', '六'],
        color: '#98A2B3',
        fontSize: 11
      },
      monthLabel: {
        nameMap: 'cn',
        color: '#1F2937',      // 月份文字颜色调深一点，呼应“框”
        fontWeight: 'bold',    // 加粗月份文字
        fontSize: 12,
        margin: 12
      }
    },

    // 数据系列
    series: {
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: calendarData.value // 绑定上面写死的数据
    }
  }

  heatmapChartInstance.value.setOption(option, true)
}
const recommendationList = ref<RecommendationItem[]>([])
const loadingRecommendations = ref(false)
const currentPage = ref(1)
const RECOMMENDATIONS_PER_PAGE = 8
const recommendationSectionRef = ref<HTMLElement | null>(null)
const todayRecommendationSubmitted = ref(false)
const DAILY_RECOMMENDATION_JUST_GENERATED_KEY = 'dailyRecommendationJustGenerated'

const isVideoRecommendation = (item: RecommendationItem) => {
  const value = String(item.resourceType || '').toLowerCase()
  return value.includes('video') || value.includes('micro')
}

const isTextRecommendation = (item: RecommendationItem) => {
  const value = String(item.resourceType || '').toLowerCase()
  return value === 'text' || value.includes('tutorial') || value.includes('plan')
}

const isPracticeRecommendation = (item: RecommendationItem) => {
  const value = String(item.resourceType || '').toLowerCase()
  return value.includes('quiz') || value.includes('practice') || value.includes('review') || value.includes('homework')
}

const isTodayRecommendation = (item: RecommendationItem) => {
  return TODAY_RECOMMENDATION_SOURCES.has(String(item.recommendationSource || '').toLowerCase())
}

const getRecommendationTabKey = (item: RecommendationItem) => {
  if (isVideoRecommendation(item)) return 'video'
  if (isTextRecommendation(item)) return 'text'
  if (isPracticeRecommendation(item)) return 'practice'
  return 'all'
}

const recommendationIdentity = (item: RecommendationItem) => {
  const category = getRecommendationTabKey(item)
  if (category === 'video' && item.courseId != null) {
    return `${category}:course:${item.courseId}`
  }
  const title = String(item.resourceTitle || item.courseName || item.knowledgeName || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '')
  if (title) return `${category}:${title}`
  return `${category}:${item.resourceType || ''}:${item.resourceId || item.id || ''}`
}

const buildResourceActionUrl = (category: RecommendationCategory, item: ResourceSearchItem) => {
  if (category === 'video') return `/learn/${item.id}`
  if (category === 'text') return '/student/tutorial'
  return '/student/coding'
}

const resourceToRecommendation = (
  item: ResourceSearchItem,
  category: RecommendationCategory,
  index: number
): RecommendationItem => ({
  id: -10_000 - index - (category === 'video' ? 0 : category === 'text' ? 1_000 : 2_000) - Number(item.id || 0),
  courseId: category === 'video' ? item.id : undefined,
  courseName: item.course || undefined,
  coverImg: item.cover || undefined,
  resourceId: item.id,
  resourceType: item.type || RESOURCE_TYPE_BY_CATEGORY[category],
  resourceTitle: item.title || item.course || '学习资源',
  knowledgeName: item.tags?.[0] || item.course || item.title || '综合补强',
  recommendationReason: item.desc || item.previewText || '从平台资源库中为你补充的学习资源。',
  practiceSuggestion: item.previewText || '',
  recommendationSource: 'resource_pool',
  status: 'pending',
  actionUrl: buildResourceActionUrl(category, item),
  actionLabel: category === 'video' ? '开始学习' : category === 'text' ? '阅读教程' : '开始练习',
  shortReason: item.desc || item.previewText || ''
})

const tutorialToRecommendation = (item: TutorialCourseItem, index: number): RecommendationItem => ({
  id: -20_000 - index - Number(item.id || 0),
  courseId: item.id,
  courseName: item.name,
  coverImg: item.coverImg,
  resourceId: item.id,
  resourceType: 'tutorial',
  resourceTitle: item.name || '图文教程',
  knowledgeName: item.name || '图文教程',
  recommendationReason: item.description || '来自学生端图文课程列表，适合用来课后阅读和查漏补缺。',
  practiceSuggestion: '',
  recommendationSource: 'tutorial',
  status: 'pending',
  actionUrl: `/student/tutorial/${item.id}/read`,
  actionLabel: '阅读教程',
  shortReason: item.description || '来自学生端图文课程列表。'
})

const topicToRecommendation = (
  item: Pick<RecommendationItem, 'resourceTitle' | 'knowledgeName' | 'recommendationReason' | 'actionUrl'>,
  category: RecommendationCategory,
  index: number
): RecommendationItem => ({
  id: -50_000 - index - (category === 'video' ? 0 : category === 'text' ? 1_000 : 2_000),
  resourceId: undefined,
  resourceType: category === 'video' ? 'video' : category === 'text' ? 'plan' : 'quiz',
  resourceTitle: item.resourceTitle,
  knowledgeName: item.knowledgeName,
  recommendationReason: item.recommendationReason,
  practiceSuggestion: '',
  recommendationSource: 'fallback',
  status: 'pending',
  actionUrl: item.actionUrl,
  actionLabel: category === 'video' ? '查找课程' : category === 'text' ? '阅读教程' : '开始练习',
  shortReason: item.recommendationReason
})

const fetchTutorialFallbackRecommendations = async () => {
  try {
    const tutorials = await request.get<TutorialCourseItem[], TutorialCourseItem[]>('/tutorial/list', {
      skipErrorToast: true
    })
    return (tutorials || [])
      .slice()
      .sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
      .map((item, index) => tutorialToRecommendation(item, index))
  } catch (error) {
    console.warn('加载学生端图文教程兜底失败', error)
    return []
  }
}

const fetchResourceFallbackRecommendations = async () => {
  const [entries, textItems] = await Promise.all([
    Promise.allSettled(
    (['video', 'practice'] as RecommendationCategory[]).map(async (category) => {
      const page = await searchInternalResources({
        current: 1,
        pageSize: MIN_RECOMMENDATIONS_PER_CATEGORY,
        type: RESOURCE_TYPE_BY_CATEGORY[category],
        sortMode: 'newest'
      })
      return [
        category,
        (page.records || []).map((item, index) => resourceToRecommendation(item, category, index))
      ] as const
    })
    ),
    fetchTutorialFallbackRecommendations()
  ])

  return entries.reduce((acc, result) => {
    if (result.status === 'fulfilled') {
      const [category, items] = result.value
      acc[category] = items
    }
    return acc
  }, { video: [], text: textItems, practice: [] } as Record<RecommendationCategory, RecommendationItem[]>)
}

const fillCategoryRecommendations = (
  items: RecommendationItem[],
  category: RecommendationCategory,
  resourceFallbacks: RecommendationItem[]
) => {
  const result = [...items]
  const seen = new Set(result.map(recommendationIdentity))
  const appendUnique = (item: RecommendationItem) => {
    const key = recommendationIdentity(item)
    if (seen.has(key)) return
    seen.add(key)
    result.push(item)
  }

  resourceFallbacks.forEach(appendUnique)
  CATEGORY_FALLBACK_TOPICS[category]
    .map((item, index) => topicToRecommendation(item, category, index))
    .forEach(appendUnique)

  return result.slice(0, Math.max(result.length, MIN_RECOMMENDATIONS_PER_CATEGORY))
}

const ensureRecommendationMinimums = (
  items: RecommendationItem[],
  resourceFallbacks: Record<RecommendationCategory, RecommendationItem[]>
) => {
  const byCategory: Record<RecommendationCategory, RecommendationItem[]> = {
    video: [],
    text: [],
    practice: []
  }
  const uncategorized: RecommendationItem[] = []

  items.forEach((item) => {
    const category = getRecommendationTabKey(item)
    if (category === 'video' || category === 'text' || category === 'practice') {
      byCategory[category].push(item)
    } else {
      uncategorized.push(item)
    }
  })

  const filled = (Object.keys(byCategory) as RecommendationCategory[])
    .flatMap((category) => fillCategoryRecommendations(byCategory[category], category, resourceFallbacks[category]))

  return mergeRecommendations([...filled, ...uncategorized])
}

const fillRecommendationListToPage = (items: RecommendationItem[]) => {
  // “今日推荐”必须忠实展示后端按今日主题生成的结果，不能从全量资源池补位。
  if (activeTab.value === 'today') return items
  if (items.length >= RECOMMENDATIONS_PER_PAGE) return items

  const seen = new Set(items.map(recommendationIdentity))
  const supplemental = recommendationList.value
    .filter((item) => !seen.has(recommendationIdentity(item)))
    .sort((a, b) => {
      const fallbackDelta = Number(a.recommendationSource === 'fallback') - Number(b.recommendationSource === 'fallback')
      if (fallbackDelta !== 0) return fallbackDelta
      return Number(isTodayRecommendation(b)) - Number(isTodayRecommendation(a))
    })
    .slice(0, RECOMMENDATIONS_PER_PAGE - items.length)

  return [...items, ...supplemental]
}

const mergeRecommendations = (items: RecommendationItem[]) => {
  const seen = new Set<string>()
  return items
    .filter((item) => item && (item.id || item.resourceTitle || item.knowledgeName))
    .sort((a, b) => {
      const videoDelta = Number(isVideoRecommendation(b)) - Number(isVideoRecommendation(a))
      if (videoDelta !== 0) return videoDelta
      const dailyDelta = Number(b.recommendationSource === 'daily_survey') - Number(a.recommendationSource === 'daily_survey')
      if (dailyDelta !== 0) return dailyDelta
      return Number(a.status === 'completed') - Number(b.status === 'completed')
    })
    .filter((item) => {
      const key = recommendationIdentity(item)
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
}

const filteredRecommendationList = computed(() => {
  let items: RecommendationItem[]
  if (activeTab.value === 'today') {
    items = recommendationList.value.filter(isTodayRecommendation)
  } else if (activeTab.value === 'all') {
    items = recommendationList.value
  } else {
    items = recommendationList.value.filter((item) => getRecommendationTabKey(item) === activeTab.value)
  }
  return mergeRecommendations(fillRecommendationListToPage(items))
})

const filteredTotal = computed(() => filteredRecommendationList.value.length)
const hasTodayRecommendationRecords = computed(() => recommendationList.value.some(isTodayRecommendation))

const emptyRecommendationMessage = computed(() => {
  if (activeTab.value === 'today' && todayRecommendationSubmitted.value) {
    if (hasTodayRecommendationRecords.value) {
      return '今天的推荐已经处理完了，当前没有新的待展示资源。'
    }
    return '今日设定已保存，但暂未匹配到真实的临期作业或学习资源。可以换一个关键词或课程后重新生成。'
  }
  if (activeTab.value === 'today') {
    return '暂无今日推荐，先完成今日推荐问答。'
  }
  return '暂无个性化推荐，先完成今日推荐问答。'
})

const emptyRecommendationActionText = computed(() => (
  todayRecommendationSubmitted.value ? '重新生成推荐' : '生成今日推荐'
))

const pagedRecommendationList = computed(() => {
  const start = (currentPage.value - 1) * RECOMMENDATIONS_PER_PAGE
  const end = start + RECOMMENDATIONS_PER_PAGE
  return filteredRecommendationList.value.slice(start, end)
})

const displayedRecommendationList = computed(() => (
  mergeRecommendations(pagedRecommendationList.value).slice(0, RECOMMENDATIONS_PER_PAGE)
))

const recommendationSpinVisible = computed(() => (
  loadingRecommendations.value && displayedRecommendationList.value.length === 0
))

watch(activeTab, () => {
  currentPage.value = 1
})

watch(filteredTotal, (total) => {
  const maxPage = Math.max(1, Math.ceil(total / RECOMMENDATIONS_PER_PAGE))
  if (currentPage.value > maxPage) {
    currentPage.value = maxPage
  }
})

const applyRecommendationCache = (cache: DashboardRecommendationCache) => {
  recommendationList.value = cache.recommendationList
  todayRecommendationSubmitted.value = cache.todayRecommendationSubmitted
}

const writeRecommendationCache = (cache: DashboardRecommendationCache) => {
  const cacheHost = getDashboardRecommendationCacheHost()
  const versionedCache = {
    ...cache,
    version: DASHBOARD_RECOMMENDATION_CACHE_VERSION
  }
  dashboardRecommendationCache = versionedCache
  cacheHost.__studentDashboardRecommendationCache = versionedCache
}

const applyTodayRecommendationResult = (today?: DailyRecommendationToday | null) => {
  if (!today) return
  const todayItems = today.recommendations || []
  todayRecommendationSubmitted.value = today.status === 'completed'
  recommendationList.value = ensureRecommendationMinimums(
    mergeRecommendations(todayItems),
    { video: [], text: [], practice: [] }
  )
  writeRecommendationCache({
    recommendationList: recommendationList.value,
    todayRecommendationSubmitted: todayRecommendationSubmitted.value,
    cachedAt: Date.now()
  })
}

const fetchPersonalizedRecommendations = async (options: { force?: boolean } = {}) => {
  const cacheHost = getDashboardRecommendationCacheHost()
  if (options.force) {
    dashboardRecommendationCache = null
    cacheHost.__studentDashboardRecommendationCache = null
  }
  const cached = cacheHost.__studentDashboardRecommendationCache || dashboardRecommendationCache
  if (
    !options.force &&
    cached &&
    cached.version === DASHBOARD_RECOMMENDATION_CACHE_VERSION &&
    Date.now() - cached.cachedAt < DASHBOARD_RECOMMENDATION_CACHE_TTL
  ) {
    applyRecommendationCache(cached)
    loadingRecommendations.value = false
    return
  }

  const pendingRequest = cacheHost.__studentDashboardRecommendationRequest || dashboardRecommendationRequest
  if (!options.force && pendingRequest) {
    loadingRecommendations.value = recommendationList.value.length === 0
    try {
      await pendingRequest
      const latestCache = cacheHost.__studentDashboardRecommendationCache || dashboardRecommendationCache
      if (latestCache) {
        applyRecommendationCache(latestCache)
      }
    } finally {
      loadingRecommendations.value = false
    }
    return
  }

  loadingRecommendations.value = true
  const requestVersion = Math.max(
    dashboardRecommendationRequestVersion,
    cacheHost.__studentDashboardRecommendationRequestVersion || 0
  ) + 1
  dashboardRecommendationRequestVersion = requestVersion
  cacheHost.__studentDashboardRecommendationRequestVersion = requestVersion
  const isLatestRequest = () => cacheHost.__studentDashboardRecommendationRequestVersion === requestVersion

  const requestPromise = (async () => {
  try {
    const todayResult = await fetchCachedTodayDailyRecommendation()
    if (!isLatestRequest()) return
    const todayItems = todayResult?.recommendations || []
    todayRecommendationSubmitted.value = todayResult?.status === 'completed'
    recommendationList.value = ensureRecommendationMinimums(
      mergeRecommendations(todayItems),
      { video: [], text: [], practice: [] }
    )
    writeRecommendationCache({
      recommendationList: recommendationList.value,
      todayRecommendationSubmitted: todayRecommendationSubmitted.value,
      cachedAt: Date.now()
    })
    fetchResourceFallbackRecommendations()
      .then((resourceFallbacks) => {
        if (!isLatestRequest()) return
        recommendationList.value = ensureRecommendationMinimums(
          mergeRecommendations(todayItems),
          resourceFallbacks
        )
        writeRecommendationCache({
          recommendationList: recommendationList.value,
          todayRecommendationSubmitted: todayRecommendationSubmitted.value,
          cachedAt: Date.now()
        })
      })
      .catch((fallbackError) => {
        console.warn('加载推荐兜底资源失败', fallbackError)
      })
  } catch (error) {
    if (!isLatestRequest()) return
    console.error('加载个性化推荐失败', error)
    recommendationList.value = ensureRecommendationMinimums([], { video: [], text: [], practice: [] })
  } finally {
    if (isLatestRequest()) {
      dashboardRecommendationRequest = null
      cacheHost.__studentDashboardRecommendationRequest = null
      loadingRecommendations.value = false
    }
  }
  })()

  dashboardRecommendationRequest = requestPromise
  cacheHost.__studentDashboardRecommendationRequest = requestPromise
  await requestPromise
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  scrollToRecommendationSection()
}

const handleSearch = () => {
  const keyword = searchText.value.trim()

  if (!keyword) {
    message.warning('请输入要搜索的课程关键词')
    return
  }

  router.push({
    path: '/student/search',
    query: { keyword }
  })
}

const fetchHomeworkTasks = () => {}

const isTimerModalVisible = ref(false)
const timerHours = ref(0)
const timerMinutes = ref(25)
const timerMessage = ref('专注时间结束，喝杯水休息一下吧！')
const isTimerRunning = ref(false)
let timerId: ReturnType<typeof setTimeout> | null = null

const startTimer = () => {
  const totalMinutes = (timerHours.value || 0) * 60 + (timerMinutes.value || 0)
  if (totalMinutes <= 0) {
    message.warning('专注时间不能为 0 哦')
    return
  }
  isTimerRunning.value = true
  isTimerModalVisible.value = false
  message.success(`专注模式已开启，将在 ${totalMinutes} 分钟后提醒您`)

  if (timerId) clearTimeout(timerId)
  timerId = setTimeout(() => {
    isTimerRunning.value = false
    Modal.info({
      title: '⏰ 专注完成',
      content: timerMessage.value || '专注结束啦，快活动一下筋骨吧！',
      okText: '知道了',
      centered: true,
      maskClosable: true
    })
  }, totalMinutes * 60 * 1000)
}

const cancelTimer = () => {
  if (timerId) clearTimeout(timerId)
  isTimerRunning.value = false
  message.success('已结束当前专注')
}

const goToProfile = () => router.push('/student/profile')
const goToLearningProfile = () => router.push('/student/diagnosis')
const openSurvey = () => {
  window.dispatchEvent(new CustomEvent('open-daily-recommendation'))
}

const scrollToRecommendationSection = () => {
  nextTick(() => {
    recommendationSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

const handleDailyRecommendationUpdated = async (event?: Event) => {
  sessionStorage.removeItem(DAILY_RECOMMENDATION_JUST_GENERATED_KEY)
  activeTab.value = 'today'
  currentPage.value = 1
  todayRecommendationSubmitted.value = true
  const today = (event as CustomEvent<{ today?: DailyRecommendationToday | null }> | undefined)?.detail?.today
  applyTodayRecommendationResult(today)
  await fetchPersonalizedRecommendations({ force: true })
  scrollToRecommendationSection()
}
const resourceTypeLabel = (type?: string) => {
  if (String(type || '').toLowerCase().includes('homework')) return '待完成作业'
  if (isTextRecommendation({ resourceType: type } as RecommendationItem)) return '图文教程'
  if (String(type || '').toLowerCase().includes('micro')) return '微课视频'
  if (isVideoRecommendation({ resourceType: type } as RecommendationItem)) return '视频课程'
  if (isPracticeRecommendation({ resourceType: type } as RecommendationItem)) return '练习任务'
  return '学习资源'
}

const recommendationSourceLabel = (source?: string) => {
  const value = String(source || '').toLowerCase()
  if (value === 'daily_survey') return '今日问答'
  if (value === 'learning_history') return '学习历史'
  if (value === 'video_behavior') return '视频行为'
  if (value === 'exam_behavior') return '考试表现'
  if (value === 'homework_behavior') return '作业表现'
  if (value === 'profile') return '学习画像'
  if (value === 'resource_pool') return '资源库'
  if (value === 'tutorial') return '图文课程'
  if (value === 'fallback') return '推荐入口'
  return ''
}

const failedRecommendationCoverKeys = ref<Set<string>>(new Set())

const markRecommendationCoverFailed = (item: RecommendationItem) => {
  const next = new Set(failedRecommendationCoverKeys.value)
  next.add(recommendationIdentity(item))
  failedRecommendationCoverKeys.value = next
}

const getRecommendationCover = (item: RecommendationItem) => {
  if (failedRecommendationCoverKeys.value.has(recommendationIdentity(item))) return ''
  const rawCover = item.coverImg
  if (rawCover) return normalizeAssetUrl(rawCover)
  return ''
}

type RecommendationCoverTheme = {
  className: string
  badge: string
  symbol: string
  title: string
}

const getRecommendationSearchText = (item: RecommendationItem) => {
  return [
    item.resourceTitle,
    item.courseName,
    item.knowledgeName,
    item.recommendationReason,
    item.practiceSuggestion,
    item.resourceType
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

const getRecommendationCoverTheme = (item: RecommendationItem): RecommendationCoverTheme => {
  const text = getRecommendationSearchText(item)
  if (text.includes('python')) {
    return { className: 'cover-python', badge: 'PY', symbol: 'def', title: 'Python 基础' }
  }
  if (text.includes('java')) {
    return { className: 'cover-java', badge: 'JVM', symbol: 'class', title: 'Java 训练' }
  }
  if (text.includes('sql') || text.includes('数据库') || text.includes('查询')) {
    return { className: 'cover-sql', badge: 'SQL', symbol: 'SELECT', title: '查询练习' }
  }
  if (text.includes('数组') || text.includes('字符串')) {
    return { className: 'cover-array', badge: 'DS', symbol: 'A[i]', title: '数组与字符串' }
  }
  if (text.includes('排序') || text.includes('查找') || text.includes('二分')) {
    return { className: 'cover-algo', badge: 'ALG', symbol: 'O(n)', title: '排序与查找' }
  }
  if (text.includes('前端') || text.includes('dom') || text.includes('html') || text.includes('css')) {
    return { className: 'cover-frontend', badge: 'WEB', symbol: '</>', title: '前端交互' }
  }
  if (text.includes('错题') || text.includes('回顾')) {
    return { className: 'cover-review', badge: 'FIX', symbol: '✓?', title: '错题回顾' }
  }
  if (text.includes('栈') || text.includes('队列') || text.includes('数据结构')) {
    return { className: 'cover-structure', badge: 'DS', symbol: 'S/Q', title: '数据结构' }
  }
  if (isVideoRecommendation(item)) {
    return { className: 'cover-video', badge: 'VIDEO', symbol: 'PLAY', title: '视频课程' }
  }
  if (isTextRecommendation(item)) {
    return { className: 'cover-text', badge: 'NOTE', symbol: 'DOC', title: '图文教程' }
  }
  return { className: 'cover-practice', badge: 'TASK', symbol: 'LAB', title: '练习巩固' }
}

const getRecommendationCoverMarks = (item: RecommendationItem) => {
  const text = getRecommendationSearchText(item)
  if (text.includes('python')) return ['变量', '判断', '循环']
  if (text.includes('java')) return ['类', '方法', '对象']
  if (text.includes('sql') || text.includes('数据库') || text.includes('查询')) return ['SELECT', 'JOIN', 'GROUP']
  if (text.includes('数组') || text.includes('字符串')) return ['索引', '切片', '匹配']
  if (text.includes('排序') || text.includes('查找') || text.includes('二分')) return ['二分', '排序', '边界']
  if (text.includes('前端') || text.includes('dom') || text.includes('html') || text.includes('css')) return ['DOM', '事件', '状态']
  if (text.includes('错题') || text.includes('回顾')) return ['定位', '订正', '复盘']
  if (text.includes('栈') || text.includes('队列') || text.includes('数据结构')) return ['栈', '队列', '操作']
  if (isVideoRecommendation(item)) return ['观看', '理解', '记录']
  if (isTextRecommendation(item)) return ['阅读', '标注', '总结']
  return ['练习', '反馈', '巩固']
}

const displayRecommendationTitle = (item: RecommendationItem) => {
  return item.resourceTitle || item.courseName || item.knowledgeName || '学习资源'
}

const shouldCreatePersonalPractice = (item: RecommendationItem) => {
  const resourceType = String(item.resourceType || '').toLowerCase()
  const actionUrl = String(item.actionUrl || '').toLowerCase()
  return isPracticeRecommendation(item)
    && !resourceType.includes('homework')
    && !actionUrl.startsWith('/student/homework/')
}

const getRecommendationChapterId = (item: RecommendationItem) => {
  return (item as RecommendationItem & { chapterId?: number | string | null }).chapterId ?? null
}

const goToRecommendation = async (item: RecommendationItem) => {
  reportLearningEvents([{
    eventType: 'resource_click',
    resourceId: item.resourceId ?? item.id,
    resourceType: item.resourceType || 'recommendation',
    knowledgeName: item.knowledgeName,
    extraJson: JSON.stringify({ recommendationId: item.id, source: item.recommendationSource || 'dashboard' })
  }]).catch(() => {})

  if (shouldCreatePersonalPractice(item)) {
    const knowledgeName = item.knowledgeName || item.resourceTitle || item.courseName || '综合练习'
    const chapterId = getRecommendationChapterId(item)
    const loadingKey = 'personal-practice-create'
    message.loading({ content: '正在生成个性化练习任务...', key: loadingKey, duration: 0 })
    try {
      const practice = await createPersonalPractice({
        courseId: item.courseId ?? null,
        chapterId,
        knowledgeName
      })
      message.success({
        content: `${practice.sourceLabel || '个性化题库'}已就绪，共 ${practice.questionCount || 1} 题`,
        key: loadingKey,
        duration: 1.5
      })
      reportLearningEvents([{
        eventType: 'practice_start',
        courseId: item.courseId,
        chapterId,
        resourceId: practice.assignmentId,
        resourceType: 'personal_practice',
        knowledgeName,
        extraJson: JSON.stringify({
          recommendationId: item.id,
          sourceType: practice.sourceType,
          questionCount: practice.questionCount
        })
      }]).catch(() => {})
      router.push(`/student/homework/${practice.assignmentId}`)
    } catch (error) {
      console.error('创建个性化练习任务失败', error)
      message.error({ content: '练习任务生成失败，请稍后再试', key: loadingKey, duration: 2 })
    }
    return
  }

  const fallbackKeyword = encodeURIComponent(item.knowledgeName || item.resourceTitle || '')
  const targetUrl = item.actionUrl || `/student/search?keyword=${fallbackKeyword}`
  if (/^https?:\/\//i.test(targetUrl)) {
    window.open(targetUrl, '_blank')
    return
  }
  router.push(targetUrl)
}

const goToHomework = (hw: any) => {
  message.loading(`正在进入作业...`, 0.5)
  setTimeout(() => router.push(`/student/homework/${hw.taskId}`), 500)
}

const handleAddPlan = () => {
  const content = newPlanContent.value.trim()
  if (!content) return
  if (userStore.addPlan) userStore.addPlan(content)
  newPlanContent.value = ''
}


const ENABLE_WEEKLY_FOCUS_DEMO = true

const DEMO_WEEKLY_FOCUS = {
  labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  data: [1.5, 2.1, 1.7, 2.8, 3.4, 2.6, 3.0]
}

const formatWeekdayLabel = (dateText: string, fallback = '') => {
  const date = new Date(`${dateText}T00:00:00`)
  if (Number.isNaN(date.getTime())) return fallback
  return new Intl.DateTimeFormat('zh-CN', { weekday: 'short' }).format(date)
}

const shouldUseDemoWeekData = (data: number[]) => {
  return !Array.isArray(data) || data.length === 0 || data.every(item => Number(item || 0) === 0)
}

const chartRef = ref<HTMLElement | null>(null)
const chartInstance = shallowRef<echarts.ECharts | null>(null)

const initChart = async () => {
  if (!chartRef.value) return

  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
  chartInstance.value = echarts.init(chartRef.value)

  let weekLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  let weekData = [0, 0, 0, 0, 0, 0, 0]

  try {
    let userId: number | null = null
    try {
      const uStr = getLoginUserRaw()
      if (uStr) userId = JSON.parse(uStr).id || null
    } catch (e) {
      // ignore
    }

    if (userId) {
      const days = await getStudyHeatmap(userId, 7)
      if (Array.isArray(days) && days.length > 0) {
        weekLabels = days.map((d: any, index: number) => formatWeekdayLabel(d.date, weekLabels[index] || ''))
        weekData = days.map((d: any) => Number(d.hours || 0))
      }
    }
  } catch (e) {
    console.warn('获取学习时长失败，准备使用演示数据')
  }

  // 只有接口空数据/全 0 时，才使用演示假数据
  if (ENABLE_WEEKLY_FOCUS_DEMO && shouldUseDemoWeekData(weekData)) {
    weekLabels = [...DEMO_WEEKLY_FOCUS.labels]
    weekData = [...DEMO_WEEKLY_FOCUS.data]
  }

  const yMax = Math.max(2, ...weekData.map((v: number) => Math.ceil(v + 0.5)))
  const chartPrimaryColor = '#2563EB'

  const option = {
    grid: { top: 30, bottom: 25, left: 38, right: 10 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: '#E7ECF3',
      borderRadius: 8,
      padding: [8, 12],
      textStyle: { color: '#1F2937', fontSize: 13 },
      formatter: (params: any) => {
        const item = Array.isArray(params) ? params[0] : params
        return `<span style="color:#667085;font-size:12px">${item?.axisValue || ''}</span><br/><strong style="font-size:14px">${item?.data ?? 0}</strong> 小时`
      }
    },
    xAxis: {
      type: 'category',
      data: weekLabels,
      axisLine: { lineStyle: { color: '#E7ECF3' } },
      axisTick: { show: false },
      axisLabel: { color: '#667085', fontSize: 11, margin: 10, interval: 0 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: yMax,
      splitLine: { lineStyle: { color: '#F3F4F6', type: 'dashed' } },
      axisLabel: { color: '#98A2B3', fontSize: 11, formatter: '{value} h' }
    },
    series: [
      {
        data: weekData,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: false,
        itemStyle: {
          color: chartPrimaryColor,
          borderColor: '#fff',
          borderWidth: 2
        },
        lineStyle: {
          color: chartPrimaryColor,
          width: 3
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.15)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.01)' }
          ])
        }
      }
    ]
  }

  chartInstance.value.setOption(option, true)
}

const handleResize = () => {
  chartInstance.value?.resize()
  heatmapChartInstance.value?.resize()
}

onMounted(() => {
  if (userStore.loadLocalData) userStore.loadLocalData()
  if (userStore.fetchFreshUserInfo) userStore.fetchFreshUserInfo()
  if (userStore.fetchCheckInStatus) userStore.fetchCheckInStatus()

  loadPlatformAssets()
  window.addEventListener('daily-recommendation-updated', handleDailyRecommendationUpdated)

  const shouldFocusRecommendation = sessionStorage.getItem(DAILY_RECOMMENDATION_JUST_GENERATED_KEY) === '1'
  if (shouldFocusRecommendation) {
    sessionStorage.removeItem(DAILY_RECOMMENDATION_JUST_GENERATED_KEY)
    handleDailyRecommendationUpdated()
  } else {
    fetchPersonalizedRecommendations()
  }

  setTimeout(() => {
    initChart()
    loadCalendarData().finally(() => initHeatmapChart())
  }, 100)

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('daily-recommendation-updated', handleDailyRecommendationUpdated)
  chartInstance.value?.dispose()
  heatmapChartInstance.value?.dispose()
})

const isAiOpen = ref(false)
const isAiTyping = ref(false)
const userQuery = ref('')
const msgContainer = ref<HTMLElement | null>(null)
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' + hljs.highlight(str, { language: lang, ignoreIllegals: true }).value + '</code></pre>'
      } catch (__) {
        // ignore
      }
    }
    return ''
  }
})

const chatHistory = ref([
  { role: 'ai', content: '你好！我是你的智能学习助理。无论是知识点解析、代码调试，还是学习规划，都可以随时问我。' }
])

const toggleAi = () => {
  isAiOpen.value = !isAiOpen.value
  if (isAiOpen.value) scrollBottom()
}

const scrollBottom = () => {
  nextTick(() => {
    if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  })
}

const sendAiMessage = async () => {
  const q = userQuery.value.trim()
  if (!q) return
  userQuery.value = ''
  chatHistory.value.push({ role: 'user', content: q })
  scrollBottom()
  const idx = chatHistory.value.push({ role: 'ai', content: '' }) - 1
  isAiTyping.value = true
  try {
    const token = getAuthToken()
    const res = await fetch(`${API_BASE_URL}/ai/stream`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ question: q, type: 'chat' })
    })
    const reader = res.body?.getReader()
    const decoder = new TextDecoder('utf-8')
    isAiTyping.value = false
    if (!reader) throw new Error('no reader')
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      chatHistory.value[idx].content += decoder.decode(value, { stream: true })
      scrollBottom()
    }
  } catch {
    chatHistory.value[idx].content = '网络开了小差，请稍后再试。'
    isAiTyping.value = false
  }
}
</script>

<style scoped>
:root {
  --primary-color: #2563EB;
  --primary-hover: #1D4ED8;
  --bg-page: #F6F8FC;
  --bg-card: #FFFFFF;
  --bg-sub: #F8FAFD;
  --text-main: #1F2937;
  --text-regular: #344054;
  --text-sub: #667085;
  --text-light: #98A2B3;
  --border-color: #E7ECF3;
  --radius-xl: 12px;
  --radius-lg: 8px;
  --radius-md: 5px;
  --radius-sm: 3px;
  --shadow-sm: 0 2px 8px rgba(15, 23, 42, 0.04);
  --shadow-md: 0 8px 24px rgba(15, 23, 42, 0.06);
  --shadow-lg: 0 16px 48px rgba(15, 23, 42, 0.08);
}

.page {
  min-height: 100vh;
  background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
  color: var(--text-regular, #344054);
}



.main-content-wrapper {
  width: 100%;
  background: transparent;
}

.layer-welcome {
  background: transparent;
  padding: 24px 0 16px;
}

.layer-dashboard {
  background: transparent;
  padding: 16px 0 32px;
  border-bottom: none;
}

.layer-bottom {
  background: transparent;
  margin-top: 16px;
  padding: 40px 0 46px;
  border-top: 1px solid var(--border-color, #E7ECF3);
}

.layer-community {
  background: transparent;
  padding: 12px 0 58px;
}

.layer-exam-history {
  background: transparent;
  padding: 40px 0 20px;
}

.exam-history-card {
  background: var(--bg-card, #FFFFFF);
  border: 1px solid var(--border-color, #E7ECF3);
  border-radius: var(--radius-lg, 8px);
  padding: 16px;
}

.container-1200 {
  width: 75%;
  max-width: 1600px;
  min-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.welcome-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0px;
  padding: 0 8px;
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.greeting-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-main, #1F2937);
  margin: 0;
  line-height: 1.2;
  letter-spacing: 0.5px;
}

.greeting-sub {
  font-size: 15px;
  color: var(--text-sub, #667085);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.hero-search-box {
  display: flex;
  align-items: center;
  width: 280px; /* 👈 将原先的 380px 修改为 280px */
  height: 48px;
  background: var(--bg-card, #FFF);
  border-radius: 99px;
  border: 1px solid var(--border-color, #E7ECF3);
  box-shadow: var(--shadow-sm);
  transition: all 0.2s;
  padding: 0 6px 0 16px;
  overflow: hidden;
}

.hero-search-box:focus-within {
  border-color: #93C5FD;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.search-icon {
  color: var(--text-light, #98A2B3);
  font-size: 16px;
}

.hero-search-box input {
  flex: 1;
  min-width: 0; /* 👈 新增：打破输入框默认最小宽度限制，使其能够平滑收缩 */
  border: none;
  padding: 0 12px;
  font-size: 14px;
  outline: none;
  background: transparent;
  color: var(--text-main);
}

.hero-search-box input::placeholder {
  color: var(--text-light, #98A2B3);
}

.btn-hero-search {
  flex-shrink: 0; /* 👈 新增：禁止按钮在空间不足时被挤压 */
  height: 36px;
  padding: 0 20px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  background: var(--primary-color, #2563EB);
  color: #fff;
  border-radius: 99px;
  transition: background 0.2s;
}

.btn-hero-search:hover {
  background: var(--primary-hover, #1D4ED8);
}

.card {
  background: var(--bg-card, #FFFFFF);
  border: 1px solid var(--border-color, #E7ECF3);
  border-radius: var(--radius-lg, 8px);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.3s;
}

.card:hover {
  box-shadow: var(--shadow-md);
}

.card-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main, #1F2937);
  margin: 0;
}

.banner-wrapper :deep(.ant-carousel) {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

:deep(.ant-carousel .slick-slider),
:deep(.ant-carousel .slick-list),
:deep(.ant-carousel .slick-track),
:deep(.ant-carousel .slick-slide),
:deep(.ant-carousel .slick-slide > div) {
  height: 100% !important;
  min-height: 0;
}

.carousel-slide {
  height: 100% !important;
  outline: none;
  display: block !important;
  position: relative;
}

.carousel-slide.clickable {
  cursor: pointer;
}

.banner-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  border-radius: 5px !important;
}

.banner-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 40px 32px 32px;
  background: linear-gradient(to top, rgba(15, 23, 42, 0.8) 0%, rgba(15, 23, 42, 0) 100%);
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.banner-tag {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  color: #FFF;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
  font-weight: 500;
}

.banner-title {
  color: #FFF;
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

:deep(.ant-carousel .slick-dots) {
  bottom: 16px;
  justify-content: flex-end;
  padding-right: 32px;
}

:deep(.ant-carousel .slick-dots li button) {
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.4);
}

:deep(.ant-carousel .slick-dots li.slick-active button) {
  width: 24px;
  background: #fff;
}

:deep(.ant-carousel .slick-prev),
:deep(.ant-carousel .slick-next) {
  display: none !important;
}

.heatmap-chart-box {
  flex: 1;
  width: 100%;
  min-height: 140px;
  margin-top: 5px;
}

.heatmap-chart-box :deep(canvas) {
  outline: none !important;
}

.heatmap-footer {
  font-size: 14px;
  color: #ac9e9e;
  text-align: right;
  padding-top: 5px;
  flex-shrink: 0;
}

.echarts-box {
  height: 220px !important;
  width: 100%;
  flex-shrink: 0;
  flex-grow: 0;
}

.todo-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  width: 100%;
  box-sizing: border-box;
  border-top: 1px solid var(--border-color, #E7ECF3);
  padding-top: 16px;
  margin-top: 4px;
}

.todo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.divider {
  height: 1px;
  background: var(--border-color);
  margin: 0 24px;
  flex-shrink: 0;
}

.plan-progress-wrapper {
  background: #EEF2FF;
  padding: 4px 10px;
  border-radius: 99px;
}

.plan-progress-text {
  font-size: 12px;
  color: var(--primary-color);
  font-weight: 700;
}

.todo-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  overflow-y: auto;
  min-height: 0;
  padding-right: 4px;
}

.todo-list::-webkit-scrollbar {
  width: 4px;
}

.todo-list::-webkit-scrollbar-thumb {
  background: #E7ECF3;
  border-radius: 4px;
}

.todo-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.todo-item:hover {
  background: var(--bg-sub, #F8FAFD);
}

.todo-text {
  flex: 1;
  font-size: 14px;
  color: var(--text-regular);
  transition: 0.2s;
  cursor: pointer;
  line-height: 1.5;
  margin-top: -1px;
}

.todo-item.done .todo-text {
  color: var(--text-light);
  text-decoration: line-through;
}

.btn-delete-todo {
  background: none;
  border: none;
  color: #EF4444;
  opacity: 0;
  cursor: pointer;
  transition: 0.2s;
  font-size: 13px;
  padding: 2px;
}

.todo-item:hover .btn-delete-todo {
  opacity: 0.7;
}

.todo-item:hover .btn-delete-todo:hover {
  opacity: 1;
}

.custom-checkbox {
  position: relative;
  display: inline-block;
  width: 18px;
  height: 18px;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 1px;
}

.custom-checkbox input {
  opacity: 0;
  width: 0;
  height: 0;
}

.checkmark {
  position: absolute;
  top: 0;
  left: 0;
  height: 18px;
  width: 18px;
  background: #fff;
  border: 2px solid #D1D5DB;
  border-radius: 5px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.custom-checkbox:hover input ~ .checkmark {
  border-color: var(--primary-color);
}

.custom-checkbox input:checked ~ .checkmark {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
  left: 5px;
  top: 2px;
  width: 4px;
  height: 9px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.custom-checkbox input:checked ~ .checkmark:after {
  display: block;
}

.todo-add-area {
  display: flex;
  gap: 10px;
  margin-top: auto;
  width: 100%;
  box-sizing: border-box;
  padding-top: 10px;
  padding-bottom: 2px;
}

.todo-add-area input {
  flex: 1;
  min-width: 0;
  width: 0;
  height: 36px;
  border: 1px solid #D1D5DB;
  border-radius: 5px;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
  background: #F9FAFB;
  color: var(--text-main);
  transition: border-color 0.2s, background 0.2s;
  box-sizing: border-box;
}

.todo-add-area input:focus {
  border-color: #9CA3AF;
  background: #FFFFFF;
  box-shadow: none !important;
}

.btn-todo-add {
  flex-shrink: 0;
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 5px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  /* 加上 #2563EB 作为兜底色 */
  background: var(--primary-color, #2563EB);
  color: #fff;
  transition: background-color 0.2s;
}

.btn-todo-add:hover:not(:disabled) {
  /* 加上 #1D4ED8 作为悬浮时的兜底色，防止背景丢失 */
  background: var(--primary-hover, #1D4ED8);
}

.btn-todo-add:disabled {
  background: #F3F4F6;
  color: #9CA3AF;
  border: 1px solid #E7ECF3;
  cursor: not-allowed;
  opacity: 1;
}

.plan-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-light);
  font-size: 13px;
}

.enhanced-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 0;
  border-bottom: none;
}

.header-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.section-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
  line-height: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title::before {
  content: '';
  display: block;
  width: 4px;
  height: 18px;
  background: var(--primary-color);
  border-radius: 2px;
}

.section-subtitle {
  margin: 0;
  color: var(--text-sub);
  font-size: 14px;
  line-height: 1.6;
}

.tab-row {
  display: flex;
  gap: 4px;
  background: #F1F5F9;
  padding: 5px;
  border-radius: 99px;
  border: none;
}

.recommendation-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.profile-link-btn {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.profile-link-btn:hover {
  background: #dbeafe;
  border-color: #93c5fd;
}

.tab-btn {
  background: transparent;
  border: none;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-sub);
  padding: 8px 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 99px;
}

.tab-btn:hover {
  color: var(--primary-color);
}

.tab-btn.active {
  color: var(--primary-color);
  font-weight: 600;
  background: #FFFFFF;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08);
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  align-items: stretch;
}

.new-course-card {
  background: #FFFFFF;
  border-radius: 5px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  height: 100%;
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
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.new-course-card:hover .ncc-overlay {
  opacity: 1;
}

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

.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  align-items: stretch;
}

.recommendation-grid > .recommendation-card:nth-child(n + 9) {
  display: none !important;
}

.recommendation-card {
  background: #FFFFFF;
  border-radius: 5px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.recommendation-card:hover {
  border-color: #93C5FD;
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
}

.recommendation-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  position: relative;
  overflow: hidden;
  background: #F3F4F6;
}

.recommendation-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.recommendation-cover-empty,
.recommendation-smart-cover {
  width: 100%;
  height: 100%;
}

.recommendation-cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F1F5F9;
  color: #64748B;
  font-size: 14px;
  font-weight: 700;
}

.recommendation-cover-placeholder {
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

.recommendation-cover-placeholder span {
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

.recommendation-cover-placeholder strong {
  color: #334155;
  font-size: 16px;
  font-weight: 800;
}

.recommendation-smart-cover {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px;
  overflow: hidden;
  color: #1F2937;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(248, 250, 252, 0.76)),
    linear-gradient(135deg, #EAF2FF, #F7FAFF);
}

.recommendation-smart-cover::before {
  content: '';
  position: absolute;
  width: 112px;
  height: 82px;
  right: 18px;
  bottom: 18px;
  border: 1px solid rgba(37, 99, 235, 0.13);
  border-radius: 10px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(255, 255, 255, 0.38)),
    repeating-linear-gradient(180deg, rgba(37, 99, 235, 0.16) 0 1px, transparent 1px 12px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  transform: rotate(-4deg);
}

.recommendation-smart-cover::after {
  content: '';
  position: absolute;
  width: 74px;
  height: 74px;
  right: 82px;
  bottom: 42px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.68), rgba(255, 255, 255, 0.24)),
    currentColor;
  opacity: 0.1;
  border-radius: 16px;
  transform: rotate(10deg);
  pointer-events: none;
}

.smart-cover-grid {
  position: absolute;
  inset: 0;
  opacity: 0.35;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.06) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.9), transparent 82%);
}

.smart-cover-top,
.smart-cover-center,
.smart-cover-bottom {
  position: relative;
  z-index: 1;
}

.smart-cover-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  font-weight: 800;
}

.smart-cover-top span {
  max-width: 48%;
  min-height: 26px;
  padding: 5px 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(37, 99, 235, 0.12);
  color: #1D4ED8;
  box-shadow: 0 3px 8px rgba(15, 23, 42, 0.04);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.smart-cover-center {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.smart-cover-symbol {
  max-width: 100%;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 34px;
  line-height: 1;
  font-weight: 900;
  letter-spacing: 0;
  color: #2563EB;
  white-space: nowrap;
}

.smart-cover-title {
  max-width: 100%;
  font-size: 18px;
  line-height: 1.15;
  font-weight: 900;
  color: #1F2937;
  text-wrap: balance;
}

.smart-cover-bottom {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.smart-cover-bottom span {
  min-height: 24px;
  padding: 4px 8px;
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.74);
  color: #475569;
  border: 1px solid rgba(148, 163, 184, 0.16);
  font-size: 12px;
  font-weight: 800;
}

.cover-python {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(239, 246, 255, 0.72)),
    linear-gradient(135deg, #DBEAFE, #CCFBF1);
  color: #1D4ED8;
}

.cover-java {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 247, 237, 0.7)),
    linear-gradient(135deg, #FEE2E2, #DBEAFE);
  color: #B45309;
}

.cover-sql {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(240, 253, 250, 0.72)),
    linear-gradient(135deg, #CCFBF1, #E0F2FE);
  color: #0F766E;
}

.cover-array {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(245, 243, 255, 0.72)),
    linear-gradient(135deg, #EDE9FE, #E0E7FF);
  color: #6D28D9;
}

.cover-algo {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 251, 235, 0.72)),
    linear-gradient(135deg, #FEF3C7, #FFEDD5);
  color: #B45309;
}

.cover-frontend {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(240, 249, 255, 0.72)),
    linear-gradient(135deg, #BAE6FD, #E0E7FF);
  color: #0369A1;
}

.cover-review {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(253, 242, 248, 0.72)),
    linear-gradient(135deg, #FCE7F3, #FEE2E2);
  color: #BE123C;
}

.cover-structure {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(236, 253, 245, 0.72)),
    linear-gradient(135deg, #D1FAE5, #DBEAFE);
  color: #047857;
}

.cover-video {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(239, 246, 255, 0.72)),
    linear-gradient(135deg, #DBEAFE, #E0F2FE);
  color: #2563EB;
}

.cover-text {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 250, 252, 0.78)),
    linear-gradient(135deg, #EEF4FF, #ECFDF5);
  color: #2563EB;
}

.cover-practice {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(240, 253, 250, 0.72)),
    linear-gradient(135deg, #CCFBF1, #DBEAFE);
  color: #0F766E;
}

.recommendation-card:hover .recommendation-cover img {
  transform: scale(1.04);
}

.recommendation-card:hover .recommendation-smart-cover {
  filter: saturate(1.08);
}

.recommendation-cover-overlay {
  display: none;
  position: absolute;
  inset: 0;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.32);
  color: #FFFFFF;
  font-size: 15px;
  font-weight: 700;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.recommendation-card:hover .recommendation-cover-overlay {
  opacity: 0;
}

.recommendation-topline,
.recommendation-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 18px;
}

.recommendation-topline {
  padding-top: 14px;
}

.recommendation-footer {
  margin-top: auto;
  padding-bottom: 16px;
}

.resource-type-pill,
.source-pill,
.knowledge-chip {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  height: 24px;
  padding: 0 9px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.resource-type-pill {
  color: #1D4ED8;
  background: #EFF6FF;
}

.source-pill {
  color: #047857;
  background: #ECFDF5;
}

.recommendation-main {
  flex: 1;
  min-height: 0;
  padding: 12px 18px 14px;
}

.recommendation-title {
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

.recommendation-reason {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.knowledge-chip {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #475569;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
}

.recommendation-action {
  flex-shrink: 0;
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 800;
}

.empty-action {
  margin-left: 10px;
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 5px;
  background: var(--primary-color, #2563EB);
  color: #FFFFFF;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.empty-action:hover {
  background: var(--primary-hover, #1D4ED8);
}

.recommendation-regenerate-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 28px 0 8px;
}

.regenerate-text {
  color: #475569;
  font-size: 16px;
  font-weight: 700;
}

.regenerate-action {
  height: 40px;
  min-width: 128px;
  padding: 0 22px;
  border-radius: 7px;
  font-size: 15px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18);
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.regenerate-action:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.22);
}

/* ================= 翻页组件美化 ================= */
.pagination-area {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  padding-bottom: 24px;
}

/* 覆盖 Ant Design 分页容器的默认间距 */
.pagination-area :deep(.ant-pagination) {
  display: flex;
  align-items: center;
  gap: 8px; /* 增加页码之间的呼吸感 */
}

/* 统一控制所有页码块和前后按钮的基础样式 */
.pagination-area :deep(.ant-pagination-item),
.pagination-area :deep(.ant-pagination-prev),
.pagination-area :deep(.ant-pagination-next),
.pagination-area :deep(.ant-pagination-jump-prev),
.pagination-area :deep(.ant-pagination-jump-next) {
  min-width: 38px;
  height: 38px;
  line-height: 36px;
  border-radius: 8px; /* 呼应平台卡片的圆角 */
  background: var(--bg-card, #FFFFFF);
  border: 1px solid var(--border-color, #E7ECF3);
  margin: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 页码字体样式 */
.pagination-area :deep(.ant-pagination-item a) {
  color: var(--text-regular, #344054);
  font-weight: 500;
  font-family: 'Inter', sans-serif;
  transition: color 0.3s;
}

/* 悬浮状态下的互动反馈 */
.pagination-area :deep(.ant-pagination-item:hover),
.pagination-area :deep(.ant-pagination-prev:hover:not(.ant-pagination-disabled)),
.pagination-area :deep(.ant-pagination-next:hover:not(.ant-pagination-disabled)) {
  border-color: var(--primary-color, #2563EB);
  background: var(--bg-sub, #F8FAFD);
  transform: translateY(-2px); /* 轻微上浮 */
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
}

.pagination-area :deep(.ant-pagination-item:hover a) {
  color: var(--primary-color, #2563EB);
}

/* 选中（当前页）状态，使用平台主色和轻投影 */
.pagination-area :deep(.ant-pagination-item-active) {
  background: var(--primary-color, #2563EB);
  border-color: var(--primary-color, #2563EB);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
  transform: none; /* 当前选中项不浮动 */
}

.pagination-area :deep(.ant-pagination-item-active a),
.pagination-area :deep(.ant-pagination-item-active:hover a) {
  color: #FFFFFF;
}

/* 选中状态悬浮时使用更深的交互色 */
.pagination-area :deep(.ant-pagination-item-active:hover) {
  background: var(--primary-hover, #1D4ED8);
  border-color: var(--primary-hover, #1D4ED8);
}

/* 箭头图标重置 */
.pagination-area :deep(.ant-pagination-prev .ant-pagination-item-link),
.pagination-area :deep(.ant-pagination-next .ant-pagination-item-link) {
  color: var(--text-sub, #667085);
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 禁用状态（例如第一页的前一页） */
.pagination-area :deep(.ant-pagination-disabled),
.pagination-area :deep(.ant-pagination-disabled:hover) {
  background: #F9FAFB;
  border-color: #E7ECF3;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.pagination-area :deep(.ant-pagination-disabled .ant-pagination-item-link) {
  color: #D1D5DB;
}

/* 调整省略号（更多）的样式 */
.pagination-area :deep(.ant-pagination-jump-prev .ant-pagination-item-container .ant-pagination-item-ellipsis),
.pagination-area :deep(.ant-pagination-jump-next .ant-pagination-item-container .ant-pagination-item-ellipsis) {
  color: var(--text-light, #98A2B3);
  letter-spacing: 2px;
}

.empty-course-state {
  text-align: center;
  padding: 60px 0;
  color: var(--text-light);
  font-size: 15px;
}



.top-section {
  display: flex;
  gap: 16px;
  align-items: stretch;
  margin-bottom: 0;
}

.left-block {
  width: 220px;
  padding: 24px 16px; /* 稍微缩小上下内边距，腾出空间给行间距 */
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  /* 🚩 核心：如果你希望它高度固定，可以参考中间块的高度（约 616px） */
  height: 616px;
  overflow-y: auto; /* 内容多了就内部滚动，不会撑开页面 */
  justify-content: flex-start; /* 🚩 改为靠顶对齐，不要用 space-between */
}

/* 隐藏滚动条美化 */
.left-block::-webkit-scrollbar {
  width: 0;
}

.left-block-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

/* 专门控制兴趣课的行间距 */
.excellent-section .category-item {
  margin-bottom: 18px; /* 设置一个更大的间距 */
}

.category-item {
  text-align: center;
  cursor: pointer;
  margin-bottom: 14px;
  transition: transform 0.3s ease;
}



.cat-icon-img {
  display: block;
  width: 45px;
  height: 45px;
  margin: 0 auto 6px;
  object-fit: contain;
}
.category-item:hover {
  transform: translateY(-4px);
}

.cat-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-regular);
}

.middle-top-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.banner-wrapper {
  height: 360px;
  flex: none !important;
  border-radius: 5px !important;
  overflow: hidden !important;
  position: relative;
  background: var(--bg-sub);
  border: 1px solid var(--border-color);
  transform: translateZ(0);
}

.heatmap-card {
  height: 240px;
  flex: none !important;
  border-radius: 5px !important;
  padding: 12px 20px 10px;
  /* 使用全局统一的卡片背景色 */
  background-color: var(--bg-card, #FFFFFF);
  display: flex;
  flex-direction: column;
}

.right-top-block {
  width: 280px;
  flex: none;
  padding: 16px 20px 20px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-sizing: border-box;
}

@media (max-width: 1200px) {
  .top-section {
    flex-wrap: wrap;
  }

  .left-block {
    width: 220px;
    padding: 18px 16px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    /* 确保这里没有 height: fit-content 和 align-self: flex-start */
  }

  .middle-top-block {
    flex: 1;
    width: 100%;
  }

  .right-top-block {
    width: 100%;
    flex: none;
    height: 450px;
  }
}



.popover-title {
  font-weight: 600;
  color: var(--text-main);
}

.pop-checkin-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 8px 12px;
  width: 200px;
}

.checkin-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid #E7ECF3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: 0.3s;
  background: #FFF;
}

.checkin-circle.done {
  border-color: #10B981;
  background: #ECFDF5;
  color: #10B981;
}

.checkin-day {
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

/* 请完整替换之前的打卡按钮样式 */
.btn-checkin-submit {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 8px;
  /* 增加了 #2563EB 直接色号作为兜底，防止 CSS 变量在弹窗中失效 */
  background-color: var(--primary-color, #2563EB);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  opacity: 1 !important; /* 强制可见，覆盖可能残留的透明属性 */
  visibility: visible !important;
  transition: all 0.2s ease;
}

.btn-checkin-submit:hover {
  /* 悬浮时的深蓝色兜底 */
  background-color: var(--primary-hover, #1D4ED8);
}

/* 已打卡完成的灰色状态 */
.btn-checkin-submit.done {
  background-color: #E5E7EB !important;
  color: #9CA3AF !important;
  cursor: default;
}

.hw-pop-body {
  width: 280px;
  max-height: 320px;
  overflow-y: auto;
  padding: 4px;
}

.hw-pop-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  border-radius: 8px;
  transition: background 0.2s;
}

.hw-pop-item:hover {
  background: var(--bg-sub);
  border-bottom-color: transparent;
}

.hw-info {
  flex: 1;
  padding-right: 12px;
}

.hw-pop-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 4px;
}

.hw-pop-sub {
  font-size: 12px;
  color: var(--text-light);
}

.btn-do-hw {
  background: #EEF2FF;
  border: none;
  color: var(--primary-color);
  cursor: pointer;
  font-weight: 600;
  border-radius: 6px;
  padding: 6px 12px;
  transition: 0.2s;
}

.btn-do-hw:hover {
  background: #E0E7FF;
}

.hw-pop-empty {
  text-align: center;
  padding: 32px 0;
  color: var(--text-sub);
}

/* ================= 定时器弹窗美化 ================= */
.timer-modal-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.timer-inputs {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 32px;
  background: var(--bg-sub, #F8FAFD);
  padding: 24px 32px;
  border-radius: 16px;
  border: 1px solid var(--border-color, #E7ECF3);
}

.input-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* 穿透修改 a-input-number 样式，打造数字时钟质感 */
.input-group :deep(.ant-input-number) {
  width: 86px;
  height: 72px;
  border-radius: 12px;
  border: 1px solid transparent;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  background: #FFF;
  transition: all 0.3s ease;
}

.input-group :deep(.ant-input-number:hover),
.input-group :deep(.ant-input-number-focused) {
  border-color: var(--primary-color, #2563EB);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.12);
}

.input-group :deep(.ant-input-number-input) {
  height: 72px;
  font-size: 36px;
  font-weight: 800;
  text-align: center;
  color: var(--text-main, #1F2937);
  font-family: 'Inter', -apple-system, monospace;
}

/* 隐藏输入框自带的上下小箭头，保持界面清爽 */
.input-group :deep(.ant-input-number-handler-wrap) {
  display: none;
}

.time-unit {
  color: var(--text-sub, #667085);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 2px;
}

.time-colon {
  font-size: 36px;
  font-weight: 600;
  color: var(--primary-color, #2563EB);
  margin-top: -28px;
  /* 增加呼吸灯闪烁动画 */
  animation: colon-pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes colon-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.timer-msg-input {
  width: 100%;
  margin-bottom: 32px;
}

.timer-msg-input :deep(.ant-input) {
  height: 44px;
  border-radius: 8px;
  font-size: 14px;
  text-align: center;
  background: var(--bg-card, #FFF);
  border-color: var(--border-color, #E7ECF3);
  transition: all 0.3s;
}

.timer-msg-input :deep(.ant-input:focus) {
  border-color: var(--primary-color, #2563EB);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.timer-actions {
  display: flex;
  gap: 16px;
  width: 100%;
  justify-content: center;
}

.timer-actions button {
  height: 44px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-timer-cancel {
  width: 120px;
  background: #F1F5F9;
  color: var(--text-regular, #344054);
}

.btn-timer-cancel:hover {
  background: #E2E8F0;
  color: var(--text-main, #1F2937);
}

.btn-timer-start {
  flex: 1;
  background: var(--primary-color, #2563EB);
  color: #FFF;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.btn-timer-start:hover {
  background: var(--primary-hover, #1D4ED8);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}

.btn-timer-stop {
  width: 100%;
  background: #FEF2F2;
  color: #EF4444;
  border: 1px solid #FECACA !important;
}

.btn-timer-stop:hover {
  background: #FEE2E2;
}

.ai-float {
  position: fixed;
  right: 32px;
  bottom: 32px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.ai-fab-pill {
  height: 48px;
  padding: 0 20px;
  border-radius: 99px;
  border: none;
  background-color: #1F2937 !important;
  color: #FFFFFF !important;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 8px 24px rgba(31, 41, 55, 0.25) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ai-fab-pill:hover {
  background-color: #111827 !important;
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(31, 41, 55, 0.35) !important;
}

.ai-fab-pill.is-open {
  width: 48px;
  padding: 0;
  justify-content: center;
}

.ai-fab-pill.is-open:hover {
  transform: none;
}

.fab-text {
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.ai-panel {
  position: absolute;
  right: 70px;
  bottom: 64px;
  width: 380px;
  height: 560px;
  border-radius: 5px !important;
  background-color: #FFFFFF !important;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.15) !important;
  display: flex;
  flex-direction: column;
  z-index: 101;
  border: 1px solid #E7ECF3 !important;
  overflow: hidden !important;
}

.ai-head {
  flex-shrink: 0;
  padding: 16px 20px;
  border-bottom: 1px solid #E7ECF3;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to right, #F8FAFD, #FFFFFF);
}

.ai-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  background: var(--primary-color);
  color: #FFF;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.ai-title-wrap {
  display: flex;
  flex-direction: column;
}

.ai-title {
  font-weight: 600;
  color: var(--text-main);
  font-size: 15px;
}

.ai-status {
  font-size: 12px;
  color: #10B981;
  display: flex;
  align-items: center;
  gap: 4px;
}

.ai-status::before {
  content: '';
  display: block;
  width: 6px;
  height: 6px;
  background: #10B981;
  border-radius: 50%;
}

.ai-close {
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--text-light);
  font-size: 18px;
  padding: 4px;
  transition: 0.2s;
}

.ai-close:hover {
  color: var(--text-main);
  background: #F3F4F6;
  border-radius: 5px;
}

.ai-body {
  flex: 1 1 0;
  min-height: 0;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background-color: #FAFAFA !important;
}

.msg {
  display: flex;
  max-width: 90%;
  gap: 10px;
}

.msg.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 5px;
  background: #DBEAFE;
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 5px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.msg.ai .msg-bubble {
  background: #FFFFFF;
  border: 1px solid #E7ECF3;
  color: var(--text-regular);
  border-top-left-radius: 0;
}

.msg.user .msg-bubble {
  background: var(--primary-color);
  color: #fff;
  border-top-right-radius: 0;
}

.ai-foot {
  flex-shrink: 0;
  width: 100%;
  box-sizing: border-box;
  padding: 16px;
  background-color: #FFFFFF !important;
  border-top: 1px solid #E7ECF3;
  display: flex;
  gap: 10px;
}

.ai-foot input {
  flex: 1;
  border: 1px solid #E7ECF3;
  background: #F8FAFD;
  border-radius: 5px;
  padding: 0 16px;
  height: 40px;
  outline: none;
  font-size: 14px;
  transition: 0.2s;
}

.ai-foot input:focus {
  border-color: #93C5FD;
  background: #FFFFFF;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.btn-send {
  width: 40px;
  height: 40px;
  border: none;
  background: var(--primary-color);
  color: #fff;
  border-radius: 5px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
}

.btn-send:hover:not(:disabled) {
  transform: scale(1.05);
}

.btn-send:disabled {
  background: #D1D5DB;
  cursor: not-allowed;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
  pointer-events: none;
}

.markdown-body {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-regular);
  white-space: normal;
}

:deep(.markdown-body pre) {
  margin: 10px 0;
  padding: 12px;
  border-radius: 5px;
  background-color: #1a1a1a !important;
  overflow-x: auto;
}

:deep(.markdown-body code) {
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 13px;
}

.site-footer {
  background-color: transparent;
  border-top: 1px solid rgba(231, 236, 243, 0.6);
  padding: 16px 0 30px;
  margin-top: 0px;
}

.footer-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
}

.footer-links {
  flex: 1;
  display: flex;
  justify-content: space-between;
  padding-right: 80px;
}

.footer-links dl {
  margin: 0;
}

.footer-links dt {
  font-size: 20px;
  font-weight: 500;
  color: var(--text-main, #1F2937);
  margin-bottom: 16px;
  letter-spacing: 0.5px;
}

.footer-links dd {
  margin: 0 0 10px 0;
  font-size: 14px;
}

.footer-links dd a {
  color: var(--text-sub, #667085);
  text-decoration: none;
  transition: color 0.2s ease;
}

.footer-links dd a:hover {
  color: var(--primary-color, #2563EB);
}

.footer-qrcode {
  width: 140px;
  text-align: center;
  flex-shrink: 0;
}

.qr-item img {
  width: 120px;
  height: 120px;
  object-fit: contain;
  background: #FFF;
  padding: 8px;
  border: 1px solid var(--border-color, #E7ECF3);
  border-radius: 8px;
  margin-bottom: 12px;
  box-shadow: var(--shadow-sm);
  transition: transform 0.3s;
}

.qr-item img:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.qr-item p {
  margin: 0;
  font-size: 13px;
  color: var(--text-sub, #667085);
}

.footer-copyright {
  border-top: 1px solid var(--border-color, #E7ECF3);
  padding-top: 18px;
  text-align: center;
}

.bottom-links {
  margin: 0 0 12px 0;
  font-size: 14px;
}

.bottom-links .divider {
  color: #D1D5DB;
  margin: 0 12px;
  font-size: 12px;
}

.bottom-links a {
  color: var(--text-sub, #667085);
  text-decoration: none;
  transition: color 0.2s ease;
}

.bottom-links a:hover {
  color: var(--primary-color, #2563EB);
}

.footer-copyright p:last-child {
  margin: 0;
  font-size: 13px;
  color: var(--text-light, #98A2B3);
  letter-spacing: 0.5px;
}

.left-block,
.banner-wrapper,
.heatmap-card,
.right-top-block {
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
  transition: box-shadow 0.25s ease, transform 0.25s ease, border-color 0.25s ease;
}

.left-block:hover,
.banner-wrapper:hover,
.heatmap-card:hover,
.right-top-block:hover {
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

@media (max-width: 992px) {
  .footer-main {
    flex-direction: column;
    align-items: center;
    gap: 40px;
  }

  .footer-links {
    width: 100%;
    padding-right: 0;
    flex-wrap: wrap;
    gap: 32px;
  }

  .footer-links dl {
    min-width: 160px;
  }
}

@media (max-width: 640px) {
  .footer-links {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .bottom-links .divider {
    display: none;
  }

  .bottom-links a {
    display: block;
    margin: 8px 0;
  }
}

.typing {
  display: flex;
  gap: 5px;
  align-items: center;
  height: 24px;
  padding: 0 8px;
}

.typing span {
  width: 6px;
  height: 6px;
  background: var(--primary-color);
  opacity: 0.6;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

@media (max-width: 1100px) {
  .dashboard-top-half {
    flex-direction: column;
    height: auto;
  }

  .col-right {
    width: 100%;
  }

  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .recommendation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-search-box {
    width: 100%;
    max-width: 400px;
  }
}

@media (max-width: 640px) {
  .enhanced-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .tab-row {
    width: 100%;
    overflow-x: auto;
  }

  .recommendation-grid {
    grid-template-columns: 1fr;
  }
}

.left-top-content {
  /* 利用 auto 会自动吸附剩余空间的特性，将下方的区块向下推 */
}

</style>


<style>
/* 严谨修复 Ant Design 弹窗引发的页面滚动条抖动问题 */
html {
  /* 始终保留垂直滚动条的轨道，避免滚动条消失引起的宽度突变 */
  overflow-y: scroll !important;
}
body {
  /* 强制覆盖 antd 动态添加的内边距和宽度限制 */
  width: 100% !important;
  padding-right: 0 !important;
}
</style>
