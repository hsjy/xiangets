/**
 * Mock RESTful 服务：在浏览器内模拟 SpringBoot 后端。
 * 路由、状态码、响应结构与真实后端（../api 文档约定）完全一致，
 * 真实环境下由 axios 直连 HTTP，本文件不参与。
 *
 * 统一响应：{ code, message, data }；鉴权失败 401，无权限 403，资源不存在 404，参数错误 400。
 */
import { loadDB, tx } from './db'
import { POETS, POEMS, POEM_TRACKS } from './poetry_data'
import { signToken, verifyToken } from './jwt'

/* ---------------- 响应助手 ---------------- */
const ok = (data = null) => ({ status: 200, data: { code: 200, message: 'success', data } })
const fail = (status, message, code = status) => ({ status, data: { code, message, data: null } })

function getAuthPayload(ctx) {
  // 与真实后端一致：剥离 "Bearer " 前缀后再校验
  const raw = String(ctx.headers?.authorization || '').replace(/^Bearer\s+/i, '').trim()
  return verifyToken(raw)
}

async function requireAuth(ctx) {
  const payload = await getAuthPayload(ctx)
  if (!payload) {
    return { error: fail(401, '登录已过期，请重新登录', 401) }
  }
  const db = loadDB()
  const user = db.user.find((u) => u.id === payload.uid)
  if (!user) {
    return { error: fail(401, '用户不存在或已被注销', 401) }
  }
  return { user, payload }
}

function requireAdmin(user) {
  if (!user || user.role !== 'admin') return fail(403, '无权限执行该操作，需要管理员身份')
  return null
}

/* ---------------- 视图组装 ---------------- */
function songView(db, song) {
  const singer = db.singer.find((s) => s.id === song.singerId)
  return { ...song, singerName: singer ? singer.name : '未知歌手' }
}
function playlistView(db, pl) {
  const creator = pl.creatorId ? db.user.find((u) => u.id === pl.creatorId) : null
  const count = db.playlistSong.filter((r) => r.playlistId === pl.id).length
  return { ...pl, creatorName: creator ? creator.name : '官方', creatorAvatar: creator ? creator.avatar : '', songCount: count }
}
function commentView(db, c) {
  const user = db.user.find((u) => u.id === c.userId)
  return { ...c, username: user ? user.name : '已注销用户', avatar: user ? user.avatar : '' }
}

/* ---------------- 业务规则 ---------------- */
function canManagePlaylist(user, pl) {
  return user && (user.role === 'admin' || pl.creatorId === user.id)
}

function cascadeDeleteSong(db, songId) {
  db.playlistSong = db.playlistSong.filter((r) => r.songId !== songId)
  db.comment = db.comment.filter((c) => c.songId !== songId)
  for (const uid of Object.keys(db.like)) {
    db.like[uid] = db.like[uid].filter((id) => id !== songId)
  }
}
function cascadeDeletePlaylist(db, playlistId) {
  db.playlistSong = db.playlistSong.filter((r) => r.playlistId !== playlistId)
  for (const uid of Object.keys(db.favorite)) {
    db.favorite[uid] = db.favorite[uid].filter((id) => id !== playlistId)
  }
}

/* ---------------- 路由表 ---------------- */
export async function handle(ctx) {
  const { method, path } = ctx
  const seg = path.split('/').filter(Boolean) // e.g. ['playlist','song','add']

  try {
    /* ===== 认证模块 ===== */
    if (seg[0] === 'auth') {
      if (method === 'POST' && seg[1] === 'register') return register(ctx.body)
      if (method === 'POST' && seg[1] === 'login') return login(ctx.body)
      if (method === 'GET' && seg[1] === 'me') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        return ok(publicUser(user))
      }
    }

    /* ===== 用户管理 ===== */
    if (seg[0] === 'user') {
      if (method === 'GET' && seg[1] === 'list') {
        const db = loadDB()
        return ok(db.user.map(publicUser))
      }
      if (method === 'GET' && seg[1] === 'detail') {
        const db = loadDB()
        const u = db.user.find((x) => x.id === Number(seg[2]))
        return u ? ok(publicUser(u)) : fail(404, '用户不存在')
      }
      if (method === 'PUT' && seg[1] === 'update') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const targetId = Number(seg[2])
        if (user.id !== targetId && user.role !== 'admin') return fail(403, '只能修改自己的资料')
        return tx((db) => {
          const u = db.user.find((x) => x.id === targetId)
          if (!u) return fail(404, '用户不存在')
          const b = ctx.body || {}
          if (b.name !== undefined) u.name = String(b.name).slice(0, 20)
          if (b.sex !== undefined) u.sex = Number(b.sex)
          if (b.birth !== undefined) u.birth = b.birth
          if (b.intro !== undefined) u.intro = String(b.intro).slice(0, 120)
          if (b.phone !== undefined) u.phone = b.phone
          if (b.email !== undefined) u.email = b.email
          if (b.avatar !== undefined) u.avatar = b.avatar
          if (b.password) u.password = String(b.password)
          return ok(publicUser(u))
        })
      }
      if (method === 'DELETE' && seg[1] === 'delete') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const e = requireAdmin(user)
        if (e) return e
        const targetId = Number(seg[2])
        return tx((db) => {
          const idx = db.user.findIndex((x) => x.id === targetId)
          if (idx === -1) return fail(404, '用户不存在')
          if (targetId === user.id) return fail(400, '不能删除当前登录的账号')
          db.user.splice(idx, 1)
          // 级联：其歌单、评论、收藏与喜欢记录
          db.playlist.filter((p) => p.creatorId === targetId).forEach((p) => cascadeDeletePlaylist(db, p.id))
          db.playlist = db.playlist.filter((p) => p.creatorId !== targetId)
          db.comment = db.comment.filter((c) => c.userId !== targetId)
          delete db.favorite[targetId]
          delete db.like[targetId]
          return ok()
        })
      }
    }

    /* ===== 歌手管理 ===== */
    if (seg[0] === 'singer') {
      if (method === 'GET' && seg[1] === 'list') {
        const db = loadDB()
        const keyword = (ctx.query.keyword || '').trim()
        let list = db.singer
        if (keyword) {
          list = list.filter((s) => s.name.includes(keyword) || (s.style || '').includes(keyword) || (s.location || '').includes(keyword))
        }
        const withCount = list.map((s) => ({ ...s, songCount: db.song.filter((x) => x.singerId === s.id).length }))
        return ok(withCount)
      }
      if (method === 'GET' && seg[1] === 'detail') {
        const db = loadDB()
        const s = db.singer.find((x) => x.id === Number(seg[2]))
        return s ? ok(s) : fail(404, '歌手不存在')
      }
      if (method === 'POST' && seg[1] === 'add') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const e = requireAdmin(user)
        if (e) return e
        const b = ctx.body || {}
        if (!b.name) return fail(400, '歌手名不能为空')
        return tx((db) => {
          db.singer = db.singer || []
          const id = (db.seq.singer = (db.seq.singer || 100) + 1)
          const singer = { id, name: b.name, gender: Number(b.gender) || 0, birth: b.birth || '', location: b.location || '', style: b.style || '流行', intro: b.intro || '' }
          db.singer.push(singer)
          return ok(singer)
        })
      }
      if (method === 'PUT' && seg[1] === 'update') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const e = requireAdmin(user)
        if (e) return e
        return tx((db) => {
          const s = db.singer.find((x) => x.id === Number(seg[2]))
          if (!s) return fail(404, '歌手不存在')
          Object.assign(s, pick(ctx.body, ['name', 'gender', 'birth', 'location', 'style', 'intro']))
          return ok(s)
        })
      }
      if (method === 'DELETE' && seg[1] === 'delete') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const e = requireAdmin(user)
        if (e) return e
        return tx((db) => {
          const id = Number(seg[2])
          const idx = db.singer.findIndex((x) => x.id === id)
          if (idx === -1) return fail(404, '歌手不存在')
          db.song.filter((x) => x.singerId === id).forEach((x) => cascadeDeleteSong(db, x.id))
          db.song = db.song.filter((x) => x.singerId !== id)
          db.singer.splice(idx, 1)
          return ok()
        })
      }
    }

    /* ===== 歌曲模块 ===== */
    if (seg[0] === 'song') {
      if (method === 'GET' && seg[1] === 'list') {
        const db = loadDB()
        // 诗之音轨：按 poemId 查询时返回朗诵/配乐轨（来自 poetry_data）
        if (ctx.query.poemId) {
          const pid = Number(ctx.query.poemId)
          return ok(POEM_TRACKS.filter((t) => t.poemId === pid)
            .map((t) => ({ ...t, singerName: t.name.match(/（(.+?)）/)?.[1] || '数字乐府' })))
        }
        return ok(songList(db, ctx.query))
      }
      if (method === 'GET' && seg[1] === 'hot') {
        const db = loadDB()
        const limit = Number(ctx.query.limit) || 10
        const list = [...db.song].sort((a, b) => b.playCount - a.playCount).slice(0, limit)
        return ok(list.map((s) => songView(db, s)))
      }
      if (method === 'GET' && seg[1] === 'detail') {
        const db = loadDB()
        const s = db.song.find((x) => x.id === Number(seg[2]))
        return s ? ok(songView(db, s)) : fail(404, '歌曲不存在')
      }
      if (method === 'POST' && seg[1] === 'play') {
        const db0 = loadDB()
        const s = db0.song.find((x) => x.id === Number(seg[2]))
        if (!s) return fail(404, '歌曲不存在')
        return tx((db) => {
          const song = db.song.find((x) => x.id === Number(seg[2]))
          song.playCount = (song.playCount || 0) + 1
          return ok({ playCount: song.playCount })
        })
      }
      if (method === 'POST' && seg[1] === 'like') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        return tx((db) => {
          const songId = Number(seg[2])
          if (!db.song.find((x) => x.id === songId)) return fail(404, '歌曲不存在')
          db.like[user.id] = db.like[user.id] || []
          const i = db.like[user.id].indexOf(songId)
          if (i >= 0) db.like[user.id].splice(i, 1)
          else db.like[user.id].push(songId)
          return ok({ liked: i < 0, likes: db.like[user.id].length })
        })
      }
      if (method === 'GET' && seg[1] === 'like' && seg[2] === 'list') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const db = loadDB()
        const ids = (db.like[user.id] || []).slice()
        const list = ids.map((id) => db.song.find((x) => x.id === id)).filter(Boolean).map((s) => songView(db, s))
        return ok(list)
      }
      if (method === 'POST' && seg[1] === 'add') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const e = requireAdmin(user)
        if (e) return e
        const b = ctx.body || {}
        if (!b.name) return fail(400, '歌曲名不能为空')
        if (!b.singerId) return fail(400, '请选择歌手')
        if (!b.url) return fail(400, '请提供音频地址')
        return tx((db) => {
          const id = (db.seq.song = (db.seq.song || 100) + 1)
          const song = {
            id, singerId: Number(b.singerId), name: b.name, url: b.url,
            style: b.style || '流行', duration: Number(b.duration) || 0,
            playCount: 0, createTime: Date.now()
          }
          db.song.push(song)
          return ok(songView(db, song))
        })
      }
      if (method === 'PUT' && seg[1] === 'update') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const e = requireAdmin(user)
        if (e) return e
        return tx((db) => {
          const s = db.song.find((x) => x.id === Number(seg[2]))
          if (!s) return fail(404, '歌曲不存在')
          Object.assign(s, pick(ctx.body, ['name', 'singerId', 'url', 'style', 'duration']))
          return ok(songView(db, s))
        })
      }
      if (method === 'DELETE' && seg[1] === 'delete') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const e = requireAdmin(user)
        if (e) return e
        return tx((db) => {
          const idx = db.song.findIndex((x) => x.id === Number(seg[2]))
          if (idx === -1) return fail(404, '歌曲不存在')
          cascadeDeleteSong(db, db.song[idx].id)
          db.song.splice(idx, 1)
          return ok()
        })
      }
    }

    /* ===== 歌单模块 ===== */
    if (seg[0] === 'playlist') {
      if (method === 'GET' && seg[1] === 'list') {
        const db = loadDB()
        const keyword = (ctx.query.keyword || '').trim()
        const style = (ctx.query.style || '').trim()
        let list = db.playlist
        if (keyword) list = list.filter((p) => p.name.includes(keyword) || (p.style || '').includes(keyword))
        if (style) list = list.filter((p) => p.style === style)
        return ok(list.map((p) => playlistView(db, p)))
      }
      if (method === 'GET' && seg[1] === 'my') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const db = loadDB()
        const created = db.playlist.filter((p) => p.creatorId === user.id).map((p) => playlistView(db, p))
        const favIds = db.favorite[user.id] || []
        const favorited = favIds.map((id) => db.playlist.find((p) => p.id === id)).filter(Boolean).map((p) => playlistView(db, p))
        return ok({ created, favorited })
      }
      if (method === 'GET' && seg[1] === 'detail') {
        const db = loadDB()
        const p = db.playlist.find((x) => x.id === Number(seg[2]))
        if (!p) return fail(404, '歌单不存在')
        const favIds = (await getAuthPayload(ctx)) ? db.favorite[(await getAuthPayload(ctx)).uid] || [] : []
        return ok({ ...playlistView(db, p), favorited: favIds.includes(p.id) })
      }
      if (method === 'POST' && seg[1] === 'add') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const b = ctx.body || {}
        if (!b.name || !String(b.name).trim()) return fail(400, '歌单名不能为空')
        return tx((db) => {
          const id = (db.seq.playlist = (db.seq.playlist || 100) + 1)
          const pl = {
            id, name: String(b.name).trim().slice(0, 30), style: b.style || '我的收藏',
            creatorId: user.id, cover: b.cover || `https://picsum.photos/seed/yuetin-upl-${id}-${user.id}/400`,
            intro: String(b.intro || '').slice(0, 120), createTime: Date.now()
          }
          db.playlist.push(pl)
          return ok(playlistView(db, pl))
        })
      }
      if (method === 'PUT' && seg[1] === 'update') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        return tx((db) => {
          const pl = db.playlist.find((x) => x.id === Number(seg[2]))
          if (!pl) return fail(404, '歌单不存在')
          if (!canManagePlaylist(user, pl)) return fail(403, '只有创建者可以编辑该歌单')
          const b = pick(ctx.body, ['name', 'style', 'intro', 'cover'])
          if (b.name !== undefined && !String(b.name).trim()) return fail(400, '歌单名不能为空')
          Object.assign(pl, b)
          return ok(playlistView(db, pl))
        })
      }
      if (method === 'DELETE' && seg[1] === 'delete') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        return tx((db) => {
          const id = Number(seg[2])
          const pl = db.playlist.find((x) => x.id === id)
          if (!pl) return fail(404, '歌单不存在')
          if (!canManagePlaylist(user, pl)) return fail(403, '只有创建者可以删除该歌单')
          cascadeDeletePlaylist(db, id)
          db.playlist = db.playlist.filter((x) => x.id !== id)
          return ok()
        })
      }
      if (method === 'POST' && seg[1] === 'favorite') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        return tx((db) => {
          const id = Number(seg[2])
          if (!db.playlist.find((p) => p.id === id)) return fail(404, '歌单不存在')
          db.favorite[user.id] = db.favorite[user.id] || []
          const arr = db.favorite[user.id]
          const i = arr.indexOf(id)
          if (i >= 0) arr.splice(i, 1)
          else arr.push(id)
          return ok({ favorited: i < 0 })
        })
      }

      /* -- 歌单歌曲关联（playlist_song）-- */
      if (seg[1] === 'song') {
        if (method === 'GET' && seg[2] === 'list') {
          const db = loadDB()
          const pid = Number(seg[3])
          const pl = db.playlist.find((x) => x.id === pid)
          if (!pl) return fail(404, '歌单不存在')
          const ids = db.playlistSong.filter((r) => r.playlistId === pid).map((r) => r.songId)
          const list = ids.map((id) => db.song.find((s) => s.id === id)).filter(Boolean).map((s) => songView(db, s))
          return ok(list)
        }
        if (method === 'POST' && seg[2] === 'add') {
          const { user, error } = await requireAuth(ctx)
          if (error) return error
          const b = ctx.body || {}
          return tx((db) => {
            const pl = db.playlist.find((x) => x.id === Number(b.playlistId))
            if (!pl) return fail(404, '歌单不存在')
            if (!canManagePlaylist(user, pl)) return fail(403, '只有创建者可以编辑该歌单')
            const song = db.song.find((x) => x.id === Number(b.songId))
            if (!song) return fail(404, '歌曲不存在')
            if (db.playlistSong.some((r) => r.playlistId === pl.id && r.songId === song.id)) {
              return fail(400, '歌曲已在歌单中')
            }
            db.playlistSong.push({ playlistId: pl.id, songId: song.id })
            return ok(playlistView(db, pl))
          })
        }
        if (method === 'POST' && seg[2] === 'remove') {
          const { user, error } = await requireAuth(ctx)
          if (error) return error
          const b = ctx.body || {}
          return tx((db) => {
            const pl = db.playlist.find((x) => x.id === Number(b.playlistId))
            if (!pl) return fail(404, '歌单不存在')
            if (!canManagePlaylist(user, pl)) return fail(403, '只有创建者可以编辑该歌单')
            const i = db.playlistSong.findIndex((r) => r.playlistId === pl.id && r.songId === Number(b.songId))
            if (i === -1) return fail(404, '歌曲不在该歌单中')
            db.playlistSong.splice(i, 1)
            return ok()
          })
        }
        if (method === 'PUT' && seg[2] === 'order') {
          const { user, error } = await requireAuth(ctx)
          if (error) return error
          const b = ctx.body || {}
          return tx((db) => {
            const pl = db.playlist.find((x) => x.id === Number(b.playlistId))
            if (!pl) return fail(404, '歌单不存在')
            if (!canManagePlaylist(user, pl)) return fail(403, '只有创建者可以编辑该歌单')
            const rows = db.playlistSong.filter((r) => r.playlistId === pl.id)
            const idx = rows.findIndex((r) => r.songId === Number(b.songId))
            if (idx === -1) return fail(404, '歌曲不在该歌单中')
            const swapWith = b.direction === 'up' ? idx - 1 : idx + 1
            if (swapWith < 0 || swapWith >= rows.length) return ok()
            const a = rows[idx], c = rows[swapWith]
            const gi = (r) => db.playlistSong.indexOf(r)
            ;[db.playlistSong[gi(a)], db.playlistSong[gi(c)]] = [db.playlistSong[gi(c)], db.playlistSong[gi(a)]]
            return ok()
          })
        }
      }
    }

    /* ===== 评论模块 ===== */
    if (seg[0] === 'comment') {
      if (method === 'GET' && seg[1] === 'list') {
        const db = loadDB()
        const songId = Number(seg[2])
        const list = db.comment.filter((c) => c.songId === songId).map((c) => commentView(db, c))
        return ok(list)
      }
      if (method === 'POST' && seg[1] === 'add') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const b = ctx.body || {}
        const content = String(b.content || '').trim()
        if (!content) return fail(400, '评论内容不能为空')
        return tx((db) => {
          if (!db.song.find((x) => x.id === Number(b.songId))) return fail(404, '歌曲不存在')
          const id = (db.seq.comment = (db.seq.comment || 100) + 1)
          const c = { id, songId: Number(b.songId), userId: user.id, content: content.slice(0, 200), createTime: Date.now() }
          db.comment.push(c)
          return ok(commentView(db, c))
        })
      }
      if (method === 'DELETE' && seg[1] === 'delete') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        return tx((db) => {
          const idx = db.comment.findIndex((c) => c.id === Number(seg[2]))
          if (idx === -1) return fail(404, '评论不存在')
          if (db.comment[idx].userId !== user.id && user.role !== 'admin') return fail(403, '只能删除自己的评论')
          db.comment.splice(idx, 1)
          return ok()
        })
      }
    }

    /* ===== AI 谱曲（mock：本地即时完成，音频留待真实后端） ===== */
    if (seg[0] === 'ai' && seg[1] === 'music') {
      const db = loadDB()
      db.aiMusic = db.aiMusic || []
      if (method === 'POST' && seg[2] === 'generate') {
        const poemId = Number(ctx.body?.poemId)
        const existed = db.aiMusic.find((j) => j.poemId === poemId && j.status === 'done')
        if (existed) return ok(existed)
        const job = {
          id: db.aiMusic.length + 1, poemId, provider: 'preset',
          prompt: 'chinese traditional, guzheng (mock)', status: 'done',
          audioUrl: '', trackName: '', duration: 60,
          createTime: Date.now(), finishTime: Date.now()
        }
        db.aiMusic.push(job)
        return ok(job)
      }
      if (method === 'GET' && seg[2] === 'job' && seg[3]) {
        const j = db.aiMusic.find((x) => x.id === Number(seg[3]))
        return j ? ok(j) : fail(404, '任务不存在')
      }
      if (method === 'GET' && seg[2] === 'list' && seg[3]) {
        return ok(db.aiMusic.filter((j) => j.poemId === Number(seg[3]) && j.status === 'done'))
      }
    }

    /* ===== 站点统计 ===== */
    if (seg[0] === 'stats' && seg[1] === 'overview' && method === 'GET') {
      const db = loadDB()
      return ok({
        users: db.user.length,
        singers: db.singer.length,
        songs: db.song.length,
        playlists: db.playlist.length,
        totalPlays: db.song.reduce((s, x) => s + (x.playCount || 0), 0)
      })
    }

    /* ===== 诗与诗人（欣赏主体，数据来自 poetry_data.js，与 MySQL 保持一致） ===== */
    if (seg[0] === 'poem' || seg[0] === 'poet') {
      const db = loadDB()
      db.poemComment = db.poemComment || []
      db.poemFav = db.poemFav || {}
      db.seq = db.seq || {}
      const poetName = (id) => POETS.find((p) => p.id === id)?.name || ''
      const poemView = (m) => ({ ...m, poetName: poetName(m.poetId) })

      if (seg[0] === 'poem') {
        if (method === 'GET' && seg[1] === 'list') {
          let list = POEMS.map(poemView)
          if (ctx.query.dynasty) list = list.filter((m) => m.dynasty === ctx.query.dynasty)
          if (ctx.query.mood) list = list.filter((m) => m.mood === ctx.query.mood)
          if (ctx.query.keyword) {
            const k = ctx.query.keyword
            list = list.filter((m) => (m.title + m.subtitle + m.paragraphs + m.poetName).includes(k))
          }
          return ok(list)
        }
        if (method === 'GET' && seg[1] === 'detail') {
          const m = POEMS.map(poemView).find((x) => x.id === Number(seg[2]))
          return m ? ok(m) : fail(404, '诗不存在')
        }
        if (method === 'GET' && seg[1] === 'random') {
          const i = Math.floor(Math.random() * POEMS.length)
          return ok(poemView(POEMS[i]))
        }
        if (method === 'GET' && seg[1] === 'today') {
          if (!POEMS.length) return ok({ poem: null })
          const day = Math.floor(Date.now() / 86400000) % POEMS.length
          return ok({ poem: poemView(POEMS[day]), date: new Date().toISOString().slice(0, 10), index: day, total: POEMS.length })
        }
        if (method === 'GET' && seg[1] === 'facets') {
          return ok({
            dynasties: [...new Set(POETS.map((p) => p.dynasty))].sort(),
            moods: [...new Set(POEMS.map((m) => m.mood).filter(Boolean))].sort(),
            poemCount: POEMS.length
          })
        }
        if (seg[1] === 'favorite') {
          const { user, error } = await requireAuth(ctx)
          if (error) return error
          if (method === 'POST' && seg[2] && !seg[3]) {
            const pid = Number(seg[2])
            const arr = (db.poemFav[user.id] = db.poemFav[user.id] || [])
            const i = arr.indexOf(pid)
            if (i > -1) { arr.splice(i, 1); return ok({ favorited: false }) }
            arr.push(pid); return ok({ favorited: true })
          }
          if (method === 'GET' && seg[2] === 'state' && seg[3]) {
            return ok({ favorited: (db.poemFav[user.id] || []).includes(Number(seg[3])) })
          }
          if (method === 'GET' && seg[2] === 'list') {
            const favs = db.poemFav[user.id] || []
            return ok(POEMS.filter((m) => favs.includes(m.id)).map(poemView))
          }
        }
        if (seg[1] === 'comment') {
          const { user, error } = await requireAuth(ctx)
          if (error && !(method === 'GET' && seg[2] === 'list')) return error
          if (method === 'GET' && seg[2] === 'list' && seg[3]) {
            const pid = Number(seg[3])
            return ok(db.poemComment.filter((c) => c.poemId === pid)
              .map((c) => ({ ...c, username: db.user.find((u) => u.id === c.userId)?.name || '已注销', avatar: '' })))
          }
          if (method === 'POST' && seg[2] === 'add') {
            const poemId = Number(ctx.body?.poemId)
            const content = String(ctx.body?.content || '').trim()
            if (!content) return fail(400, '感悟内容不能为空')
            if (!POEMS.find((m) => m.id === poemId)) return fail(404, '诗不存在')
            const c = { id: db.seq.pc || 1, poemId, userId: user.id, content, createTime: Date.now() }
            db.seq.pc = (db.seq.pc || 1) + 1
            db.poemComment.push(c)
            return ok({ ...c, username: user.name, avatar: '' })
          }
          if (method === 'GET' && seg[2] === 'mine') {
            return ok(db.poemComment.filter((c) => c.userId === user.id)
              .map((c) => ({ ...c, poemTitle: POEMS.find((m) => m.id === c.poemId)?.title })))
          }
          if (method === 'DELETE' && seg[2] === 'delete' && seg[3]) {
            const idx = db.poemComment.findIndex((c) => c.id === Number(seg[3]) && c.userId === user.id)
            if (idx < 0) return fail(403, '只能删除自己的感悟')
            db.poemComment.splice(idx, 1)
            return ok()
          }
        }
      }

      if (seg[0] === 'poet') {
        if (method === 'GET' && seg[1] === 'list') {
          let list = POETS.map((p) => ({ ...p, poemCount: POEMS.filter((m) => m.poetId === p.id).length }))
          if (ctx.query.dynasty) list = list.filter((p) => p.dynasty === ctx.query.dynasty)
          return ok(list)
        }
        if (method === 'GET' && seg[1] === 'detail' && seg[2]) {
          const p = POETS.find((x) => x.id === Number(seg[2]))
          if (!p) return fail(404, '诗人不存在')
          return ok({
            poet: { ...p, poemCount: POEMS.filter((m) => m.poetId === p.id).length },
            poems: POEMS.filter((m) => m.poetId === p.id).map(poemView)
          })
        }
      }
    }

    return fail(404, `接口不存在: ${method} /${seg.join('/')}`)
  } catch (err) {
    console.error('[mock-server] 未处理异常', err)
    return fail(500, '服务器内部错误：' + (err && err.message))
  }
}

/* ---------------- 工具函数 ---------------- */
function publicUser(u) {
  const { password, ...rest } = u
  return rest
}
function pick(obj, keys) {
  const out = {}
  if (!obj) return out
  for (const k of keys) if (obj[k] !== undefined) out[k] = obj[k]
  return out
}
function songList(db, q) {
  let list = db.song.slice()
  const singerId = Number(q.singerId) || 0
  const style = (q.style || '').trim()
  const keyword = (q.keyword || '').trim()
  if (singerId) list = list.filter((s) => s.singerId === singerId)
  if (style) list = list.filter((s) => s.style === style)
  if (keyword) {
    const hit = (s) => {
      const singer = db.singer.find((x) => x.id === s.singerId)
      return s.name.includes(keyword) || (singer && singer.name.includes(keyword))
    }
    list = list.filter(hit)
  }
  const sort = q.sort || 'pop'
  if (sort === 'pop') list.sort((a, b) => b.playCount - a.playCount)
  else if (sort === 'time') list.sort((a, b) => b.createTime - a.createTime)
  else if (sort === 'name') list.sort((a, b) => a.name.localeCompare(b.name, 'zh-Hans-CN'))
  return list.map((s) => songView(db, s))
}

/* ---------------- 登录 / 注册 ---------------- */
async function login(body) {
  const { username, password } = body || {}
  if (!username || !password) return fail(400, '请输入用户名和密码')
  const db = loadDB()
  const user = db.user.find((u) => u.username === String(username).trim())
  if (!user || user.password !== String(password)) return fail(400, '用户名或密码错误')
  const token = await signToken({ uid: user.id, role: user.role })
  return ok({ token, user: publicUser(user) })
}

async function register(body) {
  const b = body || {}
  const username = String(b.username || '').trim()
  const password = String(b.password || '')
  if (!/^[a-zA-Z0-9_]{3,16}$/.test(username)) return fail(400, '用户名需为 3-16 位字母、数字或下划线')
  if (password.length < 6) return fail(400, '密码长度至少 6 位')
  const db = loadDB()
  if (db.user.some((u) => u.username === username)) return fail(400, '用户名已被注册')
  const id = (db.seq.user = (db.seq.user || 100) + 1)
  const user = {
    id, username, password,
    name: String(b.name || '').trim().slice(0, 20) || username,
    avatar: '',
    sex: 0, phone: '', email: '', birth: '', intro: '', role: 'user', createTime: Date.now()
  }
  const r = tx((d) => {
    d.user.push(user)
    return true
  })
  if (!r) return fail(500, '注册失败')
  const token = await signToken({ uid: user.id, role: user.role })
  return ok({ token, user: publicUser(user) })
}
