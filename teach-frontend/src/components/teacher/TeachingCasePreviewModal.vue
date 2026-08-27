<template>
  <a-modal
    v-model:open="visible"
    :title="detail?.title || '案例预览'"
    width="1100px"
    centered
    class="teaching-case-preview-modal teacher-wide-modal"
    :footer="null"
    @cancel="close"
  >
    <a-spin :spinning="loading">
      <div v-if="detail" class="preview-shell">
        <div class="preview-meta">
          <a-tag v-if="detail.courseName" color="blue">{{ detail.courseName }}</a-tag>
          <a-tag v-if="fileTypeLabel" color="geekblue">{{ fileTypeLabel }}</a-tag>
          <span v-if="detail.sourceName">{{ detail.sourceName }}</span>
        </div>

        <iframe
          v-if="isPdf"
          class="document-frame"
          :src="documentPreviewUrl"
          title="PDF preview"
        ></iframe>

        <section v-else class="word-preview">
          <div class="word-preview-head">
            <strong>文档正文预览</strong>
          </div>
          <div v-if="previewHtml" class="preview-html" v-html="previewHtml"></div>
          <pre v-else-if="normalizedPreviewText" class="preview-text">{{ normalizedPreviewText }}</pre>
          <a-empty v-else description="暂未提取到可预览正文" />
        </section>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getTeachingCasePreviewDetail,
  getTeachingCasePreviewUrl,
  type TeachingCasePreviewDetail,
} from '@/api/case'

const visible = ref(false)
const loading = ref(false)
const detail = ref<TeachingCasePreviewDetail | null>(null)

const fileUrl = computed(() => detail.value?.pdfUrl || '')
const lowerFileUrl = computed(() => fileUrl.value.toLowerCase())
const isPdf = computed(() => lowerFileUrl.value.includes('.pdf'))
const isDocx = computed(() => lowerFileUrl.value.includes('.docx'))
const isDoc = computed(() => !isDocx.value && lowerFileUrl.value.includes('.doc'))

const fileTypeLabel = computed(() => {
  if (isPdf.value) return 'PDF'
  if (isDocx.value) return 'Word DOCX'
  if (isDoc.value) return 'Word DOC'
  return '文档'
})

const documentPreviewUrl = computed(() => {
  return detail.value?.id ? getTeachingCasePreviewUrl(detail.value.id) : ''
})

const normalizedPreviewText = computed(() => {
  const text = detail.value?.previewText || detail.value?.summary || ''
  return text.replace(/\r\n/g, '\n').replace(/\n{3,}/g, '\n\n').trim()
})
const previewHtml = computed(() => detail.value?.previewHtml || '')

const open = async (id: number) => {
  if (!id) {
    message.warning('该案例没有可预览文件')
    return
  }
  visible.value = true
  loading.value = true
  detail.value = null
  try {
    detail.value = await getTeachingCasePreviewDetail(id)
  } catch (error: any) {
    message.error(error?.message || '案例预览加载失败')
    visible.value = false
  } finally {
    loading.value = false
  }
}

const close = () => {
  visible.value = false
}

defineExpose({ open })
</script>

<style scoped>
.preview-shell {
  height: 100%;
  min-height: 0;
  max-height: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.preview-meta {
  min-height: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.document-frame {
  width: 100%;
  flex: 1;
  min-height: 0;
  height: auto;
  max-height: none;
  border: 1px solid #dbe5f2;
  border-radius: 8px;
  background: #f8fafc;
}

.word-preview {
  flex: 1;
  min-height: 0;
  height: auto;
  max-height: none;
  border: 1px solid #dbe5f2;
  border-radius: 8px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.word-preview-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  color: #0f172a;
}

.preview-text {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 18px 22px 28px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font: 15px/1.8 "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  color: #1e293b;
  background: #fff;
}

.preview-html {
  flex: 1;
  min-height: 0;
  padding: 18px 22px 28px;
  overflow: auto;
  font: 15px/1.8 "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  color: #1e293b;
  background: #fff;
}

.preview-html :deep(p) {
  margin: 0 0 12px;
}

.preview-html :deep(h3) {
  margin: 18px 0 10px;
  color: #0f172a;
  font-size: 18px;
}

.preview-html :deep(.case-preview-table) {
  width: 100%;
  margin: 14px 0;
  border-collapse: collapse;
  table-layout: fixed;
}

.preview-html :deep(.case-preview-table td) {
  padding: 8px 10px;
  border: 1px solid #cbd5e1;
  vertical-align: top;
  word-break: break-word;
}

.preview-html :deep(.case-preview-figure) {
  margin: 16px 0;
  text-align: center;
}

.preview-html :deep(.case-preview-figure img) {
  max-width: 100%;
  max-height: 420px;
  border-radius: 8px;
  object-fit: contain;
}

.preview-html :deep(.case-preview-figure figcaption) {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

:global(.teacher-wide-modal) {
  max-width: calc(100vw - 48px);
}
</style>
