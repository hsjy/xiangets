# 弦歌 · 本地运行手册（本机速查）

> 这是给本机用的启动速查，含这台机器的实际路径与端口。
> 公开说明文档见 README.md；部署教程见 deploy/DEPLOY.md（目前不上云，备用）。

## 平时只用这两条

### ① 完整体验（推荐）：一键起全栈

```bash
bash tools/dev_up.sh
# 打开 http://localhost:5173
```

一条命令起四个服务，**已在跑的自动跳过**，进程分离（关终端不退）：

| 端口 | 服务 | 说明 |
| --- | --- | --- |
| 5173 | 前端（real 模式） | 必须用 `localhost` 访问，`127.0.0.1` 会被拒（Vite 只绑 IPv6） |
| 8080 | SpringBoot 后端 | 自动带 DeepSeek key，问诗/对话/谱曲全可用 |
| 3307 | MySQL（项目私有） | 数据在 `backend/mysql-data/`，凭据 root/root，库名 yuetin |
| 8000 | AI 谱曲边车 | MusicGen 本地推理，Mac MPS |

日志都在 `/tmp/xiangge-*.log`（frontend / backend / mysql / sidecar）。

### ② 快速看前端（不开数据库）：Mock 模式

```bash
cd frontend && npm run dev -- --port 5174
# 打开 http://localhost:5174
```

纯浏览器运行：数据是内置 122 首精选快照，无需 MySQL/后端。显式给 `--port 5174` 是为了避开 ① 占用的 5173。AI 伴读照常可用：点顶部 ✦ 填自己的 DeepSeek key（只存本机）。

> 两者区别：① 连真实数据库和全部 34 万语料、有声、可谱曲；② 无声、语料仅精选层，适合快速改前端样式/验证 UI。

## 手动逐个启动（排查问题时用）

```bash
# MySQL（项目私有实例，不是系统 MySQL）
$(brew --prefix mysql)/bin/mysqld \
  --datadir=$PWD/backend/mysql-data \
  --socket=$PWD/backend/mysql-data/mysql.sock --port=3307 --mysqlx=OFF &

# 后端（必须 JDK 17，系统默认更高版本会导致 Lombok 编译失败）
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
AI_DEEPSEEK_KEY=$(cat ../docs/.deepseek_key) \
AI_ACESTEP_URL=http://127.0.0.1:8000/generate \
MYSQL_PORT=3307 \
mvn -o spring-boot:run

# 前端（real 模式；忘带 --mode real 会连不上后端）
cd frontend && npm run dev -- --mode real

# AI 谱曲边车（可选）
HF_ENDPOINT=https://hf-mirror.com python3 tools/ai_server.py
```

## 衍生命令

```bash
# 前端构建（产物 frontend/dist；忘带 --mode real 会构建成 mock 站）
cd frontend && npm run build -- --mode real

# 批量预生成 AI 曲（幂等，跳过已有的；本机约 1 分钟/首，需 ① 三件套在跑）
bash tools/pregenerate.sh

# 修改数据库内容后同步 mock 数据（必须在 tools/ 目录下运行）
cd tools && python3 export_poetry_to_mock.py

# 停服务（端口即进程）：按端口找 pid 再 kill
lsof -nP -iTCP:8080 -sTCP:LISTEN        # 找到 PID
kill <PID>                               # 同理 3307 / 8000 / 5173
```

## 内置账号

| 账号 | 密码 | 用途 |
| --- | --- | --- |
| demo | demo123456 | 普通用户 |
| admin | admin123 | 管理员（可进管理后台） |

## 踩坑速查

| 症状 | 原因与解法 |
| --- | --- |
| `127.0.0.1` 连接被拒 | Vite 只绑 IPv6，改用 `http://localhost:5173` |
| 后端编译报 Lombok 错误 | JDK 版本不是 17，`export JAVA_HOME=/opt/homebrew/opt/openjdk@17/...` |
| 前端报接口 404/网络错误 | 启动时忘带 `--mode real`（变成了 mock 站）或后端没起（看 `/tmp/xiangge-backend.log`） |
| 诗选只有 122 首 | 这是 mock 模式，正常；全量 34 万首需 real 模式 |
| mock 里点「听此曲」没反应 | mock 不含音频，属预期；real 模式有声 |
| 谱曲一直转圈 | 边车(8000)没起，或生成中（约 2~4 分钟/首） |
| Flyway 校验失败启动不了 | 改过已应用的迁移文件；新建迁移文件解决，勿改旧文件 |
