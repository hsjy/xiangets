#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
弦歌 · 本地 AI 音乐生成边车（MusicGen / Apple Silicon MPS）

把 Meta MusicGen 包成 HTTP 服务，供 SpringBoot AiMusicService 调用：
    POST /generate  { tags: str, lyrics: str, durationSec: int }
    → 200 audio/mpeg 二进制

说明：
- MusicGen 是文本→器乐模型，理解意境/情绪/乐器/节奏，不生成人声；
  诗行原文不作演唱歌词，而是与 tags 一起参与 prompt（影响生成的意象走向）。
- MPS (Apple GPU) 上 musicgen-small 生成 30s 约 1~3 分钟。
- 启动：HF_ENDPOINT=https://hf-mirror.com python3 tools/ai_server.py
  然后后端设置 AI_ACESTEP_URL=http://127.0.0.1:8000/generate
"""
import io
import re
import wave

import lameenc
import numpy as np
import torch
from fastapi import FastAPI
from fastapi.responses import Response
from pydantic import BaseModel

app = FastAPI(title="弦歌 AI 谱曲边车")
_state = {"model": None}


def get_model():
    if _state["model"] is None:
        from transformers import MusicgenForConditionalGeneration, MusicgenProcessor
        print("加载 MusicGen（首次约 10~30s）…", flush=True)
        proc = MusicgenProcessor.from_pretrained("facebook/musicgen-small")
        model = MusicgenForConditionalGeneration.from_pretrained(
            "facebook/musicgen-small", torch_dtype=torch.float32
        )
        model.to("mps")
        model.eval()
        _state["model"] = (proc, model)
    return _state["model"]


class GenReq(BaseModel):
    tags: str = "chinese traditional, guzheng, peaceful"
    lyrics: str = ""
    durationSec: int = 30


def clean_prompt(tags: str, lyrics: str) -> str:
    """MusicGen 吃英文描述；把诗行里的意象词以权重形式追加，影响走向。"""
    imagery = {
        "月": "moonlight", "夜": "night", "雨": "rain", "雪": "snow",
        "风": "wind", "山": "mountains", "江": "river", "水": "water",
        "花": "flowers", "酒": "mellow", "剑": "heroic", "马": "galloping",
        "雁": "wild geese calls", "笛": "flute", "琴": "strings",
    }
    found = [en for zh, en in imagery.items() if zh in (tags + lyrics)]
    prompt = tags.strip()
    if found:
        prompt += ", " + ", ".join(sorted(set(found))[:6])
    return prompt


def pcm_to_mp3(pcm: np.ndarray, sr: int) -> bytes:
    """float32 [-1,1] 单声道 → mp3。"""
    pcm16 = np.clip(pcm * 32767, -32767, 32767).astype(np.int16)
    enc = lameenc.Encoder()
    enc.set_bit_rate(128)
    enc.set_in_sample_rate(sr)
    enc.set_channels(1)
    enc.set_quality(2)
    return enc.encode(pcm16.tobytes()) + enc.flush()


@app.post("/generate")
def generate(req: GenReq):
    proc, model = get_model()
    # MusicGen 每秒约 50 token，封顶 30s（MPS 上更久不划算）
    dur = max(10, min(int(req.durationSec or 30), 30))
    max_tokens = int(dur * 50.2)
    prompt = clean_prompt(req.tags, req.lyrics)
    print(f"[generate] prompt={prompt!r} tokens={max_tokens}", flush=True)

    inputs = proc(text=[prompt], padding=True, return_tensors="pt").to("mps")
    with torch.no_grad():
        audio = model.generate(**inputs, do_sample=True, guidance_scale=3.0, max_new_tokens=max_tokens)
    sr = model.config.audio_encoder.sampling_rate
    wav = audio[0, 0].cpu().float().numpy()

    # 首尾淡入淡出
    fade = min(int(sr * 1.5), len(wav) // 4)
    wav[:fade] *= np.linspace(0, 1, fade)
    wav[-fade:] *= np.linspace(1, 0, fade)
    peak = max(np.max(np.abs(wav)), 1e-9)
    wav = wav / peak * 0.85

    mp3 = bytes(pcm_to_mp3(wav, sr))
    return Response(
        content=mp3, media_type="audio/mpeg",
        headers={"X-Duration-Seconds": f"{round(len(wav) / sr, 1)}"},
    )


@app.get("/health")
def health():
    return {"ok": True, "model": "facebook/musicgen-small", "device": "mps"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000, log_level="info")
