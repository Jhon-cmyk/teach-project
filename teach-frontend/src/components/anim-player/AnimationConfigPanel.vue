<template>
  <section class="tool-config-panel scroll-y anim-config-panel">
    <div class="panel-title">
      <monitor-outlined style="color: #3b82f6; margin-right: 8px;" />
      抽象概念动画课件
    </div>

    <div class="example-strip">
      <button
        v-for="item in examples"
        :key="item.concept"
        type="button"
        class="example-chip"
        @click="$emit('apply-example', item)"
      >
        <thunderbolt-outlined />
        <span>{{ item.concept }}</span>
      </button>
    </div>

    <a-form layout="vertical" :model="form" class="tool-form">
      <a-form-item label="核心概念" required>
        <a-textarea
          v-model:value="form.concept"
          :rows="4"
          placeholder="例如：TCP 三次握手、冒泡排序、栈的深入浅出、链表插入删除、二叉搜索树查找、递归调用"
        />
      </a-form-item>

      <div class="form-row-2">
        <a-form-item label="概念类型">
          <a-select v-model:value="form.conceptType" size="large">
            <a-select-option value="auto">自动识别</a-select-option>
            <a-select-option value="algorithm">算法过程</a-select-option>
            <a-select-option value="protocol">协议时序</a-select-option>
            <a-select-option value="data-structure">数据结构</a-select-option>
            <a-select-option value="concept">通用概念</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="适用对象">
          <a-input
            v-model:value="form.targetGroup"
            size="large"
            placeholder="例如：本科一年级、零基础学生、计算机网络入门班"
          />
        </a-form-item>
      </div>

      <a-form-item label="教学目标">
        <a-input
          v-model:value="form.teachingGoal"
          size="large"
          placeholder="例如：帮助学生理解状态变化、关键步骤和为什么这样变化"
        />
      </a-form-item>

      <a-form-item label="重点强调">
        <a-textarea
          v-model:value="form.emphasis"
          :rows="3"
          placeholder="例如：突出 client/server 双方状态、比较/交换位置、栈顶变化、链表指针断开与重连"
        />
      </a-form-item>

      <a-form-item label="其他要求">
        <a-textarea
          v-model:value="form.extraRequirements"
          :rows="3"
          placeholder="例如：语言更通俗；每一步都要说明当前动作、状态变化和原因；适合课堂手动推演"
        />
      </a-form-item>

      <div class="tool-submit-bar">
        <a-button
          type="primary"
          size="large"
          class="generate-btn plan-btn"
          :loading="isGenerating"
          @click="$emit('generate')"
        >
          {{ isGenerating ? '正在生成动画课件...' : '生成动画课件' }}
        </a-button>

        <a-button
          size="large"
          :disabled="isGenerating || isOptimizing"
          @click="$emit('reset')"
        >
          重置参数
        </a-button>
      </div>
    </a-form>
  </section>
</template>

<script setup lang="ts">
import { MonitorOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import type { AnimConceptType, AnimFormModel } from '@/components/anim-player/core/animTypes'

interface ConceptExample {
  concept: string
  conceptType: AnimConceptType
}

defineProps<{
  form: AnimFormModel
  isGenerating: boolean
  isOptimizing: boolean
}>()

defineEmits<{
  (e: 'generate'): void
  (e: 'reset'): void
  (e: 'apply-example', example: ConceptExample): void
}>()

const examples: ConceptExample[] = [
  { concept: 'TCP 三次握手', conceptType: 'protocol' },
  { concept: '冒泡排序', conceptType: 'algorithm' },
  { concept: '栈的深入浅出', conceptType: 'data-structure' },
  { concept: '队列入队出队', conceptType: 'data-structure' },
  { concept: '链表插入删除', conceptType: 'data-structure' },
  { concept: '二叉搜索树查找', conceptType: 'data-structure' },
  { concept: 'BFS 图遍历', conceptType: 'algorithm' },
  { concept: '递归调用', conceptType: 'algorithm' },
]
</script>

<style scoped>
.anim-config-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.example-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.example-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #dbe6ff;
  border-radius: 5px;
  background: #ffffff;
  color: #3556b1;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}

.example-chip:hover {
  border-color: #93b4ff;
  background: #eff6ff;
}

.tool-submit-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px dashed #e2e8f0;
}

.generate-btn.plan-btn {
  background: linear-gradient(135deg, #3b82f6, #6366f1) !important;
  border: none !important;
  color: white !important;
  font-weight: 700 !important;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.ant-btn) {
  border-radius: 5px !important;
}
</style>
