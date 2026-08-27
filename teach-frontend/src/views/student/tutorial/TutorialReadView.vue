<template>
  <div class="read-page">
    <div class="read-layout">
      <aside class="sidebar" aria-label="课程目录">
        <div class="sidebar-header">
          <a-button type="text" size="small" class="back-btn" @click="router.back()">
            <template #icon><arrow-left-outlined /></template>
            返回列表
          </a-button>
          <div class="header-title">
            <span>目录导航</span>
            <small>共 {{ directory.length }} 个章节</small>
          </div>
        </div>

        <div v-if="directory.length" class="menu-list">
          <div
            v-for="(node, index) in directory"
            :key="node.id"
            :class="['menu-item', activeNodeId === node.id ? 'active' : '']"
            @click="handleNodeClick(node.id)"
          >
            <span class="menu-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="menu-title">{{ node.title }}</span>
          </div>
        </div>
        <div v-else class="menu-empty">暂无目录内容</div>
      </aside>

    <main class="content-area">
      <div v-if="loading" class="loading-state">
        <a-spin tip="正在加载内容..." />
      </div>

      <article v-else-if="currentNode.id" class="markdown-wrapper">
        <header class="article-header">
          <h1 class="article-title">{{ currentNode.title }}</h1>
          <div class="meta-info">更新时间：{{ formatTime(currentNode.createTime) || '暂无时间' }}</div>
        </header>
        <div class="markdown-body" v-html="renderMarkdown(currentNode.content)"></div>
      </article>

      <div v-else class="empty-state">
        <div class="empty-icon">文</div>
        <p>请选择左侧章节开始阅读</p>
      </div>
    </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request'
import { message } from 'ant-design-vue';
import { ArrowLeftOutlined } from '@ant-design/icons-vue';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark-dimmed.css';

const route = useRoute();
const router = useRouter();
const courseId = route.params.courseId;

// --- Markdown 配置 ---
const md = new MarkdownIt({
  html: true, linkify: true, typographer: true,
  highlight: (str, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>';
      } catch (__) {}
    }
    return '';
  }
});

// 数据
const directory = ref<any[]>([]);
const activeNodeId = ref<number | null>(null);
const currentNode = ref<any>({});
const loading = ref(false);

// 1. 获取目录
const fetchDirectory = async () => {
  try {
    const data = await request.get<any[], any[]>(`/tutorial/directory/${courseId}`)
    directory.value = data || []

    if (directory.value.length > 0) {
      handleNodeClick(directory.value[0].id)
    }
  } catch (e) {
    message.error('目录加载失败')
  }
}

// 2. 点击目录，加载文章详情
const handleNodeClick = async (nodeId: number) => {
  activeNodeId.value = nodeId
  loading.value = true
  try {
    const data = await request.get<any, any>(`/tutorial/node/${nodeId}`)
    currentNode.value = data
  } catch (e) {
    message.error('内容加载失败')
  } finally {
    loading.value = false
  }
}
const renderMarkdown = (text: string) => {
  return md.render(text || '');
};

const formatTime = (timeStr: string) => {
  if(!timeStr) return '';
  return timeStr.replace('T', ' ').substring(0, 19);
}

onMounted(() => {
  fetchDirectory();
});
</script>

<style scoped>
.read-page {
  --primary-color: #2563EB;
  --primary-hover: #1D4ED8;
  --text-main: #1F2937;
  --text-regular: #344054;
  --text-sub: #667085;
  --text-light: #98A2B3;
  --border-color: #E7ECF3;
  --bg-page: #F6F8FC;
  --bg-sub: #F8FAFD;
  --bg-card: #FFFFFF;

  min-height: calc(100vh - 70px);
  padding: 20px 24px 40px;
  background: var(--bg-page);
  color: var(--text-regular);
}

.read-layout {
  display: flex;
  width: 75%;
  max-width: 1400px;
  min-height: calc(100vh - 130px);
  margin: 0 auto;
  gap: 18px;
}

.sidebar {
  width: 292px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
}

.sidebar-header {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 18px 18px;
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(180deg, #FFFFFF 0%, var(--bg-sub) 100%);
}

.back-btn {
  width: fit-content;
  height: 32px;
  padding: 0 10px;
  border-radius: 5px;
  border: 1px solid #BFDBFE;
  background: #EFF6FF;
  color: #1D4ED8;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.08);
}

.back-btn:hover {
  border-color: #93C5FD;
  background: #DBEAFE;
  color: #1E40AF;
}

.back-btn:focus-visible {
  outline: 3px solid rgba(37, 99, 235, 0.18);
  outline-offset: 2px;
}

.header-title {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  position: relative;
  text-align: center;
}

.header-title span {
  color: var(--text-main);
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
}

.header-title small {
  position: absolute;
  right: 0;
  color: var(--text-light);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  transform: translateY(1px);
}

.menu-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 10px;
}

.menu-list::-webkit-scrollbar,
.content-area::-webkit-scrollbar {
  width: 6px;
}

.menu-list::-webkit-scrollbar-thumb,
.content-area::-webkit-scrollbar-thumb {
  background: #CBD5E1;
  border-radius: 999px;
}

.menu-list::-webkit-scrollbar-track,
.content-area::-webkit-scrollbar-track {
  background: transparent;
}

.menu-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: flex-start;
  gap: 10px;
  min-height: 44px;
  padding: 11px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-sub);
  transition: background-color 0.18s ease, color 0.18s ease;
}

.menu-item + .menu-item {
  margin-top: 4px;
}

.menu-item:hover {
  background: #F1F5F9;
  color: var(--text-main);
}

.menu-item.active {
  background: #EEF4FF;
  color: var(--primary-color);
}

.menu-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  border-radius: 5px;
  background: #F8FAFC;
  color: #94A3B8;
  font-size: 11px;
  font-weight: 800;
}

.menu-item.active .menu-index {
  background: var(--primary-color);
  color: #FFFFFF;
}

.menu-title {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.55;
  word-break: break-word;
}

.menu-empty {
  display: grid;
  place-items: center;
  flex: 1;
  min-height: 220px;
  color: var(--text-light);
  font-size: 14px;
}

.content-area {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
}

.loading-state,
.empty-state {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-sub);
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 8px;
  background: #EEF4FF;
  color: var(--primary-color);
  font-size: 20px;
  font-weight: 800;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.markdown-wrapper {
  max-width: 920px;
  margin: 0 auto;
  padding: 38px 52px 64px;
}

.article-header {
  padding-bottom: 22px;
  margin-bottom: 26px;
  border-bottom: 1px solid var(--border-color);
}

.article-title {
  margin: 0 0 12px;
  color: var(--text-main);
  font-size: 28px;
  font-weight: 800;
  line-height: 1.35;
  text-wrap: balance;
}

.meta-info {
  color: var(--text-light);
  font-size: 13px;
  font-weight: 600;
}

:deep(.markdown-body) {
  color: var(--text-regular);
  font-size: 16px;
  line-height: 1.85;
  text-wrap: pretty;
}

:deep(.markdown-body > h1:first-of-type) {
  display: none;
}

:deep(.markdown-body > *:first-child) {
  margin-top: 0;
}

:deep(.markdown-body h1),
:deep(.markdown-body h2),
:deep(.markdown-body h3) {
  color: var(--text-main);
  font-weight: 800;
  line-height: 1.35;
}

:deep(.markdown-body h2) {
  margin: 34px 0 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
  font-size: 22px;
}

:deep(.markdown-body h3) {
  margin: 28px 0 12px;
  font-size: 18px;
}

:deep(.markdown-body p) {
  margin: 0 0 16px;
}

:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  padding-left: 22px;
  margin: 0 0 18px;
}

:deep(.markdown-body li + li) {
  margin-top: 6px;
}

:deep(.markdown-body blockquote) {
  margin: 20px 0;
  padding: 14px 16px;
  border-radius: 6px;
  background: #F8FAFC;
  color: #475569;
  box-shadow: inset 3px 0 0 #BFDBFE;
}

:deep(.markdown-body pre) {
  margin: 20px 0;
  padding: 16px;
  border-radius: 8px;
  overflow: auto;
  background: #111827 !important;
  color: #F8FAFC;
}

:deep(.markdown-body code) {
  padding: 0.18em 0.4em;
  border-radius: 5px;
  background: #F1F5F9;
  color: #1E3A8A;
  font-family: "SF Mono", Consolas, monospace;
  font-size: 0.92em;
}

:deep(.markdown-body pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}

:deep(.markdown-body img) {
  max-width: 100%;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

:deep(.markdown-body a) {
  color: var(--primary-color);
  font-weight: 700;
  text-decoration: none;
}

:deep(.markdown-body a:hover) {
  color: var(--primary-hover);
  text-decoration: underline;
}

@media (max-width: 1200px) {
  .read-layout {
    width: 95%;
  }
}

@media (max-width: 860px) {
  .read-page {
    padding: 14px 12px 28px;
  }

  .read-layout {
    width: 100%;
    flex-direction: column;
    min-height: auto;
  }

  .sidebar {
    width: 100%;
    max-height: 320px;
  }

  .content-area {
    overflow: visible;
  }

  .markdown-wrapper {
    padding: 28px 22px 42px;
  }

  .article-title {
    font-size: 24px;
  }
}
</style>
