# 弦歌 · 诗乐雅集

> 诗为经，乐为纬 —— 一个古诗配乐欣赏网站。

[![在线演示](https://img.shields.io/badge/演示-hsjy.github.io%2Fxiangets-a5402d)](https://hsjy.github.io/xiangets/)
![技术栈](https://img.shields.io/badge/Vue3%20·%20SpringBoot%203%20·%20MySQL-8B0000)
![数据规模](https://img.shields.io/badge/诗词语料-34万首-556B2F)

**弦歌**把 34.3 万首古典诗词（先秦至清，11 个朝代、1.3 万位诗人）装进一个可以「听」的网站：精选诗配有 AI 谱写的中国器乐曲与朗诵，配竖排诗文、行旅地图、交游图谱与互动游戏。全部诗词原文来自公开语料库 [chinese-poetry](https://github.com/chinese-poetry/chinese-poetry)，背景古画为公有领域作品（CC0）。

## 在线演示

**https://hsjy.github.io/xiangets/**

演示站为浏览器内 Mock 模式：内置精选语料，可注册演示账号（数据保存在你自己的浏览器 localStorage 中），**AI 对话/谱曲/全量语料/多用户互动等完整功能需要本地或服务器部署**——见下文。

## 功能一览

- **意境页**：竖排诗行、注释/译文/赏析、名句专赏卡、同心境推荐、配乐朗诵与 AI 器乐曲播放
- **诗人页**：生平故事、可缩放行旅图（分期着色 + 生涯回放动画）、交游图谱、AI 扮演诗人本人对话（以库中生平资料为事实依据）
- **AI 能力**（接 [DeepSeek API](https://platform.deepseek.com/)）：问诗（带全诗上下文的多轮问答）、诗人角色扮演、语料层诗歌按需生成注译赏、读诗谱曲（MusicGen）
- **互动与游戏**：飞花令（34 万首随机出句）、诗游四游戏（拼句/交游/地理/接龙）、背诵挖空、感悟与社交（好友/关注/私信/积分称号）
- **选集**：9 大主题（四季/节令/山水/边塞……），支持连播
- **基础设施**：JWT 登录、管理后台、夜读模式、PWA（可安装、离线缓存）、Chrome 新标签页插件

## 快速开始（本地运行）

**环境要求**：JDK 17 · Node 18+ · MySQL 8 · 可选 Python 3（AI 谱曲边车）

```bash
# 一键起全栈（MySQL + 后端 + 前端，已在跑的自动跳过）
bash tools/dev_up.sh
# 打开 http://localhost:5173 （注意：必须用 localhost，前端仅绑定 IPv6）
```

手动逐个启动：

```bash
# 后端（DeepSeek key 放在 docs/.deepseek_key，无 key 时 AI 对话功能降级，其余正常）
cd backend
export JAVA_HOME=/path/to/jdk-17   # 必须 17，更高版本 Lombok 编译失败
AI_DEEPSEEK_KEY=sk-xxx \
AI_ACESTEP_URL=http://127.0.0.1:8000/generate \
MYSQL_PORT=3307 \
mvn spring-boot:run

# 前端（--mode real 连真实后端；不带该参数则为 Mock 演示模式）
cd frontend && npm install && npm run dev -- --mode real
```

内置账号：`demo / demo123456`（普通用户）、`admin / admin123`（管理员，可进管理后台）。数据库由 Flyway 自动建表并灌入精选层种子数据；34 万首语料层可用 `tools/import_corpus.py` 导入（见脚本头注释）。后端接口文档：http://localhost:8080/api/swagger-ui.html

## 部署上线

部署材料在 [`deploy/`](deploy/) 目录，**完整教程（含每一步的原理讲解与踩坑清单）见 [deploy/DEPLOY.md](deploy/DEPLOY.md)**。最简路径：

```bash
cd deploy
cp .env.example .env
# 编辑 .env：数据库强密码、JWT_SECRET（openssl rand -base64 48）、DEEPSEEK_API_KEY
docker compose up -d --build
curl -s http://127.0.0.1/api/poem/today | head -c 200   # 探活
```

三个容器（nginx / backend / mysql）：nginx 直出前端静态文件与音频、反代 `/api`；MySQL 仅容器内网可达。数据迁移推荐从本地 `mysqldump` 导入（教程第 6 节）。

> **音频说明**：朗诵与 AI 曲目的 MP3 不进 git（见 `.gitignore`），服务器部署时需把本地 `backend/audio/` 同步到服务器同路径后再 `docker compose build`：`rsync -avz backend/audio/ root@服务器:/opt/xiangets/backend/audio/`。

**HTTPS**：有域名后用 certbot 签发证书放入 `deploy/certs/`，按 `nginx.conf` 内注释解开 443 配置即可。国内服务器需 ICP 备案，海外/香港节点免备案。

## GitHub Pages 演示与发版

推 `v*` 标签即自动触发 [release.yml](.github/workflows/release.yml)：

- **Pages 演示站**：前端以 Mock 模式构建并发布到仓库子路径（`https://你的用户名.github.io/仓库名/`）
- **浏览器插件**：自动打包 zip 并创建 GitHub Release
- （Android APK / 桌面客户端的构建步骤已预留注释，启用 Capacitor/Tauri 后打开即可）

```bash
git tag v1.0.0 && git push origin v1.0.0
```

## 目录结构

```
frontend/          Vue3 + Vite + Pinia SPA（hash 路由，mock/real 双模式，PWA）
backend/           SpringBoot 3.3.4 + MyBatis + Flyway（V1~V29 迁移）
  audio/           朗诵 MP3 + AI 生成曲（不进 git，脚本重建）
deploy/            Docker Compose + nginx + 部署教程 DEPLOY.md
tools/             语料导入 / TTS 朗诵 / AI 曲预生成 / 行旅地图生成等脚本
extension/         Chrome 插件（新标签页今日一诗 + 右键搜索）
docs/              API 文档；.deepseek_key（已 gitignore）
.github/workflows/ 推 tag 自动构建发布
```

## AI 功能的上线策略

| 能力 | 本地 | 线上建议 |
| --- | --- | --- |
| 问诗 / 诗人对话 / 按需注译赏 | DeepSeek API | 直接可用，个人用量每月几元 |
| 朗诵 TTS | edge-tts 实时生成 | 预生成（`tools/build_recite.py`） |
| AI 谱曲 | MusicGen 本地推理（Mac MPS 约 2~4 分钟/首） | 预生成（`tools/pregenerate.sh`），云 CPU 不适合实时 |

## 许可

诗词原文为公有领域；背景古画为公有领域（CC0）。代码与编撰文案版权所有 © 2026，暂未采用开源许可证——如需引用请先开 issue 联系。
