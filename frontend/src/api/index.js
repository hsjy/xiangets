/**
 * 接口封装层：与后端 RESTful 规范一一对应。
 * （接口清单与字段详见仓库 docs/API.md）
 */
import request from './client'

/* 认证 */
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  me: () => request.get('/auth/me')
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
