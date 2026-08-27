<template>
  <div class="stack-stage" :class="{ compact: props.compact, embedded: props.embedded }">
    <div class="stack-animation-zone">
      <div
        v-if="currentStep.operation === 'push' && currentStep.activeValue !== null && currentStep.activeValue !== undefined"
        :key="`push-${stepIndex}-${currentStep.activeValue}`"
        class="flying-token push-token"
      >
        {{ currentStep.activeValue }}
      </div>
      <div
        v-if="currentStep.operation === 'pop' && currentStep.poppedValue !== null && currentStep.poppedValue !== undefined"
        :key="`pop-${stepIndex}-${currentStep.poppedValue}`"
        class="flying-token pop-token"
      >
        {{ currentStep.poppedValue }}
      </div>

      <div class="stack-top-badge">只看栈顶</div>
      <div class="stack-column">
        <div
          v-for="(item, index) in currentStackDisplay"
          :key="`${String(item)}-${index}-${stepIndex}`"
          class="stack-item"
          :class="{
            active: index === 0,
            entering: currentStep.operation === 'push' && index === 0,
            peeking: currentStep.operation === 'peek' && index === 0,
          }"
        >
          {{ item }}
        </div>
        <div v-if="!currentStackDisplay.length" class="stack-empty">空栈</div>
      </div>
    </div>

    <div class="stack-operation-board">
      <div class="stack-op-chip" :class="currentStep.operation">
        {{ operationLabel }}
      </div>
      <div class="stack-rule-line">
        {{ flowHint }}
      </div>
      <div class="stack-mini-state">
        <span>栈底</span>
        <strong>{{ currentStep.stack.length ? currentStep.stack.join(' → ') : '空' }}</strong>
        <span>栈顶: {{ stackTop }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StackAnimPayload, StackAnimStep } from './core/animTypes.ts'

const props = defineProps<{
  payload: StackAnimPayload
  stepIndex: number
  compact?: boolean
  embedded?: boolean
}>()

const currentStep = computed<StackAnimStep>(() => {
  return (
    props.payload.steps[props.stepIndex] || {
      title: '',
      desc: '',
      stack: props.payload.initialStack || [],
      operation: 'init',
      activeValue: null,
      poppedValue: null,
    }
  )
})

const currentStackDisplay = computed(() => [...currentStep.value.stack].reverse())

const stackTop = computed(() => {
  const stack = currentStep.value.stack
  return stack.length ? stack[stack.length - 1] : '空'
})

const operationLabel = computed(() => {
  const map: Record<StackAnimStep['operation'], string> = {
    init: '初始化',
    push: '入栈 PUSH',
    pop: '出栈 POP',
    peek: '查看栈顶 PEEK',
    done: '完成',
  }
  return map[currentStep.value.operation] || currentStep.value.operation.toUpperCase()
})

const flowHint = computed(() => {
  if (currentStep.value.stageCaption) return currentStep.value.stageCaption
  if (currentStep.value.operation === 'push') return `${currentStep.value.activeValue ?? ''} 从外部飞入栈顶`
  if (currentStep.value.operation === 'pop') return `${currentStep.value.poppedValue ?? ''} 从栈顶弹出`
  if (currentStep.value.operation === 'peek') return '只观察栈顶,不移动元素'
  if (currentStep.value.operation === 'done') return '所有操作都只围绕栈顶发生'
  return '先看到一个只能从顶部操作的容器'
})
</script>

<style scoped>
.stack-stage {
  min-height: 330px;
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 260px;
  align-items: center;
  gap: 30px;
  padding: 30px 28px;
  border-radius: 14px;
  background:
    linear-gradient(90deg, rgba(219, 234, 254, 0.38) 1px, transparent 1px),
    linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
  background-size: 36px 36px, auto;
  border: 1px solid #dbeafe;
}

.stack-animation-zone {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 286px;
}

.stack-top-badge {
  margin-bottom: 12px;
  padding: 6px 12px;
  border-radius: 999px;
  background: #0f172a;
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
}

.stack-column {
  width: 184px;
  min-height: 226px;
  border-radius: 18px 18px 28px 28px;
  border: 3px solid #93c5fd;
  border-top: 3px dashed #2563eb;
  background: linear-gradient(180deg, #ffffff 0%, #eff6ff 100%);
  box-shadow: inset 0 -12px 24px rgba(37, 99, 235, 0.08);
  padding: 14px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 10px;
}

.stack-item {
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border: 2px solid #bfdbfe;
  color: #1e3a8a;
  font-size: 20px;
  font-weight: 900;
  transition: all 0.24s ease;
}

.stack-item.active {
  border-color: #2563eb;
  box-shadow: 0 12px 22px rgba(37, 99, 235, 0.16);
}

.stack-item.entering {
  animation: stack-land 620ms cubic-bezier(.2,.8,.2,1) both;
}

.stack-item.peeking {
  animation: peek-pulse 900ms ease both;
  background: #f5f3ff;
  border-color: #8b5cf6;
  color: #5b21b6;
}

.stack-empty {
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8ea0cc;
  background: rgba(255, 255, 255, 0.75);
  border: 1px dashed #93c5fd;
  font-weight: 800;
}

.flying-token {
  position: absolute;
  width: 54px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: #fff7ed;
  border: 2px solid #f97316;
  color: #9a3412;
  font-weight: 900;
  z-index: 4;
  box-shadow: 0 14px 28px rgba(249, 115, 22, 0.2);
}

.push-token {
  top: 8px;
  right: 18%;
  animation: push-token 760ms cubic-bezier(.2,.8,.2,1) both;
}

.pop-token {
  top: 92px;
  left: 50%;
  animation: pop-token 820ms cubic-bezier(.2,.8,.2,1) both;
}

.stack-operation-board {
  padding: 18px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #dbeafe;
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.1);
}

.stack-op-chip {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  font-weight: 900;
  margin-bottom: 14px;
  background: #eff6ff;
  color: #1d4ed8;
}

.stack-op-chip.push { background: #dcfce7; color: #166534; }
.stack-op-chip.pop { background: #fff7ed; color: #9a3412; }
.stack-op-chip.peek { background: #f5f3ff; color: #5b21b6; }
.stack-op-chip.done { background: #f1f5f9; color: #475569; }

.stack-rule-line {
  color: #1e293b;
  font-size: 15px;
  line-height: 1.5;
  font-weight: 800;
  margin-bottom: 16px;
}

.stack-mini-state {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
}

.stack-mini-state strong {
  color: #1e3a8a;
  font-size: 14px;
  word-break: break-word;
}

.stack-stage.compact {
  min-height: 280px;
  grid-template-columns: minmax(190px, 1fr) 230px;
  gap: 18px;
  padding: 22px 20px;
}

.stack-stage.compact .stack-animation-zone {
  min-height: 238px;
}

.stack-stage.compact .stack-column {
  width: 150px;
  min-height: 190px;
  padding: 12px;
  gap: 8px;
}

.stack-stage.compact .stack-top-badge {
  margin-bottom: 8px;
}

.stack-stage.compact .stack-item,
.stack-stage.compact .stack-empty {
  height: 38px;
  border-radius: 10px;
  font-size: 17px;
}

.stack-stage.compact .stack-operation-board {
  padding: 14px;
}

.stack-stage.compact .stack-rule-line {
  font-size: 13px;
  margin-bottom: 12px;
}

.stack-stage.compact .flying-token {
  width: 46px;
  height: 36px;
}

.stack-stage.embedded {
  width: 100%;
  min-height: 252px;
  box-sizing: border-box;
  grid-template-columns: 1fr;
  gap: 10px;
  padding: 16px 14px;
  overflow: hidden;
}

.stack-stage.embedded .stack-animation-zone {
  min-height: 188px;
}

.stack-stage.embedded .stack-top-badge {
  margin-bottom: 7px;
  padding: 5px 10px;
  font-size: 11px;
}

.stack-stage.embedded .stack-column {
  width: min(148px, 60%);
  min-height: 156px;
  padding: 10px;
  gap: 7px;
}

.stack-stage.embedded .stack-item,
.stack-stage.embedded .stack-empty {
  height: 34px;
  border-radius: 9px;
  font-size: 16px;
}

.stack-stage.embedded .stack-operation-board {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 8px 10px;
  padding: 10px 12px;
  border-radius: 12px;
}

.stack-stage.embedded .stack-op-chip {
  margin-bottom: 0;
  padding: 6px 10px;
  font-size: 12px;
}

.stack-stage.embedded .stack-rule-line {
  margin-bottom: 0;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.stack-stage.embedded .stack-mini-state {
  display: none;
}

.stack-stage.embedded .flying-token {
  width: 42px;
  height: 34px;
}

.stack-stage.embedded .push-token {
  right: 28%;
  animation-name: embedded-push-token;
}

.stack-stage.embedded .pop-token {
  animation-name: embedded-pop-token;
}

@keyframes push-token {
  0% { transform: translate(90px, -28px) scale(.9); opacity: 0; }
  30% { opacity: 1; }
  78% { transform: translate(-52px, 82px) scale(1); opacity: 1; }
  100% { transform: translate(-52px, 92px) scale(.9); opacity: 0; }
}

@keyframes pop-token {
  0% { transform: translate(-50%, 78px) scale(.95); opacity: 0; }
  25% { opacity: 1; }
  100% { transform: translate(120px, -44px) scale(1); opacity: 1; }
}

@keyframes embedded-push-token {
  0% { transform: translate(44px, -20px) scale(.9); opacity: 0; }
  30% { opacity: 1; }
  78% { transform: translate(-34px, 66px) scale(1); opacity: 1; }
  100% { transform: translate(-34px, 76px) scale(.9); opacity: 0; }
}

@keyframes embedded-pop-token {
  0% { transform: translate(-50%, 56px) scale(.95); opacity: 0; }
  25% { opacity: 1; }
  100% { transform: translate(58px, -34px) scale(1); opacity: 1; }
}

@keyframes stack-land {
  0% { transform: translateY(-46px) scale(.94); opacity: .3; }
  70% { transform: translateY(4px) scale(1.03); opacity: 1; }
  100% { transform: translateY(0) scale(1); }
}

@keyframes peek-pulse {
  0%, 100% { transform: translateY(0); }
  40% { transform: translateY(-6px); }
}

@media (max-width: 640px) {
  .stack-stage {
    grid-template-columns: 1fr;
  }
}
</style>
