<template>
  <div class="space-y-6">
    <!-- 统计卡片 -->
    <div class="grid grid-cols-4 gap-4">
      <div v-for="card in statCards" :key="card.title"
        class="bg-white rounded-lg shadow-sm p-5 border border-gray-100 hover:shadow-md transition-shadow">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">{{ card.title }}</p>
            <p class="text-3xl font-bold mt-1" :class="card.color">{{ card.value }}</p>
          </div>
          <el-icon :size="40" :color="card.iconColor">
            <component :is="card.icon" />
          </el-icon>
        </div>
        <p class="text-xs text-gray-400 mt-2">{{ card.subtitle }}</p>
      </div>
    </div>

    <!-- 告警趋势图 + 严重等级分布 -->
    <div class="grid grid-cols-3 gap-4">
      <div class="col-span-2 bg-white rounded-lg shadow-sm p-5 border border-gray-100">
        <h3 class="text-base font-semibold text-gray-700 mb-4">24 小时告警趋势</h3>
        <v-chart :option="trendOption" style="height: 280px" autoresize />
      </div>
      <div class="bg-white rounded-lg shadow-sm p-5 border border-gray-100">
        <h3 class="text-base font-semibold text-gray-700 mb-4">告警严重等级分布</h3>
        <v-chart :option="severityOption" style="height: 280px" autoresize />
      </div>
    </div>

    <!-- MITRE ATT&CK 热力图 -->
    <div class="bg-white rounded-lg shadow-sm p-5 border border-gray-100">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-base font-semibold text-gray-700">MITRE ATT&CK 战术分布</h3>
        <el-tag type="info" size="small">{{ mitreTotal }} 条告警含 MITRE 标记</el-tag>
      </div>
      <div v-if="mitreTacticData.length > 0">
        <v-chart :option="mitreOption" style="height: 300px" autoresize />
      </div>
      <div v-else class="text-center text-gray-400 py-8 text-sm">
        暂无 MITRE ATT&CK 映射数据
      </div>
    </div>

    <!-- 最新告警 -->
    <div class="bg-white rounded-lg shadow-sm p-5 border border-gray-100">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <h3 class="text-base font-semibold text-gray-700">最新告警</h3>
          <span v-if="wsConnected" class="flex items-center gap-1 text-xs text-green-500">
            <span class="inline-block w-2 h-2 rounded-full bg-green-500 animate-pulse" />
            实时
          </span>
        </div>
        <el-button text type="primary" @click="$router.push('/alerts')">查看全部</el-button>
      </div>
      <el-table :data="recentAlerts" stripe size="small" :show-header="true">
        <el-table-column prop="id" label="#" width="60" />
        <el-table-column prop="ruleName" label="规则名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="severity" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="severityTag(row.severity)" size="small">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { dashboardApi } from '../api/dashboard'

use([CanvasRenderer, LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

// 统计数据
const alertStats = ref<any>({})
const agentStats = ref<any>({})
const vulnStats = ref<any>({})
const fimStats = ref<any>({})
const alertTrend = ref<any[]>([])
const recentAlerts = ref<any[]>([])
const loading = ref(false)

// MITRE ATT&CK
const mitreData = ref<any>({ byTactic: {}, byTechnique: {}, totalWithMitre: 0 })
const mitreTotal = computed(() => mitreData.value.totalWithMitre ?? 0)
const mitreTacticData = computed(() => {
  const bt = mitreData.value.byTactic ?? {}
  return Object.entries(bt)
    .map(([name, count]) => ({ name, count: count as number }))
    .sort((a, b) => b.count - a.count)
})

// WebSocket
let ws: WebSocket | null = null
const wsConnected = ref(false)
let wsReconnectTimer: ReturnType<typeof setTimeout> | null = null
const WS_RECONNECT_DELAY = 5000

const statCards = computed(() => [
  {
    title: '活跃 Agent',
    value: agentStats.value?.byStatus?.ACTIVE ?? 0,
    subtitle: `共 ${agentStats.value?.total ?? 0} 台`,
    icon: 'Monitor',
    color: 'text-green-600',
    iconColor: '#22c55e'
  },
  {
    title: '开放告警',
    value: alertStats.value?.byStatus?.OPEN ?? 0,
    subtitle: `共 ${alertStats.value?.total ?? 0} 条`,
    icon: 'Bell',
    color: 'text-red-500',
    iconColor: '#ef4444'
  },
  {
    title: '高危漏洞',
    value: vulnStats.value?.bySeverity?.CRITICAL ?? 0,
    subtitle: `OPEN: ${vulnStats.value?.open ?? 0}`,
    icon: 'WarnTriangleFilled',
    color: 'text-orange-500',
    iconColor: '#f97316'
  },
  {
    title: 'FIM 事件',
    value: fimStats.value?.totalEvents ?? 0,
    subtitle: `变更: ${fimStats.value?.byType?.MODIFIED ?? 0}`,
    icon: 'Document',
    color: 'text-blue-500',
    iconColor: '#3b82f6'
  }
])

// 趋势图配置
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: alertTrend.value.map((t: any) => t.hour),
    axisLabel: { fontSize: 11 }
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    type: 'line',
    data: alertTrend.value.map((t: any) => t.count),
    smooth: true,
    areaStyle: { color: 'rgba(99,102,241,0.1)' },
    lineStyle: { color: '#6366f1', width: 2 },
    itemStyle: { color: '#6366f1' }
  }]
}))

// 严重等级饼图
const severityOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { fontSize: 11 } },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    avoidLabelOverlap: true,
    label: { show: false },
    data: [
      { value: alertStats.value?.bySeverity?.CRITICAL ?? 0, name: 'CRITICAL', itemStyle: { color: '#dc2626' } },
      { value: alertStats.value?.bySeverity?.HIGH ?? 0, name: 'HIGH', itemStyle: { color: '#f97316' } },
      { value: alertStats.value?.bySeverity?.MEDIUM ?? 0, name: 'MEDIUM', itemStyle: { color: '#eab308' } },
      { value: alertStats.value?.bySeverity?.LOW ?? 0, name: 'LOW', itemStyle: { color: '#22c55e' } }
    ].filter(d => d.value > 0)
  }]
}))

// MITRE ATT&CK 战术分布横向柱状图
const mitreOption = computed(() => {
  const data = mitreTacticData.value
  const names = data.map(d => d.name).reverse()
  const counts = data.map(d => d.count).reverse()
  const maxCount = Math.max(...counts, 1)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 160, right: 40, top: 10, bottom: 20 },
    xAxis: {
      type: 'value',
      minInterval: 1,
      max: maxCount + 1
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 11, color: '#4f46e5', fontWeight: 'bold' }
    },
    series: [{
      type: 'bar',
      data: counts.map((v: number) => ({
        value: v,
        itemStyle: {
          color: v >= 10 ? '#dc2626' : v >= 5 ? '#f97316' : v >= 2 ? '#eab308' : '#6366f1',
          borderRadius: [0, 4, 4, 0]
        }
      })),
      barMaxWidth: 24,
      label: {
        show: true,
        position: 'right',
        fontSize: 12,
        fontWeight: 'bold',
        color: '#374151'
      }
    }]
  }
})

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function severityTag(s: string): TagType {
  const map: Record<string, TagType> = { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: 'primary', LOW: 'success' }
  return map[s] || 'info'
}

function statusTag(s: string): TagType {
  const map: Record<string, TagType> = { OPEN: 'danger', ACKNOWLEDGED: 'warning', RESOLVED: 'success', FALSE_POSITIVE: 'info' }
  return map[s] || 'info'
}

// WebSocket 连接管理
function connectWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  const wsUrl = `${protocol}//${host}/ws/alerts`

  try {
    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      wsConnected.value = true
      console.log('[WS] Connected to', wsUrl)
    }

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'new_alert' && msg.data) {
          // 将新告警插入列表顶部
          recentAlerts.value = [msg.data, ...recentAlerts.value.slice(0, 19)]
          // 刷新统计
          refreshStats()
        }
      } catch (e) {
        console.warn('[WS] Failed to parse message:', e)
      }
    }

    ws.onclose = () => {
      wsConnected.value = false
      console.log('[WS] Disconnected, reconnecting in', WS_RECONNECT_DELAY, 'ms')
      wsReconnectTimer = setTimeout(connectWebSocket, WS_RECONNECT_DELAY)
    }

    ws.onerror = (err) => {
      console.warn('[WS] Error:', err)
      ws?.close()
    }
  } catch (e) {
    console.warn('[WS] Failed to connect:', e)
    wsReconnectTimer = setTimeout(connectWebSocket, WS_RECONNECT_DELAY)
  }
}

function disconnectWebSocket() {
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer)
    wsReconnectTimer = null
  }
  if (ws) {
    ws.close()
    ws = null
  }
  wsConnected.value = false
}

async function refreshStats() {
  try {
    const [a, m] = await Promise.allSettled([
      dashboardApi.getAlertStats(),
      dashboardApi.getMitreStats()
    ])
    if (a.status === 'fulfilled') alertStats.value = a.value
    if (m.status === 'fulfilled') mitreData.value = m.value as any
  } catch { /* ignore */ }
}

onMounted(async () => {
  loading.value = true
  try {
    const [a, ag, v, f, t, r, m] = await Promise.allSettled([
      dashboardApi.getAlertStats(),
      dashboardApi.getAgentStats(),
      dashboardApi.getVulnStats(),
      dashboardApi.getFimStats(),
      dashboardApi.getAlertTrend(),
      dashboardApi.getRecentAlerts(8),
      dashboardApi.getMitreStats()
    ])
    if (a.status === 'fulfilled') alertStats.value = a.value
    if (ag.status === 'fulfilled') agentStats.value = ag.value
    if (v.status === 'fulfilled') vulnStats.value = v.value
    if (f.status === 'fulfilled') fimStats.value = f.value
    if (t.status === 'fulfilled') alertTrend.value = t.value as unknown as any[]
    if (r.status === 'fulfilled') {
      const rv = r.value as any
      recentAlerts.value = rv?.content ?? (Array.isArray(rv) ? rv : [])
    }
    if (m.status === 'fulfilled') mitreData.value = m.value as any
  } finally {
    loading.value = false
  }

  // 建立 WebSocket 连接实现实时推送
  connectWebSocket()
})

onUnmounted(() => {
  disconnectWebSocket()
})
</script>
