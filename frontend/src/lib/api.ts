import { clearSession, getToken } from './session'
import type {
  DutyGroup,
  DutyShift,
  LoginResponse,
  MenuConfigItem,
  MenuConfigResponse,
  NavigationConfigResponse,
  NavigationFavoriteState,
  MessageSubscriptionPayload,
  PlatformPageStatePayload,
  PlatformBootstrap,
  ProcessDefinition,
  ProcessDefinitionDetail,
  SaveNavigationGroupPayload,
  SaveNavigationItemPayload,
  SaveDutyGroupPayload,
  SaveDutyShiftPayload,
  SaveProcessDefinitionPayload,
  SaveWorkOrderCatalogPayload,
  UserOption,
  UserProfile,
  WorkOrderCatalog,
  WorkOrderDetailResponse,
  WorkOrderPayload,
  WorkOrderTransitionPayload,
} from '../types/platform'
import type { AiConversation, AiJob, AiKnowledgeEntry, AiMessage, AiRuntimeConfig, AiRuntimeConfigUpdate, AiRuntimeTestResult } from '../types/ai'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export interface StreamCallbacks {
  onStep?: (payload: Record<string, unknown>) => void
  onStepProgress?: (payload: Record<string, unknown>) => void
  onDelta?: (payload: Record<string, unknown>) => void
  onDone?: (payload: Record<string, unknown>) => void
  onError?: (payload: Record<string, unknown>) => void
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  })

  if (response.status === 401) {
    clearSession()
    throw new Error('UNAUTHORIZED')
  }

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || '请求失败')
  }

  if (response.status === 204) {
    return undefined as T
  }

  const payload = await response.text()
  return (payload ? JSON.parse(payload) : undefined) as T
}

async function streamRequest(path: string, body: Record<string, unknown>, callbacks: StreamCallbacks): Promise<void> {
  const headers = new Headers()
  headers.set('Accept', 'text/event-stream')
  headers.set('Content-Type', 'application/json')

  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  })

  if (response.status === 401) {
    clearSession()
    throw new Error('UNAUTHORIZED')
  }

  if (!response.ok || !response.body) {
    const message = await response.text()
    throw new Error(message || '流式请求失败')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const emitEvent = (rawEvent: string): void => {
    const lines = rawEvent.split(/\r?\n/)
    let eventName = 'message'
    const dataLines: string[] = []

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    }

    if (!dataLines.length) {
      return
    }

    const payloadText = dataLines.join('\n')
    const payload = payloadText ? JSON.parse(payloadText) as Record<string, unknown> : {}

    if (eventName === 'step') {
      callbacks.onStep?.(payload)
      return
    }
    if (eventName === 'step-progress') {
      callbacks.onStepProgress?.(payload)
      return
    }
    if (eventName === 'delta') {
      callbacks.onDelta?.(payload)
      return
    }
    if (eventName === 'done') {
      callbacks.onDone?.(payload)
      return
    }
    if (eventName === 'error') {
      callbacks.onError?.(payload)
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split(/\r?\n\r?\n/)
    buffer = parts.pop() ?? ''
    for (const part of parts) {
      emitEvent(part)
    }
  }

  if (buffer.trim()) {
    emitEvent(buffer)
  }
}

export function login(username: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function getCurrentUser(): Promise<UserProfile> {
  return request<UserProfile>('/api/auth/me')
}

export function getWorkbenchBootstrap(path?: string): Promise<PlatformBootstrap> {
  const query = path ? `?path=${encodeURIComponent(path)}` : ''
  return request<PlatformBootstrap>(`/api/workbench/bootstrap${query}`)
}

export function getUsers(): Promise<UserOption[]> {
  return request<UserOption[]>('/api/workbench/users')
}

export function createWorkOrder(payload: WorkOrderPayload): Promise<{ orderNo: string; title: string; status: string }> {
  return request('/api/workbench/orders', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getWorkOrderDetail(orderNo: string): Promise<WorkOrderDetailResponse> {
  return request(`/api/workbench/orders/${orderNo}`)
}

export function transitionWorkOrder(
  orderNo: string,
  payload: WorkOrderTransitionPayload,
): Promise<{ orderNo: string; title: string; status: string }> {
  return request(`/api/workbench/orders/${orderNo}/transition`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function saveSubscriptions(payload: MessageSubscriptionPayload[]): Promise<void> {
  return request('/api/workbench/subscriptions', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function getSubscriptions(username?: string): Promise<Array<Record<string, unknown>>> {
  const query = username ? `?username=${encodeURIComponent(username)}` : ''
  return request<Array<Record<string, unknown>>>(`/api/workbench/subscriptions${query}`)
}

export function saveSubscriptionsForUser(payload: MessageSubscriptionPayload[], username?: string): Promise<void> {
  const query = username ? `?username=${encodeURIComponent(username)}` : ''
  return request(`/api/workbench/subscriptions${query}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function getCatalogs(): Promise<WorkOrderCatalog[]> {
  return request<WorkOrderCatalog[]>('/api/workbench/catalogs')
}

export function saveCatalog(payload: SaveWorkOrderCatalogPayload): Promise<WorkOrderCatalog> {
  return request<WorkOrderCatalog>('/api/workbench/catalogs', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function deleteCatalog(catalogCode: string): Promise<void> {
  return request(`/api/workbench/catalogs/${catalogCode}`, {
    method: 'DELETE',
  })
}

export function getProcesses(): Promise<ProcessDefinition[]> {
  return request<ProcessDefinition[]>('/api/workbench/processes')
}

export function getProcessDetail(processCode: string): Promise<ProcessDefinitionDetail> {
  return request<ProcessDefinitionDetail>(`/api/workbench/processes/${processCode}`)
}

export function saveProcessDefinition(payload: SaveProcessDefinitionPayload): Promise<ProcessDefinition> {
  return request<ProcessDefinition>('/api/workbench/processes', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getDutyGroups(): Promise<DutyGroup[]> {
  return request<DutyGroup[]>('/api/workbench/duty/groups')
}

export function getDutyShifts(): Promise<DutyShift[]> {
  return request<DutyShift[]>('/api/workbench/duty/shifts')
}

export function saveDutyGroup(payload: SaveDutyGroupPayload): Promise<DutyGroup> {
  return request<DutyGroup>('/api/workbench/duty/groups', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function saveDutyShift(payload: SaveDutyShiftPayload): Promise<DutyShift> {
  return request<DutyShift>('/api/workbench/duty/shifts', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getMenuConfig(): Promise<MenuConfigResponse> {
  return request<MenuConfigResponse>('/api/workbench/menu-config')
}

export function saveMenuConfig(payload: MenuConfigItem): Promise<MenuConfigItem> {
  return request<MenuConfigItem>('/api/workbench/menu-config/menus', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function deleteMenuConfig(menuCode: string): Promise<void> {
  return request(`/api/workbench/menu-config/menus/${menuCode}`, {
    method: 'DELETE',
  })
}

export function getNavigationConfig(): Promise<NavigationConfigResponse> {
  return request<NavigationConfigResponse>('/api/workbench/navigation-config')
}

export function saveNavigationGroup(payload: SaveNavigationGroupPayload): Promise<{ groupCode: string; title: string }> {
  return request('/api/workbench/navigation-config/groups', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function saveNavigationItem(payload: SaveNavigationItemPayload): Promise<void> {
  return request('/api/workbench/navigation-config/items', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function deleteNavigationItem(itemCode: string): Promise<void> {
  return request(`/api/workbench/navigation-config/items/${itemCode}`, {
    method: 'DELETE',
  })
}

export function toggleNavigationFavorite(itemCode: string): Promise<NavigationFavoriteState> {
  return request<NavigationFavoriteState>(`/api/workbench/navigation-config/favorites/${itemCode}`, {
    method: 'POST',
  })
}

export function savePlatformPageState(payload: PlatformPageStatePayload): Promise<void> {
  return request('/api/workbench/platform-pages/state', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}


export function getAiConfig(): Promise<AiRuntimeConfig> {
  return request<AiRuntimeConfig>('/api/ai/config')
}

export function updateAiConfig(payload: AiRuntimeConfigUpdate): Promise<AiRuntimeConfig> {
  return request<AiRuntimeConfig>('/api/ai/config', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function testAiConfig(payload: AiRuntimeConfigUpdate): Promise<AiRuntimeTestResult> {
  return request<AiRuntimeTestResult>('/api/ai/config/test', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function listAiConversations(): Promise<AiConversation[]> {
  return request<AiConversation[]>('/api/ai/conversations')
}

export function createAiConversation(title?: string): Promise<AiConversation> {
  return request<AiConversation>('/api/ai/conversations', {
    method: 'POST',
    body: JSON.stringify({ title }),
  })
}

export function listAiMessages(conversationId: number): Promise<AiMessage[]> {
  return request<AiMessage[]>(`/api/ai/conversations/${conversationId}/messages`)
}

export function sendAiMessage(conversationId: number, content: string): Promise<{ message: AiMessage }> {
  return request<{ message: AiMessage }>(`/api/ai/conversations/${conversationId}/messages`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
}

export function sendAiMessageStream(conversationId: number, content: string, callbacks: StreamCallbacks): Promise<void> {
  return streamRequest(`/api/ai/conversations/${conversationId}/messages/stream`, { content }, callbacks)
}

export function listAiJobs(): Promise<AiJob[]> {
  return request<AiJob[]>('/api/ai/jobs')
}

export function createAiJob(jobType: string, inputJson?: string): Promise<AiJob> {
  return request<AiJob>('/api/ai/jobs', {
    method: 'POST',
    body: JSON.stringify({ jobType, inputJson }),
  })
}

export function listAiKnowledge(): Promise<AiKnowledgeEntry[]> {
  return request<AiKnowledgeEntry[]>('/api/ai/knowledge')
}

export function createAiKnowledge(payload: Partial<AiKnowledgeEntry>): Promise<AiKnowledgeEntry> {
  return request<AiKnowledgeEntry>('/api/ai/knowledge', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function deleteAiKnowledge(id: number): Promise<void> {
  return request<void>(`/api/ai/knowledge/${id}`, { method: 'DELETE' })
}


