<template>
  <div class="bilibili-layout-container scroll-y">
    <div class="content-wrapper">

      <div class="page-top-header">
        <h1 class="course-title" :title="displayCourseTitle">
          {{ displayCourseTitle || '\u00A0' }}
        </h1>
        <button class="btn-collect-course" @click="toggleFavour" :class="{ active: isFavour }">
          <star-filled v-if="isFavour" class="icon" />
          <star-outlined v-else class="icon" />
          <span>{{ isFavour ? '已收藏' : '收藏课程' }}</span>
        </button>
      </div>

      <div class="top-section">
        <div class="video-column">
          <div class="video-player-box">
            <iframe
              v-if="currentVideoEmbedUrl"
              :key="currentVideoEmbedUrl"
              class="main-video"
              :src="currentVideoEmbedUrl"
              allow="autoplay; fullscreen; picture-in-picture"
              sandbox="allow-scripts allow-same-origin allow-presentation"
              allowfullscreen
              frameborder="0"
            ></iframe>
            <video
              v-else-if="currentVideoUrl"
              ref="mainVideoRef"
              :key="currentVideoUrl"
              controls
              autoplay
              class="main-video"
              :src="currentVideoUrl"
              @play="handleVideoPlay"
              @pause="handleVideoPause"
              @seeking="handleVideoSeeking"
              @seeked="handleVideoSeeked"
              @ratechange="handleVideoRateChange"
              @timeupdate="handleVideoTimeUpdate"
              @ended="handleVideoEnded"
            ></video>
            <div v-else class="loading-box">
              <loading-outlined v-if="loadingChapters" class="spinner" />
              <div v-if="!loadingChapters && chapterList.length === 0" class="empty-tip">
                <inbox-outlined class="empty-icon" /> 老师暂未发布任何视频选集
              </div>
              <div v-else-if="loadingChapters" class="loading-tip">正在获取视频流...</div>
            </div>
          </div>
        </div>

        <div class="sidebar-column">
          <div class="fatigue-card shadow-card">
            <div class="monitor-header">
              <div class="monitor-title-group">
                <span class="m-title"><dashboard-outlined style="color: #4f46e5; margin-right: 6px;" /> 学习状态监测</span>
              </div>
              <div class="monitor-actions">
                <span class="monitor-policy" :class="{ required: courseRequiresFaceDetection }">
                  {{ courseRequiresFaceDetection ? '教师要求开启' : '本课程未强制' }}
                </span>
                <a-button v-if="!isCameraOn" type="primary" size="small" shape="round" ghost @click="startCamera">
                  {{ courseRequiresFaceDetection ? '开启检测' : '自愿开启' }}
                </a-button>
                <a-button v-else danger size="small" shape="round" @click="stopCamera">
                  <close-outlined />
                  关闭检测
                </a-button>
              </div>
            </div>
            <div v-if="!isCameraOn" class="monitor-hint">
              {{ courseRequiresFaceDetection ? '本课程要求记录学习状态，请授权摄像头后继续学习。' : '你可以自愿开启学习状态监测，数据用于自己的学习画像。' }}
            </div>
            <div class="monitor-body" v-if="isCameraOn">
              <div class="camera-mini">
                <video ref="cameraVideo" autoplay playsinline muted class="camera-feed"></video>
                <canvas ref="canvas" style="display: none;"></canvas>
              </div>
              <div class="status-info">
                <div class="indicator">
                  <span class="dot pulse" :class="{ red: isFatigue, green: !isFatigue }"></span>
                  <span class="s-text" :class="{ warning: isFatigue }">{{ statusText }}</span>
                </div>
                <div class="fatigue-counters">
                  <span class="counter-tag tag-blue" title="摄像头累计检测时长">{{ formatMonitorTime() }}</span>
                  <span class="counter-tag tag-warn" title="打哈欠次数">哈欠 {{ fatigueStats.yawnCount }}</span>
                  <span class="counter-tag tag-danger" title="闭眼疲劳次数">闭眼 {{ fatigueStats.fatigueCount }}</span>
                  <span class="counter-tag tag-muted" title="离开屏幕次数">离屏 {{ fatigueStats.noFaceCount }}</span>
                  <span class="counter-tag tag-ok" title="专注率">专注 {{ focusRate() }}%</span>
                </div>
              </div>
            </div>
          </div>

          <div class="interaction-card shadow-card">
            <div class="custom-tabs-header">
              <div class="tab-item" :class="{ active: topTab === 'chapters' }" @click="topTab = 'chapters'">
                <bars-outlined style="margin-right: 4px;" /> 视频选集 <span class="badge">{{ chapterList.length }}</span>
              </div>
              <div class="tab-item" :class="{ active: topTab === 'ai' }" @click="topTab = 'ai'">
                <robot-outlined style="margin-right: 4px;" /> AI 伴学
              </div>
            </div>

            <div class="custom-tabs-content">
              <div v-show="topTab === 'chapters'" class="chapter-list scroll-y">
                <div
                  v-for="(chapter, index) in chapterList"
                  :key="chapter.id"
                  class="ep-item"
                  :class="{ playing: currentChapter?.id === chapter.id }"
                  @click="selectChapter(chapter)"
                >
                  <div class="ep-left">
                    <span class="ep-num">P{{ index + 1 }}</span>
                    <span class="ep-name">{{ chapter.title }}</span>
                  </div>
                  <div class="ep-right" v-if="currentChapter?.id === chapter.id">
                    <div class="playing-anim"><i></i><i></i><i></i></div>
                  </div>
                </div>
              </div>

              <div v-show="topTab === 'ai'" class="ai-workspace">
                <div class="ai-companion-switch">
                  <div>
                    <strong>AI伴学</strong>
                    <p v-if="aiCompanionEnabled">开启后，会根据暂停、回看等行为主动提供解释。</p>
                  </div>
                  <div class="ai-companion-controls">
                    <label>
                      <span>行为提醒</span>
                      <a-switch v-model:checked="aiCompanionEnabled" size="small" @change="handleAiCompanionToggle" />
                    </label>
                    <label>
                      <span>语音播报</span>
                      <a-switch
                        :checked="aiSpeechEnabled"
                        size="small"
                        :disabled="!speechSupported"
                        @change="handleAiSpeechToggle"
                      />
                    </label>
                  </div>
                </div>
                <div class="message-list scroll-y" ref="messageListRef">
                  <div
                    v-for="(msg, index) in chatHistory"
                    :key="index"
                    :class="['message-wrapper', msg.role === 'user' ? 'my-msg' : 'ai-msg']"
                  >
                    <div class="avatar" :class="msg.role === 'user' ? 'user-avatar' : 'ai-avatar'">
                      <user-outlined v-if="msg.role === 'user'" />
                      <span v-else class="assistant-avatar-mark">AI</span>
                    </div>
                    <div class="bubble">
                      <div v-if="msg.role === 'ai'" v-html="renderMarkdown(stripAnswerSectionFromMarkdown(msg.content))" class="markdown-body"></div>
                      <div v-else>{{ msg.content }}</div>
                      <button
                        v-if="msg.role === 'ai' && speechSupported && msg.content && msg.content !== '...'"
                        class="speech-msg-btn"
                        :class="{ active: speakingMessageIndex === index }"
                        type="button"
                        :title="speakingMessageIndex === index ? '停止播报' : '播报这条回复'"
                        @click="toggleSpeakMessage(index, msg.content)"
                      >
                        <pause-circle-outlined v-if="speakingMessageIndex === index" />
                        <sound-outlined v-else />
                      </button>
                    </div>
                  </div>
                  <div v-if="pauseHelpPrompt" class="message-wrapper ai-msg">
                    <div class="avatar ai-avatar"><span class="assistant-avatar-mark">AI</span></div>
                    <div class="bubble pause-help-card">
                      <div class="pause-help-title">检测到你在此暂停过久，请问是否需要帮助？</div>
                      <div class="pause-help-meta">
                        {{ pauseHelpPrompt.segment.knowledgeName }} · {{ formatSegmentTime(pauseHelpPrompt.segment) }}
                      </div>
                      <div class="pause-help-actions">
                        <a-button type="primary" size="small" :loading="aiLoading" @click="acceptPauseHelp">是，需要帮助</a-button>
                        <a-button size="small" @click="dismissPauseHelp">否，暂不需要</a-button>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="input-area">
                  <a-textarea
                    v-model:value="questionText"
                    placeholder="遇到不懂的概念？随时问我..."
                    :auto-size="{ minRows: 1, maxRows: 3 }"
                    @pressEnter.prevent="sendQuestion"
                    class="chat-input"
                  />
                  <a-button type="primary" class="send-btn" :loading="aiLoading" @click="sendQuestion" :disabled="!questionText.trim()">
                    <send-outlined />
                  </a-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="bottom-section shadow-card">
        <a-tabs v-model:activeKey="bottomTab" size="large">

          <a-tab-pane key="mindmap">
            <template #tab>
              <span><apartment-outlined /> 思维导图</span>
            </template>

            <CourseMindmapPanel
              :loading="mindmapLoading"
              :regenerating="mindmapRegenerating"
              :error="mindmapError"
              :data="mindmapData"
              @retry="handleMindmapRetry"
              @regenerate="handleMindmapRegenerate"
            />
          </a-tab-pane>

          <a-tab-pane key="anim">
            <template #tab>
              <span><play-square-outlined /> 交互课件</span>
            </template>
            <div class="anim-container">
              <div class="anim-tip">
                <span class="tag">教师发布</span> 本节课件包含交互式推演组件，请在下方手动推演以加深理解。
              </div>
              <div class="anim-iframe-box">
                <!-- 新版：JSON 模板 → 用 AnimationWorkbench 渲染 -->
                <AnimationWorkbench
                  v-if="animPayload"
                  :payload="animPayload"
                  render-status="ready"
                  :validation-errors="[]"
                  :is-generating="false"
                  :is-optimizing="false"
                  :preview-mode="true"
                />

                <!-- 旧版兼容：纯 HTML → 用 iframe 渲染 -->
                <iframe
                  v-else-if="animIsHtml"
                  :srcdoc="currentChapter.animHtml"
                  frameborder="0"
                  width="100%"
                  height="100%"
                  sandbox="allow-scripts allow-same-origin"
                />

                <!-- 空状态 -->
                <div v-else class="empty-anim">
                  <desktop-outlined class="huge-icon" />
                  <p>该章节暂无交互式课件</p>
                </div>
              </div>
            </div>
          </a-tab-pane>

          <a-tab-pane key="note">
            <template #tab>
              <span><edit-outlined /> 随堂笔记</span>
            </template>
            <div class="note-container">
              <button class="btn-new-note" @click="startNewNote">+ 新增笔记</button>

              <div v-if="noteEditing" class="note-edit-card">
                <div style="border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; background: #fff;">
                  <Toolbar
                    style="border-bottom: 1px solid #e5e7eb"
                    :editor="editorRef"
                    :defaultConfig="toolbarConfig"
                    mode="default"
                  />
                  <Editor
                    style="height: 350px; overflow-y: hidden;"
                    v-model="noteContent"
                    :defaultConfig="editorConfig"
                    mode="default"
                    @onCreated="handleCreated"
                  />
                </div>
                <div class="note-edit-actions">
                  <button class="btn-note-cancel" @click="cancelEdit">取消</button>
                  <button class="btn-note-save" @click="saveNote" :disabled="!editorHasContent">保存</button>
                </div>
              </div>

              <div v-if="savedNotesList.length > 0" class="note-list">
                <div v-for="(n, i) in savedNotesList" :key="i" class="note-card">
                  <div class="note-card-header">
                    <h4>{{ n.title || '随堂笔记' }}</h4>
                    <span class="note-card-time">{{ formatTime(n.savedAt) }}</span>
                  </div>
                  <div class="note-card-body" v-html="renderMarkdown(n.content)"></div>
                  <div class="note-card-footer">
                    <button class="btn-note-edit" @click="editNote(i)">编辑</button>
                    <button class="btn-note-delete" @click="deleteNote(i)">删除</button>
                  </div>
                </div>
              </div>

              <div v-else-if="!noteEditing" class="note-empty">
                <edit-outlined style="font-size: 32px; color: #ccc; margin-bottom: 10px;" />
                <p>暂无笔记，点击上方按钮开始记录</p>
              </div>
            </div>
          </a-tab-pane>



          <a-tab-pane key="discuss">
            <template #tab>
              <span><comment-outlined /> 观点分享</span>
            </template>

            <div class="discuss-container">
              <div class="discuss-publish">
                <a-avatar :src="displayAvatarUrl" :size="40" class="publish-avatar" />
                <div class="publish-input-box">
                  <a-textarea
                    v-model:value="newDiscussContent"
                    placeholder="这节课听得过瘾吗？写下你的感悟、疑问或神吐槽..."
                    :auto-size="{ minRows: 2, maxRows: 4 }"
                    class="discuss-textarea"
                  />
                  <div class="publish-actions">
                    <span class="publish-tip">支持 Markdown 语法，请文明发言</span>
                    <button class="btn-publish" :disabled="!newDiscussContent.trim()" @click="publishDiscuss">
                      发表观点
                    </button>
                  </div>
                </div>
              </div>

              <div class="discuss-divider"></div>

              <div class="discuss-list-section">
                <h3 class="discuss-title">全部观点 ({{ discussList.length }})</h3>

                <div v-if="discussList.length > 0" class="discuss-list">
                  <div v-for="(discuss, index) in discussList" :key="discuss.id" class="discuss-item">
                    <a-avatar :src="discuss.userAvatar" :size="32" class="item-avatar" />
                    <div class="item-main">
                      <div class="item-header">
                        <span class="item-author">{{ discuss.userName }}</span>
                        <span class="item-time">{{ formatTime(discuss.createTime) }}</span>
                      </div>

                      <div class="item-content markdown-body" v-html="renderMarkdown(discuss.content)"></div>

                      <div class="item-footer">
                        <span class="action-btn" :class="{ 'liked': discuss.isLiked }" @click="toggleDiscussLike(index)">
                          <like-filled v-if="discuss.isLiked" />
                          <like-outlined v-else />
                          {{ discuss.likes > 0 ? discuss.likes : '点赞' }}
                        </span>
                        <span v-if="discuss.userId === getCurrentUserId()" class="action-btn delete-btn" @click="deleteDiscuss(index)">
                          删除
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-else class="discuss-empty">
                  <comment-outlined class="huge-icon" style="margin-bottom: 16px; color: #cbd5e1; font-size: 48px;" />
                  <p>还没有人发表观点，快来抢个沙发吧！🛋️</p>
                </div>
              </div>
            </div>
          </a-tab-pane>

          <a-tab-pane v-if="isClassBoundCourse" key="homework">
            <template #tab>
              <span><form-outlined /> 章节练习</span>
            </template>

            <div class="chapter-homework-panel">
              <div v-if="chapterHomeworkLoading" class="placeholder-box empty-anim chapter-homework-empty-state">
                <loading-outlined class="huge-icon" />
                <p>正在读取本章节练习...</p>
              </div>

              <div v-else-if="!currentChapter" class="placeholder-box empty-anim chapter-homework-empty-state">
                <form-outlined class="huge-icon" />
                <p>请先选择章节</p>
              </div>

              <div v-else-if="!chapterHomework" class="placeholder-box empty-anim chapter-homework-empty-state">
                <form-outlined class="huge-icon" />
                <p>本章节暂未发布随堂练习</p>
              </div>

              <template v-else>
                <div class="chapter-homework-card">
                  <div class="chapter-homework-head">
                    <div class="chapter-homework-title-wrap">
                      <h3 class="chapter-homework-title">{{ chapterHomework.title }}</h3>
                      <p class="chapter-homework-subtitle">
                        当前章节：{{ chapterHomework.chapterTitle || currentChapter?.title || '未命名章节' }}
                      </p>
                    </div>

                    <span class="chapter-homework-status" :class="homeworkUiState">
            {{ homeworkUiStateText }}
          </span>
                  </div>

                  <div class="chapter-homework-grid">
                    <div class="chapter-homework-meta">
                      <span class="meta-label">练习形式</span>
                      <strong>随学随练</strong>
                    </div>

                    <div class="chapter-homework-meta">
            <span class="meta-label">
              <eye-outlined style="margin-right: 6px;" />
              题目数量
            </span>
                      <strong>{{ chapterHomework.questionCount ?? '--' }} 题</strong>
                    </div>

                    <div class="chapter-homework-meta">
                      <span class="meta-label">总分</span>
                      <strong>{{ chapterHomework.totalScore ?? '--' }} 分</strong>
                    </div>

                    <div class="chapter-homework-meta">
                      <span class="meta-label">练习状态</span>
                      <strong>{{ homeworkUiStateText }}</strong>
                    </div>
                  </div>

                  <div v-if="chapterHomework.teacherNote" class="chapter-homework-note">
                    <div class="note-tag">教师寄语</div>
                    <div class="note-text">{{ chapterHomework.teacherNote }}</div>
                  </div>

                  <div v-if="homeworkUiState === 'completed'" class="chapter-homework-score-box">
                    本次得分：
                    <span class="score">{{ chapterHomework.latestScore ?? '--' }}</span>
                    分
                  </div>

                  <div class="chapter-homework-actions">
                    <button
                      v-if="homeworkUiState === 'todo'"
                      class="homework-primary-btn"
                      @click="openInlineHomeworkPractice"
                    >
                      {{ chapterHomework.attemptCount > 0 ? '再次练习' : '开始练习' }}
                    </button>

                    <button
                      v-else-if="homeworkUiState === 'judging'"
                      class="homework-primary-btn ghost"
                      disabled
                    >
                      等待教师批改
                    </button>

                    <template v-else-if="homeworkUiState === 'completed'">
                      <button
                        class="homework-primary-btn ghost"
                        @click="router.push('/student/profile?tab=homework')"
                      >
                        查看作业记录
                      </button>

                      <button
                        class="homework-primary-btn"
                        @click="openInlineHomeworkPractice"
                      >
                        再次练习
                      </button>
                    </template>
                  </div>
                </div>

                <!-- 页内做题区 -->
                <div v-if="homeworkPanelMode === 'practice'" class="inline-practice-panel">
                  <div class="inline-practice-header">
                    <div>
                      <h3>章节练习</h3>
                      <p>直接在当前页面完成练习，提交后由教师批改</p>
                    </div>
                    <div class="inline-practice-header-actions">
                      <button class="homework-primary-btn ghost" @click="backToHomeworkOverview">
                        返回概览
                      </button>
                      <button
                        class="homework-primary-btn"
                        :disabled="inlineHomeworkSubmitting || inlineHomeworkLoading || inlinePracticeQuestions.length === 0"
                        @click="submitInlinePractice"
                      >
                        {{ inlineHomeworkSubmitting ? '提交中...' : '提交练习' }}
                      </button>
                    </div>
                  </div>

                  <div v-if="inlineHomeworkLoading" class="placeholder-box empty-anim">
                    <loading-outlined class="huge-icon" />
                    <p>正在加载练习题目...</p>
                  </div>

                  <div v-else-if="inlinePracticeQuestions.length === 0" class="placeholder-box empty-anim">
                    <form-outlined class="huge-icon" />
                    <p>当前练习题暂时无法解析，请检查题目结构</p>
                  </div>

                  <div v-else class="inline-question-list">
                    <div
                      v-for="question in inlinePracticeQuestions"
                      :key="question.num"
                      class="inline-question-card"
                    >
                      <div class="inline-question-head">
                        <span class="question-badge">第 {{ question.num }} 题</span>
                        <span class="question-type">{{ practiceTypeText(question.type) }}</span>
                      </div>

                      <div class="inline-question-stem">
                        {{ question.stem }}
                      </div>

                      <!-- 单选 / 判断 -->
                      <div
                        v-if="question.type === 'radio' || question.type === 'judge'"
                        class="inline-option-group"
                      >
                        <label
                          v-for="option in question.options"
                          :key="`${question.num}-${option.label}`"
                          class="inline-option-item"
                          :class="{ active: question.answer === option.label }"
                        >
                          <input
                            type="radio"
                            :name="`question-${question.num}`"
                            :checked="question.answer === option.label"
                            @change="updateSingleChoice(question, option.label)"
                          />
                          <span class="option-label">{{ option.label }}</span>
                          <span class="option-content">{{ option.content }}</span>
                        </label>
                      </div>

                      <!-- 多选 -->
                      <div v-else-if="question.type === 'checkbox'" class="inline-option-group">
                        <label
                          v-for="option in question.options"
                          :key="`${question.num}-${option.label}`"
                          class="inline-option-item"
                          :class="{ active: Array.isArray(question.answer) && question.answer.includes(option.label) }"
                        >
                          <input
                            type="checkbox"
                            :checked="Array.isArray(question.answer) && question.answer.includes(option.label)"
                            @change="toggleMultiChoice(question, option.label)"
                          />
                          <span class="option-label">{{ option.label }}</span>
                          <span class="option-content">{{ option.content }}</span>
                        </label>
                      </div>

                      <!-- 填空 -->
                      <div v-else-if="question.type === 'fill'" class="inline-answer-box">
                        <input
                          v-model="question.answer"
                          class="inline-answer-input"
                          type="text"
                          placeholder="请输入你的答案"
                        />
                      </div>

                      <!-- 简答 -->
                      <div v-else class="inline-answer-box">
              <textarea
                v-model="question.answer"
                class="inline-answer-textarea"
                rows="4"
                placeholder="请输入你的作答内容"
              />
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 页内报告区 -->
                <div v-if="homeworkPanelMode === 'report'" class="inline-report-panel">
                  <div class="inline-practice-header">
                    <div>
                      <h3>练习已提交</h3>
                      <p>教师批改完成后，可在个人主页的作业记录中查看结果</p>
                    </div>
                    <div class="inline-practice-header-actions">
                      <button class="homework-primary-btn ghost" @click="backToHomeworkOverview">
                        返回概览
                      </button>
                      <button class="homework-primary-btn" @click="openInlineHomeworkPractice">
                        再次练习
                      </button>
                    </div>
                  </div>

                  <div class="placeholder-box empty-anim">
                    <form-outlined class="huge-icon" />
                    <p>练习已提交，等待教师批改</p>
                    <button class="homework-primary-btn" @click="router.push('/student/profile?tab=homework')">
                      查看作业记录
                    </button>
                  </div>
                </div>
              </template>
            </div>
          </a-tab-pane>

        </a-tabs>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css';
// @ts-ignore  <-- 加上这一行，意思是“别管这一行的类型检查了”
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
// 引入了 shallowRef
import { ref, onMounted, onUnmounted, nextTick, computed, watch, shallowRef } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import { message } from 'ant-design-vue';
import {
  StarOutlined,
  StarFilled,
  LoadingOutlined,
  ArrowLeftOutlined,
  SendOutlined,
  InboxOutlined,
  EyeOutlined,
  CalendarOutlined,
  DashboardOutlined,
  RobotOutlined,
  UserOutlined,
  BarsOutlined,
  PlaySquareOutlined,
  EditOutlined,
  ApartmentOutlined,
  DesktopOutlined,
  CommentOutlined,
  FormOutlined,
  LikeOutlined,
  LikeFilled,
  CloseOutlined,
  SoundOutlined,
  PauseCircleOutlined
} from '@ant-design/icons-vue';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark-dimmed.css';
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/stores/user';
import { useTutorContextStore, type TutorMode } from '@/stores/tutorContext';
import CourseMindmapPanel from '@/components/course-mindmap/CourseMindmapPanel.vue';
import { getCourseMindmap, regenerateCourseMindmap } from '@/api/courseMindmap';
import type { CourseMindmapData } from '@/types/courseMindmap';
import AnimationWorkbench from '@/components/anim-player/AnimationWorkbench.vue'
import request from '@/utils/request'
import { getAuthToken, getLoginUserRaw } from '@/utils/authStorage'
import { normalizeQuizMarkdownLayout } from '@/utils/paperParser'
import {
  checkVideoIntervention,
  fetchChapterSegments,
  reportVideoLearningEvents,
  startVideoLearningSession,
  type VideoInterventionResult,
  type VideoKnowledgeSegment,
  type VideoLearningEventPayload,
  type VideoLearningEventType
} from '@/api/videoLearning'
import {
  reportLearningEvents,
  type LearningEventPayload
} from '@/api/learning'

const route = useRoute();
const router = useRouter();
const courseId = route.params.id;

const userStore = useUserStore();
const tutorContext = useTutorContextStore();
const { userInfo } = storeToRefs(userStore);

const displayAvatarUrl = computed(() => {
  const rawAvatar = userInfo.value?.avatar
  if (!rawAvatar) return 'https://api.dicebear.com/7.x/notionists/svg?seed=smart-edu'
  return resolveAssetUrl(rawAvatar)
})

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'

const SERVER_ORIGIN = API_BASE_URL.replace(/\/api\/?$/, '')

const FACE_DETECT_API =
  import.meta.env.VITE_FACE_DETECT_API || '/face/detect'

const resolveAssetUrl = (raw?: string) => {
  if (!raw) return ''
  if (raw.startsWith('http') || raw.startsWith('data:image')) return raw

  if (raw.startsWith('/api/')) {
    return `${SERVER_ORIGIN}${raw}`
  }

  const normalized = raw.startsWith('/') ? raw : `/${raw}`
  return `${API_BASE_URL}${normalized}`
}

const getBilibiliEmbedUrl = (raw?: string) => {
  if (!raw) return ''
  const bvidMatch = raw.match(/(?:bilibili\.com\/video\/|\/video\/)(BV[a-zA-Z0-9]+)/)
  if (!bvidMatch) return ''

  let page = '1'
  try {
    const parsed = new URL(raw)
    page = parsed.searchParams.get('p') || '1'
  } catch (error) {
    const pageMatch = raw.match(/[?&]p=(\d+)/)
    page = pageMatch?.[1] || '1'
  }

  return `https://player.bilibili.com/player.html?bvid=${bvidMatch[1]}&page=${page}&autoplay=0`
}
const faceSessionId = `face_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;

const topTab = ref('chapters');
const summarizedChapters = new Set();
const bottomTab = ref('mindmap');

// --- 富文本编辑器配置开始 ---
const editorRef = shallowRef();
const toolbarConfig = {};
const editorConfig = { placeholder: '在这里记录随堂笔记，支持富文本格式...' };

const handleCreated = (editor: any) => {
  editorRef.value = editor;
};

// 校验编辑器是否有实质性内容 (修复响应式丢失问题)
const editorHasContent = computed(() => {
  // 👈 核心修复：读取一下 noteContent.value，强制触发 Vue 的响应式更新
  const _trigger = noteContent.value;

  if (!editorRef.value) return false;
  // 返回编辑器内置的空状态判断
  return !editorRef.value.isEmpty();
});
// --- 富文本编辑器配置结束 ---

const mindmapLoading = ref(false);
const mindmapRegenerating = ref(false);
const mindmapError = ref('');
const mindmapData = ref<CourseMindmapData | null>(null);
const mindmapInitialized = ref(false);

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: (str: string, lang: string): string => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>';
      } catch (_) {
        return '';
      }
    }
    return '';
  }
});

const course = ref<any>({});
const isClassBoundCourse = ref(false);
const courseSourceResolved = ref(false);
const courseRequiresFaceDetection = computed(() => {
  const value = course.value?.faceDetectionRequired;
  return value === true || value === 1 || value === '1';
});

const isFavour = ref(false);

const chapterList = ref<any[]>([]);
const currentChapter = ref<any>(null);

const syncTutorCourseContext = () => {
  tutorContext.setCourseContext({
    courseId: courseId as string | number,
    courseName: displayCourseTitle.value || course.value?.name || course.value?.courseName || '课程学习',
    chapterId: currentChapter.value?.id,
    chapterTitle: currentChapter.value?.title
  })
}

const getFileNameFromUrl = (url?: string) => {
  if (!url) return '';
  try {
    const cleanUrl = url.split('?')[0].split('#')[0];
    const fileName = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
    return decodeURIComponent(fileName || '');
  } catch {
    return '';
  }
};

const displayCourseTitle = computed(() => {
  // 优先从课程对象里取名字，覆盖后端常见的各种字段名
  const c = course.value;
  const fromCourse =
    c?.name ||
    c?.courseName ||
    c?.title ||
    c?.course_name ||
    c?.courseTitle ||
    c?.course_title ||
    c?.subject ||
    c?.label;
  if (fromCourse) return fromCourse;

  // 课程对象没取到时，fallback 到章节信息（至少显示点内容）
  const ch = currentChapter.value;
  return (
    ch?.courseName ||
    ch?.courseTitle ||
    ch?.name ||
    ch?.title ||
    getFileNameFromUrl(ch?.videoUrl) ||
    ''
  );
});
const currentVideoUrl = ref('');
const currentVideoEmbedUrl = computed(() => getBilibiliEmbedUrl(currentVideoUrl.value));
const loadingChapters = ref(false);
const mainVideoRef = ref<HTMLVideoElement | null>(null);
const videoSegments = ref<VideoKnowledgeSegment[]>([]);
const videoLearningSessionId = ref<number | null>(null);
const pendingVideoEvents = ref<VideoLearningEventPayload[]>([]);
const promptedSegmentIds = ref<Set<number>>(new Set());
const promptedPauseHelpKeys = ref<Set<string>>(new Set());
const videoLearningContext = ref<Record<string, any>>({});
const videoEventFlushTimer = ref<number | null>(null);
const pauseHelpPrompt = ref<{ segment: VideoKnowledgeSegment; prompt: string } | null>(null);
const AI_COMPANION_KEY = `ai_companion_enabled_${courseId}`;
const aiCompanionEnabled = ref(localStorage.getItem(AI_COMPANION_KEY) !== '0');
const AI_SPEECH_KEY = `ai_speech_enabled_${courseId}`;
const speechSupported = typeof window !== 'undefined' && 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window;
const aiSpeechEnabled = ref(speechSupported && localStorage.getItem(AI_SPEECH_KEY) === '1');
const speakingMessageIndex = ref<number | null>(null);
let activeSpeechUtterance: SpeechSynthesisUtterance | null = null;
let seekingFromSecond: number | null = null;
let pauseStartedAt = 0;
let pauseTriggeredByAiSpeech = false;
let pauseHelpTimer: number | null = null;
let lastHeartbeatAt = 0;
let lastStableVideoSecond = 0;

const chapterHomeworkLoading = ref(false);
const chapterHomework = ref<any>(null);


const homeworkPanelMode = ref<'overview' | 'practice' | 'report'>('overview')
const inlineHomeworkLoading = ref(false)
const inlineHomeworkSubmitting = ref(false)
const inlineReportLoading = ref(false)

const inlineHomeworkDetail = ref<any>(null)
const inlineHomeworkReport = ref<any>(null)
const inlinePracticeQuestions = ref<InlinePracticeQuestion[]>([])
const inlineCurrentAssignmentId = ref<number | null>(null)

const cameraVideo = ref<HTMLVideoElement | null>(null);
const canvas = ref<HTMLCanvasElement | null>(null);
const isCameraOn = ref(false);
const isFatigue = ref(false);
const statusText = ref('未开启');
let timer: number | null = null;
let isDetecting = false;

const questionText = ref('');
const aiLoading = ref(false);

const getTodayStr = () => new Date().toISOString().slice(0, 10);
const getCurrentUserId = (): number | null => {
  try {
    // 优先读取当前标签页的登录用户，兼容升级前的共享缓存。
    // 不读 sessionStorage.user_login，该 key 是历史 bug 产物，会被污染
    const uStr = getLoginUserRaw();
    if (uStr) {
      const user = JSON.parse(uStr);
      return user.id ?? user.userId ?? user.ID ?? null;
    }
  } catch (e) { /* ignore */ }
  return null;
};

const getFatigueStorageKey = () => {
  const uid = getCurrentUserId();
  return uid ? `fatigue_stats_${uid}` : 'fatigue_stats_anonymous';
};

const fatigueStats = ref({
  dateKey: getTodayStr(),
  yawnCount: 0,
  fatigueCount: 0,
  noFaceCount: 0,
  totalDetections: 0,
  normalCount: 0,
  lastStatus: 'normal' as string,
  monitorSeconds: 0,
  lastUpdated: Date.now(),
  events: [] as Array<{ t: number; type: string; ear?: number; mar?: number }>,
  earSamples: [] as Array<{ t: number; v: number }>,
  marSamples: [] as Array<{ t: number; v: number }>,
});

let prevDetectStatus = 'normal';
let sampleCounter = 0;

let cameraTimerInterval: number | null = null;
const startCameraTimer = () => {
  if (cameraTimerInterval) return;
  cameraTimerInterval = window.setInterval(() => {
    fatigueStats.value.monitorSeconds++;
  }, 1000);
};
const stopCameraTimer = () => {
  if (cameraTimerInterval) {
    window.clearInterval(cameraTimerInterval);
    cameraTimerInterval = null;
  }
};

const saveFatigueStats = () => {
  fatigueStats.value.lastUpdated = Date.now();
  if (fatigueStats.value.events.length > 500) {
    fatigueStats.value.events = fatigueStats.value.events.slice(-500);
  }
  if (fatigueStats.value.earSamples.length > 300) {
    fatigueStats.value.earSamples = fatigueStats.value.earSamples.slice(-300);
  }
  localStorage.setItem(getFatigueStorageKey(), JSON.stringify(fatigueStats.value));  // ✅
};

const loadFatigueStats = () => {
  try {
    const key = getFatigueStorageKey();
    const raw = localStorage.getItem(key);
    if (raw) {
      const saved = JSON.parse(raw);
      if (saved.dateKey === getTodayStr()) {
        fatigueStats.value = { ...fatigueStats.value, ...saved };
      } else {
        localStorage.removeItem(key);  // ✅
      }
    }
  } catch (e) {
    console.warn('加载疲劳数据失败', e);
  }
};

let reportTimer: number | null = null;
const reportToBackend = async () => {
  if (!getCurrentUserId()) return;

  const s = fatigueStats.value;
  try {
    await request.post('/fatigue/report', {
      courseId: Number(courseId),
      chapterId: currentChapter.value?.id ?? null,
      yawnCount: s.yawnCount,
      fatigueCount: s.fatigueCount,
      noFaceCount: s.noFaceCount,
      normalCount: s.normalCount,
      totalDetections: s.totalDetections,
      monitorSeconds: s.monitorSeconds,
      events: JSON.stringify(s.events),
      earSamples: JSON.stringify(s.earSamples),
      marSamples: JSON.stringify(s.marSamples),
      lastStatus: s.lastStatus
    }, {
      skipErrorToast: true
    });
  } catch (e) {
    console.warn('❌ 疲劳数据上报失败:', e);
  }
};

const startReportTimer = () => {
  if (reportTimer) return;
  reportTimer = window.setInterval(reportToBackend, 30000);
};

const stopReportTimer = () => {
  if (reportTimer) {
    window.clearInterval(reportTimer);
    reportTimer = null;
  }
  reportToBackend();
};

const focusRate = () => {
  const total = fatigueStats.value.totalDetections;
  if (total === 0) return 0;
  return Math.round((fatigueStats.value.normalCount / total) * 100);
};

const formatMonitorTime = () => {
  const s = fatigueStats.value.monitorSeconds;
  const min = Math.floor(s / 60);
  const sec = s % 60;
  return `${min}分${sec < 10 ? '0' : ''}${sec}秒`;
};
const messageListRef = ref<HTMLElement | null>(null);
const chatHistory = ref([
  { role: 'ai', content: '同学你好！我是专属 AI 助教。\n\n视频结束后我会为你生成**重点复盘**哦！' }
]);


const renderMarkdown = (text: string) => md.render(text || '');

// ↓↓↓ 新增：将 animHtml 字段中的 JSON 模板转换为可渲染的 HTML ↓↓↓
function buildProtocolAnimHtml(data: any): string {
  const steps = data.steps || []
  const stepsHtml = steps.map((step: any, i: number) => `
    <div class="step" id="step-${i}" style="display:none">
      <div class="step-title">${step.title || ''}</div>
      <div class="step-desc">${step.desc || ''}</div>
      <div class="message-arrow ${step.messageType || ''}">
        <span class="from">${step.from || ''}</span>
        <span class="arrow">──[ ${step.message || ''} ]──▶</span>
        <span class="to">${step.to || ''}</span>
      </div>
      <div class="states">
        <span class="state">客户端：<b>${step.clientState || ''}</b></span>
        <span class="state">服务器：<b>${step.serverState || ''}</b></span>
      </div>
    </div>
  `).join('')

  return `<!DOCTYPE html><html><head><meta charset="utf-8"><style>
    body { font-family: -apple-system, sans-serif; padding: 24px; background: #f8fafc; }
    h2 { color: #1e3a8a; margin-bottom: 4px; }
    .subtitle { color: #64748b; font-size: 14px; margin-bottom: 20px; }
    .stage { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 24px; min-height: 150px; margin-bottom: 16px; }
    .step-title { font-weight: 700; font-size: 16px; color: #1e293b; margin-bottom: 8px; }
    .step-desc { color: #475569; font-size: 14px; line-height: 1.6; margin-bottom: 14px; }
    .message-arrow { display: flex; align-items: center; gap: 10px; background: #eff6ff; padding: 12px 16px; border-radius: 8px; margin-bottom: 12px; }
    .from, .to { font-weight: 700; color: #1d4ed8; }
    .arrow { flex: 1; text-align: center; color: #3b82f6; }
    .request .arrow { color: #2563eb; }
    .response .arrow { color: #16a34a; }
    .confirm .arrow { color: #7c3aed; }
    .states { display: flex; gap: 12px; }
    .state { background: #f1f5f9; border-radius: 6px; padding: 6px 12px; font-size: 13px; color: #475569; }
    .state b { color: #0f172a; }
    .controls { display: flex; align-items: center; gap: 12px; }
    button { padding: 8px 20px; border-radius: 8px; border: none; cursor: pointer; font-size: 14px; font-weight: 600; }
    #prev-btn { background: #e2e8f0; color: #475569; }
    #next-btn { background: #2563eb; color: #fff; }
    button:disabled { opacity: 0.4; cursor: not-allowed; }
    .progress { color: #94a3b8; font-size: 13px; }
  </style></head><body>
  <h2>${data.title || '协议推演'}</h2>
  <div class="subtitle">${data.subtitle || ''}</div>
  <div class="stage">${stepsHtml}</div>
  <div class="controls">
    <button id="prev-btn" disabled>◀ 上一步</button>
    <span class="progress" id="prog">1 / ${steps.length}</span>
    <button id="next-btn">下一步 ▶</button>
  </div>
  <script>
    let cur = 0, total = ${steps.length};
    function show(i) {
      document.querySelectorAll('.step').forEach(el => el.style.display = 'none');
      document.getElementById('step-' + i).style.display = 'block';
      document.getElementById('prog').textContent = (i + 1) + ' / ' + total;
      document.getElementById('prev-btn').disabled = i === 0;
      document.getElementById('next-btn').textContent = i === total - 1 ? '✓ 完成' : '下一步 ▶';
    }
    document.getElementById('prev-btn').onclick = () => { if (cur > 0) show(--cur); };
    document.getElementById('next-btn').onclick = () => { if (cur < total - 1) show(++cur); };
    show(0);
  <\/script></body></html>`
}

const resolvedAnimHtml = computed(() => {
  const raw = currentChapter.value?.animHtml
  if (!raw) return null
  try {
    const trimmed = raw.trim()
    if (trimmed.startsWith('{')) {
      const parsed = JSON.parse(trimmed)
      if (parsed.templateType === 'protocol') {
        return buildProtocolAnimHtml(parsed)
      }
    }
  } catch (e) {
    // 不是 JSON，直接当 HTML 使用
  }
  return raw
})

const animPayload = computed(() => {
  const raw = currentChapter.value?.animHtml
  if (!raw) return null
  try {
    const trimmed = raw.trim()
    if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
      return JSON.parse(trimmed)
    }
  } catch (e) {}
  return null  // 不是 JSON，说明是旧版 HTML 格式
})

// 兼容旧版：如果 animHtml 是纯 HTML 字符串则直接用 iframe
const animIsHtml = computed(() => {
  const raw = currentChapter.value?.animHtml
  if (!raw) return false
  return raw.trim().startsWith('<')
})
// ↑↑↑ 新增结束 ↑↑↑

const homeworkUiState = computed(() => {
  const hw = chapterHomework.value;
  if (!hw) return 'empty';

  const status = hw.submitStatus;
  if (!status || status === 'not_started') return 'todo';
  if (status === 'submitted' || status === 'judging' || status === 'review_pending') return 'judging';
  if (status === 'completed') return 'completed';

  // failed 等异常状态，仍按“可重新进入详情页/重做”处理
  return 'todo';
});

const homeworkUiStateText = computed(() => {
  if (homeworkUiState.value === 'todo') {
    return chapterHomework.value?.attemptCount > 0 ? '可再次练习' : '可开始练习';
  }
  if (homeworkUiState.value === 'judging') {
    return '待教师批改';
  }
  if (homeworkUiState.value === 'completed') {
    return '已完成，可再次练习';
  }
  return '暂无练习';
});


const formatHomeworkDateTime = (value: any) => {
  if (!value) return '未设置';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);

  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const isHomeworkDeadlinePassed = (value: any) => {
  if (!value) return false;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return false;
  return d.getTime() < Date.now();
};

const fetchCurrentChapterHomework = async (chapter?: any) => {
  if (!isClassBoundCourse.value) {
    chapterHomework.value = null
    chapterHomeworkLoading.value = false
    return
  }
  if (!chapter?.id || !courseId) {
    chapterHomework.value = null
    return
  }

  chapterHomeworkLoading.value = true
  try {
    const data = await request.get<any, any>(
      '/homework/student/course-chapter/practice/current',
      {
        params: {
          courseId,
          chapterId: chapter.id
        }
      }
    )
    chapterHomework.value = data || null
  } catch (error) {
    chapterHomework.value = null
    message.error('读取章节练习失败')
  } finally {
    chapterHomeworkLoading.value = false
  }
}

const backToHomeworkOverview = () => {
  homeworkPanelMode.value = 'overview'
}

const resetInlineHomeworkState = () => {
  homeworkPanelMode.value = 'overview'
  inlineHomeworkLoading.value = false
  inlineHomeworkSubmitting.value = false
  inlineReportLoading.value = false
  inlineHomeworkDetail.value = null
  inlineHomeworkReport.value = null
  inlinePracticeQuestions.value = []
  inlineCurrentAssignmentId.value = null
}


type InlinePracticeOption = {
  label: string
  content: string
}

type InlinePracticeQuestion = {
  num: string
  type: 'radio' | 'checkbox' | 'judge' | 'fill' | 'text'
  stem: string
  options: InlinePracticeOption[]
  score?: number | null
  answer: string | string[]
}

const safeParseJson = (text: string) => {
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch (e) {
    return null
  }
}

const normalizePracticeType = (
  rawType: any,
  options: InlinePracticeOption[] = [],
  inheritedType = ''
): 'radio' | 'checkbox' | 'judge' | 'fill' | 'text' => {
  const text = `${rawType || ''} ${inheritedType || ''}`.toLowerCase()

  if (text.includes('multiple') || text.includes('checkbox') || text.includes('多选')) return 'checkbox'
  if (text.includes('judge') || text.includes('true') || text.includes('false') || text.includes('判断')) return 'judge'
  if (text.includes('fill') || text.includes('blank') || text.includes('填空')) return 'fill'
  if (
    text.includes('single') ||
    text.includes('radio') ||
    text.includes('choice') ||
    text.includes('select') ||
    text.includes('单选') ||
    text.includes('选择')
  ) return 'radio'

  if (options.length === 2) {
    const optionText = options.map(item => item.content).join('|')
    if (optionText.includes('对') && optionText.includes('错')) {
      return 'judge'
    }
  }

  if (options.length > 0) return 'radio'
  return 'text'
}

const normalizeOptions = (rawOptions: any): InlinePracticeOption[] => {
  if (!Array.isArray(rawOptions)) return []

  return rawOptions
    .map((item: any, index: number) => {
      if (typeof item === 'string') {
        return {
          label: String.fromCharCode(65 + index),
          content: item.trim()
        }
      }

      const label =
        item?.label ||
        item?.key ||
        item?.code ||
        item?.optionKey ||
        String.fromCharCode(65 + index)

      const content =
        item?.content ||
        item?.text ||
        item?.value ||
        item?.title ||
        item?.optionContent ||
        ''

      return {
        label: String(label).trim(),
        content: String(content).trim()
      }
    })
    .filter((item) => item.content)
}

const looksLikeAnswerBlock = (text: string) => {
  const t = String(text || '').trim()
  if (!t) return false

  return (
    /^(\*\*)?\[?答案\]?(\*\*)?/i.test(t) ||
    /^(\*\*)?\[?解析\]?(\*\*)?/i.test(t) ||
    /^答案[：:]/.test(t) ||
    /^解析[：:]/.test(t) ||
    /^参考答案/.test(t) ||
    /^标准答案/.test(t) ||
    /^(?:#{1,6}\s*)?(?:[一二三四五六七八九十]+|\d+)[、.．]?\s*(?:单项选择题|单选题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|案例分析题)\s*(?:参考)?答案(?:与解析|解析)?/.test(t)
  )
}

const stripAnswerSectionFromMarkdown = (content: string) => {
  if (!content) return ''

  let text = normalizeQuizMarkdownLayout(content)

  const answerSectionPatterns = [
    /^#{1,6}\s*参考答案与解析.*$/m,
    /^#{1,6}\s*参考答案.*$/m,
    /^#{1,6}\s*答案与解析.*$/m,
    /^#{1,6}\s*标准答案.*$/m,
    /^#{1,6}\s*答案解析.*$/m,
    /^#{0,6}\s*(?:[一二三四五六七八九十]+|\d+)[、.．]?\s*(?:单项选择题|单选题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|案例分析题)\s*(?:参考)?答案(?:与解析|解析)?.*$/m
  ]

  let cutIndex = -1
  answerSectionPatterns.forEach((pattern) => {
    const match = pattern.exec(text)
    if (match && match.index >= 0) {
      if (cutIndex === -1 || match.index < cutIndex) {
        cutIndex = match.index
      }
    }
  })

  if (cutIndex >= 0) {
    text = text.slice(0, cutIndex)
  }

  const lines = text.split('\n')
  const filtered: string[] = []

  for (const line of lines) {
    const trimmed = line.trim()
    if (looksLikeAnswerBlock(trimmed)) {
      continue
    }
    filtered.push(line)
  }

  return filtered.join('\n').trim()
}

const normalizeSpeechText = (content: string) => {
  return stripAnswerSectionFromMarkdown(content)
    .replace(/```[\s\S]*?```/g, '这里有一段代码。')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/!\[[^\]]*]\([^)]*\)/g, '')
    .replace(/\[([^\]]+)]\([^)]*\)/g, '$1')
    .replace(/^#{1,6}\s*/gm, '')
    .replace(/[*_~>]/g, '')
    .replace(/<[^>]+>/g, '')
    .replace(/https?:\/\/\S+/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

const detectQuestionTypeFromSectionTitle = (line: string): 'radio' | 'checkbox' | 'judge' | 'fill' | 'text' | '' => {
  const t = line.trim()
  const typeHeading = '(?:单项选择题|单选题|选择题|多项选择题|多选题|判断题|填空题|简答题|问答题|论述题|计算题|编程题|代码题|综合题|案例分析题)\\s*(?:[（(][^）)]*[）)])?\\s*(?:参考)?(?:答案)?(?:与解析|解析)?'
  const directHeading = new RegExp(`^#{0,6}\\s*${typeHeading}$`)
  const numberedHeading = new RegExp(`^#{0,6}\\s*(?:[一二三四五六七八九十]+|\\d+)[、.．]\\s*${typeHeading}$`)

  if (!directHeading.test(t) && !numberedHeading.test(t)) return ''

  if (t.includes('单项选择题') || t.includes('单选题') || t.includes('选择题')) return 'radio'
  if (t.includes('多项选择题') || t.includes('多选题')) return 'checkbox'
  if (t.includes('判断题')) return 'judge'
  if (t.includes('填空题')) return 'fill'
  if (t.includes('简答题') || t.includes('问答题') || t.includes('编程题')) return 'text'

  return ''
}

const parseQuestionsFromMarkdown = (content: string): InlinePracticeQuestion[] => {
  const cleaned = stripAnswerSectionFromMarkdown(content)
  if (!cleaned) return []

  const lines = cleaned.split('\n')

  const result: InlinePracticeQuestion[] = []
  let currentType: InlinePracticeQuestion['type'] | '' = ''
  let currentQuestion: InlinePracticeQuestion | null = null

  const pushCurrent = () => {
    if (!currentQuestion) return

    currentQuestion.stem = currentQuestion.stem.trim()
    currentQuestion.options = currentQuestion.options.filter(item => item.content.trim())

    if (!currentQuestion.stem) {
      currentQuestion = null
      return
    }

    if (currentQuestion.type === 'judge' && currentQuestion.options.length === 0) {
      currentQuestion.options = [
        { label: 'A', content: '对' },
        { label: 'B', content: '错' }
      ]
    }

    if (!currentQuestion.type) {
      currentQuestion.type = currentQuestion.options.length > 0 ? 'radio' : 'text'
    }

    result.push(currentQuestion)
    currentQuestion = null
  }

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) continue

    if (looksLikeAnswerBlock(line)) {
      continue
    }

    const sectionType = detectQuestionTypeFromSectionTitle(line)
    if (sectionType) {
      currentType = sectionType
      continue
    }

    const qMatch = line.match(/^(?:#{0,6}\s*)?(?:第\s*(\d+)\s*题|(\d+)[.．、])\s*(.+)$/)
    if (qMatch) {
      pushCurrent()

      const num = qMatch[1] || qMatch[2] || String(result.length + 1)
      const stem = (qMatch[3] || '').trim()

      currentQuestion = {
        num,
        type: currentType || 'text',
        stem,
        options: [],
        score: null,
        answer: currentType === 'checkbox' ? [] : ''
      }
      continue
    }

    const optionMatch = line.match(/^([A-H])[.．、:：)\s]\s*(.+)$/)
    if (optionMatch && currentQuestion) {
      currentQuestion.options.push({
        label: optionMatch[1],
        content: optionMatch[2].trim()
      })
      continue
    }

    if (!currentQuestion) {
      continue
    }

    // 避免把“答案/解析”拼进题干
    if (looksLikeAnswerBlock(line)) {
      continue
    }

    currentQuestion.stem += `\n${line}`
  }

  pushCurrent()

  return result.map((item) => {
    const finalType = normalizePracticeType(item.type, item.options, item.type)
    return {
      ...item,
      type: finalType,
      answer: finalType === 'checkbox' ? [] : ''
    }
  })
}

const buildInlineQuestionFromParams = (
  item: any,
  index: number,
  inheritedType = ''
): InlinePracticeQuestion | null => {
  const rawOptions =
    item?.options ||
    item?.choices ||
    item?.optionList ||
    item?.choiceList ||
    []

  const options = normalizeOptions(rawOptions)

  const stem = String(
    item?.stem ||
    item?.question ||
    item?.title ||
    item?.content ||
    ''
  ).trim()

  if (!stem || looksLikeAnswerBlock(stem)) {
    return null
  }

  const type = normalizePracticeType(
    item?.type || item?.questionType,
    options,
    inheritedType
  )

  const num = String(item?.num || item?.questionNo || item?.no || index + 1)

  return {
    num,
    type,
    stem,
    options:
      type === 'judge' && options.length === 0
        ? [
          { label: 'A', content: '对' },
          { label: 'B', content: '错' }
        ]
        : options,
    score: item?.score ?? item?.fullScore ?? item?.points ?? null,
    answer: type === 'checkbox' ? [] : ''
  }
}

const extractQuestionItemsFromParams = (root: any): Array<{ item: any; inheritedType: string }> => {
  const result: Array<{ item: any; inheritedType: string }> = []

  const visit = (node: any, inheritedType = '') => {
    if (!node) return

    if (Array.isArray(node)) {
      node.forEach((child) => visit(child, inheritedType))
      return
    }

    if (typeof node !== 'object') return

    const nextType = String(
      node?.type || node?.questionType || node?.sectionType || node?.name || inheritedType || ''
    )

    const hasQuestionContent = !!(node?.stem || node?.question || node?.title || node?.content)
    const hasOptionInfo = !!(node?.options || node?.choices || node?.optionList || node?.choiceList)
    const hasTypeInfo = !!(node?.type || node?.questionType || node?.score || node?.fullScore || node?.points)

    // 过滤明显答案节点
    const textForCheck = `${node?.stem || ''} ${node?.question || ''} ${node?.title || ''} ${node?.content || ''}`
    const hasAnswerSignals =
      node?.answer !== undefined ||
      node?.correctAnswer !== undefined ||
      node?.analysis !== undefined ||
      node?.explanation !== undefined

    if (hasQuestionContent && (hasOptionInfo || hasTypeInfo) && !looksLikeAnswerBlock(textForCheck)) {
      result.push({ item: node, inheritedType })
    } else if (hasQuestionContent && hasAnswerSignals && !hasOptionInfo) {
      // 这类大概率是答案块，不进结果
    }

    if (node?.questions) visit(node.questions, nextType)
    if (node?.questionList) visit(node.questionList, nextType)
    if (node?.items) visit(node.items, nextType)
    if (node?.list) visit(node.list, nextType)
    if (node?.children) visit(node.children, nextType)

    if (Array.isArray(node?.sections)) {
      node.sections.forEach((section: any) => visit(section, nextType))
    }
  }

  visit(root)
  return result
}

const parseInlinePracticeQuestions = (detail: any): InlinePracticeQuestion[] => {
  // 先用 markdown 解析，优先避免把答案区带出来
  const markdownQuestions = parseQuestionsFromMarkdown(detail?.contentSnapshot || '')
  if (markdownQuestions.length > 0) {
    return markdownQuestions.map((item, index) => ({
      ...item,
      num: item.num || String(index + 1),
      answer: item.type === 'checkbox' ? [] : ''
    }))
  }

  // markdown 不行，再退回 params
  const paramsRoot = safeParseJson(detail?.paramsSnapshot || '')
  if (paramsRoot) {
    const rawItems = extractQuestionItemsFromParams(paramsRoot)

    const questions = rawItems
      .map(({ item, inheritedType }, index) => buildInlineQuestionFromParams(item, index, inheritedType))
      .filter(Boolean) as InlinePracticeQuestion[]

    return questions.map((item, index) => ({
      ...item,
      num: item.num || String(index + 1),
      answer: item.type === 'checkbox' ? [] : ''
    }))
  }

  return []
}

const practiceTypeText = (type: string) => {
  if (type === 'radio') return '单选题'
  if (type === 'checkbox') return '多选题'
  if (type === 'judge') return '判断题'
  if (type === 'fill') return '填空题'
  return '简答题'
}

const submitStatusText = (status: string | undefined) => {
  const map: Record<string, string> = {
    not_started: '未开始',
    submitted: '已提交',
    judging: '待教师批改',
    review_pending: '待教师批改',
    completed: '已完成',
    failed: '批改失败'
  }
  return status ? (map[status] ?? status) : '--'
}

const updateSingleChoice = (question: InlinePracticeQuestion, value: string) => {
  question.answer = value
}

const toggleMultiChoice = (question: InlinePracticeQuestion, value: string) => {
  const current = Array.isArray(question.answer) ? [...question.answer] : []
  const exists = current.includes(value)

  question.answer = exists
    ? current.filter(item => item !== value)
    : [...current, value]
}

const isQuestionAnswered = (question: InlinePracticeQuestion) => {
  if (question.type === 'checkbox') {
    return Array.isArray(question.answer) && question.answer.length > 0
  }
  return String(question.answer || '').trim().length > 0
}

const loadInlineHomeworkDetail = async (assignmentId: number) => {
  inlineHomeworkLoading.value = true
  try {
    const data = await request.get<any, any>('/homework/student/detail', {
      params: { assignmentId }
    })
    inlineHomeworkDetail.value = data || null
    inlinePracticeQuestions.value = parseInlinePracticeQuestions(data || {})
    inlineCurrentAssignmentId.value = assignmentId
  } catch (error) {
    message.error('读取练习详情失败')
  } finally {
    inlineHomeworkLoading.value = false
  }
}

const loadInlineHomeworkReport = async (submissionId: number) => {
  inlineReportLoading.value = true
  try {
    const data = await request.get<any, any>('/homework/student/report', {
      params: { submissionId }
    })
    inlineHomeworkReport.value = data || null
  } catch (error) {
    inlineHomeworkReport.value = null
    message.error('读取练习报告失败')
  } finally {
    inlineReportLoading.value = false
  }
}

const openInlineHomeworkPractice = async () => {
  const hw = chapterHomework.value
  if (!hw?.assignmentId) return

  homeworkPanelMode.value = 'practice'
  inlineHomeworkReport.value = null
  reportLearningEvent({
    eventType: 'practice_start',
    resourceId: hw.assignmentId,
    resourceType: 'chapter_practice',
    knowledgeName: hw.chapterTitle || currentChapter.value?.title || ''
  })

  await loadInlineHomeworkDetail(hw.assignmentId)
}

const openInlineHomeworkReport = async () => {
  const hw = chapterHomework.value
  if (!hw?.latestSubmissionId) {
    message.warning('当前还没有可查看的练习报告')
    return
  }

  homeworkPanelMode.value = 'report'
  await loadInlineHomeworkReport(hw.latestSubmissionId)
  if ((inlineHomeworkReport.value?.submission?.wrongCount || 0) > 0) {
    reportLearningEvent({
      eventType: 'wrong_question_review',
      resourceId: hw.latestSubmissionId,
      resourceType: 'homework_report',
      knowledgeName: hw.chapterTitle || currentChapter.value?.title || '',
      extraJson: JSON.stringify({ wrongCount: inlineHomeworkReport.value?.submission?.wrongCount || 0 })
    })
  }
}

const buildSubmitAnswerPayload = () => {
  return inlinePracticeQuestions.value.map((question, index) => ({
    num: String(index + 1),
    originalQuestionNo: question.num,
    type: question.type,
    stem: question.stem,
    fullScore: question.score ?? null,
    answer: question.type === 'checkbox'
      ? (Array.isArray(question.answer) ? question.answer : [])
      : String(question.answer || '').trim()
  }))
}

const submitInlinePractice = async () => {
  const hw = chapterHomework.value
  if (!hw?.assignmentId) return

  const unanswered = inlinePracticeQuestions.value.filter((question) => !isQuestionAnswered(question))
  if (unanswered.length > 0) {
    message.warning(`还有 ${unanswered.length} 题未作答，请完成后再提交`)
    return
  }

  inlineHomeworkSubmitting.value = true
  try {
    const payload = {
      assignmentId: hw.assignmentId,
      studentAnswerJson: JSON.stringify(buildSubmitAnswerPayload())
    }

    await request.post<number, number>(
      '/homework/submission/submit-async',
      payload
    )

    message.success('已提交，等待教师批改')
    reportLearningEvent({
      eventType: 'practice_submit',
      resourceId: hw.assignmentId,
      resourceType: 'chapter_practice',
      knowledgeName: hw.chapterTitle || currentChapter.value?.title || '',
      extraJson: JSON.stringify({ questionCount: inlinePracticeQuestions.value.length })
    })
    homeworkPanelMode.value = 'report'
    await fetchCurrentChapterHomework(currentChapter.value)
  } catch (error: any) {
    message.error(error?.message || '练习提交失败')
  } finally {
    inlineHomeworkSubmitting.value = false
  }
}


const resolveCourseSource = async () => {
  try {
    const data = await request.get<any[], any[]>('/course/list/my-class', {
      skipErrorToast: true
    });
    const list = Array.isArray(data) ? data : [];
    const matchedCourse = list.find((item: any) => String(item.id) === String(courseId));
    isClassBoundCourse.value = !!matchedCourse;
    if (matchedCourse) {
      course.value = matchedCourse;
      syncTutorCourseContext();
    }
  } catch (error) {
    isClassBoundCourse.value = false;
  } finally {
    courseSourceResolved.value = true;
    if (!isClassBoundCourse.value && bottomTab.value === 'homework') {
      bottomTab.value = 'mindmap';
    }
  }
};

const fetchCourseDetail = async () => {
  try {
    // /course/list/all 是分页接口，默认只返回 10 条
    // 传 size=1000 确保能拿到所有平台课程，再按 ID 匹配
    const data = await request.get<any, any>('/course/list/all', {
      params: { current: 1, size: 1000 }
    });

    const list: any[] = Array.isArray(data)
      ? data
      : Array.isArray(data?.records)
        ? data.records
        : Array.isArray(data?.data)
          ? data.data
          : [];

    const found = list.find((c: any) => String(c.id) === String(courseId));
    if (found) {
      course.value = found;
      syncTutorCourseContext();
    }
  } catch (e) {
    console.warn('获取课程详情失败', e);
  }
};

const currentVideoSecond = () => Math.floor(mainVideoRef.value?.currentTime || 0);

const findSegmentBySecond = (second: number) => {
  return videoSegments.value.find((segment) => {
    return second >= segment.startSecond && second < segment.endSecond;
  }) || null;
};

const currentSegment = () => findSegmentBySecond(currentVideoSecond());

const currentChapterAsSegment = (): VideoKnowledgeSegment | null => {
  if (!currentChapter.value?.id) return null;
  const second = currentVideoSecond();
  return {
    startSecond: second,
    endSecond: second + 1,
    knowledgeName: currentChapter.value?.title || displayCourseTitle.value || '当前章节',
    description: displayCourseTitle.value ? `当前课程：${displayCourseTitle.value}` : '',
    difficulty: ''
  };
};

const pauseHelpKey = (segment: VideoKnowledgeSegment | null) => {
  if (segment?.id) return `segment:${segment.id}`;
  if (currentChapter.value?.id) return `chapter:${currentChapter.value.id}`;
  return '';
};

const formatSegmentTime = (segment?: VideoKnowledgeSegment | null) => {
  if (!segment) return '';
  const fmt = (seconds: number) => {
    const mm = Math.floor(seconds / 60).toString().padStart(2, '0');
    const ss = Math.floor(seconds % 60).toString().padStart(2, '0');
    return `${mm}:${ss}`;
  };
  return `${fmt(segment.startSecond)}-${fmt(segment.endSecond)}`;
};

const reportLearningEvent = (event: LearningEventPayload) => {
  reportLearningEvents([
    {
      courseId: event.courseId ?? (courseId as string | number),
      chapterId: event.chapterId ?? currentChapter.value?.id ?? null,
      ...event
    }
  ]).catch(() => undefined)
}

const resetVideoLearningState = () => {
  if (videoEventFlushTimer.value) {
    window.clearTimeout(videoEventFlushTimer.value);
    videoEventFlushTimer.value = null;
  }
  clearPauseHelpTimer();
  pauseHelpPrompt.value = null;
  videoSegments.value = [];
  videoLearningSessionId.value = null;
  pendingVideoEvents.value = [];
  promptedSegmentIds.value = new Set();
  promptedPauseHelpKeys.value = new Set();
  videoLearningContext.value = {};
  seekingFromSecond = null;
  pauseStartedAt = 0;
  lastHeartbeatAt = 0;
  lastStableVideoSecond = 0;
};

const clearPauseHelpTimer = () => {
  if (pauseHelpTimer) {
    window.clearTimeout(pauseHelpTimer);
    pauseHelpTimer = null;
  }
};

const handleAiCompanionToggle = (checked: boolean) => {
  localStorage.setItem(AI_COMPANION_KEY, checked ? '1' : '0');
  if (!checked) {
    clearPauseHelpTimer();
    pauseHelpPrompt.value = null;
    message.info('行为提醒已关闭');
  } else {
    message.success('已开启行为伴学提醒');
  }
};

const stopAiSpeech = () => {
  if (speechSupported) {
    window.speechSynthesis.cancel();
  }
  activeSpeechUtterance = null;
  speakingMessageIndex.value = null;
};

const speakAiMessage = (content: string, messageIndex: number | null = null) => {
  if (!speechSupported) {
    message.warning('当前浏览器不支持语音播报');
    return;
  }

  const text = normalizeSpeechText(content);
  if (!text) return;

  stopAiSpeech();
  if (mainVideoRef.value && !mainVideoRef.value.paused && !mainVideoRef.value.ended) {
    pauseTriggeredByAiSpeech = true;
    mainVideoRef.value.pause();
  }

  const utterance = new SpeechSynthesisUtterance(text.slice(0, 1200));
  utterance.lang = 'zh-CN';
  utterance.rate = 1.25;
  utterance.pitch = 1;

  const voices = window.speechSynthesis.getVoices();
  const preferredVoice = voices.find((voice) =>
    voice.lang.toLowerCase().includes('zh') || /中文|chinese/i.test(voice.name)
  );
  if (preferredVoice) {
    utterance.voice = preferredVoice;
  }

  utterance.onend = () => {
    if (activeSpeechUtterance === utterance) {
      activeSpeechUtterance = null;
      speakingMessageIndex.value = null;
    }
  };
  utterance.onerror = utterance.onend;

  activeSpeechUtterance = utterance;
  speakingMessageIndex.value = messageIndex;
  window.speechSynthesis.speak(utterance);
};

const toggleSpeakMessage = (index: number, content: string) => {
  if (speakingMessageIndex.value === index) {
    stopAiSpeech();
    return;
  }
  speakAiMessage(content, index);
};

const handleAiSpeechToggle = (checked: boolean) => {
  if (!speechSupported) {
    aiSpeechEnabled.value = false;
    message.warning('当前浏览器不支持语音播报');
    return;
  }
  aiSpeechEnabled.value = checked;
  localStorage.setItem(AI_SPEECH_KEY, checked ? '1' : '0');
  if (!checked) {
    stopAiSpeech();
    message.info('已关闭 AI 语音播报');
  } else {
    message.success('已开启 AI 语音播报');
  }
};

const flushVideoEvents = async () => {
  if (videoEventFlushTimer.value) {
    window.clearTimeout(videoEventFlushTimer.value);
    videoEventFlushTimer.value = null;
  }
  if (!videoLearningSessionId.value || pendingVideoEvents.value.length === 0) return;

  const events = pendingVideoEvents.value.splice(0, pendingVideoEvents.value.length);
  try {
    await reportVideoLearningEvents({
      sessionId: videoLearningSessionId.value,
      events
    });
  } catch (e) {
    pendingVideoEvents.value.unshift(...events);
  }
};

const scheduleVideoEventFlush = () => {
  if (videoEventFlushTimer.value) return;
  videoEventFlushTimer.value = window.setTimeout(() => {
    flushVideoEvents();
  }, 5000);
};

const queueVideoEvent = async (
  eventType: VideoLearningEventType,
  options: Partial<VideoLearningEventPayload> = {},
  flushNow = false
) => {
  if (!videoLearningSessionId.value) return;
  const second = options.toSecond ?? options.fromSecond ?? currentVideoSecond();
  const segment = options.segmentId ? null : findSegmentBySecond(second || 0);
  pendingVideoEvents.value.push({
    eventType,
    segmentId: options.segmentId ?? segment?.id ?? null,
    fromSecond: options.fromSecond ?? second ?? null,
    toSecond: options.toSecond ?? second ?? null,
    durationSecond: options.durationSecond ?? null,
    playbackRate: options.playbackRate ?? mainVideoRef.value?.playbackRate ?? null,
    extraJson: options.extraJson
  });
  if (flushNow) {
    await flushVideoEvents();
  } else {
    scheduleVideoEventFlush();
  }
};

const startVideoLearning = async (chapter: any) => {
  resetVideoLearningState();
  if (!chapter?.id) return;
  try {
    const [segments, sessionId] = await Promise.all([
      fetchChapterSegments(chapter.id),
      startVideoLearningSession({ courseId: courseId as string | number, chapterId: chapter.id })
    ]);
    videoSegments.value = segments || [];
    videoLearningSessionId.value = sessionId;
  } catch (e) {
    console.warn('视频学习行为采集初始化失败', e);
  }
};

const maybeTriggerVideoIntervention = async (
  segment: VideoKnowledgeSegment | null,
  latestEventType: VideoLearningEventType
) => {
  if (!aiCompanionEnabled.value || !videoLearningSessionId.value || !segment?.id || promptedSegmentIds.value.has(segment.id) || aiLoading.value) {
    return;
  }
  try {
    const result: VideoInterventionResult = await checkVideoIntervention({
      sessionId: videoLearningSessionId.value,
      segmentId: segment.id,
      latestEventType
    });
    if (!result?.triggered || !result.suggestedPrompt) return;

    promptedSegmentIds.value.add(segment.id);
    videoLearningContext.value = {
      knowledgeName: result.knowledgeName || segment.knowledgeName,
      description: segment.description || '',
      difficulty: segment.difficulty || '',
      timeRange: formatSegmentTime(segment),
      behaviorSummary: result.behaviorSummary || ''
    };
    chatHistory.value.push({
      role: 'ai',
      content: `我注意到你在 **${result.knowledgeName || segment.knowledgeName}** 这里停留较多，我换一种方式讲一遍。`
    });
    scrollToBottom();
    await callAiStream(result.suggestedPrompt, 'explain');
  } catch (e) {
    console.warn('视频学习干预检查失败', e);
  }
};

const buildPauseHelpPrompt = (segment: VideoKnowledgeSegment) => {
  return [
    `学生在视频知识点「${segment.knowledgeName}」对应时间段 ${formatSegmentTime(segment)} 暂停超过 10 秒。`,
    `请用适合初学者的方式解释这个知识点，先说明它解决什么问题，再拆成 3 个以内的关键点。`,
    segment.description ? `教师标注说明：${segment.description}` : '',
    segment.difficulty ? `知识点难度：${segment.difficulty}` : '',
    `最后给学生一个很小的自测问题，帮助确认是否听懂。`
  ].filter(Boolean).join('\n');
};

const showPauseHelpPrompt = (segment: VideoKnowledgeSegment) => {
  const key = pauseHelpKey(segment);
  if (!aiCompanionEnabled.value || !key || promptedPauseHelpKeys.value.has(key) || aiLoading.value) return;
  promptedPauseHelpKeys.value.add(key);
  if (segment.id) {
    promptedSegmentIds.value.add(segment.id);
  }
  videoLearningContext.value = {
    knowledgeName: segment.knowledgeName,
    description: segment.description || '',
    difficulty: segment.difficulty || '',
    timeRange: formatSegmentTime(segment),
    behaviorSummary: '学生在该知识点范围内暂停超过 10 秒'
  };
  pauseHelpPrompt.value = {
    segment,
    prompt: buildPauseHelpPrompt(segment)
  };
  topTab.value = 'ai';
  queueVideoEvent('intervention_shown', {
    segmentId: segment.id ?? null,
    extraJson: JSON.stringify({
      trigger: 'pause_over_10s',
      knowledgeName: segment.knowledgeName,
      timeRange: formatSegmentTime(segment)
    })
  }, false);
  scrollToBottom();
};

const schedulePauseHelpPrompt = (segment: VideoKnowledgeSegment | null) => {
  clearPauseHelpTimer();
  if (!aiCompanionEnabled.value) return;
  const targetSegment = segment || currentChapterAsSegment();
  const targetKey = pauseHelpKey(targetSegment);
  if (!targetSegment || !targetKey || promptedPauseHelpKeys.value.has(targetKey)) return;
  pauseHelpTimer = window.setTimeout(() => {
    pauseHelpTimer = null;
    const video = mainVideoRef.value;
    if (!video || !video.paused || video.ended || pauseStartedAt <= 0) return;
    const latestSegment = currentSegment() || currentChapterAsSegment();
    if (!latestSegment || pauseHelpKey(latestSegment) !== targetKey) return;
    showPauseHelpPrompt(latestSegment);
  }, 6000);
};

const acceptPauseHelp = async () => {
  const prompt = pauseHelpPrompt.value;
  if (!prompt || aiLoading.value) return;
  pauseHelpPrompt.value = null;
  chatHistory.value.push({ role: 'user', content: '需要，请帮我解释一下。' });
  await queueVideoEvent('intervention_clicked', {
    segmentId: prompt.segment.id ?? null,
    extraJson: JSON.stringify({ action: 'accept_pause_help', trigger: 'pause_over_10s' })
  }, true);
  scrollToBottom();
  await callAiStream(prompt.prompt, 'explain');
};

const dismissPauseHelp = async () => {
  const prompt = pauseHelpPrompt.value;
  pauseHelpPrompt.value = null;
  if (!prompt) return;
  await queueVideoEvent('intervention_clicked', {
    segmentId: prompt.segment.id ?? null,
    extraJson: JSON.stringify({ action: 'dismiss_pause_help', trigger: 'pause_over_10s' })
  }, true);
};

const handleVideoPlay = async () => {
  if (courseRequiresFaceDetection.value && !isCameraOn.value) {
    message.warning('本课程要求开启学习状态检测，请先开启摄像头检测');
  }
  clearPauseHelpTimer();
  lastStableVideoSecond = currentVideoSecond();
  const now = Date.now();
  if (pauseStartedAt > 0) {
    const durationSecond = Math.round((now - pauseStartedAt) / 1000);
    const segment = currentSegment();
    if (durationSecond >= 1) {
      await queueVideoEvent('pause', { durationSecond, segmentId: segment?.id ?? null }, true);
    }
    pauseStartedAt = 0;
    await queueVideoEvent('resume', {}, true);
    return;
  }
  await queueVideoEvent('play', {}, true);
};

const handleVideoPause = () => {
  if (mainVideoRef.value?.ended) return;
  pauseStartedAt = Date.now();
  if (pauseTriggeredByAiSpeech) {
    pauseTriggeredByAiSpeech = false;
    return;
  }
  schedulePauseHelpPrompt(currentSegment() || currentChapterAsSegment());
};

const handleVideoSeeking = () => {
  clearPauseHelpTimer();
  seekingFromSecond = lastStableVideoSecond || currentVideoSecond();
};

const handleVideoSeeked = async () => {
  if (seekingFromSecond === null) return;
  const fromSecond = seekingFromSecond;
  const toSecond = currentVideoSecond();
  const delta = toSecond - fromSecond;
  seekingFromSecond = null;

  if (delta <= -10) {
    const segment = findSegmentBySecond(toSecond);
    await queueVideoEvent('seek_backward', { fromSecond, toSecond, segmentId: segment?.id ?? null }, true);
    await maybeTriggerVideoIntervention(segment, 'seek_backward');
  } else if (delta >= 15) {
    const segment = findSegmentBySecond(fromSecond);
    await queueVideoEvent('seek_forward', { fromSecond, toSecond, segmentId: segment?.id ?? null }, true);
  }
  lastStableVideoSecond = toSecond;
};

const handleVideoRateChange = async () => {
  await queueVideoEvent('rate_change', {
    playbackRate: mainVideoRef.value?.playbackRate ?? 1
  }, true);
};

const handleVideoTimeUpdate = () => {
  if (!mainVideoRef.value?.seeking) {
    lastStableVideoSecond = currentVideoSecond();
  }
  const now = Date.now();
  if (now - lastHeartbeatAt < 30000) return;
  lastHeartbeatAt = now;
  queueVideoEvent('heartbeat', {}, false);
};

const finishVideoLearning = async () => {
  clearPauseHelpTimer();
  if (pauseStartedAt > 0) {
    const durationSecond = Math.round((Date.now() - pauseStartedAt) / 1000);
    const segment = currentSegment();
    await queueVideoEvent('pause', { durationSecond, segmentId: segment?.id ?? null }, false);
    pauseStartedAt = 0;
  }
  await queueVideoEvent('ended', {}, true);
};

const fetchChapters = async () => {
  loadingChapters.value = true
  try {
    const data = await request.get<any[], any[]>('/chapter/list', {
      params: { courseId }
    })
    chapterList.value = data || []

    if (chapterList.value.length > 0) {
      const queryChapterId = Number(route.query.chapterId)
      const targetChapter = Number.isFinite(queryChapterId)
        ? chapterList.value.find((chapter: any) => Number(chapter.id) === queryChapterId)
        : null
      await selectChapter(targetChapter || chapterList.value[0])
    }
  } catch (e) {
    message.error('获取选集列表失败')
  } finally {
    loadingChapters.value = false
  }
}

const selectChapter = async (chapter: any) => {
  stopAiSpeech()
  await flushVideoEvents()
  currentChapter.value = chapter
  currentVideoUrl.value = resolveAssetUrl(chapter.videoUrl)
  reportLearningEvent({
    eventType: 'resource_click',
    resourceId: chapter.id,
    resourceType: 'chapter',
    knowledgeName: chapter.title || ''
  })
  syncTutorCourseContext()
  await startVideoLearning(chapter)

  resetInlineHomeworkState()

  chatHistory.value.push({
    role: 'ai',
    content: `正在为您播放：**${chapter.title}**`
  })
  scrollToBottom()
  loadChapterNote()
  fetchCurrentChapterHomework(chapter)
}

const handleVideoEnded = async () => {
  await finishVideoLearning();
  const chapterId = currentChapter.value?.id;
  if (!aiCompanionEnabled.value || aiLoading.value || (chapterId && summarizedChapters.has(chapterId))) {
    return;
  }
  if (chapterId) summarizedChapters.add(chapterId);

  const curTitle = currentChapter.value?.title || course.value.name;
  message.success('学习完成！AI 助教正在生成报告...');
  topTab.value = 'ai';

  const summaryPrompt = `我刚刚看完了【${displayCourseTitle.value || course.value.name || '当前课程'}】中的章节：【${curTitle}】。请生成核心摘要、一个例子和一道检查题。`;
  await callAiStream(summaryPrompt, 'summary');
};

const sendQuestion = async () => {
  if (!questionText.value.trim() || aiLoading.value) return;

  stopAiSpeech();
  const q = questionText.value;
  reportLearningEvent({
    eventType: 'ai_question',
    resourceType: 'ai_tutor',
    knowledgeName: videoLearningContext.value?.knowledgeName || currentChapter.value?.title || '',
    extraJson: JSON.stringify({ question: q.slice(0, 200) })
  })
  chatHistory.value.push({ role: 'user', content: q });
  questionText.value = '';
  scrollToBottom();

  const lastAiMessage = chatHistory.value.slice().reverse().find(msg => msg.role === 'ai')?.content || '';
  const qaPrompt = lastAiMessage ? `结合上一次回答继续帮助我：${q}` : q;
  await callAiStream(qaPrompt, 'hint');
};

const callAiStream = async (promptInput: string, mode: TutorMode = 'hint') => {
  const aiMessageIndex = chatHistory.value.push({ role: 'ai', content: '...' }) - 1;
  aiLoading.value = true;
  let isFirstChunk = true;
  let streamCompleted = false;

  try {
    const token = getAuthToken();
    const response = await fetch(`${API_BASE_URL}/ai/tutor/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      credentials: 'include',
      body: JSON.stringify({
        message: promptInput,
        mode,
        context: {
          ...tutorContext.requestContext,
          videoLearning: videoLearningContext.value
        }
      })
    });

    if (!response.ok) throw new Error(`AI 请求失败：${response.status}`);
    if (!response.body) throw new Error('AI 流式响应为空');

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true });
      if (isFirstChunk) {
        chatHistory.value[aiMessageIndex].content = '';
        isFirstChunk = false;
      }
      chatHistory.value[aiMessageIndex].content += chunk;
      scrollToBottom();
    }
    streamCompleted = true;
  } catch (err) {
    chatHistory.value[aiMessageIndex].content = '网络波动，请稍后重试。';
  } finally {
    aiLoading.value = false;
    if (streamCompleted && aiSpeechEnabled.value) {
      speakAiMessage(chatHistory.value[aiMessageIndex].content, aiMessageIndex);
    }
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  });
};

const stopCamera = () => {
  if (timer) {
    window.clearInterval(timer);
    timer = null;
  }
  stopCameraTimer();
  stopReportTimer();
  saveFatigueStats();

  if (cameraVideo.value?.srcObject) {
    const stream = cameraVideo.value.srcObject as MediaStream;
    stream.getTracks().forEach(track => track.stop());
    cameraVideo.value.srcObject = null;
  }

  isCameraOn.value = false;
  isDetecting = false;
};

const startCamera = async () => {
  try {
    stopCamera();
    isCameraOn.value = true;
    await nextTick();

    const stream = await navigator.mediaDevices.getUserMedia({
      video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' },
      audio: false
    });

    if (cameraVideo.value) {
      cameraVideo.value.srcObject = stream;
      await cameraVideo.value.play().catch(() => undefined);
      isFatigue.value = false;
      statusText.value = '分析中...';
      startDetection();
      startCameraTimer();
      startReportTimer();
    }
  } catch (err) {
    isCameraOn.value = false;
    statusText.value = '摄像头开启失败';
    message.error('无法调用摄像头，请允许权限');
  }
};

const startDetection = () => {
  if (timer) window.clearInterval(timer);

  timer = window.setInterval(async () => {
    if (!cameraVideo.value || !canvas.value || isDetecting) return;
    if (cameraVideo.value.readyState < 2) return;

    const context = canvas.value.getContext('2d');
    if (!context) return;

    isDetecting = true;

    try {
      const vw = cameraVideo.value.videoWidth;
      const vh = cameraVideo.value.videoHeight;
      canvas.value.width = vw;
      canvas.value.height = vh;
      context.drawImage(cameraVideo.value, 0, 0, vw, vh);

      const imgData = canvas.value.toDataURL('image/jpeg', 0.5);
      const res = await axios.post(
        FACE_DETECT_API,
        { image: imgData, sessionId: faceSessionId },
        { withCredentials: false, timeout: 5000 }
      );

      const result = res.data || {};
      const status = result.status;
      statusText.value = result.msg || '分析完成';

      const now = Date.now();
      fatigueStats.value.totalDetections++;

      sampleCounter++;
      if (sampleCounter % 10 === 0 && result.ear !== undefined) {
        fatigueStats.value.earSamples.push({ t: now, v: result.ear });
        fatigueStats.value.marSamples.push({ t: now, v: result.mar || 0 });
      }

      if (status === 'normal') fatigueStats.value.normalCount++;

      if (status === 'fatigue' || status === 'yawn') {
        if (prevDetectStatus !== status) {
          if (status === 'yawn') fatigueStats.value.yawnCount++;
          if (status === 'fatigue') fatigueStats.value.fatigueCount++;
          fatigueStats.value.events.push({
            t: now, type: status,
            ear: result.ear, mar: result.mar
          });
          saveFatigueStats();
        }
        if (!isFatigue.value) {
          message.warning(`⚠️ ${result.msg}，注意休息！`);
        }
        isFatigue.value = true;
      } else if (status === 'no_face') {
        if (prevDetectStatus !== 'no_face') {
          fatigueStats.value.noFaceCount++;
          fatigueStats.value.events.push({ t: now, type: 'no_face' });
          saveFatigueStats();
        }
        isFatigue.value = false;
      } else {
        isFatigue.value = false;
      }

      fatigueStats.value.lastStatus = status;
      prevDetectStatus = status;
    } catch (e) {
      isFatigue.value = false;
      statusText.value = '检测服务连接失败';
    } finally {
      isDetecting = false;
    }
  }, 500);
};

// ✅ 替换掉原来第 1891~1893 行的 toggleFavour
const checkFavourStatus = async () => {
  try {
    const data = await request.get<boolean, boolean>(`/favour/check/${courseId}`);
    isFavour.value = !!data;
  } catch (e) {
    isFavour.value = false;
  }
};

const toggleFavour = async () => {
  try {
    const result = await request.post<number, number>('/favour/', { courseId: Number(courseId) });
    // 后端返回 1 = 已收藏，-1 = 已取消
    isFavour.value = result === 1;
    message.success(isFavour.value ? '收藏成功！' : '已取消收藏');
  } catch (e) {
    message.error('操作失败，请稍后重试');
  }
};

const recordHistory = async () => {
  try {
    await request.post('/history/record', { courseId: Number(courseId) });
  } catch (e) {
    // 记录失败不影响主流程，静默处理
    console.warn('记录历史失败', e);
  }
};
// 笔记
const noteContent = ref('');
const noteTitle = ref('');
const noteEditing = ref(false);
const savedNotesList = ref<any[]>([]);
let editingNoteIndex = -1;

const NOTE_KEY = `course_notes_${courseId}`;

const startNewNote = () => {
  noteTitle.value = currentChapter.value?.title || '';
  noteContent.value = '';
  editingNoteIndex = -1;
  noteEditing.value = true;
};
const editNote = (index: number) => {
  const n = savedNotesList.value[index];
  noteTitle.value = n.title || n.chapterTitle || '';
  noteContent.value = n.content;
  editingNoteIndex = index;
  noteEditing.value = true;
};
const cancelEdit = () => {
  noteEditing.value = false;
  noteContent.value = '';
  noteTitle.value = '';
  editingNoteIndex = -1;
};
const saveNote = () => {
  // 校验富文本内容是否为空
  if (!editorHasContent.value) return;

  const all = loadAllNotes();

  // 1. 优化标题生成逻辑：只有新建时才合成 [章节名]
  // 如果当前章节名已经包含了 [1] 这种前缀，避免二次嵌套
  const chapterName = currentChapter.value?.title || '随堂笔记';
  const autoTitle = `[${chapterName}] 随堂笔记`;

  const noteObj = {
    title: autoTitle,
    chapterTitle: currentChapter.value?.title || '', // 仅存原始章节名作为备份
    content: noteContent.value, // 富文本 HTML 内容
    savedAt: Date.now()
  };

  // 2. 处理编辑与新增
  if (editingNoteIndex >= 0) {
    // 编辑时，通常保持原来的 title 不动，只更新内容
    const oldNote = all[editingNoteIndex];
    noteObj.title = oldNote.title;
    all[editingNoteIndex] = noteObj;
  } else {
    // 新增时，推入开头
    all.unshift(noteObj);
  }

  localStorage.setItem(NOTE_KEY, JSON.stringify(all));
  savedNotesList.value = all;

  // 重置编辑器状态
  noteEditing.value = false;
  noteContent.value = '';
  editingNoteIndex = -1;
  message.success('笔记已保存');
};
const deleteNote = (index: number) => {
  const all = loadAllNotes();
  const removed = all.splice(index, 1);
  localStorage.setItem(NOTE_KEY, JSON.stringify(all));
  savedNotesList.value = all;
  message.success(`已删除「${removed[0]?.title || '笔记'}」`);
};
const loadChapterNote = () => {
  savedNotesList.value = loadAllNotes();
};
const loadAllNotes = (): any[] => {
  try {
    const raw = localStorage.getItem(NOTE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch { return []; }
};
const formatTime = (ts: number): string => {
  const d = new Date(ts);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const loadMindmap = async (force = false) => {
  if (!courseId) return;
  if (mindmapLoading.value) return;

  if (!force && mindmapInitialized.value && mindmapData.value) {
    return;
  }

  mindmapInitialized.value = true;
  mindmapLoading.value = true;
  mindmapError.value = '';

  try {
    const data = await getCourseMindmap(courseId as string | number);
    mindmapData.value = data;
  } catch (error: any) {
    mindmapData.value = null;
    mindmapError.value = error?.message || '思维导图加载失败';
  } finally {
    mindmapLoading.value = false;
  }
};

const handleMindmapRetry = () => {
  loadMindmap(true);
};

const handleMindmapRegenerate = async () => {
  if (!courseId || mindmapRegenerating.value) return;

  mindmapInitialized.value = true;
  mindmapRegenerating.value = true;
  mindmapError.value = '';

  try {
    const data = await regenerateCourseMindmap(courseId as string | number);
    mindmapData.value = data;
    message.success('课程导图已重新生成');
  } catch (error: any) {
    mindmapError.value = error?.message || '课程导图重新生成失败';
    message.error(mindmapError.value);
  } finally {
    mindmapRegenerating.value = false;
  }
};

watch(
  () => bottomTab.value,
  (val) => {
    if (val === 'mindmap' && !mindmapInitialized.value) {
      loadMindmap();
    }
  },
  { immediate: true } // 增加此配置：让监听器在绑定时立刻执行一次
);

// 评论
const newDiscussContent = ref('');
const discussList = ref<any[]>([]);

const loadDiscussList = async () => {
  try {
    const data = await request.get<any[], any[]>('/comment/list', {
      params: { courseId }
    })
    reportLearningEvent({
      eventType: 'comment_view',
      resourceType: 'discussion',
      knowledgeName: currentChapter.value?.title || displayCourseTitle.value || ''
    })
    discussList.value = (data || []).map((item: any) => ({
      ...item,
      isLiked: false
    }))
  } catch (error) {
    discussList.value = []
  }
}

const publishDiscuss = async () => {
  if (!newDiscussContent.value.trim()) return

  const payload = {
    courseId,
    userId: getCurrentUserId(),
    userName: userInfo.value?.name || '热心同学',
    userAvatar: displayAvatarUrl.value,
    content: newDiscussContent.value
  }

  try {
    await request.post('/comment/add', payload)
    message.success('观点发表成功！')
    reportLearningEvent({
      eventType: 'comment_post',
      resourceType: 'discussion',
      knowledgeName: currentChapter.value?.title || displayCourseTitle.value || '',
      extraJson: JSON.stringify({ length: newDiscussContent.value.length })
    })
    newDiscussContent.value = ''
    await loadDiscussList()
  } catch (error: any) {
    message.error(error?.message || '发表失败')
  }
}

const toggleDiscussLike = async (index: number) => {
  const discuss = discussList.value[index]
  const targetState = !discuss.isLiked

  try {
    await request.post(`/comment/like/${discuss.id}`, null, {
      params: { isLike: targetState },
      skipSuccessToast: true
    })

    if (targetState) {
      discuss.likes++
      discuss.isLiked = true
    } else {
      discuss.likes--
      discuss.isLiked = false
    }
  } catch (error) {
    message.error('点赞操作失败')
  }
}

const deleteDiscuss = async (index: number) => {
  const discuss = discussList.value[index]
  try {
    await request.delete(`/comment/delete/${discuss.id}`)
    message.success('观点已删除')
    discussList.value.splice(index, 1)
  } catch (error: any) {
    message.error(error?.message || '删除失败')
  }
}

// ✅ 替换掉原来的 onMounted（第 2173~2178 行）
onMounted(async () => {
  syncTutorCourseContext();
  loadFatigueStats();
  await resolveCourseSource();
  fetchCourseDetail();
  await fetchChapters();
  loadDiscussList();
  checkFavourStatus(); // ← 新增：初始化收藏状态
  recordHistory();     // ← 新增：记录学习历史
});

onUnmounted(() => {
  clearPauseHelpTimer();
  flushVideoEvents();
  stopCamera();
  stopAiSpeech();

  // 销毁富文本编辑器实例
  const editor = editorRef.value;
  if (editor == null) return;
  editor.destroy();
});
</script>

<style scoped>
.bilibili-layout-container {
  height: 100vh;
  overflow-y: auto;
  /* 替换为与首页一致的对角线渐变背景 */
  background: linear-gradient(120deg, #FFFFFF 0%, #F1F5F9 100%);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}
.scroll-y::-webkit-scrollbar { width: 6px; }
.scroll-y::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }

.content-wrapper {
  width: 75%;
  max-width: 1600px;
  min-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 60px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.shadow-card { background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.03); border: 1px solid #e2e8f0; overflow: hidden; }

/* ✅ 整体顶部对齐 */
.top-section {
  display: flex;
  position: relative; /* 开启定位锚点 */
  align-items: flex-start;
  height: clamp(480px, calc(100vh - 285px), 590px);
  min-height: 480px;
}
.video-column {
  width: calc(100% - 370px);
  height: 100%;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

/* ✅ 视频播放器包裹，增加悬浮按钮功能 */
.video-player-box {
  width: 100%;
  height: 100%;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}
.main-video {
  width: 100%;
  height: 100%;
  outline: none;
  object-fit: contain;
}

/* 1. 最外层容器化身“独立卡片”，统管标题和右侧按钮 */
.page-top-header {
  display: flex;
  align-items: center; /* 确保内部标题和按钮在同一水平线垂直居中 */
  justify-content: space-between;
  height: 72px; /* 固定整体高度，解决被撑高的问题 */
  padding: 0 24px 0 32px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
  position: relative; /* 左侧蓝条的定位基准 */
  margin-top: 20px; /* 与下方的视频/监控区域拉开安全距离 */
}

/* 2. 卡片左侧的修饰色块 */
.page-top-header::before {
  content: '';
  position: absolute;
  left: -1px;
  top: -1px;
  bottom: -1px;
  width: 8px;
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 10px 0 0 10px;
  box-shadow: 2px 0 8px rgba(59, 130, 246, 0.2);
}

/* 3. 课程标题：剥离框体属性，专注文字居中与单行截断 */
.course-title {
  flex: 1; /* 占据中间全部可用空间 */
  min-width: 0; /* 关键：flex 子元素默认 min-width: auto，会阻止 overflow 生效，必须手动置 0 */
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
  text-align: center; /* 核心：完美水平居中 */

  /* 文本溢出隐藏逻辑 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 24px; /* 防止标题过长时贴脸右侧按钮 */
}

/* 4. 收藏按钮：固定在右侧，防止被超长标题挤压变形 */
.btn-collect-course {
  flex-shrink: 0; /* 绝对关键！保证按钮原尺寸，绝不会因为标题太长被挤扁或挤换行 */

  /* 以下保留原本的精致按钮样式 */
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 18px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
}

.btn-collect-course:hover {
  color: #2563eb;
  border-color: #bfdbfe;
  background: #f8fafc;
  transform: translateY(-1px);
}

.btn-collect-course.active {
  color: #f59e0b;
  border-color: #fde68a;
  background: #fffbeb;
}

.btn-collect-course {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 18px;
  border-radius: 8px; /* 统一使用圆角矩形，更贴合整体表单风格 */
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
}

.btn-collect-course:hover {
  color: #2563eb;
  border-color: #bfdbfe;
  background: #f8fafc;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
}

.btn-collect-course.active {
  color: #f59e0b;
  border-color: #fde68a;
  background: #fffbeb;
}

.btn-collect-course .icon {
  font-size: 16px;
}

.loading-box { color: #94a3b8; display: flex; flex-direction: column; align-items: center; }
.spinner { font-size: 32px; margin-bottom: 12px; color: #3b82f6; }
.empty-tip { display: flex; flex-direction: column; align-items: center; gap: 12px; font-size: 14px; color: #64748b; }
.empty-icon { font-size: 40px; color: #cbd5e1; }

/* ✅ 侧边栏样式，通过 bottom: 0 保证与左侧视频完全对齐 */
.sidebar-column {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0; /* 绝杀：强行拉伸到和左侧一模一样的高度 */
  width: 350px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.fatigue-card {
  flex: 0 0 auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.monitor-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.monitor-title-group { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.m-title { font-size: 14px; font-weight: 700; color: #1e293b; display: flex; align-items: center; }
.monitor-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}
.monitor-policy {
  flex: 0 0 auto;
  padding: 2px 7px;
  border-radius: 5px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.monitor-policy.required {
  background: #eff6ff;
  color: #1d4ed8;
}
.monitor-hint {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}
.monitor-body { display: flex; gap: 12px; align-items: center; padding-top: 4px; border-top: 1px dashed #e2e8f0; }
.camera-mini { width: 64px; height: 40px; background: #000; border-radius: 4px; overflow: hidden; }
.camera-feed { width: 100%; height: 100%; object-fit: cover; transform: scaleX(-1); }
.status-info { flex: 1; display: flex; flex-direction: column; }
.indicator { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #334155; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.dot.green { background: #10b981; }
.dot.red { background: #ef4444; }
.s-text.warning { color: #ef4444; }
.sub-text { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.fatigue-counters { display: flex; gap: 4px; margin-top: 6px; flex-wrap: wrap; }
.counter-tag { font-size: 10px; font-weight: 700; padding: 2px 7px; border-radius: 10px; white-space: nowrap; }
.tag-warn { background: #fef3c7; color: #92400e; }
.tag-danger { background: #fee2e2; color: #991b1b; }
.tag-muted { background: #f1f5f9; color: #475569; }
.tag-ok { background: #dcfce7; color: #166534; }
.tag-blue { background: #dbeafe; color: #1e40af; }

.interaction-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 330px;
}
.custom-tabs-header { display: flex; height: 48px; background: #f8fafc; border-bottom: 1px solid #e2e8f0; }
.tab-item { flex: 1; display: flex; justify-content: center; align-items: center; font-size: 14px; font-weight: 600; color: #64748b; cursor: pointer; position: relative; transition: 0.2s; }
.tab-item:hover { color: #3b82f6; }
.tab-item.active { color: #2563eb; background: #fff; }
.tab-item.active::after { content: ''; position: absolute; bottom: -1px; left: 20%; right: 20%; height: 3px; background: #2563eb; border-radius: 3px 3px 0 0; }
.badge { font-size: 12px; font-weight: 500; color: #94a3b8; background: #e2e8f0; padding: 1px 6px; border-radius: 10px; margin-left: 6px; }
.custom-tabs-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; background: #fff; min-height: 0; }

.chapter-list { flex: 1; padding: 12px; display: flex; flex-direction: column; gap: 6px; }
.ep-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: 0.2s; background: #f8fafc; }
.ep-item:hover { background: #f1f5f9; }
.ep-item.playing { background: #eff6ff; color: #2563eb; }
.ep-left { display: flex; align-items: center; gap: 12px; overflow: hidden; }
.ep-num { font-size: 13px; font-weight: 700; color: #94a3b8; }
.playing .ep-num { color: #3b82f6; }
.ep-name { font-size: 14px; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.playing .ep-name { font-weight: 600; color: #1e3a8a; }

.playing-anim { display: flex; align-items: flex-end; gap: 2px; height: 12px; }
.playing-anim i { width: 3px; background: #3b82f6; border-radius: 2px; animation: eq 1s ease-in-out infinite; }
.playing-anim i:nth-child(1) { height: 60%; animation-delay: 0s; }
.playing-anim i:nth-child(2) { height: 100%; animation-delay: 0.2s; }
.playing-anim i:nth-child(3) { height: 40%; animation-delay: 0.4s; }
@keyframes eq { 0%, 100% { transform: scaleY(0.4); } 50% { transform: scaleY(1); } }

.ai-workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  background: #f8fbff;
}
.ai-companion-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #dbe5f1;
  background: #ffffff;
}
.ai-companion-switch strong {
  color: #1e293b;
  font-size: 13px;
}
.ai-companion-switch p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}
.ai-companion-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  flex-shrink: 0;
}
.ai-companion-controls label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 12px;
  white-space: nowrap;
}
.message-list {
  flex: 1;
  padding: 12px 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #f8fbff;
  overflow-y: auto;
  overflow-x: hidden;
}
.message-list::-webkit-scrollbar { width: 6px; }
.message-list::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
.message-wrapper { display: flex; align-items: flex-start; font-size: 12px; line-height: 1.42; }
.my-msg { flex-direction: row-reverse; }
.avatar {
  width: 26px;
  height: 26px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  margin: 2px 8px 0;
  border-radius: 7px;
  flex-shrink: 0;
}
.ai-avatar {
  background: #eef6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  box-shadow: inset 0 0 0 2px rgba(255,255,255,0.6);
}
.assistant-avatar-mark { font-size: 10px; font-weight: 800; letter-spacing: 0; }
.user-avatar { background: #eef2f7; color: #475569; border: 1px solid #dbe3ee; }
.bubble {
  max-width: calc(100% - 50px);
  padding: 9px 11px;
  border-radius: 8px;
  line-height: 1.42;
  position: relative;
  word-wrap: break-word;
  overflow-wrap: anywhere;
  overflow: hidden;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}
.ai-msg .bubble { padding-right: 34px; background: #fff; color: #334155; border: 1px solid #dbe5f1; }
.my-msg .bubble { background: #1d4ed8; color: #fff; border: 1px solid #1d4ed8; box-shadow: 0 8px 18px rgba(29,78,216,0.16); }
.speech-msg-btn {
  position: absolute;
  right: 8px;
  bottom: 7px;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dbe5f1;
  border-radius: 6px;
  background: #f8fafc;
  color: #64748b;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease, border-color 0.16s ease;
}
.speech-msg-btn:hover,
.speech-msg-btn.active {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #2563eb;
}
.bubble :deep(.markdown-body) {
  font-size: 12px;
  line-height: 1.42;
}
.bubble :deep(.markdown-body > :first-child) {
  margin-top: 0;
}
.bubble :deep(.markdown-body > :last-child) {
  margin-bottom: 0;
}
.bubble :deep(p) {
  margin: 0 0 4px;
}
.ai-msg .pause-help-card {
  border-color: #bfdbfe;
  background: #eff6ff;
}
.pause-help-title {
  margin-bottom: 3px;
  color: #1e3a8a;
  font-weight: 700;
}
.pause-help-meta {
  margin-bottom: 8px;
  color: #475569;
  font-size: 11px;
}
.pause-help-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.input-area { padding: 10px; background: rgba(255,255,255,0.94); border-top: 1px solid #dbe5f1; display: flex; gap: 8px; align-items: flex-end; }
.chat-input { flex: 1; border-radius: 8px; background: #f1f5f9; border: 1px solid transparent; resize: none; padding: 7px 10px; font-size: 12px; }
.chat-input:focus { background: #fff; border-color: #93c5fd; box-shadow: 0 0 0 3px rgba(147,197,253,0.28); }
.send-btn { width: 34px; height: 34px; border-radius: 8px; display: flex; justify-content: center; align-items: center; padding: 0; box-shadow: none; }

/* ================= 底部区域整体优化 ================= */
.bottom-section {
  padding: 10px 32px 20px; /* 缩小底部 padding，把空间留给内部滚动条 */
  background: #fff;
  height: 920px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
}

.bottom-section :deep(.ant-tabs) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
/* 调整底部 Tabs 切换按钮之间的间距 */
.bottom-section :deep(.ant-tabs-tab) {
  margin-right: 35px !important; /* 👈 调整这里的数字！默认大概是 32px，数字越大间距越宽 */
}

/* 顺便：如果你觉得这几个切换按钮的字太小，可以在这里顺便改大 */
.bottom-section :deep(.ant-tabs-tab-btn) {
  font-size: 16px; /* 字体大小 */
  font-weight: 400; /* 字体粗细 */
}

/* 去掉最后一个按钮右边的多余间距 */
.bottom-section :deep(.ant-tabs-tab:last-child) {
  margin-right: 0 !important;
}

/* 锁定 Tab 头部不动，让内容区吸收剩余高度，并独立滚动！ */
.bottom-section :deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
  overflow-y: auto; /* 内容过多时自动出现滚动条 */
  padding-right: 8px; /* 防止滚动条贴着文字 */
}

/* 内部区域专属的美化滚动条 */
.bottom-section :deep(.ant-tabs-content-holder)::-webkit-scrollbar { width: 6px; }
.bottom-section :deep(.ant-tabs-content-holder)::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }

/* 保证里面的面板高度能够100%继承 */
.bottom-section :deep(.ant-tabs-content),
.bottom-section :deep(.ant-tabs-tabpane) {
  height: 100%;
  min-height: 0;
}

/* 1. 交互课件：既然高度锁死了，直接让它 100% 填满，抛弃之前的自适应比例 */
.anim-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  height: 100%; /* 👈 完美填满当前剩余高度 */
}

.anim-tip {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #166534;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 14px;
  flex-shrink: 0; /* 防止顶部提示被挤压 */
}

.anim-tip .tag { background: #22c55e; color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px; margin-right: 8px; font-weight: 600; }

.anim-iframe-box {
  flex: 1;
  background: #fafafa;
  border-radius: 12px;
  overflow: hidden;
  border: 1px dashed #cbd5e1;
  position: relative;
}

/* ↓ 新增：让 AnimationWorkbench 撑满整个 anim-iframe-box */
.anim-iframe-box :deep(.anim-workbench),
.anim-iframe-box :deep(.animation-workbench),
.anim-iframe-box > * {
  height: 100%;
  width: 100%;
}

.empty-anim {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #94a3b8;
  background: #f8fafc;
  gap: 12px;
}

.huge-icon { font-size: 40px; color: #cbd5e1; }

/* 空占位符通用样式优化：没内容时也完全居中撑开 */
.placeholder-box {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  border: 1px dashed #e2e8f0;
  background: #f8fafc;
  border-radius: 12px;
  margin: 0 auto;
}

/* 2. 文本阅读区（笔记、观点分享）：限制最大宽度并居中，拯救大屏阅读体验 */
.note-container,
.discuss-container {
  margin: 0 auto;
  padding: 10px 0 20px; /* 调整内边距适应滚动流 */
}

/* 观点与笔记区 css (原封不动保留) */
.note-container { padding: 20px 0; }
.btn-new-note { display: inline-block; padding: 10px 28px; border: none; border-radius: 8px; background: #3b82f6; color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; margin-bottom: 20px; transition: background 0.15s; }
.btn-new-note:hover { background: #2563eb; }

.note-edit-card { background: #fafafa; border: 1px solid #e5e7eb; border-radius: 10px; padding: 20px; margin-bottom: 20px; }
.note-title-input { width: 100%; height: 40px; border: 1px solid #e5e7eb; border-radius: 6px; padding: 0 14px; font-size: 15px; font-weight: 600; outline: none; margin-bottom: 12px; background: #fff; }
.note-title-input:focus { border-color: #3b82f6; }
.note-textarea { font-family: -apple-system, sans-serif; font-size: 18px; line-height: 1.7; border-radius: 6px; background: #fff; border: 1px solid #e5e7eb; }
.note-textarea:focus { border-color: #3b82f6; }
.note-edit-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
.btn-note-cancel { padding: 8px 20px; border: 1px solid #ddd; border-radius: 6px; background: #fff; color: #666; font-size: 13px; cursor: pointer; }
.btn-note-cancel:hover { border-color: #bbb; }
.btn-note-save { padding: 8px 24px; border: none; border-radius: 6px; background: #3b82f6; color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; }
.btn-note-save:hover { background: #2563eb; }
.btn-note-save:disabled { opacity: 0.4; cursor: not-allowed; }

.note-list { display: flex; flex-direction: column; gap: 16px; }
.note-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 20px 24px; transition: box-shadow 0.15s; }
.note-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.note-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.note-card-header h4 { margin: 0; font-size: 20px; font-weight: 700; color: #1e293b; }
.note-card-time { font-size: 16px; color: #0b0a0a; }
.note-card-body { font-size: 16px; color: #475569; line-height: 1.8; }
.note-card-body :deep(p) { margin-bottom: 6px; }
.note-card-body :deep(h1), .note-card-body :deep(h2), .note-card-body :deep(h3) { margin: 10px 0 6px; color: #1e293b; }
.note-card-body :deep(pre) { background: #1e293b; padding: 10px; border-radius: 6px; color: #e2e8f0; margin: 8px 0; overflow-x: auto; }
.note-card-body :deep(code) { font-family: Consolas, monospace; font-size: 13px; }
.note-card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 5px; /* 按钮之间的间距稍微缩紧一点，因为按钮本身变大了 */
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
}

/* 基础按钮样式：放大字号、加粗、并扩大真实的点击区域 */
.btn-note-edit,
.btn-note-delete {
  background: transparent;
  border: none;
  font-size: 18px; /* 从原先的 13px 放大到 15px，清晰很多 */
  font-weight: 600; /* 加粗，突出交互感 */
  cursor: pointer;
  padding: 6px 16px; /* 👈 核心：增加内边距，让鼠标更容易点中 */
  border-radius: 6px; /* 为悬浮效果准备的圆角 */
  transition: all 0.2s ease;
}
/* 编辑按钮：默认灰蓝，悬浮变亮蓝并带浅蓝背景 */
.btn-note-edit {
  color: #64748b;
}
.btn-note-edit:hover {
  color: #2563eb;
  background: #eff6ff;
}
/* 删除按钮：默认浅红，悬浮变深红并带浅红背景 */
.btn-note-delete {
  color: #f87171; /* 默认状态下颜色不要太刺眼 */
}
.btn-note-delete:hover {
  color: #dc2626;
  background: #fef2f2;
}
.note-empty { text-align: center; padding: 60px 0; color: #94a3b8; display: flex; flex-direction: column; align-items: center; }
.note-empty p { margin: 0; font-size: 14px; }

.discuss-container {
  height: 100%;
  min-height: 0;
  padding: 12px 0 16px;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.discuss-publish { display: flex; gap: 12px; align-items: flex-start; }
.publish-avatar { flex-shrink: 0; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.publish-input-box { flex: 1; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px; transition: all 0.3s; }
.publish-input-box:focus-within { background: #fff; border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }
.discuss-textarea { border: none; background: transparent; font-size: 13px; line-height: 1.5; resize: none; outline: none; box-shadow: none; padding: 0; }
.discuss-textarea:focus { box-shadow: none; }
.publish-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; padding-top: 8px; border-top: 1px dashed #e2e8f0; }
.publish-tip { font-size: 12px; color: #94a3b8; }
.btn-publish { padding: 6px 16px; background: #3b82f6; color: white; border: none; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; transition: 0.2s; }
.btn-publish:hover:not(:disabled) { background: #2563eb; }
.btn-publish:disabled { background: #cbd5e1; cursor: not-allowed; }
.discuss-divider { height: 1px; background: #e2e8f0; margin: 18px 0 14px; }
.discuss-list-section { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 12px; }
.discuss-title { font-size: 15px; font-weight: 700; color: #1e293b; margin: 0; }
.discuss-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding: 0 8px 42px 0;
  box-sizing: border-box;
  scroll-padding-bottom: 42px;
}
.discuss-list::after { content: ''; flex: 0 0 1px; }
.discuss-list::-webkit-scrollbar { width: 6px; }
.discuss-list::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
.discuss-item { display: flex; gap: 10px; }
.item-avatar { flex-shrink: 0; }
.item-main { flex: 1; min-width: 0; border-bottom: 1px solid #f1f5f9; padding-bottom: 10px; }
.item-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 4px; }
.item-author { font-size: 14px; font-weight: 600; color: #475569; }
.item-time { flex-shrink: 0; font-size: 12px; color: #64748b; }
.item-content { font-size: 14px; color: #334155; line-height: 1.55; margin-bottom: 8px; }
.item-content :deep(p) { margin: 0 0 4px; }
.item-footer { display: flex; gap: 16px; align-items: center; }
.action-btn { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #64748b; cursor: pointer; transition: color 0.2s; }
.action-btn:hover { color: #3b82f6; }
.action-btn.liked { color: #f59e0b; font-weight: 600; }
.delete-btn:hover { color: #ef4444; }
.discuss-empty { display: flex; flex-direction: column; align-items: center; padding: 40px 0; color: #94a3b8; }
/* ================= 章节练习（统一扁平化蓝色系 UI） ================= */
.chapter-homework-panel {
  min-height: 200px; /* 减小无意义的占位高度 */
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: 24px 0 30px;
  margin: 0 auto;
}

.chapter-homework-empty-state {
  flex: 1;
  width: 100%;
  min-height: 480px;
}

.chapter-homework-card {
  background: #ffffff; /* 改为纯白，显得更干净 */
  border: 1px solid #e2e8f0;
  border-radius: 8px; /* 略微收敛圆角 */
  padding: 20px 24px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02); /* 极微弱阴影提升质感 */
}

.chapter-homework-head {
  display: flex;
  justify-content: space-between;
  align-items: center; /* 垂直居中对齐 */
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9; /* 增加底部柔和分割线区分层级 */
}

.chapter-homework-title-wrap {
  display: flex;
  align-items: center; /* 标题和副标题放在同一行，省去垂直空间 */
  gap: 12px;
  flex-wrap: wrap;
}

.chapter-homework-title {
  margin: 0;
  font-size: 16px; /* 缩小字号，避免突兀 */
  font-weight: 600;
  color: #0f172a;
}

.chapter-homework-subtitle {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  background: #f1f5f9; /* 副标题变成标签样式 */
  padding: 2px 8px;
  border-radius: 4px;
}

.chapter-homework-status {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

/* 状态标签增加淡色边框更精致 */
.chapter-homework-status.todo { background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe;}
.chapter-homework-status.judging { background: #fff7ed; color: #ea580c; border: 1px solid #fed7aa;}
.chapter-homework-status.completed { background: #ecfdf5; color: #059669; border: 1px solid #a7f3d0;}

/* ★ 核心：将原本 4 个巨大的信息块，压缩成一行紧凑的信息条 ★ */
.chapter-homework-grid {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 24px;
  margin-bottom: 16px;
  background: #f8fafc;
  padding: 10px 16px;
  border-radius: 6px;
}

.chapter-homework-meta {
  display: flex;
  flex-direction: row; /* 从上下堆叠变为左右同行排列 */
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  padding: 0;
}

.meta-label {
  color: #64748b;
  font-size: 13px;
}
.meta-label::after {
  content: "："; /* 利用 CSS 悄悄加个冒号，不用改 HTML */
}

.chapter-homework-meta strong {
  color: #1e293b;
  font-size: 13px;
  font-weight: 600;
}

/* 教师寄语紧凑化 */
.chapter-homework-note {
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: 6px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  display: flex;
  gap: 8px;
}

.note-tag {
  margin: 0;
  font-size: 12px;
  color: #92400e;
  background: #fcd34d;
  border-radius: 4px;
  padding: 2px 6px;
  height: fit-content;
  white-space: nowrap;
}

.note-text {
  color: #78350f;
  line-height: 1.5;
  font-size: 13px;
}

/* ★ 得分大块改为清爽的窄条 ★ */
.chapter-homework-score-box {
  margin-bottom: 16px;
  padding: 10px 16px;
  border-radius: 6px;
  background: #ecfdf5;
  border: 1px dashed #a7f3d0;
  color: #065f46;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
}

.chapter-homework-score-box .score {
  font-size: 18px; /* 收敛原有的超大数字 */
  font-weight: 700;
  margin: 0 4px;
  color: #059669;
}

.chapter-homework-actions {
  display: flex;
  gap: 12px;
  padding-top: 4px;
}

/* 核心：将原有的紫色渐变按钮替换为系统的标准主色按钮 */
.homework-primary-btn {
  height: 38px;
  padding: 0 20px;
  border: none;
  border-radius: 8px;
  background: #3b82f6;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.homework-primary-btn:hover:not(:disabled) {
  background: #2563eb;
}

.homework-primary-btn.ghost {
  background: #fff;
  color: #475569;
  border: 1px solid #cbd5e1;
}
.homework-primary-btn.ghost:hover {
  color: #2563eb;
  border-color: #bfdbfe;
  background: #f8fafc;
}


/* --- 内部练习与报告面板 --- */
.inline-practice-panel,
.inline-report-panel {
  margin-top: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 24px;
}

.inline-practice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.inline-practice-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.inline-practice-header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.inline-practice-header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.inline-question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.inline-question-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
  background: #fff; /* 去除渐变背景，保持干净 */
}

.inline-question-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

/* 题号小标签变更为标准蓝 */
.question-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 4px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.question-type {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.inline-question-stem {
  font-size: 15px;
  color: #1e293b;
  line-height: 1.7;
  margin-bottom: 16px;
  white-space: pre-wrap;
}

.inline-option-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.inline-option-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s ease;
}

.inline-option-item:hover {
  background: #fff;
  border-color: #cbd5e1;
}

/* 选中项变更为标准蓝 */
.inline-option-item.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.inline-option-item input {
  margin-top: 4px;
  cursor: pointer;
}

.option-label {
  min-width: 20px;
  font-weight: 700;
  color: #2563eb; /* 由紫变蓝 */
}

.option-content {
  color: #334155;
  line-height: 1.6;
}

.inline-answer-box { margin-top: 4px; }

.inline-answer-input,
.inline-answer-textarea {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 14px;
  color: #1e293b;
  background: #fff;
  outline: none;
  transition: all 0.2s;
}

.inline-answer-input:focus,
.inline-answer-textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15); /* 统一系统的聚焦阴影 */
}

.inline-answer-textarea {
  resize: vertical;
  min-height: 100px;
}

/* --- 报告面板样式 --- */
.inline-report-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.report-stat-card {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.report-stat-card span {
  color: #64748b;
  font-size: 13px;
}

.report-stat-card strong {
  color: #1e293b;
  font-size: 22px;
  font-weight: 700;
}

.inline-report-markdown {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 18px;
}

.inline-report-detail-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inline-report-detail-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  background: #fff;
}

.detail-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}

.detail-card-body p {
  margin: 6px 0;
  color: #475569;
  line-height: 1.6;
  font-size: 14px;
}

.streaming-report-tip {
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #bfdbfe;
}

@media (max-width: 1200px) {
  .inline-report-summary,
  .chapter-homework-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .inline-practice-header { flex-direction: column; align-items: stretch; }
}


</style>
