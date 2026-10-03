package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.entity.Comment;
import com.yuetin.music.entity.Poem;
import com.yuetin.music.entity.Poet;
import com.yuetin.music.mapper.CommentMapper;
import com.yuetin.music.mapper.PoemFavoriteMapper;
import com.yuetin.music.mapper.PoemMapper;
import com.yuetin.music.mapper.PoetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PoetryService {

    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final CommentMapper commentMapper;
    private final PoemFavoriteMapper poemFavoriteMapper;

    /** 诗选：朝代/情绪/关键词自由组合。 */
    public List<Poem> listPoems(String dynasty, String mood, String keyword) {
        return poemMapper.search(blank(dynasty), blank(mood), blank(keyword));
    }

    /** 诗选分页（语料库全量浏览用）；tier 为空表示全部层级。 */
    public Map<String, Object> pagePoems(String dynasty, String mood, String keyword,
                                         Integer page, Integer size, Integer tier) {
        int p = Math.max(1, page == null ? 1 : page);
        int s = Math.min(100, Math.max(1, size == null ? 48 : size));
        long total = poemMapper.countSearch(blank(dynasty), blank(mood), blank(keyword), tier);
        List<Poem> list = total == 0 ? List.of()
                : poemMapper.pageSearch(blank(dynasty), blank(mood), blank(keyword), tier,
                        (long) (p - 1) * s, s);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", p);
        data.put("size", s);
        return data;
    }

    /** 诗人分页（语料库全量浏览用）。 */
    public Map<String, Object> pagePoets(String dynasty, String keyword, Integer page, Integer size) {
        int p = Math.max(1, page == null ? 1 : page);
        int s = Math.min(100, Math.max(1, size == null ? 48 : size));
        long total = poetMapper.countList(blank(dynasty), blank(keyword));
        List<Poet> list = total == 0 ? List.of()
                : poetMapper.pageList(blank(dynasty), blank(keyword), (long) (p - 1) * s, s);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", p);
        data.put("size", s);
        return data;
    }

    public Poem poemDetail(Long id) {
        Poem poem = poemMapper.findById(id);
        if (poem == null) throw BizException.notFound("诗不存在");
        return poem;
    }

    /**
     * 今日一诗：按年内天数确定性轮换，同一天所有人看到同一首。
     */
    /** 随机一诗：每次调用都可能不同（首页「此刻」卷首用）。 */
    public Poem randomPoem() {
        long count = poemMapper.countCurated();
        if (count == 0) throw BizException.notFound("诗库为空");
        int offset = java.util.concurrent.ThreadLocalRandom.current().nextInt((int) count);
        Poem poem = poemMapper.findByOffsetCurated(offset);
        if (poem == null) throw BizException.notFound("诗不存在");
        return poem;
    }

    public Map<String, Object> todayPoem() {
        long count = poemMapper.countCurated();
        Map<String, Object> data = new HashMap<>();
        if (count == 0) {
            data.put("poem", null);
            return data;
        }
        int day = LocalDate.now().getDayOfYear();
        Poem poem = poemMapper.findByOffsetCurated(day % count);
        data.put("poem", poem);
        data.put("date", LocalDate.now().toString());
        data.put("index", day % count);
        data.put("total", count);
        return data;
    }

    /** 诗人名录（含各人存诗数）。 */
    public List<Poet> listPoets(String dynasty) {
        return poetMapper.list(blank(dynasty));
    }

    /** 诗人页：生平故事 + 存诗。 */
    public Map<String, Object> poetDetail(Long id) {
        Poet poet = poetMapper.findById(id);
        if (poet == null) throw BizException.notFound("诗人不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("poet", poet);
        data.put("poems", poemMapper.findByPoet(id));
        return data;
    }

    /** 情绪/朝代筛选的可选项（前端筛选项不写死）。 */
    public Map<String, Object> facets() {
        Map<String, Object> data = new HashMap<>();
        data.put("dynasties", poetMapper.distinctDynasties());
        data.put("moods", poemMapper.distinctMoods());
        data.put("poemCount", poemMapper.countAll());
        return data;
    }

    /* ---------------- 收藏与感悟 ---------------- */

    /** 收藏切换，返回切换后的状态。 */
    public Map<String, Object> toggleFavorite(Long poemId) {
        Long uid = requireLogin();
        if (poemMapper.findById(poemId) == null) throw BizException.notFound("诗不存在");
        boolean favorited;
        if (poemFavoriteMapper.exists(uid, poemId)) {
            poemFavoriteMapper.remove(uid, poemId);
            favorited = false;
        } else {
            poemFavoriteMapper.add(uid, poemId);
            favorited = true;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("favorited", favorited);
        return data;
    }

    /** 意境页查询当前用户是否已收藏；未登录返回 false。 */
    public Map<String, Object> favoriteState(Long poemId) {
        Long uid = UserContext.id();
        Map<String, Object> data = new HashMap<>();
        data.put("favorited", uid != null && poemFavoriteMapper.exists(uid, poemId));
        return data;
    }

    /** 书斋：收藏的诗。 */
    public List<Map<String, Object>> favoriteList() {
        return poemFavoriteMapper.listByUser(requireLogin());
    }

    /** 诗的感悟列表（公开，匿名可读）。 */
    public List<Comment> poemComments(Long poemId) {
        return commentMapper.findByPoem(poemId);
    }

    public Comment addPoemComment(Long poemId, String content) {
        Long uid = requireLogin();
        if (content == null || content.trim().isEmpty()) {
            throw BizException.badRequest("感悟内容不能为空");
        }
        if (content.length() > 500) throw BizException.badRequest("感悟最多 500 字");
        if (poemMapper.findById(poemId) == null) throw BizException.notFound("诗不存在");

        Comment c = new Comment();
        c.setPoemId(poemId);
        c.setUserId(uid);
        c.setContent(content.trim());
        commentMapper.insertPoem(c);
        c.setUsername(UserContext.get() == null ? null : UserContext.get().getUsername());
        return c;
    }

    /** 我的感悟（书斋）。 */
    public List<Comment> myComments() {
        return commentMapper.mine(requireLogin());
    }

    /** 删除自己的感悟。 */
    public void deleteComment(Long id) {
        Long uid = requireLogin();
        if (commentMapper.deleteByIdAndUser(id, uid) == 0) {
            throw BizException.forbidden("只能删除自己的感悟");
        }
    }

    private Long requireLogin() {
        Long uid = UserContext.id();
        if (uid == null) throw BizException.unauthorized("登录已过期，请重新登录");
        return uid;
    }

    private String blank(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
