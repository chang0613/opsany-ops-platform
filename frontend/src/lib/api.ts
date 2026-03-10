import { clearSession, getToken } from './session'
import type {
  LoginResponse,
  MessageSubscriptionPayload,
  PlatformBootstrap,
  UserProfile,
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
