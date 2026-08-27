<template>
  <div class="teacher-community-desk">
    <div v-if="isFocusMode" class="focus-status-bar">
      <div class="focus-status-main">
        <span class="focus-status-pill">当前视角</span>
        <div class="focus-status-copy">
          <strong class="focus-status-label">{{ currentFocusLabel }}</strong>
          <p class="focus-status-text">{{ currentFocusDescription }}</p>
        </div>
      </div>
      <button class="btn-ghost" @click="clearFocusView">
        返回全部视图
      </button>
    </div>

    <section v-if="forbiddenAccess" class="page-state-card">
      <div class="page-state-icon">!</div>
      <h3 class="page-state-title">当前账号无权进入社区处理台</h3>
      <p class="page-state-desc">社区处理台仅对教师角色开放，请从教师首页或正确账号进入。</p>
      <button class="btn-light" @click="router.push('/teacher/dashboard')">返回教师首页</button>
    </section>

    <section v-else-if="pageError && !hasDeskData && !loading" class="page-state-card">
      <div class="page-state-icon">!</div>
      <h3 class="page-state-title">社区处理台加载失败</h3>
      <p class="page-state-desc">{{ pageError }}</p>
      <button class="btn-light" @click="fetchDeskData">重新加载</button>
    </section>

    <template v-else>
      <section class="answer-inbox-hero">
        <div class="answer-inbox-title">
          <h1><AuditOutlined /> 社区处理台</h1>
        </div>

        <div class="answer-inbox-metrics">
          <div class="metric-chip urgent">
            <span>待回答</span>
            <strong>{{ stats.openHomeworkCount }}</strong>
          </div>
          <div class="metric-chip">
            <span>历史</span>
            <strong>{{ historyTotal }}</strong>
          </div>
          <div class="metric-chip">
            <span>待精选</span>
            <strong>{{ stats.pendingFeatureCount }}</strong>
          </div>
          <button class="refresh-quiet" :disabled="loading" @click="fetchDeskData">
            <ReloadOutlined />
            {{ loading ? '刷新中' : '刷新' }}
          </button>
        </div>
      </section>

      <section class="answer-inbox-shell">
        <aside class="question-queue">
          <div class="queue-search">
            <input
              v-model="deskKeyword"
              placeholder="搜索标题、课程、学生..."
              @keyup.enter="handleQueueSearch"
            />
            <button :disabled="loading" @click="handleQueueSearch">查询</button>
          </div>

          <div class="queue-tabs">
            <button
              v-for="mode in queueModes"
              :key="mode.key"
              :class="{ active: queueMode === mode.key }"
              @click="switchQueueMode(mode.key)"
            >
              <span>{{ mode.label }}</span>
              <em>{{ getQueueCount(mode.key) }}</em>
            </button>
          </div>

          <div v-if="loading && activeQueue.length === 0" class="queue-state">
            <a-spin />
            <span>正在整理记录...</span>
          </div>

          <div v-else-if="activeQueue.length === 0" class="queue-state empty">
            <MessageOutlined />
            <strong>{{ emptyQueueTitle }}</strong>
            <p>{{ emptyQueueText }}</p>
          </div>

          <div v-else class="queue-list">
            <button
              v-for="item in activeQueue"
              :key="`${item.source}-${item.id}`"
              class="queue-card"
              :class="{ active: String(activeDetail?.id) === String(item.id) }"
              @click="selectQueueItem(item)"
            >
              <div class="queue-card-top">
                <span class="queue-type" :class="item.source">{{ item.badge }}</span>
                <span class="queue-time">{{ item.lastActiveTime }}</span>
              </div>
              <h3>{{ item.title }}</h3>
              <p v-if="item.excerpt">{{ item.excerpt }}</p>
              <div class="queue-meta">
                <span>{{ item.courseName }}</span>
                <span>{{ item.replyCount }} 回复</span>
                <span v-if="item.authorName">{{ item.authorName }}</span>
              </div>
            </button>
          </div>

          <div v-if="queueMode === 'history' && historyTotal > HISTORY_PAGE_SIZE" class="history-pagination">
            <a-pagination
              v-model:current="historyPage"
              :total="historyTotal"
              :page-size="HISTORY_PAGE_SIZE"
              size="small"
              show-less-items
              @change="handleHistoryPageChange"
            />
          </div>
        </aside>

        <main class="answer-reader">
          <div v-if="detailLoading" class="answer-empty">
            <a-spin />
            <span>正在打开问题...</span>
          </div>

          <div v-else-if="!activeDetail" class="answer-empty">
            <MessageOutlined />
            <h2>选择一条记录开始处理</h2>
            <p>左侧可以查询待解决、历史记录和待精选讨论。点开后就能查看详情并直接回复学生。</p>
          </div>

          <template v-else>
            <div class="answer-reader-head">
              <div class="answer-reader-status-row">
                <div class="answer-badges">
                  <span class="course-pill">{{ activeDetail.courseName }}</span>
                  <span v-if="activeDetail.postType === 'homework' && activeDetail.status === 'open'" class="status-pill open">待解决</span>
                  <span v-if="activeDetail.postType === 'homework' && activeDetail.status === 'resolved'" class="status-pill resolved">已解决</span>
                  <span v-if="activeDetail.isTeacherAnswered" class="status-pill teacher">老师已答</span>
                  <span v-if="isFeatured(activeDetail.id)" class="status-pill featured">已精选</span>
                </div>
                <div class="reader-actions">
                  <button
                    v-if="canResolveActiveDetail"
                    class="btn-soft-success"
                    :disabled="String(resolvingId) === String(activeDetail.id)"
                    @click="handleResolve(activeDetail.id)"
                  >
                    {{ String(resolvingId) === String(activeDetail.id) ? '处理中...' : '标记已解决' }}
                  </button>
                  <button
                    v-if="canFeatureActiveDetail"
                    class="btn-soft-warning"
                    :disabled="featureSubmitting"
                    @click="openFeaturedModal()"
                  >
                    加入精选
                  </button>
                </div>
              </div>

              <div class="answer-title-row">
                <h2>{{ activeDetail.title }}</h2>
                <p class="answer-meta">
                  <span>{{ activeDetail.authorName || '匿名同学' }}</span>
                  <span>{{ activeDetail.createdAt }}</span>
                  <span>{{ activeDetail.replyCount > 0 ? `${activeDetail.replyCount} 条回复` : '暂无回复' }}</span>
                </p>
              </div>
            </div>

            <section class="question-paper">
              <h3>学生问题</h3>
              <p v-for="(line, index) in parseParagraphs(activeDetail.content)" :key="index">{{ line }}</p>
            </section>

            <div v-if="activeDetail.replies.length === 0" class="reply-empty-inline">暂无回复</div>

            <section v-else class="reply-thread">
              <div class="reply-thread-title">
                <h3>回复记录</h3>
                <span>{{ activeDetail.replies.length }} 条</span>
              </div>

              <div
                v-for="reply in activeDetail.replies"
                :key="reply.id"
                class="reply-bubble"
                :class="{ teacher: isTeacherReply(reply) }"
              >
                <div class="reply-bubble-head">
                  <strong>{{ reply.authorName || '匿名用户' }}</strong>
                  <div class="reply-bubble-meta-actions">
                    <span>{{ isTeacherReply(reply) ? '教师回复' : '学生追问' }} · {{ reply.createdAt }}</span>
                    <button
                      v-if="canDeleteReply(reply)"
                      class="reply-delete-btn"
                      :disabled="String(deleteReplyLoadingId) === String(reply.id)"
                      @click="handleDeleteReply(reply)"
                    >
                      {{ String(deleteReplyLoadingId) === String(reply.id) ? '删除中...' : '删除' }}
                    </button>
                  </div>
                </div>
                <div class="reply-rich-content" v-html="normalizeRichHtml(reply.content)"></div>
                <button
                  v-if="isTeacherReply(reply) && !isFeatured(activeDetail.id)"
                  class="inline-feature-btn"
                  @click="openFeaturedModal(reply)"
                >
                  将这条回复加入精选
                </button>
              </div>
            </section>

            <section class="reply-composer">
              <label>回复学生</label>
              <div class="reply-editor-shell">
                <Toolbar
                  class="reply-editor-toolbar"
                  :editor="replyEditorRef"
                  :defaultConfig="replyToolbarConfig"
                  mode="default"
                />
                <Editor
                  class="reply-editor-content"
                  v-model="replyContent"
                  :defaultConfig="replyEditorConfig"
                  mode="default"
                  @onCreated="handleReplyEditorCreated"
                />
              </div>
              <div class="composer-actions">
                <span>支持图文混排，单张图片不超过 5MB</span>
                <button class="btn-submit" :disabled="replySubmitting" @click="submitReply">
                  {{ replySubmitting ? '发送中...' : '发送回复' }}
                </button>
              </div>
            </section>
          </template>
        </main>
      </section>

      <section v-if="false" class="overview-grid">
        <div class="overview-card">
          <div class="overview-icon icon-blue">
            <FileTextOutlined />
          </div>
          <div class="overview-body">
            <span class="overview-label">待解决问题</span>
            <strong class="overview-value">{{ stats.openHomeworkCount }}</strong>
            <span class="overview-sub">当前仍处于 open 的作业互助问题</span>
          </div>
        </div>

        <div class="overview-card">
          <div class="overview-icon icon-orange">
            <FireOutlined />
          </div>
          <div class="overview-body">
            <span class="overview-label">今日新增提问</span>
            <strong class="overview-value">{{ stats.todayQuestionCount }}</strong>
            <span class="overview-sub">来自社区概览的今日新增作业提问数</span>
          </div>
        </div>

        <div class="overview-card">
          <div class="overview-icon icon-purple">
            <StarOutlined />
          </div>
          <div class="overview-body">
            <span class="overview-label">待精选讨论</span>
            <strong class="overview-value">{{ stats.pendingFeatureCount }}</strong>
            <span class="overview-sub">已有教师回复，但还未进入精选</span>
          </div>
        </div>

        <div class="overview-card">
          <div class="overview-icon icon-green">
            <CheckCircleOutlined />
          </div>
          <div class="overview-body">
            <span class="overview-label">本周新增精选</span>
            <strong class="overview-value">{{ stats.weeklyFeaturedCount }}</strong>
            <span class="overview-sub">来自社区概览的周新增精选数</span>
          </div>
        </div>
      </section>

      <section v-if="false" class="content-grid">
        <div class="main-column">
          <div
            ref="homeworkSectionRef"
            class="desk-card"
            :class="{
              'section-focused': isPrimarySection('homework'),
              'section-muted': isMutedSection('homework')
            }"
          >
            <div class="section-header">
              <div>
                <h2 class="section-title">
                  {{ homeworkView === 'pending' ? '待解决作业问题' : '历史处理记录' }}
                </h2>
                <p class="section-desc">
                  {{
                    homeworkView === 'pending'
                      ? '仅展示你所教授课程中仍待解决的作业问题'
                      : '回看你所教授课程中已经解决的历史问题'
                  }}
                </p>
                <div v-if="isPrimarySection('homework')" class="section-focus-row">
                  <span class="section-focus-badge">
                    {{ currentFocusSection === 'homework' ? '当前聚焦' : '已定位' }}
                  </span>
                  <span class="focus-tip">
                    {{
                      currentFocusSection === 'homework'
                        ? '当前正在处理待解决作业问题'
                        : '已根据首页摘要定位到待解决作业区域'
                    }}
                  </span>
                </div>
              </div>
              <div class="history-switch" aria-label="作业问题视图">
                <button
                  :class="{ active: homeworkView === 'pending' }"
                  @click="homeworkView = 'pending'"
                >
                  待处理 <span>{{ stats.openHomeworkCount }}</span>
                </button>
                <button
                  :class="{ active: homeworkView === 'history' }"
                  @click="homeworkView = 'history'"
                >
                  历史 <span>{{ historyTotal }}</span>
                </button>
              </div>
            </div>

            <div v-if="loading && displayHomework.length === 0" class="state-block">
              <a-spin />
              <span>正在加载{{ homeworkView === 'pending' ? '待解决问题' : '历史记录' }}...</span>
            </div>

            <a-empty
              v-else-if="displayHomework.length === 0"
              :description="homeworkView === 'pending' ? '当前没有待解决的作业问题' : '暂无历史处理记录'"
            />

            <div v-else class="task-list">
              <div v-for="item in displayHomework" :key="item.id" class="task-item">
                <div class="task-main">
                  <div class="task-top">
                    <h3 class="task-title">{{ item.title }}</h3>
                    <span class="course-pill">{{ item.courseName }}</span>
                    <span class="status-pill" :class="homeworkView === 'pending' ? 'open' : 'resolved'">
                      {{ homeworkView === 'pending' ? '待解决' : '已解决' }}
                    </span>
                  </div>

                  <p v-if="item.excerpt" class="task-excerpt">{{ item.excerpt }}</p>

                  <div class="task-meta">
                    <span>{{ item.authorName || '匿名同学' }}</span>
                    <span>{{ item.replyCount }} 条回复</span>
                    <span>{{ item.lastActiveTime }}</span>
                  </div>
                </div>

                <div class="task-actions">
                  <button
                    v-if="homeworkView === 'pending'"
                    class="btn-soft-success"
                    :disabled="String(resolvingId) === String(item.id)"
                    @click="handleResolve(item.id)"
                  >
                    {{ String(resolvingId) === String(item.id) ? '处理中...' : '标记已解决' }}
                  </button>

                  <button class="btn-soft-primary" @click="openDetail(item.id)">
                    查看详情
                  </button>
                </div>
              </div>
            </div>

            <div v-if="homeworkView === 'history' && historyTotal > HISTORY_PAGE_SIZE" class="history-pagination">
              <a-pagination
                v-model:current="historyPage"
                :total="historyTotal"
                :page-size="HISTORY_PAGE_SIZE"
                size="small"
                show-less-items
                @change="handleHistoryPageChange"
              />
            </div>
          </div>

          <div
            ref="featuredSectionRef"
            class="desk-card"
            :class="{
              'section-focused': isPrimarySection('featured'),
              'section-muted': isMutedSection('featured')
            }"
          >
            <div class="section-header">
              <div>
                <h2 class="section-title">待加入精选的讨论</h2>
                <p class="section-desc">已有教师回复，但尚未沉淀进答疑精选的讨论</p>
                <div v-if="isPrimarySection('featured')" class="section-focus-row">
                  <span class="section-focus-badge">
                    {{ currentFocusSection === 'featured' ? '当前聚焦' : '已定位' }}
                  </span>
                  <span class="focus-tip">
                    {{
                      currentFocusSection === 'featured'
                        ? '当前正在处理待加入精选的讨论'
                        : '已根据首页摘要定位到待精选讨论区域'
                    }}
                  </span>
                </div>
              </div>
              <span class="section-count">{{ featureCandidates.length }} 条</span>
            </div>

            <div v-if="loading && featureCandidates.length === 0" class="state-block">
              <a-spin />
              <span>正在加载待精选讨论...</span>
            </div>

            <div v-else-if="featureCandidates.length === 0" class="empty-guide">
              <div class="empty-guide-icon">
                <StarOutlined />
              </div>
              <h4 class="empty-guide-title">当前没有待加入精选的讨论</h4>
              <p class="empty-guide-desc">
                当学生在社区发帖提问、教师进行回复后，该讨论就会自动出现在这里，等待你审核加入精选。
              </p>
              <details class="empty-guide-details">
                <summary class="empty-guide-summary">想知道如何产生更多待精选讨论？</summary>
                <div class="empty-guide-steps">
                  <div class="guide-step">
                    <span class="guide-step-num">1</span>
                    <span>学生在社区发帖提问（讨论帖或作业互助均可）</span>
                  </div>
                  <div class="guide-step">
                    <span class="guide-step-num">2</span>
                    <span>教师在该帖子下进行回复答疑</span>
                  </div>
                  <div class="guide-step">
                    <span class="guide-step-num">3</span>
                    <span>系统自动标记为"老师已答"，该讨论即出现在此待精选列表中</span>
                  </div>
                  <div class="guide-step">
                    <span class="guide-step-num">4</span>
                    <span>教师在此处审核并"加入精选"，优质内容沉淀到答疑精选板块</span>
                  </div>
                </div>
              </details>
            </div>

            <div v-else class="task-list">
              <div v-for="item in featureCandidates" :key="item.id" class="task-item">
                <div class="task-main">
                  <div class="task-top">
                    <h3 class="task-title">{{ item.title }}</h3>
                    <span class="course-pill">{{ item.courseName }}</span>
                    <span class="status-pill teacher">老师已答</span>
                  </div>

                  <div class="task-meta">
                    <span>{{ item.replyCount }} 条回复</span>
                    <span>{{ item.viewCount }} 次浏览</span>
                    <span>{{ item.lastActiveTime }}</span>
                  </div>
                </div>

                <div class="task-actions">
                  <button
                    class="btn-soft-warning"
                    :disabled="String(featureLoadingId) === String(item.id)"
                    @click="prepareFeature(item.id)"
                  >
                    {{ String(featureLoadingId) === String(item.id) ? '处理中...' : '加入精选' }}
                  </button>

                  <button class="btn-soft-primary" @click="openDetail(item.id)">
                    查看详情
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <aside class="side-column">
          <div class="desk-card side-card" :class="{ 'side-card-focused': isFocusMode }">
            <div class="section-header compact">
              <div>
                <h2 class="section-title">近期关注</h2>
                <p class="section-desc">{{ recentFocusDescription }}</p>
              </div>

              <button v-if="isFocusMode" class="mini-link-btn" @click="clearFocusView">
                全部视图
              </button>
            </div>

            <div v-if="isFocusMode" class="side-focus-banner">
              当前正在关注{{ currentFocusLabel }}
            </div>

            <div v-if="displayRecentFocus.length === 0" class="state-block state-empty compact-empty">
              <a-empty :description="isFocusMode ? '当前视角下暂无需要关注的内容' : '暂无需要关注的内容'" />
            </div>

            <div v-else class="focus-list">
              <div
                v-for="item in displayRecentFocus"
                :key="`${item.kind}-${item.id}`"
                class="focus-item"
                :class="{ 'focus-item-active': isActiveFocusItem(item) }"
                @click="openDetail(item.id)"
              >
                <div class="focus-top">
                  <span class="focus-kind" :class="item.kind">
                    {{ item.kind === 'homework' ? '作业互助' : '待精选' }}
                  </span>
                  <span class="focus-time">{{ item.lastActiveTime }}</span>
                </div>
                <h4 class="focus-title">{{ item.title }}</h4>
                <p class="focus-course">{{ item.courseName }}</p>
              </div>
            </div>
          </div>
        </aside>
      </section>

      <a-drawer
        v-if="activeDetail"
        v-model:open="detailVisible"
        :width="760"
        title="讨论详情"
        destroyOnClose
      >
        <div v-if="detailLoading" class="drawer-state">
          <a-spin />
          <span>正在加载详情...</span>
        </div>

        <div v-else-if="!activeDetail" class="drawer-state">
          <a-empty description="未加载到详情内容" />
        </div>

        <div v-else class="drawer-content">
          <div class="drawer-head">
            <h2 class="drawer-title">{{ activeDetail.title }}</h2>

            <div class="drawer-meta">
              <span class="course-pill">{{ activeDetail.courseName }}</span>
              <span v-if="activeDetail.isTeacherAnswered" class="status-pill teacher">老师已答</span>
              <span
                v-if="activeDetail.postType === 'homework' && activeDetail.status === 'open'"
                class="status-pill open"
              >
                待解决
              </span>
              <span
                v-if="activeDetail.postType === 'homework' && activeDetail.status === 'resolved'"
                class="status-pill resolved"
              >
                已解决
              </span>
              <span
                v-if="isFeatured(activeDetail.id)"
                class="status-pill featured"
              >
                已入选精选
              </span>
              <span class="drawer-meta-text">{{ activeDetail.authorName }}</span>
              <span class="drawer-meta-dot">·</span>
              <span class="drawer-meta-text">{{ activeDetail.createdAt }}</span>
            </div>
          </div>

          <div class="drawer-action-bar">
            <button
              v-if="canResolveActiveDetail"
              class="btn-soft-success"
              :disabled="String(resolvingId) === String(activeDetail.id)"
              @click="handleResolve(activeDetail.id)"
            >
              {{ String(resolvingId) === String(activeDetail.id) ? '处理中...' : '标记已解决' }}
            </button>

            <button
              v-if="canFeatureActiveDetail"
              class="btn-soft-warning"
              :disabled="featureSubmitting"
              @click="openFeaturedModal()"
            >
              {{ featureSubmitting ? '处理中...' : '加入答疑精选' }}
            </button>

            <button
              v-else-if="isFeatured(activeDetail.id)"
              class="btn-soft-disabled"
              disabled
            >
              已加入精选
            </button>
          </div>

          <div class="drawer-body-card">
            <h3 class="block-title">问题内容</h3>
            <div class="drawer-article">
              <p v-for="(line, index) in parseParagraphs(activeDetail.content)" :key="index">
                {{ line }}
              </p>
            </div>

            <div class="drawer-stats">
              <span><EyeOutlined /> {{ activeDetail.viewCount }} 次浏览</span>
              <span><MessageOutlined /> {{ activeDetail.replyCount }} 条回复</span>
              <span><ClockCircleOutlined /> {{ activeDetail.lastActiveTime }}</span>
            </div>
          </div>

          <div class="drawer-body-card">
            <div class="reply-header">
              <h3 class="block-title">回复区</h3>
              <span class="reply-count">{{ activeDetail.replies.length }} 条</span>
            </div>

            <div class="reply-editor">
              <textarea
                v-model="replyContent"
                class="reply-textarea"
                rows="4"
                maxlength="2000"
                placeholder="在这里直接回复同学问题..."
              />
              <div class="reply-actions">
                <span class="reply-tip">提交后会自动刷新当前详情与处理台列表</span>
                <button
                  class="btn-primary"
                  :disabled="replySubmitting"
                  @click="submitReply"
                >
                  {{ replySubmitting ? '提交中...' : '提交回复' }}
                </button>
              </div>
            </div>

            <div v-if="activeDetail.replies.length === 0" class="state-block state-empty compact-empty">
              <a-empty description="暂无回复" />
            </div>

            <div v-else class="reply-list">
              <div
                v-for="reply in activeDetail.replies"
                :key="reply.id"
                class="reply-item"
                :class="{ teacher: isTeacherReply(reply) }"
              >
                <div class="reply-main">
                  <div class="reply-top">
                    <div class="reply-author-wrap">
                      <span class="reply-author">{{ reply.authorName }}</span>
                      <span v-if="isTeacherReply(reply)" class="reply-badge">教师</span>
                      <span class="reply-time">{{ reply.createdAt }}</span>
                    </div>

                    <button
                      v-if="isTeacherReply(reply) && canFeatureActiveDetail"
                      class="text-link"
                      @click="openFeaturedModal(reply)"
                    >
                      设为精选来源
                    </button>
                  </div>

                  <div class="reply-content">
                    <p v-for="(line, index) in parseParagraphs(reply.content)" :key="index">
                      {{ line }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </a-drawer>

      <a-modal
        v-model:open="featuredVisible"
        title="加入答疑精选"
        :footer="null"
        :maskClosable="!featureSubmitting"
        destroyOnClose
      >
        <div class="featured-form">
          <div class="form-item">
            <label class="form-label">精选来源</label>
            <select v-model="featuredForm.replyId" class="form-select" @change="handleFeaturedReplyChange">
              <option value="">基于当前帖子正文</option>
              <option
                v-for="reply in teacherReplies"
                :key="reply.id"
                :value="String(reply.id)"
              >
                基于教师回复：{{ reply.authorName }} - {{ reply.createdAt }}
              </option>
            </select>
          </div>

          <div class="form-item">
            <label class="form-label">精选摘要</label>
            <textarea
              v-model="featuredForm.excerpt"
              class="form-textarea"
              rows="5"
              maxlength="300"
              placeholder="请输入展示在答疑精选中的摘要内容"
            />
          </div>

          <label class="featured-check">
            <input v-model="featuredForm.isRecommended" type="checkbox" />
            <span>设为推荐</span>
          </label>

          <div class="form-actions">
            <button class="btn-cancel" :disabled="featureSubmitting" @click="featuredVisible = false">
              取消
            </button>
            <button class="btn-submit" :disabled="featureSubmitting" @click="submitFeatured">
              {{ featureSubmitting ? '提交中...' : '加入精选' }}
            </button>
          </div>
        </div>
      </a-modal>
    </template>
  </div>
</template>

<script setup lang="ts">
import { getLoginUserRaw } from '@/utils/authStorage'
import '@wangeditor/editor/dist/css/style.css'
// @ts-ignore
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  AuditOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  EyeOutlined,
  FileTextOutlined,
  FireOutlined,
  MessageOutlined,
  ReloadOutlined,
  StarOutlined
} from '@ant-design/icons-vue'

import {
  addCommunityReply,
  addFeaturedAnswer,
  deleteCommunityReply,
  getCommunityOverview,
  getDiscussionDetail,
  getFeaturedAnswersList,
  getTeacherDiscussionList,
  getTeacherHomeworkHelpList,
  resolveCommunityPost,
  uploadCommunityReplyImage
} from '@/api/community'

import type {
  DiscussionDetail,
  DiscussionItem,
  DiscussionReply,
  HomeworkQuestionItem
} from '@/types/community'

type FocusItem = {
  id: number | string
  title: string
  courseName: string
  lastActiveTime: string
  lastActiveTimestamp?: number
  kind: 'homework' | 'discussion'
}

type DeskFocus = 'homework' | 'featured' | 'overview' | ''
type HighlightSection = 'homework' | 'featured' | ''
type QueueMode = 'pending' | 'history' | 'featured' | 'discussion'

type QueueItem = {
  id: number | string
  title: string
  courseName: string
  lastActiveTime: string
  replyCount: number
  viewCount?: number
  authorName?: string
  excerpt?: string
  badge: string
  source: QueueMode
}

const HOMEWORK_FETCH_SIZE = 20
const DISCUSSION_FETCH_SIZE = 100
const FEATURED_FETCH_SIZE = 100
const HISTORY_PAGE_SIZE = 6

const route = useRoute()
const router = useRouter()



const homeworkSectionRef = ref<HTMLElement | null>(null)
const featuredSectionRef = ref<HTMLElement | null>(null)

const activeHighlightSection = ref<HighlightSection>('')

let sectionHighlightTimer: number | null = null

const currentFocus = computed<DeskFocus>(() => {
  const raw = String(route.query.focus || '')
  if (raw === 'homework' || raw === 'featured' || raw === 'overview') {
    return raw
  }
  return ''
})

const currentFocusSection = computed<HighlightSection>(() => {
  if (currentFocus.value === 'homework' || currentFocus.value === 'featured') {
    return currentFocus.value
  }
  return ''
})

const isFocusMode = computed(() => !!currentFocusSection.value)

const currentFocusLabel = computed(() => {
  if (currentFocusSection.value === 'homework') return '待解决作业问题'
  if (currentFocusSection.value === 'featured') return '待加入精选的讨论'
  return '全部视图'
})

const currentFocusDescription = computed(() => {
  if (currentFocusSection.value === 'homework') {
    return '当前从 Dashboard 带着待解决作业视角进入处理台，页面会优先强调作业问题与相关近期关注。'
  }
  if (currentFocusSection.value === 'featured') {
    return '当前从 Dashboard 带着待精选讨论视角进入处理台，页面会优先强调精选候选与相关近期关注。'
  }
  return '已恢复默认视图。'
})

const recentFocusDescription = computed(() => {
  if (currentFocusSection.value === 'homework') {
    return '当前只看与待解决作业相关的近期内容'
  }
  if (currentFocusSection.value === 'featured') {
    return '当前只看与待精选讨论相关的近期内容'
  }
  return '按最近活跃时间整理'
})

const loading = ref(false)
const forbiddenAccess = ref(false)
const pageError = ref('')

const stats = ref({
  openHomeworkCount: 0,
  todayQuestionCount: 0,
  pendingFeatureCount: 0,
  weeklyFeaturedCount: 0
})

const pendingHomework = ref<HomeworkQuestionItem[]>([])
const resolvedHomework = ref<HomeworkQuestionItem[]>([])
const homeworkView = ref<'pending' | 'history'>('pending')
const queueMode = ref<QueueMode>('pending')
const deskKeyword = ref('')
const historyPage = ref(1)
const historyTotal = ref(0)
const featureCandidates = ref<DiscussionItem[]>([])
const allDiscussions = ref<DiscussionItem[]>([])
const discussionTotal = ref(0)
const recentFocus = ref<FocusItem[]>([])

const queueModes: Array<{ key: QueueMode; label: string }> = [
  { key: 'pending', label: '待解决' },
  { key: 'history', label: '历史记录' },
  { key: 'featured', label: '待精选' },
  { key: 'discussion', label: '全部讨论' }
]

const displayRecentFocus = computed(() => {
  if (currentFocusSection.value === 'homework') {
    return recentFocus.value.filter(item => item.kind === 'homework')
  }
  if (currentFocusSection.value === 'featured') {
    return recentFocus.value.filter(item => item.kind === 'discussion')
  }
  return recentFocus.value
})

const displayHomework = computed(() => {
  return homeworkView.value === 'pending' ? pendingHomework.value : resolvedHomework.value
})

const activeQueue = computed<QueueItem[]>(() => {
  const keyword = deskKeyword.value.trim().toLowerCase()
  const list = (() => {
    if (queueMode.value === 'pending') {
      return pendingHomework.value.map(item => toQueueItem(item, 'pending', '待解决'))
    }
    if (queueMode.value === 'history') {
      return resolvedHomework.value.map(item => toQueueItem(item, 'history', '已解决'))
    }
    if (queueMode.value === 'featured') {
      return featureCandidates.value.map(item => toQueueItem(item, 'featured', '待精选'))
    }
    return allDiscussions.value.map(item => toQueueItem(item, 'discussion', item.isTeacherAnswered ? '老师已答' : '讨论'))
  })()

  if (!keyword) return list

  return list.filter(item => {
    return [
      item.title,
      item.courseName,
      item.authorName,
      item.excerpt
    ].some(value => String(value || '').toLowerCase().includes(keyword))
  })
})

const emptyQueueTitle = computed(() => {
  if (deskKeyword.value.trim()) return '没有找到匹配记录'
  if (queueMode.value === 'pending') return '当前没有待解决问题'
  if (queueMode.value === 'history') return '暂无历史处理记录'
  if (queueMode.value === 'featured') return '当前没有待精选讨论'
  return '暂无讨论记录'
})

const emptyQueueText = computed(() => {
  if (deskKeyword.value.trim()) return '换个关键词试试，或清空搜索查看全部记录。'
  if (queueMode.value === 'pending') return '本班课程暂无需要立即处理的作业问题。'
  if (queueMode.value === 'history') return '处理过的问题会沉淀在这里，方便后续复盘。'
  if (queueMode.value === 'featured') return '教师回复后的优质讨论会出现在这里，方便加入精选。'
  return '学生讨论会按最近活跃时间显示。'
})

const featuredDiscussionIds = ref(new Set<string>())

const detailVisible = ref(false)
const detailLoading = ref(false)
const activeDetail = ref<DiscussionDetail | null>(null)

const replyContent = ref('')
const replySubmitting = ref(false)
const deleteReplyLoadingId = ref<number | string | null>(null)
const replyEditorRef = shallowRef<any>()
const replyToolbarConfig = {
  toolbarKeys: [
    'bold',
    'italic',
    'underline',
    'through',
    'color',
    'bgColor',
    'bulletedList',
    'numberedList',
    'blockquote',
    'codeBlock',
    'insertLink',
    'uploadImage'
  ]
}
const replyEditorConfig: any = {
  placeholder: '写下解题思路、关键步骤或下一步建议，可插入图片说明…',
  maxLength: 10000,
  MENU_CONF: {
    uploadImage: {
      maxFileSize: 5 * 1024 * 1024,
      maxNumberOfFiles: 5,
      allowedFileTypes: ['image/jpeg', 'image/png', 'image/gif'],
      async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
        try {
          const url = await uploadCommunityReplyImage(file)
          insertFn(url, file.name || '回复图片', url)
        } catch (error) {
          message.error(extractErrorMessage(error, '图片上传失败'))
          throw error
        }
      }
    }
  }
}

function handleReplyEditorCreated(editor: any) {
  replyEditorRef.value = editor
}

const resolvingId = ref<number | string | null>(null)
const featureLoadingId = ref<number | string | null>(null)

const featuredVisible = ref(false)
const featureSubmitting = ref(false)
const featuredForm = ref({
  replyId: '',
  excerpt: '',
  isRecommended: true
})

const loginUser = computed(() => {
  try {
    const raw = getLoginUserRaw()
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
})

const isTeacherRole = computed(() => loginUser.value?.userRole === 'teacher')

const currentTeacherId = computed(() => loginUser.value?.id ?? loginUser.value?.userId)
const currentTeacherName = computed(() => loginUser.value?.userName ?? loginUser.value?.username)

const teacherReplies = computed(() => {
  return (activeDetail.value?.replies || []).filter(item => isTeacherReply(item))
})

const hasDeskData = computed(() => {
  return stats.value.openHomeworkCount > 0
    || stats.value.pendingFeatureCount > 0
    || stats.value.weeklyFeaturedCount > 0
    || stats.value.todayQuestionCount > 0
    || pendingHomework.value.length > 0
    || featureCandidates.value.length > 0
    || recentFocus.value.length > 0
})

const canResolveActiveDetail = computed(() => {
  return !!activeDetail.value
    && activeDetail.value.postType === 'homework'
    && activeDetail.value.status === 'open'
})

const canFeatureActiveDetail = computed(() => {
  return !!activeDetail.value
    && !!activeDetail.value.isTeacherAnswered
    && !isFeatured(activeDetail.value.id)
})

function extractErrorMessage(error: unknown, fallback = '请求失败') {
  if (!error) return fallback
  if (error instanceof Error) return error.message || fallback
  if (typeof error === 'string') return error
  return fallback
}

function isAuthError(error: unknown) {
  const msg = extractErrorMessage(error, '')
  return /请先登录|未登录|登录失效|无权限|权限不足|仅教师/.test(msg)
}

function isFeatured(id: number | string) {
  return featuredDiscussionIds.value.has(String(id))
}

function isPrimarySection(section: HighlightSection) {
  return currentFocusSection.value === section || activeHighlightSection.value === section
}

function isMutedSection(section: HighlightSection) {
  return !!currentFocusSection.value && currentFocusSection.value !== section
}

function isActiveFocusItem(item: FocusItem) {
  if (currentFocusSection.value === 'homework') return item.kind === 'homework'
  if (currentFocusSection.value === 'featured') return item.kind === 'discussion'
  return false
}

function toQueueItem(
  item: HomeworkQuestionItem | DiscussionItem,
  source: QueueMode,
  badge: string
): QueueItem {
  const raw = item as any
  return {
    id: item.id,
    title: item.title,
    courseName: item.courseName,
    lastActiveTime: item.lastActiveTime,
    replyCount: item.replyCount || 0,
    viewCount: raw.viewCount,
    authorName: raw.authorName,
    excerpt: raw.excerpt,
    source,
    badge
  }
}

function getQueueCount(mode: QueueMode) {
  if (mode === 'pending') return stats.value.openHomeworkCount
  if (mode === 'history') return historyTotal.value
  if (mode === 'featured') return stats.value.pendingFeatureCount
  return discussionTotal.value
}

async function switchQueueMode(mode: QueueMode) {
  queueMode.value = mode
  homeworkView.value = mode === 'history' ? 'history' : 'pending'

  if (mode === 'history' && resolvedHomework.value.length === 0 && historyTotal.value > 0) {
    await handleHistoryPageChange(historyPage.value)
    return
  }

  await nextTick()
  const first = activeQueue.value[0]
  if (first) {
    await selectQueueItem(first)
  } else {
    activeDetail.value = null
  }
}

async function handleQueueSearch() {
  historyPage.value = 1
  await fetchDeskData()
}

async function selectQueueItem(item: QueueItem) {
  if (String(activeDetail.value?.id) === String(item.id)) return
  await openDetail(item.id)
}

function clearFocusView() {
  clearSectionHighlightTimer()
  activeHighlightSection.value = ''

  const nextQuery = { ...route.query }
  delete nextQuery.focus

  router.replace({
    path: route.path,
    query: nextQuery
  })
}

function parseParagraphs(text?: string) {
  if (!text) return []
  return text
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)
}

function escapeHtml(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

function toPlainText(content?: string) {
  const raw = String(content || '')
  if (!raw) return ''
  const node = document.createElement('div')
  node.innerHTML = raw
  return node.textContent || ''
}

function normalizeRichHtml(content?: string) {
  const raw = String(content || '').trim()
  if (!raw) return ''
  if (/<\/?[a-z][\s\S]*>/i.test(raw)) return raw
  return raw
    .split('\n')
    .map(line => `<p>${escapeHtml(line)}</p>`)
    .join('')
}

function hasRichTextContent(content: string) {
  return /<img\b/i.test(content) || !!toPlainText(content).trim()
}

function getCurrentUserId() {
  try {
    const raw = getLoginUserRaw()
    if (!raw) return null
    const user = JSON.parse(raw)
    return user?.id ?? user?.userId ?? user?.ID ?? null
  } catch {
    return null
  }
}

const currentLoginUserId = computed(() => getCurrentUserId())

function isTeacherReply(reply: DiscussionReply) {
  return Boolean(reply && (reply.isTeacher ?? reply.teacher))
}

function canDeleteReply(reply: DiscussionReply) {
  if (!isTeacherReply(reply)) return false
  if (reply.userId == null || currentLoginUserId.value == null) return true
  return String(reply.userId) === String(currentLoginUserId.value)
}

function buildExcerptFromText(text: string, max = 120) {
  const clean = toPlainText(text).replace(/\n+/g, ' ').trim()
  return clean.length > max ? `${clean.slice(0, max)}...` : clean
}

function buildRecentFocus(
  homeworkList: HomeworkQuestionItem[],
  candidateList: DiscussionItem[]
) {
  const homeworkItems: FocusItem[] = homeworkList.map(item => ({
    id: item.id,
    title: item.title,
    courseName: item.courseName,
    lastActiveTime: item.lastActiveTime,
    lastActiveTimestamp: item.lastActiveTimestamp,
    kind: 'homework'
  }))

  const discussionItems: FocusItem[] = candidateList.map(item => ({
    id: item.id,
    title: item.title,
    courseName: item.courseName,
    lastActiveTime: item.lastActiveTime,
    lastActiveTimestamp: item.lastActiveTimestamp,
    kind: 'discussion'
  }))

  return [...homeworkItems, ...discussionItems]
    .sort((a, b) => Number(b.lastActiveTimestamp || 0) - Number(a.lastActiveTimestamp || 0))
    .slice(0, 6)
}

function clearSectionHighlightTimer() {
  if (sectionHighlightTimer !== null) {
    window.clearTimeout(sectionHighlightTimer)
    sectionHighlightTimer = null
  }
}

function triggerSectionHighlight(section: HighlightSection) {
  if (!section) return

  clearSectionHighlightTimer()
  activeHighlightSection.value = section

  sectionHighlightTimer = window.setTimeout(() => {
    activeHighlightSection.value = ''
    sectionHighlightTimer = null
  }, 2200)
}

async function applyRouteFocus() {
  const focus = currentFocus.value

  await nextTick()

  if (!focus) {
    activeHighlightSection.value = ''
    return
  }

  if (focus === 'homework' && homeworkSectionRef.value) {
    homeworkSectionRef.value.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
    triggerSectionHighlight('homework')
    return
  }

  if (focus === 'featured' && featuredSectionRef.value) {
    featuredSectionRef.value.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
    triggerSectionHighlight('featured')
    return
  }

  if (focus === 'overview') {
    activeHighlightSection.value = ''
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    })
  }
}

async function fetchDeskData() { // <--- 这里补上左大括号 {
  if (!isTeacherRole.value) {
    forbiddenAccess.value = true
    pageError.value = ''
    return
  }

  loading.value = true
  pageError.value = ''
  forbiddenAccess.value = false

  try {
    const [overviewData, homeworkData, historyData, discussionData, featuredData] = await Promise.all([
      getCommunityOverview(),
      getTeacherHomeworkHelpList({
        page: 1,
        pageSize: HOMEWORK_FETCH_SIZE,
        status: 'open',
        keyword: deskKeyword.value.trim()
      }),
      getTeacherHomeworkHelpList({
        page: historyPage.value,
        pageSize: HISTORY_PAGE_SIZE,
        status: 'resolved',
        keyword: deskKeyword.value.trim()
      }),
      getTeacherDiscussionList({
        page: 1,
        pageSize: DISCUSSION_FETCH_SIZE,
        sort: 'latest',
        keyword: deskKeyword.value.trim()
      }),
      getFeaturedAnswersList({
        page: 1,
        pageSize: FEATURED_FETCH_SIZE,
        sort: 'latest'
      })
    ])

    const homeworkRecords = homeworkData.records || []
    const discussionRecords = discussionData.records || []
    const featuredRecords = featuredData.records || []

    const featuredIdSet = new Set(
      featuredRecords.map(item => String(item.discussionId))
    )
    featuredDiscussionIds.value = featuredIdSet

    const pendingFeatureAll = discussionRecords.filter(item => {
      return !!item.isTeacherAnswered && !featuredIdSet.has(String(item.id))
    })

    pendingHomework.value = homeworkRecords.slice(0, 6)
    resolvedHomework.value = historyData.records || []
    historyTotal.value = historyData.total || 0
    allDiscussions.value = discussionRecords
    discussionTotal.value = discussionData.total || discussionRecords.length
    featureCandidates.value = pendingFeatureAll.slice(0, 6)
    recentFocus.value = buildRecentFocus(homeworkRecords, pendingFeatureAll)
    // featureCandidates.value = [
    //   {
    //     id: 'mock_feat_1',
    //     title: '在配置VLAN和OSPF时，将IP尾数按照要求设置为06和09后，为什么无法建立邻居关系？',
    //     courseName: '网络工程实践',
    //     replyCount: 8,
    //     viewCount: 215,
    //     lastActiveTime: '15分钟前',
    //     lastActiveTimestamp: Date.now() - 15 * 60 * 1000,
    //     isTeacherAnswered: true
    //   },
    //   {
    //     id: 'mock_feat_2',
    //     title: '在Python中处理CPU密集型任务时，多线程(threading)和多进程(multiprocessing)到底该怎么选？为什么我用了多线程反而变慢了？',
    //     courseName: 'Python核心进阶实战',
    //     replyCount: 23,
    //     viewCount: 856,
    //     lastActiveTime: '2小时前',
    //     lastActiveTimestamp: Date.now() - 120 * 60 * 1000,
    //     isTeacherAnswered: true
    //   }
    // ]
    //
    // // 把假数据也混入侧边栏的“近期关注”中，方便一起测试右侧联动
    // recentFocus.value = buildRecentFocus(
    //   homeworkRecords,
    //   [...pendingFeatureAll, ...featureCandidates.value]
    // )

    stats.value = {
      openHomeworkCount: homeworkData.total || homeworkRecords.length,
      todayQuestionCount: overviewData?.homeworkHelp?.todayQuestionCount || 0,
      pendingFeatureCount: pendingFeatureAll.length,
      weeklyFeaturedCount: overviewData?.featuredAnswers?.weeklySelectedCount || 0
    }

    if (queueMode.value === 'pending' && (homeworkData.total || homeworkRecords.length) === 0 && historyTotal.value > 0) {
      queueMode.value = 'history'
      homeworkView.value = 'history'
    }

    await nextTick()
    const first = activeQueue.value[0]
    if (first && (!activeDetail.value || !activeQueue.value.some(item => String(item.id) === String(activeDetail.value?.id)))) {
      await loadDetail(first.id)
    }

    await applyRouteFocus()
  } catch (error) {
    console.error('[TeacherCommunityDesk] 加载失败', error)

    if (isAuthError(error)) {
      forbiddenAccess.value = true
      pageError.value = '当前账号无权访问社区处理台'
    } else {
      pageError.value = extractErrorMessage(error, '社区处理台加载失败，请稍后重试')
      message.error(pageError.value)
    }
  } finally {
    loading.value = false
  }
}

async function handleHistoryPageChange(page: number) {
  historyPage.value = page
  try {
    const data = await getTeacherHomeworkHelpList({
      page,
      pageSize: HISTORY_PAGE_SIZE,
      status: 'resolved',
      keyword: deskKeyword.value.trim()
    })
    resolvedHomework.value = data.records || []
    historyTotal.value = data.total || 0
  } catch (error) {
    message.error(extractErrorMessage(error, '历史记录加载失败'))
  }
}

// const MOCK_DETAILS: Record<string, DiscussionDetail> = {
//   mock_feat_1: {
//     id: 'mock_feat_1',
//     title: '在配置VLAN和OSPF时，将IP尾数按照要求设置为06和09后，为什么无法建立邻居关系？',
//     content: '按照实验手册要求，将路由器接口IP尾数分别配置为.06和.09，但OSPF邻居表始终无法建立，show ip ospf neighbor 输出为空。已确认OSPF进程号和区域一致，防火墙也已关闭，不知道是哪里出了问题。',
//     courseName: '网络工程实践',
//     postType: 'discussion',
//     status: 'open',
//     authorName: '同学A',
//     createdAt: '2026-04-14 09:10',
//     replyCount: 8,
//     viewCount: 215,
//     lastActiveTime: '15分钟前',
//     isTeacherAnswered: true,
//     replies: [
//       {
//         id: 'mock_feat_1_r1',
//         authorName: '王老师',
//         isTeacher: true,
//         createdAt: '2026-04-14 09:45',
//         content: '问题出在子网掩码上。.06和.09如果使用/29掩码，这两个地址不在同一广播域，OSPF hello包无法互达。建议改用/24或检查掩码配置是否与对端一致。'
//       },
//       {
//         id: 'mock_feat_1_r2',
//         authorName: '同学A',
//         isTeacher: false,
//         createdAt: '2026-04-14 10:02',
//         content: '确实是掩码问题，改成/24后邻居关系正常建立了，感谢老师！'
//       }
//     ]
//   },
//   mock_feat_2: {
//     id: 'mock_feat_2',
//     title: '在Python中处理CPU密集型任务时，多线程(threading)和多进程(multiprocessing)到底该怎么选？为什么我用了多线程反而变慢了？',
//     content: '我写了一个图像批处理脚本，用threading开了8个线程并行处理，结果比单线程还慢。换成multiprocessing后速度明显提升。想搞清楚背后的原因，以及以后该怎么判断用哪个。',
//     courseName: 'Python核心进阶实战',
//     postType: 'discussion',
//     status: 'open',
//     authorName: '同学B',
//     createdAt: '2026-04-14 07:00',
//     replyCount: 23,
//     viewCount: 856,
//     lastActiveTime: '2小时前',
//     isTeacherAnswered: true,
//     replies: [
//       {
//         id: 'mock_feat_2_r1',
//         authorName: '李老师',
//         isTeacher: true,
//         createdAt: '2026-04-14 07:30',
//         content: '核心原因是GIL（全局解释器锁）。CPython同一时刻只允许一个线程执行Python字节码，CPU密集型任务多线程不但没有并行，反而因为频繁的锁竞争和上下文切换产生额外开销。多进程每个进程有独立GIL，才能真正利用多核。IO密集型任务（网络请求、文件读写）线程会主动释放GIL等待，多线程反而合适。记住这个原则：CPU密集用多进程，IO密集用多线程。'
//       }
//     ]
//   }
// }

async function loadDetail(id: number | string) {
  detailLoading.value = true
  activeDetail.value = null

  try {

    const data = await getDiscussionDetail(id)
    activeDetail.value = data
  } catch (error) {
    console.error('[TeacherCommunityDesk] 详情加载失败', error)
    message.error(extractErrorMessage(error, '加载详情失败'))
  } finally {
    detailLoading.value = false
  }
}

async function openDetail(id: number | string) {
  detailVisible.value = false
  replyContent.value = ''
  await loadDetail(id)
}

async function handleResolve(id: number | string) {
  resolvingId.value = id
  try {
    await resolveCommunityPost(id)
    message.success('已标记为已解决')
    await fetchDeskData()
  } catch (error) {
    console.error('[TeacherCommunityDesk] 标记已解决失败', error)
    message.error(extractErrorMessage(error, '操作失败'))
  } finally {
    resolvingId.value = null
  }
}
async function prepareFeature(id: number | string) {
  featureLoadingId.value = id
  try {
    if (!detailVisible.value || String(activeDetail.value?.id) !== String(id)) {
      detailVisible.value = true
      await loadDetail(id)
    }

    if (!activeDetail.value) return

    if (isFeatured(activeDetail.value.id)) {
      message.info('该讨论已加入答疑精选')
      return
    }

    openFeaturedModal()
  } catch (error) {
    console.error('[TeacherCommunityDesk] 打开精选弹窗失败', error)
    message.error(extractErrorMessage(error, '加载精选信息失败'))
  } finally {
    featureLoadingId.value = null
  }
}

function openFeaturedModal(reply?: DiscussionReply) {
  if (!activeDetail.value) return

  featuredForm.value.replyId = reply ? String(reply.id) : ''
  featuredForm.value.excerpt = reply
    ? buildExcerptFromText(reply.content, 120)
    : buildExcerptFromText(activeDetail.value.content || activeDetail.value.title, 120)
  featuredForm.value.isRecommended = true
  featuredVisible.value = true
}

function handleFeaturedReplyChange() {
  if (!activeDetail.value) return

  const replyId = featuredForm.value.replyId
  if (!replyId) {
    featuredForm.value.excerpt = buildExcerptFromText(
      activeDetail.value.content || activeDetail.value.title,
      120
    )
    return
  }

  const targetReply = teacherReplies.value.find(item => String(item.id) === String(replyId))
  if (targetReply) {
    featuredForm.value.excerpt = buildExcerptFromText(targetReply.content, 120)
  }
}

async function submitFeatured() {
  if (!activeDetail.value?.id) return

  if (!currentTeacherId.value || !currentTeacherName.value) {
    message.warning('未获取到当前教师信息')
    return
  }

  const excerpt = featuredForm.value.excerpt.trim()
  if (!excerpt) {
    message.warning('请输入精选摘要')
    return
  }

  featureSubmitting.value = true
  try {
    await addFeaturedAnswer({
      postId: activeDetail.value.id,
      replyId: featuredForm.value.replyId || undefined,
      teacherId: currentTeacherId.value,
      teacherName: currentTeacherName.value,
      excerpt,
      isRecommended: featuredForm.value.isRecommended
    })

    featuredDiscussionIds.value.add(String(activeDetail.value.id))
    featuredVisible.value = false
    message.success('已加入答疑精选')
    await fetchDeskData()
    await loadDetail(activeDetail.value.id)
  } catch (error) {
    console.error('[TeacherCommunityDesk] 加入精选失败', error)
    message.error(extractErrorMessage(error, '加入精选失败'))
  } finally {
    featureSubmitting.value = false
  }
}

async function submitReply() {
  if (!activeDetail.value?.id) return

  const content = replyContent.value.trim()
  if (!hasRichTextContent(content)) {
    message.warning('请输入回复内容')
    return
  }
  if (content.length > 60000) {
    message.warning('回复内容过长，请减少文字或图片数量')
    return
  }

  replySubmitting.value = true
  try {
    await addCommunityReply({
      postId: activeDetail.value.id,
      content
    })

    replyContent.value = ''
    message.success('回复成功')
    await loadDetail(activeDetail.value.id)
    await fetchDeskData()
  } catch (error) {
    console.error('[TeacherCommunityDesk] 回复失败', error)
    message.error(extractErrorMessage(error, '回复失败'))
  } finally {
    replySubmitting.value = false
  }
}

async function deleteReply(reply: DiscussionReply) {
  if (!activeDetail.value?.id || !reply?.id) return

  deleteReplyLoadingId.value = reply.id
  try {
    await deleteCommunityReply(reply.id)
    message.success('回复已删除')
    const activeId = activeDetail.value.id
    await loadDetail(activeId)
    await fetchDeskData()
  } catch (error) {
    console.error('[TeacherCommunityDesk] 删除回复失败', error)
    message.error(extractErrorMessage(error, '删除回复失败'))
    throw error
  } finally {
    deleteReplyLoadingId.value = null
  }
}

function handleDeleteReply(reply: DiscussionReply) {
  if (!isTeacherReply(reply)) {
    message.warning('只能删除自己发布的教师回复')
    return
  }

  Modal.confirm({
    title: '确认删除这条回复？',
    content: '删除后这条回复将不再展示；如果它已加入答疑精选，对应精选也会同步下架。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await deleteReply(reply)
    }
  })
}

watch(featuredVisible, (visible) => {
  if (!visible) {
    featuredForm.value = {
      replyId: '',
      excerpt: '',
      isRecommended: true
    }
  }
})

watch(
  () => route.query.focus,
  async () => {
    if (!loading.value) {
      await applyRouteFocus()
    }
  }
)


onMounted(() => {
  if (!isTeacherRole.value) {
    forbiddenAccess.value = true
    return
  }
  fetchDeskData()
})

onBeforeUnmount(() => {
  clearSectionHighlightTimer()
  replyEditorRef.value?.destroy()
})
</script>



<style scoped>
.teacher-community-desk {
  font-family: 'Plus Jakarta Sans', sans-serif;
  animation: fadeIn 0.4s ease;
  background: #f8fafc;
  border-radius: 5px;
  padding: 14px 24px 24px;
  box-sizing: border-box;

  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;

  /* 关键修改1：彻底禁止页面全局出现滚动条 */
  overflow: hidden !important;
}

/* 关键修改2：保护顶部卡片，不允许它们被 Flex 布局压缩 */
.page-header,
.focus-status-bar,
.overview-grid,
.page-state-card {
  flex-shrink: 0;
}

/* 补充统一的出场动画关键帧 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 顶部标题区域 (同步资源库风格) ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.title-group h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  display: flex;
  align-items: center;
}

.title-icon {
  margin-right: 10px;
  font-size: 28px;
}

.desk-icon {
  color: #7c3aed; /* 质感紫罗兰色，契合“处理审核”的沉稳感 */
}

.title-group .subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.overview-card,
.desk-card {
  background: #fff;
  border: 1px solid #edf2f7;
  border-radius: 5px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
  box-sizing: border-box; /* 👈 必须加上这行 */
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px 20px;
}

.overview-icon {
  width: 46px;
  height: 46px;
  border-radius: 5px; /* 修改：统一 5px */
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.icon-blue {
  background: #eff6ff;
  color: #2563eb;
}

.icon-orange {
  background: #fff7ed;
  color: #ea580c;
}

.icon-purple {
  background: #f5f3ff;
  color: #7c3aed;
}

.icon-green {
  background: #f0fdf4;
  color: #16a34a;
}

.overview-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.overview-label {
  font-size: 13px;
  color: #667085;
}

.overview-value {
  font-size: 26px;
  line-height: 1;
  color: #182230;
  font-weight: 800;
}

.overview-sub {
  font-size: 12px;
  color: #98a2b3;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) 320px;
  gap: 18px;
  align-items: stretch;

  /* 关键修改3：让网格区域自动拉伸，填满屏幕下方所有的剩余高度 */
  flex: 1;
  min-height: 0;
}
.main-column,
.side-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100%;

  /* 关键修改4：只允许两部分内容在自己的区域内独立上下滑动 */
  overflow-y: auto;
  padding-right: 6px; /* 给右侧滚动条留出一点呼吸空间，防止贴太紧 */
}

.main-column::-webkit-scrollbar,
.side-column::-webkit-scrollbar {
  width: 6px;
}
.main-column::-webkit-scrollbar-thumb,
.side-column::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 5px;
}

.desk-card {
  padding: 22px;
}

.side-card {
  /* 关键修改：移除 sticky 定位，并使其 flex 撑满高度 */
  flex: 1;
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  flex-shrink: 0; /* 👈 防止卡片高度太小时，标题区被变形压缩 */
}

.section-header.compact {
  margin-bottom: 14px;
}

.section-title {
  margin: 0 0 6px;
  color: #182230;
  font-size: 20px;
  font-weight: 700;
}

.section-desc {
  margin: 0;
  font-size: 13px;
  color: #667085;
}

.section-count {
  min-width: 56px;
  height: 32px;
  padding: 0 12px;
  border-radius: 5px; /* 修改：统一 5px */
  background: #f8fafc;
  border: 1px solid #eef2f6;
  color: #475467;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.history-switch {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #f8fafc;
}

.history-switch button {
  height: 32px;
  padding: 0 11px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.history-switch button span {
  margin-left: 4px;
  color: #98a2b3;
}

.history-switch button.active {
  background: #ffffff;
  color: #1d4ed8;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.12);
}

.history-switch button.active span {
  color: #2563eb;
}

.history-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
  border-top: 1px solid #f0f2f5;
  flex-shrink: 0;
}

.task-list,
.focus-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1; /* 👈 撑满卡片除了标题外的所有剩余高度 */
  overflow-y: auto; /* 👈 开启卡片内部独立滚动 */
  min-height: 0; /* 👈 关键点：打破 Flex 子元素的默认最小高度限制 */
  padding-right: 4px; /* 可选：留出一点滚动条呼吸空间，防止文字贴边 */
}

.task-item {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  border-radius: 5px; /* 修改：统一 5px */
  background: #fbfcfe;
  border: 1px solid #edf2f7;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.task-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.task-title {
  margin: 0;
  color: #182230;
  font-size: 17px;
  font-weight: 700;
}

.course-pill,
.status-pill {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 5px; /* 修改：统一 5px */
  font-size: 12px;
  font-weight: 700;
}

.course-pill {
  background: #f5f7fb;
  color: #475467;
  border: 1px solid #e8edf5;
}

.status-pill.open {
  background: #eef4ff;
  color: #2563eb;
}

.status-pill.teacher {
  background: #f5f3ff;
  color: #7c3aed;
}

.status-pill.resolved {
  background: #ecfdf3;
  color: #16a34a;
}

.task-excerpt {
  margin: 10px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.7;
}

.task-meta {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #98a2b3;
  font-size: 12px;
}

.task-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex-shrink: 0;
}



.focus-item {
  padding: 14px 14px 15px;
  border-radius: 5px; /* 修改：统一 5px */
  background: #fbfcfe;
  border: 1px solid #edf2f7;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.focus-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.focus-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.focus-kind {
  height: 24px;
  padding: 0 8px;
  border-radius: 5px; /* 修改：统一 5px */
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 700;
}

.focus-kind.homework {
  background: #eef4ff;
  color: #2563eb;
}

.focus-kind.discussion {
  background: #f5f3ff;
  color: #7c3aed;
}

.focus-time {
  color: #98a2b3;
  font-size: 12px;
}

.focus-title {
  margin: 0 0 6px;
  color: #182230;
  font-size: 14px;
  line-height: 1.6;
  font-weight: 700;
}

.focus-course {
  margin: 0;
  color: #667085;
  font-size: 12px;
}

.state-block,
.drawer-state {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  justify-content: center;
  color: #667085;
}

.compact-empty {
  min-height: 140px;
  /* 关键修改：让右侧无数据时能够完美居中占满变长的卡片高度 */
  flex: 1;
}

.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.drawer-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.drawer-title {
  margin: 0;
  color: #182230;
  font-size: 24px;
  line-height: 1.5;
  font-weight: 800;
}

.drawer-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.drawer-meta-text,
.drawer-meta-dot {
  color: #667085;
  font-size: 13px;
}

.drawer-action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.drawer-body-card {
  padding: 18px;
  border-radius: 5px; /* 修改：统一 5px */
  background: #fbfcfe;
  border: 1px solid #edf2f7;
}

.block-title {
  margin: 0 0 12px;
  color: #182230;
  font-size: 16px;
  font-weight: 700;
}

.drawer-article p,
.reply-content p {
  margin: 0 0 10px;
  color: #344054;
  font-size: 14px;
  line-height: 1.9;
}

.drawer-article p:last-child,
.reply-content p:last-child {
  margin-bottom: 0;
}

.drawer-stats {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #667085;
  font-size: 13px;
}

.drawer-stats span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.reply-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.reply-count {
  color: #667085;
  font-size: 13px;
  font-weight: 600;
}

.reply-editor {
  margin-top: 12px;
  margin-bottom: 18px;
}

.reply-textarea,
.form-textarea,
.form-select {
  width: 100%;
  border: 1px solid #e4eaf2;
  border-radius: 5px; /* 修改：统一 5px */
  background: #fff;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.reply-textarea,
.form-textarea {
  padding: 12px 14px;
  color: #344054;
  font-size: 14px;
  resize: vertical;
}

.reply-textarea:focus,
.form-textarea:focus,
.form-select:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.08);
}

.reply-actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.reply-tip {
  color: #98a2b3;
  font-size: 12px;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.reply-item {
  padding: 16px;
  border-radius: 5px; /* 修改：统一 5px */
  background: #fff;
  border: 1px solid #edf2f7;
}

.reply-item.teacher {
  background: #fbfcff;
  border-color: #dde8ff;
}

.reply-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.reply-author-wrap {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.reply-author {
  color: #182230;
  font-size: 14px;
  font-weight: 700;
}

.reply-badge {
  height: 22px;
  padding: 0 8px;
  border-radius: 5px; /* 修改：统一 5px */
  background: #eef4ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
}

.reply-time {
  color: #98a2b3;
  font-size: 12px;
}

.text-link {
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.featured-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  color: #344054;
  font-size: 13px;
  font-weight: 700;
}

.form-select {
  height: 42px;
  padding: 0 12px;
  color: #344054;
  font-size: 14px;
}

.featured-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #475467;
  font-size: 13px;
  cursor: pointer;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

.btn-light,
.btn-primary,
.btn-soft-primary,
.btn-soft-success,
.btn-soft-warning,
.btn-soft-disabled,
.btn-cancel,
.btn-submit {
  border: none;
  border-radius: 5px; /* 修改：统一 5px */
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-light {
  height: 42px;
  padding: 0 16px;
  background: #fff;
  color: #344054;
  border: 1px solid #e7ecf3;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-primary,
.btn-submit {
  height: 42px;
  padding: 0 16px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: #fff;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.16);
}

.btn-cancel {
  height: 42px;
  padding: 0 16px;
  background: #f8fafc;
  color: #475467;
  border: 1px solid #e7ecf3;
}

.btn-soft-primary,
.btn-soft-success,
.btn-soft-warning,
.btn-soft-disabled {
  min-width: 112px;
  height: 40px;
  padding: 0 14px;
}

.btn-soft-primary {
  background: #eef4ff;
  color: #2563eb;
}

.btn-soft-success {
  background: #ecfdf3;
  color: #16a34a;
}

.btn-soft-warning {
  background: #fff7ed;
  color: #ea580c;
}

.btn-soft-disabled {
  background: #f5f7fb;
  color: #98a2b3;
  cursor: not-allowed;
}

.btn-light:hover,
.btn-cancel:hover {
  border-color: #d6dde8;
}

.btn-primary:hover,
.btn-submit:hover,
.btn-soft-primary:hover,
.btn-soft-success:hover,
.btn-soft-warning:hover {
  transform: translateY(-1px);
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none !important;
}

@media (max-width: 1280px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .side-card {
    position: static;
  }
}

@media (max-width: 768px) {
  .teacher-community-desk {
    padding: 16px; /* 因为桌面端改成了32px，这里移动端需要统一缩小四边留白 */
  }

  .hero-card,
  .desk-card,
  .overview-card {
    border-radius: 5px; /* 修改：统一 5px */
  }

  .hero-card {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px;
  }

  .focus-status-bar {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .task-item {
    flex-direction: column;
  }

  .task-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .reply-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}

.desk-card {
  scroll-margin-top: 92px;
  transition: border-color 0.28s ease, box-shadow 0.28s ease, background-color 0.28s ease;
}

.section-focused {
  border-color: #dbeafe;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  box-shadow:
    0 0 0 4px rgba(59, 130, 246, 0.08),
    0 12px 28px rgba(15, 23, 42, 0.06);
}

.focus-tip {
  margin: 0;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.focus-status-bar {
  margin: 8px 0 16px 0; /* 调整独立后的上下间距 */
  padding: 14px 16px;
  border-radius: 5px;
  background: #f8fbff;
  border: 1px solid #ddeafe;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}
.focus-status-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.focus-status-pill {
  height: 28px;
  padding: 0 10px;
  border-radius: 5px; /* 修改：统一 5px */
  background: #eaf2ff;
  color: #2563eb;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.focus-status-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.focus-status-label {
  color: #182230;
  font-size: 14px;
  font-weight: 800;
}

.focus-status-text {
  margin: 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

.btn-ghost,
.mini-link-btn {
  border: 1px solid #dbe6f3;
  background: #fff;
  color: #475467;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 700;
}

.btn-ghost {
  height: 38px;
  padding: 0 14px;
  border-radius: 5px; /* 修改：统一 5px */
  flex-shrink: 0;
}

.btn-ghost:hover,
.mini-link-btn:hover {
  border-color: #c7d7ee;
  color: #2563eb;
}

.mini-link-btn {
  height: 30px;
  padding: 0 10px;
  border-radius: 5px; /* 修改：统一 5px */
  font-size: 12px;
}

.section-focus-row {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.section-focus-badge {
  height: 24px;
  padding: 0 8px;
  border-radius: 5px; /* 修改：统一 5px */
  background: #eef4ff;
  color: #2563eb;
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 700;
}

.section-muted {
  opacity: 0.74;
  background: linear-gradient(135deg, #ffffff 0%, #fbfcfe 100%);
}

.side-card-focused {
  border-color: #e4ebf5;
}

.side-focus-banner {
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 5px; /* 修改：统一 5px */
  background: #f8fbff;
  border: 1px solid #e2ebfb;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
}

.focus-item-active {
  border-color: #dbeafe;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.06);
}

.graph-context-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 18px;
}

.graph-related-strip {
  border: 1px solid #e2e8f0;
  border-radius: 5px; /* 修改：统一 5px */
  background: #ffffff;
  padding: 18px 20px;
}

.graph-related-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  color: #334155;
}

.graph-related-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.graph-related-item {
  border: 1px solid #e2e8f0;
  border-radius: 5px; /* 修改：统一 5px */
  background: #f8fafc;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  cursor: pointer;
}

.graph-related-item strong {
  color: #0f172a;
}

.graph-related-type, .graph-related-meta {
  font-size: 12px;
  color: #64748b;
}

/* 覆盖组件库原生表单元素的圆角 (全局统一) */
:deep(.ant-spin),
:deep(.ant-empty) {
  border-radius: 5px !important;
}

/* ===== 补充：左侧卡片高度拉伸，强制平分高度并与右侧完美对齐 ===== */
.desk-card {
  display: flex;
  flex-direction: column;
}

.main-column .desk-card {
  flex: 1 1 0%; /* 强制绝对平分高度，无视内部内容差异 */
  min-height: 0; /* 关键点：打破内部内容撑开卡片的限制 */
}

/* 保证没数据时，空状态的插画在拉伸后的大卡片里依然能完美垂直居中 */
.desk-card > .ant-empty,
.desk-card > .state-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  margin: 0;
}

/* ===== 待精选空状态引导 ===== */
.empty-guide {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  text-align: center;
}
.empty-guide-icon {
  font-size: 36px;
  color: #c4b5fd;
  margin-bottom: 12px;
}
.empty-guide-title {
  margin: 0 0 8px;
  color: #475467;
  font-size: 15px;
  font-weight: 700;
}
.empty-guide-desc {
  margin: 0 0 16px;
  color: #98a2b3;
  font-size: 13px;
  line-height: 1.8;
  max-width: 420px;
}
.empty-guide-desc a {
  color: #7c3aed;
  font-weight: 600;
  text-decoration: none;
}
.empty-guide-details {
  width: 100%;
  max-width: 460px;
  text-align: left;
}
.empty-guide-summary {
  color: #7c3aed;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 8px 0;
  user-select: none;
}
.empty-guide-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  margin-top: 8px;
  background: #fafaff;
  border: 1px solid #ede9fe;
  border-radius: 5px;
}
.guide-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #475467;
  font-size: 13px;
  line-height: 1.6;
}
.guide-step-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #ede9fe;
  color: #7c3aed;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}

/* ===== 新版答疑收件箱 ===== */
.answer-inbox-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  overflow: visible;
  flex-shrink: 0;
}

.answer-inbox-hero::after {
  display: none;
}

.answer-inbox-title {
  position: relative;
  max-width: 760px;
  z-index: 1;
}

.answer-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.answer-inbox-title h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
  font-weight: 850;
  letter-spacing: -0.5px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.answer-inbox-title h1 :deep(.anticon) {
  color: #7c3aed;
  font-size: 30px;
}

.answer-inbox-title p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.6;
}

.answer-inbox-metrics {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.metric-chip {
  min-width: 84px;
  padding: 7px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #ffffff;
}

.metric-chip span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.metric-chip strong {
  display: block;
  margin-top: 2px;
  color: #0f172a;
  font-size: 21px;
  line-height: 1;
}

.metric-chip.urgent strong {
  color: #dc2626;
}

.refresh-quiet {
  height: 38px;
  padding: 0 16px;
  border: 0;
  border-radius: 5px;
  color: #ffffff;
  background: #0f172a;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.refresh-quiet:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.answer-inbox-shell {
  display: grid;
  grid-template-columns: minmax(340px, 420px) minmax(0, 1fr);
  gap: 18px;
  min-height: 0;
  flex: 1;
}

.question-queue,
.answer-reader {
  min-height: 0;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.question-queue {
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 14px;
}

.queue-search {
  display: flex;
  gap: 8px;
}

.queue-search input {
  flex: 1;
  min-width: 0;
  height: 42px;
  padding: 0 13px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  color: #0f172a;
  outline: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.queue-search input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.queue-search button {
  width: 64px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: #2563eb;
  font-weight: 800;
  cursor: pointer;
}

.queue-tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.queue-tabs button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
  color: #475569;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}

.queue-tabs button:hover {
  transform: translateY(-1px);
}

.queue-tabs button.active {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #bfdbfe;
}

.queue-tabs em {
  min-width: 24px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: #ffffff;
  color: inherit;
  font-style: normal;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.queue-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow-y: auto;
  padding-right: 3px;
  min-height: 0;
}

.queue-card {
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #e7edf5;
  border-radius: 11px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.queue-card:hover,
.queue-card.active {
  border-color: #93c5fd;
  background: #f8fbff;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.07);
  transform: translateY(-1px);
}

.queue-card-top,
.queue-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.queue-card-top {
  justify-content: space-between;
}

.queue-type {
  padding: 2px 7px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4338ca;
  font-size: 11px;
  font-weight: 800;
}

.queue-type.pending {
  background: #fff1f2;
  color: #e11d48;
}

.queue-type.history {
  background: #ecfdf3;
  color: #15803d;
}

.queue-type.featured {
  background: #fef3c7;
  color: #b45309;
}

.queue-time,
.queue-meta {
  color: #94a3b8;
  font-size: 11px;
}

.queue-card h3 {
  margin: 6px 0 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 850;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.queue-card p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.queue-meta {
  margin-top: 6px;
}

.queue-state,
.answer-empty {
  flex: 1;
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #64748b;
  text-align: center;
}

.queue-state.empty svg,
.answer-empty svg {
  font-size: 34px;
  color: #cbd5e1;
}

.queue-state strong,
.answer-empty h2 {
  margin: 0;
  color: #334155;
  font-size: 18px;
}

.queue-state p,
.answer-empty p {
  max-width: 320px;
  margin: 0;
  color: #94a3b8;
  line-height: 1.7;
}

.answer-reader {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
  overflow-y: auto;
}

.answer-reader-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf2f7;
}

.answer-reader-status-row,
.answer-title-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.answer-title-row {
  align-items: baseline;
}

.answer-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 0;
}

.answer-reader-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 26px;
  line-height: 1.28;
  letter-spacing: -0.03em;
}

.answer-meta {
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  white-space: nowrap;
  flex-shrink: 0;
}

.answer-meta span + span::before {
  content: '·';
  margin-right: 14px;
  color: #cbd5e1;
}

.reader-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
  flex-shrink: 0;
}

.question-paper,
.reply-thread,
.reply-composer {
  border: 1px solid #edf2f7;
  border-radius: 16px;
  background: #fbfdff;
  padding: 18px;
}

.question-paper h3,
.reply-thread-title h3,
.reply-composer label {
  margin: 0 0 12px;
  color: #334155;
  font-size: 15px;
  font-weight: 850;
}

.question-paper p,
.reply-rich-content :deep(p) {
  margin: 0 0 10px;
  color: #475569;
  font-size: 14px;
  line-height: 1.85;
}

.reply-rich-content :deep(p:last-child) {
  margin-bottom: 0;
}

.reply-rich-content :deep(img) {
  display: block;
  max-width: min(100%, 720px);
  height: auto;
  margin: 12px 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.reply-rich-content :deep(a) {
  color: #2563eb;
  text-decoration: underline;
}

.reply-thread-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reply-thread-title span {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
}

.reply-empty-inline {
  padding: 2px 0;
  color: #94a3b8;
  font-size: 14px;
}

.reply-bubble {
  margin-top: 12px;
  padding: 14px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #e7edf5;
}

.reply-bubble.teacher {
  background: #f8fbff;
  border-color: #bfdbfe;
}

.reply-bubble-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.reply-bubble-head strong {
  color: #0f172a;
}

.reply-bubble-head span {
  color: #94a3b8;
  font-size: 12px;
}

.reply-bubble-meta-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  white-space: nowrap;
}

.reply-delete-btn {
  border: 0;
  border-radius: 999px;
  padding: 3px 8px;
  background: #fff1f2;
  color: #e11d48;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.reply-delete-btn:hover:not(:disabled) {
  background: #ffe4e6;
  color: #be123c;
  transform: translateY(-1px);
}

.reply-delete-btn:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.inline-feature-btn {
  border: 0;
  padding: 7px 10px;
  border-radius: 10px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.reply-composer {
  background: linear-gradient(180deg, #ffffff, #f8fafc);
}

.reply-editor-shell {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
}

.reply-editor-toolbar {
  border-bottom: 1px solid #e2e8f0;
}

.reply-editor-content {
  height: 180px !important;
  overflow-y: auto !important;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.composer-actions span {
  color: #94a3b8;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .answer-inbox-hero,
  .answer-reader-status-row,
  .answer-title-row {
    flex-direction: column;
    align-items: stretch;
  }

  .answer-meta {
    justify-content: flex-start;
    flex-wrap: wrap;
    white-space: normal;
  }

  .answer-inbox-shell {
    grid-template-columns: 1fr;
  }
}
</style>
