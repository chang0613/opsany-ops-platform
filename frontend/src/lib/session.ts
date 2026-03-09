import type { UserProfile } from '../types/platform'

const TOKEN_KEY = 'opsany-replica:token'
const USER_KEY = 'opsany-replica:user'

export function getToken(): string | null {
  return window.localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser(): UserProfile | null {
  const raw = window.localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as UserProfile
  } catch {
    return null
  }
}

export function saveSession(token: string, user: UserProfile): void {
  window.localStorage.setItem(TOKEN_KEY, token)
  window.localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession(): void {
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(USER_KEY)
}
