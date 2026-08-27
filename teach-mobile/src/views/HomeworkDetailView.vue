<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchHomeworkDetail, submitHomework, uploadHomeworkImage } from '@/api/student'
import { isImagePickCanceled, pickHomeworkImages, type HomeworkImageSource } from '@/utils/mobileImage'
import type { HomeworkDetail } from '@/types/student'

const route = useRoute()
const router = useRouter()
const assignmentId = computed(() => String(route.params.id || ''))

const detail = ref<HomeworkDetail | null>(null)
const answerText = ref('')
const imageUrls = ref<string[]>([])
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const error = ref('')
const success = ref('')

const canEdit = computed(() => {
  return !detail.value?.completed || Boolean(detail.value?.allowRedo)
})

const canSubmit = computed(() => {
  if (!detail.value) return false
  if (!canEdit.value) return false
  return answerText.value.trim().length > 0 || imageUrls.value.length > 0
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await fetchHomeworkDetail(assignmentId.value)
  } catch (err: any) {
    error.value = err?.message || '作业详情加载失败'
  } finally {
    loading.value = false
  }
}

async function uploadFiles(files: File[]) {
  if (!files.length) return
  uploading.value = true
  error.value = ''
  success.value = ''
  try {
    const uploaded: string[] = []
    for (const file of files) {
      uploaded.push(await uploadHomeworkImage(file))
    }
    imageUrls.value.push(...uploaded)
    success.value = `已添加 ${uploaded.length} 张作答图片`
  } catch (err: any) {
    error.value = err?.message || '图片上传失败'
  } finally {
    uploading.value = false
  }
}

async function handleImagePick(source: HomeworkImageSource) {
  if (!canEdit.value || uploading.value) return
  try {
    const files = await pickHomeworkImages(source)
    await uploadFiles(files)
  } catch (err: any) {
    if (!isImagePickCanceled(err)) {
      error.value = err?.message || '图片选择失败'
    }
  }
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  await uploadFiles(files)
  input.value = ''
}

function removeImage(index: number) {
  imageUrls.value.splice(index, 1)
}

async function submit() {
  if (!detail.value || !canSubmit.value) return
  submitting.value = true
  error.value = ''
  success.value = ''
  const submissionType = imageUrls.value.length && answerText.value.trim() ? 'mixed' : imageUrls.value.length ? 'image' : 'online'

  try {
    const submissionId = await submitHomework({
      assignmentId: detail.value.assignmentId,
      submissionType,
      studentAnswerJson: JSON.stringify([
        {
          num: '1',
          type: 'text',
          stem: '移动端作答',
          answer: answerText.value.trim()
        }
      ]),
      wholePaperImageUrls: imageUrls.value,
      questionImageItems: []
    })
    success.value = `提交成功，记录号 ${submissionId}`
    await load()
  } catch (err: any) {
    error.value = err?.message || '提交失败'
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <main class="page">
    <header class="detail-head">
      <button class="ghost-link" type="button" @click="router.push('/homework')">‹ 返回作业</button>
      <h1>{{ detail?.title || '作业详情' }}</h1>
      <p v-if="detail?.teacherNote">{{ detail.teacherNote }}</p>
    </header>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="success" class="success-text">{{ success }}</p>

    <section v-if="detail" class="meta-panel panel">
      <div>
        <span>题目</span>
        <strong>{{ detail.questionCount || 0 }}</strong>
      </div>
      <div>
        <span>尝试</span>
        <strong>{{ detail.attemptCount || 0 }}</strong>
      </div>
      <div>
        <span>总分</span>
        <strong>{{ detail.totalScore || '-' }}</strong>
      </div>
    </section>

    <section class="section-title">
      <h2>题目内容</h2>
      <span>{{ loading ? '加载中' : detail?.answerMode || 'online' }}</span>
    </section>
    <article class="paper panel">
      <pre v-if="detail?.contentSnapshot">{{ detail.contentSnapshot }}</pre>
      <div v-else class="empty-state">暂无题目正文。</div>
    </article>

    <section class="section-title">
      <h2>我的作答</h2>
      <span>文字或图片</span>
    </section>
    <section class="answer-panel panel">
      <textarea
        v-model="answerText"
        :disabled="!canEdit"
        placeholder="在这里输入你的解题过程、答案或补充说明。"
      ></textarea>

      <div class="upload-actions">
        <button class="upload-button" type="button" :disabled="uploading || !canEdit" @click="handleImagePick('camera')">
          拍照上传
        </button>
        <button class="upload-button" type="button" :disabled="uploading || !canEdit" @click="handleImagePick('photos')">
          相册选择
        </button>
        <label class="upload-button file-upload" :class="{ disabled: uploading || !canEdit }">
          {{ uploading ? '上传中' : '文件选择' }}
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            multiple
            :disabled="uploading || !canEdit"
            @change="handleFileChange"
          />
        </label>
      </div>

      <div v-if="imageUrls.length" class="image-list">
        <div v-for="(url, index) in imageUrls" :key="url" class="image-chip">
          <a :href="url" target="_blank">图片 {{ index + 1 }}</a>
          <button type="button" @click="removeImage(index)">×</button>
        </div>
      </div>

      <button class="primary-button submit-button" :disabled="!canSubmit || submitting" type="button" @click="submit">
        {{ submitting ? '提交中' : detail?.completed && !detail?.allowRedo ? '已完成' : '提交作业' }}
      </button>
    </section>
  </main>
</template>

<style scoped>
.detail-head h1 {
  margin: 14px 0 8px;
  font-size: 28px;
  line-height: 1.2;
}

.detail-head p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
  font-size: 14px;
}

.error-text {
  color: #af4a31;
  font-size: 13px;
}

.success-text {
  color: var(--green-deep);
  font-size: 13px;
  font-weight: 800;
}

.meta-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 18px;
  padding: 16px;
}

.meta-panel span {
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
}

.meta-panel strong {
  display: block;
  margin-top: 6px;
  color: var(--green-deep);
  font-size: 26px;
}

.paper {
  max-height: 300px;
  overflow: auto;
  padding: 16px;
}

.paper pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--ink);
  font: inherit;
  line-height: 1.75;
}

.answer-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
}

textarea {
  width: 100%;
  min-height: 150px;
  resize: vertical;
  border: 1px solid rgba(31, 42, 46, 0.14);
  border-radius: 8px;
  padding: 12px;
  color: var(--ink);
  background: #fff;
  outline: none;
}

textarea:focus {
  border-color: rgba(31, 122, 91, 0.65);
  box-shadow: 0 0 0 3px rgba(31, 122, 91, 0.12);
}

.upload-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.upload-button {
  position: relative;
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border: 1px dashed rgba(31, 122, 91, 0.35);
  border-radius: 8px;
  color: var(--green-deep);
  background: rgba(31, 122, 91, 0.06);
  text-align: center;
  font-weight: 900;
  overflow: hidden;
}

.upload-button:disabled,
.upload-button.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.file-upload input {
  position: absolute;
  inset: 0;
  opacity: 0;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid rgba(45, 95, 139, 0.18);
  border-radius: 8px;
  background: #fff;
  font-size: 12px;
}

.image-chip a {
  color: var(--blue);
  font-weight: 900;
}

.image-chip button {
  color: var(--muted);
  background: transparent;
  font-size: 18px;
  line-height: 1;
}

.submit-button {
  width: 100%;
}

.submit-button:disabled {
  opacity: 0.55;
}
</style>
