<template>
  <div class="edu-app">
    <main class="edu-main">
      <div class="page-shell">
        <!-- ============ 顶部 Banner ============ -->
        <section class="page-intro glass-panel">
          <div class="intro-copy">
            <div class="overline"><radar-chart-outlined /> 多维认知状态评估系统 (MCAS)</div>
            <h1 class="page-title">学习状态检测与认知画像</h1>
            <p class="subtitle">
              融合量表自评、面部疲劳监测和近 7 天学习画像，生成六维状态判断与学习干预建议。
            </p>

            <div class="physio-badges" v-if="fatigueStats">
              <span class="badge-pill pill-amber"><thunderbolt-outlined /> 哈欠 {{ fatigueStats.yawnCount }} 次</span>
              <span class="badge-pill pill-red"><eye-outlined /> 闭眼疲劳 {{ fatigueStats.fatigueCount }} 次</span>
              <span class="badge-pill pill-gray"><user-outlined /> 离屏 {{ fatigueStats.noFaceCount }} 次</span>
              <span class="badge-pill pill-green"><dashboard-outlined /> 专注率 {{ computedFocusRate }}%</span>
              <span class="badge-pill pill-blue"><clock-circle-outlined /> 监测 {{ monitorDurationMin }} 分钟</span>
              <span class="badge-pill pill-violet"><book-outlined /> 学习画像 {{ learningProfileStatusText }}</span>
            </div>

            <div class="physio-badges" v-else>
              <span class="badge-pill pill-gray">暂无生理监测数据 — 请先前往看课页面开启摄像头检测</span>
              <span class="badge-pill pill-violet"><book-outlined /> 学习画像 {{ learningProfileStatusText }}</span>
            </div>
          </div>

          <div class="hero-stats">
            <div class="mini-stat">
              <span class="mini-label">评估进度</span>
              <div class="mini-value">
                {{ currentStep < questions.length ? currentStep + 1 : questions.length }}
                <small>/ {{ questions.length }}</small>
              </div>
            </div>
            <div class="mini-stat">
              <span class="mini-label">数据通道</span>
              <div class="mini-value">{{ dataChannelText }}</div>
            </div>
            <div class="mini-stat">
              <span class="mini-label">分析维度</span>
              <div class="mini-value">6<small>维</small></div>
            </div>
          </div>
        </section>

        <!-- ============ 主体内容 ============ -->
        <section class="content-grid">
          <!-- ====== 左栏：AI 引导对话 ====== -->
          <div class="chat-section glass-panel">
            <div class="chat-header">
              <div class="header-left">
                <div class="pulse-dot"></div>
                <span>状态评估</span>
                <span class="header-tag">{{ headerModeTag }}</span>
              </div>

              <button class="reset-btn" @click="resetAssessment" v-if="currentStep > 0">
                <redo-outlined /> 重新评估
              </button>
            </div>

            <div class="chat-window" ref="msgContainer">
              <div
                v-for="(msg, index) in chatHistory"
                :key="index"
                :class="['msg-bubble', msg.role]"
              >
                <div v-if="msg.role === 'ai'" class="avatar ai-avatar">
                  <robot-outlined />
                </div>

                <div class="content" v-html="msg.content"></div>

                <div v-if="msg.role === 'user'" class="avatar user-avatar">
                  <user-outlined />
                </div>
              </div>

              <div v-if="isAnalyzing" class="msg-bubble ai">
                <div class="avatar ai-avatar"><robot-outlined /></div>
                <div class="content analyzing-box">
                  <loading-outlined class="spin-icon" />
                  <div class="analyze-text">
                    <span>正在进行六维认知画像分析...</span>
                    <span class="analyze-sub">融合主观量表 + {{ fatigueStats ? '客观生理数据' : '行为观察' }} + 学习画像</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="input-area" v-if="!analysisData && !isAnalyzing">
              <template v-if="selectedMode === null">
                <div class="quick-options mode-selection">
                  <button
                    v-if="fatigueStats && fatigueStats.monitorSeconds >= MIN_VALID_SECONDS"
                    class="option-btn dual-btn"
                    @click="handleModeSelection('dual')"
                  >
                    <radar-chart-outlined /> 双通道深度融合分析
                  </button>

                  <button
                    v-if="fatigueStats && fatigueStats.monitorSeconds > 0 && fatigueStats.monitorSeconds < MIN_VALID_SECONDS"
                    class="option-btn force-dual-btn"
                    @click="handleModeSelection('dual')"
                  >
                    <warning-outlined /> 忽略警告，强制融合
                  </button>

                  <button
                    class="option-btn subjective-btn"
                    @click="handleModeSelection('subjective')"
                  >
                    <user-outlined /> 纯主观评估速测
                  </button>
                </div>
              </template>

              <template v-else-if="currentStep < questions.length">
                <div class="question-meta">
                  <span class="q-scale">{{ questions[currentStep].scale }}</span>
                  <span class="q-step">Q{{ currentStep + 1 }}/{{ questions.length }}</span>
                </div>

                <div class="quick-options">
                  <button
                    v-for="(opt, idx) in questions[currentStep].options"
                    :key="idx"
                    class="option-btn"
                    @click="handleAnswer(opt)"
                  >
                    {{ opt }}
                  </button>
                </div>

                <div class="custom-input">
                  <input
                    v-model="userText"
                    @keyup.enter="handleCustomAnswer"
                    type="text"
                    placeholder="也可以用自己的话补充当前状况..."
                  />
                  <button class="send-btn" @click="handleCustomAnswer" :disabled="!userText.trim()">
                    <send-outlined /> 发送
                  </button>
                </div>
              </template>
            </div>
          </div>

          <!-- ====== 右栏：分析仪表盘 ====== -->
          <div class="dashboard-section">
            <div v-if="!analysisData" class="empty-card glass-panel">
              <div class="empty-icon"><pie-chart-outlined /></div>
              <h3>等待分析数据</h3>
              <p>完成左侧 {{ questions.length }} 轮评估后，系统将生成六维认知画像。</p>
              <div class="method-tags">
                <span class="m-tag">唤醒理论</span>
                <span class="m-tag">认知负荷</span>
                <span class="m-tag">心流模型</span>
                <span class="m-tag">疲劳监测</span>
                <span class="m-tag">学习画像</span>
              </div>
              <div class="learning-context-card">
                <div class="learning-context-top">
                  <strong>已关联学习数据</strong>
                  <span>{{ learningProfileStatusText }}</span>
                </div>
                <p>{{ learningBriefText }}</p>
                <div class="learning-chip-row">
                  <span v-for="item in learningContextChips" :key="item" class="learning-chip">{{ item }}</span>
                </div>
              </div>
            </div>

            <div v-else class="data-board fade-in">
              <!-- 六维指标卡片 -->
              <div class="metrics-row">
                <div class="metric-card glass-panel" v-for="m in metricCards" :key="m.key">
                  <div class="m-icon" :class="m.color">
                    <component :is="m.icon" />
                  </div>
                  <div class="m-info">
                    <span class="m-val">{{ getMetricLabel(m.key, analysisData[m.key]) }}</span>
                    <span class="m-lbl">{{ m.label }}</span>
                  </div>
                  <div class="m-bar-bg">
                    <div
                      class="m-bar-fill"
                      :class="m.color"
                      :style="{ width: (analysisData[m.key] * 10) + '%' }"
                    ></div>
                  </div>
                </div>
              </div>

              <!-- 生理数据面板 -->
              <div
                class="physio-panel glass-panel"
                v-if="hasTimelineData"
              >
                <div class="card-title-row">
                  <h3><monitor-outlined /> 生理疲劳时序图</h3>
                  <span class="data-source-tag">生理监测</span>
                </div>
                <div ref="timelineChartRef" class="timeline-chart-box"></div>
              </div>

              <!-- 雷达图 + 理论引用 -->
              <div class="chart-card glass-panel full-row-card">
                <div class="card-title-row title-with-info">
                  <h3><radar-chart-outlined /> 六维认知雷达</h3>

                  <div class="theory-hover-wrapper">
                    <button class="info-badge-btn">
                      <info-circle-outlined /> 理论依据与风险
                    </button>

                    <div class="floating-panel glass-panel">
                      <h4 class="pop-title"><book-outlined /> 理论依据</h4>
                      <div class="pop-list">
                        <div v-for="(t, i) in analysisData.theories" :key="'t'+i" class="pop-item">
                          <span class="pop-num">{{ i + 1 }}</span>
                          <span class="pop-text">{{ t }}</span>
                        </div>
                      </div>

                      <h4 class="pop-title" style="margin-top: 16px;"><alert-outlined /> 风险识别</h4>
                      <div class="pop-list">
                        <div v-for="(r, i) in analysisData.riskFlags" :key="'r'+i" class="pop-item risk-text">
                          <warning-outlined /> {{ r }}
                        </div>
                        <div v-if="!analysisData.riskFlags || analysisData.riskFlags.length === 0" class="pop-item safe-text">
                          <check-circle-outlined /> 未发现显著风险因素
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div ref="radarChartRef" class="radar-box enlarged-radar"></div>
              </div>

              <!-- 三级建议 -->
              <div class="suggestion-card glass-panel">
                <h3><experiment-outlined /> 分层学习干预建议</h3>
                <div class="verdict-bar">
                  <bulb-outlined class="verdict-icon" />
                  <span>{{ analysisData.verdict }}</span>
                </div>

                <div class="suggestion-tiers">
                  <div class="tier-item">
                    <div class="tier-badge tier-now">立即执行</div>
                    <p>{{ analysisData.suggestions?.immediate || '暂无' }}</p>
                  </div>
                  <div class="tier-item">
                    <div class="tier-badge tier-week">本周调整</div>
                    <p>{{ analysisData.suggestions?.shortTerm || '暂无' }}</p>
                  </div>
                  <div class="tier-item">
                    <div class="tier-badge tier-habit">长期习惯</div>
                    <p>{{ analysisData.suggestions?.habit || '暂无' }}</p>
                  </div>
                </div>

              </div>

              <div class="learning-evidence-card glass-panel">
                <div class="card-title-row">
                  <h3><book-outlined /> 学习证据联动</h3>
                  <span class="data-source-tag">学习画像</span>
                </div>
                <p>{{ learningBriefText }}</p>
                <div class="learning-evidence-grid">
                  <div v-for="item in learningEvidenceItems" :key="item.label" class="learning-evidence-item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                    <em>{{ item.detail }}</em>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import { message } from 'ant-design-vue';
import {
  RobotOutlined,
  UserOutlined,
  SendOutlined,
  LoadingOutlined,
  CoffeeOutlined,
  RocketOutlined,
  RadarChartOutlined,
  PieChartOutlined,
  CloudOutlined,
  ThunderboltOutlined,
  FireOutlined,
  ExperimentOutlined,
  EyeOutlined,
  DashboardOutlined,
  ClockCircleOutlined,
  AlertOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  BookOutlined,
  BulbOutlined,
  MonitorOutlined,
  RedoOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue';
import request from '@/utils/request'
import { getAuthToken, getLoginUserRaw } from '@/utils/authStorage'
import { fetchMyLearningProfile, type StudentLearningProfile } from '@/api/learning'

type ChatRole = 'ai' | 'user';

interface ChatMessage {
  role: ChatRole;
  content: string;
}

interface QuestionItem {
  scale: string;
  text: string;
  options: string[];
}

const userText = ref('');
const isAnalyzing = ref(false);
const msgContainer = ref<HTMLElement | null>(null);

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820';

// ================================================================
// 从后端 API 获取今日疲劳数据（优先），localStorage 做降级兜底
// ================================================================
const fatigueStats = ref<any>(null);
const learningProfile = ref<StudentLearningProfile | null>(null);
const learningProfileLoading = ref(false);

// 新增：当前选择的评估模式
const selectedMode = ref<'dual' | 'subjective' | null>(null);

// 新增：定义客观数据有效性的最小阈值（例如 3分钟 = 180秒）
const MIN_VALID_SECONDS = 180;
const LEARNING_PROFILE_DAYS = 7;

const safeParseJsonArray = (value: unknown): any[] => {
  if (Array.isArray(value)) return value;
  if (typeof value !== 'string' || !value.trim()) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

/** 获取当前登录用户ID */
const getCurrentUserId = (): number | null => {
  try {
    const uStr = getLoginUserRaw();
    if (uStr) {
      const user = JSON.parse(uStr);
      return user.id || null;
    }
  } catch {
    // ignore
  }
  return null;
};

const loadFatigueStats = async () => {
  const userId = getCurrentUserId();

  // 优先从后端数据库获取（更权威）
  if (userId) {
    try {
      const db = await request.get<any, any>('/fatigue/today');
      if (db) {
        fatigueStats.value = {
          yawnCount: db.yawnCount || 0,
          fatigueCount: db.fatigueCount || 0,
          noFaceCount: db.noFaceCount || 0,
          normalCount: db.normalCount || 0,
          totalDetections: db.totalDetections || 0,
          monitorSeconds: db.monitorSeconds || 0,
          lastStatus: db.lastStatus || 'normal',
          sessionStart: db.sessionStart || null,
          events: safeParseJsonArray(db.events),
          earSamples: safeParseJsonArray(db.earSamples),
          marSamples: safeParseJsonArray(db.marSamples),
        };
        console.log('从数据库加载今日疲劳数据成功');
        return;
      }
    } catch (e) {
      console.warn('后端获取疲劳数据失败，降级到 localStorage:', e);
    }
  }

  // 降级：从 localStorage 读取
  try {
    const storageKey = userId ? `fatigue_stats_${userId}` : 'fatigue_stats';
    const raw = localStorage.getItem(storageKey);
    if (raw) {
      const saved = JSON.parse(raw);
      const today = new Date().toISOString().slice(0, 10);
      if (saved.dateKey === today) {
        fatigueStats.value = {
          ...saved,
          events: safeParseJsonArray(saved.events),
          earSamples: safeParseJsonArray(saved.earSamples),
          marSamples: safeParseJsonArray(saved.marSamples),
        };
        console.log('从 localStorage 加载今日疲劳数据');
      }
    }
  } catch (e) {
    console.warn('localStorage 读取也失败:', e);
  }
};

const toLimitedList = <T,>(list: T[] | undefined | null, limit: number): T[] =>
  Array.isArray(list) ? list.slice(0, limit) : [];

const buildLearningProfileSnapshot = () => {
  const p = learningProfile.value;
  if (!p) return null;
  return {
    days: p.days || LEARNING_PROFILE_DAYS,
    insight: p.insight || null,
    preference: p.preference || null,
    weakPoints: toLimitedList(p.weakPoints, 6),
    wrongQuestions: toLimitedList(p.wrongQuestions, 4).map(item => ({
      assignmentTitle: item.assignmentTitle,
      questionNo: item.questionNo,
      questionType: item.questionType,
      aiComment: item.aiComment,
      actionUrl: item.actionUrl,
      actionLabel: item.actionLabel,
    })),
    recommendations: toLimitedList(p.recommendations, 5).map(item => ({
      courseName: item.courseName,
      resourceType: item.resourceType,
      resourceTitle: item.resourceTitle,
      knowledgeName: item.knowledgeName,
      recommendationReason: item.recommendationReason,
      practiceSuggestion: item.practiceSuggestion,
      actionUrl: item.actionUrl,
      actionLabel: item.actionLabel,
      shortReason: item.shortReason,
    })),
    actionPlans: toLimitedList(p.actionPlans, 4),
    evidenceItems: toLimitedList(p.evidenceItems, 4),
  };
};

const summarizeLearningProfile = (): string => {
  const snapshot = buildLearningProfileSnapshot();
  if (!snapshot) return '暂未获取到学习画像数据，本次仅基于状态检测和问卷结果生成建议。';

  const weak = snapshot.weakPoints
    .map((item: any) => `${item.knowledgeName || '未知知识点'}(${item.masteryScore ?? '--'}%)`)
    .join('、') || '暂无明显薄弱点';
  const wrong = snapshot.wrongQuestions
    .map((item: any) => item.assignmentTitle || item.questionNo || '错题')
    .filter(Boolean)
    .slice(0, 3)
    .join('、') || '暂无近期错题';
  const rec = snapshot.recommendations
    .map((item: any) => item.resourceTitle || item.knowledgeName)
    .filter(Boolean)
    .slice(0, 3)
    .join('、') || '暂无待办推荐';

  return `近 ${snapshot.days} 天学习画像：${snapshot.insight?.title || '已生成学习背景'}；趋势：${snapshot.insight?.trendLabel || '暂无趋势'}；薄弱点：${weak}；近期错题：${wrong}；推荐任务：${rec}。`;
};

const buildLearningContext = (): string => {
  const snapshot = buildLearningProfileSnapshot();
  if (!snapshot) {
    return '【学习画像背景】暂未读取到近期学习画像，请不要臆造具体课程、薄弱点或错题，仅给出通用学习状态建议。';
  }

  const lines: string[] = [];
  lines.push(`【学习画像背景 — 来自数据库最近 ${snapshot.days} 天 learning_event / student_knowledge_mastery / homework / recommendation】`);
  if (snapshot.insight) {
    lines.push(`- 综合学习判断：${snapshot.insight.title || '暂无标题'}`);
    lines.push(`- 学习画像说明：${snapshot.insight.body || '暂无说明'}`);
    lines.push(`- 掌握度：${snapshot.insight.overallScore ?? '--'}%，风险：${snapshot.insight.riskLabel || '暂无'}，趋势：${snapshot.insight.trendLabel || '暂无'}`);
    lines.push(`- 行为记录 ${snapshot.insight.recentActivityCount ?? 0} 条，薄弱点 ${snapshot.insight.weakPointCount ?? 0} 个，错题信号 ${snapshot.insight.wrongQuestionCount ?? 0} 条，置信度 ${snapshot.insight.confidence ?? 0}%`);
  }
  if (snapshot.preference?.summary) {
    lines.push(`- 学习偏好：${snapshot.preference.summary}`);
  }
  if (snapshot.weakPoints.length) {
    lines.push('- 当前薄弱点：');
    snapshot.weakPoints.forEach((item: any, index: number) => {
      lines.push(`  ${index + 1}. ${item.knowledgeName || '未知知识点'}，掌握度 ${item.masteryScore ?? '--'}%，证据：${item.evidenceSummary || '暂无'}`);
    });
  }
  if (snapshot.wrongQuestions.length) {
    lines.push('- 近期错题信号：');
    snapshot.wrongQuestions.forEach((item: any, index: number) => {
      lines.push(`  ${index + 1}. ${item.assignmentTitle || '未命名练习'} ${item.questionNo || ''}，类型：${item.questionType || '未知'}，点评：${item.aiComment || '暂无'}`);
    });
  }
  if (snapshot.recommendations.length) {
    lines.push('- 系统推荐任务：');
    snapshot.recommendations.forEach((item: any, index: number) => {
      lines.push(`  ${index + 1}. ${item.resourceTitle || item.knowledgeName || '学习任务'}，知识点：${item.knowledgeName || '综合'}，原因：${item.shortReason || item.recommendationReason || '暂无'}`);
    });
  }
  if (snapshot.actionPlans.length) {
    lines.push('- 建议行动计划：');
    snapshot.actionPlans.forEach((item: any, index: number) => {
      lines.push(`  ${index + 1}. ${item.title || item.target || '学习任务'}，预计 ${item.minutes || 10} 分钟，理由：${item.reason || '暂无'}`);
    });
  }

  lines.push('请把状态结果和学习画像交叉分析：如果疲劳高，不要直接建议攻克高难题；如果状态好，可安排薄弱点补强或错题回练。建议必须具体到上述知识点、错题或推荐任务。');
  return lines.join('\n');
};

const loadLearningProfile = async () => {
  learningProfileLoading.value = true;
  try {
    learningProfile.value = await fetchMyLearningProfile({ days: LEARNING_PROFILE_DAYS });
    console.log('从数据库加载学习画像成功');
  } catch (e) {
    learningProfile.value = null;
    console.warn('学习画像加载失败，本次状态检测将使用通用学习建议:', e);
  } finally {
    learningProfileLoading.value = false;
  }
};

const computedFocusRate = computed(() => {
  if (!fatigueStats.value || fatigueStats.value.totalDetections === 0) return 0;
  return Math.round(((fatigueStats.value.normalCount || 0) / fatigueStats.value.totalDetections) * 100);
});

const hasTimelineData = computed(() => {
  const s = fatigueStats.value;
  if (!s) return false;
  return Boolean(
    (Array.isArray(s.events) && s.events.length > 0) ||
    (Array.isArray(s.earSamples) && s.earSamples.length > 0) ||
    (Array.isArray(s.marSamples) && s.marSamples.length > 0)
  );
});

/** 监测时长：基于摄像头累计秒数 */
const monitorDurationMin = computed(() => {
  if (!fatigueStats.value) return 0;
  return Math.round((fatigueStats.value.monitorSeconds || 0) / 60);
});

const learningProfileStatusText = computed(() => {
  if (learningProfileLoading.value) return '读取中';
  if (!learningProfile.value) return '暂无数据';
  const insight = learningProfile.value.insight;
  const count = insight?.recentActivityCount ?? 0;
  const weakCount = learningProfile.value.weakPoints?.length || 0;
  return `${count} 条行为 · ${weakCount} 个薄弱点`;
});

const dataChannelText = computed(() => {
  const base = fatigueStats.value ? '双通道' : '单通道';
  return learningProfile.value ? `${base}+画像` : base;
});

const headerModeTag = computed(() => {
  const base = fatigueStats.value ? '双通道融合' : '主观自评';
  return learningProfile.value ? `${base} · 学习画像` : base;
});

const learningBriefText = computed(() => {
  const p = learningProfile.value;
  if (learningProfileLoading.value) return '正在同步学习证据。';
  if (!p) return '暂无近期学习画像，仍可完成状态检测。';

  const risk = p.insight?.riskLabel || '学习画像已同步';
  const weakPoint = p.weakPoints?.[0]?.knowledgeName;
  const task = p.actionPlans?.[0]?.title || p.recommendations?.[0]?.resourceTitle;
  if (weakPoint) return `${risk}，优先关注 ${weakPoint}。`;
  if (task) return `${risk}，建议先完成 ${task}。`;
  return risk;
});

const learningContextChips = computed(() => {
  const p = learningProfile.value;
  if (!p) return ['等待学习画像', '可继续完成检测'];
  const chips = [
    p.insight?.riskLabel,
    p.insight?.confidenceLabel,
    p.insight?.trendLabel,
    p.preference?.dominantType ? `偏好 ${p.preference.dominantType}` : '',
  ].filter(Boolean) as string[];
  return chips.length ? chips : ['学习证据偏少', '建议补充练习'];
});

const learningEvidenceItems = computed(() => {
  const p = learningProfile.value;
  if (p?.evidenceItems?.length) {
    return p.evidenceItems.slice(0, 4);
  }
  return [
    { label: '行为记录', value: '0', detail: '暂无近期行为证据' },
    { label: '薄弱点', value: '0', detail: '完成练习后自动生成' },
    { label: '错题信号', value: '0', detail: '提交作业后自动关联' },
  ];
});

// ================================================================
// 五轮科学评估问卷（每题标注理论来源）
// ================================================================
const questions: QuestionItem[] = [
  {
    scale: '基于 Karolinska 嗜睡量表 (KSS)',
    text: '你好，我是认知状态评估助手。首先评估你的<b>主观警觉度</b>——如果把大脑比作电池，现在还剩多少电量？',
    options: ['满血复活 (80-100%)', '正常续航 (50-80%)', '电量告急 (20-50%)', '濒临关机 (<20%)']
  },
  {
    scale: '基于 NASA-TLX 认知负荷量表',
    text: '好的。接下来评估<b>认知负荷</b>——你觉得最近的学习任务在脑力消耗上属于哪个档位？',
    options: ['轻松无压力，游刃有余', '中等但可控，需要一定专注', '较高，需要刻意努力集中', '已经超负荷，大脑快转不动了']
  },
  {
    scale: '基于 PANAS 积极/消极情绪量表',
    text: '收到。现在评估<b>情绪效价</b>——用一个词描述你当前的情绪底色：',
    options: ['积极愉悦，有干劲', '平静中性，波澜不惊', '有些焦虑或烦躁', '疲惫低落，缺乏动力']
  },
  {
    scale: '基于 Csikszentmihalyi 心流理论',
    text: '明白了。评估<b>心流体验</b>——回想过去一小时的学习，你有多频繁出现“全神贯注、忘记时间”的状态？',
    options: ['几乎全程沉浸，非常投入', '偶尔进入状态，但容易被打断', '很难集中注意力，频繁走神', '完全无法投入，坐不住']
  },
  {
    scale: '行为意向评估',
    text: '最后一题。面对当前状态，你接下来 1-2 小时打算怎么安排？',
    options: ['强行硬核学习，冲刺任务', '看些轻松的教学视频放松', '彻底休息，出去走走', '先补一觉恢复精力']
  }
];

const currentStep = ref(0);
const userAnswers = ref<string[]>([]);
const chatHistory = ref<ChatMessage[]>([]);

// 六维指标卡片配置
const metricCards = [
  { key: 'stressLevel', label: '压力负荷', icon: CloudOutlined, color: 'orange' },
  { key: 'energyLevel', label: '能量水平', icon: ThunderboltOutlined, color: 'green' },
  { key: 'focusLevel', label: '专注状态', icon: FireOutlined, color: 'blue' },
  { key: 'cognitiveLoad', label: '认知负荷', icon: DashboardOutlined, color: 'purple' },
  { key: 'flowScore', label: '心流指数', icon: RocketOutlined, color: 'cyan' },
  { key: 'emotionScore', label: '情绪效价', icon: BulbOutlined, color: 'pink' },
];

// ================================================================
// 指标数值转定性描述
// ================================================================
const getMetricLabel = (key: string, value: number) => {
  if (typeof value !== 'number') return '未知';

  // 负向指标（分数越高说明状态越差）：压力负荷、认知负荷
  const isNegativeMetric = ['stressLevel', 'cognitiveLoad'].includes(key);

  if (isNegativeMetric) {
    if (value >= 8) return '严重过载';
    if (value >= 6) return '负荷偏高';
    if (value >= 4) return '适中可控';
    return '轻松无压';
  } else {
    // 正向指标（分数越高说明状态越好）：能量、专注、心流、情绪
    if (value >= 8) return '状态极佳';
    if (value >= 6) return '表现良好';
    if (value >= 4) return '稍显不足';
    return '状态低迷';
  }
};

// ================================================================
// 文本兜底：把偶发英文结果转成中文
// ================================================================
const escapeHtml = (value: string): string =>
  value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const formatUserMessage = (value: string): string => escapeHtml(value).replace(/\n/g, '<br/>');

const translateMonitorStatus = (status: unknown): string => {
  const text = String(status ?? '').trim().toLowerCase();
  const map: Record<string, string> = {
    normal: '正常',
    yawn: '打哈欠',
    fatigue: '闭眼疲劳',
    no_face: '离屏',
    noface: '离屏',
    absent: '离屏',
  };
  return map[text] || String(status ?? '正常');
};

const translatePsychoText = (value: unknown): string => {
  const raw = String(value ?? '').trim();
  if (!raw) return '';

  const normalized = raw.replace(/\r?\n+/g, ' ').replace(/\s+/g, ' ').trim();

  const exactMap: Record<string, string> = {
    'High cognitive load': '认知负荷较高',
    'Low energy and motivation': '能量与学习动机偏低',
    'Poor focus and flow': '专注状态与心流体验较差',
    'Subjective-objective data discrepancy': '主客观数据存在不一致',
    'Subjective-objective discrepancy': '主客观数据存在不一致',
    'Yerkes-Dodson law': 'Yerkes-Dodson 唤醒理论',
    'Yerkes-Dodson Law': 'Yerkes-Dodson 唤醒理论',
    'Flow theory': '心流理论',
    'Flow Theory': '心流理论',
    'Csikszentmihalyi Flow Theory': 'Csikszentmihalyi 心流理论',
    'NASA-TLX': 'NASA-TLX 认知负荷量表',
    'PANAS': 'PANAS 情绪量表',
    'Karolinska Sleepiness Scale': 'Karolinska 嗜睡量表',
  };

  if (exactMap[normalized]) return exactMap[normalized];

  return normalized
    .replace(/The user is experiencing/gi, '当前用户处于')
    .replace(/high cognitive load/gi, '高认知负荷')
    .replace(/low energy and motivation/gi, '低能量与低动机状态')
    .replace(/low energy/gi, '低能量状态')
    .replace(/poor focus and flow/gi, '专注与心流状态较差')
    .replace(/poor focus/gi, '注意力较差')
    .replace(/negative emotions/gi, '负面情绪')
    .replace(/significant cognitive fatigue and stress/gi, '较明显的认知疲劳与压力')
    .replace(/However, the objective physiological data shows no fatigue events/gi, '但客观生理数据暂未显示明显疲劳事件')
    .replace(/objective physiological data/gi, '客观生理数据')
    .replace(/no fatigue events/gi, '未检测到明显疲劳事件')
    .replace(/which contradicts/gi, '这与')
    .replace(/This discrepancy suggests/gi, '这种差异提示')
    .replace(/Recommendations prioritize/gi, '建议优先')
    .replace(/rest and activity/gi, '休息与低负荷活动')
    .replace(/while advising improved objective monitoring/gi, '同时建议优化客观监测')
    .replace(/subjective reports/gi, '主观反馈')
    .replace(/severe fatigue/gi, '明显疲劳')
    .replace(/difficulty concentrating/gi, '注意力难以集中')
    .replace(/subjective-objective data discrepancy/gi, '主客观数据存在不一致')
    .replace(/discrepancy/gi, '差异')
    .replace(/suggests potential issues with/gi, '提示可能存在')
    .replace(/objective monitoring/gi, '客观监测')
    .replace(/short duration/gi, '监测时长较短')
    .replace(/low frame rate/gi, '采样频率偏低')
    .replace(/or subjective overreporting/gi, '或主观报告偏重')
    .replace(/\s+/g, ' ')
    .trim();
};

const toIntScore = (value: unknown, fallback = 5): number => {
  const num = Number(value);
  if (Number.isNaN(num)) return fallback;
  return Math.max(1, Math.min(10, Math.round(num)));
};

const normalizeStringArray = (value: unknown, fallback: string[] = []): string[] => {
  if (!Array.isArray(value)) return fallback;
  const list = value.map(item => translatePsychoText(item)).filter(Boolean);
  return list.length ? list : fallback;
};

const normalizeSuggestions = (value: unknown, fallbackText = '暂无') => {
  if (Array.isArray(value)) {
    return {
      immediate: translatePsychoText(value[0] || fallbackText) || fallbackText,
      shortTerm: translatePsychoText(value[1] || '暂无') || '暂无',
      habit: translatePsychoText(value[2] || '暂无') || '暂无',
    };
  }

  if (typeof value === 'string') {
    return {
      immediate: translatePsychoText(value) || fallbackText,
      shortTerm: '暂无',
      habit: '暂无',
    };
  }

  if (value && typeof value === 'object') {
    const obj = value as Record<string, unknown>;
    return {
      immediate: translatePsychoText(obj.immediate || obj.now || obj.current || fallbackText) || fallbackText,
      shortTerm: translatePsychoText(obj.shortTerm || obj.week || obj.thisWeek || '暂无') || '暂无',
      habit: translatePsychoText(obj.habit || obj.longTerm || obj.longterm || '暂无') || '暂无',
    };
  }

  return {
    immediate: fallbackText,
    shortTerm: '暂无',
    habit: '暂无',
  };
};

const normalizePsychoResult = (data: any) => {
  const result = { ...(data || {}) };

  result.stressLevel = toIntScore(result.stressLevel, 5);
  result.energyLevel = toIntScore(result.energyLevel, 5);
  result.focusLevel = toIntScore(result.focusLevel, 5);
  result.cognitiveLoad = toIntScore(result.cognitiveLoad ?? result.stressLevel, 5);
  result.flowScore = toIntScore(result.flowScore ?? result.focusLevel, 5);
  result.emotionScore = toIntScore(result.emotionScore, 5);

  result.theories = normalizeStringArray(result.theories, [
    'Yerkes-Dodson 唤醒理论',
    'NASA-TLX 认知负荷量表',
    'Csikszentmihalyi 心流理论',
  ]);

  result.riskFlags = normalizeStringArray(result.riskFlags, []);
  result.verdict = translatePsychoText(result.verdict || result.suggestion || '评估完成') || '评估完成';
  result.suggestions = normalizeSuggestions(result.suggestions, translatePsychoText(result.suggestion || '暂无') || '暂无');

  return result;
};

// ================================================================
// 对话控制逻辑
// ================================================================
const initChat = () => {
  const msgs: ChatMessage[] = [];

  // 获取已存在的监测秒数
  const monSec = fatigueStats.value?.monitorSeconds || 0;
  const learningTip = learningProfile.value
    ? `<br/><span class="chat-muted">已同步近 ${learningProfile.value.days || LEARNING_PROFILE_DAYS} 天学习画像：${escapeHtml(learningProfile.value.insight?.riskLabel || '学习背景已关联')}。</span>`
    : `<br/><span class="chat-muted">暂未读取到近期学习画像，本次会先完成状态检测。</span>`;

  if (monSec >= MIN_VALID_SECONDS) {
    // 场景3：数据充足
    msgs.push({
      role: 'ai',
      content: `<b>检测到有效的生理监测记录</b><br/>
                已累计监测 ${Math.round(monSec/60)} 分钟，包含 ${fatigueStats.value.fatigueCount} 次疲劳事件。<br/>
                为了获得最严谨的认知画像，您希望如何进行本次评估？${learningTip}`
    });
    // 此时 UI 应渲染两个按钮：[双通道深度融合分析] 和 [纯主观速测]

  } else if (monSec > 0 && monSec < MIN_VALID_SECONDS) {
    // 场景2：数据不足
    msgs.push({
      role: 'ai',
      content: `<b>生理监测数据样本量不足</b><br/>
                当前仅监测了 ${monSec} 秒，未达到建立可靠基线的最低时长（3分钟）。强制融合可能影响准确率。<br/>
                建议您进行【纯主观评估】，或前往看课页继续积累数据。${learningTip}`
    });
    // 此时 UI 应渲染按钮：[纯主观速测]

  } else {
    // 场景1：毫无数据
    msgs.push({
      role: 'ai',
      content: `<b>暂无客观生理监测数据</b><br/>
                系统未接收到您的摄像头疲劳监测记录。您可以进行基于量表的【纯主观评估】。${learningTip}`
    });
    // 此时 UI 应渲染按钮：[纯主观速测]
  }

  chatHistory.value = msgs;
};

// 当用户点击选择模式按钮后触发
const handleModeSelection = (mode: 'dual' | 'subjective') => {
  selectedMode.value = mode;

  const modeText = mode === 'dual' ? '双通道深度融合分析' : '纯主观评估';
  chatHistory.value.push({ role: 'user', content: `我选择：${modeText}` });

  // 确认模式后，正式抛出第一道题
  setTimeout(() => {
    chatHistory.value.push({ role: 'ai', content: questions[0].text });
    scrollToBottom();
  }, 500);
};


const resetAssessment = () => {
  currentStep.value = 0;
  userAnswers.value = [];
  analysisData.value = null;
  userText.value = '';

  // ======== 新增这一行：重置模式选择 ========
  selectedMode.value = null;

  if (radarChart) radarChart.dispose();
  if (timelineChart) timelineChart.dispose();
  radarChart = null;
  timelineChart = null;

  initChat();
  scrollToBottom();
};

const scrollToBottom = () => {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight;
    }
  });
};

const handleAnswer = (answer: string) => {
  chatHistory.value.push({ role: 'user', content: formatUserMessage(answer) });
  userAnswers.value.push(answer);
  scrollToBottom();
  proceedToNextStep();
};

const handleCustomAnswer = () => {
  if (!userText.value.trim()) return;

  const answer = userText.value.trim();
  userText.value = '';

  chatHistory.value.push({ role: 'user', content: formatUserMessage(answer) });
  userAnswers.value.push(answer);
  scrollToBottom();
  proceedToNextStep();
};

const proceedToNextStep = () => {
  currentStep.value++;

  if (currentStep.value < questions.length) {
    setTimeout(() => {
      chatHistory.value.push({ role: 'ai', content: questions[currentStep.value].text });
      scrollToBottom();
    }, 400);
  } else {
    submitToAI();
  }
};

// ================================================================
// 发送六维分析请求
// ================================================================
const analysisData = ref<any>(null);
const radarChartRef = ref<HTMLElement | null>(null);
const timelineChartRef = ref<HTMLElement | null>(null);
let radarChart: echarts.ECharts | null = null;
let timelineChart: echarts.ECharts | null = null;

// 👇 把这段完整覆盖原来的 buildFatigueContext
const buildFatigueContext = (): string => {
  // 严谨控制：如果用户选择了纯主观模式，或者没有生理数据，严禁向 AI 传递客观数据
  if (selectedMode.value === 'subjective' || !fatigueStats.value) {
    return '【说明】本次评估采取纯主观量表模式，无客观生理数据，请仅基于上述五项问卷结果进行六维画像构建。';
  }

  // 走到这里，说明是双通道模式且存在数据
  const s = fatigueStats.value;
  const dur = monitorDurationMin.value;
  const focus = computedFocusRate.value;
  const eventCount = s.events?.length || 0;
  const monSec = s.monitorSeconds || 0;

  let ctx = `【客观生理疲劳监测报告 — MediaPipe FaceMesh 实时面部关键点检测】\n`;
  ctx += `- 摄像头累计监测时长：${dur} 分 ${monSec % 60} 秒（仅计算摄像头开启时间）\n`;
  ctx += `- 检测总帧数：${s.totalDetections || 0}（约 2 FPS）\n`;
  ctx += `- 打哈欠次数：${s.yawnCount || 0} 次（MAR > 0.50 触发）\n`;
  ctx += `- 闭眼疲劳次数：${s.fatigueCount || 0} 次（EAR < 0.20 持续 1.5 秒触发）\n`;
  ctx += `- 离开屏幕次数：${s.noFaceCount || 0} 次\n`;
  ctx += `- 计算专注率：${focus}%（正常帧占比）\n`;
  ctx += `- 疲劳事件总数：${eventCount} 次状态切换\n`;
  ctx += `- 最终检测状态：${translateMonitorStatus(s.lastStatus)}\n`;

  if (dur > 0) {
    const density = ((Number(s.yawnCount || 0) + Number(s.fatigueCount || 0)) / dur).toFixed(2);
    ctx += `- 疲劳事件密度：${density} 次/分钟\n`;
    if (parseFloat(density) > 0.3) {
      ctx += `  ⚠ 疲劳密度偏高，表明学生在监测期间频繁出现生理疲劳信号\n`;
    }
  }

  return ctx;
};

// ================================================================
// 将分析结果持久化到数据库
// ================================================================
const saveAnalysisRecord = async (result: any) => {
  const userId = getCurrentUserId();
  if (!userId) {
    console.warn('未检测到登录用户, 跳过评估结果保存');
    return;
  }

  // 仅在 dual 模式时保存客观数据快照, 避免 subjective 模式里混入脏数据
  const isDual = selectedMode.value === 'dual' && fatigueStats.value;
  const snapshot = isDual
    ? JSON.stringify({
      events: fatigueStats.value.events || [],
      earSamples: fatigueStats.value.earSamples || [],
      marSamples: fatigueStats.value.marSamples || [],
      lastStatus: fatigueStats.value.lastStatus,
      sessionStart: fatigueStats.value.sessionStart,
      totalDetections: fatigueStats.value.totalDetections || 0,
    })
    : null;

  const payload = {
    userId,
    assessmentMode: selectedMode.value || 'subjective',

    // 主观问卷
    answersJson: JSON.stringify(userAnswers.value),

    // 六维指标
    stressLevel: result.stressLevel,
    energyLevel: result.energyLevel,
    focusLevel: result.focusLevel,
    cognitiveLoad: result.cognitiveLoad,
    flowScore: result.flowScore,
    emotionScore: result.emotionScore,

    // 结论
    verdict: result.verdict || '',
    theoriesJson: JSON.stringify(result.theories || []),
    riskFlagsJson: JSON.stringify(result.riskFlags || []),
    suggestionsJson: JSON.stringify(result.suggestions || {}),

    // 客观数据快照
    fatigueSnapshot: snapshot,
    monitorSeconds: isDual ? (fatigueStats.value.monitorSeconds || 0) : 0,
    yawnCount: isDual ? (fatigueStats.value.yawnCount || 0) : 0,
    fatigueCount: isDual ? (fatigueStats.value.fatigueCount || 0) : 0,
    focusRate: isDual ? computedFocusRate.value : 0,
    learningProfileDays: learningProfile.value?.days || LEARNING_PROFILE_DAYS,
    learningContextSummary: summarizeLearningProfile(),
    learningProfileSnapshot: learningProfile.value ? JSON.stringify(buildLearningProfileSnapshot()) : null,
  };

  try {
    const recordId = await request.post<number, number>('/mental-state/save', payload);
    console.log('[MentalState] 评估记录已保存, id =', recordId);
    message.success('评估结果已存档');
  } catch (e) {
    // 保存失败不影响前端展示, 仅记录日志
    console.error('[MentalState] 保存异常:', e);
  }
};

const submitToAI = async () => {
  isAnalyzing.value = true;
  const fatigueContext = buildFatigueContext();
  const learningContext = buildLearningContext();

  const finalContext = `
【主观量表评估结果】
Q1 主观警觉度（KSS）：${userAnswers.value[0] || '未填写'}
Q2 认知负荷（NASA-TLX）：${userAnswers.value[1] || '未填写'}
Q3 情绪效价（PANAS）：${userAnswers.value[2] || '未填写'}
Q4 心流体验（Flow Short Scale）：${userAnswers.value[3] || '未填写'}
Q5 行为意向：${userAnswers.value[4] || '未填写'}

${fatigueContext}

${learningContext}

请综合以上【主观量表】、【客观生理数据】和【学习画像背景】进行六维认知状态评估。
当主观自评与客观生理数据存在矛盾时（例如学生自评“满血复活”但打哈欠 5 次），请在 riskFlags 和 verdict 中明确指出“主客观数据存在不一致”。
当学习状态和学习画像存在冲突时（例如学生疲劳高但薄弱点很多），请优先给出低负荷补强路径，而不是泛泛建议继续努力。
返回的 suggestions 必须体现“当前状态适合做什么学习任务、不适合做什么、下一步从哪个薄弱点或推荐任务开始”。
请确保所有返回内容使用简体中文。
  `.trim();

  try {
    const response = await fetch(`${API_BASE_URL}/ai/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
      },
      body: JSON.stringify({ question: finalContext, type: 'psycho' })
    });

    if (!response.ok) {
      throw new Error(`请求失败：${response.status}`);
    }

    const reader = response.body?.getReader();
    const decoder = new TextDecoder('utf-8');
    let jsonBuffer = '';

    if (!reader) {
      throw new Error('网络异常');
    }

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      jsonBuffer += decoder.decode(value, { stream: true });
    }

    const cleanJson = jsonBuffer.replace(/```json/gi, '').replace(/```/g, '').trim();
    const parsedData = JSON.parse(cleanJson);

    // 兼容旧格式
    if (!parsedData.cognitiveLoad) parsedData.cognitiveLoad = parsedData.stressLevel || 5;
    if (!parsedData.flowScore) parsedData.flowScore = parsedData.focusLevel || 5;
    if (!parsedData.emotionScore) parsedData.emotionScore = 5;
    if (!parsedData.theories) parsedData.theories = [parsedData.theory || 'Yerkes-Dodson 唤醒理论'];
    if (!parsedData.riskFlags) parsedData.riskFlags = [];
    if (!parsedData.suggestions) {
      parsedData.suggestions = {
        immediate: parsedData.suggestion || '暂无',
        shortTerm: '暂无',
        habit: '暂无'
      };
    }
    if (!parsedData.verdict) parsedData.verdict = parsedData.suggestion || '评估完成';

    const normalizedData = normalizePsychoResult(parsedData);

    analysisData.value = normalizedData;

    // ===== 新增: 异步持久化到数据库 (失败不阻塞 UI) =====
    saveAnalysisRecord(normalizedData);

    chatHistory.value.push({
      role: 'ai',
      content: `分析完成！请查看右侧仪表盘的六维认知画像。<br/><b>总结：</b>${escapeHtml(normalizedData.verdict)}`
    });

    nextTick(() => {
      renderRadarChart(normalizedData);
      renderTimelineChart();
    });
  } catch (error) {
    console.error('AI 分析失败:', error);
    message.error('AI 分析失败，请检查后端服务');
    chatHistory.value.push({
      role: 'ai',
      content: '抱歉，分析引擎遇到异常，请稍后重试。'
    });
  } finally {
    isAnalyzing.value = false;
    scrollToBottom();
  }
};

// ================================================================
// ECharts 渲染
// ================================================================
const renderRadarChart = (data: any) => {
  if (!radarChartRef.value) return;

  if (radarChart) radarChart.dispose();
  radarChart = echarts.init(radarChartRef.value);

  radarChart.setOption({
    tooltip: { trigger: 'item' },
    radar: {
      indicator: [
        { name: '压力负荷', max: 10 },
        { name: '能量水平', max: 10 },
        { name: '专注状态', max: 10 },
        { name: '认知负荷', max: 10 },
        { name: '心流指数', max: 10 },
        { name: '情绪效价', max: 10 },
      ],
      radius: '72%',
      splitNumber: 5,
      splitArea: {
        areaStyle: {
          color: [
            'rgba(99,102,241,0.02)',
            'rgba(99,102,241,0.04)',
            'rgba(99,102,241,0.06)',
            'rgba(99,102,241,0.08)',
            'rgba(99,102,241,0.10)'
          ]
        }
      },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      axisName: { color: '#64748b', fontWeight: 'bold', fontSize: 15 }
    },
    series: [
      {
        type: 'radar',
        symbol: 'circle',
        symbolSize: 6,
        data: [
          {
            value: [
              data.stressLevel,
              data.energyLevel,
              data.focusLevel,
              data.cognitiveLoad,
              data.flowScore,
              data.emotionScore
            ],
            name: '当前认知画像',
            itemStyle: { color: '#6366f1' },
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(99,102,241,0.35)' },
                  { offset: 1, color: 'rgba(99,102,241,0.05)' }
                ]
              }
            },
            lineStyle: { width: 2, color: '#6366f1' }
          }
        ]
      }
    ]
  });
};

const renderTimelineChart = () => {
  if (!timelineChartRef.value || !fatigueStats.value) return;
  if (timelineChart) timelineChart.dispose();

  const events = fatigueStats.value.events || [];
  const earSamples = fatigueStats.value.earSamples || [];
  const marSamples = fatigueStats.value.marSamples || [];
  if (events.length === 0 && earSamples.length === 0 && marSamples.length === 0) return;

  timelineChart = echarts.init(timelineChartRef.value);

  const validTime = (value: unknown): value is number =>
    typeof value === 'number' && Number.isFinite(value) && value > 0;
  const timestamps = [
    ...events.map((e: any) => e?.t),
    ...earSamples.map((s: any) => s?.t),
    ...marSamples.map((s: any) => s?.t)
  ].filter(validTime);
  const fallbackTime =
    (validTime(fatigueStats.value.sessionStart) ? fatigueStats.value.sessionStart : null) ||
    (timestamps.length > 0 ? Math.min(...timestamps) : null) ||
    Date.now();
  const toTime = (t: unknown) => validTime(t) ? t : fallbackTime;
  const formatClock = (value: unknown, withSeconds = false) => {
    const date = new Date(toTime(value));
    const pad = (n: number) => String(n).padStart(2, '0');
    const base = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
    return withSeconds ? `${base}:${pad(date.getSeconds())}` : base;
  };

  const yawnData: any[] = [];
  const fatigueData: any[] = [];
  const noFaceData: any[] = [];

  events.forEach((e: any) => {
    const time = toTime(e.t);
    if (e.type === 'yawn') yawnData.push([time, 3]);
    else if (e.type === 'fatigue') fatigueData.push([time, 2]);
    else noFaceData.push([time, 1]);
  });

  const earLineData = earSamples.map((s: any) => [
    toTime(s.t),
    Number(s.v || 0)
  ]);
  const marLineData = marSamples.map((s: any) => [
    toTime(s.t),
    Number(s.v || 0)
  ]);

  timelineChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        let html = `${formatClock(params[0]?.value?.[0] ?? params[0]?.axisValue, true)}<br/>`;
        params.forEach((p: any) => {
          html += `${p.marker} ${p.seriesName}: ${
            (p.seriesName === 'EAR 趋势' || p.seriesName === 'MAR 趋势')
              ? Number(p.value?.[1] || 0).toFixed(3)
              : '事件'
          }<br/>`;
        });
        return html;
      }
    },
    legend: {
      data: ['打哈欠', '闭眼疲劳', '离屏', 'EAR 趋势', 'MAR 趋势'],
      top: 0,
      textStyle: { fontSize: 11, color: '#64748b' }
    },
    grid: { left: 50, right: 30, top: 40, bottom: 30 },
    xAxis: {
      type: 'time',
      name: '时间',
      nameTextStyle: { color: '#94a3b8', fontSize: 11 },
      axisLabel: {
        color: '#94a3b8',
        fontSize: 11,
        formatter: (value: number) => formatClock(value)
      },
      splitLine: { lineStyle: { color: '#f1f5f9' } }
    },
    yAxis: [
      {
        type: 'value',
        name: '事件',
        min: 0,
        max: 4,
        axisLabel: {
          color: '#94a3b8',
          fontSize: 11,
          formatter: (v: number) => ['', '离屏', '闭眼', '哈欠', ''][v] || ''
        },
        splitLine: { lineStyle: { color: '#f1f5f9' } }
      },
      {
        type: 'value',
        name: 'EAR/MAR',
        min: 0,
        max: 1,
        position: 'right',
        axisLabel: { color: '#94a3b8', fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '打哈欠',
        type: 'scatter',
        data: yawnData,
        symbolSize: 12,
        itemStyle: { color: '#f59e0b' }
      },
      {
        name: '闭眼疲劳',
        type: 'scatter',
        data: fatigueData,
        symbolSize: 12,
        itemStyle: { color: '#ef4444' }
      },
      {
        name: '离屏',
        type: 'scatter',
        data: noFaceData,
        symbolSize: 10,
        itemStyle: { color: '#94a3b8' }
      },
      {
        name: 'EAR 趋势',
        type: 'line',
        yAxisIndex: 1,
        data: earLineData,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.5, color: '#6366f1', opacity: 0.6 },
        areaStyle: { color: 'rgba(99,102,241,0.08)' },
        markLine: {
          silent: true,
          data: [{ yAxis: 0.2, name: 'EAR 阈值', lineStyle: { color: '#ef4444', type: 'dashed', width: 1 } }],
          label: { formatter: 'EAR=0.20', fontSize: 10, color: '#ef4444' }
        }
      },
      {
        name: 'MAR 趋势',
        type: 'line',
        yAxisIndex: 1,
        data: marLineData,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 1.5, color: '#f59e0b', opacity: 0.55 },
        areaStyle: { color: 'rgba(245,158,11,0.06)' },
        markLine: {
          silent: true,
          data: [{ yAxis: 0.5, name: 'MAR 阈值', lineStyle: { color: '#f59e0b', type: 'dashed', width: 1 } }],
          label: { formatter: 'MAR=0.50', fontSize: 10, color: '#f59e0b' }
        }
      }
    ]
  });
};

// ================================================================
// 生命周期
// ================================================================
const handleResize = () => {
  radarChart?.resize();
  timelineChart?.resize();
};

onMounted(async () => {
  await Promise.all([loadFatigueStats(), loadLearningProfile()]);
  initChat();
  scrollToBottom();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  radarChart?.dispose();
  timelineChart?.dispose();
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

/* 1. 统一晨雾留白背景，锁定页面总高度 */
.edu-app {
  height: calc(100vh - 82px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: linear-gradient(120deg, #ffffff 0%, #f1f5f9 100%);
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #0f172a;
}

/* 2. 纯白无毛玻璃的扁平质感卡片 */
.glass-panel {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.03);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}

/* 3. 主体布局自适应撑满 */
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
}

.page-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* ============ 顶部 Banner 紧凑化改造 ============ */
.page-intro {
  padding: 16px 20px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.intro-copy {
  flex: 1;
}

.overline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  padding: 4px 10px;
  border-radius: 5px;
  background: rgba(91, 108, 255, 0.1);
  color: #5b6cff;
  font-size: 11px;
  font-weight: 800;
}

.page-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
  font-weight: 800;
  color: #0f172a;
}

.subtitle {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.55;
  color: #64748b;
  max-width: 720px;
}

.physio-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.badge-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 700;
}

.pill-amber {
  background: #fef3c7;
  color: #92400e;
}

.pill-red {
  background: #fee2e2;
  color: #991b1b;
}

.pill-gray {
  background: #f1f5f9;
  color: #475569;
}

.pill-green {
  background: #dcfce7;
  color: #166534;
}

.pill-blue {
  background: #dbeafe;
  color: #1e40af;
}

.pill-violet {
  background: #ede9fe;
  color: #5b21b6;
}

/* 右侧的数据统计块同步压缩 */
.hero-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.mini-stat {
  border-radius: 5px;
  min-width: 90px;
  padding: 10px 14px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px solid rgba(226, 232, 240, 0.86);
}

.mini-label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 2px;
}

.mini-value {
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.mini-value small {
  font-size: 10px;
  color: #94a3b8;
}

.content-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 1.1fr);
  gap: 22px;
  height: 100%;
}

.chat-section {
  border-radius: 5px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.chat-header {
  padding: 18px 22px;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  color: #0f172a;
  font-size: 17px;
}

.header-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  color: #fff;
}

.pulse-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.13);
}

.reset-btn {
  border-radius: 5px;
  border: 1px solid #dbe4f0;
  background: #fff;
  color: #64748b;
  padding: 8px 14px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: 0.2s;
}

.reset-btn:hover {
  border-color: #93c5fd;
  color: #2563eb;
}

.chat-window {
  flex: 1;
  overflow-y: auto;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.msg-bubble {
  display: flex;
  gap: 12px;
  max-width: 92%;
}

.msg-bubble.user {
  align-self: flex-end;
}

.avatar {
  border-radius: 5px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.ai-avatar {
  background: linear-gradient(135deg, rgba(91, 108, 255, 0.14), rgba(57, 198, 255, 0.14));
  color: #5b6cff;
}

.user-avatar {
  background: #edf2f7;
  color: #64748b;
}

.content {
  border-radius: 5px;
  padding: 14px 16px;
  line-height: 1.65;
  font-size: 15px;
  word-break: break-word;
}

:deep(.chat-muted) {
  color: #64748b;
  font-size: 12px;
}

.ai .content {
  background: #f8fbff;
  border: 1px solid #e6edf7;
  color: #334155;
  border-top-left-radius: 6px;
}

.user .content {
  background: linear-gradient(135deg, #5b6cff, #39c6ff);
  color: #fff;
  border-top-right-radius: 6px;
}

.analyzing-box {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #5b6cff !important;
  font-weight: 700;
}

.spin-icon {
  animation: spin 1s linear infinite;
  font-size: 18px;
}

.analyze-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.analyze-sub {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.input-area {
  padding: 18px 22px 22px;
  border-top: 1px solid #eef2f7;
  background: linear-gradient(180deg, rgba(248, 251, 255, 0.8), rgba(255, 255, 255, 0.7));
}

.question-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.q-scale {
  font-size: 11px;
  font-weight: 700;
  color: #6366f1;
  background: rgba(99, 102, 241, 0.08);
  padding: 3px 10px;
  border-radius: 6px;
}

.q-step {
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
}

.quick-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.option-btn {
  border: 1px solid #dbe4f0;
  background: #fff;
  color: #475569;
  padding: 10px 16px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
}

.option-btn:hover {
  transform: translateY(-2px);
  border-color: #93c5fd;
  color: #2563eb;
  background: #f8fbff;
}

.custom-input {
  display: flex;
  gap: 12px;
}

.custom-input input {
  border-radius: 5px;
  flex: 1;
  height: 44px;
  border: 1px solid #dbe4f0;
  background: #fff;
  padding: 0 16px;
  outline: none;
  font-size: 14px;
}

.custom-input input:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.08);
}

.send-btn {
  border-radius: 5px;
  height: 44px;
  padding: 0 18px;
  border: none;
  background: linear-gradient(135deg, #5b6cff, #39c6ff);
  color: #fff;
  font-weight: 800;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dashboard-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}

.dashboard-section::-webkit-scrollbar {
  width: 6px;
}

.dashboard-section::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.empty-card {
  border-radius: 5px;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 40px;
}

.empty-icon {
  font-size: 54px;
  color: #bcc8d8;
  margin-bottom: 14px;
}

.empty-card h3 {
  margin: 0 0 10px;
  font-size: 26px;
}

.empty-card p {
  color: #64748b;
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 18px;
}

.method-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.m-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(99, 102, 241, 0.06);
  color: #6366f1;
  border: 1px solid rgba(99, 102, 241, 0.15);
}

.learning-context-card {
  width: min(100%, 520px);
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid #e6edf7;
  border-radius: 8px;
  background: #f8fbff;
  text-align: left;
}

.learning-context-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.learning-context-top strong {
  font-size: 16px;
  color: #0f172a;
}

.learning-context-top span {
  font-size: 11px;
  font-weight: 800;
  color: #5b21b6;
  background: #ede9fe;
  border-radius: 999px;
  padding: 4px 9px;
  white-space: nowrap;
}

.learning-context-card p {
  margin: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
}

.learning-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.learning-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 3px 9px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 11px;
  font-weight: 700;
}

.data-board {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.fade-in {
  animation: fadeIn 0.35s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
  }
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.metric-card {
  border-radius: 5px;
  padding: 14px 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.m-icon {
  border-radius: 5px;
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.m-icon.orange {
  background: #fff7ed;
  color: #f97316;
}

.m-icon.green {
  background: #ecfdf5;
  color: #10b981;
}

.m-icon.blue {
  background: #eff6ff;
  color: #3b82f6;
}

.m-icon.purple {
  background: #f5f3ff;
  color: #7c3aed;
}

.m-icon.cyan {
  background: #ecfeff;
  color: #06b6d4;
}

.m-icon.pink {
  background: #fdf2f8;
  color: #ec4899;
}

.m-info {
  flex: 1;
  min-width: 0;
}

.m-val {
  font-size: 21px;
  font-weight: 800;
  color: #0f172a;
}

.m-val small {
  font-size: 11px;
  margin-left: 2px;
  color: #94a3b8;
}

.m-lbl {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.m-bar-bg {
  width: 100%;
  height: 4px;
  border-radius: 4px;
  background: #f1f5f9;
}

.m-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 1s ease;
}

.m-bar-fill.orange {
  background: linear-gradient(90deg, #fbbf24, #f97316);
}

.m-bar-fill.green {
  background: linear-gradient(90deg, #34d399, #10b981);
}

.m-bar-fill.blue {
  background: linear-gradient(90deg, #60a5fa, #3b82f6);
}

.m-bar-fill.purple {
  background: linear-gradient(90deg, #a78bfa, #7c3aed);
}

.m-bar-fill.cyan {
  background: linear-gradient(90deg, #22d3ee, #06b6d4);
}

.m-bar-fill.pink {
  background: linear-gradient(90deg, #f472b6, #ec4899);
}

.physio-panel {
  border-radius: 5px;
  padding: 18px 20px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 8px;
}

.card-title-row h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 800;
}

.data-source-tag {
  font-size: 10px;
  color: #94a3b8;
  font-weight: 600;
  background: #f8fafc;
  padding: 3px 8px;
  border-radius: 6px;
}

.timeline-chart-box {
  width: 100%;
  height: 220px;
}

.chart-theory-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  border-radius: 5px;
  padding: 18px 20px;
}

.radar-box {
  width: 100%;
  height: 260px;
}

.theory-card {
  border-radius: 5px;
  padding: 18px 20px;
}

.theory-card h3 {
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 800;
}

.theory-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.theory-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  color: #334155;
  line-height: 1.6;
}

.theory-num {
  border-radius: 5px;
  width: 22px;
  height: 22px;
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
  font-size: 11px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.risk-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #dc2626;
  padding: 6px 10px;
  background: #fef2f2;
  border-radius: 8px;
}

.risk-icon {
  font-size: 14px;
}

.risk-item.safe {
  color: #16a34a;
  background: #f0fdf4;
}

.safe-icon {
  font-size: 14px;
}

.suggestion-card {
  border-radius: 5px;
  padding: 20px 22px;
}

.learning-evidence-card {
  border-radius: 5px;
  padding: 18px 20px;
}

.learning-evidence-card p {
  margin: 0 0 14px;
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
}

.learning-evidence-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.learning-evidence-item {
  min-height: 86px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e6edf7;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.learning-evidence-item span {
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.learning-evidence-item strong {
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
}

.learning-evidence-item em {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  font-style: normal;
}

.suggestion-card h3 {
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 800;
}

.verdict-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.06), rgba(6, 182, 212, 0.06));
  border-radius: 14px;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  line-height: 1.6;
}

.verdict-icon {
  font-size: 20px;
  color: #6366f1;
  flex-shrink: 0;
}

.suggestion-tiers {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.tier-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.tier-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 8px;
  white-space: nowrap;
  flex-shrink: 0;
  margin-top: 2px;
}

.tier-now {
  background: #fee2e2;
  color: #991b1b;
}

.tier-week {
  background: #fef3c7;
  color: #92400e;
}

.tier-habit {
  background: #dbeafe;
  color: #1e40af;
}

.tier-item p {
  margin: 0;
  font-size: 14px;
  color: #475569;
  line-height: 1.65;
}

.action-btn-group {
  margin-top: 8px;
}

.action-btn-group button {
  border-radius: 5px;
  width: 100%;
  height: 48px;
  font-weight: 800;
  font-size: 14px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.rest-btn {
  background: #ecfdf5;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.push-btn {
  background: linear-gradient(135deg, #5b6cff, #39c6ff);
  color: #fff;
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .chart-theory-row {
    grid-template-columns: 1fr;
  }

  .metrics-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* =======================================
   悬浮窗与全宽雷达图样式
======================================= */
.full-row-card {
  width: 100%;
}

.title-with-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
}

.enlarged-radar {
  width: 100%;
  height: 380px; /* 高度从原来的260px大幅增加，图表会更震撼 */
}

/* 浮窗触发按钮 */
.theory-hover-wrapper {
  position: relative;
}

.info-badge-btn {
  background: rgba(99, 102, 241, 0.08);
  color: #6366f1;
  border: 1px solid rgba(99, 102, 241, 0.15);
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.info-badge-btn:hover {
  background: #6366f1;
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}

/* 浮窗面板样式 */
.floating-panel {
  position: absolute;
  top: 130%;
  right: 0;
  width: 320px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
  border-radius: 10px;
  padding: 18px;
  z-index: 100;
  /* 初始隐藏，带下拉动画 */
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
}

/* 鼠标放上去时显示浮窗 */
.theory-hover-wrapper:hover .floating-panel {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
  pointer-events: auto;
}

/* 浮窗内部文字排版 */
.pop-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 800;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 6px;
}

.pop-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pop-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  line-height: 1.6;
}

.pop-num {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 10px;
  flex-shrink: 0;
  margin-top: 1px;
}

.pop-text {
  color: #475569;
}

.risk-text {
  color: #dc2626;
  background: #fef2f2;
  padding: 6px 10px;
  border-radius: 6px;
}

.safe-text {
  color: #16a34a;
  background: #f0fdf4;
  padding: 6px 10px;
  border-radius: 6px;
}

@media (max-width: 768px) {
  .page-intro {
    border-radius: 5px;
    padding: 24px 30px;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 24px;
  }

  .metrics-row {
    grid-template-columns: 1fr;
  }

  .header-inner {
    flex-wrap: wrap;
    height: auto;
    padding: 16px 20px;
  }

  .nav-links {
    order: 3;
    width: 100%;
    overflow-x: auto;
  }
}
</style>
