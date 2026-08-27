<template>
  <div class="edu-app">
    <main class="edu-main">
      <div class="page-shell">
        <!-- 顶部说明 -->
        <section class="page-hero glass-panel">
          <div class="hero-copy">
            <span class="hero-tag"><code-outlined /> 编程实践</span>
            <h1 class="hero-title">编程练习</h1>
            <p class="hero-desc">动手写代码，在实践中成长，多语言支持，提交即时评测。</p>
          </div>
        </section>

        <!-- 题目清单 -->
        <section class="content-section glass-panel">
          <div class="toolbar-card">
            <div class="filter-search">
              <search-outlined class="filter-search-icon" />
              <input
                v-model="filter.keyword"
                type="text"
                placeholder="搜索题目标题..."
                class="filter-search-input"
              />
              <button v-if="filter.keyword" class="filter-search-clear" @click="filter.keyword = ''">
                ×
              </button>
            </div>

            <div class="filter-selects">
              <a-select v-model:value="filter.difficulty" style="width: 120px" placeholder="难度" allow-clear>
                <a-select-option value="easy">简单</a-select-option>
                <a-select-option value="medium">中等</a-select-option>
                <a-select-option value="hard">困难</a-select-option>
              </a-select>
              <a-select v-model:value="filter.language" style="width: 140px" placeholder="支持语言" allow-clear>
                <a-select-option value="java">Java</a-select-option>
                <a-select-option value="python">Python</a-select-option>
                <a-select-option value="cpp">C++</a-select-option>
                <a-select-option value="javascript">JavaScript</a-select-option>
              </a-select>
              <a-select v-model:value="filter.status" style="width: 140px" placeholder="完成状态" allow-clear>
                <a-select-option value="solved">已通过</a-select-option>
                <a-select-option value="attempted">练习中</a-select-option>
                <a-select-option value="untouched">未尝试</a-select-option>
              </a-select>
              <a-select v-model:value="filter.sort" style="width: 160px">
                <a-select-option value="newest">最新优先</a-select-option>
                <a-select-option value="difficultyAsc">难度由易到难</a-select-option>
                <a-select-option value="difficultyDesc">难度由难到易</a-select-option>
                <a-select-option value="scoreDesc">我的得分最高优先</a-select-option>
              </a-select>
            </div>
          </div>

          <a-spin :spinning="loading" wrapper-class-name="content-spin">
            <div class="problem-list" v-if="pagedList.length > 0">
              <div
                v-for="item in pagedList"
                :key="item.id"
                class="problem-row"
                @click="goToProblem(item)"
                :class="{ 'row-expired': item.deadline && isDeadlinePassed(item.deadline) }"
              >
                <div class="card-head">
                  <span class="card-title">{{ item.title }}</span>
                  <a-tag :color="difficultyColor(item.difficulty)" class="diff-tag">
                    {{ difficultyLabel(item.difficulty) }}
                  </a-tag>
                </div>

                <div class="card-desc">{{ truncate(stripMd(item.description), 88) }}</div>

                <div class="card-footer">
                  <div class="card-status">
                    <span v-for="lang in (item.languages || [])" :key="lang" class="lang-chip">
                      {{ lang }}
                    </span>
                    <template v-if="item.myBestScore != null">
                      <span class="score-chip" :class="item.myBestScore >= 60 ? 'pass' : 'warn'">
                        <check-circle-filled v-if="item.myBestScore >= 60" />
                        <clock-circle-filled v-else />
                        最高 {{ item.myBestScore }} 分
                      </span>
                      <span class="attempt-text">· {{ item.myAttemptCount }} 次提交</span>
                    </template>
                    <span v-else class="untouched-chip">未尝试</span>
                  </div>

                  <span
                    v-if="item.deadline"
                    class="deadline-chip"
                    :class="{
                      expired: isDeadlinePassed(item.deadline),
                      soon: !isDeadlinePassed(item.deadline) && isDeadlineSoon(item.deadline)
                    }"
                  >
                    <clock-circle-outlined />
                    {{ isDeadlinePassed(item.deadline) ? '已截止' : '截止 ' + formatDeadline(item.deadline) }}
                  </span>
                </div>
              </div>
            </div>

            <div v-else-if="!loading" class="state-box">
              <div class="state-icon-circle">
                <code-outlined />
              </div>
              <p class="state-text">
                {{ problemList.length === 0 ? '暂无编程练习题目' : '没有匹配的题目' }}
              </p>
              <button v-if="hasFilter" class="btn-retry" @click="resetFilter">重置筛选</button>
            </div>

            <div class="pagination-area" v-if="displayList.length > 0">
              <a-pagination
                v-model:current="currentPage"
                v-model:pageSize="pageSize"
                :total="displayList.length"
                :showSizeChanger="false"
                show-less-items
              />
            </div>
          </a-spin>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CheckCircleFilled, ClockCircleFilled, CodeOutlined,
  ClockCircleOutlined, SearchOutlined
} from '@ant-design/icons-vue'
import { getStudentProblemList } from '@/api/coding'
import dayjs from 'dayjs'

const router = useRouter()
const problemList = ref<any[]>([])
const loading = ref(false)

const currentPage = ref(1)
const pageSize = ref(6) // 两行三列 = 6 个/页

const filter = ref({
  keyword: '',
  difficulty: undefined as string | undefined,
  language: undefined as string | undefined,
  status: undefined as 'solved' | 'attempted' | 'untouched' | undefined,
  sort: 'newest' as 'newest' | 'difficultyAsc' | 'difficultyDesc' | 'scoreDesc'
})

// 任何筛选/排序变更后回到第一页
watch(filter, () => { currentPage.value = 1 }, { deep: true })

const statusOf = (p: any): 'solved' | 'attempted' | 'untouched' => {
  if (p.myBestScore == null) return 'untouched'
  if (p.myBestScore >= 60) return 'solved'
  return 'attempted'
}

const difficultyWeight = (d: string) => d === 'easy' ? 1 : d === 'medium' ? 2 : d === 'hard' ? 3 : 0

const hasFilter = computed(() => {
  return !!(filter.value.keyword || filter.value.difficulty || filter.value.language || filter.value.status)
})

const resetFilter = () => {
  filter.value.keyword = ''
  filter.value.difficulty = undefined
  filter.value.language = undefined
  filter.value.status = undefined
}

const displayList = computed(() => {
  const kw = (filter.value.keyword || '').trim().toLowerCase()
  let list = problemList.value.filter(p => {
    if (filter.value.difficulty && p.difficulty !== filter.value.difficulty) return false
    if (filter.value.language && !(p.languages || []).includes(filter.value.language)) return false
    if (filter.value.status && statusOf(p) !== filter.value.status) return false
    if (kw && !(p.title || '').toLowerCase().includes(kw)) return false
    return true
  })

  const sorted = [...list]
  switch (filter.value.sort) {
    case 'difficultyAsc':
      sorted.sort((a, b) => difficultyWeight(a.difficulty) - difficultyWeight(b.difficulty))
      break
    case 'difficultyDesc':
      sorted.sort((a, b) => difficultyWeight(b.difficulty) - difficultyWeight(a.difficulty))
      break
    case 'scoreDesc':
      sorted.sort((a, b) => (b.myBestScore ?? -1) - (a.myBestScore ?? -1))
      break
    case 'newest':
    default:
      sorted.sort((a, b) => (b.id || 0) - (a.id || 0))
  }
  return sorted
})

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return displayList.value.slice(start, start + pageSize.value)
})

const difficultyColor = (d: string) => {
  if (d === 'easy') return 'green'
  if (d === 'hard') return 'red'
  return 'orange'
}

const difficultyLabel = (d: string) => {
  if (d === 'easy') return '简单'
  if (d === 'hard') return '困难'
  return '中等'
}

const stripMd = (text: string) => (text || '').replace(/[#*`>\-!\[\]()]/g, '').replace(/\n+/g, ' ')

const truncate = (text: string, len: number) => {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '...' : text
}

const goToProblem = (item: any) => {
  if (item.deadline && isDeadlinePassed(item.deadline)) {
    message.warning('该练习已截止，无法进入')
    return
  }
  router.push(`/student/coding/${item.id}`)
}

const formatDeadline = (dl: string) => dayjs(dl).format('MM-DD HH:mm')
const isDeadlinePassed = (dl: string) => dayjs(dl).isBefore(dayjs())
const isDeadlineSoon = (dl: string) => !isDeadlinePassed(dl) && dayjs(dl).isBefore(dayjs().add(1, 'day'))

onMounted(async () => {
  loading.value = true
  try {
    const data = await getStudentProblemList()
    problemList.value = Array.isArray(data) ? data : []
  } catch (e) {
    problemList.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.edu-app {
  height: calc(100vh - 70px);
  overflow: hidden;
  background: linear-gradient(120deg, #ffffff 0%, #f1f5f9 100%);
  color: #0f172a;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
  display: flex;
  flex-direction: column;
}

.glass-panel {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}

.edu-main {
  flex: 1;
  min-height: 0;
  width: 75%;
  max-width: 1600px;
  min-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

.page-hero {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  padding: 18px 22px;
  border-radius: 5px;
}

.hero-copy { min-width: 0; }

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  margin-bottom: 6px;
  padding: 4px 10px;
  border-radius: 5px;
  background: rgba(37, 99, 235, 0.1);
  color: #2563EB;
  font-size: 13px;
  font-weight: 800;
}

.hero-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}

.hero-desc {
  margin: 6px 0 0;
  max-width: 800px;
  font-size: 15px;
  line-height: 1.65;
  color: #475569;
}

.toolbar-card {
  flex-shrink: 0;
  padding: 14px 0;
  background: transparent;
  border: none;
  border-bottom: 1px solid #e7ecf3;
  box-shadow: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-search {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 240px;
  max-width: 400px;
  height: 42px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #ffffff;
  padding: 0 34px 0 40px;
  transition: all 0.2s;
}
.filter-search:focus-within {
  border-color: #2563eb;
  background: #FFF;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}
.filter-search-icon {
  position: absolute;
  left: 14px;
  color: #64748b;
  font-size: 16px;
}
.filter-search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  background: transparent;
  font-size: 15px;
  color: #0f172a;
  outline: none;
}
.filter-search-input::placeholder { color: #64748b; }
.filter-search-clear {
  position: absolute;
  right: 9px;
  width: 22px;
  height: 22px;
  border: none;
  background: #e2e8f0;
  color: #334155;
  border-radius: 50%;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.filter-search-clear:hover { background: #cbd5e1; color: #0f172a; }

.filter-selects {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-card :deep(.ant-select-selector) {
  border-radius: 6px !important;
  height: 42px !important;
  border-color: #cbd5e1 !important;
  background: #fff !important;
  font-size: 15px !important;
  transition: all 0.2s !important;
}
.toolbar-card :deep(.ant-select-selector:hover) {
  border-color: #93c5fd !important;
}
.toolbar-card :deep(.ant-select-focused .ant-select-selector) {
  border-color: #2563EB !important;
  background: #FFF !important;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12) !important;
}
.toolbar-card :deep(.ant-select-selection-item),
.toolbar-card :deep(.ant-select-selection-placeholder) {
  line-height: 40px !important;
  color: #0f172a !important;
  font-size: 15px !important;
  font-weight: 600 !important;
}
.toolbar-card :deep(.ant-select-selection-placeholder) {
  color: #475569 !important;
}
.toolbar-card :deep(.ant-select-arrow) {
  color: #475569 !important;
}

.content-section {
  flex: 1;
  min-height: 0;
  border-radius: 5px;
  padding: 0 24px 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-section :deep(.ant-spin-nested-loading),
.content-section :deep(.ant-spin-container) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.problem-list {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(430px, 1fr));
  align-content: start;
  gap: 14px;
  padding: 16px 0 4px;
  overflow-y: auto;
}

.problem-row {
  background: #fbfdff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  box-shadow: none;
  padding: 18px 18px 16px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 152px;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
  position: relative;
}

.problem-row:hover {
  background: #ffffff;
  border-color: #bfdbfe;
  transform: translateY(-1px);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}
.card-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}
.diff-tag {
  border-radius: 4px !important;
  font-size: 14px !important;
  padding: 0 8px !important;
  line-height: 22px !important;
  font-weight: 700;
  margin: 0 !important;
  flex-shrink: 0;
}

.card-desc {
  font-size: 15px;
  line-height: 1.65;
  color: #475569;
  min-height: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.lang-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 14px;
  color: #1D4ED8;
  background: transparent;
  border: 1px solid #dbeafe;
  border-radius: 4px;
  line-height: 20px;
  font-weight: 700;
}

.card-footer {
  margin-top: auto;
  padding-top: 0;
  border-top: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  min-width: 0;
}
.card-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.score-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 9px;
  font-size: 14px;
  font-weight: 700;
  border-radius: 4px;
  line-height: 21px;
}
.score-chip.pass {
  background: #ECFDF5;
  color: #10B981;
  border: 1px solid #A7F3D0;
}
.score-chip.warn {
  background: #FFFBEB;
  color: #F59E0B;
  border: 1px solid #FCD34D;
}

.untouched-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  font-size: 14px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  line-height: 21px;
}

.attempt-text {
  font-size: 14px;
  color: #64748b;
}

.deadline-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 9px;
  font-size: 13px;
  font-weight: 700;
  color: #2563EB;
  background: transparent;
  border: 1px solid #DBEAFE;
  border-radius: 4px;
  line-height: 20px;
  flex-shrink: 0;
}
.deadline-chip.soon {
  color: #F59E0B;
  background: #FFFBEB;
  border-color: #FCD34D;
}
.deadline-chip.expired {
  color: #EF4444;
  background: #FEF2F2;
  border-color: #FECACA;
}

.state-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
}
.state-icon-circle {
  width: auto;
  height: auto;
  border-radius: 0;
  background: transparent;
  color: #2563EB;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.state-text {
  font-size: 16px;
  color: #475569;
  font-weight: 700;
}
.btn-retry {
  height: 38px;
  padding: 0 20px;
  border: none;
  border-radius: 6px;
  background: #2563EB;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
.btn-retry:hover { background: #1D4ED8; }

.pagination-area {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
  margin-top: 14px;
  border-top: 1px solid #e7ecf3;
}

.pagination-area :deep(.ant-pagination-item),
.pagination-area :deep(.ant-pagination-prev),
.pagination-area :deep(.ant-pagination-next),
.pagination-area :deep(.ant-pagination-jump-prev),
.pagination-area :deep(.ant-pagination-jump-next) {
  min-width: 34px;
  height: 34px;
  line-height: 32px;
  border-radius: 6px;
  background: #FFF;
  border: 1px solid #E7ECF3;
  margin: 0 4px;
  transition: all 0.2s;
}
.pagination-area :deep(.ant-pagination-item a) {
  color: #344054;
  font-weight: 500;
}
.pagination-area :deep(.ant-pagination-item:hover),
.pagination-area :deep(.ant-pagination-prev:hover:not(.ant-pagination-disabled)),
.pagination-area :deep(.ant-pagination-next:hover:not(.ant-pagination-disabled)) {
  border-color: #2563EB !important;
  background: #F8FBFF !important;
}
.pagination-area :deep(.ant-pagination-item:hover a) {
  color: #2563EB !important;
}
.pagination-area :deep(.ant-pagination-item-active),
.pagination-area :deep(.ant-pagination-item-active:focus),
.pagination-area :deep(.ant-pagination-item-active:hover) {
  background: #2563EB !important;
  border-color: #2563EB !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2) !important;
  outline: none !important;
}
.pagination-area :deep(.ant-pagination-item-active a),
.pagination-area :deep(.ant-pagination-item-active:hover a),
.pagination-area :deep(.ant-pagination-item-active:focus a) {
  color: #FFF !important;
}

.problem-row.row-expired {
  cursor: not-allowed;
  opacity: 0.55;
  filter: grayscale(0.5);
}
.problem-row.row-expired:hover {
  background: #fbfdff;
  border-color: #e8eef7;
  transform: none;
}

@media (max-width: 1200px) {
  .edu-main {
    width: 92%;
    min-width: 0;
  }
  .page-hero {
    align-items: flex-start;
  }
  .problem-list { grid-template-columns: 1fr; }
  .card-footer {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .hero-title { font-size: 22px; }
  .toolbar-card {
    flex-direction: column;
    align-items: stretch;
  }
  .filter-search { max-width: none; }
  .filter-selects { width: 100%; }
  .problem-list { grid-template-columns: 1fr; }
  .problem-row { padding: 16px 14px; }
  .card-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
