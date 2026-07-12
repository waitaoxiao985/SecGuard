<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-bold text-gray-800">Agent 管理</h1>
      <el-input v-model="keyword" placeholder="搜索名称/IP/主机名" clearable style="width:260px" @clear="loadData" @keyup.enter="loadData">
        <template #append>
          <el-button @click="loadData"><el-icon><Search /></el-icon></el-button>
        </template>
      </el-input>
    </div>

    <div class="bg-white rounded-lg shadow-sm border border-gray-100">
      <el-table :data="agents" stripe v-loading="loading">
        <el-table-column prop="id" label="#" width="60" />
        <el-table-column prop="name" label="名称" width="150" show-overflow-tooltip />
        <el-table-column prop="hostname" label="主机名" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.hostname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP 地址" width="140" />
        <el-table-column prop="os" label="系统" width="100">
          <template #default="{ row }">
            <span class="capitalize">{{ row.os }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="CPU / 内存" width="150">
          <template #default="{ row }">
            <span v-if="row.cpuUsage != null" class="text-sm">
              {{ row.cpuUsage?.toFixed(1) }}% / {{ row.memUsage?.toFixed(1) }}%
            </span>
            <span v-else class="text-gray-400 text-xs">N/A</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastKeepalive" label="最后心跳" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'DISABLED'" size="small" type="warning" text @click="toggleAgent(row.id, 'disable')">
              禁用
            </el-button>
            <el-button v-else size="small" type="success" text @click="toggleAgent(row.id, 'enable')">
              启用
            </el-button>
            <el-button size="small" type="danger" text @click="deleteAgent(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end p-4">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api/index'

const agents = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

function statusTag(s: string): TagType {
  const map: Record<string, TagType> = { ACTIVE: 'success', DISCONNECTED: 'warning', PENDING: 'info', DISABLED: 'danger' }
  return map[s] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: currentPage.value - 1, size: pageSize }
    if (keyword.value) params.keyword = keyword.value
    const data: any = await api.get('/api/agents', { params })
    agents.value = data.content ?? []
    total.value = data.totalElements ?? 0
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

async function toggleAgent(id: number, action: 'disable' | 'enable') {
  try {
    await api.put(`/api/agents/${id}/${action}`)
    ElMessage.success(`Agent ${action === 'disable' ? '已禁用' : '已启用'}`)
    await loadData()
  } catch { /* ignore */ }
}

async function deleteAgent(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除 Agent「${row.name}」？所有关联数据将被清除。`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await api.delete(`/api/agents/${row.id}`)
    ElMessage.success('已删除')
    await loadData()
  } catch { /* cancelled or error */ }
}

onMounted(loadData)
</script>
