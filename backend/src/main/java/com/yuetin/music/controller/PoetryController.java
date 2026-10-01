package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.entity.Poem;
import com.yuetin.music.entity.Poet;
import com.yuetin.music.service.PoetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 诗与诗人：欣赏网站的主体接口。
 */
@RestController
@RequiredArgsConstructor
public class PoetryController {

    private final PoetryService poetryService;

    @GetMapping("/poem/list")
    public Result<List<Poem>> listPoems(@RequestParam(required = false) String dynasty,
                                        @RequestParam(required = false) String mood,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(poetryService.listPoems(dynasty, mood, keyword));
    }

    @GetMapping("/poem/detail/{id}")
    public Result<Poem> poemDetail(@PathVariable Long id) {
        return Result.ok(poetryService.poemDetail(id));
    }

    /** 今日一诗：按日期确定性轮换。 */
    @GetMapping("/poem/today")
    public Result<Map<String, Object>> today() {
        return Result.ok(poetryService.todayPoem());
    }

    /** 筛选项：朝代 / 情绪 / 总量。 */
    @GetMapping("/poem/facets")
    public Result<Map<String, Object>> facets() {
        return Result.ok(poetryService.facets());
    }

    @GetMapping("/poet/list")
    public Result<List<Poet>> listPoets(@RequestParam(required = false) String dynasty) {
        return Result.ok(poetryService.listPoets(dynasty));
    }

    /** 诗人页：生平故事 + 存诗。 */
    @GetMapping("/poet/detail/{id}")
    public Result<Map<String, Object>> poetDetail(@PathVariable Long id) {
        return Result.ok(poetryService.poetDetail(id));
    }
}
