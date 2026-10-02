package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.entity.AiMusic;
import com.yuetin.music.service.AiMusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** AI 谱曲（古诗转音乐）接口。 */
@RestController
@RequiredArgsConstructor
public class AiMusicController {

    private final AiMusicService aiMusicService;

    /** 发起谱曲：幂等——已有完成缓存直接返回；force=true 时重新谱一版。 */
    @PostMapping("/ai/music/generate")
    public Result<AiMusic> generate(@RequestBody Map<String, Object> body) {
        Long poemId = Long.valueOf(String.valueOf(body.get("poemId")));
        boolean force = Boolean.parseBoolean(String.valueOf(body.getOrDefault("force", false)));
        return Result.ok(aiMusicService.generate(poemId, force));
    }

    /** 任务状态（前端轮询）。 */
    @GetMapping("/ai/music/job/{id}")
    public Result<AiMusic> job(@PathVariable Long id) {
        return Result.ok(aiMusicService.job(id));
    }

    /** 某诗已生成的 AI 曲。 */
    @GetMapping("/ai/music/list/{poemId}")
    public Result<List<AiMusic>> list(@PathVariable Long poemId) {
        return Result.ok(aiMusicService.listDone(poemId));
    }
}
