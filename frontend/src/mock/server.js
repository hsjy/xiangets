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

/** 归一化：去标点空白，用于对句比较 */
const normLine = (s) => String(s || '').replace(/[，。？！；、：""''\s]/g, '')

/** mock 主题选集（与后端 V17 种子对齐） */
const MOCK_COLLECTIONS = [
  { id: 1, title: '明月与乡愁', subtitle: '一轮月，照千古离人', intro: '从床前明月到千里共婵娟，月亮是中国人共同的信箱。', mood: '月夜', sort: 1, poemIds: [3, 10, 13, 18, 31, 77, 78, 79, 96, 99, 117, 95] },
  { id: 2, title: '边塞烽烟', subtitle: '大漠孤烟，金戈铁马', intro: '出塞、从军、国殇——戍卒的月与将军的血。', mood: '家国', sort: 2, poemIds: [33, 35, 63, 73, 75, 82, 94, 64, 98] },
  { id: 3, title: '田园与归隐', subtitle: '种豆南山，采菊东篱', intro: '把身体交给土地，把心交给自然。', mood: '田园', sort: 3, poemIds: [1, 2, 7, 8, 12, 23, 30, 57, 67, 80, 85, 86, 115] },
  { id: 4, title: '长恨与深情', subtitle: '此情可待成追忆', intro: '从上古誓言到纳兰一叹，深情者的独白。', mood: '怀人', sort: 4, poemIds: [37, 38, 39, 47, 49, 51, 60, 61, 62, 69, 70, 71, 91, 92, 102, 103, 116, 22] },
  { id: 5, title: '壮志与家国', subtitle: '铁马冰河，丹心汗青', intro: '他们把失意与不屈，都写成了山河。', mood: '家国', sort: 5, poemIds: [11, 24, 26, 27, 28, 43, 58, 59, 64, 65, 66, 93, 95, 97, 109, 110, 111, 114, 118, 119] },
  { id: 6, title: '四季流转', subtitle: '人间寒暑皆成诗', intro: '春晓、夏荷、秋思、冬梅，一年的风景都在诗里。', mood: '闲适', sort: 6, poemIds: [21, 29, 42, 56, 76, 84, 87, 105, 106, 107, 108, 112, 113, 36] },
  { id: 7, title: '山水行旅', subtitle: '烟霞入眼皆成章', intro: '从庐山飞瀑到鹳雀高楼，行踪所至，山水入诗。', mood: '山水', sort: 7, poemIds: [4, 6, 19, 20, 25, 32, 46, 50, 74, 88] },
  { id: 8, title: '长亭古道', subtitle: '劝君更尽一杯酒', intro: '离别是中国诗最古老的母题——古道、残笛与远行的帆。', mood: '送别', sort: 8, poemIds: [9, 15, 34, 45, 81] },
  { id: 9, title: '哲思与怀古', subtitle: '读史早知今日事', intro: '咏史以鉴今，观物以明理——诗是最好的哲学。', mood: '哲理', sort: 9, poemIds: [40, 44, 52, 55, 68, 72, 89, 90, 100, 104, 120, 121] }
]

/** mock 诗人关系（与后端 V18 对齐；按名字定位 id） */
const MOCK_RELATIONS = [
  ['李白', '孟浩然', '倾慕', '黄鹤楼送别孟浩然下扬州，「孤帆远影碧空尽」，把对前辈的倾慕望成了一江春水；另有「吾爱孟夫子，风流天下闻」，倾慕之情毫不掩饰。'],
  ['李白', '杜甫', '挚友', '天宝三载洛阳相识，同游梁宋齐鲁，「醉眠秋共被，携手日同行」。此后一别再未重逢，杜甫写了十余首诗怀念李白，李白回得少——情谊深浅不在回信多少。'],
  ['李白', '王昌龄', '挚友', '闻王昌龄贬龙标，遥寄一诗：「我寄愁心与明月，随风直到夜郎西」——把自己的心拆一半托付给月亮，送去贬所。'],
  ['李白', '高适', '交游', '天宝三载与杜甫、高适同游梁宋，登吹台、怀古纵猎。杜甫晚年忆及仍写「忆与高李辈，论交入酒垆」。'],
  ['李白', '贺知章', '知遇', '长安紫极宫初逢，贺知章读《蜀道难》惊呼「谪仙人」，当场解下腰间金龟换酒共醉。一别成永诀——贺知章殁后，李白再访不遇，作《对酒忆贺监》。'],
  ['杜甫', '高适', '交游', '梁宋之游同伴；晚年高适治蜀，杜甫成都草堂遇困，高适及时送米相济。'],
  ['杜甫', '岑参', '交游', '同朝为官、往来酬唱，杜甫有《九日寄岑参》，雨天念友不能同游。'],
  ['杜甫', '贺知章', '交游', '《饮中八仙歌》第一位写的就是贺知章：「知章骑马似乘船，眼花落井水底眠」——以画笔为前辈存像。'],
  ['王昌龄', '孟浩然', '交游', '开元末王昌龄游襄阳访孟浩然，纵情宴饮，食鲜疾发而逝——这场重逢成了永别，也是唐诗史上最重的一次聚餐。'],
  ['王昌龄', '高适', '交游', '开元中与王之涣、高适雪天会于旗亭，赌梨园伶人唱谁的诗多——「旗亭画壁」成为唐诗界最著名的赌局。'],
  ['王昌龄', '王之涣', '交游', '旗亭画壁之会，压轴双鬟唱的正是王之涣「黄河远上白云间」，三人抚掌大笑。'],
  ['高适', '王之涣', '交游', '旗亭画壁之会，三人以诗赌胜，高适首轮落败而不服，终为王之涣拔得头筹。'],
  ['王维', '孟浩然', '并称', '「王孟」并称山水田园双璧；孟浩然殁后，王维过郢州画其像于刺史亭，曰浩然亭，后改孟亭。'],
  ['陶渊明', '王维', '文脉', '王维辋川诗「复值接舆醉，狂歌五柳前」——五柳先生是他心里隐逸的标高。'],
  ['陶渊明', '苏轼', '文脉', '东坡晚年被贬儋州，遍和陶诗一百余首，自言「欲以晚节师范其万一」。'],
  ['白居易', '元稹', '挚友', '元白唱和千余首；元稹闻白居易贬江州，「垂死病中惊坐起」——通讯靠驿马，情谊却从未迟到。'],
  ['白居易', '刘禹锡', '挚友', '晚年同居洛阳诗酒唱和，合刊《刘白唱和集》；刘禹锡殁，白居易哭之曰「四海齐名白与刘」。'],
  ['韩愈', '孟郊', '交游', '联句成癖，以奇险相激，韩孟诗派由此得名；孟郊殁，韩愈作墓志铭。'],
  ['韩愈', '贾岛', '知遇', '「僧敲月下门」冲撞京兆尹仪仗，韩愈非但不罪，反与其共论推敲——一文坛佳话，一诗人转机。'],
  ['孟郊', '贾岛', '并称', '「郊寒岛瘦」——元好问以后，两人的苦吟并成一个审美词。'],
  ['李商隐', '杜牧', '并称', '「小李杜」；李商隐赠诗「刻意伤春复伤别，人间唯有杜司勋」——惺惺相惜，隔代知音。'],
  ['晏殊', '欧阳修', '师承', '晏殊权知贡举取中欧阳修，座主门生，同开北宋倚声之门。'],
  ['欧阳修', '范仲淹', '同志', '庆历新政同进同退；范仲淹贬饶州，满朝无人敢言，欧阳修愤而致书司谏高若讷，因此再贬夷陵。'],
  ['欧阳修', '苏轼', '师承', '嘉祐二年主贡举，读苏轼文谓梅尧臣：「老夫当避路，放他出一头地」——此后真的一路避让，把文坛让给了他。'],
  ['欧阳修', '王安石', '知遇', '王安石布衣时，欧阳修读其文「叹誉不已」，又赠诗延誉；后政见相左而私谊不改。'],
  ['苏轼', '秦观', '师承', '苏门四学士之首；秦观殁于北归途中，苏轼恸曰「哀哉！痛哉！世岂复有斯人乎」。'],
  ['苏轼', '王安石', '对手', '乌台诗案，已罢相的王安石上书「安有圣世而杀才士乎」，救了苏轼；八年后金陵相会，唱和累日，恩怨尽付诗酒。'],
  ['苏轼', '辛弃疾', '文脉', '「苏辛」并称豪放双峰——东坡开境，稼轩拓土，中间隔着一百年的山河。'],
  ['陆游', '辛弃疾', '同志', '嘉泰年间同被起用，陆游作诗相送：「中原麟凤争自奋」；两位主战老人，共用一个未竟的中原梦。'],
  ['朱熹', '辛弃疾', '交游', '鹅湖寺重逢，共论天下事十日；朱熹卒时党禁未解，辛弃疾不畏往哭，祭文曰「所不朽者，垂万世名」。'],
  ['李煜', '纳兰性德', '文脉', '王国维评纳兰「北宋以来，一人而已」，而其词直追李后主——两个亡国与伤情之人，隔八百年共鸣。'],
  ['曹操', '曹植', '父子', '铜雀台上命诸子赋诗，曹植援笔立成；宠爱引发争储，也埋下后半生猜忌。'],
  ['刘邦', '项羽', '对手', '楚汉相争四年，一个唱《大风歌》，一个歌《垓下》——胜利与失败，各留一首绝唱给历史。'],
  ['屈原', '李白', '文脉', '「屈平词赋悬日月，楚王台榭空山丘」——李白对屈原的评价，也是他浪漫想象力的源头。'],
  ['柳永', '晏殊', '交游', '柳永谒晏殊求转官，晏殊讥曰「贤俊作曲子么」，柳永回「只如相公亦作曲子」——一场不欢而散的著名对话。'],
  ['晏殊', '范仲淹', '知遇', '晏殊知应天府，聘丁忧中的范仲淹执掌府学，大兴教育；后又荐其为御史——先忧后乐者，最初被看见是在晏殊席上。'],
  ['陆游', '杨万里', '并称', '同列南宋四大家，以诗相知数十年；杨万里评陆游诗「重嚼久之，愈觉有味」，陆游为杨万里诗集作序。']
].map(([n1, n2, relType, note]) => ({
  poetId: POETS.find((p) => p.name === n1)?.id,
  relatedId: POETS.find((p) => p.name === n2)?.id,
  relType, note
})).filter((r) => r.poetId && r.relatedId)

const colView = (c) => ({ id: c.id, title: c.title, subtitle: c.subtitle, intro: c.intro, mood: c.mood, sort: c.sort, poemCount: c.poemIds.length })

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
      /* 忘记密码（mock）：密保问答 + 重置 */
      if (method === 'GET' && seg[1] === 'pwd' && seg[2] === 'question') {
        const db = loadDB()
        const u = db.user.find((x) => x.username === ctx.query.username)
        if (!u || !u.pwdQuestion) return fail(400, '该账号未设置密保问题，请联系管理员重置')
        return ok({ question: u.pwdQuestion })
      }
      if (method === 'POST' && seg[1] === 'pwd' && seg[2] === 'reset') {
        const db = loadDB()
        const u = db.user.find((x) => x.username === ctx.body?.username)
        if (!u || !u.pwdAnswer || normLine(ctx.body?.answer) !== normLine(u.pwdAnswer)) {
          return fail(400, '密保答案不正确')
        }
        if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(ctx.body?.newPassword || '')) {
          return fail(400, '新密码需至少 8 位，且同时包含字母和数字')
        }
        u.password = ctx.body.newPassword
        saveDB(db)
        return ok({ ok: true })
      }
      if (method === 'POST' && seg[1] === 'password') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        if (user.password !== ctx.body?.oldPassword) return fail(400, '当前密码不正确')
        if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(ctx.body?.newPassword || '')) {
          return fail(400, '新密码需至少 8 位，且同时包含字母和数字')
        }
        user.password = ctx.body.newPassword
        saveDB(db)
        return ok({ ok: true })
      }
    }

    /* ===== 用户管理 ===== */
    if (seg[0] === 'user') {
      if (method === 'GET' && seg[1] === 'list') {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const gate = requireAdmin(user)
        if (gate) return gate
        const db = loadDB()
        return ok(db.user.map((u) => ({ ...publicUser(u),
          role: u.role, points: u.points ?? 0, lockedUntil: u.lockedUntil || null })))
      }
      if (method === 'PUT' && seg[1] === 'role' && seg[2]) {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const gate = requireAdmin(user)
        if (gate) return gate
        const db = loadDB()
        const u = db.user.find((x) => x.id === Number(seg[2]))
        if (!u) return fail(404, '用户不存在')
        if (u.id === user.id) return fail(400, '不能修改自己的角色')
        u.role = ctx.body?.role === 'admin' ? 'admin' : 'user'
        saveDB(db)
        return ok()
      }
      if (method === 'POST' && seg[1] === 'reset-pwd' && seg[2]) {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const gate = requireAdmin(user)
        if (gate) return gate
        const db = loadDB()
        const u = db.user.find((x) => x.id === Number(seg[2]))
        if (!u) return fail(404, '用户不存在')
        if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(ctx.body?.newPassword || '')) {
          return fail(400, '新密码需至少 8 位，且同时包含字母和数字')
        }
        u.password = ctx.body.newPassword
        u.lockedUntil = null
        saveDB(db)
        return ok()
      }
      if (method === 'POST' && seg[1] === 'points' && seg[2]) {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const gate = requireAdmin(user)
        if (gate) return gate
        const db = loadDB()
        const u = db.user.find((x) => x.id === Number(seg[2]))
        if (!u) return fail(404, '用户不存在')
        const delta = Number(ctx.body?.delta || 0)
        u.points = Math.max(0, (u.points ?? 0) + delta)
        saveDB(db)
        return ok({ points: u.points })
      }
      if (method === 'POST' && seg[1] === 'unlock' && seg[2]) {
        const { user, error } = await requireAuth(ctx)
        if (error) return error
        const gate = requireAdmin(user)
        if (gate) return gate
        const db = loadDB()
        const u = db.user.find((x) => x.id === Number(seg[2]))
        if (!u) return fail(404, '用户不存在')
        u.lockedUntil = null
        saveDB(db)
        return ok()
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
          if (db.comment[idx].userId !== user.id && user.role !== 'admin') return fail(403, '只能删除自己的感悟')
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

    /* ---------- 策展：选集 / 关系 / 飞花令 / 问诗（mock 版） ---------- */
    if (seg[0] === 'collection') {
      if (method === 'GET' && seg[1] === 'list') return ok(MOCK_COLLECTIONS.map(colView))
      if (method === 'GET' && seg[1] === 'detail' && seg[2]) {
        const c = MOCK_COLLECTIONS.find((x) => x.id === Number(seg[2]))
        if (!c) return fail(404, '选集不存在')
        const poems = POEMS.filter((m) => c.poemIds.includes(m.id))
          .map((m) => ({ ...m, poetName: POETS.find((p) => p.id === m.poetId)?.name || '' }))
        return ok({ collection: colView(c), poems })
      }
    }
    if (seg[0] === 'poet' && seg[1] === 'relations' && seg[2]) {
      const pid = Number(seg[2])
      const rel = MOCK_RELATIONS.filter((r) => r.poetId === pid || r.relatedId === pid)
        .map((r) => {
          const other = r.poetId === pid ? r.relatedId : r.poetId
          const p = POETS.find((x) => x.id === other)
          return p ? { relatedId: p.id, relatedName: p.name, relatedTitle: p.title, relatedDynasty: p.dynasty, relType: r.relType, note: (r.note || '').trim() } : null
        })
        .filter(Boolean)
      return ok(rel)
    }
    if (seg[0] === 'feihua') {
      if (method === 'POST' && seg[1] === 'draw') {
        const ch = String(ctx.body?.char || '')
        const exclude = new Set((ctx.body?.exclude || []).map(normLine))
        const hits = []
        for (const m of POEMS) {
          for (const line of (m.paragraphs || '').split('\n')) {
            if (line.includes(ch) && !exclude.has(normLine(line))) {
              hits.push({ line, title: m.title, dynasty: m.dynasty, poetName: POETS.find((p) => p.id === m.poetId)?.name || '' })
            }
          }
        }
        if (!hits.length) return fail(400, `诗库中未找到含「${ch}」的诗句`)
        return ok(hits[Math.floor(Math.random() * hits.length)])
      }
      if (method === 'POST' && seg[1] === 'check') {
        const ch = String(ctx.body?.char || '')
        const line = normLine(ctx.body?.line || '')
        if (!line.includes(ch)) return fail(400, `对句里没有「${ch}」字`)
        if (line.length < 4) return fail(400, '太短了，至少写完整一句')
        const exists = POEMS.some((m) => normLine(m.paragraphs).includes(line))
        if (!exists) return fail(400, '诗库里没有找到这一句，再想想？')
        const { user, error } = await requireAuth(ctx)
        if (!error && user) {
          user.points = (user.points ?? 0) + 10
          saveDB(db)
        }
        return ok({ ok: true })
      }
    }
    if (seg[0] === 'poem' && seg[1] === 'comment' && seg[2] === 'all') {
      const { user, error } = await requireAuth(ctx)
      if (error) return error
      const gate = requireAdmin(user)
      if (gate) return gate
      const db = loadDB()
      const rows = db.comment.map((c) => {
        const u = db.user.find((x) => x.id === c.userId) || {}
        const m = POEMS.find((x) => x.id === c.poemId) || {}
        return { id: c.id, content: c.content, createTime: c.createTime,
                 username: u.username || '', userName: u.name || '',
                 poemId: c.poemId, poemTitle: m.title || '' }
      }).sort((a, b) => String(b.createTime).localeCompare(String(a.createTime)))
      return ok(rows)
    }

    if (seg[0] === 'poem' && seg[1] === 'ask' && seg[2]) {
      return ok({ answer: '（mock 模式不接入 DeepSeek）此诗的妙处，待连上后端后再与先生细论。' })
    }

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
        if (method === 'GET' && seg[1] === 'page') {
          let list = POEMS.map(poemView)
          if (ctx.query.dynasty) list = list.filter((m) => m.dynasty === ctx.query.dynasty)
          if (ctx.query.mood) list = list.filter((m) => m.mood === ctx.query.mood)
          if (ctx.query.keyword) {
            const k = ctx.query.keyword
            list = list.filter((m) => (m.title + m.subtitle + m.paragraphs + m.poetName).includes(k))
          }
          const total = list.length
          const size = Math.min(100, Number(ctx.query.size) || 48)
          const page = Math.max(1, Number(ctx.query.page) || 1)
          list = list.slice((page - 1) * size, page * size)
          return ok({ list, total, page, size })
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
              .map((c) => ({
                ...c,
                username: db.user.find((u) => u.id === c.userId)?.name || '已注销',
                avatar: '',
                likeCount: (db.commentLike || []).filter((l) => l.commentId === c.id).length,
                likedByMe: (db.commentLike || []).some((l) => l.commentId === c.id && l.userId === user.id)
              })))
          }
          if (method === 'POST' && seg[2] === 'like' && seg[3]) {
            const cid = Number(seg[3])
            db.commentLike = db.commentLike || []
            const idx = db.commentLike.findIndex((l) => l.commentId === cid && l.userId === user.id)
            if (idx >= 0) db.commentLike.splice(idx, 1)
            else db.commentLike.push({ commentId: cid, userId: user.id, createTime: Date.now() })
            saveDB(db)
            return ok({ liked: idx < 0, likeCount: db.commentLike.filter((l) => l.commentId === cid).length })
          }
          if (method === 'POST' && seg[2] === 'add') {
            const poemId = Number(ctx.body?.poemId)
            const content = String(ctx.body?.content || '').trim()
            if (!content) return fail(400, '感悟内容不能为空')
            if (!POEMS.find((m) => m.id === poemId)) return fail(404, '诗不存在')
            const parentId = ctx.body?.parentId ? Number(ctx.body.parentId) : null
            const c = { id: db.seq.pc || 1, poemId, userId: user.id, parentId, content, createTime: Date.now() }
            db.seq.pc = (db.seq.pc || 1) + 1
            db.poemComment.push(c)
            return ok({ ...c, username: user.name, avatar: '' })
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
        if (method === 'GET' && seg[1] === 'page') {
          let list = POETS.map((p) => ({ ...p, poemCount: POEMS.filter((m) => m.poetId === p.id).length }))
          if (ctx.query.dynasty) list = list.filter((p) => p.dynasty === ctx.query.dynasty)
          if (ctx.query.keyword) {
            const k = ctx.query.keyword
            list = list.filter((p) => (p.name + p.zi + p.hao + p.title).includes(k))
          }
          const total = list.length
          const size = Math.min(100, Number(ctx.query.size) || 48)
          const page = Math.max(1, Number(ctx.query.page) || 1)
          list = list.slice((page - 1) * size, page * size)
          return ok({ list, total, page, size })
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
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(password)) return fail(400, '密码需至少 8 位，且同时包含字母和数字')
  if (!b.pwdQuestion || !b.pwdAnswer) return fail(400, '请设置密保问题与答案，用于找回密码')
  const db = loadDB()
  if (db.user.some((u) => u.username === username)) return fail(400, '用户名已被注册')
  const id = (db.seq.user = (db.seq.user || 100) + 1)
  const user = {
    id, username, password,
    pwdQuestion: b.pwdQuestion, pwdAnswer: b.pwdAnswer,
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
