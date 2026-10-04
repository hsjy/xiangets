package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.entity.PoemCollection;
import com.yuetin.music.service.CuratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 策展接口：主题选集与诗人关系。 */
@RestController
@RequiredArgsConstructor
public class CuratorController {

    private final CuratorService curatorService;

    @GetMapping("/collection/list")
    public Result<List<PoemCollection>> collections() {
        return Result.ok(curatorService.listCollections());
    }

    @GetMapping("/collection/detail/{id}")
    public Result<Map<String, Object>> collectionDetail(@PathVariable Long id) {
        return Result.ok(curatorService.collectionDetail(id));
    }

    @GetMapping("/poet/relations/{id}")
    public Result<List<Map<String, Object>>> poetRelations(@PathVariable Long id) {
        return Result.ok(curatorService.poetRelations(id));
    }
}
