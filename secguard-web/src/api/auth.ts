import api from './index'

export const authApi = {
  /** 登录 */
  login(username: string, password: string) {
    return api.post('/api/auth/login', { username, password })
  },

  /** 获取当前用户信息 */
  getProfile() {
    return api.get('/api/auth/profile')
  }
}
