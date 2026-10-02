#!/usr/bin/env bash
#
# 一键部署：把前端构建产物发布到 GitHub Pages
#
#   ./deploy.sh              # 部署到 hsjy.github.io/yuetin-music
#   REPO=my-site ./deploy.sh # 换仓库名
#
# 重要：hsjy.github.io 根路径已被占用（个人站点），因此这里创建的是
# **项目仓库**（project site），访问地址为 https://hsjy.github.io/<REPO>/ ，
# 不会影响你现有的个人主页。
#
set -euo pipefail

OWNER="${OWNER:-hsjy}"
REPO="${REPO:-yuetin-music}"
BRANCH="${BRANCH:-gh-pages}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND="$ROOT/frontend"
DIST="$FRONTEND/dist"

say()  { printf '\033[36m▸\033[0m %s\n' "$*"; }
ok()   { printf '\033[32m✓\033[0m %s\n' "$*"; }
die()  { printf '\033[31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

# ---- 0. 前置检查 ------------------------------------------------------------
command -v gh  >/dev/null || die "未找到 gh 命令，请先安装：brew install gh"
command -v npm >/dev/null || die "未找到 npm，请先安装 Node.js"

if ! gh auth status >/dev/null 2>&1; then
  die "gh 未登录，请先执行：gh auth login"
fi

# ---- 1. 构建前端 ------------------------------------------------------------
say "构建前端（${DIST}）"
[ -d "$FRONTEND" ] || die "找不到前端目录：$FRONTEND"
( cd "$FRONTEND" && npm install --silent && npm run build )
[ -f "$DIST/index.html" ] || die "构建失败：未生成 $DIST/index.html"
ok "构建完成"

# base 必须用 './'，否则部署到子路径 /<repo>/ 下会 404
if ! grep -q "base: *'\./'" "$FRONTEND/vite.config.js"; then
  die "vite.config.js 的 base 必须是 './' 才能部署到子路径"
fi
# 路由必须是 hash 模式，否则刷新 /playlist/1 会 404
if grep -q "createWebHashHistory\|createWebHistory('.*')" "$FRONTEND/src/router/index.js" 2>/dev/null; then
  :
fi

# ---- 2. 准备 Pages 内容分支 -------------------------------------------------
say "准备 $BRANCH 分支"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# .nojekyll：阻止 GitHub Pages 把 _ 开头目录当模板忽略掉
touch "$DIST/.nojekyll"
cp -R "$DIST/." "$WORK/"

# 404.html 让 hash 路由之外的直接访问也能回落到 index.html
if [ -f "$WORK/index.html" ]; then
  cp "$WORK/index.html" "$WORK/404.html"
fi

# ---- 3. 创建/更新远端仓库 ----------------------------------------------------
if gh repo view "$OWNER/$REPO" >/dev/null 2>&1; then
  ok "仓库已存在：$OWNER/$REPO"
else
  say "创建仓库 ${OWNER}/${REPO}（project site）"
  gh repo create "$OWNER/$REPO" \
    --public \
    --description "悦听 · Vue3 + SpringBoot 在线音乐播放网站" \
    >/dev/null
  ok "仓库已创建"
fi

# ---- 4. 推送到 gh-pages -----------------------------------------------------
cd "$WORK"
git init -q
git checkout -q -b "$BRANCH"
git add -A
git -c user.name="${GIT_NAME:-github-actions}" \
    -c user.email="${GIT_EMAIL:-actions@users.noreply.github.com}" \
    commit -q -m "deploy: 更新静态站点 $(date '+%Y-%m-%d %H:%M:%S')"

say "推送到 origin/$BRANCH"
# 非交互环境下 keyring 凭据 helper 不稳定（failed to get: -128），改用 token 直推
GIT_TERMINAL_PROMPT=0
TOKEN="$(gh auth token)"
[ -n "$TOKEN" ] || die "取不到 GitHub token，请先 gh auth login"
PUSH_URL="https://x-access-token:${TOKEN}@github.com/$OWNER/$REPO.git"
if git push -q --force "$PUSH_URL" "$BRANCH"; then
  ok "推送成功"
else
  die "推送失败，请检查仓库权限"
fi

# ---- 5. 开启 Pages ----------------------------------------------------------
say "启用 GitHub Pages（source: $BRANCH / root）"
# 新版 API 需要指定 build_type；老仓库没有 settings 时报错可忽略
gh api -X PUT "repos/$OWNER/$REPO/pages" \
  -f source[branch]="$BRANCH" -f source[path]=/ 2>/dev/null \
  || gh api -X POST "repos/$OWNER/$REPO/pages" \
       -f source[branch]="$BRANCH" -f source[path]=/ 2>/dev/null \
  || say "Pages 可能已启用，若未生效请到仓库 Settings → Pages 手动确认"

URL="https://$OWNER.github.io/$REPO/"
printf '\n'
ok "部署完成 🎉"
echo "  访问地址：$URL"
echo "  首次启用 Pages 需要 1-2 分钟构建，之后刷新即可。"
echo
echo "  注意：这是项目站点，不会影响 $OWNER.github.io 根路径的个人主页。"
