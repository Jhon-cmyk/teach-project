<template>
  <div class="rich-editor-shell">
    <div class="rich-editor-surface">
      <Toolbar
        style="border-bottom: 1px solid #E2E8F0"
        :editor="editorRef"
        :defaultConfig="toolbarConfig"
        mode="default"
      />
      <Editor
        :style="editorStyle"
        v-model="content"
        :defaultConfig="editorConfig"
        mode="default"
        @onCreated="handleCreated"
        @onDestroyed="handleDestroyed"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
// @ts-ignore
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  height?: string
  toolbarKeys?: string[]
}>(), {
  modelValue: '',
  placeholder: '请输入内容',
  height: '200px',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const escapeHtml = (value: string) => value
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')

const escapeAttribute = (value: string) => escapeHtml(value).replace(/"/g, '&quot;')

const sanitizeEditorHtml = (value?: string) => {
  const html = (value || '').trim()
  if (!html || typeof window === 'undefined' || typeof DOMParser === 'undefined') {
    return html
  }

  const doc = new DOMParser().parseFromString(html, 'text/html')

  doc.querySelectorAll('pre').forEach((pre) => {
    const code = pre.querySelector('code')
    const text = (code?.textContent || pre.textContent || '').replace(/[\u200B-\u200D\uFEFF]/g, '')
    if (!text.trim()) {
      pre.remove()
      return
    }

    const className = code?.getAttribute('class') || ''
    const safeClass = className
      .split(/\s+/)
      .filter((item) => /^language-[\w-]+$/.test(item))
      .join(' ')

    pre.innerHTML = `<code${safeClass ? ` class="${escapeAttribute(safeClass)}"` : ''}>${escapeHtml(text)}</code>`
  })

  doc.querySelectorAll('code').forEach((code) => {
    if (code.parentElement?.tagName.toLowerCase() === 'pre') return

    const text = (code.textContent || '').replace(/[\u200B-\u200D\uFEFF]/g, '')
    if (!text.trim()) {
      code.remove()
      return
    }

    code.innerHTML = escapeHtml(text)
  })

  doc.querySelectorAll('span').forEach((span) => {
    if (!span.attributes.length && !span.textContent?.trim() && !span.children.length) {
      span.remove()
    }
  })

  return doc.body.innerHTML
}

const editorRef = shallowRef()
const content = ref(sanitizeEditorHtml(props.modelValue))
let syncingFromProps = false

const defaultToolbarKeys = [
  'headerSelect',
  'bold',
  'italic',
  'underline',
  'through',
  'divider',
  'color',
  'bgColor',
  'fontSize',
  'divider',
  'indent',
  'lineHeight',
  'justifyLeft',
  'justifyCenter',
  'justifyRight',
  'divider',
  'numberedList',
  'bulletedList',
  'blockquote',
  'divider',
  'insertLink',
]

const toolbarConfig = computed(() => ({
  toolbarKeys: props.toolbarKeys?.length ? props.toolbarKeys : defaultToolbarKeys,
}))

const editorConfig = computed(() => ({
  placeholder: props.placeholder,
}))

const editorStyle = computed(() => ({
  height: props.height,
  overflowY: 'hidden',
}))

watch(() => props.modelValue, (newVal) => {
  const nextContent = sanitizeEditorHtml(newVal)
  if (content.value !== nextContent) {
    syncingFromProps = true
    content.value = nextContent
    queueMicrotask(() => {
      syncingFromProps = false
    })
  }
})

watch(content, (newVal) => {
  if (syncingFromProps) return
  emit('update:modelValue', sanitizeEditorHtml(newVal))
})

const handleCreated = (editor: any) => {
  editorRef.value = editor
}

const handleDestroyed = () => {
  editorRef.value = null
}

onBeforeUnmount(() => {
  editorRef.value?.destroy?.()
})
</script>

<style scoped>
.rich-editor-shell {
  border: 1px solid #E7ECF3;
  border-radius: 5px;
  overflow: hidden;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.rich-editor-shell:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

.rich-editor-surface {
  background: #FFFFFF;
}
</style>
