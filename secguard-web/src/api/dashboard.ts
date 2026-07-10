import api from './index'

export const dashboardApi = {
  /** 告警统计 */
  getAlertStats() {
    return api.get('/api/alerts/stats')
  },

  /** Agent 统计 */
  getAgentStats() {
    return api.get('/api/agents/stats')
  },

  /** 漏洞统计 */
  getVulnStats() {
    return api.get('/api/vulnerabilities/stats')
  },

  /** FIM 统计 */
  getFimStats() {
    return api.get('/api/events/fim/stats')
  },

  /** 最近 24h 告警趋势 (每小时) */
  getAlertTrend() {
    return api.get('/api/alerts/trend')
  },

  /** 最新告警列表 */
  getRecentAlerts(limit = 10) {
    return api.get('/api/alerts', { params: { page: 0, size: limit } })
  }
}
