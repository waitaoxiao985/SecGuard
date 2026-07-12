<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-bold text-gray-800">检测规则</h1>
      <div class="flex gap-2">
        <el-input v-model="searchText" placeholder="搜索规则名称/描述" clearable style="width: 250px" @keyup.enter="filterRules" @clear="filterRules">
          <template #append>
            <el-button @click="filterRules"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
        <el-select v-model="filterCategory" placeholder="类别" clearable style="width: 140px" @change="filterRules">
          <el-option v-for="c in allCategories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filterSeverity" placeholder="等级" clearable style="width: 120px" @change="filterRules">
          <el-option label="CRITICAL" value="CRITICAL" />
          <el-option label="HIGH" value="HIGH" />
          <el-option label="MEDIUM" value="MEDIUM" />
          <el-option label="LOW" value="LOW" />
        </el-select>
        <el-button type="warning" @click="hotReload" :loading="reloading">
          热重载
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-5 gap-3">
      <div class="bg-white rounded-lg shadow-sm p-4 border border-gray-100 text-center">
        <p class="text-xs text-gray-500">规则总数</p>
        <p class="text-2xl font-bold text-gray-700">{{ stats.total ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-red-100 text-center">
        <p class="text-xs text-gray-500">严重 (≥12)</p>
        <p class="text-2xl font-bold text-red-600">{{ stats.bySeverity?.CRITICAL ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-orange-100 text-center">
        <p class="text-xs text-gray-500">高危 (8-11)</p>
        <p class="text-2xl font-bold text-orange-500">{{ stats.bySeverity?.HIGH ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-blue-100 text-center">
        <p class="text-xs text-gray-500">中危 (5-7)</p>
        <p class="text-2xl font-bold text-blue-500">{{ stats.bySeverity?.MEDIUM ?? 0 }}</p>
      </div>
      <div class="bg-white rounded-lg shadow-sm p-4 border border-green-100 text-center">
        <p class="text-xs text-gray-500">低危 (&lt;5)</p>
        <p class="text-2xl font-bold text-green-500">{{ stats.bySeverity?.LOW ?? 0 }}</p>
      </div>
    </div>

    <!-- 类别分布 -->
    <div v-if="Object.keys(stats.byCategory ?? {}).length > 0" class="bg-white rounded-lg shadow-sm p-5 border border-gray-100">
      <h3 class="text-sm font-semibold text-gray-600 mb-3">类别分布</h3>
      <div class="flex flex-wrap gap-2">
        <el-tag
          v-for="item in categoryTags"
          :key="item.name"
          :type="filterCategory === item.name ? 'primary' : 'info'"
          class="cursor-pointer"
          @click="filterCategory = filterCategory === item.name ? '' : item.name; filterRules()"
        >
          {{ item.name }}: {{ item.count }}
        </el-tag>
      </div>
    </div>

    <!-- 规则列表 -->
    <div class="bg-white rounded-lg shadow-sm border border-gray-100">
      <el-table :data="filteredRules" stripe v-loading="loading">
        <el-table-column prop="ruleId" label="#" width="80">
          <template #default="{ row }"><span class="font-mono text-xs">{{ row.ruleId }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="规则名称" min-width="240" show-overflow-tooltip />
        <el-table-column prop="level" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" size="small">{{ levelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="类别" width="130">
          <template #default="{ row }">
            <el-tag type="info" size="small" effect="plain">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
        <el-table-column label="MITRE ATT&CK" width="200">
          <template #default="{ row }">
            <div v-if="row.mitre">
              <div v-if="row.mitre.tactic?.length" class="mb-1">
                <el-tag v-for="t in row.mitre.tactic" :key="t" size="small" class="mr-1 mb-1" effect="plain" color="#eef2ff" style="color:#4f46e5;border-color:#c7d2fe">{{ t }}</el-tag>
              </div>
              <div v-if="row.mitre.technique?.length">
                <span v-for="t in row.mitre.technique" :key="t" class="font-mono text-xs text-indigo-500 mr-2">{{ t }}</span>
              </div>
            </div>
            <span v-else class="text-gray-400 text-xs">-</span>
          </template>
        </el-table-column>
        <el-table-column label="PCI DSS" width="120">
          <template #default="{ row }">
            <span v-if="row.pciDss?.length" class="text-xs text-gray-500">{{ row.pciDss.join(', ') }}</span>
            <span v-else class="text-gray-400 text-xs">-</span>
          </template>
        </el-table-column>
        <el-table-column label="匹配条件" width="100">
          <template #default="{ row }">
            <el-tooltip v-if="row.conditions?.fieldMatch?.length" placement="top">
              <template #content>
                <div v-for="(cond, i) in row.conditions.fieldMatch" :key="i" class="text-xs">
                  {{ cond.field }} {{ cond.operator || '~' }} {{ cond.value }}
                </div>
              </template>
              <el-badge :value="row.conditions.fieldMatch.length" type="primary" />
            </el-tooltip>
            <span v-else class="text-gray-400 text-xs">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex items-center justify-between p-4 text-xs text-gray-400">
        <span>显示 {{ filteredRules.length }} / {{ allRules.length }} 条规则</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ruleApi } from '../api/rule'

const allRules = ref<any[]>([])
const stats = ref<any>({})
const loading = ref(false)
const reloading = ref(false)
const searchText = ref('')
const filterCategory = ref('')
const filterSeverity = ref('')

const allCategories = computed(() => {
  const cats = new Set(allRules.value.map((r: any) => r.category).filter(Boolean))
  return Array.from(cats).sort() as string[]
})

const categoryTags = computed(() => {
  const bc = stats.value.byCategory ?? {}
  return Object.entries(bc).map(([name, count]) => ({ name, count: count as number }))
})

const filteredRules = computed(() => {
  let rules = allRules.value
  const kw = searchText.value.toLowerCase()
  if (kw) {
    rules = rules.filter((r: any) =>
      (r.name || '').toLowerCase().includes(kw) ||
      (r.description || '').toLowerCase().includes(kw)
    )
  }
  if (filterCategory.value) {
    rules = rules.filter((r: any) => r.category === filterCategory.value)
  }
  if (filterSeverity.value) {
    rules = rules.filter((r: any) => levelLabel(r.level) === filterSeverity.value)
  }
  return rules
})

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function levelLabel(level: number): string {
  if (level >= 12) return 'CRITICAL'
  if (level >= 8) return 'HIGH'
  if (level >= 5) return 'MEDIUM'
  return 'LOW'
}

function levelTagType(level: number): TagType {
  if (level >= 12) return 'danger'
  if (level >= 8) return 'warning'
  if (level >= 5) return 'primary'
  return 'success'
}

function filterRules() {
  // 纯前端过滤，无需请求
}

async function loadRules() {
  loading.value = true
  try {
    const data: any = await ruleApi.list()
    allRules.value = Array.isArray(data) ? data : (data ? Object.values(data) : [])
  } catch { allRules.value = [] } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await ruleApi.stats() as any
  } catch { /* ignore */ }
}

async function hotReload() {
  reloading.value = true
  try {
    const data: any = await ruleApi.reload()
    ElMessage.success(data.message || `规则重载完成，共 ${data.loaded} 条`)
    await Promise.all([loadRules(), loadStats()])
  } catch { /* handled */ } finally {
    reloading.value = false
  }
}

onMounted(() => {
  loadRules()
  loadStats()
})
</script>
