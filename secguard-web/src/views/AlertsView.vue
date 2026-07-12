<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-bold text-gray-800">告警管理</h1>
      <div class="flex gap-2">
        <el-select v-model="filters.severity" placeholder="严重等级" clearable style="width: 130px" @change="loadAlerts">
          <el-option label="CRITICAL" value="CRITICAL" />
          <el-option label="HIGH" value="HIGH" />
          <el-option label="MEDIUM" value="MEDIUM" />
          <el-option label="LOW" value="LOW" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 130px" @change="loadAlerts">
          <el-option label="OPEN" value="OPEN" />
          <el-option label="ACKNOWLEDGED" value="ACKNOWLEDGED" />
          <el-option label="RESOLVED" value="RESOLVED" />
          <el-option label="FALSE_POSITIVE" value="FALSE_POSITIVE" />
        </el-select>
        <el-select v-model="filters.category" placeholder="类别" clearable style="width: 140px" @change="loadAlerts">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-button type="primary" @click="loadAlerts" :loading="loading">
          <el-icon class="mr-1"><Search /></el-icon>查询
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-4 gap-3">
      <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-100 text-center">
        <p class="text-xs text-gray-500">总告警</p>
        <p class="text-2xl font-bold text-gray-700">{{ stats.total ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-red-100 text-center">
        <p class="text-xs text-gray-500">待处理</p>
        <p class="text-2xl font-bold text-red-500">{{ stats.open ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-orange-100 text-center">
        <p class="text-xs text-gray-500">24h 新增</p>
        <p class="text-2xl font-bold text-orange-500">{{ stats.last24h ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-red-100 text-center">
        <p class="text-xs text-gray-500">严重告警</p>
        <p class="text-2xl font-bold text-red-600">{{ stats.bySeverity?.CRITICAL ?? 0 }}</p>
      </div>
    </div>

    <!-- 告警列表 -->
    <div class="bg-white rounded-lg shadow-sm border border-gray-100">
      <el-table :data="alerts" stripe v-loading="loading" @row-click="showDetail">
        <el-table-column prop="id" label="#" width="60" />
        <el-table-column prop="ruleName" label="规则名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="severity" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="severityTag(row.severity)" size="small">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="类别" width="130" />
        <el-table-column prop="mitreTechnique" label="MITRE" width="120">
          <template #default="{ row }">
            <span v-if="row.mitreTechnique" class="text-xs font-mono text-indigo-600">{{ row.mitreTechnique }}</span>
            <span v-else class="text-gray-400 text-xs">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceIp" label="来源 IP" width="140">
          <template #default="{ row }">{{ row.sourceIp || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'OPEN'">
              <el-button size="small" type="warning" text @click.stop="acknowledge(row.id)">确认</el-button>
              <el-button size="small" type="success" text @click.stop="resolve(row.id)">解决</el-button>
              <el-button size="small" type="info" text @click.stop="markFP(row.id)">误报</el-button>
            </template>
            <span v-else class="text-gray-400 text-xs">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end p-4">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadAlerts"
        />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="告警详情" size="500px">
      <template v-if="selectedAlert">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ selectedAlert.id }}</el-descriptions-item>
          <el-descriptions-item label="规则">{{ selectedAlert.ruleName }} ({{ selectedAlert.ruleId }})</el-descriptions-item>
          <el-descriptions-item label="等级">
            <el-tag :type="severityTag(selectedAlert.severity)" size="small">{{ selectedAlert.severity }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类别">{{ selectedAlert.category }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(selectedAlert.status)" size="small">{{ selectedAlert.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="MITRE 战术">
            <span class="font-mono text-xs">{{ selectedAlert.mitreTactic || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="MITRE 技术">
            <span class="font-mono text-xs">{{ selectedAlert.mitreTechnique || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="来源 IP">{{ selectedAlert.sourceIp || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Agent ID">{{ selectedAlert.agentId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ selectedAlert.description }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ selectedAlert.createdAt }}</el-descriptions-item>
        </el-descriptions>
        <div class="mt-4" v-if="selectedAlert.rawEvent">
          <h4 class="text-sm font-semibold text-gray-600 mb-2">原始事件数据</h4>
          <pre class="bg-gray-50 p-3 rounded text-xs overflow-auto max-h-60 border">{{ formatJson(selectedAlert.rawEvent) }}</pre>
        </div>
        <div class="mt-4 flex gap-2" v-if="selectedAlert.status === 'OPEN'">
          <el-button type="warning" @click="acknowledge(selectedAlert.id); drawerVisible = false">确认</el-button>
          <el-button type="success" @click="resolve(selectedAlert.id); drawerVisible = false">解决</el-button>
          <el-button type="info" @click="markFP(selectedAlert.id); drawerVisible = false">标记误报</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { alertApi } from '../api/alert'

const categories = ['AUTHENTICATION', 'FIM', 'NETWORK', 'SYSTEM', 'WEB', 'FIREWALL', 'INVENTORY', 'VULNERABILITY']

const alerts = ref<any[]>([])
const stats = ref<any>({})
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)
const drawerVisible = ref(false)
const selectedAlert = ref<any>(null)

const filters = reactive({
  severity: '',
  status: '',
  category: ''
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

function formatJson(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

async function loadAlerts() {
  loading.value = true
  try {
    const params: any = { page: currentPage.value - 1, size: pageSize }
    if (filters.severity) params.severity = filters.severity
    if (filters.status) params.status = filters.status
    if (filters.category) params.category = filters.category
    const data: any = await alertApi.list(params)
    alerts.value = data.content ?? []
    total.value = data.totalElements ?? 0
  } catch { /* handled by interceptor */ } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await alertApi.stats() as any
  } catch { /* ignore */ }
}

function showDetail(row: any) {
  selectedAlert.value = row
  drawerVisible.value = true
}

async function acknowledge(id: number) {
  try {
    await alertApi.acknowledge(id)
    ElMessage.success('告警已确认')
    await loadAlerts()
    await loadStats()
  } catch { /* ignore */ }
}

async function resolve(id: number) {
  try {
    await alertApi.resolve(id)
    ElMessage.success('告警已解决')
    await loadAlerts()
    await loadStats()
  } catch { /* ignore */ }
}

async function markFP(id: number) {
  try {
    await alertApi.falsePositive(id)
    ElMessage.success('已标记为误报')
    await loadAlerts()
    await loadStats()
  } catch { /* ignore */ }
}

onMounted(() => {
  loadAlerts()
  loadStats()
})
</script>
