<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-bold text-gray-800">主机资产</h1>
      <div class="flex gap-2">
        <el-select v-model="selectedAgentId" placeholder="选择 Agent" style="width: 260px" @change="onAgentChange">
          <el-option v-for="a in agents" :key="a.id" :label="`${a.name} (${a.hostname || a.ip})`" :value="a.id" />
        </el-select>
        <el-button type="primary" @click="loadAll" :loading="loading">
          <el-icon class="mr-1"><Search /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <!-- 全局统计 -->
    <div class="grid grid-cols-4 gap-3">
      <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-100 text-center">
        <p class="text-xs text-gray-500">已采集 Agent</p>
        <p class="text-2xl font-bold text-gray-700">{{ globalStats.agentsWithInventory ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-indigo-100 text-center">
        <p class="text-xs text-gray-500">软件总数</p>
        <p class="text-2xl font-bold text-indigo-500">{{ globalStats.totalSoftware ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-cyan-100 text-center">
        <p class="text-xs text-gray-500">端口总数</p>
        <p class="text-2xl font-bold text-cyan-500">{{ globalStats.totalPorts ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-teal-100 text-center">
        <p class="text-xs text-gray-500">网卡总数</p>
        <p class="text-2xl font-bold text-teal-500">{{ globalStats.totalNetworks ?? 0 }}</p>
      </div>
    </div>

    <!-- Agent 概要卡片 -->
    <div v-if="summary" class="bg-white rounded-lg shadow-sm p-5 border border-gray-100">
      <h3 class="text-sm font-semibold text-gray-600 mb-3">主机概要</h3>
      <div class="grid grid-cols-4 gap-x-6 gap-y-2 text-sm">
        <div><span class="text-gray-400">主机名：</span><span class="font-medium">{{ summary.hostname || '-' }}</span></div>
        <div><span class="text-gray-400">操作系统：</span><span class="font-medium">{{ summary.os || '-' }} {{ summary.osVersion || '' }}</span></div>
        <div><span class="text-gray-400">内核：</span><span class="font-medium font-mono text-xs">{{ summary.kernel || '-' }}</span></div>
        <div><span class="text-gray-400">CPU：</span><span class="font-medium">{{ summary.cpuModel || '-' }} ({{ summary.cpuCores ?? '?' }} 核)</span></div>
        <div><span class="text-gray-400">内存：</span><span class="font-medium">{{ summary.ramTotalMb ? (summary.ramTotalMb / 1024).toFixed(1) + ' GB' : '-' }}</span></div>
        <div><span class="text-gray-400">软件数：</span><span class="font-medium text-indigo-600">{{ summary.softwareCount ?? 0 }}</span></div>
        <div><span class="text-gray-400">端口数：</span><span class="font-medium text-cyan-600">{{ summary.portCount ?? 0 }}</span></div>
        <div><span class="text-gray-400">采集时间：</span><span class="font-medium text-xs">{{ summary.collectedAt || '-' }}</span></div>
      </div>
    </div>

    <!-- 四维资产面板 -->
    <div v-if="selectedAgentId" class="bg-white rounded-lg shadow-sm border border-gray-100">
      <el-tabs v-model="activeTab" class="px-4" @tab-change="onTabChange">

        <!-- 系统信息 -->
        <el-tab-pane label="系统信息" name="system">
          <el-table :data="systemInfo ? [systemInfo] : []" stripe v-loading="tabLoading">
            <el-table-column prop="hostname" label="主机名" min-width="140" />
            <el-table-column prop="os" label="操作系统" width="120" />
            <el-table-column prop="osVersion" label="版本" width="120" />
            <el-table-column prop="kernel" label="内核" width="160">
              <template #default="{ row }">
                <span class="font-mono text-xs">{{ row.kernel }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="cpuModel" label="CPU" min-width="160" show-overflow-tooltip />
            <el-table-column prop="cpuCores" label="核心" width="70" />
            <el-table-column label="内存" width="100">
              <template #default="{ row }">{{ row.ramTotalMb ? (row.ramTotalMb / 1024).toFixed(1) + ' GB' : '-' }}</template>
            </el-table-column>
            <el-table-column label="运行时间" width="120">
              <template #default="{ row }">{{ formatUptime(row.uptimeSeconds) }}</template>
            </el-table-column>
            <el-table-column prop="collectedAt" label="采集时间" width="170" />
          </el-table>

          <div class="mt-4" v-if="systemHistory.length > 0">
            <h4 class="text-sm font-semibold text-gray-500 mb-2">历史快照</h4>
            <el-table :data="systemHistory" stripe size="small">
              <el-table-column prop="hostname" label="主机名" width="130" />
              <el-table-column prop="kernel" label="内核" width="160">
                <template #default="{ row }"><span class="font-mono text-xs">{{ row.kernel }}</span></template>
              </el-table-column>
              <el-table-column prop="cpuCores" label="核心" width="70" />
              <el-table-column label="内存" width="100">
                <template #default="{ row }">{{ row.ramTotalMb ? (row.ramTotalMb / 1024).toFixed(1) + ' GB' : '-' }}</template>
              </el-table-column>
              <el-table-column prop="collectedAt" label="采集时间" width="170" />
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 软件清单 -->
        <el-tab-pane label="软件清单" name="software">
          <div class="flex items-center justify-between mb-3">
            <el-input v-model="softwareKeyword" placeholder="搜索软件名称" clearable style="width: 250px" @keyup.enter="searchSoftware" @clear="loadSoftware">
              <template #append>
                <el-button @click="searchSoftware"><el-icon><Search /></el-icon></el-button>
              </template>
            </el-input>
            <span class="text-xs text-gray-400">共 {{ softwareTotal }} 条</span>
          </div>
          <el-table :data="software" stripe v-loading="tabLoading">
            <el-table-column prop="name" label="软件名称" min-width="250" show-overflow-tooltip />
            <el-table-column prop="version" label="版本" width="140">
              <template #default="{ row }"><span class="font-mono text-xs">{{ row.version }}</span></template>
            </el-table-column>
            <el-table-column prop="vendor" label="厂商" width="160" show-overflow-tooltip />
            <el-table-column prop="format" label="格式" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.format" size="small" type="info">{{ row.format }}</el-tag>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="collectedAt" label="采集时间" width="170" />
          </el-table>
          <div class="flex justify-end p-4">
            <el-pagination v-model:current-page="softwarePage" :page-size="20" :total="softwareTotal" layout="total, prev, pager, next" @current-change="loadSoftware" />
          </div>
        </el-tab-pane>

        <!-- 开放端口 -->
        <el-tab-pane label="开放端口" name="ports">
          <div class="flex items-center justify-between mb-3">
            <el-input v-model.number="portSearch" placeholder="按端口号搜索" clearable type="number" style="width: 200px" @keyup.enter="searchPortByNum" @clear="loadPorts">
              <template #append>
                <el-button @click="searchPortByNum"><el-icon><Search /></el-icon></el-button>
              </template>
            </el-input>
            <span class="text-xs text-gray-400">共 {{ portTotal }} 条</span>
          </div>
          <el-table :data="ports" stripe v-loading="tabLoading">
            <el-table-column prop="protocol" label="协议" width="80">
              <template #default="{ row }">
                <el-tag :type="row.protocol === 'tcp' ? 'primary' : 'warning'" size="small">{{ row.protocol?.toUpperCase() }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="localPort" label="端口" width="100">
              <template #default="{ row }"><span class="font-mono font-bold">{{ row.localPort }}</span></template>
            </el-table-column>
            <el-table-column prop="state" label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="row.state === 'LISTEN' ? 'success' : 'info'" size="small" effect="plain">{{ row.state }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="processName" label="进程名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="processPid" label="PID" width="90">
              <template #default="{ row }">
                <span class="font-mono text-xs">{{ row.processPid ?? '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="collectedAt" label="采集时间" width="170" />
          </el-table>
          <div class="flex justify-end p-4">
            <el-pagination v-model:current-page="portPage" :page-size="20" :total="portTotal" layout="total, prev, pager, next" @current-change="loadPorts" />
          </div>
        </el-tab-pane>

        <!-- 网络接口 -->
        <el-tab-pane label="网络接口" name="networks">
          <el-table :data="networks" stripe v-loading="tabLoading">
            <el-table-column prop="interfaceName" label="接口名" width="140">
              <template #default="{ row }"><span class="font-mono font-medium">{{ row.interfaceName }}</span></template>
            </el-table-column>
            <el-table-column prop="ipv4" label="IPv4" width="150">
              <template #default="{ row }"><span class="font-mono text-sm">{{ row.ipv4 || '-' }}</span></template>
            </el-table-column>
            <el-table-column prop="ipv6" label="IPv6" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tooltip v-if="row.ipv6" :content="row.ipv6" placement="top">
                  <span class="font-mono text-xs text-gray-500">{{ row.ipv6.length > 24 ? row.ipv6.substring(0, 24) + '...' : row.ipv6 }}</span>
                </el-tooltip>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="macAddress" label="MAC" width="160">
              <template #default="{ row }"><span class="font-mono text-xs">{{ row.macAddress || '-' }}</span></template>
            </el-table-column>
            <el-table-column prop="gateway" label="网关" width="140">
              <template #default="{ row }"><span class="font-mono text-sm">{{ row.gateway || '-' }}</span></template>
            </el-table-column>
            <el-table-column prop="dns" label="DNS" width="150" show-overflow-tooltip>
              <template #default="{ row }"><span class="text-xs">{{ row.dns || '-' }}</span></template>
            </el-table-column>
            <el-table-column prop="collectedAt" label="采集时间" width="170" />
          </el-table>
          <div class="flex justify-end p-4">
            <el-pagination v-model:current-page="networkPage" :page-size="20" :total="networkTotal" layout="total, prev, pager, next" @current-change="loadNetworks" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 无选中提示 -->
    <div v-if="!selectedAgentId" class="bg-white rounded-lg shadow-sm p-12 border border-gray-100 text-center text-gray-400">
      请从上方选择一个 Agent 查看其资产信息
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { inventoryApi } from '../api/inventory'
import api from '../api/index'

const agents = ref<any[]>([])
const selectedAgentId = ref<number | null>(null)
const loading = ref(false)
const tabLoading = ref(false)
const activeTab = ref('system')

// 全局统计
const globalStats = ref<any>({})

// Agent 概要
const summary = ref<any>(null)

// 系统信息
const systemInfo = ref<any>(null)
const systemHistory = ref<any[]>([])

// 软件
const software = ref<any[]>([])
const softwarePage = ref(1)
const softwareTotal = ref(0)
const softwareKeyword = ref('')

// 端口
const ports = ref<any[]>([])
const portPage = ref(1)
const portTotal = ref(0)
const portSearch = ref<number | null>(null)

// 网卡
const networks = ref<any[]>([])
const networkPage = ref(1)
const networkTotal = ref(0)

function formatUptime(seconds: number | null): string {
  if (!seconds) return '-'
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (d > 0) return `${d}天 ${h}小时`
  if (h > 0) return `${h}小时 ${m}分`
  return `${m}分钟`
}

async function loadAgents() {
  try {
    const data: any = await api.get('/api/agents', { params: { page: 0, size: 100 } })
    agents.value = data.content ?? []
  } catch { /* ignore */ }
}

async function loadGlobalStats() {
  try {
    globalStats.value = await inventoryApi.stats() as any
  } catch { /* ignore */ }
}

function onAgentChange() {
  if (selectedAgentId.value) loadAll()
}

async function loadAll() {
  if (!selectedAgentId.value) return
  loading.value = true
  try {
    await Promise.all([
      loadSummary(),
      loadTabData()
    ])
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try {
    summary.value = await inventoryApi.summary(selectedAgentId.value!) as any
  } catch { summary.value = null }
}

function onTabChange() {
  loadTabData()
}

async function loadTabData() {
  if (!selectedAgentId.value) return
  tabLoading.value = true
  try {
    switch (activeTab.value) {
      case 'system':
        await Promise.all([loadSystem(), loadSystemHistory()])
        break
      case 'software':
        await loadSoftware()
        break
      case 'ports':
        await loadPorts()
        break
      case 'networks':
        await loadNetworks()
        break
    }
  } finally {
    tabLoading.value = false
  }
}

async function loadSystem() {
  try {
    systemInfo.value = await inventoryApi.system(selectedAgentId.value!) as any
  } catch { systemInfo.value = null }
}

async function loadSystemHistory() {
  try {
    const data: any = await inventoryApi.systemHistory(selectedAgentId.value!, 0, 5)
    systemHistory.value = data.content ?? []
  } catch { systemHistory.value = [] }
}

async function loadSoftware() {
  try {
    if (softwareKeyword.value) {
      const data: any = await inventoryApi.searchSoftware(softwareKeyword.value, softwarePage.value - 1, 20)
      software.value = data.content ?? []
      softwareTotal.value = data.totalElements ?? 0
    } else {
      const data: any = await inventoryApi.software(selectedAgentId.value!, softwarePage.value - 1, 20)
      software.value = data.content ?? []
      softwareTotal.value = data.totalElements ?? 0
    }
  } catch { software.value = [] }
}

function searchSoftware() {
  softwarePage.value = 1
  loadSoftware()
}

async function loadPorts() {
  try {
    const data: any = await inventoryApi.ports(selectedAgentId.value!, portPage.value - 1, 20)
    ports.value = data.content ?? []
    portTotal.value = data.totalElements ?? 0
  } catch { ports.value = [] }
}

async function searchPortByNum() {
  if (!portSearch.value) {
    await loadPorts()
    return
  }
  tabLoading.value = true
  try {
    const data: any = await inventoryApi.searchPort(portSearch.value)
    ports.value = Array.isArray(data) ? data : []
    portTotal.value = ports.value.length
  } catch { ports.value = [] } finally {
    tabLoading.value = false
  }
}

async function loadNetworks() {
  try {
    const data: any = await inventoryApi.networks(selectedAgentId.value!, networkPage.value - 1, 20)
    networks.value = data.content ?? []
    networkTotal.value = data.totalElements ?? 0
  } catch { networks.value = [] }
}

onMounted(() => {
  loadAgents()
  loadGlobalStats()
})
</script>
