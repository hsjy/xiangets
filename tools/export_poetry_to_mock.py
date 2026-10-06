#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 MySQL 里的诗人/诗/音轨导出为前端 mock 层数据文件
frontend/src/mock/poetry_data.js —— 保证 mock 模式与真实后端内容一致。

用法：python3 tools/export_poetry_to_mock.py
"""
import json
import re
import subprocess
from pathlib import Path

MYSQL = '/opt/homebrew/opt/mysql/bin/mysql'
SOCK = '/Users/hs/Projects/music/backend/mysql-data/mysql.sock'


def unescape(v):
    """mysql -B 会把字段内的换行/制表符/反斜杠转义为 \n \t \\，这里还原。"""
    return v.replace('\\n', '\n').replace('\\t', '\t').replace('\\\\', '\\')


def q(sql):
    """batch 模式查询 → TSV 行列表。"""
    out = subprocess.run(
        [MYSQL, f'--socket={SOCK}', '-uroot', '-proot', '-N', '-B', 'yuetin', '-e', sql],
        capture_output=True, text=True, check=True).stdout
    return [[unescape(c) for c in line.split('\t')] for line in out.strip().split('\n') if line]


def esc(v):
    return v.replace('\\', '\\\\').replace("'", "\\'").replace('\n', '\\n')


def cast(v):
    """MySQL batch 输出全是字符串；id/数值字段必须转回数字，否则 === 匹配失败。"""
    if v is None or v == 'NULL':
        return None
    if re.fullmatch(r'-?\d+', v):
        return int(v)
    if re.fullmatch(r'-?\d+\.\d+', v):
        return float(v)
    return v


poets = []
for row in q("SELECT id,name,IFNULL(zi,''),IFNULL(hao,''),dynasty,IFNULL(years,''),"
             "IFNULL(title,''),IFNULL(intro,''),IFNULL(story,''),IFNULL(story_title,''),"
             "IFNULL(journey,'') FROM poet WHERE tier=1 ORDER BY id"):
    poets.append(dict(zip(['id', 'name', 'zi', 'hao', 'dynasty', 'years', 'title',
                           'intro', 'story', 'storyTitle', 'journey'], [cast(x) for x in row])))

poems = []
for row in q("SELECT m.id,m.title,IFNULL(m.subtitle,''),m.poet_id,m.dynasty,IFNULL(m.genre,''),"
             "m.paragraphs,IFNULL(m.annotation,''),IFNULL(m.translation,''),IFNULL(m.appreciation,''),"
             "IFNULL(m.mood,''),IFNULL(m.famous_line,''),IFNULL(m.famous_note,''),p.name FROM poem m "
             "JOIN poet p ON p.id=m.poet_id WHERE m.tier=1 ORDER BY m.id"):
    poems.append(dict(zip(['id', 'title', 'subtitle', 'poetId', 'dynasty', 'genre', 'paragraphs',
                           'annotation', 'translation', 'appreciation', 'mood', 'famousLine',
                           'poetName', 'famousNote'], [cast(x) for x in row])))

tracks = []
for row in q("SELECT s.id,s.name,s.poem_id,s.track_kind,s.url,ROUND(s.duration,1),s.style "
             "FROM song s WHERE s.poem_id IS NOT NULL OR s.track_kind IS NOT NULL ORDER BY s.id"):
    tracks.append(dict(zip(['id', 'name', 'poemId', 'trackKind', 'url', 'duration', 'style'],
                           [cast(x) for x in row])))

lines = ['/**', ' * 诗与诗人数据（由 tools/export_poetry_to_mock.py 从 MySQL 导出，勿手改）', ' */']
lines = ['/**', ' * 诗与诗人数据（由 tools/export_poetry_to_mock.py 从 MySQL 导出，勿手改）', ' */']
lines.append('export const POETS = ' + json.dumps(poets, ensure_ascii=False) + ';')
lines.append('export const POEMS = ' + json.dumps(poems, ensure_ascii=False) + ';')
lines.append('export const POEM_TRACKS = ' + json.dumps(tracks, ensure_ascii=False) + ';')

Path('../frontend/src/mock/poetry_data.js').write_text('\n'.join(lines) + '\n', encoding='utf-8')
print(f'✓ 导出 {len(poets)} 诗人 / {len(poems)} 诗 / {len(tracks)} 音轨 → frontend/src/mock/poetry_data.js')
