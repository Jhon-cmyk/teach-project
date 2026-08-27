<template>
  <div class="coding-bank-page modern-page">
    <!-- 页面标题 + 类型切换 -->
    <div class="page-header-card glass-panel">
      <div class="header-top">
        <div class="title-group">
          <h2><code-outlined class="title-icon" /> 题库管理</h2>
          <p class="subtitle">统一管理编程题、课后作业与考试试卷</p>
        </div>

        <div class="header-actions">
          <a-button v-if="activeTab === 'coding'" type="primary" class="add-btn" @click="goToCodingGenerator">
            <plus-outlined /> 新建题目
          </a-button>
          <a-button v-else type="primary" class="add-btn" @click="goToQuizGenerator">
            <plus-outlined /> 去创建
          </a-button>
        </div>
      </div>

      <div class="header-tabs-wrapper">
        <a-tabs v-model:activeKey="activeTab" class="bank-tabs" @change="onTabChange">
          <a-tab-pane key="coding" tab="编程题" />
          <a-tab-pane key="homework" tab="课后作业" />
          <a-tab-pane key="exam" tab="考试试卷" />
        </a-tabs>
      </div>
    </div>

    <!-- 统计卡片 - 编程题 -->
    <div v-if="activeTab === 'coding'" class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:#eef2ff;color:#6366f1;"><code-outlined /></div>
        <div class="stat-body">
          <span class="stat-label">题目总数</span>
          <strong class="stat-value">{{ problemList.length }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f0fdf4;color:#22c55e;"><check-circle-outlined /></div>
        <div class="stat-body">
          <span class="stat-label">简单</span>
          <strong class="stat-value">{{ problemList.filter((p:any) => p.difficulty === 'easy').length }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fff7ed;color:#f97316;"><warning-outlined /></div>
        <div class="stat-body">
          <span class="stat-label">中等</span>
          <strong class="stat-value">{{ problemList.filter((p:any) => p.difficulty === 'medium').length }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fff1f2;color:#ef4444;"><close-circle-outlined /></div>
        <div class="stat-body">
          <span class="stat-label">困难</span>
          <strong class="stat-value">{{ problemList.filter((p:any) => p.difficulty === 'hard').length }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f0f9ff;color:#0ea5e9;"><send-outlined /></div>
        <div class="stat-body">
          <span class="stat-label">已发布</span>
          <strong class="stat-value">{{ problemList.filter((p:any) => (p.publishCount || 0) > 0).length }}</strong>
        </div>
      </div>
    </div>

    <!-- 统计卡片 - 课后作业 / 考试试卷 -->
    <div v-else class="stats-row quiz-stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:#eef2ff;color:#6366f1;"><file-text-outlined /></div>
        <div class="stat-body">
          <span class="stat-label">试卷总数</span>
          <strong class="stat-value">{{ activeTab === 'homework' ? homeworkQuizList.length : examQuizList.length }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f0fdf4;color:#22c55e;"><check-circle-outlined /></div>
        <div class="stat-body">
          <span class="stat-label">已发布</span>
          <strong class="stat-value">{{ activeTab === 'homework' ? homeworkQuizList.filter((q:any) => q.homeworkPublished).length : examQuizList.filter((q:any) => q.examPublished).length }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fff7ed;color:#f97316;"><clock-circle-outlined /></div>
        <div class="stat-body">
          <span class="stat-label">待发布</span>
          <strong class="stat-value">{{ activeTab === 'homework' ? homeworkQuizList.filter((q:any) => !q.homeworkPublished).length : examQuizList.filter((q:any) => !q.examPublished).length }}</strong>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#f3e8ff;color:#a855f7;"><question-circle-outlined /></div>
        <div class="stat-body">
          <span class="stat-label">总题量</span>
          <strong class="stat-value">{{ activeTab === 'homework' ? homeworkQuizList.reduce((s:number,q:any) => s + (q.questionCount||0), 0) : examQuizList.reduce((s:number,q:any) => s + (q.questionCount||0), 0) }}</strong>
        </div>
      </div>
    </div>

    <!-- ===== 编程题 Tab ===== -->
    <div v-if="activeTab === 'coding'" class="coding-tab-content">
      <div class="filter-dashboard glass-panel">
        <div class="filter-group">
          <div class="filter-item">
            <a-input
              v-model:value="filter.keyword"
              class="bank-search-input"
              placeholder="搜索标题/描述"
              style="width: 240px"
              allow-clear
              @pressEnter="applyFilter"
            >
              <template #suffix>
                <search-outlined class="bank-search-icon" @click="applyFilter" />
              </template>
            </a-input>
          </div>
          <div class="filter-item">
            <span class="label">难度</span>
            <a-select v-model:value="filter.difficulty" style="width: 120px" placeholder="全部" allow-clear>
              <a-select-option value="easy">简单</a-select-option>
              <a-select-option value="medium">中等</a-select-option>
              <a-select-option value="hard">困难</a-select-option>
            </a-select>
          </div>
          <div class="filter-item">
            <span class="label">语言</span>
            <a-select v-model:value="filter.language" style="width: 140px" placeholder="全部" allow-clear>
              <a-select-option value="java">Java</a-select-option>
              <a-select-option value="python">Python</a-select-option>
              <a-select-option value="cpp">C++</a-select-option>
              <a-select-option value="javascript">JavaScript</a-select-option>
            </a-select>
          </div>
          <div class="filter-item">
            <span class="label">学期</span>
            <a-select
              v-model:value="filter.semesterLabel"
              style="width: 190px"
              placeholder="全部学期"
              allow-clear
            >
              <a-select-option v-for="item in semesterOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
              <a-select-option value="__unset__">未设置</a-select-option>
            </a-select>
          </div>
        </div>
        <div class="filter-group">
          <a-button @click="resetFilter">重置</a-button>
        </div>
      </div>

      <div class="table-panel glass-panel">
        <a-table
          :dataSource="filteredList"
          :columns="codingColumns"
          :loading="loading"
          rowKey="id"
          :pagination="{ pageSize: 5, pageSizeOptions: ['5', '10', '20'], showSizeChanger: true }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'difficulty'">
              <a-tag :color="difficultyColor(record.difficulty)">{{ difficultyLabel(record.difficulty) }}</a-tag>
            </template>
            <template v-if="column.key === 'languages'">
              <a-tag v-for="lang in (record.languages || [])" :key="lang" color="blue">{{ lang }}</a-tag>
            </template>
            <template v-if="column.key === 'semesterLabel'">
              <span v-if="resolveSemesterLabel(record)" class="semester-cell">{{ resolveSemesterLabel(record) }}</span>
              <span v-else class="muted-cell">未设置</span>
            </template>
            <template v-if="column.key === 'publishCount'">
              <a-tooltip :title="(record.publishedClasses || []).join('、') || '暂未发布'">
                <span class="icon-text-cell"><team-outlined /> {{ record.publishCount || 0 }}</span>
              </a-tooltip>
            </template>
            <template v-if="column.key === 'publishStatus'">
              <a-tag :color="(record.publishCount || 0) > 0 ? 'green' : 'orange'">
                {{ (record.publishCount || 0) > 0 ? '已发布' : '未发布' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'submissionCount'">
              <a-tooltip title="累计提交人数">
                <span class="icon-text-cell"><form-outlined /> {{ record.submissionCount || 0 }}</span>
              </a-tooltip>
            </template>
            <template v-if="column.key === 'action'">
              <a-space class="action-space">
                <a-button size="small" type="link" @click="openEditModal(record)">编辑</a-button>
                <a-button size="small" type="link" @click="openSubmissions(record)">详情</a-button>
                <a-button size="small" type="link" :disabled="(record.publishCount || 0) > 0" @click="openPublish(record)">发布</a-button>
                <a-popconfirm title="确定删除？该题相关的发布和提交数据不受影响" @confirm="handleDelete(record.id)">
                  <a-button size="small" type="link" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <!-- ===== 课后作业 Tab ===== -->
    <div v-if="activeTab === 'homework'" class="coding-tab-content">
      <div class="filter-dashboard glass-panel">
        <div class="filter-group">
          <div class="filter-item">
            <a-input
              v-model:value="homeworkFilter.keyword"
              class="bank-search-input"
              placeholder="搜索试卷标题"
              style="width: 240px"
              allow-clear
              @pressEnter="applyHomeworkFilter"
            >
              <template #suffix>
                <search-outlined class="bank-search-icon" @click="applyHomeworkFilter" />
              </template>
            </a-input>
          </div>
          <div class="filter-item">
            <span class="label">状态</span>
            <a-select v-model:value="homeworkFilter.publishStatus" style="width: 120px" placeholder="全部" allow-clear>
              <a-select-option value="published">已发布</a-select-option>
              <a-select-option value="unpublished">未发布</a-select-option>
            </a-select>
          </div>
        </div>
        <div class="filter-group">
          <a-button @click="resetHomeworkFilter">重置</a-button>
        </div>
      </div>

      <div class="table-panel glass-panel">
        <a-table
          :dataSource="filteredHomeworkList"
          :columns="homeworkColumns"
          :loading="quizLoading"
          rowKey="id"
          :pagination="{ pageSize: 5, pageSizeOptions: ['5', '10', '20'], showSizeChanger: true }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'questionCount'">
              {{ record.questionCount || 0 }} 道
            </template>
            <template v-if="column.key === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
            <template v-if="column.key === 'isPublished'">
              <a-tag :color="record.homeworkPublished ? 'green' : 'orange'">
                {{ record.homeworkPublished ? '已发布' : '未发布' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space class="action-space">
                <a-button size="small" type="link" @click="previewQuizContent(record)">查看</a-button>
                <a-button size="small" type="link" :disabled="record.homeworkPublished" @click="openHomeworkPublish(record)">发布</a-button>
                <a-button v-if="record.homeworkPublished" size="small" type="link" @click="openCompletionView(record)">完成情况</a-button>
                <a-popconfirm title="确定删除该试卷？" @confirm="handleDeleteQuiz(record.id)">
                  <a-button size="small" type="link" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <!-- ===== 考试试卷 Tab ===== -->
    <div v-if="activeTab === 'exam'" class="coding-tab-content">
      <div class="filter-dashboard glass-panel">
        <div class="filter-group">
          <div class="filter-item">
            <a-input
              v-model:value="examFilter.keyword"
              class="bank-search-input"
              placeholder="搜索试卷标题"
              style="width: 240px"
              allow-clear
              @pressEnter="applyExamFilter"
            >
              <template #suffix>
                <search-outlined class="bank-search-icon" @click="applyExamFilter" />
              </template>
            </a-input>
          </div>
          <div class="filter-item">
            <span class="label">状态</span>
            <a-select v-model:value="examFilter.publishStatus" style="width: 120px" placeholder="全部" allow-clear>
              <a-select-option value="published">已发布</a-select-option>
              <a-select-option value="unpublished">未发布</a-select-option>
            </a-select>
          </div>
        </div>
        <div class="filter-group">
          <a-button @click="resetExamFilter">重置</a-button>
        </div>
      </div>

      <div class="table-panel glass-panel">
        <a-table
          :dataSource="filteredExamList"
          :columns="examQuizColumns"
          :loading="quizLoading"
          rowKey="id"
          :pagination="{ pageSize: 5, pageSizeOptions: ['5', '10', '20'], showSizeChanger: true }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'questionCount'">
              {{ record.questionCount || 0 }} 道
            </template>
            <template v-if="column.key === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
            <template v-if="column.key === 'isPublished'">
              <a-tag :color="record.examPublished ? 'green' : 'orange'">
                {{ record.examPublished ? '已发布' : '未发布' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space class="action-space">
                <a-button size="small" type="link" @click="previewQuizContent(record)">查看</a-button>
                <a-button size="small" type="link" :disabled="record.examPublished" @click="openExamPublish(record)">发布</a-button>
                <a-button v-if="record.examPublished" size="small" type="link" @click="openExamGrading(record)">批阅</a-button>
                <a-popconfirm title="确定删除该试卷？" @confirm="handleDeleteQuiz(record.id)">
                  <a-button size="small" type="link" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <!-- ========== 所有弹窗/Drawer 保持原有逻辑不变 ========== -->

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="showFormModal"
      :title="isEdit ? '编辑编程题' : '新建编程题'"
      width="1100px"
      :footer="null"
      centered
      destroyOnClose
      class="bank-modal"
    >
      <div class="problem-modal-body">
        <div class="problem-form-col">
          <a-form layout="vertical">
            <a-form-item label="题目标题" required>
              <a-input v-model:value="form.title" placeholder="例如：两数之和" />
            </a-form-item>

            <a-form-item label="难度">
              <a-select v-model:value="form.difficulty">
                <a-select-option value="easy">简单</a-select-option>
                <a-select-option value="medium">中等</a-select-option>
                <a-select-option value="hard">困难</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="所属学期">
              <a-select v-model:value="form.semesterLabel" placeholder="选择学期，便于题库归档" allow-clear>
                <a-select-option v-for="item in semesterOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="支持语言">
              <a-checkbox-group v-model:value="form.languages" @change="syncTemplates">
                <a-checkbox value="java">Java</a-checkbox>
                <a-checkbox value="python">Python</a-checkbox>
                <a-checkbox value="cpp">C++</a-checkbox>
                <a-checkbox value="javascript">JavaScript</a-checkbox>
              </a-checkbox-group>
            </a-form-item>

            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="时间限制（ms）">
                  <a-input-number v-model:value="form.timeLimitMs" :min="500" :max="30000" :step="500" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="内存限制（KB）">
                  <a-input-number v-model:value="form.memoryLimitKb" :min="32768" :max="1048576" :step="32768" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="题目描述（支持 Markdown）" required>
              <a-textarea v-model:value="form.description" :rows="8" placeholder="Markdown 格式的题目描述" />
            </a-form-item>
          </a-form>
        </div>

        <div class="problem-extra-col">
          <div class="problem-extra-header">
            <span class="problem-extra-title">代码模板与测试用例</span>
            <a-button type="primary" :loading="formSubmitting" @click="handleSubmitForm">
              <check-circle-filled /> {{ isEdit ? '保存修改' : '创建题目' }}
            </a-button>
          </div>
          <div class="problem-extra-content">
            <div class="extra-section">
              <div class="extra-section-title">代码模板（按语言）</div>
              <a-tabs v-if="form.languages.length" v-model:activeKey="activeTemplateLang" size="small">
                <a-tab-pane v-for="lang in form.languages" :key="lang" :tab="lang">
                  <a-form-item label="初始代码" class="compact-form-item">
                    <a-textarea
                      :value="getTemplate(lang).starterCode"
                      @update:value="(v: string) => setTemplate(lang, 'starterCode', v)"
                      :rows="4"
                      :placeholder="`${lang} 初始代码，学生打开题目时看到`"
                      class="mono-text"
                    />
                  </a-form-item>
                  <a-form-item label="参考答案" class="compact-form-item">
                    <a-textarea
                      :value="getTemplate(lang).referenceSolution"
                      @update:value="(v: string) => setTemplate(lang, 'referenceSolution', v)"
                      :rows="4"
                      placeholder="可选：记录标准答案"
                      class="mono-text"
                    />
                  </a-form-item>
                </a-tab-pane>
              </a-tabs>
              <a-empty v-else description="请先选择至少一种支持语言" />
            </div>

            <div class="extra-section">
              <div class="extra-section-title">
                测试用例
                <a-button size="small" type="link" @click="addTestCase">+ 添加</a-button>
              </div>
              <div v-if="form.testCases.length === 0" class="hint">
                没有测试用例也可以保存，但学生提交时将默认通过。建议至少添加 1-2 个样例 + 若干隐藏用例。
              </div>
              <div v-for="(tc, idx) in form.testCases" :key="idx" class="test-case-row">
                <a-row :gutter="8" align="middle">
                  <a-col :span="2">#{{ idx + 1 }}</a-col>
                  <a-col :span="9">
                    <a-textarea v-model:value="tc.input" :rows="3" placeholder="输入（可留空）" class="mono-text" />
                  </a-col>
                  <a-col :span="9">
                    <a-textarea v-model:value="tc.expectedOutput" :rows="3" placeholder="期望输出" class="mono-text" />
                  </a-col>
                  <a-col :span="4">
                    <a-space direction="vertical" size="small" style="width: 100%">
                      <a-checkbox :checked="tc.isSample === 1" @change="(e: any) => tc.isSample = e.target.checked ? 1 : 0">样例</a-checkbox>
                      <a-input-number v-model:value="tc.score" :min="0" :max="100" placeholder="分值" style="width: 100%" />
                      <a-button size="small" danger type="link" @click="form.testCases.splice(idx, 1)">删除</a-button>
                    </a-space>
                  </a-col>
                </a-row>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 提交记录抽屉 -->
    <a-drawer v-model:open="showSubmissionsDrawer" title="学生提交记录" width="900px" class="bank-drawer">
      <div v-if="submissionsProblem" class="submissions-header">
        <a-tag color="blue">{{ submissionsProblem.title }}</a-tag>
        <span class="hint">共 {{ submissions.length }} 条提交</span>
      </div>
      <a-table
        :dataSource="submissions"
        :columns="submissionColumns"
        :pagination="{ pageSize: 10 }"
        rowKey="id"
        size="small"
        :loading="submissionsLoading"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'finalScore'">
            <span :style="{ color: record.finalScore >= 60 ? '#52c41a' : '#f5222d', fontWeight: 600 }">
              {{ record.finalScore }}
            </span>
          </template>
          <template v-if="column.key === 'passRate'">
            {{ record.passedCount }}/{{ record.totalCount }}
          </template>
          <template v-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-if="column.key === 'codeAction'">
            <a-button size="small" type="link" @click="viewSubmissionCode(record)">查看详情</a-button>
            <a-popconfirm title="确定删除该学生的这条提交记录？" @confirm="handleDeleteSubmission(record.id)">
              <a-button size="small" type="link" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-drawer>

    <!-- 发布弹窗 -->
    <a-modal
      v-model:open="showPublishModal"
      title="发布编程题到班级"
      width="1100px"
      @ok="handlePublishConfirm"
      :confirmLoading="publishing"
      class="bank-modal"
    >
      <p>将题目「<strong>{{ publishRecord?.title }}</strong>」发布到指定班级</p>
      <a-form layout="vertical">
        <a-form-item label="选择班级" required>
          <a-select
            v-model:value="publishForm.classIds"
            mode="multiple"
            placeholder="选择一个或多个班级"
            :loading="classListLoading"
            style="width: 100%"
            :options="classOptions"
            :filter-option="filterClassOption"
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <a-form-item label="截止时间（可选）">
          <a-date-picker
            v-model:value="publishForm.deadline"
            show-time
            style="width: 100%"
            placeholder="不设置则学生可无限期提交"
            :disabled-date="disabledDate"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 发布课后作业弹窗 -->
    <a-modal
      v-model:open="showHomeworkPublishModal"
      title="发布课后作业"
      width="760px"
      @ok="handleHomeworkPublish"
      :confirmLoading="homeworkPublishing"
      centered
      ok-text="发布作业"
      cancel-text="取消"
      wrap-class-name="assignment-publish-modal-wrap"
      class="bank-modal assignment-publish-modal"
    >
      <p class="publish-dialog-summary">将试卷「<strong>{{ homeworkPublishRecord?.title }}</strong>」发布为课后作业</p>
      <a-form layout="vertical" class="publish-dialog-form">
        <a-form-item label="选择班级" required>
          <a-select
            v-model:value="homeworkPublishForm.classId"
            placeholder="选择一个班级"
            :loading="homeworkClassListLoading"
            style="width: 100%"
            :options="homeworkClassOptions"
            :filter-option="filterClassOption"
            show-search
            option-filter-prop="label"
            :list-height="224"
            not-found-content="暂无可发布的授课班级"
            size="large"
          />
        </a-form-item>
        <a-form-item label="截止时间">
          <a-date-picker
            v-model:value="homeworkPublishForm.deadline"
            show-time
            style="width: 100%"
            placeholder="不设置则学生可无限期提交"
            :disabled-date="disabledDate"
            size="large"
          />
        </a-form-item>
        <a-form-item label="允许重做">
          <a-switch v-model:checked="homeworkPublishForm.allowRedo" />
        </a-form-item>
        <a-form-item v-if="homeworkPublishForm.allowRedo" label="最大重做次数">
          <a-input-number v-model:value="homeworkPublishForm.maxAttemptCount" :min="2" :max="10" style="width: 160px" size="large" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 发布考试弹窗 -->
    <a-modal
      v-model:open="showExamPublishModal"
      title="发布考试"
      width="760px"
      @ok="handleExamPublish"
      :confirmLoading="examPublishing"
      centered
      ok-text="发布考试"
      cancel-text="取消"
      wrap-class-name="assignment-publish-modal-wrap"
      class="bank-modal assignment-publish-modal"
    >
      <p class="publish-dialog-summary">将试卷「<strong>{{ examPublishRecord?.title }}</strong>」发布为考试</p>
      <a-form layout="vertical" class="publish-dialog-form">
        <a-form-item label="选择班级" required>
          <a-select
            v-model:value="examPublishForm.classId"
            placeholder="选择一个班级"
            :loading="examClassListLoading"
            style="width: 100%"
            :options="examClassOptions"
            :filter-option="filterClassOption"
            show-search
            option-filter-prop="label"
            :list-height="224"
            not-found-content="暂无可发布的授课班级"
            size="large"
          />
        </a-form-item>
        <a-form-item label="考试时长（分钟）" required>
          <a-input-number v-model:value="examPublishForm.durationMinutes" :min="5" :max="300" style="width: 160px" size="large" />
        </a-form-item>
        <a-form-item label="截止时间">
          <a-date-picker
            v-model:value="examPublishForm.deadline"
            show-time
            style="width: 100%"
            placeholder="不设置则无截止时间"
            :disabled-date="disabledDate"
            size="large"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批阅抽屉 -->
    <a-drawer
      v-model:open="showExamGradingDrawer"
      :title="`批阅考试 - ${examGradingRecord?.title || ''}`"
      width="900px"
      class="bank-drawer"
    >
      <a-tabs v-if="examSubmissionsByClass.length > 1" v-model:activeKey="activeExamClassKey" @change="onExamClassTabChange">
        <a-tab-pane v-for="group in examSubmissionsByClass" :key="group.classId" :tab="`班级 ${group.classId}`" />
      </a-tabs>
      <a-table
        :dataSource="examGradingStudents"
        :columns="examGradingColumns"
        :loading="examGradingLoading"
        rowKey="submissionId"
        :pagination="{ pageSize: 10 }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'submitStatus'">
            <a-tag :color="record.submitStatus === 'completed' ? 'green' : 'orange'">
              {{ record.submitStatus === 'completed' ? '已批阅' : '待批阅' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'submitTime'">
            {{ record.submitTime ? formatTime(record.submitTime) : '--' }}
          </template>
          <template v-if="column.key === 'totalScore'">
            {{ record.totalScore != null ? record.totalScore : '--' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-button size="small" type="link" @click="openGradeStudent(record)">
              {{ record.submitStatus === 'completed' ? '预览' : '批阅' }}
            </a-button>
          </template>
        </template>
      </a-table>
    </a-drawer>

    <!-- 批阅工作台弹窗 -->
    <a-modal
      v-model:open="showGradingModal"
      :title="`${isGradePreview ? '批阅预览' : '批阅'} - ${gradingStudent?.studentName || ''}`"
      width="1100px"
      :footer="null"
      centered
      class="bank-modal grading-modal"
    >
      <a-spin :spinning="gradeDetailLoading || aiGradingLoading || gradeCommentGenerating">
        <div v-if="processedGradingDetails.length > 0" class="grading-workbench">
          <div class="grading-paper-col">
            <div class="grading-paper-header">试卷与参考答案</div>
            <div class="grading-paper-content doc-style" v-html="renderMd(gradingContentSnapshot)"></div>
          </div>
          <div class="grading-grading-col">
            <div class="grading-col-header">
              <div class="grading-student-info">
                <span class="grading-student-name">{{ gradingStudent?.studentName }}</span>
                <a-tag :color="gradingStudent?.submitStatus === 'completed' ? 'green' : 'orange'">
                  {{ gradingStudent?.submitStatus === 'completed' ? '已批阅' : '待批阅' }}
                </a-tag>
              </div>
              <a-button
                v-if="!isGradePreview"
                type="primary"
                ghost
                size="small"
                :loading="aiGradingLoading"
                @click="triggerAiGrading"
              >
                AI自动批阅
              </a-button>
            </div>
            <div class="grading-scroll-area">
              <div v-for="section in processedGradingSections" :key="section.key" class="grade-section">
                <div class="grade-section-title">
                  <span>{{ section.title }}</span>
                  <span>{{ section.items.length }} 题</span>
                </div>
                <div v-for="d in section.items" :key="d.id" class="grade-item">
                <div class="grade-item-header">
                  <span>第 {{ d._sectionNo }} 题</span>
                  <a-tag v-if="hasReviewImages(d)" color="blue">图片作答</a-tag>
                  <a-tag v-else-if="d.score != null" color="green">AI已判</a-tag>
                </div>
                <div class="grade-stem" v-if="d.stemSnapshot">{{ d.stemSnapshot }}</div>
                <div class="grade-answer">
                  <span class="grade-label">学生答案：</span>
                  <span class="grade-value" v-html="reviewStudentAnswerText(d)"></span>
                </div>
                <div class="grade-answer" v-if="d.standardAnswer">
                  <span class="grade-label">参考答案：</span>
                  <span class="grade-value" style="color:#059669;">{{ d.standardAnswer }}</span>
                </div>
                <div v-if="hasReviewImages(d)" class="review-images grade-review-images">
                  <a-image
                    v-for="(url, imageIdx) in parseReviewImageUrls(d.imageUrlsJson)"
                    :key="url"
                    :src="url"
                    :width="96"
                    :height="72"
                    :alt="`第${d.questionNo || ''}题图片${imageIdx + 1}`"
                  />
                </div>
                <div class="grade-score-input">
                  <span class="grade-label">得分：</span>
                  <a-input-number v-model:value="gradeScores[d._scoreIndex]" :min="0" :max="d.fullScore ?? 100" :disabled="isGradePreview" style="width: 120px" />
                  <span class="grade-score-max">/ {{ d.fullScore ?? '--' }} 分</span>
                </div>
                <div class="grade-ai-comment" v-if="d.aiComment">
                  <span class="grade-label">AI评语：</span>
                  <span style="color:#6B7280; font-size:13px;">{{ d.aiComment }}</span>
                </div>
              </div>
            </div>
            </div>
            <div class="grading-footer">
              <div class="grade-remark-area">
                <span class="grade-label">总评语：</span>
                <a-textarea v-model:value="gradeTeacherRemark" :rows="2" :disabled="isGradePreview" placeholder="输入总评语（可选）" />
              </div>
              <div v-if="!isGradePreview" class="grade-actions">
                <a-button :loading="gradeCommentGenerating" @click="generateExamGradeRemark">生成评语</a-button>
                <a-button type="primary" :loading="gradeSaving" @click="saveGrade">保存批阅</a-button>
              </div>
            </div>
          </div>
        </div>
        <div v-else>
          <a-empty description="暂无答题详情" />
        </div>
      </a-spin>
    </a-modal>

    <!-- 试卷预览弹窗 -->
    <a-modal
      v-model:open="showQuizPreviewModal"
      width="1100px"
      :footer="null"
      centered
      class="preview-modal"
    >
      <template #title>
        <div class="modal-custom-title">
          <file-text-outlined class="m-icon" />
          <span>{{ previewQuizRecord?.title || '试卷预览' }}</span>
        </div>
      </template>
      <div v-if="previewQuizRecord" class="quiz-preview-wrapper">
        <div class="quiz-preview-meta">
          <a-tag color="purple">{{ previewQuizRecord.scenario || '试卷' }}</a-tag>
          <span class="quiz-preview-count">{{ previewQuizRecord.questionCount || 0 }} 道题</span>
          <span class="quiz-preview-time">创建于 {{ formatTime(previewQuizRecord.createTime) }}</span>
        </div>
        <div class="quiz-preview-content doc-style" v-html="renderMd(previewQuizRecord.content || '')"></div>
      </div>
    </a-modal>

    <!-- 完成情况弹窗 -->
    <a-modal
      v-model:open="showCompletionModal"
      :title="`完成情况 - ${completionQuizTitle}`"
      width="1100px"
      :footer="null"
      centered
      class="bank-modal"
    >
      <template v-if="completionAssignments.length > 1">
        <a-tabs v-model:activeKey="activeAssignmentKey" @change="onAssignmentTabChange">
          <a-tab-pane v-for="a in completionAssignments" :key="a.assignmentId" :tab="`班级 ${a.classId}`" />
        </a-tabs>
      </template>
      <div v-if="completionActiveAssignment" class="completion-summary">
        <a-space wrap>
          <span>学生总数: <strong>{{ completionActiveAssignment.studentTotal || 0 }}</strong></span>
          <a-divider type="vertical" />
          <span>已完成: <strong style="color:#16a34a">{{ completionActiveAssignment.completedCount || 0 }}</strong></span>
          <a-divider type="vertical" />
          <span>待批改: <strong style="color:#2563eb">{{ completionActiveAssignment.reviewPendingCount || 0 }}</strong></span>
          <a-divider type="vertical" />
          <span>未提交: <strong style="color:#f59e0b">{{ completionActiveAssignment.pendingCount || 0 }}</strong></span>
          <a-divider type="vertical" />
          <span>平均分: <strong>{{ completionActiveAssignment.avgScore != null ? completionActiveAssignment.avgScore.toFixed(1) : '--' }}</strong></span>
          <a-divider type="vertical" />
          <a-select v-model:value="completionStatusFilter" size="small" style="width: 120px">
            <a-select-option value="all">全部状态</a-select-option>
            <a-select-option value="review_pending">待批改</a-select-option>
            <a-select-option value="completed">已完成</a-select-option>
            <a-select-option value="pending">未提交</a-select-option>
          </a-select>
        </a-space>
      </div>
      <a-table
        :dataSource="filteredCompletionStudents"
        :columns="completionStudentColumns"
        :loading="completionStudentsLoading"
        rowKey="studentId"
        :pagination="{ pageSize: 10 }"
        size="small"
        style="margin-top: 16px;"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'submitStatus'">
            <span :class="submitStatusClass(record.submitStatus)">{{ submitStatusText(record.submitStatus) }}</span>
          </template>
          <template v-if="column.key === 'totalScore'">
            <span v-if="record.submitStatus === 'review_pending'">
              待批改
            </span>
            <span v-else>{{ record.totalScore != null ? record.totalScore : '--' }}</span>
          </template>
          <template v-if="column.key === 'pendingReviewQuestionCount'">
            {{ record.pendingReviewQuestionCount || '--' }}
          </template>
          <template v-if="column.key === 'correctCount'">
            {{ record.correctCount != null ? record.correctCount : '--' }}
          </template>
          <template v-if="column.key === 'wrongCount'">
            {{ record.wrongCount != null ? record.wrongCount : '--' }}
          </template>
          <template v-if="column.key === 'submitTime'">
            {{ record.submitTime ? formatTime(record.submitTime) : '--' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-button
              v-if="(record.submitStatus === 'completed' || record.submitStatus === 'review_pending') && record.submissionId"
              size="small"
              type="link"
              @click="openStudentReport(record)"
            >
              {{ record.submitStatus === 'review_pending' ? '批改' : '查看结果' }}
            </a-button>
            <span v-else class="hint">--</span>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 学生报告抽屉 -->
    <a-drawer
      v-model:open="showStudentReportDrawer"
      :title="studentReportDrawerTitle"
      width="1100px"
      class="bank-drawer"
    >
      <a-spin :spinning="studentReportLoading">
        <HomeworkReportPanel
          v-if="studentReportData"
          :report="studentReportData"
          :student-name="studentReportName"
          :show-student-name="false"
          role="teacher"
        />
        <div v-if="studentReportData?.submission?.submitStatus === 'review_pending'" class="review-box">
          <div class="review-title">教师批改</div>
          <div v-for="item in studentReportData.details || []" :key="item.id" class="review-row">
            <div class="review-main">
              <div class="review-question-line">
                <span class="review-question">第 {{ item.questionNo }} 题</span>
                <a-tag>{{ questionTypeText(item.questionType) }}</a-tag>
                <a-tag v-if="hasReviewImages(item)" color="blue">图片作答</a-tag>
                <a-tag v-else color="green">{{ isObjectiveReviewItem(item) ? '自动判定' : 'AI判定' }}</a-tag>
              </div>
              <div v-if="item.stemSnapshot" class="review-stem">{{ item.stemSnapshot }}</div>
              <div v-if="parseReviewOptions(item.optionsJson).length" class="review-options">
                <div
                  v-for="option in parseReviewOptions(item.optionsJson)"
                  :key="option.label"
                  class="review-option"
                >
                  <span class="review-option-label">{{ option.label }}</span>
                  <span>{{ option.text }}</span>
                </div>
              </div>
              <div class="review-answer">学生答案: {{ reviewStudentAnswerText(item) }}</div>
              <div class="review-answer">参考答案: {{ item.standardAnswer || '未提供' }}</div>
              <div v-if="hasReviewImages(item)" class="review-images">
                <a-image
                  v-for="(url, idx) in parseReviewImageUrls(item.imageUrlsJson)"
                  :key="url"
                  :src="url"
                  :width="96"
                  :height="72"
                  :alt="`第${item.questionNo || ''}题图片${idx + 1}`"
                />
              </div>
            </div>
            <span class="review-suggest">{{ reviewJudgmentText(item) }}</span>
            <a-input-number v-model:value="reviewScoreMap[item.id]" :min="0" :max="item.fullScore || 100" size="small" />
          </div>
          <a-textarea
            v-model:value="reviewRemark"
            :auto-size="{ minRows: 3, maxRows: 6 }"
            placeholder="输入批改评语"
          />
          <div class="review-actions">
            <a-button
              :loading="reviewRegrading"
              @click="regradeStudentSubmission"
            >
              重新 AI 判定
            </a-button>
            <a-button @click="generateReviewRemark" :loading="commentGenerating">生成评语</a-button>
            <a-button type="primary" @click="submitStudentReview" :loading="reviewSubmitting">保存批改</a-button>
          </div>
        </div>
        <a-empty v-else-if="!studentReportLoading" description="暂无作答报告" />
      </a-spin>
    </a-drawer>

    <!-- 提交详情弹窗 -->
    <a-modal
      v-model:open="showCodeModal"
      width="1100px"
      :footer="null"
      centered
      class="bank-modal"
    >
      <template #title>
        <div class="modal-custom-title">
          <file-text-outlined class="m-icon" />
          <span>{{ codeViewTitle }}</span>
        </div>
      </template>

      <div v-if="activeSubmissionDetail" class="report-wrapper">
        <div class="report-meta-bar">
          <span class="rmb-chip"><strong>语言</strong> {{ activeSubmissionDetail.language }}</span>
          <span class="rmb-chip"><strong>通过率</strong> {{ activeSubmissionDetail.passedCount ?? 0 }} / {{ activeSubmissionDetail.totalCount ?? 0 }}</span>
          <span class="rmb-chip"><strong>测试分</strong> {{ activeSubmissionDetail.testScore ?? 0 }} 分</span>
          <span class="rmb-chip"><strong>AI 分</strong> {{ activeSubmissionDetail.aiScore ?? 0 }} 分</span>
          <span class="rmb-chip">
            <strong>总分</strong>
            <span class="chip-score" :class="(activeSubmissionDetail.finalScore || 0) >= 60 ? 'pass' : 'fail'">
              {{ activeSubmissionDetail.finalScore ?? 0 }}
            </span>
          </span>
          <span class="rmb-chip"><strong>提交时间</strong> {{ formatTime(activeSubmissionDetail.createTime) }}</span>
        </div>

        <div class="report-content-grid">
          <div class="report-text-section">
            <div class="rts-header">
              <file-text-outlined class="rts-icon" />
              <span>提交的代码</span>
            </div>
            <div class="report-content code-content">
              <pre class="detail-code-block">{{ activeSubmissionDetail.code || '（无代码记录）' }}</pre>
            </div>
          </div>

          <div class="report-text-section">
            <div class="rts-header">
              <bulb-outlined class="rts-icon" />
              <span>AI 评估报告</span>
            </div>
            <div class="report-content doc-style">
              <div v-if="activeSubmissionDetail.aiReviewMd" class="detail-ai-review md-body" v-html="renderMd(activeSubmissionDetail.aiReviewMd)"></div>
              <div v-else class="empty-hint" style="padding: 20px 0; color: #94a3b8; text-align: center;">
                <p>暂无 AI 评估报告</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { TeamOutlined, FormOutlined, CodeOutlined, PlusOutlined, CheckCircleFilled, FileTextOutlined, BulbOutlined, BookOutlined, CheckCircleOutlined, WarningOutlined, CloseCircleOutlined, SendOutlined, ClockCircleOutlined, QuestionCircleOutlined, SearchOutlined } from '@ant-design/icons-vue'
import MarkdownIt from 'markdown-it'
import dayjs, { type Dayjs } from 'dayjs'
import { buildSemesterOptions, getCurrentSemesterValue, mergeSemesterOptions } from '@/utils/semester'
import {
  getTeacherProblemList,
  addCodingProblem,
  updateCodingProblem,
  deleteCodingProblem,
  publishCodingProblem,
  getTeacherProblemDetail,
  getProblemSubmissions,
  getMyTeachingClassList,
  getSubmissionDetail,
  deleteSubmission
} from '@/api/coding'

import request from '@/utils/request'
import { useRouter } from 'vue-router'
import HomeworkReportPanel from '@/components/homework/HomeworkReportPanel.vue'

const router = useRouter()

const md = new MarkdownIt({ breaks: true, linkify: true, html: false })
const renderMd = (text: string) => md.render(text || '')

const goToCodingGenerator = () => {
  router.push({ path: '/teacher/ai', query: { type: 'coding' } })
}

const goToQuizGenerator = () => {
  router.push({ path: '/teacher/ai', query: { type: 'quiz' } })
}

// ====== 列表 ======
const problemList = ref<any[]>([])
const loading = ref(false)

const baseSemesterOptions = buildSemesterOptions()
const currentSemesterLabel = getCurrentSemesterValue()

const resolveSemesterLabel = (record: any) => {
  return record?.semesterLabel || record?.semester || record?.term || ''
}

const semesterOptions = computed(() =>
  mergeSemesterOptions(baseSemesterOptions, problemList.value.map(resolveSemesterLabel))
)

const filter = ref({
  keyword: '',
  difficulty: undefined as string | undefined,
  language: undefined as string | undefined,
  semesterLabel: undefined as string | undefined
})
const homeworkFilter = ref({ keyword: '', publishStatus: undefined as string | undefined })
const examFilter = ref({ keyword: '', publishStatus: undefined as string | undefined })

const filteredList = computed(() => {
  const kw = (filter.value.keyword || '').trim().toLowerCase()
  return problemList.value.filter(p => {
    if (filter.value.difficulty && p.difficulty !== filter.value.difficulty) return false
    if (filter.value.language && !(p.languages || []).includes(filter.value.language)) return false
    if (filter.value.semesterLabel) {
      const semester = resolveSemesterLabel(p)
      if (filter.value.semesterLabel === '__unset__') {
        if (semester) return false
      } else if (semester !== filter.value.semesterLabel) {
        return false
      }
    }
    if (kw) {
      const hay = ((p.title || '') + ' ' + (p.description || '')).toLowerCase()
      if (!hay.includes(kw)) return false
    }
    return true
  })
})

const codingColumns = [
  { title: '序号', key: 'index', width: 70, align: 'center', customRender: ({ index }: any) => index + 1 },
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true, align: 'center' },
  { title: '难度', key: 'difficulty', width: 90, align: 'center' },
  { title: '语言', key: 'languages', width: 180, align: 'center' },
  { title: '学期', key: 'semesterLabel', width: 170, align: 'center' },
  { title: '发布状态', key: 'publishStatus', width: 110, align: 'center' },
  { title: '班级', key: 'publishCount', width: 80, align: 'center' },
  { title: '提交人数', key: 'submissionCount', width: 120, align: 'center' },
  { title: '操作', key: 'action', width: 280, align: 'center' }
]

const applyFilter = () => { /* computed */ }
const resetFilter = () => {
  filter.value = { keyword: '', difficulty: undefined, language: undefined, semesterLabel: undefined }
}
const applyHomeworkFilter = () => { /* computed */ }
const resetHomeworkFilter = () => {
  homeworkFilter.value = { keyword: '', publishStatus: undefined }
}
const applyExamFilter = () => { /* computed */ }
const resetExamFilter = () => {
  examFilter.value = { keyword: '', publishStatus: undefined }
}

const loadList = async () => {
  loading.value = true
  try {
    const data = await getTeacherProblemList()
    problemList.value = Array.isArray(data) ? data : (data?.records || [])
  } catch (e) {
    problemList.value = []
  } finally {
    loading.value = false
  }
}

// ====== 新建/编辑表单 ======
const showFormModal = ref(false)
const formSubmitting = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const activeTemplateLang = ref<string>('java')

interface TemplateItem { language: string; starterCode: string; referenceSolution: string }
interface TestCaseItem { input: string; expectedOutput: string; isSample: number; score: number }

const emptyForm = () => ({
  title: '',
  description: '',
  difficulty: 'medium',
  semesterLabel: currentSemesterLabel as string | undefined,
  languages: ['java'] as string[],
  timeLimitMs: 5000,
  memoryLimitKb: 262144,
  templates: [{ language: 'java', starterCode: '', referenceSolution: '' }] as TemplateItem[],
  testCases: [] as TestCaseItem[]
})
const form = ref(emptyForm())

const openAddModal = () => {
  isEdit.value = false
  editingId.value = null
  form.value = emptyForm()
  activeTemplateLang.value = 'java'
  showFormModal.value = true
}

const openEditModal = async (record: any) => {
  isEdit.value = true
  editingId.value = record.id
  formSubmitting.value = true
  try {
    const full = await getTeacherProblemDetail({ problemId: record.id })
    form.value = {
      title: full.title || '',
      description: full.description || '',
      difficulty: full.difficulty || 'medium',
      semesterLabel: resolveSemesterLabel(full) || undefined,
      languages: full.languages || [],
      timeLimitMs: full.timeLimitMs || 5000,
      memoryLimitKb: full.memoryLimitKb || 262144,
      templates: (full.templatesWithSolution || []).map((t: any) => ({
        language: t.language,
        starterCode: t.starterCode || '',
        referenceSolution: t.referenceSolution || ''
      })),
      testCases: (full.allTestCases || []).map((tc: any) => ({
        input: tc.input || '',
        expectedOutput: tc.expectedOutput || '',
        isSample: tc.isSample || 0,
        score: tc.score || 0
      }))
    }
    syncTemplates()
    activeTemplateLang.value = form.value.languages[0] || 'java'
    showFormModal.value = true
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    formSubmitting.value = false
  }
}

const syncTemplates = () => {
  const existing = new Map(form.value.templates.map(t => [t.language, t]))
  form.value.templates = form.value.languages.map(lang =>
    existing.get(lang) || { language: lang, starterCode: '', referenceSolution: '' }
  )
  if (!form.value.languages.includes(activeTemplateLang.value) && form.value.languages.length) {
    activeTemplateLang.value = form.value.languages[0]
  }
}

const getTemplate = (lang: string): TemplateItem => {
  let t = form.value.templates.find(x => x.language === lang)
  if (!t) {
    t = { language: lang, starterCode: '', referenceSolution: '' }
    form.value.templates.push(t)
  }
  return t
}

const setTemplate = (lang: string, field: 'starterCode' | 'referenceSolution', value: string) => {
  const t = getTemplate(lang)
  t[field] = value
}

const addTestCase = () => {
  form.value.testCases.push({ input: '', expectedOutput: '', isSample: 0, score: 10 })
}

const handleSubmitForm = async () => {
  if (!form.value.title.trim() || !form.value.description.trim()) {
    message.warning('请填写标题和描述')
    return
  }
  if (!form.value.languages.length) {
    message.warning('至少选择一种语言')
    return
  }
  if (form.value.testCases.length > 0) {
    for (let i = 0; i < form.value.testCases.length; i++) {
      const tc = form.value.testCases[i]
      if (!tc.expectedOutput.trim()) {
        message.warning(`测试用例 ${i + 1} 的期望输出不能为空`)
        return
      }
    }
    const hasSample = form.value.testCases.some(tc => tc.isSample === 1)
    const hasNonSample = form.value.testCases.some(tc => tc.isSample !== 1)
    if (!hasSample) {
      message.warning('建议至少添加 1 个样例用例供学生参考')
    }
    if (!hasNonSample) {
      message.warning('至少需要 1 个非样例测试用例用于判分')
      return
    }
  }
  formSubmitting.value = true
  try {
    const payload = {
      title: form.value.title,
      description: form.value.description,
      difficulty: form.value.difficulty,
      semesterLabel: form.value.semesterLabel || null,
      languages: form.value.languages,
      timeLimitMs: form.value.timeLimitMs,
      memoryLimitKb: form.value.memoryLimitKb,
      templates: form.value.templates.filter(t => form.value.languages.includes(t.language)),
      testCases: form.value.testCases
    } as any
    if (isEdit.value && editingId.value != null) {
      payload.id = editingId.value
      await updateCodingProblem(payload)
      message.success('已更新')
    } else {
      await addCodingProblem(payload)
      message.success('创建成功')
    }
    showFormModal.value = false
    loadList()
  } catch (e: any) {
    // 请求拦截器已统一展示后端错误，避免同一错误重复弹出。
    console.error('保存编程题失败：', e)
  } finally {
    formSubmitting.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteCodingProblem(id)
    message.success('已删除')
    loadList()
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

// ====== 提交记录 ======
const showSubmissionsDrawer = ref(false)
const submissionsProblem = ref<any>(null)
const submissions = ref<any[]>([])
const submissionsLoading = ref(false)
const submissionColumns = [
  { title: '学生', dataIndex: 'studentName', key: 'studentName', width: 110 },
  { title: '语言', dataIndex: 'language', key: 'language', width: 90 },
  { title: '通过率', key: 'passRate', width: 80 },
  { title: '测试分', dataIndex: 'testScore', key: 'testScore', width: 80 },
  { title: 'AI分', dataIndex: 'aiScore', key: 'aiScore', width: 80 },
  { title: '最终', key: 'finalScore', width: 80 },
  { title: '提交时间', key: 'createTime', width: 140 },
  { title: '操作', key: 'codeAction', width: 150 }
]

const openSubmissions = async (record: any) => {
  submissionsProblem.value = record
  submissionsLoading.value = true
  submissions.value = []
  showSubmissionsDrawer.value = true
  try {
    const data = await getProblemSubmissions({ problemId: record.id })
    submissions.value = Array.isArray(data) ? data : []
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    submissionsLoading.value = false
  }
}

const formatTime = (t: any) => t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
const disabledDate = (current: dayjs.Dayjs) => current && current.isBefore(dayjs().startOf('day'))

// ====== 发布 ======
const showPublishModal = ref(false)
const publishRecord = ref<any>(null)
const publishing = ref(false)
const publishForm = ref<{ classIds: number[]; deadline: Dayjs | null }>({ classIds: [], deadline: null })
const classOptions = ref<{ label: string; value: number }[]>([])
const classListLoading = ref(false)

const loadClassList = async () => {
  classListLoading.value = true
  try {
    const data = await getMyTeachingClassList()
    const list = Array.isArray(data) ? data : []
    classOptions.value = list.map((c: any) => ({
      label: `${c.name}${c.major ? ' · ' + c.major : ''} (${c.studentCount || 0}人)`,
      value: c.id
    }))
  } catch (e) {
    classOptions.value = []
  } finally {
    classListLoading.value = false
  }
}

const filterClassOption = (input: string, option: any) =>
  (option?.label || '').toString().toLowerCase().includes(input.toLowerCase())

const openPublish = (record: any) => {
  publishRecord.value = record
  publishForm.value = { classIds: [], deadline: null }
  showPublishModal.value = true
  if (!classOptions.value.length) loadClassList()
}

const handlePublishConfirm = async () => {
  if (!publishForm.value.classIds.length) {
    message.warning('请至少选择一个班级')
    return
  }
  publishing.value = true
  try {
    await publishCodingProblem({
      problemId: publishRecord.value.id,
      classIds: publishForm.value.classIds,
      deadline: publishForm.value.deadline ? publishForm.value.deadline.toDate() : null
    })
    message.success('发布成功')
    showPublishModal.value = false
    loadList()
  } catch (e: any) {
    message.error(e.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

const difficultyColor = (d: string) => d === 'easy' ? 'green' : d === 'hard' ? 'red' : 'orange'
const difficultyLabel = (d: string) => d === 'easy' ? '简单' : d === 'hard' ? '困难' : '中等'


// ====== 提交详情查看 ======
const showCodeModal = ref(false)
const codeViewTitle = ref('')
const activeSubmissionDetail = ref<any>(null)
const viewSubmissionCode = async (record: any) => {
  try {
    const data = await getSubmissionDetail({ submissionId: record.id })
    activeSubmissionDetail.value = data
    codeViewTitle.value = `${record.studentName || '学生'} · ${record.language} · ${record.finalScore}分`
    showCodeModal.value = true
  } catch (e: any) {
    message.error(e.message || '获取详情失败')
  }
}

const handleDeleteSubmission = async (submissionId: number) => {
  try {
    await deleteSubmission(submissionId)
    message.success('已删除')
    submissions.value = submissions.value.filter((s: any) => s.id !== submissionId)
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

// ====== 试卷管理 (课后作业 + 考试试卷) ======
const activeTab = ref('coding')
const quizList = ref<any[]>([])
const quizLoading = ref(false)

const homeworkColumns = [
  { title: '序号', key: 'index', width: 70, align: 'center', customRender: ({ index }: any) => index + 1 },
  { title: '试卷标题', dataIndex: 'title', key: 'title', ellipsis: true, align: 'center' },
  { title: '题目数', key: 'questionCount', width: 100, align: 'center' },
  { title: '创建时间', key: 'createTime', width: 180, align: 'center' },
  { title: '发布状态', key: 'isPublished', width: 100, align: 'center' },
  { title: '操作', key: 'action', width: 300, align: 'center' }
]

const examQuizColumns = [
  { title: '序号', key: 'index', width: 70, align: 'center', customRender: ({ index }: any) => index + 1 },
  { title: '试卷标题', dataIndex: 'title', key: 'title', ellipsis: true, align: 'center' },
  { title: '题目数', key: 'questionCount', width: 100, align: 'center' },
  { title: '创建时间', key: 'createTime', width: 180, align: 'center' },
  { title: '发布状态', key: 'isPublished', width: 100, align: 'center' },
  { title: '操作', key: 'action', width: 280, align: 'center' }
]

const homeworkQuizList = computed(() =>
  quizList.value
    .filter(q => q.scenario === '课后作业')
    .map(q => ({
      ...q,
      homeworkPublished: monitorAssignmentMap.value.has(q.id)
    }))
)

const examQuizList = computed(() =>
  quizList.value
    .filter(q => q.scenario === '考试试卷')
    .map(q => ({
      ...q,
      examPublished: examAssignmentMap.value.has(q.id)
    }))
)

const filteredHomeworkList = computed(() => {
  const kw = (homeworkFilter.value.keyword || '').trim().toLowerCase()
  return homeworkQuizList.value.filter(q => {
    if (homeworkFilter.value.publishStatus === 'published' && !q.homeworkPublished) return false
    if (homeworkFilter.value.publishStatus === 'unpublished' && q.homeworkPublished) return false
    if (kw && !(q.title || '').toLowerCase().includes(kw)) return false
    return true
  })
})

const filteredExamList = computed(() => {
  const kw = (examFilter.value.keyword || '').trim().toLowerCase()
  return examQuizList.value.filter(q => {
    if (examFilter.value.publishStatus === 'published' && !q.examPublished) return false
    if (examFilter.value.publishStatus === 'unpublished' && q.examPublished) return false
    if (kw && !(q.title || '').toLowerCase().includes(kw)) return false
    return true
  })
})

const loadQuizList = async () => {
  quizLoading.value = true
  try {
    const data = await request.get('/ai/resource/quiz/list')
    quizList.value = Array.isArray(data) ? data : []
  } catch (e: any) {
    message.error(e.message || '加载试卷列表失败')
    quizList.value = []
  } finally {
    quizLoading.value = false
  }
}

const onTabChange = (key: string) => {
  if (key === 'homework' || key === 'exam') {
    loadQuizList()
  }
  if (key === 'homework') {
    loadMonitorAssignments()
  }
  if (key === 'exam') {
    loadExamAssignments()
  }
}

// ====== 试卷预览 ======
const showQuizPreviewModal = ref(false)
const previewQuizRecord = ref<any>(null)

const previewQuizContent = (record: any) => {
  previewQuizRecord.value = record
  showQuizPreviewModal.value = true
}

// ====== 发布课后作业 ======
const showHomeworkPublishModal = ref(false)
const homeworkPublishRecord = ref<any>(null)
const homeworkPublishing = ref(false)
const homeworkPublishForm = ref({
  classId: null as number | null,
  deadline: null as Dayjs | null,
  allowRedo: false,
  maxAttemptCount: 3
})
const homeworkClassOptions = ref<{ label: string; value: number }[]>([])
const homeworkClassListLoading = ref(false)

const loadHomeworkClassList = async () => {
  homeworkClassListLoading.value = true
  try {
    const data = await getMyTeachingClassList()
    const list = Array.isArray(data) ? data : []
    homeworkClassOptions.value = list.map((c: any) => ({
      label: `${c.name}${c.major ? ' · ' + c.major : ''} (${c.studentCount || 0}人)`,
      value: c.id
    }))
  } catch (e: any) {
    homeworkClassOptions.value = []
    message.error(e.message || '加载授课班级失败')
  } finally {
    homeworkClassListLoading.value = false
  }
}

const openHomeworkPublish = (record: any) => {
  homeworkPublishRecord.value = record
  homeworkPublishForm.value = {
    classId: null,
    deadline: null,
    allowRedo: false,
    maxAttemptCount: 3
  }
  showHomeworkPublishModal.value = true
  if (!homeworkClassOptions.value.length) loadHomeworkClassList()
}

const handleHomeworkPublish = async () => {
  if (!homeworkPublishForm.value.classId) {
    message.warning('请选择一个班级')
    return
  }
  homeworkPublishing.value = true
  try {
    await request.post('/homework/assignment/publish', {
      quizResourceId: homeworkPublishRecord.value.id,
      classId: homeworkPublishForm.value.classId,
      title: homeworkPublishRecord.value.title,
      teacherNote: '',
      answerMode: 'online',
      imageGranularity: 'per_question',
      gradingMode: 'auto',
      deadline: homeworkPublishForm.value.deadline ? homeworkPublishForm.value.deadline.toDate() : null,
      allowRedo: homeworkPublishForm.value.allowRedo ? 1 : 0,
      maxAttemptCount: homeworkPublishForm.value.allowRedo ? homeworkPublishForm.value.maxAttemptCount : 1
    })
    message.success('课后作业发布成功')
    showHomeworkPublishModal.value = false
    loadQuizList()
    loadMonitorAssignments()
  } catch (e: any) {
    message.error(e.message || '发布失败')
  } finally {
    homeworkPublishing.value = false
  }
}

// ====== 删除试卷 ======
const handleDeleteQuiz = async (id: number) => {
  try {
    await request.post(`/ai/resource/delete/${id}`)
    message.success('已删除')
    quizList.value = quizList.value.filter(q => q.id !== id)
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

// ====== 作业发布状态与完成情况 ======
const monitorAssignmentMap = ref<Map<number, any[]>>(new Map())

const loadMonitorAssignments = async () => {
  try {
    const data = await request.get('/homework/teacher/monitor/list')
    const list = Array.isArray(data) ? data : []
    const map = new Map<number, any[]>()
    for (const item of list) {
      const key = item.quizResourceId
      if (key == null) continue
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(item)
    }
    monitorAssignmentMap.value = map
  } catch (e) {
    // silently fail
  }
}

// ====== 考试发布状态 ======
const examAssignmentMap = ref<Map<number, any[]>>(new Map())

const loadExamAssignments = async () => {
  try {
    const data = await request.get('/homework/teacher/monitor/list')
    const list = Array.isArray(data) ? data : []
    const map = new Map<number, any[]>()
    for (const item of list) {
      if (item.assignmentType !== 'exam') continue
      const key = item.quizResourceId
      if (key == null) continue
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(item)
    }
    examAssignmentMap.value = map
  } catch (e) {
    // silently fail
  }
}

// ====== 发布考试 ======
const showExamPublishModal = ref(false)
const examPublishRecord = ref<any>(null)
const examPublishing = ref(false)
const examPublishForm = ref({
  classId: null as number | null,
  durationMinutes: 60,
  deadline: null as Dayjs | null,
})
const examClassOptions = ref<{ label: string; value: number }[]>([])
const examClassListLoading = ref(false)

const loadExamClassList = async () => {
  examClassListLoading.value = true
  try {
    const data = await getMyTeachingClassList()
    const list = Array.isArray(data) ? data : []
    examClassOptions.value = list.map((c: any) => ({
      label: `${c.name}${c.major ? ' · ' + c.major : ''} (${c.studentCount || 0}人)`,
      value: c.id
    }))
  } catch (e: any) {
    examClassOptions.value = []
    message.error(e.message || '加载授课班级失败')
  } finally {
    examClassListLoading.value = false
  }
}

const openExamPublish = (record: any) => {
  examPublishRecord.value = record
  examPublishForm.value = { classId: null, durationMinutes: 60, deadline: null }
  showExamPublishModal.value = true
  if (!examClassOptions.value.length) loadExamClassList()
}

const handleExamPublish = async () => {
  if (!examPublishForm.value.classId) {
    message.warning('请选择一个班级')
    return
  }
  if (!examPublishForm.value.durationMinutes || examPublishForm.value.durationMinutes < 5) {
    message.warning('考试时长至少5分钟')
    return
  }
  examPublishing.value = true
  try {
    await request.post('/homework/assignment/publish', {
      quizResourceId: examPublishRecord.value.id,
      classId: examPublishForm.value.classId,
      title: examPublishRecord.value.title,
      assignmentType: 'exam',
      durationMinutes: examPublishForm.value.durationMinutes,
      deadline: examPublishForm.value.deadline ? examPublishForm.value.deadline.toDate() : null,
    })
    message.success('考试发布成功')
    showExamPublishModal.value = false
    loadQuizList()
    loadExamAssignments()
  } catch (e: any) {
    message.error(e.message || '发布失败')
  } finally {
    examPublishing.value = false
  }
}

// ====== 批阅考试 ======
const showExamGradingDrawer = ref(false)
const examGradingRecord = ref<any>(null)
const examGradingStudents = ref<any[]>([])
const examGradingLoading = ref(false)
const examSubmissionsByClass = ref<any[]>([])
const activeExamClassKey = ref<string>('')

const examGradingColumns = [
  { title: '学生', dataIndex: 'studentName', key: 'studentName', width: 120 },
  { title: '状态', key: 'submitStatus', width: 90 },
  { title: '提交时间', key: 'submitTime', width: 160 },
  { title: '总分', key: 'totalScore', width: 70 },
  { title: '操作', key: 'action', width: 80 }
]

const openExamGrading = async (record: any) => {
  examGradingRecord.value = record
  showExamGradingDrawer.value = true
  examGradingLoading.value = true
  examGradingStudents.value = []
  try {
    const assignments = examAssignmentMap.value.get(record.id) || []
    if (assignments.length === 0) {
      message.warning('该考试尚未发布')
      return
    }
    const data = await request.get('/exam/teacher/submissions', {
      params: { assignmentId: assignments[0].assignmentId }
    })
    const list = Array.isArray(data) ? data : []
    examGradingStudents.value = list
    examSubmissionsByClass.value = [{ classId: assignments[0].classId || 'default', submissions: list }]
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    examGradingLoading.value = false
  }
}

const onExamClassTabChange = (key: string) => {
  // handle if multiple classes
}

const showGradingModal = ref(false)
const gradingStudent = ref<any>(null)
const gradingDetails = ref<any[]>([])
const processedGradingDetails = ref<any[]>([])
const gradingContentSnapshot = ref('')
const gradeScores = ref<number[]>([])
const gradeTeacherRemark = ref('')
const gradeDetailLoading = ref(false)
const gradeSaving = ref(false)
const aiGradingLoading = ref(false)
const gradeCommentGenerating = ref(false)
const isGradePreview = computed(() => gradingStudent.value?.submitStatus === 'completed')

const typeLabelMap: Record<string, string> = {
  radio: '单选题',
  checkbox: '多选题',
  judge: '判断题',
  fill: '填空题',
  text: '简答题',
  image: '图片题'
}

const gradingTypeOrder = ['radio', 'checkbox', 'judge', 'fill', 'text', 'image', 'unknown']
const gradingSectionTitleMap: Record<string, string> = {
  radio: '一、单选题',
  checkbox: '二、多选题',
  judge: '三、判断题',
  fill: '四、填空题',
  text: '五、简答题',
  image: '六、图片题',
  unknown: '其他题目'
}

const normalizeQuestionTypeKey = (type?: string) => {
  const key = String(type || '').trim()
  return gradingTypeOrder.includes(key) ? key : 'unknown'
}

const questionNoValue = (value: any, fallback: number) => {
  const matched = String(value ?? '').match(/\d+/)
  return matched ? Number(matched[0]) : fallback
}

const normalizeReviewAnswer = (value: any) =>
  String(value ?? '')
    .replace(/<[^>]*>/g, '')
    .replace(/^\s*(答案|参考答案|标准答案)\s*[:：]?\s*/u, '')
    .replace(/[，,。.;；、：:\s]/g, '')
    .toUpperCase()

const normalizeReviewJudgeAnswer = (value: any) => {
  const text = normalizeReviewAnswer(value)
  if (['正确', '对', 'TRUE', 'T', '√'].includes(text)) return '正确'
  if (['错误', '错', 'FALSE', 'F', '×', 'X'].includes(text)) return '错误'
  return text
}

const extractReviewCommentAnswer = (comment: any, label: string) => {
  const escapedLabel = label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = String(comment || '').match(new RegExp(`${escapedLabel}\\s*为\\s*([^，,。；;\\s]+)`))
  return match?.[1] || ''
}

const reviewStudentAnswerForMatch = (item: any) =>
  item?.studentAnswer || extractReviewCommentAnswer(item?.aiComment, '学生答案')

const reviewStandardAnswerForMatch = (item: any) =>
  item?.standardAnswer || extractReviewCommentAnswer(item?.aiComment, '参考答案')

const isJudgeLikeReviewAnswer = (value: any) =>
  ['正确', '对', 'TRUE', 'T', '√', '错误', '错', 'FALSE', 'F', '×', 'X'].includes(normalizeReviewJudgeAnswer(value))

const reviewAnswerMatchesStandard = (item: any) => {
  const studentAnswer = reviewStudentAnswerForMatch(item)
  const standardAnswer = reviewStandardAnswerForMatch(item)
  if (!studentAnswer || !standardAnswer) return false
  const questionType = String(item?.questionType || '')
  const judgeLike = questionType === 'judge' || questionType.includes('判断') ||
    isJudgeLikeReviewAnswer(studentAnswer) || isJudgeLikeReviewAnswer(standardAnswer)
  if (judgeLike) {
    return normalizeReviewJudgeAnswer(studentAnswer) === normalizeReviewJudgeAnswer(standardAnswer)
  }
  return normalizeReviewAnswer(studentAnswer) === normalizeReviewAnswer(standardAnswer)
}

const resolveReviewScore = (item: any) => {
  const score = Number(item?.score ?? 0)
  const fullScore = Number(item?.fullScore ?? 0)
  if (score <= 0 && fullScore > 0 && reviewAnswerMatchesStandard(item)) {
    return fullScore
  }
  if (fullScore > 0 && score > fullScore) {
    return fullScore
  }
  return item?.score
}

const gradeQuestionWeight = (type?: string) => {
  const key = String(type || '')
  if (key === 'radio' || key === 'judge') return 1
  if (key === 'checkbox') return 1.2
  if (key === 'fill') return 1.5
  if (key === 'text' || key === 'image') return 3
  return 2
}

const distributeGradeFullScores = (details: any[]) => {
  const list = details || []
  if (!list.length) return []
  const existingScores = list.map((item: any) => Number(item?.fullScore ?? 0))
  const existingTotal = existingScores.reduce((sum: number, score: number) => sum + (score > 0 ? score : 0), 0)
  const allHaveScore = existingScores.every((score: number) => score > 0)
  if (allHaveScore && existingTotal === 100) {
    return existingScores
  }

  const weights = list.map((item: any) => gradeQuestionWeight(item?.questionType))
  const totalWeight = weights.reduce((sum: number, weight: number) => sum + weight, 0) || list.length
  const exactScores = weights.map((weight: number) => weight * 100 / totalWeight)
  const minScore = list.length <= 100 ? 1 : 0
  const scores = exactScores.map((score: number) => Math.max(minScore, Math.floor(score)))
  let assigned = scores.reduce((sum: number, score: number) => sum + score, 0)

  while (assigned > 100) {
    let idx = -1
    scores.forEach((score: number, i: number) => {
      if (score > minScore && (idx < 0 || score > scores[idx])) idx = i
    })
    if (idx < 0) break
    scores[idx]--
    assigned--
  }
  while (assigned < 100) {
    let idx = 0
    exactScores.forEach((score: number, i: number) => {
      const currentFraction = score - Math.floor(score)
      const bestFraction = exactScores[idx] - Math.floor(exactScores[idx])
      if (currentFraction > bestFraction || (currentFraction === bestFraction && scores[i] < scores[idx])) {
        idx = i
      }
    })
    scores[idx]++
    assigned++
  }
  return scores
}

const buildProcessedDetails = (details: any[]) => {
  const sectionCounters: Record<string, number> = {}
  const distributedFullScores = distributeGradeFullScores(details)
  return [...(details || [])]
    .map((item: any, idx: number) => ({
      ...item,
      fullScore: distributedFullScores[idx] ?? item.fullScore,
      _rawIndex: idx,
      _typeKey: normalizeQuestionTypeKey(item.questionType),
      _sortNo: questionNoValue(item.questionNo, idx + 1)
    }))
    .sort((a: any, b: any) => {
      const typeDiff = gradingTypeOrder.indexOf(a._typeKey) - gradingTypeOrder.indexOf(b._typeKey)
      if (typeDiff !== 0) return typeDiff
      return a._sortNo - b._sortNo || a._rawIndex - b._rawIndex
    })
    .map((item: any, idx: number) => {
      const typeKey = item._typeKey
      sectionCounters[typeKey] = (sectionCounters[typeKey] || 0) + 1
      return {
        ...item,
        score: resolveReviewScore(item),
        _scoreIndex: idx,
        _sectionNo: sectionCounters[typeKey],
        _displayType: questionTypeText(item.questionType),
        _displayNo: item.questionNo ?? idx + 1
      }
    })
}

const processedGradingSections = computed(() => {
  const groups = new Map<string, any[]>()
  for (const detail of processedGradingDetails.value || []) {
    const key = normalizeQuestionTypeKey(detail.questionType)
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(detail)
  }
  return Array.from(groups.entries()).map(([key, items]) => ({
    key,
    title: gradingSectionTitleMap[key] || gradingSectionTitleMap.unknown,
    items
  }))
})

const openGradeStudent = (record: any) => {
  gradingStudent.value = record
  gradingDetails.value = record.details || []
  processedGradingDetails.value = buildProcessedDetails(record.details || [])
  gradeScores.value = processedGradingDetails.value.map((d: any) => d.score ?? 0)
  gradeTeacherRemark.value = record.teacherRemark || ''
  gradingContentSnapshot.value = record.contentSnapshot || ''
  showGradingModal.value = true
}

const triggerAiGrading = async () => {
  if (!gradingStudent.value?.submissionId) {
    message.warning('提交记录ID不存在')
    return
  }
  if (gradingStudent.value.submitStatus === 'completed') {
    message.info('该答卷已批阅完成')
    return
  }
  aiGradingLoading.value = true
  try {
    await request.post(`/exam/teacher/auto-grade/${gradingStudent.value.submissionId}`, {}, { timeout: 60000 })
    message.success('AI批阅完成')
    if (examGradingRecord.value) {
      await openExamGrading(examGradingRecord.value)
    }
    const updated = examGradingStudents.value.find(
      (s: any) => s.submissionId === gradingStudent.value.submissionId
    )
    if (updated) {
      openGradeStudent(updated)
    }
  } catch (e: any) {
    message.error(e?.message || 'AI批阅失败')
  } finally {
    aiGradingLoading.value = false
  }
}

const currentExamGradeDetailsPayload = () =>
  (processedGradingDetails.value || []).map((d: any, idx: number) => ({
    id: d.id,
    score: gradeScores.value[idx] ?? 0,
  }))

const generateExamGradeRemark = async () => {
  if (!gradingStudent.value?.submissionId) {
    message.warning('提交记录ID不存在')
    return
  }
  if (isGradePreview.value) {
    message.info('已批阅答卷仅支持预览')
    return
  }
  gradeCommentGenerating.value = true
  try {
    const comment = await request.post<string, string>('/exam/teacher/comment-generate', {
      submissionId: gradingStudent.value.submissionId,
      teacherRemark: gradeTeacherRemark.value,
      details: currentExamGradeDetailsPayload(),
    }, { timeout: 60000 })
    gradeTeacherRemark.value = comment || gradeTeacherRemark.value
    message.success('评语已生成，可继续修改')
  } catch (e: any) {
    message.error(e.message || '生成评语失败')
  } finally {
    gradeCommentGenerating.value = false
  }
}

const saveGrade = async () => {
  if (!gradingStudent.value?.submissionId) {
    message.warning('提交记录ID不存在')
    return
  }
  if (isGradePreview.value) {
    message.info('该答卷已批阅完成，不能重复批阅')
    return
  }
  gradeSaving.value = true
  try {
    await request.post('/exam/teacher/grade', {
      submissionId: gradingStudent.value.submissionId,
      teacherRemark: gradeTeacherRemark.value,
      details: currentExamGradeDetailsPayload(),
    })
    message.success('批阅保存成功')
    showGradingModal.value = false
    if (examGradingRecord.value) {
      openExamGrading(examGradingRecord.value)
    }
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    gradeSaving.value = false
  }
}

// ====== 完成情况弹窗 ======
const showCompletionModal = ref(false)
const completionQuizTitle = ref('')
const completionAssignments = ref<any[]>([])
const completionActiveAssignment = ref<any>(null)
const completionStudents = ref<any[]>([])
const completionStudentsLoading = ref(false)
const completionStatusFilter = ref('all')

const filteredCompletionStudents = computed(() => {
  if (completionStatusFilter.value === 'all') return completionStudents.value
  return completionStudents.value.filter((item: any) => item.submitStatus === completionStatusFilter.value)
})

const openCompletionView = async (record: any) => {
  completionQuizTitle.value = record.title || ''
  const assignments = monitorAssignmentMap.value.get(record.id) || []
  completionAssignments.value = assignments
  completionActiveAssignment.value = assignments.length > 0 ? assignments[0] : null
  showCompletionModal.value = true
  if (completionActiveAssignment.value) {
    await loadCompletionStudents(completionActiveAssignment.value.assignmentId)
  }
}

const loadCompletionStudents = async (assignmentId: number) => {
  completionStudentsLoading.value = true
  completionStudents.value = []
  try {
    const data = await request.get('/homework/teacher/monitor/detail', {
      params: { assignmentId }
    })
    completionStudents.value = Array.isArray(data) ? data : []
  } catch (e: any) {
    message.error(e.message || '加载学生作答情况失败')
  } finally {
    completionStudentsLoading.value = false
  }
}

const submitStatusText = (status: string) => {
  if (status === 'review_pending') return '待批改'
  const map: Record<string, string> = {
    completed: '已完成',
    judging: '待批改',
    submitted: '已提交',
    failed: '批改失败',
    pending: '未提交'
  }
  return map[status] || status
}

const submitStatusClass = (status: string) => {
  if (status === 'completed') return 'status-completed'
  if (status === 'review_pending') return 'status-review'
  if (status === 'failed') return 'status-failed'
  if (status === 'judging' || status === 'submitted') return 'status-pending'
  return 'status-plain'
}

const completionStudentColumns = [
  { title: '学生姓名', dataIndex: 'studentName', key: 'studentName', width: 120 },
  { title: '作答状态', key: 'submitStatus', width: 110 },
  { title: '提交时间', key: 'submitTime', width: 160 },
  { title: '总分', key: 'totalScore', width: 90 },
  { title: '待批题数', key: 'pendingReviewQuestionCount', width: 90 },
  { title: '正确题数', key: 'correctCount', width: 90 },
  { title: '错误题数', key: 'wrongCount', width: 90 },
  { title: '操作', key: 'action', width: 100 }
]

const activeAssignmentKey = ref<string>('')

const onAssignmentTabChange = (key: string) => {
  const assignment = completionAssignments.value.find(a => String(a.assignmentId) === key)
  if (assignment) {
    completionActiveAssignment.value = assignment
    loadCompletionStudents(assignment.assignmentId)
  }
}

watch(completionAssignments, (assignments) => {
  if (assignments.length > 0 && !activeAssignmentKey.value) {
    activeAssignmentKey.value = String(assignments[0].assignmentId)
  }
})

// ====== 学生报告抽屉 ======
const showStudentReportDrawer = ref(false)
const studentReportData = ref<any>(null)
const studentReportName = ref('')
const studentReportLoading = ref(false)
const reviewScoreMap = ref<Record<string, number>>({})
const reviewRemark = ref('')
const reviewSubmitting = ref(false)
const commentGenerating = ref(false)
const reviewRegrading = ref(false)

const studentReportDrawerTitle = computed(() => {
  const action = studentReportData.value?.submission?.submitStatus === 'review_pending' ? '批改作业' : '作答报告'
  return `${studentReportName.value} 的${action}`
})

const openStudentReport = async (student: any) => {
  if (!student.submissionId) {
    message.warning('该学生暂无作答报告')
    return
  }
  studentReportLoading.value = true
  showStudentReportDrawer.value = true
  studentReportName.value = student.studentName || ''
  studentReportData.value = null
  try {
    const data = await request.get('/homework/teacher/monitor/submission-report', {
      params: { submissionId: student.submissionId }
    })
    studentReportData.value = data
    hydrateReviewForm(data)
  } catch (e: any) {
    message.error(e.message || '加载报告失败')
  } finally {
    studentReportLoading.value = false
  }
}

const hydrateReviewForm = (report: any) => {
  const map: Record<string, number> = {}
  ;(report?.details || []).forEach((item: any) => {
    if (item?.id != null) {
      map[String(item.id)] = item.score ?? 0
    }
  })
  reviewScoreMap.value = map
  reviewRemark.value = report?.submission?.teacherRemark || ''
}

const parseReviewImageUrls = (raw: any): string[] => {
  if (!raw) return []
  if (Array.isArray(raw)) return raw.filter(Boolean)
  try {
    const parsed = JSON.parse(String(raw))
    return Array.isArray(parsed) ? parsed.filter(Boolean) : []
  } catch {
    return []
  }
}

const hasReviewImages = (item: any) => parseReviewImageUrls(item?.imageUrlsJson).length > 0

const questionTypeText = (type?: string) => typeLabelMap[String(type || '')] || '未知题型'

const parseReviewOptions = (raw?: string) => {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .map((item: any) => ({
        label: String(item?.label ?? item?.key ?? '').trim(),
        text: String(item?.text ?? item?.content ?? item?.value ?? '').trim()
      }))
      .filter((item: any) => item.label && item.text)
  } catch {
    return []
  }
}

const reviewStudentAnswerText = (item: any) => {
  const answer = String(item?.studentAnswer || '').trim()
  if (answer) return answer
  if (hasReviewImages(item)) return '见图片作答'
  return '（未作答）'
}

const isObjectiveReviewItem = (item: any) =>
  ['radio', 'single', 'choice', 'checkbox', 'multiple', 'judge', 'true_false', 'truefalse', 'fill']
    .includes(String(item?.questionType || '').toLowerCase())

const reviewJudgmentText = (item: any) => {
  if (hasReviewImages(item)) return '待教师判分'
  if (item?.aiComment) return item.aiComment
  const source = isObjectiveReviewItem(item) ? '系统自动判定' : 'AI判定'
  if (item?.isCorrect === 1) return `${source}：正确`
  if (item?.isCorrect === 0) return `${source}：错误`
  return item?.aiComment || '待教师判分'
}

const currentReviewDetailsPayload = () =>
  (studentReportData.value?.details || []).map((item: any) => ({
    id: item.id,
    score: reviewScoreMap.value[String(item.id)] ?? 0
  }))

const submitStudentReview = async () => {
  const submissionId = studentReportData.value?.submission?.id
  if (!submissionId) return
  reviewSubmitting.value = true
  try {
    await request.post('/homework/teacher/submission/review', {
      submissionId,
      teacherRemark: reviewRemark.value,
      details: currentReviewDetailsPayload()
    })
    message.success('批改已保存')
    await openStudentReport({ submissionId, studentName: studentReportName.value })
    if (completionActiveAssignment.value?.assignmentId) {
      await loadCompletionStudents(completionActiveAssignment.value.assignmentId)
    }
  } catch (e: any) {
    message.error(e.message || '批改保存失败')
  } finally {
    reviewSubmitting.value = false
  }
}

const regradeStudentSubmission = async () => {
  const submissionId = studentReportData.value?.submission?.id
  if (!submissionId) {
    message.warning('提交记录不存在')
    return
  }
  reviewRegrading.value = true
  try {
    await request.post(
      `/homework/teacher/submission/regrade-ai/${submissionId}`,
      {},
      { timeout: 120000 }
    )
    message.success('已重新判定，请确认分数后保存批改')
    await openStudentReport({ submissionId, studentName: studentReportName.value })
  } catch (e: any) {
    message.error(e?.message || '重新判定失败，请稍后重试')
  } finally {
    reviewRegrading.value = false
  }
}

const generateReviewRemark = async () => {
  const submissionId = studentReportData.value?.submission?.id
  if (!submissionId) return
  commentGenerating.value = true
  try {
    const comment = await request.post<string, string>('/homework/teacher/submission/comment-generate', {
      submissionId,
      teacherRemark: reviewRemark.value,
      details: currentReviewDetailsPayload()
    })
    reviewRemark.value = comment || reviewRemark.value
    message.success('评语已生成，可继续修改')
  } catch (e: any) {
    message.error(e.message || '生成评语失败')
  } finally {
    commentGenerating.value = false
  }
}

onMounted(() => {
  loadList()
  loadQuizList()
  loadMonitorAssignments()
  loadExamAssignments()
})
</script>

<style scoped>
/* ===== 基础容器 ===== */
.modern-page {
  font-family: 'Plus Jakarta Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  padding: 20px 28px 24px;
  height: 100%;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  gap: 0;
}

/* ===== 页面标题 ===== */


.title-group h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  color: #8b5cf6;
  font-size: 30px;
}

.title-group .subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 15px;
}

.add-btn {
  background: #8b5cf6 !important;
  border-color: #8b5cf6 !important;
  font-weight: 600;
  border-radius: 8px !important;
  height: 40px;
  padding: 0 18px;
  box-shadow: 0 4px 14px rgba(139, 92, 246, 0.25);
}
.add-btn:hover {
  background: #7c3aed !important;
  border-color: #7c3aed !important;
}

/* ===== 统计卡片行 ===== */
.stats-row {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.stat-card {
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: box-shadow 0.2s;
}
.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.04);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}

/* ===== 类型切换：放进标题区，弱化成普通导航 ===== */
/* ===== 页面标题区域优化 (替代原有的 .page-header) ===== */
.page-header-card {
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 10px;
  padding: 20px 24px 0; /* 顶部和两侧内边距，底部设为0让Tabs自然贴底 */
  margin-bottom: 24px; /* 【关键点】大幅增加与下方统计卡片的间距，拉开呼吸感 */
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px; /* 标题和下面 Tabs 之间的呼吸空间 */
}

.title-group h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-group .subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 15px;
}

/* ===== 类型切换 Tabs 优化 ===== */
.header-tabs-wrapper {
  width: 100%;
}

.bank-tabs {
  flex: none;
}
.bank-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}
.bank-tabs :deep(.ant-tabs-nav::before) {
  border-bottom: none !important; /* 隐藏默认的贯穿分割线，让卡片更干净 */
}
.bank-tabs :deep(.ant-tabs-nav-list) {
  background: transparent;
  padding: 0;
  border: none;
}
.quiz-stats-row {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.bank-tabs :deep(.ant-tabs-tab) {
  padding: 8px 0 14px !important;
  margin: 0 32px 0 0 !important; /* 适当拉开各个 Tab 之间的距离 */
  font-weight: 600;
  font-size: 14px; /* 稍微调大一点字体，让导航层级更清晰 */
  color: #64748b;
  border: none !important;
  background: transparent !important;
  transition: color 0.2s;
}
.bank-tabs :deep(.ant-tabs-tab:hover) {
  color: #8b5cf6;
}
.bank-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #7c3aed !important;
}
.bank-tabs :deep(.ant-tabs-ink-bar) {
  height: 3px !important; /* 加粗一点指示条 */
  background: #8b5cf6 !important;
  border-radius: 3px 3px 0 0;
}

/* ===== 统计卡片行（顺手调整一下这部分的下边距） ===== */
.stats-row {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px; /* 原来是14px，改大一点，与下方的搜索栏也拉开距离 */
}

/* ===== Tab 内容区 ===== */
.stats-row.quiz-stats-row {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

:global(.modern-content > .coding-bank-page .page-header-card) {
  margin-bottom: 6px !important;
  padding-bottom: 0 !important;
}

.bank-tabs :deep(.ant-tabs-tab) {
  padding: 10px 0 12px !important;
  margin-right: 38px !important;
  font-size: 16px !important;
  line-height: 1.45 !important;
  font-weight: 700 !important;
  color: #475569;
}

.bank-tabs :deep(.ant-tabs-ink-bar) {
  height: 4px !important;
}

.stats-row .stat-card {
  min-height: 96px;
  padding: 18px 20px;
}

.stats-row .stat-icon {
  width: 44px;
  height: 44px;
  font-size: 20px;
}

.coding-tab-content {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ===== 筛选栏 ===== */
.filter-dashboard {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 10px;
}

.filter-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-item .label {
  font-size: 14.5px;
  line-height: 1.45;
  font-weight: 600;
  color: #475569;
}

/* ===== 表表面板 ===== */
.table-panel {
  flex: 1;
  min-height: 0;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e8eef6;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
}

/* ===== 表格深度美化 ===== */
:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-btn) {
  border-radius: 8px !important;
}

:deep(.ant-input),
:deep(.ant-input-affix-wrapper),
:deep(.ant-select-selector),
:deep(.ant-btn),
:deep(.ant-pagination),
:deep(.ant-select-item-option-content) {
  font-size: 14.5px !important;
  line-height: 1.45 !important;
}

.filter-dashboard :deep(.ant-input),
.filter-dashboard :deep(.ant-input-affix-wrapper input),
.filter-dashboard :deep(.ant-select-selection-item) {
  color: #1f2937 !important;
  font-weight: 500;
}

.filter-dashboard :deep(.ant-input::placeholder),
.filter-dashboard :deep(.ant-input-affix-wrapper input::placeholder),
.filter-dashboard :deep(.ant-select-selection-placeholder) {
  color: #475569 !important;
  opacity: 1 !important;
  font-weight: 500;
}

.filter-dashboard :deep(.bank-search-input.ant-input-affix-wrapper) {
  height: 40px;
  min-height: 40px;
  padding: 0 12px 0 16px;
  align-items: center;
  border-radius: 8px !important;
}

.filter-dashboard :deep(.bank-search-input .ant-input) {
  height: 38px;
  line-height: 38px !important;
  padding: 0;
}

.filter-dashboard :deep(.bank-search-input .ant-input-clear-icon) {
  display: inline-flex;
  align-items: center;
}

.filter-dashboard :deep(.ant-select-selector),
.filter-dashboard :deep(.ant-btn) {
  min-height: 40px;
}

.filter-dashboard :deep(.ant-select-selector) {
  height: 40px !important;
  align-items: center;
}

.filter-dashboard :deep(.ant-select-selection-search-input) {
  height: 38px !important;
}

.filter-dashboard :deep(.ant-select-selection-item),
.filter-dashboard :deep(.ant-select-selection-placeholder) {
  line-height: 38px !important;
}

.bank-search-icon {
  color: #64748b;
  cursor: pointer;
  font-size: 18px;
  transition: color 0.15s;
}

.bank-search-icon:hover {
  color: #147ed9;
}

:deep(.ant-table),
:deep(.ant-table-container) {
  background: transparent !important;
}

:deep(.ant-table-wrapper) {
  font-size: 15px !important;
  line-height: 1.55 !important;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fafc !important;
  color: #475569;
  font-weight: 700;
  font-size: 14px !important;
  line-height: 1.45 !important;
  letter-spacing: 0;
  border-bottom: 2px solid #e8eef6 !important;
  padding: 15px 18px !important;
}
:deep(.ant-table-thead > tr > th::before) {
  display: none !important;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 17px 18px !important;
  border-bottom: 1px solid #f1f5f9 !important;
  color: #334155;
  font-size: 15px !important;
  line-height: 1.55 !important;
  vertical-align: middle !important;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: #fafbfc !important;
}

:deep(.ant-tag) {
  border-radius: 6px !important;
  border: none !important;
  padding: 4px 10px;
  font-weight: 600;
  font-size: 13.5px !important;
  line-height: 1.45 !important;
}

:deep(.ant-table-tbody .ant-btn-link) {
  padding: 5px 9px;
  height: auto;
  border-radius: 6px;
  font-weight: 600;
  font-size: 14px !important;
  line-height: 1.45 !important;
  transition: all 0.15s;
}
:deep(.ant-table-tbody .ant-btn-link:hover) {
  background-color: #eff6ff;
}
:deep(.ant-table-tbody .ant-btn-link.ant-btn-dangerous) {
  color: #ef4444;
}
:deep(.ant-table-tbody .ant-btn-link.ant-btn-dangerous:hover) {
  background-color: #fef2f2;
}

/* 表格 flex 布局 */
:deep(.ant-table-wrapper),
:deep(.ant-spin-nested-loading),
:deep(.ant-spin-container) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

:deep(.ant-table) {
  flex: 1;
  min-height: 0; /* 【关键】让表格知道自己的边界，不要无限撑开 */
  overflow-y: auto; /* 【关键】数据过多时内部自动出现滚动条 */
}

:deep(.ant-table-pagination) {
  margin-top: auto !important;
  padding-top: 16px;
  flex-shrink: 0; /* 【关键】保护分页栏不被挤压，永远固定在最底部 */
}

/* ===== 图标文字对齐 ===== */
.icon-text-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: 600;
  color: #475569;
  font-size: 14.5px;
  line-height: 1.45;
}
.icon-text-cell .anticon {
  margin-right: 0 !important;
  font-size: 15px;
  color: #94a3b8;
}

.semester-cell {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 10px;
  border-radius: 6px;
  background: #eef6ff;
  color: #1d4ed8;
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.4;
}

.muted-cell {
  color: #94a3b8;
  font-weight: 500;
}

/* ===== 测试用例 ===== */
.test-case-row {
  padding: 10px;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #f8fafc;
}

.hint {
  color: #94a3b8;
  font-size: 12px;
  margin-bottom: 8px;
}

/* ===== 提交详情弹窗 ===== */
.modal-custom-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}
.modal-custom-title .m-icon {
  color: #8b5cf6;
}

:global(.bank-modal),
:global(.preview-modal) {
  max-width: calc(100vw - 48px);
}

:global(.bank-modal .ant-modal-content),
:global(.preview-modal .ant-modal-content) {
  border-radius: 10px;
}

:global(.bank-modal .ant-modal-body),
:global(.preview-modal .ant-modal-body) {
  max-height: 680px;
  overflow-y: auto;
}

/* 作业与考试发布使用同一弹窗规格，避免内容数量影响视觉位置。 */
:global(.assignment-publish-modal) {
  max-width: calc(100vw - 32px);
  padding-bottom: 0;
}

:global(.assignment-publish-modal .ant-modal-content) {
  overflow: hidden;
  border: 1px solid #e6ebf2;
  border-radius: 14px;
  box-shadow: 0 20px 56px rgba(15, 23, 42, 0.18);
}

:global(.assignment-publish-modal .ant-modal-header) {
  margin: 0;
  padding: 22px 28px 18px;
  border-bottom: 1px solid #edf1f6;
}

:global(.assignment-publish-modal .ant-modal-title) {
  color: #172033;
  font-size: 18px;
  font-weight: 700;
}

:global(.assignment-publish-modal .ant-modal-body) {
  box-sizing: border-box;
  height: 348px;
  max-height: min(55vh, 348px);
  padding: 20px 28px 8px;
  overflow-y: auto;
}

:global(.assignment-publish-modal .ant-modal-footer) {
  margin: 0;
  padding: 16px 28px 20px;
  border-top: 1px solid #edf1f6;
}

:global(.assignment-publish-modal .ant-modal-footer .ant-btn) {
  min-width: 88px;
  height: 38px;
  border-radius: 8px;
}

.publish-dialog-summary {
  margin: 0 0 20px;
  padding: 12px 14px;
  color: #526078;
  line-height: 1.6;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.publish-dialog-summary strong {
  color: #172033;
}

.publish-dialog-form :deep(.ant-form-item) {
  margin-bottom: 18px;
}

.publish-dialog-form :deep(.ant-form-item-label > label) {
  color: #344054;
  font-weight: 600;
}

@media (max-width: 640px) {
  :global(.assignment-publish-modal .ant-modal-header) {
    padding: 18px 20px 15px;
  }

  :global(.assignment-publish-modal .ant-modal-body) {
    height: auto;
    max-height: 62vh;
    padding: 18px 20px 4px;
  }

  :global(.assignment-publish-modal .ant-modal-footer) {
    padding: 14px 20px 18px;
  }
}

:global(.grading-modal .ant-modal-body) {
  max-height: none;
  overflow: hidden;
}

.report-wrapper {
  height: 680px;
  max-height: 680px;
  overflow-y: auto;
  padding-right: 16px;
  margin-right: -8px;
}
.report-wrapper::-webkit-scrollbar {
  width: 6px;
}
.report-wrapper::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}

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
.rmb-chip .chip-score {
  font-weight: 700;
}
.rmb-chip .chip-score.pass { color: #10B981; }
.rmb-chip .chip-score.fail { color: #EF4444; }

.report-content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  height: calc(100% - 50px);
  min-height: 500px;
}

.report-text-section {
  border: 1px solid #eef2f7;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fff;
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
  flex-shrink: 0;
}

.rts-icon {
  color: #8b5cf6;
  font-size: 16px;
}

.report-content {
  padding: 16px 20px;
  overflow-y: auto;
  flex: 1;
}

.report-content.code-content {
  padding: 0;
  background: #1e1e2e;
}

.detail-code-block {
  background: transparent;
  color: #cdd6f4;
  padding: 16px 20px;
  margin: 0;
  border: none;
  font-family: 'SF Mono', Consolas, Monaco, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.doc-style {
  line-height: 1.8;
  color: #334155;
}

/* AI 评估报告 */
.detail-ai-review {
  color: #334155;
  line-height: 1.9;
  font-size: 14px;
}
.detail-ai-review :deep(p) { margin: 0 0 12px; }
.detail-ai-review :deep(h1),
.detail-ai-review :deep(h2),
.detail-ai-review :deep(h3),
.detail-ai-review :deep(h4) {
  margin: 16px 0 10px;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
  padding-left: 10px;
  border-left: 4px solid #8b5cf6;
}
.detail-ai-review :deep(h1) { font-size: 16px; }
.detail-ai-review :deep(h2) { font-size: 15px; }
.detail-ai-review :deep(h3) { font-size: 14px; }
.detail-ai-review :deep(h4) { font-size: 13px; }
.detail-ai-review :deep(strong) {
  color: #1e293b;
  font-weight: 700;
  background: #f3e8ff;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #e9d5ff;
}
.detail-ai-review :deep(ul),
.detail-ai-review :deep(ol) {
  margin: 0 0 12px;
  padding-left: 0;
  list-style: none;
}
.detail-ai-review :deep(ul > li),
.detail-ai-review :deep(ol > li) {
  position: relative;
  margin-bottom: 8px;
  padding: 10px 12px 10px 36px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}
.detail-ai-review :deep(ol) { counter-reset: report-step; }
.detail-ai-review :deep(ol > li::before) {
  counter-increment: report-step;
  content: counter(report-step);
  position: absolute;
  left: 10px;
  top: 10px;
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #f3e8ff;
  color: #7c3aed;
  font-size: 11px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-ai-review :deep(ul > li::before) {
  content: '';
  position: absolute;
  left: 14px;
  top: 16px;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #8b5cf6;
}
.detail-ai-review :deep(pre) {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px 14px;
  overflow-x: auto;
  font-family: 'SF Mono', Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  margin: 10px 0;
  color: #334155;
}
.detail-ai-review :deep(code) {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  color: #c41d7f;
}
.detail-ai-review :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: 13px;
}
.detail-ai-review :deep(blockquote) {
  margin: 12px 0;
  padding: 10px 14px;
  background: #fffbeb;
  border-left: 4px solid #f59e0b;
  border-radius: 0 6px 6px 0;
  color: #92400e;
  font-size: 13px;
}

/* ===== 编辑弹窗左右分栏 ===== */
.problem-modal-body {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 0;
  height: 680px;
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}
.problem-form-col {
  border-right: 1px solid #e2e8f0;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
}
.problem-extra-col {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}
.problem-extra-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  flex-shrink: 0;
  gap: 12px;
}
.problem-extra-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}
.problem-extra-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}
.extra-section {
  margin-bottom: 16px;
}
.extra-section-title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.compact-form-item {
  margin-bottom: 10px;
}
.compact-form-item :deep(.ant-form-item-label) {
  padding-bottom: 4px;
}
.compact-form-item :deep(label) {
  font-size: 12px;
}

.mono-text :deep(textarea) {
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
}

/* ===== 完成情况弹窗 ===== */
.completion-summary {
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

/* ===== 试卷预览 ===== */
.quiz-preview-wrapper {
  height: 680px;
  max-height: 680px;
  overflow-y: auto;
  padding-right: 16px;
}
.quiz-preview-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}
.quiz-preview-count {
  font-weight: 600;
  color: #475569;
  font-size: 14px;
}
.quiz-preview-time {
  color: #94a3b8;
  font-size: 13px;
  margin-left: auto;
}
.quiz-preview-content {
  color: #334155;
  line-height: 1.8;
  font-size: 14px;
}

/* ===== 批阅工作台 ===== */
:global(.grading-modal .ant-modal-body) {
  padding: 0;
}
.grading-workbench {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 0;
  height: 680px;
  overflow: hidden;
}
.grading-paper-col {
  border-right: 1px solid #e2e8f0;
  overflow-y: auto;
  padding: 20px;
  background: #f8fafc;
}
.grading-paper-header {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e2e8f0;
}
.grading-paper-content { font-size: 13px; line-height: 1.8; }
.grading-paper-content :deep(h1) { font-size: 16px; font-weight: 700; color: #1f2937; }
.grading-paper-content :deep(h2) { font-size: 14px; font-weight: 700; color: #334155; background: #f1f5f9; padding: 6px 10px; border-left: 3px solid #8b5cf6; margin: 12px 0 8px; }
.grading-paper-content :deep(p) { margin: 0 0 8px; }
.grading-paper-content :deep(pre) { background: #f6f8fa; padding: 10px; border-radius: 5px; overflow-x: auto; font-size: 12px; }

.grading-grading-col {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px 20px;
  background: #fff;
}
.grading-col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}
.grading-student-info { display: flex; align-items: center; gap: 8px; }
.grading-student-name { font-size: 15px; font-weight: 700; color: #1e293b; }
.grading-scroll-area { flex: 1; overflow-y: auto; padding-right: 4px; min-height: 0; }
.grading-footer { flex-shrink: 0; margin-top: 12px; padding-top: 12px; border-top: 1px solid #e2e8f0; }

/* ===== 考试批阅样式 ===== */
.grade-section { margin-bottom: 18px; }
.grade-section-title { position: sticky; top: 0; z-index: 2; display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; margin-bottom: 10px; border-left: 3px solid #2563eb; border-radius: 6px; background: #eff6ff; color: #1e3a8a; font-size: 14px; font-weight: 700; }
.grade-item { background: #FFFFFF; border: 1px solid #E7ECF3; border-radius: 8px; padding: 14px 16px; margin-bottom: 12px; }
.grade-item-header { display: flex; align-items: center; gap: 8px; font-weight: 600; color: #1F2937; font-size: 14px; margin-bottom: 8px; }
.grade-stem { font-size: 13px; color: #475569; line-height: 1.7; margin-bottom: 8px; }
.grade-answer { font-size: 13px; margin-bottom: 8px; }
.grade-review-images { margin: 8px 0 10px; }
.grade-score-input { font-size: 13px; margin-bottom: 4px; }
.grade-label { color: #64748B; margin-right: 8px; }
.grade-value { color: #2563EB; font-weight: 500; word-break: break-all; }
.grade-remark-area { margin-top: 16px; }
.grade-score-max { color: #94a3b8; font-size: 13px; margin-left: 4px; }
.grade-ai-comment { font-size: 13px; margin-top: 6px; padding: 6px 10px; background: #F9FAFB; border-radius: 4px; }
.grade-actions { margin-top: 16px; text-align: right; }

.status-completed { color: #16a34a; font-weight: 600; }
.status-failed { color: #dc2626; font-weight: 600; }
.status-pending { color: #d97706; font-weight: 600; }
.status-review { color: #2563eb; font-weight: 700; }
.status-plain { color: #94a3b8; }

.review-box {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid #dbeafe;
  background: #f8fbff;
  border-radius: 8px;
}

.review-title {
  font-size: 14px;
  font-weight: 700;
  color: #1d4ed8;
  margin-bottom: 12px;
}

.review-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px 110px;
  align-items: start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #e5edf8;
}

.review-row:last-of-type {
  border-bottom: none;
}

.review-question {
  font-size: 13px;
  color: #1f2937;
  font-weight: 600;
}

.review-question-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.review-stem,
.review-answer {
  font-size: 12px;
  color: #475569;
  line-height: 1.6;
  margin-top: 4px;
  word-break: break-word;
}

.review-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  margin: 8px 0;
}

.review-option {
  display: flex;
  gap: 6px;
  padding: 6px 8px;
  border: 1px solid #e5edf8;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  color: #334155;
}

.review-option-label {
  flex: 0 0 auto;
  font-weight: 700;
  color: #2563eb;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.review-images :deep(.ant-image) {
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  overflow: hidden;
  background: #eff6ff;
}

.review-images :deep(.ant-image-img) {
  object-fit: cover;
}

.review-suggest {
  font-size: 12px;
  color: #64748b;
  line-height: 24px;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.submissions-header {
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.md-body { line-height: 1.8; }
.md-body :deep(pre) {
  background: #f6f8fa;
  padding: 10px;
  border-radius: 5px;
  overflow-x: auto;
}

/* ===== 滚动条 ===== */
.coding-tab-content::-webkit-scrollbar {
  width: 5px;
}
.coding-tab-content::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 10px;
}

/* ===== 响应式 ===== */
@media (max-width: 1280px) {
  .stats-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
  .modern-page { padding: 14px 16px; }
  .page-header { flex-direction: column; align-items: stretch; }
  .header-left { flex-direction: column; gap: 10px; }
  .stats-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .filter-dashboard { flex-direction: column; gap: 12px; align-items: stretch; }
}
</style>
