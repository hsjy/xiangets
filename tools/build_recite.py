#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
弦歌 · 朗诵生成器
从 MySQL 读取诗行，用 edge-tts 生成配乐朗诵用的语音轨（女声/男声按诗人气质分配）。
音频落盘 backend/audio/recite/poem-{id}.mp3，供 V5 种子挂载到 song 表。

用法：python3 tools/build_recite.py [--sock ...] [--out ../backend/audio/recite]
"""
import argparse
import json
import subprocess
import sys
from pathlib import Path

import edge_tts

# 女声给李清照，其余男声（云溪：温润书生音色）
VOICES = {
    '李清照': ('zh-CN-XiaoxiaoNeural', '晓晓'),
    '_default': ('zh-CN-YunxiNeural', '云溪'),
}
RATE = '-8%'  # 诗不宜快


def fetch_poems(sock, mysql_bin):
    """batch 模式取诗；paragraphs 内换行替换为 ◆ 防止 TSV 断行。"""
    sql = (
        "SELECT m.id, m.title, IFNULL(m.subtitle,''), p.name, p.dynasty, "
        "REPLACE(m.paragraphs, '\\n', '◆') "
        "FROM poem m JOIN poet p ON p.id = m.poet_id ORDER BY m.id;"
    )
    out = subprocess.run(
        [mysql_bin, f'--socket={sock}', '-uroot', '-proot', '-N', '-B', 'yuetin', '-e', sql],
        capture_output=True, text=True, check=True,
    ).stdout
    poems = []
    for line in out.strip().split('\n'):
        fid, title, subtitle, poet, dynasty, paragraphs = line.split('\t')
        poems.append(dict(id=int(fid), title=title, subtitle=subtitle,
                          poet=poet, dynasty=dynasty,
                          paragraphs=paragraphs.replace('◆', '\n')))
    return poems


async def synth(poem, out_path, voice):
    text = f'{poem["title"]}。'
    if poem['subtitle']:
        text += f'{poem["subtitle"]}。'
    text += f'{poem["dynasty"]}，{poem["poet"]}。\n\n{poem["paragraphs"]}'
    tts = edge_tts.Communicate(text, voice, rate=RATE)
    await tts.save(str(out_path))


async def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--sock', default='/Users/hs/Projects/music/backend/mysql-data/mysql.sock')
    ap.add_argument('--mysql', default='/opt/homebrew/opt/mysql/bin/mysql')
    ap.add_argument('--out', default='../backend/audio/recite')
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    poems = fetch_poems(args.sock, args.mysql)
    print(f'共 {len(poems)} 首待生成')
    import asyncio
    for p in poems:
        voice, persona = VOICES.get(p['poet'], VOICES['_default'])
        path = out / f'poem-{p["id"]}.mp3'
        if path.exists() and path.stat().st_size > 10000:
            print(f'  跳过（已存在）poem-{p["id"]} {p["title"]}')
            continue
        # edge-tts 偶发 NoAudioReceived / 网络抖动，指数退避重试
        for attempt in range(1, 5):
            try:
                await synth(p, path, voice)
                print(f'  ✓ poem-{p["id"]}《{p["title"]}》({persona}) {path.stat().st_size // 1024}KB')
                break
            except Exception as e:
                if attempt == 4:
                    print(f'  ✗ poem-{p["id"]}《{p["title"]}》失败: {e}')
                    break
                await asyncio.sleep(2 ** attempt)


if __name__ == '__main__':
    import asyncio
    asyncio.run(main())
