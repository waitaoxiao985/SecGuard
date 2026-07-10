import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'

export interface UserInfo {
  id: number
  username: string
  role: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('sg_token') || '')
  const user = ref<UserInfo | null>(
    localStorage.getItem('sg_user')
      ? JSON.parse(localStorage.getItem('sg_user')!)
      : null
  )

  const isLoggedIn = computed(() => !!token.value)

  async function login(username: string, password: string) {
    const data: any = await authApi.login(username, password)
    token.value = data.token
    user.value = {
      id: data.userId || 0,
      username: data.username || username,
      role: data.role || 'ADMIN'
    }
    localStorage.setItem('sg_token', data.token)
    localStorage.setItem('sg_user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('sg_token')
    localStorage.removeItem('sg_user')
  }

  return { token, user, isLoggedIn, login, logout }
})
