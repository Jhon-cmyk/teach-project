<template>
  <div class="graph-stage" :class="{ compact: props.compact, embedded: props.embedded }">
    <div class="graph-canvas">
      <svg class="graph-svg" viewBox="0 0 520 300" preserveAspectRatio="xMidYMid meet">
        <defs>
          <marker id="graph-arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#94a3b8" />
          </marker>
          <marker id="graph-arrow-active" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#2563eb" />
          </marker>
        </defs>

        <g v-for="(edge, index) in normalizedEdges" :key="`edge-${index}`">
          <line
            :x1="nodePosition(edge.from).x"
            :y1="nodePosition(edge.from).y"
            :x2="nodePosition(edge.to).x"
            :y2="nodePosition(edge.to).y"
            class="graph-edge"
            :class="{ active: isActiveEdge(edge) }"
            :marker-end="edge.directed ? (isActiveEdge(edge) ? 'url(#graph-arrow-active)' : 'url(#graph-arrow)') : undefined"
          />
          <text
            v-if="edge.label"
            class="graph-edge-label"
            :x="(nodePosition(edge.from).x + nodePosition(edge.to).x) / 2"
            :y="(nodePosition(edge.from).y + nodePosition(edge.to).y) / 2 - 6"
          >
            {{ edge.label }}
          </text>
        </g>

        <g
          v-for="node in positionedNodes"
          :key="`${String(node.id)}-${stepIndex}`"
          :transform="`translate(${node.x}, ${node.y})`"
          class="graph-node"
          :class="{
            active: isActiveNode(node.id),
            visited: isVisited(node.id),
            frontier: isFrontier(node.id),
          }"
        >
          <circle r="24" />
          <text text-anchor="middle" dominant-baseline="central">{{ node.id }}</text>
        </g>
      </svg>
    </div>

    <div class="graph-side">
      <div class="graph-op-chip" :class="currentStep.operation">{{ operationLabel }}</div>
      <div class="graph-rule">{{ flowHint }}</div>
      <div class="graph-state">
        <span>Frontier</span>
        <strong>{{ frontierText }}</strong>
      </div>
      <div class="graph-state">
        <span>Visited</span>
        <strong>{{ visitedText }}</strong>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { GraphAnimPayload, GraphAnimStep, GraphEdge } from './core/animTypes.ts'

const props = defineProps<{
  payload: GraphAnimPayload
  stepIndex: number
  compact?: boolean
  embedded?: boolean
}>()

const currentStep = computed<GraphAnimStep>(() => {
  return (
    props.payload.steps[props.stepIndex] || {
      title: '',
      desc: '',
      operation: 'init',
      activeNode: props.payload.nodes?.[0],
      visited: [],
      frontier: [],
      activeEdges: [],
    }
  )
})

const normalizedEdges = computed<GraphEdge[]>(() => props.payload.edges || [])

const positionedNodes = computed(() => {
  const nodes = props.payload.nodes || []
  const cx = 260
  const cy = 150
  const radius = nodes.length <= 4 ? 92 : 112
  return nodes.map((id, index) => {
    const angle = -Math.PI / 2 + (index * Math.PI * 2) / Math.max(nodes.length, 1)
    return {
      id,
      x: cx + Math.cos(angle) * radius,
      y: cy + Math.sin(angle) * radius,
    }
  })
})

const nodePosition = (id: string | number) => {
  return positionedNodes.value.find((node) => String(node.id) === String(id)) || { x: 260, y: 150 }
}

const edgeKey = (from: unknown, to: unknown) => `${String(from)}->${String(to)}`

const activeEdgeKeys = computed(() => {
  const keys = new Set<string>()
  ;(currentStep.value.activeEdges || []).forEach((item: any) => {
    if (item && typeof item === 'object' && item.from !== undefined && item.to !== undefined) {
      keys.add(edgeKey(item.from, item.to))
      if (!item.directed) keys.add(edgeKey(item.to, item.from))
    } else if (typeof item === 'string') {
      keys.add(item)
    }
  })
  return keys
})

const isActiveEdge = (edge: GraphEdge) => {
  return activeEdgeKeys.value.has(edgeKey(edge.from, edge.to)) || activeEdgeKeys.value.has(edgeKey(edge.to, edge.from))
}

const isActiveNode = (id: string | number) => String(currentStep.value.activeNode) === String(id)
const isVisited = (id: string | number) => (currentStep.value.visited || []).map(String).includes(String(id))
const isFrontier = (id: string | number) => (currentStep.value.frontier || []).map(String).includes(String(id))

const operationLabel = computed(() => {
  const map: Record<GraphAnimStep['operation'], string> = {
    init: '初始化',
    visit: '访问节点',
    enqueue: '加入队列',
    dequeue: '队列取出',
    push: '压入栈',
    pop: '栈中弹出',
    relax: '松弛边',
    done: '完成',
  }
  return map[currentStep.value.operation] || '图操作'
})

const flowHint = computed(() => {
  if (currentStep.value.stageCaption) return currentStep.value.stageCaption
  if (currentStep.value.activeNode !== undefined && currentStep.value.activeNode !== null) {
    return `当前访问节点 ${currentStep.value.activeNode}`
  }
  return currentStep.value.title || '观察图结构'
})

const frontierText = computed(() => currentStep.value.frontier?.length ? currentStep.value.frontier.join('、') : '空')
const visitedText = computed(() => currentStep.value.visited?.length ? currentStep.value.visited.join('、') : '暂无')
</script>

<style scoped>
.graph-stage {
  min-height: 330px;
  display: grid;
  grid-template-columns: minmax(360px, 1fr) 260px;
  align-items: center;
  gap: 28px;
  padding: 30px 28px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background:
    radial-gradient(circle at 48% 40%, rgba(99, 102, 241, 0.11), transparent 32%),
    linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
}

.graph-canvas {
  min-height: 260px;
  display: grid;
  place-items: center;
  border: 1px solid #dbe6ff;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
  overflow: hidden;
}

.graph-svg {
  width: 100%;
  height: 260px;
}

.graph-edge {
  stroke: #94a3b8;
  stroke-width: 2.5;
  transition: all .22s ease;
}

.graph-edge.active {
  stroke: #2563eb;
  stroke-width: 4;
}

.graph-edge-label {
  fill: #64748b;
  font-size: 12px;
  font-weight: 800;
  paint-order: stroke;
  stroke: #ffffff;
  stroke-width: 4px;
}

.graph-node circle {
  fill: #ffffff;
  stroke: #bfdbfe;
  stroke-width: 2.5;
  filter: drop-shadow(0 10px 14px rgba(37, 99, 235, 0.12));
  transition: all .22s ease;
}

.graph-node text {
  fill: #1e3a8a;
  font-size: 14px;
  font-weight: 900;
}

.graph-node.frontier circle {
  fill: #fef3c7;
  stroke: #f59e0b;
}

.graph-node.visited circle {
  fill: #dcfce7;
  stroke: #22c55e;
}

.graph-node.active circle {
  fill: #fff7ed;
  stroke: #f97316;
  animation: graph-active 900ms ease both;
}

.graph-side {
  min-height: 210px;
  padding: 18px;
  border: 1px solid #dbe6ff;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.1);
}

.graph-op-chip {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 900;
}

.graph-op-chip.enqueue,
.graph-op-chip.push {
  background: #fef3c7;
  color: #92400e;
}

.graph-op-chip.relax {
  background: #f5f3ff;
  color: #6d28d9;
}

.graph-rule {
  margin-top: 16px;
  color: #334155;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.6;
}

.graph-state {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.graph-state strong {
  color: #1e3a8a;
  font-size: 14px;
}

.graph-stage.compact,
.graph-stage.embedded {
  grid-template-columns: 1fr;
  gap: 16px;
  padding: 20px;
}

@keyframes graph-active {
  0%, 100% { transform: scale(1); }
  45% { transform: scale(1.12); }
}
</style>
