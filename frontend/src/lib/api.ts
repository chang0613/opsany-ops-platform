import { clearSession, getToken } from './session'
import type {
  DutyGroup,
  DutyShift,
  LoginResponse,
  MenuConfigItem,
  MenuConfigResponse,
  MessageSubscriptionPayload,
  PlatformBootstrap,
  ProcessDefinition,
  ProcessDefinitionDetail,
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

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

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

export function login(username: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function getCurrentUser(): Promise<UserProfile> {
  return request<UserProfile>('/api/auth/me')
}

export function getWorkbenchBootstrap(): Promise<PlatformBootstrap> {
  return request<PlatformBootstrap>('/api/workbench/bootstrap')
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
