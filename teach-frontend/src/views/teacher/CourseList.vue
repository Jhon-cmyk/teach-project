<template>
  <div class="teaching-manage modern-page">
    <div class="page-header">
      <div class="title-group">
        <h2><read-outlined class="title-icon" /> 教学管理中心</h2>
        <p class="subtitle">集中管理您的课程体系、选集资源与带教班级数据。</p>
      </div>

      <div class="header-actions" :class="{ 'header-actions--hidden': activeTab !== 'course' }">
        <div class="search-box">
          <search-outlined class="s-icon" />
          <input
            v-model="searchText"
            @keyup.enter="fetchData"
            type="text"
            placeholder="搜索课程名称..."
          />
        </div>
        <button class="primary-btn" @click="openModal('add')">
          <plus-outlined /> 发布新课程
        </button>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab" class="custom-tabs" size="large">
      <a-tab-pane key="course">
        <template #tab>
          <span><book-outlined /> 课程资源管理</span>
        </template>

        <div class="table-container glass-panel">
          <a-table
            :columns="columns"
            :data-source="courseList"
            :loading="loading"
            :pagination="pagination"
            @change="handleTableChange"
            row-key="id"
            class="modern-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'coverImg'">
                <div class="cover-wrapper">
                  <img :src="normalizeServerAssetUrl(record.coverImg) || 'https://via.placeholder.com/150'" class="cover-thumb" alt="封面" />
                </div>
              </template>

              <template v-if="column.dataIndex === 'name'">
                <div class="course-name">{{ record.name }}</div>
              </template>

              <template v-if="column.dataIndex === 'description'">
                <div class="desc-text" :title="record.description">{{ record.description || '暂无简介' }}</div>
              </template>

              <template v-if="column.key === 'type'">
                <span class="type-tag" :class="record.type === 'video' ? 'video-tag' : 'text-tag'">
                  <video-camera-outlined v-if="record.type === 'video'" class="tag-icon" />
                  <file-text-outlined v-else class="tag-icon" />
                </span>
              </template>

              <template v-if="column.key === 'action'">
                <div class="action-group">
                  <button class="icon-action-btn list" @click="openChapterDrawer(record)" title="选集目录管理">
                    <unordered-list-outlined />
                  </button>
                  <button class="icon-action-btn edit" @click="openModal('edit', record)" title="编辑基础信息">
                    <edit-outlined />
                  </button>
                  <a-popconfirm
                    title="确定要删除这门课程及所有章节吗？"
                    @confirm="handleDelete(record.id)"
                    okText="确认删除"
                    okType="danger"
                    cancelText="取消"
                  >
                    <button class="icon-action-btn delete" title="删除课程">
                      <delete-outlined />
                    </button>
                  </a-popconfirm>
                </div>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <a-tab-pane key="class">
        <template #tab>
          <span><team-outlined /> 班级与学生管理</span>
        </template>

        <div class="class-grid" v-if="myClasses.length > 0">
          <div class="class-card glass-panel" v-for="cls in myClasses" :key="cls.id">
            <div class="card-top">
              <div class="class-icon">
                <bank-outlined />
              </div>
              <div class="class-info">
                <h3>{{ cls.name }}</h3>
                <span class="major">计算机科学与技术系</span>
              </div>
            </div>
            <div class="card-bottom">
              <div class="stats">
                <span class="student-count"><user-outlined /> {{ cls.studentCount || 0 }} 人</span>
              </div>
              <a-button type="primary" class="view-student-btn" @click="openStudentDrawer(cls)">
                查看学生名单
              </a-button>
            </div>
          </div>
        </div>

        <div v-else class="empty-box glass-panel">
          <div class="empty-icon"><inbox-outlined /></div>
          <h3>暂无带教班级</h3>
          <p>系统未查询到您名下分配的班级数据。</p>
        </div>
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="modalVisible"
      @ok="handleSubmit"
      :confirmLoading="submitLoading"
      width="600px"
      centered
      class="modern-modal"
      okText="确认保存"
      cancelText="取消"
    >
      <template #title>
        <div class="modal-custom-title">
          <plus-circle-outlined v-if="modalType === 'add'" class="m-icon add-icon" />
          <edit-outlined v-else class="m-icon edit-icon" />
          <span>{{ modalType === 'add' ? '发布新课程' : '编辑课程信息' }}</span>
        </div>
      </template>

      <a-form layout="vertical" :model="formState" class="custom-form" style="margin-top: 30px;">
        <a-form-item label="课程名称" required>
          <a-input v-model:value="formState.name" placeholder="请输入吸引人的课程标题" size="large" />
        </a-form-item>
        <a-form-item label="课程简介">
          <a-textarea v-model:value="formState.description" placeholder="简要介绍课程亮点和学习目标..." :rows="3" />
        </a-form-item>
        <a-form-item label="课程封面">
          <a-upload
            v-model:file-list="fileList" name="file" list-type="picture-card" class="avatar-uploader"
            :show-upload-list="false" :customRequest="(opts: any) => customRequest(opts, 'img')"
          >
            <img v-if="formState.coverImg" :src="normalizeServerAssetUrl(formState.coverImg)" alt="avatar" class="uploaded-img" />
            <div v-else class="upload-placeholder">
              <loading-outlined v-if="uploadLoading" /> <plus-outlined v-else />
              <div class="ant-upload-text" style="margin-top: 8px; font-size: 13px">点击上传封面</div>
            </div>
          </a-upload>
        </a-form-item>
        <a-form-item label="分配班级 (可多选)" class="full-width">
          <a-select
            v-model:value="formState.classIds" mode="multiple" placeholder="请选择要上这门课的班级..."
            :options="classList" size="large"
          />
        </a-form-item>
        <a-form-item label="学习监测设置" class="full-width">
          <div class="course-monitor-setting">
            <div>
              <strong>要求学生看课时开启人脸检测</strong>
              <p>开启后，学生进入看课页会看到教师要求，并需要授权摄像头进行学习状态记录。</p>
            </div>
            <a-switch v-model:checked="formState.faceDetectionRequired" />
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="chapterDrawerVisible"
      width="980px"
      centered
      :footer="null"
      class="modern-modal chapter-manage-modal"
    >
      <template #title>
        <div class="modal-custom-title">
          <play-square-outlined class="d-icon" />
          <span>《{{ activeCourse?.name || '' }}》- 选集管理</span>
        </div>
      </template>

      <div class="chapter-modal-body">
        <div class="chapter-ai-overview">
          <div>
            <h3>视频字幕识别与知识点切分</h3>
            <p>上传选集后系统会自动识别语音、生成字幕时间线，并切分成可用于学生提醒的知识点时间段。</p>
          </div>
          <div class="chapter-ai-steps">
            <span>上传视频</span>
            <span>语音识别</span>
            <span>知识点确认</span>
          </div>
        </div>

        <div class="chapter-add-box glass-panel">
        <h3 class="box-title"><plus-circle-outlined /> 添加新选集</h3>
        <p class="chapter-add-tip">保存后会自动进入后台识别，教师不需要手动填写时间段。</p>
        <a-form layout="vertical">
          <a-form-item label="本集标题" required><a-input v-model:value="chapterForm.title" placeholder="例如: 01. Java环境搭建" /></a-form-item>
          <div class="form-row">
            <a-form-item label="播放顺序" required class="half-width"><a-input-number v-model:value="chapterForm.sortOrder" :min="1" style="width: 100%" /></a-form-item>
            <a-form-item label="视频文件" required class="half-width">
              <a-upload v-model:file-list="chapterVideoList" :show-upload-list="false" :customRequest="(opts: any) => customRequest(opts, 'chapterVideo')">
                <a-button :loading="videoUploadLoading" block><upload-outlined /> {{ chapterForm.videoUrl ? '重新上传' : '点击上传' }}</a-button>
              </a-upload>
            </a-form-item>
          </div>
          <a-button type="primary" block @click="submitChapter" :loading="chapterSubmitLoading" style="border-radius: 5px;">保存选集并开始识别</a-button>
        </a-form>
        </div>
      <a-divider style="border-color: #e2e8f0;">已发布的选集</a-divider>
      <a-table
        :dataSource="chapterList"
        :columns="chapterColumns"
        size="small"
        rowKey="id"
        :loading="chapterListLoading"
        :pagination="false"
        :rowClassName="chapterRowClassName"
        class="chapter-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sortOrder'"><a-tag color="blue">第 {{ record.sortOrder }} 集</a-tag></template>
          <template v-if="column.key === 'timelineStatus'">
            <div class="timeline-status-cell">
              <span class="timeline-status-pill" :class="`is-${getChapterTimelineStatus(record).status}`">
                <span class="status-dot"></span>
                {{ getChapterTimelineStatus(record).label }}
              </span>
              <span class="timeline-status-detail">{{ getChapterTimelineStatus(record).detail }}</span>
            </div>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openSegmentEditor(record)">查看时间轴</a-button>
            <a-popconfirm title="确定删除吗？" @confirm="deleteChapter(record.id)" okText="删除" okType="danger">
              <a-button type="text" danger size="small"><delete-outlined /></a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>

      <div v-if="activeSegmentChapter" class="segment-editor">
        <div class="segment-editor-head">
          <div>
            <div class="segment-title-line">
              <h3>知识点时间轴</h3>
              <span class="timeline-status-pill" :class="`is-${getChapterTimelineStatus(activeSegmentChapter).status}`">
                <span class="status-dot"></span>
                {{ getChapterTimelineStatus(activeSegmentChapter).label }}
              </span>
            </div>
            <p>{{ activeSegmentChapter.title }}</p>
            <p v-if="timelineAiStatusText" class="segment-task-status">{{ timelineAiStatusText }}</p>
          </div>
          <div class="segment-actions">
            <a-button size="small" :loading="timelineAiGenerating" @click="handleGenerateTimelineDraft">重新识别</a-button>
            <a-button size="small" @click="addSegmentRow">添加一段</a-button>
            <a-button type="primary" size="small" :loading="segmentSaving" @click="saveSegments">确认保存</a-button>
          </div>
        </div>
        <a-spin :spinning="segmentLoading">
          <div v-if="segmentList.length" class="segment-list">
            <div v-for="(segment, index) in segmentList" :key="segment.id || index" class="segment-card">
              <div class="segment-card-top">
                <div class="segment-index">#{{ index + 1 }}</div>
                <a-button type="text" danger size="small" @click="removeSegmentRow(index)">删除</a-button>
              </div>
              <div class="segment-form-grid">
                <label class="segment-field time-field">
                  <span>开始时间</span>
                  <a-input v-model:value="segment.startText" placeholder="00:00" />
                </label>
                <label class="segment-field time-field">
                  <span>结束时间</span>
                  <a-input v-model:value="segment.endText" placeholder="05:20" />
                </label>
                <label class="segment-field name-field">
                  <span>知识点名称</span>
                  <a-input v-model:value="segment.knowledgeName" placeholder="例如：数据结构的基本概念" />
                </label>
                <label class="segment-field difficulty-field">
                  <span>难度</span>
                  <a-select v-model:value="segment.difficulty">
                    <a-select-option value="低">低</a-select-option>
                    <a-select-option value="中">中</a-select-option>
                    <a-select-option value="高">高</a-select-option>
                  </a-select>
                </label>
                <label class="segment-field desc-field">
                  <span>老师标注说明</span>
                  <a-textarea
                    v-model:value="segment.description"
                    :auto-size="{ minRows: 2, maxRows: 4 }"
                    placeholder="写给 AI 助教的讲解依据，例如：这里重点解释抽象数据类型与数据结构的区别。"
                  />
                </label>
              </div>
            </div>
          </div>
          <div v-else class="segment-empty">
            <strong>{{ timelineAiGenerating ? '正在生成知识点时间轴' : '暂无知识点时间轴' }}</strong>
            <span>{{ timelineAiGenerating ? '完成后会自动显示在这里。' : '新上传视频会自动识别，也可以手动重新识别。' }}</span>
          </div>
        </a-spin>
      </div>
      </div>
    </a-modal>

    <a-modal
      v-model:open="studentDrawerVisible"
      width="1100px"
      centered
      :footer="null"
      class="student-list-modal teacher-wide-modal"
    >
      <template #title>
        <div class="drawer-custom-title">
          <idcard-outlined class="d-icon" />
          <span>【{{ activeClass?.name }}】- 学生名单</span>
        </div>
      </template>

      <div class="student-list-body">
        <div class="drawer-header-tools">
          <div class="student-search-group">
            <a-input-search
              v-model:value="studentKeyword"
              placeholder="输入学号或姓名搜索..."
              allow-clear
            />
            <div class="student-list-summary">
              共 {{ studentList.length }} 名学生
              <span v-if="studentKeyword.trim()">，筛选出 {{ filteredStudentList.length }} 名</span>
            </div>
          </div>
          <a-button type="primary" ghost @click="handleExportStudents">导出名单</a-button>
        </div>

        <a-table
          :columns="studentColumns"
          :dataSource="filteredStudentList"
          :loading="studentListLoading"
          :pagination="studentPagination"
          :scroll="{ y: 480 }"
          rowKey="id"
          size="middle"
          class="student-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'student'">
              <div class="stu-info-cell">
                <div class="stu-avatar">
                  <a-avatar
                    v-if="record.userAvatar"
                    :src="record.userAvatar"
                    :size="48"
                    class="stu-avatar-img"
                  />
                  <template v-else>
                    <user-outlined />
                  </template>
                </div>
                <div class="stu-text">
                  <span class="stu-name">{{ record.name }}</span>
                  <span class="stu-no">{{ record.studentNo }}</span>
                </div>
              </div>
            </template>

            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click="openTrajectoryModal(record)">
                学习轨迹
              </a-button>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>

    <StudentPortraitModal
      v-model:open="trajectoryVisible"
      :classInfo="activeClass"
      :student="activeStudent"
    />

    <a-modal
      v-if="false"
      v-model:open="trajectoryVisible"
      width="900px"
      :footer="null"
      centered
      class="trajectory-modal"
      destroyOnClose
    >
      <template #title>
        <div class="modal-custom-title trajectory-title">
          <read-outlined class="m-icon" />
          <span>
        {{ activeStudentTrajectory?.studentName || activeStudent?.name || '学生' }} - 学习轨迹
      </span>
        </div>
      </template>

      <div class="trajectory-wrapper">
        <a-spin :spinning="trajectoryLoading">
          <template v-if="activeStudentTrajectory">
            <div class="trajectory-meta-bar">
          <span class="trajectory-chip">
            <strong>班级</strong> {{ activeStudentTrajectory.className || `班级 ${activeStudentTrajectory.classId}` }}
          </span>
              <span class="trajectory-chip">
            <strong>学号</strong> {{ activeStudentTrajectory.studentNo || '--' }}
          </span>
              <span class="trajectory-chip">
            <strong>最近疲劳记录</strong> {{ activeStudentTrajectory.fatigue?.latestRecordDate || '暂无' }}
          </span>
            </div>

            <div class="trajectory-stats-row">
              <div class="trajectory-stat-card primary">
                <div class="ts-label">今日学习时长</div>
                <div class="ts-value">{{ activeStudentTrajectory.summary?.totalStudyDurationText || '0分钟' }}</div>
                <div class="ts-sub">累计 {{ activeStudentTrajectory.summary?.totalStudySeconds || 0 }} 秒</div>
              </div>

              <div class="trajectory-stat-card success">
                <div class="ts-label">已完成作业</div>
                <div class="ts-value">{{ activeStudentTrajectory.summary?.completedHomeworkCount || 0 }}</div>
                <div class="ts-sub">
                  共 {{ activeStudentTrajectory.summary?.totalHomeworkCount || 0 }} 份
                </div>
              </div>

              <div class="trajectory-stat-card warning">
                <div class="ts-label">未完成作业</div>
                <div class="ts-value">{{ activeStudentTrajectory.summary?.unfinishedHomeworkCount || 0 }}</div>
                <div class="ts-sub">
                  {{ activeStudentTrajectory.summary?.hasUnfinishedHomework ? '存在待完成项' : '已全部完成' }}
                </div>
              </div>

              <div class="trajectory-stat-card danger">
                <div class="ts-label">疲劳检测结果</div>
                <div class="ts-value">{{ activeStudentTrajectory.fatigue?.fatigueLevelText || '暂无记录' }}</div>
                <div class="ts-sub">
                  今日状态：{{ activeStudentTrajectory.fatigue?.lastStatusText || '暂无记录' }}
                </div>
              </div>
            </div>

            <div class="trajectory-section">
              <div class="trajectory-section-title">作业完成情况</div>

              <div class="completion-progress-card">
                <div class="completion-meta">
                  <span>完成率</span>
                  <strong>{{ activeStudentTrajectory.summary?.completionRate || 0 }}%</strong>
                </div>
                <div class="completion-track">
                  <div
                    class="completion-fill"
                    :style="{ width: `${activeStudentTrajectory.summary?.completionRate || 0}%` }"
                  ></div>
                </div>

                <div class="completion-split">
                  <span class="done">已完成 {{ activeStudentTrajectory.summary?.completedHomeworkCount || 0 }} 份</span>
                  <span class="todo">未完成 {{ activeStudentTrajectory.summary?.unfinishedHomeworkCount || 0 }} 份</span>
                </div>
              </div>
            </div>

            <div class="trajectory-section">
              <div class="trajectory-section-title">未完成作业</div>

              <div
                v-if="activeStudentTrajectory.unfinishedHomeworkList?.length"
                class="unfinished-list"
              >
                <div
                  v-for="item in activeStudentTrajectory.unfinishedHomeworkList"
                  :key="item.assignmentId"
                  class="unfinished-item"
                >
                  <div class="unfinished-main">
                    <div class="unfinished-title">{{ item.title || '未命名作业' }}</div>
                    <div class="unfinished-meta">
                      <span>题目数：{{ item.questionCount ?? '--' }}</span>
                      <span>总分：{{ item.totalScore ?? '--' }}</span>
                      <span>截止时间：{{ formatDateTimeText(item.deadline) }}</span>
                    </div>
                  </div>
                  <div class="unfinished-badge">未完成</div>
                </div>
              </div>

              <div v-else class="trajectory-empty">
                当前没有未完成作业
              </div>
            </div>

            <div class="trajectory-section">
              <div class="trajectory-section-title">疲劳检测结果</div>

              <div class="fatigue-panel">
                <div class="fatigue-top">
                  <div class="fatigue-level" :class="fatigueLevelClass">
                    {{ activeStudentTrajectory.fatigue?.fatigueLevelText || '暂无记录' }}
                  </div>
                  <div class="fatigue-status-text">
                    今日状态：{{ activeStudentTrajectory.fatigue?.lastStatusText || '暂无记录' }}
                  </div>
                </div>

                <div class="fatigue-grid">
                  <div class="fatigue-cell">
                    <span class="fc-label">疲劳次数</span>
                    <span class="fc-value">{{ activeStudentTrajectory.fatigue?.fatigueCount || 0 }}</span>
                  </div>
                  <div class="fatigue-cell">
                    <span class="fc-label">打哈欠次数</span>
                    <span class="fc-value">{{ activeStudentTrajectory.fatigue?.yawnCount || 0 }}</span>
                  </div>
                  <div class="fatigue-cell">
                    <span class="fc-label">离屏次数</span>
                    <span class="fc-value">{{ activeStudentTrajectory.fatigue?.noFaceCount || 0 }}</span>
                  </div>
                  <div class="fatigue-cell">
                    <span class="fc-label">本次监测时长</span>
                    <span class="fc-value">{{ activeStudentTrajectory.fatigue?.monitorSeconds || 0 }} 秒</span>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <div v-else class="trajectory-empty">
            暂无学习轨迹数据
          </div>
        </a-spin>
      </div>
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { Modal, message } from 'ant-design-vue';
import request from '@/utils/request';
import StudentPortraitModal from './components/StudentPortraitModal.vue';
import {
  fetchChapterSegments,
  saveChapterSegments,
  startChapterTimelineAnalysis,
  fetchLatestChapterTimelineAnalysisTask,
  fetchChapterTimelineAnalysisTask,
  type VideoKnowledgeSegment,
  type VideoTimelineAnalysisTask
} from '@/api/videoLearning';
import {
  PlusOutlined, UploadOutlined, LoadingOutlined, SearchOutlined,
  EditOutlined, DeleteOutlined, UnorderedListOutlined, ReadOutlined,
  BookOutlined, TeamOutlined, BankOutlined, UserOutlined, InboxOutlined,
  VideoCameraOutlined, FileTextOutlined, PlusCircleOutlined,
  PlaySquareOutlined, IdcardOutlined
} from '@ant-design/icons-vue';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const SERVER_BASE_URL = API_BASE_URL.replace(/\/api\/?$/, '')

const normalizeServerAssetUrl = (url?: string) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('data:image')) return url
  return `${SERVER_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`
}

// --- 全局 Tab 状态 ---
const activeTab = ref('course');

// --- 课程主表格相关 ---
const loading = ref(false);
const courseList = ref([]);
const searchText = ref('');
const pagination = reactive({
  current: 1,
  pageSize: 4, // 👉 核心：默认每页显示 5 条
  pageSizeOptions: ['4', '10', '20'], // 👉 顺便让用户在右下角也能选择 5 条/页
  total: 0,
  showSizeChanger: true
});

const columns = [
  { title: '封面', key: 'coverImg', width: 136, align: 'center' },
  { title: '课程名称', dataIndex: 'name', width: 150, align: 'center' },
  { title: '课程简介', dataIndex: 'description', align: 'left' },
  { title: '操作', key: 'action', width: 132, align: 'center' }
];

const modalVisible = ref(false);
const modalType = ref<'add' | 'edit'>('add');
const submitLoading = ref(false);
const formState = reactive({
  id: undefined,
  name: '',
  description: '',
  coverImg: '',
  type: 'video',
  classIds: [] as number[],
  faceDetectionRequired: false
});
const fileList = ref([]);
const uploadLoading = ref(false);

const chapterDrawerVisible = ref(false);
const activeCourse = ref<any>(null);
const chapterList = ref<any[]>([]);
const chapterListLoading = ref(false);
const chapterSubmitLoading = ref(false);
const videoUploadLoading = ref(false);
const chapterVideoList = ref([]);
const chapterForm = reactive({ title: '', sortOrder: 1, videoUrl: '' });
const chapterColumns = [
  { title: '集数', key: 'sortOrder', width: 80 },
  { title: '章节标题', dataIndex: 'title', ellipsis: true },
  { title: '字幕与知识点', key: 'timelineStatus', width: 190 },
  { title: '操作', key: 'action', width: 150, align: 'center' }
];
type SegmentEditorRow = VideoKnowledgeSegment & {
  startText: string
  endText: string
}
type ChapterTimelineStatus = {
  status: 'checking' | 'empty' | 'running' | 'ready' | 'failed'
  label: string
  detail: string
  taskId?: number
}
const activeSegmentChapter = ref<any>(null);
const segmentList = ref<SegmentEditorRow[]>([]);
const segmentLoading = ref(false);
const segmentSaving = ref(false);
const timelineAiGenerating = ref(false);
const timelineAiTaskId = ref<number | null>(null);
const timelineAiStatusText = ref('');
const chapterTimelineStatusMap = ref<Record<number, ChapterTimelineStatus>>({});
let timelineAiPollingTimer: ReturnType<typeof setInterval> | null = null;

const publishModalVisible = ref(false);
const publishSubmitLoading = ref(false);
const publishMetaLoading = ref(false);
const publishCourse = ref<any>(null);

const publishForm = reactive({
  courseId: undefined as number | undefined,
  chapterId: undefined as number | undefined,
  classId: undefined as number | undefined,
  quizResourceId: undefined as number | undefined,
  title: '',
  teacherNote: ''
});

const publishMeta = reactive({
  chapterOptions: [] as Array<{ label: string; value: number }>,
  classOptions: [] as Array<{ label: string; value: number }>,
  quizOptions: [] as Array<{ label: string; value: number }>
});

/**
 * 作业来源过滤逻辑：强制只显示与当前课程相关的 quiz 资源
 *
 * quiz 资源标题约定格式为 "{知识点}-{具体标题}"，例如：
 *   - "数据库-数据库基础习题"
 *   - "python-python基础语法习题"
 *   - "操作系统-操作系统的基本概念习题"
 * 按课程名和 label 做双向模糊匹配：
 *   1) label 里含有课程名        -> 命中（最常见）
 *   2) 课程名里含有知识点前缀    -> 命中（比如课程"数据库原理" 匹配前缀"数据库"）
 *   3) 知识点前缀里含有课程名    -> 命中（容错）
 * 如果匹配不到任何资源，Select 会显示空状态提示老师去资源库生成对应题目，
 * 避免把别科目的习题塞进当前课程。
 */
const filteredQuizOptions = computed(() => {
  const courseName = (publishCourse.value?.name || '').trim().toLowerCase();
  if (!courseName) return publishMeta.quizOptions;

  return publishMeta.quizOptions.filter(opt => {
    const label = (opt.label || '').toLowerCase();
    if (!label) return false;
    // 取第一个分隔符前的知识点前缀（支持 -、—、–、:、：）
    const prefix = label.split(/[-—–:：]/)[0].trim();
    return (
      label.includes(courseName) ||
      (prefix && courseName.includes(prefix)) ||
      (prefix && prefix.includes(courseName))
    );
  });
});

// 若过滤后当前选中的资源已不在列表里，自动清空，避免提交脏数据
watch(filteredQuizOptions, (newOptions) => {
  if (
    publishForm.quizResourceId &&
    !newOptions.find(o => o.value === publishForm.quizResourceId)
  ) {
    publishForm.quizResourceId = undefined;
  }
});

// ================= 🔥 班级与学生管理状态 =================
const classList = ref<Array<{ label: string; value: number }>>([]);// 用于表单下拉框的简写格式
const myClasses = ref<any[]>([]); // 用于班级管理 Tab 的完整数据格式

const studentDrawerVisible = ref(false);
const activeClass = ref<any>(null);
const studentList = ref<any[]>([]);
const studentListLoading = ref(false);

const studentColumns = [
  { title: '学生信息', key: 'student' },
  { title: '学习轨迹', key: 'action', width: 120, align: 'center' }
];

type StudentTrajectoryData = {
  classId: number
  className: string
  studentId: number
  studentNo: string
  studentName: string
  summary: {
    totalStudySeconds: number
    totalStudyDurationText: string
    totalHomeworkCount: number
    completedHomeworkCount: number
    unfinishedHomeworkCount: number
    completionRate: number
    hasUnfinishedHomework: boolean
  }
  fatigue: {
    latestRecordDate: string
    fatigueCount: number
    yawnCount: number
    noFaceCount: number
    normalCount: number
    totalDetections: number
    monitorSeconds: number
    lastStatus: string
    lastStatusText: string
    fatigueLevelText: string
  }
  unfinishedHomeworkList: Array<{
    assignmentId: number
    title: string
    deadline: string
    questionCount: number
    totalScore: number
    assignmentType: string
  }>
}

const studentKeyword = ref('');
const studentCurrentPage = ref(1);
const studentPageSize = ref(7);
const trajectoryVisible = ref(false);
const trajectoryLoading = ref(false);
const activeStudent = ref<any>(null);
const activeStudentTrajectory = ref<any | null>(null);

const filteredStudentList = computed(() => {
  const keyword = studentKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return studentList.value;
  }

  return (studentList.value || []).filter((item: any) => {
    const name = String(item.name || '').toLowerCase();
    const studentNo = String(item.studentNo || '').toLowerCase();
    return name.includes(keyword) || studentNo.includes(keyword);
  });
});

const studentPagination = computed(() => ({
  current: studentCurrentPage.value,
  pageSize: studentPageSize.value,
  total: filteredStudentList.value.length,
  showSizeChanger: true,
  pageSizeOptions: ['7', '10', '20', '50'],
  showTotal: (total: number) => `共 ${total} 名学生`,
  onChange: (page: number, pageSize: number) => {
    studentCurrentPage.value = page;
    studentPageSize.value = pageSize;
  },
  onShowSizeChange: (_current: number, pageSize: number) => {
    studentCurrentPage.value = 1;
    studentPageSize.value = pageSize;
  },
}));

watch(studentKeyword, () => {
  studentCurrentPage.value = 1;
});

const fatigueLevelClass = computed(() => {
  const text = activeStudentTrajectory.value?.fatigue?.fatigueLevelText || '';
  if (text.includes('疲劳较明显')) return 'danger';
  if (text.includes('轻度疲劳') || text.includes('离屏较多')) return 'warn';
  if (text.includes('正常')) return 'ok';
  return 'default';
});

// --- 接口调用逻辑 ---

const fetchClassList = async () => {
  try {
    const data = await request.get<any[], any[]>('/class/my-teaching-classes', {
      skipErrorToast: true
    });

    myClasses.value = data || [];
    classList.value = (data || []).map((item: any) => ({
      label: item.name,
      value: item.id
    }));
  } catch (error) {
    console.error('获取班级失败', error);
  }
};

const openStudentDrawer = (classInfo: any) => {
  activeClass.value = classInfo;
  studentDrawerVisible.value = true;
  studentKeyword.value = '';
  studentCurrentPage.value = 1;
  activeStudent.value = null;
  activeStudentTrajectory.value = null;
  fetchStudents(classInfo.id);
};

// 拉取某班级的学生名单
const fetchStudents = async (classId: number) => {
  studentListLoading.value = true;
  try {
    const data = await request.get<any[], any[]>('/class/students', {
      params: { classId },
      skipErrorToast: true
    });

    studentList.value = data || [];
  } catch (e: any) {
    message.error(e?.message || '获取学生名单失败，请检查网络');
  } finally {
    studentListLoading.value = false;
  }
};

const openTrajectoryModal = async (student: any) => {
  if (!activeClass.value?.id) {
    message.warning('班级信息异常');
    return;
  }

  activeStudent.value = student;
  activeStudentTrajectory.value = null;
  trajectoryVisible.value = true;
};

const handleExportStudents = () => {
  if (!activeClass.value) {
    message.warning('当前班级信息为空');
    return;
  }

  const rows = filteredStudentList.value || [];
  if (!rows.length) {
    message.warning('暂无可导出的学生数据');
    return;
  }

  const header = ['学号', '姓名', '班级'];
  const content = rows.map((item: any) => [
    item.studentNo || '',
    item.name || '',
    activeClass.value?.name || ''
  ]);

  const csv = [header, ...content]
    .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n');

  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);

  link.href = url;
  link.download = `${activeClass.value.name || '学生名单'}.csv`;
  link.click();

  URL.revokeObjectURL(url);
  message.success('学生名单导出成功');
};

const formatDateTimeText = (value?: string) => {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mi = String(date.getMinutes()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
};

// ================= 🔥 原有课程管理 CRUD 完整逻辑 =================

const customRequest = async (options: any, type: 'img' | 'chapterVideo') => {
  const { file, onSuccess, onError } = options;
  const formData = new FormData();
  formData.append('file', file);

  if (type === 'img') uploadLoading.value = true;
  else videoUploadLoading.value = true;

  try {
    const fileUrl = await request.post<string, string>('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      skipErrorToast: true
    });

    message.success('上传成功！');
    if (type === 'img') {
      formState.coverImg = fileUrl;
    } else {
      chapterForm.videoUrl = fileUrl;
    }
    onSuccess?.(fileUrl, file);
  } catch (err: any) {
    message.error(err?.message || '上传失败');
    onError?.(err);
  } finally {
    uploadLoading.value = false;
    videoUploadLoading.value = false;
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const data = await request.get<any, any>('/course/list/page', {
      params: {
        current: pagination.current,
        size: pagination.pageSize,
        name: searchText.value
      },
      skipErrorToast: true
    });

    courseList.value = data?.records || [];
    pagination.total = data?.total || 0;
  } catch (error: any) {
    message.error(error?.message || '加载失败');
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pag: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchData();
};

const openModal = async (type: 'add' | 'edit', record?: any) => {
  modalType.value = type;
  modalVisible.value = true;
  fileList.value = [];

  if (type === 'edit' && record) {
    // 1. 手动赋值，不要用 Object.assign，防止 record 中携带的无效字段污染 Select 组件
    formState.id = record.id;
    formState.name = record.name || '';
    formState.description = record.description || '';
    formState.coverImg = record.coverImg || '';
    formState.type = record.type || 'video';
    formState.faceDetectionRequired = record.faceDetectionRequired === true || record.faceDetectionRequired === 1;
    formState.classIds = []; // 2. 班级 ID 先强行置空，等待下方接口拉取真实数据

    try {
      // 修改点 1：将 URL 换成我们刚在 CourseController 写的 /api/course/classIds
      const data = await request.get<number[], number[]>('/course/classIds', {
        params: { courseId: record.id },
        skipErrorToast: true
      })

      formState.classIds = data || []
    } catch (e) {
      console.error('回显班级失败', e);
    }
  } else {
    // 新增模式清空数据
    formState.id = undefined;
    formState.name = '';
    formState.description = '';
    formState.coverImg = '';
    formState.type = 'video';
    formState.classIds = [];
    formState.faceDetectionRequired = false;
  }
};

const handleSubmit = async () => {
  if (!formState.name) return message.warning('请填写课程名称');
  submitLoading.value = true;
  try {
    const url = modalType.value === 'add' ? '/course/add' : '/course/update';

    await request.post<any, any>(url, formState, {
      skipErrorToast: true
    });

    message.success(modalType.value === 'add' ? '发布成功' : '修改成功');
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '操作失败');
  } finally {
    submitLoading.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await request.post<any, any>('/course/delete', { id }, {
      skipErrorToast: true
    });
    message.success('已删除');
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '删除失败');
  }
};

const resetPublishForm = () => {
  publishForm.courseId = undefined;
  publishForm.chapterId = undefined;
  publishForm.classId = undefined;
  publishForm.quizResourceId = undefined;
  publishForm.title = '';
  publishForm.teacherNote = '';
};


const fetchPublishMeta = async (courseId: number) => {
  publishMetaLoading.value = true;
  try {
    const data = await request.get<any, any>('/homework/teacher/course-practice-meta', {
      params: { courseId },
      skipErrorToast: true
    });

    publishMeta.chapterOptions = (data?.chapterList || []).map((item: any) => ({
      label: item.sortOrder ? `第 ${item.sortOrder} 集 · ${item.chapterTitle}` : item.chapterTitle,
      value: item.chapterId
    }));

    publishMeta.classOptions = (data?.classList || []).map((item: any) => ({
      label: item.className || `班级 ${item.classId}`,
      value: item.classId
    }));

    publishMeta.quizOptions = (data?.quizList || []).map((item: any) => ({
      label: item.title,
      value: item.quizResourceId
    }));
  } catch (error: any) {
    message.error(error?.message || '读取发布元数据失败');
  } finally {
    publishMetaLoading.value = false;
  }
};

const openPublishHomeworkModal = async (record: any) => {
  publishCourse.value = record;
  publishModalVisible.value = true;
  resetPublishForm();
  publishForm.courseId = record.id;

  publishMeta.chapterOptions = [];
  publishMeta.classOptions = [];
  publishMeta.quizOptions = [];

  await fetchPublishMeta(record.id);
};

const handlePublishChapterHomework = async () => {
  if (!publishForm.courseId) return message.warning('课程信息异常');
  if (!publishForm.chapterId) return message.warning('请选择所属章节');
  if (!publishForm.classId) return message.warning('请选择目标班级');
  if (!publishForm.quizResourceId) return message.warning('请选择作业来源');

  publishSubmitLoading.value = true;
  try {
    const payload = {
      courseId: publishForm.courseId,
      chapterId: publishForm.chapterId,
      classId: publishForm.classId,
      quizResourceId: publishForm.quizResourceId,
      title: publishForm.title?.trim() || '',
      teacherNote: publishForm.teacherNote?.trim() || ''
    };

    await request.post<any, any>('/homework/practice/publish', payload, {
      skipErrorToast: true
    });

    message.success('章节练习发布成功');
    publishModalVisible.value = false;
    resetPublishForm();
  } catch (error) {
    message.error('章节练习发布失败');
  } finally {
    publishSubmitLoading.value = false;
  }
};

const defaultTimelineStatus: ChapterTimelineStatus = {
  status: 'checking',
  label: '读取中',
  detail: '正在读取状态'
};

const getChapterTimelineStatus = (chapter?: any): ChapterTimelineStatus => {
  if (!chapter?.id) return defaultTimelineStatus;
  return chapterTimelineStatusMap.value[chapter.id] || defaultTimelineStatus;
};

const setChapterTimelineStatus = (chapterId: number, status: ChapterTimelineStatus) => {
  chapterTimelineStatusMap.value = {
    ...chapterTimelineStatusMap.value,
    [chapterId]: status
  };
};

const timelineStatusFromTask = (task: VideoTimelineAnalysisTask | null): ChapterTimelineStatus => {
  if (!task?.taskId) {
    return { status: 'empty', label: '未生成', detail: '保存视频后自动识别' };
  }
  if (task.status === 'succeeded') {
    return { status: 'ready', label: '已生成', detail: '等待教师确认', taskId: task.taskId };
  }
  if (task.status === 'failed') {
    return { status: 'failed', label: '识别失败', detail: task.errorMessage || '可重新识别', taskId: task.taskId };
  }
  return { status: 'running', label: '识别中', detail: '语音识别与切分中', taskId: task.taskId };
};

const refreshChapterTimelineStatus = async (chapter: any) => {
  if (!chapter?.id) return;
  setChapterTimelineStatus(chapter.id, defaultTimelineStatus);
  try {
    const segments = await fetchChapterSegments(chapter.id);
    if ((segments || []).length > 0) {
      setChapterTimelineStatus(chapter.id, {
        status: 'ready',
        label: `已生成 ${segments.length} 段`,
        detail: '可检查确认'
      });
      return;
    }

    const task = await fetchLatestChapterTimelineAnalysisTask(chapter.id);
    setChapterTimelineStatus(chapter.id, timelineStatusFromTask(task));
  } catch (error) {
    setChapterTimelineStatus(chapter.id, {
      status: 'failed',
      label: '状态异常',
      detail: '稍后刷新'
    });
  }
};

const refreshChapterTimelineStatuses = async (chapters: any[] = chapterList.value) => {
  await Promise.all((chapters || []).map((chapter) => refreshChapterTimelineStatus(chapter)));
};

const chapterRowClassName = (record: any) => {
  return record?.id === activeSegmentChapter.value?.id ? 'chapter-row-active' : '';
};

const openChapterDrawer = (record: any) => {
  activeCourse.value = record;
  chapterDrawerVisible.value = true;
  chapterForm.title = '';
  chapterForm.videoUrl = '';
  chapterForm.sortOrder = (chapterList.value.length || 0) + 1;
  chapterVideoList.value = [];
  activeSegmentChapter.value = null;
  segmentList.value = [];
  chapterTimelineStatusMap.value = {};
  fetchChapters(record.id);
};

const fetchChapters = async (courseId: number) => {
  chapterListLoading.value = true;
  try {
    const data = await request.get<any[], any[]>('/chapter/list', {
      params: { courseId },
      skipErrorToast: true
    });

    chapterList.value = data || [];
    chapterForm.sortOrder = (data || []).length + 1;
    refreshChapterTimelineStatuses(chapterList.value);
  } catch (e: any) {
    message.error(e?.message || '获取目录失败');
  } finally {
    chapterListLoading.value = false;
  }
};

const submitChapter = async () => {
  if (!chapterForm.title) return message.warning('请填写本集标题');
  if (!chapterForm.videoUrl) return message.warning('请先上传视频文件');

  chapterSubmitLoading.value = true;
  try {
    const chapterId = await request.post<any, any>('/chapter/add', {
      courseId: activeCourse.value.id,
      title: chapterForm.title,
      sortOrder: chapterForm.sortOrder,
      videoUrl: chapterForm.videoUrl
    }, {
      skipErrorToast: true
    });

    message.success('选集已保存，系统开始自动识别');
    chapterForm.title = '';
    chapterForm.videoUrl = '';
    chapterVideoList.value = [];
    await fetchChapters(activeCourse.value.id);
    const createdChapter = chapterList.value.find((item: any) => item.id === chapterId);
    if (createdChapter) {
      await openSegmentEditor(createdChapter);
    }
  } catch (e: any) {
    message.error(e?.message || '添加选集失败');
  } finally {
    chapterSubmitLoading.value = false;
  }
};

const deleteChapter = async (id: number) => {
  try {
    await request.post<any, any>('/chapter/delete', { id }, {
      skipErrorToast: true
    });

    message.success('删除成功');
    if (activeSegmentChapter.value?.id === id) {
      activeSegmentChapter.value = null;
      segmentList.value = [];
    }
    const nextStatusMap = { ...chapterTimelineStatusMap.value };
    delete nextStatusMap[id];
    chapterTimelineStatusMap.value = nextStatusMap;
    fetchChapters(activeCourse.value.id);
  } catch (e: any) {
    message.error(e?.message || '删除失败');
  }
};

const mapSegmentsToEditorRows = (segments: VideoKnowledgeSegment[] = []): SegmentEditorRow[] => segments.map((item, index) => ({
  ...item,
  startText: formatTimelineText(item.startSecond),
  endText: formatTimelineText(item.endSecond),
  difficulty: item.difficulty || '中',
  sortOrder: item.sortOrder || index + 1
}));

const reloadChapterSegments = async (chapterId: number) => {
  const data = await fetchChapterSegments(chapterId);
  segmentList.value = mapSegmentsToEditorRows(data || []);
  if (segmentList.value.length > 0) {
    setChapterTimelineStatus(chapterId, {
      status: 'ready',
      label: `已生成 ${segmentList.value.length} 段`,
      detail: '可检查确认'
    });
  }
  return segmentList.value.length;
};

const openSegmentEditor = async (chapter: any) => {
  clearTimelineAiPolling(true);
  activeSegmentChapter.value = chapter;
  segmentLoading.value = true;
  try {
    const segmentCount = await reloadChapterSegments(chapter.id);
    if (segmentCount === 0) {
      await syncLatestTimelineTask(chapter.id);
    } else {
      timelineAiStatusText.value = '已生成知识点时间轴，教师确认后即可用于学生学习提醒。';
    }
  } catch (e: any) {
    message.error(e?.message || '知识点时间轴加载失败');
  } finally {
    segmentLoading.value = false;
  }
};

const addSegmentRow = () => {
  const last = segmentList.value[segmentList.value.length - 1];
  const startSecond = last?.endSecond ?? 0;
  segmentList.value.push({
    startSecond,
    endSecond: startSecond + 60,
    startText: formatTimelineText(startSecond),
    endText: formatTimelineText(startSecond + 60),
    knowledgeName: '',
    description: '',
    difficulty: '中',
    sortOrder: segmentList.value.length + 1
  });
};

const removeSegmentRow = (index: number) => {
  segmentList.value.splice(index, 1);
};

const clearTimelineAiPolling = (resetLoading = false) => {
  if (timelineAiPollingTimer) {
    clearInterval(timelineAiPollingTimer);
    timelineAiPollingTimer = null;
  }
  if (resetLoading) {
    timelineAiGenerating.value = false;
    timelineAiTaskId.value = null;
    timelineAiStatusText.value = '';
  }
};

const applyTimelineDraft = (segments: VideoKnowledgeSegment[]) => {
  segmentList.value = (segments || [])
    .slice()
    .sort((a, b) => (a.startSecond || 0) - (b.startSecond || 0))
    .map((item, index) => ({
      startSecond: item.startSecond,
      endSecond: item.endSecond,
      startText: formatTimelineText(item.startSecond),
      endText: formatTimelineText(item.endSecond),
      knowledgeName: item.knowledgeName || '',
      description: item.description || '',
      difficulty: item.difficulty || '中',
      sortOrder: index + 1
    }));
};

const checkTimelineAiTask = async (taskId: number, reloadFromDb = false) => {
  if (!activeSegmentChapter.value?.id) {
    clearTimelineAiPolling(true);
    return true;
  }

  const chapterId = activeSegmentChapter.value.id;
  try {
    const task = await fetchChapterTimelineAnalysisTask(chapterId, taskId);
    if (task.status === 'succeeded') {
      clearTimelineAiPolling(true);
      if (reloadFromDb) {
        await reloadChapterSegments(chapterId);
        timelineAiStatusText.value = '系统已自动生成并保存时间轴，请检查后确认。';
        message.success('时间轴已自动生成');
      } else {
        applyTimelineDraft(task.segments || []);
        timelineAiStatusText.value = 'AI草稿已生成，请检查后保存。';
        message.success('AI草稿已生成，请检查后保存');
      }
      return true;
    }
    if (task.status === 'failed') {
      clearTimelineAiPolling(true);
      setChapterTimelineStatus(chapterId, timelineStatusFromTask(task));
      timelineAiStatusText.value = task.errorMessage || '自动分析失败，可重新生成或稍后再试。';
      message.error(task.errorMessage || 'AI生成时间轴失败');
      return true;
    }
    timelineAiGenerating.value = true;
    setChapterTimelineStatus(chapterId, timelineStatusFromTask(task));
    timelineAiStatusText.value = '系统正在自动识别视频语音并切分知识点，请稍候。';
    return false;
  } catch (e: any) {
    clearTimelineAiPolling(true);
    message.error(e?.message || '查询AI生成任务失败');
    return true;
  }
};

const syncLatestTimelineTask = async (chapterId: number) => {
  try {
    const task = await fetchLatestChapterTimelineAnalysisTask(chapterId);
    if (!task?.taskId) {
      timelineAiStatusText.value = '暂无时间轴结果。新上传的视频会自动生成，也可手动点击 AI生成时间轴。';
      return;
    }
    if (task.status === 'succeeded') {
      const segmentCount = await reloadChapterSegments(chapterId);
      timelineAiStatusText.value = segmentCount > 0
        ? '系统已自动生成并保存时间轴，请检查后确认。'
        : 'AI任务已完成，但没有可用的时间轴结果，可重新生成。';
      return;
    }
    if (task.status === 'failed') {
      setChapterTimelineStatus(chapterId, timelineStatusFromTask(task));
      timelineAiStatusText.value = task.errorMessage || '自动分析失败，可重新生成或稍后再试。';
      return;
    }

    timelineAiTaskId.value = task.taskId;
    timelineAiGenerating.value = true;
    setChapterTimelineStatus(chapterId, timelineStatusFromTask(task));
    timelineAiStatusText.value = '系统正在自动识别视频语音并切分知识点，请稍候。';
    timelineAiPollingTimer = setInterval(() => {
      if (timelineAiTaskId.value) {
        checkTimelineAiTask(timelineAiTaskId.value, true);
      }
    }, 3000);
  } catch (e: any) {
    timelineAiStatusText.value = '后台任务状态读取失败，可稍后刷新。';
  }
};

const startTimelineDraftGeneration = async () => {
  if (!activeSegmentChapter.value?.id) return;
  clearTimelineAiPolling(false);
  timelineAiGenerating.value = true;
  try {
    const taskId = await startChapterTimelineAnalysis(activeSegmentChapter.value.id);
    timelineAiTaskId.value = taskId;
    setChapterTimelineStatus(activeSegmentChapter.value.id, {
      status: 'running',
      label: '识别中',
      detail: '语音识别与切分中',
      taskId
    });
    const done = await checkTimelineAiTask(taskId);
    if (!done) {
      timelineAiPollingTimer = setInterval(() => {
        if (timelineAiTaskId.value) {
          checkTimelineAiTask(timelineAiTaskId.value);
        }
      }, 3000);
    }
  } catch (e: any) {
    clearTimelineAiPolling(true);
    message.error(e?.message || '启动AI生成时间轴失败');
  }
};

const handleGenerateTimelineDraft = () => {
  if (!activeSegmentChapter.value?.id) return;
  if (segmentList.value.length > 0) {
    Modal.confirm({
      title: 'AI草稿会替换当前编辑区内容',
      content: '这不会覆盖已保存的时间轴，检查后点击“保存时间轴”才会正式生效。',
      okText: '继续生成',
      cancelText: '取消',
      onOk: startTimelineDraftGeneration
    });
    return;
  }
  startTimelineDraftGeneration();
};

const formatTimelineText = (seconds?: number) => {
  const safeSeconds = Math.max(0, Number(seconds || 0));
  const hours = Math.floor(safeSeconds / 3600);
  const minutes = Math.floor((safeSeconds % 3600) / 60);
  const restSeconds = safeSeconds % 60;
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(restSeconds).padStart(2, '0')}`;
  }
  return `${String(minutes).padStart(2, '0')}:${String(restSeconds).padStart(2, '0')}`;
};

const parseTimelineText = (value?: string) => {
  const text = String(value || '').trim();
  if (!text) return null;
  if (/^\d+$/.test(text)) return Number(text);
  const parts = text.split(':').map((part) => part.trim());
  if (parts.length < 2 || parts.length > 3 || parts.some((part) => !/^\d+$/.test(part))) {
    return null;
  }
  const nums = parts.map(Number);
  const seconds = nums.pop() || 0;
  const minutes = nums.pop() || 0;
  const hours = nums.pop() || 0;
  if (seconds >= 60 || minutes >= 60) return null;
  return hours * 3600 + minutes * 60 + seconds;
};

const validateSegments = (): VideoKnowledgeSegment[] | null => {
  const rows = segmentList.value
    .map((item, index) => ({
      ...item,
      startSecond: parseTimelineText(item.startText),
      endSecond: parseTimelineText(item.endText),
      sortOrder: index + 1
    }))
    .sort((a, b) => (a.startSecond || 0) - (b.startSecond || 0));

  for (let i = 0; i < rows.length; i += 1) {
    const item = rows[i];
    if (!item.knowledgeName?.trim()) {
      message.warning(`第 ${i + 1} 条知识点名称不能为空`);
      return null;
    }
    if (
      item.startSecond === null ||
      item.endSecond === null ||
      item.startSecond < 0 ||
      item.endSecond <= item.startSecond
    ) {
      message.warning(`第 ${i + 1} 条时间不合法，请输入 05:20 这样的格式`);
      return null;
    }
    const prevEndSecond = i > 0 ? rows[i - 1].endSecond : null;
    if (prevEndSecond !== null && item.startSecond < prevEndSecond) {
      message.warning('同一章节的知识点时间段不能重叠');
      return null;
    }
  }

  return rows.map((item, index) => {
    const startSecond = item.startSecond ?? 0;
    const endSecond = item.endSecond ?? 0;
    return {
      id: item.id,
      startSecond,
      endSecond,
      knowledgeName: item.knowledgeName.trim(),
      description: item.description || '',
      difficulty: item.difficulty || '中',
      sortOrder: index + 1
    };
  });
};

const saveSegments = async () => {
  if (!activeSegmentChapter.value?.id) return;
  const payload = validateSegments();
  if (!payload) return;

  segmentSaving.value = true;
  try {
    await saveChapterSegments(activeSegmentChapter.value.id, payload);
    message.success('知识点时间轴已保存');
    await openSegmentEditor(activeSegmentChapter.value);
  } catch (e: any) {
    message.error(e?.message || '知识点时间轴保存失败');
  } finally {
    segmentSaving.value = false;
  }
};

onMounted(() => {
  fetchData();
  fetchClassList();
});

onBeforeUnmount(() => {
  clearTimelineAiPolling(true);
});
</script>

<style scoped>
.modern-page {
  font-family: 'Plus Jakarta Sans', sans-serif;
  animation: fadeIn 0.4s ease;
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden !important;
  background: transparent;
}

/* --- Tabs 样式穿透拦截 --- */
:deep(.custom-tabs) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

:deep(.custom-tabs > .ant-tabs-content-holder) {
  flex: 1;
  display: flex;
  min-height: 0;
  position: relative;
  overflow: hidden;
}
:deep(.custom-tabs .ant-tabs-content) {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
}

:deep(.custom-tabs .ant-tabs-tabpane) {
  position: relative;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

/* 班级列表和空状态保持原来的滚动逻辑 */
.class-grid,
.empty-box {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  box-sizing: border-box;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  margin: 0;
}

/* 核心修复：课程资源表格外层容器必须锁死高度，禁止全局滚动 */
.table-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  box-sizing: border-box;
  overflow: hidden !important;
  margin: 0;
}

@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

/* --- 页面头部 --- */
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.title-group h2 { margin: 0; font-size: 28px; font-weight: 800; color: #0f172a; letter-spacing: -0.5px; display: flex; align-items: center; }
.title-icon { color: #4f46e5; margin-right: 10px; font-size: 30px; }
.title-group .subtitle { margin: 6px 0 0; color: #64748b; font-size: 15px; }

.header-actions {
  display: flex;
  gap: 12px;
  position: relative;
  align-items: center;
  justify-content: flex-end;
  flex: 0 0 456px;
  min-height: 42px;
}

.header-actions--hidden {
  visibility: hidden;
  pointer-events: none;
}
.search-box {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  width: 280px;
  transition: 0.3s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);

  /* 👇 以下为修改和新增的代码 👇 */
  height: 42px;          /* 强制高度与按钮(primary-btn)的 42px 保持一致 */
  box-sizing: border-box;/* 确保 1px 的边框被包含在 42px 高度内，防止溢出 */
  padding: 0 16px;       /* 移除原有的 8px 上下 padding，只保留左右内边距 */
}
.search-box:focus-within { border-color: #4f46e5; box-shadow: 0 4px 12px rgba(79, 70, 229, 0.15); transform: translateY(-1px); }
.search-box input { border: none; outline: none; margin-left: 8px; width: 100%; font-size: 14px; }
.search-box .s-icon { color: #94a3b8; }

.primary-btn { background: linear-gradient(135deg, #4f46e5, #6366f1); color: #fff; border: none; border-radius: 5px; /* 修改圆角为 5px */ padding: 0 24px; height: 42px; font-weight: 600; font-size: 14px; cursor: pointer; transition: 0.2s; display: flex; align-items: center; gap: 8px; box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2); }
.primary-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3); }

/* --- Tabs 样式 --- */
:deep(.custom-tabs .ant-tabs-nav::before) { border-bottom-color: #e2e8f0; }

/* --- 容器面板 --- */
.glass-panel { background: #fff; border-radius: 5px; /* 修改圆角为 5px */ padding: 20px; box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.05); border: 1px solid #f1f5f9; }

/* --- 课程表格样式 --- */
:deep(.modern-table .ant-table) { background: transparent; }
:deep(.modern-table .ant-table-thead > tr > th) { background: #f8fafc; color: #64748b; font-weight: 600; font-size: 16px; border-bottom: 1px solid #f1f5f9; padding: 16px; }
:deep(.modern-table .ant-table-tbody > tr > td) {
  border-bottom: 1px solid #f1f5f9;
  padding: 10px 16px; /* 从 16px 缩小到 10px，4行共可省出 48px 空间 */
  transition: background 0.2s;
}
:deep(.modern-table .ant-table-tbody > tr:hover > td) { background: #f8fafc; }
/* 让表格基础结构撑满可用高度 */
:deep(.modern-table table) {
  width: 100% !important;
  min-width: 0 !important;
  table-layout: fixed;
}

:deep(.modern-table .ant-table-cell) {
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

/* 因为每页固定 4 条，所以让每行占据 25% 的高度，实现自动均分和贴底 */
.cover-wrapper {
  width: min(112px, 100%);
  height: 63px;
  margin: 0 auto;
  border-radius: 5px;
  overflow: hidden;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
}
.cover-thumb { width: 100%; height: 100%; object-fit: cover; transition: 0.3s; }
.cover-wrapper:hover .cover-thumb { transform: scale(1.1); }
.course-name {
  font-weight: 700;
  color: #1e293b;
  font-size: 16px;
  line-height: 1.35;
  max-width: 100%;
  overflow-wrap: anywhere;
  word-break: break-word;
}
.desc-text {
  color: #64748b;
  font-size: 14px;
  line-height: 1.55;
  max-width: 100%;
  text-align: left;
  overflow-wrap: anywhere;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.type-tag { padding: 4px 10px; border-radius: 5px; /* 修改圆角为 5px */ font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; }
.tag-icon { margin-right: 4px; font-size: 14px; }
.video-tag { background: #eff6ff; color: #3b82f6; }
.text-tag { background: #f0fdf4; color: #10b981; }
.action-group { display: flex; gap: 6px; justify-content: center; align-items: center; flex-wrap: nowrap; }
.icon-action-btn { width: 32px; height: 32px; border-radius: 5px; /* 修改圆角为 5px */ border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: 0.2s; background: #f8fafc; font-size: 14px; flex: 0 0 32px;}
.icon-action-btn.list { color: #8b5cf6; }
.icon-action-btn.list:hover { background: #ede9fe; transform: translateY(-2px); }
.icon-action-btn.edit { color: #3b82f6; }
.icon-action-btn.edit:hover { background: #eff6ff; transform: translateY(-2px); }
.icon-action-btn.delete { color: #ef4444; }
.icon-action-btn.delete:hover { background: #fef2f2; transform: translateY(-2px); }

/* ================= 新增：班级管理样式 ================= */
.class-grid {
  flex: 1;
  overflow-y: auto;
  align-content: flex-start;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  padding: 4px;
}
.course-name, .desc-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
  word-break: break-all;
  line-height: 1.5;
}
.class-card { padding: 24px; display: flex; flex-direction: column; justify-content: space-between; gap: 24px; transition: 0.3s; cursor: default;}
.class-card:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.06); border-color: #cbd5e1; }
.card-top { display: flex; gap: 16px; align-items: center; }
.class-icon { width: 50px; height: 50px; border-radius: 5px; /* 修改圆角为 5px */ background: #e0e7ff; color: #4f46e5; font-size: 24px; display: flex; justify-content: center; align-items: center; }
.class-info h3 { margin: 0 0 4px 0; font-size: 18px; font-weight: 800; color: #1e293b; }
.class-info .major { font-size: 13px; color: #64748b; background: #f1f5f9; padding: 2px 8px; border-radius: 5px; /* 修改圆角为 5px */ }
.card-bottom { display: flex; justify-content: space-between; align-items: center; border-top: 1px dashed #e2e8f0; padding-top: 16px; }
.student-count { font-weight: 700; color: #3b82f6; font-size: 14px; background: #eff6ff; padding: 4px 10px; border-radius: 5px; /* 修改圆角为 5px */ display: flex; align-items: center; gap: 4px;}
.view-student-btn { font-weight: 600; border-radius: 5px; /* 修改圆角为 5px */ }

/* 学生名单抽屉样式 */
.student-list-body {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.drawer-header-tools {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
  flex-shrink: 0;
}
.student-search-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: min(460px, 100%);
}
.student-search-group :deep(.ant-input),
.student-search-group :deep(.ant-input-search-button) {
  height: 42px;
  font-size: 16px;
}
.student-search-group :deep(.ant-input::placeholder) {
  font-size: 16px;
}
.student-list-summary {
  color: #475569;
  font-size: 16px;
  line-height: 1.4;
  font-weight: 500;
}
.drawer-header-tools :deep(.ant-btn) {
  height: 42px;
  padding: 0 20px;
  font-size: 16px;
  font-weight: 700;
}
.stu-info-cell { display: flex; align-items: center; gap: 14px; }
.stu-avatar {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 24px;
  overflow: hidden;
  flex-shrink: 0;
}
.stu-avatar-img {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  object-fit: cover;
}
.stu-text { display: flex; flex-direction: column; min-width: 0; }
.stu-name { font-size: 17px; font-weight: 800; color: #1e293b; line-height: 1.35; }
.stu-no { font-size: 14px; color: #64748b; font-family: monospace; line-height: 1.35; }
:deep(.student-table) {
  flex: 1;
  min-height: 0;
}
:deep(.student-table .ant-table-thead > tr > th) {
  background: #f8fafc;
  font-weight: 700;
  padding: 13px 14px;
  font-size: 16px;
  color: #0f172a;
}
:deep(.student-table .ant-table-tbody > tr > td) {
  padding: 12px 14px;
  font-size: 16px;
}
:deep(.student-table .ant-btn-link) {
  height: 30px;
  padding: 0 4px;
  font-size: 16px;
  font-weight: 700;
}
:deep(.student-table .ant-table-pagination.ant-pagination) {
  flex-shrink: 0;
  margin: 12px 0 0;
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
  font-size: 16px;
}
:deep(.student-table .ant-pagination-total-text),
:deep(.student-table .ant-pagination-item),
:deep(.student-table .ant-pagination-prev),
:deep(.student-table .ant-pagination-next) {
  font-size: 16px;
}
:deep(.student-table .ant-select-selector) {
  font-size: 16px;
}

:global(.student-list-modal .drawer-custom-title) {
  font-size: 22px;
  font-weight: 800;
}

:global(.student-list-modal .drawer-custom-title .d-icon) {
  font-size: 22px;
}

:global(.student-list-modal .ant-table-wrapper),
:global(.student-list-modal .ant-spin-nested-loading),
:global(.student-list-modal .ant-spin-container) {
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:global(.student-list-modal .ant-table),
:global(.student-list-modal .ant-table-container) {
  min-height: 0;
}

:global(.student-list-modal .ant-table-container) {
  border-radius: 6px;
}

:global(.student-list-modal .ant-table-body) {
  overflow-y: auto !important;
}

/* ================= 修复封面上传组件排版 ================= */
.avatar-uploader {
  display: block;
}

/* 1. 外层容器固定尺寸，并加上 relative 定位 */
:deep(.avatar-uploader .ant-upload.ant-upload-select-picture-card) {
  width: 240px !important;
  height: 135px !important;
  border-radius: 5px;
  overflow: hidden;
  padding: 0 !important;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  transition: all 0.3s;

  display: flex !important;
  justify-content: center;
  align-items: center;
  position: relative !important; /* 关键点：作为内部图片的定位基准 */
}

:deep(.avatar-uploader .ant-upload:hover) {
  border-color: #4f46e5;
  background: #f1f5f9;
}

/* 2. 图片绝对定位，无视内部嵌套，强行撑满 100% */
.uploaded-img {
  position: absolute !important; /* 关键点：脱离常规文档流 */
  top: 0;
  left: 0;
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;  /* 关键点：铺满整个 16:9 区域，多余部分自动裁剪 */
  display: block;
}

/* 3. 提示文字区域 */
.upload-placeholder {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: #64748b;
}

/* 课程类型单选按钮组撑满宽度，两边对齐更整齐 */
.full-width-radio {
  display: flex;
  width: 100%;
  height: 40px;
}
:deep(.full-width-radio .ant-radio-button-wrapper) {
  flex: 1;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ================= 优化多选框的标签样式 ================= */
:deep(.ant-select-multiple .ant-select-selection-item) {
  background: #eff6ff;        /* 淡蓝色背景 */
  border: 1px solid #bfdbfe;  /* 浅蓝色边框 */
  color: #2563eb;             /* 深蓝色文字 */
  border-radius: 5px;         /* 统一圆角 */
  font-weight: 500;
  padding: 0 8px;
}

:deep(.ant-select-multiple .ant-select-selection-item-remove) {
  color: #60a5fa;             /* 叉号颜色 */
  margin-left: 4px;
}

:deep(.ant-select-multiple .ant-select-selection-item-remove:hover) {
  color: #1d4ed8;             /* 鼠标悬停时叉号变深 */
}

/* 确保下拉框的高度在选中多个标签时能自然撑开，而不是挤成一团 */
:deep(.ant-select-selector) {
  padding-top: 2px !important;
  padding-bottom: 2px !important;
}

/* 定制 Modal / Drawer 标题 */
.modal-custom-title, .drawer-custom-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 700; color: #1e293b; }
.m-icon { font-size: 18px; }
.m-icon.add-icon { color: #10b981; }
.m-icon.edit-icon { color: #3b82f6; }
.d-icon { font-size: 18px; color: #8b5cf6; }

.box-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 700; color: #1e293b; margin-bottom: 16px;}

.empty-box { text-align: center; padding: 80px 0; }
.empty-icon { font-size: 64px; margin-bottom: 16px; color: #cbd5e1; }
.empty-box h3 { font-size: 18px; color: #1e293b; margin-bottom: 8px; }
.empty-box p { color: #64748b; margin-bottom: 24px; }

/* 覆盖 Ant Design Vue 的组件全局圆角 (当前文件内生效) */
:deep(.ant-input),
:deep(.ant-input-number),
:deep(.ant-select-selector),
:deep(.ant-btn),
:deep(.ant-tag) {
  border-radius: 5px !important;
}

/* 如果使用 radio button 组，覆盖两端的圆角 */
:deep(.ant-radio-button-wrapper:first-child) {
  border-start-start-radius: 5px;
  border-end-start-radius: 5px;
}
:deep(.ant-radio-button-wrapper:last-child) {
  border-start-end-radius: 5px;
  border-end-end-radius: 5px;
}

.icon-action-btn.homework {
  color: #10b981;
}
.icon-action-btn.homework:hover {
  background: #ecfdf5;
  transform: translateY(-2px);
}

.publish-form-tip {
  margin-top: 4px;
  padding: 12px 14px;
  border-radius: 5px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.form-row {
  display: flex;
  gap: 16px;
}

.half-width {
  flex: 1;
}

/* ================= 固定分页器在卡片右下角 (Flex 终极方案) ================= */
/* 1. 让最外层卡片变成 Flex 列布局 */
.table-container.glass-panel {
  display: block;
  padding-bottom: 20px !important;
}

/* 2. 让表格外壳撑满卡片的剩余高度 */
.modern-table {
  display: block;
}

/* 3. 强行打通 Ant Design 内部嵌套的层层 div，让它们都能继承高度并作为 Flex 容器 */
:deep(.modern-table .ant-table-wrapper),
:deep(.modern-table .ant-spin-nested-loading),
:deep(.modern-table .ant-spin-container) {
  height: auto;
  display: block;
}

/* 4. 强行打通内部高度，让表格数据区域接管滚动 */
:deep(.modern-table .ant-table) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0; /* 必须加：打破 flex 默认的无限撑开 */
}

:deep(.modern-table .ant-table-container) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

:deep(.modern-table .ant-table-content) {
  flex: 1;
  overflow-x: hidden !important;
  overflow-y: auto !important;
}

/* 5. 分页器：固定在容器绝对底部 */
:deep(.modern-table .ant-table-pagination.ant-pagination) {
  margin-top: auto !important;
  margin-bottom: 0 !important;
  padding: 16px 0;
  background: #fff;
  border-top: 1px dashed #e2e8f0;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0; /* 必须加：防止排版空间不够时分页器被压扁 */
}

/* ================= 学习轨迹弹窗样式优化 (极简明亮风格) ================= */
/* Keep the teacher course list at its natural density. One page shows 4 rows,
   but rows should not stretch to fill the whole panel. */
.table-container.glass-panel {
  display: block !important;
  position: relative !important;
  height: 100%;
  padding-bottom: 84px !important;
}

.modern-table,
:deep(.modern-table .ant-table-wrapper),
:deep(.modern-table .ant-spin-nested-loading),
:deep(.modern-table .ant-spin-container),
:deep(.modern-table .ant-table),
:deep(.modern-table .ant-table-container) {
  height: auto !important;
  min-height: 0 !important;
  display: block !important;
  flex: none !important;
  position: static !important;
}

:deep(.modern-table table) {
  height: auto !important;
}

:deep(.modern-table .ant-table-tbody > tr) {
  height: auto !important;
}

:deep(.modern-table .ant-table-content) {
  flex: none !important;
  overflow-x: hidden !important;
  overflow-y: visible !important;
}

:deep(.modern-table .ant-table-pagination.ant-pagination) {
  position: absolute;
  right: 20px;
  bottom: 20px;
  margin: 0 !important;
  padding: 0;
  border-top: 0;
  flex-shrink: 0;
  background: #fff;
  z-index: 2;
}

.chapter-manage-modal :deep(.ant-modal-content),
.trajectory-modal :deep(.ant-modal-content) {
  border-radius: 5px;
  overflow: hidden;
}

.chapter-manage-modal :deep(.ant-modal-body) {
  padding-top: 12px;
}

.chapter-modal-body {
  max-height: calc(100vh - 190px);
  overflow-y: auto;
  overflow-x: hidden;
  padding: 2px 4px 8px 0;
}

.chapter-ai-overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #f8fbff;
}

.chapter-ai-overview h3 {
  margin: 0;
  color: #172554;
  font-size: 15px;
  font-weight: 700;
}

.chapter-ai-overview p {
  margin: 5px 0 0;
  max-width: 58ch;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.chapter-ai-steps {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.chapter-ai-steps span {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 9px;
  border-radius: 5px;
  background: #ffffff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

.chapter-add-tip {
  margin: -2px 0 12px;
  color: #64748b;
  font-size: 12px;
}

.course-monitor-setting {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
}

.course-monitor-setting strong {
  display: block;
  color: #1e293b;
  font-size: 14px;
}

.course-monitor-setting p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.chapter-table :deep(.chapter-row-active > td) {
  background: #f8fbff !important;
}

.timeline-status-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.timeline-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
  padding: 2px 8px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.timeline-status-detail {
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.9;
}

.timeline-status-pill.is-checking {
  color: #64748b;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.timeline-status-pill.is-empty {
  color: #475569;
  background: #f8fafc;
  border-color: #cbd5e1;
}

.timeline-status-pill.is-running {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.timeline-status-pill.is-ready {
  color: #15803d;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.timeline-status-pill.is-failed {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.trajectory-wrapper {
  max-height: 72vh;
  overflow-y: auto;
  padding: 4px 2px 8px;
}

.trajectory-title .m-icon {
  color: #4f46e5;
}

.trajectory-meta-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.trajectory-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  color: #475569;
  font-size: 13px;
}

.trajectory-stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

/* 数据卡片扁平化，去除渐变和阴影，通过顶部边框区分状态 */
.trajectory-stat-card {
  border-radius: 5px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  position: relative;
}

.trajectory-stat-card.primary {
  border-top: 3px solid #4f46e5;
  background: #f8fafc;
}

.trajectory-stat-card.success {
  border-top: 3px solid #10b981;
  background: #f8fafc;
}

.trajectory-stat-card.warning {
  border-top: 3px solid #f59e0b;
  background: #f8fafc;
}

.trajectory-stat-card.danger {
  border-top: 3px solid #ef4444;
  background: #f8fafc;
}

.ts-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

.ts-value {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
  margin-bottom: 6px;
}

.ts-sub {
  font-size: 12px;
  color: #94a3b8;
}

.trajectory-section {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  padding: 18px;
  margin-bottom: 16px;
}

.trajectory-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #e2e8f0;
}

.completion-progress-card {
  padding: 4px 0;
}

.completion-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: #334155;
}

.completion-meta strong {
  font-size: 18px;
  color: #4f46e5;
}

/* 进度条统一 5px 圆角，去除渐变 */
.completion-track {
  height: 10px;
  background: #f1f5f9;
  border-radius: 5px;
  overflow: hidden;
}

.completion-fill {
  height: 100%;
  background: #4f46e5;
  border-radius: 5px;
  transition: width 0.3s ease;
}

.completion-split {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.completion-split .done {
  color: #10b981;
  font-weight: 500;
}

.completion-split .todo {
  color: #f59e0b;
  font-weight: 500;
}

.unfinished-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.unfinished-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 5px;
  background: #fafafa;
  border: 1px solid #e2e8f0;
}

.unfinished-main {
  flex: 1;
  min-width: 0;
}

.unfinished-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
}

.unfinished-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 12px;
  color: #64748b;
}

/* 状态徽标统一 5px，加入淡边框增强边界感 */
.unfinished-badge {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 5px;
  background: #fef2f2;
  color: #ef4444;
  border: 1px solid #fecaca;
  font-size: 12px;
  font-weight: 500;
}

.fatigue-panel {
  background: #fafafa;
  border-radius: 5px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.fatigue-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.fatigue-level {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 5px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid transparent;
}

.fatigue-level.ok { background: #f0fdf4; color: #15803d; border-color: #bbf7d0; }
.fatigue-level.warn { background: #fffbeb; color: #b45309; border-color: #fde68a; }
.fatigue-level.danger { background: #fef2f2; color: #dc2626; border-color: #fecaca; }
.fatigue-level.default { background: #f8fafc; color: #475569; border-color: #e2e8f0; }

.fatigue-status-text {
  font-size: 13px;
  color: #64748b;
}

.fatigue-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.fatigue-cell {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.fc-label {
  font-size: 12px;
  color: #64748b;
}

.fc-value {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.trajectory-empty {
  text-align: center;
  color: #94a3b8;
  padding: 40px 0;
  font-size: 14px;
}

.segment-editor {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #fbfdff;
}

.segment-editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.segment-editor-head h3 {
  margin: 0;
  font-size: 15px;
  color: #1e293b;
}

.segment-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.segment-editor-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.segment-task-status {
  max-width: 480px;
  color: #2563eb !important;
  line-height: 1.5;
}

.segment-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.segment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.segment-card {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #ffffff;
}

.segment-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.segment-index {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border-radius: 5px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.segment-form-grid {
  display: grid;
  grid-template-columns: 112px 112px minmax(180px, 1fr) 96px;
  gap: 12px;
  align-items: start;
}

.segment-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.segment-field > span {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.segment-field :deep(.ant-input),
.segment-field :deep(.ant-select),
.segment-field :deep(.ant-input-number),
.segment-field :deep(.ant-input-affix-wrapper) {
  width: 100%;
}

.desc-field {
  grid-column: 1 / -1;
}

.time-field :deep(.ant-input) {
  font-variant-numeric: tabular-nums;
}

.segment-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px 20px;
  text-align: center;
  color: #64748b;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  background: #fff;
}

.segment-empty strong {
  color: #334155;
  font-size: 14px;
}

.segment-empty span {
  font-size: 12px;
}

@media (max-width: 1100px) {
  .chapter-ai-overview,
  .segment-editor-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .chapter-ai-steps {
    flex-wrap: wrap;
  }

  .trajectory-stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .fatigue-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .segment-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .name-field,
  .difficulty-field,
  .desc-field {
    grid-column: 1 / -1;
  }
}
</style>
