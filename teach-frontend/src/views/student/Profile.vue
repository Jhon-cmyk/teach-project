<template>
  <div class="edu-app">

    <main class="edu-main">
      <div class="page-header">
        <h2 class="page-title">个人设置中心</h2>
        <p class="page-subtitle">管理你的账号信息、查看学习历史与专属收藏</p>
      </div>

      <div class="content-layout">
        <aside class="left-sidebar">
          <div class="user-info-card glass-panel">
            <div class="avatar-upload-wrapper">
              <a-upload
                name="file"
                list-type="picture-card"
                class="avatar-uploader"
                :show-upload-list="false"
                :action="`${API_BASE_URL}/file/upload`"
                :with-credentials="true"
                :before-upload="beforeUpload"
                @change="handleAvatarChange"
              >
                <img v-if="userInfo.userAvatar" :src="getAvatarUrl(userInfo.userAvatar)" alt="avatar" class="avatar-img" />
                <div v-else class="upload-placeholder">
                  <loading-outlined v-if="uploadLoading" />
                  <plus-outlined v-else />
                  <div class="ant-upload-text" style="margin-top: 8px;">上传头像</div>
                </div>
                <div class="avatar-hover-mask">
                  <span style="color: white; font-size: 12px; font-weight: 600;">更换头像</span>
                </div>
              </a-upload>
            </div>

            <h3 class="card-name">{{ userInfo.userName || '同学' }}</h3>
            <p class="card-id">账号：{{ userInfo.userAccount || '---' }}</p>

            <div class="stats-row">
              <div class="stat">
                <span class="num blue-text">{{ userInfo.points || 0 }}</span>
                <span class="label">我的积分</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat">
                <span class="num purple-text">{{ myHistoryList.length }}</span>
                <span class="label">学习足迹</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat">
                <span class="num orange-text">{{ myFavList.length }}</span>
                <span class="label">我的收藏</span>
              </div>
            </div>
          </div>

          <div class="menu-card glass-panel">
            <div class="menu-title">账号管理</div>
            <div class="menu-item" :class="{ active: currentTab === 'settings' }" @click="currentTab = 'settings'">
              <div class="menu-icon"><setting-outlined /></div>
              <span>基本资料</span>
            </div>
            <div class="menu-item" :class="{ active: currentTab === 'homework' }" @click="currentTab = 'homework'">
              <div class="menu-icon"><form-outlined /></div>
              <span>作业记录</span>
            </div>
            <div class="menu-item" :class="{ active: currentTab === 'exam' }" @click="currentTab = 'exam'">
              <div class="menu-icon"><file-text-outlined /></div>
              <span>考试记录</span>
            </div>
            <div class="menu-item" :class="{ active: currentTab === 'history' }" @click="currentTab = 'history'">
              <div class="menu-icon"><history-outlined /></div>
              <span>学习历史</span>
            </div>
            <div class="menu-item" :class="{ active: currentTab === 'fav' }" @click="currentTab = 'fav'">
              <div class="menu-icon"><star-outlined /></div>
              <span>我的收藏</span>
            </div>
            <div class="divider"></div>
            <div class="menu-item danger" @click="handleLogout">
              <div class="menu-icon"><logout-outlined /></div>
              <span>退出登录</span>
            </div>
          </div>
        </aside>

        <main class="right-panel glass-panel">

          <!-- 基本资料 -->
          <div v-if="currentTab === 'settings'" class="fade-in">
            <div class="panel-header">
              <div class="panel-title-group">
                <h3>基本资料</h3>
                <span class="sub-text">更新你的个人信息，展现独特的自己</span>
              </div>
            </div>
            <a-form layout="vertical" class="setting-form">
              <a-form-item label="昵称">
                <a-input v-model:value="formState.userName" size="large" placeholder="请输入你的新昵称" class="modern-input" />
              </a-form-item>
              <a-form-item label="所在大学">
                <a-auto-complete
                  v-model:value="formState.universityName"
                  :options="universityAutocompleteOptions"
                  :filter-option="false"
                  size="large"
                  allow-clear
                  placeholder="搜索或直接输入大学全称"
                  class="university-autocomplete"
                />
              </a-form-item>
              <a-form-item label="发展目标">
                <div class="profile-goal-grid" role="radiogroup" aria-label="发展目标">
                  <button
                    v-for="option in developmentGoalOptions"
                    :key="option.value"
                    type="button"
                    role="radio"
                    :aria-checked="formState.developmentGoal === option.value"
                    :class="{ active: formState.developmentGoal === option.value }"
                    @click="formState.developmentGoal = option.value"
                  >
                    <strong>{{ option.label }}</strong>
                    <span>{{ option.description }}</span>
                  </button>
                </div>
              </a-form-item>
              <a-form-item label="个性签名">
                <a-textarea v-model:value="formState.userProfile" placeholder="写下一句代表你的话吧..." :rows="4" show-count :maxlength="100"/>
              </a-form-item>
              <div class="form-actions">
                <button class="btn-primary" :disabled="updateLoading" @click="handleUpdateInfo">
                  <loading-outlined v-if="updateLoading" style="margin-right: 8px;" /> 保存修改
                </button>
                <button class="btn-secondary" @click.prevent="showPwdModal = true">修改密码</button>
              </div>
            </a-form>
          </div>

          <!-- ====== 作业记录 ====== -->
          <div v-if="currentTab === 'homework'" class="fade-in">
            <div class="panel-header">
              <div class="panel-title-group">
                <h3>作业记录</h3>
                <span class="sub-text">查看你的提交记录、最终分数和教师评语</span>
              </div>
            </div>

            <div class="list-container" v-if="homeworkHistory.length > 0">
              <div v-for="item in homeworkHistory" :key="item.submissionId" class="history-item hw-history-item">
                <div class="hw-left">
                  <div class="hw-title-row">
                    <span class="hw-title">{{ item.title }}</span>
                    <span class="hw-status-tag" :class="item.submitStatus">
                      {{ homeworkStatusText(item.submitStatus) }}
                    </span>
                  </div>
                  <div class="hw-meta">
                    <span v-if="item.submitTime">提交于 {{ formatDate(item.submitTime) }}</span>
                    <span v-if="item.totalScore != null" class="hw-score">得分：<strong>{{ item.totalScore }}</strong></span>
                    <span v-if="item.correctCount != null">对{{ item.correctCount }}题 / 错{{ item.wrongCount }}题</span>
                  </div>
                  <div class="hw-summary" v-if="item.reportSummary">{{ item.reportSummary }}</div>
                </div>
                <div class="record-actions">
                  <button class="btn-outline" @click="viewHomeworkReport(item.submissionId)">查看结果</button>
                  <a-popconfirm
                    title="确定删除这条作业记录吗？"
                    ok-text="删除记录"
                    cancel-text="保留"
                    ok-type="danger"
                    placement="topRight"
                    @confirm="deleteHomeworkHistory(item.submissionId)"
                  >
                    <button
                      class="btn-danger-outline"
                      :disabled="isDeleting('homework', item.submissionId)"
                    >
                      <loading-outlined v-if="isDeleting('homework', item.submissionId)" />
                      <delete-outlined v-else />
                      删除记录
                    </button>
                  </a-popconfirm>
                </div>
              </div>
            </div>

            <div v-else class="empty-state">
              <div class="empty-icon">📝</div>
              <p>还没有作业记录，快去完成老师布置的作业吧！</p>
            </div>
          </div>

          <!-- ====== 考试记录 ====== -->
          <div v-if="currentTab === 'exam'" class="fade-in">
            <div class="panel-header">
              <div class="panel-title-group">
                <h3>考试记录</h3>
                <span class="sub-text">查看你的考试作答记录与教师批阅反馈</span>
              </div>
            </div>

            <div class="list-container" v-if="examHistory.length > 0">
              <div v-for="item in examHistory" :key="item.submissionId" class="history-item hw-history-item">
                <div class="hw-left">
                  <div class="hw-title-row">
                    <span class="hw-title">{{ item.title }}</span>
                    <span class="hw-status-tag" :class="item.submitStatus">
                      {{ item.submitStatus === 'completed' ? '已批阅' : '批阅中' }}
                    </span>
                  </div>
                  <div class="hw-meta">
                    <span v-if="item.submitTime">提交于 {{ formatDate(item.submitTime) }}</span>
                    <span v-if="item.totalScore != null" class="hw-score">得分：<strong>{{ item.totalScore }}</strong></span>
                  </div>
                  <div class="hw-summary" v-if="item.submitStatus === 'completed' && item.reportSummary">{{ item.reportSummary }}</div>
                </div>
                <button
                  class="btn-outline"
                  :disabled="item.submitStatus !== 'completed'"
                  @click="viewExamReport(item.submissionId)"
                >{{ item.submitStatus === 'completed' ? '查看报告' : '批阅中' }}</button>
              </div>
            </div>

            <div v-else class="empty-state">
              <div class="empty-icon">📝</div>
              <p>还没有考试记录，快去完成老师发布的考试吧！</p>
            </div>
          </div>

          <!-- 学习历史 -->
          <div v-if="currentTab === 'history'" class="fade-in history-panel">
            <div class="panel-header">
              <div class="panel-title-group">
                <h3>最近学习</h3>
                <span class="sub-text">温故而知新，快速回到上次的学习状态</span>
              </div>
            </div>
            <div class="list-container">
              <div v-for="item in myHistoryList" :key="item.id" class="history-item">
                <div class="item-cover">
                  <img :src="getCover(item.coverImg)" />
                  <div class="cover-overlay"><div class="play-icon">▶</div></div>
                </div>
                <div class="item-info">
                  <div class="item-title">{{ item.name || '未知课程' }}</div>
                  <div class="item-desc">{{ item.description ? item.description.substring(0, 40) + '...' : '暂无课程简介' }}</div>
                  <div class="item-meta">
                    <span class="time-tag">上次访问: {{ formatDate(item.updateTime) }}</span>
                  </div>
                </div>
                <div class="record-actions">
                  <button class="btn-outline" @click="router.push(`/learn/${item.id}`)">继续学习</button>
                  <a-popconfirm
                    title="确定删除这条学习历史吗？"
                    ok-text="删除记录"
                    cancel-text="保留"
                    ok-type="danger"
                    placement="topRight"
                    @confirm="deleteLearningHistory(item.id)"
                  >
                    <button
                      class="btn-danger-outline"
                      :disabled="isDeleting('history', item.id)"
                    >
                      <loading-outlined v-if="isDeleting('history', item.id)" />
                      <delete-outlined v-else />
                      删除记录
                    </button>
                  </a-popconfirm>
                </div>
              </div>
              <div v-if="myHistoryList.length === 0" class="empty-state">
                <div class="empty-icon">⏳</div>
                <p>还没有学习记录哦，快去探索课程吧！</p>
              </div>
            </div>
          </div>

          <!-- 我的收藏 -->
          <div v-if="currentTab === 'fav'" class="fade-in">
            <div class="panel-header">
              <div class="panel-title-group">
                <h3>我的收藏</h3>
                <span class="sub-text">你珍藏的知识宝库</span>
              </div>
            </div>
            <div class="fav-grid">
              <div v-for="course in myFavList" :key="course.id" class="edu-card" @click="router.push(`/learn/${course.id}`)">
                <div class="card-cover">
                  <img :src="getCover(course.coverImg)" />
                  <div class="badge">已收藏</div>
                </div>
                <div class="card-body">
                  <h4 class="title" :title="course.name">{{ course.name }}</h4>
                  <div class="meta-row">
                    <span class="teacher-info"><a-avatar :size="18" style="background:#4f46e5; margin-right:4px;">T</a-avatar>{{ course.teacherName || '金牌讲师' }}</span>
                    <span class="rating">⭐ 4.9</span>
                  </div>
                  <div class="fav-card-actions" @click.stop>
                    <a-popconfirm
                      title="确定取消收藏这门课程吗？"
                      ok-text="取消收藏"
                      cancel-text="保留"
                      ok-type="danger"
                      placement="topRight"
                      @confirm="deleteFavourite(course.id)"
                    >
                      <button
                        class="fav-remove-button"
                        :disabled="isDeleting('favourite', course.id)"
                      >
                        <loading-outlined v-if="isDeleting('favourite', course.id)" />
                        <delete-outlined v-else />
                        取消收藏
                      </button>
                    </a-popconfirm>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="myFavList.length === 0" class="empty-state">
              <div class="empty-icon">⭐</div>
              <p>收藏夹空空如也，遇到喜欢的课程点个星吧！</p>
            </div>
          </div>
        </main>
      </div>
    </main>

    <!-- 修改密码弹窗 -->
    <a-modal v-model:open="showPwdModal" title="安全设置 - 修改密码" @ok="handleChangePassword" :confirmLoading="pwdLoading" centered :bodyStyle="{ padding: '24px' }">
      <a-form layout="vertical">
        <a-form-item label="当前密码" required><a-input-password v-model:value="pwdForm.oldPassword" placeholder="请输入当前密码" size="large" /></a-form-item>
        <a-form-item label="新密码" required><a-input-password v-model:value="pwdForm.newPassword" placeholder="设置新的安全密码(至少6位)" size="large" /></a-form-item>
        <a-form-item label="确认新密码" required><a-input-password v-model:value="pwdForm.confirmPassword" placeholder="再次输入确认" size="large" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- 作业报告详情弹窗 -->
    <a-modal
      v-model:open="reportModalVisible"
      :title="reportModalTitle"
      width="1080px"
      :footer="null"
      centered
      :body-style="{ padding: '18px 22px 22px' }"
    >
      <div v-if="reportDetail" class="report-modal-body">
        <HomeworkReportPanel :report="reportDetail" role="student" :show-submission-id="false" />
      </div>
      <a-spin v-else tip="加载中..." style="display: block; text-align: center; padding: 60px 0;" />
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { setTabLoginUser } from '@/utils/authStorage';
import { fetchStudentLearningContext, updateStudentLearningContext } from '@/api/learning';
import { getUniversityOptions } from '@/data/chineseUniversities';
import { useUserStore } from '@/stores/user'
import { message } from 'ant-design-vue';
import HomeworkReportPanel from '@/components/homework/HomeworkReportPanel.vue';
import {
  SettingOutlined, HistoryOutlined, StarOutlined, LogoutOutlined,
  PlusOutlined, LoadingOutlined, FormOutlined, FileTextOutlined, DeleteOutlined,
} from '@ant-design/icons-vue';

const router = useRouter();
const route = useRoute();
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const SERVER_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const normalizeServerAssetUrl = (url?: string) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:image')) return url
  return `${SERVER_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`
}

const getAvatarUrl = (url?: string) => normalizeServerAssetUrl(url)
const currentTab = ref(String(route.query.tab || 'settings'));
const userInfo = ref<any>({});
const myHistoryList = ref<any[]>([]);
const myFavList = ref<any[]>([]);

const homeworkStatusText = (status?: string) => {
  const map: Record<string, string> = {
    completed: '已批改',
    review_pending: '待教师批改',
    judging: '待教师批改',
    submitted: '已提交',
    failed: '提交失败',
    pending: '未提交'
  }
  return status ? (map[status] || status) : '未提交'
}

const formState = reactive({
  userName: '',
  userProfile: '',
  universityName: '',
  developmentGoal: '' as 'postgraduate' | 'employment' | 'undecided' | ''
});
const developmentGoalOptions = [
  { value: 'postgraduate' as const, label: '考研', description: '基础、原理与考试训练' },
  { value: 'employment' as const, label: '就业', description: '项目、实战与面试能力' },
  { value: 'undecided' as const, label: '暂未确定', description: '按课程与薄弱点推荐' }
];
const updateLoading = ref(false);
const showPwdModal = ref(false);
const pwdLoading = ref(false);
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });
const uploadLoading = ref(false);

// 作业历史
const homeworkHistory = ref<any[]>([]);
const reportModalVisible = ref(false);
const reportDetail = ref<any>(null);
const reportModalTitle = ref('作业批改结果');

// 考试历史
const examHistory = ref<any[]>([]);
const deletingRecordKey = ref('');

type DeletableRecordType = 'homework' | 'history' | 'favourite';
const recordKey = (type: DeletableRecordType, id: number) => `${type}:${id}`;
const isDeleting = (type: DeletableRecordType, id: number) => deletingRecordKey.value === recordKey(type, id);

const getCover = (imgUrl: string) => {
  return normalizeServerAssetUrl(imgUrl) || `https://picsum.photos/300/180?random=${Math.random()}`
};
const formatDate = (dateStr: string) => (!dateStr ? '刚刚' : new Date(dateStr).toLocaleDateString());

const universityAutocompleteOptions = computed(() => getUniversityOptions(formState.universityName));

const fetchLearningContext = async () => {
  try {
    const context = await fetchStudentLearningContext();
    formState.universityName = context?.universityName || '';
    formState.developmentGoal = context?.developmentGoal || '';
  } catch {
    // The fields remain editable even if the initial context request fails.
  }
};

const fetchLatestUserInfo = async () => {
  try {
    const data = await request.get<any, any>('/user/get/login', {
      skipErrorToast: true
    });

    userInfo.value = data || {};
    formState.userName = userInfo.value.userName || '';
    formState.userProfile = userInfo.value.userProfile || '';
    setTabLoginUser(userInfo.value);
  } catch (e) {
    console.error(e);
    message.error('登录状态已失效，请重新登录');
    router.push('/auth');
  }
};

const handleUpdateInfo = async () => {
  if (!formState.userName) return message.warning('昵称不能为空');
  formState.universityName = formState.universityName.trim();
  if (!formState.universityName) return message.warning('请填写所在大学');
  if (!formState.developmentGoal) return message.warning('请选择发展目标');
  updateLoading.value = true;
  try {
    await Promise.all([
      request.post<any, any>('/user/update/my', {
        userName: formState.userName,
        userProfile: formState.userProfile,
        userAvatar: userInfo.value.userAvatar
      }, {
        skipErrorToast: true
      }),
      updateStudentLearningContext({
        universityName: formState.universityName,
        developmentGoal: formState.developmentGoal
      })
    ]);

    message.success('资料更新成功');
    fetchLatestUserInfo();
  } catch (e: any) {
    message.error(e?.message || '保存失败');
  } finally {
    updateLoading.value = false;
  }
};

const beforeUpload = (file: any) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
  if (!isJpgOrPng) message.error('只能上传 JPG/PNG 文件!');
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) message.error('图片必须小于 2MB!');
  return isJpgOrPng && isLt2M;
};

const handleAvatarChange = (info: any) => {
  if (info.file.status === 'uploading') { uploadLoading.value = true; return; }
  if (info.file.status === 'done') {
    uploadLoading.value = false;
    const res = info.file.response;
    if (res.code === 0) { userInfo.value.userAvatar = res.data; message.success('头像上传成功，记得点击【保存修改】哦'); }
    else { message.error('上传失败'); }
  }
  if (info.file.status === 'error') { uploadLoading.value = false; message.error('上传出错'); }
};

const handleChangePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
    return message.warning('请完整填写密码信息');
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    return message.warning('两次输入的新密码不一致');
  }

  pwdLoading.value = true;
  try {
    await request.post<any, any>('/user/update/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    }, {
      skipErrorToast: true
    });

    message.success('密码修改成功，请重新登录');
    showPwdModal.value = false;
    setTimeout(() => handleLogout(), 1000);
  } catch (e: any) {
    message.error(e?.message || '密码修改失败');
  } finally {
    pwdLoading.value = false;
  }
};
const userStore = useUserStore()

const handleLogout = async () => {
  await userStore.logout()     // 🌟 统一由 store 清理
  router.push('/auth')         // 跳转路径保持原样
}

const fetchHistory = async () => {
  try {
    const data = await request.get<any[], any[]>('/history/my', {
      skipErrorToast: true
    });
    myHistoryList.value = data || [];
  } catch (e) {}
};
const fetchMyFavs = async () => {
  try {
    const data = await request.get<any[], any[]>('/favour/my', {
      skipErrorToast: true
    });
    myFavList.value = data || [];
  } catch (e) {}
};

const deleteLearningHistory = async (courseId: number) => {
  deletingRecordKey.value = recordKey('history', courseId);
  try {
    await request.delete<any, any>(`/history/${courseId}`, { skipErrorToast: true });
    myHistoryList.value = myHistoryList.value.filter(item => item.id !== courseId);
    message.success('学习历史已删除');
  } catch (e: any) {
    message.error(e?.message || '删除学习历史失败');
  } finally {
    deletingRecordKey.value = '';
  }
};

const deleteFavourite = async (courseId: number) => {
  deletingRecordKey.value = recordKey('favourite', courseId);
  try {
    await request.delete<any, any>(`/favour/${courseId}`, { skipErrorToast: true });
    myFavList.value = myFavList.value.filter(course => course.id !== courseId);
    message.success('已取消收藏');
  } catch (e: any) {
    message.error(e?.message || '取消收藏失败');
  } finally {
    deletingRecordKey.value = '';
  }
};

// ====== 作业记录相关 ======
const fetchHomeworkHistory = async () => {
  try {
    const data = await request.get<any[], any[]>('/homework/student/history', {
      skipErrorToast: true
    });
    homeworkHistory.value = data || [];
  } catch (e) {
    console.warn('获取作业历史失败', e);
  }
};

const deleteHomeworkHistory = async (submissionId: number) => {
  deletingRecordKey.value = recordKey('homework', submissionId);
  try {
    await request.delete<any, any>(`/homework/student/history/${submissionId}`, { skipErrorToast: true });
    homeworkHistory.value = homeworkHistory.value.filter(item => item.submissionId !== submissionId);
    message.success('作业记录已删除');
  } catch (e: any) {
    message.error(e?.message || '删除作业记录失败');
  } finally {
    deletingRecordKey.value = '';
  }
};

const viewHomeworkReport = async (submissionId: number) => {
  reportDetail.value = null;
  reportModalTitle.value = '作业批改结果';
  reportModalVisible.value = true;
  try {
    const data = await request.get<any, any>('/homework/student/report', {
      params: { submissionId },
      skipErrorToast: true
    });
    reportDetail.value = data;
  } catch (e: any) {
    message.error(e?.message || '加载报告失败');
  }
};

// ====== 考试记录相关 ======
const fetchExamHistory = async () => {
  try {
    const data = await request.get<any[], any[]>('/exam/student/history', {
      skipErrorToast: true
    });
    examHistory.value = data || [];
  } catch (e) {
    console.warn('获取考试历史失败', e);
  }
};

const viewExamReport = async (submissionId: number) => {
  reportDetail.value = null;
  reportModalTitle.value = '考试批阅结果';
  reportModalVisible.value = true;
  try {
    const data = await request.get<any, any>('/exam/student/report', {
      params: { submissionId },
      skipErrorToast: true
    });
    reportDetail.value = data;
  } catch (e: any) {
    message.error(e?.message || '加载报告失败');
  }
};

onMounted(() => {
  fetchLatestUserInfo();
  fetchLearningContext();
  fetchHistory();
  fetchMyFavs();
  fetchHomeworkHistory();
  fetchExamHistory();
});
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');
.edu-app { height: calc(100vh - 82px); overflow: hidden; display: flex; flex-direction: column; background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%); font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif; color: #0f172a; }
.glass-panel { background: #FFFFFF; border: 1px solid rgba(0,0,0,0.03); box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04); border-radius: 5px; }
.edu-main { flex: 1; min-height: 0; width: 75%; max-width: 1600px; min-width: 1200px; margin: 0 auto; padding: 24px; box-sizing: border-box; display: flex; flex-direction: column; }
.page-header { margin-bottom: 20px; flex-shrink: 0; }
.page-title { font-size: 24px; font-weight: 800; color: #0f172a; margin-bottom: 4px; }
.page-subtitle { color: #64748b; font-size: 13px; margin: 0; }
.content-layout { flex: 1; min-height: 0; display: flex; gap: 24px; align-items: stretch; }
.left-sidebar { width: 280px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; height: 100%; }
.user-info-card { padding: 24px 20px; text-align: center; display: flex; flex-direction: column; align-items: center; }
.avatar-upload-wrapper { margin-bottom: 16px; }
.card-name { font-size: 18px; font-weight: 800; margin-bottom: 4px; color: #0f172a; }
.card-id { font-size: 12px; color: #64748b; margin-bottom: 20px; }
.stats-row { border-radius: 5px; width: 100%; display: flex; justify-content: space-between; align-items: center; background: #f8fafc; padding: 12px; }
.stat { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1; }
.stat .num { font-size: 18px; font-weight: 800; }
.stat .label { font-size: 11px; color: #64748b; font-weight: 600; }
.blue-text { color: #3b82f6; }
.purple-text { color: #8b5cf6; }
.orange-text { color: #f59e0b; }
.stat-divider { width: 1px; height: 24px; background: #e2e8f0; }
.menu-card { padding: 16px; flex: 1; }
.menu-title { font-size: 12px; font-weight: 800; color: #94a3b8; margin-bottom: 12px; padding-left: 8px; text-transform: uppercase; letter-spacing: 0.5px; }
.menu-item { border-radius: 5px; display: flex; align-items: center; gap: 12px; padding: 10px 16px; color: #64748b; font-weight: 600; cursor: pointer; transition: all 0.2s; margin-bottom: 4px; }
.menu-item:hover { background: #f8fafc; color: #0f172a; }
.menu-item.active { background: #eff6ff; color: #4f46e5; }
.menu-item.danger { color: #ef4444; margin-top: auto; }
.menu-item.danger:hover { background: #fef2f2; }
.menu-icon { border-radius: 5px; width: 32px; height: 32px; background: #f1f5f9; display: flex; justify-content: center; align-items: center; transition: 0.2s; }
.menu-item.active .menu-icon { background: #e0e7ff; color: #4f46e5; }
.menu-item.danger .menu-icon { background: #fee2e2; color: #ef4444; }
.divider { height: 1px; background: #f1f5f9; margin: 12px 0; }
.right-panel { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 24px 32px; overflow-y: auto; }
.right-panel::-webkit-scrollbar { width: 6px; }
.right-panel::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.panel-header { margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid #e8eef6; flex-shrink: 0; }
.panel-title-group h3 { font-size: 22px; font-weight: 800; color: #0f172a; margin-bottom: 8px; }
.panel-title-group .sub-text { color: #64748b; font-size: 13px;}
.fade-in { animation: fadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.setting-form { max-width: 500px; }
.profile-goal-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.profile-goal-grid button { display: grid; gap: 4px; min-width: 0; padding: 11px 10px; border: 1px solid #dbe5f1; border-radius: 6px; background: #fff; color: #475569; text-align: left; cursor: pointer; transition: 0.2s; }
.profile-goal-grid button:hover { border-color: #93b4ee; }
.profile-goal-grid button.active { border-color: #2563eb; background: #eff6ff; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.08); }
.profile-goal-grid strong { color: #0f172a; font-size: 12px; }
.profile-goal-grid button.active strong { color: #1d4ed8; }
.profile-goal-grid span { color: #64748b; font-size: 10px; line-height: 1.35; }
.modern-input { border-radius: 5px; border: 1px solid #dbe5f1; padding: 10px 16px; background: #f8fafc; color: #0f172a; transition: 0.3s; }
.modern-input:focus { background: #fff; border-color: #4f46e5; box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1); }
:deep(.university-autocomplete.ant-select) { width: 100%; }
:deep(.university-autocomplete .ant-select-selector) { height: 46px !important; padding: 0 48px 0 16px !important; border-radius: 5px !important; border-color: #dbe5f1 !important; background: #ffffff !important; align-items: center; }
:deep(.university-autocomplete.ant-select-focused .ant-select-selector) { border-color: #4f46e5 !important; box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1) !important; }
:deep(.university-autocomplete .ant-select-selection-search-input) { height: 44px !important; }
:deep(.university-autocomplete .ant-select-selection-placeholder) { line-height: 44px !important; }
:deep(.university-autocomplete .ant-select-clear) { top: 50%; right: 16px; display: grid; place-items: center; width: 18px; height: 18px; margin-top: 0; transform: translateY(-50%); border-radius: 50%; background: #94a3b8; color: #ffffff; font-size: 11px; }
:deep(.university-autocomplete .ant-select-clear:hover) { background: #64748b; }
.form-actions { margin-top: 40px; display: flex; gap: 16px; }
.btn-primary { border-radius: 5px; background: linear-gradient(135deg, #4f46e5, #6366f1); color: #fff; border: none; padding: 0 32px; height: 44px; font-weight: 600; cursor: pointer; transition: 0.2s; box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2); display: flex; align-items: center; }
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3); }
.btn-primary:disabled { opacity: 0.7; cursor: not-allowed; transform: none; }
.btn-secondary { border-radius: 5px; background: #fff; color: #64748b; border: 1px solid #e2e8f0; padding: 0 24px; height: 44px; font-weight: 600; cursor: pointer; transition: 0.2s; }
.btn-secondary:hover { color: #1e293b; border-color: #cbd5e1; background: #f8fafc; }
.history-panel { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.list-container { flex: 1; min-height: 0; overflow-y: auto; padding-right: 8px; display: flex; flex-direction: column; gap: 14px; }
.history-item { border-radius: 5px; display: flex; align-items: center; gap: 20px; padding: 12px; background: #fff; border: 1px solid #eaf0f7; transition: all 0.3s ease; }
.history-item:hover { border-color: #e2e8f0; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.item-cover { border-radius: 5px; position: relative; width: 140px; height: 80px; overflow: hidden; flex-shrink: 0; background: #f1f5f9; }
.item-cover img { width: 100%; height: 100%; object-fit: cover; transition: 0.3s; }
.history-item:hover .item-cover img { transform: scale(1.05); }
.cover-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.2); display: flex; justify-content: center; align-items: center; opacity: 0; transition: 0.3s; }
.history-item:hover .cover-overlay { opacity: 1; }
.play-icon { width: 36px; height: 36px; background: rgba(255,255,255,0.9); border-radius: 50%; display: flex; justify-content: center; align-items: center; color: #4f46e5; padding-left: 2px; }
.item-info { flex: 1; }
.item-title { font-weight: 800; font-size: 15px; margin-bottom: 6px; color: #1e293b; }
.item-desc { font-size: 12px; color: #64748b; margin-bottom: 8px; line-height: 1.5; }
.time-tag { background: #f1f5f9; color: #64748b; padding: 4px 8px; border-radius: 5px; font-size: 11px; font-weight: 600; }
.btn-outline { border-radius: 5px; background: transparent; border: 1px solid #dbe4ee; color: #4f46e5; font-size: 14px; font-weight: 700; min-height: 36px; padding: 0 16px; cursor: pointer; transition: 0.2s; white-space: nowrap; flex-shrink: 0; }
.btn-outline:hover { border-color: #4f46e5; background: #eff6ff; }
.record-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.btn-danger-outline { border-radius: 5px; background: #fff; border: 1px solid #fecaca; color: #dc2626; font-size: 13px; font-weight: 700; min-height: 36px; padding: 0 13px; cursor: pointer; transition: 0.2s; white-space: nowrap; display: inline-flex; align-items: center; gap: 6px; }
.btn-danger-outline:hover:not(:disabled) { border-color: #ef4444; background: #fef2f2; }
.btn-danger-outline:disabled { opacity: 0.55; cursor: wait; }
.fav-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
.edu-card { border-radius: 5px; background: #fff; overflow: hidden; border: 1px solid #f1f5f9; transition: all 0.3s ease; cursor: pointer; }
.edu-card:hover { transform: translateY(-4px); box-shadow: 0 10px 20px rgba(0,0,0,0.06); border-color: #e2e8f0; }
.card-cover { border-radius: 5px 5px 0 0; position: relative; aspect-ratio: 16/10; overflow: hidden; background: #f1f5f9; }
.card-cover img { width: 100%; height: 100%; object-fit: cover; transition: 0.5s; }
.edu-card:hover .card-cover img { transform: scale(1.05); }
.badge { border-radius: 4px; position: absolute; top: 10px; right: 10px; background: rgba(255,255,255,0.9); color: #f59e0b; font-size: 11px; font-weight: 700; padding: 4px 8px; }
.card-body { padding: 16px; }
.title { font-size: 14px; font-weight: 700; margin-bottom: 10px; line-height: 1.4; color: #1e293b; height: 38px; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.meta-row { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #64748b; }
.teacher-info { display: flex; align-items: center; font-weight: 600; }
.rating { color: #f59e0b; font-weight: 700; }
.fav-card-actions { margin-top: 14px; padding-top: 12px; border-top: 1px solid #f1f5f9; display: flex; justify-content: flex-end; }
.fav-remove-button { border: 0; background: transparent; color: #dc2626; font-size: 12px; font-weight: 700; padding: 4px 0; cursor: pointer; display: inline-flex; align-items: center; gap: 5px; }
.fav-remove-button:hover:not(:disabled) { color: #b91c1c; }
.fav-remove-button:disabled { opacity: 0.55; cursor: wait; }
.empty-state { text-align: center; padding: 30px 0; color: #94a3b8; }
.empty-icon { font-size: 40px; margin-bottom: 12px; opacity: 0.8; }

/* ====== 作业历史专用样式 ====== */
.hw-history-item { flex-direction: row; align-items: flex-start; padding: 18px 22px; min-height: 98px; }
.hw-left { flex: 1; min-width: 0; }
.hw-title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 9px; }
.hw-title { font-size: 16px; font-weight: 750; color: #111827; line-height: 1.35; }
.hw-status-tag { font-size: 12px; font-weight: 800; padding: 3px 9px; border-radius: 5px; }
.hw-status-tag.completed { background: #ECFDF5; color: #059669; }
.hw-status-tag.judging { background: #FFF7ED; color: #D97706; }
.hw-status-tag.failed { background: #FEF2F2; color: #EF4444; }
.hw-status-tag.draft { background: #F1F5F9; color: #64748B; }
.hw-meta { display: flex; flex-wrap: wrap; gap: 8px 16px; font-size: 13px; color: #475569; margin-bottom: 8px; align-items: center; line-height: 1.5; }
.hw-score strong { color: #2563EB; font-size: 15px; }
.hw-summary { font-size: 13px; color: #64748b; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

/* 报告弹窗 */
.report-modal-body { max-height: min(78vh, 760px); overflow-y: auto; padding-right: 6px; }
.report-score-bar { display: flex; align-items: baseline; gap: 6px; padding: 16px 20px; background: #EFF6FF; border-radius: 8px; margin-bottom: 20px; border: 1px solid #BFDBFE; }
.report-score-label { font-size: 14px; color: #64748B; font-weight: 600; }
.report-score-num { font-size: 32px; font-weight: 800; color: #2563EB; }
.report-score-unit { font-size: 16px; color: #2563EB; }
.report-extra { margin-left: 16px; font-size: 13px; color: #64748B; }
.report-md { padding: 0 4px; }

/* 考试报告详情 */
.teacher-remark-block { background: #FFFBEB; border: 1px solid #FDE68A; border-radius: 5px; padding: 12px 16px; margin-bottom: 16px; }
.teacher-remark-block strong { color: #92400E; }
.teacher-remark-block p { margin: 4px 0 0; color: #78350F; font-size: 14px; }
.exam-detail-list { display: flex; flex-direction: column; gap: 12px; }
.exam-detail-item { background: #FFFFFF; border: 1px solid #E7ECF3; border-radius: 5px; padding: 14px 16px; }
.exam-detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-weight: 600; color: #1F2937; }
.detail-score { font-weight: 700; font-size: 16px; }
.score-pass { color: #16A34A; }
.score-fail { color: #DC2626; }
.exam-detail-stem { font-size: 13px; color: #475569; line-height: 1.7; margin-bottom: 8px; }
.exam-detail-answer { font-size: 13px; margin-bottom: 4px; }
.exam-detail-label { color: #64748B; margin-right: 4px; }
.exam-detail-value { color: #2563EB; font-weight: 500; word-break: break-all; }
.exam-answer-images { display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0 10px; padding-left: 72px; }
.exam-answer-image { border: 1px solid #dbeafe; border-radius: 5px; overflow: hidden; background: #f8fafc; }
.exam-answer-image :deep(.ant-image-img) { object-fit: cover; display: block; }

/* 子Tab样式 */
.hw-sub-tabs { margin-top: 8px; }
.hw-sub-tabs :deep(.ant-tabs-nav) { margin-bottom: 0; }

:deep(.doc-style) { color: #334155; line-height: 1.8; font-size: 14px; }
:deep(.doc-style h1) { font-size: 18px; text-align: center; border-bottom: 1px solid #E7ECF3; padding-bottom: 12px; color: #1F2937; }
:deep(.doc-style h2) { font-size: 15px; background: #F8FAFD; padding: 8px 12px; border-left: 3px solid #2563EB; margin: 24px 0 12px; color: #1F2937; }

:deep(.ant-form-item-label > label) { color: #334155 !important; font-weight: 700; }
:deep(.ant-input), :deep(.ant-input-affix-wrapper), :deep(.ant-input-number), :deep(.ant-input-textarea textarea) { color: #0f172a !important; background: #ffffff !important; }
:deep(.ant-input::placeholder), :deep(.ant-input-textarea textarea::placeholder) { color: #94a3b8 !important; }
:deep(.ant-upload.ant-upload-select-picture-card) { width: 100px !important; height: 100px !important; border-radius: 50% !important; overflow: hidden !important; background: #f8fafc !important; padding: 0 !important; border: 1px solid #e2e8f0 !important; }
:deep(.ant-upload.ant-upload-select-picture-card > .ant-upload) { padding: 0 !important; display: block !important; position: relative; width: 100%; height: 100%; }
.avatar-img { width: 100%; height: 100%; object-fit: contain; object-position: center center; display: block; position: absolute; top: 0; left: 0; z-index: 1; }
.avatar-hover-mask { position: absolute; inset: 0; background: rgba(0, 0, 0, 0.4); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.3s ease; z-index: 2; border-radius: 50%; }
:deep(.ant-upload.ant-upload-select-picture-card):hover .avatar-hover-mask { opacity: 1; }
</style>
