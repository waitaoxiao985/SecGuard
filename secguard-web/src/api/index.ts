import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:8900',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截：自动附加 JWT Token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('sg_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一错误处理
api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('sg_token')
      localStorage.removeItem('sg_user')
      router.push('/login')
      ElMessage.warning('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default api
