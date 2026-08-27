export type CourseMindmapStatus = 'ready' | 'fallback'

export interface CourseMindmapNode {
  name: string
  children?: CourseMindmapNode[]
}

export interface CourseMindmapData {
  title: string
  root: CourseMindmapNode
  status: CourseMindmapStatus
  updatedAt: string
  sourceHash: string
}
