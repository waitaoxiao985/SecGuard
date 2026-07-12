import api from './index'

export const alertApi = {
  /** 告警列表（分页 + 过滤） */
  list(params: Record<string, any>) {
    return api.get('/api/alerts', { params })
  },

  /** 告警详情 */
  getById(id: number) {
    return api.get(`/api/alerts/${id}`)
  },

  /** 确认告警 */
  acknowledge(id: number) {
    return api.put(`/api/alerts/${id}/acknowledge`)
  },

  /** 解决告警 */
  resolve(id: number) {
    return api.put(`/api/alerts/${id}/resolve`)
  },

  /** 标记误报 */
  falsePositive(id: number) {
    return api.put(`/api/alerts/${id}/false-positive`)
  },

  /** 告警统计 */
  stats() {
    return api.get('/api/alerts/stats')
  },

  /** 24h 告警趋势 */
  trend() {
    return api.get('/api/alerts/trend')
  }
}
