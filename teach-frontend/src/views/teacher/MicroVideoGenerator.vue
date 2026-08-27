<template>
  <div class="micro-video-shell">
    <section class="micro-config scroll-y">
      <div class="panel-heading">
        <video-camera-outlined />
        <span>微课生成参数</span>
      </div>

      <a-form layout="vertical" :model="form" class="micro-form">
        <a-form-item label="课程主题" required>
          <a-input v-model:value="form.topic" size="large" placeholder="例如：TCP 三次握手" />
        </a-form-item>

        <a-form-item label="知识点">
          <a-textarea v-model:value="form.knowledgePoints" :rows="3" placeholder="用逗号分隔核心知识点，例如：SYN、ACK、状态转换" />
        </a-form-item>

        <div class="micro-grid">
          <a-form-item label="学段">
            <a-select v-model:value="form.grade" size="large">
              <a-select-option value="本科一年级">本科一年级</a-select-option>
              <a-select-option value="本科二年级">本科二年级</a-select-option>
              <a-select-option value="本科三年级">本科三年级</a-select-option>
              <a-select-option value="本科四年级">本科四年级</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="预计时长">
            <a-input-number v-model:value="form.durationMinutes" :min="3" :max="8" size="large" style="width: 100%" />
          </a-form-item>
        </div>

        <a-form-item label="讲解风格">
          <a-segmented v-model:value="form.style" :options="styleOptions" block />
        </a-form-item>

        <a-form-item label="画面质量">
          <a-segmented v-model:value="form.qualityMode" :options="qualityOptions" block />
        </a-form-item>

        <a-form-item label="配音音色">
          <a-select v-model:value="form.voiceId" size="large" :options="cloudVoiceOptions" />
        </a-form-item>

        <a-form-item label="输出设置">
          <div class="switch-row">
            <span>生成字幕文件</span>
            <a-switch v-model:checked="form.subtitlesEnabled" />
          </div>
          <div class="switch-row">
            <span>烧录字幕到画面</span>
            <a-switch v-model:checked="form.burnSubtitles" />
          </div>
          <div class="switch-row">
            <span>使用 AI 关键帧增强</span>
            <a-switch v-model:checked="form.useAiKeyframes" :disabled="form.qualityMode !== 'keyframe'" />
          </div>
        </a-form-item>

        <div class="action-row">
          <a-button type="primary" size="large" :loading="scriptLoading" @click="generateScript">
            生成脚本
          </a-button>
          <a-button size="large" @click="resetAll">重置</a-button>
        </div>
      </a-form>
    </section>

    <section class="micro-result scroll-y">
      <div class="result-toolbar">
        <span class="status-pill" :class="taskStatus">
          <sync-outlined v-if="scriptLoading || renderPolling" spin />
          <check-circle-filled v-else-if="taskStatus === 'succeeded'" />
          <video-camera-outlined v-else />
          {{ statusText }}
        </span>
        <div class="toolbar-actions" v-if="scriptDraft">
          <a-button type="link" @click="editMode = editMode === 'cards' ? 'json' : 'cards'">
            {{ editMode === 'cards' ? 'JSON 高级编辑' : '分镜卡片编辑' }}
          </a-button>
          <a-button type="link" @click="copyScript">复制</a-button>
          <a-button type="link" :disabled="!scriptDraft || renderLoading" :loading="renderLoading" @click="submitRender">
            渲染视频
          </a-button>
          <a-button type="link" :disabled="!canPublish" :loading="publishLoading" @click="publishMicroVideo">
            保存到我的资源
          </a-button>
        </div>
      </div>

      <div v-if="!scriptDraft && !scriptLoading" class="empty-state">
        <video-camera-outlined />
        <h3>从主题到电影感教学微课</h3>
        <p>先生成可渲染导演稿，再合成画面、镜头运动、字幕与配音音轨。</p>
      </div>

      <a-progress
        v-if="taskId"
        :percent="renderProgress"
        :status="taskStatus === 'failed' ? 'exception' : taskStatus === 'succeeded' ? 'success' : 'active'"
      />

      <div v-if="videoUrl" class="video-preview">
        <video :src="videoUrl" :poster="coverUrl" controls playsinline />
        <div class="video-meta">
          <span>{{ scriptDraft?.title || form.topic }}</span>
          <a :href="videoUrl" target="_blank" rel="noreferrer">打开视频</a>
        </div>
      </div>

      <div v-if="currentTask" class="diagnostic-grid">
        <div class="diag-card">
          <span class="diag-label">实际时长</span>
          <strong>{{ durationLabel }}</strong>
        </div>
        <div class="diag-card">
          <span class="diag-label">音轨状态</span>
          <strong>{{ audioStatus }}</strong>
        </div>
        <div class="diag-card">
          <span class="diag-label">TTS 声音</span>
          <strong>{{ voiceLabel }}</strong>
        </div>
        <div class="diag-card">
          <span class="diag-label">字幕</span>
          <strong>{{ currentTask.subtitleUrl ? '已生成' : '等待生成' }}</strong>
        </div>
        <div class="diag-card">
          <span class="diag-label">音画同步</span>
          <strong>{{ syncStatusLabel }}</strong>
        </div>
        <div class="diag-card">
          <span class="diag-label">关键帧</span>
          <strong>{{ keyframeLabel }}</strong>
        </div>
        <div class="diag-card">
          <span class="diag-label">音量</span>
          <strong>{{ volumeLabel }}</strong>
        </div>
        <div class="diag-card">
          <span class="diag-label">视频码率</span>
          <strong>{{ bitrateLabel }}</strong>
        </div>
      </div>

      <a-alert v-if="taskError" type="error" :message="taskError" show-icon class="task-alert" />
      <a-alert
        v-if="warningItems.length"
        type="warning"
        show-icon
        class="task-alert"
        :message="`渲染完成，但有 ${warningItems.length} 条提示`"
        :description="warningItems.join('；')"
      />

      <a-textarea
        v-if="editMode === 'json' && scriptDraft"
        v-model:value="editableScript"
        :rows="20"
        class="script-editor"
        @blur="applyJsonEdit"
      />

      <div v-else-if="scriptDraft" class="storyboard">
        <div class="story-title">
          <div>
            <h2>{{ scriptDraft.title || 'AI 微课导演稿' }}</h2>
            <p>{{ scriptDraft.summary || '脚本生成完成，可继续微调分镜或渲染。' }}</p>
          </div>
          <span>{{ parsedScenes.length }} 幕</span>
        </div>

        <div class="scene-card" v-for="scene in parsedScenes" :key="scene.index">
          <div class="scene-index">{{ String(scene.index).padStart(2, '0') }}</div>
          <div class="scene-body">
            <div class="scene-row">
              <a-input v-model:value="scene.title" class="scene-title-input" />
              <a-select v-model:value="scene.layoutType" class="layout-select" :options="layoutOptions" />
              <a-input-number v-model:value="scene.durationSeconds" :min="10" :max="60" />
            </div>
            <a-textarea v-model:value="scene.narration" :rows="3" placeholder="旁白" />
            <a-textarea v-model:value="scene.visual" :rows="2" placeholder="画面说明" />
            <div class="scene-row">
              <a-textarea v-model:value="scene.subtitle" :rows="2" placeholder="字幕" />
              <a-textarea :value="linesFromArray(scene.onScreenText)" :rows="2" placeholder="屏幕短句，每行一条" @change="updateLines(scene, 'onScreenText', $event)" />
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleFilled,
  SyncOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons-vue'
import request from '@/utils/request'
import { getAuthToken } from '@/utils/authStorage'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const SCRIPT_URL = `${API_BASE}/ai/micro-video/script`

type EditMode = 'cards' | 'json'

type MicroTask = {
  id: number
  status: string
  progress: number
  title?: string
  videoUrl?: string
  coverUrl?: string
  subtitleUrl?: string
  audioUrl?: string
  durationSeconds?: number
  warningsJson?: string
  renderStatsJson?: string
  errorMessage?: string
  courseId?: number
  chapterId?: number
}

type Scene = {
  index: number
  title: string
  durationSeconds?: number
  narration: string
  visual: string
  subtitle?: string
  materials?: string[]
  layoutType?: string
  visualPlan?: Record<string, any>
  onScreenText?: string[]
  motion?: string[]
  keyframePrompt?: string
  subtitleSegments?: string[]
  voiceStyle?: string
}

const styleOptions = ['标准讲解', '案例导入', '任务驱动']
const qualityOptions = [
  { label: '快速', value: 'fast' },
  { label: '标准', value: 'standard' },
  { label: '关键帧增强', value: 'keyframe' },
]
const cloudVoiceOptions = [
  { label: '温柔女声', value: 'warm_female' },
  { label: '清晰男声', value: 'clear_male' },
  { label: '沉稳讲师', value: 'calm_teacher' },
  { label: '活泼讲解', value: 'bright_teacher' },
]
const layoutOptions = ['protocol', 'flow', 'comparison', 'code', 'timeline', 'concept', 'stack', 'queue', 'tree', 'graph', 'memory_table'].map((value) => ({ label: value, value }))

const form = reactive({
  topic: '',
  knowledgePoints: '',
  grade: '本科一年级',
  durationMinutes: 5,
  style: '标准讲解',
  qualityMode: 'standard',
  voiceId: 'warm_female',
  subtitlesEnabled: true,
  burnSubtitles: true,
  useAiKeyframes: false,
})

const scriptLoading = ref(false)
const renderLoading = ref(false)
const renderPolling = ref(false)
const publishLoading = ref(false)
const editMode = ref<EditMode>('cards')
const scriptDraft = ref<any | null>(null)
const editableScript = ref('')
const taskId = ref<number | null>(null)
const currentTask = ref<MicroTask | null>(null)
const pollTimer = ref<number | null>(null)

watch(() => form.qualityMode, (value) => {
  form.useAiKeyframes = value === 'keyframe'
})

const parsedScenes = computed<Scene[]>(() => {
  const scenes = scriptDraft.value?.scenes
  return Array.isArray(scenes) ? scenes : []
})

const taskStatus = computed(() => currentTask.value?.status || (scriptDraft.value ? 'scripted' : 'idle'))
const renderProgress = computed(() => Number(currentTask.value?.progress || 0))
const videoUrl = computed(() => cacheBusted(currentTask.value?.videoUrl || ''))
const coverUrl = computed(() => cacheBusted(currentTask.value?.coverUrl || ''))
const taskError = computed(() => currentTask.value?.errorMessage || '')
const canPublish = computed(() => taskStatus.value === 'succeeded' && !!videoUrl.value && audioValid.value)
const renderStats = computed<Record<string, any>>(() => parseJson(currentTask.value?.renderStatsJson, {}))
const warningItems = computed<string[]>(() => parseJson(currentTask.value?.warningsJson, []))
const audioValid = computed(() => renderStats.value?.audio?.valid !== false)
const durationLabel = computed(() => {
  const seconds = Number(currentTask.value?.durationSeconds || renderStats.value.durationSeconds || 0)
  if (!seconds) return '等待生成'
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
})
const audioStatus = computed(() => {
  const status = renderStats.value?.audio?.status
  const provider = renderStats.value?.audio?.provider
  if (!currentTask.value?.audioUrl) return '等待生成'
  if (!audioValid.value) return '音量异常'
  return status === 'ok' ? (provider === 'windows_sapi' ? '本地 TTS 已对齐' : 'TTS 配音') : '无可用 TTS'
})
const voiceLabel = computed(() => {
  const voice = renderStats.value?.audio?.resolvedVoiceName
  if (voice) return voice
  return currentTask.value?.audioUrl ? '本地默认声音' : '等待生成'
})
const syncStatusLabel = computed(() => {
  const sync = renderStats.value?.sync
  if (!sync) return currentTask.value?.videoUrl ? '等待检测' : '等待生成'
  if (sync.status === 'timeline_aligned') {
    const audio = Number(sync.audioSeconds || 0)
    const video = Number(sync.videoSeconds || 0)
    const diff = Math.abs(audio - video)
    return diff <= 0.5 ? '已按音频对齐' : `需复核 ${diff.toFixed(1)}s`
  }
  return sync.status || '等待检测'
})
const volumeLabel = computed(() => {
  const volume = renderStats.value?.audio?.meanVolumeDb
  if (volume === undefined || volume === null) return '等待检测'
  return `${Number(volume).toFixed(1)} dB`
})
const bitrateLabel = computed(() => {
  const bitrate = Number(renderStats.value?.video?.bitrate || 0)
  if (!bitrate) return '等待生成'
  return `${Math.round(bitrate / 1000)} kbps`
})
const keyframeLabel = computed(() => {
  const count = Number(renderStats.value?.keyframeCount || 0)
  if (!count) return form.qualityMode === 'keyframe' ? '已降级模板' : '本地模板'
  return `${count} 张`
})
const statusText = computed(() => {
  if (scriptLoading.value) return '脚本生成中'
  if (renderPolling.value) return `视频渲染中 ${renderProgress.value}%`
  if (taskStatus.value === 'queued') return '任务已排队'
  if (taskStatus.value === 'running') return `视频渲染中 ${renderProgress.value}%`
  if (taskStatus.value === 'succeeded') return '视频已生成'
  if (taskStatus.value === 'failed') return '渲染失败'
  if (scriptDraft.value) return '导演稿已就绪'
  return '等待生成'
})

const extractJson = (text: string) => {
  const clean = (text || '').trim().replace(/^```(?:json)?\s*/i, '').replace(/```$/i, '').trim()
  const first = clean.indexOf('{')
  const last = clean.lastIndexOf('}')
  return first >= 0 && last > first ? clean.slice(first, last + 1) : clean
}

const parseJson = <T,>(text: string | undefined, fallback: T): T => {
  if (!text) return fallback
  try {
    return JSON.parse(text) as T
  } catch {
    return fallback
  }
}

const cacheBusted = (url: string) => {
  if (!url) return ''
  const joiner = url.includes('?') ? '&' : '?'
  return `${url}${joiner}v=${currentTask.value?.id || taskId.value || 'task'}-${currentTask.value?.status || 'pending'}-${currentTask.value?.progress || 0}`
}

const generateScript = async () => {
  if (!form.topic.trim()) {
    message.warning('请先填写课程主题')
    return
  }
  clearTask()
  scriptLoading.value = true
  scriptDraft.value = null
  editableScript.value = ''
  editMode.value = 'cards'

  try {
    const res = await fetch(SCRIPT_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
      },
      credentials: 'include',
      body: JSON.stringify({ ...form }),
    })
    if (!res.ok || !res.body) {
      throw new Error(await res.text() || '脚本生成失败')
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let raw = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (!line.trim()) continue
        try {
          const event = JSON.parse(line)
          if (event.type === 'content') {
            raw += event.delta || ''
          }
          if (event.type === 'error') {
            throw new Error(event.message || '脚本生成失败')
          }
        } catch (error: any) {
          if (line.trim().startsWith('{')) throw error
          raw += line
        }
      }
    }

    scriptDraft.value = JSON.parse(extractJson(raw))
    syncJsonFromDraft()
    message.success('微课导演稿已生成')
  } catch (error: any) {
    message.error(error?.message || '脚本生成失败')
  } finally {
    scriptLoading.value = false
  }
}

const submitRender = async () => {
  const finalScript = buildScriptPayload()
  if (!finalScript) {
    message.warning('请先生成脚本')
    return
  }

  renderLoading.value = true
  try {
    const task = await request.post<any, MicroTask>('/ai/micro-video/render', {
      title: scriptDraft.value?.title || form.topic,
      scriptJson: finalScript,
      durationMinutes: form.durationMinutes,
      subtitlesEnabled: form.subtitlesEnabled,
      style: form.style,
      qualityMode: form.qualityMode,
      voiceId: form.voiceId,
      burnSubtitles: form.burnSubtitles,
      useAiKeyframes: form.useAiKeyframes,
    })
    taskId.value = task.id
    currentTask.value = task
    startPolling()
    message.success('已提交视频渲染任务')
  } catch (error: any) {
    message.error(error?.message || '提交渲染失败')
  } finally {
    renderLoading.value = false
  }
}

const buildScriptPayload = () => {
  if (editMode.value === 'json') {
    applyJsonEdit()
  } else {
    syncJsonFromDraft()
  }
  if (!scriptDraft.value) return ''
  return JSON.stringify(scriptDraft.value)
}

const syncJsonFromDraft = () => {
  editableScript.value = scriptDraft.value ? JSON.stringify(scriptDraft.value, null, 2) : ''
}

const applyJsonEdit = () => {
  if (!editableScript.value.trim()) return
  try {
    scriptDraft.value = JSON.parse(extractJson(editableScript.value))
    syncJsonFromDraft()
  } catch {
    message.error('JSON 格式不正确，请修正后再渲染')
  }
}

const startPolling = () => {
  if (!taskId.value) return
  stopPolling()
  renderPolling.value = true
  pollTimer.value = window.setInterval(fetchTask, 2500)
  fetchTask()
}

const stopPolling = () => {
  if (pollTimer.value) {
    window.clearInterval(pollTimer.value)
    pollTimer.value = null
  }
  renderPolling.value = false
}

const fetchTask = async () => {
  if (!taskId.value) return
  try {
    const task = await request.get<any, MicroTask>(`/ai/micro-video/task/${taskId.value}`, {
      skipErrorToast: true,
    })
    currentTask.value = task
    if (task.status === 'succeeded' || task.status === 'failed') {
      stopPolling()
      if (task.status === 'succeeded') {
        message.success('微课视频渲染完成')
      }
    }
  } catch {
    stopPolling()
    message.warning('任务轮询中断，请检查后端日志或刷新后重试')
  }
}

const publishMicroVideo = async () => {
  if (!taskId.value) return
  publishLoading.value = true
  try {
    const updated = await request.post<any, MicroTask>('/ai/micro-video/save', {
      taskId: taskId.value,
      title: scriptDraft.value?.title || form.topic,
      duration: durationLabel.value === '等待生成' ? `${form.durationMinutes}分钟` : durationLabel.value,
    })
    currentTask.value = updated
    message.success('微课已保存到我的资源，可在我的资源中发布')
  } catch (error: any) {
    message.error(error?.message || '发布失败')
  } finally {
    publishLoading.value = false
  }
}

const copyScript = async () => {
  syncJsonFromDraft()
  await navigator.clipboard.writeText(editableScript.value)
  message.success('已复制脚本')
}

const linesFromArray = (value?: string[]) => (Array.isArray(value) ? value.join('\n') : '')

const updateLines = (scene: Scene, key: 'onScreenText', event: Event) => {
  const value = (event.target as HTMLTextAreaElement).value
  scene[key] = value.split('\n').map((item) => item.trim()).filter(Boolean)
}

const clearTask = () => {
  stopPolling()
  taskId.value = null
  currentTask.value = null
}

const resetAll = () => {
  Object.assign(form, {
    topic: '',
    knowledgePoints: '',
    grade: '本科一年级',
    durationMinutes: 5,
    style: '标准讲解',
    qualityMode: 'standard',
    voiceId: 'warm_female',
    subtitlesEnabled: true,
    burnSubtitles: true,
    useAiKeyframes: false,
  })
  scriptDraft.value = null
  editableScript.value = ''
  editMode.value = 'cards'
  clearTask()
}

</script>

<style scoped>
.micro-video-shell {
  display: grid;
  grid-template-columns: 390px minmax(0, 1fr);
  height: 100%;
  background: #f3f6fa;
}

.micro-config {
  padding: 24px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
}

.micro-result {
  padding: 24px;
}

.scroll-y {
  overflow-y: auto;
}

.panel-heading,
.result-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  color: #102033;
  font-weight: 800;
}

.panel-heading {
  justify-content: flex-start;
  font-size: 18px;
}

.micro-grid,
.scene-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.scene-row {
  grid-template-columns: minmax(0, 1fr) 150px 92px;
  align-items: center;
}

.switch-row {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #e8eef6;
  border-radius: 8px;
  padding: 0 12px;
  margin-bottom: 10px;
  background: #fbfdff;
}

.action-row,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #d8e4f0;
  background: #ffffff;
  border-radius: 999px;
  padding: 8px 14px;
  color: #334155;
}

.status-pill.succeeded {
  color: #047857;
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.status-pill.failed {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fff1f2;
}

.empty-state {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #64748b;
  text-align: center;
}

.empty-state :deep(svg) {
  font-size: 48px;
  color: #2563eb;
  margin-bottom: 16px;
}

.empty-state h3 {
  color: #102033;
  margin-bottom: 8px;
}

.video-preview {
  background: #0b1220;
  border-radius: 8px;
  overflow: hidden;
  margin: 18px 0;
}

.video-preview video {
  width: 100%;
  aspect-ratio: 16 / 9;
  display: block;
  background: #020617;
}

.video-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #e5e7eb;
  padding: 12px 16px;
}

.video-meta a {
  color: #93c5fd;
}

.diagnostic-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.diag-card,
.story-title,
.scene-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.diag-card {
  padding: 14px 16px;
}

.diag-label {
  display: block;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}

.diag-card strong {
  color: #102033;
}

.task-alert {
  margin: 16px 0;
}

.script-editor {
  font-family: Consolas, Monaco, monospace;
  border-radius: 8px;
}

.storyboard {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.story-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
}

.story-title h2 {
  margin: 0 0 8px;
  color: #102033;
}

.story-title p {
  margin: 0;
  color: #64748b;
}

.scene-card {
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 16px;
  padding: 18px;
}

.scene-index {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  background: #102033;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.scene-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scene-title-input :deep(input) {
  font-weight: 700;
}

@media (max-width: 1180px) {
  .micro-video-shell {
    grid-template-columns: 1fr;
  }

  .micro-config {
    border-right: none;
    border-bottom: 1px solid #e2e8f0;
  }

  .diagnostic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
