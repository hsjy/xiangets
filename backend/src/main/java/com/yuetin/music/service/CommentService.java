package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.entity.Comment;
import com.yuetin.music.mapper.CommentMapper;
import com.yuetin.music.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final SongMapper songMapper;
    private final com.yuetin.music.mapper.PoemMapper poemMapper;

    public List<Comment> list(Long songId) {
        return commentMapper.findBySong(songId);
    }

    public Comment add(Long songId, String content) {
        Long uid = requireLogin();
        if (content == null || content.trim().isEmpty()) {
            throw BizException.badRequest("评论内容不能为空");
        }
        if (content.length() > 500) {
            throw BizException.badRequest("评论最多 500 字");
        }
        if (songMapper.findById(songId) == null) {
            throw BizException.notFound("歌曲不存在");
        }

        Comment c = new Comment();
        c.setSongId(songId);
        c.setUserId(uid);
        c.setContent(content.trim());
        commentMapper.insert(c);
        // 回填展示字段，前端无需再请求一次
        c.setUsername(currentName());
        return c;
    }

    public void delete(Long id) {
        Long uid = requireLogin();
        // 管理员可删除任意感悟；普通用户只能删自己的
        if (isAdmin()) {
            commentMapper.deleteById(id);
            return;
        }
        int rows = commentMapper.deleteByIdAndUser(id, uid);
        if (rows == 0) {
            throw BizException.forbidden("只能删除自己的感悟");
        }
    }

    /** 全站感悟（管理后台用）：带用户名与诗题。 */
    public List<Map<String, Object>> allForAdmin() {
        if (!isAdmin()) throw BizException.forbidden("需要管理员身份");
        return commentMapper.findAllWithMeta();
    }

    private boolean isAdmin() {
        var cu = UserContext.get();
        return cu != null && cu.isAdmin();
    }

    /* ---------------- 诗的感悟：互动 ---------------- */

    /** 感悟列表：平铺（主感悟 + 回复），前端按层级渲染；点赞数与是否已赞随 viewer。 */
    public List<Comment> listByPoem(Long poemId, Long viewerId) {
        return commentMapper.findByPoem(poemId, viewerId);
    }

    /** 发感悟或回复：parentId 非空即为回复（统一挂到根感悟，仅一级展开）。 */
    public Comment addPoemComment(Long poemId, String content, Long parentId, Long uid) {
        if (content == null || content.trim().isEmpty()) {
            throw BizException.badRequest("感悟内容不能为空");
        }
        if (content.length() > 500) throw BizException.badRequest("感悟最多 500 字");
        if (poemMapper.findById(poemId) == null) throw BizException.notFound("诗不存在");

        Comment c = new Comment();
        c.setPoemId(poemId);
        c.setUserId(uid);
        c.setContent(content.trim());
        if (parentId != null) {
            Map<String, Object> parent = commentMapper.findMeta(parentId);
            if (parent == null) throw BizException.notFound("回复的感悟不存在");
            // 只展开一级：回复的回复统一挂到根感悟
            c.setParentId(parent.get("parentId") != null ? (Long) parent.get("parentId") : parentId);
            c.setReplyTo((String) parent.get("userName"));
        }
        commentMapper.insertPoem(c);
        c.setUsername(UserContext.get() == null ? null : UserContext.get().getUsername());
        return c;
    }

    /** 删除感悟：主感悟连同其回复与点赞；管理员可删任意。 */
    public void deletePoemComment(Long id, Long uid) {
        Comment c = commentMapper.findByIdPoem(id);
        if (c == null) throw BizException.notFound("感悟不存在");
        var cu = UserContext.get();
        boolean admin = cu != null && cu.isAdmin();
        if (!admin && (c.getUserId() == null || !c.getUserId().equals(uid))) {
            throw BizException.forbidden("只能删除自己的感悟");
        }
        commentMapper.deleteLikesCascade(id);
        commentMapper.deleteReplies(id);
        commentMapper.deleteByIdPoem(id);
    }

    /** 点赞/取消点赞（切换），返回最新状态。 */
    public Map<String, Object> toggleLike(Long commentId, Long uid) {
        if (commentMapper.findMeta(commentId) == null) throw BizException.notFound("感悟不存在");
        int liked = commentMapper.likedBy(commentId, uid);
        if (liked > 0) {
            commentMapper.deleteLike(commentId, uid);
        } else {
            commentMapper.insertLike(commentId, uid);
        }
        return Map.of("liked", liked == 0, "likeCount", commentMapper.likeCount(commentId));
    }

    private String currentName() {
        return UserContext.get() == null ? null : UserContext.get().getUsername();
    }

    private Long requireLogin() {
        Long uid = UserContext.id();
        if (uid == null) throw BizException.unauthorized("登录已过期，请重新登录");
        return uid;
    }
}
