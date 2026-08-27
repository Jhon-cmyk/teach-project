<template>
  <div class="teacher-global-assistant">
    <transition name="assistant-panel">
      <section
        v-if="isOpen"
        ref="panelRef"
        class="assistant-panel"
        :style="{ right: panelPos.right + 'px', bottom: panelPos.bottom + 'px' }"
      >
        <header class="assistant-header" @mousedown="startDrag">
          <div class="assistant-brand">
            <div class="assistant-avatar">
              <robot-outlined />
            </div>
            <div>
              <h3>通用教学助手</h3>
              <p>{{ assistantStatus }}</p>
            </div>
          </div>

          <div class="assistant-actions">
            <button
              v-if="messages.length"
              class="icon-btn"
              type="button"
              title="清空上下文"
              :disabled="isGenerating"
              @click.stop="clearHistory"
            >
              <delete-outlined />
            </button>
            <button class="icon-btn" type="button" title="关闭" @click.stop="isOpen = false">
              <close-outlined />
            </button>
          </div>
        </header>

        <main ref="messageBoxRef" class="assistant-body">
          <div v-if="messages.length === 0" class="assistant-empty">
            <div class="empty-icon"><message-outlined /></div>
            <h4>备课、课堂、学情材料都可以直接问</h4>
            <p>支持文字提问、上传图片解析、上传 PDF/Word/文本文件，也可以语音输入。</p>
            <div class="suggestions">
              <button
                v-for="item in suggestions"
                :key="item"
                type="button"
                @click="fillSuggestion(item)"
              >
                {{ item }}
              </button>
            </div>
          </div>

          <div v-else class="message-list">
            <article
              v-for="(item, index) in messages"
              :key="index"
              class="message-item"
              :class="item.role"
            >
              <div class="message-avatar">
                <user-outlined v-if="item.role === 'user'" />
                <robot-outlined v-else />
              </div>
              <div class="message-bubble">
                <template v-if="item.role === 'user'">
                  <img v-if="item.imageUrl" :src="item.imageUrl" alt="上传图片" class="message-image" />
                  <div v-if="item.fileName" class="message-file">
                    <file-text-outlined />
                    <span>{{ item.fileName }}</span>
                  </div>
                  <span>{{ item.content }}</span>
                </template>
                <template v-else>
                  <div v-if="item.content" class="markdown-render" v-html="renderMd(item.content)"></div>
                  <div v-else class="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </template>
              </div>
            </article>
          </div>
        </main>

        <footer class="assistant-compose">
          <div v-if="selectedAttachment" class="attachment-preview" :class="selectedAttachment.kind">
            <img v-if="selectedAttachment.kind === 'image'" :src="selectedAttachment.url" alt="图片预览" />
            <div v-else class="attachment-file-icon"><file-text-outlined /></div>
            <div class="attachment-meta">
              <strong>{{ selectedAttachment.name }}</strong>
              <span>{{ selectedAttachment.kind === 'image' ? '图片解析' : '文件解析' }} · {{ selectedAttachment.sizeText }}</span>
            </div>
            <button type="button" :disabled="isGenerating" @click="removeAttachment">
              <close-outlined />
            </button>
          </div>

          <div v-if="isRecording || isVoiceTranscribing" class="voice-state">
            <span v-if="isRecording">
              {{ isRealtimeSpeechActive ? '实时识别中' : '录音中' }} {{ formattedRecordTime }}
            </span>
            <span v-else>正在识别语音...</span>
          </div>

          <div class="compose-row">
            <input
              ref="fileInputRef"
              class="file-input"
              type="file"
              accept="image/jpeg,image/png,image/webp,.pdf,.doc,.docx,.txt,.md,.markdown,.csv,.json,.xml,.html,.css,.js,.ts,.java,.py,.sql,.yml,.yaml,.vue"
              @change="handleFileSelect"
            />
            <button
              class="tool-btn"
              type="button"
              title="上传图片或文件"
              :disabled="isGenerating || isRecording"
              @click="triggerFileSelect"
            >
              <paper-clip-outlined />
            </button>
            <textarea
              ref="inputRef"
              v-model="inputText"
              :disabled="isGenerating || isVoiceTranscribing || isRecording"
              rows="1"
              placeholder="输入问题，或上传图片/文件后提问…"
              @keydown.enter.prevent="handleEnter"
              @input="resizeInput"
            ></textarea>
            <button
              class="tool-btn"
              type="button"
              title="语音输入"
              :class="{ recording: isRecording }"
              :disabled="isGenerating || isVoiceTranscribing"
              @click="toggleRecording"
            >
              <audio-outlined />
            </button>
            <button
              class="send-btn"
              type="button"
              :disabled="!canSend"
              @click="sendMessage"
            >
              <loading-outlined v-if="isGenerating" />
              <send-outlined v-else />
            </button>
          </div>
        </footer>
      </section>
    </transition>

    <button
      class="assistant-fab"
      type="button"
      :class="{ active: isOpen }"
      title="通用教学助手"
      aria-label="通用教学助手"
      @click="toggleOpen"
    >
      <robot-outlined v-if="!isOpen" />
      <close-outlined v-else />
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import request from '@/utils/request'
import { getAuthToken } from '@/utils/authStorage'
import {
  AudioOutlined,
  CloseOutlined,
  DeleteOutlined,
  FileTextOutlined,
  LoadingOutlined,
  MessageOutlined,
  PaperClipOutlined,
  RobotOutlined,
  SendOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'

type ChatMessage = {
  role: 'user' | 'ai'
  content: string
  imageUrl?: string
  fileName?: string
}

type SelectedAttachment = {
  file: File
  name: string
  kind: 'image' | 'file'
  sizeText: string
  url?: string
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const AI_STREAM_URL = `${API_BASE}/ai/stream`
const TEACHER_VISION_URL = `${API_BASE}/ai/teacher/vision/stream`
const TEACHER_FILE_URL = `${API_BASE}/ai/teacher/file/stream`

const suggestions = [
  '帮我设计一个 5 分钟课堂导入',
  '把这份材料提炼成课堂讲解提纲',
  '学生上课注意力不集中，怎么调整互动？',
]

const md = new MarkdownIt({ breaks: true, html: true })
const renderMd = (text: string) => md.render(text || '')

const isOpen = ref(false)
const isGenerating = ref(false)
const inputText = ref('')
const messages = ref<ChatMessage[]>([])
const selectedAttachment = ref<SelectedAttachment | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const messageBoxRef = ref<HTMLElement | null>(null)
const panelRef = ref<HTMLElement | null>(null)
const panelPos = ref({ right: 28, bottom: 92 })

const isRecording = ref(false)
const isRealtimeSpeechActive = ref(false)
const isVoiceTranscribing = ref(false)
const recordSeconds = ref(0)

const MAX_IMAGE_SIZE_MB = 8
const MAX_FILE_SIZE_MB = 20
const MAX_RECORD_SECONDS = 60

let isDragging = false
let dragStartX = 0
let dragStartY = 0
let dragStartRight = 0
let dragStartBottom = 0
let panelWidth = 420
let panelHeight = 620
let mediaRecorder: MediaRecorder | null = null
let recordStream: MediaStream | null = null
let recordChunks: BlobPart[] = []
let recordTimer: ReturnType<typeof setInterval> | null = null
let shouldTranscribeRecord = true
let speechRecognition: any = null
let speechStopRequested = false
let realtimeVoiceBaseText = ''
let realtimeFinalText = ''
let preferRealtimeSpeech = true

const assistantStatus = computed(() => {
  if (isGenerating.value) return '正在生成回答…'
  if (isRecording.value) return isRealtimeSpeechActive.value ? '正在实时识别语音' : '正在录音'
  if (isVoiceTranscribing.value) return '正在转写语音'
  return '支持文字、文件、图片和语音'
})

const formattedRecordTime = computed(() => {
  const minute = Math.floor(recordSeconds.value / 60).toString().padStart(2, '0')
  const second = (recordSeconds.value % 60).toString().padStart(2, '0')
  return `${minute}:${second}`
})

const canSend = computed(() => {
  return !isGenerating.value
    && !isVoiceTranscribing.value
    && !isRecording.value
    && (!!inputText.value.trim() || !!selectedAttachment.value)
})

const toggleOpen = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    nextTick(() => {
      inputRef.value?.focus()
      scrollToBottom()
    })
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageBoxRef.value) {
      messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight
    }
  })
}

const resizeInput = () => {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 112)}px`
}

const fillSuggestion = (text: string) => {
  inputText.value = text
  nextTick(() => {
    resizeInput()
    inputRef.value?.focus()
  })
}

const handleEnter = (event: KeyboardEvent) => {
  if (event.shiftKey) {
    const target = event.target as HTMLTextAreaElement
    const start = target.selectionStart
    const end = target.selectionEnd
    inputText.value = `${inputText.value.slice(0, start)}\n${inputText.value.slice(end)}`
    nextTick(() => {
      target.selectionStart = start + 1
      target.selectionEnd = start + 1
      resizeInput()
    })
    return
  }
  sendMessage()
}

const clearHistory = () => {
  messages.value = []
  inputText.value = ''
  removeAttachment()
  resizeInput()
}

const formatFileSize = (size: number) => {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const isImageFile = (file: File) => ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)

const triggerFileSelect = () => {
  fileInputRef.value?.click()
}

const removeAttachment = () => {
  if (selectedAttachment.value?.url) {
    URL.revokeObjectURL(selectedAttachment.value.url)
  }
  selectedAttachment.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const image = isImageFile(file)
  const maxSizeMb = image ? MAX_IMAGE_SIZE_MB : MAX_FILE_SIZE_MB
  if (file.size > maxSizeMb * 1024 * 1024) {
    message.warning(`${image ? '图片' : '文件'}不能超过 ${maxSizeMb}MB`)
    input.value = ''
    return
  }

  removeAttachment()
  selectedAttachment.value = {
    file,
    name: file.name || (image ? '教学图片' : '教学文件'),
    kind: image ? 'image' : 'file',
    sizeText: formatFileSize(file.size),
    url: image ? URL.createObjectURL(file) : undefined,
  }
}

const sendMessage = async () => {
  if (!canSend.value) return

  const question = inputText.value.trim()
  const attachment = selectedAttachment.value
  inputText.value = ''
  selectedAttachment.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
  resizeInput()

  if (attachment?.kind === 'image') {
    await sendImageMessage(question, attachment)
    return
  }
  if (attachment?.kind === 'file') {
    await sendFileMessage(question, attachment)
    return
  }
  await sendTextMessage(question)
}

const pushAiPlaceholder = () => {
  return messages.value.push({ role: 'ai', content: '' }) - 1
}

const getAuthHeaders = (): Record<string, string> => {
  const token = getAuthToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const streamIntoMessage = async (url: string, init: RequestInit, aiIndex: number) => {
  const response = await fetch(url, {
    ...init,
    credentials: 'include',
  })
  if (!response.ok) {
    throw new Error(`AI 请求失败：${response.status}`)
  }
  const reader = response.body?.getReader()
  if (!reader) throw new Error('AI 响应为空')

  const decoder = new TextDecoder('utf-8')
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    messages.value[aiIndex].content += decoder.decode(value, { stream: true })
    scrollToBottom()
  }
}

const sendTextMessage = async (question: string) => {
  messages.value.push({ role: 'user', content: question })
  scrollToBottom()
  const aiIndex = pushAiPlaceholder()
  isGenerating.value = true

  try {
    await streamIntoMessage(AI_STREAM_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders(),
      },
      body: JSON.stringify({
        question,
        type: 'teacher_assistant',
      }),
    }, aiIndex)
  } catch (error: any) {
    messages.value[aiIndex].content = `⚠️ ${error?.message || '连接失败，请稍后再试'}`
    message.error('教学助手暂时连接失败')
  } finally {
    isGenerating.value = false
    scrollToBottom()
  }
}

const sendImageMessage = async (question: string, attachment: SelectedAttachment) => {
  const displayText = question || '请解析这张图片，并给出适合教师使用的建议。'
  messages.value.push({
    role: 'user',
    content: displayText,
    imageUrl: attachment.url,
  })
  scrollToBottom()
  const aiIndex = pushAiPlaceholder()
  isGenerating.value = true

  try {
    const formData = new FormData()
    formData.append('file', attachment.file)
    formData.append('message', question)
    await streamIntoMessage(TEACHER_VISION_URL, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: formData,
    }, aiIndex)
  } catch (error: any) {
    messages.value[aiIndex].content = `⚠️ ${error?.message || '图片解析失败，请确认图片清晰后再试'}`
    message.error('图片解析失败')
  } finally {
    isGenerating.value = false
    scrollToBottom()
  }
}

const sendFileMessage = async (question: string, attachment: SelectedAttachment) => {
  const displayText = question || '请解析这份文件，提炼重点并给出教学建议。'
  messages.value.push({
    role: 'user',
    content: displayText,
    fileName: attachment.name,
  })
  scrollToBottom()
  const aiIndex = pushAiPlaceholder()
  isGenerating.value = true

  try {
    const formData = new FormData()
    formData.append('file', attachment.file)
    formData.append('message', question)
    await streamIntoMessage(TEACHER_FILE_URL, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: formData,
    }, aiIndex)
  } catch (error: any) {
    messages.value[aiIndex].content = `⚠️ ${error?.message || '文件解析失败，请换一个 PDF、Word 或文本文件重试'}`
    message.error('文件解析失败')
  } finally {
    isGenerating.value = false
    scrollToBottom()
  }
}

const getSpeechRecognitionCtor = () => {
  const win = window as any
  return win.SpeechRecognition || win.webkitSpeechRecognition
}

const clearRecordTimer = () => {
  if (recordTimer) {
    clearInterval(recordTimer)
    recordTimer = null
  }
}

const startRecordTimer = () => {
  clearRecordTimer()
  recordSeconds.value = 0
  recordTimer = setInterval(() => {
    recordSeconds.value += 1
    if (recordSeconds.value >= MAX_RECORD_SECONDS) {
      stopRecording()
    }
  }, 1000)
}

const toggleRecording = async () => {
  if (isRecording.value) {
    stopRecording()
    return
  }
  if (preferRealtimeSpeech && startRealtimeSpeechRecognition()) {
    return
  }
  await startUploadRecording()
}

const startRealtimeSpeechRecognition = () => {
  const SpeechRecognitionCtor = getSpeechRecognitionCtor()
  if (!SpeechRecognitionCtor) return false

  try {
    speechStopRequested = false
    realtimeVoiceBaseText = inputText.value.trim()
    realtimeFinalText = ''

    const recognition = new SpeechRecognitionCtor()
    recognition.lang = 'zh-CN'
    recognition.continuous = true
    recognition.interimResults = true
    recognition.maxAlternatives = 1

    recognition.onstart = () => {
      speechRecognition = recognition
      isRealtimeSpeechActive.value = true
      isRecording.value = true
      startRecordTimer()
    }

    recognition.onresult = (event: any) => {
      let interimText = ''
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const result = event.results[i]
        const text = result?.[0]?.transcript?.trim() || ''
        if (!text) continue
        if (result.isFinal) {
          realtimeFinalText = `${realtimeFinalText}${text}`
        } else {
          interimText += text
        }
      }
      const liveText = `${realtimeFinalText}${interimText}`.trim()
      inputText.value = [realtimeVoiceBaseText, liveText].filter(Boolean).join(' ')
      nextTick(resizeInput)
    }

    recognition.onerror = (event: any) => {
      const error = event?.error || ''
      if (error === 'not-allowed' || error === 'service-not-allowed') {
        message.error('无法获取麦克风权限，请检查浏览器设置')
      } else if (error !== 'no-speech') {
        preferRealtimeSpeech = false
        message.warning('实时语音识别暂不可用，下次将使用录音转写')
      }
    }

    recognition.onend = () => {
      clearRecordTimer()
      isRecording.value = false
      isRealtimeSpeechActive.value = false
      speechRecognition = null
      if (!speechStopRequested && !realtimeFinalText.trim()) {
        message.warning('没有识别到有效语音')
      }
      speechStopRequested = false
    }

    recognition.start()
    return true
  } catch {
    speechRecognition = null
    isRealtimeSpeechActive.value = false
    isRecording.value = false
    return false
  }
}

const startUploadRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    recordStream = stream
    mediaRecorder = new MediaRecorder(stream)
    recordChunks = []

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        recordChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = async () => {
      recordStream?.getTracks().forEach(track => track.stop())
      recordStream = null
      clearRecordTimer()
      isRecording.value = false

      if (!shouldTranscribeRecord) {
        shouldTranscribeRecord = true
        return
      }
      if (!recordChunks.length) {
        message.warning('没有录到有效语音')
        return
      }

      const audioBlob = new Blob(recordChunks, { type: 'audio/webm' })
      const audioFile = new File([audioBlob], `Teacher_Assistant_Voice_${Date.now()}.webm`, { type: 'audio/webm' })
      await transcribeVoice(audioFile)
    }

    mediaRecorder.start()
    isRealtimeSpeechActive.value = false
    isRecording.value = true
    startRecordTimer()
  } catch {
    message.error('无法获取麦克风权限，请检查浏览器设置')
  }
}

const stopRecording = (transcribe = true) => {
  if (speechRecognition) {
    speechStopRequested = true
    speechRecognition.stop()
    return
  }

  shouldTranscribeRecord = transcribe
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
}

const transcribeVoice = async (audioFile: File) => {
  isVoiceTranscribing.value = true
  try {
    const formData = new FormData()
    formData.append('file', audioFile)
    const text = await request.post<string, string>('/ai/teacher/speech-to-text', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 0,
      skipErrorToast: true,
    })
    inputText.value = [inputText.value.trim(), text].filter(Boolean).join(' ')
    nextTick(resizeInput)
    message.success('语音已转成文字')
  } catch (error: any) {
    message.error(error?.message || '语音识别失败，请稍后再试')
  } finally {
    isVoiceTranscribing.value = false
  }
}

const startDrag = (event: MouseEvent) => {
  if ((event.target as HTMLElement).closest('.assistant-actions')) return

  isDragging = true
  dragStartX = event.clientX
  dragStartY = event.clientY
  dragStartRight = panelPos.value.right
  dragStartBottom = panelPos.value.bottom
  panelWidth = panelRef.value?.offsetWidth || panelWidth
  panelHeight = panelRef.value?.offsetHeight || panelHeight

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (event: MouseEvent) => {
  if (!isDragging) return
  event.preventDefault()

  const nextRight = dragStartRight - (event.clientX - dragStartX)
  const nextBottom = dragStartBottom - (event.clientY - dragStartY)
  const maxRight = Math.max(12, window.innerWidth - panelWidth - 12)
  const maxBottom = Math.max(12, window.innerHeight - panelHeight - 12)

  panelPos.value = {
    right: Math.max(12, Math.min(nextRight, maxRight)),
    bottom: Math.max(76, Math.min(nextBottom, maxBottom)),
  }
}

const stopDrag = () => {
  isDragging = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

onUnmounted(() => {
  stopDrag()
  clearRecordTimer()
  if (speechRecognition) {
    speechStopRequested = true
    speechRecognition.stop()
  }
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    stopRecording(false)
  }
  recordStream?.getTracks().forEach(track => track.stop())
  messages.value.forEach(item => {
    if (item.imageUrl) URL.revokeObjectURL(item.imageUrl)
  })
  removeAttachment()
})
</script>

<style scoped>
.teacher-global-assistant {
  position: fixed;
  right: 28px;
  bottom: 26px;
  z-index: 1200;
  pointer-events: none;
}

.assistant-fab,
.assistant-panel {
  pointer-events: auto;
}

.assistant-fab {
  width: 54px;
  height: 54px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background:
    radial-gradient(circle at 18% 12%, rgba(255, 255, 255, 0.32), transparent 28%),
    linear-gradient(135deg, #2563eb 0%, #0f766e 100%);
  box-shadow: 0 16px 34px rgba(37, 99, 235, 0.24);
  cursor: pointer;
  font-size: 23px;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.assistant-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 42px rgba(37, 99, 235, 0.3);
}

.assistant-fab.active {
  background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
}

.assistant-panel {
  position: fixed;
  width: min(450px, calc(100vw - 32px));
  height: min(660px, calc(100vh - 116px));
  border-radius: 22px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid rgba(203, 213, 225, 0.9);
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.22);
  display: flex;
  flex-direction: column;
}

.assistant-header {
  flex: 0 0 auto;
  height: 76px;
  padding: 15px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(20, 184, 166, 0.1)),
    #ffffff;
  border-bottom: 1px solid #e2e8f0;
  cursor: move;
  user-select: none;
}

.assistant-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.assistant-avatar {
  width: 42px;
  height: 42px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #0d9488);
  font-size: 21px;
}

.assistant-brand h3 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
  font-weight: 900;
}

.assistant-brand p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
}

.assistant-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-btn,
.tool-btn {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 11px;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: 0.18s ease;
}

.icon-btn:hover:not(:disabled),
.tool-btn:hover:not(:disabled) {
  background: #e0f2fe;
  color: #0369a1;
}

.icon-btn:disabled,
.tool-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.tool-btn.recording {
  color: #ffffff;
  background: #ef4444;
  animation: recordingPulse 1.1s infinite ease-in-out;
}

.assistant-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.08), transparent 32%),
    #f8fafc;
}

.assistant-empty {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
}

.empty-icon {
  width: 58px;
  height: 58px;
  border-radius: 20px;
  margin: 0 auto 14px;
  display: grid;
  place-items: center;
  color: #2563eb;
  background: #eff6ff;
  font-size: 26px;
}

.assistant-empty h4 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
  font-weight: 900;
}

.assistant-empty p {
  margin: 8px auto 18px;
  max-width: 330px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.suggestions {
  display: grid;
  gap: 8px;
}

.suggestions button {
  border: 1px solid #dbeafe;
  border-radius: 14px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.86);
  color: #1e40af;
  text-align: left;
  cursor: pointer;
  font-size: 13px;
  transition: 0.18s ease;
}

.suggestions button:hover {
  border-color: #93c5fd;
  background: #ffffff;
  transform: translateY(-1px);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.message-item {
  display: flex;
  gap: 9px;
  align-items: flex-start;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #e0f2fe;
  color: #0369a1;
}

.message-item.ai .message-avatar {
  background: #e0f2f1;
  color: #0f766e;
}

.message-bubble {
  max-width: calc(100% - 42px);
  border-radius: 16px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #1e293b;
  font-size: 13px;
  line-height: 1.7;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.05);
  white-space: pre-wrap;
  word-break: break-word;
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.2);
}

.message-image {
  display: block;
  width: 100%;
  max-height: 180px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.22);
}

.message-file {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  margin-bottom: 8px;
  padding: 6px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
}

.message-file span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.markdown-render {
  white-space: normal;
}

.markdown-render :deep(p) {
  margin: 0 0 8px;
}

.markdown-render :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-render :deep(ul),
.markdown-render :deep(ol) {
  padding-left: 18px;
  margin: 6px 0;
}

.markdown-render :deep(code) {
  padding: 2px 5px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #0f172a;
}

.markdown-render :deep(pre) {
  margin: 8px 0;
  padding: 10px;
  overflow-x: auto;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
}

.typing-dots {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 20px;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #38bdf8;
  animation: typing 1s infinite ease-in-out;
}

.typing-dots span:nth-child(2) {
  animation-delay: 0.12s;
}

.typing-dots span:nth-child(3) {
  animation-delay: 0.24s;
}

.assistant-compose {
  flex: 0 0 auto;
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 9px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
}

.attachment-preview {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 30px;
  gap: 10px;
  align-items: center;
  padding: 8px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: #f8fbff;
}

.attachment-preview img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 10px;
}

.attachment-file-icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #2563eb;
  background: #eff6ff;
  font-size: 22px;
}

.attachment-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.attachment-meta strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-meta span {
  color: #64748b;
  font-size: 12px;
}

.attachment-preview button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 9px;
  color: #64748b;
  background: #ffffff;
  cursor: pointer;
}

.voice-state {
  padding: 7px 10px;
  border-radius: 12px;
  color: #b45309;
  background: #fffbeb;
  font-size: 12px;
}

.compose-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 34px 42px;
  gap: 8px;
  align-items: end;
}

.file-input {
  display: none;
}

.compose-row textarea {
  width: 100%;
  min-height: 42px;
  max-height: 112px;
  resize: none;
  border: 1px solid #dbe3ef;
  border-radius: 14px;
  padding: 10px 12px;
  outline: none;
  color: #0f172a;
  background: #f8fafc;
  font-size: 13px;
  line-height: 20px;
  transition: 0.18s ease;
}

.compose-row textarea:focus {
  border-color: #60a5fa;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.send-btn {
  width: 42px;
  height: 42px;
  border: 0;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #0d9488);
  cursor: pointer;
  transition: 0.18s ease;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.22);
}

.send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.assistant-panel-enter-active,
.assistant-panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.assistant-panel-enter-from,
.assistant-panel-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}

@keyframes typing {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

@keyframes recordingPulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.28);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(239, 68, 68, 0);
  }
}

@media (max-width: 640px) {
  .teacher-global-assistant {
    right: 16px;
    bottom: 18px;
  }

  .assistant-panel {
    right: 16px !important;
    bottom: 78px !important;
    width: calc(100vw - 32px);
    height: min(620px, calc(100vh - 104px));
  }

  .assistant-fab {
    width: 52px;
    height: 52px;
  }
}
</style>
