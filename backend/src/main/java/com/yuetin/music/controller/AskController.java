package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.service.PoemAskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 问诗：DeepSeek 诗歌问答。 */
@RestController
@RequiredArgsConstructor
public class AskController {

    private final PoemAskService poemAskService;

    @PostMapping("/poem/ask/{poemId}")
    public Result<Map<String, Object>> ask(@PathVariable Long poemId,
                                           @RequestBody Map<String, Object> body) {
        String question = String.valueOf(body.getOrDefault("question", ""));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.get("history");
        return Result.ok(poemAskService.ask(poemId, question, history));
    }
}
