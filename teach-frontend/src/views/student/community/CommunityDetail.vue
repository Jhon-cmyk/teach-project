<template>
  <div class="detail-page">
    <div class="page-container">
      <div class="page-top">
        <button class="btn-back-link" @click="goBack">
          <LeftOutlined />
          <span>{{ backLabel }}</span>
        </button>
      </div>

      <div v-if="detailState === 'loading'" class="state-card">
        <a-spin />
        <span class="state-text">正在加载讨论...</span>
      </div>

      <div v-else-if="detailState === 'auth'" class="state-card">
        <div class="state-icon-circle state-auth-circle">!</div>
        <p class="state-text">请先登录后查看讨论详情</p>
        <button class="btn-retry" @click="goBack">返回上一级</button>
      </div>

      <div v-else-if="detailState === 'error'" class="state-card">
        <div class="state-icon-circle state-error-circle">!</div>
        <p class="state-text">{{ detailErrorText || '加载失败，请稍后重试' }}</p>
        <button class="btn-retry" @click="retryLoad">重新加载</button>
      </div>

      <div v-else-if="detailState === 'not-found'" class="state-card">
        <div class="state-icon-circle state-empty-circle">
          <SearchOutlined />
        </div>
        <p class="state-text">未找到该讨论，内容可能已被删除</p>
        <button class="btn-retry" @click="goBack">返回上一级</button>
      </div>

      <template v-else-if="detail">
        <div class="detail-layout">
          <div class="detail-main">
            <div
              ref="overviewSectionRef"
              class="content-card"
              :class="{
                'section-focused': activeFocusArea === 'overview',
                'section-dimmed': isFocusMode && activeFocusArea === 'replies'
              }"
            >
              <div
                v-if="entryNotice"
                class="entry-notice"
                :class="`is-${entryNotice.kind}`"
              >
                <CheckCircleOutlined v-if="entryNotice.kind === 'resolved'" class="entry-notice-icon" />
                <InfoCircleOutlined v-else class="entry-notice-icon" />
                <div class="entry-notice-copy">
                  <strong>{{ entryNotice.title }}</strong>
                  <span>{{ entryNotice.text }}</span>
                </div>
              </div>

              <div class="detail-heading-row">
                <h1 class="detail-title">{{ detail.title }}</h1>
                <div class="detail-stats">
                  <span class="stat-item">
                    <EyeOutlined class="stat-icon" />
                    {{ detail.viewCount }} 次浏览
                  </span>
                  <span class="stat-item">
                    <MessageOutlined class="stat-icon" />
                    {{ detail.replyCount }} 条回复
                  </span>
                  <span class="stat-item stat-time">最后活跃：{{ detail.lastActiveTime }}</span>
                </div>
              </div>

              <div class="detail-info">
                <span class="course-tag">{{ detail.courseName }}</span>
                <span v-if="detail.isHot" class="badge badge-hot">热门</span>
                <span v-if="detail.isTeacherAnswered" class="badge badge-teacher">老师已答</span>
                <span v-if="detail.status === 'resolved'" class="badge badge-resolved">已解决</span>
                <span v-else-if="detail.postType === 'homework'" class="badge badge-open">待解决</span>
                <span v-if="isFeaturedDiscussion" class="badge badge-featured">
                  {{ featuredMeta?.isRecommended ? '推荐精选' : '已入选精选' }}
                </span>
                <span class="info-sep">·</span>
                <span class="info-item">{{ detail.authorName }}</span>
                <span class="info-sep">·</span>
                <span class="info-item">{{ detail.createdAt }}</span>
              </div>

              <div v-if="isFeaturedDiscussion" class="featured-status-strip">
                <div class="featured-status-main">
                  <span
                    class="featured-status-badge"
                    :class="{ recommended: featuredMeta?.isRecommended }"
                  >
                    {{ featuredMeta?.isRecommended ? '推荐精选' : '已入选答疑精选' }}
                  </span>
                  <span class="featured-status-text">{{ featuredTipText }}</span>
                </div>

              </div>

              <div v-if="isTeacher" class="teacher-action-bar">
                <div class="teacher-action-left">
                  <span class="teacher-action-label">教师操作</span>
                  <span v-if="canResolvePost" class="teacher-action-hint">可将当前作业问题标记为已解决</span>
                  <span
                    v-else-if="detail.postType === 'homework' && detail.status === 'resolved'"
                    class="teacher-action-hint"
                  >
                    当前问题已解决
                  </span>
                  <span v-else class="teacher-action-hint">可将当前讨论加入答疑精选</span>
                </div>

                <div class="teacher-action-buttons">
                  <button
                    v-if="canResolvePost"
                    class="btn-soft-success"
                    :disabled="resolving"
                    @click="handleResolve"
                  >
                    {{ resolving ? '处理中...' : '标记已解决' }}
                  </button>

                  <button
                    class="btn-soft-primary"
                    :disabled="featuredSubmitting"
                    @click="openFeaturedModal()"
                  >
                    {{ featuredSubmitting ? '处理中...' : '加入答疑精选' }}
                  </button>
                </div>
              </div>

              <div class="detail-body rich-content" v-html="normalizeRichHtml(detail.content)"></div>
            </div>

            <div
              ref="repliesSectionRef"
              class="replies-section"
              :class="{
                'section-focused': activeFocusArea === 'replies',
                'section-dimmed': isFocusMode && activeFocusArea === 'overview'
              }"
            >
              <div class="replies-header">
                <h2 class="replies-title">
                  全部回复
                  <span class="replies-count">{{ detail.replies.length }}</span>
                </h2>

              </div>

              <div class="reply-editor">
                <div class="rich-editor-shell reply-rich-editor">
                  <div class="rich-editor-surface">
                    <Toolbar
                      style="border-bottom: 1px solid #E2E8F0"
                      :editor="replyEditorRef"
                      :defaultConfig="replyToolbarConfig"
                      mode="default"
                    />
                    <Editor
                      style="height: 220px; overflow-y: hidden;"
                      v-model="replyContent"
                      :defaultConfig="replyEditorConfig"
                      mode="default"
                      @onCreated="handleReplyEditorCreated"
                    />
                  </div>
                </div>
                <div class="reply-editor-actions">
                  <span class="reply-tip">支持富文本回复，提交后会自动刷新列表</span>
                  <button
                    class="btn-reply-submit"
                    :disabled="replySubmitting"
                    @click="submitReply"
                  >
                    {{ replySubmitting ? '提交中...' : '提交回复' }}
                  </button>
                </div>
              </div>

              <div v-if="detail.replies.length === 0" class="replies-empty">
                <p>暂无回复，等待同学和老师的解答</p>
              </div>

              <div v-else class="replies-list">
                <div
                  v-for="reply in detail.replies"
                  :key="reply.id"
                  class="reply-item"
                  :class="{ 'is-teacher': reply.isTeacher }"
                >
                  <div class="reply-avatar">
                    <span class="avatar-char" :class="{ teacher: reply.isTeacher }">
                      {{ reply.authorName?.charAt(0) || '回' }}
                    </span>
                  </div>

                  <div class="reply-body">
                    <div class="reply-header">
                      <span class="reply-author">{{ reply.authorName }}</span>
                      <span v-if="reply.isTeacher" class="teacher-badge">教师</span>
                      <span class="reply-time">{{ reply.createdAt }}</span>

                      <button
                        v-if="isTeacher && reply.isTeacher"
                        class="reply-featured-link"
                        @click="openFeaturedModal(reply)"
                      >
                        设为精选来源
                      </button>
                    </div>

                    <div class="reply-content rich-content" v-html="normalizeRichHtml(reply.content)"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <a-modal
      v-model:open="featuredVisible"
      title="加入答疑精选"
      :footer="null"
      :maskClosable="!featuredSubmitting"
      destroyOnClose
    >
      <div class="featured-form">
        <div class="form-item">
          <label class="form-label">精选来源</label>
          <select v-model="featuredForm.replyId" class="form-select">
            <option value="">基于当前帖子正文</option>
            <option
              v-for="reply in teacherReplies"
              :key="reply.id"
              :value="String(reply.id)"
            >
              基于教师回复：{{ reply.authorName }} - {{ reply.createdAt }}
            </option>
          </select>
        </div>

        <div class="form-item">
          <label class="form-label">精选摘要</label>
          <div class="rich-editor-shell excerpt-rich-editor">
            <div class="rich-editor-surface">
              <Toolbar
                style="border-bottom: 1px solid #E2E8F0"
                :editor="excerptEditorRef"
                :defaultConfig="excerptToolbarConfig"
                mode="default"
              />
              <Editor
                style="height: 180px; overflow-y: hidden;"
                v-model="featuredForm.excerpt"
                :defaultConfig="excerptEditorConfig"
                mode="default"
                @onCreated="handleExcerptEditorCreated"
              />
            </div>
          </div>
        </div>

        <label class="featured-check">
          <input v-model="featuredForm.isRecommended" type="checkbox" />
          <span>设为推荐精选</span>
        </label>

        <div class="form-actions">
          <button
            class="btn-cancel"
            :disabled="featuredSubmitting"
            @click="featuredVisible = false"
          >
            取消
          </button>
          <button
            class="btn-submit"
            :disabled="featuredSubmitting"
            @click="submitFeatured"
          >
            {{ featuredSubmitting ? '提交中...' : '加入精选' }}
          </button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { getLoginUserRaw } from '@/utils/authStorage'
import '@wangeditor/editor/dist/css/style.css'
// @ts-ignore
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  LeftOutlined,
  SearchOutlined,
  MessageOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'
import {
  addCommunityReply,
  addFeaturedAnswer,
  extractCommunityErrorMessage,
  getDiscussionDetail,
  getFeaturedDiscussionMeta,
  isCommunityAuthError,
  resolveCommunityPost
} from '@/api/community'
import type {
  DiscussionDetail,
  DiscussionReply,
  FeaturedAnswerItem
} from '@/types/community'
import { useCommunityNotificationBadge } from '@/composables/useCommunityNotificationBadge' // 🌟 新增：引入消息红点组件

const router = useRouter()
const route = useRoute()

// 🌟 新增：获取未读消息数
const { unreadCount } = useCommunityNotificationBadge()

const loading = ref(false)
const resolving = ref(false)
const replySubmitting = ref(false)
const featuredSubmitting = ref(false)

const detail = ref<DiscussionDetail | null>(null)
const featuredMeta = ref<FeaturedAnswerItem | null>(null)

const detailState = ref<'loading' | 'ready' | 'auth' | 'error' | 'not-found'>('loading')
const detailErrorText = ref('')

const replyContent = ref('')
const featuredVisible = ref(false)
const featuredForm = ref({
  replyId: '',
  excerpt: '',
  isRecommended: true
})

const replyEditorRef = shallowRef()
const excerptEditorRef = shallowRef()
const replyToolbarConfig = {}
const excerptToolbarConfig = {}
const replyEditorConfig = { placeholder: '写下你的回复，帮助同学解决问题...' }
const excerptEditorConfig = { placeholder: '提炼这条答疑的关键信息，方便学生快速阅读' }

const handleReplyEditorCreated = (editor: any) => {
  replyEditorRef.value = editor
}

const handleExcerptEditorCreated = (editor: any) => {
  excerptEditorRef.value = editor
}

const overviewSectionRef = ref<HTMLElement | null>(null)
const repliesSectionRef = ref<HTMLElement | null>(null)
const activeFocusArea = ref<'overview' | 'replies' | ''>('')
let focusTimer: number | null = null

const loginUser = computed(() => {
  try {
    const raw = getLoginUserRaw()
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
})

const isTeacher = computed(() => loginUser.value?.userRole === 'teacher')
const currentTeacherId = computed(() => loginUser.value?.id ?? loginUser.value?.userId)
const currentTeacherName = computed(() => loginUser.value?.userName ?? loginUser.value?.username)

const sourceFrom = computed(() => String(route.query.from || 'list'))
const focusTarget = computed(() => String(route.query.focus || ''))
const notificationType = computed(() => String(route.query.notificationType || ''))
const highlightType = computed(() => String(route.query.highlight || ''))
const isFocusMode = computed(() => focusTarget.value === 'overview' || focusTarget.value === 'replies')

const backLabel = computed(() => {
  if (sourceFrom.value === 'notifications') return '返回社区动态'
  if (sourceFrom.value === 'mine') return '返回我的社区'
  if (sourceFrom.value === 'featured') return '返回学习交流'
  if (sourceFrom.value === 'homework') return '返回作业互助'
  if (sourceFrom.value === 'dashboard') return '返回首页'
  return '返回学习交流'
})

const overviewFocusTip = computed(() => {
  if (sourceFrom.value === 'notifications') {
    return '已为你定位到正文内容'
  }
  return ''
})

const entryNotice = computed(() => {
  if (sourceFrom.value !== 'notifications') return null

  if (
    notificationType.value === 'post_resolved'
    || highlightType.value === 'resolved'
    || detail.value?.status === 'resolved'
  ) {
    return {
      kind: 'resolved',
      title: '问题已解决',
      text: '你从通知进入，正文已定位到这里。'
    }
  }

  return {
    kind: 'info',
    title: '来自社区通知',
    text: overviewFocusTip.value || '已定位到相关内容。'
  }
})

const isFeaturedDiscussion = computed(() => {
  return !!featuredMeta.value
    || highlightType.value === 'featured'
    || notificationType.value === 'post_featured'
})

const featuredTipText = computed(() => {
  if (featuredMeta.value?.isRecommended) {
    return `这条讨论已被老师或助教整理为推荐精选，适合优先阅读。${featuredMeta.value.teacherName ? `整理者：${featuredMeta.value.teacherName}。` : ''}`
  }

  if (featuredMeta.value) {
    return `这条讨论已被收录进答疑精选，可作为同类问题参考。${featuredMeta.value.teacherName ? `整理者：${featuredMeta.value.teacherName}。` : ''}`
  }

  return '这条讨论已入选精选内容，可以作为高质量参考阅读。'
})

const teacherReplies = computed(() => {
  return (detail.value?.replies || []).filter(reply => reply.isTeacher)
})

const canResolvePost = computed(() => {
  return isTeacher.value
    && detail.value?.postType === 'homework'
    && detail.value?.status === 'open'
})

function escapeHtml(text: string) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function plainTextToHtml(text: string) {
  const lines = String(text || '')
    .replace(/\r/g, '')
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)

  if (lines.length === 0) return ''
  return lines.map(line => `<p>${escapeHtml(line)}</p>`).join('')
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

function normalizeRichHtml(content: string) {
  const raw = String(content || '').trim()
  if (!raw) return '<p>暂无内容</p>'

  const looksLikeHtml = /<\/?[a-z][\s\S]*>/i.test(raw)
  if (looksLikeHtml) return raw

  return plainTextToHtml(raw) || '<p>暂无内容</p>'
}

function hasRichTextContent(content: string) {
  return !!toPlainText(content).trim()
}

function parseContent(content: string) {
  return String(content || '')
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)
}

function buildExcerptFromText(text: string, max = 120) {
  const clean = toPlainText(text).replace(/\n+/g, ' ').trim()
  return clean.length > max ? `${clean.slice(0, max)}...` : clean
}

function clearFocusTimer() {
  if (focusTimer !== null) {
    window.clearTimeout(focusTimer)
    focusTimer = null
  }
}

function triggerFocus(area: 'overview' | 'replies') {
  clearFocusTimer()
  activeFocusArea.value = area
  focusTimer = window.setTimeout(() => {
    activeFocusArea.value = ''
    focusTimer = null
  }, 2600)
}

async function applyRouteFocus() {
  await nextTick()

  if (focusTarget.value === 'replies' && repliesSectionRef.value) {
    repliesSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
    triggerFocus('replies')
    return
  }

  if (focusTarget.value === 'overview' && overviewSectionRef.value) {
    overviewSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
    triggerFocus('overview')
  }
}

async function fetchDetail(id: string) {
  loading.value = true
  detailState.value = 'loading'
  detailErrorText.value = ''
  detail.value = null
  featuredMeta.value = null

  try {
    const data = await getDiscussionDetail(id)

    if (!data) {
      detailState.value = 'not-found'
      return
    }

    detail.value = data

    featuredMeta.value = await getFeaturedDiscussionMeta(data.id).catch(() => null)

    detailState.value = 'ready'
    await applyRouteFocus()
  } catch (error) {
    console.error('[CommunityDetail] 加载失败', error)
    detailState.value = isCommunityAuthError(error) ? 'auth' : 'error'
    detailErrorText.value = extractCommunityErrorMessage(error, '加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function retryLoad() {
  const id = String(route.params.id || '')
  if (id) {
    fetchDetail(id)
  }
}

function goBack() {
  if (sourceFrom.value === 'notifications') {
    router.push({ name: 'CommunityNotifications' })
    return
  }

  if (sourceFrom.value === 'mine') {
    const tab = route.query.tab === 'replies' ? 'replies' : 'posts'
    router.push({
      name: 'MyCommunity',
      query: tab === 'replies' ? { tab: 'replies' } : undefined
    })
    return
  }

  if (sourceFrom.value === 'featured') {
    router.push({ name: 'CommunityList' })
    return
  }

  if (sourceFrom.value === 'homework') {
    router.push({ name: 'HomeworkHelp' })
    return
  }

  if (sourceFrom.value === 'dashboard') {
    router.push('/student/dashboard')
    return
  }

  router.push({ name: 'CommunityList' })
}

async function submitReply() {
  if (!detail.value?.id) return

  const content = replyContent.value
  if (!hasRichTextContent(content)) {
    message.warning('请输入回复内容')
    return
  }

  replySubmitting.value = true
  try {
    await addCommunityReply({
      postId: detail.value.id,
      content
    })
    message.success('回复成功')
    replyContent.value = ''
    await fetchDetail(String(detail.value.id))
    triggerFocus('replies')
  } catch (error) {
    console.error('[CommunityDetail] 回复失败', error)
    message.error(extractCommunityErrorMessage(error, '回复失败，请稍后再试'))
  } finally {
    replySubmitting.value = false
  }
}

async function handleResolve() {
  if (!detail.value?.id) return

  resolving.value = true
  try {
    await resolveCommunityPost(detail.value.id)
    message.success('已标记为已解决')
    await fetchDetail(String(detail.value.id))
    triggerFocus('overview')
  } catch (error) {
    console.error('[CommunityDetail] 标记已解决失败', error)
    message.error(extractCommunityErrorMessage(error, '操作失败'))
  } finally {
    resolving.value = false
  }
}

function openFeaturedModal(reply?: DiscussionReply) {
  if (!detail.value) return

  featuredForm.value.replyId = reply ? String(reply.id) : ''
  featuredForm.value.excerpt = plainTextToHtml(
    reply
      ? buildExcerptFromText(reply.content, 120)
      : buildExcerptFromText(detail.value.content || detail.value.title, 120)
  )
  featuredForm.value.isRecommended = true
  featuredVisible.value = true
}

async function submitFeatured() {
  if (!detail.value?.id) return

  if (!currentTeacherId.value || !currentTeacherName.value) {
    message.warning('未获取到当前教师信息')
    return
  }

  const excerpt = toPlainText(featuredForm.value.excerpt)
  if (!excerpt) {
    message.warning('请输入精选摘要')
    return
  }

  featuredSubmitting.value = true
  try {
    await addFeaturedAnswer({
      postId: detail.value.id,
      replyId: featuredForm.value.replyId || undefined,
      teacherId: currentTeacherId.value,
      teacherName: currentTeacherName.value,
      excerpt,
      isRecommended: featuredForm.value.isRecommended
    })

    message.success('已加入答疑精选')
    featuredVisible.value = false
    await fetchDetail(String(detail.value.id))
    triggerFocus('overview')
  } catch (error) {
    console.error('[CommunityDetail] 加入精选失败', error)
    message.error(extractCommunityErrorMessage(error, '加入精选失败'))
  } finally {
    featuredSubmitting.value = false
  }
}

watch(
  () => route.params.id,
  newId => {
    if (newId && route.name === 'CommunityDetail') {
      fetchDetail(String(newId))
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }
)

watch(
  () => [route.query.focus, route.query.notificationType, route.query.highlight],
  async () => {
    if (detailState.value === 'ready' && detail.value && !loading.value) {
      await applyRouteFocus()
    }
  }
)

watch(
  () => featuredForm.value.replyId,
  replyId => {
    if (!detail.value) return

    if (!replyId) {
      featuredForm.value.excerpt = plainTextToHtml(buildExcerptFromText(detail.value.content || detail.value.title, 120))
      return
    }

    const matched = teacherReplies.value.find(item => String(item.id) === String(replyId))
    if (matched) {
      featuredForm.value.excerpt = plainTextToHtml(buildExcerptFromText(matched.content, 120))
    }
  }
)

watch(featuredVisible, visible => {
  if (!visible) {
    featuredForm.value = {
      replyId: '',
      excerpt: '',
      isRecommended: true
    }
  }
})

onMounted(() => {
  const id = route.params.id as string
  if (id) fetchDetail(id)
})

onBeforeUnmount(() => {
  clearFocusTimer()
  replyEditorRef.value?.destroy?.()
  excerptEditorRef.value?.destroy?.()
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #F8FAFC;
  color: #334155;
  padding-bottom: 80px;
}

.page-container {
  width: 75%;
  min-width: 1000px;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-top {
  padding: 20px 0 14px;
}
.btn-back-link {
  height: 36px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px 0 12px;
  margin-left: 0;
  border: 1px solid #DDE7F2;
  border-radius: 6px;
  background: #FFFFFF;
  color: #475569;
  font-size: 14px;
  font-weight: 750;
  cursor: pointer;
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

.header-actions { display: flex; gap: 12px; }
.header-action-btn {
  height: 36px; padding: 0 16px; border: 1px solid #E2E8F0; border-radius: 6px;
  background: #FFFFFF; color: #475569; font-size: 13px; font-weight: 500;
  cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.2s;
}
.header-action-btn:hover { border-color: #CBD5E1; background: #F8FAFC; color: #0F172A; }
.notification-badge { background: #EF4444; color: #FFF; padding: 0 6px; border-radius: 10px; font-size: 12px; font-weight: 600;}

.detail-main { display: flex; flex-direction: column; gap: 20px; }
.content-card, .replies-section {
  background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 8px;
  padding: 28px 32px; box-shadow: none; transition: opacity 0.3s, border-color 0.3s, box-shadow 0.3s;
}

.entry-notice {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
  padding: 10px 12px;
  border: 1px solid #DDE7F2;
  border-radius: 6px;
  background: #F8FAFC;
}
.entry-notice.is-resolved {
  border-color: #BBF7D0;
  background: #F0FDF4;
}
.entry-notice-icon {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #2563EB;
  font-size: 16px;
}
.entry-notice.is-resolved .entry-notice-icon {
  color: #16A34A;
}
.entry-notice-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.entry-notice-copy strong {
  color: #0F172A;
  font-size: 14px;
  font-weight: 750;
  line-height: 1.35;
}
.entry-notice-copy span {
  color: #64748B;
  font-size: 13px;
  line-height: 1.5;
}
.section-focused { border-color: #CBD5E1; box-shadow: none; }
.section-dimmed { opacity: 0.6; pointer-events: none; }

.detail-heading-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 14px;
}

.detail-title {
  min-width: 0;
  max-width: 760px;
  font-size: 22px;
  line-height: 1.45;
  font-weight: 750;
  color: #0F172A;
  margin: 0;
}

.detail-info { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 18px; }
.course-tag, .badge { height: 24px; padding: 0 8px; border-radius: 4px; display: inline-flex; align-items: center; font-size: 13px; font-weight: 500; }
.course-tag { background: #F1F5F9; border: 1px solid #E2E8F0; color: #475569; }
.badge-hot { background: #FEF3C7; color: #D97706; }
.badge-teacher { background: #ECFCCB; color: #4D7C0F; }
.badge-resolved { background: #DCFCE7; color: #15803D; }
.badge-open { background: #EFF6FF; color: #2563EB; }
.badge-featured { background: #FFF7ED; color: #EA580C; }
.info-sep { color: #CBD5E1; }
.info-item { font-size: 14px; color: #64748B; }

.featured-status-strip, .teacher-action-bar {
  margin-top: 24px; padding: 16px 20px; border-radius: 6px;
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
}
.featured-status-strip { background: #FFFBEB; border: 1px solid #FEF08A; }
.teacher-action-bar { background: #F8FAFC; border: 1px solid #E2E8F0; }

.featured-status-main, .teacher-action-left { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.teacher-action-left { flex-direction: column; align-items: flex-start; gap: 4px; }
.featured-status-badge { height: 26px; padding: 0 10px; border-radius: 4px; background: #FEF3C7; color: #D97706; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; }
.featured-status-text, .teacher-action-label { font-size: 14px; font-weight: 500; color: #1C1917; }
.teacher-action-label { color: #0F172A; }
.teacher-action-hint { font-size: 13px; color: #64748B; }

.featured-status-link, .btn-soft-success, .btn-soft-primary {
  height: 34px; padding: 0 14px; border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer; border: 1px solid transparent;
}
.featured-status-link { background: #FFFFFF; border-color: #FDE047; color: #D97706; }
.featured-status-link:hover { background: #FEF3C7; }
.btn-soft-success { background: #FFFFFF; border-color: #BBF7D0; color: #15803D; }
.btn-soft-success:hover { background: #DCFCE7; }
.btn-soft-primary { background: #0F172A; color: #FFFFFF; }
.btn-soft-primary:hover { background: #334155; }

.detail-body { margin-top: 22px; max-width: 860px; }
.detail-body p { margin: 0 0 14px; font-size: 15px; line-height: 1.75; color: #334155; letter-spacing: 0; }

.detail-stats {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px 12px;
  max-width: 420px;
  padding-top: 4px;
}
.stat-item {
  font-size: 13px;
  color: #64748B;
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}
.stat-icon {
  color: #94A3B8;
}

.replies-header { display: flex; align-items: center; margin-bottom: 24px; }
.replies-title { font-size: 20px; font-weight: 600; color: #0F172A; margin: 0; display: flex; align-items: center; gap: 8px; }
.replies-count { background: #F1F5F9; color: #475569; padding: 2px 10px; border-radius: 12px; font-size: 13px; }

.reply-editor { margin-bottom: 32px; }
.rich-editor-shell {
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
  background: #FFFFFF;
}
.rich-editor-surface { background: #FFFFFF; }
.reply-rich-editor :deep(.w-e-bar),
.excerpt-rich-editor :deep(.w-e-bar) { background: #F8FAFC; }
.reply-rich-editor :deep(.w-e-text-container),
.excerpt-rich-editor :deep(.w-e-text-container) { background: #FFFFFF; }
.reply-rich-editor :deep(.w-e-text-placeholder),
.excerpt-rich-editor :deep(.w-e-text-placeholder) { top: 14px; }
.reply-rich-editor :deep(.w-e-text-container [data-slate-editor]),
.excerpt-rich-editor :deep(.w-e-text-container [data-slate-editor]) { padding: 0 14px; }
.reply-textarea {
  width: 100%; border: 1px solid #E2E8F0; border-radius: 8px; padding: 16px;
  font-size: 15px; line-height: 1.6; color: #0F172A; resize: vertical; transition: 0.2s;
}
.reply-textarea:focus { border-color: #2563EB; outline: none; box-shadow: 0 0 0 2px rgba(37,99,235,0.1); }
.reply-editor-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.reply-tip { font-size: 13px; color: #94A3B8; }
.btn-reply-submit { background: #2563EB; color: #FFF; border: none; height: 38px; padding: 0 20px; border-radius: 6px; font-weight: 500; cursor: pointer; }
.btn-reply-submit:hover { background: #1D4ED8; }

.replies-empty { text-align: center; padding: 48px 0; color: #64748B; border: 1px dashed #CBD5E1; border-radius: 8px; }
.replies-list { display: flex; flex-direction: column; gap: 24px; }

.reply-item { display: flex; gap: 16px; padding-bottom: 24px; border-bottom: 1px solid #E2E8F0; }
.reply-item:last-child { border-bottom: none; padding-bottom: 0; }
.reply-item.is-teacher { padding: 16px; background: #FAFAF9; border: 1px solid #E7E5E4; border-radius: 8px; }

.avatar-char { width: 40px; height: 40px; border-radius: 20px; background: #F1F5F9; color: #475569; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 16px; }
.avatar-char.teacher { background: #ECFCCB; color: #4D7C0F; }

.reply-body { flex: 1; min-width: 0; }
.reply-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.reply-author { font-size: 15px; font-weight: 600; color: #0F172A; }
.teacher-badge { background: #DCFCE7; color: #15803D; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; }
.reply-time { margin-left: auto; font-size: 13px; color: #94A3B8; white-space: nowrap; }
.reply-featured-link { margin-left: 0; background: none; border: none; color: #EA580C; font-size: 13px; font-weight: 500; cursor: pointer; }
.reply-featured-link:hover { text-decoration: underline; }
.reply-content p { margin: 0 0 8px; font-size: 15px; line-height: 1.6; color: #334155; }

.rich-content :deep(p) { margin: 0 0 10px; font-size: 15px; line-height: 1.8; color: #334155; }
.rich-content :deep(ul),
.rich-content :deep(ol) { margin: 0 0 12px 20px; color: #334155; }
.rich-content :deep(li) { line-height: 1.8; margin-bottom: 6px; }
.rich-content :deep(blockquote) {
  margin: 12px 0;
  padding: 10px 14px;
  border-left: 4px solid #BFDBFE;
  background: #F8FAFC;
  color: #475569;
  border-radius: 0 8px 8px 0;
}
.rich-content :deep(pre) {
  margin: 12px 0;
  padding: 14px;
  background: #0F172A;
  color: #E2E8F0;
  border-radius: 8px;
  overflow-x: auto;
}
.rich-content :deep(code) {
  font-family: Consolas, Monaco, monospace;
}
.rich-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

/* 模态框内的表单样式重置为清爽态 */
.form-item { margin-bottom: 16px; }
.form-label { display: block; font-size: 14px; font-weight: 500; color: #334155; margin-bottom: 8px; }
.form-select { width: 100%; border: 1px solid #E2E8F0; border-radius: 6px; padding: 10px 12px; outline: none; }
.form-select:focus { border-color: #2563EB; }
.featured-check { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #334155; margin-bottom: 24px; cursor: pointer; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; }
.btn-cancel { background: #FFFFFF; border: 1px solid #E2E8F0; padding: 8px 16px; border-radius: 6px; color: #475569; cursor: pointer; }
.btn-submit { background: #0F172A; color: #FFF; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; }

@media (max-width: 1100px) {
  .detail-heading-row {
    flex-direction: column;
    gap: 10px;
  }

  .detail-stats {
    justify-content: flex-start;
    max-width: 100%;
    padding-top: 0;
  }
}
</style>
