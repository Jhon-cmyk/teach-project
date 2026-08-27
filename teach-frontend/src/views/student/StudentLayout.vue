<template>
  <div class="layout-container">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="logo" @click="router.push('/student/dashboard')">
          <img src="/favicon.png" alt="智学云Logo" class="brand-logo-image" />
          <div class="logo-text-group">
            <span class="logo-text-main">智慧教育</span>
            <span class="logo-text-sub">多模态教学过程分析与数字化资源制作</span>
          </div>
        </div>

        <nav class="nav-menu">
          <router-link to="/student/dashboard" class="nav-item" active-class="active">首页</router-link>
          <router-link to="/student/analysis" class="nav-item" active-class="active">课表分析</router-link>
          <router-link to="/student/tutorial" class="nav-item" active-class="active">资源检索</router-link>
          <router-link to="/student/community" class="nav-item" active-class="active">交流广场</router-link>
          <router-link to="/student/mental-state" class="nav-item" active-class="active">状态检测</router-link>
          <router-link to="/student/my-courses" class="nav-item" active-class="active">我的课程</router-link>
          <router-link to="/student/coding" class="nav-item" active-class="active">编程练习</router-link>
        </nav>

        <div class="user-block" @click="goToProfile">
          <a-avatar :size="48" :src="displayAvatarUrl" />
          <span class="user-name">{{ displayUserName }}</span>
        </div>
      </div>
    </header>

    <main class="main-content-wrapper">
      <router-view />
    </main>

    <a-modal
      v-model:open="recommendModalOpen"
      width="680px"
      :footer="null"
      :closable="false"
      :maskClosable="false"
      centered
      class="daily-recommend-modal"
    >
      <div v-if="recommendStep === 'intro'" class="recommend-intro">
        <span class="recommend-kicker">{{ promptKicker }}</span>
        <h2>{{ promptTitle }}</h2>
        <p>{{ promptBody }}</p>
        <div v-if="!isOnboardingPrompt" class="recommend-preview-block">
          <div class="recommend-preview-heading">
            <strong>今日清单</strong>
            <span>{{ promptRecommendations.length }} 项</span>
          </div>
          <div v-if="promptRecommendations.length" class="recommend-preview-list">
            <div v-for="item in promptRecommendations" :key="item.id" class="recommend-preview-item">
              <div class="recommend-preview-topline">
                <span>{{ recommendationSourceText(item) }}</span>
                <em>{{ item.actionLabel || recommendationActionText(item) }}</em>
              </div>
              <strong>{{ recommendationTitle(item) }}</strong>
              <span>{{ item.shortReason || item.recommendationReason || '系统根据近期学习记录整理。' }}</span>
            </div>
          </div>
          <div v-else class="recommend-preview-empty">
            暂未匹配到真实的临期作业或学习资源，点击“我想自己设定”补充今天的目标后重新生成。
          </div>
        </div>
        <div v-else class="recommend-preview-block recommend-assessment-block">
          <div class="recommend-preview-heading">
            <strong>测评会影响这些推荐</strong>
            <span>3 项</span>
          </div>
          <div class="recommend-preview-list">
            <div class="recommend-preview-item">
              <div class="recommend-preview-topline">
                <span>学习基础</span>
                <em>判断起点</em>
              </div>
              <strong>先确认当前适合补基础、巩固，还是做提升题。</strong>
              <span>没有历史记录时，系统不会假装知道学生情况，会先用测评建立初始画像。</span>
            </div>
            <div class="recommend-preview-item">
              <div class="recommend-preview-topline">
                <span>学习性格</span>
                <em>调整节奏</em>
              </div>
              <strong>根据稳扎稳打、喜欢挑战或需要引导来安排推荐顺序。</strong>
              <span>后续有学习记录后，会逐步以真实行为替代初始测评。</span>
            </div>
            <div class="recommend-preview-item">
              <div class="recommend-preview-topline">
                <span>资源偏好</span>
                <em>匹配形式</em>
              </div>
              <strong>决定优先展示视频、图文，还是练习资源。</strong>
              <span>学生可以随时重新设定当天目标，推荐会按最新选择刷新。</span>
            </div>
          </div>
        </div>
        <div class="recommend-actions">
          <a-button :loading="recommendLoading" @click="dismissDailyPrompt">{{ dismissButtonLabel }}</a-button>
          <a-button type="primary" @click="openRecommendForm">{{ openFormButtonLabel }}</a-button>
        </div>
      </div>

      <div v-else class="recommend-form">
        <div class="recommend-form-header">
          <div class="recommend-form-heading">
            <span class="recommend-kicker">{{ formKicker }}</span>
            <h2>{{ formTitle }}</h2>
          </div>
          <div class="recommend-mode-switch" role="tablist" aria-label="选择信息收集方式">
            <button
              type="button"
              role="tab"
              :aria-selected="recommendMode === 'questionnaire'"
              :class="{ active: recommendMode === 'questionnaire' }"
              @click="recommendMode = 'questionnaire'"
            >
              <span class="mode-icon">表</span>
              <span><strong>快速问卷</strong><small>直接填写，约 1 分钟</small></span>
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="recommendMode === 'ai'"
              :class="{ active: recommendMode === 'ai' }"
              @click="switchToAiMode"
            >
              <span class="mode-icon ai">AI</span>
              <span><strong>AI 学习访谈</strong><small>边聊边梳理学习需求</small></span>
            </button>
          </div>
        </div>

        <a-form v-if="recommendMode === 'questionnaire'" layout="vertical" class="recommend-questionnaire">
          <section class="learning-background-card" aria-labelledby="learning-background-title">
            <div class="background-card-heading">
              <div>
                <strong id="learning-background-title">学习背景</strong>
                <span>长期保存，之后可以在个人资料中修改</span>
              </div>
              <em>用于调整推荐方向</em>
            </div>
            <a-form-item label="你目前就读于哪所大学？" required>
              <a-auto-complete
                v-model:value="recommendForm.universityName"
                :options="universityAutocompleteOptions"
                :filter-option="false"
                :get-popup-container="getUniversityPopupContainer"
                allow-clear
                placeholder="搜索或直接输入大学全称"
                class="university-autocomplete"
                @select="selectUniversity"
              />
            </a-form-item>
            <a-form-item label="你目前更偏向哪一个发展目标？" required>
              <div class="development-goal-grid" role="radiogroup" aria-label="发展目标">
                <button
                  v-for="option in developmentGoalOptions"
                  :key="option.value"
                  type="button"
                  role="radio"
                  :aria-checked="recommendForm.developmentGoal === option.value"
                  :class="{ active: recommendForm.developmentGoal === option.value }"
                  @click="recommendForm.developmentGoal = option.value"
                >
                  <strong>{{ option.label }}</strong>
                  <span>{{ option.description }}</span>
                </button>
              </div>
            </a-form-item>
          </section>

          <a-form-item label="今天想重点学习哪门课程？">
            <a-select
              v-model:value="recommendForm.courseId"
              allow-clear
              show-search
              :loading="courseLoading"
              placeholder="可不选，系统会综合推荐"
              :filter-option="filterCourseOption"
            >
              <a-select-option v-for="course in courseOptions" :key="course.id" :value="course.id">
                {{ course.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item v-if="isOnboardingPrompt" label="目前整体学习情况更接近哪一种？">
            <a-select v-model:value="recommendForm.learningSituation" placeholder="选择一个最接近的状态">
              <a-select-option value="基础薄弱，需要从头补">基础薄弱，需要从头补</a-select-option>
              <a-select-option value="能听懂，但做题不稳定">能听懂，但做题不稳定</a-select-option>
              <a-select-option value="想提升速度和综合应用">想提升速度和综合应用</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="今天的学习目标是什么？">
            <a-select v-model:value="recommendForm.goal" placeholder="请选择一个目标">
              <a-select-option value="复习巩固">复习巩固</a-select-option>
              <a-select-option value="预习新课">预习新课</a-select-option>
              <a-select-option value="查漏补缺">查漏补缺</a-select-option>
              <a-select-option value="提升拓展">提升拓展</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="目前最困惑的内容是什么？">
            <a-textarea
              v-model:value="recommendForm.difficultyText"
              :maxlength="160"
              :auto-size="{ minRows: 2, maxRows: 4 }"
              placeholder="例如：递归、链表反转、Vue 组件通信..."
            />
          </a-form-item>

          <a-form-item label="今天大约能投入多久？">
            <a-input-number
              v-model:value="recommendForm.availableMinutes"
              :min="5"
              :max="180"
              :step="5"
              addon-after="分钟"
              style="width: 180px"
            />
          </a-form-item>

          <a-form-item v-if="isOnboardingPrompt" label="你的学习性格更像哪一种？">
            <a-radio-group v-model:value="recommendForm.personalityType">
              <a-radio-button value="steady">稳扎稳打</a-radio-button>
              <a-radio-button value="challenge">喜欢挑战</a-radio-button>
              <a-radio-button value="guided">需要引导</a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="更希望优先看到哪类资源？">
            <a-radio-group v-model:value="recommendForm.preferredResourceType">
              <a-radio-button value="video">视频优先</a-radio-button>
              <a-radio-button value="text">图文优先</a-radio-button>
              <a-radio-button value="balanced">均衡推荐</a-radio-button>
            </a-radio-group>
          </a-form-item>
        </a-form>

        <div v-else class="recommend-interview">
          <div class="interview-status">
            <div>
              <span>学习画像完成度</span>
              <strong>{{ interviewProgress }}%</strong>
            </div>
            <div class="interview-progress" aria-hidden="true">
              <span :style="{ width: `${interviewProgress}%` }"></span>
            </div>
          </div>

          <div ref="interviewTranscript" class="interview-transcript" aria-live="polite">
            <div
              v-for="(item, index) in interviewMessages"
              :key="`${item.role}-${index}`"
              class="interview-message"
              :class="item.role"
            >
              <span v-if="item.role === 'assistant'" class="interview-avatar">AI</span>
              <div class="interview-bubble">{{ item.content }}</div>
            </div>
            <div v-if="interviewLoading" class="interview-message assistant">
              <span class="interview-avatar">AI</span>
              <div class="interview-bubble interview-thinking"><i></i><i></i><i></i></div>
            </div>
          </div>

          <div v-if="interviewMessages.length === 1" class="interview-suggestions" aria-label="快捷回答">
            <button v-for="suggestion in interviewSuggestions" :key="suggestion" type="button" @click="sendInterviewMessage(suggestion)">
              {{ suggestion }}
            </button>
          </div>

          <div v-if="interviewFacts.length" class="interview-facts">
            <span class="facts-label">AI 已了解</span>
            <span v-for="fact in interviewFacts" :key="fact">{{ fact }}</span>
          </div>

          <div v-if="interviewDegraded" class="interview-warning">
            AI 顾问暂时不可用，已填写内容不会丢失。
            <button type="button" @click="recommendMode = 'questionnaire'">切换到快速问卷</button>
          </div>

          <div class="interview-composer">
            <a-textarea
              v-model:value="interviewInput"
              :maxlength="500"
              :auto-size="{ minRows: 1, maxRows: 3 }"
              :disabled="interviewLoading"
              placeholder="像聊天一样回答，按 Enter 发送，Shift + Enter 换行"
              @keydown.enter.exact.prevent="sendInterviewMessage()"
            />
            <button
              type="button"
              class="interview-send"
              :disabled="interviewLoading || !interviewInput.trim()"
              aria-label="发送回答"
              @mousedown.prevent
              @click="sendInterviewMessage()"
            >
              ↑
            </button>
          </div>
        </div>

        <div class="recommend-actions">
          <a-button @click="recommendStep = 'intro'">返回</a-button>
          <span v-if="recommendMode === 'ai' && !interviewReady" class="recommend-action-hint">再回答几句，AI 会整理出推荐条件</span>
          <a-button
            type="primary"
            :loading="recommendLoading"
            :disabled="recommendMode === 'ai' ? !interviewReady : !learningContextComplete"
            @click="submitDailyPrompt"
          >
            {{ recommendMode === 'ai' ? '按访谈结果生成推荐' : submitButtonLabel }}
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { storeToRefs } from 'pinia';
import { message } from 'ant-design-vue';
import request from '@/utils/request';
import { getLoginUser } from '@/utils/authStorage';
import {
  continueDailyRecommendationInterview,
  dismissTodayDailyRecommendation,
  fetchStudentLearningContext,
  fetchTodayDailyRecommendation,
  submitTodayDailyRecommendation,
  type DailyRecommendationInterviewMessage,
  type DailyRecommendationToday,
  type DailyRecommendationSubmitPayload
} from '@/api/learning';
import { getUniversityOptions } from '@/data/chineseUniversities';

const router = useRouter();
const userStore = useUserStore();
const { userInfo } = storeToRefs(userStore);

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820';

const mergedUserInfo = computed(() => {
  const localUser = getLoginUser<any>() || {};
  return {
    ...localUser,
    ...(userInfo.value || {}),
  };
});

const displayUserName = computed(() => {
  return mergedUserInfo.value?.userName || mergedUserInfo.value?.name || '同学';
});

const displayAvatarUrl = computed(() => {
  const rawAvatar =
    mergedUserInfo.value?.userAvatar || mergedUserInfo.value?.avatar;

  if (!rawAvatar) {
    return 'https://api.dicebear.com/7.x/notionists/svg?seed=smart-edu';
  }

  if (rawAvatar.startsWith('http') || rawAvatar.startsWith('data:image')) {
    return rawAvatar;
  }

  return `${API_BASE_URL}${rawAvatar}`;
});

const goToProfile = () => router.push('/student/profile');

type CourseOption = {
  id: number
  name: string
}

const recommendModalOpen = ref(false);
const recommendStep = ref<'intro' | 'form'>('intro');
const recommendMode = ref<'questionnaire' | 'ai'>('questionnaire');
const recommendLoading = ref(false);
const dailyPrompt = ref<DailyRecommendationToday | null>(null);
const courseLoading = ref(false);
const courseOptions = ref<CourseOption[]>([]);
const interviewInput = ref('');
const interviewLoading = ref(false);
const interviewReady = ref(false);
const interviewProgress = ref(20);
const interviewDegraded = ref(false);
const interviewSummary = ref('');
const interviewTranscript = ref<HTMLElement | null>(null);
const interviewMessages = ref<DailyRecommendationInterviewMessage[]>([
  { role: 'assistant', content: '你好，我会用几句对话帮你梳理今天的学习计划。先说说，你今天最想解决哪个学习问题？' }
]);
const interviewSuggestions = [
  '复习最近没掌握的内容',
  '预习下一节课',
  '做一些提升练习'
];
const developmentGoalOptions = [
  { value: 'postgraduate' as const, label: '考研', description: '偏重基础、原理与考试训练' },
  { value: 'employment' as const, label: '就业', description: '偏重项目、实战与面试能力' },
  { value: 'undecided' as const, label: '暂未确定', description: '先按当前课程和薄弱点推荐' }
];
const recommendForm = reactive<DailyRecommendationSubmitPayload>({
  courseId: undefined,
  goal: '查漏补缺',
  difficultyText: '',
  learningSituation: '',
  personalityType: 'steady',
  universityName: '',
  developmentGoal: '',
  availableMinutes: 30,
  preferredResourceType: 'balanced'
});

const filterCourseOption = (input: string, option: any) => {
  const text = String(option?.children?.[0]?.children || option?.children || '').toLowerCase();
  return text.includes(input.toLowerCase());
};

const universityAutocompleteOptions = computed(() => getUniversityOptions(String(recommendForm.universityName || '')));

const getUniversityPopupContainer = (triggerNode: HTMLElement) => triggerNode.parentElement || document.body;

const selectUniversity = (value: string | number) => {
  recommendForm.universityName = String(value || '').trim();
};

const isOnboardingPrompt = computed(() => dailyPrompt.value?.promptType === 'onboarding_assessment');
const isProfileEnrichmentPrompt = computed(() => dailyPrompt.value?.promptType === 'profile_enrichment');
const learningContextComplete = computed(() => Boolean(
  String(recommendForm.universityName || '').trim() && recommendForm.developmentGoal
));
const promptRecommendations = computed(() => (dailyPrompt.value?.recommendations || []).slice(0, 3));
const selectedCourseName = computed(() => courseOptions.value.find(
  course => String(course.id) === String(recommendForm.courseId || '')
)?.name || '');
const interviewFacts = computed(() => {
  if (!interviewMessages.value.some(item => item.role === 'user')) return [];
  const facts: string[] = [];
  if (recommendForm.universityName) facts.push(String(recommendForm.universityName));
  const goalText = developmentGoalOptions.find(option => option.value === recommendForm.developmentGoal)?.label;
  if (goalText) facts.push(`方向：${goalText}`);
  if (selectedCourseName.value) facts.push(selectedCourseName.value);
  if (recommendForm.goal) facts.push(String(recommendForm.goal));
  if (recommendForm.difficultyText) facts.push(`困惑：${String(recommendForm.difficultyText).slice(0, 18)}`);
  if (recommendForm.availableMinutes) facts.push(`${recommendForm.availableMinutes} 分钟`);
  const resourceText = { video: '视频优先', text: '图文优先', balanced: '均衡推荐' }[
    recommendForm.preferredResourceType || 'balanced'
  ];
  if (resourceText) facts.push(resourceText);
  return facts.slice(0, 6);
});

const promptKicker = computed(() => {
  if (isOnboardingPrompt.value) return '新生学习测评';
  if (isProfileEnrichmentPrompt.value) return '完善学习背景';
  return '今日个性化推荐';
});
const promptTitle = computed(() => (
  isOnboardingPrompt.value
    ? '先了解你的学习情况和偏好'
    : isProfileEnrichmentPrompt.value
      ? '补充两项信息，让推荐更贴近你的方向'
    : '已根据历史学习和待完成任务生成今日建议'
));
const promptBody = computed(() => (
  isOnboardingPrompt.value
    ? '这个账号还没有学习、视频或作业记录。先完成一次简短测评，系统会据此判断你的学习情况、节奏和资源偏好。'
    : isProfileEnrichmentPrompt.value
      ? '告诉系统你所在的大学，以及目前更偏向考研还是就业。学校不会被用于能力评判，发展目标只会帮助系统调整资源排序。'
    : '系统已先参考昨天和近 7 天学习时长、薄弱点、临期未完成作业和近期错题生成建议。不合适时，可以自己重新设定今天想学什么。'
));
const dismissButtonLabel = computed(() => (isOnboardingPrompt.value || isProfileEnrichmentPrompt.value) ? '稍后再说' : '按这个学');
const openFormButtonLabel = computed(() => isOnboardingPrompt.value ? '开始测评' : isProfileEnrichmentPrompt.value ? '现在完善' : '我想自己设定');
const formKicker = computed(() => isOnboardingPrompt.value ? '学习画像测评' : isProfileEnrichmentPrompt.value ? '完善长期画像' : '重新设定今日推荐');
const formTitle = computed(() => isOnboardingPrompt.value ? '告诉系统你的学习状态' : isProfileEnrichmentPrompt.value ? '先补充学习背景，再生成推荐' : '今天想怎么学？');
const submitButtonLabel = computed(() => isOnboardingPrompt.value ? '保存测评并生成推荐' : isProfileEnrichmentPrompt.value ? '保存背景并生成推荐' : '重新生成今日推荐');

const recommendationTitle = (item: any) => {
  return item?.courseName || item?.resourceTitle || item?.knowledgeName || '学习任务';
};

const recommendationSourceText = (item: any) => {
  const type = String(item?.resourceType || '').toLowerCase();
  const source = String(item?.recommendationSource || '').toLowerCase();
  if (type.includes('homework')) return '未完成作业';
  if (source === 'learning_history') return '学习历史';
  if (source === 'video_behavior') return '视频学习';
  if (source === 'exam_behavior') return '考试薄弱点';
  if (source === 'homework_behavior') return '作业表现';
  if (source === 'daily_survey') return '自定义偏好';
  return '推荐资源';
};

const recommendationActionText = (item: any) => {
  const type = String(item?.resourceType || '').toLowerCase();
  if (type.includes('homework')) return '去完成';
  if (type.includes('text') || type.includes('tutorial')) return '去阅读';
  if (type.includes('quiz') || type.includes('practice')) return '去练习';
  return '去学习';
};

const loadCourseOptions = async () => {
  courseLoading.value = true;
  try {
    const data = await request.get<CourseOption[], CourseOption[]>('/course/list/my-class', { skipErrorToast: true });
    courseOptions.value = Array.isArray(data) ? data : [];
  } catch {
    courseOptions.value = [];
  } finally {
    courseLoading.value = false;
  }
};

const loadStudentLearningContext = async () => {
  try {
    const context = await fetchStudentLearningContext();
    recommendForm.universityName = context?.universityName || '';
    recommendForm.developmentGoal = context?.developmentGoal || '';
    if (!interviewMessages.value.some(item => item.role === 'user')) {
      if (!recommendForm.universityName) {
        interviewMessages.value[0].content = '我们先从学习背景开始。你目前在哪所大学就读？可以直接输入大学全称。';
      } else if (!recommendForm.developmentGoal) {
        interviewMessages.value[0].content = `我知道你就读于${recommendForm.universityName}。你目前更偏向考研、就业，还是暂未确定？`;
      } else {
        interviewMessages.value[0].content = '你好，我已经了解你的学习背景。接下来想问问，你今天最想解决哪个学习问题？';
      }
    }
  } catch {
    // Context can still be entered directly in the questionnaire or interview.
  }
};

const checkDailyPrompt = async () => {
  if (mergedUserInfo.value?.userRole !== 'student') return;
  try {
    const today = await fetchTodayDailyRecommendation();
    dailyPrompt.value = today || null;
    if (today?.shouldPrompt) {
      recommendStep.value = 'intro';
      recommendModalOpen.value = true;
      loadCourseOptions();
      loadStudentLearningContext();
    }
  } catch {
    recommendModalOpen.value = false;
  }
};

const dismissDailyPrompt = async () => {
  recommendLoading.value = true;
  try {
    await dismissTodayDailyRecommendation();
    recommendModalOpen.value = false;
  } finally {
    recommendLoading.value = false;
  }
};

const openRecommendForm = () => {
  recommendStep.value = 'form';
  if (!courseOptions.value.length) loadCourseOptions();
  loadStudentLearningContext();
};

const scrollInterviewToBottom = async () => {
  await nextTick();
  if (interviewTranscript.value) {
    interviewTranscript.value.scrollTop = interviewTranscript.value.scrollHeight;
  }
};

const switchToAiMode = () => {
  recommendMode.value = 'ai';
  scrollInterviewToBottom();
};

const applyInterviewProfile = (profile?: DailyRecommendationSubmitPayload) => {
  if (!profile) return;
  if (profile.courseId !== undefined) recommendForm.courseId = profile.courseId;
  if (profile.goal) recommendForm.goal = profile.goal;
  if (profile.difficultyText) recommendForm.difficultyText = profile.difficultyText;
  if (profile.learningSituation) recommendForm.learningSituation = profile.learningSituation;
  if (profile.personalityType) recommendForm.personalityType = profile.personalityType;
  if (profile.universityName) recommendForm.universityName = profile.universityName;
  if (profile.developmentGoal) recommendForm.developmentGoal = profile.developmentGoal;
  if (profile.availableMinutes != null) recommendForm.availableMinutes = profile.availableMinutes;
  if (profile.preferredResourceType) recommendForm.preferredResourceType = profile.preferredResourceType;
  if (profile.interviewSummary) interviewSummary.value = profile.interviewSummary;
};

const clearInterviewInput = async () => {
  interviewInput.value = '';
  await nextTick();
  // Ant Design TextArea may emit a delayed value update after losing focus.
  // Confirm the cleared value after Vue has flushed the current render.
  interviewInput.value = '';
};

const sendInterviewMessage = async (suggestion?: string) => {
  const content = String(suggestion ?? interviewInput.value).trim();
  if (!content || interviewLoading.value) return;
  const shouldRestoreOnFailure = suggestion === undefined;
  interviewLoading.value = true;
  interviewMessages.value.push({ role: 'user', content });
  await clearInterviewInput();
  interviewDegraded.value = false;
  scrollInterviewToBottom();
  try {
    const result = await continueDailyRecommendationInterview({
      messages: interviewMessages.value,
      profile: { ...recommendForm, collectionMode: 'ai_interview' },
      courses: courseOptions.value
    });
    applyInterviewProfile(result?.profile);
    interviewReady.value = Boolean(result?.ready);
    interviewProgress.value = Math.min(100, Math.max(0, Number(result?.progress) || 0));
    interviewDegraded.value = Boolean(result?.degraded);
    interviewMessages.value.push({
      role: 'assistant',
      content: result?.reply || '我还需要再了解一点。你今天大约能投入多长时间？'
    });
  } catch {
    if (shouldRestoreOnFailure) interviewInput.value = content;
    interviewDegraded.value = true;
    interviewReady.value = false;
    interviewMessages.value.push({
      role: 'assistant',
      content: 'AI 学习顾问暂时没有响应。你可以重试，或切换到快速问卷继续。'
    });
  } finally {
    interviewLoading.value = false;
    scrollInterviewToBottom();
  }
};

const handleOpenDailyRecommendation = () => {
  recommendModalOpen.value = true;
  openRecommendForm();
};

const submitDailyPrompt = async () => {
  recommendForm.universityName = String(recommendForm.universityName || '').trim();
  if (!learningContextComplete.value) {
    recommendMode.value = 'questionnaire';
    message.warning('请先填写所在大学，并选择考研、就业或暂未确定');
    return;
  }
  recommendLoading.value = true;
  try {
    const today = await submitTodayDailyRecommendation({
      ...recommendForm,
      courseId: recommendForm.courseId || null,
      learningSituation: String(recommendForm.learningSituation || '').trim(),
      personalityType: String(recommendForm.personalityType || '').trim(),
      difficultyText: String(recommendForm.difficultyText || '').trim(),
      collectionMode: recommendMode.value === 'ai' ? 'ai_interview' : 'questionnaire',
      interviewSummary: recommendMode.value === 'ai' ? interviewSummary.value : ''
    });
    dailyPrompt.value = today || dailyPrompt.value;
    sessionStorage.setItem('dailyRecommendationJustGenerated', '1');
    message.success('今日推荐已生成，已更新到首页下方“个性化资源推荐”的“今日推荐”里');
    recommendModalOpen.value = false;
    window.dispatchEvent(new CustomEvent('daily-recommendation-updated', {
      detail: { today }
    }));
    router.push('/student/dashboard');
  } finally {
    recommendLoading.value = false;
  }
};

onMounted(() => {
  document.body.classList.add('student-no-select');
  checkDailyPrompt();
  window.addEventListener('open-daily-recommendation', handleOpenDailyRecommendation);
});

onBeforeUnmount(() => {
  document.body.classList.remove('student-no-select');
  window.removeEventListener('open-daily-recommendation', handleOpenDailyRecommendation);
});
</script>

<style scoped>
/* 全局变量 (建议后续抽离到全局 css 中) */
.layout-container {
  --primary-color: #2563EB;
  --text-main: #1F2937;
  --text-sub: #667085;
  --bg-page: #F6F8FC;
  min-height: 100vh;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
}

/* 顶部导航条样式（提取自 Dashboard.vue） */
.topbar {
  position: sticky;
  top: 0;
  z-index: 999;
  height: 70px;
  width: 100%;       /* 确保背景条是全屏的 */
  display: flex;
  justify-content: center; /* 确保内部容器能居中 */
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.06);
}

.topbar-inner {
  position: relative;
  /* 🌟 核心修改：锁定最大宽度并居中 */
  width: 100%;
  max-width: 1400px; /* 👈 这个数值越小，Logo 和头像就靠得越近。建议在 1200px-1400px 之间 */
  margin: 0 auto;    /* 👈 必须配合 auto 才能实现整体居中 */

  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-sizing: border-box;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.brand-logo-image {
  height: 60px;
  width: auto;
  max-width: 150px;
  object-fit: contain;
}
.logo-text-group {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1.1;
}

.logo-text-main {
  font-size: 28px;
  font-weight: 600;
  color: #400d6e;
  letter-spacing: -0.3px;
}

.logo-text-sub {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-sub);
  margin-top: 3px;
}

.nav-menu {
  flex: 1;
  display: flex;
  justify-content: center; /* 保持原有的居中布局 */
  align-items: center;
  gap: 6px;
  height: 64px;
  margin: 0 20px;

  /* 增加这一行：向左平移 40 像素。你可以根据需要调整 -40px 这个数值 */
  transform: translateX(-40px);
}
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 64px;
  font-size: 18px;
  font-weight: 650;
  color: var(--text-sub);
  text-decoration: none;
  padding: 0 16px;
  transition: color 0.2s;

  /* 🚀 核心修复：强制文本不换行并禁止元素被挤压 */
  white-space: nowrap;
  flex-shrink: 0;
}
.nav-item:hover { color: var(--text-main); }
.nav-item.active { color: var(--primary-color); font-weight: 750; }
.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 32px;
  height: 3px;
  background: var(--primary-color);
  border-radius: 3px 3px 0 0;
}

.user-block {
  display: flex; align-items: center; gap: 10px;
  background-color: rgba(0, 0, 0, 0.04);;
  padding: 6px 12px 6px 6px; border-radius: 25px;
  flex-shrink: 0;
  cursor: pointer;
}

.user-name { font-size: 20px; font-weight: 400; color: var(--text-main); }

.main-content-wrapper {
  flex: 1; /* 让主体内容撑满剩余空间 */
  width: 100%;
}

.daily-recommend-modal :deep(.ant-modal-content) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.18);
}

.daily-recommend-modal :deep(.ant-modal-body) {
  padding: 0;
}

.recommend-intro,
.recommend-form {
  display: flex;
  flex-direction: column;
  height: min(600px, calc(100vh - 96px));
  max-height: min(600px, calc(100vh - 96px));
  padding: 26px 28px 24px;
  overflow: hidden;
  background: #ffffff;
}

.recommend-form {
  padding: 18px 28px 20px;
}

.recommend-form-header {
  display: grid;
  grid-template-columns: minmax(180px, 0.8fr) minmax(360px, 1.4fr);
  align-items: end;
  gap: 20px;
  flex: 0 0 auto;
}

.recommend-form-heading {
  min-width: 0;
}

.recommend-kicker {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 800;
}

.recommend-intro h2,
.recommend-form h2 {
  margin: 14px 0 8px;
  color: #111827;
  font-size: 22px;
  line-height: 1.28;
  font-weight: 800;
  text-wrap: balance;
}

.recommend-form-heading h2 {
  margin: 8px 0 2px;
  font-size: 21px;
  line-height: 1.25;
  white-space: normal;
  text-wrap: balance;
}

.recommend-intro p {
  margin: 0;
  max-width: 62ch;
  color: #334155;
  font-size: 14px;
  line-height: 1.72;
}

.recommend-preview-list {
  display: grid;
  gap: 10px;
  min-height: 0;
  max-height: none;
  overflow-y: auto;
  padding-right: 4px;
}

.recommend-preview-block {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  margin-top: 18px;
  padding: 14px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.recommend-preview-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.recommend-preview-heading strong {
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.recommend-preview-heading span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.recommend-preview-item {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px #e2e8f0;
}

.recommend-preview-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.recommend-preview-topline span,
.recommend-preview-topline em {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  white-space: nowrap;
}

.recommend-preview-topline span {
  color: #1d4ed8;
  background: #eff6ff;
}

.recommend-preview-topline em {
  color: #047857;
  background: #ecfdf5;
}

.recommend-preview-item strong {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.4;
}

.recommend-preview-item span {
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.recommend-preview-empty {
  display: grid;
  place-items: center;
  flex: 1;
  min-height: 180px;
  padding: 18px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}

.recommend-form :deep(.ant-form) {
  flex: 1;
  min-height: 0;
  margin-top: 14px;
  overflow-y: auto;
  padding: 2px 8px 2px 0;
}

.recommend-mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 0;
  padding: 5px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.recommend-mode-switch button {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 9px 11px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  color: #475569;
  text-align: left;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.recommend-mode-switch button.active {
  border-color: #bfdbfe;
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 3px 12px rgba(37, 99, 235, 0.1);
}

.recommend-mode-switch button > span:last-child {
  display: grid;
  min-width: 0;
}

.recommend-mode-switch strong {
  font-size: 13px;
  line-height: 1.35;
}

.recommend-mode-switch small {
  margin-top: 2px;
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mode-icon {
  display: grid;
  place-items: center;
  flex: 0 0 28px;
  height: 28px;
  border-radius: 8px;
  background: #e2e8f0;
  color: #475569;
  font-size: 11px;
  font-weight: 900;
}

.recommend-mode-switch button.active .mode-icon {
  background: #eff6ff;
  color: #1d4ed8;
}

.mode-icon.ai {
  letter-spacing: -0.5px;
}

.recommend-questionnaire {
  margin-top: 12px !important;
}

.learning-background-card {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid #c7d7f5;
  border-radius: 12px;
  background: #f7faff;
}

.background-card-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 13px;
}

.background-card-heading > div {
  display: grid;
  gap: 2px;
}

.background-card-heading strong {
  color: #0f172a;
  font-size: 14px;
}

.background-card-heading span {
  color: #64748b;
  font-size: 11px;
}

.background-card-heading em {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  background: #e8eefc;
  color: #1d4ed8;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
}

.learning-background-card :deep(.ant-form-item:last-child) {
  margin-bottom: 0;
}

.learning-background-card :deep(.university-autocomplete.ant-select) {
  width: 100%;
}

.learning-background-card :deep(.university-autocomplete .ant-select-selector) {
  padding-right: 42px !important;
}

.learning-background-card :deep(.university-autocomplete .ant-select-clear) {
  top: 50%;
  right: 13px;
  display: grid;
  place-items: center;
  width: 17px;
  height: 17px;
  margin-top: 0;
  transform: translateY(-50%);
  border-radius: 50%;
  background: #94a3b8;
  color: #ffffff;
  font-size: 10px;
}

.development-goal-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.development-goal-grid button {
  display: grid;
  gap: 3px;
  min-width: 0;
  padding: 10px;
  border: 1px solid #dbe3ef;
  border-radius: 9px;
  background: #ffffff;
  color: #475569;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

.development-goal-grid button:hover {
  border-color: #93b4ee;
}

.development-goal-grid button.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.08);
}

.development-goal-grid strong {
  color: #0f172a;
  font-size: 12px;
}

.development-goal-grid button.active strong {
  color: #1d4ed8;
}

.development-goal-grid span {
  color: #64748b;
  font-size: 10px;
  line-height: 1.35;
}

.recommend-interview {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  margin-top: 14px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.interview-status {
  padding: 10px 14px 8px;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
}

.interview-status > div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.interview-status strong {
  color: #1d4ed8;
  font-size: 12px;
}

.interview-progress {
  height: 4px;
  margin-top: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.interview-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #2563eb;
  transition: width 0.3s ease;
}

.interview-transcript {
  flex: 1;
  min-height: 150px;
  overflow-y: auto;
  padding: 12px 14px;
  scroll-behavior: smooth;
}

.interview-message {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 8px;
}

.interview-message.user {
  justify-content: flex-end;
}

.interview-avatar {
  display: grid;
  place-items: center;
  flex: 0 0 28px;
  height: 28px;
  border-radius: 8px;
  background: #1d4ed8;
  color: #ffffff;
  font-size: 10px;
  font-weight: 900;
}

.interview-bubble {
  max-width: 78%;
  padding: 9px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px 12px 12px 3px;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.interview-message.user .interview-bubble {
  border-color: #2563eb;
  border-radius: 12px 12px 3px 12px;
  background: #2563eb;
  color: #ffffff;
}

.interview-thinking {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 34px;
}

.interview-thinking i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #94a3b8;
  animation: interview-pulse 1s infinite ease-in-out;
}

.interview-thinking i:nth-child(2) { animation-delay: 0.14s; }
.interview-thinking i:nth-child(3) { animation-delay: 0.28s; }

@keyframes interview-pulse {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.45; }
  30% { transform: translateY(-3px); opacity: 1; }
}

.interview-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 14px 10px;
}

.interview-facts {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: nowrap;
  gap: 6px;
  overflow-x: auto;
  padding: 8px 14px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
  scrollbar-width: none;
}

.interview-facts::-webkit-scrollbar {
  display: none;
}

.interview-suggestions button {
  padding: 5px 9px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #ffffff;
  color: #1d4ed8;
  font-size: 11px;
  cursor: pointer;
}

.interview-facts span {
  max-width: 170px;
  overflow: hidden;
  padding: 4px 8px;
  border-radius: 999px;
  background: #e8eefc;
  color: #334155;
  font-size: 10px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.interview-facts .facts-label {
  background: transparent;
  color: #64748b;
  padding-left: 0;
}

.interview-warning {
  margin: 0 14px 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 11px;
}

.interview-warning button {
  margin-left: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #1d4ed8;
  font-weight: 800;
  cursor: pointer;
}

.interview-composer {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 10px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
}

.interview-composer :deep(.ant-input) {
  min-height: 42px;
  padding-right: 42px;
  padding-block: 9px;
  resize: none;
}

.interview-send {
  position: absolute;
  right: 17px;
  bottom: 15px;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  background: #2563eb;
  color: #ffffff;
  font-size: 17px;
  font-weight: 800;
  cursor: pointer;
}

.interview-send:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.recommend-form :deep(.ant-form-item) {
  margin-bottom: 15px;
}

.recommend-form :deep(.ant-form-item-label > label) {
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.recommend-form :deep(.ant-select-selector),
.recommend-form :deep(.ant-input),
.recommend-form :deep(.ant-input-number),
.recommend-form :deep(.ant-input-number-group-addon) {
  border-radius: 8px;
}

.recommend-form :deep(.ant-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.recommend-form :deep(.ant-radio-button-wrapper) {
  height: 32px;
  border-radius: 8px;
  border-left-width: 1px;
  font-size: 13px;
  line-height: 30px;
}

.recommend-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.recommend-action-hint {
  align-self: center;
  margin-right: auto;
  color: #64748b;
  font-size: 11px;
}

.recommend-actions :deep(.ant-btn) {
  min-width: 112px;
  height: 38px;
  border-radius: 8px;
  font-weight: 700;
}

@media (max-width: 640px) {
  .daily-recommend-modal {
    width: calc(100vw - 28px) !important;
  }

  .recommend-intro,
  .recommend-form {
    height: min(540px, calc(100vh - 72px));
    max-height: min(540px, calc(100vh - 72px));
    padding: 18px 18px 16px;
  }

  .recommend-form-header {
    grid-template-columns: 1fr;
    align-items: stretch;
    gap: 10px;
  }

  .recommend-form-heading h2 {
    margin-bottom: 0;
  }

  .recommend-mode-switch small,
  .recommend-action-hint {
    display: none;
  }

  .recommend-mode-switch button {
    justify-content: center;
    padding-inline: 7px;
  }

  .development-goal-grid {
    grid-template-columns: 1fr;
  }

  .development-goal-grid button {
    grid-template-columns: 62px 1fr;
    align-items: center;
  }
}
</style>

<style>
html {
  overflow-y: scroll !important;
}

body.student-no-select {
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
}

body.student-no-select input,
body.student-no-select textarea,
body.student-no-select select {
  -webkit-user-select: auto;
  -moz-user-select: auto;
  -ms-user-select: auto;
  user-select: auto;
}
</style>
