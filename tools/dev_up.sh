#!/usr/bin/env bash
# ============================================================
# 本地一键起全栈：MySQL(3307) → 后端(8080) → 边车(8000) → 前端(5173)
#
# 关键点：每个服务用 python 的 start_new_session 分离进程组/会话，
# 脚本退出或终端会话结束后服务仍存活（普通 nohup 仍可能被进程组连带杀掉）。
#
# 用法：bash tools/dev_up.sh        # 已在跑的会跳过
# 日志：/tmp/xiangge-*.log
# ============================================================
set -uo pipefail
cd "$(dirname "$0")/.."
ROOT=$PWD

port_up() { lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }

# spawn <log> <cwd> <cmd...>：分离会话启动，立即返回
spawn() {
  local log=$1 cwd=$2; shift 2
  python3 - "$log" "$cwd" "$@" <<'PY'
import os, subprocess, sys
log, cwd, cmd = sys.argv[1], sys.argv[2], sys.argv[3:]
with open(log, 'ab') as f:
    p = subprocess.Popen(cmd, cwd=cwd, env=os.environ.copy(),
                         stdout=f, stderr=subprocess.STDOUT,
                         start_new_session=True)
print(f'  pid {p.pid} → {log}')
PY
}

# 1. MySQL（项目私有数据目录）
if port_up 3307; then
  echo "· MySQL 已在跑"
else
  echo "· 启动 MySQL…"
  spawn /tmp/xiangge-mysql.log "$ROOT" "$(brew --prefix mysql)/bin/mysqld" \
    --datadir="$ROOT/backend/mysql-data" \
    --socket="$ROOT/backend/mysql-data/mysql.sock" --port=3307 --mysqlx=OFF
  for _ in $(seq 1 30); do
    mysql -h127.0.0.1 -P3307 -uroot -proot -e "select 1" >/dev/null 2>&1 && break
    sleep 1
  done
fi

# 2. 后端（JDK17 + DeepSeek key + 边车地址）
if port_up 8080; then
  echo "· 后端已在跑"
else
  echo "· 启动后端…"
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
  export AI_DEEPSEEK_KEY="$(cat "$ROOT/docs/.deepseek_key")"
  export AI_ACESTEP_URL=http://127.0.0.1:8000/generate
  export MYSQL_PORT=3307
  spawn /tmp/xiangge-backend.log "$ROOT/backend" mvn -o spring-boot:run
fi

# 3. 谱曲边车
if port_up 8000; then
  echo "· 边车已在跑"
else
  echo "· 启动 ACE-Step 边车…"
  export HF_ENDPOINT=https://hf-mirror.com
  spawn /tmp/xiangge-sidecar.log "$ROOT" python3 tools/ai_server.py
fi

# 4. 前端（real 模式，必须带 --mode real）
if port_up 5173; then
  echo "· 前端已在跑"
else
  echo "· 启动前端…"
  spawn /tmp/xiangge-frontend.log "$ROOT/frontend" npm run dev -- --mode real
fi

# 等全部就绪
for _ in $(seq 1 60); do
  ready=1
  for p in 3307 8080 8000 5173; do port_up "$p" || ready=0; done
  (( ready == 1 )) && break
  sleep 2
done

echo "--- 状态 ---"
for p in 3307 8080 8000 5173; do
  printf '%s: ' "$p"; port_up "$p" && echo up || echo DOWN
done
