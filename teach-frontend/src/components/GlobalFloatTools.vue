<template>
  <div class="global-float-tools">
    <div class="float-buttons">
      <a-popover placement="left" trigger="click" overlayClassName="custom-popover">
        <template #title><span class="popover-title">每日打卡</span></template>
        <template #content>
          <div class="pop-checkin-body">
            <div class="checkin-circle" :class="{ done: isCheckedIn }">
              <span class="checkin-day">{{ currentDay }}</span>
            </div>
            <button
              class="btn-checkin-submit"
              :class="{ done: isCheckedIn }"
              :disabled="isCheckedIn || checkInLoading"
              @click="userStore.handleCheckIn"
            >
              {{ isCheckedIn ? '今日已打卡 ✓' : checkInLoading ? '同步中...' : '立即签到' }}
            </button>
          </div>
        </template>
        <template #default>
          <div class="float-btn" :class="{ 'has-dot': !isCheckedIn }">
            <calendar-outlined />
            <span>打卡</span>
          </div>
        </template>
      </a-popover>

      <a-popover placement="left" trigger="click" overlayClassName="custom-popover">
        <template #title><span class="popover-title">待办作业</span></template>
        <template #content>
          <div class="hw-pop-body">
            <template v-if="homeworkLoadFailed">
              <div class="hw-pop-empty">
                <exclamation-circle-outlined style="font-size: 22px; color: #f59e0b;" />
                <p>作业列表加载失败，请稍后刷新重试</p>
              </div>
            </template>
            <template v-else-if="pendingHomework && pendingHomework.length > 0">
              <div v-for="hw in pendingHomework" :key="hw.assignmentId" class="hw-pop-item">
                <div class="hw-info">
                  <div class="hw-pop-title">{{ hw.title || '未命名作业' }}</div>
                  <div class="hw-pop-sub">
                    {{ hw.courseName || '课程作业' }}
                    <template v-if="hw.deadline"> · {{ formatDeadline(hw.deadline) }}</template>
                  </div>
                  <div v-if="hw.teacherNote" class="hw-pop-note">{{ hw.teacherNote }}</div>
                </div>
                <button class="btn-do-hw" @click="goToHomework(hw)">去做</button>
              </div>
            </template>
            <div v-else class="hw-pop-empty">
              <span style="font-size: 24px;">🎉</span>
              <p>太棒了，暂无待办作业</p>
            </div>
          </div>
        </template>
        <template #default>
          <div class="float-btn" :class="{ 'has-dot': pendingHomework && pendingHomework.length > 0 }">
            <bell-outlined />
            <span>作业</span>
          </div>
        </template>
      </a-popover>

      <!-- ===== 新增：作业提醒气泡（紧跟在作业 popover 后面） ===== -->
      <transition name="reminder-pop">
        <div v-if="hasNewReminder" class="hw-reminder-bubble">
          <div class="reminder-bar"></div>
          <div class="reminder-content">
            <div class="reminder-header">
              <svg class="reminder-bell-icon" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M10 2a6 6 0 0 0-6 6v2.586l-1.707 1.707A1 1 0 0 0 3 14h14a1 1 0 0 0 .707-1.707L16 10.586V8a6 6 0 0 0-6-6Z" fill="currentColor" fill-opacity="0.12" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
                <path d="M8 14a2 2 0 1 0 4 0" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
              </svg>
              <span class="reminder-label">作业提醒</span>
              <button class="reminder-close-btn" @click="dismissReminder">
                <svg viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
                </svg>
              </button>
            </div>
            <p class="reminder-text">{{ reminderMessage }}</p>
            <div class="reminder-footer">
              <span class="reminder-time">刚刚</span>
              <button class="reminder-action-btn" @click="dismissReminder">知道了</button>
            </div>
          </div>
        </div>
      </transition>
      <!-- ===== end 作业提醒气泡 ===== -->

      <a-popover placement="left" trigger="click" overlayClassName="custom-popover">
        <template #title><span class="popover-title">待考列表</span></template>
        <template #content>
          <div class="hw-pop-body">
            <template v-if="pendingExams.length > 0">
              <div v-for="exam in pendingExams" :key="exam.assignmentId" class="hw-pop-item">
                <div class="hw-info">
                  <div class="hw-pop-title">{{ exam.title || '未命名考试' }}</div>
                  <div class="hw-pop-sub">
                    {{ exam.questionCount || 0 }} 道题
                    <template v-if="exam.durationMinutes"> · {{ exam.durationMinutes }} 分钟</template>
                    <template v-if="exam.deadline"> · {{ formatDeadline(exam.deadline) }}</template>
                  </div>
                  <div v-if="exam.teacherNote" class="hw-pop-note">{{ exam.teacherNote }}</div>
                </div>
                <button class="btn-do-hw" @click="goToExam(exam)">去考试</button>
              </div>
            </template>
            <div v-else class="hw-pop-empty">
              <span style="font-size: 24px;">📝</span>
              <p>暂无待考考试</p>
            </div>
          </div>
        </template>
        <template #default>
          <div class="float-btn" :class="{ 'has-dot': pendingExams.length > 0 }">
            <file-text-outlined />
            <span>考试</span>
          </div>
        </template>
      </a-popover>

      <div class="float-btn" :class="{ 'has-dot': isTimerRunning }" @click="isTimerModalVisible = true">
        <clock-circle-outlined />
        <span>定时</span>
      </div>

      <div class="float-btn" :class="{ 'is-active': isAiOpen }" @click="toggleAi">
        <robot-outlined />
        <span>AI助手</span>
      </div>

      <div class="float-btn" @click="scrollToTop">
        <arrow-up-outlined />
        <span>顶部</span>
      </div>
    </div>

    <a-modal
      v-model:open="isTimerModalVisible"
      title="专注定时"
      :footer="null"
      width="400px"
      centered
      :bodyStyle="{ padding: '24px' }"
    >
      <div class="timer-modal-body">
        <div class="timer-inputs">
          <div class="input-group">
            <a-input-number v-model:value="timerHours" :min="0" :max="23" class="time-input" />
            <span class="time-unit">时</span>
          </div>
          <span class="time-colon">:</span>
          <div class="input-group">
            <a-input-number v-model:value="timerMinutes" :min="0" :max="59" class="time-input" />
            <span class="time-unit">分</span>
          </div>
        </div>
        <div class="timer-msg-input">
          <a-input v-model:value="timerMessage" placeholder="例如：喝杯水，活动一下！" />
        </div>
        <div class="timer-actions">
          <button v-if="!isTimerRunning" class="btn-timer-cancel" @click="isTimerModalVisible = false">取消</button>
          <button v-if="!isTimerRunning" class="btn-timer-start" @click="startTimer">开始专注</button>
          <button v-else class="btn-timer-stop" @click="cancelTimer">结束专注</button>
        </div>
      </div>
    </a-modal>

    <div class="avatar-float avatar-float-left">
      <transition name="slide-up-left">
        <div v-if="isAvatarOpen" class="avatar-panel">
          <div class="avatar-head">
            <div class="avatar-head-left">
              <div class="avatar-head-icon"><robot-outlined /></div>
              <div class="avatar-title-wrap">
                <span class="avatar-title">数字人助教</span>
                <span class="avatar-status">{{ avatarStatusText }}</span>
              </div>
            </div>
            <button class="avatar-close" @click="closeAvatarPanel"><close-outlined /></button>
          </div>

          <div class="avatar-stage">
            <div ref="avatarWrapperRef" class="avatar-preview"></div>
            <div v-if="!avatarIsInitiated" class="avatar-stage-mask">
              <div class="avatar-stage-icon"><robot-outlined /></div>
              <div class="avatar-stage-title">数字人尚未启动</div>
              <div class="avatar-stage-desc">点击下方按钮启动</div>
            </div>
          </div>

          <div class="avatar-body">
            <textarea
              v-model="avatarTextarea"
              class="avatar-textarea"
              maxlength="1000"
              :disabled="avatarIsThinking || avatarIsTranscribing"
              placeholder="输入问题，数字人会思考后回答…"
              @keydown.enter.exact.prevent="handleAvatarEnter"
            />

            <div class="avatar-actions">
              <button
                class="avatar-btn"
                :class="avatarIsInitiated ? 'avatar-btn-success' : 'avatar-btn-primary'"
                :disabled="avatarIsStarting || (avatarIsInitiated && (avatarIsThinking || avatarIsRecording || avatarIsTranscribing || !avatarTextarea.trim()))"
                @click="handleAvatarPrimaryAction"
              >
                {{ avatarIsStarting ? '正在连接…' : avatarIsInitiated ? (avatarIsThinking ? '思考中…' : '提问并播报') : '启动' }}
              </button>
              <button
                class="avatar-btn avatar-btn-voice"
                :class="{ recording: avatarIsRecording }"
                :disabled="!avatarIsInitiated || avatarIsThinking || avatarIsTranscribing"
                :aria-pressed="avatarIsRecording"
                :title="avatarIsRecording ? '结束语音输入' : '开始语音输入'"
                @click="toggleAvatarVoiceInput"
              >
                <audio-outlined />
                {{ avatarIsRecording ? '结束语音' : '语音输入' }}
              </button>
              <button
                class="avatar-btn avatar-btn-info"
                :disabled="!avatarIsInitiated"
                @click="interrupt"
              >
                打断
              </button>
              <button
                class="avatar-btn avatar-btn-danger"
                :disabled="!avatarIsInitiated"
                @click="stopAvatar"
              >
                关闭
              </button>
            </div>
          </div>
        </div>
      </transition>

      <button class="avatar-fab-pill" @click="toggleAvatar" :class="{ 'is-open': isAvatarOpen }">
        <robot-outlined v-if="!isAvatarOpen" />
        <close-outlined v-else />
      </button>
    </div>

    <transition name="slide-up">
      <div v-if="isAiOpen" class="ai-panel" ref="aiPanelRef" :style="{ right: aiPanelPos.right + 'px', bottom: aiPanelPos.bottom + 'px' }">
        <div class="ai-head" @mousedown="startDrag">
          <div class="ai-head-left">
            <div class="ai-avatar"><robot-outlined /></div>
            <div class="ai-title-wrap">
              <span class="ai-title">AI 讲题助手</span>
              <span class="ai-status">可上传截图或语音提问</span>
            </div>
          </div>
          <button class="ai-close" @click="isAiOpen = false"><close-outlined /></button>
        </div>
        <div ref="msgContainer" class="ai-body">
          <div v-if="chatHistory.length === 0 && !isAiTyping" class="ai-welcome">
            <div class="ai-welcome-icon"><robot-outlined /></div>
            <div class="ai-welcome-copy">
              <h3>你好，我是你的 AI 讲题助手</h3>
              <p>可以把题目截图发给我，也可以直接输入题干、知识点或你的解题思路。我会按步骤拆解，并指出容易卡住的地方。</p>
            </div>
            <div class="ai-welcome-prompts">
              <button type="button" @click="fillAiPrompt('帮我一步步讲解这道题')">逐步讲解题目</button>
              <button type="button" @click="fillAiPrompt('帮我检查这道题的解题思路')">检查解题思路</button>
              <button type="button" @click="fillAiPrompt('帮我总结这类题的解题方法')">总结解题方法</button>
            </div>
          </div>
          <div v-for="(msg, i) in chatHistory" :key="i" class="msg" :class="msg.role">
            <div class="msg-avatar" v-if="msg.role === 'ai'"><robot-outlined /></div>
            <div v-if="msg.role === 'ai'" class="msg-bubble markdown-body" v-html="md.render(msg.content)"></div>
            <div v-else class="msg-bubble">
              <img
                v-if="msg.imageUrl"
                :src="msg.imageUrl"
                alt="题目截图"
                class="msg-image"
                role="button"
                tabindex="0"
                @click="openAiImagePreview(msg.imageUrl)"
                @keydown.enter.prevent="openAiImagePreview(msg.imageUrl)"
                @keydown.space.prevent="openAiImagePreview(msg.imageUrl)"
              />
              <span>{{ msg.content }}</span>
            </div>
          </div>
          <div v-if="isAiTyping" class="msg ai">
            <div class="msg-avatar"><robot-outlined /></div>
            <div class="msg-bubble typing"><span /><span /><span /></div>
          </div>
        </div>
        <div class="ai-compose">
          <div v-if="selectedImage" class="image-preview-card">
            <img :src="selectedImage.url" alt="题目截图预览" />
            <div class="image-preview-meta">
              <span>{{ selectedImage.name }}</span>
              <button type="button" @click="removeSelectedImage" :disabled="isAiTyping">
                <delete-outlined />
              </button>
            </div>
          </div>

          <div class="voice-state" v-if="isAiRecording || isVoiceTranscribing">
            <span v-if="isAiRecording">{{ isRealtimeSpeechActive ? '实时识别中' : '录音中' }} {{ formattedAiRecordTime }}</span>
            <span v-else>正在识别语音...</span>
          </div>

          <div class="ai-input-row">
            <input
              ref="imageInputRef"
              class="ai-file-input"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              @change="handleAiImageSelect"
            />
            <button class="tool-btn" type="button" :disabled="isAiTyping || isAiRecording" @click="triggerAiImageSelect">
              <paper-clip-outlined />
            </button>
            <textarea
              ref="aiTextareaRef"
              v-model="userQuery"
              :disabled="isAiTyping || isVoiceTranscribing || isAiRecording"
              placeholder="输入问题，或上传题目截图..."
              rows="1"
            />
            <div class="ai-input-actions">
              <button
                class="tool-btn"
                type="button"
                :class="{ recording: isAiRecording }"
                :disabled="isAiTyping || isVoiceTranscribing"
                @click="toggleAiRecording"
              >
                <audio-outlined />
              </button>
              <button
                class="btn-send"
                type="button"
                @click="sendAiMessage"
                :disabled="isAiTyping || isVoiceTranscribing || isAiRecording || (!userQuery.trim() && !selectedImage)"
              >
                <send-outlined />
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <a-modal
      v-model:open="isAiImagePreviewOpen"
      class="ai-image-preview-modal"
      :footer="null"
      :width="860"
      :z-index="3000"
      centered
      destroyOnClose
      @after-close="aiPreviewImageUrl = ''"
    >
      <img v-if="aiPreviewImageUrl" :src="aiPreviewImageUrl" alt="题目截图预览" class="ai-preview-image" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { message, Modal } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { useTutorContextStore, type TutorMode } from '@/stores/tutorContext'
import request from '@/utils/request'
import { getAuthToken, getLoginUserRaw } from '@/utils/authStorage'
import { getAvatarSession, type AvatarSessionConfig } from '@/api/avatar'

import {
  AudioOutlined,
  BellOutlined,
  CloseOutlined,
  DeleteOutlined,
  RobotOutlined,
  SendOutlined,
  CalendarOutlined,
  ArrowUpOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  FileTextOutlined,
  PaperClipOutlined
} from '@ant-design/icons-vue'

import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import AvatarPlatform, {
  PlayerEvents,
  SDKEvents
} from '@/vm-sdk/avatar-sdk-web_3.2.3.1002/esm/index.js'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'

const router = useRouter()
const userStore = useUserStore()
const tutorContext = useTutorContextStore()
const { isCheckedIn, checkInLoading } = storeToRefs(userStore)

const today = new Date()
const currentDay = today.getDate()

const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

// ====== 作业相关：改为从后端拉取 ======
const pendingHomework = ref<any[]>([])
const homeworkLoadFailed = ref(false)

const fetchHomeworkTasks = async () => {
  homeworkLoadFailed.value = false

  try {
    const data = await request.get('/homework/student/pending', {
      skipErrorToast: true,
    })

    pendingHomework.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.warn('获取待办作业失败：', e)
    pendingHomework.value = []
    homeworkLoadFailed.value = true
  }
}

const formatDeadline = (deadline: string | Date) => {
  if (!deadline) return ''
  const d = new Date(deadline)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hours = String(d.getHours()).padStart(2, '0')
  const mins = String(d.getMinutes()).padStart(2, '0')
  return `${month}/${day} ${hours}:${mins} 前`
}

const goToHomework = (hw: any) => {
  message.loading(`正在进入作业...`, 0.5)
  setTimeout(() => router.push(`/student/homework/${hw.assignmentId}`), 500)
}

// ====== 考试相关 ======
const pendingExams = ref<any[]>([])

const fetchExamTasks = async () => {
  try {
    const data = await request.get('/exam/student/pending', {
      skipErrorToast: true,
    })
    pendingExams.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.warn('获取待考列表失败：', e)
    pendingExams.value = []
  }
}

const goToExam = (exam: any) => {
  message.loading(`正在进入考试...`, 0.5)
  setTimeout(() => router.push(`/student/exam/${exam.assignmentId}`), 500)
}

// ====== 定时专注相关（不变） ======
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

// ====== AI 场景化助教 ======
const isAiOpen = ref(false)
const isAiTyping = ref(false)
const userQuery = ref('')
const msgContainer = ref<HTMLElement | null>(null)
const aiTextareaRef = ref<HTMLTextAreaElement | null>(null)

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' + hljs.highlight(str, { language: lang, ignoreIllegals: true }).value + '</code></pre>'
      } catch (__) {}
    }
    return ''
  }
})

type ChatMessage = {
  role: 'ai' | 'user'
  content: string
  imageUrl?: string
}

const chatHistory = ref<ChatMessage[]>([])
const imageInputRef = ref<HTMLInputElement | null>(null)
const selectedImage = ref<{ file: File; url: string; name: string } | null>(null)
const isAiImagePreviewOpen = ref(false)
const aiPreviewImageUrl = ref('')
const isVoiceTranscribing = ref(false)
const isAiRecording = ref(false)
const isRealtimeSpeechActive = ref(false)
const aiRecordSeconds = ref(0)
const MAX_AI_IMAGE_SIZE_MB = 8
const MAX_AI_RECORD_SECONDS = 60
let aiMediaRecorder: MediaRecorder | null = null
let aiRecordStream: MediaStream | null = null
let aiRecordChunks: BlobPart[] = []
let aiRecordTimer: ReturnType<typeof setInterval> | null = null
let shouldTranscribeAiRecord = true
let aiSpeechRecognition: any = null
let aiSpeechStopRequested = false
let aiRealtimeVoiceBaseText = ''
let aiRealtimeFinalText = ''
let preferRealtimeSpeech = true

const formattedAiRecordTime = computed(() => {
  const m = Math.floor(aiRecordSeconds.value / 60).toString().padStart(2, '0')
  const s = (aiRecordSeconds.value % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

const toggleAi = () => {
  isAiOpen.value = !isAiOpen.value
  if (isAiOpen.value) {
    scrollBottom()
    nextTick(() => aiTextareaRef.value?.focus())
  }
}

// ====== AI 面板拖拽相关 ======
const aiPanelRef = ref<HTMLElement | null>(null) // 绑定面板 DOM
const aiPanelPos = ref({ right: 90, bottom: 40 })

let isAiDragging = false
let dragStartX = 0
let dragStartY = 0
let dragStartRight = 0
let dragStartBottom = 0

// 记录面板的宽高，用于边界计算
let panelWidth = 380
let panelHeight = 560

const startDrag = (e: MouseEvent) => {
  if ((e.target as HTMLElement).closest('.ai-close')) return

  isAiDragging = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragStartRight = aiPanelPos.value.right
  dragStartBottom = aiPanelPos.value.bottom

  // 动态获取当前面板的实际宽高（增加容错）
  if (aiPanelRef.value) {
    panelWidth = aiPanelRef.value.offsetWidth
    panelHeight = aiPanelRef.value.offsetHeight
  }

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e: MouseEvent) => {
  if (!isAiDragging) return
  e.preventDefault()

  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY

  let newRight = dragStartRight - dx
  let newBottom = dragStartBottom - dy

  // --- 核心边界计算 ---
  // 最大 right 值 = 屏幕宽度 - 面板宽度
  const maxRight = window.innerWidth - panelWidth
  // 最大 bottom 值 = 屏幕高度 - 面板高度
  const maxBottom = window.innerHeight - panelHeight

  // 限制 right：最小不能小于 0（超出右边），最大不能大于 maxRight（超出左边）
  newRight = Math.max(0, Math.min(newRight, maxRight))

  // 限制 bottom：最小不能小于 0（超出下边），最大不能大于 maxBottom（超出上边）
  newBottom = Math.max(0, Math.min(newBottom, maxBottom))

  // 更新位置
  aiPanelPos.value.right = newRight
  aiPanelPos.value.bottom = newBottom
}

const stopDrag = () => {
  isAiDragging = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const scrollBottom = () => {
  nextTick(() => {
    if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  })
}

const fillAiPrompt = (prompt: string) => {
  userQuery.value = prompt
  nextTick(() => aiTextareaRef.value?.focus())
}

const triggerAiImageSelect = () => {
  imageInputRef.value?.click()
}

const removeSelectedImage = () => {
  if (selectedImage.value?.url) {
    URL.revokeObjectURL(selectedImage.value.url)
  }
  selectedImage.value = null
  if (imageInputRef.value) {
    imageInputRef.value.value = ''
  }
}

const openAiImagePreview = (imageUrl?: string) => {
  if (!imageUrl) return
  aiPreviewImageUrl.value = imageUrl
  isAiImagePreviewOpen.value = true
}

const handleAiImageSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    message.warning('仅支持 JPG、PNG、WebP 图片')
    input.value = ''
    return
  }

  if (file.size > MAX_AI_IMAGE_SIZE_MB * 1024 * 1024) {
    message.warning(`图片不能超过 ${MAX_AI_IMAGE_SIZE_MB}MB`)
    input.value = ''
    return
  }

  if (selectedImage.value?.url) {
    URL.revokeObjectURL(selectedImage.value.url)
  }

  selectedImage.value = {
    file,
    url: URL.createObjectURL(file),
    name: file.name || '题目截图'
  }
}

const sendAiMessage = () => {
  const q = userQuery.value.trim()
  const image = selectedImage.value
  if (!q && !image) return
  userQuery.value = ''

  if (image) {
    selectedImage.value = null
    if (imageInputRef.value) {
      imageInputRef.value.value = ''
    }
    sendTutorVisionMessage(q, image.file, image.url)
    return
  }

  sendTutorMessage(q, 'explain')
}

const sendTutorMessage = async (q: string, mode: TutorMode = 'explain') => {
  if (isAiTyping.value) return
  chatHistory.value.push({ role: 'user', content: q })
  scrollBottom()

  const idx = chatHistory.value.push({ role: 'ai', content: '' }) - 1
  isAiTyping.value = true

  try {
    const token = getAuthToken()
    const res = await fetch(`${API_BASE_URL}/ai/tutor/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      credentials: 'include',
      body: JSON.stringify({
        message: q,
        mode,
        source: 'ai_assistant',
        context: tutorContext.requestContext
      })
    })

    if (!res.ok) throw new Error(`AI 请求失败：${res.status}`)

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
    void speakAvatarText(chatHistory.value[idx].content)
  } catch {
    chatHistory.value[idx].content = '网络开了小差，请稍后再试。'
    isAiTyping.value = false
  }
}

const sendTutorVisionMessage = async (q: string, file: File, imageUrl: string) => {
  if (isAiTyping.value) return
  const displayText = q || '请解析这张题目截图。'
  chatHistory.value.push({ role: 'user', content: displayText, imageUrl })
  scrollBottom()

  const idx = chatHistory.value.push({ role: 'ai', content: '' }) - 1
  isAiTyping.value = true

  try {
    const token = getAuthToken()
    const formData = new FormData()
    formData.append('file', file)
    formData.append('message', q)
    formData.append('context', JSON.stringify(tutorContext.requestContext))

    const res = await fetch(`${API_BASE_URL}/ai/tutor/vision/stream`, {
      method: 'POST',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      credentials: 'include',
      body: formData
    })

    if (!res.ok) throw new Error(`图片解析失败：${res.status}`)

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
    void speakAvatarText(chatHistory.value[idx].content)
  } catch {
    chatHistory.value[idx].content = '图片解析失败，请确认图片清晰后再试。'
    isAiTyping.value = false
  }
}

const toggleAiRecording = async () => {
  if (isAiRecording.value) {
    stopAiRecording()
    return
  }
  if (preferRealtimeSpeech && startRealtimeSpeechRecognition()) {
    return
  }
  await startAiUploadRecording()
}

const getSpeechRecognitionCtor = () => {
  const win = window as any
  return win.SpeechRecognition || win.webkitSpeechRecognition
}

const clearAiRecordTimer = () => {
  if (aiRecordTimer) {
    clearInterval(aiRecordTimer)
    aiRecordTimer = null
  }
}

const startAiRecordTimer = () => {
  clearAiRecordTimer()
  aiRecordSeconds.value = 0
  aiRecordTimer = setInterval(() => {
    aiRecordSeconds.value += 1
    if (aiRecordSeconds.value >= MAX_AI_RECORD_SECONDS) {
      stopAiRecording()
    }
  }, 1000)
}

const startRealtimeSpeechRecognition = () => {
  const SpeechRecognitionCtor = getSpeechRecognitionCtor()
  if (!SpeechRecognitionCtor) {
    return false
  }

  try {
    aiSpeechStopRequested = false
    aiRealtimeVoiceBaseText = userQuery.value.trim()
    aiRealtimeFinalText = ''

    const recognition = new SpeechRecognitionCtor()
    recognition.lang = 'zh-CN'
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    recognition.onstart = () => {
      aiSpeechRecognition = recognition
      isRealtimeSpeechActive.value = true
      isAiRecording.value = true
      startAiRecordTimer()
    }

    recognition.onresult = (event: any) => {
      let interimText = ''
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const result = event.results[i]
        const text = result?.[0]?.transcript?.trim() || ''
        if (!text) continue
        if (result.isFinal) {
          aiRealtimeFinalText = `${aiRealtimeFinalText}${text}`
        } else {
          interimText += text
        }
      }

      const liveText = `${aiRealtimeFinalText}${interimText}`.trim()
      userQuery.value = [aiRealtimeVoiceBaseText, liveText].filter(Boolean).join(' ')
    }

    recognition.onerror = (event: any) => {
      const error = event?.error || ''
      if (error === 'not-allowed' || error === 'service-not-allowed') {
        message.error('无法获取麦克风权限，请检查浏览器设置')
      } else if (error !== 'no-speech') {
        preferRealtimeSpeech = false
        message.warning('实时语音识别暂不可用，下次将使用普通录音识别')
      }
    }

    recognition.onend = () => {
      clearAiRecordTimer()
      isAiRecording.value = false
      isRealtimeSpeechActive.value = false
      aiSpeechRecognition = null

      if (!aiSpeechStopRequested && !aiRealtimeFinalText.trim()) {
        message.warning('没有识别到有效语音')
      }
      aiSpeechStopRequested = false
    }

    recognition.start()
    return true
  } catch {
    aiSpeechRecognition = null
    isRealtimeSpeechActive.value = false
    isAiRecording.value = false
    return false
  }
}

const startAiUploadRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    aiRecordStream = stream
    aiMediaRecorder = new MediaRecorder(stream)
    aiRecordChunks = []

    aiMediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        aiRecordChunks.push(event.data)
      }
    }

    aiMediaRecorder.onstop = async () => {
      aiRecordStream?.getTracks().forEach(track => track.stop())
      aiRecordStream = null
      clearAiRecordTimer()
      isAiRecording.value = false

      if (!shouldTranscribeAiRecord) {
        shouldTranscribeAiRecord = true
        return
      }

      if (!aiRecordChunks.length) {
        message.warning('没有录到有效语音')
        return
      }

      const audioBlob = new Blob(aiRecordChunks, { type: 'audio/webm' })
      const audioFile = new File([audioBlob], `Tutor_Voice_${Date.now()}.webm`, { type: 'audio/webm' })
      await transcribeAiVoice(audioFile)
    }

    aiMediaRecorder.start()
    isRealtimeSpeechActive.value = false
    isAiRecording.value = true
    startAiRecordTimer()
  } catch {
    message.error('无法获取麦克风权限，请检查浏览器设置')
  }
}

const stopAiRecording = (transcribe = true) => {
  if (aiSpeechRecognition) {
    aiSpeechStopRequested = true
    aiSpeechRecognition.stop()
    return
  }

  shouldTranscribeAiRecord = transcribe
  if (aiMediaRecorder && aiMediaRecorder.state !== 'inactive') {
    aiMediaRecorder.stop()
  }
}

const transcribeAiVoice = async (audioFile: File) => {
  isVoiceTranscribing.value = true
  try {
    const formData = new FormData()
    formData.append('file', audioFile)
    const text = await request.post<string, string>('/ai/tutor/speech-to-text', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 0,
      skipErrorToast: true
    })
    userQuery.value = [userQuery.value.trim(), text].filter(Boolean).join(' ')
    message.success('语音已转成文字')
  } catch (error: any) {
    message.error(error?.message || '语音识别失败，请稍后再试')
  } finally {
    isVoiceTranscribing.value = false
  }
}

// ====== 数字人相关（完全不变） ======
const isAvatarOpen = ref(false)
const avatarWrapperRef = ref<HTMLElement | null>(null)
const avatarTextarea = ref('')
const avatarIsInitiated = ref(false)
const avatarIsStarting = ref(false)
const avatarIsThinking = ref(false)
const avatarIsRecording = ref(false)
const avatarIsTranscribing = ref(false)
const avatarAnswer = ref('')
const avatarStatusText = computed(() => {
  if (avatarIsStarting.value) return '正在连接…'
  if (avatarIsThinking.value) return '正在思考并组织回答…'
  if (avatarIsRecording.value) return '正在聆听你的问题…'
  if (avatarIsTranscribing.value) return '正在识别语音…'
  return avatarIsInitiated.value ? '已连接，可语音或文字提问' : '待启动'
})
const avatarGlobalParams = {
  stream: { protocol: 'xrtc', fps: 25, bitrate: 1000000, alpha: false },
  avatar: { avatar_id: '', width: 920, height: 1180, scale: 1, move_h: 0, move_v: 0, audio_format: 1 },
  tts: { vcn: '', speed: 45, pitch: 55, volume: 100 },
  avatar_dispatch: { interactive_mode: 1 },
  subtitle: { subtitle: 1, font_color: '#FFFFFF' },
  enable: false,
  background: {
    type: 'res_key',
    data: '22SLM2teIw+aqR6Xsm2JbH6Ng310kDam2NiCY/RQ9n6dw47gMO+7gGUJfWWfkqD3IxsU/HMK1uJTTxxF2llcKSM4dlSdBy0Piag/DndHocqs32kTOwXUw6lkyggYQBXF0uwTv9jVFm1ZjZgSehV3kpx5RTvizZ9MqEI8lotCRvokC9HLI0pGfKtSmlKgCKL+OUoc9QI5HW3wLtYbLersumd4UCKEPk/uWAdKEh4ntSJiW2km8waGFsg/VSNFj5vaDK3LC4PxfsRvi1a2veZW7JUs/VOleE9wwgTH+A/oqPPcyksBY7aQ4TxYjvS9Qj9LtXkvOwttQMgPGwoxlqBEBhR/xLUwmecHkHzgjACFtxE='
  },
  air: { air: 1, add_nonsemantic: 1 }
}

let avatarPlatformInstance: any = null
let avatarSessionConfig: AvatarSessionConfig | null = null
let avatarSpeechRecognition: any = null
let avatarMediaRecorder: MediaRecorder | null = null
let avatarRecordStream: MediaStream | null = null
let avatarRecordChunks: BlobPart[] = []
let avatarVoiceBaseText = ''
let avatarVoiceShouldSubmit = false
let avatarStreamReady = false
let avatarRuntimeFailure: Error | null = null
let avatarIntentionalStop = false
const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))
const toggleAvatar = () => {
  if (isAvatarOpen.value) {
    closeAvatarPanel()
    return
  }
  isAvatarOpen.value = true
}

const initAvatarSDK = () => { if (!avatarPlatformInstance) { avatarPlatformInstance = new AvatarPlatform() } }

const getAvatarRuntimeError = (error: any, fallback: string) => {
  return new Error(error?.message || error?.msg || error?.error_message || error?.data?.message || fallback)
}

const markAvatarStreamReady = () => {
  avatarStreamReady = true
  avatarRuntimeFailure = null
}

const markAvatarRuntimeFailure = (error: any, fallback: string) => {
  if (avatarIntentionalStop) return
  avatarStreamReady = false
  avatarRuntimeFailure = getAvatarRuntimeError(error, fallback)
  const wasReady = avatarIsInitiated.value
  avatarIsInitiated.value = false
  if (wasReady && isAvatarOpen.value) {
    message.error(`${avatarRuntimeFailure.message}，请点击“启动”重新连接`)
  }
}

const waitForAvatarPlayer = async (timeoutMs = 20_000) => {
  const deadline = Date.now() + timeoutMs
  while (!avatarStreamReady && Date.now() < deadline) {
    if (avatarRuntimeFailure) throw avatarRuntimeFailure
    await wait(100)
  }
  if (avatarRuntimeFailure) throw avatarRuntimeFailure
  if (!avatarStreamReady) {
    throw new Error('数字人视频流连接超时')
  }
}

const setAvatarSDKEvent = () => {
  if (!avatarPlatformInstance) return
  avatarPlatformInstance
    .on(SDKEvents.connected, (initResp: unknown) => { console.log('SDKEvent.connect:initResp:', initResp) })
    .on(SDKEvents.stream_start, () => console.log('stream_start'))
    .on(SDKEvents.disconnected, (err: unknown) => {
      console.error('SDKEvent.disconnected:', err)
      markAvatarRuntimeFailure(err, '数字人服务连接已断开')
    })
    .on(SDKEvents.nlp, (nlpData: unknown) => console.log('语义理解内容nlp:', nlpData))
    .on(SDKEvents.frame_start, (frameStart: unknown) => console.log('推流开始frame_start:', frameStart))
    .on(SDKEvents.frame_stop, (frameStop: unknown) => console.log('推流结束frame_stop:', frameStop))
    .on(SDKEvents.error, (error: unknown) => {
      console.error('错误信息error:', error)
      if (avatarIsStarting.value || !avatarIsInitiated.value) {
        markAvatarRuntimeFailure(error, '数字人服务返回异常')
      }
    })
    .on(SDKEvents.asr, (asrData: unknown) => console.log('语音识别数据asr:', asrData))
    .on(SDKEvents.tts_duration, (ttsData: unknown) => console.log('语音合成用时tts：', ttsData))
    .on(SDKEvents.subtitle_info, (subtitleData: unknown) => console.log('subtitleData：', subtitleData))
    .on(SDKEvents.action_start, (actionStart: unknown) => console.log('动作推流开始action_start:', actionStart))
    .on(SDKEvents.action_stop, (actionStop: unknown) => console.log('动作推流结束action_stop：', actionStop))
}

const setAvatarPlayerEvent = () => {
  if (!avatarPlatformInstance?.createPlayer) return
  const player = avatarPlatformInstance.createPlayer()
  player.defaultMuted = false
  player
    .on(PlayerEvents.play, () => console.log('play'))
    .on(PlayerEvents.playing, () => {
      console.log('playing')
      markAvatarStreamReady()
    })
    .on(PlayerEvents.waiting, () => console.log('waiting'))
    .on(PlayerEvents.stop, () => {
      console.log('stop')
      if (avatarIsInitiated.value) {
        markAvatarRuntimeFailure(null, '数字人视频流已停止')
      }
    })
    .on(PlayerEvents.error, (error: unknown) => {
      console.error('player error:', error)
      markAvatarRuntimeFailure(error, '数字人播放器启动失败')
    })
    .on('not-allowed', () => { console.log('触发自动播放限制，等待用户恢复播放'); })
}

const setAvatarApiInfo = (config: AvatarSessionConfig) => {
  if (!avatarPlatformInstance) return
  avatarPlatformInstance.setApiInfo({
    appId: config.appId,
    sceneId: config.sceneId,
    signedUrl: config.signedUrl,
  })
}

const setAvatarGlobalParams = (config: AvatarSessionConfig) => {
  if (!avatarPlatformInstance) return
  const params: Record<string, any> = JSON.parse(JSON.stringify(avatarGlobalParams))
  if (params.enable === false) { delete params.background; delete params.enable }
  params.stream.alpha = params.stream.alpha ? 1 : 0
  params.avatar.avatar_id = config.avatarId
  params.tts.vcn = config.voiceName
  avatarPlatformInstance.setGlobalParams(params)
}

const autoPlayAvatarWelcome = () => {
  const welcomeText = avatarSessionConfig?.welcomeText?.trim()
  if (!avatarPlatformInstance || !welcomeText) return
  setTimeout(() => {
    void speakAvatarText(welcomeText)
  }, 1000)
}

const startAvatarStream = async () => {
  if (!avatarPlatformInstance) {
    message.error('数字人实例不存在，请先初始化')
    return
  }

  if (!avatarWrapperRef.value) {
    message.error('数字人容器不存在，请检查 ref 是否绑定成功')
    return
  }

  try {
    console.log('数字人实例：', avatarPlatformInstance)
    console.log('容器元素：', avatarWrapperRef.value)

    avatarStreamReady = false
    avatarRuntimeFailure = null
    avatarWrapperRef.value.replaceChildren()

    await avatarPlatformInstance.start({
      wrapper: avatarWrapperRef.value
    })

    await waitForAvatarPlayer()

    avatarIsInitiated.value = true
    autoPlayAvatarWelcome()
    message.success('数字人已启动')
  } catch (error: any) {
    console.error('数字人启动真实错误：', error)
    throw new Error(
      error?.message || error?.msg || error?.data?.message || '数字人启动失败，请查看控制台错误'
    )
  }
}

const initAndStartAvatar = async (config: AvatarSessionConfig) => {
  initAvatarSDK(); await wait(200); setAvatarSDKEvent(); await wait(200)
  setAvatarPlayerEvent(); await wait(200); setAvatarApiInfo(config); await wait(200); setAvatarGlobalParams(config); await wait(200); await startAvatarStream()
}

const startAvatar = async () => {
  isAvatarOpen.value = true
  await nextTick()
  if (avatarIsInitiated.value || avatarIsStarting.value) return

  if (avatarPlatformInstance) {
    destroyAvatar()
  }

  avatarIsStarting.value = true
  try {
    const config = await getAvatarSession()
    avatarSessionConfig = config
    await initAndStartAvatar(config)
  } catch (error: any) {
    destroyAvatar()
    message.error(error?.message || '数字人启动失败，请检查服务配置后重试')
  } finally {
    avatarIsStarting.value = false
  }
}

const normalizeAvatarSpeechText = (value: string) => value
  .replace(/```[\s\S]*?```/g, ' 下面是代码示例，请查看对话框中的完整内容。 ')
  .replace(/`([^`]+)`/g, '$1')
  .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
  .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
  .replace(/^#{1,6}\s+/gm, '')
  .replace(/[*_~>|]/g, '')
  .replace(/\s+/g, ' ')
  .trim()
  .slice(0, 2000)

const speakAvatarText = async (value: string) => {
  if (!avatarPlatformInstance || !avatarIsInitiated.value) return
  const text = normalizeAvatarSpeechText(value)
  if (!text) return

  try {
    await avatarPlatformInstance.writeText(text, {
      nlp: false,
      tts: { vcn: avatarSessionConfig?.voiceName, volume: 100 },
      avatar_dispatch: { interactive_mode: 1 },
      air: { air: 1, add_nonsemantic: 1 },
    })
  } catch (error) {
    console.warn('数字人播报失败：', error)
  }
}

const askAvatar = async () => {
  if (!avatarPlatformInstance || !avatarIsInitiated.value || avatarIsThinking.value) return
  const question = avatarTextarea.value.trim()
  if (!question) {
    message.warning('请先输入要问数字人的问题')
    return
  }

  avatarTextarea.value = ''
  avatarAnswer.value = ''
  avatarIsThinking.value = true

  try {
    const token = getAuthToken()
    const response = await fetch(`${API_BASE_URL}/ai/tutor/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      credentials: 'include',
      body: JSON.stringify({
        message: question,
        mode: 'explain',
        source: 'avatar',
        context: tutorContext.requestContext
      })
    })

    if (!response.ok) throw new Error(`AI 请求失败：${response.status}`)
    const reader = response.body?.getReader()
    if (!reader) throw new Error('未获取到 AI 回答流')

    const decoder = new TextDecoder('utf-8')
    let answer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      answer += decoder.decode(value, { stream: true })
      avatarAnswer.value = answer
    }
    answer += decoder.decode()
    avatarAnswer.value = answer.trim()

    if (!avatarAnswer.value) throw new Error('AI 未返回有效内容')
    await speakAvatarText(avatarAnswer.value)
  } catch (error: any) {
    avatarTextarea.value = question
    avatarAnswer.value = ''
    message.error(error?.message || 'AI 回答失败，请稍后再试')
  } finally {
    avatarIsThinking.value = false
  }
}

const handleAvatarEnter = (event: KeyboardEvent) => {
  if (event.isComposing) return
  void askAvatar()
}

const handleAvatarPrimaryAction = () => {
  if (avatarIsInitiated.value) {
    void askAvatar()
    return
  }
  void startAvatar()
}

const finishAvatarVoiceState = () => {
  avatarIsRecording.value = false
  avatarSpeechRecognition = null
}

const startAvatarBrowserSpeechRecognition = () => {
  const SpeechRecognitionCtor = getSpeechRecognitionCtor()
  if (!SpeechRecognitionCtor) return false

  try {
    avatarVoiceBaseText = avatarTextarea.value.trim()
    let finalText = ''
    const recognition = new SpeechRecognitionCtor()
    recognition.lang = 'zh-CN'
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    recognition.onstart = () => {
      avatarVoiceShouldSubmit = false
      avatarSpeechRecognition = recognition
      avatarIsRecording.value = true
    }
    recognition.onresult = (event: any) => {
      let interimText = ''
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const result = event.results[i]
        const text = result?.[0]?.transcript?.trim() || ''
        if (!text) continue
        if (result.isFinal) finalText += text
        else interimText += text
      }
      const speechText = `${finalText}${interimText}`.trim()
      avatarTextarea.value = [avatarVoiceBaseText, speechText].filter(Boolean).join(' ')
    }
    recognition.onerror = (event: any) => {
      const error = event?.error || ''
      if (error === 'not-allowed' || error === 'service-not-allowed') {
        message.error('无法获取麦克风权限，请在浏览器设置中允许麦克风')
      } else if (error !== 'no-speech' && error !== 'aborted') {
        message.warning('语音识别暂时不可用，请重试')
      }
    }
    recognition.onend = () => {
      const shouldSubmit = avatarVoiceShouldSubmit
      avatarVoiceShouldSubmit = false
      finishAvatarVoiceState()
      if (!avatarTextarea.value.trim()) {
        message.warning('没有识别到有效语音')
        return
      }
      if (shouldSubmit) void askAvatar()
    }
    recognition.start()
    return true
  } catch {
    finishAvatarVoiceState()
    return false
  }
}

const transcribeAvatarVoice = async (audioFile: File) => {
  avatarIsTranscribing.value = true
  let recognized = false
  try {
    const formData = new FormData()
    formData.append('file', audioFile)
    const text = await request.post<string, string>('/ai/tutor/speech-to-text', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 0,
      skipErrorToast: true
    })
    avatarTextarea.value = [avatarTextarea.value.trim(), text?.trim()].filter(Boolean).join(' ')
    recognized = Boolean(text?.trim())
  } catch (error: any) {
    message.error(error?.message || '语音识别失败，请稍后再试')
  } finally {
    avatarIsTranscribing.value = false
  }
  return recognized
}

const startAvatarUploadRecording = async () => {
  if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
    message.error('当前浏览器不支持语音输入，请使用最新版 Chrome 或 Edge')
    return
  }

  try {
    avatarRecordStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    avatarRecordChunks = []
    avatarMediaRecorder = new MediaRecorder(avatarRecordStream)
    avatarMediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) avatarRecordChunks.push(event.data)
    }
    avatarMediaRecorder.onstop = async () => {
      const shouldSubmit = avatarVoiceShouldSubmit
      avatarVoiceShouldSubmit = false
      avatarRecordStream?.getTracks().forEach(track => track.stop())
      avatarRecordStream = null
      avatarIsRecording.value = false
      if (!avatarRecordChunks.length) {
        if (shouldSubmit) message.warning('没有录到有效语音')
        return
      }
      if (!shouldSubmit) return
      const audioBlob = new Blob(avatarRecordChunks, { type: 'audio/webm' })
      const audioFile = new File([audioBlob], `Avatar_Voice_${Date.now()}.webm`, { type: 'audio/webm' })
      const recognized = await transcribeAvatarVoice(audioFile)
      if (recognized && shouldSubmit) await askAvatar()
    }
    avatarVoiceShouldSubmit = false
    avatarMediaRecorder.start()
    avatarIsRecording.value = true
  } catch {
    avatarRecordStream?.getTracks().forEach(track => track.stop())
    avatarRecordStream = null
    avatarIsRecording.value = false
    message.error('无法获取麦克风权限，请在浏览器设置中允许麦克风')
  }
}

const stopAvatarVoiceInput = (submitAfterRecognition = true) => {
  avatarVoiceShouldSubmit = submitAfterRecognition
  if (avatarSpeechRecognition) {
    avatarSpeechRecognition.stop()
    return
  }
  if (avatarMediaRecorder?.state && avatarMediaRecorder.state !== 'inactive') {
    avatarMediaRecorder.stop()
  }
}

const toggleAvatarVoiceInput = async () => {
  if (avatarIsRecording.value) {
    stopAvatarVoiceInput(true)
    return
  }
  if (startAvatarBrowserSpeechRecognition()) return
  await startAvatarUploadRecording()
}

const interrupt = () => { if (!avatarPlatformInstance || !avatarIsInitiated.value) return; avatarPlatformInstance.interrupt() }
const stopAvatar = () => {
  if (!avatarPlatformInstance) return
  destroyAvatar()
  message.success('数字人已关闭')
}

const closeAvatarPanel = () => {
  isAvatarOpen.value = false
  stopAvatarVoiceInput(false)
  if (avatarPlatformInstance) destroyAvatar()
}

const destroyAvatar = () => {
  avatarIntentionalStop = true
  try { avatarPlatformInstance?.stop?.(); avatarPlatformInstance?.destroy?.() }
  catch (error) { console.warn('销毁数字人实例时出现异常：', error) }
  finally {
    avatarWrapperRef.value?.replaceChildren()
    avatarPlatformInstance = null
    avatarSessionConfig = null
    avatarStreamReady = false
    avatarRuntimeFailure = null
    avatarIsInitiated.value = false
    avatarIsStarting.value = false
    avatarIsThinking.value = false
    avatarIntentionalStop = false
  }
}

// ====== 新增：作业提醒轮询 ======
const hasNewReminder = ref(false)
const reminderMessage = ref('')
const REMINDER_CHECK_KEY = 'smartedu_last_reminder_check'
let reminderPollTimer: ReturnType<typeof setInterval> | null = null

const dismissReminder = () => {
  hasNewReminder.value = false
}

// ✅ 修复：从本地缓存读取 studentId，作为请求参数传给后端（避免依赖 Session）
const getLocalStudentId = (): number | null => {
  try {
    const raw = getLoginUserRaw()
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (parsed?.userRole !== 'student') return null
    return parsed?.id ?? null
  } catch {
    return null
  }
}

const checkHomeworkReminder = async () => {
  try {
    const studentId = getLocalStudentId()
    if (!studentId) return  // 未登录或非学生，静默跳过

    const lastCheckedAt = Number(localStorage.getItem(REMINDER_CHECK_KEY) || '0')
    const data = await request.get('/notification/check-homework-reminder', {
      params: { lastCheckedAt },
      skipErrorToast: true
    }) as any
    if (data?.hasNew) {
      reminderMessage.value = data.message || '请及时完成作业！'
      hasNewReminder.value = true
      // 更新时间戳，防止同一条提醒重复弹出
      localStorage.setItem(REMINDER_CHECK_KEY, String(data.remindAt))
      // 10 秒后自动收起
      setTimeout(() => { hasNewReminder.value = false }, 10_000)
    }
  } catch {
    // 静默：轮询失败不影响页面
  }
}
// ====== end 作业提醒轮询 ======

onMounted(() => {
  fetchHomeworkTasks()
  fetchExamTasks()
  if (userStore.fetchCheckInStatus) userStore.fetchCheckInStatus()
  // 新增：挂载时立即检查一次，之后每 60s 轮询
  checkHomeworkReminder()
  reminderPollTimer = setInterval(checkHomeworkReminder, 60_000)
})

onUnmounted(() => {
  if (timerId) clearTimeout(timerId)
  clearAiRecordTimer()
  if (aiSpeechRecognition) {
    aiSpeechStopRequested = true
    aiSpeechRecognition.stop()
  }
  if (aiMediaRecorder && aiMediaRecorder.state !== 'inactive') stopAiRecording(false)
  aiRecordStream?.getTracks().forEach(track => track.stop())
  stopAvatarVoiceInput(false)
  avatarRecordStream?.getTracks().forEach(track => track.stop())
  if (selectedImage.value?.url) URL.revokeObjectURL(selectedImage.value.url)
  chatHistory.value.forEach(msg => {
    if (msg.imageUrl?.startsWith('blob:')) URL.revokeObjectURL(msg.imageUrl)
  })
  // 新增：清除提醒轮询
  if (reminderPollTimer) clearInterval(reminderPollTimer)
  destroyAvatar()
  // 补充下面两行
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
})
</script>

<style scoped>
.float-buttons { position: fixed; right: 24px; top: 50%; transform: translateY(-50%); display: flex; flex-direction: column; gap: 12px; z-index: 998; }
.float-btn {
  position: relative;
  width: 52px;
  height: 58px;
  background-color: var(--bg-card, #FFFFFF);
  border: 1px solid var(--border-color, #E7ECF3);
  border-radius: 8px;
  /* 1. 优化默认阴影：增加一层极淡的负扩展阴影，显得更通透 */
  box-shadow: 0 4px 12px -2px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-sub, #667085);
  /* 2. 将动画曲线改为弹性曲线，使得上浮的动作更加丝滑自然 */
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.float-btn:hover {
  color: var(--primary-color, #2563EB);
  border-color: #BFDBFE;
  /* 3. 核心修复：引入 -4px 的扩展半径。
     这会让阴影的核心体积缩小，仅把最柔和的羽化边缘透出来，彻底消除实体边框感 */
  box-shadow: 0 12px 24px -4px rgba(37, 99, 235, 0.25),
  0 4px 8px -2px rgba(37, 99, 235, 0.1);
  transform: translateY(-3px);
}

.float-btn.is-active {
  color: var(--primary-color, #2563EB);
  border-color: #BFDBFE;
  background: #F8FAFD;
  /* 激活状态同步柔和阴影，并保持上浮反馈 */
  box-shadow: 0 12px 24px -4px rgba(37, 99, 235, 0.25),
  0 4px 8px -2px rgba(37, 99, 235, 0.1);
  transform: translateY(-3px);
}
.float-btn :deep(svg) { font-size: 20px; margin-bottom: 4px; transition: all 0.3s ease; }
.float-btn span { font-size: 12px; line-height: 1; font-weight: 500; transition: all 0.3s ease; }
.float-btn.has-dot::after { content: ''; position: absolute; top: 8px; right: 12px; width: 8px; height: 8px; border-radius: 50%; background: #EF4444; border: 1.5px solid #fff; }
.popover-title { font-weight: 600; color: var(--text-main); }
.pop-checkin-body { display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 8px 12px; width: 200px; }
.checkin-circle { width: 80px; height: 80px; border-radius: 50%; border: 3px solid #E7ECF3; display: flex; flex-direction: column; align-items: center; justify-content: center; transition: 0.3s; background: #FFF; }
.checkin-circle.done { border-color: #10B981; background: #ECFDF5; color: #10B981; }
.checkin-day { font-size: 28px; font-weight: 800; line-height: 1; }
.btn-checkin-submit { width: 100%; height: 40px; border: none; border-radius: 8px; background-color: var(--primary-color, #2563EB); color: #ffffff; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s ease; }
.btn-checkin-submit:hover { background-color: var(--primary-hover, #1D4ED8); }
.btn-checkin-submit.done { background-color: #E5E7EB !important; color: #9CA3AF !important; cursor: default; }
.hw-pop-body { width: 280px; max-height: 320px; overflow-y: auto; padding: 4px; }
.hw-pop-item { display: flex; justify-content: space-between; align-items: center; padding: 12px; border-bottom: 1px solid var(--border-color); border-radius: 8px; transition: background 0.2s; }
.hw-pop-item:hover { background: var(--bg-sub); border-bottom-color: transparent; }
.hw-info { flex: 1; padding-right: 12px; }
.hw-pop-title { font-size: 14px; font-weight: 600; color: var(--text-main); margin-bottom: 4px; }
.hw-pop-sub { font-size: 12px; color: var(--text-light); }
.btn-do-hw { background: #EEF2FF; border: none; color: var(--primary-color); cursor: pointer; font-weight: 600; border-radius: 6px; padding: 6px 12px; transition: 0.2s; }
.btn-do-hw:hover { background: #E0E7FF; }
.hw-pop-empty { text-align: center; padding: 32px 0; color: var(--text-sub); }
.timer-modal-body { display: flex; flex-direction: column; align-items: center; padding: 10px 0; }
.timer-inputs { display: flex; align-items: center; justify-content: center; gap: 16px; margin-bottom: 32px; background: var(--bg-sub, #F8FAFD); padding: 24px 32px; border-radius: 16px; border: 1px solid var(--border-color, #E7ECF3); }
.input-group { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.input-group :deep(.ant-input-number) { width: 86px; height: 72px; border-radius: 12px; border: 1px solid transparent; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04); background: #FFF; transition: all 0.3s ease; }
.input-group :deep(.ant-input-number:hover), .input-group :deep(.ant-input-number-focused) { border-color: var(--primary-color, #2563EB); box-shadow: 0 6px 16px rgba(37, 99, 235, 0.12); }
.input-group :deep(.ant-input-number-input) { height: 72px; font-size: 36px; font-weight: 800; text-align: center; color: var(--text-main, #1F2937); font-family: 'Inter', -apple-system, monospace; }
.input-group :deep(.ant-input-number-handler-wrap) { display: none; }
.time-unit { color: var(--text-sub, #667085); font-size: 13px; font-weight: 600; letter-spacing: 2px; }
.time-colon { font-size: 36px; font-weight: 600; color: var(--primary-color, #2563EB); margin-top: -28px; animation: colon-pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite; }
@keyframes colon-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.timer-msg-input { width: 100%; margin-bottom: 32px; }
.timer-msg-input :deep(.ant-input) { height: 44px; border-radius: 8px; font-size: 14px; text-align: center; background: var(--bg-card, #FFF); border-color: var(--border-color, #E7ECF3); transition: all 0.3s; }
.timer-msg-input :deep(.ant-input:focus) { border-color: var(--primary-color, #2563EB); box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1); }
.timer-actions { display: flex; gap: 16px; width: 100%; justify-content: center; }
.timer-actions button { height: 44px; border-radius: 8px; font-size: 15px; font-weight: 600; border: none; cursor: pointer; transition: all 0.2s; }
.btn-timer-cancel { width: 120px; background: #F1F5F9; color: var(--text-regular, #344054); }
.btn-timer-cancel:hover { background: #E2E8F0; color: var(--text-main, #1F2937); }
.btn-timer-start { flex: 1; background: var(--primary-color, #2563EB); color: #FFF; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2); }
.btn-timer-start:hover { background: var(--primary-hover, #1D4ED8); transform: translateY(-1px); box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3); }
.btn-timer-stop { width: 100%; background: #FEF2F2; color: #EF4444; border: 1px solid #FECACA !important; }
.btn-timer-stop:hover { background: #FEE2E2; }
.avatar-float { position: fixed; bottom: 70px; left: 15px; z-index: 200; display: flex; flex-direction: column; }
.avatar-float-left { align-items: flex-start; }
.avatar-fab-pill { width: 48px; height: 48px; padding: 0; border-radius: 50%; border: none; background: linear-gradient(135deg, #2563EB, #1D4ED8); color: #FFFFFF; font-size: 20px; font-weight: 600; cursor: pointer; display: flex; justify-content: center; align-items: center; box-shadow: 0 10px 26px rgba(37, 99, 235, 0.28); transition: all 0.3s ease; }
.avatar-fab-pill:hover { transform: translateY(-2px); box-shadow: 0 14px 30px rgba(37, 99, 235, 0.34); }
.avatar-panel { position: absolute; left: 0; bottom: 60px; width: 200px; border-radius: 5px; background: rgba(255, 255, 255, 0.96); box-shadow: 0 18px 48px rgba(15, 23, 42, 0.16); border: 1px solid #E7ECF3; overflow: hidden; backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px); }
.avatar-head { padding: 16px 18px; border-bottom: 1px solid #E7ECF3; display: flex; justify-content: space-between; align-items: center; background: linear-gradient(180deg, #F8FBFF 0%, #FFFFFF 100%); }
.avatar-head-left { display: flex; align-items: center; gap: 12px; }
.avatar-head-icon { width: 38px; height: 38px; border-radius: 5px; background: linear-gradient(135deg, #DBEAFE, #BFDBFE); color: #2563EB; display: flex; align-items: center; justify-content: center; font-size: 18px; }
.avatar-title-wrap { display: flex; flex-direction: column; }
.avatar-title { font-size: 15px; font-weight: 700; color: #1F2937; }
.avatar-status { margin-top: 2px; font-size: 12px; color: #64748B; }
.avatar-close { border: none; background: transparent; color: #64748B; cursor: pointer; font-size: 18px; padding: 4px; border-radius: 5px; transition: all 0.2s ease; }
.avatar-close:hover { background: #F1F5F9; color: #1F2937; }
.avatar-stage { position: relative; margin: 5px 5px 0; height: 280px; border-radius: 5px; overflow: hidden; background: linear-gradient(180deg, #0F172A 0%, #1E293B 100%); border: 1px solid rgba(148, 163, 184, 0.18); }
.avatar-preview {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.avatar-preview :deep(#xvideo > div) {
  width: 100% !important;
  height: 100% !important;
  transform: none !important;
  left: 0 !important;
  top: 0 !important;
}

.avatar-preview :deep(div[id^="player_"]) {
  width: 100% !important;
  height: 100% !important;
}

/* 越过 SDK 复杂的中间层，直接把最终画面"钉"在四角，并按比例拉伸 */
.avatar-preview :deep(video),
.avatar-preview :deep(canvas) {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  /* 如果填满后发现人物头部被裁，可以调整这里，比如 center 10% 或 center top */
  object-position: center top !important;
}
.avatar-stage-mask { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px; text-align: center; color: #E2E8F0; background: radial-gradient(circle at top, rgba(59, 130, 246, 0.22), transparent 45%), linear-gradient(180deg, rgba(15, 23, 42, 0.72), rgba(15, 23, 42, 0.9)); }
.avatar-stage-icon { width: 58px; height: 58px; border-radius: 50%; background: rgba(255, 255, 255, 0.12); display: flex; align-items: center; justify-content: center; font-size: 28px; margin-bottom: 14px; }
.avatar-stage-title { font-size: 16px; font-weight: 700; color: #FFFFFF; margin-bottom: 6px; }
.avatar-stage-desc { font-size: 13px; line-height: 1.6; color: rgba(226, 232, 240, 0.92); }
.avatar-body {
  /* 修改前是 padding: 16px; */
  padding: 14px 5px 16px 5px; /* 上边距 14px，左右边距 5px（与上方对齐），下边距 16px */
}
.avatar-textarea { width: 100%; min-height: 92px; resize: none; border-radius: 5px; border: 1px solid #DCE3ED; background: #F8FAFC; padding: 12px 14px; box-sizing: border-box; font-size: 14px; color: #1F2937; outline: none; transition: all 0.2s ease; }
.avatar-textarea:focus { border-color: #93C5FD; background: #FFFFFF; box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08); }
.avatar-textarea:disabled { color: #64748B; cursor: wait; }
.avatar-actions { margin-top: 14px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.avatar-btn { min-height: 40px; padding: 0 8px; border: none; border-radius: 5px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s ease; display: inline-flex; align-items: center; justify-content: center; gap: 5px; }
.avatar-btn:hover:not(:disabled) { transform: translateY(-1px); }
.avatar-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.avatar-btn-primary { background: linear-gradient(135deg, #2563EB, #1D4ED8); color: #FFFFFF; box-shadow: 0 10px 20px rgba(37, 99, 235, 0.2); }
.avatar-btn-neutral { background: #F1F5F9; color: #334155; }
.avatar-btn-warning { background: #FFF7ED; color: #C2410C; border: 1px solid #FED7AA; }
.avatar-btn-success { background: #ECFDF3; color: #047857; border: 1px solid #A7F3D0; }
.avatar-btn-voice { background: #F8FAFC; color: #334155; border: 1px solid #CBD5E1; }
.avatar-btn-voice:hover:not(:disabled) { border-color: #93C5FD; color: #1D4ED8; background: #EFF6FF; }
.avatar-btn-voice.recording { border-color: #FCA5A5; color: #B91C1C; background: #FEF2F2; }
.avatar-btn-info { background: #EFF6FF; color: #1D4ED8; border: 1px solid #BFDBFE; }
.avatar-btn-danger { background: #FEF2F2; color: #DC2626; border: 1px solid #FECACA; }
.slide-up-left-enter-active, .slide-up-left-leave-active { transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-up-left-enter-from, .slide-up-left-leave-to { opacity: 0; transform: translateY(16px) scale(0.96); pointer-events: none; }
@media (max-width: 768px) { .avatar-float-left { left: 16px; } .avatar-panel { width: min(360px, calc(100vw - 32px)); } .avatar-stage { height: 240px; } }
.ai-panel { position: fixed; right: 90px; bottom: 40px; width: 430px; height: 640px; border-radius: 8px !important; background-color: #FFFFFF !important; box-shadow: 0 16px 42px rgba(15, 23, 42, 0.16) !important; display: flex; flex-direction: column; z-index: 1001; border: 1px solid #E7ECF3 !important; overflow: hidden !important; }
.ai-head {
  flex-shrink: 0;
  padding: 16px 20px;
  border-bottom: 1px solid #E7ECF3;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #FFFFFF;

  cursor: move;
  user-select: none;
}
.ai-head-left { display: flex; align-items: center; gap: 10px; }
.ai-avatar {
  width: 36px;
  height: 36px;
  background: var(--primary-color, #2563EB); /* 修改了这一行，增加后备颜色 */
  color: #FFF;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.ai-title-wrap { display: flex; flex-direction: column; }
.ai-title { font-weight: 600; color: var(--text-main); font-size: 15px; }
.ai-status { font-size: 12px; color: #10B981; display: flex; align-items: center; gap: 4px; }
.ai-status::before { content: ''; display: block; width: 6px; height: 6px; background: #10B981; border-radius: 50%; }
.ai-close { border: none; background: transparent; cursor: pointer; color: var(--text-light); font-size: 18px; padding: 4px; transition: 0.2s; }
.ai-close:hover { color: var(--text-main); background: #F3F4F6; border-radius: 5px; }
.ai-body { flex: 1 1 0; min-height: 0; padding: 16px; overflow-y: auto; display: flex; flex-direction: column; gap: 14px; background-color: #F8FAFC !important; }
.ai-welcome {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border: 1px solid #DDE6F3;
  border-radius: 8px;
  background: #FFFFFF;
  color: #344054;
}
.ai-welcome-icon {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #EFF6FF;
  color: #2563EB;
  font-size: 20px;
}
.ai-welcome-copy h3 {
  margin: 0 0 6px;
  color: #101828;
  font-size: 17px;
  font-weight: 800;
}
.ai-welcome-copy p {
  margin: 0;
  color: #667085;
  font-size: 14px;
  line-height: 1.65;
}
.ai-welcome-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.ai-welcome-prompts button {
  height: 32px;
  padding: 0 11px;
  border: 1px solid #BFDBFE;
  border-radius: 6px;
  background: #F8FAFC;
  color: #1D4ED8;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}
.ai-welcome-prompts button:hover {
  border-color: #93C5FD;
  background: #EFF6FF;
  color: #1E40AF;
}
.ai-welcome-prompts button:focus-visible {
  outline: 3px solid rgba(37, 99, 235, 0.22);
  outline-offset: 2px;
}
.msg { display: flex; max-width: 90%; gap: 10px; }
.msg.user { align-self: flex-end; flex-direction: row-reverse; }
.msg-avatar { width: 28px; height: 28px; border-radius: 5px; background: #DBEAFE; color: var(--primary-color); display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0; }
.msg-bubble { padding: 12px 16px; border-radius: 5px; font-size: 14px; line-height: 1.6; word-break: break-word; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02); display: flex; flex-direction: column; gap: 8px; }
.msg.ai .msg-bubble { background: #FFFFFF; border: 1px solid #E7ECF3; color: var(--text-regular); border-top-left-radius: 0; }
.msg.user .msg-bubble { background: #FFFFFF; color: #1F2937; border: 1px solid #DDE6F3; border-top-right-radius: 0; }
.msg-image { width: 180px; max-height: 140px; object-fit: cover; border-radius: 5px; border: 1px solid #E2E8F0; background: #F8FAFC; cursor: zoom-in; transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease; }
.msg-image:hover, .msg-image:focus-visible { border-color: #93C5FD; box-shadow: 0 8px 18px rgba(37, 99, 235, 0.14); outline: none; transform: translateY(-1px); }
.ai-preview-image { display: block; width: 100%; max-height: min(78vh, 760px); object-fit: contain; border-radius: 6px; background: #F8FAFC; }
:deep(.ai-image-preview-modal) { z-index: 3001; }
:deep(.ai-image-preview-modal .ant-modal-content) { padding: 14px; border-radius: 8px; overflow: hidden; }
:deep(.ai-image-preview-modal .ant-modal-body) { padding: 0; }
.ai-compose { flex-shrink: 0; padding: 12px 14px 14px; background: #FFFFFF; border-top: 1px solid #E7ECF3; display: flex; flex-direction: column; gap: 10px; }
.image-preview-card { display: grid; grid-template-columns: 78px 1fr; gap: 10px; padding: 8px; border: 1px solid #E1E8F2; border-radius: 8px; background: #F8FAFC; }
.image-preview-card img { width: 78px; height: 58px; object-fit: cover; border-radius: 5px; border: 1px solid #DDE6F3; background: #FFFFFF; }
.image-preview-meta { min-width: 0; display: flex; align-items: center; justify-content: space-between; gap: 10px; color: #344054; font-size: 13px; }
.image-preview-meta span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.image-preview-meta button { width: 30px; height: 30px; border: 1px solid #E7ECF3; border-radius: 5px; background: #FFFFFF; color: #64748B; cursor: pointer; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.image-preview-meta button:hover:not(:disabled) { color: #DC2626; border-color: #FECACA; background: #FEF2F2; }
.voice-state { height: 28px; padding: 0 10px; border-radius: 5px; background: #EFF6FF; color: #1D4ED8; display: flex; align-items: center; font-size: 12px; font-weight: 600; }
.ai-input-row { display: grid; grid-template-columns: 44px minmax(0, 1fr) auto; gap: 10px; align-items: end; }
.ai-file-input { display: none; }
.ai-input-actions { display: flex; align-items: center; gap: 8px; }
.tool-btn, .btn-send { width: 44px; height: 44px; border: 1px solid #E1E8F2; background: #FFFFFF; color: #475569; border-radius: 5px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: 0.18s; }
.tool-btn:hover:not(:disabled) { border-color: #93C5FD; color: #1D4ED8; background: #EFF6FF; }
.tool-btn.recording { border-color: #FCA5A5; color: #DC2626; background: #FEF2F2; }
.ai-input-row textarea { width: 100%; min-width: 0; height: 44px; min-height: 44px; max-height: 44px; resize: none; overflow-y: auto; border: 1px solid #E1E8F2; background: #F8FAFC; border-radius: 5px; padding: 10px 12px; outline: none; font-size: 14px; line-height: 22px; color: #1F2937; transition: 0.18s; }
.ai-input-row textarea:focus { border-color: #93C5FD; background: #FFFFFF; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08); }
.btn-send { border: none; background: var(--primary-color, #2563EB); color: #fff; }
.btn-send:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 8px 18px rgba(37, 99, 235, 0.22); }
.tool-btn:disabled, .btn-send:disabled, .ai-input-row textarea:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-send:disabled { background: #D1D5DB; box-shadow: none; }
@media (max-width: 768px) {
  .ai-panel {
    right: 12px !important;
    bottom: 16px !important;
    width: calc(100vw - 24px);
    height: min(640px, calc(100vh - 32px));
  }
  .ai-input-row {
    grid-template-columns: 40px minmax(0, 1fr) auto;
    gap: 6px;
  }
  .tool-btn, .btn-send {
    width: 40px;
    height: 40px;
  }
  .ai-input-actions {
    gap: 6px;
  }
  .ai-input-row textarea {
    height: 40px;
    min-height: 40px;
    max-height: 40px;
    line-height: 20px;
    padding: 9px 10px;
  }
}
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); pointer-events: none; }
.markdown-body { font-size: 14px; line-height: 1.6; color: var(--text-regular); white-space: normal; }
:deep(.markdown-body pre) { margin: 10px 0; padding: 12px; border-radius: 5px; background-color: #1a1a1a !important; overflow-x: auto; }
:deep(.markdown-body code) { font-family: 'SF Mono', Consolas, monospace; font-size: 13px; }
.typing { display: flex; gap: 5px; align-items: center; height: 24px; padding: 0 8px; }
.typing span { width: 6px; height: 6px; background: var(--primary-color); opacity: 0.6; border-radius: 50%; animation: bounce 1.4s infinite ease-in-out both; }
.typing span:nth-child(1) { animation-delay: -0.32s; }
.typing span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

/* ===== 新增：作业提醒气泡样式 ===== */
.hw-reminder-bubble {
  position: fixed;
  right: 88px;
  top: 50%;
  transform: translateY(-50%);
  width: 256px;
  background: var(--bg-card, #FFFFFF);
  border: 1px solid var(--border-color, #E7ECF3);
  border-radius: 12px;
  display: flex;
  align-items: stretch;
  box-shadow: 0 4px 24px rgba(37, 99, 235, 0.10), 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 999;
  user-select: none;
  animation: reminder-slide-in 0.36s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  overflow: hidden;
}
.reminder-bar {
  width: 4px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #2563EB 0%, #60A5FA 100%);
  border-radius: 12px 0 0 12px;
}
.reminder-content {
  flex: 1;
  padding: 12px 12px 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.reminder-header {
  display: flex;
  align-items: center;
  gap: 6px;
}
.reminder-bell-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--primary-color, #2563EB);
}
.reminder-label {
  flex: 1;
  font-size: 13px;
  font-weight: 700;
  color: var(--primary-color, #2563EB);
  letter-spacing: 0.02em;
}
.reminder-close-btn {
  width: 22px;
  height: 22px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: var(--text-sub, #94A3B8);
  padding: 3px;
  transition: background 0.15s, color 0.15s;
  flex-shrink: 0;
}
.reminder-close-btn:hover { background: #F1F5F9; color: var(--text-main, #1F2937); }
.reminder-close-btn svg { width: 12px; height: 12px; }
.reminder-text {
  margin: 0;
  font-size: 13px;
  color: var(--text-regular, #344054);
  line-height: 1.6;
  word-break: break-all;
}
.reminder-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 2px;
}
.reminder-time {
  font-size: 11px;
  color: var(--text-sub, #94A3B8);
}
.reminder-action-btn {
  height: 26px;
  padding: 0 10px;
  border: 1px solid #BFDBFE;
  background: #EFF6FF;
  color: var(--primary-color, #2563EB);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.reminder-action-btn:hover { background: #DBEAFE; border-color: #93C5FD; }
@keyframes reminder-slide-in {
  from { opacity: 0; transform: translateY(-50%) translateX(16px) scale(0.95); }
  to   { opacity: 1; transform: translateY(-50%) translateX(0)     scale(1);    }
}
.reminder-pop-enter-active { animation: reminder-slide-in 0.36s cubic-bezier(0.34, 1.56, 0.64, 1) both; }
.reminder-pop-leave-active  { transition: all 0.16s ease-in; }
.reminder-pop-leave-to { opacity: 0; transform: translateY(-50%) translateX(12px) scale(0.94); }
/* ===== end 作业提醒气泡样式 ===== */
</style>
