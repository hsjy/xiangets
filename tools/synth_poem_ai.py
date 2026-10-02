#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
弦歌 · 每诗专属 AI 风格曲预生成
为每首诗按「情绪配方 + 诗 id 种子」合成一段专属曲（45~75s），
落盘 backend/audio/generated/ai-poem-{id}.mp3，作为 AI 谱曲模块的预生成兜底。

无 GPU 时用户点「为此诗谱曲」秒级完成；
配置 ACE-STEP_URL 后同一任务流切换为真实 AI 生成。

用法：python3 tools/synth_poem_ai.py [--sock ...] [--out ../backend/audio/generated]
"""
import argparse
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from synth_bgm import make_piece, encode_mp3  # noqa: E402

MYSQL = '/opt/homebrew/opt/mysql/bin/mysql'

# 情绪 → 合成参数（音阶用 MIDI 半音集合；调式/密度/音区决定气质）
MOOD_PARAMS = {
    '月夜':  dict(scale=[9, 11, 14, 16, 19, 23], register=-12, density=0.42, tempo=1.05, brightness=0.3),
    '田园':  dict(scale=[0, 2, 4, 7, 9, 12, 14], register=0, density=0.6, tempo=0.82, brightness=0.55),
    '山水':  dict(scale=[0, 2, 4, 7, 9, 12, 16], register=4, density=0.6, tempo=0.75, brightness=0.5),
    '送别':  dict(scale=[9, 12, 14, 16, 19, 21], register=-5, density=0.42, tempo=1.12, brightness=0.3),
    '思乡':  dict(scale=[9, 11, 14, 16, 19, 22], register=-9, density=0.46, tempo=1.0, brightness=0.34),
    '家国':  dict(scale=[0, 2, 4, 7, 9, 12], register=-12, density=0.5, tempo=0.95, brightness=0.42),
    '悲秋':  dict(scale=[9, 11, 14, 16, 19, 21], register=-10, density=0.44, tempo=1.08, brightness=0.3),
    '豪放':  dict(scale=[0, 2, 4, 7, 9, 12], register=-5, density=0.68, tempo=0.68, brightness=0.6),
    '闲适':  dict(scale=[0, 2, 4, 7, 9, 14], register=5, density=0.5, tempo=0.85, brightness=0.5),
    '哲理':  dict(scale=[0, 2, 4, 7, 9, 11], register=-7, density=0.48, tempo=0.92, brightness=0.42),
    '壮志':  dict(scale=[0, 2, 4, 7, 9, 12], register=-7, density=0.7, tempo=0.65, brightness=0.62),
    '怀人':  dict(scale=[9, 11, 14, 16, 19, 23], register=7, density=0.44, tempo=1.06, brightness=0.36),
}
DEFAULT_PARAMS = MOOD_PARAMS['月夜']

PATTERNS = {
    '月夜': lambda b, r: r.choice(['single', 'rest', 'harm', 'double'], p=[0.4, 0.3, 0.2, 0.1]),
    '田园': lambda b, r: r.choice(['single', 'arp', 'double', 'rest'], p=[0.4, 0.25, 0.2, 0.15]),
    '山水': lambda b, r: r.choice(['arp', 'single', 'harm', 'rest'], p=[0.35, 0.3, 0.2, 0.15]),
    '送别': lambda b, r: r.choice(['single', 'double', 'rest', 'harm'], p=[0.45, 0.2, 0.25, 0.1]),
    '思乡': lambda b, r: r.choice(['single', 'rest', 'harm', 'double'], p=[0.42, 0.28, 0.2, 0.1]),
    '家国': lambda b, r: r.choice(['double', 'single', 'rest', 'arp'], p=[0.3, 0.35, 0.2, 0.15]),
    '悲秋': lambda b, r: r.choice(['single', 'rest', 'harm', 'double'], p=[0.4, 0.3, 0.18, 0.12]),
    '豪放': lambda b, r: r.choice(['double', 'arp', 'single', 'rest'], p=[0.32, 0.3, 0.26, 0.12]),
    '闲适': lambda b, r: r.choice(['single', 'arp', 'harm', 'rest'], p=[0.38, 0.24, 0.2, 0.18]),
    '哲理': lambda b, r: r.choice(['single', 'harm', 'rest', 'double'], p=[0.4, 0.22, 0.24, 0.14]),
    '壮志': lambda b, r: r.choice(['double', 'arp', 'single', 'rest'], p=[0.35, 0.3, 0.25, 0.1]),
    '怀人': lambda b, r: r.choice(['single', 'harm', 'rest', 'arp'], p=[0.4, 0.25, 0.2, 0.15]),
}


def fetch_poems(sock, mysql_bin):
    sql = ("SELECT m.id, IFNULL(m.mood,'月夜') FROM poem m ORDER BY m.id;")
    out = subprocess.run(
        [mysql_bin, f'--socket={sock}', '-uroot', '-proot', '-N', '-B', 'yuetin', '-e', sql],
        capture_output=True, text=True, check=True).stdout
    return [(int(l.split('\t')[0]), l.split('\t')[1]) for l in out.strip().split('\n') if l]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--sock', default='/Users/hs/Projects/music/backend/mysql-data/mysql.sock')
    ap.add_argument('--mysql', default=MYSQL)
    ap.add_argument('--out', default='../backend/audio/generated')
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    poems = fetch_poems(args.sock, args.mysql)
    print(f'共 {len(poems)} 首，开始预生成专属曲…')
    for pid, mood in poems:
        path = out / f'ai-poem-{pid}.mp3'
        if path.exists() and path.stat().st_size > 10000:
            print(f'  跳过 ai-poem-{pid}（已存在）')
            continue
        cfg = MOOD_PARAMS.get(mood, DEFAULT_PARAMS)
        dur = 48 + (pid * 7) % 28          # 48~75s，因诗而异
        stereo = make_piece(
            f'ai-{pid}', dur,
            tempo=cfg['tempo'] * (1 + (pid % 5) * 0.03),   # 微调节拍避免千篇一律
            scale_midi=cfg['scale'], register_shift=cfg['register'],
            pattern=PATTERNS.get(mood, PATTERNS['月夜']),
            density=cfg['density'], seed=7000 + pid,        # 诗 id 即种子
            brightness=cfg['brightness'],
        )
        path.write_bytes(encode_mp3(stereo, kbps=104))
        print(f'  ✓ ai-poem-{pid}《{mood}》{dur}s {path.stat().st_size // 1024}KB')


if __name__ == '__main__':
    main()
