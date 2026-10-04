package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.service.FeihuaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 飞花令：随机抽取含令字的诗句。 */
@RestController
@RequiredArgsConstructor
public class FeihuaController {

    private final FeihuaService feihuaService;

    @PostMapping("/feihua/check")
    public Result<Map<String, Object>> check(@RequestBody Map<String, Object> body) {
        String ch = String.valueOf(body.getOrDefault("char", ""));
        String line = String.valueOf(body.getOrDefault("line", ""));
        @SuppressWarnings("unchecked")
        List<String> exclude = (List<String>) body.get("exclude");
        return Result.ok(feihuaService.check(ch, line, exclude));
    }

    @PostMapping("/feihua/draw")
    public Result<Map<String, Object>> draw(@RequestBody Map<String, Object> body) {
        String ch = String.valueOf(body.getOrDefault("char", ""));
        @SuppressWarnings("unchecked")
        List<String> exclude = (List<String>) body.get("exclude");
        return Result.ok(feihuaService.draw(ch, exclude));
    }
}
