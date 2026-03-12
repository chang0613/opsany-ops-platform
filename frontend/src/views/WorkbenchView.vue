<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import CreateOrderModal from '../components/CreateOrderModal.vue'
import PlatformModulePage from '../components/PlatformModulePage.vue'
import PlatformNavigationPanel from '../components/PlatformNavigationPanel.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ToggleSwitch from '../components/ToggleSwitch.vue'
import {
  createWorkOrder,
  deleteCatalog,
  deleteNavigationItem,
  deleteMenuConfig,
  getCatalogs,
  getDutyGroups,
  getDutyShifts,
  getMenuConfig,
  getNavigationConfig,
  getProcessDetail,
  getProcesses,
  getSubscriptions,
  getUsers,
  getWorkbenchBootstrap,
  getWorkOrderDetail,
  saveCatalog,
  saveDutyGroup,
  saveDutyShift,
  saveMenuConfig,
  saveNavigationGroup,
  saveNavigationItem,
  saveProcessDefinition,
  saveSubscriptionsForUser,
  toggleNavigationFavorite,
  transitionWorkOrder,
} from '../lib/api'
import { clearSession, getStoredUser } from '../lib/session'
import type {
  DutyGroup,
  DutyShift,
  MenuConfigItem,
  MenuConfigResponse,
  NavigationConfigResponse,
  NavigationGroup,
  NavigationItem,
  NavigationFavoriteState,
  PageData,
  PlatformBootstrap,
  ProcessDefinition,
  ProcessDefinitionDetail,
  ProcessNodePayload,
  SaveNavigationGroupPayload,
  SaveNavigationItemPayload,
  SaveDutyGroupPayload,
  SaveDutyShiftPayload,
  SaveProcessDefinitionPayload,
  SaveWorkOrderCatalogPayload,
  UserOption,
  WorkOrderCatalog,
  WorkOrderDetailResponse,
  WorkOrderPayload,
} from '../types/platform'

interface SubscriptionRow {
  type: string
  source: string
  siteEnabled: boolean
  smsEnabled: boolean
  mailEnabled: boolean
  wxEnabled: boolean
  dingEnabled: boolean
}

const route = useRoute()
const router = useRouter()

const bootstrap = ref<PlatformBootstrap | null>(null)
const loading = ref(true)
const loadError = ref('')
const orderModalOpen = ref(false)
const orderDetailOpen = ref(false)
const orderDetailLoading = ref(false)
const orderTransitioning = ref(false)
const selectedOrderNo = ref('')
const orderComment = ref('')
const orderDetail = ref<WorkOrderDetailResponse | null>(null)
const userOptions = ref<UserOption[]>([])
const subscriptionTargetUser = ref('')
const subscriptionRows = ref<SubscriptionRow[]>([])
const catalogRows = ref<WorkOrderCatalog[]>([])
const processRows = ref<ProcessDefinition[]>([])
const dutyGroupRows = ref<DutyGroup[]>([])
const dutyShiftRows = ref<DutyShift[]>([])
const menuConfig = ref<MenuConfigResponse | null>(null)
const navigationConfig = ref<NavigationConfigResponse | null>(null)
const catalogDialogOpen = ref(false)
const catalogSaving = ref(false)
const processDesignerOpen = ref(false)
const processDesignerLoading = ref(false)
const processSaving = ref(false)
const dutyGroupDialogOpen = ref(false)
const dutyGroupSaving = ref(false)
const dutyShiftDialogOpen = ref(false)
const dutyShiftSaving = ref(false)
const menuDialogOpen = ref(false)
const menuSaving = ref(false)
const navigationPanelOpen = ref(false)
const navigationGroupDialogOpen = ref(false)
const navigationGroupSaving = ref(false)
const navigationItemDialogOpen = ref(false)
const navigationItemSaving = ref(false)
const toast = ref('')
const currentTime = ref(formatNow())
const storedUser = ref(getStoredUser())

const tabs = reactive<Record<string, string>>({
  'overview-tabs': '总览',
  servicePortal: '全部服务(7)',
  orders: '我提交的(16)',
  tasks: '我的待办(0)',
  dutyView: '日历视图',
  dutyRange: '周',
  messageTypes: '全部消息类型(1)',
  processes: '流程设计(9)',
  apis: '公共API(4)',
  dutyManage: '值班日历',
  dutyManageView: '日历视图',
})

const toggles = reactive<Record<string, boolean>>({})
const catalogForm = reactive<SaveWorkOrderCatalogPayload>({
  catalogCode: '',
  name: '',
  category: '默认分组',
  type: '请求管理',
  scope: '全部用户',
  online: true,
  processCode: 'STANDARD_REQUEST',
  slaName: '-',
  ownerUsername: '',
  description: '',
  sortNo: 0,
})
const processForm = reactive<SaveProcessDefinitionPayload>({
  processCode: '',
  name: '',
  category: '请求管理',
  status: '未发布',
  description: '',
  definitionJson: '',
  nodes: [],
})
const dutyGroupForm = reactive<SaveDutyGroupPayload>({
  name: '',
  ownerUsername: '',
  members: 0,
  coverage: '工作日',
  description: '',
})
const dutyShiftForm = reactive<SaveDutyShiftPayload>({
  groupId: 0,
  dutyDate: '',
  shiftTime: '09:00 - 18:00',
  ownerUsername: '',
  status: '待值班',
})
const menuForm = reactive<MenuConfigItem>({
  menuCode: '',
  groupName: '工作台',
  groupSortNo: 1,
  label: '',
  route: '/',
  icon: '',
  sortNo: 0,
  permissionCode: '',
  visible: true,
  roleCodes: [],
})
const navigationGroupForm = reactive<SaveNavigationGroupPayload>({
  groupCode: '',
  title: '',
  sortNo: 1,
})
const navigationItemForm = reactive<SaveNavigationItemPayload>({
  itemCode: '',
  groupCode: '',
  name: '',
  icon: '导',
  link: '/',
  mobileVisible: false,
  description: '',
  sortNo: 1,
  enabled: true,
})
let clockTimer: number | undefined
let toastTimer: number | undefined

const fallbackShell = {
  productName: '工作台',
  platformButton: '平台导航',
  topNav: ['控制台', '工作台', '消息', '支持'],
  basePath: '/o/workbench',
  platformKey: 'workbench',
  user: {
    account: storedUser.value?.username ?? 'demo',
    displayName: storedUser.value?.displayName ?? '演示用户',
  },
}

const fallbackPage: PageData = {
  title: '概览',
  breadcrumb: ['工作台', '概览'],
  intro: '工作台默认首页，聚合基础设施、待办、值班、消息和大屏入口。',
  kind: 'overview',
}

const shell = computed(() => bootstrap.value?.shell ?? fallbackShell)
const overviewData = computed<Record<string, any>>(() => (bootstrap.value?.overviewData ?? {}) as Record<string, any>)
const menuGroups = computed(() => bootstrap.value?.menu ?? [])
const navigationGroups = computed(() => bootstrap.value?.navigationGroups ?? [])
const favoriteNavigations = computed(() => bootstrap.value?.favoriteNavigations ?? [])
const navigationAdminGroups = computed(() => navigationConfig.value?.groups ?? navigationGroups.value)
const messageBadge = computed(() => {
  const rows = ((bootstrap.value?.pages['/msgCenter/messageManage'] as any)?.rows ?? []) as any[]
  return rows.length > 0 ? String(rows.length) : '1'
})
const platformBasePath = computed(() => normalizeBasePath(shell.value.basePath))
const currentRoute = computed(() => {
  if (!bootstrap.value) {
    return resolvePageKey(route.path, inferBasePath(route.path))
  }

  const routeKey = resolvePageKey(route.path, platformBasePath.value)
  if (bootstrap.value.pages[routeKey]) {
    return routeKey
  }

  if (bootstrap.value.pages[route.path]) {
    return route.path
  }

  return '/'
})
const activePlatformLink = computed(() => `${platformBasePath.value}/`)

const page = computed<Record<string, any>>(
  () => (bootstrap.value?.pages[currentRoute.value] ?? bootstrap.value?.pages['/'] ?? fallbackPage) as Record<string, any>,
)

const serviceOptions = computed(() => {
  const services = ((bootstrap.value?.pages['/personSetting/serviceFolder'] as any)?.services ?? []) as Array<{ title: string }>
  return services.map((item) => item.title)
})
const menuRoles = computed(() => menuConfig.value?.roles ?? [])
const menuItems = computed(() => menuConfig.value?.menus ?? [])
const navigationGroupOptions = computed(() => navigationAdminGroups.value.map((group) => ({
  label: group.title,
  value: group.groupCode ?? '',
})))
const subscriptionUserLabel = computed(() => {
  const current = userOptions.value.find((item) => item.username === subscriptionTargetUser.value)
  return current ? `${current.displayName} (${current.username})` : subscriptionTargetUser.value
})

const orderActionOptions = computed(() => {
  const nodeCode = orderDetail.value?.order.currentNodeCode ?? ''
  if (nodeCode === 'TRIAGE') {
    return [
      { label: '受理工单', action: 'APPROVE', type: 'success' as const },
      { label: '驳回工单', action: 'REJECT', type: 'danger' as const },
    ]
  }

  if (nodeCode === 'ENGINEER_HANDLE') {
    return [
      { label: '提交确认', action: 'APPROVE', type: 'success' as const },
      { label: '驳回工单', action: 'REJECT', type: 'danger' as const },
    ]
  }

  if (nodeCode === 'REQUESTER_CONFIRM') {
    return [
      { label: '确认完成', action: 'CONFIRM', type: 'success' as const },
      { label: '重新处理', action: 'REOPEN', type: 'warning' as const },
    ]
  }

  return []
})

function formatNow(): string {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    weekday: 'long',
    hour12: false,
  }).format(new Date())
}

function normalizeAbsolutePath(path: string | undefined): string {
  if (!path) {
    return '/o/workbench'
  }
  let normalized = path.replace(/#/g, '/').replace(/\/+/g, '/')
  if (normalized.length > 1 && normalized.endsWith('/')) {
    normalized = normalized.slice(0, -1)
  }
  return normalized || '/o/workbench'
}

function normalizeBasePath(path: string | undefined): string {
  const normalized = normalizeAbsolutePath(path)
  if (!normalized.startsWith('/o/')) {
    return '/o/workbench'
  }
  const segments = normalized.split('/').filter(Boolean)
  return segments.length >= 2 ? `/o/${segments[1]}` : '/o/workbench'
}

function inferBasePath(path: string | undefined): string {
  const normalized = normalizeAbsolutePath(path)
  if (!normalized.startsWith('/o/')) {
    return '/o/workbench'
  }
  return normalizeBasePath(normalized)
}

function resolvePageKey(path: string | undefined, basePath: string): string {
  const normalizedPath = normalizeAbsolutePath(path)
  const normalizedBasePath = normalizeBasePath(basePath)

  if (!normalizedPath.startsWith('/o/')) {
    return normalizedPath === '/login' ? '/' : normalizedPath
  }

  if (normalizedPath === normalizedBasePath) {
    return '/'
  }

  if (normalizedPath.startsWith(`${normalizedBasePath}/`)) {
    const suffix = normalizedPath.slice(normalizedBasePath.length)
    return suffix || '/'
  }

  return '/'
}

function resolveAppRoute(path: string, basePath = platformBasePath.value): string {
  if (!path || path === '/') {
    return `${normalizeBasePath(basePath)}/`
  }
  if (path.startsWith('/o/')) {
    return normalizeAbsolutePath(path)
  }
  return normalizeAbsolutePath(`${normalizeBasePath(basePath)}${path.startsWith('/') ? path : `/${path}`}`)
}

function activeTab(group: string, fallback = ''): string {
  return tabs[group] || fallback
}

function updateTab(group: string, value: string): void {
  tabs[group] = value
}

function isActiveRoute(path: string): boolean {
  return currentRoute.value === path
}

function toggleValue(id: string): boolean {
  return toggles[id] ?? true
}

function setToggle(id: string, value: boolean): void {
  toggles[id] = value
}

function subscriptionToggleKey(index: number, channel: string): string {
  return `subscription-${index}-${channel}`
}

function showToast(message: string): void {
  toast.value = message
  if (toastTimer) {
    window.clearTimeout(toastTimer)
  }
  toastTimer = window.setTimeout(() => {
    toast.value = ''
  }, 1800)
}

function orderPrimaryActionLabel(status: string): string {
  if (status.includes('待受理')) {
    return '受理'
  }
  if (status.includes('待确认')) {
    return '确认'
  }
  if (status.includes('处理')) {
    return '处理'
  }
  return '查看'
}

function historyActionLabel(action: string): string {
  if (action === 'SUBMIT') {
    return '提交工单'
  }
  if (action === 'REJECT') {
    return '驳回'
  }
  if (action === 'REOPEN') {
    return '重新处理'
  }
  if (action === 'CONFIRM') {
    return '确认完成'
  }
  if (action === 'APPROVE') {
    return '流转通过'
  }
  return action
}

function timelineTone(action: string): 'success' | 'warning' | 'danger' | 'primary' | 'info' {
  if (action === 'REJECT') {
    return 'danger'
  }
  if (action === 'REOPEN') {
    return 'warning'
  }
  if (action === 'SUBMIT') {
    return 'primary'
  }
  if (action === 'CONFIRM' || action === 'APPROVE') {
    return 'success'
  }
  return 'info'
}

function statusTagType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  if (status.includes('完成')) {
    return 'success'
  }
  if (status.includes('驳回')) {
    return 'danger'
  }
  if (status.includes('待')) {
    return 'warning'
  }
  return 'info'
}

function currentUsername(): string {
  return storedUser.value?.username ?? bootstrap.value?.shell.user.account ?? 'demo'
}

async function handleRequestFailure(error: unknown, fallback: string, toastError = true): Promise<boolean> {
  const message = error instanceof Error ? error.message : fallback
  if (message === 'UNAUTHORIZED') {
    clearSession()
    await router.replace('/login')
    return true
  }
  if (toastError) {
    showToast(message || fallback)
  }
  return false
}

function resetCatalogForm(row?: WorkOrderCatalog): void {
  catalogForm.catalogCode = row?.catalogCode ?? ''
  catalogForm.name = row?.name ?? ''
  catalogForm.category = row?.category ?? '默认分组'
  catalogForm.type = row?.type ?? '请求管理'
  catalogForm.scope = row?.scope ?? '全部用户'
  catalogForm.online = row?.online ?? true
  catalogForm.processCode = row?.processCode ?? processRows.value[0]?.processCode ?? 'STANDARD_REQUEST'
  catalogForm.slaName = row?.slaName ?? '-'
  catalogForm.ownerUsername = row?.ownerUsername ?? currentUsername()
  catalogForm.description = row?.description ?? ''
  catalogForm.sortNo = row?.sortNo ?? catalogRows.value.length + 1
}

function resetProcessForm(detail?: ProcessDefinitionDetail): void {
  processForm.processCode = detail?.definition.processCode ?? ''
  processForm.name = detail?.definition.name ?? ''
  processForm.category = detail?.definition.category ?? '请求管理'
  processForm.status = detail?.definition.status ?? '未发布'
  processForm.description = detail?.definition.description ?? ''
  processForm.definitionJson = detail?.definition.definitionJson ?? ''
  processForm.nodes = detail?.nodes?.map((item) => ({ ...item })) ?? [
    {
      nodeCode: 'START',
      nodeName: '提交',
      nodeType: 'START',
      sortNo: 1,
      assigneeRole: 'REQUESTER',
      nextApproveNode: 'TRIAGE',
      nextRejectNode: '',
    },
    {
      nodeCode: 'TRIAGE',
      nodeName: '主管受理',
      nodeType: 'APPROVAL',
      sortNo: 2,
      assigneeRole: 'PLATFORM_ADMIN',
      nextApproveNode: 'ENGINEER_HANDLE',
      nextRejectNode: 'END_REJECTED',
    },
    {
      nodeCode: 'ENGINEER_HANDLE',
      nodeName: '工程师处理',
      nodeType: 'TASK',
      sortNo: 3,
      assigneeRole: 'ENGINEER',
      nextApproveNode: 'REQUESTER_CONFIRM',
      nextRejectNode: 'END_REJECTED',
    },
    {
      nodeCode: 'REQUESTER_CONFIRM',
      nodeName: '申请人确认',
      nodeType: 'CONFIRM',
      sortNo: 4,
      assigneeRole: 'REQUESTER',
      nextApproveNode: 'END_DONE',
      nextRejectNode: 'ENGINEER_HANDLE',
    },
    {
      nodeCode: 'END_DONE',
      nodeName: '已完成',
      nodeType: 'END',
      sortNo: 5,
      assigneeRole: 'END',
      nextApproveNode: '',
      nextRejectNode: '',
    },
  ]
}

function resetDutyGroupForm(row?: DutyGroup): void {
  ;(dutyGroupForm as any).id = row?.id
  dutyGroupForm.name = row?.name ?? ''
  dutyGroupForm.ownerUsername = row?.ownerUsername ?? currentUsername()
  dutyGroupForm.members = row?.members ?? 0
  dutyGroupForm.coverage = row?.coverage ?? '工作日'
  dutyGroupForm.description = row?.description ?? ''
}

function resetDutyShiftForm(row?: DutyShift): void {
  ;(dutyShiftForm as any).id = row?.id
  dutyShiftForm.groupId = row?.groupId ?? dutyGroupRows.value[0]?.id ?? 0
  dutyShiftForm.dutyDate = row?.dutyDate ?? new Date().toISOString().slice(0, 10)
  dutyShiftForm.shiftTime = row?.shiftTime ?? '09:00 - 18:00'
  dutyShiftForm.ownerUsername = row?.ownerUsername ?? currentUsername()
  dutyShiftForm.status = row?.status ?? '待值班'
}

function resetMenuForm(row?: MenuConfigItem): void {
  menuForm.menuCode = row?.menuCode ?? ''
  menuForm.groupName = row?.groupName ?? '工作台'
  menuForm.groupSortNo = row?.groupSortNo ?? 1
  menuForm.label = row?.label ?? ''
  menuForm.route = row?.route ?? '/'
  menuForm.icon = row?.icon ?? ''
  menuForm.sortNo = row?.sortNo ?? menuItems.value.length + 1
  menuForm.permissionCode = row?.permissionCode ?? ''
  menuForm.visible = row?.visible ?? true
  menuForm.roleCodes = row?.roleCodes ? [...row.roleCodes] : menuRoles.value.map((role) => role.roleCode)
}

function resetNavigationGroupForm(group?: NavigationGroup): void {
  navigationGroupForm.groupCode = group?.groupCode ?? ''
  navigationGroupForm.title = group?.title ?? ''
  navigationGroupForm.sortNo = group?.sortNo ?? navigationAdminGroups.value.length + 1
}

function resetNavigationItemForm(item?: NavigationItem, groupCode?: string): void {
  navigationItemForm.itemCode = item?.itemCode ?? ''
  navigationItemForm.groupCode = item?.groupCode ?? groupCode ?? navigationGroupOptions.value[0]?.value ?? ''
  navigationItemForm.name = item?.name ?? ''
  navigationItemForm.icon = item?.icon ?? '导'
  navigationItemForm.link = item?.link ?? '/'
  navigationItemForm.mobileVisible = item?.mobileVisible ?? item?.mobile === '是'
  navigationItemForm.description = item?.desc ?? ''
  navigationItemForm.sortNo = item?.sortNo ?? 1
  navigationItemForm.enabled = item?.enabled ?? true
}

function normalizeSubscriptionRows(rows: Array<Record<string, unknown>>): SubscriptionRow[] {
  return rows.map((row) => ({
    type: String(row.type ?? row.messageType ?? ''),
    source: String(row.source ?? ''),
    siteEnabled: Boolean(row.siteEnabled),
    smsEnabled: Boolean(row.smsEnabled),
    mailEnabled: Boolean(row.mailEnabled),
    wxEnabled: Boolean(row.wxEnabled),
    dingEnabled: Boolean(row.dingEnabled),
  }))
}

async function ensureUserOptionsLoaded(): Promise<void> {
  userOptions.value = await getUsers()
  if (!subscriptionTargetUser.value) {
    subscriptionTargetUser.value = currentUsername()
  }
}

async function loadSubscriptionRows(targetUsername?: string): Promise<void> {
  const username = targetUsername || subscriptionTargetUser.value || currentUsername()
  subscriptionTargetUser.value = username
  const rows = await getSubscriptions(username)
  subscriptionRows.value = normalizeSubscriptionRows(rows)
  syncSubscriptionToggles()
}

async function loadCatalogRows(): Promise<void> {
  catalogRows.value = await getCatalogs()
}

async function loadProcessRows(): Promise<void> {
  processRows.value = await getProcesses()
}

async function loadDutyRows(): Promise<void> {
  const [groups, shifts] = await Promise.all([getDutyGroups(), getDutyShifts()])
  dutyGroupRows.value = groups
  dutyShiftRows.value = shifts
}

async function loadMenuRows(): Promise<void> {
  menuConfig.value = await getMenuConfig()
}

async function loadNavigationRows(): Promise<void> {
  navigationConfig.value = await getNavigationConfig()
}

async function loadRouteData(path = currentRoute.value): Promise<void> {
  if (shell.value.platformKey !== 'workbench') {
    return
  }

  try {
    if (path === '/msgCenter/subscriptionSetting') {
      await ensureUserOptionsLoaded()
      await loadSubscriptionRows(subscriptionTargetUser.value || currentUsername())
      return
    }
    if (path === '/serveSetting/orderDirectory') {
      await Promise.all([ensureUserOptionsLoaded(), loadProcessRows(), loadCatalogRows()])
      return
    }
    if (path === '/serveSetting/orderProcess') {
      await loadProcessRows()
      return
    }
    if (path === '/serveSetting/dutyManage') {
      await Promise.all([ensureUserOptionsLoaded(), loadDutyRows()])
      return
    }
    if (path === '/serveSetting/navSetting') {
      await Promise.all([loadMenuRows(), loadNavigationRows()])
    }
  } catch (error) {
    await handleRequestFailure(error, '加载管理数据失败')
  }
}

async function loadBootstrap(): Promise<void> {
  loading.value = true
  loadError.value = ''

  try {
    bootstrap.value = await getWorkbenchBootstrap(route.path)
    storedUser.value = {
      id: storedUser.value?.id ?? 0,
      username: bootstrap.value.shell.user.account,
      displayName: bootstrap.value.shell.user.displayName,
    }

    const routeKey = resolvePageKey(route.path, normalizeBasePath(bootstrap.value.shell.basePath))
    if (!bootstrap.value.pages[routeKey]) {
      await router.replace(resolveAppRoute('/', bootstrap.value.shell.basePath))
      return
    }
    await loadRouteData(routeKey)
  } catch (error) {
    const message = error instanceof Error ? error.message : '加载平台失败'
    if (message === 'UNAUTHORIZED') {
      clearSession()
      await router.replace('/login')
      return
    }
    loadError.value = message
  } finally {
    loading.value = false
  }
}

function navigateTo(path: string): void {
  const target = resolveAppRoute(path)
  if (target === normalizeAbsolutePath(route.path)) {
    return
  }
  router.push(target)
}

function togglePlatformNavigation(): void {
  navigationPanelOpen.value = !navigationPanelOpen.value
}

function selectPlatformNavigation(item: NavigationItem): void {
  navigationPanelOpen.value = false
  navigateTo(item.link)
}

function logout(): void {
  clearSession()
  router.replace('/login')
}

function handleAction(action: string): void {
  if (action === '提单' || action === '提交工单') {
    orderModalOpen.value = true
    return
  }

  if (action === '全部开') {
    void updateAllSubscriptions(true)
    return
  }

  if (action === '全部关') {
    void updateAllSubscriptions(false)
    return
  }

  if (action === '退出登录') {
    logout()
    return
  }

  if (action === '工作台') {
    navigateTo('/o/workbench/')
    return
  }

  if (action === '控制台') {
    navigateTo('/o/bastion/console/console')
    return
  }

  if (action === '消息') {
    navigateTo('/o/workbench/msgCenter/messageManage')
    return
  }

  if (action === '资源平台概览') {
    navigateTo('/o/cmdb/')
    return
  }

  if (action === shell.value.platformButton) {
    togglePlatformNavigation()
    return
  }

  showToast(`${action} 为演示动作，后续可接入真实业务逻辑`)
}

async function submitWorkOrder(payload: WorkOrderPayload): Promise<void> {
  try {
    const response = await createWorkOrder(payload)
    orderModalOpen.value = false
    await loadBootstrap()
    await router.push(resolveAppRoute('/personSetting/orderManage'))
    showToast(`工单 ${response.orderNo} 已创建`)
  } catch (error) {
    const message = error instanceof Error ? error.message : '工单创建失败'
    if (message === 'UNAUTHORIZED') {
      clearSession()
      await router.replace('/login')
      return
    }
    showToast(message)
  }
}

function closeOrderDetail(): void {
  orderDetailOpen.value = false
  orderDetail.value = null
  orderComment.value = ''
  selectedOrderNo.value = ''
}

async function openOrderDetail(orderNo: string): Promise<void> {
  selectedOrderNo.value = orderNo
  orderComment.value = ''
  orderDetailOpen.value = true
  orderDetailLoading.value = true

  try {
    orderDetail.value = await getWorkOrderDetail(orderNo)
  } catch (error) {
    const message = error instanceof Error ? error.message : '加载工单详情失败'
    if (message === 'UNAUTHORIZED') {
      clearSession()
      await router.replace('/login')
      return
    }
    showToast(message)
  } finally {
    orderDetailLoading.value = false
  }
}

async function submitOrderTransition(action: string): Promise<void> {
  if (!selectedOrderNo.value) {
    return
  }

  orderTransitioning.value = true
  try {
    const response = await transitionWorkOrder(selectedOrderNo.value, {
      action,
      comment: orderComment.value || undefined,
    })
    await loadBootstrap()
    await openOrderDetail(selectedOrderNo.value)
    orderComment.value = ''
    showToast(`工单 ${response.orderNo} 已更新为 ${response.status}`)
  } catch (error) {
    const message = error instanceof Error ? error.message : '工单流转失败'
    if (message === 'UNAUTHORIZED') {
      clearSession()
      await router.replace('/login')
      return
    }
    showToast(message)
  } finally {
    orderTransitioning.value = false
  }
}

function syncSubscriptionToggles(): void {
  subscriptionRows.value.forEach((row, index) => {
    toggles[subscriptionToggleKey(index, 'site')] = Boolean(row.siteEnabled)
    toggles[subscriptionToggleKey(index, 'sms')] = Boolean(row.smsEnabled)
    toggles[subscriptionToggleKey(index, 'mail')] = Boolean(row.mailEnabled)
    toggles[subscriptionToggleKey(index, 'wx')] = Boolean(row.wxEnabled)
    toggles[subscriptionToggleKey(index, 'dd')] = Boolean(row.dingEnabled)
  })
}

async function updateSubscriptionToggle(index: number, field: string, channel: string, value: boolean): Promise<void> {
  setToggle(subscriptionToggleKey(index, channel), value)

  const row = subscriptionRows.value[index] as Record<string, any> | undefined
  if (!row) {
    return
  }

  row[field] = value
  await persistSubscriptions(subscriptionRows.value as Array<Record<string, any>>)
}

async function updateAllSubscriptions(value: boolean): Promise<void> {
  subscriptionRows.value.forEach((row, index) => {
    row.siteEnabled = value
    row.smsEnabled = value
    row.mailEnabled = value
    row.wxEnabled = value
    row.dingEnabled = value
    toggles[subscriptionToggleKey(index, 'site')] = value
    toggles[subscriptionToggleKey(index, 'sms')] = value
    toggles[subscriptionToggleKey(index, 'mail')] = value
    toggles[subscriptionToggleKey(index, 'wx')] = value
    toggles[subscriptionToggleKey(index, 'dd')] = value
  })

  await persistSubscriptions(subscriptionRows.value as Array<Record<string, any>>)
}

async function persistSubscriptions(rows: Array<Record<string, any>>): Promise<void> {
  try {
    await saveSubscriptionsForUser(
      rows.map((row) => ({
        messageType: String(row.type ?? ''),
        source: String(row.source ?? ''),
        siteEnabled: Boolean(row.siteEnabled),
        smsEnabled: Boolean(row.smsEnabled),
        mailEnabled: Boolean(row.mailEnabled),
        wxEnabled: Boolean(row.wxEnabled),
        dingEnabled: Boolean(row.dingEnabled),
      })),
      subscriptionTargetUser.value || currentUsername(),
    )
    showToast('订阅设置已保存')
  } catch (error) {
    await handleRequestFailure(error, '保存订阅设置失败')
  }
}

async function changeSubscriptionTarget(username: string): Promise<void> {
  subscriptionTargetUser.value = username
  await loadSubscriptionRows(username)
}

function openCatalogEditor(row?: WorkOrderCatalog): void {
  resetCatalogForm(row)
  catalogDialogOpen.value = true
}

async function saveCatalogEditor(): Promise<void> {
  catalogSaving.value = true
  try {
    await saveCatalog({ ...catalogForm })
    catalogDialogOpen.value = false
    await Promise.all([loadCatalogRows(), loadBootstrap()])
    showToast('工单目录已保存')
  } catch (error) {
    await handleRequestFailure(error, '保存工单目录失败')
  } finally {
    catalogSaving.value = false
  }
}

async function removeCatalogRow(catalogCode: string): Promise<void> {
  if (!window.confirm(`确认删除目录 ${catalogCode} 吗？`)) {
    return
  }
  try {
    await deleteCatalog(catalogCode)
    await Promise.all([loadCatalogRows(), loadBootstrap()])
    showToast('工单目录已删除')
  } catch (error) {
    await handleRequestFailure(error, '删除工单目录失败')
  }
}

function addProcessNode(): void {
  processForm.nodes.push({
    nodeCode: `NODE_${processForm.nodes.length + 1}`,
    nodeName: `节点${processForm.nodes.length + 1}`,
    nodeType: 'TASK',
    sortNo: processForm.nodes.length + 1,
    assigneeRole: 'ENGINEER',
    nextApproveNode: '',
    nextRejectNode: '',
  })
}

function removeProcessNode(index: number): void {
  if (processForm.nodes.length <= 1) {
    return
  }
  processForm.nodes.splice(index, 1)
}

function normalizeProcessNodes(): ProcessNodePayload[] {
  const normalizedCodes = processForm.nodes.map((node, index) => {
    const source = (node.nodeCode || `NODE_${index + 1}`).trim()
    return source.replace(/\s+/g, '_').toUpperCase()
  })
  return processForm.nodes.map((node, index) => {
    const nodeCode = normalizedCodes[index] ?? `NODE_${index + 1}`
    return {
      ...node,
      nodeCode,
      nodeName: node.nodeName || `节点${index + 1}`,
      nodeType: node.nodeType || 'TASK',
      sortNo: index + 1,
      assigneeRole: node.assigneeRole || 'ENGINEER',
      nextApproveNode: node.nextApproveNode || normalizedCodes[index + 1] || '',
      nextRejectNode: node.nextRejectNode || '',
    }
  })
}

async function openProcessDesigner(processCode?: string): Promise<void> {
  processDesignerOpen.value = true
  processDesignerLoading.value = Boolean(processCode)
  try {
    if (processCode) {
      const detail = await getProcessDetail(processCode)
      resetProcessForm(detail)
    } else {
      resetProcessForm()
    }
  } catch (error) {
    await handleRequestFailure(error, '加载流程设计器失败')
  } finally {
    processDesignerLoading.value = false
  }
}

async function saveProcessDesigner(status?: string): Promise<void> {
  processSaving.value = true
  try {
    await saveProcessDefinition({
      ...processForm,
      status: status || processForm.status,
      nodes: normalizeProcessNodes(),
    })
    processDesignerOpen.value = false
    await Promise.all([loadProcessRows(), loadBootstrap()])
    showToast(status === '已发布' ? '流程已发布' : '流程设计已保存')
  } catch (error) {
    await handleRequestFailure(error, '保存流程失败')
  } finally {
    processSaving.value = false
  }
}

async function publishProcess(processCode: string): Promise<void> {
  try {
    const detail = await getProcessDetail(processCode)
    resetProcessForm(detail)
    await saveProcessDesigner('已发布')
  } catch (error) {
    await handleRequestFailure(error, '发布流程失败')
  }
}

function openDutyGroupEditor(row?: DutyGroup): void {
  resetDutyGroupForm(row)
  dutyGroupDialogOpen.value = true
}

async function saveDutyGroupEditor(): Promise<void> {
  dutyGroupSaving.value = true
  try {
    await saveDutyGroup({ ...(dutyGroupForm as SaveDutyGroupPayload) })
    dutyGroupDialogOpen.value = false
    await Promise.all([loadDutyRows(), loadBootstrap()])
    showToast('值班组已保存')
  } catch (error) {
    await handleRequestFailure(error, '保存值班组失败')
  } finally {
    dutyGroupSaving.value = false
  }
}

function openDutyShiftEditor(row?: DutyShift): void {
  resetDutyShiftForm(row)
  dutyShiftDialogOpen.value = true
}

async function saveDutyShiftEditor(): Promise<void> {
  dutyShiftSaving.value = true
  try {
    await saveDutyShift({ ...(dutyShiftForm as SaveDutyShiftPayload) })
    dutyShiftDialogOpen.value = false
    await Promise.all([loadDutyRows(), loadBootstrap()])
    showToast('值班排班已保存')
  } catch (error) {
    await handleRequestFailure(error, '保存值班排班失败')
  } finally {
    dutyShiftSaving.value = false
  }
}

function openMenuEditor(row?: MenuConfigItem): void {
  resetMenuForm(row)
  menuDialogOpen.value = true
}

async function saveMenuEditor(): Promise<void> {
  menuSaving.value = true
  try {
    await saveMenuConfig({ ...menuForm, roleCodes: [...menuForm.roleCodes] })
    menuDialogOpen.value = false
    await Promise.all([loadMenuRows(), loadBootstrap()])
    showToast('菜单权限已保存')
  } catch (error) {
    await handleRequestFailure(error, '保存菜单权限失败')
  } finally {
    menuSaving.value = false
  }
}

async function removeMenuItem(menuCode: string): Promise<void> {
  if (!window.confirm(`确认删除菜单 ${menuCode} 吗？`)) {
    return
  }
  try {
    await deleteMenuConfig(menuCode)
    await Promise.all([loadMenuRows(), loadBootstrap()])
    showToast('菜单已删除')
  } catch (error) {
    await handleRequestFailure(error, '删除菜单失败')
  }
}

function openNavigationGroupEditor(group?: NavigationGroup): void {
  resetNavigationGroupForm(group)
  navigationGroupDialogOpen.value = true
}

async function saveNavigationGroupEditor(): Promise<void> {
  navigationGroupSaving.value = true
  try {
    await saveNavigationGroup({ ...navigationGroupForm })
    navigationGroupDialogOpen.value = false
    await Promise.all([loadNavigationRows(), loadBootstrap()])
    showToast('导航分组已保存')
  } catch (error) {
    await handleRequestFailure(error, '保存导航分组失败')
  } finally {
    navigationGroupSaving.value = false
  }
}

function openNavigationItemEditor(item?: NavigationItem, groupCode?: string): void {
  resetNavigationItemForm(item, groupCode)
  navigationItemDialogOpen.value = true
}

async function saveNavigationItemEditor(): Promise<void> {
  navigationItemSaving.value = true
  try {
    await saveNavigationItem({ ...navigationItemForm })
    navigationItemDialogOpen.value = false
    await Promise.all([loadNavigationRows(), loadBootstrap()])
    showToast('导航入口已保存')
  } catch (error) {
    await handleRequestFailure(error, '保存导航入口失败')
  } finally {
    navigationItemSaving.value = false
  }
}

async function removeNavigationEntry(itemCode: string): Promise<void> {
  if (!window.confirm(`确认删除导航 ${itemCode} 吗？`)) {
    return
  }
  try {
    await deleteNavigationItem(itemCode)
    await Promise.all([loadNavigationRows(), loadBootstrap()])
    showToast('导航入口已删除')
  } catch (error) {
    await handleRequestFailure(error, '删除导航入口失败')
  }
}

async function changeNavigationFavorite(item: NavigationItem): Promise<void> {
  try {
    const result: NavigationFavoriteState = await toggleNavigationFavorite(item.itemCode)
    await Promise.all([
      loadBootstrap(),
      currentRoute.value === '/serveSetting/navSetting' ? loadNavigationRows() : Promise.resolve(),
    ])
    showToast(result.favorite ? `${item.name} 已加入收藏` : `${item.name} 已取消收藏`)
  } catch (error) {
    await handleRequestFailure(error, '更新收藏失败')
  }
}

function hasMenuRole(menu: MenuConfigItem, roleCode: string): boolean {
  return menu.roleCodes.includes(roleCode)
}

watch(
  () => route.path,
  async (path) => {
    navigationPanelOpen.value = false
    if (!bootstrap.value || path === '/login') {
      return
    }

    const nextBasePath = inferBasePath(path)
    if (nextBasePath !== platformBasePath.value) {
      await loadBootstrap()
      return
    }

    const routeKey = resolvePageKey(path, platformBasePath.value)
    if (!bootstrap.value.pages[routeKey]) {
      await router.replace(resolveAppRoute('/'))
      return
    }
    await loadRouteData(routeKey)
  },
)

onMounted(async () => {
  await loadBootstrap()
  clockTimer = window.setInterval(() => {
    currentTime.value = formatNow()
  }, 1000)
})

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
  }
  if (toastTimer) {
    window.clearTimeout(toastTimer)
  }
})
</script>

<template>
  <div v-if="loading && !bootstrap" class="loading-screen">
    <div class="loading-card">
      <h2>正在加载平台</h2>
      <p>前端正在拉取 Spring Boot 聚合接口，并按当前平台装配对应的菜单、页面和导航数据。</p>
    </div>
  </div>

  <div v-else-if="loadError && !bootstrap" class="error-screen">
    <div class="error-card">
      <h2>平台加载失败</h2>
      <p>{{ loadError }}</p>
      <button class="action-btn primary" @click="loadBootstrap">重新加载</button>
    </div>
  </div>

  <div v-else>
    <header class="topbar">
      <div class="brand">
        <span class="brand-icon">▣</span>
        <span class="brand-title">{{ shell.productName }}</span>
      </div>

      <button class="platform-switch" :class="{ active: navigationPanelOpen }" @click="togglePlatformNavigation()">
        <span class="platform-switch-icon">≡</span>
        <span>{{ shell.platformButton }}</span>
        <span class="platform-switch-caret">▼</span>
      </button>

      <div class="topbar-right">
        <nav class="top-nav">
          <button v-for="item in shell.topNav" :key="item" class="top-nav-item" @click="handleAction(item)">
            {{ item }}
            <span v-if="item === '消息'" class="nav-badge">{{ messageBadge }}</span>
          </button>
        </nav>

        <div class="user-chip" @click="handleAction('退出登录')">
          <span class="avatar">{{ shell.user.account.slice(0, 1).toUpperCase() }}</span>
          <span class="user-caret">▼</span>
        </div>
      </div>
    </header>

    <PlatformNavigationPanel
      v-if="navigationPanelOpen"
      :active-link="activePlatformLink"
      :groups="navigationGroups"
      :favorites="favoriteNavigations"
      @close="navigationPanelOpen = false"
      @select="selectPlatformNavigation"
    />

    <div class="layout">
      <aside class="sidebar">
        <section v-for="group in menuGroups" :key="group.group" class="menu-group">
          <div v-if="group.group" class="menu-group-title">{{ group.group }}</div>
          <div class="menu-items">
            <template v-for="item in group.items" :key="item.route || item.label">
              <div v-if="!item.route" class="menu-subgroup-title">{{ item.label }}</div>
              <button
                v-else
                class="menu-item"
                :class="{ active: isActiveRoute(item.route) }"
                @click="navigateTo(item.route)"
              >
                <span class="menu-icon">{{ item.label.slice(0, 1) }}</span>
                <span>{{ item.label }}</span>
              </button>
            </template>
          </div>
        </section>
      </aside>

      <main class="content">
        <section class="page-header panel">
          <div class="page-header-main">
            <h1>{{ page.title }}</h1>
            <div class="breadcrumb">{{ (page.breadcrumb || []).join('  >  ') }}</div>
            <div class="page-intro">
              {{ page.intro }}
              <button class="inline-link" @click="handleAction('了解更多')">了解更多</button>
            </div>
          </div>
        </section>

        <section v-if="page.kind === 'overview'" class="page-stack">
          <section class="subtabs panel">
            <div class="chip-row">
              <button
                v-for="item in ['总览', '基础设施监控']"
                :key="item"
                class="tab-chip"
                :class="{ active: activeTab('overview-tabs', '总览') === item }"
                @click="updateTab('overview-tabs', item)"
              >
                {{ item }}
              </button>
            </div>
          </section>

          <section class="overview-grid">
            <article class="panel span-2">
              <div class="panel-header">
                <h3>资源平台概览</h3>
                <button class="inline-link" @click="handleAction('资源平台概览')">查看更多 ></button>
              </div>
              <div class="metric-grid">
                <div
                  v-for="item in overviewData.infrastructure || []"
                  :key="item.label"
                  class="metric-card"
                  :style="{ '--accent': item.color }"
                >
                  <div class="metric-label">{{ item.label }}</div>
                  <div class="metric-value">{{ item.value }}</div>
                </div>
              </div>
            </article>

            <article class="panel">
              <div class="panel-header">
                <h3>云管平台概览</h3>
                <button class="inline-link" @click="handleAction('云管平台概览')">查看更多 ></button>
              </div>
              <div class="ring-layout">
                <div class="ring-chart multicolor">
                  <div class="ring-center">
                    <strong>0台</strong>
                    <span>主机数量</span>
                  </div>
                </div>
                <div class="legend-grid compact">
                  <div v-for="item in (overviewData.cloudVendors || []).slice(0, 8)" :key="item.label" class="legend-item">
                    <span class="legend-dot" :style="{ background: item.color }"></span>
                    <span>{{ item.label }}</span>
                  </div>
                </div>
              </div>
            </article>

            <article class="panel side-stack">
              <div class="profile-card">
                <div class="profile-avatar">{{ shell.user.account.slice(0, 1).toUpperCase() }}</div>
                <div>
                  <div class="profile-name">{{ shell.user.account }}</div>
                  <div class="profile-subtitle">中文名：{{ shell.user.displayName }}</div>
                </div>
              </div>
              <div class="mini-banner">{{ currentTime }}</div>
              <div class="todo-box">
                <div class="todo-title">待办事项</div>
                <div class="todo-stats">
                  <div class="todo-stat">
                    <strong>{{ overviewData.userPanel?.todoSubmitted ?? 0 }}</strong>
                    <span>我的提单</span>
                  </div>
                  <div class="todo-stat">
                    <strong>{{ overviewData.userPanel?.todoPending ?? 0 }}</strong>
                    <span>我的待办</span>
                  </div>
                </div>
              </div>
            </article>

            <article class="panel">
              <div class="panel-header">
                <h3>管控平台概览</h3>
                <button class="inline-link" @click="handleAction('管控平台概览')">查看更多 ></button>
              </div>
              <div class="ring-layout">
                <div class="ring-chart blue">
                  <div class="ring-center">
                    <strong>8台</strong>
                    <span>总数量</span>
                  </div>
                </div>
                <div class="summary-list">
                  <div v-for="item in overviewData.controlPlatform || []" :key="item.label" class="summary-card">
                    <span class="legend-dot" :style="{ background: item.color }"></span>
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }} 台</strong>
                  </div>
                </div>
              </div>
            </article>

            <article class="panel">
              <div class="panel-header">
                <h3>基础监控概览</h3>
                <button class="inline-link" @click="handleAction('基础监控概览')">查看更多 ></button>
              </div>
              <div class="ring-layout">
                <div class="ring-chart green">
                  <div class="ring-center">
                    <strong>0台</strong>
                    <span>主机数量</span>
                  </div>
                </div>
                <div class="severity-list">
                  <div v-for="item in overviewData.monitorLevels || []" :key="item.label" class="severity-item">
                    <span>{{ item.label }}</span>
                    <div class="severity-line">
                      <i :style="{ background: item.color, width: item.value === 0 ? '100%' : `${item.value * 10}%` }"></i>
                    </div>
                    <span>{{ item.value }} 台</span>
                  </div>
                </div>
              </div>
            </article>

            <article class="panel">
              <div class="panel-header">
                <h3>值班看板</h3>
                <button class="inline-link" @click="handleAction('值班看板')">查看更多 ></button>
              </div>
              <div class="duty-cards">
                <div
                  v-for="item in overviewData.dutyBoard || []"
                  :key="`${item.label}-${item.date}`"
                  class="duty-card"
                  :class="item.accent"
                >
                  <div class="duty-card-head">
                    <span>{{ item.label }}</span>
                    <span>{{ item.date }}</span>
                  </div>
                  <div class="duty-card-body">{{ item.text }}</div>
                </div>
              </div>
            </article>

            <article class="panel">
              <div class="panel-header">
                <h3>作业平台概览</h3>
                <button class="inline-link" @click="handleAction('作业平台概览')">查看更多 ></button>
              </div>
              <div class="stat-grid four">
                <div v-for="item in overviewData.jobStats || []" :key="item.label" class="stat-box">
                  <strong>{{ item.value }}</strong>
                  <span>{{ item.label }}</span>
                </div>
              </div>
            </article>

            <article class="panel">
              <div class="panel-header">
                <h3>堡垒机概览</h3>
                <button class="inline-link" @click="handleAction('堡垒机概览')">查看更多 ></button>
              </div>
              <div class="stat-grid">
                <div v-for="item in overviewData.bastionStats || []" :key="item.label" class="stat-box">
                  <strong>{{ item.value }}</strong>
                  <span>{{ item.label }}</span>
                </div>
              </div>
            </article>

            <article class="panel">
              <div class="panel-header">
                <h3>最新消息</h3>
                <button class="inline-link" @click="navigateTo('/msgCenter/messageManage')">查看更多 ></button>
              </div>
              <div class="feed-list">
                <div v-for="item in overviewData.latestMessages || []" :key="item.title + item.time" class="feed-item">
                  <div class="feed-title">{{ item.title }}</div>
                  <div class="feed-time">{{ item.time }}</div>
                </div>
              </div>
            </article>

            <article class="panel span-2">
              <div class="panel-header">
                <h3>大屏展示</h3>
                <button class="inline-link" @click="navigateTo('/bigScreen/bigScreenList')">查看更多 ></button>
              </div>
              <div class="screen-grid">
                <div
                  v-for="item in overviewData.screenCards || []"
                  :key="item.title"
                  class="screen-card"
                  :class="item.accent"
                >
                  <div class="screen-title">{{ item.title }}</div>
                  <div class="screen-desc">{{ item.desc }}</div>
                  <button class="action-btn primary" @click="handleAction('进入大屏')">进入大屏</button>
                </div>
              </div>
            </article>
          </section>
        </section>

        <section v-else-if="page.kind === 'servicePortal'" class="panel page-block">
          <div class="chip-row">
            <button
              v-for="item in page.tabs || []"
              :key="item"
              class="tab-chip"
              :class="{ active: activeTab('servicePortal', page.tabs?.[0] || '') === item }"
              @click="updateTab('servicePortal', item)"
            >
              {{ item }}
            </button>
          </div>

          <div class="toolbar">
            <input class="text-input" placeholder="输入名称或描述等关键词搜索" />
            <div class="toolbar-actions">
              <button class="action-btn" @click="handleAction('搜 索')">搜 索</button>
              <button class="action-btn primary" @click="handleAction('提单')">提单</button>
            </div>
          </div>

          <div class="portal-layout">
            <aside class="category-list">
              <button
                v-for="(item, index) in page.serviceCategories || []"
                :key="item"
                class="category-item"
                :class="{ active: index === 0 }"
                @click="handleAction(String(item))"
              >
                <span>{{ item }}</span>
              </button>
            </aside>

            <div class="portal-cards">
              <article v-for="service in page.services || []" :key="service.title" class="service-card">
                <div class="service-type">{{ service.type }}</div>
                <h3>{{ service.title }}</h3>
                <p>{{ service.description }}</p>
                <div class="service-footer">
                  <span>{{ service.count }}</span>
                  <button class="action-btn primary" @click="handleAction('提单')">提单</button>
                </div>
              </article>
            </div>
          </div>
        </section>

        <section v-else-if="page.kind === 'orders'" class="panel page-block">
          <div class="chip-row">
            <button
              v-for="item in page.tabs || []"
              :key="item"
              class="tab-chip"
              :class="{ active: activeTab('orders', page.tabs?.[0] || '') === item }"
              @click="updateTab('orders', item)"
            >
              {{ item }}
            </button>
          </div>

          <div class="notice success-light">展示所有我提交的工单，右上角提交工单按钮可以快速提交工单。</div>

          <div class="toolbar">
            <select class="select-input">
              <option>工单标题</option>
            </select>
            <input class="text-input" placeholder="请输入关键字搜索" />
            <button class="inline-action" @click="handleAction('高级搜索')">高级搜索</button>
            <div class="toolbar-actions">
              <button class="action-btn" @click="loadBootstrap">刷新</button>
              <button class="action-btn primary" @click="handleAction('提交工单')">提交工单</button>
            </div>
          </div>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th></th>
                  <th>单号</th>
                  <th>工单标题</th>
                  <th>工单类型</th>
                  <th>提单人</th>
                  <th>当前进度</th>
                  <th>状态</th>
                  <th>预计处理时间</th>
                  <th>创建时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in page.rows || []" :key="row.id">
                  <td><input type="checkbox" /></td>
                  <td class="linkish">{{ row.id }}</td>
                  <td>{{ row.title }}</td>
                  <td>{{ row.type }}</td>
                  <td>{{ row.creator }}</td>
                  <td><StatusBadge :label="row.progress" /></td>
                  <td><StatusBadge :label="row.status" /></td>
                  <td>{{ row.eta }}</td>
                  <td>{{ row.createdAt }}</td>
                  <td class="op-cell">
                    <button class="mini-link" @click="openOrderDetail(String(row.id))">详情</button>
                    <button class="mini-link" @click="openOrderDetail(String(row.id))">{{ orderPrimaryActionLabel(String(row.status)) }}</button>
                    <button class="mini-link" @click="handleAction('撤单')">撤单</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-else-if="page.kind === 'tasks'" class="panel page-block">
          <div class="chip-row">
            <button
              v-for="item in page.tabs || []"
              :key="item"
              class="tab-chip"
              :class="{ active: activeTab('tasks', page.tabs?.[0] || '') === item }"
              @click="updateTab('tasks', item)"
            >
              {{ item }}
            </button>
          </div>

          <div class="toolbar">
            <input class="text-input" placeholder="请输入关键字搜索" />
            <div class="toolbar-actions">
              <button class="action-btn" @click="loadBootstrap">刷新</button>
              <button class="action-btn primary" @click="handleAction('新建')">新建</button>
            </div>
          </div>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>任务编号</th>
                  <th>任务标题</th>
                  <th>任务来源</th>
                  <th>关联工单</th>
                  <th>状态</th>
                  <th>处理人</th>
                  <th>优先级</th>
                  <th>创建人</th>
                  <th>创建时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in page.rows || []" :key="row.id">
                  <td class="linkish">{{ row.id }}</td>
                  <td>{{ row.title }}</td>
                  <td>{{ row.source }}</td>
                  <td>{{ row.ticket }}</td>
                  <td><StatusBadge :label="row.status" /></td>
                  <td>{{ row.assignee }}</td>
                  <td>{{ row.priority }}</td>
                  <td>{{ row.creator }}</td>
                  <td>{{ row.createdAt }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-else-if="page.kind === 'duty'" class="panel page-block">
          <div class="toolbar split">
            <div class="toolbar-inline">
              <div class="chip-row">
                <button
                  v-for="item in ['日历视图', '列表视图']"
                  :key="item"
                  class="tab-chip"
                  :class="{ active: activeTab('dutyView', '日历视图') === item }"
                  @click="updateTab('dutyView', item)"
                >
                  {{ item }}
                </button>
              </div>
              <div class="chip-row">
                <button
                  v-for="item in ['周', '月']"
                  :key="item"
                  class="tab-chip"
                  :class="{ active: activeTab('dutyRange', '周') === item }"
                  @click="updateTab('dutyRange', item)"
                >
                  {{ item }}
                </button>
              </div>
            </div>
            <div class="toolbar-actions">
              <button class="action-btn" @click="loadBootstrap">刷新</button>
              <button class="action-btn primary" @click="handleAction('打卡')">打卡</button>
            </div>
          </div>

          <div class="calendar-grid">
            <div
              v-for="shift in page.shifts || []"
              :key="`${shift.date}-${shift.group}`"
              class="calendar-card"
              :class="{ today: String(shift.status).includes('今日') }"
            >
              <div class="calendar-card-head">
                <strong>{{ shift.date }}</strong>
                <span>{{ shift.label }}</span>
              </div>
              <div class="calendar-card-body">
                <div>{{ shift.group }}</div>
                <div>{{ shift.owner }}</div>
                <div>{{ shift.time }}</div>
              </div>
              <StatusBadge :label="shift.status" />
            </div>
          </div>
        </section>

        <section v-else-if="page.kind === 'bigScreens'" class="panel page-block">
          <div class="screen-grid full">
            <article
              v-for="(screen, index) in page.screens || []"
              :key="screen.title"
              class="screen-card large"
              :class="`tone-${Number(index) + 1}`"
            >
              <div class="screen-tag">{{ screen.tag }}</div>
              <h3>{{ screen.title }}</h3>
              <p>{{ screen.desc }}</p>
              <button class="action-btn primary" @click="handleAction('进入大屏')">进入大屏</button>
            </article>
          </div>
        </section>

        <section v-else-if="page.kind === 'messages'" class="panel page-block">
          <div class="chip-row">
            <button
              v-for="item in (page.messageTypes || []).slice(0, 8)"
              :key="item"
              class="tab-chip"
              :class="{ active: activeTab('messageTypes', (page.messageTypes || [])[0] || '') === item }"
              @click="updateTab('messageTypes', item)"
            >
              {{ item }}
            </button>
          </div>

          <div class="toolbar">
            <select class="select-input">
              <option>全部消息</option>
            </select>
            <div class="toolbar-actions">
              <button class="action-btn" @click="loadBootstrap">刷新</button>
              <button class="action-btn" @click="handleAction('详情')">详情</button>
              <button class="action-btn" @click="handleAction('批量操作')">批量操作</button>
              <button class="action-btn" @click="handleAction('删除已读')">删除已读</button>
              <button class="action-btn" @click="handleAction('全部删除')">全部删除</button>
              <button class="action-btn primary" @click="handleAction('全部已读')">全部已读</button>
            </div>
          </div>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>标题</th>
                  <th>发送时间</th>
                  <th>类型</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in page.rows || []" :key="row.title + row.time">
                  <td>{{ row.title }}</td>
                  <td>{{ row.time }}</td>
                  <td>{{ row.type }}</td>
                  <td class="op-cell">
                    <button class="mini-link" @click="handleAction('详情')">详情</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-else-if="page.kind === 'subscriptions'" class="panel page-block">
          <div class="toolbar">
            <div class="toolbar-inline channel-list">
              <el-select
                v-model="subscriptionTargetUser"
                class="toolbar-user-select"
                placeholder="选择用户"
                @change="changeSubscriptionTarget"
              >
                <el-option
                  v-for="item in userOptions"
                  :key="item.username"
                  :label="`${item.displayName} (${item.username})`"
                  :value="item.username"
                />
              </el-select>
              <span v-for="item in ['站内消息', '短信', '电子邮箱', '企业微信', '钉钉']" :key="item" class="channel-pill">
                {{ item }}
              </span>
            </div>
            <div class="toolbar-actions">
              <button class="action-btn" @click="loadSubscriptionRows(subscriptionTargetUser)">刷新</button>
              <button class="action-btn" @click="updateAllSubscriptions(true)">全部开</button>
              <button class="action-btn danger" @click="updateAllSubscriptions(false)">全部关</button>
            </div>
          </div>

          <div class="notice success-light">当前正在管理 {{ subscriptionUserLabel || '当前用户' }} 的消息订阅策略。</div>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>消息类型</th>
                  <th>消息来源</th>
                  <th>站内消息</th>
                  <th>短信</th>
                  <th>邮箱</th>
                  <th>企业微信</th>
                  <th>钉钉</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in subscriptionRows" :key="`${row.type}-${row.source}`">
                  <td>{{ row.type }}</td>
                  <td>{{ row.source }}</td>
                  <td><ToggleSwitch :model-value="toggleValue(subscriptionToggleKey(Number(index), 'site'))" @update:model-value="updateSubscriptionToggle(Number(index), 'siteEnabled', 'site', $event)" /></td>
                  <td><ToggleSwitch :model-value="toggleValue(subscriptionToggleKey(Number(index), 'sms'))" @update:model-value="updateSubscriptionToggle(Number(index), 'smsEnabled', 'sms', $event)" /></td>
                  <td><ToggleSwitch :model-value="toggleValue(subscriptionToggleKey(Number(index), 'mail'))" @update:model-value="updateSubscriptionToggle(Number(index), 'mailEnabled', 'mail', $event)" /></td>
                  <td><ToggleSwitch :model-value="toggleValue(subscriptionToggleKey(Number(index), 'wx'))" @update:model-value="updateSubscriptionToggle(Number(index), 'wxEnabled', 'wx', $event)" /></td>
                  <td><ToggleSwitch :model-value="toggleValue(subscriptionToggleKey(Number(index), 'dd'))" @update:model-value="updateSubscriptionToggle(Number(index), 'dingEnabled', 'dd', $event)" /></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-else-if="page.kind === 'orderDirectory'" class="panel page-block">
          <div class="toolbar">
            <button class="inline-action" @click="handleAction('高级搜索')">高级搜索</button>
            <input class="text-input" placeholder="支持关键字搜索" />
            <div class="toolbar-actions">
              <button class="action-btn" @click="loadCatalogRows">刷新</button>
              <button class="action-btn primary" @click="openCatalogEditor()">新增目录</button>
            </div>
          </div>
          <div class="portal-layout">
            <aside class="category-list">
              <button
                v-for="(item, index) in page.categories || []"
                :key="item"
                class="category-item"
                :class="{ active: index === 0 }"
                @click="handleAction(String(item))"
              >
                <span>{{ item }}</span>
              </button>
            </aside>
            <div class="flex-1">
                <div class="table-wrap">
                  <table class="table">
                  <thead>
                    <tr>
                      <th>工单名称</th>
                      <th>工单类型</th>
                      <th>可见范围类型</th>
                      <th>是否上线</th>
                      <th>流程版本</th>
                      <th>SLA</th>
                      <th>负责人</th>
                      <th>创建时间</th>
                      <th>工单描述</th>
                      <th>操作</th>
                    </tr>
                    </thead>
                    <tbody>
                      <tr v-for="row in catalogRows" :key="row.catalogCode">
                        <td>{{ row.name }}</td>
                        <td>{{ row.type }}</td>
                        <td>{{ row.scope }}</td>
                        <td><StatusBadge :label="row.online ? '已上线' : '未上线'" /></td>
                        <td>{{ row.processCode }}</td>
                        <td>{{ row.slaName }}</td>
                        <td>{{ row.ownerDisplayName }}</td>
                        <td>{{ row.createdAt }}</td>
                        <td>{{ row.description }}</td>
                        <td class="op-cell">
                          <button class="mini-link" @click="openCatalogEditor(row)">编辑</button>
                          <button class="mini-link" @click="removeCatalogRow(row.catalogCode)">删除</button>
                        </td>
                      </tr>
                    </tbody>
                </table>
              </div>
            </div>
          </div>
        </section>

        <section v-else-if="page.kind === 'processes'" class="panel page-block">
          <div class="chip-row">
            <button
              v-for="item in page.tabs || []"
              :key="item"
              class="tab-chip"
              :class="{ active: activeTab('processes', page.tabs?.[0] || '') === item }"
              @click="updateTab('processes', item)"
            >
              {{ item }}
            </button>
          </div>

          <div class="toolbar">
            <button class="inline-action" @click="handleAction('高级搜索')">高级搜索</button>
            <input class="text-input" placeholder="支持关键字搜索" />
            <div class="toolbar-actions">
              <button class="action-btn" @click="loadProcessRows">刷新</button>
              <button class="action-btn primary" @click="openProcessDesigner()">新建流程</button>
            </div>
          </div>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>流程名称</th>
                  <th>负责人</th>
                  <th>创建人</th>
                  <th>更新人</th>
                  <th>状态</th>
                  <th>更新时间</th>
                  <th>描述</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in processRows" :key="row.processCode">
                  <td>{{ row.name }}</td>
                  <td>{{ row.owner }}</td>
                  <td>{{ row.creator }}</td>
                  <td>{{ row.updater }}</td>
                  <td><StatusBadge :label="row.status" /></td>
                  <td>{{ row.updatedAt }}</td>
                  <td>{{ row.description }}</td>
                  <td class="op-cell">
                    <button class="mini-link" @click="openProcessDesigner(row.processCode)">设计</button>
                    <button class="mini-link" @click="publishProcess(row.processCode)">发布</button>
                    <button class="mini-link" @click="handleAction('更多操作')">更多操作</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-else-if="page.kind === 'sla'" class="panel page-block">
          <div class="toolbar">
            <input class="text-input" placeholder="支持关键字搜索" />
          </div>
          <div class="empty-state">
            <div class="empty-illustration">SLA</div>
            <h3>暂无数据</h3>
            <p>当前还没有服务级别协议记录，可在后续版本补充响应和处理时限规则。</p>
          </div>
        </section>

        <section v-else-if="page.kind === 'apis'" class="panel page-block">
          <div class="chip-row">
            <button
              v-for="item in page.tabs || []"
              :key="item"
              class="tab-chip"
              :class="{ active: activeTab('apis', page.tabs?.[0] || '') === item }"
              @click="updateTab('apis', item)"
            >
              {{ item }}
            </button>
          </div>

          <div class="toolbar">
            <input class="text-input" placeholder="支持关键字搜索" />
            <div class="toolbar-actions">
              <button class="action-btn" @click="loadBootstrap">刷新</button>
            </div>
          </div>

          <div class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>接口名称</th>
                  <th>接口路径</th>
                  <th>启用/禁用</th>
                  <th>负责人</th>
                  <th>接入数</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in page.rows || []" :key="row.name">
                  <td>{{ row.name }}</td>
                  <td class="monospace">{{ row.path }}</td>
                  <td><StatusBadge :label="row.enabled" /></td>
                  <td>{{ row.owner }}</td>
                  <td>{{ row.count }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-else-if="page.kind === 'dutyManage'" class="panel page-block">
          <div class="chip-row">
            <button
              v-for="item in page.tabs || []"
              :key="item"
              class="tab-chip"
              :class="{ active: activeTab('dutyManage', page.tabs?.[0] || '') === item }"
              @click="updateTab('dutyManage', item)"
            >
              {{ item }}
            </button>
          </div>

          <div class="toolbar">
            <div class="toolbar-inline">
              <div class="chip-row">
                <button
                  v-for="item in ['日历视图', '列表视图']"
                  :key="item"
                  class="tab-chip"
                  :class="{ active: activeTab('dutyManageView', '日历视图') === item }"
                  @click="updateTab('dutyManageView', item)"
                >
                  {{ item }}
                </button>
              </div>
              </div>
              <div class="toolbar-actions">
                <button class="action-btn" @click="loadBootstrap">刷新</button>
                <button class="action-btn" @click="openDutyShiftEditor()">新增排班</button>
                <button class="action-btn primary" @click="openDutyGroupEditor()">新增值班组</button>
              </div>
            </div>

            <div class="calendar-grid">
              <div v-for="group in dutyGroupRows" :key="group.name" class="calendar-card group-card">
                <div class="calendar-card-head">
                  <strong>{{ group.name }}</strong>
                  <StatusBadge :label="group.coverage" />
                </div>
                <div class="calendar-card-body">
                  <div>负责人：{{ group.ownerDisplayName }}</div>
                  <div>成员数：{{ group.members }}</div>
                  <div>排班覆盖：{{ group.coverage }}</div>
                </div>
                <div class="group-actions">
                  <button class="mini-link" @click="openDutyGroupEditor(group)">编辑</button>
                  <button class="mini-link" @click="openDutyShiftEditor()">排班</button>
                </div>
              </div>
            </div>

            <div class="table-wrap duty-table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>值班组</th>
                    <th>值班日期</th>
                    <th>时间范围</th>
                    <th>负责人</th>
                    <th>状态</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="shift in dutyShiftRows" :key="shift.id">
                    <td>{{ shift.groupName }}</td>
                    <td>{{ shift.dutyDate }}</td>
                    <td>{{ shift.shiftTime }}</td>
                    <td>{{ shift.ownerDisplayName }}</td>
                    <td><StatusBadge :label="shift.status" /></td>
                    <td class="op-cell">
                      <button class="mini-link" @click="openDutyShiftEditor(shift)">编辑</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

        <section v-else-if="page.kind === 'navigation'" class="page-stack">
          <article class="panel page-block">
            <div class="panel-header">
              <div>
                <h3>平台导航总览</h3>
                <p class="panel-subtitle">头部的“平台导航”已和这里共用数据库配置，支持我的收藏和分组卡片切换。</p>
              </div>
              <div class="toolbar-actions">
                <button class="action-btn" @click="navigationPanelOpen = true">预览导航</button>
                <button class="action-btn primary" @click="openNavigationGroupEditor()">新增分组</button>
              </div>
            </div>
            <div class="summary-grid navigation-summary-grid">
              <article class="summary-panel">
                <strong>{{ navigationAdminGroups.length }}</strong>
                <span>导航分组</span>
              </article>
              <article class="summary-panel">
                <strong>{{ navigationAdminGroups.reduce((sum, group) => sum + group.rows.length, 0) }}</strong>
                <span>导航入口</span>
              </article>
              <article class="summary-panel">
                <strong>{{ favoriteNavigations.length }}</strong>
                <span>我的收藏</span>
              </article>
            </div>
          </article>

          <article v-for="group in navigationAdminGroups" :key="group.title" class="panel page-block">
            <div class="panel-header">
              <div>
                <h3>{{ group.title }}</h3>
                <p class="panel-subtitle">分组编码：{{ group.groupCode || '--' }}</p>
              </div>
              <div class="toolbar-actions">
                <button class="action-btn" @click="openNavigationGroupEditor(group)">编辑分组</button>
                <button class="action-btn primary" @click="openNavigationItemEditor(undefined, group.groupCode)">新增导航</button>
              </div>
            </div>
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>导航名称</th>
                    <th>导航图标</th>
                    <th>创建用户</th>
                    <th>导航链接</th>
                    <th>移动端</th>
                    <th>收藏</th>
                    <th>状态</th>
                    <th>描述</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in group.rows" :key="row.itemCode">
                    <td>{{ row.id }}</td>
                    <td>{{ row.name }}</td>
                    <td><span class="nav-icon-badge">{{ row.icon }}</span></td>
                    <td>{{ row.creator }}</td>
                    <td class="monospace">{{ row.link }}</td>
                    <td>{{ row.mobileVisible ? '是' : '否' }}</td>
                    <td>
                      <el-tag :type="row.favorite ? 'warning' : 'info'" effect="light" round>
                        {{ row.favorite ? '已收藏' : '未收藏' }}
                      </el-tag>
                    </td>
                    <td><StatusBadge :label="row.enabled ? '启用' : '停用'" /></td>
                    <td>{{ row.desc }}</td>
                    <td class="op-cell">
                      <button class="mini-link" @click="changeNavigationFavorite(row)">{{ row.favorite ? '取消收藏' : '加入收藏' }}</button>
                      <button class="mini-link" @click="openNavigationItemEditor(row)">编辑</button>
                      <button class="mini-link" @click="removeNavigationEntry(row.itemCode)">删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </article>

          <article class="panel page-block">
            <div class="panel-header">
              <h3>工作台菜单权限</h3>
              <button class="action-btn primary" @click="openMenuEditor()">新增菜单</button>
            </div>
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>菜单名称</th>
                    <th>菜单编码</th>
                    <th>路由</th>
                    <th>分组</th>
                    <th>显示</th>
                    <th v-for="role in menuRoles" :key="role.roleCode">{{ role.roleName }}</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="menu in menuItems" :key="menu.menuCode">
                    <td>{{ menu.label }}</td>
                    <td class="monospace">{{ menu.menuCode }}</td>
                    <td class="monospace">{{ menu.route }}</td>
                    <td>{{ menu.groupName }}</td>
                    <td><StatusBadge :label="menu.visible ? '显示' : '隐藏'" /></td>
                    <td v-for="role in menuRoles" :key="`${menu.menuCode}-${role.roleCode}`">
                      <el-tag :type="hasMenuRole(menu, role.roleCode) ? 'success' : 'info'" effect="light" round>
                        {{ hasMenuRole(menu, role.roleCode) ? '有权限' : '无权限' }}
                      </el-tag>
                    </td>
                    <td class="op-cell">
                      <button class="mini-link" @click="openMenuEditor(menu)">编辑</button>
                      <button class="mini-link" @click="removeMenuItem(menu.menuCode)">删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </article>
        </section>
        <PlatformModulePage
          v-else-if="page.kind === 'platformOverview' || page.kind === 'platformModule'"
          :page="page"
          :page-key="currentRoute"
          :platform-key="shell.platformKey"
          @action="handleAction"
          @navigate="navigateTo"
        />

        <section v-else-if="page.kind === 'platformLanding'" class="page-stack">
          <article class="panel page-block platform-landing-hero">
            <div class="platform-landing-mark">{{ page.icon || '导' }}</div>
            <div class="platform-landing-copy">
              <div class="platform-landing-tag">{{ page.groupTitle }}</div>
              <h3>{{ page.title }}</h3>
              <p>{{ page.intro }}</p>
              <div class="toolbar-actions">
                <button class="action-btn primary" @click="navigationPanelOpen = true">切换其他平台</button>
                <button class="action-btn" @click="navigateTo('/')">返回工作台</button>
              </div>
            </div>
          </article>

          <article class="panel page-block">
            <div class="panel-header">
              <h3>入口能力</h3>
              <span class="monospace">{{ page.link }}</span>
            </div>
            <div class="platform-highlight-list">
              <div v-for="item in page.highlights || []" :key="item" class="platform-highlight-item">
                <span class="platform-highlight-dot"></span>
                <span>{{ item }}</span>
              </div>
            </div>
          </article>
        </section>

        <section v-else-if="page.kind === 'systemSettings'" class="panel page-block">
          <div class="settings-grid">
            <article v-for="card in page.cards || []" :key="card.title" class="setting-card">
              <div class="setting-tag">{{ card.tag }}</div>
              <h3>{{ card.title }}</h3>
              <p>{{ card.desc }}</p>
              <button class="action-btn primary" @click="handleAction('进入配置')">进入配置</button>
            </article>
          </div>
        </section>

        <section v-else class="panel page-block">
          <div class="empty-state">
            <div class="empty-illustration">OpsAny</div>
            <h3>页面构建中</h3>
            <p>当前模块保留了导航入口，后续可以继续补充更细的业务能力和联动逻辑。</p>
          </div>
        </section>

        <footer class="footer">Copyright © 2019-2026 OpsAny. All Rights Reserved【企业专业版】</footer>
      </main>
    </div>

    <div v-if="toast" class="toast">{{ toast }}</div>

    <el-drawer
      v-model="orderDetailOpen"
      class="order-detail-drawer"
      direction="rtl"
      size="560px"
      @close="closeOrderDetail"
    >
      <template #header>
        <div class="order-detail-header">
          <div>
            <h3>{{ orderDetail?.order.title ?? '工单详情' }}</h3>
            <p>{{ orderDetail?.order.orderNo ?? selectedOrderNo }}</p>
          </div>
          <el-tag
            v-if="orderDetail"
            :type="statusTagType(orderDetail.order.status)"
            effect="light"
            round
          >
            {{ orderDetail.order.status }}
          </el-tag>
        </div>
      </template>

      <div v-if="orderDetailLoading" class="drawer-loading">
        <el-skeleton :rows="8" animated />
      </div>

      <template v-else-if="orderDetail">
        <div class="drawer-section">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单单号">{{ orderDetail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="工单类型">{{ orderDetail.order.type }}</el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ orderDetail.order.currentNodeName }}</el-descriptions-item>
            <el-descriptions-item label="当前处理人">{{ orderDetail.order.currentHandler }}</el-descriptions-item>
            <el-descriptions-item label="优先级">{{ orderDetail.order.priority }}</el-descriptions-item>
            <el-descriptions-item label="服务名称">{{ orderDetail.order.serviceName }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ orderDetail.order.creatorDisplayName }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ orderDetail.order.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ orderDetail.order.description }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="drawer-section">
          <div class="drawer-section-title">处理意见</div>
          <el-input
            v-model="orderComment"
            type="textarea"
            :rows="4"
            placeholder="可选，记录本次受理、处理或确认意见"
          />
          <p v-if="!orderActionOptions.length" class="drawer-tip">当前节点已到结束态，如需继续处理可在后端流程定义中扩展节点。</p>
        </div>

        <div class="drawer-section">
          <div class="drawer-section-title">流转记录</div>
          <el-timeline>
            <el-timeline-item
              v-for="item in orderDetail.histories"
              :key="item.id"
              :timestamp="item.createdAt"
              :type="timelineTone(item.action)"
            >
              <div class="history-card">
                <div class="history-title">{{ item.operatorDisplayName }} · {{ historyActionLabel(item.action) }}</div>
                <div class="history-status">{{ item.fromStatus || '新建' }} -> {{ item.toStatus }}</div>
                <div v-if="item.comment" class="history-comment">{{ item.comment }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>

      <div v-else class="drawer-empty">未获取到工单详情。</div>

      <template #footer>
        <div class="order-detail-footer">
          <el-button @click="closeOrderDetail">关闭</el-button>
          <el-button
            v-for="action in orderActionOptions"
            :key="action.action"
            :type="action.type"
            :loading="orderTransitioning"
            @click="submitOrderTransition(action.action)"
          >
            {{ action.label }}
          </el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="catalogDialogOpen" width="720px" class="config-dialog">
      <template #header>
        <div class="modal-header">
          <div>
            <h3>{{ catalogForm.catalogCode ? '编辑工单目录' : '新增工单目录' }}</h3>
            <p>目录配置会同步影响服务门户、提单入口和流程映射。</p>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="modal-body">
        <div class="form-grid">
          <el-form-item label="目录编码">
            <el-input v-model="catalogForm.catalogCode" :disabled="Boolean(catalogForm.catalogCode)" placeholder="留空自动生成" />
          </el-form-item>
          <el-form-item label="目录名称">
            <el-input v-model="catalogForm.name" placeholder="例如：数据库创建申请" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="目录分组">
            <el-input v-model="catalogForm.category" placeholder="默认分组" />
          </el-form-item>
          <el-form-item label="工单类型">
            <el-select v-model="catalogForm.type" class="full-width">
              <el-option label="请求管理" value="请求管理" />
              <el-option label="变更管理" value="变更管理" />
              <el-option label="事件管理" value="事件管理" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="流程编码">
            <el-select v-model="catalogForm.processCode" class="full-width">
              <el-option v-for="item in processRows" :key="item.processCode" :label="`${item.name} (${item.processCode})`" :value="item.processCode" />
            </el-select>
          </el-form-item>
          <el-form-item label="负责人">
            <el-select v-model="catalogForm.ownerUsername" class="full-width">
              <el-option v-for="item in userOptions" :key="item.username" :label="`${item.displayName} (${item.username})`" :value="item.username" />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="可见范围">
            <el-input v-model="catalogForm.scope" placeholder="全部用户 / 组织架构" />
          </el-form-item>
          <el-form-item label="SLA">
            <el-input v-model="catalogForm.slaName" placeholder="-" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="排序">
            <el-input-number v-model="catalogForm.sortNo" :min="0" class="full-width" />
          </el-form-item>
          <el-form-item label="是否上线">
            <el-switch v-model="catalogForm.online" inline-prompt active-text="上线" inactive-text="下线" />
          </el-form-item>
        </div>

        <el-form-item label="目录描述">
          <el-input v-model="catalogForm.description" type="textarea" :rows="4" placeholder="补充目录用途、适用对象和处理说明" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer">
          <el-button @click="catalogDialogOpen = false">取消</el-button>
          <el-button type="success" :loading="catalogSaving" @click="saveCatalogEditor">保存目录</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="processDesignerOpen" class="process-designer-drawer" direction="rtl" size="760px">
      <template #header>
        <div class="order-detail-header">
          <div>
            <h3>{{ processForm.name || '流程设计器' }}</h3>
            <p>{{ processForm.processCode || '新建流程' }}</p>
          </div>
          <el-tag :type="processForm.status === '已发布' ? 'success' : 'warning'" effect="light" round>
            {{ processForm.status }}
          </el-tag>
        </div>
      </template>

      <div v-if="processDesignerLoading" class="drawer-loading">
        <el-skeleton :rows="10" animated />
      </div>

      <template v-else>
        <div class="drawer-section">
          <el-form label-position="top">
            <div class="form-grid">
              <el-form-item label="流程编码">
                <el-input v-model="processForm.processCode" placeholder="例如：CHANGE_APPROVAL_FLOW" />
              </el-form-item>
              <el-form-item label="流程名称">
                <el-input v-model="processForm.name" placeholder="例如：变更审批流程" />
              </el-form-item>
            </div>
            <div class="form-grid">
              <el-form-item label="流程分类">
                <el-input v-model="processForm.category" placeholder="请求管理 / 变更管理" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="processForm.status" class="full-width">
                  <el-option label="未发布" value="未发布" />
                  <el-option label="已发布" value="已发布" />
                </el-select>
              </el-form-item>
            </div>
            <el-form-item label="流程描述">
              <el-input v-model="processForm.description" type="textarea" :rows="3" placeholder="说明流程适用范围和关键节点" />
            </el-form-item>
          </el-form>
        </div>

        <div class="drawer-section">
          <div class="panel-header">
            <h3>可视化流程</h3>
            <button class="action-btn" @click="addProcessNode">新增节点</button>
          </div>
          <div class="process-flow-board">
            <div v-for="(node, index) in processForm.nodes" :key="`${node.nodeCode}-${index}`" class="process-node-card">
              <div class="process-node-index">节点 {{ index + 1 }}</div>
              <div class="process-node-name">{{ node.nodeName || `节点${index + 1}` }}</div>
              <div class="process-node-meta">{{ node.nodeType }} · {{ node.assigneeRole }}</div>
              <div class="process-node-link">通过 -> {{ node.nextApproveNode || '自动顺延' }}</div>
              <div class="process-node-link reject">驳回 -> {{ node.nextRejectNode || '无' }}</div>
              <button v-if="processForm.nodes.length > 1" class="mini-link danger-link" @click="removeProcessNode(index)">移除</button>
            </div>
          </div>
        </div>

        <div class="drawer-section">
          <div class="drawer-section-title">节点编辑</div>
          <div v-for="(node, index) in processForm.nodes" :key="`editor-${index}`" class="node-editor-card">
            <div class="form-grid">
              <el-form-item :label="`节点编码 ${index + 1}`">
                <el-input v-model="node.nodeCode" />
              </el-form-item>
              <el-form-item label="节点名称">
                <el-input v-model="node.nodeName" />
              </el-form-item>
            </div>
            <div class="form-grid">
              <el-form-item label="节点类型">
                <el-select v-model="node.nodeType" class="full-width">
                  <el-option label="START" value="START" />
                  <el-option label="APPROVAL" value="APPROVAL" />
                  <el-option label="TASK" value="TASK" />
                  <el-option label="CONFIRM" value="CONFIRM" />
                  <el-option label="END" value="END" />
                </el-select>
              </el-form-item>
              <el-form-item label="处理角色">
                <el-select v-model="node.assigneeRole" class="full-width">
                  <el-option label="REQUESTER" value="REQUESTER" />
                  <el-option label="PLATFORM_ADMIN" value="PLATFORM_ADMIN" />
                  <el-option label="ENGINEER" value="ENGINEER" />
                  <el-option label="END" value="END" />
                </el-select>
              </el-form-item>
            </div>
            <div class="form-grid">
              <el-form-item label="通过流向">
                <el-input v-model="node.nextApproveNode" placeholder="留空按顺序串联" />
              </el-form-item>
              <el-form-item label="驳回流向">
                <el-input v-model="node.nextRejectNode" placeholder="可选" />
              </el-form-item>
            </div>
          </div>
        </div>
      </template>

      <template #footer>
        <div class="order-detail-footer">
          <el-button @click="processDesignerOpen = false">关闭</el-button>
          <el-button :loading="processSaving" @click="saveProcessDesigner()">保存草稿</el-button>
          <el-button type="success" :loading="processSaving" @click="saveProcessDesigner('已发布')">保存并发布</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="dutyGroupDialogOpen" width="640px" class="config-dialog">
      <template #header>
        <div class="modal-header">
          <div>
            <h3>{{ (dutyGroupForm as any).id ? '编辑值班组' : '新增值班组' }}</h3>
            <p>配置值班负责人、覆盖范围和成员规模。</p>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="modal-body">
        <div class="form-grid">
          <el-form-item label="值班组名称">
            <el-input v-model="dutyGroupForm.name" />
          </el-form-item>
          <el-form-item label="负责人">
            <el-select v-model="dutyGroupForm.ownerUsername" class="full-width">
              <el-option v-for="item in userOptions" :key="item.username" :label="`${item.displayName} (${item.username})`" :value="item.username" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="成员数">
            <el-input-number v-model="dutyGroupForm.members" :min="0" class="full-width" />
          </el-form-item>
          <el-form-item label="排班覆盖">
            <el-select v-model="dutyGroupForm.coverage" class="full-width">
              <el-option label="工作日" value="工作日" />
              <el-option label="7 x 24" value="7 x 24" />
              <el-option label="自定义" value="自定义" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="值班说明">
          <el-input v-model="dutyGroupForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer">
          <el-button @click="dutyGroupDialogOpen = false">取消</el-button>
          <el-button type="success" :loading="dutyGroupSaving" @click="saveDutyGroupEditor">保存值班组</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="dutyShiftDialogOpen" width="640px" class="config-dialog">
      <template #header>
        <div class="modal-header">
          <div>
            <h3>{{ (dutyShiftForm as any).id ? '编辑值班排班' : '新增值班排班' }}</h3>
            <p>排班保存后会同步到我的值班和管理视图。</p>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="modal-body">
        <div class="form-grid">
          <el-form-item label="值班组">
            <el-select v-model="dutyShiftForm.groupId" class="full-width">
              <el-option v-for="item in dutyGroupRows" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="值班日期">
            <el-date-picker v-model="dutyShiftForm.dutyDate" type="date" value-format="YYYY-MM-DD" class="full-width" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="负责人">
            <el-select v-model="dutyShiftForm.ownerUsername" class="full-width">
              <el-option v-for="item in userOptions" :key="item.username" :label="`${item.displayName} (${item.username})`" :value="item.username" />
            </el-select>
          </el-form-item>
          <el-form-item label="值班状态">
            <el-select v-model="dutyShiftForm.status" class="full-width">
              <el-option label="待值班" value="待值班" />
              <el-option label="今日当班" value="今日当班" />
              <el-option label="已完成" value="已完成" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="时间范围">
          <el-input v-model="dutyShiftForm.shiftTime" placeholder="例如：09:00 - 18:00" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer">
          <el-button @click="dutyShiftDialogOpen = false">取消</el-button>
          <el-button type="success" :loading="dutyShiftSaving" @click="saveDutyShiftEditor">保存排班</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialogOpen" width="720px" class="config-dialog">
      <template #header>
        <div class="modal-header">
          <div>
            <h3>{{ menuForm.menuCode ? '编辑菜单权限' : '新增菜单权限' }}</h3>
            <p>保存后会立即影响侧边栏菜单显示和菜单授权。</p>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="modal-body">
        <div class="form-grid">
          <el-form-item label="菜单编码">
            <el-input v-model="menuForm.menuCode" :disabled="Boolean(menuForm.menuCode)" placeholder="留空自动生成" />
          </el-form-item>
          <el-form-item label="菜单名称">
            <el-input v-model="menuForm.label" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="菜单分组">
            <el-input v-model="menuForm.groupName" />
          </el-form-item>
          <el-form-item label="菜单路由">
            <el-input v-model="menuForm.route" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="分组排序">
            <el-input-number v-model="menuForm.groupSortNo" :min="0" class="full-width" />
          </el-form-item>
          <el-form-item label="菜单排序">
            <el-input-number v-model="menuForm.sortNo" :min="0" class="full-width" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="图标">
            <el-input v-model="menuForm.icon" placeholder="可选" />
          </el-form-item>
          <el-form-item label="权限编码">
            <el-input v-model="menuForm.permissionCode" placeholder="例如：order:view" />
          </el-form-item>
        </div>
        <el-form-item label="授权角色">
          <el-select v-model="menuForm.roleCodes" class="full-width" multiple collapse-tags>
            <el-option v-for="role in menuRoles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见状态">
          <el-switch v-model="menuForm.visible" inline-prompt active-text="显示" inactive-text="隐藏" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer">
          <el-button @click="menuDialogOpen = false">取消</el-button>
          <el-button type="success" :loading="menuSaving" @click="saveMenuEditor">保存菜单</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="navigationGroupDialogOpen" width="560px" class="config-dialog">
      <template #header>
        <div class="modal-header">
          <div>
            <h3>{{ navigationGroupForm.groupCode ? '编辑导航分组' : '新增导航分组' }}</h3>
            <p>分组会直接展示在顶部平台导航弹层中。</p>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="modal-body">
        <div class="form-grid">
          <el-form-item label="分组编码">
            <el-input v-model="navigationGroupForm.groupCode" :disabled="Boolean(navigationGroupForm.groupCode)" placeholder="留空自动生成" />
          </el-form-item>
          <el-form-item label="分组名称">
            <el-input v-model="navigationGroupForm.title" />
          </el-form-item>
        </div>
        <el-form-item label="排序">
          <el-input-number v-model="navigationGroupForm.sortNo" :min="1" class="full-width" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer">
          <el-button @click="navigationGroupDialogOpen = false">取消</el-button>
          <el-button type="success" :loading="navigationGroupSaving" @click="saveNavigationGroupEditor">保存分组</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="navigationItemDialogOpen" width="720px" class="config-dialog">
      <template #header>
        <div class="modal-header">
          <div>
            <h3>{{ navigationItemForm.itemCode ? '编辑导航入口' : '新增导航入口' }}</h3>
            <p>这里配置的链接会同时驱动顶部平台导航和导航管理页。</p>
          </div>
        </div>
      </template>

      <el-form label-position="top" class="modal-body">
        <div class="form-grid">
          <el-form-item label="导航编码">
            <el-input v-model="navigationItemForm.itemCode" :disabled="Boolean(navigationItemForm.itemCode)" placeholder="留空自动生成" />
          </el-form-item>
          <el-form-item label="所属分组">
            <el-select v-model="navigationItemForm.groupCode" class="full-width">
              <el-option v-for="group in navigationGroupOptions" :key="group.value" :label="group.label" :value="group.value" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="导航名称">
            <el-input v-model="navigationItemForm.name" />
          </el-form-item>
          <el-form-item label="导航图标">
            <el-input v-model="navigationItemForm.icon" maxlength="2" placeholder="例如：工" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="导航链接">
            <el-input v-model="navigationItemForm.link" placeholder="例如：/o/cmdb/" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="navigationItemForm.sortNo" :min="1" class="full-width" />
          </el-form-item>
        </div>
        <el-form-item label="描述">
          <el-input v-model="navigationItemForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="移动端展示">
            <el-switch v-model="navigationItemForm.mobileVisible" inline-prompt active-text="是" inactive-text="否" />
          </el-form-item>
          <el-form-item label="启用状态">
            <el-switch v-model="navigationItemForm.enabled" inline-prompt active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="modal-footer">
          <el-button @click="navigationItemDialogOpen = false">取消</el-button>
          <el-button type="success" :loading="navigationItemSaving" @click="saveNavigationItemEditor">保存导航</el-button>
        </div>
      </template>
    </el-dialog>

    <CreateOrderModal
      :open="orderModalOpen"
      :services="serviceOptions"
      @close="orderModalOpen = false"
      @submit="submitWorkOrder"
    />
  </div>
</template>
