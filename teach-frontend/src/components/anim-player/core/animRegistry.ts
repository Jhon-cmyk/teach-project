import type { AnimPayload, AnimTemplateType } from './animTypes.ts'

export interface AnimTemplateRegistryItem {
  key: AnimTemplateType
  label: string
  emptyHint: string
}

export const ANIM_TEMPLATE_REGISTRY: Record<AnimTemplateType, AnimTemplateRegistryItem> = {
  sort: {
    key: 'sort',
    label: '排序/过程模板',
    emptyHint: '适合排序、查找、循环对比等固定长度过程演示',
  },
  protocol: {
    key: 'protocol',
    label: '协议/时序模板',
    emptyHint: '适合 TCP、HTTP、请求链路、握手挥手等双方时序演示',
  },
  stack: {
    key: 'stack',
    label: '栈结构模板',
    emptyHint: '适合栈、入栈、出栈、栈顶变化等后进先出演示',
  },
  queue: {
    key: 'queue',
    label: '队列结构模板',
    emptyHint: '适合队列、入队、出队、队头队尾与先进先出演示',
  },
  tree: {
    key: 'tree',
    label: '树结构模板',
    emptyHint: '适合二叉树、BST 查找、树遍历与递归路径演示',
  },
  graph: {
    key: 'graph',
    label: '图结构模板',
    emptyHint: '适合 BFS、DFS、最短路径、拓扑关系等图遍历演示',
  },
  concept: {
    key: 'concept',
    label: '通用概念模板',
    emptyHint: '适合二叉树、链表、if 语句、递归、概念讲解等抽象概念的分步卡片式讲解',
  },
}

export const getAnimTemplateMeta = (templateType?: AnimTemplateType | '') => {
  if (!templateType) return null
  return ANIM_TEMPLATE_REGISTRY[templateType] ?? null
}

export const getPayloadTemplateType = (
  payload: AnimPayload | null | undefined,
): AnimTemplateType | '' => {
  return payload?.templateType ?? ''
}

export const isTemplateType = (value: unknown): value is AnimTemplateType => {
  return (
    value === 'sort' ||
    value === 'protocol' ||
    value === 'stack' ||
    value === 'queue' ||
    value === 'tree' ||
    value === 'graph' ||
    value === 'concept'
  )
}
