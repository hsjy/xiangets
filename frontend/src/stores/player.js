import { defineStore } from 'pinia'
import { songApi } from '../api'

const VOL_KEY = 'yuetin_volume'
const MODE_KEY = 'yuetin_playmode'

/**
 * 全局播放器状态：单一 <audio> 实例 + 队列管理。
 * 播放模式：order 列表循环 / random 随机 / single 单曲循环
 */
export const usePlayerStore = defineStore('player', {
  state: () => ({
    audio: null,            // 单例 HTMLAudioElement，attach() 时注入
    queue: [],              // 播放队列 [{id,name,singerName,url,duration,...}]
    currentId: null,        // 当前播放歌曲 id
    playing: false,
    currentTime: 0,
    duration: 0,
    buffered: 0,
    volume: Number(localStorage.getItem(VOL_KEY) ?? 0.8),
    muted: false,
    mode: localStorage.getItem(MODE_KEY) || 'order', // order | random | single
    queueVisible: false,
    _randHistory: []        // 随机模式回溯用
  }),
  getters: {
    current: (s) => s.queue.find((x) => x.id === s.currentId) || null,
    currentIndex: (s) => s.queue.findIndex((x) => x.id === s.currentId),
    hasTrack: (s) => s.currentId != null
  },
  actions: {
    attach(el) {
      this.audio = el
      el.volume = this.volume
      el.muted = this.muted
      // 恢复媒体会话（浏览器媒体键控制）
      if ('mediaSession' in navigator) {
        try {
          navigator.mediaSession.setActionHandler('previoustrack', () => this.prev())
          navigator.mediaSession.setActionHandler('nexttrack', () => this.next())
          navigator.mediaSession.setActionHandler('play', () => el.play())
          navigator.mediaSession.setActionHandler('pause', () => el.pause())
        } catch { /* 部分浏览器不支持 */ }
      }
    },
    /** 播放一首歌：可带队列上下文（歌单/列表） */
    async playSong(song, context = null) {
      if (Array.isArray(context) && context.length) {
        // 上下文与当前队列不同则重建队列
        const sig = context.map((s) => s.id).join(',')
        if (sig !== this.queue.map((s) => s.id).join(',')) {
          this.queue = context.slice()
          this._randHistory = []
        }
      } else if (!this.queue.some((s) => s.id === song.id)) {
        this.queue.push(song)
      }
      const target = this.queue.find((s) => s.id === song.id)
      if (!target) return
      if (this.currentId === target.id && this.audio) {
        // 同一首：切换播放/暂停
        if (this.audio.paused) await this.resume()
        else this.pause()
        return
      }
      this.currentId = target.id
      this.currentTime = 0
      this.duration = target.duration || 0
      await this._loadAndPlay()
      songApi.play(target.id).catch(() => {}) // 播放计数，失败静默
      this._updateMediaSession(target)
    },
    async _loadAndPlay() {
      const el = this.audio
      if (!el || !this.current) return
      el.src = this.current.url
      try {
        await el.play()
      } catch (e) {
        // Autoplay 被浏览器策略拦截时静默，等待用户交互
        this.playing = false
      }
    },
    pause() {
      this.audio?.pause()
    },
    async resume() {
      try {
        await this.audio?.play()
      } catch { /* ignore */ }
    },
    async toggle() {
      if (!this.hasTrack) return
      if (this.audio.paused) await this.resume()
      else this.pause()
    },
    /** 上一曲/下一曲：按当前播放模式决定 */
    async next(auto = false) {
      if (!this.queue.length) return
      if (this.mode === 'single' && auto) {
        this.currentTime = 0
        await this._loadAndPlay()
        return
      }
      if (this.mode === 'random') {
        this._pickRandom()
      } else {
        const i = this.currentIndex
        const n = (i + 1) % this.queue.length // 列表循环
        this.currentId = this.queue[n].id
      }
      this.currentTime = 0
      await this._loadAndPlay()
      if (this.current) songApi.play(this.current.id).catch(() => {})
      this._updateMediaSession(this.current)
    },
    async prev() {
      if (!this.queue.length) return
      if (this.mode === 'random' && this._randHistory.length > 1) {
        this._randHistory.pop()
        this.currentId = this._randHistory[this._randHistory.length - 1]
      } else {
        const i = this.currentIndex
        const p = (i - 1 + this.queue.length) % this.queue.length
        this.currentId = this.queue[p].id
      }
      this.currentTime = 0
      await this._loadAndPlay()
      if (this.current) songApi.play(this.current.id).catch(() => {})
      this._updateMediaSession(this.current)
    },
    _pickRandom() {
      if (this.queue.length === 1) {
        this.currentId = this.queue[0].id
        return
      }
      let id
      do {
        id = this.queue[Math.floor(Math.random() * this.queue.length)].id
      } while (id === this.currentId)
      this._randHistory.push(id)
      if (this._randHistory.length > 30) this._randHistory.shift()
      this.currentId = id
    },
    seek(sec) {
      if (!this.audio) return
      const d = this.duration || this.audio.duration || 0
      const t = Math.max(0, Math.min(sec, d || sec))
      this.audio.currentTime = t
      this.currentTime = t
    },
    setVolume(v) {
      this.volume = Math.max(0, Math.min(1, Number(v)))
      if (this.audio) {
        this.audio.volume = this.volume
        this.audio.muted = false
        this.muted = false
      }
      localStorage.setItem(VOL_KEY, String(this.volume))
    },
    toggleMute() {
      this.muted = !this.muted
      if (this.audio) this.audio.muted = this.muted
    },
    cycleMode() {
      const order = ['order', 'random', 'single']
      this.mode = order[(order.indexOf(this.mode) + 1) % order.length]
      localStorage.setItem(MODE_KEY, this.mode)
    },
    addToQueue(song) {
      if (!this.queue.some((s) => s.id === song.id)) {
        this.queue.push(song)
        return true
      }
      return false
    },
    removeFromQueue(id) {
      const i = this.queue.findIndex((s) => s.id === id)
      if (i === -1) return
      this.queue.splice(i, 1)
      if (id === this.currentId) {
        this.currentId = this.queue[Math.min(i, this.queue.length - 1)]?.id ?? null
        if (this.currentId) this._loadAndPlay()
        else this._reset()
      }
    },
    clearQueue() {
      this.audio?.pause()
      this._reset()
    },
    _reset() {
      this.queue = []
      this.currentId = null
      this.playing = false
      this.currentTime = 0
      this.duration = 0
      if (this.audio) this.audio.removeAttribute('src')
    },
    toggleQueue() {
      this.queueVisible = !this.queueVisible
    },
    _updateMediaSession(song) {
      if (!('mediaSession' in navigator) || !song) return
      try {
        navigator.mediaSession.metadata = new MediaMetadata({
          title: song.name,
          artist: song.singerName || '',
          album: '弦歌 · 诗乐雅集',
          artwork: [{ src: 'favicon.svg', sizes: '64x64', type: 'image/svg+xml' }]
        })
      } catch { /* ignore */ }
    }
  }
})
