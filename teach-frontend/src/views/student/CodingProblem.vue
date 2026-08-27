<template>
  <div class="coding-problem-page">
    <!-- 顶部精简工具栏 -->
    <header class="page-header">
      <div class="header-actions header-left">
        <a-button class="header-btn" @click="goBack">
          <arrow-left-outlined /> 返回题库
        </a-button>
      </div>

      <div class="header-actions">
        <a-button v-if="lastSubmission" class="header-btn" @click="restoreLastCode">
          <undo-outlined /> 恢复上次代码
        </a-button>
        <a-button class="header-btn" @click="openHistory">
          <history-outlined /> 作答记录
        </a-button>
      </div>
    </header>

    <!-- 三栏式主内容 -->
    <div class="main-grid" v-if="problem">
      <!-- ========== 左栏：题目信息 ========== -->
      <aside class="panel problem-panel">
        <div class="panel-head">
          <span class="panel-icon">
            <file-text-outlined />
          </span>
          <h2 class="panel-title">{{ problem.title || '编程练习' }}</h2>
        </div>

        <div class="panel-scroll">
          <div class="info-section">
            <div class="info-row">
              <span class="info-label">难度标准:</span>
              <a-tag :color="difficultyColor(problem.difficulty)" class="diff-tag">
                {{ difficultyLabel(problem.difficulty) }}
              </a-tag>
            </div>

            <div v-if="problem.timeLimitMs || problem.memoryLimitKb" class="info-block">
              <div class="info-label">时间 / 内存限制</div>
              <div class="info-values">
                <span v-if="problem.timeLimitMs" class="info-chip">
                  <clock-circle-outlined /> {{ problem.timeLimitMs }}ms
                </span>
                <span v-if="problem.memoryLimitKb" class="info-chip">
                  <database-outlined /> {{ Math.round(problem.memoryLimitKb / 1024) }}MB
                </span>
              </div>
            </div>

            <div v-if="deadlineText" class="info-block">
              <div class="info-label">截止时间</div>
              <div class="info-values">
                <span class="info-chip" :class="{ 'deadline-warn': isDeadlineSoon }">
                  <clock-circle-outlined /> {{ deadlineText }}
                </span>
              </div>
            </div>
          </div>

          <!-- tab 切换：任务要求 / 样例 -->
          <div class="info-tabs">
            <div
              class="info-tab"
              :class="{ active: leftTab === 'desc' }"
              @click="leftTab = 'desc'"
            >任务要求</div>
            <div
              class="info-tab"
              :class="{ active: leftTab === 'samples' }"
              @click="leftTab = 'samples'"
            >样例</div>
          </div>

          <div class="tab-body">
            <div v-show="leftTab === 'desc'" class="problem-content md-body" v-html="renderedDescription"></div>
            <div v-show="leftTab === 'samples'" class="sample-panel">
              <div v-if="sampleCases.length === 0" class="empty-hint">
                <file-text-outlined style="font-size: 32px; color: #cbd5e1;" />
                <p>题目暂未提供样例</p>
              </div>
              <div v-for="(tc, idx) in sampleCases" :key="idx" class="sample-case">
                <div class="sample-label">样例 {{ idx + 1 }}</div>
                <div class="sample-block">
                  <div class="sample-key">输入</div>
                  <pre>{{ tc.input || '(无输入)' }}</pre>
                </div>
                <div class="sample-block">
                  <div class="sample-key">输出</div>
                  <pre>{{ tc.expectedOutput }}</pre>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- ========== 中栏：代码编辑 ========== -->
      <section class="panel editor-panel">
        <div class="panel-head editor-head">
          <div class="panel-head-left">
            <span class="panel-icon">
              <file-text-outlined />
            </span>
            <h2 class="panel-title">代码编辑</h2>
          </div>
          <div class="editor-head-actions">
            <button class="action-btn" @click="handleRun" :disabled="running">
              <caret-right-outlined /> {{ running ? '运行中' : '自测运行' }}
            </button>
            <button class="action-btn primary" @click="handleSubmit" :disabled="submitting || isDeadlinePassed">
              <cloud-upload-outlined /> {{ submitting ? '提交中' : '保存代码' }}
            </button>
          </div>
        </div>

        <div class="editor-wrap">
          <div class="editor-sub-toolbar">
            <a-select v-model:value="currentLanguage" style="width: 120px" @change="onLanguageChange" size="small">
              <a-select-option v-for="lang in (problem.languages || [])" :key="lang" :value="lang">
                {{ lang }}
              </a-select-option>
            </a-select>
            <div class="editor-sub-right">
              <a-tooltip title="重置为初始模板">
                <button class="icon-btn" @click="resetTemplate"><reload-outlined /></button>
              </a-tooltip>
              <a-tooltip title="清空代码">
                <button class="icon-btn" @click="clearCode"><clear-outlined /></button>
              </a-tooltip>
            </div>
          </div>
          <div class="editor-wrapper" ref="editorContainerRef"></div>
          <div class="editor-status">
            <span class="lang-label">{{ currentLanguage }}</span>
            <span>{{ code.length }} 字符 / {{ lineCount }} 行</span>
          </div>
        </div>

        <!-- 运行结果（自测运行） -->
        <div v-if="runResult" class="inline-result">
          <div class="inline-result-head">
            <span class="inline-result-title">
              <caret-right-outlined /> 运行结果
            </span>
            <span v-if="runResult.status && runResult.status !== 'accepted'" class="result-status fail">
              {{ runResult.statusDescription || runResult.status }}
            </span>
            <span v-else-if="runResult.testCaseResults && runResult.testCaseResults.every((t: any) => t.passed)" class="result-status pass">
              全部通过
            </span>
          </div>
          <div class="inline-result-body">
            <div v-if="runResult.testCaseResults && runResult.testCaseResults.length > 0">
              <div v-for="(tc, idx) in runResult.testCaseResults" :key="idx" class="case-item">
                <div class="case-verdict" :class="tc.passed ? 'pass' : 'fail'">
                  <check-circle-filled v-if="tc.passed" />
                  <close-circle-filled v-else />
                  样例 {{ idx + 1 }}: {{ tc.passed ? '通过' : '未通过' }}
                </div>
                <div v-if="!tc.passed" class="case-diff">
                  <div v-if="caseDiagnostic(tc)" class="case-error">
                    <span class="diff-label">错误信息</span>
                    <pre class="diff-output bad">{{ caseDiagnostic(tc) }}</pre>
                  </div>
                  <div class="diff-col">
                    <span class="diff-label">你的输出</span>
                    <pre class="diff-output bad">{{ tc.actualOutput || '(空)' }}</pre>
                  </div>
                  <div class="diff-col">
                    <span class="diff-label">期望输出</span>
                    <pre class="diff-output">{{ tc.expectedOutput || '(空)' }}</pre>
                  </div>
                </div>
              </div>
            </div>
            <div v-else>
              <div v-if="runResult.compileOutput" class="error-block">
                <span class="diff-label">编译输出</span>
                <pre class="diff-output bad">{{ runResult.compileOutput }}</pre>
              </div>
              <div v-if="runResult.stderr" class="error-block">
                <span class="diff-label">错误输出</span>
                <pre class="diff-output bad">{{ runResult.stderr }}</pre>
              </div>
              <div v-if="runResult.stdout" class="error-block">
                <span class="diff-label">标准输出</span>
                <pre class="diff-output">{{ runResult.stdout }}</pre>
              </div>
              <div v-if="runResult.statusDescription" class="case-verdict" :class="runResult.accepted ? 'pass' : 'fail'">
                {{ runResult.statusDescription }}
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ========== 右栏：AI评估结果 ========== -->
      <aside class="panel ai-panel">
        <div class="panel-head">
          <span class="panel-icon">
            <bulb-outlined />
          </span>
          <h2 class="panel-title">AI评估结果</h2>
        </div>

        <div class="panel-scroll">
          <!-- 未提交时 -->
          <div v-if="!submitResult && !aiReviewText && !submitting" class="ai-empty">
            <div class="ai-empty-icon"><bulb-outlined /></div>
            <p class="ai-empty-title">等待代码评估</p>
            <p class="ai-empty-text">点击「保存代码」提交后，<br/>AI 将为你提供详细评审</p>
          </div>

          <!-- 提交中 / 已提交 -->
          <div v-else class="ai-review-wrap">
            <div class="ai-review-head">
              <span class="ai-review-emoji">💯</span>
              <span class="ai-review-head-text">代码评审</span>
            </div>

            <!-- 总分 -->
            <div v-if="submitResult?.finalScore != null" class="ai-total-score">
              <span class="total-score-label">总分:</span>
              <span class="total-score-value" :class="(submitResult.finalScore || 0) >= 60 ? 'pass' : 'fail'">
                {{ submitResult.finalScore || 0 }}
              </span>
              <span class="total-score-unit">分</span>
            </div>

            <!-- 功能实现 -->
            <div v-if="submitResult?.testScore != null" class="ai-sub-score">
              <div class="ai-sub-score-head">
                <strong>功能实现:</strong>
                <span class="ai-sub-score-value">{{ submitResult.testScore }} 分</span>
              </div>
              <div class="ai-sub-score-meta">
                通过用例: {{ submitResult.passedCount ?? '-' }} / {{ submitResult.totalCount ?? '-' }}
              </div>
            </div>

            <!-- 代码质量 / AI 分 -->
            <div v-if="submitResult?.aiScore != null" class="ai-sub-score">
              <div class="ai-sub-score-head">
                <strong>代码质量:</strong>
                <span class="ai-sub-score-value">{{ submitResult.aiScore }} 分</span>
              </div>
            </div>

            <!-- 未通过样例详情 -->
            <div v-for="(tc, idx) in (submitResult?.testCaseResults || []).filter((t: any) => t.isSample && !t.passed)" :key="'s'+idx" class="case-item small">
              <div class="case-verdict fail">
                <close-circle-filled /> 样例未通过
              </div>
              <div class="case-diff">
                <div v-if="caseDiagnostic(tc)" class="case-error">
                  <span class="diff-label">错误信息</span>
                  <pre class="diff-output bad">{{ caseDiagnostic(tc) }}</pre>
                </div>
                <div class="diff-col">
                  <span class="diff-label">你的输出</span>
                  <pre class="diff-output bad">{{ tc.actualOutput || '(空)' }}</pre>
                </div>
                <div class="diff-col">
                  <span class="diff-label">期望输出</span>
                  <pre class="diff-output">{{ tc.expectedOutput || '(空)' }}</pre>
                </div>
              </div>
            </div>

            <!-- AI 评审报告 -->
            <div v-if="aiReviewText" class="ai-review-text md-body" v-html="renderMd(aiReviewText)"></div>

            <!-- 提交进行中的等待状态 -->
            <div v-if="submitting" class="ai-loading">
              <a-spin size="small" /> <span>AI 正在评审中...</span>
            </div>

            <!-- 失败状态 -->
            <div v-if="submitResult?.statusDescription && submitResult.accepted === false" class="ai-fail-hint">
              {{ submitResult.statusDescription }}
            </div>
          </div>
        </div>
      </aside>
    </div>

    <a-spin v-else-if="loading" style="display: block; padding: 120px; text-align: center" />

    <a-drawer v-model:open="showHistory" title="我的提交历史" width="640" class="history-drawer">
      <a-spin :spinning="historyLoading">
        <a-empty v-if="!historyLoading && history.length === 0" description="暂无提交记录" />
        <div class="drawer-history-list">
          <div
            v-for="h in history"
            :key="h.id"
            class="history-card"
            :class="{ 'is-passed': h.accepted }"
            @click="openHistoryDetail(h)"
          >
            <div class="history-top">
              <div class="history-title">
        <span class="history-score" :class="(h.finalScore || 0) >= 60 ? 'pass' : 'fail'">
          {{ h.finalScore || 0 }}分
        </span>
                <span class="history-status" :class="h.accepted ? 'pass' : 'fail'">
          {{ h.accepted ? '已通过' : '未通过' }}
        </span>
              </div>
              <div class="history-card-actions">
                <a-button size="small" type="link" @click.stop="restoreFromHistory(h)">恢复代码</a-button>
              </div>
            </div>

            <div class="history-time-row">
              <clock-circle-outlined /> {{ formatTime(h.createTime) }}
            </div>

            <div class="history-meta">
              <span class="meta-tag">{{ h.language }}</span>
              <span class="meta-tag">通过 {{ h.passedCount ?? 0 }}/{{ h.totalCount ?? 0 }}</span>
              <span class="meta-tag">测试 {{ h.testScore ?? 0 }}分</span>
              <span class="meta-tag">AI {{ h.aiScore ?? 0 }}分</span>
            </div>

            <div class="history-stats-box">
              <div class="mini-stat">
                <span class="mini-label">功能实现</span>
                <span class="mini-value">{{ h.testScore ?? 0 }}</span>
              </div>
              <div class="mini-stat">
                <span class="mini-label">代码质量</span>
                <span class="mini-value">{{ h.aiScore ?? 0 }}</span>
              </div>
              <div class="mini-stat" :class="(h.finalScore || 0) >= 60 ? '' : 'danger'">
                <span class="mini-label">总分</span>
                <span class="mini-value">{{ h.finalScore ?? 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </a-spin>
    </a-drawer>

    <!-- 提交详情弹窗 -->
    <a-modal
      v-model:open="historyDetailVisible"
      title="提交详情"
      width="800px"
      :footer="null"
      centered
      :bodyStyle="{ maxHeight: '60vh', overflowY: 'auto', padding: '20px 24px' }"
      class="history-detail-modal"
    >
      <div v-if="activeHistoryItem" class="history-detail-body">
        <div class="detail-header">
          <div class="detail-score-row">
            <span class="detail-score-label">总分</span>
            <span class="detail-score-value" :class="(activeHistoryItem.finalScore || 0) >= 60 ? 'pass' : 'fail'">
              {{ activeHistoryItem.finalScore ?? 0 }}
            </span>
          </div>
          <div class="detail-meta-row">
            <span class="detail-meta-tag">{{ activeHistoryItem.language }}</span>
            <span class="detail-meta-tag">通过 {{ activeHistoryItem.passedCount ?? 0 }}/{{ activeHistoryItem.totalCount ?? 0 }}</span>
            <span class="detail-meta-tag">{{ formatTime(activeHistoryItem.createTime) }}</span>
          </div>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">
            <file-text-outlined /> 提交的代码
          </div>
          <pre class="detail-code-block">{{ activeHistoryItem.code || '（无代码记录）' }}</pre>
        </div>

        <div v-if="activeHistoryItem.aiReviewMd" class="detail-section">
          <div class="detail-section-title">
            <bulb-outlined /> AI 评估报告
          </div>
          <div class="detail-ai-review md-body" v-html="renderMd(activeHistoryItem.aiReviewMd)"></div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { getLoginUser } from '@/utils/authStorage'
import { ref, onMounted, onBeforeUnmount, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { useTutorContextStore } from '@/stores/tutorContext'
import { storeToRefs } from 'pinia'
import {
  HistoryOutlined, ReloadOutlined, CaretRightOutlined, CloudUploadOutlined,
  BulbOutlined, ClearOutlined, ArrowLeftOutlined, UndoOutlined,
  ClockCircleOutlined, DatabaseOutlined, FileTextOutlined,
  CheckCircleFilled, CloseCircleFilled
} from '@ant-design/icons-vue'
import MarkdownIt from 'markdown-it'
import dayjs from 'dayjs'
import { getProblemDetail, runCode, submitCode, getSubmissionHistory } from '@/api/coding'

// CodeMirror imports
import { EditorView, keymap, lineNumbers, highlightActiveLineGutter, highlightSpecialChars, drawSelection, highlightActiveLine } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { defaultKeymap, history as cmHistory, historyKeymap, indentWithTab } from '@codemirror/commands'
import { syntaxHighlighting, defaultHighlightStyle, bracketMatching, foldGutter, indentOnInput } from '@codemirror/language'
import { autocompletion, closeBrackets, closeBracketsKeymap, completionKeymap } from '@codemirror/autocomplete'
import { searchKeymap } from '@codemirror/search'
import { oneDark } from '@codemirror/theme-one-dark'
import { java } from '@codemirror/lang-java'
import { python } from '@codemirror/lang-python'
import { javascript } from '@codemirror/lang-javascript'
import { cpp } from '@codemirror/lang-cpp'

// ====== 全局用户状态逻辑 ======
const userStore = useUserStore()
const tutorContext = useTutorContextStore()
const { userInfo } = storeToRefs(userStore)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820'

const mergedUserInfo = computed(() => {
  const localUser = getLoginUser<any>() || {}
  return { ...localUser, ...(userInfo.value || {}) }
})

const displayUserName = computed(() => {
  return mergedUserInfo.value?.userName || mergedUserInfo.value?.name || '同学'
})

const displayAvatarUrl = computed(() => {
  const rawAvatar = mergedUserInfo.value?.userAvatar || mergedUserInfo.value?.avatar
  if (!rawAvatar) return 'https://api.dicebear.com/7.x/notionists/svg?seed=smart-edu'
  if (rawAvatar.startsWith('http') || rawAvatar.startsWith('data:image')) return rawAvatar
  return `${API_BASE_URL}${rawAvatar.startsWith('/') ? '' : '/'}${rawAvatar}`
})

const md = new MarkdownIt({ breaks: true, linkify: true, html: false })

const preprocessAiReview = (text: string): string => {
  if (!text) return ''
  let t = text
  // 1. 去掉 <SCORE>...</SCORE> 标签（包括空标签）
  t = t.replace(/<SCORE>[\s\S]*?<\/SCORE>/gi, '')
  // 2. 把行内挤在一起的 ### 标题拆开（如：报告###总体评价 -> 报告\n\n### 总体评价）
  t = t.replace(/([^\n])#{3,6}\s*([^#\n])/g, '$1\n\n### $2')
  // 3. 修复行首标题缺少空格：###标题 -> ### 标题
  t = t.replace(/^(#{1,6})([^\s#])/gm, '$1 $2')
  // 4. 保留粗体 **文字** 原样，不插入空格（Markdown 要求 ** 紧贴文字）
  // 5. 处理 checklist 符号与 - 挤在一起的情况
  t = t.replace(/-\s*([✅✓✔☑])/g, '- $1')
  t = t.replace(/([✅✓✔☑])\s*-/g, '$1\n- ')
  // 6. 尝试把 javaimport / pythonimport 这种挤在一起的代码拆开
  t = t.replace(/\b(java|python|cpp|c\+\+|javascript|js)(import|public|class|def|function|#include|#ifdef)/gi, '$1\n$2')
  // 7. 多个换行统一为两个
  t = t.replace(/\n{3,}/g, '\n\n')
  return t.trim()
}

const renderMd = (text: string) => md.render(preprocessAiReview(text))

const caseDiagnostic = (tc: any) => {
  return tc?.compileOutput || tc?.stderr || tc?.statusDescription || (tc?.status && tc.status !== 'accepted' ? tc.status : '')
}

const route = useRoute()
const router = useRouter()

const problem = ref<any>(null)
const loading = ref(false)
const code = ref('')
const currentLanguage = ref('java')
const running = ref(false)
const submitting = ref(false)
const runResult = ref<any>(null)
const submitResult = ref<any>(null)
const leftTab = ref<'desc' | 'samples'>('desc')

const aiReviewText = ref('')

const problemId = computed(() => Number(route.params.id))
const storageKey = computed(() => `coding:${problemId.value}:${currentLanguage.value}`)
const langPrefKey = computed(() => `coding:${problemId.value}:lang`)

const sampleCases = computed(() => problem.value?.sampleTestCases || [])
const lineCount = computed(() => Math.max(1, (code.value || '').split('\n').length))

const renderedDescription = computed(() => renderMd(problem.value?.description || ''))

const deadlineText = computed(() => {
  const dl = problem.value?.deadline
  if (!dl) return ''
  const d = dayjs(dl)
  if (d.isBefore(dayjs())) return '已截止'
  return '截止 ' + d.format('MM-DD HH:mm')
})
const isDeadlineSoon = computed(() => problem.value?.deadline && dayjs(problem.value.deadline).isBefore(dayjs().add(1, 'hour')))
const isDeadlinePassed = computed(() => problem.value?.deadline && dayjs(problem.value.deadline).isBefore(dayjs()))

// ====== CodeMirror ======
const editorContainerRef = ref<HTMLElement | null>(null)
let editorView: EditorView | null = null

const getLangExtension = (lang: string) => {
  switch (lang.toLowerCase()) {
    case 'java': return java()
    case 'python': return python()
    case 'javascript': return javascript()
    case 'cpp': return cpp()
    default: return []
  }
}

const createEditor = () => {
  if (!editorContainerRef.value) return
  if (editorView) editorView.destroy()

  const state = EditorState.create({
    doc: code.value,
    extensions: [
      lineNumbers(), highlightActiveLineGutter(), highlightSpecialChars(), cmHistory(),
      foldGutter(), drawSelection(), indentOnInput(),
      syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
      bracketMatching(), closeBrackets(), autocompletion(), highlightActiveLine(),
      keymap.of([...closeBracketsKeymap, ...defaultKeymap, ...searchKeymap, ...historyKeymap, ...completionKeymap, indentWithTab]),
      keymap.of([
        { key: 'Ctrl-Enter', run: () => { handleRun(); return true } },
        { key: 'Ctrl-Shift-Enter', run: () => { handleSubmit(); return true } },
        { key: 'Tab', run: (target) => { target.dispatch(target.state.update(target.state.replaceSelection('    '))); return true } }
      ]),
      getLangExtension(currentLanguage.value), oneDark,
      EditorView.updateListener.of((update) => {
        if (update.docChanged) code.value = update.state.doc.toString()
      }),
      EditorView.theme({ '&': { height: '100%' }, '.cm-scroller': { overflow: 'auto' } })
    ]
  })

  editorView = new EditorView({ state, parent: editorContainerRef.value })
}

const switchEditorLanguage = () => {
  if (!editorView) return
  const currentContent = editorView.state.doc.toString()
  const newState = EditorState.create({
    doc: currentContent,
    extensions: [
      lineNumbers(), highlightActiveLineGutter(), highlightSpecialChars(), cmHistory(), foldGutter(), drawSelection(), indentOnInput(),
      syntaxHighlighting(defaultHighlightStyle, { fallback: true }), bracketMatching(), closeBrackets(), autocompletion(), highlightActiveLine(),
      keymap.of([...closeBracketsKeymap, ...defaultKeymap, ...searchKeymap, ...historyKeymap, ...completionKeymap, indentWithTab]),
      getLangExtension(currentLanguage.value), oneDark,
      EditorView.updateListener.of((update) => { if (update.docChanged) code.value = update.state.doc.toString() }),
      EditorView.theme({ '&': { height: '100%' }, '.cm-scroller': { overflow: 'auto' } })
    ]
  })
  editorView.setState(newState)
}

const setEditorContent = (content: string) => {
  if (!editorView) return
  editorView.dispatch({ changes: { from: 0, to: editorView.state.doc.length, insert: content } })
}

// ====== 代码管理 ======
const goBack = () => router.push('/student/coding')
const templateFor = (lang: string) => (problem.value?.templates || []).find((x: any) => x.language === lang)?.starterCode || ''

const loadCodeForLang = () => {
  const saved = localStorage.getItem(storageKey.value)
  code.value = saved != null ? saved : templateFor(currentLanguage.value)
  if (editorView) setEditorContent(code.value)
}

const onLanguageChange = () => {
  localStorage.setItem(langPrefKey.value, currentLanguage.value)
  loadCodeForLang()
  switchEditorLanguage()
  runResult.value = null
  submitResult.value = null
  syncTutorCodingContext()
}

const resetTemplate = () => {
  const t = templateFor(currentLanguage.value)
  code.value = t
  setEditorContent(t)
  message.success('已恢复初始模板')
}

const clearCode = () => {
  code.value = ''
  setEditorContent('')
}

watch(code, (v) => {
  if (problem.value) localStorage.setItem(storageKey.value, v || '')
  syncTutorCodingContext()
})

// ====== 运行 ======
const handleRun = async () => {
  if (!code.value.trim()) { message.warning('代码为空'); return }
  running.value = true; runResult.value = null; submitResult.value = null
  try {
    runResult.value = await runCode({ problemId: problemId.value, language: currentLanguage.value, code: code.value })
  } catch (e: any) {
    runResult.value = { statusDescription: '运行失败: ' + (e.message || '未知错误'), accepted: false }
  } finally {
    running.value = false
    syncTutorCodingContext()
  }
}

// ====== 同步提交（与教师端报告生成一致的效果）======
const handleSubmit = async () => {
  if (!code.value.trim()) { message.warning('代码为空'); return }
  submitting.value = true
  runResult.value = null
  submitResult.value = null
  aiReviewText.value = ''

  try {
    const result = await submitCode({ problemId: problemId.value, language: currentLanguage.value, code: code.value })
    submitResult.value = result
    aiReviewText.value = result.aiReviewMd || ''
    if (result.accepted) message.success('全部用例通过！')
  } catch (e: any) {
    submitResult.value = { statusDescription: '提交失败: ' + (e.message || '未知错误'), accepted: false }
  } finally {
    submitting.value = false
    loadLastSubmission()
    syncTutorCodingContext()
  }
}

// ====== 历史提交 ======
const showHistory = ref(false)
const history = ref<any[]>([])
const historyLoading = ref(false)
const lastSubmission = ref<any>(null)
const historyDetailVisible = ref(false)
const activeHistoryItem = ref<any>(null)

const openHistoryDetail = (h: any) => {
  activeHistoryItem.value = h
  historyDetailVisible.value = true
}

const openHistory = async () => {
  showHistory.value = true; historyLoading.value = true
  try {
    const data = await getSubmissionHistory({ problemId: problemId.value })
    history.value = (Array.isArray(data) ? data : []).map((h: any) => ({ ...h, _showCode: false }))
  } catch (e) { history.value = [] } finally { historyLoading.value = false }
}

const loadLastSubmission = async () => {
  try {
    const data = await getSubmissionHistory({ problemId: problemId.value })
    lastSubmission.value = (Array.isArray(data) ? data : [])[0] || null
  } catch (e) { lastSubmission.value = null }
}

const restoreLastCode = () => {
  if (lastSubmission.value?.code) {
    code.value = lastSubmission.value.code
    setEditorContent(lastSubmission.value.code)
    message.success('已恢复上次提交的代码')
  } else message.info('暂无上次提交记录')
}

const restoreFromHistory = (h: any) => {
  if (h.code) {
    code.value = h.code; setEditorContent(h.code); showHistory.value = false; message.success('已恢复该提交的代码')
  } else message.warning('该提交无代码记录')
}

const formatTime = (t: any) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
const difficultyColor = (d: string) => d === 'easy' ? 'green' : d === 'hard' ? 'red' : 'orange'
const difficultyLabel = (d: string) => d === 'easy' ? '简单' : d === 'hard' ? '困难' : '中等'

const syncTutorCodingContext = () => {
  if (!problem.value) return
  tutorContext.setCodingContext({
    problemId: problemId.value,
    problemTitle: problem.value.title || '编程练习',
    difficulty: difficultyLabel(problem.value.difficulty),
    language: currentLanguage.value,
    code: code.value,
    runResultSummary: tutorContext.summarizeRunResult(runResult.value || submitResult.value)
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await getProblemDetail({ problemId: problemId.value })
    problem.value = data
    if (data?.languages?.length) {
      const pref = localStorage.getItem(langPrefKey.value)
      currentLanguage.value = pref && data.languages.includes(pref) ? pref : data.languages[0]
    }
    loadCodeForLang()
    await nextTick()
    createEditor()
    loadLastSubmission()
    syncTutorCodingContext()
  } catch (e) {} finally { loading.value = false }
})

onBeforeUnmount(() => {
  if (editorView) { editorView.destroy(); editorView = null }
})
</script>


<style scoped>
/* ============================================================
   设计令牌 - 与 Dashboard 保持完全一致
============================================================ */
.coding-problem-page {
  --primary-color: #2563EB;
  --primary-hover: #1D4ED8;
  --primary-soft: #EFF6FF;
  --bg-card: #FFFFFF;
  --bg-sub: #F8FAFD;
  --text-main: #1F2937;
  --text-regular: #344054;
  --text-sub: #667085;
  --text-light: #98A2B3;
  --border-color: #E7ECF3;
  --radius-xl: 12px;
  --radius-lg: 8px;
  --radius-md: 5px;   /* 标准圆角 */
  --radius-sm: 3px;
  --shadow-sm: 0 2px 8px rgba(15, 23, 42, 0.04);
  --shadow-md: 0 8px 24px rgba(15, 23, 42, 0.06);

  --pass: #10B981;
  --pass-bg: #ECFDF5;
  --pass-border: #A7F3D0;
  --fail: #EF4444;
  --fail-bg: #FEF2F2;
  --fail-border: #FECACA;
  --warn: #D97706;

  /* 整页锁死：不允许页面级滚动 */
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: var(--text-regular);
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* ============================================================
   顶部精简 Header
============================================================ */
.page-header {
  flex-shrink: 0;
  height: 64px;
  padding: 0 92px;    /* 两侧为悬浮按钮和数字人留出空间 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: transparent;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-btn {
  height: 36px !important;
  padding: 0 16px !important;
  border-radius: var(--radius-md) !important;
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color) !important;
  color: var(--text-regular) !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  box-shadow: var(--shadow-sm) !important;
  display: inline-flex !important;
  align-items: center;
  gap: 6px;
  transition: all 0.2s !important;
}
.header-btn:hover {
  color: var(--primary-color) !important;
  border-color: #BFDBFE !important;
}

/* ============================================================
   三栏主网格 - 占满剩余高度，不允许溢出
============================================================ */
.main-grid {
  flex: 1;
  min-height: 0;    /* 关键：允许 flex 子项被挤压 */
  display: grid;
  grid-template-columns: 320px 1fr 360px;
  gap: 16px;
  padding: 0 92px 20px;    /* 两侧为悬浮按钮和数字人留出空间 */
  box-sizing: border-box;
  overflow: hidden;
}

/* ============================================================
   卡片通用样式（和 Dashboard 对齐）
============================================================ */
.panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;      /* 让滚动发生在内部区域 */
  min-height: 0;
  transition: box-shadow 0.3s;
}
.panel:hover { box-shadow: var(--shadow-md); }

.panel-head {
  flex-shrink: 0;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-card);
}
.panel-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.panel-icon {
  width: 26px;
  height: 26px;
  border-radius: var(--radius-md);
  background: var(--primary-soft);
  color: var(--primary-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}
.panel-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 只允许在 panel-scroll 内部滚动 */
.panel-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px;
  min-height: 0;
}

/* 统一定制滚动条（细） */
.panel-scroll::-webkit-scrollbar,
.inline-result-body::-webkit-scrollbar,
.diff-output::-webkit-scrollbar { width: 6px; height: 6px; }
.panel-scroll::-webkit-scrollbar-thumb,
.inline-result-body::-webkit-scrollbar-thumb,
.diff-output::-webkit-scrollbar-thumb { background: #D6DEE8; border-radius: 3px; }
.panel-scroll::-webkit-scrollbar-track { background: transparent; }

/* ============================================================
   左栏：题目信息
============================================================ */
.info-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px dashed var(--border-color);
}

.info-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.info-label {
  font-size: 12px;
  color: var(--text-sub);
  font-weight: 500;
}
.info-block { display: flex; flex-direction: column; gap: 5px; }
.info-values {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.info-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 10px;
  font-size: 12px;
  color: var(--text-regular);
  background: var(--bg-sub);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  line-height: 20px;
}
.info-chip.deadline-warn {
  color: var(--warn);
  background: #FEF3C7;
  border-color: #FCD34D;
  font-weight: 600;
}

.diff-tag {
  border-radius: var(--radius-md) !important;
  font-size: 12px !important;
  padding: 0 10px !important;
  line-height: 22px !important;
  font-weight: 500;
  margin: 0 !important;
}

/* tab 切换 */
.info-tabs {
  display: flex;
  gap: 4px;
  padding: 3px;
  background: var(--bg-sub);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  margin-bottom: 14px;
}
.info-tab {
  flex: 1;
  text-align: center;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-sub);
  border-radius: var(--radius-md);
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
}
.info-tab:hover { color: var(--text-main); }
.info-tab.active {
  background: var(--bg-card);
  color: var(--primary-color);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.tab-body {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-regular);
}
.problem-content { font-size: 13px; line-height: 1.8; }

/* 样例 */
.sample-case { margin-bottom: 16px; }
.sample-label {
  font-weight: 700;
  color: var(--text-main);
  font-size: 13px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.sample-label::before {
  content: ''; width: 3px; height: 12px;
  background: var(--primary-color); border-radius: 2px;
}
.sample-block { margin-bottom: 8px; }
.sample-key {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-sub);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.sample-block pre {
  background: var(--bg-sub);
  border: 1px solid var(--border-color);
  padding: 10px 12px;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 0;
  font-family: 'SF Mono', Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-main);
}
.empty-hint {
  text-align: center;
  padding: 40px 0;
  color: var(--text-light);
}
.empty-hint p { margin: 12px 0 0; font-size: 13px; }

/* Markdown */
.md-body :deep(pre) {
  background: var(--bg-sub);
  padding: 12px 14px;
  border-radius: var(--radius-md);
  overflow-x: auto;
  border: 1px solid var(--border-color);
  font-family: 'SF Mono', Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.6;
}
.md-body :deep(code) {
  background: var(--primary-soft);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-family: 'SF Mono', Consolas, Monaco, monospace;
  font-size: 12px;
  color: var(--primary-hover);
}
.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3) {
  color: var(--text-main);
  font-weight: 700;
}

/* ============================================================
   中栏：编辑器
============================================================ */
.editor-panel { min-width: 0; }

.editor-head {
  justify-content: space-between;
}
.editor-head-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  height: 32px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-regular);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}
.action-btn:hover:not(:disabled) {
  color: var(--primary-color);
  border-color: #BFDBFE;
  background: var(--primary-soft);
}
.action-btn.primary {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
}
.action-btn.primary:hover:not(:disabled) {
  background: var(--primary-hover);
  border-color: var(--primary-hover);
  color: #fff;
}
.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.editor-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 12px;
  gap: 8px;
}

.editor-sub-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2px;
  flex-shrink: 0;
}
.editor-sub-right { display: flex; gap: 4px; }

.editor-sub-toolbar :deep(.ant-select-selector) {
  border-radius: var(--radius-md) !important;
  height: 30px !important;
  font-size: 13px !important;
  border-color: var(--border-color) !important;
}
.editor-sub-toolbar :deep(.ant-select-selection-item) { line-height: 28px !important; }

.icon-btn {
  width: 30px;
  height: 30px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-sub);
  cursor: pointer;
  border-radius: var(--radius-md);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s;
}
.icon-btn:hover {
  background: var(--primary-soft);
  color: var(--primary-color);
}

.editor-wrapper {
  flex: 1;
  min-height: 0;        /* 关键：允许被挤压 */
  background: #1e1e1e;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid #2a2a2a;
}
.editor-wrapper :deep(.cm-editor) {
  font-family: 'SF Mono', Consolas, Monaco, monospace;
  font-size: 13px;
  line-height: 1.6;
  height: 100%;
}
.editor-wrapper :deep(.cm-scroller) { overflow: auto; }

.editor-status {
  flex-shrink: 0;
  padding: 4px 8px;
  display: flex;
  justify-content: space-between;
  color: var(--text-light);
  font-size: 11px;
  font-family: 'SF Mono', Consolas, Monaco, monospace;
}
.lang-label { color: var(--primary-color); text-transform: lowercase; font-weight: 600; }

/* 运行结果内嵌 */
.inline-result {
  flex-shrink: 0;
  margin: 0 12px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-sub);
  max-height: 200px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.inline-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-card);
  flex-shrink: 0;
}
.inline-result-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.inline-result-title :deep(.anticon) { color: var(--primary-color); }
.inline-result-body {
  padding: 10px 12px;
  overflow-y: auto;
  font-size: 13px;
}

.result-status {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-md);
}
.result-status.pass { background: var(--pass-bg); color: var(--pass); border: 1px solid var(--pass-border); }
.result-status.fail { background: var(--fail-bg); color: var(--fail); border: 1px solid var(--fail-border); }

.case-verdict {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: var(--radius-md);
  margin-bottom: 6px;
}
.case-verdict.pass { background: var(--pass-bg); color: var(--pass); }
.case-verdict.fail { background: var(--fail-bg); color: var(--fail); }

.case-item { margin-bottom: 10px; }
.case-item.small { margin-bottom: 8px; }
.case-diff {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 6px;
}
.case-error {
  grid-column: 1 / -1;
}
.diff-col { display: flex; flex-direction: column; gap: 4px; }
.diff-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-sub);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.diff-output {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  padding: 8px 10px;
  border-radius: var(--radius-md);
  font-family: 'SF Mono', Consolas, Monaco, monospace;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-main);
  margin: 0;
  overflow-x: auto;
}
.diff-output.bad {
  background: var(--fail-bg);
  border-color: var(--fail-border);
  color: var(--fail);
}
.error-block { margin-bottom: 10px; }

/* ============================================================
   右栏：AI 评估结果
============================================================ */
.ai-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 280px;
  text-align: center;
  padding: 20px;
  color: var(--text-light);
}
.ai-empty-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  background: var(--primary-soft);
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 14px;
}
.ai-empty-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0 0 6px;
}
.ai-empty-text {
  font-size: 13px;
  color: var(--text-sub);
  margin: 0;
  line-height: 1.7;
}

.ai-review-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ai-review-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
}
.ai-review-emoji { font-size: 16px; }

.ai-total-score {
  display: flex;
  align-items: baseline;
  gap: 4px;
  padding: 10px 12px;
  background: var(--bg-sub);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}
.total-score-label {
  font-size: 13px;
  color: var(--text-regular);
  font-weight: 600;
  margin-right: 4px;
}
.total-score-value {
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
}
.total-score-value.pass { color: var(--primary-color); }
.total-score-value.fail { color: var(--fail); }
.total-score-unit {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-regular);
  margin-left: 2px;
}

.ai-sub-score {
  padding: 10px 12px;
  background: var(--bg-sub);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}
.ai-sub-score-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 13px;
  color: var(--text-main);
  margin-bottom: 4px;
}
.ai-sub-score-head strong { font-weight: 600; }
.ai-sub-score-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--primary-color);
}
.ai-sub-score-meta {
  font-size: 12px;
  color: var(--text-sub);
}

.ai-review-text {
  color: #334155;
  line-height: 1.9;
  font-size: 14px;
}
.ai-review-text :deep(p) {
  margin: 0 0 12px;
}
.ai-review-text :deep(h1),
.ai-review-text :deep(h2),
.ai-review-text :deep(h3),
.ai-review-text :deep(h4) {
  margin: 16px 0 10px;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
  padding-left: 10px;
  border-left: 4px solid #2563eb;
}
.ai-review-text :deep(h1) { font-size: 16px; }
.ai-review-text :deep(h2) { font-size: 15px; }
.ai-review-text :deep(h3) { font-size: 14px; }
.ai-review-text :deep(h4) { font-size: 13px; }
.ai-review-text :deep(strong) {
  color: #1e293b;
  font-weight: 700;
  background: #eef2ff;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #e0e7ff;
}
.ai-review-text :deep(ul),
.ai-review-text :deep(ol) {
  margin: 0 0 12px;
  padding-left: 0;
  list-style: none;
}
.ai-review-text :deep(ul > li),
.ai-review-text :deep(ol > li) {
  position: relative;
  margin-bottom: 8px;
  padding: 10px 12px 10px 36px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}
.ai-review-text :deep(ol) {
  counter-reset: report-step;
}
.ai-review-text :deep(ol > li::before) {
  counter-increment: report-step;
  content: counter(report-step);
  position: absolute;
  left: 10px;
  top: 10px;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #dbeafe;
  color: #2563eb;
  font-size: 11px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ai-review-text :deep(ul > li::before) {
  content: '';
  position: absolute;
  left: 14px;
  top: 16px;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #2563eb;
}
.ai-review-text :deep(pre) {
  background: #1e1e2e;
  border: 1px solid #313244;
  border-radius: 6px;
  padding: 12px 14px;
  overflow-x: auto;
  font-family: 'SF Mono', Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  margin: 10px 0;
  color: #cdd6f4;
}
.ai-review-text :deep(code) {
  background: #f1f5f9;
  padding: 1px 5px;
  border-radius: 3px;
  font-family: 'SF Mono', Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  color: #c41d7f;
}
.ai-review-text :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: 12px;
}
.ai-review-text :deep(blockquote) {
  margin: 12px 0;
  padding: 10px 14px;
  background: #fffbeb;
  border-left: 4px solid #f59e0b;
  border-radius: 0 6px 6px 0;
  color: #92400e;
  font-size: 13px;
}
.ai-review-text :deep(hr) {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 14px 0;
}
.ai-review-text :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
  font-size: 13px;
}
.ai-review-text :deep(th),
.ai-review-text :deep(td) {
  border: 1px solid #e2e8f0;
  padding: 8px 10px;
  text-align: left;
}
.ai-review-text :deep(th) {
  background: #f8fafc;
  font-weight: 600;
}

.ai-cursor {
  display: inline-block;
  animation: blink 1s infinite;
  color: var(--primary-color);
}
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }

.ai-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--primary-soft);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--primary-hover);
}

.ai-fail-hint {
  padding: 8px 12px;
  background: var(--fail-bg);
  border: 1px solid var(--fail-border);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--fail);
}

/* ============================================================
   历史提交抽屉 - 卡片布局 (参考 TaskMonitor 优化)
============================================================ */
.drawer-history-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px; /* 防止悬浮阴影被抽屉边缘截断 */
}

.history-card {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}

/* 左侧状态指示线 */
.history-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #e2e8f0;
  transition: background 0.25s ease;
}

.history-card:hover {
  transform: translateY(-2px);
  border-color: #cbd5e1;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.04);
}

/* 悬浮时左侧边栏亮起，通过为绿，未通过为红 */
.history-card.is-passed:hover::before {
  background: #10B981;
}
.history-card:not(.is-passed):hover::before {
  background: #EF4444;
}

.history-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-left: 4px; /* 避开左侧指示线 */
}

.history-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-score {
  font-size: 20px;
  font-weight: 800;
}
.history-score.pass { color: #10B981; }
.history-score.fail { color: #1e293b; } /* 没过时采用深灰，避免全红太刺眼 */

.history-status {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 4px;
}
.history-status.pass {
  background: #ECFDF5;
  color: #10B981;
  border: 1px solid #A7F3D0;
}
.history-status.fail {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.history-time-row {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  padding-left: 4px;
}

.history-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  padding-left: 4px;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  font-size: 12px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  font-weight: 500;
}

/* 底部数据网格面板 */
.history-stats-box {
  display: flex;
  gap: 12px;
  background: #f8fafc;
  padding: 12px;
  border-radius: 6px;
  margin-left: 4px;
}

.mini-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mini-label {
  font-size: 12px;
  color: #64748b;
}

.mini-value {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.mini-stat.danger .mini-value {
  color: #ef4444;
}
/* ============================================================
   提交详情弹窗
============================================================ */
.history-detail-modal :deep(.ant-modal-content) {
  /* 确保弹窗外壳不会被撑开，留出一点圆角和阴影的舒适区 */
  overflow: hidden;
}

.history-detail-modal :deep(.ant-modal-body) {
  /* 核心修改：设置固定高度，而不是 max-height。这里用 60vh 适配不同屏幕 */
  height: 60vh;
  min-height: 400px;
  /* 核心修改：内容超出高度时，强制在内部生成滚动条 */
  overflow-y: auto;
  padding: 20px 24px;
}

/* 美化弹窗内部的滚动条 */
.history-detail-modal :deep(.ant-modal-body)::-webkit-scrollbar {
  width: 6px;
}
.history-detail-modal :deep(.ant-modal-body)::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}
.history-detail-modal :deep(.ant-modal-body)::-webkit-scrollbar-track {
  background: transparent;
}

.detail-header {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}
.detail-score-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}
.detail-score-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-regular);
}
.detail-score-value {
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
}
.detail-score-value.pass { color: var(--pass); }
.detail-score-value.fail { color: var(--fail); }

.detail-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.detail-meta-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  font-size: 12px;
  color: var(--text-sub);
  background: var(--bg-sub);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-weight: 500;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.detail-section-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
  display: flex;
  align-items: center;
  gap: 8px;
}
.detail-section-title :deep(.anticon) {
  color: var(--primary-color);
}

.detail-code-block {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 14px;
  border-radius: var(--radius-md);
  overflow-x: auto;
  font-family: 'SF Mono', Consolas, Monaco, monospace;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  border: 1px solid #313244;
  max-height: 360px;
  overflow-y: auto;
}

.detail-ai-review {
  color: #334155;
  line-height: 1.9;
  font-size: 14px;
}
.detail-ai-review :deep(p) { margin: 0 0 12px; }
.detail-ai-review :deep(h1),
.detail-ai-review :deep(h2),
.detail-ai-review :deep(h3),
.detail-ai-review :deep(h4) {
  margin: 16px 0 10px;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
  padding-left: 10px;
  border-left: 4px solid #2563eb;
}
.detail-ai-review :deep(h1) { font-size: 16px; }
.detail-ai-review :deep(h2) { font-size: 15px; }
.detail-ai-review :deep(h3) { font-size: 14px; }
.detail-ai-review :deep(h4) { font-size: 13px; }
.detail-ai-review :deep(strong) {
  color: #1e293b;
  font-weight: 700;
  background: #eef2ff;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #e0e7ff;
}
.detail-ai-review :deep(ul),
.detail-ai-review :deep(ol) {
  margin: 0 0 12px;
  padding-left: 0;
  list-style: none;
}
.detail-ai-review :deep(ul > li),
.detail-ai-review :deep(ol > li) {
  position: relative;
  margin-bottom: 8px;
  padding: 10px 12px 10px 36px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}
.detail-ai-review :deep(ol) { counter-reset: report-step; }
.detail-ai-review :deep(ol > li::before) {
  counter-increment: report-step;
  content: counter(report-step);
  position: absolute;
  left: 10px;
  top: 10px;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #dbeafe;
  color: #2563eb;
  font-size: 11px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-ai-review :deep(ul > li::before) {
  content: '';
  position: absolute;
  left: 14px;
  top: 16px;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #2563eb;
}
.detail-ai-review :deep(pre) {
  background: #1e1e2e;
  border: 1px solid #313244;
  border-radius: 6px;
  padding: 12px 14px;
  overflow-x: auto;
  font-family: 'SF Mono', Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  margin: 10px 0;
  color: #cdd6f4;
}
.detail-ai-review :deep(code) {
  background: #f1f5f9;
  padding: 1px 5px;
  border-radius: 3px;
  font-family: 'SF Mono', Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  color: #c41d7f;
}
.detail-ai-review :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: 12px;
}
.detail-ai-review :deep(blockquote) {
  margin: 12px 0;
  padding: 10px 14px;
  background: #fffbeb;
  border-left: 4px solid #f59e0b;
  border-radius: 0 6px 6px 0;
  color: #92400e;
  font-size: 13px;
}

/* ============================================================
   窄屏兜底（依然保持整页不滚）
============================================================ */
@media (max-width: 1400px) {
  .main-grid {
    grid-template-columns: 280px 1fr 320px;
    gap: 14px;
    padding: 0 84px 16px;
  }
  .page-header { padding: 0 84px; }
}

@media (max-width: 1100px) {
  /* 窄屏下放开整页滚动并堆叠，并收回过大的 padding */
  .coding-problem-page { height: auto; min-height: 100vh; overflow: auto; }
  .main-grid {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 0 76px 16px;
  }
  .page-header { padding: 0 76px; }
  .panel { min-height: 360px; }
  .editor-panel { min-height: 500px; }
}
</style>

<style>
/* 全局样式，专门针对挂载在 body 上的历史记录弹窗滚动条 */
.history-detail-modal .ant-modal-body::-webkit-scrollbar {
  width: 6px;
}
.history-detail-modal .ant-modal-body::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}
.history-detail-modal .ant-modal-body::-webkit-scrollbar-track {
  background: transparent;
}
</style>
