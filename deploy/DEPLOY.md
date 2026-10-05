# 弦歌 · 部署手册（DEPLOY.md）

> 这份手册既是操作指南也是教程：每一步都解释**为什么**。
> 目标形态：一台服务器上跑三个容器（nginx / backend / mysql），
> 浏览器访问域名即得完整网站——与本地预演用的是同一套文件。

---

## 0. 五分钟看懂架构

```
                    ┌──────────────── 服务器（一台 Linux）────────────────┐
浏览器 ──HTTPS──▶  │  nginx(80/443)                                     │
                    │   ├── /            → 前端静态文件（构建产物）          │
                    │   ├── /api/audio/  → 音频文件直出（长缓存）            │
                    │   └── /api/...     ──反代──▶  backend(SpringBoot:8080)│
                    │                                └── mysql(容器内网)    │
                    └────────────────────────────────────────────────────┘
外部依赖：DeepSeek API（问诗/读诗谱曲，按量付费，几元/月）
```

**为什么这么分？**

- **nginx 做入口**：静态文件它最擅长；`/api` 反代给后端，前后端同域，天然无跨域问题。
- **音频由 nginx 直出**：MP3 是不变的静态文件，让 nginx 直接读磁盘并带一年缓存，比走 Java 快且省资源。
- **数据库不暴露端口**：MySQL 只在容器内网被 backend 访问，公网扫不到。
- **三个容器各自独立升级**：改前端不用动数据库，换数据库配置不用重发前端。

**核心概念速查**（只列本项目用到的）：

| 概念 | 一句话 | 在本项目的体现 |
| --- | --- | --- |
| 镜像 image | 打包好的运行环境快照 | `Dockerfile.backend` 编译出的 jar 运行环境 |
| 容器 container | 镜像的运行实例 | `docker compose ps` 里的三行 |
| 卷 volume | 容器删除后仍保留的数据 | MySQL 数据、音频文件 |
| 反向代理 reverse proxy | 统一入口，按路径转发到内部服务 | nginx 把 `/api` 转给 backend |
| 健康检查 healthcheck | 编排器探测服务是否就绪 | mysql 就绪后 backend 才启动 |

---

## 1. 本地预演（强烈建议先做）

不用买服务器，装个 Docker Desktop 就能在本机完整演练一遍，与线上一模一样。

```bash
# 安装 Docker Desktop（Mac: 官网下载 / brew install --cask docker-desktop）
cd deploy
cp .env.example .env
# 编辑 .env：设置 MYSQL_ROOT_PASSWORD，生成 JWT_SECRET：
#   openssl rand -base64 48
# 填入 DEEPSEEK_API_KEY（不填则问诗不可用，其余功能正常）

docker compose up -d --build        # 首次构建约 5~10 分钟
docker compose ps                    # 三个容器应为 running/healthy
open http://localhost                # 完整网站
```

> 首次启动数据库是空的？不会——后端的 Flyway 会自动执行 `V1~V20` 全部迁移：
> 建表、精选层种子一次到位。语料层（34 万首）见「第 6 步 数据」。

常用排查命令：

```bash
docker compose logs -f backend       # 看后端日志（最常用）
docker compose logs nginx
docker compose down                  # 停止（卷保留）
docker compose down -v               # ⚠️ 连数据一起删（重来用）
docker compose up -d --build         # 改代码后重建
```

---

## 2. 买服务器与域名

1. **选服务器**：阿里云/腾讯云「轻量应用服务器」2C4G 起步（¥60~150/月）。
   - 国内节点：需 ICP 备案（个人 2~4 周，免费）；
   - 香港/海外节点：免备案，当天可用，延迟略高。
2. **买域名**并解析一条 A 记录指向服务器公网 IP。
3. 系统镜像选 **Ubuntu 22.04/24.04**。

---

## 3. 服务器初始化

```bash
ssh root@你的服务器IP

# 装 Docker（官方一键脚本）
curl -fsSL https://get.docker.com | sh

# 国内服务器建议同时配置镜像加速（控制台有现成地址）
# 验证
docker --version && docker compose version
```

---

## 4. 代码上服务器

```bash
# 方式一：git（推荐，升级也靠它）
apt install -y git && git clone https://github.com/你的用户名/你的仓库.git /opt/xiangets
cd /opt/xiangets/deploy

# 方式二：本地直传（含音频与语料时更省事）
# rsync -avz --exclude node_modules --exclude .git ./ root@服务器IP:/opt/xiangets/
```

> 注意：仓库里不含 `deploy/.env`（已被 gitignore），服务器上要按第 1 节手工创建。

---

## 5. 配置与启动

```bash
cd /opt/xiangets/deploy
cp .env.example .env
vim .env        # 设置：数据库强密码、JWT_SECRET（openssl rand -base64 48）、DEEPSEEK_API_KEY

docker compose up -d --build
docker compose ps        # 三个容器 healthy
curl -s http://127.0.0.1/api/poem/today | head -c 200   # 探活
```

**HTTPS**（有域名后）：

```bash
apt install -y certbot && certbot certonly --standalone -d 你的域名
# 证书在 /etc/letsencrypt/live/域名/ 下，拷贝到 deploy/certs/：
#   fullchain.pem privkey.pem
# 然后按 nginx.conf 内注释解开 443 配置，重启：
docker compose up -d web
```

---

## 6. 数据：两种方式

**方式 A（推荐）：从本地导出整套数据**——保留你在本地调好的收藏/感悟等状态：

```bash
# 本地执行（项目根目录）
mysqldump -h127.0.0.1 -P3307 -uroot -proot --single-transaction \
  --set-gtid-purged=OFF yuetin > /tmp/yuetin.sql

# 传到服务器并导入
scp /tmp/yuetin.sql root@服务器:/tmp/
ssh root@服务器 "docker compose -f /opt/xiangets/deploy/docker-compose.yml exec -T mysql \
  mysql -uroot -p\$MYSQL_ROOT_PASSWORD yuetin < /tmp/yuetin.sql"
```

**方式 B：生产从零重放**——Flyway 自动建表+精选层种子；语料层再跑一次导入：

```bash
# 服务器上（需先拿到 /tmp/corpus2 语料，见 tools/import_corpus.py 头注释）
python3 tools/import_corpus.py
python3 tools/backfill_bios.py
```

音频：`backend/audio/` 已随镜像打包；预生成的 AI 曲用 `tools/pregenerate.sh` 在本地跑完后随 `docker compose build` 带上去。

---

## 7. AI 能力的上线策略（重要取舍）

| 能力 | 本地 | 线上策略 |
| --- | --- | --- |
| 问诗（DeepSeek） | 可用 | **直接可用**（服务器访问 api.deepseek.com，按量计费，个人用量每月几元） |
| 朗诵 TTS | edge-tts | **预生成**（已含 121 条；新诗跑 `tools/build_recite.py`） |
| AI 谱曲（MusicGen） | MPS 本地 2~4 分钟/首 | **预生成**（`FORCE=0 bash tools/pregenerate.sh`，云 CPU 不适合实时）；想实时要么租 GPU 服务器（¥1500+/月），要么接付费 API |

---

## 8. 日常运维

```bash
# 备份（crontab 每日 03:00，保留 7 天）
0 3 * * * cd /opt/xiangets/deploy && docker compose exec -T mysql \
  mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" yuetin | gzip > /opt/backups/yuetin-$(date +\%F).sql.gz
# 别忘了在 .env 里 export 出 MYSQL_ROOT_PASSWORD 或在 cron 环境里 source 它

# 升级：拉新代码重建（数据在卷里，不丢）
cd /opt/xiangets && git pull && cd deploy && docker compose up -d --build

# 看资源
docker stats
```

---

## 9. 常见坑清单（都是真会踩的）

| 坑 | 症状 | 解法 |
| --- | --- | --- |
| 前端构建忘带 `--mode real` | 线上变成 mock 演示站 | Dockerfile 已写死 `-- --mode real`，别删 |
| JWT_SECRET 用默认值 | 任何人可伪造登录 | `.env` 必须覆盖，openssl 生成 |
| 80/443 被占 | nginx 起不来 | `lsof -i:80` 查占用（常见是宿主机自带 apache） |
| MySQL 时区不对 | 时间差 8 小时 | compose 里已设 `TZ=Asia/Shanghai` |
| 拉镜像慢/失败 | build 卡住 | 配 Docker 镜像加速器；Maven 已配阿里云镜像 |
| 语料层没数据 | 诗选只有 121 首 | 跑 `tools/import_corpus.py`（需 /tmp/corpus2） |
| 谱曲按钮转圈 | 线上无 MusicGen | 属预期：预生成策略，见第 7 节 |
| 备案未完成被阻断 | 国内服务器 80/443 不通 | 用香港节点过渡，或等备案下发 |

---

## 10. 上线检查清单

- [ ] `.env` 全部填写，JWT_SECRET 非默认值
- [ ] `docker compose ps` 三容器 healthy
- [ ] `curl /api/poem/today` 返回 200
- [ ] 浏览器过一遍：首页 / 诗选翻页 / 意境页播放 / 问诗 / 飞花令 / 登录
- [ ] 备份 cron 已生效（`ls /opt/backups`）
- [ ] HTTPS 证书生效，http 自动跳转
- [ ] 服务器防火墙只开 22/80/443
