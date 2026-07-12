import api from './index'

export const inventoryApi = {
  /** 资产统计摘要 */
  stats() {
    return api.get('/api/inventory/stats')
  },

  /** Agent 资产概要 */
  summary(agentId: number) {
    return api.get(`/api/inventory/summary/${agentId}`)
  },

  /** 最新系统信息 */
  system(agentId: number) {
    return api.get('/api/inventory/system', { params: { agentId } })
  },

  /** 系统信息历史 */
  systemHistory(agentId: number, page = 0, size = 10) {
    return api.get('/api/inventory/system/history', { params: { agentId, page, size } })
  },

  /** 软件列表 */
  software(agentId: number, page = 0, size = 20) {
    return api.get('/api/inventory/software', { params: { agentId, page, size } })
  },

  /** 软件搜索 */
  searchSoftware(keyword: string, page = 0, size = 20) {
    return api.get('/api/inventory/software/search', { params: { keyword, page, size } })
  },

  /** 端口列表 */
  ports(agentId: number, page = 0, size = 20) {
    return api.get('/api/inventory/ports', { params: { agentId, page, size } })
  },

  /** 端口号搜索 */
  searchPort(port: number) {
    return api.get('/api/inventory/ports/search', { params: { port } })
  },

  /** 网络接口 */
  networks(agentId: number, page = 0, size = 20) {
    return api.get('/api/inventory/networks', { params: { agentId, page, size } })
  }
}
