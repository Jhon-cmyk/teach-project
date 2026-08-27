<template>
  <div class="class-analysis-page modern-page">
    <div class="page-header">
      <div class="title-group">
        <h2><audio-outlined class="title-icon" /> 课堂录音 AI 分析</h2>
        <p class="subtitle">上传或实时录制课堂音频，AI 将自动转写并结合选定的教案，为您生成深度的教学诊断报告。</p>
      </div>
    </div>

    <div class="action-dashboard glass-panel">
      <div class="file-status">
        <div class="status-item" :class="{ active: selectedPlan }">
          <file-text-outlined class="s-icon" />
          <div class="s-info">
            <span class="label">参考教案</span>
            <span class="val" v-if="selectedPlan">已关联: 《{{ selectedPlan.title }}》</span>
            <span class="val text-muted" v-else>未关联 (建议关联以提升评价精度)</span>
          </div>
        </div>
        <div class="status-divider"></div>
        <div class="status-item" :class="{ active: audioUploaded || isRecording }">
          <sound-outlined class="s-icon" />
          <div class="s-info">
            <span class="label">课堂实录</span>
            <span class="val" v-if="isRecording" style="color: #ef4444;">正在录音中...</span>
            <span class="val" v-else-if="audioUploaded">音频已就绪</span>
            <span class="val text-muted" v-else>等待录制或上传</span>
          </div>
        </div>
        <div class="status-divider"></div>
      </div>

      <div class="action-buttons">
        <a-button
          size="large"
          :class="selectedPlan ? 'ghost-btn unlink-btn' : 'ghost-btn'"
          @click="selectedPlan ? clearSelectedPlan() : openPlanModal()"
        >
          <link-outlined v-if="!selectedPlan" />
          <disconnect-outlined v-else />
          {{ selectedPlan ? '取消关联' : '关联教案' }}
        </a-button>
        <a-button size="large" class="ghost-btn rec-btn" @click="openRecordModal" :disabled="isTranscribing || isAnalyzing">
          <audio-outlined /> 实时录音
        </a-button>
        <a-upload :show-upload-list="false" :customRequest="handleAudioUpload" :disabled="isRecording || isTranscribing || isAnalyzing">
          <a-button type="primary" size="large" class="upload-btn" :loading="isUploading">
            <cloud-upload-outlined /> 上传音频
          </a-button>
        </a-upload>
      </div>
    </div>

    <div class="analysis-workspace" v-if="audioUploaded || isTranscribing || transcript.length > 0">

      <div class="left-panel glass-panel">
        <div class="panel-header">
          <h3><align-left-outlined /> 课堂转写记录</h3>
          <span v-if="isTranscribing" class="status-text text-primary">引擎处理中...</span>
        </div>

        <div class="transcript-area scroll-y">
          <div v-if="isTranscribing" class="transcribing-state">
            <div class="wave-loader">
              <span class="bar"></span><span class="bar"></span><span class="bar"></span><span class="bar"></span><span class="bar"></span>
            </div>
            <p>语音识别引擎(ASR)正在提取并分离师生对话，请稍候...</p>
          </div>

          <div v-else-if="transcript.length > 0" class="transcript-list">
            <div v-for="(line, index) in transcript" :key="index" class="dialogue-row" :class="line.role">
              <div class="time-badge">{{ line.time }}</div>
              <div class="dialogue-bubble">
                <div class="speaker"><user-outlined v-if="line.role === 'student'"/> <span v-else>教师</span></div>
                <div class="text">{{ line.content }}</div>
              </div>
            </div>
          </div>

          <div v-else class="empty-transcript">
            未能解析出有效语音内容，请检查音频质量。
          </div>
        </div>
      </div>

      <div class="right-panel glass-panel">
        <div class="panel-header">
          <h3><radar-chart-outlined /> AI 课堂评价</h3>
          <div class="header-actions">
            <a-button
              v-if="currentStatus !== 'completed'"
              class="action-btn generate-btn"
              :loading="isAnalyzing"
              :disabled="transcript.length === 0 || !currentRecordId"
              @click="startGenerateReport"
            >
              <thunderbolt-outlined v-if="!isAnalyzing" /> 生成诊断分析
            </a-button>

            <a-button
              v-if="aiReport && currentStatus === 'completed'"
              class="action-btn save-btn"
              :disabled="savedToLibrary"
              @click="saveToResourceLibrary"
            >
              <folder-open-outlined /> {{ savedToLibrary ? '已保存' : '保存到资源库' }}
            </a-button>
          </div>
        </div>

        <div class="report-area scroll-y">
          <div v-if="isAnalyzing && !aiReport" class="analyzing-state">
            <div class="radar-spinner"></div>
            <p>大模型正在比对教案目标与实际授课内容，深度解析中...</p>
          </div>

          <div v-else-if="aiReport" class="report-content fade-in">

            <!-- 无教案提示条 -->
            <a-alert
              v-if="!selectedPlan"
              type="warning"
              show-icon
              class="no-plan-alert"
              message="未关联教案，已使用通用教学理论评估"
              description="当前报告不含知识点覆盖率与教案对齐度分析。重新关联教案后点击「生成诊断分析」可获得更精准的诊断。"
            />

            <!-- 统计卡片：动态读取 AI 返回的 reportStats -->
            <div class="stats-grid">
              <div class="stat-box">
                <span class="label">互动频次测算</span>
                <span
                  class="val"
                  :class="{
                    success:  reportStats?.interaction === '良好',
                    warning:  reportStats?.interaction === '一般',
                    danger:   reportStats?.interaction === '较差',
                    'text-muted': !reportStats,
                  }"
                >
                  {{ reportStats?.interaction ?? '分析中' }}
                </span>
              </div>
              <div class="stat-box">
                <span class="label">知识点覆盖率</span>
                <span
                  class="val"
                  :class="{
                    highlight:    reportStats?.coverage === '高',
                    warning:      reportStats?.coverage === '中',
                    danger:       reportStats?.coverage === '低',
                    'text-muted': !reportStats || reportStats?.coverage === 'N/A' || reportStats?.coverage === '待评估',
                  }"
                >
                  {{ reportStats?.coverage === 'N/A' ? '无教案' : (reportStats?.coverage ?? '待评估') }}
                </span>
              </div>
              <div class="stat-box">
                <span class="label">大模型综合评价</span>
                <span class="val warning">已生成</span>
              </div>
            </div>

            <!-- 报告正文：剥离 STATS 标记后按模块卡片化渲染 -->
            <div v-if="reportSections.length > 0" class="report-sections">
              <section
                v-for="section in reportSections"
                :key="section.key"
                class="report-section-card"
                :class="section.tone"
              >
                <div class="section-head">
                  <div class="section-index">{{ section.index }}</div>
                  <div class="section-heading">
                    <div class="section-title">{{ section.title }}</div>
                    <div class="section-subtitle">{{ section.subtitle }}</div>
                  </div>
                </div>
                <div class="section-body markdown-render doc-style report-section-md" v-html="section.html"></div>
              </section>
            </div>
            <div v-else class="markdown-render doc-style report-md" v-html="renderMd(cleanReport)"></div>
          </div>

          <div v-else class="empty-report">
            <bulb-outlined class="empty-icon" />
            <p>等待 AI 引擎出具教学诊断报告</p>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="welcome-wrapper glass-panel">
      <div class="welcome-content">
        <div class="welcome-header">
          <div class="icon-ring">
            <AudioOutlined class="main-icon" />
          </div>
          <h3>开始您的智能评课之旅</h3>
          <p>只需简单三步，让 AI 助教为您深度提取师生对话、复盘教学细节并出具诊断报告。</p>
        </div>

        <div class="steps-grid">
          <div class="step-card">
            <div class="step-icon-wrap bg-blue">
              <LinkOutlined />
            </div>
            <h4>1. 关联教案</h4>
            <p>选择您要讲授的教案，使 AI 评价能够精准对齐您的教学目标与重难点。</p>
          </div>
          <div class="step-card">
            <div class="step-icon-wrap bg-purple">
              <SoundOutlined />
            </div>
            <h4>2. 采集课堂录音</h4>
            <p>通过上方控制台实时录音，或直接上传已经录制好的本地音频文件。</p>
          </div>
          <div class="step-card">
            <div class="step-icon-wrap bg-green">
              <RadarChartOutlined />
            </div>
            <h4>3. 生成诊断分析</h4>
            <p>自动转写并分离师生对话，多维评估互动频次与知识点覆盖，获取改进建议。</p>
          </div>
        </div>
      </div>
    </div>

    <a-modal v-model:open="planModalVisible" title="从资源库关联教案" :footer="null" width="1000px" centered>
      <div class="plan-modal-body">
        <!-- 左侧：教案列表 -->
        <div class="plan-list-col">
          <div class="plan-filter-panel">
            <a-input
              v-model:value="planSearchKeyword"
              placeholder="搜索教案标题 / 内容"
              allow-clear
              size="large"
            >
              <template #prefix><search-outlined style="color:#94a3b8" /></template>
            </a-input>

            <div class="plan-filter-row">
              <a-select v-model:value="planTimeFilter" size="small" class="plan-filter-select">
                <a-select-option value="all">全部时间</a-select-option>
                <a-select-option value="7d">近 7 天</a-select-option>
                <a-select-option value="30d">近 30 天</a-select-option>
                <a-select-option value="90d">近 90 天</a-select-option>
              </a-select>
              <a-select v-model:value="planSortMode" size="small" class="plan-filter-select">
                <a-select-option value="newest">最新优先</a-select-option>
                <a-select-option value="oldest">最早优先</a-select-option>
                <a-select-option value="title">标题排序</a-select-option>
              </a-select>
            </div>

            <div class="plan-filter-meta">
              <span>共 {{ filteredPlans.length }} / {{ myPlans.length }} 份教案</span>
              <a-button v-if="hasPlanFilters" type="link" size="small" @click="resetPlanFilters">清空筛选</a-button>
            </div>
          </div>
          <div v-if="plansLoading" style="text-align:center;padding:40px 0;color:#94a3b8;">加载中...</div>
          <div v-else-if="filteredPlans.length > 0" class="plan-list">
            <div
              v-for="plan in filteredPlans"
              :key="plan.id"
              class="plan-card"
              :class="{ selected: previewPlan?.id === plan.id, confirmed: selectedPlan?.id === plan.id }"
              @click="previewPlanItem(plan)"
            >
              <div class="plan-info">
                <span class="p-title">{{ plan.title }}</span>
                <span class="p-date">{{ formatDateTime(plan.createTime) }}</span>
              </div>
              <check-circle-filled v-if="selectedPlan?.id === plan.id" class="check-icon" />
            </div>
          </div>
          <div v-else class="empty-plans">
            <inbox-outlined class="e-icon" />
            <p>{{ hasPlanFilters ? '没有匹配的教案，请调整搜索或筛选条件' : '您还没有生成过教案，请先前往「AI备课室」生成。' }}</p>
          </div>
        </div>

        <!-- 右侧：预览区 -->
        <div class="plan-preview-col">
          <div v-if="!previewPlan" class="plan-preview-empty">
            <file-text-outlined class="prev-empty-icon" />
            <p>点击左侧教案查看预览</p>
          </div>
          <template v-else>
            <div class="plan-preview-header">
              <span class="plan-preview-title">{{ previewPlan.title }}</span>
              <a-button type="primary" @click="confirmSelectPlan">
                <check-circle-filled /> 确认关联此教案
              </a-button>
            </div>
            <div class="plan-preview-content markdown-render doc-style" v-html="renderMd(previewPlan.content || '')"></div>
          </template>
        </div>
      </div>
    </a-modal>

    <a-modal v-model:open="recordModalVisible" :closable="false" :maskClosable="false" :footer="null" width="400px" centered wrapClassName="record-modal-wrap">
      <div class="record-container">
        <div class="mic-wrapper" :class="{ recording: isRecording }">
          <audio-outlined class="mic-icon" />
          <div class="ripple"></div>
          <div class="ripple delay"></div>
        </div>
        <h3 class="record-time">{{ formattedRecordTime }}</h3>
        <p class="record-hint">{{ isRecording ? '正在收音，请清晰讲述课堂内容...' : '准备就绪，点击下方按钮开始' }}</p>

        <div class="record-actions">
          <a-button shape="circle" size="large" class="btn-cancel" @click="closeRecordModal" v-if="!isRecording">
            <close-outlined />
          </a-button>

          <a-button v-if="!isRecording" type="primary" shape="round" size="large" class="btn-start" @click="startRecording">
            开始录制
          </a-button>

          <a-button v-else type="primary" danger shape="round" size="large" class="btn-stop" @click="stopRecording">
            结束并上传解析
          </a-button>
        </div>
      </div>
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import MarkdownIt from 'markdown-it';
import request from '@/utils/request';
import {
  AudioOutlined, FileTextOutlined, SoundOutlined, LinkOutlined,
  CloudUploadOutlined, AlignLeftOutlined, PlayCircleOutlined,
  RadarChartOutlined, ThunderboltOutlined, UserOutlined, BulbOutlined,
  CheckCircleFilled, InboxOutlined, CloseOutlined, FolderOpenOutlined,
  SearchOutlined, DisconnectOutlined
} from '@ant-design/icons-vue';

const md = new MarkdownIt({ breaks: true, html: true });
const renderMd = (text: string) => md.render(text || '');

// --- 状态 ---
const audioUploaded = ref(false);
const isUploading = ref(false);
const isTranscribing = ref(false);
const isAnalyzing = ref(false);
const currentRecordId = ref<number | null>(null);
const currentStatus = ref('');

const transcript = ref<any[]>([]);
const aiReport = ref('');

// ── 统计卡片动态化 ───────────────────────────────────────────────────
// AI 在报告第一行嵌入标记：<!--STATS:{"interaction":"良好","coverage":"低"}-->
// cleanReport 负责剥离这行标记供 Markdown 渲染；
// reportStats 负责把解析结果暴露给模板中的统计卡片。

// === 时间格式化函数 ===
const formatDateTime = (isoStr: string) => {
  if (!isoStr) return '';
  const date = new Date(isoStr);

  // 防御性处理：如果后端传来的不是有效时间，直接原样返回
  if (isNaN(date.getTime())) return isoStr;

  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');

  return `${y}-${m}-${d} ${h}:${min}`;
};

const reportStats = ref<{ interaction: string; coverage: string } | null>(null);

// ClassAnalysis.vue —— cleanReport computed（约第 327 行）
const cleanReport = computed(() => {
  const raw = aiReport.value;
  if (!raw) return '';

  // ✅ 正确：捕获 <!--STATS:{...}--> 整行并提取 JSON
  const match = raw.match(/^<!--STATS:(.+?)-->\n?/);
  if (match) {
    try {
      reportStats.value = JSON.parse(match[1]);
    } catch (e) {
      reportStats.value = null;
    }
    // ✅ 正确：把整行 STATS 标记从报告正文中剥离
    return raw.replace(/^<!--STATS:.+?-->\n?/, '');
  }

  reportStats.value = null;
  return raw;
});

const statusLabel = computed(() => {
  const map: Record<string, string> = {
    transcribing: '转写中...',
    transcribed:  '转写完成，可生成报告',
    analyzing:    'AI报告生成中...',
    completed:    '报告已生成',
    failed:       '任务失败',
  };
  return map[currentStatus.value] || '等待中';
});

type ReportSectionTone = 'purple' | 'blue' | 'cyan' | 'green' | 'orange' | 'slate';

type ReportSection = {
  key: string;
  index: number;
  title: string;
  subtitle: string;
  tone: ReportSectionTone;
  html: string;
};

const getSectionTone = (title: string): ReportSectionTone => {
  if (title.includes('整体评价')) return 'purple';
  if (title.includes('教案执行') || title.includes('教学逻辑')) return 'blue';
  if (title.includes('知识点覆盖')) return 'cyan';
  if (title.includes('互动')) return 'green';
  if (title.includes('亮点')) return 'green';
  if (title.includes('改进建议')) return 'orange';
  return 'slate';
};

const getSectionSubtitle = (title: string) => {
  if (title.includes('整体评价')) return '先看全局判断，再进入细节维度';
  if (title.includes('教案执行')) return '对照目标、重难点和课堂环节逐项查看';
  if (title.includes('知识点覆盖')) return '聚焦知识点是否讲到、讲透、讲完整';
  if (title.includes('教学逻辑')) return '看课堂推进是否顺畅、衔接是否自然';
  if (title.includes('互动')) return '关注提问方式、反馈频次和参与度';
  if (title.includes('亮点')) return '把值得保留的有效做法单独提炼出来';
  if (title.includes('改进建议')) return '按“问题来源 → 改进方向 → 参考做法”阅读最直观';
  return '按模块拆分阅读，避免整段文字堆叠';
};

const reportSections = computed<ReportSection[]>(() => {
  const raw = cleanReport.value?.trim();
  if (!raw) return [];

  const blocks = raw
    .split(/\n(?=###\s+)/g)
    .map((item) => item.trim())
    .filter(Boolean);

  const sections = blocks
    .map((block, index) => {
      const lines = block.split('\n');
      const titleLine = lines.shift() || '';
      if (!/^###\s+/.test(titleLine)) return null;

      const title = titleLine.replace(/^###\s+/, '').trim();
      const body = lines.join('\n').trim();

      return {
        key: `${index}-${title}`,
        index: index + 1,
        title,
        subtitle: getSectionSubtitle(title),
        tone: getSectionTone(title),
        html: renderMd(body),
      } as ReportSection;
    })
    .filter(Boolean) as ReportSection[];

  return sections;
});

// === 教案关联 ===
const planModalVisible = ref(false);
const myPlans = ref<any[]>([]);
const selectedPlan = ref<any>(null);
const previewPlan = ref<any>(null);
const plansLoading = ref(false);

const planSearchKeyword = ref('');
const planTimeFilter = ref<'all' | '7d' | '30d' | '90d'>('all');
const planSortMode = ref<'newest' | 'oldest' | 'title'>('newest');

const hasPlanFilters = computed(() => (
  Boolean(planSearchKeyword.value.trim()) ||
  planTimeFilter.value !== 'all' ||
  planSortMode.value !== 'newest'
));

const getPlanTime = (plan: any) => {
  const raw = plan?.createTime || plan?.updateTime || plan?.createdAt || plan?.updatedAt || '';
  const time = raw ? new Date(raw).getTime() : 0;
  return Number.isFinite(time) ? time : 0;
};

const matchPlanTimeFilter = (plan: any) => {
  if (planTimeFilter.value === 'all') return true;
  const dayMap: Record<'7d' | '30d' | '90d', number> = {
    '7d': 7,
    '30d': 30,
    '90d': 90,
  };
  const planTime = getPlanTime(plan);
  if (!planTime) return false;
  const days = dayMap[planTimeFilter.value];
  return Date.now() - planTime <= days * 24 * 60 * 60 * 1000;
};

const getPlanSearchText = (plan: any) => [
  plan?.title,
  plan?.content,
  plan?.subject,
  plan?.courseName,
  plan?.lessonType,
].filter(Boolean).join(' ').toLowerCase();

const filteredPlans = computed(() => {
  const kw = planSearchKeyword.value.trim().toLowerCase();
  const list = myPlans.value.filter((p: any) => {
    const keywordMatched = !kw || getPlanSearchText(p).includes(kw);
    return keywordMatched && matchPlanTimeFilter(p);
  });

  return [...list].sort((a: any, b: any) => {
    if (planSortMode.value === 'title') {
      return String(a?.title || '').localeCompare(String(b?.title || ''), 'zh-Hans-CN');
    }
    const diff = getPlanTime(b) - getPlanTime(a);
    return planSortMode.value === 'oldest' ? -diff : diff;
  });
});

const resetPlanFilters = () => {
  planSearchKeyword.value = '';
  planTimeFilter.value = 'all';
  planSortMode.value = 'newest';
};

const openPlanModal = async () => {
  planModalVisible.value = true;
  previewPlan.value = null;
  resetPlanFilters();
  plansLoading.value = true;
  try {
    const data = await request.get('/analysis/my-plans');
    myPlans.value = Array.isArray(data) ? data : [];
  } catch (e) {
    message.error('获取教案列表失败');
    myPlans.value = [];
  } finally {
    plansLoading.value = false;
  }
};

const previewPlanItem = (plan: any) => {
  previewPlan.value = plan;
};

const confirmSelectPlan = () => {
  if (!previewPlan.value) return;
  selectedPlan.value = previewPlan.value;
  message.success(`已关联教案: ${previewPlan.value.title}`);
  planModalVisible.value = false;
};

const clearSelectedPlan = () => {
  selectedPlan.value = null;
  message.info('已取消关联教案');
};

// === 录音 ===
const recordModalVisible = ref(false);
const isRecording = ref(false);
const recordSeconds = ref(0);
let mediaRecorder: MediaRecorder | null = null;
let audioChunks: BlobPart[] = [];
let timerInterval: any = null;

const formattedRecordTime = computed(() => {
  const m = Math.floor(recordSeconds.value / 60).toString().padStart(2, '0');
  const s = (recordSeconds.value % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
});

const openRecordModal  = () => { recordSeconds.value = 0; recordModalVisible.value = true; };
const closeRecordModal = () => { recordModalVisible.value = false; };

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    mediaRecorder = new MediaRecorder(stream);
    audioChunks = [];

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data);
    };

    mediaRecorder.onstop = async () => {
      stream.getTracks().forEach(track => track.stop());
      const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
      const audioFile = new File([audioBlob], `Class_Record_${new Date().getTime()}.webm`, { type: 'audio/webm' });
      closeRecordModal();
      await handleAudioUpload({ file: audioFile });
    };

    mediaRecorder.start();
    isRecording.value = true;
    recordSeconds.value = 0;
    timerInterval = setInterval(() => { recordSeconds.value++; }, 1000);
  } catch (err) {
    message.error('无法获取麦克风权限，请检查浏览器设置');
  }
};

const stopRecording = () => {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop();
    isRecording.value = false;
    clearInterval(timerInterval);
  }
};

// === 上传 -> 转写 -> 报告 ===

const handleAudioUpload = async (options: any) => {
  const { file } = options;
  const formData = new FormData();
  formData.append('file', file);

  isUploading.value = true;
  try {
    const audioUrl = await request.post<string, string>('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 0,
      skipErrorToast: true,
    });
    audioUploaded.value = true;
    message.success('音频已上传至云端，触发 ASR 分析...');
    submitTranscriptionTask(audioUrl);
  } catch (err: any) {
    message.error(err?.message || '网络错误，请检查后端 OSS 配置');
  } finally {
    isUploading.value = false;
  }
};

const submitTranscriptionTask = async (audioUrl: string) => {
  isTranscribing.value = true;
  transcript.value = [];
  aiReport.value = '';
  reportStats.value = null;
  currentStatus.value = 'transcribing';

  // 有教案时传入教案内容，无教案时传 null，后端以 planId 是否存在作为分支依据
  const planText = selectedPlan.value ? selectedPlan.value.content : null;

  try {
    const recordId = await request.post('/analysis/submit', {
      audioUrl,
      planId:   selectedPlan.value?.id || null,
      planText,
    });
    currentRecordId.value = Number(recordId);
    pollTaskStatus(currentRecordId.value);
  } catch (error) {
    message.error('提交转写任务失败');
    isTranscribing.value = false;
    currentStatus.value = 'failed';
  }
};

const pollTaskStatus = (recordId: number) => {
  const timer = setInterval(async () => {
    try {
      const record: any = await request.get('/analysis/status', { params: { id: recordId } });
      currentStatus.value = record.status || '';

      if (record.status === 'transcribed' || record.status === 'completed') {
        clearInterval(timer);
        isTranscribing.value = false;

        if (record.transcriptJson && record.transcriptJson !== '[]') {
          transcript.value = JSON.parse(record.transcriptJson);
          if (record.status === 'transcribed') {
            message.success('声纹分离与语音转写完成！可以点击「生成诊断分析」。');
          }
        } else {
          message.warning('阿里云未能识别出语音内容，请确认音频包含人声。');
        }

        if (record.status === 'completed' && record.aiReport) {
          aiReport.value = record.aiReport;
          isAnalyzing.value = false;
        }
      } else if (record.status === 'analyzing') {
        isTranscribing.value = false;
        isAnalyzing.value = true;
      } else if (record.status === 'failed') {
        clearInterval(timer);
        isTranscribing.value = false;
        isAnalyzing.value = false;
        message.error('任务处理失败');
      }
    } catch (error) {
      clearInterval(timer);
      isTranscribing.value = false;
    }
  }, 2000);
};

const savedToLibrary = ref(false);

const saveToResourceLibrary = async () => {
  if (!currentRecordId.value) return;
  try {
    await request.post('/analysis/save-to-resource', {
      recordId: currentRecordId.value,
    });
    savedToLibrary.value = true;
    message.success('已保存到教学资源库');
  } catch (e) {
    message.error('保存失败');
  }
};

const startGenerateReport = async () => {
  if (!currentRecordId.value) {
    message.warning('没有可用的转写记录');
    return;
  }

  isAnalyzing.value = true;
  aiReport.value = '';
  reportStats.value = null;
  currentStatus.value = 'analyzing';

  try {
    const record: any = await request.post('/analysis/generate-report', {
      recordId: currentRecordId.value,
    }, {
      timeout: 120000,
      skipErrorToast: true,
    });

    if (record && record.aiReport) {
      aiReport.value = record.aiReport;
      currentStatus.value = 'completed';
      message.success('评课报告已生成并保存');
    } else {
      currentStatus.value = 'failed';
      message.error('报告生成失败');
    }
  } catch (error) {
    currentStatus.value = 'failed';
    message.error('报告生成请求失败');
  } finally {
    isAnalyzing.value = false;
  }
};
</script>

<style scoped>
/* ===== 基础布局与动画 ===== */
.modern-page {
  font-family: 'Plus Jakarta Sans', sans-serif;
  animation: fadeIn 0.4s ease;
  background: #f8fafc;
  border-radius: 5px;
  padding: 32px;
  box-sizing: border-box;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden !important;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ===== 面板通用样式 ===== */
.glass-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  border-radius: 5px;
}

/* ===== 顶部 Header ===== */
.page-header {
  margin-bottom: 20px;
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.title-group h2 { margin: 0; font-size: 28px; font-weight: 800; color: #0f172a; display: flex; align-items: center; }
.title-icon { color: #8b5cf6; margin-right: 10px; font-size: 30px; }
.title-group .subtitle { margin: 6px 0 0; color: #64748b; font-size: 15px; }

/* ===== 状态控制台 ===== */
.action-dashboard {
  padding: 20px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  flex-shrink: 0;
  background: #ffffff;
}
.file-status { display: flex; align-items: center; gap: 12px; }
.status-divider { width: 1px; height: 32px; background: #e2e8f0; }
.status-item { display: flex; align-items: center; gap: 10px; padding: 4px 8px; border-radius: 5px; transition: background 0.2s; }
.status-item.active .s-icon { color: #8b5cf6; }
.s-icon { font-size: 20px; color: #cbd5e1; transition: color 0.2s; }
.s-info { display: flex; flex-direction: column; }
.s-info .label { font-size: 11px; color: #94a3b8; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.s-info .val { font-size: 13px; font-weight: 700; color: #1e293b; }
.s-info .val.text-muted { color: #94a3b8; font-weight: 500; }

.action-buttons { display: flex; gap: 12px; align-items: center; }
.ghost-btn { border-color: #e2e8f0; color: #475569; font-weight: 600; border-radius: 5px; }
.ghost-btn:hover { border-color: #8b5cf6; color: #8b5cf6; }
.unlink-btn { border-color: #fca5a5 !important; color: #ef4444 !important; }
.unlink-btn:hover { border-color: #ef4444 !important; color: #dc2626 !important; background: #fef2f2 !important; }
.rec-btn:hover { border-color: #ef4444 !important; color: #ef4444 !important; }
.upload-btn { background: #8b5cf6; border: none; font-weight: 700; border-radius: 5px; }
.upload-btn:hover { background: #7c3aed !important; }

/* ===== 欢迎引导页 ===== */
.welcome-wrapper { flex: 1; display: flex; justify-content: center; align-items: center; }
.welcome-content { max-width: 800px; width: 100%; padding: 48px 32px; }
.welcome-header { text-align: center; margin-bottom: 40px; }
.icon-ring {
  width: 72px; height: 72px;
  background: linear-gradient(135deg, #ede9fe, #ddd6fe);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 20px;
}
.main-icon { font-size: 32px; color: #8b5cf6; }
.welcome-header h3 { font-size: 22px; font-weight: 800; color: #0f172a; margin: 0 0 10px; }
.welcome-header p { color: #64748b; font-size: 14px; margin: 0; }
.steps-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.step-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  padding: 24px 20px;
  text-align: center;
}
.step-icon-wrap {
  width: 48px; height: 48px;
  border-radius: 5px;
  margin: 0 auto 16px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
}
.bg-blue   { background: #eff6ff; color: #3b82f6; }
.bg-purple { background: #f5f3ff; color: #8b5cf6; }
.bg-green  { background: #f0fdf4; color: #10b981; }
.step-card h4 { font-size: 16px; font-weight: 700; color: #1e293b; margin-bottom: 10px; }
.step-card p  { font-size: 13px; color: #64748b; line-height: 1.6; margin: 0; }

/* ===== 工作区布局 ===== */
.analysis-workspace {
  flex: 1;
  display: flex;
  gap: 24px;
  overflow: hidden;
  min-height: 0;
}
.left-panel, .right-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
  height: 100%;
}
.left-panel  { flex: 4; }
.right-panel { flex: 6; }

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  background: #fcfcfd;
  flex-shrink: 0;
  border-radius: 5px 5px 0 0;
}
.panel-header h3 { margin: 0; font-size: 15px; font-weight: 800; color: #1e293b; display: flex; align-items: center; gap: 8px; }
.text-primary { color: #8b5cf6; font-size: 12px; font-weight: 600; }

.scroll-y { flex: 1; overflow-y: auto; padding: 20px; }
.scroll-y::-webkit-scrollbar       { width: 6px; }
.scroll-y::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 5px; }

/* ===== 对话气泡 ===== */
.transcript-list { display: flex; flex-direction: column; gap: 18px; }
.dialogue-row    { display: flex; gap: 14px; align-items: flex-start; }
.time-badge {
  font-size: 12px; color: #94a3b8;
  font-family: 'Consolas', monospace;
  margin-top: 10px; width: 44px;
  text-align: right; flex-shrink: 0;
}
.dialogue-bubble {
  flex: 1;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  padding: 14px 18px;
  border-radius: 5px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.02);
}
.speaker {
  font-size: 12px; font-weight: 800; color: #4f46e5;
  margin-bottom: 6px; display: flex; align-items: center; gap: 6px;
}
.text { font-size: 14px; color: #334155; line-height: 1.7; }
.dialogue-row.student .dialogue-bubble { background: #f8fafc; border-color: #eef2f6; }
.dialogue-row.student .speaker { color: #10b981; }
/* ===== 报告分析区 - 按钮统一风格 ===== */
.header-actions {
  display: flex;
  gap: 12px; /* 稍微增加间距，让视觉更舒展 */
  align-items: center;
}

/* 统一基础按钮风格 */
.action-btn {
  height: 32px;
  border-radius: 5px;
  font-weight: 600;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 16px;
  box-shadow: none; /* 移除 Antd 默认的阴影，保持扁平现代感 */
  transition: all 0.2s ease;
}

/* 主操作按钮：生成诊断 */
.generate-btn {
  background: #10b981;
  border: 1px solid #10b981; /* 加上与背景同色的边框，对齐另一按钮的盒模型 */
  color: #ffffff;
}
.generate-btn:hover:not(:disabled) {
  background: #059669;
  border-color: #059669;
  color: #ffffff;
}
.generate-btn:disabled {
  background: #f1f5f9;
  border-color: #e2e8f0;
  color: #94a3b8;
}

/* 次操作按钮：保存到资源库 */
.save-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
}
.save-btn:hover:not(:disabled) {
  border-color: #8b5cf6; /* 悬浮时呼应你的全局品牌色 */
  color: #8b5cf6;
  background: #f8fafc;
}
.save-btn:disabled {
  background: #f8fafc;
  border-color: #e2e8f0;
  color: #94a3b8;
}

/* ===== 加载动画 ===== */
.transcribing-state, .analyzing-state {
  height: 100%; display: flex; flex-direction: column;
  justify-content: center; align-items: center;
  color: #8b5cf6; font-weight: 600;
}
.wave-loader { display: flex; gap: 4px; margin-bottom: 16px; height: 30px; align-items: center; }
.wave-loader .bar { width: 4px; height: 10px; background: #8b5cf6; border-radius: 5px; animation: wave 1s ease-in-out infinite; }
.wave-loader .bar:nth-child(2) { animation-delay: 0.1s; }
.wave-loader .bar:nth-child(3) { animation-delay: 0.2s; }
.wave-loader .bar:nth-child(4) { animation-delay: 0.3s; }
.wave-loader .bar:nth-child(5) { animation-delay: 0.4s; }
@keyframes wave { 0%, 100% { height: 10px; } 50% { height: 30px; } }

.radar-spinner {
  width: 50px; height: 50px;
  border: 3px solid #ede9fe; border-top: 3px solid #8b5cf6;
  border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 16px;
}
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

/* ===== 报告分析区 ===== */

/* 无教案提示条 */
.no-plan-alert {
  margin-bottom: 16px;
  border-radius: 5px;
}

/* 统计卡片 */
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-box {
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  padding: 16px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  display: flex; flex-direction: column; align-items: flex-start;
  box-shadow: 0 2px 4px rgba(15, 23, 42, 0.02);
}
.stat-box .label { font-size: 12px; color: #64748b; font-weight: 600; margin-bottom: 8px; }
.stat-box .val   { font-size: 20px; font-weight: 800; line-height: 1; }
/* 颜色语义 */
.stat-box .val.success    { color: #10b981; }  /* 良好 / 互动好 */
.stat-box .val.highlight  { color: #8b5cf6; }  /* 覆盖率高 */
.stat-box .val.warning    { color: #f59e0b; }  /* 一般 / 已生成 */
.stat-box .val.danger     { color: #ef4444; }  /* 较差 / 低 */
.stat-box .val.text-muted { color: #94a3b8; }  /* 无教案 / 待评估 */

/* 报告正文 */
.report-md {
  background: #fff;
  padding: 24px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
}

.report-sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-section-card {
  overflow: hidden;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}

.section-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid #eef2f7;
  background: linear-gradient(180deg, #fcfdff 0%, #f8fafc 100%);
}

.section-index {
  width: 32px;
  height: 32px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  flex-shrink: 0;
}

.section-heading {
  min-width: 0;
}

.section-title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}

.section-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.section-body {
  padding: 18px;
}

.report-section-card.purple .section-index {
  background: #f3e8ff;
  color: #7c3aed;
}
.report-section-card.blue .section-index {
  background: #dbeafe;
  color: #2563eb;
}
.report-section-card.cyan .section-index {
  background: #cffafe;
  color: #0891b2;
}
.report-section-card.green .section-index {
  background: #dcfce7;
  color: #16a34a;
}
.report-section-card.orange .section-index {
  background: #ffedd5;
  color: #ea580c;
}
.report-section-card.slate .section-index {
  background: #e2e8f0;
  color: #475569;
}

.report-section-card.purple .section-head {
  border-top: 3px solid #8b5cf6;
}
.report-section-card.blue .section-head {
  border-top: 3px solid #3b82f6;
}
.report-section-card.cyan .section-head {
  border-top: 3px solid #06b6d4;
}
.report-section-card.green .section-head {
  border-top: 3px solid #22c55e;
}
.report-section-card.orange .section-head {
  border-top: 3px solid #f59e0b;
}
.report-section-card.slate .section-head {
  border-top: 3px solid #94a3b8;
}

:deep(.doc-style) {
  color: #334155;
  line-height: 1.9;
  font-size: 14px;
}

:deep(.doc-style h3) {
  font-size: 16px;
  color: #0f172a;
  margin: 0 0 12px 0;
  padding-left: 10px;
  border-left: 4px solid #8b5cf6;
}

:deep(.doc-style p) {
  margin: 0 0 12px;
}

:deep(.doc-style ul),
:deep(.doc-style ol) {
  margin: 0;
  padding-left: 0;
}

:deep(.report-section-md ul),
:deep(.report-section-md ol) {
  list-style: none;
  counter-reset: report-step;
}

:deep(.report-section-md li) {
  position: relative;
  margin-bottom: 12px;
  padding: 14px 16px 14px 48px;
  border-radius: 5px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

:deep(.report-section-md ol > li::before) {
  counter-increment: report-step;
  content: counter(report-step);
  position: absolute;
  left: 14px;
  top: 14px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #e9d5ff;
  color: #7c3aed;
  font-size: 12px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.report-section-md ul > li::before) {
  content: '';
  position: absolute;
  left: 18px;
  top: 22px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #8b5cf6;
}

:deep(.doc-style strong) {
  color: #1e293b;
  font-weight: 700;
  background: #eef2ff;
  padding: 3px 8px;
  border-radius: 5px;
  border: 1px solid #e0e7ff;
}

:deep(.doc-style blockquote) {
  margin: 16px 0 0;
  padding: 12px 16px;
  background: #fffbeb;
  border-left: 4px solid #f59e0b;
  border-radius: 0 5px 5px 0;
  color: #92400e;
  font-size: 13px;
}

.empty-transcript, .empty-report {
  height: 100%; display: flex; flex-direction: column;
  justify-content: center; align-items: center;
  color: #94a3b8; font-size: 14px; text-align: center;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; color: #cbd5e1; }

/* ===== 教案列表弹窗 ===== */
.plan-modal-body {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 0;
  height: 680px; /* 将此处修改为您期望的高度，或改为 70vh */
  overflow: hidden;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
}
.plan-list-col {
  border-right: 1px solid #e2e8f0;
  overflow: hidden;
  padding: 12px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}
.plan-filter-panel {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.plan-filter-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.plan-filter-select {
  width: 100%;
}
.plan-filter-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 24px;
  color: #64748b;
  font-size: 12px;
}
.plan-filter-meta :deep(.ant-btn) {
  height: 24px;
  padding: 0;
  font-size: 12px;
}
.plan-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  padding-right: 4px;
}
.plan-list::-webkit-scrollbar {
  width: 6px;
}
.plan-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}
.plan-card {
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex; justify-content: space-between; align-items: center;
  background: #ffffff;
  min-width: 0;
  flex-shrink: 0;
}
.plan-card:hover    { border-color: #93c5fd; box-shadow: 0 2px 8px rgba(59,130,246,0.08); }
.plan-card.selected { border-color: #3b82f6; background: #eff6ff; }
.plan-card.confirmed { border-color: #22c55e; background: #f0fdf4; }
.plan-info  { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.p-title    { font-weight: 700; color: #1e293b; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.p-date     { font-size: 12px; color: #94a3b8; }
.check-icon { color: #22c55e; font-size: 18px; flex-shrink: 0; }
.empty-plans {
  /* 撑满外层 .plan-list-col 的剩余高度 */
  flex: 1;
  display: flex;
  flex-direction: column;
  /* 垂直居中 */
  justify-content: center;
  /* 水平居中 */
  align-items: center;
  color: #94a3b8;
}

.e-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #cbd5e1;
}

/* 右侧预览列 */
.plan-preview-col {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}
.plan-preview-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 14px;
  gap: 12px;
}
.prev-empty-icon { font-size: 48px; color: #cbd5e1; }
.plan-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  flex-shrink: 0;
  gap: 12px;
}
.plan-preview-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plan-preview-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  font-size: 13px;
  line-height: 1.8;
}

/* ===== 录音弹窗 ===== */
:deep(.record-modal-wrap .ant-modal-content) { border-radius: 5px !important; padding: 40px 20px; }
.record-container { display: flex; flex-direction: column; align-items: center; }
.mic-wrapper {
  width: 80px; height: 80px; background: #f1f5f9;
  border-radius: 50%; display: flex; justify-content: center; align-items: center;
  position: relative; margin-bottom: 24px; transition: 0.3s;
}
.mic-icon { font-size: 32px; color: #64748b; z-index: 10; transition: 0.3s; }
.mic-wrapper.recording          { background: #ef4444; }
.mic-wrapper.recording .mic-icon { color: #fff; }
.ripple { position: absolute; inset: 0; background: #ef4444; border-radius: 50%; z-index: 1; opacity: 0; }
.mic-wrapper.recording .ripple       { animation: ripple-anim 2s infinite cubic-bezier(0.4, 0, 0.2, 1); }
.mic-wrapper.recording .ripple.delay { animation-delay: 1s; }
/* 修改后：将 2.5 改为 1.6（您可以根据视觉感受在 1.5 到 1.8 之间微调） */
@keyframes ripple-anim { 0% { transform: scale(1); opacity: 0.6; } 100% { transform: scale(1.5); opacity: 0; } }

.record-time    { font-size: 36px; font-weight: 800; font-family: 'Consolas', monospace; color: #1e293b; margin: 0 0 8px 0; }
.record-hint    { color: #64748b; margin-bottom: 32px; font-size: 14px; }
.record-actions { display: flex; gap: 16px; align-items: center; }
.btn-cancel { border-color: #e2e8f0; color: #64748b; border-radius: 5px !important; }
.btn-start  { font-weight: 700; padding: 0 32px; border-radius: 5px !important; }
.btn-stop   { font-weight: 700; padding: 0 32px; box-shadow: 0 6px 16px rgba(239, 68, 68, 0.3); border-radius: 5px !important; }

/* ===== 全局 Ant Design 组件覆盖 ===== */
:deep(.ant-btn)           { border-radius: 5px; }
:deep(.ant-modal-content) { border-radius: 5px; }
:deep(.ant-alert)         { border-radius: 5px; }

/* ===== 响应式适配 ===== */
@media (max-width: 1024px) {
  .modern-page { padding: 16px; }
  .analysis-workspace { flex-direction: column; overflow-y: auto; }
  .left-panel, .right-panel { height: 500px; flex: none; }
  .steps-grid { grid-template-columns: 1fr; }
}
</style>
