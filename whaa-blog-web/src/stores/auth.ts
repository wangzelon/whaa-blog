import { computed, ref } from 'vue'

export interface AuthUser {
  userId: number
  username: string
  nickname: string
  role: 'ADMIN' | 'USER'
}

function readUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem('whaa_user')
    return raw ? JSON.parse(raw) as AuthUser : null
  } catch {
    return null
  }
}

export const currentUser = ref<AuthUser | null>(readUser())
export const isLoggedIn = computed(() => Boolean(localStorage.getItem('whaa_token') && currentUser.value))

export function saveAuth(user: AuthUser, accessToken: string, refreshToken: string) {
  localStorage.setItem('whaa_token', accessToken)
  localStorage.setItem('whaa_refresh_token', refreshToken)
  localStorage.setItem('whaa_user', JSON.stringify(user))
  currentUser.value = user
}

export function clearAuth() {
  localStorage.removeItem('whaa_token')
  localStorage.removeItem('whaa_refresh_token')
  localStorage.removeItem('whaa_user')
  currentUser.value = null
}
