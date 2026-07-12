<template>
  <el-container class="min-h-screen">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="bg-slate-900 transition-all duration-300">
      <div class="flex items-center justify-center h-16 border-b border-slate-700">
        <div class="flex items-center gap-2" v-show="!isCollapse">
          <el-icon :size="24" color="#818cf8"><View /></el-icon>
          <span class="text-white font-bold text-lg tracking-wide">SecGuard</span>
        </div>
        <el-icon :size="20" color="#818cf8" v-show="isCollapse"><View /></el-icon>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        background-color="#0f172a"
        text-color="#94a3b8"
        active-text-color="#818cf8"
        :collapse="isCollapse"
        class="border-none"
      >
        <el-menu-item
          v-for="route in menuRoutes"
          :key="route.path"
          :index="route.path"
        >
          <el-icon><component :is="route.meta?.icon" /></el-icon>
          <template #title>{{ route.meta?.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="bg-white shadow-sm flex items-center justify-between px-6 h-16">
        <div class="flex items-center gap-4">
          <el-icon
            class="cursor-pointer text-gray-500 hover:text-indigo-500"
            :size="20"
            @click="isCollapse = !isCollapse"
          >
            <component :is="isCollapse ? 'Expand' : 'Fold'" />
          </el-icon>
          <span class="text-gray-700 font-medium">{{ currentRoute?.meta?.title || '控制台' }}</span>
        </div>

        <div class="flex items-center gap-4">
          <el-badge :value="alertCount" :hidden="alertCount === 0" type="danger">
            <el-icon :size="20" class="text-gray-500"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="flex items-center gap-2 cursor-pointer text-gray-700">
              <el-avatar :size="32" class="bg-indigo-500">
                {{ authStore.user?.username?.[0]?.toUpperCase() || 'A' }}
              </el-avatar>
              <span class="text-sm">{{ authStore.user?.username || '管理员' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 页面内容 -->
      <el-main class="bg-gray-50 p-6">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { alertApi } from '../api/alert'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapse = ref(false)
const alertCount = ref(0)
let alertTimer: ReturnType<typeof setInterval> | null = null

const menuRoutes = router.getRoutes()
  .find(r => r.path === '/')
  ?.children?.filter(c => c.meta?.title) || []

const currentRoute = computed(() => route)

async function refreshAlertCount() {
  try {
    const data: any = await alertApi.stats()
    alertCount.value = data.open ?? 0
  } catch { /* ignore */ }
}

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  refreshAlertCount()
  alertTimer = setInterval(refreshAlertCount, 30000) // 每 30s 刷新
})

onUnmounted(() => {
  if (alertTimer) clearInterval(alertTimer)
})
</script>
