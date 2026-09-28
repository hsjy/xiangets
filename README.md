# 悦听 · 在线音乐播放网站

> 2024.01 ~ 2024.05 · Vue3 + SpringBoot + MySQL 前后端分离项目

一个功能完整、可在线体验的在线音乐播放网站：用户登录注册、歌曲与歌手浏览、
HTML5 在线播放、歌单管理、评论与收藏。

**在线体验：** https://hsjy.github.io/yuetin-music/ （演示账号 `demo` / `123456`）

---

## 项目亮点

**一套代码，两种运行模式。** 前端通过自定义 Axios 适配器实现了一个浏览器内的
mock 后端，把完整��� RESTful 契约跑在 localStorage 上。因此纯静态的
GitHub Pages 部署也能登录、建歌单、收藏、评论，功能不是"只能看不能点"的空壳；
而把适配器切回真实模式，同一份前端代码就能直接对接 SpringBoot 服务。

```
mock 模式（默认）              real 模式
前端 ──Axios 适配器──▶ mock/server.js    前端 ──Axios──▶ SpringBoot ──MyBatis──▶ MySQL
         └──▶ localStorage 数据库                                    └──▶ MySQL
```

切换方式：`npm run dev -- --mode real`（见 `src/api/client.js`）。

---

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 前端框架 | Vue 3（`<script setup>` 组合式 API） |
| 构建 | Vite 6 |
| 路由 | Vue Router 4（hash 模式，适配静态托管） |
| 状态管理 | Pinia 2 |
| HTTP | Axios 1.7（自定义 adapter / 拦截器） |
| 后端框架 | Spring Boot 2.7 |
| 持久层 | MyBatis（XML 映射） |
| 数据库 | MySQL 8 |
| 鉴权 | JWT（HS256，jjwt 0.11.5） |
| 播放器 | HTML5 `<audio>` + MediaSession API |

---

## 功能清单

**用户**
- 注册 / 登录 / 退出，JWT 鉴权，7 天有效期
- 个人资料维护（昵称、头像、性别、生日、签名、联系方式）
- 改密码需校验原密码

**浏览**
- 发现页：站点统计 + 歌单广场 + 热门歌曲 + 歌手推荐
- 歌曲库：关键词 / 风格筛选，按热度 / 最新 / 歌名三种排序，客户端分页
- 歌手页：列表检索、歌手详情与该歌手作品
- 歌曲详情：信息、播放、同歌手推荐、评论

**播放**
- 播放 / 暂停、进度拖拽、音量与静音
- 上一首 / 下一首、循环顺序 / 单曲循环 / 随机播放
- 播放队列抽屉：查看队列、切歌、移除、清空
- 桌面媒体控制中心（锁屏 / 耳机按键联动）

**歌单**
- 创建 / 修改 / 删除、收藏 / 取消收藏
- 向歌单添加歌曲、移除歌曲、上移 / 下移排序
- 「我创建的」与「我收藏的」分组管理

**互动**
- 「我喜欢的音乐」红心
- 歌曲评论发表与删除（仅限本人）

---

## 目录结构

```
music/
├── frontend/                    Vue3 前端
│   ├── src/
│   │   ├── api/                 接口封装 + Axios 实例与 mock 适配器
│   │   ├── mock/                浏览器内 mock 后端（server / db / jwt / seed）
│   │   ├── components/          PlayerBar、QueueDrawer、TopBar、SideNav 等
│   │   ├── views/               10 个页面
│   │   ├── stores/              player / user / ui 三个 Pinia store
│   │   ├── router/  utils/      路由与格式化工具
│   │   └── styles/
│   ├── vite.config.js           base: './'，dev 代理 /api → :8080
│   └── package.json
│
├── backend/                     SpringBoot 后端
│   ├── src/main/java/com/yuetin/music/
│   │   ├── common/              统一响应、全局异常、登录上下文
│   │   ├── config/              CORS、JWT 拦截器、密码升级
│   │   ├── entity/  mapper/     实体与 MyBatis 接口
│   │   ├── service/  controller/ 业务层与 REST 接口
│   │   └── util/                JWT 签发校验、密码散列
│   └── src/main/resources/
│       ├── db/schema.sql        8 张表的建表脚本
│       ├── db/data.sql          与前端 mock 一致的种子数据
│       ├── mapper/*.xml         MyBatis SQL 映射
│       └── application.yml
│
├── docs/API.md                  RESTful 接口文档
└── deploy.sh                    一键部署到 GitHub Pages
```

---

## 数据库设计

| 表 | 说明 | 关键关系 |
| --- | --- | --- |
| `user` | 用户 | 被 playlist、comment、song_like 引用 |
| `singer` | 歌手 | 1 → N song |
| `song` | 歌曲 | N → 1 singer |
| `playlist` | 歌单 | `creator_id` 为 NULL 表示官方歌单 |
| `playlist_song` | 歌单歌曲关联 | N ↔ N，`sort_no` 记录自定义顺序 |
| `comment` | 歌曲评论 | N → 1 song / user |
| `user_favorite` | 收藏的歌单 | N ↔ N |
| `song_like` | 红心歌曲 | N ↔ N |

删除采用外键 `ON DELETE CASCADE`：删歌手连带删其歌曲，删歌曲连带清理
歌单关联、评论与红心，避免留下孤儿数据。

---

## 本地运行

### 前端（mock 模式，无需任何后端）

```bash
cd frontend
npm install
npm run dev
# 打开 http://localhost:5173
```

演示账号：`demo` / `123456`，管理员：`admin` / `admin123`。

### 前端（对接真实后端）

```bash
cd frontend
npm run dev -- --mode real   # /api 请求经 vite 代理转发到 localhost:8080
```

### 后端

需要 JDK 8+、Maven 3.6+、MySQL 8：

```bash
mysql -uroot -p -e "CREATE DATABASE yuetin DEFAULT CHARSET utf8mb4;"
cd backend
mvn spring-boot:run
```

数据库连接等参数走环境变量，默认值见 `application.yml`：

| 变量 | 默认值 |
| --- | --- |
| `MYSQL_HOST` / `MYSQL_PORT` | `localhost` / `3306` |
| `MYSQL_DB` | `yuetin` |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `root` / `root` |
| `JWT_SECRET` | `yuetin-music-secret-2024` |
| `CORS_ORIGINS` | `*` |

建表与种子数据在启动时自动执行（`schema.sql` 全部 `IF NOT EXISTS`，
`data.sql` 全部为固定主键的 `INSERT`，重复启动不会产生脏数据）。
服务启动后接口文档在 http://localhost:8080/api/swagger-ui.html 。

---

## 部署

```bash
./deploy.sh
```

脚本会构建前端、创建 `hsjy/yuetin-music` 项目仓库、把产物推到 `gh-pages`
分支并开启 Pages，最终地址为 <https://hsjy.github.io/yuetin-music/>。

> 脚本部署到**项目仓库子路径**，`hsjy.github.io` 根路径的个人主页不受影响。
> 需要先 `gh auth login` 并安装 GitHub CLI（`brew install gh`）。

GitHub Pages 是纯静态托管，跑不了 SpringBoot。因此线上默认以 **mock 模式**
运行——所有数据存在浏览器 localStorage 里，登录、歌单、收藏、评论都能真实操作。
若要连真实后端，把 `VITE_API_BASE` 指向线上 SpringBoot 地址并在服务端放开 CORS 即可。

---

## 实现要点

**JWT 校验的坑。** 客户端发的是 `Authorization: Bearer eyJ...`，服务端必须先
剥掉 `Bearer ` 前缀再验签，否则整个 header 会被当成 token，签名比对必然失败
（这个 bug 曾导致所有登录态页面被弹回登录页）。mock 层与真实后端
`JwtUtil` 对这一点的处理完全一致。

**Vue `<transition>` 在后台标签页会卡死。** Vue 移除 `enter-from` 类依赖
`requestAnimationFrame`，而非前台标签页里 rAF 会被节流甚至停摆。表现是遮罩层
带着 `opacity: 0` 和 `pointer-events: auto` 残留在 DOM 上——看不见，却拦住整页点击，
后续所有自动化点击都会超时，看起来像"事件处理器死了"。本项目的入场动画因此统一
改用纯 CSS `animation`，不依赖帧回调。

**相对 base + hash 路由。** 部署到 `/<repo>/` 子路径时，绝对路径 `base: '/'`
会让 JS/CSS 全部 404；`base: './'` 配合 hash 路由是静态托管下的标准解法，
`deploy.sh` 会在构建后校验这两项配置。

**密码存储。** `SHA-256(明文 + 固定盐)`，实体上用
`@JsonProperty(access = WRITE_ONLY)` 确保密码永不出现在响应里。
种子脚本为了可读性存明文，服务启动时由 `PasswordInitializer` 自动升级为散列。
课程项目级别的实现，生产环境应换成 BCrypt / SCrypt。
