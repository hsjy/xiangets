#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成行旅图的朝代行政区划图层：唐·道 / 北宋·路 / 东晋·州（示意）。

做法：阿里 DataV 省界 GeoJSON，用 shapely 按朝代归组（必要时以经纬线切分
跨越单元的省份，如江苏按长江分属淮南/两浙），合并为行政单元，等经纬投影到
与 china-outline.js 相同的 1000x720 视口，Douglas-Peucker 抽稀后各存为
frontend/src/assets/china-dyn-<朝代>.js，供 JourneyMap 懒加载。

界线为现代省界归组的近似示意，仅供意境参考，不做史实依据。
用法：python3 tools/gen_dynasty_map.py
"""
import json
import math
import urllib.request
from pathlib import Path

from shapely.geometry import shape, box
from shapely.ops import unary_union

SRC = 'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'
OUT_DIR = Path(__file__).resolve().parent.parent / 'frontend/src/assets'

W, H = 1000, 720
LON0, LON1 = 73.0, 135.2
LAT0, LAT1 = 17.8, 53.8
TOL = 1.2


def proj_pt(p):
    return ((p[0] - LON0) / (LON1 - LON0) * W,
            (LAT1 - p[1]) / (LAT1 - LAT0) * H)


def perp_dist(p, a, b):
    if a == b:
        return math.hypot(p[0] - a[0], p[1] - a[1])
    t = ((p[0] - a[0]) * (b[0] - a[0]) + (p[1] - a[1]) * (b[1] - a[1]))
    t /= (b[0] - a[0]) ** 2 + (b[1] - a[1]) ** 2
    t = max(0, min(1, t))
    px, py = a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1])
    return math.hypot(p[0] - px, p[1] - py)


def dp(pts, tol):
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


def path_of(rings):
    d = ''
    for pts in rings:
        if len(pts) < 4:
            continue
        d += f'M{pts[0][0]:.1f} {pts[0][1]:.1f}' + ''.join(
            f'L{x:.1f} {y:.1f}' for x, y in pts[1:]) + 'Z'
    return d


def rings_of(geom):
    """投影 + 抽稀后的环列表（仅外环，示意足够）。"""
    out = []
    geoms = getattr(geom, 'geoms', [geom])
    for g in geoms:
        out.append(dp([proj_pt(p) for p in g.exterior.coords], TOL))
    return out


# 切分经纬线：省份跨朝代单元时按此切
def cut_lat(geom, lat):
    return (geom & box(LON0 - 5, LAT0 - 5, LON1 + 5, lat),
            geom & box(LON0 - 5, lat, LON1 + 5, LAT1 + 5))


def cut_lon(geom, lon):
    return (geom & box(LON0 - 5, LAT0 - 5, lon, LAT1 + 5),
            geom & box(lon, LAT0 - 5, LON1 + 5, LAT1 + 5))


# ---------- 朝代归组表 ----------
# 条目：(单元名, [组分])；组分 = 省名 或 (省名, 切分函数)
# 省份切分后 [南, 北] / [西, 东] 依序取用

def jiangsu(g):    # [苏南→江南/两浙, 苏北]
    return cut_lat(g, 32.1)


def jiangsu2(g):   # [苏南, 苏北(徐州)]
    return cut_lat(g, 33.5)


def anhui(g):      # [皖南→江南, 皖北]
    return cut_lat(g, 30.9)


def anhui_mid(g):  # [皖中(江淮间), 皖北]，皖南已被 30.9 切走
    mid_north = cut_lat(g, 30.9)[1]
    return cut_lat(mid_north, 32.5)


def anhui2(g):     # [皖南, 皖中北]
    return cut_lat(g, 31.0)


def neimeng(g):    # [西部→关内, 东部→河北]
    return cut_lon(g, 111.0)


def pick(parts, idx):
    return parts[idx] if parts else None


def build_units(groups, provinces):
    """groups: [(unit_name, [spec])]，spec = 省名 或 (省名, 切分函数, 子索引)"""
    units = []
    for name, specs in groups:
        geoms = []
        for spec in specs:
            if isinstance(spec, tuple):
                pname, cutter, idx = spec
                pieces = cutter(provinces[pname])
                g = pick(pieces, idx)
            else:
                g = provinces.get(spec)
            if g is not None and not g.is_empty and g.area > 1e-9:
                geoms.append(g)
        if not geoms:
            continue
        merged = unary_union(geoms) if len(geoms) > 1 else geoms[0]
        merged = merged.buffer(0)          # 修自交
        label = merged.representative_point()
        units.append({
            'name': name,
            'rings': rings_of(merged),
            'label': proj_pt((label.x, label.y)),
        })
    return units


TANG = [
    ('关内道', ['陕西省', '宁夏回族自治区', ('内蒙古自治区', neimeng, 0)]),
    ('河南道', ['山东省', '河南省', ('江苏省', jiangsu, 1), ('安徽省', anhui_mid, 1)]),
    ('河东道', ['山西省']),
    ('河北道', ['河北省', '北京市', '天津市', '辽宁省', ('内蒙古自治区', neimeng, 1)]),
    ('陇右道', ['甘肃省', '新疆维吾尔自治区', '青海省']),
    ('山南东道', ['湖北省', '重庆市']),
    ('淮南道', [('江苏省', jiangsu, 1), ('安徽省', anhui_mid, 0)]),
    ('江南东道', ['浙江省', '福建省', '上海市', ('江苏省', jiangsu, 0), ('安徽省', anhui, 0)]),
    ('江南西道', ['江西省', '湖南省']),
    ('黔中道', ['贵州省']),
    ('剑南道', ['四川省']),
    ('岭南道', ['广东省', '广西壮族自治区', '海南省', '香港特别行政区', '澳门特别行政区']),
    ('吐蕃', ['西藏自治区']),
    ('南诏', ['云南省']),
]

SONG = [
    ('京东东路', ['山东省']),
    ('京畿路', ['河南省']),
    ('河北东路·西路', ['河北省', '北京市', '天津市']),
    ('河东路', ['山西省']),
    ('永兴军路', ['陕西省', '宁夏回族自治区']),
    ('秦凤路', ['甘肃省']),
    ('京东西路', [('江苏省', jiangsu2, 1)]),
    ('淮南东路', [('江苏省', jiangsu2, 0)]),
    ('两浙路', ['浙江省', '上海市', ('江苏省', jiangsu, 0)]),
    ('淮南西路', [('安徽省', anhui, 1)]),
    ('江南东路', [('安徽省', anhui, 0)]),
    ('江南西路', ['江西省']),
    ('荆湖南路', ['湖南省']),
    ('荆湖北路', ['湖北省']),
    ('成都府路', ['四川省']),
    ('夔州路', ['重庆市']),
    ('广南东路', ['广东省', '香港特别行政区', '澳门特别行政区']),
    ('广南西路', ['广西壮族自治区', '海南省']),
    ('福建路', ['福建省']),
    ('北境 · 辽', ['内蒙古自治区', '辽宁省', '吉林省', '黑龙江省']),
    ('大理', ['云南省', '贵州省']),
    ('西陲诸部', ['新疆维吾尔自治区', '青海省', '西藏自治区']),
]

DONGJIN = [
    ('江州', ['江西省', '福建省']),
    ('荆州', ['湖北省', '湖南省', '贵州省']),
    ('扬州', ['浙江省', '上海市', ('江苏省', jiangsu, 0), ('安徽省', anhui2, 0)]),
    ('徐州·豫州', [('江苏省', jiangsu, 1), ('安徽省', anhui2, 1)]),
    ('广州', ['广东省', '广西壮族自治区', '海南省', '香港特别行政区', '澳门特别行政区']),
    ('梁州·益州', ['四川省', '重庆市']),
    ('宁州', ['云南省']),
    ('北境 · 十六国', ['山东省', '山西省', '陕西省', '甘肃省', '宁夏回族自治区',
                     '河北省', '北京市', '天津市', '河南省', '辽宁省', '吉林省',
                     '黑龙江省', '内蒙古自治区']),
    ('西陲诸部', ['新疆维吾尔自治区', '青海省', '西藏自治区']),
]

DYNASTIES = [('tang', '唐 · 开元十五道（示意）', TANG),
             ('song', '宋 · 路（示意）', SONG),
             ('dongjin', '东晋 · 州（示意）', DONGJIN)]


def main():
    with urllib.request.urlopen(SRC, timeout=60) as r:
        data = json.load(r)
    provinces = {}
    for f in data['features']:
        name = f['properties'].get('name')
        if not name:
            continue
        provinces[name] = shape(f['geometry']).buffer(0)

    for key, title, groups in DYNASTIES:
        units = build_units(groups, provinces)
        body = {
            'dynasty': key,
            'title': title,
            'units': [{'name': u['name'],
                       'd': path_of(u['rings']),
                       'label': [round(u['label'][0], 1), round(u['label'][1], 1)]}
                      for u in units if u['rings']],
        }
        out = OUT_DIR / f'china-dyn-{key}.js'
        out.write_text(
            f'/** 朝代行政区划图层（由 tools/gen_dynasty_map.py 生成，勿手改；界线系示意）*/\n'
            'export const DYNASTY_MAP = ' + json.dumps(body, ensure_ascii=False) + '\n',
            encoding='utf-8')
        kb = out.stat().st_size / 1024
        print(f"✓ {key}: {len(body['units'])} 单元, {kb:.0f}KB → {out.name}")


if __name__ == '__main__':
    main()
