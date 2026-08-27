<template>
  <div class="tree-stage" :class="{ compact: props.compact, embedded: props.embedded }">
    <div class="tree-canvas">
      <svg class="tree-svg" :viewBox="treeViewBox" preserveAspectRatio="xMidYMid meet">
        <line
          v-for="(edge, index) in treeLayout.edges"
          :key="`edge-${index}`"
          :x1="edge.x1"
          :y1="edge.y1"
          :x2="edge.x2"
          :y2="edge.y2"
          :class="{ active: isEdgeOnPath(edge.from, edge.to) }"
        />
        <g
          v-for="node in treeLayout.nodes"
          :key="`${String(node.value)}-${stepIndex}`"
          :transform="`translate(${node.x}, ${node.y})`"
          class="tree-node"
          :class="{
            current: isCurrent(node.value),
            visited: isVisited(node.value),
            path: isPath(node.value),
          }"
        >
          <circle r="22" />
          <text text-anchor="middle" dominant-baseline="central">{{ node.value }}</text>
        </g>
      </svg>
    </div>

    <div class="tree-side">
      <div class="tree-op-chip" :class="currentStep.operation">{{ operationLabel }}</div>
      <div class="tree-rule">{{ flowHint }}</div>
      <div class="tree-path">
        <span>当前路径</span>
        <strong>{{ pathText }}</strong>
      </div>
      <div class="tree-visited">
        <span>已访问</span>
        <strong>{{ visitedText }}</strong>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TreeAnimPayload, TreeAnimStep, TreeNode } from './core/animTypes.ts'

const props = defineProps<{
  payload: TreeAnimPayload
  stepIndex: number
  compact?: boolean
  embedded?: boolean
}>()

interface PositionedNode {
  value: string | number
  x: number
  y: number
}

interface PositionedEdge {
  from: string | number
  to: string | number
  x1: number
  y1: number
  x2: number
  y2: number
}

const currentStep = computed<TreeAnimStep>(() => {
  return (
    props.payload.steps[props.stepIndex] || {
      title: '',
      desc: '',
      operation: 'init',
      currentNode: props.payload.root?.value,
      path: [],
      visited: [],
    }
  )
})

const NODE_H_GAP = 52
const NODE_V_GAP = 62
const PADDING = 28

const computeTreeLayout = (root: TreeNode | null | undefined) => {
  const nodes: PositionedNode[] = []
  const edges: PositionedEdge[] = []
  let cursor = 0

  const walk = (node: TreeNode | null | undefined, depth: number): PositionedNode | null => {
    if (!node) return null
    const left = walk(node.left, depth + 1)
    const x = cursor * NODE_H_GAP + PADDING
    cursor += 1
    const current = { value: node.value, x, y: depth * NODE_V_GAP + PADDING }
    nodes.push(current)
    const right = walk(node.right, depth + 1)

    ;[left, right].forEach((child) => {
      if (!child) return
      edges.push({
        from: current.value,
        to: child.value,
        x1: current.x,
        y1: current.y + 22,
        x2: child.x,
        y2: child.y - 22,
      })
    })
    return current
  }

  walk(root, 0)
  const width = Math.max(240, cursor * NODE_H_GAP + PADDING)
  const height = Math.max(220, Math.max(...nodes.map((node) => node.y), 0) + PADDING + 36)
  return { nodes, edges, width, height }
}

const treeLayout = computed(() => computeTreeLayout(props.payload.root))
const treeViewBox = computed(() => `0 0 ${treeLayout.value.width} ${treeLayout.value.height}`)

const pathValues = computed(() => (currentStep.value.path || []).map(String))
const visitedValues = computed(() => (currentStep.value.visited || []).map(String))

const isCurrent = (value: string | number) => String(currentStep.value.currentNode) === String(value)
const isPath = (value: string | number) => pathValues.value.includes(String(value))
const isVisited = (value: string | number) => visitedValues.value.includes(String(value))

const isEdgeOnPath = (from: string | number, to: string | number) => {
  const path = pathValues.value
  for (let index = 0; index < path.length - 1; index += 1) {
    if (path[index] === String(from) && path[index + 1] === String(to)) return true
  }
  return false
}

const operationLabel = computed(() => {
  const map: Record<TreeAnimStep['operation'], string> = {
    init: '初始化',
    visit: '访问节点',
    compare: '比较判断',
    'go-left': '转向左子树',
    'go-right': '转向右子树',
    backtrack: '回溯',
    done: '完成',
  }
  return map[currentStep.value.operation] || '树操作'
})

const flowHint = computed(() => {
  if (currentStep.value.stageCaption) return currentStep.value.stageCaption
  if (currentStep.value.currentNode !== undefined && currentStep.value.currentNode !== null) {
    return `当前关注节点 ${currentStep.value.currentNode}`
  }
  return currentStep.value.title || '观察树结构'
})

const pathText = computed(() => currentStep.value.path?.length ? currentStep.value.path.join(' → ') : '暂无')
const visitedText = computed(() => currentStep.value.visited?.length ? currentStep.value.visited.join('、') : '暂无')
</script>

<style scoped>
.tree-stage {
  min-height: 330px;
  display: grid;
  grid-template-columns: minmax(340px, 1fr) 260px;
  align-items: center;
  gap: 28px;
  padding: 30px 28px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background:
    radial-gradient(circle at 50% 32%, rgba(16, 185, 129, 0.1), transparent 30%),
    linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
}

.tree-canvas {
  min-height: 260px;
  display: grid;
  place-items: center;
  border: 1px solid #dbe6ff;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  overflow: hidden;
}

.tree-svg {
  width: 100%;
  height: 260px;
}

.tree-svg line {
  stroke: #b4c6f0;
  stroke-width: 2.5;
  transition: all .25s ease;
}

.tree-svg line.active {
  stroke: #2563eb;
  stroke-width: 4;
}

.tree-node circle {
  fill: #ffffff;
  stroke: #bfdbfe;
  stroke-width: 2.5;
  filter: drop-shadow(0 10px 14px rgba(37, 99, 235, 0.12));
  transition: all .22s ease;
}

.tree-node text {
  fill: #1e3a8a;
  font-size: 13px;
  font-weight: 900;
}

.tree-node.path circle {
  fill: #eff6ff;
  stroke: #60a5fa;
}

.tree-node.visited circle {
  fill: #dcfce7;
  stroke: #22c55e;
}

.tree-node.current circle {
  fill: #fff7ed;
  stroke: #f97316;
  animation: tree-current 900ms ease both;
}

.tree-side {
  min-height: 210px;
  padding: 18px;
  border: 1px solid #dbe6ff;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.1);
}

.tree-op-chip {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 900;
}

.tree-op-chip.go-left,
.tree-op-chip.go-right {
  background: #ecfdf5;
  color: #047857;
}

.tree-op-chip.backtrack {
  background: #fef3c7;
  color: #92400e;
}

.tree-rule {
  margin-top: 16px;
  color: #334155;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.6;
}

.tree-path,
.tree-visited {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.tree-path strong,
.tree-visited strong {
  color: #1e3a8a;
  font-size: 14px;
}

.tree-stage.compact,
.tree-stage.embedded {
  grid-template-columns: 1fr;
  gap: 16px;
  padding: 20px;
}

@keyframes tree-current {
  0%, 100% { transform: scale(1); }
  45% { transform: scale(1.14); }
}
</style>
