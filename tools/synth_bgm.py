#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
弦歌 · 古筝风配乐合成器
Karplus-Strong 拨弦物理建模 + 五声音阶（宫商角徵羽），
按情绪生成 90 秒立体声配乐，供诗的意境页铺底。

零版权风险（纯合成）、离线可复现（固定随机种子）。
用法：python3 tools/synth_bgm.py [--out ../backend/audio/bgm]
"""
import argparse
import struct
import sys
from pathlib import Path

import lameenc
import numpy as np

SR = 44100

# 五声音阶（D 宫系统），以半音数表示
GONG = [0, 2, 4, 7, 9]          # 宫调：大调色彩，明亮
YU = [9, 11, 14, 16, 19]        # 羽调：小调色彩，柔和暗淡（相对同主音）
BASE = 62                        # D4 的 MIDI 编号


def midi_hz(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def pluck(freq, dur, brightness=0.5, amp=0.5):
    """Karplus-Strong 拨弦：噪声激励 + 延迟线低通反馈。"""
    n = int(SR * dur)
    period = max(2, int(SR / freq))
    rng = np.random.default_rng(int(freq * 1000) % (2 ** 32))
    excitation = rng.uniform(-1, 1, period) * (0.6 + 0.4 * brightness)
    buf = np.copy(excitation)
    out = np.empty(n)
    decay = 0.996 + 0.003 * (1 - brightness)   # 越暗越绵长
    for i in range(n):
        j = i % period
        out[i] = buf[j]
        buf[j] = decay * 0.5 * (buf[j] + buf[(j + 1) % period])
    env = np.minimum(1, np.arange(n) / (SR * 0.004))   # 4ms 起音，避免爆音
    return out * env * amp


def harmonic(freq, dur, amp=0.3):
    """泛音：清越的高频点缀（仿古筝泛音奏法）。"""
    n = int(SR * dur)
    t = np.arange(n) / SR
    env = np.exp(-t * 3.5)
    return (np.sin(2 * np.pi * freq * t) + 0.4 * np.sin(2 * np.pi * freq * 2 * t)) * env * amp


def add(buf, start_s, sig, gain=1.0, pan=0.0):
    """把信号叠进立体声缓冲，pan∈[-1,1] 用 Haas 延迟营造宽度。"""
    start = int(start_s * SR)
    end = min(start + len(sig), len(buf[0]))
    seg = sig[: end - start] * gain
    pan_l, pan_r = (1 - pan) / 2, (1 + pan) / 2
    buf[0][start:end] += seg * pan_l
    buf[1][start + int(0.008 * SR):end + int(0.008 * SR) - int(0.008 * SR)] += \
        seg[: max(0, (end - start) - int(0.008 * SR))] * pan_r


def echo(buf, delay_s=0.24, feedback=0.28, mix=0.22):
    """简单立体声延迟，仿山间回响。"""
    d = int(delay_s * SR)
    for ch in range(2):
        out = np.copy(buf[ch])
        src = np.copy(buf[ch])
        for rep in range(1, 4):
            shifted = np.zeros_like(src)
            shifted[d * rep:] = src[:-d * rep] * (feedback ** rep)
            out += shifted
        buf[ch] = buf[ch] * (1 - mix) + out * mix


def make_piece(name, dur_s, tempo, scale_midi, register_shift, pattern, density, seed, brightness):
    """
    name: 输出名；tempo: 节拍间隔秒；scale_midi: 可用音高集合；
    register_shift: 整体移位（MIDI 半音）；pattern: 音序生成风格
    """
    rng = np.random.default_rng(seed)
    total = int(SR * dur_s)
    buf = [np.zeros(total), np.zeros(total)]

    t = 0.0
    beat = 0
    while t < dur_s - 4:
        # 按 pattern 决定本拍行为：拨弦单音 / 双八度 / 琶音 / 泛音 / 留白
        action = pattern(beat, rng)
        if action == 'rest':
            t += tempo * 2
            beat += 1
            continue
        midi_base = int(rng.choice(scale_midi)) + register_shift
        if action == 'double':                       # 低八度 + 主音，古筝撮弦
            for off, g in ((-12, 0.5), (0, 0.8)):
                sig = pluck(midi_hz(midi_base + off), 3.2, brightness, 1.0)
                add(buf, t, sig, g, rng.uniform(-0.4, 0.4))
            t += tempo * 2
        elif action == 'arp':                        # 琶音：连拨四音
            for k, off in enumerate((0, 4, 7, 12)):
                sig = pluck(midi_hz(midi_base + off - scale_midi[0]), 2.2, brightness, 0.85)
                add(buf, t + k * tempo * 0.5, sig, 0.8, rng.uniform(-0.5, 0.5))
            t += tempo * 2.6
        elif action == 'harm':                       # 泛音点缀
            sig = harmonic(midi_hz(midi_base + 12), 2.6, 0.9)
            add(buf, t, sig, 0.9, rng.uniform(-0.3, 0.3))
            t += tempo * 1.4
        else:                                        # 单音
            sig = pluck(midi_hz(midi_base), 3.0, brightness, 0.95)
            add(buf, t, sig, 0.95, rng.uniform(-0.45, 0.45))
            t += tempo
        # 密度控制：随机插入呼吸空隙
        if rng.random() > density:
            t += tempo * rng.integers(1, 4)
        beat += 1

    echo(buf)
    # 首尾淡入淡出
    fade = int(2.5 * SR)
    for ch in range(2):
        buf[ch][:fade] *= np.linspace(0, 1, fade)
        buf[ch][-fade:] *= np.linspace(1, 0, fade)
    peak = max(np.max(np.abs(buf[0])), np.max(np.abs(buf[1])), 1e-9)
    for ch in range(2):
        buf[ch] = buf[ch] / peak * 0.82
    return buf


# 情绪配方：d(宫调 D 宫) 为明亮，y(羽调) 为暗柔
RECIPES = {
    'yueye':   dict(cn='月夜', dur=92, tempo=1.05, register=-12, density=0.45, brightness=0.32,
                    scale=[9, 11, 14, 16, 19, 23, 26], seed=101, pat=lambda b, r: r.choice(['single', 'rest', 'harm', 'double'], p=[0.4, 0.3, 0.2, 0.1])),
    'tianyuan': dict(cn='田园', dur=90, tempo=0.8, register=0, density=0.6, brightness=0.55,
                     scale=[0, 2, 4, 7, 9, 12, 14], seed=202, pat=lambda b, r: r.choice(['single', 'arp', 'double', 'rest'], p=[0.4, 0.25, 0.2, 0.15])),
    'shanshui': dict(cn='山水', dur=91, tempo=0.72, register=4, density=0.62, brightness=0.5,
                     scale=[0, 2, 4, 7, 9, 12, 16], seed=303, pat=lambda b, r: r.choice(['arp', 'single', 'harm', 'rest'], p=[0.35, 0.3, 0.2, 0.15])),
    'songbie': dict(cn='送别', dur=93, tempo=1.15, register=-5, density=0.4, brightness=0.3,
                    scale=[9, 12, 14, 16, 19, 21, 24], seed=404, pat=lambda b, r: r.choice(['single', 'double', 'rest', 'harm'], p=[0.45, 0.2, 0.25, 0.1])),
    'zhuangzhi': dict(cn='壮志', dur=90, tempo=0.62, register=-7, density=0.72, brightness=0.62,
                      scale=[0, 2, 4, 7, 9, 12], seed=505, pat=lambda b, r: r.choice(['double', 'arp', 'single', 'rest'], p=[0.35, 0.3, 0.25, 0.1])),
    'huairen': dict(cn='怀人', dur=92, tempo=1.1, register=7, density=0.42, brightness=0.36,
                    scale=[9, 11, 14, 16, 19, 23], seed=606, pat=lambda b, r: r.choice(['single', 'harm', 'rest', 'arp'], p=[0.4, 0.25, 0.2, 0.15])),
}


def encode_mp3(stereo, kbps=112):
    enc = lameenc.Encoder()
    enc.set_bit_rate(kbps)
    enc.set_in_sample_rate(SR)
    enc.set_channels(2)
    enc.set_quality(2)
    pcm = np.empty(stereo[0].size * 2, dtype=np.int16)
    pcm[0::2] = np.clip(stereo[0] * 32767, -32767, 32767).astype(np.int16)
    pcm[1::2] = np.clip(stereo[1] * 32767, -32767, 32767).astype(np.int16)
    data = enc.encode(pcm.tobytes()) + enc.flush()
    return data


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default='../backend/audio/bgm')
    args = ap.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    for key, r in RECIPES.items():
        path = out / f'bgm-{key}.mp3'
        print(f'合成 {r["cn"]}（{r["dur"]}s）…', end=' ', flush=True)
        stereo = make_piece(key, r['dur'], r['tempo'], r['scale'],
                            r['register'], r['pat'], r['density'], r['seed'], r['brightness'])
        path.write_bytes(encode_mp3(stereo))
        print(f'✓ {path.name} ({path.stat().st_size // 1024}KB)')


if __name__ == '__main__':
    main()
