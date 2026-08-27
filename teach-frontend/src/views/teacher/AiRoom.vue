<template>
  <div class="ai-console-wrapper">
    <aside class="engine-sidebar">
      <div class="sidebar-header">
        <div class="pulse-ring"></div>
        <h3>AI Co-Pilots</h3>
      </div>
      <p class="sub-title">选择您的专属助教引擎</p>

      <div class="agent-list">
        <div
          v-for="(config, key) in agentConfig"
          :key="key"
          class="agent-item"
          :class="{ active: currentType === key }"
          @click="switchAgent(key)"
        >
          <div class="agent-icon">
            <component :is="iconMap[config.icon]" />
          </div>
          <div class="agent-info">
            <div class="name">{{ config.name }}</div>
            <div class="desc">{{ config.desc }}</div>
          </div>
        </div>
      </div>
    </aside>

    <main class="chat-workspace">
      <header class="workspace-header">
        <div class="current-engine">
          <span class="engine-icon">
            <component :is="iconMap[currentAgentIcon]" />
          </span>
          <span class="engine-name">{{ currentAgentName }}</span>
        </div>

        <button
          v-if="messages.length > 0 && isChatMode"
          class="ghost-btn"
          @click="clearHistory"
        >
          <delete-outlined />
          清除上下文
        </button>
      </header>


      <!-- PPT -->
      <div v-if="currentType === 'ppt'" class="ppt-iframe-container">
        <iframe
          src="https://pipipi-pikachu.github.io/PPTist/"
          frameborder="0"
          width="100%"
          height="100%"
          allowfullscreen="true"
        ></iframe>
      </div>

      <!-- 教案生成 -->
      <div v-else-if="currentType === 'plan'" class="tool-generator-container">
        <div class="tool-layout">
          <section class="tool-config-panel scroll-y">
            <div class="panel-title">
              <form-outlined style="color: #3b82f6; margin-right: 8px;" />
              教案生成参数配置
            </div>

            <a-form layout="vertical" :model="planForm" class="tool-form">

              <div class="config-group">
                <div class="group-title" style="font-weight: 600; color: #475569; margin-bottom: 16px;">基础信息</div>
                <div class="form-row-2">
                  <a-form-item label="所属学科" required>
                    <a-input v-model:value="planForm.subject" placeholder="例如：C语言" size="large" />
                  </a-form-item>
                  <a-form-item label="教学课题" required>
                    <a-input v-model:value="planForm.topic" placeholder="例如：链表" size="large" />
                  </a-form-item>
                </div>

                <div class="form-row-2">
                  <a-form-item label="学段/年级">
                    <a-select v-model:value="planForm.grade" size="large">
                      <a-select-option value="本科一年级">本科一年级</a-select-option>
                      <a-select-option value="本科二年级">本科二年级</a-select-option>
                      <a-select-option value="本科三年级">本科三年级</a-select-option>
                      <a-select-option value="本科四年级">本科四年级</a-select-option>
                    </a-select>
                  </a-form-item>
                  <a-form-item label="课型">
                    <a-select v-model:value="planForm.lessonType" size="large">
                      <a-select-option value="新授课">新授课</a-select-option>
                      <a-select-option value="复习课">复习课</a-select-option>
                      <a-select-option value="习题课">习题课</a-select-option>
                      <a-select-option value="实验课">实验课</a-select-option>
                    </a-select>
                  </a-form-item>
                </div>

                <div class="form-row-2">
                  <a-form-item label="课时数量">
                    <a-input-number v-model:value="planForm.lessonCount" :min="1" :max="20" size="large" style="width: 100%" />
                  </a-form-item>
                  <a-form-item label="单课时长（分钟）">
                    <a-input-number v-model:value="planForm.duration" :min="10" :max="180" size="large" style="width: 100%" />
                  </a-form-item>
                </div>
              </div>

              <a-divider dashed style="margin: 8px 0 24px 0; border-color: #e2e8f0;" />

              <div class="config-group">
                <div class="group-title" style="font-weight: 600; color: #475569; margin-bottom: 16px;">学情与基础</div>
                <div class="form-row-2">
                  <a-form-item label="学生基础">
                    <a-select v-model:value="planForm.studentLevel" size="large">
                      <a-select-option value="较弱">较弱</a-select-option>
                      <a-select-option value="一般">一般</a-select-option>
                      <a-select-option value="较好">较好</a-select-option>
                    </a-select>
                  </a-form-item>
                  <a-form-item label="先修知识">
                    <a-input v-model:value="planForm.prereqKnowledge" placeholder="例如：掌握函数基础" size="large" />
                  </a-form-item>
                </div>

                <a-form-item label="常见学习难点">
                  <a-checkbox-group v-model:value="planForm.difficulties" class="custom-checkbox-group">
                    <a-checkbox value="概念抽象">概念抽象</a-checkbox>
                    <a-checkbox value="理解困难">理解困难</a-checkbox>
                    <a-checkbox value="迁移应用弱">迁移应用弱</a-checkbox>
                    <a-checkbox value="计算易错">计算易错</a-checkbox>
                  </a-checkbox-group>
                </a-form-item>
              </div>

              <a-divider dashed style="margin: 8px 0 24px 0; border-color: #e2e8f0;" />

              <div class="config-group">
                <div class="group-title" style="font-weight: 600; color: #475569; margin-bottom: 16px;">教学策略</div>
                <a-form-item label="教学方法">
                  <a-checkbox-group v-model:value="planForm.methods" class="custom-checkbox-group">
                    <a-tooltip
                      v-for="(desc, method) in teachingMethodTips"
                      :key="method"
                      :title="desc"
                      placement="top"
                      :mouseEnterDelay="0.4"
                    >
                      <a-checkbox :value="method">{{ method }}</a-checkbox>
                    </a-tooltip>
                  </a-checkbox-group>
                </a-form-item>

                <a-form-item label="课堂附加设计">
                  <a-checkbox-group v-model:value="planForm.activities" class="custom-checkbox-group">
                    <a-checkbox value="课堂提问设计">课堂提问设计</a-checkbox>
                    <a-checkbox value="板书设计">板书设计</a-checkbox>
                    <a-checkbox value="随堂练习">随堂练习</a-checkbox>
                    <a-checkbox value="分层任务">分层任务</a-checkbox>
                  </a-checkbox-group>
                </a-form-item>
              </div>

              <a-divider dashed style="margin: 8px 0 24px 0; border-color: #e2e8f0;" />

              <div class="config-group">
                <div class="group-title" style="font-weight: 600; color: #475569; margin-bottom: 16px;">补充要求与参考</div>
                <a-form-item label="其他要求">
                  <a-textarea v-model:value="planForm.extraRequirements" :rows="3" placeholder="例如：加入课程思政元素、设计 Packet Tracer 演示" />
                </a-form-item>

                <a-form-item label="参考案例">
                  <div class="platform-case-recommend">
                    <div class="recommend-head">
                      <span>{{ recommendedCases.length ? '系统已从案例库自动匹配相关案例' : '填写学科和课题后自动推荐相关案例' }}</span>
                      <div class="recommend-actions">
                        <a-button size="small" type="link" :loading="caseRecommendLoading" @click="loadRecommendedCases">
                          <sync-outlined />
                          重新匹配
                        </a-button>
                        <a-button size="small" @click="openCasePicker">
                          <folder-open-outlined />
                          从案例库选择
                        </a-button>
                      </div>
                    </div>
                    <div v-if="selectedCaseItems.length" class="selected-case-list">
                      <div v-for="item in selectedCaseItems" :key="item.id" class="selected-case-chip">
                        <file-text-outlined />
                        <span>{{ item.title }}</span>
                        <a-button size="small" type="link" @click="previewCaseById(item.id)">预览</a-button>
                        <a-button size="small" type="link" danger @click="removeSelectedCase(item.id)">移除</a-button>
                      </div>
                    </div>
                    <div v-if="recommendedCases.length" class="recommend-grid">
                      <button
                        v-for="item in recommendedCases"
                        :key="item.id"
                        type="button"
                        class="recommend-card"
                        :class="{ selected: selectedPlatformCaseIds.includes(item.id) }"
                        @click="toggleRecommendedCase(item)"
                      >
                        <div class="recommend-card-top">
                          <strong>{{ item.title }}</strong>
                          <a-tag :color="selectedPlatformCaseIds.includes(item.id) ? 'blue' : recommendMatchColor(item.matchLevel)">
                            {{ selectedPlatformCaseIds.includes(item.id) ? '已选' : recommendMatchLabel(item.matchLevel) }}
                          </a-tag>
                        </div>
                        <p>{{ item.summary || item.matchReason || '案例库中的相关教学案例' }}</p>
                        <div v-if="item.evidenceSnippet" class="recommend-evidence">
                          <span>{{ item.evidenceTitle || '匹配证据' }}</span>
                          <em>{{ item.evidenceSnippet }}</em>
                        </div>
                        <div v-else-if="item.matchReason" class="recommend-evidence weak">
                          <span>{{ item.matchReason }}</span>
                        </div>
                        <div class="recommend-meta">
                          <span>{{ item.courseName || '高校计算机课程' }}</span>
                          <span>{{ item.sourceName || '公开来源' }}</span>
                          <span>素材链接 {{ item.materialCount || 0 }}</span>
                          <a @click.stop="previewCaseById(item.id)">预览</a>
                        </div>
                      </button>
                    </div>
                    <a-empty v-else-if="caseRecommendationTouched && !caseRecommendLoading" description="暂无匹配案例，可从案例库选择或直接生成" />
                    <div class="case-hint">系统会默认带入最相关的案例。可按需选择多份，AI 将融合案例结构并补充推荐素材来源。</div>
                  </div>
                </a-form-item>

                <a-form-item v-if="false" label="参考案例（选填）">
                  <div v-if="selectedTeachingCase" class="selected-case-panel">
                    <div class="selected-case-main">
                      <div class="selected-case-title">
                        <file-text-outlined />
                        <span>{{ selectedTeachingCase?.title }}</span>
                      </div>
                      <div class="selected-case-meta">
                        <span>{{ selectedTeachingCase?.courseName || '未指定适用课程' }}</span>
                        <a-tag color="blue">{{ caseCategoryLabel(selectedTeachingCase?.category || '') }}</a-tag>
                        <a-tag :color="caseDifficultyColor(selectedTeachingCase?.difficulty || '')">
                          {{ caseDifficultyLabel(selectedTeachingCase?.difficulty || '') }}
                        </a-tag>
                      </div>
                    </div>
                    <div class="selected-case-actions">
                      <a-button size="small" @click="openCasePicker">
                        <folder-open-outlined />
                        更换
                      </a-button>
                      <a-button size="small" @click="previewSelectedCase">
                        <eye-outlined />
                        预览
                      </a-button>
                      <a-button size="small" danger @click="clearSelectedCase">
                        <close-circle-outlined />
                        清除
                      </a-button>
                    </div>
                  </div>
                  <a-button v-else size="large" class="case-picker-trigger" @click="openCasePicker">
                    <folder-open-outlined />
                    选择参考案例
                  </a-button>
                  <div class="case-hint">
                    选中案例后，AI 将在生成教案时融入该案例的情境与内容
                  </div>
                </a-form-item>
              </div>

              <div class="tool-submit-bar">
                <a-button type="primary" size="large" class="generate-btn plan-btn" :loading="isPlanGenerating" @click="generatePlan">
                  生成教案
                </a-button>
                <a-button size="large" @click="resetPlanForm">重置参数</a-button>

              </div>
            </a-form>
          </section>

          <section class="tool-result-panel scroll-y">
            <div v-if="!planResult && !isPlanGenerating" class="empty-result">
              <file-text-outlined class="large-empty-icon" />
              <p>填写参数后，AI 将为您生成一份可继续编辑的结构化教案</p>
            </div>

            <div v-else class="result-content">
              <div class="result-toolbar">
          <span class="status-tag">
  <sync-outlined v-if="isPlanGenerating" spin style="color: #3b82f6;" />
  <check-circle-filled v-else style="color: #10b981;" />
  {{
              isPlanGenerating
                ? '教案生成中...'
                : isPlanEditing
                  ? '教案编辑中'
                  : '教案生成完毕'
            }}
</span>

                <div v-if="!isPlanGenerating && planResult" class="toolbar-actions">
                  <a-button
                    v-if="!isPlanEditing"
                    type="link"
                    class="tool-btn"
                    @click="startEditPlan"
                  >
                    <form-outlined />
                    编辑
                  </a-button>

                  <a-button
                    v-if="isPlanEditing"
                    type="link"
                    class="tool-btn"
                    @click="applyEditedPlan"
                  >
                    <check-circle-outlined />
                    应用修改
                  </a-button>

                  <a-button
                    v-if="isPlanEditing"
                    type="link"
                    class="tool-btn"
                    @click="cancelEditPlan"
                  >
                    <delete-outlined />
                    取消编辑
                  </a-button>

                  <a-button type="link" class="tool-btn" @click="savePlanToCloud">
                    <save-outlined />
                    {{ currentPlanId ? '保存修改' : '保存至云端' }}
                  </a-button>

                  <a-button type="link" class="tool-btn" :loading="isPlanExporting" @click="downloadPlan('docx')">
                    <download-outlined />
                    导出
                  </a-button>

                  <a-button
                    type="link"
                    class="tool-btn"
                    @click="copyText(getCurrentPlanMarkdown())"
                  >
                    <copy-outlined />
                    复制
                  </a-button>
                </div>
              </div>

              <div v-if="showPlanAgentMeta" class="plan-meta-shell">
                <button
                  type="button"
                  class="plan-meta-toggle"
                  :aria-expanded="isPlanMetaExpanded"
                  @click="isPlanMetaExpanded = !isPlanMetaExpanded"
                >
                  <span>
                    <eye-outlined />
                    {{ isPlanMetaExpanded ? '收起检索信息' : '查看检索信息' }}
                  </span>
                  <em v-if="planMetaSummary">{{ planMetaSummary }}</em>
                </button>

                <div v-if="isPlanMetaExpanded" class="agent-meta-panel">
                  <div v-if="agentCitations.length" class="agent-citation-list">
                    <div class="citation-head">
                      <span>本次参考资源</span>
                      <button v-if="excludedCitationKeys.length" type="button" @click="clearCitationExclusions">恢复全部</button>
                    </div>
                    <div
                      v-for="item in agentCitations"
                      :key="citationKey(item)"
                      class="citation-item"
                      :class="{ excluded: isCitationExcluded(item) }"
                    >
                      <div class="citation-main">
                        <span v-if="item.evidenceId" class="citation-evidence">{{ item.evidenceId }}</span>
                        <span class="citation-type">{{ citationTypeLabel(item) }}</span>
                        <span v-if="citationReasonLabel(item.reason)" class="citation-reason">{{ citationReasonLabel(item.reason) }}</span>
                        <strong>{{ item.title || item.sourceId || '未命名资源' }}</strong>
                        <span v-if="citationScoreText(item.score)" class="citation-score">{{ citationScoreText(item.score) }}</span>
                      </div>
                      <p v-if="item.snippet">{{ item.snippet }}</p>
                      <button type="button" @click="toggleCitationExcluded(item)">
                        {{ isCitationExcluded(item) ? '恢复' : '排除' }}
                      </button>
                    </div>
                  </div>
                  <div v-else-if="agentRetrievalDone" class="agent-meta-small">
                    未检索到强相关资源
                  </div>
                  <div v-if="agentCaseAnalysis" class="agent-meta-small">
                    教学案例：{{ agentCaseAnalysis.title || '已选择案例' }}
                    <span v-if="agentCaseAnalysis.pdfParseOk === false">（PDF 解析失败，仅参考元信息）</span>
                  </div>
                  <div v-if="agentQualityReport" class="agent-meta-small">
                    Quality: {{ agentQualityReport.score ?? '-' }}
                  </div>
                </div>
              </div>

              <div
                v-if="!isPlanEditing"
                class="markdown-render doc-style"
                v-html="renderMd(planResult)"
              ></div>

              <div v-else class="editor-panel">
                <RichTextEditor
                  v-model="editablePlanHtml"
                  class="plan-editor"
                  height="720px"
                  placeholder="你可以在这里继续修改教案内容"
                />
              </div>

              <div v-if="isPlanGenerating" class="skeleton-loader">
                <div class="sk-line title"></div>
                <div class="sk-line"></div>
                <div class="sk-line"></div>
                <div class="sk-line short"></div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- 智能出题 -->
      <div v-else-if="currentType === 'quiz'" class="tool-generator-container">
        <div class="tool-layout">
          <section class="tool-config-panel scroll-y">
            <div class="panel-title">
              <form-outlined style="color: #3b82f6; margin-right: 8px;" />
              智能出题参数配置
            </div>

            <a-form layout="vertical" :model="quizForm" class="tool-form">

              <div class="config-group">
                <div class="group-title" style="font-weight: 600; color: #475569; margin-bottom: 16px;">基础设定</div>

                <div class="form-row-2">
                  <a-form-item label="所属学科" required>
                    <a-input v-model:value="quizForm.subject" placeholder="例如：C语言" size="large" />
                  </a-form-item>

                  <a-form-item label="考核知识点" required>
                    <a-input v-model:value="quizForm.knowledgePoints" placeholder="例如：基础语法" size="large" />
                  </a-form-item>
                </div>
              </div>

              <a-divider dashed style="margin: 8px 0 24px 0; border-color: #e2e8f0;" />

              <div class="config-group">
                <div class="group-title" style="font-weight: 600; color: #475569; margin-bottom: 16px;">题型与结构</div>
                <div class="form-row-2">
                  <a-form-item label="使用场景">
                    <a-select v-model:value="quizForm.scenario" size="large">
                      <a-select-option value="课堂检测">课堂检测</a-select-option>
                      <a-select-option value="课后作业">课后作业</a-select-option>
                      <a-select-option value="考试试卷">考试试卷</a-select-option>
                    </a-select>
                  </a-form-item>
                </div>

                <a-form-item label="题型与数量（按题型分别配置，0 表示不出该题型）" required>
                  <div class="quiz-type-count-grid">
                    <div
                      v-for="t in QUIZ_TYPE_OPTIONS"
                      :key="t"
                      class="quiz-type-count-item"
                      :class="{ 'is-active': (quizForm.typeCounts[t] || 0) > 0 }"
                    >
                      <span class="type-label">{{ t }}</span>
                      <div class="count-wrap">
                        <a-input-number
                          :value="quizForm.typeCounts[t]"
                          :min="0"
                          :max="20"
                          @update:value="(v: number | null) => updateQuizTypeCount(t, v)"
                        />
                        <span class="unit">道</span>
                      </div>
                    </div>
                  </div>
                  <div class="quiz-total-summary">
                    合计：<strong>{{ quizTotalCount }}</strong> 道题
                    <span v-if="quizTotalCount === 0" class="warn">（请至少为一种题型配置数量）</span>
                    <span v-else-if="quizTotalCount > 50" class="warn">（总数已超过 50 道，建议拆成多份）</span>
                  </div>
                </a-form-item>
              </div>

              <a-divider dashed style="margin: 8px 0 24px 0; border-color: #e2e8f0;" />

              <div class="config-group">
                <div class="group-title" style="font-weight: 600; color: #475569; margin-bottom: 16px;">难度与偏好</div>
                <div class="form-row-2">
                  <a-form-item label="总体难度">
                    <a-select v-model:value="quizForm.difficulty" size="large">
                      <a-select-option value="基础">基础</a-select-option>
                      <a-select-option value="中等">中等</a-select-option>
                      <a-select-option value="中等偏上">中等偏上</a-select-option>
                      <a-select-option value="较难">较难</a-select-option>
                      <a-select-option value="综合提升">综合提升</a-select-option>
                    </a-select>
                  </a-form-item>
                  <a-form-item label="题目风格">
                    <a-select v-model:value="quizForm.style" size="large">
                      <a-select-option value="标准规范型">标准规范型</a-select-option>
                      <a-select-option value="课堂检测型">课堂检测型</a-select-option>
                      <a-select-option value="启发探究型">启发探究型</a-select-option>
                      <a-select-option value="应用导向型">应用导向型</a-select-option>
                      <a-select-option value="综合拔高型">综合拔高型</a-select-option>
                    </a-select>
                  </a-form-item>
                </div>
                <div class="form-row-2">
                  <a-form-item label="是否偏基础题">
                    <a-select v-model:value="quizForm.preferFoundation" size="large">
                      <a-select-option value="是">是</a-select-option>
                      <a-select-option value="否">否</a-select-option>
                    </a-select>
                  </a-form-item>
                  <a-form-item label="是否偏应用题">
                    <a-select v-model:value="quizForm.preferApplication" size="large">
                      <a-select-option value="是">是</a-select-option>
                      <a-select-option value="否">否</a-select-option>
                    </a-select>
                  </a-form-item>
                </div>
              </div>

              <div class="tool-submit-bar">

                <a-button
                  type="primary"
                  size="large"
                  class="generate-btn plan-btn"
                  :loading="isQuizGenerating || isQuizOptimizing"
                  @click="generateQuiz"
                >

                  {{
                    isQuizGenerating
                      ? '生成习题中...'
                      : isQuizOptimizing
                        ? '优化习题中...'
                        : '生成习题'
                  }}
                </a-button>

                <a-button
                  size="large"
                  :disabled="isQuizGenerating || isQuizOptimizing"
                  @click="resetQuizForm"
                >
                  重置参数
                </a-button>


              </div>
            </a-form>
          </section>

          <section class="tool-result-panel scroll-y">
            <div v-if="!quizResult && !isQuizGenerating && !isQuizOptimizing" class="empty-result">
              <form-outlined class="large-empty-icon" />
              <p>填写左侧配置后，AI 将为您生成一份可继续编辑、优化、保存的习题</p>
            </div>

            <div v-else class="result-content">
              <div class="result-toolbar">
          <span class="status-tag">
  <sync-outlined v-if="isQuizGenerating" spin style="color: #3b82f6;" />
            <check-circle-filled v-else style="color: #3b82f6;" />
  {{
              isQuizGenerating
                ? '习题生成中...'
                : isQuizEditing
                  ? '习题编辑中'
                  : isQuizOptimizing
                    ? '习题优化中'
                    : '习题已生成'
            }}
</span>

                <div v-if="(quizResult || editableQuizContent) && !isQuizGenerating" class="toolbar-actions">

                  <a-button
                    v-if="!isQuizEditing"
                    type="link"
                    class="tool-btn"
                    :disabled="isQuizOptimizing"
                    @click="startEditQuiz"
                  >
                    <form-outlined />
                    编辑
                  </a-button>

                  <a-button
                    v-if="isQuizEditing"
                    type="link"
                    class="tool-btn"
                    :disabled="isQuizOptimizing"
                    @click="applyEditedQuiz"
                  >
                    <check-circle-outlined />
                    应用修改
                  </a-button>

                  <a-button
                    v-if="isQuizEditing"
                    type="link"
                    class="tool-btn"
                    :disabled="isQuizOptimizing"
                    @click="cancelEditQuiz"
                  >
                    <delete-outlined />
                    取消编辑
                  </a-button>

                  <a-button
                    type="link"
                    class="tool-btn"
                    :disabled="isQuizOptimizing"
                    @click="saveQuizToCloud"
                  >
                    <save-outlined />
                    {{ currentQuizId ? '保存修改' : '保存至云端' }}
                  </a-button>

                  <a-button
                    type="link"
                    class="tool-btn"
                    :disabled="isQuizOptimizing"
                    @click="downloadQuiz"
                  >
                    <download-outlined />
                    导出
                  </a-button>

                  <a-button
                    type="link"
                    class="tool-btn"
                    :disabled="isQuizOptimizing"
                    @click="copyText(isQuizEditing ? editableQuizContent : quizResult)"
                  >
                    <copy-outlined />
                    复制
                  </a-button>
                </div>
              </div>


              <div v-if="showAgentMeta && (agentStages.length || agentCitations.length || agentQualityReport || agentCaseAnalysis)" class="agent-meta-panel">
                <div v-if="agentStages.length" class="agent-meta-row">
                  <span
                    v-for="stage in agentStages"
                    :key="stage.name"
                    class="agent-stage-chip"
                    :class="stage.status"
                  >
                    {{ stage.name }}
                  </span>
                </div>
                <div v-if="agentCitations.length" class="agent-citation-list">
                  <div class="citation-head">
                    <span>本次参考资源</span>
                    <button v-if="excludedCitationKeys.length" type="button" @click="clearCitationExclusions">恢复全部</button>
                  </div>
                  <div
                    v-for="item in agentCitations"
                    :key="citationKey(item)"
                    class="citation-item"
                    :class="{ excluded: isCitationExcluded(item) }"
                  >
                    <div class="citation-main">
                      <span v-if="item.evidenceId" class="citation-evidence">{{ item.evidenceId }}</span>
                      <span class="citation-type">{{ citationTypeLabel(item) }}</span>
                      <span v-if="citationReasonLabel(item.reason)" class="citation-reason">{{ citationReasonLabel(item.reason) }}</span>
                      <strong>{{ item.title || item.sourceId || '未命名资源' }}</strong>
                      <span v-if="citationScoreText(item.score)" class="citation-score">{{ citationScoreText(item.score) }}</span>
                    </div>
                    <p v-if="item.snippet">{{ item.snippet }}</p>
                    <button type="button" @click="toggleCitationExcluded(item)">
                      {{ isCitationExcluded(item) ? '恢复' : '排除' }}
                    </button>
                  </div>
                </div>
                <div v-else-if="agentRetrievalDone" class="agent-meta-small">
                  未检索到强相关资源
                </div>
                <div v-if="agentCaseAnalysis" class="agent-meta-small">
                  教学案例：{{ agentCaseAnalysis.title || '已选择案例' }}
                  <span v-if="agentCaseAnalysis.pdfParseOk === false">（PDF 解析失败，仅参考元信息）</span>
                </div>
                <div v-if="agentQualityReport" class="agent-meta-small">
                  Quality: {{ agentQualityReport.score ?? '-' }}
                </div>
              </div>

              <div
                v-if="!isQuizEditing"
                class="markdown-render doc-style"
                v-html="renderMd(quizResult)"
              ></div>

              <div v-else class="editor-panel">
                <a-textarea
                  v-model:value="editableQuizContent"
                  :rows="28"
                  class="plan-editor quiz-editor"
                  placeholder="你可以继续修改习题内容。请保持题目区与答案解析区分离。"
                />
              </div>

              <div v-if="isQuizGenerating || isQuizOptimizing" class="skeleton-loader">
                <div class="sk-line title"></div>
                <div class="sk-line"></div>
                <div class="sk-line"></div>
                <div class="sk-line short"></div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- 交互课件 -->
      <div v-else-if="currentType === 'anim'" class="tool-generator-container">
        <div class="tool-layout">
          <AnimationConfigPanel
            :form="animForm"
            :is-generating="isAnimGenerating"
            :is-optimizing="isAnimOptimizing"
            @generate="generateAnimation"
            @reset="resetAnimForm"
            @apply-example="applyAnimExample"
          />

          <section class="tool-result-panel anim-result-panel">
            <div v-if="showAgentMeta && (agentStages.length || agentCitations.length || agentQualityReport)" class="agent-meta-panel anim-agent-meta">
              <div v-if="agentStages.length" class="agent-meta-row">
                <span
                  v-for="stage in agentStages"
                  :key="stage.name"
                  class="agent-stage-chip"
                  :class="stage.status"
                >
                  {{ stage.name }}
                </span>
              </div>
              <div v-if="agentCitations.length" class="agent-citation-list">
                <div class="citation-head">
                  <span>本次参考资源</span>
                  <button v-if="excludedCitationKeys.length" type="button" @click="clearCitationExclusions">恢复全部</button>
                </div>
                <div
                  v-for="item in agentCitations"
                  :key="citationKey(item)"
                  class="citation-item"
                  :class="{ excluded: isCitationExcluded(item) }"
                >
                  <div class="citation-main">
                    <span v-if="item.evidenceId" class="citation-evidence">{{ item.evidenceId }}</span>
                    <span class="citation-type">{{ citationTypeLabel(item) }}</span>
                    <span v-if="citationReasonLabel(item.reason)" class="citation-reason">{{ citationReasonLabel(item.reason) }}</span>
                    <strong>{{ item.title || item.sourceId || '未命名资源' }}</strong>
                    <span v-if="citationScoreText(item.score)" class="citation-score">{{ citationScoreText(item.score) }}</span>
                  </div>
                  <p v-if="item.snippet">{{ item.snippet }}</p>
                  <button type="button" @click="toggleCitationExcluded(item)">
                    {{ isCitationExcluded(item) ? '恢复' : '排除' }}
                  </button>
                </div>
              </div>
              <div v-else-if="agentRetrievalDone" class="agent-meta-small">
                未检索到强相关资源
              </div>
              <div v-if="agentQualityReport" class="agent-meta-small">
                Quality: {{ agentQualityReport.score ?? '-' }}
              </div>
            </div>
            <AnimationWorkbench
              :payload="animJsonResult"
              :render-status="animRenderStatus"
              :validation-errors="animValidationErrors"
              :is-generating="isAnimGenerating"
              :is-optimizing="isAnimOptimizing"
              :autoplay-delay="animAutoPlayInterval"
              :current-resource-id="currentAnimId"
              display-mode="embedded"
              @optimize="handleQuickOptimize"
              @copy-json="copyAnimJson"
              @save-json="saveAnimComponent"
            />
          </section>
        </div>
      </div>

      <!-- 编程题生成 -->
      <div v-else-if="currentType === 'micro_video'" class="tool-generator-container micro-video-container">
        <MicroVideoGenerator />
      </div>

      <div v-else-if="currentType === 'coding'" class="tool-generator-container">
        <div class="tool-layout">
          <section class="tool-config-panel scroll-y">
            <div class="panel-title">
              <form-outlined style="color: #3b82f6; margin-right: 8px;" />
              编程题生成参数配置
            </div>

            <a-form layout="vertical" :model="codingForm" class="tool-form">
              <div class="config-group">
                <div class="group-title" style="font-weight: 600; color: #475569; margin-bottom: 16px;">需求描述</div>
                <a-form-item required>
                  <a-textarea v-model:value="codingForm.description" :rows="4" placeholder="例如：我想要出一个求两数之和的 Java 练习题，要求使用数组存储输入数据..." />
                </a-form-item>
              </div>

              <a-divider dashed style="margin: 8px 0 24px 0; border-color: #e2e8f0;" />

              <div class="config-group">
                <div class="group-title" style="font-weight: 600; color: #475569; margin-bottom: 16px;">语言与难度</div>
                <a-form-item label="支持语言" required>
                  <a-checkbox-group v-model:value="codingForm.languages" class="custom-checkbox-group">
                    <a-checkbox value="java">Java</a-checkbox>
                    <a-checkbox value="python">Python</a-checkbox>
                    <a-checkbox value="cpp">C++</a-checkbox>
                    <a-checkbox value="javascript">JavaScript</a-checkbox>
                  </a-checkbox-group>
                </a-form-item>

                <a-form-item label="难度">
                  <a-select v-model:value="codingForm.difficulty" size="large">
                    <a-select-option value="easy">简单</a-select-option>
                    <a-select-option value="medium">中等</a-select-option>
                    <a-select-option value="hard">困难</a-select-option>
                  </a-select>
                </a-form-item>
              </div>

              <div class="tool-submit-bar">
                <a-button type="primary" size="large" class="generate-btn plan-btn" :loading="isCodingGenerating" @click="generateCodingProblem">
                  生成编程题
                </a-button>
                <a-button size="large" @click="resetCodingForm">重置参数</a-button>
              </div>
            </a-form>
          </section>

          <section class="tool-result-panel scroll-y">
            <div v-if="!codingResult && !isCodingGenerating" class="empty-result">
              <code-outlined class="large-empty-icon" />
              <p>填写左侧需求后，AI 将为您生成完整的编程题（含测试用例与参考解）</p>
            </div>

            <div v-else class="result-content">
              <div class="result-toolbar">
                <span class="status-tag">
                  <sync-outlined v-if="isCodingGenerating" spin style="color: #3b82f6;" />
                  <check-circle-filled v-else style="color: #10b981;" />
                  {{ isCodingGenerating ? '编程题生成中...' : '编程题生成完毕' }}
                </span>

                <div v-if="!isCodingGenerating && codingResult" class="toolbar-actions">
                  <a-button type="link" class="tool-btn" @click="saveCodingToBank">
                    <save-outlined />
                    保存到编程题库
                  </a-button>
                  <a-button type="link" class="tool-btn" @click="saveCodingToCloud">
                    <save-outlined />
                    {{ currentCodingId ? '保存修改' : '保存至云端' }}
                  </a-button>
                </div>
              </div>

              <div v-if="showAgentMeta && (agentStages.length || agentCitations.length || agentQualityReport)" class="agent-meta-panel">
                <div v-if="agentStages.length" class="agent-meta-row">
                  <span
                    v-for="stage in agentStages"
                    :key="stage.name"
                    class="agent-stage-chip"
                    :class="stage.status"
                  >
                    {{ stage.name }}
                  </span>
                </div>
                <div v-if="agentCitations.length" class="agent-citation-list">
                  <div class="citation-head">
                    <span>本次参考资源</span>
                    <button v-if="excludedCitationKeys.length" type="button" @click="clearCitationExclusions">恢复全部</button>
                  </div>
                  <div
                    v-for="item in agentCitations"
                    :key="citationKey(item)"
                    class="citation-item"
                    :class="{ excluded: isCitationExcluded(item) }"
                  >
                    <div class="citation-main">
                      <span v-if="item.evidenceId" class="citation-evidence">{{ item.evidenceId }}</span>
                      <span class="citation-type">{{ citationTypeLabel(item) }}</span>
                      <span v-if="citationReasonLabel(item.reason)" class="citation-reason">{{ citationReasonLabel(item.reason) }}</span>
                      <strong>{{ item.title || item.sourceId || '未命名资源' }}</strong>
                      <span v-if="citationScoreText(item.score)" class="citation-score">{{ citationScoreText(item.score) }}</span>
                    </div>
                    <p v-if="item.snippet">{{ item.snippet }}</p>
                    <button type="button" @click="toggleCitationExcluded(item)">
                      {{ isCitationExcluded(item) ? '恢复' : '排除' }}
                    </button>
                  </div>
                </div>
                <div v-else-if="agentRetrievalDone" class="agent-meta-small">
                  未检索到强相关资源
                </div>
                <div v-if="agentQualityReport" class="agent-meta-small">
                  Quality: {{ agentQualityReport.score ?? '-' }}
                </div>
              </div>

              <div v-if="isCodingGenerating" class="skeleton-loader">
                <div class="sk-line title"></div>
                <div class="sk-line"></div>
                <div class="sk-line"></div>
                <div class="sk-line short"></div>
              </div>

              <div v-else-if="codingResult" class="coding-editor-panel">
                <div class="coding-section">
                  <div class="section-label">题目标题</div>
                  <a-input v-model:value="codingResult.title" placeholder="题目标题" size="large" />
                </div>

                <div class="coding-section">
                  <div class="section-label">难度</div>
                  <a-select v-model:value="codingResult.difficulty" size="large" style="width: 200px;">
                    <a-select-option value="easy">简单</a-select-option>
                    <a-select-option value="medium">中等</a-select-option>
                    <a-select-option value="hard">困难</a-select-option>
                  </a-select>
                </div>

                <div class="coding-section">
                  <div class="section-label">所属学期</div>
                  <a-select v-model:value="codingResult.semesterLabel" size="large" style="width: 240px;">
                    <a-select-option v-for="item in semesterOptions" :key="item.value" :value="item.value">
                      {{ item.label }}
                    </a-select-option>
                  </a-select>
                </div>

                <div class="coding-section">
                  <div class="section-label">题目描述（支持 Markdown）</div>
                  <a-textarea v-model:value="codingResult.description" :rows="6" placeholder="题目描述..." />
                </div>

                <div class="coding-section">
                  <div class="section-label">限制</div>
                  <div class="form-row-2">
                    <a-form-item label="时间限制 (ms)">
                      <a-input-number v-model:value="codingResult.timeLimitMs" :min="100" :max="60000" style="width: 100%" />
                    </a-form-item>
                    <a-form-item label="内存限制 (KB)">
                      <a-input-number v-model:value="codingResult.memoryLimitKb" :min="1024" :max="524288" style="width: 100%" />
                    </a-form-item>
                  </div>
                </div>

                <div class="coding-section">
                  <div class="section-label" style="display: flex; justify-content: space-between; align-items: center;">
                    <span>测试用例</span>
                    <a-button type="dashed" size="small" @click="addTestCase">
                      <plus-outlined /> 添加用例
                    </a-button>
                  </div>
                  <a-table
                    :dataSource="codingResult.testCases || []"
                    :columns="testCaseColumns"
                    size="small"
                    :pagination="false"
                    bordered
                  >
                    <template #bodyCell="{ column, record, index }">
                      <template v-if="column.dataIndex === 'input'">
                        <a-textarea v-model:value="record.input" :rows="2" placeholder="输入内容..." />
                      </template>
                      <template v-else-if="column.dataIndex === 'expectedOutput'">
                        <a-textarea v-model:value="record.expectedOutput" :rows="2" placeholder="期望输出..." />
                      </template>
                      <template v-else-if="column.dataIndex === 'isSample'">
                        <a-switch v-model:checked="record.isSample" :checkedValue="1" :unCheckedValue="0" />
                      </template>
                      <template v-else-if="column.dataIndex === 'score'">
                        <a-input-number v-model:value="record.score" :min="0" :max="100" style="width: 80px;" />
                      </template>
                      <template v-else-if="column.dataIndex === 'sortOrder'">
                        <a-input-number v-model:value="record.sortOrder" :min="0" style="width: 80px;" />
                      </template>
                      <template v-else-if="column.dataIndex === 'action'">
                        <a-button type="link" danger size="small" @click="removeTestCase(index)">
                          删除
                        </a-button>
                      </template>
                    </template>
                  </a-table>
                </div>

                <div class="coding-section">
                  <div class="section-label">代码模板</div>
                  <a-tabs type="card" size="small">
                    <a-tab-pane v-for="(tpl, idx) in codingResult.templates || []" :key="idx" :tab="tpl.language">
                      <div class="template-block">
                        <div class="tpl-label">初始代码（学生看到的）</div>
                        <a-textarea v-model:value="tpl.starterCode" :rows="8" placeholder="初始代码..." class="code-editor" />
                      </div>
                      <div class="template-block">
                        <div class="tpl-label">参考解（必须能通过所有测试用例）</div>
                        <a-textarea v-model:value="tpl.referenceSolution" :rows="8" placeholder="参考解..." class="code-editor" />
                      </div>
                    </a-tab-pane>
                  </a-tabs>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <!-- 通用聊天 / 学情分析 -->
      <div v-else class="chat-mode-container">
        <div ref="messagesBox" class="chat-scroll-area">
          <div v-if="messages.length === 0" class="empty-hero">
            <message-outlined class="hero-icon" />
            <h2>{{ currentAgentName }}</h2>
            <p>请输入你的问题，AI 将基于当前模式为你提供帮助。</p>

            <div v-if="currentSuggestions.length" class="suggestion-grid">
              <div
                v-for="item in currentSuggestions"
                :key="item"
                class="suggestion-card"
                @click="fillInput(item)"
              >
                <span>{{ item }}</span>
                <arrow-right-outlined class="arrow" />
              </div>
            </div>
          </div>

          <div v-else class="message-list">
            <div
              v-for="(item, index) in messages"
              :key="index"
              class="message-wrapper"
              :class="item.role"
            >
              <div class="avatar">
                <user-outlined v-if="item.role === 'user'" />
                <component :is="iconMap[currentAgentIcon]" v-else />
              </div>

              <div class="message-content">
                <div v-if="item.role === 'user'" class="user-text">
                  {{ item.content }}
                </div>

                <div v-else>
                  <div
                    v-if="item.content"
                    class="markdown-render"
                    v-html="renderMd(item.content)"
                  ></div>

                  <div v-else class="loading-bubble">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="floating-input-zone">
          <div class="input-container" :class="{ focused: isFocused, disabled: isGenerating }">
      <textarea
        ref="inputArea"
        v-model="inputText"
        rows="1"
        :disabled="isGenerating"
        :placeholder="isGenerating ? '引擎正在输出中，请稍候...' : `给 ${currentAgentName} 发送指令... (Shift + Enter 换行)`"
        @keydown.enter.prevent="handleEnter"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @input="autoResize"
      ></textarea>

            <button
              class="send-btn"
              :class="{ active: inputText.trim() && !isGenerating }"
              :disabled="!inputText.trim() || isGenerating"
              @click="sendMessage"
            >
              <send-outlined v-if="!isGenerating" />
              <loading-outlined v-else />
            </button>
          </div>
        </div>
      </div>
    </main>
    <TeachingCasePicker
      v-model:open="casePickerOpen"
      :selected-id="planForm.caseId"
      :recommend-context="caseRecommendContext"
      @select="handleCaseSelected"
    />
    <TeachingCasePreviewModal ref="casePreviewModal" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import MarkdownIt from 'markdown-it'
import { message } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { getAuthToken } from '@/utils/authStorage'
import { normalizeQuizMarkdownLayout } from '@/utils/paperParser'
import { buildSemesterOptions, getCurrentSemesterValue } from '@/utils/semester'
import AnimationWorkbench from '@/components/anim-player/AnimationWorkbench.vue'
import AnimationConfigPanel from '@/components/anim-player/AnimationConfigPanel.vue'
import MicroVideoGenerator from '@/views/teacher/MicroVideoGenerator.vue'
import RichTextEditor from '@/components/RichTextEditor.vue'
import TeachingCasePicker from '@/components/teacher/TeachingCasePicker.vue'
import TeachingCasePreviewModal from '@/components/teacher/TeachingCasePreviewModal.vue'
import {
  DeleteOutlined,
  ArrowRightOutlined,
  SendOutlined,
  LoadingOutlined,
  FileTextOutlined,
  CheckCircleFilled,
  CopyOutlined,
  SaveOutlined,
  DownloadOutlined,
  MessageOutlined,
  FormOutlined,
  DesktopOutlined,
  BarChartOutlined,
  FundProjectionScreenOutlined,
  UserOutlined,
  ThunderboltOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  CodeOutlined,
  PlusOutlined,
  VideoCameraOutlined,
  FolderOpenOutlined,
  EyeOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue'

import type {
  AnimFormModel,
  AnimOptimizeAction,
} from '@/components/anim-player/core/animTypes'

import { useAnimEngine } from '@/components/anim-player/composables/useAnimEngine'
import { extractJsonText } from '@/components/anim-player/core/animValidator'
import {
  recommendTeachingCases,
  type RecommendTeachingCasePayload,
  type RecommendedTeachingCaseItem,
  type TeachingCaseItem,
} from '@/api/case'

type ChatRole = 'user' | 'ai'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'
const AI_STREAM_URL = `${API_BASE}/ai/stream`
const AI_AGENT_STREAM_URL = `${API_BASE}/ai/agent/stream`
const route = useRoute()

const iconMap: Record<string, any> = {
  MessageOutlined,
  FileTextOutlined,
  FormOutlined,
  DesktopOutlined,
  BarChartOutlined,
  FundProjectionScreenOutlined,
  CodeOutlined,
  VideoCameraOutlined
}

const md = new MarkdownIt({ breaks: true, html: true })
const renderMd = (text: string) => {
  if (!text) return ''
  const processed = text.replace(
    /【案例参考】/g,
    '<span class="case-badge">📖 案例参考</span>'
  )
  return md.render(processed)
}

const graphPromptBlock = computed(() => '')

const currentType = ref('plan')

const teachingMethodTips: Record<string, string> = {
  '讲授演示法': '以教师讲解和示范为主，适合新知识引入、概念讲解和规范操作展示。',
  '案例教学法': '通过真实或典型案例组织教学，帮助学生在情境中理解知识和方法。',
  '项目驱动法': '围绕一个完整项目展开学习，强调任务完成、成果产出和综合应用。',
  '任务驱动法': '将学习目标拆解成具体任务，引导学生在完成任务过程中掌握知识。',
  '探究式学习': '通过提出问题、分析问题和自主探究，培养学生思考与解决问题能力。',
  '合作学习': '通过小组协作、讨论与分工完成学习活动，强化互动与共同建构。'
}

const agentConfig: Record<string, any> = {
  plan: {
    name: '教案生成',
    icon: 'FileTextOutlined',
    desc: '结构化输出标准教案',
    tips: []
  },
  quiz: {
    name: '智能出题',
    icon: 'FormOutlined',
    desc: '配置生成、编辑优化、保存导出一体化',
    tips: []
  },
  anim: {
    name: '互动课件生成',
    icon: 'DesktopOutlined',
    desc: '生成具有交互能力的 Web 原生演示组件',
    tips: []
  },
  micro_video: {
    name: '微课生成',
    icon: 'VideoCameraOutlined',
    desc: '生成图文讲解型微课视频并发布到课程选集',
    tips: []
  },
  ppt: {
    name: 'PPT生成',
    icon: 'FundProjectionScreenOutlined',
    desc: '内嵌专业级可视化编辑引擎',
    tips: []
  },
  coding: {
    name: '编程题生成',
    icon: 'CodeOutlined',
    desc: '一句话生成完整编程题（含测试用例与参考解）',
    tips: []
  }
}

const currentAgentName = computed(() => agentConfig[currentType.value].name)
const currentAgentIcon = computed(() => agentConfig[currentType.value].icon)
const currentSuggestions = computed(() => agentConfig[currentType.value].tips || [])
type GraphPolicy = 'auto' | 'graphFirst' | 'resourceFirst' | 'off'
const graphPolicy = ref<GraphPolicy>('auto')
const isChatMode = computed(() => !['plan', 'quiz', 'anim', 'micro_video', 'ppt', 'coding'].includes(currentType.value))
const isAnyGenerating = computed(() => {
  return (
    isGenerating.value ||
    isPlanGenerating.value ||
    isQuizGenerating.value ||
    isQuizOptimizing.value ||
    isAnimGenerating.value ||
    isAnimOptimizing.value ||
    isCodingGenerating.value
  )
})

const switchAgent = (type: string) => {
  if (currentType.value === type) return
  currentType.value = type
  if (type === 'report') {
    setTimeout(() => autoResize(), 50)
  }
}

watch(
  () => route.query.type,
  (type) => {
    const targetType = Array.isArray(type) ? type[0] : type
    if (targetType && agentConfig[targetType]) {
      switchAgent(targetType)
    }
  },
  { immediate: true },
)





const copyText = async (text: string) => {
  if (!text) {
    message.warning('没有可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制')
  } catch {
    message.error('复制失败')
  }
}

const downloadTextFile = (
  filename: string,
  content: string,
  mime = 'text/plain;charset=utf-8'
) => {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

const downloadBlobFile = (filename: string, blob: Blob) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

const saveResourceToCloudDB = async (
  type: string,
  title: string,
  content: string,
  paramsObj: any
) => {
  if (!content) {
    message.warning('没有可保存的内容')
    return
  }
  try {
    await request.post('/ai/resource/save', {
      type,
      title,
      content,
      paramsJson: JSON.stringify(paramsObj)
    })
    message.success('保存成功！已同步至云端资源库。')
  } catch (error) {
    console.error('云端保存失败:', error)
  }
}

const streamText = async (
  question: string,
  type: string,
  onChunk: (chunk: string) => void,
  caseId?: number
) => {
  const res = await fetch(AI_STREAM_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
    },
    credentials: 'include',
    body: JSON.stringify({ question, type, caseId })
  })

  if (!res.ok) {
    const errText = await res.text()
    throw new Error(errText || '请求失败')
  }

  if (!res.body) {
    throw new Error('未获取到返回流')
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    onChunk(decoder.decode(value, { stream: true }))
  }
}

type AgentStage = {
  name: string
  status: string
  detail?: string
}

type AgentCitation = {
  evidenceId?: string
  sourceType?: string
  sourceId?: string | number
  title?: string
  score?: number
  snippet?: string
  resourceType?: string
  reason?: string
  chunkId?: string
  sourceKey?: string
  pdfParseOk?: boolean
  parseStatus?: string
  useFor?: string[]
}

type AgentCaseAnalysis = {
  id?: string | number
  title?: string
  pdfParseOk?: boolean
  summary?: string
  coreSituation?: string
}

type AgentQualityReport = {
  score?: number
  checks?: Array<{
    name: string
    passed: boolean
    expected?: number
    actual?: number
  }>
}

const agentStages = ref<AgentStage[]>([])
const agentCitations = ref<AgentCitation[]>([])
const agentQualityReport = ref<AgentQualityReport | null>(null)
const agentCaseAnalysis = ref<AgentCaseAnalysis | null>(null)
const excludedCitationKeys = ref<string[]>([])
const agentMetaOwner = ref<string | null>(null)
const agentRetrievalDone = ref(false)
const agentWorkflowRunIds = ref<Record<string, string>>({})
const isPlanMetaExpanded = ref(false)

const agentTypeToPageType = (type: string | null) => {
  if (!type) return ''
  if (type === 'quiz_optimize') return 'quiz'
  if (type === 'anim_repair' || type === 'anim_optimize') return 'anim'
  return type
}

const agentMetaOwnerMatchesCurrentType = computed(() => {
  return agentTypeToPageType(agentMetaOwner.value) === currentType.value
})

const showAgentMeta = computed(() =>
  agentMetaOwnerMatchesCurrentType.value &&
  (
    agentStages.value.length > 0 ||
    agentCitations.value.length > 0 ||
    !!agentQualityReport.value ||
    !!agentCaseAnalysis.value
  )
)

const showPlanAgentMeta = computed(() =>
  currentType.value === 'plan' &&
  agentMetaOwnerMatchesCurrentType.value &&
  (
    agentStages.value.length > 0 ||
    agentCitations.value.length > 0 ||
    !!agentQualityReport.value ||
    !!agentCaseAnalysis.value ||
    agentRetrievalDone.value
  )
)

const planMetaSummary = computed(() => {
  const parts: string[] = []
  if (agentCitations.value.length) parts.push(`${agentCitations.value.length} 条资源`)
  if (agentCaseAnalysis.value) parts.push('案例分析')
  if (agentQualityReport.value) parts.push('质量检查')
  if (!parts.length && agentRetrievalDone.value) parts.push('暂无强相关资源')
  return parts.join('、')
})

const citationKey = (item: AgentCitation) => {
  return item.sourceKey || `${item.sourceType || 'context'}-${item.sourceId || item.chunkId || item.title || ''}`
}

const isCitationExcluded = (item: AgentCitation) => {
  return excludedCitationKeys.value.includes(citationKey(item))
}

const toggleCitationExcluded = (item: AgentCitation) => {
  const key = citationKey(item)
  if (!key) return
  if (excludedCitationKeys.value.includes(key)) {
    excludedCitationKeys.value = excludedCitationKeys.value.filter(itemKey => itemKey !== key)
  } else {
    excludedCitationKeys.value = [...excludedCitationKeys.value, key]
  }
}

const clearCitationExclusions = () => {
  excludedCitationKeys.value = []
}

const citationTypeLabel = (item: AgentCitation) => {
  const type = item.resourceType || item.sourceType || 'resource'
  const map: Record<string, string> = {
    graph_node: '知识点',
    ai_resource: '资源',
    plan: '教案',
    quiz: '习题',
    anim: '课件',
    coding: '编程题',
    case: '案例'
  }
  return map[type] || type
}

const citationReasonLabel = (reason?: string) => {
  const map: Record<string, string> = {
    pinned: '已固定',
    graph_resource: '绑定资源',
    graph_relation: '图谱关系',
    graph_priority: '图谱优先',
    graph_semantic: '图谱语义',
    resource_semantic: '资源语义',
    case_semantic: '案例语义',
    fallback_keyword: '关键词'
  }
  return reason ? (map[reason] || '') : ''
}

const citationScoreText = (score?: number) => {
  if (typeof score !== 'number') return ''
  return `${Math.round(Math.min(score, 1) * 100)}%`
}

const NO_RETRIEVAL_AGENT_TYPES = new Set(['anim', 'anim_repair', 'anim_optimize', 'coding'])

const buildRetrievalOptions = (agentType: string) => {
  if (NO_RETRIEVAL_AGENT_TYPES.has(agentType) || graphPolicy.value === 'off') {
    return {
      mode: 'off',
      topK: 0,
      graphPolicy: 'off',
      excludedSources: []
    }
  }

  const canReuseExclusions =
    agentMetaOwnerMatchesCurrentType.value &&
    agentTypeToPageType(agentType) === currentType.value

  return {
    mode: 'auto',
    topK: agentType === 'plan' ? 4 : 6,
    graphPolicy: graphPolicy.value,
    excludedSources: canReuseExclusions ? excludedCitationKeys.value : []
  }
}

const resetAgentMeta = () => {
  agentStages.value = []
  agentCitations.value = []
  agentQualityReport.value = null
  agentCaseAnalysis.value = null
  agentMetaOwner.value = null
  agentRetrievalDone.value = false
}

const upsertAgentStage = (event: any) => {
  const name = event.name || 'agent'
  const nextStage = {
    name,
    status: event.status || 'running',
    detail: event.detail
  }
  const index = agentStages.value.findIndex(item => item.name === name)
  if (index >= 0) {
    agentStages.value[index] = nextStage
  } else {
    agentStages.value.push(nextStage)
  }
}

const streamPrepareAgent = async (
  agentType: 'plan' | 'quiz' | 'quiz_optimize' | 'anim' | 'anim_repair' | 'anim_optimize' | 'coding',
  form: Record<string, any>,
  onChunk: (chunk: string) => void,
  options: { caseId?: number; caseIds?: number[]; sourceContent?: string; autoCase?: boolean } = {}
) => {
  const retrievalOptions = buildRetrievalOptions(agentType)
  if (agentType === 'plan' && typeof options.autoCase === 'boolean') {
    ;(retrievalOptions as Record<string, any>).autoCase = options.autoCase
  }
  agentMetaOwner.value = agentType
  const res = await fetch(AI_AGENT_STREAM_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
    },
    credentials: 'include',
    body: JSON.stringify({
      agentType,
      form,
      caseId: options.caseId,
      caseIds: options.caseIds,
      sourceContent: options.sourceContent,
      retrievalOptions
    })
  })

  if (!res.ok) {
    const errText = await res.text()
    throw new Error(errText || 'agent request failed')
  }

  if (!res.body) {
    throw new Error('empty agent stream')
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed) continue

      let event: any
      try {
        event = JSON.parse(trimmed)
      } catch {
        onChunk(line)
        continue
      }

      if (event.type === 'content') {
        onChunk(event.delta || '')
      } else if (event.type === 'workflow_state') {
        if (event.requestId) {
          const resourceType = agentTypeToPageType(agentType)
          agentWorkflowRunIds.value[resourceType] = String(event.requestId)
        }
      } else if (event.type === 'stage') {
        upsertAgentStage(event)
      } else if (event.type === 'citation') {
        agentCitations.value = event.items || []
        agentRetrievalDone.value = true
      } else if (event.type === 'case_analysis') {
        agentCaseAnalysis.value = event.item || null
      } else if (event.type === 'quality') {
        agentQualityReport.value = event.report || null
      } else if (event.type === 'error') {
        throw new Error(event.message || 'agent failed')
      }
    }
  }
}

const {
  animJsonResult,
  animRenderStatus,
  animValidationErrors,
  isAnimGenerating,
  isAnimOptimizing,
  currentAnimOptimizeAction,
  animAutoPlayInterval,
  generateAnimation: runGenerateAnimation,
  optimizeAnimation: runOptimizeAnimation,
  resetAnimEngine,
  getCurrentJsonText,
} = useAnimEngine({
  streamText,
  streamPrepareAgent,
  notify: {
    success: (text) => message.success(text),
    error: (text) => message.error(text),
    warning: (text) => message.warning(text),
  },
  initialAutoPlayInterval: 1800,
})


/* =========================
   通用聊天
========================= */
const inputText = ref('')
const isGenerating = ref(false)
const isFocused = ref(false)
const messagesBox = ref<HTMLElement | null>(null)
const inputArea = ref<HTMLTextAreaElement | null>(null)
const messages = ref<{ role: ChatRole; content: string }[]>([])

const fillInput = (text: string) => {
  inputText.value = text
  autoResize()
  inputArea.value?.focus()
}

const autoResize = () => {
  if (!inputArea.value) return
  inputArea.value.style.height = 'auto'
  inputArea.value.style.height = `${Math.min(inputArea.value.scrollHeight, 120)}px`
}

const handleEnter = (e: KeyboardEvent) => {
  if (e.shiftKey) return
  sendMessage()
  setTimeout(() => autoResize(), 10)
}

const clearHistory = () => {
  messages.value = []
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesBox.value) {
    messagesBox.value.scrollTop = messagesBox.value.scrollHeight
  }
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || isGenerating.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  autoResize()
  scrollToBottom()

  isGenerating.value = true
  const aiMsgIndex = messages.value.push({ role: 'ai', content: '' }) - 1

  try {
    await streamText(text, currentType.value, (chunk) => {
      messages.value[aiMsgIndex].content += chunk
      scrollToBottom()
    })
  } catch (error: any) {
    messages.value[aiMsgIndex].content = `⚠️ ${error?.message || '连接失败'}`
  } finally {
    isGenerating.value = false
  }
}

/* =========================
   教案生成
========================= */
const isPlanGenerating = ref(false)
const planResult = ref('')
const editablePlanContent = ref('')
const editablePlanHtml = ref('')
const isPlanEditing = ref(false)
const currentPlanId = ref<number | null>(null)
const isPlanExporting = ref(false)
const casePickerOpen = ref(false)
const selectedTeachingCase = ref<TeachingCaseItem | null>(null)
const recommendedCases = ref<RecommendedTeachingCaseItem[]>([])
const selectedPlatformCaseIds = ref<number[]>([])
const caseRecommendLoading = ref(false)
const caseRecommendationTouched = ref(false)
const caseSelectionTouched = ref(false)
const casePreviewModal = ref<InstanceType<typeof TeachingCasePreviewModal> | null>(null)
let recommendTimer: ReturnType<typeof setTimeout> | null = null

const selectedCaseIds = computed(() => {
  const ids = [...selectedPlatformCaseIds.value]
  if (selectedTeachingCase.value?.id && !ids.includes(selectedTeachingCase.value.id)) {
    ids.push(selectedTeachingCase.value.id)
  }
  return ids
})

const selectedCaseItems = computed(() => {
  const result: Array<{ id: number; title: string }> = []
  for (const id of selectedPlatformCaseIds.value) {
    const item = recommendedCases.value.find(candidate => candidate.id === id)
    result.push({
      id,
      title: item?.title || `案例 ${id}`,
    })
  }
  if (selectedTeachingCase.value && !result.some(item => item.id === selectedTeachingCase.value?.id)) {
    result.push({
      id: selectedTeachingCase.value.id,
      title: selectedTeachingCase.value.title,
    })
  }
  return result
})

const caseCategoryLabel = (category: string) => {
  const map: Record<string, string> = {
    course_design: '课程设计',
    enterprise: '企业实际工程',
    competition: '大赛资源',
    small_project: '小项目',
  }
  return map[category] || category || '未分类'
}

const caseDifficultyLabel = (difficulty: string) => {
  const map: Record<string, string> = {
    easy: '初级',
    medium: '中等',
    hard: '困难',
  }
  return map[difficulty] || difficulty || '未设置'
}

const caseDifficultyColor = (difficulty: string) => {
  const map: Record<string, string> = {
    easy: 'green',
    medium: 'orange',
    hard: 'red',
  }
  return map[difficulty] || 'default'
}

const recommendMatchLabel = (level?: RecommendedTeachingCaseItem['matchLevel']) => {
  const map: Record<string, string> = {
    precise: '精准匹配',
    evidence: '强关联',
    related: '仅相关',
    fallback: '兜底',
  }
  return level ? map[level] || '推荐' : '推荐'
}

const recommendMatchColor = (level?: RecommendedTeachingCaseItem['matchLevel']) => {
  const map: Record<string, string> = {
    precise: 'green',
    evidence: 'blue',
    related: 'orange',
    fallback: 'default',
  }
  return level ? map[level] || 'default' : 'default'
}

const openCasePicker = () => {
  casePickerOpen.value = true
}

const handleCaseSelected = (item: TeachingCaseItem) => {
  caseSelectionTouched.value = true
  selectedTeachingCase.value = item
  planForm.caseId = item.id
}

const clearSelectedCase = () => {
  caseSelectionTouched.value = true
  selectedTeachingCase.value = null
  syncPrimaryCaseId()
}

const previewSelectedCase = () => {
  if (!selectedTeachingCase.value) return
  casePreviewModal.value?.open(selectedTeachingCase.value.id)
}

const previewCaseById = (id: number) => {
  casePreviewModal.value?.open(id)
}

const syncPrimaryCaseId = () => {
  planForm.caseId = selectedCaseIds.value[0]
}

const removeSelectedCase = (id: number) => {
  caseSelectionTouched.value = true
  selectedPlatformCaseIds.value = selectedPlatformCaseIds.value.filter(itemId => itemId !== id)
  if (selectedTeachingCase.value?.id === id) {
    clearSelectedCase()
  }
  syncPrimaryCaseId()
}

const loadRecommendedCases = async () => {
  if (!planForm.subject || !planForm.topic) {
    caseRecommendationTouched.value = true
    recommendedCases.value = []
    if (!caseSelectionTouched.value) {
      selectedPlatformCaseIds.value = []
      planForm.caseId = undefined
    }
    return
  }
  caseRecommendLoading.value = true
  caseRecommendationTouched.value = true
  try {
    const data = await recommendTeachingCases({
      subject: planForm.subject,
      grade: planForm.grade,
      topic: planForm.topic,
      lessonType: planForm.lessonType,
      courseName: planForm.subject,
    })
    recommendedCases.value = data || []
    if (!caseSelectionTouched.value) {
      selectedPlatformCaseIds.value = recommendedCases.value
        .filter(item => item.matchLevel === 'precise' || item.matchLevel === 'evidence')
        .map(item => item.id)
      selectedTeachingCase.value = null
      syncPrimaryCaseId()
    } else {
      selectedPlatformCaseIds.value = selectedPlatformCaseIds.value.filter(id =>
        recommendedCases.value.some(item => item.id === id)
      )
    }
  } catch (error: any) {
    console.error('案例推荐失败:', error)
    recommendedCases.value = []
  } finally {
    caseRecommendLoading.value = false
  }
}

const scheduleCaseRecommendation = () => {
  if (recommendTimer) clearTimeout(recommendTimer)
  recommendTimer = setTimeout(() => {
    loadRecommendedCases()
  }, 600)
}

const toggleRecommendedCase = (item: RecommendedTeachingCaseItem) => {
  caseSelectionTouched.value = true
  if (selectedPlatformCaseIds.value.includes(item.id)) {
    selectedPlatformCaseIds.value = selectedPlatformCaseIds.value.filter(id => id !== item.id)
    syncPrimaryCaseId()
    return
  }
  selectedPlatformCaseIds.value = [...selectedPlatformCaseIds.value, item.id]
  syncPrimaryCaseId()
}

const planForm = reactive({
  subject: '',
  topic: '',
  grade: '本科一年级',
  lessonType: '新授课',
  duration: 45,
  lessonCount: 1,
  studentLevel: '一般',
  difficulties: ['概念抽象'],
  methods: ['讲授演示法'],
  activities: ['课堂提问设计', '随堂练习'],
  prereqKnowledge: '',
  extraRequirements: '',
  caseId: undefined as number | undefined,
})

const caseRecommendContext = computed<RecommendTeachingCasePayload>(() => ({
  subject: planForm.subject,
  grade: planForm.grade,
  topic: planForm.topic,
  lessonType: planForm.lessonType,
  courseName: planForm.subject,
}))

watch(
  () => [planForm.subject, planForm.topic, planForm.grade, planForm.lessonType],
  () => {
    if (planForm.subject && planForm.topic) {
      scheduleCaseRecommendation()
    }
  }
)

const planReflectionPattern = /教学反思|课后反思|反思|复盘/

const buildPlanAgentForm = () => {
  const selectedActivities = [...planForm.activities]
  const excludedSections: string[] = []

  if (!selectedActivities.includes('板书设计')) {
    excludedSections.push('板书设计')
  }
  if (!planReflectionPattern.test(planForm.extraRequirements || '')) {
    excludedSections.push('教学反思', '教学效果评价', '改进方向', '课后反思')
  }

  return {
    ...planForm,
    selectedMethods: [...planForm.methods],
    selectedActivities,
    excludedSections,
  }
}

const cleanPlanContent = (text: string) => {
  if (!text) return text

  const markers = ['# ', '## ', '《', '一、', '### 教学主题', '## 教学主题']
  let bestIndex = -1

  for (const marker of markers) {
    const idx = text.indexOf(marker)
    if (idx !== -1 && (bestIndex === -1 || idx < bestIndex)) {
      bestIndex = idx
    }
  }

  return bestIndex > 0 ? text.slice(bestIndex).trim() : text.trim()
}

/**
 * 清理 AI 生成内容中的参考标识（保存/导出前使用）
 * 清理：【案例参考】、【参考：E编号】及删除标记后残留的空 Markdown 加粗符号
 */
const stripReferenceMarkers = (text: string): string => {
  if (!text) return text
  return text
    .replace(/\*\*\s*【案例参考】\s*\*\*/g, '')
    .replace(/\*\*\s*【参考\s*[:：]\s*E\d+(?:[、,，\s]*E\d+)*】\s*\*\*/g, '')
    .replace(/【(?:案例参考|参考\s*[:：]\s*E\d+(?:[、,，\s]*E\d+)*)】\s*/g, '')
    .replace(/(^|\n)[ \t]*\*{4,}[ \t]*/g, '$1')
    .replace(/(^|\n)[ \t]*\*{2}[ \t]+/g, '$1')
    .replace(/[ \t]+\*{4,}(?=[ \t]*(?:\n|$))/g, '')
    .trim()
}

const markdownToEditorHtml = (text: string) => {
  return text ? renderMd(text) : ''
}

const htmlToMarkdown = (html: string): string => {
  if (!html) return ''
  if (typeof DOMParser === 'undefined') return html

  const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html')

  const normalizeBlock = (value: string) => value.replace(/\n{3,}/g, '\n\n').trim()
  const inlineText = (node: Node): string => Array.from(node.childNodes).map(walk).join('')
  const listText = (node: Element, ordered: boolean) => {
    return Array.from(node.children)
      .filter(child => child.tagName.toLowerCase() === 'li')
      .map((child, index) => {
        const content = normalizeBlock(inlineText(child)).replace(/\n/g, '\n  ')
        return `${ordered ? `${index + 1}.` : '-'} ${content}`
      })
      .join('\n')
  }

  const walk = (node: Node): string => {
    if (node.nodeType === Node.TEXT_NODE) {
      return node.textContent || ''
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return ''

    const element = node as Element
    const tag = element.tagName.toLowerCase()
    const content = inlineText(element)
    const block = (value: string) => `${normalizeBlock(value)}\n\n`

    if (/^h[1-6]$/.test(tag)) {
      const level = Number(tag.slice(1))
      return block(`${'#'.repeat(level)} ${normalizeBlock(content)}`)
    }
    if (tag === 'p') return block(content)
    if (tag === 'div') return block(content)
    if (tag === 'br') return '\n'
    if (tag === 'strong' || tag === 'b') return content ? `**${content}**` : ''
    if (tag === 'em' || tag === 'i') return content ? `*${content}*` : ''
    if (tag === 's' || tag === 'del') return content ? `~~${content}~~` : ''
    if (tag === 'blockquote') {
      return block(normalizeBlock(content).split('\n').map(line => `> ${line}`).join('\n'))
    }
    if (tag === 'ul') return block(listText(element, false))
    if (tag === 'ol') return block(listText(element, true))
    if (tag === 'li') return normalizeBlock(content)
    if (tag === 'a') {
      const href = element.getAttribute('href')
      return href && content ? `[${content}](${href})` : content
    }
    if (tag === 'pre') return block(`\`\`\`\n${element.textContent || ''}\n\`\`\``)
    if (tag === 'code') return content ? `\`${content}\`` : ''
    return content
  }

  return normalizeBlock(inlineText(doc.body.firstElementChild || doc.body))
}

const getEditablePlanMarkdown = () => htmlToMarkdown(editablePlanHtml.value).trim()

const getCurrentPlanMarkdown = () => {
  return (isPlanEditing.value ? getEditablePlanMarkdown() : planResult.value).trim()
}

const resetPlanForm = () => {
  Object.assign(planForm, {
    subject: '',
    topic: '',
    grade: '本科一年级',
    lessonType: '新授课',
    duration: 45,
    lessonCount: 1,
    studentLevel: '一般',
    difficulties: ['概念抽象'],
    methods: ['讲授演示法'],
    activities: ['课堂提问设计', '随堂练习'],
    prereqKnowledge: '',
    extraRequirements: '',
    caseId: undefined,
  })

  planResult.value = ''
  editablePlanContent.value = ''
  editablePlanHtml.value = ''
  currentPlanId.value = null
  isPlanEditing.value = false
  selectedTeachingCase.value = null
  selectedPlatformCaseIds.value = []
  caseSelectionTouched.value = false
  recommendedCases.value = []
  caseRecommendationTouched.value = false
  casePickerOpen.value = false
  isPlanMetaExpanded.value = false
  resetAgentMeta()
}

const startEditPlan = () => {
  if (!planResult.value) return
  editablePlanContent.value = planResult.value
  editablePlanHtml.value = markdownToEditorHtml(planResult.value)
  isPlanEditing.value = true
}

const cancelEditPlan = () => {
  editablePlanContent.value = planResult.value
  editablePlanHtml.value = markdownToEditorHtml(planResult.value)
  isPlanEditing.value = false
}

const applyEditedPlan = () => {
  const nextContent = getEditablePlanMarkdown()
  planResult.value = nextContent
  editablePlanContent.value = nextContent
  editablePlanHtml.value = markdownToEditorHtml(nextContent)
  isPlanEditing.value = false
  message.success('已更新本地教案内容，记得保存到云端')
}

const savePlanToCloud = async () => {
  const finalContent = stripReferenceMarkers(getCurrentPlanMarkdown())

  if (!finalContent) {
    message.warning('没有可保存的教案')
    return
  }

  const payload = {
    id: currentPlanId.value ?? undefined,
    type: 'plan',
    title: planForm.topic || 'AI生成教案',
    content: finalContent,
    paramsJson: JSON.stringify(buildPlanAgentForm()),
    agentRequestId: agentWorkflowRunIds.value.plan
  }

  try {
    if (currentPlanId.value) {
      await request.post<any, boolean>('/ai/resource/update', payload)
      message.success('教案修改已保存')
    } else {
      const newId = await request.post<any, number>('/ai/resource/save', payload)
      currentPlanId.value = Number(newId)
      message.success('教案已保存到云端')
    }


    planResult.value = finalContent
    editablePlanContent.value = finalContent
    editablePlanHtml.value = markdownToEditorHtml(finalContent)
    isPlanEditing.value = false
  } catch (error: any) {
    console.error('保存教案失败:', error)
    message.error(error?.message || '保存失败')
  }
}

const downloadPlanMd = () => {
  const finalContent = stripReferenceMarkers(getCurrentPlanMarkdown())
  if (!finalContent) {
    message.warning('没有可导出的教案')
    return
  }
  downloadTextFile(
    `${planForm.topic || 'AI教案'}.md`,
    finalContent,
    'text/markdown;charset=utf-8'
  )
}

const downloadPlan = async (format: 'docx') => {
  const finalContent = stripReferenceMarkers(getCurrentPlanMarkdown())
  if (!finalContent) {
    message.warning('娌℃湁鍙鍑虹殑鏁欐')
    return
  }
  isPlanExporting.value = true
  try {
    const res = await fetch(`${API_BASE}/ai/agent/export/plan`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(getAuthToken() ? { Authorization: `Bearer ${getAuthToken()}` } : {}),
      },
      credentials: 'include',
      body: JSON.stringify({
        format,
        title: planForm.topic || 'AI教案',
        contentMarkdown: finalContent,
      }),
    })
    if (!res.ok) {
      throw new Error(await res.text() || '导出失败')
    }
    const blob = await res.blob()
    downloadBlobFile(`${planForm.topic || 'AI教案'}.${format}`, blob)
    message.success('导出完成')
  } catch (error: any) {
    console.error('导出教案失败:', error)
    message.error(error?.message || '导出失败')
  } finally {
    isPlanExporting.value = false
  }
}

const generatePlan = async () => {
  if (!planForm.subject || !planForm.topic) {
    message.warning('请先填写学科和教学课题')
    return
  }

  isPlanGenerating.value = true
  planResult.value = ''
  editablePlanContent.value = ''
  editablePlanHtml.value = ''
  isPlanEditing.value = false
  currentPlanId.value = null
  isPlanMetaExpanded.value = false
  resetAgentMeta()

  try {
    if (!caseSelectionTouched.value && selectedCaseIds.value.length === 0) {
      await loadRecommendedCases()
    }
    let raw = ''
    const caseIds = selectedCaseIds.value
    await streamPrepareAgent('plan', buildPlanAgentForm(), (chunk) => {
      raw += chunk
      planResult.value = raw
    }, { caseId: caseIds[0], caseIds, autoCase: !caseSelectionTouched.value })

    planResult.value = cleanPlanContent(raw)
    editablePlanContent.value = planResult.value
    editablePlanHtml.value = markdownToEditorHtml(planResult.value)
  } catch (error: any) {
    message.error(error?.message || '教案生成失败，请检查网络或后端服务')
  } finally {
    isPlanGenerating.value = false
  }
}

/* =========================
   智能出题
========================= */
type QuizOptimizeAction =
  | 'basicWeak'
  | 'harder'
  | 'application'
  | 'distractors'
  | 'conciseAnalysis'

const isQuizGenerating = ref(false)
const isQuizOptimizing = ref(false)
const quizResult = ref('')
const editableQuizContent = ref('')
const isQuizEditing = ref(false)
const currentQuizId = ref<number | null>(null)
const currentQuizOptimizeAction = ref<QuizOptimizeAction | ''>('')

const QUIZ_TYPE_OPTIONS = [
  '单项选择题',
  '多项选择题',
  '判断题',
  '填空题',
  '简答题',
  '案例分析题'
] as const
type QuizType = typeof QUIZ_TYPE_OPTIONS[number]

const createDefaultQuizForm = () => ({
  subject: '',
  knowledgePoints: '',
  scenario: '课堂检测',
  typeCounts: {
    单项选择题: 5,
    多项选择题: 0,
    判断题: 3,
    填空题: 2,
    简答题: 0,
    案例分析题: 0
  } as Record<QuizType, number>,
  difficulty: '中等',
  style: '标准规范型',
  analysisDetail: '标准',
  preferFoundation: '否',
  preferApplication: '否',
  extraRequirements: ''
})

const quizForm = reactive(createDefaultQuizForm())

const updateQuizTypeCount = (type: QuizType, value: number | null | undefined) => {
  const n = Number(value)
  quizForm.typeCounts[type] = Number.isFinite(n) && n > 0 ? Math.floor(n) : 0
}

const quizTotalCount = computed(() =>
  QUIZ_TYPE_OPTIONS.reduce((sum, t) => sum + (quizForm.typeCounts[t] || 0), 0)
)

const activeQuizTypes = computed(() =>
  QUIZ_TYPE_OPTIONS.filter(t => (quizForm.typeCounts[t] || 0) > 0)
)

const quizTypeBreakdownText = computed(() =>
  activeQuizTypes.value
    .map(t => `- ${t}：${quizForm.typeCounts[t]} 道`)
    .join('\n')
)

const chineseSectionNo = (index: number) => {
  const nums = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
  return nums[index] || String(index + 1)
}

const quizFixedFormatText = computed(() => {
  const questionSections = activeQuizTypes.value.map((type, index) => {
    const count = quizForm.typeCounts[type] || 0
    const prefix = `${chineseSectionNo(index)}、${type}（共${count}题）`
    const secondLine = count > 1 ? '\n2、题干（  ）。' : ''
    if (type === '单项选择题' || type === '多项选择题') {
      const secondChoice = count > 1
        ? `
2、题干（  ）。
A. 选项内容
B. 选项内容
C. 选项内容
D. 选项内容`
        : ''
      return `${prefix}
1、题干（  ）。
A. 选项内容
B. 选项内容
C. 选项内容
D. 选项内容${secondChoice}`
    }
    if (type === '判断题') {
      return `${prefix}
1、题干。（  ）${secondLine}`
    }
    if (type === '填空题') {
      const secondFill = count > 1 ? '\n2、题干______。' : ''
      return `${prefix}
1、题干______。${secondFill}`
    }
    const secondText = count > 1 ? '\n2、题干。' : ''
    return `${prefix}
1、题干。${secondText}`
  }).join('\n\n')

  const answerSections = activeQuizTypes.value.map((type, index) => {
    const count = quizForm.typeCounts[type] || 0
    const secondAnswer = count > 1
      ? `
2、答案：...
解析：...`
      : ''
    return `${chineseSectionNo(index)}、${type}答案解析（共${count}题）
1、答案：...
解析：...${secondAnswer}`
  }).join('\n\n')

  return `${questionSections}

---

## 参考答案与解析

${answerSections}`
})

const quizQuickActions: Array<{ key: QuizOptimizeAction; label: string }> = [
  { key: 'basicWeak', label: '更适合基础薄弱学生' },
  { key: 'harder', label: '提高整体难度' },
  { key: 'distractors', label: '增强选择题干扰项' },
  { key: 'conciseAnalysis', label: '精简答案解析' }
]

const cleanQuizContent = (text: string) => {
  if (!text) return ''

  let content = text
    .trim()
    .replace(/^```(?:markdown)?\s*/i, '')
    .replace(/```$/i, '')
    .trim()

  const lines = content.split('\n')

  const startIndex = lines.findIndex((line) => {
    const t = line.trim()
    if (!t) return false

    return (
      /^(#\s*)?(习题|习题集|练习题|练习卷|试卷|测试卷)/.test(t) ||
      /^(#\s*)?(一[、.．]|1[、.．\)])/.test(t) ||
      /^#{1,3}\s*(单项选择题|多项选择题|判断题|填空题|简答题|案例分析题|参考答案与解析)/.test(t) ||
      /^(单项选择题|多项选择题|判断题|填空题|简答题|案例分析题)/.test(t)
    )
  })

  if (startIndex > 0) {
    content = lines.slice(startIndex).join('\n').trim()
  }

  content = content
    .replace(/^(好的[，、。]?\s*|下面.*?\n|以下是.*?\n|已根据.*?\n|我已为你.*?\n)+/i, '')
    .trim()

  return content
}

const getQuizFinalContent = () => {
  return (isQuizEditing.value ? editableQuizContent.value : quizResult.value).trim()
}

const buildQuizTitle = () => {
  const knowledge = (quizForm.knowledgePoints || '')
    .replace(/\n+/g, '、')
    .replace(/\s+/g, ' ')
    .trim()

  const shortKnowledge = knowledge ? knowledge.slice(0, 30) : '综合练习'
  const shortSubject = (quizForm.subject || 'AI').trim()

  return `${shortSubject}-${shortKnowledge}习题`
}

const buildQuizPrompt = () => {
  const breakdown = quizTypeBreakdownText.value
  const total = quizTotalCount.value
  return `
${graphPromptBlock.value ? `${graphPromptBlock.value}\n\n` : ''}请根据以下配置生成一份正式习题正文：

【所属学科】${quizForm.subject}
【考核知识点】${quizForm.knowledgePoints}
【使用场景】${quizForm.scenario}
【题型与数量配置（必须严格按此分布出题）】
${breakdown}
【题目总数】${total} 道（即上述各题型数量之和，不能多也不能少）
【总体难度】${quizForm.difficulty}
【题目风格】${quizForm.style}
【解析详略】${quizForm.analysisDetail}
【是否偏基础题】${quizForm.preferFoundation}
【是否偏应用题】${quizForm.preferApplication}
【其他要求】${quizForm.extraRequirements || '无'}

【必须照抄的排版骨架】
${quizFixedFormatText.value}

请严格遵守以下规则：
1. 必须严格按【题型与数量配置】中每种题型的数量进行命题，每种题型的题数必须完全一致，不能多也不能少。
2. 绝对禁止出现配置中未列出（数量为 0）的题型，例如配置里没有"多项选择题"就绝对不能出多项选择题。
3. 总题数必须严格等于 ${total} 道。
4. 每个题型必须单独成节，按【题型与数量配置】中的顺序排列。
5. 每个题型内部题号都必须从 1 开始递增，进入下一个题型后重新从 1 开始。
   例如：一、单项选择题 1-5 题；二、判断题必须重新写 1-3 题；三、填空题必须重新写 1-2 题。
6. 必须采用"题目区 + 答案解析区"分离的结构。
7. 前半部分只输出题目，不得提前泄露答案。
8. 在题目区结束后，单独输出一行 ---
9. 然后统一输出：## 参考答案与解析
10. 参考答案与解析也必须按题型分节，题型顺序与题目区完全一致；每个答案小节内部也从 1 开始编号。
11. 题型标题固定写法为“一、单项选择题（共N题）”“二、判断题（共N题）”“三、填空题（共N题）”，不要省略“共N题”。
12. 每道题题号固定使用中文顿号格式，如“1、题干”，不要使用“1.”。
13. 选择题参考答案只能写选项字母（如 A、B、C；多选写 AC），不要写选项内容；判断题只写“正确”或“错误”。
14. 排版必须严格换行：题干独占一行；每个选项必须独占一行；每道题之间空一行；代码块围栏必须独占一行；下一题题号必须另起一行。
15. 禁止写成“题干 A. 选项”“1. 题干 2. 题干”“解析。2. 答案”“判断题全部在同一行”这种粘连格式。
16. 不要在题干后添加【参考：E1】等参考来源标记。
17. 不要输出任何前言、说明、提示语、客套话。
18. 直接从习题标题、题型标题或第一道题开始输出。
`.trim()
}

const resetQuizForm = () => {
  Object.assign(quizForm, createDefaultQuizForm())
  quizResult.value = ''
  editableQuizContent.value = ''
  isQuizEditing.value = false
  currentQuizId.value = null
  currentQuizOptimizeAction.value = ''
  resetAgentMeta()
}

const startEditQuiz = () => {
  if (!quizResult.value) return
  editableQuizContent.value = quizResult.value
  isQuizEditing.value = true
}

const cancelEditQuiz = () => {
  editableQuizContent.value = quizResult.value
  isQuizEditing.value = false
}

const applyEditedQuiz = () => {
  const finalContent = editableQuizContent.value.trim()
  if (!finalContent) {
    message.warning('习题内容不能为空')
    return
  }
  quizResult.value = finalContent
  editableQuizContent.value = finalContent
  isQuizEditing.value = false
  message.success('已应用习题修改，记得保存到云端')
}

const saveQuizToCloud = async () => {
  const finalContent = stripReferenceMarkers(getQuizFinalContent())

  if (!finalContent) {
    message.warning('没有可保存的习题')
    return
  }

  const payload = {
    id: currentQuizId.value ?? undefined,
    type: 'quiz',
    title: buildQuizTitle(),
    content: finalContent,
    agentRequestId: agentWorkflowRunIds.value.quiz,
    paramsJson: JSON.stringify({
      ...quizForm,
      agentCitations: agentCitations.value,
      agentQualityReport: agentQualityReport.value
    })
  }

  try {
    if (currentQuizId.value) {
      await request.post<any, boolean>('/ai/resource/update', payload)
      message.success('习题修改已保存')
    } else {
      const newId = await request.post<any, number>('/ai/resource/save', payload)
      currentQuizId.value = Number(newId)
      message.success('习题已保存到云端')
    }

    quizResult.value = finalContent
    editableQuizContent.value = finalContent
    isQuizEditing.value = false
  } catch (error: any) {
    console.error('保存习题失败:', error)
    message.error(error?.message || '保存失败')
  }
}

const downloadQuiz = () => {
  const finalContent = stripReferenceMarkers(getQuizFinalContent())

  if (!finalContent) {
    message.warning('没有可导出的习题')
    return
  }

  downloadTextFile(
    `${buildQuizTitle()}.md`,
    finalContent,
    'text/markdown;charset=utf-8'
  )
}

const QUIZ_TYPE_TITLES = [
  '单项选择题',
  '多项选择题',
  '判断题',
  '填空题',
  '简答题',
  '案例分析题',
]

const quizSectionHeaderRegex = new RegExp(
  `^\\s*(?:#{1,6}\\s*)?(?:[一二三四五六七八九十]+[、.．]\\s*)?(?:${QUIZ_TYPE_TITLES.join('|')}).*$`
)

const splitQuizParts = (content: string) => {
  const parts = content.split(/\n\s*---\s*\n/)
  return {
    questionPart: (parts[0] || '').trim(),
    answerPart: parts.length > 1 ? parts.slice(1).join('\n---\n').trim() : ''
  }
}

const countQuizQuestions = (content: string) => {
  const { questionPart } = splitQuizParts(content)
  const matches = questionPart.match(/^\s*\d+[.、．\)]\s+/gm)
  return matches ? matches.length : 0
}

/** 内部工具：在一段文本里按题型分段计数 */
const tallyByType = (text: string): Record<string, number> => {
  const result: Record<string, number> = {}
  let currentType = ''

  for (const raw of text.split('\n')) {
    // 归一化后用于识别标题：去掉 md 井号、粗体/斜体标记
    const titleLine = raw
      .replace(/^\s*#{1,6}\s*/, '')
      .replace(/\*\*/g, '')
      .replace(/__/g, '')
      .trim()

    // 标题：容许 "第X"、全/半角括号、中文或阿拉伯编号、可选顿号/句点
    const hitType = QUIZ_TYPE_TITLES.find((t) => {
      const re = new RegExp(
        `^(?:第\\s*)?(?:[（(]?[一二三四五六七八九十\\d]+[）)]?[、.．]?\\s*)?${t}`
      )
      return re.test(titleLine)
    })
    if (hitType) {
      currentType = hitType
      if (!(currentType in result)) result[currentType] = 0
      continue
    }

    // 题号行：容许行首粗体星号、任意空格缩进
    const contentLine = raw.trimStart().replace(/^\*+\s*/, '')
    if (currentType && /^\d+\s*[.、．\)）]/.test(contentLine)) {
      result[currentType] = (result[currentType] || 0) + 1
    }
  }
  return result
}

/** 按题型统计实际题数（题目区与答案区各扫一次，逐题型取较大值，两边都能兜底） */
const countQuizQuestionsByType = (content: string): Record<string, number> => {
  const { questionPart, answerPart } = splitQuizParts(content)
  const fromQuestion = tallyByType(questionPart)
  const fromAnswer = answerPart ? tallyByType(answerPart) : {}

  const merged: Record<string, number> = {}
  for (const t of QUIZ_TYPE_OPTIONS) {
    merged[t] = Math.max(fromQuestion[t] || 0, fromAnswer[t] || 0)
  }
  return merged
}

/** 对比实际与期望，返回不匹配的文字描述（为空说明完全匹配） */
const diffQuizCounts = (actual: Record<string, number>): string[] => {
  const mismatches: string[] = []
  for (const t of QUIZ_TYPE_OPTIONS) {
    const exp = quizForm.typeCounts[t] || 0
    const act = actual[t] || 0
    if (exp !== act) {
      mismatches.push(`${t} 期望 ${exp} 道 / 实际 ${act} 道`)
    }
  }
  return mismatches
}

const normalizeQuizSectionNumbering = (block: string) => {
  if (!block) return ''

  const lines = block.split('\n')
  let currentNo = 0
  let inQuizSection = false

  return lines
    .map((line) => {
      const trimmed = line.trim()

      if (quizSectionHeaderRegex.test(trimmed)) {
        inQuizSection = true
        currentNo = 0
        return line
      }

      if (!inQuizSection) {
        return line
      }

      return line.replace(
        /^(\s*)(\d+)([.、．\)）])(\s*)/,
        (_, indent) => `${indent}${++currentNo}、`
      )
    })
    .join('\n')
}

const normalizeQuizContent = (content: string) => {
  const cleaned = normalizeQuizMarkdownLayout(stripReferenceMarkers(cleanQuizContent(content)))
  const { questionPart, answerPart } = splitQuizParts(cleaned)

  const normalizedQuestionPart = normalizeQuizSectionNumbering(questionPart)
  const normalizedAnswerPart = answerPart
    ? normalizeQuizSectionNumbering(answerPart)
    : ''

  return normalizedAnswerPart
    ? `${normalizedQuestionPart}\n\n---\n\n${normalizedAnswerPart}`.trim()
    : normalizedQuestionPart.trim()
}

const repairQuizStructureIfNeeded = async (content: string) => {
  const normalized = normalizeQuizContent(content)
  const actualByType = countQuizQuestionsByType(normalized)
  const mismatches = diffQuizCounts(actualByType)

  if (mismatches.length === 0) {
    return normalized
  }

  // 【保险】总数对得上就不修复，只在控制台留痕
  // 避免"按题型识别失败"误触发重做，把原本正确的内容搞坏
  const expected = quizTotalCount.value
  const rawTotal = countQuizQuestions(normalized)
  const totalByType = Object.values(actualByType).reduce((a, b) => a + b, 0)
  if (rawTotal === expected || totalByType === expected) {
    console.warn('[quiz] 按题型计数有偏差但总数正确，跳过自动修复：', mismatches)
    return normalized
  }

  message.warning(`题型数量不匹配（${mismatches.join('；')}），正在自动修正...`)

  const breakdown = quizTypeBreakdownText.value
  let repairedRaw = ''
  const repairPrompt = `
请修复下面这份习题，只做结构修复，不要解释，不要前言。

【修复后必须严格满足的题型与数量】
${breakdown}
（总共 ${quizTotalCount.value} 道题）

【必须照抄的排版骨架】
${quizFixedFormatText.value}

【修复要求】
1. 每种题型的题数必须与上方列表完全一致，多则删、少则补。
2. 绝对不能出现上表未列出的题型（含数量为 0 的题型）。
3. 总题数必须严格等于 ${quizTotalCount.value} 道。
4. 每个题型必须单独成节，按列表顺序排列，每个题型必须带“X、题型名（共N题）”这样的小节标题。
5. 每个题型内部题号都必须从 1 开始递增，进入下一个题型后重新从 1 开始。
6. 题目区与答案解析区必须分离：先输出题目区，再单独输出一行 ---，然后统一输出：## 参考答案与解析
7. 答案解析区也必须按题型分节，顺序与题目区完全一致；每个答案小节内部也从 1 开始编号。
8. 每道题题号固定使用中文顿号格式，如“1、题干”，不要使用“1.”。
9. 选择题参考答案只能写选项字母（如 A、B、C；多选写 AC），不要写选项内容；判断题只写“正确”或“错误”。
10. 排版必须严格换行：题干独占一行；每个选项必须独占一行；每道题之间空一行；代码块围栏必须独占一行；下一题题号必须另起一行。
11. 禁止写成“题干 A. 选项”“1. 题干 2. 题干”“解析。2. 答案”“判断题全部在同一行”这种粘连格式。
12. 不要在题干后添加【参考：E1】等参考来源标记。
13. 不要输出说明、解释、总结、提示语；直接输出修复后的完整习题正文。

【当前习题全文】
${normalized}
`.trim()

  await streamText(repairPrompt, 'quiz', (chunk) => {
    repairedRaw += chunk
    quizResult.value = repairedRaw
  })

  return normalizeQuizContent(repairedRaw)
}
const generateQuiz = async () => {
  if (!quizForm.subject || !quizForm.knowledgePoints) {
    message.warning('请先填写所属学科和考核知识点')
    return
  }

  if (quizTotalCount.value === 0) {
    message.warning('请至少为一种题型配置数量')
    return
  }

  isQuizGenerating.value = true
  quizResult.value = ''
  editableQuizContent.value = ''
  isQuizEditing.value = false
  currentQuizId.value = null
  currentQuizOptimizeAction.value = ''
  resetAgentMeta()

  const fullPrompt = buildQuizPrompt()
  void fullPrompt

  try {
    let raw = ''
    await streamPrepareAgent('quiz', { ...quizForm }, (chunk) => {
      raw += chunk
      quizResult.value = raw
    })

    let finalContent = normalizeQuizContent(raw)
    finalContent = await repairQuizStructureIfNeeded(finalContent)
    finalContent = normalizeQuizContent(finalContent)

    quizResult.value = finalContent
    editableQuizContent.value = finalContent

    const mismatches = diffQuizCounts(countQuizQuestionsByType(finalContent))
    const rawTotal = countQuizQuestions(finalContent)
    if (mismatches.length === 0 || rawTotal === quizTotalCount.value) {
      message.success('习题生成完成')
    } else {
      message.warning(`习题已生成，但仍存在题型数量偏差：${mismatches.join('；')}`)
    }
  } catch (error: any) {
    message.error(error?.message || '生成失败，请检查网络')
  } finally {
    isQuizGenerating.value = false
  }
}

const optimizeQuiz = async (action: QuizOptimizeAction) => {
  const sourceContent = getQuizFinalContent()

  if (!sourceContent) {
    message.warning('请先生成或编辑一份习题')
    return
  }

  const actionPromptMap: Record<QuizOptimizeAction, string> = {
    basicWeak:
      '在不改变总题数的前提下，让整份习题更适合基础薄弱学生，适当降低门槛，突出基础概念、基础方法和循序渐进的梯度。',
    harder:
      '在不改变总题数的前提下，提高整份习题的整体难度，增强思维深度、综合性和区分度。',
    application:
      '在不改变总题数的前提下，增加综合应用题倾向，突出情境化、案例化、实际问题解决能力。',
    distractors:
      '重点优化选择题，让错误选项更有迷惑性但仍然合理，提升干扰项质量；非选择题可做轻微协调优化。',
    conciseAnalysis:
      '保留答案正确性与必要解释的前提下，整体精简答案解析，避免冗长重复。'
  }

  isQuizOptimizing.value = true
  currentQuizOptimizeAction.value = action
  resetAgentMeta()

  try {
    let raw = ''
    const optimizePrompt = `
你将对一份现有习题进行二次优化，请直接输出优化后的完整习题正文。

【优化目标】
${actionPromptMap[action]}

【必须遵守的题型与数量（不可改动）】
${quizTypeBreakdownText.value}
（总共 ${quizTotalCount.value} 道题）

【必须照抄的排版骨架】
${quizFixedFormatText.value}

【必须遵守】
1. 每种题型的题数必须完全保持上述配置，不得增减、不得互换。
2. 必须继续保持"题目区"和"答案解析区"分离。
3. 必须先输出题目，再单独输出一行 ---
4. 在 --- 之后统一输出：## 参考答案与解析
5. 题目区和答案解析区都必须按题型分节，题型顺序保持一致。
6. 每个题型内部题号都必须从 1 开始递增，进入下一个题型后重新从 1 开始。
7. 每道题题号固定使用中文顿号格式，如“1、题干”，不要使用“1.”。
8. 选择题参考答案只能写选项字母（如 A、B、C；多选写 AC），不要写选项内容；判断题只写“正确”或“错误”。
9. 排版必须严格换行：题干独占一行；每个选项必须独占一行；每道题之间空一行；下一题题号必须另起一行。
10. 禁止写成“题干 A. 选项”“1. 题干 2. 题干”“解析。2. 答案”“判断题全部在同一行”这种粘连格式。
11. 不要在题干后添加【参考：E1】等参考来源标记。
12. 不要输出前言、说明、修改说明、总结语，直接输出优化后的正文。
13. 若当前习题已经符合要求，则在原结构上做最小必要优化。

【当前习题全文】
${sourceContent}
`.trim()
    void optimizePrompt

    await streamPrepareAgent('quiz_optimize', { ...quizForm, optimizeAction: action }, (chunk) => {
      raw += chunk
      quizResult.value = raw
    }, { sourceContent })

    const cleaned = normalizeQuizContent(raw)
    const finalContent = await repairQuizStructureIfNeeded(cleaned)

    quizResult.value = finalContent
    editableQuizContent.value = finalContent
    isQuizEditing.value = false
    message.success('习题优化完成')
  } catch (error: any) {
    message.error(error?.message || '优化失败，请检查网络')
  } finally {
    isQuizOptimizing.value = false
    currentQuizOptimizeAction.value = ''
  }
}

/* =========================
   交互课件（JSON 模板播放器版）
========================= */

const createDefaultAnimForm = (): AnimFormModel => ({
  concept: '',
  conceptType: 'auto',
  targetGroup: '本科一年级',
  teachingGoal: '帮助学生理解概念本质与运行过程',
  emphasis: '',
  extraRequirements: ''
})

const animForm = reactive(createDefaultAnimForm())
const currentAnimId = ref<number | null>(null)

const generateAnimation = async () => {
  currentAnimId.value = null
  resetAgentMeta()
  return await runGenerateAnimation(animForm)
}

const optimizeAnimation = async (action: AnimOptimizeAction) => {
  resetAgentMeta()
  return await runOptimizeAnimation(action, animForm)
}

const resetAnimForm = () => {
  Object.assign(animForm, createDefaultAnimForm())
  resetAnimEngine()
  currentAnimId.value = null
  resetAgentMeta()
}

const applyAnimExample = (example: Pick<AnimFormModel, 'concept' | 'conceptType'>) => {
  animForm.concept = example.concept
  animForm.conceptType = example.conceptType
  currentAnimId.value = null
}

const handleQuickOptimize = async (action: AnimOptimizeAction) => {
  await optimizeAnimation(action)
}

const copyAnimJson = async () => {
  if (!animJsonResult.value) {
    message.warning('没有可复制的 JSON')
    return
  }

  await copyText(getCurrentJsonText())
}

const saveAnimComponent = async () => {
  if (!animJsonResult.value) {
    message.warning('没有可保存的课件 JSON')
    return
  }

  const jsonContent = getCurrentJsonText()
  if (!jsonContent) {
    message.warning('无法获取课件内容')
    return
  }

  const title = animForm.concept
    ? `${animForm.concept}-交互课件`
    : 'AI生成交互课件'

  const payload = {
    id: currentAnimId.value ?? undefined,
    type: 'anim',
    title,
    content: jsonContent,
    agentRequestId: agentWorkflowRunIds.value.anim,
    paramsJson: JSON.stringify({
      ...animForm,
      agentCitations: agentCitations.value,
      agentQualityReport: agentQualityReport.value
    })
  }

  try {
    if (currentAnimId.value) {
      await request.post<any, boolean>('/ai/resource/update', payload)
      message.success('课件修改已保存')
    } else {
      const newId = await request.post<any, number>('/ai/resource/save', payload)
      currentAnimId.value = Number(newId)
      message.success('课件已保存到云端')
    }

  } catch (error: any) {
    console.error('保存课件失败:', error)
    message.error(error?.message || '保存失败')
  }
}

/* =========================
   编程题生成
========================= */
const isCodingGenerating = ref(false)
const codingResult = ref<any>(null)
const currentCodingId = ref<number | null>(null)
const semesterOptions = buildSemesterOptions()
const currentSemesterLabel = getCurrentSemesterValue()

const testCaseColumns = [
  { title: '输入', dataIndex: 'input', width: '22%' },
  { title: '期望输出', dataIndex: 'expectedOutput', width: '22%' },
  { title: '样例', dataIndex: 'isSample', width: '10%' },
  { title: '分值', dataIndex: 'score', width: '10%' },
  { title: '排序', dataIndex: 'sortOrder', width: '10%' },
  { title: '操作', dataIndex: 'action', width: '10%' }
]

const codingForm = reactive({
  description: '',
  languages: ['java'] as string[],
  difficulty: 'easy'
})

const resetCodingForm = () => {
  Object.assign(codingForm, {
    description: '',
    languages: ['java'],
    difficulty: 'easy'
  })
  codingResult.value = null
  currentCodingId.value = null
}

const normalizeCodingAgentResult = (raw: string) => {
  const data = JSON.parse(extractJsonText(raw))
  const languages = Array.isArray(data.languages) && data.languages.length
    ? data.languages
    : [...codingForm.languages]
  const testCases = Array.isArray(data.testCases)
    ? data.testCases.map((item: any, index: number) => ({
      input: item?.input ?? '',
      expectedOutput: item?.expectedOutput ?? '',
      isSample: Number(item?.isSample ?? 0),
      score: Number(item?.score ?? 0),
      sortOrder: Number(item?.sortOrder ?? index)
    }))
    : []
  const templates = Array.isArray(data.templates)
    ? data.templates.map((item: any) => ({
      language: item?.language ?? '',
      starterCode: item?.starterCode ?? '',
      referenceSolution: item?.referenceSolution ?? ''
    }))
    : []

  if (!data.title || !data.description) {
    throw new Error('AI 返回的编程题缺少标题或题面')
  }
  if (!testCases.length) {
    throw new Error('AI 返回的编程题缺少测试用例')
  }
  if (!templates.length) {
    throw new Error('AI 返回的编程题缺少代码模板')
  }

  return {
    title: data.title,
    description: data.description,
    difficulty: data.difficulty || codingForm.difficulty,
    semesterLabel: data.semesterLabel || currentSemesterLabel,
    languages,
    timeLimitMs: Number(data.timeLimitMs ?? 5000),
    memoryLimitKb: Number(data.memoryLimitKb ?? 262144),
    testCases,
    templates
  }
}

const generateCodingProblem = async () => {
  if (!codingForm.description) {
    message.warning('请先填写需求描述')
    return
  }
  if (codingForm.languages.length === 0) {
    message.warning('请至少选择一种编程语言')
    return
  }

  isCodingGenerating.value = true
  codingResult.value = null
  currentCodingId.value = null
  resetAgentMeta()

  try {
    let raw = ''
    await streamPrepareAgent('coding', {
      description: codingForm.description,
      languages: [...codingForm.languages],
      difficulty: codingForm.difficulty
    }, (chunk) => {
      raw += chunk
    })
    codingResult.value = normalizeCodingAgentResult(raw)
    message.success('编程题生成成功')
  } catch (error: any) {
    console.error('生成编程题失败:', error)
    message.error(error?.message || '智能体生成失败，请检查网络或后端服务')
  } finally {
    isCodingGenerating.value = false
  }
}

const addTestCase = () => {
  if (!codingResult.value) return
  if (!codingResult.value.testCases) {
    codingResult.value.testCases = []
  }
  codingResult.value.testCases.push({
    input: '',
    expectedOutput: '',
    isSample: 0,
    score: 10
  })
}

const removeTestCase = (index: number) => {
  if (!codingResult.value?.testCases) return
  codingResult.value.testCases.splice(index, 1)
}

const saveCodingToBank = async () => {
  if (!codingResult.value) {
    message.warning('没有可保存的编程题')
    return
  }
  const payload = {
    title: codingResult.value.title,
    description: codingResult.value.description,
    difficulty: codingResult.value.difficulty,
    semesterLabel: codingResult.value.semesterLabel || currentSemesterLabel,
    languages: codingResult.value.languages,
    timeLimitMs: codingResult.value.timeLimitMs ?? 5000,
    memoryLimitKb: codingResult.value.memoryLimitKb ?? 262144,
    templates: codingResult.value.templates || [],
    testCases: codingResult.value.testCases || []
  }
  if (!payload.title) {
    message.warning('题目标题不能为空')
    return
  }
  const hiddenCases = payload.testCases.filter((tc: any) => tc.isSample === 0 || tc.isSample === false)
  if (hiddenCases.length === 0) {
    message.warning('请至少添加一个隐藏测试用例（非样例）')
    return
  }
  try {
    await request.post('/coding/problem/add', payload)
    message.success('已保存到编程题库')
  } catch (error: any) {
    console.error('保存到编程题库失败:', error)
    message.error(error?.message || '保存失败')
  }
}

const saveCodingToCloud = async () => {
  if (!codingResult.value) {
    message.warning('没有可保存的内容')
    return
  }
  const title = codingResult.value.title || 'AI生成编程题'
  const content = codingResult.value.description || ''
  const paramsObj = {
    ...codingResult.value,
    agentCitations: agentCitations.value,
    agentQualityReport: agentQualityReport.value
  }
  const payload = {
    id: currentCodingId.value ?? undefined,
    type: 'coding',
    title,
    content,
    agentRequestId: agentWorkflowRunIds.value.coding,
    paramsJson: JSON.stringify(paramsObj)
  }
  try {
    if (currentCodingId.value) {
      await request.post<any, boolean>('/ai/resource/update', payload)
      message.success('修改已保存')
    } else {
      const newId = await request.post<any, number>('/ai/resource/save', payload)
      currentCodingId.value = Number(newId)
      message.success('已保存到云端资源库')
    }
  } catch (error: any) {
    console.error('保存资源失败:', error)
    message.error(error?.message || '保存失败')
  }
}



</script>

<style scoped>
.chat-workspace :deep(.course-graph-context-card) { margin: 16px 20px 0; }

.micro-video-container {
  height: calc(100% - 0px);
  padding: 0;
  overflow: hidden;
}

.ai-console-wrapper {
  display: flex;
  height: 85vh;
  background: #ffffff;
  border-radius: 5px;
  overflow: hidden;
  box-shadow: 0 10px 40px -10px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
  font-family: 'Plus Jakarta Sans', sans-serif;
}

.engine-sidebar {
  width: 280px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
  padding: 0 8px;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.pulse-ring {
  width: 12px;
  height: 12px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4);
  animation: pulsing 2s infinite cubic-bezier(0.66, 0, 0, 1);
}

.sub-title {
  font-size: 12px;
  color: #64748b;
  padding: 0 8px;
  margin-bottom: 24px;
  font-weight: 500;
}

.agent-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.agent-list::-webkit-scrollbar {
  width: 0;
}

.agent-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  background: transparent;
  position: relative;
}

.agent-item:hover {
  background: #f1f5f9;
}

.agent-item.active {
  background: #fff;
  border-color: #e2e8f0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}

.agent-icon {
  width: 40px;
  height: 40px;
  background: #f1f5f9;
  border-radius: 5px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 20px;
  color: #64748b;
}

.agent-item.active .agent-icon {
  background: #eff6ff;
  color: #3b82f6;
}

.agent-info {
  flex: 1;
}

.agent-info .name {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.agent-info .desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
  line-height: 1.3;
}

.status-indicator {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
}

.dot.live {
  width: 6px;
  height: 6px;
  background: #3b82f6;
  border-radius: 50%;
  display: inline-block;
  box-shadow: 0 0 8px #3b82f6;
}

.chat-workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  position: relative;
  min-width: 0;
}

.workspace-header {
  height: 64px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 0 32px;
  flex-shrink: 0;
}

.current-engine {
  display: flex;
  align-items: center;
  gap: 12px;
}

.engine-icon {
  font-size: 20px;
  color: #4f46e5;
}

.engine-name {
  font-weight: 700;
  font-size: 16px;
  color: #0f172a;
}

.engine-status {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 5px;
  background: #f0fdf4;
  color: #16a34a;
  transition: 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.engine-status.busy {
  background: #fff7ed;
  color: #ea580c;
}

.ghost-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ghost-btn:hover {
  color: #ef4444;
}

.ppt-iframe-container {
  flex: 1;
  width: 100%;
  height: 100%;
  background: #f1f5f9;
}

.tool-generator-container {
  flex: 1;
  background: #f8fafc;
  padding: 24px;
  overflow: hidden;
}

.tool-layout {
  display: flex;
  gap: 24px;
  height: 100%;
}

.scroll-y {
  overflow-y: auto;
  scroll-behavior: smooth;
}

.scroll-y::-webkit-scrollbar {
  width: 6px;
}

.scroll-y::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 5px;
}

.tool-config-panel {
  width: 420px;
  background: #fff;
  border-radius: 5px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.tool-result-panel {
  flex: 1;
  background: #fff;
  border-radius: 5px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  border: 1px solid #e2e8f0;
  position: relative;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-row-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

/* --- 替换原有的 custom-checkbox-group 相关样式 --- */

.custom-checkbox-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 强制严格的双列等宽网格 */
  gap: 12px;
  width: 100%;
}

:deep(.custom-checkbox-group .ant-checkbox-wrapper) {
  margin: 0;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 8px 14px; /* 略微增加内边距，提升点击区域的舒适度 */
  border-radius: 6px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  width: 100%; /* 必须设置为撑满网格单元格 */
  box-sizing: border-box;
}

/* 选中状态 */
:deep(.custom-checkbox-group .ant-checkbox-wrapper-checked) {
  background: #eff6ff;
  border-color: #3b82f6;
}

/* 针对智能出题模块的橙色主题适配 */
:deep(.custom-checkbox-group.orange-theme .ant-checkbox-wrapper-checked) {
  background: #fff7ed;
  border-color: #f59e0b;
}

/* 内部文本对齐控制：让选框和文字保持固定间距，并强制文字左对齐 */
:deep(.custom-checkbox-group .ant-checkbox-wrapper span:last-child) {
  flex: 1;
  text-align: left;
  padding-left: 12px; /* 将这里的间距调大，拉开文字与方框的距离 */
  color: #475569;
  font-weight: 500;
}

:deep(.custom-checkbox-group .ant-checkbox-wrapper-checked span:last-child) {
  color: #2563eb;
}

:deep(.custom-checkbox-group.orange-theme .ant-checkbox-wrapper-checked span:last-child) {
  color: #d97706;
}

.form-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 32px;
  border-top: 1px solid #f1f5f9;
  padding-top: 24px;
}

.tool-submit-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start; /* 新增这行：让按钮组整体靠右对齐 */
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px dashed #e2e8f0;
}

.case-hint {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.case-picker-trigger {
  width: 100%;
  height: 46px;
  justify-content: center;
  border-style: dashed;
  border-color: #93c5fd;
  color: #2563eb;
  font-weight: 700;
}

.platform-case-recommend {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  background: #fbfdff;
}

.recommend-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 10px;
}

.recommend-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.selected-case-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.selected-case-chip {
  min-height: 36px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  border-radius: 8px;
  padding: 6px 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.selected-case-chip span {
  flex: 1;
  min-width: 0;
  color: #1e3a8a;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.recommend-card {
  width: 100%;
  text-align: left;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.recommend-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.08);
}

.recommend-card.selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.recommend-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.recommend-card-top strong {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.35;
}

.recommend-card p {
  margin: 8px 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recommend-evidence {
  margin: 8px 0;
  padding: 8px;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
}

.recommend-evidence span {
  display: block;
  margin-bottom: 4px;
  color: #2563eb;
  font-weight: 700;
}

.recommend-evidence em {
  display: -webkit-box;
  color: #334155;
  font-style: normal;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recommend-evidence.weak span {
  margin-bottom: 0;
  color: #b45309;
}

.recommend-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  color: #64748b;
  font-size: 12px;
}

.recommend-meta span {
  padding: 2px 7px;
  border-radius: 6px;
  background: #f1f5f9;
}

.selected-case-panel {
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.selected-case-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.selected-case-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
  font-weight: 700;
}

.selected-case-title span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-case-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
  flex-wrap: wrap;
}

.selected-case-actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .selected-case-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .selected-case-actions {
    justify-content: flex-start;
  }
}

/* 注意：务必删掉或注释掉之前那两段对具体的按钮设置 flex 的代码 */



.generate-btn {
  font-weight: 700;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 5px;
}

.generate-btn.plan-btn {
  background: linear-gradient(135deg, #3b82f6, #6366f1);
}

.generate-btn.quiz-btn {
  background: linear-gradient(135deg, #f59e0b, #f97316);
}
.tool-form .config-group:has(.orange-theme) ~ .config-group .group-title::before,
.tool-form .config-group:has(input[v-model*="quizForm"]) .group-title::before,
.tool-generator-container:nth-child(3) .group-title::before {
  background: #f59e0b;
}

.generate-btn.anim-btn {
  /* 修改为与教案按钮一致的蓝色渐变 */
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: white;
}

.empty-result {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #94a3b8;
  text-align: center;
}

.large-empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
  color: #cbd5e1;
}

.result-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  gap: 12px;
  flex-wrap: wrap;
}

.status-tag {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 6px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tool-btn {
  color: #64748b;
  font-weight: 600;
}

.tool-btn:hover {
  color: #3b82f6;
  background: #f8fafc;
  border-radius: 5px;
}

.plan-meta-shell {
  margin: 8px 0 16px;
}

.plan-meta-toggle {
  width: 100%;
  min-height: 38px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #f8fbff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.plan-meta-toggle:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.plan-meta-toggle:focus-visible {
  outline: 2px solid #93c5fd;
  outline-offset: 2px;
}

.plan-meta-toggle span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.plan-meta-toggle em {
  min-width: 0;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.markdown-render {
  color: #334155;
  line-height: 1.9;
  font-size: 15px;
}

:deep(.markdown-render h1),
:deep(.markdown-render h2),
:deep(.markdown-render h3),
:deep(.markdown-render h4) {
  color: #0f172a;
  font-weight: 800;
  margin: 1.1em 0 0.6em;
}

:deep(.markdown-render p) {
  margin: 0.8em 0;
}

:deep(.markdown-render img) {
  display: block;
  max-width: 100%;
  max-height: 420px;
  width: auto;
  height: auto;
  margin: 14px auto;
  border-radius: 8px;
  object-fit: contain;
}

:deep(.markdown-render ul),
:deep(.markdown-render ol) {
  padding-left: 1.4em;
}

:deep(.markdown-render blockquote) {
  margin: 1em 0;
  padding: 0.8em 1em;
  background: #f8fafc;
  border-left: 4px solid #94a3b8;
  border-radius: 5px;
}

:deep(.markdown-render code) {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 5px;
}

:deep(.markdown-render .case-badge) {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 700;
  color: #b45309;
  background: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 4px;
  margin-right: 6px;
  vertical-align: middle;
  line-height: 1.6;
}

.editor-panel {
  margin-top: 12px;
}

:deep(.plan-editor) {
  font-size: 15px;
  line-height: 1.8;
  border-radius: 5px;
}

:deep(.plan-editor textarea) {
  min-height: 720px !important;
  line-height: 1.8;
  padding: 18px 20px;
  background: #fcfdff;
  font-family: inherit;
  border-radius: 5px;
}

:deep(.plan-editor .w-e-toolbar) {
  background: #f8fafc;
}

:deep(.plan-editor .w-e-text-container) {
  background: #fcfdff;
}

:deep(.plan-editor .w-e-scroll) {
  padding: 18px 20px;
}

:deep(.plan-editor .w-e-text-placeholder) {
  top: 18px;
  left: 20px;
  color: #64748b;
}

:deep(.plan-editor [data-slate-editor]) {
  color: #334155;
  font-size: 15px;
  line-height: 1.9;
}

.skeleton-loader {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sk-line {
  height: 16px;
  background: #f1f5f9;
  border-radius: 5px;
  animation: pulse 1.5s infinite;
}

.sk-line.title {
  height: 24px;
  width: 40%;
}

.sk-line.short {
  width: 60%;
}

.anim-theme .panel-title {
  color: #3b82f6;
}

.anim-tip {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 24px;
  line-height: 1.6;
  padding: 12px;
  background: #fff1f2;
  border-radius: 5px;
  border: 1px solid #ffe4e6;
}

.anim-result-panel {
  flex: 1;
  background: #fff;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 0 !important;
}

.anim-workspace {
  flex: 1;
  color: #3b82f6;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.coding-state {
  flex: 1;
  background: #0f172a;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fb7185;
  font-family: monospace;
}

.radar-spinner {
  width: 60px;
  height: 60px;
  border: 4px solid rgba(59, 130, 246, 0.2);
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s infinite linear;
  margin-bottom: 20px;
}

.player-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.player-header {
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.p-title {
  font-weight: 700;
  color: #0f172a;
}

.video-wrapper {
  flex: 1;
  min-height: 0;
}

.render-iframe {
  width: 100%;
  height: 100%;
  background: white;
}

.chat-mode-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.chat-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 40px 10%;
  display: flex;
  flex-direction: column;
  scroll-behavior: smooth;
}

.empty-hero {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #64748b;
  text-align: center;
}

.hero-icon {
  font-size: 56px;
  margin-bottom: 24px;
  color: #cbd5e1;
}

.empty-hero h2 {
  color: #1e293b;
  font-weight: 800;
  font-size: 24px;
  margin-bottom: 8px;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 40px;
  max-width: 600px;
  width: 100%;
}

.suggestion-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 16px 20px;
  border-radius: 5px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.suggestion-card:hover {
  border-color: #cbd5e1;
  background: #fff;
  transform: translateY(-2px);
}

.suggestion-card .arrow {
  opacity: 0;
  transition: 0.2s;
  color: #3b82f6;
}

.suggestion-card:hover .arrow {
  opacity: 1;
  transform: translateX(4px);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding-bottom: 80px;
}

.message-wrapper {
  display: flex;
  gap: 20px;
  max-width: 85%;
}

.message-wrapper.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-wrapper.ai {
  align-self: flex-start;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.user .avatar {
  background: #4f46e5;
  color: #fff;
}

.ai .avatar {
  background: #f1f5f9;
  color: #475569;
}

.message-content {
  font-size: 15px;
  line-height: 1.7;
  color: #1e293b;
  width: 100%;
}

.user-text {
  background: #f1f5f9;
  padding: 12px 20px;
  border-radius: 5px;
  display: inline-block;
  word-break: break-word;
}

.loading-bubble {
  background: #f8fafc;
  padding: 12px 20px;
  border-radius: 5px;
  border: 1px solid #e2e8f0;
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 48px;
}

.loading-bubble .dot {
  width: 6px;
  height: 6px;
  background: #94a3b8;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-bubble .dot:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-bubble .dot:nth-child(2) {
  animation-delay: -0.16s;
}

.floating-input-zone {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 0 10% 32px;
  background: linear-gradient(to top, #fff 60%, transparent);
}

.input-container {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  padding: 8px 12px 8px 24px;
  display: flex;
  align-items: flex-end;
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.1);
  transition: 0.3s;
}

.input-container.focused {
  border-color: #cbd5e1;
  transform: translateY(-2px);
}

.input-container.disabled {
  background: #f8fafc;
  opacity: 0.8;
}

.input-container textarea {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 12px 0;
  font-size: 15px;
  color: #1e293b;
  resize: none;
  max-height: 120px;
  font-family: inherit;
  line-height: 1.5;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 5px;
  border: none;
  background: #f1f5f9;
  color: #94a3b8;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 18px;
  transition: 0.2s;
  cursor: not-allowed;
  margin-bottom: 2px;
}

.send-btn.active {
  background: #3b82f6;
  color: #fff;
  cursor: pointer;
}

@keyframes pulsing {
  to {
    box-shadow: 0 0 0 10px rgba(16, 185, 129, 0);
  }
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

@keyframes pulse {
  0% {
    opacity: 0.55;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.55;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.quiz-workbench-hint {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 5px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #9a3412;
  font-size: 13px;
  line-height: 1.6;
}

.quiz-optimize-panel {
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 5px;
  background: linear-gradient(180deg, #fffaf5 0%, #fff7ed 100%);
  border: 1px solid #fed7aa;
}

.quiz-optimize-title {
  font-size: 13px;
  font-weight: 700;
  color: #9a3412;
  margin-bottom: 12px;
}

.quiz-optimize-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.anim-hint-box {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 5px;
  background: #fff1f2;
  border: 1px solid #fecdd3;
  color: #9f1239;
  font-size: 13px;
  line-height: 1.6;
}

.anim-optimize-panel {
  padding: 14px 16px 0;
}

.anim-optimize-title {
  font-size: 13px;
  font-weight: 700;
  color: #be123c;
  margin-bottom: 10px;
}

.anim-optimize-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

:deep(.anim-optimize-btn) {
  border-radius: 5px;
  border-color: #fda4af;
  color: #be123c;
  background: #fff;
  font-weight: 600;
}

:deep(.anim-optimize-btn:hover) {
  border-color: #fb7185;
  color: #e11d48;
  background: #fff1f2;
}

.anim-workspace {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.preview-shell {
  flex: 1;
  min-height: 0;
  padding: 18px;
  overflow: auto;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.preview-stage {
  width: min(100%, 1180px);
  margin: 0 auto;
  height: clamp(420px, 68vh, 760px);
}

.preview-card {
  width: 100%;
  height: 100%;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  overflow: hidden;
  box-shadow: 0 18px 50px -24px rgba(15, 23, 42, 0.25);
}

.video-wrapper {
  width: 100%;
  height: 100%;
  background: #ffffff;
}

.render-iframe {
  width: 100%;
  height: 100%;
  display: block;
  background: #ffffff;
}

.anim-result-panel {
  overflow: hidden;
}

:deep(.quiz-optimize-btn) {
  border-radius: 5px;
  border-color: #fdba74;
  color: #c2410c;
  background: #ffffff;
  font-weight: 600;
}

:deep(.quiz-optimize-btn:hover) {
  color: #ea580c;
  border-color: #fb923c;
  background: #fff7ed;
}

:deep(.quiz-editor textarea) {
  min-height: 780px !important;
  border-radius: 5px;
}

.anim-validation-box {
  margin: 14px 18px 0;
  padding: 14px 16px;
  border-radius: 5px;
  border: 1px solid #fbcfe8;
  background: #fff1f2;
}

.anim-validation-title {
  font-size: 13px;
  font-weight: 700;
  color: #be123c;
  margin-bottom: 8px;
}

.anim-validation-box ul {
  margin: 0;
  padding-left: 18px;
  color: #9f1239;
  line-height: 1.7;
}

.anim-json-card {
  display: flex;
  flex-direction: column;
}

.anim-json-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.anim-json-header h3 {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
}

.anim-json-header p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.anim-stage {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 24px;
  background:
    radial-gradient(circle at top left, rgba(99, 102, 241, 0.12), transparent 35%),
    linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.sort-stage {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 14px;
}

.sort-block {
  width: 68px;
  height: 68px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #ffffff 0%, #eef2ff 100%);
  border: 1px solid #cbd5e1;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
  box-shadow: 0 10px 24px -18px rgba(15, 23, 42, 0.45);
  transition: all 0.25s ease;
}

.sort-block.active {
  transform: translateY(-4px) scale(1.03);
  border-color: #f59e0b;
  box-shadow: 0 18px 30px -18px rgba(245, 158, 11, 0.7);
}

.sort-block.swapped {
  border-color: #ef4444;
  background: linear-gradient(180deg, #fff1f2 0%, #ffe4e6 100%);
}

.sort-block.sorted {
  border-color: #22c55e;
  background: linear-gradient(180deg, #f0fdf4 0%, #dcfce7 100%);
  color: #166534;
}

.protocol-stage {
  width: 100%;
}

.protocol-actors {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 1.2fr) minmax(180px, 1fr);
  gap: 18px;
  align-items: center;
}

.actor-card {
  min-height: 124px;
  padding: 18px;
  border-radius: 5px;
  background: #ffffff;
  border: 1px solid #dbeafe;
  box-shadow: 0 18px 38px -28px rgba(59, 130, 246, 0.5);
}

.actor-name {
  font-size: 18px;
  font-weight: 800;
  color: #1e3a8a;
  margin-bottom: 10px;
}

.actor-state {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 5px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.protocol-arrow-zone {
  display: flex;
  align-items: center;
  justify-content: center;
}

.protocol-message {
  width: 100%;
  padding: 16px 18px;
  border-radius: 5px;
  text-align: center;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  box-shadow: 0 18px 38px -28px rgba(15, 23, 42, 0.35);
}

.protocol-message.request {
  border-color: #93c5fd;
  background: linear-gradient(180deg, #eff6ff 0%, #dbeafe 100%);
}

.protocol-message.response {
  border-color: #fdba74;
  background: linear-gradient(180deg, #fff7ed 0%, #ffedd5 100%);
}

.protocol-message.confirm {
  border-color: #86efac;
  background: linear-gradient(180deg, #f0fdf4 0%, #dcfce7 100%);
}

.protocol-message.close {
  border-color: #fda4af;
  background: linear-gradient(180deg, #fff1f2 0%, #ffe4e6 100%);
}

.protocol-message .from,
.protocol-message .to {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.protocol-message .arrow {
  display: inline-block;
  margin: 0 10px;
  font-size: 18px;
  font-weight: 900;
  color: #475569;
}

.message-text {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.5;
  word-break: break-word;
}

.anim-step-panel {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
}

.step-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.step-title-row strong {
  font-size: 16px;
  color: #0f172a;
}

.step-title-row span {
  flex-shrink: 0;
  font-size: 13px;
  color: #64748b;
}

.step-desc-row {
  font-size: 14px;
  line-height: 1.8;
  color: #334155;
}

.anim-control-bar {
  display: flex;
  justify-content: center;
  gap: 10px;
  padding: 14px 20px 18px;
  border-top: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.anim-json-card.is-stable .anim-stage {
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
}

.anim-json-card.is-stable .sort-block,
.anim-json-card.is-stable .protocol-message,
.anim-json-card.is-stable .actor-card {
  box-shadow: none;
  border-width: 1px;
}

.anim-json-card.is-vivid .sort-block.active {
  transform: translateY(-6px) scale(1.06);
  box-shadow: 0 24px 34px -18px rgba(245, 158, 11, 0.8);
}

.anim-json-card.is-vivid .protocol-message {
  box-shadow: 0 24px 44px -28px rgba(15, 23, 42, 0.45);
}

.anim-json-card.is-slow .step-desc-row {
  line-height: 1.95;
}

@media (max-width: 980px) {
  .protocol-actors {
    grid-template-columns: 1fr;
  }

  .sort-block {
    width: 58px;
    height: 58px;
    font-size: 18px;
  }

  .anim-json-header {
    flex-direction: column;
  }

  .step-title-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .anim-control-bar {
    flex-wrap: wrap;
  }
}

.stack-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 28px;
  width: 100%;
  min-height: 280px;
  padding: 12px 8px;
}

.stack-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.stack-top-badge {
  padding: 4px 12px;
  border-radius: 5px;
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.stack-column {
  width: 140px;
  min-height: 220px;
  padding: 14px 12px;
  border-radius: 5px;
  border: 2px solid rgba(148, 163, 184, 0.32);
  background: linear-gradient(180deg, #f8fbff 0%, #eef4ff 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 10px;
  box-shadow: inset 0 -12px 24px rgba(59, 130, 246, 0.06);
}

.stack-item {
  height: 42px;
  border-radius: 5px;
  background: linear-gradient(135deg, #ffffff 0%, #dbeafe 100%);
  border: 1px solid rgba(96, 165, 250, 0.3);
  color: #1e3a8a;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.28s ease;
}

.stack-item.active,
.stack-item.peeking {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.18);
  border-color: rgba(59, 130, 246, 0.65);
}

.stack-item.popping {
  border-color: rgba(244, 63, 94, 0.5);
  background: linear-gradient(135deg, #fff1f2 0%, #ffe4e6 100%);
  color: #be123c;
}

.stack-empty {
  height: 42px;
  border-radius: 5px;
  border: 1px dashed rgba(148, 163, 184, 0.45);
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.stack-side-panel {
  min-width: 220px;
  max-width: 260px;
  padding: 18px 18px 16px;
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.stack-op-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 7px 14px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 14px;
}

.stack-op-chip.init,
.stack-op-chip.done {
  background: rgba(100, 116, 139, 0.12);
  color: #475569;
}

.stack-op-chip.push {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.stack-op-chip.pop {
  background: rgba(244, 63, 94, 0.14);
  color: #be123c;
}

.stack-op-chip.peek {
  background: rgba(59, 130, 246, 0.14);
  color: #2563eb;
}

.stack-side-text {
  display: grid;
  gap: 10px;
  color: #475569;
  font-size: 14px;
  line-height: 1.65;
}

:deep(.ant-btn),
:deep(.ant-input),
:deep(.ant-select-selector) {
  border-radius: 5px !important;
}

/* 题型与数量矩阵 */
.quiz-type-count-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}

.quiz-type-count-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 14px;
  min-height: 48px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.quiz-type-count-item.is-active {
  background: #eff6ff;
  border-color: #3b82f6;
}

.quiz-type-count-item .type-label {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quiz-type-count-item.is-active .type-label {
  color: #2563eb;
}

.quiz-type-count-item .count-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.quiz-type-count-item :deep(.ant-input-number) {
  width: 50px !important;
  border-radius: 5px !important;
}

.quiz-type-count-item :deep(.ant-input-number-handler-wrap) {
  width: 16px !important; /* 原默认值通常在 22px 左右，将其调窄 */
}

.quiz-type-count-item :deep(.ant-input-number-input) {
  height: 30px;
  text-align: center;
  font-weight: 600;
  color: #334155;
  padding-left: 0 !important; /* 清除默认左边距 */
  padding-right: 16px !important; /* 留出与箭头区等宽的安全距离，防止数字被遮挡 */
}

.quiz-type-count-item.is-active :deep(.ant-input-number-input) {
  color: #2563eb;
}

.quiz-type-count-item .unit {
  flex-shrink: 0;
  font-size: 13px;
  color: #64748b;
}

.quiz-type-count-item.is-active .unit {
  color: #2563eb;
}

.quiz-total-summary {
  margin-top: 14px;
  font-size: 13px;
  color: #475569;
}

.quiz-total-summary strong {
  margin: 0 4px;
  color: #2563eb;
  font-size: 15px;
  font-weight: 700;
}

.quiz-total-summary .warn {
  margin-left: 10px;
  color: #f59e0b;
}

.agent-meta-panel {
  margin: 12px 0 16px;
  padding: 12px 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.agent-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.agent-stage-chip {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 12px;
  font-weight: 600;
}

.agent-stage-chip.done {
  background: #dcfce7;
  color: #166534;
}

.agent-meta-small {
  margin-top: 8px;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
}

.agent-citation-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.citation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #1e293b;
  font-size: 12px;
  font-weight: 800;
}

.citation-head button,
.citation-item button {
  border: 0;
  background: transparent;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.citation-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 12px;
  padding: 10px 12px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #ffffff;
}

.citation-item.excluded {
  opacity: 0.56;
  background: #f8fafc;
}

.citation-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.citation-main strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
  font-size: 12px;
}

.citation-evidence,
.citation-type,
.citation-reason,
.citation-score {
  flex-shrink: 0;
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}

.citation-evidence {
  background: #111827;
  color: #ffffff;
  border-radius: 4px;
}

.citation-type {
  background: #e0f2fe;
  color: #0369a1;
}

.citation-reason {
  background: #f1f5f9;
  color: #475569;
}

.citation-score {
  background: #dcfce7;
  color: #166534;
}

.citation-item p {
  grid-column: 1 / -1;
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
}

.plan-meta-shell .agent-meta-panel {
  margin: 8px 0 0;
  padding: 14px;
  border-color: #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.plan-meta-shell .agent-citation-list {
  margin-top: 0;
  gap: 10px;
}

.plan-meta-shell .citation-head {
  height: 30px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.plan-meta-shell .citation-head button {
  height: 26px;
  padding: 0 10px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #f8fbff;
  color: #2563eb;
}

.plan-meta-shell .citation-head button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.plan-meta-shell .citation-item {
  position: relative;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px 12px;
  padding: 12px 14px;
  border-color: #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.plan-meta-shell .citation-item:hover {
  border-color: #bfdbfe;
  background: #fbfdff;
}

.plan-meta-shell .citation-item.excluded {
  opacity: 1;
  border-color: #e5e7eb;
  background: #f8fafc;
}

.plan-meta-shell .citation-main {
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  padding-right: 4px;
}

.plan-meta-shell .citation-main strong {
  flex: 1 1 180px;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.45;
  white-space: nowrap;
}

.plan-meta-shell .citation-evidence,
.plan-meta-shell .citation-type,
.plan-meta-shell .citation-reason,
.plan-meta-shell .citation-score {
  height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  font-size: 11px;
  font-weight: 800;
}

.plan-meta-shell .citation-evidence {
  min-width: 36px;
  justify-content: center;
  border-radius: 5px;
  background: #111827;
}

.plan-meta-shell .citation-type {
  background: #e0f2fe;
  color: #0369a1;
}

.plan-meta-shell .citation-reason {
  background: #f1f5f9;
  color: #475569;
}

.plan-meta-shell .citation-score {
  background: #dcfce7;
  color: #166534;
}

.plan-meta-shell .citation-item p {
  grid-column: 1 / 2;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.plan-meta-shell .citation-item > button {
  grid-column: 2;
  grid-row: 1 / span 2;
  align-self: center;
  height: 28px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #ffffff;
  color: #475569;
}

.plan-meta-shell .citation-item > button:hover {
  border-color: #93c5fd;
  color: #2563eb;
  background: #eff6ff;
}

.plan-meta-shell .agent-meta-small {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
}

@media (max-width: 720px) {
  .plan-meta-shell .citation-item {
    grid-template-columns: 1fr;
  }

  .plan-meta-shell .citation-item p,
  .plan-meta-shell .citation-item > button {
    grid-column: 1;
  }

  .plan-meta-shell .citation-item > button {
    grid-row: auto;
    justify-self: flex-start;
  }
}

.anim-agent-meta {
  margin: 12px 18px 0;
  flex-shrink: 0;
}

.coding-editor-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.coding-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  padding: 16px;
}

.section-label {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 12px;
}

.template-block {
  margin-bottom: 16px;
}

.tpl-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
}

.code-editor :deep(textarea) {
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.5;
  background: #0f172a;
  color: #e2e8f0;
}

@media (max-width: 720px) {
  .quiz-type-count-grid {
    grid-template-columns: 1fr;
  }
}
</style>
