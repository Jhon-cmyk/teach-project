<template>
  <div class="concept-stage" :class="{ compact: props.compact, embedded: props.embedded }">
    <!-- 顶部:核心概念 + 一句话核心观点 -->
    <div class="concept-header">
      <div class="concept-main-term">
        <span class="term-dot"></span>
        <span class="term-text">{{ payload.mainTerm }}</span>
      </div>
      <div class="concept-core-idea">{{ payload.coreIdea }}</div>
    </div>

    <!-- 中部:当前 step 的卡片 -->
    <div class="concept-card">
      <div class="concept-card-top" v-if="currentStep.focus || currentStep.visualHint">
        <div v-if="currentStep.focus" class="concept-focus-chip">
          {{ currentStep.focus }}
        </div>
        <div v-if="currentStep.visualHint" class="concept-hint-chip" :class="hintClass">
          {{ currentStep.visualHint }}
        </div>
      </div>

      <!-- ================== 视觉原语渲染区 ================== -->
      <div v-if="currentStep.visual" class="concept-visual">
        <!-- nodes-chain -->
        <div
          v-if="currentStep.visual.type === 'nodes-chain'"
          class="visual-nodes-chain"
          :class="nodesChainMotionClass"
        >
          <template v-for="(node, idx) in currentStep.visual.nodes" :key="`node-${idx}-${stepIndex}`">
            <div
              class="node-box"
              :class="{
                'node-highlight': (currentStep.visual.highlight || []).includes(idx),
                'node-action-target':
                  currentStep.visual.actionIndex === idx &&
                  currentStep.visual.action !== 'none',
                'node-entering': isNodeEntering(idx),
                'node-deleting': isNodeDeleting(idx),
              }"
            >
              {{ node }}
              <div
                v-if="currentStep.visual.actionIndex === idx && currentStep.visual.action && currentStep.visual.action !== 'none'"
                class="node-action-badge"
                :class="`action-${currentStep.visual.action}`"
              >
                {{ actionBadgeText(currentStep.visual) }}
              </div>
            </div>
            <div
              v-if="idx < currentStep.visual.nodes.length - 1"
              class="node-connector"
              :class="{
                'node-connector-linked': currentStep.visual.linked,
                'node-connector-rewire': isConnectorRewiring(idx),
              }"
            >
              <span v-if="currentStep.visual.linked" class="arrow-head">→</span>
            </div>
          </template>
        </div>

        <!-- tree -->
        <div
          v-else-if="currentStep.visual.type === 'tree'"
          class="visual-tree"
        >
          <svg
            class="tree-svg"
            :viewBox="treeViewBox"
            preserveAspectRatio="xMidYMid meet"
          >
            <line
              v-for="(edge, ei) in treeLayout.edges"
              :key="`edge-${ei}`"
              :x1="edge.x1"
              :y1="edge.y1"
              :x2="edge.x2"
              :y2="edge.y2"
              stroke="#b4c6f0"
              stroke-width="2"
            />
            <g
              v-for="(n, ni) in treeLayout.nodes"
              :key="`node-${ni}`"
              :transform="`translate(${n.x}, ${n.y})`"
              :class="{
                'tree-path-node': isTreeNodeOnPath(n.value),
                'tree-highlight-node': isTreeNodeHighlight(n.value),
              }"
              :style="treeNodeStyle(n.value)"
            >
              <circle
                :r="20"
                :fill="getTreeNodeFill(n.value)"
                :stroke="getTreeNodeStroke(n.value)"
                stroke-width="2"
              />
              <text
                text-anchor="middle"
                dominant-baseline="central"
                :fill="getTreeNodeText(n.value)"
                font-size="13"
                font-weight="700"
              >
                {{ n.value }}
              </text>
            </g>
          </svg>
        </div>

        <!-- branching -->
        <div
          v-else-if="currentStep.visual.type === 'branching'"
          class="visual-branching"
        >
          <div class="branch-diamond">
            <div class="diamond-inner">
              <div class="diamond-label">条件</div>
              <div class="diamond-text">{{ currentStep.visual.condition }}</div>
            </div>
          </div>
          <div class="branch-pipes">
            <div class="pipe pipe-true" :class="{ active: currentStep.visual.activeBranch === 'true' }">
              <span class="pipe-label">真</span>
            </div>
            <div class="pipe pipe-false" :class="{ active: currentStep.visual.activeBranch === 'false' }">
              <span class="pipe-label">假</span>
            </div>
          </div>
          <div class="branch-results">
            <div class="branch-box" :class="{ active: currentStep.visual.activeBranch === 'true' }">
              {{ currentStep.visual.trueLabel }}
            </div>
            <div class="branch-box" :class="{ active: currentStep.visual.activeBranch === 'false' }">
              {{ currentStep.visual.falseLabel }}
            </div>
          </div>
        </div>

        <!-- comparison -->
        <div
          v-else-if="currentStep.visual.type === 'comparison'"
          class="visual-comparison"
        >
          <div class="compare-col" :class="{ winner: currentStep.visual.winner === 'left' }">
            <div class="compare-title">{{ currentStep.visual.leftTitle }}</div>
            <ul class="compare-list">
              <li v-for="(item, idx) in currentStep.visual.leftItems" :key="`l-${idx}-${stepIndex}`">
                {{ item }}
              </li>
            </ul>
          </div>
          <div class="compare-vs">VS</div>
          <div class="compare-col" :class="{ winner: currentStep.visual.winner === 'right' }">
            <div class="compare-title">{{ currentStep.visual.rightTitle }}</div>
            <ul class="compare-list">
              <li v-for="(item, idx) in currentStep.visual.rightItems" :key="`r-${idx}-${stepIndex}`">
                {{ item }}
              </li>
            </ul>
          </div>
        </div>

        <!-- highlight-card -->
        <div
          v-else-if="currentStep.visual.type === 'highlight-card'"
          class="visual-highlight-card"
          :class="`tone-${currentStep.visual.tone || 'info'}`"
        >
          <div class="hc-label" v-if="currentStep.visual.label">{{ currentStep.visual.label }}</div>
          <div class="hc-main">{{ currentStep.visual.mainValue }}</div>
        </div>

        <!-- flow -->
        <div
          v-else-if="currentStep.visual.type === 'flow'"
          class="visual-flow"
        >
          <template v-for="(box, idx) in currentStep.visual.boxes" :key="`flow-${idx}-${stepIndex}`">
            <div
              class="flow-box"
              :class="{
                active: currentStep.visual.activeIndex === idx,
                done: typeof currentStep.visual.activeIndex === 'number' && idx < currentStep.visual.activeIndex,
              }"
              :style="{ '--flow-delay': `${idx * 80}ms` }"
            >
              {{ box }}
            </div>
            <div v-if="idx < currentStep.visual.boxes.length - 1" class="flow-arrow">→</div>
          </template>
          <div v-if="currentStep.visual.loopBack" class="flow-loop-hint">循环回到起点</div>
        </div>
      </div>

      <!-- 类比 -->
      <div v-if="showSupplementText && currentStep.analogy" class="concept-analogy">
        <span class="analogy-label">类比</span>
        <span class="analogy-text">{{ currentStep.analogy }}</span>
      </div>

      <!-- 关键点 -->
      <div
        v-if="showSupplementText && Array.isArray(currentStep.keyPoints) && currentStep.keyPoints.length"
        class="concept-points"
      >
        <div
          v-for="(point, idx) in currentStep.keyPoints"
          :key="`kp-${idx}-${stepIndex}`"
          class="concept-point-item"
        >
          <div class="point-index">{{ idx + 1 }}</div>
          <div class="point-text">{{ point }}</div>
        </div>
      </div>

      <!-- 代码片段 -->
      <div v-if="showSupplementText && currentStep.codeSnippet" class="concept-code-wrap">
        <div class="code-tag">示例代码</div>
        <pre class="concept-code"><code>{{ currentStep.codeSnippet }}</code></pre>
      </div>

      <!-- 进度点 -->
      <div class="concept-progress">
        <span
          v-for="(_, idx) in payload.steps"
          :key="`dot-${idx}`"
          class="progress-dot"
          :class="{ active: idx === stepIndex, done: idx < stepIndex }"
        ></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  ConceptAnimPayload,
  ConceptAnimStep,
  TreeNode,
  VisualNodesChain,
} from './core/animTypes.ts'

const props = defineProps<{
  payload: ConceptAnimPayload
  stepIndex: number
  compact?: boolean
  embedded?: boolean
}>()

const currentStep = computed<ConceptAnimStep>(() => {
  return (
    props.payload.steps[props.stepIndex] || {
      title: '',
      desc: '',
      focus: '',
      keyPoints: [],
      visualHint: '',
    }
  )
})

const hintClass = computed(() => {
  const hint = (currentStep.value.visualHint || '').trim()
  if (/起点|开始|定义/.test(hint)) return 'start'
  if (/结束|总结|小结|应用/.test(hint)) return 'end'
  if (/提醒|易错|误区|坑/.test(hint)) return 'warn'
  if (/流程|过程|步骤/.test(hint)) return 'flow'
  return 'default'
})

const showSupplementText = computed(() => !currentStep.value.visual)

const actionBadgeText = (v: VisualNodesChain) => {
  const label =
    v.action === 'insert'
      ? '插入'
      : v.action === 'delete'
        ? '删除'
        : v.action === 'access'
          ? '访问'
          : v.action === 'search'
            ? '查找'
            : ''
  if (!label) return ''
  if (v.actionValue !== undefined && v.actionValue !== null) {
    return `${label} ${v.actionValue}`
  }
  return label
}

const nodesChainMotionClass = computed(() => {
  const v = currentStep.value.visual
  if (!v || v.type !== 'nodes-chain') return ''
  const motionType = currentStep.value.motion?.type
  const action = v.action
  if (motionType === 'insert-node' || action === 'insert') return 'motion-insert'
  if (motionType === 'delete-node' || action === 'delete') return 'motion-delete'
  if (motionType === 'visit' || action === 'access' || action === 'search') return 'motion-visit'
  return ''
})

const getNodeActionIndex = () => {
  const v = currentStep.value.visual
  if (!v || v.type !== 'nodes-chain') return undefined
  return currentStep.value.motion?.toIndex ?? currentStep.value.motion?.fromIndex ?? v.actionIndex
}

const isNodeEntering = (index: number) => {
  const v = currentStep.value.visual
  if (!v || v.type !== 'nodes-chain') return false
  return (currentStep.value.motion?.type === 'insert-node' || v.action === 'insert') &&
    getNodeActionIndex() === index
}

const isNodeDeleting = (index: number) => {
  const v = currentStep.value.visual
  if (!v || v.type !== 'nodes-chain') return false
  return (currentStep.value.motion?.type === 'delete-node' || v.action === 'delete') &&
    getNodeActionIndex() === index
}

const isConnectorRewiring = (index: number) => {
  const actionIndex = getNodeActionIndex()
  if (actionIndex === undefined) return false
  return index === actionIndex || index === actionIndex - 1
}

/* ================== Tree 布局 ================== */
interface PositionedNode {
  value: string | number
  x: number
  y: number
  depth: number
}

interface TreeEdge {
  x1: number
  y1: number
  x2: number
  y2: number
}

interface TreeLayout {
  nodes: PositionedNode[]
  edges: TreeEdge[]
  width: number
  height: number
}

// 收紧间距,让树更紧凑
const NODE_H_GAP = 48
const NODE_V_GAP = 58
const PADDING = 26

const computeTreeLayout = (root: TreeNode | null | undefined): TreeLayout => {
  if (!root) return { nodes: [], edges: [], width: 280, height: 160 }

  const positioned: PositionedNode[] = []
  const edges: TreeEdge[] = []
  let xCounter = 0

  const walk = (node: TreeNode | null | undefined, depth: number): PositionedNode | null => {
    if (!node || typeof node !== 'object') return null

    const leftPos = walk(node.left, depth + 1)
    const myXIndex = xCounter++
    const myPos: PositionedNode = {
      value: node.value,
      depth,
      x: PADDING + myXIndex * NODE_H_GAP,
      y: PADDING + depth * NODE_V_GAP,
    }
    positioned.push(myPos)

    if (leftPos) edges.push({ x1: myPos.x, y1: myPos.y, x2: leftPos.x, y2: leftPos.y })
    const rightPos = walk(node.right, depth + 1)
    if (rightPos) edges.push({ x1: myPos.x, y1: myPos.y, x2: rightPos.x, y2: rightPos.y })

    return myPos
  }

  walk(root, 0)

  const maxX = positioned.reduce((m, n) => Math.max(m, n.x), 0)
  const maxY = positioned.reduce((m, n) => Math.max(m, n.y), 0)

  return {
    nodes: positioned,
    edges,
    width: Math.max(280, maxX + PADDING),
    height: Math.max(160, maxY + PADDING),
  }
}

const treeLayout = computed<TreeLayout>(() => {
  const v = currentStep.value.visual
  if (!v || v.type !== 'tree') return { nodes: [], edges: [], width: 280, height: 160 }
  return computeTreeLayout(v.root)
})

const treeViewBox = computed(() => `0 0 ${treeLayout.value.width} ${treeLayout.value.height}`)

const isTreeNodeHighlight = (value: string | number): boolean => {
  const v = currentStep.value.visual
  if (!v || v.type !== 'tree') return false
  return (v.highlight || []).some((x) => String(x) === String(value))
}

const isTreeNodeOnPath = (value: string | number): boolean => {
  const v = currentStep.value.visual
  if (!v || v.type !== 'tree') return false
  return (v.path || []).some((x) => String(x) === String(value))
}

const treePathOrder = (value: string | number): number => {
  const v = currentStep.value.visual
  if (!v || v.type !== 'tree') return -1
  return (v.path || []).findIndex((x) => String(x) === String(value))
}

const treeNodeStyle = (value: string | number) => {
  const order = treePathOrder(value)
  return order >= 0 ? { '--tree-delay': `${order * 160}ms` } : {}
}

const getTreeNodeFill = (value: string | number) => {
  if (isTreeNodeHighlight(value)) return '#fff7ed'
  if (isTreeNodeOnPath(value)) return '#eef4ff'
  return '#ffffff'
}
const getTreeNodeStroke = (value: string | number) => {
  if (isTreeNodeHighlight(value)) return '#f59e0b'
  if (isTreeNodeOnPath(value)) return '#6366f1'
  return '#b4c6f0'
}
const getTreeNodeText = (value: string | number) => {
  if (isTreeNodeHighlight(value)) return '#9a5a00'
  if (isTreeNodeOnPath(value)) return '#3b4ea0'
  return '#23386d'
}
</script>

<style scoped>
/* ====== 关键:不要再占过多高度,让父容器决定尺寸 ====== */
.concept-stage {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f7faff 0%, #eef4ff 100%);
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
  /* 去掉了之前的 min-height: 250px 和 overflow: hidden */
}

/* ==================== 顶部 ==================== */
.concept-header {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #dbe5ff;
}

.concept-main-term {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 800;
  color: #23386d;
}

.term-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.concept-core-idea {
  color: #4a5b86;
  font-size: 12.5px;
  line-height: 1.55;
}

/* ==================== 中部卡片 ==================== */
.concept-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #dbe5ff;
  box-shadow: 0 8px 20px rgba(83, 112, 255, 0.08);
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.concept-card-top {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.concept-focus-chip {
  padding: 4px 11px;
  border-radius: 999px;
  background: linear-gradient(135deg, #eef4ff, #edf0ff);
  color: #3b4ea0;
  font-size: 11.5px;
  font-weight: 700;
}

.concept-hint-chip {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 700;
  background: #edf3ff;
  color: #4667c6;
}
.concept-hint-chip.start { background: #ebfff4; color: #177247; }
.concept-hint-chip.end   { background: #f2eeff; color: #6747c7; }
.concept-hint-chip.warn  { background: #fff7ed; color: #c2410c; }
.concept-hint-chip.flow  { background: #eef4ff; color: #2c4ea2; }

/* ==================== 视觉原语容器(收紧 padding) ==================== */
.concept-visual {
  padding: 14px 12px;
  border-radius: 12px;
  background: linear-gradient(180deg, #f8fbff 0%, #f0f5ff 100%);
  border: 1px solid #e2ecff;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  overflow: hidden;
}

/* ---- nodes-chain ---- */
.visual-nodes-chain {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 5px;
  row-gap: 34px;
}

.node-box {
  position: relative;
  min-width: 46px;
  height: 46px;
  padding: 0 8px;
  border-radius: 12px;
  background: #ffffff;
  border: 2px solid #c8d7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  color: #23386d;
  box-shadow: 0 4px 12px rgba(83, 112, 255, 0.1);
  transition: all 0.25s ease;
}

.node-box.node-highlight {
  background: #fff4de;
  border-color: #ffbf5f;
  color: #9a5a00;
  transform: translateY(-3px);
  box-shadow: 0 8px 18px rgba(255, 191, 95, 0.3);
}

.node-box.node-action-target {
  transform: translateY(-5px);
  box-shadow: 0 10px 22px rgba(99, 102, 241, 0.28);
  border-color: #6366f1;
}

.node-action-badge {
  position: absolute;
  bottom: -24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 800;
  white-space: nowrap;
  box-shadow: 0 3px 8px rgba(99, 102, 241, 0.25);
}

.node-action-badge.action-insert { background: #dcfce7; color: #166534; }
.node-action-badge.action-delete { background: #ffe4e6; color: #9f1239; }
.node-action-badge.action-access,
.node-action-badge.action-search { background: #e0e7ff; color: #3730a3; }

.node-connector {
  width: 14px;
  height: 2px;
  background: #d1ddf5;
}
.node-connector-linked {
  background: transparent;
  width: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 2px;
}
.node-connector-linked .arrow-head {
  color: #6b7fc7;
  font-size: 16px;
  font-weight: 900;
}

.visual-nodes-chain.motion-insert .node-entering {
  animation: node-fly-in 0.72s cubic-bezier(0.2, 0.9, 0.2, 1.1) both;
}

.visual-nodes-chain.motion-delete .node-deleting {
  animation: node-pop-out 0.72s ease both;
}

.visual-nodes-chain.motion-visit .node-action-target,
.visual-nodes-chain.motion-visit .node-highlight {
  animation: node-visit-pulse 0.88s ease both;
}

.node-connector-rewire .arrow-head,
.node-connector-rewire {
  animation: connector-rewire 0.86s ease both;
}

/* ---- tree(关键:限制最大高度) ---- */
.visual-tree {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 2px;
  box-sizing: border-box;
  overflow: hidden;
}

.tree-svg {
  width: 100%;
  max-width: 480px;
  height: auto;
  max-height: 260px;   /* 限制树的最大高度 */
  display: block;
}

/* ---- branching(整体缩小,避免超高) ---- */
.tree-svg .tree-path-node {
  animation: tree-path-pulse 0.7s ease both;
  animation-delay: var(--tree-delay, 0ms);
}

.tree-svg .tree-highlight-node {
  animation: tree-target-pop 0.85s ease both;
  animation-delay: var(--tree-delay, 0ms);
}

.visual-branching {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100%;
  min-width: 0;
}

.branch-diamond {
  width: 160px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(45deg);
  background: linear-gradient(135deg, #eef4ff, #e6edff);
  border: 2px solid #93b4ff;
  border-radius: 10px;
  flex-shrink: 0;
}
.diamond-inner {
  transform: rotate(-45deg);
  text-align: center;
  padding: 0 6px;
  max-width: 120px;
}
.diamond-label {
  font-size: 10px;
  font-weight: 800;
  color: #6b7fc7;
  letter-spacing: 1px;
}
.diamond-text {
  margin-top: 2px;
  font-size: 13px;
  font-weight: 800;
  color: #1e3a8a;
  word-break: break-word;
}

.branch-pipes {
  display: flex;
  gap: 110px;
  margin-top: 2px;
}
.pipe {
  width: 2px;
  height: 24px;
  background: #c8d7ff;
  position: relative;
}
.pipe-label {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  left: 6px;
  font-size: 10.5px;
  font-weight: 800;
  color: #6b7fc7;
  padding: 1px 6px;
  border-radius: 5px;
  background: #ffffff;
  border: 1px solid #dbe5ff;
  white-space: nowrap;
}
.pipe.active {
  background: #6366f1;
  box-shadow: 0 0 6px rgba(99, 102, 241, 0.5);
  animation: branch-light 0.8s ease both;
}
.pipe.active .pipe-label {
  color: #4338ca;
  border-color: #a5b4fc;
  background: #eef2ff;
}

.branch-results {
  display: flex;
  gap: 18px;
  max-width: 100%;
  flex-wrap: wrap;
  justify-content: center;
}
.branch-box {
  min-width: 110px;
  max-width: 180px;
  padding: 9px 14px;
  border-radius: 10px;
  background: #ffffff;
  border: 2px solid #d4dffd;
  text-align: center;
  font-weight: 700;
  color: #4b5d8e;
  font-size: 12.5px;
  transition: all 0.25s ease;
  word-break: break-word;
}
.branch-box.active {
  background: #eef2ff;
  border-color: #6366f1;
  color: #3730a3;
  box-shadow: 0 8px 18px rgba(99, 102, 241, 0.2);
  transform: translateY(-2px);
  animation: branch-box-select 0.75s ease both;
}

/* ---- comparison ---- */
.visual-comparison {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 10px;
  align-items: stretch;
  min-width: 0;
}
.compare-col {
  padding: 10px 12px;
  border-radius: 12px;
  background: #ffffff;
  border: 2px solid #e2ecff;
  transition: all 0.25s ease;
  min-width: 0;
}
.compare-col.winner {
  border-color: #34d399;
  box-shadow: 0 8px 18px rgba(52, 211, 153, 0.2);
}
.compare-title {
  font-weight: 800;
  color: #1e3a8a;
  margin-bottom: 6px;
  padding-bottom: 4px;
  border-bottom: 1px dashed #dbe5ff;
  font-size: 13px;
}
.compare-list {
  margin: 0;
  padding-left: 16px;
  color: #425272;
  font-size: 12.5px;
  line-height: 1.65;
  word-break: break-word;
}
.compare-vs {
  align-self: center;
  padding: 4px 8px;
  border-radius: 6px;
  background: #6366f1;
  color: #fff;
  font-weight: 900;
  font-size: 11px;
}

/* ---- highlight-card(关键:限制最大高度、收紧 padding) ---- */
.visual-highlight-card {
  padding: 18px 16px;
  border-radius: 14px;
  text-align: center;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  border: 1px solid #c7d2fe;
  max-height: 200px;     /* 关键:不再像之前那样独占大块垂直空间 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.visual-highlight-card.tone-success { background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%); border-color: #6ee7b7; }
.visual-highlight-card.tone-warn    { background: linear-gradient(135deg, #fff7ed 0%, #fed7aa 100%); border-color: #fdba74; }
.visual-highlight-card.tone-danger  { background: linear-gradient(135deg, #fef2f2 0%, #fecaca 100%); border-color: #fca5a5; }
.hc-label {
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 1.5px;
  color: #6b7fc7;
}
.hc-main {
  font-size: 22px;       /* 从 26px 缩小,避免独占过大 */
  font-weight: 900;
  color: #1e293b;
  line-height: 1.3;
  word-break: break-word;
}

/* ---- flow ---- */
.visual-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px;
  position: relative;
}
.flow-box {
  min-width: 86px;
  max-width: 100%;
  padding: 9px 12px;
  border-radius: 9px;
  background: #ffffff;
  border: 2px solid #d4dffd;
  color: #4b5d8e;
  font-weight: 700;
  font-size: 12.5px;
  text-align: center;
  transition: all 0.25s ease;
  word-break: break-word;
}
.flow-box.done {
  background: #f0fdf4;
  border-color: #86efac;
  color: #166534;
  animation: flow-step-done 0.45s ease both;
  animation-delay: var(--flow-delay, 0ms);
}
.flow-box.active {
  background: #eef2ff;
  border-color: #6366f1;
  color: #3730a3;
  transform: translateY(-3px);
  box-shadow: 0 8px 18px rgba(99, 102, 241, 0.2);
  animation: flow-cursor 0.72s ease both;
  animation-delay: var(--flow-delay, 0ms);
}
.flow-arrow {
  color: #6b7fc7;
  font-size: 18px;
  font-weight: 900;
  animation: flow-arrow-pulse 1.1s ease infinite;
}
.flow-loop-hint {
  width: 100%;
  text-align: center;
  margin-top: 6px;
  font-size: 11.5px;
  color: #6b7fc7;
  font-style: italic;
}

/* ==================== 文本补充区 ==================== */
.concept-analogy {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 9px 12px;
  border-radius: 12px;
  background: #f8fbff;
  border: 1px solid #e2ecff;
  color: #3a4a75;
  font-size: 12.5px;
  line-height: 1.6;
}
.analogy-label {
  flex-shrink: 0;
  padding: 2px 7px;
  border-radius: 5px;
  background: #e7eeff;
  color: #4667c6;
  font-size: 10.5px;
  font-weight: 800;
}
.analogy-text { flex: 1; }

.concept-points {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.concept-point-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 10px;
  background: #fafbff;
  border: 1px solid #eaeffd;
}
.point-index {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #ffffff;
  font-size: 11px;
  font-weight: 800;
}
.point-text {
  flex: 1;
  font-size: 12.5px;
  line-height: 1.55;
  color: #2a3759;
  word-break: break-word;
}

.concept-code-wrap {
  position: relative;
  margin-top: 4px;
}
.code-tag {
  position: absolute;
  top: -9px;
  left: 12px;
  padding: 2px 9px;
  border-radius: 5px;
  background: #1e293b;
  color: #f8fafc;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.concept-code {
  margin: 0;
  padding: 14px 12px 12px;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.65;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.concept-progress {
  display: flex;
  justify-content: center;
  gap: 5px;
  padding-top: 4px;
}
.progress-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #dbe5ff;
  transition: all 0.25s ease;
}
.progress-dot.done { background: #93b4ff; }
.progress-dot.active {
  width: 20px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
}

.concept-stage.compact {
  gap: 8px;
  padding: 10px 12px;
}

.concept-stage.compact .concept-header {
  display: none;
}

.concept-stage.compact .concept-card {
  padding: 10px 12px;
  gap: 8px;
}

.concept-stage.compact .concept-visual {
  padding: 12px 10px;
}

.concept-stage.compact .visual-highlight-card {
  max-height: 150px;
  padding: 14px 12px;
}

.concept-stage.compact .hc-main {
  font-size: 18px;
}

.concept-stage.compact .node-box {
  min-width: 40px;
  height: 40px;
  border-radius: 10px;
  font-size: 13px;
}

.concept-stage.compact .tree-svg {
  max-height: 220px;
}

.concept-stage.compact .flow-box {
  min-width: 72px;
  padding: 8px 10px;
}

.concept-stage.embedded {
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  padding: 8px 10px;
}

.concept-stage.embedded .concept-card {
  padding: 9px 10px;
}

.concept-stage.embedded .concept-card-top {
  display: none;
}

.concept-stage.embedded .concept-visual {
  padding: 10px 8px;
}

.concept-stage.embedded .visual-nodes-chain {
  gap: 4px;
  row-gap: 28px;
}

.concept-stage.embedded .node-box {
  min-width: 36px;
  height: 36px;
  padding: 0 6px;
  font-size: 12px;
}

.concept-stage.embedded .node-connector {
  width: 10px;
}

.concept-stage.embedded .tree-svg {
  max-height: 190px;
}

.concept-stage.embedded .flow-box {
  min-width: 64px;
  padding: 7px 8px;
  font-size: 11.5px;
}

/* 响应式 */
@keyframes node-fly-in {
  0% {
    opacity: 0;
    transform: translateY(-34px) scale(0.78);
  }
  58% {
    opacity: 1;
    transform: translateY(8px) scale(1.06);
  }
  100% {
    opacity: 1;
    transform: translateY(-5px) scale(1);
  }
}

@keyframes node-pop-out {
  0% {
    opacity: 1;
    transform: translateY(-5px) scale(1);
  }
  100% {
    opacity: 0.38;
    transform: translateY(26px) scale(0.84);
  }
}

@keyframes node-visit-pulse {
  0%, 100% {
    transform: translateY(-5px) scale(1);
  }
  45% {
    transform: translateY(-11px) scale(1.08);
  }
}

@keyframes connector-rewire {
  0% {
    opacity: 0.35;
    transform: scaleX(0.35);
  }
  55% {
    opacity: 1;
    transform: scaleX(1.18);
  }
  100% {
    opacity: 1;
    transform: scaleX(1);
  }
}

@keyframes tree-path-pulse {
  0% {
    opacity: 0.35;
    transform: scale(0.82);
  }
  60% {
    opacity: 1;
    transform: scale(1.16);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes tree-target-pop {
  0% {
    transform: scale(0.84);
  }
  55% {
    transform: scale(1.22);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes branch-light {
  0% {
    transform: scaleY(0.2);
    transform-origin: top;
  }
  100% {
    transform: scaleY(1);
    transform-origin: top;
  }
}

@keyframes branch-box-select {
  0% {
    opacity: 0.55;
    transform: translateY(8px);
  }
  100% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

@keyframes flow-step-done {
  from {
    opacity: 0.6;
    transform: translateY(3px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes flow-cursor {
  0% {
    transform: translateY(10px) scale(0.92);
  }
  55% {
    transform: translateY(-6px) scale(1.05);
  }
  100% {
    transform: translateY(-3px) scale(1);
  }
}

@keyframes flow-arrow-pulse {
  0%, 100% {
    opacity: 0.55;
    transform: translateX(0);
  }
  50% {
    opacity: 1;
    transform: translateX(3px);
  }
}

@media (max-width: 900px) {
  .visual-comparison {
    grid-template-columns: 1fr;
  }
  .compare-vs { justify-self: center; }
  .branch-diamond { width: 140px; height: 78px; }
  .branch-pipes { gap: 85px; }
  .branch-results {
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
  .hc-main { font-size: 18px; }
}

@media (max-width: 640px) {
  .branch-diamond { width: 120px; height: 68px; }
  .branch-pipes { gap: 65px; }
  .diamond-text { font-size: 11px; }
  .hc-main { font-size: 16px; }
  .tree-svg { max-height: 220px; }
}
</style>
