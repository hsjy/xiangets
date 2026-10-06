package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.entity.Poem;
import com.yuetin.music.entity.Poet;
import com.yuetin.music.service.PoetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 随机一诗：每次刷新都可能不同。 */
    @GetMapping("/poem/random")
    public Result<Poem> random() {
        return Result.ok(poetryService.randomPoem());
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
    /** 诗选分页：语料库全量浏览；tier=1 只看精选。 */
    @GetMapping("/poem/page")
    public Result<Map<String, Object>> pagePoems(@RequestParam(required = false) String dynasty,
                                                 @RequestParam(required = false) String mood,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer tier,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "48") Integer size) {
        return Result.ok(poetryService.pagePoems(dynasty, mood, keyword, page, size, tier));
    }

    /** 诗人分页：语料库全量浏览。 */
    @GetMapping("/poet/page")
    public Result<Map<String, Object>> pagePoets(@RequestParam(required = false) String dynasty,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "48") Integer size) {
        return Result.ok(poetryService.pagePoets(dynasty, keyword, page, size));
    }

    @GetMapping("/poet/detail/{id}")
    public Result<Map<String, Object>> poetDetail(@PathVariable Long id) {
        return Result.ok(poetryService.poetDetail(id));
    }

    /* ---------------- 收藏与感悟 ---------------- */

    /** 收藏切换。 */
    @PostMapping("/poem/favorite/{id}")
    public Result<Map<String, Object>> favorite(@PathVariable Long id) {
        return Result.ok(poetryService.toggleFavorite(id));
    }

    @GetMapping("/poem/favorite/state/{id}")
    public Result<Map<String, Object>> favoriteState(@PathVariable Long id) {
        return Result.ok(poetryService.favoriteState(id));
    }

    /** 书斋：收藏的诗。 */
    @GetMapping("/poem/favorite/list")
    public Result<List<Map<String, Object>>> favoriteList() {
        return Result.ok(poetryService.favoriteList());
    }

    @GetMapping("/poem/comment/list/{poemId}")
    public Result<List<com.yuetin.music.entity.Comment>> poemComments(@PathVariable Long poemId) {
        return Result.ok(poetryService.poemComments(poemId));
    }

    @PostMapping("/poem/comment/add")
    public Result<com.yuetin.music.entity.Comment> addPoemComment(@RequestBody Map<String, Object> body) {
        Long poemId = Long.valueOf(String.valueOf(body.get("poemId")));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        Long parentId = body.get("parentId") == null ? null
                : Long.valueOf(String.valueOf(body.get("parentId")));
        return Result.ok(poetryService.addPoemComment(poemId, content, parentId));
    }

    /** 书斋：我的感悟。 */
    /** 全站感悟（管理后台）。 */
    @GetMapping("/poem/comment/all")
    public Result<List<Map<String, Object>>> allComments() {
        return Result.ok(poetryService.allCommentsForAdmin());
    }

    @GetMapping("/poem/comment/mine")
    public Result<List<com.yuetin.music.entity.Comment>> myComments() {
        return Result.ok(poetryService.myComments());
    }

    @DeleteMapping("/poem/comment/delete/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        poetryService.deleteComment(id);
        return Result.ok();
    }

    /** 点赞/取消点赞感悟（切换）。 */
    @PostMapping("/poem/comment/like/{id}")
    public Result<Map<String, Object>> likeComment(@PathVariable Long id) {
        return Result.ok(poetryService.likeComment(id));
    }
}