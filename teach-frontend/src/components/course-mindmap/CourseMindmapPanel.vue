<template>
  <div class="course-mindmap-panel">
    <!-- 加载状态 -->
    <div v-if="showInitialLoading" class="state-card">
      <div class="state-icon loading-ring"></div>
      <h3>正在生成课程思维导图</h3>
      <p>系统正在整理课程主线、模块关系与核心知识点，请稍候。</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="!safeData && error" class="state-card error-card">
      <div class="state-icon error-icon">!</div>
      <h3>课程思维导图加载失败</h3>
      <p>{{ error }}</p>
      <div class="state-actions">
        <a-button @click="$emit('retry')">重新加载</a-button>
        <a-button type="primary" :loading="regenerating" @click="$emit('regenerate')">
          重新生成
        </a-button>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!safeData" class="state-card empty-card">
      <div class="state-icon empty-icon">◌</div>
      <h3>暂无课程思维导图</h3>
      <p>当前课程还没有可展示的知识结构内容。</p>
      <div class="state-actions">
        <a-button type="primary" :loading="regenerating" @click="$emit('regenerate')">
          立即生成
        </a-button>
      </div>
    </div>

    <!-- 思维导图主体 -->
    <div v-else class="mindmap-shell">
      <!-- 头部 -->
      <div class="panel-header">
        <div class="header-main">
          <div class="title-row">
            <h3>{{ safeData.title }}</h3>
            <span class="status-badge" :class="statusClass">{{ statusText }}</span>
          </div>
          <p class="header-desc">课程知识结构思维导图，展示核心模块与知识点关系</p>
        </div>
        <div class="header-side">
          <div class="header-meta">
            <span class="meta-item">{{ branchList.length }} 个模块</span>
            <span class="meta-item">{{ totalLeafCount }} 个知识点</span>
            <span v-if="safeData.updatedAt" class="meta-time">{{ safeData.updatedAt }}</span>
          </div>
          <a-button
            size="small"
            type="primary"
            ghost
            :loading="regenerating"
            @click="$emit('regenerate')"
          >
            重新生成
          </a-button>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="error" class="inline-error">{{ error }}</div>

      <!-- 思维导图 SVG -->
      <div class="mindmap-container">
        <svg
          class="mindmap-svg"
          :viewBox="`0 0 ${layout.width} ${layout.height}`"
          preserveAspectRatio="xMidYMid meet"
        >
          <defs>
            <linearGradient id="gradLeft" x1="100%" y1="0%" x2="0%" y2="0%">
              <stop offset="0%" stop-color="#6366f1" stop-opacity="0.9"/>
              <stop offset="100%" stop-color="#a5b4fc" stop-opacity="0.3"/>
            </linearGradient>
            <linearGradient id="gradRight" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#6366f1" stop-opacity="0.9"/>
              <stop offset="100%" stop-color="#a5b4fc" stop-opacity="0.3"/>
            </linearGradient>
            <filter id="shadow" x="-50%" y="-50%" width="200%" height="200%">
              <feDropShadow dx="0" dy="2" stdDeviation="4" flood-color="#6366f1" flood-opacity="0.12"/>
            </filter>
          </defs>

          <!-- ========== 连接线 ========== -->
          <!-- 左侧：中心 -> 模块 -->
          <path
            v-for="(module, idx) in leftLayout"
            :key="`line-left-branch-${idx}`"
            :d="bezier(layout.centerX - 70, layout.centerY, module.x + module.w, module.y)"
            class="line-branch"
            stroke="url(#gradLeft)"
          />
          <!-- 左侧：模块 -> 知识点 -->
          <template v-for="(module, idx) in leftLayout" :key="`lines-left-leaf-${idx}`">
            <path
              v-for="(leaf, li) in module.leaves"
              :key="`line-left-leaf-${idx}-${li}`"
              :d="bezier(module.x, module.y, leaf.x + leaf.w, leaf.y)"
              class="line-leaf"
            />
          </template>

          <!-- 右侧：中心 -> 模块 -->
          <path
            v-for="(module, idx) in rightLayout"
            :key="`line-right-branch-${idx}`"
            :d="bezier(layout.centerX + 70, layout.centerY, module.x, module.y)"
            class="line-branch"
            stroke="url(#gradRight)"
          />
          <!-- 右侧：模块 -> 知识点 -->
          <template v-for="(module, idx) in rightLayout" :key="`lines-right-leaf-${idx}`">
            <path
              v-for="(leaf, li) in module.leaves"
              :key="`line-right-leaf-${idx}-${li}`"
              :d="bezier(module.x + module.w, module.y, leaf.x, leaf.y)"
              class="line-leaf"
            />
          </template>

          <!-- ========== 中心节点 ========== -->
          <g class="node-center">
            <circle
              :cx="layout.centerX"
              :cy="layout.centerY"
              r="72"
              fill="none"
              stroke="#e0e7ff"
              stroke-width="1.5"
              stroke-dasharray="6 4"
              opacity="0.6"
            />
            <rect
              :x="layout.centerX - 70"
              :y="layout.centerY - 45"
              width="140"
              height="90"
              rx="16"
              fill="#fff"
              stroke="#c7d2fe"
              stroke-width="1.5"
              filter="url(#shadow)"
            />
            <rect
              :x="layout.centerX - 30"
              :y="layout.centerY - 36"
              width="60"
              height="20"
              rx="10"
              fill="#6366f1"
            />
            <text
              :x="layout.centerX"
              :y="layout.centerY - 22"
              text-anchor="middle"
              fill="#fff"
              font-size="11"
              font-weight="600"
            >课程核心</text>
            <text
              :x="layout.centerX"
              :y="layout.centerY + 6"
              text-anchor="middle"
              fill="#1e293b"
              font-size="17"
              font-weight="700"
            >{{ truncate(safeData.root.name, 8) }}</text>
            <text
              :x="layout.centerX"
              :y="layout.centerY + 28"
              text-anchor="middle"
              fill="#64748b"
              font-size="11"
            >{{ branchList.length }} 模块 · {{ totalLeafCount }} 知识点</text>
          </g>

          <!-- ========== 左侧模块节点 ========== -->
          <g
            v-for="(module, idx) in leftLayout"
            :key="`node-left-branch-${idx}`"
            class="node-branch"
          >
            <title>{{ leftModules[idx].name }}</title>
            <rect
              :x="module.x"
              :y="module.y - 22"
              :width="module.w"
              height="44"
              rx="10"
              fill="#fff"
              stroke="#e0e7ff"
              stroke-width="1"
              filter="url(#shadow)"
            />
            <!-- 序号标签 -->
            <rect
              :x="module.x + module.w - 34"
              :y="module.y - 14"
              width="26"
              height="20"
              rx="6"
              fill="#6366f1"
            />
            <text
              :x="module.x + module.w - 21"
              :y="module.y"
              text-anchor="middle"
              fill="#fff"
              font-size="10"
              font-weight="600"
            >{{ pad(idx + 1) }}</text>
            <!-- 数量圆圈 -->
            <circle :cx="module.x + 16" :cy="module.y" r="11" fill="#f1f5f9"/>
            <text
              :x="module.x + 16"
              :y="module.y + 4"
              text-anchor="middle"
              fill="#64748b"
              font-size="10"
              font-weight="600"
            >{{ leftModules[idx].children?.length || 0 }}</text>
            <!-- 标题 -->
            <text
              :x="module.x + 34"
              :y="module.y + 4"
              fill="#1e293b"
              font-size="13"
              font-weight="600"
            >{{ truncate(leftModules[idx].name, 6) }}</text>
          </g>

          <!-- ========== 左侧知识点节点 ========== -->
          <template v-for="(module, idx) in leftLayout" :key="`nodes-left-leaf-${idx}`">
            <g
              v-for="(leaf, li) in module.leaves"
              :key="`node-left-leaf-${idx}-${li}`"
              class="node-leaf"
            >
              <rect
                :x="leaf.x"
                :y="leaf.y - 14"
                :width="leaf.w"
                height="28"
                rx="6"
                fill="#fff"
                stroke="#e2e8f0"
                stroke-width="0.5"
              />
              <text
                :x="leaf.x + leaf.w / 2"
                :y="leaf.y + 4"
                text-anchor="middle"
                fill="#475569"
                font-size="11"
              >{{ truncate(leftModules[idx].children?.[li]?.name || '', 7) }}</text>
            </g>
          </template>

          <!-- ========== 右侧模块节点 ========== -->
          <g
            v-for="(module, idx) in rightLayout"
            :key="`node-right-branch-${idx}`"
            class="node-branch"
          >
            <title>{{ rightModules[idx].name }}</title>
            <rect
              :x="module.x"
              :y="module.y - 22"
              :width="module.w"
              height="44"
              rx="10"
              fill="#fff"
              stroke="#e0e7ff"
              stroke-width="1"
              filter="url(#shadow)"
            />
            <!-- 序号标签 -->
            <rect
              :x="module.x + 8"
              :y="module.y - 14"
              width="26"
              height="20"
              rx="6"
              fill="#6366f1"
            />
            <text
              :x="module.x + 21"
              :y="module.y"
              text-anchor="middle"
              fill="#fff"
              font-size="10"
              font-weight="600"
            >{{ pad(leftModules.length + idx + 1) }}</text>
            <!-- 数量圆圈 -->
            <circle :cx="module.x + module.w - 16" :cy="module.y" r="11" fill="#f1f5f9"/>
            <text
              :x="module.x + module.w - 16"
              :y="module.y + 4"
              text-anchor="middle"
              fill="#64748b"
              font-size="10"
              font-weight="600"
            >{{ rightModules[idx].children?.length || 0 }}</text>
            <!-- 标题 -->
            <text
              :x="module.x + 42"
              :y="module.y + 4"
              fill="#1e293b"
              font-size="13"
              font-weight="600"
            >{{ truncate(rightModules[idx].name, 5) }}</text>
          </g>

          <!-- ========== 右侧知识点节点 ========== -->
          <template v-for="(module, idx) in rightLayout" :key="`nodes-right-leaf-${idx}`">
            <g
              v-for="(leaf, li) in module.leaves"
              :key="`node-right-leaf-${idx}-${li}`"
              class="node-leaf"
            >
              <rect
                :x="leaf.x"
                :y="leaf.y - 14"
                :width="leaf.w"
                height="28"
                rx="6"
                fill="#fff"
                stroke="#e2e8f0"
                stroke-width="0.5"
              />
              <text
                :x="leaf.x + leaf.w / 2"
                :y="leaf.y + 4"
                text-anchor="middle"
                fill="#475569"
                font-size="11"
              >{{ truncate(rightModules[idx].children?.[li]?.name || '', 7) }}</text>
            </g>
          </template>
        </svg>
      </div>

      <!-- 图例 -->
      <div class="mindmap-legend">
        <div class="legend-item">
          <span class="legend-dot center-dot"></span>
          <span>课程核心</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot branch-dot"></span>
          <span>知识模块</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot leaf-dot"></span>
          <span>知识点</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CourseMindmapData, CourseMindmapNode } from '@/types/courseMindmap'

const props = withDefaults(defineProps<{
  loading: boolean
  regenerating?: boolean
  error: string
  data: CourseMindmapData | null
}>(), {
  regenerating: false,
})

defineEmits<{
  (e: 'retry'): void
  (e: 'regenerate'): void
}>()

// ==================== 常量 ====================
const SVG_WIDTH = 1100
const MIN_HEIGHT = 400
const BRANCH_W = 160
const BRANCH_H = 44
const LEAF_W = 100
const LEAF_H = 28
const LEAF_GAP = 36
const BRANCH_GAP = 90
const CENTER_TO_BRANCH = 140
const BRANCH_TO_LEAF = 30

// ==================== 数据 ====================
const safeData = computed<CourseMindmapData | null>(() => {
  if (!props.data?.root?.name) return null
  return props.data
})

const showInitialLoading = computed(() => props.loading && !safeData.value)

const branchList = computed<CourseMindmapNode[]>(() =>
  safeData.value?.root?.children || []
)

const leftModules = computed(() => {
  const total = branchList.value.length
  return branchList.value.slice(0, Math.ceil(total / 2))
})

const rightModules = computed(() => {
  return branchList.value.slice(leftModules.value.length)
})

const totalLeafCount = computed(() =>
  branchList.value.reduce((s, m) => s + (m.children?.length || 0), 0)
)

const statusText = computed(() =>
  safeData.value?.status === 'fallback' ? '兜底生成' : 'AI 生成'
)

const statusClass = computed(() =>
  safeData.value?.status === 'fallback' ? 'status-fallback' : 'status-ready'
)

// ==================== 布局计算 ====================
interface LeafPos { x: number; y: number; w: number }
interface ModulePos { x: number; y: number; w: number; leaves: LeafPos[] }

const calcSideHeight = (modules: CourseMindmapNode[]) => {
  if (!modules.length) return 0
  let h = 0
  modules.forEach((m, i) => {
    const leafCount = Math.min(m.children?.length || 0, 4)
    const blockH = Math.max(BRANCH_H, leafCount * LEAF_GAP)
    h += blockH
    if (i < modules.length - 1) h += BRANCH_GAP
  })
  return h
}

const layout = computed(() => {
  const leftH = calcSideHeight(leftModules.value)
  const rightH = calcSideHeight(rightModules.value)
  const contentH = Math.max(leftH, rightH, 150)
  const height = Math.max(MIN_HEIGHT, contentH + 100)
  return {
    width: SVG_WIDTH,
    height,
    centerX: SVG_WIDTH / 2,
    centerY: height / 2
  }
})

const calcModulePositions = (
  modules: CourseMindmapNode[],
  side: 'left' | 'right'
): ModulePos[] => {
  const positions: ModulePos[] = []
  if (!modules.length) return positions

  const totalH = calcSideHeight(modules)
  const startY = layout.value.centerY - totalH / 2
  let currentY = startY

  modules.forEach((module) => {
    const leafCount = Math.min(module.children?.length || 0, 4)
    const blockH = Math.max(BRANCH_H, leafCount * LEAF_GAP)
    const moduleY = currentY + blockH / 2

    let moduleX: number
    let leafBaseX: number

    if (side === 'left') {
      moduleX = layout.value.centerX - 70 - CENTER_TO_BRANCH - BRANCH_W
      leafBaseX = moduleX - BRANCH_TO_LEAF - LEAF_W
    } else {
      moduleX = layout.value.centerX + 70 + CENTER_TO_BRANCH
      leafBaseX = moduleX + BRANCH_W + BRANCH_TO_LEAF
    }

    // 计算知识点位置
    const leaves: LeafPos[] = []
    if (leafCount > 0) {
      const leafStartY = moduleY - ((leafCount - 1) * LEAF_GAP) / 2
      for (let i = 0; i < leafCount; i++) {
        leaves.push({
          x: leafBaseX,
          y: leafStartY + i * LEAF_GAP,
          w: LEAF_W
        })
      }
    }

    positions.push({ x: moduleX, y: moduleY, w: BRANCH_W, leaves })
    currentY += blockH + BRANCH_GAP
  })

  return positions
}

const leftLayout = computed(() => calcModulePositions(leftModules.value, 'left'))
const rightLayout = computed(() => calcModulePositions(rightModules.value, 'right'))

// ==================== 工具函数 ====================
const truncate = (s: string, max: number) =>
  s && s.length > max ? s.slice(0, max) + '...' : (s || '')

const pad = (n: number) => String(n).padStart(2, '0')

const bezier = (x1: number, y1: number, x2: number, y2: number) => {
  const mx = (x1 + x2) / 2
  return `M${x1},${y1} C${mx},${y1} ${mx},${y2} ${x2},${y2}`
}
</script>

<style scoped>
.course-mindmap-panel {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.state-card,
.mindmap-shell {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.06);
}

.state-card {
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  text-align: center;
  padding: 32px 24px;
}

.state-card h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
  font-weight: 700;
}

.state-card p {
  margin: 0;
  max-width: 520px;
  color: #64748b;
  line-height: 1.7;
  font-size: 14px;
}

.state-actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}

.state-icon {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #2563eb;
  font-size: 24px;
  font-weight: 700;
}

.loading-ring {
  border: 3px solid #dbeafe;
  border-top-color: #2563eb;
  background: transparent;
  animation: spin 0.9s linear infinite;
}

.error-card .state-icon {
  background: #fef2f2;
  color: #ef4444;
}

.empty-card .state-icon {
  background: #f8fafc;
  color: #94a3b8;
}

.mindmap-shell {
  height: 100%;
  min-height: 100%;
  padding: 18px;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
}

.header-main { flex: 1; }

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.title-row h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.status-badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-ready {
  background: #ecfdf5;
  color: #059669;
}

.status-fallback {
  background: #fef3c7;
  color: #d97706;
}

.header-desc {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.header-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.header-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #64748b;
}

.meta-item {
  padding: 4px 10px;
  background: #f8fafc;
  border-radius: 6px;
}

.meta-time { color: #94a3b8; }

.inline-error {
  padding: 12px 16px;
  margin-bottom: 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #dc2626;
  font-size: 14px;
}

.mindmap-container {
  flex: 1;
  width: 100%;
  min-height: 500px;
  overflow-x: auto;
  background: linear-gradient(135deg, #fafbff 0%, #f5f7ff 100%);
  border-radius: 16px;
  border: 1px solid #e8eef6;
}

.mindmap-svg {
  display: block;
  width: 100%;
  min-width: 700px;
  height: 100%;
  min-height: 500px;
}

.line-branch {
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
}

.line-leaf {
  fill: none;
  stroke: #c7d2fe;
  stroke-width: 1.5;
  stroke-linecap: round;
}

.node-center,
.node-branch,
.node-leaf {
  transition: opacity 0.2s;
}

.node-branch:hover,
.node-leaf:hover {
  opacity: 0.85;
  cursor: default;  /* 加这一行，悬浮时显示默认箭头而非文字光标 */
}

.mindmap-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 4px;
}

.center-dot {
  background: linear-gradient(135deg, #6366f1, #818cf8);
}

.branch-dot {
  background: #fff;
  border: 1.5px solid #e0e7ff;
  box-shadow: 0 2px 4px rgba(99, 102, 241, 0.1);
}

.leaf-dot {
  background: #fff;
  border: 1px solid #e2e8f0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1100px) {
  .panel-header { flex-direction: column; }
  .header-side {
    flex-direction: row;
    justify-content: space-between;
    width: 100%;
  }
}

@media (max-width: 768px) {
  .mindmap-shell { padding: 16px; }
  .mindmap-legend { flex-wrap: wrap; gap: 16px; }
}
</style>
