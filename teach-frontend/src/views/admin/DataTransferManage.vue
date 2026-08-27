<template>
  <div class="admin-ops-page data-transfer-page">
    <section class="transfer-hero">
      <div>
        <h2>导入导出中心</h2>
        <p>统一处理学生账号、班级专业、模板下载、错误报告和数据库备份恢复。所有关键操作都会写入审计日志。</p>
      </div>
      <div class="hero-actions">
        <a-button @click="download('/admin/data-transfer/template/students')">
          <template #icon><FileExcelOutlined /></template>
          学生模板
        </a-button>
        <a-button @click="download('/admin/data-transfer/template/classes')">
          <template #icon><FileExcelOutlined /></template>
          班级模板
        </a-button>
      </div>
    </section>

    <section class="transfer-layout">
      <div class="transfer-main">
        <div class="section-title">
          <div>
            <h3>数据导入导出</h3>
            <p>建议先下载模板填写，先导入班级专业，再导入学生账号。</p>
          </div>
        </div>

        <div class="transfer-grid">
          <article class="transfer-card primary-card">
            <div class="card-icon upload"><CloudUploadOutlined /></div>
            <div class="card-content">
              <div class="card-title">
                <strong>导入学生账号</strong>
                <a-tag color="blue">账号=学号</a-tag>
              </div>
              <p>字段顺序：学号、姓名、专业、班级、学院。默认密码为学号后 8 位，姓名会写入昵称。</p>
              <div class="card-actions">
                <a-upload accept=".xlsx,.xls" :show-upload-list="false" :before-upload="beforeUploadStudents" :disabled="uploading">
                  <a-button type="primary" :loading="uploading && activeTask === 'students'">
                    <template #icon><UploadOutlined /></template>
                    上传学生 Excel
                  </a-button>
                </a-upload>
                <a-button type="link" @click="download('/admin/data-transfer/template/students')">下载模板</a-button>
              </div>
            </div>
          </article>

          <article class="transfer-card">
            <div class="card-icon class-card"><ApartmentOutlined /></div>
            <div class="card-content">
              <div class="card-title">
                <strong>导入班级专业</strong>
                <a-tag>基础数据</a-tag>
              </div>
              <p>字段顺序：班级名称、专业、学院。班级名称已存在时会跳过，不覆盖原数据。</p>
              <div class="card-actions">
                <a-upload accept=".xlsx,.xls" :show-upload-list="false" :before-upload="beforeUploadClasses" :disabled="uploading">
                  <a-button :loading="uploading && activeTask === 'classes'">
                    <template #icon><UploadOutlined /></template>
                    上传班级 Excel
                  </a-button>
                </a-upload>
                <a-button type="link" @click="download('/admin/data-transfer/template/classes')">下载模板</a-button>
              </div>
            </div>
          </article>

          <article class="transfer-card compact-card">
            <div class="card-icon export"><DownloadOutlined /></div>
            <div class="card-content">
              <div class="card-title"><strong>导出用户账号</strong></div>
              <p>导出学生、教师、管理员账号基础信息，便于核对角色、班级、教师职称和注册号。</p>
              <div class="card-actions">
                <a-button @click="download('/admin/data-transfer/export/users')">
                  <template #icon><DownloadOutlined /></template>
                  导出用户
                </a-button>
              </div>
            </div>
          </article>

          <article class="transfer-card compact-card">
            <div class="card-icon export"><FileDoneOutlined /></div>
            <div class="card-content">
              <div class="card-title"><strong>导出班级专业</strong></div>
              <p>导出班级、专业、学院和学生数量，用于导入后复核基础数据。</p>
              <div class="card-actions">
                <a-button @click="download('/admin/data-transfer/export/classes')">
                  <template #icon><DownloadOutlined /></template>
                  导出班级
                </a-button>
              </div>
            </div>
          </article>
        </div>
      </div>

      <aside class="transfer-side">
        <section class="side-card">
          <div class="side-head">
            <FileSearchOutlined />
            <strong>最近导入结果</strong>
          </div>

          <template v-if="lastResult">
            <div class="result-stats">
              <div><span>新增</span><strong>{{ lastResult.created }}</strong></div>
              <div><span>跳过</span><strong>{{ lastResult.skipped }}</strong></div>
              <div><span>异常</span><strong>{{ lastResult.errors?.length || 0 }}</strong></div>
            </div>

            <a-alert v-if="lastResult.errors?.length" type="warning" show-icon class="result-alert">
              <template #message>部分行未导入</template>
              <template #description>
                <div class="error-list">
                  <div v-for="item in lastResult.errors.slice(0, 8)" :key="item">{{ item }}</div>
                </div>
                <a-button size="small" class="error-download" @click="downloadErrorReport">
                  <template #icon><DownloadOutlined /></template>
                  下载错误报告
                </a-button>
              </template>
            </a-alert>
          </template>

          <p v-else class="empty-result">完成一次导入后，这里会显示新增、跳过和异常行。</p>
        </section>

        <section class="side-card">
          <div class="side-head">
            <SafetyCertificateOutlined />
            <strong>导入前检查</strong>
          </div>
          <ol class="check-list">
            <li>优先使用模板文件，避免列顺序错误。</li>
            <li>学号、班级名称不要带空格。</li>
            <li>大批量导入前，先创建一次数据库备份。</li>
          </ol>
        </section>
      </aside>
    </section>

    <section class="import-history-panel">
      <div class="section-title backup-title">
        <div>
          <h3>导入批次记录</h3>
          <p>每次导入都会保存批次、文件名、结果统计、操作者和来源 IP，异常批次可下载错误报告。</p>
        </div>
        <div class="history-actions">
          <a-select
            v-model:value="importBatchQuery.importType"
            class="history-filter"
            :options="importTypeOptions"
            @change="handleImportTypeChange"
          />
          <a-button :loading="importBatchLoading" @click="loadImportBatches">
            <template #icon><ReloadOutlined /></template>
            刷新记录
          </a-button>
        </div>
      </div>

      <a-table
        row-key="id"
        size="middle"
        :columns="importBatchColumns"
        :data-source="importBatchList"
        :loading="importBatchLoading"
        :pagination="{
          current: importBatchQuery.current,
          pageSize: importBatchQuery.size,
          total: importBatchTotal,
          showSizeChanger: false
        }"
        :scroll="{ x: 920 }"
        @change="handleImportBatchPageChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'importType'">
            <a-tag color="blue">{{ importTypeText(record.importType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'result'">
            <div class="batch-result-cell">
              <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              <span>新增 {{ record.createdCount }} / 跳过 {{ record.skippedCount }} / 异常 {{ record.errorCount }}</span>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'adminName'">
            <strong>{{ record.adminName || record.adminAccount || '-' }}</strong>
            <div class="muted">{{ record.requestIp || '-' }}</div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" :disabled="!record.errorCount" @click="downloadBatchErrorReport(record.id)">
              错误报告
            </a-button>
          </template>
        </template>
      </a-table>
    </section>

    <section class="backup-panel">
      <div class="section-title backup-title">
        <div>
          <h3>数据库备份与恢复</h3>
          <p>
            备份文件保存在后端服务器：
            <span class="path-text">{{ backupStatus?.backupDir || '-' }}</span>
          </p>
        </div>
        <div class="backup-actions">
          <a-button @click="loadBackupData">
            <template #icon><ReloadOutlined /></template>
            刷新状态
          </a-button>
          <a-button type="primary" :loading="backupLoading" @click="handleCreateBackup">
            <template #icon><CloudServerOutlined /></template>
            创建备份
          </a-button>
        </div>
      </div>

      <div class="backup-summary">
        <div>
          <span>备份数量</span>
          <strong>{{ backupStatus?.backupCount || 0 }}</strong>
        </div>
        <div>
          <span>最近备份</span>
          <strong>{{ backupStatus?.latestBackup?.createTime || '暂无备份' }}</strong>
        </div>
        <div>
          <span>恢复能力</span>
          <strong>{{ backupStatus?.restoreEnabled ? '已启用' : '未启用' }}</strong>
        </div>
      </div>

      <a-table
        row-key="filename"
        size="middle"
        :columns="backupColumns"
        :data-source="backupList"
        :loading="backupLoading"
        :pagination="{ pageSize: 5, hideOnSinglePage: true }"
        :scroll="{ x: 760 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" @click="downloadBackup(record.filename)">下载</a-button>
            <a-button type="link" danger @click="confirmRestore(record.filename)">恢复</a-button>
          </template>
        </template>
      </a-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  ApartmentOutlined,
  CloudServerOutlined,
  CloudUploadOutlined,
  DatabaseOutlined,
  DownloadOutlined,
  FileDoneOutlined,
  FileExcelOutlined,
  FileSearchOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import {
  createAdminBackup,
  getAdminBackupList,
  getAdminBackupStatus,
  getAdminImportBatchList,
  importAdminClasses,
  importAdminStudents,
  restoreAdminBackup
} from '@/api/admin'
import type {
  AdminBackupFile,
  AdminBackupStatus,
  AdminImportBatchItem,
  AdminImportBatchParams,
  AdminImportResult
} from '@/types/admin'
import './AdminOps.css'

type ImportTask = 'students' | 'classes'

const lastResult = ref<AdminImportResult | null>(null)
const uploading = ref(false)
const activeTask = ref<ImportTask | ''>('')
const backupLoading = ref(false)
const backupStatus = ref<AdminBackupStatus | null>(null)
const backupList = ref<AdminBackupFile[]>([])
const importBatchLoading = ref(false)
const importBatchList = ref<AdminImportBatchItem[]>([])
const importBatchTotal = ref(0)
const importBatchQuery = ref<AdminImportBatchParams>({
  current: 1,
  size: 6,
  importType: ''
})
const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8820/api'

const importTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '学生账号', value: 'students' },
  { label: '班级专业', value: 'classes' }
]

const importBatchColumns = [
  { title: '导入类型', dataIndex: 'importType', width: 110 },
  { title: '文件名', dataIndex: 'fileName', ellipsis: true },
  { title: '导入结果', key: 'result', width: 230 },
  { title: '管理员 / IP', dataIndex: 'adminName', width: 160 },
  { title: '导入时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

const backupColumns = [
  { title: '文件名', dataIndex: 'filename', width: 320 },
  { title: '大小', dataIndex: 'sizeText', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

const runImport = async (task: ImportTask, file: File) => {
  uploading.value = true
  activeTask.value = task
  try {
    lastResult.value = task === 'students'
      ? await importAdminStudents(file)
      : await importAdminClasses(file)
    const errorCount = lastResult.value.errors?.length || 0
    if (errorCount > 0) {
      message.warning(`导入完成，新增 ${lastResult.value.created} 条，跳过 ${lastResult.value.skipped} 条，异常 ${errorCount} 条`)
    } else {
      message.success(`导入完成，新增 ${lastResult.value.created} 条，跳过 ${lastResult.value.skipped} 条`)
    }
    importBatchQuery.value.current = 1
    await loadImportBatches()
  } finally {
    uploading.value = false
    activeTask.value = ''
  }
}

const beforeUploadStudents = (file: File) => {
  void runImport('students', file)
  return false
}

const beforeUploadClasses = (file: File) => {
  void runImport('classes', file)
  return false
}

const download = (path: string) => {
  window.open(`${apiBase}${path}`, '_blank')
}

const loadImportBatches = async () => {
  importBatchLoading.value = true
  try {
    const page = await getAdminImportBatchList(importBatchQuery.value)
    importBatchList.value = page.records || []
    importBatchTotal.value = page.total || 0
    importBatchQuery.value.current = page.current || importBatchQuery.value.current
    importBatchQuery.value.size = page.size || importBatchQuery.value.size
  } finally {
    importBatchLoading.value = false
  }
}

const handleImportTypeChange = () => {
  importBatchQuery.value.current = 1
  void loadImportBatches()
}

const handleImportBatchPageChange = (pagination: { current?: number; pageSize?: number }) => {
  importBatchQuery.value.current = pagination.current || 1
  importBatchQuery.value.size = pagination.pageSize || importBatchQuery.value.size
  void loadImportBatches()
}

const importTypeText = (type: string) => {
  if (type === 'students') return '学生账号'
  if (type === 'classes') return '班级专业'
  return type || '-'
}

const statusColor = (status: string) => {
  if (status === 'success') return 'green'
  if (status === 'partial') return 'orange'
  return 'red'
}

const statusText = (status: string) => {
  if (status === 'success') return '成功'
  if (status === 'partial') return '部分异常'
  return '失败'
}

const downloadBatchErrorReport = (id: number) => {
  download(`/admin/data-transfer/import-batches/error-report?id=${id}`)
}

const loadBackupData = async () => {
  backupLoading.value = true
  try {
    const [status, list] = await Promise.all([getAdminBackupStatus(), getAdminBackupList()])
    backupStatus.value = status
    backupList.value = list || []
  } finally {
    backupLoading.value = false
  }
}

const handleCreateBackup = async () => {
  backupLoading.value = true
  try {
    await createAdminBackup()
    message.success('数据库备份已创建')
    await loadBackupData()
  } finally {
    backupLoading.value = false
  }
}

const downloadBackup = (filename: string) => {
  download(`/admin/data-transfer/backup/download?filename=${encodeURIComponent(filename)}`)
}

const confirmRestore = (filename: string) => {
  Modal.confirm({
    title: '确认恢复数据库备份',
    content: `恢复会用备份文件覆盖当前数据库数据：${filename}。建议先创建当前备份后再执行。`,
    okText: '确认恢复',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      await restoreAdminBackup(filename)
      message.success('数据库恢复已执行，请刷新页面确认数据状态')
      await loadBackupData()
    }
  })
}

const downloadErrorReport = () => {
  const errors = lastResult.value?.errors || []
  if (!errors.length) {
    message.info('暂无异常行')
    return
  }
  const rows = ['序号,错误信息', ...errors.map((item, index) => `${index + 1},"${item.replace(/"/g, '""')}"`)]
  const blob = new Blob([`\uFEFF${rows.join('\n')}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `导入错误报告_${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  void loadBackupData()
  void loadImportBatches()
})
</script>

<style scoped>
.data-transfer-page {
  gap: 18px;
}

.transfer-hero,
.transfer-main,
.side-card,
.import-history-panel,
.backup-panel {
  border: 1px solid #e2e8f0;
  background: #ffffff;
}

.transfer-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 26px 28px;
  border-color: #e8eef7;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.04);
}

.transfer-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.transfer-hero h2 {
  margin: 0;
  color: #182230;
  font-size: 28px;
  line-height: 1.25;
  font-weight: 800;
}

.transfer-hero p {
  max-width: 760px;
  margin: 8px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.9;
}

.hero-actions,
.history-actions,
.backup-actions,
.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.transfer-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: start;
}

.transfer-main,
.import-history-panel,
.backup-panel {
  padding: 20px;
  border-color: #e8eef7;
  border-radius: 20px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.section-title,
.backup-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.section-title h3 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
  font-weight: 800;
}

.section-title p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.path-text {
  color: #334155;
  word-break: break-all;
}

.history-filter {
  width: 140px;
}

.batch-result-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: #334155;
  font-size: 13px;
}

.muted {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.35;
}

.transfer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 14px;
}

.transfer-card {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 14px;
  min-height: 210px;
  padding: 16px;
  border: 1px solid #e8eef7;
  border-radius: 12px;
  background: #ffffff;
}

.primary-card {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.compact-card {
  min-height: 156px;
}

.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 20px;
}

.card-icon.class-card {
  color: #047857;
  background: #ecfdf5;
}

.card-icon.export {
  color: #7c3aed;
  background: #f5f3ff;
}

.card-content {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-title strong {
  color: #0f172a;
  font-size: 16px;
}

.card-content p {
  margin: 8px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.card-actions {
  margin-top: auto;
  padding-top: 14px;
}

.transfer-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.side-card {
  padding: 16px;
  border-color: #e8eef7;
  border-radius: 20px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.side-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
  font-size: 15px;
}

.side-head .anticon {
  color: #2563eb;
}

.result-stats,
.backup-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 14px 0;
}

.result-stats div,
.backup-summary div {
  min-width: 0;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.result-stats span,
.backup-summary span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.result-stats strong,
.backup-summary strong {
  display: block;
  margin-top: 4px;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.backup-summary strong {
  font-size: 15px;
}

.result-alert {
  margin-top: 12px;
}

.error-list {
  display: grid;
  gap: 4px;
  max-height: 160px;
  overflow: auto;
  color: #78350f;
  font-size: 12px;
}

.error-download {
  margin-top: 10px;
}

.empty-result,
.check-list {
  color: #64748b;
  font-size: 13px;
  line-height: 1.8;
}

.empty-result {
  margin: 12px 0 0;
}

.check-list {
  margin: 12px 0 0;
  padding-left: 18px;
}

@media (max-width: 1180px) {
  .transfer-layout {
    grid-template-columns: 1fr;
  }

  .transfer-side {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .transfer-hero,
  .section-title,
  .backup-title {
    align-items: stretch;
    flex-direction: column;
  }

  .transfer-grid,
  .transfer-side,
  .backup-summary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .transfer-hero,
  .transfer-main,
  .side-card,
  .import-history-panel,
  .backup-panel {
    padding: 14px;
  }

  .transfer-card {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .card-actions :deep(.ant-btn),
  .card-actions :deep(.ant-upload),
  .hero-actions .ant-btn,
  .history-actions .ant-btn,
  .history-filter,
  .backup-actions .ant-btn {
    width: 100%;
  }

  .result-stats {
    grid-template-columns: 1fr;
  }
}
</style>
