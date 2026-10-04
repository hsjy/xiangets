#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
诗人小传回填：用 chinese-poetry 语料自带的作者小传，
填补语料层诗人（tier=2）空缺的 intro，让诗人页不再大面积空白。

来源：/tmp/corpus2/全唐诗/authors.tang.json、authors.song.json、宋词/author.song.json
匹配：姓名 + 朝代（唐/宋）精确匹配，只填空值，不覆盖已有小传。
用法：python3 tools/backfill_bios.py [--dry-run]
"""
import argparse
import json
import subprocess

from opencc import OpenCC

MYSQL = ['mysql', '-h127.0.0.1', '-P3307', '-uroot', '-proot',
         '--default-character-set=utf8mb4', 'yuetin']
CORPUS = '/tmp/corpus2'
cc = OpenCC('t2s')


def q(sql):
    return subprocess.run(MYSQL + ['-N', '-B', '-e', sql],
                          capture_output=True, text=True, check=True).stdout


def esc(v):
    return str(v).replace('\\', '\\\\').replace("'", "''").replace('\n', ' ')


def load_bios():
    bios = {}
    for p, dynasty in [(f'{CORPUS}/全唐诗/authors.tang.json', '唐'),
                       (f'{CORPUS}/全唐诗/authors.song.json', '宋'),
                       (f'{CORPUS}/宋词/author.song.json', '宋')]:
        try:
            data = json.load(open(p, encoding='utf-8'))
        except Exception as e:
            print(f'跳过 {p}: {e}')
            continue
        for x in data:
            name = cc.convert((x.get('name') or '').strip())
            intro = cc.convert((x.get('desc') or x.get('description')
                                or x.get('short_description') or '').strip())
            if name and intro and (name, dynasty) not in bios:
                bios[(name, dynasty)] = intro[:500]
    return bios


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--dry-run', action='store_true')
    args = ap.parse_args()

    bios = load_bios()
    print(f'语料小传 {len(bios)} 条')

    targets = {}
    for line in q("SELECT id,name,dynasty FROM poet WHERE tier=2 AND (intro IS NULL OR intro='')").strip().split('\n'):
        if not line:
            continue
        pid, name, dynasty = line.split('\t')
        bio = bios.get((name, dynasty))
        if bio:
            targets[int(pid)] = bio
    print(f'可回填诗人 {len(targets)} 位')
    if args.dry_run or not targets:
        return

    done = 0
    batch = []
    for pid, bio in targets.items():
        batch.append(f"UPDATE poet SET intro='{esc(bio)}' WHERE id={pid};")
        if len(batch) >= 200:
            subprocess.run(MYSQL, input='\n'.join(batch), text=True, check=True)
            done += len(batch)
            batch = []
    if batch:
        subprocess.run(MYSQL, input='\n'.join(batch), text=True, check=True)
        done += len(batch)
    print(f'✓ 回填 {done} 位诗人生平小传')


if __name__ == '__main__':
    main()
