<template>
  <section
    ref="workbenchRef"
    class="anim-workbench"
    :class="{
      'preview-only': previewMode,
      'embedded-workbench': isEmbeddedWorkbench,
      'compact-workbench': isCompactWorkbench,
      'narrow-workbench': isNarrowWorkbench,
    }"
  >
    <div v-if="!previewMode && payload" class="wb-toolbar">
      <div class="wb-toolbar-actions">
        <a-button class="wb-tool-btn" @click="$emit('optimize', 'vivid')" :disabled="disableOptimize">
          更生动形象
        </a-button>
        <a-button class="wb-tool-btn" @click="$emit('optimize', 'stable')" :disabled="disableOptimize">
          布局更稳健
        </a-button>
        <a-button class="wb-tool-btn" @click="$emit('optimize', 'slow')" :disabled="disableOptimize">
          节奏更慢
        </a-button>

        <span class="wb-divider"></span>

        <a-button class="wb-tool-btn" @click="$emit('copy-json')">
          <copy-outlined />
          复制
        </a-button>
        <a-button
          v-if="isEmbeddedWorkbench"
          class="wb-tool-btn"
          @click="fullPreviewVisible = true"
        >
          <fullscreen-outlined />
          放大预览
        </a-button>
        <a-button type="primary" class="wb-tool-btn wb-save-btn" @click="$emit('save-json')">
          <save-outlined />
          {{ currentResourceId ? '保存修改' : '保存至云端' }}
        </a-button>
      </div>
    </div>

    <!-- ===== 校验错误 ===== -->
    <div v-if="validationErrors.length && !previewMode" class="wb-error-box">
      <div class="wb-error-title">数据校验未通过</div>
      <ul>
        <li v-for="(item, index) in validationErrors" :key="`${item}-${index}`">{{ item }}</li>
      </ul>
    </div>

    <!-- ===== 生成中 Loading ===== -->
    <div v-if="(isGenerating || isOptimizing) && !previewMode" class="wb-loading-box">
      <div class="wb-spinner"></div>
      <div class="wb-loading-title">{{ isOptimizing ? '正在优化课件…' : '正在生成课件…' }}</div>
      <div class="wb-loading-desc">JSON 生成、校验与模板渲染中,请稍候</div>
    </div>

    <!-- ===== 动画播放器主体 ===== -->
    <template v-else-if="payload && currentStepData">
      <div class="wb-scene-card">
        <div class="wb-scene-header">
          <div class="wb-scene-title-row">
            <h2 class="wb-scene-title">{{ payload.title }}</h2>
            <span v-if="!previewMode" class="wb-badge" :class="renderStatus">{{ renderStatusLabel }}</span>
          </div>
          <p class="wb-scene-subtitle">{{ payload.subtitle }}</p>
        </div>

        <div class="wb-scene-stage">
          <div class="wb-stage-caption">{{ currentStageCaption }}</div>
          <SortAnimationPlayer
            v-if="payload.templateType === 'sort'"
            :payload="payload"
            :step-index="currentStepIndex"
            :compact="isCompactWorkbench"
            :embedded="isEmbeddedWorkbench"
          />
          <ProtocolAnimationPlayer
            v-else-if="payload.templateType === 'protocol'"
            :payload="payload"
            :step-index="currentStepIndex"
            :compact="isCompactWorkbench"
            :embedded="isEmbeddedWorkbench"
          />
          <StackAnimationPlayer
            v-else-if="payload.templateType === 'stack'"
            :payload="payload"
            :step-index="currentStepIndex"
            :compact="isCompactWorkbench"
            :embedded="isEmbeddedWorkbench"
          />
          <QueueAnimationPlayer
            v-else-if="payload.templateType === 'queue'"
            :payload="payload"
            :step-index="currentStepIndex"
            :compact="isCompactWorkbench"
            :embedded="isEmbeddedWorkbench"
          />
          <TreeAnimationPlayer
            v-else-if="payload.templateType === 'tree'"
            :payload="payload"
            :step-index="currentStepIndex"
            :compact="isCompactWorkbench"
            :embedded="isEmbeddedWorkbench"
          />
          <GraphAnimationPlayer
            v-else-if="payload.templateType === 'graph'"
            :payload="payload"
            :step-index="currentStepIndex"
            :compact="isCompactWorkbench"
            :embedded="isEmbeddedWorkbench"
          />
          <ConceptAnimationPlayer
            v-else-if="payload.templateType === 'concept'"
            :payload="payload"
            :step-index="currentStepIndex"
            :compact="isCompactWorkbench"
            :embedded="isEmbeddedWorkbench"
          />
        </div>

        <div class="wb-step-nav" aria-label="步骤导航">
          <button
            v-for="(_, index) in payload.steps"
            :key="`step-nav-${index}`"
            type="button"
            class="wb-step-dot"
            :class="{ active: index === currentStepIndex, done: index < currentStepIndex }"
            @click="goToStep(index)"
          >
            {{ index + 1 }}
          </button>
        </div>

        <div class="wb-step-panel">
          <div class="wb-step-content">
            <div class="wb-step-title">{{ currentStepData.title }}</div>
            <div class="wb-step-one-line">{{ currentShortHint }}</div>
          </div>
          <span class="wb-step-counter">{{ currentStepIndex + 1 }} / {{ totalSteps }}</span>
        </div>

        <div class="wb-controls">
          <a-button size="small" @click="prevStep" :disabled="currentStepIndex <= 0">
            <step-backward-outlined /> 上一步
          </a-button>
          <a-button size="small" @click="nextStep" :disabled="currentStepIndex >= totalSteps - 1">
            下一步 <step-forward-outlined />
          </a-button>
          <a-button
            size="small"
            type="primary"
            class="wb-autoplay-btn"
            @click="toggleAutoplay"
            :disabled="totalSteps <= 1"
          >
            <pause-outlined v-if="isAutoplaying" />
            <caret-right-outlined v-else />
            {{ isAutoplaying ? '暂停' : '自动播放' }}
          </a-button>
          <a-button size="small" @click="resetPlayback" :disabled="totalSteps <= 0">
            <undo-outlined /> 重置
          </a-button>
        </div>
      </div>
    </template>

    <!-- ===== 空状态 ===== -->
    <div v-else-if="!previewMode" class="wb-empty">
      <desktop-outlined class="wb-empty-icon" />
      <p class="wb-empty-text">输入核心概念后,AI 将为您生成可分步演示的互动课件</p>
      <span class="wb-empty-hint">当前支持排序、协议、栈、队列、树、图、通用概念模板</span>
    </div>
    <a-modal
      v-if="fullPreviewVisible"
      v-model:open="fullPreviewVisible"
      title="课件完整预览"
      width="1100px"
      :footer="null"
      centered
      class="wb-full-preview-modal"
    >
      <div class="wb-full-preview-body">
        <AnimationWorkbench
          :payload="payload"
          :render-status="renderStatus"
          :validation-errors="validationErrors"
          :is-generating="false"
          :is-optimizing="false"
          :autoplay-delay="autoplayDelay"
          :preview-mode="true"
          display-mode="full"
        />
      </div>
    </a-modal>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import SortAnimationPlayer from './SortAnimationPlayer.vue'
import ProtocolAnimationPlayer from './ProtocolAnimationPlayer.vue'
import StackAnimationPlayer from './StackAnimationPlayer.vue'
import QueueAnimationPlayer from './QueueAnimationPlayer.vue'
import TreeAnimationPlayer from './TreeAnimationPlayer.vue'
import GraphAnimationPlayer from './GraphAnimationPlayer.vue'
import ConceptAnimationPlayer from './ConceptAnimationPlayer.vue'
import {
  DesktopOutlined,
  CopyOutlined,
  SaveOutlined,
  StepBackwardOutlined,
  StepForwardOutlined,
  CaretRightOutlined,
  PauseOutlined,
  UndoOutlined,
  FullscreenOutlined,
} from '@ant-design/icons-vue'
import type {
  AnimOptimizeAction,
  AnimPayload,
  AnimRenderStatus,
  AnimBaseStep,
} from '../anim-player/core/animTypes'

const props = withDefaults(
  defineProps<{
    payload: AnimPayload | null
    renderStatus: AnimRenderStatus
    validationErrors: string[]
    isGenerating?: boolean
    isOptimizing?: boolean
    autoplayDelay?: number
    previewMode?: boolean
    currentResourceId?: number | null
    displayMode?: 'auto' | 'embedded' | 'full'
  }>(),
  {
    isGenerating: false,
    isOptimizing: false,
    autoplayDelay: 1800,
    previewMode: false,
    currentResourceId: null,
    displayMode: 'auto',
  },
)

const emit = defineEmits<{
  (e: 'optimize', action: AnimOptimizeAction): void
  (e: 'copy-json'): void
  (e: 'save-json'): void
  (e: 'step-change', stepIndex: number): void
}>()

const currentStepIndex = ref(0)
const isAutoplaying = ref(false)
const workbenchRef = ref<HTMLElement | null>(null)
const workbenchWidth = ref(0)
const fullPreviewVisible = ref(false)
let autoplayTimer: ReturnType<typeof setInterval> | null = null
let resizeObserver: ResizeObserver | null = null

const totalSteps = computed(() => props.payload?.steps?.length ?? 0)
const isEmbeddedWorkbench = computed(() => !props.previewMode && props.displayMode === 'embedded')
const isCompactWorkbench = computed(() => {
  if (props.previewMode || props.displayMode === 'full') return false
  if (props.displayMode === 'embedded') return true
  return workbenchWidth.value > 0 && workbenchWidth.value < 980
})
const isNarrowWorkbench = computed(() => {
  if (props.previewMode || props.displayMode === 'full') return false
  if (props.displayMode === 'embedded') return true
  return workbenchWidth.value > 0 && workbenchWidth.value < 720
})

const currentStepData = computed<AnimBaseStep | null>(() => {
  if (!props.payload || !props.payload.steps?.length) return null
  return props.payload.steps[currentStepIndex.value] || null
})

const currentStageCaption = computed(() => {
  const step = currentStepData.value
  return step?.stageCaption || step?.title || '观察动画流程'
})

const currentShortHint = computed(() => {
  const step = currentStepData.value
  const text = step?.desc || step?.stageCaption || ''
  return text.length > 54 ? `${text.slice(0, 54)}...` : text
})

const renderStatusLabel = computed(() => {
  if (props.renderStatus === 'ready') return '已通过验收'
  if (props.renderStatus === 'fallback') return '精品模板'
  if (props.renderStatus === 'validating') return '校验中'
  return '待生成'
})

const disableOptimize = computed(() => {
  return !props.payload || props.isGenerating || props.isOptimizing
})

const stopAutoplay = () => {
  if (autoplayTimer) {
    clearInterval(autoplayTimer)
    autoplayTimer = null
  }
  isAutoplaying.value = false
}

const nextStep = () => {
  if (!totalSteps.value) return
  if (currentStepIndex.value >= totalSteps.value - 1) {
    stopAutoplay()
    return
  }
  currentStepIndex.value += 1
}

const prevStep = () => {
  if (currentStepIndex.value <= 0) return
  currentStepIndex.value -= 1
}

const goToStep = (index: number) => {
  if (index < 0 || index >= totalSteps.value) return
  stopAutoplay()
  currentStepIndex.value = index
}

const resetPlayback = () => {
  stopAutoplay()
  currentStepIndex.value = 0
}

const toggleAutoplay = () => {
  if (isAutoplaying.value) {
    stopAutoplay()
    return
  }
  if (totalSteps.value <= 1) return
  autoplayTimer = setInterval(() => {
    if (currentStepIndex.value >= totalSteps.value - 1) {
      stopAutoplay()
      return
    }
    currentStepIndex.value += 1
  }, props.autoplayDelay)
  isAutoplaying.value = true
}

watch(
  () => props.payload,
  () => { resetPlayback() },
  { deep: true },
)

watch(
  () => [props.isGenerating, props.isOptimizing],
  ([generating, optimizing]) => {
    if (generating || optimizing) stopAutoplay()
  },
)

watch(currentStepIndex, (value) => {
  emit('step-change', value)
})

onMounted(() => {
  if (!workbenchRef.value) return
  workbenchWidth.value = workbenchRef.value.clientWidth
  resizeObserver = new ResizeObserver(([entry]) => {
    workbenchWidth.value = entry.contentRect.width
  })
  resizeObserver.observe(workbenchRef.value)
})

onBeforeUnmount(() => {
  stopAutoplay()
  resizeObserver?.disconnect()
  resizeObserver = null
})
</script>

<style scoped>
/* ===== 容器:整个工作台高度占满,内部用 flex 分层 ===== */
.anim-workbench {
  display: flex;
  flex-direction: column;
  height: 100%;
  flex: 1;
  padding: 20px;
  min-height: 0;       /* 关键:允许自身收缩 */
  box-sizing: border-box;
}

/* ===== 工具栏(固定高度,不参与收缩) ===== */
.wb-toolbar {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
  flex-shrink: 0;       /* 关键:不让工具栏被挤压 */
}

.wb-toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.wb-divider {
  width: 1px;
  height: 18px;
  background: #e2e8f0;
  margin: 0 6px;
  flex-shrink: 0;
}

.wb-tool-btn {
  font-weight: 600;
  font-size: 13px;
  border-radius: 5px;
}

.wb-save-btn {
  background: #3b82f6;
  border-color: #3b82f6;
  font-weight: 700;
}

.wb-save-btn:hover, .wb-save-btn:focus {
  background: #2563eb !important;
  border-color: #2563eb !important;
}

/* ===== 校验错误(固定高度,不收缩) ===== */
.wb-error-box {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  padding: 14px 18px;
  margin-bottom: 14px;
  flex-shrink: 0;
}

.wb-error-title {
  color: #1e40af;
  font-weight: 700;
  margin-bottom: 6px;
}

.wb-error-box ul {
  margin: 0;
  padding-left: 18px;
  color: #2563eb;
  font-size: 13px;
  line-height: 1.7;
}

/* ===== Loading ===== */
.wb-loading-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  text-align: center;
}

.wb-spinner {
  width: 38px;
  height: 38px;
  border: 3px solid #dbeafe;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: wb-spin 0.9s linear infinite;
  margin-bottom: 16px;
}

@keyframes wb-spin {
  to { transform: rotate(360deg); }
}

.wb-loading-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
}

.wb-loading-desc {
  font-size: 13px;
  color: #94a3b8;
}

/* ===== 场景卡片:内部三段式布局(header 固定 / stage 可伸缩 / step+controls 固定) ===== */
.wb-scene-card {
  border-radius: 14px;
  background: #fafbfd;
  border: 1px solid #edf0f7;
  display: flex;
  flex-direction: column;
  flex: 1;                /* 占满剩余高度 */
  min-height: 0;          /* 关键 */
  overflow: hidden;       /* 让内部自己滚动 */
}

.wb-scene-header {
  padding: 16px 22px 10px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;        /* 不被挤压 */
}

.wb-scene-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.wb-scene-title {
  margin: 0;
  font-size: 17px;
  font-weight: 800;
  color: #1e293b;
}

.wb-scene-subtitle {
  margin: 4px 0 0;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.5;
}

.wb-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 700;
}

.wb-badge.ready      { background: #dcfce7; color: #166534; }
.wb-badge.fallback   { background: #fff7ed; color: #c2410c; }
.wb-badge.validating { background: #eff6ff; color: #2563eb; }
.wb-badge.idle       { background: #f1f5f9; color: #64748b; }

/* ========== 核心修复:stage 区可收缩 + 可滚动 ========== */
.wb-scene-stage {
  padding: 10px 16px 14px;
  flex: 1;              /* 占据中间可用空间 */
  min-height: 0;        /* 允许收缩 */
  min-width: 0;
  overflow-y: auto;     /* 内部溢出自己滚动,不撑破父容器 */
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #ffffff;
}

.wb-scene-stage > * {
  max-width: 100%;
  min-width: 0;
}

.wb-stage-caption {
  align-self: center;
  max-width: min(760px, 92%);
  padding: 8px 14px;
  border-radius: 999px;
  background: #0f172a;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.35;
  text-align: center;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.18);
  flex-shrink: 0;
}

/* 自定义滚动条(低调一些) */
.wb-scene-stage::-webkit-scrollbar {
  width: 6px;
}
.wb-scene-stage::-webkit-scrollbar-thumb {
  background: #d4dffd;
  border-radius: 3px;
}
.wb-scene-stage::-webkit-scrollbar-thumb:hover {
  background: #93b4ff;
}

/* step 面板(固定高度) */
.wb-step-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  padding: 8px 22px;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;
  flex-shrink: 0;
}

.wb-step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid #dbe5ff;
  background: #ffffff;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.18s ease;
}

.wb-step-dot:hover {
  border-color: #93b4ff;
  color: #3556b1;
}

.wb-step-dot.done {
  background: #eff6ff;
  color: #3b82f6;
}

.wb-step-dot.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: #ffffff;
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.22);
}

.wb-step-panel {
  padding: 9px 22px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-shrink: 0;
  background: #fafbfd;
}

.wb-step-content {
  flex: 1;
  min-width: 0;
}

.wb-step-title {
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
}

.wb-step-one-line {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.wb-step-counter {
  white-space: nowrap;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 10px;
  background: #f8fafc;
  border-radius: 99px;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

/* 控制栏(始终在底部) */
.wb-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 10px 22px 14px;
  border-top: 1px solid #f1f5f9;
  flex-shrink: 0;
  background: #fafbfd;
}
.wb-autoplay-btn {
  background: linear-gradient(135deg, #3b82f6, #6366f1) !important;
  border: none !important;
}
.wb-autoplay-btn:hover {
  opacity: 0.9;
}

/* ===== 空状态 ===== */
.wb-empty {
  flex: 1;
  min-height: 260px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.wb-empty-icon {
  font-size: 52px;
  margin-bottom: 14px;
  color: #cbd5e1;
}

.wb-empty-text {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.wb-empty-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

/* ===== 预览模式(资源中心弹窗)微调 ===== */
.anim-workbench.preview-only {
  padding: 0;
  height: 100%;
}

.anim-workbench.preview-only .wb-scene-card {
  border: none;
  background: transparent;
  border-radius: 0;
}

.anim-workbench.preview-only .wb-scene-header {
  padding: 10px 16px 8px;
}

.anim-workbench.preview-only .wb-scene-title {
  font-size: 16px;
}

.anim-workbench.preview-only .wb-scene-stage {
  padding: 10px 14px;
}

.anim-workbench.preview-only .wb-step-panel {
  padding: 10px 16px;
  background: transparent;
}

.anim-workbench.preview-only .wb-controls {
  padding: 10px 16px 12px;
  background: transparent;
}

/* ===== 响应式 ===== */
.anim-workbench.compact-workbench {
  padding: 12px;
}

.anim-workbench.embedded-workbench {
  padding: 10px;
}

.anim-workbench.compact-workbench .wb-toolbar {
  margin-bottom: 10px;
}

.anim-workbench.compact-workbench .wb-toolbar-actions {
  gap: 8px;
  justify-content: center;
}

.anim-workbench.compact-workbench .wb-tool-btn {
  min-width: 0;
  padding-inline: 12px;
  font-size: 12px;
}

.anim-workbench.compact-workbench .wb-divider {
  display: none;
}

.anim-workbench.compact-workbench .wb-scene-header {
  padding: 14px 18px 8px;
}

.anim-workbench.compact-workbench .wb-scene-title {
  font-size: 16px;
}

.anim-workbench.compact-workbench .wb-scene-stage {
  padding: 8px 12px 10px;
  gap: 8px;
  overflow-y: auto;
}

.anim-workbench.embedded-workbench .wb-scene-stage {
  overflow-x: hidden;
}

.anim-workbench.compact-workbench .wb-stage-caption {
  max-width: 88%;
  padding: 7px 12px;
  font-size: 12px;
}

.anim-workbench.compact-workbench .wb-step-nav {
  padding: 7px 14px;
  gap: 5px;
  flex-wrap: wrap;
}

.anim-workbench.compact-workbench .wb-step-dot {
  width: 26px;
  height: 26px;
}

.anim-workbench.compact-workbench .wb-step-panel {
  padding: 8px 16px;
}

.anim-workbench.compact-workbench .wb-controls {
  padding: 8px 14px 12px;
  gap: 6px;
  flex-wrap: wrap;
}

.anim-workbench.narrow-workbench .wb-toolbar-actions {
  width: 100%;
}

.anim-workbench.narrow-workbench .wb-tool-btn {
  flex: 1 1 136px;
}

.wb-full-preview-body {
  height: 680px;
  max-height: calc(100vh - 160px);
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

:global(.wb-full-preview-modal) {
  max-width: calc(100vw - 48px);
}

:global(.wb-full-preview-modal .ant-modal-body) {
  overflow: hidden;
}

.wb-full-preview-body > .anim-workbench {
  min-height: 0;
}

@media (max-width: 1200px) {
  .wb-step-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .wb-step-one-line {
    white-space: normal;
  }

  .wb-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
