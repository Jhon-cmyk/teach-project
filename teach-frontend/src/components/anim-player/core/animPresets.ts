import type {
  AnimBaseStep,
  AnimMotionType,
  AnimPayload,
  ConceptAnimPayload,
  ConceptVisual,
  GraphAnimPayload,
  PresetBuildOptions,
  ProtocolAnimPayload,
  QueueAnimPayload,
  SortAnimPayload,
  StackAnimPayload,
  TreeAnimPayload,
} from './animTypes.ts'

const withDefaults = (options: PresetBuildOptions = {}) => ({
  targetGroup: options.targetGroup || '本科一年级',
  teachingGoal: options.teachingGoal || '',
})

const trimStepText = (text: unknown, limit = 40) => {
  const value = String(text || '').replace(/\s+/g, ' ').trim()
  return value.length > limit ? `${value.slice(0, limit - 1)}…` : value
}

const ensureBaseFlow = (step: AnimBaseStep, fallbackCaption: string) => {
  if (!step.stageCaption) {
    step.stageCaption = trimStepText(fallbackCaption || step.title || step.desc, 28)
  }
  step.desc = trimStepText(step.desc || step.stageCaption, 40)
}

const inferConceptMotion = (
  visual: ConceptVisual | undefined,
  index: number,
  isLast: boolean,
): AnimBaseStep['motion'] => {
  if (isLast) return { type: 'done' }
  if (!visual) return { type: index === 0 ? 'observe' : 'flow' }

  if (visual.type === 'nodes-chain') {
    if (visual.action === 'insert') {
      return {
        type: 'insert-node',
        toIndex: visual.actionIndex,
        value: visual.actionValue,
      }
    }
    if (visual.action === 'delete') {
      return { type: 'delete-node', fromIndex: visual.actionIndex }
    }
    if (visual.action === 'access' || visual.action === 'search' || visual.highlight?.length) {
      return { type: 'visit', indexes: visual.highlight || [], toIndex: visual.actionIndex }
    }
  }

  if (visual.type === 'tree') {
    return { type: 'visit', path: visual.path || visual.highlight || [] }
  }

  if (visual.type === 'flow') {
    return { type: 'flow', toIndex: visual.activeIndex }
  }

  if (visual.type === 'branching') {
    return { type: 'branch', branch: visual.activeBranch || 'none' }
  }

  return { type: index === 0 ? 'observe' : 'flow' }
}

export const withAnimationFlowHints = <T extends AnimPayload>(payload: T): T => {
  payload.steps.forEach((step: any, index: number) => {
    const isLast = index === payload.steps.length - 1

    if (payload.templateType === 'sort') {
      const swap = Array.isArray(step.swap) ? step.swap : []
      const highlight = Array.isArray(step.highlight) ? step.highlight : []
      const type: AnimMotionType =
        isLast ? 'done' : swap.length === 2 ? 'swap' : highlight.length >= 2 ? 'compare' : 'observe'
      step.motion = step.motion || {
        type,
        indexes: highlight,
        fromIndex: swap[0] ?? highlight[0],
        toIndex: swap[1] ?? highlight[1],
      }
      ensureBaseFlow(step, type === 'swap' ? '交换相邻元素' : type === 'compare' ? '比较两个元素' : step.title)
      return
    }

    if (payload.templateType === 'protocol') {
      step.motion = step.motion || {
        type: isLast ? 'done' : 'send',
        from: step.from,
        to: step.to,
        value: step.message,
      }
      ensureBaseFlow(step, step.message || step.title)
      return
    }

    if (payload.templateType === 'stack') {
      const operation = step.operation === 'init' ? 'observe' : step.operation
      step.motion = step.motion || {
        type: isLast ? 'done' : operation,
        value: step.poppedValue ?? step.activeValue,
      }
      ensureBaseFlow(step, step.activeValue || step.poppedValue || step.title)
      return
    }

    if (payload.templateType === 'queue') {
      const operation = step.operation === 'init' ? 'observe' : step.operation
      step.motion = step.motion || {
        type: isLast ? 'done' : operation,
        value: step.removedValue ?? step.activeValue,
      }
      ensureBaseFlow(step, step.activeValue || step.removedValue || step.title)
      return
    }

    if (payload.templateType === 'tree') {
      step.motion = step.motion || {
        type: isLast ? 'done' : 'visit',
        value: step.currentNode,
        path: step.path || [],
      }
      ensureBaseFlow(step, step.currentNode ? `关注节点 ${step.currentNode}` : step.title)
      return
    }

    if (payload.templateType === 'graph') {
      step.motion = step.motion || {
        type: isLast ? 'done' : step.operation === 'enqueue' || step.operation === 'dequeue' ? step.operation : 'visit',
        value: step.activeNode,
      }
      ensureBaseFlow(step, step.activeNode ? `访问节点 ${step.activeNode}` : step.title)
      return
    }

    step.motion = step.motion || inferConceptMotion(step.visual, index, isLast)
    ensureBaseFlow(step, step.visualHint || step.focus || step.title)
  })

  return payload
}

/* ==================== 排序类 preset ==================== */

export const buildBubbleSortPreset = (options: PresetBuildOptions = {}): SortAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'sort',
    title: '冒泡排序算法分步推演',
    subtitle: '通过相邻元素比较与交换,将最大元素逐步移动到末尾',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解冒泡排序如何逐轮把最大值移动到末尾',
    initialData: [5, 1, 4, 2, 8],
    steps: [
      { title: '初始数组', desc: '先观察整个数组。冒泡排序每一轮都会从左到右比较相邻两个元素。', array: [5, 1, 4, 2, 8], highlight: [0, 1], swap: [], sortedTailStart: null },
      { title: '比较 5 和 1', desc: '5 比 1 大,所以交换它们。较大的数会往右侧移动一步。', array: [1, 5, 4, 2, 8], highlight: [0, 1], swap: [0, 1], sortedTailStart: null },
      { title: '比较 5 和 4', desc: '现在比较新的相邻元素 5 和 4。因为 5 仍然更大,所以继续交换。', array: [1, 4, 5, 2, 8], highlight: [1, 2], swap: [1, 2], sortedTailStart: null },
      { title: '比较 5 和 2', desc: '5 比 2 大,再交换一次。可以看到 5 正在不断向右移动。', array: [1, 4, 2, 5, 8], highlight: [2, 3], swap: [2, 3], sortedTailStart: null },
      { title: '比较 5 和 8', desc: '5 不大于 8,所以这一对不需要交换。第一轮结束后,最大值 8 已经在末尾。', array: [1, 4, 2, 5, 8], highlight: [3, 4], swap: [], sortedTailStart: 4 },
      { title: '第二轮继续冒泡', desc: '从头再来一轮,只需要处理未排序区域。4 和 2 交换后,较大的 5 继续留在右侧。', array: [1, 2, 4, 5, 8], highlight: [1, 2], swap: [1, 2], sortedTailStart: 3 },
      { title: '排序完成', desc: '经过多轮比较与交换,数组已经从小到大排好序。冒泡排序的核心就是相邻比较、逐步把大数推到右边。', array: [1, 2, 4, 5, 8], highlight: [0, 1], swap: [], sortedTailStart: 0 },
    ],
  }
}

export const buildSelectionSortPreset = (options: PresetBuildOptions = {}): SortAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'sort',
    title: '选择排序算法分步推演',
    subtitle: '每一轮从未排序区中找出最小值,放到最前面',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解选择排序如何逐轮选出最小值',
    initialData: [64, 25, 12, 22, 11],
    steps: [
      { title: '初始数组', desc: '先把整个数组看成未排序区。选择排序会在每一轮寻找当前最小值。', array: [64, 25, 12, 22, 11], highlight: [0], swap: [], sortedTailStart: null },
      { title: '找到最小值 11', desc: '第一轮扫描后,发现 11 是最小值,把它与第一个位置交换。', array: [11, 25, 12, 22, 64], highlight: [0, 4], swap: [0, 4], sortedTailStart: null },
      { title: '第二轮找最小值', desc: '在剩余未排序区中,12 是最小值,把它放到第二个位置。', array: [11, 12, 25, 22, 64], highlight: [1, 2], swap: [1, 2], sortedTailStart: null },
      { title: '第三轮继续选择', desc: '此时未排序区里最小的是 22,将它交换到前面。', array: [11, 12, 22, 25, 64], highlight: [2, 3], swap: [2, 3], sortedTailStart: null },
      { title: '排序完成', desc: '每一轮都把一个最小值放到正确位置,最终数组有序。', array: [11, 12, 22, 25, 64], highlight: [3, 4], swap: [], sortedTailStart: 0 },
    ],
  }
}

export const buildInsertionSortPreset = (options: PresetBuildOptions = {}): SortAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'sort',
    title: '插入排序算法分步推演',
    subtitle: '把当前元素插入到前面已经有序的序列中',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解插入排序如何逐步扩展有序区',
    initialData: [5, 2, 4, 6, 1],
    steps: [
      { title: '初始状态', desc: '默认第一个元素已经有序,后面的元素会依次插入到前面的有序区。', array: [5, 2, 4, 6, 1], highlight: [0, 1], swap: [], sortedTailStart: null },
      { title: '插入 2', desc: '2 比 5 小,所以 2 插入到 5 前面。', array: [2, 5, 4, 6, 1], highlight: [0, 1], swap: [0, 1], sortedTailStart: null },
      { title: '插入 4', desc: '4 应该放在 2 和 5 之间,于是向左移动并插入。', array: [2, 4, 5, 6, 1], highlight: [1, 2], swap: [1, 2], sortedTailStart: null },
      { title: '插入 6', desc: '6 比前面的元素都大,所以保持在当前位置。', array: [2, 4, 5, 6, 1], highlight: [2, 3], swap: [], sortedTailStart: null },
      { title: '插入 1', desc: '1 比前面所有元素都小,因此一路向左插入到最前面。', array: [1, 2, 4, 5, 6], highlight: [0, 4], swap: [0, 4], sortedTailStart: 0 },
      { title: '排序完成', desc: '插入排序每次只处理一个新元素,并把它插入到已经有序的区域中。', array: [1, 2, 4, 5, 6], highlight: [0, 1], swap: [], sortedTailStart: 0 },
    ],
  }
}

export const buildTcpHandshakePreset = (options: PresetBuildOptions = {}): ProtocolAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'protocol',
    title: 'TCP 三次握手',
    subtitle: '客户端与服务器通过三次报文交换建立可靠连接',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解客户端与服务器如何建立连接',
    actors: ['客户端', '服务器'],
    steps: [
      { title: '建立前的状态', desc: '在连接建立之前,服务器处于监听状态,等待客户端发起请求。', from: '客户端', to: '服务器', message: '准备发起连接', clientState: 'CLOSED', serverState: 'LISTEN', messageType: 'request' },
      { title: '第一次握手', desc: '客户端发送 SYN 报文,表示自己想建立连接,并给出自己的初始序列号。', from: '客户端', to: '服务器', message: 'SYN=1, seq=x', clientState: 'SYN-SENT', serverState: 'LISTEN', messageType: 'request' },
      { title: '第二次握手', desc: '服务器收到后,回复 SYN + ACK,说明我收到了你的请求,也同意建立连接。', from: '服务器', to: '客户端', message: 'SYN=1, ACK=1, seq=y, ack=x+1', clientState: 'SYN-SENT', serverState: 'SYN-RECEIVED', messageType: 'response' },
      { title: '第三次握手', desc: '客户端再发送 ACK,告诉服务器你的回复我也收到了。到这里双方都确认通信能力正常。', from: '客户端', to: '服务器', message: 'ACK=1, seq=x+1, ack=y+1', clientState: 'ESTABLISHED', serverState: 'ESTABLISHED', messageType: 'confirm' },
      { title: '连接建立完成', desc: '三次握手完成后,客户端和服务器都进入已建立连接状态,可以正式传输数据。', from: '客户端', to: '服务器', message: '连接可用,开始传输数据', clientState: 'ESTABLISHED', serverState: 'ESTABLISHED', messageType: 'confirm' },
    ],
  }
}

export const buildTcpClosePreset = (options: PresetBuildOptions = {}): ProtocolAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'protocol',
    title: 'TCP 四次挥手',
    subtitle: '通过四次报文交换安全关闭连接',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解 TCP 如何关闭连接',
    actors: ['客户端', '服务器'],
    steps: [
      { title: '客户端发起关闭', desc: '客户端先发 FIN,表示自己没有数据要发送了。', from: '客户端', to: '服务器', message: 'FIN=1, seq=u', clientState: 'FIN-WAIT-1', serverState: 'ESTABLISHED', messageType: 'close' },
      { title: '服务器确认收到', desc: '服务器回 ACK,说明已经收到关闭请求,但自己可能还有数据要发。', from: '服务器', to: '客户端', message: 'ACK=1, ack=u+1', clientState: 'FIN-WAIT-2', serverState: 'CLOSE-WAIT', messageType: 'response' },
      { title: '服务器准备关闭', desc: '等服务器的数据发送完成后,再主动发 FIN。', from: '服务器', to: '客户端', message: 'FIN=1, seq=v', clientState: 'FIN-WAIT-2', serverState: 'LAST-ACK', messageType: 'close' },
      { title: '客户端最终确认', desc: '客户端回复 ACK,服务器收到后连接关闭;客户端等待一段时间后也关闭。', from: '客户端', to: '服务器', message: 'ACK=1, ack=v+1', clientState: 'TIME-WAIT', serverState: 'CLOSED', messageType: 'confirm' },
      { title: '连接关闭完成', desc: '四次挥手结束后,双方都完成断开连接。', from: '客户端', to: '服务器', message: '连接已关闭', clientState: 'CLOSED', serverState: 'CLOSED', messageType: 'confirm' },
    ],
  }
}

export const buildHttpFlowPreset = (options: PresetBuildOptions = {}): ProtocolAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'protocol',
    title: 'HTTP 请求流程',
    subtitle: '客户端发请求,服务器返回响应',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解一次 HTTP 请求的基本流程',
    actors: ['浏览器', '服务器'],
    steps: [
      { title: '发起请求前', desc: '浏览器准备访问资源,服务器等待处理请求。', from: '浏览器', to: '服务器', message: '准备请求资源', clientState: 'READY', serverState: 'WAITING', messageType: 'request' },
      { title: '浏览器发送请求', desc: '浏览器向服务器发送 HTTP 请求,请求页面或接口数据。', from: '浏览器', to: '服务器', message: 'GET /index.html HTTP/1.1', clientState: 'REQUEST-SENT', serverState: 'RECEIVING', messageType: 'request' },
      { title: '服务器处理请求', desc: '服务器解析请求,查找资源或执行业务逻辑。', from: '服务器', to: '浏览器', message: '处理请求中...', clientState: 'WAITING', serverState: 'PROCESSING', messageType: 'response' },
      { title: '服务器返回响应', desc: '服务器将状态码、响应头和响应体返回给浏览器。', from: '服务器', to: '浏览器', message: '200 OK + HTML / JSON', clientState: 'RENDERING', serverState: 'RESPONDED', messageType: 'response' },
      { title: '浏览器渲染结果', desc: '浏览器收到响应后开始渲染页面或展示数据。', from: '服务器', to: '浏览器', message: '页面展示完成', clientState: 'DONE', serverState: 'IDLE', messageType: 'confirm' },
    ],
  }
}

export const buildStackPreset = (options: PresetBuildOptions = {}): StackAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'stack',
    title: '栈的深入浅出',
    subtitle: '通过入栈与出栈理解后进先出的工作方式',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解栈的后进先出与栈顶操作',
    initialStack: [],
    steps: [
      { title: '空栈起步', desc: '先把栈看成一个只能从顶部操作的竖直容器。现在它还是空的。', stack: [], operation: 'init', activeValue: null, poppedValue: null },
      { title: '入栈 A', desc: '把 A 放入栈中。因为栈只能从顶部进出,所以 A 会先成为栈顶。', stack: ['A'], operation: 'push', activeValue: 'A', poppedValue: null },
      { title: '继续入栈 B', desc: '再把 B 放进去,B 会压在 A 的上面,此时 B 成为新的栈顶。', stack: ['A', 'B'], operation: 'push', activeValue: 'B', poppedValue: null },
      { title: '继续入栈 C', desc: '继续把 C 放入栈中。越后进入的元素,位置越靠近顶部。', stack: ['A', 'B', 'C'], operation: 'push', activeValue: 'C', poppedValue: null },
      { title: '查看栈顶', desc: '现在不取出元素,只观察顶部。可以看到当前最先能操作到的是 C。', stack: ['A', 'B', 'C'], operation: 'peek', activeValue: 'C', poppedValue: null },
      { title: '出栈 C', desc: '执行一次出栈,最先离开的不是最早进入的 A,而是最后进入的 C,这就是后进先出。', stack: ['A', 'B'], operation: 'pop', activeValue: 'B', poppedValue: 'C' },
      { title: '再出栈 B', desc: '继续从顶部取出,接下来离开的是 B。说明栈总是优先处理离顶部最近的元素。', stack: ['A'], operation: 'pop', activeValue: 'A', poppedValue: 'B' },
      { title: '栈结构总结', desc: '栈的核心规律是后进先出。入栈和出栈都只围绕栈顶进行。', stack: ['A'], operation: 'done', activeValue: 'A', poppedValue: null },
    ],
  }
}

export const buildQueuePreset = (options: PresetBuildOptions = {}): QueueAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'queue',
    title: '队列入队出队演示',
    subtitle: '从队尾进入,从队头离开,理解先进先出',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解队列的队头、队尾与先进先出规则',
    initialQueue: [],
    steps: [
      { title: '空队列起步', desc: '队列初始为空,元素只能从队尾进入。', queue: [], operation: 'init', activeValue: null, removedValue: null },
      { title: 'A 入队', desc: 'A 从队尾进入,同时也是当前队头。', queue: ['A'], operation: 'enqueue', activeValue: 'A', removedValue: null },
      { title: 'B 入队', desc: 'B 接在 A 后面,队头仍然是 A。', queue: ['A', 'B'], operation: 'enqueue', activeValue: 'B', removedValue: null },
      { title: 'C 入队', desc: 'C 从队尾进入,队列顺序变成 A、B、C。', queue: ['A', 'B', 'C'], operation: 'enqueue', activeValue: 'C', removedValue: null },
      { title: '查看队头', desc: '只查看队头不会移除元素,此时队头是 A。', queue: ['A', 'B', 'C'], operation: 'peek', activeValue: 'A', removedValue: null },
      { title: 'A 出队', desc: '最早进入的 A 最先离开,体现先进先出。', queue: ['B', 'C'], operation: 'dequeue', activeValue: 'B', removedValue: 'A' },
      { title: 'B 出队', desc: '接下来离开的是 B,队头移动到 C。', queue: ['C'], operation: 'dequeue', activeValue: 'C', removedValue: 'B' },
      { title: '队列规则总结', desc: '入队看队尾,出队看队头,顺序始终保持先进先出。', queue: ['C'], operation: 'done', activeValue: 'C', removedValue: null },
    ],
  }
}

export const buildBstSearchPreset = (options: PresetBuildOptions = {}): TreeAnimPayload => {
  const ctx = withDefaults(options)
  const root = {
    value: 7,
    left: {
      value: 3,
      left: { value: 1 },
      right: { value: 5 },
    },
    right: {
      value: 10,
      right: { value: 14 },
    },
  }
  return {
    templateType: 'tree',
    title: '二叉搜索树查找',
    subtitle: '沿着比较结果逐层缩小查找范围',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解 BST 如何通过大小比较选择左右子树',
    root,
    steps: [
      { title: '观察根节点', desc: '从根节点 7 开始查找目标值 5。', currentNode: 7, path: [7], visited: [7], operation: 'init' },
      { title: '比较 5 和 7', desc: '5 小于 7,下一步进入左子树。', currentNode: 7, path: [7], visited: [7], operation: 'compare' },
      { title: '转向左子树', desc: '来到节点 3,查找范围缩小到左侧分支。', currentNode: 3, path: [7, 3], visited: [7, 3], operation: 'go-left' },
      { title: '比较 5 和 3', desc: '5 大于 3,下一步进入 3 的右子树。', currentNode: 3, path: [7, 3], visited: [7, 3], operation: 'compare' },
      { title: '找到目标节点', desc: '访问节点 5,与目标值相等,查找成功。', currentNode: 5, path: [7, 3, 5], visited: [7, 3, 5], operation: 'visit' },
      { title: '查找完成', desc: 'BST 每次比较都能排除一半方向的子树。', currentNode: 5, path: [7, 3, 5], visited: [7, 3, 5], operation: 'done' },
    ],
  }
}

export const buildBfsGraphPreset = (options: PresetBuildOptions = {}): GraphAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'graph',
    title: 'BFS 图遍历',
    subtitle: '用队列按层访问图中的节点',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解 BFS 如何通过 frontier 逐层扩展',
    nodes: ['A', 'B', 'C', 'D', 'E', 'F'],
    edges: [
      { from: 'A', to: 'B' },
      { from: 'A', to: 'C' },
      { from: 'B', to: 'D' },
      { from: 'B', to: 'E' },
      { from: 'C', to: 'F' },
    ],
    steps: [
      { title: '从 A 开始', desc: '选择 A 作为起点,先把它放入 frontier。', activeNode: 'A', visited: [], frontier: ['A'], activeEdges: [], operation: 'init' },
      { title: '访问 A', desc: '取出 A 并访问,把 B、C 加入 frontier。', activeNode: 'A', visited: ['A'], frontier: ['B', 'C'], activeEdges: [{ from: 'A', to: 'B' }, { from: 'A', to: 'C' }], operation: 'visit' },
      { title: '访问 B', desc: '按队列顺序访问 B,发现 D、E。', activeNode: 'B', visited: ['A', 'B'], frontier: ['C', 'D', 'E'], activeEdges: [{ from: 'B', to: 'D' }, { from: 'B', to: 'E' }], operation: 'dequeue' },
      { title: '访问 C', desc: '继续访问 C,发现 F。', activeNode: 'C', visited: ['A', 'B', 'C'], frontier: ['D', 'E', 'F'], activeEdges: [{ from: 'C', to: 'F' }], operation: 'dequeue' },
      { title: '访问下一层', desc: 'D、E、F 属于下一层,会依次被访问。', activeNode: 'D', visited: ['A', 'B', 'C', 'D'], frontier: ['E', 'F'], activeEdges: [{ from: 'B', to: 'D' }], operation: 'visit' },
      { title: '遍历完成', desc: '所有可达节点都被访问,BFS 得到按层扩展的顺序。', activeNode: 'F', visited: ['A', 'B', 'C', 'D', 'E', 'F'], frontier: [], activeEdges: [], operation: 'done' },
    ],
  }
}

export const buildDijkstraGraphPreset = (options: PresetBuildOptions = {}): GraphAnimPayload => {
  const ctx = withDefaults(options)
  return {
    templateType: 'graph',
    title: 'Dijkstra 最短路径',
    subtitle: '每次选择当前距离最小的节点,逐步松弛相邻边',
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || '帮助学生理解最短路径算法中的距离更新和已确定集合',
    nodes: ['A', 'B', 'C', 'D', 'E'],
    edges: [
      { from: 'A', to: 'B', label: '2', directed: true },
      { from: 'A', to: 'C', label: '5', directed: true },
      { from: 'B', to: 'C', label: '1', directed: true },
      { from: 'B', to: 'D', label: '2', directed: true },
      { from: 'C', to: 'E', label: '3', directed: true },
      { from: 'D', to: 'E', label: '1', directed: true },
    ],
    steps: [
      { title: '初始化距离', desc: '源点 A 距离为 0,其他节点暂记为无穷大。', activeNode: 'A', visited: [], frontier: ['A'], activeEdges: [], operation: 'init' },
      { title: '确定 A', desc: '选择距离最小的 A,松弛 A 到 B、C 的边。', activeNode: 'A', visited: ['A'], frontier: ['B', 'C'], activeEdges: [{ from: 'A', to: 'B', directed: true }, { from: 'A', to: 'C', directed: true }], operation: 'relax' },
      { title: '确定 B', desc: 'B 的距离最小,继续松弛 B 到 C、D。', activeNode: 'B', visited: ['A', 'B'], frontier: ['C', 'D'], activeEdges: [{ from: 'B', to: 'C', directed: true }, { from: 'B', to: 'D', directed: true }], operation: 'relax' },
      { title: '更新 C 与 D', desc: '通过 B 到 C 更短,D 也得到新的候选距离。', activeNode: 'C', visited: ['A', 'B', 'C'], frontier: ['D', 'E'], activeEdges: [{ from: 'C', to: 'E', directed: true }], operation: 'relax' },
      { title: '确定 D', desc: '选择 D 后松弛 D 到 E,E 的最短距离被更新。', activeNode: 'D', visited: ['A', 'B', 'C', 'D'], frontier: ['E'], activeEdges: [{ from: 'D', to: 'E', directed: true }], operation: 'relax' },
      { title: '最短路径完成', desc: '所有关键节点距离确定,得到从 A 出发的最短路径结果。', activeNode: 'E', visited: ['A', 'B', 'C', 'D', 'E'], frontier: [], activeEdges: [], operation: 'done' },
    ],
  }
}

/* ==================== 通用概念兜底(带 visual) ==================== */

/**
 * 根据概念名,自动判断应该给兜底 preset 挑选什么视觉原语
 */
const buildVisualForConcept = (concept: string): Array<any> => {
  const c = concept.trim()

  // 二叉树 / 树
  if (/二叉树|BST|搜索树|红黑树|AVL|堆|tree/i.test(c)) {
    const tree = {
      value: 7,
      left: {
        value: 3,
        left: { value: 1 },
        right: { value: 5 },
      },
      right: {
        value: 10,
        right: { value: 14 },
      },
    }
    return [
      { type: 'highlight-card', label: '核心定义', mainValue: `${c} 是一种层级结构`, tone: 'info' },
      { type: 'tree', root: tree, highlight: [7] },
      { type: 'tree', root: tree, path: [7, 3, 5], highlight: [5] },
      { type: 'tree', root: tree, path: [7, 10, 14], highlight: [14] },
      { type: 'highlight-card', label: '复杂度', mainValue: '平均 O(log n)', tone: 'success' },
    ]
  }

  // 线性表 / 数组 / 顺序表
  if (/线性表|顺序表|数组/.test(c)) {
    return [
      { type: 'highlight-card', label: '核心定义', mainValue: `${c}:同类元素的线性排列`, tone: 'info' },
      { type: 'nodes-chain', nodes: ['a1', 'a2', 'a3', 'a4', 'a5'], linked: false, highlight: [0, 4] },
      {
        type: 'comparison',
        leftTitle: '顺序存储(数组)',
        leftItems: ['内存连续', '访问 O(1)', '插删 O(n)'],
        rightTitle: '链式存储(链表)',
        rightItems: ['内存离散', '访问 O(n)', '插删 O(1)'],
        winner: 'none',
      },
      { type: 'nodes-chain', nodes: ['a1', 'a2', 'X', 'a3', 'a4'], linked: false, highlight: [2], action: 'insert', actionIndex: 2, actionValue: 'X' },
      { type: 'highlight-card', label: '提醒', mainValue: '线性表 ≠ 数组', tone: 'warn' },
    ]
  }

  // 链表
  if (/链表/.test(c)) {
    return [
      { type: 'highlight-card', label: '核心定义', mainValue: `${c}:通过指针串起来的节点`, tone: 'info' },
      { type: 'nodes-chain', nodes: ['A', 'B', 'C', 'D'], linked: true, highlight: [0] },
      { type: 'nodes-chain', nodes: ['A', 'X', 'B', 'C', 'D'], linked: true, highlight: [1], action: 'insert', actionIndex: 1, actionValue: 'X' },
      { type: 'nodes-chain', nodes: ['A', 'B', 'D'], linked: true, highlight: [2], action: 'delete', actionIndex: 2 },
      { type: 'highlight-card', label: '插入/删除', mainValue: 'O(1)', tone: 'success' },
    ]
  }

  // 队列
  if (/队列/.test(c)) {
    return [
      { type: 'highlight-card', label: '核心性质', mainValue: '先进先出 FIFO', tone: 'info' },
      { type: 'nodes-chain', nodes: ['A', 'B', 'C'], linked: false, highlight: [0] },
      { type: 'nodes-chain', nodes: ['A', 'B', 'C', 'D'], linked: false, highlight: [3], action: 'insert', actionIndex: 3, actionValue: 'D' },
      { type: 'nodes-chain', nodes: ['B', 'C', 'D'], linked: false, highlight: [0], action: 'delete', actionIndex: 0 },
      { type: 'highlight-card', label: '特点', mainValue: '队头出,队尾进', tone: 'success' },
    ]
  }

  // if / 分支
  if (/if|else|switch|条件语句|分支/i.test(c)) {
    return [
      { type: 'highlight-card', label: '核心用途', mainValue: `${c}:让程序根据条件选择路径`, tone: 'info' },
      { type: 'branching', condition: 'x > 0', trueLabel: '执行 A 分支', falseLabel: '执行 B 分支', activeBranch: 'none' },
      { type: 'branching', condition: 'x > 0', trueLabel: '执行 A 分支', falseLabel: '执行 B 分支', activeBranch: 'true' },
      { type: 'branching', condition: 'x > 0', trueLabel: '执行 A 分支', falseLabel: '执行 B 分支', activeBranch: 'false' },
      { type: 'highlight-card', label: '要点', mainValue: '任何时候只走一条路径', tone: 'success' },
    ]
  }

  // 循环
  if (/循环|for|while/i.test(c)) {
    return [
      { type: 'highlight-card', label: '用途', mainValue: '重复执行一段代码', tone: 'info' },
      { type: 'flow', boxes: ['初始化 i=0', '判断 i<n', '执行循环体', 'i++'], activeIndex: 0, loopBack: true },
      { type: 'flow', boxes: ['初始化 i=0', '判断 i<n', '执行循环体', 'i++'], activeIndex: 2, loopBack: true },
      { type: 'branching', condition: 'i < n', trueLabel: '继续循环', falseLabel: '退出循环', activeBranch: 'false' },
      { type: 'highlight-card', label: '要点', mainValue: '必须有退出条件', tone: 'warn' },
    ]
  }

  // 递归
  if (/递归|recursion/i.test(c)) {
    const recTree = {
      value: 'f(4)',
      left: { value: 'f(3)', left: { value: 'f(2)', left: { value: 'f(1)' } } },
      right: { value: 'f(2)' },
    }
    return [
      { type: 'highlight-card', label: '核心', mainValue: '函数调用自己', tone: 'info' },
      { type: 'tree', root: recTree, highlight: ['f(1)'] },
      { type: 'flow', boxes: ['判断终止条件', '调用更小子问题', '合并结果', '返回'], activeIndex: 1, loopBack: false },
      { type: 'highlight-card', label: '必须要素', mainValue: '终止条件 + 递推关系', tone: 'warn' },
      { type: 'highlight-card', label: '典型应用', mainValue: '阶乘 / 斐波那契 / 树遍历', tone: 'success' },
    ]
  }

  // 哈希表
  if (/哈希|hash|散列|map|字典/i.test(c)) {
    return [
      { type: 'highlight-card', label: '核心思想', mainValue: 'key → hash → index', tone: 'info' },
      { type: 'flow', boxes: ['输入 key', 'hash 函数', '得到 index', '存入/读取槽位'], activeIndex: 2 },
      { type: 'nodes-chain', nodes: ['[0]', '[1] apple', '[2]', '[3] banana', '[4]'], linked: false, highlight: [1, 3] },
      { type: 'highlight-card', label: '平均复杂度', mainValue: 'O(1)', tone: 'success' },
      { type: 'highlight-card', label: '提醒', mainValue: '哈希冲突要处理', tone: 'warn' },
    ]
  }

  // 默认:只用 highlight-card 保底
  return [
    { type: 'highlight-card', label: '核心定义', mainValue: c, tone: 'info' },
    undefined,
    undefined,
    undefined,
    { type: 'highlight-card', label: '小结', mainValue: `${c} 的关键要点`, tone: 'success' },
  ]
}

export const buildConceptFallbackPreset = (
  concept: string,
  options: PresetBuildOptions = {},
): ConceptAnimPayload => {
  const ctx = withDefaults(options)
  const safe = concept.trim() || '核心概念'
  const visuals = buildVisualForConcept(safe)

  return {
    templateType: 'concept',
    title: `${safe} · 概念讲解`,
    subtitle: `通过分步可视化讲解 ${safe} 的核心要点`,
    targetGroup: ctx.targetGroup,
    teachingGoal: ctx.teachingGoal || `帮助学生建立对 ${safe} 的整体认识与关键理解`,
    mainTerm: safe,
    coreIdea: `${safe} 是课程中的一个重要概念,我们将分步骤可视化它的关键特征与典型用法。`,
    steps: [
      {
        title: `什么是 ${safe}`,
        desc: `先从定义出发,理解 ${safe} 在本课程语境下指什么、解决什么问题。`,
        focus: '定义',
        keyPoints: ['它是什么', '用来解决什么问题', '常见应用场景'],
        visualHint: '起点',
        visual: visuals[0],
      },
      {
        title: `${safe} 的结构/特征`,
        desc: `直观地看到 ${safe} 的结构,建立最小可用的"心智模型"。`,
        focus: '结构特征',
        keyPoints: ['核心组成', '关键关系', '与相似概念的区别'],
        visualHint: '要点',
        visual: visuals[1],
      },
      {
        title: `${safe} 的工作方式`,
        desc: `用一个典型的例子,演示 ${safe} 在实际中是怎么一步一步运作的。`,
        focus: '工作流程',
        keyPoints: ['输入', '中间过程', '输出'],
        visualHint: '流程',
        visual: visuals[2],
      },
      {
        title: `${safe} 的常见误区`,
        desc: `整理学生最容易搞混的地方,帮助提前避坑。`,
        focus: '易错点',
        keyPoints: ['容易和谁混', '边界条件', '实现时最常见的 bug'],
        visualHint: '提醒',
        visual: visuals[3],
      },
      {
        title: `小结与应用`,
        desc: `把前面几步合在一起,形成对 ${safe} 的完整认知,并指出它在后续课程中的延伸。`,
        focus: '总结',
        keyPoints: ['核心一句话', '适用场景', '后续课程的连接'],
        visualHint: '结束',
        visual: visuals[4],
      },
    ],
  }
}

export const resolvePresetByConcept = (
  concept: string,
  options: PresetBuildOptions = {},
): AnimPayload | null => {
  const raw = (concept || '').trim()
  if (!raw) return null

  const normalized = raw.replace(/\s+/g, '').toLowerCase()

  const isPureStack = /^(栈|堆栈|stack)$/i.test(raw) || /^(栈|堆栈)(结构|的.*)?$/.test(raw)
  const isPureQueue = /队列|queue|FIFO|先进先出|入队|出队|循环队列/i.test(raw)
  const isTreeScene = /二叉树|二叉搜索树|BST|搜索树|树遍历|前序|中序|后序|堆结构/i.test(raw)
  const isShortestPathScene = /最短路径|Dijkstra/i.test(raw)
  const isGraphScene = /图遍历|BFS|DFS|拓扑排序|有向图|无向图|邻接/i.test(raw)
  const isPureBubble = /^冒泡排序/.test(raw)
  const isPureSelection = /^选择排序/.test(raw)
  const isPureInsertion = /^插入排序/.test(raw)
  const isPureTcpHandshake = /^(tcp)?\s*三次握手/i.test(raw) || /^tcp三次握手/i.test(normalized)
  const isPureTcpClose = /^(tcp)?\s*四次挥手/i.test(raw) || /^tcp四次挥手/i.test(normalized)
  const isPureHttp = /^http(请求流程)?$/i.test(normalized)

  if (isPureBubble) return withAnimationFlowHints(buildBubbleSortPreset(options))
  if (isPureSelection) return withAnimationFlowHints(buildSelectionSortPreset(options))
  if (isPureInsertion) return withAnimationFlowHints(buildInsertionSortPreset(options))
  if (isPureStack) return withAnimationFlowHints(buildStackPreset(options))
  if (isPureQueue) return withAnimationFlowHints(buildQueuePreset(options))
  if (isTreeScene) return withAnimationFlowHints(buildBstSearchPreset(options))
  if (isShortestPathScene) return withAnimationFlowHints(buildDijkstraGraphPreset(options))
  if (isGraphScene) return withAnimationFlowHints(buildBfsGraphPreset(options))
  if (isPureTcpHandshake) return withAnimationFlowHints(buildTcpHandshakePreset(options))
  if (isPureTcpClose) return withAnimationFlowHints(buildTcpClosePreset(options))
  if (isPureHttp) return withAnimationFlowHints(buildHttpFlowPreset(options))

  return null
}

export const resolveFallbackPayload = (
  concept: string,
  options: PresetBuildOptions = {},
): AnimPayload => {
  return resolvePresetByConcept(concept, options) || withAnimationFlowHints(buildConceptFallbackPreset(concept, options))
}
