import { defineStore } from 'pinia'
import { authApi, userApi } from '../api'

const TOKEN_KEY = 'yuetin_token'
const USER_KEY = 'yuetin_user'

function readCachedUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY)) || null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readCachedUser()
  }),
  getters: {
    isLoggedIn: (s) => Boolean(s.token && s.user),
    isAdmin: (s) => s.user?.role === 'admin'
  },
  actions: {
    async login(username, password) {
      const { data } = await authApi.login({ username, password })
      this._apply(data)
      return data.user
    },
    async register(form) {
      const { data } = await authApi.register(form)
      this._apply(data)
      return data.user
    },
    /** 用 token 换取最新用户信息（启动时校验登录态是否有效） */
    async fetchProfile() {
      try {
        const { data } = await authApi.me()
        this.user = data
        localStorage.setItem(USER_KEY, JSON.stringify(data))
        return data
      } catch {
        // token 失效则清理本地登录态
        this.logout()
        return null
      }
    },
    async updateProfile(id, form) {
      const { data } = await userApi.update(id, form)
      this.user = data
      localStorage.setItem(USER_KEY, JSON.stringify(data))
      return data
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
    _apply({ token, user }) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    }
  }
})
