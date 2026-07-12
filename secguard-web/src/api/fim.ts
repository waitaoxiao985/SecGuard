import api from './index'

export const fimApi = {
  /** FIM 事件列表（分页 + 过滤） */
  list(params: Record<string, any>) {
    return api.get('/api/events/fim', { params })
  },

  /** FIM 统计 */
  stats(trendDays = 7) {
    return api.get('/api/events/fim/stats', { params: { trendDays } })
  },

  /** 基线查询 */
  baseline(params: Record<string, any>) {
    return api.get('/api/events/fim/baseline', { params })
  },

  /** 重置基线 */
  resetBaseline(agentId: number) {
    return api.put(`/api/events/fim/baseline/${agentId}/reset`)
  }
}
