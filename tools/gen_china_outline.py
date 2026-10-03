#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成诗人行旅图底图资产 frontend/src/assets/china-outline.js

来源：阿里 DataV.GeoAtlas 全国轮廓（简化后内嵌，运行期零依赖、零请求）。
做法：等经纬投影到 1000x720 视口，主图保留 lat>=17.8 的陆块，
南海南部诸岛与九段线缩入右下小图框（标准地图表示法），最后 Douglas-Peucker 抽稀。
长江/黄河为装饰性曲线，由手工航点经 Catmull-Rom 平滑而来。

用法：python3 tools/gen_china_outline.py
"""
import json
import math
import urllib.request
from pathlib import Path

SRC = 'https://geo.datav.aliyun.com/areas_v3/bound/100000.json'
OUT = Path(__file__).resolve().parent.parent / 'frontend/src/assets/china-outline.js'

W, H = 1000, 720
LON0, LON1 = 73.0, 135.2
LAT0, LAT1 = 17.8, 53.8          # 主图纬度范围；lat 更低的岛进南海小图框
MAIN_TOL = 1.1                   # 主图抽稀容差（视口单位）
INSET = dict(x=812, y=520, w=164, h=196)   # 小图框（右下）
INSET_LAT0, INSET_LAT1 = 3.2, 21.5
INSET_LON0, INSET_LON1 = 106.0, 122.5
KEEP_PT = 5                       # 抽稀后少于该点数的碎岛丢弃


def proj(lon, lat):
    return ((lon - LON0) / (LON1 - LON0) * W,
            (LAT1 - lat) / (LAT1 - LAT0) * H)


def proj_inset(lon, lat):
    s = min(INSET['w'] / (INSET_LON1 - INSET_LON0),
            INSET['h'] / (INSET_LAT1 - INSET_LAT0))
    ox = INSET['x'] + (INSET['w'] - (INSET_LON1 - INSET_LON0) * s) / 2
    oy = INSET['y'] + (INSET['h'] - (INSET_LAT1 - INSET_LAT0) * s) / 2
    return (ox + (lon - INSET_LON0) * s,
            oy + (INSET_LAT1 - lat) * s)


def perp_dist(p, a, b):
    if a == b:
        return math.hypot(p[0] - a[0], p[1] - a[1])
    t = ((p[0] - a[0]) * (b[0] - a[0]) + (p[1] - a[1]) * (b[1] - a[1]))
    t /= (b[0] - a[0]) ** 2 + (b[1] - a[1]) ** 2
    t = max(0, min(1, t))
    px, py = a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1])
    return math.hypot(p[0] - px, p[1] - py)


def dp(pts, tol):
    """Douglas-Peucker 抽稀。"""
    if len(pts) < 3:
        return pts
    stack, keep = [(0, len(pts) - 1)], [False] * len(pts)
    keep[0] = keep[-1] = True
    while stack:
        i, j = stack.pop()
        if j <= i + 1:
            continue
        d, k = -1, -1
        for n in range(i + 1, j):
            dd = perp_dist(pts[n], pts[i], pts[j])
            if dd > d:
                d, k = dd, n
        if d > tol:
            keep[k] = True
            stack += [(i, k), (k, j)]
    return [p for p, k in zip(pts, keep) if k]


def path_of(pts):
    d = f'M{pts[0][0]:.1f} {pts[0][1]:.1f}'
    for x, y in pts[1:]:
        d += f'L{x:.1f} {y:.1f}'
    return d + 'Z'


def catmull_path(pts):
    """航点 → 平滑贝塞尔曲线（Catmull-Rom 转三次贝塞尔）。"""
    if len(pts) < 3:
        return path_of(pts + pts[-1:])[:-1]
    d = f'M{pts[0][0]:.1f} {pts[0][1]:.1f}'
    for i in range(len(pts) - 1):
        p0 = pts[max(i - 1, 0)]
        p1, p2 = pts[i], pts[i + 1]
        p3 = pts[min(i + 2, len(pts) - 1)]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        d += f'C{c1[0]:.1f} {c1[1]:.1f} {c2[0]:.1f} {c2[1]:.1f} {p2[0]:.1f} {p2[1]:.1f}'
    return d


def fetch():
    with urllib.request.urlopen(SRC, timeout=30) as r:
        return json.load(r)


def main():
    data = fetch()
    rings = data['features'][0]['geometry']['coordinates']

    main_land, inset_land = [], []
    for poly in rings:
        ring = poly[0]
        lat_min = min(p[1] for p in ring)
        target, pj = (main_land, proj) if lat_min >= LAT0 - 0.5 else (inset_land, proj_inset)
        pts = [pj(*p) for p in ring]
        pts = dp(pts, MAIN_TOL if target is main_land else MAIN_TOL / 2.2)
        if len(pts) >= KEEP_PT:
            target.append(path_of(pts))

    # 装饰性江河（航点取沿江代表性城市坐标，平滑后仅示意走向）
    yangtze = [(91.0, 33.4), (96.0, 32.8), (98.5, 31.0), (100.2, 26.9), (102.0, 26.9),
               (104.6, 28.75), (106.55, 29.56), (108.5, 30.3), (111.3, 30.7),
               (114.3, 30.6), (116.0, 29.7), (117.3, 30.9), (118.8, 32.06),
               (120.0, 32.2), (121.9, 31.4)]
    yellow = [(96.5, 34.9), (100.0, 35.0), (103.8, 36.06), (105.5, 37.5), (106.8, 39.8),
              (106.2, 40.6), (108.0, 40.6), (110.8, 40.3), (111.3, 39.0), (110.5, 38.0),
              (110.4, 36.5), (110.3, 34.75), (111.5, 34.8), (113.6, 34.85),
              (115.5, 35.2), (117.0, 36.2), (118.2, 37.0), (119.0, 37.8)]
    rivers = [catmull_path([proj(*p) for p in yangtze]),
              catmull_path([proj(*p) for p in yellow])]

    js = {
        'width': W, 'height': H,
        'land': ' '.join(main_land),
        'inset': ' '.join(inset_land),
        'insetBox': INSET,
        'rivers': rivers,
    }
    OUT.write_text(
        '/** 行旅图底图（由 tools/gen_china_outline.py 生成，勿手改）*/\n'
        'export const CHINA_MAP = ' + json.dumps(js, ensure_ascii=False) + '\n',
        encoding='utf-8')
    n_main = js['land'].count('M')
    print(f'✓ 主图 {n_main} 块陆域 / 南海小图 {js["inset"].count("M")} 块 → {OUT}')


if __name__ == '__main__':
    main()
