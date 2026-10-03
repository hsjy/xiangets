import { defineStore } from 'pinia'
import { songApi } from '../api'

const VOL_KEY = 'yuetin_volume'

/**
 * 全局播放器状态：单一 <audio> 实例 + 队列管理。
 * 播放策略：曲目自然播完即停（不循环、不自动切下一首），
 * 上一首/下一首仅由用户手动触发。
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
    queueVisible: false
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
      const el = this.audio
      if (!el) return
      // 播完后再次点播放：从头开始，而不是从结尾的 0 秒「假播放」
      if (el.ended || (this.duration && el.currentTime >= this.duration - 0.05)) {
        el.currentTime = 0
        this.currentTime = 0
      }
      try {
        await el.play()
      } catch { /* ignore */ }
    },
    async toggle() {
      if (!this.hasTrack) return
      if (this.audio.paused) await this.resume()
      else this.pause()
    },
    /** 曲目自然播完：一遍即停，不循环也不自动切下一首 */
    handleEnded() {
      this.playing = false
      if (this.audio) this.audio.currentTime = 0
      this.currentTime = 0
    },
    /** 下一曲（仅手动触发）；队列只有一首时重播该曲 */
    async next() {
      if (!this.queue.length) return
      const n = (this.currentIndex + 1) % this.queue.length
      this.currentId = this.queue[n].id
      this.currentTime = 0
      await this._loadAndPlay()
      if (this.current) songApi.play(this.current.id).catch(() => {})
      this._updateMediaSession(this.current)
    },
    /** 上一曲（仅手动触发） */
    async prev() {
      if (!this.queue.length) return
      const p = (this.currentIndex - 1 + this.queue.length) % this.queue.length
      this.currentId = this.queue[p].id
      this.currentTime = 0
      await this._loadAndPlay()
      if (this.current) songApi.play(this.current.id).catch(() => {})
      this._updateMediaSession(this.current)
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
