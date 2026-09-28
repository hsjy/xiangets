/**
 * axios 客户端：
 * - 默认模式（静态部署 / GitHub Pages）：使用自定义 adapter 把请求交给浏览器内 Mock 服务处理；
 * - real 模式（vite --mode real）：直连真实 SpringBoot 后端（开发态走 vite 代理 /api -> localhost:8080）。
 * 两种模式共用同一套 RESTful 约定与统一响应结构 { code, message, data }。
 */
import axios from 'axios'
import { handle as mockHandle } from '../mock/server'

const useMock = import.meta.env.MODE !== 'real'

const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 请求拦截：附带 JWT
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('yuetin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：剥离外层结构；401 统一处理
request.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const message = err.response?.data?.message || err.message || '网络异常，请稍后重试'
    if (status === 401 && localStorage.getItem('yuetin_token') && !location.hash.includes('/login')) {
      // 仅当本地存在 token（登录已过期）时才跳转登录页；
      // 匿名请求的 401（如查询喜欢列表）由调用方自行降级处理
      localStorage.removeItem('yuetin_token')
      localStorage.removeItem('yuetin_user')
      setTimeout(() => location.assign('#/login'), 0)
    }
    return Promise.reject(Object.assign(new Error(message), { status }))
  }
)

// Mock adapter：把 axios 请求翻译成 Mock 路由调用
if (useMock) {
  request.defaults.adapter = async (config) => {
    const url = new URL(config.url, 'http://mock.local')
    const path = url.pathname.replace(/^\/+api/, '')
    const query = Object.fromEntries(url.searchParams.entries())

    let body = config.data
    if (typeof body === 'string') {
      try { body = JSON.parse(body) } catch { /* 保留原文 */ }
    }

    const headers = { authorization: config.headers?.Authorization || config.headers?.authorization }
    const res = await mockHandle({ method: (config.method || 'get').toUpperCase(), path, query, body, headers })

    if (res.status >= 400) {
      throw Object.assign(new Error(res.data.message), {
        config, response: { status: res.status, data: res.data, headers: {} }
      })
    }
    return { data: res.data, status: res.status, statusText: 'OK', headers: {}, config }
  }
}

export default request
