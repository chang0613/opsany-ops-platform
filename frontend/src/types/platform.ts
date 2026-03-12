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
  catalogCode?: string
  processCode?: string
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

export interface UserOption {
  id: number
  username: string
  displayName: string
}

export interface WorkOrderCatalog {
  id: number
  catalogCode: string
  name: string
  category: string
  type: string
  scope: string
  online: boolean
  processCode: string
  slaName: string
  ownerUsername: string
  ownerDisplayName: string
  description: string
  sortNo: number
  createdAt: string
  updatedAt: string
}

export interface SaveWorkOrderCatalogPayload {
  catalogCode?: string
  name: string
  category: string
  type: string
  scope: string
  online: boolean
  processCode: string
  slaName: string
  ownerUsername: string
  description: string
  sortNo: number
}

export interface ProcessNodePayload {
  nodeCode: string
  nodeName: string
  nodeType: string
  sortNo: number
  assigneeRole: string
  nextApproveNode?: string
  nextRejectNode?: string
}

export interface ProcessDefinition {
  id: number
  processCode: string
  name: string
  category: string
  version: number
  status: string
  owner: string
  creator: string
  updater: string
  updatedAt: string
  description: string
  definitionJson?: string
}

export interface ProcessDefinitionDetail {
  definition: ProcessDefinition
  nodes: ProcessNodePayload[]
}

export interface SaveProcessDefinitionPayload {
  processCode?: string
  name: string
  category: string
  status: string
  description: string
  definitionJson?: string
  nodes: ProcessNodePayload[]
}

export interface DutyGroup {
  id: number
  name: string
  ownerUsername: string
  ownerDisplayName: string
  members: number
  coverage: string
  description?: string
}

export interface DutyShift {
  id: number
  groupId: number
  groupName: string
  dutyDate: string
  dateLabel: string
  shiftLabel: string
  shiftTime: string
  ownerUsername: string
  ownerDisplayName: string
  status: string
}

export interface SaveDutyGroupPayload {
  id?: number
  name: string
  ownerUsername: string
  members: number
  coverage: string
  description: string
}

export interface SaveDutyShiftPayload {
  id?: number
  groupId: number
  dutyDate: string
  shiftTime: string
  ownerUsername: string
  status: string
}

export interface AppRole {
  roleCode: string
  roleName: string
  description?: string
  sortNo: number
}

export interface MenuConfigItem {
  id?: number
  menuCode: string
  groupName: string
  groupSortNo: number
  label: string
  route: string
  icon?: string
  sortNo: number
  permissionCode?: string
  visible: boolean
  roleCodes: string[]
}

export interface MenuConfigResponse {
  roles: AppRole[]
  menus: MenuConfigItem[]
}

export interface ShellConfig {
  productName: string
  platformButton: string
  topNav: string[]
  basePath?: string
  platformKey?: string
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
  itemCode: string
  groupCode?: string
  name: string
  icon: string
  creator: string
  creatorUsername?: string
  link: string
  mobile: string
  mobileVisible?: boolean
  desc: string
  sortNo?: number
  enabled?: boolean
  favorite?: boolean
}

export interface NavigationGroup {
  groupCode?: string
  title: string
  sortNo?: number
  rows: NavigationItem[]
}

export interface NavigationConfigResponse {
  groups: NavigationGroup[]
}

export interface SaveNavigationGroupPayload {
  groupCode?: string
  title: string
  sortNo: number
}

export interface SaveNavigationItemPayload {
  itemCode?: string
  groupCode: string
  name: string
  icon: string
  link: string
  mobileVisible: boolean
  description: string
  sortNo: number
  enabled: boolean
}

export interface NavigationFavoriteState {
  itemCode: string
  favorite: boolean
}

export interface PlatformPageTableState {
  tableKey: string
  rows: Array<Record<string, unknown>>
}

export interface PlatformPageStatePayload {
  platformKey: string
  pageKey: string
  tableStates: PlatformPageTableState[]
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
  favoriteNavigations: NavigationItem[]
  overviewData: Record<string, unknown>
  pages: Record<string, PageData>
}

