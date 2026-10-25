/**
 * 本地"数据库"：以 localStorage 持久化，表结构与后端 MySQL schema 一一对应。
 * 表：user / singer / song / playlist / playlist_song（歌单歌曲关联表）
 *
 * 说明：静态托管（GitHub Pages）环境下用它代替 MySQL；
 * 联调真实后端时 api/client.js 会切换到真实 HTTP 请求，本模块不参与。
 */
import { buildSeed, SEED_USERS } from './seed'

const DB_KEY = 'yuetin_db_v1'

let cache = null

export function loadDB() {
  if (cache) return cache
  try {
    const raw = localStorage.getItem(DB_KEY)
    if (raw) {
      cache = JSON.parse(raw)
      // 种子账号对齐：老浏览器库可能存着旧版种子密码，按用户名同步（保留用户产生的其余数据）
      let dirty = false
      for (const su of SEED_USERS) {
        const row = (cache.user || []).find((u) => u.username === su.username)
        if (row) {
          if (row.password !== su.password) { row.password = su.password; dirty = true }
        } else {
          cache.user.push({ ...su })
          dirty = true
        }
      }
      if (dirty) saveDB()
      return cache
    }
  } catch (e) {
    console.warn('[mock-db] 读取本地数据库失败，将重建种子数据', e)
  }
  cache = buildSeed()
  saveDB()
  return cache
}

export function saveDB() {
  if (!cache) return
  try {
    localStorage.setItem(DB_KEY, JSON.stringify(cache))
  } catch (e) {
    console.warn('[mock-db] 写入 localStorage 失败', e)
  }
}

/** 自增主键 */
export function nextId(table) {
  const db = loadDB()
  db.seq = db.seq || {}
  db.seq[table] = (db.seq[table] || 100) + 1
  return db.seq[table]
}

/** 直接操作库（配合 saveDB 提交事务式修改） */
export function tx(fn) {
  const db = loadDB()
  const result = fn(db)
  saveDB()
  return result
}

/** 重置演示数据（设置页/控制台可调用） */
export function resetDB() {
  cache = null
  localStorage.removeItem(DB_KEY)
  return loadDB()
}
