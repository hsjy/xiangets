#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 chinese-poetry 开源语料库导入本地 MySQL 语料层（poem.tier=2）。

来源：/tmp/corpus2（由 raw.githubusercontent.com 下载）
  全唐诗/poet.tang.*.json   全唐诗约 5.7 万首（繁体）
  全唐诗/poet.song.*.json   全宋诗约 25.4 万首（繁体，含大量残篇）
  宋词/ci.song.*.json       全宋词约 2.1 万首
  五代诗词/huajianji/*.json 花间集约 500 首
  元曲/yuanqu.json          元曲约 1.1 万篇
  全唐诗/authors.*.json     诗人小传（intro 用）

处理：OpenCC 繁→简；按 (诗人, 朝代) 复用或新建诗人；按
（诗人, 题名, 首句前16字）去重——既避开精选层已有的 121 首，
也去除语料内部重复。语料层 tier=2，注释/译文/赏析/意境留空。

用法：python3 tools/import_corpus.py [--dry-run]
"""
import argparse
import glob
import json
import subprocess
import sys

from opencc import OpenCC

MYSQL = ['mysql', '-h127.0.0.1', '-P3307', '-uroot', '-proot',
         '--default-character-set=utf8mb4', 'yuetin']
CORPUS = '/tmp/corpus2'
cc = OpenCC('t2s')

stats = {'poems': 0, 'dup': 0, 'skip': 0, 'poets_new': 0}


def sql_escape(v):
    if v is None:
        return ''
    return (str(v).replace('\\', '\\\\').replace("'", "''")
            .replace('\r', '').replace('\x00', ''))


def q(sql):
    """单次查询。"""
    return subprocess.run(MYSQL + ['-N', '-B', '-e', sql],
                          capture_output=True, text=True, check=True).stdout


def load_existing():
    """精选层诗人与去重键。"""
    poets = {}
    for line in q("SELECT id,name,dynasty FROM poet").strip().split('\n'):
        if not line:
            continue
        pid, name, dynasty = line.split('\t')
        poets[(name, dynasty)] = int(pid)
    seen = set()
    for line in q("SELECT p.name, m.title, LEFT(REPLACE(m.paragraphs,'\\n',''),16) "
                  "FROM poem m JOIN poet p ON p.id=m.poet_id").strip().split('\n'):
        if line:
            seen.add(line)
    return poets, seen


def iter_poems():
    """产出 (author, dynasty, title, paragraphs[], genre) 元组。"""
    def rd(path):
        with open(path, encoding='utf-8') as f:
            return json.load(f)

    for p in sorted(glob.glob(f'{CORPUS}/全唐诗/poet.tang.*.json')):
        for x in rd(p):
            yield x.get('author', ''), '唐', x.get('title', ''), x.get('paragraphs') or [], ''
    for p in sorted(glob.glob(f'{CORPUS}/全唐诗/poet.song.*.json')):
        for x in rd(p):
            yield x.get('author', ''), '宋', x.get('title', ''), x.get('paragraphs') or [], ''
    for p in sorted(glob.glob(f'{CORPUS}/宋词/ci.song.*.json')):
        for x in rd(p):
            yield (x.get('author', ''), '宋', x.get('rhythmic', ''),
                   x.get('paragraphs') or [], '词')
    for p in sorted(glob.glob(f'{CORPUS}/五代诗词/huajianji/*.json')):
        for x in rd(p):
            t = x.get('title') or x.get('rhythmic') or ''
            yield x.get('author', ''), '五代', t, x.get('paragraphs') or [], '词'
    for x in rd(f'{CORPUS}/元曲/yuanqu.json'):
        yield x.get('author', ''), '元', x.get('title', ''), x.get('paragraphs') or [], '曲'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--dry-run', action='store_true')
    args = ap.parse_args()

    poets, seen = load_existing()
    print(f'现有诗人 {len(poets)} 位，去重键 {len(seen)} 条')

    # 1) 语料诗人小传 → 补 intro（同已存在诗人则更新其空 intro）
    bios = {}
    for p, dynasty in [(f'{CORPUS}/全唐诗/authors.tang.json', '唐'),
                       (f'{CORPUS}/全唐诗/authors.song.json', '宋'),
                       (f'{CORPUS}/宋词/author.song.json', '宋')]:
        for x in json.load(open(p, encoding='utf-8')):
            name = cc.convert((x.get('name') or '').strip())
            intro = cc.convert((x.get('desc') or x.get('description')
                                or x.get('short_description') or '').strip())
            if name and intro:
                bios[(name, dynasty)] = intro[:500]

    # 2) 收集语料作者全集（转简体后 find-or-create）
    need = set()
    plan = []
    for author, dynasty, title, paras, genre in iter_poems():
        if not author or not title or not paras:
            stats['skip'] += 1
            continue
        text = '\n'.join(cc.convert(line) for line in paras)
        if not text.strip() or len(text) > 12000:
            stats['skip'] += 1
            continue
        author = cc.convert(author.strip())[:50]
        title = cc.convert(str(title).strip())[:100]
        need.add((author, dynasty))
        plan.append((author, dynasty, title, text, genre))

    new_poets = sorted(need - set(poets))
    print(f'语料诗 {len(plan)} 首（跳过 {stats["skip"]}），新诗人 {len(new_poets)} 位')
    if args.dry_run:
        return

    if new_poets:
        rows = ','.join(
            f"('{sql_escape(n)}','', '', '{sql_escape(d)}', '', '', "
            f"'{sql_escape(bios.get((n, d), ''))}', '', '', 2, NOW())"
            for n, d in new_poets)
        subprocess.run(MYSQL, input=(
            'INSERT INTO poet (name, zi, hao, dynasty, years, title, intro, story, '
            'story_title, tier, create_time) VALUES ' + rows + ';'),
            text=True, check=True)
        stats['poets_new'] = len(new_poets)

    poets, _ = load_existing()          # 重新载入含新诗人 id

    # 3) 诗批量入库（500/批，流式写 stdin）
    proc = subprocess.Popen(MYSQL, stdin=subprocess.PIPE, text=True)
    w = proc.stdin
    w.write("SET NAMES utf8mb4;\n")
    batch = []
    for author, dynasty, title, text, genre in plan:
        pid = poets.get((author, dynasty))
        if pid is None:
            stats['skip'] += 1
            continue
        key = f'{author}\t{title}\t{text.replace(chr(10), "")[:16]}'
        if key in seen:
            stats['dup'] += 1
            continue
        seen.add(key)
        batch.append(
            f"('{sql_escape(title)}','', {pid}, '{sql_escape(dynasty)}', "
            f"'{sql_escape(genre)}', '{sql_escape(text)}', '', '', 2, NOW())")
        if len(batch) >= 500:
            w.write('INSERT INTO poem (title, subtitle, poet_id, dynasty, genre, '
                    'paragraphs, mood, famous_line, tier, create_time) VALUES '
                    + ','.join(batch) + ';\n')
            stats['poems'] += len(batch)
            batch = []
            if stats['poems'] % 20000 < 500:
                print(f"  …已写 {stats['poems']} 首", flush=True)
    if batch:
        w.write('INSERT INTO poem (title, subtitle, poet_id, dynasty, genre, '
                'paragraphs, mood, famous_line, tier, create_time) VALUES '
                + ','.join(batch) + ';\n')
        stats['poems'] += len(batch)
    w.close()
    proc.wait()
    print(f"完成：入库 {stats['poems']} 首，去重 {stats['dup']}，"
          f"跳过 {stats['skip']}，新诗人 {stats['poets_new']}")
    if proc.returncode != 0:
        sys.exit(1)


if __name__ == '__main__':
    main()
