<template>
  <div class="graph-page" @click="closeContextMenu">
    <!-- 顶部导航 -->
    <div class="page-header">
      <div class="title-group">
        <h2><share-alt-outlined class="title-icon graph-icon" /> 课程知识图谱</h2>
        <div class="subtitle-wrapper">
          <span class="subtitle">当前视图路径：</span>
          <a-breadcrumb class="graph-breadcrumb">
            <a-breadcrumb-item v-for="(item, index) in history" :key="item.id ?? 'root'">
              <a @click="jumpToHistory(index)">{{ item.name }}</a>
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>
      </div>
      <div class="header-actions">
        <a-button @click="openAddNode" type="primary"> <plus-outlined /> 添加节点 </a-button>
        <a-button @click="handleSeedDefault"> <database-outlined /> 导入默认图谱 </a-button>
        <a-button @click="resetView"> <reload-outlined /> 重置视图 </a-button>
        <a-button @click="exportData"> <download-outlined /> 导出数据 </a-button>
      </div>
    </div>

    <!-- 图谱主体 -->
    <div class="graph-body">
      <a-spin :spinning="loading" class="chart-spin" tip="加载中…">
        <div class="chart-card" ref="chartCardRef">
          <div v-show="!isEmpty" class="chart-container" ref="chartRef"></div>
          <div v-if="isEmpty && !loading" class="empty-overlay">
            <a-empty description="课程图谱暂无数据">
              <template #extra>
                <a-button type="primary" @click="handleSeedDefault">导入默认图谱</a-button>
              </template>
            </a-empty>
          </div>
          <div v-if="linkMode.active" class="link-overlay">
            <div class="link-banner">
              <span>请点击目标节点以创建连接…</span>
              <a-button size="small" danger shape="circle" @click="cancelLinkMode">✕</a-button>
            </div>
          </div>
        </div>
      </a-spin>
    </div>

    <!-- 右键菜单 -->
    <teleport to="body">
      <div
        v-if="ctxMenu.visible"
        class="context-menu"
        :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
        @click.stop
      >
        <div
          v-for="item in ctxMenu.items"
          :key="item.label"
          class="context-menu-item"
          :class="{ danger: item.danger }"
          @click="
            () => {
              item.onClick()
              closeContextMenu()
            }
          "
        >
          {{ item.label }}
        </div>
      </div>
    </teleport>

    <!-- 节点编辑弹窗 -->
    <a-modal
      v-model:open="nodeDialog.visible"
      :title="nodeDialog.isEditing ? '编辑节点' : '新增节点'"
      :width="480"
      @cancel="nodeDialog.visible = false"
    >
      <a-form layout="vertical">
        <a-form-item label="节点名称" required>
          <a-input v-model:value="formNode.name" placeholder="请输入节点名称" />
        </a-form-item>
        <a-form-item label="类别">
          <a-select v-model:value="formNode.category" style="width: 100%">
            <a-select-option v-for="cat in categories" :key="cat.name" :value="cat.name">
              {{ cat.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="重要性（节点大小）">
          <a-slider v-model:value="formNode.symbolSize" :min="10" :max="100" />
        </a-form-item>
        <a-form-item label="学习链接">
          <a-input v-model:value="formNode.learnUrl" placeholder="叶子节点可填写 URL" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="formNode.description" :rows="3" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button v-if="nodeDialog.isEditing" danger style="float: left" @click="handleDeleteNode">
          删除节点
        </a-button>
        <a-button @click="nodeDialog.visible = false">取消</a-button>
        <a-button type="primary" @click="handleSaveNode">确定</a-button>
      </template>
    </a-modal>

    <!-- 绑定作业/练习弹窗 -->
    <a-modal
      v-model:open="bindDialog.visible"
      :title="'绑定作业/练习 - ' + bindDialog.nodeName"
      :width="560"
      :footer="null"
      @cancel="bindDialog.visible = false"
    >
      <a-spin :spinning="bindDialog.loading">
        <!-- 已绑定活动 -->
        <div v-if="boundActivities.length > 0" class="bound-section">
          <div class="bound-label">已绑定活动</div>
          <div v-for="act in boundActivities" :key="act.id" class="bound-item">
            <a-tag :color="act.activityType === 'coding' ? 'blue' : 'green'">
              {{ act.activityType === 'coding' ? '编程题' : act.activityType === 'practice' ? '练习' : '作业' }}
            </a-tag>
            <span class="bound-title">{{ act.activityTitle }}</span>
            <a-button size="small" danger type="link" @click="handleUnbindActivity(act.id)">解绑</a-button>
          </div>
        </div>
        <!-- 候选活动 -->
        <a-tabs size="small" style="margin-top: 12px">
          <a-tab-pane key="homework" tab="作业">
            <div v-for="hw in bindCandidates.homework" :key="hw.id" class="candidate-item">
              <span class="candidate-title">{{ hw.title }}</span>
              <a-button
                size="small"
                type="primary"
                :disabled="isActivityBound('homework', hw.id)"
                @click="handleBindActivity('homework', hw.id)"
              >
                {{ isActivityBound('homework', hw.id) ? '已绑定' : '绑定' }}
              </a-button>
            </div>
            <a-empty v-if="!bindCandidates.homework?.length" description="暂无可绑定的作业" :image="null" />
          </a-tab-pane>
          <a-tab-pane key="coding" tab="编程题">
            <div v-for="cp in bindCandidates.coding" :key="cp.id" class="candidate-item">
              <span class="candidate-title">{{ cp.title }}</span>
              <a-button
                size="small"
                type="primary"
                :disabled="isActivityBound('coding', cp.id)"
                @click="handleBindActivity('coding', cp.id)"
              >
                {{ isActivityBound('coding', cp.id) ? '已绑定' : '绑定' }}
              </a-button>
            </div>
            <a-empty v-if="!bindCandidates.coding?.length" description="暂无可绑定的编程题" :image="null" />
          </a-tab-pane>
        </a-tabs>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { message, Modal } from 'ant-design-vue'
import {
  ShareAltOutlined,
  PlusOutlined,
  ReloadOutlined,
  DownloadOutlined,
  DatabaseOutlined,
} from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchGraphData,
  createNode as apiCreateNode,
  updateNode as apiUpdateNode,
  deleteNode as apiDeleteNode,
  createLink as apiCreateLink,
  deleteLink as apiDeleteLink,
  seedDefaultGraph,
  bindNodeActivity,
  unbindNodeActivity,
  listNodeActivities,
} from '@/api/courseGraph'
import { fetchActivityCandidates } from '@/api/courseGraphStats'

// ═══════════════════════════════════════════════════
//  类型定义
// ═══════════════════════════════════════════════════
interface GraphNode {
  id: string
  parentId: string | null
  name: string
  category: string
  symbolSize: number
  description?: string
  learnUrl?: string
}

interface GraphLink {
  id: number
  source: string
  target: string
  description?: string
}

interface GraphCategory {
  name: string
}

interface CtxMenuItem {
  label: string
  danger?: boolean
  onClick: () => void
}

// ═══════════════════════════════════════════════════
//  响应式数据
// ═══════════════════════════════════════════════════
const router = useRouter()
const loading = ref(false)
const isEmpty = ref(false)
const categories = ref<GraphCategory[]>([])
const allNodes = ref<GraphNode[]>([])
const allLinks = ref<GraphLink[]>([])
const history = ref<{ id: string | null; name: string }[]>([{ id: null, name: '主视图' }])

const chartRef = ref<HTMLElement | null>(null)
const chartCardRef = ref<HTMLElement | null>(null)
let myChart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const nodeDialog = reactive({ visible: false, isEditing: false })
const formNode = reactive({
  id: '',
  name: '',
  category: '未分类',
  symbolSize: 30,
  learnUrl: '',
  description: '',
  parentId: null as string | null,
})

const linkMode = reactive<{ active: boolean; sourceNode: GraphNode | null }>({
  active: false,
  sourceNode: null,
})

const bindDialog = reactive({ visible: false, nodeId: '', nodeName: '', loading: false })
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const bindCandidates = ref<{ homework: any[]; coding: any[] }>({ homework: [], coding: [] })
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const boundActivities = ref<any[]>([])

const ctxMenu = reactive<{ visible: boolean; x: number; y: number; items: CtxMenuItem[] }>({
  visible: false,
  x: 0,
  y: 0,
  items: [],
})

const closeContextMenu = (): void => {
  ctxMenu.visible = false
}
const currentRootId = (): string | null => history.value[history.value.length - 1].id

// ═══════════════════════════════════════════════════
//  本地旧存储清理（迁移到云端后一次性清除）
// ═══════════════════════════════════════════════════
const clearOldLocalStorage = (): void => {
  localStorage.removeItem('graphNodes_v5')
  localStorage.removeItem('graphLinks_v5')
  localStorage.removeItem('graphCategories_v5')
}

// ═══════════════════════════════════════════════════
//  基于 links 构建邻接表（实际数据 parentId 可能全为空）
// ═══════════════════════════════════════════════════
let childrenMap = new Map<string, string[]>()
let inDegreeMap = new Map<string, number>()

const buildGraphMaps = (): void => {
  childrenMap = new Map<string, string[]>()
  inDegreeMap = new Map<string, number>()

  for (const node of allNodes.value) {
    childrenMap.set(node.id, [])
    inDegreeMap.set(node.id, 0)
  }

  for (const link of allLinks.value) {
    const src = String(link.source)
    const tgt = String(link.target)
    if (childrenMap.has(src)) {
      childrenMap.get(src)!.push(tgt)
    }
    if (inDegreeMap.has(tgt)) {
      inDegreeMap.set(tgt, (inDegreeMap.get(tgt) ?? 0) + 1)
    }
  }
}

const getRootIds = (): string[] => {
  return allNodes.value
    .filter((n) => (inDegreeMap.get(n.id) ?? 0) === 0)
    .map((n) => n.id)
}

const isRootNode = (n: GraphNode): boolean => {
  // 优先看 parentId；如果 parentId 无效， fallback 到入度判断
  if (n.parentId === null || n.parentId === undefined || n.parentId === '') {
    return (inDegreeMap.get(n.id) ?? 0) === 0
  }
  return false
}

// ═══════════════════════════════════════════════════
//  数据加载（后端 API）
// ═══════════════════════════════════════════════════
const loadData = async (): Promise<void> => {
  loading.value = true
  try {
    const data = await fetchGraphData()
    allNodes.value = data.nodes ?? []
    allLinks.value = data.links ?? []
    categories.value = data.categories ?? []
    buildGraphMaps()
    isEmpty.value = allNodes.value.length === 0
    // eslint-disable-next-line no-console
    console.log('[GraphView] loaded', { nodeCount: allNodes.value.length, linkCount: allLinks.value.length, isEmpty: isEmpty.value, roots: getRootIds() })
  } catch (err) {
    message.error('图谱数据加载失败，请检查网络')
    allNodes.value = []
    allLinks.value = []
    categories.value = []
    childrenMap = new Map()
    inDegreeMap = new Map()
    isEmpty.value = true
  } finally {
    loading.value = false
  }
}

const handleSeedDefault = async (): Promise<void> => {
  if (allNodes.value.length > 0) {
    const ok = await new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: '确认导入默认图谱？',
        content: '当前已有图谱数据，导入默认图谱将清空所有现有数据，是否继续？',
        okText: '确认导入',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      })
    })
    if (!ok) return
  }
  loading.value = true
  try {
    const data = await seedDefaultGraph()
    allNodes.value = data.nodes ?? []
    allLinks.value = data.links ?? []
    categories.value = data.categories ?? []
    buildGraphMaps()
    isEmpty.value = allNodes.value.length === 0
    renderGraph(currentRootId())
    message.success('默认图谱已导入')
  } catch (err) {
    message.error('导入默认图谱失败')
  } finally {
    loading.value = false
  }
}

const exportData = (): void => {
  const payload = { nodes: allNodes.value, links: allLinks.value, categories: categories.value }
  const dataUrl =
    'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(payload, null, 2))
  const anchor = document.createElement('a')
  anchor.href = dataUrl
  anchor.download = 'course-graph.json'
  anchor.click()
  message.success('数据已导出')
}

// ═══════════════════════════════════════════════════
//  图表
// ═══════════════════════════════════════════════════
const initChart = (): void => {
  if (!chartRef.value) return
  myChart = echarts.init(chartRef.value)

  chartRef.value.addEventListener('contextmenu', (e: MouseEvent) => e.preventDefault())

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  myChart.on('click', (params: any) => {
    if (params.dataType !== 'node') return
    const nodeId = String(params.data.id)
    const nodeData: GraphNode = { ...params.data, id: nodeId }

    if (linkMode.active) {
      handleCreateLink(nodeData)
      return
    }

    // 如果点击的是当前中心节点，不重复下钻
    if (nodeId === currentRootId()) {
      return
    }

    const childIds = childrenMap.get(nodeId) ?? []
    const hasChildren = childIds.length > 0
    const currentDepth = history.value.length - 1 // 0=主视图, 1=根, 2=一级分类
    if (hasChildren && currentDepth < 2) {
      history.value.push({ id: nodeId, name: nodeData.name })
      renderGraph(nodeId)
    } else {
      // 叶子节点：跳转到知识点详情页
      router.push(`/teacher/graph/node/${nodeId}`)
    }
  })

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  myChart.on('contextmenu', (params: any) => {
    const event: MouseEvent = params.event?.event ?? params.event
    ctxMenu.x = event.clientX
    ctxMenu.y = event.clientY
    ctxMenu.items = buildContextMenuItems(params)
    ctxMenu.visible = true
  })
}

const renderGraph = (rootId: string | null): void => {
  if (!myChart) return

  let visibleNodes: GraphNode[]
  if (rootId === null) {
    visibleNodes = allNodes.value.filter(isRootNode)
  } else {
    const rootNode = allNodes.value.find((n: GraphNode) => n.id === rootId)
    const childIds = childrenMap.get(rootId) ?? []
    const children = allNodes.value.filter((n: GraphNode) => childIds.includes(n.id))
    visibleNodes = rootNode ? [rootNode, ...children] : children
    // eslint-disable-next-line no-console
    console.log('[renderGraph children]', { rootId, rootName: rootNode?.name, childIds, childNames: children.map(c => c.name) })
  }

  // eslint-disable-next-line no-console
  console.log('[renderGraph]', { rootId, total: allNodes.value.length, visible: visibleNodes.length, names: visibleNodes.map(n => n.name) })

  const visibleIdSet = new Set(visibleNodes.map((n: GraphNode) => n.id))
  const visibleLinks = allLinks.value.filter(
    (l: GraphLink) => visibleIdSet.has(l.source) && visibleIdSet.has(l.target),
  )

  const currentRoot = rootId ? allNodes.value.find((n: GraphNode) => n.id === rootId) : null

  myChart.setOption(
    {
      tooltip: {
        trigger: 'item',
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        formatter: (params: any) => {
          if (params.dataType === 'node') {
            const n = allNodes.value.find((x: GraphNode) => x.id === String(params.data.id))
            return `<b>${params.data.name}</b>${n?.description ? '<br/>' + n.description : ''}`
          }
          if (params.dataType === 'edge') {
            return params.data.description?.formatter || ''
          }
          return ''
        },
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e8ecf1',
        textStyle: { color: '#1e293b' },
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
      },
      legend: [
        {
          data: categories.value.map((c: GraphCategory) => c.name),
          textStyle: {
            color: '#475569',
            fontWeight: 500,
            fontFamily: "'Plus Jakarta Sans', sans-serif",
          },
          bottom: 10,
          type: 'scroll',
        },
      ],
      series: [
        {
          type: 'graph',
          layout: 'force',
          roam: true,
          draggable: true,
          force: {
            repulsion: 820,
            edgeLength: [105, 220],
            gravity: 0.06,
          },
          categories: categories.value.map((cat: GraphCategory, i: number) => ({
            name: cat.name,
            itemStyle: {
              color: CATEGORY_COLORS[i % CATEGORY_COLORS.length],
              borderColor: '#ffffff',
              borderWidth: 1.5,
              shadowBlur: 8,
              shadowColor: CATEGORY_COLORS[i % CATEGORY_COLORS.length] + '80',
            },
          })),
          data: visibleNodes.map((n: GraphNode) => ({
            id: n.id,
            name: n.name,
            value: n.symbolSize,
            category: n.category,
            symbolSize: currentRoot && n.id === currentRoot.id
              ? Math.max(42, Math.round(n.symbolSize * 0.78))
              : Math.max(26, Math.round(n.symbolSize * 0.58)),
            draggable: true,
            itemStyle: currentRoot && n.id === currentRoot.id
              ? {
                  borderColor: '#1677ff',
                  borderWidth: 3,
                  shadowBlur: 16,
                  shadowColor: 'rgba(22, 119, 255, 0.4)',
                }
              : undefined,
          })),
          links: visibleLinks.map((l: GraphLink) => ({
            source: l.source,
            target: l.target,
            label: l.description
              ? {
                  show: true,
                  formatter: l.description,
                  fontSize: 12,
                  fontWeight: 700,
                  color: '#64748b',
                  backgroundColor: 'rgba(255, 255, 255, 0.85)',
                  padding: [4, 7],
                  borderRadius: 4,
                }
              : undefined,
            lineStyle: { curveness: 0.25 },
          })),
          label: {
            show: true,
            position: 'right',
            formatter: '{b}',
            color: '#1e293b',
            fontSize: 16,
            fontWeight: 700,
            fontFamily: "'Plus Jakarta Sans', 'Microsoft YaHei', sans-serif",
            textBorderColor: '#ffffff',
            textBorderWidth: 3,
            distance: 10,
          },
          lineStyle: {
            width: 2,
            color: 'source',
            opacity: 0.46,
            curveness: 0.25,
          },
          emphasis: {
            focus: 'adjacency',
            lineStyle: { width: 5, opacity: 0.9 },
            itemStyle: { shadowBlur: 20 },
          },
        },
      ],
    },
    true,
  )
}

// ═══════════════════════════════════════════════════
//  颜色映射（与 categories 顺序一一对应）
// ═══════════════════════════════════════════════════
const CATEGORY_COLORS = [
  '#4F46E5', '#10B981', '#F59E0B', '#EF4444', '#0EA5E9',
  '#8B5CF6', '#EC4899', '#14B8A6', '#F97316', '#64748B',
]

// ═══════════════════════════════════════════════════
//  导航
// ═══════════════════════════════════════════════════
const jumpToHistory = (index: number): void => {
  if (index < history.value.length - 1) {
    history.value = history.value.slice(0, index + 1)
    renderGraph(currentRootId())
  }
}

const resetView = (): void => {
  history.value = [{ id: null, name: '主视图' }]
  renderGraph(null)
  myChart?.dispatchAction({ type: 'restore' })
  message.success('视图已重置')
}

// ═══════════════════════════════════════════════════
//  节点 CRUD（走后端 API）
// ═══════════════════════════════════════════════════
const openAddNode = (): void => {
  nodeDialog.isEditing = false
  Object.assign(formNode, {
    id: '',
    name: '',
    category: categories.value[0]?.name ?? '未分类',
    symbolSize: 30,
    learnUrl: '',
    description: '',
    parentId: currentRootId(),
  })
  nodeDialog.visible = true
}

const openEditNode = (node: GraphNode): void => {
  nodeDialog.isEditing = true
  Object.assign(formNode, {
    id: node.id,
    name: node.name,
    category: node.category || '未分类',
    symbolSize: node.symbolSize || 30,
    learnUrl: node.learnUrl || '',
    description: node.description || '',
    parentId: node.parentId,
  })
  nodeDialog.visible = true
}

const handleSaveNode = async (): Promise<void> => {
  if (!formNode.name.trim()) {
    message.warning('请填写节点名称')
    return
  }

  if (nodeDialog.isEditing) {
    try {
      await apiUpdateNode({
        id: formNode.id,
        name: formNode.name.trim(),
        category: formNode.category,
        symbolSize: formNode.symbolSize,
        parentId: formNode.parentId,
        learnUrl: formNode.learnUrl.trim() || undefined,
        description: formNode.description.trim() || undefined,
      })
      await loadData()
      renderGraph(currentRootId())
      nodeDialog.visible = false
      message.success('节点已保存')
    } catch {
      message.error('保存节点失败')
    }
  } else {
    try {
      await apiCreateNode({
        parentId: formNode.parentId,
        name: formNode.name.trim(),
        category: formNode.category,
        symbolSize: formNode.symbolSize,
        learnUrl: formNode.learnUrl.trim() || undefined,
        description: formNode.description.trim() || undefined,
      })
      await loadData()
      renderGraph(currentRootId())
      nodeDialog.visible = false
      message.success('节点已创建')
    } catch {
      message.error('创建节点失败')
    }
  }
}

const handleDeleteNode = (): void => {
  Modal.confirm({
    title: '确定删除？',
    content: '将同时删除该节点的所有子孙节点和相关连接。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await apiDeleteNode(formNode.id)
        await loadData()
        renderGraph(currentRootId())
        nodeDialog.visible = false
        message.success('节点已删除')
      } catch {
        message.error('删除节点失败')
      }
    },
  })
}

// ═══════════════════════════════════════════════════
//  连线（走后端 API）
// ═══════════════════════════════════════════════════
const startLinkMode = (node: GraphNode): void => {
  linkMode.active = true
  linkMode.sourceNode = node
}

const cancelLinkMode = (): void => {
  linkMode.active = false
  linkMode.sourceNode = null
}

const handleCreateLink = async (targetNode: GraphNode): Promise<void> => {
  const src = linkMode.sourceNode
  if (!src || !targetNode) {
    cancelLinkMode()
    return
  }
  if (src.id === targetNode.id) {
    message.warning('不能连接到自身')
    cancelLinkMode()
    return
  }

  const exists = allLinks.value.some(
    (l: GraphLink) =>
      (l.source === src.id && l.target === targetNode.id) ||
      (l.source === targetNode.id && l.target === src.id),
  )
  if (exists) {
    message.warning('连接已存在')
    cancelLinkMode()
    return
  }

  try {
    await apiCreateLink({ source: src.id, target: targetNode.id })
    await loadData()
    renderGraph(currentRootId())
    cancelLinkMode()
    message.success(`已连接：${src.name} → ${targetNode.name}`)
  } catch {
    message.error('创建连接失败')
    cancelLinkMode()
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleDeleteLink = async (linkData: any): Promise<void> => {
  const linkId = Number(linkData.id)
  if (!linkId || isNaN(linkId)) return

  Modal.confirm({
    title: '删除连接？',
    content: '确定删除该连接？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await apiDeleteLink(linkId)
        await loadData()
        renderGraph(currentRootId())
        message.success('连接已删除')
      } catch {
        message.error('删除连接失败')
      }
    },
  })
}

// ═══════════════════════════════════════════════════
//  右键菜单
// ═══════════════════════════════════════════════════
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const buildContextMenuItems = (params: any): CtxMenuItem[] => {
  if (params.dataType === 'node') {
    const nodeId = String(params.data?.id || '')
    const node = allNodes.value.find((n: GraphNode) => n.id === nodeId)
    if (!node) return []
    return [
      { label: '编辑节点', onClick: () => openEditNode(node) },
      { label: '创建连接', onClick: () => startLinkMode(node) },
      { label: '绑定作业/练习', onClick: () => openBindDialog(node) },
      {
        label: '删除节点',
        danger: true,
        onClick: () => {
          openEditNode(node)
          nextTick(handleDeleteNode)
        },
      },
    ]
  }
  if (params.dataType === 'edge') {
    return [{ label: '删除连接', danger: true, onClick: () => handleDeleteLink(params.data) }]
  }
  return [{ label: '添加新节点', onClick: openAddNode }]
}

// ═══════════════════════════════════════════════════
//  绑定学习活动
// ═══════════════════════════════════════════════════
const openBindDialog = async (node: GraphNode): Promise<void> => {
  bindDialog.nodeId = node.id
  bindDialog.nodeName = node.name
  bindDialog.visible = true
  bindDialog.loading = true
  try {
    const [candidates, activities] = await Promise.all([
      fetchActivityCandidates(),
      listNodeActivities(node.id),
    ])
    bindCandidates.value = candidates ?? { homework: [], coding: [] }
    boundActivities.value = activities ?? []
  } catch {
    message.error('加载活动列表失败')
  } finally {
    bindDialog.loading = false
  }
}

const handleBindActivity = async (activityType: string, activityId: number): Promise<void> => {
  try {
    await bindNodeActivity(bindDialog.nodeId, activityType, activityId)
    message.success('绑定成功')
    boundActivities.value = await listNodeActivities(bindDialog.nodeId)
  } catch {
    message.error('绑定失败')
  }
}

const handleUnbindActivity = async (activityId: number): Promise<void> => {
  try {
    await unbindNodeActivity(activityId)
    message.success('解绑成功')
    boundActivities.value = await listNodeActivities(bindDialog.nodeId)
  } catch {
    message.error('解绑失败')
  }
}

const isActivityBound = (type: string, id: number): boolean => {
  return boundActivities.value.some((a: any) => a.activityType === type && a.activityId === id)
}

// ═══════════════════════════════════════════════════
//  生命周期
// ═══════════════════════════════════════════════════
const handleResize = (): void => {
  myChart?.resize()
}

onMounted(async () => {
  await loadData()
  initChart()
  renderGraph(null)
  clearOldLocalStorage()

  if (chartCardRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(handleResize)
    resizeObserver.observe(chartCardRef.value)
  } else {
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', handleResize)
  myChart?.dispose()
  myChart = null
})
</script>

<style scoped>
.graph-page {
  font-family: 'Plus Jakarta Sans', sans-serif;
  animation: fadeIn 0.4s ease;
  background: #f8fafc;
  border-radius: 5px;
  padding: 18px 24px 8px;
  display: flex;
  flex-direction: column;
  /* 关键：不要再减 88px */
  height: 100vh;
  min-height: 100vh;

  gap: 12px;
  box-sizing: border-box;
  overflow: hidden;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 0;
  flex-shrink: 0;
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
.graph-icon {
  color: #1677ff;
}

.subtitle-wrapper {
  margin: 8px 0 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.subtitle {
  color: #64748b;
  font-size: 14px;
}

.graph-breadcrumb {
  background: #f1f5f9;
  padding: 4px 12px;
  border-radius: 5px;
}
.graph-breadcrumb a {
  font-weight: 600;
  cursor: pointer;
  color: #475569;
}
.graph-breadcrumb a:hover {
  color: #1677ff;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}
.header-actions .ant-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  border-radius: 5px;
  font-weight: 500;
}

.chart-spin {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.chart-spin :deep(.ant-spin-container) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chart-card {
  position: relative;
  flex: 1;
  min-height: 0;
  border-radius: 5px;
  background: #fff;
  border: 1px solid #e8ecf1;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.graph-body {
  flex: 1;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
}


.chart-container {
  width: 100%;
  flex: 1;
  min-height: 0;
}

.empty-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  z-index: 20;
}

.link-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.08);
  z-index: 10;
  pointer-events: none;
}

.link-banner {
  pointer-events: auto;
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  border-radius: 5px;
  background: #1677ff;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);
}

.context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 140px;
  padding: 6px 0;
  border-radius: 5px;
  background: #fff;
  border: 1px solid #e8ecf1;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.context-menu-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}
.context-menu-item:hover {
  background: #f0f5ff;
  color: #1677ff;
}
.context-menu-item.danger {
  color: #e63946;
}
.context-menu-item.danger:hover {
  background: #fff1f0;
  color: #cf1322;
}

@media (max-width: 768px) {
  .graph-page {
    padding: 16px;
    min-height: calc(100vh - 72px);
  }
  .graph-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-actions {
    width: 100%;
  }
  .header-actions :deep(.ant-btn) {
    flex: 1;
  }

  .graph-body,
  .chart-card,
  .chart-container {
    min-height: 520px;
  }
}

.bound-section { margin-bottom: 12px; }
.bound-label { font-weight: 600; font-size: 13px; color: #475569; margin-bottom: 8px; }
.bound-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; border-bottom: 1px solid #f1f5f9;
}
.bound-title { flex: 1; font-size: 13px; color: #1e293b; }
.candidate-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 0; border-bottom: 1px solid #f8fafc;
}
.candidate-title { font-size: 13px; color: #334155; }
</style>
