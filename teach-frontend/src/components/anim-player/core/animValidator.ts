import type {
  AnimPayload,
  ConceptAnimPayload,
  ConceptVisual,
  GraphAnimPayload,
  ProtocolAnimPayload,
  QueueAnimPayload,
  SortAnimPayload,
  StackAnimPayload,
  TreeAnimPayload,
} from './animTypes.ts'

export interface AnimValidationResult {
  valid: boolean
  errors: string[]
}

export const extractJsonText = (raw: string) => {
  let text = (raw || '').trim()
  if (!text) return text

  if (text.startsWith('```')) {
    text = text.replace(/^```(?:json)?\s*/i, '')
    text = text.replace(/```$/i, '')
  }

  const firstBrace = text.indexOf('{')
  const lastBrace = text.lastIndexOf('}')

  if (firstBrace !== -1 && lastBrace !== -1 && lastBrace > firstBrace) {
    text = text.slice(firstBrace, lastBrace + 1)
  }

  return text.trim()
}

export const safeParseAnimJson = (raw: string): AnimPayload => {
  const jsonText = extractJsonText(raw)
  if (!jsonText) {
    throw new Error('模型没有返回有效 JSON')
  }
  return JSON.parse(jsonText)
}

export const validateSortAnim = (data: SortAnimPayload) => {
  const errors: string[] = []

  if (!Array.isArray(data.initialData) || data.initialData.length < 2) {
    errors.push('排序模板缺少 initialData,且长度至少为 2。')
  }

  if (!Array.isArray(data.steps) || data.steps.length < 3) {
    errors.push('排序模板 steps 至少需要 3 步。')
    return errors
  }

  const expectedLength = Array.isArray(data.initialData) ? data.initialData.length : null

  data.steps.forEach((step, index) => {
    normalizeBaseStepFields(step)

    if (!Array.isArray(step.array)) {
      errors.push(`第 ${index + 1} 步缺少 array。`)
      return
    }

    if (expectedLength !== null && step.array.length !== expectedLength) {
      errors.push(`第 ${index + 1} 步的 array 长度与 initialData 不一致。`)
    }

    const hasHighlight = step.highlight !== undefined && step.highlight !== null
    if (hasHighlight) {
      if (!Array.isArray(step.highlight)) {
        errors.push(`第 ${index + 1} 步的 highlight 非法。`)
      } else if (step.highlight.some((n) => n < 0 || n >= step.array.length)) {
        errors.push(`第 ${index + 1} 步的 highlight 存在越界下标。`)
      }
    }

    const hasSwap = step.swap !== undefined && step.swap !== null
    if (hasSwap) {
      if (!Array.isArray(step.swap)) {
        errors.push(`第 ${index + 1} 步的 swap 非法。`)
      } else if (![0, 2].includes(step.swap.length)) {
        errors.push(`第 ${index + 1} 步的 swap 非法。`)
      } else if (step.swap.some((n) => n < 0 || n >= step.array.length)) {
        errors.push(`第 ${index + 1} 步的 swap 存在越界下标。`)
      }
    }
  })

  return errors
}

const normalizeBaseStepFields = (step: any) => {
  if (!step || typeof step !== 'object') return

  if (step.stageCaption !== undefined && step.stageCaption !== null) {
    step.stageCaption = String(step.stageCaption).slice(0, 80)
  }

  if (step.evidenceIds !== undefined && step.evidenceIds !== null) {
    step.evidenceIds = Array.isArray(step.evidenceIds)
      ? step.evidenceIds.map((id: any) => String(id)).filter((id: string) => /^E\d+$/.test(id))
      : []
  }

  if (step.motion !== undefined && step.motion !== null) {
    if (typeof step.motion !== 'object') {
      delete step.motion
      return
    }

    const allowedMotionTypes = [
      'observe',
      'compare',
      'swap',
      'send',
      'push',
      'pop',
      'enqueue',
      'dequeue',
      'peek',
      'visit',
      'insert-node',
      'delete-node',
      'branch',
      'call',
      'return',
      'flow',
      'done',
    ]
    if (!allowedMotionTypes.includes(step.motion.type)) {
      delete step.motion
    }
  }
}

export const validateProtocolAnim = (data: ProtocolAnimPayload) => {
  const errors: string[] = []

  if (!Array.isArray(data.actors) || data.actors.length !== 2) {
    errors.push('协议模板必须提供 2 个 actors。')
  }

  if (!Array.isArray(data.steps) || data.steps.length < 3) {
    errors.push('协议模板 steps 至少需要 3 步。')
    return errors
  }

  data.steps.forEach((step, index) => {
    normalizeBaseStepFields(step)

    if (!step.from || !step.to) {
      errors.push(`第 ${index + 1} 步缺少 from / to。`)
    }

    if (Array.isArray(data.actors) && data.actors.length === 2) {
      if (![data.actors[0], data.actors[1]].includes(step.from)) {
        errors.push(`第 ${index + 1} 步的 from 不在 actors 内。`)
      }
      if (![data.actors[0], data.actors[1]].includes(step.to)) {
        errors.push(`第 ${index + 1} 步的 to 不在 actors 内。`)
      }
    }

    if (!step.message) {
      errors.push(`第 ${index + 1} 步缺少 message。`)
    }

    if (!step.clientState || !step.serverState) {
      errors.push(`第 ${index + 1} 步缺少 clientState 或 serverState。`)
    }
  })

  return errors
}

export const validateStackAnim = (data: StackAnimPayload) => {
  const errors: string[] = []

  if (!Array.isArray(data.initialStack)) {
    errors.push('栈模板缺少 initialStack。')
  }

  if (!Array.isArray(data.steps) || data.steps.length < 3) {
    errors.push('栈模板 steps 至少需要 3 步。')
    return errors
  }

  data.steps.forEach((step, index) => {
    normalizeBaseStepFields(step)

    if (!Array.isArray(step.stack)) {
      errors.push(`第 ${index + 1} 步缺少 stack。`)
    }

    if (!step.operation || !['init', 'push', 'pop', 'peek', 'done'].includes(step.operation)) {
      errors.push(`第 ${index + 1} 步的 operation 非法。`)
    }
  })

  return errors
}

export const validateQueueAnim = (data: QueueAnimPayload) => {
  const errors: string[] = []

  if (!Array.isArray(data.initialQueue)) {
    errors.push('队列模板缺少 initialQueue。')
  }

  if (!Array.isArray(data.steps) || data.steps.length < 3) {
    errors.push('队列模板 steps 至少需要 3 步。')
    return errors
  }

  data.steps.forEach((step, index) => {
    normalizeBaseStepFields(step)

    if (!Array.isArray(step.queue)) {
      errors.push(`第 ${index + 1} 步缺少 queue。`)
    }

    if (!step.operation || !['init', 'enqueue', 'dequeue', 'peek', 'done'].includes(step.operation)) {
      errors.push(`第 ${index + 1} 步的 operation 非法。`)
    }
  })

  return errors
}

const flattenTreeValues = (root: any): Array<string | number> => {
  if (!root || typeof root !== 'object') return []
  return [
    root.value,
    ...flattenTreeValues(root.left),
    ...flattenTreeValues(root.right),
  ].filter((value) => value !== undefined && value !== null)
}

export const validateTreeAnim = (data: TreeAnimPayload) => {
  const errors: string[] = []

  if (!data.root || typeof data.root !== 'object' || data.root.value === undefined) {
    errors.push('树模板缺少 root。')
  }

  if (!Array.isArray(data.steps) || data.steps.length < 3) {
    errors.push('树模板 steps 至少需要 3 步。')
    return errors
  }

  const values = new Set(flattenTreeValues(data.root).map(String))

  data.steps.forEach((step, index) => {
    normalizeBaseStepFields(step)

    if (!step.operation || !['init', 'visit', 'compare', 'go-left', 'go-right', 'backtrack', 'done'].includes(step.operation)) {
      errors.push(`第 ${index + 1} 步的 operation 非法。`)
    }

    const currentNode = step.currentNode
    if (currentNode !== undefined && currentNode !== null && values.size && !values.has(String(currentNode))) {
      errors.push(`第 ${index + 1} 步的 currentNode 不在树中。`)
    }

    if (step.path !== undefined && !Array.isArray(step.path)) {
      errors.push(`第 ${index + 1} 步的 path 非法。`)
    }

    if (step.visited !== undefined && !Array.isArray(step.visited)) {
      errors.push(`第 ${index + 1} 步的 visited 非法。`)
    }
  })

  return errors
}

const edgeKey = (from: unknown, to: unknown) => `${String(from)}->${String(to)}`

export const validateGraphAnim = (data: GraphAnimPayload) => {
  const errors: string[] = []

  if (!Array.isArray(data.nodes) || data.nodes.length < 2) {
    errors.push('图模板 nodes 至少需要 2 个节点。')
  }

  if (!Array.isArray(data.edges) || data.edges.length < 1) {
    errors.push('图模板至少需要 1 条 edge。')
  }

  if (!Array.isArray(data.steps) || data.steps.length < 3) {
    errors.push('图模板 steps 至少需要 3 步。')
    return errors
  }

  const nodeSet = new Set((data.nodes || []).map(String))
  const edgeSet = new Set<string>()
  ;(data.edges || []).forEach((edge, index) => {
    if (!edge || edge.from === undefined || edge.to === undefined) {
      errors.push(`第 ${index + 1} 条 edge 缺少 from/to。`)
      return
    }
    if (!nodeSet.has(String(edge.from)) || !nodeSet.has(String(edge.to))) {
      errors.push(`第 ${index + 1} 条 edge 指向不存在的节点。`)
    }
    edgeSet.add(edgeKey(edge.from, edge.to))
    if (!edge.directed) edgeSet.add(edgeKey(edge.to, edge.from))
  })

  data.steps.forEach((step, index) => {
    normalizeBaseStepFields(step)

    if (!step.operation || !['init', 'visit', 'enqueue', 'dequeue', 'push', 'pop', 'relax', 'done'].includes(step.operation)) {
      errors.push(`第 ${index + 1} 步的 operation 非法。`)
    }

    if (step.activeNode !== undefined && step.activeNode !== null && !nodeSet.has(String(step.activeNode))) {
      errors.push(`第 ${index + 1} 步的 activeNode 不在 nodes 内。`)
    }

    if (step.visited !== undefined && !Array.isArray(step.visited)) {
      errors.push(`第 ${index + 1} 步的 visited 非法。`)
    }

    if (step.frontier !== undefined && !Array.isArray(step.frontier)) {
      errors.push(`第 ${index + 1} 步的 frontier 非法。`)
    }

    if (step.activeEdges !== undefined && !Array.isArray(step.activeEdges)) {
      errors.push(`第 ${index + 1} 步的 activeEdges 非法。`)
    } else if (Array.isArray(step.activeEdges)) {
      step.activeEdges.forEach((item: any) => {
        if (item && typeof item === 'object' && item.from !== undefined && item.to !== undefined) {
          if (!edgeSet.has(edgeKey(item.from, item.to))) {
            errors.push(`第 ${index + 1} 步的 activeEdges 包含不存在的边。`)
          }
        }
      })
    }
  })

  return errors
}

/**
 * 校验 visual 字段,不强求,但如果 AI 返回了非法的 visual,直接丢弃而不让整份 JSON 失败
 * 返回:清理后的 visual(合法就原样返回,非法返回 undefined)
 */
export const sanitizeConceptVisual = (visual: unknown): ConceptVisual | undefined => {
  if (!visual || typeof visual !== 'object') return undefined
  const v = visual as any

  switch (v.type) {
    case 'nodes-chain':
      if (!Array.isArray(v.nodes) || v.nodes.length === 0) return undefined
      return {
        type: 'nodes-chain',
        nodes: v.nodes.map((n: any) => (typeof n === 'number' ? n : String(n))),
        linked: Boolean(v.linked),
        highlight: Array.isArray(v.highlight)
          ? v.highlight.filter((i: any) => typeof i === 'number' && i >= 0 && i < v.nodes.length)
          : [],
        action: ['insert', 'delete', 'access', 'search', 'none'].includes(v.action)
          ? v.action
          : 'none',
        actionIndex:
          typeof v.actionIndex === 'number' && v.actionIndex >= 0 && v.actionIndex <= v.nodes.length
            ? v.actionIndex
            : undefined,
        actionValue: v.actionValue !== undefined ? v.actionValue : undefined,
      }

    case 'tree':
      if (!v.root || typeof v.root !== 'object') return undefined
      return {
        type: 'tree',
        root: v.root, // 递归结构不深度清洗,交给渲染器容错
        highlight: Array.isArray(v.highlight) ? v.highlight : [],
        path: Array.isArray(v.path) ? v.path : [],
      }

    case 'branching':
      if (!v.condition || !v.trueLabel || !v.falseLabel) return undefined
      return {
        type: 'branching',
        condition: String(v.condition),
        trueLabel: String(v.trueLabel),
        falseLabel: String(v.falseLabel),
        activeBranch: ['true', 'false', 'none'].includes(v.activeBranch)
          ? v.activeBranch
          : 'none',
      }

    case 'comparison':
      if (!Array.isArray(v.leftItems) || !Array.isArray(v.rightItems)) return undefined
      return {
        type: 'comparison',
        leftTitle: String(v.leftTitle || '方案 A'),
        leftItems: v.leftItems.map((x: any) => String(x)),
        rightTitle: String(v.rightTitle || '方案 B'),
        rightItems: v.rightItems.map((x: any) => String(x)),
        winner: ['left', 'right', 'none'].includes(v.winner) ? v.winner : 'none',
      }

    case 'highlight-card':
      if (!v.mainValue) return undefined
      return {
        type: 'highlight-card',
        mainValue: String(v.mainValue),
        label: v.label ? String(v.label) : undefined,
        tone: ['info', 'success', 'warn', 'danger'].includes(v.tone) ? v.tone : 'info',
      }

    case 'flow':
      if (!Array.isArray(v.boxes) || v.boxes.length === 0) return undefined
      return {
        type: 'flow',
        boxes: v.boxes.map((x: any) => String(x)),
        activeIndex:
          typeof v.activeIndex === 'number' && v.activeIndex >= 0 && v.activeIndex < v.boxes.length
            ? v.activeIndex
            : undefined,
        loopBack: Boolean(v.loopBack),
      }

    default:
      return undefined
  }
}

export const validateConceptAnim = (data: ConceptAnimPayload) => {
  const errors: string[] = []

  if (!data.mainTerm || typeof data.mainTerm !== 'string') {
    errors.push('概念模板缺少 mainTerm。')
  }

  if (!data.coreIdea || typeof data.coreIdea !== 'string') {
    errors.push('概念模板缺少 coreIdea。')
  }

  if (!Array.isArray(data.steps) || data.steps.length < 3) {
    errors.push('概念模板 steps 至少需要 3 步。')
    return errors
  }

  data.steps.forEach((step, index) => {
    normalizeBaseStepFields(step)

    if (!step.title || typeof step.title !== 'string') {
      errors.push(`第 ${index + 1} 步缺少 title。`)
    }
    if (!step.desc || typeof step.desc !== 'string') {
      errors.push(`第 ${index + 1} 步缺少 desc。`)
    }
    if (step.keyPoints !== undefined && step.keyPoints !== null) {
      if (!Array.isArray(step.keyPoints)) {
        errors.push(`第 ${index + 1} 步的 keyPoints 非法,应该是字符串数组。`)
      }
    }
    // visual 是宽容校验:非法直接清理,不作为错误
    if (step.visual !== undefined && step.visual !== null) {
      const cleaned = sanitizeConceptVisual(step.visual)
      step.visual = cleaned // 原地改写,丢弃非法的 visual
    }
  })

  return errors
}

export const validateAnimJson = (data: unknown): AnimValidationResult => {
  const errors: string[] = []

  if (!data || typeof data !== 'object') {
    return { valid: false, errors: ['返回结果不是对象。'] }
  }

  const payload = data as Record<string, any>

  if (
    !payload.templateType ||
    !['sort', 'protocol', 'stack', 'queue', 'tree', 'graph', 'concept'].includes(payload.templateType)
  ) {
    errors.push('templateType 必须为 sort、protocol、stack、queue、tree、graph 或 concept。')
  }

  if (!payload.title || typeof payload.title !== 'string') {
    errors.push('缺少 title。')
  }

  if (!payload.subtitle || typeof payload.subtitle !== 'string') {
    errors.push('缺少 subtitle。')
  }

  if (!payload.targetGroup || typeof payload.targetGroup !== 'string') {
    errors.push('缺少 targetGroup。')
  }

  if (!payload.teachingGoal || typeof payload.teachingGoal !== 'string') {
    errors.push('缺少 teachingGoal。')
  }

  if (!Array.isArray(payload.steps) || payload.steps.length < 3) {
    errors.push('steps 至少需要 3 步。')
  }

  if (errors.length > 0) {
    return { valid: false, errors }
  }

  if (payload.templateType === 'sort') {
    const sortErrors = validateSortAnim(payload as SortAnimPayload)
    return { valid: sortErrors.length === 0, errors: sortErrors }
  }

  if (payload.templateType === 'protocol') {
    const protocolErrors = validateProtocolAnim(payload as ProtocolAnimPayload)
    return { valid: protocolErrors.length === 0, errors: protocolErrors }
  }

  if (payload.templateType === 'stack') {
    const stackErrors = validateStackAnim(payload as StackAnimPayload)
    return { valid: stackErrors.length === 0, errors: stackErrors }
  }

  if (payload.templateType === 'queue') {
    const queueErrors = validateQueueAnim(payload as QueueAnimPayload)
    return { valid: queueErrors.length === 0, errors: queueErrors }
  }

  if (payload.templateType === 'tree') {
    const treeErrors = validateTreeAnim(payload as TreeAnimPayload)
    return { valid: treeErrors.length === 0, errors: treeErrors }
  }

  if (payload.templateType === 'graph') {
    const graphErrors = validateGraphAnim(payload as GraphAnimPayload)
    return { valid: graphErrors.length === 0, errors: graphErrors }
  }

  const conceptErrors = validateConceptAnim(payload as ConceptAnimPayload)
  return { valid: conceptErrors.length === 0, errors: conceptErrors }
}
