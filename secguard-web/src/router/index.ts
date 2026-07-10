import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { title: '安全概览', icon: 'Odometer' }
      },
      {
        path: 'agents',
        name: 'Agents',
        component: () => import('../views/AgentsView.vue'),
        meta: { title: 'Agent 管理', icon: 'Monitor' }
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('../views/placeholder/ComingSoonView.vue'),
        meta: { title: '告警管理', icon: 'Bell' }
      },
      {
        path: 'vulnerabilities',
        name: 'Vulnerabilities',
        component: () => import('../views/VulnerabilityView.vue'),
        meta: { title: '漏洞管理', icon: 'WarnTriangleFilled' }
      },
      {
        path: 'fim',
        name: 'FIM',
        component: () => import('../views/placeholder/ComingSoonView.vue'),
        meta: { title: '文件监控', icon: 'Document' }
      },
      {
        path: 'inventory',
        name: 'Inventory',
        component: () => import('../views/placeholder/ComingSoonView.vue'),
        meta: { title: '资产清单', icon: 'Box' }
      },
      {
        path: 'rules',
        name: 'Rules',
        component: () => import('../views/placeholder/ComingSoonView.vue'),
        meta: { title: '规则管理', icon: 'Setting' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('sg_token')

  if (to.meta.requiresAuth === false) {
    // 登录页：已登录则跳转首页
    if (token && to.name === 'Login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    // 需要认证
    if (token) {
      next()
    } else {
      next({ name: 'Login', query: { redirect: to.fullPath } })
    }
  }
})

export default router
