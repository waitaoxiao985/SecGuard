<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900">
    <!-- 背景网格 -->
    <div class="absolute inset-0 opacity-10"
         style="background-image: radial-gradient(circle at 1px 1px, rgba(129,140,248,0.3) 1px, transparent 0); background-size: 40px 40px;">
    </div>

    <div class="relative z-10 w-full max-w-md">
      <!-- Logo -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-indigo-500/20 rounded-2xl mb-4">
          <el-icon :size="32" color="#818cf8"><View /></el-icon>
        </div>
        <h1 class="text-3xl font-bold text-white tracking-wide">SecGuard</h1>
        <p class="text-slate-400 mt-2">鹰眼守卫 · 轻量级主机安全监控平台</p>
      </div>

      <!-- 登录表单 -->
      <el-card class="rounded-2xl shadow-2xl border-0" shadow="never">
        <div class="p-6">
          <h2 class="text-xl font-semibold text-gray-800 mb-6">管理员登录</h2>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            @submit.prevent="handleLogin"
            size="large"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="用户名"
                prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="w-full"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>

          <p class="text-center text-xs text-gray-400 mt-4">
            默认账号: admin / SecGuard@2026
          </p>
        </div>
      </el-card>

      <p class="text-center text-slate-500 text-xs mt-6">
        SecGuard v1.0.0 · W8 Dashboard
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage, type FormInstance } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (e: any) {
    // 错误已由 API 拦截器处理
  } finally {
    loading.value = false
  }
}
</script>
