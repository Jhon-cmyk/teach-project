<template>
  <div class="sort-stage" :class="{ compact: props.compact, embedded: props.embedded }">
    <div class="sort-flow-label">{{ actionLabel }}</div>

    <div class="sort-array-row">
      <div
        v-for="(num, index) in currentStep.array"
        :key="`${stepIndex}-${index}-${num}`"
        class="sort-item-wrap"
        :style="blockStyle(num, index)"
      >
        <div
          class="sort-block"
          :class="{
            active: activeIndexes.includes(index),
            swapped: swapIndexes.includes(index),
            sorted: isSortedIndex(index),
            moving: Math.abs(moveDelta(num, index)) > 0,
          }"
        >
          {{ num }}
        </div>
        <div class="sort-index">{{ index }}</div>
      </div>

      <div
        v-if="activeIndexes.length >= 2"
        class="compare-bridge"
        :style="compareBridgeStyle"
      ></div>
    </div>

    <div v-if="typeof currentStep.sortedTailStart === 'number'" class="sorted-boundary">
      <span></span>
      <strong>已排序区从 {{ currentStep.sortedTailStart }} 开始</strong>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SortAnimPayload, SortAnimStep } from './core/animTypes.ts'

const props = defineProps<{
  payload: SortAnimPayload
  stepIndex: number
  compact?: boolean
  embedded?: boolean
}>()

const currentStep = computed<SortAnimStep>(() => {
  return (
    props.payload.steps[props.stepIndex] || {
      title: '',
      desc: '',
      array: props.payload.initialData || [],
      highlight: [],
      swap: [],
      sortedTailStart: null,
    }
  )
})

const previousStep = computed<SortAnimStep>(() => {
  if (props.stepIndex <= 0) {
    return {
      title: '',
      desc: '',
      array: props.payload.initialData || currentStep.value.array || [],
      highlight: [],
      swap: [],
      sortedTailStart: null,
    }
  }
  return props.payload.steps[props.stepIndex - 1] || currentStep.value
})

const inferredMotionType = computed(() => {
  const motionType = currentStep.value.motion?.type
  if (motionType) return motionType
  if ((currentStep.value.swap || []).length === 2) return 'swap'
  if ((currentStep.value.highlight || []).length >= 2) return 'compare'
  if (typeof currentStep.value.sortedTailStart === 'number') return 'done'
  return 'observe'
})

const activeIndexes = computed(() => {
  const fromMotion = currentStep.value.motion?.indexes
  if (Array.isArray(fromMotion) && fromMotion.length) return fromMotion
  return currentStep.value.highlight || []
})

const swapIndexes = computed(() => {
  const motion = currentStep.value.motion
  if (motion?.type === 'swap' && Array.isArray(motion.indexes)) return motion.indexes
  return currentStep.value.swap || []
})

const actionLabel = computed(() => {
  if (currentStep.value.stageCaption) return currentStep.value.stageCaption
  if (inferredMotionType.value === 'swap' && swapIndexes.value.length === 2) {
    return `两个方块交换位置: ${swapIndexes.value[0]} ↔ ${swapIndexes.value[1]}`
  }
  if (inferredMotionType.value === 'compare' && activeIndexes.value.length >= 2) {
    return `抬起两个方块进行比较: ${activeIndexes.value[0]} 和 ${activeIndexes.value[1]}`
  }
  if (inferredMotionType.value === 'done') return '右侧淡绿色区域表示已经排好'
  return currentStep.value.title || '观察当前数组'
})

const isSortedIndex = (index: number) => {
  return (
    currentStep.value.sortedTailStart !== null &&
    currentStep.value.sortedTailStart !== undefined &&
    index >= currentStep.value.sortedTailStart
  )
}

const previousIndexByOccurrence = (value: number, currentIndex: number) => {
  const prev = previousStep.value.array || []
  const currentPrefixCount = currentStep.value.array
    .slice(0, currentIndex + 1)
    .filter((item) => item === value).length
  let seen = 0
  for (let i = 0; i < prev.length; i += 1) {
    if (prev[i] === value) {
      seen += 1
      if (seen === currentPrefixCount) return i
    }
  }
  return currentIndex
}

const moveDelta = (value: number, index: number) => {
  const prevIndex = previousIndexByOccurrence(value, index)
  return (prevIndex - index) * 78
}

const blockStyle = (value: number, index: number) => {
  const delta = moveDelta(value, index)
  return {
    '--from-x': `${delta}px`,
    '--delay': `${Math.min(index * 40, 220)}ms`,
  }
}

const compareBridgeStyle = computed(() => {
  const [a, b] = [...activeIndexes.value].sort((x, y) => x - y)
  const itemWidth = props.compact ? 54 : 64
  const gap = props.compact ? 10 : 14
  const unit = itemWidth + gap
  return {
    left: `${a * unit + itemWidth / 2}px`,
    width: `${Math.max(itemWidth, (b - a) * unit)}px`,
  }
})
</script>

<style scoped>
.sort-stage {
  min-height: 330px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 22px;
  padding: 30px 28px;
  border-radius: 14px;
  background:
    linear-gradient(90deg, rgba(219, 234, 254, 0.38) 1px, transparent 1px),
    linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
  background-size: 36px 36px, auto;
  border: 1px solid #dbeafe;
}

.sort-flow-label {
  padding: 8px 14px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 10px 22px rgba(59, 130, 246, 0.12);
}

.sort-array-row {
  position: relative;
  display: flex;
  justify-content: center;
  gap: 14px;
  padding: 24px 12px 18px;
  overflow-x: auto;
  max-width: 100%;
}

.sort-item-wrap {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  animation: sort-slide-in 520ms cubic-bezier(.2,.8,.2,1) both;
  animation-delay: var(--delay);
}

.sort-block {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 900;
  color: #243b7a;
  background: #ffffff;
  border: 2px solid #d9e4ff;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.12);
  transition: all 0.24s ease;
}

.sort-block.active {
  transform: translateY(-18px);
  border-color: #2563eb;
  box-shadow: 0 18px 34px rgba(37, 99, 235, 0.2);
}

.sort-block.swapped {
  background: #fff7ed;
  border-color: #f97316;
  color: #9a3412;
}

.sort-block.sorted {
  background: #dcfce7;
  border-color: #22c55e;
  color: #166534;
}

.sort-block.moving {
  outline: 2px solid rgba(249, 115, 22, 0.18);
}

.sort-index {
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.compare-bridge {
  position: absolute;
  top: 8px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, #2563eb, transparent);
  animation: compare-pulse 900ms ease-in-out both;
}

.sorted-boundary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #166534;
  font-size: 12px;
}

.sorted-boundary span {
  width: 44px;
  height: 8px;
  border-radius: 999px;
  background: #86efac;
}

.sort-stage.compact {
  min-height: 280px;
  gap: 16px;
  padding: 22px 18px;
}

.sort-stage.compact .sort-flow-label {
  padding: 7px 12px;
  font-size: 12px;
}

.sort-stage.compact .sort-array-row {
  gap: 10px;
  padding: 20px 8px 14px;
}

.sort-stage.compact .sort-block {
  width: 54px;
  height: 54px;
  border-radius: 12px;
  font-size: 19px;
}

.sort-stage.compact .sort-block.active {
  transform: translateY(-14px);
}

.sort-stage.embedded {
  width: 100%;
  min-height: 236px;
  box-sizing: border-box;
  gap: 12px;
  padding: 16px 12px;
  overflow: hidden;
}

.sort-stage.embedded .sort-flow-label {
  max-width: 94%;
  padding: 6px 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sort-stage.embedded .sort-array-row {
  width: 100%;
  justify-content: flex-start;
  gap: 8px;
  padding: 18px 4px 12px;
}

.sort-stage.embedded .sort-block {
  width: 48px;
  height: 48px;
  font-size: 17px;
}

.sort-stage.embedded .sort-block.active {
  transform: translateY(-12px);
}

@keyframes sort-slide-in {
  0% {
    transform: translateX(var(--from-x)) scale(.96);
  }
  62% {
    transform: translateX(0) scale(1.04);
  }
  100% {
    transform: translateX(0) scale(1);
  }
}

@keyframes compare-pulse {
  0%, 100% { opacity: .2; transform: scaleX(.75); }
  45% { opacity: 1; transform: scaleX(1); }
}
</style>
