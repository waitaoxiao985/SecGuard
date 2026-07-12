<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-bold text-gray-800">文件完整性监控</h1>
      <div class="flex gap-2">
        <el-select v-model="filters.eventType" placeholder="事件类型" clearable style="width: 130px" @change="loadEvents">
          <el-option label="ADDED" value="ADDED" />
          <el-option label="MODIFIED" value="MODIFIED" />
          <el-option label="DELETED" value="DELETED" />
        </el-select>
        <el-input v-model="filters.pathPattern" placeholder="路径关键词" clearable style="width: 180px" @keyup.enter="loadEvents" />
        <el-button type="primary" @click="loadEvents" :loading="loading">
          <el-icon class="mr-1"><Search /></el-icon>查询
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-5 gap-3">
      <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-100 text-center">
        <p class="text-xs text-gray-500">总事件数</p>
        <p class="text-2xl font-bold text-gray-700">{{ stats.totalEvents ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-green-100 text-center">
        <p class="text-xs text-gray-500">新增文件</p>
        <p class="text-2xl font-bold text-green-500">{{ stats.byType?.ADDED ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-orange-100 text-center">
        <p class="text-xs text-gray-500">文件修改</p>
        <p class="text-2xl font-bold text-orange-500">{{ stats.byType?.MODIFIED ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-red-100 text-center">
        <p class="text-xs text-gray-500">文件删除</p>
        <p class="text-2xl font-bold text-red-500">{{ stats.byType?.DELETED ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-indigo-100 text-center">
        <p class="text-xs text-gray-500">7日趋势</p>
        <p class="text-2xl font-bold text-indigo-500">{{ stats.recentCount ?? 0 }}</p>
      </div>
    </div>

    <!-- 趋势图 -->
    <div v-if="trendData.length > 0" class="bg-white rounded-lg shadow-sm p-5 border border-gray-100">
      <h3 class="text-sm font-semibold text-gray-600 mb-3">近 7 天变更趋势</h3>
      <v-chart :option="trendOption" style="height: 200px" autoresize />
    </div>

    <!-- Tab 切换：事件列表 / 基线管理 -->
    <div class="bg-white rounded-lg shadow-sm border border-gray-100">
      <el-tabs v-model="activeTab" class="px-4">
        <el-tab-pane label="变更事件" name="events">
          <el-table :data="events" stripe v-loading="loading">
            <el-table-column prop="id" label="#" width="60" />
            <el-table-column prop="filePath" label="文件路径" min-width="300" show-overflow-tooltip />
            <el-table-column prop="eventType" label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="eventTypeTag(row.eventType)" size="small">{{ row.eventType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sha256Hash" label="SHA-256" width="180">
              <template #default="{ row }">
                <el-tooltip v-if="row.sha256Hash" :content="row.sha256Hash" placement="top">
                  <span class="font-mono text-xs text-gray-500">{{ row.sha256Hash?.substring(0, 12) }}...</span>
                </el-tooltip>
                <span v-else class="text-gray-400">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="大小" width="90">
              <template #default="{ row }">
                {{ row.fileSize != null ? formatSize(row.fileSize) : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="属主" width="100" />
            <el-table-column prop="createdAt" label="时间" width="170" />
          </el-table>

          <div class="flex justify-end p-4">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="loadEvents"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="基线管理" name="baseline">
          <div class="flex items-center justify-between mb-3">
            <el-select v-model="baselineAgentId" placeholder="选择 Agent" style="width: 200px" @change="loadBaseline">
              <el-option v-for="a in agents" :key="a.id" :label="`${a.name} (${a.hostname || a.ip})`" :value="a.id" />
            </el-select>
            <el-button type="warning" :disabled="!baselineAgentId" @click="resetBaselineConfirm">
              重置基线
            </el-button>
          </div>
          <el-table :data="baseline" stripe v-loading="baselineLoading">
            <el-table-column prop="filePath" label="文件路径" min-width="350" show-overflow-tooltip />
            <el-table-column prop="sha256Hash" label="SHA-256" width="180">
              <template #default="{ row }">
                <el-tooltip :content="row.sha256Hash" placement="top">
                  <span class="font-mono text-xs text-gray-500">{{ row.sha256Hash?.substring(0, 12) }}...</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="大小" width="90">
              <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column prop="permissions" label="权限" width="100" />
            <el-table-column prop="owner" label="属主" width="100" />
            <el-table-column prop="baselineVersion" label="版本" width="70" />
          </el-table>

          <div class="flex justify-end p-4">
            <el-pagination
              v-model:current-page="baselinePage"
              :page-size="50"
              :total="baselineTotal"
              layout="total, prev, pager, next"
              @current-change="loadBaseline"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { fimApi } from '../api/fim'
import api from '../api/index'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent])

const events = ref<any[]>([])
const stats = ref<any>({})
const trendData = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const activeTab = ref('events')

const filters = reactive({
  eventType: '',
  pathPattern: ''
})

// 基线相关
const agents = ref<any[]>([])
const baselineAgentId = ref<number | null>(null)
const baseline = ref<any[]>([])
const baselineLoading = ref(false)
const baselinePage = ref(1)
const baselineTotal = ref(0)

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 10, bottom: 30 },
  xAxis: {
    type: 'category',
    data: trendData.value.map((d: any) => d.date),
    axisLabel: { fontSize: 11 }
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    type: 'bar',
    data: trendData.value.map((d: any) => d.count),
    itemStyle: { color: '#6366f1', borderRadius: [4, 4, 0, 0] }
  }]
}))

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function eventTypeTag(t: string): TagType {
  const map: Record<string, TagType> = { ADDED: 'success', MODIFIED: 'warning', DELETED: 'danger' }
  return map[t] || 'info'
}

function formatSize(bytes: number): string {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function loadEvents() {
  loading.value = true
  try {
    const params: any = { page: currentPage.value - 1, size: pageSize }
    if (filters.eventType) params.eventType = filters.eventType
    if (filters.pathPattern) params.pathPattern = filters.pathPattern
    const data: any = await fimApi.list(params)
    events.value = data.content ?? []
    total.value = data.totalElements ?? 0
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const data: any = await fimApi.stats(7)
    stats.value = data
    trendData.value = data.dailyTrend ?? []
  } catch { /* ignore */ }
}

async function loadAgents() {
  try {
    const data: any = await api.get('/api/agents', { params: { page: 0, size: 100 } })
    agents.value = data.content ?? []
  } catch { /* ignore */ }
}

async function loadBaseline() {
  if (!baselineAgentId.value) return
  baselineLoading.value = true
  try {
    const data: any = await fimApi.baseline({ agentId: baselineAgentId.value, page: baselinePage.value - 1, size: 50 })
    baseline.value = data.content ?? []
    baselineTotal.value = data.totalElements ?? 0
  } catch { /* ignore */ } finally {
    baselineLoading.value = false
  }
}

async function resetBaselineConfirm() {
  if (!baselineAgentId.value) return
  try {
    await ElMessageBox.confirm('确定重置该 Agent 的 FIM 基线？Agent 将在下次扫描时重新上报基线。', '确认重置', { type: 'warning' })
    await fimApi.resetBaseline(baselineAgentId.value)
    ElMessage.success('基线已重置')
    await loadBaseline()
  } catch { /* cancelled or error */ }
}

onMounted(() => {
  loadEvents()
  loadStats()
  loadAgents()
})
</script>
