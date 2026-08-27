import { computed, ref } from 'vue'
import type { Ref } from 'vue'
import type {
  AnimFormModel,
  AnimOptimizeAction,
  AnimPayload,
  AnimRenderStatus,
} from '../core/animTypes.ts'
import { cloneAnimData } from '../core/animTypes'
import {
  resolvePresetByConcept,
  resolveFallbackPayload,
  buildConceptFallbackPreset,
  withAnimationFlowHints,
} from '../core/animPresets'
import { safeParseAnimJson, validateAnimJson } from '../core/animValidator'
import { getAnimTemplateMeta, getPayloadTemplateType } from '../core/animRegistry.ts'

export interface AnimNotifyApi {
  success?: (message: string) => void
  error?: (message: string) => void
  warning?: (message: string) => void
}

export type StreamTextFn = (
  question: string,
  type: string,
  onChunk: (chunk: string) => void,
) => Promise<void>

export type StreamPrepareAgentFn = (
  agentType: 'anim' | 'anim_repair' | 'anim_optimize',
  form: Record<string, any>,
  onChunk: (chunk: string) => void,
  options?: { sourceContent?: string },
) => Promise<void>

export interface UseAnimEngineOptions {
  streamText: StreamTextFn
  streamPrepareAgent?: StreamPrepareAgentFn
  notify?: AnimNotifyApi
  initialAutoPlayInterval?: number
}

export interface UseAnimEngineResult {
  animJsonResult: Ref<AnimPayload | null>
  animRenderStatus: Ref<AnimRenderStatus>
  animValidationErrors: Ref<string[]>
  isAnimGenerating: Ref<boolean>
  isAnimOptimizing: Ref<boolean>
  currentAnimOptimizeAction: Ref<AnimOptimizeAction | ''>
  animAutoPlayInterval: Ref<number>
  currentTemplateType: Ref<ReturnType<typeof getPayloadTemplateType>>
  currentTemplateMeta: Ref<ReturnType<typeof getAnimTemplateMeta>>
  generateAnimation: (form: AnimFormModel) => Promise<boolean>
  optimizeAnimation: (action: AnimOptimizeAction, form: AnimFormModel) => Promise<boolean>
  resetAnimEngine: () => void
  applyAnimResult: (data: AnimPayload, status?: AnimRenderStatus) => boolean
  getCurrentJsonText: () => string
}

const DEFAULT_AUTOPLAY_INTERVAL = 1800

export const useAnimEngine = (options: UseAnimEngineOptions): UseAnimEngineResult => {
  const {
    streamText,
    streamPrepareAgent,
    notify,
    initialAutoPlayInterval = DEFAULT_AUTOPLAY_INTERVAL,
  } = options

  const animJsonResult = ref<AnimPayload | null>(null)
  const animRenderStatus = ref<AnimRenderStatus>('idle')
  const animValidationErrors = ref<string[]>([])
  const isAnimGenerating = ref(false)
  const isAnimOptimizing = ref(false)
  const currentAnimOptimizeAction = ref<AnimOptimizeAction | ''>('')
  const animAutoPlayInterval = ref(initialAutoPlayInterval)

  const currentTemplateType = computed(() => getPayloadTemplateType(animJsonResult.value))
  const currentTemplateMeta = computed(() => getAnimTemplateMeta(currentTemplateType.value))

  const resetAnimEngine = () => {
    animJsonResult.value = null
    animRenderStatus.value = 'idle'
    animValidationErrors.value = []
    currentAnimOptimizeAction.value = ''
    animAutoPlayInterval.value = initialAutoPlayInterval
  }

  const getConceptTypeLabel = (type: AnimFormModel['conceptType'] = 'auto') => {
    const labels: Record<AnimFormModel['conceptType'], string> = {
      auto: '自动识别',
      algorithm: '算法过程',
      protocol: '协议时序',
      'data-structure': '数据结构',
      concept: '通用概念',
    }
    return labels[type] || labels.auto
  }

  const inferAnimSceneHint = (concept: string) => {
    const c = concept.trim()

    if (/(冒泡|选择|插入|快速|归并|希尔|堆)排序/.test(c)) {
      return 'sort 模板:明确是数组比较/交换的线性排序过程。initialData 必须是数字数组,steps[].array 长度须一致。'
    }

    if (/^(栈|堆栈|stack)$/i.test(c) || /^栈结构/.test(c) || /栈的(入栈|出栈|后进先出)/.test(c)) {
      return 'stack 模板:必须是栈本身作为核心概念。突出栈顶、入栈、出栈、后进先出。'
    }

    if (/(队列|queue|FIFO|先进先出|入队|出队|循环队列)/i.test(c)) {
      return 'queue 模板:必须展示队头、队尾、入队、出队和先进先出。initialQueue 表示初始队列,steps[].queue 表示当前队列。'
    }

    if (/(tcp|udp|http|https|websocket|tls|ssl)/i.test(c) ||
      /(三次握手|四次挥手|握手|挥手)/.test(c) ||
      /(请求[与和]?响应|请求流程|客户端[与和]?服务器|rpc|客户端\/服务器)/i.test(c)) {
      return 'protocol 模板:必须是明确的双方(客户端/服务器,或类似两方)时序通信。actors 固定 2 个,每步有 from、to、message。'
    }

    if (/(二叉树|B[\+\-]?树|红黑树|AVL树|搜索树|字典树|前缀树|哈夫曼树|堆|森林)/.test(c)) {
      return 'tree 模板:用于二叉树/BST/堆/树遍历。root 必须使用 value/left/right 递归结构,steps 要展示 currentNode、path、visited 和操作方向。'
    }
    if (/(图\b|有向图|无向图|邻接|DFS|BFS|最短路径|Dijkstra|拓扑排序)/i.test(c)) {
      return 'graph 模板:用于图结构、BFS、DFS、最短路径和拓扑关系。nodes/edges 必须自洽,steps 要展示 activeNode、visited、frontier、activeEdges。'
    }

    // concept 模板下的视觉原语推荐
    if (/(链表|单链表|双链表|循环链表)/.test(c)) {
      return 'concept 模板 + visual 用 "nodes-chain" 原语,linked=true,让节点之间有箭头连接,真正画出链表。'
    }
    if (/(队列|双端队列|循环队列)/.test(c)) {
      return 'concept 模板 + visual 用 "nodes-chain" 原语,linked=false。入队/出队用 action 字段展示。'
    }
    if (/(线性表|顺序表|数组)/.test(c)) {
      return 'concept 模板 + visual 大量使用 "nodes-chain" 原语(linked=false)和 "comparison" 原语(对比顺序存储与链式存储)。插入/删除步骤要用 action 字段真正演示。'
    }
    if (/(散列|哈希表|hash|集合|map|字典|dict)/i.test(c)) {
      return 'concept 模板 + visual 混用 "comparison"(与线性查找对比)、"nodes-chain"(展示桶/槽)、"flow"(展示 hash→index→store 流程)。'
    }
    if (/(if|else|switch|条件语句|分支)/i.test(c)) {
      return 'concept 模板 + visual 用 "branching" 原语,至少要有一步展示 activeBranch=true,再一步展示 activeBranch=false,让学生看到两条路径。'
    }
    if (/(循环|for|while|do-while)/i.test(c)) {
      return 'concept 模板 + visual 用 "flow" 原语,boxes 展示"初始化→判断→执行→更新",loopBack=true 表示循环。配合 "branching" 展示退出条件。'
    }
    if (/(递归|函数调用)/i.test(c)) {
      return 'concept 模板 + visual 用 "tree" 原语画调用树,用 "flow" 展示调用/返回。'
    }

    return 'concept 模板优先。必须为每一步选一个合适的 visual 原语(除非真的只是纯概念定义)。禁止整份课件只有文字没有 visual。'
  }

  /* ==================== 视觉原语 schema ==================== */
  const visualSchemas = `
【视觉原语 visual(concept 模板专用,强烈建议每一步都带一个)】
每个 step 可选 visual 字段,type 必须是下面 6 种之一:

1) nodes-chain —— 一排节点,用于线性表/链表/数组/队列
{
  "type": "nodes-chain",
  "nodes": ["A", "B", "C", "D"],
  "linked": true,                 // 链表 true,数组/顺序表 false
  "highlight": [2],               // 高亮第 2 个节点
  "action": "insert",             // insert | delete | access | search | none
  "actionIndex": 2,               // 动作作用的位置
  "actionValue": "X"              // 动作相关的值(如插入的值)
}

2) tree —— 递归树结构,用于二叉树/BST/堆
{
  "type": "tree",
  "root": {
    "value": 7,
    "left":  { "value": 3, "left": { "value": 1 }, "right": { "value": 5 } },
    "right": { "value": 10, "right": { "value": 14 } }
  },
  "highlight": [5],               // 要高亮的节点值
  "path": [7, 3, 5]               // 查找路径(会以不同颜色标识)
}

3) branching —— 条件分支菱形,用于 if / switch
{
  "type": "branching",
  "condition": "x > 0",
  "trueLabel": "打印 正数",
  "falseLabel": "打印 非正数",
  "activeBranch": "true"          // "true" | "false" | "none"
}

4) comparison —— 左右对比,用于"两种方案比较"
{
  "type": "comparison",
  "leftTitle": "顺序存储",
  "leftItems": ["内存连续", "随机访问 O(1)", "插入删除慢 O(n)"],
  "rightTitle": "链式存储",
  "rightItems": ["内存离散", "随机访问 O(n)", "插入删除快 O(1)"],
  "winner": "none"                // "left" | "right" | "none"
}

5) highlight-card —— 一个强调大卡片,用于强调一个核心概念/值/公式
{
  "type": "highlight-card",
  "mainValue": "O(1)",
  "label": "访问时间复杂度",
  "tone": "success"               // "info" | "success" | "warn" | "danger"
}

6) flow —— 一排带箭头的流程框,用于算法步骤/循环
{
  "type": "flow",
  "boxes": ["初始化 i=0", "判断 i<n", "执行循环体", "i++"],
  "activeIndex": 2,
  "loopBack": true                // 有循环回溯
}

【visual 使用强制要求】
- concept 模板每一步都应该配一个 visual,优先使用最贴合该步内容的原语。
- 不同 step 的 visual 可以且应该不同,形成"定义卡→对比→动态演示→分支/流程→总结"的视觉节奏。
- 一份 5~7 步的 concept 课件,至少有 4 步要带 visual;只有纯"引言"或"总结寄语"步可以省略 visual。
- visual 数据必须真实贴合概念(如二叉树的 tree 结构要合理,不能乱写),不要生成与讲解无关的 visual。`

  const motionSchemas = `
【动画流程字段(所有模板 step 都可用,强烈建议每一步都带)】
每个 step 除 title/desc 外,还应该提供:
{
  "stageCaption": "图上短提示,最多一句话,不要超过 25 个汉字",
  "motion": {
    "type": "observe | compare | swap | send | push | pop | enqueue | dequeue | peek | visit | insert-node | delete-node | branch | call | return | flow | done",
    "indexes": [0, 1],             // compare/swap 用
    "fromIndex": 0,                // swap/insert/delete 可用
    "toIndex": 1,
    "value": "A",                  // push/pop/insert/call 等动作值
    "from": "客户端",              // send 用
    "to": "服务器",
    "path": [7, 3, 5],             // tree visit/call 路径用
    "branch": "true"               // branch 用:true | false | none
  }
}

【动画流程硬性要求】
- 先设计学生能看见的动作,再写一句 stageCaption; desc 只保留一句补充,不要写长段解释。
- sort:比较用 compare,交换用 swap,每步必须能从上一数组状态运动到当前数组状态。
- protocol:每个报文交换用 send,让 message 气泡从 from 飞到 to,状态在抵达后变化。
- stack:入栈 push、出栈 pop、查看栈顶 peek 必须有对应 motion。
- queue:入队 enqueue、出队 dequeue、查看队头 peek 必须有对应 motion。
- tree:查找/遍历用 visit,路径放在 path 中。
- graph:访问节点用 visit,入队/出队用 enqueue/dequeue,最短路径松弛用 flow 或 visit 配合 activeEdges。
- concept:nodes-chain 插入/删除用 insert-node/delete-node; tree 查找用 visit; flow 推进用 flow; branching 用 branch。
- 若某一步只是初始观察,使用 observe;最后一步使用 done。`

  const buildAnimJsonPrompt = (form: AnimFormModel) => {
    const decisionTree = `
【模板选择决策树 —— 严格按顺序判断】
1. 概念是否是"数组/序列上的比较与交换过程"(如冒泡/选择/插入/快速排序)?
   → 是,用 sort 模板。否则继续。
2. 概念是否是"两方之间明确的时序通信"(如 TCP 握手、HTTP 请求、客户端-服务器消息交换)?
   → 是,用 protocol 模板。否则继续。
3. 概念是否是"栈本身作为核心数据结构"(强调入栈、出栈、栈顶、LIFO)?
   → 是,用 stack 模板。否则继续。
4. 概念是否是"队列本身作为核心数据结构"(强调入队、出队、队头队尾、FIFO)?
   → 是,用 queue 模板。否则继续。
5. 概念是否是"树结构或树上过程"(如二叉树、BST 查找、树遍历、堆结构)?
   → 是,用 tree 模板。否则继续。
6. 概念是否是"图结构或图上算法"(如 BFS、DFS、最短路径、拓扑排序、邻接关系)?
   → 是,用 graph 模板。否则继续。
7. 其余所有情况(链表、哈希表、线性表、if 语句、循环、递归、面向对象、设计模式……)
   → 一律用 concept 模板,并且必须为每一步挑选合适的 visual 原语,让学生能"看到图",而不是"只读字"。`

    const schemaSort = `{
  "templateType": "sort",
  "title": "课件标题",
  "subtitle": "一句副标题",
  "targetGroup": "适用对象",
  "teachingGoal": "教学目标",
  "initialData": [5, 1, 4, 2, 8],
  "steps": [
    {
      "title": "步骤标题",
      "desc": "步骤说明",
      "stageCaption": "看图:第一个数比第二个大,交换位置",
      "motion": { "type": "swap", "indexes": [0, 1], "fromIndex": 0, "toIndex": 1 },
      "array": [5, 1, 4, 2, 8],
      "highlight": [0, 1],
      "swap": [0, 1],
      "sortedTailStart": null
    }
  ]
}`

    const schemaProtocol = `{
  "templateType": "protocol",
  "title": "课件标题",
  "subtitle": "一句副标题",
  "targetGroup": "适用对象",
  "teachingGoal": "教学目标",
  "actors": ["客户端", "服务器"],
  "steps": [
    {
      "title": "步骤标题",
      "desc": "步骤说明",
      "stageCaption": "SYN 从客户端飞向服务器",
      "motion": { "type": "send", "from": "客户端", "to": "服务器", "value": "SYN" },
      "from": "客户端",
      "to": "服务器",
      "message": "报文/请求内容",
      "clientState": "客户端状态",
      "serverState": "服务器状态",
      "messageType": "request | response | confirm | close"
    }
  ]
}`

    const schemaStack = `{
  "templateType": "stack",
  "title": "课件标题",
  "subtitle": "一句副标题",
  "targetGroup": "适用对象",
  "teachingGoal": "教学目标",
  "initialStack": [],
  "steps": [
    {
      "title": "步骤标题",
      "desc": "步骤说明",
      "stageCaption": "B 从栈顶压入",
      "motion": { "type": "push", "value": "B" },
      "stack": ["A", "B"],
      "operation": "init | push | pop | peek | done",
      "activeValue": "B",
      "poppedValue": null
    }
  ]
}`

    const schemaQueue = `{
  "templateType": "queue",
  "title": "课件标题",
  "subtitle": "一句副标题",
  "targetGroup": "适用对象",
  "teachingGoal": "教学目标",
  "initialQueue": [],
  "steps": [
    {
      "title": "步骤标题",
      "desc": "步骤说明",
      "stageCaption": "D 从队尾进入",
      "motion": { "type": "enqueue", "value": "D" },
      "queue": ["A", "B", "C", "D"],
      "operation": "init | enqueue | dequeue | peek | done",
      "activeValue": "D",
      "removedValue": null
    }
  ]
}`

    const schemaTree = `{
  "templateType": "tree",
  "title": "课件标题",
  "subtitle": "一句副标题",
  "targetGroup": "适用对象",
  "teachingGoal": "教学目标",
  "root": { "value": 7, "left": { "value": 3 }, "right": { "value": 10 } },
  "steps": [
    {
      "title": "步骤标题",
      "desc": "步骤说明",
      "stageCaption": "比较目标值与当前节点",
      "motion": { "type": "visit", "path": [7, 3, 5] },
      "currentNode": 3,
      "path": [7, 3],
      "visited": [7, 3],
      "operation": "init | visit | compare | go-left | go-right | backtrack | done"
    }
  ]
}`

    const schemaGraph = `{
  "templateType": "graph",
  "title": "课件标题",
  "subtitle": "一句副标题",
  "targetGroup": "适用对象",
  "teachingGoal": "教学目标",
  "nodes": ["A", "B", "C", "D"],
  "edges": [{ "from": "A", "to": "B", "directed": false }],
  "steps": [
    {
      "title": "步骤标题",
      "desc": "步骤说明",
      "stageCaption": "访问 A,把相邻节点加入 frontier",
      "motion": { "type": "visit", "value": "A" },
      "activeNode": "A",
      "visited": ["A"],
      "frontier": ["B", "C"],
      "activeEdges": [{ "from": "A", "to": "B" }],
      "operation": "init | visit | enqueue | dequeue | push | pop | relax | done"
    }
  ]
}`

    const schemaConcept = `{
  "templateType": "concept",
  "title": "课件标题(建议包含核心概念名)",
  "subtitle": "一句副标题",
  "targetGroup": "适用对象",
  "teachingGoal": "教学目标",
  "mainTerm": "核心概念名",
  "coreIdea": "一句最关键的话,放在整个课件头部",
  "steps": [
    {
      "title": "步骤标题",
      "desc": "一句补充说明,不要超过 40 个汉字",
      "stageCaption": "节点 X 插入到 A 和 B 中间",
      "motion": { "type": "insert-node", "fromIndex": 0, "toIndex": 1, "value": "X" },
      "focus": "本步关键词",
      "analogy": "可选类比",
      "keyPoints": ["要点1", "要点2"],
      "visualHint": "可选的视觉标签(起点/要点/流程/提醒/结束)",
      "codeSnippet": "可选代码片段",
      "visual": { "type": "..." , ... 见视觉原语章节 }
    }
  ]
}`

    const exampleLinear = `
【"线性表"完整参考示例(concept 模板 + visual)】
{
  "templateType": "concept",
  "title": "线性表 · 概念可视化讲解",
  "subtitle": "从一排方块开始理解顺序存储与链式存储",
  "targetGroup": "本科一年级",
  "teachingGoal": "帮助学生形象理解线性表的定义、存储方式、基本操作",
  "mainTerm": "线性表",
  "coreIdea": "线性表是 n 个相同类型元素按线性顺序排列,下一个元素唯一对应上一个元素。",
  "steps": [
    {
      "title": "定义:线性表是什么",
      "desc": "线性表是一种最基本的线性数据结构,元素按一条线依次排列。",
      "focus": "定义",
      "visual": { "type": "highlight-card", "label": "核心定义", "mainValue": "n 个同类型元素的线性序列", "tone": "info" }
    },
    {
      "title": "可视化:一排方块",
      "desc": "每个方块是一个元素,方块之间的顺序就是它们的线性关系。",
      "focus": "结构",
      "visual": { "type": "nodes-chain", "nodes": ["a1","a2","a3","a4","a5"], "linked": false, "highlight": [0,4] }
    },
    {
      "title": "两种存储:顺序 vs 链式",
      "desc": "线性表在内存中有两种实现方式。",
      "focus": "存储方式",
      "visual": {
        "type": "comparison",
        "leftTitle": "顺序存储(数组)",
        "leftItems": ["内存地址连续", "随机访问 O(1)", "插入/删除 O(n)"],
        "rightTitle": "链式存储(链表)",
        "rightItems": ["内存地址离散", "随机访问 O(n)", "插入/删除 O(1)"],
        "winner": "none"
      }
    },
    {
      "title": "插入操作",
      "desc": "在位置 2 插入新元素 X,其后的元素全部后移。",
      "focus": "插入",
      "visual": {
        "type": "nodes-chain",
        "nodes": ["a1","a2","X","a3","a4","a5"],
        "linked": false,
        "highlight": [2],
        "action": "insert",
        "actionIndex": 2,
        "actionValue": "X"
      }
    },
    {
      "title": "删除操作",
      "desc": "删除位置 3 的元素,后面元素全部前移。",
      "focus": "删除",
      "visual": {
        "type": "nodes-chain",
        "nodes": ["a1","a2","X","a4","a5"],
        "linked": false,
        "highlight": [3],
        "action": "delete",
        "actionIndex": 3
      }
    },
    {
      "title": "常见误区",
      "desc": "不要把'线性表'等同于'数组',它是抽象结构,实现可以是数组也可以是链表。",
      "focus": "易错点",
      "visualHint": "提醒",
      "visual": { "type": "highlight-card", "label": "提醒", "mainValue": "线性表 ≠ 数组", "tone": "warn" }
    }
  ]
}`

    const templateRules = [
      '【硬性规则】',
      '1. 严格按决策树选 templateType。',
      '2. concept 模板下必须为大多数步骤提供 visual 字段,让学生"看到图",不要只写文字。',
      '3. 每一步必须尽量提供 stageCaption 和 motion,让播放器能演出运动过程。',
      '4. visual 的数据必须真实合理,不能乱编(如二叉树要符合 BST 规则、链表步骤要连贯、对比项要成对)。',
      '5. 非排序概念禁止使用 sort;非双方通信禁止使用 protocol;不是栈本身禁止使用 stack。',
      '6. 如果是 sort 模板:提供 initialData(数字数组 ≥ 2),每步 array 长度一致。',
      '7. 如果是 protocol 模板:actors 长度 2,每步 from/to 必须在 actors 中。',
      '8. 如果是 stack 模板:提供 initialStack,每步 operation 取值 init/push/pop/peek/done。',
      '9. 如果是 queue 模板:提供 initialQueue,每步 operation 取值 init/enqueue/dequeue/peek/done,queue 表示当前队列。',
      '10. 如果是 tree 模板:提供 root,每步 operation 取值 init/visit/compare/go-left/go-right/backtrack/done,并用 currentNode/path/visited 表示进展。',
      '11. 如果是 graph 模板:提供 nodes 和 edges,edges 只能引用已有节点,每步用 activeNode/visited/frontier/activeEdges 表示 BFS/DFS/最短路径进展。',
      '12. 如果是 concept 模板:必须提供 mainTerm 和 coreIdea,steps 数量 5~7 步,覆盖"定义 → 结构/特征 → 工作过程 → 常见误区 → 小结"。',
      '13. 所有文字必须是中文,专业术语(BST/FIFO/O(n))可保留。',
      '14. 只返回一个 JSON 对象,不要 markdown、代码围栏、解释文字。',
    ].join('\n')

    const parts = [
      '你是一名互动课件结构化生成助手。',
      '核心目标:让学生"看见"抽象概念,不是"读到"抽象概念。',
      '',
      `【核心概念】${form.concept}`,
      `【概念类型】${getConceptTypeLabel(form.conceptType)}`,
      `【适用对象】${form.targetGroup}`,
      `【教学目标】${form.teachingGoal}`,
      '【交互方式】以手动推演为主:上一步/下一步、步骤导航、状态变化清晰;可保留自动播放,但不要依赖自动播放表达重点。',
      '【呈现重点】动画流程是主解释,文字只做一句短提示。禁止用大段文字解释代替动画。',
      form.emphasis ? `【重点强调】${form.emphasis}` : '',
      form.extraRequirements ? `【其他要求】${form.extraRequirements}` : '',
      '',
      decisionTree,
      '',
      `【模板选型提示】${inferAnimSceneHint(form.concept)}`,
      '',
      '【sort 模板 schema】',
      schemaSort,
      '',
      '【protocol 模板 schema】',
      schemaProtocol,
      '',
      '【stack 模板 schema】',
      schemaStack,
      '',
      '【queue 模板 schema】',
      schemaQueue,
      '',
      '【tree 模板 schema】',
      schemaTree,
      '',
      '【graph 模板 schema】',
      schemaGraph,
      '',
      '【concept 模板 schema(通用兜底,大多数概念用这个)】',
      schemaConcept,
      '',
      visualSchemas,
      '',
      motionSchemas,
      '',
      exampleLinear,
      '',
      templateRules,
    ]

    return parts.filter(Boolean).join('\n').trim()
  }

  const requestAnimJsonRaw = async (form: AnimFormModel) => {
    let rawContent = ''
    if (streamPrepareAgent) {
      await streamPrepareAgent('anim', { ...form }, (chunk) => {
        rawContent += chunk
      }, { sourceContent: buildAnimJsonPrompt(form) })
    } else {
      await streamText(buildAnimJsonPrompt(form), 'anim_json', (chunk) => {
        rawContent += chunk
      })
    }
    return rawContent
  }

  const parseAndValidateAnimJson = (rawContent: string) => {
    try {
      const data = safeParseAnimJson(rawContent)
      const validationResult = validateAnimJson(data)
      return { data, errors: validationResult.errors, valid: validationResult.valid }
    } catch (error: any) {
      return {
        data: null,
        errors: [error?.message || 'JSON 解析失败'],
        valid: false,
      }
    }
  }

  const normalizeConceptText = (value: unknown) =>
    String(value || '')
      .toLowerCase()
      .replace(/[\s"'`，。、“”‘’：:；;,.!?！？（）()[\]{}<>《》·\-_/\\|]+/g, '')

  const buildConceptKeywords = (concept: string) => {
    const raw = concept.trim()
    const normalized = normalizeConceptText(raw)
    const keywords = new Set<string>()

    if (normalized.length >= 2) {
      keywords.add(normalized)
    }

    raw.toLowerCase()
      .match(/[a-z][a-z0-9+#.-]{1,}/g)
      ?.forEach((token) => keywords.add(token.replace(/[^a-z0-9+#.-]/g, '')))

    const knownTerms = [
      'tcp', 'udp', 'http', 'https', 'websocket', 'tls', 'ssl',
      '三次握手', '四次挥手', '握手', '挥手',
      '冒泡', '选择排序', '插入排序', '快速排序', '归并排序', '排序',
      '栈', '堆栈', '队列', '链表', '单链表', '双链表', '循环链表',
      '二叉搜索树', '二叉树', '搜索树', '红黑树', 'avl', 'bst', '树',
      'bfs', 'dfs', '图遍历', '有向图', '无向图', '最短路径', 'dijkstra', '拓扑',
      '递归', '循环', '哈希', '散列', '数组', '线性表', '顺序表',
      '条件', '分支', 'if', 'switch',
    ]
    knownTerms.forEach((term) => {
      const normalizedTerm = normalizeConceptText(term)
      if (normalized.includes(normalizedTerm)) {
        keywords.add(normalizedTerm)
      }
    })

    const chinese = raw.replace(/[^\u4e00-\u9fa5]/g, '')
    const generic = new Set(['概念', '过程', '原理', '结构', '深入', '浅出', '讲解', '可视', '动画', '课件'])
    for (let i = 0; i < chinese.length - 1; i += 1) {
      const token = chinese.slice(i, i + 2)
      if (!generic.has(token)) {
        keywords.add(token)
      }
    }

    return [...keywords].filter((item) => item.length >= 2)
  }

  const isAnimPayloadAlignedWithConcept = (data: AnimPayload, concept: string) => {
    const keywords = buildConceptKeywords(concept)
    if (!keywords.length) return true

    const payloadText = normalizeConceptText(JSON.stringify(data))
    return keywords.some((keyword) => payloadText.includes(keyword))
  }

  const buildAnimRepairJsonPrompt = (
    form: AnimFormModel,
    rawContent: string,
    errors: string[],
  ) => {
    return [
      '你需要修复一份互动课件 JSON。只返回修复后的完整 JSON 对象,不要解释,不要 Markdown。',
      '',
      '【修复目标】',
      '1. 必须输出合法 JSON 对象。',
      '2. templateType 只能是 sort、protocol、stack、queue、tree、graph、concept。',
      '3. 严格按原始核心概念选择模板:排序用 sort; TCP/HTTP/握手/请求响应用 protocol; 栈/LIFO 用 stack; 队列/FIFO 用 queue; 二叉树/BST/树遍历用 tree; 图/BFS/DFS/最短路径用 graph; 链表/哈希/递归/分支等用 concept。',
      '4. concept 模板至少 5 步,至少 4 步带 visual 原语(nodes-chain/tree/branching/comparison/highlight-card/flow)。',
      '5. 每步尽量补足 stageCaption 和 motion,让播放器能演出运动过程。',
      '6. sort 每步 array 长度与 initialData 一致; protocol actors 必须 2 个且 from/to 在 actors 内; stack/queue/tree/graph 的 operation 必须使用各自允许值。',
      '7. 每步 desc 压缩为一句,不要用长段文字替代动画。',
      '',
      `【核心概念】${form.concept}`,
      `【概念类型】${getConceptTypeLabel(form.conceptType)}`,
      `【适用对象】${form.targetGroup}`,
      `【教学目标】${form.teachingGoal}`,
      errors.length ? `【校验错误】\n${errors.map((item) => `- ${item}`).join('\n')}` : '',
      '',
      '【待修复内容】',
      rawContent,
    ].filter(Boolean).join('\n')
  }

  const requestAnimJsonRepair = async (
    form: AnimFormModel,
    rawContent: string,
    errors: string[],
  ) => {
    let repairedContent = ''
    if (streamPrepareAgent) {
      await streamPrepareAgent('anim_repair', { ...form, validationErrors: errors }, (chunk) => {
        repairedContent += chunk
      }, { sourceContent: buildAnimRepairJsonPrompt(form, rawContent, errors) })
    } else {
      await streamText(buildAnimRepairJsonPrompt(form, rawContent, errors), 'anim_json', (chunk) => {
        repairedContent += chunk
      })
    }
    return repairedContent
  }

  const buildAnimOptimizeJsonPrompt = (instruction: string) => {
    return [
      '你将优化一份现有的互动课件 JSON。',
      '要求:',
      '1. 保持 templateType 与 steps 结构稳定。',
      '2. 除非必要,不要改动步骤数量与顺序。',
      '3. 重点优化 subtitle、teachingGoal、steps[].title、steps[].stageCaption、steps[].motion、steps[].desc(concept 模板可优化 visual 的参数)。',
      '4. 不能更换 templateType。',
      '5. 只返回完整 JSON 对象,不要解释。',
      '',
      `【优化目标】${instruction}`,
      '',
      '【当前 JSON】',
      JSON.stringify(animJsonResult.value, null, 2),
    ].join('\n')
  }

  const requestAnimOptimizeJson = async (instruction: string) => {
    let rawContent = ''
    if (streamPrepareAgent) {
      await streamPrepareAgent('anim_optimize', {
        optimizeInstruction: instruction,
      }, (chunk) => {
        rawContent += chunk
      }, { sourceContent: buildAnimOptimizeJsonPrompt(instruction) })
    } else {
      await streamText(buildAnimOptimizeJsonPrompt(instruction), 'anim_optimize_json', (chunk) => {
        rawContent += chunk
      })
    }
    return safeParseAnimJson(rawContent)
  }

  const applyAnimResult = (data: AnimPayload, status: AnimRenderStatus = 'ready') => {
    const validationResult = validateAnimJson(data)
    animValidationErrors.value = validationResult.errors

    if (!validationResult.valid) {
      animRenderStatus.value = 'idle'
      return false
    }

    animJsonResult.value = cloneAnimData(data)
    animRenderStatus.value = status
    return true
  }

  const makeAnimMoreBasic = (data: AnimPayload): AnimPayload => {
    const cloned = cloneAnimData(data)
    cloned.subtitle = `${cloned.subtitle}(零基础友好版)`
    cloned.teachingGoal = `用更生活化的方式帮助学生理解:${cloned.teachingGoal}`

    cloned.steps = cloned.steps.map((step: any, index: number) => ({
      ...step,
      title: step.title || `第 ${index + 1} 步`,
      desc: step.desc && step.desc.includes('可以把它理解成')
        ? step.desc
        : `可以把它理解成一个循序渐进的小过程。${step.desc || ''}`,
    }))

    return cloned
  }

  const makeAnimMoreVivid = (data: AnimPayload): AnimPayload => {
    const cloned = cloneAnimData(data)
    cloned.subtitle = `${cloned.subtitle}(讲解更生动)`

    cloned.steps = cloned.steps.map((step: any, index: number) => ({
      ...step,
      title: step.title && step.title.startsWith('关键') ? step.title : `关键过程 ${index + 1}:${step.title || ''}`,
      desc: step.desc && step.desc.includes('最关键的变化')
        ? step.desc
        : `${step.desc || ''} 此时最关键的变化是:学习者可以直接观察到状态正在发生转移。`,
    }))

    return cloned
  }

  const makeAnimSlower = (data: AnimPayload): AnimPayload => {
    const cloned = cloneAnimData(data)

    cloned.steps = cloned.steps.map((step: any) => ({
      ...step,
      desc: step.desc && step.desc.includes('再慢一点看')
        ? step.desc
        : `${step.desc || ''} 再慢一点看,这一步只需要抓住一个重点:先看当前状态,再理解为什么会这样变化。`,
    }))

    return cloned
  }

  const generateAnimation = async (form: AnimFormModel) => {
    if (!form.concept.trim()) {
      notify?.warning?.('请输入需要推演的核心概念')
      return false
    }

    isAnimGenerating.value = true
    animValidationErrors.value = []
    animJsonResult.value = null
    animRenderStatus.value = 'validating'
    animAutoPlayInterval.value = initialAutoPlayInterval

    try {
      const rawContent = await requestAnimJsonRaw(form)
      let parsed = parseAndValidateAnimJson(rawContent)

      if (parsed.valid && parsed.data && !isAnimPayloadAlignedWithConcept(parsed.data, form.concept)) {
        parsed = {
          data: parsed.data,
          valid: false,
          errors: ['生成结果与核心概念不一致,需要重新修复为当前主题。'],
        }
      }

      if (!parsed.valid) {
        const repairedContent = await requestAnimJsonRepair(form, rawContent, parsed.errors)
        const repaired = parseAndValidateAnimJson(repairedContent)
        if (repaired.valid && repaired.data && isAnimPayloadAlignedWithConcept(repaired.data, form.concept)) {
          parsed = repaired
          notify?.warning?.('AI 首次返回的 JSON 已自动修复')
        } else {
          parsed = {
            ...repaired,
            errors: repaired.errors.length ? repaired.errors : parsed.errors,
          }
        }
      }

      const ok = parsed.data ? applyAnimResult(parsed.data, 'ready') : false

      if (ok) {
        notify?.success?.('动画课件生成完毕')
        return true
      }

      animValidationErrors.value = parsed.errors
      const errorMsg = parsed.errors.join(';') || '结构校验未通过'
      const preset = resolvePresetByConcept(form.concept, {
        targetGroup: form.targetGroup,
        teachingGoal: form.teachingGoal,
      })
      if (preset && applyAnimResult(preset, 'fallback')) {
        notify?.warning?.('AI 返回结构不合规,已自动切换到同主题精品模板')
        return true
      }

      const fallback = withAnimationFlowHints(buildConceptFallbackPreset(form.concept, {
        targetGroup: form.targetGroup,
        teachingGoal: form.teachingGoal,
      }))
      if (applyAnimResult(fallback, 'fallback')) {
        notify?.warning?.('AI 返回结构不合规,已自动切换到通用概念模板')
        return true
      }

      throw new Error(errorMsg)
    } catch (error: any) {
      const preset = resolvePresetByConcept(form.concept, {
        targetGroup: form.targetGroup,
        teachingGoal: form.teachingGoal,
      })
      if (preset && applyAnimResult(preset, 'fallback')) {
        notify?.warning?.('AI 生成失败,已自动回退到同主题精品模板')
        return true
      }

      const fallback = resolveFallbackPayload(form.concept, {
        targetGroup: form.targetGroup,
        teachingGoal: form.teachingGoal,
      })
      if (applyAnimResult(fallback, 'fallback')) {
        notify?.warning?.('AI 生成失败,已自动回退到通用概念模板')
        return true
      }

      notify?.error?.(error?.message || '生成失败,请检查网络或后端 anim_json 接口')
      return false
    } finally {
      isAnimGenerating.value = false
    }
  }

  const optimizeAnimation = async (action: AnimOptimizeAction, form: AnimFormModel) => {
    if (!animJsonResult.value) {
      notify?.warning?.('请先生成一份动画课件')
      return false
    }

    isAnimOptimizing.value = true
    currentAnimOptimizeAction.value = action

    try {
      if (action === 'stable') {
        notify?.success?.('已切换为稳健展示模式')
        return true
      }

      if (action === 'slow') {
        animAutoPlayInterval.value = 2600
        const slower = makeAnimSlower(animJsonResult.value)
        return applyAnimResult(slower, animRenderStatus.value === 'fallback' ? 'fallback' : 'ready')
      }

      if (action === 'vivid') {
        try {
          const optimized = await requestAnimOptimizeJson(
            '让讲解更生动形象,但重点是动画流程。为每个 step 补充或优化 stageCaption 和 motion。对于 concept 模板,如果某些 step 还没有 visual 字段,请补上合适的视觉原语(tree / nodes-chain / branching / comparison / highlight-card / flow)。不要改变 templateType、steps 顺序和核心结构。',
          )
          if (
            !applyAnimResult(
              optimized,
              animRenderStatus.value === 'fallback' ? 'fallback' : 'ready',
            )
          ) {
            throw new Error('优化结果未通过校验')
          }
        } catch {
          applyAnimResult(
            makeAnimMoreVivid(animJsonResult.value),
            animRenderStatus.value === 'fallback' ? 'fallback' : 'ready',
          )
        }
        notify?.success?.('课件已优化为更生动的讲解风格')
        return true
      }

      if (action === 'basic') {
        try {
          const optimized = await requestAnimOptimizeJson(
            '让内容更适合零基础学生,只改 subtitle、teachingGoal、steps[].title、steps[].desc,不改结构。',
          )
          if (
            !applyAnimResult(
              optimized,
              animRenderStatus.value === 'fallback' ? 'fallback' : 'ready',
            )
          ) {
            throw new Error('优化结果未通过校验')
          }
        } catch {
          applyAnimResult(
            makeAnimMoreBasic(animJsonResult.value),
            animRenderStatus.value === 'fallback' ? 'fallback' : 'ready',
          )
        }
        notify?.success?.('课件已优化为零基础友好版')
        return true
      }

      return false
    } catch (error: any) {
      notify?.error?.(error?.message || '优化失败,请稍后重试')
      return false
    } finally {
      isAnimOptimizing.value = false
      currentAnimOptimizeAction.value = ''
    }
  }

  return {
    animJsonResult,
    animRenderStatus,
    animValidationErrors,
    isAnimGenerating,
    isAnimOptimizing,
    currentAnimOptimizeAction,
    animAutoPlayInterval,
    currentTemplateType,
    currentTemplateMeta,
    generateAnimation,
    optimizeAnimation,
    resetAnimEngine,
    applyAnimResult,
    getCurrentJsonText: () => JSON.stringify(animJsonResult.value, null, 2),
  }
}
