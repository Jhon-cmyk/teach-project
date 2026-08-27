<template>
  <div class="queue-stage" :class="{ compact: props.compact, embedded: props.embedded }">
    <div class="queue-board">
      <div
        v-if="currentStep.operation === 'enqueue' && currentStep.activeValue !== null && currentStep.activeValue !== undefined"
        :key="`enqueue-${stepIndex}-${currentStep.activeValue}`"
        class="queue-token enqueue-token"
      >
        {{ currentStep.activeValue }}
      </div>
      <div
        v-if="currentStep.operation === 'dequeue' && currentStep.removedValue !== null && currentStep.removedValue !== undefined"
        :key="`dequeue-${stepIndex}-${currentStep.removedValue}`"
        class="queue-token dequeue-token"
      >
        {{ currentStep.removedValue }}
      </div>

      <div class="queue-labels">
        <span>队头 FRONT</span>
        <span>队尾 REAR</span>
      </div>

      <div class="queue-lane">
        <div v-if="!currentStep.queue.length" class="queue-empty">空队列</div>
        <div
          v-for="(item, index) in currentStep.queue"
          :key="`${String(item)}-${index}-${stepIndex}`"
          class="queue-item"
          :class="{
            front: index === 0,
            rear: index === currentStep.queue.length - 1,
            entering: currentStep.operation === 'enqueue' && index === currentStep.queue.length - 1,
            leaving: currentStep.operation === 'dequeue' && index === 0,
            peeking: currentStep.operation === 'peek' && index === 0,
          }"
        >
          {{ item }}
        </div>
      </div>
    </div>

    <div class="queue-side">
      <div class="queue-op-chip" :class="currentStep.operation">{{ operationLabel }}</div>
      <div class="queue-rule">{{ flowHint }}</div>
      <div class="queue-state">
        <span>当前队列</span>
        <strong>{{ currentStep.queue.length ? currentStep.queue.join(' → ') : '空' }}</strong>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { QueueAnimPayload, QueueAnimStep } from './core/animTypes.ts'

const props = defineProps<{
  payload: QueueAnimPayload
  stepIndex: number
  compact?: boolean
  embedded?: boolean
}>()

const currentStep = computed<QueueAnimStep>(() => {
  return (
    props.payload.steps[props.stepIndex] || {
      title: '',
      desc: '',
      queue: props.payload.initialQueue || [],
      operation: 'init',
      activeValue: null,
      removedValue: null,
    }
  )
})

const operationLabel = computed(() => {
  const map: Record<QueueAnimStep['operation'], string> = {
    init: '初始化',
    enqueue: '入队 ENQUEUE',
    dequeue: '出队 DEQUEUE',
    peek: '查看队头 PEEK',
    done: '完成',
  }
  return map[currentStep.value.operation] || '队列操作'
})

const flowHint = computed(() => {
  if (currentStep.value.stageCaption) return currentStep.value.stageCaption
  if (currentStep.value.operation === 'enqueue') return `${currentStep.value.activeValue ?? ''} 从队尾进入`
  if (currentStep.value.operation === 'dequeue') return `${currentStep.value.removedValue ?? ''} 从队头离开`
  if (currentStep.value.operation === 'peek') return '只观察队头元素,不改变队列'
  if (currentStep.value.operation === 'done') return '先进来的元素会先离开'
  return '队列从队尾进入,从队头离开'
})
</script>

<style scoped>
.queue-stage {
  min-height: 330px;
  display: grid;
  grid-template-columns: minmax(320px, 1fr) 260px;
  align-items: center;
  gap: 28px;
  padding: 30px 28px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background:
    linear-gradient(90deg, rgba(219, 234, 254, 0.38) 1px, transparent 1px),
    linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
  background-size: 36px 36px, auto;
}

.queue-board {
  position: relative;
  min-height: 230px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
}

.queue-labels {
  display: flex;
  justify-content: space-between;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 900;
}

.queue-lane {
  min-height: 88px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border: 3px solid #93c5fd;
  border-left-style: dashed;
  border-right-style: dashed;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: inset 0 -12px 24px rgba(37, 99, 235, 0.08);
  overflow-x: auto;
}

.queue-item,
.queue-token {
  min-width: 58px;
  height: 52px;
  display: grid;
  place-items: center;
  border: 2px solid #bfdbfe;
  border-radius: 14px;
  background: #ffffff;
  color: #1e3a8a;
  font-size: 20px;
  font-weight: 900;
  box-shadow: 0 12px 22px rgba(37, 99, 235, 0.12);
}

.queue-item.front {
  border-color: #2563eb;
}

.queue-item.rear {
  border-color: #10b981;
}

.queue-item.entering {
  animation: queue-enter 620ms cubic-bezier(.2,.8,.2,1) both;
}

.queue-item.leaving {
  animation: queue-leave 680ms cubic-bezier(.2,.8,.2,1) both;
}

.queue-item.peeking {
  background: #f5f3ff;
  border-color: #8b5cf6;
  color: #5b21b6;
  animation: queue-peek 900ms ease both;
}

.queue-empty {
  width: 100%;
  text-align: center;
  color: #8ea0cc;
  font-weight: 800;
}

.queue-token {
  position: absolute;
  z-index: 4;
  background: #fff7ed;
  border-color: #f97316;
  color: #9a3412;
}

.enqueue-token {
  top: 10px;
  right: 8px;
  animation: queue-token-in 760ms cubic-bezier(.2,.8,.2,1) both;
}

.dequeue-token {
  top: 118px;
  left: 6px;
  animation: queue-token-out 760ms cubic-bezier(.2,.8,.2,1) both;
}

.queue-side {
  min-height: 188px;
  padding: 18px;
  border: 1px solid #dbe6ff;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.1);
}

.queue-op-chip {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 900;
}

.queue-op-chip.dequeue {
  background: #fff7ed;
  color: #c2410c;
}

.queue-op-chip.done {
  background: #dcfce7;
  color: #166534;
}

.queue-rule {
  margin-top: 16px;
  color: #334155;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.6;
}

.queue-state {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.queue-state strong {
  color: #1e3a8a;
  font-size: 15px;
}

.queue-stage.compact,
.queue-stage.embedded {
  grid-template-columns: 1fr;
  gap: 16px;
  padding: 20px;
}

@keyframes queue-enter {
  from { transform: translateX(42px) scale(.9); opacity: .35; }
  to { transform: translateX(0) scale(1); opacity: 1; }
}

@keyframes queue-leave {
  from { transform: translateX(0); opacity: 1; }
  to { transform: translateX(-34px); opacity: .3; }
}

@keyframes queue-peek {
  0%, 100% { transform: translateY(0); }
  45% { transform: translateY(-10px); }
}

@keyframes queue-token-in {
  from { transform: translate(38px, -24px) scale(.86); opacity: 0; }
  to { transform: translate(0, 92px) scale(1); opacity: 1; }
}

@keyframes queue-token-out {
  from { transform: translate(0, 0) scale(1); opacity: 1; }
  to { transform: translate(-48px, 0) scale(.9); opacity: 0; }
}
</style>
