import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 生产环境应对 Nginx 同域反代时用 `/api`；不要用 Windows 路径作 VITE_API_URL，否则会出现 Unsupported protocol D:
 */
function resolveApiBaseURL(): string {
  const raw = import.meta.env.VITE_API_URL as string | undefined
  if (raw != null && raw !== '' && /^[A-Za-z]:/.test(raw)) {
    console.warn(
      '[request] VITE_API_URL 像 Windows 路径，已忽略。请设为 /api 或 https://域名/api，重新 npm run build'
    )
    return import.meta.env.PROD ? '/api' : 'http://localhost:8080/api'
  }
  if (raw != null && raw !== '') {
    return raw
  }
  return import.meta.env.PROD ? '/api' : 'http://localhost:8080/api'
}

// 创建 axios 实例
const service = axios.create({
  baseURL: resolveApiBaseURL(),
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 400:
          ElMessage.error(data?.message || '请求参数错误')
          break
        case 401:
          ElMessage.error('登录失效，请重新登录')
          localStorage.removeItem('token')
          {
            const path = window.location.pathname + window.location.search
            const q = path !== '/login' ? `?redirect=${encodeURIComponent(path)}` : ''
            window.location.href = `/login${q}`
          }
          break
        case 403:
          // 曾携带 token 访问受保护接口但无认证上下文时，Spring 可能返回 403，与 401 同样处理
          if (localStorage.getItem('token')) {
            ElMessage.error('登录失效，请重新登录')
            localStorage.removeItem('token')
            const path = window.location.pathname + window.location.search
            const q = path !== '/login' ? `?redirect=${encodeURIComponent(path)}` : ''
            window.location.href = `/login${q}`
          } else {
            ElMessage.error('没有权限访问该资源')
          }
          break
        case 404:
          ElMessage.error(data?.message || '资源不存在')
          break
        case 500:
          ElMessage.error(data.message || '服务器错误')
          break
        case 503:
          ElMessage.error(data?.message || '服务暂不可用')
          break
        default:
          ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接错误')
    }
    return Promise.reject(error)
  }
)

export default service
