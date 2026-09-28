# 悦听 · RESTful 接口文档

> 基础路径：`/api`（SpringBoot `server.servlet.context-path`）
> 演示站点（mock 模式）直接把同样的路径交给浏览器内的 mock 适配器处理，无需服务器。

## 统一响应格式

所有接口返回同一个信封，前端只需判断 `code`：

```json
{ "code": 200, "message": "success", "data": { } }
```

| code | 含义 | 前端处理 |
| --- | --- | --- |
| 200 | 成功 | 取 `data` 使用 |
| 400 | 参数错误 / 业务校验失败 | `toast.error(message)` |
| 401 | 未登录或登录过期 | 清除本地 token，跳转 `#/login` |
| 403 | 无权限（非创建者、非管理员） | `toast.error(message)` |
| 404 | 资源不存在 | 展示空态 |
| 500 | 服务端异常 | `toast.error(message)` |

## 认证方式

登录后拿到 `token`，之后所有需要身份的请求都要带：

```
Authorization: Bearer <token>
```

token 由 `HS256` 签发，有效期 7 天。

---

## 1. 认证模块 `/auth`

### POST `/auth/register` — 注册

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 3-20 位字母、数字或下划线 |
| password | string | 是 | 至少 6 位 |
| name | string | 否 | 昵称，缺省同 username |
| email / phone / birth / intro | string | 否 | 资料字段 |

`data`：`{ token, user }`

### POST `/auth/login` — 登录

```json
{ "username": "demo", "password": "123456" }
```

`data`：`{ token, user }`。用户名不存在与密码错误返回同一提示，避免用户名枚举。

### GET `/auth/me` — 当前用户 🔒

刷新页面后用于恢复登录态。`data` 为用户对象（不含密码）。

---

## 2. 用户模块 `/user`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/user/list` | 公开 | 用户列表（不含密码等敏感字段） |
| GET | `/user/detail/{id}` | 公开 | 用户详情 |
| PUT | `/user/update/{id}` | 本人 / 管理员 | 改资料；传 `password` 时必须同时传正确的 `oldPassword` |
| DELETE | `/user/delete/{id}` | 本人 / 管理员 | 注销账号，关联数据外键级联清理 |

---

## 3. 歌手模块 `/singer`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/singer/list?keyword=` | 公开 | keyword 同时匹配歌手名 / 风格 / 地区 |
| GET | `/singer/detail/{id}` | 公开 | 歌手详情（含 `songCount`） |
| POST | `/singer/add` | 管理员 | 新增歌手 |
| PUT | `/singer/update/{id}` | 管理员 | 修改歌手 |
| DELETE | `/singer/delete/{id}` | 管理员 | 删除歌手，其名下歌曲级联删除 |

---

## 4. 歌曲模块 `/song`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/song/list` | 公开 | 查询参数 `keyword` `style` `singerId` `sort` |
| GET | `/song/hot?limit=10` | 公开 | 按播放量倒序的热门歌曲 |
| GET | `/song/detail/{id}` | 公开 | 歌曲详情，含 `singerName` |
| POST | `/song/play/{id}` | 公开 | 播放计数 +1，返回 `{ song, playCount }` |
| POST | `/song/like/{id}` | 登录 | 红心切换，返回 `{ liked }` |
| GET | `/song/like/state/{id}` | 公开 | 查询红心状态，匿名访问返回 `liked: false` |
| GET | `/song/like/list` | 登录 | 我喜欢的音乐，按喜欢时间倒序 |
| POST | `/song/add` | 管理员 | 新增歌曲 |
| PUT | `/song/update/{id}` | 管理员 | 修改歌曲 |
| DELETE | `/song/delete/{id}` | 管理员 | 删除歌曲，歌单关联 / 评论 / 红心级联清理 |

**`sort` 取值**：`pop`（播放量，默认）、`time`（最新）、`name`（歌名）。

---

## 5. 歌单模块 `/playlist`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/playlist/list?keyword=&style=` | 公开 | 歌单广场 |
| GET | `/playlist/my` | 登录 | 返回 `{ created, favorited }` |
| GET | `/playlist/detail/{id}` | 公开 | 歌单对象，含 `creatorName` / `songCount` |
| POST | `/playlist/add` | 登录 | 创建歌单，字段 `name` `intro` `style` `cover` |
| PUT | `/playlist/update/{id}` | 创建者 / 管理员 | 修改歌单 |
| DELETE | `/playlist/delete/{id}` | 创建者 / 管理员 | 删除歌单 |
| POST | `/playlist/favorite/{id}` | 登录 | 收藏切换，返回 `{ favorited }` |
| GET | `/playlist/favorite/state/{id}` | 登录 | 查询收藏状态 |

### 歌单歌曲关联

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/playlist/song/list/{playlistId}` | 公开 | 歌单内歌曲，按 `sort_no` 升序 |
| POST | `/playlist/song/add` | 创建者 / 管理员 | 体：`{ playlistId, songId }`，重复加入静默跳过 |
| POST | `/playlist/song/remove` | 创建者 / 管理员 | 体：`{ playlistId, songId }` |
| PUT | `/playlist/song/order` | 创建者 / 管理员 | 体：`{ playlistId, songId, direction }`，`direction` 为 `-1` 上移 / `1` 下移 |

---

## 6. 评论模块 `/comment`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/comment/list/{songId}` | 公开 | 歌曲评论，倒序 |
| POST | `/comment/add` | 登录 | 体：`{ songId, content }`，最多 500 字 |
| DELETE | `/comment/delete/{id}` | 本人 | 只能删自己的评论（管理员也不行，与 mock 层一致） |

---

## 7. 统计 `/stats`

### GET `/stats/overview` — 站点概览

```json
{ "users": 2, "singers": 8, "songs": 16, "playlists": 7, "totalPlays": 4739867 }
```

---

## 权限判定说明

歌单的修改 / 删除采用「**管理员或创建者本人**」规则（`canManage`），
与前端 mock 层 `canManagePlaylist` 完全一致。普通用户对他人歌单的任何写操作都会收到 403。

密码以 `SHA-256(明文 + 固定盐)` 存储，响应中永不出现 `password` 字段
（实体上用 `@JsonProperty(access = WRITE_ONLY)` 保证）。
