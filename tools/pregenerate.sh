#!/usr/bin/env bash
# ============================================================
# 弦歌 · 精选层预生成脚本
#
# 上线策略：MusicGen 实时谱曲不适合云 CPU（每首 5~10 分钟），
# 做法是——部署前在本地把精选层全部诗的 AI 曲跑出来，
# 音频随镜像/卷发布，线上只提供播放。
#
# 前置（都在本机）：
#   1. MySQL(3307) 与 SpringBoot(8080) 在跑
#   2. MusicGen 边车在跑：HF_ENDPOINT=https://hf-mirror.com python3 tools/ai_server.py
#   3. 后端启动时带 AI_ACESTEP_URL=http://127.0.0.1:8000/generate
#
# 用法：bash tools/pregenerate.sh            # 跳过已有 AI 曲的
#       FORCE=1 bash tools/pregenerate.sh    # 全部重谱（会逐首排队）
# ============================================================
set -uo pipefail

API="${API:-http://127.0.0.1:8080/api}"
GAP="${GAP:-10}"          # 每首之间的冷却秒数（MPS/GPU 排队缓冲）
TIER1_IDS=()

say()  { printf '\033[36m▸\033[0m %s\n' "$*"; }
ok()   { printf '\033[32m✓\033[0m %s\n' "$*"; }
warn() { printf '\033[33m!\033[0m %s\n' "$*"; }
die()  { printf '\033[31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

# ---- 0. 探活 ----
curl -sf -m 5 "$API/poem/today" >/dev/null || die "后端未就绪：请先启动 SpringBoot(8080)"
say "后端已就绪，拉取精选层诗单…"

# ---- 1. 拉取精选层全部诗 id（tier=1 分页拉全） ----
page=1
while :; do
  json=$(curl -sf -m 15 "$API/poem/page?tier=1&page=$page&size=100") || die "拉取诗单失败"
  ids=$(echo "$json" | python3 -c "
import json,sys
d=json.load(sys.stdin)['data']
print(' '.join(str(p['id']) for p in d['list']))
print(d['total'], d['page'], file=sys.stderr)")
  TIER1_IDS+=($ids)
  total=$(echo "$json" | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['total'])")
  (( page * 100 >= total )) && break
  ((page++))
done
say "精选层共 ${#TIER1_IDS[@]} 首"

# ---- 2. 逐首：已有 AI 曲则跳过，否则建单并轮询 ----
DONE=0; SKIP=0; FAIL=0; FAILED_IDS=()
for id in "${TIER1_IDS[@]}"; do
  title=$(curl -sf -m 10 "$API/poem/detail/$id" | python3 -c "
import json,sys
d=json.load(sys.stdin)['data']
print(d['title'] + ('·'+d['subtitle'] if d.get('subtitle') else ''))" 2>/dev/null || echo "诗$id")

  has_ai=$(curl -sf -m 10 "$API/song/list?poemId=$id" | python3 -c "
import json,sys
d=json.load(sys.stdin)['data']
print('yes' if any(t.get('trackKind')=='ai' for t in d) else 'no')" 2>/dev/null || echo no)

  if [[ "$has_ai" == "yes" && "${FORCE:-0}" != "1" ]]; then
    ((SKIP++)); continue
  fi

  say "谱曲：#$id《$title》"
  job=$(curl -sf -m 30 -X POST "$API/ai/music/generate" -H 'Content-Type: application/json' -d "{\"poemId\":$id}" | python3 -c "
import json,sys
print(json.load(sys.stdin)['data']['id'])" 2>/dev/null) || { warn "建单失败 #$id"; ((FAIL++)); FAILED_IDS+=("$id"); continue; }

  # 轮询任务（MusicGen 每首 2~4 分钟，上限 10 分钟）
  for _ in $(seq 1 60); do
    sleep 10
    st=$(curl -sf -m 10 "$API/ai/music/job/$job" | python3 -c "
import json,sys
d=json.load(sys.stdin)['data']
print(d['status'])" 2>/dev/null || echo unknown)
    [[ "$st" == "done" ]] && { ok "#$id《$title》完成"; ((DONE++)); break; }
    [[ "$st" == "failed" ]] && { warn "#$id《$title》失败"; ((FAIL++)); FAILED_IDS+=("$id"); break; }
  done
  sleep "$GAP"
done

echo
ok "汇总：完成 $DONE · 跳过(已有) $SKIP · 失败 $FAIL"
(( FAIL > 0 )) && warn "失败清单：${FAILED_IDS[*]} （可重跑本脚本自动补齐）"
exit 0
