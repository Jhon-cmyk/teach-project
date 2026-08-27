export type AnimOptimizeAction = 'vivid' | 'stable' | 'basic' | 'slow'
export type AnimTemplateType = 'sort' | 'protocol' | 'stack' | 'queue' | 'tree' | 'graph' | 'concept'
export type AnimRenderStatus = 'idle' | 'validating' | 'ready' | 'fallback'
export type AnimConceptType = 'auto' | 'algorithm' | 'protocol' | 'data-structure' | 'concept'
export type AnimMotionType =
  | 'observe'
  | 'compare'
  | 'swap'
  | 'send'
  | 'push'
  | 'pop'
  | 'enqueue'
  | 'dequeue'
  | 'peek'
  | 'visit'
  | 'insert-node'
  | 'delete-node'
  | 'branch'
  | 'call'
  | 'return'
  | 'flow'
  | 'done'

export interface AnimFormModel {
  concept: string
  conceptType: AnimConceptType
  targetGroup: string
  teachingGoal: string
  demoMode?: string
  visualStyle?: string
  pace?: string
  emphasis: string
  extraRequirements: string
}

export interface AnimBaseStep {
  title: string
  desc: string
  evidenceIds?: string[]
  stageCaption?: string
  motion?: {
    type: AnimMotionType
    fromIndex?: number
    toIndex?: number
    indexes?: number[]
    value?: string | number
    from?: string
    to?: string
    path?: Array<string | number>
    branch?: 'true' | 'false' | 'none'
  }
}

export interface SortAnimStep extends AnimBaseStep {
  array: number[]
  highlight?: number[]
  swap?: number[]
  sortedTailStart?: number | null
}

export interface ProtocolAnimStep extends AnimBaseStep {
  from: string
  to: string
  message: string
  clientState: string
  serverState: string
  messageType?: 'request' | 'response' | 'confirm' | 'close'
}

export interface StackAnimStep extends AnimBaseStep {
  stack: Array<string | number>
  operation: 'init' | 'push' | 'pop' | 'peek' | 'done'
  activeValue?: string | number | null
  poppedValue?: string | number | null
}

export interface QueueAnimStep extends AnimBaseStep {
  queue: Array<string | number>
  operation: 'init' | 'enqueue' | 'dequeue' | 'peek' | 'done'
  activeValue?: string | number | null
  removedValue?: string | number | null
}

export interface TreeAnimStep extends AnimBaseStep {
  currentNode?: string | number | null
  path?: Array<string | number>
  visited?: Array<string | number>
  operation: 'init' | 'visit' | 'compare' | 'go-left' | 'go-right' | 'backtrack' | 'done'
}

export interface GraphEdge {
  from: string | number
  to: string | number
  label?: string
  directed?: boolean
}

export interface GraphAnimStep extends AnimBaseStep {
  activeNode?: string | number | null
  visited?: Array<string | number>
  frontier?: Array<string | number>
  activeEdges?: Array<string | number | GraphEdge>
  operation: 'init' | 'visit' | 'enqueue' | 'dequeue' | 'push' | 'pop' | 'relax' | 'done'
}

/* ==================== 视觉原语 ==================== */

/** 节点链:线性表/链表/数组/队列 */
export interface VisualNodesChain {
  type: 'nodes-chain'
  nodes: Array<string | number>
  linked?: boolean // 是否显示连接线(链表 true,数组 false)
  highlight?: number[] // 高亮的索引
  action?: 'insert' | 'delete' | 'access' | 'search' | 'none'
  actionIndex?: number // 动作作用的位置
  actionValue?: string | number // 动作涉及的值(如插入的值)
}

/** 递归树节点 */
export interface TreeNode {
  value: string | number
  left?: TreeNode | null
  right?: TreeNode | null
}

/** 树:二叉树/BST/堆 */
export interface VisualTree {
  type: 'tree'
  root: TreeNode
  highlight?: Array<string | number> // 高亮的节点值
  path?: Array<string | number> // 路径(如查找路径)
}

/** 条件分支:if/switch */
export interface VisualBranching {
  type: 'branching'
  condition: string
  trueLabel: string
  falseLabel: string
  activeBranch?: 'true' | 'false' | 'none'
}

/** 对比:顺序存储 vs 链式、值传递 vs 引用 */
export interface VisualComparison {
  type: 'comparison'
  leftTitle: string
  leftItems: string[]
  rightTitle: string
  rightItems: string[]
  winner?: 'left' | 'right' | 'none' // 可选:标注更优方
}

/** 高亮卡片:定义、复杂度、核心公式 */
export interface VisualHighlightCard {
  type: 'highlight-card'
  mainValue: string // 大字:核心词/值
  label?: string // 小字:描述
  tone?: 'info' | 'success' | 'warn' | 'danger'
}

/** 流程框图:循环、递归、算法步骤 */
export interface VisualFlow {
  type: 'flow'
  boxes: string[]
  activeIndex?: number // 当前激活的步骤
  loopBack?: boolean // 是否有回溯箭头(循环)
}

export type ConceptVisual =
  | VisualNodesChain
  | VisualTree
  | VisualBranching
  | VisualComparison
  | VisualHighlightCard
  | VisualFlow

/**
 * 通用概念卡片步骤
 * 新增 visual 字段:如果提供,播放器会渲染对应的视觉原语
 */
export interface ConceptAnimStep extends AnimBaseStep {
  focus?: string
  analogy?: string
  keyPoints?: string[]
  visualHint?: string
  codeSnippet?: string
  visual?: ConceptVisual
}

export interface SortAnimPayload {
  templateType: 'sort'
  title: string
  subtitle: string
  targetGroup: string
  teachingGoal: string
  initialData: number[]
  steps: SortAnimStep[]
}

export interface ProtocolAnimPayload {
  templateType: 'protocol'
  title: string
  subtitle: string
  targetGroup: string
  teachingGoal: string
  actors: string[]
  steps: ProtocolAnimStep[]
}

export interface StackAnimPayload {
  templateType: 'stack'
  title: string
  subtitle: string
  targetGroup: string
  teachingGoal: string
  initialStack: Array<string | number>
  steps: StackAnimStep[]
}

export interface QueueAnimPayload {
  templateType: 'queue'
  title: string
  subtitle: string
  targetGroup: string
  teachingGoal: string
  initialQueue: Array<string | number>
  steps: QueueAnimStep[]
}

export interface TreeAnimPayload {
  templateType: 'tree'
  title: string
  subtitle: string
  targetGroup: string
  teachingGoal: string
  root: TreeNode
  steps: TreeAnimStep[]
}

export interface GraphAnimPayload {
  templateType: 'graph'
  title: string
  subtitle: string
  targetGroup: string
  teachingGoal: string
  nodes: Array<string | number>
  edges: GraphEdge[]
  steps: GraphAnimStep[]
}

export interface ConceptAnimPayload {
  templateType: 'concept'
  title: string
  subtitle: string
  targetGroup: string
  teachingGoal: string
  mainTerm: string
  coreIdea: string
  steps: ConceptAnimStep[]
}

export type AnimPayload =
  | SortAnimPayload
  | ProtocolAnimPayload
  | StackAnimPayload
  | QueueAnimPayload
  | TreeAnimPayload
  | GraphAnimPayload
  | ConceptAnimPayload

export interface PresetBuildOptions {
  targetGroup?: string
  teachingGoal?: string
}

export const cloneAnimData = <T>(data: T): T => JSON.parse(JSON.stringify(data))
