export interface UserProfile {
  id: number
  username: string
  displayName: string
}

export interface LoginResponse {
  token: string
  user: UserProfile
  landingRoute: string
}

export interface WorkOrderPayload {
  title: string
  type: string
  serviceName?: string
  description?: string
  priority?: string
}

export interface WorkOrderTransitionPayload {
  action: string
  comment?: string
}

export interface WorkOrderDetail {
  id: number
  orderNo: string
  title: string
  type: string
  creatorUsername: string
  creatorDisplayName: string
  progress: string
  status: string
  priority: string
  serviceName: string
  description: string
  estimatedAt: string
  createdAt: string
  updatedAt: string
  processCode: string
  currentNodeCode: string
  currentNodeName: string
  currentHandler: string
}

export interface WorkOrderHistoryEntry {
  id: number
  orderNo: string
  action: string
  fromStatus: string | null
  toStatus: string
  fromNodeCode: string | null
  toNodeCode: string
  operatorUsername: string
  operatorDisplayName: string
  comment: string | null
  createdAt: string
}

export interface WorkOrderDetailResponse {
  order: WorkOrderDetail
  histories: WorkOrderHistoryEntry[]
}

export interface MessageSubscriptionPayload {
  messageType: string
  source: string
  siteEnabled: boolean
  smsEnabled: boolean
  mailEnabled: boolean
  wxEnabled: boolean
  dingEnabled: boolean
}

export interface ShellConfig {
  productName: string
  platformButton: string
  topNav: string[]
  user: {
    account: string
    displayName: string
  }
}

export interface MenuItem {
  label: string
  route: string
}

export interface MenuGroup {
  group: string
  items: MenuItem[]
}

export interface NavigationItem {
  id: number
  name: string
  icon: string
  creator: string
  link: string
  mobile: string
  desc: string
}

export interface NavigationGroup {
  title: string
  rows: NavigationItem[]
}

export interface PageData {
  title: string
  breadcrumb: string[]
  intro: string
  kind: string
  [key: string]: unknown
}

export interface PlatformBootstrap {
  shell: ShellConfig
  menu: MenuGroup[]
  navigationGroups: NavigationGroup[]
  overviewData: Record<string, unknown>
  pages: Record<string, PageData>
}
