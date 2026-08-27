<template>
  <div class="monitor-page modern-page">
    <div class="page-header">
      <div class="title-group">
        <h2><monitor-outlined class="title-icon" /> 学情监控与任务回收站</h2>
        <p class="subtitle">基于作业与考试数据追踪完成情况、成绩表现与历史诊断报告。</p>
      </div>
    </div>

    <div class="filter-dashboard glass-panel">
      <div class="filter-group">
        <div class="filter-item">
          <span class="label"><environment-outlined class="f-icon" /> 目标班级：</span>
          <a-select v-model:value="selectedClass" style="width: 140px" size="large">
            <a-select-option value="all">所有班级</a-select-option>
            <a-select-option
              v-for="c in classOptions"
              :key="String(c)"
              :value="String(c)"
            >
              班级 {{ c }}
            </a-select-option>
          </a-select>
        </div>

        <div class="filter-item">
          <span class="label"><calendar-outlined class="f-icon" /> 发布日期：</span>
          <a-select v-model:value="selectedDate" style="width: 140px" size="large">
            <a-select-option value="all">所有时间段</a-select-option>
            <a-select-option
              v-for="d in dateOptions"
              :key="d"
              :value="d"
            >
              {{ d }}
            </a-select-option>
          </a-select>
        </div>

        <div class="filter-item">
          <span class="label"><book-outlined class="f-icon" /> 类型：</span>
          <a-select v-model:value="selectedType" style="width: 140px" size="large">
            <a-select-option value="all">全部类型</a-select-option>
            <a-select-option value="homework">作业</a-select-option>
            <a-select-option value="exam">考试</a-select-option>
          </a-select>
        </div>
      </div>

      <div class="action-group" style="display: flex; gap: 12px;">
        <a-button size="large" @click="openHistoryDrawer">
          <template #icon><history-outlined /></template>
          历史记录
        </a-button>

        <a-tooltip :title="canGenerateReport ? '' : '当前筛选维度下没有已批改的真实成绩样本'">
          <a-button
            type="primary"
            size="large"
            class="ai-report-btn"
            @click="generateClassReport"
            :loading="isAnalyzing"
            :disabled="!canGenerateReport"
          >
            <template #icon>
              <thunderbolt-outlined v-if="!isAnalyzing" />
            </template>
            <span v-if="!isAnalyzing">生成诊断报告</span>
            <span v-else>正在生成并保存报告...</span>
          </a-button>
        </a-tooltip>
      </div>
    </div>

    <div class="main-scroll-area">
      <div v-if="loading" class="empty-box glass-panel">
        <div class="empty-content">
          <div class="empty-icon spin-soft"><bar-chart-outlined /></div>
          <h3>正在加载学情数据</h3>
          <p>正在汇总教师已发布作业与学生最新作答结果…</p>
        </div>
      </div>

      <div v-else-if="filteredTasks.length > 0" class="task-grid">
        <div
          v-for="task in filteredTasks"
          :key="task.assignmentId"
          class="task-card glass-panel"
        >
          <div class="card-header">
            <div class="course-name">
              <span>班级 {{ task.classId ?? '-' }}</span>
            </div>
            <div class="card-header-right">
              <div class="type-badge" :class="task.assignmentType">
                {{ task.assignmentType === 'exam' ? '考试' : '作业' }}
              </div>
              <div class="status-badge" :class="displayStatusClass(task)">
                <check-circle-outlined
                  v-if="displayStatusClass(task) === 'completed'"
                  class="s-icon"
                />
                <clock-circle-outlined v-else class="s-icon" />
                {{ displayStatusText(task) }}
              </div>
              <a-popconfirm
                :title="buildDeleteTaskWarning(task)"
                ok-text="确定删除"
                cancel-text="取消"
                ok-type="danger"
                placement="topRight"
                @confirm="deleteTask(task)"
              >
                <a-button
                  type="text"
                  size="small"
                  class="delete-task-btn"
                  :loading="deletingTaskId === task.assignmentId"
                  title="删除这份作业"
                >
                  <delete-outlined />
                </a-button>
              </a-popconfirm>
            </div>
          </div>

          <div class="card-body">
            <h3 class="title">{{ task.title || '未命名作业' }}</h3>

            <div class="meta-info">
              <span>发布时间：{{ formatDateTime(task.publishTime) }}</span>
              <span>截止时间：{{ formatDateTime(task.deadline) }}</span>
              <span v-if="task.assignmentType === 'exam' && task.durationMinutes" class="exam-duration">
                考试时长：{{ task.durationMinutes }} 分钟
              </span>
            </div>

            <div class="extra-meta">
              <span>题目数：{{ task.questionCount ?? '--' }}</span>
              <span>总分：{{ task.totalScore ?? '--' }}</span>
            </div>

            <div class="stats-grid">
              <div class="stat-item">
                <span class="stat-label">完成率</span>
                <span class="stat-value">{{ formatPercent(task.completionRate) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">平均分</span>
                <span class="stat-value">{{ formatScore(task.avgScore) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">已完成</span>
                <span class="stat-value">{{ task.completedCount ?? 0 }}/{{ task.studentTotal ?? 0 }}</span>
              </div>
              <div class="stat-item danger-soft">
                <span class="stat-label">未完成</span>
                <span class="stat-value">{{ task.pendingCount ?? 0 }}</span>
              </div>
            </div>
          </div>

          <div class="card-footer">
            <div class="risk-tip">
              <span class="risk-label">低分风险：</span>
              <span class="risk-value">{{ task.lowScoreCount ?? 0 }} 人</span>
            </div>

            <a-button
              type="primary"
              ghost
              size="small"
              @click="viewDetail(task)"
              class="view-btn"
            >
              查看详情
            </a-button>
          </div>
        </div>
      </div>

      <div v-else class="empty-box glass-panel">
        <div class="empty-content">
          <div class="empty-icon"><inbox-outlined /></div>
          <h3 v-if="tasks.length === 0">暂无作业或考试分发记录</h3>
          <h3 v-else>当前筛选维度下没有数据</h3>
          <p v-if="tasks.length === 0">请先在教师端发布作业或考试，等待学生完成真实作答后再分析。</p>
          <p v-else>您可以尝试切换班级、试卷或发布日期。</p>
        </div>
      </div>
    </div>

    <!-- ===== 生成诊断报告 - 前置配置弹窗 ===== -->
    <a-modal
      v-model:open="reportConfigVisible"
      title="配置诊断报告参数"
      :footer="null"
      width="460px"
      centered
      :maskClosable="false"
    >
      <div class="report-config-form">

        <div class="config-field">
          <label class="config-label"><span class="required-star">*</span> 目标班级</label>
          <a-select
            v-model:value="reportConfigForm.classId"
            size="large"
            style="width: 100%"
            placeholder="请选择具体班级"
            :status="reportConfigErrors.classId ? 'error' : ''"
          >
            <a-select-option
              v-for="c in classOptions"
              :key="String(c)"
              :value="String(c)"
            >
              班级 {{ c }}
            </a-select-option>
          </a-select>
          <span v-if="reportConfigErrors.classId" class="config-error">{{ reportConfigErrors.classId }}</span>
        </div>

        <div class="config-field">
          <label class="config-label"><span class="required-star">*</span> 试卷/习题</label>
          <a-select
            v-model:value="reportConfigForm.quizResourceId"
            size="large"
            style="width: 100%"
            placeholder="请选择要分析的试卷或习题"
            :disabled="!reportConfigForm.classId"
          >
            <a-select-option
              v-for="q in reportConfigQuizOptions"
              :key="String(q.id)"
              :value="String(q.id)"
            >
              {{ q.title }}
            </a-select-option>
          </a-select>
          <span v-if="reportConfigForm.classId && reportConfigQuizOptions.length === 0" class="config-hint">
            该班级下暂无匹配的试卷或习题
          </span>
        </div>

        <div class="config-field">
          <label class="config-label">
            <span class="required-star">*</span> 发布日期
            <span v-if="isDateLockedByQuiz" class="config-hint">（已根据所选试卷自动锁定）</span>
          </label>
          <a-select
            v-model:value="reportConfigForm.publishDate"
            size="large"
            style="width: 100%"
            :placeholder="!reportConfigForm.classId ? '请先选择班级' : (isDateLockedByQuiz ? '' : '请选择具体日期')"
            :disabled="isDateLockedByQuiz || !reportConfigForm.classId"
            :status="reportConfigErrors.publishDate ? 'error' : ''"
          >
            <a-select-option
              v-for="d in reportConfigDateOptions"
              :key="d"
              :value="d"
            >
              {{ d }}
            </a-select-option>
          </a-select>
          <span v-if="reportConfigErrors.publishDate" class="config-error">{{ reportConfigErrors.publishDate }}</span>
        </div>

        <div class="config-form-footer">
          <a-button size="large" @click="reportConfigVisible = false">取消</a-button>
          <a-button type="primary" size="large" class="confirm-report-btn" @click="confirmAndGenerateReport">
            <thunderbolt-outlined /> 开始生成报告
          </a-button>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:open="detailVisible"
      width="1100px"
      :footer="null"
      centered
      class="preview-modal teacher-wide-modal"
    >
      <template #title>
        <div class="modal-custom-title">
          <file-text-outlined class="m-icon" />
          <span>{{ currentTask?.title || '作业详情' }}</span>
        </div>
      </template>

      <div class="detail-container">
        <div class="summary-bar" v-if="currentTask">
          <div class="summary-chip">班级 {{ currentTask.classId ?? '-' }}</div>
          <div class="summary-chip">发布时间 {{ formatDateTime(currentTask.publishTime) }}</div>
          <div class="summary-chip">截止时间 {{ formatDateTime(currentTask.deadline) }}</div>
        </div>

        <a-table
          :columns="detailColumns"
          :data-source="currentStudents"
          :loading="detailLoading"
          :pagination="{ pageSize: 8, showSizeChanger: false }"
          :rowKey="(record: MonitorStudent) => record.studentId"
          size="middle"
          class="student-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'submitStatus'">
              <span class="table-status" :class="submitStatusClass(record.submitStatus)">
                {{ submitStatusText(record.submitStatus) }}
              </span>
            </template>

            <template v-else-if="column.dataIndex === 'submitTime'">
              {{ formatDateTime(record.submitTime) }}
            </template>

            <template v-else-if="column.dataIndex === 'totalScore'">
              {{ record.totalScore ?? '--' }}
            </template>

            <template v-else-if="column.dataIndex === 'correctCount'">
              {{ record.correctCount ?? '--' }}
            </template>

            <template v-else-if="column.dataIndex === 'wrongCount'">
              {{ record.wrongCount ?? '--' }}
            </template>

            <template v-else-if="column.key === 'action'">
              <a-button
                v-if="(record.submitStatus === 'completed' || record.submitStatus === 'submitted') && record.submissionId"
                type="link"
                size="small"
                @click="viewStudentSubmissionReport(record)"
              >
                查看结果
              </a-button>
              <span v-else class="action-disabled">--</span>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>

    <a-modal
      v-model:open="submissionReportVisible"
      :title="`${activeSubmissionStudentName || '学生'} - 作答详情`"
      width="1100px"
      :footer="null"
      centered
      destroy-on-close
      class="submission-report-modal teacher-wide-modal"
    >
      <div class="submission-report-wrapper">
        <a-spin :spinning="submissionReportLoading">
          <HomeworkReportPanel
            :report="activeSubmissionReport"
            :student-name="activeSubmissionStudentName"
            :show-student-name="true"
            role="teacher"
          />
        </a-spin>
      </div>
    </a-modal>

    <a-modal
      v-model:open="reportVisible"
      width="1100px"
      :footer="null"
      centered
      class="teacher-wide-modal"
    >
      <template #title>
        <div class="modal-custom-title report-title">
          <bar-chart-outlined class="m-icon" />
          <span>{{ activeReport?.reportTitle || `${reportTargetName} - 综合学情诊断` }}</span>
        </div>
      </template>

      <div class="report-wrapper">
        <div v-if="isAnalyzing" class="analyzing-state">
          <div class="radar-spinner"></div>
          <p>正在读取真实提交数据、聚合成绩结果并生成可落库报告…</p>
        </div>

        <template v-else>
          <!-- ===== 顶部元信息条 ===== -->
          <div class="report-meta-bar" v-if="activeReport">
            <span class="rmb-chip"><strong>报告时间</strong> {{ formatDateTime(activeReport.createTime) }}</span>
            <span class="rmb-chip"><strong>统计作业</strong> {{ activeReport.assignmentCount ?? 0 }} 份</span>
            <span class="rmb-chip"><strong>班级</strong> {{ activeReport.classId ? `班级 ${activeReport.classId}` : '全部' }}</span>
            <span class="rmb-chip"><strong>试卷</strong> {{ activeReport.quizTitle || '全部' }}</span>
          </div>

          <!-- ===== 可视化图表区 ===== -->
          <div class="report-charts-row" v-if="activeReport">

            <!-- Card 1: 完成率环形图 -->
            <div class="rc-card donut-card">
              <div class="rc-label">学生完成率</div>
              <div class="donut-wrap">
                <svg viewBox="0 0 120 120" class="donut-svg">
                  <circle cx="60" cy="60" r="46" fill="none" stroke="#ede9fe" stroke-width="13"/>
                  <circle
                    cx="60" cy="60" r="46"
                    fill="none"
                    :stroke="completionRingColor"
                    stroke-width="13"
                    stroke-linecap="round"
                    :stroke-dasharray="completionRingDash"
                    transform="rotate(-90 60 60)"
                  />
                  <text x="60" y="55" text-anchor="middle" dominant-baseline="middle"
                        font-size="21" font-weight="800" :fill="completionRingColor">
                    {{ Math.round(activeReport.overallCompletionRate ?? 0) }}%
                  </text>
                  <text x="60" y="74" text-anchor="middle" font-size="11" fill="#94a3b8">完成率</text>
                </svg>
              </div>
              <div class="donut-sub-stats">
                <span class="dss-item ok">✓ 已完成 {{ activeReport.completedCount ?? 0 }} 人</span>
                <span class="dss-item warn">✗ 未完成 {{ activeReport.pendingCount ?? 0 }} 人</span>
              </div>
            </div>

            <!-- Card 2: 综合情况进度条 -->
            <div class="rc-card bars-card">
              <div class="rc-label">综合情况</div>

              <div class="bar-row">
                <div class="bar-meta">
                  <span class="bar-name">班级平均分</span>
                  <span class="bar-val" :style="{ color: avgScoreColor }">{{ formatScore(activeReport.overallAvgScore) }} 分</span>
                </div>
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: avgScorePct + '%', background: avgScoreColor }"></div>
                </div>
              </div>

              <div class="bar-row">
                <div class="bar-meta">
                  <span class="bar-name">完成人数</span>
                  <span class="bar-val" style="color:#16a34a">{{ activeReport.completedCount ?? 0 }} / {{ activeReport.studentTotal ?? 0 }} 人</span>
                </div>
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: completedPct + '%', background: '#22c55e' }"></div>
                </div>
              </div>

              <div class="bar-row">
                <div class="bar-meta">
                  <span class="bar-name">低分风险人数</span>
                  <span class="bar-val" :style="{ color: riskBarColor }">{{ activeReport.lowScoreCount ?? 0 }} 人</span>
                </div>
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: lowScorePct + '%', background: riskBarColor }"></div>
                </div>
              </div>

              <div class="bar-row">
                <div class="bar-meta">
                  <span class="bar-name">统计作业数</span>
                  <span class="bar-val">{{ activeReport.assignmentCount ?? 0 }} 份</span>
                </div>
                <div class="assignments-dots">
                  <span
                    v-for="i in Math.min(activeReport.assignmentCount ?? 0, 24)"
                    :key="i"
                    class="dot-assignment"
                  ></span>
                  <span v-if="(activeReport.assignmentCount ?? 0) > 24" class="dot-more">
                    +{{ (activeReport.assignmentCount ?? 0) - 24 }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Card 3: 低分风险预警环 -->
            <div class="rc-card risk-card">
              <div class="rc-label">低分风险预警</div>
              <div class="risk-donut-wrap">
                <svg viewBox="0 0 100 100" class="risk-svg">
                  <circle cx="50" cy="50" r="37" fill="none" stroke="#fef2f2" stroke-width="10"/>
                  <circle
                    cx="50" cy="50" r="37"
                    fill="none"
                    :stroke="riskRingColor"
                    stroke-width="10"
                    stroke-linecap="round"
                    :stroke-dasharray="riskRingDash"
                    transform="rotate(-90 50 50)"
                  />
                  <text x="50" y="46" text-anchor="middle" dominant-baseline="middle"
                        font-size="20" font-weight="800" :fill="riskRingColor">
                    {{ activeReport.lowScoreCount ?? 0 }}
                  </text>
                  <text x="50" y="63" text-anchor="middle" font-size="10" fill="#94a3b8">人低分</text>
                </svg>
              </div>
              <div class="risk-verdict" :class="riskLevelClass">{{ riskLevelText }}</div>
              <div class="risk-pct">占总人数 {{ riskPercentText }}</div>
            </div>

          </div>

          <!-- ===== AI 诊断文字报告 ===== -->
          <div class="report-text-section">
            <div class="rts-header">
              <bar-chart-outlined class="rts-icon" />
              <span>AI 诊断分析</span>
            </div>
            <div
              class="markdown-render report-content doc-style"
              v-html="renderMd(activeReport?.reportMarkdown || '')"
            ></div>
          </div>
        </template>
      </div>
    </a-modal>

    <a-modal
      v-model:open="historyDrawerVisible"
      title="最近生成报告"
      width="1100px"
      :footer="null"
      centered
      class="history-modal teacher-wide-modal"
    >
      <div class="history-modal-body">
        <div class="history-filter-bar">
          <a-select
            v-model:value="historyFilterClass"
            style="width: 120px"
            placeholder="全部班级"
            allow-clear
          >
            <a-select-option value="">全部班级</a-select-option>
            <a-select-option
              v-for="c in historyClassOptions"
              :key="String(c)"
              :value="String(c)"
            >班级 {{ c }}</a-select-option>
          </a-select>

          <a-select
            v-model:value="historyFilterQuiz"
            style="flex: 1"
            placeholder="全部试卷"
            allow-clear
          >
            <a-select-option value="">全部试卷</a-select-option>
            <a-select-option
              v-for="q in historyQuizOptions"
              :key="String(q.id)"
              :value="String(q.id)"
            >{{ q.title }}</a-select-option>
          </a-select>

          <a-button @click="historyFilterClass = ''; historyFilterQuiz = ''">重置</a-button>
        </div>

        <div class="history-filter-summary">
          共 <strong>{{ filteredHistory.length }}</strong> 条
        </div>

        <div v-if="historyLoading" class="history-empty">正在读取历史报告...</div>

        <div v-else-if="filteredHistory.length === 0" class="history-empty">
          {{ reportHistory.length === 0 ? '暂无历史报告，先生成一份看看。' : '当前筛选条件下无匹配报告' }}
        </div>

        <div v-else class="drawer-history-list">
          <div
            v-for="item in filteredHistory"
            :key="item.reportId"
            class="history-card"
            @click="openHistoryReport(item)"
          >
            <div class="history-top">
              <div class="history-title">
                {{ item.reportTitle || (item.quizTitle ? `《${item.quizTitle}》学情诊断报告` : '综合学情诊断报告') }}
              </div>
              <div class="history-card-actions">
                <a-popconfirm
                  title="确定删除这份报告？"
                  ok-text="删除"
                  cancel-text="取消"
                  ok-type="danger"
                  @confirm.stop="deleteReport(item.reportId)"
                  @click.stop
                >
                  <a-button
                    type="text"
                    size="small"
                    class="delete-report-btn"
                    :loading="deletingReportId === item.reportId"
                    @click.stop
                  >
                    <delete-outlined />
                  </a-button>
                </a-popconfirm>
              </div>
            </div>

            <div class="history-time-row">
              <clock-circle-outlined /> {{ formatDateTime(item.createTime) }}
            </div>

            <div class="history-meta">
              <span class="meta-tag">{{ item.classId ? `班级 ${item.classId}` : '全部班级' }}</span>
              <span class="meta-tag">{{ item.quizTitle ? `《${item.quizTitle}》` : '全部试卷' }}</span>
              <span class="meta-tag">{{ item.publishDate || '全部日期' }}</span>
              <span class="meta-tag">作业数 {{ item.assignmentCount ?? 0 }}</span>
            </div>

            <div class="history-stats-box">
              <div class="mini-stat">
                <span class="mini-label">完成率</span>
                <span class="mini-value">{{ formatPercent(item.overallCompletionRate) }}</span>
              </div>
              <div class="mini-stat">
                <span class="mini-label">平均分</span>
                <span class="mini-value">{{ formatScore(item.overallAvgScore) }}</span>
              </div>
              <div class="mini-stat danger">
                <span class="mini-label">低分人数</span>
                <span class="mini-value">{{ item.lowScoreCount ?? 0 }}</span>
              </div>
            </div>

            <div class="history-preview-row">
              <span>点击卡片预览完整报告</span>
              <a-button type="link" size="small" @click.stop="openHistoryReport(item)">预览报告</a-button>
            </div>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import { message } from 'ant-design-vue'
import request from '@/utils/request'
import HomeworkReportPanel from '@/components/homework/HomeworkReportPanel.vue'
import {
  MonitorOutlined,
  EnvironmentOutlined,
  CalendarOutlined,
  BookOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  InboxOutlined,
  BarChartOutlined,
  FileTextOutlined,
  HistoryOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'

type RawDate = string | number | Date | null | undefined

interface MonitorTask {
  assignmentId: number
  assignmentType: string
  title: string
  classId: number | null
  courseId: number | null
  quizResourceId: number | null
  quizTitle: string | null
  publishTime: RawDate
  deadline: RawDate
  questionCount: number | null
  totalScore: number | null
  studentTotal: number
  completedCount: number
  pendingCount: number
  completionRate: number
  avgScore: number
  lowScoreCount: number
  status: string
  durationMinutes: number | null
}

interface MonitorStudent {
  studentId: number
  studentName: string
  submitStatus: string
  submitTime: RawDate
  totalScore: number | null
  correctCount: number | null
  wrongCount: number | null
  submissionId: number | null
}

interface MonitorReport {
  reportId: number
  reportTitle: string
  classId: number | null
  publishDate: string | null
  quizResourceId: number | null
  quizTitle: string | null
  reportMarkdown?: string | null
  assignmentIds?: number[]
  assignmentCount?: number
  studentTotal?: number
  completedCount?: number
  pendingCount?: number
  overallCompletionRate?: number
  overallAvgScore?: number
  lowScoreCount?: number
  createTime?: RawDate
}

interface ReportRequest {
  classId: number | null
  publishDate: string | null
  quizResourceId: number | null
  assignmentType: string | null
}

interface SubmissionDetailItem {
  id?: number
  questionNo?: string
  questionType?: string
  stemSnapshot?: string
  standardAnswer?: string
  studentAnswer?: string
  fullScore?: number | null
  score?: number | null
  isCorrect?: number | null
  aiComment?: string
}

interface SubmissionEntity {
  id?: number
  assignmentId?: number
  submitStatus?: string
  totalScore?: number | null
  correctCount?: number | null
  wrongCount?: number | null
  submitTime?: RawDate
  aiReportMarkdown?: string | null
  teacherRemark?: string | null
}

interface SubmissionReportVO {
  submission?: SubmissionEntity
  assignmentTitle?: string
  contentSnapshot?: string
  details?: SubmissionDetailItem[]
  examMode?: boolean
}

const historyDrawerVisible = ref(false)
const historyFilterClass = ref('')
const historyFilterQuiz = ref('')

const deletingReportId = ref<number | null>(null)
const deletingTaskId = ref<number | null>(null)

const historyClassOptions = computed(() => {
  const ids = Array.from(
    new Set(
      reportHistory.value
        .map((r) => r.classId)
        .filter((id): id is number => id !== null && id !== undefined)
    )
  )
  return ids.sort((a, b) => a - b)
})

const historyQuizOptions = computed(() => {
  const map = new Map<number, string>()
  reportHistory.value.forEach((r) => {
    if (r.quizResourceId != null && !map.has(r.quizResourceId)) {
      map.set(r.quizResourceId, r.quizTitle || `试卷 ${r.quizResourceId}`)
    }
  })
  return Array.from(map, ([id, title]) => ({ id, title }))
    .sort((a, b) => a.title.localeCompare(b.title, 'zh-CN'))
})

const filteredHistory = computed(() => {
  return reportHistory.value.filter((item) => {
    // 班级筛选
    if (historyFilterClass.value && historyFilterClass.value !== '') {
      if (String(item.classId ?? '') !== historyFilterClass.value) return false
    }
    // 试卷筛选
    if (historyFilterQuiz.value && historyFilterQuiz.value !== '') {
      if (String(item.quizResourceId ?? '') !== historyFilterQuiz.value) return false
    }
    // 已彻底移除时间段筛选逻辑
    return true
  })
})

const md = new MarkdownIt({ breaks: true, html: true })
const renderMd = (text: string) => md.render(text || '')

const tasks = ref<MonitorTask[]>([])
const loading = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentTask = ref<MonitorTask | null>(null)
const currentStudents = ref<MonitorStudent[]>([])

const reportVisible = ref(false)
const isAnalyzing = ref(false)
const activeReport = ref<MonitorReport | null>(null)

const reportHistory = ref<MonitorReport[]>([])
const historyLoading = ref(false)

const selectedClass = ref('all')
const selectedDate = ref('all')
const selectedType = ref('all')

const submissionReportVisible = ref(false)
const submissionReportLoading = ref(false)
const activeSubmissionReport = ref<SubmissionReportVO | null>(null)
const activeSubmissionStudentName = ref('')

const detailColumns = [
  { title: '学生 ID', dataIndex: 'studentId', key: 'studentId', width: 50, align: 'center' },
  { title: '学生姓名', dataIndex: 'studentName', key: 'studentName', width: 50, align: 'center' },
  { title: '作答状态', dataIndex: 'submitStatus', key: 'submitStatus', width: 60, align: 'center' },
  { title: '提交时间', dataIndex: 'submitTime', key: 'submitTime', width: 100, align: 'center' },
  { title: '总分', dataIndex: 'totalScore', key: 'totalScore', width: 60, align: 'center' },
  { title: '正确题数', dataIndex: 'correctCount', key: 'correctCount', width: 50, align: 'center' },
  { title: '错误题数', dataIndex: 'wrongCount', key: 'wrongCount', width: 50, align: 'center' },
  { title: '操作', dataIndex: 'action', key: 'action', width: 60, align: 'center' },
]

const classOptions = computed(() => {
  const values = Array.from(
    new Set(
      tasks.value
        .map((item) => item.classId)
        .filter((item): item is number => item !== null && item !== undefined)
    )
  )
  return values.sort((a, b) => a - b)
})

const dateOptions = computed(() => {
  const values = Array.from(
    new Set(
      tasks.value
        .map((item) => formatDate(item.publishTime))
        .filter((item) => item !== '--')
    )
  )
  return values.sort((a, b) => (a < b ? 1 : -1))
})

const filteredTasks = computed(() => {
  return tasks.value.filter((task) => {
    const matchClass =
      selectedClass.value === 'all' || String(task.classId ?? '') === selectedClass.value
    const matchDate =
      selectedDate.value === 'all' || formatDate(task.publishTime) === selectedDate.value
    const matchType =
      selectedType.value === 'all' || task.assignmentType === selectedType.value
    return matchClass && matchDate && matchType
  })
})

const canGenerateReport = computed(() => {
  return filteredTasks.value.some((task) => Number(task.completedCount || 0) > 0)
})

const reportTargetName = computed(() => {
  const classText = selectedClass.value === 'all' ? '全部班级' : `班级 ${selectedClass.value}`
  const dateText = selectedDate.value === 'all' ? '全部日期' : selectedDate.value
  const typeText = selectedType.value === 'all' ? '全部类型' : (selectedType.value === 'exam' ? '考试' : '作业')
  return `${classText} / ${typeText} / ${dateText}`
})

// ===== 诊断报告图表计算属性 =====

const completionRingDash = computed(() => {
  const r = 46
  const circumference = 2 * Math.PI * r
  const rate = Math.min(100, Math.max(0, activeReport.value?.overallCompletionRate ?? 0))
  const filled = (rate / 100) * circumference
  return `${filled.toFixed(2)} ${circumference.toFixed(2)}`
})

const completionRingColor = computed(() => {
  const rate = activeReport.value?.overallCompletionRate ?? 0
  if (rate >= 80) return '#22c55e'
  if (rate >= 60) return '#8b5cf6'
  if (rate >= 40) return '#f59e0b'
  return '#ef4444'
})

const completedPct = computed(() => {
  const total = activeReport.value?.studentTotal ?? 0
  if (!total) return 0
  return Math.round(((activeReport.value?.completedCount ?? 0) / total) * 100)
})

const avgScorePct = computed(() => {
  const score = activeReport.value?.overallAvgScore ?? 0
  return Math.min(100, Math.max(0, score))
})

const avgScoreColor = computed(() => {
  const score = activeReport.value?.overallAvgScore ?? 0
  if (score >= 85) return '#22c55e'
  if (score >= 70) return '#8b5cf6'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
})

const lowScorePct = computed(() => {
  const total = activeReport.value?.studentTotal ?? 0
  if (!total) return 0
  return Math.round(((activeReport.value?.lowScoreCount ?? 0) / total) * 100)
})

const riskBarColor = computed(() => {
  const pct = lowScorePct.value
  if (pct >= 30) return '#ef4444'
  if (pct >= 15) return '#f59e0b'
  return '#22c55e'
})

const riskRingDash = computed(() => {
  const r = 37
  const circumference = 2 * Math.PI * r
  const filled = (lowScorePct.value / 100) * circumference
  return `${filled.toFixed(2)} ${circumference.toFixed(2)}`
})

const riskRingColor = computed(() => riskBarColor.value)

const riskLevelClass = computed(() => {
  const pct = lowScorePct.value
  if (pct >= 30) return 'level-high'
  if (pct >= 15) return 'level-mid'
  return 'level-low'
})

const riskLevelText = computed(() => {
  const pct = lowScorePct.value
  if (pct >= 30) return '⚠ 高风险'
  if (pct >= 15) return '△ 中风险'
  return '✓ 低风险'
})

const riskPercentText = computed(() => {
  const total = activeReport.value?.studentTotal ?? 0
  if (!total) return '--'
  return `${lowScorePct.value}%`
})

onMounted(() => {
  loadTasks()
  loadReportHistory()
})

function openHistoryDrawer() {
  historyDrawerVisible.value = true
  loadReportHistory()
}

async function loadTasks() {
  loading.value = true
  try {
    const result = await request.get<any, MonitorTask[]>('/homework/teacher/monitor/list', {
      skipErrorToast: true,
    })
    const raw = Array.isArray(result) ? result : []
    tasks.value = raw
  } catch (error: any) {
    message.error(error?.message || '学情数据加载失败')
  } finally {
    loading.value = false
  }
}

async function loadReportHistory() {
  historyLoading.value = true
  try {
    const result = await request.get<any, MonitorReport[]>('/homework/teacher/monitor/report/history', {
      skipErrorToast: true,
    })
    reportHistory.value = Array.isArray(result) ? result : []
  } catch (error: any) {
    message.error(error?.message || '历史报告加载失败')
  } finally {
    historyLoading.value = false
  }
}

async function deleteReport(reportId: number) {
  deletingReportId.value = reportId
  try {
    await request.post(`/homework/teacher/monitor/report/delete/${reportId}`)
    reportHistory.value = reportHistory.value.filter((r) => r.reportId !== reportId)
    message.success('报告已删除')
  } catch (error: any) {
    message.error(error?.message || '删除失败，请稍后重试')
  } finally {
    deletingReportId.value = null
  }
}

// 根据作业状态动态生成删除警告文案
// 进行中 / 未到截止的作业给更强警告，已截止的简单确认即可
function buildDeleteTaskWarning(task: MonitorTask): string {
  const typeLabel = task.assignmentType === 'exam' ? '考试' : '作业'
  const deadline = task.deadline ? new Date(task.deadline as string) : null
  const isOngoing =
    task.status === 'published' &&
    (!deadline || Number.isNaN(deadline.getTime()) || deadline.getTime() > Date.now())

  if (isOngoing) {
    const pending = task.pendingCount ?? 0
    const pendingHint = pending > 0 ? `当前仍有 ${pending} 位学生未提交，` : ''
    return `该${typeLabel}尚未截止，${pendingHint}删除后学生端将立即不可见。确定删除吗？`
  }
  return `确定删除这份${typeLabel}吗？学生的提交记录会保留，${typeLabel}本身会从学情分析里移除。`
}

async function deleteTask(task: MonitorTask) {
  if (!task?.assignmentId) return
  deletingTaskId.value = task.assignmentId
  try {
    await request.post(`/homework/teacher/monitor/assignment/delete/${task.assignmentId}`)
    // 本地直接移除，不用再请求一次列表
    tasks.value = tasks.value.filter((t) => t.assignmentId !== task.assignmentId)
    const deleteLabel = task.assignmentType === 'exam' ? '考试' : '作业'
    message.success(deleteLabel + '已删除')
  } catch (error: any) {
    message.error(error?.message || '删除失败，请稍后重试')
  } finally {
    deletingTaskId.value = null
  }
}

async function viewDetail(task: MonitorTask) {
  currentTask.value = task
  currentStudents.value = []
  detailVisible.value = true
  detailLoading.value = true

  try {
    const result = await request.get<any, MonitorStudent[]>('/homework/teacher/monitor/detail', {
      params: {
        assignmentId: task.assignmentId,
      },
      skipErrorToast: true,
    })
    currentStudents.value = Array.isArray(result) ? result : []
  } catch (error: any) {
    message.error(error?.message || '作业详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function viewStudentSubmissionReport(record: MonitorStudent) {
  if (!record?.submissionId) {
    message.warning('该学生暂无可查看的作答详情')
    return
  }

  activeSubmissionStudentName.value = record.studentName || '学生'
  submissionReportVisible.value = true
  submissionReportLoading.value = true
  activeSubmissionReport.value = null

  try {
    const result = await request.get<any, SubmissionReportVO>('/homework/teacher/monitor/submission-report', {
      params: {
        submissionId: record.submissionId,
      },
      skipErrorToast: true,
    })
    activeSubmissionReport.value = result || null
  } catch (error: any) {
    message.error(error?.message || '学生作答详情加载失败')
    submissionReportVisible.value = false
  } finally {
    submissionReportLoading.value = false
  }
}

// === 诊断报告 - 前置配置弹窗 ===
const reportConfigVisible = ref(false)
const reportConfigForm = ref<{ classId: string; publishDate: string; quizResourceId: string | null; assignmentType: string }>({
  classId: '',
  publishDate: '',
  quizResourceId: null,
  assignmentType: '',
})
const reportConfigErrors = ref<{ classId?: string; publishDate?: string; quizResourceId?: string }>({})

// 选中了具体试卷时，日期就应该由试卷本身的发布时间决定
const isDateLockedByQuiz = computed(() => !!reportConfigForm.value.quizResourceId)

// 弹窗里的"试卷/习题"下拉——只列出当前所选班级+类型下真实发布过的试卷
const reportConfigQuizOptions = computed(() => {
  const cid = reportConfigForm.value.classId
  const atype = reportConfigForm.value.assignmentType
  if (!cid) return []
  const map = new Map<number, string>()
  tasks.value
    .filter((t) => {
      if (String(t.classId ?? '') !== cid) return false
      if (atype && t.assignmentType !== atype) return false
      return true
    })
    .forEach((t) => {
      if (t.quizResourceId != null && !map.has(t.quizResourceId)) {
        map.set(t.quizResourceId, t.quizTitle || `试卷 ${t.quizResourceId}`)
      }
    })
  return Array.from(map, ([id, title]) => ({ id, title }))
    .sort((a, b) => a.title.localeCompare(b.title, 'zh-CN'))
})

// 弹窗里的"发布日期"下拉——只列出当前所选班级（+可选试卷）真实发布过的日期
// 这样可以避免教师选到"该班级根本没发布作业"的日期
const reportConfigDateOptions = computed(() => {
  const cid = reportConfigForm.value.classId
  const qid = reportConfigForm.value.quizResourceId
  if (!cid) return []
  const values = Array.from(
    new Set(
      tasks.value
        .filter((t) => {
          if (String(t.classId ?? '') !== cid) return false
          if (qid && String(t.quizResourceId ?? '') !== qid) return false
          return true
        })
        .map((t) => formatDate(t.publishTime))
        .filter((d) => d !== '--')
    )
  )
  return values.sort((a, b) => (a < b ? 1 : -1))
})

// 给定某个 quizResourceId，找出它最近一次的发布日期（yyyy-MM-dd）
// 同一套题可能被发布到不同班级 / 不同日期，这里必须叠加班级过滤，
// 否则会把"别的班级"的发布日期回填到"当前班级"上。
function resolvePublishDateByQuiz(quizResourceId: string | null, classId?: string): string {
  if (!quizResourceId) return ''
  const qid = Number(quizResourceId)
  const cid = classId ?? reportConfigForm.value.classId
  const dates = tasks.value
    .filter((t) => {
      if (t.quizResourceId !== qid) return false
      if (cid && String(t.classId ?? '') !== cid) return false
      return true
    })
    .map((t) => formatDate(t.publishTime))
    .filter((d) => d && d !== '--')
  if (dates.length === 0) return ''
  // formatDate 输出 yyyy-MM-dd 字典序和时间序一致，倒序取第一条 = 最近一次
  return dates.sort().reverse()[0]
}

// 监听班级变化：若已选的"试卷/日期"不再属于新班级，自动清空并提示
watch(
  () => reportConfigForm.value.classId,
  (newClassId, oldClassId) => {
    if (!newClassId || newClassId === oldClassId) return
    let dirty = false
    const validQuizIds = reportConfigQuizOptions.value.map((q) => String(q.id))
    if (
      reportConfigForm.value.quizResourceId &&
      !validQuizIds.includes(String(reportConfigForm.value.quizResourceId))
    ) {
      reportConfigForm.value.quizResourceId = null
      dirty = true
    }
    // 先处理完 quiz 再判断日期，因为日期的可选集依赖 quiz
    const validDates = reportConfigDateOptions.value
    if (
      reportConfigForm.value.publishDate &&
      !validDates.includes(reportConfigForm.value.publishDate)
    ) {
      reportConfigForm.value.publishDate = ''
      dirty = true
    }
    if (dirty) {
      message.info('已根据新班级重置试卷和发布日期选项')
    }
    // 班级变了，之前的校验错误作废
    reportConfigErrors.value = {}
  }
)

// 监听试卷变化：选中 → 自动回填日期并锁定；清空 → 不强制改动，让用户自己选
watch(
  () => reportConfigForm.value.quizResourceId,
  (newQuizId) => {
    if (newQuizId) {
      // 按当前班级找这套题的最近发布日期，而不是跨班级找
      const autoDate = resolvePublishDateByQuiz(newQuizId, reportConfigForm.value.classId)
      if (autoDate) {
        reportConfigForm.value.publishDate = autoDate
        // 如果之前存在日期相关的校验错误，一并清掉
        if (reportConfigErrors.value.publishDate) {
          reportConfigErrors.value = { ...reportConfigErrors.value, publishDate: undefined }
        }
      }
    }
  }
)

function generateClassReport() {
  if (!canGenerateReport.value) {
    message.warning('当前筛选维度下没有已批改的成绩样本')
    return
  }
  // 预填当前筛选项（若已是具体值则直接带入，否则留空让用户选）
  const presetClass = selectedClass.value !== 'all' ? selectedClass.value : ''
  const presetType = selectedType.value !== 'all' ? selectedType.value : ''
  const presetDate = selectedDate.value !== 'all' ? selectedDate.value : ''
  reportConfigForm.value = {
    classId: presetClass,
    publishDate: presetDate,
    quizResourceId: null,
    assignmentType: presetType,
  }
  reportConfigErrors.value = {}
  reportConfigVisible.value = true
}

async function confirmAndGenerateReport() {
  // 基本校验
  const errors: { classId?: string; publishDate?: string; quizResourceId?: string } = {}
  if (!reportConfigForm.value.classId) {
    errors.classId = '请选择具体班级，不可使用"全部班级"'
  }
  if (!reportConfigForm.value.quizResourceId) {
    errors.quizResourceId = '请选择具体的试卷或习题'
  }
  if (!reportConfigForm.value.publishDate) {
    errors.publishDate = '请选择具体发布日期，不可使用"全部日期"'
  }
  reportConfigErrors.value = errors
  if (Object.keys(errors).length > 0) return

  // 组合一致性校验
  const cid = reportConfigForm.value.classId
  const qid = reportConfigForm.value.quizResourceId
  const pd = reportConfigForm.value.publishDate
  const matchedTasks = tasks.value.filter((t) => {
    if (String(t.classId ?? '') !== cid) return false
    if (String(t.quizResourceId ?? '') !== qid) return false
    if (formatDate(t.publishTime) !== pd) return false
    return true
  })
  if (matchedTasks.length === 0) {
    message.warning('所选"班级 / 试卷 / 发布日期"组合下没有对应的任务，请重新选择')
    return
  }
  // 要求至少有一份匹配作业已有批改成绩，否则生成出来也是空报告
  const hasSample = matchedTasks.some((t) => Number(t.completedCount || 0) > 0)
  if (!hasSample) {
    message.warning('该组合下暂无已批改的真实成绩样本，暂时无法生成诊断报告')
    return
  }

  // 校验通过，关闭配置弹窗，打开报告弹窗并生成
  reportConfigVisible.value = false
  reportVisible.value = true
  isAnalyzing.value = true
  activeReport.value = null

  const payload: ReportRequest = {
    classId: Number(reportConfigForm.value.classId),
    publishDate: reportConfigForm.value.publishDate,
    quizResourceId: Number(reportConfigForm.value.quizResourceId),
    assignmentType: reportConfigForm.value.assignmentType || null,
  }

  try {
    const result = await request.post<any, MonitorReport>(
      '/homework/teacher/monitor/report',
      payload,
      { skipErrorToast: true }
    )
    activeReport.value = result
    message.success('诊断报告已生成')
    await loadReportHistory()
  } catch (error: any) {
    message.error(error?.message || '诊断报告生成失败')
    reportVisible.value = false
  } finally {
    isAnalyzing.value = false
  }
}

async function openHistoryReport(item: MonitorReport) {
  if (!item?.reportId) return

  historyDrawerVisible.value = false
  reportVisible.value = true
  isAnalyzing.value = true
  activeReport.value = null

  try {
    const result = await request.get<any, MonitorReport>('/homework/teacher/monitor/report/read', {
      params: {
        reportId: item.reportId,
      },
      skipErrorToast: true,
    })
    activeReport.value = result
  } catch (error: any) {
    message.error(error?.message || '历史报告读取失败')
    reportVisible.value = false
  } finally {
    isAnalyzing.value = false
  }
}

function parseDate(value: RawDate): Date | null {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function formatDate(value: RawDate): string {
  const date = parseDate(value)
  if (!date) return '--'
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatDateTime(value: RawDate): string {
  const date = parseDate(value)
  if (!date) return '--'
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}`
}

function formatPercent(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '--'
  return `${Number(value).toFixed(1)}%`
}

function formatScore(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '--'
  return Number(value).toFixed(1)
}

function displayStatusText(task: MonitorTask): string {
  if (task.status === 'closed') return '已关闭'
  const deadline = parseDate(task.deadline)
  if (deadline && deadline.getTime() < Date.now()) return '已截止'
  return '进行中'
}

function displayStatusClass(task: MonitorTask): 'pending' | 'completed' | 'closed' {
  if (task.status === 'closed') return 'closed'
  const deadline = parseDate(task.deadline)
  if (deadline && deadline.getTime() < Date.now()) return 'completed'
  return 'pending'
}

function submitStatusText(status?: string): string {
  switch (status) {
    case 'completed':
      return '已完成'
    case 'judging':
      return 'AI批改中'
    case 'submitted':
      return '已提交'
    case 'failed':
      return '批改失败'
    case 'pending':
    default:
      return '未提交'
  }
}

function submitStatusClass(status?: string): string {
  switch (status) {
    case 'completed':
      return 'ok'
    case 'judging':
      return 'warning'
    case 'submitted':
      return 'warning'
    case 'failed':
      return 'danger'
    case 'pending':
    default:
      return 'plain'
  }
}
</script>

<style scoped>
.modern-page {
  font-family: 'Plus Jakarta Sans', sans-serif;
  padding: 14px 30px;
  height: 100%;
  background: #f8fafc;
  border-radius: 5px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.glass-panel {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  border-radius: 5px;
}

.page-header {
  flex-shrink: 0;
  margin-bottom: 20px;
}

.title-group h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  display: flex;
  align-items: center;
}

.title-icon {
  color: #8b5cf6;
  margin-right: 10px;
  font-size: 30px;
}

.title-group .subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 15px;
}

.filter-dashboard {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  margin-bottom: 18px;
}

.filter-group {
  display: flex;
  gap: 16px; /* 原为 32px，缩小间距让整体更紧凑 */
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-item .label {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 6px;
}

.f-icon {
  color: #94a3b8;
}

.ai-report-btn {
  background: linear-gradient(135deg, #8b5cf6, #d946ef);
  border: none;
  font-weight: 700;
  box-shadow: 0 4px 15px rgba(139, 92, 246, 0.3);
  border-radius: 5px;
}

.main-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding-right: 12px;
  padding-bottom: 20px;
}

.main-scroll-area::-webkit-scrollbar {
  width: 6px;
}

.main-scroll-area::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}

.history-empty {
  min-height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  background: #f8fafc;
  border-radius: 5px;
}

.history-modal-body {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-modal-body > .history-empty {
  flex: 1;
  min-height: 0;
}

/* ===== 历史报告筛选栏 ===== */
.history-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px; /* 加大与下方的间距 */
  flex-wrap: nowrap; /* 移除日期后不需要换行 */
  flex-shrink: 0;
}
.history-filter-summary {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.history-filter-summary strong {
  color: #475569;
}

.drawer-history-list {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  align-content: start;
  align-items: start;
  grid-auto-rows: max-content;
  gap: 14px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 10px;
  margin-right: -4px;
}

.drawer-history-list::-webkit-scrollbar {
  width: 6px;
}

.drawer-history-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}

/* ===== 历史记录卡片 ===== */
.history-card {
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: visible;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.drawer-history-list .history-card {
  margin-bottom: 0;
}

/* 左侧状态线设计 */
.history-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #e2e8f0;
  border-radius: 8px 0 0 8px;
  transition: background 0.25s ease;
  pointer-events: none;
}

.history-card:hover {
  transform: translateY(-2px);
  border-color: #cbd5e1;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.04);
}

.history-card:hover::before {
  background: #8b5cf6; /* 悬浮时左侧亮起紫色 */
}

.history-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.history-title {
  font-size: 15px;
  line-height: 1.5;
  font-weight: 800;
  color: #0f172a;
  flex: 1;
  min-width: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
}

.history-card-actions {
  flex-shrink: 0;
}

.delete-report-btn {
  color: #cbd5e1;
  padding: 0 4px;
  height: 24px;
}
.delete-report-btn:hover {
  color: #ef4444 !important;
}

.history-time-row {
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.history-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.meta-tag {
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  max-width: 100%;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 底部数据统计面板 */
.history-stats-box {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  background: #f8fafc;
  padding: 12px;
  border-radius: 6px;
  min-width: 0;
}

.history-preview-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid #eef2f7;
  color: #94a3b8;
  font-size: 12px;
  min-width: 0;
}

.history-preview-row span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-preview-row :deep(.ant-btn) {
  height: 24px;
  padding: 0;
  font-weight: 700;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.mini-label {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
}

.mini-value {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.25;
  white-space: nowrap;
}

.mini-stat.danger .mini-value {
  color: #ef4444;
}


.delete-report-btn {
  color: #cbd5e1;
  padding: 0 4px;
  height: 22px;
  line-height: 22px;
}
.delete-report-btn:hover { color: #ef4444 !important; }

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.task-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  transition: 0.3s;
  background: #fff;
}

.task-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.course-name {
  font-size: 12px;
  font-weight: 700;
  color: #4f46e5;
  background: #e0e7ff;
  padding: 4px 10px;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 70%;
}



.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.status-badge.pending {
  background: #fff7ed;
  color: #d97706;
}

.status-badge.completed {
  background: #dcfce7;
  color: #166534;
}

.status-badge.closed {
  background: #e2e8f0;
  color: #475569;
}

.type-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  letter-spacing: 0.5px;
}
.type-badge.homework {
  background: #ede9fe;
  color: #7c3aed;
}
.type-badge.exam {
  background: #fef3c7;
  color: #d97706;
}

/* 卡片右上角容器：状态徽章 + 删除按钮 */
.card-header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.delete-task-btn {
  color: #cbd5e1;
  padding: 0 4px;
  height: 22px;
  line-height: 22px;
  min-width: auto;
}
.delete-task-btn:hover {
  color: #ef4444 !important;
  background: transparent !important;
}

.card-body {
  flex: 1;
  margin-bottom: 12px;
  border-bottom: 1px dashed #e2e8f0;
  padding-bottom: 12px;
}

.card-body .title {
  font-size: 15px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta-info,
.extra-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
}

.extra-meta {
  margin-top: 10px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.stat-item {
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 5px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-item.danger-soft {
  background: #fff7f7;
  border-color: #fee2e2;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.risk-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #b91c1c;
  font-size: 13px;
}

.risk-label {
  color: #64748b;
}

.risk-value {
  font-weight: 800;
}

.empty-box {
  height: 100%;
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px dashed #cbd5e1;
}

.empty-content {
  text-align: center;
  transform: translateY(-20px);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: #cbd5e1;
}

.spin-soft {
  animation: floatSpin 2.2s linear infinite;
}

@keyframes floatSpin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.modal-custom-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  overflow-y: hidden;
  padding-right: 8px;
}

.summary-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  border: 1px solid #e2e8f0;
}

.student-table {
  background: #fff;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.submission-report-wrapper {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  padding-right: 8px;
}

:deep(.submission-report-wrapper .ant-spin-nested-loading),
:deep(.submission-report-wrapper .ant-spin-container) {
  min-height: 0;
  height: 100%;
}
:deep(.student-table .ant-table-empty .ant-table-tbody > tr.ant-table-placeholder > td) {
  height: 380px; /* 撑开无数据时的高度，让图标居中 */
  border-bottom: none;
}

.table-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.table-status.ok {
  background: #dcfce7;
  color: #166534;
}

.table-status.warning {
  background: #fff7ed;
  color: #b45309;
}

.table-status.danger {
  background: #fee2e2;
  color: #b91c1c;
}

.table-status.plain {
  background: #f1f5f9;
  color: #475569;
}

.action-disabled {
  color: #94a3b8;
}

.submission-report-header {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.submission-report-card {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f7f8fa;
  border: 1px solid #eef0f3;
}

.submission-report-card .label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 6px;
}

.submission-report-card .value {
  font-size: 20px;
  font-weight: 700;
  color: #262626;
}

.submission-report-card .value.text {
  font-size: 14px;
  line-height: 1.6;
}

.submission-meta-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.meta-chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 12px;
}

.submission-section {
  margin-top: 18px;
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
}

.section-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #1f1f1f;
}

.question-card {
  padding: 14px 16px;
  border-radius: 14px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.question-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.question-no {
  font-size: 15px;
  font-weight: 700;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.question-type-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background: #ede9fe;
  color: #6d28d9;
  font-size: 12px;
  font-weight: 600;
}

.question-score {
  font-size: 13px;
  color: #595959;
  white-space: nowrap;
}

.question-row {
  margin-bottom: 8px;
  line-height: 1.8;
  color: #434343;
}

.row-label {
  color: #8c8c8c;
  margin-right: 6px;
}

.row-value {
  color: #262626;
  white-space: pre-wrap;
  word-break: break-word;
}

.judge-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.judge-badge.correct {
  background: #dcfce7;
  color: #166534;
}

.judge-badge.wrong {
  background: #fee2e2;
  color: #b91c1c;
}

.report-wrapper {
  height: 680px; /* 统一修改为标准高度 */
  overflow-y: auto;
  padding-right: 16px;
}
.analyzing-state {
  height: 100%; /* 让它撑满父容器 report-wrapper 的高度 */
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.radar-spinner {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  border: 4px solid #ede9fe;
  border-top-color: #8b5cf6;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.report-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.summary-card {
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 5px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-k {
  font-size: 12px;
  color: #64748b;
}

.summary-v {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.doc-style {
  line-height: 1.8;
  color: #334155;
}

.doc-style :deep(h1),
.doc-style :deep(h2),
.doc-style :deep(h3) {
  color: #0f172a;
  margin-top: 16px;
}

.doc-style :deep(ul) {
  padding-left: 18px;
}

/* ===== 诊断报告配置弹窗 ===== */
.report-config-form {
  padding: 4px 0 8px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.config-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 5px;
  border-left: 3px solid #8b5cf6;
}
.config-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.config-label {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}
.required-star {
  color: #ef4444;
  margin-right: 4px;
}
.config-hint {
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
  margin-left: 4px;
}
.config-error {
  font-size: 12px;
  color: #ef4444;
}
.config-form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 4px;
  border-top: 1px solid #f1f5f9;
  margin-top: 4px;
}
.confirm-report-btn {
  background: #8b5cf6 !important;
  border-color: #8b5cf6 !important;
  font-weight: 700;
}
.confirm-report-btn:hover {
  background: #7c3aed !important;
  border-color: #7c3aed !important;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-btn),
:deep(.ant-table),
:deep(.ant-table-container),
:deep(.ant-table-thead > tr > th),
:deep(.ant-table-tbody > tr > td) {
  border-radius: 5px !important;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fafc !important;
  color: #475569;
  font-weight: 700;
}

@media (max-width: 1200px) {
  .filter-dashboard {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .filter-group {
    flex-wrap: wrap;
    gap: 16px;
  }

  .report-summary,
  .submission-report-header {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .modern-page {
    padding: 12px;
  }

  .task-grid,
  .report-summary,
  .stats-grid,
  .submission-report-header {
    grid-template-columns: 1fr;
  }

  .question-top,
  .card-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
/* ===== 诊断报告 - 顶部元信息条 ===== */
.report-meta-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.rmb-chip {
  padding: 5px 12px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  font-size: 12px;
  color: #475569;
}

.rmb-chip strong {
  color: #1e293b;
  margin-right: 4px;
}

/* ===== 诊断报告 - 可视化图表区 ===== */
.report-charts-row {
  display: grid;
  grid-template-columns: 190px 1fr 165px;
  gap: 14px;
  margin-bottom: 20px;
}

.rc-card {
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 16px;
  padding: 16px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.rc-label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  align-self: flex-start;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.donut-wrap {
  width: 118px;
  height: 118px;
}

.donut-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.donut-sub-stats {
  display: flex;
  flex-direction: column;
  gap: 5px;
  align-self: stretch;
}

.dss-item {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  display: block;
  white-space: nowrap;
}

.dss-item.ok {
  background: #dcfce7;
  color: #166534;
}

.dss-item.warn {
  background: #fef3c7;
  color: #92400e;
}

.bars-card {
  align-items: stretch;
  gap: 13px;
}

.bar-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.bar-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bar-name {
  font-size: 12px;
  color: #64748b;
}

.bar-val {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}

.bar-track {
  height: 7px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.9s cubic-bezier(0.34, 1.56, 0.64, 1);
  min-width: 4px;
}

.assignments-dots {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 1px;
}

.dot-assignment {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #8b5cf6;
  opacity: 0.65;
  display: inline-block;
}

.dot-more {
  font-size: 11px;
  color: #64748b;
  align-self: center;
  margin-left: 2px;
}

.risk-card {
  gap: 8px;
}

.risk-donut-wrap {
  width: 100px;
  height: 100px;
}

.risk-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.risk-verdict {
  font-size: 13px;
  font-weight: 700;
  padding: 3px 14px;
  border-radius: 999px;
  white-space: nowrap;
}

.risk-verdict.level-high {
  background: #fee2e2;
  color: #b91c1c;
}

.risk-verdict.level-mid {
  background: #fef3c7;
  color: #92400e;
}

.risk-verdict.level-low {
  background: #dcfce7;
  color: #166534;
}

.risk-pct {
  font-size: 11px;
  color: #94a3b8;
}

.report-text-section {
  border: 1px solid #eef2f7;
  border-radius: 16px;
  overflow: hidden;
}

.rts-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #eef2f7;
  font-size: 13px;
  font-weight: 700;
  color: #475569;
}

.rts-icon {
  color: #8b5cf6;
  font-size: 16px;
}

.report-content {
  padding: 16px 20px;
}

@media (max-width: 900px) {
  .drawer-history-list {
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  }

  .report-charts-row {
    grid-template-columns: 1fr 1fr;
  }
  .risk-card {
    grid-column: span 2;
    flex-direction: row;
    justify-content: center;
    gap: 24px;
  }
}

@media (max-width: 600px) {
  .history-filter-bar {
    flex-wrap: wrap;
  }

  .history-filter-bar :deep(.ant-select) {
    flex: 1 1 100% !important;
    width: 100% !important;
  }

  .drawer-history-list {
    grid-template-columns: 1fr;
  }

  .report-charts-row {
    grid-template-columns: 1fr;
  }
  .risk-card {
    grid-column: span 1;
    flex-direction: column;
    align-items: center;
  }
}

</style>
