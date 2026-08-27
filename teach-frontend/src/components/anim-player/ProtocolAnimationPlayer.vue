<template>
  <div class="protocol-stage" :class="{ compact: props.compact, embedded: props.embedded }">
    <div class="protocol-lanes">
      <div class="actor-card" :class="{ changing: stateChanged.client }">
        <div class="actor-icon">C</div>
        <div class="actor-name">{{ actors[0] }}</div>
        <div class="actor-state">{{ currentStep.clientState }}</div>
      </div>

      <div class="protocol-wire">
        <div class="wire-line"></div>
        <div class="wire-arrow" :class="{ reverse: isReverseMessage }">
          {{ isReverseMessage ? '←' : '→' }}
        </div>
        <div
          :key="`${stepIndex}-${currentStep.message}`"
          class="packet"
          :class="[currentStep.messageType || 'request', { reverse: isReverseMessage }]"
        >
          <span class="packet-type">{{ messageTypeLabel }}</span>
          <strong>{{ currentStep.message }}</strong>
        </div>
      </div>

      <div class="actor-card server" :class="{ changing: stateChanged.server }">
        <div class="actor-icon">S</div>
        <div class="actor-name">{{ actors[1] }}</div>
        <div class="actor-state">{{ currentStep.serverState }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProtocolAnimPayload, ProtocolAnimStep } from './core/animTypes.ts'

const props = defineProps<{
  payload: ProtocolAnimPayload
  stepIndex: number
  compact?: boolean
  embedded?: boolean
}>()

const actors = computed<[string, string]>(() => {
  return [props.payload.actors?.[0] || '客户端', props.payload.actors?.[1] || '服务器']
})

const currentStep = computed<ProtocolAnimStep>(() => {
  return (
    props.payload.steps[props.stepIndex] || {
      title: '',
      desc: '',
      from: actors.value[0],
      to: actors.value[1],
      message: '',
      clientState: '',
      serverState: '',
      messageType: 'request',
    }
  )
})

const previousStep = computed<ProtocolAnimStep | null>(() => {
  if (props.stepIndex <= 0) return null
  return props.payload.steps[props.stepIndex - 1] || null
})

const isReverseMessage = computed(() => {
  const motion = currentStep.value.motion
  if (motion?.type === 'send' && motion.from && motion.to) {
    return motion.from === actors.value[1]
  }
  return currentStep.value.from === actors.value[1]
})

const stateChanged = computed(() => {
  const prev = previousStep.value
  if (!prev) return { client: false, server: false }
  return {
    client: prev.clientState !== currentStep.value.clientState,
    server: prev.serverState !== currentStep.value.serverState,
  }
})

const messageTypeLabel = computed(() => {
  const map: Record<string, string> = {
    request: '请求',
    response: '响应',
    confirm: '确认',
    close: '关闭',
  }
  return map[currentStep.value.messageType || 'request'] || '报文'
})
</script>

<style scoped>
.protocol-stage {
  min-height: 330px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px 28px;
  border-radius: 14px;
  background:
    radial-gradient(circle at 50% 36%, rgba(59, 130, 246, 0.12), transparent 34%),
    linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
  border: 1px solid #dbeafe;
}

.protocol-lanes {
  width: 100%;
  display: grid;
  grid-template-columns: 190px minmax(260px, 1fr) 190px;
  align-items: center;
  gap: 22px;
}

.actor-card {
  min-height: 168px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #dbe6ff;
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.12);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 10px;
  transition: all .25s ease;
}

.actor-card.changing {
  animation: state-arrive 780ms ease both;
}

.actor-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 900;
}

.actor-card.server .actor-icon {
  background: #dcfce7;
  color: #166534;
}

.actor-name {
  font-size: 18px;
  font-weight: 900;
  color: #1e3a8a;
}

.actor-state {
  min-height: 34px;
  padding: 8px 14px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 800;
}

.protocol-wire {
  position: relative;
  height: 172px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wire-line {
  width: 100%;
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, #93c5fd, #3b82f6, #93c5fd);
}

.wire-arrow {
  position: absolute;
  top: 51%;
  right: 4px;
  transform: translateY(-50%);
  color: #1d4ed8;
  font-size: 24px;
  font-weight: 900;
}

.wire-arrow.reverse {
  left: 4px;
  right: auto;
}

.packet {
  position: absolute;
  top: 34px;
  left: 0;
  min-width: 170px;
  max-width: 260px;
  padding: 11px 14px;
  border-radius: 14px;
  background: #ffffff;
  border: 2px solid #3b82f6;
  color: #1e3a8a;
  box-shadow: 0 16px 34px rgba(37, 99, 235, 0.22);
  display: flex;
  flex-direction: column;
  gap: 4px;
  animation: packet-send 900ms cubic-bezier(.2,.8,.2,1) both;
}

.packet.reverse {
  left: auto;
  right: 0;
  animation-name: packet-send-reverse;
}

.packet.response {
  border-color: #8b5cf6;
  color: #5b21b6;
}

.packet.confirm {
  border-color: #22c55e;
  color: #166534;
}

.packet.close {
  border-color: #f97316;
  color: #9a3412;
}

.packet-type {
  align-self: flex-start;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 900;
}

.packet strong {
  font-size: 17px;
  line-height: 1.3;
}

.protocol-stage.compact {
  min-height: 280px;
  padding: 22px 18px;
}

.protocol-stage.compact .protocol-lanes {
  grid-template-columns: 150px minmax(190px, 1fr) 150px;
  gap: 12px;
}

.protocol-stage.compact .actor-card {
  min-height: 126px;
  border-radius: 14px;
  gap: 7px;
}

.protocol-stage.compact .actor-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
}

.protocol-stage.compact .actor-name {
  font-size: 15px;
}

.protocol-stage.compact .actor-state {
  min-height: 28px;
  padding: 6px 10px;
  font-size: 12px;
}

.protocol-stage.compact .protocol-wire {
  height: 132px;
}

.protocol-stage.compact .packet {
  top: 20px;
  min-width: 138px;
  max-width: 210px;
  padding: 9px 11px;
  border-radius: 12px;
}

.protocol-stage.compact .packet strong {
  font-size: 14px;
}

.protocol-stage.embedded {
  width: 100%;
  min-height: 250px;
  box-sizing: border-box;
  overflow: hidden;
  padding: 18px 14px;
}

.protocol-stage.embedded .protocol-lanes {
  grid-template-columns: minmax(88px, 0.42fr) minmax(118px, 1fr) minmax(88px, 0.42fr);
  gap: 8px;
  min-width: 0;
}

.protocol-stage.embedded .actor-card {
  min-width: 0;
  min-height: 106px;
  border-radius: 12px;
  gap: 6px;
  padding: 10px 6px;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.1);
}

.protocol-stage.embedded .actor-icon {
  width: 28px;
  height: 28px;
  border-radius: 9px;
}

.protocol-stage.embedded .actor-name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.protocol-stage.embedded .actor-state {
  max-width: 100%;
  min-height: 26px;
  padding: 5px 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
}

.protocol-stage.embedded .protocol-wire {
  min-width: 0;
  height: 116px;
  overflow: hidden;
}

.protocol-stage.embedded .wire-line {
  height: 3px;
}

.protocol-stage.embedded .wire-arrow {
  font-size: 20px;
}

.protocol-stage.embedded .packet {
  top: 14px;
  left: 50%;
  right: auto;
  min-width: 0;
  width: min(168px, 92%);
  max-width: 92%;
  padding: 8px 10px;
  border-radius: 11px;
  text-align: center;
  animation-name: embedded-packet-send;
}

.protocol-stage.embedded .packet.reverse {
  left: 50%;
  right: auto;
  animation-name: embedded-packet-send-reverse;
}

.protocol-stage.embedded .packet-type {
  align-self: center;
  font-size: 10px;
}

.protocol-stage.embedded .packet strong {
  font-size: 13px;
  word-break: break-word;
}

@keyframes packet-send {
  0% { transform: translateX(0) translateY(34px) scale(.92); opacity: 0; }
  18% { opacity: 1; }
  78% { transform: translateX(calc(100% + 120px)) translateY(0) scale(1); opacity: 1; }
  100% { transform: translateX(calc(100% + 160px)) translateY(0) scale(.98); opacity: .16; }
}

@keyframes packet-send-reverse {
  0% { transform: translateX(0) translateY(34px) scale(.92); opacity: 0; }
  18% { opacity: 1; }
  78% { transform: translateX(calc(-100% - 120px)) translateY(0) scale(1); opacity: 1; }
  100% { transform: translateX(calc(-100% - 160px)) translateY(0) scale(.98); opacity: .16; }
}

@keyframes embedded-packet-send {
  0% { transform: translate(-92%, 26px) scale(.92); opacity: 0; }
  18% { opacity: 1; }
  74% { transform: translate(-50%, 0) scale(1); opacity: 1; }
  100% { transform: translate(-8%, 0) scale(.96); opacity: .22; }
}

@keyframes embedded-packet-send-reverse {
  0% { transform: translate(-8%, 26px) scale(.92); opacity: 0; }
  18% { opacity: 1; }
  74% { transform: translate(-50%, 0) scale(1); opacity: 1; }
  100% { transform: translate(-92%, 0) scale(.96); opacity: .22; }
}

@keyframes state-arrive {
  0%, 62% { transform: translateY(0); }
  76% { transform: translateY(-6px); border-color: #60a5fa; }
  100% { transform: translateY(0); }
}

@media (max-width: 640px) {
  .protocol-lanes {
    grid-template-columns: 1fr;
  }

  .protocol-wire {
    height: 120px;
  }
}
</style>
