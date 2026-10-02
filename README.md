# 弦歌 · 诗乐雅集

> 原型为 2024.01~2024.05 课程项目「在线音乐播放网站」，现已重构为
> **以古诗为主体的欣赏网站**：古诗、诗人、诗人故事为路线，
> 音乐（配乐朗诵 / 纯音乐 / AI 谱曲）退为佐诗之茶。

**在线体验：** https://hsjy.github.io/yuetin-music/ （演示账号 `demo` / `123456`）

---

## 这是什么

一座古风的诗乐小站：

- **今日一诗**——按日期轮换的名句签长卷，今日属《春夜喜雨》，明日轮到他篇
- **诗选**——按朝代（魏晋/唐/宋）与心境（月夜/田园/送别/壮志…12 种意象）浏览 24 首名篇
- **诗人**——八位诗人各有一篇叙事故事：陶渊明不为五斗米折腰、李白醉中脱靴、
  苏轼乌台诗案后的黄州岁月、李清照赌书泼茶、辛弃疾五十骑夜擒叛将……
- **意境页（核心）**——一首诗一页：竖排诗行、注/译/赏、同心境推荐，
  以及三种声音：**配乐朗诵**（TTS 生成）、**古筝铺底**（程序化合成）、
  **AI 谱曲**（点一下按钮，按诗的意象生成专属曲）
- **书斋**——你收藏的诗，与你写下的感悟

技术栈：Vue 3 + Vite + Pinia + Axios ／ Spring Boot 3.3 + MyBatis + Flyway ／ MySQL 8。

---

## 项目亮点

**1. 一套代码，两种运行模式。** 前端通过自定义 Axios 适配器在浏览器内实现了
完整的 RESTful mock 后端（路由、状态码、JWT 鉴权与真实服务逐条对齐），
纯静态的 GitHub Pages 上登录、收藏、写感悟全都可用；
`npm run dev -- --mode real` 一键切换到真实 SpringBoot + MySQL，前端代码零改动。

**2. 音频资产 100% 程序化生成，零版权素材。**
- 朗诵：edge-tts（男声"云溪"诵 22 首，女声"晓晓"专诵易安词）
- 配乐：自研 **Karplus-Strong 拨弦物理建模合成器**，五声音阶按情绪配方生成
  六段古筝风铺底与 24 首诗的专属曲（`tools/synth_bgm.py`、`tools/synth_poem_ai.py`）

**3. AI 谱曲任务流（古诗转音乐）。** 意境页「✦ 为此诗谱曲」：
情绪→提示词映射（如"月夜"→ guzheng + xiao flute, slow, moonlit night），
诗行原文参与提示词，异步任务 + 轮询。提供商可插拔：
- `python3 tools/ai_server.py` 启动本地 **MusicGen** 推理边车（Apple Silicon
  MPS 实测 30s 曲目约 40~60s 生成），后端配 `AI_ACESTEP_URL` 即接入真实生成；
- 不配置或生成失败时自动回落预生成曲，演示永不翻车。

**4. 现代化后端。** Spring Boot 3.3 / JDK 17，Flyway 版本化迁移（V1~V8），
统一响应封装、全局异常、JWT（HS256）、SHA-256 加盐口令、外键级联设计。

---

## 目录结构

```
music/
├── frontend/                    Vue3 前端
│   ├── src/
│   │   ├── api/                 接口封装 + Axios 实例与 mock 适配器
│   │   ├── mock/                浏览器内 mock 后端（poetry_data.js 由脚本从 MySQL 导出）
│   │   ├── components/          PlayerBar、UserAvatar（印章头像）等
│   │   ├── views/               今日/诗选/意境/诗人/诗人页/书斋/登录
│   │   ├── stores/              player / user / ui
│   │   └── assets/styles/       古风设计 token（宣纸/松绿/朱砂/泥金）
│   └── index.html               霞鹜文楷 + 马善政楷书 CDN
│
├── backend/                     SpringBoot 3.3 后端
│   ├── src/main/java/com/yuetin/music/
│   │   ├── common/  config/     统一响应、CORS、JWT 拦截器、Flyway
│   │   ├── entity/  mapper/     12 张表的实体与 MyBatis 映射
│   │   ├── service/ controller/ 诗选/诗人/AI 谱曲/音轨/用户 等业务
│   │   └── util/                JWT、口令散列
│   ├── src/main/resources/
│   │   ├── db/migration/        Flyway V1~V8（建表/种子/诗/音轨/AI）
│   │   ├── mapper/*.xml
│   │   └── application.yml
│   └── audio/                   音频落盘目录（recite/bgm/generated，脚本生成不入 git）
│
├── tools/                       内容生产管线（Python）
│   ├── synth_bgm.py             古筝风配乐合成器（Karplus-Strong）
│   ├── build_recite.py          edge-tts 朗诵批量生成
│   ├── synth_poem_ai.py         每诗专属 AI 风格曲预生成
│   ├── ai_server.py             MusicGen 推理边车（FastAPI + MPS）
│   └── export_poetry_to_mock.py MySQL → mock 数据同步
│
├── docs/                        古风改造方案 + 接口文档
└── deploy.sh                    一键部署到 GitHub Pages
```

---

## 数据库设计（Flyway V1~V8）

| 表 | 说明 |
| --- | --- |
| `user` / `poet` / `poem` | 账号；诗人（字/号/生平/故事）；诗（诗行/注/译/赏/情绪/名句） |
| `singer` / `song` | 音轨演绎者（现为"数字乐府"朗诵者/合成器）；音轨挂 `poem_id` + `track_kind`（recitation/bgm/ai） |
| `playlist` / `playlist_song` | 选集（旧歌单机制，待转型诗集） |
| `poem_favorite` / `song_like` / `comment`(poem_id) | 收藏的诗 / 红心 / 感悟 |

删除级联由外键保证：删诗人连带诗、音轨、感悟、收藏一并清理。

---

## 本地运行

### 前端（mock 模式，无需后端）

```bash
cd frontend && npm install && npm run dev   # http://localhost:5173
```

### 全栈（推荐，意境页音频完整体验）

需要 JDK 17、Maven、MySQL 8（`brew install openjdk@17 maven mysql`）：

```bash
# 1. 项目内私有 MySQL（数据落在 backend/mysql-data，端口 3307）
mkdir -p backend/mysql-data
$(brew --prefix mysql)/bin/mysqld --initialize-insecure \
  --datadir=$PWD/backend/mysql-data
$(brew --prefix mysql)/bin/mysqld \
  --datadir=$PWD/backend/mysql-data \
  --socket=$PWD/backend/mysql-data/mysql.sock --port=3307 --mysqlx=OFF &

# 2. 建库（首次）
$(brew --prefix mysql)/bin/mysql --socket=$PWD/backend/mysql-data/mysql.sock -uroot \
  -e "CREATE DATABASE IF NOT EXISTS yuetin DEFAULT CHARSET utf8mb4; ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';"

# 3. 生成音频资产（朗诵 + 配乐，约几分钟）
pip3 install edge-tts numpy lameenc
python3 tools/build_recite.py
python3 tools/synth_bgm.py && python3 tools/synth_poem_ai.py

# 4. 起后端（Flyway 自动建表 + 种子；私有库走 3307）
cd backend && MYSQL_PORT=3307 mvn spring-boot:run

# 5. 前端 real 模式
cd ../frontend && npm run dev -- --mode real
```

后端 http://localhost:8080/api （Swagger：/api/swagger-ui.html）。
数据库连接/JWT/CORS/ACE-Step 均可用环境变量覆盖（见 application.yml）。

### AI 谱曲切换真实生成

自部署 ACE-Step 推理服务后：

```bash
AI_ACESTEP_URL=http://your-gpu-host:8000/generate mvn spring-boot:run
```

服务约定：`POST {tags, lyrics, durationSec}` → 返回 mp3 二进制（其他返回形态在
`AiMusicService#callAceStep` 适配）。未配置时自动使用预生成曲兜底。

---

## 部署

```bash
./deploy.sh   # 构建前端 → hsjy/yuetin-music 项目仓库 gh-pages 分支 → 开启 Pages
```

线上为纯静态 mock 模式：诗、诗人、故事、收藏、感悟全部可用；
朗诵音频需本地/服务器 real 模式播放。

---

## 实现要点（踩坑记录）

**JWT 校验前先剥 `Bearer ` 前缀**，否则整个 header 被当作 token，验签必然失败。

**Vue `<transition>` 在后台标签页会卡死**：移除 enter-from 类依赖 rAF，
非前台标签页 rAF 停摆会让遮罩带着 `opacity:0; pointer-events:auto` 残留——
看不见却拦截整页点击。全站入场动画改用纯 CSS `animation`。

**相对 base + hash 路由**是静态托管子路径部署的标准解法（`base: './'`）。

**`INSERT IGNORE` + 固定主键**让种子脚本幂等；Flyway 接管后由版本化迁移保证。

**口令存储**：SHA-256(明文+盐)，实体 `@JsonProperty(WRITE_ONLY)` 确保响应不带密码；
种子脚本存明文，启动时 `PasswordInitializer` 自动升级为散列。

**Bash 里 `$VAR（全角括号）`** 在 C locale 下会把多字节字符吞进变量名——
`deploy.sh` 曾因此报 unbound variable，`${DIST}` 显式定界是正解。
