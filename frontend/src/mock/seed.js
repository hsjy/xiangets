/**
 * 演示种子数据（首次访问时写入 localStorage，之后完全由接口增删改查驱动）。
 * 音频来自 SoundHelix 免费示例曲库（192kbps MP3，可自由试听），
 * 时长为真实探测值。歌手/歌名/歌单均为演示用虚构内容。
 */
const AUDIO = (n) => `https://www.soundhelix.com/examples/mp3/SoundHelix-Song-${n}.mp3`

const base = new Date('2024-01-10T10:00:00+08:00').getTime()

/**
 * 种子账号。demo 是公开演示账号；admin 仅保留数据行以兼容历史关联，
 * mock 登录已封禁（见 server.js login），真实后端的管理员与本文件无关。
 */
export const SEED_USERS = [
  { id: 1, username: 'demo', password: 'demo123456', name: '演示用户', avatar: '', sex: 0, phone: '13800000000', email: 'demo@yuetin.example', birth: '2000-01-01', intro: '这个人很懒，但很爱听歌。', role: 'user', createTime: base },
  { id: 2, username: 'admin', password: 'admin123', name: '管理员', avatar: '', sex: 1, phone: '', email: 'admin@yuetin.example', birth: '', intro: '站点管理员。', role: 'admin', createTime: base }
]

// 真实探测时长（秒）
const DUR = {
  1: 372.7, 2: 425.9, 3: 344.0, 4: 302.5, 5: 353.9, 6: 279.6,
  7: 420.6, 8: 325.3, 9: 389.3, 10: 527.3, 11: 551.0, 12: 513.6,
  13: 470.0, 14: 516.2, 15: 438.0, 16: 483.4
}

const SINGERS = [
  { id: 1, name: '林晚晴', gender: 1, birth: '1996-04-18', location: '浙江 杭州', style: '流行', intro: '声音干净温暖的创作型歌手，擅长把日常心事写成旋律。代表作《晚风信》《时光邮局》。' },
  { id: 2, name: '星尘乐团', gender: 2, birth: '2015-09-01', location: '北京', style: '摇滚', intro: '五人制独立摇滚乐团，以恢弘编曲与少年感歌词著称，现场感染力极强。' },
  { id: 3, name: '陈叙白', gender: 0, birth: '1990-11-06', location: '四川 成都', style: '民谣', intro: '行走过的城市都写进了歌里。一把木吉他，唱尽小镇与远方。' },
  { id: 4, name: '苏酥', gender: 1, birth: '1999-07-22', location: '上海', style: '流行', intro: '甜而不腻的嗓音自带气泡感，是夏日限定的快乐源泉。' },
  { id: 5, name: '慕辰', gender: 0, birth: '1993-02-14', location: '广东 深圳', style: '电子', intro: '电子音乐制作人，专注合成器美学，在霓虹与深海之间构建声场。' },
  { id: 6, name: '顾云深', gender: 0, birth: '1988-12-03', location: '陕西 西安', style: '古风', intro: '以山水入词、以丝竹入曲，用现代编曲复活古典意境。' },
  { id: 7, name: '夏未至', gender: 1, birth: '1997-05-30', location: '湖南 长沙', style: 'R&B', intro: '律动至上的 R&B 新声代，唱腔慵懒，句句都是深夜心事。' },
  { id: 8, name: '岚烟', gender: 1, birth: '1995-08-09', location: '江苏 苏州', style: '古风', intro: '江南女子，歌声如画。一笔一墨皆是山河岁月。' }
]

const styleOf = { 1: '流行', 2: '摇滚', 3: '民谣', 4: '流行', 5: '电子', 6: '古风', 7: 'R&B', 8: '古风' }

const SONG_TITLES = {
  1: '晚风信', 2: '时光邮局', 3: '银河漫游', 4: '逆光飞行',
  5: '南方小镇', 6: '旧巷', 7: '甜品店', 8: '夏日气泡',
  9: '霓虹雨', 10: '深空电台', 11: '墨色山水', 12: '长安引',
  13: '深夜便利店', 14: '心动信号', 15: '山河图', 16: '画中人'
}

// 歌手 -> 歌曲编号
const SONGS_OF = { 1: [1, 2], 2: [3, 4], 3: [5, 6], 4: [7, 8], 5: [9, 10], 6: [11, 12], 7: [13, 14], 8: [15, 16] }

const PLAY_COUNT = {
  1: 284567, 2: 156234, 3: 512081, 4: 98765, 5: 342190, 6: 77432,
  7: 423675, 8: 654301, 9: 231456, 10: 188932, 11: 76543, 12: 334567,
  13: 456789, 14: 567890, 15: 123456, 16: 210987
}

export function buildSeed() {
  const songs = Object.entries(SONGS_OF).flatMap(([sid, nums]) =>
    nums.map((n, idx) => ({
      id: n,
      singerId: Number(sid),
      name: SONG_TITLES[n],
      url: AUDIO(n),
      style: styleOf[sid],
      duration: DUR[n],
      playCount: PLAY_COUNT[n],
      createTime: base + n * 86400000 * 3
    }))
  )

  const playlist = [
    { id: 1, name: '深夜学习时段', style: '学习', creatorId: null, cover: pic(1), intro: '适合专注的安静旋律，让思绪随音乐沉淀。', createTime: base },
    { id: 2, name: '通勤路上的轻快旋律', style: '通勤', creatorId: null, cover: pic(2), intro: '早高峰也要有好心情，节奏刚刚好的歌单。', createTime: base + 86400000 * 10 },
    { id: 3, name: '雨夜古风精选', style: '古风', creatorId: null, cover: pic(3), intro: '窗外落雨，室内茶香，一曲古风入梦来。', createTime: base + 86400000 * 25 },
    { id: 4, name: '电子音浪派对', style: '电子', creatorId: null, cover: pic(4), intro: '合成器浪潮来袭，重低音爱好者请进。', createTime: base + 86400000 * 40 },
    { id: 5, name: '华语流行新歌潮', style: '流行', creatorId: null, cover: pic(5), intro: '近期最值得一听的华语流行作品合集。', createTime: base + 86400000 * 55 },
    { id: 6, name: '睡前轻音', style: '助眠', creatorId: null, cover: pic(6), intro: '温柔收官这一天，愿你好梦。', createTime: base + 86400000 * 70 }
  ]

  const playlistSong = [
    { playlistId: 1, songId: 1 }, { playlistId: 1, songId: 4 }, { playlistId: 1, songId: 6 },
    { playlistId: 1, songId: 8 }, { playlistId: 1, songId: 12 },
    { playlistId: 2, songId: 2 }, { playlistId: 2, songId: 3 }, { playlistId: 2, songId: 7 },
    { playlistId: 2, songId: 8 }, { playlistId: 2, songId: 14 },
    { playlistId: 3, songId: 6 }, { playlistId: 3, songId: 11 }, { playlistId: 3, songId: 12 },
    { playlistId: 3, songId: 15 }, { playlistId: 3, songId: 16 },
    { playlistId: 4, songId: 3 }, { playlistId: 4, songId: 9 }, { playlistId: 4, songId: 10 },
    { playlistId: 4, songId: 13 },
    { playlistId: 5, songId: 1 }, { playlistId: 5, songId: 2 }, { playlistId: 5, songId: 7 },
    { playlistId: 5, songId: 8 }, { playlistId: 5, songId: 13 }, { playlistId: 5, songId: 14 },
    { playlistId: 6, songId: 5 }, { playlistId: 6, songId: 11 }, { playlistId: 6, songId: 12 },
    { playlistId: 6, songId: 16 }
  ]

  return {
    version: 1,
    seq: { user: 3, singer: 8, song: 16, playlist: 6, comment: 2 },
    user: SEED_USERS,
    singer: SINGERS,
    song: songs,
    playlist,
    playlistSong,
    comment: [
      { id: 1, songId: 1, userId: 1, content: '前奏一响就像收到了一封夏天的信！', createTime: base + 86400000 * 12 },
      { id: 2, songId: 3, userId: 2, content: '副歌的吉他太顶了，单曲循环中。', createTime: base + 86400000 * 20 }
    ],
    // 用户行为数据：收藏歌单 / 喜欢的歌曲（按用户 id 分组）
    favorite: { 1: [1, 3] },
    like: {}
  }
}

function pic(n) {
  return `https://picsum.photos/seed/yuetin-playlist-${n}/400`
}

function dicebear(seed) {
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${seed}&backgroundColor=b6e3f4,c0aede,d1d4f9`
}
