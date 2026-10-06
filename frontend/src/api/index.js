/**
 * 接口封装层：与后端 RESTful 规范一一对应。
 * （接口清单与字段详见仓库 docs/API.md）
 */
import request from './client'

/* 认证 */
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  me: () => request.get('/auth/me'),
  pwdQuestion: (username) => request.get('/auth/pwd/question', { params: { username } }),
  pwdReset: (data) => request.post('/auth/pwd/reset', data),
  changePassword: (data) => request.post('/auth/password', data)
}

/* 用户 */
export const userApi = {
  list: () => request.get('/user/list'),
  detail: (id) => request.get(`/user/detail/${id}`),
  update: (id, data) => request.put(`/user/update/${id}`, data),
  remove: (id) => request.delete(`/user/delete/${id}`)
}

/* 歌手 */
export const singerApi = {
  list: (params) => request.get('/singer/list', { params }),
  detail: (id) => request.get(`/singer/detail/${id}`),
  add: (data) => request.post('/singer/add', data),
  update: (id, data) => request.put(`/singer/update/${id}`, data),
  remove: (id) => request.delete(`/singer/delete/${id}`)
}

/* 歌曲 */
export const songApi = {
  list: (params) => request.get('/song/list', { params }),
  hot: (limit = 10) => request.get('/song/hot', { params: { limit } }),
  detail: (id) => request.get(`/song/detail/${id}`),
  play: (id) => request.post(`/song/play/${id}`),
  like: (id) => request.post(`/song/like/${id}`),
  likeList: () => request.get('/song/like/list'),
  add: (data) => request.post('/song/add', data),
  update: (id, data) => request.put(`/song/update/${id}`, data),
  remove: (id) => request.delete(`/song/delete/${id}`)
}

/* 歌单 */
export const playlistApi = {
  list: (params) => request.get('/playlist/list', { params }),
  my: () => request.get('/playlist/my'),
  detail: (id) => request.get(`/playlist/detail/${id}`),
  add: (data) => request.post('/playlist/add', data),
  update: (id, data) => request.put(`/playlist/update/${id}`, data),
  remove: (id) => request.delete(`/playlist/delete/${id}`),
  favorite: (id) => request.post(`/playlist/favorite/${id}`)
}

/* 歌单歌曲关联 */
export const playlistSongApi = {
  list: (playlistId) => request.get(`/playlist/song/list/${playlistId}`),
  add: (playlistId, songId) => request.post('/playlist/song/add', { playlistId, songId }),
  remove: (playlistId, songId) => request.post('/playlist/song/remove', { playlistId, songId }),
  order: (playlistId, songId, direction) => request.put('/playlist/song/order', { playlistId, songId, direction })
}

/* 评论 */
export const commentApi = {
  list: (songId) => request.get(`/comment/list/${songId}`),
  add: (songId, content) => request.post('/comment/add', { songId, content }),
  remove: (id) => request.delete(`/comment/delete/${id}`)
}

/* 统计 */
export const statsApi = {
  overview: () => request.get('/stats/overview')
}

/* 诗与诗人（欣赏主体） */
export const poetryApi = {
  poemList: (params) => request.get('/poem/list', { params }),
  poemPage: (params) => request.get('/poem/page', { params }),
  poemDetail: (id) => request.get(`/poem/detail/${id}`),
  random: () => request.get('/poem/random'),
  today: () => request.get('/poem/today'),
  facets: () => request.get('/poem/facets'),
  poetList: (params) => request.get('/poet/list', { params }),
  poetPage: (params) => request.get('/poet/page', { params }),
  poetDetail: (id) => request.get(`/poet/detail/${id}`)
}

/* AI 谱曲（古诗转音乐） */
export const aiMusicApi = {
  generate: (poemId, force = false) => request.post('/ai/music/generate', { poemId, force }),
  job: (id) => request.get(`/ai/music/job/${id}`),
  list: (poemId) => request.get(`/ai/music/list/${poemId}`)
}

/* 收藏的诗（书斋） */
export const poemFavApi = {
  toggle: (id) => request.post(`/poem/favorite/${id}`),
  state: (id) => request.get(`/poem/favorite/state/${id}`),
  list: () => request.get('/poem/favorite/list')
}

/* 诗的感悟 */
export const poemCommentApi = {
  list: (poemId) => request.get(`/poem/comment/list/${poemId}`),
  add: (poemId, content, parentId) => request.post('/poem/comment/add', { poemId, content, parentId }),
  like: (id) => request.post(`/poem/comment/like/${id}`),
  mine: () => request.get('/poem/comment/mine'),
  remove: (id) => request.delete(`/poem/comment/delete/${id}`)
}

/* 主题选集 */
export const collectionApi = {
  list: () => request.get('/collection/list'),
  detail: (id) => request.get(`/collection/detail/${id}`)
}

/* 诗人关系图谱 */
export const relationApi = {
  byPoet: (id) => request.get(`/poet/relations/${id}`)
}

/* 问诗（DeepSeek 诗歌问答） */
export const askApi = {
  ask: (poemId, question, history) => request.post(`/poem/ask/${poemId}`, { question, history })
}

/* 飞花令 */
export const feihuaApi = {
  draw: (ch, exclude) => request.post('/feihua/draw', { char: ch, exclude }),
  check: (ch, line, exclude) => request.post('/feihua/check', { char: ch, line, exclude })
}

/* 管理后台（仅管理员） */
export const adminApi = {
  songRename: (id, name) => request.put(`/song/update/${id}`, { name }),
  songDelete: (id) => request.delete(`/song/delete/${id}`),
  users: () => request.get('/user/list'),
  setRole: (id, role) => request.put(`/user/role/${id}`, { role }),
  resetUserPwd: (id, newPassword) => request.post(`/user/reset-pwd/${id}`, { newPassword }),
  unlock: (id) => request.post(`/user/unlock/${id}`),
  deleteUser: (id) => request.delete(`/user/delete/${id}`),
  comments: () => request.get('/poem/comment/all'),
  deleteComment: (id) => request.delete(`/poem/comment/delete/${id}`)
}
