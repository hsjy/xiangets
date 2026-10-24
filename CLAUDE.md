# 弦歌 · 诗乐雅集 — 项目交接文档

> 最后更新：2026-10-08 · 本文档供新对话快速了解项目全貌并继续开发

## 一句话

古诗配乐欣赏网站——Vue3 SPA + SpringBoot + MySQL，34.3 万首诗（11 个朝代），本地打磨中，未上线。

## 快速启动

```bash
# 一键起全栈（MySQL+后端+边车+前端，已在跑的自动跳过）
bash tools/dev_up.sh
# 日志：/tmp/xiangge-*.log；进程用 start_new_session 分离，会话结束不退出

# 手动逐个启动：
# 后端（必须带 DeepSeek key，否则问诗/谱曲降级；key 在仓库根 docs/.deepseek_key）
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
AI_DEEPSEEK_KEY=$(cat ../docs/.deepseek_key) \
AI_ACESTEP_URL=http://127.0.0.1:8000/generate \
MYSQL_PORT=3307 \
mvn -o spring-boot:run

# 前端（real 模式连真实后端）
cd frontend && npm run dev -- --mode real

# MusicGen 边车（AI 谱曲用，可选）
HF_ENDPOINT=https://hf-mirror.com python3 tools/ai_server.py
```

**访问**：http://localhost:5173 —— 必须用 `localhost`，Vite 只绑 IPv6（`::1`），用 `127.0.0.1` 会连接被拒。

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| Vite 前端 | 5173 | 开发模式（热更新） |
| SpringBoot 后端 | 8080 | API + 音频静态服务 |
| MySQL | 3307 | 数据在 `backend/mysql-data/`（不是系统 MySQL） |
| MusicGen 边车 | 8000 | AI 谱曲（Mac MPS，可选） |

**MySQL 凭据**：root/root，库名 yuetin，端口 3307（不是默认 3306）
**构建 JDK**：必须用 JDK 17（`/opt/homebrew/opt/openjdk@17`），系统默认可能更高版本导致 Lombok 报错
**演示账号**：demo / demo123456（普通用户），admin / admin123（管理员）

## 架构

```
frontend/          Vue3 + Vite + Pinia（SPA，hash 路由）
  src/views/       15 个页面组件
  src/components/  12 个公共组件
  src/mock/        mock 模式（无后端时用浏览器 localStorage 模拟）
  src/assets/paintings/  5 幅公有领域古画（CC0，背景纹理）
extension/         Chrome 浏览器插件（Manifest V3，新标签页今日一诗）
backend/           SpringBoot 3.3.4 + MyBatis + Flyway（29 个迁移 V1~V29）
  src/main/resources/db/migration/  数据库版本化迁移（改表必须新建迁移文件）
  audio/           朗诵 MP3 + AI 生成曲（随镜像分发）
deploy/            Docker Compose + nginx + .env 模板 + DEPLOY.md
tools/             Python 工具脚本（导入语料/TTS/合成/预生成/古画背景生成）
.github/workflows/ GitHub Actions（推 tag 自动构建发布）
extension/         浏览器插件（新标签页今日一诗 + 右键搜索）
docs/.deepseek_key DeepSeek API Key（已 gitignore）
```

## 数据规模

- **诗**：343,653 首（精选层 tier=1 共 121 首，带注/译/赏/意境/名句/名句专赏；语料层 tier=2 共 343,532 首，仅全文）
- **诗人**：13,718 位（精选层 62 位带完整生平/故事/行旅图；语料层 13,656 位带小传）
- **音轨**：269 条（朗诵 121 + AI 曲 126——精选层 122 首全覆盖 + 铺底 22）
- **选集**：9 大主题，121 首全覆盖
- **诗人关系**：37 组（师承/挚友/交游/并称/对手/文脉等）

## 已实现功能清单

### 内容与浏览
- 诗选页：服务端分页（34 万首）、朝代/心境筛选、ngram 全文搜索、tier 过滤
- 意境页：竖排诗行、注/译/赏（分条换列）、名句专赏卡、同心境推荐
- 语料层按需注译赏：意境页「调用 AI 生成注译赏」按钮 → DeepSeek 生成 → 缓存入库 → tier 升精选
- 诗人页：生平故事、行旅图（可缩放、朝代区划层、生涯回放动画、**分期着色**——路线/落脚点/序号按停留时段由青绿渐变朱砂，附岁月图例，支持公元前年份）、交游图谱（连线可点看故事）、诗人对话 AI（角色扮演）
- 主题选集：9 大主题 121 首全覆盖，连播功能

### AI 功能
- AI 谱曲：DeepSeek 读诗 → MusicGen 生成 30s 器乐曲（Mac MPS）
- 朗诵：edge-tts 生成配乐朗诵
- 问诗：DeepSeek 多轮问答（带全诗上下文）
- 诗人对话：AI 角色扮演诗人本人（第一人称，system prompt 注入库中生平资料——简介/故事/行旅足迹，语料层短小传同样受益）
- 语料层注译赏：按需生成并缓存

### 互动与游戏
- 飞花令：令字选择、34 万首随机出句、60s 倒计时、帮填 3 次、积分 +10/句
- 诗游：拼句（名句字序打乱）、交游（猜关系）、地理（猜朝代）、接龙（以字接句）
- 背诵模式：名句挖空填字
- 感悟：发/删/回复/点赞/热度排序
- 好友：搜索加友、同意/拒绝、关注/取关、私信聊天
- 积分：飞花令对句加分、称号升级（5 级）、管理后台调整

### 基础设施
- 登录：JWT、记住我（30 天）、防爆破锁定（5 次锁 10 分钟）、密保找回密码
- 管理后台：用户管理（角色/重置密码/解锁/注销）、感悟管理、好友管理
- PWA：可安装、离线缓存、Service Worker
- 浏览器插件：新标签页今日一诗 + popup 搜索 + 右键菜单
- GitHub Actions：推 tag 自动构建发布
- 夜读模式：全套暗色 token
- 播放器：浮动曲签、闲置虚化、队列、AI 曲管理（重命名/删除）

## 关键技术决策与教训

1. **Flyway 迁移**：改表结构必须新建迁移文件，已应用的迁移文件不可修改（否则校验和不匹配启动失败）
2. **MyBatis 别名**：实体类不能叫 `Collection`（与 java.util.Collection 冲突）
3. **Lombok + JDK**：必须用 JDK 17 编译，系统默认 JDK 27 会导致 Lombok 注解处理器失败
4. **前端构建**：`npm run build -- --mode real`，忘带 `--mode real` 会构建出 mock 演示站
5. **Vue 模板编辑**：批量 Python 脚本编辑 .vue 文件容易破坏 HTML 结构，每次改完必须验证编译
6. **删已应用的迁移行**：如果迁移是幂等的（UPDATE/幂等 INSERT），可删 history 行让 Flyway 重放
7. **macOS bash 3.2**：`$id《` 会把全角字符吞进变量名报 unbound variable，紧邻全角字符的变量必须写成 `${id}《`（tools/pregenerate.sh 已踩坑）
8. **AI 谱曲批量**：`bash tools/pregenerate.sh` 幂等可重跑（跳过已有 AI 曲），本机约 1 分钟/首；前置 = MySQL + 后端(带 AI_ACESTEP_URL) + 边车三件套在跑

## 文件修改注意事项

- `tools/export_poetry_to_mock.py`：修改数据后必须重新运行以同步 mock
- `tools/gen_china_outline.py` / `gen_dynasty_map.py`：地理底图生成（需要网络）
- 意境页 PoemDetailView.vue 是最大的文件，改动时注意 Vue 模板闭合

## 未实现 / 可继续做的

- 精选层扩充流水线（筛选→生成→入库，把 121 首扩到几百首，路线图仅剩此项）
- 朗诵对照录音（MediaRecorder API）
- 诗人年表可视化（横向时间轴）
- 诗词填空挑战（比背诵更游戏化）
- 更多选集主题
- Tauri 桌面客户端 / Android APK（workflow 已预留注释）
- 上线部署（deploy/ 目录材料齐全，等指令）
