import axios from 'axios'
import { Message } from 'element-ui'
import { getToken } from '@/utils/auth'
import store from '@/store'
import router from '@/router'

// 创建 axios 实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // 从 .env 中读取，如 http://119.45.10.196:8080
  timeout: 5000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 如果有 token，就在请求头里携带
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}` // 后端接收格式
    }
    return config
  },
  error => {
    // 请求错误处理
    console.error('Request Error: ', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // response.data 就是后端返回的 JSON 对象
    const res = response.data

    // —— 业务层面判断失败 ——
    // 如果后端返回了 success 字段，且为 false，则视为业务错误
    if (typeof res.success !== 'undefined' && !res.success) {
      // 用 ElementUI 的全局 Message 提示错误信息
      Message({
        message: res.message || 'Error',
        type: 'error',
        duration: 5 * 1000
      })
      // 拒绝 Promise，进入调用处的 catch
      return Promise.reject(new Error(res.message || 'Error'))
    }

    // 业务正常，直接返回 data
    return res
  },
  error => {
    // HTTP 层面错误（网络错误、超时、5xx 等）
    console.error('Response Error: ', error)

    if (error.response) {
      const { status, data } = error.response

      // 401: 未认证（登录过期、token无效等）
      if (status === 401) {
        Message({
          message: '登录已过期，请重新登录',
          type: 'warning',
          duration: 3000
        })

        // 清空用户信息，重置 token
        store.dispatch('user/resetToken').then(() => {
          // 跳转到登录页，带上当前路径方便登录后重定向
          router.push(`/login?redirect=${router.currentRoute.fullPath}`)
        })
      } else if (status === 403) {
        // 优先使用后端返回的错误信息
        const message = (data && data.message) || '权限不足，无法访问该资源'
        Message({
          message: message,
          type: 'error',
          duration: 5000
        })
      } else if (status === 400) {
        const message = (data && data.message) || '请求参数错误'
        Message({
          message: message,
          type: 'error',
          duration: 5000
        })
      } else if (status === 404) {
        Message({
          message: '请求的资源不存在',
          type: 'error',
          duration: 3000
        })
      } else if (status >= 500) {
        Message({
          message: '服务器内部错误，请稍后重试',
          type: 'error',
          duration: 5000
        })
      } else {
        const message = (data && data.message) || error.message || `HTTP ${status} 错误`
        Message({
          message: message,
          type: 'error',
          duration: 5000
        })
      }
    } else {
      // 网络错误、超时等
      Message({
        message: error.message || '网络连接失败，请检查网络设置',
        type: 'error',
        duration: 5000
      })
    }

    return Promise.reject(error)
  }
)

export default service
