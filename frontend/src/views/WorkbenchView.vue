<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import CreateOrderModal from '../components/CreateOrderModal.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ToggleSwitch from '../components/ToggleSwitch.vue'
import { createWorkOrder, getWorkbenchBootstrap, getWorkOrderDetail, saveSubscriptions, transitionWorkOrder } from '../lib/api'
import { clearSession, getStoredUser } from '../lib/session'
import type {
  PageData,
  PlatformBootstrap,
  WorkOrderDetailResponse,
  WorkOrderPayload,
} from '../types/platform'

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

let clockTimer: number | undefined
let toastTimer: number | undefined

const fallbackShell = {
  productName: '工作台',
  platformButton: '平台导航',
  topNav: ['控制台', '工作台', '消息', '支持'],
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
const messageBadge = computed(() => {
  const rows = ((bootstrap.value?.pages['/msgCenter/messageManage'] as any)?.rows ?? []) as any[]
  return rows.length > 0 ? String(rows.length) : '1'
})

const currentRoute = computed(() => {
  if (!bootstrap.value) {
    return route.path === '/login' ? '/' : route.path
  }

  if (bootstrap.value.pages[route.path]) {
    return route.path
  }

  return '/'
})

const page = computed<Record<string, any>>(
  () => (bootstrap.value?.pages[currentRoute.value] ?? bootstrap.value?.pages['/'] ?? fallbackPage) as Record<string, any>,
)

const serviceOptions = computed(() => {
  const services = ((bootstrap.value?.pages['/personSetting/serviceFolder'] as any)?.services ?? []) as Array<{ title: string }>
  return services.map((item) => item.title)
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

async function loadBootstrap(): Promise<void> {
  loading.value = true
  loadError.value = ''

  try {
    bootstrap.value = await getWorkbenchBootstrap()
    storedUser.value = {
      id: storedUser.value?.id ?? 0,
      username: bootstrap.value.shell.user.account,
      displayName: bootstrap.value.shell.user.displayName,
    }
    syncSubscriptionToggles()

    if (!bootstrap.value.pages[route.path] && route.path !== '/') {
      await router.replace('/')
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : '加载工作台失败'
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
  if (path === currentRoute.value) {
    return
  }
  router.push(path)
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

  showToast(`${action} 为演示动作，后续可接入真实业务逻辑`)
}

async function submitWorkOrder(payload: WorkOrderPayload): Promise<void> {
  try {
    const response = await createWorkOrder(payload)
    orderModalOpen.value = false
    await loadBootstrap()
    await router.push('/personSetting/orderManage')
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
  const rows = ((bootstrap.value?.pages['/msgCenter/subscriptionSetting'] as any)?.rows ?? []) as Array<Record<string, any>>
  rows.forEach((row, index) => {
    toggles[subscriptionToggleKey(index, 'site')] = Boolean(row.siteEnabled)
    toggles[subscriptionToggleKey(index, 'sms')] = Boolean(row.smsEnabled)
    toggles[subscriptionToggleKey(index, 'mail')] = Boolean(row.mailEnabled)
    toggles[subscriptionToggleKey(index, 'wx')] = Boolean(row.wxEnabled)
    toggles[subscriptionToggleKey(index, 'dd')] = Boolean(row.dingEnabled)
  })
}

async function updateSubscriptionToggle(index: number, field: string, channel: string, value: boolean): Promise<void> {
  setToggle(subscriptionToggleKey(index, channel), value)

  const rows = ((bootstrap.value?.pages['/msgCenter/subscriptionSetting'] as any)?.rows ?? []) as Array<Record<string, any>>
  const row = rows[index]
  if (!row) {
    return
  }

  row[field] = value
  await persistSubscriptions(rows)
}

async function updateAllSubscriptions(value: boolean): Promise<void> {
  const rows = ((bootstrap.value?.pages['/msgCenter/subscriptionSetting'] as any)?.rows ?? []) as Array<Record<string, any>>
  rows.forEach((row, index) => {
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

  await persistSubscriptions(rows)
}

async function persistSubscriptions(rows: Array<Record<string, any>>): Promise<void> {
  try {
    await saveSubscriptions(
      rows.map((row) => ({
        messageType: String(row.type ?? ''),
        source: String(row.source ?? ''),
        siteEnabled: Boolean(row.siteEnabled),
        smsEnabled: Boolean(row.smsEnabled),
        mailEnabled: Boolean(row.mailEnabled),
        wxEnabled: Boolean(row.wxEnabled),
        dingEnabled: Boolean(row.dingEnabled),
      })),
    )
    showToast('订阅设置已保存')
  } catch (error) {
    const message = error instanceof Error ? error.message : '保存订阅设置失败'
    showToast(message)
  }
}

watch(
  () => route.path,
  async (path) => {
    if (!bootstrap.value || path === '/login') {
      return
    }
    if (!bootstrap.value.pages[path] && path !== '/') {
      await router.replace('/')
    }
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
      <h2>正在加载工作台</h2>
      <p>前端正在拉取 Spring Boot 聚合接口，并同步 MySQL / Redis / RabbitMQ 驱动出来的数据。</p>
    </div>
  </div>

  <div v-else-if="loadError && !bootstrap" class="error-screen">
    <div class="error-card">
      <h2>工作台加载失败</h2>
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

      <button class="platform-switch" @click="handleAction(shell.platformButton)">
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

    <div class="layout">
      <aside class="sidebar">
        <section v-for="group in menuGroups" :key="group.group" class="menu-group">
          <div class="menu-group-title">{{ group.group }}</div>
          <div class="menu-items">
            <button
              v-for="item in group.items"
              :key="item.route"
              class="menu-item"
              :class="{ active: isActiveRoute(item.route) }"
              @click="navigateTo(item.route)"
            >
              <span class="menu-icon">{{ item.label.slice(0, 1) }}</span>
              <span>{{ item.label }}</span>
            </button>
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
              <span v-for="item in ['站内消息', '短信', '电子邮箱', '企业微信', '钉钉']" :key="item" class="channel-pill">
                {{ item }}
              </span>
            </div>
            <div class="toolbar-actions">
              <button class="action-btn" @click="updateAllSubscriptions(true)">全部开</button>
              <button class="action-btn danger" @click="updateAllSubscriptions(false)">全部关</button>
            </div>
          </div>

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
                <tr v-for="(row, index) in page.rows || []" :key="row.type">
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
                    <tr v-for="row in page.rows || []" :key="row.name">
                      <td>{{ row.name }}</td>
                      <td>{{ row.type }}</td>
                      <td>{{ row.scope }}</td>
                      <td><StatusBadge :label="row.online" /></td>
                      <td>{{ row.version }}</td>
                      <td>{{ row.sla }}</td>
                      <td>{{ row.owner }}</td>
                      <td>{{ row.createdAt }}</td>
                      <td>{{ row.desc }}</td>
                      <td class="op-cell">
                        <button class="mini-link" @click="handleAction('编辑')">编辑</button>
                        <button class="mini-link" @click="handleAction('删除')">删除</button>
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
                <tr v-for="row in page.rows || []" :key="row.name">
                  <td>{{ row.name }}</td>
                  <td>{{ row.owner }}</td>
                  <td>{{ row.creator }}</td>
                  <td>{{ row.updater }}</td>
                  <td><StatusBadge :label="row.status" /></td>
                  <td>{{ row.updatedAt }}</td>
                  <td>{{ row.desc }}</td>
                  <td class="op-cell">
                    <button class="mini-link" @click="handleAction('设计')">设计</button>
                    <button class="mini-link" @click="handleAction('发布')">发布</button>
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
              <button class="action-btn" @click="handleAction('操作记录')">操作记录</button>
            </div>
          </div>

          <div class="calendar-grid">
            <div v-for="group in page.groups || []" :key="group.name" class="calendar-card group-card">
              <div class="calendar-card-head">
                <strong>{{ group.name }}</strong>
                <StatusBadge :label="group.coverage" />
              </div>
              <div class="calendar-card-body">
                <div>负责人：{{ group.owner }}</div>
                <div>成员数：{{ group.members }}</div>
                <div>排班覆盖：{{ group.coverage }}</div>
              </div>
              <div class="group-actions">
                <button class="mini-link" @click="handleAction('查看日历')">查看日历</button>
                <button class="mini-link" @click="handleAction('排班模板')">排班模板</button>
              </div>
            </div>
          </div>
        </section>

        <section v-else-if="page.kind === 'navigation'" class="page-stack">
          <article v-for="group in navigationGroups" :key="group.title" class="panel page-block">
            <div class="group-header">⌄ {{ group.title }}</div>
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>导航名称</th>
                    <th>导航图标</th>
                    <th>创建用户</th>
                    <th>导航链接</th>
                    <th>移动端是否展示</th>
                    <th>描述</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in group.rows" :key="row.id">
                    <td>{{ row.id }}</td>
                    <td>{{ row.name }}</td>
                    <td><span class="nav-icon-badge">{{ row.icon }}</span></td>
                    <td>{{ row.creator }}</td>
                    <td class="monospace">{{ row.link }}</td>
                    <td>{{ row.mobile }}</td>
                    <td>{{ row.desc }}</td>
                    <td>--</td>
                  </tr>
                </tbody>
              </table>
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

    <CreateOrderModal
      :open="orderModalOpen"
      :services="serviceOptions"
      @close="orderModalOpen = false"
      @submit="submitWorkOrder"
    />
  </div>
</template>
