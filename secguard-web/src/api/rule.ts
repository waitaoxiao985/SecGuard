import api from './index'

export const ruleApi = {
  /** 规则列表 */
  list() {
    return api.get('/api/rules')
  },

  /** 规则统计 */
  stats() {
    return api.get('/api/rules/stats')
  },

  /** 热重载 */
  reload() {
    return api.post('/api/rules/reload')
  }
}
