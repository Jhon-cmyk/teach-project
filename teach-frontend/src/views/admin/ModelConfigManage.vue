<template>
  <div class="service-console-page">
    <section class="service-overview">
      <div class="overview-copy">
        <h2>接口服务与健康检测</h2>
        <p>统一维护 AI 模型、OSS、ASR、Judge0 等服务配置，并查看后端实时检测结果。</p>
      </div>
      <div class="overview-actions">
        <a-button :loading="healthLoading || configLoading" @click="refreshAll">
          <template #icon>
            <ReloadOutlined />
          </template>
          重新检测
        </a-button>
      </div>
    </section>

    <section class="service-card">
      <div class="section-head">
        <div>
          <h3>真实状态检测</h3>
          <p>数据库连接会执行 SQL 检测；模型配置读取真实配置表；外部能力检查当前后端配置是否可用。</p>
        </div>
      </div>
      <a-spin :spinning="healthLoading">
        <div class="health-grid">
          <article v-for="item in healthOverview?.items || []" :key="item.key" class="health-item">
            <div class="health-title">
              <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
              <strong>{{ item.name }}</strong>
            </div>
            <p>{{ item.detail || '-' }}</p>
          </article>
        </div>
      </a-spin>
    </section>

    <section class="service-card">
      <div class="section-head">
        <div>
          <h3>接口服务配置</h3>
          <p>配置数据来自后端数据库，保存后会写入审计日志并刷新检测状态。</p>
        </div>
      </div>

      <a-table
        row-key="id"
        :columns="columns"
        :data-source="configList"
        :loading="configLoading"
        :pagination="false"
        :scroll="{ x: 980 }"
        :locale="{ emptyText: configLoading ? '配置加载中...' : '暂无接口服务配置' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'interfaceName'">
            <div class="interface-cell">
              <span class="interface-name">{{ record.interfaceName }}</span>
              <span class="interface-key">{{ record.interfaceKey }}</span>
            </div>
          </template>

          <template v-else-if="column.dataIndex === 'endpointUrl'">
            <a-tooltip :title="record.endpointUrl">
              <span class="url-text">{{ record.endpointUrl }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'enabled'">
            <a-tag :color="record.enabled === 1 ? 'green' : 'default'">
              {{ record.enabled === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="openEditModal(record)">
              <template #icon>
                <EditOutlined />
              </template>
              编辑
            </a-button>
          </template>
        </template>
      </a-table>
    </section>

    <a-modal
      v-model:open="editModalOpen"
      title="编辑接口服务"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      destroyOnClose
      width="860px"
      centered
    >
      <a-form layout="vertical">
        <div class="form-grid">
          <a-form-item label="接口标识">
            <a-input :value="formState.interfaceKey" disabled />
          </a-form-item>

          <a-form-item label="接口名称" required>
            <a-input v-model:value="formState.interfaceName" placeholder="请输入接口名称" />
          </a-form-item>
        </div>

        <div class="form-grid">
          <a-form-item label="供应商">
            <a-input v-model:value="formState.provider" placeholder="例如 DeepSeek / DashScope / Aliyun OSS" />
          </a-form-item>

          <a-form-item label="状态">
            <a-switch
              v-model:checked="formState.enabled"
              checked-children="启用"
              un-checked-children="停用"
            />
          </a-form-item>
        </div>

        <a-form-item label="服务地址" required>
          <a-input v-model:value="formState.endpointUrl" placeholder="https://example.com/v1/chat/completions" />
        </a-form-item>

        <a-form-item label="模型或参数" required>
          <a-input v-model:value="formState.modelName" placeholder="例如 deepseek-chat / qwen-vl-plus / bucket / API 版本" />
        </a-form-item>

        <a-form-item label="备注">
          <a-textarea
            v-model:value="formState.remark"
            :rows="3"
            placeholder="说明这个接口用于哪些功能"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  EditOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import {
  getAdminHealthOverview,
  getAiModelConfigList,
  updateAiModelConfig
} from '@/api/admin'
import type { AdminHealthOverview, AiModelConfigItem } from '@/types/admin'

const healthLoading = ref(false)
const configLoading = ref(false)
const submitLoading = ref(false)
const editModalOpen = ref(false)
const healthOverview = ref<AdminHealthOverview | null>(null)
const configList = ref<AiModelConfigItem[]>([])

const formState = reactive({
  id: 0,
  interfaceKey: '',
  interfaceName: '',
  provider: '',
  endpointUrl: '',
  modelName: '',
  enabled: true,
  remark: ''
})

const columns = computed(() => [
  {
    title: '接口',
    dataIndex: 'interfaceName',
    width: 220
  },
  {
    title: '供应商',
    dataIndex: 'provider',
    width: 150
  },
  {
    title: '服务地址',
    dataIndex: 'endpointUrl'
  },
  {
    title: '模型或参数',
    dataIndex: 'modelName',
    width: 170
  },
  {
    title: '状态',
    dataIndex: 'enabled',
    width: 100
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    fixed: 'right'
  }
])

const loadHealthOverview = async () => {
  healthLoading.value = true
  try {
    healthOverview.value = await getAdminHealthOverview()
  } finally {
    healthLoading.value = false
  }
}

const loadConfigList = async () => {
  configLoading.value = true
  try {
    configList.value = await getAiModelConfigList() || []
  } finally {
    configLoading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadHealthOverview(), loadConfigList()])
}

const statusColor = (status: string) => {
  if (status === 'normal') return 'green'
  if (status === 'error') return 'red'
  return 'orange'
}

const statusText = (status: string) => {
  if (status === 'normal') return '正常'
  if (status === 'error') return '异常'
  return '需关注'
}

const openEditModal = (record: AiModelConfigItem) => {
  formState.id = record.id
  formState.interfaceKey = record.interfaceKey
  formState.interfaceName = record.interfaceName || ''
  formState.provider = record.provider || ''
  formState.endpointUrl = record.endpointUrl || ''
  formState.modelName = record.modelName || ''
  formState.enabled = record.enabled === 1
  formState.remark = record.remark || ''
  editModalOpen.value = true
}

const handleSubmit = async () => {
  if (!formState.interfaceName.trim()) {
    message.warning('请输入接口名称')
    return
  }
  if (!/^https?:\/\//i.test(formState.endpointUrl.trim())) {
    message.warning('服务地址必须以 http:// 或 https:// 开头')
    return
  }
  if (!formState.modelName.trim()) {
    message.warning('请输入模型或参数')
    return
  }

  submitLoading.value = true
  try {
    await updateAiModelConfig({
      id: formState.id,
      interfaceName: formState.interfaceName.trim(),
      provider: formState.provider.trim(),
      endpointUrl: formState.endpointUrl.trim(),
      modelName: formState.modelName.trim(),
      enabled: formState.enabled,
      remark: formState.remark.trim()
    })
    message.success('接口服务配置已保存')
    editModalOpen.value = false
    await refreshAll()
  } finally {
    submitLoading.value = false
  }
}

onMounted(refreshAll)
</script>

<style scoped>
.service-console-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.service-overview,
.health-summary,
.service-card {
  border: 1px solid #e8eef7;
  background: #ffffff;
}

.service-overview {
  padding: 26px 28px;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.04);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.overview-copy h2,
.section-head h3 {
  margin: 0;
  color: #0f172a;
  font-weight: 800;
}

.overview-copy h2 {
  color: #182230;
  font-size: 28px;
  line-height: 1.25;
}

.overview-copy p,
.section-head p {
  margin: 10px 0 0;
  color: #667085;
  font-size: 14px;
  line-height: 1.8;
}

.health-summary {
  padding: 18px 20px;
  border-radius: 18px;
  display: grid;
  grid-template-columns: 160px 160px minmax(220px, 1fr);
  gap: 12px;
}

.summary-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid #e8eef7;
  border-radius: 12px;
  background: #f8fbff;
}

.summary-item span {
  color: #64748b;
  font-size: 12px;
}

.summary-item strong {
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
}

.summary-item.wide strong {
  font-size: 16px;
}

.summary-item .danger {
  color: #dc2626;
}

.service-card {
  padding: 20px;
  border-radius: 20px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.04);
}

.section-head {
  margin-bottom: 14px;
}

.health-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.health-item {
  padding: 14px;
  border: 1px solid #e8eef7;
  border-radius: 12px;
  background: #f8fafc;
}

.health-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.health-title strong {
  color: #0f172a;
  font-size: 15px;
}

.health-item p {
  margin: 10px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}

.interface-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.interface-name {
  color: #0f172a;
  font-weight: 650;
}

.interface-key {
  color: #64748b;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.url-text {
  display: inline-block;
  max-width: 520px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
  color: #334155;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

:deep(.ant-table-wrapper .ant-table-thead > tr > th) {
  background: #f8fbff;
  color: #334155;
  font-weight: 650;
}

@media (max-width: 980px) {
  .service-overview {
    flex-direction: column;
  }

  .health-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-item.wide {
    grid-column: 1 / -1;
  }
}

@media (max-width: 680px) {
  .health-summary,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .overview-actions,
  .overview-actions .ant-btn {
    width: 100%;
  }

  .url-text {
    max-width: 280px;
  }
}
</style>
